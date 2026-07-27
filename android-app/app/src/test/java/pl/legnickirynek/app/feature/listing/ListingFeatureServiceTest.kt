package pl.legnickirynek.app.feature.listing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.legnickirynek.app.core.database.ListingDao
import pl.legnickirynek.app.core.database.ListingEntity

class ListingFeatureServiceTest {

    @Test
    fun createUpdateFavoriteAndDeleteFormCompleteCycle() = runTest {
        val dao = FakeListingDao()
        val photoStorage = FakePhotoStorage()
        val service = ListingFeatureService(
            listingDao = dao,
            photoStorage = photoStorage,
            validator = ListingValidator(),
            clock = { 1_000L },
            idFactory = { "listing-test" }
        )
        val gallery = ListingGalleryManager().let { manager ->
            (manager.addPhoto(
                ListingGallery(),
                "content://images/cover",
                "image/jpeg"
            ) as GalleryMutationResult.Success).gallery
        }

        val created = service.create(validDraft(), gallery)
        assertTrue(created is ListingMutationResult.Success)
        assertEquals(1, dao.entities.value.size)
        assertEquals(gallery, photoStorage.read("listing-test"))
        assertEquals(1_000L, dao.entities.value.single().createdAt)

        val updated = service.update(
            listingId = "listing-test",
            draft = validDraft().copy(title = "Rower po przeglądzie", price = 1300)
        )
        assertTrue(updated is ListingMutationResult.Success)
        assertEquals("Rower po przeglądzie", dao.entities.value.single().title)
        assertEquals(1_000L, dao.entities.value.single().createdAt)
        assertFalse(dao.entities.value.single().isFavorite)

        val favorite = service.toggleFavorite("listing-test")
        assertTrue(favorite is ListingMutationResult.Success)
        assertTrue(dao.entities.value.single().isFavorite)

        val deleted = service.delete("listing-test")
        assertTrue(deleted is ListingMutationResult.Success)
        assertTrue(dao.entities.value.isEmpty())
        assertEquals(ListingGallery(), photoStorage.read("listing-test"))
    }

    @Test
    fun invalidCreateDoesNotWriteDatabaseOrPhotos() = runTest {
        val dao = FakeListingDao()
        val photoStorage = FakePhotoStorage()
        val service = ListingFeatureService(
            listingDao = dao,
            photoStorage = photoStorage,
            validator = ListingValidator(),
            clock = { 1_000L },
            idFactory = { "listing-invalid" }
        )

        val result = service.create(validDraft().copy(title = "x"))

        assertTrue(result is ListingMutationResult.ValidationFailed)
        assertTrue(dao.entities.value.isEmpty())
        assertEquals(ListingGallery(), photoStorage.read("listing-invalid"))
    }

    @Test
    fun updateAndDeleteReturnNotFoundForUnknownId() = runTest {
        val service = ListingFeatureService(
            listingDao = FakeListingDao(),
            photoStorage = FakePhotoStorage(),
            validator = ListingValidator(),
            clock = { 1_000L },
            idFactory = { "listing-test" }
        )

        assertTrue(service.update("missing", validDraft()) is ListingMutationResult.NotFound)
        assertTrue(service.delete("missing") is ListingMutationResult.NotFound)
        assertTrue(service.toggleFavorite("missing") is ListingMutationResult.NotFound)
    }

    private fun validDraft(): ListingDraft = ListingDraft(
        title = "Rower trekkingowy",
        price = 1250,
        location = "Legnica",
        categoryId = "sport",
        description = "Sprawny rower po pełnym przeglądzie technicznym."
    )

    private class FakePhotoStorage : ListingPhotoStorage {
        private val galleries = mutableMapOf<String, ListingGallery>()

        override suspend fun read(listingId: String): ListingGallery =
            galleries[listingId] ?: ListingGallery()

        override suspend fun write(listingId: String, gallery: ListingGallery) {
            galleries[listingId] = gallery
        }

        override suspend fun delete(listingId: String) {
            galleries.remove(listingId)
        }
    }

    private class FakeListingDao(
        initial: List<ListingEntity> = emptyList()
    ) : ListingDao {
        val entities = MutableStateFlow(initial)

        override fun observeAll(): Flow<List<ListingEntity>> = entities

        override suspend fun count(): Int = entities.value.size

        override suspend fun upsert(listing: ListingEntity) {
            entities.value = entities.value
                .filterNot { it.id == listing.id }
                .plus(listing)
                .sortedByDescending(ListingEntity::createdAt)
        }

        override suspend fun upsertAll(listings: List<ListingEntity>) {
            val byId = entities.value.associateBy { it.id }.toMutableMap()
            listings.forEach { byId[it.id] = it }
            entities.value = byId.values.sortedByDescending(ListingEntity::createdAt)
        }

        override suspend fun toggleFavorite(listingId: String) {
            entities.value = entities.value.map { entity ->
                if (entity.id == listingId) entity.copy(isFavorite = !entity.isFavorite) else entity
            }
        }

        override suspend fun deleteAll() {
            entities.value = emptyList()
        }
    }
}
