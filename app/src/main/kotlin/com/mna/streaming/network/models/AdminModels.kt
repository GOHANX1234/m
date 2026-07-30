package com.mna.streaming.network.models

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────────────────────
// Shared Admin types
// ─────────────────────────────────────────────────────────────────────────────

data class AdminCastMember(
    val name: String,
    val character: String = "",
    val image: String = "",
    val order: Int = 0
)

// ─────────────────────────────────────────────────────────────────────────────
// Movies
// ─────────────────────────────────────────────────────────────────────────────

data class AdminMovie(
    @SerializedName("_id") val id: String,
    val title: String,
    val slug: String?,
    val description: String?,
    val posterUrl: String?,
    val bannerUrl: String?,
    val trailerUrl: String?,
    val videoUrl: String?,
    val videoType: String?,
    val externalId: String?,
    val duration: Int?,
    val releaseYear: Int?,
    val genres: List<ApiGenre>,
    val cast: List<ApiCastMember>?,
    val rating: Double?,
    val ratingCount: Int?,
    val views: Int?,
    val status: String,      // "published" | "draft"
    val createdAt: String?,
    val updatedAt: String?
)

data class AdminMovieResponse(val movie: AdminMovie)

data class AdminMoviesListResponse(
    val movies: List<AdminMovie>,
    val total: Int,
    val page: Int,
    val totalPages: Int
)

data class CreateMovieRequest(
    val title: String,
    val description: String?,
    val posterUrl: String?,
    val bannerUrl: String?,
    val trailerUrl: String?,
    val videoUrl: String?,
    val videoType: String?,
    val externalId: String?,
    val duration: Int?,
    val releaseYear: Int?,
    val genres: List<String>?,     // Genre ObjectIds
    val cast: List<AdminCastMember>?,
    val status: String?,
    val rating: Double?
)

data class UpdateMovieRequest(
    val title: String?,
    val description: String?,
    val posterUrl: String?,
    val bannerUrl: String?,
    val trailerUrl: String?,
    val videoUrl: String?,
    val videoType: String?,
    val externalId: String?,
    val duration: Int?,
    val releaseYear: Int?,
    val genres: List<String>?,
    val cast: List<AdminCastMember>?,
    val status: String?,
    val rating: Double?
)

// ─────────────────────────────────────────────────────────────────────────────
// Series / Anime
// ─────────────────────────────────────────────────────────────────────────────

data class AdminSeries(
    @SerializedName("_id") val id: String,
    val title: String,
    val slug: String?,
    val description: String?,
    val posterUrl: String?,
    val bannerUrl: String?,
    val trailerUrl: String?,
    val externalId: String?,
    val totalSeasons: Int?,
    val releaseYear: Int?,
    val genres: List<ApiGenre>,
    val cast: List<ApiCastMember>?,
    val status: String?,          // "ongoing" | "completed"
    val type: String?,            // "anime" | "series"
    val publishStatus: String?,   // "published" | "draft"
    val rating: Double?,
    val ratingCount: Int?,
    val views: Int?,
    val createdAt: String?,
    val updatedAt: String?
)

data class AdminSeriesResponse(val series: AdminSeries)

data class AdminSeriesListResponse(
    val series: List<AdminSeries>,
    val total: Int,
    val page: Int,
    val totalPages: Int
)

data class CreateSeriesRequest(
    val title: String,
    val description: String?,
    val posterUrl: String?,
    val bannerUrl: String?,
    val trailerUrl: String?,
    val externalId: String?,
    val totalSeasons: Int?,
    val releaseYear: Int?,
    val genres: List<String>?,
    val cast: List<AdminCastMember>?,
    val status: String?,
    val type: String?,
    val publishStatus: String?,
    val rating: Double?
)

data class UpdateSeriesRequest(
    val title: String?,
    val description: String?,
    val posterUrl: String?,
    val bannerUrl: String?,
    val trailerUrl: String?,
    val externalId: String?,
    val totalSeasons: Int?,
    val releaseYear: Int?,
    val genres: List<String>?,
    val cast: List<AdminCastMember>?,
    val status: String?,
    val type: String?,
    val publishStatus: String?,
    val rating: Double?
)

// ─────────────────────────────────────────────────────────────────────────────
// Episodes
// ─────────────────────────────────────────────────────────────────────────────

data class AdminEpisodeResponse(val episode: ApiAdminEpisode)

/** List wrapper for the admin episodes endpoint — kept here so R8 preserves it for Gson. */
data class AdminEpisodesListResponse(val episodes: List<ApiAdminEpisode>)

data class CreateEpisodeRequest(
    val series: String,
    val season: Int?,
    val episodeNumber: Int,
    val title: String?,
    val videoUrl: String?,
    val videoType: String?
)

data class UpdateEpisodeRequest(
    val season: Int?,
    val episodeNumber: Int?,
    val title: String?,
    val videoUrl: String?,
    val videoType: String?
)

// ─────────────────────────────────────────────────────────────────────────────
// Genres
// ─────────────────────────────────────────────────────────────────────────────

data class AdminGenresResponse(val genres: List<ApiGenre>)

data class CreateGenreRequest(val name: String)

data class AdminGenreResponse(val genre: ApiGenre)

// ─────────────────────────────────────────────────────────────────────────────
// Users
// ─────────────────────────────────────────────────────────────────────────────

data class AdminUser(
    @SerializedName("_id") val id: String,
    val nickname: String,
    val email: String,
    val image: String?,
    val role: String,
    val loginAttempts: Int?,
    val lockedUntil: String?,
    val createdAt: String?
)

data class AdminUsersResponse(
    val users: List<AdminUser>,
    val total: Int,
    val page: Int,
    val totalPages: Int
)

data class AdminUserResponse(val user: AdminUser)

data class UpdateUserRoleRequest(val role: String)

// ─────────────────────────────────────────────────────────────────────────────
// Content Requests (admin view)
// ─────────────────────────────────────────────────────────────────────────────

data class AdminRequestUser(
    @SerializedName("_id") val id: String,
    val nickname: String,
    val email: String
)

data class AdminContentRequest(
    @SerializedName("_id") val id: String,
    val user: AdminRequestUser?,
    val title: String,
    val type: String,
    val note: String?,
    val status: String,
    val adminNote: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class AdminRequestsResponse(val requests: List<AdminContentRequest>)

data class AdminRequestResponse(val request: AdminContentRequest)

data class UpdateRequestBody(
    val status: String,
    val adminNote: String?
)

// ─────────────────────────────────────────────────────────────────────────────
// TMDB Autofill
// ─────────────────────────────────────────────────────────────────────────────

data class TmdbSearchResult(
    val externalId: String,
    val title: String,
    val description: String?,
    val posterUrl: String?,
    val bannerUrl: String?,
    val releaseYear: Int?,
    val rating: Double?,
    val totalSeasons: Int? = null   // present for TV results
)

data class TmdbSearchResponse(val results: List<TmdbSearchResult>)

data class TmdbCastMember(
    val name: String,
    val character: String?,
    val image: String?,
    val order: Int
)

data class TmdbMovieDetailsResponse(
    val duration: Int?,
    val genreNames: List<String>,
    val cast: List<TmdbCastMember>
)

data class TmdbTvDetailsResponse(
    val genreNames: List<String>,
    val cast: List<TmdbCastMember>
)

// ─────────────────────────────────────────────────────────────────────────────
// AniList Autofill  (replaces Jikan — requests go through legacy /api/admin/jikan/* routes
//                    which are now proxied to AniList server-side)
// ─────────────────────────────────────────────────────────────────────────────

data class AniListSearchResult(
    val externalId: String,      // AniList numeric ID (not MAL ID)
    val title: String,
    val description: String?,
    val posterUrl: String?,
    val bannerUrl: String?,
    val releaseYear: Int?,
    val rating: Double?,
    val episodes: Int?,
    val genreNames: List<String>?
)

data class AniListSearchResponse(val results: List<AniListSearchResult>)

data class AniListCastMember(
    val name: String,            // Voice actor name
    val character: String?,      // Character name
    val image: String?,
    val order: Int
)

data class AniListCharactersResponse(val cast: List<AniListCastMember>)

/**
 * Response from GET /api/admin/anilist/anime/{id}/episodes/{episodeNumber}.
 * AniList does not expose per-episode titles — the server always returns title = null.
 */
data class AniListEpisodeTitleResponse(
    val title: String?,
    val note: String?
)

// ─────────────────────────────────────────────────────────────────────────────
// Generic
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// App Version Management
// ─────────────────────────────────────────────────────────────────────────────

data class AdminAppVersion(
    @SerializedName("_id") val id: String,
    val versionName: String,
    val versionCode: Int,
    val platform: String,       // "android" | "ios" | "all"
    val channel: String,        // "stable" | "beta"
    val downloadUrl: String,
    val releaseNotes: String?,
    val forceUpdate: Boolean,
    val minSupportedVersionCode: Int?,
    val rolloutPercentage: Int?,
    val isActive: Boolean,
    val adminNotes: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class AdminAppVersionResponse(val version: AdminAppVersion)

data class AdminAppVersionsListResponse(
    val versions: List<AdminAppVersion>,
    val total: Int,
    val page: Int,
    val totalPages: Int
)

data class CreateAppVersionRequest(
    val versionName: String,
    val versionCode: Int,
    val platform: String,
    val channel: String,
    val downloadUrl: String,
    val releaseNotes: String?,
    val forceUpdate: Boolean?,
    val minSupportedVersionCode: Int?,
    val rolloutPercentage: Int?,
    val isActive: Boolean?,
    val adminNotes: String?
)

data class UpdateAppVersionRequest(
    val versionName: String?,
    val platform: String?,
    val channel: String?,
    val downloadUrl: String?,
    val releaseNotes: String?,
    val forceUpdate: Boolean?,
    val minSupportedVersionCode: Int?,
    val rolloutPercentage: Int?,
    val isActive: Boolean?,
    val adminNotes: String?
)

data class AdminAppVersionDeleteResponse(val ok: Boolean)

// ─────────────────────────────────────────────────────────────────────────────
// Generic
// ─────────────────────────────────────────────────────────────────────────────

data class AdminSuccessResponse(val success: Boolean)

data class AdminErrorResponse(val error: String)
