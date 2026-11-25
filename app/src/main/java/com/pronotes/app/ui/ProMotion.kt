package com.pronotes.app.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import com.pronotes.app.data.MotionStyle
import com.pronotes.app.data.PerformanceMode

/**
 * Unified crisp spring motion language for Pro Notes, calibrated for 60Hz–120Hz displays.
 * Automatically adapts to [LocalReduceMotion], [LocalMotionStyle], and [LocalPerformanceMode].
 */
object ProMotion {

    private fun stiffnessMultiplier(mode: PerformanceMode, style: MotionStyle): Float = when {
        style == MotionStyle.MINIMAL_CRISP -> 1.25f
        mode == PerformanceMode.MAXIMUM -> 1.15f
        else -> 1.0f
    }

    /** Snappy tactile spring for pills, chips, segmented indicators, and toolbar buttons. */
    fun <T> snappySpring(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): FiniteAnimationSpec<T> =
        if (reduceMotion) snap()
        else spring(
            dampingRatio = if (style == MotionStyle.MINIMAL_CRISP) 0.92f else 0.80f,
            stiffness = 620f * stiffnessMultiplier(mode, style),
        )

    /** Smooth crisp spring for dialogs, floating popovers, and sheets. */
    fun <T> sheetSpring(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): FiniteAnimationSpec<T> =
        if (reduceMotion) snap()
        else spring(
            dampingRatio = if (style == MotionStyle.MINIMAL_CRISP) 0.94f else 0.82f,
            stiffness = 540f * stiffnessMultiplier(mode, style),
        )

    /** Expressive yet controlled spring for cards, reorder lifts, and selection pops. */
    fun <T> cardSpring(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): FiniteAnimationSpec<T> =
        if (reduceMotion) snap()
        else spring(
            dampingRatio = if (style == MotionStyle.MINIMAL_CRISP) 0.90f else 0.76f,
            stiffness = 520f * stiffnessMultiplier(mode, style),
        )

    /** Crisp directional slide spring for screen and section transitions. */
    fun navOffsetSpring(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): FiniteAnimationSpec<IntOffset> =
        if (reduceMotion) snap()
        else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 560f * stiffnessMultiplier(mode, style),
        )

    /** Fast non-blocking color/opacity transition. */
    fun <T> quickTween(reduceMotion: Boolean, durationMs: Int = 160): FiniteAnimationSpec<T> =
        if (reduceMotion) snap() else tween(durationMillis = durationMs, easing = FastOutSlowInEasing)

    /** Forward screen navigation enter (subtle slide + depth scale + fast fade). */
    fun screenEnter(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): EnterTransition {
        if (reduceMotion) return EnterTransition.None
        return fadeIn(tween(170, easing = FastOutSlowInEasing)) +
            slideInHorizontally(animationSpec = navOffsetSpring(false, mode, style)) { (it * 0.06f).toInt() } +
            scaleIn(
                initialScale = 0.975f,
                animationSpec = sheetSpring(false, mode, style),
            )
    }

    /** Forward screen navigation exit (gentle recede with quick fade). */
    fun screenExit(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): ExitTransition {
        if (reduceMotion) return ExitTransition.None
        return fadeOut(tween(130, easing = FastOutLinearInEasing)) +
            slideOutHorizontally(animationSpec = navOffsetSpring(false, mode, style)) { -(it * 0.04f).toInt() } +
            scaleOut(
                targetScale = 0.985f,
                animationSpec = tween(140, easing = FastOutLinearInEasing),
            )
    }

    /** Back navigation re-enter (pops smoothly back to full depth). */
    fun screenPopEnter(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): EnterTransition {
        if (reduceMotion) return EnterTransition.None
        return fadeIn(tween(160, easing = FastOutSlowInEasing)) +
            slideInHorizontally(animationSpec = navOffsetSpring(false, mode, style)) { -(it * 0.05f).toInt() } +
            scaleIn(
                initialScale = 0.985f,
                animationSpec = sheetSpring(false, mode, style),
            )
    }

    /** Back navigation exit (crisp slide-out + fade). */
    fun screenPopExit(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): ExitTransition {
        if (reduceMotion) return ExitTransition.None
        return fadeOut(tween(135, easing = FastOutLinearInEasing)) +
            slideOutHorizontally(animationSpec = navOffsetSpring(false, mode, style)) { (it * 0.06f).toInt() } +
            scaleOut(
                targetScale = 0.975f,
                animationSpec = tween(135, easing = FastOutLinearInEasing),
            )
    }

    /** Crisp content swap for Library views, Settings categories, and sub-panels. */
    fun contentSwap(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
        horizontalShiftFraction: Float = 0.04f,
    ): ContentTransform {
        if (reduceMotion) return fadeIn(snap()) togetherWith fadeOut(snap())
        val enter = fadeIn(tween(160, easing = FastOutSlowInEasing)) +
            slideInHorizontally(animationSpec = navOffsetSpring(false, mode, style)) { (it * horizontalShiftFraction).toInt() }
        val exit = fadeOut(tween(115, easing = FastOutLinearInEasing))
        return enter togetherWith exit
    }

    /** Crisp top/bottom bar chrome reveal for Reader & Editor overlays. */
    fun barEnter(fromTop: Boolean, reduceMotion: Boolean, mode: PerformanceMode = PerformanceMode.BALANCED): EnterTransition {
        if (reduceMotion) return fadeIn(snap())
        return fadeIn(tween(150, easing = FastOutSlowInEasing)) +
            slideInVertically(animationSpec = navOffsetSpring(false, mode)) { if (fromTop) -(it / 3) else (it / 3) }
    }

    fun barExit(fromTop: Boolean, reduceMotion: Boolean): ExitTransition {
        if (reduceMotion) return fadeOut(snap())
        return fadeOut(tween(120, easing = FastOutLinearInEasing)) +
            slideOutVertically(animationSpec = tween(130, easing = FastOutLinearInEasing)) { if (fromTop) -(it / 3) else (it / 3) }
    }
}

/** GPU-only tactile press scale micro-interaction (zero layout invalidation). */
fun Modifier.tactilePressScale(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val mode = LocalPerformanceMode.current
    val style = LocalMotionStyle.current
    if (!enabled || reduceMotion) {
        this
    } else {
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) pressedScale else 1f,
            animationSpec = ProMotion.snappySpring(false, mode, style),
            label = "tactilePress",
        )
        this.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }
}
