package pl.legnickirynek.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "favorites",
    primaryKeys = ["accountId", "listingId"],
    foreignKeys = [
        ForeignKey(
            entity = ListingEntity::class,
            parentColumns = ["id"],
            childColumns = ["listingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["listingId"])
    ]
)
data class FavoriteEntity(
    val accountId: String,
    val listingId: String,
    val createdAt: Long = System.currentTimeMillis()
)
