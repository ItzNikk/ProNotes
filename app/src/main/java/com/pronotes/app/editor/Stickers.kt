package com.pronotes.app.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

/** A sticker in the picker. Built-in ones are drawn in code; the user's own are PNG files. */
sealed interface Sticker {
    data class Builtin(val id: String, val label: String) : Sticker
    data class Emoji(val text: String) : Sticker
    data class Custom(val file: File) : Sticker
}

/**
 * Sticker library: a set of planner-style stickers drawn in code (labels, badges,
 * arrows, check marks), emoji, and stickers the user imports from photos.
 */
object StickerLibrary {
    val builtins = listOf(
        Sticker.Builtin("important", "Important"), Sticker.Builtin("todo", "To do"), Sticker.Builtin("done", "Done"),
        Sticker.Builtin("note", "Note"), Sticker.Builtin("idea", "Idea"), Sticker.Builtin("due", "Due"),
        Sticker.Builtin("exam", "Exam"), Sticker.Builtin("review", "Review"),
        Sticker.Builtin("star", "Star"), Sticker.Builtin("heart", "Heart"), Sticker.Builtin("check", "Check"),
        Sticker.Builtin("cross", "Cross"), Sticker.Builtin("arrow", "Arrow"), Sticker.Builtin("pin", "Pin"),
        Sticker.Builtin("bulb", "Bulb"), Sticker.Builtin("flag", "Flag"),
    )

    private val labelColors = mapOf(
        "important" to (0xFFE34948.toInt() to 0xFFD55181.toInt()), "todo" to (0xFF2A78D6.toInt() to 0xFF7161E0.toInt()),
        "done" to (0xFF0CA30C.toInt() to 0xFF1BAF7A.toInt()), "note" to (0xFFEDA100.toInt() to 0xFFEB6834.toInt()),
        "idea" to (0xFF8173E3.toInt() to 0xFFC04873.toInt()), "due" to (0xFFD95926.toInt() to 0xFFE34948.toInt()),
        "exam" to (0xFF256ABF.toInt() to 0xFF0F7E5C.toInt()), "review" to (0xFF5F5E5A.toInt() to 0xFF898781.toInt()),
    )

    fun dir(context: Context) = File(context.filesDir, "stickers").apply { mkdirs() }

    fun custom(context: Context): List<Sticker.Custom> =
        dir(context).listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }?.map { Sticker.Custom(it) } ?: emptyList()

    /** Copies a picked picture into the sticker library (scaled down to 800 px). */
    fun import(context: Context, uri: Uri): File? = runCatching {
        val src = context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) } ?: return null
        val s = (800f / maxOf(src.width, src.height)).coerceAtMost(1f)
        val bmp = Bitmap.createScaledBitmap(src, (src.width * s).toInt().coerceAtLeast(1), (src.height * s).toInt().coerceAtLeast(1), true)
        val f = File(dir(context), "s_${UUID.randomUUID()}.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        f
    }.getOrNull()

    fun bitmap(sticker: Sticker, size: Int = 360): Bitmap? = when (sticker) {
        is Sticker.Custom -> BitmapFactory.decodeFile(sticker.file.path)
        is Sticker.Emoji -> emoji(sticker.text, size)
        is Sticker.Builtin -> builtin(sticker.id, size)
    }

    private fun emoji(text: String, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size * 0.8f; textAlign = Paint.Align.CENTER }
        val c = Canvas(bmp)
        val fm = p.fontMetrics
        c.drawText(text, size / 2f, size / 2f - (fm.ascent + fm.descent) / 2f, p)
        return bmp
    }

    private fun builtin(id: String, size: Int): Bitmap {
        val label = labelColors[id]
        if (label != null) {
            val w = size; val h = (size * 0.42f).toInt()
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val r = RectF(6f, 6f, w - 6f, h - 6f)
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(r.left, r.top, r.right, r.bottom, label.first, label.second, Shader.TileMode.CLAMP)
                setShadowLayer(6f, 0f, 3f, 0x33000000)
            }
            c.drawRoundRect(r, r.height() / 2f, r.height() / 2f, bg)
            val t = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; textSize = h * 0.42f; textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); letterSpacing = 0.06f
            }
            val fm = t.fontMetrics
            c.drawText(builtins.first { it.id == id }.label.uppercase(), w / 2f, h / 2f - (fm.ascent + fm.descent) / 2f, t)
            return bmp
        }
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val s = size.toFloat()
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = s * 0.08f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = Color.WHITE
        }
        fun disc(a: Int, b: Int) {
            fill.shader = LinearGradient(0f, 0f, s, s, a, b, Shader.TileMode.CLAMP)
            c.drawCircle(s / 2, s / 2, s * 0.44f, fill)
            fill.shader = null
        }
        when (id) {
            "star" -> {
                val path = Path()
                for (i in 0 until 10) {
                    val a = -Math.PI / 2 + i * Math.PI / 5
                    val k = if (i % 2 == 0) 0.46f else 0.2f
                    val x = s / 2 + (s * k * cos(a)).toFloat(); val y = s / 2 + (s * k * sin(a)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                fill.shader = LinearGradient(0f, 0f, s, s, 0xFFFAB219.toInt(), 0xFFEB6834.toInt(), Shader.TileMode.CLAMP)
                c.drawPath(path, fill)
            }
            "heart" -> {
                val path = Path()
                for (i in 0..120) {
                    val t = i / 120.0 * 2 * Math.PI
                    val x = 16 * sin(t) * sin(t) * sin(t)
                    val y = 13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t)
                    val px = s / 2 + (x / 17.0 * s * 0.45).toFloat(); val py = s / 2 - (y / 17.0 * s * 0.45).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                fill.shader = LinearGradient(0f, 0f, s, s, 0xFFE46191.toInt(), 0xFFE34948.toInt(), Shader.TileMode.CLAMP)
                c.drawPath(path, fill)
            }
            "check" -> { disc(0xFF35B231.toInt(), 0xFF0F7E5C.toInt()); c.drawPath(Path().apply { moveTo(s * 0.3f, s * 0.52f); lineTo(s * 0.44f, s * 0.66f); lineTo(s * 0.72f, s * 0.36f) }, line) }
            "cross" -> {
                disc(0xFFE34948.toInt(), 0xFFB93535.toInt())
                c.drawLine(s * 0.34f, s * 0.34f, s * 0.66f, s * 0.66f, line); c.drawLine(s * 0.66f, s * 0.34f, s * 0.34f, s * 0.66f, line)
            }
            "arrow" -> {
                line.color = 0xFF2A78D6.toInt(); line.strokeWidth = s * 0.09f
                c.drawLine(s * 0.14f, s * 0.5f, s * 0.82f, s * 0.5f, line)
                c.drawPath(Path().apply { moveTo(s * 0.6f, s * 0.28f); lineTo(s * 0.84f, s * 0.5f); lineTo(s * 0.6f, s * 0.72f) }, line)
            }
            "pin" -> {
                fill.color = 0xFFE34948.toInt(); c.drawCircle(s / 2, s * 0.36f, s * 0.22f, fill)
                fill.color = 0x33FFFFFF; c.drawCircle(s * 0.44f, s * 0.3f, s * 0.07f, fill)
                line.color = 0xFF5F5E5A.toInt(); line.strokeWidth = s * 0.05f; c.drawLine(s / 2, s * 0.58f, s / 2, s * 0.92f, line)
            }
            "bulb" -> {
                disc(0xFFFAB219.toInt(), 0xFFEDA100.toInt())
                line.strokeWidth = s * 0.06f
                c.drawCircle(s / 2, s * 0.44f, s * 0.16f, line)
                c.drawLine(s * 0.43f, s * 0.68f, s * 0.57f, s * 0.68f, line)
            }
            else -> { // flag
                line.color = 0xFF383835.toInt(); line.strokeWidth = s * 0.05f; c.drawLine(s * 0.24f, s * 0.12f, s * 0.24f, s * 0.9f, line)
                fill.shader = LinearGradient(0f, 0f, s, 0f, 0xFF2A78D6.toInt(), 0xFF7161E0.toInt(), Shader.TileMode.CLAMP)
                c.drawPath(Path().apply { moveTo(s * 0.26f, s * 0.14f); lineTo(s * 0.84f, s * 0.3f); lineTo(s * 0.26f, s * 0.48f); close() }, fill)
            }
        }
        return bmp
    }
}
