package pl.legnickirynek.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query(
        "SELECT * FROM conversations " +
            "WHERE accountId = :accountId ORDER BY updatedAt DESC"
    )
    fun observeConversations(accountId: String): Flow<List<ConversationEntity>>

    @Query(
        "SELECT * FROM conversations " +
            "WHERE id = :conversationId AND accountId = :accountId LIMIT 1"
    )
    fun observeConversation(accountId: String, conversationId: String): Flow<ConversationEntity?>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM conversations " +
            "WHERE id = :conversationId AND accountId = :accountId)"
    )
    suspend fun conversationExists(accountId: String, conversationId: String): Boolean

    @Query(
        "SELECT messages.* FROM messages " +
            "INNER JOIN conversations ON conversations.id = messages.conversationId " +
            "WHERE messages.conversationId = :conversationId " +
            "AND conversations.accountId = :accountId " +
            "ORDER BY messages.sentAt ASC"
    )
    fun observeMessages(accountId: String, conversationId: String): Flow<List<MessageEntity>>

    @Upsert
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Upsert
    suspend fun upsertConversations(conversations: List<ConversationEntity>)

    @Upsert
    suspend fun upsertMessage(message: MessageEntity)

    @Upsert
    suspend fun upsertMessages(messages: List<MessageEntity>)

    @Query(
        "UPDATE conversations " +
            "SET lastMessage = :lastMessage, updatedAt = :updatedAt, unreadCount = :unreadCount " +
            "WHERE id = :conversationId AND accountId = :accountId"
    )
    suspend fun updateConversationPreview(
        accountId: String,
        conversationId: String,
        lastMessage: String,
        updatedAt: Long,
        unreadCount: Int
    ): Int

    @Query(
        "UPDATE messages SET isRead = 1 " +
            "WHERE conversationId = :conversationId AND sentByCurrentUser = 0 " +
            "AND EXISTS (SELECT 1 FROM conversations " +
            "WHERE id = :conversationId AND accountId = :accountId)"
    )
    suspend fun markIncomingMessagesRead(accountId: String, conversationId: String): Int

    @Query(
        "UPDATE conversations SET unreadCount = 0 " +
            "WHERE id = :conversationId AND accountId = :accountId"
    )
    suspend fun clearUnreadCount(accountId: String, conversationId: String): Int

    @Query(
        "DELETE FROM conversations " +
            "WHERE id = :conversationId AND accountId = :accountId"
    )
    suspend fun deleteConversation(accountId: String, conversationId: String): Int

    @Query(
        "UPDATE conversations SET accountId = :accountId " +
            "WHERE accountId = '' OR accountId = 'legacy-local'"
    )
    suspend fun claimLegacyConversations(accountId: String): Int

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun conversationCount(): Int
}
