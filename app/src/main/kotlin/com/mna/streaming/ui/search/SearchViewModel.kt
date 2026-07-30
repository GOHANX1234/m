package com.mna.streaming.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
import com.mna.streaming.data.LocalProfileStore
import com.mna.streaming.data.repository.AnimeRepository
import com.mna.streaming.data.repository.MovieRepository
import com.mna.streaming.data.model.Movie
import com.mna.streaming.network.models.ApiAnime
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Unified search result — either a movie or an anime/web-series title. */
sealed class SearchResult {
    abstract val id: String
    abstract val title: String
    abstract val posterUrl: String
    abstract val year: Int?

    data class MovieItem(val movie: Movie) : SearchResult() {
        override val id        = movie.id
        override val title     = movie.title
        override val posterUrl = movie.posterUrl
        override val year      = movie.year
    }

    data class AnimeItem(val anime: ApiAnime) : SearchResult() {
        override val id        = anime.id
        override val title     = anime.title
        override val posterUrl = anime.posterUrl ?: ""
        override val year      = anime.releaseYear
    }
}

/**
 * "Recently added" rails shown on the idle (empty query) state — the top 10
 * newest movies, anime, and web series, so the screen is never blank.
 */
data class DiscoverContent(
    val movies: List<Movie> = emptyList(),
    val anime: List<ApiAnime> = emptyList(),
    val webSeries: List<ApiAnime> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val isEmpty: Boolean get() = movies.isEmpty() && anime.isEmpty() && webSeries.isEmpty()
}

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val hasSearched: Boolean = false,   // true after first search attempt
    val error: String? = null,
    val history: List<String> = emptyList(),
    val discover: DiscoverContent = DiscoverContent()
)

class SearchViewModel(
    private val movieRepository: MovieRepository,
    private val animeRepository: AnimeRepository,
    private val localProfileStore: LocalProfileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadHistory()
        // Show cached "recently added" content instantly if we have a fresh-enough
        // copy — the Search screen's ViewModel is recreated on every visit (it's
        // scoped to the nav back-stack entry), so without this every re-open would
        // re-run 3 network calls before anything appears, which reads as "laggy"
        // on a slow connection.
        val cached = DiscoverCache.get()
        if (cached != null) {
            _uiState.update { it.copy(discover = cached) }
        } else {
            loadDiscoverContent()
        }
    }

    // ── Discover — recently added content shown before the user types ────────

    fun loadDiscoverContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(discover = it.discover.copy(isLoading = true, error = null)) }
            try {
                val moviesDeferred = async { movieRepository.getLatest(10) }
                val animeDeferred  = async { animeRepository.getAnime(sort = "latest", limit = 10).series }
                val seriesDeferred = async { animeRepository.getWebSeries(sort = "latest", limit = 10).series }

                val movies = moviesDeferred.await()
                val anime  = animeDeferred.await()
                val series = seriesDeferred.await()

                val content = DiscoverContent(
                    movies    = movies,
                    anime     = anime,
                    webSeries = series,
                    isLoading = false
                )
                DiscoverCache.set(content)
                _uiState.update { it.copy(discover = content) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(discover = it.discover.copy(isLoading = false, error = e.message ?: "Failed to load"))
                }
            }
        }
    }

    /**
     * Process-lifetime cache for the "recently added" rails, keyed by nothing
     * (single global entry) since the content is the same for every user.
     * Lives in a companion object rather than a repository so it survives the
     * Search screen's ViewModel being recreated on every visit, without
     * introducing a broader caching layer other screens don't have yet.
     */
    private object DiscoverCache {
        private const val TTL_MS = 5 * 60 * 1000L
        private var content: DiscoverContent? = null
        private var fetchedAtMs: Long = 0L

        fun get(): DiscoverContent? {
            val snapshot = content ?: return null
            if (System.currentTimeMillis() - fetchedAtMs > TTL_MS) return null
            return snapshot
        }

        fun set(newContent: DiscoverContent) {
            content = newContent
            fetchedAtMs = System.currentTimeMillis()
        }
    }

    // ── Search history ────────────────────────────────────────────────────────

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(history = localProfileStore.getSearchHistory()) }
        }
    }

    /**
     * Persist the current query as a history entry. Called on explicit intent
     * signals only (keyboard "search" submission, tapping a result) rather
     * than on every debounced keystroke, so history reads as a list of real
     * searches instead of partial words typed along the way.
     */
    fun commitCurrentQueryToHistory() = commitToHistory(_uiState.value.query)

    private fun commitToHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        viewModelScope.launch {
            localProfileStore.addSearchHistoryItem(trimmed)
            _uiState.update { it.copy(history = localProfileStore.getSearchHistory()) }
        }
    }

    /** User tapped a past search term — re-run it and bump it back to the top. */
    fun searchFromHistory(query: String) {
        onQueryChanged(query)
        commitToHistory(query)
    }

    fun removeHistoryItem(query: String) {
        viewModelScope.launch {
            localProfileStore.removeSearchHistoryItem(query)
            _uiState.update { it.copy(history = localProfileStore.getSearchHistory()) }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            localProfileStore.clearSearchHistory()
            _uiState.update { it.copy(history = emptyList()) }
        }
    }

    // ── Live search ───────────────────────────────────────────────────────────

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query, error = null) }

        searchJob?.cancel()

        if (query.length < 2) {
            _uiState.update { it.copy(isSearching = false, results = emptyList(), hasSearched = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)   // debounce — wait 300 ms after typing stops
            _uiState.update { it.copy(isSearching = true) }
            try {
                // Run both searches in parallel
                val moviesDeferred = async { movieRepository.search(query) }
                val animeDeferred  = async { animeRepository.search(query) }

                val movies = moviesDeferred.await().map { SearchResult.MovieItem(it) }
                val anime  = animeDeferred.await().map  { SearchResult.AnimeItem(it) }

                // Merge: interleave so neither type is buried at the bottom
                val combined = buildList {
                    val m = movies.iterator()
                    val a = anime.iterator()
                    while (m.hasNext() || a.hasNext()) {
                        if (m.hasNext()) add(m.next())
                        if (a.hasNext()) add(a.next())
                    }
                }

                _uiState.update {
                    it.copy(
                        isSearching = false,
                        results     = combined,
                        hasSearched = true
                    )
                }

                // Save automatically once a search actually settles on results —
                // most people glance at the results grid without ever tapping the
                // keyboard's search action or a result, so relying only on those
                // explicit signals left history permanently empty in practice.
                if (combined.isNotEmpty()) {
                    commitToHistory(query)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        hasSearched = true,
                        error       = e.message ?: "Search failed"
                    )
                }
            }
        }
    }

    fun clearQuery() {
        searchJob?.cancel()
        // Reset only the search-related fields — keep history/discover intact
        // so clearing the field doesn't force a reload flicker.
        _uiState.update {
            it.copy(
                query       = "",
                isSearching = false,
                results     = emptyList(),
                hasSearched = false,
                error       = null
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SearchViewModel(
                    movieRepository   = MAApplication.movieRepository,
                    animeRepository   = MAApplication.animeRepository,
                    localProfileStore = MAApplication.localProfileStore
                )
            }
        }
    }
}
