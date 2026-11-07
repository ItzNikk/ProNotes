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
import kotlin.math.roundToInt

enum class Tool { PEN, HIGHLIGHTER, ERASER, SHAPE, LASSO, TEXT, IMAGE, STICKY, TAPE, STICKER, LASER }

/** Callbacks from the canvas to the editor state. Page indices refer to the notebook's page list. */
interface CanvasHost {
    fun brush(tool: Tool): Brush
    fun shapeKind(): ShapeKind
    fun eraserRadius(): Float
    fun tapeColor(): Int
    fun tapeFreehand(): Boolean
    fun beginEdit(page: Int)
    fun setItems(page: Int, items: List<PageItem>)
    fun onSelectionChanged(page: Int, ids: Set<Long>, screenBounds: RectF?)
    fun onTextTap(page: Int, x: Float, y: Float, existing: TextItem?)
    fun onStickyTap(page: Int, x: Float, y: Float, existing: StickyItem?)
    fun onCurrentPage(index: Int)
    fun onPullAddPage()
    fun undo()
    fun redo()
    fun pdfBitmap(page: Page, widthPx: Int, onReady: () -> Unit): Bitmap?
    fun highlights(page: Int): List<RectF>
    fun activeHighlight(): Pair<Int, RectF>?
    /** Fill colour for new closed shapes, or null for outline only. */
    fun shapeFill(): Int?
    /** A pen loop around content was drawn; show (or hide, with null) the "Select" chip. */
    fun onCircleCandidate(screenBounds: RectF?)
    /** Circle-and-hold made a selection (true) or that selection ended (false). The toolbar shows lasso meanwhile. */
    fun onCircleSelect(active: Boolean) {}
    /** Ink link ends to draw as badges. */
    fun linkEnds(): List<LinkEnd> = emptyList()
    /** A box was dragged for a new flashcard (page space). */
    fun onAreaPicked(page: Int, rect: RectF) {}
    /** The other end of a new ink link was tapped (page space). */
    fun onLinkTarget(page: Int, x: Float, y: Float) {}
    fun onLinkLongPress(id: String) {}
    fun onOpenUrl(url: String) {}
    /** Maths answer reveal in progress (sweep + glow). */
    fun mathAnim(): EditorViewModel.MathAnim? = null
    /** Pattern for new tape ("stripes", "dots", "image:<file>", ...). */
    fun tapePattern(): String = "stripes"
    /** The pen is down (true) or lifted (false): the toolbar fades while you write under it. */
    fun onPenActive(active: Boolean) {}
    /** Sharp viewport render of a zoomed PDF page, if ready. */
    fun pdfDetail(page: Int): EditorViewModel.Detail? = null
    /** (page, visible region in page units, screen px per unit) for zoomed PDF pages. */
    fun requestDetail(wants: List<Triple<Int, RectF, Float>>, onReady: () -> Unit) {}
    /** Long-press on PDF text (page space). Return true when a word was selected. */
    fun onTextPress(page: Int, x: Float, y: Float): Boolean = false
    fun onTextDrag(x: Float, y: Float) {}
    fun onTextEnd(screen: RectF?) {}
    fun onTextClear() {}
    fun textSelection(page: Int): List<RectF> = emptyList()
    fun onEraserLifted()
    fun onZoom(percent: Int?)
    /** Settings for the pen with this input-device name (registers new pens). */
    fun penProfile(deviceName: String): com.pronotes.app.data.PenProfile
    /** Pen button clicked while hovering (no touch). */
    fun onPenButton(doubleClick: Boolean, x: Float, y: Float, deviceName: String)
    /** Colour and on-screen size of the hover dot, or null for no dot. */
    fun hoverInk(): Pair<Int, Float>?
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
        set(value) {
            // Laser ink is a pointer, not ink: it goes away when you pick another tool.
            if (field == Tool.LASER && value != Tool.LASER) layer.clearLasers()
            field = value
            layer.invalidate()
        }

    /** Waiting for a dragged box (new flashcard) or a tap (other end of an ink link). */
    var capturing = false
    var linkPicking = false
    private var capturingDrag = false

    private var linkPress: LinkEnd? = null
    private var linkLongFired = false
    private var linkPressX = 0f
    private var linkPressY = 0f
    private val linkLongRunnable = Runnable { linkPress?.let { linkLongFired = true; buzz(); host.onLinkLongPress(it.id) } }
    /** Target of the last link jump, pulsed for a moment so the eye finds it. */
    private var linkFlash: Triple<Int, Float, Float>? = null
    private var linkFlashAt = 0L

    private fun linkAt(x: Float, y: Float): LinkEnd? {
        val ends = host.linkEnds()
        if (ends.isEmpty()) return null
        val a = FloatArray(2)
        return ends.firstOrNull { e ->
            if (e.page !in pages.indices) return@firstOrNull false
            a[0] = e.x; a[1] = e.y; pageMatrix(e.page, tmpMatrix).mapPoints(a)
            hypot(x - a[0], y - a[1]) < 24f * density
        }
    }

    /** A finger tap (or any tap in read-only mode) inside linked content. */
    private fun linkBoxAt(x: Float, y: Float): LinkEnd? {
        val ends = host.linkEnds()
        if (ends.isEmpty()) return null
        val (dx, dy) = toDoc(x, y)
        val pi = pageAt(dx, dy)
        val (lx, ly) = toLocal(x, y, pi)
        return ends.firstOrNull { e -> e.page == pi && e.box != null && RectF(e.box).apply { inset(-12f, -12f) }.contains(lx, ly) }
    }

    private fun jumpTo(e: LinkEnd) {
        buzz()
        e.url?.let { host.onOpenUrl(it); return }
        if (e.toPage !in pages.indices) return
        scrollToPage(e.toPage, animate = true)
        linkFlash = Triple(e.toPage, e.tx, e.ty); linkFlashAt = SystemClock.uptimeMillis()
        layer.invalidate()
    }

    /** Read-only: pen and fingers only scroll and zoom; nothing can be changed. */
    var readOnly = false
        set(v) { field = v; if (v) { clearSelection(); clearCircleCandidate() } }

    /** Side-button action of the pen currently in use (from its own pen profile). */
    private var penSide: SideButton? = null

    /** True when the app is in dark mode; the desk around pages follows it. */
    var darkUi = false
        set(value) { field = value; layer.invalidate() }

    private fun deskArgb(): Int = when (settings.deskColor) {
        com.pronotes.app.data.DeskColor.AUTO -> if (darkUi) 0xFF141518.toInt() else 0xFFECEEF1.toInt()
        else -> settings.deskColor.argb
    }

    var settings = AppSettings()
        set(value) {
            val relayout = field.scroll != value.scroll ||
                (value.scroll == ScrollDirection.HORIZONTAL && field.toolbarDock != value.toolbarDock)
            field = value
            if (relayout) { val keep = currentIndex; layout(pages); fit(keep) }
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
    /** Page to show on first layout (the one the notebook was closed on). */
    var startPage = 0
    private var pull = 0f                      // overscroll past the last page, screen px
    private val pullTrigger get() = 110f * density

    // selection lives on a single page
    var selection: Set<Long> = emptySet()
        private set
    private var selectionPage = -1
    /** Live move / resize / rotate of the selection, in page coordinates. Baked in on release. */
    private var liveMatrix: Matrix? = null
    private enum class Grab { MOVE, SCALE, ROTATE }
    private var grab = Grab.MOVE
    private var anchorX = 0f
    private var anchorY = 0f
    private var grabStartX = 0f
    private var grabStartY = 0f
    private var liveScale = 1f
    private var liveDeg = 0f

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
                    val added = entries.map { e ->
                        InkItem(ItemIds.next(), e.value).also { item ->
                            pendingCircle?.takeIf { it.strokeId == e.key }?.itemId = item.id
                        }
                    }
                    updatePage(pi, p.items + added)
                }
                wetInk.removeFinishedStrokes(strokes.keys)
            }
        })
    }

    fun redraw() { layer.invalidate() }

    fun pageToScreen(page: Int, r: RectF): RectF? {
        if (page !in pages.indices) return null
        return RectF(r).also { pageMatrix(page, tmpMatrix).mapRect(it) }
    }

    /** Replaces the page list (after undo, page add/delete, recolour...). */
    fun setPages(newPages: List<Page>) {
        val structural = newPages.size != pages.size || newPages.zip(pages).any { (a, b) -> a.id != b.id || a.width != b.width }
        val keepSel = selectionPage in newPages.indices && pages.getOrNull(selectionPage)?.id == newPages[selectionPage].id
        pages = newPages
        if (structural) layout(newPages)
        if (!fitted && width > 0) fit(startPage.coerceIn(0, (newPages.size - 1).coerceAtLeast(0)))
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
                // Horizontal paging: each page sits in a slot one screen wide at fit size,
                // so neighbouring pages stay off screen (one page at a time).
                val fit = horizontalFit(p)
                val slot = if (width > 0 && fit > 0f) max(p.width + gap, width / fit) else p.width + gap
                origins[i * 2] = cursor + (slot - p.width) / 2f
                origins[i * 2 + 1] = (maxH - p.height) / 2f
                cursor += slot
            }
        }
        if (!vertical) cursor += gap   // layout below subtracts one gap
        if (vertical) { docW = maxW; docH = max(0f, cursor - gap) } else { docW = max(0f, cursor - gap); docH = maxH }
    }

    private fun ox(i: Int) = origins.getOrElse(i * 2) { 0f }
    private fun oy(i: Int) = origins.getOrElse(i * 2 + 1) { 0f }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val keep = if (!fitted) startPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0)) else currentIndex
        // Horizontal page slots depend on the screen size.
        if (settings.scroll == ScrollDirection.HORIZONTAL && pages.isNotEmpty()) layout(pages)
        // Height-only changes (bars hiding, keyboard) must not refit: refitting there could jump to an older page.
        val page = if (!scroller.isFinished) lastTarget else keep
        if (pages.isNotEmpty() && (!fitted || w != oldw)) fit(page) else if (settings.scroll == ScrollDirection.HORIZONTAL && pages.isNotEmpty()) { centerOn(page); updateMatrix() } else { clamp(); updateMatrix() }
    }

    private fun fitScale(i: Int = currentIndex): Float {
        val p = pages.getOrNull(i) ?: return 1f
        val margin = 40f * density
        return if (settings.scroll == ScrollDirection.VERTICAL) min((width - margin) / p.width, 3f)
        else horizontalFit(p)
    }

    /** Space kept free for a floating toolbar at the top or bottom edge. */
    /** The floating toolbar sits over the page (and fades while you write), so pages use the full height. */
    private fun toolbarInset(): Float = 0f

    /** Centre pages in the space left by the toolbar (horizontal mode). */
    private fun toolbarShift(): Float = when (settings.toolbarDock) {
        com.pronotes.app.data.ToolbarDock.TOP -> toolbarInset() / 2f
        com.pronotes.app.data.ToolbarDock.BOTTOM -> -toolbarInset() / 2f
        else -> 0f
    }

    /** Whole page visible, clear of the floating toolbar. */
    private fun horizontalFit(p: Page): Float {
        if (width == 0 || height == 0) return 0f
        val margin = 32f * density
        return min((width - margin) / p.width, (height - margin - toolbarInset()) / p.height).coerceAtLeast(0.05f)
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
            transY = (height - docH * scale) / 2f + toolbarShift()
        }
        clamp()
    }

    /** transX that centres page [i] horizontally at the current scale. */
    private fun pageCenterX(i: Int): Float {
        val p = pages.getOrNull(i) ?: return transX
        return (width - p.width * scale) / 2f - ox(i) * scale
    }

    private var lastTarget = 0

    fun scrollToPage(index: Int, animate: Boolean) {
        if (index !in pages.indices || width == 0) return
        lastTarget = index
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
        val restY = if (ch <= height && !vertical) (height - ch) / 2f + toolbarShift() else min(height - ch - pad, pad)
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
        if (idx != currentIndex) {
            currentIndex = idx; host.onCurrentPage(idx)
            if (settings.scroll == ScrollDirection.HORIZONTAL) { syncFeel(); feel.cue(PaperFeel.Cue.PAGE) }
        }
        layer.invalidate()
        reportSelection()
        askForDetail()
    }

    /** Asks for sharp detail of the PDF pages on screen when zoomed past what the base bitmap holds. */
    private fun askForDetail() {
        if (width == 0 || pages.isEmpty()) return
        val wants = ArrayList<Triple<Int, RectF, Float>>()
        val view = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val inv = Matrix()
        pages.forEachIndexed { i, p ->
            if (p.pdfPage < 0 || p.width * scale < 1700f) return@forEachIndexed
            val pm = pageMatrix(i, Matrix())
            val sr = RectF(0f, 0f, p.width, p.height); pm.mapRect(sr)
            if (!RectF.intersects(sr, view)) return@forEachIndexed
            pm.invert(inv)
            val local = RectF(view); inv.mapRect(local)
            local.inset(-local.width() * 0.08f, -local.height() * 0.08f)
            if (!local.intersect(0f, 0f, p.width, p.height)) return@forEachIndexed
            wants += Triple(i, local, scale)
        }
        host.requestDetail(wants) { layer.postInvalidate() }
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
    /** True once two fingers zoomed in this gesture: then no page turning and no fling. */
    private var pinched = false
    /** Fit size of the page the gesture started on, fixed for the whole pinch. */
    private var gestureFit = 1f
    private var maxFingers = 0
    private var fingerDownTime = 0L
    private var fingerMoved = false
    private var fingerStartX = 0f
    private var fingerStartY = 0f

    private val holdRunnable = Runnable { onHold() }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = true

    private var stylusPointer = -1          // pointer id of the pen while it touches the screen
    private var touchActive = false
    private var pageAtGestureStart = 0
    private var lastStylusTime = 0L
    private var ignoreFingers = false       // swallow the rest of a palm / finger gesture
    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f

    private fun isPen(type: Int) = type == MotionEvent.TOOL_TYPE_STYLUS || type == MotionEvent.TOOL_TYPE_ERASER

    /**
     * Routing rules:
     *  1. The pen always wins. If a palm is already on the glass when the pen lands,
     *     the palm gesture is dropped and the pen draws.
     *  2. While the pen is down, every finger/palm event is ignored.
     *  3. Fingers only draw when "Draw with stylus only" is off. Otherwise they scroll and zoom.
     *  4. Palm rejection: a finger that lands right after the pen was near is ignored.
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        predictor.record(event)
        val action = normalizeSamsungAction(event)
        val now = SystemClock.uptimeMillis()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchActive = true
                // A swipe that starts while a page turn is still animating counts from its target page.
                pageAtGestureStart = if (!scroller.isFinished) lastTarget else currentIndex
                layer.invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> touchActive = false
        }
        // Ink link badges answer any finger or pen, even in read-only mode.
        if (event.actionMasked == MotionEvent.ACTION_DOWN && !capturing && !linkPicking) {
            linkAt(event.x, event.y)?.let { hit ->
                linkPress = hit; linkLongFired = false; linkPressX = event.x; linkPressY = event.y
                postDelayed(linkLongRunnable, 600)
                return true
            }
        }
        linkPress?.let { lp ->
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> if (hypot(event.x - linkPressX, event.y - linkPressY) > touchSlop * 5) { removeCallbacks(linkLongRunnable); linkPress = null }
                MotionEvent.ACTION_UP -> { removeCallbacks(linkLongRunnable); linkPress = null; if (!linkLongFired) jumpTo(lp) }
                MotionEvent.ACTION_CANCEL -> { removeCallbacks(linkLongRunnable); linkPress = null }
            }
            return true
        }
        if (readOnly) { handleFingers(event, action); return true }
        val penIndex = (0 until event.pointerCount).firstOrNull { isPen(event.getToolType(it)) }
        if (penIndex != null) { sawStylus = true; lastStylusTime = now; lastPenTouchAt = now }

        // --- pen
        if (stylusPointer == -1 && penIndex != null) {
            val landing = action == MotionEvent.ACTION_DOWN ||
                (action == MotionEvent.ACTION_POINTER_DOWN && isPen(event.getToolType(event.actionIndex)))
            if (landing) {
                // Each pen keeps its own settings.
                penSide = event.device?.name?.let { host.penProfile(it).sideButton }
                if (settings.penHaptics) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                host.onTextClear()
                host.onPenActive(true)
                if (mode == Mode.PAN) endPan() else if (mode != Mode.NONE) cancelCurrent(event)
                stylusPointer = event.getPointerId(if (action == MotionEvent.ACTION_DOWN) 0 else event.actionIndex)
                handleDraw(event, MotionEvent.ACTION_DOWN, stylusPointer)
                return true
            }
        }
        if (stylusPointer != -1) {
            val sIdx = event.findPointerIndex(stylusPointer)
            when {
                action == MotionEvent.ACTION_CANCEL -> { cancelCurrent(event); stylusPointer = -1; ignoreFingers = event.pointerCount > 1; host.onPenActive(false) }
                sIdx < 0 -> { cancelCurrent(event); stylusPointer = -1; host.onPenActive(false) }
                action == MotionEvent.ACTION_UP || (action == MotionEvent.ACTION_POINTER_UP && event.actionIndex == sIdx) -> {
                    host.onPenActive(false)
                    handleDraw(event, MotionEvent.ACTION_UP, stylusPointer)
                    stylusPointer = -1
                    ignoreFingers = action == MotionEvent.ACTION_POINTER_UP
                }
                action == MotionEvent.ACTION_MOVE -> handleDraw(event, MotionEvent.ACTION_MOVE, stylusPointer)
            }
            return true
        }

        // --- fingers
        if (action == MotionEvent.ACTION_DOWN) {
            ignoreFingers = settings.palmRejection && sawStylus && now - lastStylusTime < PALM_WINDOW_MS
        }
        if (ignoreFingers) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) ignoreFingers = false
            return true
        }
        if ((capturing || linkPicking) && event.pointerCount == 1 && mode != Mode.PAN) {
            handleDraw(event, action, event.getPointerId(0))
            return true
        }
        val fingerDraws = !settings.stylusOnly
        if (fingerDraws && mode != Mode.PAN) {
            if (event.pointerCount == 1) {
                handleDraw(event, action, event.getPointerId(0))
                return true
            }
            // A second finger while finger-drawing: switch to scroll / zoom.
            cancelCurrent(event)
            mode = Mode.PAN
            handleFingers(event, MotionEvent.ACTION_POINTER_DOWN)
            return true
        }
        handleFingers(event, action)
        return true
    }

    private fun endPan() {
        mode = Mode.NONE
        maxFingers = 0; fingerDownTime = 0L; pinchStartDist = 0f; pull = 0f
        velocity?.recycle(); velocity = null
        host.onZoom(null)
        clamp(); updateMatrix()
    }

    private fun buzz(kind: Int = HapticFeedbackConstants.LONG_PRESS) {
        if (settings.haptics) performHapticFeedback(kind)
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

    private fun effectiveTool(e: MotionEvent, index: Int = 0): Tool {
        if (e.getToolType(index) == MotionEvent.TOOL_TYPE_ERASER) return Tool.ERASER
        if (sideButtonHeld(e)) {
            when (penSide ?: settings.sideButton) {
                SideButton.ERASER -> return Tool.ERASER
                SideButton.LASSO -> return Tool.LASSO
                SideButton.NONE -> Unit
            }
        }
        return tool
    }

    private fun handleDraw(e: MotionEvent, action: Int, pid: Int) {
        if (pages.isEmpty()) return
        val pIdx = e.findPointerIndex(pid).coerceAtLeast(0)
        val px = e.getX(pIdx)
        val py = e.getY(pIdx)
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                scroller.forceFinished(true)
                requestUnbufferedDispatch(e)
                activePointer = pid
                // Circle to select: press and hold the end of a loop you just drew.
                pendingCircle?.let { pc ->
                    val a = floatArrayOf(pc.endX, pc.endY); pageMatrix(pc.page, tmpMatrix).mapPoints(a)
                    val near = hypot(px - a[0], py - a[1]) < 40f * density
                    val fresh = SystemClock.uptimeMillis() - pc.at < CIRCLE_WINDOW_MS
                    if (near && fresh && pc.itemId != -1L && isPen(e.getToolType(pIdx))) {
                        activePointer = pid
                        circleHoldArmed = true
                        holdDownX = px; holdDownY = py
                        mode = Mode.IGNORE
                        layer.invalidate()
                        postDelayed(circleHoldRunnable, CIRCLE_HOLD_MS)
                        return
                    }
                    clearCircleCandidate()
                }
                val (ddx, ddy) = toDoc(px, py)
                activePage = pageAt(ddx, ddy)
                points.clear()
                holdFired = false
                downTime = SystemClock.uptimeMillis(); downX = px; downY = py
                val (wx, wy) = toLocal(px, py, activePage)
                points += wx to wy
                lastMovePt = wx to wy
                val t = effectiveTool(e, pIdx)
                strokeTool = t
                if (capturing) {
                    mode = Mode.LASSO; capturingDrag = true
                    lassoPoints.clear(); lassoPoints += px to py
                    layer.lasso = lassoPoints; layer.startAnts()
                    return
                }
                if (linkPicking) { mode = Mode.IGNORE; host.onLinkTarget(activePage, wx, wy); return }

                // Handles resize / rotate; touching inside the selection moves it, whatever the tool.
                if (selection.isNotEmpty() && t != Tool.ERASER && beginGrab(px, py)) return
                if (selection.isNotEmpty()) {
                    val wasCircle = circleSelect
                    clearSelection()
                    // After circle-to-select, a tap outside just lets go and goes back to the pen.
                    if (wasCircle) { mode = Mode.IGNORE; return }
                }

                when (t) {
                    Tool.PEN, Tool.HIGHLIGHTER -> startInk(e, t)
                    Tool.SHAPE -> if (host.shapeKind() == ShapeKind.AUTO) startInk(e, t) else {
                        mode = Mode.SHAPE_DRAG
                        layer.shapePreview = null
                    }
                    Tool.ERASER -> {
                        mode = Mode.ERASE
                        syncFeel(); feelX = px; feelY = py; feelT = e.eventTime
                        feel.start(PaperFeel.Texture.ERASER)
                        host.beginEdit(activePage)
                        lastErase = wx to wy
                        eraseAt(wx, wy)
                        layer.eraserAt = px to py
                    }
                    Tool.LASSO -> {
                        mode = Mode.LASSO
                        lassoPoints.clear(); lassoPoints += px to py
                        layer.lasso = lassoPoints
                        layer.startAnts()
                    }
                    Tool.LASER -> {
                        mode = Mode.LASER
                        layer.laserBegin(toDoc(px, py))
                    }
                    Tool.TAPE -> { mode = Mode.TAPE; layer.tapePreview = null }
                    Tool.TEXT, Tool.STICKY -> mode = Mode.TAP
                    Tool.IMAGE, Tool.STICKER -> mode = Mode.IGNORE
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = e.findPointerIndex(activePointer).takeIf { it >= 0 } ?: return
                if (circleHoldArmed) {
                    if (hypot(e.getX(idx) - holdDownX, e.getY(idx) - holdDownY) > touchSlop * 1.5f) {
                        // Not a hold: it was the start of a new stroke after all.
                        circleHoldArmed = false; removeCallbacks(circleHoldRunnable); clearCircleCandidate(); layer.invalidate()
                        handleDraw(e, MotionEvent.ACTION_DOWN, activePointer)
                    }
                    return
                }
                when (mode) {
                    Mode.INK -> {
                        val id = strokeId ?: return
                        val fx = e.getX(idx); val fy = e.getY(idx)
                        feel.move(hypot(fx - feelX, fy - feelY), e.eventTime - feelT)
                        feelX = fx; feelY = fy; feelT = e.eventTime
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
                        feel.move(hypot(e.getX(idx) - feelX, e.getY(idx) - feelY), e.eventTime - feelT)
                        feelX = e.getX(idx); feelY = e.getY(idx); feelT = e.eventTime
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
                        if (settings.lassoMode == com.pronotes.app.data.LassoMode.RECT || capturingDrag) {
                            val (sx, sy) = lassoPoints.first()
                            val cx = e.getX(idx); val cy = e.getY(idx)
                            lassoPoints.clear()
                            lassoPoints += listOf(sx to sy, cx to sy, cx to cy, sx to cy, sx to sy)
                        } else {
                            for (h in 0 until e.historySize) lassoPoints += e.getHistoricalX(idx, h) to e.getHistoricalY(idx, h)
                            lassoPoints += e.getX(idx) to e.getY(idx)
                        }
                        layer.invalidate()
                    }
                    Mode.MOVE -> {
                        val (wx, wy) = toLocal(e.getX(idx), e.getY(idx), activePage)
                        updateGrab(wx, wy)
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
                        if (host.tapeFreehand()) {
                            for (h in 0 until e.historySize) points += toLocal(e.getHistoricalX(idx, h), e.getHistoricalY(idx, h), activePage)
                            points += toLocal(e.getX(idx), e.getY(idx), activePage)
                            layer.tapeFree = ArrayList(points)
                        } else {
                            val (sx, sy) = points.first()
                            val (cx, cy) = toLocal(e.getX(idx), e.getY(idx), activePage)
                            layer.tapePreview = tapeRect(sx, sy, cx, cy)
                        }
                        layer.invalidate()
                    }
                    else -> Unit
                }
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(holdRunnable)
                feel.stop()
                if (mode == Mode.INK && feel.sound) feel.cue(PaperFeel.Cue.LIFT)
                if (circleHoldArmed) { circleHoldArmed = false; removeCallbacks(circleHoldRunnable); layer.invalidate() }
                val isTap = SystemClock.uptimeMillis() - downTime < 350 && hypot(px - downX, py - downY) < touchSlop
                when (mode) {
                    Mode.INK -> finishInk(e, isTap)
                    Mode.ERASE -> {
                        layer.eraserAt = null; lastErase = null; layer.invalidate()
                        if (strokeTool == Tool.ERASER && tool == Tool.ERASER) host.onEraserLifted()
                    }
                    Mode.LASSO -> {
                        val local = lassoPoints.map { toLocal(it.first, it.second, activePage) }
                        layer.lasso = null
                        layer.stopAnts()
                        if (capturingDrag) {
                            capturingDrag = false
                            if (local.size > 3) {
                                val r = RectF(local.minOf { it.first }, local.minOf { it.second }, local.maxOf { it.first }, local.maxOf { it.second })
                                if (r.width() > 20f && r.height() > 20f) host.onAreaPicked(activePage, r)
                            }
                        } else if (local.size > 3) select(activePage, Lasso(local, settings.lassoTouching), outline = local.takeIf { settings.lassoMode != com.pronotes.app.data.LassoMode.RECT })
                        layer.invalidate()
                    }
                    Mode.MOVE -> commitMove()
                    Mode.LASER -> laserUp()
                    Mode.SHAPE_DRAG -> {
                        val shapes = layer.shapePreview
                        layer.shapePreview = null
                        if (shapes != null && !isTap) addShapes(shapes, fillable = host.shapeKind().closed)
                        layer.invalidate()
                    }
                    Mode.TAPE -> {
                        val r = layer.tapePreview
                        val free = layer.tapeFree
                        layer.tapePreview = null; layer.tapeFree = null
                        val (wx, wy) = toLocal(px, py, activePage)
                        when {
                            isTap -> toggleTapeAt(activePage, wx, wy)
                            free != null && pathLength(free) > 24f -> {
                                // Light smoothing so the tape edge is not jittery.
                                val pts = simplifyForTape(free)
                                val abs = FloatArray(pts.size * 2).also { a -> pts.forEachIndexed { k, (x, y) -> a[k * 2] = x; a[k * 2 + 1] = y } }
                                val p = pages[activePage]
                                host.beginEdit(activePage)
                                updatePage(activePage, p.items + freehandTape(ItemIds.next(), abs, host.tapeColor(), 46f, host.tapePattern()))
                            }
                            r != null -> addTape(r)
                            else -> toggleTapeAt(activePage, wx, wy)
                        }
                        layer.invalidate()
                    }
                    Mode.TAP -> {
                        val (wx, wy) = toLocal(px, py, activePage)
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
            MotionEvent.ACTION_CANCEL -> { feel.stop(); cancelCurrent(e) }
        }
    }

    private val feel = PaperFeel(context, this)
    private fun syncFeel() { feel.sound = settings.paperSound; feel.haptics = settings.paperHaptics; feel.volume = settings.paperVolume }
    private var feelX = 0f
    private var feelY = 0f
    private var feelT = 0L

    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); feel.release() }

    private fun startInk(e: MotionEvent, t: Tool) {
        mode = Mode.INK
        syncFeel()
        feelX = e.getX(e.findPointerIndex(activePointer).coerceAtLeast(0)); feelY = e.getY(e.findPointerIndex(activePointer).coerceAtLeast(0)); feelT = e.eventTime
        feel.start(if (t == Tool.HIGHLIGHTER) PaperFeel.Texture.MARKER else PaperFeel.Texture.PENCIL)
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
        if (settings.scribbleToErase && (strokeTool == Tool.PEN || strokeTool == Tool.HIGHLIGHTER) && isScribble(points)) {
            // Only ink and typed text, only what the scribble mostly covers, never pictures, tape or
            // stickies, and the highlighter only erases highlights (and the pen only pen ink).
            val sb = RectF(points.minOf { it.first }, points.minOf { it.second }, points.maxOf { it.first }, points.maxOf { it.second })
            val hit = itemsUnder(activePage, points).filter { hid ->
                val it = p.items.firstOrNull { x -> x.id == hid } ?: return@filter false
                val ok = when (it) {
                    is InkItem -> it.isHighlighter == (strokeTool == Tool.HIGHLIGHTER)
                    is TextItem -> strokeTool == Tool.PEN
                    else -> false
                }
                val b = it.bounds
                val ov = RectF(b); val covered = ov.intersect(sb) && ov.width() * ov.height() >= b.width().coerceAtLeast(1f) * b.height().coerceAtLeast(1f) * 0.35f
                ok && covered
            }.toSet()
            if (hit.isNotEmpty()) {
                wetInk.cancelStroke(id, e); strokePages.remove(id)
                val gone = p.items.filter { it.id in hit }.map { RectF(it.bounds).also { r -> pageMatrix(activePage, tmpMatrix).mapRect(r) } }
                host.beginEdit(activePage)
                updatePage(activePage, p.items.filterNot { it.id in hit })
                layer.puff(gone)
                syncFeel(); feel.cue(PaperFeel.Cue.TAP)
                return
            }
        }
        if (strokeTool == Tool.SHAPE) {
            val shape = ShapeRecognizer.recognize(points)
            if (shape != null) {
                wetInk.cancelStroke(id, e); strokePages.remove(id)
                addShapes(listOf(shape), fillable = isClosedLoop(shape))
                return
            }
        }
        // Highlighter: always straight, or straightened when it was nearly straight anyway.
        if (strokeTool == Tool.HIGHLIGHTER && points.size > 1) {
            val mode = settings.highlighterMode
            val len = pathLength(points)
            val straighten = mode == com.pronotes.app.data.HighlighterMode.STRAIGHT ||
                (mode == com.pronotes.app.data.HighlighterMode.AUTO && isNearlyStraight(points, max(10f, len * 0.07f)))
            if (straighten) {
                wetInk.cancelStroke(id, e); strokePages.remove(id)
                addStraightHighlight(points.first(), points.last())
                return
            }
        }
        // Only a real pen loop (not the highlighter, shapes, eraser or side button) that is big enough
        // and nearly closed offers circle-to-select.
        val loopBox = RectF(points.minOf { it.first }, points.minOf { it.second }, points.maxOf { it.first }, points.maxOf { it.second })
        if (settings.circleToLasso && strokeTool == Tool.PEN && tool == Tool.PEN && !sideButtonHeld(e) &&
            loopBox.width() * scale > 40f * density && loopBox.height() * scale > 30f * density && isClosedLoop(points)) {
            val ids = loopSelects(activePage, points)
            if (ids.isNotEmpty()) {
                val end = points.last()
                pendingCircle = PendingCircle(id, activePage, ids, loop = ArrayList(points), endX = end.first, endY = end.second, at = SystemClock.uptimeMillis())
                layer.invalidate()
            }
        }
        wetInk.finishStroke(e, activePointer, id)
    }

    /** What a hand-drawn loop would select: anything mostly or partly inside. */
    private fun loopSelects(page: Int, loop: List<Pt>): Set<Long> {
        val p = pages.getOrNull(page) ?: return emptySet()
        val lasso = Lasso(loop, settings.lassoTouching, minInside = 0.3f)
        return p.items.filter { lassoAllows(it) && lasso.selects(it) }.map { it.id }.toSet()
    }

    private var circleHoldArmed = false
    private var holdDownX = 0f
    private var holdDownY = 0f
    private val circleHoldRunnable = Runnable { fireCircleHold() }

    /** The end of the loop was held: the loop ink becomes the lasso and the pen can drag at once. */
    private fun fireCircleHold() {
        if (!circleHoldArmed) return
        circleHoldArmed = false
        val pc = pendingCircle ?: return
        pendingCircle = null
        host.onCircleCandidate(null)
        val p = pages.getOrNull(pc.page) ?: return
        host.beginEdit(pc.page)
        updatePage(pc.page, p.items.filterNot { it.id == pc.itemId })
        val ids = pc.ids.filter { id -> pages[pc.page].items.any { it.id == id } }.toSet()
        if (ids.isEmpty()) return
        buzz()
        val screen = pc.loop.map { (x, y) -> val a = floatArrayOf(x, y); pageMatrix(pc.page, tmpMatrix).mapPoints(a); a[0] to a[1] }
        selectItems(pc.page, ids, outline = pc.loop)
        if (!circleSelect) { circleSelect = true; host.onCircleSelect(true) }
        layer.playLassoMorph(screen) {}
        val (lx, ly) = toLocal(holdDownX, holdDownY, pc.page)
        activePage = pc.page
        grab = Grab.MOVE
        mode = Mode.MOVE
        grabStartX = lx; grabStartY = ly
        liveScale = 1f; liveDeg = 0f
        liveMatrix = Matrix()
    }

    private fun addStraightHighlight(a: Pt, b0: Pt) {
        val p = pages.getOrNull(activePage) ?: return
        var b = b0
        // Nearly level lines become perfectly level.
        if (abs(b.second - a.second) < abs(b.first - a.first) * 0.12f) b = b.first to a.second
        val stroke = InkCodec.strokeFromPoints(densifyPublic(listOf(a, b)), host.brush(Tool.HIGHLIGHTER))
        host.beginEdit(activePage)
        updatePage(activePage, p.items + InkItem(ItemIds.next(), stroke))
    }

    private fun trackHold(cur: Pt) {
        val last = lastMovePt
        if (last == null || hypot(cur.first - last.first, cur.second - last.second) * scale > touchSlop) {
            lastMovePt = cur
            removeCallbacks(holdRunnable)
            if (settings.holdToShape || settings.circleToLasso) postDelayed(holdRunnable, HOLD_MS)
        }
    }

    /**
     * A loop drawn by hand: closed by the usual test, or the path wraps almost all the way
     * round its centre (gaps up to about 60° are fine).
     */
    private fun isLooseLoop(pts: List<Pt>): Boolean {
        if (isClosedLoop(pts)) return true
        if (pts.size < 10) return false
        val cx = pts.sumOf { it.first.toDouble() } / pts.size
        val cy = pts.sumOf { it.second.toDouble() } / pts.size
        var turned = 0.0
        var prev = kotlin.math.atan2(pts[0].second - cy, pts[0].first - cx)
        for (i in 1 until pts.size) {
            val a = kotlin.math.atan2(pts[i].second - cy, pts[i].first - cx)
            var d = a - prev
            if (d > Math.PI) d -= 2 * Math.PI
            if (d < -Math.PI) d += 2 * Math.PI
            turned += d; prev = a
        }
        return kotlin.math.abs(turned) > Math.toRadians(300.0)
    }

    /** Pen held still at the end of a stroke: circle-to-lasso or snap to a shape. */
    private fun onHold() {
        if (mode != Mode.INK || holdFired) return
        val id = strokeId ?: return
        val p = pages.getOrNull(activePage) ?: return
        if (points.size < 6 || pathLength(points) < 30f) return

        // A loop around content stays ink while drawing; press and hold its end afterwards to select.
        if (strokeTool == Tool.HIGHLIGHTER) {
            holdFired = true
            cancelWet(id)
            // Hold snaps the highlight: a loop becomes a clean shape, anything else a straight bar.
            val shape = if (isClosedLoop(points)) ShapeRecognizer.recognize(points) else null
            if (shape != null) {
                val p2 = pages[activePage]
                host.beginEdit(activePage)
                updatePage(activePage, p2.items + InkItem(ItemIds.next(), InkCodec.strokeFromPoints(shape, host.brush(Tool.HIGHLIGHTER))))
            } else addStraightHighlight(points.first(), points.last())
            buzz()
            return
        }
        if (settings.holdToShape) {
            val shape = ShapeRecognizer.recognize(points) ?: return
            holdFired = true
            cancelWet(id)
            addShapes(listOf(shape), fillable = strokeTool == Tool.SHAPE && isClosedLoop(shape), brushTool = strokeTool)
            buzz(); syncFeel(); feel.cue(PaperFeel.Cue.SNAP)
        }
    }

    private fun cancelWet(id: InProgressStrokeId) {
        val now = SystemClock.uptimeMillis()
        val ev = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        wetInk.cancelStroke(id, ev)
        strokePages.remove(id)
        ev.recycle()
    }

    /** Lasso filter from settings: which kinds of content can be selected. */
    private fun lassoAllows(item: PageItem): Boolean {
        val f = settings.lassoFilter
        return when (item) {
            is InkItem -> if (item.isHighlighter) f.highlighter else f.ink
            is TextItem -> f.text
            is ImageItem -> f.images
            is FillItem -> f.ink
            else -> f.other
        }
    }

    private fun addShapes(shapes: List<List<Pt>>, fillable: Boolean = false, brushTool: Tool = Tool.SHAPE) {
        val p = pages.getOrNull(activePage) ?: return
        val brush = host.brush(if (brushTool == Tool.PEN) Tool.PEN else Tool.SHAPE)
        val items = ArrayList<PageItem>()
        val fill = if (fillable) host.shapeFill() else null
        val outline = shapes.firstOrNull()
        if (fill != null && outline != null && outline.size > 2) {
            val arr = FloatArray(outline.size * 2)
            outline.forEachIndexed { i, (x, y) -> arr[i * 2] = x; arr[i * 2 + 1] = y }
            items += FillItem(ItemIds.next(), arr, fill)
        }
        shapes.filter { it.size > 1 }.forEach { items += InkItem(ItemIds.next(), InkCodec.strokeFromPoints(it, brush)) }
        if (items.isEmpty()) return
        host.beginEdit(activePage)
        updatePage(activePage, p.items + items)
        if (settings.lassoAfterShape) selectItems(activePage, items.map { it.id }.toSet())
    }

    // ------------------------------------------------------- circle to select

    private class PendingCircle(
        val strokeId: InProgressStrokeId, val page: Int, val ids: Set<Long>, var itemId: Long = -1,
        val loop: List<Pt> = emptyList(), val endX: Float = 0f, val endY: Float = 0f, val at: Long = 0L,
    )

    private var pendingCircle: PendingCircle? = null

    /** The user tapped the "Select" chip: turn the drawn loop into a selection. */
    fun acceptCircle() {
        val pc = pendingCircle ?: return
        pendingCircle = null
        host.onCircleCandidate(null)
        val p = pages.getOrNull(pc.page) ?: return
        if (pc.itemId != -1L) updatePage(pc.page, p.items.filterNot { it.id == pc.itemId })
        selectItems(pc.page, pc.ids.filter { id -> pages[pc.page].items.any { it.id == id } }.toSet())
        buzz()
    }

    fun clearCircleCandidate() {
        if (pendingCircle == null) return
        pendingCircle = null
        host.onCircleCandidate(null)
    }

    /** Keeps points at least 8 units apart, averaging neighbours for a smooth tape path. */
    private fun simplifyForTape(pts: List<Pt>): List<Pt> {
        val spaced = ArrayList<Pt>()
        for (p in pts) if (spaced.isEmpty() || hypot(p.first - spaced.last().first, p.second - spaced.last().second) > 8f) spaced += p
        if (spaced.size < 3) return spaced.ifEmpty { pts.take(2) }
        return spaced.mapIndexed { i, p ->
            if (i == 0 || i == spaced.size - 1) p
            else ((spaced[i - 1].first + p.first * 2 + spaced[i + 1].first) / 4f) to ((spaced[i - 1].second + p.second * 2 + spaced[i + 1].second) / 4f)
        }
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
        updatePage(activePage, p.items + TapeItem(ItemIds.next(), r.left, r.top, r.width(), r.height(), host.tapeColor(), pattern = host.tapePattern()))
    }

    private fun toggleTapeAt(page: Int, x: Float, y: Float) {
        val p = pages.getOrNull(page) ?: return
        val hit = p.items.lastOrNull { it is TapeItem && it.bounds.contains(x, y) } as TapeItem? ?: return
        updatePage(page, p.items.map { if (it.id == hit.id) hit.copy(revealed = !hit.revealed) else it })
        buzz(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun cancelCurrent(e: MotionEvent) {
        removeCallbacks(holdRunnable)
        strokeId?.let { wetInk.cancelStroke(it, e); strokePages.remove(it) }
        strokeId = null
        if (mode == Mode.LASSO) { layer.lasso = null; layer.stopAnts() }
        if (mode == Mode.MOVE) liveMatrix = null
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
        buzz()
    }

    // ----------------------------------------------------------- eraser

    private fun eraseAt(wx: Float, wy: Float) {
        val p = pages.getOrNull(activePage) ?: return
        val prev = lastErase ?: (wx to wy)
        lastErase = wx to wy
        val r = host.eraserRadius() / scale
        if (settings.eraserMode == com.pronotes.app.data.EraserMode.PIXEL) { pixelErase(p, prev, wx, wy, r); return }
        val hit = HashSet<Long>()
        val para = MutableParallelogram()
        for (item in p.items) {
            if (settings.eraseHighlighterOnly && !(item is InkItem && item.isHighlighter)) continue
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

    /** Pixel eraser: cuts strokes where the eraser passes and keeps the rest. */
    private fun pixelErase(p: Page, prev: Pt, wx: Float, wy: Float, r: Float) {
        val steps = max(1, (hypot(wx - prev.first, wy - prev.second) / (r * 0.5f)).toInt())
        var items = p.items
        var changed = false
        for (s in 1..steps) {
            val t = s / steps.toFloat()
            val cx = prev.first + (wx - prev.first) * t
            val cy = prev.second + (wy - prev.second) * t
            val next = ArrayList<PageItem>(items.size)
            for (item in items) {
                if (item !is InkItem || (settings.eraseHighlighterOnly && !item.isHighlighter)) {
                    if (item !is InkItem && !settings.eraseHighlighterOnly && item.bounds.contains(cx, cy) && item !is FillItem) { changed = true; continue }
                    next += item; continue
                }
                val b = item.bounds
                if (cx < b.left - r || cx > b.right + r || cy < b.top - r || cy > b.bottom + r) { next += item; continue }
                val pieces = InkCodec.eraseFrom(item.stroke, cx - item.dx, cy - item.dy, r + item.stroke.brush.size / 2f)
                if (pieces == null) next += item
                else {
                    changed = true
                    pieces.forEach { next += InkItem(ItemIds.next(), it, item.dx, item.dy) }
                }
            }
            items = next
        }
        if (changed) updatePage(activePage, items)
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

    private fun select(page: Int, lasso: Lasso, outline: List<Pt>? = null) {
        val p = pages.getOrNull(page) ?: return
        selectItems(page, p.items.filter { lassoAllows(it) && lasso.selects(it) }.map { it.id }.toSet(), outline)
    }

    /** Page-space loop drawn around a lasso selection (null = plain box). */
    private var selectionOutline: List<Pt>? = null
    private var circleSelect = false

    fun selectItems(page: Int, ids: Set<Long>, outline: List<Pt>? = null) {
        selection = ids
        selectionPage = page
        selectionOutline = if (ids.isEmpty()) null else outline
        layer.invalidate()
        if (ids.isNotEmpty()) { layer.popSelection(); syncFeel(); feel.cue(PaperFeel.Cue.SELECT) }
        reportSelection()
    }

    fun clearSelection() {
        if (circleSelect) { circleSelect = false; host.onCircleSelect(false) }
        if (selection.isEmpty()) return
        selection = emptySet()
        selectionOutline = null
        liveMatrix = null
        layer.invalidate()
        reportSelection()
    }

    private fun selectionPageBounds(): RectF? {
        val p = pages.getOrNull(selectionPage) ?: return null
        val items = p.items.filter { it.id in selection }
        if (items.isEmpty()) return null
        val r = RectF(items.first().bounds)
        items.drop(1).forEach { r.union(it.bounds) }
        r.inset(-12f, -12f)
        liveMatrix?.let { if (grab != Grab.ROTATE) it.mapRect(r) }
        return r
    }

    /** Selection box without live transform, for hit testing and rotate drawing. */
    private fun selectionBaseBounds(): RectF? {
        val keep = liveMatrix; liveMatrix = null
        val r = selectionPageBounds(); liveMatrix = keep
        return r
    }

    private fun handleRadius() = 22f * density
    private fun rotateHandleOffset() = 30f * density

    /** Starts move / resize / rotate when the pen lands on the selection or one of its handles. */
    private fun beginGrab(px: Float, py: Float): Boolean {
        val base = selectionBaseBounds() ?: return false
        val sb = RectF(base); pageMatrix(selectionPage, tmpMatrix).mapRect(sb)
        val (lx, ly) = toLocal(px, py, selectionPage)
        val corners = listOf(sb.left to sb.top, sb.right to sb.top, sb.right to sb.bottom, sb.left to sb.bottom)
        val baseCorners = listOf(base.left to base.top, base.right to base.top, base.right to base.bottom, base.left to base.bottom)
        val hit = corners.indexOfFirst { (cx, cy) -> hypot(px - cx, py - cy) < handleRadius() }
        grab = when {
            hit >= 0 -> { val opp = baseCorners[(hit + 2) % 4]; anchorX = opp.first; anchorY = opp.second; Grab.SCALE }
            hypot(px - sb.centerX(), py - (sb.top - rotateHandleOffset())) < handleRadius() -> {
                anchorX = base.centerX(); anchorY = base.centerY(); Grab.ROTATE
            }
            sb.contains(px, py) -> Grab.MOVE
            else -> return false
        }
        activePage = selectionPage
        mode = Mode.MOVE
        grabStartX = lx; grabStartY = ly
        liveScale = 1f; liveDeg = 0f
        liveMatrix = Matrix()
        return true
    }

    private fun updateGrab(x: Float, y: Float) {
        val m = Matrix()
        when (grab) {
            Grab.MOVE -> m.setTranslate(x - grabStartX, y - grabStartY)
            Grab.SCALE -> {
                val d0 = hypot(grabStartX - anchorX, grabStartY - anchorY).coerceAtLeast(1f)
                liveScale = (hypot(x - anchorX, y - anchorY) / d0).coerceIn(0.1f, 12f)
                m.setScale(liveScale, liveScale, anchorX, anchorY)
            }
            Grab.ROTATE -> {
                val a0 = Math.toDegrees(kotlin.math.atan2((grabStartY - anchorY).toDouble(), (grabStartX - anchorX).toDouble()))
                val a1 = Math.toDegrees(kotlin.math.atan2((y - anchorY).toDouble(), (x - anchorX).toDouble()))
                var deg = (a1 - a0).toFloat()
                val snapped = (Math.round(deg / 15f) * 15).toFloat()
                if (abs(deg - snapped) < 4f) deg = snapped   // snap to 15� steps
                liveDeg = deg
                m.setRotate(deg, anchorX, anchorY)
            }
        }
        liveMatrix = m
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
        val m = liveMatrix
        liveMatrix = null
        if (m != null && !m.isIdentity) {
            host.beginEdit(selectionPage)
            val items = when (grab) {
                Grab.MOVE -> {
                    val v = FloatArray(9); m.getValues(v)
                    p.items.map { if (it.id in selection) it.translated(v[Matrix.MTRANS_X], v[Matrix.MTRANS_Y]) else it }
                }
                Grab.SCALE -> p.items.map { if (it.id in selection) it.transformed(m, liveScale, 0f) else it }
                Grab.ROTATE -> p.items.map { if (it.id in selection) it.transformed(m, 1f, liveDeg) else it }
            }
            updatePage(selectionPage, items)
            // The lasso loop follows the content.
            selectionOutline = selectionOutline?.map { (x, y) -> val a = floatArrayOf(x, y); m.mapPoints(a); a[0] to a[1] }
        }
        liveScale = 1f; liveDeg = 0f
        reportSelection()
    }

    // ----------------------------------------------------------- fingers

    private fun handleFingers(e: MotionEvent, action: Int) {
        if (velocity == null) velocity = VelocityTracker.obtain()
        velocity?.addMovement(e)
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                scroller.forceFinished(true)
                zoomAnim?.cancel()
                mode = Mode.PAN
                panLastX = e.x; panLastY = e.y
                fingerStartX = e.x; fingerStartY = e.y
                maxFingers = 1
                fingerDownTime = SystemClock.uptimeMillis()
                fingerMoved = false
                pinched = false
                textSelecting = false
                // Long-press on PDF text selects it.
                removeCallbacks(textPressRunnable)
                postDelayed(textPressRunnable, 450)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                removeCallbacks(textPressRunnable)
                if (textSelecting) return
                mode = Mode.PAN
                maxFingers = max(maxFingers, e.pointerCount)
                if (fingerDownTime == 0L) fingerDownTime = SystemClock.uptimeMillis()
                if (e.pointerCount >= 2) {
                    if (!pinched) gestureFit = fitScale(pageAtGestureStart.coerceIn(0, max(0, pages.size - 1)))
                    pinched = true
                    scroller.forceFinished(true)
                    pinchStartDist = spacing(e)
                    pinchStartScale = scale
                }
                val (cx, cy) = focus(e)
                panLastX = cx; panLastY = cy
            }
            MotionEvent.ACTION_MOVE -> {
                if (textSelecting) {
                    val (lx, ly) = toLocal(e.x, e.y, textPage)
                    host.onTextDrag(lx, ly)
                    layer.invalidate()
                    return
                }
                val (cx, cy) = focus(e)
                if (hypot(cx - fingerStartX, cy - fingerStartY) > touchSlop * 2) { fingerMoved = true; removeCallbacks(textPressRunnable) }
                if (e.pointerCount >= 2 && pinchStartDist > 0f) {
                    val newScale = (pinchStartScale * spacing(e) / pinchStartDist).coerceIn(gestureFit * 0.9f, 6f)
                    if (abs(newScale - scale) > 0.001f) {
                        fingerMoved = true
                        host.onZoom((newScale / gestureFit * 100f).toInt())
                        val (fx, fy) = toDoc(cx, cy)
                        scale = newScale
                        transX = cx - fx * scale
                        transY = cy - fy * scale
                    }
                }
                var dx = cx - panLastX; var dy = cy - panLastY
                if (pull > 0f) { dx *= 0.45f; dy *= 0.45f }   // rubber band while pulling
                // Horizontal paging at fit size: pages slide sideways only.
                val horizontal = settings.scroll == ScrollDirection.HORIZONTAL
                val paging = !pinched && horizontal && e.pointerCount == 1 && scale <= fitScale() * 1.05f
                if (paging) dy = 0f
                transX += dx
                // Zoomed in (horizontal mode): pan inside the current page only, never across pages.
                if (horizontal && !paging && e.pointerCount == 1 && pages.isNotEmpty()) {
                    val (lo, hi) = pageXRange(pageAtGestureStart.coerceIn(0, pages.size - 1))
                    transX = transX.coerceIn(lo, hi)
                }
                // One swipe = one page: the drag can reach the next or previous page, no further.
                if (paging && pages.isNotEmpty()) {
                    val last = pageAtGestureStart >= pages.size - 1
                    var lo = pageCenterX((pageAtGestureStart + 1).coerceAtMost(pages.size - 1))
                    val hi = pageCenterX((pageAtGestureStart - 1).coerceAtLeast(0))
                    // On the last page the drag may go past it, so "pull to add a page" can trigger.
                    if (last && settings.pullToAddPage) lo -= 220f * density
                    transX = transX.coerceIn(min(lo, hi), max(lo, hi))
                }
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
                removeCallbacks(textPressRunnable)
                if (textSelecting) {
                    textSelecting = false
                    val rects = host.textSelection(textPage)
                    val sb = if (rects.isEmpty()) null else RectF(rects.first()).also { r -> rects.drop(1).forEach { r.union(it) }; pageMatrix(textPage, tmpMatrix).mapRect(r) }
                    host.onTextEnd(sb)
                    mode = Mode.NONE; maxFingers = 0; fingerDownTime = 0L
                    velocity?.recycle(); velocity = null
                    return
                }
                val quick = SystemClock.uptimeMillis() - fingerDownTime < 300
                if (action == MotionEvent.ACTION_UP && quick && !fingerMoved && maxFingers == 1) host.onTextClear()
                if (action == MotionEvent.ACTION_UP && quick && !fingerMoved && settings.fingerTapUndo && !readOnly) {
                    when (maxFingers) {
                        2 -> { host.undo(); syncFeel(); feel.cue(PaperFeel.Cue.UNDO) }
                        3 -> { host.redo(); syncFeel(); feel.cue(PaperFeel.Cue.UNDO) }
                    }
                }
                val link = if (action == MotionEvent.ACTION_UP && maxFingers == 1 && quick && !fingerMoved) linkBoxAt(e.x, e.y) else null
                if (link != null) { jumpTo(link); lastTapTime = 0L }
                else if (action == MotionEvent.ACTION_UP && maxFingers == 1 && quick && !fingerMoved) {
                    val now = SystemClock.uptimeMillis()
                    if (settings.doubleTapZoom && now - lastTapTime < 320 && hypot(e.x - lastTapX, e.y - lastTapY) < touchSlop * 3) {
                        toggleZoom(e.x, e.y)
                        lastTapTime = 0L
                    } else {
                        lastTapTime = now; lastTapX = e.x; lastTapY = e.y
                        if (selection.isNotEmpty()) clearSelection()
                        clearCircleCandidate()
                    }
                }
                host.onZoom(null)
                val pulled = pull >= pullTrigger
                if (pulled) {
                    buzz(); syncFeel(); feel.cue(PaperFeel.Cue.ADD)
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

    private var textSelecting = false
    private var textPage = 0
    private val textPressRunnable = Runnable {
        if (fingerMoved || maxFingers != 1 || mode != Mode.PAN) return@Runnable
        val (dx, dy) = toDoc(fingerStartX, fingerStartY)
        val pi = pageAt(dx, dy)
        val (lx, ly) = toLocal(fingerStartX, fingerStartY, pi)
        if (host.onTextPress(pi, lx, ly)) {
            textSelecting = true; textPage = pi
            scroller.forceFinished(true)
            buzz()
            layer.invalidate()
        }
    }

    /** transX range that keeps page [i] filling the screen sideways (or centred when narrower). */
    private fun pageXRange(i: Int): Pair<Float, Float> {
        val p = pages[i]
        val left = -ox(i) * scale
        val w = p.width * scale
        if (w <= width) { val c = (width - w) / 2f + left; return c to c }
        return (width - w + left) to left
    }

    private fun releaseFling(pulled: Boolean) {
        if (pinched) { pinched = false; settleAfterPinch(); return }
        val vt = velocity ?: return
        vt.computeCurrentVelocity(1000)
        val vx = vt.xVelocity; val vy = vt.yVelocity
        val horizontal = settings.scroll == ScrollDirection.HORIZONTAL
        // A plain tap must not move the page at all, and after a pull the new page's scroll animation must not be overridden by a fling.
        if (!fingerMoved || pulled) return
        val atFit = scale <= fitScale() * 1.05f
        if (horizontal && !atFit && pages.isNotEmpty()) {
            // Zoomed in: fling inside the current page only.
            val (lo, hi) = pageXRange(pageAtGestureStart.coerceIn(0, pages.size - 1))
            val ch = docH * scale
            scroller.fling(
                transX.toInt(), transY.toInt(), vx.toInt(), vy.toInt(), lo.toInt(), hi.toInt(),
                min(0f, height - ch - 24f * density).toInt(), (24f * density).toInt(),
            )
            postInvalidateOnAnimation()
            return
        }
        if (horizontal && atFit && !pulled) {
            // Page-by-page snapping, like turning pages: at most one page per swipe.
            val start = pageAtGestureStart.coerceIn(0, max(0, pages.size - 1))
            var target = currentIndex.coerceIn((start - 1).coerceAtLeast(0), (start + 1).coerceAtMost(pages.size - 1))
            if (vx < -400) target = (start + 1).coerceAtMost(pages.size - 1)
            else if (vx > 400) target = (start - 1).coerceAtLeast(0)
            scale = fitScale(target)
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

    /**
     * After a pinch: stay on the page you were on. Near fit size in page mode it eases back to that
     * page; zoomed out past fit it eases back to fit; zoomed in it simply stays where you left it.
     */
    private fun settleAfterPinch() {
        if (pages.isEmpty() || width == 0) return
        val horizontal = settings.scroll == ScrollDirection.HORIZONTAL
        val page = pageAtGestureStart.coerceIn(0, pages.size - 1)
        val fromS = scale; val fromX = transX; val fromY = transY
        when {
            horizontal && scale <= gestureFit * 1.3f -> { scale = fitScale(page); centerOn(page); lastTarget = page }
            scale < gestureFit -> {
                val (fx, fy) = toDoc(width / 2f, height / 2f)
                scale = gestureFit
                transX = width / 2f - fx * scale; transY = height / 2f - fy * scale
                clamp()
            }
            else -> { clamp(); updateMatrix(); return }
        }
        val toS = scale; val toX = transX; val toY = transY
        scale = fromS; transX = fromX; transY = fromY
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 240; interpolator = DecelerateInterpolator()
            addUpdateListener {
                val t = it.animatedValue as Float
                scale = fromS + (toS - fromS) * t
                transX = fromX + (toX - fromX) * t
                transY = fromY + (toY - fromY) * t
                updateMatrix()
            }
            start()
        }
    }

    /** Double-tap with a finger: zoom in to 2x around the tap, or back to fit. */
    private var zoomAnim: ValueAnimator? = null

    /**
     * Works out the end state first (scale and position), then eases there, so a double tap
     * never lands between pages or on the wrong one.
     */
    private fun toggleZoom(x: Float, y: Float) {
        if (pages.isEmpty()) return
        zoomAnim?.cancel()
        scroller.forceFinished(true)
        val page = currentIndex
        val fit = fitScale(page)
        val zoomIn = scale <= fit * 1.3f
        val fromS = scale; val fromX = transX; val fromY = transY
        val (fx, fy) = toDoc(x, y)
        if (zoomIn) {
            scale = fit * 2f
            transX = x - fx * scale; transY = y - fy * scale
            clamp()
        } else {
            scale = fit
            if (settings.scroll == ScrollDirection.VERTICAL) {
                // Same spot of the page stays under the finger, centred left to right.
                transY = y - fy * scale
                transX = (width - docW * scale) / 2f
                clamp()
            } else centerOn(page)
            lastTarget = page
        }
        val toS = scale; val toX = transX; val toY = transY
        scale = fromS; transX = fromX; transY = fromY
        zoomAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260; interpolator = DecelerateInterpolator()
            addUpdateListener {
                val t = it.animatedValue as Float
                scale = fromS + (toS - fromS) * t
                transX = fromX + (toX - fromX) * t
                transY = fromY + (toY - fromY) * t
                updateMatrix()
            }
            start()
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

    // ------------------------------------------------ pen button click / double click (hovering)
    private var lastPenTouchAt = 0L      // last time the pen touched the screen
    private var buttonDown = false
    private var buttonDownAt = 0L
    private var lastClickAt = 0L
    private var pendingClick: Runnable? = null

    private fun trackPenButton(e: MotionEvent) {
        val held = e.buttonState and (MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_SECONDARY) != 0 ||
            e.actionMasked == MotionEvent.ACTION_BUTTON_PRESS
        val now = SystemClock.uptimeMillis()
        if (held && !buttonDown) { buttonDown = true; buttonDownAt = now; return }
        val released = (!held && buttonDown) || e.actionMasked == MotionEvent.ACTION_BUTTON_RELEASE
        if (!released || !buttonDown) return
        buttonDown = false
        val pressMs = now - buttonDownAt
        // Brushes against the button, long holds, and presses right after writing are not clicks.
        if (pressMs < 40 || pressMs > 450) return
        if (now - lastPenTouchAt < 600) return
        val x = e.x; val y = e.y
        val name = e.device?.name ?: "Stylus"
        pendingClick?.let { removeCallbacks(it) }
        if (now - lastClickAt < 450) {
            pendingClick = null; lastClickAt = 0L
            host.onPenButton(true, x, y, name)
        } else {
            lastClickAt = now
            // Wait so a double click is not also read as a single one; the wait is a setting.
            val wait = settings.penClickDelay.ms.coerceAtLeast(500L)
            pendingClick = Runnable { pendingClick = null; host.onPenButton(false, x, y, name) }.also { postDelayed(it, wait) }
        }
    }

    // Stylus hover: show the eraser ring when the side button is held over the page.
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val tt = event.getToolType(0)
        if (tt == MotionEvent.TOOL_TYPE_STYLUS || tt == MotionEvent.TOOL_TYPE_ERASER) {
            lastStylusTime = SystemClock.uptimeMillis(); sawStylus = true
            trackPenButton(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_HOVER_ENTER -> {
                    val erasing = effectiveTool(event) == Tool.ERASER
                    layer.hoverAt = if (erasing) event.x to event.y else null
                    // Hover preview: a dot in the pen's colour and size where it will touch.
                    layer.hoverDot = if (!erasing && settings.hoverCursor && (tool == Tool.PEN || tool == Tool.HIGHLIGHTER || tool == Tool.SHAPE))
                        host.hoverInk()?.let { (c, s) -> Triple(event.x, event.y, c to s * scale) } else null
                    layer.invalidate()
                }
                MotionEvent.ACTION_HOVER_EXIT -> { layer.hoverAt = null; layer.hoverDot = null; layer.invalidate() }
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }

    fun refresh() = layer.invalidate()

    companion object {
        private const val HOLD_MS = 550L
        /** How long after drawing a loop its end can be held to select. */
        private const val CIRCLE_WINDOW_MS = 6000L
        private const val CIRCLE_HOLD_MS = 450L
        private const val PALM_WINDOW_MS = 300L
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
        var tapeFree: List<Pt>? = null
        /** Hover preview: x, y, and (colour, radius in px). */
        var hoverDot: Triple<Float, Float, Pair<Int, Float>>? = null
        private val hoverPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val hoverRing = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f * density; color = 0x66000000 }

        fun clearLasers() { lasers.clear(); invalidate() }

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
        private val selRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.8f * density; color = 0xFF3D6CF4.toInt()
        }
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
        private val pageNumberPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f * density; textAlign = Paint.Align.CENTER }
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

        private var morph: List<Pt>? = null
        private var morphT = 0f
        private val morphGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = 0xFF3D6CF4.toInt()
        }

        private var puffs: List<RectF> = emptyList()
        private var puffT = 0f
        private val puffPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val puffSeeds = FloatArray(48) { kotlin.random.Random(it * 7 + 3).nextFloat() }

        /** Scribble-erase: erased ink crumbles into little flakes that drift up and fade. */
        fun puff(rects: List<RectF>) {
            if (rects.isEmpty()) return
            puffs = rects
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 520; interpolator = DecelerateInterpolator()
                addUpdateListener { puffT = it.animatedValue as Float; invalidate() }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) { puffs = emptyList(); invalidate() }
                })
                start()
            }
        }

        private fun drawPuffs(canvas: Canvas) {
            if (puffs.isEmpty()) return
            val t = puffT
            puffs.forEach { r ->
                for (k in 0 until 16) {
                    val sx = r.left + r.width() * puffSeeds[k]
                    val sy = r.top + r.height() * puffSeeds[k + 16]
                    val dx = (puffSeeds[k + 32] - 0.5f) * 60f * density * t
                    val dy = -40f * density * t * (0.5f + puffSeeds[k])
                    puffPaint.color = if (k % 3 == 0) 0xFFB8C0CC.toInt() else 0xFF8A93A3.toInt()
                    puffPaint.alpha = ((1f - t) * 220).toInt()
                    canvas.drawCircle(sx + dx, sy + dy, (2.5f + 3f * puffSeeds[k + 8]) * density * (1f - 0.5f * t), puffPaint)
                }
            }
        }

        /** Circle-and-hold: the ink loop glows, turns into a dashed lasso, then [onDone] selects. */
        fun playLassoMorph(screen: List<Pt>, onDone: () -> Unit) {
            morph = screen
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 420; interpolator = DecelerateInterpolator()
                addUpdateListener { morphT = it.animatedValue as Float; invalidate() }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        morph = null; invalidate(); onDone()
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
            lassoFill.alpha = (0x14 + 40 * t).toInt().coerceAtMost(255)
            canvas.drawPath(tmpPath, lassoFill)
            morphGlow.alpha = ((1f - t) * 120).toInt()
            morphGlow.strokeWidth = (4f + 10f * t) * density
            canvas.drawPath(tmpPath, morphGlow)
            lassoPaint.pathEffect = DashPathEffect(floatArrayOf(9f * density * t + 0.1f, 4f * density * t + 0.1f), t * 40f)
            canvas.drawPath(tmpPath, lassoPaint)
            lassoFill.alpha = 0x14
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
            pagePaintBg.color = deskArgb()
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), pagePaintBg)
            r.paperColor = settings.paperTint.argb
            r.highlighterBehind = settings.highlighterBehind
            pageNumberPaint.color = if (Color.luminance(deskArgb()) < 0.4f) 0xFFB0B6C0.toInt() else 0xFF7B8391.toInt()
            val view = RectF(0f, 0f, width.toFloat(), height.toFloat())
            val active = host.activeHighlight()
            pages.forEachIndexed { i, p ->
                val sr = RectF(0f, 0f, p.width, p.height)
                pageMatrix(i, m).mapRect(sr)
                if (!RectF.intersects(sr, view)) return@forEachIndexed
                canvas.save()
                canvas.concat(m)
                val pdf = if (p.pdfPage >= 0) host.pdfBitmap(p, (p.width * scale).toInt()) { postInvalidate() } else null
                r.darkPdf = settings.darkPdf
                r.drawPaper(canvas, p, pdf, shadow = true)
                host.pdfDetail(i)?.let { d -> r.drawPdfDetail(canvas, d.bmp, d.region) }
                val hl = host.highlights(i)
                if (hl.isNotEmpty()) r.drawHighlights(canvas, hl, active?.takeIf { it.first == i }?.second)
                host.textSelection(i).forEach { canvas.drawRoundRect(it, 4f, 4f, textSelPaint) }
                if (i == selectionPage) r.drawItems(canvas, p.items, m, transform = liveMatrix, transformIds = selection)
                else r.drawItems(canvas, p.items, m)
                tapePreview?.takeIf { i == activePage }?.let { canvas.drawRoundRect(it, 6f, 6f, tapePreviewPaint) }
                tapeFree?.takeIf { i == activePage && it.size > 1 }?.let { pts ->
                    tmpPath.reset()
                    pts.forEachIndexed { k, (x, y) -> if (k == 0) tmpPath.moveTo(x, y) else tmpPath.lineTo(x, y) }
                    tapePreviewPaint.style = Paint.Style.STROKE; tapePreviewPaint.strokeWidth = 46f; tapePreviewPaint.strokeJoin = Paint.Join.ROUND
                    canvas.drawPath(tmpPath, tapePreviewPaint)
                    tapePreviewPaint.style = Paint.Style.FILL
                }
                shapePreview?.takeIf { i == activePage }?.forEach { line ->
                    tmpPath.reset()
                    line.forEachIndexed { k, (x, y) -> if (k == 0) tmpPath.moveTo(x, y) else tmpPath.lineTo(x, y) }
                    previewPaint.strokeWidth = 2f * density / scale
                    canvas.drawPath(tmpPath, previewPaint)
                }
                canvas.restore()
                if (settings.pageNumbers && pages.size > 1) {
                    canvas.drawText("${i + 1}", sr.centerX(), sr.bottom + 18f * density, pageNumberPaint)
                }
            }

            drawLinks(canvas)
            drawMathAnim(canvas)
            drawPuffs(canvas)
            drawPull(canvas)
            drawSelection(canvas)
            lasso?.let { drawLasso(canvas, it) }
            drawMorph(canvas)
            drawLoopEnd(canvas)
            (eraserAt ?: hoverAt)?.let { (x, y) ->
                val rad = host.eraserRadius()
                canvas.drawCircle(x, y, rad, eraserFill)
                canvas.drawCircle(x, y, rad, eraserPaint)
            }
            drawLaser(canvas)
            hoverDot?.let { (x, y, cs) ->
                val r = (cs.second / 2f).coerceIn(2f * density, 40f * density)
                hoverPaint.color = (cs.first and 0x00FFFFFF) or (0x99 shl 24)
                canvas.drawCircle(x, y, r, hoverPaint)
                canvas.drawCircle(x, y, r + 1.5f * density, hoverRing)
            }
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

        private val outlinePath = Path()
        private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE }
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        /**
         * Maths answer reveal: a glowing line runs under the sum from its first symbol to the answer,
         * shifting from violet to blue to green, then a soft burst blooms where the answer lands.
         */
        private fun drawMathAnim(canvas: Canvas) {
            val a = host.mathAnim() ?: return
            if (a.page !in pages.indices) return
            val t = (SystemClock.uptimeMillis() - a.start).toFloat()
            val sum = RectF(a.sum); val ans = RectF(a.answer)
            pageMatrix(a.page, m).also { it.mapRect(sum); it.mapRect(ans) }
            // An orange glow rises off the sum and flies in an arc to where the answer lands.
            val x0 = sum.centerX(); val y0 = sum.centerY()
            val x1 = ans.centerX(); val y1 = ans.centerY()
            val lift = (sum.height() * 0.9f).coerceAtLeast(24f * density)
            fun at(q: Float): Pair<Float, Float> {
                val u = 1f - q
                val cx = (x0 + x1) / 2f; val cy = minOf(y0, y1) - lift
                return (u * u * x0 + 2 * u * q * cx + q * q * x1) to (u * u * y0 + 2 * u * q * cy + q * q * y1)
            }
            val p = (t / SWEEP_MS).coerceIn(0f, 1f)
            val e = 1f - (1f - p) * (1f - p) * (1f - p)
            if (p < 1f) {
                // Soft orange wash over the sum, fading as the glow leaves it.
                glowPaint.color = 0xFFFF8A3D.toInt(); glowPaint.alpha = ((1f - e) * 60).toInt()
                canvas.drawRoundRect(sum, 10f * density, 10f * density, glowPaint)
                for (k in 0 until 10) {
                    val q = (e - k * 0.035f).coerceAtLeast(0f)
                    val (hx, hy) = at(q)
                    glowPaint.color = 0xFFFF8A3D.toInt(); glowPaint.alpha = ((1f - k / 10f) * 200).toInt()
                    canvas.drawCircle(hx, hy, (7f - k * 0.5f) * density, glowPaint)
                }
                val (hx, hy) = at(e)
                glowPaint.color = 0xFFFFFFFF.toInt(); glowPaint.alpha = 230
                canvas.drawCircle(hx, hy, 3f * density, glowPaint)
            }
            if (t >= SWEEP_MS) {
                val k = ((t - SWEEP_MS) / 520f).coerceIn(0f, 1f)
                val r = maxOf(ans.width(), ans.height()) * (0.6f + 0.9f * k)
                glowPaint.shader = android.graphics.RadialGradient(ans.centerX(), ans.centerY(), r.coerceAtLeast(1f),
                    intArrayOf((((1f - k) * 0x77).toInt() shl 24) or 0xFF8A3D, 0x00FF8A3D), null, android.graphics.Shader.TileMode.CLAMP)
                glowPaint.alpha = 255
                canvas.drawCircle(ans.centerX(), ans.centerY(), r, glowPaint)
                glowPaint.shader = null
            }
            postInvalidateOnAnimation()
        }
        private val textSelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x553D6CF4 }
        private val linkFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3D6CF4.toInt(); setShadowLayer(6f, 0f, 2f, 0x40000000) }
        private val linkGlyph = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.WHITE; strokeWidth = 1.8f * density; strokeCap = Paint.Cap.ROUND }
        private val linkBoxFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x143D6CF4 }
        private val linkBoxLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.5f * density; color = 0x993D6CF4.toInt()
            pathEffect = DashPathEffect(floatArrayOf(6f * density, 4f * density), 0f)
        }
        private val linkRing = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0xFF3D6CF4.toInt(); strokeWidth = 3f * density }

        /** Blue chain badges at both ends of every ink link, plus a pulse where the last jump landed. */
        private fun drawLinks(canvas: Canvas) {
            val ends = host.linkEnds()
            val a = FloatArray(2)
            val r = 12f * density
            for (e in ends) {
                if (e.page !in pages.indices) continue
                e.box?.let { b ->
                    val sb = RectF(b); pageMatrix(e.page, m).mapRect(sb)
                    if (RectF.intersects(sb, RectF(0f, 0f, width.toFloat(), height.toFloat()))) {
                        canvas.drawRoundRect(sb, 8f * density, 8f * density, linkBoxFill)
                        canvas.drawRoundRect(sb, 8f * density, 8f * density, linkBoxLine)
                    }
                }
                a[0] = e.x; a[1] = e.y; pageMatrix(e.page, m).mapPoints(a)
                if (a[0] < -r || a[1] < -r || a[0] > width + r || a[1] > height + r) continue
                canvas.drawCircle(a[0], a[1], r, linkFill)
                canvas.save(); canvas.rotate(-45f, a[0], a[1])
                val w = 4.2f * density; val h = 2.6f * density
                canvas.drawRoundRect(a[0] - w - 1.2f * density, a[1] - h, a[0] + 1.2f * density, a[1] + h, h, h, linkGlyph)
                canvas.drawRoundRect(a[0] - 1.2f * density, a[1] - h, a[0] + w + 1.2f * density, a[1] + h, h, h, linkGlyph)
                canvas.restore()
            }
            val f = linkFlash ?: return
            val age = SystemClock.uptimeMillis() - linkFlashAt
            if (age > 1400 || f.first !in pages.indices) { linkFlash = null; return }
            a[0] = f.second; a[1] = f.third; pageMatrix(f.first, m).mapPoints(a)
            val t = (age % 700) / 700f
            linkRing.alpha = ((1f - t) * 255).toInt()
            canvas.drawCircle(a[0], a[1], (14f + 26f * t) * density, linkRing)
            postInvalidateOnAnimation()
        }
        private val loopHint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f * density; color = 0xFF3D6CF4.toInt() }
        private val loopDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3D6CF4.toInt() }

        /** Pulsing dot at the end of a fresh loop: "press and hold here to select". */
        private fun drawLoopEnd(canvas: Canvas) {
            val pc = pendingCircle ?: return
            if (pc.itemId == -1L) return
            val age = SystemClock.uptimeMillis() - pc.at
            if (age > CIRCLE_WINDOW_MS) return
            val a = floatArrayOf(pc.endX, pc.endY); pageMatrix(pc.page, m).mapPoints(a)
            val t = if (circleHoldArmed) 1f else (age % 1100) / 1100f
            loopHint.alpha = if (circleHoldArmed) 230 else ((1f - t) * 200).toInt()
            canvas.drawCircle(a[0], a[1], (7f + 12f * t) * density, loopHint)
            canvas.drawCircle(a[0], a[1], 4.5f * density, loopDot)
            postInvalidateOnAnimation()
        }

        private fun drawSelection(canvas: Canvas) {
            val loop = selectionOutline
            if (loop != null && selection.isNotEmpty() && loop.size > 2) {
                outlinePath.reset()
                loop.forEachIndexed { i, (x, y) -> if (i == 0) outlinePath.moveTo(x, y) else outlinePath.lineTo(x, y) }
                outlinePath.close()
                liveMatrix?.let { outlinePath.transform(it) }
                outlinePath.transform(pageMatrix(selectionPage, m))
                lassoFill.alpha = 0x10
                canvas.drawPath(outlinePath, lassoFill)
                lassoFill.alpha = 0x14
                canvas.drawPath(outlinePath, selPaint)
            }
            val rotating = grab == Grab.ROTATE && liveMatrix != null
            val wb = (if (rotating) selectionBaseBounds() else selectionPageBounds()) ?: return
            val sb = RectF(wb)
            pageMatrix(selectionPage, m).mapRect(sb)
            canvas.save()
            canvas.scale(selScale, selScale, sb.centerX(), sb.centerY())
            if (rotating) canvas.rotate(liveDeg, sb.centerX(), sb.centerY())
            val rad = 10f * density
            if (loop == null) canvas.drawRoundRect(sb, rad, rad, selPaint)
            val hr = 6f * density
            // rotate handle
            val ry = sb.top - rotateHandleOffset()
            canvas.drawLine(sb.centerX(), sb.top, sb.centerX(), ry + hr, handlePaint.apply { strokeWidth = 1.5f * density })
            canvas.drawCircle(sb.centerX(), ry, hr + 2f * density, handleFill)
            canvas.drawCircle(sb.centerX(), ry, hr + 2f * density, selRing)
            canvas.drawArc(RectF(sb.centerX() - hr * 0.6f, ry - hr * 0.6f, sb.centerX() + hr * 0.6f, ry + hr * 0.6f), -60f, 280f, false, selRing)
            for ((x, y) in listOf(sb.left to sb.top, sb.right to sb.top, sb.left to sb.bottom, sb.right to sb.bottom)) {
                canvas.drawCircle(x, y, hr, handleFill)
                canvas.drawCircle(x, y, hr, selRing)
            }
            canvas.restore()
            if (rotating) {
                pullText.color = 0xFF3D6CF4.toInt()
                canvas.drawText("${liveDeg.roundToInt()}�", sb.centerX(), sb.bottom + 24f * density, pullText)
                pullText.color = 0xFF5B6475.toInt()
            } else if (grab == Grab.SCALE && liveMatrix != null) {
                pullText.color = 0xFF3D6CF4.toInt()
                canvas.drawText("${(liveScale * 100).roundToInt()}%", sb.centerX(), sb.bottom + 24f * density, pullText)
                pullText.color = 0xFF5B6475.toInt()
            }
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
            // While the pen or a finger is on the screen, laser ink stays put.
            if (touchActive) lasers.forEach { if (it.end != Long.MAX_VALUE && now - it.end <= hold + fade) it.end = now }
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
