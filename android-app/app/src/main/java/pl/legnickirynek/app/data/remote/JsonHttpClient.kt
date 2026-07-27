package pl.legnickirynek.app.data.remote

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class JsonHttpResponse(
    val statusCode: Int,
    val body: String
)

class JsonHttpClient(
    private val connectTimeoutMillis: Int = 12_000,
    private val readTimeoutMillis: Int = 20_000,
    private val maxResponseChars: Int = 2_000_000
) {
    suspend fun request(
        method: String,
        url: String,
        body: String? = null,
        bearerToken: String = ""
    ): JsonHttpResponse = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method.uppercase()
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            instanceFollowRedirects = true
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty(
                "User-Agent",
                "LegnickiRynek-Android/0.1 (+https://github.com/totalnybet-create/Legnicki-Rynek-)"
            )
            if (bearerToken.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
            if (body != null) {
                doOutput = true
            }
        }

        try {
            if (body != null) {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(body)
                }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val result = StringBuilder()
                val buffer = CharArray(8_192)
                while (true) {
                    val read = reader.read(buffer)
                    if (read < 0) break
                    result.append(buffer, 0, read)
                    if (result.length > maxResponseChars) {
                        throw RemoteDataException("Odpowiedź API jest zbyt duża.")
                    }
                }
                result.toString()
            }.orEmpty()

            if (status !in 200..299) {
                throw RemoteDataException(
                    "API zwróciło błąd HTTP $status${responseBody.take(180).takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()}"
                )
            }

            JsonHttpResponse(statusCode = status, body = responseBody)
        } catch (error: RemoteDataException) {
            throw error
        } catch (error: Exception) {
            throw RemoteDataException("Nie udało się połączyć z API ogłoszeń.", error)
        } finally {
            connection.disconnect()
        }
    }
}
