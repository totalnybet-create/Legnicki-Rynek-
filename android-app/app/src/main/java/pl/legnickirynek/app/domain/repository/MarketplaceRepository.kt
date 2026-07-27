package pl.legnickirynek.app.domain.repository

import kotlinx.coroutines.flow.Flow
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.UserProfile

interface MarketplaceRepository {
    fun observeListings(): Flow<List<Listing>>

    fun observeProfile(): Flow<UserProfile>

    suspend fun initialize()

    suspend fun addListing(listing: Listing)

    suspend fun toggleFavorite(listingId: String)

    suspend fun updateProfile(profile: UserProfile)

    suspend fun logout()
}
