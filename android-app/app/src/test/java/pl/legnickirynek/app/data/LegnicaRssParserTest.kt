package pl.legnickirynek.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegnicaRssParserTest {
    @Test
    fun `parser odczytuje i czyści aktualności RSS`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <item>
                  <title>Nowe wydarzenie w Legnicy</title>
                  <link>https://portal.legnica.eu/test/1</link>
                  <guid>news-1</guid>
                  <pubDate>Mon, 27 Jul 2026 12:00:00 +0200</pubDate>
                  <description><![CDATA[<p>Opis <strong>wydarzenia</strong>&nbsp;w mieście.</p>]]></description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val news = LegnicaRssParser.parse(xml)

        assertEquals(1, news.size)
        assertEquals("news-1", news.single().id)
        assertEquals("Nowe wydarzenie w Legnicy", news.single().title)
        assertEquals("Opis wydarzenia w mieście.", news.single().description)
        assertTrue(news.single().sourceUrl.endsWith("/test/1"))
    }

    @Test
    fun `parser pomija element bez linku`() {
        val xml = """
            <rss><channel><item><title>Brak linku</title></item></channel></rss>
        """.trimIndent()

        assertTrue(LegnicaRssParser.parse(xml).isEmpty())
    }
}
