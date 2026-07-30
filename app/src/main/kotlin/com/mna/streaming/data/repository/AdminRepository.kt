package com.mna.streaming.data.repository

import com.google.gson.Gson
import com.mna.streaming.network.ApiClient
import com.mna.streaming.network.models.*
import com.mna.streaming.security.NativeApiSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * All admin API operations.
 *
 * Every endpoint path is decoded at init time from the native C security layer
 * (XOR-obfuscated in security.c), so no admin API path ever appears as a plain
 * string in Kotlin bytecode or the DEX.
 *
 * All calls use OkHttp directly — same pattern as streaming / anime episode paths.
 */
class AdminRepository(private val apiClient: ApiClient) {

    private val okHttp  = apiClient.okHttpClient
    private val base    = apiClient.baseUrl.trimEnd('/')
    private val gson    = Gson()
    private val json    = "application/json; charset=utf-8".toMediaType()

    // ── Paths decoded from C layer (never plain strings) ─────────────────────

    private val adminMoviesPath       by lazy { NativeApiSecurity.getAdminMoviesPath() }
    private val moviesPrefix          by lazy { NativeApiSecurity.getMoviesPrefix() }
    private val adminSeriesPath       by lazy { NativeApiSecurity.getAdminSeriesPath() }
    private val adminSeriesPrefix     by lazy { NativeApiSecurity.getAdminSeriesPrefix() }
    private val adminEpisodesPath     by lazy { NativeApiSecurity.getAdminEpisodesPath() }
    private val adminGenresPath       by lazy { NativeApiSecurity.getAdminGenresPath() }
    private val adminGenresPrefix     by lazy { NativeApiSecurity.getAdminGenresPrefix() }
    private val adminUsersPath        by lazy { NativeApiSecurity.getAdminUsersPath() }
    private val adminUsersPrefix      by lazy { NativeApiSecurity.getAdminUsersPrefix() }
    private val adminRequestsPath     by lazy { NativeApiSecurity.getAdminRequestsPath() }
    private val adminRequestsPrefix   by lazy { NativeApiSecurity.getAdminRequestsPrefix() }
    private val tmdbSearchPath        by lazy { NativeApiSecurity.getAdminTmdbSearchPath() }
    private val tmdbMoviePrefix       by lazy { NativeApiSecurity.getAdminTmdbMoviePrefix() }
    private val tmdbTvPrefix          by lazy { NativeApiSecurity.getAdminTmdbTvPrefix() }
    // Legacy /api/admin/jikan/* routes are still active — now proxied to AniList server-side.
    private val aniListSearchPath  by lazy { NativeApiSecurity.getAdminJikanSearchPath() }
    private val aniListAnimePrefix by lazy { NativeApiSecurity.getAdminJikanAnimePrefix() }
    private val adminAppVersionsPath   by lazy { NativeApiSecurity.getAdminAppVersionsPath() }
    private val adminAppVersionsPrefix by lazy { NativeApiSecurity.getAdminAppVersionsPrefix() }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun url(path: String) = "$base$path"

    private inline fun <reified T> get(path: String): T {
        val req = Request.Builder().url(url(path)).get().build()
        val body = okHttp.newCall(req).execute().use { resp ->
            val bodyStr = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception(extractError(bodyStr, resp.code))
            bodyStr
        }
        return gson.fromJson(body, T::class.java)
    }

    private inline fun <reified T> post(path: String, payload: Any): T {
        val body = gson.toJson(payload).toRequestBody(json)
        val req  = Request.Builder().url(url(path)).post(body).build()
        val resp = okHttp.newCall(req).execute()
        val bodyStr = resp.body?.string() ?: ""
        if (!resp.isSuccessful) throw Exception(extractError(bodyStr, resp.code))
        return gson.fromJson(bodyStr, T::class.java)
    }

    private inline fun <reified T> patch(path: String, payload: Any): T {
        val body = gson.toJson(payload).toRequestBody(json)
        val req  = Request.Builder().url(url(path)).patch(body).build()
        val resp = okHttp.newCall(req).execute()
        val bodyStr = resp.body?.string() ?: ""
        if (!resp.isSuccessful) throw Exception(extractError(bodyStr, resp.code))
        return gson.fromJson(bodyStr, T::class.java)
    }

    private fun delete(path: String) {
        val req  = Request.Builder().url(url(path)).delete().build()
        val resp = okHttp.newCall(req).execute()
        val bodyStr = resp.body?.string() ?: ""
        if (!resp.isSuccessful) throw Exception(extractError(bodyStr, resp.code))
    }

    private fun extractError(body: String, code: Int): String = try {
        gson.fromJson(body, AdminErrorResponse::class.java)?.error
            ?: "HTTP $code"
    } catch (_: Exception) { "HTTP $code" }

    // ── Movies ────────────────────────────────────────────────────────────────

    suspend fun listMovies(
        page: Int = 1,
        limit: Int = 24,
        sort: String = "latest",
        status: String? = null,   // "published" | "draft" | null = all
        q: String? = null
    ): AdminMoviesListResponse = withContext(Dispatchers.IO) {
        // GET /api/admin/movies — admin endpoint returns ALL statuses (draft + published).
        // The public /api/movies endpoint only returns published items.
        val params = buildString {
            append("?page=$page&limit=$limit&sort=$sort")
            if (!status.isNullOrBlank()) append("&status=$status")
            if (!q.isNullOrBlank()) append("&q=${java.net.URLEncoder.encode(q, "UTF-8")}")
        }
        get("$adminMoviesPath$params")
    }

    suspend fun getMovie(id: String): AdminMovie = withContext(Dispatchers.IO) {
        val r: AdminMovieResponse = get("${moviesPrefix}$id")
        r.movie
    }

    suspend fun createMovie(req: CreateMovieRequest): AdminMovie = withContext(Dispatchers.IO) {
        val r: AdminMovieResponse = post(adminMoviesPath, req)
        r.movie
    }

    suspend fun updateMovie(id: String, req: UpdateMovieRequest): AdminMovie =
        withContext(Dispatchers.IO) {
            val r: AdminMovieResponse = patch("${moviesPrefix}$id", req)
            r.movie
        }

    suspend fun deleteMovie(id: String): Unit = withContext(Dispatchers.IO) {
        delete("${moviesPrefix}$id")
    }

    // ── Series ────────────────────────────────────────────────────────────────

    suspend fun listSeries(
        type: String = "anime",
        page: Int = 1,
        limit: Int = 24,
        sort: String = "latest",
        publishStatus: String? = null,  // "published" | "draft" | null = all
        status: String? = null,         // "ongoing" | "completed" | null = all
        q: String? = null
    ): AdminSeriesListResponse = withContext(Dispatchers.IO) {
        // GET /api/admin/series — admin endpoint returns ALL publishStatus values (draft + published).
        // The public /api/anime endpoint only returns published items.
        val params = buildString {
            append("?type=$type&page=$page&limit=$limit&sort=$sort")
            if (!publishStatus.isNullOrBlank()) append("&publishStatus=$publishStatus")
            if (!status.isNullOrBlank()) append("&status=$status")
            if (!q.isNullOrBlank()) append("&q=${java.net.URLEncoder.encode(q, "UTF-8")}")
        }
        get("$adminSeriesPath$params")
    }

    suspend fun createSeries(req: CreateSeriesRequest): AdminSeries =
        withContext(Dispatchers.IO) {
            val r: AdminSeriesResponse = post(adminSeriesPath, req)
            r.series
        }

    suspend fun updateSeries(id: String, req: UpdateSeriesRequest): AdminSeries =
        withContext(Dispatchers.IO) {
            val r: AdminSeriesResponse = patch("$adminSeriesPrefix$id", req)
            r.series
        }

    suspend fun deleteSeries(id: String): Unit = withContext(Dispatchers.IO) {
        delete("$adminSeriesPrefix$id")
    }

    // ── Episodes ──────────────────────────────────────────────────────────────

    suspend fun listEpisodes(seriesId: String): List<ApiAdminEpisode> =
        withContext(Dispatchers.IO) {
            val r: AdminEpisodesListResponse = get("$adminEpisodesPath?seriesId=$seriesId")
            r.episodes
        }

    suspend fun createEpisode(req: CreateEpisodeRequest): ApiAdminEpisode =
        withContext(Dispatchers.IO) {
            val r: AdminEpisodeResponse = post(adminEpisodesPath, req)
            r.episode
        }

    suspend fun updateEpisode(id: String, req: UpdateEpisodeRequest): ApiAdminEpisode =
        withContext(Dispatchers.IO) {
            val r: AdminEpisodeResponse = patch("$adminEpisodesPath/$id", req)
            r.episode
        }

    suspend fun deleteEpisode(id: String): Unit = withContext(Dispatchers.IO) {
        delete("$adminEpisodesPath/$id")
    }

    // ── Genres ────────────────────────────────────────────────────────────────

    suspend fun listGenres(): List<ApiGenre> = withContext(Dispatchers.IO) {
        val r: AdminGenresResponse = get(adminGenresPath)
        r.genres
    }

    suspend fun createGenre(name: String): ApiGenre = withContext(Dispatchers.IO) {
        val r: AdminGenreResponse = post(adminGenresPath, CreateGenreRequest(name))
        r.genre
    }

    suspend fun deleteGenre(id: String): Unit = withContext(Dispatchers.IO) {
        delete("$adminGenresPrefix$id")
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    suspend fun listUsers(
        page: Int = 1,
        limit: Int = 50,
        role: String? = null,
        q: String? = null
    ): AdminUsersResponse = withContext(Dispatchers.IO) {
        val params = buildString {
            append("?page=$page&limit=$limit")
            if (!role.isNullOrBlank()) append("&role=$role")
            if (!q.isNullOrBlank()) append("&q=${java.net.URLEncoder.encode(q, "UTF-8")}")
        }
        get("$adminUsersPath$params")
    }

    suspend fun updateUserRole(id: String, role: String): AdminUser =
        withContext(Dispatchers.IO) {
            val r: AdminUserResponse = patch("$adminUsersPrefix$id", UpdateUserRoleRequest(role))
            r.user
        }

    // ── Content Requests (admin) ──────────────────────────────────────────────

    suspend fun listRequests(): List<AdminContentRequest> = withContext(Dispatchers.IO) {
        val r: AdminRequestsResponse = get(adminRequestsPath)
        r.requests
    }

    suspend fun updateRequest(id: String, status: String, adminNote: String?): AdminContentRequest =
        withContext(Dispatchers.IO) {
            val r: AdminRequestResponse = patch(
                "$adminRequestsPrefix$id",
                UpdateRequestBody(status, adminNote)
            )
            r.request
        }

    suspend fun deleteRequest(id: String): Unit = withContext(Dispatchers.IO) {
        delete("$adminRequestsPrefix$id")
    }

    // ── TMDB Autofill ─────────────────────────────────────────────────────────

    suspend fun searchTmdb(query: String, type: String = "movie"): List<TmdbSearchResult> =
        withContext(Dispatchers.IO) {
            val q = java.net.URLEncoder.encode(query, "UTF-8")
            val r: TmdbSearchResponse = get("$tmdbSearchPath?q=$q&type=$type")
            r.results
        }

    suspend fun getTmdbMovieDetails(tmdbId: String): TmdbMovieDetailsResponse =
        withContext(Dispatchers.IO) {
            get("$tmdbMoviePrefix$tmdbId")
        }

    suspend fun getTmdbTvDetails(tmdbId: String): TmdbTvDetailsResponse =
        withContext(Dispatchers.IO) {
            get("$tmdbTvPrefix$tmdbId")
        }

    // ── Jikan Autofill ────────────────────────────────────────────────────────

    suspend fun searchAniList(query: String): List<AniListSearchResult> =
        withContext(Dispatchers.IO) {
            val q = java.net.URLEncoder.encode(query, "UTF-8")
            val r: AniListSearchResponse = get("$aniListSearchPath?q=$q")
            r.results
        }

    suspend fun getAniListCharacters(aniListId: String): List<AniListCastMember> =
        withContext(Dispatchers.IO) {
            val r: AniListCharactersResponse = get("${aniListAnimePrefix}$aniListId/characters")
            r.cast
        }

    /**
     * GET /api/admin/anilist/anime/{id}/episodes/{episodeNumber}
     *
     * AniList does not expose per-episode titles. The server always returns
     * { "title": null, "note": "..." }. Call this when you want to show the
     * "enter manually" note in the UI; handle a null title gracefully.
     */
    suspend fun getAniListEpisodeTitle(aniListId: String, episodeNumber: Int): AniListEpisodeTitleResponse =
        withContext(Dispatchers.IO) {
            get("${aniListAnimePrefix}$aniListId/episodes/$episodeNumber")
        }

    /**
     * Resolves a list of genre names to local ObjectIds by calling the
     * idempotent POST /api/admin/genres endpoint for each name.
     *
     * Per spec §7.2, the endpoint returns the existing genre if the name
     * already exists (case-insensitive), or creates it and returns the new one.
     * Failures for individual genres are silently skipped so a single bad name
     * does not abort the whole resolution.
     */
    suspend fun resolveGenreNames(names: List<String>): List<String> =
        withContext(Dispatchers.IO) {
            names.mapNotNull { name ->
                try { createGenre(name).id } catch (_: Exception) { null }
            }
        }

    // ── App Version Management ────────────────────────────────────────────────

    suspend fun listAppVersions(
        page: Int = 1,
        limit: Int = 50,
        platform: String? = null,
        channel: String? = null
    ): AdminAppVersionsListResponse = withContext(Dispatchers.IO) {
        val params = buildString {
            append("?page=$page&limit=$limit")
            if (!platform.isNullOrBlank()) append("&platform=$platform")
            if (!channel.isNullOrBlank()) append("&channel=$channel")
        }
        get("$adminAppVersionsPath$params")
    }

    suspend fun getAppVersion(id: String): AdminAppVersion = withContext(Dispatchers.IO) {
        val r: AdminAppVersionResponse = get("$adminAppVersionsPrefix$id")
        r.version
    }

    suspend fun createAppVersion(req: CreateAppVersionRequest): AdminAppVersion =
        withContext(Dispatchers.IO) {
            val r: AdminAppVersionResponse = post(adminAppVersionsPath, req)
            r.version
        }

    suspend fun updateAppVersion(id: String, req: UpdateAppVersionRequest): AdminAppVersion =
        withContext(Dispatchers.IO) {
            val r: AdminAppVersionResponse = patch("$adminAppVersionsPrefix$id", req)
            r.version
        }

    suspend fun deleteAppVersion(id: String): Unit = withContext(Dispatchers.IO) {
        delete("$adminAppVersionsPrefix$id")
    }
}

