package com.pronotes.app.editor

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.ink.brush.Brush
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

data class ToolStyle(val colors: List<Int>, val sizes: List<Float>, val color: Int, val size: Float)

val PenPalette = listOf(
    0xFF1B1F2A, 0xFF3D6CF4, 0xFFEB5757, 0xFF2BB673, 0xFFF2994A, 0xFF9B51E0,
    0xFF56CCF2, 0xFFF2C94C, 0xFF6D4C41, 0xFF828282, 0xFFE91E63, 0xFF00897B,
    0xFF0D47A1, 0xFF1B5E20, 0xFFB71C1C, 0xFF4A148C, 0xFFFFFFFF, 0xFF000000,
).map { it.toInt() }

class EditorViewModel(app: Application, val noteId: String) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val pages = mutableStateListOf<Page>()
    var pageIndex by mutableStateOf(0)
        private set
    var loaded by mutableStateOf(false)
        private set
    var title by mutableStateOf(repo.meta(noteId)?.title ?: "Notebook")
        private set

    var tool by mutableStateOf(Tool.PEN)
    var penStyle by mutableStateOf(ToolStyle(listOf(PenPalette[0], PenPalette[1], PenPalette[2]), listOf(3f, 6f, 10f), PenPalette[0], 6f))
    var highlighterStyle by mutableStateOf(
        ToolStyle(listOf(0x66F2C94C, 0x6656CCF2, 0x66EB5757), listOf(16f, 28f, 40f), 0x66F2C94C, 28f),
    )
    var shapeStyle by mutableStateOf(ToolStyle(listOf(PenPalette[0], PenPalette[1], PenPalette[2]), listOf(3f, 6f, 10f), PenPalette[1], 6f))
    var eraserSize by mutableStateOf(1)   // 0..2
    var textSize by mutableStateOf(40f)

    var selection by mutableStateOf<Set<Long>>(emptySet())
        private set
    var selectionBounds by mutableStateOf<RectF?>(null)
        private set

    var settings by mutableStateOf(loadSettings())
        private set

    var textEdit by mutableStateOf<TextEdit?>(null)
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    data class TextEdit(val x: Float, val y: Float, val existing: TextItem?)

    private val undoStack = ArrayDeque<Pair<Int, List<PageItem>>>()
    private val redoStack = ArrayDeque<Pair<Int, List<PageItem>>>()
    private var saveJob: Job? = null
    private var pdf: PdfPages? = null
    private val pendingPdf = HashSet<Int>()

    /** Set by the screen so model changes can be pushed into the canvas. */
    var onPagesChanged: (() -> Unit)? = null

    init {
        viewModelScope.launch {
            val loadedPages = repo.loadPages(noteId)
            pages.clear(); pages.addAll(loadedPages)
            if (repo.pdfFile(noteId).exists()) pdf = PdfPages(repo.pdfFile(noteId))
            loaded = true
            onPagesChanged?.invoke()
        }
    }

    val currentPage: Page? get() = pages.getOrNull(pageIndex)

    fun renderer() = PageRenderer(repo.noteDir(noteId))

    // ------------------------------------------------------------- brushes

    fun styleFor(t: Tool): ToolStyle? = when (t) {
        Tool.PEN -> penStyle
        Tool.HIGHLIGHTER -> highlighterStyle
        Tool.SHAPE -> shapeStyle
        else -> null
    }

    fun updateStyle(t: Tool, s: ToolStyle) {
        when (t) {
            Tool.PEN -> penStyle = s
            Tool.HIGHLIGHTER -> highlighterStyle = s
            Tool.SHAPE -> shapeStyle = s
            else -> Unit
        }
    }

    fun brushFor(t: Tool): Brush = when (t) {
        Tool.HIGHLIGHTER -> Brushes.create(Brushes.HIGHLIGHTER, highlighterStyle.color, highlighterStyle.size)
        Tool.SHAPE -> Brushes.create(Brushes.PEN, shapeStyle.color, shapeStyle.size)
        else -> Brushes.create(Brushes.PEN, penStyle.color, penStyle.size)
    }

    fun eraserRadiusPx(density: Float) = listOf(8f, 16f, 30f)[eraserSize] * density

    // --------------------------------------------------------------- edits

    fun beginEdit() {
        val p = currentPage ?: return
        undoStack.addLast(pageIndex to p.items)
        if (undoStack.size > 100) undoStack.removeFirst()
        redoStack.clear()
        refreshUndo()
    }

    fun setItems(items: List<PageItem>) {
        val p = currentPage ?: return
        pages[pageIndex] = p.copy(items = items)
        scheduleSave()
    }

    private fun applyItems(items: List<PageItem>) {
        setItems(items)
        onPagesChanged?.invoke()
    }

    fun undo() {
        val (idx, items) = undoStack.removeLastOrNull() ?: return
        val page = pages.getOrNull(idx) ?: return
        redoStack.addLast(idx to page.items)
        pages[idx] = page.copy(items = items)
        if (idx != pageIndex) pageIndex = idx
        refreshUndo(); scheduleSave(); onPagesChanged?.invoke()
    }

    fun redo() {
        val (idx, items) = redoStack.removeLastOrNull() ?: return
        val page = pages.getOrNull(idx) ?: return
        undoStack.addLast(idx to page.items)
        pages[idx] = page.copy(items = items)
        if (idx != pageIndex) pageIndex = idx
        refreshUndo(); scheduleSave(); onPagesChanged?.invoke()
    }

    private fun refreshUndo() {
        canUndo = undoStack.isNotEmpty(); canRedo = redoStack.isNotEmpty()
    }

    fun onSelection(ids: Set<Long>, bounds: RectF?) {
        selection = ids
        selectionBounds = bounds
    }

    fun deleteSelection() {
        val p = currentPage ?: return
        beginEdit()
        applyItems(p.items.filterNot { it.id in selection })
        selection = emptySet(); selectionBounds = null
    }

    fun duplicateSelection(): Set<Long> {
        val p = currentPage ?: return emptySet()
        beginEdit()
        val copies = p.items.filter { it.id in selection }.map { item ->
            val moved = item.translated(30f, 30f)
            when (moved) {
                is InkItem -> InkItem(ItemIds.next(), moved.stroke, moved.dx, moved.dy, moved.localBounds)
                is TextItem -> moved.copy(id = ItemIds.next())
                is ImageItem -> moved.copy(id = ItemIds.next())
            }
        }
        applyItems(p.items + copies)
        return copies.map { it.id }.toSet()
    }

    fun recolorSelection(color: Int) {
        val p = currentPage ?: return
        beginEdit()
        applyItems(p.items.map { item ->
            if (item.id !in selection) item
            else when (item) {
                is InkItem -> item.withStroke(androidx.ink.strokes.Stroke(Brushes.recolor(item.stroke.brush, color), item.stroke.inputs))
                is TextItem -> item.copy(color = color)
                else -> item
            }
        })
    }

    fun commitText(text: String) {
        val edit = textEdit ?: return
        textEdit = null
        val p = currentPage ?: return
        beginEdit()
        val existing = edit.existing
        val items = when {
            existing != null && text.isBlank() -> p.items.filterNot { it.id == existing.id }
            existing != null -> p.items.map { if (it.id == existing.id) existing.copy(text = text) else it }
            text.isBlank() -> return
            else -> p.items + TextItem(ItemIds.next(), text, edit.x, edit.y, textSize, penStyle.color)
        }
        applyItems(items)
    }

    fun insertImage(uri: Uri, viewW: Float) {
        viewModelScope.launch {
            val name = runCatching { repo.saveImage(noteId, uri) }.getOrNull() ?: return@launch
            val bmp = withContext(Dispatchers.IO) { renderer().image(name) } ?: return@launch
            val p = currentPage ?: return@launch
            val w = (p.width * 0.6f).coerceAtMost(bmp.width.toFloat().coerceAtLeast(200f))
            val h = w * bmp.height / bmp.width
            beginEdit()
            applyItems(p.items + ImageItem(ItemIds.next(), name, (p.width - w) / 2f, (p.height - h) / 3f, w, h))
        }
    }

    // --------------------------------------------------------------- pages

    fun goToPage(i: Int) {
        if (i in pages.indices && i != pageIndex) {
            pageIndex = i
            onPagesChanged?.invoke()
        }
    }

    fun addPage() {
        val cur = currentPage
        val paper = if (cur == null || cur.pdfPage >= 0) repo.meta(noteId)?.paper ?: PaperStyle.RULED else cur.paper
        pages.add(pageIndex + 1, Page(UUID.randomUUID().toString(), com.pronotes.app.data.PageSize.WIDTH, com.pronotes.app.data.PageSize.HEIGHT, paper))
        pageIndex += 1
        scheduleSave(); onPagesChanged?.invoke()
    }

    fun deletePage() {
        if (pages.size <= 1) return
        pages.removeAt(pageIndex)
        undoStack.clear(); redoStack.clear(); refreshUndo()
        pageIndex = pageIndex.coerceAtMost(pages.size - 1)
        scheduleSave(); onPagesChanged?.invoke()
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

    // ------------------------------------------------------------ settings

    private fun loadSettings() = InputSettings(
        stylusOnly = prefs.getBoolean("stylusOnly", true),
        sideButton = SideButtonAction.valueOf(prefs.getString("sideButton", SideButtonAction.ERASER.name)!!),
        scribbleToErase = prefs.getBoolean("scribble", true),
        holdToShape = prefs.getBoolean("holdShape", true),
        circleToLasso = prefs.getBoolean("circleLasso", true),
    )

    fun updateSettings(s: InputSettings) {
        settings = s
        prefs.edit()
            .putBoolean("stylusOnly", s.stylusOnly)
            .putString("sideButton", s.sideButton.name)
            .putBoolean("scribble", s.scribbleToErase)
            .putBoolean("holdShape", s.holdToShape)
            .putBoolean("circleLasso", s.circleToLasso)
            .apply()
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
        super.onCleared()
    }
}
