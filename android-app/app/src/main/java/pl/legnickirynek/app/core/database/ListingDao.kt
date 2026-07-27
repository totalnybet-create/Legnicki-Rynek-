package pl.legnickirynek.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {
    @Query("SELECT * FROM listings ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ListingEntity>>

    @Query("SELECT COUNT(*) FROM listings")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(listing: ListingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(listings: List<ListingEntity>)

    @Query(
        """
        UPDATE listings
        SET isFavorite = CASE WHEN isFavorite = 1 THEN 0 ELSE 1 END
        WHERE id = :listingId
        """
    )
    suspend fun toggleFavorite(listingId: String)

    @Query("DELETE FROM listings")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(listings: List<ListingEntity>) {
        deleteAll()
        upsertAll(listings)
    }
}
