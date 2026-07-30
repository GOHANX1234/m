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

// ─── Mobile (native) login ───────────────────────────────────────────────────
//
// POST /api/auth/mobile/login
// Returns a 30-day encrypted Bearer token for native API calls.

data class MobileLoginRequest(
    val email: String,
    val password: String
)

data class MobileLoginUser(
    val id: String,
    val nickname: String,
    val email: String,
    val image: String? = null,
    val role: String
)

data class MobileLoginResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: MobileLoginUser
)

// ─── Device token (push notifications) ───────────────────────────────────────
//
// POST  /api/notifications/device-token  — register or refresh
// DELETE /api/notifications/device-token — remove on logout

data class DeviceTokenRequest(
    val token: String,
    val platform: String = "android",
    val deviceId: String? = null,
    val appVersion: String
)

data class DeviceTokenDeleteRequest(
    val token: String
)
