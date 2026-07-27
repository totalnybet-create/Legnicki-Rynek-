package pl.legnickirynek.app.feature.listing

import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import pl.legnickirynek.app.model.Listing

class ListingQueryEngine @Inject constructor() {

    fun apply(
        listings: List<Listing>,
        filter: ListingFilter
    ): List<Listing> {
        val normalizedQuery = normalize(filter.query)
        val normalizedLocation = normalize(filter.locationQuery)
        val minimumPrice = minOfNullable(filter.minimumPrice, filter.maximumPrice)
        val maximumPrice = maxOfNullable(filter.minimumPrice, filter.maximumPrice)

        val matching = listings.filter { listing ->
            matchesQuery(listing, normalizedQuery) &&
                (filter.categoryIds.isEmpty() || listing.categoryId in filter.categoryIds) &&
                (minimumPrice == null || listing.price >= minimumPrice) &&
                (maximumPrice == null || listing.price <= maximumPrice) &&
                (normalizedLocation.isBlank() || normalize(listing.location).contains(normalizedLocation)) &&
                (!filter.favoritesOnly || listing.isFavorite)
        }

        return when (filter.sort) {
            ListingSort.RELEVANCE -> {
                if (normalizedQuery.isBlank()) matching
                else matching.sortedByDescending { relevanceScore(it, normalizedQuery) }
            }
            ListingSort.PRICE_ASCENDING -> matching.sortedBy { it.price }
            ListingSort.PRICE_DESCENDING -> matching.sortedByDescending { it.price }
            ListingSort.TITLE_ASCENDING -> matching.sortedBy { normalize(it.title) }
            ListingSort.TITLE_DESCENDING -> matching.sortedByDescending { normalize(it.title) }
        }
    }

    private fun matchesQuery(listing: Listing, query: String): Boolean {
        if (query.isBlank()) return true
        val tokens = query.split(' ').filter { it.isNotBlank() }
        val searchable = listOf(
            listing.title,
            listing.description,
            listing.location,
            listing.categoryId
        ).joinToString(separator = " ") { normalize(it) }
        return tokens.all(searchable::contains)
    }

    private fun relevanceScore(listing: Listing, query: String): Int {
        val title = normalize(listing.title)
        val description = normalize(listing.description)
        val location = normalize(listing.location)
        val category = normalize(listing.categoryId)
        return when {
            title == query -> 100
            title.startsWith(query) -> 70
            title.contains(query) -> 50
            location == query -> 35
            location.contains(query) -> 25
            category.contains(query) -> 15
            description.contains(query) -> 10
            else -> 0
        }
    }

    internal fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        return DIACRITIC_REGEX.replace(decomposed, "")
            .lowercase(POLISH_LOCALE)
            .replace('ł', 'l')
            .replace(WHITESPACE_REGEX, " ")
    }

    private fun minOfNullable(first: Int?, second: Int?): Int? = when {
        first == null -> second
        second == null -> first
        else -> minOf(first, second)
    }

    private fun maxOfNullable(first: Int?, second: Int?): Int? = when {
        first == null -> second
        second == null -> first
        else -> maxOf(first, second)
    }

    companion object {
        private val POLISH_LOCALE = Locale("pl", "PL")
        private val DIACRITIC_REGEX = Regex("\\p{Mn}+")
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}
