package com.mna.streaming.ui.admin.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
import com.mna.streaming.data.repository.AdminRepository
import com.mna.streaming.network.models.AdminContentRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminRequestsUiState(
    val requests: List<AdminContentRequest> = emptyList(),
    val isLoading: Boolean                  = false,
    val error: String?                      = null,
    val updatingId: String?                 = null,
    val deletingId: String?                 = null
)

class AdminRequestsViewModel(private val repo: AdminRepository) : ViewModel() {

    private val _state = MutableStateFlow(AdminRequestsUiState())
    val state: StateFlow<AdminRequestsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val requests = repo.listRequests()
                _state.update { it.copy(isLoading = false, requests = requests) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateStatus(id: String, status: String, adminNote: String?) {
        viewModelScope.launch {
            _state.update { it.copy(updatingId = id) }
            try {
                val updated = repo.updateRequest(id, status, adminNote)
                _state.update { s ->
                    s.copy(
                        updatingId = null,
                        requests   = s.requests.map { if (it.id == id) updated else it }
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(updatingId = null, error = e.message) }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(deletingId = id) }
            try {
                repo.deleteRequest(id)
                _state.update { s -> s.copy(deletingId = null, requests = s.requests.filter { it.id != id }) }
            } catch (e: Exception) {
                _state.update { it.copy(deletingId = null, error = e.message) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AdminRequestsViewModel(MAApplication.adminRepository) }
        }
    }
}
