package pl.legnickirynek.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query(
        "SELECT listingId FROM favorites " +
            "WHERE accountId = :accountId ORDER BY createdAt DESC"
    )
    fun observeFavoriteListingIds(accountId: String): Flow<List<String>>

    @Upsert
    suspend fun upsert(favorite: FavoriteEntity)

    @Query(
        "DELETE FROM favorites " +
            "WHERE accountId = :accountId AND listingId = :listingId"
    )
    suspend fun delete(accountId: String, listingId: String)

    @Query(
        "INSERT OR IGNORE INTO favorites(accountId, listingId, createdAt) " +
            "SELECT :accountId, listingId, createdAt FROM favorites " +
            "WHERE accountId = 'legacy-local'"
    )
    suspend fun copyLegacyFavorites(accountId: String)

    @Query("DELETE FROM favorites WHERE accountId = 'legacy-local'")
    suspend fun deleteLegacyFavorites()

    @Transaction
    suspend fun claimLegacyFavorites(accountId: String) {
        copyLegacyFavorites(accountId)
        deleteLegacyFavorites()
    }
}
