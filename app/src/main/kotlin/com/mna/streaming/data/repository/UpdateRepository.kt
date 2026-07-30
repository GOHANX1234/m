package com.mna.streaming.data.repository

import android.content.Context
import android.provider.Settings
import com.mna.streaming.network.ApiClient
import com.mna.streaming.network.models.VersionCheckRequest
import com.mna.streaming.network.models.VersionCheckResponse
import com.mna.streaming.security.NativeApiSecurity
import kotlin.math.absoluteValue

/**
 * Handles the OTA version-check API call and all rollout / gate logic.
 *
 * Strings used in the request (platform, channel, URL path) are decoded from
 * the native layer at call-time so they never appear as Kotlin literals.
 */
class UpdateRepository(private val apiClient: ApiClient) {

    /**
     * Calls POST /api/app/version/check and returns the server response.
     *
     * Returns [Result.failure] on any network or parsing error — callers must
     * silently ignore failures and never block the user on a connectivity issue.
     */
    suspend fun checkForUpdate(currentVersionCode: Int): Result<VersionCheckResponse> {
        return try {
            val fullUrl = NativeApiSecurity.getBaseUrl() +
                    NativeApiSecurity.getUpdateCheckPath()

            val body = VersionCheckRequest(
                versionCode = currentVersionCode,
                platform    = NativeApiSecurity.getUpdatePlatform(),
                channel     = NativeApiSecurity.getUpdateChannel()
            )

            val response = apiClient.updateApiService.checkVersion(fullUrl, body)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Determines whether this device falls inside the server's rollout slice.
     *
     * Uses a deterministic hash of [Settings.Secure.ANDROID_ID] so the same
     * device always gets the same answer, matching the spec in the API docs.
     *
     * @param rolloutPercentage 0–100 from the server response; null treated as 100.
     * @param context           Required to read [Settings.Secure.ANDROID_ID].
     */
    fun isInRollout(rolloutPercentage: Int?, context: Context): Boolean {
        val pct = rolloutPercentage ?: 100
        if (pct >= 100) return true
        if (pct <= 0) return false
        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: return true // unknown device → include in rollout
        // Math.floorMod avoids the Int.MIN_VALUE edge-case where absoluteValue
        // returns a negative value, which would make the bucket calculation wrong.
        val bucket = Math.floorMod(deviceId.hashCode(), 100) + 1  // 1..100
        return bucket <= pct
    }
}
