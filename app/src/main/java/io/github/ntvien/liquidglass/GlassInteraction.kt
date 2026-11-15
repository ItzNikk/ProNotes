package io.github.ntvien.liquidglass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Interactive press, directional elastic stretch, and luminous radial touch glow for Liquid Glass elements.
 * Caches [ShaderBrush] and dirty-checks shader uniforms so zero allocations occur inside draw frames.
 */
@Stable
public class GlassInteraction internal constructor(
    private val scope: CoroutineScope,
    dampingRatio: Float,
    stiffness: Float,
    private val glowColor: Color = Color(0xFF7CE8D8),
) {
    private val pressSpec = spring(dampingRatio, stiffness, 0.001f)
    private val positionSpec = spring(dampingRatio, stiffness, Offset.VisibilityThreshold)
    private val press = Animatable(0f, 0.001f)
    private val position = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)
    private var start = Offset.Zero

    public val pressProgress: Float get() = press.value
    public val dragOffset: Offset get() = position.value - start

    private val shader: RuntimeShader? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        RuntimeShader(
            """
uniform float2 size;
layout(color) uniform half4 coreColor;
layout(color) uniform half4 auraColor;
uniform float radius;
uniform float2 position;
half4 main(float2 coord) {
    float dist = distance(coord, position);
    float core = smoothstep(radius * 0.55, 0.0, dist);
    float aura = smoothstep(radius, radius * 0.15, dist);
    return coreColor * core + auraColor * aura;
}""",
        )
    } else {
        null
    }

    // Cached once — zero per-frame ShaderBrush allocations.
    private val cachedShaderBrush: ShaderBrush? = shader?.let { ShaderBrush(it) }

    // Dirty-check cache so uniforms only update when values actually change.
    private var lastW = Float.NaN
    private var lastH = Float.NaN
    private var lastRadius = Float.NaN
    private var lastPx = Float.NaN
    private var lastPy = Float.NaN
    private var lastCoreArgb = 0
    private var lastAuraArgb = 0

    internal val layerBlock: GraphicsLayerScope.() -> Unit = {
        val w = size.width
        val h = size.height
        if (w > 0f && h > 0f) {
            val progress = press.value
            val swell = (6.dp.toPx() / max(w, h).coerceAtLeast(48f)).coerceIn(0.025f, 0.065f)
            val baseScale = lerp(1f, 1f + swell, progress)
            val maxOffset = size.minDimension.coerceAtLeast(1f)
            val offset = position.value - start
            translationX = maxOffset * tanh(0.065f * offset.x / maxOffset)
            translationY = maxOffset * tanh(0.065f * offset.y / maxOffset)
            val maxDragScale = (8.dp.toPx() / h.coerceAtLeast(36f)).coerceIn(0.02f, 0.08f)
            val angle = atan2(offset.y, offset.x)
            val maxDim = size.maxDimension.coerceAtLeast(1f)
            scaleX = baseScale + maxDragScale * abs(cos(angle) * offset.x / maxDim) * (w / h).fastCoerceAtMost(1f)
            scaleY = baseScale + maxDragScale * abs(sin(angle) * offset.y / maxDim) * (h / w).fastCoerceAtMost(1f)
        }
    }

    internal val highlightModifier: Modifier = Modifier.drawWithContent {
        drawContent()
        val progress = press.value
        if (progress > 0.005f) {
            val px = position.value.x.fastCoerceIn(0f, size.width)
            val py = position.value.y.fastCoerceIn(0f, size.height)
            val radius = max(size.width, size.height).coerceAtLeast(64f) * 0.85f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null && cachedShaderBrush != null) {
                drawRect(Color.White.copy(alpha = 0.06f * progress), blendMode = BlendMode.Plus)
                if (size.width != lastW || size.height != lastH) {
                    shader.setFloatUniform("size", size.width, size.height)
                    lastW = size.width
                    lastH = size.height
                }
                if (radius != lastRadius) {
                    shader.setFloatUniform("radius", radius)
                    lastRadius = radius
                }
                if (px != lastPx || py != lastPy) {
                    shader.setFloatUniform("position", px, py)
                    lastPx = px
                    lastPy = py
                }
                val coreArgb = Color.White.copy(alpha = 0.34f * progress).toArgb()
                if (coreArgb != lastCoreArgb) {
                    shader.setColorUniform("coreColor", coreArgb)
                    lastCoreArgb = coreArgb
                }
                val auraArgb = glowColor.copy(alpha = 0.24f * progress).toArgb()
                if (auraArgb != lastAuraArgb) {
                    shader.setColorUniform("auraColor", auraArgb)
                    lastAuraArgb = auraArgb
                }
                drawRect(cachedShaderBrush, blendMode = BlendMode.Plus)
            } else {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.36f * progress),
                            glowColor.copy(alpha = 0.18f * progress),
                            Color.Transparent,
                        ),
                        center = Offset(px, py),
                        radius = radius,
                    ),
                    blendMode = BlendMode.Plus,
                )
            }
        }
    }

    internal val gestureModifier: Modifier = Modifier.pointerInput(this) {
        inspectDragGestures(
            onDragStart = { down ->
                start = down.position
                scope.launch {
                    launch { press.animateTo(1f, pressSpec) }
                    launch { position.snapTo(start) }
                }
            },
            onDragEnd = { release() },
            onDragCancel = { release() },
        ) { change -> scope.launch { position.snapTo(change.position) } }
    }

    private fun release() {
        scope.launch {
            launch { press.animateTo(0f, pressSpec) }
            launch { position.animateTo(start, positionSpec) }
        }
    }
}

@Composable
public fun rememberGlassInteraction(
    dampingRatio: Float = 0.48f,
    stiffness: Float = 360f,
): GlassInteraction {
    val scope = rememberCoroutineScope()
    val accent = LocalGlassColors.current.accent
    return remember(scope, dampingRatio, stiffness, accent) {
        GlassInteraction(scope, dampingRatio, stiffness, glowColor = accent)
    }
}

private suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDrag: (change: PointerInputChange) -> Unit,
) {
    awaitEachGesture {
        val initialDown = awaitFirstDown(false, PointerEventPass.Initial)
        val down = awaitFirstDown(false)
        onDragStart(down)
        onDrag(initialDown)
        val up = trackDrag(initialDown.id, onDrag)
        if (up == null) onDragCancel() else onDragEnd()
    }
}

private suspend fun AwaitPointerEventScope.trackDrag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
    if (currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true) return null
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (change.changedToUpIgnoreConsumed()) {
            val other = event.changes.fastFirstOrNull { it.pressed } ?: return change
            pointer = other.id
            continue
        }
        if (change.isConsumed) return null
        if (change.previousPosition != change.position) onDrag(change)
    }
}
