package pl.legnickirynek.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.legnickirynek.app.data.ListingRepository
import pl.legnickirynek.app.data.LocalStore
import pl.legnickirynek.app.data.MessageRepository
import pl.legnickirynek.app.data.OfflineListingRepository
import pl.legnickirynek.app.data.OfflineMessageRepository
import pl.legnickirynek.app.data.ProfilePreferencesStore
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.data.local.AppDatabase
import pl.legnickirynek.app.domain.ListingOperations
import pl.legnickirynek.app.model.ChatMessage
import pl.legnickirynek.app.model.Conversation
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus
import pl.legnickirynek.app.model.UserProfile

data class AppUiState(
    val listings: List<Listing> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val profile: UserProfile = UserProfile(),
    val dataError: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val initialListings = LocalStore.loadListings(appContext)
        .ifEmpty { SampleData.listings }
    private val initialProfile = LocalStore.loadProfile(appContext)
    private val listingRepository: ListingRepository = OfflineListingRepository(
        database.listingDao()
    )
    private val messageRepository: MessageRepository = OfflineMessageRepository(database)
    private val profileStore = ProfilePreferencesStore(appContext)

    private val _uiState = MutableStateFlow(
        AppUiState(
            listings = initialListings,
            conversations = SampleData.conversations,
            profile = initialProfile
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            migrateLegacyData()

            combine(
                listingRepository.observeListings(),
                messageRepository.observeConversations(),
                profileStore.profile
            ) { listings, conversations, profile ->
                AppUiState(
                    listings = listings,
                    conversations = conversations,
                    profile = profile
                )
            }.collect { state ->
                _uiState.update { current ->
                    state.copy(dataError = current.dataError)
                }
            }
        }
    }

    fun observeConversation(conversationId: String): Flow<Conversation?> =
        messageRepository.observeConversation(conversationId)

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        messageRepository.observeMessages(conversationId)

    fun addListing(listing: Listing) {
        val currentListings = _uiState.value.listings
        val sellerName = _uiState.value.profile
            .takeIf { it.loggedIn }
            ?.name
            .orEmpty()
            .ifBlank { listing.sellerName }
        val newListings = ListingOperations.add(
            listings = currentListings,
            listing = listing,
            sellerName = sellerName
        )
        val listingToInsert = newListings.first()

        applyListingChange(newListings) {
            listingRepository.upsert(listingToInsert)
        }
    }

    fun updateListing(listing: Listing) {
        val newListings = ListingOperations.update(
            listings = _uiState.value.listings,
            listing = listing,
            updatedAt = System.currentTimeMillis()
        )
        val updatedListing = newListings.firstOrNull { it.id == listing.id } ?: return

        applyListingChange(newListings) {
            listingRepository.upsert(updatedListing)
        }
    }

    fun deleteListing(id: String) {
        val newListings = ListingOperations.delete(
            listings = _uiState.value.listings,
            id = id
        )

        applyListingChange(newListings) {
            listingRepository.delete(id)
        }
    }

    fun toggleFavorite(id: String) {
        val newListings = ListingOperations.toggleFavorite(
            listings = _uiState.value.listings,
            id = id
        )
        val updatedListing = newListings.firstOrNull { it.id == id } ?: return

        applyListingChange(newListings) {
            listingRepository.upsert(updatedListing)
        }
    }

    fun updateListingStatus(id: String, status: ListingStatus) {
        val newListings = ListingOperations.updateStatus(
            listings = _uiState.value.listings,
            id = id,
            status = status,
            updatedAt = System.currentTimeMillis()
        )
        val updatedListing = newListings.firstOrNull { it.id == id } ?: return

        applyListingChange(newListings) {
            listingRepository.upsert(updatedListing)
        }
    }

    fun ensureConversation(listing: Listing): String {
        val identity = _uiState.value.profile.email.ifBlank { "local-user" }
        val conversationId = "conversation-${listing.id}-${stableId(identity)}"
        val existing = _uiState.value.conversations.firstOrNull { it.id == conversationId }
        if (existing != null) return existing.id

        val now = System.currentTimeMillis()
        val conversation = Conversation(
            id = conversationId,
            person = listing.sellerName,
            listingId = listing.id,
            listingTitle = listing.title,
            lastMessage = "Rozpoczęto rozmowę",
            updatedAt = now,
            unreadCount = 0
        )
        val conversations = listOf(conversation) + _uiState.value.conversations

        applyConversationChange(conversations) {
            messageRepository.upsertConversation(conversation)
        }
        return conversationId
    }

    fun sendMessage(conversationId: String, body: String) {
        val cleanBody = body.trim().take(2000)
        if (cleanBody.isBlank()) return

        val conversation = _uiState.value.conversations
            .firstOrNull { it.id == conversationId }
            ?: return
        val now = System.currentTimeMillis()
        val updatedConversation = conversation.copy(
            lastMessage = cleanBody,
            updatedAt = now,
            unreadCount = 0
        )
        val updatedConversations = listOf(updatedConversation) +
            _uiState.value.conversations.filterNot { it.id == conversationId }
        val message = ChatMessage(
            id = "message-$now-${stableId(cleanBody)}",
            conversationId = conversationId,
            senderName = _uiState.value.profile.name.ifBlank { "Ty" },
            body = cleanBody,
            sentAt = now,
            sentByCurrentUser = true,
            isRead = true
        )

        applyConversationChange(updatedConversations) {
            messageRepository.sendMessage(updatedConversation, message)
        }
    }

    fun markConversationRead(conversationId: String) {
        val updatedConversations = _uiState.value.conversations.map { conversation ->
            if (conversation.id == conversationId) {
                conversation.copy(unreadCount = 0)
            } else {
                conversation
            }
        }

        applyConversationChange(updatedConversations) {
            messageRepository.markConversationRead(conversationId)
        }
    }

    fun deleteConversation(conversationId: String) {
        val updatedConversations = _uiState.value.conversations
            .filterNot { it.id == conversationId }

        applyConversationChange(updatedConversations) {
            messageRepository.deleteConversation(conversationId)
        }
    }

    fun login(name: String, email: String) {
        val profile = UserProfile(
            name = name,
            email = email,
            loggedIn = true
        )
        applyProfileChange(profile)
    }

    fun logout() {
        applyProfileChange(UserProfile())
    }

    fun clearDataError() {
        _uiState.update { it.copy(dataError = null) }
    }

    private suspend fun migrateLegacyData() {
        runCatching {
            profileStore.migrateFromLegacy(initialProfile)

            if (!LocalStore.isListingMigrationComplete(appContext)) {
                if (listingRepository.count() == 0) {
                    listingRepository.upsertAll(initialListings)
                }
                LocalStore.markListingMigrationComplete(appContext)
            }

            if (!LocalStore.isMessageInitializationComplete(appContext)) {
                if (messageRepository.conversationCount() == 0) {
                    messageRepository.seed(
                        conversations = SampleData.conversations,
                        messages = SampleData.messages
                    )
                }
                LocalStore.markMessageInitializationComplete(appContext)
            }
        }.onFailure { error ->
            _uiState.update {
                it.copy(dataError = error.message ?: "Nie udało się przenieść danych lokalnych.")
            }
        }
    }

    private fun applyListingChange(
        newListings: List<Listing>,
        persist: suspend () -> Unit
    ) {
        val previousListings = _uiState.value.listings
        _uiState.update {
            it.copy(
                listings = newListings,
                dataError = null
            )
        }

        viewModelScope.launch {
            runCatching { persist() }
                .onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            listings = if (current.listings == newListings) {
                                previousListings
                            } else {
                                current.listings
                            },
                            dataError = error.message ?: "Nie udało się zapisać ogłoszenia."
                        )
                    }
                }
        }
    }

    private fun applyConversationChange(
        newConversations: List<Conversation>,
        persist: suspend () -> Unit
    ) {
        val previousConversations = _uiState.value.conversations
        _uiState.update {
            it.copy(
                conversations = newConversations,
                dataError = null
            )
        }

        viewModelScope.launch {
            runCatching { persist() }
                .onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            conversations = if (current.conversations == newConversations) {
                                previousConversations
                            } else {
                                current.conversations
                            },
                            dataError = error.message ?: "Nie udało się zapisać wiadomości."
                        )
                    }
                }
        }
    }

    private fun applyProfileChange(profile: UserProfile) {
        val previousProfile = _uiState.value.profile
        _uiState.update {
            it.copy(
                profile = profile,
                dataError = null
            )
        }

        viewModelScope.launch {
            runCatching { profileStore.save(profile) }
                .onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            profile = if (current.profile == profile) {
                                previousProfile
                            } else {
                                current.profile
                            },
                            dataError = error.message ?: "Nie udało się zapisać profilu."
                        )
                    }
                }
        }
    }

    private fun stableId(value: String): String = value.hashCode()
        .toString()
        .replace('-', 'n')
}
