package com.pronotes.app.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextPaint
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Keyboard
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pronotes.app.data.AppSettings
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppContextMenu
import com.pronotes.app.ui.AppDialog
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
import kotlinx.coroutines.Job
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
internal fun StickerSheet(onDragStarted: () -> Unit, onPick: (Bitmap) -> Unit) {
    val view = androidx.compose.ui.platform.LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var mine by remember { mutableStateOf(StickerLibrary.custom(context)) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val imported = withContext(Dispatchers.IO) { StickerLibrary.import(context, uri, autoRemoveBg = true) }
                if (imported == null) {
                    Toast.makeText(context, "Could not import sticker image.", Toast.LENGTH_SHORT).show()
                } else {
                    mine = StickerLibrary.custom(context)
                    tab = 2
                    val msg = if (StickerLibrary.lastImportCutout) "Subject detected — background removed!" else "Sticker imported!"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
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
                        .pointerInput(st) {
                            detectTapGestures(onTap = { pick(st) }, onLongPress = {
                                val image = preview ?: return@detectTapGestures
                                val shadow = object : android.view.View.DragShadowBuilder(view) {
                                    override fun onProvideShadowMetrics(size: android.graphics.Point, touch: android.graphics.Point) {
                                        size.set(image.width, image.height)
                                        touch.set(image.width / 2, image.height / 2)
                                    }
                                    override fun onDrawShadow(canvas: android.graphics.Canvas) { canvas.drawBitmap(image, 0f, 0f, null) }
                                }
                                val code = when (st) {
                                    is Sticker.Builtin -> "b:" + st.id
                                    is Sticker.Emoji -> "e:" + st.text
                                    is Sticker.Custom -> "c:" + st.file.name
                                }
                                val clip = android.content.ClipData("ProNotes sticker", arrayOf("application/vnd.pronotes.sticker"), android.content.ClipData.Item(code))
                                if (view.startDragAndDrop(clip, shadow, st, android.view.View.DRAG_FLAG_GLOBAL)) onDragStarted()
                            })
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    preview?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize().padding(6.dp)) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Wraps long paragraphs at word boundaries so committed canvas [TextItem] lines
 * never overflow outside the inline text box or page margin width.
 */
private fun wrapTextToBoxWidth(
    raw: String,
    maxWidthPageUnits: Float,
    fontSize: Float,
    style: TextStyleSpec,
): String {
    if (raw.isEmpty() || maxWidthPageUnits <= fontSize * 2f) return raw
    val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)
    applyTextStyle(paint, fontSize, style)
    val safeWidth = (maxWidthPageUnits - 12f).coerceAtLeast(fontSize * 2f)
    val out = StringBuilder(raw.length + 16)
    val paragraphs = raw.split('\n')
    paragraphs.forEachIndexed { pIdx, para ->
        if (pIdx > 0) out.append('\n')
        if (para.isEmpty() || paint.measureText(para) <= safeWidth) {
            out.append(para)
        } else {
            var start = 0
            while (start < para.length) {
                val count = paint.breakText(para, start, para.length, true, safeWidth, null)
                if (count <= 0) {
                    out.append(para[start])
                    start++
                    continue
                }
                var end = start + count
                if (end < para.length) {
                    val lastSpace = para.lastIndexOf(' ', end - 1)
                    if (lastSpace > start) {
                        end = lastSpace
                    }
                }
                if (start > 0) out.append('\n')
                out.append(para.substring(start, end).trimEnd())
                start = end
                while (start < para.length && para[start] == ' ') start++
            }
        }
    }
    return out.toString()
}

private enum class BoxResizeHandle {
    TL, T, TR, R, BR, B, BL, L
}

/**
 * Unified direct-on-page inline text & keyboard editor used by BOTH the Text tool and
 * full-page Keyboard Mode.
 *
 * Renders a completely transparent, resizable text box directly aligned over the live notebook
 * paper using a native Android [EditText] (supporting hardware/soft keyboard typing and native
 * API 33+ stylus-to-text handwriting directly in place), paired with a 1-tap transparent ML Kit
 * handwriting capture toggle and a slim formatting bar docked cleanly above the soft keyboard.
 */
@Composable
internal fun InlinePageTextEditor(
    vm: EditorViewModel,
    edit: EditorViewModel.TextEdit,
    canvas: NoteCanvasView?,
    areaSize: IntSize,
    darkUi: Boolean,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val page = vm.pages.getOrNull(edit.page)
    val pageW = page?.width ?: 1200f
    val pageH = page?.height ?: 1600f

    val ex = edit.existing
    var text by remember(edit) { mutableStateOf(ex?.text ?: "") }
    var fontSize by remember(edit) { mutableFloatStateOf(ex?.size ?: vm.textSize) }
    var textColor by remember(edit) { mutableIntStateOf(ex?.color ?: vm.textColor ?: vm.penStyle.color) }
    var style by remember(edit) { mutableStateOf(ex?.style ?: vm.textStyle) }
    var boxX by remember(edit) { mutableFloatStateOf(edit.x) }
    var boxY by remember(edit) { mutableFloatStateOf(edit.y) }
    var boxW by remember(edit) { mutableFloatStateOf(edit.boxWidth) }
    var boxH by remember(edit) { mutableFloatStateOf(edit.boxHeight) }

    var showColorPicker by remember { mutableStateOf(false) }
    var handwriteMode by remember(edit) { mutableStateOf(false) }
    var editTextRef by remember { mutableStateOf<EditText?>(null) }
    var suppressWatcher by remember { mutableStateOf(false) }

    // Inline transparent ML Kit handwriting capture state (universal fallback / stylus ink toggle)
    val inkStrokes = remember(edit) { mutableStateListOf<List<TimedStrokePoint>>() }
    var activeInkStroke by remember(edit) { mutableStateOf<List<TimedStrokePoint>>(emptyList()) }
    var convertJob by remember { mutableStateOf<Job?>(null) }
    var candidates by remember(edit) { mutableStateOf<List<String>>(emptyList()) }
    var lastInsertedRange by remember(edit) { mutableStateOf<Pair<Int, Int>?>(null) }

    fun hideSoftKeyboard() {
        val et = editTextRef ?: return
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(et.windowToken, 0)
    }

    fun showSoftKeyboard() {
        val et = editTextRef ?: return
        et.requestFocus()
        et.post {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    var showHandwritingStudio by remember { mutableStateOf(false) }

    fun commitAsHandwriting() {
        convertJob?.cancel()
        hideSoftKeyboard()
        val liveText = editTextRef?.text?.toString() ?: text
        if (liveText.isBlank()) {
            showHandwritingStudio = true
            return
        }
        vm.commitTextAsHandwriting(
            text = liveText,
            size = fontSize,
            color = textColor,
            x = boxX,
            y = boxY,
            maxWidth = boxW,
        )
    }

    fun commitAndClose() {
        convertJob?.cancel()
        hideSoftKeyboard()
        val liveText = editTextRef?.text?.toString() ?: text
        val wrapped = wrapTextToBoxWidth(liveText, boxW, fontSize, style)
        vm.commitText(
            text = wrapped,
            size = fontSize,
            color = textColor,
            style = style,
            x = boxX,
            y = boxY,
        )
    }

    fun deleteAndClose() {
        convertJob?.cancel()
        hideSoftKeyboard()
        if (ex != null) {
            vm.commitText("", fontSize, textColor, style, boxX, boxY)
        } else {
            vm.dismissTextEdit()
        }
    }

    fun insertPrefixAtCursorLine(prefix: String) {
        val et = editTextRef
        val current = et?.text?.toString() ?: text
        val sel = (et?.selectionStart ?: current.length).coerceIn(0, current.length)
        val lineStart = current.lastIndexOf('\n', (sel - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val updated = current.substring(0, lineStart) + prefix + current.substring(lineStart)
        val newCursor = (sel + prefix.length).coerceIn(0, updated.length)
        text = updated
        if (et != null) {
            suppressWatcher = true
            et.setText(updated)
            et.setSelection(newCursor)
            suppressWatcher = false
        }
    }

    fun insertRecognizedWord(word: String, alts: List<String>) {
        val clean = word.trim()
        if (clean.isEmpty()) return
        val et = editTextRef
        val current = et?.text?.toString() ?: text
        val selStart = (et?.selectionStart ?: current.length).coerceIn(0, current.length)
        val selEnd = (et?.selectionEnd ?: selStart).coerceIn(selStart, current.length)
        val needsSpace = selStart > 0 && !current[selStart - 1].isWhitespace()
        val insertion = if (needsSpace) " $clean" else clean
        val updated = current.substring(0, selStart) + insertion + current.substring(selEnd)
        val wordStart = selStart + if (needsSpace) 1 else 0
        val wordEnd = wordStart + clean.length
        text = updated
        lastInsertedRange = wordStart to wordEnd
        candidates = alts.filter { it.isNotBlank() }.distinct().take(4)
        if (et != null) {
            suppressWatcher = true
            et.setText(updated)
            et.setSelection(wordEnd.coerceIn(0, updated.length))
            suppressWatcher = false
        }
    }

    fun replaceLastCandidate(replacement: String) {
        val range = lastInsertedRange ?: return
        val et = editTextRef
        val current = et?.text?.toString() ?: text
        val start = range.first.coerceIn(0, current.length)
        val end = range.second.coerceIn(start, current.length)
        val updated = current.substring(0, start) + replacement + current.substring(end)
        val newEnd = start + replacement.length
        text = updated
        lastInsertedRange = start to newEnd
        if (et != null) {
            suppressWatcher = true
            et.setText(updated)
            et.setSelection(newEnd.coerceIn(0, updated.length))
            suppressWatcher = false
        }
    }

    fun flushInlineInk(canvasW: Float, canvasH: Float) {
        if (inkStrokes.isEmpty()) return
        val snapshot = inkStrokes.toList()
        inkStrokes.clear()
        activeInkStroke = emptyList()
        scope.launch {
            val res = vm.recognizeHandwritingPad(
                strokes = snapshot,
                width = canvasW.coerceAtLeast(160f),
                height = canvasH.coerceAtLeast(100f),
                preContext = text.takeLast(24),
            )
            if (res.text.isNotBlank()) {
                insertRecognizedWord(res.text, res.candidates)
            }
        }
    }

    // Hook into ViewModel so toggling Keyboard Mode off in top bar commits cleanly
    DisposableEffect(edit) {
        vm.onRequestCommitText = { commitAndClose() }
        onDispose {
            if (vm.onRequestCommitText != null) vm.onRequestCommitText = null
        }
    }

    BackHandler(enabled = !showColorPicker && !showHandwritingStudio) {
        commitAndClose()
    }

    val zoomScale = (canvas?.currentZoomScale() ?: 1f).coerceIn(0.3f, 4f)
    val mappedRect = remember(canvas, edit.page, boxX, boxY, boxW, boxH, areaSize) {
        canvas?.pageToScreen(edit.page, RectF(boxX, boxY, boxX + boxW, boxY + boxH))
    }

    val barReservePx = with(density) { (if (candidates.isNotEmpty()) 112.dp else 68.dp).toPx() }
    val topMarginPx = with(density) { 12.dp.toPx() }
    val sideMarginPx = with(density) { 12.dp.toPx() }
    val handleSizeDp = 12.dp
    val handleHalfPx = with(density) { (handleSizeDp / 2f).toPx() }
    val handleTouchPx = with(density) { 28.dp.toPx() }

    val availW = areaSize.width.toFloat().coerceAtLeast(240f)
    val availH = (areaSize.height.toFloat() - barReservePx).coerceAtLeast(140f)

    val maxEditorWidth = (availW - sideMarginPx * 2f).coerceAtLeast(1f)
    val maxEditorHeight = (availH - topMarginPx * 2f).coerceAtLeast(1f)
    val rawW = (mappedRect?.width() ?: (boxW * zoomScale)).coerceIn(minOf(with(density) { 130.dp.toPx() }, maxEditorWidth), maxEditorWidth)
    val rawH = (mappedRect?.height() ?: (boxH * zoomScale)).coerceIn(minOf(with(density) { 58.dp.toPx() }, maxEditorHeight), maxEditorHeight)
    val screenLeft = (mappedRect?.left ?: ((availW - rawW) * 0.5f))
        .coerceIn(sideMarginPx, (availW - rawW - sideMarginPx).coerceAtLeast(sideMarginPx))
    val screenTop = (mappedRect?.top ?: (topMarginPx * 2f))
        .coerceIn(topMarginPx, (availH - rawH - topMarginPx).coerceAtLeast(topMarginPx))

    val boxWidthDp = with(density) { rawW.toDp() }
    val boxHeightDp = with(density) { rawH.toDp() }

    val barBg = if (darkUi) Color(0xF51D2029) else Color(0xFAFFFFFF)
    val barFg = if (darkUi) Color(0xFFF3F5F8) else Color(0xFF1A1F28)
    val barBorder = if (darkUi) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f)
    val frameBlue = ProButtonPalette.ElectricBlue

    Box(Modifier.fillMaxSize()) {
        // 1) Transparent backdrop — tapping anywhere outside the inline box commits cleanly
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(edit) {
                    detectTapGestures { commitAndClose() }
                },
        )

        // 2) Top floating move/delete pill directly above the text box
        if (!edit.isFullPage) {
        val pillY = (screenTop - with(density) { 38.dp.toPx() }).coerceAtLeast(with(density) { 6.dp.toPx() })
        val pillX = screenLeft.coerceIn(sideMarginPx, (availW - with(density) { 148.dp.toPx() }).coerceAtLeast(sideMarginPx))
        Row(
            modifier = Modifier
                .offset { IntOffset(pillX.roundToInt(), pillY.roundToInt()) }
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(barBg)
                .border(1.dp, barBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (!edit.isFullPage) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(zoomScale) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                boxX = (boxX + dragAmount.x / zoomScale).coerceIn(16f, (pageW - boxW - 16f).coerceAtLeast(16f))
                                boxY = (boxY + dragAmount.y / zoomScale).coerceIn(16f, (pageH - boxH - 16f).coerceAtLeast(16f))
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(Icons.Outlined.DragIndicator, "Move text box", Modifier.size(15.dp), tint = frameBlue)
                    Text("Move", style = MaterialTheme.typography.labelSmall, color = barFg, fontWeight = FontWeight.Medium)
                }
            } else {
                Text(
                    "Page ${edit.page + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = frameBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Box(Modifier.width(1.dp).height(14.dp).background(barBorder))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { deleteAndClose() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Icon(Icons.Outlined.DeleteOutline, "Delete text", Modifier.size(16.dp), tint = Color(0xFFE5484D))
            }
        }

        }
        // 3) Transparent, resizable inline text box directly over the notebook paper
        Box(
            modifier = Modifier
                .offset { IntOffset(screenLeft.roundToInt(), screenTop.roundToInt()) }
                .size(boxWidthDp, boxHeightDp)
                .border(
                    width = if (edit.isFullPage) 1.25.dp else 1.5.dp,
                    color = if (edit.isFullPage) Color.Transparent else frameBlue,
                    shape = RoundedCornerShape(4.dp),
                )
                .background(Color.Transparent)
                .pointerInput(Unit) {
                    // Consume taps inside the box so backdrop doesn't dismiss it
                    detectTapGestures {
                        if (!handwriteMode) showSoftKeyboard()
                    }
                },
        ) {
            // Native Android EditText: 100% transparent, supports soft/hardware keyboard AND native API 33+ stylus-to-text
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                factory = { ctx ->
                    EditText(ctx).apply {
                        background = null
                        setPadding(4, 2, 4, 2)
                        gravity = Gravity.TOP or Gravity.START
                        inputType = InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        isSingleLine = false
                        isFocusable = true
                        isFocusableInTouchMode = true
                        isVerticalScrollBarEnabled = true
                        hint = if (edit.isFullPage) null else "Type or write…"
                        setHintTextColor(if (darkUi) 0x77CBD5E1 else 0x6664748B)
                        if (Build.VERSION.SDK_INT >= 33) {
                            isAutoHandwritingEnabled = true
                        }
                        setText(text)
                        setSelection(text.length)
                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                            override fun afterTextChanged(s: Editable?) {
                                if (!suppressWatcher) {
                                    text = s?.toString() ?: ""
                                }
                            }
                        })
                        editTextRef = this
                        post {
                            requestFocus()
                            if (!handwriteMode) {
                                val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                            }
                        }
                    }
                },
                update = { et ->
                    et.typeface = style.resolvedTypeface()
                    et.paint.isUnderlineText = style.underline
                    et.setTextSize(TypedValue.COMPLEX_UNIT_PX, (fontSize * zoomScale).coerceIn(13f, 220f))
                    et.setTextColor(textColor)
                    et.gravity = Gravity.TOP or when (style.align) {
                        TextAlign.LEFT -> Gravity.START
                        TextAlign.CENTER -> Gravity.CENTER_HORIZONTAL
                        TextAlign.RIGHT -> Gravity.END
                    }
                    et.setLineSpacing(0f, 1.15f)
                },
            )

            // Optional transparent ML Kit inline handwriting overlay when user taps "Write"
            if (handwriteMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(frameBlue.copy(alpha = 0.03f))
                        .pointerInput(rawW, rawH) {
                            awaitEachGesture {
                                val pts = mutableListOf<TimedStrokePoint>()
                                convertJob?.cancel()
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val ch = ev.changes.firstOrNull() ?: break
                                    if (ch.pressed) {
                                        pts.add(TimedStrokePoint(ch.position.x, ch.position.y, System.currentTimeMillis()))
                                        activeInkStroke = pts.toList()
                                        ch.consume()
                                    } else {
                                        ch.consume()
                                        break
                                    }
                                }
                                if (pts.size >= 2) {
                                    inkStrokes.add(pts.toList())
                                }
                                activeInkStroke = emptyList()
                                if (inkStrokes.isNotEmpty()) {
                                    convertJob?.cancel()
                                    convertJob = scope.launch {
                                        delay(720L)
                                        flushInlineInk(rawW, rawH)
                                    }
                                }
                            }
                        },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val strokeCol = Color(textColor)
                        fun drawStroke(pts: List<TimedStrokePoint>) {
                            if (pts.size < 2) return
                            val path = Path().apply {
                                moveTo(pts.first().x, pts.first().y)
                                for (i in 1 until pts.size) {
                                    val prev = pts[i - 1]
                                    val cur = pts[i]
                                    quadraticTo(prev.x, prev.y, (prev.x + cur.x) * 0.5f, (prev.y + cur.y) * 0.5f)
                                }
                            }
                            drawPath(path, strokeCol, style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round))
                        }
                        inkStrokes.forEach { drawStroke(it) }
                        drawStroke(activeInkStroke)
                    }
                }
            }
        }

        // 4) 8 crisp square resize handles (4 corners + 4 midpoints) aligned to the box border
        val activeHandles = if (edit.isFullPage) {
            emptyList()
        } else {
            BoxResizeHandle.entries
        }
        val minBoxW = 160f
        val minBoxH = 72f
        activeHandles.forEach { handle ->
            val hx = when (handle) {
                BoxResizeHandle.TL, BoxResizeHandle.L, BoxResizeHandle.BL -> screenLeft
                BoxResizeHandle.T, BoxResizeHandle.B -> screenLeft + rawW * 0.5f
                BoxResizeHandle.TR, BoxResizeHandle.R, BoxResizeHandle.BR -> screenLeft + rawW
            }
            val hy = when (handle) {
                BoxResizeHandle.TL, BoxResizeHandle.T, BoxResizeHandle.TR -> screenTop
                BoxResizeHandle.L, BoxResizeHandle.R -> screenTop + rawH * 0.5f
                BoxResizeHandle.BL, BoxResizeHandle.B, BoxResizeHandle.BR -> screenTop + rawH
            }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (hx - handleTouchPx * 0.5f).roundToInt(),
                            (hy - handleTouchPx * 0.5f).roundToInt(),
                        )
                    }
                    .size(with(density) { handleTouchPx.toDp() })
                    .pointerInput(handle, zoomScale) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            val dx = drag.x / zoomScale
                            val dy = drag.y / zoomScale
                            when (handle) {
                                BoxResizeHandle.R -> {
                                    boxW = (boxW + dx).coerceIn(minBoxW, (pageW - boxX - 16f).coerceAtLeast(minBoxW))
                                }
                                BoxResizeHandle.L -> {
                                    val newX = (boxX + dx).coerceIn(16f, (boxX + boxW - minBoxW).coerceAtLeast(16f))
                                    boxW += (boxX - newX)
                                    boxX = newX
                                }
                                BoxResizeHandle.B -> {
                                    boxH = (boxH + dy).coerceIn(minBoxH, (pageH - boxY - 16f).coerceAtLeast(minBoxH))
                                }
                                BoxResizeHandle.T -> {
                                    val newY = (boxY + dy).coerceIn(16f, (boxY + boxH - minBoxH).coerceAtLeast(16f))
                                    boxH += (boxY - newY)
                                    boxY = newY
                                }
                                BoxResizeHandle.BR -> {
                                    boxW = (boxW + dx).coerceIn(minBoxW, (pageW - boxX - 16f).coerceAtLeast(minBoxW))
                                    boxH = (boxH + dy).coerceIn(minBoxH, (pageH - boxY - 16f).coerceAtLeast(minBoxH))
                                }
                                BoxResizeHandle.TR -> {
                                    boxW = (boxW + dx).coerceIn(minBoxW, (pageW - boxX - 16f).coerceAtLeast(minBoxW))
                                    val newY = (boxY + dy).coerceIn(16f, (boxY + boxH - minBoxH).coerceAtLeast(16f))
                                    boxH += (boxY - newY)
                                    boxY = newY
                                }
                                BoxResizeHandle.BL -> {
                                    val newX = (boxX + dx).coerceIn(16f, (boxX + boxW - minBoxW).coerceAtLeast(16f))
                                    boxW += (boxX - newX)
                                    boxX = newX
                                    boxH = (boxH + dy).coerceIn(minBoxH, (pageH - boxY - 16f).coerceAtLeast(minBoxH))
                                }
                                BoxResizeHandle.TL -> {
                                    val newX = (boxX + dx).coerceIn(16f, (boxX + boxW - minBoxW).coerceAtLeast(16f))
                                    boxW += (boxX - newX)
                                    boxX = newX
                                    val newY = (boxY + dy).coerceIn(16f, (boxY + boxH - minBoxH).coerceAtLeast(16f))
                                    boxH += (boxY - newY)
                                    boxY = newY
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(handleSizeDp)
                        .background(Color.White, RoundedCornerShape(2.dp))
                        .border(1.5.dp, frameBlue, RoundedCornerShape(2.dp)),
                )
            }
            @Suppress("UNUSED_VARIABLE")
            val unusedHalf = handleHalfPx
        }

        // 5) Slim keyboard-docked formatting bar (inside imePadding, rides smoothly above keyboard)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (candidates.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(barBg)
                        .border(1.dp, barBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    candidates.forEachIndexed { idx, word ->
                        AppChip(
                            selected = idx == 0,
                            onClick = { replaceLastCandidate(word) },
                            label = word,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(barBg)
                    .border(1.dp, barBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Toggle between Keyboard & transparent inline ML Kit Stylus Handwriting capture
                    AppPill(
                        selected = handwriteMode,
                        tint = frameBlue,
                        onClick = {
                            handwriteMode = !handwriteMode
                            if (handwriteMode) {
                                hideSoftKeyboard()
                            } else {
                                if (inkStrokes.isNotEmpty()) flushInlineInk(rawW, rawH)
                                showSoftKeyboard()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            if (handwriteMode) Icons.Outlined.Keyboard else Icons.Outlined.Edit,
                            contentDescription = if (handwriteMode) "Switch to keyboard" else "Write with stylus",
                            modifier = Modifier.size(15.dp),
                            tint = if (handwriteMode) Color.White else barFg,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (handwriteMode) "Keyboard" else "Write",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (handwriteMode) Color.White else barFg,
                        )
                    }

                    // Quick structured text blocks
                    InlineFmtChip("H1", false, barFg) { insertPrefixAtCursorLine("# ") }
                    InlineFmtChip("H2", false, barFg) { insertPrefixAtCursorLine("## ") }
                    InlineFmtChip("•", false, barFg) { insertPrefixAtCursorLine("• ") }
                    InlineFmtChip("☐", false, barFg) { insertPrefixAtCursorLine("☐ ") }
                    InlineFmtChip("1.", false, barFg) { insertPrefixAtCursorLine("1. ") }

                    Box(Modifier.width(1.dp).height(18.dp).background(barBorder))

                    // Bold / Italic / Underline
                    InlineFmtIcon(Icons.Outlined.FormatBold, style.bold, "Bold", barFg) {
                        style = style.copy(bold = !style.bold)
                    }
                    InlineFmtIcon(Icons.Outlined.FormatItalic, style.italic, "Italic", barFg) {
                        style = style.copy(italic = !style.italic)
                    }
                    InlineFmtIcon(Icons.Outlined.FormatUnderlined, style.underline, "Underline", barFg) {
                        style = style.copy(underline = !style.underline)
                    }

                    // Alignment cycle
                    val alignIcon = when (style.align) {
                        TextAlign.LEFT -> Icons.AutoMirrored.Outlined.FormatAlignLeft
                        TextAlign.CENTER -> Icons.Outlined.FormatAlignCenter
                        TextAlign.RIGHT -> Icons.AutoMirrored.Outlined.FormatAlignRight
                    }
                    InlineFmtIcon(alignIcon, false, "Alignment", barFg) {
                        val nextAlign = when (style.align) {
                            TextAlign.LEFT -> TextAlign.CENTER
                            TextAlign.CENTER -> TextAlign.RIGHT
                            TextAlign.RIGHT -> TextAlign.LEFT
                        }
                        style = style.copy(align = nextAlign)
                    }

                    // Font family cycle
                    InlineFmtChip(style.font.label, false, barFg) {
                        val entries = TextFont.entries
                        val nextIdx = (entries.indexOf(style.font) + 1) % entries.size
                        style = style.copy(font = entries[nextIdx])
                    }

                    // Size stepper
                    InlineFmtChip("A−", false, barFg) { fontSize = (fontSize - 3f).coerceAtLeast(16f) }
                    Text(
                        "${fontSize.roundToInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = barFg.copy(alpha = 0.78f),
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                    InlineFmtChip("A+", false, barFg) { fontSize = (fontSize + 3f).coerceAtMost(120f) }

                    // Train personal handwriting library
                    InlineFmtChip("Train", false, barFg) {
                        hideSoftKeyboard()
                        showHandwritingStudio = true
                    }

                    // Colour dot
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { showColorPicker = true }
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Color(textColor))
                            .border(1.5.dp, barFg.copy(alpha = 0.35f), CircleShape),
                    )
                }

                // Convert typed text into Personal Handwriting Ink strokes
                AppPill(
                    selected = text.isNotBlank(),
                    tint = ProButtonPalette.Emerald,
                    onClick = { commitAsHandwriting() },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        "My Ink",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (text.isNotBlank()) Color.White else barFg,
                    )
                }

                // Commit / Done button
                AppPill(
                    selected = true,
                    tint = frameBlue,
                    onClick = { commitAndClose() },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        "Done",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }

    if (showColorPicker) {
        ColorEditorDialog(
            initial = textColor,
            presets = PenPalette,
            recent = vm.recentColors(),
            onDismiss = { showColorPicker = false },
        ) { c ->
            textColor = c
            vm.pushRecent(c)
            showColorPicker = false
        }
    }

    if (showHandwritingStudio) {
        HandwritingStudioDialog(onDismiss = { showHandwritingStudio = false })
    }
}

@Composable
private fun InlineFmtChip(
    label: String,
    active: Boolean,
    fg: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 30.dp, minHeight = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) ProButtonPalette.ElectricBlue.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (active) ProButtonPalette.ElectricBlue else fg,
        )
    }
}

@Composable
private fun InlineFmtIcon(
    icon: ImageVector,
    active: Boolean,
    description: String,
    fg: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) ProButtonPalette.ElectricBlue.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(17.dp),
            tint = if (active) ProButtonPalette.ElectricBlue else fg,
        )
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
