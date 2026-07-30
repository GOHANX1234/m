package com.mna.streaming.ui.admin.series

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

data class AdminSeriesUiState(
    val seriesList: List<AdminSeries>             = emptyList(),
    val isLoading: Boolean                        = false,
    val error: String?                            = null,
    val isSaving: Boolean                         = false,
    val saveError: String?                        = null,
    val editingSeries: AdminSeries?               = null,
    val isLoadingEdit: Boolean                    = false,
    val editLoadError: String?                    = null,
    val genres: List<ApiGenre>                    = emptyList(),
    // ── AniList autofill (anime) ──────────────────────────────────────────
    val aniListResults: List<AniListSearchResult> = emptyList(),
    val isSearchingAniList: Boolean               = false,
    val aniListCast: List<AniListCastMember>      = emptyList(),
    /**
     * Genre ObjectIds resolved from AniList genreNames via POST /api/admin/genres.
     * Null = no resolution has run yet.
     */
    val aniListResolvedGenreIds: Set<String>?     = null,
    // ── TMDB autofill (web series) ────────────────────────────────────────
    val tmdbResults: List<TmdbSearchResult>       = emptyList(),
    val isSearchingTmdb: Boolean                  = false,
    val tmdbTvDetails: TmdbTvDetailsResponse?     = null,
    val isLoadingTmdbDetails: Boolean             = false,
    /**
     * Genre ObjectIds resolved from TMDB TV genreNames via POST /api/admin/genres.
     * Null = no resolution has run yet.
     */
    val tmdbResolvedGenreIds: Set<String>?        = null,
    //
    val deletingId: String?                       = null,
    val currentType: String                       = "anime"   // "anime" | "series"
)

class AdminSeriesViewModel(private val repo: AdminRepository) : ViewModel() {

    private val _state = MutableStateFlow(AdminSeriesUiState())
    val state: StateFlow<AdminSeriesUiState> = _state.asStateFlow()

    init {
        loadSeries("anime")
        loadGenres()
    }

    fun loadSeries(type: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, currentType = type) }
            try {
                val resp = repo.listSeries(type = type, limit = 50)
                _state.update { it.copy(isLoading = false, seriesList = resp.series) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadGenres() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(genres = repo.listGenres()) }
            } catch (_: Exception) {}
        }
    }

    fun loadEditSeries(id: String) {
        val found = _state.value.seriesList.find { it.id == id }
        _state.update { it.copy(editingSeries = found) }
    }

    fun clearEditSeries() = _state.update {
        it.copy(
            editingSeries           = null,
            saveError               = null,
            editLoadError           = null,
            aniListResolvedGenreIds = null,
            tmdbResolvedGenreIds    = null
        )
    }

    fun createSeries(req: CreateSeriesRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            try {
                val s = repo.createSeries(req)
                _state.update { st -> st.copy(isSaving = false, seriesList = listOf(s) + st.seriesList) }
                onSuccess()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, saveError = e.message) }
            }
        }
    }

    fun updateSeries(id: String, req: UpdateSeriesRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            try {
                val s = repo.updateSeries(id, req)
                _state.update { st ->
                    st.copy(isSaving = false, seriesList = st.seriesList.map { if (it.id == id) s else it })
                }
                onSuccess()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, saveError = e.message) }
            }
        }
    }

    fun deleteSeries(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(deletingId = id) }
            try {
                repo.deleteSeries(id)
                _state.update { st -> st.copy(deletingId = null, seriesList = st.seriesList.filter { it.id != id }) }
            } catch (e: Exception) {
                _state.update { it.copy(deletingId = null, error = e.message) }
            }
        }
    }

    // ── AniList autofill ──────────────────────────────────────────────────────

    fun searchAniList(query: String) {
        if (query.isBlank()) { _state.update { it.copy(aniListResults = emptyList()) }; return }
        viewModelScope.launch {
            _state.update { it.copy(isSearchingAniList = true) }
            try {
                val results = repo.searchAniList(query)
                _state.update { it.copy(isSearchingAniList = false, aniListResults = results) }
            } catch (_: Exception) {
                _state.update { it.copy(isSearchingAniList = false, aniListResults = emptyList()) }
            }
        }
    }

    fun loadAniListCast(aniListId: String) {
        viewModelScope.launch {
            try {
                val cast = repo.getAniListCharacters(aniListId)
                _state.update { it.copy(aniListCast = cast) }
            } catch (_: Exception) {}
        }
    }

    /**
     * Resolves AniList genre names to local ObjectIds via POST /api/admin/genres
     * (spec §11.1, §15.4). Idempotent: existing genres are returned unchanged,
     * new ones are created. Updates [AdminSeriesUiState.aniListResolvedGenreIds].
     */
    fun resolveAniListGenres(genreNames: List<String>?) {
        val names = genreNames ?: return
        if (names.isEmpty()) return
        viewModelScope.launch {
            try {
                val ids = repo.resolveGenreNames(names).toSet()
                _state.update { it.copy(aniListResolvedGenreIds = ids) }
            } catch (_: Exception) {}
        }
    }

    /**
     * Clears the AniList search results and cast list (e.g. when the search dialog closes).
     * Does NOT clear [AdminSeriesUiState.aniListResolvedGenreIds] because genre resolution
     * is async and may still be in flight when this is called from applyAniListResult().
     * Resolved IDs are reset in [clearEditSeries] when the form is torn down.
     */
    fun clearAniList() = _state.update {
        it.copy(aniListResults = emptyList(), aniListCast = emptyList())
    }

    // ── TMDB TV autofill (web series) ─────────────────────────────────────────

    fun searchTmdb(query: String) {
        if (query.isBlank()) { _state.update { it.copy(tmdbResults = emptyList()) }; return }
        viewModelScope.launch {
            _state.update { it.copy(isSearchingTmdb = true) }
            try {
                val results = repo.searchTmdb(query, type = "tv")
                _state.update { it.copy(isSearchingTmdb = false, tmdbResults = results) }
            } catch (_: Exception) {
                _state.update { it.copy(isSearchingTmdb = false, tmdbResults = emptyList()) }
            }
        }
    }

    fun loadTmdbTvDetails(tmdbId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingTmdbDetails = true, tmdbResolvedGenreIds = null) }
            try {
                val details = repo.getTmdbTvDetails(tmdbId)
                // Resolve genre names via POST /api/admin/genres (spec §10.3, §15.3).
                val resolvedIds = repo.resolveGenreNames(details.genreNames).toSet()
                _state.update { it.copy(isLoadingTmdbDetails = false, tmdbTvDetails = details, tmdbResolvedGenreIds = resolvedIds) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoadingTmdbDetails = false, tmdbTvDetails = null) }
            }
        }
    }

    /**
     * Clears TMDB search results (e.g. when the TMDB dialog closes).
     * Does NOT clear [AdminSeriesUiState.tmdbResolvedGenreIds] because genre resolution
     * is async and may still be in flight when this is called from applyTmdbResult().
     * Resolved IDs are reset in [clearEditSeries] when the form is torn down.
     */
    fun clearTmdb() = _state.update { it.copy(tmdbResults = emptyList(), tmdbTvDetails = null) }

    // ── Common ────────────────────────────────────────────────────────────────

    fun clearSaveError() = _state.update { it.copy(saveError = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AdminSeriesViewModel(MAApplication.adminRepository) }
        }
    }
}
