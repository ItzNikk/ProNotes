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
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Brush
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
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pronotes.app.data.AppSettings
import com.pronotes.app.data.EraserMode
import com.pronotes.app.data.HighlighterMode
import com.pronotes.app.data.LassoFilter
import com.pronotes.app.data.LassoMode
import com.pronotes.app.data.MotionStyle
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.PenAction
import com.pronotes.app.data.PenPreset
import com.pronotes.app.data.Repository
import com.pronotes.app.data.ScribbleSensitivity
import com.pronotes.app.data.ScrollDirection
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.ToolbarDock
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppContextMenu
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.AppDialogSurface
import com.pronotes.app.ui.AppMenuButton
import com.pronotes.app.ui.AppMenuItem
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.AppSegmentedControl
import com.pronotes.app.ui.AppSheet
import com.pronotes.app.ui.AppSliderRow
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.AppToggle
import com.pronotes.app.ui.ColorEditorDialog
import com.pronotes.app.ui.LocalMotionStyle
import com.pronotes.app.ui.PenGlyph
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import com.pronotes.app.ui.ScaleInAnimatedText
import com.pronotes.app.ui.ShapeGlyph
import com.pronotes.app.ui.StickerSet
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.Swatch
import com.pronotes.app.ui.ToolGlyph
import com.pronotes.app.ui.appCardShape
import com.pronotes.app.ui.appPillShape
import com.pronotes.app.ui.elasticStretch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val Pill = CircleShape

// SVG-aligned Obsidian Dark & Paper Light workspace tokens (matching editor.svg & editor-pen-popover.svg)
private val TabStripDarkBg = Color(0xFF1F3162)
private val TabStripLightBg = Color(0xFFE3EAF4)
private val TopBarDarkBg = Color(0xFF27507F)
private val TopBarLightBg = Color(0xFFD7E4F8)
private val WorkspaceDarkBg = Color(0xFF1C1C1C)
private val WorkspaceLightBg = Color(0xFFF3F1EA)
private val DockDarkBg = Color(0xFF1A1A1A)
private val DockLightBg = Color(0xFFFFFFFF)
private val TealActiveTile = Color(0xFF2F7F93)
private val PopoverDarkBg = Color(0xFF1C1C1C)
private val PopoverLightBg = Color(0xFFFAF8F3)

/** Shares a bitmap as a PNG through the system share sheet. */
private suspend fun shareBitmap(context: Context, bmp: Bitmap, name: String) {
    val file = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        File(dir, name.replace(Regex("[^A-Za-z0-9 _-]"), "_") + ".png").also { f ->
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, "Share image"))
}

@Composable
fun EditorScreen(
    noteId: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwitchNote: (String) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: EditorViewModel = viewModel(
        key = noteId,
        factory = viewModelFactory { initializer { EditorViewModel(app, noteId) } },
    )
    val settings by vm.settingsFlow.collectAsStateWithLifecycle()
    val store = remember { SettingsStore.get(context) }
    val repo = remember { Repository.get(context) }
    val library by repo.library.collectAsStateWithLifecycle()
    val tabs by store.tabs.collectAsStateWithLifecycle()
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
    var showTemplates by remember { mutableStateOf(false) }
    var showCards by remember { mutableStateOf(false) }
    var showCollabRoom by remember { mutableStateOf(false) }
    var kindleMode by remember { mutableStateOf(false) }
    var activePopoverTool by remember { mutableStateOf<Tool?>(null) }
    var splitId by remember { mutableStateOf<String?>(null) }
    var splitWeight by remember { mutableFloatStateOf(0.8f) }

    val pickBackground = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setPageBackground(uri)
    }
    val importFilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.importFilesIntoCurrent(uris)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.insertImage(uri) { page, id -> canvas?.selectItems(page, setOf(id)) }
        vm.tool = Tool.LASSO
    }
    val exportPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch { context.contentResolver.openOutputStream(uri)?.use { vm.exportPdf(it) } }
    }

    LaunchedEffect(noteId) { store.openTab(noteId) }
    LaunchedEffect(library) {
        store.retainTabs(library.notebooks.filter { it.deletedAt == null }.map { it.id }.toSet())
    }
    LaunchedEffect(activePopoverTool) { vm.cardOpen = activePopoverTool != null }

    fun sharePdf() = scope.launch {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, vm.title.replace(Regex("[^A-Za-z0-9 _-]"), "_") + ".pdf")
        file.outputStream().use { vm.exportPdf(it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Share PDF"))
    }

    BackHandler {
        when {
            kindleMode -> kindleMode = false
            activePopoverTool != null -> activePopoverTool = null
            focus -> focus = false
            showPages -> showPages = false
            else -> {
                vm.saveNow()
                onBack()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            vm.saveNow()
            vm.onPagesChanged = null
            vm.onScrollTo = null
            vm.onRedraw = null
        }
    }

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
    val darkUi = MaterialTheme.colorScheme.background.luminance() < 0.4f
    LaunchedEffect(darkUi, canvas) { canvas?.darkUi = darkUi }
    DisposableEffect(settings.keepScreenOn) {
        val window = (context as? android.app.Activity)?.window
        if (settings.keepScreenOn) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    fun switchTo(id: String) {
        if (id != noteId) {
            vm.saveNow()
            onSwitchNote(id)
        }
    }

    // Android 15+: optionally hide tools during screen recording.
    var recording by remember { mutableStateOf(false) }
    DisposableEffect(settings.hideToolsWhenRecording) {
        val activity = context as? android.app.Activity
        var unregister: (() -> Unit)? = null
        if (settings.hideToolsWhenRecording && activity != null && android.os.Build.VERSION.SDK_INT >= 35) {
            runCatching {
                val wm = activity.windowManager
                val cb = java.util.function.Consumer<Int> { state ->
                    recording = state == android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE
                }
                val now = wm.addScreenRecordingCallback(activity.mainExecutor, cb)
                recording = now == android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE
                unregister = { runCatching { wm.removeScreenRecordingCallback(cb) } }
            }
        } else {
            recording = false
        }
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
    LaunchedEffect(vm.moveMindSubtree, canvas) { canvas?.moveMindSubtree = vm.moveMindSubtree }

    val workspaceBg = if (darkUi) WorkspaceDarkBg else WorkspaceLightBg
    val tabStripBg = if (darkUi) TabStripDarkBg else TabStripLightBg
    val topBarBg = if (darkUi) TopBarDarkBg else TopBarLightBg
    val topBarFg = if (darkUi) Color.White else Color(0xFF16233B)
    val resolvedTabs = remember(tabs, library.notebooks, noteId, vm.title) {
        val list = tabs.mapNotNull { id ->
            library.notebooks.firstOrNull { it.id == id && it.deletedAt == null }?.let { id to it.title }
        }
        if (list.any { it.first == noteId }) list else list + (noteId to vm.title)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(workspaceBg)
            .imePadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = !focus,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp)
                        .background(if (settings.showTabs) tabStripBg else topBarBg)
                        .statusBarsPadding(),
                ) {
                    if (settings.showTabs) {
                        TabStrip(
                            tabs = resolvedTabs,
                            current = noteId,
                            darkUi = darkUi,
                            tabStripBg = tabStripBg,
                            activeTabBg = topBarBg,
                            fgColor = topBarFg,
                            onHome = { vm.saveNow(); onBack() },
                            onRenameCurrent = { renaming = true },
                            onNewPageTab = { vm.addPage(atEnd = true) },
                            onSelect = ::switchTo,
                            onClose = { id ->
                                val rest = tabs - id
                                store.closeTab(id)
                                if (id == noteId) {
                                    vm.saveNow()
                                    if (rest.isNotEmpty()) onSwitchNote(rest.last()) else onBack()
                                }
                            },
                        )
                    }
                    Surface(
                        color = topBarBg,
                        contentColor = topBarFg,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CompositionLocalProvider(LocalContentColor provides topBarFg) {
                            TopBar(
                                vm = vm,
                                settings = settings,
                                showTabs = settings.showTabs,
                                showSearch = showSearch,
                                showPages = showPages,
                                hideTools = hideTools,
                                darkUi = darkUi,
                                activePopoverTool = activePopoverTool,
                                onTogglePopover = { t ->
                                    activePopoverTool = if (activePopoverTool == t) null else t
                                },
                                onHome = { vm.saveNow(); onBack() },
                                onPages = { showPages = !showPages },
                                onSearch = {
                                    showSearch = !showSearch
                                    if (!showSearch) vm.clearSearch()
                                },
                                onRename = { renaming = true },
                                onFocus = { focus = true },
                                onExport = { exportPdf.launch(vm.title + ".pdf") },
                                onShare = { sharePdf() },
                                onSharePage = {
                                    scope.launch {
                                        vm.pageImage(vm.pageIndex)?.let { shareBitmap(context, it, "${vm.title} p${vm.pageIndex + 1}") }
                                    }
                                },
                                onSettings = { vm.saveNow(); onOpenSettings() },
                                onGoTo = { goTo = true },
                                onScrollToggle = {
                                    store.update {
                                        it.copy(
                                            scroll = if (it.scroll == ScrollDirection.VERTICAL) ScrollDirection.HORIZONTAL else ScrollDirection.VERTICAL,
                                        )
                                    }
                                },
                                onPaste = { vm.paste()?.let { (pg, ids) -> canvas?.selectItems(pg, ids) } },
                                onPageColor = { pageColor = true },
                                onCards = { showCards = true },
                                onSlideShow = { vm.showSlideShow = true },
                                onKindleMode = { kindleMode = true },
                                onCollabRoom = { showCollabRoom = true },
                                onAddWebLink = {
                                    val p = vm.currentPage
                                    vm.openWebLink(vm.pageIndex, (p?.width ?: 1240f) / 2f, 340f, null)
                                },
                                onTemplates = { showTemplates = true },
                                onImportFiles = {
                                    importFilesLauncher.launch(
                                        arrayOf("application/pdf", "text/*", "image/*", "application/json", "application/octet-stream"),
                                    )
                                },
                                onSplit = {
                                    splitId = if (splitId != null) null else library.notebooks.firstOrNull { it.deletedAt == null && it.id != noteId }?.id ?: noteId
                                },
                                onPageImage = {
                                    pickBackground.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            )
                        }
                    }
                    AnimatedVisibility(showSearch, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        SearchBar(vm)
                    }
                }
            }

            Row(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedVisibility(
                    showPages && !focus,
                    enter = slideInHorizontally { -it } + fadeIn(),
                    exit = slideOutHorizontally { -it } + fadeOut(),
                ) {
                    PagesPanel(vm, onJump = { vm.goToPage(it) }, onClose = { showPages = false })
                }

                var areaSize by remember { mutableStateOf(IntSize.Zero) }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onSizeChanged { areaSize = it },
                ) {
                    if (vm.loaded) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
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
                                    override fun onStickyChecklistToggle(page: Int, sticky: StickyItem, lineIndex: Int) =
                                        vm.toggleStickyCheckLine(page, sticky, lineIndex)
                                    override fun onWebLinkTap(page: Int, x: Float, y: Float, existing: WebLinkItem?) =
                                        vm.openWebLink(page, x, y, existing)
                                    override fun onMindMapTap(page: Int, x: Float, y: Float, existing: MindNodeItem?) =
                                        vm.onMindMapTap(page, x, y, existing)
                                    override fun onCurrentPage(index: Int) = vm.onCurrentPage(index)
                                    override fun onPullAddPage() = vm.addPage(atEnd = true)
                                    override fun undo() = vm.undo()
                                    override fun redo() = vm.redo()
                                    override fun pdfBitmap(page: Page, widthPx: Int, onReady: () -> Unit) = vm.pdfBitmap(page, widthPx, onReady)
                                    override fun pdfDetail(page: Int) = vm.pdfDetail(page)
                                    override fun tapePattern() = vm.tapePattern
                                    override fun onPenActive(active: Boolean) { vm.penActive = active }
                                    override fun requestDetail(wants: List<Triple<Int, android.graphics.RectF, Float>>, onReady: () -> Unit) =
                                        vm.requestDetail(wants, onReady)
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
                                            PenAction.QUICK_WHEEL -> wheelAt = Offset(x, y)
                                            PenAction.UNDO -> vm.undo()
                                            PenAction.TOGGLE_ERASER -> vm.tool = if (vm.tool == Tool.ERASER) Tool.PEN else Tool.ERASER
                                            PenAction.LAST_TOOL -> vm.swapToPrevious()
                                            PenAction.NONE -> Unit
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

                    val overlayFade by animateFloatAsState(if (vm.penActive) 0.14f else 1f, tween(280), label = "ov")

                    // SVG Top-Left Floating Undo / Redo Pill
                    if (!focus) {
                        val undoPillBg = if (darkUi) PopoverDarkBg else PopoverLightBg
                        val undoPillBorder = if (darkUi) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.12f)
                        val undoFg = if (darkUi) Color(0xFFD1D5DB) else Color(0xFF1F2937)
                        AppSurface(
                            role = SurfaceRole.Chrome,
                            containerColor = undoPillBg,
                            contentColor = undoFg,
                            shape = RoundedCornerShape(16.dp),
                            borderColor = undoPillBorder,
                            shadowElevation = 6.dp,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 12.dp, top = 12.dp)
                                .graphicsLayer { alpha = overlayFade },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                BarIcon(
                                    icon = ProIcon.UNDO,
                                    label = "Undo",
                                    enabled = vm.canUndo,
                                    fgColor = undoFg,
                                    onClick = vm::undo,
                                )
                                BarIcon(
                                    icon = ProIcon.REDO,
                                    label = "Redo",
                                    enabled = vm.canRedo,
                                    fgColor = undoFg,
                                    onClick = vm::redo,
                                )
                                if (vm.currentPage?.isWhiteboard == true) {
                                    Box(
                                        Modifier
                                            .padding(horizontal = 4.dp)
                                            .width(1.dp)
                                            .height(18.dp)
                                            .background(undoPillBorder),
                                    )
                                    TextButton(
                                        onClick = { canvas?.resetWhiteboardView() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp),
                                    ) {
                                        Text("Reset view", style = MaterialTheme.typography.labelSmall, color = ProButtonPalette.ElectricBlue)
                                    }
                                }
                            }
                        }
                    }

                    // SVG Floating Obsidian Sub-Toolbar / Pen & Options Dock
                    if (!hideTools) {
                        ObsidianSubToolDock(
                            vm = vm,
                            settings = settings,
                            darkUi = darkUi,
                            area = areaSize,
                            overlayFade = overlayFade,
                            activePopoverTool = activePopoverTool,
                            onTogglePopover = { t ->
                                activePopoverTool = if (activePopoverTool == t) null else t
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp, start = 148.dp, end = 12.dp),
                        )
                    }

                    // SVG-anchored Caret Tool Popover (Pen Studio / Highlighter / Eraser / Shapes / etc.)
                    activePopoverTool?.let { popoverTool ->
                        val anchorAlign = if (LocalConfiguration.current.screenWidthDp < 720) Alignment.TopCenter else Alignment.TopCenter
                        Popup(
                            alignment = anchorAlign,
                            offset = IntOffset(0, (78 * density).roundToInt()),
                            onDismissRequest = { activePopoverTool = null },
                            properties = PopupProperties(focusable = true),
                        ) {
                            ToolPopoverCard(
                                tool = popoverTool,
                                vm = vm,
                                darkUi = darkUi,
                                onDismiss = { activePopoverTool = null },
                            )
                        }
                    }

                    // Floating selection menu
                    val sb = vm.selectionBounds
                    AnimatedVisibility(
                        visible = vm.selection.isNotEmpty() && sb != null,
                        enter = fadeIn() + scaleIn(initialScale = 0.85f),
                        exit = fadeOut() + scaleOut(targetScale = 0.85f),
                        modifier = Modifier.offset {
                            val b = sb ?: android.graphics.RectF()
                            val below = b.bottom + 18 * density
                            val y = if (below + 60 * density < areaSize.height) below else (b.top - 110 * density).coerceAtLeast(8f)
                            IntOffset(
                                b.left.roundToInt().coerceIn(8, (areaSize.width - 480 * density).roundToInt().coerceAtLeast(8)),
                                y.roundToInt(),
                            )
                        },
                    ) {
                        SelectionMenu(
                            vm = vm,
                            onDuplicate = {
                                val ids = vm.duplicateSelection()
                                canvas?.selectItems(vm.selectionPage, ids)
                            },
                            onColor = { c ->
                                val ids = vm.selection
                                val pg = vm.selectionPage
                                vm.recolorSelection(c)
                                canvas?.selectItems(pg, ids)
                            },
                            keep = { block ->
                                val ids = vm.selection
                                val pg = vm.selectionPage
                                block()
                                canvas?.selectItems(pg, ids)
                            },
                            onShareImage = {
                                scope.launch { vm.selectionImage()?.let { shareBitmap(context, it, vm.title + " selection") } }
                            },
                        )
                    }

                    // Zoom indicator pill
                    vm.zoomPercent?.let { z ->
                        AppSurface(
                            role = SurfaceRole.Frost,
                            containerColor = Color(0xE01B1F2A),
                            contentColor = Color.White,
                            shape = appPillShape(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.align(Alignment.Center),
                        ) {
                            Text("$z%", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                    }

                    // Exit Focus Mode button
                    if (focus) {
                        AppPill(
                            onClick = { focus = false },
                            contentPadding = PaddingValues(10.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(12.dp),
                        ) {
                            ProIconView(ProIcon.CLOSE, LocalContentColor.current, Modifier.size(20.dp))
                        }
                    }

                    // Long-press on PDF text: copy, highlight, underline, strike, flashcard, search, share
                    vm.textMenu?.let { r ->
                        val clip = LocalClipboardManager.current
                        AppSurface(
                            role = SurfaceRole.Popover,
                            containerColor = Color(0xEB1B1F2A),
                            contentColor = Color.White,
                            shape = appPillShape(),
                            shadowElevation = 10.dp,
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.offset {
                                val y = (r.top - 64 * density).let { if (it < 8) r.bottom + 12 * density else it }
                                IntOffset(
                                    r.left.roundToInt().coerceIn(8, (areaSize.width - 360 * density).roundToInt().coerceAtLeast(8)),
                                    y.roundToInt(),
                                )
                            },
                        ) {
                            Row(
                                Modifier
                                    .widthIn(max = 640.dp)
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextButton(onClick = {
                                    vm.textSel?.text?.let { clip.setText(AnnotatedString(it)) }
                                    vm.clearTextSel()
                                }) { Text("Copy", color = Color.White) }
                                listOf(0xFFFFE066, 0xFF8CE99A, 0xFF74C0FC, 0xFFFFA8A8).forEach { c ->
                                    Box(
                                        Modifier
                                            .padding(3.dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(c))
                                            .clickable { vm.highlightTextSel(c.toInt()) },
                                    )
                                }
                                TextButton(onClick = { vm.lineTextSel(strike = false) }) { Text("Underline", color = Color.White) }
                                TextButton(onClick = { vm.lineTextSel(strike = true) }) { Text("Strike", color = Color.White) }
                                TextButton(onClick = { vm.cardFromTextSel() }) { Text("Flashcard", color = Color.White) }
                                TextButton(onClick = {
                                    val q = vm.textSel?.text ?: ""
                                    vm.clearTextSel()
                                    showSearch = true
                                    vm.search(q)
                                }) { Text("Search", color = Color.White) }
                                TextButton(onClick = {
                                    val q = vm.textSel?.text ?: ""
                                    vm.clearTextSel()
                                    context.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, q),
                                            "Share",
                                        ),
                                    )
                                }) { Text("Share", color = Color.White) }
                            }
                        }
                    }

                    // Toast banner
                    vm.toast?.let { msg ->
                        AppSurface(
                            role = SurfaceRole.Popover,
                            containerColor = Color(0xEB1B1F2A),
                            contentColor = Color.White,
                            shape = appPillShape(),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 112.dp),
                        ) {
                            Text(msg, style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }

                    // Mind-map contextual bottom strip
                    if (!focus && !vm.readOnly && (vm.tool == Tool.MINDMAP || vm.activeMindNodeId != null)) {
                        val selNode = vm.selectedMindNode()
                        val hasKids = selNode != null && vm.currentPage?.items?.any { it is MindNodeItem && it.parentId == selNode.id } == true
                        AppSurface(
                            role = SurfaceRole.Chrome,
                            shape = appPillShape(),
                            shadowElevation = 6.dp,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 18.dp)
                                .graphicsLayer { alpha = overlayFade },
                        ) {
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
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
                                    TextButton(onClick = { vm.deleteMindNode(selNode.id) }) {
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    }
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
                            }
                        }
                    }

                    // Read-only badge
                    if (vm.readOnly && !focus) {
                        AppPill(
                            onClick = { vm.toggleReadOnly() },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(12.dp),
                        ) {
                            Icon(Icons.Outlined.Lock, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Read only · tap to edit", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // SVG Bottom-Left Page Indicator Pill ("X of Y")
                    if (!focus && settings.pageNumbers && vm.pages.isNotEmpty()) {
                        val pagePillBg = if (darkUi) Color(0xFF2A2A2A) else Color(0xFFE8E5DC)
                        val pagePillFg = if (darkUi) Color.White else Color(0xFF1F2421)
                        AppSurface(
                            role = SurfaceRole.Capsule,
                            containerColor = pagePillBg,
                            contentColor = pagePillFg,
                            shape = appPillShape(),
                            shadowElevation = 4.dp,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                            onClick = { goTo = true },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .navigationBarsPadding()
                                .padding(start = 16.dp, bottom = 16.dp)
                                .graphicsLayer { alpha = overlayFade },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                if (vm.currentPage?.bookmarked == true) {
                                    ProIconView(ProIcon.BOOKMARK, Color(0xFFE34948), Modifier.size(13.dp), ink = Color(0xFFE34948))
                                }
                                Text(
                                    text = if (vm.currentPage?.isWhiteboard == true) {
                                        "Board · ${vm.pageIndex + 1} of ${vm.pages.size}"
                                    } else {
                                        "${vm.pageIndex + 1} of ${vm.pages.size}"
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    // Draggable Study Timer (defaulting to bottom-right 0.86, 0.92)
                    if (settings.studyTimer && !focus) {
                        StudyTimer(vm, settings, areaSize)
                    }

                    // Area-capture / Link-target banner
                    val banner = when {
                        vm.capturingCard && vm.backFor != null -> "Drag a box over the answer"
                        vm.capturingCard -> "Drag a box over what goes on the card"
                        vm.linkFrom != null -> "Tap where this should link to"
                        else -> null
                    }
                    if (banner != null) {
                        AppPill(
                            selected = true,
                            tint = ProButtonPalette.ElectricBlue,
                            contentPadding = PaddingValues(start = 18.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 72.dp),
                        ) {
                            Text(banner, style = MaterialTheme.typography.labelLarge, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { vm.capturingCard = false; vm.linkFrom = null }) {
                                Text("Cancel", color = Color.White)
                            }
                        }
                    }
                }

                // Split view (right)
                splitId?.let { other ->
                    Box(
                        Modifier
                            .width(12.dp)
                            .fillMaxHeight()
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
                        onClose = { splitId = null },
                        modifier = Modifier.weight(splitWeight).fillMaxHeight(),
                    )
                }
            }
        }

        // Quick tool wheel (stylus button click while hovering)
        wheelAt?.let { at -> ToolWheel(vm, at, onDismiss = { wheelAt = null }) }

        // Kindle Reading Mode overlay with animated 3D page turns
        if (kindleMode) {
            KindleReaderOverlay(vm = vm, onExit = { kindleMode = false })
        }
    }

    // ------------------------------------------------------------------ dialogs & sheets
    vm.textEdit?.let { edit -> TextDialog(vm, edit) }
    vm.mindNodeEdit?.let { edit -> MindNodeDialog(vm, edit) }
    vm.cardDraft?.let { d -> CardEditorDialog(vm, d) }
    if (vm.linkSheet != null) LinkSheet(vm)
    if (showCards) FlashcardsDialog(vm, onClose = { showCards = false })
    if (vm.showSlideShow) SlideShowDialog(vm, onClose = { vm.showSlideShow = false })
    if (showTemplates) TemplateSheet(vm, onClose = { showTemplates = false })

    vm.linkToDelete?.let { id ->
        AppDialog(
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
        val styles = listOf("classic" to "Classic", "lined" to "Lined", "tinted" to "Tinted", "outline" to "Outline")
        AppDialog(
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
                        title,
                        { title = it.take(48) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
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
                        AppChip(
                            selected = pinned,
                            onClick = { pinned = !pinned },
                            label = if (pinned) "Pinned" else "Pin position",
                        )
                        AppChip(
                            selected = collapsed,
                            onClick = { collapsed = !collapsed },
                            label = if (collapsed) "Folded" else "Fold strip",
                        )
                        TextButton(onClick = {
                            text = if (text.isBlank()) "- [ ] " else text.trimEnd() + "\n- [ ] "
                        }) { Text("+ Task") }
                    }
                    OutlinedTextField(
                        text,
                        { text = it },
                        minLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("Write a note, formula, or checklist (- [ ] Item)…") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(color).copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
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
                    Text(
                        if (edit.isNew) "Cancel" else "Delete",
                        color = if (edit.isNew) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    )
                }
            },
        )
    }

    vm.webLinkEdit?.let { edit -> WebLinkDialog(vm, edit) }

    if (pageColor) {
        val opts = listOf(0xFFFFFFFF, 0xFFFBF7EC, 0xFFF1F2F4, 0xFFFFF4E0, 0xFFEAF4FF, 0xFFEFF8EC, 0xFF22252B).map { it.toInt() }
        ColorEditorDialog(vm.currentPage?.paperColor ?: 0xFFFFFFFF.toInt(), opts, emptyList(), onDismiss = { pageColor = false }) { c ->
            vm.setPageColor(c)
            pageColor = false
        }
    }

    if (renaming) {
        var name by remember { mutableStateOf(vm.title) }
        AppDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename notebook") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, shape = Pill) },
            confirmButton = { TextButton(onClick = { vm.rename(name.trim()); renaming = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } },
        )
    }

    if (goTo) {
        var n by remember { mutableStateOf("${vm.pageIndex + 1}") }
        AppDialog(
            onDismissRequest = { goTo = false },
            title = { Text("Go to page") },
            text = {
                OutlinedTextField(
                    n,
                    { v -> n = v.filter { it.isDigit() }.take(5) },
                    singleLine = true,
                    shape = Pill,
                    supportingText = { Text("1 – ${vm.pages.size}") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    n.toIntOrNull()?.let { vm.goToPage((it - 1).coerceIn(0, vm.pages.size - 1)) }
                    goTo = false
                }) { Text("Go") }
            },
            dismissButton = { TextButton(onClick = { goTo = false }) { Text("Cancel") } },
        )
    }

    if (showStickers) {
        AppSheet(onDismissRequest = { showStickers = false; vm.tool = Tool.LASSO }) {
            StickerSheet { bmp ->
                showStickers = false
                vm.tool = Tool.LASSO
                vm.insertStickerBitmap(bmp) { pg, id -> canvas?.selectItems(pg, setOf(id)) }
            }
        }
    }

    if (showCollabRoom) {
        LiveCollabRoomDialog(vm = vm, onDismiss = { showCollabRoom = false })
    }
}

// ============================================================== tabs + top bar

@Composable
private fun TabStrip(
    tabs: List<Pair<String, String>>,
    current: String,
    darkUi: Boolean,
    tabStripBg: Color,
    activeTabBg: Color,
    fgColor: Color,
    onHome: () -> Unit,
    onRenameCurrent: () -> Unit,
    onNewPageTab: () -> Unit,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
) {
    val mutedFg = if (darkUi) Color(0xFF8E9BBE) else Color(0xFF475569)
    val closeFg = if (darkUi) Color(0xFF8FA3C4) else Color(0xFF64748B)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(tabStripBg)
            .padding(start = 10.dp, end = 10.dp, top = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .size(30.dp)
                .clip(CircleShape)
                .elasticStretch()
                .clickable(onClick = onHome),
            contentAlignment = Alignment.Center,
        ) {
            ProIconView(ProIcon.HOME, mutedFg, Modifier.size(18.dp))
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tabs.forEach { (id, tabTitle) ->
                val sel = id == current
                val shape = if (sel) {
                    RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                } else {
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                }
                val bg = if (sel) activeTabBg else if (darkUi) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                val labelColor = if (sel) fgColor else mutedFg
                Row(
                    modifier = Modifier
                        .height(if (sel) 36.dp else 32.dp)
                        .widthIn(min = 126.dp, max = 240.dp)
                        .clip(shape)
                        .background(bg)
                        .clickable {
                            if (sel) onRenameCurrent() else onSelect(id)
                        }
                        .padding(start = 14.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = tabTitle,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = labelColor,
                    )
                    if (sel) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Rename",
                            tint = fgColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .clickable { onClose(id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        ProIconView(ProIcon.CLOSE, if (sel) closeFg else mutedFg, Modifier.size(10.dp))
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .elasticStretch()
                .clickable(onClick = onNewPageTab),
            contentAlignment = Alignment.Center,
        ) {
            ProIconView(ProIcon.ADD, mutedFg, Modifier.size(17.dp))
        }
    }
}

@Composable
private fun BarIcon(
    icon: ProIcon,
    label: String,
    enabled: Boolean = true,
    active: Boolean = false,
    fgColor: Color = LocalContentColor.current,
    activeBg: Color = ProButtonPalette.ElectricBlue,
    activeFg: Color = Color.White,
    onClick: () -> Unit,
) {
    val tint = when {
        !enabled -> fgColor.copy(alpha = 0.32f)
        active -> activeFg
        else -> fgColor
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .elasticStretch(enabled)
            .clip(RoundedCornerShape(11.dp))
            .background(if (active) activeBg else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ProIconView(icon, tint, Modifier.size(20.dp))
    }
}

@Composable
private fun TopBar(
    vm: EditorViewModel,
    settings: AppSettings,
    showTabs: Boolean,
    showSearch: Boolean,
    showPages: Boolean,
    hideTools: Boolean,
    darkUi: Boolean,
    activePopoverTool: Tool?,
    onTogglePopover: (Tool) -> Unit,
    onHome: () -> Unit,
    onPages: () -> Unit,
    onSearch: () -> Unit,
    onRename: () -> Unit,
    onFocus: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onSharePage: () -> Unit,
    onSettings: () -> Unit,
    onGoTo: () -> Unit,
    onScrollToggle: () -> Unit,
    onPaste: () -> Unit,
    onPageColor: () -> Unit,
    onPageImage: () -> Unit,
    onCards: () -> Unit,
    onSlideShow: () -> Unit,
    onKindleMode: () -> Unit,
    onCollabRoom: () -> Unit,
    onAddWebLink: () -> Unit,
    onTemplates: () -> Unit,
    onImportFiles: () -> Unit,
    onSplit: () -> Unit,
) {
    val store = SettingsStore.get(LocalContext.current)
    val fgColor = LocalContentColor.current
    val dividerColor = fgColor.copy(alpha = 0.24f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (!showTabs) {
            BarIcon(ProIcon.HOME, "Library", fgColor = fgColor, onClick = onHome)
        }
        BarIcon(ProIcon.SIDEBAR, "Pages", active = showPages, fgColor = fgColor, onClick = onPages)
        BarIcon(ProIcon.SEARCH, "Search", active = showSearch, fgColor = fgColor, onClick = onSearch)
        BarIcon(ProIcon.PAGES, "Jump to page", fgColor = fgColor, onClick = onGoTo)

        if (!hideTools) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val visible = ToolbarOrder.visible(settings.toolbarTools)
                    visible.forEach { t ->
                        ToolButton(
                            vm = vm,
                            t = t,
                            settings = settings,
                            darkUi = darkUi,
                            barFg = fgColor,
                            popoverOpen = activePopoverTool == t,
                            onOpenPopover = { onTogglePopover(t) },
                            onReorderStep = { toolToMove, step ->
                                val idx = visible.indexOf(toolToMove)
                                val targetIdx = (idx + step).coerceIn(0, visible.lastIndex)
                                if (idx >= 0 && targetIdx != idx) {
                                    store.update {
                                        it.copy(toolbarTools = ToolbarOrder.moveVisible(it.toolbarTools, toolToMove, visible[targetIdx]))
                                    }
                                }
                            },
                        )
                    }
                }
            }
        } else {
            Text(
                vm.title,
                style = MaterialTheme.typography.titleMedium,
                color = fgColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .clickable(onClick = onRename),
            )
        }

        Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(22.dp).background(dividerColor))

        // Quick Book Mode (Kindle Reader) trigger in TopBar
        BarIcon(
            icon = ProIcon.BOOK,
            label = "Book Mode",
            fgColor = fgColor,
            onClick = onKindleMode,
        )

        AppMenuButton(
            contentDescription = "Add page or template",
            containerColor = ProButtonPalette.Emerald.copy(alpha = if (darkUi) 0.24f else 0.16f),
            contentPadding = PaddingValues(7.dp),
            modifier = Modifier.elasticStretch(),
            buttonContent = {
                ProIconView(ProIcon.ADD, if (darkUi) Color(0xFF6EE7B7) else Color(0xFF047857), Modifier.size(20.dp))
            },
            items = listOf(
                AppMenuItem("New page after this") { vm.addPage() },
                AppMenuItem("New page at the end") { vm.addPage(atEnd = true) },
                AppMenuItem("Infinite whiteboard page") { vm.addPage(paper = PaperStyle.DOTTED, isWhiteboard = true) },
                AppMenuItem("Choose a template…") { onTemplates() },
            ),
        )
        Spacer(Modifier.width(3.dp))
        AppMenuButton(
            contentDescription = "Share and export",
            containerColor = ProButtonPalette.SkyCyan.copy(alpha = if (darkUi) 0.24f else 0.16f),
            contentPadding = PaddingValues(7.dp),
            modifier = Modifier.elasticStretch(),
            buttonContent = {
                ProIconView(ProIcon.SHARE, if (darkUi) Color(0xFF7DD3FC) else Color(0xFF0369A1), Modifier.size(20.dp))
            },
            items = listOf(
                AppMenuItem("Export as PDF…", icon = { Icon(Icons.Outlined.PictureAsPdf, null, Modifier.size(18.dp)) }) { onExport() },
                AppMenuItem("Share PDF", icon = { Icon(Icons.Outlined.IosShare, null, Modifier.size(18.dp)) }) { onShare() },
                AppMenuItem("Share this page as image", icon = { Icon(Icons.Outlined.Image, null, Modifier.size(18.dp)) }) { onSharePage() },
            ),
        )
        Spacer(Modifier.width(3.dp))
        val moreEntries = buildList {
            add(
                AppMenuItem(
                    "Book Mode (Kindle)",
                    icon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(18.dp), tint = ProButtonPalette.VibrantOrange) },
                ) { onKindleMode() },
            )
            add(
                AppMenuItem(
                    "Flashcards",
                    icon = { ProIconView(ProIcon.FLASHCARDS, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp), ink = ProButtonPalette.RoyalBlue.copy(alpha = 0.22f)) },
                ) { onCards() },
            )
            add(
                AppMenuItem(
                    "Flashcard from area",
                    icon = { Icon(Icons.Outlined.Crop, null, Modifier.size(18.dp)) },
                ) { vm.capturingCard = true },
            )
            add(
                AppMenuItem(
                    "SlideShow",
                    icon = { ProIconView(ProIcon.SLIDESHOW, ProButtonPalette.MagentaPink, Modifier.size(18.dp)) },
                ) { onSlideShow() },
            )
            add(
                AppMenuItem(
                    "Live Study Room",
                    icon = { Icon(Icons.Outlined.Groups, null, Modifier.size(18.dp), tint = ProButtonPalette.ElectricBlue) },
                ) { onCollabRoom() },
            )
            add(
                AppMenuItem(
                    "Add web link card…",
                    icon = { ProIconView(ProIcon.WEBLINK, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp), ink = ProButtonPalette.ElectricBlue) },
                ) { onAddWebLink() },
            )
            add(
                AppMenuItem(
                    "Link item to page/URL…",
                    icon = { Icon(Icons.Outlined.Link, null, Modifier.size(18.dp)) },
                ) { vm.beginLinkFlow() },
            )
            add(
                AppMenuItem(
                    "Split view",
                    icon = { ProIconView(ProIcon.SPLIT, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) { onSplit() },
            )
            add(
                AppMenuItem(
                    "Page template…",
                    icon = { ProIconView(ProIcon.TEMPLATE, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) { onTemplates() },
            )
            add(
                AppMenuItem(
                    if (vm.currentPage?.isWhiteboard == true) "Switch to standard page" else "Infinite whiteboard",
                    icon = { Icon(Icons.Outlined.AllInclusive, null, Modifier.size(18.dp)) },
                ) { vm.toggleCurrentWhiteboard() },
            )
            add(
                AppMenuItem(
                    "Import files into notebook…",
                    icon = { Icon(Icons.Outlined.UploadFile, null, Modifier.size(18.dp)) },
                ) { onImportFiles() },
            )
            add(
                AppMenuItem(
                    "Page colour…",
                    icon = { ProIconView(ProIcon.PALETTE, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) { onPageColor() },
            )
            add(
                AppMenuItem(
                    if (vm.currentPage?.bgImage != null) "Remove page picture" else "Picture as page…",
                    icon = { ProIconView(ProIcon.IMAGE, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) {
                    if (vm.currentPage?.bgImage != null) vm.setPageBackground(null) else onPageImage()
                },
            )
            add(
                AppMenuItem(
                    if (vm.currentPage?.bookmarked == true) "Remove bookmark" else "Bookmark page",
                    icon = {
                        ProIconView(
                            ProIcon.BOOKMARK,
                            Color(0xFFE34948),
                            Modifier.size(18.dp),
                            ink = if (vm.currentPage?.bookmarked == true) Color(0xFFE34948) else null,
                        )
                    },
                ) { vm.toggleBookmark() },
            )
            if (vm.hasClipboard) {
                add(
                    AppMenuItem(
                        "Paste",
                        icon = { Icon(Icons.Outlined.ContentPaste, null, Modifier.size(18.dp)) },
                    ) { onPaste() },
                )
            }
            add(
                AppMenuItem(
                    "Duplicate page",
                    icon = { ProIconView(ProIcon.COPY, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) { vm.duplicatePage() },
            )
            if (vm.pages.size > 1) {
                add(
                    AppMenuItem(
                        "Delete page",
                        destructive = true,
                        icon = { ProIconView(ProIcon.TRASH, MaterialTheme.colorScheme.error, Modifier.size(18.dp)) },
                    ) { vm.deletePage() },
                )
            }
            add(
                AppMenuItem(
                    "Rename notebook",
                    icon = { ProIconView(ProIcon.RENAME, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) { onRename() },
            )
            add(
                AppMenuItem(
                    if (vm.readOnly) "Turn off read only" else "Read only",
                    icon = {
                        ProIconView(
                            ProIcon.LOCK,
                            MaterialTheme.colorScheme.onSurface,
                            Modifier.size(18.dp),
                            ink = if (vm.readOnly) MaterialTheme.colorScheme.primary else null,
                        )
                    },
                ) { vm.toggleReadOnly() },
            )
            add(
                AppMenuItem(
                    "Go to page…",
                    icon = { ProIconView(ProIcon.PAGES, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) { onGoTo() },
            )
            add(
                AppMenuItem(
                    if (settings.scroll == ScrollDirection.VERTICAL) "Scroll horizontally" else "Scroll vertically",
                    icon = { Icon(Icons.Outlined.SwapVert, null, Modifier.size(18.dp)) },
                ) { onScrollToggle() },
            )
            add(
                AppMenuItem(
                    "Focus mode",
                    icon = { ProIconView(ProIcon.FOCUS, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) { onFocus() },
            )
            add(
                AppMenuItem(
                    "Settings",
                    icon = { ProIconView(ProIcon.SETTINGS, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) { onSettings() },
            )
        }
        AppMenuButton(
            contentDescription = "More tools and actions",
            containerColor = ProButtonPalette.WarmAmber.copy(alpha = if (darkUi) 0.24f else 0.16f),
            contentPadding = PaddingValues(7.dp),
            modifier = Modifier.elasticStretch(),
            buttonContent = {
                ProIconView(ProIcon.MORE, if (darkUi) Color(0xFFFCD34D) else Color(0xFFB45309), Modifier.size(20.dp))
            },
            items = moreEntries,
        )
    }
}

// ============================================================== SVG obsidian sub-tool dock

@Composable
private fun ObsidianSubToolDock(
    vm: EditorViewModel,
    settings: AppSettings,
    darkUi: Boolean,
    area: IntSize,
    overlayFade: Float,
    activePopoverTool: Tool?,
    onTogglePopover: (Tool) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dockBg = if (darkUi) DockDarkBg else DockLightBg
    val dockFg = if (darkUi) Color.White else Color(0xFF1E2321)
    val dividerColor = dockFg.copy(alpha = 0.20f)
    val dens = LocalDensity.current
    val areaW = with(dens) { area.width.toDp() }
    val maxDockW = (areaW - 180.dp).coerceAtLeast(220.dp)
    val quickTools = remember { listOf(Tool.PEN, Tool.HIGHLIGHTER, Tool.TAPE, Tool.SHAPE) }

    AppSurface(
        role = SurfaceRole.Chrome,
        containerColor = dockBg,
        contentColor = dockFg,
        shape = appPillShape(),
        shadowElevation = 10.dp,
        borderColor = if (darkUi) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.08f),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        modifier = modifier
            .widthIn(max = maxDockW)
            .graphicsLayer { alpha = overlayFade },
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // SVG Quick-Tool Strip (highlighted active teal slot with chevron)
            quickTools.forEach { t ->
                val isCurrent = vm.tool == t || (vm.tool !in quickTools && t == Tool.PEN)
                val showActiveSlot = vm.tool == t
                val slotInk = Color(vm.styleFor(t)?.color ?: if (t == Tool.TAPE) vm.tapeColor else vm.penStyle.color)
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (showActiveSlot) TealActiveTile else Color.Transparent)
                        .elasticStretch(pressScale = 0.92f)
                        .clickable {
                            if (vm.tool == t) onTogglePopover(t) else vm.tool = t
                        }
                        .padding(horizontal = if (showActiveSlot) 10.dp else 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    ToolGlyph(
                        tool = t,
                        tint = if (showActiveSlot) Color.White else dockFg,
                        inkColor = slotInk,
                        modifier = Modifier.size(21.dp),
                        active = showActiveSlot || isCurrent,
                    )
                    if (showActiveSlot) {
                        Icon(
                            if (activePopoverTool == t) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Tool options",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            // Secondary tool trigger chip if another tool (Eraser, Text, MindMap, Lasso, Sticky, WebLink) is active
            if (vm.tool !in quickTools && vm.tool != Tool.IMAGE && vm.tool != Tool.STICKER) {
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(TealActiveTile)
                        .elasticStretch()
                        .clickable { onTogglePopover(vm.tool) }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    ToolGlyph(vm.tool, Color.White, Color(vm.penStyle.color), Modifier.size(19.dp), active = true)
                    Text(
                        toolDisplayName(vm.tool),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        if (activePopoverTool == vm.tool) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Tool studio",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Box(Modifier.padding(horizontal = 2.dp).width(1.dp).height(24.dp).background(dividerColor))

            // Active Tool Width & Colour Options right inside the Obsidian dock
            ToolOptions(vm = vm, vertical = false, settings = settings, dockFg = dockFg, darkDock = darkUi)
        }
    }
}

// ============================================================== search

@Composable
private fun SearchBar(vm: EditorViewModel) {
    AppCard(
        hero = false,
        shape = appCardShape(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = vm.searchQuery,
                    onValueChange = vm::search,
                    singleLine = true,
                    placeholder = { Text(if (vm.isPdf) "Search in PDF and notes" else "Search across notes") },
                    leadingIcon = { ProIconView(ProIcon.SEARCH, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp)) },
                    shape = Pill,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        vm.searching -> "Searching…"
                        vm.searchQuery.isBlank() -> ""
                        vm.searchHits.isEmpty() -> "No results"
                        else -> "${vm.activeHit + 1} of ${vm.searchHits.size}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = { vm.stepHit(-1) }, enabled = vm.searchHits.isNotEmpty()) {
                    Icon(Icons.Filled.KeyboardArrowUp, "Previous")
                }
                IconButton(onClick = { vm.stepHit(1) }, enabled = vm.searchHits.isNotEmpty()) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Next")
                }
                IconButton(onClick = vm::clearSearch) {
                    Icon(Icons.Filled.Close, "Clear")
                }
            }
            if (vm.searchHits.isNotEmpty()) {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
                    itemsIndexed(vm.searchHits, key = { idx, hit -> "${hit.page}:$idx:${hit.snippet.hashCode()}" }) { i, hit ->
                        AppPill(
                            selected = i == vm.activeHit,
                            tint = ProButtonPalette.ElectricBlue,
                            onClick = { vm.jumpToHit(i) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                val fg = if (i == vm.activeHit) Color.White else MaterialTheme.colorScheme.onSurface
                                Text(
                                    "p.${hit.page + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (i == vm.activeHit) fg else ProButtonPalette.ElectricBlue,
                                    modifier = Modifier.width(44.dp),
                                )
                                Text(
                                    hit.snippet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = fg,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================== pages panel

/** Left panel with page thumbnails (drag to reorder) and a bookmark outline. */
@Composable
private fun PagesPanel(vm: EditorViewModel, onJump: (Int) -> Unit, onClose: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    AppSurface(
        role = SurfaceRole.Frost,
        shape = RoundedCornerShape(0.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.width(204.dp).fillMaxHeight(),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScaleInAnimatedText(
                    text = "Pages",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { vm.addPage(atEnd = true) }) { Icon(Icons.Outlined.Add, "Add page") }
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close") }
            }
            AppSegmentedControl(
                options = listOf("All", "Bookmarks"),
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
            if (tab == 0) {
                val listState = rememberLazyListState()
                val reorder = rememberReorderableLazyListState(listState) { from, to -> vm.movePage(from.index, to.index) }
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(vm.pages, key = { _, p -> p.id }) { i, page ->
                        ReorderableItem(reorder, key = page.id) { dragging ->
                            val lift by animateFloatAsState(if (dragging) 1.06f else 1f, label = "lift")
                            PageThumb(
                                vm = vm,
                                i = i,
                                page = page,
                                modifier = Modifier
                                    .graphicsLayer { scaleX = lift; scaleY = lift }
                                    .longPressDraggableHandle(),
                                onJump = onJump,
                            )
                        }
                    }
                }
            } else {
                val marks = vm.pages.indices.filter { vm.pages[it].bookmarked }
                if (marks.isEmpty()) {
                    Text(
                        "Bookmark pages from the More menu to list them here.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(marks.size, key = { vm.pages[marks[it]].id }) { k ->
                        PageThumb(vm, marks[k], vm.pages[marks[k]], Modifier, onJump)
                    }
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
            Modifier
                .fillMaxWidth()
                .aspectRatio(page.width / page.height)
                .shadow(3.dp, RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(
                    if (i == vm.pageIndex) 2.dp else 0.dp,
                    ProButtonPalette.ElectricBlue,
                    RoundedCornerShape(8.dp),
                )
                .clickable { onJump(i) },
        ) {
            thumb?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
            if (page.bookmarked) {
                ProIconView(
                    ProIcon.BOOKMARK,
                    Color(0xFFE34948),
                    Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp),
                    ink = Color(0xFFE34948),
                )
            }
        }
        Text("${i + 1}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

// ============================================================== tool icons & options

@Composable
private fun PenTypeGlyph(type: PenType, tint: Color) {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.1f, h * 0.7f)
            cubicTo(w * 0.35f, h * 0.05f, w * 0.6f, h * 0.95f, w * 0.9f, h * 0.3f)
        }
        val width = when (type) {
            PenType.BRUSH -> 4.5f
            PenType.BALL -> 1.6f
            else -> 2.4f
        }.dp.toPx()
        val dash = if (type == PenType.DASHED) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null
        drawPath(path, tint, style = Stroke(width, cap = StrokeCap.Round, pathEffect = dash))
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

@Composable
private fun ToolButton(
    vm: EditorViewModel,
    t: Tool,
    settings: AppSettings,
    darkUi: Boolean,
    barFg: Color,
    popoverOpen: Boolean,
    onOpenPopover: () -> Unit,
    onReorderStep: ((Tool, Int) -> Unit)? = null,
) {
    val selected = vm.tool == t
    val motion = LocalMotionStyle.current
    val ink = Color(
        vm.styleFor(t)?.color ?: when (t) {
            Tool.TAPE -> vm.tapeColor
            Tool.STICKY -> vm.stickyColor
            Tool.WEBLINK -> vm.webLinkColor
            else -> vm.penStyle.color
        },
    )
    val sz = (38 * settings.toolbarSize.scale).dp
    val density = LocalDensity.current.density
    var hovered by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var stretchDx by remember { mutableFloatStateOf(0f) }
    var stretchDy by remember { mutableFloatStateOf(0f) }

    val springDamping = when (motion) {
        MotionStyle.ULTRA_SPRING -> 0.56f
        MotionStyle.MINIMAL_CRISP -> Spring.DampingRatioNoBouncy
        MotionStyle.REDUCED -> 1f
    }
    val stretchX by animateFloatAsState(
        targetValue = if (dragging) {
            1.14f + (abs(stretchDx) / (75f * density)).coerceIn(0f, 0.24f) - (abs(stretchDy) / (150f * density)).coerceIn(0f, 0.12f)
        } else if (selected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = springDamping, stiffness = Spring.StiffnessMediumLow),
        label = "sx",
    )
    val stretchY by animateFloatAsState(
        targetValue = if (dragging) {
            1.14f + (abs(stretchDy) / (75f * density)).coerceIn(0f, 0.24f) - (abs(stretchDx) / (150f * density)).coerceIn(0f, 0.12f)
        } else if (selected) 1.04f else 1f,
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
        Modifier.pointerInput(t) {
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
                    dragAccum += amount.x
                    if (abs(dragAccum) >= stepPx) {
                        val dir = if (dragAccum > 0f) 1 else -1
                        onReorderStep(t, dir)
                        dragAccum -= dir * stepPx
                    }
                },
            )
        }
    } else Modifier

    val activeSlotBg = if (darkUi) Color(0xFFCBD5E1) else ProButtonPalette.RoyalBlue
    val activeSlotFg = if (darkUi) Color(0xFF1A1A1A) else Color.White

    Box {
        Box(
            modifier = Modifier
                .padding(1.dp)
                .size(sz)
                .elasticStretch(enabled = !dragging, pressScale = 0.91f)
                .graphicsLayer {
                    scaleX = stretchX
                    scaleY = stretchY
                    translationX = offsetX
                    translationY = offsetY
                }
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when {
                        dragging -> ProButtonPalette.SkyCyan
                        selected -> activeSlotBg
                        else -> Color.Transparent
                    },
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
                        else -> onOpenPopover()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val tint = if (selected || dragging) activeSlotFg else barFg
            ToolGlyph(
                tool = t,
                tint = tint,
                inkColor = ink,
                modifier = Modifier.size((21 * settings.toolbarSize.scale).dp),
                active = selected || dragging,
            )
        }

        if (hovered && !dragging && !popoverOpen && settings.showHoverTooltips && settings.hoverCursor) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = IntOffset(0, (40 * density).toInt()),
                properties = PopupProperties(focusable = false),
            ) {
                AppSurface(
                    role = SurfaceRole.Popover,
                    shape = appPillShape(),
                    shadowElevation = 6.dp,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        toolDisplayName(t),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolOptions(
    vm: EditorViewModel,
    vertical: Boolean,
    settings: AppSettings,
    dockFg: Color = MaterialTheme.colorScheme.onSurface,
    darkDock: Boolean = false,
) {
    val store = SettingsStore.get(LocalContext.current)
    AnimatedContent(
        targetState = vm.tool,
        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.92f)) togetherWith fadeOut() },
        label = "options",
    ) { t ->
        val content = @Composable {
            val style = vm.styleFor(t)
            when {
                style != null -> StyleOptions(vm, t, style, dockFg, darkDock)
                t == Tool.ERASER -> {
                    SizeDots(listOf(8f, 16f, 30f), listOf(8f, 16f, 30f)[vm.eraserSize], ring = true, fg = dockFg, darkDock = darkDock) { s ->
                        vm.eraserSize = listOf(8f, 16f, 30f).indexOf(s)
                    }
                    MiniChip(if (settings.eraserMode == EraserMode.PIXEL) "Pixel" else "Stroke") {
                        store.update {
                            it.copy(eraserMode = if (it.eraserMode == EraserMode.PIXEL) EraserMode.STROKE else EraserMode.PIXEL)
                        }
                    }
                }
                t == Tool.TEXT -> SizeDots(listOf(28f, 40f, 64f), vm.textSize, fg = dockFg, darkDock = darkDock) { vm.textSize = it }
                t == Tool.TAPE -> {
                    CustomSwatches(TapeColors, vm.tapeColor, vm) { vm.tapeColor = it }
                    MiniChip(if (vm.tapeFreehand) "Freehand" else "Straight") { vm.tapeFreehand = !vm.tapeFreehand }
                }
                t == Tool.STICKY -> CustomSwatches(StickyColors, vm.stickyColor, vm) { vm.stickyColor = it }
                t == Tool.WEBLINK -> CustomSwatches(WebLinkColors, vm.webLinkColor, vm) { vm.webLinkColor = it }
                t == Tool.MINDMAP -> {
                    MindPalette.take(5).forEach { c ->
                        Swatch(c, c == vm.mindColor, 18) { vm.recolorMindNode(color = c) }
                    }
                    EditorViewModel.MindLayout.entries.forEach { layout ->
                        MiniChip(layout.label) { vm.autoLayoutMindMap(layout) }
                    }
                    MiniChip("Fit Map") { vm.fitMindMapToView() }
                    MiniChip(if (vm.moveMindSubtree) "Subtree: On" else "Subtree: Off") { vm.moveMindSubtree = !vm.moveMindSubtree }
                    MiniChip("Cards") { vm.flashcardsFromMindMap() }
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
    AppPill(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
        modifier = Modifier.padding(3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CustomSwatches(base: List<Int>, current: Int, vm: EditorViewModel, onPick: (Int) -> Unit) {
    var custom by remember { mutableStateOf(false) }
    base.forEach { c -> Swatch(c, c == current, 18) { onPick(c) } }
    if (current !in base) Swatch(current, true, 18) { custom = true }
    AddSwatchButton { custom = true }
    if (custom) {
        ColorEditorDialog(current, PenPalette, vm.recentColors(), onDismiss = { custom = false }) {
            onPick(it)
            vm.pushRecent(it)
            custom = false
        }
    }
}

@Composable
private fun AddSwatchButton(onClick: () -> Unit) {
    Box(
        Modifier
            .padding(3.dp)
            .size(22.dp)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    listOf(
                        Color(0xFFFF3B30),
                        Color(0xFFFFCC00),
                        Color(0xFF34C759),
                        Color(0xFF0A84FF),
                        Color(0xFFAF52DE),
                        Color(0xFFFF3B30),
                    ),
                ),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Add, "Custom colour", Modifier.size(11.dp))
        }
    }
}

@Composable
private fun StyleOptions(
    vm: EditorViewModel,
    t: Tool,
    style: ToolStyle,
    dockFg: Color,
    darkDock: Boolean,
) {
    var editing by remember { mutableStateOf<Int?>(null) }
    var adding by remember { mutableStateOf(false) }
    var removeMenu by remember { mutableStateOf<Int?>(null) }

    SizeDots(style.sizes, style.size, fg = dockFg, darkDock = darkDock) { sz ->
        vm.updateStyle(t, style.copy(size = sz))
    }
    Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(22.dp).background(dockFg.copy(alpha = 0.2f)))
    style.colors.forEachIndexed { i, c ->
        val selected = c == style.color
        Box {
            Swatch(c, selected, 18, onLongClick = { removeMenu = i }) {
                if (selected) editing = i else vm.updateStyle(t, style.copy(color = c))
            }
            AppContextMenu(
                expanded = removeMenu == i,
                onDismissRequest = { removeMenu = null },
                items = buildList {
                    add(AppMenuItem("Edit colour") { editing = i })
                    if (style.colors.size > 1) {
                        add(AppMenuItem("Remove", destructive = true) { vm.removeSwatch(t, i) })
                    }
                },
            )
        }
    }
    if (style.colors.size < 8) AddSwatchButton { adding = true }

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
        ColorEditorDialog(style.color, presets, vm.recentColors(), onDismiss = { adding = false }) { picked ->
            vm.addSwatch(t, picked)
            adding = false
        }
    }
}

@Composable
private fun SizeDots(
    sizes: List<Float>,
    current: Float,
    ring: Boolean = false,
    fg: Color = MaterialTheme.colorScheme.onSurface,
    darkDock: Boolean = false,
    onPick: (Float) -> Unit,
) {
    val activeBg = if (darkDock) Color(0xFF2A2A2A) else MaterialTheme.colorScheme.primaryContainer
    sizes.forEachIndexed { i, s ->
        val selected = s == current
        Box(
            Modifier
                .padding(horizontal = 2.dp)
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) activeBg else Color.Transparent)
                .elasticStretch(pressScale = 0.9f)
                .clickable { onPick(s) },
            contentAlignment = Alignment.Center,
        ) {
            val w = (2 + 2.5f * i).dp
            if (ring) {
                Box(Modifier.size((8 + 5 * i).dp).border(1.6.dp, fg, CircleShape))
            } else {
                Box(Modifier.width(18.dp).height(w).clip(Pill).background(fg))
            }
        }
    }
}

// ============================================================== SVG caret tool popovers

@Composable
private fun ToolPopoverCard(
    tool: Tool,
    vm: EditorViewModel,
    darkUi: Boolean,
    onDismiss: () -> Unit,
) {
    val pickTape = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.useTapeImage(uri)
    }
    val cardBg = if (darkUi) PopoverDarkBg else PopoverLightBg
    val cardFg = if (darkUi) Color.White else Color(0xFF181A1F)
    val borderTint = if (darkUi) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.08f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(min = 310.dp, max = 380.dp),
    ) {
        // SVG upward caret pointer
        Canvas(Modifier.size(width = 24.dp, height = 11.dp)) {
            val tri = Path().apply {
                moveTo(0f, size.height)
                lineTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(tri, cardBg)
        }
        AppSurface(
            role = SurfaceRole.Popover,
            containerColor = cardBg,
            contentColor = cardFg,
            shape = RoundedCornerShape(16.dp),
            borderColor = borderTint,
            shadowElevation = 18.dp,
            contentPadding = PaddingValues(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CompositionLocalProvider(LocalContentColor provides cardFg) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 540.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        ScaleInAnimatedText(
                            text = if (tool == Tool.PEN) vm.penType.label else toolDisplayName(tool),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = cardFg,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = cardFg.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                    }

                    when (tool) {
                        Tool.PEN -> PenCardContent(vm, darkUi, cardFg)
                        Tool.HIGHLIGHTER -> HighlighterCardContent(vm, darkUi, cardFg)
                        Tool.SHAPE -> ShapeCardContent(vm)
                        Tool.ERASER -> EraserCardContent(vm)
                        Tool.LASSO -> LassoCardContent()
                        Tool.LASER -> LaserCardContent()
                        Tool.TEXT -> TextCardContent(vm)
                        Tool.TAPE -> {
                            Segments(listOf("Straight", "Freehand"), if (vm.tapeFreehand) 1 else 0) { vm.tapeFreehand = it == 1 }
                            Row(verticalAlignment = Alignment.CenterVertically) { CustomSwatches(TapeColors, vm.tapeColor, vm) { vm.tapeColor = it } }
                            TapePatternPicker(vm) {
                                pickTape.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        }
                        Tool.STICKY -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CustomSwatches(StickyColors, vm.stickyColor, vm) { vm.stickyColor = it }
                        }
                        Tool.WEBLINK -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CustomSwatches(WebLinkColors, vm.webLinkColor, vm) { vm.webLinkColor = it }
                        }
                        Tool.MINDMAP -> MindMapCardContent(vm)
                        Tool.STICKER, Tool.IMAGE -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun Segments(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    AppSegmentedControl(
        options = options,
        selectedIndex = selected.coerceIn(0, (options.size - 1).coerceAtLeast(0)),
        onSelect = onPick,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StrokePreview(color: Color, width: Float, taper: Boolean, darkUi: Boolean) {
    val boxBg = if (darkUi) Color(0xFF242424) else Color(0xFFECE9E0)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(boxBg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Canvas(Modifier.fillMaxWidth().height(64.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.08f, size.height * 0.62f)
                cubicTo(size.width * 0.3f, size.height * 1.05f, size.width * 0.45f, size.height * 0.05f, size.width * 0.92f, size.height * 0.45f)
            }
            if (taper) {
                for (k in 0..3) {
                    drawPath(path, color, style = Stroke(width * (0.4f + k * 0.2f), cap = StrokeCap.Round), alpha = 0.35f)
                }
            }
            drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round))
        }
    }
}

@Immutable
private data class PenCardSettings(
    val penName: String,
    val paperSound: Boolean,
    val paperVolume: Float,
    val paperHaptics: Boolean,
)

@Immutable
private data class HighlighterCardSettings(
    val highlighterOpacity: Float,
    val highlighterMode: HighlighterMode,
    val highlighterBehind: Boolean,
)

@Immutable
private data class EraserCardSettings(
    val eraserMode: EraserMode,
    val scribbleSensitivity: ScribbleSensitivity,
    val eraseHighlighterOnly: Boolean,
    val eraserAutoSwitch: Boolean,
    val scribbleToErase: Boolean,
    val liveScribbleErase: Boolean,
)

@Immutable
private data class LassoCardSettings(
    val lassoMode: LassoMode,
    val lassoFilter: LassoFilter,
    val lassoTouching: Boolean,
    val circleToLasso: Boolean,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PenCardContent(vm: EditorViewModel, darkUi: Boolean, fgColor: Color) {
    val style = vm.penStyle
    val presets by vm.presets.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val s by remember(store) {
        store.select { PenCardSettings(it.penName, it.paperSound, it.paperVolume, it.paperHaptics) }
    }.collectAsStateWithLifecycle(
        initialValue = store.settings.value.let { PenCardSettings(it.penName, it.paperSound, it.paperVolume, it.paperHaptics) },
    )
    var editingPenName by remember { mutableStateOf(false) }
    var savingPreset by remember { mutableStateOf(false) }
    var renamingPreset by remember { mutableStateOf<PenPreset?>(null) }

    val tileBg = if (darkUi) Color(0xFF2C2C2C) else MaterialTheme.colorScheme.primaryContainer
    val tileIdleBg = if (darkUi) Color(0xFF232326) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val mutedFg = fgColor.copy(alpha = 0.62f)

    // Stylus profile header pill
    AppCard(
        onClick = { editingPenName = true },
        containerColor = tileIdleBg,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp), tint = ProButtonPalette.ElectricBlue)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("ACTIVE STYLUS", style = MaterialTheme.typography.labelSmall, color = mutedFg)
                Text(s.penName.ifBlank { "Pro Stylus" }, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = fgColor)
            }
            Text("Rename", style = MaterialTheme.typography.labelSmall, color = ProButtonPalette.ElectricBlue)
        }
    }

    StrokePreview(Color(style.color), style.size * vm.penType.sizeFactor * 2.2f, vm.penType != PenType.BALL, darkUi)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PenType.entries.forEach { type ->
            val sel = vm.penType == type
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (sel) tileBg else tileIdleBg)
                    .border(if (sel) 1.5.dp else 0.dp, if (sel) ProButtonPalette.ElectricBlue else Color.Transparent, RoundedCornerShape(14.dp))
                    .elasticStretch()
                    .clickable { vm.penType = type }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PenGlyph(type, fgColor, Color(style.color), Modifier.size(28.dp))
                Spacer(Modifier.height(4.dp))
                Text(type.label, style = MaterialTheme.typography.labelSmall, color = fgColor, maxLines = 1)
            }
        }
    }

    HorizontalDivider(color = fgColor.copy(alpha = 0.08f))
    SliderRow("THICKNESS", "%.1f pt".format(style.size), style.size, 0.5f..20f) { v ->
        val sizes = style.sizes.map { if (it == style.size) v else it }
        vm.updateStyle(Tool.PEN, style.copy(size = v, sizes = sizes))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("FAVOURITE PENS", style = MaterialTheme.typography.labelSmall, color = mutedFg, modifier = Modifier.weight(1f))
        TextButton(onClick = { savingPreset = true }) {
            Icon(Icons.Outlined.StarBorder, null, Modifier.size(16.dp), tint = ProButtonPalette.ElectricBlue)
            Spacer(Modifier.width(4.dp))
            Text("Save pen", color = ProButtonPalette.ElectricBlue)
        }
    }
    if (presets.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.forEach { p ->
                PresetChip(
                    p = p,
                    fgColor = fgColor,
                    onClick = { vm.applyPreset(p) },
                    onRename = { renamingPreset = p },
                    onRemove = { vm.removePreset(p) },
                )
            }
        }
    }

    HorizontalDivider(color = fgColor.copy(alpha = 0.08f))
    Text("SETTINGS", style = MaterialTheme.typography.labelSmall, color = mutedFg, letterSpacing = 0.8.sp)
    ToggleLine("Writing sound", s.paperSound, fgColor) { v -> store.update { it.copy(paperSound = v) } }
    if (s.paperSound) {
        SliderRow("VOLUME", "%.0f%%".format(s.paperVolume * 100), s.paperVolume, 0.05f..1f) { v ->
            store.update { it.copy(paperVolume = v) }
        }
    }
    ToggleLine("Paper haptics", s.paperHaptics, fgColor) { v -> store.update { it.copy(paperHaptics = v) } }

    if (editingPenName) {
        var name by remember(s.penName) { mutableStateOf(s.penName) }
        AppDialog(
            onDismissRequest = { editingPenName = false },
            title = { Text("Stylus name") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, shape = Pill, label = { Text("Pen display name") }) },
            confirmButton = { TextButton(onClick = { vm.setPenName(name); editingPenName = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editingPenName = false }) { Text("Cancel") } },
        )
    }

    if (savingPreset) {
        var label by remember { mutableStateOf("${vm.penType.label} ${style.size.roundToInt()}") }
        AppDialog(
            onDismissRequest = { savingPreset = false },
            title = { Text("Name this pen preset") },
            text = { OutlinedTextField(label, { label = it }, singleLine = true, shape = Pill) },
            confirmButton = { TextButton(onClick = { vm.savePreset(label); savingPreset = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { savingPreset = false }) { Text("Cancel") } },
        )
    }

    renamingPreset?.let { p ->
        var label by remember(p) { mutableStateOf(p.name) }
        AppDialog(
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
private fun PresetChip(p: PenPreset, fgColor: Color, onClick: () -> Unit, onRename: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .clip(Pill)
                .border(1.dp, fgColor.copy(alpha = 0.22f), Pill)
                .combinedClickable(onClick = onClick, onLongClick = { menu = true })
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PenGlyph(runCatching { PenType.valueOf(p.type) }.getOrDefault(PenType.FOUNTAIN), fgColor, Color(p.color), Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(width = 16.dp, height = (p.size / 2f).coerceIn(2f, 8f).dp).clip(Pill).background(Color(p.color)))
            if (p.name.isNotBlank()) {
                Spacer(Modifier.width(5.dp))
                Text(p.name, style = MaterialTheme.typography.labelSmall, color = fgColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        AppContextMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            items = listOf(
                AppMenuItem("Rename") { onRename() },
                AppMenuItem("Remove", destructive = true) { onRemove() },
            ),
        )
    }
}

@Composable
private fun HighlighterCardContent(vm: EditorViewModel, darkUi: Boolean, fgColor: Color) {
    val style = vm.highlighterStyle
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val s by remember(store) {
        store.select { HighlighterCardSettings(it.highlighterOpacity, it.highlighterMode, it.highlighterBehind) }
    }.collectAsStateWithLifecycle(
        initialValue = store.settings.value.let { HighlighterCardSettings(it.highlighterOpacity, it.highlighterMode, it.highlighterBehind) },
    )
    StrokePreview(Color(style.color).copy(alpha = s.highlighterOpacity), style.size * 1.2f, false, darkUi)
    SliderRow("THICKNESS", "%.0f".format(style.size), style.size, 6f..70f) { v ->
        val sizes = style.sizes.map { if (it == style.size) v else it }
        vm.updateStyle(Tool.HIGHLIGHTER, style.copy(size = v, sizes = sizes))
    }
    SliderRow("OPACITY", "%.0f%%".format(s.highlighterOpacity * 100), s.highlighterOpacity, 0.3f..0.9f) { v ->
        store.update { it.copy(highlighterOpacity = v) }
    }
    Text("LINE", style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.62f))
    Segments(HighlighterMode.entries.map { it.label }, s.highlighterMode.ordinal) { i ->
        store.update { it.copy(highlighterMode = HighlighterMode.entries[i]) }
    }
    ToggleLine("Draw behind ink", s.highlighterBehind, fgColor) { v -> store.update { it.copy(highlighterBehind = v) } }
}

@Composable
private fun EraserCardContent(vm: EditorViewModel) {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val fgColor = LocalContentColor.current
    val s by remember(store) {
        store.select {
            EraserCardSettings(it.eraserMode, it.scribbleSensitivity, it.eraseHighlighterOnly, it.eraserAutoSwitch, it.scribbleToErase, it.liveScribbleErase)
        }
    }.collectAsStateWithLifecycle(
        initialValue = store.settings.value.let {
            EraserCardSettings(it.eraserMode, it.scribbleSensitivity, it.eraseHighlighterOnly, it.eraserAutoSwitch, it.scribbleToErase, it.liveScribbleErase)
        },
    )
    Segments(EraserMode.entries.map { it.label }, s.eraserMode.ordinal) { i ->
        store.update { it.copy(eraserMode = EraserMode.entries[i]) }
    }
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        SizeDots(listOf(8f, 16f, 30f), listOf(8f, 16f, 30f)[vm.eraserSize], ring = true, fg = fgColor) { v ->
            vm.eraserSize = listOf(8f, 16f, 30f).indexOf(v)
        }
    }
    Text("SCRIBBLE SENSITIVITY", style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.62f))
    Segments(ScribbleSensitivity.entries.map { it.label }, s.scribbleSensitivity.ordinal) { i ->
        store.update { it.copy(scribbleSensitivity = ScribbleSensitivity.entries[i]) }
    }
    ToggleLine("Erase highlighter only", s.eraseHighlighterOnly, fgColor) { v -> store.update { it.copy(eraseHighlighterOnly = v) } }
    ToggleLine("Switch back after erasing", s.eraserAutoSwitch, fgColor) { v -> store.update { it.copy(eraserAutoSwitch = v) } }
    ToggleLine("Scribble to erase with the pen", s.scribbleToErase, fgColor) { v -> store.update { it.copy(scribbleToErase = v) } }
    ToggleLine("Erase mid-stroke while writing", s.liveScribbleErase, fgColor) { v -> store.update { it.copy(liveScribbleErase = v) } }
    HorizontalDivider(color = fgColor.copy(alpha = 0.08f))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .elasticStretch()
            .clickable { vm.clearPageInk() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CleaningServices, null, Modifier.size(17.dp), tint = Color(0xFFF28B82))
        Spacer(Modifier.width(8.dp))
        Text("Clear all ink on this page", style = MaterialTheme.typography.labelLarge, color = Color(0xFFF28B82))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LassoCardContent() {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val fgColor = LocalContentColor.current
    val s by remember(store) {
        store.select { LassoCardSettings(it.lassoMode, it.lassoFilter, it.lassoTouching, it.circleToLasso) }
    }.collectAsStateWithLifecycle(
        initialValue = store.settings.value.let { LassoCardSettings(it.lassoMode, it.lassoFilter, it.lassoTouching, it.circleToLasso) },
    )
    Segments(LassoMode.entries.map { it.label }, s.lassoMode.ordinal) { i -> store.update { it.copy(lassoMode = LassoMode.entries[i]) } }
    Text("SELECT", style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.62f))
    val f = s.lassoFilter
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AppChip(f.ink, { store.update { it.copy(lassoFilter = f.copy(ink = !f.ink)) } }, label = "Ink")
        AppChip(f.highlighter, { store.update { it.copy(lassoFilter = f.copy(highlighter = !f.highlighter)) } }, label = "Highlighter")
        AppChip(f.text, { store.update { it.copy(lassoFilter = f.copy(text = !f.text)) } }, label = "Text")
        AppChip(f.images, { store.update { it.copy(lassoFilter = f.copy(images = !f.images)) } }, label = "Images & stickers")
        AppChip(f.other, { store.update { it.copy(lassoFilter = f.copy(other = !f.other)) } }, label = "Notes & tape")
    }
    ToggleLine("Select anything the lasso touches", s.lassoTouching, fgColor) { v -> store.update { it.copy(lassoTouching = v) } }
    ToggleLine("Circle and hold with the pen", s.circleToLasso, fgColor) { v -> store.update { it.copy(circleToLasso = v) } }
}

@Composable
private fun LaserCardContent() {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val laserSeconds by remember(store) {
        store.select { it.laserSeconds }
    }.collectAsStateWithLifecycle(initialValue = store.settings.value.laserSeconds)
    SliderRow("STAYS FOR", "%.0f s".format(laserSeconds), laserSeconds, 1f..8f) { v ->
        store.update { it.copy(laserSeconds = v) }
    }
    Text(
        "Keep the pen or a finger on the screen to hold the laser pointer active; trails fade smoothly after release.",
        style = MaterialTheme.typography.labelSmall,
        color = LocalContentColor.current.copy(alpha = 0.7f),
    )
}

@Composable
private fun TextCardContent(vm: EditorViewModel) {
    SliderRow("DEFAULT SIZE", "%.0f".format(vm.textSize), vm.textSize, 16f..160f) { vm.textSize = it }
    Segments(
        TextFont.entries.take(4).map { it.label },
        TextFont.entries.indexOf(vm.textStyle.font).coerceAtMost(3),
    ) { i -> vm.textStyle = vm.textStyle.copy(font = TextFont.entries[i]) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MindMapCardContent(vm: EditorViewModel) {
    val fgColor = LocalContentColor.current
    val shapes = listOf("pill" to "Pill", "rect" to "Card", "diamond" to "Diamond", "circle" to "Oval")
    Text("NODE SHAPE", style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.62f))
    Segments(shapes.map { it.second }, shapes.indexOfFirst { it.first == vm.mindShape }.coerceAtLeast(0)) { i ->
        vm.mindShape = shapes[i].first
    }
    Text("NODE COLOUR", style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.62f))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        MindPalette.forEach { c ->
            Swatch(c, c == vm.mindColor, 20) { vm.recolorMindNode(color = c) }
        }
    }
    ToggleLine("Move child branch with node", vm.moveMindSubtree, fgColor) { vm.moveMindSubtree = it }
    Text("LAYOUT", style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.62f))
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
    AppPill(
        selected = true,
        tint = ProButtonPalette.ElectricBlue,
        onClick = { vm.buildMindMapFromNotes() },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Outlined.Hub, null, Modifier.size(16.dp), tint = Color.White)
        Spacer(Modifier.width(6.dp))
        Text("Build mind map from notes", style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
    Text(
        "Tap the canvas to add nodes, drag nodes to arrange, and double-tap a node to edit.",
        style = MaterialTheme.typography.labelSmall,
        color = fgColor.copy(alpha = 0.68f),
    )
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
    AppDialog(
        onDismissRequest = { vm.mindNodeEdit = null },
        title = { Text(if (edit.isNew) "New mind map node" else "Edit node") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    label,
                    { label = it },
                    label = { Text("Node title") },
                    placeholder = { Text("Enter concept or branch title") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    tag,
                    { tag = it.take(28) },
                    label = { Text("Tag (optional)") },
                    placeholder = { Text("e.g. Key idea, Exam, Formula") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    notes,
                    { notes = it },
                    label = { Text("Notes / Details") },
                    placeholder = { Text("Summary, example, or sub-detail shown on the card") },
                    minLines = 2,
                    maxLines = 4,
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
                TextButton(onClick = { vm.deleteMindNode(edit.item.id) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = { vm.mindNodeEdit = null }) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun ToggleLine(label: String, value: Boolean, fgColor: Color = LocalContentColor.current, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = fgColor)
        AppToggle(checked = value, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShapeCardContent(vm: EditorViewModel) {
    var fillEdit by remember { mutableStateOf(false) }
    val fgColor = LocalContentColor.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ShapeKind.entries.forEach { k ->
            val sel = vm.shapeKind == k
            Column(
                Modifier
                    .size(66.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (sel) ProButtonPalette.ElectricBlue.copy(alpha = 0.2f) else fgColor.copy(alpha = 0.08f))
                    .border(if (sel) 1.5.dp else 0.dp, if (sel) ProButtonPalette.ElectricBlue else Color.Transparent, RoundedCornerShape(16.dp))
                    .elasticStretch()
                    .clickable { vm.shapeKind = k },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ShapeGlyph(
                    k,
                    if (sel) ProButtonPalette.ElectricBlue else fgColor,
                    Modifier.size(26.dp),
                    fill = if (vm.shapeFillOn) Color(vm.shapeFillColor) else null,
                )
                Text(k.label, style = MaterialTheme.typography.labelSmall, color = fgColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    ToggleLine("Dashed line", vm.shapeDashed, fgColor) { vm.shapeDashed = it }
    ToggleLine("Fill", vm.shapeFillOn, fgColor) { vm.shapeFillOn = it }
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
    if (fillEdit) {
        ColorEditorDialog(vm.shapeFillColor, PenPalette, vm.recentColors(), onDismiss = { fillEdit = false }) { c ->
            vm.shapeFillColor = (c and 0x00FFFFFF) or (vm.shapeFillColor and 0xFF000000.toInt())
            fillEdit = false
        }
    }
}

@Composable
private fun SliderRow(label: String, value: String, v: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    AppSliderRow(
        title = label,
        valueText = value,
        value = v,
        valueRange = range,
        onValueChange = onChange,
    )
}

// ============================================================== stickers

@Composable
private fun StickerSheet(onPick: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var mine by remember { mutableStateOf(StickerLibrary.custom(context)) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                withContext(Dispatchers.IO) { StickerLibrary.import(context, uri) }
                mine = StickerLibrary.custom(context)
                tab = 2
            }
        }
    }
    fun pick(s: Sticker) = scope.launch {
        withContext(Dispatchers.Default) { StickerLibrary.bitmap(s) }?.let(onPick)
    }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScaleInAnimatedText(
                text = "Stickers",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            AppPill(
                selected = true,
                tint = ProButtonPalette.ElectricBlue,
                onClick = { importer.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Icon(Icons.Outlined.Add, null, Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("Import", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        AppSegmentedControl(
            options = listOf("Planner", "Labels", "My stickers"),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        val list: List<Sticker> = when (tab) {
            0 -> StickerLibrary.builtins
            1 -> StickerSet.map { Sticker.Emoji(it) }
            else -> mine
        }
        if (tab == 2 && mine.isEmpty()) {
            Text(
                "Import a picture to use it as a sticker. PNGs with transparency look best.",
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyVerticalGrid(
            GridCells.Adaptive(if (tab == 0) 96.dp else 72.dp),
            Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(vertical = 12.dp),
        ) {
            items(
                list.size,
                key = { idx ->
                    when (val item = list[idx]) {
                        is Sticker.Builtin -> "b:${item.id}"
                        is Sticker.Emoji -> "e:${item.text}"
                        is Sticker.Custom -> "c:${item.file.name}"
                    }
                },
            ) { i ->
                val st = list[i]
                val preview by produceState<Bitmap?>(null, st) {
                    value = withContext(Dispatchers.Default) { StickerLibrary.bitmap(st, 160) }
                }
                Box(
                    Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .elasticStretch()
                        .clickable { pick(st) },
                    contentAlignment = Alignment.Center,
                ) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextDialog(vm: EditorViewModel, edit: EditorViewModel.TextEdit) {
    val ex = edit.existing
    var text by remember(edit) { mutableStateOf(ex?.text ?: "") }
    var size by remember(edit) { mutableFloatStateOf(ex?.size ?: vm.textSize) }
    var color by remember(edit) { mutableIntStateOf(ex?.color ?: vm.textColor ?: vm.penStyle.color) }
    var style by remember(edit) { mutableStateOf(ex?.style ?: vm.textStyle) }
    var colorEdit by remember { mutableStateOf<Int?>(null) }

    AppDialog(
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
                    textAlign = when (style.align) {
                        TextAlign.LEFT -> androidx.compose.ui.text.style.TextAlign.Start
                        TextAlign.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
                        TextAlign.RIGHT -> androidx.compose.ui.text.style.TextAlign.End
                    },
                ).let { s ->
                    style.gradient?.let { g -> s.copy(brush = Brush.linearGradient(listOf(Color(color), Color(g)))) } ?: s.copy(color = Color(color))
                }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(14.dp)) {
                    Text(text.ifBlank { "Preview" }, style = previewStyle, modifier = Modifier.fillMaxWidth(), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                OutlinedTextField(text, { text = it }, minLines = 3, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppMenuButton(
                        role = SurfaceRole.Control,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        buttonContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(style.font.label, style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(16.dp))
                            }
                        },
                        items = TextFont.entries.map { f ->
                            AppMenuItem(f.label) { style = style.copy(font = f) }
                        },
                    )
                    Spacer(Modifier.weight(1f))
                    FmtToggle(Icons.Outlined.FormatBold, style.bold) { style = style.copy(bold = !style.bold) }
                    FmtToggle(Icons.Outlined.FormatItalic, style.italic) { style = style.copy(italic = !style.italic) }
                    FmtToggle(Icons.Outlined.FormatUnderlined, style.underline) { style = style.copy(underline = !style.underline) }
                    FmtToggle(Icons.AutoMirrored.Outlined.FormatAlignLeft, style.align == TextAlign.LEFT) {
                        style = style.copy(align = TextAlign.LEFT)
                    }
                    FmtToggle(Icons.Outlined.FormatAlignCenter, style.align == TextAlign.CENTER) {
                        style = style.copy(align = TextAlign.CENTER)
                    }
                    FmtToggle(Icons.AutoMirrored.Outlined.FormatAlignRight, style.align == TextAlign.RIGHT) {
                        style = style.copy(align = TextAlign.RIGHT)
                    }
                }
                SliderRow("SIZE", "%.0f".format(size), size, 16f..160f) { size = it }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    PenPalette.forEach { c -> Swatch(c, c == color, 20) { color = c } }
                    AddSwatchButton { colorEdit = 0 }
                }
                ToggleLine("Gradient", style.gradient != null) { on ->
                    style = style.copy(gradient = if (on) 0xFF3D6CF4.toInt() else null)
                }
                style.gradient?.let { g ->
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        PenPalette.forEach { c -> Swatch(c, c == g, 20) { style = style.copy(gradient = c) } }
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
            vm.pushRecent(c)
            colorEdit = null
        }
    }
}

@Composable
private fun FmtToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(2.dp)
            .size(36.dp)
            .clip(Pill)
            .background(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

// ============================================================== selection menu

@Composable
private fun SelectionMenu(
    vm: EditorViewModel,
    onDuplicate: () -> Unit,
    onColor: (Int) -> Unit,
    keep: (() -> Unit) -> Unit,
    onShareImage: () -> Unit,
) {
    var colors by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    val fg = MaterialTheme.colorScheme.onSurface
    AppSurface(
        role = SurfaceRole.Popover,
        shape = appPillShape(),
        shadowElevation = 10.dp,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            Row(Modifier.padding(horizontal = 2.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = { vm.startLinkFromSelection() },
                    shape = appPillShape(),
                    color = ProButtonPalette.ElectricBlue,
                    contentColor = Color.White,
                    modifier = Modifier.padding(4.dp).elasticStretch(),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Link, null, Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Link", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
                if (vm.selectionLinks().isNotEmpty()) {
                    Surface(
                        onClick = { vm.unlinkSelection() },
                        shape = appPillShape(),
                        color = Color(0xFFE5484D),
                        contentColor = Color.White,
                        modifier = Modifier.padding(4.dp).elasticStretch(),
                    ) {
                        Text(
                            "Unlink",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
                IconButton(onClick = vm::cutSelection, modifier = Modifier.elasticStretch()) { Icon(Icons.Outlined.ContentCut, "Cut") }
                IconButton(onClick = vm::copySelection, modifier = Modifier.elasticStretch()) { Icon(Icons.Outlined.ContentCopy, "Copy") }
                IconButton(onClick = onDuplicate, modifier = Modifier.elasticStretch()) { ProIconView(ProIcon.ADD, fg, Modifier.size(22.dp)) }
                IconButton(onClick = { vm.rainbowSelection() }, modifier = Modifier.elasticStretch()) {
                    Icon(Icons.Outlined.ColorLens, "Rainbow tint", tint = ProButtonPalette.MagentaPink)
                }
                IconButton(onClick = { colors = true }, modifier = Modifier.elasticStretch()) { Icon(Icons.Outlined.Palette, "Colour") }
                IconButton(onClick = { keep { vm.rotateSelection(90f) } }, modifier = Modifier.elasticStretch()) {
                    Icon(Icons.Outlined.RotateRight, "Rotate 90°")
                }
                Box {
                    IconButton(onClick = { moreOpen = true }, modifier = Modifier.elasticStretch()) {
                        ProIconView(ProIcon.MORE, fg, Modifier.size(22.dp))
                    }
                    AppContextMenu(
                        expanded = moreOpen,
                        onDismissRequest = { moreOpen = false },
                        items = listOf(
                            AppMenuItem("Flip horizontally", icon = { Icon(Icons.Outlined.Flip, null, Modifier.size(18.dp)) }) {
                                keep { vm.flipSelection() }
                            },
                            AppMenuItem("Bring to front", icon = { Icon(Icons.Outlined.FlipToFront, null, Modifier.size(18.dp)) }) {
                                keep { vm.arrange(true) }
                            },
                            AppMenuItem("Send to back", icon = { Icon(Icons.Outlined.FlipToBack, null, Modifier.size(18.dp)) }) {
                                keep { vm.arrange(false) }
                            },
                            AppMenuItem("Share as image", icon = { Icon(Icons.Outlined.IosShare, null, Modifier.size(18.dp)) }) {
                                onShareImage()
                            },
                            AppMenuItem("Make flashcard", icon = { Icon(Icons.Outlined.Style, null, Modifier.size(18.dp)) }) {
                                vm.cardFromSelection()
                            },
                            AppMenuItem("Add link…", icon = { Icon(Icons.Outlined.Link, null, Modifier.size(18.dp)) }) {
                                vm.startLinkFromSelection()
                            },
                            AppMenuItem("Save as screenshot", icon = { Icon(Icons.Outlined.Image, null, Modifier.size(18.dp)) }) {
                                vm.saveSelectionScreenshot()
                            },
                        ),
                    )
                }
                IconButton(onClick = vm::deleteSelection, modifier = Modifier.elasticStretch()) {
                    Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (colors) {
        ColorEditorDialog(0xFF1B1F2A.toInt(), PenPalette, vm.recentColors(), onDismiss = { colors = false }) {
            onColor(it)
            colors = false
        }
    }
}

// ============================================================== study timer

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun StudyTimer(
    vm: EditorViewModel,
    settings: AppSettings,
    area: IntSize,
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
    val owner = LocalLifecycleOwner.current

    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (e == Lifecycle.Event.ON_PAUSE) {
                vm.addStudy(unsaved)
                unsaved = 0L
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose {
            owner.lifecycle.removeObserver(obs)
            vm.addStudy(unsaved)
            unsaved = 0L
        }
    }

    LaunchedEffect(running, resumed, pomoMode) {
        while (running && resumed) {
            delay(1000)
            val idleMs = settings.studyIdleMinutes * 60_000L
            idle = idleMs > 0 && System.currentTimeMillis() - vm.lastActivity > idleMs
            if (idle) continue
            session += 1000
            unsaved += 1000
            if (pomoMode) {
                pomoRemMs = (pomoRemMs - 1000L).coerceAtLeast(0L)
                if (pomoRemMs <= 0L) {
                    running = false
                    pomoRemMs = settings.pomodoroMinutes.coerceIn(5, 90) * 60_000L
                }
            }
            if (unsaved >= 30_000) {
                vm.addStudy(unsaved)
                unsaved = 0L
            }
        }
    }

    fun fmt(ms: Long, secs: Boolean = settings.studySeconds): String {
        val s = ms / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
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
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .onSizeChanged { pill = it }
            .pointerInput(area) {
                detectDragGestures(
                    onDragEnd = {
                        val nx = ((x + pill.width / 2f) / area.width).coerceIn(0f, 1f)
                        val ny = ((y + pill.height / 2f) / area.height).coerceIn(0f, 1f)
                        pos = nx to ny
                        drag = Offset.Zero
                        vm.setTimerPos(nx, ny)
                    },
                    onDragCancel = { drag = Offset.Zero },
                ) { change, amount ->
                    change.consume()
                    drag += amount
                }
            },
    ) {
        AppSurface(
            role = SurfaceRole.Chrome,
            shape = appPillShape(),
            shadowElevation = 6.dp,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .elasticStretch()
                .combinedClickable(onClick = { running = !running }, onLongClick = { menu = true }),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Timer,
                    if (running) "Pause study timer" else "Start study timer",
                    Modifier.size(16.dp),
                    tint = if (running && !idle) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (pomoMode) "Pomo ${fmt(pomoRemMs, true)}" else fmt(session),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                )
                val state = when {
                    !running && session == 0L && !pomoMode -> "tap to start"
                    !running -> "paused"
                    idle -> "idle"
                    else -> null
                }
                if (state != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(state, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        val total = vm.studyTotal() + unsaved
        AppContextMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            items = listOf(
                AppMenuItem("This session: ${fmt(session, true)}", enabled = false) {},
                AppMenuItem("This notebook: ${fmt(total, true)}", enabled = false) {},
                AppMenuItem(if (running) "Pause" else "Start") { running = !running },
                AppMenuItem(if (pomoMode) "Switch to Stopwatch" else "Pomodoro (${settings.pomodoroMinutes}m)") {
                    pomoMode = !pomoMode
                    pomoRemMs = settings.pomodoroMinutes.coerceIn(5, 90) * 60_000L
                    running = true
                },
                AppMenuItem("Restart session") {
                    vm.addStudy(unsaved)
                    unsaved = 0L
                    session = 0L
                    pomoRemMs = settings.pomodoroMinutes.coerceIn(5, 90) * 60_000L
                    running = true
                },
                AppMenuItem("Move back to bottom-right") {
                    pos = 0.86f to 0.92f
                    vm.setTimerPos(0.86f, 0.92f)
                },
            ),
        )
    }
}

// ============================================================== tool wheel

@Composable
private fun ToolWheel(vm: EditorViewModel, at: Offset, onDismiss: () -> Unit) {
    val density = LocalDensity.current.density
    val tools = listOf(Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.LASSO, Tool.SHAPE, Tool.MINDMAP, Tool.TEXT, Tool.LASER, Tool.TAPE)
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f))
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .background(Color.Black.copy(alpha = 0.14f * appear.value))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        val radius = 92f * density
        val cx = at.x.coerceIn(radius + 40 * density, (size.width - radius - 40 * density).coerceAtLeast(radius + 40 * density))
        val cy = at.y.coerceIn(radius + 40 * density, (size.height - radius - 40 * density).coerceAtLeast(radius + 40 * density))
        AppSurface(
            role = SurfaceRole.Popover,
            shape = CircleShape,
            shadowElevation = 8.dp,
            modifier = Modifier
                .offset { IntOffset((cx - 34 * density).roundToInt(), (cy - 34 * density).roundToInt()) }
                .size(68.dp)
                .graphicsLayer { scaleX = appear.value; scaleY = appear.value },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ProIconView(ProIcon.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(18.dp))
            }
        }
        tools.forEachIndexed { i, t ->
            val a = -Math.PI / 2 + i * 2 * Math.PI / tools.size
            val r = radius * appear.value
            val x = cx + (r * cos(a)).toFloat()
            val y = cy + (r * sin(a)).toFloat()
            val sel = vm.tool == t
            Box(
                Modifier
                    .offset { IntOffset((x - 26 * density).roundToInt(), (y - 26 * density).roundToInt()) }
                    .size(52.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(if (sel) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.surface)
                    .clickable { vm.tool = t; onDismiss() },
                contentAlignment = Alignment.Center,
            ) {
                ToolGlyph(
                    t,
                    if (sel) Color.White else MaterialTheme.colorScheme.onSurface,
                    Color(vm.styleFor(t)?.color ?: vm.penStyle.color),
                    Modifier.size(24.dp),
                    active = sel,
                )
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
    AppDialog(
        onDismissRequest = { vm.webLinkEdit = null },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProIconView(ProIcon.WEBLINK, Color(color), Modifier.size(22.dp), ink = Color(color).copy(alpha = 0.25f))
                Spacer(Modifier.width(8.dp))
                Text(if (edit.isNew) "Add web link" else "Web link card", Modifier.weight(1f))
                if (!edit.isNew && url.isNotBlank()) {
                    TextButton(onClick = { vm.openExternalUrl(url) }) { Text("Open URL") }
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
            TextButton(enabled = url.trim().isNotBlank(), onClick = { vm.commitWebLink(url, linkTitle, note, color) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = { if (edit.isNew) vm.webLinkEdit = null else vm.deleteWebLink() }) {
                Text(
                    if (edit.isNew) "Cancel" else "Delete",
                    color = if (edit.isNew) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

// ============================================================== writable split view

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
    onClose: () -> Unit,
    modifier: Modifier,
) {
    val app = LocalContext.current.applicationContext as Application
    val vm2: EditorViewModel = viewModel(
        key = "split-$noteId",
        factory = viewModelFactory { initializer { EditorViewModel(app, noteId) } },
    )
    var splitZoom by remember(noteId) { mutableFloatStateOf(1f) }
    var splitPan by remember(noteId) { mutableStateOf(Offset.Zero) }
    var showSplitSearch by remember(noteId) { mutableStateOf(false) }
    var syncPage by remember(noteId) { mutableStateOf(false) }
    var writeMode by remember(noteId) { mutableStateOf(false) }
    var inkKind by remember(noteId) { mutableStateOf("pen") }
    var inkColor by remember(noteId) { mutableIntStateOf(0xFF1B1F2A.toInt()) }

    val listState = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()
    val visiblePage by remember(listState, vm2.pages.size) {
        derivedStateOf { listState.firstVisibleItemIndex.coerceIn(0, (vm2.pages.size - 1).coerceAtLeast(0)) }
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

    Column(modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.weight(1f)) {
                AppMenuButton(
                    role = SurfaceRole.Control,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    buttonContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.MenuBook, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text(vm2.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(16.dp))
                        }
                    },
                    items = others.map { (id, t) -> AppMenuItem(t) { onPick(id) } },
                )
            }

            Row(
                Modifier
                    .clip(Pill)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 2.dp, vertical = 2.dp),
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

            AppPill(
                selected = writeMode,
                tint = ProButtonPalette.ElectricBlue,
                onClick = { writeMode = !writeMode },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Icon(
                    if (writeMode) Icons.Outlined.Brush else Icons.Outlined.MenuBook,
                    null,
                    Modifier.size(13.dp),
                    tint = if (writeMode) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (writeMode) "Write" else "Read",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (writeMode) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }

            if (writeMode) {
                IconButton(enabled = vm2.canUndo, onClick = vm2::undo, modifier = Modifier.size(32.dp).elasticStretch()) {
                    Icon(Icons.AutoMirrored.Outlined.Undo, "Undo", Modifier.size(17.dp))
                }
            }

            AppMenuButton(
                role = SurfaceRole.Control,
                contentPadding = PaddingValues(6.dp),
                buttonContent = { Icon(Icons.Outlined.MoreHoriz, "Split Options", Modifier.size(18.dp)) },
                items = listOf(
                    AppMenuItem(
                        label = if (showSplitSearch) "Hide Search" else "Search in Note",
                        icon = { Icon(Icons.Outlined.Search, null, Modifier.size(18.dp)) },
                    ) {
                        showSplitSearch = !showSplitSearch
                        if (!showSplitSearch) vm2.clearSearch()
                    },
                    AppMenuItem(
                        label = "Insert Page Text to Note",
                        icon = { Icon(Icons.Outlined.PostAdd, null, Modifier.size(18.dp)) },
                    ) {
                        val extracted = vm2.extractPageSummaryText(visiblePage)
                        onInsertToPage(extracted.ifBlank { "Notes from ${vm2.title} (Page ${visiblePage + 1})" }, vm2.title)
                    },
                    AppMenuItem(
                        label = "Clip Page as Sticky",
                        icon = { Icon(Icons.Outlined.PushPin, null, Modifier.size(18.dp)) },
                    ) {
                        val extracted = vm2.extractPageSummaryText(visiblePage)
                        onClipToSticky(extracted.ifBlank { "Reference from ${vm2.title} (Page ${visiblePage + 1})" }, vm2.title)
                    },
                    AppMenuItem(
                        label = if (syncPage) "Sync Page Scrolling: On" else "Sync Page Scrolling: Off",
                        icon = { Icon(Icons.Outlined.SwapVert, null, Modifier.size(18.dp)) },
                    ) { syncPage = !syncPage },
                    AppMenuItem(
                        label = "Open Fullscreen",
                        icon = { Icon(Icons.Outlined.UnfoldMore, null, Modifier.size(18.dp)) },
                    ) { onOpen(noteId) },
                    AppMenuItem(
                        label = "Close Split View",
                        destructive = true,
                        icon = { Icon(Icons.Filled.Close, null, Modifier.size(18.dp)) },
                    ) { onClose() },
                ),
            )
        }

        AnimatedVisibility(writeMode) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("pen" to "Pen", "highlighter" to "Highlight", "eraser" to "Eraser").forEach { (k, label) ->
                    AppChip(selected = inkKind == k, onClick = { inkKind = k }, label = label)
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
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
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
                    Text("${vm2.activeHit + 1}/${vm2.searchHits.size}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp))
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f))
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
                                val event = awaitPointerEvent(PointerEventPass.Initial)
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
                                        pinching || abs(p.y) > abs(p.x) * 1.15f
                                    } else {
                                        pinching || abs(p.x) > abs(p.y) * 1.15f
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
                                    if (abs(splitZoom - 1f) > 0.08f) {
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
                    Box(
                        modifier = cardModifier
                            .shadow(2.dp, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .onSizeChanged { pageBoxSize = it },
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
                                    Canvas(Modifier.matchParentSize()) {
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
                        items(vm2.pages.size, key = { vm2.pages[it].id }) { i ->
                            SplitPageCard(i, vm2.pages[i], Modifier.fillParentMaxWidth(0.95f))
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = transformMod,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        items(vm2.pages.size, key = { vm2.pages[it].id }) { i ->
                            SplitPageCard(i, vm2.pages[i], Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}
