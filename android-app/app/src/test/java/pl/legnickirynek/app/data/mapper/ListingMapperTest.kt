package pl.legnickirynek.app.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.legnickirynek.app.model.Listing

class ListingMapperTest {
    @Test
    fun mappingRoundTripPreservesListingData() {
        val listing = Listing(
            id = "listing-test",
            title = "Rower trekkingowy",
            price = 1250,
            location = "Legnica",
            categoryId = "sport",
            description = "Sprawny rower po przegladzie.",
            isFavorite = true
        )

        val restored = listing
            .toEntity(createdAt = 1234L)
            .toDomain()

        assertEquals(listing, restored)
    }
}
