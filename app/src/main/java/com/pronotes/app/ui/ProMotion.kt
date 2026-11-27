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

    /** Snappy fluid spring for pills, chips, segmented indicators, and toolbar buttons. */
    fun <T> snappySpring(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): FiniteAnimationSpec<T> =
        if (reduceMotion) snap()
        else spring(
            dampingRatio = if (style == MotionStyle.MINIMAL_CRISP) 0.88f else 0.72f,
            stiffness = 460f * stiffnessMultiplier(mode, style),
        )

    /** Silky connected spring for dialogs, floating popovers, and sheets. */
    fun <T> sheetSpring(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): FiniteAnimationSpec<T> =
        if (reduceMotion) snap()
        else spring(
            dampingRatio = if (style == MotionStyle.MINIMAL_CRISP) 0.90f else 0.76f,
            stiffness = 380f * stiffnessMultiplier(mode, style),
        )

    /** Expressive yet controlled spring for cards, reorder lifts, and selection pops. */
    fun <T> cardSpring(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): FiniteAnimationSpec<T> =
        if (reduceMotion) snap()
        else spring(
            dampingRatio = if (style == MotionStyle.MINIMAL_CRISP) 0.88f else 0.70f,
            stiffness = 360f * stiffnessMultiplier(mode, style),
        )

    /** Smooth directional slide spring for screen and section transitions. */
    fun navOffsetSpring(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): FiniteAnimationSpec<IntOffset> =
        if (reduceMotion) snap()
        else spring(
            dampingRatio = if (style == MotionStyle.MINIMAL_CRISP) Spring.DampingRatioNoBouncy else 0.86f,
            stiffness = 390f * stiffnessMultiplier(mode, style),
        )

    /** Fast non-blocking color/opacity transition. */
    fun <T> quickTween(reduceMotion: Boolean, durationMs: Int = 210): FiniteAnimationSpec<T> =
        if (reduceMotion) snap() else tween(durationMillis = durationMs, easing = FastOutSlowInEasing)

    /** Forward screen navigation enter (fluid spring slide + depth scale + smooth fade). */
    fun screenEnter(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): EnterTransition {
        if (reduceMotion) return EnterTransition.None
        return fadeIn(tween(240, easing = FastOutSlowInEasing)) +
            slideInHorizontally(animationSpec = navOffsetSpring(false, mode, style)) { (it * 0.10f).toInt() } +
            scaleIn(
                initialScale = 0.955f,
                animationSpec = sheetSpring(false, mode, style),
            )
    }

    /** Forward screen navigation exit (gentle recede with smooth fade). */
    fun screenExit(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): ExitTransition {
        if (reduceMotion) return ExitTransition.None
        return fadeOut(tween(180, easing = FastOutLinearInEasing)) +
            slideOutHorizontally(animationSpec = navOffsetSpring(false, mode, style)) { -(it * 0.06f).toInt() } +
            scaleOut(
                targetScale = 0.97f,
                animationSpec = tween(200, easing = FastOutLinearInEasing),
            )
    }

    /** Back navigation re-enter (pops smoothly back to full depth). */
    fun screenPopEnter(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): EnterTransition {
        if (reduceMotion) return EnterTransition.None
        return fadeIn(tween(220, easing = FastOutSlowInEasing)) +
            slideInHorizontally(animationSpec = navOffsetSpring(false, mode, style)) { -(it * 0.08f).toInt() } +
            scaleIn(
                initialScale = 0.965f,
                animationSpec = sheetSpring(false, mode, style),
            )
    }

    /** Back navigation exit (crisp spring slide-out + fade). */
    fun screenPopExit(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
    ): ExitTransition {
        if (reduceMotion) return ExitTransition.None
        return fadeOut(tween(180, easing = FastOutLinearInEasing)) +
            slideOutHorizontally(animationSpec = navOffsetSpring(false, mode, style)) { (it * 0.10f).toInt() } +
            scaleOut(
                targetScale = 0.955f,
                animationSpec = tween(190, easing = FastOutLinearInEasing),
            )
    }

    /** Silky connected content swap for Library views, Settings categories, and sub-panels. */
    fun contentSwap(
        reduceMotion: Boolean,
        mode: PerformanceMode = PerformanceMode.BALANCED,
        style: MotionStyle = MotionStyle.SPRING,
        horizontalShiftFraction: Float = 0.05f,
    ): ContentTransform {
        if (reduceMotion) return fadeIn(snap()) togetherWith fadeOut(snap())
        val enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
            slideInHorizontally(animationSpec = navOffsetSpring(false, mode, style)) { (it * horizontalShiftFraction).toInt() } +
            scaleIn(initialScale = 0.975f, animationSpec = sheetSpring(false, mode, style))
        val exit = fadeOut(tween(150, easing = FastOutLinearInEasing)) +
            scaleOut(targetScale = 0.985f, animationSpec = tween(150, easing = FastOutLinearInEasing))
        return enter togetherWith exit
    }

    /** Smooth spring top/bottom bar chrome reveal for Reader & Editor overlays. */
    fun barEnter(fromTop: Boolean, reduceMotion: Boolean, mode: PerformanceMode = PerformanceMode.BALANCED): EnterTransition {
        if (reduceMotion) return fadeIn(snap())
        return fadeIn(tween(200, easing = FastOutSlowInEasing)) +
            slideInVertically(animationSpec = navOffsetSpring(false, mode)) { if (fromTop) -(it / 2) else (it / 2) }
    }

    fun barExit(fromTop: Boolean, reduceMotion: Boolean): ExitTransition {
        if (reduceMotion) return fadeOut(snap())
        return fadeOut(tween(160, easing = FastOutLinearInEasing)) +
            slideOutVertically(animationSpec = tween(180, easing = FastOutLinearInEasing)) { if (fromTop) -(it / 2) else (it / 2) }
    }
}

/** GPU-only tactile press scale micro-interaction (zero layout invalidation or per-frame recomposition). */
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
        val scaleState = animateFloatAsState(
            targetValue = if (pressed) pressedScale else 1f,
            animationSpec = ProMotion.snappySpring(false, mode, style),
            label = "tactilePress",
        )
        this.graphicsLayer {
            val s = scaleState.value
            scaleX = s
            scaleY = s
        }
    }
}
