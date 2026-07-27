package pl.legnickirynek.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.legnickirynek.app.model.Conversation

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["listingId"]),
        Index(value = ["updatedAt"])
    ]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val person: String,
    val listingId: String?,
    val listingTitle: String,
    val lastMessage: String,
    val updatedAt: Long,
    val unreadCount: Int
)

fun ConversationEntity.toModel(): Conversation = Conversation(
    id = id,
    person = person,
    listingId = listingId,
    listingTitle = listingTitle,
    lastMessage = lastMessage,
    updatedAt = updatedAt,
    unreadCount = unreadCount
)

fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    person = person,
    listingId = listingId,
    listingTitle = listingTitle,
    lastMessage = lastMessage,
    updatedAt = updatedAt,
    unreadCount = unreadCount
)
