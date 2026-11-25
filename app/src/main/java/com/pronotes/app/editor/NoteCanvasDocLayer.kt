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
        strokeWidth = 7.2f * owner.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xE6FFFFFF.toInt()
    }
    private val lassoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6.5f * owner.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xFF3D6CF4.toInt()
    }
    private val lassoFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x183D6CF4 }
    private val selHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f * owner.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xE6FFFFFF.toInt()
        pathEffect = DashPathEffect(floatArrayOf(22f * owner.density, 14f * owner.density), 0f)
    }
    private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6.5f * owner.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xFF3D6CF4.toInt()
        pathEffect = DashPathEffect(floatArrayOf(22f * owner.density, 14f * owner.density), 0f)
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
    private val handleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        setShadowLayer(4f * owner.density, 0f, 1.5f * owner.density, 0x38000000)
    }
    private val handleActiveFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2563EB.toInt()
        setShadowLayer(5f * owner.density, 0f, 2f * owner.density, 0x482563EB)
    }
    private val selRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * owner.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xFF2563EB.toInt()
    }
    private val handleGlyph = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.7f * owner.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xFF2563EB.toInt()
    }
    private val handleGlyphActive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.7f * owner.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }
    private val handleGlyphFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF2563EB.toInt()
    }
    private val handleGlyphActiveFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val rotateArrowPath = Path()
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
    private val previewFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val tapePreviewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x993D6CF4.toInt() }
    private val laserCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val tmpViewRect = RectF()
    private val tmpPageRect = RectF()
    private val tmpPullRect = RectF()
    private val tmpLinkRect = RectF()
    private val tmpSelRect = RectF()
    private val tmpArcRect = RectF()
    private val tmpHandleRect = RectF()
    private val tmpPt2 = FloatArray(2)
    private var cachedGuideScale = -1f
    private var cachedGuideDash: DashPathEffect? = null
    private var cachedLassoPhase = Float.NaN
    private var cachedLassoDash: DashPathEffect? = null
    private var liftSource: List<PageItem>? = null
    private var liftIds: Set<Long> = emptySet()
    private var liftedItems: List<PageItem> = emptyList()
    private var remainingItems: List<PageItem> = emptyList()
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

    private class EraseParticle {
        var active: Boolean = false
        var startX: Float = 0f
        var startY: Float = 0f
        var vx: Float = 0f
        var vy: Float = 0f
        var radius: Float = 0f
        var color: Int = 0
        var secondaryColor: Int = 0
    }

    private val particlePool = Array(128) { EraseParticle() }
    private var activeParticleCount = 0
    private var puffs: List<RectF> = emptyList()
    private var puffT = 0f
    private val puffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val puffRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val puffSeeds = FloatArray(64) { kotlin.random.Random(it * 17 + 11).nextFloat() }

    private val luminousAccentGradient = intArrayOf(
        0xFF818CF8.toInt(), // Indigo
        0xFF38BDF8.toInt(), // Sky blue
        0xFFA78BFA.toInt(), // Violet
        0xFFF472B6.toInt(), // Rose
        0xFF34D399.toInt(), // Emerald
    )

    private fun blendColors(c1: Int, c2: Int, ratio: Float): Int {
        val r0 = ratio.coerceIn(0f, 1f)
        val inv = 1f - r0
        val a = (Color.alpha(c1) * inv + Color.alpha(c2) * r0).toInt().coerceIn(0, 255)
        val r = (Color.red(c1) * inv + Color.red(c2) * r0).toInt().coerceIn(0, 255)
        val g = (Color.green(c1) * inv + Color.green(c2) * r0).toInt().coerceIn(0, 255)
        val b = (Color.blue(c1) * inv + Color.blue(c2) * r0).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }

    /** Scribble-erase: vibrant gradient particles dispersing from erased ink into theme accents. */
    fun puff(
        rects: List<RectF>,
        strokeColors: List<Int> = emptyList(),
        themeAccent: Int = 0xFF6366F1.toInt(),
    ) {
        if (rects.isEmpty()) return
        puffs = rects
        activeParticleCount = 0

        // Populate pre-allocated particle pool (zero allocations during animation)
        val particlesPerRect = (particlePool.size / rects.size).coerceIn(10, 24)
        for ((rIdx, r) in rects.withIndex()) {
            val rw = r.width().coerceAtLeast(12f * density)
            val rh = r.height().coerceAtLeast(12f * density)
            val baseColor = if (strokeColors.isNotEmpty()) {
                val sc = strokeColors[rIdx % strokeColors.size]
                // If the stroke color is very dark or near black, brighten it slightly for luminous burst
                if (Color.red(sc) < 40 && Color.green(sc) < 40 && Color.blue(sc) < 40) {
                    blendColors(sc, themeAccent, 0.45f)
                } else sc
            } else luminousAccentGradient[rIdx % luminousAccentGradient.size]

            val accent = if (themeAccent != 0) themeAccent else luminousAccentGradient[(rIdx + 1) % luminousAccentGradient.size]

            for (k in 0 until particlesPerRect) {
                if (activeParticleCount >= particlePool.size) break
                val p = particlePool[activeParticleCount++]
                p.active = true
                val s1 = puffSeeds[(k + rIdx * 7) % 64]
                val s2 = puffSeeds[(k * 3 + 13 + rIdx) % 64]
                val s3 = puffSeeds[(k * 5 + 29) % 64]
                val ang = (k.toDouble() * (2.0 * Math.PI / particlesPerRect.toDouble()) + s1 * 0.6)
                val speed = (18f + 38f * s2) * density
                p.startX = r.left + s1 * rw
                p.startY = r.top + s2 * rh
                p.vx = cos(ang).toFloat() * speed
                p.vy = sin(ang).toFloat() * speed
                p.radius = (2.4f + 3.8f * s3) * density
                p.color = baseColor
                p.secondaryColor = blendColors(baseColor, accent, 0.55f + 0.35f * s3)
            }
        }

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
                    activeParticleCount = 0
                    invalidate()
                }
            })
            start()
        }
    }

    private fun drawPuffs(canvas: Canvas) {
        if (puffs.isEmpty() || activeParticleCount == 0) return
        val t = puffT
        val ease = 1f - (1f - t) * (1f - t)
        val fade = (1f - t).coerceIn(0f, 1f)

        // Draw soft expanding shockwave rings around erased bounds
        puffs.forEachIndexed { rIdx, r ->
            val cx = r.centerX()
            val cy = r.centerY()
            val span = max(r.width(), r.height()).coerceAtLeast(22f * density)
            val ringRadius = (6f * density) + (span.coerceAtMost(120f * density) * 0.45f) * ease
            val ringColor = if (activeParticleCount > 0) particlePool[(rIdx * 12) % activeParticleCount].secondaryColor else 0xFF818CF8.toInt()
            puffRingPaint.color = ringColor
            puffRingPaint.strokeWidth = 1.6f * density * fade
            puffRingPaint.alpha = (fade * 110f).toInt().coerceIn(0, 255)
            canvas.drawCircle(cx, cy, ringRadius, puffRingPaint)
        }

        // Draw gradient particles
        for (i in 0 until activeParticleCount) {
            val p = particlePool[i]
            if (!p.active) continue
            val px = p.startX + p.vx * ease
            val py = p.startY + p.vy * ease - (12f * density * ease) // slight upward buoyancy
            val currentRadius = p.radius * (1f - t * 0.42f)
            val currentColor = blendColors(p.color, p.secondaryColor, t)
            puffPaint.color = currentColor
            puffPaint.alpha = (fade * 240f).toInt().coerceIn(0, 255)
            canvas.drawCircle(px, py, currentRadius, puffPaint)
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

        drawDashedLasso(canvas, tmpPath)
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
        tmpViewRect.set(0f, 0f, width.toFloat(), height.toFloat())
        val active = owner.host.activeHighlight()
        val loaderPhase = ((SystemClock.uptimeMillis() % 1200L) / 1200f)
        var anyPdfLoading = false
        val lift = owner.liveMatrix?.takeIf {
            owner.mode == NoteCanvasView.Mode.MOVE && owner.grab == NoteCanvasView.Grab.MOVE && !it.isIdentity
        }
        val sourceItems = lift?.let { owner.pages.getOrNull(owner.selectionPage)?.items }
        if (sourceItems != null && (sourceItems !== liftSource || owner.selection != liftIds)) {
            liftSource = sourceItems
            liftIds = owner.selection
            val (selected, remaining) = sourceItems.partition { it.id in liftIds }
            liftedItems = selected
            remainingItems = remaining
        } else if (sourceItems == null && liftSource != null) {
            liftSource = null
            liftIds = emptySet()
            liftedItems = emptyList()
            remainingItems = emptyList()
        }
        owner.visiblePageRange().forEach { i ->
            val p = owner.pages[i]
            tmpPageRect.set(0f, 0f, p.width, p.height)
            owner.pageMatrix(i, m).mapRect(tmpPageRect)
            if (!p.isWhiteboard && !RectF.intersects(tmpPageRect, tmpViewRect)) return@forEach
            if (p.isWhiteboard && i != owner.currentIndex && !RectF.intersects(tmpPageRect, tmpViewRect)) return@forEach
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
            when {
                i != owner.selectionPage -> r.drawItems(canvas, p.items, m)
                sourceItems != null -> r.drawItems(canvas, remainingItems, m)
                else -> r.drawItems(canvas, p.items, m, transform = owner.liveMatrix, transformIds = owner.selection)
            }
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
            shapePreview?.takeIf { i == owner.activePage && it.isNotEmpty() }?.let { lines ->
                val shapeBrush = owner.host.brush(Tool.SHAPE)
                val fill = if (owner.host.shapeKind().closed) owner.host.shapeFill() else null
                if (fill != null) {
                    val outline = lines.first()
                    if (outline.size > 2) {
                        tmpPath.reset()
                        outline.forEachIndexed { k, (x, y) -> if (k == 0) tmpPath.moveTo(x, y) else tmpPath.lineTo(x, y) }
                        tmpPath.close()
                        previewFillPaint.color = fill
                        canvas.drawPath(tmpPath, previewFillPaint)
                    }
                }
                previewPaint.color = shapeBrush.colorIntArgb
                previewPaint.strokeWidth = max(shapeBrush.size, 2f * density / owner.scale.coerceAtLeast(0.25f))
                lines.forEach { line ->
                    if (line.size > 1) {
                        tmpPath.reset()
                        line.forEachIndexed { k, (x, y) -> if (k == 0) tmpPath.moveTo(x, y) else tmpPath.lineTo(x, y) }
                        canvas.drawPath(tmpPath, previewPaint)
                    }
                }
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
                // Zoom-independent dashed cap/x-height guide (cached by zoom to avoid per-frame allocations)
                guideXHeightPaint.strokeWidth = (1.3f * density * invScale).coerceIn(1.2f, 4.5f)
                if (kotlin.math.abs(owner.scale - cachedGuideScale) > 0.01f || cachedGuideDash == null) {
                    cachedGuideScale = owner.scale
                    cachedGuideDash = DashPathEffect(
                        floatArrayOf((8f * density * invScale).coerceIn(6f, 24f), (6f * density * invScale).coerceIn(5f, 18f)),
                        0f,
                    )
                }
                guideXHeightPaint.pathEffect = cachedGuideDash
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
                canvas.drawText("${i + 1}", tmpPageRect.centerX(), tmpPageRect.bottom + 18f * density, pageNumberPaint)
            }
        }

        if (lift != null && sourceItems != null) {
            canvas.save()
            canvas.concat(owner.pageMatrix(owner.selectionPage, m))
            r.drawItems(canvas, liftedItems, m, transform = lift, transformIds = liftIds)
            canvas.restore()
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
        tmpPullRect.set(0f, 0f, owner.pages[last].width, owner.pages[last].height)
        owner.pageMatrix(last, m).mapRect(tmpPullRect)
        val cx = if (vertical) width / 2f else tmpPullRect.right + owner.pull / 2f
        val cy = if (vertical) tmpPullRect.bottom + owner.pull / 2f else height / 2f
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
        val a = tmpPt2
        val r = 12f * density
        for (e in ends) {
            if (e.page !in owner.pages.indices) continue
            e.box?.let { b ->
                tmpLinkRect.set(b)
                owner.pageMatrix(e.page, m).mapRect(tmpLinkRect)
                if (RectF.intersects(tmpLinkRect, tmpViewRect)) {
                    canvas.drawRoundRect(tmpLinkRect, 8f * density, 8f * density, linkBoxFill)
                    canvas.drawRoundRect(tmpLinkRect, 8f * density, 8f * density, linkBoxLine)
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
        val a = tmpPt2
        a[0] = pc.endX
        a[1] = pc.endY
        owner.pageMatrix(pc.page, m).mapPoints(a)
        val armed = owner.circleDetector.isHoldArmed()
        val t = if (armed) 1f else (age % 1100) / 1100f
        loopHint.alpha = if (armed) 230 else ((1f - t) * 200).toInt()
        canvas.drawCircle(a[0], a[1], (7f + 12f * t) * density, loopHint)
        canvas.drawCircle(a[0], a[1], 4.5f * density, loopDot)
        postInvalidateOnAnimation()
    }

    private fun drawSelection(canvas: Canvas) {
        if (owner.selection.isEmpty()) return
        val bounds = owner.selectionPageBounds() ?: return
        owner.pageMatrix(owner.selectionPage, m).mapRect(bounds)
        outlinePath.reset()
        outlinePath.addRoundRect(bounds, 12f * density, 12f * density, Path.Direction.CW)
        drawDashedLasso(canvas, outlinePath)
        if (!owner.selectionTransformEnabled) return
        val rotating = owner.grab == NoteCanvasView.Grab.ROTATE && owner.liveMatrix != null
        val wb = (if (rotating) owner.selectionBaseBounds() else owner.selectionPageBounds()) ?: return
        val sb = tmpSelRect
        sb.set(wb)
        owner.pageMatrix(owner.selectionPage, m).mapRect(sb)
        canvas.save()
        canvas.scale(selScale, selScale, sb.centerX(), sb.centerY())
        if (rotating) canvas.rotate(owner.liveDeg, sb.centerX(), sb.centerY())
        val rad = 10f * density
        canvas.drawRoundRect(sb, rad, rad, selHaloPaint)
        canvas.drawRoundRect(sb, rad, rad, selPaint)

        // Elevated rotate handle button with curved arrow + crisp arrowhead
        val rotR = 11.5f * density
        val rx = sb.centerX()
        val ry = sb.top - owner.rotateHandleOffset()
        canvas.drawLine(rx, sb.top, rx, ry + rotR, handlePaint.apply { strokeWidth = 1.8f * density })
        val rotActive = owner.liveMatrix != null && owner.grab == NoteCanvasView.Grab.ROTATE
        canvas.drawCircle(rx, ry, rotR, if (rotActive) handleActiveFill else handleFill)
        if (!rotActive) canvas.drawCircle(rx, ry, rotR, selRing)
        val iconR = rotR * 0.48f
        val rStroke = if (rotActive) handleGlyphActive else handleGlyph
        val rFill = if (rotActive) handleGlyphActiveFill else handleGlyphFill
        tmpArcRect.set(rx - iconR, ry - iconR, rx + iconR, ry + iconR)
        canvas.drawArc(tmpArcRect, -62f, 268f, false, rStroke)
        // Arrowhead at end of arc (-62 + 268 = 206 deg -> pointing up-right along tangent)
        val tipAngle = Math.toRadians(-62.0)
        val tipX = rx + (iconR * cos(tipAngle)).toFloat()
        val tipY = ry + (iconR * sin(tipAngle)).toFloat()
        val ah = 3.2f * density
        rotateArrowPath.reset()
        rotateArrowPath.moveTo(tipX + ah * 0.55f, tipY + ah * 0.65f)
        rotateArrowPath.lineTo(tipX - ah * 0.85f, tipY + ah * 0.35f)
        rotateArrowPath.lineTo(tipX + ah * 0.25f, tipY - ah * 0.85f)
        rotateArrowPath.close()
        canvas.drawPath(rotateArrowPath, rFill)

        // Corner resize handles with elevated circular pill + subtle diagonal resize cue
        val hr = 7.6f * density
        val scaleActive = owner.liveMatrix != null && owner.grab == NoteCanvasView.Grab.SCALE
        val cFill = if (scaleActive) handleActiveFill else handleFill
        val cGlyph = if (scaleActive) handleGlyphActive else handleGlyph
        fun drawCornerHandle(cx: Float, cy: Float, diagSign: Float) {
            canvas.drawCircle(cx, cy, hr, cFill)
            if (!scaleActive) canvas.drawCircle(cx, cy, hr, selRing)
            val d = hr * 0.34f
            canvas.drawLine(cx - d, cy - d * diagSign, cx + d, cy + d * diagSign, cGlyph)
        }
        drawCornerHandle(sb.left, sb.top, 1f)
        drawCornerHandle(sb.right, sb.top, -1f)
        drawCornerHandle(sb.left, sb.bottom, -1f)
        drawCornerHandle(sb.right, sb.bottom, 1f)

        // Edge stretch handles (clean elevated pill capsules)
        val pw = 9.5f * density
        val ph = 3.6f * density
        fun drawEdgeHandle(l: Float, t: Float, r: Float, b: Float, active: Boolean) {
            tmpHandleRect.set(l, t, r, b)
            canvas.drawRoundRect(tmpHandleRect, ph, ph, if (active) handleActiveFill else handleFill)
            if (!active) canvas.drawRoundRect(tmpHandleRect, ph, ph, selRing)
        }
        val activeGrab = if (owner.liveMatrix != null) owner.grab else null
        drawEdgeHandle(sb.centerX() - pw, sb.top - ph, sb.centerX() + pw, sb.top + ph, activeGrab == NoteCanvasView.Grab.STRETCH_T)
        drawEdgeHandle(sb.centerX() - pw, sb.bottom - ph, sb.centerX() + pw, sb.bottom + ph, activeGrab == NoteCanvasView.Grab.STRETCH_B)
        drawEdgeHandle(sb.left - ph, sb.centerY() - pw, sb.left + ph, sb.centerY() + pw, activeGrab == NoteCanvasView.Grab.STRETCH_L)
        drawEdgeHandle(sb.right - ph, sb.centerY() - pw, sb.right + ph, sb.centerY() + pw, activeGrab == NoteCanvasView.Grab.STRETCH_R)
        canvas.restore()
        val label = when {
            rotating -> "${owner.liveDeg.roundToInt()}\u00B0"
            owner.liveMatrix == null -> null
            owner.grab == NoteCanvasView.Grab.SCALE -> "${(owner.liveScale * 100).roundToInt()}%"
            owner.grab in setOf(NoteCanvasView.Grab.STRETCH_L, NoteCanvasView.Grab.STRETCH_R,
                NoteCanvasView.Grab.STRETCH_T, NoteCanvasView.Grab.STRETCH_B) ->
                "${(owner.liveScaleX * 100).roundToInt()}% \u00D7 ${(owner.liveScaleY * 100).roundToInt()}%"
            else -> null
        }
        if (label != null) drawTransformLabel(canvas, label, sb)
    }

    private val transformLabelBounds = RectF()
    private val transformLabelFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xEB000000.toInt() }
    private val transformLabelText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 13f * density
        textAlign = Paint.Align.LEFT
    }

    /** Keep transform feedback beside the selection, clear of its center and handles. */
    private fun drawTransformLabel(canvas: Canvas, label: String, selectionBounds: RectF) {
        val margin = 8f * density
        val pad = 10f * density
        val badgeWidth = transformLabelText.measureText(label) + pad * 2
        val badgeHeight = 30f * density
        val maxX = (width - margin - badgeWidth).coerceAtLeast(margin)
        val maxY = (height - margin - badgeHeight).coerceAtLeast(margin)
        val right = selectionBounds.right + margin
        val left = selectionBounds.left - margin - badgeWidth
        val x = when {
            right <= maxX -> right
            left >= margin -> left
            else -> selectionBounds.right - badgeWidth
        }.coerceIn(margin, maxX)
        val y = (selectionBounds.bottom + margin).coerceIn(margin, maxY)
        transformLabelBounds.set(x, y, x + badgeWidth, y + badgeHeight)
        canvas.drawRoundRect(transformLabelBounds, 10f * density, 10f * density, transformLabelFill)
        val baseline = y + (badgeHeight - transformLabelText.ascent() - transformLabelText.descent()) / 2f
        canvas.drawText(label, x + pad, baseline, transformLabelText)
    }
    private fun drawDashedLasso(canvas: Canvas, path: Path) {
        // Keep the entire contour dashed, including circle-to-lasso conversion.
        if (antsPhase != cachedLassoPhase || cachedLassoDash == null) {
            cachedLassoPhase = antsPhase
            cachedLassoDash = DashPathEffect(floatArrayOf(22f * density, 14f * density), antsPhase)
        }
        lassoHaloPaint.pathEffect = cachedLassoDash
        lassoPaint.pathEffect = cachedLassoDash
        canvas.drawPath(path, lassoHaloPaint)
        canvas.drawPath(path, lassoPaint)
    }
    private fun drawLasso(canvas: Canvas, pts: List<Pt>) {
        if (pts.size < 2) return
        tmpPath.reset()
        pts.forEachIndexed { i, (x, y) -> if (i == 0) tmpPath.moveTo(x, y) else tmpPath.lineTo(x, y) }
        canvas.drawPath(tmpPath, lassoFill)
        drawDashedLasso(canvas, tmpPath)
    }

    private fun drawLaser(canvas: Canvas) {
        if (lasers.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        val hold = (owner.settings.laserSeconds * 1000).toLong()
        val fade = 520L
        if (owner.touchActive) lasers.forEach { if (it.end != Long.MAX_VALUE && now - it.end <= hold + fade) it.end = now }
        lasers.removeAll { it.end != Long.MAX_VALUE && now - it.end > hold + fade }
        for (s in lasers) {
            val age = if (s.end == Long.MAX_VALUE) 0L else now - s.end
            val linearLife = (if (age <= hold) 1f else 1f - (age - hold) / fade.toFloat()).coerceIn(0f, 1f)
            // Cubic smoothstep ease-out fade for silky disappearance
            val life = linearLife * linearLife * (3f - 2f * linearLife)
            val count = s.pts.size
            if (count < 2 || life <= 0.005f) continue
            tmpPath.reset()
            var prevX = 0f
            var prevY = 0f
            for (i in 0 until count) {
                val p = s.pts[i]
                tmpPt2[0] = p.first
                tmpPt2[1] = p.second
                owner.viewMatrix.mapPoints(tmpPt2)
                val sx = tmpPt2[0]
                val sy = tmpPt2[1]
                when (i) {
                    0 -> tmpPath.moveTo(sx, sy)
                    1 -> tmpPath.lineTo((prevX + sx) * 0.5f, (prevY + sy) * 0.5f)
                    else -> tmpPath.quadTo(prevX, prevY, (prevX + sx) * 0.5f, (prevY + sy) * 0.5f)
                }
                prevX = sx
                prevY = sy
            }
            tmpPath.lineTo(prevX, prevY)
            // Bright vivid laser red
            val baseR = 255
            val baseG = 23
            val baseB = 68
            // 1. Wide soft outer bloom
            laserCore.color = Color.argb((life * 44).toInt(), baseR, baseG, baseB)
            laserCore.strokeWidth = 17.0f * density
            canvas.drawPath(tmpPath, laserCore)
            // 2. Luminous crimson mid-glow aura
            laserCore.color = Color.argb((life * 112).toInt(), 255, 45, 75)
            laserCore.strokeWidth = 9.0f * density
            canvas.drawPath(tmpPath, laserCore)
            // 3. Saturated bright red laser beam
            laserCore.color = Color.argb((life * 248).toInt(), baseR, baseG, baseB)
            laserCore.strokeWidth = 4.0f * density
            canvas.drawPath(tmpPath, laserCore)
            // 4. Clean luminous white-rose inner filament
            laserCore.color = Color.argb((life * 230).toInt(), 255, 240, 242)
            laserCore.strokeWidth = 1.5f * density
            canvas.drawPath(tmpPath, laserCore)
        }
        if (lasers.isNotEmpty()) postInvalidateOnAnimation()
    }
}
