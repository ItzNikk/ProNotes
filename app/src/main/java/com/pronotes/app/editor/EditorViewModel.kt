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
    private var _tool by mutableStateOf(Tool.PEN)
    private var previousTool = Tool.PEN
    /** Current tool. Remembers the last non-eraser tool for "switch back after erasing". */
    var tool: Tool
        get() = _tool
        set(v) { if (_tool != Tool.ERASER && _tool != v) previousTool = _tool; _tool = v }
    var penType by mutableStateOf(PenType.FOUNTAIN)
    var penStyle by mutableStateOf(style("pen", listOf(PenPalette[0], PenPalette[2], PenPalette[10], PenPalette[5], PenPalette[13]), listOf(2f, 4f, 7f), 4f))
    var highlighterStyle by mutableStateOf(style("highlighter", HighlighterPalette, listOf(16f, 28f, 42f), 28f))
    var shapeStyle by mutableStateOf(style("shape", listOf(PenPalette[0], PenPalette[2], PenPalette[10], PenPalette[5], PenPalette[13]), listOf(2f, 4f, 7f), 4f))
    var shapeKind by mutableStateOf(ShapeKind.AUTO)
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
    var eraserSize by mutableStateOf(1)
    var textSize by mutableStateOf(40f)
    var tapeColor by mutableStateOf(TapeColors[0])
    var tapeFreehand by mutableStateOf(false)
    var stickyColor by mutableStateOf(StickyColors[0])

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

    /** Set by the screen so model changes can be pushed into the canvas. */
    var onPagesChanged: (() -> Unit)? = null
    var onScrollTo: ((Int) -> Unit)? = null

    init {
        viewModelScope.launch {
            val loadedPages = repo.loadPages(noteId)
            pages.clear(); pages.addAll(loadedPages)
            if (repo.pdfFile(noteId).exists()) pdf = PdfPages(repo.pdfFile(noteId))
            loaded = true
            repo.markOpened(noteId)
            onPagesChanged?.invoke()
        }
    }

    private fun style(key: String, colors: List<Int>, sizes: List<Float>, size: Float): ToolStyle {
        val c = store.swatches(key, colors)
        return ToolStyle(c, sizes, c.first(), size)
    }

    val currentPage: Page? get() = pages.getOrNull(pageIndex)

    fun renderer() = PageRenderer(repo.noteDir(noteId))

    fun onCurrentPage(i: Int) { pageIndex = i }

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
        Tool.HIGHLIGHTER -> Brushes.create(Brushes.HIGHLIGHTER, highlighterStyle.color, highlighterStyle.size)
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

    fun pdfBitmap(page: Page, onReady: () -> Unit): Bitmap? {
        val src = pdf ?: return null
        src.cached(page.pdfPage)?.let { return it }
        if (pendingPdf.add(page.pdfPage)) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) { src.render(page.pdfPage, page.width.toInt(), page.height.toInt()) }
                pendingPdf.remove(page.pdfPage)
                onReady()
            }
        }
        return null
    }

    suspend fun thumbnail(index: Int, width: Int): Bitmap? = withContext(Dispatchers.Default) {
        val page = pages.getOrNull(index) ?: return@withContext null
        val scale = width / page.width
        val h = (page.height * scale).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(width, h, Bitmap.Config.ARGB_8888)
        val bg = if (page.pdfPage >= 0) withContext(Dispatchers.IO) {
            PdfPages(repo.pdfFile(noteId)).let { pp -> pp.render(page.pdfPage, width, h, useCache = false).also { pp.close() } }
        } else null
        val r = renderer().apply { paperColor = settingsFlow.value.paperTint.argb }
        r.renderPage(Canvas(bmp), page, scale, bg)
        bmp
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

    fun highlights(page: Int): List<RectF> = searchHits.filter { it.page == page }.flatMap { it.rects }
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
        super.onCleared()
    }
}
