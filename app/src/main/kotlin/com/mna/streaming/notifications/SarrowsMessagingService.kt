package com.mna.streaming.notifications

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mna.streaming.MAApplication
import com.mna.streaming.MainActivity
import com.mna.streaming.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * FCM message handler for the Sarrows app.
 *
 * Responsibilities:
 *  1. [onNewToken]       — re-registers the refreshed FCM token with the server.
 *  2. [onMessageReceived] — builds and displays a rich local notification for
 *                           new-content messages; tapping opens the correct detail screen.
 *
 * All network and image-download work runs on an IO dispatcher so the service
 * never blocks the main thread.
 *
 * The service scope uses [SupervisorJob] so a failed registration doesn't cancel
 * an in-flight notification display, and vice-versa.
 */
class SarrowsMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Token refresh ─────────────────────────────────────────────────────────

    /**
     * Called by Firebase when the registration token is refreshed.
     * Re-registers with the Sarrows server so future notifications are delivered.
     * Silently ignored if the user is not logged in (no saved Bearer token).
     */
    override fun onNewToken(token: String) {
        serviceScope.launch {
            runCatching {
                MAApplication.authRepository.registerDeviceToken(token)
            }
        }
    }

    // ── Message handling ──────────────────────────────────────────────────────

    /**
     * Called for every FCM message while the app is in the foreground, and for
     * data-only messages in the background/killed state.
     *
     * Expected data keys (sent by the Sarrows backend):
     *   action          — always "open_content"
     *   contentType     — "movie" | "anime" | "series"
     *   contentId       — MongoDB document ID
     *   contentTitle    — human-readable title
     *   contentImageUrl — poster / banner URL
     *   appLogoUrl      — Sarrows logo URL (unused here; icon is the app resource)
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data

        // Both pieces of data required to deep-link on tap — ignore anything else
        val contentType = data["contentType"]?.takeIf { it.isNotBlank() } ?: return
        val contentId   = data["contentId"]?.takeIf   { it.isNotBlank() } ?: return

        val notifTitle   = message.notification?.title
            ?: "New ${contentType.replaceFirstChar { it.uppercase() }} on Sarrows"
        val notifBody    = message.notification?.body
            ?: data["contentTitle"]
            ?: ""
        val imageUrl     = data["contentImageUrl"]

        serviceScope.launch {
            showNotification(
                contentType = contentType,
                contentId   = contentId,
                title       = notifTitle,
                body        = notifBody,
                imageUrl    = imageUrl
            )
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun showNotification(
        contentType: String,
        contentId:   String,
        title:       String,
        body:        String,
        imageUrl:    String?
    ) {
        // Download the content image on the current IO thread before posting
        val bitmap = NotificationHelper.downloadBitmap(imageUrl)

        // Tap intent → open MainActivity with deep-link extras
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_CONTENT_TYPE, contentType)
            putExtra(MainActivity.EXTRA_CONTENT_ID,   contentId)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            /* requestCode — unique per content item so tapping different notifications
               each opens the correct screen even when several are stacked. */
            contentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (bitmap != null) {
            // Show the content image in both the collapsed and expanded notification
            builder
                .setLargeIcon(bitmap)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        // Collapse the large icon in the expanded view so only the
                        // full-width picture is shown (standard pattern)
                        .bigLargeIcon(null as android.graphics.Bitmap?)
                )
        } else {
            // Fallback: show the text in expanded form when there is no image
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        // POST_NOTIFICATIONS permission is enforced by the OS on Android 13+;
        // the call is a no-op (not an exception) if it is denied, so no try/catch needed.
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(contentId.hashCode(), builder.build())
        }
    }
}
