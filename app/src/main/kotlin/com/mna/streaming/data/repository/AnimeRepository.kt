package com.mna.streaming.data.repository

import com.google.gson.Gson
import com.mna.streaming.network.ApiClient
import com.mna.streaming.network.models.*
import com.mna.streaming.security.NativeApiSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Single source of truth for all anime-related data operations.
 *
 * Most calls go through the Retrofit [AnimeApiService].
 *
 * Stream/embed and admin-episodes endpoints are called via OkHttp directly,
 * with URLs built at runtime from paths decoded by the C security layer.
 * This means "/api/stream/episode/", "/embed", and "/api/admin/episodes"
 * never appear as string literals anywhere in the Kotlin/DEX code.
 *
 * Loaded anime are cached in-memory by ID so that the detail screen can
 * look them up without a second network round-trip (no public GET /api/anime/:id exists).
 */
class AnimeRepository(private val apiClient: ApiClient) {

    private val service = apiClient.animeApiService
    private val okHttp  = apiClient.okHttpClient
    private val baseUrl = apiClient.baseUrl.trimEnd('/')
    private val gson    = Gson()

    /** In-memory anime cache: id → ApiAnime. Populated by browse / search calls. */
    private val animeCache = mutableMapOf<String, ApiAnime>()

    // ── Browse ────────────────────────────────────────────────────────────────

    /**
     * Fetch a page of anime with optional filters.
     * Results are stored in the in-memory cache for later detail lookups.
     */
    suspend fun getAnime(
        genre: String? = null,
        status: String? = null,
        sort: String = "latest",
        page: Int = 1,
        limit: Int = 24
    ): AnimeListResponse = withContext(Dispatchers.IO) {
        val response = service.getAnime(
            type   = "anime",
            genre  = genre,
            status = status,
            sort   = sort,
            page   = page,
            limit  = limit
        )
        // Cache all returned anime
        response.series.forEach { animeCache[it.id] = it }
        response
    }

    /**
     * Fetch a page of web series (type="series") from /api/series.
     * Results are also stored in the in-memory cache.
     */
    suspend fun getWebSeries(
        sort: String = "latest",
        page: Int = 1,
        limit: Int = 20
    ): AnimeListResponse = withContext(Dispatchers.IO) {
        val response = service.getWebSeries(
            sort  = sort,
            page  = page,
            limit = limit
        )
        response.series.forEach { animeCache[it.id] = it }
        response
    }

    /** Look up a cached anime by ID. Returns null if not yet loaded. */
    fun getCachedAnime(id: String): ApiAnime? = animeCache[id]

    /** Store an anime in the cache (useful when navigating with a pre-loaded object). */
    fun cacheAnime(anime: ApiAnime) { animeCache[anime.id] = anime }

    /**
     * Fetch a single series (anime or web series) by its MongoDB _id from the
     * network. The result is stored in [animeCache] so subsequent lookups are
     * served from memory.
     *
     * Used as a cold-start fallback when [getCachedAnime] returns null — e.g.
     * the detail screen was opened directly from a notification tap and the
     * browse cache has never been populated.
     *
     * @throws Exception on network failure or a non-2xx response.
     */
    suspend fun getSeriesById(id: String): ApiAnime = withContext(Dispatchers.IO) {
        val anime = service.getSeriesById(id).series
        animeCache[anime.id] = anime
        anime
    }

    // ── Episodes ──────────────────────────────────────────────────────────────

    /**
     * Fetch the episode list for a series.
     *
     * Strategy (both paths decoded from the C security layer at runtime):
     *  1. Try the public endpoint  GET /api/episodes?seriesId=...  — works for all users.
     *  2. If that returns a non-2xx response, fall back to the admin endpoint
     *     GET /api/admin/episodes?seriesId=... — same list plus videoUrl/videoType
     *     for admin accounts.
     *
     * Returns an empty list only if both endpoints fail, rather than throwing.
     */
    suspend fun getEpisodesForSeries(seriesId: String): List<ApiAdminEpisode> =
        withContext(Dispatchers.IO) {
            // ── Step 1: public endpoint ───────────────────────────────────────
            val publicPath = NativeApiSecurity.getEpisodesPath()
            val publicUrl  = "$baseUrl$publicPath?seriesId=$seriesId"
            val publicReq  = Request.Builder().url(publicUrl).build()
            val publicResp = okHttp.newCall(publicReq).execute()
            val publicBody = publicResp.body?.string().orEmpty()

            if (publicResp.isSuccessful) {
                val episodes = gson.fromJson(publicBody, EpisodesListResponse::class.java)
                    ?.episodes
                // Only trust the result if we actually got episodes back
                if (!episodes.isNullOrEmpty()) return@withContext episodes
            }

            // ── Step 2: admin fallback ────────────────────────────────────────
            val adminPath = NativeApiSecurity.getAdminEpisodesPath()
            val adminUrl  = "$baseUrl$adminPath?seriesId=$seriesId"
            val adminReq  = Request.Builder().url(adminUrl).build()
            val adminResp = okHttp.newCall(adminReq).execute()
            val adminBody = adminResp.body?.string().orEmpty()

            if (!adminResp.isSuccessful) return@withContext emptyList()
            gson.fromJson(adminBody, AdminEpisodesResponse::class.java)?.episodes
                ?: emptyList()
        }

    // ── Streaming — two-step probe via C security layer ───────────────────────

    /**
     * Probes the episode stream endpoint and returns the appropriate stream info.
     *
     * Flow (per API docs §4.3):
     *  1. GET /api/stream/episode/:id — path decoded from native C layer.
     *     • 2xx → direct / HLS stream — return [AnimeStreamInfo.Stream] with the URL.
     *     • 400 → embed type — fall through to step 2.
     *  2. GET /api/stream/episode/:id/embed
     *     → return [AnimeStreamInfo.Embed] with the raw embed URL.
     *
     * The paths "/api/stream/episode/" and "/embed" never appear as string literals
     * anywhere in the Kotlin code — they are assembled entirely from native-decoded strings.
     *
     * @throws Exception on auth errors (401), rate-limit (429), or unexpected failures.
     */
    suspend fun probeEpisodeStream(episodeId: String): AnimeStreamInfo =
        withContext(Dispatchers.IO) {
            val streamBase = NativeApiSecurity.getAnimeStreamEpisodePath()
            val embedSuffix = NativeApiSecurity.getEmbedSuffix()
            val streamUrl = "$baseUrl$streamBase$episodeId"

            // Step 1 — probe the stream endpoint
            val probeReq = Request.Builder()
                .url(streamUrl)
                .addHeader("Range", "bytes=0-0")
                .build()
            val probeResp = okHttp.newCall(probeReq).execute()
            val probeCode = probeResp.code
            val contentType = probeResp.header("Content-Type") ?: ""
            probeResp.close()

            when {
                probeCode == 401 -> throw Exception("Login required to stream")
                probeCode == 429 -> throw Exception("Too many requests — please wait a moment")
                probeCode == 400 -> {
                    // Step 2 — embed type: fetch the embed URL
                    val embedUrl = "$baseUrl$streamBase$episodeId$embedSuffix"
                    val embedReq = Request.Builder().url(embedUrl).build()
                    val embedResp = okHttp.newCall(embedReq).execute()
                    val embedBody = embedResp.body?.string()
                        ?: throw Exception("Empty embed response")
                    if (!embedResp.isSuccessful) {
                        val err = runCatching {
                            gson.fromJson(embedBody, ErrorBody::class.java).error
                        }.getOrDefault("Embed unavailable (${embedResp.code})")
                        throw Exception(err)
                    }
                    val url = gson.fromJson(embedBody, AnimeEmbedResponse::class.java).url
                        ?: throw Exception("No embed URL in response")
                    AnimeStreamInfo.Embed(url)
                }
                probeCode in 200..299 || probeCode == 206 -> {
                    AnimeStreamInfo.Stream(streamUrl, contentType)
                }
                else -> throw Exception("Stream error ($probeCode)")
            }
        }

    // ── Watchlist ─────────────────────────────────────────────────────────────

    suspend fun getWatchlistStatus(seriesId: String): Boolean = withContext(Dispatchers.IO) {
        service.getWatchlistStatus(targetType = "Series", targetId = seriesId).inWatchlist
    }

    /** Toggle watchlist for an anime series. Returns the new [inWatchlist] value. */
    suspend fun toggleWatchlist(seriesId: String): Boolean = withContext(Dispatchers.IO) {
        service.toggleWatchlist(AnimeWatchlistToggleRequest(targetId = seriesId)).inWatchlist
    }

    // ── View tracking ─────────────────────────────────────────────────────────

    /** Record a series view (call when the detail screen opens). Best-effort. */
    suspend fun trackSeriesView(seriesId: String) = withContext(Dispatchers.IO) {
        runCatching {
            service.trackView(AnimeViewsRequest(targetType = "Series", targetId = seriesId))
        }
    }

    /** Record an episode view (call when playback starts). Best-effort. */
    suspend fun trackEpisodeView(episodeId: String) = withContext(Dispatchers.IO) {
        runCatching {
            service.trackView(AnimeViewsRequest(targetType = "Episode", targetId = episodeId))
        }
    }

    // ── Watch history ─────────────────────────────────────────────────────────

    /** Save episode playback progress. Call every 15–30 s and on player exit. */
    suspend fun saveProgress(episodeId: String, progressSeconds: Int) =
        withContext(Dispatchers.IO) {
            runCatching {
                service.saveWatchHistory(
                    AnimeWatchHistoryRequest(targetId = episodeId, progressSeconds = progressSeconds)
                )
            }
        }

    // ── Reviews ───────────────────────────────────────────────────────────────

    suspend fun getReviews(seriesId: String): List<ApiReview> = withContext(Dispatchers.IO) {
        service.getReviews(targetType = "Series", targetId = seriesId).reviews
    }

    /** Create or update the current user's review (server upserts). */
    suspend fun submitReview(seriesId: String, rating: Int, comment: String?): ApiReview =
        withContext(Dispatchers.IO) {
            service.submitReview(
                AnimeReviewRequest(targetId = seriesId, rating = rating, comment = comment)
            ).review
        }

    suspend fun deleteReview(reviewId: String) = withContext(Dispatchers.IO) {
        service.deleteReview(reviewId)
    }

    /**
     * Return up to 15 anime titles that share the first genre of [genres], excluding
     * [currentAnimeId] itself.  Results are stored in the in-memory cache.
     * Returns an empty list on any error so the caller never has to handle a failure.
     */
    suspend fun getSimilarAnime(currentAnimeId: String, genres: List<String>): List<ApiAnime> =
        withContext(Dispatchers.IO) {
            val genre = genres.firstOrNull() ?: return@withContext emptyList()
            runCatching {
                service.getAnime(type = "anime", genre = genre, sort = "rating", limit = 20)
                    .series
                    .filter { it.id != currentAnimeId }
                    .take(15)
                    .also { results -> results.forEach { animeCache[it.id] = it } }
            }.getOrDefault(emptyList())
        }

    // ── Search ────────────────────────────────────────────────────────────────

    /** Search anime and web series by keyword. Returns both types (type=="anime" and type=="series"). */
    suspend fun search(query: String): List<ApiAnime> = withContext(Dispatchers.IO) {
        if (query.length < 2) return@withContext emptyList()
        val results = service.search(query).series.filter { it.type == "anime" || it.type == "series" }
        results.forEach { animeCache[it.id] = it }
        results
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private data class ErrorBody(val error: String = "")
}
