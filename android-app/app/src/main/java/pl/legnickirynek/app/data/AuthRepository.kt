package pl.legnickirynek.app.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.Locale
import pl.legnickirynek.app.data.remote.JsonHttpClient
import pl.legnickirynek.app.data.remote.RestRemoteListingService
import pl.legnickirynek.app.domain.UserIdentity
import pl.legnickirynek.app.model.AuthResult
import pl.legnickirynek.app.model.AuthSession
import pl.legnickirynek.app.model.UserProfile

interface AuthRepository {
    val isConfigured: Boolean
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(name: String, email: String, password: String): AuthResult
    suspend fun logout()
    suspend fun currentAccessToken(): String
    suspend fun hasValidSession(): Boolean
}

class RestAuthRepository(
    baseUrl: String,
    private val httpClient: JsonHttpClient,
    private val sessionStore: AuthSessionStore,
    private val buildToken: String = "",
    private val gson: Gson = Gson()
) : AuthRepository {
    private val normalizedBaseUrl = baseUrl.trim().trimEnd('/')

    override val isConfigured: Boolean =
        RestRemoteListingService.isAllowedBaseUrl(normalizedBaseUrl)

    override suspend fun login(email: String, password: String): AuthResult {
        checkConfigured()
        val cleanEmail = normalizeEmail(email)
        validatePassword(password)
        val body = JsonObject().apply {
            addProperty("email", cleanEmail)
            addProperty("password", password)
        }
        return authenticate("/auth/login", body, fallbackEmail = cleanEmail)
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): AuthResult {
        checkConfigured()
        val cleanName = name.trim().take(80)
        require(cleanName.length >= 2) { "Imię musi mieć co najmniej 2 znaki." }
        val cleanEmail = normalizeEmail(email)
        validatePassword(password)
        val body = JsonObject().apply {
            addProperty("name", cleanName)
            addProperty("email", cleanEmail)
            addProperty("password", password)
        }
        return authenticate(
            path = "/auth/register",
            body = body,
            fallbackName = cleanName,
            fallbackEmail = cleanEmail
        )
    }

    override suspend fun logout() {
        val accessToken = sessionStore.accessToken()
        try {
            if (isConfigured && accessToken.isNotBlank()) {
                runCatching {
                    httpClient.request(
                        method = "POST",
                        url = "$normalizedBaseUrl/auth/logout",
                        body = "{}",
                        bearerToken = accessToken
                    )
                }
            }
        } finally {
            sessionStore.clear()
        }
    }

    override suspend fun currentAccessToken(): String =
        sessionStore.accessToken().ifBlank { buildToken }

    override suspend fun hasValidSession(): Boolean =
        sessionStore.load()?.isValid == true

    private suspend fun authenticate(
        path: String,
        body: JsonObject,
        fallbackName: String = "",
        fallbackEmail: String
    ): AuthResult {
        val response = httpClient.request(
            method = "POST",
            url = "$normalizedBaseUrl$path",
            body = gson.toJson(body)
        )
        val result = AuthJsonCodec.decodeAuthResult(
            json = response.body,
            fallbackName = fallbackName,
            fallbackEmail = fallbackEmail
        )
        sessionStore.save(result.session)
        return result
    }

    private fun checkConfigured() {
        check(isConfigured) { "Serwer kont użytkowników nie jest skonfigurowany." }
    }

    private fun normalizeEmail(email: String): String {
        val cleanEmail = email.trim().lowercase(Locale.ROOT).take(160)
        require(EMAIL_PATTERN.matches(cleanEmail)) { "Podaj prawidłowy adres e-mail." }
        return cleanEmail
    }

    private fun validatePassword(password: String) {
        require(password.length in 8..128) {
            "Hasło musi mieć od 8 do 128 znaków."
        }
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}

object AuthJsonCodec {
    fun decodeAuthResult(
        json: String,
        fallbackName: String = "",
        fallbackEmail: String = ""
    ): AuthResult {
        val root = JsonParser.parseString(json)
            .takeIf { it.isJsonObject }
            ?.asJsonObject
            ?: throw IllegalArgumentException("Odpowiedź logowania nie jest obiektem JSON.")
        val sessionObject = root.get("session")
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?: root
        val userObject = root.get("user")
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?: root
        val accessToken = sequenceOf("accessToken", "access_token", "token")
            .mapNotNull(sessionObject::stringOrNull)
            .firstOrNull()
            ?.trim()
            ?.take(8_192)
            .orEmpty()
        require(accessToken.isNotBlank()) {
            "Odpowiedź logowania nie zawiera tokenu dostępu."
        }
        val refreshToken = sequenceOf("refreshToken", "refresh_token")
            .mapNotNull(sessionObject::stringOrNull)
            .firstOrNull()
            ?.trim()
            ?.take(8_192)
            .orEmpty()
        val expiresAt = sessionObject.longOrNull("expiresAt")
            ?: sessionObject.longOrNull("expires_at")
            ?: sessionObject.longOrNull("expiresAtEpochMillis")
            ?: sessionObject.longOrNull("expiresIn")?.let { seconds ->
                System.currentTimeMillis() + seconds.coerceIn(0L, 31_536_000L) * 1_000L
            }
            ?: 0L
        val email = userObject.stringOrNull("email")
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.take(160)
            ?.ifBlank { null }
            ?: fallbackEmail.trim().lowercase(Locale.ROOT).take(160)
        require(email.isNotBlank()) {
            "Odpowiedź logowania nie zawiera adresu e-mail użytkownika."
        }
        val name = userObject.stringOrNull("name")
            ?.trim()
            ?.take(80)
            ?.ifBlank { null }
            ?: fallbackName.trim().take(80).ifBlank { email.substringBefore('@') }
        val id = userObject.stringOrNull("id")
            ?.trim()
            ?.take(160)
            ?.ifBlank { null }
            ?: UserIdentity.fromEmail(email)

        return AuthResult(
            profile = UserProfile(
                id = id,
                name = name,
                email = email,
                loggedIn = true,
                remoteSession = true
            ),
            session = AuthSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAtEpochMillis = expiresAt.coerceAtLeast(0L)
            )
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        get(key)
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
            ?.asString

    private fun JsonObject.longOrNull(key: String): Long? =
        runCatching { get(key)?.asLong }.getOrNull()
}
