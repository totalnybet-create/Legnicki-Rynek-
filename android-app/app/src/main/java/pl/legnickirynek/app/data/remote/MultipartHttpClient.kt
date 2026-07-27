package pl.legnickirynek.app.data.remote

import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MultipartFile(
    val fieldName: String,
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
)

class MultipartHttpClient(
    private val connectTimeoutMillis: Int = 15_000,
    private val readTimeoutMillis: Int = 30_000,
    private val maxResponseChars: Int = 200_000
) {
    suspend fun upload(
        url: String,
        file: MultipartFile,
        bearerToken: String = ""
    ): JsonHttpResponse = withContext(Dispatchers.IO) {
        require(file.bytes.isNotEmpty()) { "Plik zdjęcia jest pusty." }
        require(file.bytes.size <= MAX_UPLOAD_BYTES) { "Zdjęcie przekracza limit 10 MB." }
        require(file.mimeType.startsWith("image/")) { "Dozwolone są wyłącznie obrazy." }

        val boundary = "LegnickiRynek-${UUID.randomUUID()}"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            instanceFollowRedirects = true
            useCaches = false
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty(
                "User-Agent",
                "LegnickiRynek-Android/0.1 (+https://github.com/totalnybet-create/Legnicki-Rynek-)"
            )
            if (bearerToken.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
        }

        try {
            DataOutputStream(connection.outputStream).use { output ->
                output.writeBytes("--$boundary\r\n")
                output.writeBytes(
                    "Content-Disposition: form-data; name=\"${safeHeader(file.fieldName)}\"; " +
                        "filename=\"${safeHeader(file.fileName)}\"\r\n"
                )
                output.writeBytes("Content-Type: ${safeHeader(file.mimeType)}\r\n")
                output.writeBytes("Content-Transfer-Encoding: binary\r\n\r\n")
                output.write(file.bytes)
                output.writeBytes("\r\n--$boundary--\r\n")
                output.flush()
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val result = StringBuilder()
                val buffer = CharArray(4_096)
                while (true) {
                    val read = reader.read(buffer)
                    if (read < 0) break
                    result.append(buffer, 0, read)
                    if (result.length > maxResponseChars) {
                        throw RemoteDataException("Odpowiedź uploadu jest zbyt duża.")
                    }
                }
                result.toString()
            }.orEmpty()

            if (status !in 200..299) {
                throw RemoteDataException(
                    "Upload zdjęcia zwrócił błąd HTTP $status${responseBody.take(180).takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()}"
                )
            }
            if (responseBody.isBlank()) {
                throw RemoteDataException("Serwer uploadu zwrócił pustą odpowiedź.")
            }
            JsonHttpResponse(statusCode = status, body = responseBody)
        } catch (error: RemoteDataException) {
            throw error
        } catch (error: Exception) {
            throw RemoteDataException("Nie udało się wysłać zdjęcia.", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun safeHeader(value: String): String = value
        .replace("\r", "")
        .replace("\n", "")
        .replace("\"", "'")
        .take(180)

    companion object {
        const val MAX_UPLOAD_BYTES = 10 * 1024 * 1024
    }
}
