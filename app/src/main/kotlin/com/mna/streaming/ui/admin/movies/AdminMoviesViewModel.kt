package com.mna.streaming.ui.admin.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
import com.mna.streaming.data.repository.AdminRepository
import com.mna.streaming.network.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminMoviesUiState(
    val movies: List<AdminMovie>       = emptyList(),
    val isLoading: Boolean             = false,
    val error: String?                 = null,
    // Form state
    val isSaving: Boolean              = false,
    val saveError: String?             = null,
    val editingMovie: AdminMovie?      = null,
    val isLoadingEdit: Boolean         = false,
    // Genres for picker
    val genres: List<ApiGenre>         = emptyList(),
    // TMDB autofill
    val tmdbResults: List<TmdbSearchResult> = emptyList(),
    val isSearchingTmdb: Boolean       = false,
    val tmdbDetails: TmdbMovieDetailsResponse? = null,
    val isLoadingTmdbDetails: Boolean  = false,
    /**
     * Genre ObjectIds resolved from TMDB genreNames via POST /api/admin/genres.
     * Null = no resolution has run yet. Set alongside tmdbDetails.
     */
    val tmdbResolvedGenreIds: Set<String>? = null,
    // Delete
    val deletingId: String?            = null
)

class AdminMoviesViewModel(private val repo: AdminRepository) : ViewModel() {

    private val _state = MutableStateFlow(AdminMoviesUiState())
    val state: StateFlow<AdminMoviesUiState> = _state.asStateFlow()

    init {
        loadMovies()
        loadGenres()
    }

    fun loadMovies() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val resp = repo.listMovies(limit = 50)
                _state.update { it.copy(isLoading = false, movies = resp.movies) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadGenres() {
        viewModelScope.launch {
            try {
                val genres = repo.listGenres()
                _state.update { it.copy(genres = genres) }
            } catch (_: Exception) {}
        }
    }

    fun loadEditMovie(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingEdit = true, editingMovie = null) }
            try {
                val movie = repo.getMovie(id)
                _state.update { it.copy(isLoadingEdit = false, editingMovie = movie) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingEdit = false, saveError = e.message) }
            }
        }
    }

    fun clearEditMovie() = _state.update {
        it.copy(editingMovie = null, saveError = null, tmdbResolvedGenreIds = null)
    }

    fun createMovie(req: CreateMovieRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            try {
                val movie = repo.createMovie(req)
                _state.update { s ->
                    s.copy(isSaving = false, movies = listOf(movie) + s.movies)
                }
                onSuccess()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, saveError = e.message) }
            }
        }
    }

    fun updateMovie(id: String, req: UpdateMovieRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            try {
                val movie = repo.updateMovie(id, req)
                _state.update { s ->
                    s.copy(
                        isSaving = false,
                        movies   = s.movies.map { if (it.id == id) movie else it }
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, saveError = e.message) }
            }
        }
    }

    fun deleteMovie(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(deletingId = id) }
            try {
                repo.deleteMovie(id)
                _state.update { s ->
                    s.copy(deletingId = null, movies = s.movies.filter { it.id != id })
                }
            } catch (e: Exception) {
                _state.update { it.copy(deletingId = null, error = e.message) }
            }
        }
    }

    // ── TMDB autofill ─────────────────────────────────────────────────────────

    fun searchTmdb(query: String) {
        if (query.isBlank()) { _state.update { it.copy(tmdbResults = emptyList()) }; return }
        viewModelScope.launch {
            _state.update { it.copy(isSearchingTmdb = true) }
            try {
                val results = repo.searchTmdb(query, "movie")
                _state.update { it.copy(isSearchingTmdb = false, tmdbResults = results) }
            } catch (_: Exception) {
                _state.update { it.copy(isSearchingTmdb = false, tmdbResults = emptyList()) }
            }
        }
    }

    fun loadTmdbDetails(tmdbId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingTmdbDetails = true, tmdbDetails = null, tmdbResolvedGenreIds = null) }
            try {
                val details = repo.getTmdbMovieDetails(tmdbId)
                // Resolve genre names via POST /api/admin/genres (spec §10.2, §15.2).
                // This is idempotent: existing genres are returned unchanged, new ones
                // are created. We must call the API rather than doing local name-matching
                // so that genres not yet in the local list are created and returned.
                val resolvedIds = repo.resolveGenreNames(details.genreNames).toSet()
                _state.update { it.copy(isLoadingTmdbDetails = false, tmdbDetails = details, tmdbResolvedGenreIds = resolvedIds) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoadingTmdbDetails = false) }
            }
        }
    }

    /**
     * Clears TMDB search results (e.g. when the TMDB dialog closes).
     * Does NOT clear [AdminMoviesUiState.tmdbResolvedGenreIds] because genre resolution
     * is async and may still be in flight when this is called from applyTmdbResult().
     * Resolved IDs are reset in [clearEditMovie] when the form is torn down.
     */
    fun clearTmdb() = _state.update {
        it.copy(tmdbResults = emptyList(), tmdbDetails = null)
    }

    fun clearSaveError() = _state.update { it.copy(saveError = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AdminMoviesViewModel(MAApplication.adminRepository) }
        }
    }
}
