package pl.legnickirynek.app.data

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import pl.legnickirynek.app.data.remote.TextHttpClient
import pl.legnickirynek.app.model.LocalEvent

interface LocalEventsRepository {
    suspend fun getUpcomingEvents(limit: Int = 16): List<LocalEvent>
}

class LegnicaCalendarEventsRepository(
    private val httpClient: TextHttpClient,
    private val clock: Clock = Clock.systemDefaultZone()
) : LocalEventsRepository {
    override suspend fun getUpcomingEvents(limit: Int): List<LocalEvent> {
        require(limit in 1..50) { "Limit wydarzeń musi mieścić się w zakresie 1–50." }

        val today = LocalDate.now(clock)
        val months = listOf(YearMonth.from(today), YearMonth.from(today).plusMonths(1))
        return months
            .flatMap { month ->
                val html = httpClient.get(monthUrl(month))
                LegnicaCalendarParser.parse(html, month)
            }
            .distinctBy(LocalEvent::id)
            .filter { event -> event.localDateOrNull()?.let { !it.isBefore(today) } ?: true }
            .sortedWith(compareBy<LocalEvent> { it.localDateOrNull() ?: LocalDate.MAX }.thenBy { it.title })
            .take(limit)
    }

    companion object {
        private const val BASE_URL = "https://portal.legnica.eu/kalendarz-wydarzen/"

        fun monthUrl(month: YearMonth): String = BASE_URL +
            month.monthValue.toString().padStart(2, '0') +
            "-${month.year}%2Cmiesiac.html"
    }
}

object LegnicaCalendarParser {
    private val eventLinkRegex = Regex(
        """<a\b[^>]*href\s*=\s*["']([^"']*/kalendarz-wydarzen/[^"']*wydarzenie\.html)["'][^>]*>(.*?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val dateRegex = Regex("(?<!\\d)(\\d{2})\\.(\\d{2})(?:\\.(\\d{4}))?(?:\\s+(\\d{2}:\\d{2}))?")

    fun parse(html: String, month: YearMonth): List<LocalEvent> {
        require(html.isNotBlank()) { "Kalendarz wydarzeń jest pusty." }
        val matches = eventLinkRegex.findAll(html).toList()

        return matches.mapNotNull { match ->
            val sourceUrl = absoluteUrl(decodeEntities(match.groupValues[1]).trim())
            val title = cleanHtml(match.groupValues[2])
            if (title.isBlank()) return@mapNotNull null

            val nextStart = matches.firstOrNull { it.range.first > match.range.first }?.range?.first
                ?: html.length
            val nearbyEnd = minOf(nextStart, match.range.last + 700, html.length)
            val nearbyText = cleanHtml(html.substring(match.range.last + 1, nearbyEnd))
            val dateMatch = dateRegex.find(nearbyText) ?: return@mapNotNull null
            val day = dateMatch.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val parsedMonth = dateMatch.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            val parsedYear = dateMatch.groupValues[3].toIntOrNull() ?: month.year
            val time = dateMatch.groupValues[4]
            val date = runCatching { LocalDate.of(parsedYear, parsedMonth, day) }.getOrNull()
                ?: return@mapNotNull null

            LocalEvent(
                id = stableId("$sourceUrl|$date|$time"),
                title = title,
                date = buildString {
                    append(date.dayOfMonth.toString().padStart(2, '0'))
                    append('.')
                    append(date.monthValue.toString().padStart(2, '0'))
                    append('.')
                    append(date.year)
                    if (time.isNotBlank()) append(" • $time")
                },
                location = "Legnica",
                description = "Szczegóły wydarzenia na Oficjalnym Portalu Miasta Legnica.",
                sourceUrl = sourceUrl
            )
        }.distinctBy { event -> event.sourceUrl to event.date }
    }

    private fun LocalEvent.localDateOrNull(): LocalDate? {
        val datePart = date.substringBefore(" • ")
        val parts = datePart.split('.')
        if (parts.size != 3) return null
        return runCatching {
            LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
        }.getOrNull()
    }

    private fun absoluteUrl(url: String): String = when {
        url.startsWith("https://", ignoreCase = true) -> url
        url.startsWith("http://", ignoreCase = true) -> url.replaceFirst("http://", "https://")
        url.startsWith("/") -> "https://portal.legnica.eu$url"
        else -> "https://portal.legnica.eu/$url"
    }

    private fun cleanHtml(value: String): String = decodeEntities(
        value
            .replace(Regex("<script\\b[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("<style\\b[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("<[^>]+>"), " ")
    ).replace(Regex("\\s+"), " ").trim()

    private fun decodeEntities(value: String): String = value
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace("&apos;", "'", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)

    private fun stableId(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
        .take(24)
}

private fun LocalEvent.localDateOrNull(): LocalDate? {
    val datePart = date.substringBefore(" • ")
    val parts = datePart.split('.')
    if (parts.size != 3) return null
    return runCatching {
        LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
    }.getOrNull()
}
