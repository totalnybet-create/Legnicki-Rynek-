package pl.legnickirynek.app.feature.listing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.legnickirynek.app.model.Listing

class ListingValidationAndQueryTest {

    private val validator = ListingValidator()
    private val galleryManager = ListingGalleryManager()
    private val queryEngine = ListingQueryEngine()

    @Test
    fun validDraftIsNormalizedAndAccepted() {
        val result = validator.validate(
            ListingDraft(
                title = "  Rower   trekkingowy ",
                price = 1250,
                location = " Legnica ",
                categoryId = "motoryzacja",
                description = "  Rower jest sprawny i gotowy do jazdy.  "
            )
        )

        assertTrue(result.isValid)
        assertEquals("Rower trekkingowy", result.normalizedDraft?.title)
        assertEquals("Legnica", result.normalizedDraft?.location)
    }

    @Test
    fun invalidDraftReportsAllRelevantFields() {
        val result = validator.validate(
            ListingDraft(
                title = "x",
                price = -1,
                location = "",
                categoryId = "!",
                description = "krótki"
            )
        )

        assertFalse(result.isValid)
        assertEquals(
            setOf(
                ListingField.TITLE,
                ListingField.PRICE,
                ListingField.LOCATION,
                ListingField.CATEGORY,
                ListingField.DESCRIPTION
            ),
            result.issues.map { it.field }.toSet()
        )
    }

    @Test
    fun gallerySupportsAddMoveCoverAndRemove() {
        val first = galleryManager.addPhoto(ListingGallery(), "content://images/1", "image/jpeg")
        assertTrue(first is GalleryMutationResult.Success)
        val firstGallery = (first as GalleryMutationResult.Success).gallery
        val second = galleryManager.addPhoto(firstGallery, "content://images/2", "image/png")
        val secondGallery = (second as GalleryMutationResult.Success).gallery

        assertEquals(2, secondGallery.photos.size)
        assertTrue(secondGallery.photos.first().isCover)

        val moved = galleryManager.movePhoto(secondGallery, 1, 0) as GalleryMutationResult.Success
        assertEquals("content://images/2", moved.gallery.photos.first().uri)

        val cover = galleryManager.setCover(moved.gallery, moved.gallery.photos.last().id)
            as GalleryMutationResult.Success
        assertTrue(cover.gallery.photos.last().isCover)
        assertEquals(1, cover.gallery.photos.count { it.isCover })

        val removed = galleryManager.removePhoto(cover.gallery, cover.gallery.photos.last().id)
            as GalleryMutationResult.Success
        assertEquals(1, removed.gallery.photos.size)
        assertTrue(removed.gallery.photos.first().isCover)
    }

    @Test
    fun galleryRejectsDuplicatesAndNinthPhoto() {
        var gallery = ListingGallery()
        repeat(ListingValidator.MAX_PHOTOS) { index ->
            gallery = (galleryManager.addPhoto(
                gallery,
                "content://images/$index",
                "image/jpeg"
            ) as GalleryMutationResult.Success).gallery
        }

        assertTrue(
            galleryManager.addPhoto(gallery, "content://images/0", "image/jpeg")
                is GalleryMutationResult.Failure
        )
        assertTrue(
            galleryManager.addPhoto(gallery, "content://images/9", "image/jpeg")
                is GalleryMutationResult.Failure
        )
    }

    @Test
    fun searchIgnoresPolishDiacriticsAndUsesAllTokens() {
        val listings = sampleListings()
        val result = queryEngine.apply(
            listings,
            ListingFilter(query = "lodz rower")
        )

        assertEquals(listOf("rower"), result.map { it.id })
    }

    @Test
    fun filtersCategoryPriceLocationAndFavoritesTogether() {
        val result = queryEngine.apply(
            sampleListings(),
            ListingFilter(
                categoryIds = setOf("dom"),
                minimumPrice = 500,
                maximumPrice = 1500,
                locationQuery = "Legnica",
                favoritesOnly = true,
                sort = ListingSort.PRICE_DESCENDING
            )
        )

        assertEquals(listOf("sofa"), result.map { it.id })
    }

    @Test
    fun reversedPriceBoundsAreHandledSafely() {
        val result = queryEngine.apply(
            sampleListings(),
            ListingFilter(minimumPrice = 1500, maximumPrice = 500)
        )

        assertEquals(setOf("rower", "sofa"), result.map { it.id }.toSet())
    }

    private fun sampleListings(): List<Listing> = listOf(
        Listing(
            id = "rower",
            title = "Rower miejski Łódź",
            price = 1200,
            location = "Legnica",
            categoryId = "sport",
            description = "Sprawny rower do codziennej jazdy.",
            isFavorite = false
        ),
        Listing(
            id = "sofa",
            title = "Sofa rozkładana",
            price = 990,
            location = "Legnica, Tarninów",
            categoryId = "dom",
            description = "Sofa z pojemnikiem na pościel.",
            isFavorite = true
        ),
        Listing(
            id = "telefon",
            title = "Telefon 128 GB",
            price = 1690,
            location = "Chojnów",
            categoryId = "elektronika",
            description = "Telefon w bardzo dobrym stanie.",
            isFavorite = true
        )
    )
}
