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
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resumable background-capable in-app updates from public GitHub releases:
 * supports HTTP Range resume (.part + metadata verification), Pause/Resume/Cancel,
 * live throughput & ETA, ForegroundService background execution, and PackageInstaller commit.
 */
object Updater {
    private const val API_LATEST = "https://api.github.com/repos/ItzNikk/ProNotes/releases/latest"
    private const val API_LIST = "https://api.github.com/repos/ItzNikk/ProNotes/releases?per_page=10"
    private const val USER_AGENT = "ProNotes-Android"
    const val ACTION_INSTALL_RESULT = "com.pronotes.app.INSTALL_RESULT"
    const val NOTIF_READY_TO_INSTALL = 4203

    data class Release(val tag: String, val apkUrl: String, val notes: String, val size: Long)

    enum class Phase { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, PAUSED, READY, INSTALLING, FAILED }

    var phase by mutableStateOf(Phase.IDLE)
        private set
    var release by mutableStateOf<Release?>(null)
        private set
    var progress by mutableStateOf(0f)
        private set
    var downloadedBytes by mutableStateOf(0L)
        private set
    var totalBytes by mutableStateOf(0L)
        private set
    var speedBytesPerSec by mutableStateOf(0L)
        private set
    var etaSeconds by mutableStateOf<Long?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var showDialog by mutableStateOf(false)
    var remindedThisLaunch = false
    var pendingConfirm by mutableStateOf<Intent?>(null)

    private var apk: File? = null
    private var waitingForPermission = false
    private var checkedThisLaunch = false
    private var downloadJob: Job? = null
    @Volatile private var controlSignal: ControlSignal? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private enum class ControlSignal { PAUSE, CANCEL }

    private fun updateDir(context: Context) = File(context.cacheDir, "updates").apply { mkdirs() }
    private fun partFile(context: Context) = File(updateDir(context), "ProNotes.apk.part")
    private fun metaFile(context: Context) = File(updateDir(context), "ProNotes.apk.meta")
    private fun finalApkFile(context: Context) = File(updateDir(context), "ProNotes.apk")

    fun startDownload(context: Context) {
        if (phase == Phase.DOWNLOADING || phase == Phase.INSTALLING) return
        val rel = release ?: return
        val app = context.applicationContext
        controlSignal = null
        prefs(app).edit()
            .putString("pendingTag", rel.tag)
            .putString("pendingUrl", rel.apkUrl)
            .putString("pendingNotes", rel.notes)
            .putLong("pendingSize", rel.size)
            .apply()
        runCatching {
            ContextCompat.startForegroundService(app, UpdateDownloadService.startIntent(app))
        }.onFailure {
            launchWorkerDownload(app)
        }
    }

    fun launchWorkerDownload(context: Context, onFinished: (() -> Unit)? = null) {
        val app = context.applicationContext
        if (downloadJob?.isActive == true) return
        if (release == null) {
            val p = prefs(app)
            val tag = p.getString("pendingTag", null)
            val url = p.getString("pendingUrl", null)
            if (!tag.isNullOrBlank() && !url.isNullOrBlank()) {
                release = Release(
                    tag = tag,
                    apkUrl = url,
                    notes = p.getString("pendingNotes", "").orEmpty(),
                    size = p.getLong("pendingSize", 0L),
                )
            }
        }
        downloadJob = scope.launch {
            try {
                download(app)
            } finally {
                onFinished?.invoke()
            }
        }
    }

    fun pauseDownload(context: Context) {
        if (phase != Phase.DOWNLOADING) return
        controlSignal = ControlSignal.PAUSE
        speedBytesPerSec = 0L
        etaSeconds = null
        phase = Phase.PAUSED
        runCatching { context.applicationContext.startService(UpdateDownloadService.pauseIntent(context.applicationContext)) }
    }

    fun resumeDownload(context: Context) {
        if (phase == Phase.DOWNLOADING || phase == Phase.INSTALLING) return
        startDownload(context)
    }

    fun cancelDownload(context: Context) {
        val app = context.applicationContext
        controlSignal = ControlSignal.CANCEL
        downloadJob?.cancel()
        downloadJob = null
        partFile(app).delete()
        metaFile(app).delete()
        downloadedBytes = 0L
        totalBytes = release?.size ?: 0L
        progress = 0f
        speedBytesPerSec = 0L
        etaSeconds = null
        error = null
        phase = if (release != null) Phase.AVAILABLE else Phase.IDLE
        runCatching { app.startService(UpdateDownloadService.cancelIntent(app)) }
    }

    fun hasPartialDownload(context: Context): Boolean {
        val part = partFile(context)
        return part.exists() && part.length() > 0L
    }

    fun shouldPreserveCacheOnStart(context: Context): Boolean =
        isWaitingForInstall(context) || hasPartialDownload(context) || phase == Phase.DOWNLOADING || phase == Phase.PAUSED

    private fun prefs(c: Context) = c.getSharedPreferences("updater", Context.MODE_PRIVATE)
    fun autoOpen(c: Context) = prefs(c).getBoolean("autoOpen", false)
    fun setAutoOpen(c: Context, on: Boolean) { prefs(c).edit().putBoolean("autoOpen", on).apply() }

    /** True while waiting for the user to return from "Allow from this source" (survives process restart). */
    fun isWaitingForInstall(c: Context): Boolean {
        val at = prefs(c).getLong("waitingPermAt", 0L)
        val valid = at > 0L && System.currentTimeMillis() - at < 30 * 60 * 1000L
        return (waitingForPermission || valid) && hasReadyApk(c)
    }

    /** Whether an update started inside this app installed within the last half hour. */
    fun consumeStartedFlag(c: Context): Boolean {
        val p = prefs(c)
        val at = p.getLong("startedAt", 0L)
        p.edit().remove("startedAt").remove("waitingPermAt").apply()
        return at > 0 && System.currentTimeMillis() - at < 30 * 60 * 1000L
    }

    fun currentVersion(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "0"

    /** "v9.11.0" > "9.7.1" -> true. */
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
        val f = apk ?: finalApkFile(context)
        if (!f.exists() || f.length() <= 100_000L) return false
        val targetTag = release?.tag
        val cachedTag = prefs(context).getString("apkTag", null)
        return targetTag == null || cachedTag == null || targetTag == cachedTag
    }

    suspend fun check(context: Context, quiet: Boolean = false) {
        if (quiet && checkedThisLaunch) return
        checkedThisLaunch = true
        if (phase == Phase.DOWNLOADING || phase == Phase.INSTALLING || phase == Phase.PAUSED) return
        phase = Phase.CHECKING
        error = null
        val r = withContext(Dispatchers.IO) { runCatching { fetchLatest() } }
        r.onSuccess { rel ->
            release = rel
            if (rel != null && isNewer(rel.tag, currentVersion(context))) {
                val partial = partFile(context)
                val meta = readMeta(context)
                if (partial.exists() && partial.length() > 0L && meta?.first == rel.tag) {
                    downloadedBytes = partial.length()
                    totalBytes = meta.third.takeIf { it > 0L } ?: rel.size
                    if (totalBytes > 0L) progress = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                    phase = Phase.PAUSED
                } else {
                    phase = Phase.AVAILABLE
                }
            } else {
                phase = Phase.UP_TO_DATE
            }
        }.onFailure { err ->
            error = err.message ?: "Could not check for updates."
            phase = if (quiet) Phase.IDLE else Phase.FAILED
        }
    }

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
        val (listCode, listBody) = openGitHubJson(API_LIST)
        if (listCode !in 200..299) {
            throw IllegalStateException("GitHub check failed (HTTP $listCode)")
        }
        val arr: JsonArray = Json.parseToJsonElement(listBody).jsonArray
        return arr.asSequence().mapNotNull { parseRelease(it.jsonObject) }.firstOrNull()
    }

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.content

    private fun readMeta(context: Context): Triple<String, String, Long>? {
        val mf = metaFile(context)
        if (!mf.exists()) return null
        val lines = runCatching { mf.readLines() }.getOrNull().orEmpty()
        if (lines.size < 3) return null
        return Triple(lines[0], lines[1], lines[2].toLongOrNull() ?: 0L)
    }

    private fun writeMeta(context: Context, tag: String, etag: String, total: Long) {
        runCatching { metaFile(context).writeText("$tag\n$etag\n$total") }
    }

    private sealed class DownloadOutcome {
        data class Completed(val file: File) : DownloadOutcome()
        data object Paused : DownloadOutcome()
        data object Cancelled : DownloadOutcome()
    }

    suspend fun download(context: Context) {
        val rel = release ?: return
        val app = context.applicationContext
        controlSignal = null
        phase = Phase.DOWNLOADING
        error = null

        val result = withContext(Dispatchers.IO) {
            runCatching<DownloadOutcome> {
                val part = partFile(app)
                val out = finalApkFile(app)
                val savedMeta = readMeta(app)
                // Verify cached partial matches the requested release tag
                if (savedMeta != null && savedMeta.first != rel.tag) {
                    part.delete()
                    metaFile(app).delete()
                }
                val initialOffset = if (part.exists() && part.length() > 0L) part.length() else 0L

                var url = URL(rel.apkUrl)
                var conn: HttpURLConnection
                var hops = 0
                var resumeOffset = initialOffset
                while (true) {
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = false
                        connectTimeout = 15000
                        readTimeout = 30000
                        setRequestProperty("User-Agent", USER_AGENT)
                        if (resumeOffset > 0L) {
                            setRequestProperty("Range", "bytes=$resumeOffset-")
                        }
                    }
                    val code = conn.responseCode
                    if (code in 300..399 && hops++ < 8) {
                        val next = conn.getHeaderField("Location") ?: throw IllegalStateException("Redirect missing Location")
                        url = URL(url, next)
                        conn.disconnect()
                        continue
                    }
                    if (code == 416 && resumeOffset > 0L) {
                        // Stale/oversized .part: restart cleanly from byte 0
                        conn.disconnect()
                        part.delete()
                        metaFile(app).delete()
                        resumeOffset = 0L
                        url = URL(rel.apkUrl)
                        hops = 0
                        continue
                    }
                    if (code !in 200..299 && code != HttpURLConnection.HTTP_PARTIAL) {
                        conn.disconnect()
                        throw IllegalStateException("Download failed (HTTP $code)")
                    }
                    break
                }

                val code = conn.responseCode
                val isPartial = code == HttpURLConnection.HTTP_PARTIAL
                val responseETag = conn.getHeaderField("ETag").orEmpty()
                if (isPartial && savedMeta != null && savedMeta.second.isNotBlank() && responseETag.isNotBlank() && savedMeta.second != responseETag) {
                    // Server asset changed while paused; restart cleanly
                    conn.disconnect()
                    part.delete()
                    metaFile(app).delete()
                    return@runCatching restartFreshDownload(app, rel)
                }

                val appendMode = isPartial && resumeOffset > 0L
                val existingBytes = if (appendMode) resumeOffset else 0L
                if (!appendMode) {
                    part.delete()
                }
                val contentRange = conn.getHeaderField("Content-Range")
                val rangeTotal = contentRange?.substringAfterLast('/')?.toLongOrNull()
                val headerLen = conn.contentLengthLong.takeIf { it > 0L }
                val total = rangeTotal ?: headerLen?.let { if (appendMode) existingBytes + it else it } ?: rel.size
                writeMeta(app, rel.tag, responseETag, total)

                withContext(Dispatchers.Main) {
                    downloadedBytes = existingBytes
                    totalBytes = total
                    progress = if (total > 0L) (existingBytes.toFloat() / total).coerceIn(0f, 1f) else 0f
                }

                var done = existingBytes
                var windowBytes = 0L
                var windowStartMs = SystemClock.uptimeMillis()
                var lastUiMs = 0L

                try {
                    conn.inputStream.use { input ->
                        RandomAccessFile(part, "rw").use { raf ->
                            raf.seek(existingBytes)
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                when (controlSignal) {
                                    ControlSignal.PAUSE -> return@runCatching DownloadOutcome.Paused
                                    ControlSignal.CANCEL -> return@runCatching DownloadOutcome.Cancelled
                                    null -> Unit
                                }
                                val n = input.read(buf)
                                if (n < 0) break
                                raf.write(buf, 0, n)
                                done += n
                                windowBytes += n
                                val now = SystemClock.uptimeMillis()
                                if (now - lastUiMs >= 160L || (total > 0L && done >= total)) {
                                    val elapsedMs = (now - windowStartMs).coerceAtLeast(1L)
                                    val spd = (windowBytes * 1000L) / elapsedMs
                                    if (elapsedMs >= 1500L) {
                                        windowBytes = 0L
                                        windowStartMs = now
                                    }
                                    lastUiMs = now
                                    val pct = if (total > 0L) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
                                    val remaining = (total - done).coerceAtLeast(0L)
                                    val eta = if (total > 0L && spd > 2048L) remaining / spd else null
                                    withContext(Dispatchers.Main) {
                                        downloadedBytes = done
                                        totalBytes = total
                                        progress = pct
                                        speedBytesPerSec = spd
                                        etaSeconds = eta
                                    }
                                    UpdateDownloadService.notifyProgress(app, rel.tag, pct, done, total, spd, eta)
                                }
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
                }

                if (controlSignal == ControlSignal.PAUSE) return@runCatching DownloadOutcome.Paused
                if (controlSignal == ControlSignal.CANCEL) return@runCatching DownloadOutcome.Cancelled

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
                metaFile(app).delete()
                DownloadOutcome.Completed(out)
            }
        }

        result.onSuccess { outcome ->
            when (outcome) {
                is DownloadOutcome.Completed -> {
                    apk = outcome.file
                    prefs(app).edit().putString("apkTag", rel.tag).apply()
                    downloadedBytes = outcome.file.length()
                    totalBytes = outcome.file.length()
                    speedBytesPerSec = 0L
                    etaSeconds = 0L
                    progress = 1f
                    phase = Phase.READY
                    UpdateDownloadService.stopService(app)
                    install(app)
                }
                DownloadOutcome.Paused -> {
                    speedBytesPerSec = 0L
                    etaSeconds = null
                    phase = Phase.PAUSED
                }
                DownloadOutcome.Cancelled -> {
                    speedBytesPerSec = 0L
                    etaSeconds = null
                    downloadedBytes = 0L
                    progress = 0f
                    phase = Phase.AVAILABLE
                }
            }
        }.onFailure { err ->
            if (err is CancellationException) {
                if (controlSignal == ControlSignal.PAUSE) phase = Phase.PAUSED
                return
            }
            speedBytesPerSec = 0L
            etaSeconds = null
            error = err.message ?: "Download failed."
            phase = if (hasPartialDownload(app)) Phase.PAUSED else Phase.FAILED
        }
    }

    private suspend fun restartFreshDownload(app: Context, rel: Release): DownloadOutcome {
        val part = partFile(app)
        val out = finalApkFile(app)
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
        val total = conn.contentLengthLong.takeIf { it > 0L } ?: rel.size
        writeMeta(app, rel.tag, conn.getHeaderField("ETag").orEmpty(), total)
        var done = 0L
        var lastUiMs = 0L
        try {
            conn.inputStream.use { input ->
                part.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        if (controlSignal == ControlSignal.PAUSE) return DownloadOutcome.Paused
                        if (controlSignal == ControlSignal.CANCEL) return DownloadOutcome.Cancelled
                        val n = input.read(buf)
                        if (n < 0) break
                        o.write(buf, 0, n)
                        done += n
                        val now = SystemClock.uptimeMillis()
                        if (now - lastUiMs >= 160L || (total > 0L && done >= total)) {
                            lastUiMs = now
                            val pct = if (total > 0L) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
                            withContext(Dispatchers.Main) {
                                downloadedBytes = done
                                totalBytes = total
                                progress = pct
                            }
                            UpdateDownloadService.notifyProgress(app, rel.tag, pct, done, total, 0L, null)
                        }
                    }
                    o.flush()
                }
            }
        } finally {
            conn.disconnect()
        }
        out.delete()
        if (!part.renameTo(out)) {
            part.copyTo(out, overwrite = true)
            part.delete()
        }
        metaFile(app).delete()
        return DownloadOutcome.Completed(out)
    }

    /** Called by MainActivity.onResume or UpdateDialogHost when the app is visible. */
    fun resumeIfWaiting(context: Context) {
        pendingConfirm?.let { confirm ->
            if (launchConfirm(context, confirm)) return
        }
        val persistedWait = prefs(context).getLong("waitingPermAt", 0L) > 0L
        if (!waitingForPermission && !persistedWait) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()) {
            waitingForPermission = false
            prefs(context).edit().remove("waitingPermAt").apply()
            if (hasReadyApk(context)) install(context)
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

    fun reportInstallFailure(message: String?, deleteCachedApk: Boolean = false, context: Context? = null) {
        pendingConfirm = null
        waitingForPermission = false
        if (deleteCachedApk && context != null) {
            apk = null
            runCatching { File(context.cacheDir, "updates").deleteRecursively() }
            prefs(context).edit().remove("apkTag").remove("waitingPermAt").apply()
        }
        error = message ?: "The installer stopped."
        phase = Phase.FAILED
    }

    fun install(context: Context) {
        val app = context.applicationContext
        val file = apk?.takeIf { it.exists() && it.length() > 100_000L }
            ?: finalApkFile(app).takeIf { it.exists() && it.length() > 100_000L }
        if (file == null) {
            reportInstallFailure("The update file is missing. Download it again.", deleteCachedApk = true, context = app)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !app.packageManager.canRequestPackageInstalls()) {
            waitingForPermission = true
            prefs(app).edit().putLong("waitingPermAt", System.currentTimeMillis()).apply()
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
