package pl.legnickirynek.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.legnickirynek.app.model.ListingStatus

class ListingConvertersTest {
    private val converters = ListingConverters()

    @Test
    fun `galeria zachowuje kolejność podczas konwersji`() {
        val imageUris = listOf(
            "content://gallery/first",
            "content://gallery/second"
        )

        val restored = converters.jsonToImageUris(
            converters.imageUrisToJson(imageUris)
        )

        assertEquals(imageUris, restored)
    }

    @Test
    fun `uszkodzony JSON galerii zwraca pustą listę`() {
        assertEquals(emptyList<String>(), converters.jsonToImageUris("not-json"))
    }

    @Test
    fun `status zachowuje wartość podczas konwersji`() {
        ListingStatus.entries.forEach { status ->
            assertEquals(
                status,
                converters.stringToStatus(converters.statusToString(status))
            )
        }
    }

    @Test
    fun `nieznany status wraca do aktywnego`() {
        assertEquals(
            ListingStatus.ACTIVE,
            converters.stringToStatus("UNKNOWN")
        )
    }
}
