package com.pronotes.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pronotes.app.data.Updater

/**
 * Daily background check for a new release. Posts one "update available" notification per new
 * version; tapping it opens the app, which then shows what changed.
 */
class UpdateCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val done = goAsync()
        Thread {
            try { check(app) } finally { done.finish() }
        }.start()
    }

    private fun check(context: Context) {
        val rel = Updater.latestBlocking() ?: return
        if (!Updater.isNewer(rel.tag, Updater.currentVersion(context))) return
        val prefs = context.getSharedPreferences("updater", Context.MODE_PRIVATE)
        if (prefs.getString("notifiedTag", null) == rel.tag) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("updates", "Updates", NotificationManager.IMPORTANCE_DEFAULT))
        val open = (context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(context, 1, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val firstLine = rel.notes.lineSequence().map { it.trim().trimStart('-', '*', ' ').replace("**", "") }.firstOrNull { it.isNotBlank() && !it.startsWith("#") }
        nm.notify(
            4202,
            NotificationCompat.Builder(context, "updates")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Pro Notes ${rel.tag} is ready")
                .setContentText(firstLine ?: "Tap to see what's new")
                .setStyle(NotificationCompat.BigTextStyle().bigText(firstLine ?: "Tap to see what's new"))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build(),
        )
        prefs.edit().putString("notifiedTag", rel.tag).apply()
    }

    companion object {
        /** Turns the daily check on or off. Safe to call on every launch. */
        fun schedule(context: Context, on: Boolean) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                context, 7, Intent(context, UpdateCheckReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            am.cancel(pi)
            if (on) am.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR * 3,
                AlarmManager.INTERVAL_DAY, pi,
            )
        }
    }
}
