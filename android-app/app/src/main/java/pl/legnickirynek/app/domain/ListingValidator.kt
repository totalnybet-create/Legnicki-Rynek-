package pl.legnickirynek.app.domain

import pl.legnickirynek.app.model.Listing

data class ListingValidationResult(
    val isValid: Boolean,
    val errors: Map<String, String>
)

class ListingValidationException(
    val fieldErrors: Map<String, String>
) : IllegalArgumentException(
    fieldErrors.values.firstOrNull() ?: "Nieprawidłowe dane ogłoszenia."
)

object ListingValidator {
    const val MAX_TITLE_LENGTH = 80
    const val MAX_DESCRIPTION_LENGTH = 5000
    const val MAX_IMAGES = 8

    private val supportedImageUriPattern = Regex(
        pattern = "^(content|file|https?)://.+",
        option = RegexOption.IGNORE_CASE
    )

    fun validate(listing: Listing): ListingValidationResult {
        val errors = linkedMapOf<String, String>()

        val title = listing.title.trim()
        val description = listing.description.trim()
        val location = listing.location.trim()
        val categoryId = listing.categoryId.trim()

        if (title.length < 3) {
            errors["title"] = "Tytuł musi mieć co najmniej 3 znaki."
        } else if (title.length > MAX_TITLE_LENGTH) {
            errors["title"] = "Tytuł może mieć maksymalnie $MAX_TITLE_LENGTH znaków."
        }

        if (listing.price < 0) {
            errors["price"] = "Cena nie może być ujemna."
        }

        if (location.length < 2) {
            errors["location"] = "Podaj lokalizację."
        }

        if (categoryId.isBlank()) {
            errors["categoryId"] = "Wybierz kategorię."
        }

        if (description.length < 10) {
            errors["description"] = "Opis musi mieć co najmniej 10 znaków."
        } else if (description.length > MAX_DESCRIPTION_LENGTH) {
            errors["description"] = "Opis może mieć maksymalnie $MAX_DESCRIPTION_LENGTH znaków."
        }

        if (listing.imageUris.size > MAX_IMAGES) {
            errors["imageUris"] = "Możesz dodać maksymalnie $MAX_IMAGES zdjęć."
        } else if (listing.imageUris.any { !isSupportedImageUri(it) }) {
            errors["imageUris"] = "Lista zdjęć zawiera nieprawidłowy adres."
        }

        return ListingValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    fun requireValid(listing: Listing): Listing {
        val result = validate(listing)
        if (!result.isValid) {
            throw ListingValidationException(result.errors)
        }
        return listing
    }

    fun isSupportedImageUri(uri: String): Boolean =
        supportedImageUriPattern.matches(uri.trim())
}
