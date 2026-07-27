package pl.legnickirynek.app.feature.messages

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

class MessagingService internal constructor(
    private val storage: MessageStorage,
    private val validator: MessageValidator,
    private val clock: () -> Long,
    private val idFactory: () -> String
) {

    @Inject
    constructor(
        fileStore: MessageFileStore,
        validator: MessageValidator
    ) : this(
        storage = fileStore,
        validator = validator,
        clock = System::currentTimeMillis,
        idFactory = { UUID.randomUUID().toString() }
    )

    suspend fun send(command: SendMessageCommand): MessageOperationResult {
        val validation = validator.validate(command)
        val normalized = validation.normalizedCommand
            ?: return MessageOperationResult.ValidationFailed(validation.issues)
        val now = clock()
        val conversationId = conversationId(normalized)

        return runCatching {
            val messages = storage.readAll()
            val duplicate = messages.lastOrNull { message ->
                message.conversationId == conversationId &&
                    message.senderId == normalized.senderId &&
                    message.body == normalized.body &&
                    now - message.sentAtEpochMillis in 0..DUPLICATE_WINDOW_MILLIS
            }
            if (duplicate != null) {
                return@runCatching MessageOperationResult.Success(duplicate)
            }

            val message = UserMessage(
                id = idFactory(),
                conversationId = conversationId,
                senderId = normalized.senderId,
                recipientId = normalized.recipientId,
                body = normalized.body,
                listingId = normalized.listingId,
                sentAtEpochMillis = now,
                readAtEpochMillis = null
            )
            storage.replaceAll(messages + message)
            MessageOperationResult.Success(message)
        }.getOrElse { error ->
            MessageOperationResult.Failure(
                message = "Nie udało się wysłać wiadomości.",
                cause = error
            )
        }
    }

    suspend fun thread(
        conversationId: String,
        userId: String
    ): List<UserMessage> = storage.readAll()
        .asSequence()
        .filter { it.conversationId == conversationId }
        .filter { it.senderId == userId || it.recipientId == userId }
        .sortedBy(UserMessage::sentAtEpochMillis)
        .toList()

    suspend fun conversations(userId: String): List<ConversationSummary> = storage.readAll()
        .asSequence()
        .filter { it.senderId == userId || it.recipientId == userId }
        .groupBy(UserMessage::conversationId)
        .mapNotNull { (conversationId, messages) ->
            val last = messages.maxByOrNull(UserMessage::sentAtEpochMillis) ?: return@mapNotNull null
            val participant = if (last.senderId == userId) last.recipientId else last.senderId
            ConversationSummary(
                conversationId = conversationId,
                participantId = participant,
                listingId = last.listingId,
                lastMessage = last.body,
                lastMessageAtEpochMillis = last.sentAtEpochMillis,
                unreadCount = messages.count {
                    it.recipientId == userId && it.readAtEpochMillis == null
                }
            )
        }
        .sortedByDescending(ConversationSummary::lastMessageAtEpochMillis)

    suspend fun markConversationRead(
        conversationId: String,
        userId: String
    ): Boolean = runCatching {
        val messages = storage.readAll()
        var changed = false
        val updated = messages.map { message ->
            if (
                message.conversationId == conversationId &&
                message.recipientId == userId &&
                message.readAtEpochMillis == null
            ) {
                changed = true
                message.copy(readAtEpochMillis = clock())
            } else {
                message
            }
        }
        if (changed) storage.replaceAll(updated)
        changed
    }.getOrDefault(false)

    suspend fun deleteConversation(
        conversationId: String,
        userId: String
    ): Boolean = runCatching {
        val messages = storage.readAll()
        val conversation = messages.filter { it.conversationId == conversationId }
        if (conversation.isEmpty()) return@runCatching false
        if (conversation.none { it.senderId == userId || it.recipientId == userId }) {
            return@runCatching false
        }
        storage.replaceAll(messages.filterNot { it.conversationId == conversationId })
        true
    }.getOrDefault(false)

    private fun conversationId(command: SendMessageCommand): String {
        val users = listOf(command.senderId, command.recipientId).sorted()
        val input = "${users[0]}|${users[1]}|${command.listingId.orEmpty()}"
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(StandardCharsets.UTF_8))
        return hash.take(16).joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val DUPLICATE_WINDOW_MILLIS = 2_000L
    }
}
