package com.mna.streaming

import android.app.Application
import com.google.firebase.FirebaseApp
import com.mna.streaming.data.LocalProfileStore
import com.mna.streaming.data.SessionManager
import com.mna.streaming.data.repository.AdminRepository
import com.mna.streaming.data.repository.AuthRepository
import com.mna.streaming.data.repository.AnimeRepository
import com.mna.streaming.data.repository.MovieRepository
import com.mna.streaming.data.repository.UpdateRepository
import com.mna.streaming.network.ApiClient
import com.mna.streaming.notifications.NotificationHelper
import com.mna.streaming.security.IntegrityGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application class — initialises the dependency graph exactly once.
 *
 * Integrity check runs **first**, before any repository or network layer is
 * created.  If the APK has been re-signed or modified, [isTampered] is set to
 * `true` and MainActivity will show [TamperDetectedScreen] instead of the
 * normal UI.  No sensitive objects (ApiClient, SessionManager, …) are
 * constructed while [isTampered] is true.
 *
 * No DI framework is used. Components are created here and accessed through
 * the companion object by ViewModels and Activities.
 */
class MAApplication : Application() {

    companion object {
        /**
         * Set to `true` during [onCreate] if any integrity check fails.
         * Read by [MainActivity] to decide which UI to render.
         */
        var isTampered: Boolean = false
            private set

        lateinit var sessionManager: SessionManager
            private set

        lateinit var localProfileStore: LocalProfileStore
            private set

        lateinit var apiClient: ApiClient
            private set

        lateinit var authRepository: AuthRepository
            private set

        lateinit var movieRepository: MovieRepository
            private set

        lateinit var animeRepository: AnimeRepository
            private set

        lateinit var adminRepository: AdminRepository
            private set

        lateinit var updateRepository: UpdateRepository
            private set

        /**
         * Application-level coroutine scope for fire-and-forget writes that must
         * survive ViewModel lifecycle (e.g. watch-history persistence).
         * Uses SupervisorJob so a child failure never cancels the whole scope.
         */
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    override fun onCreate() {
        super.onCreate()

        // ── Firebase — must be initialised before any Firebase API is used ─────
        //
        // FirebaseApp.initializeApp reads google-services.json automatically.
        // Calling it here (before the integrity check) is safe because Firebase
        // itself does not access any Sarrows-specific secrets or network resources
        // at init time.  It is required this early so that the FCM service can
        // call FirebaseMessaging.getInstance() as soon as the process starts.
        FirebaseApp.initializeApp(this)

        // ── Notification channel ──────────────────────────────────────────────
        //
        // The channel must exist before any notification is posted.  Creating it
        // here on every startup is safe — Android is a no-op when the channel
        // already exists.
        NotificationHelper.createChannel(this)

        // ── Integrity check — must run before sensitive initialisation ────────
        //
        // IntegrityGuard verifies:
        //   1. Signing-certificate SHA-256 fingerprint (native comparison)
        //   2. Debugger presence via /proc/self/status  (release builds only)
        //   3. Frida / injection via /proc/self/maps    (release builds only)
        //
        // If any check fails we set isTampered = true and return immediately.
        // No sensitive state is initialised, so a modified APK gets nothing
        // useful from the dependency graph.
        isTampered = IntegrityGuard.runAllChecks(this) is IntegrityGuard.IntegrityResult.Fail
        if (isTampered) return

        // ── Normal dependency-graph initialisation ─────────────────────────
        sessionManager    = SessionManager(applicationContext)
        localProfileStore = LocalProfileStore(applicationContext)
        apiClient         = ApiClient(sessionManager)
        authRepository    = AuthRepository(apiClient, sessionManager, localProfileStore)
        movieRepository   = MovieRepository(apiClient, localProfileStore)
        animeRepository   = AnimeRepository(apiClient)
        adminRepository   = AdminRepository(apiClient)
        updateRepository  = UpdateRepository(apiClient)
    }
}
