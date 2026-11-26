package com.pronotes.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pronotes.app.data.MotionStyle
import kotlinx.coroutines.delay

/**
 * Playback controller inspired by alexmercerind/moving-letters-android.
 * Allows external start, pause, resume, and reset of character animations.
 */
@Stable
class AnimatedTextState internal constructor(initiallyRunning: Boolean) {
    internal var isPlaying by mutableStateOf(initiallyRunning)
    internal var restartNonce by mutableIntStateOf(0)

    fun start() {
        isPlaying = true
        restartNonce++
    }

    fun pause() {
        isPlaying = false
    }

    fun resume() {
        isPlaying = true
    }

    fun stop() {
        isPlaying = false
        restartNonce++
    }
}

@Composable
fun rememberAnimatedTextState(animateOnMount: Boolean = true): AnimatedTextState =
    remember { AnimatedTextState(initiallyRunning = animateOnMount) }

private enum class LetterMotionMode {
    SCALE_IN,
    FADE,
}

@Composable
fun ScaleInAnimatedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    state: AnimatedTextState = rememberAnimatedTextState(animateOnMount = true),
    staggerMs: Long = 28L,
    startDelayMs: Long = 0L,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) = MovingLettersCore(
    text = text,
    modifier = modifier,
    style = style,
    color = color,
    state = state,
    staggerMs = staggerMs,
    startDelayMs = startDelayMs,
    mode = LetterMotionMode.SCALE_IN,
    maxLines = maxLines,
    overflow = overflow,
)

@Composable
fun FadeAnimatedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    state: AnimatedTextState = rememberAnimatedTextState(animateOnMount = true),
    staggerMs: Long = 22L,
    startDelayMs: Long = 0L,
) = MovingLettersCore(
    text = text,
    modifier = modifier,
    style = style,
    color = color,
    state = state,
    staggerMs = staggerMs,
    startDelayMs = startDelayMs,
    mode = LetterMotionMode.FADE,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovingLettersCore(
    text: String,
    modifier: Modifier,
    style: TextStyle,
    color: Color,
    state: AnimatedTextState,
    staggerMs: Long,
    startDelayMs: Long,
    mode: LetterMotionMode,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val reducedMotion = LocalMotionStyle.current == MotionStyle.REDUCED
    val resolvedColor = if (color != Color.Unspecified) color else style.color
    val resolvedStyle = if (resolvedColor != Color.Unspecified) style.copy(color = resolvedColor) else style

    if (reducedMotion || maxLines == 1 || text.length > 72) {
        // Fallback smoothly for single-line ellipsized headers, reduced motion, or very long passages.
        val alpha = remember(text, state.restartNonce) { Animatable(if (reducedMotion) 1f else 0f) }
        LaunchedEffect(text, state.isPlaying, state.restartNonce, reducedMotion) {
            if (reducedMotion) {
                alpha.snapTo(1f)
            } else if (state.isPlaying) {
                alpha.snapTo(0f)
                delay(startDelayMs)
                alpha.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
            }
        }
        Text(
            text = text,
            style = resolvedStyle,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier.graphicsLayer { this.alpha = alpha.value },
        )
        return
    }

    val words = remember(text) {
        val tokens = ArrayList<Pair<String, Int>>()
        var cursor = 0
        text.split(' ').forEach { word ->
            if (word.isNotEmpty()) {
                tokens += word to cursor
                cursor += word.length + 1
            }
        }
        tokens
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        words.forEach { (word, wordStartIndex) ->
            Row {
                word.forEachIndexed { localIdx, ch ->
                    val globalIndex = wordStartIndex + localIdx
                    val progress = remember(text, globalIndex, state.restartNonce) {
                        Animatable(0f)
                    }
                    LaunchedEffect(text, globalIndex, state.isPlaying, state.restartNonce) {
                        if (!state.isPlaying) return@LaunchedEffect
                        if (progress.value < 1f) {
                            delay(startDelayMs + globalIndex * staggerMs)
                            val spec = when (mode) {
                                LetterMotionMode.SCALE_IN -> spring<Float>(
                                    dampingRatio = 0.58f,
                                    stiffness = Spring.StiffnessMediumLow,
                                )
                                LetterMotionMode.FADE -> tween<Float>(
                                    durationMillis = 360,
                                    easing = FastOutSlowInEasing,
                                )
                            }
                            progress.animateTo(1f, spec)
                        }
                    }

                    Text(
                        text = ch.toString(),
                        style = resolvedStyle,
                        modifier = Modifier.graphicsLayer {
                            val p = progress.value
                            when (mode) {
                                LetterMotionMode.SCALE_IN -> {
                                    alpha = p.coerceIn(0f, 1f)
                                    val s = 0.35f + 0.65f * p
                                    scaleX = s
                                    scaleY = s
                                    translationY = (1f - p) * 14.dp.toPx()
                                }
                                LetterMotionMode.FADE -> {
                                    alpha = p.coerceIn(0f, 1f)
                                    translationY = (1f - p) * 8.dp.toPx()
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
