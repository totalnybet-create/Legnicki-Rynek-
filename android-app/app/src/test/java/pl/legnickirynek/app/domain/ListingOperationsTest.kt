package pl.legnickirynek.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus

class ListingOperationsTest {
    @Test
    fun `dodawanie umieszcza nowe ogłoszenie na początku i przypisuje sprzedawcę`() {
        val existing = listing(id = "old")
        val added = listing(id = "new", sellerName = "Użytkownik")

        val result = ListingOperations.add(
            listings = listOf(existing),
            listing = added,
            sellerName = "Jan"
        )

        assertEquals(listOf("new", "old"), result.map { it.id })
        assertEquals("Jan", result.first().sellerName)
    }

    @Test
    fun `edycja zachowuje datę utworzenia i stan ulubionych`() {
        val existing = listing(
            id = "1",
            title = "Stary tytuł",
            createdAt = 100L,
            updatedAt = 100L,
            isFavorite = true
        )
        val edited = existing.copy(
            title = "Nowy tytuł",
            createdAt = 999L,
            isFavorite = false
        )

        val result = ListingOperations.update(
            listings = listOf(existing),
            listing = edited,
            updatedAt = 200L
        ).single()

        assertEquals("Nowy tytuł", result.title)
        assertEquals(100L, result.createdAt)
        assertEquals(200L, result.updatedAt)
        assertTrue(result.isFavorite)
    }

    @Test
    fun `usuwanie usuwa tylko wskazane ogłoszenie`() {
        val result = ListingOperations.delete(
            listings = listOf(listing("1"), listing("2")),
            id = "1"
        )

        assertEquals(listOf("2"), result.map { it.id })
    }

    @Test
    fun `ulubione przełącza się w obie strony`() {
        val initial = listOf(listing(id = "1", isFavorite = false))
        val favorite = ListingOperations.toggleFavorite(initial, "1")
        val notFavorite = ListingOperations.toggleFavorite(favorite, "1")

        assertTrue(favorite.single().isFavorite)
        assertFalse(notFavorite.single().isFavorite)
    }

    @Test
    fun `zmiana statusu aktualizuje status i czas modyfikacji`() {
        val result = ListingOperations.updateStatus(
            listings = listOf(listing("1")),
            id = "1",
            status = ListingStatus.SOLD,
            updatedAt = 500L
        ).single()

        assertEquals(ListingStatus.SOLD, result.status)
        assertEquals(500L, result.updatedAt)
    }

    private fun listing(
        id: String,
        title: String = "Ogłoszenie",
        sellerName: String = "Użytkownik",
        createdAt: Long = 1L,
        updatedAt: Long = createdAt,
        isFavorite: Boolean = false
    ) = Listing(
        id = id,
        title = title,
        price = 100,
        location = "Legnica",
        categoryId = "inne",
        description = "Opis wystarczającej długości.",
        sellerName = sellerName,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isFavorite = isFavorite
    )
}
