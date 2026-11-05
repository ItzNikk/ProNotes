package com.pronotes.app.editor

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.FormatAlignRight
import androidx.compose.material.icons.outlined.FormatAlignCenter
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.pronotes.app.data.PaperStyle
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
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

private val DockTools = listOf(
    Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.SHAPE, Tool.LASSO, Tool.IMAGE, Tool.TEXT,
    Tool.STICKY, Tool.TAPE, Tool.STICKER, Tool.LASER,
)

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
    var focus by remember { mutableStateOf(settings.fullscreen) }

    LaunchedEffect(noteId) { store.openTab(noteId) }
    LaunchedEffect(library) { store.retainTabs(library.notebooks.map { it.id }.toSet()) }

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

    BackHandler { if (focus) focus = false else { vm.saveNow(); onBack() } }
    DisposableEffect(Unit) { onDispose { vm.saveNow(); vm.onPagesChanged = null; vm.onScrollTo = null } }

    LaunchedEffect(canvas) {
        val c = canvas ?: return@LaunchedEffect
        vm.onPagesChanged = { c.setPages(vm.pages.toList()) }
        vm.onScrollTo = { i -> c.scrollToPage(i, animate = true) }
        c.setPages(vm.pages.toList())
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
    DisposableEffect(settings.keepScreenOn) {
        val window = (context as? android.app.Activity)?.window
        if (settings.keepScreenOn) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    fun switchTo(id: String) { if (id != noteId) { vm.saveNow(); onSwitchNote(id) } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedVisibility(!focus, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                Column(Modifier.statusBarsPadding()) {
                    if (settings.showTabs) {
                        TabStrip(
                            tabs = tabs.mapNotNull { id -> library.notebooks.firstOrNull { it.id == id }?.let { id to it.title } },
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
                        vm = vm, settings = settings, showSearch = showSearch,
                        onHome = { vm.saveNow(); onBack() },
                        onPages = { showPages = true },
                        onSearch = { showSearch = !showSearch; if (!showSearch) vm.clearSearch() },
                        onRename = { renaming = true },
                        onFocus = { focus = true },
                        onExport = { exportPdf.launch(vm.title + ".pdf") },
                        onShare = { sharePdf() },
                        onSettings = { vm.saveNow(); onOpenSettings() },
                        onScrollToggle = {
                            store.update { it.copy(scroll = if (it.scroll == ScrollDirection.VERTICAL) ScrollDirection.HORIZONTAL else ScrollDirection.VERTICAL) }
                        },
                        onPaste = { vm.paste()?.let { (pg, ids) -> canvas?.selectItems(pg, ids) } },
                    )
                    AnimatedVisibility(showSearch, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        SearchBar(vm)
                    }
                }
            }
        }

        // -------------------------------------------------------------- canvas
        var areaSize by remember { mutableStateOf(IntSize.Zero) }
        Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { areaSize = it }) {
            if (vm.loaded) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        NoteCanvasView(ctx, object : CanvasHost {
                            override fun brush(tool: Tool) = vm.brushFor(tool)
                            override fun shapeKind() = vm.shapeKind
                            override fun eraserRadius() = vm.eraserRadiusPx(density)
                            override fun tapeColor() = vm.tapeColor
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
                            override fun onCircleCandidate(screenBounds: android.graphics.RectF?) = vm.showCircleChip(screenBounds)
                            override fun onEraserLifted() = vm.onEraserLifted()
                            override fun onZoom(percent: Int?) = vm.onZoom(percent)
                        }).also {
                            it.renderer = vm.renderer()
                            it.settings = settings
                            it.tool = vm.tool
                            canvas = it
                        }
                    },
                )
            }

            // "Select" chip after circling content with the pen.
            val chip = vm.circleChip
            androidx.compose.animation.AnimatedVisibility(
                visible = chip != null,
                enter = fadeIn() + scaleIn(initialScale = 0.6f),
                exit = fadeOut() + scaleOut(targetScale = 0.6f),
                modifier = Modifier.offset {
                    val r = chip ?: android.graphics.RectF()
                    IntOffset((r.right - 20 * density).roundToInt().coerceAtLeast(8), (r.top - 44 * density).roundToInt().coerceAtLeast(8))
                },
            ) {
                Surface(
                    shape = CircleShape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 8.dp, modifier = Modifier.clickable { canvas?.acceptCircle() },
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProIconView(ProIcon.LASSO, MaterialTheme.colorScheme.onPrimary, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Select", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Floating selection menu
            val sb = vm.selectionBounds
            androidx.compose.animation.AnimatedVisibility(
                visible = vm.selection.isNotEmpty() && sb != null,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f),
                modifier = Modifier.offset {
                    val b = sb ?: android.graphics.RectF()
                    IntOffset(b.left.roundToInt().coerceAtLeast(8), (b.top - 56 * density).roundToInt().coerceAtLeast(8))
                },
            ) {
                SelectionMenu(
                    vm = vm,
                    onDuplicate = { val ids = vm.duplicateSelection(); canvas?.selectItems(vm.selectionPage, ids) },
                    onColor = { c -> val ids = vm.selection; val pg = vm.selectionPage; vm.recolorSelection(c); canvas?.selectItems(pg, ids) },
                )
            }

            if (settings.toolbarDock != ToolbarDock.HEADER) {
                ToolDock(vm, settings, areaSize, onDock = { d -> store.update { it.copy(toolbarDock = d) } })
            } else {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) { OptionsBar(vm, vertical = false, settings = settings) }
            }

            vm.zoomPercent?.let { z ->
                Surface(shape = CircleShape, color = Color(0xCC1B1F2A), contentColor = Color.White, modifier = Modifier.align(Alignment.Center)) {
                    Text("$z%", Modifier.padding(horizontal = 14.dp, vertical = 6.dp), style = MaterialTheme.typography.titleMedium)
                }
            }

            if (focus) {
                Surface(
                    shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp,
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp).clickable { focus = false },
                ) { ProIconView(ProIcon.CLOSE, MaterialTheme.colorScheme.onSurface, Modifier.padding(10.dp).size(20.dp)) }
            }

            if (vm.pages.isNotEmpty()) {
                PagePill(
                    index = vm.pageIndex, count = vm.pages.size, bookmarked = vm.currentPage?.bookmarked == true,
                    onPrev = { vm.goToPage(vm.pageIndex - 1) }, onNext = { vm.goToPage(vm.pageIndex + 1) },
                    modifier = Modifier.align(if (settings.toolbarDock == ToolbarDock.BOTTOM) Alignment.TopEnd else Alignment.BottomEnd)
                        .navigationBarsPadding().padding(16.dp),
                )
            }
        }
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
                    Row { StickyColors.forEach { c -> Swatch(c, c == color) { color = c } } }
                    OutlinedTextField(
                        text, { text = it }, minLines = 4, modifier = Modifier.fillMaxWidth().background(Color(color), RoundedCornerShape(8.dp)),
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

    if (renaming) {
        var name by remember { mutableStateOf(vm.title) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename notebook") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.rename(name.trim()); renaming = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } },
        )
    }

    if (showStickers) {
        ModalBottomSheet(onDismissRequest = { showStickers = false; vm.tool = Tool.LASSO }) {
            Text("Stickers", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp))
            LazyVerticalGrid(GridCells.Adaptive(56.dp), Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(16.dp)) {
                items(StickerSet.size) { i ->
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).clickable {
                            showStickers = false
                            vm.insertSticker(StickerSet[i])?.let { (pg, id) -> vm.tool = Tool.LASSO; canvas?.selectItems(pg, setOf(id)) }
                        },
                        contentAlignment = Alignment.Center,
                    ) { Text(StickerSet[i], style = MaterialTheme.typography.headlineSmall) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPages) {
        ModalBottomSheet(onDismissRequest = { showPages = false }) {
            PagesSheet(vm, onJump = { showPages = false; vm.goToPage(it) })
        }
    }
}

// ============================================================== tabs + top bar

@Composable
private fun TabStrip(tabs: List<Pair<String, String>>, current: String, onSelect: (String) -> Unit, onClose: (String) -> Unit) {
    if (tabs.isEmpty()) return
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).horizontalScroll(rememberScrollState())
            .padding(start = 8.dp, end = 8.dp, top = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        tabs.forEach { (id, title) ->
            val sel = id == current
            val bg by animateFloatAsState(if (sel) 1f else 0f, label = "tab")
            Row(
                Modifier.padding(end = 4.dp).widthIn(min = 120.dp, max = 220.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f + 0.65f * bg))
                    .clickable { onSelect(id) }.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title, Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (sel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(Modifier.size(24.dp).clip(CircleShape).clickable { onClose(id) }, contentAlignment = Alignment.Center) {
                    ProIconView(ProIcon.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(12.dp))
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
        Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
            .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { ProIconView(icon, tint, Modifier.size(24.dp)) }
}

@Composable
private fun TopBar(
    vm: EditorViewModel, settings: AppSettings, showSearch: Boolean,
    onHome: () -> Unit, onPages: () -> Unit, onSearch: () -> Unit, onRename: () -> Unit, onFocus: () -> Unit,
    onExport: () -> Unit, onShare: () -> Unit, onSettings: () -> Unit, onScrollToggle: () -> Unit, onPaste: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BarIcon(ProIcon.HOME, "Library", onClick = onHome)
        BarIcon(ProIcon.SIDEBAR, "Pages", onClick = onPages)
        BarIcon(ProIcon.SEARCH, "Search", active = showSearch, onClick = onSearch)
        if (settings.toolbarDock == ToolbarDock.HEADER) {
            Spacer(Modifier.weight(1f))
            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                BarIcon(ProIcon.UNDO, "Undo", enabled = vm.canUndo, onClick = vm::undo)
                BarIcon(ProIcon.REDO, "Redo", enabled = vm.canRedo, onClick = vm::redo)
                Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outlineVariant))
                DockTools.forEach { t -> ToolButton(vm, t, settings) }
                ColorDot(vm)
            }
            Spacer(Modifier.weight(1f))
        } else {
            Text(
                vm.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).clickable(onClick = onRename),
            )
            BarIcon(ProIcon.UNDO, "Undo", enabled = vm.canUndo, onClick = vm::undo)
            BarIcon(ProIcon.REDO, "Redo", enabled = vm.canRedo, onClick = vm::redo)
        }
        BarIcon(ProIcon.ADD, "Add page") { vm.addPage() }
        var shareMenu by remember { mutableStateOf(false) }
        Box {
            BarIcon(ProIcon.SHARE, "Export") { shareMenu = true }
            DropdownMenu(shareMenu, { shareMenu = false }) {
                DropdownMenuItem(text = { Text("Export as PDF…") }, leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) },
                    onClick = { shareMenu = false; onExport() })
                DropdownMenuItem(text = { Text("Share PDF") }, leadingIcon = { Icon(Icons.Outlined.IosShare, null) },
                    onClick = { shareMenu = false; onShare() })
            }
        }
        var more by remember { mutableStateOf(false) }
        Box {
            BarIcon(ProIcon.MORE, "More") { more = true }
            DropdownMenu(more, { more = false }) {
                DropdownMenuItem(text = { Text("Rename notebook") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                    onClick = { more = false; onRename() })
                DropdownMenuItem(text = { Text(if (vm.currentPage?.bookmarked == true) "Remove bookmark" else "Bookmark page") },
                    leadingIcon = { ProIconView(ProIcon.BOOKMARK, MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp)) },
                    onClick = { more = false; vm.toggleBookmark() })
                if (vm.hasClipboard) DropdownMenuItem(text = { Text("Paste") }, leadingIcon = { Icon(Icons.Outlined.ContentPaste, null) },
                    onClick = { more = false; onPaste() })
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
                DropdownMenuItem(text = { Text("Settings") }, leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                    onClick = { more = false; onSettings() })
            }
        }
    }
}

/** The active colour, shown as a dot next to the tools (opens the colour editor). */
@Composable
private fun ColorDot(vm: EditorViewModel) {
    val t = if (vm.styleFor(vm.tool) != null) vm.tool else Tool.PEN
    val style = vm.styleFor(t) ?: return
    var edit by remember { mutableStateOf(false) }
    Box(
        Modifier.padding(horizontal = 6.dp).size(22.dp).clip(CircleShape).background(Color(style.color))
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            .border(3.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape)
            .clickable { edit = true },
    )
    if (edit) {
        ColorEditorDialog(style.color, PenPalette, vm.recentColors(), onDismiss = { edit = false }) { c ->
            val idx = style.colors.indexOf(style.color).coerceAtLeast(0)
            vm.updateStyle(t, style.copy(colors = style.colors.toMutableList().also { it[idx] = c }, color = c))
            vm.pushRecent(c); edit = false
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
                shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f),
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
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(if (i == vm.activeHit) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { vm.jumpToHit(i) }.padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text("p.${hit.page + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(44.dp))
                        Text(hit.snippet, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ============================================================== pages sheet

@Composable
private fun PagesSheet(vm: EditorViewModel, onJump: (Int) -> Unit) {
    var onlyBookmarks by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pages", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            FilterChip(onlyBookmarks, { onlyBookmarks = !onlyBookmarks }, label = { Text("Bookmarked") })
            Spacer(Modifier.width(8.dp))
            var paperMenu by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { paperMenu = true }) { Text("Paper: ${vm.currentPage?.paper?.label ?: ""}") }
                DropdownMenu(paperMenu, { paperMenu = false }) {
                    PaperStyle.entries.forEach { s -> DropdownMenuItem(text = { Text(s.label) }, onClick = { paperMenu = false; vm.setPaper(s) }) }
                }
            }
            TextButton(onClick = { vm.addPage(atEnd = true) }) { Icon(Icons.Outlined.Add, null); Text("Add page") }
        }
        val indices = vm.pages.indices.filter { !onlyBookmarks || vm.pages[it].bookmarked }
        LazyVerticalGrid(GridCells.Adaptive(120.dp), Modifier.fillMaxWidth().heightIn(max = 560.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(indices.size, key = { vm.pages[indices[it]].id }) { n ->
                val i = indices[n]
                val page = vm.pages[i]
                val thumb by produceState<Bitmap?>(null, page) { value = vm.thumbnail(i, 240) }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.animateItem()) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(page.width / page.height)
                            .shadow(4.dp, RoundedCornerShape(6.dp)).background(Color.White)
                            .border(if (i == vm.pageIndex) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                            .clickable { onJump(i) },
                    ) {
                        thumb?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
                        if (page.bookmarked) ProIconView(ProIcon.BOOKMARK, Color(0xFFE34948), Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp), ink = Color(0xFFE34948))
                    }
                    Text("${i + 1}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

// ============================================================== tool dock

/**
 * Floating toolbar. Drag the grip to any edge (top, bottom, left, right);
 * it snaps there and remembers the choice. Tool options sit beside it.
 */
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
                Modifier.size(28.dp).pointerInput(area) {
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
            DockTools.forEach { t -> ToolButton(vm, t, settings) }
            ColorDot(vm)
        }
        val bar = @Composable {
            Surface(
                shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
                shadowElevation = if (dragging) 14.dp else 6.dp, tonalElevation = 1.dp,
            ) {
                if (isVertical) Column(Modifier.padding(6.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) { grip(); tools() }
                else Row(Modifier.padding(6.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) { grip(); tools() }
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

@Composable
private fun OptionsBar(vm: EditorViewModel, vertical: Boolean, settings: AppSettings) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f), shadowElevation = 4.dp) {
        if (vertical) Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) { ToolOptions(vm, true, settings) }
        else Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { ToolOptions(vm, false, settings) }
    }
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
    val lift by animateDpAsState(if (selected) (-1).dp else 0.dp, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "lift")
    val sz = (42 * settings.toolbarSize.scale).dp
    var card by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier.padding(1.dp).size(sz).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = bg))
                .clickable {
                    // Tapping the active pen, shape, highlighter or eraser opens its settings card.
                    if (selected && (t == Tool.PEN || t == Tool.SHAPE || t == Tool.HIGHLIGHTER || t == Tool.ERASER)) card = true
                    else vm.tool = t
                },
            contentAlignment = Alignment.Center,
        ) {
            val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            ToolGlyph(t, tint, ink, Modifier.size((24 * settings.toolbarSize.scale).dp).offset(y = lift), active = selected)
        }
        if (card) {
            Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, 150), onDismissRequest = { card = false }, properties = PopupProperties(focusable = true)) {
                when (t) {
                    Tool.PEN -> PenCard(vm)
                    Tool.SHAPE -> ShapeCard(vm)
                    Tool.ERASER -> EraserCard()
                    else -> HighlighterCard(vm)
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
                t == Tool.TAPE -> CustomSwatches(TapeColors, vm.tapeColor, vm) { vm.tapeColor = it }
                t == Tool.STICKY -> CustomSwatches(StickyColors, vm.stickyColor, vm) { vm.stickyColor = it }
                else -> Text(
                    when (t) {
                        Tool.LASSO -> "Circle to select · drag to move"
                        Tool.LASER -> "Fades after ${"%.0f".format(settings.laserSeconds)}s · circle to select"
                        Tool.IMAGE -> "Pick a photo"
                        Tool.STICKER -> "Pick a sticker"
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        if (vertical) Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
        else Row(verticalAlignment = Alignment.CenterVertically) { content() }
    }
}

@Composable
private fun MiniChip(label: String, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(4.dp).clickable(onClick = onClick)) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
    }
}

/** Fixed colours plus a "+" for any custom colour (tape, sticky notes). */
@Composable
private fun CustomSwatches(base: List<Int>, current: Int, vm: EditorViewModel, onPick: (Int) -> Unit) {
    var custom by remember { mutableStateOf(false) }
    base.forEach { c -> Swatch(c, c == current) { onPick(c) } }
    if (current !in base) Swatch(current, true) { custom = true }
    AddSwatchButton { custom = true }
    if (custom) ColorEditorDialog(current, PenPalette, vm.recentColors(), onDismiss = { custom = false }) { onPick(it); vm.pushRecent(it); custom = false }
}

@Composable
private fun AddSwatchButton(onClick: () -> Unit) {
    Box(
        Modifier.padding(4.dp).size(26.dp).clip(CircleShape)
            .background(Brush.sweepGradient(listOf(Color(0xFFFF3B30), Color(0xFFFFCC00), Color(0xFF34C759), Color(0xFF0A84FF), Color(0xFFAF52DE), Color(0xFFFF3B30))))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Add, "Custom colour", Modifier.size(12.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StyleOptions(vm: EditorViewModel, t: Tool, style: ToolStyle) {
    var editing by remember { mutableStateOf<Int?>(null) }
    var adding by remember { mutableStateOf(false) }
    var removeMenu by remember { mutableStateOf<Int?>(null) }
    style.colors.forEachIndexed { i, c ->
        val selected = c == style.color
        Box {
            Swatch(c, selected, onLongClick = { removeMenu = i }) { if (selected) editing = i else vm.updateStyle(t, style.copy(color = c)) }
            DropdownMenu(removeMenu == i, { removeMenu = null }) {
                DropdownMenuItem(text = { Text("Edit colour") }, onClick = { removeMenu = null; editing = i })
                DropdownMenuItem(text = { Text("Remove") }, enabled = style.colors.size > 1, onClick = { removeMenu = null; vm.removeSwatch(t, i) })
            }
        }
    }
    if (style.colors.size < 8) AddSwatchButton { adding = true }
    Box(Modifier.padding(4.dp).width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outlineVariant))
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
        ColorEditorDialog(style.color, presets, vm.recentColors(), onDismiss = { adding = false }) { picked ->
            vm.addSwatch(t, picked); adding = false
        }
    }
}

@Composable
private fun SizeDots(sizes: List<Float>, current: Float, ring: Boolean = false, onPick: (Float) -> Unit) {
    sizes.forEachIndexed { i, s ->
        val selected = s == current
        Box(
            Modifier.padding(1.dp).size(34.dp).clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                .clickable { onPick(s) },
            contentAlignment = Alignment.Center,
        ) {
            val w = (2 + 3 * i).dp
            if (ring) Box(Modifier.size((8 + 6 * i).dp).border(1.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape))
            else Box(Modifier.width(18.dp).height(w).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.onSurface))
        }
    }
}

// ============================================================== cards

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface, modifier = Modifier.width(340.dp)) {
        Column(Modifier.padding(18.dp).heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
            content()
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

@Composable
private fun PenCard(vm: EditorViewModel) {
    val style = vm.penStyle
    Card(vm.penType.label) {
        StrokePreview(Color(style.color), style.size * vm.penType.sizeFactor * 2.2f, vm.penType != PenType.BALL)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PenType.entries.forEach { type ->
                val sel = vm.penType == type
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
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
    }
}

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
        ToggleLine("Straight line", s.highlighterStraight) { v -> store.update { it.copy(highlighterStraight = v) } }
        ToggleLine("Draw behind ink", s.highlighterBehind) { v -> store.update { it.copy(highlighterBehind = v) } }
    }
}

@Composable
private fun EraserCard() {
    val store = SettingsStore.get(LocalContext.current)
    val s by store.settings.collectAsState()
    Card("Eraser") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EraserMode.entries.forEach { m ->
                FilterChip(s.eraserMode == m, { store.update { it.copy(eraserMode = m) } }, label = { Text(m.label) })
            }
        }
        ToggleLine("Erase highlighter only", s.eraseHighlighterOnly) { v -> store.update { it.copy(eraseHighlighterOnly = v) } }
        ToggleLine("Switch back after erasing", s.eraserAutoSwitch) { v -> store.update { it.copy(eraserAutoSwitch = v) } }
    }
}

@Composable
private fun ToggleLine(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
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
                    Modifier.size(70.dp).clip(RoundedCornerShape(12.dp))
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
                        Swatch(c, c == vm.shapeFillColor) { vm.shapeFillColor = c }
                    }
                    AddSwatchButton { fillEdit = true }
                }
                SliderRow("OPACITY", "${((vm.shapeFillColor ushr 24) * 100 / 255)}%", (vm.shapeFillColor ushr 24) / 255f, 0.1f..1f) { a ->
                    vm.shapeFillColor = (vm.shapeFillColor and 0x00FFFFFF) or ((a * 255).toInt() shl 24)
                }
            }
        }
        Text(
            if (vm.shapeKind == ShapeKind.AUTO) "Draw freely; Pro Notes turns it into a clean shape." else "Drag to draw a ${vm.shapeKind.label.lowercase()}.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (fillEdit) ColorEditorDialog(vm.shapeFillColor, PenPalette, vm.recentColors(), onDismiss = { fillEdit = false }) { c ->
        vm.shapeFillColor = (c and 0x00FFFFFF) or (vm.shapeFillColor and 0xFF000000.toInt()); fillEdit = false
    }
}

@Composable
private fun SliderRow(label: String, value: String, v: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelSmall)
        }
        Slider(v, onChange, valueRange = range)
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
    var colorEdit by remember { mutableStateOf<Int?>(null) }   // 0 = main, 1 = gradient end
    var fontMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { vm.textEdit = null },
        title = { Text(if (ex != null) "Edit text" else "Add text") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Live preview
                val previewStyle = TextStyle(
                    fontSize = (size / 2f).coerceIn(12f, 40f).sp,
                    fontFamily = style.font.compose(),
                    fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = if (style.underline) TextDecoration.Underline else null,
                    textAlign = when (style.align) { TextAlign.LEFT -> androidx.compose.ui.text.style.TextAlign.Start; TextAlign.CENTER -> androidx.compose.ui.text.style.TextAlign.Center; TextAlign.RIGHT -> androidx.compose.ui.text.style.TextAlign.End },
                ).let { s -> style.gradient?.let { g -> s.copy(brush = Brush.linearGradient(listOf(Color(color), Color(g)))) } ?: s.copy(color = Color(color)) }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(12.dp)) {
                    Text(text.ifBlank { "Preview" }, style = previewStyle, modifier = Modifier.fillMaxWidth(), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                OutlinedTextField(text, { text = it }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        TextButton(onClick = { fontMenu = true }) { Text(style.font.label); Icon(Icons.Filled.KeyboardArrowDown, null) }
                        DropdownMenu(fontMenu, { fontMenu = false }) {
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
                Row {
                    FmtToggle(Icons.AutoMirrored.Outlined.FormatAlignLeft, style.align == TextAlign.LEFT) { style = style.copy(align = TextAlign.LEFT) }
                    FmtToggle(Icons.Outlined.FormatAlignCenter, style.align == TextAlign.CENTER) { style = style.copy(align = TextAlign.CENTER) }
                    FmtToggle(Icons.AutoMirrored.Outlined.FormatAlignRight, style.align == TextAlign.RIGHT) { style = style.copy(align = TextAlign.RIGHT) }
                }
                SliderRow("SIZE", "%.0f".format(size), size, 16f..160f) { size = it }
                Text("Colour", style = MaterialTheme.typography.labelSmall)
                FlowRow {
                    PenPalette.take(12).forEach { c -> Swatch(c, c == color, 22) { color = c } }
                    AddSwatchButton { colorEdit = 0 }
                }
                ToggleLine("Gradient", style.gradient != null) { on -> style = style.copy(gradient = if (on) 0xFF7161E0.toInt() else null) }
                style.gradient?.let { g ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ends in", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 8.dp))
                        listOf(0xFF7161E0, 0xFFD55181, 0xFF3987E5, 0xFF1BAF7A, 0xFFEB6834).map { it.toInt() }.forEach { c ->
                            Swatch(c, c == g, 22) { style = style.copy(gradient = c) }
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
        Modifier.padding(2.dp).size(38.dp).clip(RoundedCornerShape(10.dp))
            .background(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
}

// ============================================================== small parts

@Composable
private fun SelectionMenu(vm: EditorViewModel, onDuplicate: () -> Unit, onColor: (Int) -> Unit) {
    var colors by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF1B1F2A), contentColor = Color.White, shadowElevation = 8.dp) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::cutSelection) { Icon(Icons.Outlined.ContentCut, "Cut") }
            IconButton(onClick = vm::copySelection) { Icon(Icons.Outlined.ContentCopy, "Copy") }
            IconButton(onClick = onDuplicate) { ProIconView(ProIcon.ADD, Color.White, Modifier.size(22.dp)) }
            IconButton(onClick = { colors = true }) { Icon(Icons.Outlined.Palette, "Colour") }
            IconButton(onClick = vm::deleteSelection) { Icon(Icons.Outlined.Delete, "Delete", tint = Color(0xFFFF8A80)) }
        }
    }
    if (colors) {
        ColorEditorDialog(0xFF1B1F2A.toInt(), PenPalette, vm.recentColors(), onDismiss = { colors = false }) { onColor(it); colors = false }
    }
}

@Composable
private fun PagePill(index: Int, count: Int, bookmarked: Boolean, onPrev: () -> Unit, onNext: () -> Unit, modifier: Modifier) {
    Surface(modifier.shadow(6.dp, CircleShape), shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = onPrev, enabled = index > 0) { Icon(Icons.Filled.ChevronLeft, "Previous page") }
            if (bookmarked) ProIconView(ProIcon.BOOKMARK, Color(0xFFE34948), Modifier.size(14.dp), ink = Color(0xFFE34948))
            AnimatedContent(targetState = index, label = "page") { i ->
                Text("${i + 1} / $count", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 2.dp))
            }
            IconButton(onClick = onNext, enabled = index < count - 1) { Icon(Icons.Filled.ChevronRight, "Next page") }
        }
    }
}
