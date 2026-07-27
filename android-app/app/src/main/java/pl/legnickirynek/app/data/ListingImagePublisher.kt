package pl.legnickirynek.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.InputStream
import pl.legnickirynek.app.data.remote.MultipartHttpClient
import pl.legnickirynek.app.data.remote.RemoteMediaService
import pl.legnickirynek.app.model.Listing

interface ListingImagePublisher {
    suspend fun publishLocalImages(listing: Listing): Listing
}

object PassThroughListingImagePublisher : ListingImagePublisher {
    override suspend fun publishLocalImages(listing: Listing): Listing = listing
}

class AndroidListingImagePublisher(
    context: Context,
    private val remoteMediaService: RemoteMediaService
) : ListingImagePublisher {
    private val contentResolver = context.applicationContext.contentResolver

    override suspend fun publishLocalImages(listing: Listing): Listing {
        if (!remoteMediaService.isConfigured) return listing
        if (listing.imageUris.none(::isLocalContentUri)) return listing

        val publishedUris = listing.imageUris
            .take(MAX_IMAGES_PER_LISTING)
            .mapIndexed { index, value ->
                if (!isLocalContentUri(value)) return@mapIndexed value

                runCatching {
                    val uri = Uri.parse(value)
                    val mimeType = contentResolver.getType(uri)
                        ?.takeIf { it.startsWith("image/") }
                        ?: throw IllegalArgumentException("Wybrany plik nie jest obrazem.")
                    val bytes = contentResolver.openInputStream(uri)?.use { input ->
                        readLimited(input)
                    } ?: throw IllegalArgumentException(
                        "Nie można odczytać wybranego zdjęcia."
                    )
                    remoteMediaService.uploadImage(
                        fileName = resolveFileName(uri, listing.id, index),
                        mimeType = mimeType,
                        bytes = bytes
                    )
                }.getOrDefault(value)
            }
            .distinct()

        return listing.copy(imageUris = publishedUris)
    }

    private fun readLimited(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MultipartHttpClient.MAX_UPLOAD_BYTES) {
                throw IllegalArgumentException("Zdjęcie przekracza limit 10 MB.")
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun resolveFileName(uri: Uri, listingId: String, index: Int): String {
        val displayName = runCatching {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column < 0) null else cursor.getString(column)
            }
        }.getOrNull()

        val safeListingId = listingId
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(60)
            .ifBlank { "listing" }

        return displayName
            ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
            ?.take(120)
            ?.takeIf(String::isNotBlank)
            ?: "$safeListingId-${index + 1}.jpg"
    }

    private fun isLocalContentUri(value: String): Boolean =
        value.startsWith("content://", ignoreCase = true)

    companion object {
        const val MAX_IMAGES_PER_LISTING = 12
    }
}
