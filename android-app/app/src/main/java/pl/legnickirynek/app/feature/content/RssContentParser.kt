package pl.legnickirynek.app.feature.content

import java.io.StringReader
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource

class RssContentParser @Inject constructor() {

    fun parse(
        xml: String,
        source: LocalContentSource,
        limit: Int = DEFAULT_LIMIT
    ): List<LocalContentItem> {
        require(limit > 0) { "Limit wpisów musi być dodatni." }
        val document = createSecureFactory()
            .newDocumentBuilder()
            .parse(InputSource(StringReader(xml)))
        val items = document.getElementsByTagName("item")

        return buildList {
            for (index in 0 until items.length) {
                if (size >= limit) break
                val element = items.item(index) as? Element ?: continue
                val title = element.text("title").cleanText()
                val articleUrl = element.text("link").trim()
                if (title.isBlank() || !articleUrl.isSafeWebUrl()) continue

                val description = element.text("description").cleanText()
                val publicationDate = parseInstant(element.text("pubDate"))
                val imageUrl = element.enclosureUrl()
                    ?: element.mediaUrl()
                    ?: extractFirstImageUrl(element.text("description"))

                add(
                    LocalContentItem(
                        id = stableId(source.id, articleUrl),
                        type = source.type,
                        title = title,
                        summary = description.take(MAX_SUMMARY_LENGTH),
                        sourceName = source.name,
                        sourceUrl = source.url,
                        articleUrl = articleUrl,
                        imageUrl = imageUrl?.takeIf { it.isSafeWebUrl() },
                        publishedAt = publicationDate,
                        eventDate = null
                    )
                )
            }
        }
    }

    private fun createSecureFactory(): DocumentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        isXIncludeAware = false
        setExpandEntityReferences(false)
        runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        runCatching { setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "") }
        runCatching { setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "") }
    }

    private fun Element.text(tagName: String): String =
        getElementsByTagName(tagName).item(0)?.textContent.orEmpty()

    private fun Element.enclosureUrl(): String? =
        (getElementsByTagName("enclosure").item(0) as? Element)
            ?.getAttribute("url")
            ?.trim()
            ?.takeIf(String::isNotBlank)

    private fun Element.mediaUrl(): String? = sequenceOf("media:content", "media:thumbnail")
        .mapNotNull { tag ->
            (getElementsByTagName(tag).item(0) as? Element)
                ?.getAttribute("url")
                ?.trim()
                ?.takeIf(String::isNotBlank)
        }
        .firstOrNull()

    private fun extractFirstImageUrl(html: String): String? = IMAGE_SOURCE_REGEX
        .find(html)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()

    private fun parseInstant(value: String): Instant? {
        val text = value.trim()
        if (text.isBlank()) return null
        val parsers = listOf<(String) -> Instant?>(
            { runCatching { ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }.getOrNull() },
            { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() },
            { runCatching { Instant.parse(it) }.getOrNull() },
            {
                runCatching {
                    ZonedDateTime.parse(
                        it,
                        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
                    ).toInstant()
                }.getOrNull()
            }
        )
        return parsers.firstNotNullOfOrNull { parser -> parser(text) }
    }

    private fun String.cleanText(): String = HTML_TAG_REGEX
        .replace(this, " ")
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace(WHITESPACE_REGEX, " ")
        .trim()

    private fun String.isSafeWebUrl(): Boolean = runCatching {
        val uri = URI(trim())
        uri.scheme?.lowercase() == "https" && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    private fun stableId(sourceId: String, articleUrl: String): String {
        val input = "$sourceId|$articleUrl"
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(StandardCharsets.UTF_8))
        return hash.take(16).joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val DEFAULT_LIMIT = 50
        const val MAX_SUMMARY_LENGTH = 600
        val HTML_TAG_REGEX = Regex("<[^>]+>")
        val WHITESPACE_REGEX = Regex("\\s+")
        val IMAGE_SOURCE_REGEX = Regex(
            pattern = "<img[^>]+src=[\\\"']([^\\\"']+)[\\\"']",
            options = setOf(RegexOption.IGNORE_CASE)
        )
    }
}
