package pl.legnickirynek.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ListingEntity::class],
    version = 1,
    exportSchema = true
)
abstract class LegnickiRynekDatabase : RoomDatabase() {
    abstract fun listingDao(): ListingDao
}
