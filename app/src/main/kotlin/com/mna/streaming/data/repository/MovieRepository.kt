package com.mna.streaming.data.repository

import com.google.gson.Gson
import com.mna.streaming.data.LocalProfileStore
import com.mna.streaming.data.LocalWatchEntry
import com.mna.streaming.data.LocalWatchlistItem
import com.mna.streaming.data.model.Category
import com.mna.streaming.data.model.Movie
import com.mna.streaming.network.ApiClient
import com.mna.streaming.network.models.*
import com.mna.streaming.security.NativeApiSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import retrofit2.HttpException

/**
 * Single source of truth for all movie-related data operations.
 *
 * Most calls go through the Retrofit [MovieApiService].
 *
 * The stream/embed endpoint is an exception: it is called via OkHttp directly,
 * with the URL built at runtime from paths decoded by the C security layer.
 * This means "/api/stream/movie" and "/embed" never appear as string literals
 * anywhere in the Kotlin/DEX code.
 */
class MovieRepository(
    private val apiClient: ApiClient,
    private val localProfileStore: LocalProfileStore
) {

    private val service = apiClient.movieApiService
    private val okHttp  = apiClient.okHttpClient
    private val baseUrl = apiClient.baseUrl.trimEnd('/')
    private val gson    = Gson()

    // ── Discovery ─────────────────────────────────────────────────────────────

    /** Fetch movies sorted by newest-added. Used for "New Releases" section. */
    suspend fun getLatest(limit: Int = 20): List<Movie> = withContext(Dispatchers.IO) {
        service.getMovies(sort = "latest", limit = limit).movies.map { it.toMovie() }
    }

    /** Fetch movies sorted by view count. Used for "Most Popular" section. */
    suspend fun getMostPopular(limit: Int = 20): List<Movie> = withContext(Dispatchers.IO) {
        service.getMovies(sort = "views", limit = limit).movies.map { it.toMovie() }
    }

    /** Fetch movies sorted by rating. Used for "Top Rated" section. */
    suspend fun getTopRated(limit: Int = 20): List<Movie> = withContext(Dispatchers.IO) {
        service.getMovies(sort = "rating", limit = limit).movies.map { it.toMovie() }
    }

    /**
     * Build the home screen category list and the hero-banner rotation list.
     *
     * Returns a [Pair] where:
     *  - first  = up to 6 movies shown in the auto-rotating hero banner
     *  - second = category rows (New Releases, Most Popular, Top Rated)
     */
    suspend fun getHomeCategories(): Pair<List<Movie>, List<Category>> =
        withContext(Dispatchers.IO) {
            val latest   = service.getMovies(sort = "latest",  limit = 20).movies
            val popular  = service.getMovies(sort = "views",   limit = 20).movies
            val topRated = service.getMovies(sort = "rating",  limit = 20).movies

            // First 6 newest movies rotate through the hero banner
            val featured = latest.take(6).map { it.toMovie() }

            val categories = listOf(
                Category("new",     "New Releases", latest.map   { it.toMovie() }),
                Category("popular", "Most Popular", popular.map  { it.toMovie() }),
                Category("rated",   "Top Rated",    topRated.map { it.toMovie() })
            ).filter { it.movies.isNotEmpty() }

            Pair(featured, categories)
        }

    // ── Movie detail ──────────────────────────────────────────────────────────

    /** Full metadata for a single movie including cast. */
    suspend fun getMovieById(id: String): Movie = withContext(Dispatchers.IO) {
        service.getMovieById(id).movie.toMovie()
    }

    /** Full raw API movie (includes cast list for the detail screen). */
    suspend fun getApiMovieById(id: String): ApiMovie = withContext(Dispatchers.IO) {
        service.getMovieById(id).movie
    }

    // ── Streaming — via C security layer ─────────────────────────────────────

    /**
     * Returns the third-party embed URL for a movie.
     *
     * The request URL is constructed at runtime from strings decoded in native
     * memory, so the path "/api/stream/movie/{id}/embed" never appears as a
     * literal anywhere in the Kotlin code or DEX file.
     *
     * @throws Exception if the movie is not an embed type or the network fails.
     */
    suspend fun getEmbedUrl(movieId: String): String = withContext(Dispatchers.IO) {
        val url = "$baseUrl${NativeApiSecurity.getStreamPath()}/$movieId${NativeApiSecurity.getEmbedSuffix()}"
        val request = Request.Builder().url(url).build()
        val response = okHttp.newCall(request).execute()
        val body = response.body?.string()
            ?: throw Exception("Empty response from stream endpoint")
        if (!response.isSuccessful) {
            val errMsg = runCatching {
                gson.fromJson(body, ErrorBody::class.java).error
            }.getOrDefault("Stream unavailable (${response.code})")
            throw Exception(errMsg)
        }
        gson.fromJson(body, EmbedResponse::class.java).url
            ?: throw Exception("No embed URL in response")
    }

    // ── Watchlist ─────────────────────────────────────────────────────────────

    suspend fun getWatchlistStatus(movieId: String): Boolean = withContext(Dispatchers.IO) {
        service.getWatchlistStatus(targetType = "Movie", targetId = movieId).inWatchlist
    }

    /**
     * Toggle saved state on the server. Returns the new [inWatchlist] value.
     *
     * @param movie Full [Movie] object — used to update the local watchlist store
     *              so the Profile → Watchlist tab stays in sync without a server
     *              list endpoint.
     */
    suspend fun toggleWatchlist(movie: Movie): Boolean = withContext(Dispatchers.IO) {
        val newState = service.toggleWatchlist(WatchlistToggleRequest(targetId = movie.id)).inWatchlist
        // Mirror the change in local storage
        if (newState) {
            localProfileStore.addWatchlistItem(
                LocalWatchlistItem(
                    movieId     = movie.id,
                    title       = movie.title,
                    posterUrl   = movie.posterUrl,
                    releaseYear = movie.year,
                    rating      = movie.rating,
                    addedAt     = System.currentTimeMillis()
                )
            )
        } else {
            localProfileStore.removeWatchlistItem(movie.id)
        }
        newState
    }

    /**
     * Legacy overload kept for any call-site that only has a movieId.
     * Does NOT update local storage (no movie metadata available).
     */
    suspend fun toggleWatchlist(movieId: String): Boolean = withContext(Dispatchers.IO) {
        service.toggleWatchlist(WatchlistToggleRequest(targetId = movieId)).inWatchlist
    }

    // ── View tracking ─────────────────────────────────────────────────────────

    /**
     * Record a unique view for this movie. The server deduplicates per user,
     * so calling this multiple times is safe.
     */
    suspend fun trackView(movieId: String) = withContext(Dispatchers.IO) {
        runCatching {
            service.trackView(ViewsRequest(targetId = movieId))
        }
        // Best-effort — swallow any error; don't break playback flow.
    }

    // ── Watch history ─────────────────────────────────────────────────────────

    /** Save playback position. Call every 15–30 s during playback. */
    suspend fun saveProgress(movieId: String, progressSeconds: Int) =
        withContext(Dispatchers.IO) {
            runCatching {
                service.saveWatchHistory(
                    WatchHistoryRequest(targetId = movieId, progressSeconds = progressSeconds)
                )
            }
        }

    /**
     * Record that the user started watching [movie] locally.
     * Upserts an entry in the on-device watch-history store so the
     * Profile → Watch History tab always reflects recent activity,
     * even though there is no server-side GET /api/watch-history.
     */
    suspend fun saveLocalWatchHistory(movie: Movie) = withContext(Dispatchers.IO) {
        runCatching {
            localProfileStore.upsertWatchEntry(
                LocalWatchEntry(
                    movieId   = movie.id,
                    title     = movie.title,
                    posterUrl = movie.posterUrl,
                    targetType = "Movie",
                    updatedAt  = System.currentTimeMillis()
                )
            )
        }
    }

    /** Read the locally stored watch-history list (newest first). */
    suspend fun getLocalWatchHistory(): List<LocalWatchEntry> =
        withContext(Dispatchers.IO) { localProfileStore.getWatchHistory() }

    /** Read the locally stored watchlist. */
    suspend fun getLocalWatchlist(): List<LocalWatchlistItem> =
        withContext(Dispatchers.IO) { localProfileStore.getWatchlist() }

    // ── Profile — server-side reads (new endpoints) ───────────────────────────

    /** Fetch full user profile + accurate server-side stats from /api/me. */
    suspend fun getMe() = withContext(Dispatchers.IO) {
        service.getMe()
    }

    /**
     * Fetch watch history from the server (/api/watch-history).
     * Maps each entry to [LocalWatchEntry] so the Profile screen reuses the
     * same display logic regardless of data source.
     */
    suspend fun getServerWatchHistory(): List<LocalWatchEntry> = withContext(Dispatchers.IO) {
        val response = service.getWatchHistoryList(page = 1, limit = 30)
        response.history.mapNotNull { entry ->
            val content = entry.content ?: return@mapNotNull null
            when (entry.targetType) {
                "Movie" -> LocalWatchEntry(
                    movieId    = content.id,
                    title      = content.title ?: "Unknown",
                    posterUrl  = content.posterUrl ?: "",
                    targetType = "Movie",
                    updatedAt  = parseIso(entry.updatedAt)
                )
                "Episode" -> {
                    val seriesTitle = content.seriesInfo?.title ?: "Unknown Series"
                    val label = buildString {
                        if ((content.season ?: 0) > 0) append("S${content.season} ")
                        if ((content.episodeNumber ?: 0) > 0) append("E${content.episodeNumber} · ")
                        append(seriesTitle)
                    }
                    LocalWatchEntry(
                        movieId    = content.id,
                        title      = label,
                        posterUrl  = content.seriesInfo?.posterUrl ?: "",
                        targetType = "Episode",
                        updatedAt  = parseIso(entry.updatedAt)
                    )
                }
                else -> null
            }
        }
    }

    /**
     * Fetch the full watchlist from the server (/api/watchlist/all).
     * Maps each entry to [LocalWatchlistItem] for reuse in the Profile screen.
     */
    suspend fun getServerWatchlist(): List<LocalWatchlistItem> = withContext(Dispatchers.IO) {
        val response = service.getWatchlistAll(page = 1, limit = 50)
        response.watchlist.mapNotNull { entry ->
            val content = entry.content ?: return@mapNotNull null
            LocalWatchlistItem(
                movieId     = content.id,
                title       = content.title,
                posterUrl   = content.posterUrl ?: "",
                releaseYear = content.releaseYear ?: 0,
                rating      = content.rating ?: 0.0,
                addedAt     = parseIso(entry.savedAt)
            )
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /** Parse an ISO-8601 timestamp string to epoch milliseconds. */
    private fun parseIso(iso: String): Long = try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .also { it.timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .parse(iso)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
        try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                .also { it.timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .parse(iso)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) { System.currentTimeMillis() }
    }

    // ── Reviews ───────────────────────────────────────────────────────────────

    suspend fun getReviews(movieId: String): List<ApiReview> = withContext(Dispatchers.IO) {
        service.getReviews(targetType = "Movie", targetId = movieId).reviews
    }

    /**
     * Create or update the current user's review (server upserts).
     * Returns the saved [ApiReview].
     */
    suspend fun submitReview(
        movieId: String,
        rating: Int,
        comment: String?
    ): ApiReview = withContext(Dispatchers.IO) {
        service.submitReview(
            ReviewRequest(targetId = movieId, rating = rating, comment = comment)
        ).review
    }

    suspend fun deleteReview(reviewId: String) = withContext(Dispatchers.IO) {
        service.deleteReview(reviewId)
    }

    // ── Content requests ──────────────────────────────────────────────────────

    /** Returns all requests the current user has ever submitted, newest first. */
    suspend fun getRequests() = withContext(Dispatchers.IO) {
        service.getRequests().requests
    }

    /**
     * Submit a new content request.
     * @throws Exception when the server returns a validation or auth error.
     */
    suspend fun submitRequest(
        title: String,
        type: String,
        note: String?
    ) = withContext(Dispatchers.IO) {
        try {
            val response = service.submitRequest(NewRequestBody(title, type, note))
            response.request ?: throw Exception(response.error ?: "Failed to submit request")
        } catch (e: HttpException) {
            throw Exception(extractHttpError(e))
        }
    }

    /** Cancel a pending request. Throws if the request is not pending or unauthorised. */
    suspend fun cancelRequest(id: String) = withContext(Dispatchers.IO) {
        try {
            service.cancelRequest(id)
        } catch (e: HttpException) {
            throw Exception(extractHttpError(e))
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    suspend fun search(query: String): List<Movie> = withContext(Dispatchers.IO) {
        if (query.length < 2) return@withContext emptyList()
        service.search(query).movies.map { it.toMovie() }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private data class ErrorBody(val error: String = "")

    /**
     * Read the JSON error body from a non-2xx Retrofit response.
     *
     * Retrofit throws [HttpException] on 4xx/5xx — the body is NOT parsed
     * into the declared return type.  This helper reads the raw error body
     * string and extracts the `"error"` field so the real server message
     * reaches the UI instead of the unhelpful "HTTP 400" status string.
     */
    private fun extractHttpError(e: HttpException): String {
        return try {
            val raw = e.response()?.errorBody()?.string().orEmpty()
            if (raw.isBlank()) return "HTTP ${e.code()}"
            gson.fromJson(raw, ErrorBody::class.java).error
                .ifBlank { "HTTP ${e.code()}" }
        } catch (_: Exception) {
            "HTTP ${e.code()}"
        }
    }
}
