package pl.legnickirynek.app.domain

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

object UserIdentity {
    private val emailPattern = Regex(
        pattern = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        option = RegexOption.IGNORE_CASE
    )

    fun fromEmail(email: String): String {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        if (!emailPattern.matches(normalizedEmail)) return ""

        return MessageDigest.getInstance("SHA-256")
            .digest(normalizedEmail.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
            .take(32)
    }
}
