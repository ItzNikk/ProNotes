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
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.MutableParallelogram
import androidx.ink.geometry.MutableSegment
import androidx.ink.geometry.MutableVec
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class Tool { PEN, ERASER, HIGHLIGHTER, SHAPE, LASSO, IMAGE, TEXT, LASER }

enum class SideButtonAction { ERASER, LASSO, NONE }

data class InputSettings(
    val stylusOnly: Boolean = true,
    val sideButton: SideButtonAction = SideButtonAction.ERASER,
    val scribbleToErase: Boolean = true,
    val holdToShape: Boolean = true,
    val circleToLasso: Boolean = true,
)

/** Callbacks from the canvas to the editor state. */
interface CanvasHost {
    fun currentBrush(tool: Tool): Brush
    fun eraserRadius(): Float
    fun beginEdit()
    fun setItems(items: List<PageItem>)
    fun onSelectionChanged(ids: Set<Long>, screenBounds: RectF?)
    fun onTextTap(worldX: Float, worldY: Float, existing: TextItem?)
    fun undo()
    fun redo()
    fun pdfBitmap(page: Page, onReady: () -> Unit): Bitmap?
}

/**
 * The drawing surface. Jetpack Ink's [InProgressStrokesView] renders wet ink with
 * low latency; everything else (dry strokes, paper, PDF, selection) is drawn by [PageLayer].
 *
 * Input routing:
 *  - stylus tip -> active tool
 *  - stylus eraser end (Wacom, Surface, USI) -> eraser
 *  - stylus side button held (S Pen, Wacom, USI, Lenovo, Xiaomi...) -> side-button action
 *  - fingers -> pan / pinch-zoom; 2-finger tap = undo, 3-finger tap = redo
 */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class NoteCanvasView(context: Context, private val host: CanvasHost) : FrameLayout(context) {

    private val layer = PageLayer(context)
    private val wetInk = InProgressStrokesView(context)
    private val predictor = MotionEventPredictor.newInstance(this)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    var renderer: PageRenderer? = null
        set(value) { field = value; layer.invalidate() }

    var page: Page? = null
        private set
    var tool: Tool = Tool.PEN
    var settings = InputSettings()

    // View transform: world -> screen
    private val viewMatrix = Matrix()
    private val inverse = Matrix()
    private var scale = 1f
    private var transX = 0f
    private var transY = 0f
    private var fitted = false

    // Selection
    var selection: Set<Long> = emptySet()
        private set
    private var dragOffset: Pair<Float, Float>? = null

    init {
        addView(layer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(wetInk, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        setBackgroundColor(0xFFE9ECF1.toInt())
        wetInk.addFinishedStrokesListener(object : InProgressStrokesFinishedListener {
            override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                val p = page
                if (p != null) {
                    host.beginEdit()
                    val added = strokes.values.map { InkItem(ItemIds.next(), it) }
                    setPageInternal(p.copy(items = p.items + added))
                    host.setItems(page!!.items)
                }
                wetInk.removeFinishedStrokes(strokes.keys)
            }
        })
    }

    fun showPage(newPage: Page, animate: Boolean) {
        val changedPage = page?.id != newPage.id
        if (changedPage) clearSelection()
        page = newPage
        if (changedPage && width > 0) fitWidth()
        if (changedPage && animate) {
            layer.alpha = 0f
            layer.translationY = 24f
            layer.animate().alpha(1f).translationY(0f).setDuration(220)
                .setInterpolator(DecelerateInterpolator()).start()
        }
        layer.invalidate()
    }

    private fun setPageInternal(p: Page) {
        page = p
        layer.invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!fitted || oldw != w) fitWidth()
    }

    private fun fitWidth() {
        val p = page ?: return
        if (width == 0) return
        val margin = 32f * resources.displayMetrics.density
        scale = min((width - margin) / p.width, 3f)
        transX = (width - p.width * scale) / 2f
        transY = margin / 2f
        fitted = true
        updateMatrix()
    }

    private fun updateMatrix() {
        val p = page
        if (p != null) {
            // Keep the page reachable.
            val pw = p.width * scale; val ph = p.height * scale
            transX = if (pw < width) (width - pw) / 2f else transX.coerceIn(width - pw - 40f, 40f)
            transY = transY.coerceIn(min(height - ph - 40f, 40f), 40f + max(0f, height - ph) / 2f + 200f)
        }
        viewMatrix.reset()
        viewMatrix.postScale(scale, scale)
        viewMatrix.postTranslate(transX, transY)
        viewMatrix.invert(inverse)
        layer.invalidate()
        reportSelection()
    }

    private fun toWorld(x: Float, y: Float): Pair<Float, Float> {
        val a = floatArrayOf(x, y)
        inverse.mapPoints(a)
        return a[0] to a[1]
    }

    // ------------------------------------------------------------------ input

    private enum class Mode { NONE, INK, ERASE, LASSO, MOVE, LASER, PAN, TEXT_TAP, IGNORE }

    private var mode = Mode.NONE
    private var activePointer = -1
    private var strokeId: InProgressStrokeId? = null
    private var strokeTool = Tool.PEN
    private val points = ArrayList<Pt>()   // world points of the current gesture
    private val lassoPoints = ArrayList<Pt>()
    private var lastErase: Pt? = null
    private var erasedAny = false
    private var moveStart: Pt? = null
    private var holdFired = false
    private var lastMoveTime = 0L
    private var lastMovePt: Pt? = null
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
            // A second finger lands while finger-drawing: switch to pan/zoom.
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
                SideButtonAction.ERASER -> return Tool.ERASER
                SideButtonAction.LASSO -> return Tool.LASSO
                SideButtonAction.NONE -> Unit
            }
        }
        return tool
    }

    private fun handleDraw(e: MotionEvent, action: Int) {
        val p = page ?: return
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                requestUnbufferedDispatch(e)
                activePointer = e.getPointerId(0)
                points.clear()
                holdFired = false
                val (wx, wy) = toWorld(e.x, e.y)
                points += wx to wy
                lastMovePt = wx to wy
                lastMoveTime = SystemClock.uptimeMillis()
                val t = effectiveTool(e)
                strokeTool = t

                // Touching inside the current selection drags it, whatever the tool.
                if (selection.isNotEmpty() && selectionWorldBounds()?.contains(wx, wy) == true && t != Tool.ERASER) {
                    mode = Mode.MOVE
                    moveStart = wx to wy
                    dragOffset = 0f to 0f
                    return
                }
                if (selection.isNotEmpty()) clearSelection()

                when (t) {
                    Tool.PEN, Tool.HIGHLIGHTER, Tool.SHAPE -> {
                        mode = Mode.INK
                        strokeId = wetInk.startStroke(e, activePointer, host.currentBrush(t), inverse, Matrix())
                        if (settings.holdToShape || settings.circleToLasso) postDelayed(holdRunnable, HOLD_MS)
                    }
                    Tool.ERASER -> {
                        mode = Mode.ERASE
                        host.beginEdit()
                        erasedAny = false
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
                        layer.addLaser(e.x, e.y)
                    }
                    Tool.TEXT -> mode = Mode.TEXT_TAP
                    Tool.IMAGE -> mode = Mode.IGNORE
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = e.findPointerIndex(activePointer).takeIf { it >= 0 } ?: return
                when (mode) {
                    Mode.INK -> {
                        val id = strokeId ?: return
                        for (h in 0 until e.historySize) points += toWorld(e.getHistoricalX(idx, h), e.getHistoricalY(idx, h))
                        val cur = toWorld(e.getX(idx), e.getY(idx))
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
                            val (hx, hy) = toWorld(e.getHistoricalX(idx, h), e.getHistoricalY(idx, h))
                            eraseAt(hx, hy)
                        }
                        val (wx, wy) = toWorld(e.getX(idx), e.getY(idx))
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
                        val (wx, wy) = toWorld(e.getX(idx), e.getY(idx))
                        val s = moveStart ?: return
                        dragOffset = (wx - s.first) to (wy - s.second)
                        layer.invalidate()
                        reportSelection()
                    }
                    Mode.LASER -> layer.addLaser(e.getX(idx), e.getY(idx))
                    else -> Unit
                }
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(holdRunnable)
                when (mode) {
                    Mode.INK -> finishInk(e, p)
                    Mode.ERASE -> {
                        layer.eraserAt = null
                        lastErase = null
                        layer.invalidate()
                    }
                    Mode.LASSO -> {
                        val world = lassoPoints.map { toWorld(it.first, it.second) }
                        layer.lasso = null
                        layer.stopAnts()
                        if (world.size > 3) select(Lasso(world))
                        layer.invalidate()
                    }
                    Mode.MOVE -> commitMove()
                    Mode.TEXT_TAP -> {
                        val (wx, wy) = toWorld(e.x, e.y)
                        val hit = page?.items?.lastOrNull { it is TextItem && it.bounds.contains(wx, wy) } as TextItem?
                        host.onTextTap(wx, wy, hit)
                    }
                    else -> Unit
                }
                mode = Mode.NONE
                strokeId = null
            }
            MotionEvent.ACTION_CANCEL -> cancelCurrent(e)
        }
    }

    private fun finishInk(e: MotionEvent, p: Page) {
        val id = strokeId ?: return
        if (holdFired) return
        // Scribble-to-erase: a zig-zag over existing ink erases it instead of drawing.
        if (settings.scribbleToErase && strokeTool == Tool.PEN && isScribble(points)) {
            val hit = itemsUnder(points)
            if (hit.isNotEmpty()) {
                wetInk.cancelStroke(id, e)
                host.beginEdit()
                setPageInternal(p.copy(items = p.items.filterNot { it.id in hit }))
                host.setItems(page!!.items)
                layer.flashErase()
                return
            }
        }
        if (strokeTool == Tool.SHAPE) {
            val shape = ShapeRecognizer.recognize(points)
            if (shape != null) {
                wetInk.cancelStroke(id, e)
                addShape(shape)
                return
            }
        }
        wetInk.finishStroke(e, activePointer, id)
    }

    private fun trackHold(cur: Pt) {
        val last = lastMovePt
        if (last == null || hypot(cur.first - last.first, cur.second - last.second) * scale > touchSlop) {
            lastMovePt = cur
            lastMoveTime = SystemClock.uptimeMillis()
            removeCallbacks(holdRunnable)
            if (settings.holdToShape || settings.circleToLasso) postDelayed(holdRunnable, HOLD_MS)
        }
    }

    /** Pen held still at the end of a stroke: circle-to-lasso or snap to a shape. */
    private fun onHold() {
        if (mode != Mode.INK || holdFired) return
        val id = strokeId ?: return
        val p = page ?: return
        if (points.size < 6 || pathLength(points) < 30f) return
        if (strokeTool == Tool.HIGHLIGHTER && !settings.holdToShape) return

        if (settings.circleToLasso && strokeTool == Tool.PEN && isClosedLoop(points)) {
            val lasso = Lasso(points)
            val ids = p.items.filter { lasso.selects(it) }.map { it.id }.toSet()
            if (ids.isNotEmpty()) {
                holdFired = true
                cancelWet(id)
                selectItems(ids)
                performHapticFeedback(HAPTIC)
                return
            }
        }
        if (settings.holdToShape) {
            val shape = ShapeRecognizer.recognize(points) ?: return
            holdFired = true
            cancelWet(id)
            addShape(shape)
            performHapticFeedback(HAPTIC)
        }
    }

    private fun cancelWet(id: InProgressStrokeId) {
        val now = SystemClock.uptimeMillis()
        val ev = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        wetInk.cancelStroke(id, ev)
        ev.recycle()
    }

    private fun addShape(shape: List<Pt>) {
        val p = page ?: return
        val stroke = InkCodec.strokeFromPoints(shape, host.currentBrush(strokeTool))
        host.beginEdit()
        setPageInternal(p.copy(items = p.items + InkItem(ItemIds.next(), stroke)))
        host.setItems(page!!.items)
    }

    private fun cancelCurrent(e: MotionEvent) {
        removeCallbacks(holdRunnable)
        strokeId?.let { wetInk.cancelStroke(it, e) }
        strokeId = null
        if (mode == Mode.LASSO) { layer.lasso = null; layer.stopAnts() }
        if (mode == Mode.MOVE) { dragOffset = null }
        layer.eraserAt = null
        mode = Mode.NONE
        layer.invalidate()
    }

    // ----------------------------------------------------------- eraser

    private fun eraseAt(wx: Float, wy: Float) {
        val p = page ?: return
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
        if (hit.isNotEmpty()) {
            erasedAny = true
            setPageInternal(p.copy(items = p.items.filterNot { it.id in hit }))
            host.setItems(page!!.items)
        }
    }

    private fun itemsUnder(path: List<Pt>): Set<Long> {
        val p = page ?: return emptySet()
        val r = 6f
        val hit = HashSet<Long>()
        val para = MutableParallelogram()
        for (i in 1 until path.size) {
            val a = path[i - 1]; val b = path[i]
            for (item in p.items) {
                if (item.id in hit) continue
                if (item is InkItem) {
                    val seg = MutableSegment(MutableVec(a.first - item.dx, a.second - item.dy), MutableVec(b.first - item.dx, b.second - item.dy))
                    para.populateFromSegmentAndPadding(seg, r)
                    if (item.stroke.shape.intersects(para, AffineTransform.IDENTITY)) hit += item.id
                } else if (item.bounds.contains(b.first, b.second)) hit += item.id
            }
        }
        return hit
    }

    // ----------------------------------------------------------- selection

    private fun select(lasso: Lasso) {
        val p = page ?: return
        selectItems(p.items.filter { lasso.selects(it) }.map { it.id }.toSet())
    }

    fun selectItems(ids: Set<Long>) {
        selection = ids
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

    private fun selectionWorldBounds(): RectF? {
        val p = page ?: return null
        val items = p.items.filter { it.id in selection }
        if (items.isEmpty()) return null
        val r = RectF(items.first().bounds)
        items.drop(1).forEach { r.union(it.bounds) }
        dragOffset?.let { r.offset(it.first, it.second) }
        r.inset(-12f, -12f)
        return r
    }

    private fun reportSelection() {
        if (selection.isEmpty()) { host.onSelectionChanged(emptySet(), null); return }
        val wb = selectionWorldBounds() ?: run { host.onSelectionChanged(emptySet(), null); return }
        val sb = RectF(wb)
        viewMatrix.mapRect(sb)
        host.onSelectionChanged(selection, sb)
    }

    private fun commitMove() {
        val p = page ?: return
        val off = dragOffset
        dragOffset = null
        if (off != null && (off.first != 0f || off.second != 0f)) {
            host.beginEdit()
            setPageInternal(p.copy(items = p.items.map { if (it.id in selection) it.translated(off.first, off.second) else it }))
            host.setItems(page!!.items)
        }
        reportSelection()
    }

    // ----------------------------------------------------------- fingers

    private fun handleFingers(e: MotionEvent, action: Int) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
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
                    val newScale = (pinchStartScale * spacing(e) / pinchStartDist).coerceIn(0.3f, 6f)
                    if (kotlin.math.abs(newScale - scale) > 0.001f) {
                        fingerMoved = true
                        // zoom around the focus point
                        val (fx, fy) = toWorld(cx, cy)
                        scale = newScale
                        transX = cx - fx * scale
                        transY = cy - fy * scale
                    }
                }
                transX += cx - panLastX
                transY += cy - panLastY
                panLastX = cx; panLastY = cy
                updateMatrix()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                // Re-anchor on the remaining pointers.
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
                if (action == MotionEvent.ACTION_UP && quick && !fingerMoved) {
                    when (maxFingers) {
                        2 -> host.undo()
                        3 -> host.redo()
                        1 -> if (selection.isNotEmpty()) clearSelection()
                    }
                }
                mode = Mode.NONE
                maxFingers = 0
                fingerDownTime = 0L
                pinchStartDist = 0f
            }
        }
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
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS || event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER) {
            when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_HOVER_ENTER -> {
                    val erasing = effectiveTool(event) == Tool.ERASER
                    layer.hoverAt = if (erasing) event.x to event.y else null
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
        private const val HAPTIC = android.view.HapticFeedbackConstants.LONG_PRESS
    }

    // ================================================================ layer

    @SuppressLint("ViewConstructor")
    private inner class PageLayer(context: Context) : View(context) {
        var lasso: List<Pt>? = null
        var eraserAt: Pair<Float, Float>? = null
        var hoverAt: Pair<Float, Float>? = null
        private val density = resources.displayMetrics.density

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
        private val laserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        private val laserTrail = ArrayList<Triple<Float, Float, Long>>()
        private var antsPhase = 0f
        private var antsAnim: ValueAnimator? = null
        private var selScale = 1f
        private var flashAlpha = 0f
        private val tmpPath = Path()

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
                duration = 220; interpolator = android.view.animation.OvershootInterpolator(2f)
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

        fun addLaser(x: Float, y: Float) {
            laserTrail += Triple(x, y, SystemClock.uptimeMillis())
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val p = page ?: return
            val r = renderer ?: return
            canvas.save()
            canvas.concat(viewMatrix)
            val pdf = if (p.pdfPage >= 0) host.pdfBitmap(p) { postInvalidate() } else null
            r.drawPaper(canvas, p, pdf, shadow = true)
            r.drawItems(canvas, p.items, viewMatrix, offset = dragOffset, offsetIds = selection)
            canvas.restore()

            drawSelection(canvas)
            lasso?.let { drawLasso(canvas, it) }
            (eraserAt ?: hoverAt)?.let { (x, y) ->
                val rad = host.eraserRadius()
                canvas.drawCircle(x, y, rad, eraserFill)
                canvas.drawCircle(x, y, rad, eraserPaint)
            }
            drawLaser(canvas)
            if (flashAlpha > 0f) {
                canvas.drawColor(Color.argb((flashAlpha * 40).toInt(), 61, 108, 244))
            }
        }

        private fun drawSelection(canvas: Canvas) {
            val wb = selectionWorldBounds() ?: return
            val sb = RectF(wb)
            viewMatrix.mapRect(sb)
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

        private fun drawLaser(canvas: Canvas) {
            if (laserTrail.isEmpty()) return
            val now = SystemClock.uptimeMillis()
            laserTrail.removeAll { now - it.third > 700 }
            for (i in 1 until laserTrail.size) {
                val a = laserTrail[i - 1]; val b = laserTrail[i]
                val life = 1f - (now - b.third) / 700f
                laserPaint.color = Color.argb((life * 90).toInt(), 255, 59, 48)
                laserPaint.strokeWidth = 14f * density * life
                canvas.drawLine(a.first, a.second, b.first, b.second, laserPaint)
                laserPaint.color = Color.argb((life * 255).toInt(), 255, 90, 80)
                laserPaint.strokeWidth = 4f * density * life
                canvas.drawLine(a.first, a.second, b.first, b.second, laserPaint)
            }
            if (laserTrail.isNotEmpty()) postInvalidateOnAnimation()
        }
    }
}
