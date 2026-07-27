package pl.legnickirynek.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.UserProfile

class ListingAccessPolicyTest {
    @Test
    fun `zalogowany profil używa identyfikatora utworzonego z emaila`() {
        val profile = UserProfile(
            name = "Jan",
            email = "JAN@example.pl",
            loggedIn = true
        )

        assertEquals(
            UserIdentity.fromEmail("jan@example.pl"),
            ListingAccessPolicy.ownerIdFor(profile)
        )
    }

    @Test
    fun `profil lokalny otrzymuje stały identyfikator urządzenia`() {
        assertEquals(
            ListingAccessPolicy.LOCAL_OWNER_ID,
            ListingAccessPolicy.ownerIdFor(UserProfile())
        )
    }

    @Test
    fun `właściciel może zarządzać swoim ogłoszeniem`() {
        val profile = authenticatedProfile()
        val listing = listing(ownerId = ListingAccessPolicy.ownerIdFor(profile))

        assertTrue(ListingAccessPolicy.canManage(listing, profile))
    }

    @Test
    fun `użytkownik nie może zarządzać cudzym ogłoszeniem`() {
        val profile = authenticatedProfile()
        val listing = listing(ownerId = "other-owner")

        assertFalse(ListingAccessPolicy.canManage(listing, profile))
    }

    @Test
    fun `lokalne starsze ogłoszenie bez właściciela pozostaje edytowalne`() {
        val listing = listing(id = "listing-legacy", ownerId = "")

        assertTrue(ListingAccessPolicy.canManage(listing, UserProfile()))
    }

    private fun authenticatedProfile() = UserProfile(
        id = "owner-1",
        name = "Jan",
        email = "jan@example.pl",
        loggedIn = true
    )

    private fun listing(
        id: String = "listing-1",
        ownerId: String
    ) = Listing(
        id = id,
        title = "Rower miejski",
        price = 750,
        location = "Legnica",
        categoryId = "sport",
        description = "Sprawny rower miejski w bardzo dobrym stanie.",
        ownerId = ownerId
    )
}
