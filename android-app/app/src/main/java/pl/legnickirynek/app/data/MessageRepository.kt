package pl.legnickirynek.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.legnickirynek.app.data.local.AppDatabase
import pl.legnickirynek.app.data.local.toEntity
import pl.legnickirynek.app.data.local.toModel
import pl.legnickirynek.app.domain.ConversationAccessPolicy
import pl.legnickirynek.app.model.ChatMessage
import pl.legnickirynek.app.model.Conversation

interface MessageRepository {
    fun observeConversations(accountId: String): Flow<List<Conversation>>
    fun observeConversation(accountId: String, conversationId: String): Flow<Conversation?>
    fun observeMessages(accountId: String, conversationId: String): Flow<List<ChatMessage>>
    suspend fun upsertConversation(accountId: String, conversation: Conversation)
    suspend fun seed(accountId: String, conversations: List<Conversation>, messages: List<ChatMessage>)
    suspend fun sendMessage(accountId: String, conversation: Conversation, message: ChatMessage)
    suspend fun markConversationRead(accountId: String, conversationId: String)
    suspend fun deleteConversation(accountId: String, conversationId: String)
    suspend fun claimLegacyConversations(accountId: String)
    suspend fun conversationCount(): Int
}

class OfflineMessageRepository(
    private val database: AppDatabase
) : MessageRepository {
    private val messageDao = database.messageDao()

    override fun observeConversations(accountId: String): Flow<List<Conversation>> {
        requireAccountId(accountId)
        return messageDao.observeConversations(accountId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    override fun observeConversation(
        accountId: String,
        conversationId: String
    ): Flow<Conversation?> {
        requireAccountId(accountId)
        require(conversationId.isNotBlank()) { "Identyfikator rozmowy nie może być pusty." }
        return messageDao.observeConversation(accountId, conversationId).map { it?.toModel() }
    }

    override fun observeMessages(
        accountId: String,
        conversationId: String
    ): Flow<List<ChatMessage>> {
        requireAccountId(accountId)
        require(conversationId.isNotBlank()) { "Identyfikator rozmowy nie może być pusty." }
        return messageDao.observeMessages(accountId, conversationId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun upsertConversation(accountId: String, conversation: Conversation) {
        ConversationAccessPolicy.requireAccess(conversation, accountId)
        messageDao.upsertConversation(conversation.toEntity())
    }

    override suspend fun seed(
        accountId: String,
        conversations: List<Conversation>,
        messages: List<ChatMessage>
    ) {
        requireAccountId(accountId)
        val ownedConversations = conversations.map { it.copy(accountId = accountId) }
        val conversationIds = ownedConversations.mapTo(mutableSetOf()) { it.id }
        require(messages.all { it.conversationId in conversationIds }) {
            "Wiadomość musi należeć do jednej z zapisywanych rozmów."
        }

        database.withTransaction {
            messageDao.upsertConversations(ownedConversations.map { it.toEntity() })
            messageDao.upsertMessages(messages.map { it.toEntity() })
        }
    }

    override suspend fun sendMessage(
        accountId: String,
        conversation: Conversation,
        message: ChatMessage
    ) {
        ConversationAccessPolicy.requireAccess(conversation, accountId)
        require(message.conversationId == conversation.id) {
            "Wiadomość nie należy do wskazanej rozmowy."
        }
        require(message.body.isNotBlank()) { "Treść wiadomości nie może być pusta." }

        database.withTransaction {
            messageDao.upsertConversation(conversation.toEntity())
            messageDao.upsertMessage(message.toEntity())
            messageDao.updateConversationPreview(
                accountId = accountId,
                conversationId = conversation.id,
                lastMessage = message.body,
                updatedAt = message.sentAt,
                unreadCount = conversation.unreadCount
            )
        }
    }

    override suspend fun markConversationRead(accountId: String, conversationId: String) {
        requireAccountAndConversation(accountId, conversationId)
        database.withTransaction {
            messageDao.markIncomingMessagesRead(accountId, conversationId)
            messageDao.clearUnreadCount(accountId, conversationId)
        }
    }

    override suspend fun deleteConversation(accountId: String, conversationId: String) {
        requireAccountAndConversation(accountId, conversationId)
        messageDao.deleteConversation(accountId, conversationId)
    }

    override suspend fun claimLegacyConversations(accountId: String) {
        requireAccountId(accountId)
        messageDao.claimLegacyConversations(accountId)
    }

    override suspend fun conversationCount(): Int = messageDao.conversationCount()

    private fun requireAccountAndConversation(accountId: String, conversationId: String) {
        requireAccountId(accountId)
        require(conversationId.isNotBlank()) { "Identyfikator rozmowy nie może być pusty." }
    }

    private fun requireAccountId(accountId: String) {
        require(accountId.isNotBlank()) { "Identyfikator konta nie może być pusty." }
    }
}
