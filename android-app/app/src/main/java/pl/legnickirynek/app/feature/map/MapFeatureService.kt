package pl.legnickirynek.app.feature.map

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import pl.legnickirynek.app.model.Listing

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude in -90.0..90.0) { "Szerokość geograficzna jest poza zakresem." }
        require(longitude in -180.0..180.0) { "Długość geograficzna jest poza zakresem." }
    }
}

data class LocatedListing(
    val listing: Listing,
    val point: GeoPoint
)

data class ListingMapMarker(
    val listingId: String,
    val title: String,
    val price: Int,
    val point: GeoPoint,
    val distanceMeters: Double? = null
)

data class MapBounds(
    val southWest: GeoPoint,
    val northEast: GeoPoint
)

data class MapCluster(
    val center: GeoPoint,
    val markers: List<ListingMapMarker>
)

class MapFeatureService @Inject constructor() {

    fun markers(
        listings: List<LocatedListing>,
        userLocation: GeoPoint? = null
    ): List<ListingMapMarker> = listings.map { item ->
        ListingMapMarker(
            listingId = item.listing.id,
            title = item.listing.title,
            price = item.listing.price,
            point = item.point,
            distanceMeters = userLocation?.let { distanceMeters(it, item.point) }
        )
    }.sortedWith(
        compareBy<ListingMapMarker> { it.distanceMeters ?: Double.MAX_VALUE }
            .thenBy { it.title }
    )

    fun withinRadius(
        listings: List<LocatedListing>,
        center: GeoPoint,
        radiusMeters: Double
    ): List<LocatedListing> {
        require(radiusMeters >= 0.0) { "Promień nie może być ujemny." }
        return listings
            .filter { distanceMeters(center, it.point) <= radiusMeters }
            .sortedBy { distanceMeters(center, it.point) }
    }

    fun bounds(points: List<GeoPoint>): MapBounds? {
        if (points.isEmpty()) return null
        return MapBounds(
            southWest = GeoPoint(
                latitude = points.minOf { it.latitude },
                longitude = points.minOf { it.longitude }
            ),
            northEast = GeoPoint(
                latitude = points.maxOf { it.latitude },
                longitude = points.maxOf { it.longitude }
            )
        )
    }

    fun cluster(
        markers: List<ListingMapMarker>,
        cellSizeDegrees: Double = DEFAULT_CLUSTER_CELL_DEGREES
    ): List<MapCluster> {
        require(cellSizeDegrees > 0.0) { "Rozmiar komórki grupowania musi być dodatni." }
        return markers
            .groupBy { marker ->
                GridCell(
                    latitudeCell = floor(marker.point.latitude / cellSizeDegrees).toLong(),
                    longitudeCell = floor(marker.point.longitude / cellSizeDegrees).toLong()
                )
            }
            .values
            .map { group ->
                MapCluster(
                    center = GeoPoint(
                        latitude = group.map { it.point.latitude }.average(),
                        longitude = group.map { it.point.longitude }.average()
                    ),
                    markers = group.sortedBy { it.title }
                )
            }
            .sortedByDescending { it.markers.size }
    }

    fun externalMapUri(
        point: GeoPoint,
        label: String
    ): String {
        val encodedLabel = URLEncoder.encode(label.trim(), StandardCharsets.UTF_8.name())
        return "geo:${point.latitude},${point.longitude}?q=${point.latitude},${point.longitude}($encodedLabel)"
    }

    fun distanceMeters(first: GeoPoint, second: GeoPoint): Double {
        val firstLatitude = Math.toRadians(first.latitude)
        val secondLatitude = Math.toRadians(second.latitude)
        val latitudeDelta = Math.toRadians(second.latitude - first.latitude)
        val longitudeDelta = Math.toRadians(second.longitude - first.longitude)
        val haversine = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(firstLatitude) * cos(secondLatitude) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        val angularDistance = 2 * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
        return EARTH_RADIUS_METERS * angularDistance
    }

    private data class GridCell(
        val latitudeCell: Long,
        val longitudeCell: Long
    )

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_000.0
        const val DEFAULT_CLUSTER_CELL_DEGREES = 0.01
    }
}
