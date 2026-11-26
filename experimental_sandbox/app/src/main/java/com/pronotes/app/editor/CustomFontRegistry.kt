package com.pronotes.app.editor

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Stores and loads user-imported `.ttf` / `.otf` fonts for Auto-Handwriting and Text items.
 */
object CustomFontRegistry {
    data class InstalledFont(val name: String, val path: String) {
        val displayName: String get() = name
    }

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
            val safeName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val ext = if (safeName.endsWith(".otf", true)) ".otf" else ".ttf"
            val base = safeName.substringBeforeLast('.').ifBlank { "Font_${System.currentTimeMillis()}" }
            val dest = File(fontsDir(context), "$base$ext")
            resolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            // Verify Android can actually parse this font file before accepting it.
            val tf = Typeface.createFromFile(dest)
            cache[dest.absolutePath] = tf
            InstalledFont(base.replace('_', ' ').replace('-', ' ').trim().ifBlank { dest.name }, dest.absolutePath)
        }.getOrNull()
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
