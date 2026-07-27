package pl.legnickirynek.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.legnickirynek.app.model.ChatMessage

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["sentAt"])
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderName: String,
    val body: String,
    val sentAt: Long,
    val sentByCurrentUser: Boolean,
    val isRead: Boolean
)

fun MessageEntity.toModel(): ChatMessage = ChatMessage(
    id = id,
    conversationId = conversationId,
    senderName = senderName,
    body = body,
    sentAt = sentAt,
    sentByCurrentUser = sentByCurrentUser,
    isRead = isRead
)

fun ChatMessage.toEntity(): MessageEntity = MessageEntity(
    id = id,
    conversationId = conversationId,
    senderName = senderName,
    body = body,
    sentAt = sentAt,
    sentByCurrentUser = sentByCurrentUser,
    isRead = isRead
)
