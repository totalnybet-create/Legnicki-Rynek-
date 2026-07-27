package pl.legnickirynek.app.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface TextHttpClient {
    suspend fun get(url: String): String
}

class RemoteDataException(
    message: String,
    cause: Throwable? = null
) : IOException(message, cause)

class UrlConnectionTextHttpClient(
    private val connectTimeoutMillis: Int = 12_000,
    private val readTimeoutMillis: Int = 15_000,
    private val maxResponseChars: Int = 1_000_000
) : TextHttpClient {
    override suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            instanceFollowRedirects = true
            useCaches = false
            setRequestProperty("Accept", "application/json, application/rss+xml, application/xml, text/xml, text/plain")
            setRequestProperty("User-Agent", "LegnickiRynek-Android/0.1")
        }

        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val result = StringBuilder()
                val buffer = CharArray(8_192)
                while (true) {
                    val read = reader.read(buffer)
                    if (read < 0) break
                    result.append(buffer, 0, read)
                    if (result.length > maxResponseChars) {
                        throw RemoteDataException("Odpowiedź serwera jest zbyt duża.")
                    }
                }
                result.toString()
            }.orEmpty()

            if (status !in 200..299) {
                throw RemoteDataException(
                    "Serwer zwrócił błąd HTTP $status${body.take(160).takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()}"
                )
            }
            if (body.isBlank()) {
                throw RemoteDataException("Serwer zwrócił pustą odpowiedź.")
            }
            body
        } catch (error: RemoteDataException) {
            throw error
        } catch (error: Exception) {
            throw RemoteDataException("Nie udało się pobrać danych z sieci.", error)
        } finally {
            connection.disconnect()
        }
    }
}
