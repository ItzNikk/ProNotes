package com.pronotes.app.editor

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.FormatAlignRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.FlipToFront
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.VerticalSplit
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pronotes.app.data.AppSettings
import com.pronotes.app.data.EraserMode
import com.pronotes.app.data.HighlighterMode
import com.pronotes.app.data.LassoMode
import com.pronotes.app.data.MotionStyle
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.PenPreset
import com.pronotes.app.data.Repository
import com.pronotes.app.data.ScribbleSensitivity
import com.pronotes.app.data.ScrollDirection
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.ToolbarDock
import com.pronotes.app.ui.ColorEditorDialog
import com.pronotes.app.ui.LayerBackdrop
import com.pronotes.app.ui.LocalMotionStyle
import com.pronotes.app.ui.OptionalLiquidGlassBox
import com.pronotes.app.ui.layerBackdrop
import com.pronotes.app.ui.rememberLayerBackdrop
import com.pronotes.app.ui.PenGlyph
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import com.pronotes.app.ui.ShapeGlyph
import com.pronotes.app.ui.StickerSet
import com.pronotes.app.ui.Swatch
import com.pronotes.app.ui.ToolGlyph
import com.pronotes.app.ui.elasticStretch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

private val Pill = CircleShape

/**
 * "Liquid glass": a see-through surface tint with a bright hairline edge and a soft
 * top highlight. Keeps the app's own colours; works on every Android version.
 */
@Composable
private fun glassOn(): Boolean = SettingsStore.get(LocalContext.current).settings.collectAsState().value.liquidGlass

@Composable
private fun glassColor(): Color = MaterialTheme.colorScheme.surface.copy(alpha = if (glassOn()) 0.78f else 1f)

@Composable
private fun Modifier.glassEdge(shape: androidx.compose.ui.graphics.Shape): Modifier {
    if (!glassOn()) return this.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.4f
    return this
        .border(
            1.dp,
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = if (dark) 0.22f else 0.85f), Color.White.copy(alpha = if (dark) 0.05f else 0.25f)),
            ),
            shape,
        )
        .background(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) 0.06f else 0.28f), Color.Transparent)),
            shape,
        )
}

/** Shares a bitmap as a PNG through the system share sheet. */
private suspend fun shareBitmap(context: Context, bmp: Bitmap, name: String) {
    val file = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        File(dir, name.replace(Regex("[^A-Za-z0-9 _-]"), "_") + ".png").also { f -> f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, "Share image"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(noteId: String, onBack: () -> Unit, onOpenSettings: () -> Unit, onSwitchNote: (String) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: EditorViewModel = viewModel(key = noteId, factory = viewModelFactory { initializer { EditorViewModel(app, noteId) } })
    val settings by vm.settingsFlow.collectAsState()
    val store = remember { SettingsStore.get(context) }
    val repo = remember { Repository.get(context) }
    val library by repo.library.collectAsState()
    val tabs by store.tabs.collectAsState()
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    var canvas by remember { mutableStateOf<NoteCanvasView?>(null) }
    var showPages by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showStickers by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var goTo by remember { mutableStateOf(false) }
    var focus by remember { mutableStateOf(false) }
    var pageColor by remember { mutableStateOf(false) }
    var wheelAt by remember { mutableStateOf<Offset?>(null) }
    val pickBackground = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setPageBackground(uri)
    }
    val importFilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.importFilesIntoCurrent(uris)
    }

    LaunchedEffect(noteId) { store.openTab(noteId) }
    LaunchedEffect(library) { store.retainTabs(library.notebooks.filter { it.deletedAt == null }.map { it.id }.toSet()) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.insertImage(uri) { page, id -> canvas?.selectItems(page, setOf(id)) }
        vm.tool = Tool.LASSO
    }
    val exportPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch { context.contentResolver.openOutputStream(uri)?.use { vm.exportPdf(it) } }
    }

    fun sharePdf() = scope.launch {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, vm.title.replace(Regex("[^A-Za-z0-9 _-]"), "_") + ".pdf")
        file.outputStream().use { vm.exportPdf(it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND).setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Share PDF"))
    }

    BackHandler {
        when {
            focus -> focus = false
            showPages -> showPages = false
            else -> { vm.saveNow(); onBack() }
        }
    }
    DisposableEffect(Unit) { onDispose { vm.saveNow(); vm.onPagesChanged = null; vm.onScrollTo = null; vm.onRedraw = null } }

    LaunchedEffect(canvas) {
        val c = canvas ?: return@LaunchedEffect
        vm.onPagesChanged = { c.setPages(vm.pages.toList()) }
        vm.onScrollTo = { i -> c.scrollToPage(i, animate = true) }
        vm.onRedraw = { c.redraw() }
        vm.toScreen = { pg, r -> c.pageToScreen(pg, r) }
        vm.fitMindMapOnCanvas = { pg, r -> c.fitRect(pg, r) }
        c.startPage = vm.startPage
        c.setPages(vm.pages.toList())
    }
    LaunchedEffect(vm.reopenTool) {
        if (vm.reopenTool == 0) return@LaunchedEffect
        when (vm.tool) {
            Tool.IMAGE -> pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            Tool.STICKER -> showStickers = true
            else -> Unit
        }
    }
    LaunchedEffect(vm.tool, canvas) {
        canvas?.tool = vm.tool
        when (vm.tool) {
            Tool.IMAGE -> pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            Tool.STICKER -> showStickers = true
            else -> Unit
        }
    }
    LaunchedEffect(settings, canvas) { canvas?.settings = settings }
    // The desk around pages follows light / dark mode.
    val darkUi = MaterialTheme.colorScheme.background.luminance() < 0.4f
    LaunchedEffect(darkUi, canvas) { canvas?.darkUi = darkUi }
    DisposableEffect(settings.keepScreenOn) {
        val window = (context as? android.app.Activity)?.window
        if (settings.keepScreenOn) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    fun switchTo(id: String) { if (id != noteId) { vm.saveNow(); onSwitchNote(id) } }

    // Android 15+: know when the screen is being recorded, to hide the toolbar if the user wants.
    var recording by remember { mutableStateOf(false) }
    DisposableEffect(settings.hideToolsWhenRecording) {
        val activity = context as? android.app.Activity
        var unregister: (() -> Unit)? = null
        if (settings.hideToolsWhenRecording && activity != null && android.os.Build.VERSION.SDK_INT >= 35) {
            runCatching {
                val wm = activity.windowManager
                val cb = java.util.function.Consumer<Int> { state -> recording = state == android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE }
                val now = wm.addScreenRecordingCallback(activity.mainExecutor, cb)
                recording = now == android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE
                unregister = { runCatching { wm.removeScreenRecordingCallback(cb) } }
            }
        } else recording = false
        onDispose { unregister?.invoke() }
    }
    val hideTools = focus || vm.readOnly || (settings.hideToolsWhenRecording && recording)
    LaunchedEffect(vm.readOnly, canvas) { canvas?.readOnly = vm.readOnly }
    LaunchedEffect(vm.capturingCard, vm.linkFrom, canvas) {
        canvas?.capturing = vm.capturingCard
        canvas?.linkPicking = vm.linkFrom != null
    }
    LaunchedEffect(vm.links, canvas) { canvas?.redraw() }
    LaunchedEffect(vm.textSel, canvas) { canvas?.redraw() }
    LaunchedEffect(vm.activeMindNodeId, canvas) {
        canvas?.activeMindNodeId = vm.activeMindNodeId
        canvas?.redraw()
    }
    LaunchedEffect(vm.moveMindSubtree, canvas) {
        canvas?.moveMindSubtree = vm.moveMindSubtree
    }
    var showQuiz by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    /** Notebook shown beside this one (split view), or null. */
    var splitId by remember { mutableStateOf<String?>(null) }
    var splitWeight by remember { mutableFloatStateOf(0.8f) }
    LaunchedEffect(vm.showQuizRequest) { if (vm.showQuizRequest > 0) showQuiz = true }
    var showCards by remember { mutableStateOf(false) }
    var showAiChat by remember { mutableStateOf(false) }
    var showAssetStudio by remember { mutableStateOf<AiAssetKind?>(null) }
    var showLectureRecorder by remember { mutableStateOf(false) }
    var showCollabRoom by remember { mutableStateOf(false) }

    // Real background blur (Android 12+) while a card, dialog or sheet is open.
    val overlayOpen = vm.textEdit != null || vm.stickyEdit != null || vm.webLinkEdit != null || vm.mindNodeEdit != null ||
        vm.showSlideShow || showAiChat || showAssetStudio != null || showLectureRecorder || showCollabRoom ||
        renaming || goTo || pageColor || showStickers || vm.cardOpen || wheelAt != null
    val blurRadius by androidx.compose.animation.core.animateDpAsState(if (overlayOpen) 14.dp else 0.dp, label = "blur")
    val blurMod = if (android.os.Build.VERSION.SDK_INT >= 31 && blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier

    val backdrop = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().then(blurMod).background(MaterialTheme.colorScheme.background)) {
        AnimatedVisibility(!focus, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            // Fixed Top Bar wrapped in Liquid Glass
            OptionalLiquidGlassBox(
                backdrop = backdrop,
                enabled = settings.liquidGlass,
                shape = RoundedCornerShape(0.dp),
                fallbackColor = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primary, 0.14f),
                modifier = Modifier.shadow(2.dp),
            ) {
                Column(Modifier.statusBarsPadding()) {
                    if (settings.showTabs) {
                        TabStrip(
                            tabs = tabs.mapNotNull { id -> library.notebooks.firstOrNull { it.id == id && it.deletedAt == null }?.let { id to it.title } },
                            current = noteId,
                            onSelect = ::switchTo,
                            onClose = { id ->
                                val rest = tabs - id
                                store.closeTab(id)
                                if (id == noteId) { vm.saveNow(); if (rest.isNotEmpty()) onSwitchNote(rest.last()) else onBack() }
                            },
                        )
                    }
                    TopBar(
                        vm = vm, settings = settings, showSearch = showSearch, showPages = showPages, hideTools = hideTools,
                        onHome = { vm.saveNow(); onBack() },
                        onPages = { showPages = !showPages },
                        onSearch = { showSearch = !showSearch; if (!showSearch) vm.clearSearch() },
                        onRename = { renaming = true },
                        onFocus = { focus = true },
                        onExport = { exportPdf.launch(vm.title + ".pdf") },
                        onShare = { sharePdf() },
                        onSharePage = { scope.launch { vm.pageImage(vm.pageIndex)?.let { shareBitmap(context, it, "${vm.title} p${vm.pageIndex + 1}") } } },
                        onSettings = { vm.saveNow(); onOpenSettings() },
                        onGoTo = { goTo = true },
                        onScrollToggle = {
                            store.update { it.copy(scroll = if (it.scroll == ScrollDirection.VERTICAL) ScrollDirection.HORIZONTAL else ScrollDirection.VERTICAL) }
                        },
                        onPaste = { vm.paste()?.let { (pg, ids) -> canvas?.selectItems(pg, ids) } },
                        onPageColor = { pageColor = true },
                        onCards = { showCards = true },
                        onQuiz = { showQuiz = true },
                        onSlideShow = { vm.showSlideShow = true },
                        onAiChat = { showAiChat = true },
                        onAssetStudio = { kind -> showAssetStudio = kind },
                        onLectureRecorder = { showLectureRecorder = true },
                        onCollabRoom = { showCollabRoom = true },
                        onAddWebLink = {
                            val p = vm.currentPage
                            vm.openWebLink(vm.pageIndex, (p?.width ?: 1240f) / 2f, 340f, null)
                        },
                        onTemplates = { showTemplates = true },
                        onImportFiles = {
                            importFilesLauncher.launch(arrayOf("application/pdf", "text/*", "image/*", "application/json", "application/octet-stream"))
                        },
                        onSplit = { splitId = if (splitId != null) null else library.notebooks.firstOrNull { it.deletedAt == null && it.id != noteId }?.id ?: noteId },
                        onPageImage = { pickBackground.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    )
                    AnimatedVisibility(showSearch, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        SearchBar(vm)
                    }
                }
            }
        }

        Row(Modifier.weight(1f).fillMaxWidth()) {
            // ------------------------------------------------------ pages panel (left)
            AnimatedVisibility(
                showPages && !focus,
                enter = slideInHorizontally { -it } + fadeIn(),
                exit = slideOutHorizontally { -it } + fadeOut(),
            ) {
                PagesPanel(vm, onJump = { vm.goToPage(it) }, onClose = { showPages = false })
            }

            // -------------------------------------------------------------- canvas
            var areaSize by remember { mutableStateOf(IntSize.Zero) }
            Box(Modifier.weight(1f).fillMaxHeight().onSizeChanged { areaSize = it }) {
                if (vm.loaded) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize().layerBackdrop(backdrop),
                        factory = { ctx ->
                            NoteCanvasView(ctx, object : CanvasHost {
                                override fun brush(tool: Tool) = vm.brushFor(tool)
                                override fun shapeKind() = vm.shapeKind
                                override fun eraserRadius() = vm.eraserRadiusPx(density)
                                override fun tapeColor() = vm.tapeColor
                                override fun tapeFreehand() = vm.tapeFreehand
                                override fun beginEdit(page: Int) = vm.beginEdit(page)
                                override fun setItems(page: Int, items: List<PageItem>) = vm.setItems(page, items)
                                override fun onSelectionChanged(page: Int, ids: Set<Long>, screenBounds: android.graphics.RectF?) =
                                    vm.onSelection(page, ids, screenBounds)
                                override fun onTextTap(page: Int, x: Float, y: Float, existing: TextItem?) {
                                    vm.textEdit = EditorViewModel.TextEdit(page, x, y, existing)
                                }
                                override fun onStickyTap(page: Int, x: Float, y: Float, existing: StickyItem?) = vm.openSticky(page, x, y, existing)
                                override fun onStickyChecklistToggle(page: Int, sticky: StickyItem, lineIndex: Int) = vm.toggleStickyCheckLine(page, sticky, lineIndex)
                                override fun onWebLinkTap(page: Int, x: Float, y: Float, existing: WebLinkItem?) = vm.openWebLink(page, x, y, existing)
                                override fun onMindMapTap(page: Int, x: Float, y: Float, existing: MindNodeItem?) = vm.onMindMapTap(page, x, y, existing)
                                override fun onCurrentPage(index: Int) = vm.onCurrentPage(index)
                                override fun onPullAddPage() = vm.addPage(atEnd = true)
                                override fun undo() = vm.undo()
                                override fun redo() = vm.redo()
                                override fun pdfBitmap(page: Page, widthPx: Int, onReady: () -> Unit) = vm.pdfBitmap(page, widthPx, onReady)
                                override fun pdfDetail(page: Int) = vm.pdfDetail(page)
                                override fun tapePattern() = vm.tapePattern
                                override fun onPenActive(active: Boolean) { vm.penActive = active }
                                override fun requestDetail(wants: List<Triple<Int, android.graphics.RectF, Float>>, onReady: () -> Unit) = vm.requestDetail(wants, onReady)
                                override fun highlights(page: Int) = vm.highlights(page)
                                override fun activeHighlight() = vm.activeHighlight()
                                override fun shapeFill() = vm.shapeFill()
                                override fun onCircleCandidate(screenBounds: android.graphics.RectF?) = Unit
                                override fun onCircleSelect(active: Boolean) = vm.circleSelect(active)
                                override fun linkEnds() = vm.linkEnds()
                                override fun onAreaPicked(page: Int, rect: android.graphics.RectF) = vm.onAreaPicked(page, rect)
                                override fun onLinkTarget(page: Int, x: Float, y: Float) = vm.finishLink(page, x, y)
                                override fun onLinkLongPress(id: String) { vm.linkToDelete = id }
                                override fun onTextPress(page: Int, x: Float, y: Float) = vm.textPress(page, x, y)
                                override fun onTextDrag(x: Float, y: Float) = vm.textDrag(x, y)
                                override fun onTextEnd(screen: android.graphics.RectF?) = vm.textEnd(screen)
                                override fun onTextClear() { if (vm.textSel != null) vm.clearTextSel() }
                                override fun textSelection(page: Int) = vm.textSelection(page)
                                override fun onOpenUrl(url: String) = vm.openExternalUrl(url)
                                override fun onEraserLifted() = vm.onEraserLifted()
                                override fun onZoom(percent: Int?) = vm.onZoom(percent)
                                override fun penProfile(deviceName: String) = store.registerPen(deviceName)
                                override fun onPenButton(doubleClick: Boolean, x: Float, y: Float, deviceName: String) {
                                    if (vm.readOnly) return
                                    val prof = store.registerPen(deviceName)
                                    when (if (doubleClick) prof.doubleClick else prof.click) {
                                        com.pronotes.app.data.PenAction.QUICK_WHEEL -> wheelAt = Offset(x, y)
                                        com.pronotes.app.data.PenAction.UNDO -> vm.undo()
                                        com.pronotes.app.data.PenAction.TOGGLE_ERASER -> vm.tool = if (vm.tool == Tool.ERASER) Tool.PEN else Tool.ERASER
                                        com.pronotes.app.data.PenAction.LAST_TOOL -> vm.swapToPrevious()
                                        com.pronotes.app.data.PenAction.NONE -> Unit
                                    }
                                }
                                override fun hoverInk(): Pair<Int, Float>? = vm.hoverInk()
                            }).also {
                                it.renderer = vm.renderer()
                                it.settings = settings
                                it.tool = vm.tool
                                canvas = it
                            }
                        },
                    )
                }

                // Floating selection menu: below the selection, or above when there is no room.
                val sb = vm.selectionBounds
                androidx.compose.animation.AnimatedVisibility(
                    visible = vm.selection.isNotEmpty() && sb != null,
                    enter = fadeIn() + scaleIn(initialScale = 0.85f),
                    exit = fadeOut() + scaleOut(targetScale = 0.85f),
                    modifier = Modifier.offset {
                        val b = sb ?: android.graphics.RectF()
                        val below = b.bottom + 18 * density
                        val y = if (below + 60 * density < areaSize.height) below else (b.top - 110 * density).coerceAtLeast(8f)
                        IntOffset(b.left.roundToInt().coerceIn(8, (areaSize.width - 480 * density).roundToInt().coerceAtLeast(8)), y.roundToInt())
                    },
                ) {
                    SelectionMenu(
                        vm = vm,
                        backdrop = backdrop,
                        liquidGlass = settings.liquidGlass,
                        onDuplicate = { val ids = vm.duplicateSelection(); canvas?.selectItems(vm.selectionPage, ids) },
                        onColor = { c -> val ids = vm.selection; val pg = vm.selectionPage; vm.recolorSelection(c); canvas?.selectItems(pg, ids) },
                        keep = { block -> val ids = vm.selection; val pg = vm.selectionPage; block(); canvas?.selectItems(pg, ids) },
                        onShareImage = { scope.launch { vm.selectionImage()?.let { shareBitmap(context, it, vm.title + " selection") } } },
                    )
                }

                // Single Fixed Top Bar only — all tools, options, and the ✨ AI button live inside TopBar.

                vm.zoomPercent?.let { z ->
                    Surface(shape = Pill, color = Color(0xCC1B1F2A), contentColor = Color.White, modifier = Modifier.align(Alignment.Center)) {
                        Text("$z%", Modifier.padding(horizontal = 16.dp, vertical = 6.dp), style = MaterialTheme.typography.titleMedium)
                    }
                }

                if (focus) {
                    Surface(
                        shape = Pill, color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp,
                        modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp).clickable { focus = false },
                    ) { ProIconView(ProIcon.CLOSE, MaterialTheme.colorScheme.onSurface, Modifier.padding(10.dp).size(20.dp)) }
                }

                // Undo and redo sit at the top left, wherever the toolbar is (inside the bar on phones).
                val narrowScreen = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 600
                // Floating bits fade while the pen writes, so nothing hides the page.
                val overlayFade by animateFloatAsState(if (vm.penActive) 0.12f else 1f, tween(300), label = "ov")
                @Suppress("UNUSED_VARIABLE") val unusedNarrow = narrowScreen

                // Long-press on PDF text: copy, highlight, flashcard, search.
                vm.textMenu?.let { r ->
                    val clip = androidx.compose.ui.platform.LocalClipboardManager.current
                    Surface(
                        shape = Pill, color = Color(0xE01B1F2A), contentColor = Color.White, shadowElevation = 8.dp,
                        modifier = Modifier.offset {
                            val y = (r.top - 64 * density).let { if (it < 8) r.bottom + 12 * density else it }
                            IntOffset(r.left.roundToInt().coerceIn(8, (areaSize.width - 360 * density).roundToInt().coerceAtLeast(8)), y.roundToInt())
                        },
                    ) {
                        Row(Modifier.padding(horizontal = 6.dp).widthIn(max = 640.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { vm.textSel?.text?.let { clip.setText(androidx.compose.ui.text.AnnotatedString(it)) }; vm.clearTextSel() }) { Text("Copy", color = Color.White) }
                            // Highlight in one of four colours.
                            listOf(0xFFFFE066, 0xFF8CE99A, 0xFF74C0FC, 0xFFFFA8A8).forEach { c ->
                                Box(Modifier.padding(3.dp).size(22.dp).clip(CircleShape).background(Color(c)).clickable { vm.highlightTextSel(c.toInt()) })
                            }
                            TextButton(onClick = { vm.lineTextSel(strike = false) }) { Text("Underline", color = Color.White) }
                            TextButton(onClick = { vm.lineTextSel(strike = true) }) { Text("Strike", color = Color.White) }
                            TextButton(onClick = { vm.explainTextSel() }) { Text("Explain", color = Color.White) }
                            TextButton(onClick = { vm.cardFromTextSel() }) { Text("Flashcard", color = Color.White) }
                            TextButton(onClick = { vm.quizFromTextSel() }) { Text("Quiz", color = Color.White) }
                            TextButton(onClick = { val q = vm.textSel?.text ?: ""; vm.clearTextSel(); showSearch = true; vm.search(q) }) { Text("Search", color = Color.White) }
                            TextButton(onClick = {
                                val q = vm.textSel?.text ?: ""; vm.clearTextSel()
                                context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, q), "Share"))
                            }) { Text("Share", color = Color.White) }
                        }
                    }
                }

                vm.toast?.let { msg ->
                    Surface(
                        shape = Pill, color = Color(0xE01B1F2A), contentColor = Color.White, shadowElevation = 6.dp,
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 120.dp),
                    ) { Text(msg, Modifier.padding(horizontal = 18.dp, vertical = 10.dp), style = MaterialTheme.typography.labelLarge) }
                }
                androidx.compose.animation.AnimatedVisibility(
                    vm.aiBusy,
                    modifier = Modifier.fillMaxSize(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) { AiParticleBackdrop(Modifier.fillMaxSize(), active = true, variant = AiBackdropVariant.AI_WAVE) }
                androidx.compose.animation.AnimatedVisibility(
                    vm.aiBusy, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 180.dp),
                    enter = fadeIn() + scaleIn(initialScale = 0.8f), exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) { ThinkingPill(engine = vm.aiEngineLabel, stage = vm.aiStage) }

                if (vm.currentPage?.isWhiteboard == true && !focus) {
                    Surface(
                        shape = Pill, color = glassColor(), shadowElevation = 4.dp,
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp).glassEdge(Pill).graphicsLayer { alpha = overlayFade },
                    ) {
                        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("∞ Whiteboard", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            TextButton(onClick = { canvas?.resetWhiteboardView() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("Reset view", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                if (!focus && !vm.readOnly && (vm.tool == Tool.MINDMAP || vm.activeMindNodeId != null)) {
                    val selNode = vm.selectedMindNode()
                    val hasKids = selNode != null && vm.currentPage?.items?.any { it is MindNodeItem && it.parentId == selNode.id } == true
                    Surface(
                        shape = Pill, color = glassColor(), shadowElevation = 8.dp,
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                            .padding(bottom = if (settings.toolbarDock == ToolbarDock.BOTTOM) 84.dp else 18.dp)
                            .glassEdge(Pill).graphicsLayer { alpha = overlayFade },
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                            if (selNode != null) {
                                TextButton(onClick = { vm.openEditSelectedMindNode() }) { Text("Edit") }
                                TextButton(onClick = { vm.addChildMindNode() }) { Text("+ Child") }
                                if (selNode.parentId != null) {
                                    TextButton(onClick = { vm.addSiblingMindNode() }) { Text("+ Sibling") }
                                }
                                if (hasKids) {
                                    TextButton(onClick = { vm.toggleCollapseMindNode(selNode.id) }) {
                                        Icon(if (selNode.collapsed) Icons.Outlined.UnfoldMore else Icons.Outlined.UnfoldLess, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (selNode.collapsed) "Expand" else "Collapse")
                                    }
                                }
                                TextButton(onClick = { vm.deleteMindNode(selNode.id) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                                Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(18.dp).background(MaterialTheme.colorScheme.outlineVariant))
                            }
                            TextButton(onClick = { vm.moveMindSubtree = !vm.moveMindSubtree }) {
                                Text(
                                    if (vm.moveMindSubtree) "Subtree: On" else "Subtree",
                                    color = if (vm.moveMindSubtree) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            EditorViewModel.MindLayout.entries.forEach { layout ->
                                TextButton(onClick = { vm.autoLayoutMindMap(layout) }) { Text(layout.label) }
                            }
                            TextButton(onClick = { vm.fitMindMapToView() }) {
                                ProIconView(ProIcon.FIT_MAP, MaterialTheme.colorScheme.primary, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Fit Map")
                            }
                            TextButton(onClick = { vm.exportMindMapOutlineToPage() }) { Text("Outline") }
                            TextButton(onClick = { vm.flashcardsFromMindMap() }) { Text("Cards") }
                            TextButton(onClick = { vm.generateMindMapFromNotes() }) {
                                Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("AI Map")
                            }
                        }
                    }
                }

                if (vm.readOnly && !focus) {
                    Surface(
                        shape = Pill, color = glassColor(), shadowElevation = 4.dp,
                        modifier = Modifier.align(Alignment.TopCenter).padding(12.dp).glassEdge(Pill).clickable { vm.toggleReadOnly() },
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lock, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Read only · tap to edit", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (settings.studyTimer && !focus) StudyTimer(vm, settings, areaSize, backdrop = backdrop)

                val banner = when {
                    vm.capturingCard && vm.backFor != null -> "Drag a box over the answer"
                    vm.capturingCard && vm.captureForQuiz -> "Drag a box over the picture or text to quiz on"
                    vm.capturingCard -> "Drag a box over what goes on the card"
                    vm.linkFrom != null -> "Tap where this should link to"
                    else -> null
                }
                if (banner != null) {
                    Surface(
                        shape = Pill, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shadowElevation = 6.dp,
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 72.dp),
                    ) {
                        Row(Modifier.padding(start = 18.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(banner, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { vm.capturingCard = false; vm.linkFrom = null }) { Text("Cancel", color = MaterialTheme.colorScheme.onPrimary) }
                        }
                    }
                }

            }

            // ------------------------------------------------------ split view (right)
            splitId?.let { other ->
                Box(
                    Modifier.width(12.dp).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                val delta = -dragAmount / 480f
                                splitWeight = (splitWeight + delta).coerceIn(0.40f, 2.35f)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.width(4.dp).height(42.dp).clip(Pill).background(MaterialTheme.colorScheme.outline))
                }
                SplitPanel(
                    noteId = other,
                    mainPageIndex = vm.pageIndex,
                    scrollDirection = settings.scroll,
                    others = library.notebooks.filter { it.deletedAt == null && it.id != noteId }.map { it.id to it.title },
                    onPick = { splitId = it },
                    onOpen = { switchTo(it) },
                    onSwap = {
                        val cur = noteId
                        splitId = cur
                        switchTo(other)
                    },
                    onRatioPreset = { ratio -> splitWeight = ratio },
                    onClipToSticky = { snippet, srcTitle -> vm.clipTextAsSticky(snippet, srcTitle) },
                    onInsertToPage = { snippet, srcTitle -> vm.clipTextAsPageItem(snippet, srcTitle) },
                    onAddWebLink = { url, lTitle, lNote -> vm.addQuickWebLink(url, lTitle, lNote) },
                    onClose = { splitId = null },
                    modifier = Modifier.weight(splitWeight).fillMaxHeight(),
                )
            }
        }
    }

    // Quick tool wheel (pen button click while hovering).
    wheelAt?.let { at -> ToolWheel(vm, at, onDismiss = { wheelAt = null }) }
    }

    // ------------------------------------------------------------------ dialogs
    vm.textEdit?.let { edit -> TextDialog(vm, edit) }
    vm.mindNodeEdit?.let { edit -> MindNodeDialog(vm, edit) }
    vm.cardDraft?.let { d -> CardEditorDialog(vm, d) }
    if (vm.linkSheet != null) LinkSheet(vm)
    if (showCards) FlashcardsDialog(vm, onClose = { showCards = false })
    if (showQuiz) QuizDialog(vm, onClose = { showQuiz = false })
    if (vm.showSlideShow) SlideShowDialog(vm, onClose = { vm.showSlideShow = false })
    vm.explanation?.let { (sel, ans) ->
        Dialog(onDismissRequest = { vm.explanation = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(20.dp).widthIn(max = 560.dp).fillMaxWidth(),
            ) {
                Box {
                    AiParticleBackdrop(Modifier.matchParentSize(), active = true, variant = AiBackdropVariant.AI_WAVE)
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Explained", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        }
                        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("\"" + sel.take(160) + "\"", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(ans, style = MaterialTheme.typography.bodyMedium)
                            Text("By ${vm.aiEngineLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                            TextButton(onClick = {
                                vm.insertAiTextToPage(ans)
                                vm.explanation = null
                            }) { Text("Insert to Page") }
                            TextButton(onClick = { vm.explanation = null }) { Text("Done", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
    if (showTemplates) TemplateSheet(vm, onAiTemplate = { showTemplates = false; showAssetStudio = AiAssetKind.TEMPLATE }, onClose = { showTemplates = false })
    vm.summary?.let { res ->
        val clip = androidx.compose.ui.platform.LocalClipboardManager.current
        SummarySheet(
            result = res,
            onClose = { vm.summary = null },
            onCopy = {
                val formatted = buildString {
                    appendLine(res.headline)
                    res.points.forEach { appendLine("• $it") }
                }
                clip.setText(androidx.compose.ui.text.AnnotatedString(formatted.trim()))
            },
            onAdd = { vm.summaryToPage() },
            onAddSticky = { vm.summaryToSticky() },
            onBuildMindMap = { vm.summary = null; vm.generateMindMapFromNotes() },
        )
    }
    vm.linkToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { vm.linkToDelete = null },
            title = { Text("Remove this link?") },
            text = { Text("Both ends of the link will go away. Your ink stays.") },
            confirmButton = { TextButton(onClick = { vm.deleteLink(id) }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { vm.linkToDelete = null }) { Text("Cancel") } },
        )
    }

    vm.stickyEdit?.let { edit ->
        var title by remember(edit) { mutableStateOf(edit.item.title) }
        var text by remember(edit) { mutableStateOf(edit.item.text) }
        var color by remember(edit) { mutableStateOf(edit.item.color) }
        var fontSize by remember(edit) { mutableFloatStateOf(edit.item.fontSize) }
        var font by remember(edit) { mutableStateOf(edit.item.font) }
        var style by remember(edit) { mutableStateOf(edit.item.style.ifBlank { "classic" }) }
        var collapsed by remember(edit) { mutableStateOf(edit.item.collapsed) }
        var pinned by remember(edit) { mutableStateOf(edit.item.pinned) }
        val fonts = listOf("SANS" to "Sans", "SERIF" to "Serif", "MONO" to "Mono", "HAND" to "Hand")
        val styles = listOf("classic" to "Classic", "lined" to "Lined", "glass" to "Glass", "outline" to "Outline")
        AlertDialog(
            onDismissRequest = { vm.stickyEdit = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProIconView(ProIcon.STICKY, MaterialTheme.colorScheme.primary, Modifier.size(22.dp), ink = Color(color))
                    Spacer(Modifier.width(8.dp))
                    Text(if (edit.isNew) "New sticky note" else "Sticky studio", Modifier.weight(1f))
                    if (!edit.isNew) {
                        IconButton(onClick = { vm.duplicateSticky() }) {
                            ProIconView(ProIcon.COPY, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp))
                        }
                    }
                }
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        title, { title = it.take(48) }, singleLine = true, shape = RoundedCornerShape(16.dp),
                        label = { Text("Sticky title (optional)") },
                        placeholder = { Text("e.g. Exam Formula, Chapter Checklist…") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StickyColors.forEach { c -> Swatch(c, c == color, 20) { color = c } }
                    }
                    Text("STYLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Segments(styles.map { it.second }, styles.indexOfFirst { it.first == style }.coerceAtLeast(0)) { i ->
                        style = styles[i].first
                    }
                    Segments(fonts.map { it.second }, fonts.indexOfFirst { it.first == font }.coerceAtLeast(0)) { i ->
                        font = fonts[i].first
                    }
                    SliderRow("TEXT SIZE", "%.0f pt".format(fontSize), fontSize, 18f..48f) { fontSize = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = pinned,
                            onClick = { pinned = !pinned },
                            label = { Text(if (pinned) "Pinned" else "Pin position") },
                            shape = Pill,
                        )
                        FilterChip(
                            selected = collapsed,
                            onClick = { collapsed = !collapsed },
                            label = { Text(if (collapsed) "Folded" else "Fold strip") },
                            shape = Pill,
                        )
                        TextButton(onClick = {
                            text = if (text.isBlank()) "- [ ] " else text.trimEnd() + "\n- [ ] "
                        }) { Text("+ Task") }
                    }
                    OutlinedTextField(
                        text, { text = it }, minLines = 4, shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("Write a note, formula, or checklist (- [ ] Item)…") },
                        modifier = Modifier.fillMaxWidth().background(Color(color).copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.stickyColor = color
                    vm.commitSticky(text, color, fontSize, font, collapsed, pinned, title, style)
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { if (edit.isNew) vm.stickyEdit = null else vm.deleteSticky() }) {
                    Text(if (edit.isNew) "Cancel" else "Delete", color = if (edit.isNew) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    vm.webLinkEdit?.let { edit ->
        WebLinkDialog(vm, edit)
    }

    if (pageColor) {
        val opts = listOf(0xFFFFFFFF, 0xFFFBF7EC, 0xFFF1F2F4, 0xFFFFF4E0, 0xFFEAF4FF, 0xFFEFF8EC, 0xFF22252B).map { it.toInt() }
        ColorEditorDialog(vm.currentPage?.paperColor ?: 0xFFFFFFFF.toInt(), opts, emptyList(), onDismiss = { pageColor = false }) { c ->
            vm.setPageColor(c); pageColor = false
        }
    }

    if (renaming) {
        var name by remember { mutableStateOf(vm.title) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename notebook") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, shape = Pill) },
            confirmButton = { TextButton(onClick = { vm.rename(name.trim()); renaming = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } },
        )
    }

    if (goTo) {
        var n by remember { mutableStateOf("${vm.pageIndex + 1}") }
        AlertDialog(
            onDismissRequest = { goTo = false },
            title = { Text("Go to page") },
            text = {
                OutlinedTextField(n, { v -> n = v.filter { it.isDigit() }.take(5) }, singleLine = true, shape = Pill,
                    supportingText = { Text("1 – ${vm.pages.size}") })
            },
            confirmButton = {
                TextButton(onClick = { n.toIntOrNull()?.let { vm.goToPage((it - 1).coerceIn(0, vm.pages.size - 1)) }; goTo = false }) { Text("Go") }
            },
            dismissButton = { TextButton(onClick = { goTo = false }) { Text("Cancel") } },
        )
    }

    if (showStickers) {
        ModalBottomSheet(onDismissRequest = { showStickers = false; vm.tool = Tool.LASSO }) {
            StickerSheet(vm, onAiSticker = { showStickers = false; showAssetStudio = AiAssetKind.STICKER }) { bmp ->
                showStickers = false
                vm.tool = Tool.LASSO
                vm.insertStickerBitmap(bmp) { pg, id -> canvas?.selectItems(pg, setOf(id)) }
            }
        }
    }
    if (showAiChat) AiNotesChatDialog(vm = vm, onClose = { showAiChat = false })
    showAssetStudio?.let { kind ->
        AiAssetStudioDialog(
            vm = vm,
            initialKind = kind,
            onInsertSticker = { bmp -> vm.insertStickerBitmap(bmp) { pg, id -> canvas?.selectItems(pg, setOf(id)) } },
            onApplyTemplate = { bmp -> vm.applyGeneratedTemplateBitmap(bmp) },
            onApplyCover = { bmp -> vm.applyGeneratedCoverBitmap(bmp) },
            onClose = { showAssetStudio = null },
        )
    }
    if (showLectureRecorder) AiLectureRecorderDialog(vm = vm, onClose = { showLectureRecorder = false })
    if (showCollabRoom) {
        LiveCollabRoomDialog(
            vm = vm,
            repo = Repository.get(context),
            onClose = { showCollabRoom = false },
        )
    }
}

// ============================================================== tabs + top bar

@Composable
private fun TabStrip(tabs: List<Pair<String, String>>, current: String, onSelect: (String) -> Unit, onClose: (String) -> Unit) {
    if (tabs.isEmpty()) return
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEach { (id, title) ->
            val sel = id == current
            val bg by animateFloatAsState(if (sel) 1f else 0f, label = "tab")
            Row(
                Modifier.widthIn(min = 110.dp, max = 220.dp).clip(Pill)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = bg))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 1f - bg), Pill)
                    .clickable { onSelect(id) }.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title, Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(Modifier.size(22.dp).clip(CircleShape).clickable { onClose(id) }, contentAlignment = Alignment.Center) {
                    ProIconView(ProIcon.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(11.dp))
                }
            }
        }
    }
}

@Composable
private fun BarIcon(icon: ProIcon, label: String, enabled: Boolean = true, active: Boolean = false, onClick: () -> Unit) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        active -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        Modifier.size(40.dp).elasticStretch().clip(Pill)
            .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { ProIconView(icon, tint, Modifier.size(22.dp)) }
}

@Composable
private fun TopBar(
    vm: EditorViewModel, settings: AppSettings, showSearch: Boolean, showPages: Boolean, hideTools: Boolean,
    onHome: () -> Unit, onPages: () -> Unit, onSearch: () -> Unit, onRename: () -> Unit, onFocus: () -> Unit,
    onExport: () -> Unit, onShare: () -> Unit, onSharePage: () -> Unit, onSettings: () -> Unit, onGoTo: () -> Unit,
    onScrollToggle: () -> Unit, onPaste: () -> Unit, onPageColor: () -> Unit, onPageImage: () -> Unit,
    onCards: () -> Unit = {},
    onQuiz: () -> Unit = {},
    onSlideShow: () -> Unit = {},
    onAiChat: () -> Unit = {},
    onAssetStudio: (AiAssetKind) -> Unit = {},
    onLectureRecorder: () -> Unit = {},
    onCollabRoom: () -> Unit = {},
    onAddWebLink: () -> Unit = {},
    onTemplates: () -> Unit = {},
    onImportFiles: () -> Unit = {},
    onSplit: () -> Unit = {},
) {
    val store = SettingsStore.get(LocalContext.current)
    Row(Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BarIcon(ProIcon.HOME, "Library", onClick = onHome)
        BarIcon(ProIcon.SIDEBAR, "Pages", active = showPages, onClick = onPages)
        BarIcon(ProIcon.SEARCH, "Search", active = showSearch, onClick = onSearch)
        // Undo and redo always live in the fixed top bar.
        Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outlineVariant))
        BarIcon(ProIcon.UNDO, "Undo", enabled = vm.canUndo, onClick = vm::undo)
        BarIcon(ProIcon.REDO, "Redo", enabled = vm.canRedo, onClick = vm::redo)
        if (!hideTools) {
            // Single Fixed Top Bar: all tools, tool options, and the dedicated ✨ AI button live right in the bar.
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                    ToolbarOrder.visible(settings.toolbarTools).forEach { t ->
                        ToolButton(
                            vm = vm,
                            t = t,
                            settings = settings,
                            isVerticalBar = false,
                            onReorderStep = { toolToMove, step ->
                                val visible = ToolbarOrder.visible(settings.toolbarTools)
                                val idx = visible.indexOf(toolToMove)
                                val targetIdx = (idx + step).coerceIn(0, visible.lastIndex)
                                if (idx >= 0 && targetIdx != idx) {
                                    store.update { it.copy(toolbarTools = ToolbarOrder.moveVisible(it.toolbarTools, toolToMove, visible[targetIdx])) }
                                }
                            },
                        )
                    }
                    val hasOptions = vm.styleFor(vm.tool) != null || vm.tool in setOf(Tool.ERASER, Tool.TAPE, Tool.STICKY, Tool.WEBLINK, Tool.TEXT, Tool.MINDMAP)
                    if (hasOptions) {
                        Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        ToolOptions(vm, false, settings)
                    }
                }
            }
        } else {
            Text(
                vm.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp).clickable(onClick = onRename),
            )
        }
        // Dedicated ✨ AI Button directly in the Fixed Top Bar
        var aiMenuOpen by remember { mutableStateOf(false) }
        Box {
            Surface(
                shape = Pill,
                color = Color.Transparent,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(36.dp)
                    .elasticStretch()
                    .clip(Pill)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF4285F4), Color(0xFF9B72CB), Color(0xFFD96570)),
                        ),
                    )
                    .clickable { aiMenuOpen = true },
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(Icons.Outlined.AutoAwesome, "AI Hub", tint = Color.White, modifier = Modifier.size(17.dp))
                    Text("AI", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                }
            }
            DropdownMenu(aiMenuOpen, { aiMenuOpen = false }, shape = RoundedCornerShape(22.dp)) {
                Text(
                    "✨ PRO AI STUDIO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                DropdownMenuItem(
                    text = { Text("AI Study Chat (Notes & PDF)", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null, tint = Color(0xFF4285F4)) },
                    onClick = { aiMenuOpen = false; onAiChat() },
                )
                DropdownMenuItem(
                    text = { Text("Creative Studio (Stickers / Covers)") },
                    leadingIcon = { Icon(Icons.Outlined.Palette, null, tint = Color(0xFF9B72CB)) },
                    onClick = { aiMenuOpen = false; onAssetStudio(AiAssetKind.STICKER) },
                )
                DropdownMenuItem(
                    text = { Text("Explain Current Page with AI") },
                    leadingIcon = { Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Color(0xFFD96570)) },
                    onClick = { aiMenuOpen = false; vm.explainCurrentPage() },
                )
                DropdownMenuItem(
                    text = { Text("AI Slide Show Revision") },
                    leadingIcon = { ProIconView(ProIcon.SLIDESHOW, MaterialTheme.colorScheme.primary, Modifier.size(20.dp)) },
                    onClick = { aiMenuOpen = false; onSlideShow() },
                )
                DropdownMenuItem(
                    text = { Text("AI Lecture Recorder") },
                    leadingIcon = { Icon(Icons.Outlined.Mic, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { aiMenuOpen = false; onLectureRecorder() },
                )
                DropdownMenuItem(
                    text = { Text("Summarize Page") },
                    leadingIcon = { ProIconView(ProIcon.SUMMARY, MaterialTheme.colorScheme.primary, Modifier.size(20.dp)) },
                    onClick = { aiMenuOpen = false; vm.summarize() },
                )
                DropdownMenuItem(
                    text = { Text("Generate Mind Map") },
                    leadingIcon = { ProIconView(ProIcon.MINDMAP, MaterialTheme.colorScheme.primary, Modifier.size(20.dp)) },
                    onClick = { aiMenuOpen = false; vm.generateMindMapFromNotes() },
                )
            }
        }
        if (vm.pages.isNotEmpty()) {
            Surface(shape = Pill, color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) {
                    IconButton(onClick = { vm.goToPage(vm.pageIndex - 1) }, enabled = vm.pageIndex > 0, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Previous page", Modifier.size(16.dp).graphicsLayer { rotationZ = -90f })
                    }
                    Row(
                        Modifier.clip(Pill).clickable(onClick = onGoTo).padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (vm.currentPage?.bookmarked == true) {
                            ProIconView(ProIcon.BOOKMARK, Color(0xFFE34948), Modifier.size(12.dp), ink = Color(0xFFE34948))
                            Spacer(Modifier.width(3.dp))
                        }
                        Text(
                            if (vm.currentPage?.isWhiteboard == true) "Board" else "${vm.pageIndex + 1}/${vm.pages.size}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    IconButton(onClick = { vm.goToPage(vm.pageIndex + 1) }, enabled = vm.pageIndex < vm.pages.size - 1, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Next page", Modifier.size(16.dp).graphicsLayer { rotationZ = 90f })
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
        }
        var addMenu by remember { mutableStateOf(false) }
        Box {
            BarIcon(ProIcon.ADD, "Add page") { addMenu = true }
            DropdownMenu(addMenu, { addMenu = false }, shape = RoundedCornerShape(18.dp)) {
                DropdownMenuItem(text = { Text("New page after this") }, onClick = { addMenu = false; vm.addPage() })
                DropdownMenuItem(text = { Text("New page at the end") }, onClick = { addMenu = false; vm.addPage(atEnd = true) })
                DropdownMenuItem(text = { Text("Infinite whiteboard page") }, onClick = { addMenu = false; vm.addPage(paper = PaperStyle.DOTTED, isWhiteboard = true) })
                DropdownMenuItem(text = { Text("Choose a template…") }, onClick = { addMenu = false; onTemplates() })
            }
        }
        var shareMenu by remember { mutableStateOf(false) }
        Box {
            BarIcon(ProIcon.SHARE, "Export") { shareMenu = true }
            DropdownMenu(shareMenu, { shareMenu = false }, shape = RoundedCornerShape(18.dp)) {
                DropdownMenuItem(text = { Text("Export as PDF…") }, leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) },
                    onClick = { shareMenu = false; onExport() })
                DropdownMenuItem(text = { Text("Share PDF") }, leadingIcon = { Icon(Icons.Outlined.IosShare, null) },
                    onClick = { shareMenu = false; onShare() })
                DropdownMenuItem(text = { Text("Share this page as image") }, leadingIcon = { Icon(Icons.Outlined.Image, null) },
                    onClick = { shareMenu = false; onSharePage() })
            }
        }
        var more by remember { mutableStateOf(false) }
        Box {
            BarIcon(ProIcon.MORE, "More") { more = true }
            DropdownMenu(more, { more = false }, shape = RoundedCornerShape(20.dp)) {
                Text(
                    "STUDY & AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                DropdownMenuItem(
                    text = { Text("Slide show revision") },
                    leadingIcon = { ProIconView(ProIcon.SLIDESHOW, MaterialTheme.colorScheme.primary, Modifier.size(20.dp), ink = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)) },
                    onClick = { more = false; onSlideShow() },
                )
                DropdownMenuItem(
                    text = { Text("Summarise page") },
                    leadingIcon = { ProIconView(ProIcon.SUMMARY, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp), ink = MaterialTheme.colorScheme.primary) },
                    onClick = { more = false; vm.summarize() },
                )
                DropdownMenuItem(
                    text = { Text("Flashcards") },
                    leadingIcon = { ProIconView(ProIcon.FLASHCARDS, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp), ink = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)) },
                    onClick = { more = false; onCards() },
                )
                DropdownMenuItem(
                    text = { Text("Quiz") },
                    leadingIcon = { ProIconView(ProIcon.QUIZ, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp)) },
                    onClick = { more = false; onQuiz() },
                )
                DropdownMenuItem(
                    text = { Text("Generate mind map") },
                    leadingIcon = { ProIconView(ProIcon.MINDMAP, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp), ink = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) },
                    onClick = { more = false; vm.generateMindMapFromNotes() },
                )
                DropdownMenuItem(
                    text = { Text("Flashcard from area") },
                    leadingIcon = { Icon(Icons.Outlined.Crop, null, Modifier.size(20.dp)) },
                    onClick = { more = false; vm.capturingCard = true },
                )
                DropdownMenuItem(
                    text = { Text("AI Chat (Notes & PDF)") },
                    leadingIcon = { Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },
                    onClick = { more = false; onAiChat() },
                )
                DropdownMenuItem(
                    text = { Text("AI Creative Studio (Stickers / Covers)") },
                    leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },
                    onClick = { more = false; onAssetStudio(AiAssetKind.STICKER) },
                )
                DropdownMenuItem(
                    text = { Text("AI Lecture Recorder") },
                    leadingIcon = { Icon(Icons.Outlined.Mic, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },
                    onClick = { more = false; onLectureRecorder() },
                )
                DropdownMenuItem(
                    text = { Text("Live Study Room") },
                    leadingIcon = { Icon(Icons.Outlined.Groups, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },
                    onClick = { more = false; onCollabRoom() },
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text(
                    "WORKSPACE & PAGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                DropdownMenuItem(
                    text = { Text("Add web link card…") },
                    leadingIcon = { ProIconView(ProIcon.WEBLINK, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp), ink = MaterialTheme.colorScheme.primary) },
                    onClick = { more = false; onAddWebLink() },
                )
                DropdownMenuItem(
                    text = { Text("Link item to page/URL…") },
                    leadingIcon = { Icon(Icons.Outlined.Link, null, Modifier.size(20.dp)) },
                    onClick = { more = false; vm.beginLinkFlow() },
                )
                DropdownMenuItem(
                    text = { Text("Split view") },
                    leadingIcon = { ProIconView(ProIcon.SPLIT, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp)) },
                    onClick = { more = false; onSplit() },
                )
                DropdownMenuItem(
                    text = { Text("Page template…") },
                    leadingIcon = { ProIconView(ProIcon.TEMPLATE, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp)) },
                    onClick = { more = false; onTemplates() },
                )
                DropdownMenuItem(
                    text = { Text(if (vm.currentPage?.isWhiteboard == true) "Switch to standard page" else "Infinite whiteboard") },
                    leadingIcon = { Icon(Icons.Outlined.AllInclusive, null, Modifier.size(20.dp)) },
                    onClick = { more = false; vm.toggleCurrentWhiteboard() },
                )
                DropdownMenuItem(
                    text = { Text("Import files into notebook…") },
                    leadingIcon = { Icon(Icons.Outlined.UploadFile, null, Modifier.size(20.dp)) },
                    onClick = { more = false; onImportFiles() },
                )
                DropdownMenuItem(
                    text = { Text("Page colour…") },
                    leadingIcon = { ProIconView(ProIcon.PALETTE, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp)) },
                    onClick = { more = false; onPageColor() },
                )
                DropdownMenuItem(
                    text = { Text(if (vm.currentPage?.bgImage != null) "Remove page picture" else "Picture as page…") },
                    leadingIcon = { ProIconView(ProIcon.IMAGE, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp)) },
                    onClick = { more = false; if (vm.currentPage?.bgImage != null) vm.setPageBackground(null) else onPageImage() },
                )
                DropdownMenuItem(
                    text = { Text(if (vm.currentPage?.bookmarked == true) "Remove bookmark" else "Bookmark page") },
                    leadingIcon = { ProIconView(ProIcon.BOOKMARK, Color(0xFFE34948), Modifier.size(20.dp), ink = if (vm.currentPage?.bookmarked == true) Color(0xFFE34948) else null) },
                    onClick = { more = false; vm.toggleBookmark() },
                )
                if (vm.hasClipboard) DropdownMenuItem(
                    text = { Text("Paste") },
                    leadingIcon = { Icon(Icons.Outlined.ContentPaste, null, Modifier.size(20.dp)) },
                    onClick = { more = false; onPaste() },
                )
                DropdownMenuItem(
                    text = { Text("Duplicate page") },
                    leadingIcon = { ProIconView(ProIcon.COPY, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp)) },
                    onClick = { more = false; vm.duplicatePage() },
                )
                DropdownMenuItem(
                    text = { Text("Delete page") },
                    leadingIcon = { ProIconView(ProIcon.TRASH, MaterialTheme.colorScheme.error, Modifier.size(20.dp)) },
                    onClick = { more = false; vm.deletePage() },
                    enabled = vm.pages.size > 1,
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text(
                    "NOTEBOOK",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                DropdownMenuItem(
                    text = { Text("Rename notebook") },
                    leadingIcon = { ProIconView(ProIcon.RENAME, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp)) },
                    onClick = { more = false; onRename() },
                )
                DropdownMenuItem(
                    text = { Text(if (vm.readOnly) "Turn off read only" else "Read only") },
                    leadingIcon = { ProIconView(ProIcon.LOCK, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp), ink = if (vm.readOnly) MaterialTheme.colorScheme.primary else null) },
                    onClick = { more = false; vm.toggleReadOnly() },
                )
                DropdownMenuItem(
                    text = { Text("Go to page…") },
                    leadingIcon = { ProIconView(ProIcon.PAGES, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp)) },
                    onClick = { more = false; onGoTo() },
                )
                DropdownMenuItem(
                    text = { Text(if (settings.scroll == ScrollDirection.VERTICAL) "Scroll horizontally" else "Scroll vertically") },
                    leadingIcon = { Icon(Icons.Outlined.SwapVert, null, Modifier.size(20.dp)) },
                    onClick = { more = false; onScrollToggle() },
                )
                DropdownMenuItem(
                    text = { Text("Focus mode") },
                    leadingIcon = { ProIconView(ProIcon.FOCUS, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp)) },
                    onClick = { more = false; onFocus() },
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    leadingIcon = { ProIconView(ProIcon.SETTINGS, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp)) },
                    onClick = { more = false; onSettings() },
                )
            }
        }
    }
}

// ============================================================== search

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchBar(vm: EditorViewModel) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = vm.searchQuery, onValueChange = vm::search, singleLine = true,
                placeholder = { Text(if (vm.isPdf) "Smart AI & exact search in PDF and notes" else "Smart AI & exact search across notes") },
                leadingIcon = { ProIconView(ProIcon.SEARCH, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp)) },
                shape = Pill, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    vm.searching -> "Searching…"
                    vm.searchQuery.isBlank() -> ""
                    vm.searchHits.isEmpty() -> "No results"
                    else -> "${vm.activeHit + 1} of ${vm.searchHits.size}"
                },
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = { vm.stepHit(-1) }, enabled = vm.searchHits.isNotEmpty()) { Icon(Icons.Filled.KeyboardArrowUp, "Previous") }
            IconButton(onClick = { vm.stepHit(1) }, enabled = vm.searchHits.isNotEmpty()) { Icon(Icons.Filled.KeyboardArrowDown, "Next") }
            IconButton(onClick = vm::clearSearch) { Icon(Icons.Filled.Close, "Clear") }
        }
        vm.searchAiAnswer?.let { ai ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProIconView(ProIcon.CUSTOM_AI, MaterialTheme.colorScheme.primary, Modifier.size(16.dp), ink = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        Spacer(Modifier.width(6.dp))
                        Text("AI Search Insight · ${vm.aiEngineLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    if (ai.answer.isNotBlank()) {
                        Text(ai.answer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    if (ai.expandedTerms.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ai.expandedTerms.forEach { term ->
                                Surface(
                                    shape = Pill,
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.clip(Pill).clickable { vm.search(term) },
                                ) {
                                    Text(term, Modifier.padding(horizontal = 9.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (vm.searchHits.isNotEmpty()) {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
                itemsIndexed(vm.searchHits) { i, hit ->
                    Row(
                        Modifier.fillMaxWidth().clip(Pill)
                            .background(if (i == vm.activeHit) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { vm.jumpToHit(i) }.padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("p.${hit.page + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(44.dp))
                        Text(hit.snippet, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ============================================================== pages panel

/** Left panel with page thumbnails (drag to reorder) and a bookmark outline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PagesPanel(vm: EditorViewModel, onJump: (Int) -> Unit, onClose: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.width(200.dp).fillMaxHeight(), shadowElevation = 2.dp) {
        Column {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Pages", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.addPage(atEnd = true) }) { Icon(Icons.Outlined.Add, "Add page") }
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close") }
            }
            PrimaryTabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                Tab(tab == 0, { tab = 0 }, text = { Text("All") })
                Tab(tab == 1, { tab = 1 }, text = { Text("Bookmarks") })
            }
            if (tab == 0) {
                val listState = rememberLazyListState()
                val reorder = rememberReorderableLazyListState(listState) { from, to -> vm.movePage(from.index, to.index) }
                LazyColumn(state = listState, contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(vm.pages, key = { _, p -> p.id }) { i, page ->
                        ReorderableItem(reorder, key = page.id) { dragging ->
                            val lift by animateFloatAsState(if (dragging) 1.06f else 1f, label = "lift")
                            PageThumb(vm, i, page, Modifier.graphicsLayer { scaleX = lift; scaleY = lift }.longPressDraggableHandle(), onJump)
                        }
                    }
                }
            } else {
                val marks = vm.pages.indices.filter { vm.pages[it].bookmarked }
                if (marks.isEmpty()) {
                    Text(
                        "Bookmark pages from the ⋯ menu to list them here.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp),
                    )
                }
                LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(marks.size) { k -> PageThumb(vm, marks[k], vm.pages[marks[k]], Modifier, onJump) }
                }
            }
        }
    }
}

@Composable
private fun PageThumb(vm: EditorViewModel, i: Int, page: Page, modifier: Modifier, onJump: (Int) -> Unit) {
    val thumb by produceState<Bitmap?>(null, page) { value = vm.thumbnail(i, 220) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(page.width / page.height)
                .shadow(3.dp, RoundedCornerShape(8.dp)).background(Color.White)
                .border(if (i == vm.pageIndex) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                .clickable { onJump(i) },
        ) {
            thumb?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
            if (page.bookmarked) ProIconView(ProIcon.BOOKMARK, Color(0xFFE34948), Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp), ink = Color(0xFFE34948))
        }
        Text("${i + 1}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

// ============================================================== tool dock

@Composable
private fun BoxScope.ToolDock(
    vm: EditorViewModel,
    settings: AppSettings,
    area: IntSize,
    backdrop: LayerBackdrop? = null,
    onDock: (ToolbarDock) -> Unit,
) {
    val dock = settings.toolbarDock
    var drag by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    val align = when (dock) {
        ToolbarDock.TOP, ToolbarDock.HEADER -> Alignment.TopCenter
        ToolbarDock.BOTTOM -> Alignment.BottomCenter
        ToolbarDock.LEFT -> Alignment.CenterStart
        ToolbarDock.RIGHT -> Alignment.CenterEnd
    }
    val lift by animateFloatAsState(if (dragging) 1.04f else 1f, label = "lift")

    AnimatedContent(
        targetState = dock,
        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.9f)) },
        modifier = Modifier.align(align).padding(12.dp).navigationBarsPadding()
            .graphicsLayer { translationX = drag.x; translationY = drag.y; scaleX = lift; scaleY = lift },
        label = "dock",
    ) { d ->
        val isVertical = d == ToolbarDock.LEFT || d == ToolbarDock.RIGHT
        val grip = @Composable {
            Box(
                Modifier.size(26.dp).pointerInput(area) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            val w = area.width.toFloat(); val h = area.height.toFloat()
                            val base = when (d) {
                                ToolbarDock.TOP, ToolbarDock.HEADER -> Offset(w / 2, 40f)
                                ToolbarDock.BOTTOM -> Offset(w / 2, h - 40f)
                                ToolbarDock.LEFT -> Offset(40f, h / 2)
                                ToolbarDock.RIGHT -> Offset(w - 40f, h / 2)
                            }
                            val at = base + drag
                            val dists = mapOf(
                                ToolbarDock.TOP to at.y, ToolbarDock.BOTTOM to h - at.y,
                                ToolbarDock.LEFT to at.x, ToolbarDock.RIGHT to w - at.x,
                            )
                            drag = Offset.Zero
                            onDock(dists.minByOrNull { it.value }!!.key)
                        },
                        onDragCancel = { dragging = false; drag = Offset.Zero },
                    ) { change, amount -> change.consume(); drag += amount }
                },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.DragIndicator, "Move toolbar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
        }
        val narrow = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 600
        val store = SettingsStore.get(LocalContext.current)
        val tools = @Composable {
            // Phones: undo and redo live in the bar itself, to save room.
            ToolbarOrder.visible(settings.toolbarTools).forEach { t ->
                ToolButton(
                    vm = vm,
                    t = t,
                    settings = settings,
                    isVerticalBar = isVertical,
                    onReorderStep = { toolToMove, step ->
                        val visible = ToolbarOrder.visible(settings.toolbarTools)
                        val idx = visible.indexOf(toolToMove)
                        val targetIdx = (idx + step).coerceIn(0, visible.lastIndex)
                        if (idx >= 0 && targetIdx != idx) {
                            store.update { it.copy(toolbarTools = ToolbarOrder.moveVisible(it.toolbarTools, toolToMove, visible[targetIdx])) }
                        }
                    },
                )
            }
        }
        val hasOptions = vm.styleFor(vm.tool) != null || vm.tool in setOf(Tool.ERASER, Tool.TAPE, Tool.STICKY, Tool.WEBLINK, Tool.TEXT, Tool.MINDMAP)
        // One bar: tools, then the active tool's colours and sizes after a thin divider.
        val divider = @Composable {
            if (isVertical) Box(Modifier.padding(vertical = 6.dp).height(1.dp).width(22.dp).background(MaterialTheme.colorScheme.outlineVariant))
            else Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outlineVariant))
        }
        // Fades while the pen is down so you can write right under it.
        val fade by animateFloatAsState(if (vm.penActive) 0.22f else 1f, tween(if (vm.penActive) 220 else 420), label = "fade")
        // Never wider (or taller) than the free space: leaves room for undo / redo and the page counter.
        val dens = LocalDensity.current
        val areaW = with(dens) { area.width.toDp() }
        val areaH = with(dens) { area.height.toDp() }
        val limit = if (isVertical) Modifier.heightIn(max = (areaH - 150.dp).coerceAtLeast(160.dp))
            else Modifier.widthIn(max = (areaW - if (narrow) 24.dp else 300.dp).coerceAtLeast(220.dp))
        val bar = @Composable {
            OptionalLiquidGlassBox(
                backdrop = backdrop,
                enabled = settings.liquidGlass,
                shape = Pill,
                fallbackColor = glassColor(),
                modifier = limit.shadow(if (dragging) 14.dp else 6.dp, Pill).graphicsLayer { alpha = fade },
            ) {
                if (isVertical) Column(Modifier.padding(5.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    grip(); tools(); if (hasOptions) { divider(); ToolOptions(vm, true, settings) }
                }
                else Row(Modifier.padding(5.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                    grip(); tools(); if (hasOptions) { divider(); ToolOptions(vm, false, settings) }
                }
            }
        }
        bar()
    }
}

/** Right-edge panel (top bar layout): pen types for the pen, then sizes and colours, top to bottom. */
@Composable
private fun SidePanel(vm: EditorViewModel, settings: AppSettings, modifier: Modifier) {
    val hasOptions = vm.styleFor(vm.tool) != null || vm.tool in setOf(Tool.ERASER, Tool.TAPE, Tool.STICKY, Tool.WEBLINK, Tool.TEXT, Tool.MINDMAP)
    val shape = RoundedCornerShape(26.dp)
    AnimatedVisibility(
        hasOptions, modifier = modifier,
        enter = fadeIn() + androidx.compose.animation.slideInHorizontally { it / 2 },
        exit = fadeOut() + androidx.compose.animation.slideOutHorizontally { it / 2 },
    ) {
        Surface(shape = shape, color = glassColor(), shadowElevation = 6.dp, modifier = Modifier.glassEdge(shape)) {
            Column(
                Modifier.padding(vertical = 8.dp, horizontal = 4.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (vm.tool == Tool.PEN) {
                    PenType.entries.forEach { type ->
                        val on = vm.penType == type
                        Box(
                            Modifier.padding(2.dp).size(40.dp).clip(CircleShape)
                                .background(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { vm.penType = type },
                            contentAlignment = Alignment.Center,
                        ) { PenTypeGlyph(type, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
                    }
                    HorizontalDivider(Modifier.width(26.dp).padding(vertical = 6.dp))
                }
                ToolOptions(vm, true, settings)
            }
        }
    }
}

/** Tiny sample stroke that shows what each pen type draws like. */
@Composable
private fun PenTypeGlyph(type: PenType, tint: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.1f, h * 0.7f)
            cubicTo(w * 0.35f, h * 0.05f, w * 0.6f, h * 0.95f, w * 0.9f, h * 0.3f)
        }
        val width = when (type) {
            PenType.BRUSH -> 4.5f
            PenType.BALL -> 1.6f
            else -> 2.4f
        }.dp.toPx()
        val dash = if (type == PenType.DASHED) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null
        drawPath(
            path, tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width, cap = androidx.compose.ui.graphics.StrokeCap.Round, pathEffect = dash),
        )
    }
}

private fun toolDisplayName(t: Tool): String = when (t) {
    Tool.PEN -> "Pen"
    Tool.HIGHLIGHTER -> "Highlighter"
    Tool.ERASER -> "Eraser"
    Tool.SHAPE -> "Shapes"
    Tool.LASSO -> "Lasso Select"
    Tool.MINDMAP -> "Mind Map"
    Tool.IMAGE -> "Insert Image"
    Tool.TEXT -> "Text Box"
    Tool.STICKY -> "Sticky Note"
    Tool.WEBLINK -> "Web Link"
    Tool.TAPE -> "Washi Tape"
    Tool.STICKER -> "Stickers"
    Tool.LASER -> "Laser Pointer"
}

/** Compact pill with the active tool's colours and sizes. Hidden when the tool has none. */
@Composable
private fun OptionsBar(vm: EditorViewModel, vertical: Boolean, settings: AppSettings) {
    val hasOptions = vm.styleFor(vm.tool) != null || vm.tool in setOf(Tool.ERASER, Tool.TAPE, Tool.STICKY, Tool.WEBLINK, Tool.TEXT, Tool.MINDMAP)
    AnimatedVisibility(hasOptions, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut() + scaleOut(targetScale = 0.9f)) {
        Surface(shape = Pill, color = glassColor(), shadowElevation = 4.dp, modifier = Modifier.glassEdge(Pill)) {
            if (vertical) Column(Modifier.padding(vertical = 6.dp, horizontal = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) { ToolOptions(vm, true, settings) }
            else Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) { ToolOptions(vm, false, settings) }
        }
    }
}

/** Where a tool's settings card opens, so it never leaves the screen. */
private fun cardPlacement(dock: ToolbarDock, density: Float): Pair<Alignment, IntOffset> = when (dock) {
    ToolbarDock.BOTTOM -> Alignment.BottomCenter to IntOffset(0, (-56 * density).toInt())
    ToolbarDock.LEFT -> Alignment.CenterStart to IntOffset((56 * density).toInt(), 0)
    ToolbarDock.RIGHT -> Alignment.CenterEnd to IntOffset((-56 * density).toInt(), 0)
    else -> Alignment.TopCenter to IntOffset(0, (56 * density).toInt())
}

@Composable
private fun ToolButton(
    vm: EditorViewModel,
    t: Tool,
    settings: AppSettings,
    isVerticalBar: Boolean = false,
    onReorderStep: ((Tool, Int) -> Unit)? = null,
) {
    val selected = vm.tool == t
    val motion = LocalMotionStyle.current
    val ink = Color(vm.styleFor(t)?.color ?: when (t) {
        Tool.TAPE -> vm.tapeColor
        Tool.STICKY -> vm.stickyColor
        Tool.WEBLINK -> vm.webLinkColor
        else -> vm.penStyle.color
    })
    val bg by animateFloatAsState(if (selected) 1f else 0f, label = "bg")
    val sz = (40 * settings.toolbarSize.scale).dp
    val density = LocalDensity.current.density
    var card by remember { mutableStateOf(false) }
    var hovered by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var stretchDx by remember { mutableFloatStateOf(0f) }
    var stretchDy by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(card) { vm.cardOpen = card }

    val springDamping = when (motion) {
        MotionStyle.ULTRA_SPRING -> 0.56f
        MotionStyle.MINIMAL_CRISP -> Spring.DampingRatioNoBouncy
        MotionStyle.REDUCED -> 1f
    }
    val stretchX by animateFloatAsState(
        targetValue = if (dragging) {
            1.14f + (kotlin.math.abs(stretchDx) / (75f * density)).coerceIn(0f, 0.24f) - (kotlin.math.abs(stretchDy) / (150f * density)).coerceIn(0f, 0.12f)
        } else if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = springDamping, stiffness = Spring.StiffnessMediumLow),
        label = "sx",
    )
    val stretchY by animateFloatAsState(
        targetValue = if (dragging) {
            1.14f + (kotlin.math.abs(stretchDy) / (75f * density)).coerceIn(0f, 0.24f) - (kotlin.math.abs(stretchDx) / (150f * density)).coerceIn(0f, 0.12f)
        } else if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = springDamping, stiffness = Spring.StiffnessMediumLow),
        label = "sy",
    )
    val offsetX by animateFloatAsState(
        targetValue = if (dragging) stretchDx.coerceIn(-22f * density, 22f * density) else 0f,
        animationSpec = spring(dampingRatio = springDamping, stiffness = Spring.StiffnessMedium),
        label = "ox",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (dragging) stretchDy.coerceIn(-22f * density, 22f * density) else 0f,
        animationSpec = spring(dampingRatio = springDamping, stiffness = Spring.StiffnessMedium),
        label = "oy",
    )

    val dragReorderMod = if (onReorderStep != null) {
        Modifier.pointerInput(t, isVerticalBar) {
            val stepPx = 38f * density
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    dragging = true
                    hovered = false
                    dragAccum = 0f
                    stretchDx = 0f
                    stretchDy = 0f
                },
                onDragEnd = {
                    dragging = false
                    dragAccum = 0f
                    stretchDx = 0f
                    stretchDy = 0f
                },
                onDragCancel = {
                    dragging = false
                    dragAccum = 0f
                    stretchDx = 0f
                    stretchDy = 0f
                },
                onDrag = { change, amount ->
                    change.consume()
                    stretchDx = (stretchDx + amount.x).coerceIn(-26f * density, 26f * density)
                    stretchDy = (stretchDy + amount.y).coerceIn(-26f * density, 26f * density)
                    val axisDelta = if (isVerticalBar) amount.y else amount.x
                    dragAccum += axisDelta
                    if (kotlin.math.abs(dragAccum) >= stepPx) {
                        val dir = if (dragAccum > 0f) 1 else -1
                        onReorderStep(t, dir)
                        dragAccum -= dir * stepPx
                    }
                },
            )
        }
    } else Modifier

    Box {
        Box(
            Modifier.padding(1.dp).size(sz)
                .elasticStretch(enabled = !dragging, pressScale = 0.91f)
                .graphicsLayer {
                    scaleX = stretchX
                    scaleY = stretchY
                    translationX = offsetX
                    translationY = offsetY
                }
                .clip(Pill)
                .background(
                    if (dragging) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = bg)
                )
                .border(
                    width = if (dragging) 1.5.dp else 0.dp,
                    color = if (dragging) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = Pill,
                )
                .pointerInput(t) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Enter -> hovered = true
                                PointerEventType.Exit -> hovered = false
                            }
                        }
                    }
                }
                .then(dragReorderMod)
                .clickable {
                    when {
                        !selected -> vm.tool = t
                        t == Tool.IMAGE || t == Tool.STICKER -> vm.reopenTool++
                        else -> card = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val tint = if (selected || dragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            ToolGlyph(t, tint, ink, Modifier.size((23 * settings.toolbarSize.scale).dp), active = selected || dragging)
        }

        if (hovered && !dragging && !card && settings.showHoverTooltips && settings.hoverCursor) {
            val (align, off) = when (settings.toolbarDock) {
                ToolbarDock.BOTTOM -> Alignment.TopCenter to IntOffset(0, (-38 * density).toInt())
                ToolbarDock.LEFT -> Alignment.CenterEnd to IntOffset((48 * density).toInt(), 0)
                ToolbarDock.RIGHT -> Alignment.CenterStart to IntOffset((-48 * density).toInt(), 0)
                else -> Alignment.BottomCenter to IntOffset(0, (38 * density).toInt())
            }
            Popup(alignment = align, offset = off, properties = PopupProperties(focusable = false)) {
                Surface(
                    shape = Pill,
                    color = Color(0xEC161B28),
                    contentColor = Color.White,
                    shadowElevation = 6.dp,
                ) {
                    Text(
                        toolDisplayName(t),
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        if (card) {
            val (align, offset) = cardPlacement(settings.toolbarDock, density)
            Popup(alignment = align, offset = offset, onDismissRequest = { card = false }, properties = PopupProperties(focusable = true)) {
                when (t) {
                    Tool.PEN -> PenCard(vm)
                    Tool.HIGHLIGHTER -> HighlighterCard(vm)
                    Tool.SHAPE -> ShapeCard(vm)
                    Tool.ERASER -> EraserCard(vm)
                    Tool.LASSO -> LassoCard()
                    Tool.LASER -> LaserCard()
                    Tool.TEXT -> TextCard(vm)
                    Tool.TAPE -> Card("Tape") {
                        val pickTape = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.useTapeImage(uri) }
                        Segments(listOf("Straight", "Freehand"), if (vm.tapeFreehand) 1 else 0) { vm.tapeFreehand = it == 1 }
                        Row(verticalAlignment = Alignment.CenterVertically) { CustomSwatches(TapeColors, vm.tapeColor, vm) { vm.tapeColor = it } }
                        TapePatternPicker(vm) { pickTape.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    }
                    Tool.STICKY -> ColorCard("Sticky note", StickyColors, vm.stickyColor, vm) { vm.stickyColor = it }
                    Tool.WEBLINK -> ColorCard("Web link card", WebLinkColors, vm.webLinkColor, vm) { vm.webLinkColor = it }
                    Tool.MINDMAP -> MindMapCard(vm)
                    Tool.STICKER, Tool.IMAGE -> Unit
                }
            }
        }
    }
}

@Composable
private fun ToolOptions(vm: EditorViewModel, vertical: Boolean, settings: AppSettings) {
    val store = SettingsStore.get(LocalContext.current)
    AnimatedContent(
        targetState = vm.tool,
        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.92f)) togetherWith fadeOut() },
        label = "options",
    ) { t ->
        val content = @Composable {
            val style = vm.styleFor(t)
            when {
                style != null -> StyleOptions(vm, t, style)
                t == Tool.ERASER -> {
                    SizeDots(listOf(8f, 16f, 30f), listOf(8f, 16f, 30f)[vm.eraserSize], ring = true) { s ->
                        vm.eraserSize = listOf(8f, 16f, 30f).indexOf(s)
                    }
                    MiniChip(if (settings.eraserMode == EraserMode.PIXEL) "Pixel" else "Stroke") {
                        store.update { it.copy(eraserMode = if (it.eraserMode == EraserMode.PIXEL) EraserMode.STROKE else EraserMode.PIXEL) }
                    }
                }
                t == Tool.TEXT -> SizeDots(listOf(28f, 40f, 64f), vm.textSize) { vm.textSize = it }
                t == Tool.TAPE -> {
                    CustomSwatches(TapeColors, vm.tapeColor, vm) { vm.tapeColor = it }
                    MiniChip(if (vm.tapeFreehand) "Freehand" else "Straight") { vm.tapeFreehand = !vm.tapeFreehand }
                }
                t == Tool.STICKY -> CustomSwatches(StickyColors, vm.stickyColor, vm) { vm.stickyColor = it }
                t == Tool.WEBLINK -> CustomSwatches(WebLinkColors, vm.webLinkColor, vm) { vm.webLinkColor = it }
                t == Tool.MINDMAP -> {
                    MindPalette.take(5).forEach { c ->
                        Swatch(c, c == vm.mindColor, 18) {
                            vm.recolorMindNode(color = c)
                        }
                    }
                    EditorViewModel.MindLayout.entries.forEach { layout ->
                        MiniChip(layout.label) { vm.autoLayoutMindMap(layout) }
                    }
                    MiniChip("Fit Map") { vm.fitMindMapToView() }
                    MiniChip(if (vm.moveMindSubtree) "Subtree: On" else "Subtree: Off") { vm.moveMindSubtree = !vm.moveMindSubtree }
                    MiniChip("Cards") { vm.flashcardsFromMindMap() }
                    MiniChip("AI Map") { vm.generateMindMapFromNotes() }
                }
                else -> Unit
            }
        }
        if (vertical) Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
        else Row(verticalAlignment = Alignment.CenterVertically) { content() }
    }
}

@Composable
private fun MiniChip(label: String, onClick: () -> Unit) {
    Surface(shape = Pill, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(3.dp).elasticStretch().clickable(onClick = onClick)) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CustomSwatches(base: List<Int>, current: Int, vm: EditorViewModel, onPick: (Int) -> Unit) {
    var custom by remember { mutableStateOf(false) }
    base.forEach { c -> Swatch(c, c == current, 18) { onPick(c) } }
    if (current !in base) Swatch(current, true, 18) { custom = true }
    AddSwatchButton { custom = true }
    if (custom) ColorEditorDialog(current, PenPalette, vm.recentColors(), onDismiss = { custom = false }) { onPick(it); vm.pushRecent(it); custom = false }
}

@Composable
private fun AddSwatchButton(onClick: () -> Unit) {
    Box(
        Modifier.padding(3.dp).size(22.dp).clip(CircleShape)
            .background(Brush.sweepGradient(listOf(Color(0xFFFF3B30), Color(0xFFFFCC00), Color(0xFF34C759), Color(0xFF0A84FF), Color(0xFFAF52DE), Color(0xFFFF3B30))))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Add, "Custom colour", Modifier.size(11.dp))
        }
    }
}

@Composable
private fun StyleOptions(vm: EditorViewModel, t: Tool, style: ToolStyle) {
    var editing by remember { mutableStateOf<Int?>(null) }
    var adding by remember { mutableStateOf(false) }
    var removeMenu by remember { mutableStateOf<Int?>(null) }
    style.colors.forEachIndexed { i, c ->
        val selected = c == style.color
        Box {
            Swatch(c, selected, 18, onLongClick = { removeMenu = i }) { if (selected) editing = i else vm.updateStyle(t, style.copy(color = c)) }
            DropdownMenu(removeMenu == i, { removeMenu = null }, shape = RoundedCornerShape(16.dp)) {
                DropdownMenuItem(text = { Text("Edit colour") }, onClick = { removeMenu = null; editing = i })
                DropdownMenuItem(text = { Text("Remove") }, enabled = style.colors.size > 1, onClick = { removeMenu = null; vm.removeSwatch(t, i) })
            }
        }
    }
    if (style.colors.size < 8) AddSwatchButton { adding = true }
    Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(18.dp).background(MaterialTheme.colorScheme.outlineVariant))
    SizeDots(style.sizes, style.size) { sz -> vm.updateStyle(t, style.copy(size = sz)) }
    val presets = if (t == Tool.HIGHLIGHTER) HighlighterPalette + PenPalette else PenPalette
    editing?.let { slot ->
        ColorEditorDialog(style.colors[slot], presets, vm.recentColors(), onDismiss = { editing = null }) { picked ->
            val colors = style.colors.toMutableList().also { it[slot] = picked }
            vm.updateStyle(t, style.copy(colors = colors, color = picked))
            vm.pushRecent(picked)
            editing = null
        }
    }
    if (adding) {
        ColorEditorDialog(style.color, presets, vm.recentColors(), onDismiss = { adding = false }) { picked -> vm.addSwatch(t, picked); adding = false }
    }
}

@Composable
private fun SizeDots(sizes: List<Float>, current: Float, ring: Boolean = false, onPick: (Float) -> Unit) {
    sizes.forEachIndexed { i, s ->
        val selected = s == current
        Box(
            Modifier.size(30.dp).clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                .clickable { onPick(s) },
            contentAlignment = Alignment.Center,
        ) {
            val w = (2 + 3 * i).dp
            if (ring) Box(Modifier.size((7 + 5 * i).dp).border(1.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape))
            else Box(Modifier.width(16.dp).height(w).clip(Pill).background(MaterialTheme.colorScheme.onSurface))
        }
    }
}

// ============================================================== cards

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), shadowElevation = 14.dp, color = MaterialTheme.colorScheme.surface, modifier = Modifier.width(340.dp)) {
        Column(Modifier.padding(18.dp).heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
            content()
        }
    }
}

/** Pill-shaped segmented control. */
@Composable
private fun Segments(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(Pill).background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp)) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).height(34.dp).clip(Pill)
                    .background(if (sel) MaterialTheme.colorScheme.surface else Color.Transparent).clickable { onPick(i) },
                contentAlignment = Alignment.Center,
            ) { Text(o, style = MaterialTheme.typography.labelSmall, color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
        }
    }
}

@Composable
private fun StrokePreview(color: Color, width: Float, taper: Boolean) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(70.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.6f)
            cubicTo(size.width * 0.3f, size.height * 1.05f, size.width * 0.45f, size.height * 0.05f, size.width * 0.9f, size.height * 0.45f)
        }
        if (taper) for (k in 0..3) drawPath(path, color, style = Stroke(width * (0.4f + k * 0.2f), cap = StrokeCap.Round), alpha = 0.35f)
        drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PenCard(vm: EditorViewModel) {
    val style = vm.penStyle
    val presets by vm.presets.collectAsState()
    val store = SettingsStore.get(LocalContext.current)
    val s by store.settings.collectAsState()
    var editingPenName by remember { mutableStateOf(false) }
    var savingPreset by remember { mutableStateOf(false) }
    var renamingPreset by remember { mutableStateOf<PenPreset?>(null) }

    Card("Pen Studio · ${vm.penType.label}") {
        // Active stylus badge with inline rename
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth().clickable { editingPenName = true },
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("ACTIVE STYLUS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(s.penName.ifBlank { "Pro Stylus" }, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
                Text("Rename", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Framed curve preview
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                StrokePreview(Color(style.color), style.size * vm.penType.sizeFactor * 2.2f, vm.penType != PenType.BALL)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PenType.entries.forEach { type ->
                val sel = vm.penType == type
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(if (sel) 1.5.dp else 0.dp, if (sel) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(18.dp))
                        .clickable { vm.penType = type }.padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PenGlyph(type, MaterialTheme.colorScheme.onSurface, Color(style.color), Modifier.size(28.dp))
                    Text(type.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
        SliderRow("THICKNESS", "%.1f pt".format(style.size), style.size, 0.5f..20f) { v ->
            val sizes = style.sizes.map { if (it == style.size) v else it }
            vm.updateStyle(Tool.PEN, style.copy(size = v, sizes = sizes))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("FAVOURITE PENS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { savingPreset = true }) {
                Icon(Icons.Outlined.StarBorder, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Save pen")
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.forEach { p ->
                PresetChip(
                    p = p,
                    onClick = { vm.applyPreset(p) },
                    onRename = { renamingPreset = p },
                    onRemove = { vm.removePreset(p) },
                )
            }
        }
        Text("PAPER FEEL", style = MaterialTheme.typography.labelSmall)
        ToggleLine("Writing sound", s.paperSound) { v -> store.update { it.copy(paperSound = v) } }
        if (s.paperSound) SliderRow("VOLUME", "%.0f%%".format(s.paperVolume * 100), s.paperVolume, 0.05f..1f) { v -> store.update { it.copy(paperVolume = v) } }
        ToggleLine("Paper haptics", s.paperHaptics) { v -> store.update { it.copy(paperHaptics = v) } }
    }

    if (editingPenName) {
        var name by remember(s.penName) { mutableStateOf(s.penName) }
        AlertDialog(
            onDismissRequest = { editingPenName = false },
            title = { Text("Stylus name") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, shape = Pill, label = { Text("Pen display name") }) },
            confirmButton = { TextButton(onClick = { vm.setPenName(name); editingPenName = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editingPenName = false }) { Text("Cancel") } },
        )
    }

    if (savingPreset) {
        var label by remember { mutableStateOf("${vm.penType.label} ${style.size.roundToInt()}") }
        AlertDialog(
            onDismissRequest = { savingPreset = false },
            title = { Text("Name this pen preset") },
            text = { OutlinedTextField(label, { label = it }, singleLine = true, shape = Pill) },
            confirmButton = { TextButton(onClick = { vm.savePreset(label); savingPreset = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { savingPreset = false }) { Text("Cancel") } },
        )
    }

    renamingPreset?.let { p ->
        var label by remember(p) { mutableStateOf(p.name) }
        AlertDialog(
            onDismissRequest = { renamingPreset = null },
            title = { Text("Rename preset") },
            text = { OutlinedTextField(label, { label = it }, singleLine = true, shape = Pill) },
            confirmButton = { TextButton(onClick = { vm.renamePreset(p, label); renamingPreset = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renamingPreset = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PresetChip(p: PenPreset, onClick: () -> Unit, onRename: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(Pill).border(1.dp, MaterialTheme.colorScheme.outlineVariant, Pill)
                .combinedClickableCompat(onClick, { menu = true }).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PenGlyph(runCatching { PenType.valueOf(p.type) }.getOrDefault(PenType.FOUNTAIN), MaterialTheme.colorScheme.onSurface, Color(p.color), Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(width = 16.dp, height = (p.size / 2f).coerceIn(2f, 8f).dp).clip(Pill).background(Color(p.color)))
            if (p.name.isNotBlank()) {
                Spacer(Modifier.width(5.dp))
                Text(p.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        DropdownMenu(menu, { menu = false }, shape = RoundedCornerShape(16.dp)) {
            DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
            DropdownMenuItem(text = { Text("Remove") }, onClick = { menu = false; onRemove() })
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit, onLong: () -> Unit) =
    this.then(Modifier.combinedClickable(onClick = onClick, onLongClick = onLong))

@Composable
private fun HighlighterCard(vm: EditorViewModel) {
    val style = vm.highlighterStyle
    val store = SettingsStore.get(LocalContext.current)
    val s by store.settings.collectAsState()
    Card("Highlighter") {
        StrokePreview(Color(style.color).copy(alpha = s.highlighterOpacity), style.size * 1.2f, false)
        SliderRow("THICKNESS", "%.0f".format(style.size), style.size, 6f..70f) { v ->
            val sizes = style.sizes.map { if (it == style.size) v else it }
            vm.updateStyle(Tool.HIGHLIGHTER, style.copy(size = v, sizes = sizes))
        }
        SliderRow("OPACITY", "%.0f%%".format(s.highlighterOpacity * 100), s.highlighterOpacity, 0.3f..0.9f) { v -> store.update { it.copy(highlighterOpacity = v) } }
        Text("LINE", style = MaterialTheme.typography.labelSmall)
        Segments(HighlighterMode.entries.map { it.label }, s.highlighterMode.ordinal) { i -> store.update { it.copy(highlighterMode = HighlighterMode.entries[i]) } }
        ToggleLine("Draw behind ink", s.highlighterBehind) { v -> store.update { it.copy(highlighterBehind = v) } }
    }
}

@Composable
private fun EraserCard(vm: EditorViewModel) {
    val store = SettingsStore.get(LocalContext.current)
    val s by store.settings.collectAsState()
    Card("Eraser") {
        Segments(EraserMode.entries.map { it.label }, s.eraserMode.ordinal) { i -> store.update { it.copy(eraserMode = EraserMode.entries[i]) } }
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            SizeDots(listOf(8f, 16f, 30f), listOf(8f, 16f, 30f)[vm.eraserSize], ring = true) { v -> vm.eraserSize = listOf(8f, 16f, 30f).indexOf(v) }
        }
        Text("SCRIBBLE SENSITIVITY", style = MaterialTheme.typography.labelSmall)
        Segments(ScribbleSensitivity.entries.map { it.label }, s.scribbleSensitivity.ordinal) { i ->
            store.update { it.copy(scribbleSensitivity = ScribbleSensitivity.entries[i]) }
        }
        ToggleLine("Erase highlighter only", s.eraseHighlighterOnly) { v -> store.update { it.copy(eraseHighlighterOnly = v) } }
        ToggleLine("Switch back after erasing", s.eraserAutoSwitch) { v -> store.update { it.copy(eraserAutoSwitch = v) } }
        ToggleLine("Scribble to erase with the pen", s.scribbleToErase) { v -> store.update { it.copy(scribbleToErase = v) } }
        ToggleLine("Erase mid-stroke while writing", s.liveScribbleErase) { v -> store.update { it.copy(liveScribbleErase = v) } }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { vm.clearPageInk() },
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.CleaningServices, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(8.dp))
                Text("Clear all ink on this page", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LassoCard() {
    val store = SettingsStore.get(LocalContext.current)
    val s by store.settings.collectAsState()
    Card("Lasso") {
        Segments(LassoMode.entries.map { it.label }, s.lassoMode.ordinal) { i -> store.update { it.copy(lassoMode = LassoMode.entries[i]) } }
        Text("SELECT", style = MaterialTheme.typography.labelSmall)
        val f = s.lassoFilter
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(f.ink, { store.update { it.copy(lassoFilter = f.copy(ink = !f.ink)) } }, label = { Text("Ink") }, shape = Pill)
            FilterChip(f.highlighter, { store.update { it.copy(lassoFilter = f.copy(highlighter = !f.highlighter)) } }, label = { Text("Highlighter") }, shape = Pill)
            FilterChip(f.text, { store.update { it.copy(lassoFilter = f.copy(text = !f.text)) } }, label = { Text("Text") }, shape = Pill)
            FilterChip(f.images, { store.update { it.copy(lassoFilter = f.copy(images = !f.images)) } }, label = { Text("Images & stickers") }, shape = Pill)
            FilterChip(f.other, { store.update { it.copy(lassoFilter = f.copy(other = !f.other)) } }, label = { Text("Notes & tape") }, shape = Pill)
        }
        ToggleLine("Select anything the lasso touches", s.lassoTouching) { v -> store.update { it.copy(lassoTouching = v) } }
        ToggleLine("Circle and hold with the pen", s.circleToLasso) { v -> store.update { it.copy(circleToLasso = v) } }
    }
}

@Composable
private fun LaserCard() {
    val store = SettingsStore.get(LocalContext.current)
    val s by store.settings.collectAsState()
    Card("Laser pointer") {
        SliderRow("STAYS FOR", "%.0f s".format(s.laserSeconds), s.laserSeconds, 1f..8f) { v -> store.update { it.copy(laserSeconds = v) } }
        Text("Keep the pen or a finger on the screen to hold the laser pointer active; trails fade smoothly after release.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TextCard(vm: EditorViewModel) {
    Card("Text") {
        SliderRow("DEFAULT SIZE", "%.0f".format(vm.textSize), vm.textSize, 16f..160f) { vm.textSize = it }
        Segments(TextFont.entries.take(4).map { it.label }, TextFont.entries.indexOf(vm.textStyle.font).coerceAtMost(3)) { i -> vm.textStyle = vm.textStyle.copy(font = TextFont.entries[i]) }
    }
}

@Composable
private fun ColorCard(title: String, base: List<Int>, current: Int, vm: EditorViewModel, onPick: (Int) -> Unit) {
    Card(title) { Row(verticalAlignment = Alignment.CenterVertically) { CustomSwatches(base, current, vm, onPick) } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MindMapCard(vm: EditorViewModel) {
    val shapes = listOf("pill" to "Pill", "rect" to "Card", "diamond" to "Diamond", "circle" to "Oval")
    Card("Mind map") {
        Text("NODE SHAPE", style = MaterialTheme.typography.labelSmall)
        Segments(shapes.map { it.second }, shapes.indexOfFirst { it.first == vm.mindShape }.coerceAtLeast(0)) { i ->
            vm.mindShape = shapes[i].first
        }
        Text("NODE COLOUR", style = MaterialTheme.typography.labelSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MindPalette.forEach { c ->
                Swatch(c, c == vm.mindColor, 20) { vm.recolorMindNode(color = c) }
            }
        }
        ToggleLine("Move child branch with node", vm.moveMindSubtree) { vm.moveMindSubtree = it }
        Text("LAYOUT", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EditorViewModel.MindLayout.entries.forEach { layout ->
                TextButton(onClick = { vm.autoLayoutMindMap(layout) }, modifier = Modifier.weight(1f)) {
                    Text(layout.label, maxLines = 1)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = { vm.exportMindMapOutlineToPage() }, modifier = Modifier.weight(1f)) {
                Text("Outline → Page", maxLines = 1)
            }
            TextButton(onClick = { vm.flashcardsFromMindMap() }, modifier = Modifier.weight(1f)) {
                Text("→ Flashcards", maxLines = 1)
            }
        }
        TextButton(onClick = { vm.generateMindMapFromNotes() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Generate mind map from notes")
        }
        Text("Tap the canvas to add nodes, drag nodes to arrange, and double-tap a node to edit.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MindNodeDialog(vm: EditorViewModel, edit: EditorViewModel.MindNodeEdit) {
    var label by remember(edit) { mutableStateOf(edit.item.label) }
    var tag by remember(edit) { mutableStateOf(edit.item.tag) }
    var notes by remember(edit) { mutableStateOf(edit.item.notes) }
    var color by remember(edit) { mutableStateOf(edit.item.color) }
    var shape by remember(edit) { mutableStateOf(edit.item.shape) }
    val shapes = listOf("pill" to "Pill", "rect" to "Card", "diamond" to "Diamond", "circle" to "Oval")
    AlertDialog(
        onDismissRequest = { vm.mindNodeEdit = null },
        title = { Text(if (edit.isNew) "New mind map node" else "Edit node") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    label, { label = it },
                    label = { Text("Node title") },
                    placeholder = { Text("Enter concept or branch title") },
                    minLines = 2, maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    tag, { tag = it.take(28) },
                    label = { Text("Tag (optional)") },
                    placeholder = { Text("e.g. Key idea, Exam, Formula") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    notes, { notes = it },
                    label = { Text("Notes / Details") },
                    placeholder = { Text("Summary, example, or sub-detail shown on the card") },
                    minLines = 2, maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Shape", style = MaterialTheme.typography.labelSmall)
                Segments(shapes.map { it.second }, shapes.indexOfFirst { it.first == shape }.coerceAtLeast(0)) { i ->
                    shape = shapes[i].first
                }
                Text("Colour", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MindPalette.forEach { c -> Swatch(c, c == color, 20) { color = c } }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.commitMindNode(label, color, shape, notes, tag) }) { Text("Save") }
        },
        dismissButton = {
            if (!edit.isNew) {
                TextButton(onClick = { vm.deleteMindNode(edit.item.id) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            } else {
                TextButton(onClick = { vm.mindNodeEdit = null }) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun ToggleLine(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(value, onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShapeCard(vm: EditorViewModel) {
    var fillEdit by remember { mutableStateOf(false) }
    Card("Shapes") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShapeKind.entries.forEach { k ->
                val sel = vm.shapeKind == k
                Column(
                    Modifier.size(70.dp).clip(RoundedCornerShape(22.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { vm.shapeKind = k },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    ShapeGlyph(
                        k, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, Modifier.size(28.dp),
                        fill = if (vm.shapeFillOn) Color(vm.shapeFillColor) else null,
                    )
                    Text(k.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        ToggleLine("Dashed line", vm.shapeDashed) { vm.shapeDashed = it }
        ToggleLine("Fill", vm.shapeFillOn) { vm.shapeFillOn = it }
        AnimatedVisibility(vm.shapeFillOn) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf(0xFFCDE2FB, 0xFFCAEAC7, 0xFFFAD6D6, 0xFFF9DCA4, 0xFFDFDBFD, 0x661B1F2A).map { it.toInt() }.forEach { c ->
                        Swatch(c, c == vm.shapeFillColor, 20) { vm.shapeFillColor = c }
                    }
                    AddSwatchButton { fillEdit = true }
                }
                SliderRow("OPACITY", "${((vm.shapeFillColor ushr 24) * 100 / 255)}%", (vm.shapeFillColor ushr 24) / 255f, 0.1f..1f) { a ->
                    vm.shapeFillColor = (vm.shapeFillColor and 0x00FFFFFF) or ((a * 255).toInt() shl 24)
                }
            }
        }
    }
    if (fillEdit) ColorEditorDialog(vm.shapeFillColor, PenPalette, vm.recentColors(), onDismiss = { fillEdit = false }) { c ->
        vm.shapeFillColor = (c and 0x00FFFFFF) or (vm.shapeFillColor and 0xFF000000.toInt()); fillEdit = false
    }
}

@Composable
private fun SliderRow(label: String, value: String, v: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(horizontal = 14.dp, vertical = 6.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelSmall)
        }
        Slider(v, onChange, valueRange = range)
    }
}

// ============================================================== stickers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StickerSheet(vm: EditorViewModel, onAiSticker: () -> Unit = {}, onPick: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var mine by remember { mutableStateOf(StickerLibrary.custom(context)) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            withContext(Dispatchers.IO) { StickerLibrary.import(context, uri) }
            mine = StickerLibrary.custom(context); tab = 2
        }
    }
    fun pick(s: Sticker) = scope.launch { withContext(Dispatchers.Default) { StickerLibrary.bitmap(s) }?.let(onPick) }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Stickers", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(start = 8.dp))
            FilledTonalButton(onClick = onAiSticker, shape = Pill, modifier = Modifier.elasticStretch()) {
                Text("✦ AI Sticker")
            }
            TextButton(onClick = { importer.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Icon(Icons.Outlined.Add, null); Text("Import")
            }
        }
        PrimaryTabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
            Tab(tab == 0, { tab = 0 }, text = { Text("Planner") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Emoji") })
            Tab(tab == 2, { tab = 2 }, text = { Text("My stickers") })
        }
        val list: List<Sticker> = when (tab) {
            0 -> StickerLibrary.builtins
            1 -> StickerSet.map { Sticker.Emoji(it) }
            else -> mine
        }
        if (tab == 2 && mine.isEmpty()) {
            Text("Import a picture to use it as a sticker. PNGs with transparency look best.",
                Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyVerticalGrid(GridCells.Adaptive(if (tab == 0) 96.dp else 64.dp), Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(vertical = 12.dp)) {
            items(list.size) { i ->
                val st = list[i]
                val preview by produceState<Bitmap?>(null, st) { value = withContext(Dispatchers.Default) { StickerLibrary.bitmap(st, 160) } }
                Box(Modifier.padding(4.dp).aspectRatio(1f).clip(RoundedCornerShape(20.dp)).clickable { pick(st) }, contentAlignment = Alignment.Center) {
                    preview?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize().padding(6.dp)) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ============================================================== text formatting

private fun TextFont.compose(): FontFamily = when (this) {
    TextFont.SANS, TextFont.ROUNDED, TextFont.CONDENSED -> FontFamily.SansSerif
    TextFont.SERIF -> FontFamily.Serif
    TextFont.MONO -> FontFamily.Monospace
    TextFont.HAND, TextFont.CURSIVE -> FontFamily.Cursive
}

/** Document-style text editor: font, size, weight, alignment, colour and gradient. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextDialog(vm: EditorViewModel, edit: EditorViewModel.TextEdit) {
    val ex = edit.existing
    var text by remember(edit) { mutableStateOf(ex?.text ?: "") }
    var size by remember(edit) { mutableStateOf(ex?.size ?: vm.textSize) }
    var color by remember(edit) { mutableStateOf(ex?.color ?: vm.textColor ?: vm.penStyle.color) }
    var style by remember(edit) { mutableStateOf(ex?.style ?: vm.textStyle) }
    var colorEdit by remember { mutableStateOf<Int?>(null) }
    var fontMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { vm.textEdit = null },
        title = { Text(if (ex != null) "Edit text" else "Add text") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val previewStyle = TextStyle(
                    fontSize = (size / 2f).coerceIn(12f, 40f).sp,
                    fontFamily = style.font.compose(),
                    fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = if (style.underline) TextDecoration.Underline else null,
                    textAlign = when (style.align) { TextAlign.LEFT -> androidx.compose.ui.text.style.TextAlign.Start; TextAlign.CENTER -> androidx.compose.ui.text.style.TextAlign.Center; TextAlign.RIGHT -> androidx.compose.ui.text.style.TextAlign.End },
                ).let { s -> style.gradient?.let { g -> s.copy(brush = Brush.linearGradient(listOf(Color(color), Color(g)))) } ?: s.copy(color = Color(color)) }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(14.dp)) {
                    Text(text.ifBlank { "Preview" }, style = previewStyle, modifier = Modifier.fillMaxWidth(), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                OutlinedTextField(text, { text = it }, minLines = 3, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        TextButton(onClick = { fontMenu = true }) { Text(style.font.label); Icon(Icons.Filled.KeyboardArrowDown, null) }
                        DropdownMenu(fontMenu, { fontMenu = false }, shape = RoundedCornerShape(16.dp)) {
                            TextFont.entries.forEach { f ->
                                DropdownMenuItem(text = { Text(f.label, fontFamily = f.compose()) }, onClick = { fontMenu = false; style = style.copy(font = f) })
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    FmtToggle(Icons.Outlined.FormatBold, style.bold) { style = style.copy(bold = !style.bold) }
                    FmtToggle(Icons.Outlined.FormatItalic, style.italic) { style = style.copy(italic = !style.italic) }
                    FmtToggle(Icons.Outlined.FormatUnderlined, style.underline) { style = style.copy(underline = !style.underline) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FmtToggle(Icons.AutoMirrored.Outlined.FormatAlignLeft, style.align == TextAlign.LEFT) { style = style.copy(align = TextAlign.LEFT) }
                    FmtToggle(Icons.Outlined.FormatAlignCenter, style.align == TextAlign.CENTER) { style = style.copy(align = TextAlign.CENTER) }
                    FmtToggle(Icons.AutoMirrored.Outlined.FormatAlignRight, style.align == TextAlign.RIGHT) { style = style.copy(align = TextAlign.RIGHT) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        val stamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date())
                        text = if (text.isBlank()) stamp else "$text $stamp"
                    }) { Icon(Icons.Outlined.Schedule, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Date") }
                }
                SliderRow("SIZE", "%.0f".format(size), size, 16f..160f) { size = it }
                Text("Colour", style = MaterialTheme.typography.labelSmall)
                FlowRow {
                    PenPalette.take(12).forEach { c -> Swatch(c, c == color, 20) { color = c } }
                    AddSwatchButton { colorEdit = 0 }
                }
                ToggleLine("Gradient", style.gradient != null) { on -> style = style.copy(gradient = if (on) 0xFF7161E0.toInt() else null) }
                style.gradient?.let { g ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ends in", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 8.dp))
                        listOf(0xFF7161E0, 0xFFD55181, 0xFF3987E5, 0xFF1BAF7A, 0xFFEB6834).map { it.toInt() }.forEach { c ->
                            Swatch(c, c == g, 20) { style = style.copy(gradient = c) }
                        }
                        AddSwatchButton { colorEdit = 1 }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.commitText(text, size, color, style) }) { Text("Done") } },
        dismissButton = { TextButton(onClick = { vm.textEdit = null }) { Text("Cancel") } },
    )
    colorEdit?.let { which ->
        ColorEditorDialog(if (which == 0) color else style.gradient ?: color, PenPalette, vm.recentColors(), onDismiss = { colorEdit = null }) { c ->
            if (which == 0) color = c else style = style.copy(gradient = c)
            vm.pushRecent(c); colorEdit = null
        }
    }
}

@Composable
private fun FmtToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.padding(2.dp).size(38.dp).clip(Pill)
            .background(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
}

// ============================================================== small parts

@Composable
private fun SelectionMenu(
    vm: EditorViewModel,
    backdrop: LayerBackdrop? = null,
    liquidGlass: Boolean = false,
    onDuplicate: () -> Unit,
    onColor: (Int) -> Unit,
    keep: (() -> Unit) -> Unit,
    onShareImage: () -> Unit,
) {
    var colors by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }
    val fg = if (liquidGlass) MaterialTheme.colorScheme.onSurface else Color.White
    OptionalLiquidGlassBox(
        backdrop = backdrop,
        enabled = liquidGlass,
        shape = Pill,
        fallbackColor = Color(0xE01B1F2A),
        modifier = Modifier.shadow(8.dp, Pill),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides fg) {
            Row(Modifier.padding(horizontal = 6.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                // ✨ Explain with AI button right inside the Lasso selection menu!
                Surface(
                    shape = Pill,
                    color = Color.Transparent,
                    contentColor = Color.White,
                    modifier = Modifier
                        .padding(4.dp)
                        .elasticStretch()
                        .clip(Pill)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF4285F4), Color(0xFF9B72CB), Color(0xFFD96570)),
                            ),
                        )
                        .clickable { vm.explainLassoSelection() },
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(17.dp), tint = Color.White)
                        Spacer(Modifier.width(5.dp))
                        Text("Explain", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                // Link button
                Surface(
                    shape = Pill, color = Color(0xFF3D6CF4), contentColor = Color.White,
                    modifier = Modifier.padding(4.dp).elasticStretch().clip(Pill).clickable { vm.startLinkFromSelection() },
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Link, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Link", style = MaterialTheme.typography.labelLarge)
                    }
                }
                // Linked content: a red Unlink right next to it.
                if (vm.selectionLinks().isNotEmpty()) Surface(
                    shape = Pill, color = Color(0xFFE5484D), contentColor = Color.White,
                    modifier = Modifier.padding(4.dp).elasticStretch().clip(Pill).clickable { vm.unlinkSelection() },
                ) { Text("Unlink", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge) }
                IconButton(onClick = vm::cutSelection, modifier = Modifier.elasticStretch()) { Icon(Icons.Outlined.ContentCut, "Cut") }
                IconButton(onClick = vm::copySelection, modifier = Modifier.elasticStretch()) { Icon(Icons.Outlined.ContentCopy, "Copy") }
                IconButton(onClick = onDuplicate, modifier = Modifier.elasticStretch()) { ProIconView(ProIcon.ADD, fg, Modifier.size(22.dp)) }
                IconButton(onClick = { colors = true }, modifier = Modifier.elasticStretch()) { Icon(Icons.Outlined.Palette, "Colour") }
                IconButton(onClick = { keep { vm.rotateSelection(90f) } }, modifier = Modifier.elasticStretch()) { Icon(Icons.Outlined.RotateRight, "Rotate 90°") }
                Box {
                    IconButton(onClick = { more = true }, modifier = Modifier.elasticStretch()) { ProIconView(ProIcon.MORE, fg, Modifier.size(22.dp)) }
                    DropdownMenu(more, { more = false }, shape = RoundedCornerShape(18.dp)) {
                        DropdownMenuItem(text = { Text("✨ Explain selection with AI") }, leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = { more = false; vm.explainLassoSelection() })
                        DropdownMenuItem(text = { Text("Flip horizontally") }, leadingIcon = { Icon(Icons.Outlined.Flip, null) },
                            onClick = { more = false; keep { vm.flipSelection() } })
                        DropdownMenuItem(text = { Text("Bring to front") }, leadingIcon = { Icon(Icons.Outlined.FlipToFront, null) },
                            onClick = { more = false; keep { vm.arrange(true) } })
                        DropdownMenuItem(text = { Text("Send to back") }, leadingIcon = { Icon(Icons.Outlined.FlipToBack, null) },
                            onClick = { more = false; keep { vm.arrange(false) } })
                        DropdownMenuItem(text = { Text("Share as image") }, leadingIcon = { Icon(Icons.Outlined.IosShare, null) },
                            onClick = { more = false; onShareImage() })
                        DropdownMenuItem(text = { Text("Make flashcard") }, leadingIcon = { Icon(Icons.Outlined.Style, null) },
                            onClick = { more = false; vm.cardFromSelection() })
                        DropdownMenuItem(text = { Text("Add link…") }, leadingIcon = { Icon(Icons.Outlined.Link, null) },
                            onClick = { more = false; vm.startLinkFromSelection() })
                        DropdownMenuItem(text = { Text("Save as screenshot") }, leadingIcon = { Icon(Icons.Outlined.Image, null) },
                            onClick = { more = false; vm.saveSelectionScreenshot() })
                    }
                }
                IconButton(onClick = vm::deleteSelection, modifier = Modifier.elasticStretch()) { Icon(Icons.Outlined.Delete, "Delete", tint = Color(0xFFFF8A80)) }
            }
        }
    }
    if (colors) {
        ColorEditorDialog(0xFF1B1F2A.toInt(), PenPalette, vm.recentColors(), onDismiss = { colors = false }) { onColor(it); colors = false }
    }
}

// ============================================================== study timer

/**
 * Stopwatch for this study session. Runs while the notebook is open and the app is in front.
 * Tap to pause or resume, long-press for totals, drag to move it anywhere (the spot is remembered).
 */
@Composable
private fun StudyTimer(
    vm: EditorViewModel,
    settings: AppSettings,
    area: IntSize,
    backdrop: LayerBackdrop? = null,
) {
    var running by remember { mutableStateOf(settings.studyAutoStart) }
    var session by remember { mutableStateOf(0L) }
    var unsaved by remember { mutableStateOf(0L) }
    var pomoMode by remember { mutableStateOf(false) }
    var pomoRemMs by remember(settings.pomodoroMinutes) { mutableStateOf(settings.pomodoroMinutes.coerceIn(5, 90) * 60_000L) }
    var menu by remember { mutableStateOf(false) }
    var resumed by remember { mutableStateOf(true) }
    var idle by remember { mutableStateOf(false) }
    var pos by remember { mutableStateOf(vm.timerPos()) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    var pill by remember { mutableStateOf(IntSize.Zero) }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            resumed = owner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
            if (e == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) { vm.addStudy(unsaved); unsaved = 0L }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs); vm.addStudy(unsaved); unsaved = 0L }
    }
    LaunchedEffect(running, resumed, pomoMode) {
        while (running && resumed) {
            kotlinx.coroutines.delay(1000)
            val idleMs = settings.studyIdleMinutes * 60_000L
            idle = idleMs > 0 && System.currentTimeMillis() - vm.lastActivity > idleMs
            if (idle) continue
            session += 1000; unsaved += 1000
            if (pomoMode) {
                pomoRemMs = (pomoRemMs - 1000L).coerceAtLeast(0L)
                if (pomoRemMs <= 0L) {
                    running = false
                    pomoRemMs = settings.pomodoroMinutes.coerceIn(5, 90) * 60_000L
                }
            }
            if (unsaved >= 30_000) { vm.addStudy(unsaved); unsaved = 0L }
        }
    }
    fun fmt(ms: Long, secs: Boolean = settings.studySeconds): String {
        val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return when {
            !secs -> if (h > 0) "%d h %02d m".format(h, m) else "%d min".format(m)
            h > 0 -> "%d:%02d:%02d".format(h, m, sec)
            else -> "%02d:%02d".format(m, sec)
        }
    }
    if (area.width == 0) return
    val x = (pos.first * area.width - pill.width / 2f + drag.x).coerceIn(8f, (area.width - pill.width - 8f).coerceAtLeast(8f))
    val y = (pos.second * area.height - pill.height / 2f + drag.y).coerceIn(8f, (area.height - pill.height - 8f).coerceAtLeast(8f))
    Box(
        Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .onSizeChanged { pill = it }
            .pointerInput(area) {
                detectDragGestures(
                    onDragEnd = {
                        val nx = ((x + pill.width / 2f) / area.width).coerceIn(0f, 1f)
                        val ny = ((y + pill.height / 2f) / area.height).coerceIn(0f, 1f)
                        pos = nx to ny; drag = Offset.Zero
                        vm.setTimerPos(nx, ny)
                    },
                    onDragCancel = { drag = Offset.Zero },
                ) { change, amount -> change.consume(); drag += amount }
            },
    ) {
        OptionalLiquidGlassBox(
            backdrop = backdrop,
            enabled = settings.liquidGlass,
            shape = Pill,
            fallbackColor = glassColor(),
            modifier = Modifier.shadow(4.dp, Pill).elasticStretch().combinedClickableCompat({ running = !running }, { menu = true }),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Timer, if (running) "Pause study timer" else "Start study timer", Modifier.size(16.dp),
                    tint = if (running && !idle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (pomoMode) "🍅 ${fmt(pomoRemMs, true)}" else fmt(session),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                )
                val state = when { !running && session == 0L && !pomoMode -> "tap to start"; !running -> "paused"; idle -> "idle"; else -> null }
                if (state != null) { Spacer(Modifier.width(6.dp)); Text(state, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        DropdownMenu(menu, { menu = false }, shape = RoundedCornerShape(18.dp)) {
            val total = vm.studyTotal() + unsaved
            DropdownMenuItem(text = { Text("This session: ${fmt(session, true)}") }, onClick = { menu = false }, enabled = false)
            DropdownMenuItem(text = { Text("This notebook: ${fmt(total, true)}") }, onClick = { menu = false }, enabled = false)
            DropdownMenuItem(text = { Text(if (running) "Pause" else "Start") }, onClick = { menu = false; running = !running })
            DropdownMenuItem(
                text = { Text(if (pomoMode) "Switch to Stopwatch" else "Pomodoro (${settings.pomodoroMinutes}m)") },
                onClick = {
                    menu = false
                    pomoMode = !pomoMode
                    pomoRemMs = settings.pomodoroMinutes.coerceIn(5, 90) * 60_000L
                    running = true
                },
            )
            DropdownMenuItem(text = { Text("Restart session") }, onClick = { menu = false; vm.addStudy(unsaved); unsaved = 0L; session = 0L; pomoRemMs = settings.pomodoroMinutes.coerceIn(5, 90) * 60_000L; running = true })
            DropdownMenuItem(text = { Text("Move back to the bottom") }, onClick = { menu = false; pos = 0.5f to 0.93f; vm.setTimerPos(0.5f, 0.93f) })
        }
    }
}

// ============================================================== tool wheel

/** Radial tool picker that pops up where the pen hovers (pen button click). */
@Composable
private fun ToolWheel(vm: EditorViewModel, at: Offset, onDismiss: () -> Unit) {
    val density = LocalDensity.current.density
    val tools = listOf(Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.LASSO, Tool.SHAPE, Tool.MINDMAP, Tool.TEXT, Tool.LASER, Tool.TAPE)
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.62f, stiffness = 420f)) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(
        Modifier.fillMaxSize().onSizeChanged { size = it }
            .background(Color.Black.copy(alpha = 0.12f * appear.value))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        val radius = 92f * density
        // Keep the wheel on screen.
        val cx = at.x.coerceIn(radius + 40 * density, (size.width - radius - 40 * density).coerceAtLeast(radius + 40 * density))
        val cy = at.y.coerceIn(radius + 40 * density, (size.height - radius - 40 * density).coerceAtLeast(radius + 40 * density))
        Box(
            Modifier.offset { IntOffset((cx - 34 * density).roundToInt(), (cy - 34 * density).roundToInt()) }
                .size(68.dp).graphicsLayer { scaleX = appear.value; scaleY = appear.value }
                .clip(CircleShape).background(glassColor()).glassEdge(CircleShape),
            contentAlignment = Alignment.Center,
        ) { ProIconView(ProIcon.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(18.dp)) }
        tools.forEachIndexed { i, t ->
            val a = -Math.PI / 2 + i * 2 * Math.PI / tools.size
            val r = radius * appear.value
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy + (r * kotlin.math.sin(a)).toFloat()
            val sel = vm.tool == t
            Box(
                Modifier.offset { IntOffset((x - 26 * density).roundToInt(), (y - 26 * density).roundToInt()) }
                    .size(52.dp).shadow(6.dp, CircleShape).clip(CircleShape)
                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .clickable { vm.tool = t; onDismiss() },
                contentAlignment = Alignment.Center,
            ) {
                ToolGlyph(t, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    Color(vm.styleFor(t)?.color ?: vm.penStyle.color), Modifier.size(24.dp), active = sel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WebLinkDialog(vm: EditorViewModel, edit: EditorViewModel.WebLinkEdit) {
    var url by remember(edit) { mutableStateOf(edit.item.url) }
    var linkTitle by remember(edit) { mutableStateOf(edit.item.title) }
    var note by remember(edit) { mutableStateOf(edit.item.note) }
    var color by remember(edit) { mutableStateOf(edit.item.color) }
    AlertDialog(
        onDismissRequest = { vm.webLinkEdit = null },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProIconView(ProIcon.WEBLINK, Color(color), Modifier.size(22.dp), ink = Color(color).copy(alpha = 0.25f))
                Spacer(Modifier.width(8.dp))
                Text(if (edit.isNew) "Add web link" else "Web link card", Modifier.weight(1f))
                if (!edit.isNew && url.isNotBlank()) {
                    TextButton(onClick = { vm.openExternalUrl(url) }) {
                        Text("Open URL")
                    }
                }
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text("Website URL") },
                    placeholder = { Text("https://en.wikipedia.org/wiki/...") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = linkTitle,
                    onValueChange = { linkTitle = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text("Title (optional)") },
                    placeholder = { Text("Auto-uses domain if empty") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text("Reference note (optional)") },
                    placeholder = { Text("Why this link matters for this chapter…") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("ACCENT COLOUR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WebLinkColors.forEach { c -> Swatch(c, c == color, 20) { color = c } }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = url.trim().isNotBlank(),
                onClick = { vm.commitWebLink(url, linkTitle, note, color) },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { if (edit.isNew) vm.webLinkEdit = null else vm.deleteWebLink() }) {
                Text(if (edit.isNew) "Cancel" else "Delete", color = if (edit.isNew) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
            }
        },
    )
}

/**
 * v10 Writable Split View:
 * - Clean single header bar with Read/Write mode toggle, Undo, and organized ⋯ menu
 * - Compact ink/highlighter/eraser strip when writing in the split pane
 * - Fixed zoom-out floor (35%–300%) with centered framing and presets (50%, Fit, 100%, 150%)
 * - Honors scrollDirection setting, Sync Page lock, Search, Swap Panes, Clip Sticky / To Page / + Link
 */
@Composable
private fun SplitPanel(
    noteId: String,
    mainPageIndex: Int,
    scrollDirection: ScrollDirection,
    others: List<Pair<String, String>>,
    onPick: (String) -> Unit,
    onOpen: (String) -> Unit,
    onSwap: () -> Unit,
    onRatioPreset: (Float) -> Unit,
    onClipToSticky: (String, String) -> Unit,
    onInsertToPage: (String, String) -> Unit,
    onAddWebLink: (String, String, String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    val app = LocalContext.current.applicationContext as Application
    val vm2: EditorViewModel = viewModel(key = "split-$noteId", factory = viewModelFactory { initializer { EditorViewModel(app, noteId) } })
    var pick by remember { mutableStateOf(false) }
    var splitMenu by remember { mutableStateOf(false) }
    var splitZoom by remember(noteId) { mutableFloatStateOf(1f) }
    var splitPan by remember(noteId) { mutableStateOf(Offset.Zero) }
    var showSplitSearch by remember(noteId) { mutableStateOf(false) }
    var syncPage by remember(noteId) { mutableStateOf(false) }
    var writeMode by remember(noteId) { mutableStateOf(false) }
    var inkKind by remember(noteId) { mutableStateOf("pen") } // "pen", "highlighter", "eraser"
    var inkColor by remember(noteId) { mutableIntStateOf(0xFF1B1F2A.toInt()) }

    val listState = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()
    val visiblePage by remember(listState, vm2.pages.size) {
        androidx.compose.runtime.derivedStateOf { listState.firstVisibleItemIndex.coerceIn(0, (vm2.pages.size - 1).coerceAtLeast(0)) }
    }

    LaunchedEffect(splitZoom) {
        if (splitZoom <= 1.0f) splitPan = Offset.Zero
    }
    LaunchedEffect(mainPageIndex, syncPage, vm2.pages.size) {
        if (syncPage && vm2.pages.isNotEmpty()) {
            val target = mainPageIndex.coerceIn(0, vm2.pages.lastIndex)
            listState.animateScrollToItem(target)
        }
    }
    LaunchedEffect(vm2.activeHit, vm2.searchHits) {
        val hit = vm2.searchHits.getOrNull(vm2.activeHit) ?: return@LaunchedEffect
        listState.animateScrollToItem(hit.page.coerceIn(0, (vm2.pages.size - 1).coerceAtLeast(0)))
    }

    fun extractPageText(pageIdx: Int): String = vm2.extractPageSummaryText(pageIdx)

    Column(modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        // Clean One UI 9 inspired Split View Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.weight(1f)) {
                Surface(
                    shape = Pill,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.clip(Pill).clickable { pick = true }.elasticStretch(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Outlined.Book, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(vm2.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(16.dp))
                    }
                }
                DropdownMenu(pick, { pick = false }, shape = RoundedCornerShape(18.dp)) {
                    others.forEach { (id, t) -> DropdownMenuItem(text = { Text(t) }, onClick = { pick = false; onPick(id) }) }
                }
            }

            // Quick Ratio & Swap Pill
            Row(
                Modifier.clip(Pill).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onSwap, modifier = Modifier.size(28.dp).elasticStretch()) {
                    Icon(Icons.Outlined.SwapHoriz, "Swap Panes", Modifier.size(16.dp))
                }
                TextButton(onClick = { onRatioPreset(1f) }, contentPadding = PaddingValues(horizontal = 6.dp), modifier = Modifier.height(28.dp)) {
                    Text("1:1", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { onRatioPreset(0.43f) }, contentPadding = PaddingValues(horizontal = 6.dp), modifier = Modifier.height(28.dp)) {
                    Text("70:30", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Write / Read Mode Toggle
            Surface(
                shape = Pill,
                color = if (writeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (writeMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clip(Pill).clickable { writeMode = !writeMode }.elasticStretch(),
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (writeMode) Icons.Outlined.Brush else Icons.Outlined.MenuBook,
                        null,
                        Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (writeMode) "Write" else "Read",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (writeMode) {
                IconButton(
                    enabled = vm2.canUndo,
                    onClick = vm2::undo,
                    modifier = Modifier.size(32.dp).elasticStretch(),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Undo, "Undo", Modifier.size(17.dp))
                }
            }

            Box {
                IconButton(onClick = { splitMenu = true }, modifier = Modifier.size(32.dp).elasticStretch()) {
                    Icon(Icons.Outlined.MoreVert, "Split Options", Modifier.size(18.dp))
                }
                DropdownMenu(splitMenu, { splitMenu = false }, shape = RoundedCornerShape(18.dp)) {
                    DropdownMenuItem(
                        text = { Text(if (showSplitSearch) "Hide Search" else "Search in Note") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(18.dp)) },
                        onClick = {
                            splitMenu = false
                            showSplitSearch = !showSplitSearch
                            if (!showSplitSearch) vm2.clearSearch()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Insert Page Text to Note") },
                        leadingIcon = { Icon(Icons.Outlined.PostAdd, null, Modifier.size(18.dp)) },
                        onClick = {
                            splitMenu = false
                            val extracted = extractPageText(visiblePage)
                            onInsertToPage(extracted.ifBlank { "Notes from ${vm2.title} (Page ${visiblePage + 1})" }, vm2.title)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Clip Page as Sticky") },
                        leadingIcon = { Icon(Icons.Outlined.PushPin, null, Modifier.size(18.dp)) },
                        onClick = {
                            splitMenu = false
                            val extracted = extractPageText(visiblePage)
                            onClipToSticky(extracted.ifBlank { "Reference from ${vm2.title} (Page ${visiblePage + 1})" }, vm2.title)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (syncPage) "Sync Page Scrolling: On" else "Sync Page Scrolling: Off") },
                        leadingIcon = { Icon(Icons.Outlined.Sync, null, Modifier.size(18.dp)) },
                        onClick = { splitMenu = false; syncPage = !syncPage },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Open Fullscreen") },
                        leadingIcon = { Icon(Icons.Outlined.Fullscreen, null, Modifier.size(18.dp)) },
                        onClick = { splitMenu = false; onOpen(noteId) },
                    )
                    DropdownMenuItem(
                        text = { Text("Close Split View") },
                        leadingIcon = { Icon(Icons.Filled.Close, null, Modifier.size(18.dp)) },
                        onClick = { splitMenu = false; onClose() },
                    )
                }
            }
        }

        // Compact write-mode ink strip (only visible when Write mode is active)
        AnimatedVisibility(writeMode) {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)
                    .horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("pen" to "Pen", "highlighter" to "Highlight", "eraser" to "Eraser").forEach { (k, label) ->
                    val active = inkKind == k
                    Surface(
                        shape = Pill,
                        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.elasticStretch().clip(Pill).clickable { inkKind = k },
                    ) {
                        Text(
                            label,
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                if (inkKind != "eraser") {
                    Box(Modifier.width(1.dp).height(16.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    listOf(0xFF1B1F2A, 0xFF3D6CF4, 0xFFEF4444, 0xFF10B981, 0xFFFFE066).forEach { c ->
                        val ci = c.toInt()
                        Swatch(ci, inkColor == ci, 16) { inkColor = ci }
                    }
                }
            }
        }

        AnimatedVisibility(showSplitSearch) {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = vm2.searchQuery,
                    onValueChange = vm2::search,
                    singleLine = true,
                    placeholder = { Text("Search inside ${vm2.title}…", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    shape = CircleShape,
                    modifier = Modifier.weight(1f),
                )
                if (vm2.searchHits.isNotEmpty()) {
                    Text(
                        "${vm2.activeHit + 1}/${vm2.searchHits.size}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                    IconButton(onClick = { vm2.stepHit(-1) }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Previous match", Modifier.size(16.dp))
                    }
                    IconButton(onClick = { vm2.stepHit(1) }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.KeyboardArrowDown, "Next match", Modifier.size(16.dp))
                    }
                }
            }
        }

        if (vm2.pages.isNotEmpty()) {
            // Ultra-clean compact page & zoom footer/pill strip
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(
                    enabled = visiblePage > 0,
                    onClick = { scope.launch { listState.animateScrollToItem((visiblePage - 1).coerceAtLeast(0)) } },
                    modifier = Modifier.size(26.dp),
                ) { Icon(Icons.Filled.KeyboardArrowUp, "Previous", Modifier.size(16.dp).graphicsLayer { rotationZ = -90f }) }
                Text("p.${visiblePage + 1}/${vm2.pages.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(
                    enabled = visiblePage < vm2.pages.size - 1,
                    onClick = { scope.launch { listState.animateScrollToItem((visiblePage + 1).coerceAtMost(vm2.pages.size - 1)) } },
                    modifier = Modifier.size(26.dp),
                ) { Icon(Icons.Filled.KeyboardArrowUp, "Next", Modifier.size(16.dp).graphicsLayer { rotationZ = 90f }) }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { splitZoom = (splitZoom - 0.15f).coerceIn(0.35f, 3.0f) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(26.dp),
                ) { Text("-", style = MaterialTheme.typography.titleSmall) }
                Text(
                    "${(splitZoom * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clickable { splitZoom = 1f; splitPan = Offset.Zero },
                )
                TextButton(
                    onClick = { splitZoom = (splitZoom + 0.15f).coerceIn(0.35f, 3.0f) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(26.dp),
                ) { Text("+", style = MaterialTheme.typography.titleSmall) }
            }
        }

        // Smooth zoom (0.35f..3.0f centered when zoomed out) & optional writable ink overlay
        val renderW = if (splitZoom > 1.35f) 1400 else 860
        BoxWithConstraints(
            Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val viewW = with(LocalDensity.current) { maxWidth.toPx() }.coerceAtLeast(120f)
            val viewH = with(LocalDensity.current) { maxHeight.toPx() }.coerceAtLeast(120f)
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(viewW, viewH, scrollDirection, writeMode) {
                        awaitEachGesture {
                            var pinching = false
                            while (true) {
                                val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                val downCount = event.changes.count { it.pressed }
                                if (downCount == 0) break
                                if (downCount >= 2) {
                                    val z = event.calculateZoom()
                                    val p = event.calculatePan()
                                    if (z != 1f || p != Offset.Zero) {
                                        val nextZoom = (splitZoom * z).coerceIn(0.35f, 3.0f)
                                        val maxPanX = ((nextZoom - 1f).coerceAtLeast(0f) * viewW * 0.55f)
                                        val maxPanY = ((nextZoom - 1f).coerceAtLeast(0f) * viewH * 0.50f)
                                        splitZoom = nextZoom
                                        splitPan = if (nextZoom <= 1.0f) Offset.Zero else Offset(
                                            (splitPan.x + p.x).coerceIn(-maxPanX, maxPanX),
                                            (splitPan.y + p.y).coerceIn(-maxPanY, maxPanY),
                                        )
                                        event.changes.forEach { it.consume() }
                                        pinching = true
                                    }
                                } else if (splitZoom > 1.03f && !writeMode) {
                                    val p = event.calculatePan()
                                    val allowSinglePan = if (scrollDirection == ScrollDirection.HORIZONTAL) {
                                        pinching || kotlin.math.abs(p.y) > kotlin.math.abs(p.x) * 1.15f
                                    } else {
                                        pinching || kotlin.math.abs(p.x) > kotlin.math.abs(p.y) * 1.15f
                                    }
                                    if (allowSinglePan && p != Offset.Zero) {
                                        val maxPanX = (splitZoom - 1f) * viewW * 0.55f
                                        val maxPanY = (splitZoom - 1f) * viewH * 0.50f
                                        splitPan = Offset(
                                            (splitPan.x + p.x).coerceIn(-maxPanX, maxPanX),
                                            (splitPan.y + p.y).coerceIn(-maxPanY, maxPanY),
                                        )
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(writeMode) {
                        if (!writeMode) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (kotlin.math.abs(splitZoom - 1f) > 0.08f) {
                                        splitZoom = 1f
                                        splitPan = Offset.Zero
                                    } else {
                                        splitZoom = 1.65f
                                    }
                                },
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val transformMod = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = splitZoom
                        scaleY = splitZoom
                        translationX = splitPan.x
                        translationY = splitPan.y
                    }

                @Composable
                fun SplitPageCard(i: Int, page: Page, cardModifier: Modifier) {
                    val bmp by produceState<Bitmap?>(null, i, page, vm2.pages.size, renderW) { value = vm2.thumbnail(i, renderW) }
                    var pageBoxSize by remember { mutableStateOf(IntSize.Zero) }
                    var livePts by remember(i) { mutableStateOf<List<Offset>>(emptyList()) }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = 2.dp,
                        color = Color.White,
                        modifier = cardModifier.onSizeChanged { pageBoxSize = it },
                    ) {
                        Box(Modifier.fillMaxWidth()) {
                            bmp?.let { Image(it.asImageBitmap(), "Page ${i + 1}", Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) }
                                ?: Box(Modifier.fillMaxWidth().aspectRatio((page.width / page.height).coerceIn(0.45f, 1.8f)))

                            if (writeMode) {
                                Box(
                                    Modifier.matchParentSize().pointerInput(i, pageBoxSize, inkKind, inkColor) {
                                        detectDragGestures(
                                            onDragStart = { start -> livePts = listOf(start) },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                livePts = livePts + change.position
                                            },
                                            onDragEnd = {
                                                val bw = pageBoxSize.width.toFloat().coerceAtLeast(1f)
                                                val bh = pageBoxSize.height.toFloat().coerceAtLeast(1f)
                                                val pagePts = livePts.map { pt ->
                                                    (pt.x / bw * page.width) to (pt.y / bh * page.height)
                                                }
                                                livePts = emptyList()
                                                if (pagePts.size >= 2) {
                                                    if (inkKind == "eraser") {
                                                        val hitIds = page.items.filterIsInstance<InkItem>().filter { item ->
                                                            pagePts.any { (px, py) ->
                                                                val b = item.bounds
                                                                px >= b.left - 24f && px <= b.right + 24f && py >= b.top - 24f && py <= b.bottom + 24f
                                                            }
                                                        }.map { it.id }.toSet()
                                                        if (hitIds.isNotEmpty()) {
                                                            vm2.beginEdit(i)
                                                            vm2.applySplitItems(i, page.items.filterNot { it.id in hitIds })
                                                        }
                                                    } else {
                                                        val brush = if (inkKind == "highlighter") {
                                                            Brushes.create(Brushes.HIGHLIGHTER, inkColor, 26f)
                                                        } else {
                                                            Brushes.create(Brushes.PEN, inkColor, 4.5f)
                                                        }
                                                        val stroke = InkCodec.strokeFromPoints(pagePts, brush)
                                                        vm2.beginEdit(i)
                                                        vm2.applySplitItems(i, page.items + InkItem(ItemIds.next(), stroke))
                                                    }
                                                }
                                            },
                                            onDragCancel = { livePts = emptyList() },
                                        )
                                    },
                                )
                                if (livePts.size > 1) {
                                    androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                                        val path = Path().apply {
                                            moveTo(livePts.first().x, livePts.first().y)
                                            for (k in 1 until livePts.size) lineTo(livePts[k].x, livePts[k].y)
                                        }
                                        val col = if (inkKind == "eraser") Color(0x88EF4444) else if (inkKind == "highlighter") Color(inkColor).copy(alpha = 0.42f) else Color(inkColor)
                                        val w = if (inkKind == "highlighter") 18f else if (inkKind == "eraser") 12f else 4f
                                        drawPath(path, col, style = Stroke(width = w, cap = StrokeCap.Round))
                                    }
                                }
                            }
                        }
                    }
                }

                if (scrollDirection == ScrollDirection.HORIZONTAL) {
                    LazyRow(
                        state = listState,
                        flingBehavior = snapBehavior,
                        modifier = transformMod,
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(vm2.pages.size) { i ->
                            SplitPageCard(i, vm2.pages[i], Modifier.fillParentMaxWidth(0.95f))
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = transformMod,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        items(vm2.pages.size) { i ->
                            SplitPageCard(i, vm2.pages[i], Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}
