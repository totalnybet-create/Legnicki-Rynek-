package pl.legnickirynek.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.legnickirynek.app.model.Listing

class ListingValidatorTest {
    @Test
    fun validListingPassesValidation() {
        val result = ListingValidator.validate(
            validListing(imageUris = listOf("content://gallery/image/1"))
        )

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun eightImagesAreAccepted() {
        val result = ListingValidator.validate(
            validListing(
                imageUris = List(ListingValidator.MAX_IMAGES) {
                    "content://gallery/image/$it"
                }
            )
        )

        assertTrue(result.isValid)
    }

    @Test
    fun tooManyImagesReturnFieldError() {
        val result = ListingValidator.validate(
            validListing(
                imageUris = List(ListingValidator.MAX_IMAGES + 1) {
                    "content://gallery/image/$it"
                }
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey("imageUris"))
    }

    @Test
    fun unsupportedImageUriReturnsFieldError() {
        val result = ListingValidator.validate(
            validListing(imageUris = listOf("ftp://server/image.jpg"))
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey("imageUris"))
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
                imageUris = listOf("")
            )
        )

        assertFalse(result.isValid)
        assertTrue(
            result.errors.keys.containsAll(
                listOf("title", "price", "location", "categoryId", "description", "imageUris")
            )
        )
    }

    private fun validListing(imageUris: List<String>) = Listing(
        id = "listing-1",
        title = "Rower miejski",
        price = 750,
        location = "Legnica",
        categoryId = "sport",
        description = "Sprawny rower miejski w bardzo dobrym stanie.",
        imageUris = imageUris
    )
}
