package pl.legnickirynek.app.feature.content

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.legnickirynek.app.feature.weather.LegnicaWeatherClient
import pl.legnickirynek.app.feature.weather.WeatherFetchResult

class WeatherAndLocalContentTest {

    @Test
    fun weatherJsonIsParsedIntoSnapshot() {
        val result = LegnicaWeatherClient().parse(
            """
            {
              "current": {
                "time": "2026-07-27T12:00",
                "temperature_2m": 24.5,
                "apparent_temperature": 25.2,
                "precipitation": 0.3,
                "weather_code": 2,
                "cloud_cover": 45,
                "wind_speed_10m": 12.4,
                "is_day": 1
              }
            }
            """.trimIndent()
        )

        assertTrue(result is WeatherFetchResult.Success)
        val weather = (result as WeatherFetchResult.Success).weather
        assertEquals(24.5, weather.temperatureCelsius, 0.01)
        assertEquals("Częściowe zachmurzenie", weather.description)
        assertEquals(45, weather.cloudCoverPercent)
        assertTrue(weather.isDay)
        assertNull(weather.observedAt)
    }

    @Test
    fun invalidWeatherJsonReturnsInvalidData() {
        val result = LegnicaWeatherClient().parse("{\"current\":{}}")

        assertTrue(result is WeatherFetchResult.InvalidData)
    }

    @Test
    fun rssParserReadsArticleImageDateAndCleansDescription() {
        val source = LocalContentSource(
            id = "city",
            name = "Miasto Legnica",
            url = "https://portal.legnica.eu/rss.xml",
            type = LocalContentType.NEWS
        )
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <item>
                  <title>Nowe wydarzenie w Legnicy</title>
                  <link>https://portal.legnica.eu/aktualnosci/test</link>
                  <description><![CDATA[<p>Krótki &amp; ważny opis.</p>]]></description>
                  <pubDate>Mon, 27 Jul 2026 10:00:00 +0200</pubDate>
                  <enclosure url="https://portal.legnica.eu/images/test.jpg" type="image/jpeg" />
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val items = RssContentParser().parse(xml, source)

        assertEquals(1, items.size)
        assertEquals("Krótki & ważny opis.", items.single().summary)
        assertEquals("https://portal.legnica.eu/images/test.jpg", items.single().imageUrl)
        assertTrue(items.single().publishedAt != null)
    }

    @Test
    fun rssParserRejectsNonHttpsArticle() {
        val source = LocalContentSource(
            id = "city",
            name = "Miasto Legnica",
            url = "https://portal.legnica.eu/rss.xml",
            type = LocalContentType.NEWS
        )
        val xml = """
            <rss><channel><item>
              <title>Niebezpieczny link</title>
              <link>javascript:alert(1)</link>
              <description>Opis</description>
            </item></channel></rss>
        """.trimIndent()

        assertTrue(RssContentParser().parse(xml, source).isEmpty())
    }

    @Test
    fun eventParserReadsCityCalendarAndResolvesRelativeLinks() {
        val source = LocalContentSource(
            id = "events",
            name = "Kalendarz Legnicy",
            url = "https://portal.legnica.eu/kalendarz-wydarzen/",
            type = LocalContentType.EVENT
        )
        val html = """
            <section>
              <h3><a href="/kalendarz/wydarzenie-1">Dni Legnicy &amp; koncert</a></h3>
              <span class="date">31.07</span>
              <h3><a href="https://portal.legnica.eu/kalendarz/wydarzenie-2">Targ staroci</a></h3>
              <span class="date">02.08</span>
            </section>
        """.trimIndent()

        val items = CityEventHtmlParser().parse(
            html = html,
            source = source,
            referenceDate = LocalDate.of(2026, 7, 27)
        )

        assertEquals(2, items.size)
        assertEquals("Dni Legnicy & koncert", items.first().title)
        assertEquals("https://portal.legnica.eu/kalendarz/wydarzenie-1", items.first().articleUrl)
        assertEquals(LocalDate.of(2026, 7, 31), items.first().eventDate)
    }
}
