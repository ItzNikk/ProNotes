package com.pronotes.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ambient atmospheric backdrop featuring soft, multi-harmonic drifting radial gradient
 * orbs tuned for both dark and light workspaces. Respects Reduced Motion & Low-Power
 * effects settings so the workspace stays calm, luminous, and silky smooth.
 */
@Composable
fun WorkspaceGlow(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    paused: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val reduceMotion = LocalReduceMotion.current
    val reduceEffects = LocalReduceEffects.current

    val phase = if (reduceMotion || reduceEffects || paused) {
        0.18f
    } else {
        val infinite = rememberInfiniteTransition(label = "workspaceGlow")
        val anim by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 32_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "glowPhase",
        )
        anim
    }

    val p1 = scheme.primary
    val p2 = scheme.secondary
    val p3 = scheme.tertiary
    val p4 = Color(0xFF38BDF8)
    val p5 = Color(0xFFA78BFA)

    // Balanced atmospheric opacity: airy watercolor bloom in light mode, rich aurora depth in dark mode
    val k = intensity.coerceIn(0.25f, 1.5f) * if (reduceEffects) 0.52f else 1f
    val a1 = (if (dark) 0.21f else 0.16f) * k
    val a2 = (if (dark) 0.18f else 0.14f) * k
    val a3 = (if (dark) 0.15f else 0.11f) * k
    val a4 = (if (dark) 0.13f else 0.10f) * k
    val a5 = (if (dark) 0.14f else 0.09f) * k

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val minDim = minOf(w, h)
        val maxDim = maxOf(w, h)
        val tau = (phase * 2.0 * PI).toFloat()

        // Smooth multi-harmonic Lissajous trajectories + gentle organic breathing scale
        fun drawOrb(
            baseX: Float,
            baseY: Float,
            ampX: Float,
            ampY: Float,
            freqX: Float,
            freqY: Float,
            phaseOff: Float,
            baseRadius: Float,
            breatheAmp: Float,
            color: Color,
            alpha: Float,
        ) {
            val cx = w * (baseX + ampX * sin(tau * freqX + phaseOff))
            val cy = h * (baseY + ampY * cos(tau * freqY + phaseOff * 0.7f))
            val breathe = 1f + breatheAmp * sin(tau * 2f + phaseOff * 1.3f)
            val r = (baseRadius * breathe).coerceAtLeast(1f)
            val center = Offset(cx, cy)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to color.copy(alpha = alpha),
                        0.32f to color.copy(alpha = alpha * 0.64f),
                        0.66f to color.copy(alpha = alpha * 0.22f),
                        1.00f to Color.Transparent,
                    ),
                    center = center,
                    radius = r,
                ),
                radius = r,
                center = center,
            )
        }

        // Top-left primary ambient bloom
        drawOrb(
            baseX = 0.14f, baseY = 0.12f,
            ampX = 0.075f, ampY = 0.055f,
            freqX = 1f, freqY = 2f, phaseOff = 0.0f,
            baseRadius = maxDim * 0.56f, breatheAmp = 0.06f,
            color = p1, alpha = a1,
        )
        // Top-right luminous cyan/secondary wash
        drawOrb(
            baseX = 0.86f, baseY = 0.16f,
            ampX = 0.065f, ampY = 0.06f,
            freqX = 2f, freqY = 1f, phaseOff = 1.4f,
            baseRadius = maxDim * 0.50f, breatheAmp = 0.07f,
            color = p4, alpha = a4,
        )
        // Center-right warm accent glow
        drawOrb(
            baseX = 0.80f, baseY = 0.76f,
            ampX = 0.08f, ampY = 0.065f,
            freqX = 1f, freqY = 1f, phaseOff = 2.6f,
            baseRadius = maxDim * 0.54f, breatheAmp = 0.055f,
            color = p2, alpha = a2,
        )
        // Bottom-left deep violet/tertiary balance orb
        drawOrb(
            baseX = 0.18f, baseY = 0.84f,
            ampX = 0.07f, ampY = 0.05f,
            freqX = 1f, freqY = 2f, phaseOff = 4.1f,
            baseRadius = minDim * 0.66f, breatheAmp = 0.065f,
            color = p3, alpha = a3,
        )
        // Subtle floating core mist that ties the palette together across the center
        drawOrb(
            baseX = 0.50f, baseY = 0.46f,
            ampX = 0.11f, ampY = 0.08f,
            freqX = 1f, freqY = 2f, phaseOff = 5.2f,
            baseRadius = minDim * 0.58f, breatheAmp = 0.08f,
            color = p5, alpha = a5,
        )
    }
}

/** Convenience container that layers [WorkspaceGlow] behind full-screen workspace content. */
@Composable
fun GlowingWorkspace(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    paused: Boolean = false,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        WorkspaceGlow(intensity = intensity, paused = paused)
        content()
    }
}
