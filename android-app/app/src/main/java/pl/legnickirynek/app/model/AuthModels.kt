package pl.legnickirynek.app.model

data class AuthSession(
    val accessToken: String,
    val refreshToken: String = "",
    val expiresAtEpochMillis: Long = 0L
) {
    val isValid: Boolean
        get() = accessToken.isNotBlank() &&
            (expiresAtEpochMillis <= 0L || expiresAtEpochMillis > System.currentTimeMillis())
}

data class AuthResult(
    val profile: UserProfile,
    val session: AuthSession
)

enum class AuthMode {
    LOGIN,
    REGISTER,
    LOCAL
}
