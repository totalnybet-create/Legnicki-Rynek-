package pl.legnickirynek.app.model

data class Category(
    val id: String,
    val name: String,
    val symbol: String
)

enum class ListingStatus {
    ACTIVE,
    RESERVED,
    SOLD,
    EXPIRED
}

data class Listing(
    val id: String,
    val title: String,
    val price: Int,
    val location: String,
    val categoryId: String,
    val description: String,
    val imageUris: List<String> = emptyList(),
    val ownerId: String = "",
    val sellerName: String = "Użytkownik",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val status: ListingStatus = ListingStatus.ACTIVE,
    val isFavorite: Boolean = false
)

data class LocalEvent(
    val id: String,
    val title: String,
    val date: String,
    val location: String,
    val description: String,
    val sourceUrl: String = ""
)

data class Conversation(
    val id: String,
    val person: String,
    val listingId: String?,
    val listingTitle: String,
    val lastMessage: String,
    val updatedAt: Long,
    val unreadCount: Int = 0
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderName: String,
    val body: String,
    val sentAt: Long,
    val sentByCurrentUser: Boolean,
    val isRead: Boolean = false
)

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val loggedIn: Boolean = false
)
