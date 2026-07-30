package com.mna.streaming.network.models

/**
 * Request body sent to POST /api/app/version/check.
 *
 * All string values (platform, channel) are decoded from the native layer
 * at call-site so they never appear as Kotlin string literals.
 */
data class VersionCheckRequest(
    val versionCode: Int,
    val platform: String,
    val channel: String
)

/**
 * Response from the Sarrows OTA update API.
 *
 * Fields match the server contract exactly; nullable fields are absent
 * from the JSON when no update is available.
 */
data class VersionCheckResponse(
    /** true if latestVersionCode > current versionCode */
    val updateAvailable: Boolean,
    /** true if the update is mandatory — app must block navigation */
    val forceUpdate: Boolean,
    /** false if versionCode < minSupportedVersionCode — always block */
    val currentVersionSupported: Boolean,
    val latestVersionCode: Int?,
    val latestVersionName: String?,
    /** Direct APK link or Play Store URL; only present when updateAvailable = true */
    val downloadUrl: String?,
    /** Changelog shown in the update dialog; only present when updateAvailable = true */
    val releaseNotes: String?,
    val channel: String?,
    /** 0–100 gradual rollout percentage; null treated as 100 */
    val rolloutPercentage: Int?,
    val minSupportedVersionCode: Int?
)
