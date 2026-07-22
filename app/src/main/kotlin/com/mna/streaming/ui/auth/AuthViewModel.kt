package com.mna.streaming.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
import com.mna.streaming.data.repository.AuthRepository
import com.mna.streaming.network.models.AuthResult
import com.mna.streaming.network.models.SessionUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── UI state models ───────────────────────────────────────────────────────────

data class AuthUiState(
    val isLoading: Boolean = false,
    val currentUser: SessionUser? = null,
    val isSessionChecked: Boolean = false,   // true once startup restore is done
    val loginError: String? = null,
    val signupError: String? = null,
    val loginFieldErrors: Map<String, String> = emptyMap(),
    val signupFieldErrors: Map<String, String> = emptyMap(),
    val signupSuccess: Boolean = false        // navigate to Login after successful signup
)

// ── ViewModel ────────────────────────────────────────────────────────────────

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        restoreSession()
    }

    // ── Session restore ───────────────────────────────────────────────────────

    private fun restoreSession() {
        viewModelScope.launch {
            val user = authRepository.restoreSession()
            _uiState.update {
                it.copy(
                    currentUser    = user,
                    isSessionChecked = true
                )
            }
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loginError = null, loginFieldErrors = emptyMap()) }

            when (val result = authRepository.signIn(email.trim(), password)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, currentUser = result.user)
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading        = false,
                            loginError       = result.message,
                            loginFieldErrors = result.fieldErrors
                        )
                    }
                }
            }
        }
    }

    // ── Sign Up ───────────────────────────────────────────────────────────────

    fun signUp(nickname: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, signupError = null, signupFieldErrors = emptyMap(), signupSuccess = false)
            }

            when (val result = authRepository.signUp(nickname.trim(), email.trim(), password)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, signupSuccess = true) }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading         = false,
                            signupError       = result.message,
                            signupFieldErrors = result.fieldErrors
                        )
                    }
                }
            }
        }
    }

    // ── Sign Out ──────────────────────────────────────────────────────────────

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.signOut()
            _uiState.update {
                it.copy(isLoading = false, currentUser = null, signupSuccess = false)
            }
        }
    }

    // ── Error clearing ────────────────────────────────────────────────────────

    fun clearLoginErrors() {
        _uiState.update { it.copy(loginError = null, loginFieldErrors = emptyMap()) }
    }

    fun clearSignupErrors() {
        _uiState.update {
            it.copy(signupError = null, signupFieldErrors = emptyMap(), signupSuccess = false)
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AuthViewModel(MAApplication.authRepository)
            }
        }
    }
}
