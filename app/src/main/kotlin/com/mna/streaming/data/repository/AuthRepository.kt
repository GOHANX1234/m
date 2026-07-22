package com.mna.streaming.data.repository

import com.google.gson.Gson
import com.mna.streaming.data.SessionManager
import com.mna.streaming.network.ApiClient
import com.mna.streaming.network.models.AuthResult
import com.mna.streaming.network.models.SessionResponse
import com.mna.streaming.network.models.SessionUser
import com.mna.streaming.network.models.SignUpRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request

/**
 * Single source of truth for all authentication operations.
 *
 * Sign-in uses OkHttp directly (not Retrofit) so we can:
 *  - Inspect the HTTP status code (NextAuth always returns 200 for credential
 *    failures — failure is signalled by "error=CredentialsSignin" in the URL).
 *  - Intercept 302 redirects that indicate CSRF or other errors.
 *
 * All other calls go through the Retrofit AuthApiService.
 */
class AuthRepository(
    private val apiClient: ApiClient,
    private val sessionManager: SessionManager
) {

    private val service = apiClient.authApiService
    private val okHttp  = apiClient.okHttpClient
    private val baseUrl = apiClient.baseUrl.trimEnd('/')
    private val gson    = Gson()

    // ── Startup session restore ───────────────────────────────────────────────

    /**
     * Called on app start. If a session token is saved in DataStore, pre-loads
     * it into the CookieJar and validates it with the server.
     *
     * @return the saved user if the session is still valid, null otherwise.
     */
    suspend fun restoreSession(): SessionUser? = withContext(Dispatchers.IO) {
        val token = sessionManager.getSessionToken() ?: return@withContext null

        // Pre-populate the cookie jar with the persisted token.
        // The server uses __Secure-authjs.session-token (not __Host-).
        apiClient.cookieJar.preload(
            host        = "sarrows.vercel.app",
            cookieName  = "__Secure-authjs.session-token",
            cookieValue = token
        )

        // Validate the token with the server
        return@withContext try {
            val session = service.getSession()
            if (session.user != null) {
                sessionManager.saveUser(session.user)
                session.user
            } else {
                sessionManager.clearSession()
                apiClient.cookieJar.clear()
                null
            }
        } catch (e: Exception) {
            // Network unavailable — fall back to cached user for offline UX
            sessionManager.getSavedUser()
        }
    }

    // ── Sign Up ───────────────────────────────────────────────────────────────

    suspend fun signUp(
        nickname: String,
        email: String,
        password: String
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val response = service.signUp(SignUpRequest(nickname, email, password))

            if (response.code() == 201) {
                // Success — the body says {"success":true}
                return@withContext AuthResult.Success(
                    SessionUser("", nickname, email, null, "user")
                )
            }

            // Non-201: always read error from errorBody() (Retrofit stores non-2xx
            // responses there; response.body() is null for 4xx/5xx).
            val rawError = response.errorBody()?.string().orEmpty()
            val errResp = runCatching {
                gson.fromJson(rawError, com.mna.streaming.network.models.SignUpResponse::class.java)
            }.getOrNull()

            AuthResult.Error(
                message     = errResp?.error ?: "Registration failed (${response.code()})",
                fieldErrors = errResp?.fieldErrors ?: emptyMap()
            )
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    // ── Sign In ───────────────────────────────────────────────────────────────

    /**
     * Full NextAuth credentials sign-in flow:
     *
     *  1. GET /api/auth/csrf       → csrfToken + __Host-authjs.csrf-token cookie
     *  2. POST /api/auth/callback/credentials (form-encoded)
     *     → 302 to callbackUrl on success (session cookie set)
     *     → 302 to /api/auth/error?error=CredentialsSignin on failure
     *  3. GET /api/auth/session    → retrieve and cache user data
     *
     *  NOTE: the endpoint is /callback/credentials, NOT /signin/credentials.
     *  /signin/credentials is the HTML sign-in page handler and always redirects
     *  back to the page, never issuing a session cookie.
     */
    suspend fun signIn(email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            // Step 1 — CSRF token (Retrofit; CookieJar auto-stores the csrf cookie)
            val csrfToken = try {
                service.getCsrfToken().csrfToken
            } catch (e: Exception) {
                return@withContext AuthResult.Error(
                    "Network error: ${e.message ?: e.javaClass.simpleName}"
                )
            }

            // Step 2 — Submit credentials via OkHttp for full response control
            val formBody = FormBody.Builder()
                .add("csrfToken", csrfToken)
                .add("email", email)
                .add("password", password)
                .add("callbackUrl", "$baseUrl/home")
                .build()

            val request = Request.Builder()
                .url("$baseUrl/api/auth/callback/credentials")
                .post(formBody)
                .build()

            val response = try {
                okHttp.newCall(request).execute()
            } catch (e: Exception) {
                return@withContext AuthResult.Error(
                    "Network error: ${e.message ?: e.javaClass.simpleName}"
                )
            }

            val responseBody   = response.body?.string() ?: ""
            val locationHeader = response.header("location") ?: ""

            // Detect failure:
            //  - Success: 302 with a clean location (callbackUrl, no "error=")
            //  - Failure: 302 to /api/auth/error?error=CredentialsSignin
            //  Do NOT fail on a bare 3xx — a successful login also returns 302.
            val isFailure = locationHeader.contains("error=") ||
                responseBody.contains("error=CredentialsSignin") ||
                (responseBody.isNotBlank() && runCatching {
                    gson.fromJson(responseBody, SignInResponseUrl::class.java).url
                }.getOrNull()?.contains("error=") == true)

            if (isFailure) {
                return@withContext AuthResult.Error("Invalid email or password")
            }

            // Step 3 — Fetch session to get user object
            val session = try {
                service.getSession()
            } catch (e: Exception) {
                return@withContext AuthResult.Error("Signed in but failed to load profile")
            }

            val user = session.user
                ?: return@withContext AuthResult.Error("Login failed — session not created")

            sessionManager.saveUser(user)
            return@withContext AuthResult.Success(user)
        }

    // ── Sign Out ──────────────────────────────────────────────────────────────

    suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        try {
            val csrfToken = service.getCsrfToken().csrfToken
            service.signOut(
                csrfToken   = csrfToken,
                callbackUrl = baseUrl
            )
        } catch (_: Exception) {
            // Best-effort: always clear local state regardless of server response
        } finally {
            sessionManager.clearSession()
            apiClient.cookieJar.clear()
        }
    }

    // ── Session check ─────────────────────────────────────────────────────────

    suspend fun getCurrentSession(): SessionResponse? = withContext(Dispatchers.IO) {
        try { service.getSession() } catch (_: Exception) { null }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private data class SignInResponseUrl(val url: String = "")
}
