package pl.legnickirynek.app

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.legnickirynek.app.data.AuthSessionStore
import pl.legnickirynek.app.data.ProfilePreferencesStore
import pl.legnickirynek.app.data.SecureAuthSessionStore
import pl.legnickirynek.app.model.UserProfile

class LegnickiRynekApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AppServices.initialize(this)
        applicationScope.launch {
            val profileStore = ProfilePreferencesStore(this@LegnickiRynekApplication)
            val profile = profileStore.profile.first()
            if (profile.remoteSession && AppServices.authSessionStore.load()?.isValid != true) {
                profileStore.save(UserProfile())
            }
        }
    }
}

object AppServices {
    @Volatile
    private var initialized = false
    private var sessionStore: AuthSessionStore? = null

    val authSessionStore: AuthSessionStore
        get() = sessionStore ?: error("AppServices nie zostały zainicjalizowane.")

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            sessionStore = SecureAuthSessionStore(context.applicationContext)
            initialized = true
        }
    }

    suspend fun currentAccessToken(): String =
        sessionStore?.accessToken().orEmpty()
}
