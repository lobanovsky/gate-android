package ru.housekpr.gate.services

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.housekpr.gate.models.Credentials
import ru.housekpr.gate.models.UserSession

interface SecureStorage {
    fun loadSession(): UserSession?
    fun saveSession(session: UserSession)
    fun clearSession()
    fun saveCredentials(credentials: Credentials)
    fun loadCredentials(): Credentials?
    fun hasCredentials(): Boolean
}

class SecurePrefsStorage(
    context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SecureStorage {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "gate_secure_storage",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun loadSession(): UserSession? {
        return prefs.getString(KEY_SESSION, null)?.let { json.decodeFromString<UserSession>(it) }
    }

    override fun saveSession(session: UserSession) {
        prefs.edit().putString(KEY_SESSION, json.encodeToString(session)).apply()
    }

    override fun clearSession() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    override fun saveCredentials(credentials: Credentials) {
        prefs.edit().putString(KEY_CREDENTIALS, json.encodeToString(credentials)).apply()
    }

    override fun loadCredentials(): Credentials? {
        return prefs.getString(KEY_CREDENTIALS, null)?.let { json.decodeFromString<Credentials>(it) }
    }

    override fun hasCredentials(): Boolean = prefs.contains(KEY_CREDENTIALS)

    private companion object {
        const val KEY_SESSION = "gate.user.session"
        const val KEY_CREDENTIALS = "gate.user.credentials"
    }
}
