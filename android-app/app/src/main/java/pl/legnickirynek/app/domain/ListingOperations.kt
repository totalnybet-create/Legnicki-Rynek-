package pl.legnickirynek.app.domain

import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus

object ListingOperations {
    fun add(listings: List<Listing>, listing: Listing, sellerName: String): List<Listing> =
        listOf(listing.copy(sellerName = sellerName.ifBlank { listing.sellerName })) + listings

    fun update(listings: List<Listing>, listing: Listing, updatedAt: Long): List<Listing> =
        listings.map { existing ->
            if (existing.id == listing.id) {
                listing.copy(
                    createdAt = existing.createdAt,
                    updatedAt = updatedAt,
                    isFavorite = existing.isFavorite
                )
            } else {
                existing
            }
        }

    fun delete(listings: List<Listing>, id: String): List<Listing> =
        listings.filterNot { it.id == id }

    fun toggleFavorite(listings: List<Listing>, id: String): List<Listing> =
        listings.map { listing ->
            if (listing.id == id) listing.copy(isFavorite = !listing.isFavorite) else listing
        }

    fun updateStatus(
        listings: List<Listing>,
        id: String,
        status: ListingStatus,
        updatedAt: Long
    ): List<Listing> = listings.map { listing ->
        if (listing.id == id) listing.copy(status = status, updatedAt = updatedAt) else listing
    }
}
