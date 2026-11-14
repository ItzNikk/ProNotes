/*
 * Press / drag effect for glass (the glass swells, tilts toward the finger and glints at the touch point).
 * Ported from Kyant0/AndroidLiquidGlass (catalog: InteractiveHighlight.kt, DragGestureInspector.kt, LiquidButton.kt),
 * Copyright Kyant, Apache License 2.0. https://github.com/Kyant0/AndroidLiquidGlass
 * Modified for liquid-glass: spring parameters exposed, wrapped into a reusable GlassInteraction.
 */
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
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Press state of a glass element: it swells when pressed, tilts toward the finger while dragged and glints at the
 * touch point. It never consumes touches, so `clickable` on the same element still receives the tap.
 *
 * Create it with [rememberGlassInteraction] and pass it to [liquidGlass]. [LiquidSurface] does this for you when it
 * has an `onClick`. All values are read in the draw/layer phase: pressing does not recompose.
 */
@Stable
public class GlassInteraction internal constructor(
    private val scope: CoroutineScope,
    dampingRatio: Float,
    stiffness: Float,
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
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;
half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}"""
        )
    } else {
        null
    }

    /** Swell + tilt toward the finger. */
    internal val layerBlock: GraphicsLayerScope.() -> Unit = {
        val w = size.width
        val h = size.height
        if (w > 0f && h > 0f) {
            val progress = press.value
            val scale = lerp(1f, 1f + 4.dp.toPx() / h, progress)
            val maxOffset = size.minDimension
            val offset = position.value - start
            translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
            translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)
            val maxDragScale = 4.dp.toPx() / h
            val angle = atan2(offset.y, offset.x)
            scaleX = scale + maxDragScale * abs(cos(angle) * offset.x / size.maxDimension) * (w / h).fastCoerceAtMost(1f)
            scaleY = scale + maxDragScale * abs(sin(angle) * offset.y / size.maxDimension) * (h / w).fastCoerceAtMost(1f)
        }
    }

    /** Glint at the touch point. */
    internal val highlightModifier: Modifier = Modifier.drawWithContent {
        val progress = press.value
        if (progress > 0f) {
            if (shader != null) {
                drawRect(Color.White.copy(alpha = 0.08f * progress), blendMode = BlendMode.Plus)
                val p = position.value
                shader.setFloatUniform("size", size.width, size.height)
                shader.setColorUniform("color", Color.White.copy(alpha = 0.15f * progress))
                shader.setFloatUniform("radius", size.minDimension * 1.5f)
                shader.setFloatUniform("position", p.x.fastCoerceIn(0f, size.width), p.y.fastCoerceIn(0f, size.height))
                drawRect(ShaderBrush(shader.asComposeShader()), blendMode = BlendMode.Plus)
            } else {
                drawRect(Color.White.copy(alpha = 0.25f * progress), blendMode = BlendMode.Plus)
            }
        }
        drawContent()
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
 *
 * @param dampingRatio spring damping of the press; lower bounces more.
 * @param stiffness spring stiffness of the press.
 */
@Composable
public fun rememberGlassInteraction(dampingRatio: Float = 0.5f, stiffness: Float = 300f): GlassInteraction {
    val scope = rememberCoroutineScope()
    return remember(scope, dampingRatio, stiffness) { GlassInteraction(scope, dampingRatio, stiffness) }
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
