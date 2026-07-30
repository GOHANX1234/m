package com.mna.streaming.network

import com.mna.streaming.network.models.VersionCheckRequest
import com.mna.streaming.network.models.VersionCheckResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the Sarrows OTA update-check endpoint.
 *
 * The path string is intentionally NOT used here as a literal — the actual
 * path is decoded from the native layer (ma-check.c) and injected at runtime
 * via a dynamic Retrofit @Url call in UpdateRepository.
 */
interface UpdateApiService {

    /**
     * Checks whether a newer version of the app is available.
     *
     * No authentication is required — this is a public endpoint.
     * The full URL is built at call-site from [NativeApiSecurity.getBaseUrl] +
     * [NativeApiSecurity.getUpdateCheckPath] so neither appears as a literal
     * in the DEX.
     *
     * The path "/api/app/version/check" is passed as a @Url parameter so Retrofit
     * treats it as a full relative URL against the base, avoiding any hardcoded
     * path string in Kotlin source.
     */
    @POST
    suspend fun checkVersion(
        @retrofit2.http.Url url: String,
        @Body body: VersionCheckRequest
    ): VersionCheckResponse
}
