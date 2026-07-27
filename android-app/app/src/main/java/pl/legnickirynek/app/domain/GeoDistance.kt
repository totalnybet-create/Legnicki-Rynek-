package pl.legnickirynek.app.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoDistance {
    private const val EARTH_RADIUS_KM = 6_371.0088

    fun distanceKm(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double
    ): Double {
        require(fromLatitude in -90.0..90.0)
        require(toLatitude in -90.0..90.0)
        require(fromLongitude in -180.0..180.0)
        require(toLongitude in -180.0..180.0)

        val fromLatRad = Math.toRadians(fromLatitude)
        val toLatRad = Math.toRadians(toLatitude)
        val deltaLat = Math.toRadians(toLatitude - fromLatitude)
        val deltaLon = Math.toRadians(toLongitude - fromLongitude)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(fromLatRad) * cos(toLatRad) *
            sin(deltaLon / 2) * sin(deltaLon / 2)
        val angularDistance = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * angularDistance
    }
}
