package pl.legnickirynek.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pl.legnickirynek.app.data.LocalStore
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.domain.ListingOperations
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
            .takeIf { it.loggedIn }
            ?.name
            .orEmpty()
            .ifBlank { listing.sellerName }

        updateListings { current ->
            ListingOperations.add(current, listing, sellerName)
        }
    }

    fun updateListing(listing: Listing) {
        updateListings { current ->
            ListingOperations.update(
                listings = current,
                listing = listing,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun deleteListing(id: String) {
        updateListings { current -> ListingOperations.delete(current, id) }
    }

    fun toggleFavorite(id: String) {
        updateListings { current -> ListingOperations.toggleFavorite(current, id) }
    }

    fun updateListingStatus(id: String, status: ListingStatus) {
        updateListings { current ->
            ListingOperations.updateStatus(
                listings = current,
                id = id,
                status = status,
                updatedAt = System.currentTimeMillis()
            )
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
