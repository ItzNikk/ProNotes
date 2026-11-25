package com.pronotes.app.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun GlowingWorkspace(
    paused: Boolean = false,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    MaterialTheme(colorScheme = LocalWorkspaceDarkScheme.current) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
            WorkspaceGlow(Modifier.matchParentSize(), paused)
            content()
        }
    }
}

@Composable
private fun WorkspaceGlow(modifier: Modifier, paused: Boolean) {
    val effects = !LocalClassicAppearance.current && !LocalReduceEffects.current
    val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val phase = if (effects && !paused && !LocalReduceMotion.current && lifecycleState.value == Lifecycle.State.RESUMED) {
        rememberInfiniteTransition(label = "Library background").animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
            label = "Gradient orbit",
        )
    } else null
    Box(modifier.graphicsLayer { clip = true }.drawWithCache {
        val radius = (size.minDimension * 0.72f).coerceAtLeast(1f)
        val primaryGlow = Brush.radialGradient(
            0f to Color(0xFFD7FFF0),
            0.17f to Color(0xFF5CFFD0),
            0.35f to Color(0xF127DFEA),
            0.56f to Color(0xC04479FF),
            0.79f to Color(0x636236FA),
            1f to Color(0x006236FA),
            center = Offset.Zero,
            radius = radius,
        )
        val secondaryGlow = Brush.radialGradient(
            0f to Color(0xFFFFF3B4),
            0.17f to Color(0xFFFFC465),
            0.35f to Color(0xF8FF796F),
            0.56f to Color(0xCFED40B7),
            0.79f to Color(0x687B34ED),
            1f to Color(0x007B34ED),
            center = Offset.Zero,
            radius = radius,
        )
        onDrawBehind {
            val angle = phase?.value ?: 0f
            val dx = cos(angle) * size.width * 0.20f
            val dy = sin(angle) * size.height * 0.16f
            val pulse = sin(angle) * 0.10f
            val primaryAlpha = if (effects) 0.90f + pulse else 0.55f
            val secondaryAlpha = if (effects) 0.90f - pulse else 0.55f
            translate(size.width * 0.24f + dx, size.height * 0.32f + dy) {
                drawCircle(primaryGlow, radius, center = Offset.Zero, alpha = primaryAlpha)
            }
            translate(size.width * 0.76f - dx, size.height * 0.73f - dy) {
                drawCircle(secondaryGlow, radius, center = Offset.Zero, alpha = secondaryAlpha)
            }
        }
    })
}
