package com.pronotes.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Vibrant multi-color palette for interactive buttons, action pills, and highlights across the UI. */
object ProButtonPalette {
    val ElectricBlue = Color(0xFF1A84FF)
    val SkyCyan = Color(0xFF00A3F5)
    val RoyalBlue = Color(0xFF0044EA)
    val VibrantOrange = Color(0xFFFF8C00)
    val MagentaPink = Color(0xFFF01E6B)
    val Emerald = Color(0xFF10B981)
    val RoyalIndigo = Color(0xFF4F46E5)
    val WarmAmber = Color(0xFFD97706)
    val CoralRose = Color(0xFFE11D48)
    val Violet = Color(0xFF7C3AED)
    val MintTeal = Color(0xFF0D9488)
    val SlateNeutral = Color(0xFF475569)
}

/** Curated 60-colour studio grid: 10 columns x 6 rows (neutrals + 5 luminance bands across 10 studio hues). */
private val StudioSquareGrid: List<List<Int>> = listOf(
    // Row 0: Neutrals & Paper Ink
    listOf(
        0xFF000000, 0xFF1C1E21, 0xFF333840, 0xFF525866, 0xFF737B8C,
        0xFF98A0B3, 0xFFBDC4D4, 0xFFDDE2EC, 0xFFF3F4F6, 0xFFFFFFFF,
    ).map { it.toInt() },
    // Row 1: Deep Studio Inks
    listOf(
        0xFF7F1D1D, 0xFF7C2D12, 0xFF78350F, 0xFF365314, 0xFF14532D,
        0xFF134E4A, 0xFF0C4A6E, 0xFF1E3A8A, 0xFF3B0764, 0xFF831843,
    ).map { it.toInt() },
    // Row 2: Rich Classic Inks
    listOf(
        0xFFB91C1C, 0xFFC2410C, 0xFFB45309, 0xFF4D7C0F, 0xFF15803D,
        0xFF0F766E, 0xFF0369A1, 0xFF1D4ED8, 0xFF6D28D9, 0xFFBE185D,
    ).map { it.toInt() },
    // Row 3: Vibrant Core
    listOf(
        0xFFEF4444, 0xFFF97316, 0xFFF59E0B, 0xFF84CC16, 0xFF22C55E,
        0xFF14B8A6, 0xFF0EA5E9, 0xFF3B82F6, 0xFF8B5CF6, 0xFFEC4899,
    ).map { it.toInt() },
    // Row 4: Soft Brights
    listOf(
        0xFFF87171, 0xFFFB923C, 0xFFFBBF24, 0xFFA3E635, 0xFF4ADE80,
        0xFF2DD4BF, 0xFF38BDF8, 0xFF60A5FA, 0xFFA78BFA, 0xFFF472B6,
    ).map { it.toInt() },
    // Row 5: Pastel / Highlighter Tints
    listOf(
        0xFFFECACA, 0xFFFED7AA, 0xFFFEF08A, 0xFFD9F99D, 0xFFBBF7D0,
        0xFF99F6E4, 0xFFBAE6FD, 0xFFBFDBFE, 0xFFDDD6FE, 0xFFFBCFE8,
    ).map { it.toInt() },
)

/** A crisp circular colour swatch with an adaptive contrasting rim so light and dark swatches stay readable on any toolbar. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Swatch(color: Int, selected: Boolean, size: Int = 26, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val label = remember(color) { "Colour #" + String.format("%08X", color) }
    val tileColor = Color(color)
    val lum = tileColor.luminance()
    val innerShape = CircleShape
    val outerShape = CircleShape

    val innerRimColor = when {
        tileColor.alpha < 0.05f -> MaterialTheme.colorScheme.outline
        lum > 0.72f -> Color(0xFF14171F).copy(alpha = 0.58f)
        lum < 0.25f -> Color(0xFFF8FAFC).copy(alpha = 0.65f)
        else -> Color.White.copy(alpha = 0.28f)
    }
    val outerRingColor = if (selected) {
        when {
            lum > 0.75f -> Color(0xFF1A84FF)
            lum < 0.22f -> Color(0xFF60A5FA)
            else -> MaterialTheme.colorScheme.primary
        }
    } else Color.Transparent

    Box(
        Modifier
            .size(maxOf(size + 8, 34).dp)
            .semantics { contentDescription = label; this.selected = selected }
            .border(
                width = if (selected) 2.2.dp else 0.dp,
                color = outerRingColor,
                shape = outerShape,
            )
            .padding(3.dp)
            .clip(innerShape)
            .background(if (tileColor.alpha < 0.05f) MaterialTheme.colorScheme.surfaceVariant else tileColor)
            .border(
                width = 1.15.dp,
                color = innerRimColor,
                shape = innerShape,
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(
            Icons.Filled.Check,
            null,
            Modifier.size(if (size <= 20) 13.dp else 16.dp),
            tint = if (tileColor.alpha < 0.05f) MaterialTheme.colorScheme.onSurface else if (lum > 0.45f) Color(0xFF101820) else Color.White,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorEditorDialog(
    initial: Int,
    presets: List<Int>,
    recent: List<Int>,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val startArgb = remember(initial) { initial or (0xFF shl 24) }
    val initialHsv = remember(startArgb) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(startArgb, it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var current by remember { mutableIntStateOf(startArgb) }
    var hex by remember { mutableStateOf(String.format("%06X", startArgb and 0xFFFFFF)) }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Square Palette, 1 = Square Spectrum

    fun applyColor(argb: Int) {
        val opaque = argb or (0xFF shl 24)
        current = opaque
        hex = String.format("%06X", opaque and 0xFFFFFF)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(opaque, hsv)
        // Preserve hue if picking pure white/black/grey so spectrum stays stable
        if (hsv[1] > 0.02f && hsv[2] > 0.02f) hue = hsv[0]
        sat = hsv[1]
        value = hsv[2]
    }

    fun applyHsv(h: Float, s: Float, v: Float) {
        hue = h.coerceIn(0f, 360f)
        sat = s.coerceIn(0f, 1f)
        value = v.coerceIn(0f, 1f)
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)) or (0xFF shl 24)
        current = argb
        hex = String.format("%06X", argb and 0xFFFFFF)
    }

    val uniquePresets = remember(presets) { presets.distinct().take(16) }
    val uniqueRecent = remember(recent, presets) { recent.distinct().filterNot { it in presets }.take(16) }

    AppDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Colour Palette", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                // Split square comparison badge (Original | Selected)
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(7.dp)),
                ) {
                    Box(
                        Modifier
                            .width(28.dp)
                            .height(34.dp)
                            .background(Color(startArgb))
                            .clickable { applyColor(startArgb) },
                    )
                    Box(
                        Modifier
                            .width(38.dp)
                            .height(34.dp)
                            .background(Color(current)),
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Studio square mode selector: Palette Grid vs 2D Square Spectrum
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf("Square Palette", "Square Spectrum").forEachIndexed { idx, title ->
                        val isSel = activeTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .border(
                                    width = if (isSel) 1.dp else 0.dp,
                                    color = if (isSel) MaterialTheme.colorScheme.outlineVariant else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp),
                                )
                                .clickable { activeTab = idx }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (activeTab == 0) {
                    // 10x6 Seamless Studio Square Color Matrix
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(3.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        StudioSquareGrid.forEach { rowColors ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                rowColors.forEach { argb ->
                                    val isSel = (argb and 0xFFFFFF) == (current and 0xFFFFFF)
                                    val tileColor = Color(argb)
                                    val lum = tileColor.luminance()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(tileColor)
                                            .then(
                                                if (isSel) {
                                                    Modifier.border(
                                                        width = 2.dp,
                                                        color = if (lum > 0.55f) Color(0xFF0F172A) else Color.White,
                                                        shape = RoundedCornerShape(3.dp),
                                                    )
                                                } else Modifier,
                                            )
                                            .clickable { applyColor(argb) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (isSel) {
                                            Box(
                                                Modifier
                                                    .size(6.dp)
                                                    .clip(RoundedCornerShape(1.5.dp))
                                                    .background(if (lum > 0.55f) Color(0xFF0F172A) else Color.White),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 7-step Tints & Shades Square Bar derived from active colour
                    Text("SHADES & TINTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val shades = remember(hue, sat) {
                            val baseSat = sat.coerceAtLeast(0.08f)
                            listOf(
                                android.graphics.Color.HSVToColor(floatArrayOf(hue, (baseSat * 1.15f).coerceAtMost(1f), 0.22f)),
                                android.graphics.Color.HSVToColor(floatArrayOf(hue, (baseSat * 1.08f).coerceAtMost(1f), 0.42f)),
                                android.graphics.Color.HSVToColor(floatArrayOf(hue, baseSat, 0.64f)),
                                android.graphics.Color.HSVToColor(floatArrayOf(hue, baseSat, 0.84f)),
                                android.graphics.Color.HSVToColor(floatArrayOf(hue, baseSat, 1.0f)),
                                android.graphics.Color.HSVToColor(floatArrayOf(hue, (baseSat * 0.52f).coerceAtLeast(0.05f), 1.0f)),
                                android.graphics.Color.HSVToColor(floatArrayOf(hue, (baseSat * 0.24f).coerceAtLeast(0.03f), 1.0f)),
                            )
                        }
                        shades.forEach { tone ->
                            val isSel = (tone and 0xFFFFFF) == (current and 0xFFFFFF)
                            val toneColor = Color(tone)
                            val lum = toneColor.luminance()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(toneColor)
                                    .border(
                                        width = if (isSel) 2.dp else 1.dp,
                                        color = if (isSel) {
                                            if (lum > 0.55f) Color(0xFF0F172A) else Color.White
                                        } else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(5.dp),
                                    )
                                    .clickable { applyColor(tone) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSel) {
                                    Icon(
                                        Icons.Filled.Check,
                                        null,
                                        Modifier.size(14.dp),
                                        tint = if (lum > 0.5f) Color(0xFF0F172A) else Color.White,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Square 2D Saturation x Value Color Field (GPU Canvas, zero circular wheel)
                    SquareSvColorField(
                        hue = hue,
                        saturation = sat,
                        value = value,
                        onChange = { s, v -> applyHsv(hue, s, v) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.22f),
                    )

                    // Horizontal Hue Spectrum Bar
                    SquareHueBar(
                        hue = hue,
                        onChange = { h -> applyHsv(h, sat, value) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                    )

                    // Live H / S / B numeric telemetry chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            "H" to "${hue.toInt()}°",
                            "S" to "${(sat * 100).toInt()}%",
                            "B" to "${(value * 100).toInt()}%",
                        ).forEach { (k, v) ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(k, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(v, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Hex input row with crisp square preview tile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(current))
                            .border(1.2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                    )
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { v ->
                            val clean = v.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
                            hex = clean
                            if (clean.length == 6) {
                                applyColor(clean.toLong(16).toInt() or (0xFF shl 24))
                            }
                        },
                        prefix = { Text("#", fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        label = { Text("Hex") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                    )
                }

                // Preset tool swatches
                if (uniquePresets.isNotEmpty()) {
                    Text("TOOL PALETTE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        uniquePresets.forEach { c ->
                            Swatch(c, (c and 0xFFFFFF) == (current and 0xFFFFFF), 24) { applyColor(c) }
                        }
                    }
                }

                // Recent colours
                if (uniqueRecent.isNotEmpty()) {
                    Text("RECENT COLOURS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        uniqueRecent.forEach { c ->
                            Swatch(c, (c and 0xFFFFFF) == (current and 0xFFFFFF), 22) { applyColor(c) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppPill(
                selected = true,
                tint = Color(current),
                onClick = { onPick(current) },
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text("Use colour", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            AppPill(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

@Composable
private fun SquareSvColorField(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hueColor = remember(hue) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue.coerceIn(0f, 360f), 1f, 1f)))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .pointerInput(hue) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun update(pos: Offset) {
                        val s = (pos.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val v = (1f - pos.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onChange(s, v)
                    }
                    down.consume()
                    update(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        change.consume()
                        update(change.position)
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Horizontal white -> pure hue gradient (Saturation)
            drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
            // Vertical transparent -> black gradient (Brightness / Value)
            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

            // Crisp square crosshair reticle
            val cx = (saturation.coerceIn(0f, 1f) * size.width)
            val cy = ((1f - value.coerceIn(0f, 1f)) * size.height)
            val boxSize = 18.dp.toPx()
            val half = boxSize / 2f
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(cx - half - 1.5f, cy - half - 1.5f),
                size = Size(boxSize + 3f, boxSize + 3f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(cx - half, cy - half),
                size = Size(boxSize, boxSize),
                cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx()),
                style = Stroke(width = 2.2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun SquareHueBar(
    hue: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spectrum = remember {
        listOf(
            Color(0xFFFF0000),
            Color(0xFFFFFF00),
            Color(0xFF00FF00),
            Color(0xFF00FFFF),
            Color(0xFF0000FF),
            Color(0xFFFF00FF),
            Color(0xFFFF0000),
        )
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(7.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun update(pos: Offset) {
                        val h = (pos.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f) * 360f
                        onChange(h)
                    }
                    down.consume()
                    update(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        change.consume()
                        update(change.position)
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(brush = Brush.horizontalGradient(spectrum))
            val thumbW = 12.dp.toPx()
            val x = ((hue.coerceIn(0f, 360f) / 360f) * (size.width - thumbW)).coerceIn(0f, (size.width - thumbW).coerceAtLeast(0f))
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(x - 1f, 1.dp.toPx()),
                size = Size(thumbW + 2f, size.height - 2.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(x, 2.dp.toPx()),
                size = Size(thumbW, size.height - 4.dp.toPx()),
                cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx()),
                style = Stroke(width = 2.2.dp.toPx()),
            )
        }
    }
}

val StickerSet = listOf(
    "Star", "Check", "Cross", "Alert", "Question", "Idea", "Pin", "Clip",
    "Focus", "Heart", "Note", "Goal", "Target", "Launch", "Date", "Clock",
    "Book", "Draft", "Concept", "Quote", "Trophy", "Milestone", "Diagram", "Summary",
)
