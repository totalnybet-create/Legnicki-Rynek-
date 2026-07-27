package pl.legnickirynek.app.domain

import java.io.Serializable
import java.text.Normalizer
import java.util.Locale
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus

enum class ListingSort : Serializable {
    NEWEST,
    OLDEST,
    PRICE_ASCENDING,
    PRICE_DESCENDING,
    TITLE_ASCENDING,
    DISTANCE_FROM_CENTER
}

data class ListingSearchCriteria(
    val query: String = "",
    val categoryId: String? = null,
    val minimumPrice: Int? = null,
    val maximumPrice: Int? = null,
    val location: String = "",
    val maximumDistanceFromCenterKm: Int? = null,
    val includeUnavailable: Boolean = false,
    val favoritesOnly: Boolean = false,
    val sort: ListingSort = ListingSort.NEWEST
) : Serializable {
    val activeFilterCount: Int
        get() = listOf(
            categoryId != null,
            minimumPrice != null,
            maximumPrice != null,
            location.isNotBlank(),
            maximumDistanceFromCenterKm != null,
            includeUnavailable,
            favoritesOnly,
            sort != ListingSort.NEWEST
        ).count { it }
}

object ListingSearch {
    const val LEGNICA_CENTER_LATITUDE = 51.2070
    const val LEGNICA_CENTER_LONGITUDE = 16.1619

    fun apply(
        listings: List<Listing>,
        criteria: ListingSearchCriteria
    ): List<Listing> {
        val normalizedQuery = criteria.query.normalized()
        val normalizedLocation = criteria.location.normalized()

        val filtered = listings.filter { listing ->
            val searchableText = listOf(
                listing.title,
                listing.description,
                listing.location,
                listing.sellerName
            ).joinToString(" ").normalized()

            val matchesQuery = normalizedQuery.isBlank() ||
                searchableText.contains(normalizedQuery)
            val matchesCategory = criteria.categoryId == null ||
                listing.categoryId == criteria.categoryId
            val matchesMinimumPrice = criteria.minimumPrice == null ||
                listing.price >= criteria.minimumPrice
            val matchesMaximumPrice = criteria.maximumPrice == null ||
                listing.price <= criteria.maximumPrice
            val matchesLocation = normalizedLocation.isBlank() ||
                listing.location.normalized().contains(normalizedLocation)
            val matchesDistance = criteria.maximumDistanceFromCenterKm == null ||
                distanceFromLegnicaCenterKm(listing)?.let {
                    it <= criteria.maximumDistanceFromCenterKm
                } == true
            val matchesAvailability = criteria.includeUnavailable ||
                listing.status == ListingStatus.ACTIVE ||
                listing.status == ListingStatus.RESERVED
            val matchesFavorite = !criteria.favoritesOnly || listing.isFavorite

            matchesQuery &&
                matchesCategory &&
                matchesMinimumPrice &&
                matchesMaximumPrice &&
                matchesLocation &&
                matchesDistance &&
                matchesAvailability &&
                matchesFavorite
        }

        return when (criteria.sort) {
            ListingSort.NEWEST -> filtered.sortedWith(
                compareByDescending<Listing> { it.createdAt }
                    .thenByDescending { it.updatedAt }
            )

            ListingSort.OLDEST -> filtered.sortedWith(
                compareBy<Listing> { it.createdAt }
                    .thenBy { it.updatedAt }
            )

            ListingSort.PRICE_ASCENDING -> filtered.sortedWith(
                compareBy<Listing> { it.price }
                    .thenByDescending { it.createdAt }
            )

            ListingSort.PRICE_DESCENDING -> filtered.sortedWith(
                compareByDescending<Listing> { it.price }
                    .thenByDescending { it.createdAt }
            )

            ListingSort.TITLE_ASCENDING -> filtered.sortedWith(
                compareBy<Listing> { it.title.normalized() }
                    .thenByDescending { it.createdAt }
            )

            ListingSort.DISTANCE_FROM_CENTER -> filtered.sortedWith(
                compareBy<Listing> {
                    distanceFromLegnicaCenterKm(it) ?: Double.MAX_VALUE
                }.thenByDescending { it.createdAt }
            )
        }
    }

    fun distanceFromLegnicaCenterKm(listing: Listing): Double? {
        val latitude = listing.latitude ?: return null
        val longitude = listing.longitude ?: return null
        if (!listing.hasCoordinates) return null

        return GeoDistance.distanceKm(
            fromLatitude = LEGNICA_CENTER_LATITUDE,
            fromLongitude = LEGNICA_CENTER_LONGITUDE,
            toLatitude = latitude,
            toLongitude = longitude
        )
    }

    private fun String.normalized(): String = Normalizer
        .normalize(trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(DIACRITICS, "")

    private val DIACRITICS = Regex("\\p{M}+")
}
