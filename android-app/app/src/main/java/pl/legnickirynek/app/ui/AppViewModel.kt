package pl.legnickirynek.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.legnickirynek.app.BuildConfig
import pl.legnickirynek.app.data.GeocodingRepository
import pl.legnickirynek.app.data.LegnicaCalendarEventsRepository
import pl.legnickirynek.app.data.LegnicaRssNewsRepository
import pl.legnickirynek.app.data.ListingRepository
import pl.legnickirynek.app.data.ListingSyncStore
import pl.legnickirynek.app.data.LocalEventsRepository
import pl.legnickirynek.app.data.LocalNewsRepository
import pl.legnickirynek.app.data.LocalStore
import pl.legnickirynek.app.data.MessageRepository
import pl.legnickirynek.app.data.NominatimGeocodingRepository
import pl.legnickirynek.app.data.OfflineListingRepository
import pl.legnickirynek.app.data.OfflineMessageRepository
import pl.legnickirynek.app.data.OpenMeteoWeatherRepository
import pl.legnickirynek.app.data.ProfilePreferencesStore
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.data.SyncingListingRepository
import pl.legnickirynek.app.data.WeatherRepository
import pl.legnickirynek.app.data.local.AppDatabase
import pl.legnickirynek.app.data.remote.JsonHttpClient
import pl.legnickirynek.app.data.remote.RestRemoteListingService
import pl.legnickirynek.app.data.remote.UrlConnectionTextHttpClient
import pl.legnickirynek.app.domain.ListingAccessPolicy
import pl.legnickirynek.app.domain.ListingOperations
import pl.legnickirynek.app.domain.ListingValidator
import pl.legnickirynek.app.domain.UserIdentity
import pl.legnickirynek.app.model.ChatMessage
import pl.legnickirynek.app.model.Conversation
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus
import pl.legnickirynek.app.model.LocalEvent
import pl.legnickirynek.app.model.LocalNewsItem
import pl.legnickirynek.app.model.UserProfile
import pl.legnickirynek.app.model.WeatherSnapshot

data class AppUiState(
    val listings: List<Listing> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val profile: UserProfile = UserProfile(),
    val weather: WeatherSnapshot? = null,
    val events: List<LocalEvent> = SampleData.events,
    val localNews: List<LocalNewsItem> = emptyList(),
    val localDataLoading: Boolean = false,
    val localDataError: String? = null,
    val dataError: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val initialListings = LocalStore.loadListings(appContext)
        .ifEmpty { SampleData.listings }
    private val initialProfile = LocalStore.loadProfile(appContext)
    private val offlineListingRepository = OfflineListingRepository(database.listingDao())
    private val listingRepository: ListingRepository = SyncingListingRepository(
        localRepository = offlineListingRepository,
        remoteService = RestRemoteListingService(
            baseUrl = BuildConfig.LISTINGS_API_BASE_URL,
            bearerToken = BuildConfig.LISTINGS_API_TOKEN,
            httpClient = JsonHttpClient()
        ),
        syncStore = ListingSyncStore(appContext)
    )
    private val messageRepository: MessageRepository = OfflineMessageRepository(database)
    private val profileStore = ProfilePreferencesStore(appContext)
    private val textHttpClient = UrlConnectionTextHttpClient()
    private val weatherRepository: WeatherRepository =
        OpenMeteoWeatherRepository(textHttpClient)
    private val localNewsRepository: LocalNewsRepository =
        LegnicaRssNewsRepository(textHttpClient)
    private val localEventsRepository: LocalEventsRepository =
        LegnicaCalendarEventsRepository(textHttpClient)
    private val geocodingRepository: GeocodingRepository =
        NominatimGeocodingRepository(textHttpClient)

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
            refreshLocalData()
        }
        viewModelScope.launch {
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
                    state.copy(
                        weather = current.weather,
                        events = current.events,
                        localNews = current.localNews,
                        localDataLoading = current.localDataLoading,
                        localDataError = current.localDataError,
                        dataError = current.dataError
                    )
                }
            }
        }
    }

    fun observeConversation(conversationId: String): Flow<Conversation?> =
        messageRepository.observeConversation(conversationId)

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        messageRepository.observeMessages(conversationId)

    fun refreshLocalData() {
        if (_uiState.value.localDataLoading) return

        _uiState.update {
            it.copy(
                localDataLoading = true,
                localDataError = null
            )
        }

        viewModelScope.launch {
            val weatherDeferred = async {
                runCatching { weatherRepository.getCurrentWeather() }
            }
            val newsDeferred = async {
                runCatching { localNewsRepository.getLatestNews() }
            }
            val eventsDeferred = async {
                runCatching { localEventsRepository.getUpcomingEvents() }
            }
            val listingSyncDeferred = async {
                runCatching { listingRepository.synchronize() }
            }
            val weatherResult = weatherDeferred.await()
            val newsResult = newsDeferred.await()
            val eventsResult = eventsDeferred.await()
            val listingSyncResult = listingSyncDeferred.await()
            val syncReport = listingSyncResult.getOrNull()
            val errors = buildList {
                weatherResult.exceptionOrNull()?.message?.let(::add)
                newsResult.exceptionOrNull()?.message?.let(::add)
                eventsResult.exceptionOrNull()?.message?.let(::add)
                listingSyncResult.exceptionOrNull()?.message?.let(::add)
                syncReport?.errors?.let(::addAll)
            }.distinct()

            _uiState.update { current ->
                current.copy(
                    weather = weatherResult.getOrNull() ?: current.weather,
                    events = eventsResult.getOrNull()
                        ?.takeIf { it.isNotEmpty() }
                        ?: current.events,
                    localNews = newsResult.getOrNull() ?: current.localNews,
                    localDataLoading = false,
                    localDataError = errors.takeIf { it.isNotEmpty() }
                        ?.joinToString(" ")
                )
            }
        }
    }

    fun addListing(listing: Listing) {
        if (!validateListing(listing)) return

        val currentListings = _uiState.value.listings
        val profile = _uiState.value.profile
        val ownerId = ListingAccessPolicy.ownerIdFor(profile)
        val sellerName = ListingAccessPolicy.sellerNameFor(profile, listing.sellerName)

        val newListings = ListingOperations.add(
            listings = currentListings,
            listing = listing.copy(latitude = null, longitude = null),
            ownerId = ownerId,
            sellerName = sellerName
        )
        val listingToInsert = newListings.first()

        applyListingChange(newListings) {
            listingRepository.upsert(listingToInsert)
        }
        geocodeListing(listingToInsert.id, listingToInsert.location)
    }

    fun updateListing(listing: Listing) {
        val existingListing = _uiState.value.listings.firstOrNull { it.id == listing.id }
            ?: run {
                setDataError("Nie znaleziono ogłoszenia do edycji.")
                return
            }
        if (!ListingAccessPolicy.canManage(existingListing, _uiState.value.profile)) {
            setDataError("Nie masz uprawnień do edycji tego ogłoszenia.")
            return
        }

        val locationChanged = !existingListing.location.equals(
            listing.location,
            ignoreCase = true
        )
        val listingForUpdate = if (locationChanged) {
            listing.copy(latitude = null, longitude = null)
        } else {
            listing.copy(
                latitude = existingListing.latitude,
                longitude = existingListing.longitude
            )
        }
        if (!validateListing(listingForUpdate)) return

        val newListings = ListingOperations.update(
            listings = _uiState.value.listings,
            listing = listingForUpdate,
            updatedAt = System.currentTimeMillis()
        )
        val updatedListing = newListings.firstOrNull { it.id == listing.id }
            ?: run {
                setDataError("Nie udało się przygotować zmian ogłoszenia.")
                return
            }

        applyListingChange(newListings) {
            listingRepository.upsert(updatedListing)
        }
        if (locationChanged || !updatedListing.hasCoordinates) {
            geocodeListing(updatedListing.id, updatedListing.location)
        }
    }

    fun deleteListing(id: String) {
        val existingListing = _uiState.value.listings.firstOrNull { it.id == id }
            ?: run {
                setDataError("Nie znaleziono ogłoszenia do usunięcia.")
                return
            }
        if (!ListingAccessPolicy.canManage(existingListing, _uiState.value.profile)) {
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
        if (_uiState.value.listings.none { it.id == id }) {
            setDataError("Nie znaleziono ogłoszenia.")
            return
        }

        val newListings = ListingOperations.toggleFavorite(
            listings = _uiState.value.listings,
            id = id
        )
        val updatedListing = newListings.firstOrNull { it.id == id }
            ?: run {
                setDataError("Nie udało się zmienić stanu ulubionych.")
                return
            }

        applyListingChange(newListings) {
            listingRepository.upsert(updatedListing)
        }
    }

    fun updateListingStatus(id: String, status: ListingStatus) {
        val existingListing = _uiState.value.listings.firstOrNull { it.id == id }
            ?: run {
                setDataError("Nie znaleziono ogłoszenia.")
                return
            }
        if (!ListingAccessPolicy.canManage(existingListing, _uiState.value.profile)) {
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
            ?: run {
                setDataError("Nie udało się zmienić statusu ogłoszenia.")
                return
            }

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
        val cleanEmail = email.trim().lowercase()
        val profile = UserProfile(
            id = UserIdentity.fromEmail(cleanEmail),
            name = name.trim(),
            email = cleanEmail,
            loggedIn = true
        )
        applyProfileChange(profile)

        viewModelScope.launch {
            runCatching {
                listingRepository.claimLegacyListings(
                    ownerId = ListingAccessPolicy.ownerIdFor(profile),
                    sellerName = ListingAccessPolicy.sellerNameFor(profile, "Użytkownik")
                )
                listingRepository.synchronize()
            }.onFailure { error ->
                setDataError(
                    error.message
                        ?: "Nie udało się przypisać lokalnych ogłoszeń do konta."
                )
            }
        }
    }

    fun logout() {
        applyProfileChange(UserProfile())
    }

    fun clearDataError() {
        _uiState.update { it.copy(dataError = null) }
    }

    private fun geocodeListing(listingId: String, location: String) {
        val requestedLocation = location.trim()
        if (requestedLocation.isBlank()) return

        viewModelScope.launch {
            val point = runCatching {
                geocodingRepository.geocode(requestedLocation)
            }.getOrNull() ?: return@launch

            val currentListing = _uiState.value.listings.firstOrNull { it.id == listingId }
                ?: return@launch
            if (!currentListing.location.equals(requestedLocation, ignoreCase = true)) {
                return@launch
            }

            val geocodedListing = currentListing.copy(
                latitude = point.latitude,
                longitude = point.longitude
            )
            val geocodedListings = _uiState.value.listings.map { existing ->
                if (existing.id == listingId) geocodedListing else existing
            }
            applyListingChange(geocodedListings) {
                listingRepository.upsert(geocodedListing)
            }
        }
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
