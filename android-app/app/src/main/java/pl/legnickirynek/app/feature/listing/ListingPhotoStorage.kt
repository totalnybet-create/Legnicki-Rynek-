package pl.legnickirynek.app.feature.listing

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface ListingPhotoStorage {
    suspend fun read(listingId: String): ListingGallery
    suspend fun write(listingId: String, gallery: ListingGallery)
    suspend fun delete(listingId: String)
}

@Singleton
class ListingPhotoFileStore @Inject constructor(
    @ApplicationContext context: Context
) : ListingPhotoStorage {

    private val storageFile = File(context.filesDir, FILE_NAME)
    private val temporaryFile = File(context.filesDir, "$FILE_NAME.tmp")
    private val mutex = Mutex()

    override suspend fun read(listingId: String): ListingGallery = withContext(Dispatchers.IO) {
        mutex.withLock {
            decodeGallery(readRoot().optJSONArray(listingId))
        }
    }

    override suspend fun write(listingId: String, gallery: ListingGallery) = withContext(Dispatchers.IO) {
        require(listingId.isNotBlank()) { "Identyfikator ogłoszenia nie może być pusty." }
        mutex.withLock {
            val root = readRoot()
            root.put(listingId, encodeGallery(gallery))
            writeRoot(root)
        }
    }

    override suspend fun delete(listingId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val root = readRoot()
            root.remove(listingId)
            writeRoot(root)
        }
    }

    private fun readRoot(): JSONObject {
        if (!storageFile.exists()) return JSONObject()
        return runCatching { JSONObject(storageFile.readText()) }
            .getOrElse { JSONObject() }
    }

    private fun writeRoot(root: JSONObject) {
        temporaryFile.writeText(root.toString())
        if (storageFile.exists() && !storageFile.delete()) {
            temporaryFile.delete()
            error("Nie udało się zastąpić magazynu zdjęć.")
        }
        if (!temporaryFile.renameTo(storageFile)) {
            temporaryFile.delete()
            error("Nie udało się zapisać magazynu zdjęć.")
        }
    }

    private fun encodeGallery(gallery: ListingGallery): JSONArray = JSONArray().apply {
        gallery.photos.sortedBy { it.position }.forEach { photo ->
            put(
                JSONObject()
                    .put("id", photo.id)
                    .put("uri", photo.uri)
                    .put("mimeType", photo.mimeType)
                    .put("position", photo.position)
                    .put("isCover", photo.isCover)
            )
        }
    }

    private fun decodeGallery(array: JSONArray?): ListingGallery {
        if (array == null) return ListingGallery()
        val photos = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id")
                val uri = item.optString("uri")
                val mimeType = item.optString("mimeType")
                if (id.isBlank() || uri.isBlank() || mimeType.isBlank()) continue
                add(
                    ListingPhoto(
                        id = id,
                        uri = uri,
                        mimeType = mimeType,
                        position = item.optInt("position", index),
                        isCover = item.optBoolean("isCover", index == 0)
                    )
                )
            }
        }
        return ListingGallery(photos.sortedBy { it.position })
    }

    private companion object {
        const val FILE_NAME = "listing_photos.json"
    }
}
