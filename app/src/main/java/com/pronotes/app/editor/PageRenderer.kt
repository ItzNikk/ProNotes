package com.pronotes.app.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
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
    // Limited by memory, not by page count, so large pages still stay cached.
    private val cache = object : LruCache<Int, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 6).coerceIn(32L * 1024 * 1024, 160L * 1024 * 1024).toInt(),
    ) {
        override fun sizeOf(key: Int, value: Bitmap) = value.allocationByteCount
    }

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
        // Reuse a cached page when it is already sharp enough for this size.
        if (useCache) cache.get(index)?.let { if (it.width >= width * 0.85f) return it }
        val r = open() ?: return null
        if (index !in 0 until r.pageCount) return null
        return r.openPage(index).use { page ->
            val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            if (useCache) cache.put(index, bmp)
            // Upload to the GPU now, off the UI thread, so the first frame with this page does not stutter.
            if (useCache) bmp.prepareToDraw()
            bmp
        }
    }

    /**
     * Renders only [region] of a page (page units; the whole page is [pageW] units wide) into an
     * [outW] x [outH] bitmap. Used for sharp detail when zoomed in, without a giant full-page bitmap.
     */
    @Synchronized
    fun renderRegion(index: Int, pageW: Float, region: RectF, outW: Int, outH: Int): Bitmap? {
        val r = open() ?: return null
        if (index !in 0 until r.pageCount || outW <= 0 || outH <= 0) return null
        return r.openPage(index).use { page ->
            val u2p = page.width / pageW                    // page units -> PDF points
            val s = outW / (region.width() * u2p)           // PDF points -> bitmap pixels
            val m = android.graphics.Matrix().apply {
                setTranslate(-region.left * u2p, -region.top * u2p)
                postScale(s, s)
            }
            val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, m, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bmp.prepareToDraw()
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

    /** Highlighter ink goes under pen ink, like a real highlighter under printed text. */
    var highlighterBehind: Boolean = true

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    /** Shows PDF pages inverted (light text on dark) for night reading. */
    var darkPdf: Boolean = false
    private val invertPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = android.graphics.ColorMatrixColorFilter(
            floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f),
        )
    }

    private val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val softShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
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

    private var currentBg = Color.WHITE
    private val isDark get() = Color.luminance(currentBg) < 0.4f

    private val coverPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** Sharp zoomed-in part of a PDF page, drawn over the base page bitmap. */
    fun drawPdfDetail(canvas: Canvas, bmp: Bitmap, region: RectF) {
        canvas.drawBitmap(bmp, null, region, if (darkPdf) invertPaint else bitmapPaint)
    }

    /** A notebook cover drawn as a full page, so it can be written on. */
    fun drawCover(canvas: Canvas, r: RectF, spec: String) {
        val parts = spec.split(':')
        if (parts.firstOrNull() == "image") {
            val bmp = image(parts.drop(1).joinToString(":"))
            if (bmp != null) {
                // Centre-crop the photo to the page.
                val s = maxOf(r.width() / bmp.width, r.height() / bmp.height)
                val w = r.width() / s; val h = r.height() / s
                val src = android.graphics.Rect(
                    ((bmp.width - w) / 2).toInt(), ((bmp.height - h) / 2).toInt(),
                    ((bmp.width + w) / 2).toInt(), ((bmp.height + h) / 2).toInt(),
                )
                canvas.drawBitmap(bmp, src, r, bitmapPaint)
                return
            }
        }
        val color = (parts.lastOrNull()?.toLongOrNull() ?: 0xFF3D6CF4).toInt()
        coverPaint.shader = null
        coverPaint.color = color
        canvas.drawRect(r, coverPaint)
        val light = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = 0x47FFFFFF }
        val w = r.width(); val h = r.height()
        val pattern = if (parts.firstOrNull() == "pattern") parts.getOrNull(1) else null
        drawCoverPattern(canvas, pattern, color, w, h, light)
        // Spine shading and elastic band, like the library cover.
        coverPaint.shader = android.graphics.LinearGradient(0f, 0f, w * 0.08f, 0f, 0x38000000, 0x00000000, android.graphics.Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w * 0.08f, h, coverPaint)
        coverPaint.shader = null
        coverPaint.color = 0x29000000
        canvas.drawRect(w * 0.82f, 0f, w * 0.87f, h, coverPaint)
    }

    /** Cover textures, all generated in code so they stay sharp at any size. */
    private fun drawCoverPattern(canvas: Canvas, pattern: String?, color: Int, w: Float, h: Float, light: Paint) {
        val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = 0x24000000 }
        val rnd = java.util.Random(pattern.hashCode().toLong() * 31 + color)
        when (pattern) {
            "dots" -> {
                val st = w / 8; var y = st / 2; var row = 0
                while (y < h) {
                    var x = if (row % 2 == 0) st / 2 else st
                    while (x < w) { canvas.drawCircle(x, y, st * 0.18f, light); x += st }
                    y += st; row++
                }
            }
            "grid" -> {
                light.strokeWidth = w / 220f
                val st = w / 7
                var x = 0f
                while (x < w) { canvas.drawLine(x, 0f, x, h, light); x += st }
                var y = 0f
                while (y < h) { canvas.drawLine(0f, y, w, y, light); y += st }
            }
            "waves" -> {
                light.style = Paint.Style.STROKE; light.strokeWidth = h / 40
                val st = h / 9; var y = 0f
                while (y < h + st) {
                    val path = Path(); path.moveTo(0f, y)
                    var x = 0f
                    while (x <= w) { path.lineTo(x, y + kotlin.math.sin(x / w * 12.56f) * st * 0.3f); x += w / 60 }
                    canvas.drawPath(path, light); y += st
                }
            }
            "scales" -> {
                light.style = Paint.Style.STROKE; light.strokeWidth = w / 60
                val r = w / 6; var y = 0f; var row = 0
                while (y < h + r) {
                    var x = if (row % 2 == 0) 0f else r
                    while (x < w + r) { canvas.drawCircle(x, y, r, light); x += r * 2 }
                    y += r * 0.9f; row++
                }
            }
            "linen" -> {
                val a = Paint().apply { this.color = 0x14FFFFFF; strokeWidth = w / 300 }
                val b = Paint().apply { this.color = 0x10000000; strokeWidth = w / 300 }
                var y = 0f; while (y < h) { canvas.drawLine(0f, y, w, y, a); y += w / 120 }
                var x = 0f; while (x < w) { canvas.drawLine(x, 0f, x, h, b); x += w / 120 }
            }
            "leather" -> {
                // Pebbled grain plus stitched border.
                repeat(1400) { canvas.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, w / 180 + rnd.nextFloat() * w / 160, if (it % 2 == 0) dark else light.apply { alpha = 26 }) }
                val stitch = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = w / 160; this.color = 0x66FFFFFF
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(w / 40, w / 60), 0f)
                }
                canvas.drawRoundRect(RectF(w * 0.06f, h * 0.04f, w * 0.94f, h * 0.96f), w * 0.04f, w * 0.04f, stitch)
            }
            "kraft" -> {
                val fibre = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = w / 400 }
                repeat(900) {
                    fibre.color = if (rnd.nextBoolean()) 0x22FFFFFF else 0x1A000000
                    val x = rnd.nextFloat() * w; val y = rnd.nextFloat() * h; val l = w / 30 * rnd.nextFloat()
                    canvas.drawLine(x, y, x + l, y + l * (rnd.nextFloat() - 0.5f), fibre)
                }
            }
            "marble" -> {
                val vein = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
                repeat(14) {
                    vein.color = if (it % 3 == 0) 0x55FFFFFF else 0x22FFFFFF; vein.strokeWidth = w / 200 + rnd.nextFloat() * w / 90
                    val path = Path(); var x = rnd.nextFloat() * w; var y = -h * 0.1f
                    path.moveTo(x, y)
                    while (y < h * 1.1f) { x += (rnd.nextFloat() - 0.45f) * w / 6; y += h / 14; path.lineTo(x, y) }
                    canvas.drawPath(path, vein)
                }
            }
            "gradient" -> {
                val g = Paint().apply {
                    shader = android.graphics.LinearGradient(0f, 0f, w, h, intArrayOf(0x55FFFFFF, 0x00FFFFFF, 0x33000000), null, android.graphics.Shader.TileMode.CLAMP)
                }
                canvas.drawRect(0f, 0f, w, h, g)
            }
            "geometric" -> {
                val st = w / 4
                var y = 0f; var row = 0
                while (y < h) {
                    var x = if (row % 2 == 0) 0f else -st / 2
                    while (x < w) {
                        val path = Path().apply { moveTo(x, y + st); lineTo(x + st / 2, y); lineTo(x + st, y + st); close() }
                        canvas.drawPath(path, if ((row + (x / st).toInt()) % 2 == 0) light.apply { alpha = 38 } else dark)
                        x += st / 2
                    }
                    y += st; row++
                }
            }
            "terrazzo" -> {
                val chip = Paint(Paint.ANTI_ALIAS_FLAG)
                val tones = intArrayOf(0x66FFFFFF, 0x33000000, 0x55FFD6A5, 0x55A0C4FF, 0x55FFADAD)
                repeat(180) {
                    chip.color = tones[rnd.nextInt(tones.size)]
                    val cx = rnd.nextFloat() * w; val cy = rnd.nextFloat() * h; val s = w / 60 + rnd.nextFloat() * w / 30
                    val path = Path().apply {
                        moveTo(cx, cy - s); lineTo(cx + s * rnd.nextFloat() + s / 2, cy); lineTo(cx, cy + s * 0.8f); lineTo(cx - s, cy + s * 0.2f); close()
                    }
                    canvas.drawPath(path, chip)
                }
            }
            "plaid" -> {
                val band = Paint().apply { this.color = 0x22FFFFFF }
                val thin = Paint().apply { this.color = 0x33000000 }
                val st = w / 5
                var x = 0f; while (x < w) { canvas.drawRect(x, 0f, x + st * 0.35f, h, band); canvas.drawRect(x + st * 0.6f, 0f, x + st * 0.65f, h, thin); x += st }
                var y = 0f; while (y < h) { canvas.drawRect(0f, y, w, y + st * 0.35f, band); canvas.drawRect(0f, y + st * 0.6f, w, y + st * 0.65f, thin); y += st }
            }
            "spiral" -> {
                // Spiral binding down the left edge.
                val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = w / 90; this.color = 0xFFB8BEC8.toInt() }
                val hole = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = 0x55000000 }
                var y = h * 0.05f
                while (y < h * 0.96f) { canvas.drawCircle(w * 0.07f, y, w / 70, hole); canvas.drawArc(RectF(-w * 0.01f, y - w / 28, w * 0.09f, y + w / 28), 200f, 220f, false, ring); y += h / 22 }
            }
            "stripes" -> {
                light.strokeWidth = w / 14
                val st = w / 5
                var x = -h
                while (x < w) { canvas.drawLine(x, h, x + h, 0f, light); x += st }
            }
            else -> Unit
        }
    }

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
        val bg = if (page.pdfPage >= 0) Color.WHITE else page.paperColor ?: paperColor
        currentBg = if (page.pdfPage >= 0 && darkPdf) Color.BLACK else bg
        if (shadow) {
            // A live blur shadow is redrawn every frame while scrolling; a few soft layers look alike and are far cheaper.
            for (k in 1..4) {
                softShadow.alpha = 14
                val g = k * 5f
                canvas.drawRoundRect(rect.left - g, rect.top - g + 6f, rect.right + g, rect.bottom + g + 6f, g, g, softShadow)
            }
        }
        pagePaint.color = bg
        canvas.drawRect(rect, pagePaint)
        page.cover?.let { drawCover(canvas, rect, it); return }
        // Custom page: the user's own picture as the background (centre-cropped).
        page.bgImage?.let { name -> drawCover(canvas, rect, "image:$name"); return }
        if (pdf != null) {
            // Dark PDF pages (light text on a dark slide) need highlights blended the other way.
            currentBg = if (isDarkBitmap(pdf) != darkPdf) Color.BLACK else Color.WHITE
            canvas.drawBitmap(pdf, null, rect, if (darkPdf) invertPaint else bitmapPaint)
            return
        }
        if (page.pdfPage >= 0) return
        currentBg = bg
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
        transform: Matrix? = null,
        transformIds: Set<Long> = emptySet(),
    ) {
        // The part of the page currently on screen, in page coordinates. Anything outside is skipped.
        val clip = canvas.clipBounds
        visible.set(clip.left.toFloat(), clip.top.toFloat(), clip.right.toFloat(), clip.bottom.toFloat())
        val parts = partsOf(items)
        if (parts.hasHighlight) {
            drawSeq(canvas, parts.fills, worldToScreen, transform, transformIds)
            // Highlights blend with the page so text always stays on top: multiply keeps dark text dark
            // on light pages, lighten keeps light text bright on dark pages.
            // The offscreen layer covers only the visible area, not the whole screen.
            val layer = canvas.saveLayer(visible, if (isDark) lightenPaint else multiplyPaint)
            drawSeq(canvas, parts.highlights, worldToScreen, transform, transformIds)
            canvas.restoreToCount(layer)
            drawSeq(canvas, parts.rest, worldToScreen, transform, transformIds)
        } else drawSeq(canvas, items, worldToScreen, transform, transformIds)
    }

    private val visible = RectF()

    /** Items split into fills / highlights / the rest, remembered per list so we do not rebuild it every frame. */
    private class Parts(val fills: List<PageItem>, val highlights: List<PageItem>, val rest: List<PageItem>) {
        val hasHighlight get() = highlights.isNotEmpty()
    }

    private val partsCache = java.util.IdentityHashMap<List<PageItem>, Parts>()

    private fun partsOf(items: List<PageItem>): Parts {
        if (!highlighterBehind) return Parts(emptyList(), emptyList(), items)
        partsCache[items]?.let { return it }
        if (partsCache.size > 16) partsCache.clear()
        val fills = ArrayList<PageItem>(); val hl = ArrayList<PageItem>(); val rest = ArrayList<PageItem>()
        for (it in items) when {
            it is FillItem -> fills += it
            it is InkItem && it.isHighlighter -> hl += it
            else -> rest += it
        }
        return Parts(fills, hl, rest).also { partsCache[items] = it }
    }

    private fun onScreen(item: PageItem): Boolean = when (item) {
        is InkItem -> {
            val b = item.localBounds
            RectF.intersects(visible, tmp.apply { set(b.left + item.dx, b.top + item.dy, b.right + item.dx, b.bottom + item.dy) })
        }
        else -> true
    }
    private val tmp = RectF()
    private val lightenPaint = Paint().apply {
        if (android.os.Build.VERSION.SDK_INT >= 29) blendMode = android.graphics.BlendMode.LIGHTEN
        else xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.LIGHTEN)
    }
    private val darkness = java.util.WeakHashMap<Bitmap, Boolean>()

    /** Average brightness of a PDF page, sampled on a 12 x 12 grid and remembered per bitmap. */
    private fun isDarkBitmap(b: Bitmap): Boolean = darkness.getOrPut(b) {
        if (b.config == Bitmap.Config.HARDWARE) return@getOrPut false
        var sum = 0.0
        for (i in 0 until 12) for (j in 0 until 12) {
            sum += Color.luminance(b.getPixel(((i + 0.5f) / 12f * b.width).toInt(), ((j + 0.5f) / 12f * b.height).toInt()))
        }
        sum / 144.0 < 0.4
    }
    private val multiplyPaint = Paint().apply {
        if (android.os.Build.VERSION.SDK_INT >= 29) blendMode = android.graphics.BlendMode.MULTIPLY
        else xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.MULTIPLY)
    }

    private fun drawSeq(canvas: Canvas, ordered: List<PageItem>, worldToScreen: Matrix, transform: Matrix?, transformIds: Set<Long>) {
        for (item in ordered) {
            val moved = transform != null && item.id in transformIds
            if (!moved && !onScreen(item)) continue
            if (moved) {
                canvas.save()
                canvas.concat(transform!!)
            }
            when (item) {
                is InkItem -> {
                    canvas.save()
                    canvas.translate(item.dx, item.dy)
                    scratch.set(worldToScreen)
                    if (moved) scratch.preConcat(transform!!)
                    scratch.preTranslate(item.dx, item.dy)
                    strokeRenderer.draw(canvas, item.stroke, scratch)
                    canvas.restore()
                }
                is TextItem -> drawText(canvas, item)
                is FillItem -> {
                    fillPaint.color = item.color
                    canvas.save(); canvas.translate(item.dx, item.dy); canvas.drawPath(item.path, fillPaint); canvas.restore()
                }
                is ImageItem -> {
                    val bmp = image(item.file)
                    if (bmp != null) rotated(canvas, item.raw, item.rot) { canvas.drawBitmap(bmp, null, item.raw, bitmapPaint) }
                }
                is TapeItem -> {
                    tapePaint.color = if (item.revealed) (item.color and 0x00FFFFFF) or (0x40 shl 24) else item.color
                    val tp = item.path
                    if (tp != null) {
                        tapePaint.style = Paint.Style.STROKE; tapePaint.strokeWidth = item.thick
                        tapePaint.strokeCap = Paint.Cap.SQUARE; tapePaint.strokeJoin = Paint.Join.ROUND
                        canvas.save(); canvas.translate(item.x, item.y); canvas.drawPath(tp, tapePaint); canvas.restore()
                        tapePaint.style = Paint.Style.FILL
                        if (moved) canvas.restore()
                        continue
                    }
                    val r = item.raw
                    rotated(canvas, r, item.rot) {
                        canvas.drawRoundRect(r, 6f, 6f, tapePaint)
                        if (!item.revealed) {
                            canvas.save(); canvas.clipRect(r)
                            var x = r.left - r.height()
                            while (x < r.right) { canvas.drawLine(x, r.bottom, x + r.height(), r.top, tapeStripe); x += 28f }
                            canvas.restore()
                        }
                    }
                }
                is StickyItem -> {
                    stickyPaint.color = item.color
                    val r = item.raw
                    rotated(canvas, r, item.rot) {
                        canvas.drawRect(r, stickyPaint)
                        if (item.text.isNotEmpty()) {
                            stickyText.textSize = 30f * (r.width() / 280f).coerceIn(0.3f, 6f)
                            val layout = StaticLayout.Builder.obtain(item.text, 0, item.text.length, stickyText, (r.width() - 36f).toInt().coerceAtLeast(10))
                                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setMaxLines(12).build()
                            canvas.save(); canvas.translate(r.left + 18f, r.top + 18f); layout.draw(canvas); canvas.restore()
                        }
                    }
                }
            }
            if (moved) canvas.restore()
        }
    }

    private inline fun rotated(canvas: Canvas, r: RectF, deg: Float, block: () -> Unit) {
        if (deg == 0f) { block(); return }
        canvas.save(); canvas.rotate(deg, r.centerX(), r.centerY()); block(); canvas.restore()
    }

    fun drawText(canvas: Canvas, item: TextItem) {
        if (item.rot != 0f) { canvas.save(); canvas.rotate(item.rot, item.raw.centerX(), item.raw.centerY()) }
        drawTextUnrotated(canvas, item)
        if (item.rot != 0f) canvas.restore()
    }

    private fun drawTextUnrotated(canvas: Canvas, item: TextItem) {
        applyTextStyle(textPaint, item.size, item.style)
        val b = item.raw
        val g = item.style.gradient
        textPaint.color = item.color
        textPaint.shader = if (g != null) android.graphics.LinearGradient(
            b.left, b.top, b.right, b.bottom, item.color, g, android.graphics.Shader.TileMode.CLAMP,
        ) else null
        val lh = item.size * 1.3f
        item.text.split('\n').forEachIndexed { i, line ->
            val w = textPaint.measureText(line)
            val x = when (item.style.align) {
                TextAlign.LEFT -> b.left
                TextAlign.CENTER -> b.left + (b.width() - w) / 2f
                TextAlign.RIGHT -> b.right - w
            }
            canvas.drawText(line, x, item.y + item.size + i * lh, textPaint)
        }
        textPaint.shader = null
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
