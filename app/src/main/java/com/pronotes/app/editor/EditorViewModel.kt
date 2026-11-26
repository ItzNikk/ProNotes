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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.util.Collections
import java.util.UUID

const val MAX_COLOR_SLOTS = 16

data class ToolStyle(val colors: List<Int>, val sizes: List<Float>, val color: Int, val size: Float)

val PenPalette = listOf(
    0xFF14171A, 0xFF1D4ED8, 0xFF15803D, 0xFFDC2626, 0xFFD97706, 0xFF7C3AED, 0xFF0F766E, 0xFFDB2777,
    0xFF475569, 0xFF0284C7, 0xFF65A30D, 0xFFEA580C, 0xFF9333EA, 0xFF6D4C41, 0xFF000000, 0xFFFFFFFF,
    0xFF2E4A7A, 0xFF7A2E42,
).map { it.toInt() }

val HighlighterPalette = listOf(
    0xFFFEF08A, 0xFFA7F3D0, 0xFFBAE6FD, 0xFFFECACA,
    0xFFE9D5FF, 0xFFFED7AA, 0xFFD9F99D, 0xFF99F6E4,
).map { it.toInt() }

val CrayonPalette = listOf(
    0xFFE63946, 0xFFF77F00, 0xFFFCBF49, 0xFF2A9D8F,
    0xFF3A86FF, 0xFF8338EC, 0xFFEC4899, 0xFF3D405B,
).map { it.toInt() }

val TapeColors = listOf(
    0xFF24483F, 0xFF2E4A7A, 0xFF7A2E42, 0xFFB98A2E,
    0xFF6D28D9, 0xFF0F766E, 0xFFDC2626, 0xFF64748B,
).map { it.toInt() }

val StickyColors = listOf(
    0xFFF4EEDF, 0xFFFFF3A3, 0xFFD8F5C8, 0xFFCDEBFF,
    0xFFFFD6E0, 0xFFE9D5FF, 0xFFFFE4C4, 0xFFCCFBF1,
).map { it.toInt() }

val WebLinkColors = listOf(
    0xFF24483F, 0xFF2E4A7A, 0xFF2F6B4F, 0xFF7A2E42,
    0xFFB98A2E, 0xFF3D6CF4, 0xFF7C3AED, 0xFF0F766E,
).map { it.toInt() }

val MindPalette = listOf(
    0xFF24483F, 0xFF2E4A7A, 0xFF2F6B4F, 0xFF7A2E42,
    0xFFB98A2E, 0xFF7C3AED, 0xFF0284C7, 0xFF5A5F58,
).map { it.toInt() }

class EditorViewModel(app: Application, val noteId: String) : AndroidViewModel(app) {
    internal val repo = Repository.get(app)
    internal val store = SettingsStore.get(app)
    private var recentPalette by mutableStateOf(
        store.pref("recentColors_$noteId").split(',').mapNotNull { it.toIntOrNull() }.distinct().take(MAX_COLOR_SLOTS),
    )

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
    var penStyle by mutableStateOf(style("pen", PenPalette.take(8), listOf(2f, 4f, 7f), 3.6f, 2))
    var crayonStyle by mutableStateOf(style("crayon", CrayonPalette.take(8), listOf(10f, 18f, 28f), 16f, 28))
    var highlighterStyle by mutableStateOf(style("highlighter", HighlighterPalette.take(8), listOf(16f, 28f, 42f), 22f, 3))
    var shapeStyle by mutableStateOf(style("shape", PenPalette.take(8), listOf(2f, 4f, 7f), 3.6f, 4))
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
    var systemHasClipboard by mutableStateOf(false)
        private set
    private val sysClipboard: android.content.ClipboardManager? =
        app.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    private var lastInternalCopyMs: Long = 0L
    private var lastMirroredSystemClipTimestamp: Long = -1L
    private var lastExternalClipChangeMs: Long = 0L
    private var suppressNextSystemClipEvent: Boolean = false
    private val systemClipListener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
        if (suppressNextSystemClipEvent) {
            suppressNextSystemClipEvent = false
            systemHasClipboard = runCatching { sysClipboard?.hasPrimaryClip() == true }.getOrDefault(false)
            return@OnPrimaryClipChangedListener
        }
        lastExternalClipChangeMs = System.currentTimeMillis()
        refreshSystemClipboardState()
    }
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

    internal val undoStack = ArrayDeque<Map<Int, List<PageItem>>>()
    internal val redoStack = ArrayDeque<Map<Int, List<PageItem>>>()
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
        runCatching { sysClipboard?.addPrimaryClipChangedListener(systemClipListener) }
        refreshSystemClipboardState()
        viewModelScope.launch {
            val loadedPages = withContext(Dispatchers.IO) { repo.loadPages(noteId) }
            startPage = store.pref("page_$noteId", "0").toIntOrNull()?.coerceIn(0, (loadedPages.size - 1).coerceAtLeast(0)) ?: 0
            pageIndex = startPage
            pages.clear(); pages.addAll(loadedPages)
            if (repo.pdfFile(noteId).exists()) pdf = PdfPages(repo.pdfFile(noteId))
            loaded = true
            onPagesChanged?.invoke()

            // Secondary metadata, caches & handwriting ML Kit model load in the background without delaying the canvas.
            launch(Dispatchers.IO) { loadStudy() }
            launch(Dispatchers.IO) { repo.markOpened(noteId) }
            launch(Dispatchers.IO) {
                runCatching {
                    HandwritingRecognizer.ensureModelReady(settingsFlow.value.toolDefaults.handwritingLanguage)
                }
            }
            prefetchPdf(startPage)
        }
    }

    private fun style(key: String, colors: List<Int>, sizes: List<Float>, size: Float, slot: Int): ToolStyle {
        val c = store.swatches("${key}_$noteId", store.swatches(key, colors))
        val sel = saved.getOrNull(slot)?.split(',')
        val color = if (key == "shape") penStyle.color else sel?.getOrNull(0)?.toIntOrNull() ?: c.first()
        val sz = sel?.getOrNull(1)?.toFloatOrNull() ?: size
        val widths = saved.getOrNull(slot + 15)?.split(',')?.mapNotNull { it.toFloatOrNull()?.takeIf { w -> w.isFinite() && w > 0f } }
        val palette = if (color in c) c else (c + color).takeLast(MAX_COLOR_SLOTS)
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
            maxSpan <= 1 -> intArrayOf(0, 1, -1)
            maxSpan <= 3 -> intArrayOf(0, 1, -1, 2, -2)
            else -> intArrayOf(0, 1, -1, 2, -2, 3, -3, 4)
        }
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(45)
            for (d in offsets) {
                val page = pages.getOrNull(center + d) ?: continue
                if (page.pdfPage < 0 || src.cached(page.pdfPage) != null) continue
                // Do not compete with a page that is on screen right now.
                while (pendingPdf.isNotEmpty()) kotlinx.coroutines.delay(25)
                val w = 1080
                val h = (w * page.height / page.width).toInt().coerceAtLeast(1)
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
        val palette = if (color in current.colors) current.colors else (current.colors + color).takeLast(MAX_COLOR_SLOTS)
        val updated = current.copy(colors = palette, color = color)
        if (target == Tool.PEN) penStyle = updated else shapeStyle = updated
        val key = if (target == Tool.PEN) "pen" else "shape"
        store.saveSwatches("${key}_$noteId", palette)
        pushRecent(color)
    }

    fun updateStyle(t: Tool, s: ToolStyle) {
        val previous = styleFor(t) ?: return
        val colors = s.colors.distinct().take(MAX_COLOR_SLOTS).ifEmpty { listOf(s.color) }
        val next = s.copy(colors = colors)
        if (previous == next) return
        if (previous.color != next.color) {
            if (t == Tool.PEN || t == Tool.SHAPE) shareInkColor(t, next.color)
            else pushRecent(next.color)
        }
        val key = when (t) {
            Tool.PEN -> { penStyle = next; "pen" }
            Tool.CRAYON -> { crayonStyle = next; "crayon" }
            Tool.HIGHLIGHTER -> { highlighterStyle = next; "highlighter" }
            Tool.SHAPE -> { shapeStyle = next; "shape" }
            else -> return
        }
        // Size, opacity and active-color changes must not rewrite an unchanged palette.
        if (previous.colors != next.colors) store.saveSwatches("${key}_$noteId", next.colors)
    }
    fun shapeFill(): Int? = if (shapeFillOn) shapeFillColor else null

    fun addSwatch(t: Tool, color: Int) {
        val s = styleFor(t) ?: return
        if (color !in s.colors && s.colors.size >= MAX_COLOR_SLOTS) return
        updateStyle(t, s.copy(colors = (s.colors + color).distinct(), color = color))
        pushRecent(color)
    }

    fun removeSwatch(t: Tool, index: Int) {
        val s = styleFor(t) ?: return
        if (s.colors.size <= 1 || index !in s.colors.indices) return
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

    private var zoomJob: Job? = null

    fun onZoom(p: Int?) {
        zoomPercent = p
        zoomJob?.cancel()
        if (p != null) {
            zoomJob = viewModelScope.launch {
                delay(1200)
                zoomPercent = null
            }
        }
    }

    fun copySelection() {
        val p = pages.getOrNull(selectionPage) ?: return
        val picked = p.items.filter { it.id in selection }
        if (picked.isEmpty()) return
        clipboard = picked
        hasClipboard = true
        lastInternalCopyMs = System.currentTimeMillis()
        mirrorSelectionToSystemClipboard(picked)
        say("Copied")
    }

    private fun mirrorSelectionToSystemClipboard(items: List<PageItem>) {
        val cm = sysClipboard ?: return
        val app = getApplication<Application>()
        val extractedText = items.mapNotNull { item ->
            when (item) {
                is TextItem -> item.text.trim().takeIf { it.isNotEmpty() }
                is StickyItem -> item.text.trim().takeIf { it.isNotEmpty() }
                is WebLinkItem -> item.url.trim().takeIf { it.isNotEmpty() }
                is MindNodeItem -> item.label.trim().takeIf { it.isNotEmpty() }
                is WriteBoxItem -> item.convertedText.trim().takeIf { it.isNotEmpty() }
                else -> null
            }
        }.joinToString("\n\n")

        if (extractedText.isNotBlank()) {
            runCatching {
                suppressNextSystemClipEvent = true
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Pro Notes", extractedText))
                lastMirroredSystemClipTimestamp = cm.primaryClipDescription?.timestamp ?: System.currentTimeMillis()
                systemHasClipboard = true
            }
        } else {
            // Render ink/image selection to a cached PNG and share its content URI to the phone clipboard
            viewModelScope.launch {
                val bmp = selectionImage() ?: return@launch
                val uri = withContext(Dispatchers.IO) {
                    runCatching {
                        val dir = File(app.cacheDir, "clipboard").apply { mkdirs() }
                        val out = File(dir, "pronotes_clip.png")
                        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
                        androidx.core.content.FileProvider.getUriForFile(app, app.packageName + ".files", out)
                    }.getOrNull()
                }
                bmp.recycle()
                if (uri != null) {
                    runCatching {
                        suppressNextSystemClipEvent = true
                        cm.setPrimaryClip(android.content.ClipData.newUri(app.contentResolver, "Pro Notes Selection", uri))
                        lastMirroredSystemClipTimestamp = cm.primaryClipDescription?.timestamp ?: System.currentTimeMillis()
                        systemHasClipboard = true
                    }
                }
            }
        }
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
    fun resizeSelection(factor: Float) {
        if (!factor.isFinite() || factor !in 0.1f..5f) return
        transformSelection { bounds ->
            android.graphics.Matrix().apply { setScale(factor, factor, bounds.centerX(), bounds.centerY()) } to (factor to 0f)
        }
    }

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
        val colors = if (p.color in penStyle.colors) penStyle.colors else (penStyle.colors + p.color).takeLast(MAX_COLOR_SLOTS)
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

    /** Refreshes Android system clipboard status (call on resume when Android grants clipboard focus). */
    fun refreshSystemClipboardState() {
        val cm = sysClipboard ?: return
        val hasPrimary = runCatching { cm.hasPrimaryClip() }.getOrDefault(false)
        systemHasClipboard = hasPrimary
        if (hasPrimary) {
            val ts = runCatching { cm.primaryClipDescription?.timestamp ?: 0L }.getOrDefault(0L)
            if (ts > 0L && ts != lastMirroredSystemClipTimestamp && ts > lastInternalCopyMs) {
                lastExternalClipChangeMs = maxOf(lastExternalClipChangeMs, ts)
            }
        }
    }

    /** True when either internal selection clipboard or Android whole-phone system clipboard has content. */
    fun hasAnyClipboardContent(): Boolean {
        if (clipboard.isNotEmpty() || systemHasClipboard) return true
        val hasSys = runCatching { sysClipboard?.hasPrimaryClip() == true }.getOrDefault(false)
        if (hasSys != systemHasClipboard) systemHasClipboard = hasSys
        return hasSys
    }

    /** Returns true if the phone's system clipboard was updated more recently than our in-app selection copy. */
    private fun shouldPreferSystemClipboard(): Boolean {
        if (clipboard.isEmpty()) return true
        val cm = sysClipboard ?: return false
        if (!runCatching { cm.hasPrimaryClip() }.getOrDefault(false)) return false
        val descTs = runCatching { cm.primaryClipDescription?.timestamp ?: 0L }.getOrDefault(0L)
        if (descTs > 0L && descTs != lastMirroredSystemClipTimestamp && descTs > lastInternalCopyMs) {
            return true
        }
        return lastExternalClipChangeMs > lastInternalCopyMs
    }

    /** Pastes from either the phone's Android system clipboard (text, HTML, images/screenshots) or in-app selection clipboard. */
    fun paste(onPlaced: (Int, Set<Long>) -> Unit = { _, _ -> }): Pair<Int, Set<Long>>? =
        pasteInto(pageIndex, null, null, onPlaced)

    /** Pastes clipboard items (whole-phone image/text/HTML or in-app selection) centred around ([x], [y]) on [page]. */
    fun pasteAt(page: Int, x: Float, y: Float, onPlaced: (Int, Set<Long>) -> Unit = { _, _ -> }): Pair<Int, Set<Long>>? =
        pasteInto(page, x, y, onPlaced)

    private fun pasteInto(
        page: Int,
        x: Float?,
        y: Float?,
        onPlaced: (Int, Set<Long>) -> Unit,
    ): Pair<Int, Set<Long>>? {
        if (readOnly) return null
        val idx = page.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        val p = pages.getOrNull(idx) ?: return null

        fun pasteInternal(): Pair<Int, Set<Long>>? {
            if (clipboard.isEmpty()) return null
            val copies = if (x != null && y != null) {
                val union = RectF(clipboard.first().bounds).apply {
                    clipboard.drop(1).forEach { union(it.bounds) }
                }
                val dx = x - union.centerX()
                val dy = y - union.centerY()
                clipboard.map { it.translated(dx, dy).withId(ItemIds.next()) }
            } else {
                val nudged = clipboard.map { it.translated(40f, 40f).withId(ItemIds.next()) }
                clipboard = nudged
                nudged
            }
            beginEdit(idx)
            applyItems(idx, p.items + copies)
            val ids = copies.map { it.id }.toSet()
            onPlaced(idx, ids)
            return idx to ids
        }

        fun pasteFromSystem(): Pair<Int, Set<Long>>? {
            val app = getApplication<Application>()
            val cm = sysClipboard ?: return null
            val clip = runCatching { cm.primaryClip }.getOrNull() ?: return null
            if (clip.itemCount == 0) return null

            // 1. Check all clip items for pasted images / screenshots / stickers from anywhere on the phone
            val imageUris = ArrayList<Uri>()
            val descIsImage = clip.description?.let { d ->
                (0 until d.mimeTypeCount).any { d.getMimeType(it).startsWith("image/") }
            } == true
            for (i in 0 until clip.itemCount) {
                val uri = clip.getItemAt(i).uri ?: continue
                val mime = runCatching { app.contentResolver.getType(uri) }.getOrNull().orEmpty()
                val path = uri.toString().lowercase()
                val looksLikeImg = mime.startsWith("image/") ||
                    descIsImage ||
                    path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".webp") || path.endsWith(".gif")
                if (looksLikeImg) {
                    imageUris.add(uri)
                }
            }
            if (imageUris.isNotEmpty()) {
                val baseX = x ?: (p.width / 2f)
                val baseY = y ?: (p.height * 0.36f)
                imageUris.forEachIndexed { i, uri ->
                    insertImage(uri, idx, baseX + i * 24f, baseY + i * 24f) { placedPage, id ->
                        onPlaced(placedPage, setOf(id))
                    }
                }
                say(if (imageUris.size == 1) "Pasted image from clipboard" else "Pasted ${imageUris.size} images")
                return null // Async placement notifies via onPlaced
            }

            // 2. Extract plain text, rich HTML text, or URI text from the phone's clipboard
            val parts = ArrayList<String>()
            for (i in 0 until clip.itemCount) {
                val clipItem = clip.getItemAt(i)
                val raw = when {
                    !clipItem.text.isNullOrBlank() -> clipItem.text.toString()
                    !clipItem.htmlText.isNullOrBlank() -> android.text.Html.fromHtml(
                        clipItem.htmlText,
                        android.text.Html.FROM_HTML_MODE_COMPACT,
                    ).toString()
                    else -> runCatching { clipItem.coerceToText(app)?.toString() }.getOrNull().orEmpty()
                }.trim()
                if (raw.isNotEmpty()) parts.add(raw)
            }
            val clipText = parts.joinToString("\n\n").trim()
            if (clipText.isNotEmpty()) {
                val targetX = (x ?: (p.width * 0.10f)).coerceIn(36f, (p.width - 220f).coerceAtLeast(36f))
                val targetY = (y ?: (p.height * 0.18f)).coerceIn(36f, (p.height - 96f).coerceAtLeast(36f))
                val maxWidth = (p.width - targetX - 36f).coerceAtLeast(240f)
                val wrappedText = wrapPastedText(clipText, textSize, textStyle, maxWidth)
                val item = TextItem(
                    id = ItemIds.next(),
                    text = wrappedText,
                    x = targetX,
                    y = targetY,
                    size = textSize,
                    color = textColor,
                    style = textStyle,
                )
                beginEdit(idx)
                applyItems(idx, p.items + item)
                val ids = setOf(item.id)
                onPlaced(idx, ids)
                say("Pasted from clipboard")
                return idx to ids
            }
            return null
        }

        val preferSystem = shouldPreferSystemClipboard()
        if (preferSystem) {
            val sysResult = pasteFromSystem()
            if (sysResult != null) return sysResult
            // Note: async image paste already triggered insertImage if clip had image URIs
            val hasSysImages = runCatching {
                val c = sysClipboard?.primaryClip
                c != null && (0 until c.itemCount).any { c.getItemAt(it).uri != null }
            }.getOrDefault(false)
            if (hasSysImages) return null
            return pasteInternal() ?: run {
                say("Nothing to paste")
                null
            }
        } else {
            return pasteInternal() ?: pasteFromSystem() ?: run {
                say("Nothing to paste")
                null
            }
        }
    }

    /** Wraps long pasted paragraphs cleanly so text copied from browsers/apps fits inside the page width. */
    private fun wrapPastedText(text: String, fontSize: Float, style: TextStyleSpec, maxWidth: Float): String {
        val approxCharW = (fontSize * 0.56f).coerceAtLeast(8f)
        val maxCharsPerLine = (maxWidth / approxCharW).toInt().coerceIn(18, 96)
        val out = StringBuilder(text.length + 32)
        val paragraphs = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        paragraphs.forEachIndexed { pIdx, para ->
            if (pIdx > 0) out.append('\n')
            if (para.length <= maxCharsPerLine) {
                out.append(para)
            } else {
                var lineLen = 0
                val words = para.split(Regex("(?<=\\s)"))
                for (word in words) {
                    if (lineLen > 0 && lineLen + word.length > maxCharsPerLine) {
                        out.append('\n')
                        val trimmed = word.trimStart()
                        out.append(trimmed)
                        lineLen = trimmed.length
                    } else {
                        out.append(word)
                        lineLen += word.length
                    }
                }
            }
        }
        return out.toString().trimEnd()
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

    fun bookmarkPages(ids: Set<String>, marked: Boolean) {
        if (readOnly) return
        for (i in pages.indices) {
            val page = pages[i]
            if (page.id in ids && page.bookmarked != marked) {
                pages[i] = page.copy(bookmarked = marked)
                dirtyPageIds += page.id
            }
        }
        scheduleSave()
        onPagesChanged?.invoke()
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

    fun recentColors(): List<Int> = recentPalette
    fun pushRecent(c: Int) {
        val next = (listOf(c) + recentPalette).distinct().take(MAX_COLOR_SLOTS)
        if (next == recentPalette) return
        recentPalette = next
        store.setPref("recentColors_$noteId", next.joinToString(","))
    }
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
        recordEdit(mapOf(page to p.items))
    }

    private fun recordEdit(previous: Map<Int, List<PageItem>>) {
        undoStack.addLast(previous)
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

    internal fun commitPageItemsEdit(changes: Map<Int, List<PageItem>>) {
        if (changes.isEmpty() || changes.keys.any { it !in pages.indices }) return
        recordEdit(changes.keys.associateWith { pages[it].items })
        touched()
        replacePageItems(changes)
        scheduleSave()
    }

    private fun replacePageItems(changes: Map<Int, List<PageItem>>) {
        changes.forEach { (index, items) ->
            val page = pages[index]
            pages[index] = page.copy(items = items)
            dirtyPageIds += page.id
        }
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

    fun undo() = swap(undoStack, redoStack, showDestination = false)
    fun redo() = swap(redoStack, undoStack, showDestination = true)

    private fun swap(
        from: ArrayDeque<Map<Int, List<PageItem>>>,
        to: ArrayDeque<Map<Int, List<PageItem>>>,
        showDestination: Boolean,
    ) {
        val changes = from.removeLastOrNull() ?: return
        if (changes.keys.any { it !in pages.indices }) {
            refreshUndo()
            return
        }
        to.addLast(changes.keys.associateWith { pages[it].items })
        replacePageItems(changes)
        refreshUndo(); scheduleSave(); onPagesChanged?.invoke()
        val idx = if (showDestination) changes.keys.last() else changes.keys.first()
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
        val page = pages.getOrNull(selectionPage) ?: return
        deletePageItems(page.id, selection)
    }

    internal fun deletePageItems(pageId: String, ids: Set<Long>) {
        val index = pages.indexOfFirst { it.id == pageId }
        val page = pages.getOrNull(index) ?: return
        if (readOnly || page.items.none { it.id in ids }) return
        beginEdit(index)
        applyItems(index, page.items.filterNot { it.id in ids })
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
                is InkItem -> item.withStroke(Stroke(Brushes.recolor(item.stroke.brush, color, item.brushFamily), item.stroke.inputs))
                is TextItem -> item.copy(color = color)
                is TapeItem -> item.copy(color = color)
                is StickyItem -> item.copy(color = color)
                is WebLinkItem -> item.copy(color = color)
                is WriteBoxItem -> item.copy(
                    strokeColor = (color and 0x00FFFFFF) or (0x88 shl 24),
                    fillColor = (color and 0x00FFFFFF) or (0x14 shl 24),
                )
                // Fills keep their transparency when recoloured.
                is FillItem -> item.recolored((color and 0x00FFFFFF) or (item.color and 0xFF000000.toInt()))
                else -> item
            }
        })
    }

    /** Returns true if the current lasso selection includes any text, sticky note, or write box. */
    fun selectionHasStylableText(): Boolean {
        val p = pages.getOrNull(selectionPage) ?: return false
        return p.items.any { it.id in selection && (it is TextItem || it is StickyItem || it is WriteBoxItem) }
    }

    /** Returns the active (TextFont, customFontPath) of the first selected text/sticky/writebox. */
    fun selectionActiveFont(): Pair<TextFont, String> {
        val p = pages.getOrNull(selectionPage) ?: return textStyle.font to textStyle.customFontPath
        val first = p.items.firstOrNull { it.id in selection && (it is TextItem || it is StickyItem || it is WriteBoxItem) }
        return when (first) {
            is TextItem -> first.style.font to first.style.customFontPath
            is StickyItem -> {
                if (first.font.contains('/') || first.font.endsWith(".ttf", true) || first.font.endsWith(".otf", true)) {
                    TextFont.SANS to first.font
                } else {
                    (TextFont.entries.firstOrNull { it.name.equals(first.font, ignoreCase = true) } ?: TextFont.SANS) to ""
                }
            }
            is WriteBoxItem -> first.font to ""
            else -> textStyle.font to textStyle.customFontPath
        }
    }

    /**
     * Changes the font of all selected TextItems, StickyItems, and WriteBoxItems in one undoable edit.
     * If the selection contains only handwritten InkItems, converts the handwriting into typed text
     * using the chosen font directly.
     */
    fun setSelectionFont(
        font: TextFont,
        customFontPath: String = "",
        onConverted: (Long) -> Unit = {},
    ) {
        val p = pages.getOrNull(selectionPage) ?: return
        val selected = p.items.filter { it.id in selection }
        if (selected.isEmpty()) return
        val hasText = selected.any { it is TextItem || it is StickyItem || it is WriteBoxItem }
        if (!hasText) {
            if (selected.any { it is InkItem }) {
                convertSelectionToText(onConverted = onConverted, outputFont = font, customFontPath = customFontPath)
            } else {
                say("Select text, sticky note, or handwriting first")
            }
            return
        }
        val stickyFontKey = if (customFontPath.isNotBlank()) customFontPath else font.name
        beginEdit(selectionPage)
        applyItems(selectionPage, p.items.map { item ->
            if (item.id !in selection) item
            else when (item) {
                is TextItem -> item.copy(style = item.style.copy(font = font, customFontPath = customFontPath))
                is StickyItem -> item.copy(font = stickyFontKey)
                is WriteBoxItem -> item.copy(font = font)
                else -> item
            }
        })
        textStyle = textStyle.copy(font = font, customFontPath = customFontPath)
    }

    fun toggleSelectionBold() {
        val p = pages.getOrNull(selectionPage) ?: return
        val texts = p.items.filter { it.id in selection && it is TextItem }.map { it as TextItem }
        if (texts.isEmpty()) return
        val targetBold = !texts.all { it.style.bold }
        beginEdit(selectionPage)
        applyItems(selectionPage, p.items.map { item ->
            if (item.id in selection && item is TextItem) item.copy(style = item.style.copy(bold = targetBold)) else item
        })
    }

    fun toggleSelectionItalic() {
        val p = pages.getOrNull(selectionPage) ?: return
        val texts = p.items.filter { it.id in selection && it is TextItem }.map { it as TextItem }
        if (texts.isEmpty()) return
        val targetItalic = !texts.all { it.style.italic }
        beginEdit(selectionPage)
        applyItems(selectionPage, p.items.map { item ->
            if (item.id in selection && item is TextItem) item.copy(style = item.style.copy(italic = targetItalic)) else item
        })
    }

    fun scaleSelectionFontSize(delta: Float) {
        val p = pages.getOrNull(selectionPage) ?: return
        val hasAny = p.items.any { it.id in selection && (it is TextItem || it is StickyItem || it is WriteBoxItem) }
        if (!hasAny) return
        beginEdit(selectionPage)
        applyItems(selectionPage, p.items.map { item ->
            if (item.id !in selection) item
            else when (item) {
                is TextItem -> item.copy(size = (item.size + delta).coerceIn(14f, 180f))
                is StickyItem -> item.copy(fontSize = (item.fontSize + delta).coerceIn(14f, 64f))
                is WriteBoxItem -> item.copy(fontSize = (item.fontSize + delta).coerceIn(14f, 64f))
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
                    is WriteBoxItem -> item.copy(
                        strokeColor = (c and 0x00FFFFFF) or (0x88 shl 24),
                        fillColor = (c and 0x00FFFFFF) or (0x14 shl 24),
                    )
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
    var handwritingGlow by mutableStateOf<Pair<String, RectF>?>(null)
        private set
    internal var animateHandwritingConversion = false

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
    fun convertSelectionToText(
        onConverted: (Long) -> Unit = {},
        outputFont: TextFont? = null,
        customFontPath: String? = null,
    ) {
        val pageIdx = selectionPage
        val p = pages.getOrNull(pageIdx) ?: return
        val selectedInk = p.items.filter { it.id in selection && it is InkItem }.map { it as InkItem }
        if (selectedInk.isEmpty()) {
            say("Select handwritten ink first")
            return
        }
        viewModelScope.launch {
            recognizeInk(
                pageId = p.id,
                selectedInk = selectedInk,
                outputFont = outputFont,
                automatic = false,
                overrideCustomFontPath = customFontPath,
                onConverted = onConverted,
            )
        }
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

    var straightenInk by mutableStateOf(store.pref("straightenInk_$noteId") == "true")
        private set

    fun toggleInkStraightening() {
        straightenInk = !straightenInk
        if (straightenInk && settingsFlow.value.toolDefaults.autoHandwriting) setAutoHandwriting(false)
        store.setPref("straightenInk_$noteId", straightenInk.toString())
    }

    fun queueHandwriting(page: Int, ink: List<InkItem>) {
        val viaAutoTool = tool == Tool.AUTO_TEXT
        if (ink.isEmpty() || readOnly || (!settingsFlow.value.toolDefaults.autoHandwriting && !viaAutoTool && !straightenInk)) return
        val sheet = pages.getOrNull(page) ?: return
        pendingInk.getOrPut(sheet.id) { mutableListOf() }.addAll(ink)
        writingPause?.cancel()
        writingPause = viewModelScope.launch {
            do { delay(700) } while (penActive || handwritingBusy)
            val options = settingsFlow.value.toolDefaults
            if ((!options.autoHandwriting && tool != Tool.AUTO_TEXT && !straightenInk) || readOnly) {
                pendingInk.clear()
                return@launch
            }
            val font = TextFont.entries.find { it.name == options.autoScript } ?: TextFont.CURSIVE
            for ((id, strokes) in pendingInk.toMap()) {
                val current = pages.firstOrNull { it.id == id }
                val surviving = strokes.filter { current?.items?.contains(it) == true }
                for (line in handwritingLines(surviving)) {
                    while (penActive) delay(150)
                    if (options.autoHandwriting || tool == Tool.AUTO_TEXT) {
                        recognizeInk(id, line, font, automatic = true) {}
                    } else if (straightenInk) {
                        val idx = pages.indexOfFirst { it.id == id }
                        val sheetNow = pages.getOrNull(idx) ?: continue
                        val leveled = levelInkLine(sheetNow, line) ?: continue
                        beginEdit(idx)
                        applyItems(idx, sheetNow.items.map { leveled[it.id] ?: it })
                    }
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
        overrideCustomFontPath: String? = null,
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
        val showGlow = animateHandwritingConversion
        val started = android.os.SystemClock.uptimeMillis()
        if (showGlow) handwritingGlow = pageId to union
        try {
            val result = HandwritingRecognizer.recognize(
                strokes = timedStrokes,
                writingWidth = union.width().coerceAtLeast(64f),
                writingHeight = union.height().coerceAtLeast(48f),
                languageTag = settingsFlow.value.toolDefaults.handwritingLanguage,
                allowFallback = true,
            )
            if (showGlow && result.text.isNotBlank()) {
                val remaining = 100L - (android.os.SystemClock.uptimeMillis() - started)
                if (remaining > 0L) delay(remaining)
            }
            val options = settingsFlow.value.toolDefaults
            val idx = pages.indexOfFirst { it.id == pageId }
            val page = pages.getOrNull(idx) ?: return
            if (readOnly || !page.items.containsAll(selectedInk)) return
            val rawText = if (automatic) {
                result.text.trim().lines().joinToString("\n") { it.trim().replace(Regex("\\s+"), " ") }.trim()
            } else {
                result.text.trim()
            }
            val text = if (options.autoSpellCheck) AutoSpellChecker.autoCorrect(rawText, options.handwritingLanguage) else rawText
            val hasRecognizableContent = text.any { it.isLetterOrDigit() || it in HandwritingRecognizer.RECOGNIZABLE_SYMBOLS }
            val targetSticky = page.items.filterIsInstance<StickyItem>().lastOrNull { s ->
                !s.collapsed && (s.bounds.contains(union.centerX(), union.centerY()) || RectF.intersects(s.bounds, union))
            }
            if (targetSticky != null) {
                // Writing on an expanded sticky note with Auto Handwriting converts straight into the sticky note's text!
                if (automatic && ((!options.autoHandwriting && tool != Tool.AUTO_TEXT) || penActive)) return
                if (text.isBlank() || (automatic && !hasRecognizableContent)) return
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
            if (automatic && ((!options.autoHandwriting && tool != Tool.AUTO_TEXT) || penActive || !hasRecognizableContent)) return
            if (text.isBlank()) {
                say("Could not recognise handwritten ink")
                return
            }
            // Dynamic Bold: automatically bold larger written headings/emphasis text relative to base size
            val shouldDynamicBold = options.dynamicBoldHandwriting &&
                (estimatedSize >= (textSize * 1.28f) || union.height() >= 68f)
            val customFont = overrideCustomFontPath ?: options.customHandwritingFontPath
            val baseStyle = if (automatic) {
                TextStyleSpec(
                    font = outputFont ?: TextFont.CURSIVE,
                    bold = shouldDynamicBold,
                    customFontPath = customFont,
                )
            } else {
                (outputFont?.let { textStyle.copy(font = it) } ?: textStyle).copy(
                    bold = textStyle.bold || shouldDynamicBold,
                    customFontPath = overrideCustomFontPath ?: textStyle.customFontPath.ifBlank { options.customHandwritingFontPath },
                )
            }
            val ids = selectedInk.map { it.id }.toSet()
            val cleanedText = text.lines()
                .map { it.trim().replace(Regex("\\s+"), " ") }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
                .ifBlank { text.trim() }

            // 1. Inline line continuation: if user is writing immediately to the right of an existing TextItem
            // on the same horizontal baseline, merge cleanly onto that exact line instead of scattering boxes.
            val sameLineTarget = if (!cleanedText.contains('\n')) {
                findSameLineTextTarget(page, union, ids)
            } else null

            if (sameLineTarget != null && (automatic || union.left - sameLineTarget.raw.right <= sameLineTarget.size * 4.5f)) {
                val mergedRaw = smartJoinInlineText(sameLineTarget.text, cleanedText)
                val maxW = (page.width - sameLineTarget.x - 32f).coerceAtLeast(180f)
                val wrapped = wrapPastedText(mergedRaw, sameLineTarget.size, sameLineTarget.style, maxW)
                val updated = sameLineTarget.copy(text = wrapped)
                if (!automatic) handwritingCandidates = result.candidates
                beginEdit(idx)
                applyItems(
                    idx,
                    page.items.filterNot { it.id in ids }.map { if (it.id == sameLineTarget.id) updated else it },
                )
                if (automatic) {
                    val alternatives = result.candidates.map { it.trim().replace(Regex("\\s+"), " ") }
                        .filter { it.isNotBlank() && it != cleanedText }.distinct().take(4)
                        .map { alt ->
                            wrapPastedText(smartJoinInlineText(sameLineTarget.text, alt), sameLineTarget.size, sameLineTarget.style, maxW)
                        }
                    handwritingReview = HandwritingReview(page.id, updated, selectedInk, alternatives)
                }
                onConverted(updated.id)
                return
            }

            // 2. Paragraph / line alignment: snap to left margin & font size of the line directly above
            // (if writing consecutive lines), or snap neatly onto paper lines so text sits in crisp straight lines.
            val aboveTarget = findParagraphAboveTarget(page, union, ids)
            val baseX = when {
                aboveTarget != null -> aboveTarget.x
                options.alignHandwriting && page.paper == PaperStyle.CORNELL -> (page.width * 0.28f).coerceAtLeast(union.left)
                else -> union.left
            }.coerceIn(28f, (page.width - 140f).coerceAtLeast(28f))

            val uniformSize = when {
                aboveTarget != null && !shouldDynamicBold -> aboveTarget.size
                else -> estimatedSize.coerceIn(22f, 52f)
            }
            val maxW = (page.width - baseX - 32f).coerceAtLeast(160f)
            val wrappedFinal = wrapPastedText(cleanedText, uniformSize, baseStyle, maxW)

            val layout = singleLineLayout(
                page = page,
                text = wrappedFinal.substringBefore('\n'),
                font = baseStyle.font,
                x = baseX,
                union = union,
                estimatedSize = uniformSize,
                alignToLines = options.alignHandwriting || !automatic,
            ) ?: (uniformSize to union.top.coerceAtLeast(24f))

            val alignedY = when {
                aboveTarget != null && !options.alignHandwriting -> {
                    val lineGap = (aboveTarget.size * 0.34f).coerceIn(8f, 22f)
                    val snappedBelow = aboveTarget.raw.bottom + lineGap
                    if (kotlin.math.abs(layout.second - snappedBelow) <= aboveTarget.size * 1.6f) snappedBelow else layout.second
                }
                else -> layout.second
            }.coerceIn(24f, (page.height - layout.first * 1.4f).coerceAtLeast(24f))

            val item = TextItem(
                id = ItemIds.next(),
                text = wrappedFinal,
                x = baseX,
                y = alignedY,
                size = layout.first,
                color = selectedInk.first().stroke.brush.colorIntArgb,
                style = baseStyle,
            )
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
            handwritingGlow = null
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
            val w = 420f
            val h = 360f
            val sx = (x - w / 2f).coerceIn(16f, (p.width - w - 16f).coerceAtLeast(16f))
            val sy = (y - h / 2f).coerceIn(16f, (p.height - h - 16f).coerceAtLeast(16f))
            val placed = StickyItem(ItemIds.next(), sx, sy, w, h, stickyColor, "", fontSize = 24f)
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

    fun toggleStickyPin(page: Int, sticky: StickyItem) {
        val p = pages.getOrNull(page) ?: return
        val updated = sticky.copy(pinned = !sticky.pinned)
        beginEdit(page)
        applyItems(page, p.items.map { if (it.id == sticky.id) updated else it })
        say(if (updated.pinned) "Sticky note locked in place" else "Sticky note unlocked")
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

    fun addPage(atEnd: Boolean = false, paper: PaperStyle? = null, isWhiteboard: Boolean? = null, autoScroll: Boolean = true) {
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
        if (autoScroll) onScrollTo?.invoke(at)
    }

    fun deletePage(pageId: String) {
        if (readOnly || pages.size <= 1) return
        val index = pages.indexOfFirst { it.id == pageId }
        if (index < 0) return
        pages.removeAt(index)
        structureDirty = true
        undoStack.clear(); redoStack.clear(); refreshUndo()
        pageIndex = (pageIndex - if (index < pageIndex) 1 else 0).coerceAtMost(pages.size - 1)
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

    private data class CachedHandwritingLine(
        val bounds: RectF,
        val primaryText: String,
        val allCandidates: List<String>,
        val wordBoxes: List<Pair<RectF, String>>,
    )

    /** Per-page cached handwriting recognition results (keyed by pageId + ink fingerprint) for instant handwritten search. */
    private val handwritingSearchCache = java.util.concurrent.ConcurrentHashMap<String, List<CachedHandwritingLine>>()

    private fun normalizeForSearch(raw: String): String {
        val nfd = java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFD)
        val sb = StringBuilder(nfd.length)
        var lastSpace = false
        for (ch in nfd) {
            if (Character.getType(ch) == Character.NON_SPACING_MARK.toInt()) continue
            if (ch.isWhitespace()) {
                if (!lastSpace && sb.isNotEmpty()) {
                    sb.append(' ')
                    lastSpace = true
                }
            } else {
                lastSpace = false
                sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString().trim()
    }

    /** Confusable-tolerant form for handwriting OCR (0/o, 1/l/i, 5/s, 2/z, rn/m). */
    private fun ocrFuzzyFold(s: String): String = s
        .replace("rn", "m")
        .replace('0', 'o')
        .replace('1', 'l')
        .replace('i', 'l')
        .replace('5', 's')
        .replace('2', 'z')

    private fun tokenizeForSearch(norm: String): List<String> =
        norm.split(Regex("[\\s,;:()\\[\\]{}\"']+")).filter { it.isNotEmpty() }

    /**
     * Evaluates how well [haystack] matches [queryNorm] / [queryTokens].
     * Returns match score (>0 on match, 0 on no match):
     *   100 = exact phrase match
     *    86 = all multi-word tokens matched (exact/prefix)
     *    74 = OCR-confusable or fuzzy edit-distance match
     */
    private fun scoreTextMatch(haystackRaw: String, queryNorm: String, queryTokens: List<String>): Int {
        if (haystackRaw.isBlank() || queryNorm.isEmpty()) return 0
        val hayNorm = normalizeForSearch(haystackRaw)
        if (hayNorm.isEmpty()) return 0
        if (hayNorm.contains(queryNorm)) return 100

        val hayOcr = ocrFuzzyFold(hayNorm)
        val qOcr = ocrFuzzyFold(queryNorm)
        if (qOcr.length >= 2 && hayOcr.contains(qOcr)) return 88

        val hayTokens = tokenizeForSearch(hayNorm)
        if (hayTokens.isEmpty() || queryTokens.isEmpty()) return 0

        var totalQuality = 0
        for (qt in queryTokens) {
            val qtOcr = ocrFuzzyFold(qt)
            val maxDist = when {
                qt.length >= 7 -> 2
                qt.length >= 4 -> 1
                else -> 0
            }
            var bestTokenScore = 0
            for (ht in hayTokens) {
                if (ht == qt) {
                    bestTokenScore = 100; break
                }
                if (ht.startsWith(qt) || ht.contains(qt)) {
                    bestTokenScore = maxOf(bestTokenScore, 90)
                } else if (qt.length >= 2 && ocrFuzzyFold(ht).contains(qtOcr)) {
                    bestTokenScore = maxOf(bestTokenScore, 82)
                } else if (maxDist > 0 && kotlin.math.abs(ht.length - qt.length) <= maxDist + 1) {
                    val d = AutoSpellChecker.editDistance(ht, qt)
                    if (d <= maxDist) {
                        bestTokenScore = maxOf(bestTokenScore, if (d == 1) 76 else 68)
                    } else if (ocrFuzzyFold(ht).let { AutoSpellChecker.editDistance(it, qtOcr) } <= maxDist) {
                        bestTokenScore = maxOf(bestTokenScore, 66)
                    }
                }
            }
            if (bestTokenScore == 0) return 0
            totalQuality += bestTokenScore
        }
        return (totalQuality / queryTokens.size).coerceIn(60, 95)
    }

    fun search(query: String) {
        searchQuery = query
        searchJob?.cancel()
        if (query.isBlank()) {
            clearSearch()
            return
        }
        searchJob = viewModelScope.launch {
            delay(180)
            searching = true
            val cleanQ = query.trim()
            val queryNorm = normalizeForSearch(cleanQ)
            val queryTokens = tokenizeForSearch(queryNorm)
            val hits = ArrayList<SearchHit>()
            if (isPdf) {
                val index = textIndex()
                val map = pages.withIndex().filter { it.value.pdfPage >= 0 }.associate { it.value.pdfPage to it.index }
                if (index != null) hits += withContext(Dispatchers.Default) { index.search(cleanQ) { map[it] } }
            }
            val lang = settingsFlow.value.toolDefaults.handwritingLanguage
            val snapshotPages = pages.toList()
            // 1) Typed text, sticky notes, web links, write boxes, and concept map nodes (fuzzy + multi-word + diacritic-insensitive)
            snapshotPages.forEachIndexed { i, p ->
                p.items.forEach { item ->
                    val (source, text) = when (item) {
                        is TextItem -> "Text" to item.text
                        is StickyItem -> "Sticky" to listOf(item.title, item.text).filter { it.isNotBlank() }.joinToString(" · ")
                        is WebLinkItem -> "Link" to listOf(item.title, item.url, item.note).filter { it.isNotBlank() }.joinToString(" · ")
                        is MindNodeItem -> "Mind Map" to listOf(item.label, item.tag, item.notes).filter { it.isNotBlank() }.joinToString(" · ")
                        is WriteBoxItem -> "Write Box" to item.convertedText
                        else -> "" to ""
                    }
                    if (text.isNotEmpty()) {
                        val score = scoreTextMatch(text, queryNorm, queryTokens)
                        if (score > 0) {
                            val snippet = text.replace('\n', ' ').replace(Regex("\\s+"), " ").trim().take(68)
                            hits += SearchHit(i, listOf(item.bounds), snippet, source = source, score = score)
                        }
                    }
                }
            }
            // 2) Deep handwriting search across all pages with word-level cluster highlighting & persistent fingerprint cache
            val hwHits = withContext(Dispatchers.Default) {
                val out = ArrayList<SearchHit>()
                snapshotPages.forEachIndexed { pageIdx, p ->
                    val inkItems = p.items.filterIsInstance<InkItem>().filter { !it.isHighlighter }
                    if (inkItems.isEmpty()) return@forEachIndexed
                    val fp = "${p.id}:${lang}:${inkItems.size}:${inkItems.fold(0L) { acc, it -> acc * 31L + it.id }}"
                    val lines = handwritingSearchCache[fp] ?: run {
                        val recognized = ArrayList<CachedHandwritingLine>()
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
                                    allowFallback = true,
                                )
                            }.getOrNull()
                            val primary = res?.text?.trim().orEmpty()
                            val cands = res?.candidates.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(5)
                            if (primary.isEmpty() && cands.isEmpty()) continue

                            // Build approximate word-level highlight boxes by aligning recognized tokens with horizontal ink clusters
                            val clusters = handwritingWordClusters(lineStrokes)
                            val clusterRects = clusters.map { cluster ->
                                RectF(cluster.first().bounds).apply { cluster.drop(1).forEach { union(it.bounds) } }
                            }
                            val words = primary.split(Regex("\\s+")).filter { it.isNotEmpty() }
                            val wordBoxes = ArrayList<Pair<RectF, String>>()
                            if (words.isNotEmpty() && clusterRects.isNotEmpty()) {
                                if (words.size == clusterRects.size) {
                                    for (k in words.indices) wordBoxes += clusterRects[k] to words[k]
                                } else {
                                    // Proportional character-length span projection across line width
                                    val totalChars = words.sumOf { it.length }.coerceAtLeast(1).toFloat()
                                    var cursor = union.left
                                    val lineW = union.width().coerceAtLeast(1f)
                                    for (w in words) {
                                        val segW = lineW * (w.length / totalChars)
                                        val box = RectF(cursor, union.top, (cursor + segW).coerceAtMost(union.right), union.bottom)
                                        // Snap to nearest ink cluster if overlapping
                                        val snapped = clusterRects.firstOrNull { RectF.intersects(it, box) } ?: box
                                        wordBoxes += snapped to w
                                        cursor += segW
                                    }
                                }
                            }
                            recognized += CachedHandwritingLine(
                                bounds = union,
                                primaryText = primary.ifEmpty { cands.first() },
                                allCandidates = (listOf(primary) + cands).filter { it.isNotEmpty() }.distinct(),
                                wordBoxes = wordBoxes,
                            )
                        }
                        if (handwritingSearchCache.size > 96) handwritingSearchCache.clear()
                        handwritingSearchCache[fp] = recognized
                        recognized
                    }
                    for (line in lines) {
                        var bestScore = 0
                        var bestText = line.primaryText
                        for (cand in line.allCandidates) {
                            val sc = scoreTextMatch(cand, queryNorm, queryTokens)
                            if (sc > bestScore) {
                                bestScore = sc
                                bestText = cand
                            }
                        }
                        if (bestScore > 0) {
                            // Highlight matched individual handwritten words if found, else the full handwritten line
                            val matchedBoxes = if (line.wordBoxes.isNotEmpty()) {
                                line.wordBoxes.filter { (_, w) ->
                                    scoreTextMatch(w, queryNorm, queryTokens) > 0 ||
                                        queryTokens.any { qt -> scoreTextMatch(w, qt, listOf(qt)) > 0 }
                                }.map { it.first }
                            } else emptyList()
                            val highlightRects = if (matchedBoxes.isNotEmpty()) matchedBoxes else listOf(line.bounds)
                            val display = bestText.replace('\n', ' ').replace(Regex("\\s+"), " ").trim().take(64)
                            out += SearchHit(
                                page = pageIdx,
                                rects = highlightRects,
                                snippet = display,
                                source = "Handwriting",
                                score = bestScore,
                            )
                        }
                    }
                }
                out
            }
            hits += hwHits

            coroutineContext.ensureActive()
            searchHits = hits.sortedWith(compareBy<SearchHit> { it.page }.thenByDescending { it.score })
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

    // ---------------------------------------------------------------- PDF text & lasso region selection
    data class TextSel(val page: Int, val pdfPage: Int, val anchor: IntRange, val range: IntRange, val rects: List<RectF>, val text: String)
    var textSel by mutableStateOf<TextSel?>(null)
        internal set
    var textMenu by mutableStateOf<RectF?>(null)

    data class PdfRegionSelection(
        val page: Int,
        val rect: RectF,
        val screenBounds: RectF,
        val outline: List<Pair<Float, Float>>? = null,
        val extractedText: String = "",
    )
    var pdfRegionSel by mutableStateOf<PdfRegionSelection?>(null)
        internal set

    fun clearPdfRegionSel() {
        pdfRegionSel = null
    }

    fun onPdfLassoRegion(page: Int, rect: RectF, screenBounds: RectF, outline: List<Pair<Float, Float>>?) {
        val p = pages.getOrNull(page) ?: return
        pdfRegionSel = PdfRegionSelection(page, RectF(rect), RectF(screenBounds), outline, "")
        if (p.pdfPage >= 0 && isPdf) {
            viewModelScope.launch {
                val idx = textIndex() ?: return@launch
                val extracted = withContext(Dispatchers.Default) {
                    extractPdfTextInRect(p.pdfPage, rect)
                }
                if (pdfRegionSel?.page == page && pdfRegionSel?.rect == rect) {
                    pdfRegionSel = pdfRegionSel?.copy(extractedText = extracted)
                }
            }
        }
    }

    private suspend fun extractPdfTextInRect(pdfPage: Int, rect: RectF): String {
        val idx = pageTextIdx[pdfPage] ?: textIndex() ?: return ""
        val full = idx.pageText(pdfPage)
        if (full.isBlank()) return ""
        val out = StringBuilder()
        var lastWasSpace = false
        for (i in full.indices) {
            val box = idx.rects(pdfPage, i..i).firstOrNull()
            val ch = full[i]
            if (box != null) {
                if (rect.contains(box.centerX(), box.centerY())) {
                    out.append(ch)
                    lastWasSpace = false
                }
            } else if ((ch == ' ' || ch == '\n') && out.isNotEmpty() && !lastWasSpace) {
                out.append(ch)
                lastWasSpace = true
            }
        }
        return out.toString().replace(Regex("[ \\t]+"), " ").replace(Regex("\\n{3,}"), "\n\n").trim()
    }

    /** Renders a clipped PDF/page cutout (masked to freehand [outline] if provided). */
    private suspend fun clippedRegionBitmap(
        page: Page,
        rect: RectF,
        outline: List<Pair<Float, Float>>?,
    ): Bitmap? {
        val raw = regionImage(page, rect) ?: return null
        if (outline == null || outline.size < 3) return raw
        return withContext(Dispatchers.Default) {
            val masked = Bitmap.createBitmap(raw.width, raw.height, Bitmap.Config.ARGB_8888)
            val c = android.graphics.Canvas(masked)
            val sx = raw.width / rect.width().coerceAtLeast(1f)
            val sy = raw.height / rect.height().coerceAtLeast(1f)
            val path = android.graphics.Path().apply {
                outline.forEachIndexed { idx, (px, py) ->
                    val lx = (px - rect.left) * sx
                    val ly = (py - rect.top) * sy
                    if (idx == 0) moveTo(lx, ly) else lineTo(lx, ly)
                }
                close()
            }
            c.save()
            c.clipPath(path)
            c.drawBitmap(raw, 0f, 0f, null)
            c.restore()
            raw.recycle()
            masked
        }
    }

    /** Cuts/lifts a cropped region directly onto the page as a movable ImageItem (and copies to clipboard). */
    fun cutAreaFromPage(
        page: Int,
        rect: RectF,
        outline: List<Pair<Float, Float>>? = null,
        onPlaced: (Int, Long) -> Unit = { _, _ -> },
    ) {
        val p = pages.getOrNull(page) ?: return
        pdfRegionSel = null
        capturingCard = false
        viewModelScope.launch {
            val bmp = clippedRegionBitmap(p, rect, outline) ?: return@launch
            val name = withContext(Dispatchers.IO) {
                val n = "cut_${UUID.randomUUID().toString().take(8)}.png"
                AtomicFiles.writeBytesAtomic(File(repo.noteDir(noteId), n)) { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 96, out)
                }
                n
            }
            // Mirror cutout PNG to Android system clipboard too so it can be pasted anywhere on the phone.
            mirrorBitmapToSystemClipboard(bmp)
            bmp.recycle()
            val cur = pages.getOrNull(page) ?: return@launch
            val nx = (rect.left + 16f).coerceIn(12f, (cur.width - rect.width() - 12f).coerceAtLeast(12f))
            val ny = (rect.top + 16f).coerceIn(12f, (cur.height - rect.height() - 12f).coerceAtLeast(12f))
            val item = ImageItem(ItemIds.next(), name, nx, ny, rect.width(), rect.height())
            beginEdit(page)
            applyItems(page, cur.items + item)
            clipboard = listOf(item)
            hasClipboard = true
            lastInternalCopyMs = System.currentTimeMillis()
            onPlaced(page, item.id)
            say("Cut ready — drag to move or paste anywhere")
        }
    }

    fun cutPdfRegion(onPlaced: (Int, Long) -> Unit = { _, _ -> }) {
        val sel = pdfRegionSel ?: return
        cutAreaFromPage(sel.page, sel.rect, sel.outline, onPlaced)
    }

    fun copyPdfRegionImage() {
        val sel = pdfRegionSel ?: return
        val p = pages.getOrNull(sel.page) ?: return
        pdfRegionSel = null
        viewModelScope.launch {
            val bmp = clippedRegionBitmap(p, sel.rect, sel.outline) ?: return@launch
            val name = withContext(Dispatchers.IO) {
                val n = "clip_${UUID.randomUUID().toString().take(8)}.png"
                AtomicFiles.writeBytesAtomic(File(repo.noteDir(noteId), n)) { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 96, out)
                }
                n
            }
            mirrorBitmapToSystemClipboard(bmp)
            bmp.recycle()
            clipboard = listOf(ImageItem(ItemIds.next(), name, sel.rect.left, sel.rect.top, sel.rect.width(), sel.rect.height()))
            hasClipboard = true
            lastInternalCopyMs = System.currentTimeMillis()
            say("PDF cutout copied to clipboard")
        }
    }

    fun copyPdfRegionText() {
        val sel = pdfRegionSel ?: return
        val txt = sel.extractedText.trim()
        pdfRegionSel = null
        if (txt.isEmpty()) {
            say("No selectable PDF text in this area")
            return
        }
        runCatching {
            val cm = sysClipboard
            if (cm != null) {
                suppressNextSystemClipEvent = true
                cm.setPrimaryClip(android.content.ClipData.newPlainText("PDF Text", txt))
                lastMirroredSystemClipTimestamp = cm.primaryClipDescription?.timestamp ?: System.currentTimeMillis()
                systemHasClipboard = true
            }
        }
        say("Copied PDF text")
    }

    fun highlightPdfRegion(color: Int) {
        val sel = pdfRegionSel ?: return
        val p = pages.getOrNull(sel.page) ?: return
        pdfRegionSel = null
        val y = sel.rect.centerY()
        val h = sel.rect.height().coerceIn(24f, 180f)
        Brushes.highlightAlpha = (settingsFlow.value.highlighterOpacity.coerceIn(0.3f, 0.9f) * 255).toInt()
        val brush = Brushes.create(Brushes.HIGHLIGHTER, color, h)
        val stroke = InkCodec.strokeFromPoints(
            listOf(sel.rect.left to y, sel.rect.right to y),
            brush,
        )
        beginEdit(sel.page)
        applyItems(sel.page, p.items + InkItem(ItemIds.next(), stroke, familyHint = Brushes.HIGHLIGHTER))
    }

    fun savePdfRegionAsSticker() {
        val sel = pdfRegionSel ?: return
        val p = pages.getOrNull(sel.page) ?: return
        pdfRegionSel = null
        viewModelScope.launch {
            val bmp = clippedRegionBitmap(p, sel.rect, sel.outline) ?: return@launch
            withContext(Dispatchers.IO) {
                val app = getApplication<Application>()
                val tmp = File(app.cacheDir, "pdf_sticker_${System.currentTimeMillis()}.png")
                tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
                runCatching { StickerLibrary.import(app, Uri.fromFile(tmp), autoRemoveBg = false) }
                tmp.delete()
            }
            bmp.recycle()
            say("Saved PDF cutout to Stickers")
        }
    }

    private fun mirrorBitmapToSystemClipboard(bmp: Bitmap) {
        val cm = sysClipboard ?: return
        val app = getApplication<Application>()
        runCatching {
            val dir = File(app.cacheDir, "clipboard").apply { mkdirs() }
            val out = File(dir, "pronotes_clip.png")
            out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
            val uri = androidx.core.content.FileProvider.getUriForFile(app, app.packageName + ".files", out)
            suppressNextSystemClipEvent = true
            cm.setPrimaryClip(android.content.ClipData.newUri(app.contentResolver, "Pro Notes Cutout", uri))
            lastMirroredSystemClipTimestamp = cm.primaryClipDescription?.timestamp ?: System.currentTimeMillis()
            systemHasClipboard = true
        }
    }

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
            if (snapshot.isEmpty()) return@launch
            val dirtySnap = synchronized(dirtyPageIds) { HashSet(dirtyPageIds) }
            val structSnap = structureDirty
            try {
                repo.savePages(
                    id = noteId,
                    pages = snapshot,
                    dirtyPageIds = dirtySnap,
                    structureChanged = structSnap,
                    writeFullSnapshot = false,
                )
                synchronized(dirtyPageIds) { dirtyPageIds.removeAll(dirtySnap) }
                if (structSnap) structureDirty = false
            } catch (failure: Exception) {
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                say("Could not save changes. Your edits are still open. Please free some storage and try again.")
            }
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
        if (snapshot.isEmpty()) return
        val dirtySnap = synchronized(dirtyPageIds) {
            HashSet(dirtyPageIds).also { dirtyPageIds.clear() }
        }
        structureDirty = false
        repo.appScope.launch {
            try {
                repo.savePages(
                    id = noteId,
                    pages = snapshot,
                    dirtyPageIds = dirtySnap,
                    structureChanged = true,
                    writeFullSnapshot = true,
                )
            } catch (failure: Exception) {
                synchronized(dirtyPageIds) { dirtyPageIds.addAll(dirtySnap) }
                structureDirty = true
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                withContext(Dispatchers.Main) {
                    say("Could not save changes. Please free some storage and try again.")
                }
            }
        }
    }

    suspend fun exportPdf(out: OutputStream) = repo.exportPdf(noteId, pages.toList(), out)

    var activeWriteBoxId by mutableStateOf<Long?>(null)
    var activeWriteBoxPage by mutableStateOf(0)
    var keyboardMode by mutableStateOf(false)
    var onRequestCommitText: (() -> Unit)? = null

    override fun onCleared() {
        runCatching { sysClipboard?.removePrimaryClipChangedListener(systemClipListener) }
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

