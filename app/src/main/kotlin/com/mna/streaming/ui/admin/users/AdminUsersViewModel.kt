package com.mna.streaming.ui.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
import com.mna.streaming.data.repository.AdminRepository
import com.mna.streaming.network.models.AdminUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUsersUiState(
    val users: List<AdminUser> = emptyList(),
    val isLoading: Boolean     = false,
    val error: String?         = null,
    val updatingId: String?    = null,
    val searchQuery: String    = "",
    val roleFilter: String?    = null,
    val total: Int             = 0
)

class AdminUsersViewModel(private val repo: AdminRepository) : ViewModel() {

    private val _state = MutableStateFlow(AdminUsersUiState())
    val state: StateFlow<AdminUsersUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val resp = repo.listUsers(
                    page  = 1,
                    limit = 100,
                    role  = s.roleFilter,
                    q     = s.searchQuery.takeIf { it.isNotBlank() }
                )
                _state.update { it.copy(isLoading = false, users = resp.users, total = resp.total) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setSearch(q: String) {
        _state.update { it.copy(searchQuery = q) }
        load()
    }

    fun setRoleFilter(role: String?) {
        _state.update { it.copy(roleFilter = role) }
        load()
    }

    fun updateRole(userId: String, newRole: String) {
        viewModelScope.launch {
            _state.update { it.copy(updatingId = userId) }
            try {
                val updated = repo.updateUserRole(userId, newRole)
                _state.update { s ->
                    s.copy(
                        updatingId = null,
                        users      = s.users.map { if (it.id == userId) updated else it }
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(updatingId = null, error = e.message) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AdminUsersViewModel(MAApplication.adminRepository) }
        }
    }
}
