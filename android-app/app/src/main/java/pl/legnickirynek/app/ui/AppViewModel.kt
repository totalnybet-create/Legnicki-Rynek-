package pl.legnickirynek.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.legnickirynek.app.data.FavoriteRepository
import pl.legnickirynek.app.data.ListingRepository
import pl.legnickirynek.app.data.LocalStore
import pl.legnickirynek.app.data.MessageRepository
import pl.legnickirynek.app.data.OfflineFavoriteRepository
import pl.legnickirynek.app.data.OfflineListingRepository
import pl.legnickirynek.app.data.OfflineMessageRepository
import pl.legnickirynek.app.data.ProfilePreferencesStore
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.data.local.AppDatabase
import pl.legnickirynek.app.domain.ListingAccessPolicy
import pl.legnickirynek.app.domain.ListingOperations
import pl.legnickirynek.app.domain.ListingValidator
import pl.legnickirynek.app.domain.UserIdentity
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

@OptIn(ExperimentalCoroutinesApi::class)
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
    private val favoriteRepository: FavoriteRepository = OfflineFavoriteRepository(
        database.favoriteDao()
    )
    private val profileStore = ProfilePreferencesStore(appContext)

    private val _uiState = MutableStateFlow(
        AppUiState(
            listings = initialListings.map { it.copy(isFavorite = false) },
            conversations = emptyList(),
            profile = initialProfile
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            migrateLegacyData()

            profileStore.profile
                .flatMapLatest { profile ->
                    val accountId = activeAccountId(profile)
                    val conversations = if (accountId.isBlank()) {
                        flowOf(emptyList<Conversation>())
                    } else {
                        messageRepository.observeConversations(accountId)
                    }
                    val favoriteIds = if (accountId.isBlank()) {
                        flowOf(emptyList<String>())
                    } else {
                        favoriteRepository.observeFavoriteListingIds(accountId)
                    }

                    combine(
                        listingRepository.observeListings(),
                        conversations,
                        favoriteIds
                    ) { listings, accountConversations, accountFavoriteIds ->
                        val favoriteSet = accountFavoriteIds.toHashSet()
                        AppUiState(
                            listings = listings.map { listing ->
                                listing.copy(isFavorite = listing.id in favoriteSet)
                            },
                            conversations = accountConversations,
                            profile = profile
                        )
                    }
                }
                .collect { state ->
                    _uiState.update { current ->
                        state.copy(dataError = current.dataError)
                    }
                }
        }
    }

    fun observeConversation(conversationId: String): Flow<Conversation?> =
        profileStore.profile.flatMapLatest { profile ->
            val accountId = activeAccountId(profile)
            if (accountId.isBlank() || conversationId.isBlank()) {
                flowOf(null)
            } else {
                messageRepository.observeConversation(accountId, conversationId)
            }
        }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        profileStore.profile.flatMapLatest { profile ->
            val accountId = activeAccountId(profile)
            if (accountId.isBlank() || conversationId.isBlank()) {
                flowOf(emptyList<ChatMessage>())
            } else {
                messageRepository.observeMessages(accountId, conversationId)
            }
        }

    fun addListing(listing: Listing) {
        val profile = _uiState.value.profile
        val ownerId = activeAccountId(profile)
        if (ownerId.isBlank()) {
            setDataError("Zaloguj się, aby dodać ogłoszenie.")
            return
        }
        if (!validateListing(listing)) return

        val newListings = ListingOperations.add(
            listings = _uiState.value.listings,
            listing = listing.copy(isFavorite = false),
            ownerId = ownerId,
            sellerName = profile.name.ifBlank { listing.sellerName }
        )
        val listingToInsert = newListings.first().copy(isFavorite = false)

        applyListingChange(newListings) {
            listingRepository.upsert(listingToInsert)
        }
    }

    fun updateListing(listing: Listing) {
        val currentListing = _uiState.value.listings.firstOrNull { it.id == listing.id }
        if (currentListing == null) {
            setDataError("Nie znaleziono ogłoszenia do edycji.")
            return
        }
        if (!ListingAccessPolicy.canManage(currentListing, _uiState.value.profile)) {
            setDataError("Nie masz uprawnień do edycji tego ogłoszenia.")
            return
        }
        if (!validateListing(listing)) return

        val securedListing = listing.copy(
            ownerId = currentListing.ownerId,
            sellerName = currentListing.sellerName,
            createdAt = currentListing.createdAt,
            isFavorite = false
        )
        val newListings = ListingOperations.update(
            listings = _uiState.value.listings,
            listing = securedListing,
            updatedAt = System.currentTimeMillis()
        ).map { updated ->
            if (updated.id == currentListing.id) {
                updated.copy(isFavorite = currentListing.isFavorite)
            } else {
                updated
            }
        }
        val updatedListing = newListings.firstOrNull { it.id == listing.id }
            ?.copy(isFavorite = false)
            ?: run {
                setDataError("Nie udało się przygotować zmian ogłoszenia.")
                return
            }

        applyListingChange(newListings) {
            listingRepository.upsert(updatedListing)
        }
    }

    fun deleteListing(id: String) {
        val listing = _uiState.value.listings.firstOrNull { it.id == id }
        if (listing == null) {
            setDataError("Nie znaleziono ogłoszenia do usunięcia.")
            return
        }
        if (!ListingAccessPolicy.canManage(listing, _uiState.value.profile)) {
            setDataError("Nie masz uprawnień do usunięcia tego ogłoszenia.")
            return
        }

        val newListings = ListingOperations.delete(
            listings = _uiState.value.listings,
            id = id
        )

        applyListingChange(newListings) {
            listingRepository.delete(id)
        }
    }

    fun toggleFavorite(id: String) {
        val accountId = activeAccountId(_uiState.value.profile)
        if (accountId.isBlank()) {
            setDataError("Zaloguj się, aby zapisywać ulubione ogłoszenia.")
            return
        }

        val listing = _uiState.value.listings.firstOrNull { it.id == id }
        if (listing == null) {
            setDataError("Nie znaleziono ogłoszenia.")
            return
        }

        val shouldBeFavorite = !listing.isFavorite
        val newListings = _uiState.value.listings.map { current ->
            if (current.id == id) {
                current.copy(isFavorite = shouldBeFavorite)
            } else {
                current
            }
        }

        applyListingChange(newListings) {
            favoriteRepository.setFavorite(
                accountId = accountId,
                listingId = id,
                favorite = shouldBeFavorite
            )
        }
    }

    fun updateListingStatus(id: String, status: ListingStatus) {
        val listing = _uiState.value.listings.firstOrNull { it.id == id }
        if (listing == null) {
            setDataError("Nie znaleziono ogłoszenia.")
            return
        }
        if (!ListingAccessPolicy.canManage(listing, _uiState.value.profile)) {
            setDataError("Nie masz uprawnień do zmiany statusu tego ogłoszenia.")
            return
        }

        val newListings = ListingOperations.updateStatus(
            listings = _uiState.value.listings,
            id = id,
            status = status,
            updatedAt = System.currentTimeMillis()
        )
        val updatedListing = newListings.firstOrNull { it.id == id }
            ?.copy(isFavorite = false)
            ?: run {
                setDataError("Nie udało się zmienić statusu ogłoszenia.")
                return
            }

        applyListingChange(newListings) {
            listingRepository.upsert(updatedListing)
        }
    }

    fun ensureConversation(listing: Listing): String? {
        val profile = _uiState.value.profile
        val accountId = activeAccountId(profile)
        if (accountId.isBlank()) {
            setDataError("Zaloguj się, aby wysłać wiadomość.")
            return null
        }
        if (ListingAccessPolicy.canManage(listing, profile)) {
            setDataError("Nie możesz rozpocząć rozmowy z własnym ogłoszeniem.")
            return null
        }

        val existing = _uiState.value.conversations.firstOrNull {
            it.accountId == accountId && it.listingId == listing.id
        }
        if (existing != null) return existing.id

        val now = System.currentTimeMillis()
        val conversation = Conversation(
            id = "conversation-${UUID.randomUUID()}",
            accountId = accountId,
            person = listing.sellerName,
            listingId = listing.id,
            listingTitle = listing.title,
            lastMessage = "Rozpoczęto rozmowę",
            updatedAt = now,
            unreadCount = 0
        )
        val conversations = listOf(conversation) + _uiState.value.conversations

        applyConversationChange(conversations) {
            messageRepository.upsertConversation(accountId, conversation)
        }
        return conversation.id
    }

    fun sendMessage(conversationId: String, body: String) {
        val accountId = activeAccountId(_uiState.value.profile)
        if (accountId.isBlank()) {
            setDataError("Zaloguj się, aby wysłać wiadomość.")
            return
        }

        val cleanBody = body.trim().take(MAX_MESSAGE_LENGTH)
        if (cleanBody.isBlank()) {
            setDataError("Treść wiadomości nie może być pusta.")
            return
        }

        val conversation = _uiState.value.conversations.firstOrNull {
            it.id == conversationId && it.accountId == accountId
        } ?: run {
            setDataError("Nie znaleziono rozmowy dla aktywnego konta.")
            return
        }

        val now = System.currentTimeMillis()
        val updatedConversation = conversation.copy(
            lastMessage = cleanBody,
            updatedAt = now,
            unreadCount = 0
        )
        val updatedConversations = listOf(updatedConversation) +
            _uiState.value.conversations.filterNot { it.id == conversationId }
        val message = ChatMessage(
            id = "message-${UUID.randomUUID()}",
            conversationId = conversationId,
            senderName = _uiState.value.profile.name.ifBlank { "Ty" },
            body = cleanBody,
            sentAt = now,
            sentByCurrentUser = true,
            isRead = true
        )

        applyConversationChange(updatedConversations) {
            messageRepository.sendMessage(accountId, updatedConversation, message)
        }
    }

    fun markConversationRead(conversationId: String) {
        val accountId = activeAccountId(_uiState.value.profile)
        if (accountId.isBlank()) return
        if (_uiState.value.conversations.none {
                it.id == conversationId && it.accountId == accountId
            }
        ) return

        val updatedConversations = _uiState.value.conversations.map { conversation ->
            if (conversation.id == conversationId) {
                conversation.copy(unreadCount = 0)
            } else {
                conversation
            }
        }

        applyConversationChange(updatedConversations) {
            messageRepository.markConversationRead(accountId, conversationId)
        }
    }

    fun deleteConversation(conversationId: String) {
        val accountId = activeAccountId(_uiState.value.profile)
        if (accountId.isBlank()) {
            setDataError("Zaloguj się, aby usunąć rozmowę.")
            return
        }
        if (_uiState.value.conversations.none {
                it.id == conversationId && it.accountId == accountId
            }
        ) {
            setDataError("Nie znaleziono rozmowy dla aktywnego konta.")
            return
        }

        val updatedConversations = _uiState.value.conversations
            .filterNot { it.id == conversationId }

        applyConversationChange(updatedConversations) {
            messageRepository.deleteConversation(accountId, conversationId)
        }
    }

    fun login(name: String, email: String) {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val accountId = UserIdentity.fromEmail(cleanEmail)

        if (cleanName.length < 2) {
            setDataError("Podaj prawidłową nazwę użytkownika.")
            return
        }
        if (accountId.isBlank() || '@' !in cleanEmail) {
            setDataError("Podaj prawidłowy adres e-mail.")
            return
        }

        val profile = UserProfile(
            id = accountId,
            name = cleanName,
            email = cleanEmail,
            loggedIn = true
        )
        applyProfileChange(profile) {
            listingRepository.claimLegacyListings(accountId, cleanName)
            messageRepository.claimLegacyConversations(accountId)
            favoriteRepository.claimLegacyFavorites(accountId)
            profileStore.save(profile)
        }
    }

    fun logout() {
        applyProfileChange(UserProfile()) {
            profileStore.save(UserProfile())
        }
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
                        accountId = LEGACY_LOCAL_ACCOUNT_ID,
                        conversations = SampleData.conversations,
                        messages = SampleData.messages
                    )
                }
                LocalStore.markMessageInitializationComplete(appContext)
            }
        }.onFailure { error ->
            setDataError(error.message ?: "Nie udało się przenieść danych lokalnych.")
        }
    }

    private fun validateListing(listing: Listing): Boolean {
        val validation = ListingValidator.validate(listing)
        if (validation.isValid) return true

        setDataError(
            validation.errors.values.firstOrNull()
                ?: "Dane ogłoszenia są nieprawidłowe."
        )
        return false
    }

    private fun activeAccountId(profile: UserProfile): String = profile
        .takeIf { it.loggedIn }
        ?.let { it.id.ifBlank { UserIdentity.fromEmail(it.email) } }
        .orEmpty()

    private fun setDataError(message: String) {
        _uiState.update { it.copy(dataError = message) }
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

    private fun applyProfileChange(
        profile: UserProfile,
        persist: suspend () -> Unit
    ) {
        val previousProfile = _uiState.value.profile
        val previousConversations = _uiState.value.conversations
        _uiState.update {
            it.copy(
                profile = profile,
                conversations = emptyList(),
                dataError = null
            )
        }

        viewModelScope.launch {
            runCatching { persist() }
                .onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            profile = if (current.profile == profile) {
                                previousProfile
                            } else {
                                current.profile
                            },
                            conversations = if (current.profile == profile) {
                                previousConversations
                            } else {
                                current.conversations
                            },
                            dataError = error.message ?: "Nie udało się zapisać profilu."
                        )
                    }
                }
        }
    }

    private companion object {
        const val MAX_MESSAGE_LENGTH = 2_000
        const val LEGACY_LOCAL_ACCOUNT_ID = "legacy-local"
    }
}
