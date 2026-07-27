package pl.legnickirynek.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus

class ListingJsonCodecTest {
    @Test
    fun `kodek odczytuje listę opakowaną w pole listings`() {
        val json = """
            {
              "listings": [
                {
                  "id": "listing-1",
                  "title": "Rower miejski",
                  "price": 900,
                  "location": "Legnica, Centrum",
                  "categoryId": "sport",
                  "description": "Sprawny rower miejski.",
                  "imageUrls": ["https://cdn.example.pl/rower.jpg", "content://local/1"],
                  "ownerId": "owner-1",
                  "sellerName": "Jan",
                  "createdAt": 100,
                  "updatedAt": 200,
                  "status": "RESERVED",
                  "latitude": 51.207,
                  "longitude": 16.1619
                }
              ]
            }
        """.trimIndent()

        val listing = ListingJsonCodec.decodeList(json).single()

        assertEquals("listing-1", listing.id)
        assertEquals(ListingStatus.RESERVED, listing.status)
        assertEquals(listOf("https://cdn.example.pl/rower.jpg"), listing.imageUris)
        assertTrue(listing.hasCoordinates)
        assertFalse(listing.isFavorite)
    }

    @Test
    fun `kodowanie pomija lokalne uri zdjęć i stan ulubionych`() {
        val listing = Listing(
            id = "listing-2",
            title = "Telefon",
            price = 500,
            location = "Legnica",
            categoryId = "elektronika",
            description = "Telefon w dobrym stanie.",
            imageUris = listOf(
                "content://gallery/1",
                "https://cdn.example.pl/telefon.jpg"
            ),
            isFavorite = true
        )

        val json = ListingJsonCodec.encode(listing)

        assertTrue(json.contains("https://cdn.example.pl/telefon.jpg"))
        assertFalse(json.contains("content://gallery/1"))
        assertFalse(json.contains("isFavorite"))
    }

    @Test
    fun `kodek odczytuje bezpieczny adres po uploadzie`() {
        val url = ListingJsonCodec.decodeUploadedImageUrl(
            "{\"url\":\"https://cdn.example.pl/listings/photo.jpg\"}"
        )

        assertEquals("https://cdn.example.pl/listings/photo.jpg", url)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `kodek odrzuca lokalny adres zwrócony przez upload`() {
        ListingJsonCodec.decodeUploadedImageUrl(
            "{\"url\":\"content://gallery/1\"}"
        )
    }

    @Test
    fun `konfiguracja api wymaga https poza emulatorem`() {
        assertTrue(RestRemoteListingService.isAllowedBaseUrl("https://api.example.pl/v1"))
        assertTrue(RestRemoteListingService.isAllowedBaseUrl("http://10.0.2.2:8080/api"))
        assertFalse(RestRemoteListingService.isAllowedBaseUrl("http://api.example.pl"))
        assertFalse(RestRemoteListingService.isAllowedBaseUrl(""))
    }
}
