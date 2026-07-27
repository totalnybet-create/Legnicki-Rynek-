package pl.legnickirynek.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.legnickirynek.app.data.remote.RemoteListingService
import pl.legnickirynek.app.data.remote.RemoteMediaService
import pl.legnickirynek.app.model.Listing

class SyncingListingRepository(
    private val localRepository: ListingRepository,
    private val remoteService: RemoteListingService,
    private val syncStore: ListingSyncStateStore,
    private val imagePublisher: ListingImagePublisher = defaultImagePublisher(
        syncStore = syncStore,
        remoteService = remoteService
    )
) : ListingRepository {
    private val syncMutex = Mutex()

    override val remoteSyncEnabled: Boolean
        get() = remoteService.isConfigured

    override fun observeListings(): Flow<List<Listing>> =
        localRepository.observeListings()

    override fun observeListing(id: String): Flow<Listing?> =
        localRepository.observeListing(id)

    override suspend fun getAll(): List<Listing> =
        localRepository.getAll()

    override suspend fun upsert(listing: Listing) {
        localRepository.upsert(listing)
        if (remoteSyncEnabled) {
            runCatching {
                remoteService.upsertListing(prepareForRemote(listing))
            }
        }
    }

    override suspend fun upsertAll(listings: List<Listing>) {
        localRepository.upsertAll(listings)
        if (remoteSyncEnabled) {
            listings.forEach { listing ->
                runCatching {
                    remoteService.upsertListing(prepareForRemote(listing))
                }
            }
        }
    }

    override suspend fun delete(id: String) {
        syncStore.addPendingDeletion(id)
        localRepository.delete(id)

        if (remoteSyncEnabled) {
            runCatching { remoteService.deleteListing(id) }
                .onSuccess { syncStore.removePendingDeletion(id) }
        }
    }

    override suspend fun claimLegacyListings(ownerId: String, sellerName: String) {
        localRepository.claimLegacyListings(ownerId, sellerName)
    }

    override suspend fun count(): Int = localRepository.count()

    override suspend fun synchronize(): ListingSyncReport = syncMutex.withLock {
        if (!remoteSyncEnabled) return@withLock ListingSyncReport.Disabled

        var pulled = 0
        var pushed = 0
        var deletedRemotely = 0
        val errors = mutableListOf<String>()

        val pendingAtStart = syncStore.pendingDeletionIds()
        pendingAtStart.forEach { id ->
            runCatching { remoteService.deleteListing(id) }
                .onSuccess {
                    syncStore.removePendingDeletion(id)
                    deletedRemotely++
                }
                .onFailure { error ->
                    errors += error.message
                        ?: "Nie udało się zsynchronizować usunięcia ogłoszenia."
                }
        }

        val unresolvedDeletionIds = syncStore.pendingDeletionIds()
        val remoteListings = runCatching { remoteService.fetchListings() }
            .getOrElse { error ->
                errors += error.message ?: "Nie udało się pobrać ogłoszeń z API."
                return@withLock ListingSyncReport(
                    enabled = true,
                    deletedRemotely = deletedRemotely,
                    errors = errors.distinct()
                )
            }
            .filterNot { it.id in unresolvedDeletionIds }
        val localListings = localRepository.getAll()
            .filterNot { it.id in unresolvedDeletionIds }
        val localById = localListings.associateBy(Listing::id)
        val remoteById = remoteListings.associateBy(Listing::id)
        val allIds = (localById.keys + remoteById.keys).sorted()

        for (id in allIds) {
            val local = localById[id]
            val remote = remoteById[id]

            when {
                local == null && remote != null -> {
                    runCatching { localRepository.upsert(remote) }
                        .onSuccess { pulled++ }
                        .onFailure { error ->
                            errors += error.message
                                ?: "Nie udało się zapisać ogłoszenia z API."
                        }
                }

                local != null && remote == null -> {
                    runCatching {
                        remoteService.upsertListing(prepareForRemote(local))
                    }
                        .onSuccess { pushed++ }
                        .onFailure { error ->
                            errors += error.message
                                ?: "Nie udało się wysłać ogłoszenia do API."
                        }
                }

                local != null && remote != null && remote.updatedAt > local.updatedAt -> {
                    runCatching {
                        val merged = mergeRemoteIntoLocal(remote = remote, local = local)
                        val prepared = prepareForRemote(merged)
                        localRepository.upsert(prepared)
                        if (prepared.imageUris.none(::isLocalContentUri) &&
                            prepared.imageUris != remote.imageUris
                        ) {
                            remoteService.upsertListing(prepared)
                            pushed++
                        }
                    }
                        .onSuccess { pulled++ }
                        .onFailure { error ->
                            errors += error.message
                                ?: "Nie udało się zaktualizować lokalnego ogłoszenia."
                        }
                }

                local != null && remote != null && local.updatedAt > remote.updatedAt -> {
                    runCatching {
                        remoteService.upsertListing(prepareForRemote(local))
                    }
                        .onSuccess { pushed++ }
                        .onFailure { error ->
                            errors += error.message
                                ?: "Nie udało się zaktualizować ogłoszenia w API."
                        }
                }

                local != null && remote != null &&
                    local.updatedAt == remote.updatedAt &&
                    local.imageUris.any(::isLocalContentUri) -> {
                    runCatching {
                        remoteService.upsertListing(prepareForRemote(local))
                    }
                        .onSuccess { pushed++ }
                        .onFailure { error ->
                            errors += error.message
                                ?: "Nie udało się opublikować zdjęć ogłoszenia."
                        }
                }
            }
        }

        if (errors.isEmpty()) {
            syncStore.markSuccessfulSync()
        }
        ListingSyncReport(
            enabled = true,
            pulled = pulled,
            pushed = pushed,
            deletedRemotely = deletedRemotely,
            errors = errors.distinct()
        )
    }

    private suspend fun prepareForRemote(listing: Listing): Listing {
        val prepared = imagePublisher.publishLocalImages(listing)
        if (prepared != listing) {
            localRepository.upsert(prepared)
        }
        return prepared
    }

    private fun mergeRemoteIntoLocal(remote: Listing, local: Listing): Listing {
        val localOnlyImages = local.imageUris.filterNot(::isRemoteImageUrl)
        return remote.copy(
            imageUris = (remote.imageUris + localOnlyImages).distinct().take(12),
            isFavorite = local.isFavorite
        )
    }

    private fun isRemoteImageUrl(value: String): Boolean =
        value.startsWith("https://", ignoreCase = true) ||
            value.startsWith("http://", ignoreCase = true)

    private fun isLocalContentUri(value: String): Boolean =
        value.startsWith("content://", ignoreCase = true)

    companion object {
        private fun defaultImagePublisher(
            syncStore: ListingSyncStateStore,
            remoteService: RemoteListingService
        ): ListingImagePublisher = if (
            syncStore is ListingSyncStore && remoteService is RemoteMediaService
        ) {
            AndroidListingImagePublisher(
                context = syncStore.appContext,
                remoteMediaService = remoteService
            )
        } else {
            PassThroughListingImagePublisher
        }
    }
}
