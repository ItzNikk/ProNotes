package com.pronotes.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.kyant.backdrop.backdrops.layerBackdrop as kyantLayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop as kyantRememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/** A colour dot that grows when selected. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Swatch(color: Int, selected: Boolean, size: Int = 26, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val s by animateFloatAsState(if (selected) 1.12f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "swatch")
    Box(
        Modifier.size((size + 8).dp).scale(s)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
            .padding(3.dp).clip(CircleShape).background(Color(color))
            .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    )
}

/**
 * Custom colour editor: preset grid, recent colours, colour wheel with
 * brightness, and a hex field. Returns an opaque ARGB int.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorEditorDialog(
    initial: Int,
    presets: List<Int>,
    recent: List<Int>,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    var current by remember { mutableStateOf(initial or (0xFF shl 24)) }
    var hex by remember { mutableStateOf(String.format("%06X", initial and 0xFFFFFF)) }
    val controller = rememberColorPickerController()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Colour") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HsvColorPicker(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    controller = controller,
                    initialColor = Color(current),
                    onColorChanged = { env ->
                        if (env.fromUser) {
                            current = env.color.toArgb() or (0xFF shl 24)
                            hex = env.hexCode.takeLast(6).uppercase()
                        }
                    },
                )
                BrightnessSlider(modifier = Modifier.fillMaxWidth().height(28.dp), controller = controller)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(current)))
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { v ->
                            val clean = v.uppercase().filter { it.isDigit() || it in 'A'..'F' }.take(6)
                            hex = clean
                            if (clean.length == 6) current = clean.toLong(16).toInt() or (0xFF shl 24)
                        },
                        prefix = { Text("#") }, singleLine = true, label = { Text("Hex") },
                    )
                }
                Text("Presets", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    presets.forEach { c -> Swatch(c, c == current, 22) { current = c; hex = String.format("%06X", c and 0xFFFFFF) } }
                }
                if (recent.isNotEmpty()) {
                    Text("Recent", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        recent.forEach { c -> Swatch(c, c == current, 22) { current = c; hex = String.format("%06X", c and 0xFFFFFF) } }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(current) }) { Text("Use colour") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

val StickerSet = listOf(
    "⭐", "✅", "❌", "❗", "❓", "💡", "📌", "📎", "🔥", "❤️", "👍", "👀", "🎯", "🚀", "📅", "⏰",
    "📚", "✏️", "🧠", "💬", "🏆", "🎉", "😊", "😂", "😎", "🤔", "😴", "🙌", "🌟", "☀️", "🌙", "⚡",
    "🍎", "☕", "🎵", "🧪", "🔬", "📈", "📉", "💰", "🏠", "✈️", "🐱", "🐶", "🌸", "🍀", "➡️", "⬅️",
)

/** Universal elastic iOS-like press & hover spring feedback for cards, pills, tiles, and buttons. */
@Composable
fun Modifier.elasticStretch(enabled: Boolean = true, pressScale: Float = 0.94f): Modifier {
    if (!enabled) return this
    var pressed by remember { mutableStateOf(false) }
    val sx by animateFloatAsState(
        targetValue = if (pressed) pressScale else 1f,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 460f),
        label = "elasticX",
    )
    val sy by animateFloatAsState(
        targetValue = if (pressed) (2f - pressScale).coerceAtMost(1.06f) else 1f,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 460f),
        label = "elasticY",
    )
    return this
        .pointerInput(enabled) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    pressed = event.changes.any { it.pressed }
                }
            }
        }
        .graphicsLayer { scaleX = sx; scaleY = sy }
}

// ---------------------------------------------------------------------------
// Official Kyant0/AndroidLiquidGlass Backdrop Engine (io.github.kyant0:backdrop)
// ---------------------------------------------------------------------------

typealias LayerBackdrop = com.kyant.backdrop.backdrops.LayerBackdrop

@Composable
fun rememberLayerBackdrop(): LayerBackdrop = kyantRememberLayerBackdrop()

fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier = this.kyantLayerBackdrop(backdrop)

/**
 * Physical Liquid Glass surface powered by Kyant0/AndroidLiquidGlass (`io.github.kyant0:backdrop`).
 * Performs vibrancy boost, Gaussian backdrop blur, optical SDF edge lens refraction, and rim highlights.
 */
@Composable
fun OptionalLiquidGlassBox(
    backdrop: LayerBackdrop?,
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(28.dp),
    modifier: Modifier = Modifier,
    fallbackColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
    content: @Composable BoxScope.() -> Unit,
) {
    if (enabled && backdrop != null) {
        val surfaceTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.44f)
        val rimBrush = remember {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.14f),
                    Color(0xFF93C5FD).copy(alpha = 0.32f),
                ),
            )
        }
        val applyLens = shape != androidx.compose.ui.graphics.RectangleShape && shape != RoundedCornerShape(0.dp)
        Box(
            modifier = modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(18f.dp.toPx())
                        if (applyLens) lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(surfaceTint)
                    },
                )
                .border(0.9.dp, rimBrush, shape),
            content = content,
        )
    } else if (enabled) {
        val surfaceTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
        val rimBrush = remember {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.72f),
                    Color.White.copy(alpha = 0.2f),
                    Color(0xFF93C5FD).copy(alpha = 0.32f),
                ),
            )
        }
        Box(
            modifier = modifier
                .clip(shape)
                .background(surfaceTint, shape)
                .border(1.dp, rimBrush, shape),
            content = content,
        )
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(fallbackColor, shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
            content = content,
        )
    }
}

/** Represents a single tab in the Liquid Glass Bottom Navigation Bar. */
data class LiquidNavTab(
    val id: String,
    val label: String,
    val icon: ProIcon,
    val badgeCount: Int = 0,
)

/**
 * Modern Draggable Liquid Glass Floating Navigation Bar.
 * Inspired by One UI 9 & iOS 26 fluid design. Freely draggable across tablets and phones
 * with smooth spring feedback and double-tap to snap back to the bottom dock.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Ios26LiquidBottomNavBar(
    tabs: List<LiquidNavTab>,
    selectedId: String,
    backdrop: LayerBackdrop?,
    liquidGlass: Boolean = true,
    onSelect: (String) -> Unit,
    onPrimaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var isScrubbing by remember { mutableStateOf(false) }
    val dockScale by animateFloatAsState(
        targetValue = if (isScrubbing) 1.025f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "dockScale",
    )

    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .scale(dockScale),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {

        OptionalLiquidGlassBox(
            backdrop = backdrop,
            enabled = liquidGlass,
            shape = CircleShape,
            fallbackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 5.dp)
                    .pointerInput(tabs) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isScrubbing = true
                                val tabW = size.width.toFloat() / tabs.size.coerceAtLeast(1)
                                val idx = (offset.x / tabW).toInt().coerceIn(0, tabs.size - 1)
                                onSelect(tabs[idx].id)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val tabW = size.width.toFloat() / tabs.size.coerceAtLeast(1)
                                val idx = (change.position.x / tabW).toInt().coerceIn(0, tabs.size - 1)
                                onSelect(tabs[idx].id)
                            },
                            onDragEnd = { isScrubbing = false },
                            onDragCancel = { isScrubbing = false },
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                tabs.forEach { tab ->
                    val active = tab.id == selectedId
                    val scale by animateFloatAsState(
                        targetValue = if (active) 1.05f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                        label = "tabScale",
                    )
                    val lensAlpha by animateFloatAsState(
                        targetValue = if (active) 1f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "lensAlpha",
                    )
                    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .elasticStretch()
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f * lensAlpha),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f * lensAlpha),
                                    )
                                ),
                                CircleShape,
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.45f * lensAlpha),
                                shape = CircleShape,
                            )
                            .combinedClickable(onClick = { onSelect(tab.id) })
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            ProIconView(
                                icon = tab.icon,
                                tint = tint,
                                modifier = Modifier.size(20.dp),
                                ink = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else null,
                            )
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = tint,
                            )
                        }
                    }
                }
            }
        }

        if (onPrimaryAction != null) {
            OptionalLiquidGlassBox(
                backdrop = backdrop,
                enabled = liquidGlass,
                shape = CircleShape,
                fallbackColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(52.dp)
                    .elasticStretch()
                    .combinedClickable(onClick = onPrimaryAction),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF6366F1).copy(alpha = 0.92f),
                                    Color(0xFF8C65FF).copy(alpha = 0.92f),
                                )
                            ),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    ProIconView(
                        icon = ProIcon.ADD,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

