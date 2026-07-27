package pl.legnickirynek.app.feature.listing

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject

class ListingGalleryManager @Inject constructor() {

    fun addPhoto(
        gallery: ListingGallery,
        uri: String,
        mimeType: String
    ): GalleryMutationResult {
        val normalizedUri = uri.trim()
        val normalizedMimeType = mimeType.trim().lowercase()

        if (gallery.photos.size >= ListingValidator.MAX_PHOTOS) {
            return GalleryMutationResult.Failure(
                "Ogłoszenie może zawierać maksymalnie ${ListingValidator.MAX_PHOTOS} zdjęć."
            )
        }
        if (gallery.photos.any { it.uri == normalizedUri }) {
            return GalleryMutationResult.Failure("To zdjęcie jest już w galerii.")
        }
        if (normalizedUri.isBlank()) {
            return GalleryMutationResult.Failure("Adres zdjęcia nie może być pusty.")
        }

        val photo = ListingPhoto(
            id = stablePhotoId(normalizedUri),
            uri = normalizedUri,
            mimeType = normalizedMimeType,
            position = gallery.photos.size,
            isCover = gallery.photos.isEmpty()
        )
        return GalleryMutationResult.Success(
            sanitize(ListingGallery(gallery.photos + photo))
        )
    }

    fun removePhoto(
        gallery: ListingGallery,
        photoId: String
    ): GalleryMutationResult {
        if (gallery.photos.none { it.id == photoId }) {
            return GalleryMutationResult.Failure("Nie znaleziono zdjęcia w galerii.")
        }
        return GalleryMutationResult.Success(
            sanitize(
                ListingGallery(gallery.photos.filterNot { it.id == photoId })
            )
        )
    }

    fun movePhoto(
        gallery: ListingGallery,
        fromIndex: Int,
        toIndex: Int
    ): GalleryMutationResult {
        if (fromIndex !in gallery.photos.indices || toIndex !in gallery.photos.indices) {
            return GalleryMutationResult.Failure("Nieprawidłowa pozycja zdjęcia.")
        }
        if (fromIndex == toIndex) {
            return GalleryMutationResult.Success(sanitize(gallery))
        }

        val mutable = gallery.photos.sortedBy { it.position }.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        return GalleryMutationResult.Success(sanitize(ListingGallery(mutable)))
    }

    fun setCover(
        gallery: ListingGallery,
        photoId: String
    ): GalleryMutationResult {
        if (gallery.photos.none { it.id == photoId }) {
            return GalleryMutationResult.Failure("Nie znaleziono zdjęcia głównego.")
        }
        return GalleryMutationResult.Success(
            sanitize(
                ListingGallery(
                    gallery.photos.map { photo ->
                        photo.copy(isCover = photo.id == photoId)
                    }
                )
            )
        )
    }

    fun sanitize(gallery: ListingGallery): ListingGallery {
        if (gallery.photos.isEmpty()) return ListingGallery()

        val ordered = gallery.photos
            .distinctBy { it.uri.trim() }
            .take(ListingValidator.MAX_PHOTOS)
            .sortedBy { it.position }
        val requestedCoverId = ordered.firstOrNull { it.isCover }?.id ?: ordered.first().id

        return ListingGallery(
            ordered.mapIndexed { index, photo ->
                photo.copy(
                    uri = photo.uri.trim(),
                    mimeType = photo.mimeType.trim().lowercase(),
                    position = index,
                    isCover = photo.id == requestedCoverId
                )
            }
        )
    }

    private fun stablePhotoId(uri: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(uri.toByteArray(StandardCharsets.UTF_8))
        return digest.take(12).joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
