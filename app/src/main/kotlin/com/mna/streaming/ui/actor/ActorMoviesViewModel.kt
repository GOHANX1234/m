package com.mna.streaming.ui.actor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
import com.mna.streaming.data.repository.AnimeRepository
import com.mna.streaming.data.repository.MovieRepository
import com.mna.streaming.ui.search.SearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActorMoviesUiState(
    val isLoading: Boolean          = true,
    /**
     * Movies AND anime/web-series titles that credit this actor, unified —
     * the "Top Actors" rail on Home pulls cast from both movies and web
     * series (see HomeViewModel), so an actor can appear here for either.
     */
    val items: List<SearchResult>   = emptyList(),
    val error: String?              = null,
    /** True when cast data wasn't available in list responses → nothing found. */
    val noCastData: Boolean         = false
)

class ActorMoviesViewModel(
    private val movieRepository: MovieRepository,
    private val animeRepository: AnimeRepository,
    val actorName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActorMoviesUiState())
    val uiState: StateFlow<ActorMoviesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, noCastData = false) }
            try {
                val moviesDeferred = async { movieRepository.getMoviesForActor(actorName) }
                val seriesDeferred = async { animeRepository.getSeriesForActor(actorName) }

                val movies = moviesDeferred.await().map { SearchResult.MovieItem(it) }
                val series = seriesDeferred.await().map { SearchResult.AnimeItem(it) }
                val items  = movies + series

                _uiState.update {
                    it.copy(
                        isLoading  = false,
                        items      = items,
                        noCastData = items.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error     = e.message ?: "Failed to load titles for $actorName"
                    )
                }
            }
        }
    }

    companion object {
        fun factory(actorName: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ActorMoviesViewModel(
                    movieRepository = MAApplication.movieRepository,
                    animeRepository = MAApplication.animeRepository,
                    actorName       = actorName
                )
            }
        }
    }
}
