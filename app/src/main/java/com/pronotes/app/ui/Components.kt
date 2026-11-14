package com.pronotes.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import io.github.ntvien.liquidglass.GlassDefaults
import io.github.ntvien.liquidglass.GlassKind
import io.github.ntvien.liquidglass.GlassStyle
import io.github.ntvien.liquidglass.LiquidGlassTheme
import io.github.ntvien.liquidglass.component.LiquidGlassSlider
import io.github.ntvien.liquidglass.component.LiquidGlassToggle
import io.github.ntvien.liquidglass.component.LiquidGradientBackground
import io.github.ntvien.liquidglass.component.LiquidMenuButton
import io.github.ntvien.liquidglass.component.LiquidMenuItem
import io.github.ntvien.liquidglass.component.LiquidSurface

/** A colour dot that grows when selected. */
@OptIn(ExperimentalFoundationApi::class)
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
    LiquidGlassDialog(
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
        confirmButton = { Button(onClick = { onPick(current) }, shape = CircleShape) { Text("Use colour") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

val StickerSet = listOf(
    "⭐", "✅", "❌", "❗", "❓", "💡", "📌", "📎", "🔥", "❤️", "👍", "👀", "🎯", "🚀", "📅", "⏰",
    "📚", "✏️", "🧠", "💬", "🏆", "🎉", "😊", "😂", "😎", "🤔", "😴", "🙌", "🌟", "☀️", "🌙", "⚡",
    "🍎", "☕", "🎵", "🧪", "🔬", "📈", "📉", "💰", "🏠", "✈️", "🐱", "🐶", "🌸", "🍀", "➡️", "⬅️",
)

/** Universal elastic press spring feedback for cards, pills, tiles, and buttons. */
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

// ===========================================================================
// Prebuilt io.github.ntvien.liquidglass Wrappers & Components
// ===========================================================================

@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassToggle(
        checked = selected(),
        onCheckedChange = onSelect,
        modifier = modifier,
    )
}

@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    LiquidGlassSlider(
        value = value(),
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        steps = steps,
    )
}

/**
 * Botanical OpenLeaf background for ntvien LiquidGlassScene: rendered once into a 1/4-res bitmap for 120fps scrolling.
 */
@Composable
fun BotanicalGlassBackground(
    modifier: Modifier = Modifier,
) {
    val bg = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val dark = bg.luminance() < 0.5f
    LiquidGradientBackground(
        modifier = modifier,
        dark = dark,
        baseColor = bg,
        blobColors = if (!dark) {
            listOf(
                primary.copy(alpha = 0.24f),
                Color(0xFFB98A2E).copy(alpha = 0.16f),
                secondary.copy(alpha = 0.20f),
                primary.copy(alpha = 0.14f),
            )
        } else {
            listOf(
                primary.copy(alpha = 0.26f),
                Color(0xFFD99A4E).copy(alpha = 0.14f),
                secondary.copy(alpha = 0.18f),
                primary.copy(alpha = 0.14f),
            )
        },
    )
}

/**
 * Prebuilt ntvien LiquidSurface wrapper preserving full-app transparency & readability.
 */
@Composable
fun OptionalLiquidGlassBox(
    enabled: Boolean = true,
    shape: Shape = GlassDefaults.CardShape,
    modifier: Modifier = Modifier,
    fallbackColor: Color = LiquidGlassTheme.colors.glassFillStrong,
    useFrostForLists: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LiquidGlassTheme.colors
    val style = if (!enabled || useFrostForLists) {
        GlassDefaults.frost().copy(fallbackSurface = fallbackColor)
    } else {
        GlassDefaults.chrome().copy(
            chromaticAberration = false,
            blur = 10.dp,
            lensHeight = 14.dp,
            lensAmount = 24.dp,
            surface = colors.chromeFill,
            fallbackSurface = fallbackColor,
            shadowRadius = 12.dp,
        )
    }
    LiquidSurface(
        modifier = modifier,
        style = style,
        shape = shape,
        contentPadding = PaddingValues(0.dp),
        content = content,
    )
}

/**
 * Prebuilt ntvien/liquid-glass Dialog surface with high-contrast scrim for crisp modal legibility.
 */
@Composable
fun LiquidGlassDialogSurface(
    shape: Shape = GlassDefaults.CardShape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LiquidGlassTheme.colors
    LiquidSurface(
        modifier = modifier,
        style = GlassDefaults.frost().copy(
            fallbackSurface = colors.glassFillOpaque,
            fallbackSheen = 0.55f,
            highlightWidth = 1.dp,
            shadowRadius = 24.dp,
            shadowColor = colors.glassShadow,
            contentColor = colors.content,
        ),
        shape = shape,
        contentPadding = PaddingValues(0.dp),
        content = content,
    )
}

/**
 * Liquid Glass modal dialog replacing Material 3 AlertDialog across the app.
 */
@Composable
fun LiquidGlassDialog(
    onDismissRequest: () -> Unit,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    maxWidth: Dp = 520.dp,
) {
    val colors = LiquidGlassTheme.colors
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            LiquidGlassDialogSurface(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                CompositionLocalProvider(LocalContentColor provides colors.content) {
                    Column(Modifier.fillMaxWidth().padding(22.dp)) {
                        if (title != null) {
                            CompositionLocalProvider(
                                androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.content,
                                ),
                            ) {
                                Box(Modifier.fillMaxWidth()) { title() }
                            }
                            if (text != null) Spacer(Modifier.height(12.dp))
                        }
                        if (text != null) {
                            CompositionLocalProvider(
                                androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(
                                    color = colors.content,
                                ),
                            ) {
                                Box(Modifier.fillMaxWidth().heightIn(max = 540.dp)) { text() }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            dismissButton?.invoke()
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Liquid Glass bottom sheet replacing Material 3 ModalBottomSheet.
 */
@Composable
fun LiquidGlassSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LiquidGlassTheme.colors
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            LiquidGlassDialogSurface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                CompositionLocalProvider(LocalContentColor provides colors.content) {
                    Column(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(colors.contentMuted.copy(alpha = 0.35f)),
                        )
                        Spacer(Modifier.height(10.dp))
                        content()
                    }
                }
            }
        }
    }
}

/**
 * Liquid Glass card respecting the GPU budget: use hero=true only for 1-3 top-level hero cards per screen,
 * and hero=false (frost) for lists/repeated items.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    hero: Boolean = false,
    shape: Shape = if (hero) GlassDefaults.ContinuousCardShape else GlassDefaults.CardShape,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val style = if (hero) GlassDefaults.card() else GlassDefaults.frost()
    LiquidSurface(
        modifier = modifier,
        style = style,
        shape = shape,
        contentPadding = contentPadding,
        onClick = onClick,
        content = content,
    )
}

/**
 * Compact Liquid Glass pill / badge.
 */
@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    style: GlassStyle = if (selected) GlassDefaults.accent() else GlassDefaults.frost(),
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    LiquidSurface(
        modifier = modifier,
        style = style,
        shape = GlassDefaults.PillShape,
        contentPadding = contentPadding,
        onClick = onClick,
        content = content,
    )
}

/**
 * Liquid Glass selectable chip replacing Material FilterChip.
 */
@Composable
fun LiquidGlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val colors = LiquidGlassTheme.colors
    val fg = if (selected) colors.onAccent else colors.content
    val style = if (selected) {
        GlassDefaults.accent().copy(kind = GlassKind.Frost)
    } else {
        GlassDefaults.frost()
    }
    LiquidSurface(
        modifier = modifier,
        style = style,
        shape = GlassDefaults.PillShape,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = fg, modifier = Modifier.size(16.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Frosted glass slider row with label, value readout, and scene-aware LiquidGlassSlider.
 */
@Composable
fun LiquidSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    val colors = LiquidGlassTheme.colors
    LiquidSurface(
        modifier = modifier.fillMaxWidth(),
        style = GlassDefaults.frost(),
        shape = GlassDefaults.ControlShape,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column {
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = colors.contentMuted, modifier = Modifier.weight(1f))
                Text(valueText, style = MaterialTheme.typography.labelSmall, color = colors.accent, fontWeight = FontWeight.SemiBold)
            }
            LiquidGlassSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
            )
        }
    }
}

/**
 * Option picker row backed by ntvien's morphing LiquidMenuButton.
 */
@Composable
fun LiquidChoiceRow(
    title: String,
    value: String,
    options: List<String>,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?>? = null,
) {
    val colors = LiquidGlassTheme.colors
    val entries = remember(options, icons) {
        options.mapIndexed { idx, label ->
            LiquidMenuItem(
                label = label,
                icon = icons?.getOrNull(idx),
                onClick = { onPick(idx) },
            )
        }
    }
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.content, modifier = Modifier.weight(1f))
        LiquidMenuButton(
            items = entries,
            contentDescription = title,
            style = GlassDefaults.frost(),
            shape = GlassDefaults.PillShape,
            menuWidth = 230.dp,
            modifier = Modifier.heightIn(min = 38.dp),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.accent,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 170.dp),
                )
                Icon(Icons.Outlined.KeyboardArrowDown, null, tint = colors.accent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Long-press / programmatic Liquid Glass context menu styled to match ntvien's LiquidMenuButton.
 */
@Composable
fun LiquidContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<io.github.ntvien.liquidglass.component.LiquidMenuEntry>,
    menuWidth: Dp = 240.dp,
) {
    if (!expanded) return
    val colors = LiquidGlassTheme.colors
    androidx.compose.ui.window.Popup(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true),
    ) {
        LiquidGlassDialogSurface(
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.width(menuWidth),
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                items.forEach { entry ->
                    when (entry) {
                        is LiquidMenuItem -> {
                            val fg = when {
                                !entry.enabled -> colors.contentMuted.copy(alpha = 0.5f)
                                entry.destructive -> colors.badge
                                else -> colors.content
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clickable(enabled = entry.enabled) {
                                        onDismissRequest()
                                        entry.onClick()
                                    }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (entry.icon != null) {
                                    Icon(entry.icon, null, tint = fg, modifier = Modifier.size(20.dp))
                                } else if (entry.leadingContent != null) {
                                    entry.leadingContent.invoke()
                                }
                                Text(
                                    entry.label,
                                    color = fg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        io.github.ntvien.liquidglass.component.LiquidMenuDivider -> {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .height(0.5.dp)
                                    .background(colors.content.copy(alpha = 0.12f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

