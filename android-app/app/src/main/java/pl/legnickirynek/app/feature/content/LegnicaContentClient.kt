package pl.legnickirynek.app.feature.content

import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class LegnicaContentClient internal constructor(
    private val client: OkHttpClient,
    private val rssParser: RssContentParser,
    private val eventParser: CityEventHtmlParser
) {

    @Inject
    constructor(
        rssParser: RssContentParser,
        eventParser: CityEventHtmlParser
    ) : this(
        client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build(),
        rssParser = rssParser,
        eventParser = eventParser
    )

    suspend fun fetchNews(limitPerSource: Int = 30): LocalContentBatch = coroutineScope {
        val results = NEWS_SOURCES.map { source ->
            async(Dispatchers.IO) {
                runCatching {
                    rssParser.parse(
                        xml = fetchBody(source.url),
                        source = source,
                        limit = limitPerSource
                    )
                }.fold(
                    onSuccess = { SourceResult(it, null) },
                    onFailure = {
                        SourceResult(
                            items = emptyList(),
                            error = ContentSourceError(
                                sourceName = source.name,
                                message = errorMessage(it),
                                cause = it
                            )
                        )
                    }
                )
            }
        }.awaitAll()

        LocalContentBatch(
            items = deduplicateNews(results.flatMap(SourceResult::items)),
            errors = results.mapNotNull(SourceResult::error)
        )
    }

    suspend fun fetchEvents(
        referenceDate: LocalDate = LocalDate.now(),
        limit: Int = 100
    ): LocalContentBatch = withContext(Dispatchers.IO) {
        runCatching {
            eventParser.parse(
                html = fetchBody(EVENT_SOURCE.url),
                source = EVENT_SOURCE,
                referenceDate = referenceDate,
                limit = limit
            )
        }.fold(
            onSuccess = { items ->
                LocalContentBatch(
                    items = items.distinctBy { it.articleUrl to it.eventDate },
                    errors = emptyList()
                )
            },
            onFailure = { error ->
                LocalContentBatch(
                    items = emptyList(),
                    errors = listOf(
                        ContentSourceError(
                            sourceName = EVENT_SOURCE.name,
                            message = errorMessage(error),
                            cause = error
                        )
                    )
                )
            }
        )
    }

    private fun fetchBody(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/rss+xml, application/xml, text/html;q=0.9")
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ContentHttpException(response.code, url)
            }
            return response.body.string()
        }
    }

    private fun deduplicateNews(items: List<LocalContentItem>): List<LocalContentItem> = items
        .sortedWith(
            compareByDescending<LocalContentItem> { it.publishedAt }
                .thenBy { it.title }
        )
        .distinctBy { item ->
            item.articleUrl.ifBlank { normalize(item.title) }
        }

    private fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        return DIACRITIC_REGEX.replace(decomposed, "")
            .lowercase(POLISH_LOCALE)
            .replace('ł', 'l')
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    private fun errorMessage(error: Throwable): String = when (error) {
        is ContentHttpException -> "Źródło odpowiedziało kodem HTTP ${error.statusCode}."
        else -> error.message?.takeIf(String::isNotBlank)
            ?: "Nie udało się pobrać danych ze źródła."
    }

    private data class SourceResult(
        val items: List<LocalContentItem>,
        val error: ContentSourceError?
    )

    private class ContentHttpException(
        val statusCode: Int,
        url: String
    ) : IllegalStateException("HTTP $statusCode dla $url")

    private companion object {
        const val USER_AGENT = "LegnickiRynek-Android/0.2"
        val POLISH_LOCALE = Locale("pl", "PL")
        val DIACRITIC_REGEX = Regex("\\p{Mn}+")
        val WHITESPACE_REGEX = Regex("\\s+")

        val NEWS_SOURCES = listOf(
            LocalContentSource(
                id = "legnica-city-news",
                name = "Oficjalny Portal Miasta Legnica",
                url = "https://portal.legnica.eu/rss/aktualnosci/35.xml",
                type = LocalContentType.NEWS
            ),
            LocalContentSource(
                id = "legnica-police-news",
                name = "Komenda Miejska Policji w Legnicy",
                url = "https://legnica.policja.gov.pl/dokumenty/rss/414-rss-o-2577.rss",
                type = LocalContentType.NEWS
            )
        )

        val EVENT_SOURCE = LocalContentSource(
            id = "legnica-city-events",
            name = "Kalendarz wydarzeń Miasta Legnica",
            url = "https://portal.legnica.eu/kalendarz-wydarzen/",
            type = LocalContentType.EVENT
        )
    }
}
