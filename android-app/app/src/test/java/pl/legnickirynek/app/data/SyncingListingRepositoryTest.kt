package pl.legnickirynek.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.legnickirynek.app.data.remote.RemoteListingService
import pl.legnickirynek.app.model.Listing

class SyncingListingRepositoryTest {
    @Test
    fun `nowsza wersja z api zachowuje lokalne ulubione i zdjęcia z galerii`() = runBlocking {
        val localListing = listing(
            title = "Stary tytuł",
            updatedAt = 100,
            imageUris = listOf("content://gallery/1"),
            isFavorite = true
        )
        val remoteListing = listing(
            title = "Nowy tytuł",
            updatedAt = 200,
            imageUris = listOf("https://cdn.example.pl/1.jpg")
        )
        val local = FakeListingRepository(listOf(localListing))
        val remote = FakeRemoteListingService(listOf(remoteListing))
        val repository = SyncingListingRepository(local, remote, FakeSyncStore())

        val report = repository.synchronize()
        val merged = local.getAll().single()

        assertTrue(report.successful)
        assertEquals(1, report.pulled)
        assertEquals("Nowy tytuł", merged.title)
        assertTrue(merged.isFavorite)
        assertEquals(
            listOf("https://cdn.example.pl/1.jpg", "content://gallery/1"),
            merged.imageUris
        )
    }

    @Test
    fun `nowsza wersja lokalna jest wysyłana do api`() = runBlocking {
        val localListing = listing(title = "Lokalna", updatedAt = 300)
        val remoteListing = listing(title = "Zdalna", updatedAt = 200)
        val local = FakeListingRepository(listOf(localListing))
        val remote = FakeRemoteListingService(listOf(remoteListing))
        val repository = SyncingListingRepository(local, remote, FakeSyncStore())

        val report = repository.synchronize()

        assertEquals(1, report.pushed)
        assertEquals("Lokalna", remote.listings.single().title)
    }

    @Test
    fun `oczekujące usunięcie nie jest ponownie pobierane z api`() = runBlocking {
        val id = "listing-1"
        val local = FakeListingRepository(emptyList())
        val remote = FakeRemoteListingService(listOf(listing(id = id)))
        val store = FakeSyncStore(mutableSetOf(id))
        val repository = SyncingListingRepository(local, remote, store)

        val report = repository.synchronize()

        assertEquals(1, report.deletedRemotely)
        assertTrue(local.getAll().isEmpty())
        assertTrue(remote.listings.isEmpty())
        assertTrue(store.pendingDeletionIds().isEmpty())
    }

    @Test
    fun `brak konfiguracji api pozostawia tryb offline`() = runBlocking {
        val remote = FakeRemoteListingService(emptyList(), configured = false)
        val repository = SyncingListingRepository(
            FakeListingRepository(emptyList()),
            remote,
            FakeSyncStore()
        )

        val report = repository.synchronize()

        assertFalse(report.enabled)
        assertFalse(report.successful)
    }

    private fun listing(
        id: String = "listing-1",
        title: String = "Rower",
        updatedAt: Long = 100,
        imageUris: List<String> = emptyList(),
        isFavorite: Boolean = false
    ) = Listing(
        id = id,
        title = title,
        price = 500,
        location = "Legnica",
        categoryId = "sport",
        description = "Sprawny przedmiot w dobrym stanie.",
        imageUris = imageUris,
        createdAt = 50,
        updatedAt = updatedAt,
        isFavorite = isFavorite
    )

    private class FakeListingRepository(initial: List<Listing>) : ListingRepository {
        private val state = MutableStateFlow(initial)

        override fun observeListings(): Flow<List<Listing>> = state
        override fun observeListing(id: String): Flow<Listing?> =
            MutableStateFlow(state.value.firstOrNull { it.id == id })

        override suspend fun getAll(): List<Listing> = state.value

        override suspend fun upsert(listing: Listing) {
            state.value = listOf(listing) + state.value.filterNot { it.id == listing.id }
        }

        override suspend fun upsertAll(listings: List<Listing>) {
            listings.forEach { upsert(it) }
        }

        override suspend fun delete(id: String) {
            state.value = state.value.filterNot { it.id == id }
        }

        override suspend fun claimLegacyListings(ownerId: String, sellerName: String) = Unit
        override suspend fun count(): Int = state.value.size
    }

    private class FakeRemoteListingService(
        initial: List<Listing>,
        private val configured: Boolean = true
    ) : RemoteListingService {
        val listings = initial.toMutableList()

        override val isConfigured: Boolean
            get() = configured

        override suspend fun fetchListings(): List<Listing> = listings.toList()

        override suspend fun upsertListing(listing: Listing) {
            listings.removeAll { it.id == listing.id }
            listings += listing.copy(isFavorite = false)
        }

        override suspend fun deleteListing(id: String) {
            listings.removeAll { it.id == id }
        }
    }

    private class FakeSyncStore(
        private val pending: MutableSet<String> = mutableSetOf()
    ) : ListingSyncStateStore {
        private var lastSync = 0L

        override suspend fun pendingDeletionIds(): Set<String> = pending.toSet()
        override suspend fun addPendingDeletion(id: String) {
            pending += id
        }
        override suspend fun removePendingDeletion(id: String) {
            pending -= id
        }
        override suspend fun markSuccessfulSync(timestamp: Long) {
            lastSync = timestamp
        }
        override suspend fun lastSuccessfulSync(): Long = lastSync
    }
}
