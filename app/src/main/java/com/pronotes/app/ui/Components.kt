package com.pronotes.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.pronotes.app.data.MotionStyle
import kotlin.math.max

/** Rich color palette for tinted interactive buttons across the UI. */
object GlassButtonPalette {
    val Emerald = Color(0xFF246A5B)
    val OceanBlue = Color(0xFF2563EB)
    val StudioPurple = Color(0xFF7C3AED)
    @Deprecated("Renamed to StudioPurple", ReplaceWith("StudioPurple"))
    val GalaxyPurple = StudioPurple
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
    GlassDialog(
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
            GlassPill(
                selected = true,
                tint = GlassButtonPalette.Emerald,
                onClick = { onPick(current) },
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text("Use colour", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            GlassPill(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

val StickerSet = listOf(
    "Star", "Check", "Cross", "Alert", "Question", "Idea", "Pin", "Clip",
    "Focus", "Heart", "Note", "Goal", "Target", "Launch", "Date", "Clock",
    "Book", "Draft", "Concept", "Quote", "Trophy", "Milestone", "Diagram", "Summary",
)

/**
 * Universal elastic press spring feedback + luminous radial touch glow for cards, pills, tiles, and buttons.
 */
@Composable
fun Modifier.elasticStretch(enabled: Boolean = true, pressScale: Float = 0.94f): Modifier {
    val reducedMotion = LocalMotionStyle.current == MotionStyle.REDUCED
    if (!enabled || reducedMotion) return this
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
    val accent = MaterialTheme.colorScheme.primary
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
                            Color.White.copy(alpha = 0.30f * glowAlpha),
                            accent.copy(alpha = 0.18f * glowAlpha),
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
