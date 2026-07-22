/**
 * M&A Streaming — Native Security Layer
 *
 * Sensitive strings are XOR-obfuscated with key 0x37.
 * They are decoded in native memory at runtime; they never appear as
 * plain strings in the binary, making string-table scanning harder.
 *
 * Strings stored here:
 *   - Base URL              (https://sarrows.vercel.app)
 *   - Certificate pins      (intermediate CA + root CA)
 *   - Stream path prefix    (/api/stream/movie)
 *   - Embed URL suffix      (/embed)
 *   - Client identifier     (M&A-Android/1.0)
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

/* "/api/stream/movie" XOR 0x37
   Hides the streaming endpoint prefix so it does not appear as a
   plain string in the DEX/binary, making extraction harder. */
static const unsigned char OBF_STREAM_PATH[] = {
    0x18,0x56,0x47,0x5E,0x18,0x44,0x43,0x45,
    0x52,0x56,0x5A,0x18,0x5A,0x58,0x41,0x5E,
    0x52,0x37
};

/* "/embed" XOR 0x37 — the embed suffix appended to the stream path */
static const unsigned char OBF_EMBED_SUFFIX[] = {
    0x18,0x52,0x5A,0x55,0x52,0x53,0x37
};

/* "M&A-Android/1.0" XOR 0x37 — custom User-Agent / client identifier */
static const unsigned char OBF_CLIENT_TAG[] = {
    0x7A,0x11,0x76,0x1A,0x76,0x59,0x53,0x45,
    0x58,0x5E,0x53,0x18,0x06,0x19,0x07,0x37
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
