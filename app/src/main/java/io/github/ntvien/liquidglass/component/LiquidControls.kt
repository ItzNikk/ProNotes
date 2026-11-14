package io.github.ntvien.liquidglass.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.ntvien.liquidglass.DropletState
import io.github.ntvien.liquidglass.GlassLayer
import io.github.ntvien.liquidglass.GlassQuality
import io.github.ntvien.liquidglass.LiquidGlassTheme
import io.github.ntvien.liquidglass.LocalGlassBackdrops
import io.github.ntvien.liquidglass.LocalGlassLayer
import io.github.ntvien.liquidglass.LocalGlassQuality
import io.github.ntvien.liquidglass.droplet
import io.github.ntvien.liquidglass.internal.backdrop.drawBackdrop
import io.github.ntvien.liquidglass.internal.backdrop.effects.blur
import io.github.ntvien.liquidglass.internal.backdrop.effects.lens
import io.github.ntvien.liquidglass.internal.backdrop.highlight.Highlight
import io.github.ntvien.liquidglass.internal.backdrop.shadow.InnerShadow
import io.github.ntvien.liquidglass.internal.backdrop.shadow.Shadow
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Scene-aware Liquid Glass slider built on ntvien/liquid-glass's Droplet & LocalGlassBackdrops.
 * Eliminates orphan LayerBackdrops, stale layout widths, and transparent-thumb bugs.
 */
@Composable
public fun LiquidGlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    val colors = LiquidGlassTheme.colors
    val quality = LocalGlassQuality.current
    val backdrops = LocalGlassBackdrops.current
    val layer = LocalGlassLayer.current
    val haptic = LocalHapticFeedback.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-5f)
    val clamped = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val targetFraction = ((clamped - valueRange.start) / span).coerceIn(0f, 1f)

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentValueRange by rememberUpdatedState(valueRange)
    val currentSteps by rememberUpdatedState(steps)

    var dragging by remember { mutableFloatStateOf(-1f) }
    val pressAnim = remember { Animatable(0f) }
    val posAnim = remember { Animatable(targetFraction) }
    var lastHapticBucket by remember { mutableFloatStateOf(-1f) }

    LaunchedEffect(targetFraction, dragging) {
        if (dragging < 0f && abs(posAnim.targetValue - targetFraction) > 0.002f) {
            posAnim.animateTo(targetFraction, spring(dampingRatio = 0.82f, stiffness = 620f))
        }
    }

    fun snapToFraction(f: Float): Float {
        val raw = (currentValueRange.start + f.coerceIn(0f, 1f) * (currentValueRange.endInclusive - currentValueRange.start))
            .coerceIn(currentValueRange.start, currentValueRange.endInclusive)
        if (currentSteps <= 0) return raw
        val stepSize = (currentValueRange.endInclusive - currentValueRange.start) / (currentSteps + 1)
        val idx = kotlin.math.round((raw - currentValueRange.start) / stepSize)
        return (currentValueRange.start + idx * stepSize).coerceIn(currentValueRange.start, currentValueRange.endInclusive)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val thumbW = 38.dp
        val thumbH = 24.dp
        val thumbWPx = with(density) { thumbW.toPx() }
        val usablePx = (trackWidthPx - thumbWPx).coerceAtLeast(1f)
        val activeFraction = if (dragging >= 0f) dragging else posAnim.value
        val trackBg = colors.contentMuted.copy(alpha = 0.22f)

        // Track + touch interaction
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(usablePx, thumbWPx, isLtr) {
                    detectTapGestures { pos ->
                        val frac = (((pos.x - thumbWPx / 2f) / usablePx).let { if (isLtr) it else 1f - it }).coerceIn(0f, 1f)
                        val snapped = snapToFraction(frac)
                        val outFrac = ((snapped - currentValueRange.start) / (currentValueRange.endInclusive - currentValueRange.start)).coerceIn(0f, 1f)
                        scope.launch { posAnim.animateTo(outFrac, spring(0.8f, 680f)) }
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        currentOnValueChange(snapped)
                    }
                }
                .pointerInput(usablePx, thumbWPx, isLtr) {
                    detectHorizontalDragGestures(
                        onDragStart = { start ->
                            val startFrac = (((start.x - thumbWPx / 2f) / usablePx).let { if (isLtr) it else 1f - it }).coerceIn(0f, 1f)
                            dragging = startFrac
                            scope.launch {
                                pressAnim.animateTo(1f, spring(0.7f, 650f))
                                posAnim.snapTo(startFrac)
                            }
                            currentOnValueChange(snapToFraction(startFrac))
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val deltaFrac = (dragAmount / usablePx) * if (isLtr) 1f else -1f
                            val nextFrac = ((if (dragging >= 0f) dragging else posAnim.value) + deltaFrac).coerceIn(0f, 1f)
                            dragging = nextFrac
                            val bucket = (nextFrac * 10f).toInt().toFloat()
                            if (bucket != lastHapticBucket) {
                                lastHapticBucket = bucket
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            }
                            currentOnValueChange(snapToFraction(nextFrac))
                        },
                        onDragEnd = {
                            val endFrac = dragging.coerceIn(0f, 1f)
                            dragging = -1f
                            scope.launch {
                                posAnim.snapTo(endFrac)
                                pressAnim.animateTo(0f, spring(0.75f, 550f))
                            }
                        },
                        onDragCancel = {
                            dragging = -1f
                            scope.launch { pressAnim.animateTo(0f, spring(0.75f, 550f)) }
                        },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            // Base track
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(trackBg)
                    .border(0.75.dp, colors.glassStroke.copy(alpha = 0.55f), CircleShape),
            )
            // Filled track
            Box(
                Modifier
                    .fillMaxWidth(activeFraction.coerceIn(0.001f, 1f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }

        // Thumb
        val backdrop = if (quality != GlassQuality.Off && backdrops != null) {
            if (layer == GlassLayer.Overlay) backdrops.overlay else backdrops.background
        } else {
            null
        }
        val thumbBaseMod = Modifier
            .graphicsLayer {
                val tx = usablePx * activeFraction
                translationX = if (isLtr) tx else -tx
                val sc = 1f + 0.14f * pressAnim.value
                scaleX = sc
                scaleY = sc
            }
            .size(thumbW, thumbH)

        val thumbMod = if (backdrop != null) {
            thumbBaseMod.drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    val p = pressAnim.value
                    if (quality == GlassQuality.Full && p > 0.05f) {
                        lens(8.dp.toPx() * p, 12.dp.toPx() * p, chromaticAberration = true)
                    } else if (quality != GlassQuality.Off) {
                        blur(4.dp.toPx() * (1f - p * 0.6f))
                    }
                },
                highlight = { Highlight.Ambient.copy(alpha = 0.7f + 0.3f * pressAnim.value) },
                shadow = { Shadow(radius = 6.dp, color = colors.glassShadow) },
                innerShadow = { InnerShadow(radius = 4.dp, alpha = 0.25f * pressAnim.value) },
                onDrawSurface = {
                    // Always maintain crisp high-contrast rim & surface so thumb never disappears on parchment
                    drawRect(colors.raisedFill.copy(alpha = (0.94f - 0.18f * pressAnim.value).coerceIn(0.72f, 0.98f)))
                },
            ).border(1.dp, colors.accent.copy(alpha = 0.32f), CircleShape)
        } else {
            thumbBaseMod
                .shadow(4.dp, CircleShape, ambientColor = colors.glassShadow, spotColor = colors.glassShadow)
                .clip(CircleShape)
                .background(colors.raisedFill)
                .border(1.2.dp, colors.accent.copy(alpha = 0.42f), CircleShape)
        }

        Box(thumbMod)
    }
}

/**
 * Scene-aware Liquid Glass toggle built on ntvien/liquid-glass's Droplet & LocalGlassBackdrops.
 */
@Composable
public fun LiquidGlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LiquidGlassTheme.colors
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val density = LocalDensity.current
    val currentChecked by rememberUpdatedState(checked)
    val currentOnChange by rememberUpdatedState(onCheckedChange)

    val droplet = remember(scope) {
        DropletState(scope, if (checked) 1f else 0f, 0f..1f, pressedScale = 1.24f)
    }
    LaunchedEffect(checked) {
        val target = if (checked) 1f else 0f
        if (abs(droplet.targetValue - target) > 0.01f) {
            droplet.animateTo(target)
        }
    }

    val trackW = 58.dp
    val trackH = 30.dp
    val thumbW = 32.dp
    val thumbH = 24.dp
    val travelPx = with(density) { (trackW - thumbW - 6.dp).toPx() }.coerceAtLeast(1f)
    val trackIdleColor = colors.contentMuted.copy(alpha = 0.24f)

    Box(
        modifier = modifier
            .size(trackW, trackH)
            .clip(CircleShape)
            .background(lerp(trackIdleColor, colors.accent, droplet.value.coerceIn(0f, 1f)))
            .border(0.75.dp, colors.glassStroke.copy(alpha = 0.55f), CircleShape)
            .clickable(interactionSource = null, indication = null, role = Role.Switch) {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                currentOnChange(!currentChecked)
            }
            .pointerInput(droplet, travelPx, isLtr) {
                awaitEachGesture {
                    val down = awaitFirstDown(false)
                    var moved = 0f
                    droplet.press()
                    var pointer = down.id
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == pointer } ?: break
                        if (!ch.pressed) break
                        val dx = ch.position.x - ch.previousPosition.x
                        if (abs(dx) > 0.5f) {
                            moved += abs(dx)
                            val delta = (dx / travelPx) * if (isLtr) 1f else -1f
                            droplet.dragTo((droplet.targetValue + delta).coerceIn(0f, 1f))
                        }
                    }
                    if (moved > viewConfiguration.touchSlop) {
                        val next = droplet.targetValue >= 0.5f
                        droplet.animateTo(if (next) 1f else 0f)
                        if (next != currentChecked) {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            currentOnChange(next)
                        }
                    } else {
                        val next = !currentChecked
                        droplet.animateTo(if (next) 1f else 0f)
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        currentOnChange(next)
                    }
                }
            }
            .semantics { role = Role.Switch }
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .graphicsLayer {
                    val tx = travelPx * droplet.value.coerceIn(0f, 1f)
                    translationX = if (isLtr) tx else -tx
                }
                .size(thumbW, thumbH)
                .shadow(3.dp, CircleShape, clip = false, ambientColor = colors.glassShadow, spotColor = colors.glassShadow)
                .droplet(
                    state = droplet,
                    shape = CircleShape,
                    idleColor = colors.raisedFill,
                )
                .border(0.75.dp, colors.glassStroke.copy(alpha = 0.6f), CircleShape),
        )
    }
}
