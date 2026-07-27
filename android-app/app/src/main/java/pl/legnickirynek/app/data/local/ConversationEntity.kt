package pl.legnickirynek.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.legnickirynek.app.model.Conversation

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["listingId"]),
        Index(value = ["updatedAt"]),
        Index(value = ["accountId", "updatedAt"])
    ]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(defaultValue = "''") val accountId: String,
    val person: String,
    val listingId: String?,
    val listingTitle: String,
    val lastMessage: String,
    val updatedAt: Long,
    val unreadCount: Int
)

fun ConversationEntity.toModel(): Conversation = Conversation(
    id = id,
    accountId = accountId,
    person = person,
    listingId = listingId,
    listingTitle = listingTitle,
    lastMessage = lastMessage,
    updatedAt = updatedAt,
    unreadCount = unreadCount
)

fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    accountId = accountId,
    person = person,
    listingId = listingId,
    listingTitle = listingTitle,
    lastMessage = lastMessage,
    updatedAt = updatedAt,
    unreadCount = unreadCount
)
