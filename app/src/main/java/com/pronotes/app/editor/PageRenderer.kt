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

    /** Currently selected mind-map node (gets an active focus ring). */
    var activeMindNodeId: Long? = null

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
    private val mindLinkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val mindFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; setShadowLayer(10f, 0f, 4f, 0x26000000) }
    private val mindStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val mindTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val mindPath = Path()
    private val wbOriginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val tapeTiles = HashMap<String, Bitmap>()

    /** Repeating tile for patterned and picture tapes; null for plain colour and the classic stripes. */
    private fun tapeShader(item: TapeItem): android.graphics.Shader? {
        val pat = item.pattern
        if (pat == "stripes" || pat == "plain" || pat.isBlank()) return null
        val key = "$pat:${item.color}"
        val tile = tapeTiles[key] ?: run {
            val t = if (pat.startsWith("image:")) {
                val src = image(pat.removePrefix("image:")) ?: return null
                // Scale the picture so it is as tall as the tape.
                val hgt = 92
                Bitmap.createScaledBitmap(src, (src.width * hgt / src.height.toFloat()).toInt().coerceAtLeast(8), hgt, true)
            } else TapePatterns.tile(pat, item.color)
            if (tapeTiles.size > 24) tapeTiles.clear()
            tapeTiles[key] = t
            t
        }
        val sh = android.graphics.BitmapShader(tile, android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT)
        val m = Matrix()
        val th = if (item.pts != null) item.thick else item.raw.height()
        val s = (th / tile.height).coerceAtLeast(0.05f)
        m.setScale(s, s)
        if (item.pts == null) m.postTranslate(item.x, item.y)
        sh.setLocalMatrix(m)
        return sh
    }

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
        val line = page.lineColor ?: if (isDark) 0xFF3A3F48.toInt() else 0xFFD5DEEA.toInt()
        /** Spacing multiplier chosen in the template sheet. */
        val g = page.lineScale.coerceIn(0.5f, 2.5f)
        linePaint.color = line; boldLine.color = line; boxPaint.color = line
        dotPaint.color = if (isDark) 0xFF4A505A.toInt() else 0xFFC3CCD8.toInt()
        labelPaint.color = if (isDark) 0xFF8A93A3.toInt() else 0xFF9AA4B2.toInt()
        val w = page.width; val h = page.height
        fun ruled(start: Float, gap: Float, left: Float = 0f, right: Float = w) {
            var y = start
            while (y < h - 40f) { canvas.drawLine(left, y, right, y, linePaint); y += gap * g }
        }
        fun grid(step0: Float, paint: Paint = linePaint) {
            val step = step0 * g
            var x = step
            while (x < w) { canvas.drawLine(x, 0f, x, h, paint); x += step }
            var y = step
            while (y < h) { canvas.drawLine(0f, y, w, y, paint); y += step }
        }
        when (page.paper) {
            PaperStyle.BLANK -> Unit
            PaperStyle.RULED -> { ruled(180f, 52f); canvas.drawLine(130f, 0f, 130f, h, marginPaint) }
            PaperStyle.NARROW -> { ruled(160f, 38f); canvas.drawLine(110f, 0f, 110f, h, marginPaint) }
            PaperStyle.GRID -> grid(40f)
            PaperStyle.DOTTED -> {
                val st = 40f * g
                var y = st
                while (y < h) {
                    var x = st
                    while (x < w) { canvas.drawCircle(x, y, 2.6f, dotPaint); x += st }
                    y += st
                }
            }
            PaperStyle.COLLEGE -> { ruled(170f, 42f); canvas.drawLine(120f, 0f, 120f, h, marginPaint) }
            PaperStyle.WIDE -> { ruled(190f, 64f); canvas.drawLine(140f, 0f, 140f, h, marginPaint) }
            PaperStyle.LEGAL -> {
                ruled(220f, 50f)
                canvas.drawLine(150f, 0f, 150f, h, marginPaint); canvas.drawLine(158f, 0f, 158f, h, marginPaint)
                canvas.drawLine(0f, 160f, w, 160f, marginPaint)
            }
            PaperStyle.HANDWRITING -> {
                // Three-line penmanship: solid top, dashed middle, bold base line.
                val dash = Paint(linePaint).apply { pathEffect = android.graphics.DashPathEffect(floatArrayOf(14f, 10f), 0f) }
                var y = 180f
                val band = 96f * g
                while (y + band < h - 60f) {
                    canvas.drawLine(70f, y, w - 70f, y, linePaint)
                    canvas.drawLine(70f, y + band / 2f, w - 70f, y + band / 2f, dash)
                    canvas.drawLine(70f, y + band, w - 70f, y + band, boldLine)
                    y += band * 1.6f
                }
            }
            PaperStyle.SMALL_GRID -> grid(24f)
            PaperStyle.GRAPH -> {
                grid(20f)
                grid(100f, Paint(linePaint).apply { strokeWidth = 2.6f })
            }
            PaperStyle.ISOMETRIC -> {
                val st = 46f * g
                val rowH = st * 0.866f
                var row = 0
                var y = rowH
                while (y < h) {
                    var x = if (row % 2 == 0) st / 2f else st
                    while (x < w) { canvas.drawCircle(x, y, 2.4f, dotPaint); x += st }
                    y += rowH; row++
                }
            }
            PaperStyle.HEX -> {
                val r = 34f * g
                val hh = r * 1.732f
                val p = android.graphics.Path()
                val stroke = Paint(linePaint).apply { style = Paint.Style.STROKE }
                var col = 0
                var cx = r
                while (cx < w + r) {
                    var cy = if (col % 2 == 0) hh / 2f else hh
                    while (cy < h + hh) {
                        p.reset()
                        for (k in 0..6) {
                            val a = Math.toRadians(60.0 * k)
                            val px = cx + r * kotlin.math.cos(a).toFloat(); val py = cy + r * kotlin.math.sin(a).toFloat()
                            if (k == 0) p.moveTo(px, py) else p.lineTo(px, py)
                        }
                        canvas.drawPath(p, stroke)
                        cy += hh
                    }
                    cx += r * 1.5f; col++
                }
            }
            PaperStyle.DAILY -> {
                canvas.drawText("DATE", 80f, 110f, labelPaint)
                canvas.drawLine(170f, 112f, w * 0.5f, 112f, linePaint)
                val split = w * 0.55f
                var y = 200f; var hour = 7
                while (y < h - 80f && hour <= 21) {
                    canvas.drawText("%02d:00".format(hour), 70f, y - 8f, labelPaint)
                    canvas.drawLine(160f, y, split - 30f, y, linePaint)
                    y += (h - 280f) / 15f; hour++
                }
                canvas.drawLine(split, 160f, split, h - 80f, boldLine)
                canvas.drawText("TOP 3", split + 30f, 130f, labelPaint)
                for (k in 0 until 3) canvas.drawRoundRect(RectF(split + 30f, 160f + k * 70f, split + 64f, 194f + k * 70f), 6f, 6f, boxPaint)
                canvas.drawText("NOTES", split + 30f, 420f, labelPaint)
                var ny = 470f
                while (ny < h - 80f) { canvas.drawLine(split + 30f, ny, w - 60f, ny, linePaint); ny += 52f * g }
            }
            PaperStyle.MONTHLY -> {
                canvas.drawText("MONTH", 70f, 110f, labelPaint)
                val names = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                val top = 180f; val cw = (w - 100f) / 7f; val ch = (h - top - 60f) / 6f
                names.forEachIndexed { i, n -> canvas.drawText(n, 50f + i * cw + 10f, top - 14f, labelPaint) }
                for (r in 0 until 6) for (c in 0 until 7) canvas.drawRect(RectF(50f + c * cw, top + r * ch, 50f + (c + 1) * cw, top + (r + 1) * ch), boxPaint)
            }
            PaperStyle.MEETING -> {
                listOf("MEETING", "DATE", "ATTENDEES").forEachIndexed { i, l ->
                    val y = 110f + i * 60f
                    canvas.drawText(l, 70f, y, labelPaint); canvas.drawLine(250f, y + 4f, w - 70f, y + 4f, linePaint)
                }
                canvas.drawLine(0f, 300f, w, 300f, boldLine)
                canvas.drawText("NOTES", 70f, 350f, labelPaint)
                var y = 400f
                while (y < h * 0.78f - 20f) { canvas.drawLine(70f, y, w - 70f, y, linePaint); y += 52f * g }
                canvas.drawLine(0f, h * 0.78f, w, h * 0.78f, boldLine)
                canvas.drawText("ACTION ITEMS", 70f, h * 0.78f + 44f, labelPaint)
            }
            PaperStyle.STUDY -> {
                canvas.drawText("TOPIC", 70f, 110f, labelPaint); canvas.drawLine(190f, 114f, w - 70f, 114f, linePaint)
                canvas.drawRoundRect(RectF(60f, 160f, w - 60f, 330f), 18f, 18f, boxPaint)
                canvas.drawText("KEY IDEAS", 84f, 200f, labelPaint)
                var y = 390f
                while (y < h - 330f) { canvas.drawLine(70f, y, w - 70f, y, linePaint); y += 52f * g }
                canvas.drawRoundRect(RectF(60f, h - 300f, w - 60f, h - 70f), 18f, 18f, boxPaint)
                canvas.drawText("REMEMBER", 84f, h - 260f, labelPaint)
            }
            PaperStyle.TWO_COLUMN -> {
                canvas.drawLine(w / 2f, 120f, w / 2f, h - 60f, boldLine)
                ruled(160f, 52f, 40f, w / 2f - 20f); ruled(160f, 52f, w / 2f + 20f, w - 40f)
            }
            PaperStyle.STORYBOARD -> {
                val cw = (w - 120f) / 2; val ch = (h - 160f) / 3
                for (r in 0 until 3) for (c in 0 until 2) {
                    val x = 60f + c * cw; val y = 80f + r * ch
                    canvas.drawRoundRect(RectF(x + 10f, y + 10f, x + cw - 10f, y + ch * 0.68f), 10f, 10f, boxPaint)
                    canvas.drawLine(x + 10f, y + ch * 0.78f, x + cw - 10f, y + ch * 0.78f, linePaint)
                    canvas.drawLine(x + 10f, y + ch * 0.9f, x + cw - 10f, y + ch * 0.9f, linePaint)
                }
            }
            PaperStyle.CHECKLIST -> {
                canvas.drawText("CHECKLIST", 80f, 120f, labelPaint)
                var y = 200f
                while (y < h - 80f) {
                    canvas.drawCircle(98f, y - 16f, 16f, boxPaint)
                    canvas.drawLine(140f, y, w - 80f, y, linePaint)
                    y += 64f * g
                }
            }
            PaperStyle.TABLATURE -> {
                var y = 180f
                val gap = 18f * g
                while (y + 5 * gap < h - 60f) {
                    for (k in 0 until 6) canvas.drawLine(80f, y + k * gap, w - 80f, y + k * gap, linePaint)
                    canvas.drawText("T", 52f, y + gap * 1.6f, labelPaint); canvas.drawText("A", 52f, y + gap * 3f, labelPaint); canvas.drawText("B", 52f, y + gap * 4.4f, labelPaint)
                    y += 5 * gap + 90f
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

    /** Unbounded whiteboard backdrop: infinite grid/dots across the viewport plus a subtle origin frame. */
    fun drawInfiniteBackdrop(canvas: Canvas, pageWorldToScreen: Matrix, viewW: Float, viewH: Float, page: Page) {
        val bg = page.paperColor ?: paperColor
        currentBg = bg
        pagePaint.color = bg
        canvas.drawRect(0f, 0f, viewW, viewH, pagePaint)
        val inv = Matrix()
        if (!pageWorldToScreen.invert(inv)) return
        val world = RectF(0f, 0f, viewW, viewH)
        inv.mapRect(world)

        val line = page.lineColor ?: if (isDark) 0xFF333944.toInt() else 0xFFD9E1EC.toInt()
        val dotCol = if (isDark) 0xFF474E5C.toInt() else 0xFFC0CAD8.toInt()
        val g = page.lineScale.coerceIn(0.5f, 2.5f)
        val baseStep = when (page.paper) {
            PaperStyle.SMALL_GRID -> 28f
            PaperStyle.GRAPH -> 24f
            else -> 44f
        } * g
        val step = if (world.width() / baseStep > 90f) baseStep * 4f else baseStep

        canvas.save()
        canvas.concat(pageWorldToScreen)
        if (page.paper == PaperStyle.GRID || page.paper == PaperStyle.SMALL_GRID || page.paper == PaperStyle.GRAPH) {
            linePaint.color = line
            linePaint.strokeWidth = (if (page.paper == PaperStyle.GRAPH) 1.4f else 1.8f)
            var x = kotlin.math.floor((world.left / step).toDouble()).toFloat() * step
            while (x <= world.right) {
                canvas.drawLine(x, world.top, x, world.bottom, linePaint)
                x += step
            }
            var y = kotlin.math.floor((world.top / step).toDouble()).toFloat() * step
            while (y <= world.bottom) {
                canvas.drawLine(world.left, y, world.right, y, linePaint)
                y += step
            }
        } else {
            dotPaint.color = dotCol
            val radius = (2.2f).coerceAtLeast(1.4f)
            val startX = kotlin.math.floor((world.left / step).toDouble()).toFloat() * step
            val startY = kotlin.math.floor((world.top / step).toDouble()).toFloat() * step
            var y = startY
            while (y <= world.bottom) {
                var x = startX
                while (x <= world.right) {
                    canvas.drawCircle(x, y, radius, dotPaint)
                    x += step
                }
                y += step
            }
        }
        // Subtle origin crosshair and soft starting sheet outline so spatial orientation is always clear.
        wbOriginPaint.color = if (isDark) 0x33FFFFFF else 0x26000000
        canvas.drawRoundRect(RectF(0f, 0f, page.width, page.height), 14f, 14f, wbOriginPaint)
        linePaint.color = if (isDark) 0x44FFFFFF else 0x303D6CF4
        canvas.drawLine(-40f, 0f, 40f, 0f, linePaint)
        canvas.drawLine(0f, -40f, 0f, 40f, linePaint)
        canvas.restore()
    }

    /** IDs of mind-map nodes whose ancestor is collapsed. */
    fun hiddenMindNodeIds(items: List<PageItem>): Set<Long> {
        val nodes = items.filterIsInstance<MindNodeItem>()
        if (nodes.isEmpty()) return emptySet()
        val byId = nodes.associateBy { it.id }
        val hidden = HashSet<Long>()
        for (n in nodes) {
            var cur = n.parentId
            val seen = HashSet<Long>()
            while (cur != null && seen.add(cur)) {
                val p = byId[cur] ?: break
                if (p.collapsed) {
                    hidden += n.id
                    break
                }
                cur = p.parentId
            }
        }
        return hidden
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
        val hiddenNodes = hiddenMindNodeIds(items)
        val nodes = items.filterIsInstance<MindNodeItem>().filter { it.id !in hiddenNodes }
        if (nodes.isNotEmpty()) drawMindConnectors(canvas, items, nodes, transform, transformIds)
        val parts = partsOf(items)
        if (parts.hasHighlight) {
            drawSeq(canvas, parts.fills, worldToScreen, transform, transformIds, hiddenNodes, items)
            // Highlights blend with the page so text always stays on top: multiply keeps dark text dark
            // on light pages, lighten keeps light text bright on dark pages.
            // The offscreen layer covers only the visible area, not the whole screen.
            val layer = canvas.saveLayer(visible, if (isDark) lightenPaint else multiplyPaint)
            drawSeq(canvas, parts.highlights, worldToScreen, transform, transformIds, hiddenNodes, items)
            canvas.restoreToCount(layer)
            drawSeq(canvas, parts.rest, worldToScreen, transform, transformIds, hiddenNodes, items)
        } else drawSeq(canvas, items, worldToScreen, transform, transformIds, hiddenNodes, items)
    }

    private fun drawMindConnectors(
        canvas: Canvas,
        allItems: List<PageItem>,
        visibleNodes: List<MindNodeItem>,
        transform: Matrix?,
        transformIds: Set<Long>,
    ) {
        val map = allItems.filterIsInstance<MindNodeItem>().associateBy { it.id }
        val pt = FloatArray(2)
        for (child in visibleNodes) {
            val pid = child.parentId ?: continue
            val parent = map[pid] ?: continue
            var px = parent.x + parent.w / 2f
            var py = parent.y + parent.h / 2f
            var cx = child.x + child.w / 2f
            var cy = child.y + child.h / 2f
            if (transform != null && parent.id in transformIds) {
                pt[0] = px; pt[1] = py; transform.mapPoints(pt); px = pt[0]; py = pt[1]
            }
            if (transform != null && child.id in transformIds) {
                pt[0] = cx; pt[1] = cy; transform.mapPoints(pt); cx = pt[0]; cy = pt[1]
            }
            val dx = (cx - px) * 0.45f
            mindPath.reset()
            mindPath.moveTo(px, py)
            mindPath.cubicTo(px + dx, py, cx - dx, cy, cx, cy)
            mindLinkPaint.color = (child.color and 0x00FFFFFF) or (0xB8 shl 24)
            mindLinkPaint.strokeWidth = 4.2f
            canvas.drawPath(mindPath, mindLinkPaint)
        }
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

    private fun drawSeq(
        canvas: Canvas,
        ordered: List<PageItem>,
        worldToScreen: Matrix,
        transform: Matrix?,
        transformIds: Set<Long>,
        hiddenNodes: Set<Long>,
        allItems: List<PageItem>,
    ) {
        for (item in ordered) {
            if (item is MindNodeItem && item.id in hiddenNodes) continue
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
                    // Patterned or picture tape: a repeating tile used as the paint's shader.
                    tapePaint.shader = if (item.revealed) null else tapeShader(item)
                    val tp = item.path
                    if (tp != null) {
                        tapePaint.style = Paint.Style.STROKE; tapePaint.strokeWidth = item.thick
                        tapePaint.strokeCap = Paint.Cap.SQUARE; tapePaint.strokeJoin = Paint.Join.ROUND
                        canvas.save(); canvas.translate(item.x, item.y); canvas.drawPath(tp, tapePaint); canvas.restore()
                        tapePaint.style = Paint.Style.FILL
                        tapePaint.shader = null
                        if (moved) canvas.restore()
                        continue
                    }
                    val r = item.raw
                    rotated(canvas, r, item.rot) {
                        canvas.drawRoundRect(r, 6f, 6f, tapePaint)
                        if (!item.revealed && item.pattern == "stripes") {
                            canvas.save(); canvas.clipRect(r)
                            var x = r.left - r.height()
                            while (x < r.right) { canvas.drawLine(x, r.bottom, x + r.height(), r.top, tapeStripe); x += 28f }
                            canvas.restore()
                        }
                    }
                    tapePaint.shader = null
                }
                is StickyItem -> {
                    stickyPaint.color = item.color
                    val r = item.raw
                    rotated(canvas, r, item.rot) {
                        canvas.drawRoundRect(r, 16f, 16f, stickyPaint)
                        // Subtle darker header bar + pin/fold badges
                        fillPaint.color = 0x16000000
                        val barH = minOf(r.height(), 28f)
                        canvas.drawRoundRect(RectF(r.left, r.top, r.right, r.top + barH), 16f, 16f, fillPaint)
                        if (item.pinned || item.collapsed) {
                            stickyText.textSize = 16f
                            val tag = listOfNotNull(if (item.pinned) "📌" else null, if (item.collapsed) "▸" else null).joinToString(" ")
                            canvas.drawText(tag, r.right - 36f, r.top + 20f, stickyText)
                        }
                        if (item.text.isNotEmpty()) {
                            stickyText.typeface = when (item.font) {
                                "SERIF" -> android.graphics.Typeface.SERIF
                                "MONO" -> android.graphics.Typeface.MONOSPACE
                                "HAND" -> android.graphics.Typeface.create("cursive", android.graphics.Typeface.NORMAL)
                                else -> android.graphics.Typeface.SANS_SERIF
                            }
                            if (item.collapsed) {
                                stickyText.textSize = 22f
                                val oneLine = item.text.lineSequence().firstOrNull()?.take(28) ?: ""
                                canvas.drawText(oneLine, r.left + 16f, r.top + 36f, stickyText)
                            } else {
                                stickyText.textSize = item.fontSize.coerceIn(14f, 64f) * (r.width() / 280f).coerceIn(0.4f, 4f)
                                val layout = StaticLayout.Builder.obtain(item.text, 0, item.text.length, stickyText, (r.width() - 36f).toInt().coerceAtLeast(10))
                                    .setAlignment(Layout.Alignment.ALIGN_NORMAL).setMaxLines(14).build()
                                canvas.save(); canvas.translate(r.left + 18f, r.top + barH + 10f); layout.draw(canvas); canvas.restore()
                            }
                        }
                    }
                }
                is MindNodeItem -> drawMindNode(canvas, item, allItems)
            }
            if (moved) canvas.restore()
        }
    }

    private fun drawMindNode(canvas: Canvas, item: MindNodeItem, allItems: List<PageItem>) {
        val r = RectF(item.x, item.y, item.x + item.w, item.y + item.h)
        val isRoot = item.parentId == null
        val isSelected = item.id == activeMindNodeId
        mindFillPaint.color = item.color
        mindStrokePaint.color = if (isSelected) 0xFFFFD60A.toInt() else (0x55FFFFFF)
        mindStrokePaint.strokeWidth = if (isSelected) 5.5f else if (isRoot) 3.5f else 2f

        when (item.shape) {
            "diamond" -> {
                mindPath.reset()
                mindPath.moveTo(r.centerX(), r.top)
                mindPath.lineTo(r.right, r.centerY())
                mindPath.lineTo(r.centerX(), r.bottom)
                mindPath.lineTo(r.left, r.centerY())
                mindPath.close()
                canvas.drawPath(mindPath, mindFillPaint)
                canvas.drawPath(mindPath, mindStrokePaint)
            }
            "circle" -> {
                canvas.drawOval(r, mindFillPaint)
                canvas.drawOval(r, mindStrokePaint)
            }
            "rect" -> {
                canvas.drawRoundRect(r, 16f, 16f, mindFillPaint)
                canvas.drawRoundRect(r, 16f, 16f, mindStrokePaint)
            }
            else -> {
                val rad = minOf(r.width(), r.height()) / 2f
                canvas.drawRoundRect(r, rad, rad, mindFillPaint)
                canvas.drawRoundRect(r, rad, rad, mindStrokePaint)
            }
        }

        val lum = Color.luminance(item.color)
        mindTextPaint.color = if (lum > 0.56f) 0xFF1B1F2A.toInt() else Color.WHITE
        mindTextPaint.textSize = if (isRoot) 26f else 22f
        mindTextPaint.isFakeBoldText = isRoot
        val padX = if (item.shape == "diamond") r.width() * 0.22f else 18f
        val availW = (r.width() - padX * 2f).toInt().coerceAtLeast(24)
        val txt = item.label.ifBlank { "Idea" }
        val layout = StaticLayout.Builder.obtain(txt, 0, txt.length, mindTextPaint, availW)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(4)
            .build()
        canvas.save()
        canvas.translate(r.left + (r.width() - availW) / 2f, r.centerY() - layout.height / 2f)
        layout.draw(canvas)
        canvas.restore()

        if (item.collapsed) {
            val hiddenCount = allItems.count { it is MindNodeItem && it.parentId == item.id }
            val badge = if (hiddenCount > 0) "+$hiddenCount" else "+"
            val bx = r.right - 6f
            val by = r.centerY()
            fillPaint.color = 0xFF1B1F2A.toInt()
            canvas.drawCircle(bx, by, 16f, fillPaint)
            mindTextPaint.color = Color.WHITE
            mindTextPaint.textSize = 15f
            mindTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(badge, bx, by + 5.5f, mindTextPaint)
            mindTextPaint.textAlign = Paint.Align.LEFT
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
