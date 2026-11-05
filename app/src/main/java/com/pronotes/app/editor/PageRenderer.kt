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
import android.util.LruCache
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.pronotes.app.data.PaperStyle
import java.io.File

/** Renders PDF backgrounds with the platform PdfRenderer and keeps a small bitmap cache. */
class PdfPages(private val file: File) {
    private var renderer: PdfRenderer? = null
    private val cache = object : LruCache<Int, Bitmap>(3) {}

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
    fun render(index: Int, width: Int, height: Int): Bitmap? {
        cache.get(index)?.let { return it }
        val r = open() ?: return null
        if (index !in 0 until r.pageCount) return null
        return r.openPage(index).use { page ->
            val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            cache.put(index, bmp)
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

/** Shared drawing code for the live canvas, thumbnails and PDF export. */
class PageRenderer(private val noteDir: File) {
    val strokeRenderer: CanvasStrokeRenderer = CanvasStrokeRenderer.create()

    private val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        setShadowLayer(18f, 0f, 4f, 0x22000000)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD5DEEA.toInt(); strokeWidth = 2f }
    private val marginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66EB5757; strokeWidth = 2f }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC3CCD8.toInt() }
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val images = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val scratch = Matrix()

    fun image(name: String): Bitmap? {
        images.get(name)?.let { return it }
        val f = File(noteDir, name)
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
        canvas.drawRect(rect, if (shadow) shadowPaint else pagePaint)
        if (pdf != null) {
            canvas.drawBitmap(pdf, null, rect, bitmapPaint)
            return
        }
        when (page.paper) {
            PaperStyle.BLANK -> Unit
            PaperStyle.RULED -> {
                var y = 180f
                while (y < page.height - 40f) {
                    canvas.drawLine(0f, y, page.width, y, linePaint); y += 52f
                }
                canvas.drawLine(130f, 0f, 130f, page.height, marginPaint)
            }
            PaperStyle.GRID -> {
                var x = 40f
                while (x < page.width) { canvas.drawLine(x, 0f, x, page.height, linePaint); x += 40f }
                var y = 40f
                while (y < page.height) { canvas.drawLine(0f, y, page.width, y, linePaint); y += 40f }
            }
            PaperStyle.DOTTED -> {
                var y = 40f
                while (y < page.height) {
                    var x = 40f
                    while (x < page.width) { canvas.drawCircle(x, y, 2.6f, dotPaint); x += 40f }
                    y += 40f
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
        hidden: Set<Long> = emptySet(),
        offset: Pair<Float, Float>? = null,
        offsetIds: Set<Long> = emptySet(),
    ) {
        for (item in items) {
            if (item.id in hidden) continue
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
            }
            if (moved) canvas.restore()
        }
    }

    /** Renders a whole page into [canvas] scaled by [scale]. Used by export and thumbnails. */
    fun renderPage(canvas: Canvas, page: Page, scale: Float, pdf: Bitmap?) {
        val m = Matrix().apply { setScale(scale, scale) }
        canvas.save()
        canvas.concat(m)
        drawPaper(canvas, page, pdf, shadow = false)
        drawItems(canvas, page.items, m)
        canvas.restore()
    }
}
