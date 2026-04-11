package ru.housekpr.gate.services

import android.content.Context
import android.util.Log
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

class StorageError(
    val operation: String,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class SecurePrefsStorage(
    context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SecureStorage {
    private val appContext = context.applicationContext

    private val prefs by lazy {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                "gate_secure_storage",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to initialize secure preferences", error)
            throw StorageError(
                operation = "init",
                message = "Не удалось инициализировать защищённое хранилище.",
                cause = error
            )
        }
    }

    override fun loadSession(): UserSession? {
        return try {
            prefs.getString(KEY_SESSION, null)?.let { json.decodeFromString<UserSession>(it) }
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to load session", error)
            throw StorageError(
                operation = "loadSession",
                message = "Не удалось прочитать сохранённую сессию.",
                cause = error
            )
        }
    }

    override fun saveSession(session: UserSession) {
        try {
            prefs.edit().putString(KEY_SESSION, json.encodeToString(session)).apply()
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to save session", error)
            throw StorageError(
                operation = "saveSession",
                message = "Не удалось сохранить сессию на устройстве.",
                cause = error
            )
        }
    }

    override fun clearSession() {
        try {
            prefs.edit().remove(KEY_SESSION).apply()
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to clear session", error)
        }
    }

    override fun saveCredentials(credentials: Credentials) {
        try {
            prefs.edit().putString(KEY_CREDENTIALS, json.encodeToString(credentials)).apply()
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to save credentials", error)
            throw StorageError(
                operation = "saveCredentials",
                message = "Не удалось сохранить данные для биометрического входа.",
                cause = error
            )
        }
    }

    override fun loadCredentials(): Credentials? {
        return try {
            prefs.getString(KEY_CREDENTIALS, null)?.let { json.decodeFromString<Credentials>(it) }
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to load credentials", error)
            throw StorageError(
                operation = "loadCredentials",
                message = "Не удалось получить сохранённые данные для входа.",
                cause = error
            )
        }
    }

    override fun hasCredentials(): Boolean {
        return try {
            prefs.contains(KEY_CREDENTIALS)
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to check credentials availability", error)
            false
        }
    }

    private companion object {
        const val TAG = "SecureStorage"
        const val KEY_SESSION = "gate.user.session"
        const val KEY_CREDENTIALS = "gate.user.credentials"
    }
}
