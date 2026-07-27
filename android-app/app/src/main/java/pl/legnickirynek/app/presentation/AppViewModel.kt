package pl.legnickirynek.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.legnickirynek.app.domain.usecase.AddListingUseCase
import pl.legnickirynek.app.domain.usecase.InitializeAppUseCase
import pl.legnickirynek.app.domain.usecase.LogoutUseCase
import pl.legnickirynek.app.domain.usecase.ObserveListingsUseCase
import pl.legnickirynek.app.domain.usecase.ObserveProfileUseCase
import pl.legnickirynek.app.domain.usecase.ToggleFavoriteUseCase
import pl.legnickirynek.app.domain.usecase.UpdateProfileUseCase
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.UserProfile

data class AppUiState(
    val isLoading: Boolean = true,
    val listings: List<Listing> = emptyList(),
    val profile: UserProfile = UserProfile(),
    val errorMessage: String? = null
)

@HiltViewModel
class AppViewModel @Inject constructor(
    observeListings: ObserveListingsUseCase,
    observeProfile: ObserveProfileUseCase,
    private val initializeApp: InitializeAppUseCase,
    private val addListingUseCase: AddListingUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val initializationState = MutableStateFlow<InitializationState>(InitializationState.Loading)
    private val actionError = MutableStateFlow<String?>(null)

    val uiState = combine(
        observeListings(),
        observeProfile(),
        initializationState,
        actionError
    ) { listings, profile, initialization, currentActionError ->
        AppUiState(
            isLoading = initialization is InitializationState.Loading,
            listings = listings,
            profile = profile,
            errorMessage = currentActionError ?: (initialization as? InitializationState.Failed)?.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = AppUiState()
    )

    init {
        viewModelScope.launch {
            try {
                initializeApp()
                initializationState.value = InitializationState.Ready
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                initializationState.value = InitializationState.Failed(
                    "Nie udało się przygotować lokalnych danych aplikacji."
                )
            }
        }
    }

    fun addListing(listing: Listing) {
        launchAction("Nie udało się zapisać ogłoszenia.") {
            addListingUseCase(listing)
        }
    }

    fun toggleFavorite(listingId: String) {
        launchAction("Nie udało się zmienić ulubionych.") {
            toggleFavoriteUseCase(listingId)
        }
    }

    fun login(name: String, email: String) {
        launchAction("Nie udało się zapisać profilu.") {
            updateProfileUseCase(
                UserProfile(name = name, email = email, loggedIn = true)
            )
        }
    }

    fun logout() {
        launchAction("Nie udało się wylogować.") {
            logoutUseCase()
        }
    }

    fun clearActionError() {
        actionError.value = null
    }

    private fun launchAction(
        errorMessage: String,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            actionError.value = null

            try {
                action()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                actionError.value = errorMessage
            }
        }
    }

    private sealed interface InitializationState {
        data object Loading : InitializationState
        data object Ready : InitializationState
        data class Failed(val message: String) : InitializationState
    }
}
