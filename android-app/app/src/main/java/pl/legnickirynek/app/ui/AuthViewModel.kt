package pl.legnickirynek.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.legnickirynek.app.AppServices
import pl.legnickirynek.app.BuildConfig
import pl.legnickirynek.app.data.AuthRepository
import pl.legnickirynek.app.data.ListingRepository
import pl.legnickirynek.app.data.ListingSyncStore
import pl.legnickirynek.app.data.OfflineListingRepository
import pl.legnickirynek.app.data.ProfilePreferencesStore
import pl.legnickirynek.app.data.RestAuthRepository
import pl.legnickirynek.app.data.SyncingListingRepository
import pl.legnickirynek.app.data.local.AppDatabase
import pl.legnickirynek.app.data.remote.JsonHttpClient
import pl.legnickirynek.app.data.remote.RestRemoteListingService
import pl.legnickirynek.app.domain.ListingAccessPolicy
import pl.legnickirynek.app.domain.UserIdentity
import pl.legnickirynek.app.model.AuthMode
import pl.legnickirynek.app.model.UserProfile

data class AuthUiState(
    val apiAvailable: Boolean = false,
    val inProgress: Boolean = false,
    val error: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val jsonHttpClient = JsonHttpClient()
    private val profileStore = ProfilePreferencesStore(appContext)
    private val authRepository: AuthRepository
    private val listingRepository: ListingRepository

    private val _uiState: MutableStateFlow<AuthUiState>
    val uiState: StateFlow<AuthUiState>

    init {
        AppServices.initialize(appContext)
        authRepository = RestAuthRepository(
            baseUrl = BuildConfig.LISTINGS_API_BASE_URL,
            httpClient = jsonHttpClient,
            sessionStore = AppServices.authSessionStore,
            buildToken = BuildConfig.LISTINGS_API_TOKEN
        )
        listingRepository = SyncingListingRepository(
            localRepository = OfflineListingRepository(
                AppDatabase.getInstance(appContext).listingDao()
            ),
            remoteService = RestRemoteListingService(
                baseUrl = BuildConfig.LISTINGS_API_BASE_URL,
                bearerToken = BuildConfig.LISTINGS_API_TOKEN,
                httpClient = jsonHttpClient
            ),
            syncStore = ListingSyncStore(appContext)
        )
        _uiState = MutableStateFlow(
            AuthUiState(apiAvailable = authRepository.isConfigured)
        )
        uiState = _uiState.asStateFlow()
    }

    fun authenticate(
        name: String,
        email: String,
        password: String,
        mode: AuthMode
    ) {
        if (_uiState.value.inProgress) return
        _uiState.update { it.copy(inProgress = true, error = null) }

        viewModelScope.launch {
            runCatching {
                val profile = when (mode) {
                    AuthMode.LOGIN -> {
                        require(authRepository.isConfigured) {
                            "Serwer logowania nie jest skonfigurowany."
                        }
                        authRepository.login(email = email, password = password).profile
                    }

                    AuthMode.REGISTER -> {
                        require(authRepository.isConfigured) {
                            "Serwer rejestracji nie jest skonfigurowany."
                        }
                        authRepository.register(
                            name = name,
                            email = email,
                            password = password
                        ).profile
                    }

                    AuthMode.LOCAL -> createLocalProfile(name, email)
                }

                if (mode == AuthMode.LOCAL) {
                    AppServices.authSessionStore.clear()
                }
                profileStore.save(profile)
                listingRepository.claimLegacyListings(
                    ownerId = ListingAccessPolicy.ownerIdFor(profile),
                    sellerName = ListingAccessPolicy.sellerNameFor(profile, "Użytkownik")
                )
                listingRepository.synchronize()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        error = error.message ?: "Nie udało się zalogować."
                    )
                }
            }
            _uiState.update { it.copy(inProgress = false) }
        }
    }

    fun logout(remoteSession: Boolean) {
        if (_uiState.value.inProgress) return
        _uiState.update { it.copy(inProgress = true, error = null) }

        viewModelScope.launch {
            runCatching {
                if (remoteSession) {
                    authRepository.logout()
                } else {
                    AppServices.authSessionStore.clear()
                }
                profileStore.save(UserProfile())
            }.onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: "Nie udało się wylogować.")
                }
            }
            _uiState.update { it.copy(inProgress = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun createLocalProfile(name: String, email: String): UserProfile {
        val cleanName = name.trim().take(80)
        require(cleanName.length >= 2) { "Imię musi mieć co najmniej 2 znaki." }
        val cleanEmail = email.trim().lowercase(Locale.ROOT).take(160)
        require(EMAIL_PATTERN.matches(cleanEmail)) { "Podaj prawidłowy adres e-mail." }

        return UserProfile(
            id = UserIdentity.fromEmail(cleanEmail),
            name = cleanName,
            email = cleanEmail,
            loggedIn = true,
            remoteSession = false
        )
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
