package pl.legnickirynek.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {
    @Query("SELECT * FROM listings ORDER BY createdAt DESC, updatedAt DESC")
    fun observeAll(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ListingEntity?>

    @Upsert
    suspend fun upsert(listing: ListingEntity)

    @Upsert
    suspend fun upsertAll(listings: List<ListingEntity>)

    @Query("DELETE FROM listings WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        "UPDATE listings SET ownerId = :ownerId, sellerName = :sellerName " +
            "WHERE (ownerId = '' OR ownerId = 'local-device') AND id LIKE 'listing-%'"
    )
    suspend fun claimLegacyListings(ownerId: String, sellerName: String)

    @Query("SELECT COUNT(*) FROM listings")
    suspend fun count(): Int
}
