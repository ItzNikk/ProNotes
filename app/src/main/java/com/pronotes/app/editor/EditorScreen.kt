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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.pronotes.app.R
import com.pronotes.app.data.AppSettings
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.PenAction
import com.pronotes.app.data.Repository
import com.pronotes.app.data.ScrollDirection
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.ToolbarDock
import com.pronotes.app.data.ToolbarStyle
import com.pronotes.app.library.PdfSkeletonPreviewBox
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppContextMenu
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.AppMenuButton
import com.pronotes.app.ui.AppMenuItem
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.AppSegmentedControl
import com.pronotes.app.ui.AppSheet
import com.pronotes.app.ui.AppSliderRow
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.AppToggle
import com.pronotes.app.ui.ColorEditorDialog
import com.pronotes.app.ui.LocalReduceEffects
import com.pronotes.app.ui.LocalReduceMotion
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import com.pronotes.app.ui.ScaleInAnimatedText
import com.pronotes.app.ui.StickerSet
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.Swatch
import com.pronotes.app.ui.appBackdropBlur
import com.pronotes.app.ui.appCardShape
import com.pronotes.app.ui.appPillShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val Pill = CircleShape

// Workspace palette tokens for Dark and Paper Light themes
private val TabStripDarkBg = Color(0xFF1F3162)
private val TabStripLightBg = Color(0xFFE3EAF4)
private val TopBarDarkBg = Color(0xFF27507F)
private val TopBarLightBg = Color(0xFFD7E4F8)
private val WorkspaceDarkBg = Color(0xFF1C1C1C)
private val WorkspaceLightBg = Color(0xFFF3F1EA)
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
    var activePopoverTool by remember { mutableStateOf<Tool?>(null) }
    var quickInsert by remember { mutableStateOf<QuickInsertRequest?>(null) }
    var pendingInsertCoords by remember { mutableStateOf<Triple<Int, Float, Float>?>(null) }
    var splitId by remember { mutableStateOf<String?>(null) }
    var splitWeight by remember { mutableFloatStateOf(0.8f) }

    val pickBackground = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setPageBackground(uri)
    }
    val importFilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.importFilesIntoCurrent(uris)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val coords = pendingInsertCoords
        pendingInsertCoords = null
        if (uri != null) {
            if (coords != null) {
                vm.insertImage(uri, coords.first, coords.second, coords.third) { page, id -> canvas?.selectItems(page, setOf(id)) }
            } else {
                vm.insertImage(uri) { page, id -> canvas?.selectItems(page, setOf(id)) }
            }
        }
        vm.tool = Tool.LASSO
    }
    val exportPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch { context.contentResolver.openOutputStream(uri)?.use { vm.exportPdf(it) } }
    }

    LaunchedEffect(noteId) { store.openTab(noteId) }
    LaunchedEffect(library) {
        store.retainTabs(library.notebooks.filter { it.deletedAt == null }.map { it.id }.toSet())
    }
    LaunchedEffect(activePopoverTool, quickInsert, wheelAt) {
        vm.cardOpen = activePopoverTool != null || quickInsert != null || wheelAt != null
    }

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
            quickInsert != null -> quickInsert = null
            wheelAt != null -> wheelAt = null
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

    val isFixedBar = settings.toolbarStyle == ToolbarStyle.FIXED_BAR && settings.toolbarDock != ToolbarDock.FREE
    val reduceMotion = LocalReduceMotion.current
    val reduceEffects = LocalReduceEffects.current
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

    val selectDock: (ToolbarDock) -> Unit = { dock ->
        val defaultPos = when (dock) {
            ToolbarDock.LEFT -> "0.02,0.50"
            ToolbarDock.RIGHT -> "0.96,0.50"
            ToolbarDock.BOTTOM -> "0.50,0.92"
            else -> "0.50,0.03"
        }
        store.update {
            it.copy(
                toolbarDock = dock,
                toolbarPos = defaultPos,
                toolbarStyle = if (dock == ToolbarDock.FREE && it.toolbarStyle == ToolbarStyle.FIXED_BAR) ToolbarStyle.FLOATING_PILL else it.toolbarStyle,
            )
        }
    }
    val selectStyle: (ToolbarStyle) -> Unit = { nextStyle ->
        store.update {
            val nextDock = if (nextStyle == ToolbarStyle.FIXED_BAR && it.toolbarDock == ToolbarDock.FREE) ToolbarDock.TOP else it.toolbarDock
            it.copy(toolbarStyle = nextStyle, toolbarDock = nextDock)
        }
    }
    val resetToolbarPos: () -> Unit = {
        store.update { it.copy(toolbarDock = ToolbarDock.TOP, toolbarStyle = ToolbarStyle.FIXED_BAR, toolbarPos = "0.50,0.03") }
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
                                darkUi = darkUi,
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
                    if (!hideTools && isFixedBar && settings.toolbarDock == ToolbarDock.TOP) {
                        AdaptiveFixedToolbar(
                            vm = vm,
                            settings = settings,
                            darkUi = darkUi,
                            activePopoverTool = activePopoverTool,
                            onTogglePopover = { t -> activePopoverTool = if (activePopoverTool == t) null else t },
                            onSelectDock = selectDock,
                            onSelectStyle = selectStyle,
                            onResetPosition = resetToolbarPos,
                        )
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
                if (!hideTools && isFixedBar && settings.toolbarDock == ToolbarDock.LEFT) {
                    AdaptiveFixedToolbar(
                        vm = vm,
                        settings = settings,
                        darkUi = darkUi,
                        activePopoverTool = activePopoverTool,
                        onTogglePopover = { t -> activePopoverTool = if (activePopoverTool == t) null else t },
                        onSelectDock = selectDock,
                        onSelectStyle = selectStyle,
                        onResetPosition = resetToolbarPos,
                    )
                }

                var areaSize by remember { mutableStateOf(IntSize.Zero) }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onSizeChanged { areaSize = it },
                ) {
                    val overlayModalOpen = activePopoverTool != null || quickInsert != null || wheelAt != null
                    if (vm.loaded) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .appBackdropBlur(enabled = overlayModalOpen, radiusDp = 8.dp),
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
                                    override fun onWriteBoxDoubleTap(page: Int, item: WriteBoxItem) = vm.toggleWriteBox(page, item)
                                    override fun onPlaceWriteBox(page: Int, x: Float, y: Float, w: Float, h: Float) = vm.placeWriteBox(page, x, y, w, h)
                                    override fun onAutoConvertWriteBox(page: Int, boxId: Long) = vm.autoConvertWriteBox(page, boxId)
                                    override fun onSelectWriteBox(page: Int, boxId: Long?) = vm.selectWriteBox(page, boxId)
                                    override fun onHint(message: String) = vm.showHint(message)
                                    override fun onScribbleErased() = vm.onScribbleErased()
                                    override fun onEmptyPageLongPress(page: Int, x: Float, y: Float, screenX: Float, screenY: Float) {
                                        if (!vm.readOnly) {
                                            quickInsert = QuickInsertRequest(page, x, y, screenX, screenY)
                                        }
                                    }
                                }).also {
                                    it.renderer = vm.renderer()
                                    it.settings = settings
                                    it.tool = vm.tool
                                    canvas = it
                                }
                            },
                        )
                    }

                    val overlayFade by animateFloatAsState(
                        targetValue = if (vm.penActive) 0.14f else 1f,
                        animationSpec = if (reduceMotion) androidx.compose.animation.core.snap() else tween(260),
                        label = "ov",
                    )

                    // Top-Left Floating Undo / Redo + Reset view Pill when Fixed bar isn't hosting Undo/Redo
                    val showFloatingUndoPill = !focus && (!isFixedBar || hideTools || vm.currentPage?.isWhiteboard == true)
                    if (showFloatingUndoPill) {
                        val undoPillBg = if (darkUi) PopoverDarkBg else PopoverLightBg
                        val undoPillBorder = if (darkUi) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.14f)
                        val undoFg = if (darkUi) Color(0xFFD1D5DB) else Color(0xFF1F2937)
                        AppSurface(
                            role = SurfaceRole.Chrome,
                            containerColor = undoPillBg,
                            contentColor = undoFg,
                            shape = RoundedCornerShape(16.dp),
                            borderColor = undoPillBorder,
                            shadowElevation = if (reduceEffects) 0.dp else 6.dp,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 12.dp, top = 12.dp)
                                .graphicsLayer { alpha = overlayFade },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (!isFixedBar || hideTools) {
                                    BarIcon(
                                        icon = ProIcon.UNDO,
                                        label = stringResource(R.string.toolbar_undo),
                                        enabled = vm.canUndo,
                                        fgColor = undoFg,
                                        onClick = vm::undo,
                                    )
                                    BarIcon(
                                        icon = ProIcon.REDO,
                                        label = stringResource(R.string.toolbar_redo),
                                        enabled = vm.canRedo,
                                        fgColor = undoFg,
                                        onClick = vm::redo,
                                    )
                                }
                                if (vm.currentPage?.isWhiteboard == true) {
                                    if (!isFixedBar || hideTools) {
                                        Box(
                                            Modifier
                                                .padding(horizontal = 4.dp)
                                                .width(1.dp)
                                                .height(18.dp)
                                                .background(undoPillBorder),
                                        )
                                    }
                                    TextButton(
                                        onClick = { canvas?.resetWhiteboardView() },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                    ) {
                                        Text(stringResource(R.string.toolbar_reset_view), style = MaterialTheme.typography.labelSmall, color = ProButtonPalette.ElectricBlue)
                                    }
                                }
                            }
                        }
                    }

                    // Floating pill / Compact toolbar state & strict bounds clamping
                    var dockSize by remember { mutableStateOf(IntSize.Zero) }
                    var dockDragging by remember { mutableStateOf(false) }
                    var liveDragDelta by remember { mutableStateOf(Offset.Zero) }

                    val isVerticalDock = settings.toolbarDock == ToolbarDock.LEFT || settings.toolbarDock == ToolbarDock.RIGHT
                    val marginPx = (12f * density).roundToInt()
                    val undoClearPx = (144f * density).roundToInt()
                    val statusPadPx = with(LocalDensity.current) { WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx().roundToInt() }
                    val navPadPx = with(LocalDensity.current) { WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().toPx().roundToInt() }
                    val topSafePx = if (focus) statusPadPx + marginPx else marginPx
                    val bottomSafePx = navPadPx + marginPx

                    val parsedPos = remember(settings.toolbarPos) {
                        val parts = settings.toolbarPos.split(',')
                        val fx = parts.getOrNull(0)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.5f
                        val fy = parts.getOrNull(1)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.03f
                        fx to fy
                    }

                    val effDockW = remember(isVerticalDock, dockSize.width, density) {
                        if (isVerticalDock) (62f * density).roundToInt()
                        else if (dockSize.width > 0) dockSize.width else (340f * density).roundToInt()
                    }
                    val effDockH = remember(isVerticalDock, dockSize.height, areaSize.height, density) {
                        if (isVerticalDock) {
                            val measured = if (dockSize.height > 0) dockSize.height else (340f * density).roundToInt()
                            val maxAllowed = (areaSize.height - topSafePx - bottomSafePx).coerceAtLeast((180f * density).roundToInt())
                            measured.coerceAtMost(maxAllowed)
                        } else if (dockSize.height > 0) {
                            dockSize.height
                        } else {
                            (60f * density).roundToInt()
                        }
                    }

                    val pillSizeEst = IntSize(effDockW, effDockH)
                    val baseDockOffset = remember(areaSize, pillSizeEst, settings.toolbarDock, parsedPos, marginPx, topSafePx, bottomSafePx, undoClearPx) {
                        if (areaSize.width <= 0 || areaSize.height <= 0) {
                            IntOffset(undoClearPx, topSafePx)
                        } else {
                            val desired = when (settings.toolbarDock) {
                                ToolbarDock.TOP -> IntOffset(maxOf(undoClearPx, (areaSize.width - effDockW) / 2), topSafePx)
                                ToolbarDock.BOTTOM -> IntOffset((areaSize.width - effDockW) / 2, areaSize.height - effDockH - bottomSafePx)
                                ToolbarDock.LEFT -> IntOffset(marginPx, ((areaSize.height - effDockH) / 2).coerceAtLeast((72f * density).roundToInt()))
                                ToolbarDock.RIGHT -> IntOffset(areaSize.width - effDockW - marginPx, (areaSize.height - effDockH) / 2)
                                ToolbarDock.FREE -> return@remember ToolbarGeometry.normalizedToOffset(
                                    parsedPos.first, parsedPos.second, pillSizeEst, areaSize, marginPx, topSafePx, bottomSafePx,
                                )
                            }
                            ToolbarGeometry.clampFloatingOffset(desired.x, desired.y, pillSizeEst, areaSize, marginPx, topSafePx, bottomSafePx)
                        }
                    }

                    val activeDockOffset = remember(baseDockOffset, dockDragging, liveDragDelta, areaSize, pillSizeEst, marginPx, topSafePx, bottomSafePx) {
                        if (!dockDragging || areaSize.width <= 0) {
                            baseDockOffset
                        } else {
                            ToolbarGeometry.clampFloatingOffset(
                                desiredX = baseDockOffset.x + liveDragDelta.x.roundToInt(),
                                desiredY = baseDockOffset.y + liveDragDelta.y.roundToInt(),
                                pillSize = pillSizeEst,
                                areaSize = areaSize,
                                marginPx = marginPx,
                                topSafePx = topSafePx,
                                bottomSafePx = bottomSafePx,
                            )
                        }
                    }

                    if (!hideTools && !isFixedBar) {
                        FloatingToolbarPill(
                            vm = vm,
                            settings = settings,
                            darkUi = darkUi,
                            area = areaSize,
                            overlayFade = overlayFade,
                            activePopoverTool = activePopoverTool,
                            onTogglePopover = { t -> activePopoverTool = if (activePopoverTool == t) null else t },
                            onDragStart = {
                                dockDragging = true
                                liveDragDelta = Offset.Zero
                            },
                            onDragDelta = { delta -> liveDragDelta = delta },
                            onDragEnd = {
                                val finalPos = ToolbarGeometry.clampFloatingOffset(
                                    desiredX = baseDockOffset.x + liveDragDelta.x.roundToInt(),
                                    desiredY = baseDockOffset.y + liveDragDelta.y.roundToInt(),
                                    pillSize = pillSizeEst,
                                    areaSize = areaSize,
                                    marginPx = marginPx,
                                    topSafePx = topSafePx,
                                    bottomSafePx = bottomSafePx,
                                )
                                dockDragging = false
                                liveDragDelta = Offset.Zero
                                if (areaSize.width > 0 && areaSize.height > 0) {
                                    val centerX = (finalPos.x + effDockW / 2f) / areaSize.width
                                    val centerY = (finalPos.y + effDockH / 2f) / areaSize.height
                                    val availW = (areaSize.width - effDockW - marginPx * 2).coerceAtLeast(1).toFloat()
                                    val availH = (areaSize.height - effDockH - topSafePx - bottomSafePx).coerceAtLeast(1).toFloat()
                                    val nx = ((finalPos.x - marginPx) / availW).coerceIn(0f, 1f)
                                    val ny = ((finalPos.y - topSafePx) / availH).coerceIn(0f, 1f)
                                    val nextDock = when {
                                        centerX < 0.14f -> ToolbarDock.LEFT
                                        centerX > 0.86f -> ToolbarDock.RIGHT
                                        centerY < 0.12f -> ToolbarDock.TOP
                                        centerY > 0.88f -> ToolbarDock.BOTTOM
                                        else -> ToolbarDock.FREE
                                    }
                                    store.update {
                                        it.copy(
                                            toolbarDock = nextDock,
                                            toolbarPos = String.format(Locale.US, "%.3f,%.3f", nx, ny),
                                        )
                                    }
                                }
                            },
                            onSelectDock = selectDock,
                            onSelectStyle = selectStyle,
                            onResetPosition = resetToolbarPos,
                            modifier = Modifier
                                .offset { activeDockOffset }
                                .onSizeChanged { dockSize = it },
                        )
                    }

                    // Anchored Caret Tool Settings Sheet (anchors cleanly from Fixed Bar OR Floating Pill)
                    activePopoverTool?.let { popoverTool ->
                        var popoverSize by remember { mutableStateOf(IntSize.Zero) }
                        val popW = if (popoverSize.width > 0) popoverSize.width else (340f * density).roundToInt()
                        val popH = if (popoverSize.height > 0) popoverSize.height else (420f * density).roundToInt()
                        val gapPx = (8f * density).roundToInt()

                        val (caretSide, desiredPopX, desiredPopY) = if (isFixedBar) {
                            when (settings.toolbarDock) {
                                ToolbarDock.LEFT -> Triple(PopoverCaretSide.LEFT, marginPx, (areaSize.height - popH) / 2)
                                ToolbarDock.RIGHT -> Triple(PopoverCaretSide.RIGHT, areaSize.width - popW - marginPx, (areaSize.height - popH) / 2)
                                ToolbarDock.BOTTOM -> Triple(PopoverCaretSide.BOTTOM, (areaSize.width - popW) / 2, areaSize.height - popH - marginPx)
                                else -> Triple(PopoverCaretSide.TOP, (areaSize.width - popW) / 2, marginPx)
                            }
                        } else {
                            val dockCenterX = activeDockOffset.x + dockSize.width / 2
                            val dockCenterY = activeDockOffset.y + dockSize.height / 2
                            val placeRight = isVerticalDock && activeDockOffset.x < areaSize.width / 2
                            val placeLeft = isVerticalDock && !placeRight
                            val placeAbove = !isVerticalDock && dockCenterY > areaSize.height / 2
                            when {
                                placeRight -> Triple(PopoverCaretSide.LEFT, activeDockOffset.x + dockSize.width + gapPx, dockCenterY - popH / 2)
                                placeLeft -> Triple(PopoverCaretSide.RIGHT, activeDockOffset.x - popW - gapPx, dockCenterY - popH / 2)
                                placeAbove -> Triple(PopoverCaretSide.BOTTOM, dockCenterX - popW / 2, activeDockOffset.y - popH - gapPx)
                                else -> Triple(PopoverCaretSide.TOP, dockCenterX - popW / 2, activeDockOffset.y + dockSize.height + gapPx)
                            }
                        }

                        val popOffset = ToolbarGeometry.clampFloatingOffset(
                            desiredX = desiredPopX,
                            desiredY = desiredPopY,
                            pillSize = IntSize(popW, popH),
                            areaSize = areaSize,
                            marginPx = marginPx,
                            topSafePx = marginPx,
                            bottomSafePx = bottomSafePx,
                        )

                        Popup(
                            alignment = Alignment.TopStart,
                            offset = popOffset,
                            onDismissRequest = { activePopoverTool = null },
                            properties = PopupProperties(focusable = true),
                        ) {
                            Box(Modifier.onSizeChanged { popoverSize = it }) {
                                ToolPopoverCard(
                                    tool = popoverTool,
                                    vm = vm,
                                    darkUi = darkUi,
                                    caretSide = caretSide,
                                    onBrowseStickers = { showStickers = true },
                                    onPickImage = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    onSelectionConverted = { newId ->
                                        canvas?.selectItems(vm.selectionPage, setOf(newId))
                                    },
                                    onDismiss = { activePopoverTool = null },
                                )
                            }
                        }
                    }

                    // Floating Selection (Lasso) contextual menu
                    val sb = vm.selectionBounds
                    androidx.compose.animation.AnimatedVisibility(
                        visible = vm.selection.isNotEmpty() && sb != null,
                        enter = if (reduceMotion) fadeIn(androidx.compose.animation.core.snap()) else fadeIn() + scaleIn(initialScale = 0.85f),
                        exit = if (reduceMotion) fadeOut(androidx.compose.animation.core.snap()) else fadeOut() + scaleOut(targetScale = 0.85f),
                        modifier = Modifier.offset {
                            val b = sb ?: android.graphics.RectF()
                            val below = b.bottom + 18 * density
                            val y = if (below + 64 * density < areaSize.height) below else (b.top - 110 * density).coerceAtLeast(8f)
                            IntOffset(
                                b.left.roundToInt().coerceIn(8, (areaSize.width - 420 * density).roundToInt().coerceAtLeast(8)),
                                y.roundToInt().coerceIn(8, (areaSize.height - 64 * density).roundToInt().coerceAtLeast(8)),
                            )
                        },
                    ) {
                        SelectionMenu(
                            vm = vm,
                            areaSize = areaSize,
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
                            onConvertToText = { newId ->
                                canvas?.selectItems(vm.selectionPage, setOf(newId))
                            },
                        )
                    }

                    // Floating On-Canvas Write Box Studio Bar
                    val activeWb = vm.activeWriteBox()
                    if (activeWb != null && !focus) {
                        Box(
                            modifier = Modifier
                                .align(if (settings.toolbarDock == ToolbarDock.BOTTOM) Alignment.TopCenter else Alignment.BottomCenter)
                                .padding(
                                    top = if (settings.toolbarDock == ToolbarDock.BOTTOM) 14.dp else 0.dp,
                                    bottom = if (settings.toolbarDock == ToolbarDock.BOTTOM) 0.dp else 16.dp,
                                    start = 12.dp,
                                    end = 12.dp,
                                ),
                        ) {
                            WriteBoxStudioBar(
                                vm = vm,
                                box = activeWb,
                                pageIndex = vm.activeWriteBoxPage,
                                darkUi = darkUi,
                            )
                        }
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

                    // Toast / Undo snackbar banner
                    vm.toast?.let { msg ->
                        AppSurface(
                            role = SurfaceRole.Popover,
                            containerColor = Color(0xEB1B1F2A),
                            contentColor = Color.White,
                            shape = appPillShape(),
                            contentPadding = PaddingValues(
                                start = 18.dp,
                                end = if (vm.toastActionLabel != null) 10.dp else 18.dp,
                                top = 8.dp,
                                bottom = 8.dp,
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 112.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(msg, style = MaterialTheme.typography.labelLarge, color = Color.White)
                                val actionLabel = vm.toastActionLabel
                                val action = vm.toastAction
                                if (actionLabel != null && action != null) {
                                    TextButton(
                                        onClick = {
                                            action()
                                            vm.dismissToast()
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            actionLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color(0xFF93C5FD),
                                        )
                                    }
                                }
                            }
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
                    // Quick Insert (long-press on empty page)
                    quickInsert?.let { req ->
                        QuickInsertOverlay(
                            request = req,
                            areaSize = areaSize,
                            hasClipboard = vm.hasClipboard,
                            onDismiss = { quickInsert = null },
                            onWriteBox = {
                                quickInsert = null
                                vm.placeWriteBox(req.page, req.x - 240f, req.y - 120f, 480f, 240f)
                            },
                            onTextBox = {
                                quickInsert = null
                                vm.textEdit = EditorViewModel.TextEdit(req.page, req.x, req.y, null)
                            },
                            onSticky = {
                                quickInsert = null
                                vm.openSticky(req.page, req.x, req.y, null)
                            },
                            onWebLink = {
                                quickInsert = null
                                vm.openWebLink(req.page, req.x, req.y, null)
                            },
                            onImage = {
                                pendingInsertCoords = Triple(req.page, req.x, req.y)
                                quickInsert = null
                                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onSticker = {
                                pendingInsertCoords = Triple(req.page, req.x, req.y)
                                quickInsert = null
                                showStickers = true
                            },
                            onPaste = {
                                quickInsert = null
                                vm.pasteAt(req.page, req.x, req.y)?.let { (pg, ids) -> canvas?.selectItems(pg, ids) }
                            },
                        )
                    }
                }

                if (!hideTools && isFixedBar && settings.toolbarDock == ToolbarDock.RIGHT) {
                    AdaptiveFixedToolbar(
                        vm = vm,
                        settings = settings,
                        darkUi = darkUi,
                        activePopoverTool = activePopoverTool,
                        onTogglePopover = { t -> activePopoverTool = if (activePopoverTool == t) null else t },
                        onSelectDock = selectDock,
                        onSelectStyle = selectStyle,
                        onResetPosition = resetToolbarPos,
                    )
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

            if (!hideTools && isFixedBar && settings.toolbarDock == ToolbarDock.BOTTOM) {
                AdaptiveFixedToolbar(
                    vm = vm,
                    settings = settings,
                    darkUi = darkUi,
                    activePopoverTool = activePopoverTool,
                    onTogglePopover = { t -> activePopoverTool = if (activePopoverTool == t) null else t },
                    onSelectDock = selectDock,
                    onSelectStyle = selectStyle,
                    onResetPosition = resetToolbarPos,
                )
            }
        }

        // Quick Actions radial popup (stylus button click while hovering)
        wheelAt?.let { at -> QuickActionsOverlay(vm, at, onDismiss = { wheelAt = null }) }
    }

    // ------------------------------------------------------------------ dialogs & sheets
    if (vm.keyboardMode) KeyboardNoteModeOverlay(vm, darkUi)
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
        AppSheet(onDismissRequest = {
            showStickers = false
            pendingInsertCoords = null
            vm.tool = Tool.LASSO
        }) {
            StickerSheet { bmp ->
                val coords = pendingInsertCoords
                pendingInsertCoords = null
                showStickers = false
                vm.tool = Tool.LASSO
                if (coords != null) {
                    vm.insertStickerBitmap(bmp, coords.first, coords.second, coords.third) { pg, id -> canvas?.selectItems(pg, setOf(id)) }
                } else {
                    vm.insertStickerBitmap(bmp) { pg, id -> canvas?.selectItems(pg, setOf(id)) }
                }
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
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (active) activeBg else Color.Transparent)
            .semantics { contentDescription = label }
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
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
    darkUi: Boolean,
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
        BarIcon(ProIcon.KEYBOARD, "Keyboard Mode", active = vm.keyboardMode, fgColor = fgColor, onClick = { vm.toggleKeyboardMode() })

        Text(
            vm.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = fgColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClickLabel = "Rename notebook", onClick = onRename)
                .padding(horizontal = 10.dp, vertical = 12.dp),
        )

        Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(22.dp).background(dividerColor))

        AppMenuButton(
            contentDescription = "Add page or template",
            containerColor = ProButtonPalette.Emerald.copy(alpha = if (darkUi) 0.24f else 0.16f),
            contentPadding = PaddingValues(7.dp),
            buttonContent = {
                ProIconView(ProIcon.ADD, if (darkUi) Color(0xFF6EE7B7) else Color(0xFF047857), Modifier.size(20.dp))
            },
            items = listOf(
                AppMenuItem("New page after this") { vm.addPage() },
                AppMenuItem("New page at the end") { vm.addPage(atEnd = true) },
                AppMenuItem("Add Write Box") { vm.addWriteBox() },
                AppMenuItem("Infinite whiteboard page") { vm.addPage(paper = PaperStyle.DOTTED, isWhiteboard = true) },
                AppMenuItem("Choose a template…") { onTemplates() },
            ),
        )
        Spacer(Modifier.width(3.dp))
        AppMenuButton(
            contentDescription = "Share and export",
            containerColor = ProButtonPalette.SkyCyan.copy(alpha = if (darkUi) 0.24f else 0.16f),
            contentPadding = PaddingValues(7.dp),
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
                    if (vm.keyboardMode) "Exit Keyboard Mode" else "Keyboard Mode (Full Page)",
                    icon = { ProIconView(ProIcon.KEYBOARD, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp), ink = if (vm.keyboardMode) ProButtonPalette.ElectricBlue else null) },
                ) { vm.toggleKeyboardMode() },
            )
            add(
                AppMenuItem(
                    "Place Write Box",
                    icon = { ProIconView(ProIcon.WRITE_BOX, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp), ink = ProButtonPalette.ElectricBlue) },
                ) {
                    vm.tool = Tool.TEXT
                    vm.addWriteBox(vm.pageIndex)
                },
            )
            add(
                AppMenuItem(
                    "Flashcards",
                    icon = { ProIconView(ProIcon.FLASHCARDS, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp), ink = ProButtonPalette.RoyalBlue.copy(alpha = 0.22f)) },
                ) { onCards() },
            )
            add(
                AppMenuItem(
                    "Auto-generate flashcards from notes",
                    icon = { ProIconView(ProIcon.FLASHCARDS, ProButtonPalette.WarmAmber, Modifier.size(18.dp)) },
                ) {
                    vm.autoGenerateFlashcards()
                    onCards()
                },
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
                    "Cycle UI Colour Scheme (${settings.uiColorScheme.label})",
                    icon = { ProIconView(ProIcon.PALETTE, ProButtonPalette.MagentaPink, Modifier.size(18.dp)) },
                ) {
                    val schemes = com.pronotes.app.data.UiColorScheme.entries
                    val nextScheme = schemes[(settings.uiColorScheme.ordinal + 1) % schemes.size]
                    store.update { it.copy(uiColorScheme = nextScheme) }
                    vm.toast = "UI Colour Scheme: ${nextScheme.label}"
                },
            )
            add(
                AppMenuItem(
                    if (vm.currentPage?.isWhiteboard == true) "Switch to standard page" else "Infinite whiteboard",
                    icon = { Icon(Icons.Outlined.AllInclusive, null, Modifier.size(18.dp)) },
                ) { vm.toggleCurrentWhiteboard() },
            )
            add(
                AppMenuItem(
                    "Import PDF, Word, PPT, Sheets, EPUB…",
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
            buttonContent = {
                ProIconView(ProIcon.MORE, if (darkUi) Color(0xFFFCD34D) else Color(0xFFB45309), Modifier.size(20.dp))
            },
            items = moreEntries,
        )
    }
}

// ============================================================== Docked sub-tool bar

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
            thumb?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) } ?: run {
                val shimmer by rememberInfiniteTransition(label = "thumbShimmer").animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(1150, easing = LinearEasing), RepeatMode.Restart),
                    label = "sweep",
                )
                PdfSkeletonPreviewBox(shimmer = shimmer, modifier = Modifier.fillMaxSize())
            }
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
private fun WriteBoxStudioBar(
    vm: EditorViewModel,
    box: WriteBoxItem,
    pageIndex: Int,
    darkUi: Boolean,
) {
    val barBg = if (darkUi) Color(0xF5161B26) else Color(0xFAFFFFFF)
    val barFg = if (darkUi) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    var textValue by remember(box.id, box.convertedText) { mutableStateOf(box.convertedText) }

    AppSurface(
        role = SurfaceRole.Chrome,
        containerColor = barBg,
        contentColor = barFg,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 12.dp,
        borderColor = ProButtonPalette.ElectricBlue.copy(alpha = 0.55f),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = Modifier.widthIn(max = 680.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ProIconView(ProIcon.WRITE_BOX, ProButtonPalette.ElectricBlue, Modifier.size(18.dp), ink = ProButtonPalette.ElectricBlue)
                Text(
                    "Write Box",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = barFg,
                )
                Text(
                    if (box.isConverted) "• Converted" else "• Writing Ink (auto-converts)",
                    style = MaterialTheme.typography.labelSmall,
                    color = ProButtonPalette.ElectricBlue,
                )
                Spacer(Modifier.weight(1f))
                AppPill(
                    selected = box.isConverted,
                    tint = ProButtonPalette.ElectricBlue,
                    onClick = { vm.toggleWriteBox(pageIndex, box) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        if (box.isConverted) "Show Ink" else "Convert Now",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (box.isConverted) Color.White else barFg,
                    )
                }
                AppPill(
                    selected = true,
                    tint = ProButtonPalette.Emerald,
                    onClick = { vm.flattenWriteBoxToText(pageIndex, box.id) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        "Place Text",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
                IconButton(
                    onClick = { vm.deleteWriteBox(pageIndex, box.id) },
                    modifier = Modifier.size(28.dp),
                ) {
                    ProIconView(ProIcon.TRASH, MaterialTheme.colorScheme.error, Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { vm.selectWriteBox(pageIndex, null) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close bar", tint = barFg.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        vm.updateWriteBoxText(pageIndex, box.id, it)
                    },
                    placeholder = { Text("Write inside the box on page or type here…", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                )
                AppPill(
                    onClick = {
                        val nextSize = (box.fontSize - 3f).coerceAtLeast(16f)
                        vm.updateWriteBoxText(pageIndex, box.id, textValue, fontSize = nextSize)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Text("A-", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                AppPill(
                    onClick = {
                        val nextSize = (box.fontSize + 3f).coerceAtMost(64f)
                        vm.updateWriteBoxText(pageIndex, box.id, textValue, fontSize = nextSize)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Text("A+", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                val nextFont = TextFont.entries[(TextFont.entries.indexOf(box.font) + 1) % 4]
                AppPill(
                    onClick = {
                        vm.updateWriteBoxText(pageIndex, box.id, textValue, font = nextFont)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(box.font.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
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
    AppDialog(
        onDismissRequest = { vm.mindNodeEdit = null },
        title = { Text(if (edit.isNew) "New concept map node" else "Edit node") },
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
    var fontSize by remember(edit) { mutableFloatStateOf(ex?.size ?: vm.textSize) }
    var color by remember(edit) { mutableIntStateOf(ex?.color ?: vm.textColor ?: vm.penStyle.color) }
    var style by remember(edit) { mutableStateOf(ex?.style ?: vm.textStyle) }
    var colorEdit by remember { mutableStateOf<Int?>(null) }
    // Mode 0: Write Box Pad + Live Preview, Mode 1: Direct Keyboard Editor
    var inputMode by remember { mutableIntStateOf(0) }
    val padStrokes = remember { androidx.compose.runtime.mutableStateListOf<List<Offset>>() }
    var activeStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    fun convertPadStrokes() {
        if (padStrokes.isEmpty()) return
        val strokeCoordLists: List<List<Pair<Float, Float>>> = padStrokes.map { pts ->
            pts.map { pt -> pt.x to pt.y }
        }
        val recognized = vm.recognizeHandwritingStrokesToText(strokeCoordLists)
        if (recognized.isNotBlank()) {
            text = if (text.isBlank()) recognized else "$text $recognized"
        }
        padStrokes.clear()
        activeStroke = emptyList()
    }

    AppDialog(
        onDismissRequest = { vm.textEdit = null },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProIconView(ProIcon.WRITE_BOX, ProButtonPalette.ElectricBlue, Modifier.size(20.dp), ink = ProButtonPalette.ElectricBlue)
                Text(if (ex != null) "Edit Text / Write Box" else "Write Box & Text Box")
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppSegmentedControl(
                    options = listOf("Write Box Pad", "Keyboard Box"),
                    selectedIndex = inputMode,
                    onSelect = { inputMode = it },
                    modifier = Modifier.fillMaxWidth(),
                )

                val previewStyle = TextStyle(
                    fontSize = (fontSize / 2f).coerceIn(13f, 34f).sp,
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

                // Live converted text box at top (editable with keyboard at any time)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = previewStyle,
                    minLines = 2,
                    maxLines = 5,
                    label = { Text("Converted / Typed Text (editable)") },
                    placeholder = { Text("Write in the ruled box below or type with keyboard…") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                )

                if (inputMode == 0) {
                    // Ruled handwriting pad with instant conversion
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFFCF5))
                            .border(1.5.dp, ProButtonPalette.ElectricBlue.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ProButtonPalette.ElectricBlue.copy(alpha = 0.10f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "HANDWRITING PAD — write here with stylus or finger",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ProButtonPalette.ElectricBlue,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = {
                                    text = "$text "
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            ) { Text("Space", fontSize = 11.sp) }
                            TextButton(
                                onClick = {
                                    text = if (text.isEmpty()) "\n" else "$text\n"
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            ) { Text("Enter ↵", fontSize = 11.sp) }
                            TextButton(
                                onClick = {
                                    if (padStrokes.isNotEmpty()) {
                                        padStrokes.removeAt(padStrokes.lastIndex)
                                    } else if (text.isNotEmpty()) {
                                        text = text.dropLast(1)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            ) { Text("⌫", fontSize = 12.sp) }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        var pts = mutableListOf<Offset>()
                                        while (true) {
                                            val ev = awaitPointerEvent()
                                            val ch = ev.changes.firstOrNull() ?: break
                                            if (ch.pressed) {
                                                pts.add(ch.position)
                                                activeStroke = pts.toList()
                                                ch.consume()
                                            } else {
                                                ch.consume()
                                                break
                                            }
                                        }
                                        if (pts.size >= 2) {
                                            padStrokes.add(pts.toList())
                                        }
                                        activeStroke = emptyList()
                                    }
                                },
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                val canvasW = this.size.width
                                val canvasH = this.size.height
                                // Ruled baseline guides
                                val lineGap = canvasH / 3.2f
                                for (i in 1..2) {
                                    val y = lineGap * i
                                    drawLine(
                                        color = Color(0xFF93B4E8).copy(alpha = 0.45f),
                                        start = Offset(16.dp.toPx(), y),
                                        end = Offset(canvasW - 16.dp.toPx(), y),
                                        strokeWidth = 1.2.dp.toPx(),
                                    )
                                }
                                // Margin line
                                drawLine(
                                    color = Color(0xFFE57373).copy(alpha = 0.38f),
                                    start = Offset(28.dp.toPx(), 0f),
                                    end = Offset(28.dp.toPx(), canvasH),
                                    strokeWidth = 1.dp.toPx(),
                                )
                                val strokeColor = Color(color)
                                fun drawPts(pts: List<Offset>) {
                                    if (pts.size < 2) return
                                    val path = Path().apply {
                                        moveTo(pts.first().x, pts.first().y)
                                        for (i in 1 until pts.size) {
                                            val prev = pts[i - 1]
                                            val cur = pts[i]
                                            quadraticTo(prev.x, prev.y, (prev.x + cur.x) * 0.5f, (prev.y + cur.y) * 0.5f)
                                        }
                                    }
                                    drawPath(path, strokeColor, style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round))
                                }
                                padStrokes.forEach { drawPts(it) }
                                drawPts(activeStroke)
                            }
                            if (padStrokes.isEmpty() && activeStroke.isEmpty()) {
                                Text(
                                    "Write words here — tap 'Convert Strokes' or 'Done' to turn ink into neat text",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF7A869A),
                                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppPill(
                                selected = padStrokes.isNotEmpty(),
                                tint = ProButtonPalette.ElectricBlue,
                                onClick = { convertPadStrokes() },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    "Convert Strokes → Text",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (padStrokes.isNotEmpty()) Color.White else Color(0xFF475569),
                                )
                            }
                            TextButton(onClick = { padStrokes.clear(); activeStroke = emptyList() }) {
                                Text("Clear Pad", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    // Quick formatting chips for keyboard mode
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        AppChip(false, { text = if (text.isEmpty() || text.endsWith("\n")) "${text}• " else "$text\n• " }, label = "• Bullet")
                        AppChip(false, { text = if (text.isEmpty() || text.endsWith("\n")) "${text}☐ " else "$text\n☐ " }, label = "☐ Checklist")
                        AppChip(false, { text = if (text.isEmpty() || text.endsWith("\n")) "${text}1. " else "$text\n1. " }, label = "1. Numbered")
                    }
                }

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
                SliderRow("SIZE", "%.0f".format(fontSize), fontSize, 16f..160f) { fontSize = it }
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
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = {
                        if (padStrokes.isNotEmpty()) convertPadStrokes()
                        vm.addWriteBox(vm.pageIndex, edit.x, edit.y)
                        val latestBox = vm.currentPage?.items?.filterIsInstance<WriteBoxItem>()?.lastOrNull()
                        if (latestBox != null && text.isNotBlank()) {
                            vm.updateWriteBoxText(vm.pageIndex, latestBox.id, text, style.font, fontSize)
                        }
                        vm.textEdit = null
                    },
                ) { Text("Place as Ruled Box") }
                TextButton(
                    onClick = {
                        if (padStrokes.isNotEmpty()) convertPadStrokes()
                        vm.commitText(text, fontSize, color, style)
                    },
                ) { Text("Done", fontWeight = FontWeight.Bold) }
            }
        },
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

/**
 * Full-page Keyboard Note Mode overlay — allows users to type structured document notes
 * directly onto the current page using the hardware or on-screen keyboard, complete with
 * headings, checklists, bullet lists, font families, and automatic page persistence.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyboardNoteModeOverlay(vm: EditorViewModel, darkUi: Boolean) {
    val page = vm.currentPage
    var bodyText by remember(page?.id) { mutableStateOf(vm.getKeyboardPageText()) }
    var fontSize by remember { mutableFloatStateOf(vm.textSize.coerceIn(22f, 64f)) }
    var style by remember { mutableStateOf(vm.textStyle) }
    var textColor by remember { mutableIntStateOf(vm.textColor ?: (if (darkUi) 0xFFF8FAFC.toInt() else 0xFF1A1C1E.toInt())) }

    val panelBg = if (darkUi) Color(0xF5161820) else Color(0xFAFCFBF7)
    val fg = if (darkUi) Color(0xFFF3F5F8) else Color(0xFF181C23)
    val borderCol = if (darkUi) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 74.dp, bottom = 18.dp, start = 18.dp, end = 18.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
                .background(panelBg)
                .border(1.5.dp, borderCol, RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header & quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProIconView(ProIcon.KEYBOARD, ProButtonPalette.ElectricBlue, Modifier.size(22.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Keyboard Note Mode · Page ${vm.pageIndex + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = fg,
                    )
                    Text(
                        "Type full-page notes with keyboard; syncs directly to the canvas page",
                        style = MaterialTheme.typography.labelSmall,
                        color = fg.copy(alpha = 0.65f),
                    )
                }
                TextButton(
                    onClick = {
                        vm.saveKeyboardPageText(bodyText)
                    },
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold, color = ProButtonPalette.ElectricBlue)
                }
                AppPill(
                    selected = true,
                    tint = ProButtonPalette.ElectricBlue,
                    onClick = {
                        vm.saveKeyboardPageText(bodyText)
                        vm.keyboardMode = false
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("Done", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Formatting toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AppChip(false, {
                    bodyText = if (bodyText.isEmpty() || bodyText.endsWith("\n")) "${bodyText}# " else "$bodyText\n# "
                }, label = "H1")
                AppChip(false, {
                    bodyText = if (bodyText.isEmpty() || bodyText.endsWith("\n")) "${bodyText}## " else "$bodyText\n## "
                }, label = "H2")
                AppChip(false, {
                    bodyText = if (bodyText.isEmpty() || bodyText.endsWith("\n")) "${bodyText}• " else "$bodyText\n• "
                }, label = "• Bullet")
                AppChip(false, {
                    bodyText = if (bodyText.isEmpty() || bodyText.endsWith("\n")) "${bodyText}☐ " else "$bodyText\n☐ "
                }, label = "☐ Task")
                AppChip(false, {
                    bodyText = if (bodyText.isEmpty() || bodyText.endsWith("\n")) "${bodyText}☑ " else "$bodyText\n☑ "
                }, label = "☑ Done")
                AppChip(false, {
                    bodyText = if (bodyText.isEmpty() || bodyText.endsWith("\n")) "${bodyText}1. " else "$bodyText\n1. "
                }, label = "1. List")
                FmtToggle(Icons.Outlined.FormatBold, style.bold) {
                    style = style.copy(bold = !style.bold)
                    vm.textStyle = style
                }
                FmtToggle(Icons.Outlined.FormatItalic, style.italic) {
                    style = style.copy(italic = !style.italic)
                    vm.textStyle = style
                }
                FmtToggle(Icons.Outlined.FormatUnderlined, style.underline) {
                    style = style.copy(underline = !style.underline)
                    vm.textStyle = style
                }
                TextFont.entries.take(4).forEach { f ->
                    AppChip(style.font == f, {
                        style = style.copy(font = f)
                        vm.textStyle = style
                    }, label = f.label)
                }
            }

            // Color & size strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    PenPalette.take(8).forEach { c ->
                        Swatch(c, c == textColor, 18) {
                            textColor = c
                            vm.textColor = c
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("${fontSize.roundToInt()} pt", style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.75f))
                TextButton(onClick = { fontSize = (fontSize - 2f).coerceAtLeast(18f); vm.textSize = fontSize }) { Text("A−") }
                TextButton(onClick = { fontSize = (fontSize + 2f).coerceAtMost(72f); vm.textSize = fontSize }) { Text("A+") }
            }

            // Main full-page keyboard editor
            OutlinedTextField(
                value = bodyText,
                onValueChange = {
                    bodyText = it
                    vm.saveKeyboardPageText(it)
                },
                placeholder = {
                    Text(
                        "Start typing your notes here with the keyboard…\n• Use • Bullet, ☐ Task, or H1/H2 above\n• Everything you type is saved directly onto Page ${vm.pageIndex + 1} so you can also annotate over it with the stylus!",
                        color = fg.copy(alpha = 0.45f),
                    )
                },
                textStyle = TextStyle(
                    fontSize = (fontSize * 0.55f).coerceIn(14f, 28f).sp,
                    lineHeight = ((fontSize * 0.55f).coerceIn(14f, 28f) * 1.45f).sp,
                    fontFamily = style.font.compose(),
                    fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = if (style.underline) TextDecoration.Underline else null,
                    color = Color(textColor),
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
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
                IconButton(onClick = onSwap, modifier = Modifier.size(28.dp)) {
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
                IconButton(enabled = vm2.canUndo, onClick = vm2::undo, modifier = Modifier.size(32.dp)) {
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
                                ?: run {
                                    val shimmer by rememberInfiniteTransition(label = "splitPdfShimmer").animateFloat(
                                        initialValue = 0f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(tween(1150, easing = LinearEasing), RepeatMode.Restart),
                                        label = "sweep",
                                    )
                                    PdfSkeletonPreviewBox(
                                        shimmer = shimmer,
                                        modifier = Modifier.fillMaxWidth().aspectRatio((page.width / page.height).coerceIn(0.45f, 1.8f)),
                                    )
                                }

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
