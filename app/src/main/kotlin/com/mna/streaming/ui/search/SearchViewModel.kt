package com.mna.streaming.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
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

/** Unified search result — either a movie or an anime series. */
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

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val hasSearched: Boolean = false,   // true after first search attempt
    val error: String? = null
)

class SearchViewModel(
    private val movieRepository: MovieRepository,
    private val animeRepository: AnimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

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
        _uiState.update { SearchUiState() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SearchViewModel(
                    movieRepository = MAApplication.movieRepository,
                    animeRepository = MAApplication.animeRepository
                )
            }
        }
    }
}
