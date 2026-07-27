package pl.legnickirynek.app.feature.content

import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.DateTimeException
import java.time.LocalDate
import javax.inject.Inject

class CityEventHtmlParser @Inject constructor() {

    fun parse(
        html: String,
        source: LocalContentSource,
        referenceDate: LocalDate = LocalDate.now(),
        limit: Int = DEFAULT_LIMIT
    ): List<LocalContentItem> {
        require(source.type == LocalContentType.EVENT) { "Źródło musi być kalendarzem wydarzeń." }
        require(limit > 0) { "Limit wydarzeń musi być dodatni." }

        val candidates = EVENT_PATTERN.findAll(html)
            .mapNotNull { match ->
                val relativeUrl = match.groupValues[1].decodeHtml().trim()
                val title = match.groupValues[2].cleanText()
                val dateText = match.groupValues[3].trim()
                val articleUrl = resolveHttpsUrl(source.url, relativeUrl) ?: return@mapNotNull null
                val eventDate = parseEventDate(dateText, referenceDate) ?: return@mapNotNull null
                if (title.isBlank()) return@mapNotNull null

                LocalContentItem(
                    id = stableId(source.id, articleUrl, eventDate),
                    type = LocalContentType.EVENT,
                    title = title,
                    summary = "",
                    sourceName = source.name,
                    sourceUrl = source.url,
                    articleUrl = articleUrl,
                    imageUrl = null,
                    publishedAt = null,
                    eventDate = eventDate
                )
            }
            .distinctBy { it.articleUrl to it.eventDate }
            .sortedWith(compareBy<LocalContentItem> { it.eventDate }.thenBy { it.title })
            .take(limit)
            .toList()

        return candidates
    }

    private fun parseEventDate(value: String, referenceDate: LocalDate): LocalDate? {
        val parts = value.split('.').filter(String::isNotBlank)
        if (parts.size !in 2..3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val explicitYear = parts.getOrNull(2)?.toIntOrNull()
        val initialYear = explicitYear ?: referenceDate.year

        return try {
            val candidate = LocalDate.of(initialYear, month, day)
            if (explicitYear == null && candidate.isBefore(referenceDate.minusDays(PAST_DATE_TOLERANCE_DAYS))) {
                candidate.plusYears(1)
            } else {
                candidate
            }
        } catch (_: DateTimeException) {
            null
        }
    }

    private fun resolveHttpsUrl(baseUrl: String, candidate: String): String? = runCatching {
        val resolved = URI(baseUrl).resolve(candidate)
        resolved.toString().takeIf {
            resolved.scheme?.lowercase() == "https" && !resolved.host.isNullOrBlank()
        }
    }.getOrNull()

    private fun String.cleanText(): String = HTML_TAG_REGEX
        .replace(this, " ")
        .decodeHtml()
        .replace(WHITESPACE_REGEX, " ")
        .trim()

    private fun String.decodeHtml(): String = this
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)

    private fun stableId(sourceId: String, articleUrl: String, date: LocalDate): String {
        val input = "$sourceId|$articleUrl|$date"
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(StandardCharsets.UTF_8))
        return hash.take(16).joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val DEFAULT_LIMIT = 100
        const val PAST_DATE_TOLERANCE_DAYS = 7L
        val EVENT_PATTERN = Regex(
            pattern = "<h[1-6][^>]*>\\s*<a[^>]*href=[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>\\s*</h[1-6]>.*?(\\d{2}\\.\\d{2}(?:\\.\\d{4})?)",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val HTML_TAG_REGEX = Regex("<[^>]+>")
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
