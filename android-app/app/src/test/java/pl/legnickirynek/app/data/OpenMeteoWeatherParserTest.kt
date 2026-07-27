package pl.legnickirynek.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OpenMeteoWeatherParserTest {
    @Test
    fun `parser odczytuje aktualną pogodę`() {
        val json = """
            {
              "latitude": 51.2,
              "longitude": 16.16,
              "current": {
                "time": "2026-07-27T16:00",
                "temperature_2m": 24.6,
                "relative_humidity_2m": 58,
                "apparent_temperature": 25.1,
                "weather_code": 2,
                "wind_speed_10m": 13.4,
                "is_day": 0
              }
            }
        """.trimIndent()

        val weather = OpenMeteoWeatherParser.parse(json)

        assertEquals(24.6, weather.temperatureC, 0.001)
        assertEquals(25.1, weather.apparentTemperatureC, 0.001)
        assertEquals(58, weather.humidityPercent)
        assertEquals(13.4, weather.windSpeedKmh, 0.001)
        assertEquals("Częściowe zachmurzenie", weather.description)
        assertEquals("2026-07-27T16:00", weather.observedAt)
        assertFalse(weather.isDay)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parser odrzuca odpowiedź bez sekcji current`() {
        OpenMeteoWeatherParser.parse("{\"latitude\":51.2}")
    }
}
