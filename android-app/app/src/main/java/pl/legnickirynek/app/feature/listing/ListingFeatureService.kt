package pl.legnickirynek.app.feature.listing

import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import pl.legnickirynek.app.core.database.ListingDao
import pl.legnickirynek.app.core.database.ListingEntity
import pl.legnickirynek.app.data.mapper.toDomain
import pl.legnickirynek.app.model.Listing

class ListingFeatureService internal constructor(
    private val listingDao: ListingDao,
    private val photoStorage: ListingPhotoStorage,
    private val validator: ListingValidator,
    private val clock: () -> Long,
    private val idFactory: () -> String
) {

    @Inject
    constructor(
        listingDao: ListingDao,
        photoFileStore: ListingPhotoFileStore,
        validator: ListingValidator
    ) : this(
        listingDao = listingDao,
        photoStorage = photoFileStore,
        validator = validator,
        clock = System::currentTimeMillis,
        idFactory = { "listing-${UUID.randomUUID()}" }
    )

    suspend fun create(
        draft: ListingDraft,
        gallery: ListingGallery = ListingGallery()
    ): ListingMutationResult {
        val validation = validator.validate(draft, gallery)
        val normalizedDraft = validation.normalizedDraft
            ?: return ListingMutationResult.ValidationFailed(validation.issues)
        val current = listingDao.observeAll().first()
        val listingId = generateUniqueId(current)
        val entity = ListingEntity(
            id = listingId,
            title = normalizedDraft.title,
            price = normalizedDraft.price,
            location = normalizedDraft.location,
            categoryId = normalizedDraft.categoryId,
            description = normalizedDraft.description,
            isFavorite = false,
            createdAt = clock()
        )

        return runCatching {
            listingDao.upsert(entity)
            photoStorage.write(entity.id, validation.normalizedGallery)
            ListingMutationResult.Success(entity.toDomain(), validation.normalizedGallery)
        }.getOrElse { error ->
            runCatching { listingDao.replaceAll(current) }
            runCatching { photoStorage.delete(entity.id) }
            ListingMutationResult.Failure(
                message = "Nie udało się utworzyć ogłoszenia.",
                cause = error
            )
        }
    }

    suspend fun update(
        listingId: String,
        draft: ListingDraft,
        gallery: ListingGallery? = null
    ): ListingMutationResult {
        val current = listingDao.observeAll().first()
        val previous = current.firstOrNull { it.id == listingId }
            ?: return ListingMutationResult.NotFound(listingId)
        val previousGallery = photoStorage.read(listingId)
        val targetGallery = gallery ?: previousGallery
        val validation = validator.validate(draft, targetGallery)
        val normalizedDraft = validation.normalizedDraft
            ?: return ListingMutationResult.ValidationFailed(validation.issues)
        val updated = previous.copy(
            title = normalizedDraft.title,
            price = normalizedDraft.price,
            location = normalizedDraft.location,
            categoryId = normalizedDraft.categoryId,
            description = normalizedDraft.description
        )

        return runCatching {
            listingDao.upsert(updated)
            if (gallery != null) {
                photoStorage.write(listingId, validation.normalizedGallery)
            }
            ListingMutationResult.Success(updated.toDomain(), validation.normalizedGallery)
        }.getOrElse { error ->
            runCatching { listingDao.upsert(previous) }
            runCatching { photoStorage.write(listingId, previousGallery) }
            ListingMutationResult.Failure(
                message = "Nie udało się zaktualizować ogłoszenia.",
                cause = error
            )
        }
    }

    suspend fun delete(listingId: String): ListingMutationResult {
        val current = listingDao.observeAll().first()
        val removed = current.firstOrNull { it.id == listingId }
            ?: return ListingMutationResult.NotFound(listingId)
        val previousGallery = photoStorage.read(listingId)
        val remaining = current.filterNot { it.id == listingId }

        return runCatching {
            listingDao.replaceAll(remaining)
            photoStorage.delete(listingId)
            ListingMutationResult.Success(removed.toDomain(), previousGallery)
        }.getOrElse { error ->
            runCatching { listingDao.replaceAll(current) }
            runCatching { photoStorage.write(listingId, previousGallery) }
            ListingMutationResult.Failure(
                message = "Nie udało się usunąć ogłoszenia.",
                cause = error
            )
        }
    }

    suspend fun toggleFavorite(listingId: String): ListingMutationResult {
        val current = listingDao.observeAll().first()
        val previous = current.firstOrNull { it.id == listingId }
            ?: return ListingMutationResult.NotFound(listingId)

        return runCatching {
            listingDao.toggleFavorite(listingId)
            val updated = previous.copy(isFavorite = !previous.isFavorite)
            ListingMutationResult.Success(updated.toDomain(), photoStorage.read(listingId))
        }.getOrElse { error ->
            ListingMutationResult.Failure(
                message = "Nie udało się zmienić stanu ulubionych.",
                cause = error
            )
        }
    }

    suspend fun gallery(listingId: String): ListingGallery = photoStorage.read(listingId)

    private fun generateUniqueId(current: List<ListingEntity>): String {
        val existingIds = current.asSequence().map { it.id }.toHashSet()
        repeat(MAX_ID_ATTEMPTS) {
            val candidate = idFactory().trim()
            if (candidate.isNotBlank() && candidate !in existingIds) return candidate
        }
        error("Nie udało się wygenerować unikalnego identyfikatora ogłoszenia.")
    }

    private companion object {
        const val MAX_ID_ATTEMPTS = 10
    }
}

fun Listing.toDraft(): ListingDraft = ListingDraft(
    title = title,
    price = price,
    location = location,
    categoryId = categoryId,
    description = description
)
