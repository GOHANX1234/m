package com.mna.streaming.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
import com.mna.streaming.data.model.Category
import com.mna.streaming.data.model.Movie
import com.mna.streaming.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val featuredMovies: List<Movie> = emptyList(),
    val categories: List<Category> = emptyList(),
    val error: String? = null
)

class HomeViewModel(
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val (featuredMovies, categories) = movieRepository.getHomeCategories()
                _uiState.update {
                    it.copy(
                        isLoading      = false,
                        featuredMovies = featuredMovies,
                        categories     = categories
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error     = e.message ?: "Failed to load movies"
                    )
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(MAApplication.movieRepository) }
        }
    }
}
