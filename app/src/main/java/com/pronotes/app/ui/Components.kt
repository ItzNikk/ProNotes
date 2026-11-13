package com.pronotes.app.ui

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

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
// Open-Source AGSL Liquid Glass Backdrop Engine (SDK 36 / AGP 8.12 compatible)
// ---------------------------------------------------------------------------

/** Holds the live GraphicsLayer of the underlying note canvas for real-time optical refraction. */
@Stable
class LayerBackdrop(val graphicsLayer: GraphicsLayer) {
    var rootPosition: Offset by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberLayerBackdrop(): LayerBackdrop {
    val gl = rememberGraphicsLayer()
    return remember(gl) { LayerBackdrop(gl) }
}

fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier = this
    .onGloballyPositioned { backdrop.rootPosition = it.positionInRoot() }
    .drawWithContent {
        backdrop.graphicsLayer.record {
            this@drawWithContent.drawContent()
        }
        drawLayer(backdrop.graphicsLayer)
    }

/**
 * Open-source AGSL Liquid Glass optical lens shader (Android 13+ / API 33+):
 * Performs rounded-rect signed-distance edge lens refraction, subtle RGB chromatic
 * dispersion near the glass rim, and vibrancy saturation boost.
 */
private const val LIQUID_GLASS_AGSL = """
    uniform shader content;
    uniform float2 resolution;

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / max(resolution, float2(1.0, 1.0));
        float2 centered = uv * 2.0 - 1.0;
        float edgeDist = length(max(abs(centered) - float2(0.72, 0.72), 0.0));
        float lensStrength = smoothstep(0.0, 0.38, edgeDist) * 14.0;
        float2 dir = normalize(centered + float2(0.0001, 0.0001));
        float2 offset = dir * lensStrength;

        half4 colR = content.eval(fragCoord - offset * 1.12);
        half4 colG = content.eval(fragCoord - offset * 0.96);
        half4 colB = content.eval(fragCoord - offset * 0.80);
        half3 rgb = half3(colR.r, colG.g, colB.b);

        // Vibrancy boost
        half luma = dot(rgb, half3(0.299, 0.587, 0.114));
        rgb = mix(half3(luma), rgb, 1.28);
        return half4(rgb, colG.a);
    }
"""

/**
 * Optional real open-source Liquid Glass surface powered by Android AGSL RuntimeShader + RenderEffect
 * optical lens refraction, chromatic dispersion, and Gaussian backdrop blur.
 * Falls back cleanly to a crisp elevated frosted Material surface when [enabled] is false.
 */
@Composable
fun OptionalLiquidGlassBox(
    backdrop: LayerBackdrop?,
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(24.dp),
    modifier: Modifier = Modifier,
    fallbackColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    content: @Composable BoxScope.() -> Unit,
) {
    if (enabled && backdrop != null) {
        var boxPos by remember { mutableStateOf(Offset.Zero) }
        var boxSize by remember { mutableStateOf(IntSize.Zero) }
        val surfaceTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.36f)
        val composeRenderEffect = remember(boxSize) {
            if (Build.VERSION.SDK_INT >= 33 && boxSize.width > 0 && boxSize.height > 0) {
                runCatching {
                    val shader = RuntimeShader(LIQUID_GLASS_AGSL).apply {
                        setFloatUniform("resolution", boxSize.width.toFloat(), boxSize.height.toFloat())
                    }
                    val lensEffect = AndroidRenderEffect.createRuntimeShaderEffect(shader, "content")
                    val blurEffect = AndroidRenderEffect.createBlurEffect(26f, 26f, Shader.TileMode.CLAMP)
                    AndroidRenderEffect.createChainEffect(lensEffect, blurEffect).asComposeRenderEffect()
                }.getOrNull()
            } else if (Build.VERSION.SDK_INT >= 31) {
                runCatching {
                    AndroidRenderEffect.createBlurEffect(26f, 26f, Shader.TileMode.CLAMP).asComposeRenderEffect()
                }.getOrNull()
            } else {
                null
            }
        }
        val specularRim = remember {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.62f),
                    Color.White.copy(alpha = 0.16f),
                    Color(0xFF93C5FD).copy(alpha = 0.38f),
                )
            )
        }
        val innerSheen = remember {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.24f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.08f),
                )
            )
        }

        Box(
            modifier = modifier
                .onGloballyPositioned {
                    boxPos = it.positionInRoot()
                    boxSize = it.size
                }
                .clip(shape)
                .border(1.2.dp, specularRim, shape),
        ) {
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        renderEffect = composeRenderEffect
                    }
                    .drawWithContent {
                        val dx = backdrop.rootPosition.x - boxPos.x
                        val dy = backdrop.rootPosition.y - boxPos.y
                        translate(left = dx, top = dy) {
                            runCatching { drawLayer(backdrop.graphicsLayer) }
                        }
                        drawRect(surfaceTint)
                        drawRect(innerSheen)
                    }
            )
            content()
        }
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
