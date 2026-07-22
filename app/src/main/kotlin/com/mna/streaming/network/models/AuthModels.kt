package com.mna.streaming.network.models

import com.google.gson.annotations.SerializedName

// ─── Sign Up ────────────────────────────────────────────────────────────────

data class SignUpRequest(
    val nickname: String,
    val email: String,
    val password: String
)

data class SignUpResponse(
    val success: Boolean? = null,
    val error: String? = null,
    val fieldErrors: Map<String, String>? = null
)

// ─── CSRF ────────────────────────────────────────────────────────────────────

data class CsrfResponse(
    val csrfToken: String
)

// ─── Sign In ─────────────────────────────────────────────────────────────────

data class SignInResponse(
    val url: String = ""
)

// ─── Session ─────────────────────────────────────────────────────────────────

data class SessionResponse(
    val user: SessionUser? = null,
    val expires: String? = null
)

data class SessionUser(
    val id: String,
    val name: String,           // nickname
    val email: String,
    val image: String? = null,
    val role: String            // "user" or "admin"
)

// ─── Sign Out ────────────────────────────────────────────────────────────────

data class SignOutResponse(
    val url: String = ""
)

// ─── Auth Result (internal) ──────────────────────────────────────────────────

sealed class AuthResult {
    data class Success(val user: SessionUser) : AuthResult()
    data class Error(
        val message: String,
        val fieldErrors: Map<String, String> = emptyMap()
    ) : AuthResult()
}
