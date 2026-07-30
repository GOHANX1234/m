package com.mna.streaming.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.URL

/**
 * Utility for creating the Sarrows notification channel and downloading images
 * for use in BigPictureStyle notifications.
 *
 * The channel must be created before any notification is posted.
 * [createChannel] is idempotent — calling it multiple times is safe.
 */
object NotificationHelper {

    /** Must match the channel ID sent by the Sarrows backend. */
    const val CHANNEL_ID   = "sarrows_updates"
    const val CHANNEL_NAME = "Sarrows Updates"
    const val CHANNEL_DESC = "New movies, anime, and web series on Sarrows"

    /**
     * Creates the [CHANNEL_ID] notification channel.
     * Safe to call on every app start; Android is a no-op if the channel already exists.
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESC
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /**
     * Synchronously downloads an image from [imageUrl] and returns it as a [Bitmap],
     * or null on any error (network failure, null/blank URL, decode failure).
     *
     * Must be called from a background thread / IO dispatcher.
     */
    fun downloadBitmap(imageUrl: String?): Bitmap? {
        if (imageUrl.isNullOrBlank()) return null
        return try {
            BitmapFactory.decodeStream(URL(imageUrl).openStream())
        } catch (_: Exception) {
            null
        }
    }
}
