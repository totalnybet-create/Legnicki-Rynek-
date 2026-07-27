package pl.legnickirynek.app.feature.weather

import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class WeatherSnapshot(
    val observedAt: OffsetDateTime?,
    val temperatureCelsius: Double,
    val apparentTemperatureCelsius: Double,
    val precipitationMillimeters: Double,
    val cloudCoverPercent: Int,
    val windSpeedKilometersPerHour: Double,
    val weatherCode: Int,
    val description: String,
    val isDay: Boolean
)

sealed interface WeatherFetchResult {
    data class Success(val weather: WeatherSnapshot) : WeatherFetchResult
    data class HttpError(val statusCode: Int) : WeatherFetchResult
    data class NetworkError(val cause: Throwable) : WeatherFetchResult
    data class InvalidData(val cause: Throwable) : WeatherFetchResult
}

class LegnicaWeatherClient internal constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val endpoint: String
) {

    @Inject
    constructor() : this(
        client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build(),
        json = Json {
            ignoreUnknownKeys = true
            isLenient = false
        },
        endpoint = BASE_ENDPOINT
    )

    suspend fun fetchCurrent(): WeatherFetchResult = withContext(Dispatchers.IO) {
        val url = endpoint.toHttpUrl().newBuilder()
            .addQueryParameter("latitude", LEGNICA_LATITUDE.toString())
            .addQueryParameter("longitude", LEGNICA_LONGITUDE.toString())
            .addQueryParameter(
                "current",
                listOf(
                    "temperature_2m",
                    "apparent_temperature",
                    "precipitation",
                    "weather_code",
                    "cloud_cover",
                    "wind_speed_10m",
                    "is_day"
                ).joinToString(",")
            )
            .addQueryParameter("timezone", "Europe/Warsaw")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext WeatherFetchResult.HttpError(response.code)
                }
                val body = response.body.string()
                parse(body)
            }
        }.getOrElse { error ->
            WeatherFetchResult.NetworkError(error)
        }
    }

    internal fun parse(rawJson: String): WeatherFetchResult = runCatching {
        val response = json.decodeFromString<OpenMeteoResponse>(rawJson)
        val current = response.current
        WeatherFetchResult.Success(
            WeatherSnapshot(
                observedAt = parseDateTime(current.time),
                temperatureCelsius = current.temperature,
                apparentTemperatureCelsius = current.apparentTemperature,
                precipitationMillimeters = current.precipitation,
                cloudCoverPercent = current.cloudCover.coerceIn(0, 100),
                windSpeedKilometersPerHour = current.windSpeed,
                weatherCode = current.weatherCode,
                description = describeWeatherCode(current.weatherCode),
                isDay = current.isDay == 1
            )
        )
    }.getOrElse { error ->
        WeatherFetchResult.InvalidData(error)
    }

    private fun parseDateTime(value: String): OffsetDateTime? = try {
        OffsetDateTime.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }

    private fun describeWeatherCode(code: Int): String = when (code) {
        0 -> "Bezchmurnie"
        1 -> "Przeważnie pogodnie"
        2 -> "Częściowe zachmurzenie"
        3 -> "Pochmurno"
        45, 48 -> "Mgła"
        51, 53, 55 -> "Mżawka"
        56, 57 -> "Marznąca mżawka"
        61, 63, 65 -> "Deszcz"
        66, 67 -> "Marznący deszcz"
        71, 73, 75, 77 -> "Śnieg"
        80, 81, 82 -> "Przelotne opady deszczu"
        85, 86 -> "Przelotne opady śniegu"
        95 -> "Burza"
        96, 99 -> "Burza z gradem"
        else -> "Warunki zmienne"
    }

    @Serializable
    private data class OpenMeteoResponse(
        val current: CurrentWeather
    )

    @Serializable
    private data class CurrentWeather(
        val time: String,
        @SerialName("temperature_2m") val temperature: Double,
        @SerialName("apparent_temperature") val apparentTemperature: Double,
        val precipitation: Double = 0.0,
        @SerialName("weather_code") val weatherCode: Int,
        @SerialName("cloud_cover") val cloudCover: Int = 0,
        @SerialName("wind_speed_10m") val windSpeed: Double = 0.0,
        @SerialName("is_day") val isDay: Int = 1
    )

    private companion object {
        const val BASE_ENDPOINT = "https://api.open-meteo.com/v1/forecast"
        const val LEGNICA_LATITUDE = 51.2070
        const val LEGNICA_LONGITUDE = 16.1550
        const val USER_AGENT = "LegnickiRynek-Android/0.2"
    }
}
