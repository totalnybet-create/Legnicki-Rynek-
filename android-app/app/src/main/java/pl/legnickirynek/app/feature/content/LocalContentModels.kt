package pl.legnickirynek.app.feature.content

import java.time.Instant
import java.time.LocalDate

enum class LocalContentType {
    NEWS,
    EVENT
}

data class LocalContentSource(
    val id: String,
    val name: String,
    val url: String,
    val type: LocalContentType
)

data class LocalContentItem(
    val id: String,
    val type: LocalContentType,
    val title: String,
    val summary: String,
    val sourceName: String,
    val sourceUrl: String,
    val articleUrl: String,
    val imageUrl: String?,
    val publishedAt: Instant?,
    val eventDate: LocalDate?
)

data class ContentSourceError(
    val sourceName: String,
    val message: String,
    val cause: Throwable? = null
)

data class LocalContentBatch(
    val items: List<LocalContentItem>,
    val errors: List<ContentSourceError>
) {
    val isPartial: Boolean = items.isNotEmpty() && errors.isNotEmpty()
    val isFailure: Boolean = items.isEmpty() && errors.isNotEmpty()
}
