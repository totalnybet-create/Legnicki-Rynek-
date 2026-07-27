package pl.legnickirynek.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import pl.legnickirynek.app.model.AuthSession

private val Context.authSessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "secure_auth_session"
)

interface AuthSessionStore {
    suspend fun load(): AuthSession?
    suspend fun save(session: AuthSession)
    suspend fun clear()
    suspend fun accessToken(): String = load()?.takeIf(AuthSession::isValid)?.accessToken.orEmpty()
}

class SecureAuthSessionStore(context: Context) : AuthSessionStore {
    private val dataStore = context.applicationContext.authSessionDataStore
    private val cipher = AndroidKeystoreCipher()

    override suspend fun load(): AuthSession? {
        val preferences = dataStore.data.first()
        val encryptedAccessToken = preferences[ACCESS_TOKEN].orEmpty()
        if (encryptedAccessToken.isBlank()) return null

        return try {
            AuthSession(
                accessToken = cipher.decrypt(encryptedAccessToken),
                refreshToken = preferences[REFRESH_TOKEN]
                    ?.takeIf(String::isNotBlank)
                    ?.let(cipher::decrypt)
                    .orEmpty(),
                expiresAtEpochMillis = preferences[EXPIRES_AT] ?: 0L
            ).takeIf(AuthSession::isValid)
        } catch (_: Exception) {
            clear()
            null
        }
    }

    override suspend fun save(session: AuthSession) {
        require(session.accessToken.isNotBlank()) { "Token sesji nie może być pusty." }
        val encryptedAccessToken = cipher.encrypt(session.accessToken)
        val encryptedRefreshToken = session.refreshToken
            .takeIf(String::isNotBlank)
            ?.let(cipher::encrypt)
            .orEmpty()

        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = encryptedAccessToken
            if (encryptedRefreshToken.isBlank()) {
                preferences.remove(REFRESH_TOKEN)
            } else {
                preferences[REFRESH_TOKEN] = encryptedRefreshToken
            }
            preferences[EXPIRES_AT] = session.expiresAtEpochMillis.coerceAtLeast(0L)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(REFRESH_TOKEN)
            preferences.remove(EXPIRES_AT)
        }
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("encrypted_access_token")
        val REFRESH_TOKEN = stringPreferencesKey("encrypted_refresh_token")
        val EXPIRES_AT = longPreferencesKey("expires_at_epoch_millis")
    }
}

internal class AndroidKeystoreCipher {
    suspend fun encrypt(plainText: String): String = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        listOf(
            FORMAT_VERSION,
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        ).joinToString(":")
    }

    suspend fun decrypt(encoded: String): String = withContext(Dispatchers.IO) {
        val parts = encoded.split(':', limit = 3)
        require(parts.size == 3 && parts[0] == FORMAT_VERSION) {
            "Nieprawidłowy format zaszyfrowanej sesji."
        }
        val iv = Base64.decode(parts[1], Base64.NO_WRAP)
        val encrypted = Base64.decode(parts[2], Base64.NO_WRAP)
        require(iv.size == GCM_IV_BYTES && encrypted.isNotEmpty()) {
            "Nieprawidłowe dane zaszyfrowanej sesji."
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        ).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
        }.generateKey()
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "legnicki_rynek_auth_session_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val FORMAT_VERSION = "v1"
        const val GCM_TAG_BITS = 128
        const val GCM_IV_BYTES = 12
    }
}
