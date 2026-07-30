package com.mna.streaming.network.models

import com.google.gson.annotations.SerializedName

// ── Core anime objects ────────────────────────────────────────────────────────

data class ApiAnimeGenre(
    @SerializedName("_id") val id: String,
    val name: String
)

data class ApiAnimeCastMember(
    val name: String,
    val character: String?,
    val image: String?,
    val order: Int
)

data class ApiAnime(
    @SerializedName("_id") val id: String,
    val title: String,
    val slug: String?,
    val description: String?,
    val posterUrl: String?,
    val bannerUrl: String?,
    val trailerUrl: String?,
    val releaseYear: Int?,
    val totalSeasons: Int?,
    val type: String,
    val status: String,           // "ongoing" | "completed"
    val publishStatus: String,
    val rating: Double,
    val ratingCount: Int,
    val views: Int,
    val genres: List<ApiAnimeGenre>,
    val cast: List<ApiAnimeCastMember>?,
    val externalId: String?,
    val createdAt: String?,
    val updatedAt: String?
)

// ── Browse / list response ────────────────────────────────────────────────────

data class AnimeListResponse(
    val series: List<ApiAnime>,
    val total: Int,
    val page: Int,
    val totalPages: Int
)

// ── Single series detail response ─────────────────────────────────────────────

/** Wraps the single-series response from GET /api/series/{id}. */
data class SeriesDetailResponse(
    val series: ApiAnime
)

// ── Episodes ──────────────────────────────────────────────────────────────────

data class ApiEpisode(
    @SerializedName("_id") val id: String,
    val series: String,
    val season: Int,
    val episodeNumber: Int,
    val title: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class EpisodeDetailResponse(
    val episode: ApiEpisode
)

/**
 * Admin-only episode shape — includes videoUrl and videoType.
 * Returned by GET /api/admin/episodes?seriesId=...
 */
data class ApiAdminEpisode(
    @SerializedName("_id") val id: String,
    val series: String,
    val season: Int,
    val episodeNumber: Int,
    val title: String?,
    val videoUrl: String?,
    val videoType: String?,
    val createdAt: String?,
    val updatedAt: String?
)

/**
 * Public episodes list response — same shape as admin, but [ApiAdminEpisode.videoUrl]
 * and [ApiAdminEpisode.videoType] will be null (server omits them for non-admins).
 * Gson silently ignores missing fields, so we reuse [ApiAdminEpisode] as the element type.
 */
data class EpisodesListResponse(
    val episodes: List<ApiAdminEpisode> = emptyList()
)

data class AdminEpisodesResponse(
    val episodes: List<ApiAdminEpisode>
)

// ── Streaming probe result (sealed) ──────────────────────────────────────────

sealed class AnimeStreamInfo {
    /** Episode streams directly (HLS or MP4). Feed [streamUrl] to ExoPlayer. */
    data class Stream(val streamUrl: String, val contentType: String = "") : AnimeStreamInfo()
    /** Episode is embed type. Load [embedUrl] in a WebView. */
    data class Embed(val embedUrl: String) : AnimeStreamInfo()
}

// ── Embed endpoint response ───────────────────────────────────────────────────

data class AnimeEmbedResponse(val url: String)

// ── Watchlist (reuses Movie models, just different targetType) ────────────────

data class AnimeWatchlistToggleRequest(
    val targetType: String = "Series",
    val targetId: String
)

// ── Views (reuses Movie models pattern) ──────────────────────────────────────

data class AnimeViewsRequest(
    val targetType: String,   // "Series" or "Episode"
    val targetId: String
)

// ── Watch history ─────────────────────────────────────────────────────────────

data class AnimeWatchHistoryRequest(
    val targetType: String = "Episode",
    val targetId: String,
    val progressSeconds: Int
)

// ── Reviews (reuse shared types from MovieModels for ApiReview / ReviewUser) ──

data class AnimeReviewRequest(
    val targetType: String = "Series",
    val targetId: String,
    val rating: Int,
    val comment: String? = null
)

// ── Search result filtering ───────────────────────────────────────────────────

/**
 * The search endpoint returns both movies and series in one response.
 * We only declare the series field — Gson silently ignores the movies array,
 * which avoids any shape mismatch between movie and anime JSON objects.
 */
data class AnimeSearchResponse(
    val series: List<ApiAnime> = emptyList()
)
