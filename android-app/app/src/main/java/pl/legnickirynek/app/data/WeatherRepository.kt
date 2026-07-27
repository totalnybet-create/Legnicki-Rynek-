package pl.legnickirynek.app.data

import pl.legnickirynek.app.data.remote.TextHttpClient
import pl.legnickirynek.app.model.WeatherSnapshot

interface WeatherRepository {
    suspend fun getCurrentWeather(): WeatherSnapshot
}

class OpenMeteoWeatherRepository(
    private val httpClient: TextHttpClient
) : WeatherRepository {
    override suspend fun getCurrentWeather(): WeatherSnapshot =
        OpenMeteoWeatherParser.parse(httpClient.get(LEGNICA_WEATHER_URL))

    companion object {
        const val LEGNICA_WEATHER_URL =
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=51.2070" +
                "&longitude=16.1619" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature," +
                "weather_code,wind_speed_10m,is_day" +
                "&timezone=Europe%2FWarsaw"
    }
}

object OpenMeteoWeatherParser {
    fun parse(json: String): WeatherSnapshot {
        val current = extractObject(json, "current")
        val weatherCode = number(current, "weather_code").toInt()

        return WeatherSnapshot(
            temperatureC = number(current, "temperature_2m"),
            apparentTemperatureC = number(current, "apparent_temperature"),
            humidityPercent = number(current, "relative_humidity_2m").toInt().coerceIn(0, 100),
            windSpeedKmh = number(current, "wind_speed_10m").coerceAtLeast(0.0),
            weatherCode = weatherCode,
            description = WeatherCodeDescription.fromCode(weatherCode),
            observedAt = string(current, "time"),
            isDay = number(current, "is_day").toInt() == 1
        )
    }

    private fun extractObject(json: String, key: String): String {
        val keyIndex = Regex("\"${Regex.escape(key)}\"\\s*:").find(json)?.range?.last
            ?: throw IllegalArgumentException("Brak pola $key w odpowiedzi pogodowej.")
        val openingBrace = json.indexOf('{', startIndex = keyIndex + 1)
        if (openingBrace < 0) throw IllegalArgumentException("Pole $key nie zawiera obiektu.")

        var depth = 0
        var inString = false
        var escaped = false
        for (index in openingBrace until json.length) {
            val char = json[index]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == '"') {
                    inString = false
                }
                continue
            }

            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return json.substring(openingBrace, index + 1)
                }
            }
        }
        throw IllegalArgumentException("Nieprawidłowy obiekt $key w odpowiedzi pogodowej.")
    }

    private fun number(json: String, key: String): Double {
        val match = Regex(
            "\"${Regex.escape(key)}\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)"
        ).find(json) ?: throw IllegalArgumentException("Brak pola liczbowego $key.")
        return match.groupValues[1].toDouble()
    }

    private fun string(json: String, key: String): String {
        val match = Regex(
            "\"${Regex.escape(key)}\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
        ).find(json) ?: throw IllegalArgumentException("Brak pola tekstowego $key.")
        return match.groupValues[1]
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}

object WeatherCodeDescription {
    fun fromCode(code: Int): String = when (code) {
        0 -> "Bezchmurnie"
        1 -> "Przeważnie bezchmurnie"
        2 -> "Częściowe zachmurzenie"
        3 -> "Pochmurno"
        45, 48 -> "Mgła"
        51, 53, 55 -> "Mżawka"
        56, 57 -> "Marznąca mżawka"
        61, 63, 65 -> "Deszcz"
        66, 67 -> "Marznący deszcz"
        71, 73, 75, 77 -> "Śnieg"
        80, 81, 82 -> "Przelotny deszcz"
        85, 86 -> "Przelotny śnieg"
        95 -> "Burza"
        96, 99 -> "Burza z gradem"
        else -> "Nieznane warunki"
    }
}
