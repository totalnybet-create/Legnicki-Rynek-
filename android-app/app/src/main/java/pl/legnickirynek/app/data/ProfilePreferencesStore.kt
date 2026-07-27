package pl.legnickirynek.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import pl.legnickirynek.app.model.UserProfile

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "profile_preferences"
)

class ProfilePreferencesStore(context: Context) {
    private val dataStore = context.applicationContext.profileDataStore

    val profile: Flow<UserProfile> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            UserProfile(
                name = preferences[NAME].orEmpty(),
                email = preferences[EMAIL].orEmpty(),
                loggedIn = preferences[LOGGED_IN] ?: false
            )
        }

    suspend fun save(profile: UserProfile) {
        dataStore.edit { preferences ->
            preferences[NAME] = profile.name
            preferences[EMAIL] = profile.email
            preferences[LOGGED_IN] = profile.loggedIn
        }
    }

    suspend fun migrateFromLegacy(profile: UserProfile) {
        dataStore.edit { preferences ->
            if (preferences[MIGRATION_COMPLETE] == true) return@edit

            preferences[NAME] = profile.name
            preferences[EMAIL] = profile.email
            preferences[LOGGED_IN] = profile.loggedIn
            preferences[MIGRATION_COMPLETE] = true
        }
    }

    private companion object {
        val NAME = stringPreferencesKey("profile_name")
        val EMAIL = stringPreferencesKey("profile_email")
        val LOGGED_IN = booleanPreferencesKey("profile_logged_in")
        val MIGRATION_COMPLETE = booleanPreferencesKey("legacy_profile_migration_complete")
    }
}
