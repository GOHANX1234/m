package com.mna.streaming.security

/**
 * JNI bridge to the native security layer (security.c).
 *
 * All sensitive strings (base URL, cert pins, API paths, client identifier)
 * are XOR-obfuscated in the compiled .so and decoded in native memory at
 * runtime — they never appear as plain strings anywhere in the DEX or APK.
 */
object NativeApiSecurity {

    init {
        System.loadLibrary("security")
    }

    /** Returns "https://sarrows.vercel.app" */
    @JvmStatic external fun getBaseUrl(): String

    /** Returns the SHA-256 pin for the intermediate CA (Google Trust Services WR1). */
    @JvmStatic external fun getCertPinIntermediate(): String

    /** Returns the SHA-256 pin for the root CA (GTS Root R1). */
    @JvmStatic external fun getCertPinRoot(): String

    /**
     * Returns the movie streaming API path prefix: "/api/stream/movie"
     * Used by MovieRepository to build embed URLs via OkHttp directly.
     */
    @JvmStatic external fun getStreamPath(): String

    /** Returns the embed URL suffix: "/embed" */
    @JvmStatic external fun getEmbedSuffix(): String

    /** Returns the custom client identifier: "M&A-Android/1.0" */
    @JvmStatic external fun getClientTag(): String

    /** Returns the anime browse path: "/api/anime" */
    @JvmStatic external fun getAnimeBrowsePath(): String

    /** Returns the anime episode stream base path: "/api/stream/episode/" */
    @JvmStatic external fun getAnimeStreamEpisodePath(): String

    /** Returns the public episodes list path: "/api/episodes" */
    @JvmStatic external fun getEpisodesPath(): String

    /** Returns the admin episodes list path: "/api/admin/episodes" */
    @JvmStatic external fun getAdminEpisodesPath(): String

    // ── Admin paths — all decoded at runtime, never plain strings in the DEX ──

    /** Returns "/api/admin/movies" — used for create (POST) and list (GET). */
    @JvmStatic external fun getAdminMoviesPath(): String

    /** Returns "/api/movies/" — prefix for PATCH/DELETE /api/movies/{id}. */
    @JvmStatic external fun getMoviesPrefix(): String

    /** Returns "/api/admin/series" — used for create (POST) and list (GET). */
    @JvmStatic external fun getAdminSeriesPath(): String

    /** Returns "/api/admin/series/" — prefix for PATCH/DELETE /api/admin/series/{id}. */
    @JvmStatic external fun getAdminSeriesPrefix(): String

    /** Returns "/api/admin/genres" — used for list (GET) and create (POST). */
    @JvmStatic external fun getAdminGenresPath(): String

    /** Returns "/api/admin/genres/" — prefix for DELETE /api/admin/genres/{id}. */
    @JvmStatic external fun getAdminGenresPrefix(): String

    /** Returns "/api/admin/users" — used for paginated user list (GET). */
    @JvmStatic external fun getAdminUsersPath(): String

    /** Returns "/api/admin/users/" — prefix for PATCH /api/admin/users/{id}. */
    @JvmStatic external fun getAdminUsersPrefix(): String

    /** Returns "/api/admin/requests" — admin view of all content requests. */
    @JvmStatic external fun getAdminRequestsPath(): String

    /** Returns "/api/admin/requests/" — prefix for PATCH/DELETE /api/admin/requests/{id}. */
    @JvmStatic external fun getAdminRequestsPrefix(): String

    /** Returns "/api/admin/tmdb/search" — TMDB movie/TV search proxy. */
    @JvmStatic external fun getAdminTmdbSearchPath(): String

    /** Returns "/api/admin/tmdb/movie/" — prefix; TMDB ID appended at runtime. */
    @JvmStatic external fun getAdminTmdbMoviePrefix(): String

    /** Returns "/api/admin/jikan/search" — Jikan anime search proxy. */
    @JvmStatic external fun getAdminJikanSearchPath(): String

    /** Returns "/api/admin/jikan/anime/" — prefix; MAL ID + suffix appended at runtime. */
    @JvmStatic external fun getAdminJikanAnimePrefix(): String

    /** Returns "/api/admin/tmdb/tv/" — prefix; TMDB TV ID appended at runtime. */
    @JvmStatic external fun getAdminTmdbTvPrefix(): String

    /** Returns "/api/admin/app-versions" — used for list (GET) and create (POST). */
    @JvmStatic external fun getAdminAppVersionsPath(): String

    /** Returns "/api/admin/app-versions/" — prefix for PATCH/DELETE /api/admin/app-versions/{id}. */
    @JvmStatic external fun getAdminAppVersionsPrefix(): String

    /** Returns the protected developer credit shown in the Profile > About tab. */
    @JvmStatic external fun getDeveloperCredit(): String

    /** Returns the protected Telegram URL for the developer profile. */
    @JvmStatic external fun getDeveloperTelegramUrl(): String

    /** Returns the protected Telegram channel URL. */
    @JvmStatic external fun getTelegramChannelUrl(): String

    // ── Update check paths — stored in ma-check.c, never plain strings in DEX ──

    /** Returns the OTA update-check API path: "/api/app/version/check" */
    @JvmStatic external fun getUpdateCheckPath(): String

    /** Returns the platform identifier for the update-check request: "android" */
    @JvmStatic external fun getUpdatePlatform(): String

    /** Returns the default channel for the update-check request: "stable" */
    @JvmStatic external fun getUpdateChannel(): String
}
