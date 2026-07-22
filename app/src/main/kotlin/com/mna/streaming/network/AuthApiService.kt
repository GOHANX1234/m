package com.mna.streaming.network

import com.mna.streaming.network.models.CsrfResponse
import com.mna.streaming.network.models.SessionResponse
import com.mna.streaming.network.models.SignOutResponse
import com.mna.streaming.network.models.SignUpRequest
import com.mna.streaming.network.models.SignUpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {

    /** Step 1 of sign-in: obtain a CSRF token + csrf cookie. */
    @GET("api/auth/csrf")
    suspend fun getCsrfToken(): CsrfResponse

    /** Get the current session (returns empty body if unauthenticated). */
    @GET("api/auth/session")
    suspend fun getSession(): SessionResponse

    /**
     * Create a new user account.
     * Uses Response<> wrapper so we can inspect HTTP status (201 / 400 / 409).
     */
    @POST("api/auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): Response<SignUpResponse>

    /**
     * Sign out — clears the session cookie on the server.
     * The CSRF token and callbackUrl are sent as form fields.
     */
    @FormUrlEncoded
    @POST("api/auth/signout")
    suspend fun signOut(
        @Field("csrfToken") csrfToken: String,
        @Field("callbackUrl") callbackUrl: String,
        @Field("json") json: String = "true"
    ): Response<SignOutResponse>
}
