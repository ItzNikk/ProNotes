package com.pronotes.app.editor

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Stores and loads user-imported `.ttf` / `.otf` fonts for Auto-Handwriting and Text items.
 * Supports importing from local device files as well as HTTPS links (direct `.ttf`/`.otf` or Google Fonts links).
 */
object CustomFontRegistry {
    data class InstalledFont(val name: String, val path: String) {
        val displayName: String get() = name
    }

    private const val MAX_FONT_BYTES = 10 * 1024 * 1024 // 10 MB safety cap
    private const val MAX_CSS_BYTES = 256 * 1024 // 256 KB safety cap
    private const val MAX_REDIRECTS = 3
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000

    private val cache = ConcurrentHashMap<String, Typeface>()

    private fun fontsDir(context: Context): File =
        File(context.filesDir, "custom_fonts").apply { mkdirs() }

    fun list(context: Context): List<InstalledFont> {
        val dir = fontsDir(context)
        return dir.listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".ttf", true) || it.name.endsWith(".otf", true)) }
            ?.sortedBy { it.name.lowercase() }
            ?.map { f ->
                val clean = f.nameWithoutExtension.replace('_', ' ').replace('-', ' ').trim()
                InstalledFont(clean.ifBlank { f.name }, f.absolutePath)
            }
            .orEmpty()
    }

    fun listFonts(context: Context): List<InstalledFont> = list(context)

    fun importFont(context: Context, uri: Uri): InstalledFont? {
        return runCatching {
            val resolver = context.contentResolver
            var displayName = "CustomFont_${System.currentTimeMillis()}.ttf"
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val raw = c.getString(idx)?.trim().orEmpty()
                        if (raw.isNotBlank()) displayName = raw
                    }
                }
            }
            val bytes = resolver.openInputStream(uri)?.use { readBounded(it, MAX_FONT_BYTES) } ?: return null
            installFontBytes(context, bytes, displayName)
        }.getOrNull()
    }

    /**
     * Securely downloads and installs a `.ttf` / `.otf` font from an HTTPS link or Google Fonts URL.
     * Supports:
     *  - Direct `.ttf` / `.otf` HTTPS links (e.g. GitHub raw, CDN)
     *  - Google Fonts specimen pages (e.g. `https://fonts.google.com/specimen/Caveat`)
     *  - Google Fonts CSS links (e.g. `https://fonts.googleapis.com/css2?family=Caveat&display=swap`)
     */
    suspend fun importFontFromUrl(context: Context, rawUrl: String): Result<InstalledFont> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = rawUrl.trim()
            if (trimmed.isBlank()) error("Enter a font URL")
            val normalizedInput = if (!trimmed.contains("://")) "https://$trimmed" else trimmed
            val initialUri = parseSafeHttpsUri(normalizedInput)
            val hostLower = initialUri.host.lowercase()

            // Convert Google Fonts specimen page -> CSS2 endpoint automatically
            val (fetchUri, suggestedName) = when {
                hostLower == "fonts.google.com" && initialUri.path.orEmpty().startsWith("/specimen/") -> {
                    val slug = initialUri.path.removePrefix("/specimen/").substringBefore('/').trim()
                    if (slug.isBlank()) error("Could not read font family from Google Fonts link")
                    val decodedFamily = Uri.decode(slug).replace('+', ' ').trim()
                    val encoded = URLEncoder.encode(decodedFamily, "UTF-8").replace("+", "%20")
                    val cssUri = parseSafeHttpsUri("https://fonts.googleapis.com/css2?family=${encoded.replace("%20", "+")}&display=swap")
                    cssUri to decodedFamily
                }
                hostLower == "fonts.googleapis.com" -> {
                    val query = initialUri.rawQuery.orEmpty()
                    val famRaw = query.split('&')
                        .firstOrNull { it.startsWith("family=") }
                        ?.removePrefix("family=")
                        ?.substringBefore(':')
                        ?.let { Uri.decode(it).replace('+', ' ').trim() }
                        .orEmpty()
                    initialUri to famRaw.ifBlank { "GoogleFont" }
                }
                else -> {
                    val lastSeg = initialUri.path.orEmpty().substringAfterLast('/').substringBefore('?')
                    val clean = Uri.decode(lastSeg).ifBlank { "LinkedFont_${System.currentTimeMillis()}" }
                    initialUri to clean
                }
            }

            // If this is a Google Fonts CSS endpoint, request with an Android 6 UA so Google returns direct `.ttf` URLs.
            val isCssRequest = fetchUri.host.equals("fonts.googleapis.com", ignoreCase = true) ||
                fetchUri.path.orEmpty().endsWith(".css", ignoreCase = true)

            val (fontBytes, finalName) = if (isCssRequest) {
                val cssBytes = downloadHttpsBytes(
                    uri = fetchUri,
                    maxBytes = MAX_CSS_BYTES,
                    userAgent = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.76 Mobile Safari/537.36",
                )
                val cssText = String(cssBytes, Charsets.UTF_8)
                val fontUrlMatch = Regex("""url\((['"]?)(https://[^'")\s]+\.(?:ttf|otf)(?:\?[^'")\s]*)?)\1\)""", RegexOption.IGNORE_CASE)
                    .find(cssText)
                    ?: Regex("""url\((['"]?)(https://fonts\.gstatic\.com/[^'")\s]+)\1\)""", RegexOption.IGNORE_CASE)
                        .find(cssText)
                    ?: error("No .ttf or .otf font file found in that stylesheet link")
                val extractedUrl = fontUrlMatch.groupValues[2]
                val extractedUri = parseSafeHttpsUri(extractedUrl)
                val familyFromCss = Regex("""font-family:\s*['"]?([^'";]+)['"]?""", RegexOption.IGNORE_CASE)
                    .find(cssText)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                val ext = if (extractedUri.path.orEmpty().endsWith(".otf", true)) ".otf" else ".ttf"
                val baseName = familyFromCss.ifBlank { suggestedName }.substringBeforeLast('.')
                val bytes = downloadHttpsBytes(extractedUri, MAX_FONT_BYTES, userAgent = "ProNotes-Android/1.0")
                bytes to "$baseName$ext"
            } else {
                val bytes = downloadHttpsBytes(fetchUri, MAX_FONT_BYTES, userAgent = "ProNotes-Android/1.0")
                bytes to suggestedName
            }

            installFontBytes(context, fontBytes, finalName)
                ?: error("Downloaded file is not a valid .ttf or .otf font")
        }
    }

    private fun parseSafeHttpsUri(raw: String): URI {
        val uri = runCatching { URI(raw) }.getOrElse { error("Invalid URL format") }
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            error("Only secure https:// font links are supported")
        }
        val host = uri.host?.trim().orEmpty()
        if (host.isBlank()) error("URL is missing a host name")
        val lower = host.lowercase()
        if (lower == "localhost" || lower.endsWith(".local") || lower.endsWith(".internal") || lower.endsWith(".localhost")) {
            error("Local or private network links are not allowed")
        }
        // Resolve DNS and block loopback, private RFC1918, link-local metadata, multicast, and wildcard addresses.
        val addresses = runCatching { InetAddress.getAllByName(host) }.getOrElse {
            error("Could not reach host: $host")
        }
        if (addresses.isEmpty()) error("Could not resolve host: $host")
        for (addr in addresses) {
            if (addr.isAnyLocalAddress ||
                addr.isLoopbackAddress ||
                addr.isLinkLocalAddress ||
                addr.isSiteLocalAddress ||
                addr.isMulticastAddress
            ) {
                error("Private or local network addresses are not allowed")
            }
            val rawBytes = addr.address
            if (rawBytes.size == 4) {
                val b0 = rawBytes[0].toInt() and 0xFF
                val b1 = rawBytes[1].toInt() and 0xFF
                // Block carrier-grade NAT 100.64.0.0/10 and 0.0.0.0/8
                if (b0 == 0 || (b0 == 100 && b1 in 64..127)) {
                    error("Private network addresses are not allowed")
                }
            }
        }
        return uri
    }

    private fun downloadHttpsBytes(uri: URI, maxBytes: Int, userAgent: String): ByteArray {
        var current = uri
        repeat(MAX_REDIRECTS + 1) { hop ->
            val conn = (URL(current.toASCIIString()).openConnection() as? HttpURLConnection)
                ?: error("Unsupported connection type")
            conn.instanceFollowRedirects = false
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", userAgent)
            conn.setRequestProperty("Accept", "*/*")
            try {
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")?.trim().orEmpty()
                    if (loc.isBlank() || hop >= MAX_REDIRECTS) error("Too many redirects")
                    val resolved = current.resolve(loc).toASCIIString()
                    current = parseSafeHttpsUri(resolved)
                    return@repeat
                }
                if (code !in 200..299) {
                    error("Server returned HTTP $code")
                }
                val declaredLen = conn.contentLengthLong
                if (declaredLen > maxBytes) {
                    error("File is too large (max ${maxBytes / (1024 * 1024)} MB)")
                }
                return conn.inputStream.use { readBounded(it, maxBytes) }
            } finally {
                conn.disconnect()
            }
        }
        error("Too many redirects")
    }

    private fun readBounded(input: java.io.InputStream, maxBytes: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buf)
            if (read <= 0) break
            total += read
            if (total > maxBytes) error("File exceeds size limit")
            out.write(buf, 0, read)
        }
        return out.toByteArray()
    }

    private fun hasValidFontHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        val b2 = bytes[2].toInt() and 0xFF
        val b3 = bytes[3].toInt() and 0xFF
        // TrueType (00 01 00 00), Apple TrueType ('true'), OpenType CFF ('OTTO'), TrueType Collection ('ttcf')
        val isTtf = b0 == 0x00 && b1 == 0x01 && b2 == 0x00 && b3 == 0x00
        val isOtf = b0 == 'O'.code && b1 == 'T'.code && b2 == 'T'.code && b3 == 'O'.code
        val isTrue = b0 == 't'.code && b1 == 'r'.code && b2 == 'u'.code && b3 == 'e'.code
        val isTtc = b0 == 't'.code && b1 == 't'.code && b2 == 'c'.code && b3 == 'f'.code
        return isTtf || isOtf || isTrue || isTtc
    }

    private fun installFontBytes(context: Context, bytes: ByteArray, suggestedName: String): InstalledFont? {
        if (!hasValidFontHeader(bytes)) return null
        val dir = fontsDir(context)
        val safeName = suggestedName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val isOtfBytes = bytes[0].toInt() == 'O'.code && bytes[1].toInt() == 'T'.code
        val ext = if (isOtfBytes || safeName.endsWith(".otf", true)) ".otf" else ".ttf"
        val base = safeName.substringBeforeLast('.').trim('_').ifBlank { "Font_${System.currentTimeMillis()}" }.take(64)
        val dest = File(dir, "$base$ext")
        // Verify strict directory confinement before writing
        val dirCanon = dir.canonicalPath + File.separator
        if (!dest.canonicalPath.startsWith(dirCanon)) return null

        val tmp = File(dir, ".tmp_${System.nanoTime()}$ext")
        return try {
            tmp.writeBytes(bytes)
            val tf = Typeface.createFromFile(tmp)
            if (dest.exists()) dest.delete()
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
            cache[dest.absolutePath] = tf
            InstalledFont(base.replace('_', ' ').replace('-', ' ').trim().ifBlank { dest.name }, dest.absolutePath)
        } catch (_: Throwable) {
            runCatching { tmp.delete() }
            null
        }
    }

    fun deleteFont(path: String): Boolean {
        if (path.isBlank()) return false
        cache.remove(path)
        return runCatching { File(path).delete() }.getOrDefault(false)
    }

    fun typeface(path: String?): Typeface? {
        if (path.isNullOrBlank()) return null
        cache[path]?.let { return it }
        val file = File(path)
        if (!file.exists()) return null
        return runCatching { Typeface.createFromFile(file) }.getOrNull()?.also { cache[path] = it }
    }

    fun loadTypeface(path: String?): Typeface? = typeface(path)

    fun displayName(path: String?): String {
        if (path.isNullOrBlank()) return ""
        return File(path).nameWithoutExtension.replace('_', ' ').replace('-', ' ').trim()
    }
}
