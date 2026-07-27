package pl.legnickirynek.app.data

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegnicaCalendarParserTest {
    @Test
    fun `parser odczytuje wydarzenia z miesięcznego kalendarza`() {
        val html = """
            <ul class="events">
              <li>
                <h3><a href="/kalendarz-wydarzen/19809%2C92%2C6%2Cwydarzenie.html">W piątek o piątej</a></h3>
                <span class="date">03.07 17:00</span>
              </li>
              <li>
                <h3><a href="https://portal.legnica.eu/kalendarz-wydarzen/19810%2C92%2C6%2Cwydarzenie.html">Joga &amp; relaks</a></h3>
                <span>10.07</span>
              </li>
            </ul>
        """.trimIndent()

        val events = LegnicaCalendarParser.parse(html, YearMonth.of(2026, 7))

        assertEquals(2, events.size)
        assertEquals("W piątek o piątej", events[0].title)
        assertEquals("03.07.2026 • 17:00", events[0].date)
        assertEquals("Joga & relaks", events[1].title)
        assertEquals("10.07.2026", events[1].date)
        assertTrue(events.all { it.sourceUrl.startsWith("https://portal.legnica.eu/") })
    }

    @Test
    fun `parser pomija wpis bez daty`() {
        val html = """
            <a href="/kalendarz-wydarzen/1%2C92%2C6%2Cwydarzenie.html">Bez daty</a>
        """.trimIndent()

        assertTrue(LegnicaCalendarParser.parse(html, YearMonth.of(2026, 7)).isEmpty())
    }
}
