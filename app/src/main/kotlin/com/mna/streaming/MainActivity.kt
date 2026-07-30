package com.mna.streaming

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mna.streaming.navigation.MANavHost
import com.mna.streaming.ui.security.TamperDetectedScreen
import com.mna.streaming.ui.splash.LaunchSplash
import com.mna.streaming.ui.theme.MATheme
import com.mna.streaming.ui.update.ForceUpdateDialog
import com.mna.streaming.ui.update.OptionalUpdateDialog
import com.mna.streaming.ui.update.UpdateState
import com.mna.streaming.ui.update.UpdateViewModel

class MainActivity : ComponentActivity() {

    companion object {
        /** Intent extra key — FCM notification content type ("movie" | "anime" | "series"). */
        const val EXTRA_CONTENT_TYPE = "contentType"
        /** Intent extra key — MongoDB content document ID. */
        const val EXTRA_CONTENT_ID   = "contentId"
    }

    /**
     * Launcher for the POST_NOTIFICATIONS runtime permission (Android 13+).
     * The result is intentionally not acted upon beyond what the OS provides —
     * if the user denies we respect that silently.
     */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — no additional action needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract any notification deep-link extras that launched this Activity.
        // These are set by SarrowsMessagingService when the user taps a notification.
        val notifContentType = intent.getStringExtra(EXTRA_CONTENT_TYPE)
        val notifContentId   = intent.getStringExtra(EXTRA_CONTENT_ID)
        val pendingDeepLink  =
            if (notifContentType != null && notifContentId != null)
                notifContentType to notifContentId
            else
                null

        setContent {
            MATheme {
                // ── Integrity gate ────────────────────────────────────────
                //
                // MAApplication.isTampered is set synchronously in
                // Application.onCreate() before this Activity is created.
                // If the APK has been modified or re-signed, we show the
                // blocking TamperDetectedScreen and nothing else.  The
                // normal app UI — nav graph, update dialogs, splash — is
                // never rendered in a tampered build.
                if (MAApplication.isTampered) {
                    TamperDetectedScreen()
                    return@MATheme
                }

                // ── Normal app flow ───────────────────────────────────────
                val updateViewModel: UpdateViewModel = viewModel()
                val updateState by updateViewModel.state.collectAsState()

                var showLaunchSplash by rememberSaveable { mutableStateOf(true) }

                if (showLaunchSplash) {
                    LaunchSplash(
                        onFinished = {
                            showLaunchSplash = false
                            // Trigger update check as soon as the custom splash finishes.
                            updateViewModel.checkForUpdate()
                            // Request POST_NOTIFICATIONS permission on Android 13+.
                            // We ask once here, right after the user has seen the
                            // app's purpose, which is the recommended UX moment.
                            requestNotificationPermissionIfNeeded()
                        }
                    )
                } else {
                    MANavHost(pendingDeepLink = pendingDeepLink)

                    // Overlay update dialogs on top of the nav host so they don't
                    // interfere with any existing navigation state.
                    when (val state = updateState) {
                        is UpdateState.ForceUpdate -> {
                            ForceUpdateDialog(
                                response = state.response,
                                reason   = state.reason
                            )
                        }
                        is UpdateState.OptionalUpdate -> {
                            OptionalUpdateDialog(
                                response  = state.response,
                                onDismiss = { updateViewModel.dismissOptionalUpdate() }
                            )
                        }
                        else -> { /* Idle / NoUpdate — show nothing */ }
                    }
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
