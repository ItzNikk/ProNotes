package com.pronotes.app.editor

import android.app.Application
import android.content.Intent
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.ui.ToolGlyph
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

private val Tools = listOf(Tool.PEN, Tool.ERASER, Tool.HIGHLIGHTER, Tool.SHAPE, Tool.LASSO, Tool.IMAGE, Tool.TEXT, Tool.LASER)

@Composable
fun EditorScreen(noteId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: EditorViewModel = viewModel(key = noteId, factory = viewModelFactory { initializer { EditorViewModel(app, noteId) } })
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    var canvas by remember { mutableStateOf<NoteCanvasView?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.insertImage(uri, 0f)
        vm.tool = Tool.LASSO
    }
    val exportPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            context.contentResolver.openOutputStream(uri)?.use { vm.exportPdf(it) }
        }
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
    DisposableEffect(Unit) { onDispose { vm.saveNow(); vm.onPagesChanged = null } }

    // Push model changes (undo, page switch, deletes) into the canvas.
    LaunchedEffect(canvas) {
        val c = canvas ?: return@LaunchedEffect
        vm.onPagesChanged = {
            vm.currentPage?.let { c.showPage(it, animate = true) }
            c.clearSelection()
        }
        vm.currentPage?.let { c.showPage(it, animate = false) }
    }
    LaunchedEffect(vm.tool, canvas) {
        canvas?.tool = vm.tool
        if (vm.tool == Tool.IMAGE) pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    LaunchedEffect(vm.settings, canvas) { canvas?.settings = vm.settings }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ---------------------------------------------------------- top bar
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.saveNow(); onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    Text(
                        vm.title, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                    IconButton(onClick = vm::undo, enabled = vm.canUndo) { Icon(Icons.AutoMirrored.Filled.Undo, "Undo") }
                    IconButton(onClick = vm::redo, enabled = vm.canRedo) { Icon(Icons.AutoMirrored.Filled.Redo, "Redo") }
                    IconButton(onClick = vm::addPage) { Icon(Icons.Outlined.NoteAdd, "Add page") }
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
                            PaperStyle.entries.forEach { s ->
                                DropdownMenuItem(text = { Text("Paper: ${s.label}") }, onClick = { more = false; vm.setPaper(s) })
                            }
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Delete page") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                onClick = { more = false; vm.deletePage() }, enabled = vm.pages.size > 1)
                            DropdownMenuItem(text = { Text("Pen & input settings") }, onClick = { more = false; showSettings = true })
                        }
                    }
                }
                Toolbar(vm, density)
            }
        }

        // ---------------------------------------------------------- canvas
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (vm.loaded) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        NoteCanvasView(ctx, object : CanvasHost {
                            override fun currentBrush(tool: Tool) = vm.brushFor(tool)
                            override fun eraserRadius() = vm.eraserRadiusPx(density)
                            override fun beginEdit() = vm.beginEdit()
                            override fun setItems(items: List<PageItem>) = vm.setItems(items)
                            override fun onSelectionChanged(ids: Set<Long>, screenBounds: android.graphics.RectF?) = vm.onSelection(ids, screenBounds)
                            override fun onTextTap(worldX: Float, worldY: Float, existing: TextItem?) {
                                vm.textEdit = EditorViewModel.TextEdit(worldX, worldY, existing)
                            }
                            override fun undo() = vm.undo()
                            override fun redo() = vm.redo()
                            override fun pdfBitmap(page: Page, onReady: () -> Unit) = vm.pdfBitmap(page, onReady)
                        }).also {
                            it.renderer = vm.renderer()
                            it.settings = vm.settings
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
                    onDelete = vm::deleteSelection,
                    onDuplicate = { val ids = vm.duplicateSelection(); canvas?.selectItems(ids) },
                    onColor = { c -> vm.recolorSelection(c); canvas?.let { cv -> vm.currentPage?.let { cv.showPage(it, false) }; cv.selectItems(vm.selection) } },
                )
            }

            // Page indicator
            if (vm.pages.isNotEmpty()) {
                PagePill(
                    index = vm.pageIndex, count = vm.pages.size,
                    onPrev = { vm.goToPage(vm.pageIndex - 1) }, onNext = { vm.goToPage(vm.pageIndex + 1) },
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp),
                )
            }
        }
    }

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

    if (showSettings) SettingsDialog(vm.settings, onChange = vm::updateSettings, onDismiss = { showSettings = false })
}

// ------------------------------------------------------------------ toolbar

@Composable
private fun Toolbar(vm: EditorViewModel, density: Float) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tools.forEach { t ->
            val selected = vm.tool == t
            val ink = Color(vm.styleFor(t)?.color ?: vm.penStyle.color)
            val lift by animateDpAsState(if (selected) (-4).dp else 0.dp, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "lift")
            val bg by animateFloatAsState(if (selected) 1f else 0f, label = "bg")
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = bg))
                    .clickable { vm.tool = t },
                contentAlignment = Alignment.Center,
            ) {
                ToolGlyph(
                    t,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    inkColor = ink,
                    modifier = Modifier.size(28.dp).offset(y = lift),
                )
            }
        }
        Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant))
        AnimatedContent(
            targetState = vm.tool,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith fadeOut() },
            label = "options",
        ) { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                val style = vm.styleFor(t)
                when {
                    style != null -> StyleOptions(style) { vm.updateStyle(t, it) }
                    t == Tool.ERASER -> SizeDots(listOf(8f, 16f, 30f), listOf(8f, 16f, 30f)[vm.eraserSize], ring = true) { s ->
                        vm.eraserSize = listOf(8f, 16f, 30f).indexOf(s)
                    }
                    t == Tool.TEXT -> SizeDots(listOf(28f, 40f, 64f), vm.textSize) { vm.textSize = it }
                    else -> Text(
                        when (t) {
                            Tool.LASSO -> "Circle to select · drag to move"
                            Tool.LASER -> "Laser pointer"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun StyleOptions(style: ToolStyle, onChange: (ToolStyle) -> Unit) {
    var palette by remember { mutableStateOf<Int?>(null) }   // slot being edited
    var sizer by remember { mutableStateOf(false) }
    style.colors.forEachIndexed { i, c ->
        val selected = c == style.color
        val s by animateFloatAsState(if (selected) 1.15f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "sw")
        Box {
            Box(
                Modifier.padding(horizontal = 4.dp).size(26.dp).scale(s)
                    .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                    .padding(3.dp).clip(CircleShape).background(Color(c).copy(alpha = 1f))
                    .clickable { if (selected) palette = i else onChange(style.copy(color = c)) },
            )
            if (palette == i) {
                Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, 120), onDismissRequest = { palette = null }) {
                    ColorPalette { picked ->
                        val keepAlpha = (style.color ushr 24) and 0xFF
                        val nc = (picked and 0x00FFFFFF) or (keepAlpha shl 24)
                        val colors = style.colors.toMutableList().also { it[i] = nc }
                        onChange(style.copy(colors = colors, color = nc))
                        palette = null
                    }
                }
            }
        }
    }
    Spacer(Modifier.width(10.dp))
    Box {
        SizeDots(style.sizes, style.size) { sz -> if (sz == style.size) sizer = true else onChange(style.copy(size = sz)) }
        if (sizer) {
            Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, 120), onDismissRequest = { sizer = false }) {
                Surface(shape = RoundedCornerShape(16.dp), shadowElevation = 8.dp, modifier = Modifier.width(240.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Size ${"%.1f".format(style.size)}", style = MaterialTheme.typography.labelSmall)
                        Slider(value = style.size, onValueChange = { v ->
                            val sizes = style.sizes.map { if (it == style.size) v else it }
                            onChange(style.copy(size = v, sizes = sizes))
                        }, valueRange = 1f..60f)
                    }
                }
            }
        }
    }
}

@Composable
private fun SizeDots(sizes: List<Float>, current: Float, ring: Boolean = false, onPick: (Float) -> Unit) {
    val max = sizes.max()
    sizes.forEachIndexed { i, s ->
        val selected = s == current
        Box(
            Modifier.padding(horizontal = 2.dp).size(32.dp).clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .clickable { onPick(s) },
            contentAlignment = Alignment.Center,
        ) {
            val d = (6 + 14 * (i + 1) / sizes.size.toFloat() * (s / max).coerceAtLeast(0.4f)).dp
            Box(
                Modifier.size(d).clip(CircleShape).then(
                    if (ring) Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    else Modifier.background(MaterialTheme.colorScheme.onSurface),
                ),
            )
        }
    }
}

@Composable
private fun ColorPalette(onPick: (Int) -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), shadowElevation = 10.dp) {
        LazyVerticalGrid(GridCells.Fixed(6), Modifier.width(264.dp).padding(12.dp), userScrollEnabled = false) {
            items(PenPalette) { c ->
                Box(
                    Modifier.padding(4.dp).size(32.dp).clip(CircleShape).background(Color(c))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape).clickable { onPick(c) },
                )
            }
        }
    }
}

@Composable
private fun SelectionMenu(onDelete: () -> Unit, onDuplicate: () -> Unit, onColor: (Int) -> Unit) {
    var colors by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF1B1F2A), contentColor = Color.White, shadowElevation = 8.dp) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDuplicate) { Icon(Icons.Outlined.ContentCopy, "Duplicate") }
            Box {
                IconButton(onClick = { colors = true }) { Icon(Icons.Outlined.Palette, "Color") }
                if (colors) Popup(offset = IntOffset(0, 140), onDismissRequest = { colors = false }) {
                    ColorPalette { onColor(it); colors = false }
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Delete", tint = Color(0xFFFF8A80)) }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(s: InputSettings, onChange: (InputSettings) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Pen & input") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ToggleRow("Draw with stylus only (fingers scroll)", s.stylusOnly) { onChange(s.copy(stylusOnly = it)) }
                ToggleRow("Scribble to erase", s.scribbleToErase) { onChange(s.copy(scribbleToErase = it)) }
                ToggleRow("Draw and hold to make a shape", s.holdToShape) { onChange(s.copy(holdToShape = it)) }
                ToggleRow("Circle and hold to select", s.circleToLasso) { onChange(s.copy(circleToLasso = it)) }
                Text("Pen side button", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SideButtonAction.entries.forEachIndexed { i, a ->
                        SegmentedButton(
                            selected = s.sideButton == a,
                            onClick = { onChange(s.copy(sideButton = a)) },
                            shape = SegmentedButtonDefaults.itemShape(i, SideButtonAction.entries.size),
                        ) { Text(a.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    }
                }
                Text(
                    "Works with S Pen, Wacom, USI, Lenovo, Xiaomi and other Android styluses. " +
                        "Two-finger tap undoes, three-finger tap redoes.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(value, onChange)
    }
}
