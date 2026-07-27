package pl.legnickirynek.app.feature.listing

import pl.legnickirynek.app.model.Listing

data class ListingDraft(
    val title: String,
    val price: Int,
    val location: String,
    val categoryId: String,
    val description: String
)

data class ListingPhoto(
    val id: String,
    val uri: String,
    val mimeType: String,
    val position: Int,
    val isCover: Boolean
)

data class ListingGallery(
    val photos: List<ListingPhoto> = emptyList()
)

enum class ListingField {
    TITLE,
    PRICE,
    LOCATION,
    CATEGORY,
    DESCRIPTION,
    PHOTOS
}

data class ListingValidationIssue(
    val field: ListingField,
    val message: String
)

data class ListingValidationResult(
    val normalizedDraft: ListingDraft?,
    val normalizedGallery: ListingGallery,
    val issues: List<ListingValidationIssue>
) {
    val isValid: Boolean = issues.isEmpty() && normalizedDraft != null
}

enum class ListingSort {
    RELEVANCE,
    PRICE_ASCENDING,
    PRICE_DESCENDING,
    TITLE_ASCENDING,
    TITLE_DESCENDING
}

data class ListingFilter(
    val query: String = "",
    val categoryIds: Set<String> = emptySet(),
    val minimumPrice: Int? = null,
    val maximumPrice: Int? = null,
    val locationQuery: String = "",
    val favoritesOnly: Boolean = false,
    val sort: ListingSort = ListingSort.RELEVANCE
)

sealed interface GalleryMutationResult {
    data class Success(val gallery: ListingGallery) : GalleryMutationResult
    data class Failure(val message: String) : GalleryMutationResult
}

sealed interface ListingMutationResult {
    data class Success(
        val listing: Listing,
        val gallery: ListingGallery
    ) : ListingMutationResult

    data class ValidationFailed(
        val issues: List<ListingValidationIssue>
    ) : ListingMutationResult

    data class NotFound(val listingId: String) : ListingMutationResult
    data class Failure(val message: String, val cause: Throwable? = null) : ListingMutationResult
}
