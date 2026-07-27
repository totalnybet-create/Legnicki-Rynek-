package pl.legnickirynek.app.data

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.legnickirynek.app.data.remote.TextHttpClient
import pl.legnickirynek.app.model.GeoPoint

interface GeocodingRepository {
    suspend fun geocode(location: String): GeoPoint?
}

class NominatimGeocodingRepository(
    private val httpClient: TextHttpClient
) : GeocodingRepository {
    private val requestMutex = Mutex()
    private var lastRequestAtMillis: Long = 0L

    override suspend fun geocode(location: String): GeoPoint? {
        val cleanLocation = location.trim().take(180)
        if (cleanLocation.isBlank()) return null

        return requestMutex.withLock {
            val now = System.currentTimeMillis()
            val waitMillis = MIN_REQUEST_INTERVAL_MILLIS - (now - lastRequestAtMillis)
            if (waitMillis > 0) delay(waitMillis)

            try {
                val query = if (cleanLocation.contains("Legnica", ignoreCase = true)) {
                    cleanLocation
                } else {
                    "$cleanLocation, Legnica, Polska"
                }
                NominatimParser.parseFirst(httpClient.get(searchUrl(query)))
            } finally {
                lastRequestAtMillis = System.currentTimeMillis()
            }
        }
    }

    companion object {
        private const val MIN_REQUEST_INTERVAL_MILLIS = 1_100L
        private const val LEGNICA_VIEWBOX = "15.95,51.33,16.36,51.08"

        fun searchUrl(query: String): String {
            val encodedQuery = URLEncoder.encode(
                query,
                StandardCharsets.UTF_8.name()
            )
            return "https://nominatim.openstreetmap.org/search" +
                "?q=$encodedQuery" +
                "&format=jsonv2" +
                "&limit=1" +
                "&countrycodes=pl" +
                "&accept-language=pl" +
                "&viewbox=$LEGNICA_VIEWBOX"
        }
    }
}

object NominatimParser {
    fun parseFirst(json: String): GeoPoint? {
        if (json.isBlank() || json.trim() == "[]") return null
        val firstObject = firstJsonObject(json) ?: return null
        val latitude = stringValue(firstObject, "lat")?.toDoubleOrNull() ?: return null
        val longitude = stringValue(firstObject, "lon")?.toDoubleOrNull() ?: return null
        val displayName = stringValue(firstObject, "display_name").orEmpty()

        return runCatching {
            GeoPoint(
                latitude = latitude,
                longitude = longitude,
                displayName = displayName
            )
        }.getOrNull()
    }

    private fun firstJsonObject(json: String): String? {
        val start = json.indexOf('{')
        if (start < 0) return null

        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until json.length) {
            val char = json[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
                continue
            }

            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return json.substring(start, index + 1)
                }
            }
        }
        return null
    }

    private fun stringValue(json: String, key: String): String? {
        val match = Regex(
            "\"${Regex.escape(key)}\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
        ).find(json) ?: return null
        return decodeJsonString(match.groupValues[1])
    }

    private fun decodeJsonString(value: String): String = value
        .replace("\\/", "/")
        .replace("\\\"", "\"")
        .replace("\\n", " ")
        .replace("\\r", " ")
        .replace("\\t", " ")
        .replace("\\\\", "\\")
        .replace(Regex("\\s+"), " ")
        .trim()
}
