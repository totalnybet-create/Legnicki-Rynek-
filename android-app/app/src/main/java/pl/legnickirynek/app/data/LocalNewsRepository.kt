package pl.legnickirynek.app.data

import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource
import pl.legnickirynek.app.data.remote.TextHttpClient
import pl.legnickirynek.app.model.LocalNewsItem

interface LocalNewsRepository {
    suspend fun getLatestNews(limit: Int = 12): List<LocalNewsItem>
}

class LegnicaRssNewsRepository(
    private val httpClient: TextHttpClient
) : LocalNewsRepository {
    override suspend fun getLatestNews(limit: Int): List<LocalNewsItem> {
        require(limit in 1..50) { "Limit aktualności musi mieścić się w zakresie 1–50." }
        return LegnicaRssParser.parse(httpClient.get(NEWS_RSS_URL))
            .take(limit)
    }

    companion object {
        const val NEWS_RSS_URL = "https://portal.legnica.eu/rss/aktualnosci/35.xml"
    }
}

object LegnicaRssParser {
    fun parse(xml: String): List<LocalNewsItem> {
        require(xml.isNotBlank()) { "Kanał RSS jest pusty." }

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isXIncludeAware = false
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }
        val document = factory.newDocumentBuilder().parse(
            InputSource(StringReader(xml))
        )
        val nodes = document.getElementsByTagName("item")

        return buildList {
            for (index in 0 until nodes.length) {
                val element = nodes.item(index) as? Element ?: continue
                val title = element.text("title").cleanText()
                val link = element.text("link").trim()
                if (title.isBlank() || link.isBlank()) continue

                add(
                    LocalNewsItem(
                        id = element.text("guid").trim().ifBlank { stableId(link) },
                        title = title,
                        description = element.text("description").cleanText(),
                        publishedAt = element.text("pubDate").trim(),
                        sourceName = "Oficjalny Portal Miasta Legnica",
                        sourceUrl = link
                    )
                )
            }
        }.distinctBy(LocalNewsItem::id)
    }

    private fun Element.text(tagName: String): String =
        getElementsByTagName(tagName).item(0)?.textContent.orEmpty()

    private fun String.cleanText(): String =
        replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun stableId(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
        .take(24)
}
