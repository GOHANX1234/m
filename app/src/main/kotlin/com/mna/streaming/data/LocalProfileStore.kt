package com.mna.streaming.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

// ── Local data models ─────────────────────────────────────────────────────────

/**
 * A single locally-persisted watch history entry.
 * Written when the user taps Play; upserted on re-watch.
 */
data class LocalWatchEntry(
    val movieId: String,
    val title: String,
    val posterUrl: String,
    val targetType: String = "Movie",       // "Movie" or "Anime"
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * A single locally-persisted watchlist item.
 * Written when the user saves a title; removed on toggle-off.
 */
data class LocalWatchlistItem(
    val movieId: String,
    val title: String,
    val posterUrl: String,
    val releaseYear: Int,
    val rating: Double,
    val addedAt: Long = System.currentTimeMillis()
)

// ── DataStore instance ────────────────────────────────────────────────────────

private val Context.profileDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "ma_profile")

// ── Store ─────────────────────────────────────────────────────────────────────

/**
 * Persists the user's watch history and watchlist locally on-device.
 *
 * The server has no public GET endpoints for these lists (history and full
 * watchlist are server-side rendered on the web). This store is the single
 * source of truth for displaying them in the native profile screen.
 *
 * Both lists are stored as JSON arrays in a dedicated DataStore file ("ma_profile"),
 * separate from the session DataStore ("ma_session") used by [SessionManager].
 */
class LocalProfileStore(context: Context) {

    private val dataStore = context.profileDataStore
    private val gson      = Gson()

    companion object {
        private val KEY_WATCH_HISTORY = stringPreferencesKey("watch_history")
        private val KEY_WATCHLIST     = stringPreferencesKey("watchlist")
        private const val MAX_HISTORY = 50          // keep newest 50 entries
    }

    // ── Watch History ─────────────────────────────────────────────────────────

    suspend fun getWatchHistory(): List<LocalWatchEntry> {
        val json = dataStore.data.map { it[KEY_WATCH_HISTORY] }.firstOrNull()
            ?: return emptyList()
        return try {
            val type = object : TypeToken<List<LocalWatchEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    /**
     * Upsert: refreshes the [updatedAt] timestamp for an existing movie entry
     * or prepends a brand-new entry. The list is kept sorted by [updatedAt]
     * descending and capped at [MAX_HISTORY].
     */
    suspend fun upsertWatchEntry(entry: LocalWatchEntry) {
        val current = getWatchHistory().toMutableList()
        val idx = current.indexOfFirst { it.movieId == entry.movieId }
        if (idx >= 0) current[idx] = entry else current.add(0, entry)
        val saved = current.sortedByDescending { it.updatedAt }.take(MAX_HISTORY)
        dataStore.edit { it[KEY_WATCH_HISTORY] = gson.toJson(saved) }
    }

    // ── Watchlist ─────────────────────────────────────────────────────────────

    suspend fun getWatchlist(): List<LocalWatchlistItem> {
        val json = dataStore.data.map { it[KEY_WATCHLIST] }.firstOrNull()
            ?: return emptyList()
        return try {
            val type = object : TypeToken<List<LocalWatchlistItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    /** Prepend [item] to the local watchlist (no-op if already present). */
    suspend fun addWatchlistItem(item: LocalWatchlistItem) {
        val current = getWatchlist().toMutableList()
        if (current.none { it.movieId == item.movieId }) {
            current.add(0, item)
        }
        dataStore.edit { it[KEY_WATCHLIST] = gson.toJson(current) }
    }

    /** Remove the entry matching [movieId] from the local watchlist. */
    suspend fun removeWatchlistItem(movieId: String) {
        val updated = getWatchlist().filter { it.movieId != movieId }
        dataStore.edit { it[KEY_WATCHLIST] = gson.toJson(updated) }
    }

    // ── Session cleanup ───────────────────────────────────────────────────────

    /** Called on sign-out to wipe locally cached profile data. */
    suspend fun clearAll() {
        dataStore.edit {
            it.remove(KEY_WATCH_HISTORY)
            it.remove(KEY_WATCHLIST)
        }
    }
}
