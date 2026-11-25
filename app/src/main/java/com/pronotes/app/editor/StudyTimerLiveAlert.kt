package com.pronotes.app.editor

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pronotes.app.MainActivity
import com.pronotes.app.R

/**
 * Posts and updates a Live Alert / Live Update notification (Android 16+ Promoted Ongoing,
 * Samsung Now Bar, and ongoing chronometer chip on earlier versions) while the Study Timer runs.
 */
internal object StudyTimerLiveAlert {
    private const val CHANNEL_ID = "study_timer_live"
    private const val NOTIF_ID = 4207

    fun isSupportedAndAllowed(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return true
    }

    fun startOrUpdate(
        context: Context,
        notebookTitle: String,
        pomoMode: Boolean,
        sessionMs: Long,
        pomoRemainingMs: Long,
    ) {
        if (!isSupportedAndAllowed(context)) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureChannel(nm)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val now = System.currentTimeMillis()
        val baseWhen = if (pomoMode) now + pomoRemainingMs.coerceAtLeast(0L) else now - sessionMs.coerceAtLeast(0L)

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (pomoMode) "Pomodoro Focus" else "Study Session")
            .setContentText(notebookTitle.ifBlank { "Focused Study" })
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(pomoMode)
            .setWhen(baseWhen)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentIntent(pi)

        if (Build.VERSION.SDK_INT >= 36) {
            builder.addExtras(android.os.Bundle().apply { putBoolean("android.requestPromotedOngoing", true) })
            builder.setShortCriticalText(if (pomoMode) "Focus" else "Study")
        }

        runCatching { nm.notify(NOTIF_ID, builder.build()) }
    }

    fun stop(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching { nm.cancel(NOTIF_ID) }
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = NotificationChannel(
            CHANNEL_ID,
            "Study Timer Live Alerts",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Live ticking study timer & Pomodoro alerts"
            setShowBadge(false)
        }
        nm.createNotificationChannel(ch)
    }
}
