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
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
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
import io.github.ntvien.liquidglass.DisableGlassSceneBackdrop
import io.github.ntvien.liquidglass.GlassDefaults
import io.github.ntvien.liquidglass.GlassKind
import io.github.ntvien.liquidglass.GlassStyle
import io.github.ntvien.liquidglass.LiquidGlassTheme
import io.github.ntvien.liquidglass.component.LiquidGlassSlider
import io.github.ntvien.liquidglass.component.LiquidGlassToggle
import io.github.ntvien.liquidglass.component.LiquidGradientBackground
import io.github.ntvien.liquidglass.component.LiquidMenuButton
import io.github.ntvien.liquidglass.component.LiquidMenuDivider
import io.github.ntvien.liquidglass.component.LiquidMenuEntry
import io.github.ntvien.liquidglass.component.LiquidMenuItem
import io.github.ntvien.liquidglass.component.LiquidSurface
import kotlin.math.max

/** Rich color palette for tinted Liquid Glass buttons across the UI. */
object GlassButtonPalette {
    val Emerald = Color(0xFF246A5B)
    val OceanBlue = Color(0xFF2563EB)
    val GalaxyPurple = Color(0xFF7C3AED)
    val SkyCyan = Color(0xFF0891B2)
    val WarmAmber = Color(0xFFD97706)
    val CoralRose = Color(0xFFE11D48)
    val Indigo = Color(0xFF4F46E5)
    val MintTeal = Color(0xFF0D9488)
}

/** A colour dot that grows and stretches when selected or pressed. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Swatch(color: Int, selected: Boolean, size: Int = 26, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val s by animateFloatAsState(if (selected) 1.15f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "swatch")
    Box(
        Modifier.size((size + 8).dp).scale(s).elasticStretch(pressScale = 0.90f)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
            .padding(3.dp).clip(CircleShape).background(Color(color))
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
        confirmButton = {
            LiquidGlassPill(
                selected = true,
                tint = GlassButtonPalette.Emerald,
                onClick = { onPick(current) },
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text("Use colour", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            LiquidGlassPill(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

val StickerSet = listOf(
    "⭐", "✅", "❌", "❗", "❓", "💡", "📌", "📎", "🔥", "❤️", "👍", "👀", "🎯", "🚀", "📅", "⏰",
    "📚", "✏️", "🧠", "💬", "🏆", "🎉", "😊", "😂", "😎", "🤔", "😴", "🙌", "🌟", "☀️", "🌙", "⚡",
    "🍎", "☕", "🎵", "🧪", "🔬", "📈", "📉", "💰", "🏠", "✈️", "🐱", "🐶", "🌸", "🍀", "➡️", "⬅️",
)

/**
 * Universal elastic press spring feedback + luminous radial touch glow for cards, pills, tiles, and buttons.
 */
@Composable
fun Modifier.elasticStretch(enabled: Boolean = true, pressScale: Float = 0.94f): Modifier {
    if (!enabled) return this
    var pressed by remember { mutableStateOf(false) }
    var touchPos by remember { mutableStateOf(Offset.Zero) }
    val sx by animateFloatAsState(
        targetValue = if (pressed) pressScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 440f),
        label = "elasticX",
    )
    val sy by animateFloatAsState(
        targetValue = if (pressed) (2f - pressScale).coerceAtMost(1.06f) else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 440f),
        label = "elasticY",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(stiffness = 480f),
        label = "touchGlow",
    )
    val accent = LiquidGlassTheme.colors.accent
    return this
        .pointerInput(enabled) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val active = event.changes.firstOrNull { it.pressed }
                    if (active != null) {
                        pressed = true
                        touchPos = active.position
                    } else {
                        pressed = false
                    }
                }
            }
        }
        .graphicsLayer { scaleX = sx; scaleY = sy }
        .drawWithContent {
            drawContent()
            if (glowAlpha > 0.01f) {
                val r = max(size.width, size.height).coerceAtLeast(48f) * 0.85f
                val c = Offset(
                    touchPos.x.coerceIn(0f, size.width),
                    touchPos.y.coerceIn(0f, size.height),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.34f * glowAlpha),
                            accent.copy(alpha = 0.20f * glowAlpha),
                            Color.Transparent,
                        ),
                        center = c,
                        radius = r,
                    ),
                    blendMode = BlendMode.Plus,
                )
            }
        }
}

// ===========================================================================
// Prebuilt LiquidGlass Wrappers & Components
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
 * Botanical OpenLeaf parchment background with soft organic depth so transparent glass visibly refracts.
 * Rendered once into a 1/4-res bitmap for 120fps scrolling.
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
                primary.copy(alpha = 0.14f),
                Color(0xFFD99A4E).copy(alpha = 0.10f),
                secondary.copy(alpha = 0.12f),
                Color(0xFF4BA3C7).copy(alpha = 0.08f),
            )
        } else {
            listOf(
                primary.copy(alpha = 0.16f),
                Color(0xFFD99A4E).copy(alpha = 0.09f),
                secondary.copy(alpha = 0.12f),
                Color(0xFF38BDF8).copy(alpha = 0.09f),
            )
        },
    )
}

/**
 * Prebuilt LiquidSurface wrapper preserving full-app transparency & readability without ugly borders.
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
        GlassDefaults.frost().copy(
            fallbackSurface = fallbackColor,
            highlightWidth = 0.dp,
        )
    } else {
        GlassDefaults.chrome().copy(
            chromaticAberration = false,
            blur = 14.dp,
            lensHeight = 12.dp,
            lensAmount = 20.dp,
            highlightWidth = 0.3.dp,
            highlightAlpha = 0.22f,
            surface = colors.chromeFill,
            fallbackSurface = fallbackColor,
            shadowRadius = 10.dp,
        )
    }
    LiquidSurface(
        modifier = modifier,
        style = style,
        shape = shape,
        contentPadding = PaddingValues(0.dp),
        interactive = false,
        content = content,
    )
}

/**
 * Liquid Glass Dialog surface with translucent frosted glass and zero harsh borders.
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
            fallbackSheen = 0.48f,
            highlightWidth = 0.dp,
            shadowRadius = 24.dp,
            shadowColor = colors.glassShadow,
            contentColor = colors.content,
        ),
        shape = shape,
        contentPadding = PaddingValues(0.dp),
        interactive = false,
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
        DisableGlassSceneBackdrop {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.30f))
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
        DisableGlassSceneBackdrop {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.26f))
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
}

/**
 * Liquid Glass card with high transparency, touch glow, and elastic stretch.
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
        interactive = true,
        content = content,
    )
}

/**
 * Compact Liquid Glass pill / button with optional vibrant color [tint], touch glow, and spring stretch.
 */
@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tint: Color? = null,
    style: GlassStyle = when {
        tint != null && selected -> GlassDefaults.tinted(tint)
        tint != null -> GlassDefaults.control().copy(
            surface = tint.copy(alpha = 0.16f),
            fallbackSurface = tint.copy(alpha = 0.18f),
        )
        selected -> GlassDefaults.accent()
        else -> GlassDefaults.control()
    },
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
        interactive = true,
        content = content,
    )
}

/**
 * Liquid Glass selectable chip with optional color [tint], touch glow, and spring stretch.
 */
@Composable
fun LiquidGlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    tint: Color? = null,
) {
    val colors = LiquidGlassTheme.colors
    val activeTint = tint ?: colors.accent
    val fg = when {
        selected && tint != null -> Color.White
        selected -> colors.onAccent
        tint != null -> tint
        else -> colors.content
    }
    val style = when {
        selected -> GlassDefaults.tinted(activeTint, fg)
        tint != null -> GlassDefaults.control().copy(
            surface = tint.copy(alpha = 0.14f),
            fallbackSurface = tint.copy(alpha = 0.16f),
        )
        else -> GlassDefaults.control()
    }
    LiquidSurface(
        modifier = modifier,
        style = style,
        shape = GlassDefaults.PillShape,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        onClick = onClick,
        interactive = true,
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
        interactive = false,
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
 * Option picker row backed by LiquidMenuButton.
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
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            buttonContent = {
                Text(
                    value,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.accent,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 170.dp),
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Outlined.KeyboardArrowDown, null, tint = colors.accent, modifier = Modifier.size(18.dp))
            },
            items = entries,
            modifier = Modifier.heightIn(min = 38.dp),
            style = GlassDefaults.control(),
            shape = GlassDefaults.PillShape,
            menuWidth = 240.dp,
        )
    }
}

/**
 * Long-press / programmatic Liquid Glass context menu with spring entrance, colorful icon badges, and elastic stretch.
 */
@Composable
fun LiquidContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<LiquidMenuEntry>,
    menuWidth: Dp = 250.dp,
) {
    val colors = LiquidGlassTheme.colors
    val palette = listOf(
        GlassButtonPalette.Emerald,
        GlassButtonPalette.OceanBlue,
        GlassButtonPalette.GalaxyPurple,
        GlassButtonPalette.WarmAmber,
        GlassButtonPalette.SkyCyan,
        GlassButtonPalette.CoralRose,
    )
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.width(menuWidth),
    ) {
        DisableGlassSceneBackdrop {
            var appeared by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { appeared = true }
            val menuScale by animateFloatAsState(
                targetValue = if (appeared) 1f else 0.88f,
                animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
                label = "ctxScale",
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = menuScale
                        scaleY = menuScale
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .shadow(22.dp, RoundedCornerShape(24.dp), ambientColor = colors.glassShadow, spotColor = colors.glassShadow)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.glassFillOpaque.copy(alpha = 0.88f))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                colors.glassHighlight.copy(alpha = 0.42f),
                                colors.accent.copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                            center = Offset(40f, 0f),
                            radius = 560f,
                        ),
                    )
                    .padding(vertical = 8.dp, horizontal = 6.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items.forEachIndexed { index, entry ->
                        when (entry) {
                            is LiquidMenuItem -> {
                                val badgeTint = when {
                                    !entry.enabled -> colors.contentMuted.copy(alpha = 0.35f)
                                    entry.destructive -> colors.badge
                                    else -> palette[index % palette.size]
                                }
                                val fg = when {
                                    !entry.enabled -> colors.contentMuted.copy(alpha = 0.5f)
                                    entry.destructive -> colors.badge
                                    else -> colors.content
                                }
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .elasticStretch(enabled = entry.enabled, pressScale = 0.96f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(enabled = entry.enabled) {
                                            onDismissRequest()
                                            entry.onClick()
                                        }
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    if (entry.icon != null || entry.leadingContent != null) {
                                        Box(
                                            Modifier
                                                .size(30.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(badgeTint.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (entry.icon != null) {
                                                Icon(entry.icon, null, tint = badgeTint, modifier = Modifier.size(18.dp))
                                            } else {
                                                entry.leadingContent?.invoke()
                                            }
                                        }
                                    }
                                    Text(
                                        entry.label,
                                        color = fg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            LiquidMenuDivider -> {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                        .height(0.8.dp)
                                        .background(colors.content.copy(alpha = 0.09f)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
