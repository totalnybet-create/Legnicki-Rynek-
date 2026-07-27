package pl.legnickirynek.app.data.local

import androidx.room.Dao
import androidx.room.Query
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
        "UPDATE favorites SET accountId = :accountId " +
            "WHERE accountId = 'legacy-local'"
    )
    suspend fun claimLegacyFavorites(accountId: String)
}
