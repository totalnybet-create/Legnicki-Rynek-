package pl.legnickirynek.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pl.legnickirynek.app.data.LocalStore
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus
import pl.legnickirynek.app.model.UserProfile

data class AppUiState(
    val listings: List<Listing> = emptyList(),
    val profile: UserProfile = UserProfile()
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext

    private val _uiState = MutableStateFlow(
        AppUiState(
            listings = LocalStore.loadListings(appContext)
                .ifEmpty { SampleData.listings },
            profile = LocalStore.loadProfile(appContext)
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun addListing(listing: Listing) {
        val sellerName = _uiState.value.profile
            .takeIf(UserProfile::loggedIn)
            ?.name
            .orEmpty()
            .ifBlank { listing.sellerName }

        updateListings { current ->
            listOf(listing.copy(sellerName = sellerName)) + current
        }
    }

    fun updateListing(listing: Listing) {
        updateListings { current ->
            current.map { existing ->
                if (existing.id == listing.id) {
                    listing.copy(
                        createdAt = existing.createdAt,
                        updatedAt = System.currentTimeMillis(),
                        isFavorite = existing.isFavorite
                    )
                } else {
                    existing
                }
            }
        }
    }

    fun deleteListing(id: String) {
        updateListings { current -> current.filterNot { it.id == id } }
    }

    fun toggleFavorite(id: String) {
        updateListings { current ->
            current.map { listing ->
                if (listing.id == id) {
                    listing.copy(isFavorite = !listing.isFavorite)
                } else {
                    listing
                }
            }
        }
    }

    fun updateListingStatus(id: String, status: ListingStatus) {
        updateListings { current ->
            current.map { listing ->
                if (listing.id == id) {
                    listing.copy(
                        status = status,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    listing
                }
            }
        }
    }

    fun login(name: String, email: String) {
        val profile = UserProfile(
            name = name,
            email = email,
            loggedIn = true
        )
        _uiState.update { it.copy(profile = profile) }
        LocalStore.saveProfile(appContext, profile)
    }

    fun logout() {
        val profile = UserProfile()
        _uiState.update { it.copy(profile = profile) }
        LocalStore.saveProfile(appContext, profile)
    }

    private fun updateListings(transform: (List<Listing>) -> List<Listing>) {
        val listings = transform(_uiState.value.listings)
        _uiState.update { it.copy(listings = listings) }
        LocalStore.saveListings(appContext, listings)
    }
}
