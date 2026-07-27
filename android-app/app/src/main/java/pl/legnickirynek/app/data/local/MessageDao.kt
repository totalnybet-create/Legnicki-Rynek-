package pl.legnickirynek.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    fun observeConversation(conversationId: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY sentAt ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

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
            "WHERE id = :conversationId"
    )
    suspend fun updateConversationPreview(
        conversationId: String,
        lastMessage: String,
        updatedAt: Long,
        unreadCount: Int
    )

    @Query(
        "UPDATE messages SET isRead = 1 " +
            "WHERE conversationId = :conversationId AND sentByCurrentUser = 0"
    )
    suspend fun markIncomingMessagesRead(conversationId: String)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun clearUnreadCount(conversationId: String)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun conversationCount(): Int
}
