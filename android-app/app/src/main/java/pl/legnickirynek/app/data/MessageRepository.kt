package pl.legnickirynek.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.legnickirynek.app.data.local.AppDatabase
import pl.legnickirynek.app.data.local.toEntity
import pl.legnickirynek.app.data.local.toModel
import pl.legnickirynek.app.model.ChatMessage
import pl.legnickirynek.app.model.Conversation

interface MessageRepository {
    fun observeConversations(): Flow<List<Conversation>>
    fun observeConversation(conversationId: String): Flow<Conversation?>
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>
    suspend fun upsertConversation(conversation: Conversation)
    suspend fun seed(conversations: List<Conversation>, messages: List<ChatMessage>)
    suspend fun sendMessage(conversation: Conversation, message: ChatMessage)
    suspend fun markConversationRead(conversationId: String)
    suspend fun deleteConversation(conversationId: String)
    suspend fun conversationCount(): Int
}

class OfflineMessageRepository(
    private val database: AppDatabase
) : MessageRepository {
    private val messageDao = database.messageDao()

    override fun observeConversations(): Flow<List<Conversation>> =
        messageDao.observeConversations().map { entities ->
            entities.map { it.toModel() }
        }

    override fun observeConversation(conversationId: String): Flow<Conversation?> =
        messageDao.observeConversation(conversationId).map { it?.toModel() }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        messageDao.observeMessages(conversationId).map { entities ->
            entities.map { it.toModel() }
        }

    override suspend fun upsertConversation(conversation: Conversation) {
        messageDao.upsertConversation(conversation.toEntity())
    }

    override suspend fun seed(
        conversations: List<Conversation>,
        messages: List<ChatMessage>
    ) {
        database.withTransaction {
            messageDao.upsertConversations(conversations.map { it.toEntity() })
            messageDao.upsertMessages(messages.map { it.toEntity() })
        }
    }

    override suspend fun sendMessage(
        conversation: Conversation,
        message: ChatMessage
    ) {
        database.withTransaction {
            messageDao.upsertConversation(conversation.toEntity())
            messageDao.upsertMessage(message.toEntity())
            messageDao.updateConversationPreview(
                conversationId = conversation.id,
                lastMessage = message.body,
                updatedAt = message.sentAt,
                unreadCount = conversation.unreadCount
            )
        }
    }

    override suspend fun markConversationRead(conversationId: String) {
        database.withTransaction {
            messageDao.markIncomingMessagesRead(conversationId)
            messageDao.clearUnreadCount(conversationId)
        }
    }

    override suspend fun deleteConversation(conversationId: String) {
        messageDao.deleteConversation(conversationId)
    }

    override suspend fun conversationCount(): Int = messageDao.conversationCount()
}
