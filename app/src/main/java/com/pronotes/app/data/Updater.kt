package com.pronotes.app.data

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * In-app updates from public GitHub releases: checks the latest release,
 * downloads its APK into cache, and commits a PackageInstaller session.
 */
object Updater {
    private const val API_LATEST = "https://api.github.com/repos/ItzNikk/ProNotes/releases/latest"
    private const val API_LIST = "https://api.github.com/repos/ItzNikk/ProNotes/releases?per_page=10"
    private const val USER_AGENT = "ProNotes-Android"
    const val ACTION_INSTALL_RESULT = "com.pronotes.app.INSTALL_RESULT"
    const val NOTIF_READY_TO_INSTALL = 4203

    data class Release(val tag: String, val apkUrl: String, val notes: String, val size: Long)

    enum class Phase { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, READY, INSTALLING, FAILED }

    // Shared Compose UI state so Library, Settings, and UpdateDialog stay in sync.
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
    var remindedThisLaunch = false
    /** Installer confirm screen stored until an Activity can open it directly. */
    var pendingConfirm by mutableStateOf<Intent?>(null)

    private var apk: File? = null
    private var waitingForPermission = false
    private var checkedThisLaunch = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun startDownload(context: Context) {
        if (phase == Phase.DOWNLOADING || phase == Phase.INSTALLING) return
        val app = context.applicationContext
        scope.launch { download(app) }
    }

    private fun prefs(c: Context) = c.getSharedPreferences("updater", Context.MODE_PRIVATE)
    fun autoOpen(c: Context) = prefs(c).getBoolean("autoOpen", false)
    fun setAutoOpen(c: Context, on: Boolean) { prefs(c).edit().putBoolean("autoOpen", on).apply() }

    /** Whether an update started inside this app installed within the last half hour. */
    fun consumeStartedFlag(c: Context): Boolean {
        val at = prefs(c).getLong("startedAt", 0L)
        prefs(c).edit().remove("startedAt").apply()
        return at > 0 && System.currentTimeMillis() - at < 30 * 60 * 1000L
    }

    fun currentVersion(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "0"

    /** "v9.7.1" > "9.7.0" -> true. */
    fun isNewer(tag: String, current: String): Boolean {
        fun parts(s: String): List<Int> {
            val core = s.trim().trimStart('v', 'V').substringBefore('-').substringBefore('+')
            return core.split('.').mapNotNull { it.toIntOrNull() }
        }
        val a = parts(tag)
        val b = parts(current)
        if (a.isEmpty()) return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    fun hasReadyApk(context: Context): Boolean {
        val f = apk ?: File(context.cacheDir, "updates/ProNotes.apk")
        return f.exists() && f.length() > 100_000L
    }

    suspend fun check(context: Context, quiet: Boolean = false) {
        if (quiet && checkedThisLaunch) return
        checkedThisLaunch = true
        if (phase == Phase.DOWNLOADING || phase == Phase.INSTALLING) return
        phase = Phase.CHECKING
        error = null
        val r = withContext(Dispatchers.IO) { runCatching { fetchLatest() } }
        r.onSuccess { rel ->
            release = rel
            phase = if (rel != null && isNewer(rel.tag, currentVersion(context))) Phase.AVAILABLE else Phase.UP_TO_DATE
        }.onFailure { err ->
            error = err.message ?: "Could not check for updates."
            phase = if (quiet) Phase.IDLE else Phase.FAILED
        }
    }

    /** Blocking; for the daily background alarm reminder. */
    fun latestBlocking(): Release? = runCatching { fetchLatest() }.getOrNull()

    private fun openGitHubJson(urlStr: String): Pair<Int, String> {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            code to body
        } finally {
            conn.disconnect()
        }
    }

    private fun parseRelease(obj: JsonObject): Release? {
        if (obj["draft"]?.jsonPrimitive?.booleanOrNull == true) return null
        val tag = obj.str("tag_name") ?: return null
        val assets = obj["assets"]?.jsonArray?.map { it.jsonObject }.orEmpty()
        val asset = assets.firstOrNull {
            val n = it.str("name").orEmpty()
            n.endsWith(".apk", ignoreCase = true) && !n.contains("unsigned", ignoreCase = true)
        } ?: assets.firstOrNull {
            (it.str("name") ?: "").endsWith(".apk", ignoreCase = true)
        } ?: return null
        val url = asset.str("browser_download_url") ?: return null
        return Release(
            tag = tag,
            apkUrl = url,
            notes = obj.str("body") ?: "",
            size = asset["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
        )
    }

    private fun fetchLatest(): Release? {
        val (code, body) = openGitHubJson(API_LATEST)
        if (code in 200..299) {
            return parseRelease(Json.parseToJsonElement(body).jsonObject)
        }
        // Fallback when latest endpoint returns 404 (e.g. only prereleases exist)
        val (listCode, listBody) = openGitHubJson(API_LIST)
        if (listCode !in 200..299) {
            throw IllegalStateException("GitHub check failed (HTTP $listCode)")
        }
        val arr: JsonArray = Json.parseToJsonElement(listBody).jsonArray
        return arr.asSequence().mapNotNull { parseRelease(it.jsonObject) }.firstOrNull()
    }

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.content

    suspend fun download(context: Context) {
        val rel = release ?: return
        val app = context.applicationContext
        phase = Phase.DOWNLOADING
        progress = 0f
        error = null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(app.cacheDir, "updates").apply { mkdirs() }
                val part = File(dir, "ProNotes.apk.part")
                val out = File(dir, "ProNotes.apk")
                part.delete()
                var url = URL(rel.apkUrl)
                var conn: HttpURLConnection
                var hops = 0
                while (true) {
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = false
                        connectTimeout = 15000
                        readTimeout = 30000
                        setRequestProperty("User-Agent", USER_AGENT)
                    }
                    val code = conn.responseCode
                    if (code in 300..399 && hops++ < 8) {
                        val next = conn.getHeaderField("Location") ?: throw IllegalStateException("Redirect missing Location")
                        url = URL(url, next)
                        conn.disconnect()
                        continue
                    }
                    if (code !in 200..299) {
                        conn.disconnect()
                        throw IllegalStateException("Download failed (HTTP $code)")
                    }
                    break
                }
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: rel.size
                try {
                    conn.inputStream.use { input ->
                        part.outputStream().use { o ->
                            val buf = ByteArray(64 * 1024)
                            var done = 0L
                            var lastUiMs = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                o.write(buf, 0, n)
                                done += n
                                if (total > 0) {
                                    val now = android.os.SystemClock.uptimeMillis()
                                    if (now - lastUiMs >= 80L || done >= total) {
                                        lastUiMs = now
                                        val pct = (done.toFloat() / total).coerceIn(0f, 1f)
                                        withContext(Dispatchers.Main) { progress = pct }
                                    }
                                }
                            }
                            o.flush()
                        }
                    }
                } finally {
                    conn.disconnect()
                }
                if (!part.exists() || part.length() < 100_000L) {
                    throw IllegalStateException("Downloaded update file was incomplete.")
                }
                if (total > 0L && part.length() < total) {
                    throw IllegalStateException("Download interrupted before finishing.")
                }
                out.delete()
                if (!part.renameTo(out)) {
                    part.copyTo(out, overwrite = true)
                    part.delete()
                }
                out
            }
        }
        result.onSuccess {
            apk = it
            progress = 1f
            phase = Phase.READY
            install(context)
        }.onFailure { err ->
            error = err.message ?: "Download failed."
            phase = Phase.FAILED
        }
    }

    /** Called by MainActivity.onResume or UpdateDialogHost when the app is visible. */
    fun resumeIfWaiting(context: Context) {
        pendingConfirm?.let { confirm ->
            if (launchConfirm(context, confirm)) return
        }
        if (!waitingForPermission) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()) {
            waitingForPermission = false
            install(context)
        }
    }

    /** Launches the OS PackageInstaller confirmation activity from a foreground Activity if possible. */
    fun launchConfirm(context: Context, confirm: Intent): Boolean {
        val act = context.findActivity() ?: return false
        pendingConfirm = null
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(NOTIF_READY_TO_INSTALL)
        return runCatching {
            act.startActivity(Intent(confirm))
            true
        }.getOrElse {
            runCatching {
                context.applicationContext.startActivity(Intent(confirm).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }.getOrDefault(false)
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun reportInstallSuccess() {
        pendingConfirm = null
        waitingForPermission = false
        phase = Phase.UP_TO_DATE
        error = null
    }

    fun reportInstallFailure(message: String?) {
        pendingConfirm = null
        error = message ?: "The installer stopped."
        phase = Phase.FAILED
    }

    /**
     * Writes the downloaded APK into a PackageInstaller session off the main thread and commits it.
     * Results arrive via [com.pronotes.app.InstallResultReceiver].
     */
    fun install(context: Context) {
        val app = context.applicationContext
        val file = apk?.takeIf { it.exists() && it.length() > 100_000L }
            ?: File(app.cacheDir, "updates/ProNotes.apk").takeIf { it.exists() && it.length() > 100_000L }
        if (file == null) {
            reportInstallFailure("The update file is missing. Download it again.")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !app.packageManager.canRequestPackageInstalls()) {
            waitingForPermission = true
            val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
            val act = context.findActivity()
            if (act != null) {
                act.startActivity(permIntent)
            } else {
                app.startActivity(permIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            return
        }
        phase = Phase.INSTALLING
        error = null
        scope.launch {
            val committed = withContext(Dispatchers.IO) {
                runCatching {
                    val installer = app.packageManager.packageInstaller
                    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                        setAppPackageName(app.packageName)
                        setSize(file.length())
                        if (Build.VERSION.SDK_INT >= 31) {
                            setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                        }
                    }
                    val id = installer.createSession(params)
                    installer.openSession(id).use { session ->
                        file.inputStream().use { input ->
                            session.openWrite("ProNotes.apk", 0, file.length()).use { out ->
                                input.copyTo(out)
                                session.fsync(out)
                            }
                        }
                        prefs(app).edit().putLong("startedAt", System.currentTimeMillis()).apply()
                        val intent = Intent(app, com.pronotes.app.InstallResultReceiver::class.java)
                            .setAction(ACTION_INSTALL_RESULT)
                        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                            (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                        val sender = PendingIntent.getBroadcast(app, id, intent, flags).intentSender
                        session.commit(sender)
                    }
                }
            }
            committed.onFailure {
                openWithViewer(context, file)
            }
        }
    }

    /** Fallback: hand the APK to the system package installer activity via FileProvider. */
    private fun openWithViewer(context: Context, file: File) {
        val app = context.applicationContext
        runCatching {
            val uri = FileProvider.getUriForFile(app, app.packageName + ".files", file)
            val view = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val act = context.findActivity()
            if (act != null) {
                act.startActivity(view)
            } else {
                app.startActivity(view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            phase = Phase.READY
        }.onFailure {
            reportInstallFailure(it.message ?: "Could not open package installer.")
        }
    }
}
