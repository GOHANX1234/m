package com.mna.streaming.data.repository

import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.mna.streaming.BuildConfig
import com.mna.streaming.data.LocalProfileStore
import com.mna.streaming.data.SessionManager
import com.mna.streaming.network.ApiClient
import com.mna.streaming.network.models.AuthResult
import com.mna.streaming.network.models.DeviceTokenDeleteRequest
import com.mna.streaming.network.models.DeviceTokenRequest
import com.mna.streaming.network.models.MobileLoginRequest
import com.mna.streaming.network.models.MobileLoginResponse
import com.mna.streaming.network.models.SessionResponse
import com.mna.streaming.network.models.SessionUser
import com.mna.streaming.network.models.SignUpRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.resume

/**
 * Single source of truth for all authentication operations.
 *
 * Sign-in uses OkHttp directly (not Retrofit) so we can:
 *  - Inspect the HTTP status code (NextAuth always returns 200 for credential
 *    failures — failure is signalled by "error=CredentialsSignin" in the URL).
 *  - Intercept 302 redirects that indicate CSRF or other errors.
 *
 * All other calls go through the Retrofit AuthApiService.
 *
 * Push-notification device-token registration/removal is handled here because
 * it depends on the mobile Bearer token that is obtained alongside the regular
 * NextAuth session during sign-in.
 */
class AuthRepository(
    private val apiClient: ApiClient,
    private val sessionManager: SessionManager,
    private val localProfileStore: LocalProfileStore
) {

    private val service = apiClient.authApiService
    private val okHttp  = apiClient.okHttpClient
    private val baseUrl = apiClient.baseUrl.trimEnd('/')
    private val gson    = Gson()
    private val json    = "application/json; charset=utf-8".toMediaType()

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
            // Wipe local profile data so a subsequent login never sees
            // a previous user's watch history or watchlist.
            localProfileStore.clearAll()
        }
    }

    // ── Session check ─────────────────────────────────────────────────────────

    suspend fun getCurrentSession(): SessionResponse? = withContext(Dispatchers.IO) {
        try { service.getSession() } catch (_: Exception) { null }
    }

    // ── Push notifications — mobile login + device token registration ─────────

    /**
     * Obtains a native Bearer token via `/api/auth/mobile/login`, then registers
     * the current FCM device token with `/api/notifications/device-token`.
     *
     * Called after a successful NextAuth sign-in. All failures are swallowed —
     * push notifications are a non-critical feature and must never block login.
     */
    suspend fun mobileLoginAndRegisterToken(email: String, password: String) =
        withContext(Dispatchers.IO) {
            runCatching {
                // Step 1 — get a native Bearer token
                val loginBody = gson.toJson(MobileLoginRequest(email, password))
                    .toRequestBody(json)
                val loginReq = Request.Builder()
                    .url("$baseUrl/api/auth/mobile/login")
                    .post(loginBody)
                    .build()
                val loginResp = okHttp.newCall(loginReq).execute()
                if (!loginResp.isSuccessful) return@runCatching
                val loginJson = loginResp.body?.string() ?: return@runCatching
                val mobileLogin = gson.fromJson(loginJson, MobileLoginResponse::class.java)
                    ?: return@runCatching
                sessionManager.saveNativeToken(mobileLogin.accessToken)

                // Step 2 — register the FCM token
                registerDeviceTokenWithBearer(mobileLogin.accessToken)
            }
        }

    /**
     * Registers the current FCM device token with the Sarrows server using the
     * stored native Bearer token. Silently ignored if no Bearer token is saved
     * (i.e. the user hasn't gone through [mobileLoginAndRegisterToken] yet).
     *
     * Called from [SarrowsMessagingService.onNewToken] when Firebase refreshes the token.
     */
    suspend fun registerDeviceToken(fcmToken: String) = withContext(Dispatchers.IO) {
        val bearer = sessionManager.getNativeToken() ?: return@withContext
        runCatching { registerDeviceTokenWithBearer(bearer, fcmToken) }
    }

    /**
     * Removes the current FCM device token from the server, then clears the
     * stored native Bearer token. Called during sign-out.
     */
    suspend fun unregisterDeviceToken() = withContext(Dispatchers.IO) {
        runCatching {
            val bearer   = sessionManager.getNativeToken() ?: return@runCatching
            val fcmToken = getFcmToken()            ?: return@runCatching

            val body = gson.toJson(DeviceTokenDeleteRequest(fcmToken)).toRequestBody(json)
            val req = Request.Builder()
                .url("$baseUrl/api/notifications/device-token")
                .delete(body)
                .header("Authorization", "Bearer $bearer")
                .build()
            okHttp.newCall(req).execute().close()
        }
        // Always clear the locally stored token regardless of server response
        sessionManager.saveNativeToken("")
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Fetches the current FCM registration token from the Firebase SDK.
     * Returns null if Firebase is not initialised or the call fails.
     */
    private suspend fun getFcmToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> cont.resume(token) }
            .addOnFailureListener { cont.resume(null) }
    }

    /**
     * Sends `POST /api/notifications/device-token` with the given [bearer] token.
     * Fetches the current FCM token and stable device ID internally.
     */
    private suspend fun registerDeviceTokenWithBearer(
        bearer:   String,
        fcmToken: String? = null
    ) {
        val token    = fcmToken ?: getFcmToken() ?: return
        val deviceId = sessionManager.getOrCreateDeviceId()

        val bodyJson = gson.toJson(
            DeviceTokenRequest(
                token      = token,
                platform   = "android",
                deviceId   = deviceId,
                appVersion = BuildConfig.VERSION_NAME
            )
        )
        val req = Request.Builder()
            .url("$baseUrl/api/notifications/device-token")
            .post(bodyJson.toRequestBody(json))
            .header("Authorization", "Bearer $bearer")
            .build()
        okHttp.newCall(req).execute().close()
    }

    private data class SignInResponseUrl(val url: String = "")
}
