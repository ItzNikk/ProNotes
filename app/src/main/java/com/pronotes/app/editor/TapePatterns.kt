package com.pronotes.app.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** Washi tape patterns, drawn as small repeating tiles in the tape's colour. */
object TapePatterns {
    val names = listOf("stripes", "plain", "dots", "grid", "checks", "zigzag", "stars", "hearts", "waves", "flowers")

    fun label(p: String) = when (p) {
        "stripes" -> "Stripes"; "plain" -> "Plain"; "dots" -> "Dots"; "grid" -> "Grid"; "checks" -> "Gingham"
        "zigzag" -> "Zigzag"; "stars" -> "Stars"; "hearts" -> "Hearts"; "waves" -> "Waves"; "flowers" -> "Flowers"
        else -> "Picture"
    }

    private fun shade(c: Int, k: Float): Int {
        val r = (Color.red(c) * k).toInt().coerceIn(0, 255); val g = (Color.green(c) * k).toInt().coerceIn(0, 255); val b = (Color.blue(c) * k).toInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(c), r, g, b)
    }

    fun tile(pattern: String, color: Int): Bitmap {
        val n = 92
        val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(color)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = shade(color, 0.78f) }
        val light = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = 0x66FFFFFF }
        when (pattern) {
            "dots" -> for (y in 0..1) for (x in 0..1) c.drawCircle(23f + x * 46f + (y % 2) * 0f, 23f + y * 46f, 8f, light)
            "grid" -> { p.strokeWidth = 3f; for (k in 0..3) { c.drawLine(k * 23f, 0f, k * 23f, n.toFloat(), p); c.drawLine(0f, k * 23f, n.toFloat(), k * 23f, p) } }
            "checks" -> { p.alpha = 110; for (k in 0..1) { c.drawRect(k * 46f, 0f, k * 46f + 23f, n.toFloat(), p); c.drawRect(0f, k * 46f, n.toFloat(), k * 46f + 23f, p) } }
            "zigzag" -> {
                p.style = Paint.Style.STROKE; p.strokeWidth = 6f
                val path = Path()
                for (row in 0..1) { val y = 23f + row * 46f; path.moveTo(0f, y); var x = 0f; var up = true; while (x < n) { x += 11.5f; path.lineTo(x, if (up) y - 10f else y + 10f); up = !up } }
                c.drawPath(path, light)
            }
            "stars" -> listOf(23f to 23f, 69f to 69f).forEach { (x, y) -> star(c, x, y, 13f, light) }
            "hearts" -> listOf(23f to 26f, 69f to 72f).forEach { (x, y) -> heart(c, x, y, 12f, light) }
            "waves" -> {
                light.style = Paint.Style.STROKE; light.strokeWidth = 5f
                for (row in 0..1) {
                    val y = 23f + row * 46f
                    val path = Path().apply { moveTo(0f, y); cubicTo(23f, y - 14f, 23f, y + 14f, 46f, y); cubicTo(69f, y - 14f, 69f, y + 14f, 92f, y) }
                    c.drawPath(path, light)
                }
            }
            "flowers" -> listOf(23f to 23f, 69f to 69f).forEach { (x, y) ->
                for (k in 0 until 5) {
                    val a = Math.toRadians(72.0 * k)
                    c.drawCircle(x + 8f * kotlin.math.cos(a).toFloat(), y + 8f * kotlin.math.sin(a).toFloat(), 6f, light)
                }
                c.drawCircle(x, y, 4.5f, p)
            }
        }
        return bmp
    }

    private fun star(c: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        val path = Path()
        for (i in 0 until 10) {
            val rr = if (i % 2 == 0) r else r * 0.45f
            val a = Math.toRadians(-90.0 + i * 36.0)
            val x = cx + rr * kotlin.math.cos(a).toFloat(); val y = cy + rr * kotlin.math.sin(a).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close(); c.drawPath(path, paint)
    }

    private fun heart(c: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(cx, cy + s)
            cubicTo(cx - s * 1.6f, cy - s * 0.2f, cx - s * 0.6f, cy - s * 1.4f, cx, cy - s * 0.4f)
            cubicTo(cx + s * 0.6f, cy - s * 1.4f, cx + s * 1.6f, cy - s * 0.2f, cx, cy + s)
        }
        c.drawPath(path, paint)
    }

    @Suppress("unused") private val unusedRect = RectF()
}
