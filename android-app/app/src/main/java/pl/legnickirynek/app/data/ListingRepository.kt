package pl.legnickirynek.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.legnickirynek.app.data.local.ListingDao
import pl.legnickirynek.app.data.local.toEntity
import pl.legnickirynek.app.data.local.toModel
import pl.legnickirynek.app.model.Listing

interface ListingRepository {
    fun observeListings(): Flow<List<Listing>>
    fun observeListing(id: String): Flow<Listing?>
    suspend fun upsert(listing: Listing)
    suspend fun upsertAll(listings: List<Listing>)
    suspend fun delete(id: String)
    suspend fun count(): Int
}

class OfflineListingRepository(
    private val listingDao: ListingDao
) : ListingRepository {
    override fun observeListings(): Flow<List<Listing>> =
        listingDao.observeAll().map { entities -> entities.map { it.toModel() } }

    override fun observeListing(id: String): Flow<Listing?> =
        listingDao.observeById(id).map { it?.toModel() }

    override suspend fun upsert(listing: Listing) {
        listingDao.upsert(listing.toEntity())
    }

    override suspend fun upsertAll(listings: List<Listing>) {
        listingDao.upsertAll(listings.map { it.toEntity() })
    }

    override suspend fun delete(id: String) {
        listingDao.deleteById(id)
    }

    override suspend fun count(): Int = listingDao.count()
}
