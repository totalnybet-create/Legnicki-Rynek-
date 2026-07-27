package pl.legnickirynek.app.feature.listing

import java.net.URI
import javax.inject.Inject

class ListingValidator @Inject constructor() {

    fun validate(
        draft: ListingDraft,
        gallery: ListingGallery = ListingGallery()
    ): ListingValidationResult {
        val normalizedDraft = draft.copy(
            title = normalizeSingleLine(draft.title),
            location = normalizeSingleLine(draft.location),
            categoryId = draft.categoryId.trim(),
            description = draft.description.trim()
        )
        val normalizedGallery = normalizeGallery(gallery)
        val issues = buildList {
            if (normalizedDraft.title.length !in TITLE_LENGTH) {
                add(
                    ListingValidationIssue(
                        ListingField.TITLE,
                        "Tytuł musi mieć od ${TITLE_LENGTH.first} do ${TITLE_LENGTH.last} znaków."
                    )
                )
            }
            if (normalizedDraft.price !in PRICE_RANGE) {
                add(
                    ListingValidationIssue(
                        ListingField.PRICE,
                        "Cena musi mieścić się w zakresie od ${PRICE_RANGE.first} do ${PRICE_RANGE.last} zł."
                    )
                )
            }
            if (normalizedDraft.location.length !in LOCATION_LENGTH) {
                add(
                    ListingValidationIssue(
                        ListingField.LOCATION,
                        "Lokalizacja musi mieć od ${LOCATION_LENGTH.first} do ${LOCATION_LENGTH.last} znaków."
                    )
                )
            }
            if (!CATEGORY_PATTERN.matches(normalizedDraft.categoryId)) {
                add(
                    ListingValidationIssue(
                        ListingField.CATEGORY,
                        "Wybierz prawidłową kategorię ogłoszenia."
                    )
                )
            }
            if (normalizedDraft.description.length !in DESCRIPTION_LENGTH) {
                add(
                    ListingValidationIssue(
                        ListingField.DESCRIPTION,
                        "Opis musi mieć od ${DESCRIPTION_LENGTH.first} do ${DESCRIPTION_LENGTH.last} znaków."
                    )
                )
            }
            addAll(validatePhotos(normalizedGallery))
        }

        return ListingValidationResult(
            normalizedDraft = normalizedDraft.takeIf { issues.isEmpty() },
            normalizedGallery = normalizedGallery,
            issues = issues
        )
    }

    private fun validatePhotos(gallery: ListingGallery): List<ListingValidationIssue> = buildList {
        if (gallery.photos.size > MAX_PHOTOS) {
            add(
                ListingValidationIssue(
                    ListingField.PHOTOS,
                    "Ogłoszenie może zawierać maksymalnie $MAX_PHOTOS zdjęć."
                )
            )
        }

        val duplicateUris = gallery.photos
            .groupingBy { it.uri.trim() }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicateUris.isNotEmpty()) {
            add(
                ListingValidationIssue(
                    ListingField.PHOTOS,
                    "To samo zdjęcie nie może zostać dodane więcej niż raz."
                )
            )
        }

        gallery.photos.forEach { photo ->
            if (photo.uri.isBlank() || !hasAllowedScheme(photo.uri)) {
                add(
                    ListingValidationIssue(
                        ListingField.PHOTOS,
                        "Zdjęcie ma nieprawidłowy lub niedozwolony adres."
                    )
                )
            }
            if (photo.mimeType.lowercase() !in ALLOWED_MIME_TYPES) {
                add(
                    ListingValidationIssue(
                        ListingField.PHOTOS,
                        "Obsługiwane są zdjęcia JPEG, PNG, WebP, HEIC i HEIF."
                    )
                )
            }
        }

        if (gallery.photos.isNotEmpty() && gallery.photos.count { it.isCover } != 1) {
            add(
                ListingValidationIssue(
                    ListingField.PHOTOS,
                    "Galeria musi mieć dokładnie jedno zdjęcie główne."
                )
            )
        }
    }

    private fun normalizeGallery(gallery: ListingGallery): ListingGallery {
        val ordered = gallery.photos
            .sortedBy { it.position }
            .mapIndexed { index, photo ->
                photo.copy(
                    uri = photo.uri.trim(),
                    mimeType = photo.mimeType.trim().lowercase(),
                    position = index
                )
            }
        return ListingGallery(ordered)
    }

    private fun hasAllowedScheme(value: String): Boolean = runCatching {
        URI(value).scheme?.lowercase() in ALLOWED_URI_SCHEMES
    }.getOrDefault(false)

    private fun normalizeSingleLine(value: String): String = value
        .trim()
        .replace(WHITESPACE_REGEX, " ")

    companion object {
        const val MAX_PHOTOS = 8
        private val TITLE_LENGTH = 3..100
        private val LOCATION_LENGTH = 2..120
        private val DESCRIPTION_LENGTH = 10..5_000
        private val PRICE_RANGE = 0..100_000_000
        private val CATEGORY_PATTERN = Regex("^[a-z0-9][a-z0-9_-]{1,49}$")
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val ALLOWED_URI_SCHEMES = setOf("content", "file", "https")
        private val ALLOWED_MIME_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/heif"
        )
    }
}
