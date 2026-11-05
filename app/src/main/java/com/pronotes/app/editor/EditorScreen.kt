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
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.FlipToFront
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
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
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.PenPreset
import com.pronotes.app.data.Repository
import com.pronotes.app.data.ScrollDirection
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.ToolbarDock
import com.pronotes.app.ui.ColorEditorDialog
import com.pronotes.app.ui.PenGlyph
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import com.pronotes.app.ui.ShapeGlyph
import com.pronotes.app.ui.StickerSet
import com.pronotes.app.ui.Swatch
import com.pronotes.app.ui.ToolGlyph
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
private fun glassColor(): Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)

@Composable
private fun Modifier.glassEdge(shape: androidx.compose.ui.graphics.Shape): Modifier {
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
    DisposableEffect(Unit) { onDispose { vm.saveNow(); vm.onPagesChanged = null; vm.onScrollTo = null } }

    LaunchedEffect(canvas) {
        val c = canvas ?: return@LaunchedEffect
        vm.onPagesChanged = { c.setPages(vm.pages.toList()) }
        vm.onScrollTo = { i -> c.scrollToPage(i, animate = true) }
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

    // Real background blur (Android 12+) while a card, dialog or sheet is open.
    val overlayOpen = vm.textEdit != null || vm.stickyEdit != null || renaming || goTo || pageColor ||
        showStickers || vm.cardOpen || wheelAt != null
    val blurRadius by androidx.compose.animation.core.animateDpAsState(if (overlayOpen) 14.dp else 0.dp, label = "blur")
    val blurMod = if (android.os.Build.VERSION.SDK_INT >= 31 && blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().then(blurMod).background(MaterialTheme.colorScheme.background)) {
        AnimatedVisibility(!focus, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
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
                        vm = vm, settings = settings, showSearch = showSearch, showPages = showPages,
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
                                override fun onCurrentPage(index: Int) = vm.onCurrentPage(index)
                                override fun onPullAddPage() = vm.addPage(atEnd = true)
                                override fun undo() = vm.undo()
                                override fun redo() = vm.redo()
                                override fun pdfBitmap(page: Page, onReady: () -> Unit) = vm.pdfBitmap(page, onReady)
                                override fun highlights(page: Int) = vm.highlights(page)
                                override fun activeHighlight() = vm.activeHighlight()
                                override fun shapeFill() = vm.shapeFill()
                                override fun onCircleCandidate(screenBounds: android.graphics.RectF?) = Unit
                                override fun onEraserLifted() = vm.onEraserLifted()
                                override fun onZoom(percent: Int?) = vm.onZoom(percent)
                                override fun penProfile(deviceName: String) = store.registerPen(deviceName)
                                override fun onPenButton(doubleClick: Boolean, x: Float, y: Float, deviceName: String) {
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
                        onDuplicate = { val ids = vm.duplicateSelection(); canvas?.selectItems(vm.selectionPage, ids) },
                        onColor = { c -> val ids = vm.selection; val pg = vm.selectionPage; vm.recolorSelection(c); canvas?.selectItems(pg, ids) },
                        keep = { block -> val ids = vm.selection; val pg = vm.selectionPage; block(); canvas?.selectItems(pg, ids) },
                        onShareImage = { scope.launch { vm.selectionImage()?.let { shareBitmap(context, it, vm.title + " selection") } } },
                    )
                }

                if (settings.toolbarDock != ToolbarDock.HEADER) {
                    ToolDock(vm, settings, areaSize, onDock = { d -> store.update { it.copy(toolbarDock = d) } })
                } else {
                    Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) { OptionsBar(vm, vertical = false, settings = settings) }
                }

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

                if (vm.pages.isNotEmpty()) {
                    PagePill(
                        index = vm.pageIndex, count = vm.pages.size, bookmarked = vm.currentPage?.bookmarked == true,
                        onPrev = { vm.goToPage(vm.pageIndex - 1) }, onNext = { vm.goToPage(vm.pageIndex + 1) },
                        onTap = { goTo = true },
                        modifier = Modifier.align(if (settings.toolbarDock == ToolbarDock.BOTTOM) Alignment.TopEnd else Alignment.BottomEnd)
                            .navigationBarsPadding().padding(16.dp),
                    )
                }
            }
        }
    }

    // Quick tool wheel (pen button click while hovering).
    wheelAt?.let { at -> ToolWheel(vm, at, onDismiss = { wheelAt = null }) }
    }

    // ------------------------------------------------------------------ dialogs
    vm.textEdit?.let { edit -> TextDialog(vm, edit) }

    vm.stickyEdit?.let { edit ->
        var text by remember(edit) { mutableStateOf(edit.item.text) }
        var color by remember(edit) { mutableStateOf(edit.item.color) }
        AlertDialog(
            onDismissRequest = { vm.stickyEdit = null },
            title = { Text(if (edit.isNew) "New sticky note" else "Sticky note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row { StickyColors.forEach { c -> Swatch(c, c == color, 20) { color = c } } }
                    OutlinedTextField(
                        text, { text = it }, minLines = 4, shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().background(Color(color), RoundedCornerShape(16.dp)),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { vm.stickyColor = color; vm.commitSticky(text, color) }) { Text("Done") } },
            dismissButton = {
                TextButton(onClick = { if (edit.isNew) vm.stickyEdit = null else vm.deleteSticky() }) {
                    Text(if (edit.isNew) "Cancel" else "Delete")
                }
            },
        )
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
            StickerSheet(vm) { bmp ->
                showStickers = false
                vm.tool = Tool.LASSO
                vm.insertStickerBitmap(bmp) { pg, id -> canvas?.selectItems(pg, setOf(id)) }
            }
        }
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
        Modifier.size(40.dp).clip(Pill)
            .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { ProIconView(icon, tint, Modifier.size(22.dp)) }
}

@Composable
private fun TopBar(
    vm: EditorViewModel, settings: AppSettings, showSearch: Boolean, showPages: Boolean,
    onHome: () -> Unit, onPages: () -> Unit, onSearch: () -> Unit, onRename: () -> Unit, onFocus: () -> Unit,
    onExport: () -> Unit, onShare: () -> Unit, onSharePage: () -> Unit, onSettings: () -> Unit, onGoTo: () -> Unit,
    onScrollToggle: () -> Unit, onPaste: () -> Unit, onPageColor: () -> Unit, onPageImage: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BarIcon(ProIcon.HOME, "Library", onClick = onHome)
        BarIcon(ProIcon.SIDEBAR, "Pages", active = showPages, onClick = onPages)
        BarIcon(ProIcon.SEARCH, "Search", active = showSearch, onClick = onSearch)
        if (settings.toolbarDock == ToolbarDock.HEADER) {
            Spacer(Modifier.weight(1f))
            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                BarIcon(ProIcon.UNDO, "Undo", enabled = vm.canUndo, onClick = vm::undo)
                BarIcon(ProIcon.REDO, "Redo", enabled = vm.canRedo, onClick = vm::redo)
                Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outlineVariant))
                ToolbarOrder.visible(settings.toolbarTools).forEach { t -> ToolButton(vm, t, settings) }
            }
            Spacer(Modifier.weight(1f))
        } else {
            Text(
                vm.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp).clickable(onClick = onRename),
            )
        }
        BarIcon(ProIcon.ADD, "Add page") { vm.addPage() }
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
            DropdownMenu(more, { more = false }, shape = RoundedCornerShape(18.dp)) {
                DropdownMenuItem(text = { Text("Rename notebook") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                    onClick = { more = false; onRename() })
                DropdownMenuItem(text = { Text("Go to page…") }, leadingIcon = { Icon(Icons.Outlined.SwapHoriz, null) },
                    onClick = { more = false; onGoTo() })
                DropdownMenuItem(text = { Text(if (vm.currentPage?.bookmarked == true) "Remove bookmark" else "Bookmark page") },
                    leadingIcon = { ProIconView(ProIcon.BOOKMARK, MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp)) },
                    onClick = { more = false; vm.toggleBookmark() })
                if (vm.hasClipboard) DropdownMenuItem(text = { Text("Paste") }, leadingIcon = { Icon(Icons.Outlined.ContentPaste, null) },
                    onClick = { more = false; onPaste() })
                DropdownMenuItem(text = { Text("Page colour�") }, leadingIcon = { Icon(Icons.Outlined.Palette, null) },
                    onClick = { more = false; onPageColor() })
                DropdownMenuItem(
                    text = { Text(if (vm.currentPage?.bgImage != null) "Remove page picture" else "Picture as page…") },
                    leadingIcon = { Icon(Icons.Outlined.Image, null) },
                    onClick = { more = false; if (vm.currentPage?.bgImage != null) vm.setPageBackground(null) else onPageImage() },
                )
                DropdownMenuItem(text = { Text("Duplicate page") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                    onClick = { more = false; vm.duplicatePage() })
                DropdownMenuItem(text = { Text("Delete page") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                    onClick = { more = false; vm.deletePage() }, enabled = vm.pages.size > 1)
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(if (settings.scroll == ScrollDirection.VERTICAL) "Scroll horizontally" else "Scroll vertically") },
                    leadingIcon = { Icon(Icons.Outlined.SwapHoriz, null) }, onClick = { more = false; onScrollToggle() },
                )
                DropdownMenuItem(text = { Text("Focus mode") }, leadingIcon = { ProIconView(ProIcon.FOCUS, MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp)) },
                    onClick = { more = false; onFocus() })
                DropdownMenuItem(text = { Text("Settings") }, leadingIcon = { ProIconView(ProIcon.SETTINGS, MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp)) },
                    onClick = { more = false; onSettings() })
            }
        }
    }
}

// ============================================================== search

@Composable
private fun SearchBar(vm: EditorViewModel) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = vm.searchQuery, onValueChange = vm::search, singleLine = true,
                placeholder = { Text(if (vm.isPdf) "Search this PDF and your text" else "Search typed text and notes") },
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
private fun BoxScope.ToolDock(vm: EditorViewModel, settings: AppSettings, area: IntSize, onDock: (ToolbarDock) -> Unit) {
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
        val tools = @Composable {
            BarIcon(ProIcon.UNDO, "Undo", enabled = vm.canUndo, onClick = vm::undo)
            BarIcon(ProIcon.REDO, "Redo", enabled = vm.canRedo, onClick = vm::redo)
            ToolbarOrder.visible(settings.toolbarTools).forEach { t -> ToolButton(vm, t, settings) }
        }
        val bar = @Composable {
            Surface(
                shape = Pill, color = glassColor(),
                shadowElevation = if (dragging) 14.dp else 6.dp,
                modifier = Modifier.glassEdge(Pill),
            ) {
                if (isVertical) Column(Modifier.padding(5.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) { grip(); tools() }
                else Row(Modifier.padding(5.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) { grip(); tools() }
            }
        }
        val optionsBar = @Composable { OptionsBar(vm, isVertical, settings) }
        when (d) {
            ToolbarDock.TOP, ToolbarDock.HEADER -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { bar(); optionsBar() }
            ToolbarDock.BOTTOM -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { optionsBar(); bar() }
            ToolbarDock.LEFT -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { bar(); optionsBar() }
            ToolbarDock.RIGHT -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { optionsBar(); bar() }
        }
    }
}

/** Compact pill with the active tool's colours and sizes. Hidden when the tool has none. */
@Composable
private fun OptionsBar(vm: EditorViewModel, vertical: Boolean, settings: AppSettings) {
    val hasOptions = vm.styleFor(vm.tool) != null || vm.tool in setOf(Tool.ERASER, Tool.TAPE, Tool.STICKY, Tool.TEXT)
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
private fun ToolButton(vm: EditorViewModel, t: Tool, settings: AppSettings) {
    val selected = vm.tool == t
    val ink = Color(vm.styleFor(t)?.color ?: when (t) {
        Tool.TAPE -> vm.tapeColor
        Tool.STICKY -> vm.stickyColor
        else -> vm.penStyle.color
    })
    val bg by animateFloatAsState(if (selected) 1f else 0f, label = "bg")
    val sz = (40 * settings.toolbarSize.scale).dp
    val density = LocalDensity.current.density
    var card by remember { mutableStateOf(false) }
    LaunchedEffect(card) { vm.cardOpen = card }
    Box {
        Box(
            Modifier.padding(1.dp).size(sz).clip(Pill)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = bg))
                .clickable {
                    // Tapping the tool that is already selected opens all of its settings.
                    when {
                        !selected -> vm.tool = t
                        t == Tool.IMAGE || t == Tool.STICKER -> vm.reopenTool++   // pick another photo / sticker
                        else -> card = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            ToolGlyph(t, tint, ink, Modifier.size((23 * settings.toolbarSize.scale).dp), active = selected)
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
                        Segments(listOf("Straight", "Freehand"), if (vm.tapeFreehand) 1 else 0) { vm.tapeFreehand = it == 1 }
                        Row(verticalAlignment = Alignment.CenterVertically) { CustomSwatches(TapeColors, vm.tapeColor, vm) { vm.tapeColor = it } }
                    }
                    Tool.STICKY -> ColorCard("Sticky note", StickyColors, vm.stickyColor, vm) { vm.stickyColor = it }
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
                else -> Unit
            }
        }
        if (vertical) Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
        else Row(verticalAlignment = Alignment.CenterVertically) { content() }
    }
}

@Composable
private fun MiniChip(label: String, onClick: () -> Unit) {
    Surface(shape = Pill, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(3.dp).clickable(onClick = onClick)) {
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
    Card(vm.penType.label) {
        StrokePreview(Color(style.color), style.size * vm.penType.sizeFactor * 2.2f, vm.penType != PenType.BALL)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PenType.entries.forEach { type ->
                val sel = vm.penType == type
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                        .background(if (sel) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        .clickable { vm.penType = type }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PenGlyph(type, MaterialTheme.colorScheme.onSurface, Color(style.color), Modifier.size(30.dp))
                    Text(type.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        SliderRow("THICKNESS", "%.1f".format(style.size), style.size, 0.5f..20f) { v ->
            val sizes = style.sizes.map { if (it == style.size) v else it }
            vm.updateStyle(Tool.PEN, style.copy(size = v, sizes = sizes))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("FAVOURITE PENS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = vm::savePreset) { Icon(Icons.Outlined.StarBorder, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Save this pen") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.forEach { p -> PresetChip(p, onClick = { vm.applyPreset(p) }, onRemove = { vm.removePreset(p) }) }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PresetChip(p: PenPreset, onClick: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(Pill).border(1.dp, MaterialTheme.colorScheme.outlineVariant, Pill)
                .combinedClickableCompat(onClick, { menu = true }).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PenGlyph(runCatching { PenType.valueOf(p.type) }.getOrDefault(PenType.FOUNTAIN), MaterialTheme.colorScheme.onSurface, Color(p.color), Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(width = 18.dp, height = (p.size / 2f).coerceIn(2f, 8f).dp).clip(Pill).background(Color(p.color)))
        }
        DropdownMenu(menu, { menu = false }, shape = RoundedCornerShape(16.dp)) {
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
        StrokePreview(Color(style.color).copy(alpha = 0.45f), style.size * 1.2f, false)
        SliderRow("THICKNESS", "%.0f".format(style.size), style.size, 6f..70f) { v ->
            val sizes = style.sizes.map { if (it == style.size) v else it }
            vm.updateStyle(Tool.HIGHLIGHTER, style.copy(size = v, sizes = sizes))
        }
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
        ToggleLine("Erase highlighter only", s.eraseHighlighterOnly) { v -> store.update { it.copy(eraseHighlighterOnly = v) } }
        ToggleLine("Switch back after erasing", s.eraserAutoSwitch) { v -> store.update { it.copy(eraserAutoSwitch = v) } }
        ToggleLine("Scribble to erase with the pen", s.scribbleToErase) { v -> store.update { it.copy(scribbleToErase = v) } }
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
            FilterChip(f.ink, { store.update { it.copy(lassoFilter = f.copy(ink = !f.ink)) } }, label = { Text("Handwriting") }, shape = Pill)
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
        Text("Keep the pen or a finger on the screen to hold the laser in place. Circle content with the laser to select it.",
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
private fun StickerSheet(vm: EditorViewModel, onPick: (Bitmap) -> Unit) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Stickers", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(start = 8.dp))
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
    vm: EditorViewModel, onDuplicate: () -> Unit, onColor: (Int) -> Unit,
    keep: (() -> Unit) -> Unit, onShareImage: () -> Unit,
) {
    var colors by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }
    Surface(shape = Pill, color = Color(0xE01B1F2A), contentColor = Color.White, shadowElevation = 8.dp, modifier = Modifier.glassEdge(Pill)) {
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::cutSelection) { Icon(Icons.Outlined.ContentCut, "Cut") }
            IconButton(onClick = vm::copySelection) { Icon(Icons.Outlined.ContentCopy, "Copy") }
            IconButton(onClick = onDuplicate) { ProIconView(ProIcon.ADD, Color.White, Modifier.size(22.dp)) }
            IconButton(onClick = { colors = true }) { Icon(Icons.Outlined.Palette, "Colour") }
            IconButton(onClick = { keep { vm.rotateSelection(90f) } }) { Icon(Icons.Outlined.RotateRight, "Rotate 90°") }
            Box {
                IconButton(onClick = { more = true }) { ProIconView(ProIcon.MORE, Color.White, Modifier.size(22.dp)) }
                DropdownMenu(more, { more = false }, shape = RoundedCornerShape(18.dp)) {
                    DropdownMenuItem(text = { Text("Flip horizontally") }, leadingIcon = { Icon(Icons.Outlined.Flip, null) },
                        onClick = { more = false; keep { vm.flipSelection() } })
                    DropdownMenuItem(text = { Text("Bring to front") }, leadingIcon = { Icon(Icons.Outlined.FlipToFront, null) },
                        onClick = { more = false; keep { vm.arrange(true) } })
                    DropdownMenuItem(text = { Text("Send to back") }, leadingIcon = { Icon(Icons.Outlined.FlipToBack, null) },
                        onClick = { more = false; keep { vm.arrange(false) } })
                    DropdownMenuItem(text = { Text("Share as image") }, leadingIcon = { Icon(Icons.Outlined.IosShare, null) },
                        onClick = { more = false; onShareImage() })
                }
            }
            IconButton(onClick = vm::deleteSelection) { Icon(Icons.Outlined.Delete, "Delete", tint = Color(0xFFFF8A80)) }
        }
    }
    if (colors) {
        ColorEditorDialog(0xFF1B1F2A.toInt(), PenPalette, vm.recentColors(), onDismiss = { colors = false }) { onColor(it); colors = false }
    }
}

@Composable
private fun PagePill(index: Int, count: Int, bookmarked: Boolean, onPrev: () -> Unit, onNext: () -> Unit, onTap: () -> Unit, modifier: Modifier) {
    Surface(modifier.shadow(6.dp, Pill).glassEdge(Pill), shape = Pill, color = glassColor()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = onPrev, enabled = index > 0) { Icon(Icons.Filled.KeyboardArrowUp, "Previous page", Modifier.graphicsLayer { rotationZ = -90f }) }
            Row(Modifier.clip(Pill).clickable(onClick = onTap).padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (bookmarked) ProIconView(ProIcon.BOOKMARK, Color(0xFFE34948), Modifier.size(14.dp), ink = Color(0xFFE34948))
                AnimatedContent(targetState = index, label = "page") { i ->
                    Text("${i + 1} / $count", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 2.dp))
                }
            }
            IconButton(onClick = onNext, enabled = index < count - 1) { Icon(Icons.Filled.KeyboardArrowUp, "Next page", Modifier.graphicsLayer { rotationZ = 90f }) }
        }
    }
}

// ============================================================== tool wheel

/** Radial tool picker that pops up where the pen hovers (pen button click). */
@Composable
private fun ToolWheel(vm: EditorViewModel, at: Offset, onDismiss: () -> Unit) {
    val density = LocalDensity.current.density
    val tools = listOf(Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.LASSO, Tool.SHAPE, Tool.TEXT, Tool.LASER, Tool.TAPE)
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.62f, stiffness = 420f)) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(
        Modifier.fillMaxSize().onSizeChanged { size = it }
            .background(Color.Black.copy(alpha = 0.12f * appear.value))
            .pointerInput(Unit) { androidx.compose.foundation.gestures.detectTapGestures { onDismiss() } },
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
