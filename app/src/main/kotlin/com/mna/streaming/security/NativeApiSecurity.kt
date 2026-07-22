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
     * Returns the streaming API path prefix: "/api/stream/movie"
     * Used by MovieRepository to build embed URLs via OkHttp directly,
     * so the path never appears as a literal string in Kotlin bytecode.
     */
    @JvmStatic external fun getStreamPath(): String

    /**
     * Returns the embed URL suffix: "/embed"
     * Combined with [getStreamPath] and a movie ID to form the full embed URL.
     */
    @JvmStatic external fun getEmbedSuffix(): String

    /**
     * Returns the custom client identifier sent as a User-Agent: "M&A-Android/1.0"
     * Allows server-side analytics to distinguish native app traffic.
     */
    @JvmStatic external fun getClientTag(): String
}
