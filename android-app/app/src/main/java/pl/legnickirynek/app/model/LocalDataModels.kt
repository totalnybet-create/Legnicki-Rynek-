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
