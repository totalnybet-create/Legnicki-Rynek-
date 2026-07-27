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

interface ListingSyncStateStore {
    suspend fun pendingDeletionIds(): Set<String>
    suspend fun addPendingDeletion(id: String)
    suspend fun removePendingDeletion(id: String)
    suspend fun markSuccessfulSync(timestamp: Long = System.currentTimeMillis())
    suspend fun lastSuccessfulSync(): Long
}

class ListingSyncStore(context: Context) : ListingSyncStateStore {
    private val dataStore = context.applicationContext.listingSyncDataStore

    override suspend fun pendingDeletionIds(): Set<String> =
        dataStore.data.first()[PENDING_DELETION_IDS].orEmpty()

    override suspend fun addPendingDeletion(id: String) {
        require(id.isNotBlank()) { "Identyfikator usuwanego ogłoszenia nie może być pusty." }
        dataStore.edit { preferences ->
            preferences[PENDING_DELETION_IDS] =
                preferences[PENDING_DELETION_IDS].orEmpty() + id
        }
    }

    override suspend fun removePendingDeletion(id: String) {
        dataStore.edit { preferences ->
            preferences[PENDING_DELETION_IDS] =
                preferences[PENDING_DELETION_IDS].orEmpty() - id
        }
    }

    override suspend fun markSuccessfulSync(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_SUCCESSFUL_SYNC] = timestamp.coerceAtLeast(0L)
        }
    }

    override suspend fun lastSuccessfulSync(): Long =
        dataStore.data.first()[LAST_SUCCESSFUL_SYNC] ?: 0L

    private companion object {
        val PENDING_DELETION_IDS = stringSetPreferencesKey("pending_listing_deletion_ids")
        val LAST_SUCCESSFUL_SYNC = longPreferencesKey("last_successful_listing_sync")
    }
}
