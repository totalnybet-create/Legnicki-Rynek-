package pl.legnickirynek.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidationTest {
    @Test
    fun `poprawne ogłoszenie przechodzi walidację`() {
        val result = ListingValidator.validate(
            title = "Rower miejski",
            price = "850",
            location = "Legnica",
            description = "Rower po pełnym przeglądzie."
        )

        assertNull(result)
    }

    @Test
    fun `krótki tytuł jest odrzucany`() {
        val result = ListingValidator.validate(
            title = "Abc",
            price = "100",
            location = "Legnica",
            description = "Opis wystarczającej długości."
        )

        assertEquals(ListingValidationError.TITLE_TOO_SHORT, result)
    }

    @Test
    fun `nieprawidłowa cena jest odrzucana`() {
        val result = ListingValidator.validate(
            title = "Poprawny tytuł",
            price = "abc",
            location = "Legnica",
            description = "Opis wystarczającej długości."
        )

        assertEquals(ListingValidationError.PRICE_INVALID, result)
    }

    @Test
    fun `pusta lokalizacja jest odrzucana`() {
        val result = ListingValidator.validate(
            title = "Poprawny tytuł",
            price = "100",
            location = "   ",
            description = "Opis wystarczającej długości."
        )

        assertEquals(ListingValidationError.LOCATION_REQUIRED, result)
    }

    @Test
    fun `krótki opis jest odrzucany`() {
        val result = ListingValidator.validate(
            title = "Poprawny tytuł",
            price = "100",
            location = "Legnica",
            description = "Za krótki"
        )

        assertEquals(ListingValidationError.DESCRIPTION_TOO_SHORT, result)
    }

    @Test
    fun `poprawny profil przechodzi walidację`() {
        assertNull(ProfileValidator.validate("Jan", "jan@example.pl"))
    }

    @Test
    fun `puste imię jest odrzucane`() {
        assertEquals(
            ProfileValidationError.NAME_REQUIRED,
            ProfileValidator.validate("J", "jan@example.pl")
        )
    }

    @Test
    fun `nieprawidłowy email jest odrzucany`() {
        assertEquals(
            ProfileValidationError.EMAIL_INVALID,
            ProfileValidator.validate("Jan", "janexample.pl")
        )
    }
}
