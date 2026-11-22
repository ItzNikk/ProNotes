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
import com.pronotes.app.BuildConfig
import com.pronotes.app.R
import com.pronotes.app.data.AppSettings
import com.pronotes.app.data.ScrollDirection
import com.pronotes.app.data.SideButton
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class Tool { PEN, CRAYON, HIGHLIGHTER, ERASER, SHAPE, LASSO, MINDMAP, TEXT, AUTO_TEXT, IMAGE, STICKY, WEBLINK, TAPE, STICKER, LASER }

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
    /** Called when Tool.TEXT taps or drags a new inline text box directly on the page. */
    fun onTextBoxPlace(page: Int, x: Float, y: Float, w: Float, h: Float) { onTextTap(page, x, y, null) }
    fun onStickyTap(page: Int, x: Float, y: Float, existing: StickyItem?)
    fun onStickyToggleCollapse(page: Int, sticky: StickyItem) {}
    fun onStickyChecklistToggle(page: Int, sticky: StickyItem, lineIndex: Int) {}
    fun onWebLinkTap(page: Int, x: Float, y: Float, existing: WebLinkItem?) {}
    fun onMindMapTap(page: Int, x: Float, y: Float, existing: MindNodeItem?) {}
    fun onWriteBoxDoubleTap(page: Int, item: WriteBoxItem) {}
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
    /** Called when user places a new WriteBoxItem on the canvas with Tool.TEXT. */
    fun onPlaceWriteBox(page: Int, x: Float, y: Float, w: Float, h: Float) {}
    /** Called after user pauses writing inside a WriteBoxItem to auto-convert strokes to text. */
    fun onAutoConvertWriteBox(page: Int, boxId: Long) {}
    /** Called when user focuses or taps inside/outside a WriteBoxItem. */
    fun onSelectWriteBox(page: Int, boxId: Long?) {}
    /** Shows a brief non-blocking hint (e.g. 'Shape not recognised'). */
    fun onHint(message: String) {}
    /** Called when scribble-to-erase removed one or more items so an undo snackbar can appear. */
    fun onFreshInk(page: Int, ink: List<InkItem>) {}
    fun onScribbleErased() {}
    /** Long-press on empty page space (not PDF text and not an existing item): opens Quick Insert. */
    fun onEmptyPageLongPress(page: Int, x: Float, y: Float, screenX: Float, screenY: Float) {}
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
class NoteCanvasView(context: Context, internal val host: CanvasHost) : FrameLayout(context) {

    internal val layer = NoteCanvasDocLayer(context, this)
    private val wetInk = InProgressStrokesView(context)
    private val predictor = MotionEventPredictor.newInstance(this)
    internal val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    internal val effectiveTouchSlop: Float
        get() = (touchSlop / settings.touchSensitivity.coerceIn(0.4f, 2.0f)).coerceIn(2f, touchSlop * 2.5f)
    internal val density = resources.displayMetrics.density
    internal val scroller = OverScroller(context, DecelerateInterpolator(1.6f))
    internal var velocity: VelocityTracker? = null

    var activeMindNodeId: Long? = null
        set(value) {
            field = value
            renderer?.activeMindNodeId = value
            layer.invalidate()
        }

    var moveMindSubtree: Boolean = false

    var renderer: PageRenderer? = null
        set(value) {
            field = value
            value?.activeMindNodeId = activeMindNodeId
            layer.invalidate()
        }

    var tool: Tool = Tool.PEN
        set(value) {
            // Laser ink is a pointer, not ink: it goes away when you pick another tool.
            if (field == Tool.LASER && value != Tool.LASER) layer.clearLasers()
            field = value
            layer.hoverDot = null; layer.hoverAt = null
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
    internal var linkFlash: Triple<Int, Float, Float>? = null
    internal var linkFlashAt = 0L

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
    internal fun linkBoxAt(x: Float, y: Float): LinkEnd? {
        val ends = host.linkEnds()
        if (ends.isEmpty()) return null
        val (dx, dy) = toDoc(x, y)
        val pi = pageAt(dx, dy)
        val (lx, ly) = toLocal(x, y, pi)
        return ends.firstOrNull { e -> e.page == pi && e.box != null && RectF(e.box).apply { inset(-12f, -12f) }.contains(lx, ly) }
    }

    internal fun jumpTo(e: LinkEnd) {
        buzz()
        e.url?.let { host.onOpenUrl(it); return }
        if (e.toPage !in pages.indices) return
        scrollToPage(e.toPage, animate = true)
        linkFlash = Triple(e.toPage, e.tx, e.ty); linkFlashAt = SystemClock.uptimeMillis()
        layer.invalidate()
    }

    /** Read-only: pen and fingers only scroll and zoom; nothing can be changed. */
    var readOnly = false
        set(v) {
            field = v
            if (v) {
                cancelCurrent(lastTouchEvent)
                clearSelection()
                clearCircleCandidate()
            }
        }

    /** Side-button action of the pen currently in use (from its own pen profile). */
    private var penSide: SideButton? = null

    /** True when the app is in dark mode; the desk around pages follows it. */
    var darkUi = false
        set(value) { field = value; layer.invalidate() }

    internal fun deskArgb(): Int = when (settings.deskColor) {
        com.pronotes.app.data.DeskColor.AUTO -> if (darkUi) 0xFF141518.toInt() else 0xFFECEEF1.toInt()
        else -> settings.deskColor.argb
    }

    internal var gestureConfig = GestureConfig.fromSettings(AppSettings())

    var settings = AppSettings()
        set(value) {
            val relayout = field.scroll != value.scroll ||
                (value.scroll == ScrollDirection.HORIZONTAL && field.toolbarDock != value.toolbarDock)
            field = value
            gestureConfig = GestureConfig.fromSettings(value)
            if (relayout && pages.isNotEmpty()) {
                val keep = if (fitted) currentIndex else startPage
                layout(pages)
                fit(keep)
            }
            layer.invalidate()
        }

    // ------------------------------------------------------------ document layout
    internal var pages: List<Page> = emptyList()
    private var origins = FloatArray(0)      // x0, y0 per page in document units
    internal var docW = 0f
    internal var docH = 0f
    private val gap = 56f
    var currentIndex = 0
        private set

    // view transform: document -> screen
    internal val viewMatrix = Matrix()
    private val inverse = Matrix()
    internal var scale = 1f
    internal var transX = 0f
    internal var transY = 0f
    private var fitted = false
    /** Page to show on first layout (the one the notebook was closed on). */
    var startPage = 0
    internal var pull = 0f                      // overscroll past the last page, screen px
    internal val pullTrigger get() = 110f * density

    // selection lives on a single page
    var selection: Set<Long> = emptySet()
        private set
    internal var selectionPage = -1
    /** Live move / resize / rotate of the selection, in page coordinates. Baked in on release. */
    internal var liveMatrix: Matrix? = null
    internal enum class Grab { MOVE, SCALE, STRETCH_L, STRETCH_R, STRETCH_T, STRETCH_B, ROTATE }
    internal var grab = Grab.MOVE
    internal var anchorX = 0f
    internal var anchorY = 0f
    internal var grabStartX = 0f
    internal var grabStartY = 0f
    internal var liveScale = 1f
    internal var liveDeg = 0f
    internal val grabBase = RectF()
    internal val liveBox = RectF()
    internal var liveScaleX = 1f
    internal var liveScaleY = 1f

    private val strokePages = HashMap<InProgressStrokeId, Int>()
    private val strokeTools = HashMap<InProgressStrokeId, Tool>()
    private val consumedCircleStrokes = HashSet<InProgressStrokeId>()

    init {
        setWillNotDraw(false)
        addView(layer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(wetInk, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        wetInk.addFinishedStrokesListener(object : InProgressStrokesFinishedListener {
            override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                val byPage = strokes.entries.groupBy { strokePages.remove(it.key) ?: currentIndex }
                for ((pi, entries) in byPage) {
                    val p = pages.getOrNull(pi)
                    val entryTools = entries.associate { it.key to (strokeTools.remove(it.key) ?: tool) }
                    val keptEntries = if (p == null) emptyList() else entries.filterNot { e ->
                        consumedCircleStrokes.remove(e.key)
                    }
                    if (p == null || keptEntries.isEmpty()) continue
                    val added = ArrayList<InkItem>(keptEntries.size)
                    val autoTextAdded = ArrayList<InkItem>()
                    for (e in keptEntries) {
                        val item = InkItem(ItemIds.next(), e.value)
                        if (!circleDetector.attachItemId(e.key, item.id)) {
                            added += item
                            if (entryTools[e.key] == Tool.AUTO_TEXT) autoTextAdded += item
                        }
                    }
                    if (added.isEmpty()) continue
                    host.beginEdit(pi)
                    updatePage(pi, p.items + added)
                    val boxes = pages.getOrNull(pi)?.items?.filterIsInstance<WriteBoxItem>().orEmpty()
                    if (autoTextAdded.isNotEmpty()) {
                        host.onFreshInk(pi, autoTextAdded)
                    } else if (settings.toolDefaults.autoHandwriting) {
                        host.onFreshInk(pi, added.filter { ink ->
                            !ink.isHighlighter && boxes.none { RectF.intersects(it.raw, ink.bounds) }
                        })
                    }
                    if (boxes.isNotEmpty()) {
                        for (wb in boxes) {
                            val boxRect = wb.raw
                            val hit = added.any { ink -> RectF.intersects(boxRect, ink.bounds) || boxRect.contains(ink.bounds.centerX(), ink.bounds.centerY()) }
                            if (hit) {
                                scheduleAutoConvert(pi, wb.id)
                            }
                        }
                    }
                }
                wetInk.removeFinishedStrokes(strokes.keys)
            }
        })
    }

    private var dragLocation: Pt? = null
    private val scrollDuringDrop = object : Runnable {
        override fun run() {
            val (x, y) = dragLocation ?: return
            val horizontal = settings.scroll == ScrollDirection.HORIZONTAL
            val edge = 44f * density
            val distance = if (horizontal) x else y
            val extent = if (horizontal) width else height
            val step = when {
                distance < edge -> (1f - distance / edge).coerceIn(0f, 1f)
                distance > extent - edge -> -((distance - (extent - edge)) / edge).coerceIn(0f, 1f)
                else -> 0f
            } * 12f * density
            if (step != 0f) {
                scroller.forceFinished(true)
                if (horizontal) transX += step else transY += step
                if (!isWhiteboard) clamp()
                updateMatrix()
            }
            invalidate()
            postOnAnimation(this)
        }
    }

    fun previewDrop(x: Float, y: Float) {
        val first = dragLocation == null
        dragLocation = x to y
        if (first) postOnAnimation(scrollDuringDrop)
    }

    fun clearDropPreview() {
        dragLocation = null
        removeCallbacks(scrollDuringDrop)
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val (x, y) = dragLocation ?: return
        if (dropPoint(x, y) == null) return
        val paint = dropPreviewPaint
        canvas.drawCircle(x, y, 17f * density, paint)
        canvas.drawLine(x - 24f * density, y, x + 24f * density, y, paint)
        canvas.drawLine(x, y - 24f * density, x, y + 24f * density, paint)
    }

    private val dropPreviewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF357EF5.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }

    fun dropPoint(x: Float, y: Float): Triple<Int, Float, Float>? {
        val (dx, dy) = toDoc(x, y)
        val page = pageAt(dx, dy)
        val sheet = pages.getOrNull(page) ?: return null
        val (px, py) = toLocal(x, y, page)
        return if (px in 0f..sheet.width && py in 0f..sheet.height) Triple(page, px, py) else null
    }

    fun redraw() { layer.invalidate() }

    var editingTextItemId: Long?
        get() = renderer?.hiddenTextItemId
        set(value) {
            if (renderer?.hiddenTextItemId != value) {
                renderer?.hiddenTextItemId = value
                layer.invalidate()
            }
        }

    fun currentZoomScale(): Float = scale

    fun pageToScreen(page: Int, r: RectF): RectF? {
        if (page !in pages.indices) return null
        return RectF(r).also { pageMatrix(page, tmpMatrix).mapRect(it) }
    }

    /** Replaces the page list (after undo, page add/delete, recolour...). */
    fun setPages(newPages: List<Page>) {
        val structural = newPages.size != pages.size || newPages.zip(pages).any { (a, b) -> a.id != b.id || a.width != b.width || a.height != b.height }
        val keepSel = selectionPage in newPages.indices && pages.getOrNull(selectionPage)?.id == newPages[selectionPage].id
        pages = newPages
        if (structural) layout(newPages)
        if (!fitted && width > 0 && newPages.isNotEmpty()) fit(startPage.coerceIn(0, (newPages.size - 1).coerceAtLeast(0)))
        if (keepSel) {
            val ids = newPages[selectionPage].items.map { it.id }.toSet()
            selection = selection.filter { it in ids }.toSet()
        } else selection = emptySet()
        clamp()
        updateMatrix()
        reportSelection()
    }

    internal fun updatePage(index: Int, items: List<PageItem>) {
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

    internal fun ox(i: Int) = origins.getOrElse(i * 2) { 0f }
    internal fun oy(i: Int) = origins.getOrElse(i * 2 + 1) { 0f }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val keep = if (!fitted) startPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0)) else currentIndex
        // Horizontal page slots depend on the screen size.
        if (settings.scroll == ScrollDirection.HORIZONTAL && pages.isNotEmpty()) layout(pages)
        // Height-only changes (bars hiding, keyboard) must not refit: refitting there could jump to an older page.
        val page = if (!scroller.isFinished) lastTarget else keep
        if (pages.isNotEmpty() && (!fitted || w != oldw)) fit(page) else if (settings.scroll == ScrollDirection.HORIZONTAL && pages.isNotEmpty()) { centerOn(page); updateMatrix() } else { clamp(); updateMatrix() }
    }

    internal fun fitScale(i: Int = currentIndex): Float {
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

    internal fun centerOn(index: Int) {
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
    internal fun pageCenterX(i: Int): Float {
        val p = pages.getOrNull(i) ?: return transX
        return (width - p.width * scale) / 2f - ox(i) * scale
    }

    internal var lastTarget = 0

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

    /** Smoothly pans & zooms the camera so [rect] (in [pageIndex]'s coordinates) fits cleanly on screen. */
    fun fitRect(pageIndex: Int, rect: RectF, animate: Boolean = true) {
        val p = pages.getOrNull(pageIndex) ?: return
        if (width == 0 || height == 0 || rect.width() <= 1f || rect.height() <= 1f) return
        zoomAnim?.cancel()
        scroller.forceFinished(true)
        val fromS = scale; val fromX = transX; val fromY = transY
        val padPx = 48f * density
        val availW = (width - padPx * 2f).coerceAtLeast(120f)
        val availH = (height - padPx * 2f).coerceAtLeast(120f)
        val targetScale = min(availW / rect.width(), availH / rect.height()).coerceIn(fitScale(pageIndex) * 0.6f, 3.2f)
        val docCenterX = ox(pageIndex) + rect.centerX()
        val docCenterY = oy(pageIndex) + rect.centerY()
        scale = targetScale
        transX = width / 2f - docCenterX * scale
        transY = height / 2f - docCenterY * scale
        if (!p.isWhiteboard) clamp()
        val toS = scale; val toX = transX; val toY = transY
        if (!animate) { updateMatrix(); return }
        scale = fromS; transX = fromX; transY = fromY
        zoomAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320; interpolator = DecelerateInterpolator()
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

    internal val isWhiteboard: Boolean
        get() = pages.getOrNull(currentIndex)?.isWhiteboard == true

    internal fun clamp(allowPull: Boolean = false) {
        if (isWhiteboard) {
            pull = 0f
            return
        }
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

    internal fun updateMatrix() {
        viewMatrix.reset()
        viewMatrix.postScale(scale, scale)
        viewMatrix.postTranslate(transX, transY)
        viewMatrix.invert(inverse)
        val idx = computeCurrent()
        if (pages.isNotEmpty() && idx != currentIndex) {
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

    internal fun toDoc(x: Float, y: Float): Pt {
        val a = floatArrayOf(x, y)
        inverse.mapPoints(a)
        return a[0] to a[1]
    }

    internal fun pageAt(dx: Float, dy: Float): Int {
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

    internal fun toLocal(x: Float, y: Float, page: Int): Pt {
        val (dx, dy) = toDoc(x, y)
        return (dx - ox(page)) to (dy - oy(page))
    }

    internal fun pageMatrix(page: Int, out: Matrix): Matrix {
        out.set(viewMatrix); out.preTranslate(ox(page), oy(page)); return out
    }

    // ------------------------------------------------------------------ input

    internal enum class Mode { NONE, INK, ERASE, LASSO, MOVE, LASER, PAN, TAP, SHAPE_DRAG, TAPE, MIND_DRAG, WRITE_BOX_DRAG, IGNORE }

    private var pendingConvertPage = -1
    private var pendingConvertBoxId = -1L
    private val autoConvertRunnable = Runnable {
        val p = pendingConvertPage
        val bid = pendingConvertBoxId
        if (p >= 0 && bid != -1L) {
            host.onAutoConvertWriteBox(p, bid)
        }
    }

    private fun scheduleAutoConvert(page: Int, boxId: Long) {
        removeCallbacks(autoConvertRunnable)
        pendingConvertPage = page
        pendingConvertBoxId = boxId
        postDelayed(autoConvertRunnable, 900L)
    }

    private var lastTouchEvent: MotionEvent? = null
    private fun recordLastTouchEvent(e: MotionEvent) {
        lastTouchEvent?.recycle()
        lastTouchEvent = MotionEvent.obtain(e)
    }

    private fun holdSlopPx(): Float = max(effectiveTouchSlop, gestureConfig.holdSlopDp * density / settings.touchSensitivity.coerceIn(0.5f, 1.8f))

    internal var mode = Mode.NONE
    private var activePointer = -1
    internal var activePage = 0
    private var strokeId: InProgressStrokeId? = null
    private var strokeTool = Tool.PEN
    private val points = ArrayList<Pt>()       // page-local points of the current gesture
    private val lassoPoints = ArrayList<Pt>()  // screen points
    internal var lastErase: Pt? = null
    private var dragMindId: Long = -1L
    private var dragMindOrigX = 0f
    private var dragMindOrigY = 0f
    private var dragMindOrigPos: Map<Long, Pair<Float, Float>> = emptyMap()
    private var dragMindOrigPage = 0
    private var dragMindMoved = false
    private var dragMindDownWx = 0f
    private var dragMindDownWy = 0f
    private var holdFired = false
    private var lastMovePt: Pt? = null
    private var downTime = 0L
    private var downX = 0f
    private var downY = 0f
    private var sawStylus = false

    // finger gesture state
    internal var panLastX = 0f
    internal var panLastY = 0f
    internal var pinchStartDist = 0f
    internal var pinchStartScale = 1f
    /** True once two fingers zoomed in this gesture: then no page turning and no fling. */
    internal var pinched = false
    /** Fit size of the page the gesture started on, fixed for the whole pinch. */
    internal var gestureFit = 1f
    internal var maxFingers = 0
    internal var fingerDownTime = 0L
    internal var fingerMoved = false
    internal var fingerStartX = 0f
    internal var fingerStartY = 0f

    private val holdRunnable = Runnable { onHold() }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = true

    private var stylusPointer = -1          // pointer id of the pen while it touches the screen
    internal var touchActive = false
    internal var pageAtGestureStart = 0
    private var lastStylusTime = 0L
    private var ignoreFingers = false       // swallow the rest of a palm / finger gesture
    internal var lastTapTime = 0L
    internal var lastTapX = 0f
    internal var lastTapY = 0f

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
        recordLastTouchEvent(event)
        predictor.record(event)
        val action = normalizeStylusAction(event)
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
                MotionEvent.ACTION_MOVE -> if (hypot(event.x - linkPressX, event.y - linkPressY) > effectiveTouchSlop * 5) { removeCallbacks(linkLongRunnable); linkPress = null }
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
                // The hover preview must never stay behind as a ghost dot once the pen touches.
                clearHover()
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
                action == MotionEvent.ACTION_CANCEL -> {
                    touchActive = false
                    cancelCurrent(event)
                    stylusPointer = -1
                    ignoreFingers = event.pointerCount > 1
                    host.onPenActive(false)
                }
                sIdx < 0 -> {
                    touchActive = false
                    cancelCurrent(event)
                    stylusPointer = -1
                    host.onPenActive(false)
                }
                action == MotionEvent.ACTION_UP || (action == MotionEvent.ACTION_POINTER_UP && event.actionIndex == sIdx) -> {
                    touchActive = false
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
            val palmMs = (gestureConfig.palmRejectionWindowMs / settings.touchSensitivity.coerceIn(0.4f, 2.0f)).toLong()
            ignoreFingers = settings.stylusOnly && settings.palmRejection && sawStylus && now - lastStylusTime < palmMs
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
                if (action == MotionEvent.ACTION_DOWN) host.onPenActive(true)
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) host.onPenActive(false)
                handleDraw(event, action, event.getPointerId(0))
                return true
            }
            // A second finger while finger-drawing: switch to scroll / zoom.
            host.onPenActive(false)
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

    internal fun buzz(kind: Int = HapticFeedbackConstants.LONG_PRESS) {
        if (settings.haptics) performHapticFeedback(kind)
    }

    /** Legacy S Pen firmware reports button-held events with custom action codes 211-214. */
    private fun normalizeStylusAction(e: MotionEvent): Int = when (e.actionMasked) {
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


    private fun beginDrawDown(e: MotionEvent, pIdx: Int, pid: Int, px: Float, py: Float) {
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

        // Collapsed sticky messaging icon expands on any tap; expanded sticky folds via top-right pill.
        if (t in setOf(Tool.PEN, Tool.CRAYON, Tool.AUTO_TEXT, Tool.STICKY, Tool.TEXT, Tool.HIGHLIGHTER)) {
            val hitSticky = pages.getOrNull(activePage)?.items?.filterIsInstance<StickyItem>()
                ?.lastOrNull { s ->
                    if (s.collapsed) s.raw.contains(wx, wy)
                    else s.raw.contains(wx, wy) && wx >= s.raw.right - 56f && wy <= s.raw.top + 48f
                }
            if (hitSticky != null) {
                buzz(HapticFeedbackConstants.CLOCK_TICK)
                host.onStickyToggleCollapse(activePage, hitSticky)
                mode = Mode.IGNORE
                return
            }
        }

        when (t) {
            Tool.PEN, Tool.CRAYON, Tool.HIGHLIGHTER, Tool.AUTO_TEXT -> {
                if (t == Tool.PEN || t == Tool.CRAYON) {
                    val hitBox = pages.getOrNull(activePage)?.items?.filterIsInstance<WriteBoxItem>()?.lastOrNull { it.raw.contains(wx, wy) }
                    if (hitBox != null) {
                        removeCallbacks(autoConvertRunnable)
                        host.onSelectWriteBox(activePage, hitBox.id)
                    }
                }
                if (settings.toolDefaults.alignGuideLine && (t == Tool.AUTO_TEXT || (t == Tool.PEN && settings.toolDefaults.alignHandwriting))) {
                    layer.guideLinePage = activePage
                    layer.guideLineX = wx
                    layer.guideLineY = wy + 22f
                }
                startInk(e, pid, t)
            }
            Tool.SHAPE -> if (host.shapeKind() == ShapeKind.AUTO) startInk(e, pid, t) else {
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
            Tool.MINDMAP -> {
                val p = pages.getOrNull(activePage)
                val allItems = p?.items ?: emptyList()
                val hidden = renderer?.hiddenMindNodeIds(allItems) ?: emptySet()
                val hit = allItems.lastOrNull { it is MindNodeItem && it.id !in hidden && it.bounds.contains(wx, wy) } as MindNodeItem?
                if (hit != null) {
                    dragMindId = hit.id
                    dragMindOrigX = hit.x
                    dragMindOrigY = hit.y
                    dragMindOrigPage = activePage
                    dragMindMoved = false
                    dragMindDownWx = wx
                    dragMindDownWy = wy
                    dragMindOrigPos = if (moveMindSubtree) {
                        val subIds = HashSet<Long>().apply { add(hit.id) }
                        var added = true
                        while (added) {
                            added = false
                            for (it in allItems) {
                                if (it is MindNodeItem && it.parentId in subIds && subIds.add(it.id)) added = true
                            }
                        }
                        allItems.filterIsInstance<MindNodeItem>().filter { it.id in subIds }.associate { it.id to (it.x to it.y) }
                    } else {
                        mapOf(hit.id to (hit.x to hit.y))
                    }
                    mode = Mode.MIND_DRAG
                } else {
                    mode = Mode.TAP
                }
            }
            Tool.TEXT -> {
                val allItems = pages[activePage].items
                val hitBox = allItems.filterIsInstance<WriteBoxItem>().lastOrNull { it.raw.contains(wx, wy) }
                if (hitBox != null) {
                    removeCallbacks(autoConvertRunnable)
                    host.onSelectWriteBox(activePage, hitBox.id)
                    // Tapping the top-right status badge toggles Ink/Text or converts immediately
                    if (wx >= hitBox.raw.right - 220f && wy <= hitBox.raw.top + 44f) {
                        buzz()
                        host.onWriteBoxDoubleTap(activePage, hitBox)
                        mode = Mode.IGNORE
                    } else {
                        // Write directly inside the box with real low-latency ink!
                        strokeTool = Tool.PEN
                        startInk(e, pid, Tool.PEN)
                    }
                } else {
                    val hitText = allItems.filterIsInstance<TextItem>().lastOrNull { it.bounds.contains(wx, wy) }
                    if (hitText != null) {
                        mode = Mode.TAP
                    } else {
                        mode = Mode.WRITE_BOX_DRAG
                        layer.writeBoxPreview = null
                    }
                }
            }
            Tool.STICKY, Tool.WEBLINK -> mode = Mode.TAP
            Tool.IMAGE, Tool.STICKER -> mode = Mode.IGNORE
        }
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
                val now = SystemClock.uptimeMillis()
                // Circle to select: press and hold the end of a closed loop drawn within the short window.
                val armed = circleDetector.tryArmHold(
                    px = px,
                    py = py,
                    nowMs = now,
                    isPen = isPen(e.getToolType(pIdx)),
                    density = density,
                    config = gestureConfig,
                ) { page, x, y ->
                    val a = floatArrayOf(x, y)
                    pageMatrix(page, tmpMatrix).mapPoints(a)
                    a[0] to a[1]
                }
                if (armed) {
                    armedDownEvent?.recycle()
                    armedDownEvent = MotionEvent.obtain(e)
                    mode = Mode.IGNORE
                    layer.invalidate()
                    postDelayed(circleHoldRunnable, gestureConfig.circleHoldDelayMs)
                    return
                }
                host.onCircleCandidate(null)
                beginDrawDown(e, pIdx, pid, px, py)
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = e.findPointerIndex(activePointer).takeIf { it >= 0 } ?: return
                if (circleDetector.isHoldArmed()) {
                    if (!circleDetector.shouldCancelHoldOnMove(e.getX(idx), e.getY(idx), holdSlopPx())) {
                        return
                    }
                    // Movement past slop: replay the preserved ACTION_DOWN first so zero initial points are dropped.
                    removeCallbacks(circleHoldRunnable)
                    circleDetector.reset()
                    host.onCircleCandidate(null)
                    val downEv = armedDownEvent
                    armedDownEvent = null
                    if (downEv != null) {
                        try {
                            val dIdx = downEv.findPointerIndex(activePointer).coerceAtLeast(0)
                            beginDrawDown(downEv, dIdx, activePointer, downEv.getX(dIdx), downEv.getY(dIdx))
                        } finally {
                            downEv.recycle()
                        }
                    } else {
                        beginDrawDown(e, idx, activePointer, e.getX(idx), e.getY(idx))
                    }
                    layer.invalidate()
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
                        val remapped = remapInkEvent(e, activePointer)
                        val remappedPred = predicted?.let { remapInkEvent(it, activePointer) }
                        try {
                            wetInk.addToStroke(remapped, activePointer, id, remappedPred)
                        } finally {
                            if (remapped !== e) remapped.recycle()
                            if (remappedPred != null && remappedPred !== predicted) remappedPred.recycle()
                            predicted?.recycle()
                        }
                    }
                    Mode.ERASE -> {
                        // A recognized scribble erases its target once. The trailing lift must never become a free eraser.
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
                        layer.shapePreview = runCatching { ShapeBuilder.build(host.shapeKind(), sx, sy, cx, cy) }.getOrNull() ?: layer.shapePreview
                        layer.invalidate()
                    }
                    Mode.WRITE_BOX_DRAG -> {
                        val (wx, wy) = toLocal(e.getX(idx), e.getY(idx), activePage)
                        val (x0, y0) = points.first()
                        if (hypot(wx - x0, wy - y0) > 16f) {
                            val page = pages[activePage]
                            val l = min(x0, wx).coerceIn(20f, page.width - 180f)
                            val r = max(x0, wx).coerceIn(l + 160f, page.width - 20f)
                            val t = min(y0, wy).coerceIn(20f, page.height - 110f)
                            val b = max(y0, wy).coerceIn(t + 90f, page.height - 20f)
                            layer.writeBoxPreview = RectF(l, t, r, b)
                            layer.invalidate()
                        }
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
                    Mode.MIND_DRAG -> {
                        val (wx, wy) = toLocal(e.getX(idx), e.getY(idx), dragMindOrigPage)
                        val dx = wx - dragMindDownWx
                        val dy = wy - dragMindDownWy
                        if (!dragMindMoved && hypot(dx, dy) * scale > effectiveTouchSlop * 0.6f) {
                            dragMindMoved = true
                            host.beginEdit(dragMindOrigPage)
                        }
                        if (dragMindMoved) {
                            val p = pages.getOrNull(dragMindOrigPage) ?: return
                            val updated = p.items.map { item ->
                                val orig = if (item is MindNodeItem) dragMindOrigPos[item.id] else null
                                if (item is MindNodeItem && orig != null) {
                                    var nx = orig.first + dx
                                    var ny = orig.second + dy
                                    if (!p.isWhiteboard) {
                                        val maxX = (p.width - item.w - 56f).coerceAtLeast(56f)
                                        val maxY = (p.height - item.h - 56f).coerceAtLeast(56f)
                                        nx = nx.coerceIn(56f, maxX)
                                        ny = ny.coerceIn(56f, maxY)
                                    }
                                    item.copy(x = nx, y = ny)
                                } else item
                            }
                            pages = pages.toMutableList().also { it[dragMindOrigPage] = p.copy(items = updated) }
                            layer.invalidate()
                        }
                    }
                    else -> Unit
                }
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(holdRunnable)
                feel.stop()
                if (mode == Mode.INK && feel.sound) feel.cue(PaperFeel.Cue.LIFT)
                if (circleDetector.isHoldArmed()) {
                    circleDetector.reset()
                    removeCallbacks(circleHoldRunnable)
                    armedDownEvent?.recycle(); armedDownEvent = null
                    host.onCircleCandidate(null)
                    clearTransientExitState(cancelWetEvent = null, clearWet = false)
                    assertNoSurvivingWetStroke("pen-up", null)
                    return
                }
                val isTap = SystemClock.uptimeMillis() - downTime < 350 && hypot(px - downX, py - downY) < effectiveTouchSlop
                val upStrokeId = strokeId
                when (mode) {
                    Mode.INK -> finishInk(e, isTap)
                    Mode.ERASE -> {
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
                        } else {
                            val dragLen = if (local.size > 1) pathLength(local) else 0f
                            if (isTap || local.size <= 3 || dragLen < 18f / scale.coerceAtLeast(0.25f)) {
                                // Tap-to-select in Lasso mode: pick the topmost selectable item under the tap.
                                val (wx, wy) = toLocal(px, py, activePage)
                                val hitId = topSelectableItemAt(activePage, wx, wy)
                                if (hitId != null) {
                                    selectItems(activePage, setOf(hitId), outline = null)
                                    buzz(HapticFeedbackConstants.CLOCK_TICK)
                                } else {
                                    clearSelection()
                                }
                            } else {
                                select(
                                    activePage,
                                    Lasso(local, settings.lassoTouching),
                                    outline = local.takeIf { settings.lassoMode != com.pronotes.app.data.LassoMode.RECT },
                                )
                            }
                        }
                        layer.invalidate()
                    }
                    Mode.MOVE -> commitMove()
                    Mode.LASER -> laserUp()
                    Mode.SHAPE_DRAG -> {
                        // Build from the final pen position, so a quick flick still makes a full-size shape.
                        val (sx, sy) = points.first()
                        val (ex, ey) = toLocal(px, py, activePage)
                        val shapes = (if (hypot(ex - sx, ey - sy) >= 6f) runCatching { ShapeBuilder.build(host.shapeKind(), sx, sy, ex, ey) }.getOrNull() else null)
                            ?: layer.shapePreview
                        layer.shapePreview = null
                        val moved = hypot(px - downX, py - downY) > effectiveTouchSlop * 0.6f
                        if (shapes != null && (!isTap || moved)) addShapes(shapes, fillable = host.shapeKind().closed)
                        layer.invalidate()
                    }
                    Mode.WRITE_BOX_DRAG -> {
                        layer.writeBoxPreview = null
                        val (wx, wy) = toLocal(px, py, activePage)
                        val (x0, y0) = points.first()
                        val page = pages[activePage]
                        val dragged = hypot(wx - x0, wy - y0) > 28f
                        val boxW = if (dragged) abs(wx - x0).coerceIn(240f, page.width - 40f) else 560f.coerceAtMost(page.width - 64f)
                        val boxH = if (dragged) abs(wy - y0).coerceIn(100f, page.height - 40f) else 160f
                        val boxX = (if (dragged) min(x0, wx) else (x0 - 24f)).coerceIn(24f, (page.width - boxW - 24f).coerceAtLeast(24f))
                        val boxY = (if (dragged) min(y0, wy) else (y0 - 24f)).coerceIn(24f, (page.height - boxH - 24f).coerceAtLeast(24f))
                        buzz(HapticFeedbackConstants.CLOCK_TICK)
                        host.onTextBoxPlace(activePage, boxX, boxY, boxW, boxH)
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
                    Mode.MIND_DRAG -> {
                        val p = pages.getOrNull(dragMindOrigPage)
                        if (dragMindMoved && p != null) {
                            updatePage(dragMindOrigPage, p.items)
                        } else if (p != null) {
                            val node = p.items.firstOrNull { it is MindNodeItem && it.id == dragMindId } as MindNodeItem?
                            val (wx, wy) = toLocal(px, py, dragMindOrigPage)
                            host.onMindMapTap(dragMindOrigPage, wx, wy, node)
                        }
                        dragMindId = -1L
                        dragMindMoved = false
                        dragMindOrigPos = emptyMap()
                    }
                    Mode.TAP -> {
                        val (wx, wy) = toLocal(px, py, activePage)
                        val items = pages[activePage].items
                        when (strokeTool) {
                            Tool.TEXT -> {
                                val hit = items.lastOrNull { it is TextItem && it.bounds.contains(wx, wy) } as TextItem?
                                host.onTextTap(activePage, wx, wy, hit)
                            }
                            Tool.WEBLINK -> {
                                val hit = items.lastOrNull { it is WebLinkItem && it.bounds.contains(wx, wy) } as WebLinkItem?
                                if (hit != null && wx >= hit.bounds.right - 56f && wy <= hit.bounds.top + 56f) {
                                    host.onOpenUrl(hit.url)
                                } else {
                                    host.onWebLinkTap(activePage, wx, wy, hit)
                                }
                            }
                            Tool.MINDMAP -> {
                                val hidden = renderer?.hiddenMindNodeIds(items) ?: emptySet()
                                val hit = items.lastOrNull { it is MindNodeItem && it.id !in hidden && it.bounds.contains(wx, wy) } as MindNodeItem?
                                host.onMindMapTap(activePage, wx, wy, hit)
                            }
                            else -> {
                                val hit = items.lastOrNull { it is StickyItem && it.bounds.contains(wx, wy) } as StickyItem?
                                val lineHit = hit?.let { findStickyChecklistLine(it, wx, wy) }
                                if (hit != null && lineHit != null) host.onStickyChecklistToggle(activePage, hit, lineHit)
                                else host.onStickyTap(activePage, wx, wy, hit)
                            }
                        }
                    }
                    else -> Unit
                }
                clearTransientExitState(cancelWetEvent = null, clearWet = false)
                assertNoSurvivingWetStroke("pen-up", upStrokeId)
            }
            MotionEvent.ACTION_CANCEL -> { cancelCurrent(e) }
        }
    }

    internal val feel = PaperFeel(context, this)
    internal fun syncFeel() { feel.sound = settings.paperSound; feel.haptics = settings.paperHaptics; feel.volume = settings.paperVolume }
    private var feelX = 0f
    private var feelY = 0f
    private var feelT = 0L

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(holdRunnable)
        removeCallbacks(circleHoldRunnable)
        removeCallbacks(autoConvertRunnable)
        removeCallbacks(linkLongRunnable)
        removeCallbacks(textPressRunnable)
        removeCallbacks(hoverTimeout)
        removeCallbacks(wetAssertionRunnable)
        pendingClick?.let { removeCallbacks(it) }; pendingClick = null
        zoomAnim?.cancel(); zoomAnim = null
        layer.stopAnts()
        velocity?.recycle(); velocity = null
        armedDownEvent?.recycle(); armedDownEvent = null
        lastTouchEvent?.recycle(); lastTouchEvent = null
        feel.release()
    }

    private var stabX = Float.NaN
    private var stabY = Float.NaN

    /**
     * Remaps stylus/touch pressure according to user's pressureEnabled & pressureSensitivity settings,
     * and applies optional exponential moving average smoothing when penStabilization > 0.
     * Guarantees pressure sensitivity slider genuinely controls thin-to-thick response in real time.
     */
    private fun remapInkEvent(e: MotionEvent, pid: Int): MotionEvent {
        val td = settings.toolDefaults
        val isPressureTool = strokeTool == Tool.PEN || strokeTool == Tool.CRAYON || strokeTool == Tool.AUTO_TEXT
        if (!isPressureTool) return e
        val idx = e.findPointerIndex(pid)
        if (idx < 0) return e
        val count = e.pointerCount
        val props = Array(count) { i -> MotionEvent.PointerProperties().also { e.getPointerProperties(i, it) } }
        val coords = Array(count) { i -> MotionEvent.PointerCoords().also { e.getPointerCoords(i, it) } }
        val target = coords[idx]

        // Apply real pressure curve remapping
        val isStylus = isPen(props[idx].toolType)
        if (!td.pressureEnabled) {
            target.pressure = 0.5f
        } else {
            val sens = td.pressureSensitivity.coerceIn(0.3f, 2.0f)
            val rawP = target.pressure.coerceIn(0.02f, 1.0f)
            if (isStylus) {
                // Gamma curve: higher sensitivity (>1.0) produces thicker strokes with lighter pressure;
                // lower sensitivity (<1.0) requires firmer press for maximum width.
                val gamma = (1.0 / sens.toDouble()).coerceIn(0.42, 3.2)
                target.pressure = Math.pow(rawP.toDouble(), gamma).toFloat().coerceIn(0.05f, 1.0f)
            } else {
                // Non-pressure finger/mouse: map sensitivity into a clean visible pressure level.
                target.pressure = (0.35f + 0.45f * (sens / 2f)).coerceIn(0.18f, 1.0f)
            }
        }

        // Apply optional real-time pen stabilization smoothing
        val stab = td.penStabilization.coerceIn(0f, 1f)
        if (stab > 0.04f && e.actionMasked == MotionEvent.ACTION_MOVE) {
            if (stabX.isNaN() || stabY.isNaN()) {
                stabX = target.x; stabY = target.y
            } else {
                val alpha = (1f - stab * 0.65f).coerceIn(0.25f, 1f)
                stabX += (target.x - stabX) * alpha
                stabY += (target.y - stabY) * alpha
                target.x = stabX
                target.y = stabY
            }
        } else if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            stabX = target.x
            stabY = target.y
        }

        return MotionEvent.obtain(
            e.downTime,
            e.eventTime,
            e.action,
            count,
            props,
            coords,
            e.metaState,
            e.buttonState,
            e.xPrecision,
            e.yPrecision,
            e.deviceId,
            e.edgeFlags,
            e.source,
            e.flags,
        )
    }

    private fun startInk(e: MotionEvent, pid: Int, t: Tool) {
        // A stroke that was never finished or cancelled would stay on screen forever as ghost ink.
        cancelActiveWetStroke(e)
        removeCallbacks(wetAssertionRunnable)
        mode = Mode.INK
        stabX = Float.NaN; stabY = Float.NaN
        syncFeel()
        val ptrIdx = e.findPointerIndex(pid).coerceAtLeast(0)
        feelX = e.getX(ptrIdx); feelY = e.getY(ptrIdx); feelT = e.eventTime
        feel.start(if (t == Tool.HIGHLIGHTER) PaperFeel.Texture.MARKER else PaperFeel.Texture.PENCIL)
        val toWorld = Matrix(inverse).apply { postTranslate(-ox(activePage), -oy(activePage)) }
        val remapped = remapInkEvent(e, pid)
        val id = try {
            wetInk.startStroke(remapped, pid, host.brush(t), toWorld, Matrix())
        } finally {
            if (remapped !== e) remapped.recycle()
        }
        strokeId = id
        strokePages[id] = activePage
        strokeTools[id] = t
        if (shouldTrackHold()) postDelayed(holdRunnable, gestureConfig.holdDelayMs)
    }

    private fun shouldTrackHold(): Boolean =
        strokeTool == Tool.SHAPE || settings.holdToShape || settings.circleToLasso

    private fun finishInk(e: MotionEvent, isTap: Boolean) {
        val id = strokeId ?: return
        if (holdFired) {
            cancelActiveWetStroke(e)
            return
        }
        val p = pages[activePage]
        val (tx, ty) = points.first()
        if (isTap && p.items.any { it is TapeItem && it.bounds.contains(tx, ty) }) {
            cancelActiveWetStroke(e)
            toggleTapeAt(activePage, tx, ty)
            return
        }
        if (isTap && strokeTool == Tool.PEN && settings.toolDefaults.tapSelect) {
            topSelectableItemAt(activePage, tx, ty)?.let { hit ->
                cancelActiveWetStroke(e)
                selectItems(activePage, setOf(hit))
                return
            }
        }
        // Scribble-to-erase: uses the same surgical hit-testing & eraser policy as live scribble.
        if (!isTap && (strokeTool == Tool.PEN || strokeTool == Tool.CRAYON) && settings.scribbleToErase &&
            isDeliberateScribble(points, scale / density, e.eventTime - e.downTime, gestureConfig)) {
            val hit = scribbleHitIds(activePage, points)
            if (hit.isNotEmpty()) {
                cancelActiveWetStroke(e)
                val gone = p.items.filter { it.id in hit }.map { RectF(it.bounds).also { r -> pageMatrix(activePage, tmpMatrix).mapRect(r) } }
                host.beginEdit(activePage)
                updatePage(activePage, p.items.filterNot { it.id in hit })
                layer.puff(gone)
                syncFeel(); feel.cue(PaperFeel.Cue.TAP)
                host.onScribbleErased()
                return
            }
        }
        if (strokeTool == Tool.SHAPE) {
            val shape = ShapeRecognizer.recognize(points, forceNearest = true)
            if (shape != null) {
                cancelActiveWetStroke(e)
                addShapes(listOf(shape), fillable = isClosedLoop(shape, gestureConfig))
                buzz(); syncFeel(); feel.cue(PaperFeel.Cue.SNAP)
                return
            }
            // Nothing matched: silently keep the freehand stroke editable.
        }
        // Highlighter: always straight, or straightened when it was nearly straight anyway.
        if (strokeTool == Tool.HIGHLIGHTER && points.size > 1) {
            val hMode = settings.highlighterMode
            val len = pathLength(points)
            val straighten = hMode == com.pronotes.app.data.HighlighterMode.STRAIGHT ||
                (hMode == com.pronotes.app.data.HighlighterMode.AUTO && isNearlyStraight(points, max(10f, len * 0.07f)))
            if (straighten) {
                cancelActiveWetStroke(e)
                addStraightHighlight(points.first(), points.last())
                return
            }
        }
        // Offer unified circle-to-select when a closed pen loop encloses selectable content.
        if (settings.circleToLasso && strokeTool == Tool.PEN && tool == Tool.PEN && !sideButtonHeld(e) &&
            circleDetector.isValidLoopCandidate(points, scale, density, gestureConfig)
        ) {
            val ids = loopSelects(activePage, points)
            if (ids.isNotEmpty()) {
                val cand = circleDetector.offerCandidate(id, activePage, ids, points, SystemClock.uptimeMillis())
                if (cand != null) {
                    val loopBox = RectF(points.minOf { it.first }, points.minOf { it.second }, points.maxOf { it.first }, points.maxOf { it.second })
                    host.onCircleCandidate(pageToScreen(activePage, loopBox))
                    layer.invalidate()
                }
            }
        }
        strokeId = null
        val remapped = remapInkEvent(e, activePointer)
        try {
            wetInk.finishStroke(remapped, activePointer, id)
        } finally {
            if (remapped !== e) remapped.recycle()
        }
    }

    /** What a hand-drawn loop would select: anything mostly or partly inside. */
    private fun loopSelects(page: Int, loop: List<Pt>): Set<Long> {
        val p = pages.getOrNull(page) ?: return emptySet()
        val lasso = Lasso(loop, settings.lassoTouching, minInside = 0.3f)
        return p.items.filter { lassoAllows(it) && lasso.selects(it) }.map { it.id }.toSet()
    }

    internal val circleDetector = CircleDetector()
    private var armedDownEvent: MotionEvent? = null
    private val circleHoldRunnable = Runnable { fireCircleHold() }

    /** Activates circle selection cleanly for both live-hold and post-lift hold. */
    private fun activateCircleSelection(
        page: Int,
        loop: List<Pt>,
        targetIds: Set<Long>,
        grabLocalX: Float,
        grabLocalY: Float,
        itemIdToRemove: Long = -1L,
    ) {
        val p = pages.getOrNull(page) ?: return
        if (itemIdToRemove != -1L) {
            host.beginEdit(page)
            updatePage(page, p.items.filterNot { it.id == itemIdToRemove })
        }
        val latest = pages.getOrNull(page) ?: return
        val ids = targetIds.filter { id -> latest.items.any { it.id == id } }.toSet()
        if (ids.isEmpty()) return
        buzz()
        val screen = loop.map { (x, y) ->
            val a = floatArrayOf(x, y)
            pageMatrix(page, tmpMatrix).mapPoints(a)
            a[0] to a[1]
        }
        selectItems(page, ids, outline = loop)
        if (!circleSelect) { circleSelect = true; host.onCircleSelect(true) }
        layer.playLassoMorph(screen) {}
        activePage = page
        grab = Grab.MOVE
        mode = Mode.MOVE
        grabStartX = grabLocalX; grabStartY = grabLocalY
        liveScale = 1f; liveScaleX = 1f; liveScaleY = 1f; liveDeg = 0f
        liveMatrix = Matrix()
    }

    /** The end of the loop was held: the loop ink becomes the lasso and the pen can drag at once. */
    private fun fireCircleHold() {
        val (pc, downScreen) = circleDetector.triggerArmedHold() ?: return
        armedDownEvent?.recycle(); armedDownEvent = null
        host.onCircleCandidate(null)
        if (pc.itemId == -1L && pc.strokeKey is InProgressStrokeId) {
            consumedCircleStrokes += pc.strokeKey
        }
        val (lx, ly) = toLocal(downScreen.first, downScreen.second, pc.page)
        activateCircleSelection(
            page = pc.page,
            loop = pc.loop,
            targetIds = pc.targetIds,
            grabLocalX = lx,
            grabLocalY = ly,
            itemIdToRemove = pc.itemId,
        )
    }

    /** The loop was held still before lifting: its ink becomes the lasso and the pen can drag it straight away. */
    private fun fireLiveCircle(ids: Set<Long>) {
        holdFired = true
        val page = activePage
        val loop = ArrayList(points)
        cancelActiveWetStroke()
        clearCircleCandidate()
        val (lx, ly) = points.last()
        activateCircleSelection(
            page = page,
            loop = loop,
            targetIds = ids,
            grabLocalX = lx,
            grabLocalY = ly,
        )
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
        if (last == null || hypot(cur.first - last.first, cur.second - last.second) * scale > holdSlopPx()) {
            lastMovePt = cur
            removeCallbacks(holdRunnable)
            if (shouldTrackHold()) postDelayed(holdRunnable, gestureConfig.holdDelayMs)
        }
    }

    /** Pen held still at the end of a stroke: circle-to-lasso or snap to a shape. */
    private fun onHold() {
        if (mode != Mode.INK || holdFired) return
        if (strokeId == null) return
        pages.getOrNull(activePage) ?: return
        if (points.size < 6 || pathLength(points) < 30f) return
        // A scribble that ends with a pause is still a scribble (erase), never a shape.
        if (strokeTool == Tool.PEN && settings.scribbleToErase &&
            isDeliberateScribble(points, scale / density, SystemClock.uptimeMillis() - downTime, gestureConfig) &&
            scribbleHitIds(activePage, points).isNotEmpty()) return

        // Circle to lasso: uses the exact same closed-loop rule as post-lift circle detection.
        if (settings.circleToLasso && strokeTool == Tool.PEN && tool == Tool.PEN &&
            circleDetector.isValidLoopCandidate(points, scale, density, gestureConfig)
        ) {
            val ids = loopSelects(activePage, points)
            if (ids.isNotEmpty()) { fireLiveCircle(ids); return }
        }

        // Highlighter holds snap to shapes or straight highlight lines.
        if (strokeTool == Tool.HIGHLIGHTER) {
            holdFired = true
            cancelActiveWetStroke()
            val shape = if (isClosedLoop(points, gestureConfig)) ShapeRecognizer.recognize(points) else null
            if (shape != null) {
                val p2 = pages[activePage]
                host.beginEdit(activePage)
                updatePage(activePage, p2.items + InkItem(ItemIds.next(), InkCodec.strokeFromPoints(shape, host.brush(Tool.HIGHLIGHTER))))
            } else addStraightHighlight(points.first(), points.last())
            buzz()
            return
        }
        if (strokeTool == Tool.SHAPE || settings.holdToShape) {
            val shape = ShapeRecognizer.recognize(points, forceNearest = (strokeTool == Tool.SHAPE)) ?: return
            holdFired = true
            cancelActiveWetStroke()
            addShapes(listOf(shape), fillable = strokeTool == Tool.SHAPE && isClosedLoop(shape, gestureConfig), brushTool = strokeTool)
            buzz(); syncFeel(); feel.cue(PaperFeel.Cue.SNAP)
        }
    }

    private fun cancelActiveWetStroke(event: MotionEvent? = lastTouchEvent) {
        val id = strokeId ?: return
        strokeId = null
        strokePages.remove(id)
        strokeTools.remove(id)
        val ev = if (event != null) {
            MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
        } else {
            val now = SystemClock.uptimeMillis()
            MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, downX, downY, 0)
        }
        try {
            wetInk.cancelStroke(id, ev)
        } finally {
            ev.recycle()
        }
    }

    /** Lasso filter from settings: which kinds of content can be selected. */
    internal fun lassoAllows(item: PageItem): Boolean {
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

    /** The user tapped the "Select" chip: turn the drawn loop into a selection. */
    fun acceptCircle() {
        val pc = circleDetector.activeCandidate(SystemClock.uptimeMillis(), gestureConfig) ?: return
        pc.pendingRemoval = true
        circleDetector.reset()
        host.onCircleCandidate(null)
        if (pc.itemId == -1L && pc.strokeKey is InProgressStrokeId) {
            consumedCircleStrokes += pc.strokeKey
        }
        val p = pages.getOrNull(pc.page) ?: return
        if (pc.itemId != -1L) updatePage(pc.page, p.items.filterNot { it.id == pc.itemId })
        val latest = pages.getOrNull(pc.page) ?: return
        selectItems(pc.page, pc.targetIds.filter { id -> latest.items.any { it.id == id } }.toSet())
        buzz()
    }

    fun clearCircleCandidate() {
        if (circleDetector.state == CircleDetector.State.Idle) return
        circleDetector.reset()
        armedDownEvent?.recycle(); armedDownEvent = null
        removeCallbacks(circleHoldRunnable)
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

    private var pendingCheckStrokeId: InProgressStrokeId? = null
    private val wetAssertionRunnable = Runnable {
        if (!BuildConfig.DEBUG) return@Runnable
        val staleId = pendingCheckStrokeId
        pendingCheckStrokeId = null
        val leaked = !touchActive && mode == Mode.NONE &&
            (strokeId != null || (staleId != null && staleId in strokePages))
        if (leaked) {
            android.util.Log.w("NoteCanvasView", "In-progress stroke survived pen-up")
        }
    }

    private fun assertNoSurvivingWetStroke(reason: String, finishedId: InProgressStrokeId? = null) {
        if (!BuildConfig.DEBUG) return
        if (strokeId != null || (reason == "cancel" && strokePages.isNotEmpty())) {
            android.util.Log.w("NoteCanvasView", "In-progress stroke survived $reason")
            return
        }
        if (finishedId != null && finishedId in strokePages) {
            pendingCheckStrokeId = finishedId
            removeCallbacks(wetAssertionRunnable)
            postDelayed(wetAssertionRunnable, 350L)
        }
    }

    private fun clearTransientExitState(cancelWetEvent: MotionEvent?, clearWet: Boolean) {
        removeCallbacks(holdRunnable)
        removeCallbacks(circleHoldRunnable)
        removeCallbacks(linkLongRunnable)
        removeCallbacks(textPressRunnable)
        feel.stop()
        if (clearWet) {
            cancelActiveWetStroke(cancelWetEvent)
            if (circleDetector.isHoldArmed()) {
                circleDetector.reset()
                host.onCircleCandidate(null)
            }
        }
        armedDownEvent?.recycle(); armedDownEvent = null
        if (mode == Mode.LASSO) layer.stopAnts()
        layer.lasso = null
        layer.shapePreview = null
        layer.tapePreview = null
        layer.tapeFree = null
        layer.writeBoxPreview = null
        layer.eraserAt = null
        clearHover()
        liveMatrix = null
        lastErase = null
        holdFired = false
        capturingDrag = false
        dragMindId = -1L
        dragMindMoved = false
        dragMindOrigPos = emptyMap()
        mode = Mode.NONE
        activePointer = -1
        layer.invalidate()
    }

    private fun cancelCurrent(e: MotionEvent?) {
        clearTransientExitState(cancelWetEvent = e ?: lastTouchEvent, clearWet = true)
        assertNoSurvivingWetStroke("cancel", null)
    }

    // ------------------------------------------------------------- laser

    /** Laser is strictly a pointer and never converts to a lasso selection. */
    private fun laserUp() {
        layer.laserEnd()
    }

    // ----------------------------------------------------------- selection & gesture state (implementations in NoteCanvasEraser/Selection/Gestures/DocLayer.kt)

    /** Page-space loop drawn around a lasso selection (null = plain box). */
    internal var selectionOutline: List<Pt>? = null
    private var circleSelect = false
    internal val tmpMatrix = Matrix()

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

    internal var textSelecting = false
    internal var textPage = 0
    internal val textPressRunnable = Runnable {
        if (fingerMoved || maxFingers != 1 || mode != Mode.PAN) return@Runnable
        val (dx, dy) = toDoc(fingerStartX, fingerStartY)
        val pi = pageAt(dx, dy)
        val (lx, ly) = toLocal(fingerStartX, fingerStartY, pi)
        if (host.onTextPress(pi, lx, ly)) {
            textSelecting = true; textPage = pi
            scroller.forceFinished(true)
            buzz()
            layer.invalidate()
        } else if (!readOnly && pi in pages.indices) {
            val hitAnyItem = pages[pi].items.any { it.bounds.contains(lx, ly) }
            if (!hitAnyItem) {
                mode = Mode.NONE
                velocity?.recycle(); velocity = null
                buzz(HapticFeedbackConstants.LONG_PRESS)
                host.onEmptyPageLongPress(pi, lx, ly, fingerStartX, fingerStartY)
            }
        }
    }

    /** Double-tap with a finger: zoom in to 2x around the tap, or back to fit. */
    internal var zoomAnim: ValueAnimator? = null

    fun resetWhiteboardView() {
        if (pages.isEmpty() || width == 0) return
        zoomAnim?.cancel()
        scroller.forceFinished(true)
        val page = currentIndex
        val p = pages[page]
        val fromS = scale; val fromX = transX; val fromY = transY
        val toS = fitScale(page)
        val toX = (width - p.width * toS) / 2f - ox(page) * toS
        val toY = (height - p.height * toS) / 2f - oy(page) * toS
        zoomAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 280; interpolator = DecelerateInterpolator()
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

    // ------------------------------------------------ pen button click / double click (hovering)
    internal var lastPenTouchAt = 0L      // last time the pen touched the screen
    internal var buttonDown = false
    internal var buttonDownAt = 0L
    internal var lastClickAt = 0L
    internal var pendingClick: Runnable? = null
    internal val hoverTimeout = Runnable { clearHover() }

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
                    // Hover preview: custom imported hover cursor or a dot in the pen's colour and size.
                    val hasCustomHover = settings.customHoverIconPath.isNotBlank()
                    layer.hoverDot = if (!erasing && settings.hoverCursor && stylusPointer == -1 && mode == Mode.NONE &&
                        (hasCustomHover || tool == Tool.PEN || tool == Tool.CRAYON || tool == Tool.AUTO_TEXT || tool == Tool.HIGHLIGHTER || tool == Tool.SHAPE))
                        (host.hoverInk() ?: (0xFF3D6CF4.toInt() to 6f)).let { (c, s) -> Triple(event.x, event.y, c to s * scale) } else null
                    // Update writing guide line position when hovering in Auto-Text or Align Handwriting mode
                    if (!erasing && settings.toolDefaults.alignGuideLine && (tool == Tool.AUTO_TEXT || (tool == Tool.PEN && settings.toolDefaults.alignHandwriting))) {
                        val (ddx, ddy) = toDoc(event.x, event.y)
                        val pi = pageAt(ddx, ddy)
                        if (pi in pages.indices) {
                            val (lx, ly) = toLocal(event.x, event.y, pi)
                            layer.guideLinePage = pi
                            layer.guideLineX = lx
                            layer.guideLineY = ly + 22f
                        }
                    }
                    layer.invalidate()
                    // Some pens never send HOVER_EXIT: drop the dot when hover reports stop.
                    removeCallbacks(hoverTimeout); postDelayed(hoverTimeout, 350)
                }
                MotionEvent.ACTION_HOVER_EXIT -> clearHover()
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }

    fun refresh() = layer.invalidate()
}
