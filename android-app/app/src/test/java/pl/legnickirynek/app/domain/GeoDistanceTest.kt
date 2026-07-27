package pl.legnickirynek.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoDistanceTest {
    @Test
    fun `odległość tego samego punktu wynosi zero`() {
        assertEquals(
            0.0,
            GeoDistance.distanceKm(51.2070, 16.1619, 51.2070, 16.1619),
            0.000001
        )
    }

    @Test
    fun `odległość między centrum Legnicy a Lubinem ma realistyczną wartość`() {
        val distance = GeoDistance.distanceKm(
            fromLatitude = 51.2070,
            fromLongitude = 16.1619,
            toLatitude = 51.3977,
            toLongitude = 16.2096
        )

        assertTrue(distance in 20.0..23.0)
    }
}
