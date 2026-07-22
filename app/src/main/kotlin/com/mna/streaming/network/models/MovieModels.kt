package com.mna.streaming.network.models

import com.google.gson.annotations.SerializedName
import com.mna.streaming.data.model.Movie

// ── Raw API shapes (matches the JSON exactly) ────────────────────────────────

data class ApiGenre(
    @SerializedName("_id") val id: String,
    val name: String
)

data class ApiCastMember(
    val name: String,
    val character: String,
    val image: String?,
    val order: Int
)

data class ApiMovie(
    @SerializedName("_id") val id: String,
    val title: String,
    val slug: String?,
    val description: String,
    val posterUrl: String,
    val bannerUrl: String?,
    val trailerUrl: String?,
    val duration: Int,          // seconds
    val releaseYear: Int,
    val genres: List<ApiGenre>,
    val cast: List<ApiCastMember>?,
    val rating: Double,
    val ratingCount: Int,
    val views: Int,
    val status: String
) {
    /** Map to the clean UI-side [Movie] model. */
    fun toMovie() = Movie(
        id             = id,
        title          = title,
        description    = description,
        genres         = genres.map { it.name },
        year           = releaseYear,
        rating         = rating,
        ratingCount    = ratingCount,
        durationSeconds = duration,
        posterUrl      = posterUrl.takeIf { it.isNotBlank() } ?: "",
        backdropUrl    = bannerUrl?.takeIf { it.isNotBlank() } ?: posterUrl,
        trailerUrl     = trailerUrl?.takeIf { it.isNotBlank() },
        views          = views
    )
}

// ── List/pagination ───────────────────────────────────────────────────────────

data class MoviesResponse(
    val movies: List<ApiMovie>,
    val total: Int,
    val page: Int,
    val totalPages: Int
)

// ── Detail ────────────────────────────────────────────────────────────────────

data class MovieDetailResponse(
    val movie: ApiMovie
)

// ── Search ────────────────────────────────────────────────────────────────────

data class SearchResponse(
    val movies: List<ApiMovie>
)

// ── Streaming ─────────────────────────────────────────────────────────────────

data class EmbedResponse(val url: String)

// ── Watchlist ─────────────────────────────────────────────────────────────────

data class WatchlistStatusResponse(val inWatchlist: Boolean)

data class WatchlistToggleRequest(
    val targetType: String = "Movie",
    val targetId: String
)

// ── Views ─────────────────────────────────────────────────────────────────────

data class ViewsRequest(
    val targetType: String = "Movie",
    val targetId: String
)

data class ViewsResponse(
    val ok: Boolean,
    val counted: Boolean? = null
)

// ── Watch history ─────────────────────────────────────────────────────────────

data class WatchHistoryRequest(
    val targetType: String = "Movie",
    val targetId: String,
    val progressSeconds: Int
)

data class WatchHistoryResponse(val ok: Boolean)

// ── Reviews ───────────────────────────────────────────────────────────────────

data class ReviewRequest(
    val targetType: String = "Movie",
    val targetId: String,
    val rating: Int,
    val comment: String? = null
)

data class ReviewUser(
    @SerializedName("_id") val id: String,
    val nickname: String,
    val image: String?
)

data class ApiReview(
    @SerializedName("_id") val id: String,
    val user: ReviewUser,
    val targetType: String,
    val targetId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String,
    val updatedAt: String
)

data class ReviewSubmitResponse(val review: ApiReview)

data class ReviewsResponse(val reviews: List<ApiReview>)

data class GenericSuccessResponse(val success: Boolean)
