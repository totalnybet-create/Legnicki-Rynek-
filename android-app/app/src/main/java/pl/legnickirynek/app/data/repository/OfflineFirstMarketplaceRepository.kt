package pl.legnickirynek.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pl.legnickirynek.app.core.database.ListingDao
import pl.legnickirynek.app.core.datastore.ProfileDataStore
import pl.legnickirynek.app.data.LocalStore
import pl.legnickirynek.app.data.SampleData
import pl.legnickirynek.app.data.mapper.toDomain
import pl.legnickirynek.app.data.mapper.toEntity
import pl.legnickirynek.app.domain.repository.MarketplaceRepository
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.UserProfile

@Singleton
class OfflineFirstMarketplaceRepository @Inject constructor(
    private val listingDao: ListingDao,
    private val profileDataStore: ProfileDataStore,
    @ApplicationContext private val context: Context
) : MarketplaceRepository {

    override fun observeListings(): Flow<List<Listing>> =
        listingDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeProfile(): Flow<UserProfile> = profileDataStore.profile

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        if (profileDataStore.isLegacyListingsMigrated()) {
            return@withContext
        }

        if (listingDao.count() == 0) {
            val legacyListings = LocalStore.loadListings(context)
            val listingsToStore = legacyListings.ifEmpty { SampleData.listings }
            val migrationTimestamp = System.currentTimeMillis()

            listingDao.replaceAll(
                listingsToStore.mapIndexed { index, listing ->
                    listing.toEntity(createdAt = migrationTimestamp - index)
                }
            )
        }

        profileDataStore.markLegacyListingsMigrated()
        LocalStore.clearListings(context)
    }

    override suspend fun addListing(listing: Listing) {
        listingDao.upsert(listing.toEntity())
    }

    override suspend fun toggleFavorite(listingId: String) {
        listingDao.toggleFavorite(listingId)
    }

    override suspend fun updateProfile(profile: UserProfile) {
        profileDataStore.saveProfile(profile)
    }

    override suspend fun logout() {
        profileDataStore.saveProfile(UserProfile())
    }
}
