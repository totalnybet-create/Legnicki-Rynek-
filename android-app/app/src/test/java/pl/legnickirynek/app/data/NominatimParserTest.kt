package pl.legnickirynek.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NominatimParserTest {
    @Test
    fun `parser odczytuje pierwszy wynik geokodowania`() {
        val json = """
            [
              {
                "place_id": 123,
                "lat": "51.2070067",
                "lon": "16.1619002",
                "display_name": "Rynek, Legnica, województwo dolnośląskie, Polska"
              }
            ]
        """.trimIndent()

        val point = NominatimParser.parseFirst(json)

        requireNotNull(point)
        assertEquals(51.2070067, point.latitude, 0.0000001)
        assertEquals(16.1619002, point.longitude, 0.0000001)
        assertEquals(
            "Rynek, Legnica, województwo dolnośląskie, Polska",
            point.displayName
        )
    }

    @Test
    fun `parser zwraca null dla pustej listy`() {
        assertNull(NominatimParser.parseFirst("[]"))
    }

    @Test
    fun `parser odrzuca współrzędne poza zakresem`() {
        val json = """[{"lat":"123.0","lon":"16.0","display_name":"Błędny punkt"}]"""

        assertNull(NominatimParser.parseFirst(json))
    }
}
