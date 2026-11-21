package com.pronotes.app.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.ProButtonPalette

private enum class StudioTab(val label: String, val chars: List<String>) {
    LOWER("a–z", HandwritingFontChars.LOWER),
    UPPER("A–Z", HandwritingFontChars.UPPER),
    DIGITS("0–9", HandwritingFontChars.DIGITS),
    PUNCT(". , ? !", HandwritingFontChars.PUNCT),
    PREVIEW("Preview", emptyList()),
}

@Composable
fun HandwritingStudioDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { HandwritingFontStore.get(context) }
    val profile by store.profile.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(StudioTab.LOWER) }
    var selectedChar by remember { mutableStateOf(HandwritingFontChars.LOWER.first()) }
    var confirmReset by remember { mutableStateOf(false) }

    val trainedChars = profile.trainedCharCount()
    val totalChars = HandwritingFontChars.ALL.size
    val totalSamples = profile.totalSampleCount()
    val maxSamples = totalChars * HandwritingGuides.COPIES_PER_CHAR
    val progress = (totalSamples.toFloat() / maxSamples.coerceAtLeast(1)).coerceIn(0f, 1f)

    val cs = MaterialTheme.colorScheme
    val accent = ProButtonPalette.ElectricBlue
    val doneGreen = ProButtonPalette.Emerald

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 880.dp)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(24.dp),
            color = cs.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "My Handwriting Style",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface,
                        )
                        Text(
                            "$trainedChars / $totalChars characters · $totalSamples / $maxSamples samples saved offline",
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurfaceVariant,
                        )
                    }
                    if (totalSamples > 0) {
                        TextButton(onClick = { confirmReset = true }) {
                            Text("Reset all", color = cs.error)
                        }
                    }
                    AppPill(
                        selected = true,
                        tint = accent,
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            "Done",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (trainedChars >= totalChars) doneGreen else accent,
                    trackColor = cs.surfaceVariant,
                )

                // Category tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StudioTab.entries.forEach { tab ->
                        val tabDone = if (tab.chars.isEmpty()) "" else {
                            val count = tab.chars.count { profile.recordedCount(it) > 0 }
                            " ($count/${tab.chars.size})"
                        }
                        AppChip(
                            selected = activeTab == tab,
                            onClick = {
                                activeTab = tab
                                if (tab.chars.isNotEmpty() && selectedChar !in tab.chars) {
                                    selectedChar = tab.chars.first()
                                }
                            },
                            label = "${tab.label}$tabDone",
                        )
                    }
                }

                HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.5f))

                // Body
                if (activeTab == StudioTab.PREVIEW) {
                    HandwritingPreviewPane(
                        profile = profile,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                    )
                } else {
                    val chars = activeTab.chars
                    val currentIdx = chars.indexOf(selectedChar).coerceAtLeast(0)
                    val currentChar = chars[currentIdx]
                    val slots = profile.slotsFor(currentChar)

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Character selector strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            chars.forEach { ch ->
                                val recorded = profile.recordedCount(ch)
                                val sel = ch == currentChar
                                val borderColor = when {
                                    sel -> accent
                                    recorded >= HandwritingGuides.COPIES_PER_CHAR -> doneGreen.copy(alpha = 0.65f)
                                    recorded > 0 -> accent.copy(alpha = 0.45f)
                                    else -> cs.outlineVariant.copy(alpha = 0.6f)
                                }
                                val bgColor = when {
                                    sel -> accent.copy(alpha = 0.14f)
                                    recorded >= HandwritingGuides.COPIES_PER_CHAR -> doneGreen.copy(alpha = 0.08f)
                                    else -> cs.surfaceVariant.copy(alpha = 0.45f)
                                }
                                Column(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(bgColor)
                                        .border(if (sel) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
                                        .clickable { selectedChar = ch }
                                        .padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        ch,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (sel) accent else cs.onSurface,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        repeat(HandwritingGuides.COPIES_PER_CHAR) { dot ->
                                            Box(
                                                Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (dot < recorded) {
                                                            if (recorded >= HandwritingGuides.COPIES_PER_CHAR) doneGreen else accent
                                                        } else {
                                                            cs.onSurface.copy(alpha = 0.18f)
                                                        },
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Active character controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Write '$currentChar' 3 times on the blue baseline",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = cs.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (profile.recordedCount(currentChar) > 0) {
                                TextButton(onClick = { store.clearChar(currentChar) }) {
                                    Text("Clear 3")
                                }
                            }
                            IconButton(
                                onClick = {
                                    val prev = (currentIdx - 1 + chars.size) % chars.size
                                    selectedChar = chars[prev]
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Previous character")
                            }
                            IconButton(
                                onClick = {
                                    val next = (currentIdx + 1) % chars.size
                                    selectedChar = chars[next]
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Next character")
                            }
                        }

                        // Three live writing boxes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            repeat(HandwritingGuides.COPIES_PER_CHAR) { slotIndex ->
                                GlyphWritingBox(
                                    charKey = currentChar,
                                    slotIndex = slotIndex,
                                    sample = slots[slotIndex],
                                    onSave = { strokes, w, h ->
                                        store.saveSample(currentChar, slotIndex, strokes, w, h)
                                    },
                                    onClear = { store.clearSample(currentChar, slotIndex) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        Text(
                            "Write naturally across all 3 boxes. When you type and choose My Ink, Pro Notes picks a random copy each time and varies size, tilt, and spacing so no two letters repeat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (confirmReset) {
        AppDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset handwriting library?") },
            text = { Text("This clears all saved character samples so you can record them fresh.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.clearAll()
                        confirmReset = false
                    },
                ) {
                    Text("Reset", color = cs.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun GlyphWritingBox(
    charKey: String,
    slotIndex: Int,
    sample: GlyphSample,
    onSave: (List<List<Pair<Float, Float>>>, Float, Float) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val inkColor = cs.onSurface
    val baselineColor = ProButtonPalette.ElectricBlue.copy(alpha = 0.55f)
    val guideColor = cs.onSurface.copy(alpha = 0.14f)
    val recorded = sample.isRecorded

    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var activeStroke by remember(charKey, slotIndex) { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Copy ${slotIndex + 1}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (recorded) ProButtonPalette.Emerald else cs.onSurfaceVariant,
            )
            if (recorded) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Clear copy ${slotIndex + 1}",
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClear),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(cs.surfaceVariant.copy(alpha = 0.35f))
                .border(
                    width = if (recorded) 1.5.dp else 1.dp,
                    color = if (recorded) ProButtonPalette.Emerald.copy(alpha = 0.65f) else cs.outlineVariant,
                    shape = RoundedCornerShape(16.dp),
                )
                .onSizeChanged { boxSize = it },
        ) {
            // Subtle watermark guide character behind ink
            Text(
                text = charKey,
                fontSize = 68.sp,
                fontWeight = FontWeight.Light,
                color = cs.onSurface.copy(alpha = if (recorded) 0.05f else 0.11f),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(charKey, slotIndex, sample) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val pts = ArrayList<Pair<Float, Float>>(32)
                            pts += down.position.x to down.position.y
                            activeStroke = pts.toList()

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.pressed) {
                                    change.consume()
                                    val pos = change.position
                                    val last = pts.lastOrNull()
                                    if (last == null || kotlin.math.hypot((pos.x - last.first).toDouble(), (pos.y - last.second).toDouble()) >= 2.0) {
                                        pts += pos.x to pos.y
                                        activeStroke = pts.toList()
                                    }
                                } else {
                                    change.consume()
                                    break
                                }
                            }

                            val w = boxSize.width.toFloat().coerceAtLeast(1f)
                            val h = boxSize.height.toFloat().coerceAtLeast(1f)
                            val existingPx = sample.strokes.map { st ->
                                st.map { pt -> (pt.x * w) to (pt.y * h) }
                            }
                            activeStroke = emptyList()
                            if (pts.isNotEmpty()) {
                                onSave(existingPx + listOf(pts), w, h)
                            }
                        }
                    },
            ) {
                val w = size.width
                val h = size.height
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                // Cap, x-height, baseline, and descender guides
                drawLine(guideColor, Offset(0f, h * HandwritingGuides.CAP_LINE), Offset(w, h * HandwritingGuides.CAP_LINE), strokeWidth = 1.2f)
                drawLine(guideColor, Offset(0f, h * HandwritingGuides.X_HEIGHT_LINE), Offset(w, h * HandwritingGuides.X_HEIGHT_LINE), strokeWidth = 1.2f, pathEffect = dash)
                drawLine(baselineColor, Offset(0f, h * HandwritingGuides.BASELINE), Offset(w, h * HandwritingGuides.BASELINE), strokeWidth = 2f)
                drawLine(guideColor, Offset(0f, h * HandwritingGuides.DESCENDER_LINE), Offset(w, h * HandwritingGuides.DESCENDER_LINE), strokeWidth = 1.2f, pathEffect = dash)

                val strokeStyle = Stroke(
                    width = (w * 0.032f).coerceIn(3.5f, 7f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )

                // Saved strokes
                for (st in sample.strokes) {
                    if (st.isEmpty()) continue
                    if (st.size == 1) {
                        val p = st[0]
                        drawCircle(inkColor, radius = strokeStyle.width * 0.55f, center = Offset(p.x * w, p.y * h))
                    } else {
                        val path = Path().apply {
                            moveTo(st[0].x * w, st[0].y * h)
                            for (i in 1 until st.size) {
                                lineTo(st[i].x * w, st[i].y * h)
                            }
                        }
                        drawPath(path, inkColor, style = strokeStyle)
                    }
                }

                // Live stroke
                if (activeStroke.isNotEmpty()) {
                    if (activeStroke.size == 1) {
                        val p = activeStroke[0]
                        drawCircle(ProButtonPalette.ElectricBlue, radius = strokeStyle.width * 0.55f, center = Offset(p.first, p.second))
                    } else {
                        val path = Path().apply {
                            moveTo(activeStroke[0].first, activeStroke[0].second)
                            for (i in 1 until activeStroke.size) {
                                lineTo(activeStroke[i].first, activeStroke[i].second)
                            }
                        }
                        drawPath(path, ProButtonPalette.ElectricBlue, style = strokeStyle)
                    }
                }
            }
        }
    }
}

@Composable
private fun HandwritingPreviewPane(
    profile: HandwritingProfile,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    var sampleText by remember {
        mutableStateOf("The quick brown fox jumps over 123 lazy dogs. Ready?")
    }
    var seed by remember { mutableLongStateOf(42L) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = sampleText,
                onValueChange = { sampleText = it },
                label = { Text("Type to preview your handwriting") },
                modifier = Modifier.weight(1f),
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
            )
            IconButton(onClick = { seed = System.nanoTime() }) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Shuffle organic variation")
            }
        }

        val previewFontSize = 32f
        val previewCellH = previewFontSize * 1.58f
        val previewLineStep = previewCellH * 0.96f
        val previewStartY = 18f
        val previewFirstBaseline = previewStartY + previewCellH * HandwritingGuides.BASELINE

        val strokes = remember(sampleText, profile, seed, canvasSize) {
            if (canvasSize.width <= 0) emptyList()
            else HandwritingSynthesizer.synthesizeStrokes(
                text = sampleText,
                startX = 28f,
                startY = previewStartY,
                maxWidth = (canvasSize.width - 56f).coerceAtLeast(180f),
                fontSize = previewFontSize,
                paper = PaperStyle.BLANK,
                lineScale = 1f,
                profile = profile,
                seed = seed,
            )
        }

        val inkColor = cs.onSurface
        val ruleColor = ProButtonPalette.ElectricBlue.copy(alpha = 0.24f)
        val marginColor = Color(0xFFD96B6B).copy(alpha = 0.28f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(cs.surfaceVariant.copy(alpha = 0.35f))
                .border(1.dp, cs.outlineVariant, RoundedCornerShape(16.dp))
                .onSizeChanged { canvasSize = it },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                var y = previewFirstBaseline
                while (y < size.height) {
                    drawLine(ruleColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
                    y += previewLineStep
                }
                drawLine(marginColor, Offset(20f, 0f), Offset(20f, size.height), strokeWidth = 1.5f)

                val strokeStyle = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                for (st in strokes) {
                    if (st.isEmpty()) continue
                    if (st.size == 1) {
                        drawCircle(inkColor, radius = 1.8f, center = Offset(st[0].first, st[0].second))
                    } else {
                        val path = Path().apply {
                            moveTo(st[0].first, st[0].second)
                            for (i in 1 until st.size) {
                                lineTo(st[i].first, st[i].second)
                            }
                        }
                        drawPath(path, inkColor, style = strokeStyle)
                    }
                }
            }
        }

        Text(
            "In any notebook, open a Text Box or Keyboard Mode and tap My Ink to place your typed words directly onto the page as real editable ink strokes.",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
    }
}
