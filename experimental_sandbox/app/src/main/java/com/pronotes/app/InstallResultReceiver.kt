package com.pronotes.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pronotes.app.data.Updater

/** Receives the system installer's answer: queues confirmation cleanly, or reports why it failed. */
class InstallResultReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirm != null) {
                    // Do NOT clear pendingConfirm blindly from a BroadcastReceiver (Android 10+ BAL drops
                    // background startActivity calls without throwing). Foreground Activity observes pendingConfirm
                    // via UpdateDialogHost/onResume; also post a notification as a tap backup.
                    Updater.pendingConfirm = confirm
                    postConfirmNotificationIfAllowed(context, confirm)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> Updater.reportInstallSuccess()
            PackageInstaller.STATUS_FAILURE_ABORTED ->
                Updater.reportInstallFailure("Install cancelled. Tap Install update to try again.")
            PackageInstaller.STATUS_FAILURE_BLOCKED ->
                Updater.reportInstallFailure("Install was blocked by the system.")
            PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                Updater.reportInstallFailure("This build was signed with a different key. Uninstall once, then install the new version.")
            PackageInstaller.STATUS_FAILURE_INVALID ->
                Updater.reportInstallFailure("The downloaded APK was invalid. Tap Retry to download again.", deleteCachedApk = true, context = context.applicationContext)
            PackageInstaller.STATUS_FAILURE_STORAGE ->
                Updater.reportInstallFailure("Not enough storage to install.")
            else -> Updater.reportInstallFailure(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
        }
    }

    @SuppressLint("MissingPermission")
    private fun postConfirmNotificationIfAllowed(context: Context, confirm: Intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("updates", "Updates", NotificationManager.IMPORTANCE_HIGH))
        }
        val pi = PendingIntent.getActivity(
            context,
            Updater.NOTIF_READY_TO_INSTALL,
            Intent(confirm).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        nm.notify(
            Updater.NOTIF_READY_TO_INSTALL,
            NotificationCompat.Builder(context, "updates")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Pro Notes update ready")
                .setContentText("Tap to confirm installation")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build(),
        )
    }
}
