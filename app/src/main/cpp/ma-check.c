/**
 * M&A Streaming — Update Check Security Layer  (ma-check.c)
 *
 * Stores all strings used by the OTA update check API as XOR-obfuscated
 * byte arrays with key 0x37.  They are decoded in native memory at runtime
 * and freed immediately after the JNI call returns, so they never reside as
 * plain strings in the DEX, string tables, or APK resource files.
 *
 * Strings stored here:
 *   — Update check path   (/api/app/version/check)
 *   — Platform identifier (android)
 *   — Channel identifier  (stable)
 *
 * XOR key: 0x37  (same key used across the native security layer)
 */

#include <jni.h>
#include <stdlib.h>
#include <string.h>

#define XOR_KEY 0x37

/* ── Helpers ─────────────────────────────────────────────────────────────── */

/**
 * Decodes an XOR-obfuscated byte array into a freshly malloc-ed, NUL-terminated
 * C string.  The caller is responsible for calling free() on the result.
 *
 * The sentinel 0x37 (= NUL XOR XOR_KEY) marks end-of-string, so we know the
 * exact length without passing it explicitly.
 *
 * Returns NULL on allocation failure.
 */
static char* ma_check_decode(const unsigned char* obf, size_t len) {
    /* len includes the sentinel byte; output is len-1 chars + NUL terminator */
    size_t out_len = len - 1; /* exclude trailing sentinel */
    char* out = (char*)malloc(out_len + 1);
    if (!out) return NULL;
    for (size_t i = 0; i < out_len; i++) {
        out[i] = (char)(obf[i] ^ XOR_KEY);
    }
    out[out_len] = '\0';
    return out;
}

/* ── Obfuscated byte arrays ──────────────────────────────────────────────── */

/*
 * "/api/app/version/check"  XOR 0x37
 *
 * Plaintext bytes (hex):
 *   2F 61 70 69 2F 61 70 70 2F 76 65 72 73 69 6F 6E 2F 63 68 65 63 6B
 * XOR 0x37:
 *   18 56 47 5E 18 56 47 47 18 41 52 45 44 5E 58 59 18 54 5F 52 54 5C
 * Sentinel: 0x37 (= 0x00 ^ 0x37)
 */
static const unsigned char OBF_UPDATE_CHECK_PATH[] = {
    0x18, 0x56, 0x47, 0x5E, /* /api */
    0x18, 0x56, 0x47, 0x47, /* /app */
    0x18, 0x41, 0x52, 0x45, 0x44, 0x5E, 0x58, 0x59, /* /version */
    0x18, 0x54, 0x5F, 0x52, 0x54, 0x5C, /* /check */
    0x37  /* sentinel */
};

/*
 * "android"  XOR 0x37
 *
 * Plaintext bytes (hex):
 *   61 6E 64 72 6F 69 64
 * XOR 0x37:
 *   56 59 53 45 58 5E 53
 * Sentinel: 0x37
 */
static const unsigned char OBF_PLATFORM[] = {
    0x56, 0x59, 0x53, 0x45, 0x58, 0x5E, 0x53,
    0x37  /* sentinel */
};

/*
 * "stable"  XOR 0x37
 *
 * Plaintext bytes (hex):
 *   73 74 61 62 6C 65
 * XOR 0x37:
 *   44 43 56 55 5B 52
 * Sentinel: 0x37
 */
static const unsigned char OBF_CHANNEL[] = {
    0x44, 0x43, 0x56, 0x55, 0x5B, 0x52,
    0x37  /* sentinel */
};

/* ── JNI exports ─────────────────────────────────────────────────────────── */

/**
 * Returns the OTA update-check API path: "/api/app/version/check"
 * Called by UpdateRepository via NativeApiSecurity.getUpdateCheckPath().
 */
JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getUpdateCheckPath(
        JNIEnv* env, jclass clazz) {
    char* path = ma_check_decode(OBF_UPDATE_CHECK_PATH, sizeof(OBF_UPDATE_CHECK_PATH));
    if (!path) return NULL;
    jstring result = (*env)->NewStringUTF(env, path);
    free(path);
    return result;
}

/**
 * Returns the platform identifier sent in the update-check request: "android"
 * Keeping this in native memory prevents a trivial strings(1) scan from
 * revealing the platform field name used by the API.
 */
JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getUpdatePlatform(
        JNIEnv* env, jclass clazz) {
    char* platform = ma_check_decode(OBF_PLATFORM, sizeof(OBF_PLATFORM));
    if (!platform) return NULL;
    jstring result = (*env)->NewStringUTF(env, platform);
    free(platform);
    return result;
}

/**
 * Returns the default channel identifier sent in the update-check request: "stable"
 * Beta opt-in logic is handled in Kotlin; this always returns the baseline channel.
 */
JNIEXPORT jstring JNICALL
Java_com_mna_streaming_security_NativeApiSecurity_getUpdateChannel(
        JNIEnv* env, jclass clazz) {
    char* channel = ma_check_decode(OBF_CHANNEL, sizeof(OBF_CHANNEL));
    if (!channel) return NULL;
    jstring result = (*env)->NewStringUTF(env, channel);
    free(channel);
    return result;
}
