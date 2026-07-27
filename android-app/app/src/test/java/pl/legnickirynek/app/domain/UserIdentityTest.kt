package pl.legnickirynek.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UserIdentityTest {
    @Test
    fun `email jest normalizowany przed utworzeniem identyfikatora`() {
        assertEquals(
            UserIdentity.fromEmail("jan@example.pl"),
            UserIdentity.fromEmail("  JAN@EXAMPLE.PL  ")
        )
    }

    @Test
    fun `różne adresy mają różne identyfikatory`() {
        assertNotEquals(
            UserIdentity.fromEmail("jan@example.pl"),
            UserIdentity.fromEmail("anna@example.pl")
        )
    }

    @Test
    fun `pusty email nie tworzy identyfikatora`() {
        assertEquals("", UserIdentity.fromEmail("   "))
    }
}
