package com.mna.streaming.ui.actor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
import com.mna.streaming.data.model.Movie
import com.mna.streaming.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActorMoviesUiState(
    val isLoading: Boolean    = true,
    val movies: List<Movie>   = emptyList(),
    val error: String?        = null,
    /** True when cast data wasn't available in list responses → no movies found. */
    val noCastData: Boolean   = false
)

class ActorMoviesViewModel(
    private val movieRepository: MovieRepository,
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
                val movies = movieRepository.getMoviesForActor(actorName)
                _uiState.update {
                    it.copy(
                        isLoading  = false,
                        movies     = movies,
                        noCastData = movies.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error     = e.message ?: "Failed to load movies for $actorName"
                    )
                }
            }
        }
    }

    companion object {
        fun factory(actorName: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ActorMoviesViewModel(MAApplication.movieRepository, actorName)
            }
        }
    }
}
