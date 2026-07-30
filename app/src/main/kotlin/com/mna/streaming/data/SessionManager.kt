package com.mna.streaming.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mna.streaming.network.models.SessionUser
import com.google.gson.Gson
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ma_session")

/**
 * Persists auth session data across app restarts using DataStore.
 *
 * Stores:
 *  - session_token        : raw NextAuth JWT cookie value
 *  - session_user         : JSON-serialized SessionUser for instant UI restoration
 *  - native_access_token  : Bearer token from /api/auth/mobile/login (used for
 *                           push-notification device-token registration/removal)
 *  - device_id            : stable UUID generated once per installation, sent with
 *                           device-token registrations so the server can de-duplicate
 *                           tokens from the same physical device across re-logins
 */
class SessionManager(context: Context) {

    private val dataStore = context.dataStore
    private val gson = Gson()

    companion object {
        private val KEY_SESSION_TOKEN   = stringPreferencesKey("session_token")
        private val KEY_SESSION_USER    = stringPreferencesKey("session_user")
        private val KEY_NATIVE_TOKEN    = stringPreferencesKey("native_access_token")
        private val KEY_DEVICE_ID       = stringPreferencesKey("device_id")
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    suspend fun saveSessionToken(token: String) {
        dataStore.edit { it[KEY_SESSION_TOKEN] = token }
    }

    suspend fun saveUser(user: SessionUser) {
        dataStore.edit { it[KEY_SESSION_USER] = gson.toJson(user) }
    }

    suspend fun saveNativeToken(token: String) {
        dataStore.edit { it[KEY_NATIVE_TOKEN] = token }
    }

    suspend fun clearSession() {
        dataStore.edit {
            it.remove(KEY_SESSION_TOKEN)
            it.remove(KEY_SESSION_USER)
            it.remove(KEY_NATIVE_TOKEN)
            // device_id is intentionally kept — it identifies the physical device
            // across re-logins and must survive a logout/login cycle.
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    suspend fun getSessionToken(): String? =
        dataStore.data.map { it[KEY_SESSION_TOKEN] }.firstOrNull()

    suspend fun getSavedUser(): SessionUser? {
        val json = dataStore.data.map { it[KEY_SESSION_USER] }.firstOrNull()
            ?: return null
        return try {
            gson.fromJson(json, SessionUser::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getNativeToken(): String? =
        dataStore.data.map { it[KEY_NATIVE_TOKEN] }.firstOrNull()

    /**
     * Returns the stored device ID, generating and persisting a new random UUID
     * on the very first call (i.e. fresh install).
     */
    suspend fun getOrCreateDeviceId(): String {
        val existing = dataStore.data.map { it[KEY_DEVICE_ID] }.firstOrNull()
        if (!existing.isNullOrBlank()) return existing
        val newId = UUID.randomUUID().toString()
        dataStore.edit { it[KEY_DEVICE_ID] = newId }
        return newId
    }
}
