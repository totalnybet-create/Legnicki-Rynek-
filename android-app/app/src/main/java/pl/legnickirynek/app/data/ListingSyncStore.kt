package pl.legnickirynek.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.listingSyncDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "listing_sync_preferences"
)

class ListingSyncStore(context: Context) {
    private val dataStore = context.applicationContext.listingSyncDataStore

    suspend fun pendingDeletionIds(): Set<String> =
        dataStore.data.first()[PENDING_DELETION_IDS].orEmpty()

    suspend fun addPendingDeletion(id: String) {
        require(id.isNotBlank()) { "Identyfikator usuwanego ogłoszenia nie może być pusty." }
        dataStore.edit { preferences ->
            preferences[PENDING_DELETION_IDS] =
                preferences[PENDING_DELETION_IDS].orEmpty() + id
        }
    }

    suspend fun removePendingDeletion(id: String) {
        dataStore.edit { preferences ->
            preferences[PENDING_DELETION_IDS] =
                preferences[PENDING_DELETION_IDS].orEmpty() - id
        }
    }

    suspend fun markSuccessfulSync(timestamp: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            preferences[LAST_SUCCESSFUL_SYNC] = timestamp.coerceAtLeast(0L)
        }
    }

    suspend fun lastSuccessfulSync(): Long =
        dataStore.data.first()[LAST_SUCCESSFUL_SYNC] ?: 0L

    private companion object {
        val PENDING_DELETION_IDS = stringSetPreferencesKey("pending_listing_deletion_ids")
        val LAST_SUCCESSFUL_SYNC = longPreferencesKey("last_successful_listing_sync")
    }
}
