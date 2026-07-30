package com.mna.streaming.ui.admin.genres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
import com.mna.streaming.data.repository.AdminRepository
import com.mna.streaming.network.models.ApiGenre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminGenresUiState(
    val genres: List<ApiGenre> = emptyList(),
    val isLoading: Boolean     = false,
    val error: String?         = null,
    val isCreating: Boolean    = false,
    val createError: String?   = null,
    val deletingId: String?    = null
)

class AdminGenresViewModel(private val repo: AdminRepository) : ViewModel() {

    private val _state = MutableStateFlow(AdminGenresUiState())
    val state: StateFlow<AdminGenresUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val genres = repo.listGenres()
                _state.update { it.copy(isLoading = false, genres = genres) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun create(name: String, onSuccess: () -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, createError = null) }
            try {
                val genre = repo.createGenre(name.trim())
                _state.update { s ->
                    // createGenre is idempotent — avoid duplicate if server returns existing
                    val existing = s.genres.any { it.id == genre.id }
                    val updated  = if (existing) s.genres else (s.genres + genre).sortedBy { it.name }
                    s.copy(isCreating = false, genres = updated)
                }
                onSuccess()
            } catch (e: Exception) {
                _state.update { it.copy(isCreating = false, createError = e.message) }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(deletingId = id) }
            try {
                repo.deleteGenre(id)
                _state.update { s -> s.copy(deletingId = null, genres = s.genres.filter { it.id != id }) }
            } catch (e: Exception) {
                _state.update { it.copy(deletingId = null, error = e.message) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AdminGenresViewModel(MAApplication.adminRepository) }
        }
    }
}
