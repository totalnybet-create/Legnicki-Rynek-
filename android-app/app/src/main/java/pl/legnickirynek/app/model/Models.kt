package pl.legnickirynek.app.model

data class Category(
    val id: String,
    val name: String,
    val symbol: String
)

data class Listing(
    val id: String,
    val title: String,
    val price: Int,
    val location: String,
    val categoryId: String,
    val description: String,
    val isFavorite: Boolean = false
)

data class LocalEvent(
    val id: String,
    val title: String,
    val date: String,
    val location: String,
    val description: String
)

data class Conversation(
    val id: String,
    val person: String,
    val listingTitle: String,
    val lastMessage: String,
    val time: String,
    val unread: Boolean
)

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val loggedIn: Boolean = false
)
