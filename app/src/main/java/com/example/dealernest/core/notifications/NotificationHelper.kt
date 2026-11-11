package com.example.dealernest.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Push notifications themselves are blocked pending a Firebase project (`google-services.json`)
 * - see .claude/docs/roadmap.md Phase 6. This is the part that doesn't need Firebase: the
 * notification channel and (on Android 13+) the runtime permission, so wiring in
 * FirebaseMessagingService later is the only remaining step.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_UPDATES,
            "Order & Collection Updates",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Order, collection and vendor updates"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_UPDATES = "updates"
    }
}
