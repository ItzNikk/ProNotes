package com.pronotes.app.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pronotes.app.MainActivity
import com.pronotes.app.R

/**
 * Foreground service keeping APK update downloads alive reliably in the background
 * with Pause, Resume, and Cancel actions inside a persistent silent progress notification.
 */
class UpdateDownloadService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel(this)
        when (intent?.action) {
            ACTION_PAUSE -> {
                Updater.pauseDownload(this)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_ID, buildPausedNotification(this))
                if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_DETACH) else @Suppress("DEPRECATION") stopForeground(false)
                stopSelf()
            }
            ACTION_CANCEL -> {
                Updater.cancelDownload(this)
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIF_ID)
                if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
                stopSelf()
            }
            else -> {
                val notif = buildProgressNotification(
                    this,
                    Updater.release?.tag ?: "Update",
                    Updater.progress,
                    Updater.downloadedBytes,
                    Updater.totalBytes,
                    Updater.speedBytesPerSec,
                    Updater.etaSeconds,
                )
                if (Build.VERSION.SDK_INT >= 29) {
                    startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(NOTIF_ID, notif)
                }
                Updater.launchWorkerDownload(this) {
                    if (Updater.phase != Updater.Phase.DOWNLOADING) {
                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        if (Updater.phase == Updater.Phase.PAUSED) {
                            nm.notify(NOTIF_ID, buildPausedNotification(this))
                            if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_DETACH) else @Suppress("DEPRECATION") stopForeground(false)
                        } else {
                            nm.cancel(NOTIF_ID)
                            if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
                        }
                        stopSelf()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    companion object {
        const val CHANNEL_ID = "updates_progress"
        const val NOTIF_ID = 4204
        const val ACTION_START = "com.pronotes.app.UPDATE_START"
        const val ACTION_PAUSE = "com.pronotes.app.UPDATE_PAUSE"
        const val ACTION_CANCEL = "com.pronotes.app.UPDATE_CANCEL"

        fun startIntent(c: Context) = Intent(c, UpdateDownloadService::class.java).setAction(ACTION_START)
        fun pauseIntent(c: Context) = Intent(c, UpdateDownloadService::class.java).setAction(ACTION_PAUSE)
        fun cancelIntent(c: Context) = Intent(c, UpdateDownloadService::class.java).setAction(ACTION_CANCEL)

        fun stopService(c: Context) {
            (c.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(NOTIF_ID)
            runCatching { c.stopService(Intent(c, UpdateDownloadService::class.java)) }
        }

        fun ensureChannel(c: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Update downloads", NotificationManager.IMPORTANCE_LOW)
                        .apply { setShowBadge(false) },
                )
            }
        }

        fun notifyProgress(c: Context, tag: String, pct: Float, done: Long, total: Long, speedBps: Long, etaSec: Long?) {
            ensureChannel(c)
            val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            nm.notify(NOTIF_ID, buildProgressNotification(c, tag, pct, done, total, speedBps, etaSec))
        }

        private fun formatBytes(b: Long): String = when {
            b >= 1_048_576L -> "%.1f MB".format(b / 1_048_576.0)
            b >= 1024L -> "%d KB".format(b / 1024L)
            else -> "$b B"
        }

        private fun formatEta(sec: Long?): String = when {
            sec == null || sec < 0 -> ""
            sec >= 60 -> " · ${sec / 60}m ${sec % 60}s left"
            else -> " · ${sec}s left"
        }

        private fun openAppIntent(c: Context): PendingIntent {
            val i = Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            return PendingIntent.getActivity(c, 1, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        fun buildProgressNotification(
            c: Context,
            tag: String,
            pct: Float,
            done: Long,
            total: Long,
            speedBps: Long,
            etaSec: Long?,
        ): Notification {
            val pausePi = PendingIntent.getService(
                c, 2, pauseIntent(c), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val cancelPi = PendingIntent.getService(
                c, 3, cancelIntent(c), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val pctInt = (pct * 100).toInt().coerceIn(0, 100)
            val speedLabel = if (speedBps > 0) " · ${formatBytes(speedBps)}/s" else ""
            val sizeLabel = if (total > 0) "${formatBytes(done)} / ${formatBytes(total)}" else formatBytes(done)
            return NotificationCompat.Builder(c, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Downloading Pro Notes $tag")
                .setContentText("$sizeLabel$speedLabel${formatEta(etaSec)}")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, pctInt, total <= 0L)
                .setContentIntent(openAppIntent(c))
                .addAction(0, "Pause", pausePi)
                .addAction(0, "Cancel", cancelPi)
                .build()
        }

        fun buildPausedNotification(c: Context): Notification {
            val resumePi = PendingIntent.getService(
                c, 4, startIntent(c), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val cancelPi = PendingIntent.getService(
                c, 5, cancelIntent(c), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val tag = Updater.release?.tag ?: "Update"
            val pct = (Updater.progress * 100).toInt().coerceIn(0, 100)
            return NotificationCompat.Builder(c, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Update $tag paused ($pct%)")
                .setContentText("Tap Resume to continue downloading in background")
                .setOnlyAlertOnce(true)
                .setProgress(100, pct, false)
                .setContentIntent(openAppIntent(c))
                .addAction(0, "Resume", resumePi)
                .addAction(0, "Cancel", cancelPi)
                .build()
        }
    }
}
