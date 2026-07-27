package pl.legnickirynek.app.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "listings",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["isFavorite"])
    ]
)
data class ListingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val price: Int,
    val location: String,
    val categoryId: String,
    val description: String,
    val isFavorite: Boolean,
    val createdAt: Long
)
