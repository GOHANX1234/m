package com.mna.streaming.network

import com.mna.streaming.network.models.*
import retrofit2.http.*

/**
 * Retrofit interface for all anime-related API endpoints.
 *
 * The stream/embed endpoints are intentionally NOT listed here — they are
 * called via OkHttp directly in AnimeRepository, using paths decoded from
 * the native C security layer so they never appear as literal strings in
 * Kotlin bytecode.
 *
 * The episodes endpoints (public + admin fallback) are also called via OkHttp directly
 * for the same reason — paths decoded from the native C security layer at runtime.
 */
interface AnimeApiService {

    // ── Browse ────────────────────────────────────────────────────────────────

    /**
     * Paginated anime list with optional genre, status, and sort filters.
     * @param type   Always "anime" for this section.
     * @param genre  Genre name filter (e.g. "Action"). Case-insensitive exact match.
     * @param status "ongoing" | "completed" — omit for both.
     * @param sort   "latest" | "views" | "rating"
     * @param page   1-based page number.
     * @param limit  Items per page (max 50).
     */
    @GET("api/anime")
    suspend fun getAnime(
        @Query("type")   type: String  = "anime",
        @Query("genre")  genre: String? = null,
        @Query("status") status: String? = null,
        @Query("sort")   sort: String?  = null,
        @Query("page")   page: Int?     = null,
        @Query("limit")  limit: Int?    = null
    ): AnimeListResponse

    /**
     * Paginated web-series list (type="series" in the backend's Series collection).
     * Returns the same [AnimeListResponse] shape as the anime endpoint.
     */
    @GET("api/series")
    suspend fun getWebSeries(
        @Query("genre")  genre: String? = null,
        @Query("status") status: String? = null,
        @Query("sort")   sort: String?  = null,
        @Query("page")   page: Int?     = null,
        @Query("limit")  limit: Int?    = null
    ): AnimeListResponse

    // ── Single series detail ──────────────────────────────────────────────────

    /**
     * Fetch a single anime or web series by its MongoDB _id.
     * Works for both type="anime" and type="series" — both live in the same
     * Series collection on the backend.
     *
     * Used as a fallback when the detail screen is opened from a notification
     * cold-start (the in-memory cache is empty).
     */
    @GET("api/series/{id}")
    suspend fun getSeriesById(@Path("id") id: String): SeriesDetailResponse

    // ── Episode metadata (public, no videoUrl) ────────────────────────────────

    /**
     * Public episode metadata — returns everything except videoUrl / videoType.
     * Use the stream endpoint for actual playback.
     */
    @GET("api/episodes/{id}")
    suspend fun getEpisodeById(@Path("id") id: String): EpisodeDetailResponse

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * Full-text search across movies and series.
     * Filter the returned series list client-side by type == "anime".
     */
    @GET("api/search")
    suspend fun search(@Query("q") q: String): AnimeSearchResponse

    // ── Watchlist ─────────────────────────────────────────────────────────────

    @GET("api/watchlist")
    suspend fun getWatchlistStatus(
        @Query("targetType") targetType: String,
        @Query("targetId")   targetId: String
    ): WatchlistStatusResponse

    @POST("api/watchlist/toggle")
    suspend fun toggleWatchlist(@Body request: AnimeWatchlistToggleRequest): WatchlistStatusResponse

    // ── Views ─────────────────────────────────────────────────────────────────

    /** Record a unique view for a series (on detail screen open) or episode (on play start). */
    @POST("api/views")
    suspend fun trackView(@Body request: AnimeViewsRequest): ViewsResponse

    // ── Watch history ─────────────────────────────────────────────────────────

    /** Save episode playback progress. Call every 15–30 s and on player exit. */
    @POST("api/watch-history")
    suspend fun saveWatchHistory(@Body request: AnimeWatchHistoryRequest): WatchHistoryResponse

    // ── Reviews ───────────────────────────────────────────────────────────────

    @GET("api/reviews")
    suspend fun getReviews(
        @Query("targetType") targetType: String,
        @Query("targetId")   targetId: String
    ): ReviewsResponse

    /** Create or update (upsert) the current user's review. One review per user per anime. */
    @POST("api/reviews")
    suspend fun submitReview(@Body request: AnimeReviewRequest): ReviewSubmitResponse

    @DELETE("api/reviews/{id}")
    suspend fun deleteReview(@Path("id") id: String): GenericSuccessResponse
}
