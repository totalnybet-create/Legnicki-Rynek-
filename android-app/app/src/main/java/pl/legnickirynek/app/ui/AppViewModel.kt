package pl.legnickirynek.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.legnickirynek.app.data.ListingRepository
import pl.legnickirynek.app.data.LocalStore
import pl.legnickirynek.app.data.OfflineListingRepository
import pl.legnickirynek.app.data.ProfilePreferencesStore
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.data.local.AppDatabase
import pl.legnickirynek.app.domain.ListingOperations
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus
import pl.legnickirynek.app.model.UserProfile

data class AppUiState(
    val listings: List<Listing> = emptyList(),
    val profile: UserProfile = UserProfile(),
    val dataError: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val initialListings = LocalStore.loadListings(appContext)
        .ifEmpty { SampleData.listings }
    private val initialProfile = LocalStore.loadProfile(appContext)
    private val listingRepository: ListingRepository = OfflineListingRepository(
        AppDatabase.getInstance(appContext).listingDao()
    )
    private val profileStore = ProfilePreferencesStore(appContext)

    private val _uiState = MutableStateFlow(
        AppUiState(
            listings = initialListings,
            profile = initialProfile
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            migrateLegacyData()

            combine(
                listingRepository.observeListings(),
                profileStore.profile
            ) { listings, profile ->
                AppUiState(
                    listings = listings,
                    profile = profile
                )
            }.collect { state ->
                _uiState.update { current ->
                    state.copy(dataError = current.dataError)
                }
            }
        }
    }

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
}
