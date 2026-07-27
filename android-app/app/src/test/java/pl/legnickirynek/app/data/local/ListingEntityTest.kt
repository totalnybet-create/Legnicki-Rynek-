package pl.legnickirynek.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
            ownerId = "owner-1",
            sellerName = "Jan",
            createdAt = 100L,
            updatedAt = 200L,
            status = ListingStatus.RESERVED,
            isFavorite = true,
            latitude = 51.2070067,
            longitude = 16.1619002
        )

        val restored = listing.toEntity().toModel()

        assertEquals(listing, restored)
        assertTrue(restored.hasCoordinates)
    }

    @Test
    fun `brak współrzędnych pozostaje wartością null`() {
        val listing = Listing(
            id = "listing-2",
            title = "Krzesło",
            price = 100,
            location = "Legnica",
            categoryId = "dom-ogrod",
            description = "Drewniane krzesło w dobrym stanie.",
            latitude = null,
            longitude = null
        )

        assertEquals(listing, listing.toEntity().toModel())
    }
}
