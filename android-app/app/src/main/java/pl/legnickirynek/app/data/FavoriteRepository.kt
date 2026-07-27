package pl.legnickirynek.app.data

import kotlinx.coroutines.flow.Flow
import pl.legnickirynek.app.data.local.FavoriteDao
import pl.legnickirynek.app.data.local.FavoriteEntity

interface FavoriteRepository {
    fun observeFavoriteListingIds(accountId: String): Flow<List<String>>
    suspend fun setFavorite(accountId: String, listingId: String, favorite: Boolean)
    suspend fun claimLegacyFavorites(accountId: String)
}

class OfflineFavoriteRepository(
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {
    override fun observeFavoriteListingIds(accountId: String): Flow<List<String>> {
        requireAccountId(accountId)
        return favoriteDao.observeFavoriteListingIds(accountId)
    }

    override suspend fun setFavorite(
        accountId: String,
        listingId: String,
        favorite: Boolean
    ) {
        requireAccountId(accountId)
        require(listingId.isNotBlank()) { "Identyfikator ogłoszenia nie może być pusty." }

        if (favorite) {
            favoriteDao.upsert(
                FavoriteEntity(
                    accountId = accountId,
                    listingId = listingId
                )
            )
        } else {
            favoriteDao.delete(accountId, listingId)
        }
    }

    override suspend fun claimLegacyFavorites(accountId: String) {
        requireAccountId(accountId)
        favoriteDao.claimLegacyFavorites(accountId)
    }

    private fun requireAccountId(accountId: String) {
        require(accountId.isNotBlank()) { "Identyfikator konta nie może być pusty." }
    }
}
