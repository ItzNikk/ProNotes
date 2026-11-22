package com.pronotes.app.editor

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import java.io.File
import android.os.SystemClock
import android.text.TextPaint
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.pronotes.app.R
import com.pronotes.app.data.ScrollDirection
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

internal class LaserStroke(val pts: ArrayList<Pt>, var end: Long = Long.MAX_VALUE)

@SuppressLint("ViewConstructor")
internal class NoteCanvasDocLayer(
    context: Context,
    private val owner: NoteCanvasView,
) : View(context) {
    private val density get() = owner.density

    var lasso: List<Pt>? = null
    var eraserAt: Pair<Float, Float>? = null
    var hoverAt: Pair<Float, Float>? = null
    var shapePreview: List<List<Pt>>? = null
    var tapePreview: RectF? = null
    var tapeFree: List<Pt>? = null
    var writeBoxPreview: RectF? = null
    /** Active writing guide line in Auto-Text or Align Handwriting mode (page space). */
    var guideLinePage: Int = -1
    var guideLineX: Float = -1f
    var guideLineY: Float = -1f

    /** Hover preview: x, y, and (colour, radius in px). */
    var hoverDot: Triple<Float, Float, Pair<Int, Float>>? = null

    private val hoverPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hoverBmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val hoverRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * owner.density
        color = 0x66000000
    }
    private var cachedHoverPath = ""
    private var cachedHoverBmp: Bitmap? = null
    private val hoverDestRect = RectF()

    private fun customHoverBitmap(): Bitmap? {
        val path = owner.settings.customHoverIconPath
        if (path == cachedHoverPath) return cachedHoverBmp
        cachedHoverPath = path
        cachedHoverBmp?.recycle()
        cachedHoverBmp = null
        if (path.isBlank()) return null
        val f = File(path)
        if (!f.exists()) return null
        cachedHoverBmp = runCatching {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.absolutePath, opts)
            var sample = 1
            while (opts.outWidth / sample > 256 || opts.outHeight / sample > 256) sample *= 2
            BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
        }.getOrNull()
        return cachedHoverBmp
    }

    fun clearLasers() {
        lasers.clear()
        invalidate()
    }

    private val lassoHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5.4f * owner.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xCCFFFFFF.toInt()
    }
    private val lassoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.2f * owner.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xFF3D6CF4.toInt()
    }
    private val lassoFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x183D6CF4 }
    private val selHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5.0f * owner.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xCCFFFFFF.toInt()
        pathEffect = DashPathEffect(floatArrayOf(13f * owner.density, 7f * owner.density), 0f)
    }
    private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.0f * owner.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xFF3D6CF4.toInt()
        pathEffect = DashPathEffect(floatArrayOf(13f * owner.density, 7f * owner.density), 0f)
    }
    private val guideBaselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.6f
        strokeCap = Paint.Cap.ROUND
        color = 0xE63D6CF4.toInt()
    }
    private val guideXHeightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        color = 0x773D6CF4
    }
    private val guideBandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x163D6CF4
    }
    private val guidePillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xEE3D6CF4.toInt()
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3D6CF4.toInt() }
    private val handleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val selRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * owner.density
        color = 0xFF3D6CF4.toInt()
    }
    private val eraserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * owner.density
        color = 0x99000000.toInt()
    }
    private val eraserFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF }
    private val previewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * owner.density
        color = 0xFF3D6CF4.toInt()
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val tapePreviewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x993D6CF4.toInt() }
    private val laserCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val laserTipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val pullPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3D6CF4.toInt() }
    private val pullText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f * owner.density
        color = 0xFF5B6475.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val pagePaintBg = Paint().apply { color = 0xFFE9ECF1.toInt() }
    private val pageNumberPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f * owner.density
        textAlign = Paint.Align.CENTER
    }
    private val lasers = ArrayList<LaserStroke>()
    private var antsPhase = 0f
    private var antsAnim: ValueAnimator? = null
    private var selScale = 1f
    private var flashAlpha = 0f
    private val tmpPath = Path()
    private val m = Matrix()

    fun startAnts() {
        antsAnim?.cancel()
        antsAnim = ValueAnimator.ofFloat(0f, 26f * density).apply {
            duration = 600
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                antsPhase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopAnts() {
        antsAnim?.cancel()
        antsAnim = null
    }

    fun popSelection() {
        ValueAnimator.ofFloat(0.92f, 1f).apply {
            duration = 220
            interpolator = OvershootInterpolator(2f)
            addUpdateListener {
                selScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun flashErase() {
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 350
            addUpdateListener {
                flashAlpha = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private var morph: List<Pt>? = null
    private var morphT = 0f

    private var puffs: List<RectF> = emptyList()
    private var puffT = 0f
    private val puffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val puffRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val puffSeeds = FloatArray(64) { kotlin.random.Random(it * 17 + 11).nextFloat() }
    private val greyShades = intArrayOf(
        0xFF94A3B8.toInt(),
        0xFF64748B.toInt(),
        0xFFCBD5E1.toInt(),
        0xFF475569.toInt(),
        0xFF9CA3AF.toInt(),
    )

    /** Scribble-erase: classic grey crumbling dust particles dispersing from erased ink. */
    fun puff(rects: List<RectF>) {
        if (rects.isEmpty()) return
        puffs = rects
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 440
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                puffT = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    puffs = emptyList()
                    invalidate()
                }
            })
            start()
        }
    }

    private fun drawPuffs(canvas: Canvas) {
        if (puffs.isEmpty()) return
        val t = puffT
        val ease = 1f - (1f - t) * (1f - t)
        val fade = (1f - t).coerceIn(0f, 1f)
        puffs.forEachIndexed { rIdx, r ->
            val cx = r.centerX()
            val cy = r.centerY()
            val span = max(r.width(), r.height()).coerceAtLeast(22f * density)

            val ringRadius = (6f * density) + (span.coerceAtMost(120f * density) * 0.45f) * ease
            puffRingPaint.color = 0xFF94A3B8.toInt()
            puffRingPaint.strokeWidth = 1.6f * density * fade
            puffRingPaint.alpha = (fade * 85f).toInt().coerceIn(0, 255)
            canvas.drawCircle(cx, cy, ringRadius, puffRingPaint)

            val rw = r.width().coerceAtLeast(10f * density)
            val rh = r.height().coerceAtLeast(10f * density)
            for (k in 0 until 14) {
                val s1 = puffSeeds[(k + rIdx * 7) % 64]
                val s2 = puffSeeds[(k * 3 + 13 + rIdx) % 64]
                val s3 = puffSeeds[(k * 5 + 29) % 64]
                val ang = k * (2.0 * Math.PI / 14.0) + s1 * 0.7
                val speed = (14f + 30f * s2) * density * ease
                val startX = r.left + s1 * rw
                val startY = r.top + s2 * rh
                val px = startX + cos(ang).toFloat() * speed
                val py = startY + sin(ang).toFloat() * speed - (10f * density * ease)
                val radius = (2.2f + 3.6f * s3) * density * (1f - t * 0.4f)
                puffPaint.color = greyShades[(k + rIdx) % greyShades.size]
                puffPaint.alpha = (fade * (135f + 65f * s3)).toInt().coerceIn(0, 255)
                canvas.drawCircle(px, py, radius, puffPaint)
            }
        }
    }

    /** Circle-and-hold: the ink loop glows, turns into a dashed lasso, then [onDone] selects. */
    fun playLassoMorph(screen: List<Pt>, onDone: () -> Unit) {
        morph = screen
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 420
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                morphT = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    morph = null
                    invalidate()
                    onDone()
                }
            })
            start()
        }
    }

    fun drawMorph(canvas: Canvas) {
        val pts = morph ?: return
        if (pts.size < 2) return
        tmpPath.reset()
        pts.forEachIndexed { i, (x, y) -> if (i == 0) tmpPath.moveTo(x, y) else tmpPath.lineTo(x, y) }
        tmpPath.close()
        val t = morphT
        lassoFill.alpha = (0x14 + 36 * t).toInt().coerceAtMost(255)
        canvas.drawPath(tmpPath, lassoFill)
        lassoPaint.pathEffect = DashPathEffect(floatArrayOf(9f * density * t + 0.1f, 4f * density * t + 0.1f), t * 40f)
        canvas.drawPath(tmpPath, lassoPaint)
        lassoFill.alpha = 0x14
    }

    fun laserBegin(p: Pt) {
        lasers += LaserStroke(arrayListOf(p))
        invalidate()
    }

    fun laserAdd(p: Pt) {
        lasers.lastOrNull()?.pts?.add(p)
        invalidate()
    }

    fun laserEnd(): List<Pt>? {
        val s = lasers.lastOrNull() ?: return null
        s.end = SystemClock.uptimeMillis()
        invalidate()
        return s.pts
    }

    fun laserDropLast() {
        if (lasers.isNotEmpty()) lasers.removeAt(lasers.size - 1)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val r = owner.renderer ?: return
        val desk = owner.deskArgb()
        pagePaintBg.color = desk
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), pagePaintBg)
        r.paperColor = owner.settings.paperTint.argb
        r.highlighterBehind = owner.settings.highlighterBehind
        pageNumberPaint.color = if (Color.luminance(desk) < 0.4f) 0xFFB0B6C0.toInt() else 0xFF7B8391.toInt()
        val view = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val active = owner.host.activeHighlight()
        val loaderPhase = ((SystemClock.uptimeMillis() % 1200L) / 1200f)
        var anyPdfLoading = false
        owner.pages.forEachIndexed { i, p ->
            val sr = RectF(0f, 0f, p.width, p.height)
            owner.pageMatrix(i, m).mapRect(sr)
            if (!p.isWhiteboard && !RectF.intersects(sr, view)) return@forEachIndexed
            if (p.isWhiteboard && i != owner.currentIndex && !RectF.intersects(sr, view)) return@forEachIndexed
            r.darkPdf = owner.settings.darkPdf
            if (p.isWhiteboard) {
                r.drawInfiniteBackdrop(canvas, m, width.toFloat(), height.toFloat(), p)
            }
            canvas.save()
            canvas.concat(m)
            val pdf = if (p.pdfPage >= 0) owner.host.pdfBitmap(p, (p.width * owner.scale).toInt()) { postInvalidate() } else null
            if (p.pdfPage >= 0 && pdf == null) anyPdfLoading = true
            if (!p.isWhiteboard) {
                r.drawPaper(canvas, p, pdf, shadow = true, loaderPhase = loaderPhase)
                owner.host.pdfDetail(i)?.let { d -> r.drawPdfDetail(canvas, d.bmp, d.region) }
            }
            val hl = owner.host.highlights(i)
            if (hl.isNotEmpty()) r.drawHighlights(canvas, hl, active?.takeIf { it.first == i }?.second)
            owner.host.textSelection(i).forEach { canvas.drawRoundRect(it, 4f, 4f, textSelPaint) }
            if (i == owner.selectionPage) r.drawItems(canvas, p.items, m, transform = owner.liveMatrix, transformIds = owner.selection)
            else r.drawItems(canvas, p.items, m)
            tapePreview?.takeIf { i == owner.activePage }?.let { canvas.drawRoundRect(it, 6f, 6f, tapePreviewPaint) }
            tapeFree?.takeIf { i == owner.activePage && it.size > 1 }?.let { pts ->
                tmpPath.reset()
                pts.forEachIndexed { k, (x, y) -> if (k == 0) tmpPath.moveTo(x, y) else tmpPath.lineTo(x, y) }
                tapePreviewPaint.style = Paint.Style.STROKE
                tapePreviewPaint.strokeWidth = 46f
                tapePreviewPaint.strokeJoin = Paint.Join.ROUND
                canvas.drawPath(tmpPath, tapePreviewPaint)
                tapePreviewPaint.style = Paint.Style.FILL
            }
            shapePreview?.takeIf { i == owner.activePage }?.forEach { line ->
                tmpPath.reset()
                line.forEachIndexed { k, (x, y) -> if (k == 0) tmpPath.moveTo(x, y) else tmpPath.lineTo(x, y) }
                previewPaint.strokeWidth = 2f * density / owner.scale
                canvas.drawPath(tmpPath, previewPaint)
            }
            writeBoxPreview?.takeIf { i == owner.activePage }?.let { wb ->
                canvas.drawRoundRect(wb, 12f, 12f, linkBoxFill)
                canvas.drawRoundRect(wb, 12f, 12f, selPaint)
                var gy = wb.top + 56f
                while (gy < wb.bottom - 18f) {
                    canvas.drawLine(wb.left + 16f, gy, wb.right - 16f, gy, linkBoxLine)
                    gy += 44f
                }
            }
            // Writing guide line in Auto-Text mode or Align Handwriting mode so user can write cleanly above it
            val showGuide = owner.settings.toolDefaults.alignGuideLine &&
                (owner.tool == Tool.AUTO_TEXT || (owner.tool == Tool.PEN && (owner.settings.toolDefaults.alignHandwriting || owner.settings.toolDefaults.autoHandwriting)))
            val activeGuidePage = if (guideLinePage >= 0) guideLinePage else owner.currentIndex
            val effectiveGuideY = if (guideLinePage == i && guideLineY > 24f && guideLineY < p.height - 16f) {
                guideLineY
            } else if (guideLinePage < 0 && i == activeGuidePage) {
                owner.defaultGuideBaselineY(p)
            } else {
                -1f
            }
            if (showGuide && i == activeGuidePage && effectiveGuideY > 24f && effectiveGuideY < p.height - 16f) {
                val invScale = (1f / owner.scale.coerceAtLeast(0.25f))
                val gxLeft = 48f.coerceAtMost(p.width * 0.12f)
                val gxRight = (p.width - 48f).coerceAtLeast(gxLeft + 120f)
                val bandH = owner.guideLineBandHeight(p)
                val xHeightY = (effectiveGuideY - bandH).coerceAtLeast(12f)
                // Soft translucent writing lane above the baseline
                canvas.drawRect(gxLeft, xHeightY, gxRight, effectiveGuideY, guideBandPaint)
                // Zoom-independent dashed cap/x-height guide
                guideXHeightPaint.strokeWidth = (1.3f * density * invScale).coerceIn(1.2f, 4.5f)
                guideXHeightPaint.pathEffect = DashPathEffect(
                    floatArrayOf((8f * density * invScale).coerceIn(6f, 24f), (6f * density * invScale).coerceIn(5f, 18f)),
                    0f,
                )
                canvas.drawLine(gxLeft, xHeightY, gxRight, xHeightY, guideXHeightPaint)
                // Crisp baseline right beneath the writing line
                guideBaselinePaint.strokeWidth = (2.2f * density * invScale).coerceIn(2.0f, 6.5f)
                canvas.drawLine(gxLeft, effectiveGuideY, gxRight, effectiveGuideY, guideBaselinePaint)
                // Anchor pill dot on left margin so the active writing line is unmistakable
                val dotR = (4.2f * density * invScale).coerceIn(3.5f, 10f)
                canvas.drawCircle(gxLeft, effectiveGuideY, dotR, guidePillPaint)
            }
            canvas.restore()
            if (!p.isWhiteboard && owner.settings.pageNumbers && owner.pages.size > 1) {
                canvas.drawText("${i + 1}", sr.centerX(), sr.bottom + 18f * density, pageNumberPaint)
            }
        }

        drawLinks(canvas)
        drawPuffs(canvas)
        drawPull(canvas)
        drawSelection(canvas)
        lasso?.let { drawLasso(canvas, it) }
        drawMorph(canvas)
        drawLoopEnd(canvas)
        (eraserAt ?: hoverAt)?.let { (x, y) ->
            val rad = owner.host.eraserRadius()
            canvas.drawCircle(x, y, rad, eraserFill)
            canvas.drawCircle(x, y, rad, eraserPaint)
        }
        drawLaser(canvas)
        hoverDot?.let { (x, y, cs) ->
            val customBmp = customHoverBitmap()
            if (customBmp != null && !customBmp.isRecycled) {
                val half = (owner.settings.hoverIconSizeDp.coerceIn(14f, 64f) * density) / 2f
                hoverDestRect.set(x - half, y - half, x + half, y + half)
                canvas.drawBitmap(customBmp, null, hoverDestRect, hoverBmpPaint)
            } else {
                val rad = (cs.second / 2f).coerceIn(2f * density, 40f * density)
                hoverPaint.color = (cs.first and 0x00FFFFFF) or (0x99 shl 24)
                canvas.drawCircle(x, y, rad, hoverPaint)
                canvas.drawCircle(x, y, rad + 1.5f * density, hoverRing)
            }
        }
        if (flashAlpha > 0f) canvas.drawColor(Color.argb((flashAlpha * 40).toInt(), 61, 108, 244))
        if (anyPdfLoading) postInvalidateDelayed(33L)
    }

    private fun drawPull(canvas: Canvas) {
        if (owner.pull <= 0f || owner.pages.isEmpty()) return
        val progress = (owner.pull / owner.pullTrigger).coerceIn(0f, 1f)
        val vertical = owner.settings.scroll == ScrollDirection.VERTICAL
        val last = owner.pages.size - 1
        val end = RectF(0f, 0f, owner.pages[last].width, owner.pages[last].height)
        owner.pageMatrix(last, m).mapRect(end)
        val cx = if (vertical) width / 2f else end.right + owner.pull / 2f
        val cy = if (vertical) end.bottom + owner.pull / 2f else height / 2f
        val rad = 22f * density * (0.6f + 0.4f * progress)
        pullPaint.alpha = (90 + 165 * progress).toInt()
        canvas.drawCircle(cx, cy, rad, pullPaint)
        val arm = rad * 0.45f
        handleFill.strokeWidth = 3f * density
        canvas.save()
        canvas.rotate(progress * 180f, cx, cy)
        canvas.drawLine(cx - arm, cy, cx + arm, cy, handleFill.apply { style = Paint.Style.STROKE })
        canvas.drawLine(cx, cy - arm, cx, cy + arm, handleFill)
        canvas.restore()
        handleFill.style = Paint.Style.FILL
        val label = context.getString(if (progress >= 1f) R.string.release_to_add_page else R.string.pull_to_add_page)
        canvas.drawText(label, cx, cy + rad + 22f * density, pullText)
    }

    private val outlinePath = Path()
    private val textSelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x553D6CF4 }
    private val linkFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3D6CF4.toInt()
        setShadowLayer(6f, 0f, 2f, 0x40000000)
    }
    private val linkGlyph = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 1.8f * owner.density
        strokeCap = Paint.Cap.ROUND
    }
    private val linkBoxFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x143D6CF4 }
    private val linkBoxLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * owner.density
        color = 0x993D6CF4.toInt()
        pathEffect = DashPathEffect(floatArrayOf(6f * owner.density, 4f * owner.density), 0f)
    }
    private val linkRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF3D6CF4.toInt()
        strokeWidth = 3f * owner.density
    }

    private fun drawLinks(canvas: Canvas) {
        val ends = owner.host.linkEnds()
        val a = FloatArray(2)
        val r = 12f * density
        for (e in ends) {
            if (e.page !in owner.pages.indices) continue
            e.box?.let { b ->
                val sb = RectF(b)
                owner.pageMatrix(e.page, m).mapRect(sb)
                if (RectF.intersects(sb, RectF(0f, 0f, width.toFloat(), height.toFloat()))) {
                    canvas.drawRoundRect(sb, 8f * density, 8f * density, linkBoxFill)
                    canvas.drawRoundRect(sb, 8f * density, 8f * density, linkBoxLine)
                }
            }
            a[0] = e.x
            a[1] = e.y
            owner.pageMatrix(e.page, m).mapPoints(a)
            if (a[0] < -r || a[1] < -r || a[0] > width + r || a[1] > height + r) continue
            canvas.drawCircle(a[0], a[1], r, linkFill)
            canvas.save()
            canvas.rotate(-45f, a[0], a[1])
            val w = 4.2f * density
            val h = 2.6f * density
            canvas.drawRoundRect(a[0] - w - 1.2f * density, a[1] - h, a[0] + 1.2f * density, a[1] + h, h, h, linkGlyph)
            canvas.drawRoundRect(a[0] - 1.2f * density, a[1] - h, a[0] + w + 1.2f * density, a[1] + h, h, h, linkGlyph)
            canvas.restore()
        }
        val f = owner.linkFlash ?: return
        val age = SystemClock.uptimeMillis() - owner.linkFlashAt
        if (age > 1400 || f.first !in owner.pages.indices) {
            owner.linkFlash = null
            return
        }
        a[0] = f.second
        a[1] = f.third
        owner.pageMatrix(f.first, m).mapPoints(a)
        val t = (age % 700) / 700f
        linkRing.alpha = ((1f - t) * 255).toInt()
        canvas.drawCircle(a[0], a[1], (14f + 26f * t) * density, linkRing)
        postInvalidateOnAnimation()
    }

    private val loopHint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * owner.density
        color = 0xFF3D6CF4.toInt()
    }
    private val loopDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3D6CF4.toInt() }

    private fun drawLoopEnd(canvas: Canvas) {
        val ageNow = SystemClock.uptimeMillis()
        val pc = owner.circleDetector.activeCandidate(ageNow, owner.gestureConfig) ?: return
        val age = ageNow - pc.createdAtMs
        if (age > owner.gestureConfig.circleWindowMs) return
        val a = floatArrayOf(pc.endX, pc.endY)
        owner.pageMatrix(pc.page, m).mapPoints(a)
        val armed = owner.circleDetector.isHoldArmed()
        val t = if (armed) 1f else (age % 1100) / 1100f
        loopHint.alpha = if (armed) 230 else ((1f - t) * 200).toInt()
        canvas.drawCircle(a[0], a[1], (7f + 12f * t) * density, loopHint)
        canvas.drawCircle(a[0], a[1], 4.5f * density, loopDot)
        postInvalidateOnAnimation()
    }

    private fun drawSelection(canvas: Canvas) {
        val loop = owner.selectionOutline
        if (loop != null && owner.selection.isNotEmpty() && loop.size > 2) {
            outlinePath.reset()
            loop.forEachIndexed { i, (x, y) -> if (i == 0) outlinePath.moveTo(x, y) else outlinePath.lineTo(x, y) }
            outlinePath.close()
            owner.liveMatrix?.let { outlinePath.transform(it) }
            outlinePath.transform(owner.pageMatrix(owner.selectionPage, m))
            lassoFill.alpha = 0x10
            canvas.drawPath(outlinePath, lassoFill)
            lassoFill.alpha = 0x18
            canvas.drawPath(outlinePath, selHaloPaint)
            canvas.drawPath(outlinePath, selPaint)
        }
        val rotating = owner.grab == NoteCanvasView.Grab.ROTATE && owner.liveMatrix != null
        val wb = (if (rotating) owner.selectionBaseBounds() else owner.selectionPageBounds()) ?: return
        val sb = RectF(wb)
        owner.pageMatrix(owner.selectionPage, m).mapRect(sb)
        canvas.save()
        canvas.scale(selScale, selScale, sb.centerX(), sb.centerY())
        if (rotating) canvas.rotate(owner.liveDeg, sb.centerX(), sb.centerY())
        val rad = 10f * density
        if (loop == null) {
            canvas.drawRoundRect(sb, rad, rad, selHaloPaint)
            canvas.drawRoundRect(sb, rad, rad, selPaint)
        }
        val hr = 6f * density
        val ry = sb.top - owner.rotateHandleOffset()
        canvas.drawLine(sb.centerX(), sb.top, sb.centerX(), ry + hr, handlePaint.apply { strokeWidth = 1.5f * density })
        canvas.drawCircle(sb.centerX(), ry, hr + 2f * density, handleFill)
        canvas.drawCircle(sb.centerX(), ry, hr + 2f * density, selRing)
        canvas.drawArc(RectF(sb.centerX() - hr * 0.6f, ry - hr * 0.6f, sb.centerX() + hr * 0.6f, ry + hr * 0.6f), -60f, 280f, false, selRing)
        for ((x, y) in listOf(sb.left to sb.top, sb.right to sb.top, sb.left to sb.bottom, sb.right to sb.bottom)) {
            canvas.drawCircle(x, y, hr, handleFill)
            canvas.drawCircle(x, y, hr, selRing)
        }
        val pw = 8f * density
        val ph = 3.5f * density
        val hTop = RectF(sb.centerX() - pw, sb.top - ph, sb.centerX() + pw, sb.top + ph)
        val hBot = RectF(sb.centerX() - pw, sb.bottom - ph, sb.centerX() + pw, sb.bottom + ph)
        val hLeft = RectF(sb.left - ph, sb.centerY() - pw, sb.left + ph, sb.centerY() + pw)
        val hRight = RectF(sb.right - ph, sb.centerY() - pw, sb.right + ph, sb.centerY() + pw)
        for (hRect in listOf(hTop, hBot, hLeft, hRight)) {
            canvas.drawRoundRect(hRect, ph, ph, handleFill)
            canvas.drawRoundRect(hRect, ph, ph, selRing)
        }
        canvas.restore()
        if (rotating) {
            pullText.color = 0xFF3D6CF4.toInt()
            canvas.drawText("${owner.liveDeg.roundToInt()}°", sb.centerX(), sb.bottom + 24f * density, pullText)
            pullText.color = 0xFF5B6475.toInt()
        } else if (owner.grab == NoteCanvasView.Grab.SCALE && owner.liveMatrix != null) {
            pullText.color = 0xFF3D6CF4.toInt()
            canvas.drawText("${(owner.liveScale * 100).roundToInt()}%", sb.centerX(), sb.bottom + 24f * density, pullText)
            pullText.color = 0xFF5B6475.toInt()
        } else if (
            owner.grab in setOf(
                NoteCanvasView.Grab.STRETCH_L,
                NoteCanvasView.Grab.STRETCH_R,
                NoteCanvasView.Grab.STRETCH_T,
                NoteCanvasView.Grab.STRETCH_B,
            ) && owner.liveMatrix != null
        ) {
            pullText.color = 0xFF3D6CF4.toInt()
            canvas.drawText(
                "${(owner.liveScaleX * 100).roundToInt()}% × ${(owner.liveScaleY * 100).roundToInt()}%",
                sb.centerX(),
                sb.bottom + 24f * density,
                pullText,
            )
            pullText.color = 0xFF5B6475.toInt()
        }
    }

    private fun drawLasso(canvas: Canvas, pts: List<Pt>) {
        if (pts.size < 2) return
        tmpPath.reset()
        pts.forEachIndexed { i, (x, y) -> if (i == 0) tmpPath.moveTo(x, y) else tmpPath.lineTo(x, y) }
        canvas.drawPath(tmpPath, lassoFill)
        val dash = DashPathEffect(floatArrayOf(14f * density, 8f * density), antsPhase)
        lassoHaloPaint.pathEffect = dash
        lassoPaint.pathEffect = dash
        canvas.drawPath(tmpPath, lassoHaloPaint)
        canvas.drawPath(tmpPath, lassoPaint)
    }

    private fun drawLaser(canvas: Canvas) {
        if (lasers.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        val hold = (owner.settings.laserSeconds * 1000).toLong()
        val fade = 500L
        if (owner.touchActive) lasers.forEach { if (it.end != Long.MAX_VALUE && now - it.end <= hold + fade) it.end = now }
        lasers.removeAll { it.end != Long.MAX_VALUE && now - it.end > hold + fade }
        val pt = FloatArray(2)
        for (s in lasers) {
            val age = if (s.end == Long.MAX_VALUE) 0L else now - s.end
            val life = (if (age <= hold) 1f else 1f - (age - hold) / fade.toFloat()).coerceIn(0f, 1f)
            if (s.pts.size < 2) continue
            tmpPath.reset()
            var tipX = 0f
            var tipY = 0f
            s.pts.forEachIndexed { i, (x, y) ->
                pt[0] = x
                pt[1] = y
                owner.viewMatrix.mapPoints(pt)
                tipX = pt[0]
                tipY = pt[1]
                if (i == 0) tmpPath.moveTo(pt[0], pt[1]) else tmpPath.lineTo(pt[0], pt[1])
            }
            // 1. Wide soft outer crimson bloom
            laserCore.color = Color.argb((life * 44).toInt(), 255, 38, 66)
            laserCore.strokeWidth = 17.5f * density
            canvas.drawPath(tmpPath, laserCore)
            // 2. Luminous mid-glow aura
            laserCore.color = Color.argb((life * 105).toInt(), 255, 52, 62)
            laserCore.strokeWidth = 9.5f * density
            canvas.drawPath(tmpPath, laserCore)
            // 3. Saturated laser beam
            laserCore.color = Color.argb((life * 245).toInt(), 255, 64, 58)
            laserCore.strokeWidth = 4.4f * density
            canvas.drawPath(tmpPath, laserCore)
            // 4. Hot glowing white-pink filament core
            laserCore.color = Color.argb((life * 225).toInt(), 255, 236, 238)
            laserCore.strokeWidth = 1.7f * density
            canvas.drawPath(tmpPath, laserCore)
            // 5. Glowing pointer tip flare while actively pointing
            if (s.end == Long.MAX_VALUE || age < 180L) {
                laserTipPaint.color = Color.argb((life * 95).toInt(), 255, 58, 72)
                canvas.drawCircle(tipX, tipY, 10f * density, laserTipPaint)
                laserTipPaint.color = Color.argb((life * 245).toInt(), 255, 245, 246)
                canvas.drawCircle(tipX, tipY, 3.2f * density, laserTipPaint)
            }
        }
        if (lasers.isNotEmpty()) postInvalidateOnAnimation()
    }
}
