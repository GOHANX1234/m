package com.mna.streaming.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException

// â”€â”€ Local data models â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * A single locally-persisted watch history entry.
 * Written when the user taps Play; upserted on re-watch.
 */
data class LocalWatchEntry(
    val movieId: String,
    val title: String,
    val posterUrl: String,
    val targetType: String = "Movie",       // "Movie" or "Episode"
    /** For Episode entries: the parent series ID used to open the anime detail screen. Null for movies. */
    val seriesId: String? = null,
    /** Specific content type: "movie", "anime", or "series". */
    val contentType: String? = null,
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
    /** Content type from the server: "movie" or "anime". Null for entries created before this field was added. */
    val targetType: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

// â”€â”€ DataStore instance â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private val Context.profileDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "ma_profile")

// â”€â”€ Store â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    /**
     * [dataStore]'s data flow with corruption/IO failures downgraded to an
     * empty preferences set instead of throwing. Without this, a single
     * corrupted read (e.g. an interrupted write from a low-battery kill)
     * would throw out of the collecting coroutine â€” on an uncaught
     * viewModelScope launch that reads as "history/watchlist silently never
     * loads" rather than a visible crash.
     */
    private val safeData = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    companion object {
        private val KEY_WATCH_HISTORY   = stringPreferencesKey("watch_history")
        private val KEY_WATCHLIST       = stringPreferencesKey("watchlist")
        private val KEY_SEARCH_HISTORY  = stringPreferencesKey("search_history")
        private const val MAX_HISTORY        = 50   // keep newest 50 watch-history entries

        // Not private: SearchViewModel mirrors this cap when optimistically
        // updating its in-memory state, so the UI never drifts from what
        // actually gets persisted.
        const val MAX_SEARCH_HISTORY = 12   // keep newest 12 search terms
    }

    // â”€â”€ Watch History â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun getWatchHistory(): List<LocalWatchEntry> {
        val json = safeData.map { it[KEY_WATCH_HISTORY] }.firstOrNull()
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

    // â”€â”€ Watchlist â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun getWatchlist(): List<LocalWatchlistItem> {
        val json = safeData.map { it[KEY_WATCHLIST] }.firstOrNull()
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

    // â”€â”€ Search history â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //
    // Device-local list of past search terms shown on the Search screen when
    // the query field is empty. Not account data (kept across sign-out) â€”
    // purely a typing-shortcut convenience, like a browser's address bar.

    suspend fun getSearchHistory(): List<String> {
        val json = safeData.map { it[KEY_SEARCH_HISTORY] }.firstOrNull()
            ?: return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    /**
     * Prepend [query] to the search history. De-duplicates case-insensitively
     * (re-searching a term bumps it back to the top rather than creating a
     * second entry) and caps the list at [MAX_SEARCH_HISTORY].
     */
    suspend fun addSearchHistoryItem(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val current = getSearchHistory().toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val saved = current.take(MAX_SEARCH_HISTORY)
        dataStore.edit { it[KEY_SEARCH_HISTORY] = gson.toJson(saved) }
    }

    /** Remove a single term from the search history (per-item "x" in the UI). */
    suspend fun removeSearchHistoryItem(query: String) {
        val updated = getSearchHistory().filterNot { it.equals(query, ignoreCase = true) }
        dataStore.edit { it[KEY_SEARCH_HISTORY] = gson.toJson(updated) }
    }

    /** Wipe the entire search history ("Clear All" in the UI). */
    suspend fun clearSearchHistory() {
        dataStore.edit { it.remove(KEY_SEARCH_HISTORY) }
    }

    // â”€â”€ Session cleanup â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Called on sign-out to wipe locally cached profile data. */
    suspend fun clearAll() {
        dataStore.edit {
            it.remove(KEY_WATCH_HISTORY)
            it.remove(KEY_WATCHLIST)
        }
    }
}
