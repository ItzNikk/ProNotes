package com.pronotes.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Persists user-imported custom tape textures permanently inside app-private storage
 * (`filesDir/custom_tapes/`) at crisp high resolution so imported tapes stay available
 * across all notebooks and sessions without blur or pixelation.
 */
object CustomTapeStore {
    private const val DIR_NAME = "custom_tapes"
    private const val MAX_IMPORT_DIM = 1024
    private const val MAX_TAPE_COUNT = 48

    @Volatile
    private var cachedBaseDir: File? = null

    fun baseDir(context: Context): File =
        File(context.applicationContext.filesDir, DIR_NAME).apply {
            if (!exists()) mkdirs()
            cachedBaseDir = this
        }

    fun resolveBaseDir(noteDir: File?): File? {
        cachedBaseDir?.let { if (it.exists()) return it }
        val appFiles = noteDir?.parentFile?.parentFile ?: return null
        return File(appFiles, DIR_NAME).takeIf { it.exists() }
    }

    private fun safeTapeFile(dir: File, fileName: String): File? {
        val clean = fileName.trim()
        if (clean.isEmpty() || clean.contains('/') || clean.contains('\\') || clean.contains("..")) return null
        if (!clean.endsWith(".png", ignoreCase = true) && !clean.endsWith(".jpg", ignoreCase = true) && !clean.endsWith(".webp", ignoreCase = true)) {
            return null
        }
        val candidate = File(dir, clean)
        val baseCanon = runCatching { dir.canonicalPath }.getOrNull() ?: return null
        val candCanon = runCatching { candidate.canonicalPath }.getOrNull() ?: return null
        if (!candCanon.startsWith(baseCanon + File.separator)) return null
        return candidate
    }

    fun listTapes(context: Context): List<String> {
        val dir = baseDir(context)
        return dir.listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".png", true) || it.name.endsWith(".jpg", true) || it.name.endsWith(".webp", true)) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.name }
            .orEmpty()
    }

    fun importTape(context: Context, uri: Uri): String? {
        val dir = baseDir(context)
        val appCtx = context.applicationContext
        val decoded = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            appCtx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            var sample = 1
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            while (longest / (sample * 2) >= MAX_IMPORT_DIM) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            appCtx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }.getOrNull() ?: return null

        val finalBmp = try {
            val longest = maxOf(decoded.width, decoded.height)
            if (longest > MAX_IMPORT_DIM) {
                val ratio = MAX_IMPORT_DIM.toFloat() / longest.toFloat()
                val nw = (decoded.width * ratio).toInt().coerceAtLeast(16)
                val nh = (decoded.height * ratio).toInt().coerceAtLeast(16)
                Bitmap.createScaledBitmap(decoded, nw, nh, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }
        } catch (_: Throwable) {
            decoded.recycle()
            return null
        }

        val fileName = "tape_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png"
        val outFile = safeTapeFile(dir, fileName) ?: run {
            finalBmp.recycle()
            return null
        }
        val saved = runCatching {
            outFile.outputStream().use { out ->
                finalBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }.getOrDefault(false)
        finalBmp.recycle()
        if (!saved) {
            outFile.delete()
            return null
        }

        // Trim very old entries only if the user exceeds the generous cap.
        val all = dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }.orEmpty()
        if (all.size > MAX_TAPE_COUNT) {
            all.drop(MAX_TAPE_COUNT).forEach { runCatching { it.delete() } }
        }
        return fileName
    }

    fun loadBitmap(context: Context, fileName: String, maxDim: Int = 768): Bitmap? =
        loadBitmapFromDir(baseDir(context), fileName, maxDim)

    fun loadBitmapForRenderer(noteDir: File?, fileName: String, maxDim: Int = 768): Bitmap? {
        val dir = resolveBaseDir(noteDir) ?: return null
        return loadBitmapFromDir(dir, fileName, maxDim)
    }

    private fun loadBitmapFromDir(dir: File, fileName: String, maxDim: Int): Bitmap? {
        val file = safeTapeFile(dir, fileName) ?: return null
        if (!file.exists()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            var sample = 1
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            while (longest / (sample * 2) >= maxDim) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        }.getOrNull()
    }

    fun deleteTape(context: Context, fileName: String): Boolean {
        val file = safeTapeFile(baseDir(context), fileName) ?: return false
        return runCatching { file.delete() }.getOrDefault(false)
    }
}
