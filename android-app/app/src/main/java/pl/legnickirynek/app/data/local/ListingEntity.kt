package pl.legnickirynek.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus

@Entity(
    tableName = "listings",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["createdAt"]),
        Index(value = ["isFavorite"]),
        Index(value = ["status"])
    ]
)
data class ListingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val price: Int,
    val location: String,
    val categoryId: String,
    val description: String,
    val imageUris: List<String>,
    val sellerName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val status: ListingStatus,
    val isFavorite: Boolean
)

fun ListingEntity.toModel(): Listing = Listing(
    id = id,
    title = title,
    price = price,
    location = location,
    categoryId = categoryId,
    description = description,
    imageUris = imageUris,
    sellerName = sellerName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = status,
    isFavorite = isFavorite
)

fun Listing.toEntity(): ListingEntity = ListingEntity(
    id = id,
    title = title,
    price = price,
    location = location,
    categoryId = categoryId,
    description = description,
    imageUris = imageUris,
    sellerName = sellerName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = status,
    isFavorite = isFavorite
)
