package com.mna.streaming.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
import com.mna.streaming.data.LocalProfileStore
import com.mna.streaming.data.LocalWatchEntry
import com.mna.streaming.data.LocalWatchlistItem
import com.mna.streaming.data.repository.MovieRepository
import com.mna.streaming.network.models.ContentRequest
import com.mna.streaming.network.models.SessionUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: SessionUser? = null,
    // ── Watch history ──────────────────────────────────────────────────────────
    val watchHistory: List<LocalWatchEntry> = emptyList(),
    val isLoadingHistory: Boolean = false,
    // ── Watchlist ─────────────────────────────────────────────────────────────
    val watchlist: List<LocalWatchlistItem> = emptyList(),
    val isLoadingWatchlist: Boolean = false,
    // ── Requests ──────────────────────────────────────────────────────────────
    val isLoadingRequests: Boolean = false,
    val requests: List<ContentRequest> = emptyList(),
    val requestsError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val cancellingId: String? = null   // ID of the request currently being cancelled
)

class ProfileViewModel(
    private val movieRepository: MovieRepository,
    private val localProfileStore: LocalProfileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Restore the user from the persisted session immediately —
            // no extra network call needed since we cached it at login.
            val user = MAApplication.sessionManager.getSavedUser()
            _uiState.update { it.copy(user = user) }
        }
        loadHistory()
        loadWatchlist()
        loadRequests()
    }

    // ── Watch History ─────────────────────────────────────────────────────────

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }
            val history = localProfileStore.getWatchHistory()
            _uiState.update { it.copy(watchHistory = history, isLoadingHistory = false) }
        }
    }

    // ── Watchlist ─────────────────────────────────────────────────────────────

    fun loadWatchlist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWatchlist = true) }
            val list = localProfileStore.getWatchlist()
            _uiState.update { it.copy(watchlist = list, isLoadingWatchlist = false) }
        }
    }

    // ── Requests ──────────────────────────────────────────────────────────────

    fun loadRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRequests = true, requestsError = null) }
            try {
                val requests = movieRepository.getRequests()
                _uiState.update { it.copy(isLoadingRequests = false, requests = requests) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingRequests = false,
                        requestsError     = e.message ?: "Failed to load requests"
                    )
                }
            }
        }
    }

    fun submitRequest(title: String, type: String, note: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }
            try {
                val newRequest = movieRepository.submitRequest(title, type, note)
                // Prepend to list so the newest request shows at the top immediately
                _uiState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        requests     = listOf(newRequest) + state.requests
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submitError  = e.message ?: "Failed to submit request"
                    )
                }
            }
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cancellingId = requestId) }
            try {
                movieRepository.cancelRequest(requestId)
                // Remove from the list locally — no need to re-fetch
                _uiState.update { state ->
                    state.copy(
                        cancellingId = null,
                        requests     = state.requests.filter { it.id != requestId }
                    )
                }
            } catch (e: Exception) {
                // Server rejected the cancel; just clear the loading spinner
                _uiState.update { it.copy(cancellingId = null) }
            }
        }
    }

    fun clearSubmitError() {
        _uiState.update { it.copy(submitError = null) }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProfileViewModel(
                    movieRepository   = MAApplication.movieRepository,
                    localProfileStore = MAApplication.localProfileStore
                )
            }
        }
    }
}
