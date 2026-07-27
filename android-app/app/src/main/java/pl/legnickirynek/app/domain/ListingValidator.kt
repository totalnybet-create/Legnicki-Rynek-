package pl.legnickirynek.app.domain

import pl.legnickirynek.app.model.Listing

data class ListingValidationResult(
    val isValid: Boolean,
    val errors: Map<String, String>
)

object ListingValidator {
    private const val MaxTitleLength = 80
    private const val MaxDescriptionLength = 5000
    private const val MaxImages = 10

    fun validate(listing: Listing): ListingValidationResult {
        val errors = linkedMapOf<String, String>()

        val title = listing.title.trim()
        val description = listing.description.trim()
        val location = listing.location.trim()
        val categoryId = listing.categoryId.trim()

        if (title.length < 3) {
            errors["title"] = "Tytuł musi mieć co najmniej 3 znaki."
        } else if (title.length > MaxTitleLength) {
            errors["title"] = "Tytuł może mieć maksymalnie $MaxTitleLength znaków."
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
        } else if (description.length > MaxDescriptionLength) {
            errors["description"] = "Opis może mieć maksymalnie $MaxDescriptionLength znaków."
        }

        if (listing.imageUris.size > MaxImages) {
            errors["imageUris"] = "Możesz dodać maksymalnie $MaxImages zdjęć."
        }

        if (listing.imageUris.any { it.isBlank() }) {
            errors["imageUris"] = "Lista zdjęć zawiera nieprawidłowy adres."
        }

        return ListingValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}
