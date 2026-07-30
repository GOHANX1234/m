package com.mna.streaming.ui.admin.episodes

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

data class AdminEpisodesUiState(
    val episodes: List<ApiAdminEpisode>  = emptyList(),
    val isLoading: Boolean               = false,
    val error: String?                   = null,
    val isSaving: Boolean                = false,
    val saveError: String?               = null,
    val editingEpisode: ApiAdminEpisode? = null,
    val showForm: Boolean                = false,
    val deletingId: String?              = null,
    // AniList does not expose per-episode titles — autofill removed
    val seriesAniListId: String?         = null
)

class AdminEpisodesViewModel(private val repo: AdminRepository) : ViewModel() {

    private val _state = MutableStateFlow(AdminEpisodesUiState())
    val state: StateFlow<AdminEpisodesUiState> = _state.asStateFlow()

    fun loadEpisodes(seriesId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val eps = repo.listEpisodes(seriesId)
                _state.update { it.copy(isLoading = false, episodes = eps) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setAniListId(aniListId: String?) = _state.update { it.copy(seriesAniListId = aniListId) }

    fun openCreateForm() = _state.update { it.copy(showForm = true, editingEpisode = null, saveError = null) }
    fun openEditForm(ep: ApiAdminEpisode) = _state.update { it.copy(showForm = true, editingEpisode = ep, saveError = null) }
    fun closeForm() = _state.update { it.copy(showForm = false, editingEpisode = null, saveError = null) }

    fun createEpisode(seriesId: String, season: Int, epNum: Int, title: String?, videoUrl: String?, videoType: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            try {
                val ep = repo.createEpisode(
                    CreateEpisodeRequest(
                        series        = seriesId,
                        season        = season,
                        episodeNumber = epNum,
                        title         = title?.takeIf { t -> t.isNotBlank() },
                        videoUrl      = videoUrl?.takeIf { v -> v.isNotBlank() },
                        videoType     = videoType?.takeIf { t -> t != "auto" }
                    )
                )
                _state.update { s ->
                    s.copy(
                        isSaving  = false,
                        showForm  = false,
                        episodes  = (s.episodes + ep).sortedWith(
                            compareBy({ it.season }, { it.episodeNumber })
                        )
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, saveError = e.message) }
            }
        }
    }

    fun updateEpisode(id: String, season: Int?, epNum: Int?, title: String?, videoUrl: String?, videoType: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            try {
                val ep = repo.updateEpisode(
                    id, UpdateEpisodeRequest(
                        season        = season,
                        episodeNumber = epNum,
                        title         = title?.takeIf { t -> t.isNotBlank() },
                        videoUrl      = videoUrl?.takeIf { v -> v.isNotBlank() },
                        videoType     = videoType?.takeIf { t -> t != "auto" }
                    )
                )
                _state.update { s ->
                    s.copy(
                        isSaving = false,
                        showForm = false,
                        episodes = s.episodes.map { if (it.id == id) ep else it }
                            .sortedWith(compareBy({ it.season }, { it.episodeNumber }))
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, saveError = e.message) }
            }
        }
    }

    fun deleteEpisode(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(deletingId = id) }
            try {
                repo.deleteEpisode(id)
                _state.update { s -> s.copy(deletingId = null, episodes = s.episodes.filter { it.id != id }) }
            } catch (e: Exception) {
                _state.update { it.copy(deletingId = null, error = e.message) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AdminEpisodesViewModel(MAApplication.adminRepository) }
        }
    }
}
