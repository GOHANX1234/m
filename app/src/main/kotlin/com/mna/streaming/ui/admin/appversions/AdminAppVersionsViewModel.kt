package com.mna.streaming.ui.admin.appversions

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

data class AdminAppVersionsUiState(
    val versions: List<AdminAppVersion> = emptyList(),
    val isLoading: Boolean              = false,
    val error: String?                  = null,
    val isSaving: Boolean               = false,
    val saveError: String?              = null,
    val editingVersion: AdminAppVersion? = null,
    val deletingId: String?             = null,
    // filter
    val filterPlatform: String?         = null,   // null = all
    val filterChannel: String?          = null    // null = all
)

class AdminAppVersionsViewModel(private val repo: AdminRepository) : ViewModel() {

    private val _state = MutableStateFlow(AdminAppVersionsUiState())
    val state: StateFlow<AdminAppVersionsUiState> = _state.asStateFlow()

    init { loadVersions() }

    fun loadVersions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val resp = repo.listAppVersions(
                    page     = 1,
                    limit    = 50,
                    platform = _state.value.filterPlatform,
                    channel  = _state.value.filterChannel
                )
                _state.update { it.copy(isLoading = false, versions = resp.versions) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setFilter(platform: String?, channel: String?) {
        _state.update { it.copy(filterPlatform = platform, filterChannel = channel) }
        loadVersions()
    }

    fun loadEditVersion(id: String) {
        val found = _state.value.versions.find { it.id == id }
        _state.update { it.copy(editingVersion = found) }
    }

    fun clearEditVersion() = _state.update { it.copy(editingVersion = null, saveError = null) }

    fun createVersion(req: CreateAppVersionRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            try {
                val v = repo.createAppVersion(req)
                _state.update { st -> st.copy(isSaving = false, versions = listOf(v) + st.versions) }
                onSuccess()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, saveError = e.message) }
            }
        }
    }

    fun updateVersion(id: String, req: UpdateAppVersionRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            try {
                val v = repo.updateAppVersion(id, req)
                _state.update { st ->
                    st.copy(isSaving = false, versions = st.versions.map { if (it.id == id) v else it })
                }
                onSuccess()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, saveError = e.message) }
            }
        }
    }

    fun deleteVersion(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(deletingId = id) }
            try {
                repo.deleteAppVersion(id)
                _state.update { st ->
                    st.copy(deletingId = null, versions = st.versions.filter { it.id != id })
                }
            } catch (e: Exception) {
                _state.update { it.copy(deletingId = null, error = e.message) }
            }
        }
    }

    fun clearSaveError() = _state.update { it.copy(saveError = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AdminAppVersionsViewModel(MAApplication.adminRepository) }
        }
    }
}
