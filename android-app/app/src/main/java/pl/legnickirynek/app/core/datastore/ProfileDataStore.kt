package pl.legnickirynek.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import pl.legnickirynek.app.model.UserProfile

private const val PROFILE_DATASTORE_NAME = "profile"
private const val LEGACY_PREFERENCES_NAME = "legnicki_rynek_local_store"
private const val LEGACY_PROFILE_KEY = "profile"

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PROFILE_DATASTORE_NAME,
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context,
                LEGACY_PREFERENCES_NAME,
                setOf(LEGACY_PROFILE_KEY)
            )
        )
    }
)

@Singleton
class ProfileDataStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore = context.profileDataStore

    private val preferencesFlow = dataStore.data.catch { throwable ->
        if (throwable is IOException) {
            emit(emptyPreferences())
        } else {
            throw throwable
        }
    }

    val profile: Flow<UserProfile> = preferencesFlow.map { preferences ->
        val legacyProfile = preferences[Keys.LegacyProfile]
            ?.let(::parseLegacyProfile)

        UserProfile(
            name = preferences[Keys.Name] ?: legacyProfile?.name.orEmpty(),
            email = preferences[Keys.Email] ?: legacyProfile?.email.orEmpty(),
            loggedIn = preferences[Keys.LoggedIn] ?: legacyProfile?.loggedIn ?: false
        )
    }

    suspend fun saveProfile(profile: UserProfile) {
        dataStore.edit { preferences ->
            preferences[Keys.Name] = profile.name
            preferences[Keys.Email] = profile.email
            preferences[Keys.LoggedIn] = profile.loggedIn
            preferences.remove(Keys.LegacyProfile)
        }
    }

    suspend fun isLegacyListingsMigrated(): Boolean =
        preferencesFlow.first()[Keys.LegacyListingsMigrated] ?: false

    suspend fun markLegacyListingsMigrated() {
        dataStore.edit { preferences ->
            preferences[Keys.LegacyListingsMigrated] = true
        }
    }

    private fun parseLegacyProfile(raw: String): UserProfile? = runCatching {
        val item = JSONObject(raw)
        UserProfile(
            name = item.optString("name"),
            email = item.optString("email"),
            loggedIn = item.optBoolean("loggedIn", false)
        )
    }.getOrNull()

    private object Keys {
        val Name = stringPreferencesKey("user_name")
        val Email = stringPreferencesKey("user_email")
        val LoggedIn = booleanPreferencesKey("user_logged_in")
        val LegacyProfile = stringPreferencesKey(LEGACY_PROFILE_KEY)
        val LegacyListingsMigrated = booleanPreferencesKey("legacy_listings_migrated")
    }
}
