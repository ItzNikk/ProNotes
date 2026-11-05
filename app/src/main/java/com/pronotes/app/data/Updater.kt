package com.pronotes.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * In-app updates from the public GitHub releases: check the latest release,
 * download its APK into the app cache and hand it to the system installer.
 */
object Updater {
    private const val API = "https://api.github.com/repos/ItzNikk/ProNotes/releases/latest"

    data class Release(val tag: String, val apkUrl: String, val notes: String, val size: Long)

    enum class Phase { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, READY, FAILED }

    // Shared UI state, so the library banner and the settings row agree.
    var phase by mutableStateOf(Phase.IDLE)
        private set
    var release by mutableStateOf<Release?>(null)
        private set
    var progress by mutableStateOf(0f)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    /** True while the "what's new" dialog is open. */
    var showDialog by mutableStateOf(false)
    private var apk: File? = null
    /** Lives as long as the app, so closing the dialog or leaving a screen never cancels a download. */
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Main)
    /** The installer's confirm screen, kept in case it could not open while the app was in the background. */
    var pendingConfirm: Intent? = null

    fun startDownload(context: Context) {
        if (phase == Phase.DOWNLOADING) return
        val app = context.applicationContext
        scope.launch { download(app) }
    }

    private fun prefs(c: Context) = c.getSharedPreferences("updater", Context.MODE_PRIVATE)
    fun autoOpen(c: Context) = prefs(c).getBoolean("autoOpen", false)
    fun setAutoOpen(c: Context, on: Boolean) { prefs(c).edit().putBoolean("autoOpen", on).apply() }
    /** Whether an update that this app started was installed within the last half hour. */
    fun consumeStartedFlag(c: Context): Boolean {
        val at = prefs(c).getLong("startedAt", 0L)
        prefs(c).edit().remove("startedAt").apply()
        return at > 0 && System.currentTimeMillis() - at < 30 * 60 * 1000L
    }
    private var checkedThisLaunch = false

    fun currentVersion(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "0"

    /** "v3.1.0" > "3.0.12" ? Compares the numeric parts. */
    fun isNewer(tag: String, current: String): Boolean {
        fun parts(s: String) = s.trim().removePrefix("v").split('.', '-').mapNotNull { it.toIntOrNull() }
        val a = parts(tag); val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    suspend fun check(context: Context, quiet: Boolean = false) {
        if (quiet && checkedThisLaunch) return
        checkedThisLaunch = true
        if (phase == Phase.DOWNLOADING) return
        phase = Phase.CHECKING; error = null
        val r = withContext(Dispatchers.IO) { runCatching { fetchLatest() } }
        r.onSuccess { rel ->
            release = rel
            phase = if (rel != null && isNewer(rel.tag, currentVersion(context))) Phase.AVAILABLE else Phase.UP_TO_DATE
        }.onFailure {
            error = it.message
            phase = if (quiet) Phase.IDLE else Phase.FAILED
        }
    }

    private fun fetchLatest(): Release? {
        val conn = (URL(API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000; readTimeout = 15000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        conn.inputStream.use { input ->
            val obj = Json.parseToJsonElement(input.readBytes().decodeToString()).jsonObject
            val asset = obj["assets"]?.jsonArray?.map { it.jsonObject }?.firstOrNull { (it.str("name") ?: "").endsWith(".apk") } ?: return null
            return Release(
                tag = obj.str("tag_name") ?: return null,
                apkUrl = asset.str("browser_download_url") ?: return null,
                notes = obj.str("body") ?: "",
                size = asset["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
            )
        }
    }

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.content

    suspend fun download(context: Context) {
        val rel = release ?: return
        phase = Phase.DOWNLOADING; progress = 0f; error = null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                val out = File(dir, "ProNotes.apk")
                var url = URL(rel.apkUrl)
                var conn: HttpURLConnection
                var hops = 0
                // GitHub redirects release downloads to its file host.
                while (true) {
                    conn = (url.openConnection() as HttpURLConnection).apply { instanceFollowRedirects = false; connectTimeout = 15000; readTimeout = 30000 }
                    val code = conn.responseCode
                    if (code in 300..399 && hops++ < 5) { url = URL(conn.getHeaderField("Location")); conn.disconnect(); continue }
                    if (code !in 200..299) error("Download failed ($code)")
                    break
                }
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: rel.size
                conn.inputStream.use { input ->
                    out.outputStream().use { o ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            o.write(buf, 0, n); done += n
                            if (total > 0) progress = (done.toFloat() / total).coerceIn(0f, 1f)
                        }
                    }
                }
                out
            }
        }
        result.onSuccess { apk = it; phase = Phase.READY; install(context) }
            .onFailure { error = it.message; phase = Phase.FAILED }
    }

    const val ACTION_INSTALL_RESULT = "com.pronotes.app.INSTALL_RESULT"
    private var waitingForPermission = false

    /** Called when the app comes back to the front: finish an install that was waiting for the permission. */
    fun resumeIfWaiting(context: Context) {
        pendingConfirm?.let { confirm ->
            pendingConfirm = null
            runCatching { context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            return
        }
        if (!waitingForPermission) return
        if (Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()) {
            waitingForPermission = false
            install(context)
        }
    }

    fun reportInstallFailure(message: String?) {
        error = message ?: "The installer stopped."
        phase = Phase.FAILED
    }

    /**
     * Hands the downloaded APK to the system through a PackageInstaller session. Unlike a plain
     * "open file" intent, this reports back why an install failed (for example a different signing key).
     */
    fun install(context: Context) {
        val file = apk ?: File(context.cacheDir, "updates/ProNotes.apk").takeIf { it.exists() }
        if (file == null) { reportInstallFailure("The update file is gone. Download it again."); return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            waitingForPermission = true
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        runCatching {
            val installer = context.packageManager.packageInstaller
            val params = android.content.pm.PackageInstaller.SessionParams(android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            params.setAppPackageName(context.packageName)
            // Android 12+: an app updating itself may skip the confirm screen once it has been allowed.
            if (Build.VERSION.SDK_INT >= 31) params.setRequireUserAction(android.content.pm.PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            val id = installer.createSession(params)
            installer.openSession(id).use { session ->
                file.inputStream().use { input ->
                    session.openWrite("ProNotes.apk", 0, file.length()).use { out ->
                        input.copyTo(out)
                        session.fsync(out)
                    }
                }
                prefs(context).edit().putLong("startedAt", System.currentTimeMillis()).apply()
                val intent = Intent(context, com.pronotes.app.InstallResultReceiver::class.java).setAction(ACTION_INSTALL_RESULT)
                val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 31) android.app.PendingIntent.FLAG_MUTABLE else 0)
                session.commit(android.app.PendingIntent.getBroadcast(context, id, intent, flags).intentSender)
            }
        }.onFailure { openWithViewer(context, file) }
    }

    /** Fallback: hand the file to the system package viewer. */
    private fun openWithViewer(context: Context, file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
            context.startActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { reportInstallFailure(it.message) }
    }
}
