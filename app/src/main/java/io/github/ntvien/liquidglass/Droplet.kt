/*
 * Water-droplet indicator for tabs (iOS 26 style): on touch/drag the droplet rises into clear glass with lens and
 * chromatic aberration, stretches with velocity while moving and bounces back when it stops.
 * Ported from Kyant0/AndroidLiquidGlass (catalog: DampedDragAnimation.kt, LiquidBottomTabs.kt),
 * Copyright Kyant, Apache License 2.0. https://github.com/Kyant0/AndroidLiquidGlass
 * Modified for liquid-glass: SystemClock-based velocity, wrapped into DropletState + Modifier.droplet, optional blur.
 */
package io.github.ntvien.liquidglass

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import io.github.ntvien.liquidglass.internal.backdrop.drawBackdrop
import io.github.ntvien.liquidglass.internal.backdrop.effects.blur
import io.github.ntvien.liquidglass.internal.backdrop.effects.lens
import io.github.ntvien.liquidglass.internal.backdrop.highlight.Highlight
import io.github.ntvien.liquidglass.internal.backdrop.shadow.InnerShadow
import io.github.ntvien.liquidglass.internal.backdrop.shadow.Shadow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Droplet state: position (0..n-1 in tabs), press "lift", stretch and velocity.
 * Every value is read in graphicsLayer/draw, so animating it does not recompose.
 */
@Stable
internal class DropletState(
    private val scope: CoroutineScope,
    initialValue: Float,
    private val valueRange: ClosedFloatingPointRange<Float>,
    private val pressedScale: Float = 78f / 56f,
) {
    private val valueSpec = spring(1f, 1000f, VISIBILITY)
    private val velocitySpec = spring(0.5f, 300f, VISIBILITY * 10f)
    private val pressSpec = spring(1f, 1000f, 0.001f)
    private val scaleXSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYSpec = spring(0.7f, 250f, 0.001f)

    private val valueAnim = Animatable(initialValue, VISIBILITY)
    private val velocityAnim = Animatable(0f, 5f)
    private val pressAnim = Animatable(0f, 0.001f)
    private val scaleXAnim = Animatable(1f, 0.001f)
    private val scaleYAnim = Animatable(1f, 0.001f)
    private val mutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnim.value
    val targetValue: Float get() = valueAnim.targetValue
    val pressProgress: Float get() = pressAnim.value

    /**
     * [pressProgress] cut to 0 below [LIFT_CUTOFF]: the spring's last ~12 frames towards 0 are invisible but would
     * still draw the whole glass stack.
     */
    val lift: Float get() = pressAnim.value.let { if (it < LIFT_CUTOFF) 0f else it }
    val velocity: Float get() = velocityAnim.value

    /** Droplet stretch: swells on press, elongates along the motion and flattens across it. */
    internal val layerBlock: GraphicsLayerScope.() -> Unit = {
        val v = velocityAnim.value / 10f
        scaleX = scaleXAnim.value / (1f - (v * 0.75f).coerceIn(-0.2f, 0.2f))
        scaleY = scaleYAnim.value * (1f - (v * 0.25f).coerceIn(-0.2f, 0.2f))
    }

    fun press() {
        velocityTracker.resetTracking()
        scope.launch {
            launch { pressAnim.animateTo(1f, pressSpec) }
            launch { scaleXAnim.animateTo(pressedScale, scaleXSpec) }
            launch { scaleYAnim.animateTo(pressedScale, scaleYSpec) }
        }
    }

    fun release() {
        scope.launch {
            withFrameNanos { }
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1f) * 0.025f
                snapshotFlow { valueAnim.value }.filter { abs(it - valueAnim.targetValue) < threshold }.first()
            }
            launch { pressAnim.animateTo(0f, pressSpec) }
            launch { scaleXAnim.animateTo(1f, scaleXSpec) }
            launch { scaleYAnim.animateTo(1f, scaleYSpec) }
        }
    }

    /** Follows the finger while dragging. */
    fun dragTo(newValue: Float) {
        val target = newValue.coerceIn(valueRange)
        scope.launch { valueAnim.animateTo(target, valueSpec) { trackVelocity() } }
    }

    /** Tab change by tap: lift → slide → bounce back. */
    fun animateTo(newValue: Float) {
        scope.launch {
            mutex.mutate {
                press()
                launch { valueAnim.animateTo(newValue.coerceIn(valueRange), valueSpec) }
                if (velocity != 0f) launch { velocityAnim.animateTo(0f, velocitySpec) }
                release()
            }
        }
    }

    private fun trackVelocity() {
        velocityTracker.addPosition(SystemClock.uptimeMillis(), Offset(value, 0f))
        val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1f)
        val target = velocityTracker.calculateVelocity().x / span
        scope.launch { velocityAnim.animateTo(target, velocitySpec) }
    }

    /**
     * Drag the droplet across the whole bar (does not consume events, so tabs stay clickable).
     * @param slotWidthPx width of one tab in px, to convert drag distance into tabs.
     * @param onSettle called with the nearest tab index on release.
     */
    // Internal helper bound to this state (pointerInput keyed on it), not a public Modifier factory.
    @Suppress("ModifierFactoryExtensionFunction")
    fun dragModifier(slotWidthPx: () -> Float, onSettle: (Int) -> Unit): Modifier = Modifier.pointerInput(this) {
        awaitEachGesture {
            awaitFirstDown(false, PointerEventPass.Initial)
            val down = awaitFirstDown(false)
            var moved = 0f
            press()
            var pointer = down.id
            while (true) {
                val event = awaitPointerEvent()
                val change: PointerInputChange = event.changes.fastFirstOrNull { it.id == pointer } ?: break
                if (change.changedToUpIgnoreConsumed()) {
                    val other = event.changes.fastFirstOrNull { it.pressed } ?: break
                    pointer = other.id
                    continue
                }
                val dx = change.positionChange().x
                if (dx != 0f) {
                    moved += abs(dx)
                    val w = slotWidthPx().coerceAtLeast(1f)
                    dragTo(targetValue + dx / w)
                }
            }
            if (moved > viewConfiguration.touchSlop) {
                val idx = Math.round(targetValue).coerceIn(valueRange.start.toInt(), valueRange.endInclusive.toInt())
                onSettle(idx)
                animateTo(idx.toFloat())
            } else {
                release()
            }
        }
    }

    private companion object {
        const val VISIBILITY = 0.001f
        const val LIFT_CUTOFF = 0.02f
    }
}

/**
 * Draws the droplet. At rest it is a pill of [idleColor]; while pressed/dragged it becomes clear glass with lens and
 * chromatic aberration. Without real glass (GlassQuality.Off) it keeps the stretch motion, without the lens.
 */
@Composable
internal fun Modifier.droplet(state: DropletState, shape: Shape, idleColor: Color, blur: Dp = 0.dp): Modifier {
    val quality = LocalGlassQuality.current
    val backdrops = LocalGlassBackdrops.current
    val layer = LocalGlassLayer.current
    if (quality == GlassQuality.Off || backdrops == null) {
        return this.graphicsLayer(state.layerBlock).background(idleColor, shape)
    }
    val backdrop = if (layer == GlassLayer.Overlay) backdrops.overlay else backdrops.background
    // When the droplet samples scrolling content (tab bar refracting content) it must be blurred like the glass
    // around it, otherwise sharp text behind it shows through over the tab label.
    val blurRadius = if (backdrop !== backdrops.background) blur else 0.dp
    // At rest (lift 0) the rim and shadows have alpha 0, and a (nearly) opaque pill hides the backdrop under it:
    // skip them, so a resting or sliding droplet does not record the backdrop and three extra layers every frame.
    // 0.9 like the segmented control's shadow: its light pill (raisedFill) is 97 % opaque.
    val opaqueAtRest = idleColor.alpha >= 0.9f
    // The lens is a new RenderEffect whenever its size changes: grow it in LENS_STEPS steps, not every frame. The
    // effects block reads only this step (0 exactly when lift is 0), so the blur + lens chain is rebuilt ~8 times per
    // press instead of on every frame of the press spring.
    val lensStep = remember(state) { derivedStateOf { ceil(state.lift * LENS_STEPS) / LENS_STEPS } }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            val q = lensStep.value
            if (blurRadius > 0.dp && (q > 0f || !opaqueAtRest)) blur(blurRadius.toPx())
            if (q > 0f) lens(10.dp.toPx() * q, 14.dp.toPx() * q, chromaticAberration = true)
        },
        highlight = { state.lift.takeIf { it > 0f }?.let { Highlight.Default.copy(alpha = it) } },
        shadow = { state.lift.takeIf { it > 0f }?.let { Shadow(alpha = it) } },
        // Fixed radius, only alpha animates: alpha is set on the recorded layer, a new radius re-records and re-blurs it.
        innerShadow = { state.lift.takeIf { it > 0f }?.let { InnerShadow(radius = 8.dp, alpha = it) } },
        layerBlock = state.layerBlock,
        onDrawBackdrop = { drawBackdrop -> if (state.lift > 0f || !opaqueAtRest) drawBackdrop() },
        onDrawSurface = {
            drawRect(idleColor, alpha = 1f - state.lift)
        },
    )
}

private const val LENS_STEPS = 8f
