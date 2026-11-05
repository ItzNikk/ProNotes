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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.ToolbarDock
import com.pronotes.app.ui.ColorEditorDialog
import com.pronotes.app.ui.PenGlyph
import com.pronotes.app.ui.ShapeGlyph
import com.pronotes.app.ui.StickerSet
import com.pronotes.app.ui.Swatch
import com.pronotes.app.ui.ToolGlyph
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

private val DockTools = listOf(
    Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.SHAPE, Tool.LASSO, Tool.TEXT,
    Tool.IMAGE, Tool.STICKY, Tool.TAPE, Tool.STICKER, Tool.LASER,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(noteId: String, onBack: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: EditorViewModel = viewModel(key = noteId, factory = viewModelFactory { initializer { EditorViewModel(app, noteId) } })
    val settings by vm.settingsFlow.collectAsState()
    val store = remember { SettingsStore.get(context) }
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    var canvas by remember { mutableStateOf<NoteCanvasView?>(null) }
    var showPages by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showStickers by remember { mutableStateOf(false) }

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

    BackHandler { vm.saveNow(); onBack() }
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

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ------------------------------------------------------------- top bar
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.saveNow(); onBack() }) { Icon(Icons.Outlined.Home, "Library") }
                    IconButton(onClick = { showPages = true }) { Icon(Icons.Outlined.GridView, "Pages") }
                    IconButton(onClick = { showSearch = !showSearch; if (!showSearch) vm.clearSearch() }) {
                        Icon(Icons.Outlined.Search, "Search", tint = if (showSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        vm.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    IconButton(onClick = vm::undo, enabled = vm.canUndo) { Icon(Icons.AutoMirrored.Filled.Undo, "Undo") }
                    IconButton(onClick = vm::redo, enabled = vm.canRedo) { Icon(Icons.AutoMirrored.Filled.Redo, "Redo") }
                    IconButton(onClick = { vm.addPage() }) { Icon(Icons.Outlined.NoteAdd, "Add page") }
                    var shareMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { shareMenu = true }) { Icon(Icons.Outlined.IosShare, "Export") }
                        DropdownMenu(shareMenu, { shareMenu = false }) {
                            DropdownMenuItem(text = { Text("Export as PDF…") }, leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) },
                                onClick = { shareMenu = false; exportPdf.launch(vm.title + ".pdf") })
                            DropdownMenuItem(text = { Text("Share PDF") }, leadingIcon = { Icon(Icons.Outlined.IosShare, null) },
                                onClick = { shareMenu = false; sharePdf() })
                        }
                    }
                    var more by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { more = true }) { Icon(Icons.Outlined.MoreHoriz, "More") }
                        DropdownMenu(more, { more = false }) {
                            DropdownMenuItem(text = { Text("Duplicate page") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                onClick = { more = false; vm.duplicatePage() })
                            DropdownMenuItem(text = { Text("Delete page") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                onClick = { more = false; vm.deletePage() }, enabled = vm.pages.size > 1)
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Settings") }, leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                                onClick = { more = false; vm.saveNow(); onOpenSettings() })
                        }
                    }
                }
                AnimatedVisibility(showSearch, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    SearchBar(vm)
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
                        }).also {
                            it.renderer = vm.renderer()
                            it.settings = settings
                            it.tool = vm.tool
                            canvas = it
                        }
                    },
                )
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
                    presets = com.pronotes.app.editor.PenPalette,
                    onDelete = vm::deleteSelection,
                    onDuplicate = { val ids = vm.duplicateSelection(); canvas?.selectItems(vm.selectionPage, ids) },
                    onColor = { c -> val ids = vm.selection; val pg = vm.selectionPage; vm.recolorSelection(c); canvas?.selectItems(pg, ids) },
                )
            }

            ToolDock(vm, settings.toolbarDock, areaSize, onDock = { d -> store.update { it.copy(toolbarDock = d) } })

            if (vm.pages.isNotEmpty()) {
                PagePill(
                    index = vm.pageIndex, count = vm.pages.size,
                    onPrev = { vm.goToPage(vm.pageIndex - 1) }, onNext = { vm.goToPage(vm.pageIndex + 1) },
                    modifier = Modifier.align(if (settings.toolbarDock == ToolbarDock.BOTTOM) Alignment.TopEnd else Alignment.BottomEnd)
                        .navigationBarsPadding().padding(16.dp),
                )
            }
        }
    }

    // ------------------------------------------------------------------ dialogs
    vm.textEdit?.let { edit ->
        var text by remember(edit) { mutableStateOf(edit.existing?.text ?: "") }
        AlertDialog(
            onDismissRequest = { vm.textEdit = null },
            title = { Text(if (edit.existing != null) "Edit text" else "Add text") },
            text = { OutlinedTextField(text, { text = it }, minLines = 3, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { vm.commitText(text) }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { vm.textEdit = null }) { Text("Cancel") } },
        )
    }

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

// ============================================================== search

@Composable
private fun SearchBar(vm: EditorViewModel) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = vm.searchQuery, onValueChange = vm::search, singleLine = true,
                placeholder = { Text(if (vm.isPdf) "Search this PDF and your text" else "Search typed text and notes") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
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
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pages", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            var paperMenu by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { paperMenu = true }) { Text("Paper: ${vm.currentPage?.paper?.label ?: ""}") }
                DropdownMenu(paperMenu, { paperMenu = false }) {
                    PaperStyle.entries.forEach { s -> DropdownMenuItem(text = { Text(s.label) }, onClick = { paperMenu = false; vm.setPaper(s) }) }
                }
            }
            TextButton(onClick = { vm.addPage(atEnd = true) }) { Icon(Icons.Outlined.Add, null); Text("Add page") }
        }
        LazyVerticalGrid(GridCells.Adaptive(120.dp), Modifier.fillMaxWidth().heightIn(max = 560.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(vm.pages.size, key = { vm.pages[it].id }) { i ->
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
private fun BoxScope.ToolDock(vm: EditorViewModel, dock: ToolbarDock, area: IntSize, onDock: (ToolbarDock) -> Unit) {
    var drag by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    val vertical = dock == ToolbarDock.LEFT || dock == ToolbarDock.RIGHT
    val align = when (dock) {
        ToolbarDock.TOP -> Alignment.TopCenter
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
                    var start = Offset.Zero
                    detectDragGestures(
                        onDragStart = { p -> dragging = true; start = p },
                        onDragEnd = {
                            dragging = false
                            // Snap to the edge nearest to where the bar was dropped.
                            val w = area.width.toFloat(); val h = area.height.toFloat()
                            val base = when (d) {
                                ToolbarDock.TOP -> Offset(w / 2, 40f)
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
        val tools = @Composable { DockTools.forEach { t -> ToolButton(vm, t) } }
        val options = @Composable { ToolOptions(vm, isVertical) }

        val bar = @Composable {
            Surface(
                shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
                shadowElevation = if (dragging) 14.dp else 6.dp, tonalElevation = 1.dp,
            ) {
                if (isVertical) Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) { grip(); tools() }
                else Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) { grip(); tools() }
            }
        }
        val optionsBar = @Composable {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f), shadowElevation = 4.dp) {
                if (isVertical) Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) { options() }
                else Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { options() }
            }
        }
        when (d) {
            ToolbarDock.TOP -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { bar(); optionsBar() }
            ToolbarDock.BOTTOM -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { optionsBar(); bar() }
            ToolbarDock.LEFT -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { bar(); optionsBar() }
            ToolbarDock.RIGHT -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { optionsBar(); bar() }
        }
    }
}

@Composable
private fun ToolButton(vm: EditorViewModel, t: Tool) {
    val selected = vm.tool == t
    val ink = Color(vm.styleFor(t)?.color ?: vm.penStyle.color)
    val bg by animateFloatAsState(if (selected) 1f else 0f, label = "bg")
    val lift by animateDpAsState(if (selected) (-2).dp else 0.dp, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "lift")
    var card by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier.padding(2.dp).size(42.dp).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = bg))
                .clickable {
                    // Tapping the active pen, shape or highlighter opens its settings card.
                    if (selected && (t == Tool.PEN || t == Tool.SHAPE || t == Tool.HIGHLIGHTER)) card = true
                    else vm.tool = t
                },
            contentAlignment = Alignment.Center,
        ) {
            val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            if (t == Tool.PEN) PenGlyph(vm.penType, tint, ink, Modifier.size(26.dp).offset(y = lift))
            else ToolGlyph(t, tint, ink, Modifier.size(26.dp).offset(y = lift))
        }
        if (card) {
            Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, 150), onDismissRequest = { card = false }, properties = PopupProperties(focusable = true)) {
                when (t) {
                    Tool.PEN -> PenCard(vm)
                    Tool.SHAPE -> ShapeCard(vm)
                    else -> HighlighterCard(vm)
                }
            }
        }
    }
}

@Composable
private fun ToolOptions(vm: EditorViewModel, vertical: Boolean) {
    AnimatedContent(
        targetState = vm.tool,
        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.92f)) togetherWith fadeOut() },
        label = "options",
    ) { t ->
        val content = @Composable {
            val style = vm.styleFor(t)
            when {
                style != null -> StyleOptions(vm, t, style)
                t == Tool.ERASER -> SizeDots(listOf(8f, 16f, 30f), listOf(8f, 16f, 30f)[vm.eraserSize], ring = true) { s ->
                    vm.eraserSize = listOf(8f, 16f, 30f).indexOf(s)
                }
                t == Tool.TEXT -> SizeDots(listOf(28f, 40f, 64f), vm.textSize) { vm.textSize = it }
                t == Tool.TAPE -> TapeColors.forEach { c -> Swatch(c, c == vm.tapeColor) { vm.tapeColor = c } }
                t == Tool.STICKY -> StickyColors.forEach { c -> Swatch(c, c == vm.stickyColor) { vm.stickyColor = c } }
                else -> Text(
                    when (t) {
                        Tool.LASSO -> "Circle to select · drag to move"
                        Tool.LASER -> "Fades after ${"%.0f".format(vm.settingsFlow.value.laserSeconds)}s · circle to select"
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
private fun StyleOptions(vm: EditorViewModel, t: Tool, style: ToolStyle) {
    var editing by remember { mutableStateOf<Int?>(null) }
    style.colors.forEachIndexed { i, c ->
        val selected = c == style.color
        Swatch(c, selected) { if (selected) editing = i else vm.updateStyle(t, style.copy(color = c)) }
    }
    Box(Modifier.padding(4.dp).width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outlineVariant))
    SizeDots(style.sizes, style.size) { sz -> vm.updateStyle(t, style.copy(size = sz)) }
    editing?.let { slot ->
        ColorEditorDialog(
            initial = style.colors[slot],
            presets = if (t == Tool.HIGHLIGHTER) HighlighterPalette + PenPalette else PenPalette,
            recent = vm.recentColors(),
            onDismiss = { editing = null },
        ) { picked ->
            val colors = style.colors.toMutableList().also { it[slot] = picked }
            vm.updateStyle(t, style.copy(colors = colors, color = picked))
            vm.pushRecent(picked)
            editing = null
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
    Surface(shape = RoundedCornerShape(22.dp), shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface, modifier = Modifier.width(320.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        if (taper) {
            for (k in 0..3) drawPath(path, color, style = Stroke(width * (0.4f + k * 0.2f), cap = StrokeCap.Round), alpha = 0.35f)
        }
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
    Card("Highlighter") {
        StrokePreview(Color(style.color).copy(alpha = 0.45f), style.size * 1.2f, false)
        SliderRow("THICKNESS", "%.0f".format(style.size), style.size, 6f..70f) { v ->
            val sizes = style.sizes.map { if (it == style.size) v else it }
            vm.updateStyle(Tool.HIGHLIGHTER, style.copy(size = v, sizes = sizes))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShapeCard(vm: EditorViewModel) {
    Card("Shapes") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShapeKind.entries.forEach { k ->
                val sel = vm.shapeKind == k
                Column(
                    Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { vm.shapeKind = k },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    ShapeGlyph(k, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, Modifier.size(26.dp))
                    Text(k.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Dashed line", modifier = Modifier.weight(1f))
            Switch(vm.shapeDashed, { vm.shapeDashed = it })
        }
        Text(
            if (vm.shapeKind == ShapeKind.AUTO) "Draw freely; Pro Notes turns it into a clean shape." else "Drag to draw a ${vm.shapeKind.label.lowercase()}.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

// ============================================================== small parts

@Composable
private fun SelectionMenu(presets: List<Int>, onDelete: () -> Unit, onDuplicate: () -> Unit, onColor: (Int) -> Unit) {
    var colors by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF1B1F2A), contentColor = Color.White, shadowElevation = 8.dp) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDuplicate) { Icon(Icons.Outlined.ContentCopy, "Duplicate") }
            IconButton(onClick = { colors = true }) { Icon(Icons.Outlined.Palette, "Colour") }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Delete", tint = Color(0xFFFF8A80)) }
        }
    }
    if (colors) {
        ColorEditorDialog(0xFF1B1F2A.toInt(), presets, emptyList(), onDismiss = { colors = false }) { onColor(it); colors = false }
    }
}

@Composable
private fun PagePill(index: Int, count: Int, onPrev: () -> Unit, onNext: () -> Unit, modifier: Modifier) {
    Surface(modifier.shadow(6.dp, CircleShape), shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = onPrev, enabled = index > 0) { Icon(Icons.Filled.ChevronLeft, "Previous page") }
            AnimatedContent(targetState = index, label = "page") { i ->
                Text("${i + 1} / $count", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onNext, enabled = index < count - 1) { Icon(Icons.Filled.ChevronRight, "Next page") }
        }
    }
}
