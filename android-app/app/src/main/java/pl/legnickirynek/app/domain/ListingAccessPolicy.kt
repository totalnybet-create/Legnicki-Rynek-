package pl.legnickirynek.app.domain

import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.UserProfile

object ListingAccessPolicy {
    const val LOCAL_OWNER_ID = "local-device"

    fun ownerIdFor(profile: UserProfile): String {
        if (!profile.loggedIn) return LOCAL_OWNER_ID

        return profile.id
            .ifBlank { UserIdentity.fromEmail(profile.email) }
            .ifBlank { LOCAL_OWNER_ID }
    }

    fun sellerNameFor(profile: UserProfile, fallback: String): String =
        profile.name
            .takeIf { profile.loggedIn }
            .orEmpty()
            .trim()
            .ifBlank { fallback.trim().ifBlank { "Użytkownik" } }

    fun canManage(listing: Listing, profile: UserProfile): Boolean {
        val currentOwnerId = ownerIdFor(profile)
        if (listing.ownerId == currentOwnerId) return true

        return listing.ownerId.isBlank() && listing.id.startsWith("listing-")
    }
}
