package com.pronotes.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class GlowOrbSpec(
    val baseX: Float,
    val baseY: Float,
    val ampX: Float,
    val ampY: Float,
    val freqX: Float,
    val freqY: Float,
    val phaseOff: Float,
    val radiusFactor: Float,
    val useMinDim: Boolean,
    val breatheAmp: Float,
    val color: Color,
    val coreAlpha: Float,
)

/**
 * Silky 120Hz zero-allocation atmospheric gradient circles backdrop for Documents & Settings screens.
 * Pre-allocates all radial gradient shaders once in [drawWithCache] and drives smooth counter-orbiting
 * motion purely via GPU matrix transforms (zero per-frame Compose recompositions or Brush allocations).
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

    // Read state ONLY inside onDrawBehind so Compose never recomposes WorkspaceGlow on animation frames.
    val phaseState = if (reduceMotion || reduceEffects || paused) {
        null
    } else {
        val infinite = rememberInfiniteTransition(label = "workspaceGlow")
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 26_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "glowPhase",
        )
    }

    val k = intensity.coerceIn(0.30f, 1.5f) * if (reduceEffects) 0.58f else 1f
    val orbs = remember(scheme.primary, scheme.secondary, scheme.tertiary, dark, k) {
        val cBlue = if (dark) Color(0xFF3B82F6) else Color(0xFF2563EB)
        val cCyan = if (dark) Color(0xFF22D3EE) else Color(0xFF06B6D4)
        val cViolet = if (dark) Color(0xFF8B5CF6) else Color(0xFF7C3AED)
        val cRose = if (dark) Color(0xFFF472B6) else Color(0xFFEC4899)
        val cMint = if (dark) Color(0xFF34D399) else Color(0xFF10B981)
        listOf(
            // Top-left luminous electric sapphire orb
            GlowOrbSpec(
                baseX = 0.16f, baseY = 0.14f,
                ampX = 0.10f, ampY = 0.08f,
                freqX = 1f, freqY = 2f, phaseOff = 0.0f,
                radiusFactor = 0.44f, useMinDim = false, breatheAmp = 0.09f,
                color = cBlue, coreAlpha = (if (dark) 0.34f else 0.24f) * k,
            ),
            // Top-right crisp aqua-cyan orb
            GlowOrbSpec(
                baseX = 0.84f, baseY = 0.18f,
                ampX = 0.09f, ampY = 0.08f,
                freqX = 2f, freqY = 1f, phaseOff = 1.35f,
                radiusFactor = 0.38f, useMinDim = false, breatheAmp = 0.10f,
                color = cCyan, coreAlpha = (if (dark) 0.28f else 0.21f) * k,
            ),
            // Mid-right vibrant violet-iris orb
            GlowOrbSpec(
                baseX = 0.78f, baseY = 0.74f,
                ampX = 0.11f, ampY = 0.09f,
                freqX = 1f, freqY = 1f, phaseOff = 2.65f,
                radiusFactor = 0.42f, useMinDim = false, breatheAmp = 0.085f,
                color = cViolet, coreAlpha = (if (dark) 0.30f else 0.22f) * k,
            ),
            // Bottom-left warm rose-magenta bloom
            GlowOrbSpec(
                baseX = 0.18f, baseY = 0.82f,
                ampX = 0.09f, ampY = 0.07f,
                freqX = 1f, freqY = 2f, phaseOff = 4.05f,
                radiusFactor = 0.52f, useMinDim = true, breatheAmp = 0.095f,
                color = cRose, coreAlpha = (if (dark) 0.25f else 0.18f) * k,
            ),
            // Floating emerald-mint harmony orb weaving through center
            GlowOrbSpec(
                baseX = 0.50f, baseY = 0.48f,
                ampX = 0.14f, ampY = 0.11f,
                freqX = 1f, freqY = 2f, phaseOff = 5.15f,
                radiusFactor = 0.42f, useMinDim = true, breatheAmp = 0.11f,
                color = cMint, coreAlpha = (if (dark) 0.22f else 0.15f) * k,
            ),
        )
    }

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                val minDim = minOf(w, h).coerceAtLeast(1f)
                val maxDim = maxOf(w, h).coerceAtLeast(1f)
                // Pre-bake GPU radial gradient shaders centered at Origin — zero allocations during animation!
                val cachedOrbs = orbs.map { spec ->
                    val r = ((if (spec.useMinDim) minDim else maxDim) * spec.radiusFactor).coerceAtLeast(24f)
                    val a = spec.coreAlpha.coerceIn(0f, 0.55f)
                    val brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to spec.color.copy(alpha = a),
                            0.24f to spec.color.copy(alpha = a * 0.78f),
                            0.52f to spec.color.copy(alpha = a * 0.34f),
                            0.78f to spec.color.copy(alpha = a * 0.09f),
                            1.00f to Color.Transparent,
                        ),
                        center = Offset.Zero,
                        radius = r,
                    )
                    // Subtle luminous inner specular ring so circles read as crisp, sculpted orbs rather than muddy haze
                    val innerR = r * 0.46f
                    val innerBrush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = if (dark) a * 0.22f else a * 0.30f),
                            0.55f to spec.color.copy(alpha = a * 0.22f),
                            1.00f to Color.Transparent,
                        ),
                        center = Offset.Zero,
                        radius = innerR,
                    )
                    Triple(spec, r to innerR, brush to innerBrush)
                }
                onDrawBehind {
                    if (w <= 0f || h <= 0f) return@onDrawBehind
                    val phase = phaseState?.value ?: 0.18f
                    val tau = (phase * 2.0 * PI).toFloat()
                    for ((spec, radii, brushes) in cachedOrbs) {
                        val (r, innerR) = radii
                        val (outerBrush, innerBrush) = brushes
                        val cx = w * (spec.baseX + spec.ampX * sin(tau * spec.freqX + spec.phaseOff))
                        val cy = h * (spec.baseY + spec.ampY * cos(tau * spec.freqY + spec.phaseOff * 0.72f))
                        val breathe = 1f + spec.breatheAmp * sin(tau * 2f + spec.phaseOff * 1.25f)
                        withTransform({
                            translate(left = cx, top = cy)
                            scale(scaleX = breathe, scaleY = breathe, pivot = Offset.Zero)
                        }) {
                            drawCircle(brush = outerBrush, radius = r, center = Offset.Zero)
                            drawCircle(brush = innerBrush, radius = innerR, center = Offset(-r * 0.08f, -r * 0.08f))
                        }
                    }
                }
            },
    )
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
