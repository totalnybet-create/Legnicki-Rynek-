package pl.legnickirynek.app.feature.messages

import javax.inject.Inject

data class SendMessageCommand(
    val senderId: String,
    val recipientId: String,
    val body: String,
    val listingId: String? = null
)

data class UserMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val body: String,
    val listingId: String?,
    val sentAtEpochMillis: Long,
    val readAtEpochMillis: Long?
)

data class ConversationSummary(
    val conversationId: String,
    val participantId: String,
    val listingId: String?,
    val lastMessage: String,
    val lastMessageAtEpochMillis: Long,
    val unreadCount: Int
)

enum class MessageField {
    SENDER,
    RECIPIENT,
    BODY,
    LISTING
}

data class MessageValidationIssue(
    val field: MessageField,
    val message: String
)

data class MessageValidationResult(
    val normalizedCommand: SendMessageCommand?,
    val issues: List<MessageValidationIssue>
) {
    val isValid: Boolean = normalizedCommand != null && issues.isEmpty()
}

sealed interface MessageOperationResult {
    data class Success(val message: UserMessage) : MessageOperationResult
    data class ValidationFailed(val issues: List<MessageValidationIssue>) : MessageOperationResult
    data class NotFound(val conversationId: String) : MessageOperationResult
    data class Failure(val message: String, val cause: Throwable? = null) : MessageOperationResult
}

class MessageValidator @Inject constructor() {

    fun validate(command: SendMessageCommand): MessageValidationResult {
        val normalized = command.copy(
            senderId = command.senderId.trim(),
            recipientId = command.recipientId.trim(),
            body = command.body.trim().replace(WHITESPACE_REGEX, " "),
            listingId = command.listingId?.trim()?.takeIf(String::isNotBlank)
        )
        val issues = buildList {
            if (!USER_ID_PATTERN.matches(normalized.senderId)) {
                add(MessageValidationIssue(MessageField.SENDER, "Nieprawidłowy identyfikator nadawcy."))
            }
            if (!USER_ID_PATTERN.matches(normalized.recipientId)) {
                add(MessageValidationIssue(MessageField.RECIPIENT, "Nieprawidłowy identyfikator odbiorcy."))
            }
            if (normalized.senderId == normalized.recipientId && normalized.senderId.isNotBlank()) {
                add(MessageValidationIssue(MessageField.RECIPIENT, "Nie można wysłać wiadomości do samego siebie."))
            }
            if (normalized.body.length !in BODY_LENGTH) {
                add(
                    MessageValidationIssue(
                        MessageField.BODY,
                        "Wiadomość musi mieć od ${BODY_LENGTH.first} do ${BODY_LENGTH.last} znaków."
                    )
                )
            }
            val listingId = normalized.listingId
            if (listingId != null && !LISTING_ID_PATTERN.matches(listingId)) {
                add(MessageValidationIssue(MessageField.LISTING, "Nieprawidłowy identyfikator ogłoszenia."))
            }
        }
        return MessageValidationResult(
            normalizedCommand = normalized.takeIf { issues.isEmpty() },
            issues = issues
        )
    }

    private companion object {
        val BODY_LENGTH = 1..2_000
        val USER_ID_PATTERN = Regex("^[A-Za-z0-9._@-]{2,128}$")
        val LISTING_ID_PATTERN = Regex("^[A-Za-z0-9._-]{2,160}$")
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
