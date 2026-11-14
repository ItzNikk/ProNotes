package com.pronotes.app.editor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.ink.brush.Brush
import androidx.ink.strokes.Stroke
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pronotes.app.data.PageSize
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.Repository
import com.pronotes.app.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.util.UUID

data class ToolStyle(val colors: List<Int>, val sizes: List<Float>, val color: Int, val size: Float)

val PenPalette = listOf(
    0xFF1E2321, 0xFF2E4A7A, 0xFF2F6B4F, 0xFF7A2E42, 0xFFB98A2E, 0xFF24483F,
    0xFF5A5F58, 0xFF3D6CF4, 0xFF0A84FF, 0xFF34C759, 0xFF00897B, 0xFFFF9500,
    0xFFEB5757, 0xFF9B51E0, 0xFF6D4C41, 0xFFFFFFFF, 0xFF000000,
).map { it.toInt() }

val HighlighterPalette = listOf(0xFFF3D77A, 0xFFA5D3C0, 0xFF9AD0EC, 0xFFFFA8A8, 0xFFE599F7).map { it.toInt() }
val TapeColors = listOf(0xFF24483F, 0xFF2E4A7A, 0xFF7A2E42, 0xFFB98A2E, 0xFF8E9189).map { it.toInt() }
val StickyColors = listOf(0xFFF4EEDF, 0xFFFFF3A3, 0xFFD8F5C8, 0xFFCDEBFF, 0xFFFFD6E0).map { it.toInt() }
val WebLinkColors = listOf(0xFF24483F, 0xFF2E4A7A, 0xFF2F6B4F, 0xFF7A2E42, 0xFFB98A2E, 0xFF3D6CF4).map { it.toInt() }
val MindPalette = listOf(0xFF24483F, 0xFF2E4A7A, 0xFF2F6B4F, 0xFF7A2E42, 0xFFB98A2E, 0xFF35655A, 0xFF5A5F58).map { it.toInt() }

class EditorViewModel(app: Application, val noteId: String) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    private val store = SettingsStore.get(app)
    val settingsFlow = store.settings

    val pages = mutableStateListOf<Page>()
    var pageIndex by mutableStateOf(0)
        private set
    var loaded by mutableStateOf(false)
        private set
    var title by mutableStateOf(repo.meta(noteId)?.title ?: "Notebook")
        private set
    val isPdf: Boolean get() = repo.pdfFile(noteId).exists()
    val aiEngineLabel: String get() = LocalAiManager.activeEngineLabel(getApplication())

    // ---------------------------------------------------------------- tools
    /** Last session's choices: tool, pen type, colours, sizes. */
    private val saved = store.pref("tools").split('|')
    private inline fun <reified E : Enum<E>> savedEnum(i: Int, def: E): E =
        saved.getOrNull(i)?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: def
    private var _tool by mutableStateOf(savedEnum(0, Tool.PEN).takeIf { it != Tool.IMAGE && it != Tool.STICKER && it != Tool.STICKY } ?: Tool.PEN)
    private var previousTool = Tool.PEN
    /** Current tool. Remembers the last non-eraser tool for "switch back after erasing". */
    var tool: Tool
        get() = _tool
        set(v) { toolBeforeCircle = null; if (_tool != Tool.ERASER && _tool != v) previousTool = _tool; _tool = v }

    /** Tool to go back to once a circle-and-hold selection ends (the toolbar shows lasso meanwhile). */
    private var toolBeforeCircle: Tool? = null

    fun circleSelect(active: Boolean) {
        if (active) {
            if (toolBeforeCircle == null && _tool != Tool.LASSO) { val keep = _tool; _tool = Tool.LASSO; toolBeforeCircle = keep }
        } else {
            toolBeforeCircle?.let { _tool = it }
            toolBeforeCircle = null
        }
    }
    var penType by mutableStateOf(savedEnum(1, PenType.FOUNTAIN))
    var penStyle by mutableStateOf(style("pen", PenPalette.take(5), listOf(2f, 4f, 7f), 3.6f, 2))
    var highlighterStyle by mutableStateOf(style("highlighter", HighlighterPalette, listOf(16f, 28f, 42f), 22f, 3))
    var shapeStyle by mutableStateOf(style("shape", PenPalette.take(5), listOf(2f, 4f, 7f), 3.6f, 4))
    var shapeKind by mutableStateOf(savedEnum(5, ShapeKind.AUTO))
    var shapeDashed by mutableStateOf(false)
    /** Bumped when the selected image or sticker tool is tapped again. */
    var reopenTool by mutableStateOf(0)
    var shapeFillOn by mutableStateOf(false)
    var shapeFillColor by mutableStateOf(0xFFCDE2FB.toInt())
    var textStyle by mutableStateOf(TextStyleSpec())
    var textColor by mutableStateOf<Int?>(null)

    var circleChip by mutableStateOf<RectF?>(null)
        private set
    private var chipJob: Job? = null
    var zoomPercent by mutableStateOf<Int?>(null)
        private set
    private var clipboard: List<PageItem> = emptyList()
    var hasClipboard by mutableStateOf(false)
        private set
    var eraserSize by mutableStateOf(saved.getOrNull(6)?.toIntOrNull()?.coerceIn(0, 2) ?: 1)
    var textSize by mutableStateOf(40f)
    var tapeColor by mutableStateOf(saved.getOrNull(7)?.toIntOrNull() ?: TapeColors[0])
    var tapeFreehand by mutableStateOf(false)
    var stickyColor by mutableStateOf(saved.getOrNull(8)?.toIntOrNull() ?: StickyColors[0])
    var webLinkColor by mutableStateOf(WebLinkColors[0])

    // ------------------------------------------------------------ selection
    var selection by mutableStateOf<Set<Long>>(emptySet())
        private set
    var selectionPage by mutableStateOf(-1)
        private set
    var selectionBounds by mutableStateOf<RectF?>(null)
        private set

    // --------------------------------------------------------------- dialogs
    data class TextEdit(val page: Int, val x: Float, val y: Float, val existing: TextItem?)
    data class StickyEdit(val page: Int, val item: StickyItem, val isNew: Boolean)
    data class WebLinkEdit(val page: Int, val item: WebLinkItem, val isNew: Boolean)
    data class MindNodeEdit(val page: Int, val item: MindNodeItem, val isNew: Boolean)
    enum class MindLayout(val label: String) { RADIAL("Radial"), TREE("Tree"), ORG_CHART("Org") }
    var textEdit by mutableStateOf<TextEdit?>(null)
    var stickyEdit by mutableStateOf<StickyEdit?>(null)
    var webLinkEdit by mutableStateOf<WebLinkEdit?>(null)
    var mindNodeEdit by mutableStateOf<MindNodeEdit?>(null)
    var activeMindNodeId by mutableStateOf<Long?>(null)
    var mindColor by mutableStateOf(MindPalette[0])
    var mindShape by mutableStateOf("pill")
    var moveMindSubtree by mutableStateOf(false)
    var fitMindMapOnCanvas: ((Int, RectF) -> Unit)? = null

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    // ---------------------------------------------------------------- search
    var searchQuery by mutableStateOf("")
    var searchHits by mutableStateOf<List<SearchHit>>(emptyList())
        private set
    var searchAiAnswer by mutableStateOf<SearchAiResult?>(null)
        private set
    var activeHit by mutableStateOf(-1)
        private set
    var searching by mutableStateOf(false)
        private set
    private var pdfIndex: PdfTextIndex? = null
    private var searchJob: Job? = null

    private val undoStack = ArrayDeque<Pair<Int, List<PageItem>>>()
    private val redoStack = ArrayDeque<Pair<Int, List<PageItem>>>()
    private var saveJob: Job? = null
    private var pdf: PdfPages? = null
    private val pendingPdf = HashSet<Int>()
    /** When each PDF page was last asked for by the canvas, and the width it wants now. */
    private val pdfSeen = java.util.concurrent.ConcurrentHashMap<Int, Long>()
    private val pdfWant = java.util.concurrent.ConcurrentHashMap<Int, Int>()

    /** Set by the screen so model changes can be pushed into the canvas. */
    var onPagesChanged: (() -> Unit)? = null
    var onScrollTo: ((Int) -> Unit)? = null
    var onRedraw: (() -> Unit)? = null

    /** Read-only: the notebook can be read and scrolled, not changed. Remembered per notebook. */
    var readOnly by mutableStateOf(store.pref("ro_$noteId") == "1")
        private set
    fun toggleReadOnly() { readOnly = !readOnly; store.setPref("ro_$noteId", if (readOnly) "1" else "0") }

    /** Last time the user wrote, erased or moved around (for the study timer's idle pause). */
    var lastActivity by mutableStateOf(System.currentTimeMillis())
    fun touched() { lastActivity = System.currentTimeMillis() }

    /** Where the study timer sits, as fractions of the canvas (shared by all notebooks). */
    fun timerPos(): Pair<Float, Float> {
        val p = store.pref("timerPos", "0.5,0.93").split(',')
        return (p.getOrNull(0)?.toFloatOrNull() ?: 0.5f) to (p.getOrNull(1)?.toFloatOrNull() ?: 0.93f)
    }
    fun setTimerPos(x: Float, y: Float) = store.setPref("timerPos", "$x,$y")

    /** Total study time in this notebook, in milliseconds (sessions add to it). */
    fun studyTotal(): Long = store.pref("study_$noteId", "0").toLongOrNull() ?: 0L
    fun addStudy(ms: Long) { if (ms > 0) store.setPref("study_$noteId", (studyTotal() + ms).toString()) }

    /** The page this notebook was on when it was last closed. */
    var startPage = 0
        private set

    init {
        // Remember tool choices as they change.
        viewModelScope.launch {
            androidx.compose.runtime.snapshotFlow {
                listOf(
                    _tool.name, penType.name, "${penStyle.color},${penStyle.size}", "${highlighterStyle.color},${highlighterStyle.size}",
                    "${shapeStyle.color},${shapeStyle.size}", shapeKind.name, eraserSize.toString(), tapeColor.toString(), stickyColor.toString(),
                ).joinToString("|")
            }.collect { store.setPref("tools", it) }
        }
        val initTool = store.pref("startTool_$noteId").takeIf { it.isNotBlank() }?.let { n ->
            store.setPref("startTool_$noteId", "")
            enumValues<Tool>().firstOrNull { it.name == n }
        }
        if (initTool != null) _tool = initTool
        viewModelScope.launch {
            LocalAiManager.stage.collect { st ->
                if (st.isNotBlank()) aiStage = st
            }
        }
        viewModelScope.launch {
            val loadedPages = withContext(Dispatchers.IO) { repo.loadPages(noteId) }
            startPage = store.pref("page_$noteId", "0").toIntOrNull()?.coerceIn(0, (loadedPages.size - 1).coerceAtLeast(0)) ?: 0
            pages.clear(); pages.addAll(loadedPages)
            if (repo.pdfFile(noteId).exists()) pdf = PdfPages(repo.pdfFile(noteId))
            loaded = true
            onPagesChanged?.invoke()

            // Secondary metadata & caches load in the background without delaying the canvas.
            launch(Dispatchers.IO) { LocalAiManager.refresh(app) }
            launch(Dispatchers.IO) { loadStudy() }
            launch(Dispatchers.IO) { repo.markOpened(noteId) }
            prefetchPdf(startPage)
        }
    }

    private fun style(key: String, colors: List<Int>, sizes: List<Float>, size: Float, slot: Int): ToolStyle {
        val c = store.swatches(key, colors)
        val sel = saved.getOrNull(slot)?.split(',')
        val color = sel?.getOrNull(0)?.toIntOrNull() ?: c.first()
        val sz = sel?.getOrNull(1)?.toFloatOrNull() ?: size
        return ToolStyle(c, sizes, color, sz)
    }

    val currentPage: Page? get() = pages.getOrNull(pageIndex)

    fun renderer() = PageRenderer(repo.noteDir(noteId))

    fun onCurrentPage(i: Int) {
        pageIndex = i
        touched()
        if (loaded) store.setPref("page_$noteId", i.toString())
        prefetchPdf(i)
    }

    private var prefetchJob: Job? = null

    /** Draws the pages around the current one ahead of time so they never flash white while you swipe. */
    private fun prefetchPdf(center: Int) {
        val src = pdf ?: return
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(90)
            for (d in intArrayOf(0, 1, -1, 2, 3, -2, 4)) {
                val page = pages.getOrNull(center + d) ?: continue
                if (page.pdfPage < 0 || src.cached(page.pdfPage) != null) continue
                // Do not compete with a page that is on screen right now.
                while (pendingPdf.isNotEmpty()) kotlinx.coroutines.delay(40)
                val w = 800
                val h = (w * page.height / page.width).toInt()
                withContext(Dispatchers.IO) { src.render(page.pdfPage, w, h) }
                onRedraw?.invoke()
            }
        }
    }

    // ------------------------------------------------------------- brushes

    fun styleFor(t: Tool): ToolStyle? = when (t) {
        Tool.PEN -> penStyle
        Tool.HIGHLIGHTER -> highlighterStyle
        Tool.SHAPE -> shapeStyle
        else -> null
    }

    fun updateStyle(t: Tool, s: ToolStyle) {
        when (t) {
            Tool.PEN -> { penStyle = s; store.saveSwatches("pen", s.colors) }
            Tool.HIGHLIGHTER -> { highlighterStyle = s; store.saveSwatches("highlighter", s.colors) }
            Tool.SHAPE -> { shapeStyle = s; store.saveSwatches("shape", s.colors) }
            else -> Unit
        }
    }

    fun shapeFill(): Int? = if (shapeFillOn) shapeFillColor else null

    fun addSwatch(t: Tool, color: Int) {
        val s = styleFor(t) ?: return
        if (s.colors.size >= 8) return
        updateStyle(t, s.copy(colors = s.colors + color, color = color))
        pushRecent(color)
    }

    fun removeSwatch(t: Tool, index: Int) {
        val s = styleFor(t) ?: return
        if (s.colors.size <= 1) return
        val colors = s.colors.toMutableList().also { it.removeAt(index) }
        updateStyle(t, s.copy(colors = colors, color = if (s.color in colors) s.color else colors.first()))
    }

    fun onEraserLifted() { if (settingsFlow.value.eraserAutoSwitch) tool = previousTool }

    /** A tool settings card is open (the editor blurs behind it). */
    var cardOpen by mutableStateOf(false)

    /** Jumps back to the tool used before this one. */
    fun swapToPrevious() { val p = previousTool; tool = p }

    /** Colour and size (page units) of the hover preview dot. */
    fun hoverInk(): Pair<Int, Float>? = when (tool) {
        Tool.PEN -> penStyle.color to penStyle.size * penType.sizeFactor
        Tool.HIGHLIGHTER -> highlighterStyle.color to highlighterStyle.size
        Tool.SHAPE -> shapeStyle.color to shapeStyle.size
        else -> null
    }

    fun showCircleChip(r: RectF?) {
        circleChip = r
        chipJob?.cancel()
        if (r != null) chipJob = viewModelScope.launch { delay(3500); circleChip = null }
    }

    fun onZoom(p: Int?) { zoomPercent = p }

    fun copySelection() {
        val p = pages.getOrNull(selectionPage) ?: return
        clipboard = p.items.filter { it.id in selection }
        hasClipboard = clipboard.isNotEmpty()
    }

    fun cutSelection() { copySelection(); deleteSelection() }

    private fun selectionUnion(): RectF? {
        val p = pages.getOrNull(selectionPage) ?: return null
        val items = p.items.filter { it.id in selection }
        if (items.isEmpty()) return null
        return RectF(items.first().bounds).also { r -> items.drop(1).forEach { r.union(it.bounds) } }
    }

    /** Selected items keep their order among themselves and move above (or below) the rest. */
    fun arrange(toFront: Boolean) {
        val p = pages.getOrNull(selectionPage) ?: return
        val (sel, rest) = p.items.partition { it.id in selection }
        beginEdit(selectionPage)
        applyItems(selectionPage, if (toFront) rest + sel else sel + rest)
    }

    fun rotateSelection(deg: Float) = transformSelection { r -> android.graphics.Matrix().apply { setRotate(deg, r.centerX(), r.centerY()) } to (1f to deg) }

    fun flipSelection() = transformSelection { r -> android.graphics.Matrix().apply { setScale(-1f, 1f, r.centerX(), r.centerY()) } to (1f to 0f) }

    private fun transformSelection(build: (RectF) -> Pair<android.graphics.Matrix, Pair<Float, Float>>) {
        val p = pages.getOrNull(selectionPage) ?: return
        val r = selectionUnion() ?: return
        val (m, sd) = build(r)
        beginEdit(selectionPage)
        applyItems(selectionPage, p.items.map { if (it.id in selection) it.transformed(m, sd.first, sd.second) else it })
    }

    /** Renders the selection on a transparent bitmap, for sharing as a picture. */
    suspend fun selectionImage(): Bitmap? = withContext(Dispatchers.Default) {
        val p = pages.getOrNull(selectionPage) ?: return@withContext null
        val r = selectionUnion() ?: return@withContext null
        r.inset(-16f, -16f)
        val scale = (1600f / maxOf(r.width(), r.height())).coerceIn(0.5f, 2f)
        val bmp = Bitmap.createBitmap((r.width() * scale).toInt().coerceAtLeast(1), (r.height() * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val m = android.graphics.Matrix().apply { setTranslate(-r.left, -r.top); postScale(scale, scale) }
        val c = Canvas(bmp)
        c.concat(m)
        renderer().drawItems(c, p.items.filter { it.id in selection }, m)
        bmp
    }

    suspend fun pageImage(index: Int): Bitmap? = thumbnail(index, 1600)

    fun movePage(from: Int, to: Int) {
        if (from !in pages.indices || to !in pages.indices || from == to) return
        val p = pages.removeAt(from)
        pages.add(to, p)
        undoStack.clear(); redoStack.clear(); refreshUndo()
        if (pageIndex == from) pageIndex = to
        scheduleSave(); onPagesChanged?.invoke()
    }

    // ----------------------------------------------------------- pen presets
    val presets = store.presets

    fun savePreset(name: String = "") =
        store.addPreset(com.pronotes.app.data.PenPreset(penType.name, penStyle.color, penStyle.size, name.trim()))
    fun renamePreset(p: com.pronotes.app.data.PenPreset, newName: String) = store.renamePreset(p, newName)
    fun removePreset(p: com.pronotes.app.data.PenPreset) = store.removePreset(p)
    fun applyPreset(p: com.pronotes.app.data.PenPreset) {
        penType = runCatching { PenType.valueOf(p.type) }.getOrDefault(PenType.FOUNTAIN)
        val colors = if (p.color in penStyle.colors) penStyle.colors else (penStyle.colors + p.color).takeLast(8)
        penStyle = penStyle.copy(colors = colors, color = p.color, size = p.size)
        tool = Tool.PEN
    }

    fun setPenName(name: String) = store.update { it.copy(penName = name.trim().ifBlank { "Pro Stylus" }) }

    fun clearPageInk() {
        val p = currentPage ?: return
        if (p.items.none { it is InkItem }) return
        beginEdit(pageIndex)
        applyItems(pageIndex, p.items.filterNot { it is InkItem })
        say("Cleared ink on page ${pageIndex + 1}")
    }

    /** Places a sticker picture in the middle of the page in view and returns it for selection. */
    fun insertStickerBitmap(bmp: Bitmap, onPlaced: (Int, Long) -> Unit) {
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) {
                val n = "sticker_${UUID.randomUUID()}.png"
                java.io.File(repo.noteDir(noteId), n).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                n
            }
            val idx = pageIndex
            val p = pages.getOrNull(idx) ?: return@launch
            val w = 300f; val h = w * bmp.height / bmp.width
            val item = ImageItem(ItemIds.next(), name, (p.width - w) / 2f, p.height / 3f, w, h)
            beginEdit(idx)
            applyItems(idx, p.items + item)
            onPlaced(idx, item.id)
        }
    }

    /** Pastes onto the page in view, nudged so it does not sit exactly on the original. */
    fun paste(): Pair<Int, Set<Long>>? {
        if (clipboard.isEmpty()) return null
        val idx = pageIndex
        val p = pages.getOrNull(idx) ?: return null
        val copies = clipboard.map { it.translated(40f, 40f).withId(ItemIds.next()) }
        clipboard = copies
        beginEdit(idx)
        applyItems(idx, p.items + copies)
        return idx to copies.map { it.id }.toSet()
    }

    fun toggleBookmark() {
        val p = currentPage ?: return
        pages[pageIndex] = p.copy(bookmarked = !p.bookmarked)
        scheduleSave(); onPagesChanged?.invoke()
    }

    fun rename(newTitle: String) {
        if (newTitle.isBlank()) return
        title = newTitle
        viewModelScope.launch { repo.renameNotebook(noteId, newTitle) }
    }

    fun recentColors() = store.recentColors()
    fun pushRecent(c: Int) = store.pushRecentColor(c)

    fun brushFor(t: Tool): Brush = when (t) {
        Tool.HIGHLIGHTER -> {
            Brushes.highlightAlpha = (store.settings.value.highlighterOpacity.coerceIn(0.3f, 0.9f) * 255).toInt()
            Brushes.create(Brushes.HIGHLIGHTER, highlighterStyle.color, highlighterStyle.size)
        }
        Tool.SHAPE -> Brushes.create(if (shapeDashed) Brushes.DASHED else Brushes.PEN, shapeStyle.color, shapeStyle.size)
        else -> Brushes.create(penType.family, penStyle.color, penStyle.size * penType.sizeFactor)
    }

    fun eraserRadiusPx(density: Float) = listOf(8f, 16f, 30f)[eraserSize] * density

    // --------------------------------------------------------------- edits

    fun beginEdit(page: Int) {
        val p = pages.getOrNull(page) ?: return
        undoStack.addLast(page to p.items)
        if (undoStack.size > 200) undoStack.removeFirst()
        redoStack.clear()
        refreshUndo()
    }

    fun setItems(page: Int, items0: List<PageItem>) {
        touched()
        val p = pages.getOrNull(page) ?: return
        pages[page] = p.copy(items = items0)
        scheduleSave()
    }

    /** Pen is down: the toolbar fades so you can write under it. */
    var penActive by mutableStateOf(false)

    /** Pattern for new tape. */
    var tapePattern by mutableStateOf("stripes")

    fun useTapeImage(uri: Uri) {
        viewModelScope.launch {
            val name = runCatching { repo.saveImage(noteId, uri) }.getOrNull() ?: return@launch
            tapePattern = "image:$name"
            tool = Tool.TAPE
        }
    }

    /**
     * Template sheet: apply [style] with spacing and colours to this page, a new page after it,
     * or every writing page (PDF pages and covers are left alone).
     */
    fun applyTemplate(style: PaperStyle, scale: Float, lineColor: Int?, paperColor: Int?, target: Int) {
        fun styled(p: Page) = p.copy(paper = style, lineScale = scale, lineColor = lineColor, paperColor = paperColor ?: p.paperColor)
        when (target) {
            0 -> { val p = currentPage ?: return; pages[pageIndex] = styled(p) }
            1 -> {
                val base = newPage()
                pages.add(pageIndex + 1, styled(base))
                undoStack.clear(); redoStack.clear(); refreshUndo()
                scheduleSave(); onPagesChanged?.invoke(); onScrollTo?.invoke(pageIndex + 1)
                return
            }
            else -> pages.indices.forEach { i -> val p = pages[i]; if (p.pdfPage < 0 && p.cover == null && p.bgImage == null) pages[i] = styled(p) }
        }
        scheduleSave(); onPagesChanged?.invoke()
    }

    private fun applyItems(page: Int, items: List<PageItem>) {
        setItems(page, items)
        onPagesChanged?.invoke()
    }

    fun undo() = swap(undoStack, redoStack)
    fun redo() = swap(redoStack, undoStack)

    private fun swap(from: ArrayDeque<Pair<Int, List<PageItem>>>, to: ArrayDeque<Pair<Int, List<PageItem>>>) {
        val (idx, items) = from.removeLastOrNull() ?: return
        val page = pages.getOrNull(idx) ?: return
        to.addLast(idx to page.items)
        pages[idx] = page.copy(items = items)
        refreshUndo(); scheduleSave(); onPagesChanged?.invoke()
        if (idx != pageIndex) onScrollTo?.invoke(idx)
    }

    private fun refreshUndo() {
        canUndo = undoStack.isNotEmpty(); canRedo = redoStack.isNotEmpty()
    }

    fun onSelection(page: Int, ids: Set<Long>, bounds: RectF?) {
        selection = ids
        selectionPage = page
        selectionBounds = bounds
    }

    fun deleteSelection() {
        val p = pages.getOrNull(selectionPage) ?: return
        beginEdit(selectionPage)
        applyItems(selectionPage, p.items.filterNot { it.id in selection })
    }

    fun duplicateSelection(): Set<Long> {
        val p = pages.getOrNull(selectionPage) ?: return emptySet()
        beginEdit(selectionPage)
        val copies = p.items.filter { it.id in selection }.map { it.translated(30f, 30f).withId(ItemIds.next()) }
        applyItems(selectionPage, p.items + copies)
        return copies.map { it.id }.toSet()
    }

    fun recolorSelection(color: Int) {
        val p = pages.getOrNull(selectionPage) ?: return
        beginEdit(selectionPage)
        applyItems(selectionPage, p.items.map { item ->
            if (item.id !in selection) item
            else when (item) {
                is InkItem -> item.withStroke(Stroke(Brushes.recolor(item.stroke.brush, color), item.stroke.inputs))
                is TextItem -> item.copy(color = color)
                is TapeItem -> item.copy(color = color)
                is StickyItem -> item.copy(color = color)
                is WebLinkItem -> item.copy(color = color)
                // Fills keep their transparency when recoloured.
                is FillItem -> item.recolored((color and 0x00FFFFFF) or (item.color and 0xFF000000.toInt()))
                else -> item
            }
        })
    }

    /** Applies a vibrant spectral rainbow gradient across all selected strokes and items. */
    fun rainbowSelection() {
        val p = pages.getOrNull(selectionPage) ?: return
        val rainbowColors = intArrayOf(
            0xFFFF1744.toInt(), // Vivid Crimson
            0xFFFF9100.toInt(), // Sunset Orange
            0xFFFFEA00.toInt(), // Electric Yellow
            0xFF00E676.toInt(), // Spring Green
            0xFF00E5FF.toInt(), // Bright Cyan
            0xFF2979FF.toInt(), // Cobalt Blue
            0xFFD500F9.toInt(), // Neon Violet
        )
        beginEdit(selectionPage)
        var idx = 0
        applyItems(selectionPage, p.items.map { item ->
            if (item.id !in selection) item
            else {
                val c = rainbowColors[idx % rainbowColors.size]
                idx++
                when (item) {
                    is InkItem -> item.withStroke(Stroke(Brushes.recolor(item.stroke.brush, c), item.stroke.inputs))
                    is TextItem -> item.copy(color = c)
                    is TapeItem -> item.copy(color = c)
                    is StickyItem -> item.copy(color = c)
                    is WebLinkItem -> item.copy(color = c)
                    is FillItem -> item.recolored((c and 0x00FFFFFF) or (item.color and 0xFF000000.toInt()))
                    else -> item
                }
            }
        })
    }

    fun commitText(text: String, size: Float = textSize, color: Int = textColor ?: penStyle.color, style: TextStyleSpec = textStyle) {
        val edit = textEdit ?: return
        textEdit = null
        val p = pages.getOrNull(edit.page) ?: return
        val existing = edit.existing
        // New text remembers the last formatting used.
        textSize = size; textStyle = style; textColor = color
        val items = when {
            existing != null && text.isBlank() -> p.items.filterNot { it.id == existing.id }
            existing != null -> p.items.map { if (it.id == existing.id) existing.copy(text = text, size = size, color = color, style = style) else it }
            text.isBlank() -> return
            else -> p.items + TextItem(ItemIds.next(), text, edit.x, edit.y, size, color, style)
        }
        beginEdit(edit.page)
        applyItems(edit.page, items)
    }

    fun openSticky(page: Int, x: Float, y: Float, existing: StickyItem?) {
        stickyEdit = if (existing != null) StickyEdit(page, existing, false)
        else StickyEdit(page, StickyItem(ItemIds.next(), x - 150f, y - 150f, 300f, 300f, stickyColor, ""), true)
    }

    fun commitSticky(
        text: String,
        color: Int,
        fontSize: Float = 28f,
        font: String = "SANS",
        collapsed: Boolean = false,
        pinned: Boolean = false,
        title: String = "",
        style: String = "classic",
    ) {
        val edit = stickyEdit ?: return
        stickyEdit = null
        val p = pages.getOrNull(edit.page) ?: return
        val item = edit.item.copy(
            text = text,
            color = color,
            fontSize = fontSize.coerceIn(18f, 48f),
            font = font,
            collapsed = collapsed,
            pinned = pinned,
            title = title.trim(),
            style = style,
        )
        beginEdit(edit.page)
        applyItems(edit.page, if (edit.isNew) p.items + item else p.items.map { if (it.id == item.id) item else it })
    }

    fun toggleStickyCheckLine(page: Int, sticky: StickyItem, lineIndex: Int) {
        val p = pages.getOrNull(page) ?: return
        val lines = sticky.text.split('\n').toMutableList()
        val raw = lines.getOrNull(lineIndex) ?: return
        val indent = raw.takeWhile { it == ' ' || it == '\t' }
        val tr = raw.drop(indent.length)
        lines[lineIndex] = when {
            tr.startsWith("- [ ] ") -> indent + "- [x] " + tr.removePrefix("- [ ] ")
            tr.startsWith("- [x] ", ignoreCase = true) -> indent + "- [ ] " + tr.drop(6)
            tr.startsWith("☐ ") -> indent + "☑ " + tr.removePrefix("☐ ")
            tr.startsWith("☑ ") -> indent + "☐ " + tr.removePrefix("☑ ")
            else -> return
        }
        val updated = sticky.copy(text = lines.joinToString("\n"))
        beginEdit(page)
        applyItems(page, p.items.map { if (it.id == sticky.id) updated else it })
    }

    fun duplicateSticky() {
        val edit = stickyEdit ?: return
        stickyEdit = null
        val p = pages.getOrNull(edit.page) ?: return
        val copy = edit.item.copy(id = ItemIds.next(), x = edit.item.x + 36f, y = edit.item.y + 36f, pinned = false)
        beginEdit(edit.page)
        applyItems(edit.page, p.items + copy)
        say("Duplicated sticky note")
    }

    fun deleteSticky() {
        val edit = stickyEdit ?: return
        stickyEdit = null
        if (edit.isNew) return
        val p = pages.getOrNull(edit.page) ?: return
        beginEdit(edit.page)
        applyItems(edit.page, p.items.filterNot { it.id == edit.item.id })
    }

    // ------------------------------------------------------------- web links

    fun openWebLink(page: Int, x: Float, y: Float, existing: WebLinkItem?) {
        val p = pages.getOrNull(page) ?: return
        val cw = 420f
        val ch = 148f
        val sx = (x - cw / 2f).coerceIn(48f, (p.width - cw - 48f).coerceAtLeast(48f))
        val sy = (y - ch / 2f).coerceIn(48f, (p.height - ch - 48f).coerceAtLeast(48f))
        webLinkEdit = if (existing != null) WebLinkEdit(page, existing, false)
        else WebLinkEdit(page, WebLinkItem(ItemIds.next(), "", "", "", sx, sy, cw, ch, webLinkColor), true)
    }

    fun addQuickWebLink(url: String, linkTitle: String = "", note: String = "") {
        val p = currentPage ?: return
        val normalized = normalizeUrl(url)
        if (normalized.isBlank()) return
        val cw = 420f
        val ch = if (note.isBlank()) 128f else 156f
        val count = p.items.count { it is WebLinkItem }
        val sx = ((p.width - cw) / 2f).coerceAtLeast(48f)
        val sy = (140f + count * 36f).coerceIn(60f, (p.height - ch - 60f).coerceAtLeast(60f))
        val item = WebLinkItem(
            id = ItemIds.next(),
            url = normalized,
            title = linkTitle.trim().ifBlank { runCatching { android.net.Uri.parse(normalized).host?.removePrefix("www.") ?: normalized }.getOrDefault(normalized) },
            note = note.trim(),
            x = sx,
            y = sy,
            w = cw,
            h = ch,
            color = webLinkColor,
        )
        beginEdit(pageIndex)
        applyItems(pageIndex, p.items + item)
        say("Saved web link onto page")
    }

    fun commitWebLink(url: String, linkTitle: String, note: String, color: Int) {
        val edit = webLinkEdit ?: return
        webLinkEdit = null
        val p = pages.getOrNull(edit.page) ?: return
        val normalized = normalizeUrl(url)
        if (normalized.isBlank()) return
        webLinkColor = color
        val cleanTitle = linkTitle.trim().ifBlank {
            runCatching { android.net.Uri.parse(normalized).host?.removePrefix("www.") ?: normalized }.getOrDefault(normalized)
        }
        val cleanNote = note.trim()
        val ch = if (cleanNote.isBlank()) 124f else 156f
        val item = edit.item.copy(url = normalized, title = cleanTitle, note = cleanNote, color = color, h = maxOf(edit.item.h, ch))
        beginEdit(edit.page)
        applyItems(edit.page, if (edit.isNew) p.items + item else p.items.map { if (it.id == item.id) item else it })
    }

    fun deleteWebLink() {
        val edit = webLinkEdit ?: return
        webLinkEdit = null
        if (edit.isNew) return
        val p = pages.getOrNull(edit.page) ?: return
        beginEdit(edit.page)
        applyItems(edit.page, p.items.filterNot { it.id == edit.item.id })
    }

    private fun normalizeUrl(raw: String): String {
        val u = raw.trim()
        if (u.isBlank()) return ""
        return if (u.startsWith("http://") || u.startsWith("https://")) u else "https://$u"
    }

    fun openExternalUrl(rawUrl: String) {
        val u = normalizeUrl(rawUrl)
        if (u.isBlank()) return
        runCatching {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(u))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
        }.onFailure { say("Couldn't open link in browser") }
    }

    // ------------------------------------------------------------- mind maps

    fun selectedMindNode(): MindNodeItem? {
        val id = activeMindNodeId ?: return null
        return currentPage?.items?.filterIsInstance<MindNodeItem>()?.firstOrNull { it.id == id }
    }

    private fun mindBoxSize(label: String, shape: String, notes: String = ""): Pair<Float, Float> {
        val clean = label.trim().ifBlank { "Idea" }
        val baseLen = maxOf(clean.length, if (notes.isNotBlank()) 18 else 0)
        val w = (baseLen * 13.5f + 78f).coerceIn(180f, 360f)
        val extra = if (notes.isNotBlank()) 32f else 0f
        val h = when (shape) {
            "circle" -> (w * 0.64f + extra).coerceIn(114f, 196f)
            "diamond" -> 116f
            else -> (if (clean.length > 22) 96f else 78f) + extra
        }
        return w to h
    }

    private fun clampMindPos(page: Page, x: Float, y: Float, w: Float, h: Float): Pair<Float, Float> {
        if (page.isWhiteboard) return x to y
        val pad = 56f
        val maxX = (page.width - w - pad).coerceAtLeast(pad)
        val maxY = (page.height - h - pad).coerceAtLeast(pad)
        return x.coerceIn(pad, maxX) to y.coerceIn(pad, maxY)
    }

    fun fitMindMapToView() {
        val p = currentPage ?: return
        val nodes = p.items.filterIsInstance<MindNodeItem>()
        if (nodes.isEmpty()) { say("No mind map on this page yet"); return }
        val r = RectF(nodes.first().bounds)
        nodes.drop(1).forEach { r.union(it.bounds) }
        r.inset(-110f, -110f)
        if (!p.isWhiteboard) {
            r.left = r.left.coerceAtLeast(0f)
            r.top = r.top.coerceAtLeast(0f)
            r.right = r.right.coerceAtMost(p.width)
            r.bottom = r.bottom.coerceAtMost(p.height)
        }
        fitMindMapOnCanvas?.invoke(pageIndex, r)
    }

    fun onMindMapTap(page: Int, x: Float, y: Float, existing: MindNodeItem?) {
        if (existing != null) {
            if (activeMindNodeId == existing.id) {
                mindNodeEdit = MindNodeEdit(page, existing, false)
            } else {
                activeMindNodeId = existing.id
                mindColor = existing.color
                mindShape = existing.shape
                onRedraw?.invoke()
            }
            return
        }
        val p = pages.getOrNull(page) ?: return
        val existingNodes = p.items.filterIsInstance<MindNodeItem>()
        val parent = activeMindNodeId?.let { aid -> existingNodes.firstOrNull { it.id == aid } }
            ?: existingNodes.firstOrNull { it.parentId == null }
        val (w, h) = mindBoxSize(if (parent == null) "Central Topic" else "New idea", mindShape)
        val col = if (parent == null) mindColor else MindPalette[(existingNodes.size + 1) % MindPalette.size]
        val (cx, cy) = clampMindPos(p, x - w / 2f, y - h / 2f, w, h)
        val draft = MindNodeItem(
            id = ItemIds.next(),
            x = cx,
            y = cy,
            w = w,
            h = h,
            label = if (parent == null) "Central Topic" else "",
            parentId = parent?.id,
            color = col,
            shape = mindShape,
        )
        mindNodeEdit = MindNodeEdit(page, draft, true)
    }

    fun openEditSelectedMindNode() {
        val n = selectedMindNode() ?: return
        mindNodeEdit = MindNodeEdit(pageIndex, n, false)
    }

    fun addChildMindNode() {
        val p = currentPage ?: return
        val nodes = p.items.filterIsInstance<MindNodeItem>()
        val parent = selectedMindNode() ?: nodes.firstOrNull { it.parentId == null } ?: nodes.firstOrNull()
        val siblings = if (parent != null) nodes.count { it.parentId == parent.id } else 0
        val rawX = if (parent != null) parent.x + parent.w + 110f else p.width / 2f - 110f
        val rawY = if (parent != null) parent.y + (siblings - 0.5f) * 110f else p.height / 2f - 44f
        val col = MindPalette[(nodes.size + 1) % MindPalette.size]
        val (w, h) = mindBoxSize("New idea", mindShape)
        val (cx, cy) = clampMindPos(p, rawX, rawY, w, h)
        val draft = MindNodeItem(
            id = ItemIds.next(),
            x = cx,
            y = cy,
            w = w,
            h = h,
            label = "",
            parentId = parent?.id,
            color = col,
            shape = mindShape,
        )
        mindNodeEdit = MindNodeEdit(pageIndex, draft, true)
    }

    fun addSiblingMindNode() {
        val sel = selectedMindNode() ?: return
        val p = currentPage ?: return
        val nodes = p.items.filterIsInstance<MindNodeItem>()
        val sameParent = nodes.count { it.parentId == sel.parentId }
        val col = MindPalette[(nodes.size + 1) % MindPalette.size]
        val (w, h) = mindBoxSize("New idea", sel.shape)
        val (cx, cy) = clampMindPos(p, sel.x, sel.y + sel.h + 44f + (sameParent % 2) * 12f, w, h)
        val draft = MindNodeItem(
            id = ItemIds.next(),
            x = cx,
            y = cy,
            w = w,
            h = h,
            label = "",
            parentId = sel.parentId,
            color = col,
            shape = sel.shape,
        )
        mindNodeEdit = MindNodeEdit(pageIndex, draft, true)
    }

    fun commitMindNode(label: String, color: Int, shape: String, notes: String = "", tag: String = "") {
        val edit = mindNodeEdit ?: return
        mindNodeEdit = null
        val p = pages.getOrNull(edit.page) ?: return
        val clean = label.trim().ifBlank { if (edit.item.parentId == null) "Central Topic" else "Idea" }
        val cleanNotes = notes.trim()
        val cleanTag = tag.trim()
        val (w, h) = mindBoxSize(clean, shape, cleanNotes)
        val (cx, cy) = clampMindPos(p, edit.item.x, edit.item.y, w, h)
        val updated = edit.item.copy(
            x = cx,
            y = cy,
            label = clean,
            color = color,
            shape = shape,
            w = w,
            h = h,
            notes = cleanNotes,
            tag = cleanTag,
        )
        mindColor = color
        mindShape = shape
        activeMindNodeId = updated.id
        beginEdit(edit.page)
        val expanded = p.items.map {
            if (it is MindNodeItem && it.id == updated.parentId && it.collapsed) it.copy(collapsed = false) else it
        }
        applyItems(
            edit.page,
            if (edit.isNew) expanded + updated else expanded.map { if (it.id == updated.id) updated else it },
        )
    }

    fun deleteMindNode(nodeId: Long? = activeMindNodeId) {
        val id = nodeId ?: return
        mindNodeEdit = null
        val p = currentPage ?: return
        val nodes = p.items.filterIsInstance<MindNodeItem>()
        if (nodes.none { it.id == id }) return
        val dead = HashSet<Long>().apply { add(id) }
        var grew = true
        while (grew) {
            grew = false
            nodes.forEach { n -> if (n.parentId in dead && dead.add(n.id)) grew = true }
        }
        if (activeMindNodeId in dead) activeMindNodeId = null
        beginEdit(pageIndex)
        applyItems(pageIndex, p.items.filterNot { it.id in dead })
    }

    fun toggleCollapseMindNode(nodeId: Long? = activeMindNodeId) {
        val id = nodeId ?: return
        val p = currentPage ?: return
        if (p.items.none { it is MindNodeItem && it.id == id }) return
        beginEdit(pageIndex)
        applyItems(pageIndex, p.items.map { if (it is MindNodeItem && it.id == id) it.copy(collapsed = !it.collapsed) else it })
    }

    fun recolorMindNode(nodeId: Long? = activeMindNodeId, color: Int) {
        val id = nodeId ?: return
        val p = currentPage ?: return
        mindColor = color
        if (p.items.none { it is MindNodeItem && it.id == id }) return
        beginEdit(pageIndex)
        applyItems(pageIndex, p.items.map { if (it is MindNodeItem && it.id == id) it.copy(color = color) else it })
    }

    fun autoLayoutMindMap(layout: MindLayout = MindLayout.RADIAL) {
        val p = currentPage ?: return
        val nodes = p.items.filterIsInstance<MindNodeItem>()
        if (nodes.isEmpty()) return
        val byId = nodes.associateBy { it.id }
        val roots = nodes.filter { it.parentId == null || it.parentId !in byId }
        val children = nodes.filter { it.parentId != null && it.parentId in byId }.groupBy { it.parentId!! }
        val pos = HashMap<Long, Pair<Float, Float>>()
        val cx = p.width / 2f
        val cy = p.height / 2f

        when (layout) {
            MindLayout.RADIAL -> {
                fun countLeaves(id: Long, seen: HashSet<Long>): Int {
                    if (!seen.add(id)) return 1
                    val ch = children[id].orEmpty()
                    return if (ch.isEmpty()) 1 else ch.sumOf { countLeaves(it.id, seen) }
                }
                fun placeSubtree(id: Long, cxNode: Float, cyNode: Float, aStart: Double, aEnd: Double, depth: Int, seen: HashSet<Long>) {
                    if (!seen.add(id)) return
                    val n = byId[id] ?: return
                    pos[id] = (cxNode - n.w / 2f) to (cyNode - n.h / 2f)
                    val ch = children[id].orEmpty()
                    if (ch.isEmpty()) return
                    val totalLeaves = ch.sumOf { countLeaves(it.id, HashSet()) }.coerceAtLeast(1)
                    val radius = if (depth == 0) 320f else 250f
                    var cursor = aStart
                    for (c in ch) {
                        val weight = countLeaves(c.id, HashSet()).toDouble() / totalLeaves
                        val span = (aEnd - aStart) * weight
                        val mid = cursor + span / 2.0
                        val nx = cxNode + (radius * kotlin.math.cos(mid)).toFloat()
                        val ny = cyNode + (radius * kotlin.math.sin(mid)).toFloat()
                        placeSubtree(c.id, nx, ny, mid - span / 2.0, mid + span / 2.0, depth + 1, seen)
                        cursor += span
                    }
                }
                roots.forEachIndexed { idx, root ->
                    val rx = cx + (idx - (roots.size - 1) / 2f) * 680f
                    placeSubtree(root.id, rx, cy, -Math.PI, Math.PI, 0, HashSet())
                }
            }
            MindLayout.TREE -> {
                val rowGap = 118f
                val colGap = 295f
                fun subtreeHeight(id: Long, seen: HashSet<Long>): Float {
                    if (!seen.add(id)) return rowGap
                    val ch = children[id].orEmpty()
                    return if (ch.isEmpty()) rowGap else ch.sumOf { subtreeHeight(it.id, seen).toDouble() }.toFloat()
                }
                fun placeTree(id: Long, xCenter: Float, yTop: Float, spanH: Float, seen: HashSet<Long>) {
                    if (!seen.add(id)) return
                    val n = byId[id] ?: return
                    pos[id] = (xCenter - n.w / 2f) to (yTop + spanH / 2f - n.h / 2f)
                    val ch = children[id].orEmpty()
                    var curY = yTop
                    for (c in ch) {
                        val chH = subtreeHeight(c.id, HashSet())
                        placeTree(c.id, xCenter + colGap, curY, chH, seen)
                        curY += chH
                    }
                }
                var curTop = cy - roots.sumOf { subtreeHeight(it.id, HashSet()).toDouble() }.toFloat() / 2f
                val startX = (cx - 280f).coerceAtLeast(140f)
                roots.forEach { r ->
                    val h = subtreeHeight(r.id, HashSet())
                    placeTree(r.id, startX, curTop, h, HashSet())
                    curTop += h + 60f
                }
            }
            MindLayout.ORG_CHART -> {
                val colGap = 240f
                val rowGap = 185f
                fun subtreeWidth(id: Long, seen: HashSet<Long>): Float {
                    if (!seen.add(id)) return colGap
                    val ch = children[id].orEmpty()
                    return if (ch.isEmpty()) colGap else ch.sumOf { subtreeWidth(it.id, seen).toDouble() }.toFloat()
                }
                fun placeOrg(id: Long, xLeft: Float, yCenter: Float, spanW: Float, seen: HashSet<Long>) {
                    if (!seen.add(id)) return
                    val n = byId[id] ?: return
                    pos[id] = (xLeft + spanW / 2f - n.w / 2f) to (yCenter - n.h / 2f)
                    val ch = children[id].orEmpty()
                    var curX = xLeft
                    for (c in ch) {
                        val cw = subtreeWidth(c.id, HashSet())
                        placeOrg(c.id, curX, yCenter + rowGap, cw, seen)
                        curX += cw
                    }
                }
                val totalW = roots.sumOf { subtreeWidth(it.id, HashSet()).toDouble() }.toFloat()
                var curX = (cx - totalW / 2f).coerceAtLeast(80f)
                val startY = 180f
                roots.forEach { r ->
                    val w = subtreeWidth(r.id, HashSet())
                    placeOrg(r.id, curX, startY, w, HashSet())
                    curX += w + 40f
                }
            }
        }

        // Keep non-whiteboard mind maps strictly within page margins
        if (!p.isWhiteboard && pos.isNotEmpty()) {
            var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
            for ((id, xy) in pos) {
                val n = byId[id] ?: continue
                minX = minOf(minX, xy.first)
                minY = minOf(minY, xy.second)
                maxX = maxOf(maxX, xy.first + n.w)
                maxY = maxOf(maxY, xy.second + n.h)
            }
            val availW = (p.width - 112f).coerceAtLeast(240f)
            val availH = (p.height - 112f).coerceAtLeast(240f)
            val spanW = (maxX - minX).coerceAtLeast(1f)
            val spanH = (maxY - minY).coerceAtLeast(1f)
            val sx = if (spanW > availW) availW / spanW else 1f
            val sy = if (spanH > availH) availH / spanH else 1f
            val boxCx = (minX + maxX) / 2f
            val boxCy = (minY + maxY) / 2f
            for ((id, xy) in pos.toMap()) {
                val n = byId[id] ?: continue
                val ncx = xy.first + n.w / 2f
                val ncy = xy.second + n.h / 2f
                val scaledX = cx + (ncx - boxCx) * sx - n.w / 2f
                val scaledY = cy + (ncy - boxCy) * sy - n.h / 2f
                pos[id] = clampMindPos(p, scaledX, scaledY, n.w, n.h)
            }
        }

        beginEdit(pageIndex)
        applyItems(
            pageIndex,
            p.items.map { item ->
                if (item is MindNodeItem) pos[item.id]?.let { (nx, ny) -> item.copy(x = nx, y = ny) } ?: item else item
            },
        )
    }

    fun exportMindMapOutlineToPage() {
        val p = currentPage ?: return
        val nodes = p.items.filterIsInstance<MindNodeItem>()
        if (nodes.isEmpty()) { say("Add mind map nodes first"); return }
        val byId = nodes.associateBy { it.id }
        val roots = nodes.filter { it.parentId == null || it.parentId !in byId }
        val children = nodes.filter { it.parentId != null && it.parentId in byId }.groupBy { it.parentId!! }
        val sb = StringBuilder()
        fun walk(n: MindNodeItem, depth: Int, seen: HashSet<Long>) {
            if (!seen.add(n.id)) return
            val prefix = when (depth) {
                0 -> ""
                else -> "  ".repeat(depth) + "• "
            }
            sb.append(prefix).append(n.label)
            if (n.tag.isNotBlank()) sb.append(" [").append(n.tag).append("]")
            if (n.notes.isNotBlank()) sb.append(" — ").append(n.notes.trim())
            sb.append('\n')
            children[n.id].orEmpty().forEach { walk(it, depth + 1, seen) }
        }
        val seen = HashSet<Long>()
        roots.forEach { walk(it, 0, seen) }
        val outline = sb.toString().trim()
        beginEdit(pageIndex)
        applyItems(pageIndex, p.items + TextItem(ItemIds.next(), outline, 70f, 90f, 24f, penStyle.color))
        say("Exported mind map outline onto page")
    }

    fun flashcardsFromMindMap() {
        val p = currentPage ?: return
        val nodes = p.items.filterIsInstance<MindNodeItem>()
        if (nodes.isEmpty()) { say("Add mind map nodes first"); return }
        val byId = nodes.associateBy { it.id }
        val children = nodes.filter { it.parentId != null && it.parentId in byId }.groupBy { it.parentId!! }
        val have = cards.map { it.front.lowercase() }.toHashSet()
        val add = ArrayList<com.pronotes.app.data.Flashcard>()
        for (n in nodes) {
            val ch = children[n.id].orEmpty()
            val front = when {
                ch.isNotEmpty() -> "Key branches of ${n.label}?"
                n.notes.isNotBlank() -> n.label
                n.parentId != null -> "${byId[n.parentId]?.label ?: title}: ${n.label}"
                else -> continue
            }
            val back = when {
                n.notes.isNotBlank() -> n.notes.trim()
                ch.isNotEmpty() -> ch.joinToString("\n• ", prefix = "• ") { it.label }
                else -> n.label
            }
            if (front.lowercase() !in have) {
                have += front.lowercase()
                add += com.pronotes.app.data.Flashcard(
                    id = UUID.randomUUID().toString(),
                    front = front,
                    back = back,
                    pageId = p.id,
                    created = System.currentTimeMillis(),
                )
            }
        }
        if (add.isNotEmpty()) {
            cards = cards + add
            saveStudy()
            say("Created ${add.size} flashcards from mind map")
        } else {
            say("All mind map cards already exist")
        }
    }

    fun generateMindMapFromNotes() {
        val pageIdx = pageIndex
        val p = pages.getOrNull(pageIdx) ?: return
        viewModelScope.launch {
            aiStage = "Building mind map branches…"
            aiBusy = true
            val (rootTitle, branches) = try {
                val (text, _) = studyText(pageIdx..pageIdx)
                runCatching { Nano.mindMap(title, text) }.getOrNull()
                    ?: LocalAiManager.buildMindMap(getApplication(), title, text)
            } finally {
                aiBusy = false
                aiStage = ""
            }
            val nowPage = pages.getOrNull(pageIdx) ?: return@launch
            val outNodes = ArrayList<MindNodeItem>()
            val (rw, rh) = mindBoxSize(rootTitle, "pill")
            val rootId = ItemIds.next()
            outNodes += MindNodeItem(
                id = rootId,
                x = nowPage.width / 2f - rw / 2f,
                y = nowPage.height / 2f - rh / 2f,
                w = rw,
                h = rh,
                label = rootTitle,
                parentId = null,
                color = MindPalette[0],
                shape = "pill",
            )
            branches.forEachIndexed { bIdx, (branchTitle, children) ->
                val branchColor = MindPalette[(bIdx + 1) % MindPalette.size]
                val (bw, bh) = mindBoxSize(branchTitle, "rect")
                val bid = ItemIds.next()
                outNodes += MindNodeItem(bid, 0f, 0f, bw, bh, branchTitle, rootId, branchColor, "rect")
                children.forEach { childLabel ->
                    val (cw, ch) = mindBoxSize(childLabel, "pill")
                    outNodes += MindNodeItem(ItemIds.next(), 0f, 0f, cw, ch, childLabel, bid, branchColor, "pill")
                }
            }
            tool = Tool.MINDMAP
            activeMindNodeId = rootId
            beginEdit(pageIdx)
            val kept = nowPage.items.filterNot { it is MindNodeItem }
            applyItems(pageIdx, kept + outNodes)
            autoLayoutMindMap(MindLayout.RADIAL)
            say("Generated mind map (${outNodes.size} nodes)")
        }
    }

    /** Emoji stickers are large text items placed in the middle of the visible page. */
    fun insertSticker(emoji: String): Pair<Int, Long>? {
        val p = currentPage ?: return null
        val item = TextItem(ItemIds.next(), emoji, p.width / 2f - 80f, p.height / 3f, 160f, 0xFF000000.toInt(), TextStyleSpec())
        beginEdit(pageIndex)
        applyItems(pageIndex, p.items + item)
        return pageIndex to item.id
    }

    fun insertImage(uri: Uri, onPlaced: (Int, Long) -> Unit) {
        viewModelScope.launch {
            val name = runCatching { repo.saveImage(noteId, uri) }.getOrNull() ?: return@launch
            val bmp = withContext(Dispatchers.IO) { renderer().image(name) } ?: return@launch
            val idx = pageIndex
            val p = pages.getOrNull(idx) ?: return@launch
            val w = (p.width * 0.6f).coerceAtMost(bmp.width.toFloat().coerceAtLeast(200f))
            val h = w * bmp.height / bmp.width
            val item = ImageItem(ItemIds.next(), name, (p.width - w) / 2f, (p.height - h) / 3f, w, h)
            beginEdit(idx)
            applyItems(idx, p.items + item)
            onPlaced(idx, item.id)
        }
    }

    // --------------------------------------------------------------- pages

    fun goToPage(i: Int) { if (i in pages.indices) onScrollTo?.invoke(i) }

    private fun newPage(): Page {
        val cur = currentPage
        // New pages copy size, template and colour from a nearby writing page (not the cover or a PDF page).
        val ref = pages.getOrNull(pageIndex)?.takeIf { it.cover == null && it.pdfPage < 0 }
            ?: pages.lastOrNull { it.cover == null && it.pdfPage < 0 }
        val paper = ref?.paper ?: repo.meta(noteId)?.paper ?: settingsFlow.value.defaultPaper
        val w = ref?.width ?: cur?.takeIf { it.pdfPage < 0 }?.width ?: PageSize.WIDTH
        val h = ref?.height ?: cur?.takeIf { it.pdfPage < 0 }?.height ?: PageSize.HEIGHT
        return Page(UUID.randomUUID().toString(), w, h, paper, paperColor = ref?.paperColor, lineScale = ref?.lineScale ?: 1f, lineColor = ref?.lineColor, isWhiteboard = ref?.isWhiteboard ?: false)
    }

    /** Custom page: uses a picture as the background of the page in view (null removes it). */
    fun setPageBackground(uri: Uri?) {
        val idx = pageIndex
        viewModelScope.launch {
            val name = if (uri == null) null else runCatching { repo.saveImage(noteId, uri) }.getOrNull() ?: return@launch
            val p = pages.getOrNull(idx) ?: return@launch
            pages[idx] = p.copy(bgImage = name)
            thumbs.evictAll()
            scheduleSave(); onPagesChanged?.invoke()
        }
    }

    /** Paper colour of the page in view; null follows the app setting. */
    fun setPageColor(color: Int?) {
        val p = currentPage ?: return
        pages[pageIndex] = p.copy(paperColor = color)
        scheduleSave(); onPagesChanged?.invoke()
    }

    fun toggleCurrentWhiteboard() {
        val p = currentPage ?: return
        val nextWb = !p.isWhiteboard
        val nextPaper = if (nextWb && p.paper == PaperStyle.BLANK) PaperStyle.DOTTED else p.paper
        pages[pageIndex] = p.copy(isWhiteboard = nextWb, paper = nextPaper)
        scheduleSave(); onPagesChanged?.invoke()
        say(if (nextWb) "Switched to ∞ Infinite Whiteboard" else "Switched to standard page")
    }

    fun addPage(atEnd: Boolean = false, paper: PaperStyle? = null, isWhiteboard: Boolean? = null) {
        val np = newPage().let {
            it.copy(
                paper = paper ?: it.paper,
                isWhiteboard = isWhiteboard ?: it.isWhiteboard,
            )
        }
        val at = if (atEnd) pages.size else pageIndex + 1
        pages.add(at, np)
        undoStack.clear(); redoStack.clear(); refreshUndo()
        scheduleSave(); onPagesChanged?.invoke()
        onScrollTo?.invoke(at)
    }

    fun deletePage() {
        if (pages.size <= 1) return
        pages.removeAt(pageIndex)
        undoStack.clear(); redoStack.clear(); refreshUndo()
        pageIndex = pageIndex.coerceAtMost(pages.size - 1)
        scheduleSave(); onPagesChanged?.invoke()
    }

    fun duplicatePage() {
        val p = currentPage ?: return
        val copy = p.copy(id = UUID.randomUUID().toString(), items = p.items.map { it.withId(ItemIds.next()) })
        pages.add(pageIndex + 1, copy)
        undoStack.clear(); redoStack.clear(); refreshUndo()
        scheduleSave(); onPagesChanged?.invoke()
        onScrollTo?.invoke(pageIndex + 1)
    }

    fun setPaper(style: PaperStyle) {
        val p = currentPage ?: return
        pages[pageIndex] = p.copy(paper = style)
        scheduleSave(); onPagesChanged?.invoke()
    }

    /**
     * The PDF page as a bitmap sharp enough for [widthPx] on screen. Returns what is cached
     * right away (maybe softer) and renders a sharper one in the background when zoomed in.
     */
    fun pdfBitmap(page: Page, widthPx: Int, onReady: () -> Unit): Bitmap? {
        val src = pdf ?: return null
        val have = src.cached(page.pdfPage)
        var want = ((widthPx + 255) / 256 * 256).coerceIn(768, 1600)
        val aspect = page.height / page.width
        // Keep very tall pages within a sane pixel budget.
        if (want * want * aspect > 4_500_000f) want = kotlin.math.sqrt(4_500_000f / aspect).toInt()
        val key = page.pdfPage
        pdfSeen[key] = android.os.SystemClock.uptimeMillis()
        pdfWant[key] = want
        if ((have == null || have.width < want * 0.85f) && pendingPdf.add(key)) {
            viewModelScope.launch {
                // First sight of a page: draw a quick softer version so it appears fast.
                // Sharpening waits until zoom and scroll settle, so pinching never floods the renderer.
                if (have != null) kotlinx.coroutines.delay(180)
                val seen = pdfSeen[key] ?: 0L
                if (android.os.SystemClock.uptimeMillis() - seen < 700) {
                    val w = if (have == null) minOf(pdfWant[key] ?: want, 760) else (pdfWant[key] ?: want)
                    withContext(Dispatchers.IO) {
                        // Skip pages that were scrolled away while waiting in line.
                        if (android.os.SystemClock.uptimeMillis() - (pdfSeen[key] ?: 0L) < 700) src.render(key, w, (w * aspect).toInt())
                    }
                }
                pendingPdf.remove(key)
                onReady()
            }
        }
        return have
    }

    // ------------------------------------------------ zoomed-in PDF detail (sharp, viewport-sized)
    private var detailPdfInstance: PdfPages? = null
    @Synchronized private fun detailPdf(): PdfPages = detailPdfInstance ?: PdfPages(repo.pdfFile(noteId)).also { detailPdfInstance = it }
    data class Detail(val page: Int, val region: RectF, val bmp: Bitmap, val scale: Float)
    @Volatile private var details: List<Detail> = emptyList()
    private var detailJob: Job? = null

    fun pdfDetail(page: Int): Detail? = details.firstOrNull { it.page == page }

    /**
     * The canvas settled at a zoom where the base bitmap would look soft: render exactly what is
     * on screen at screen resolution. Debounced, and cancelled when the view moves again.
     */
    fun requestDetail(wants: List<Triple<Int, RectF, Float>>, onReady: () -> Unit) {
        detailJob?.cancel()
        if (pdf == null) return
        if (wants.isEmpty()) { if (details.isNotEmpty()) { details = emptyList(); onReady() }; return }
        // Keep what still matches; it is redrawn in place until the new detail is ready.
        detailJob = viewModelScope.launch {
            delay(140)
            val out = ArrayList<Detail>()
            for ((pi, region, screenScale) in wants) {
                val page = pages.getOrNull(pi) ?: continue
                if (page.pdfPage < 0) continue
                val ow = (region.width() * screenScale).toInt().coerceIn(1, 2600)
                val oh = (region.height() * screenScale).toInt().coerceIn(1, 2600)
                val bmp = withContext(Dispatchers.IO) { runCatching { detailPdf().renderRegion(page.pdfPage, page.width, region, ow, oh) }.getOrNull() } ?: continue
                out += Detail(pi, RectF(region), bmp, screenScale)
            }
            details = out
            onReady()
        }
    }

    /** Thumbnails are cached until the page changes (pages are immutable, so identity is enough). */
    private val thumbs = object : android.util.LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    suspend fun thumbnail(index: Int, width: Int): Bitmap? {
        val page = pages.getOrNull(index) ?: return null
        val key = "${page.id}:${System.identityHashCode(page)}:$width"
        thumbs.get(key)?.let { return it }
        return renderThumb(page, width)?.also { thumbs.put(key, it) }
    }

    private suspend fun renderThumb(page: Page, width: Int): Bitmap? = withContext(Dispatchers.Default) {
        val scale = width / page.width
        val h = (page.height * scale).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(width, h, Bitmap.Config.ARGB_8888)
        val bg = if (page.pdfPage >= 0) withContext(Dispatchers.IO) {
            thumbPdf().render(page.pdfPage, width, h, useCache = false)
        } else null
        // One shared renderer keeps decoded images warm; its paints are not thread-safe, so take turns.
        thumbLock.withLock {
            val r = thumbRenderer ?: renderer().also { thumbRenderer = it }
            r.paperColor = settingsFlow.value.paperTint.argb
            r.renderPage(Canvas(bmp), page, scale, bg)
        }
        bmp
    }

    private val thumbLock = kotlinx.coroutines.sync.Mutex()
    private var thumbRenderer: PageRenderer? = null
    private var thumbPdfInstance: PdfPages? = null

    @Synchronized
    private fun thumbPdf(): PdfPages = thumbPdfInstance ?: PdfPages(repo.pdfFile(noteId)).also { thumbPdfInstance = it }

    // ------------------------------------------------------- flashcards + ink links
    private val study = com.pronotes.app.data.StudyStore(repo.noteDir(noteId))
    var cards by mutableStateOf<List<com.pronotes.app.data.Flashcard>>(emptyList())
        private set
    var links by mutableStateOf<List<com.pronotes.app.data.InkLink>>(emptyList())
        private set
    var slides by mutableStateOf<List<com.pronotes.app.data.SlideCard>>(emptyList())
        private set
    var showSlideShow by mutableStateOf(false)
    /** Canvas is waiting for the user to drag a box for a new flashcard. */
    var capturingCard by mutableStateOf(false)
    /** Card being created or edited (front picture already cut). */
    data class CardDraft(val card: com.pronotes.app.data.Flashcard, val isNew: Boolean)
    var cardDraft by mutableStateOf<CardDraft?>(null)
    /** Start of a link waiting for its other end. */
    var linkFrom by mutableStateOf<Triple<String, Float, Float>?>(null)
    var linkToDelete by mutableStateOf<String?>(null)

    private fun loadStudy() { val d = study.load(); cards = d.cards; links = d.links; quiz = d.quiz; slides = d.slides }
    private fun saveStudy() {
        val d = com.pronotes.app.data.StudyData(cards, links, quiz, slides)
        viewModelScope.launch(Dispatchers.IO) { runCatching { study.save(d) } }
    }

    fun dueCards(now: Long = System.currentTimeMillis()) = cards.filter { it.due <= now }

    fun cardImage(name: String?): Bitmap? = name?.let { runCatching { android.graphics.BitmapFactory.decodeFile(java.io.File(study.cardDir(), it).path) }.getOrNull() }

    /** What the next dragged box is for: a flashcard (false) or a quiz question (true). */
    var captureForQuiz by mutableStateOf(false)
    /** A question waiting in the quiz editor (opened from outside the quiz sheet). */
    var quizDraft by mutableStateOf<com.pronotes.app.data.QuizQuestion?>(null)

    /** Card waiting for its answer to be cut from the page. */
    var backFor by mutableStateOf<CardDraft?>(null)

    fun captureBack(d: CardDraft) { backFor = d; cardDraft = null; captureForQuiz = false; capturingCard = true }

    fun onAreaPicked(pageIdx: Int, r: RectF) {
        capturingCard = false
        backFor?.let { d ->
            backFor = null
            val page = pages.getOrNull(pageIdx) ?: return
            viewModelScope.launch {
                val bmp = regionImage(page, r) ?: run { cardDraft = d; return@launch }
                val name = "${d.card.id}-back.png"
                withContext(Dispatchers.IO) { java.io.File(study.cardDir(), name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
                // Also read any PDF text in the box as the written answer.
                val txt = if (page.pdfPage >= 0) indexFor(page.pdfPage)?.let { idx ->
                    val a = idx.charAt(page.pdfPage, r.left + 4f, r.top + 4f); val b = idx.charAt(page.pdfPage, r.right - 4f, r.bottom - 4f)
                    if (a != null && b != null && b >= a) idx.text(page.pdfPage, a..b) else null
                } else null
                cardDraft = d.copy(card = d.card.copy(backImage = name, back = d.card.back.ifBlank { txt ?: "" }))
            }
            return
        }
        if (captureForQuiz) { captureForQuiz = false; startQuizFromArea(pageIdx, r) } else startCardFromArea(pageIdx, r)
    }

    /**
     * Picture quiz: the boxed area becomes the question's picture. With the on-device model it reads
     * the picture and writes questions; otherwise you get an editor with the picture to write your own.
     */
    private fun startQuizFromArea(pageIdx: Int, r: RectF) {
        val page = pages.getOrNull(pageIdx) ?: return
        viewModelScope.launch {
            val bmp = regionImage(page, r) ?: return@launch
            val name = "q-${UUID.randomUUID()}.png"
            withContext(Dispatchers.IO) { java.io.File(study.cardDir(), name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            aiBusy = true
            val made = try { runCatching { Nano.quizFromImage(bmp, 3) }.getOrNull() } finally { aiBusy = false }
            if (made.isNullOrEmpty()) {
                quizDraft = com.pronotes.app.data.QuizQuestion(UUID.randomUUID().toString(), "", listOf("", "", "", ""), 0, pageId = page.id, image = name)
                showQuizRequest++
            } else {
                quiz = quiz + made.map { com.pronotes.app.data.QuizQuestion(UUID.randomUUID().toString(), it.question, it.options, it.answer, it.explanation, page.id, generated = true, image = name) }
                saveStudy()
                say("Added ${made.size} picture questions")
                showQuizRequest++
            }
        }
    }

    /** Bumped to ask the editor to open the quiz sheet. */
    var showQuizRequest by mutableStateOf(0)

    /** Best score (percent) and its time, per notebook. */
    fun quizBest(): Pair<Int, Long>? = store.pref("quizBest_$noteId").split(',').let { p ->
        val s = p.getOrNull(0)?.toIntOrNull() ?: return null
        s to (p.getOrNull(1)?.toLongOrNull() ?: 0L)
    }
    fun recordQuiz(percent: Int, ms: Long): Boolean {
        val b = quizBest()
        val better = b == null || percent > b.first || (percent == b.first && ms < b.second)
        if (better) store.setPref("quizBest_$noteId", "$percent,$ms")
        return better
    }

    /** Cuts a picture of [r] on page [pageIdx] (paper, PDF and ink) and opens the card editor. */
    fun startCardFromArea(pageIdx: Int, r: RectF) {
        val page = pages.getOrNull(pageIdx) ?: return
        viewModelScope.launch {
            val bmp = regionImage(page, r) ?: return@launch
            val id = UUID.randomUUID().toString()
            val name = "$id-front.png"
            withContext(Dispatchers.IO) { java.io.File(study.cardDir(), name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            cardDraft = CardDraft(com.pronotes.app.data.Flashcard(id, frontImage = name, pageId = page.id, created = System.currentTimeMillis()), true)
        }
    }

    fun cardFromSelection() {
        val r = selectionUnion() ?: return
        r.inset(-20f, -20f)
        startCardFromArea(selectionPage, r)
    }

    fun newTextCard() {
        cardDraft = CardDraft(com.pronotes.app.data.Flashcard(UUID.randomUUID().toString(), pageId = currentPage?.id, created = System.currentTimeMillis()), true)
    }

    fun saveCard(card: com.pronotes.app.data.Flashcard) {
        val isNew = cards.none { it.id == card.id }
        cards = if (!isNew) cards.map { if (it.id == card.id) card else it } else cards + card
        cardDraft = null
        saveStudy()
    }

    fun deleteCard(id: String) {
        val c = cards.firstOrNull { it.id == id } ?: return
        cards = cards.filterNot { it.id == id }
        listOfNotNull(c.frontImage, c.backImage).forEach { java.io.File(study.cardDir(), it).delete() }
        saveStudy()
    }

    fun gradeCard(id: String, grade: Int) {
        cards = cards.map { if (it.id == id) com.pronotes.app.data.StudyStore.review(it, grade) else it }
        saveStudy()
    }

    fun showCardSource(card: com.pronotes.app.data.Flashcard) {
        val i = pages.indexOfFirst { it.id == card.pageId }
        if (i >= 0) goToPage(i)
    }

    /** All cards as a CSV file (front, back) for other flashcard apps. */
    fun cardsCsv(): String = buildString {
        append("front,back\n")
        cards.forEach { c ->
            fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
            append(q(c.front.ifBlank { if (c.frontImage != null) "[picture]" else "" })).append(",").append(q(c.back)).append("\n")
        }
    }

    private suspend fun regionImage(page: Page, r: RectF): Bitmap? = withContext(Dispatchers.Default) {
        val s = (1400f / maxOf(r.width(), r.height(), 1f)).coerceIn(0.5f, 3f).coerceAtMost(3200f / page.width)
        val pw = (page.width * s).toInt().coerceAtLeast(1)
        val ph = (page.height * s).toInt().coerceAtLeast(1)
        val out = Bitmap.createBitmap((r.width() * s).toInt().coerceAtLeast(1), (r.height() * s).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val bg = if (page.pdfPage >= 0) withContext(Dispatchers.IO) { thumbPdf().render(page.pdfPage, pw, ph, useCache = false) } else null
        val c = Canvas(out)
        c.translate(-r.left * s, -r.top * s)
        thumbLock.withLock {
            val rr = thumbRenderer ?: renderer().also { thumbRenderer = it }
            rr.paperColor = settingsFlow.value.paperTint.argb
            rr.renderPage(c, page, s, bg)
        }
        out
    }

    /** Selected content waiting for a link target (page id + box), shown in the link sheet. */
    var linkSheet by mutableStateOf<Pair<String, RectF>?>(null)

    /** Top menu "Add link…": switch to the lasso and explain the next step. */
    fun beginLinkFlow() {
        if (selection.isNotEmpty()) { startLinkFromSelection(); return }
        tool = Tool.LASSO
        say("Circle what you want to link, then tap Link")
    }

    fun startLinkFromSelection() {
        val r = selectionUnion() ?: return
        val page = pages.getOrNull(selectionPage) ?: return
        r.inset(-8f, -8f)
        linkSheet = page.id to r
    }

    private fun withoutOverlap(pid: String, r: RectF) = links.filterNot { l ->
        l.aPage == pid && l.box.size == 4 && RectF.intersects(RectF(l.box[0], l.box[1], l.box[2], l.box[3]), r)
    }

    /** Link the selected content to page [target] of this notebook. */
    fun linkToPage(target: Int) {
        val (pid, r) = linkSheet ?: return
        val tp = pages.getOrNull(target) ?: return
        linkSheet = null
        links = withoutOverlap(pid, r) + com.pronotes.app.data.InkLink(
            UUID.randomUUID().toString(), pid, r.right, r.top, tp.id, tp.width / 2f, 0f, listOf(r.left, r.top, r.right, r.bottom),
        )
        saveStudy()
    }

    /** Link the selected content to a web address. */
    fun linkToUrl(raw: String) {
        val (pid, r) = linkSheet ?: return
        val u = raw.trim().let { if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it" }
        linkSheet = null
        links = withoutOverlap(pid, r) + com.pronotes.app.data.InkLink(
            UUID.randomUUID().toString(), pid, r.right, r.top, box = listOf(r.left, r.top, r.right, r.bottom), url = u,
        )
        saveStudy()
    }

    /** Text for the link list: where each link on this page goes. */
    fun linkLabel(e: LinkEnd): String = e.url ?: "Page ${e.toPage + 1}"

    fun finishLink(pageIdx: Int, x: Float, y: Float) {
        val from = linkFrom ?: return
        linkFrom = null
        val page = pages.getOrNull(pageIdx) ?: return
        if (page.id == from.first && kotlin.math.hypot(x - from.second, y - from.third) < 40f) return
        links = links + com.pronotes.app.data.InkLink(UUID.randomUUID().toString(), from.first, from.second, from.third, page.id, x, y)
        saveStudy()
    }

    /** Links whose box overlaps the current selection. */
    fun selectionLinks(): List<String> {
        val r = selectionUnion() ?: return emptyList()
        val pid = pages.getOrNull(selectionPage)?.id ?: return emptyList()
        return links.filter { it.aPage == pid && it.box.size == 4 && RectF.intersects(RectF(it.box[0], it.box[1], it.box[2], it.box[3]), r) }.map { it.id }
    }

    fun unlinkSelection() {
        val ids = selectionLinks().toSet()
        if (ids.isEmpty()) return
        links = links.filterNot { it.id in ids }
        saveStudy(); onRedraw?.invoke()
        say("Link removed")
    }

    fun deleteLink(id: String) { links = links.filterNot { it.id == id }; linkToDelete = null; saveStudy(); onRedraw?.invoke() }

    /** Link ends as page indexes, for drawing badges and jumping. */
    fun linkEnds(): List<LinkEnd> {
        if (links.isEmpty()) return emptyList()
        val index = HashMap<String, Int>(pages.size * 2)
        pages.forEachIndexed { i, p -> index[p.id] = i }
        val out = ArrayList<LinkEnd>()
        for (l in links) {
            val a = index[l.aPage] ?: continue
            val box = if (l.box.size == 4) RectF(l.box[0], l.box[1], l.box[2], l.box[3]) else null
            if (l.url != null) { out += LinkEnd(l.id, a, l.ax, l.ay, -1, 0f, 0f, box, l.url); continue }
            val b = index[l.bPage] ?: continue
            out += LinkEnd(l.id, a, l.ax, l.ay, b, l.bx, l.by, box)
            // Old point links (v6.0) keep a badge at both ends.
            if (box == null) out += LinkEnd(l.id, b, l.bx, l.by, a, l.ax, l.ay)
        }
        return out
    }

    // --------------------------------------------------------------- search

    fun search(query: String) {
        searchQuery = query
        searchJob?.cancel()
        if (query.isBlank()) { searchHits = emptyList(); searchAiAnswer = null; activeHit = -1; return }
        searchJob = viewModelScope.launch {
            delay(250)
            searching = true
            val hits = ArrayList<SearchHit>()
            val corpusSb = StringBuilder()
            if (isPdf) {
                val index = textIndex()
                val map = pages.withIndex().filter { it.value.pdfPage >= 0 }.associate { it.value.pdfPage to it.index }
                if (index != null) hits += withContext(Dispatchers.Default) { index.search(query) { map[it] } }
            }
            // Typed text, sticky notes, web links, and mind map nodes are searchable.
            pages.forEachIndexed { i, p ->
                if (isPdf && p.pdfPage >= 0) {
                    val pageTxt = indexFor(p.pdfPage)?.pageText(p.pdfPage).orEmpty()
                    if (pageTxt.isNotBlank()) corpusSb.append(pageTxt.take(1200)).append('\n')
                }
                p.items.forEach { item ->
                    val text = when (item) {
                        is TextItem -> item.text
                        is StickyItem -> listOf(item.title, item.text).filter { it.isNotBlank() }.joinToString(" · ")
                        is WebLinkItem -> listOf(item.title, item.url, item.note).filter { it.isNotBlank() }.joinToString(" · ")
                        is MindNodeItem -> listOf(item.label, item.tag, item.notes).filter { it.isNotBlank() }.joinToString(" · ")
                        else -> ""
                    }
                    if (text.isNotEmpty()) {
                        corpusSb.append(text).append('\n')
                        if (text.contains(query, ignoreCase = true)) hits += SearchHit(i, listOf(item.bounds), text.take(64))
                    }
                }
            }
            val aiRes = if (query.trim().length >= 3) {
                LocalAiManager.semanticSearch(getApplication(), query, corpusSb.toString())
            } else SearchAiResult()
            searchAiAnswer = aiRes.takeIf { it.answer.isNotBlank() || it.expandedTerms.isNotEmpty() }

            // Semantic concept matches from downloaded local model / Nano / Custom AI
            if (aiRes.expandedTerms.isNotEmpty()) {
                val seenPages = hits.map { it.page to it.snippet }.toHashSet()
                pages.forEachIndexed { i, p ->
                    p.items.forEach { item ->
                        val text = when (item) {
                            is TextItem -> item.text
                            is StickyItem -> listOf(item.title, item.text).filter { it.isNotBlank() }.joinToString(" · ")
                            is WebLinkItem -> listOf(item.title, item.note).filter { it.isNotBlank() }.joinToString(" · ")
                            is MindNodeItem -> listOf(item.label, item.tag, item.notes).filter { it.isNotBlank() }.joinToString(" · ")
                            else -> ""
                        }
                        if (text.isNotEmpty() && !text.contains(query, ignoreCase = true)) {
                            val term = aiRes.expandedTerms.firstOrNull { text.contains(it, ignoreCase = true) }
                            if (term != null && seenPages.add(i to text.take(64))) {
                                hits += SearchHit(i, listOf(item.bounds), "AI · $term: ${text.take(52)}")
                            }
                        }
                    }
                }
            }

            searchHits = hits.sortedBy { it.page }
            activeHit = if (hits.isEmpty()) -1 else 0
            searching = false
            searchHits.getOrNull(0)?.let { onScrollTo?.invoke(it.page) }
            onPagesChanged?.invoke()
        }
    }

    /** PDF text with character boxes, built once on first use. */
    private suspend fun textIndex(): PdfTextIndex? {
        if (!isPdf) return null
        pdfIndex?.let { return it }
        val widths = pages.filter { it.pdfPage >= 0 }.associate { it.pdfPage to it.width }
        return withContext(Dispatchers.IO) {
            runCatching { PdfTextIndex.build(getApplication(), repo.pdfFile(noteId)) { widths[it] } }.getOrNull()
        }.also { pdfIndex = it }
    }

    fun stepHit(delta: Int) {
        if (searchHits.isEmpty()) return
        activeHit = (activeHit + delta).mod(searchHits.size)
        onScrollTo?.invoke(searchHits[activeHit].page)
        onPagesChanged?.invoke()
    }

    fun jumpToHit(i: Int) {
        activeHit = i
        searchHits.getOrNull(i)?.let { onScrollTo?.invoke(it.page) }
        onPagesChanged?.invoke()
    }

    fun clearSearch() {
        searchJob?.cancel()
        searchQuery = ""
        searchHits = emptyList()
        searchAiAnswer = null
        activeHit = -1
        onPagesChanged?.invoke()
    }

    fun highlights(page: Int): List<RectF> = if (searchHits.isEmpty()) emptyList() else searchHits.filter { it.page == page }.flatMap { it.rects }
    fun activeHighlight(): Pair<Int, RectF>? = searchHits.getOrNull(activeHit)?.let { h -> h.rects.firstOrNull()?.let { h.page to it } }

    // ----------------------------------------------------------- study & selection feedback
    /** Short message shown at the bottom of the editor (null = none). */
    var toast by mutableStateOf<String?>(null)
    private fun say(msg: String) {
        toast = msg
        viewModelScope.launch { delay(2600); if (toast == msg) toast = null }
    }
    var aiBusy by mutableStateOf(false)
        private set
    var aiStage by mutableStateOf("")
        private set

    /** Lasso -> save the selection as a picture in Photos. */
    fun saveSelectionScreenshot() {
        viewModelScope.launch {
            val bmp = selectionImage() ?: return@launch
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val cv = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "ProNotes-${System.currentTimeMillis()}.png")
                        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                        if (android.os.Build.VERSION.SDK_INT >= 29) put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Pro Notes")
                    }
                    val cr = getApplication<Application>().contentResolver
                    val uri = cr.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)!!
                    cr.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }.isSuccess
            }
            say(if (ok) "Saved to Photos" else "Couldn't save the picture")
        }
    }

    // ---------------------------------------------------------------- PDF text selection
    data class TextSel(val page: Int, val pdfPage: Int, val anchor: IntRange, val range: IntRange, val rects: List<RectF>, val text: String)
    var textSel by mutableStateOf<TextSel?>(null)
        private set
    var textMenu by mutableStateOf<RectF?>(null)

    /** Text of single PDF pages, read on demand so long-press works at once even on huge PDFs. */
    private val pageTextIdx = java.util.concurrent.ConcurrentHashMap<Int, PdfTextIndex>()
    private var pageTextJob: Job? = null

    private fun indexFor(pdfPage: Int): PdfTextIndex? = pdfIndex ?: pageTextIdx[pdfPage]

    fun textPress(page: Int, x: Float, y: Float): Boolean {
        val pg = pages.getOrNull(page) ?: return false
        if (pg.pdfPage < 0) return false
        val idx = indexFor(pg.pdfPage)
        if (idx == null) {
            // Read just this page, then finish the long-press.
            if (pageTextJob?.isActive != true) pageTextJob = viewModelScope.launch {
                aiBusy = true
                val one = withContext(Dispatchers.IO) {
                    runCatching { PdfTextIndex.build(getApplication(), repo.pdfFile(noteId), only = setOf(pg.pdfPage)) { if (it == pg.pdfPage) pg.width else null } }.getOrNull()
                }
                aiBusy = false
                if (one == null) { say("This PDF has no selectable text (it may be a scan)"); return@launch }
                pageTextIdx[pg.pdfPage] = one
                if (!textPress(page, x, y)) say("No text here. Long-press on a word.")
                else textMenu = textSel?.let { s -> s.rects.takeIf { it.isNotEmpty() }?.let { rs -> val u = RectF(rs.first()); rs.drop(1).forEach { u.union(it) }; toScreen?.invoke(s.page, u) } }
            }
            return false
        }
        val c = idx.charAt(pg.pdfPage, x, y) ?: return false
        val w = idx.wordAt(pg.pdfPage, c)
        textSel = TextSel(page, pg.pdfPage, w, w, idx.rects(pg.pdfPage, w), idx.text(pg.pdfPage, w))
        textMenu = null
        return true
    }

    /** Maps a page-space box to the screen (set by the editor; used to place the text menu). */
    var toScreen: ((Int, RectF) -> RectF?)? = null

    fun textDrag(x: Float, y: Float) {
        val s = textSel ?: return
        val idx = indexFor(s.pdfPage) ?: return
        val c = idx.charAt(s.pdfPage, x, y) ?: return
        val w = idx.wordAt(s.pdfPage, c)
        val r = minOf(s.anchor.first, w.first)..maxOf(s.anchor.last, w.last)
        if (r == s.range) return
        textSel = s.copy(range = r, rects = idx.rects(s.pdfPage, r), text = idx.text(s.pdfPage, r))
    }

    fun textEnd(screen: RectF?) { textMenu = screen }
    fun clearTextSel() { textSel = null; textMenu = null }
    fun textSelection(page: Int): List<RectF> = textSel?.takeIf { it.page == page }?.rects ?: emptyList()

    fun highlightTextSel(colorPick: Int? = null) {
        val s = textSel ?: return
        val p = pages.getOrNull(s.page) ?: return
        val color = colorPick ?: highlighterStyle.color
        val items = s.rects.map { r ->
            val brush = Brushes.create(Brushes.HIGHLIGHTER, color, r.height() * 1.05f)
            val pts = (0..12).map { k -> (r.left + r.width() * k / 12f) to r.centerY() }
            InkItem(ItemIds.next(), InkCodec.strokeFromPoints(pts, brush))
        }
        beginEdit(s.page)
        applyItems(s.page, p.items + items)
        clearTextSel()
    }

    /** Thin pen line under (or through, with [strike]) the selected PDF text. */
    fun lineTextSel(strike: Boolean) {
        val s = textSel ?: return
        val p = pages.getOrNull(s.page) ?: return
        val items = s.rects.map { r ->
            val y = if (strike) r.centerY() else r.bottom - r.height() * 0.08f
            val brush = Brushes.create(Brushes.MARKER, if (strike) 0xFFE5484D.toInt() else penStyle.color, (r.height() * 0.08f).coerceIn(2f, 6f))
            InkItem(ItemIds.next(), InkCodec.strokeFromPoints((0..10).map { k -> (r.left + r.width() * k / 10f) to y }, brush))
        }
        beginEdit(s.page)
        applyItems(s.page, p.items + items)
        clearTextSel()
    }

    /** Explanation of the selected text, from on-device model when available. */
    var explanation by mutableStateOf<Pair<String, String>?>(null)

    fun explainTextSel() {
        val s = textSel ?: return
        clearTextSel()
        viewModelScope.launch {
            aiStage = "Analyzing selected concept…"
            aiBusy = true
            val ctx = try { indexFor(s.pdfPage)?.pageText(s.pdfPage) ?: "" } finally { }
            val ans = try {
                runCatching { Nano.explain(s.text, ctx) }.getOrNull()
                    ?: LocalAiManager.explain(getApplication(), s.text, ctx)
            } finally {
                aiBusy = false
                aiStage = ""
            }
            explanation = s.text to ans
        }
    }

    /** Explains whatever items/strokes/text are currently selected by the Lasso tool using AI. */
    fun explainLassoSelection() {
        val pgIdx = selectionPage
        val p = pages.getOrNull(pgIdx) ?: return
        val selectedItems = p.items.filter { it.id in selection }
        if (selectedItems.isEmpty()) return

        val extractedParts = mutableListOf<String>()
        selectedItems.forEach { item ->
            when (item) {
                is TextItem -> if (item.text.isNotBlank()) extractedParts += item.text.trim()
                is StickyItem -> {
                    val t = listOfNotNull(item.title.takeIf { it.isNotBlank() }, item.text.takeIf { it.isNotBlank() }).joinToString(": ")
                    if (t.isNotBlank()) extractedParts += t
                }
                is WebLinkItem -> {
                    val linkTxt = listOf(item.title, item.url, item.note).filter { it.isNotBlank() }.joinToString(" · ")
                    if (linkTxt.isNotBlank()) extractedParts += linkTxt
                }
                is MindNodeItem -> {
                    val mindTxt = listOf(item.tag, item.label, item.notes).filter { it.isNotBlank() }.joinToString(" — ")
                    if (mindTxt.isNotBlank()) extractedParts += mindTxt
                }
                else -> Unit
            }
        }

        viewModelScope.launch {
            aiStage = "Explaining selection…"
            aiBusy = true
            val pageCtx = extractPageSummaryText(pgIdx).ifBlank {
                p.pdfPage?.let { pdfPg -> indexFor(pdfPg)?.pageText(pdfPg) }.orEmpty()
            }
            val selectedText = if (extractedParts.isNotEmpty()) {
                extractedParts.joinToString("\n")
            } else {
                val nearbyText = p.items.filterIsInstance<TextItem>().map { it.text.trim() }.filter { it.isNotBlank() }.take(3)
                val headings = pageCtx.lines().map { it.trim() }.filter { it.length >= 4 }.take(2)
                (nearbyText + headings).firstOrNull()?.take(80) ?: title.ifBlank { "Study Concept" }
            }
            val ans = try {
                runCatching { Nano.explain(selectedText, pageCtx) }.getOrNull()
                    ?: LocalAiManager.explain(getApplication(), selectedText, pageCtx)
            } finally {
                aiBusy = false
                aiStage = ""
            }
            explanation = selectedText to ans
        }
    }

    /** Explains the current page's concepts using AI. */
    fun explainCurrentPage() {
        val pgIdx = pageIndex
        viewModelScope.launch {
            aiStage = "Explaining Page ${pgIdx + 1}…"
            aiBusy = true
            val pageCtx = extractPageSummaryText(pgIdx).ifBlank {
                pages.getOrNull(pgIdx)?.pdfPage?.let { pdfPg -> indexFor(pdfPg)?.pageText(pdfPg) }.orEmpty()
            }.ifBlank { "Notebook \"$title\" · Page ${pgIdx + 1}" }
            val ans = try {
                LocalAiManager.explain(getApplication(), pageCtx.take(600), pageCtx)
            } finally {
                aiBusy = false
                aiStage = ""
            }
            explanation = "Page ${pgIdx + 1} · $title" to ans
        }
    }

    /** Questions made from just the selected text. */
    fun quizFromTextSel() {
        val s = textSel ?: return
        val pid = pages.getOrNull(s.page)?.id
        clearTextSel()
        viewModelScope.launch {
            aiBusy = true
            val ctx = indexFor(s.pdfPage)?.pageText(s.pdfPage) ?: s.text
            val made = try {
                runCatching { Nano.quiz(s.text, 3) }.getOrNull() ?: withContext(Dispatchers.Default) { StudyGen.quiz(s.text + "\n" + ctx, 3) }
            } finally { aiBusy = false }
            val add = made.map { com.pronotes.app.data.QuizQuestion(UUID.randomUUID().toString(), it.question, it.options, it.answer, it.explanation, pid, generated = true) }
            quiz = quiz + add
            saveStudy()
            say(if (add.isEmpty()) "Select a longer passage to make questions" else "Added ${add.size} quiz questions")
        }
    }

    fun cardFromTextSel() {
        val s = textSel ?: return
        cardDraft = CardDraft(com.pronotes.app.data.Flashcard(UUID.randomUUID().toString(), front = s.text, pageId = pages.getOrNull(s.page)?.id, created = System.currentTimeMillis()), true)
        clearTextSel()
    }

    // ---------------------------------------------------------------- quiz + generation
    var quiz by mutableStateOf<List<com.pronotes.app.data.QuizQuestion>>(emptyList())
        private set

    fun saveQuestion(q: com.pronotes.app.data.QuizQuestion) {
        quiz = if (quiz.any { it.id == q.id }) quiz.map { if (it.id == q.id) q else it } else quiz + q
        saveStudy()
    }
    fun deleteQuestion(id: String) { quiz = quiz.filterNot { it.id == id }; saveStudy() }
    fun answerQuestion(id: String, right: Boolean) {
        quiz = quiz.map { if (it.id == id) (if (right) it.copy(right = it.right + 1) else it.copy(wrong = it.wrong + 1)) else it }
        saveStudy()
    }

    /** Text to learn from: PDF text, typed text, sticky notes, web links, and mind map nodes. */
    private suspend fun studyText(range: IntRange): Pair<String, String?> {
        val idx = textIndex()
        val sb = StringBuilder()
        var firstPage: String? = null
        for (i in range) {
            val p = pages.getOrNull(i) ?: continue
            val before = sb.length
            if (p.pdfPage >= 0 && idx != null) sb.append(idx.pageText(p.pdfPage)).append('\n')
            p.items.forEach {
                (it as? TextItem)?.text?.let { t -> sb.append(t).append(". ") }
                (it as? StickyItem)?.let { st ->
                    if (st.title.isNotBlank()) sb.append(st.title).append(": ")
                    sb.append(st.text).append(". ")
                }
                (it as? WebLinkItem)?.let { wl ->
                    if (wl.title.isNotBlank()) sb.append(wl.title).append(". ")
                    if (wl.note.isNotBlank()) sb.append(wl.note).append(". ")
                }
                (it as? MindNodeItem)?.let { m ->
                    sb.append(m.label).append(". ")
                    if (m.notes.isNotBlank()) sb.append(m.notes).append(". ")
                }
            }
            if (sb.length > before && firstPage == null) firstPage = p.id
        }
        return sb.toString() to firstPage
    }

    /** Makes flashcards from this page (or all pages). Returns how many were added. */
    fun generateCards(allPages: Boolean) {
        viewModelScope.launch {
            aiStage = "Reading page notes…"
            aiBusy = true
            val (text, pid) = studyText(if (allPages) pages.indices else pageIndex..pageIndex)
            if (text.isBlank()) {
                aiBusy = false; aiStage = ""
                say("No text found. Write or import something first."); return@launch
            }
            val max = if (allPages) 40 else 15
            aiStage = "Generating flashcards with ${aiEngineLabel}…"
            val made = try {
                runCatching { Nano.flashcards(text, max) }.getOrNull()
                    ?: LocalAiManager.generateFlashcards(getApplication(), text, max)
            } finally {
                aiBusy = false
                aiStage = ""
            }
            val have = cards.map { it.front.lowercase() }.toHashSet()
            val add = made.filter { it.front.lowercase() !in have }.map {
                com.pronotes.app.data.Flashcard(UUID.randomUUID().toString(), front = it.front, back = it.back, pageId = pid, created = System.currentTimeMillis())
            }
            cards = cards + add
            saveStudy()
            say(if (add.isEmpty()) "No new cards found on ${if (allPages) "these pages" else "this page"}" else "Added ${add.size} cards")
        }
    }

    /** Structured AI summary of this page, shown in Summary Studio; null = hidden. */
    var summary by mutableStateOf<RichSummaryResult?>(null)

    fun summarize() {
        viewModelScope.launch {
            aiStage = "Reading page notes…"
            aiBusy = true
            val res = try {
                val (text, _) = studyText(pageIndex..pageIndex)
                if (text.isBlank()) null
                else {
                    val nanoPts = runCatching { Nano.summary(text) }.getOrNull()
                    LocalAiManager.summarizeRich(getApplication(), text, nanoPts)
                }
            } finally {
                aiBusy = false
                aiStage = ""
            }
            if (res == null || (res.points.isEmpty() && res.definitions.isEmpty())) {
                say("Not enough text on this page to summarise")
            } else {
                summary = res
            }
        }
    }

    /** Puts the key points on the page as typed text, top left. */
    fun summaryToPage() {
        val res = summary ?: return
        val p = pages.getOrNull(pageIndex) ?: return
        summary = null
        beginEdit(pageIndex)
        val body = buildString {
            appendLine(res.headline)
            res.points.forEach { appendLine("• $it") }
        }.trim()
        applyItems(pageIndex, p.items + TextItem(ItemIds.next(), body, 60f, 60f, 26f, penStyle.color))
    }

    /** Places the summary as a colorful sticky note on the active page. */
    fun summaryToSticky() {
        val res = summary ?: return
        val p = pages.getOrNull(pageIndex) ?: return
        summary = null
        val text = buildString {
            appendLine(res.headline)
            res.points.take(4).forEach { appendLine("• $it") }
        }.trim()
        beginEdit(pageIndex)
        applyItems(
            pageIndex,
            p.items + StickyItem(ItemIds.next(), 80f, 90f, 340f, 320f, stickyColor, text, fontSize = 24f),
        )
        say("Added summary as sticky note")
    }

    /** Clips text (e.g. from Split Notes) directly onto the active page as a sticky note. */
    fun clipTextAsSticky(rawText: String, sourceTitle: String = "") {
        val p = currentPage ?: return
        val clean = rawText.trim().take(360)
        if (clean.isBlank()) { say("Nothing to clip on this split page"); return }
        val body = if (sourceTitle.isNotBlank()) "[$sourceTitle]\n$clean" else clean
        beginEdit(pageIndex)
        applyItems(
            pageIndex,
            p.items + StickyItem(ItemIds.next(), (p.width - 320f).coerceAtLeast(60f) / 2f, 120f, 320f, 280f, stickyColor, body, fontSize = 24f),
        )
        say("Clipped to sticky note")
    }

    /** Inserts text (e.g. from Split Notes or import) onto the active page as a formatted text block. */
    fun clipTextAsPageItem(rawText: String, sourceTitle: String = "") {
        val p = currentPage ?: return
        val clean = rawText.trim().take(900)
        if (clean.isBlank()) { say("Nothing to insert on this page"); return }
        val body = if (sourceTitle.isNotBlank()) "$sourceTitle\n$clean" else clean
        beginEdit(pageIndex)
        applyItems(pageIndex, p.items + TextItem(ItemIds.next(), body, 80f, 120f, 26f, penStyle.color))
        say("Inserted onto page")
    }

    /** Imports PDFs, images, Markdown/TXT/HTML/CSV/JSON files as new pages inside this notebook. */
    fun importFilesIntoCurrent(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val built = repo.buildPagesFromUris(noteId, uris)
            if (built.isEmpty()) { say("Could not read any pages from those files"); return@launch }
            val insertAt = (pageIndex + 1).coerceIn(0, pages.size)
            pages.addAll(insertAt, built)
            undoStack.clear(); redoStack.clear(); refreshUndo()
            scheduleSave(); onPagesChanged?.invoke()
            onScrollTo?.invoke(insertAt)
            say("Imported ${built.size} page${if (built.size == 1) "" else "s"}")
        }
    }

    fun generateQuiz(allPages: Boolean) {
        viewModelScope.launch {
            aiStage = "Scanning notes for quiz concepts…"
            aiBusy = true
            val (text, pid) = studyText(if (allPages) pages.indices else pageIndex..pageIndex)
            val max = if (allPages) 30 else 12
            aiStage = "Drafting quiz questions…"
            val made = try {
                runCatching { Nano.quiz(text, max) }.getOrNull()
                    ?: LocalAiManager.generateQuiz(getApplication(), text, max)
            } finally {
                aiBusy = false
                aiStage = ""
            }
            val have = quiz.map { it.question }.toHashSet()
            val add = made.filter { it.question !in have }.map {
                com.pronotes.app.data.QuizQuestion(UUID.randomUUID().toString(), it.question, it.options, it.answer, it.explanation, pid, generated = true)
            }
            quiz = quiz + add
            saveStudy()
            say(if (add.isEmpty()) "Not enough text to make questions here" else "Added ${add.size} questions")
        }
    }

    // ---------------------------------------------------------------- Slide Show Chapter Revision

    fun generateSlideShow(startPage: Int = pageIndex, endPage: Int = pageIndex, slideCount: Int = 7) {
        val lo = startPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        val hi = endPage.coerceIn(lo, (pages.size - 1).coerceAtLeast(0))
        viewModelScope.launch {
            aiStage = "Building revision slide deck…"
            aiBusy = true
            val (text, _) = studyText(lo..hi)
            val chapterLabel = if (lo == 0 && hi == pages.size - 1) title else if (lo == hi) "$title · Page ${lo + 1}" else "$title · Pages ${lo + 1}-${hi + 1}"
            val (made, engine) = try {
                LocalAiManager.generateSlideShow(getApplication(), chapterLabel, text, slideCount)
            } finally {
                aiBusy = false
                aiStage = ""
            }
            slides = made
            saveStudy()
            say("Generated ${made.size} revision slides via $engine")
        }
    }

    fun saveSlide(card: com.pronotes.app.data.SlideCard) {
        slides = if (slides.any { it.id == card.id }) slides.map { if (it.id == card.id) card else it } else slides + card
        saveStudy()
    }

    fun deleteSlide(id: String) {
        slides = slides.filterNot { it.id == id }
        saveStudy()
    }

    fun moveSlide(from: Int, to: Int) {
        if (from !in slides.indices || to !in slides.indices || from == to) return
        val mutable = slides.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        slides = mutable
        saveStudy()
    }

    // --------------------------------------------------------------- v10 AI & collab helpers

    fun noteDirFile(): File = repo.noteDir(noteId)

    suspend fun extractPagesCorpus(): List<Pair<Int, String>> {
        val idx = textIndex()
        val out = ArrayList<Pair<Int, String>>()
        pages.forEachIndexed { i, p ->
            val sb = StringBuilder()
            if (p.pdfPage >= 0 && idx != null) {
                val pdfTxt = idx.pageText(p.pdfPage)
                if (pdfTxt.isNotBlank()) sb.append(pdfTxt).append('\n')
            }
            p.items.forEach { item ->
                when (item) {
                    is TextItem -> if (item.text.isNotBlank()) sb.append(item.text).append('\n')
                    is StickyItem -> {
                        val txt = listOf(item.title, item.text).filter { it.isNotBlank() }.joinToString(": ")
                        if (txt.isNotBlank()) sb.append(txt).append('\n')
                    }
                    is WebLinkItem -> {
                        val txt = listOf(item.title, item.url, item.note).filter { it.isNotBlank() }.joinToString(" · ")
                        if (txt.isNotBlank()) sb.append(txt).append('\n')
                    }
                    is MindNodeItem -> {
                        val txt = listOf(item.tag, item.label, item.notes).filter { it.isNotBlank() }.joinToString(" — ")
                        if (txt.isNotBlank()) sb.append(txt).append('\n')
                    }
                    else -> Unit
                }
            }
            val clean = sb.toString().trim()
            if (clean.isNotEmpty()) out += i to clean
        }
        return out
    }

    fun extractPageSummaryText(pageIdx: Int): String {
        val p = pages.getOrNull(pageIdx) ?: return ""
        return p.items.mapNotNull { item ->
            when (item) {
                is TextItem -> item.text
                is StickyItem -> listOf(item.title, item.text).filter { it.isNotBlank() }.joinToString(": ")
                is WebLinkItem -> listOf(item.title, item.url, item.note).filter { it.isNotBlank() }.joinToString(" · ")
                is MindNodeItem -> listOf(item.tag, item.label, item.notes).filter { it.isNotBlank() }.joinToString(" — ")
                else -> null
            }
        }.joinToString("\n").trim()
    }

    fun insertAiTextToPage(text: String) {
        val clean = text.trim().take(2400)
        if (clean.isBlank()) return
        val idx = pageIndex
        val p = pages.getOrNull(idx) ?: return
        beginEdit(idx)
        applyItems(idx, p.items + TextItem(ItemIds.next(), clean, 72f, 96f, 26f, penStyle.color))
        say("Inserted onto page ${idx + 1}")
    }

    fun applyGeneratedTemplateBitmap(bmp: Bitmap) {
        val idx = pageIndex
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) {
                val n = "bg_ai_${System.currentTimeMillis()}.png"
                File(repo.noteDir(noteId), n).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 96, it) }
                n
            }
            val p = pages.getOrNull(idx) ?: return@launch
            pages[idx] = p.copy(bgImage = name)
            thumbs.evictAll()
            scheduleSave()
            onPagesChanged?.invoke()
            say("Applied AI template")
        }
    }

    fun applyGeneratedCoverBitmap(bmp: Bitmap) {
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) {
                val n = "cover_ai_${System.currentTimeMillis()}.png"
                File(repo.noteDir(noteId), n).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 96, it) }
                n
            }
            val spec = "image:$name"
            val hasCoverPage = repo.setCoverPage(noteId, spec)
            if (hasCoverPage && pages.isNotEmpty() && pages[0].cover != null) {
                pages[0] = pages[0].copy(cover = spec)
                scheduleSave()
            } else if (!hasCoverPage) {
                repo.setCover(noteId, spec)
            }
            thumbs.evictAll()
            onPagesChanged?.invoke()
            say("Updated notebook cover")
        }
    }

    fun applySplitItems(page: Int, items: List<PageItem>) {
        val p = pages.getOrNull(page) ?: return
        pages[page] = p.copy(items = items)
        thumbs.evictAll()
        scheduleSave()
        onPagesChanged?.invoke()
    }

    // --------------------------------------------------------------- save

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1200)
            repo.savePages(noteId, pages.toList())
        }
    }

    fun saveNow() {
        if (!loaded) return
        saveJob?.cancel()
        val snapshot = pages.toList()
        repo.appScope.launch { repo.savePages(noteId, snapshot) }
    }

    suspend fun exportPdf(out: OutputStream) = repo.exportPdf(noteId, pages.toList(), out)

    override fun onCleared() {
        if (saveJob?.isActive == true) saveNow()
        pdf?.close()
        thumbPdfInstance?.close()
        super.onCleared()
    }
}

/** One end of an ink link: badge on [page] at (x, y); tapping it goes to [toPage] at (tx, ty). */
data class LinkEnd(
    val id: String, val page: Int, val x: Float, val y: Float, val toPage: Int, val tx: Float, val ty: Float,
    /** Linked content box (page space): tap inside to follow. */
    val box: RectF? = null,
    /** Website, when the link leaves the notebook. */
    val url: String? = null,
)

