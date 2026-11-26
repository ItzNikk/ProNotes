package com.pronotes.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pronotes.app.data.Updater

/**
 * Runs right after Pro Notes has been replaced by a newer version. Android does not let an app start
 * itself from the background, so: if the user allowed "Display over other apps" and opted in, we open
 * the app directly; otherwise we post a notification that opens it with one tap.
 */
class UpdatedReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        // The update is installed: the downloaded package is no longer needed.
        runCatching { java.io.File(context.cacheDir, "updates").deleteRecursively() }
        // Only react to updates that started from inside the app.
        if (!Updater.consumeStartedFlag(context)) return
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        if (Updater.autoOpen(context) && Settings.canDrawOverlays(context)) {
            if (runCatching { context.startActivity(launch) }.isSuccess) return
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("updates", "Updates", NotificationManager.IMPORTANCE_HIGH))
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val pi = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        nm.notify(
            4201,
            NotificationCompat.Builder(context, "updates")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Pro Notes updated")
                .setContentText("Tap to open")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build(),
        )
    }
}
