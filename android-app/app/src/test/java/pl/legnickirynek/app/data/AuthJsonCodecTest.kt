package pl.legnickirynek.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthJsonCodecTest {
    @Test
    fun `kodek odczytuje sesję i profil z zagnieżdżonej odpowiedzi`() {
        val json = """
            {
              "user": {
                "id": "user-123",
                "name": "Jan Kowalski",
                "email": "JAN@example.pl"
              },
              "session": {
                "accessToken": "access-token-value",
                "refreshToken": "refresh-token-value",
                "expiresAt": 1893456000000
              }
            }
        """.trimIndent()

        val result = AuthJsonCodec.decodeAuthResult(json)

        assertEquals("user-123", result.profile.id)
        assertEquals("Jan Kowalski", result.profile.name)
        assertEquals("jan@example.pl", result.profile.email)
        assertTrue(result.profile.loggedIn)
        assertEquals("access-token-value", result.session.accessToken)
        assertEquals("refresh-token-value", result.session.refreshToken)
        assertEquals(1893456000000, result.session.expiresAtEpochMillis)
    }

    @Test
    fun `kodek obsługuje płaską odpowiedź i wartości awaryjne`() {
        val result = AuthJsonCodec.decodeAuthResult(
            json = "{\"token\":\"abcdefgh\",\"expiresIn\":3600}",
            fallbackName = "Anna",
            fallbackEmail = "anna@example.pl"
        )

        assertEquals("Anna", result.profile.name)
        assertEquals("anna@example.pl", result.profile.email)
        assertEquals("abcdefgh", result.session.accessToken)
        assertTrue(result.session.expiresAtEpochMillis > System.currentTimeMillis())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `kodek odrzuca odpowiedź bez tokenu`() {
        AuthJsonCodec.decodeAuthResult(
            json = "{\"user\":{\"email\":\"jan@example.pl\"}}"
        )
    }
}
