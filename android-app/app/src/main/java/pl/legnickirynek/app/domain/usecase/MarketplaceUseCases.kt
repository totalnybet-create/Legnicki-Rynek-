package pl.legnickirynek.app.domain.usecase

import javax.inject.Inject
import pl.legnickirynek.app.domain.repository.MarketplaceRepository
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.UserProfile

class InitializeAppUseCase @Inject constructor(
    private val repository: MarketplaceRepository
) {
    suspend operator fun invoke() = repository.initialize()
}

class ObserveListingsUseCase @Inject constructor(
    private val repository: MarketplaceRepository
) {
    operator fun invoke() = repository.observeListings()
}

class ObserveProfileUseCase @Inject constructor(
    private val repository: MarketplaceRepository
) {
    operator fun invoke() = repository.observeProfile()
}

class AddListingUseCase @Inject constructor(
    private val repository: MarketplaceRepository
) {
    suspend operator fun invoke(listing: Listing) = repository.addListing(listing)
}

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: MarketplaceRepository
) {
    suspend operator fun invoke(listingId: String) = repository.toggleFavorite(listingId)
}

class UpdateProfileUseCase @Inject constructor(
    private val repository: MarketplaceRepository
) {
    suspend operator fun invoke(profile: UserProfile) = repository.updateProfile(profile)
}

class LogoutUseCase @Inject constructor(
    private val repository: MarketplaceRepository
) {
    suspend operator fun invoke() = repository.logout()
}
