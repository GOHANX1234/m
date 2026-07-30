/**
 * M&A Streaming — Native Security Layer
 *
 * Sensitive strings are XOR-obfuscated with key 0x37.
 * They are decoded in native memory at runtime; they never appear as
 * plain strings in the binary, making string-table scanning harder.
 *
 * Strings stored here:
 *   — Base URL                      (https://sarrows.vercel.app)
 *   — Certificate pins              (intermediate CA + root CA)
 *   — Stream path prefix            (/api/stream/movie)
 *   — Embed URL suffix              (/embed)
 *   — Client identifier             (M&A-Android/1.0)
 *   — Anime browse path             (/api/anime)
 *   — Anime stream path             (/api/stream/episode/)
 *   — Public episodes path          (/api/episodes)
 *   — Admin episodes path           (/api/admin/episodes)
 *   — Admin movies path             (/api/admin/movies)
 *   — Movies prefix                 (/api/movies/)
 *   — Admin series path             (/api/admin/series)
 *   — Admin series prefix           (/api/admin/series/)
 *   — Admin genres path             (/api/admin/genres)
 *   — Admin genres prefix           (/api/admin/genres/)
 *   — Admin users path              (/api/admin/users)
 *   — Admin users prefix            (/api/admin/users/)
 *   — Admin requests path           (/api/admin/requests)
 *   — Admin requests prefix         (/api/admin/requests/)
 *   — Admin TMDB search path        (/api/admin/tmdb/search)
 *   — Admin TMDB movie prefix       (/api/admin/tmdb/movie/)
 *   — Admin Jikan search path       (/api/admin/jikan/search)
 *   — Admin Jikan anime prefix      (/api/admin/jikan/anime/)
 *   — Developer credit              (Developed By @Gohan52)
 *   — Developer Telegram URL        (https://t.me/Gohan52)
 *   — Telegram channel URL          (https://t.me/ClerXin)
 *
 * XOR key: 0x37
 */

#include <jni.h>
#include <stdlib.h>
#include <string.h>

#define XOR_KEY 0x37

/* ── Obfuscated byte arrays ──────────────────────────────────────────────── */

/* "https://sarrows.vercel.app" XOR 0x37 */
static const unsigned char OBF_BASE_URL[] = {
    0x5F,0x43,0x43,0x47,0x44,0x0D,0x18,0x18,
    0x44,0x56,0x45,0x45,0x58,0x40,0x44,0x19,
    0x41,0x52,0x45,0x54,0x52,0x5B,0x19,0x56,
    0x47,0x47,0x37
};

/* "sha256/yDu9og255NN5GEf+Bwa9rTrqFQ0EydZ0r1FCh9TdAW4=" XOR 0x37
   (Intermediate CA — Google Trust Services WR1) */
static const unsigned char OBF_PIN_INTERMEDIATE[] = {
    0x44,0x5F,0x56,0x05,0x02,0x01,0x18,0x4E,
    0x73,0x42,0x0E,0x58,0x50,0x05,0x02,0x02,
    0x79,0x79,0x02,0x70,0x72,0x51,0x1C,0x75,
    0x40,0x56,0x0E,0x45,0x63,0x45,0x46,0x71,
    0x66,0x07,0x72,0x4E,0x53,0x6D,0x07,0x45,
    0x06,0x71,0x74,0x5F,0x0E,0x63,0x53,0x76,
    0x60,0x03,0x0A,0x37
};

/* "sha256/hxqRlPTu1bMS/0DITB1SSu0vd4u/8l8TjPgfaAp63Gc=" XOR 0x37
   (Root CA — GTS Root R1) */
static const unsigned char OBF_PIN_ROOT[] = {
    0x44,0x5F,0x56,0x05,0x02,0x01,0x18,0x5F,
    0x4F,0x46,0x65,0x5B,0x67,0x63,0x42,0x06,
    0x55,0x7A,0x64,0x18,0x07,0x73,0x7E,0x63,
    0x75,0x06,0x64,0x64,0x42,0x07,0x41,0x53,
    0x03,0x42,0x18,0x0F,0x5B,0x0F,0x63,0x5D,
    0x67,0x50,0x51,0x56,0x76,0x47,0x01,0x04,
    0x70,0x54,0x0A,0x37
};

/* "/api/stream/movie" XOR 0x37 */
static const unsigned char OBF_STREAM_PATH[] = {
    0x18,0x56,0x47,0x5E,0x18,0x44,0x43,0x45,
    0x52,0x56,0x5A,0x18,0x5A,0x58,0x41,0x5E,
    0x52,0x37
};

/* "/embed" XOR 0x37 */
static const unsigned char OBF_EMBED_SUFFIX[] = {
    0x18,0x52,0x5A,0x55,0x52,0x53,0x37
};

/* "M&A-Android/1.0" XOR 0x37 */
static const unsigned char OBF_CLIENT_TAG[] = {
    0x7A,0x11,0x76,0x1A,0x76,0x59,0x53,0x45,
    0x58,0x5E,0x53,0x18,0x06,0x19,0x07,0x37
};

/* "/api/anime" XOR 0x37 */
static const unsigned char OBF_ANIME_BROWSE_PATH[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x59,0x5E,
    0x5A,0x52,0x37
};

/* "/api/stream/episode/" XOR 0x37 */
static const unsigned char OBF_ANIME_STREAM_PATH[] = {
    0x18,0x56,0x47,0x5E,0x18,0x44,0x43,0x45,
    0x52,0x56,0x5A,0x18,0x52,0x47,0x5E,0x44,
    0x58,0x53,0x52,0x18,0x37
};

/* "/api/episodes" XOR 0x37 */
static const unsigned char OBF_EPISODES_PATH[] = {
    0x18,0x56,0x47,0x5E,0x18,0x52,0x47,0x5E,
    0x44,0x58,0x53,0x52,0x44,0x37
};

/* "/api/admin/episodes" XOR 0x37 */
static const unsigned char OBF_ADMIN_EPISODES_PATH[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x52,0x47,0x5E,0x44,0x58,
    0x53,0x52,0x44,0x37
};

/* "/api/admin/movies" XOR 0x37
   Used for create (POST) and list (GET) of movies in admin context.
   Sensitive — never a plain string in the binary. */
static const unsigned char OBF_ADMIN_MOVIES_PATH[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x5A,0x58,0x41,0x5E,0x52,
    0x44,0x37
};

/* "/api/movies/" XOR 0x37
   Prefix for individual movie operations (PATCH/DELETE /api/movies/{id}).
   ID is appended at runtime. */
static const unsigned char OBF_MOVIES_PREFIX[] = {
    0x18,0x56,0x47,0x5E,0x18,0x5A,0x58,0x41,
    0x5E,0x52,0x44,0x18,0x37
};

/* "/api/admin/series" XOR 0x37
   Used for create (POST) of series/anime. */
static const unsigned char OBF_ADMIN_SERIES_PATH[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x44,0x52,0x45,0x5E,0x52,
    0x44,0x37
};

/* "/api/admin/series/" XOR 0x37
   Prefix for update/delete of a specific series. ID appended at runtime. */
static const unsigned char OBF_ADMIN_SERIES_PREFIX[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x44,0x52,0x45,0x5E,0x52,
    0x44,0x18,0x37
};

/* "/api/admin/genres" XOR 0x37
   Used for list (GET) and create (POST) genres. */
static const unsigned char OBF_ADMIN_GENRES_PATH[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x50,0x52,0x59,0x45,0x52,
    0x44,0x37
};

/* "/api/admin/genres/" XOR 0x37
   Prefix for deleting a specific genre. ID appended at runtime. */
static const unsigned char OBF_ADMIN_GENRES_PREFIX[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x50,0x52,0x59,0x45,0x52,
    0x44,0x18,0x37
};

/* "/api/admin/users" XOR 0x37
   Used for paginated user list (GET). */
static const unsigned char OBF_ADMIN_USERS_PATH[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x42,0x44,0x52,0x45,0x44,
    0x37
};

/* "/api/admin/users/" XOR 0x37
   Prefix for updating a specific user's role. ID appended at runtime. */
static const unsigned char OBF_ADMIN_USERS_PREFIX[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x42,0x44,0x52,0x45,0x44,
    0x18,0x37
};

/* "/api/admin/requests" XOR 0x37
   Admin view of all content requests (GET). */
static const unsigned char OBF_ADMIN_REQUESTS_PATH[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x45,0x52,0x46,0x42,0x52,
    0x44,0x43,0x44,0x37
};

/* "/api/admin/requests/" XOR 0x37
   Prefix for update (PATCH) or delete (DELETE) of a specific request. */
static const unsigned char OBF_ADMIN_REQUESTS_PREFIX[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x45,0x52,0x46,0x42,0x52,
    0x44,0x43,0x44,0x18,0x37
};

/* "/api/admin/tmdb/search" XOR 0x37
   TMDB movie/TV search proxy — autofill for create/edit forms. */
static const unsigned char OBF_ADMIN_TMDB_SEARCH[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x43,0x5A,0x53,0x55,0x18,
    0x44,0x52,0x56,0x45,0x54,0x5F,0x37
};

/* "/api/admin/tmdb/movie/" XOR 0x37
   Prefix for TMDB movie detail fetch. TMDB ID appended at runtime. */
static const unsigned char OBF_ADMIN_TMDB_MOVIE_PREFIX[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x43,0x5A,0x53,0x55,0x18,
    0x5A,0x58,0x41,0x5E,0x52,0x18,0x37
};

/* "/api/admin/jikan/search" XOR 0x37
   Jikan (MyAnimeList) anime search proxy — autofill for anime forms. */
static const unsigned char OBF_ADMIN_JIKAN_SEARCH[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x5D,0x5E,0x5C,0x56,0x59,
    0x18,0x44,0x52,0x56,0x45,0x54,0x5F,0x37
};

/* "/api/admin/jikan/anime/" XOR 0x37
   Prefix for Jikan character/episode endpoints.
   MAL ID + path suffix appended at runtime. */
static const unsigned char OBF_ADMIN_JIKAN_ANIME_PREFIX[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x5D,0x5E,0x5C,0x56,0x59,
    0x18,0x56,0x59,0x5E,0x5A,0x52,0x18,0x37
};

/* "Developed By @Gohan52" XOR 0x37 */
static const unsigned char OBF_DEVELOPER_CREDIT[] = {
    0x73,0x52,0x41,0x52,0x5B,0x58,0x47,0x52,
    0x53,0x17,0x75,0x4E,0x17,0x77,0x70,0x58,
    0x5F,0x56,0x59,0x02,0x05,0x37
};

/* "https://t.me/Gohan52" XOR 0x37 */
static const unsigned char OBF_DEVELOPER_TELEGRAM_URL[] = {
    0x5F,0x43,0x43,0x47,0x44,0x0D,0x18,0x18,
    0x43,0x19,0x5A,0x52,0x18,0x70,0x58,0x5F,
    0x56,0x59,0x02,0x05,0x37
};

/* "/api/admin/tmdb/tv/" XOR 0x37
   Prefix for TMDB TV detail fetch. TMDB TV ID appended at runtime. */
static const unsigned char OBF_ADMIN_TMDB_TV_PREFIX[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x43,0x5A,0x53,0x55,0x18,
    0x43,0x41,0x18,0x37
};

/* "/api/admin/app-versions" XOR 0x37
   Used for list (GET) and create (POST) of app version records. */
static const unsigned char OBF_ADMIN_APP_VERSIONS_PATH[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x56,0x47,0x47,0x1A,0x41,
    0x52,0x45,0x44,0x5E,0x58,0x59,0x44,0x37
};

/* "/api/admin/app-versions/" XOR 0x37
   Prefix for PATCH/DELETE /api/admin/app-versions/{id}. */
static const unsigned char OBF_ADMIN_APP_VERSIONS_PREFIX[] = {
    0x18,0x56,0x47,0x5E,0x18,0x56,0x53,0x5A,
    0x5E,0x59,0x18,0x56,0x47,0x47,0x1A,0x41,
    0x52,0x45,0x44,0x5E,0x58,0x59,0x44,0x18,
    0x37
};

/* "https://t.me/ClerXin" XOR 0x37 */
static const unsigned char OBF_TELEGRAM_CHANNEL_URL[] = {
    0x5F,0x43,0x43,0x47,0x44,0x0D,0x18,0x18,
    0x43,0x19,0x5A,0x52,0x18,0x74,0x5B,0x52,
    0x45,0x6F,0x5E,0x59,0x37
};

/* ── Decoder helper ───────────────────────────────────────────────────────── */

/**
 * Decode an XOR-obfuscated byte array into a newly malloc'd C string.
 * The input MUST end with (0x00 ^ XOR_KEY) == 0x37 as a sentinel.
 * Caller is responsible for free()-ing the returned buffer.
 */
static char* decode(const unsigned char* obf, size_t len) {
    char* buf = (char*) malloc(len);
    if (!buf) return NULL;
    for (size_t i = 0; i < len; i++) {
        buf[i] = (char)(obf[i] ^ XOR_KEY);
    }
    /* buf[len-1] == '\0' because the last obf byte == XOR_KEY (0x37 ^ 0x37 = 0) */
    return buf;
}

/* ── JNI exports ──────────────────────────────────────────────────────────── */

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getBaseUrl(
        JNIEnv* env, jclass clazz) {
    char* url = decode(OBF_BASE_URL, sizeof(OBF_BASE_URL));
    if (!url) return NULL;
    jstring result = (*env)->NewStringUTF(env, url);
    free(url);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getCertPinIntermediate(
        JNIEnv* env, jclass clazz) {
    char* pin = decode(OBF_PIN_INTERMEDIATE, sizeof(OBF_PIN_INTERMEDIATE));
    if (!pin) return NULL;
    jstring result = (*env)->NewStringUTF(env, pin);
    free(pin);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getCertPinRoot(
        JNIEnv* env, jclass clazz) {
    char* pin = decode(OBF_PIN_ROOT, sizeof(OBF_PIN_ROOT));
    if (!pin) return NULL;
    jstring result = (*env)->NewStringUTF(env, pin);
    free(pin);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getStreamPath(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_STREAM_PATH, sizeof(OBF_STREAM_PATH));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getEmbedSuffix(
        JNIEnv* env, jclass clazz) {
    char* suf = decode(OBF_EMBED_SUFFIX, sizeof(OBF_EMBED_SUFFIX));
    if (!suf) return NULL;
    jstring result = (*env)->NewStringUTF(env, suf);
    free(suf);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getClientTag(
        JNIEnv* env, jclass clazz) {
    char* tag = decode(OBF_CLIENT_TAG, sizeof(OBF_CLIENT_TAG));
    if (!tag) return NULL;
    jstring result = (*env)->NewStringUTF(env, tag);
    free(tag);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAnimeBrowsePath(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ANIME_BROWSE_PATH, sizeof(OBF_ANIME_BROWSE_PATH));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAnimeStreamEpisodePath(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ANIME_STREAM_PATH, sizeof(OBF_ANIME_STREAM_PATH));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getEpisodesPath(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_EPISODES_PATH, sizeof(OBF_EPISODES_PATH));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminEpisodesPath(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_EPISODES_PATH, sizeof(OBF_ADMIN_EPISODES_PATH));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminMoviesPath(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_MOVIES_PATH, sizeof(OBF_ADMIN_MOVIES_PATH));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getMoviesPrefix(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_MOVIES_PREFIX, sizeof(OBF_MOVIES_PREFIX));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminSeriesPath(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_SERIES_PATH, sizeof(OBF_ADMIN_SERIES_PATH));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminSeriesPrefix(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_SERIES_PREFIX, sizeof(OBF_ADMIN_SERIES_PREFIX));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminGenresPath(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_GENRES_PATH, sizeof(OBF_ADMIN_GENRES_PATH));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminGenresPrefix(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_GENRES_PREFIX, sizeof(OBF_ADMIN_GENRES_PREFIX));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminUsersPath(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_USERS_PATH, sizeof(OBF_ADMIN_USERS_PATH));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminUsersPrefix(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_USERS_PREFIX, sizeof(OBF_ADMIN_USERS_PREFIX));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminRequestsPath(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_REQUESTS_PATH, sizeof(OBF_ADMIN_REQUESTS_PATH));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminRequestsPrefix(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_REQUESTS_PREFIX, sizeof(OBF_ADMIN_REQUESTS_PREFIX));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminTmdbSearchPath(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_TMDB_SEARCH, sizeof(OBF_ADMIN_TMDB_SEARCH));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminTmdbMoviePrefix(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_TMDB_MOVIE_PREFIX, sizeof(OBF_ADMIN_TMDB_MOVIE_PREFIX));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminJikanSearchPath(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_JIKAN_SEARCH, sizeof(OBF_ADMIN_JIKAN_SEARCH));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminJikanAnimePrefix(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_JIKAN_ANIME_PREFIX, sizeof(OBF_ADMIN_JIKAN_ANIME_PREFIX));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getDeveloperCredit(
        JNIEnv* env, jclass clazz) {
    char* credit = decode(OBF_DEVELOPER_CREDIT, sizeof(OBF_DEVELOPER_CREDIT));
    if (!credit) return NULL;
    jstring result = (*env)->NewStringUTF(env, credit);
    free(credit);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getDeveloperTelegramUrl(
        JNIEnv* env, jclass clazz) {
    char* url = decode(OBF_DEVELOPER_TELEGRAM_URL, sizeof(OBF_DEVELOPER_TELEGRAM_URL));
    if (!url) return NULL;
    jstring result = (*env)->NewStringUTF(env, url);
    free(url);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminTmdbTvPrefix(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_TMDB_TV_PREFIX, sizeof(OBF_ADMIN_TMDB_TV_PREFIX));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminAppVersionsPath(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_APP_VERSIONS_PATH, sizeof(OBF_ADMIN_APP_VERSIONS_PATH));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getAdminAppVersionsPrefix(
        JNIEnv* env, jclass clazz) {
    char* path = decode(OBF_ADMIN_APP_VERSIONS_PREFIX, sizeof(OBF_ADMIN_APP_VERSIONS_PREFIX));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getTelegramChannelUrl(
        JNIEnv* env, jclass clazz) {
    char* url = decode(OBF_TELEGRAM_CHANNEL_URL, sizeof(OBF_TELEGRAM_CHANNEL_URL));
    if (!url) return NULL;
    jstring result = (*env)->NewStringUTF(env, url);
    free(url);
    return result;
}
