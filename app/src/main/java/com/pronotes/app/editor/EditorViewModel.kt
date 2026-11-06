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
import java.io.OutputStream
import java.util.UUID

data class ToolStyle(val colors: List<Int>, val sizes: List<Float>, val color: Int, val size: Float)

val PenPalette = listOf(
    0xFF1B1F2A, 0xFF5B6475, 0xFF3D6CF4, 0xFF0A84FF, 0xFF5AC8FA, 0xFF34C759,
    0xFF2BB673, 0xFF00897B, 0xFFFFCC00, 0xFFFF9500, 0xFFFF3B30, 0xFFEB5757,
    0xFFFF2D55, 0xFFAF52DE, 0xFF9B51E0, 0xFF6D4C41, 0xFFFFFFFF, 0xFF000000,
).map { it.toInt() }

val HighlighterPalette = listOf(0xFFFFE066, 0xFF8CE99A, 0xFF74C0FC, 0xFFFFA8A8, 0xFFE599F7).map { it.toInt() }
val TapeColors = listOf(0xFFF4A7C0, 0xFF9AD0EC, 0xFFB8E0A8, 0xFFFFD98E, 0xFFCDB4F0).map { it.toInt() }
val StickyColors = listOf(0xFFFFF3A3, 0xFFFFD6E0, 0xFFCDEBFF, 0xFFD8F5C8, 0xFFFFE2C2).map { it.toInt() }

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
    var penStyle by mutableStateOf(style("pen", listOf(PenPalette[0], PenPalette[2], PenPalette[10], PenPalette[5], PenPalette[13]), listOf(2f, 4f, 7f), 4f, 2))
    var highlighterStyle by mutableStateOf(style("highlighter", HighlighterPalette, listOf(16f, 28f, 42f), 28f, 3))
    var shapeStyle by mutableStateOf(style("shape", listOf(PenPalette[0], PenPalette[2], PenPalette[10], PenPalette[5], PenPalette[13]), listOf(2f, 4f, 7f), 4f, 4))
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
    var textEdit by mutableStateOf<TextEdit?>(null)
    var stickyEdit by mutableStateOf<StickyEdit?>(null)

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
        viewModelScope.launch {
            val loadedPages = repo.loadPages(noteId)
            startPage = store.pref("page_$noteId", "0").toIntOrNull()?.coerceIn(0, (loadedPages.size - 1).coerceAtLeast(0)) ?: 0
            pages.clear(); pages.addAll(loadedPages)
            if (repo.pdfFile(noteId).exists()) pdf = PdfPages(repo.pdfFile(noteId))
            withContext(Dispatchers.IO) { loadStudy() }
            loaded = true
            prefetchPdf(startPage)
            repo.markOpened(noteId)
            onPagesChanged?.invoke()
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

    fun savePreset() = store.addPreset(com.pronotes.app.data.PenPreset(penType.name, penStyle.color, penStyle.size))
    fun removePreset(p: com.pronotes.app.data.PenPreset) = store.removePreset(p)
    fun applyPreset(p: com.pronotes.app.data.PenPreset) {
        penType = runCatching { PenType.valueOf(p.type) }.getOrDefault(PenType.FOUNTAIN)
        val colors = if (p.color in penStyle.colors) penStyle.colors else (penStyle.colors + p.color).takeLast(8)
        penStyle = penStyle.copy(colors = colors, color = p.color, size = p.size)
        tool = Tool.PEN
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

    fun setItems(page: Int, items: List<PageItem>) {
        touched()
        val p = pages.getOrNull(page) ?: return
        pages[page] = p.copy(items = items)
        scheduleSave()
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
                // Fills keep their transparency when recoloured.
                is FillItem -> item.recolored((color and 0x00FFFFFF) or (item.color and 0xFF000000.toInt()))
                else -> item
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
        else StickyEdit(page, StickyItem(ItemIds.next(), x - 140f, y - 140f, 280f, 280f, stickyColor, ""), true)
    }

    fun commitSticky(text: String, color: Int) {
        val edit = stickyEdit ?: return
        stickyEdit = null
        val p = pages.getOrNull(edit.page) ?: return
        val item = edit.item.copy(text = text, color = color)
        beginEdit(edit.page)
        applyItems(edit.page, if (edit.isNew) p.items + item else p.items.map { if (it.id == item.id) item else it })
    }

    fun deleteSticky() {
        val edit = stickyEdit ?: return
        stickyEdit = null
        if (edit.isNew) return
        val p = pages.getOrNull(edit.page) ?: return
        beginEdit(edit.page)
        applyItems(edit.page, p.items.filterNot { it.id == edit.item.id })
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
        return Page(UUID.randomUUID().toString(), w, h, paper, paperColor = ref?.paperColor)
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

    fun addPage(atEnd: Boolean = false, paper: PaperStyle? = null) {
        val np = newPage().let { if (paper != null) it.copy(paper = paper) else it }
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
        var want = ((widthPx + 255) / 256 * 256).coerceIn(768, 2200)
        val aspect = page.height / page.width
        // Keep very tall pages within a sane pixel budget.
        if (want * want * aspect > 7_000_000f) want = kotlin.math.sqrt(7_000_000f / aspect).toInt()
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
    /** Canvas is waiting for the user to drag a box for a new flashcard. */
    var capturingCard by mutableStateOf(false)
    /** Card being created or edited (front picture already cut). */
    data class CardDraft(val card: com.pronotes.app.data.Flashcard, val isNew: Boolean)
    var cardDraft by mutableStateOf<CardDraft?>(null)
    /** Start of a link waiting for its other end. */
    var linkFrom by mutableStateOf<Triple<String, Float, Float>?>(null)
    var linkToDelete by mutableStateOf<String?>(null)

    private fun loadStudy() { val d = study.load(); cards = d.cards; links = d.links }
    private fun saveStudy() {
        val d = com.pronotes.app.data.StudyData(cards, links)
        viewModelScope.launch(Dispatchers.IO) { runCatching { study.save(d) } }
    }

    fun dueCards(now: Long = System.currentTimeMillis()) = cards.filter { it.due <= now }

    fun cardImage(name: String?): Bitmap? = name?.let { runCatching { android.graphics.BitmapFactory.decodeFile(java.io.File(study.cardDir(), it).path) }.getOrNull() }

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
        cards = if (cards.any { it.id == card.id }) cards.map { if (it.id == card.id) card else it } else cards + card
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

    fun deleteLink(id: String) { links = links.filterNot { it.id == id }; linkToDelete = null; saveStudy() }

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
        if (query.isBlank()) { searchHits = emptyList(); activeHit = -1; return }
        searchJob = viewModelScope.launch {
            delay(250)
            searching = true
            val hits = ArrayList<SearchHit>()
            if (isPdf) {
                val index = pdfIndex ?: withContext(Dispatchers.IO) {
                    runCatching { PdfTextIndex.build(getApplication(), repo.pdfFile(noteId)) }.getOrNull()
                }.also { pdfIndex = it }
                val map = pages.withIndex().filter { it.value.pdfPage >= 0 }.associate { it.value.pdfPage to it.index }
                if (index != null) hits += withContext(Dispatchers.Default) { index.search(query) { map[it] } }
            }
            // Typed text and sticky notes are searchable too.
            pages.forEachIndexed { i, p ->
                p.items.forEach { item ->
                    val text = (item as? TextItem)?.text ?: (item as? StickyItem)?.text ?: return@forEach
                    if (text.contains(query, ignoreCase = true)) hits += SearchHit(i, listOf(item.bounds), text.take(60))
                }
            }
            searchHits = hits.sortedBy { it.page }
            activeHit = if (hits.isEmpty()) -1 else 0
            searching = false
            searchHits.getOrNull(0)?.let { onScrollTo?.invoke(it.page) }
            onPagesChanged?.invoke()
        }
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

    fun clearSearch() { searchJob?.cancel(); searchQuery = ""; searchHits = emptyList(); activeHit = -1; onPagesChanged?.invoke() }

    fun highlights(page: Int): List<RectF> = if (searchHits.isEmpty()) emptyList() else searchHits.filter { it.page == page }.flatMap { it.rects }
    fun activeHighlight(): Pair<Int, RectF>? = searchHits.getOrNull(activeHit)?.let { h -> h.rects.firstOrNull()?.let { h.page to it } }

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
