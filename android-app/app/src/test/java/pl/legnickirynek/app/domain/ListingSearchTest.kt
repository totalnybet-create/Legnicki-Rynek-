package pl.legnickirynek.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.ListingStatus

class ListingSearchTest {
    @Test
    fun `wyszukiwanie ignoruje wielkość liter i polskie znaki`() {
        val listings = listOf(
            listing(id = "1", title = "Lodówka turystyczna"),
            listing(id = "2", title = "Rower miejski")
        )

        val result = ListingSearch.apply(
            listings,
            ListingSearchCriteria(query = "LODOWKA")
        )

        assertEquals(listOf("1"), result.map { it.id })
    }

    @Test
    fun `filtry kategorii ceny i lokalizacji działają łącznie`() {
        val listings = listOf(
            listing(
                id = "1",
                price = 1500,
                categoryId = "sport",
                location = "Legnica, Tarninów"
            ),
            listing(
                id = "2",
                price = 800,
                categoryId = "sport",
                location = "Lubin"
            ),
            listing(
                id = "3",
                price = 1600,
                categoryId = "dom",
                location = "Legnica"
            )
        )

        val result = ListingSearch.apply(
            listings,
            ListingSearchCriteria(
                categoryId = "sport",
                minimumPrice = 1000,
                maximumPrice = 2000,
                location = "legnica"
            )
        )

        assertEquals(listOf("1"), result.map { it.id })
    }

    @Test
    fun `sprzedane i wygasłe oferty są domyślnie ukryte`() {
        val listings = listOf(
            listing(id = "1", status = ListingStatus.ACTIVE),
            listing(id = "2", status = ListingStatus.RESERVED),
            listing(id = "3", status = ListingStatus.SOLD),
            listing(id = "4", status = ListingStatus.EXPIRED)
        )

        val visible = ListingSearch.apply(listings, ListingSearchCriteria())
        val all = ListingSearch.apply(
            listings,
            ListingSearchCriteria(includeUnavailable = true)
        )

        assertEquals(setOf("1", "2"), visible.map { it.id }.toSet())
        assertEquals(setOf("1", "2", "3", "4"), all.map { it.id }.toSet())
    }

    @Test
    fun `sortowanie po cenie działa rosnąco i malejąco`() {
        val listings = listOf(
            listing(id = "1", price = 500),
            listing(id = "2", price = 100),
            listing(id = "3", price = 900)
        )

        val ascending = ListingSearch.apply(
            listings,
            ListingSearchCriteria(sort = ListingSort.PRICE_ASCENDING)
        )
        val descending = ListingSearch.apply(
            listings,
            ListingSearchCriteria(sort = ListingSort.PRICE_DESCENDING)
        )

        assertEquals(listOf("2", "1", "3"), ascending.map { it.id })
        assertEquals(listOf("3", "1", "2"), descending.map { it.id })
    }

    private fun listing(
        id: String,
        title: String = "Ogłoszenie",
        price: Int = 100,
        categoryId: String = "inne",
        location: String = "Legnica",
        status: ListingStatus = ListingStatus.ACTIVE
    ) = Listing(
        id = id,
        title = title,
        price = price,
        location = location,
        categoryId = categoryId,
        description = "Opis wystarczającej długości.",
        createdAt = id.filter(Char::isDigit).toLongOrNull() ?: 0L,
        updatedAt = id.filter(Char::isDigit).toLongOrNull() ?: 0L,
        status = status
    )
}
