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
import com.pronotes.app.data.AtomicFiles
import com.pronotes.app.data.MemoryTrimRegistry
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
import java.util.Collections
import java.util.UUID

data class ToolStyle(val colors: List<Int>, val sizes: List<Float>, val color: Int, val size: Float)

val PenPalette = listOf(
    0xFF1E2321, 0xFF2E4A7A, 0xFF2F6B4F, 0xFF7A2E42, 0xFFB98A2E, 0xFF24483F,
    0xFF5A5F58, 0xFF3D6CF4, 0xFF0A84FF, 0xFF34C759, 0xFF00897B, 0xFFFF9500,
    0xFFEB5757, 0xFF9B51E0, 0xFF6D4C41, 0xFFFFFFFF, 0xFF000000,
).map { it.toInt() }

val HighlighterPalette = listOf(0xFFF3D77A, 0xFFA5D3C0, 0xFF9AD0EC, 0xFFFFA8A8, 0xFFE599F7).map { it.toInt() }
val CrayonPalette = listOf(0xFFE63946, 0xFFF77F00, 0xFFFCBF49, 0xFF2A9D8F, 0xFF3A86FF, 0xFF8338EC, 0xFF3D405B).map { it.toInt() }
val TapeColors = listOf(0xFF24483F, 0xFF2E4A7A, 0xFF7A2E42, 0xFFB98A2E, 0xFF8E9189).map { it.toInt() }
val StickyColors = listOf(0xFFF4EEDF, 0xFFFFF3A3, 0xFFD8F5C8, 0xFFCDEBFF, 0xFFFFD6E0).map { it.toInt() }
val WebLinkColors = listOf(0xFF24483F, 0xFF2E4A7A, 0xFF2F6B4F, 0xFF7A2E42, 0xFFB98A2E, 0xFF3D6CF4).map { it.toInt() }
val MindPalette = listOf(0xFF24483F, 0xFF2E4A7A, 0xFF2F6B4F, 0xFF7A2E42, 0xFFB98A2E, 0xFF35655A, 0xFF5A5F58).map { it.toInt() }

class EditorViewModel(app: Application, val noteId: String) : AndroidViewModel(app) {
    internal val repo = Repository.get(app)
    internal val store = SettingsStore.get(app)
    internal val notebookSettings = com.pronotes.app.data.NotebookSettings(app, noteId, viewModelScope)
    val settingsFlow = notebookSettings.settings

    val pages = mutableStateListOf<Page>()
    var pageIndex by mutableStateOf(0)
        private set
    var loaded by mutableStateOf(false)
        private set
    var title by mutableStateOf(repo.meta(noteId)?.title ?: "Notebook")
        private set
    val isPdf: Boolean get() = repo.pdfFile(noteId).exists()

    // ---------------------------------------------------------------- tools
    /** Last session's choices: tool, pen type, colours, sizes. */
    private val saved = store.pref("tools_$noteId", store.pref("tools")).split('|')
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
    var crayonStyle by mutableStateOf(style("crayon", CrayonPalette.take(6), listOf(10f, 18f, 28f), 16f, 28))
    var highlighterStyle by mutableStateOf(style("highlighter", HighlighterPalette, listOf(16f, 28f, 42f), 22f, 3))
    var shapeStyle by mutableStateOf(style("shape", PenPalette.take(5), listOf(2f, 4f, 7f), 3.6f, 4))
    var shapeKind by mutableStateOf(savedEnum(5, ShapeKind.AUTO))
    var shapeDashed by mutableStateOf(saved.getOrNull(12).toBoolean())
    /** Bumped when the selected image or sticker tool is tapped again. */
    var reopenTool by mutableStateOf(0)
    var shapeFillOn by mutableStateOf(saved.getOrNull(13).toBoolean())
    var shapeFillColor by mutableStateOf(saved.getOrNull(14)?.toIntOrNull() ?: 0xFFCDE2FB.toInt())
    var textStyle by mutableStateOf(TextStyleSpec(font = savedEnum(20, TextFont.SANS),
        bold = saved.getOrNull(21).toBoolean(), italic = saved.getOrNull(22).toBoolean(),
        underline = saved.getOrNull(23).toBoolean(), gradient = saved.getOrNull(24)?.toIntOrNull(),
        align = savedEnum(25, TextAlign.LEFT)))
    var textColor by mutableStateOf(saved.getOrNull(9)?.toIntOrNull() ?: 0xFF202124.toInt())

    var circleChip by mutableStateOf<RectF?>(null)
        private set
    private var chipJob: Job? = null
    var zoomPercent by mutableStateOf<Int?>(null)
        private set
    private var clipboard: List<PageItem> = emptyList()
    var hasClipboard by mutableStateOf(false)
        private set
    var eraserSize by mutableStateOf(saved.getOrNull(6)?.toIntOrNull()?.coerceIn(0, 2) ?: 1)
    var eraserCustomDp by mutableStateOf(saved.getOrNull(10)?.toFloatOrNull())
    var textSize by mutableStateOf(saved.getOrNull(11)?.toFloatOrNull() ?: 40f)
    var tapeColor by mutableStateOf(saved.getOrNull(7)?.toIntOrNull() ?: TapeColors[0])
    var tapePattern by mutableStateOf(saved.getOrNull(16) ?: "stripes")
    var tapeFreehand by mutableStateOf(saved.getOrNull(15).toBoolean())
    var stickyColor by mutableStateOf(saved.getOrNull(8)?.toIntOrNull() ?: StickyColors[0])
    var webLinkColor by mutableStateOf(saved.getOrNull(26)?.toIntOrNull() ?: WebLinkColors[0])

    // ------------------------------------------------------------ selection
    var selection by mutableStateOf<Set<Long>>(emptySet())
        private set
    var selectionPage by mutableStateOf(-1)
        private set
    var selectionBounds by mutableStateOf<RectF?>(null)
        private set

    // --------------------------------------------------------------- dialogs
    data class TextEdit(
        val page: Int,
        val x: Float,
        val y: Float,
        val existing: TextItem?,
        val boxWidth: Float = 560f,
        val boxHeight: Float = 160f,
        val isFullPage: Boolean = false,
    )
    data class StickyEdit(val page: Int, val item: StickyItem, val isNew: Boolean)
    data class WebLinkEdit(val page: Int, val item: WebLinkItem, val isNew: Boolean)
    data class MindNodeEdit(val page: Int, val item: MindNodeItem, val isNew: Boolean)
    enum class MindLayout(val label: String) { RADIAL("Radial"), TREE("Tree"), ORG_CHART("Org") }
    var textEdit by mutableStateOf<TextEdit?>(null)
    var stickyEdit by mutableStateOf<StickyEdit?>(null)
    var webLinkEdit by mutableStateOf<WebLinkEdit?>(null)
    var mindNodeEdit by mutableStateOf<MindNodeEdit?>(null)
    var activeMindNodeId by mutableStateOf<Long?>(null)
    var mindColor by mutableStateOf(saved.getOrNull(27)?.toIntOrNull() ?: MindPalette[0])
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
    var activeHit by mutableStateOf(-1)
        private set
    var searching by mutableStateOf(false)
        private set
    internal var pdfIndex: PdfTextIndex? = null
    private var searchJob: Job? = null

    internal val undoStack = ArrayDeque<Pair<Int, List<PageItem>>>()
    internal val redoStack = ArrayDeque<Pair<Int, List<PageItem>>>()
    private var saveJob: Job? = null
    internal val dirtyPageIds = Collections.synchronizedSet(HashSet<String>())
    @Volatile internal var structureDirty = false
    private var pdf: PdfPages? = null
    private val pendingPdf = HashSet<Int>()
    /** When each PDF page was last asked for by the canvas, and the width it wants now. */
    private val pdfSeen = java.util.concurrent.ConcurrentHashMap<Int, Long>()
    private val pdfWant = java.util.concurrent.ConcurrentHashMap<Int, Int>()

    /** Set by the screen so model changes can be pushed into the canvas. */
    var onPagesChanged: (() -> Unit)? = null
    var onScrollTo: ((Int) -> Unit)? = null
    var onRedraw: (() -> Unit)? = null
    var toScreen: ((Int, RectF) -> RectF?)? = null

    /** Read-only: the notebook can be read and scrolled, not changed. Remembered per notebook. */
    var readOnly by mutableStateOf(store.pref("ro_$noteId") == "1")
        private set
    fun toggleReadOnly() { readOnly = !readOnly; store.setPref("ro_$noteId", if (readOnly) "1" else "0") }

    /** Last time the user wrote, erased or moved around (for the study timer's idle pause). */
    var lastActivity by mutableStateOf(System.currentTimeMillis())
    fun touched() { lastActivity = System.currentTimeMillis() }

    /** Where the study timer sits, as fractions of the canvas (shared by all notebooks). */
    fun timerPos(): Pair<Float, Float> {
        val p = store.pref("timerPos_$noteId", "0.86,0.92").split(',')
        return (p.getOrNull(0)?.toFloatOrNull() ?: 0.86f) to (p.getOrNull(1)?.toFloatOrNull() ?: 0.92f)
    }
    fun setTimerPos(x: Float, y: Float) = store.setPref("timerPos_$noteId", "$x,$y")

    /** Total study time in this notebook, in milliseconds (sessions add to it). */
    fun studyTotal(): Long = store.pref("study_$noteId", "0").toLongOrNull() ?: 0L
    fun addStudy(ms: Long) { if (ms > 0) store.setPref("study_$noteId", (studyTotal() + ms).toString()) }

    /** The page this notebook was on when it was last closed. */
    var startPage = 0
        private set

    init {
        MemoryTrimRegistry.register { level ->
            if (MemoryTrimRegistry.isCriticalLevel(level)) {
                thumbs.evictAll()
                details = emptyList()
                thumbRenderer?.evictCaches()
            } else {
                thumbs.trimToSize(thumbs.maxSize() / 2)
            }
        }
        // Remember tool choices as they change.
        viewModelScope.launch {
            androidx.compose.runtime.snapshotFlow { toolPreferences() }.collect { store.setPref("tools_$noteId", it) }
        }
        val initTool = store.pref("startTool_$noteId").takeIf { it.isNotBlank() }?.let { n ->
            store.setPref("startTool_$noteId", "")
            enumValues<Tool>().firstOrNull { it.name == n }
        }
        if (initTool != null) _tool = initTool
        viewModelScope.launch {
            val loadedPages = withContext(Dispatchers.IO) { repo.loadPages(noteId) }
            startPage = store.pref("page_$noteId", "0").toIntOrNull()?.coerceIn(0, (loadedPages.size - 1).coerceAtLeast(0)) ?: 0
            pageIndex = startPage
            pages.clear(); pages.addAll(loadedPages)
            if (repo.pdfFile(noteId).exists()) pdf = PdfPages(repo.pdfFile(noteId))
            loaded = true
            onPagesChanged?.invoke()

            // Secondary metadata & caches load in the background without delaying the canvas.
            launch(Dispatchers.IO) { loadStudy() }
            launch(Dispatchers.IO) { repo.markOpened(noteId) }
            prefetchPdf(startPage)
        }
    }

    private fun style(key: String, colors: List<Int>, sizes: List<Float>, size: Float, slot: Int): ToolStyle {
        val c = store.swatches("${key}_$noteId", store.swatches(key, colors))
        val sel = saved.getOrNull(slot)?.split(',')
        val color = if (key == "shape") penStyle.color else sel?.getOrNull(0)?.toIntOrNull() ?: c.first()
        val sz = sel?.getOrNull(1)?.toFloatOrNull() ?: size
        val widths = saved.getOrNull(slot + 15)?.split(',')?.mapNotNull { it.toFloatOrNull()?.takeIf { w -> w.isFinite() && w > 0f } }
        val palette = if (color in c) c else (c + color).takeLast(8)
        return ToolStyle(palette, widths?.takeIf { it.size == sizes.size } ?: sizes, color, sz)
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
        val maxSpan = store.settings.value.performanceMode.pdfPrefetchPages
        val offsets = when {
            maxSpan <= 1 -> intArrayOf(0, 1)
            maxSpan <= 3 -> intArrayOf(0, 1, -1, 2)
            else -> intArrayOf(0, 1, -1, 2, 3, -2, 4)
        }
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(90)
            for (d in offsets) {
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
        Tool.CRAYON -> crayonStyle
        Tool.HIGHLIGHTER -> highlighterStyle
        Tool.SHAPE -> shapeStyle
        else -> null
    }

    private fun shareInkColor(source: Tool, color: Int) {
        val target = if (source == Tool.PEN) Tool.SHAPE else Tool.PEN
        val current = styleFor(target) ?: return
        val palette = if (color in current.colors) current.colors else (current.colors + color).takeLast(8)
        val updated = current.copy(colors = palette, color = color)
        if (target == Tool.PEN) penStyle = updated else shapeStyle = updated
        val key = if (target == Tool.PEN) "pen" else "shape"
        store.saveSwatches("${key}_$noteId", palette)
        pushRecent(color)
    }

    fun updateStyle(t: Tool, s: ToolStyle) {
        if (styleFor(t)?.color != s.color) {
            if (t == Tool.PEN || t == Tool.SHAPE) shareInkColor(t, s.color)
            else if (t == Tool.HIGHLIGHTER || t == Tool.CRAYON) pushRecent(s.color)
        }
        when (t) {
            Tool.PEN -> { penStyle = s; store.saveSwatches("pen_$noteId", s.colors) }
            Tool.CRAYON -> { crayonStyle = s; store.saveSwatches("crayon_$noteId", s.colors) }
            Tool.HIGHLIGHTER -> { highlighterStyle = s; store.saveSwatches("highlighter_$noteId", s.colors) }
            Tool.SHAPE -> { shapeStyle = s; store.saveSwatches("shape_$noteId", s.colors) }
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
        Tool.PEN, Tool.AUTO_TEXT -> penStyle.color to penStyle.size * penType.sizeFactor
        Tool.CRAYON -> crayonStyle.color to crayonStyle.size
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

    internal fun selectionUnion(): RectF? {
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
        structureDirty = true
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
        updateStyle(Tool.PEN, penStyle.copy(colors = colors, color = p.color, size = p.size))
        tool = Tool.PEN
    }

    fun setPenName(name: String) = notebookSettings.update { it.copy(penName = name.trim().ifBlank { "Pro Stylus" }) }

    fun clearPageInk() {
        val p = currentPage ?: return
        if (p.items.none { it is InkItem }) return
        beginEdit(pageIndex)
        applyItems(pageIndex, p.items.filterNot { it is InkItem })
    }

    /** Places a sticker picture onto [targetPage] near ([targetX], [targetY]) or page centre and returns it for selection. */
    fun insertStickerBitmap(
        bmp: Bitmap,
        targetPage: Int = pageIndex,
        targetX: Float? = null,
        targetY: Float? = null,
        onPlaced: (Int, Long) -> Unit,
    ) {
        val td = settingsFlow.value.toolDefaults
        viewModelScope.launch {
            val processed = withContext(Dispatchers.Default) {
                if (td.stickerOpacity >= 0.99f) bmp
                else applyBitmapEffects(bmp, td.stickerOpacity, drawBorder = false)
            }
            val name = withContext(Dispatchers.IO) {
                val n = "sticker_${UUID.randomUUID()}.png"
                AtomicFiles.writeBytesAtomic(File(repo.noteDir(noteId), n)) { out ->
                    processed.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                n
            }
            val idx = targetPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
            val p = pages.getOrNull(idx) ?: return@launch
            val w = (300f * td.stickerScale.coerceIn(0.4f, 2f)).coerceIn(90f, p.width * 0.85f)
            val h = if (td.stickerLockAspect && bmp.width > 0) w * bmp.height / bmp.width else w
            val ix = ((targetX ?: (p.width / 2f)) - w / 2f).coerceIn(16f, (p.width - w - 16f).coerceAtLeast(16f))
            val iy = ((targetY ?: (p.height / 3f + h / 2f)) - h / 2f).coerceIn(16f, (p.height - h - 16f).coerceAtLeast(16f))
            val item = ImageItem(ItemIds.next(), name, ix, iy, w, h)
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

    /** Pastes clipboard items centred around ([x], [y]) on [page] for Quick Insert. */
    fun pasteAt(page: Int, x: Float, y: Float): Pair<Int, Set<Long>>? {
        if (clipboard.isEmpty()) return null
        val idx = page.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        val p = pages.getOrNull(idx) ?: return null
        val union = RectF(clipboard.first().bounds).apply {
            clipboard.drop(1).forEach { union(it.bounds) }
        }
        val dx = x - union.centerX()
        val dy = y - union.centerY()
        val copies = clipboard.map { it.translated(dx, dy).withId(ItemIds.next()) }
        beginEdit(idx)
        applyItems(idx, p.items + copies)
        return idx to copies.map { it.id }.toSet()
    }

    private fun applyBitmapEffects(src: Bitmap, opacity: Float, drawBorder: Boolean): Bitmap {
        val out = Bitmap.createBitmap(src.width.coerceAtLeast(1), src.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (opacity.coerceIn(0.2f, 1f) * 255f).toInt()
        }
        c.drawBitmap(src, 0f, 0f, p)
        if (drawBorder) {
            val strokeW = (maxOf(src.width, src.height) * 0.008f).coerceIn(2f, 8f)
            val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = strokeW
                color = (0x33000000).toInt()
            }
            c.drawRect(strokeW / 2f, strokeW / 2f, out.width - strokeW / 2f, out.height - strokeW / 2f, borderPaint)
        }
        return out
    }

    fun toggleBookmark() {
        val p = currentPage ?: return
        pages[pageIndex] = p.copy(bookmarked = !p.bookmarked)
        dirtyPageIds += p.id
        scheduleSave(); onPagesChanged?.invoke()
    }

    fun rename(newTitle: String) {
        if (newTitle.isBlank()) return
        title = newTitle
        viewModelScope.launch { repo.renameNotebook(noteId, newTitle) }
    }

    fun recentColors() = store.pref("recentColors_$noteId").split(',').mapNotNull { it.toIntOrNull() }
    fun pushRecent(c: Int) = store.setPref("recentColors_$noteId", (listOf(c) + recentColors()).distinct().take(12).joinToString(","))

    fun brushFor(t: Tool): Brush {
        val td = settingsFlow.value.toolDefaults
        return when (t) {
            Tool.CRAYON -> Brushes.create(
                Brushes.CRAYON,
                crayonStyle.color,
                crayonStyle.size,
                td.crayonOpacity,
            )
            Tool.HIGHLIGHTER -> {
                Brushes.highlightAlpha = (settingsFlow.value.highlighterOpacity.coerceIn(0.3f, 0.9f) * 255).toInt()
                Brushes.create(Brushes.HIGHLIGHTER, highlighterStyle.color, highlighterStyle.size)
            }
            Tool.SHAPE -> Brushes.create(
                if (shapeDashed) Brushes.DASHED else Brushes.PEN,
                shapeStyle.color,
                shapeStyle.size,
                td.shapeOpacity,
            )
            else -> Brushes.createCustomPen(
                baseFamily = penType.family,
                color = penStyle.color,
                size = penStyle.size * penType.sizeFactor,
                opacity = td.penOpacity,
                pressureEnabled = td.pressureEnabled,
                pressureSensitivity = td.pressureSensitivity,
                nibRoundness = td.penNibRoundness,
                nibAngleDeg = td.penNibAngleDeg,
                strokeStyle = td.penStrokeStyle,
            )
        }
    }

    fun eraserRadiusPx(density: Float): Float = (eraserCustomDp ?: listOf(8f, 16f, 30f)[eraserSize]) * density

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
        dirtyPageIds += p.id
        scheduleSave()
    }

    /** Pen is down: the toolbar fades so you can write under it. */
    var penActive by mutableStateOf(false)

    /** Pattern for new tape. */


    fun useTapeImage(uri: Uri) {
        viewModelScope.launch {
            val name = runCatching { repo.saveImage(noteId, uri) }.getOrNull()
            if (name == null) {
                say("Could not read image.")
                return@launch
            }
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
            0 -> {
                val p = currentPage ?: return
                pages[pageIndex] = styled(p)
                dirtyPageIds += p.id
            }
            1 -> {
                val base = styled(newPage())
                pages.add(pageIndex + 1, base)
                dirtyPageIds += base.id
                structureDirty = true
                undoStack.clear(); redoStack.clear(); refreshUndo()
                scheduleSave(); onPagesChanged?.invoke(); onScrollTo?.invoke(pageIndex + 1)
                return
            }
            else -> pages.indices.forEach { i ->
                val p = pages[i]
                if (p.pdfPage < 0 && p.cover == null && p.bgImage == null) {
                    pages[i] = styled(p)
                    dirtyPageIds += p.id
                }
            }
        }
        scheduleSave(); onPagesChanged?.invoke()
    }

    internal fun applyItems(page: Int, items: List<PageItem>) {
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
        dirtyPageIds += page.id
        refreshUndo(); scheduleSave(); onPagesChanged?.invoke()
        if (idx != pageIndex) onScrollTo?.invoke(idx)
    }

    internal fun refreshUndo() {
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

    /** Live ranked candidates from the latest Write Box or Selection handwriting recognition. */
    var handwritingCandidates by mutableStateOf<List<String>>(emptyList())
        private set
    var handwritingBusy by mutableStateOf(false)
        private set

    internal fun extractTimedStrokes(
        items: List<InkItem>,
        originX: Float = 0f,
        originY: Float = 0f,
    ): List<List<TimedStrokePoint>> {
        val scratch = androidx.ink.strokes.StrokeInput()
        var runningTimeMs = 0L
        return items.mapNotNull { ink ->
            val count = ink.stroke.inputs.size
            if (count <= 0) return@mapNotNull null
            val pts = ArrayList<TimedStrokePoint>(count)
            var strokeStartOffset = 0L
            for (i in 0 until count) {
                ink.stroke.inputs.populate(i, scratch)
                val rawT = scratch.elapsedTimeMillis.coerceAtLeast(0L)
                if (i == 0) strokeStartOffset = rawT
                val rel = (rawT - strokeStartOffset).coerceAtLeast(i * 12L)
                pts.add(
                    TimedStrokePoint(
                        x = scratch.x + ink.dx - originX,
                        y = scratch.y + ink.dy - originY,
                        tMs = runningTimeMs + rel,
                    ),
                )
            }
            runningTimeMs = (pts.lastOrNull()?.tMs ?: runningTimeMs) + 60L
            pts
        }
    }

    /**
     * Recognises selected handwritten ink into a typed [TextItem] at its exact bounding box
     * using on-device ML Kit Digital Ink Recognition (with offline polyline fallback) in one undoable edit.
     */
    fun convertSelectionToText(onConverted: (Long) -> Unit = {}, outputFont: TextFont? = null) {
        val pageIdx = selectionPage
        val p = pages.getOrNull(pageIdx) ?: return
        val selectedInk = p.items.filter { it.id in selection && it is InkItem }.map { it as InkItem }
        if (selectedInk.isEmpty()) {
            say("Select handwritten ink first")
            return
        }
        viewModelScope.launch { recognizeInk(p.id, selectedInk, outputFont, automatic = false, onConverted) }
    }

    private var writingPause: Job? = null
    private val pendingInk = linkedMapOf<String, MutableList<InkItem>>()

    fun setAutoHandwriting(enabled: Boolean) {
        notebookSettings.update { it.copy(toolDefaults = it.toolDefaults.copy(autoHandwriting = enabled)) }
        if (!enabled) {
            writingPause?.cancel()
            pendingInk.clear()
        }
    }

    fun queueHandwriting(page: Int, ink: List<InkItem>) {
        val viaAutoTool = tool == Tool.AUTO_TEXT
        if (ink.isEmpty() || readOnly || (!settingsFlow.value.toolDefaults.autoHandwriting && !viaAutoTool)) return
        val sheet = pages.getOrNull(page) ?: return
        pendingInk.getOrPut(sheet.id) { mutableListOf() }.addAll(ink)
        writingPause?.cancel()
        writingPause = viewModelScope.launch {
            do { delay(700) } while (penActive || handwritingBusy)
            val options = settingsFlow.value.toolDefaults
            if ((!options.autoHandwriting && tool != Tool.AUTO_TEXT) || readOnly) {
                pendingInk.clear()
                return@launch
            }
            val font = TextFont.entries.find { it.name == options.autoScript } ?: TextFont.CURSIVE
            for ((id, strokes) in pendingInk.toMap()) {
                val current = pages.firstOrNull { it.id == id }
                val surviving = strokes.filter { current?.items?.contains(it) == true }
                for (line in handwritingLines(surviving)) {
                    while (penActive) delay(150)
                    recognizeInk(id, line, font, automatic = true) {}
                    pendingInk[id]?.removeAll(line.toSet())
                }
                pendingInk.remove(id)
            }
        }
    }

    data class HandwritingReview(val pageId: String, val item: TextItem, val ink: List<InkItem>, val alternatives: List<String>)

    var handwritingReview by mutableStateOf<HandwritingReview?>(null)
        private set

    fun applyHandwritingReview(alternative: String?): Boolean {
        val review = handwritingReview ?: return false
        val index = pages.indexOfFirst { it.id == review.pageId }
        val page = pages.getOrNull(index) ?: return false
        if (readOnly || review.item !in page.items) return false
        val replacement = alternative?.let { text ->
            val item = review.item.copy(text = text)
            if (item.bounds.right > page.width || page.items.any {
                    it.id != item.id && it is TextItem && RectF.intersects(it.bounds, item.bounds)
                }) return false
            listOf(item)
        } ?: review.ink
        beginEdit(index)
        applyItems(index, page.items.flatMap { if (it.id == review.item.id) replacement else listOf(it) })
        handwritingReview = null
        return true
    }

    private suspend fun recognizeInk(
        pageId: String,
        selectedInk: List<InkItem>,
        outputFont: TextFont?,
        automatic: Boolean,
        onConverted: (Long) -> Unit,
    ) {
        if (handwritingBusy || readOnly) return
        val union = RectF(selectedInk.first().bounds)
        selectedInk.forEach { union.union(it.bounds) }
        val timedStrokes = extractTimedStrokes(selectedInk)
        val estimatedSize = HandwritingRecognizer.estimateFontSize(timedStrokes, fallback = textSize)
        val idxPre = pages.indexOfFirst { it.id == pageId }
        val pagePre = pages.getOrNull(idxPre)
        val targetStickyPre = pagePre?.items?.filterIsInstance<StickyItem>()?.lastOrNull { s ->
            !s.collapsed && (s.bounds.contains(union.centerX(), union.centerY()) || RectF.intersects(s.bounds, union))
        }
        handwritingBusy = true
        try {
            val result = HandwritingRecognizer.recognize(
                strokes = timedStrokes,
                writingWidth = union.width().coerceAtLeast(64f),
                writingHeight = union.height().coerceAtLeast(48f),
                languageTag = settingsFlow.value.toolDefaults.handwritingLanguage,
                allowFallback = (!automatic || targetStickyPre != null) && settingsFlow.value.toolDefaults.handwritingLanguage.startsWith("en"),
            )
            val options = settingsFlow.value.toolDefaults
            val idx = pages.indexOfFirst { it.id == pageId }
            val page = pages.getOrNull(idx) ?: return
            if (readOnly || !page.items.containsAll(selectedInk)) return
            val rawText = if (automatic) result.text.trim().replace(Regex("\\s+"), " ") else result.text.trim()
            val text = if (options.autoSpellCheck) AutoSpellChecker.autoCorrect(rawText) else rawText
            val targetSticky = page.items.filterIsInstance<StickyItem>().lastOrNull { s ->
                !s.collapsed && (s.bounds.contains(union.centerX(), union.centerY()) || RectF.intersects(s.bounds, union))
            }
            if (targetSticky != null) {
                // Writing on an expanded sticky note with Auto Handwriting converts straight into the sticky note's text!
                if (automatic && ((!options.autoHandwriting && tool != Tool.AUTO_TEXT) || penActive)) return
                if (text.isBlank() || (automatic && text.none { it.isLetterOrDigit() })) return
                val ids = selectedInk.map { it.id }.toSet()
                val existingText = targetSticky.text.trimEnd()
                val sep = when {
                    existingText.isEmpty() -> ""
                    union.top > targetSticky.y + 88f && existingText.isNotEmpty() && !existingText.endsWith("\n") -> "\n"
                    else -> " "
                }
                val updatedSticky = targetSticky.copy(text = existingText + sep + text)
                beginEdit(idx)
                applyItems(
                    idx,
                    page.items.filterNot { it.id in ids }.map { if (it.id == targetSticky.id) updatedSticky else it },
                )
                onConverted(updatedSticky.id)
                return
            }
            if (automatic && ((!options.autoHandwriting && tool != Tool.AUTO_TEXT) || penActive || !result.usedMlKit || text.count { it.isLetterOrDigit() } < 2)) return
            if (text.isBlank()) {
                say("Could not recognise handwritten ink")
                return
            }
            // Dynamic Bold: automatically bold larger written headings/emphasis text relative to base size
            val shouldDynamicBold = options.dynamicBoldHandwriting &&
                (estimatedSize >= (textSize * 1.28f) || union.height() >= 68f)
            val customFont = options.customHandwritingFontPath
            val baseStyle = if (automatic) {
                TextStyleSpec(
                    font = outputFont ?: TextFont.CURSIVE,
                    bold = shouldDynamicBold,
                    customFontPath = customFont,
                )
            } else {
                (outputFont?.let { textStyle.copy(font = it) } ?: textStyle).copy(
                    bold = textStyle.bold || shouldDynamicBold,
                    customFontPath = textStyle.customFontPath.ifBlank { customFont },
                )
            }
            val x = union.left.coerceAtLeast(0f).coerceAtMost(page.width)
            val layout = if (automatic) singleLineLayout(page, text, baseStyle.font, x, union, estimatedSize, options.alignHandwriting) ?: return
                else estimatedSize to union.top.coerceAtLeast(0f)
            val ids = selectedInk.map { it.id }.toSet()
            val item = TextItem(ItemIds.next(), text, x, layout.second, layout.first,
                selectedInk.first().stroke.brush.colorIntArgb, baseStyle)
            if (automatic && page.items.any { it.id !in ids && it is TextItem && RectF.intersects(it.bounds, item.bounds) }) return
            if (!automatic) handwritingCandidates = result.candidates
            beginEdit(idx)
            applyItems(idx, page.items.filterNot { it.id in ids } + item)
            if (automatic) {
                val alternatives = result.candidates.map { it.trim().replace(Regex("\\s+"), " ") }
                    .filter { it.isNotBlank() && it != item.text }.distinct().take(4)
                handwritingReview = HandwritingReview(page.id, item, selectedInk, alternatives)
            }
            onConverted(item.id)
        } finally {
            handwritingBusy = false
        }
    }


    fun openInlineTextBox(
        page: Int,
        x: Float,
        y: Float,
        existing: TextItem? = null,
        boxWidth: Float = 560f,
        boxHeight: Float = 160f,
        isFullPage: Boolean = false,
    ) {
        val p = pages.getOrNull(page) ?: return
        selection = emptySet()
        val w = if (isFullPage) boxWidth.coerceAtMost(p.width - 48f)
            else if (existing != null) maxOf(existing.raw.width() + 80f, 340f).coerceAtMost(p.width - 48f)
            else boxWidth
        val h = if (isFullPage) boxHeight.coerceAtMost(p.height - 64f)
            else if (existing != null) maxOf(existing.raw.height() + 64f, 130f).coerceAtMost(p.height - 48f)
            else boxHeight
        val rx = (if (isFullPage) x else (existing?.x ?: x)).coerceIn(24f, (p.width - w - 24f).coerceAtLeast(24f))
        val ry = (if (isFullPage) y else (existing?.y ?: y)).coerceIn(24f, (p.height - h - 24f).coerceAtLeast(24f))
        textEdit = TextEdit(page, rx, ry, existing, boxWidth = w, boxHeight = h, isFullPage = isFullPage)
    }

    fun commitText(
        text: String,
        size: Float = textSize,
        color: Int = textColor ?: penStyle.color,
        style: TextStyleSpec = textStyle,
        x: Float? = null,
        y: Float? = null,
    ) {
        val edit = textEdit ?: return
        textEdit = null
        if (edit.isFullPage) keyboardMode = false
        val p = pages.getOrNull(edit.page) ?: return
        val td = settingsFlow.value.toolDefaults
        val effectiveColor = if (td.textOpacity >= 0.99f) color else Brushes.withAlpha(color, td.textOpacity)
        // New text remembers the last formatting used.
        textSize = size; textStyle = style
        textColor = color
        val finalX = x ?: edit.x
        val finalY = y ?: edit.y
        val existing = edit.existing
        val items = when {
            existing != null && text.isBlank() -> p.items.filterNot { it.id == existing.id }
            existing != null -> p.items.map {
                if (it.id == existing.id) existing.copy(text = text, x = finalX, y = finalY, size = size, color = effectiveColor, style = style)
                else it
            }
            text.isBlank() -> return
            else -> p.items + TextItem(ItemIds.next(), text, finalX, finalY, size, effectiveColor, style)
        }
        beginEdit(edit.page)
        applyItems(edit.page, items)
    }

    /**
     * Converts typed text into native Jetpack Ink [InkItem] strokes using the user's
     * recorded Personal Handwriting letter library (or clean fallback vector strokes),
     * aligned to paper rules and wrapped to the text box width.
     */
    fun commitTextAsHandwriting(
        text: String,
        size: Float = textSize,
        color: Int = textColor ?: penStyle.color,
        x: Float? = null,
        y: Float? = null,
        maxWidth: Float? = null,
    ) {
        val edit = textEdit ?: return
        textEdit = null
        if (edit.isFullPage) keyboardMode = false
        val p = pages.getOrNull(edit.page) ?: return
        val existing = edit.existing
        if (text.isBlank()) {
            if (existing != null) {
                beginEdit(edit.page)
                applyItems(edit.page, p.items.filterNot { it.id == existing.id })
            }
            return
        }
        textSize = size
        textColor = color
        val finalX = x ?: edit.x
        val finalY = y ?: edit.y
        val availW = (maxWidth ?: edit.boxWidth).coerceAtMost((p.width - finalX - 24f).coerceAtLeast(160f))
        val profile = HandwritingFontStore.get(getApplication()).profile.value
        val inkItems = HandwritingSynthesizer.synthesizeInkItems(
            text = text,
            startX = finalX,
            startY = finalY,
            maxWidth = availW,
            fontSize = size,
            color = color,
            paper = p.paper,
            lineScale = p.lineScale,
            baseBrush = brushFor(Tool.PEN),
            profile = profile,
        )
        if (inkItems.isEmpty()) return
        val baseItems = if (existing != null) p.items.filterNot { it.id == existing.id } else p.items
        beginEdit(edit.page)
        applyItems(edit.page, baseItems + inkItems)
        if (profile.trainedCharCount() == 0) {
            say("Placed as ink. Train your letters in My Handwriting Style.")
        } else {
            say("Placed in your handwriting")
        }
    }

    fun dismissTextEdit() {
        val edit = textEdit
        textEdit = null
        if (edit?.isFullPage == true) keyboardMode = false
    }

    fun openSticky(page: Int, x: Float, y: Float, existing: StickyItem?) {
        val p = pages.getOrNull(page) ?: return
        if (existing == null) {
            // Place sticky note directly on the page (no popup!) and switch to Pen so the user can write right on it.
            val w = 320f
            val h = 300f
            val sx = (x - w / 2f).coerceIn(16f, (p.width - w - 16f).coerceAtLeast(16f))
            val sy = (y - h / 2f).coerceIn(16f, (p.height - h - 16f).coerceAtLeast(16f))
            val placed = StickyItem(ItemIds.next(), sx, sy, w, h, stickyColor, "")
            beginEdit(page)
            applyItems(page, p.items + placed)
            if (tool == Tool.STICKY) {
                tool = if (settingsFlow.value.toolDefaults.autoHandwriting) Tool.AUTO_TEXT else Tool.PEN
            }
            say("Sticky note placed — write directly on it, or tap ▾ to collapse")
        } else if (existing.collapsed) {
            toggleStickyCollapse(page, existing)
        } else {
            stickyEdit = StickyEdit(page, existing, false)
        }
    }

    fun toggleStickyCollapse(page: Int, sticky: StickyItem) {
        val p = pages.getOrNull(page) ?: return
        val updated = sticky.copy(collapsed = !sticky.collapsed)
        beginEdit(page)
        applyItems(page, p.items.map { if (it.id == sticky.id) updated else it })
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

    // ------------------------------------------------------------- Concept Maps

    /** Emoji stickers are large text items placed in the middle of the visible page. */
    fun insertSticker(emoji: String): Pair<Int, Long>? {
        val p = currentPage ?: return null
        val item = TextItem(ItemIds.next(), emoji, p.width / 2f - 80f, p.height / 3f, 160f, 0xFF000000.toInt(), TextStyleSpec())
        beginEdit(pageIndex)
        applyItems(pageIndex, p.items + item)
        return pageIndex to item.id
    }

    fun insertImage(
        uri: Uri,
        targetPage: Int = pageIndex,
        targetX: Float? = null,
        targetY: Float? = null,
        onPlaced: (Int, Long) -> Unit,
    ): Job {
        val destination = pages.getOrNull(targetPage)?.id
        val td = settingsFlow.value.toolDefaults
        return viewModelScope.launch {
            val savedName = runCatching { repo.saveImage(noteId, uri) }.getOrNull()
            if (savedName == null) {
                say("Could not read image.")
                return@launch
            }
            val rawBmp = withContext(Dispatchers.IO) { renderer().image(savedName) }
            if (rawBmp == null) {
                say("Could not decode image.")
                return@launch
            }
            val finalName = if (td.imageOpacity < 0.99f || td.imageBorder) {
                val processed = withContext(Dispatchers.Default) {
                    applyBitmapEffects(rawBmp, td.imageOpacity, td.imageBorder)
                }
                val styledName = "img_styled_${UUID.randomUUID()}.png"
                withContext(Dispatchers.IO) {
                    AtomicFiles.writeBytesAtomic(File(repo.noteDir(noteId), styledName)) { out ->
                        processed.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
                styledName
            } else savedName
            val idx = pages.indexOfFirst { it.id == destination }
            val p = pages.getOrNull(idx) ?: return@launch
            val w = (p.width * td.imageWidthRatio.coerceIn(0.2f, 0.95f))
                .coerceAtMost(rawBmp.width.toFloat().coerceAtLeast(200f))
            val h = if (td.imageLockAspect && rawBmp.width > 0) w * rawBmp.height / rawBmp.width else w * 0.75f
            val ix = ((targetX ?: (p.width / 2f)) - w / 2f).coerceIn(16f, (p.width - w - 16f).coerceAtLeast(16f))
            val iy = ((targetY ?: ((p.height - h) / 3f + h / 2f)) - h / 2f).coerceIn(16f, (p.height - h - 16f).coerceAtLeast(16f))
            val item = ImageItem(ItemIds.next(), finalName, ix, iy, w, h)
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
            val name = if (uri == null) null else runCatching { repo.saveImage(noteId, uri) }.getOrNull()
            if (uri != null && name == null) {
                say("Could not read background image.")
                return@launch
            }
            val p = pages.getOrNull(idx) ?: return@launch
            pages[idx] = p.copy(bgImage = name)
            dirtyPageIds += p.id
            thumbs.evictAll()
            scheduleSave(); onPagesChanged?.invoke()
        }
    }

    /** Paper colour of the page in view; null follows the app setting. */
    fun setPageColor(color: Int?) {
        val p = currentPage ?: return
        pages[pageIndex] = p.copy(paperColor = color)
        dirtyPageIds += p.id
        scheduleSave(); onPagesChanged?.invoke()
    }

    fun toggleCurrentWhiteboard() {
        val p = currentPage ?: return
        val nextWb = !p.isWhiteboard
        val nextPaper = if (nextWb && p.paper == PaperStyle.BLANK) PaperStyle.DOTTED else p.paper
        pages[pageIndex] = p.copy(isWhiteboard = nextWb, paper = nextPaper)
        dirtyPageIds += p.id
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
        dirtyPageIds += np.id
        structureDirty = true
        undoStack.clear(); redoStack.clear(); refreshUndo()
        scheduleSave(); onPagesChanged?.invoke()
        onScrollTo?.invoke(at)
    }

    fun deletePage() {
        if (pages.size <= 1) return
        pages.removeAt(pageIndex)
        structureDirty = true
        undoStack.clear(); redoStack.clear(); refreshUndo()
        pageIndex = pageIndex.coerceAtMost(pages.size - 1)
        scheduleSave(); onPagesChanged?.invoke()
    }

    fun duplicatePage() {
        val p = currentPage ?: return
        val copy = p.copy(id = UUID.randomUUID().toString(), items = p.items.map { it.withId(ItemIds.next()) })
        pages.add(pageIndex + 1, copy)
        dirtyPageIds += copy.id
        structureDirty = true
        undoStack.clear(); redoStack.clear(); refreshUndo()
        scheduleSave(); onPagesChanged?.invoke()
        onScrollTo?.invoke(pageIndex + 1)
    }

    fun setPaper(style: PaperStyle) {
        val p = currentPage ?: return
        pages[pageIndex] = p.copy(paper = style)
        dirtyPageIds += p.id
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
                val k = minOf(1f, 2600f / (region.width() * screenScale), 2600f / (region.height() * screenScale))
                val ow = (region.width() * screenScale * k).toInt().coerceAtLeast(1)
                val oh = (region.height() * screenScale * k).toInt().coerceAtLeast(1)
                val bmp = withContext(Dispatchers.IO) { runCatching { detailPdf().renderRegion(page.pdfPage, page.width, region, ow, oh) }.getOrNull() } ?: continue
                out += Detail(pi, RectF(region), bmp, screenScale)
            }
            details = out
            onReady()
        }
    }

    /** Thumbnails are cached until the page changes (pages are immutable, so identity is enough). */
    internal val thumbs = object : android.util.LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    suspend fun thumbnail(index: Int, width: Int): Bitmap? {
        val page = pages.getOrNull(index) ?: return null
        val key = "${page.id}:${System.identityHashCode(page)}:$width"
        thumbs.get(key)?.let { return it }
        return renderThumb(page, width)?.also { thumbs.put(key, it) }
    }

    suspend fun renderReaderPage(index: Int, width: Int): Bitmap? {
        val clampedW = width.coerceIn(640, 1480)
        return thumbnail(index, clampedW)
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

    internal val thumbLock = kotlinx.coroutines.sync.Mutex()
    internal var thumbRenderer: PageRenderer? = null
    private var thumbPdfInstance: PdfPages? = null

    @Synchronized
    internal fun thumbPdf(): PdfPages = thumbPdfInstance ?: PdfPages(repo.pdfFile(noteId)).also { thumbPdfInstance = it }

    // ------------------------------------------------------- flashcards + ink links
    internal val study = com.pronotes.app.data.StudyStore(repo.noteDir(noteId))
    var cards by mutableStateOf<List<com.pronotes.app.data.Flashcard>>(emptyList())
        internal set
    var links by mutableStateOf<List<com.pronotes.app.data.InkLink>>(emptyList())
        internal set
    var slides by mutableStateOf<List<com.pronotes.app.data.SlideCard>>(emptyList())
        internal set
    var showSlideShow by mutableStateOf(false)
    /** Canvas is waiting for the user to drag a box for a new flashcard. */
    var capturingCard by mutableStateOf(false)
    /** Card being created or edited (front picture already cut). */
    data class CardDraft(val card: com.pronotes.app.data.Flashcard, val isNew: Boolean)
    var cardDraft by mutableStateOf<CardDraft?>(null)
    /** Start of a link waiting for its other end. */
    var linkFrom by mutableStateOf<Triple<String, Float, Float>?>(null)
    var linkToDelete by mutableStateOf<String?>(null)

    private fun loadStudy() { val d = study.load(); cards = d.cards; links = d.links; slides = d.slides }
    internal fun saveStudy() {
        val d = com.pronotes.app.data.StudyData(cards, links, slides)
        viewModelScope.launch(Dispatchers.IO) { runCatching { study.save(d) } }
    }

    /** Card waiting for its answer to be cut from the page. */
    var backFor by mutableStateOf<CardDraft?>(null)
    /** Selected region waiting for link destination (page or URL). */
    var linkSheet by mutableStateOf<Pair<String, RectF>?>(null)

    // --------------------------------------------------------------- search

    /** Per-page cached handwriting recognition results (keyed by pageId + ink fingerprint) for instant handwritten search. */
    private val handwritingSearchCache = java.util.concurrent.ConcurrentHashMap<String, List<Pair<RectF, String>>>()

    fun search(query: String) {
        searchQuery = query
        searchJob?.cancel()
        if (query.isBlank()) {
            clearSearch()
            return
        }
        searchJob = viewModelScope.launch {
            delay(220)
            searching = true
            val cleanQ = query.trim()
            val hits = ArrayList<SearchHit>()
            if (isPdf) {
                val index = textIndex()
                val map = pages.withIndex().filter { it.value.pdfPage >= 0 }.associate { it.value.pdfPage to it.index }
                if (index != null) hits += withContext(Dispatchers.Default) { index.search(cleanQ) { map[it] } }
            }
            val lang = settingsFlow.value.toolDefaults.handwritingLanguage
            val snapshotPages = pages.toList()
            // 1) Typed text, sticky notes, web links, write boxes, and concept map nodes
            snapshotPages.forEachIndexed { i, p ->
                p.items.forEach { item ->
                    val text = when (item) {
                        is TextItem -> item.text
                        is StickyItem -> listOf(item.title, item.text).filter { it.isNotBlank() }.joinToString(" · ")
                        is WebLinkItem -> listOf(item.title, item.url, item.note).filter { it.isNotBlank() }.joinToString(" · ")
                        is MindNodeItem -> listOf(item.label, item.tag, item.notes).filter { it.isNotBlank() }.joinToString(" · ")
                        is WriteBoxItem -> item.convertedText
                        else -> ""
                    }
                    if (text.isNotEmpty() && text.contains(cleanQ, ignoreCase = true)) {
                        hits += SearchHit(i, listOf(item.bounds), text.take(64))
                    }
                }
            }
            // 2) Recognize human handwriting across pages (cached per ink fingerprint so repeat searches are instant)
            val hwHits = withContext(Dispatchers.Default) {
                val out = ArrayList<SearchHit>()
                snapshotPages.forEachIndexed { pageIdx, p ->
                    val inkItems = p.items.filterIsInstance<InkItem>().filter { !it.isHighlighter }
                    if (inkItems.isEmpty()) return@forEachIndexed
                    val fp = "${p.id}:${inkItems.size}:${inkItems.fold(0L) { acc, it -> acc * 31L + it.id }}"
                    val lines = handwritingSearchCache[fp] ?: run {
                        val recognized = ArrayList<Pair<RectF, String>>()
                        for (lineStrokes in handwritingLines(inkItems)) {
                            if (lineStrokes.isEmpty()) continue
                            val union = RectF(lineStrokes.first().bounds).apply {
                                lineStrokes.drop(1).forEach { union(it.bounds) }
                            }
                            val timed = extractTimedStrokes(lineStrokes)
                            val res = runCatching {
                                HandwritingRecognizer.recognize(
                                    strokes = timed,
                                    writingWidth = union.width().coerceAtLeast(64f),
                                    writingHeight = union.height().coerceAtLeast(48f),
                                    languageTag = lang,
                                    allowFallback = lang.startsWith("en"),
                                )
                            }.getOrNull()
                            val combined = listOfNotNull(res?.text?.takeIf { it.isNotBlank() })
                                .plus(res?.candidates.orEmpty().take(3))
                                .distinct()
                                .joinToString(" ")
                            if (combined.isNotBlank()) {
                                recognized += union to combined
                            }
                        }
                        if (handwritingSearchCache.size > 64) handwritingSearchCache.clear()
                        handwritingSearchCache[fp] = recognized
                        recognized
                    }
                    for ((bounds, recText) in lines) {
                        val directMatch = recText.contains(cleanQ, ignoreCase = true)
                        val fuzzyMatch = !directMatch && cleanQ.length >= 4 &&
                            recText.split(Regex("\\s+")).any { word ->
                                val w = word.filter { it.isLetterOrDigit() }
                                w.length >= 3 && AutoSpellChecker.editDistance(w.lowercase(), cleanQ.lowercase()) <= 1
                            }
                        if (directMatch || fuzzyMatch) {
                            val display = recText.split(" ").take(8).joinToString(" ").take(56)
                            out += SearchHit(pageIdx, listOf(bounds), "Handwriting: $display")
                        }
                    }
                }
                out
            }
            hits += hwHits

            searchHits = hits.sortedBy { it.page }
            activeHit = if (hits.isEmpty()) -1 else 0
            searching = false
            searchHits.getOrNull(0)?.let { onScrollTo?.invoke(it.page) }
            onPagesChanged?.invoke()
        }
    }

    /** PDF text with character boxes, built once on first use. */
    internal suspend fun textIndex(): PdfTextIndex? {
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
        searching = false
        searchQuery = ""
        searchHits = emptyList()
        activeHit = -1
        onPagesChanged?.invoke()
    }

    fun highlights(page: Int): List<RectF> = if (searchHits.isEmpty()) emptyList() else searchHits.filter { it.page == page }.flatMap { it.rects }
    fun activeHighlight(): Pair<Int, RectF>? = searchHits.getOrNull(activeHit)?.let { h -> h.rects.firstOrNull()?.let { h.page to it } }

    // ----------------------------------------------------------- study & selection feedback
    /** Short message shown at the bottom of the editor (null = none). */
    var toast by mutableStateOf<String?>(null)
        private set
    var toastActionLabel by mutableStateOf<String?>(null)
        private set
    var toastAction by mutableStateOf<(() -> Unit)?>(null)
        private set

    fun dismissToast() {
        toast = null
        toastActionLabel = null
        toastAction = null
    }

    internal fun say(msg: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
        toast = msg
        toastActionLabel = actionLabel
        toastAction = action
        val duration = if (action != null) 3400L else 2200L
        viewModelScope.launch {
            delay(duration)
            if (toast == msg && toastAction === action) dismissToast()
        }
    }

    fun showHint(message: String) = say(message)

    /** Scribble-to-erase provides dust particle, audio, and haptic feedback without obstructing the canvas with a banner. */
    fun onScribbleErased() = Unit
    var pdfTextBusy by mutableStateOf(false)
        internal set

    // ---------------------------------------------------------------- PDF text selection
    data class TextSel(val page: Int, val pdfPage: Int, val anchor: IntRange, val range: IntRange, val rects: List<RectF>, val text: String)
    var textSel by mutableStateOf<TextSel?>(null)
        internal set
    var textMenu by mutableStateOf<RectF?>(null)

    /** Text of single PDF pages, read on demand so long-press works at once even on huge PDFs. */
    internal val pageTextIdx = java.util.concurrent.ConcurrentHashMap<Int, PdfTextIndex>()
    internal var pageTextJob: Job? = null

    // --------------------------------------------------------------- collab & reader helpers

    fun noteDirFile(): File = repo.noteDir(noteId)

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

    fun insertTextToPage(text: String) {
        val clean = text.trim().take(2400)
        if (clean.isBlank()) return
        val idx = pageIndex
        val p = pages.getOrNull(idx) ?: return
        beginEdit(idx)
        applyItems(idx, p.items + TextItem(ItemIds.next(), clean, 72f, 96f, 26f, penStyle.color))
        say("Inserted onto page ${idx + 1}")
    }

    fun applySplitItems(page: Int, items: List<PageItem>) {
        val p = pages.getOrNull(page) ?: return
        pages[page] = p.copy(items = items)
        dirtyPageIds += p.id
        thumbs.evictAll()
        scheduleSave()
        onPagesChanged?.invoke()
    }

    // --------------------------------------------------------------- save

    internal fun scheduleSave() {
        saveJob?.cancel()
        val debounceMs = store.settings.value.performanceMode.autosaveMs
        saveJob = viewModelScope.launch {
            delay(debounceMs)
            val snapshot = pages.toList()
            val dirtySnap = synchronized(dirtyPageIds) {
                HashSet(dirtyPageIds).also { dirtyPageIds.clear() }
            }
            val structSnap = structureDirty
            structureDirty = false
            repo.savePages(
                id = noteId,
                pages = snapshot,
                dirtyPageIds = dirtySnap,
                structureChanged = structSnap,
                writeFullSnapshot = false,
            )
        }
    }

    private fun toolPreferences() = listOf(
        (toolBeforeCircle ?: _tool).name, penType.name, "${penStyle.color},${penStyle.size}", "${highlighterStyle.color},${highlighterStyle.size}",
        "${shapeStyle.color},${shapeStyle.size}", shapeKind.name, eraserSize, tapeColor, stickyColor,
        textColor ?: "", eraserCustomDp ?: "", textSize, shapeDashed, shapeFillOn, shapeFillColor, tapeFreehand, tapePattern,
        penStyle.sizes.joinToString(","), highlighterStyle.sizes.joinToString(","), shapeStyle.sizes.joinToString(","), textStyle.font.name,
        textStyle.bold, textStyle.italic, textStyle.underline, textStyle.gradient ?: "", textStyle.align.name, webLinkColor, mindColor,
        "${crayonStyle.color},${crayonStyle.size}",
    ).joinToString("|")

    fun saveNow() {
        store.setPref("tools_$noteId", toolPreferences())
        if (!loaded) return
        saveJob?.cancel()
        val snapshot = pages.toList()
        val dirtySnap = synchronized(dirtyPageIds) {
            HashSet(dirtyPageIds).also { dirtyPageIds.clear() }
        }
        structureDirty = false
        repo.appScope.launch {
            repo.savePages(
                id = noteId,
                pages = snapshot,
                dirtyPageIds = dirtySnap,
                structureChanged = true,
                writeFullSnapshot = true,
            )
        }
    }

    suspend fun exportPdf(out: OutputStream) = repo.exportPdf(noteId, pages.toList(), out)

    var activeWriteBoxId by mutableStateOf<Long?>(null)
    var activeWriteBoxPage by mutableStateOf(0)
    var keyboardMode by mutableStateOf(false)
    var onRequestCommitText: (() -> Unit)? = null

    override fun onCleared() {
        if (saveJob?.isActive == true || synchronized(dirtyPageIds) { dirtyPageIds.isNotEmpty() } || structureDirty) {
            saveNow()
        }
        pdf?.close()
        thumbPdfInstance?.close()
        detailPdfInstance?.close()
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

