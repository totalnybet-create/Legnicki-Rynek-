package pl.legnickirynek.app.feature.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.legnickirynek.app.model.Listing

class MapFeatureServiceTest {

    private val service = MapFeatureService()
    private val legnicaCenter = GeoPoint(51.2070, 16.1550)

    @Test
    fun distanceForSamePointIsZero() {
        assertEquals(0.0, service.distanceMeters(legnicaCenter, legnicaCenter), 0.01)
    }

    @Test
    fun radiusFilterReturnsOnlyNearbyListingsInDistanceOrder() {
        val listings = listOf(
            located("near", 51.2075, 16.1550),
            located("far", 51.2600, 16.1550),
            located("middle", 51.2150, 16.1550)
        )

        val result = service.withinRadius(listings, legnicaCenter, 2_000.0)

        assertEquals(listOf("near", "middle"), result.map { it.listing.id })
    }

    @Test
    fun boundsCoverEveryPoint() {
        val points = listOf(
            GeoPoint(51.20, 16.10),
            GeoPoint(51.25, 16.20),
            GeoPoint(51.22, 16.15)
        )

        val bounds = service.bounds(points)

        assertNotNull(bounds)
        assertEquals(51.20, bounds?.southWest?.latitude ?: 0.0, 0.0001)
        assertEquals(16.10, bounds?.southWest?.longitude ?: 0.0, 0.0001)
        assertEquals(51.25, bounds?.northEast?.latitude ?: 0.0, 0.0001)
        assertEquals(16.20, bounds?.northEast?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun closeMarkersAreClusteredTogether() {
        val markers = service.markers(
            listOf(
                located("a", 51.2070, 16.1550),
                located("b", 51.2074, 16.1553),
                located("c", 51.2500, 16.2000)
            )
        )

        val clusters = service.cluster(markers, cellSizeDegrees = 0.01)

        assertEquals(2, clusters.size)
        assertEquals(2, clusters.first().markers.size)
    }

    @Test
    fun externalMapUriContainsCoordinatesAndEncodedLabel() {
        val uri = service.externalMapUri(legnicaCenter, "Rynek w Legnicy")

        assertTrue(uri.startsWith("geo:51.207,16.155?q="))
        assertTrue(uri.contains("Rynek+w+Legnicy"))
    }

    private fun located(id: String, latitude: Double, longitude: Double): LocatedListing =
        LocatedListing(
            listing = Listing(
                id = id,
                title = "Ogłoszenie $id",
                price = 100,
                location = "Legnica",
                categoryId = "inne",
                description = "Przykładowy opis ogłoszenia numer $id.",
                isFavorite = false
            ),
            point = GeoPoint(latitude, longitude)
        )
}
