package com.mna.streaming.network.models

import com.google.gson.annotations.SerializedName

// ── Content request ───────────────────────────────────────────────────────────

/**
 * A single content request submitted by the current user.
 * Status flow: pending → in_progress → fulfilled | rejected
 */
data class ContentRequest(
    @SerializedName("_id") val id: String,
    val user: String,
    val title: String,
    val type: String,           // "movie" | "series" | "anime"
    val note: String? = null,   // optional note from the user
    val status: String,         // "pending" | "in_progress" | "fulfilled" | "rejected"
    val adminNote: String? = null,
    val createdAt: String,
    val updatedAt: String
)

data class RequestsListResponse(
    val requests: List<ContentRequest> = emptyList()
)

data class NewRequestBody(
    val title: String,
    val type: String,
    val note: String? = null
)

data class NewRequestResponse(
    val request: ContentRequest? = null,
    val error: String? = null
)

data class CancelRequestResponse(
    val success: Boolean? = null,
    val error: String? = null
)
