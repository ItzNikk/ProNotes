package com.pronotes.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.pronotes.app.data.Updater

/** Receives the system installer's answer: shows its confirm screen, or reports why it failed. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                if (confirm != null) {
                    // Kept until it opens; if Android blocks it from here, the app opens it on return.
                    Updater.pendingConfirm = confirm
                    if (runCatching { context.startActivity(Intent(confirm).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) Updater.pendingConfirm = null
                }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            PackageInstaller.STATUS_FAILURE_ABORTED -> Updater.reportInstallFailure("Install cancelled. Tap Install update to try again.")
            PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                Updater.reportInstallFailure("This build was signed with a different key. Uninstall once, then install the new version.")
            PackageInstaller.STATUS_FAILURE_STORAGE -> Updater.reportInstallFailure("Not enough storage to install.")
            else -> Updater.reportInstallFailure(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
        }
    }
}
