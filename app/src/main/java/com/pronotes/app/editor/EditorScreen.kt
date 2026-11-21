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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AllInclusive
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
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PostAdd
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
    DisposableEffect(canvas, vm) {
        val target = canvas
        target?.setOnDragListener { _, event ->
            when (event.action) {
                android.view.DragEvent.ACTION_DRAG_STARTED -> !vm.readOnly && (event.localState is Sticker || event.clipDescription?.hasMimeType("application/vnd.pronotes.sticker") == true || event.clipDescription?.hasMimeType("image/*") == true)
                android.view.DragEvent.ACTION_DRAG_LOCATION -> { target.previewDrop(event.x, event.y); true }
                android.view.DragEvent.ACTION_DRAG_EXITED, android.view.DragEvent.ACTION_DRAG_ENDED -> {
                    target.clearDropPreview()
                    true
                }
                android.view.DragEvent.ACTION_DROP -> {
                    target.clearDropPreview()
                    val point = target.dropPoint(event.x, event.y)
                    if (point == null || vm.readOnly) false else {
                        val sticker = event.localState as? Sticker ?: if (event.clipDescription.hasMimeType("application/vnd.pronotes.sticker")) {
                            val code = event.clipData.getItemAt(0).text?.toString().orEmpty()
                            when {
                                code.startsWith("b:") -> StickerLibrary.builtins.find { it.id == code.drop(2) }
                                code.startsWith("c:") -> StickerLibrary.custom(context).find { it.file.name == code.drop(2) }
                                code.startsWith("e:") -> Sticker.Emoji(code.drop(2).take(16))
                                else -> null
                            }
                        } else null
                        if (sticker != null) scope.launch {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { StickerLibrary.bitmap(sticker) }?.let {
                                vm.insertStickerBitmap(it, point.first, point.second, point.third) { pg, id -> target.selectItems(pg, setOf(id)) }
                            }
                        } else {
                            val grant = (context as? android.app.Activity)?.requestDragAndDropPermissions(event)
                            val clip = event.clipData
                            val jobs = (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }.filter { it.scheme == "content" }.mapIndexed { i, uri ->
                                vm.insertImage(uri, point.first, point.second + i * 16f, point.third + i * 16f) { pg, id -> target.selectItems(pg, setOf(id)) }
                            }
                            scope.launch { try { jobs.forEach { it.join() } } finally { grant?.release() } }
                        }
                        true
                    }
                }
                else -> true
            }
        }
        onDispose { target?.clearDropPreview(); target?.setOnDragListener(null) }
    }
    var reviewHandwriting by remember { mutableStateOf(false) }
    var reviewError by remember { mutableStateOf("") }
    val review = vm.handwritingReview
    if (reviewHandwriting && review != null) {
        AppDialog(
            onDismissRequest = { reviewHandwriting = false },
            title = { Text("Review handwriting") },
            text = {
                Column {
                    Text(review.item.text, style = MaterialTheme.typography.titleMedium)
                    review.alternatives.forEach { alternative ->
                        TextButton(onClick = {
                            if (vm.applyHandwritingReview(alternative)) reviewHandwriting = false
                            else reviewError = "That reading cannot be placed here. You can restore the ink."
                        }) { Text(alternative) }
                    }
                    if (reviewError.isNotEmpty()) Text(reviewError)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (vm.applyHandwritingReview(null)) reviewHandwriting = false
                    else reviewError = "The original conversion has since been edited."
                }) { Text("Restore ink") }
            },
            confirmButton = { TextButton(onClick = { reviewHandwriting = false }) { Text("Done") } },
        )
    }
    var showPages by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showStickers by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var goTo by remember { mutableStateOf(false) }
    var focus by remember { mutableStateOf(false) }
    var pageColor by remember { mutableStateOf(false) }
    var wheelAt by remember { mutableStateOf<Offset?>(null) }
    var showTemplates by remember { mutableStateOf(false) }
    var videoUrl by remember { mutableStateOf<String?>(null) }
    videoUrl?.let { value ->
        val uri = android.net.Uri.parse(value.trim())
        AppDialog(onDismissRequest = { videoUrl = null }, title = { Text("Floating video") },
            text = { OutlinedTextField(value, { videoUrl = it }, singleLine = true, label = { Text("Direct HTTPS video or stream URL") }) },
            confirmButton = { TextButton(enabled = uri.scheme == "https" && !uri.host.isNullOrBlank(), onClick = {
                vm.onRequestCommitText?.invoke()
                context.startActivity(android.content.Intent(context, com.pronotes.app.VideoActivity::class.java).setData(uri))
                videoUrl = null
            }) { Text("Play") } })
    }
    var stickyBoard by remember { mutableStateOf(false) }
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
                vm.onRequestCommitText?.invoke(); vm.saveNow()
                onBack()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            vm.onRequestCommitText?.invoke(); vm.saveNow()
            vm.onPagesChanged = null
            vm.onScrollTo = null
            vm.onRedraw = null
        }
    }

    LaunchedEffect(canvas) {
        val c = canvas ?: return@LaunchedEffect
        vm.onPagesChanged = {
            c.startPage = vm.pageIndex
            c.setPages(vm.pages.toList())
        }
        vm.onScrollTo = { i -> c.scrollToPage(i, animate = true) }
        vm.onRedraw = { c.redraw() }
        vm.toScreen = { pg, r -> c.pageToScreen(pg, r) }
        vm.fitMindMapOnCanvas = { pg, r -> c.fitRect(pg, r) }
        c.settings = vm.settingsFlow.value
        c.startPage = vm.pageIndex
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
            vm.onRequestCommitText?.invoke(); vm.saveNow()
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
        vm.notebookSettings.update {
            it.copy(
                toolbarDock = dock,
                toolbarPos = defaultPos,
                toolbarStyle = if (dock == ToolbarDock.FREE && it.toolbarStyle == ToolbarStyle.FIXED_BAR) ToolbarStyle.FLOATING_PILL else it.toolbarStyle,
            )
        }
    }
    val selectStyle: (ToolbarStyle) -> Unit = { nextStyle ->
        vm.notebookSettings.update {
            val nextDock = if (nextStyle == ToolbarStyle.FIXED_BAR && it.toolbarDock == ToolbarDock.FREE) ToolbarDock.TOP else it.toolbarDock
            it.copy(toolbarStyle = nextStyle, toolbarDock = nextDock)
        }
    }
    val resetToolbarPos: () -> Unit = {
        vm.notebookSettings.update { it.copy(toolbarDock = ToolbarDock.TOP, toolbarStyle = ToolbarStyle.FIXED_BAR, toolbarPos = "0.50,0.03") }
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
                            onHome = { vm.onRequestCommitText?.invoke(); vm.saveNow(); onBack() },
                            onRenameCurrent = { renaming = true },
                            onNewPageTab = { vm.addPage(atEnd = true) },
                            onSelect = ::switchTo,
                            onClose = { id ->
                                val rest = tabs - id
                                store.closeTab(id)
                                if (id == noteId) {
                                    vm.onRequestCommitText?.invoke(); vm.saveNow()
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
                                onHome = { vm.onRequestCommitText?.invoke(); vm.saveNow(); onBack() },
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
                                onSettings = { vm.onRequestCommitText?.invoke(); vm.saveNow(); onOpenSettings() },
                                onGoTo = { goTo = true },
                                onScrollToggle = {
                                    vm.notebookSettings.update {
                                        it.copy(
                                            scroll = if (it.scroll == ScrollDirection.VERTICAL) ScrollDirection.HORIZONTAL else ScrollDirection.VERTICAL,
                                        )
                                    }
                                },
                                onPaste = { vm.paste()?.let { (pg, ids) -> canvas?.selectItems(pg, ids) } },
                                onPageColor = { pageColor = true },
                                onCards = { showCards = true },
                                onVideo = { videoUrl = "" },
                                onReviewHandwriting = { reviewError = ""; reviewHandwriting = true },
                                onStickyBoard = { stickyBoard = true },
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
                    val overlayModalOpen = activePopoverTool != null || quickInsert != null
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
                                    override fun onFreshInk(page: Int, ink: List<InkItem>) = vm.queueHandwriting(page, ink)
                                    override fun beginEdit(page: Int) = vm.beginEdit(page)
                                    override fun setItems(page: Int, items: List<PageItem>) = vm.setItems(page, items)
                                    override fun onSelectionChanged(page: Int, ids: Set<Long>, screenBounds: android.graphics.RectF?) =
                                        vm.onSelection(page, ids, screenBounds)
                                    override fun onTextTap(page: Int, x: Float, y: Float, existing: TextItem?) {
                                        vm.openInlineTextBox(page, x, y, existing)
                                    }
                                    override fun onTextBoxPlace(page: Int, x: Float, y: Float, w: Float, h: Float) {
                                        vm.openInlineTextBox(page, x, y, existing = null, boxWidth = w, boxHeight = h)
                                    }
                                    override fun onStickyTap(page: Int, x: Float, y: Float, existing: StickyItem?) = vm.openSticky(page, x, y, existing)
                                    override fun onStickyToggleCollapse(page: Int, sticky: StickyItem) = vm.toggleStickyCollapse(page, sticky)
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
                        val snapCandidate = if (dockDragging && areaSize.width > 0 && areaSize.height > 0) {
                            val cx = (activeDockOffset.x + effDockW / 2f) / areaSize.width
                            val cy = (activeDockOffset.y + effDockH / 2f) / areaSize.height
                            when {
                                cx < 0.14f -> ToolbarDock.LEFT
                                cx > 0.86f -> ToolbarDock.RIGHT
                                cy < 0.12f -> ToolbarDock.TOP
                                cy > 0.88f -> ToolbarDock.BOTTOM
                                else -> ToolbarDock.FREE
                            }
                        } else null

                        if (snapCandidate != null && snapCandidate != ToolbarDock.FREE) {
                            val guideColor = ProButtonPalette.ElectricBlue.copy(alpha = 0.24f)
                            val guideBorder = ProButtonPalette.ElectricBlue.copy(alpha = 0.72f)
                            val guideMod = when (snapCandidate) {
                                ToolbarDock.LEFT -> Modifier.align(Alignment.CenterStart).padding(start = 8.dp).width(6.dp).fillMaxHeight(0.36f)
                                ToolbarDock.RIGHT -> Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).width(6.dp).fillMaxHeight(0.36f)
                                ToolbarDock.TOP -> Modifier.align(Alignment.TopCenter).padding(top = 8.dp).height(6.dp).fillMaxWidth(0.32f)
                                else -> Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).height(6.dp).fillMaxWidth(0.32f)
                            }
                            Box(
                                guideMod
                                    .clip(CircleShape)
                                    .background(guideColor)
                                    .border(1.dp, guideBorder, CircleShape),
                            )
                        }

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
                                    vm.notebookSettings.update {
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

                    // Subtle auto-dismissing status toast (import/export/error feedback only)
                    vm.toast?.let { msg ->
                        AppSurface(
                            role = SurfaceRole.Popover,
                            containerColor = Color(0xEB1B1F2A),
                            contentColor = Color.White,
                            shape = appPillShape(),
                            onClick = { vm.dismissToast() },
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
                                vm.openInlineTextBox(req.page, req.x, req.y)
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
                    // Curved dual-arc Quick Actions (stylus side button)
                    wheelAt?.let { at ->
                        QuickActionsOverlay(
                            vm = vm,
                            at = at,
                            areaSize = areaSize,
                            onOpenToolSettings = { t ->
                                vm.tool = t
                                activePopoverTool = t
                            },
                            onDismiss = { wheelAt = null },
                        )
                    }
                    // Sync canvas hiddenTextItemId and close popovers whenever inline text editing starts
                    LaunchedEffect(vm.textEdit, canvas) {
                        canvas?.editingTextItemId = vm.textEdit?.existing?.id
                        if (vm.textEdit != null) {
                            activePopoverTool = null
                        }
                    }
                    // Unified direct-on-page inline text & keyboard editor (inside imePadding)
                    vm.textEdit?.let { edit ->
                        androidx.compose.runtime.key(edit) {
                            InlinePageTextEditor(
                                vm = vm,
                                edit = edit,
                                canvas = canvas,
                                areaSize = areaSize,
                                darkUi = darkUi,
                            )
                        }
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
    }

    // ------------------------------------------------------------------ dialogs & sheets
    vm.mindNodeEdit?.let { edit -> MindNodeDialog(vm, edit) }
    vm.cardDraft?.let { d -> CardEditorDialog(vm, d) }
    if (vm.linkSheet != null) LinkSheet(vm)
    if (stickyBoard) StickyBoard(vm) { stickyBoard = false }
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
        var formatting by remember(edit) { mutableStateOf(false) }
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
                    TextButton(onClick = { formatting = !formatting }) { Text(if (formatting) "Hide appearance" else "Customize appearance") }
                    if (formatting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StickyColors.forEach { c -> Swatch(c, c == color, 20) { color = c } }
                        }
                        Text("STYLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AppSegmentedControl(
                            options = styles.map { it.second },
                            selectedIndex = styles.indexOfFirst { it.first == style }.coerceAtLeast(0),
                            onSelect = { i -> style = styles[i].first },
                        )
                        AppSegmentedControl(
                            options = fonts.map { it.second },
                            selectedIndex = fonts.indexOfFirst { it.first == font }.coerceAtLeast(0),
                            onSelect = { i -> font = fonts[i].first },
                        )
                        SliderRow("TEXT SIZE", "%.0f pt".format(fontSize), fontSize, 18f..48f) { fontSize = it }
                    }
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
            StickerSheet(onDragStarted = { showStickers = false; pendingInsertCoords = null }) { bmp ->
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

// Extracted panels in EditorPanels.kt, SplitViewPanel.kt, and EditorDialogs.kt

