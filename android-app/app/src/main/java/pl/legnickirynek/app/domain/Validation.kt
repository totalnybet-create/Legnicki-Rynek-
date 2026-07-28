package pl.legnickirynek.app.domain

enum class ListingValidationError {
    TITLE_TOO_SHORT,
    PRICE_INVALID,
    LOCATION_REQUIRED,
    DESCRIPTION_TOO_SHORT
}

enum class ProfileValidationError {
    NAME_REQUIRED,
    EMAIL_INVALID
}

object ListingFormValidator {
    fun validate(
        title: String,
        price: String,
        location: String,
        description: String
    ): ListingValidationError? {
        val numericPrice = price.toIntOrNull()

        return when {
            title.trim().length < 4 -> ListingValidationError.TITLE_TOO_SHORT
            numericPrice == null || numericPrice < 0 -> ListingValidationError.PRICE_INVALID
            location.isBlank() -> ListingValidationError.LOCATION_REQUIRED
            description.trim().length < 10 -> ListingValidationError.DESCRIPTION_TOO_SHORT
            else -> null
        }
    }
}

object ProfileValidator {
    private val emailPattern = Regex(
        pattern = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        option = RegexOption.IGNORE_CASE
    )

    fun validate(name: String, email: String): ProfileValidationError? = when {
        name.trim().length < 2 -> ProfileValidationError.NAME_REQUIRED
        !emailPattern.matches(email.trim()) -> ProfileValidationError.EMAIL_INVALID
        else -> null
    }
}
