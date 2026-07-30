package com.mna.streaming.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
import com.mna.streaming.data.repository.AnimeRepository
import com.mna.streaming.network.models.ApiAnime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnimeUiState(
    val isLoading: Boolean = true,
    val anime: List<ApiAnime> = emptyList(),
    val genres: List<String> = emptyList(),     // extracted from loaded anime
    val error: String? = null,
    // Filter state
    val selectedGenre: String? = null,
    val selectedStatus: String? = null,         // null | "ongoing" | "completed"
    val selectedSort: String = "latest",        // "latest" | "views" | "rating"
    // Pagination
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoadingMore: Boolean = false
)

class AnimeViewModel(
    private val animeRepository: AnimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimeUiState())
    val uiState: StateFlow<AnimeUiState> = _uiState.asStateFlow()

    init {
        loadAnime(reset = true)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun setSort(sort: String) {
        if (_uiState.value.selectedSort == sort) return
        _uiState.update { it.copy(selectedSort = sort) }
        loadAnime(reset = true)
    }

    fun setGenre(genre: String?) {
        if (_uiState.value.selectedGenre == genre) return
        _uiState.update { it.copy(selectedGenre = genre) }
        loadAnime(reset = true)
    }

    fun setStatus(status: String?) {
        if (_uiState.value.selectedStatus == status) return
        _uiState.update { it.copy(selectedStatus = status) }
        loadAnime(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.currentPage >= state.totalPages) return
        loadAnime(reset = false)
    }

    fun retry() = loadAnime(reset = true)

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun loadAnime(reset: Boolean) {
        val current = _uiState.value
        val nextPage = if (reset) 1 else current.currentPage + 1

        viewModelScope.launch {
            _uiState.update {
                if (reset) it.copy(isLoading = true, error = null, anime = emptyList())
                else it.copy(isLoadingMore = true)
            }
            try {
                val response = animeRepository.getAnime(
                    genre  = current.selectedGenre,
                    status = current.selectedStatus,
                    sort   = current.selectedSort,
                    page   = nextPage,
                    limit  = 24
                )
                _uiState.update { state ->
                    val newList = if (reset) response.series
                                 else state.anime + response.series

                    // Extract unique genre names from all loaded anime for the filter chips
                    val genres = newList
                        .flatMap { it.genres.map { g -> g.name } }
                        .distinct()
                        .sorted()

                    state.copy(
                        isLoading    = false,
                        isLoadingMore = false,
                        error        = null,
                        anime        = newList,
                        genres       = genres,
                        currentPage  = nextPage,
                        totalPages   = response.totalPages
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading     = false,
                        isLoadingMore = false,
                        error         = e.message ?: "Failed to load anime"
                    )
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AnimeViewModel(MAApplication.animeRepository) }
        }
    }
}
