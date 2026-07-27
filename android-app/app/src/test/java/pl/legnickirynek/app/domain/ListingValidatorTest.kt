package pl.legnickirynek.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.legnickirynek.app.model.Listing

class ListingValidatorTest {
    @Test
    fun validListingPassesValidation() {
        val result = ListingValidator.validate(
            Listing(
                id = "listing-1",
                title = "Rower miejski",
                price = 750,
                location = "Legnica",
                categoryId = "sport",
                description = "Sprawny rower miejski w bardzo dobrym stanie.",
                imageUris = listOf("content://gallery/image/1")
            )
        )

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun invalidListingReturnsFieldErrors() {
        val result = ListingValidator.validate(
            Listing(
                id = "listing-2",
                title = "A",
                price = -1,
                location = "",
                categoryId = "",
                description = "krótki",
                imageUris = List(11) { "content://gallery/image/$it" }
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.keys.containsAll(
            listOf("title", "price", "location", "categoryId", "description", "imageUris")
        ))
    }
}
