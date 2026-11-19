package com.pronotes.app.editor

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.FormatAlignRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pronotes.app.data.AppSettings
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppContextMenu
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.AppMenuButton
import com.pronotes.app.ui.AppMenuItem
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.AppSegmentedControl
import com.pronotes.app.ui.AppSliderRow
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.AppToggle
import com.pronotes.app.ui.ColorEditorDialog
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import com.pronotes.app.ui.ScaleInAnimatedText
import com.pronotes.app.ui.StickerSet
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.Swatch
import com.pronotes.app.ui.appPillShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MindNodeDialog(vm: EditorViewModel, edit: EditorViewModel.MindNodeEdit) {
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
                AppSegmentedControl(
                    options = shapes.map { it.second },
                    selectedIndex = shapes.indexOfFirst { it.first == shape }.coerceAtLeast(0),
                    onSelect = { i -> shape = shapes[i].first },
                )
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
internal fun ToggleLine(
    label: String,
    value: Boolean,
    fgColor: Color = LocalContentColor.current,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = fgColor)
        AppToggle(checked = value, onCheckedChange = onChange)
    }
}

@Composable
internal fun SliderRow(
    label: String,
    value: String,
    v: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    AppSliderRow(
        title = label,
        valueText = value,
        value = v,
        valueRange = range,
        onValueChange = onChange,
    )
}

@Composable
internal fun StickerSheet(onPick: (Bitmap) -> Unit) {
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
                "Import a picture to use it as a sticker. PNG files with transparency work best.",
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

private fun TextFont.compose(): FontFamily = when (this) {
    TextFont.SANS, TextFont.ROUNDED, TextFont.CONDENSED -> FontFamily.SansSerif
    TextFont.SERIF -> FontFamily.Serif
    TextFont.MONO -> FontFamily.Monospace
    TextFont.HAND, TextFont.CURSIVE -> FontFamily.Cursive
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TextDialog(vm: EditorViewModel, edit: EditorViewModel.TextEdit) {
    val ex = edit.existing
    var text by remember(edit) { mutableStateOf(ex?.text ?: "") }
    var fontSize by remember(edit) { mutableFloatStateOf(ex?.size ?: vm.textSize) }
    var color by remember(edit) { mutableIntStateOf(ex?.color ?: vm.textColor ?: vm.penStyle.color) }
    var style by remember(edit) { mutableStateOf(ex?.style ?: vm.textStyle) }
    var colorEdit by remember { mutableStateOf<Int?>(null) }
    // Mode 0: Write Box Pad + Live Preview, Mode 1: Direct Keyboard Editor
    var inputMode by remember { mutableIntStateOf(0) }
    val padStrokes = remember { mutableStateListOf<List<Offset>>() }
    var activeStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val padScope = rememberCoroutineScope()

    fun convertPadStrokes() {
        if (padStrokes.isEmpty()) return
        val snapshot = padStrokes.toList()
        padStrokes.clear()
        activeStroke = emptyList()
        var runningT = 0L
        val timed = snapshot.map { pts ->
            val list = pts.mapIndexed { idx, pt ->
                TimedStrokePoint(pt.x, pt.y, runningT + idx * 14L)
            }
            runningT = (list.lastOrNull()?.tMs ?: runningT) + 60L
            list
        }
        padScope.launch {
            val result = vm.recognizeHandwritingPad(timed, width = 640f, height = 220f, preContext = text)
            val recognized = result.text.trim()
            if (recognized.isNotBlank()) {
                text = if (text.isBlank()) recognized else "$text $recognized"
            }
        }
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
                                onClick = { text = "$text " },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            ) { Text("Space", fontSize = 11.sp) }
                            TextButton(
                                onClick = { text = if (text.isEmpty()) "\n" else "$text\n" },
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
                                        val pts = mutableListOf<Offset>()
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
                                    "Write words here — tap Convert Strokes or Done to turn ink into neat text",
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
                    FmtToggle(Icons.Outlined.FormatBold, style.bold, "Bold") { style = style.copy(bold = !style.bold) }
                    FmtToggle(Icons.Outlined.FormatItalic, style.italic, "Italic") { style = style.copy(italic = !style.italic) }
                    FmtToggle(Icons.Outlined.FormatUnderlined, style.underline, "Underline") { style = style.copy(underline = !style.underline) }
                    FmtToggle(Icons.AutoMirrored.Outlined.FormatAlignLeft, style.align == TextAlign.LEFT, "Align left") {
                        style = style.copy(align = TextAlign.LEFT)
                    }
                    FmtToggle(Icons.Outlined.FormatAlignCenter, style.align == TextAlign.CENTER, "Align centre") {
                        style = style.copy(align = TextAlign.CENTER)
                    }
                    FmtToggle(Icons.AutoMirrored.Outlined.FormatAlignRight, style.align == TextAlign.RIGHT, "Align right") {
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
                ) { Text("Place as Write Box") }
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
private fun FmtToggle(icon: ImageVector, on: Boolean, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(2.dp)
            .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
            .clip(CircleShape)
            .background(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Full-page Keyboard Mode overlay — allows users to type structured document notes
 * directly onto the current page using a hardware or on-screen keyboard, complete with
 * headings, checklists, bullet lists, font families, and automatic page persistence.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KeyboardNoteModeOverlay(vm: EditorViewModel, darkUi: Boolean) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProIconView(ProIcon.KEYBOARD, ProButtonPalette.ElectricBlue, Modifier.size(22.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Keyboard Mode · Page ${vm.pageIndex + 1}",
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
                TextButton(onClick = { vm.saveKeyboardPageText(bodyText) }) {
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
                FmtToggle(Icons.Outlined.FormatBold, style.bold, "Bold") {
                    style = style.copy(bold = !style.bold)
                    vm.textStyle = style
                }
                FmtToggle(Icons.Outlined.FormatItalic, style.italic, "Italic") {
                    style = style.copy(italic = !style.italic)
                    vm.textStyle = style
                }
                FmtToggle(Icons.Outlined.FormatUnderlined, style.underline, "Underline") {
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

            OutlinedTextField(
                value = bodyText,
                onValueChange = {
                    bodyText = it
                    vm.saveKeyboardPageText(it)
                },
                placeholder = {
                    Text(
                        "Start typing your notes here with the keyboard…\n• Use • Bullet, ☐ Task, or H1/H2 above\n• Everything you type is saved directly onto Page ${vm.pageIndex + 1} so you can also annotate over it with the stylus.",
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StudyTimer(
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
                .defaultMinSize(minHeight = 48.dp)
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
internal fun WebLinkDialog(vm: EditorViewModel, edit: EditorViewModel.WebLinkEdit) {
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
                Text(if (edit.isNew) "Add Link Preview" else "Link Preview", Modifier.weight(1f))
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
                    placeholder = { Text("Uses domain automatically if empty") },
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
