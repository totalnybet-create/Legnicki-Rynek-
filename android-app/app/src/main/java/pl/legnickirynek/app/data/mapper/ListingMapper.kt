package pl.legnickirynek.app.data.mapper

import pl.legnickirynek.app.core.database.ListingEntity
import pl.legnickirynek.app.model.Listing

fun ListingEntity.toDomain(): Listing = Listing(
    id = id,
    title = title,
    price = price,
    location = location,
    categoryId = categoryId,
    description = description,
    isFavorite = isFavorite
)

fun Listing.toEntity(createdAt: Long = System.currentTimeMillis()): ListingEntity = ListingEntity(
    id = id,
    title = title,
    price = price,
    location = location,
    categoryId = categoryId,
    description = description,
    isFavorite = isFavorite,
    createdAt = createdAt
)
