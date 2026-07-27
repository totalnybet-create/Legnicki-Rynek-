package pl.legnickirynek.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FavoriteEntityTest {
    @Test
    fun `ten sam listing może należeć do ulubionych różnych kont`() {
        val firstAccount = FavoriteEntity(
            accountId = "account-a",
            listingId = "listing-1",
            createdAt = 100L
        )
        val secondAccount = FavoriteEntity(
            accountId = "account-b",
            listingId = "listing-1",
            createdAt = 100L
        )

        assertNotEquals(firstAccount, secondAccount)
        assertEquals("listing-1", firstAccount.listingId)
        assertEquals("listing-1", secondAccount.listingId)
    }

    @Test
    fun `konto i listing tworzą jednoznaczną relację ulubionych`() {
        val favorite = FavoriteEntity(
            accountId = "account-a",
            listingId = "listing-7",
            createdAt = 250L
        )

        assertEquals("account-a", favorite.accountId)
        assertEquals("listing-7", favorite.listingId)
        assertEquals(250L, favorite.createdAt)
    }
}
