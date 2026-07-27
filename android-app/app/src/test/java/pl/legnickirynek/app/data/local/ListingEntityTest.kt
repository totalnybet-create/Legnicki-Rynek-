package pl.legnickirynek.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus

class ListingEntityTest {
    @Test
    fun `model zachowuje wszystkie dane po mapowaniu w obie strony`() {
        val listing = Listing(
            id = "listing-1",
            title = "Rower miejski",
            price = 1200,
            location = "Legnica",
            categoryId = "sport",
            description = "Rower w bardzo dobrym stanie.",
            imageUris = listOf("content://gallery/1", "content://gallery/2"),
            sellerName = "Jan",
            createdAt = 100L,
            updatedAt = 200L,
            status = ListingStatus.RESERVED,
            isFavorite = true
        )

        assertEquals(listing, listing.toEntity().toModel())
    }
}
