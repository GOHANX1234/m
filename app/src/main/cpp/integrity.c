/**
 * M&A Streaming — APK Integrity & Anti-Tamper Layer  (integrity.c)
 *
 * Provides three JNI-exported checks called by IntegrityGuard.kt:
 *
 *   1. nativeVerifySignature  — compares the APK signing-certificate SHA-256
 *      fingerprint (computed in Kotlin via PackageManager and passed in as a
 *      64-char lowercase hex string) against an XOR-obfuscated expected value
 *      embedded in native memory.  Uses a constant-time comparison to prevent
 *      timing side-channels.
 *
 *   2. nativeCheckDebugger    — reads /proc/self/status; a non-zero TracerPid
 *      means a debugger (LLDB / GDB / jdwp) is attached.
 *
 *   3. nativeCheckFrida       — scans /proc/self/maps for known Frida-agent,
 *      gadget, and memory-fd markers that appear when Frida is live.
 *
 * XOR key for the stored fingerprint: 0x5A  (separate from API key 0x37).
 * Sentinel byte appended after the 64 hex-chars: (0x00 ^ 0x5A) = 0x5A.
 *
 * All private helpers carry __attribute__((visibility("hidden"))) so the
 * linker strips them from the .dynsym table, raising the bar for symbol-based
 * hooking.
 */

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <android/log.h>

#define SIG_XOR_KEY  0x5A
#define SIG_HEX_LEN  64   /* SHA-256 as lowercase hex = 64 chars */

/* ── Obfuscated expected signing-certificate fingerprint ─────────────────── */
/*
 * Expected cert SHA-256 (lowercase hex, no separators):
 *   55387513f183f746c550f76dd0df0f2f4202cf4dfecebc9555ce630984bb3ce1
 *
 * Each character XOR'd with SIG_XOR_KEY (0x5A).
 * Sentinel = (0x00 ^ 0x5A) = 0x5A appended at index 64.
 *
 * Derivation key:
 *   '0'=0x30, '1'=0x31, ..., '9'=0x39
 *   'a'=0x61, 'b'=0x62, ..., 'f'=0x66
 *   None of these XOR 0x5A yields 0x00, so no premature NUL is possible.
 */
static const unsigned char OBF_EXPECTED_SIG[] = {
    /* 55387513 */ 0x6F,0x6F,0x69,0x62, 0x6D,0x6F,0x6B,0x69,
    /* f183f746 */ 0x3C,0x6B,0x62,0x69, 0x3C,0x6D,0x6E,0x6C,
    /* c550f76d */ 0x39,0x6F,0x6F,0x6A, 0x3C,0x6D,0x6C,0x3E,
    /* d0df0f2f */ 0x3E,0x6A,0x3E,0x3C, 0x6A,0x3C,0x68,0x3C,
    /* 4202cf4d */ 0x6E,0x68,0x6A,0x68, 0x39,0x3C,0x6E,0x3E,
    /* fecebc95 */ 0x3C,0x3F,0x39,0x3F, 0x38,0x39,0x63,0x6F,
    /* 55ce6309 */ 0x6F,0x6F,0x39,0x3F, 0x6C,0x69,0x6A,0x63,
    /* 84bb3ce1 */ 0x62,0x6E,0x38,0x38, 0x69,0x39,0x3F,0x6B,
    /* sentinel */ 0x5A
};

/* ── Helpers ─────────────────────────────────────────────────────────────── */

/**
 * Constant-time byte-wise comparison.
 *
 * Marked __attribute__((noinline)) so the compiler cannot inline and then
 * dead-code-eliminate the loop body after seeing that the result is only
 * used in a branch.  The volatile accumulator additionally prevents
 * short-circuit optimisation and timing side-channels.
 *
 * Returns 0 if [a] and [b] are identical over [len] bytes; non-zero otherwise.
 */
__attribute__((noinline, visibility("hidden")))
static int ct_compare(const char* a, const char* b, size_t len) {
    volatile unsigned int diff = 0;
    for (size_t i = 0; i < len; i++) {
        diff |= (unsigned char)a[i] ^ (unsigned char)b[i];
    }
    return (int)diff;
}

/**
 * Volatile wipe — writes zeroes through a volatile pointer so the compiler
 * cannot prove the writes are dead and elide them (unlike plain memset,
 * which is routinely optimised away on stack buffers that go out of scope).
 *
 * Mirrors the behaviour of explicit_bzero(3) / SecureZeroMemory without
 * requiring platform-specific includes.
 */
__attribute__((noinline, visibility("hidden")))
static void volatile_wipe(void* ptr, size_t len) {
    volatile unsigned char* p = (volatile unsigned char*) ptr;
    while (len--) *p++ = 0;
}

/**
 * Decodes the obfuscated expected fingerprint into [buf] (must be at least
 * SIG_HEX_LEN+1 bytes).  Call volatile_wipe() immediately after the
 * comparison to avoid leaving sensitive data on the stack.
 */
__attribute__((visibility("hidden")))
static void decode_expected_sig(char* buf) {
    for (int i = 0; i < SIG_HEX_LEN; i++) {
        buf[i] = (char)(OBF_EXPECTED_SIG[i] ^ SIG_XOR_KEY);
    }
    buf[SIG_HEX_LEN] = '\0';
}

/* ── 1. Signature verification ───────────────────────────────────────────── */

/**
 * Called from IntegrityGuard.kt with the lowercase hex SHA-256 fingerprint
 * of the first APK signing certificate as computed by PackageManager.
 *
 * @param hexFingerprint  64-char lowercase hex string, e.g. "a1b2c3..."
 * @return JNI_TRUE  if the fingerprint matches the embedded expected value.
 *         JNI_FALSE if it does not match or the input is malformed / null.
 */
JNIEXPORT jboolean JNICALL
Java_com_mna_streaming_security_IntegrityGuard_nativeVerifySignature(
        JNIEnv* env, jclass clazz, jstring hexFingerprint) {

    if (!hexFingerprint) return JNI_FALSE;

    const char* fp = (*env)->GetStringUTFChars(env, hexFingerprint, NULL);
    if (!fp) return JNI_FALSE;

    jboolean result = JNI_FALSE;

    if (strlen(fp) == SIG_HEX_LEN) {
        char expected[SIG_HEX_LEN + 1];
        decode_expected_sig(expected);

        if (ct_compare(fp, expected, SIG_HEX_LEN) == 0) {
            result = JNI_TRUE;
        }

        /* Volatile wipe — guaranteed not to be elided by the compiler */
        volatile_wipe(expected, sizeof(expected));
    }

    (*env)->ReleaseStringUTFChars(env, hexFingerprint, fp);
    return result;
}

/* ── 2. Debugger detection ───────────────────────────────────────────────── */

/**
 * Reads /proc/self/status and inspects the TracerPid field.
 *
 * Under normal execution TracerPid is "0".  When a debugger (LLDB, GDB,
 * Android Studio debugger, jdwp) is attached the kernel sets TracerPid to
 * the debugger's PID — any non-zero value signals active debugging.
 *
 * @return JNI_TRUE  if a debugger is detected.
 *         JNI_FALSE otherwise (including on read failure, which is safe-fail).
 */
JNIEXPORT jboolean JNICALL
Java_com_mna_streaming_security_IntegrityGuard_nativeCheckDebugger(
        JNIEnv* env, jclass clazz) {

    FILE* f = fopen("/proc/self/status", "r");
    if (!f) return JNI_FALSE;

    char line[256];
    jboolean detected = JNI_FALSE;

    while (fgets(line, (int)sizeof(line), f)) {
        if (strncmp(line, "TracerPid:", 10) == 0) {
            const char* p = line + 10;
            /* Skip whitespace */
            while (*p == ' ' || *p == '\t') p++;
            /*
             * TracerPid is "0\n" when clean.  Any other value — including
             * multi-digit PIDs like "1234\n" — means a tracer is attached.
             */
            if (!(*p == '0' && (*(p + 1) == '\n' || *(p + 1) == '\r' || *(p + 1) == '\0'))) {
                detected = JNI_TRUE;
            }
            break;
        }
    }

    fclose(f);
    return detected;
}

/* ── 3. Frida / injection detection ──────────────────────────────────────── */

/*
 * Markers that appear in /proc/self/maps when Frida is injected:
 *
 *   frida-agent      — frida-agent-<arch>.so mapped as a shared library
 *   frida_agent      — alternate underscore variant
 *   frida-gadget     — frida-gadget.so when using gadget injection
 *   gum-js-loop      — Frida's internal Gum JS event-loop thread name
 *   linjector        — linjector / frida-inject helper
 *   /memfd:frida     — frida agent loaded via anonymous memfd
 *   /memfd:jit-cache — Frida's JIT compilation cache in memfd
 */
static const char* const FRIDA_MARKERS[] = {
    "frida-agent",
    "frida_agent",
    "frida-gadget",
    "gum-js-loop",
    "linjector",
    "/memfd:frida",
    /* NOTE: "/memfd:jit-cache" removed — Android's ART JIT compiler creates
     * this mapping legitimately on all modern devices, causing false positives
     * on genuine release builds. Only Frida-specific memfd names are kept. */
    NULL
};

/**
 * Scans /proc/self/maps line-by-line for any known Frida marker.
 *
 * @return JNI_TRUE  if any Frida-related mapping is found.
 *         JNI_FALSE otherwise (safe-fail on read error).
 */
JNIEXPORT jboolean JNICALL
Java_com_mna_streaming_security_IntegrityGuard_nativeCheckFrida(
        JNIEnv* env, jclass clazz) {

    FILE* f = fopen("/proc/self/maps", "r");
    if (!f) return JNI_FALSE;

    char line[1024];
    jboolean detected = JNI_FALSE;

    while (!detected && fgets(line, (int)sizeof(line), f)) {
        for (int i = 0; FRIDA_MARKERS[i] != NULL; i++) {
            if (strstr(line, FRIDA_MARKERS[i])) {
                detected = JNI_TRUE;
                break;
            }
        }
    }

    fclose(f);
    return detected;
}
