package io.github.ntvien.liquidglass.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import io.github.ntvien.liquidglass.GlassLayer
import io.github.ntvien.liquidglass.LiquidGlassTheme
import io.github.ntvien.liquidglass.LocalGlassBackdrops
import io.github.ntvien.liquidglass.LocalGlassLayer
import io.github.ntvien.liquidglass.internal.backdrop.backdrops.emptyBackdrop
import io.github.ntvien.liquidglass.internal.backdrop.backdrops.layerBackdrop
import io.github.ntvien.liquidglass.internal.backdrop.backdrops.rememberBackdrop
import io.github.ntvien.liquidglass.internal.backdrop.backdrops.rememberCombinedBackdrop
import io.github.ntvien.liquidglass.internal.backdrop.backdrops.rememberLayerBackdrop
import io.github.ntvien.liquidglass.internal.backdrop.drawBackdrop
import io.github.ntvien.liquidglass.internal.backdrop.effects.blur
import io.github.ntvien.liquidglass.internal.backdrop.effects.lens
import io.github.ntvien.liquidglass.internal.backdrop.effects.vibrancy
import io.github.ntvien.liquidglass.internal.backdrop.highlight.Highlight
import io.github.ntvien.liquidglass.internal.backdrop.shadow.InnerShadow
import io.github.ntvien.liquidglass.internal.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.tanh

private val ContinuousCapsule = RoundedCornerShape(percent = 50)

/**
 * Prebuilt Kyant0/AndroidLiquidGlass LiquidSlider with DampedDragAnimation, trackBackdrop refraction,
 * and OpenLeaf theme integration.
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
    val accentColor = colors.accent
    val trackColor = colors.contentMuted.copy(alpha = 0.24f)
    val sceneBackdrops = LocalGlassBackdrops.current
    val layer = LocalGlassLayer.current
    val resolvedBackdrop = (if (layer == GlassLayer.Overlay) sceneBackdrops?.overlay else sceneBackdrops?.background)
        ?: emptyBackdrop()

    val haptic = LocalHapticFeedback.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentValueRange by rememberUpdatedState(valueRange)
    val currentSteps by rememberUpdatedState(steps)

    fun snapValue(raw: Float): Float {
        val clamped = raw.coerceIn(currentValueRange.start, currentValueRange.endInclusive)
        if (currentSteps <= 0) return clamped
        val stepSize = (currentValueRange.endInclusive - currentValueRange.start) / (currentSteps + 1)
        if (stepSize <= 0f) return clamped
        val idx = round((clamped - currentValueRange.start) / stepSize)
        return (currentValueRange.start + idx * stepSize).coerceIn(currentValueRange.start, currentValueRange.endInclusive)
    }

    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier.fillMaxWidth().height(32.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-5f)

        val dampedDragAnimation = remember(animationScope, valueRange.start, valueRange.endInclusive) {
            KyantDampedDragAnimation(
                animationScope = animationScope,
                initialValue = value.coerceIn(valueRange),
                valueRange = valueRange,
                visibilityThreshold = (span * 0.002f).coerceAtLeast(1e-4f),
                initialScale = 1f,
                pressedScale = 1.45f,
                onDragStarted = {},
                onDragStopped = {
                    if (didDrag) {
                        currentOnValueChange(snapValue(targetValue))
                        didDrag = false
                    }
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    val delta = (currentValueRange.endInclusive - currentValueRange.start) * (dragAmount.x / trackWidth)
                    val next = if (isLtr) (targetValue + delta).coerceIn(currentValueRange)
                    else (targetValue - delta).coerceIn(currentValueRange)
                    val snapped = snapValue(next)
                    updateValue(snapped)
                    currentOnValueChange(snapped)
                },
            )
        }
        LaunchedEffect(dampedDragAnimation, value) {
            snapshotFlow { value.coerceIn(valueRange) }
                .collectLatest { v ->
                    if (abs(dampedDragAnimation.targetValue - v) > 1e-4f) {
                        dampedDragAnimation.updateValue(v)
                    }
                }
        }

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(ContinuousCapsule)
                    .background(trackColor)
                    .pointerInput(animationScope, trackWidth, isLtr) {
                        detectTapGestures { position ->
                            val delta = (currentValueRange.endInclusive - currentValueRange.start) * (position.x / trackWidth)
                            val target = snapValue(
                                if (isLtr) currentValueRange.start + delta
                                else currentValueRange.endInclusive - delta,
                            )
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            dampedDragAnimation.animateToValue(target)
                            currentOnValueChange(target)
                        }
                    }
                    .height(6f.dp)
                    .fillMaxWidth(),
            )

            Box(
                Modifier
                    .clip(ContinuousCapsule)
                    .background(accentColor)
                    .height(6f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress.fastCoerceIn(0f, 1f)).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    },
            )
        }

        Box(
            Modifier
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + trackWidth * dampedDragAnimation.progress.fastCoerceIn(0f, 1f))
                            .fastCoerceIn(-size.width / 4f, (trackWidth - size.width * 3f / 4f).coerceAtLeast(-size.width / 4f)) * if (isLtr) 1f else -1f
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        resolvedBackdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 1f, progress)
                            val scaleY = lerp(0f, 1f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        },
                    ),
                    shape = { ContinuousCapsule },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true,
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = 0.35.dp,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = (0.28f + 0.35f * progress).coerceIn(0f, 0.65f),
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 5f.dp,
                            color = Color.Black.copy(alpha = 0.12f),
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress * 0.7f,
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(colors.raisedFill.copy(alpha = (0.85f - progress * 0.70f).coerceIn(0.15f, 0.85f)))
                    },
                )
                .size(40f.dp, 24f.dp),
        )
    }
}

/**
 * Prebuilt Kyant0/AndroidLiquidGlass LiquidToggle with DampedDragAnimation, trackBackdrop refraction,
 * and OpenLeaf theme integration.
 */
@Composable
public fun LiquidGlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LiquidGlassTheme.colors
    val accentColor = colors.accent
    val trackColor = colors.contentMuted.copy(alpha = 0.26f)
    val sceneBackdrops = LocalGlassBackdrops.current
    val layer = LocalGlassLayer.current
    val resolvedBackdrop = (if (layer == GlassLayer.Overlay) sceneBackdrops?.overlay else sceneBackdrops?.background)
        ?: emptyBackdrop()

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20f.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    val currentChecked by rememberUpdatedState(checked)
    val currentOnChange by rememberUpdatedState(onCheckedChange)
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    val dampedDragAnimation = remember(animationScope) {
        KyantDampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    currentOnChange(fraction == 1f)
                    didDrag = false
                } else {
                    fraction = if (currentChecked) 0f else 1f
                    currentOnChange(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) {
                    didDrag = dragAmount.x != 0f
                }
                val delta = dragAmount.x / dragWidth
                fraction =
                    if (isLtr) (fraction + delta).fastCoerceIn(0f, 1f)
                    else (fraction - delta).fastCoerceIn(0f, 1f)
            },
        )
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { f ->
                dampedDragAnimation.updateValue(f)
            }
    }
    LaunchedEffect(checked) {
        val target = if (checked) 1f else 0f
        if (target != fraction) {
            fraction = target
            dampedDragAnimation.animateToValue(target)
        }
    }

    val trackBackdrop = rememberLayerBackdrop()

    Box(
        modifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(ContinuousCapsule)
                .drawBehind {
                    val f = dampedDragAnimation.value.fastCoerceIn(0f, 1f)
                    drawRect(lerp(trackColor, accentColor, f))
                }
                .size(64f.dp, 28f.dp),
        )

        Box(
            Modifier
                .graphicsLayer {
                    val f = dampedDragAnimation.value.fastCoerceIn(0f, 1f)
                    val padding = 2f.dp.toPx()
                    translationX =
                        if (isLtr) lerp(padding, padding + dragWidth, f)
                        else lerp(-padding, -(padding + dragWidth), f)
                }
                .semantics {
                    role = Role.Switch
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        resolvedBackdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 0.75f, progress)
                            val scaleY = lerp(0f, 0.75f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        },
                    ),
                    shape = { ContinuousCapsule },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            5f.dp.toPx() * progress,
                            10f.dp.toPx() * progress,
                            chromaticAberration = true,
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = 0.35.dp,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = (0.28f + 0.35f * progress).coerceIn(0f, 0.65f),
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 5f.dp,
                            color = Color.Black.copy(alpha = 0.12f),
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress * 0.7f,
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 50f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(colors.raisedFill.copy(alpha = (0.85f - progress * 0.70f).coerceIn(0.15f, 0.85f)))
                    },
                )
                .size(40f.dp, 24f.dp),
        )
    }
}

/**
 * Prebuilt Kyant0/AndroidLiquidGlass LiquidButton with InteractiveHighlight shader and elastic tanh drag physics.
 */
@Composable
public fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = LiquidGlassTheme.colors
    val sceneBackdrops = LocalGlassBackdrops.current
    val layer = LocalGlassLayer.current
    val resolvedBackdrop = (if (layer == GlassLayer.Overlay) sceneBackdrops?.overlay else sceneBackdrops?.background)
        ?: emptyBackdrop()
    val resolvedSurface = if (surfaceColor.isSpecified) surfaceColor else colors.glassFillStrong

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        KyantInteractiveHighlight(
            animationScope = animationScope,
        )
    }

    Row(
        modifier
            .drawBackdrop(
                backdrop = resolvedBackdrop,
                shape = { ContinuousCapsule },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(10f.dp.toPx(), 20f.dp.toPx())
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width.coerceAtLeast(1f)
                        val height = size.height.coerceAtLeast(1f)
                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 4f.dp.toPx() / height, progress)

                        val maxOffset = size.minDimension.coerceAtLeast(1f)
                        val initialDerivative = 0.05f
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = 4f.dp.toPx() / height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            scale +
                                maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension.coerceAtLeast(1f)) *
                                (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension.coerceAtLeast(1f)) *
                                (height / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                    if (resolvedSurface.isSpecified) {
                        drawRect(resolvedSurface)
                    }
                },
            )
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                },
            )
            .height(48f.dp)
            .padding(horizontal = 16f.dp),
        horizontalArrangement = Arrangement.spacedBy(8f.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
