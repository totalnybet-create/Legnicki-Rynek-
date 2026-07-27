package pl.legnickirynek.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ListingEntity::class,
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(ListingConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listingDao(): ListingDao
    abstract fun messageDao(): MessageDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS conversations (
                        id TEXT NOT NULL,
                        person TEXT NOT NULL,
                        listingId TEXT,
                        listingTitle TEXT NOT NULL,
                        lastMessage TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        unreadCount INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_conversations_listingId " +
                        "ON conversations(listingId)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_conversations_updatedAt " +
                        "ON conversations(updatedAt)"
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS messages (
                        id TEXT NOT NULL,
                        conversationId TEXT NOT NULL,
                        senderName TEXT NOT NULL,
                        body TEXT NOT NULL,
                        sentAt INTEGER NOT NULL,
                        sentByCurrentUser INTEGER NOT NULL,
                        isRead INTEGER NOT NULL,
                        PRIMARY KEY(id),
                        FOREIGN KEY(conversationId) REFERENCES conversations(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_messages_conversationId " +
                        "ON messages(conversationId)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_messages_sentAt " +
                        "ON messages(sentAt)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE listings ADD COLUMN ownerId TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_listings_ownerId ON listings(ownerId)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE listings ADD COLUMN latitude REAL")
                database.execSQL("ALTER TABLE listings ADD COLUMN longitude REAL")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "legnicki_rynek.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
