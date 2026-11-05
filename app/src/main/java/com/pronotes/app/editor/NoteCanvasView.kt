package com.pronotes.app.editor

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.text.TextPaint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.OverScroller
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.Intersection.intersects
import androidx.ink.geometry.MutableParallelogram
import androidx.ink.geometry.MutableSegment
import androidx.ink.geometry.MutableVec
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor
import com.pronotes.app.data.AppSettings
import com.pronotes.app.data.ScrollDirection
import com.pronotes.app.data.SideButton
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class Tool { PEN, HIGHLIGHTER, ERASER, SHAPE, LASSO, TEXT, IMAGE, STICKY, TAPE, STICKER, LASER }

/** Callbacks from the canvas to the editor state. Page indices refer to the notebook's page list. */
interface CanvasHost {
    fun brush(tool: Tool): Brush
    fun shapeKind(): ShapeKind
    fun eraserRadius(): Float
    fun tapeColor(): Int
    fun beginEdit(page: Int)
    fun setItems(page: Int, items: List<PageItem>)
    fun onSelectionChanged(page: Int, ids: Set<Long>, screenBounds: RectF?)
    fun onTextTap(page: Int, x: Float, y: Float, existing: TextItem?)
    fun onStickyTap(page: Int, x: Float, y: Float, existing: StickyItem?)
    fun onCurrentPage(index: Int)
    fun onPullAddPage()
    fun undo()
    fun redo()
    fun pdfBitmap(page: Page, onReady: () -> Unit): Bitmap?
    fun highlights(page: Int): List<RectF>
    fun activeHighlight(): Pair<Int, RectF>?
}

/**
 * The drawing surface: every page of the notebook laid out in one scrollable document,
 * vertically or horizontally. Jetpack Ink's [InProgressStrokesView] renders wet ink with
 * low latency; everything else is drawn by [DocLayer].
 *
 * Input routing:
 *  - stylus tip -> active tool
 *  - stylus eraser end (Wacom, Surface, USI) -> eraser
 *  - stylus side button held (S Pen, Wacom, USI, Lenovo, Xiaomi...) -> side-button action
 *  - fingers -> scroll / fling / pinch-zoom; 2-finger tap = undo, 3-finger tap = redo
 */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class NoteCanvasView(context: Context, private val host: CanvasHost) : FrameLayout(context) {

    private val layer = DocLayer(context)
    private val wetInk = InProgressStrokesView(context)
    private val predictor = MotionEventPredictor.newInstance(this)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val density = resources.displayMetrics.density
    private val scroller = OverScroller(context, DecelerateInterpolator(1.6f))
    private var velocity: VelocityTracker? = null

    var renderer: PageRenderer? = null
        set(value) { field = value; layer.invalidate() }

    var tool: Tool = Tool.PEN
    var settings = AppSettings()
        set(value) {
            val relayout = field.scroll != value.scroll
            field = value
            if (relayout) { layout(pages); fit(currentIndex) }
            layer.invalidate()
        }

    // ------------------------------------------------------------ document layout
    private var pages: List<Page> = emptyList()
    private var origins = FloatArray(0)      // x0, y0 per page in document units
    private var docW = 0f
    private var docH = 0f
    private val gap = 56f
    var currentIndex = 0
        private set

    // view transform: document -> screen
    private val viewMatrix = Matrix()
    private val inverse = Matrix()
    private var scale = 1f
    private var transX = 0f
    private var transY = 0f
    private var fitted = false
    private var pull = 0f                      // overscroll past the last page, screen px
    private val pullTrigger get() = 110f * density

    // selection lives on a single page
    var selection: Set<Long> = emptySet()
        private set
    private var selectionPage = -1
    private var dragOffset: Pair<Float, Float>? = null

    private val strokePages = HashMap<InProgressStrokeId, Int>()

    init {
        setWillNotDraw(false)
        addView(layer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(wetInk, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        wetInk.addFinishedStrokesListener(object : InProgressStrokesFinishedListener {
            override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                val byPage = strokes.entries.groupBy { strokePages.remove(it.key) ?: currentIndex }
                for ((pi, entries) in byPage) {
                    val p = pages.getOrNull(pi) ?: continue
                    host.beginEdit(pi)
                    updatePage(pi, p.items + entries.map { InkItem(ItemIds.next(), it.value) })
                }
                wetInk.removeFinishedStrokes(strokes.keys)
            }
        })
    }

    /** Replaces the page list (after undo, page add/delete, recolour...). */
    fun setPages(newPages: List<Page>) {
        val structural = newPages.size != pages.size || newPages.zip(pages).any { (a, b) -> a.id != b.id || a.width != b.width }
        val keepSel = selectionPage in newPages.indices && pages.getOrNull(selectionPage)?.id == newPages[selectionPage].id
        pages = newPages
        if (structural) layout(newPages)
        if (!fitted && width > 0) fit(0)
        if (keepSel) {
            val ids = newPages[selectionPage].items.map { it.id }.toSet()
            selection = selection.filter { it in ids }.toSet()
        } else selection = emptySet()
        clamp()
        updateMatrix()
        reportSelection()
    }

    private fun updatePage(index: Int, items: List<PageItem>) {
        val p = pages.getOrNull(index) ?: return
        pages = pages.toMutableList().also { it[index] = p.copy(items = items) }
        host.setItems(index, items)
        layer.invalidate()
    }

    private fun layout(list: List<Page>) {
        origins = FloatArray(list.size * 2)
        val vertical = settings.scroll == ScrollDirection.VERTICAL
        val maxW = list.maxOfOrNull { it.width } ?: 0f
        val maxH = list.maxOfOrNull { it.height } ?: 0f
        var cursor = 0f
        list.forEachIndexed { i, p ->
            if (vertical) {
                origins[i * 2] = (maxW - p.width) / 2f; origins[i * 2 + 1] = cursor; cursor += p.height + gap
            } else {
                origins[i * 2] = cursor; origins[i * 2 + 1] = (maxH - p.height) / 2f; cursor += p.width + gap
            }
        }
        if (vertical) { docW = maxW; docH = max(0f, cursor - gap) } else { docW = max(0f, cursor - gap); docH = maxH }
    }

    private fun ox(i: Int) = origins.getOrElse(i * 2) { 0f }
    private fun oy(i: Int) = origins.getOrElse(i * 2 + 1) { 0f }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (pages.isNotEmpty() && (!fitted || w != oldw)) fit(currentIndex) else { clamp(); updateMatrix() }
    }

    private fun fitScale(i: Int = currentIndex): Float {
        val p = pages.getOrNull(i) ?: return 1f
        val margin = 40f * density
        return if (settings.scroll == ScrollDirection.VERTICAL) min((width - margin) / p.width, 3f)
        else min((width - margin) / p.width, (height - margin) / p.height)
    }

    private fun fit(index: Int) {
        if (width == 0 || pages.isEmpty()) return
        scale = fitScale(index)
        centerOn(index)
        fitted = true
        updateMatrix()
    }

    private fun centerOn(index: Int) {
        val p = pages.getOrNull(index) ?: return
        if (settings.scroll == ScrollDirection.VERTICAL) {
            transY = -oy(index) * scale + 20f * density
            transX = (width - docW * scale) / 2f
        } else {
            transX = (width - p.width * scale) / 2f - ox(index) * scale
            transY = (height - docH * scale) / 2f
        }
        clamp()
    }

    fun scrollToPage(index: Int, animate: Boolean) {
        if (index !in pages.indices || width == 0) return
        val fromX = transX; val fromY = transY
        centerOn(index)
        if (animate) {
            val toX = transX; val toY = transY
            transX = fromX; transY = fromY
            scroller.forceFinished(true)
            scroller.startScroll(fromX.toInt(), fromY.toInt(), (toX - fromX).toInt(), (toY - fromY).toInt(), 420)
            postInvalidateOnAnimation()
        } else updateMatrix()
    }

    private fun clamp(allowPull: Boolean = false) {
        val pad = 24f * density
        val cw = docW * scale; val ch = docH * scale
        val vertical = settings.scroll == ScrollDirection.VERTICAL
        val maxPull = if (allowPull && settings.pullToAddPage) 180f * density else 0f
        // X axis
        val restX = if (cw <= width) (width - cw) / 2f else width - cw - pad
        val maxX = if (cw <= width) restX else pad
        transX = transX.coerceIn(restX - if (!vertical) maxPull else 0f, maxX)
        // Y axis
        val restY = if (ch <= height && !vertical) (height - ch) / 2f else min(height - ch - pad, pad)
        val maxY = if (ch <= height && !vertical) restY else pad
        transY = transY.coerceIn(restY - if (vertical) maxPull else 0f, maxY)
        pull = if (!allowPull) 0f else if (vertical) max(0f, restY - transY) else max(0f, restX - transX)
    }

    private fun updateMatrix() {
        viewMatrix.reset()
        viewMatrix.postScale(scale, scale)
        viewMatrix.postTranslate(transX, transY)
        viewMatrix.invert(inverse)
        val idx = computeCurrent()
        if (idx != currentIndex) { currentIndex = idx; host.onCurrentPage(idx) }
        layer.invalidate()
        reportSelection()
    }

    private fun computeCurrent(): Int {
        if (pages.isEmpty()) return 0
        val (cx, cy) = toDoc(width / 2f, height / 2f)
        val vertical = settings.scroll == ScrollDirection.VERTICAL
        var best = 0; var bestD = Float.MAX_VALUE
        pages.forEachIndexed { i, p ->
            val c = if (vertical) oy(i) + p.height / 2f else ox(i) + p.width / 2f
            val d = abs(c - if (vertical) cy else cx)
            if (d < bestD) { bestD = d; best = i }
        }
        return best
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            transX = scroller.currX.toFloat(); transY = scroller.currY.toFloat()
            clamp(); updateMatrix()
            postInvalidateOnAnimation()
        }
    }

    private fun toDoc(x: Float, y: Float): Pt {
        val a = floatArrayOf(x, y)
        inverse.mapPoints(a)
        return a[0] to a[1]
    }

    private fun pageAt(dx: Float, dy: Float): Int {
        if (pages.isEmpty()) return -1
        val vertical = settings.scroll == ScrollDirection.VERTICAL
        pages.forEachIndexed { i, p ->
            if (dx >= ox(i) && dx <= ox(i) + p.width && dy >= oy(i) && dy <= oy(i) + p.height) return i
        }
        // Between pages: pick the nearest along the scroll axis.
        var best = 0; var bestD = Float.MAX_VALUE
        pages.forEachIndexed { i, p ->
            val d = if (vertical) abs(dy - (oy(i) + p.height / 2f)) else abs(dx - (ox(i) + p.width / 2f))
            if (d < bestD) { bestD = d; best = i }
        }
        return best
    }

    private fun toLocal(x: Float, y: Float, page: Int): Pt {
        val (dx, dy) = toDoc(x, y)
        return (dx - ox(page)) to (dy - oy(page))
    }

    private fun pageMatrix(page: Int, out: Matrix): Matrix {
        out.set(viewMatrix); out.preTranslate(ox(page), oy(page)); return out
    }

    // ------------------------------------------------------------------ input

    private enum class Mode { NONE, INK, ERASE, LASSO, MOVE, LASER, PAN, TAP, SHAPE_DRAG, TAPE, IGNORE }

    private var mode = Mode.NONE
    private var activePointer = -1
    private var activePage = 0
    private var strokeId: InProgressStrokeId? = null
    private var strokeTool = Tool.PEN
    private val points = ArrayList<Pt>()       // page-local points of the current gesture
    private val lassoPoints = ArrayList<Pt>()  // screen points
    private var lastErase: Pt? = null
    private var moveStart: Pt? = null
    private var holdFired = false
    private var lastMovePt: Pt? = null
    private var downTime = 0L
    private var downX = 0f
    private var downY = 0f
    private var sawStylus = false

    // finger gesture state
    private var panLastX = 0f
    private var panLastY = 0f
    private var pinchStartDist = 0f
    private var pinchStartScale = 1f
    private var maxFingers = 0
    private var fingerDownTime = 0L
    private var fingerMoved = false
    private var fingerStartX = 0f
    private var fingerStartY = 0f

    private val holdRunnable = Runnable { onHold() }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = true

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        predictor.record(event)
        val action = normalizeSamsungAction(event)
        val isStylus = (0 until event.pointerCount).any {
            val t = event.getToolType(it)
            t == MotionEvent.TOOL_TYPE_STYLUS || t == MotionEvent.TOOL_TYPE_ERASER
        }
        if (isStylus) sawStylus = true

        val drawsWithFinger = !settings.stylusOnly || !sawStylus
        if (mode == Mode.PAN || (!isStylus && !drawsWithFinger) || (!isStylus && event.pointerCount > 1 && mode != Mode.INK)) {
            if (mode != Mode.PAN && mode != Mode.NONE) cancelCurrent(event)
            handleFingers(event, action)
            return true
        }
        if (!isStylus && event.pointerCount > 1) {
            cancelCurrent(event)
            mode = Mode.PAN
            handleFingers(event, MotionEvent.ACTION_POINTER_DOWN)
            return true
        }
        handleDraw(event, action)
        return true
    }

    /** Old Samsung S Pen firmware reports button-held events with custom action codes 211-214. */
    private fun normalizeSamsungAction(e: MotionEvent): Int = when (e.actionMasked) {
        211 -> MotionEvent.ACTION_DOWN
        212 -> MotionEvent.ACTION_UP
        213 -> MotionEvent.ACTION_MOVE
        214 -> MotionEvent.ACTION_CANCEL
        else -> e.actionMasked
    }

    private fun sideButtonHeld(e: MotionEvent): Boolean {
        if (e.actionMasked in 211..214) return true
        val bs = e.buttonState
        return bs and (MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_STYLUS_SECONDARY or MotionEvent.BUTTON_SECONDARY) != 0
    }

    private fun effectiveTool(e: MotionEvent): Tool {
        if (e.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER) return Tool.ERASER
        if (sideButtonHeld(e)) {
            when (settings.sideButton) {
                SideButton.ERASER -> return Tool.ERASER
                SideButton.LASSO -> return Tool.LASSO
                SideButton.NONE -> Unit
            }
        }
        return tool
    }

    private fun handleDraw(e: MotionEvent, action: Int) {
        if (pages.isEmpty()) return
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                scroller.forceFinished(true)
                requestUnbufferedDispatch(e)
                activePointer = e.getPointerId(0)
                val (ddx, ddy) = toDoc(e.x, e.y)
                activePage = pageAt(ddx, ddy)
                points.clear()
                holdFired = false
                downTime = SystemClock.uptimeMillis(); downX = e.x; downY = e.y
                val (wx, wy) = toLocal(e.x, e.y, activePage)
                points += wx to wy
                lastMovePt = wx to wy
                val t = effectiveTool(e)
                strokeTool = t

                // Touching inside the current selection drags it, whatever the tool.
                if (selection.isNotEmpty() && t != Tool.ERASER) {
                    val sb = selectionPageBounds()
                    val (sx, sy) = toLocal(e.x, e.y, selectionPage)
                    if (sb?.contains(sx, sy) == true) {
                        activePage = selectionPage
                        mode = Mode.MOVE
                        moveStart = sx to sy
                        dragOffset = 0f to 0f
                        return
                    }
                }
                if (selection.isNotEmpty()) clearSelection()

                when (t) {
                    Tool.PEN, Tool.HIGHLIGHTER -> startInk(e, t)
                    Tool.SHAPE -> if (host.shapeKind() == ShapeKind.AUTO) startInk(e, t) else {
                        mode = Mode.SHAPE_DRAG
                        layer.shapePreview = null
                    }
                    Tool.ERASER -> {
                        mode = Mode.ERASE
                        host.beginEdit(activePage)
                        lastErase = wx to wy
                        eraseAt(wx, wy)
                        layer.eraserAt = e.x to e.y
                    }
                    Tool.LASSO -> {
                        mode = Mode.LASSO
                        lassoPoints.clear(); lassoPoints += e.x to e.y
                        layer.lasso = lassoPoints
                        layer.startAnts()
                    }
                    Tool.LASER -> {
                        mode = Mode.LASER
                        layer.laserBegin(toDoc(e.x, e.y))
                    }
                    Tool.TAPE -> { mode = Mode.TAPE; layer.tapePreview = null }
                    Tool.TEXT, Tool.STICKY -> mode = Mode.TAP
                    Tool.IMAGE, Tool.STICKER -> mode = Mode.IGNORE
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = e.findPointerIndex(activePointer).takeIf { it >= 0 } ?: return
                when (mode) {
                    Mode.INK -> {
                        val id = strokeId ?: return
                        for (h in 0 until e.historySize) points += toLocal(e.getHistoricalX(idx, h), e.getHistoricalY(idx, h), activePage)
                        val cur = toLocal(e.getX(idx), e.getY(idx), activePage)
                        points += cur
                        trackHold(cur)
                        if (holdFired) return
                        val predicted = predictor.predict()
                        try {
                            wetInk.addToStroke(e, activePointer, id, predicted)
                        } finally {
                            predicted?.recycle()
                        }
                    }
                    Mode.ERASE -> {
                        for (h in 0 until e.historySize) {
                            val (hx, hy) = toLocal(e.getHistoricalX(idx, h), e.getHistoricalY(idx, h), activePage)
                            eraseAt(hx, hy)
                        }
                        val (wx, wy) = toLocal(e.getX(idx), e.getY(idx), activePage)
                        eraseAt(wx, wy)
                        layer.eraserAt = e.getX(idx) to e.getY(idx)
                        layer.invalidate()
                    }
                    Mode.LASSO -> {
                        for (h in 0 until e.historySize) lassoPoints += e.getHistoricalX(idx, h) to e.getHistoricalY(idx, h)
                        lassoPoints += e.getX(idx) to e.getY(idx)
                        layer.invalidate()
                    }
                    Mode.MOVE -> {
                        val (wx, wy) = toLocal(e.getX(idx), e.getY(idx), activePage)
                        val s = moveStart ?: return
                        dragOffset = (wx - s.first) to (wy - s.second)
                        layer.invalidate()
                        reportSelection()
                    }
                    Mode.LASER -> {
                        for (h in 0 until e.historySize) layer.laserAdd(toDoc(e.getHistoricalX(idx, h), e.getHistoricalY(idx, h)))
                        layer.laserAdd(toDoc(e.getX(idx), e.getY(idx)))
                    }
                    Mode.SHAPE_DRAG -> {
                        val (sx, sy) = points.first()
                        val (cx, cy) = toLocal(e.getX(idx), e.getY(idx), activePage)
                        layer.shapePreview = ShapeBuilder.build(host.shapeKind(), sx, sy, cx, cy)
                        layer.invalidate()
                    }
                    Mode.TAPE -> {
                        val (sx, sy) = points.first()
                        val (cx, cy) = toLocal(e.getX(idx), e.getY(idx), activePage)
                        layer.tapePreview = tapeRect(sx, sy, cx, cy)
                        layer.invalidate()
                    }
                    else -> Unit
                }
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(holdRunnable)
                val isTap = SystemClock.uptimeMillis() - downTime < 350 && hypot(e.x - downX, e.y - downY) < touchSlop
                when (mode) {
                    Mode.INK -> finishInk(e, isTap)
                    Mode.ERASE -> { layer.eraserAt = null; lastErase = null; layer.invalidate() }
                    Mode.LASSO -> {
                        val local = lassoPoints.map { toLocal(it.first, it.second, activePage) }
                        layer.lasso = null
                        layer.stopAnts()
                        if (local.size > 3) select(activePage, Lasso(local))
                        layer.invalidate()
                    }
                    Mode.MOVE -> commitMove()
                    Mode.LASER -> laserUp()
                    Mode.SHAPE_DRAG -> {
                        val shapes = layer.shapePreview
                        layer.shapePreview = null
                        if (shapes != null && !isTap) addShapes(shapes)
                        layer.invalidate()
                    }
                    Mode.TAPE -> {
                        val r = layer.tapePreview
                        layer.tapePreview = null
                        val (wx, wy) = toLocal(e.x, e.y, activePage)
                        if (isTap || r == null) toggleTapeAt(activePage, wx, wy) else addTape(r)
                        layer.invalidate()
                    }
                    Mode.TAP -> {
                        val (wx, wy) = toLocal(e.x, e.y, activePage)
                        val items = pages[activePage].items
                        if (strokeTool == Tool.TEXT) {
                            val hit = items.lastOrNull { it is TextItem && it.bounds.contains(wx, wy) } as TextItem?
                            host.onTextTap(activePage, wx, wy, hit)
                        } else {
                            val hit = items.lastOrNull { it is StickyItem && it.bounds.contains(wx, wy) } as StickyItem?
                            host.onStickyTap(activePage, wx, wy, hit)
                        }
                    }
                    else -> Unit
                }
                mode = Mode.NONE
                strokeId = null
            }
            MotionEvent.ACTION_CANCEL -> cancelCurrent(e)
        }
    }

    private fun startInk(e: MotionEvent, t: Tool) {
        mode = Mode.INK
        val toWorld = Matrix(inverse).apply { postTranslate(-ox(activePage), -oy(activePage)) }
        val id = wetInk.startStroke(e, activePointer, host.brush(t), toWorld, Matrix())
        strokeId = id
        strokePages[id] = activePage
        // Wet ink is drawn in page space; tell the view how to map it back to the screen.
        if (settings.holdToShape || settings.circleToLasso) postDelayed(holdRunnable, HOLD_MS)
    }

    private fun finishInk(e: MotionEvent, isTap: Boolean) {
        val id = strokeId ?: return
        if (holdFired) return
        val p = pages[activePage]
        val (tx, ty) = points.first()
        if (isTap && p.items.any { it is TapeItem && it.bounds.contains(tx, ty) }) {
            wetInk.cancelStroke(id, e); strokePages.remove(id)
            toggleTapeAt(activePage, tx, ty)
            return
        }
        // Scribble-to-erase: a zig-zag over existing ink erases it instead of drawing.
        if (settings.scribbleToErase && strokeTool == Tool.PEN && isScribble(points)) {
            val hit = itemsUnder(activePage, points)
            if (hit.isNotEmpty()) {
                wetInk.cancelStroke(id, e); strokePages.remove(id)
                host.beginEdit(activePage)
                updatePage(activePage, p.items.filterNot { it.id in hit })
                layer.flashErase()
                return
            }
        }
        if (strokeTool == Tool.SHAPE) {
            val shape = ShapeRecognizer.recognize(points)
            if (shape != null) {
                wetInk.cancelStroke(id, e); strokePages.remove(id)
                addShapes(listOf(shape))
                return
            }
        }
        wetInk.finishStroke(e, activePointer, id)
    }

    private fun trackHold(cur: Pt) {
        val last = lastMovePt
        if (last == null || hypot(cur.first - last.first, cur.second - last.second) * scale > touchSlop) {
            lastMovePt = cur
            removeCallbacks(holdRunnable)
            if (settings.holdToShape || settings.circleToLasso) postDelayed(holdRunnable, HOLD_MS)
        }
    }

    /** Pen held still at the end of a stroke: circle-to-lasso or snap to a shape. */
    private fun onHold() {
        if (mode != Mode.INK || holdFired) return
        val id = strokeId ?: return
        val p = pages.getOrNull(activePage) ?: return
        if (points.size < 6 || pathLength(points) < 30f) return

        if (settings.circleToLasso && strokeTool == Tool.PEN && isClosedLoop(points)) {
            val lasso = Lasso(points)
            val ids = p.items.filter { lasso.selects(it) }.map { it.id }.toSet()
            if (ids.isNotEmpty()) {
                holdFired = true
                cancelWet(id)
                selectItems(activePage, ids)
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                return
            }
        }
        if (settings.holdToShape) {
            val shape = ShapeRecognizer.recognize(points) ?: return
            holdFired = true
            cancelWet(id)
            addShapes(listOf(shape))
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    private fun cancelWet(id: InProgressStrokeId) {
        val now = SystemClock.uptimeMillis()
        val ev = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        wetInk.cancelStroke(id, ev)
        strokePages.remove(id)
        ev.recycle()
    }

    private fun addShapes(shapes: List<List<Pt>>) {
        val p = pages.getOrNull(activePage) ?: return
        val brush = host.brush(Tool.SHAPE)
        val items = shapes.filter { it.size > 1 }.map { InkItem(ItemIds.next(), InkCodec.strokeFromPoints(it, brush)) }
        if (items.isEmpty()) return
        host.beginEdit(activePage)
        updatePage(activePage, p.items + items)
    }

    private fun tapeRect(x0: Float, y0: Float, x1: Float, y1: Float): RectF {
        val thick = 46f
        return if (abs(x1 - x0) >= abs(y1 - y0)) RectF(min(x0, x1), y0 - thick / 2, max(x0, x1), y0 + thick / 2)
        else RectF(x0 - thick / 2, min(y0, y1), x0 + thick / 2, max(y0, y1))
    }

    private fun addTape(r: RectF) {
        if (r.width() < 30f && r.height() < 30f) return
        val p = pages[activePage]
        host.beginEdit(activePage)
        updatePage(activePage, p.items + TapeItem(ItemIds.next(), r.left, r.top, r.width(), r.height(), host.tapeColor()))
    }

    private fun toggleTapeAt(page: Int, x: Float, y: Float) {
        val p = pages.getOrNull(page) ?: return
        val hit = p.items.lastOrNull { it is TapeItem && it.bounds.contains(x, y) } as TapeItem? ?: return
        updatePage(page, p.items.map { if (it.id == hit.id) hit.copy(revealed = !hit.revealed) else it })
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun cancelCurrent(e: MotionEvent) {
        removeCallbacks(holdRunnable)
        strokeId?.let { wetInk.cancelStroke(it, e); strokePages.remove(it) }
        strokeId = null
        if (mode == Mode.LASSO) { layer.lasso = null; layer.stopAnts() }
        if (mode == Mode.MOVE) dragOffset = null
        layer.shapePreview = null; layer.tapePreview = null
        layer.eraserAt = null
        mode = Mode.NONE
        layer.invalidate()
    }

    // ------------------------------------------------------------- laser

    /** A finished laser loop around content turns into a lasso selection. */
    private fun laserUp() {
        val stroke = layer.laserEnd() ?: return
        if (stroke.size < 8) return
        val page = pageAt(stroke.first().first, stroke.first().second)
        val local = stroke.map { (it.first - ox(page)) to (it.second - oy(page)) }
        if (!isClosedLoop(local)) return
        val lasso = Lasso(local)
        val ids = pages[page].items.filter { lasso.selects(it) }.map { it.id }.toSet()
        if (ids.isEmpty()) return
        layer.laserDropLast()
        selectItems(page, ids)
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    // ----------------------------------------------------------- eraser

    private fun eraseAt(wx: Float, wy: Float) {
        val p = pages.getOrNull(activePage) ?: return
        val prev = lastErase ?: (wx to wy)
        lastErase = wx to wy
        val r = host.eraserRadius() / scale
        val hit = HashSet<Long>()
        val para = MutableParallelogram()
        for (item in p.items) {
            when (item) {
                is InkItem -> {
                    val b = item.bounds
                    if (!b.intersects(min(prev.first, wx) - r, min(prev.second, wy) - r, max(prev.first, wx) + r, max(prev.second, wy) + r)) continue
                    val seg = MutableSegment(
                        MutableVec(prev.first - item.dx, prev.second - item.dy),
                        MutableVec(wx - item.dx, wy - item.dy),
                    )
                    para.populateFromSegmentAndPadding(seg, r)
                    if (item.stroke.shape.intersects(para, AffineTransform.IDENTITY)) hit += item.id
                }
                else -> if (item.bounds.contains(wx, wy)) hit += item.id
            }
        }
        if (hit.isNotEmpty()) updatePage(activePage, p.items.filterNot { it.id in hit })
    }

    private fun itemsUnder(page: Int, path: List<Pt>): Set<Long> {
        val p = pages.getOrNull(page) ?: return emptySet()
        val hit = HashSet<Long>()
        val para = MutableParallelogram()
        for (i in 1 until path.size) {
            val a = path[i - 1]; val b = path[i]
            for (item in p.items) {
                if (item.id in hit) continue
                if (item is InkItem) {
                    val seg = MutableSegment(MutableVec(a.first - item.dx, a.second - item.dy), MutableVec(b.first - item.dx, b.second - item.dy))
                    para.populateFromSegmentAndPadding(seg, 6f)
                    if (item.stroke.shape.intersects(para, AffineTransform.IDENTITY)) hit += item.id
                } else if (item.bounds.contains(b.first, b.second)) hit += item.id
            }
        }
        return hit
    }

    // ----------------------------------------------------------- selection

    private fun select(page: Int, lasso: Lasso) {
        val p = pages.getOrNull(page) ?: return
        selectItems(page, p.items.filter { lasso.selects(it) }.map { it.id }.toSet())
    }

    fun selectItems(page: Int, ids: Set<Long>) {
        selection = ids
        selectionPage = page
        layer.invalidate()
        if (ids.isNotEmpty()) layer.popSelection()
        reportSelection()
    }

    fun clearSelection() {
        if (selection.isEmpty()) return
        selection = emptySet()
        dragOffset = null
        layer.invalidate()
        reportSelection()
    }

    private fun selectionPageBounds(): RectF? {
        val p = pages.getOrNull(selectionPage) ?: return null
        val items = p.items.filter { it.id in selection }
        if (items.isEmpty()) return null
        val r = RectF(items.first().bounds)
        items.drop(1).forEach { r.union(it.bounds) }
        dragOffset?.let { r.offset(it.first, it.second) }
        r.inset(-12f, -12f)
        return r
    }

    private val tmpMatrix = Matrix()

    private fun reportSelection() {
        val wb = if (selection.isEmpty()) null else selectionPageBounds()
        if (wb == null) { host.onSelectionChanged(selectionPage, emptySet(), null); return }
        val sb = RectF(wb)
        pageMatrix(selectionPage, tmpMatrix).mapRect(sb)
        host.onSelectionChanged(selectionPage, selection, sb)
    }

    private fun commitMove() {
        val p = pages.getOrNull(selectionPage) ?: return
        val off = dragOffset
        dragOffset = null
        if (off != null && (off.first != 0f || off.second != 0f)) {
            host.beginEdit(selectionPage)
            updatePage(selectionPage, p.items.map { if (it.id in selection) it.translated(off.first, off.second) else it })
        }
        reportSelection()
    }

    // ----------------------------------------------------------- fingers

    private fun handleFingers(e: MotionEvent, action: Int) {
        if (velocity == null) velocity = VelocityTracker.obtain()
        velocity?.addMovement(e)
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                scroller.forceFinished(true)
                mode = Mode.PAN
                panLastX = e.x; panLastY = e.y
                fingerStartX = e.x; fingerStartY = e.y
                maxFingers = 1
                fingerDownTime = SystemClock.uptimeMillis()
                fingerMoved = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                mode = Mode.PAN
                maxFingers = max(maxFingers, e.pointerCount)
                if (fingerDownTime == 0L) fingerDownTime = SystemClock.uptimeMillis()
                if (e.pointerCount >= 2) {
                    pinchStartDist = spacing(e)
                    pinchStartScale = scale
                }
                val (cx, cy) = focus(e)
                panLastX = cx; panLastY = cy
            }
            MotionEvent.ACTION_MOVE -> {
                val (cx, cy) = focus(e)
                if (hypot(cx - fingerStartX, cy - fingerStartY) > touchSlop * 2) fingerMoved = true
                if (e.pointerCount >= 2 && pinchStartDist > 0f) {
                    val newScale = (pinchStartScale * spacing(e) / pinchStartDist).coerceIn(fitScale() * 0.5f, 6f)
                    if (abs(newScale - scale) > 0.001f) {
                        fingerMoved = true
                        val (fx, fy) = toDoc(cx, cy)
                        scale = newScale
                        transX = cx - fx * scale
                        transY = cy - fy * scale
                    }
                }
                var dx = cx - panLastX; var dy = cy - panLastY
                if (pull > 0f) { dx *= 0.45f; dy *= 0.45f }   // rubber band while pulling
                transX += dx
                transY += dy
                panLastX = cx; panLastY = cy
                clamp(allowPull = e.pointerCount == 1)
                updateMatrix()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val remaining = (0 until e.pointerCount).filter { it != e.actionIndex }
                if (remaining.isNotEmpty()) {
                    panLastX = remaining.map { e.getX(it) }.average().toFloat()
                    panLastY = remaining.map { e.getY(it) }.average().toFloat()
                }
                pinchStartDist = 0f
                if (e.pointerCount - 1 >= 2) {
                    pinchStartDist = spacing(e, skip = e.actionIndex)
                    pinchStartScale = scale
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val quick = SystemClock.uptimeMillis() - fingerDownTime < 300
                if (action == MotionEvent.ACTION_UP && quick && !fingerMoved && settings.fingerTapUndo) {
                    when (maxFingers) {
                        2 -> host.undo()
                        3 -> host.redo()
                    }
                }
                if (maxFingers == 1 && quick && !fingerMoved && selection.isNotEmpty()) clearSelection()
                val pulled = pull >= pullTrigger
                if (pulled) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    host.onPullAddPage()
                }
                pull = 0f
                releaseFling(pulled)
                mode = Mode.NONE
                maxFingers = 0
                fingerDownTime = 0L
                pinchStartDist = 0f
                velocity?.recycle(); velocity = null
            }
        }
    }

    private fun releaseFling(pulled: Boolean) {
        val vt = velocity ?: return
        vt.computeCurrentVelocity(1000)
        val vx = vt.xVelocity; val vy = vt.yVelocity
        val horizontal = settings.scroll == ScrollDirection.HORIZONTAL
        val atFit = scale <= fitScale() * 1.05f
        if (horizontal && atFit && !pulled) {
            // Page-by-page snapping, like turning pages.
            var target = currentIndex
            if (vx < -600) target = (currentIndex + 1).coerceAtMost(pages.size - 1)
            else if (vx > 600) target = (currentIndex - 1).coerceAtLeast(0)
            scrollToPage(target, animate = true)
            return
        }
        val cw = docW * scale; val ch = docH * scale
        scroller.fling(
            transX.toInt(), transY.toInt(), vx.toInt(), vy.toInt(),
            min(0f, width - cw - 24f * density).toInt(), (24f * density).toInt(),
            min(0f, height - ch - 24f * density).toInt(), (24f * density).toInt(),
        )
        clamp(); updateMatrix()
        postInvalidateOnAnimation()
    }

    private fun focus(e: MotionEvent): Pair<Float, Float> {
        var sx = 0f; var sy = 0f
        for (i in 0 until e.pointerCount) { sx += e.getX(i); sy += e.getY(i) }
        return sx / e.pointerCount to sy / e.pointerCount
    }

    private fun spacing(e: MotionEvent, skip: Int = -1): Float {
        val idx = (0 until e.pointerCount).filter { it != skip }
        if (idx.size < 2) return 0f
        return hypot(e.getX(idx[0]) - e.getX(idx[1]), e.getY(idx[0]) - e.getY(idx[1]))
    }

    // Stylus hover: show the eraser ring when the side button is held over the page.
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val tt = event.getToolType(0)
        if (tt == MotionEvent.TOOL_TYPE_STYLUS || tt == MotionEvent.TOOL_TYPE_ERASER) {
            when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_HOVER_ENTER -> {
                    layer.hoverAt = if (effectiveTool(event) == Tool.ERASER) event.x to event.y else null
                    layer.invalidate()
                }
                MotionEvent.ACTION_HOVER_EXIT -> { layer.hoverAt = null; layer.invalidate() }
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }

    fun refresh() = layer.invalidate()

    companion object {
        private const val HOLD_MS = 550L
    }

    // ================================================================ layer

    private class LaserStroke(val pts: ArrayList<Pt>, var end: Long = Long.MAX_VALUE)

    @SuppressLint("ViewConstructor")
    private inner class DocLayer(context: Context) : View(context) {
        var lasso: List<Pt>? = null
        var eraserAt: Pair<Float, Float>? = null
        var hoverAt: Pair<Float, Float>? = null
        var shapePreview: List<List<Pt>>? = null
        var tapePreview: RectF? = null

        private val lassoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.6f * density; color = 0xFF3D6CF4.toInt()
        }
        private val lassoFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x143D6CF4 }
        private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.5f * density; color = 0xFF3D6CF4.toInt()
            pathEffect = DashPathEffect(floatArrayOf(8f * density, 5f * density), 0f)
        }
        private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3D6CF4.toInt() }
        private val handleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        private val eraserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.5f * density; color = 0x99000000.toInt()
        }
        private val eraserFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF }
        private val previewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2f * density; color = 0xFF3D6CF4.toInt()
            strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
        }
        private val tapePreviewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x993D6CF4.toInt() }
        private val laserGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        private val laserCore = Paint(laserGlow)
        private val pullPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3D6CF4.toInt() }
        private val pullText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f * density; color = 0xFF5B6475.toInt(); textAlign = Paint.Align.CENTER
        }
        private val pagePaintBg = Paint().apply { color = 0xFFE9ECF1.toInt() }
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
                duration = 600; repeatCount = ValueAnimator.INFINITE
                addUpdateListener { antsPhase = it.animatedValue as Float; invalidate() }
                start()
            }
        }

        fun stopAnts() { antsAnim?.cancel(); antsAnim = null }

        fun popSelection() {
            ValueAnimator.ofFloat(0.92f, 1f).apply {
                duration = 220; interpolator = OvershootInterpolator(2f)
                addUpdateListener { selScale = it.animatedValue as Float; invalidate() }
                start()
            }
        }

        fun flashErase() {
            ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 350
                addUpdateListener { flashAlpha = it.animatedValue as Float; invalidate() }
                start()
            }
        }

        fun laserBegin(p: Pt) { lasers += LaserStroke(arrayListOf(p)); invalidate() }
        fun laserAdd(p: Pt) { lasers.lastOrNull()?.pts?.add(p); invalidate() }
        fun laserEnd(): List<Pt>? {
            val s = lasers.lastOrNull() ?: return null
            s.end = SystemClock.uptimeMillis()
            invalidate()
            return s.pts
        }
        fun laserDropLast() { if (lasers.isNotEmpty()) lasers.removeAt(lasers.size - 1); invalidate() }

        override fun onDraw(canvas: Canvas) {
            val r = renderer ?: return
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), pagePaintBg)
            r.paperColor = settings.paperTint.argb
            val view = RectF(0f, 0f, width.toFloat(), height.toFloat())
            val active = host.activeHighlight()
            pages.forEachIndexed { i, p ->
                val sr = RectF(0f, 0f, p.width, p.height)
                pageMatrix(i, m).mapRect(sr)
                if (!RectF.intersects(sr, view)) return@forEachIndexed
                canvas.save()
                canvas.concat(m)
                val pdf = if (p.pdfPage >= 0) host.pdfBitmap(p) { postInvalidate() } else null
                r.drawPaper(canvas, p, pdf, shadow = true)
                val hl = host.highlights(i)
                if (hl.isNotEmpty()) r.drawHighlights(canvas, hl, active?.takeIf { it.first == i }?.second)
                if (i == selectionPage) r.drawItems(canvas, p.items, m, offset = dragOffset, offsetIds = selection)
                else r.drawItems(canvas, p.items, m)
                tapePreview?.takeIf { i == activePage }?.let { canvas.drawRoundRect(it, 6f, 6f, tapePreviewPaint) }
                shapePreview?.takeIf { i == activePage }?.forEach { line ->
                    tmpPath.reset()
                    line.forEachIndexed { k, (x, y) -> if (k == 0) tmpPath.moveTo(x, y) else tmpPath.lineTo(x, y) }
                    previewPaint.strokeWidth = 2f * density / scale
                    canvas.drawPath(tmpPath, previewPaint)
                }
                canvas.restore()
            }

            drawPull(canvas)
            drawSelection(canvas)
            lasso?.let { drawLasso(canvas, it) }
            (eraserAt ?: hoverAt)?.let { (x, y) ->
                val rad = host.eraserRadius()
                canvas.drawCircle(x, y, rad, eraserFill)
                canvas.drawCircle(x, y, rad, eraserPaint)
            }
            drawLaser(canvas)
            if (flashAlpha > 0f) canvas.drawColor(Color.argb((flashAlpha * 40).toInt(), 61, 108, 244))
        }

        private fun drawPull(canvas: Canvas) {
            if (pull <= 0f || pages.isEmpty()) return
            val progress = (pull / pullTrigger).coerceIn(0f, 1f)
            val vertical = settings.scroll == ScrollDirection.VERTICAL
            val last = pages.size - 1
            val end = RectF(0f, 0f, pages[last].width, pages[last].height)
            pageMatrix(last, m).mapRect(end)
            val cx = if (vertical) width / 2f else end.right + pull / 2f
            val cy = if (vertical) end.bottom + pull / 2f else height / 2f
            val rad = 22f * density * (0.6f + 0.4f * progress)
            pullPaint.alpha = (90 + 165 * progress).toInt()
            canvas.drawCircle(cx, cy, rad, pullPaint)
            val arm = rad * 0.45f
            handleFill.strokeWidth = 3f * density
            canvas.save(); canvas.rotate(progress * 180f, cx, cy)
            canvas.drawLine(cx - arm, cy, cx + arm, cy, handleFill.apply { style = Paint.Style.STROKE })
            canvas.drawLine(cx, cy - arm, cx, cy + arm, handleFill)
            canvas.restore()
            handleFill.style = Paint.Style.FILL
            canvas.drawText(if (progress >= 1f) "Release to add a page" else "Pull to add a page", cx, cy + rad + 22f * density, pullText)
        }

        private fun drawSelection(canvas: Canvas) {
            val wb = selectionPageBounds() ?: return
            val sb = RectF(wb)
            pageMatrix(selectionPage, m).mapRect(sb)
            canvas.save()
            canvas.scale(selScale, selScale, sb.centerX(), sb.centerY())
            val rad = 10f * density
            canvas.drawRoundRect(sb, rad, rad, selPaint)
            val hr = 5f * density
            for ((x, y) in listOf(sb.left to sb.top, sb.right to sb.top, sb.left to sb.bottom, sb.right to sb.bottom)) {
                canvas.drawCircle(x, y, hr, handleFill)
                canvas.drawCircle(x, y, hr - 1.5f * density, handlePaint)
            }
            canvas.restore()
        }

        private fun drawLasso(canvas: Canvas, pts: List<Pt>) {
            if (pts.size < 2) return
            tmpPath.reset()
            pts.forEachIndexed { i, (x, y) -> if (i == 0) tmpPath.moveTo(x, y) else tmpPath.lineTo(x, y) }
            canvas.drawPath(tmpPath, lassoFill)
            lassoPaint.pathEffect = DashPathEffect(floatArrayOf(9f * density, 4f * density), antsPhase)
            canvas.drawPath(tmpPath, lassoPaint)
        }

        /** Laser ink stays like a pen for a few seconds, then fades out. */
        private fun drawLaser(canvas: Canvas) {
            if (lasers.isEmpty()) return
            val now = SystemClock.uptimeMillis()
            val hold = (settings.laserSeconds * 1000).toLong()
            val fade = 500L
            lasers.removeAll { it.end != Long.MAX_VALUE && now - it.end > hold + fade }
            val pt = FloatArray(2)
            for (s in lasers) {
                val age = if (s.end == Long.MAX_VALUE) 0L else now - s.end
                val life = if (age <= hold) 1f else 1f - (age - hold) / fade.toFloat()
                if (s.pts.size < 2) continue
                tmpPath.reset()
                s.pts.forEachIndexed { i, (x, y) ->
                    pt[0] = x; pt[1] = y; viewMatrix.mapPoints(pt)
                    if (i == 0) tmpPath.moveTo(pt[0], pt[1]) else tmpPath.lineTo(pt[0], pt[1])
                }
                laserGlow.color = Color.argb((life * 80).toInt(), 255, 59, 48)
                laserGlow.strokeWidth = 12f * density
                canvas.drawPath(tmpPath, laserGlow)
                laserCore.color = Color.argb((life * 255).toInt(), 255, 80, 70)
                laserCore.strokeWidth = 3.5f * density
                canvas.drawPath(tmpPath, laserCore)
            }
            if (lasers.isNotEmpty()) postInvalidateOnAnimation()
        }
    }
}
