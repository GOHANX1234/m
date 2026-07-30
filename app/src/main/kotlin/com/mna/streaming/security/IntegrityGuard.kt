package com.mna.streaming.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.mna.streaming.BuildConfig
import java.security.MessageDigest

/**
 * APK integrity and anti-tamper guard.
 *
 * [runAllChecks] returns an [IntegrityResult] that is either [IntegrityResult.Pass]
 * or [IntegrityResult.Fail] with a numeric code identifying which check failed.
 * The code is shown on [TamperDetectedScreen] so issues can be diagnosed without
 * needing a connected computer or logcat.
 *
 * Error codes
 * -----------
 *  1 — native `integrity` library failed to load (missing .so / ABI mismatch)
 *  2 — signing certificate could not be read from PackageManager
 *  3 — signing certificate fingerprint does not match (re-signed APK)
 *  4 — debugger attached (release builds only)
 *  5 — Frida / injection detected (release builds only)
 *
 * ### JNI visibility
 * The native external functions must NOT be `private`.  Kotlin does not generate
 * a proper static JNI bridge for `@JvmStatic private external fun` in an object
 * declaration, which causes the JNI linker to fail at call-time.  ProGuard's
 * `-keep` rule on the enclosing class already prevents stripping and renaming.
 */
object IntegrityGuard {

    /** Sealed result type so callers can pattern-match on Pass vs Fail. */
    sealed class IntegrityResult {
        object Pass : IntegrityResult()
        data class Fail(val code: Int) : IntegrityResult()
    }

    /** True when the `integrity` native library loaded without error. */
    private val libraryLoaded: Boolean = runCatching {
        System.loadLibrary("integrity")
    }.isSuccess

    // ── Native JNI declarations ────────────────────────────────────────────
    // Must NOT be private — see class-level KDoc.

    @JvmStatic external fun nativeVerifySignature(hexFingerprint: String): Boolean
    @JvmStatic external fun nativeCheckDebugger(): Boolean
    @JvmStatic external fun nativeCheckFrida(): Boolean

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Runs all integrity checks and returns the result.
     * Call this once from [android.app.Application.onCreate] before any other init.
     */
    fun runAllChecks(context: Context): IntegrityResult {

        // Code 1 — native library could not be loaded
        if (!libraryLoaded) return IntegrityResult.Fail(1)

        // Code 2 — could not read signing cert from PackageManager
        val fingerprint = getSigningFingerprint(context)
            ?: return IntegrityResult.Fail(2)

        // Code 3 — fingerprint mismatch (re-signed or tampered APK)
        if (!nativeVerifySignature(fingerprint)) return IntegrityResult.Fail(3)

        // Codes 4 & 5 — analysis-tool checks (release builds only)
        if (!BuildConfig.DEBUG) {
            if (nativeCheckDebugger()) return IntegrityResult.Fail(4)
            if (nativeCheckFrida())   return IntegrityResult.Fail(5)
        }

        return IntegrityResult.Pass
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Returns the lowercase hex SHA-256 of the APK's first signing certificate,
     * or null on any error.
     *
     * API ≥ 28: uses GET_SIGNING_CERTIFICATES / SigningInfo.
     * API 26–27: falls back to deprecated GET_SIGNATURES.
     *
     * Multiple signers (legacy v1 multi-signer) returns null because M&A is
     * always single-signer.
     */
    private fun getSigningFingerprint(context: Context): String? {
        return try {
            val pm = context.packageManager
            val certBytes: ByteArray? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val info = pm.getPackageInfo(
                        context.packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES
                    )
                    val si = info.signingInfo
                    if (si?.hasMultipleSigners() == true) null
                    else si?.apkContentsSigners?.firstOrNull()?.toByteArray()
                } else {
                    @Suppress("DEPRECATION")
                    val info = pm.getPackageInfo(
                        context.packageName,
                        PackageManager.GET_SIGNATURES
                    )
                    @Suppress("DEPRECATION")
                    info.signatures?.firstOrNull()?.toByteArray()
                }
            certBytes?.let { sha256Hex(it) }
        } catch (_: Exception) {
            null
        }
    }

    /** SHA-256 of [data] as a 64-char lowercase hex string. */
    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return buildString(64) { digest.forEach { b -> append("%02x".format(b)) } }
    }
}
