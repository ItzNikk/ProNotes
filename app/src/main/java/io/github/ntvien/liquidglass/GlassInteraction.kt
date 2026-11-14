package io.github.ntvien.liquidglass

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
import io.github.ntvien.liquidglass.internal.backdrop.RuntimeShader
import io.github.ntvien.liquidglass.internal.backdrop.asComposeShader
import io.github.ntvien.liquidglass.internal.backdrop.isRuntimeShaderSupported
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
 * Never consumes pointer events, so child or parent clickables always receive taps cleanly.
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

    /** 0 = released, 1 = fully pressed. */
    public val pressProgress: Float get() = press.value

    /** Finger offset from where the press started. */
    public val dragOffset: Offset get() = position.value - start

    private val shader = if (isRuntimeShaderSupported()) {
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
}"""
        )
    } else {
        null
    }

    /** Elastic spring swell + directional drag stretch toward the finger. */
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

    /** Luminous dual-tone radial touch glow at the pointer position. */
    internal val highlightModifier: Modifier = Modifier.drawWithContent {
        drawContent()
        val progress = press.value
        if (progress > 0.005f) {
            val p = Offset(
                position.value.x.fastCoerceIn(0f, size.width),
                position.value.y.fastCoerceIn(0f, size.height),
            )
            val radius = max(size.width, size.height).coerceAtLeast(64f) * 0.85f
            if (shader != null) {
                drawRect(Color.White.copy(alpha = 0.06f * progress), blendMode = BlendMode.Plus)
                shader.setFloatUniform("size", size.width, size.height)
                shader.setColorUniform("coreColor", Color.White.copy(alpha = 0.34f * progress))
                shader.setColorUniform("auraColor", glowColor.copy(alpha = 0.24f * progress))
                shader.setFloatUniform("radius", radius)
                shader.setFloatUniform("position", p.x, p.y)
                drawRect(ShaderBrush(shader.asComposeShader()), blendMode = BlendMode.Plus)
            } else {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.36f * progress),
                            glowColor.copy(alpha = 0.18f * progress),
                            Color.Transparent,
                        ),
                        center = p,
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

/**
 * Remembers a [GlassInteraction] for a pressable glass element.
 */
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

/** Tracks press and drag WITHOUT consuming events, so a clickable on the same element still gets the tap. */
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
