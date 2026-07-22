package com.mna.streaming.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
import com.mna.streaming.data.model.Movie
import com.mna.streaming.data.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<Movie> = emptyList(),
    val hasSearched: Boolean = false,   // true after first search attempt
    val error: String? = null
)

class SearchViewModel(
    private val movieRepository: MovieRepository
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
                val results = movieRepository.search(query)
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        results     = results,
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
            initializer { SearchViewModel(MAApplication.movieRepository) }
        }
    }
}
