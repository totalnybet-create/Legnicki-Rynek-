package pl.legnickirynek.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ListingEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(ListingConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listingDao(): ListingDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "legnicki_rynek.db"
                ).build().also { instance = it }
            }
    }
}
