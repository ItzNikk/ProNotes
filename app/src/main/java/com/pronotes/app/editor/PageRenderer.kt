package com.pronotes.app.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.LruCache
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.pronotes.app.data.PaperStyle
import java.io.File

/** Renders PDF backgrounds with the platform PdfRenderer and keeps a small bitmap cache. */
class PdfPages(private val file: File) {
    private var renderer: PdfRenderer? = null
    private val cache = object : LruCache<Int, Bitmap>(4) {}

    val exists: Boolean get() = file.exists()

    @Synchronized
    private fun open(): PdfRenderer? {
        if (renderer == null && file.exists()) {
            renderer = PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
        }
        return renderer
    }

    fun cached(index: Int): Bitmap? = cache.get(index)

    /** Blocking; call from a background thread. */
    @Synchronized
    fun render(index: Int, width: Int, height: Int, useCache: Boolean = true): Bitmap? {
        if (useCache) cache.get(index)?.let { return it }
        val r = open() ?: return null
        if (index !in 0 until r.pageCount) return null
        return r.openPage(index).use { page ->
            val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            if (useCache) cache.put(index, bmp)
            bmp
        }
    }

    @Synchronized
    fun close() {
        renderer?.close()
        renderer = null
        cache.evictAll()
    }
}

/** Shared drawing code for the live canvas, thumbnails, previews and PDF export. */
class PageRenderer(private val noteDir: File?) {
    val strokeRenderer: CanvasStrokeRenderer = CanvasStrokeRenderer.create()

    var paperColor: Int = Color.WHITE

    private val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { setShadowLayer(22f, 0f, 6f, 0x26000000) }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 2f }
    private val boldLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f }
    private val marginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66EB5757; strokeWidth = 2f }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 26f }
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val tapePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tapeStripe = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF; strokeWidth = 10f }
    private val stickyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { setShadowLayer(10f, 0f, 4f, 0x33000000) }
    private val stickyText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 30f; color = 0xFF2B2B2B.toInt() }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FFD60A }
    private val activeHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99FF9F0A.toInt() }
    private val images = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val scratch = Matrix()

    private val isDark get() = Color.luminance(paperColor) < 0.4f

    fun image(name: String): Bitmap? {
        val dir = noteDir ?: return null
        images.get(name)?.let { return it }
        val f = File(dir, name)
        if (!f.exists()) return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, opts)
        var sample = 1
        while (opts.outWidth / sample > 2048 || opts.outHeight / sample > 2048) sample *= 2
        val bmp = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        images.put(name, bmp)
        return bmp
    }

    fun drawPaper(canvas: Canvas, page: Page, pdf: Bitmap?, shadow: Boolean) {
        val rect = RectF(0f, 0f, page.width, page.height)
        val bg = if (page.pdfPage >= 0) Color.WHITE else paperColor
        val p = if (shadow) shadowPaint else pagePaint
        p.color = bg
        canvas.drawRect(rect, p)
        if (pdf != null) {
            canvas.drawBitmap(pdf, null, rect, bitmapPaint)
            return
        }
        if (page.pdfPage >= 0) return
        val line = if (isDark) 0xFF3A3F48.toInt() else 0xFFD5DEEA.toInt()
        linePaint.color = line; boldLine.color = line; boxPaint.color = line
        dotPaint.color = if (isDark) 0xFF4A505A.toInt() else 0xFFC3CCD8.toInt()
        labelPaint.color = if (isDark) 0xFF8A93A3.toInt() else 0xFF9AA4B2.toInt()
        val w = page.width; val h = page.height
        fun ruled(start: Float, gap: Float, left: Float = 0f) {
            var y = start
            while (y < h - 40f) { canvas.drawLine(left, y, w, y, linePaint); y += gap }
        }
        when (page.paper) {
            PaperStyle.BLANK -> Unit
            PaperStyle.RULED -> { ruled(180f, 52f); canvas.drawLine(130f, 0f, 130f, h, marginPaint) }
            PaperStyle.NARROW -> { ruled(160f, 38f); canvas.drawLine(110f, 0f, 110f, h, marginPaint) }
            PaperStyle.GRID -> {
                var x = 40f
                while (x < w) { canvas.drawLine(x, 0f, x, h, linePaint); x += 40f }
                var y = 40f
                while (y < h) { canvas.drawLine(0f, y, w, y, linePaint); y += 40f }
            }
            PaperStyle.DOTTED -> {
                var y = 40f
                while (y < h) {
                    var x = 40f
                    while (x < w) { canvas.drawCircle(x, y, 2.6f, dotPaint); x += 40f }
                    y += 40f
                }
            }
            PaperStyle.CORNELL -> {
                canvas.drawLine(0f, 200f, w, 200f, boldLine)
                canvas.drawLine(w * 0.3f, 200f, w * 0.3f, h * 0.8f, boldLine)
                canvas.drawLine(0f, h * 0.8f, w, h * 0.8f, boldLine)
                var y = 252f
                while (y < h * 0.8f - 20f) { canvas.drawLine(w * 0.3f, y, w, y, linePaint); y += 52f }
                canvas.drawText("CUES", 40f, 240f, labelPaint)
                canvas.drawText("SUMMARY", 40f, h * 0.8f + 40f, labelPaint)
            }
            PaperStyle.TODO -> {
                var y = 200f
                while (y < h - 80f) {
                    canvas.drawRoundRect(RectF(80f, y - 34f, 114f, y), 6f, 6f, boxPaint)
                    canvas.drawLine(140f, y, w - 80f, y, linePaint)
                    y += 72f
                }
                canvas.drawText("TO DO", 80f, 120f, labelPaint)
            }
            PaperStyle.PLANNER -> {
                val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN", "NOTES")
                val top = 140f; val cellH = (h - top - 40f) / 4f; val cellW = (w - 80f) / 2f
                days.forEachIndexed { i, d ->
                    val cx = 40f + (i % 2) * cellW; val cy = top + (i / 2) * cellH
                    canvas.drawRoundRect(RectF(cx + 8f, cy + 8f, cx + cellW - 8f, cy + cellH - 8f), 16f, 16f, boxPaint)
                    canvas.drawText(d, cx + 28f, cy + 48f, labelPaint)
                }
            }
            PaperStyle.MUSIC -> {
                var y = 180f
                while (y + 4 * 16f < h - 60f) {
                    for (k in 0 until 5) canvas.drawLine(80f, y + k * 16f, w - 80f, y + k * 16f, linePaint)
                    y += 150f
                }
            }
        }
    }

    /**
     * Draws page content. [worldToScreen] must already be applied to [canvas];
     * it is passed again so Ink can pick the right level of detail.
     */
    fun drawItems(
        canvas: Canvas,
        items: List<PageItem>,
        worldToScreen: Matrix,
        offset: Pair<Float, Float>? = null,
        offsetIds: Set<Long> = emptySet(),
    ) {
        for (item in items) {
            val moved = offset != null && item.id in offsetIds
            if (moved) {
                canvas.save()
                canvas.translate(offset!!.first, offset.second)
            }
            when (item) {
                is InkItem -> {
                    canvas.save()
                    canvas.translate(item.dx, item.dy)
                    scratch.set(worldToScreen)
                    if (moved) scratch.preTranslate(offset!!.first, offset.second)
                    scratch.preTranslate(item.dx, item.dy)
                    strokeRenderer.draw(canvas, item.stroke, scratch)
                    canvas.restore()
                }
                is TextItem -> {
                    textPaint.textSize = item.size
                    textPaint.color = item.color
                    val lh = item.size * 1.3f
                    item.text.split('\n').forEachIndexed { i, line ->
                        canvas.drawText(line, item.x, item.y + item.size + i * lh, textPaint)
                    }
                }
                is ImageItem -> {
                    val bmp = image(item.file)
                    if (bmp != null) canvas.drawBitmap(bmp, null, item.bounds, bitmapPaint)
                }
                is TapeItem -> {
                    tapePaint.color = if (item.revealed) (item.color and 0x00FFFFFF) or (0x40 shl 24) else item.color
                    val r = item.bounds
                    canvas.drawRoundRect(r, 6f, 6f, tapePaint)
                    if (!item.revealed) {
                        canvas.save(); canvas.clipRect(r)
                        var x = r.left - r.height()
                        while (x < r.right) { canvas.drawLine(x, r.bottom, x + r.height(), r.top, tapeStripe); x += 28f }
                        canvas.restore()
                    }
                }
                is StickyItem -> {
                    stickyPaint.color = item.color
                    val r = item.bounds
                    canvas.drawRect(r, stickyPaint)
                    if (item.text.isNotEmpty()) {
                        val layout = StaticLayout.Builder.obtain(item.text, 0, item.text.length, stickyText, (r.width() - 36f).toInt().coerceAtLeast(10))
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setMaxLines(12).build()
                        canvas.save(); canvas.translate(r.left + 18f, r.top + 18f); layout.draw(canvas); canvas.restore()
                    }
                }
            }
            if (moved) canvas.restore()
        }
    }

    /** Search hits, in page coordinates. */
    fun drawHighlights(canvas: Canvas, rects: List<RectF>, active: RectF?) {
        for (r in rects) canvas.drawRoundRect(r, 4f, 4f, if (r == active) activeHighlight else highlightPaint)
    }

    /** Renders a whole page into [canvas] scaled by [scale]. Used by export and thumbnails. */
    fun renderPage(canvas: Canvas, page: Page, scale: Float, pdf: Bitmap?) {
        val m = Matrix().apply { setScale(scale, scale) }
        canvas.save()
        canvas.concat(m)
        drawPaper(canvas, page, pdf, shadow = false)
        drawItems(canvas, page.items.map { if (it is TapeItem && it.revealed) it.copy(revealed = false) else it }, m)
        canvas.restore()
    }
}
