package pl.legnickirynek.app.model

data class WeatherSnapshot(
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val weatherCode: Int,
    val description: String,
    val observedAt: String,
    val isDay: Boolean
)

data class LocalNewsItem(
    val id: String,
    val title: String,
    val description: String,
    val publishedAt: String,
    val sourceName: String,
    val sourceUrl: String
)

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val displayName: String
) {
    init {
        require(latitude in -90.0..90.0) { "Nieprawidłowa szerokość geograficzna." }
        require(longitude in -180.0..180.0) { "Nieprawidłowa długość geograficzna." }
    }
}
