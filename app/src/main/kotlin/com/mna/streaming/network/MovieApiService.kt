package com.mna.streaming.network

import com.mna.streaming.network.models.*
import retrofit2.http.*

/**
 * Retrofit interface for all movie-related API endpoints.
 *
 * The stream/embed endpoint is intentionally NOT listed here — it is called
 * via OkHttp directly in MovieRepository, using paths decoded from the native
 * C security layer so they never appear as literal strings in Kotlin bytecode.
 */
interface MovieApiService {

    // ── Discovery ─────────────────────────────────────────────────────────────

    /**
     * Paginated movie list with optional filtering and sorting.
     * @param sort  "latest" | "views" | "rating" | "year"
     * @param limit 1–50 (hard-capped at 50 by server)
     */
    @GET("api/movies")
    suspend fun getMovies(
        @Query("genre") genre: String?  = null,
        @Query("year")  year: Int?      = null,
        @Query("sort")  sort: String?   = null,
        @Query("page")  page: Int?      = null,
        @Query("limit") limit: Int?     = null
    ): MoviesResponse

    /** Full metadata for a single movie by its MongoDB _id. */
    @GET("api/movies/{id}")
    suspend fun getMovieById(@Path("id") id: String): MovieDetailResponse

    // ── Search ────────────────────────────────────────────────────────────────

    /** Keyword search on title + description. Min 2 chars. */
    @GET("api/search")
    suspend fun search(@Query("q") q: String): SearchResponse

    // ── User profile ──────────────────────────────────────────────────────────

    /** Full user profile + server-side stats (watchedCount, watchlistCount, joinedAt). */
    @GET("api/me")
    suspend fun getMe(): MeResponse

    // ── Watchlist ─────────────────────────────────────────────────────────────

    /** Full watchlist with populated content, newest-saved first. */
    @GET("api/watchlist/all")
    suspend fun getWatchlistAll(
        @Query("page")  page: Int  = 1,
        @Query("limit") limit: Int = 50
    ): WatchlistAllResponse

    @GET("api/watchlist")
    suspend fun getWatchlistStatus(
        @Query("targetType") targetType: String,
        @Query("targetId")   targetId: String
    ): WatchlistStatusResponse

    @POST("api/watchlist/toggle")
    suspend fun toggleWatchlist(@Body request: WatchlistToggleRequest): WatchlistStatusResponse

    // ── Views ─────────────────────────────────────────────────────────────────

    /** Record a unique view. Call once when playback starts. */
    @POST("api/views")
    suspend fun trackView(@Body request: ViewsRequest): ViewsResponse

    // ── Watch history ─────────────────────────────────────────────────────────

    /** Paginated watch history, newest-watched first. */
    @GET("api/watch-history")
    suspend fun getWatchHistoryList(
        @Query("page")  page: Int  = 1,
        @Query("limit") limit: Int = 30
    ): WatchHistoryListResponse

    /** Save playback progress. Call every 15–30 s and on player close. */
    @POST("api/watch-history")
    suspend fun saveWatchHistory(@Body request: WatchHistoryRequest): WatchHistoryResponse

    // ── Reviews ───────────────────────────────────────────────────────────────

    @GET("api/reviews")
    suspend fun getReviews(
        @Query("targetType") targetType: String,
        @Query("targetId")   targetId: String
    ): ReviewsResponse

    /** Create or update (upsert) the current user's review for a movie. */
    @POST("api/reviews")
    suspend fun submitReview(@Body request: ReviewRequest): ReviewSubmitResponse

    @DELETE("api/reviews/{id}")
    suspend fun deleteReview(@Path("id") id: String): GenericSuccessResponse

    // ── Content requests ──────────────────────────────────────────────────────

    /** List all content requests submitted by the current user (newest first). */
    @GET("api/requests")
    suspend fun getRequests(): RequestsListResponse

    /** Submit a new content request. Returns 201 on success. */
    @POST("api/requests")
    suspend fun submitRequest(@Body body: NewRequestBody): NewRequestResponse

    /** Cancel a pending request by its ID. Only works while status is "pending". */
    @DELETE("api/requests/{id}")
    suspend fun cancelRequest(@Path("id") id: String): CancelRequestResponse
}
