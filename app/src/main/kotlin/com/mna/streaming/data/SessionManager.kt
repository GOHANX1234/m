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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ma_session")

/**
 * Persists auth session data across app restarts using DataStore.
 *
 * Stores:
 *  - session_token  : raw NextAuth JWT cookie value
 *  - session_user   : JSON-serialized SessionUser for instant UI restoration
 */
class SessionManager(context: Context) {

    private val dataStore = context.dataStore
    private val gson = Gson()

    companion object {
        private val KEY_SESSION_TOKEN = stringPreferencesKey("session_token")
        private val KEY_SESSION_USER  = stringPreferencesKey("session_user")
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    suspend fun saveSessionToken(token: String) {
        dataStore.edit { it[KEY_SESSION_TOKEN] = token }
    }

    suspend fun saveUser(user: SessionUser) {
        dataStore.edit { it[KEY_SESSION_USER] = gson.toJson(user) }
    }

    suspend fun clearSession() {
        dataStore.edit {
            it.remove(KEY_SESSION_TOKEN)
            it.remove(KEY_SESSION_USER)
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
}
