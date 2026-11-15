package io.github.ntvien.liquidglass

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/**
 * Draws borderless, readable Liquid Glass behind this element using the upstream backdrop engine.
 * Automatically degrades to a clean flat/frosted fill when inside another glass surface (no glass-inside-glass)
 * or when quality is [GlassQuality.Flat].
 */
@Composable
public fun Modifier.liquidGlass(
    style: GlassStyle = GlassDefaults.card(),
    shape: Shape = GlassDefaults.cardShape(),
    interaction: GlassInteraction? = null,
): Modifier {
    val quality = LocalGlassQuality.current
    val backdrops = LocalGlassBackdrops.current
    val insideGlass = LocalInsideGlass.current
    val shadowsEnabled = LocalGlassShadows.current
    val colors = LocalGlassColors.current
    val highlightColor = colors.glassHighlight

    val blurScale = LocalGlassBlurScale.current
    val intensity = LocalGlassEffectIntensity.current
    val useLiquid = !insideGlass && style.kind == GlassKind.Liquid && quality != GlassQuality.Flat && backdrops != null && intensity > 0.05f
    if (!useLiquid) {
        val fill = if (insideGlass) {
            style.fallbackSurface.copy(alpha = (style.fallbackSurface.alpha * 0.72f).coerceIn(0.20f, 0.90f))
        } else {
            style.fallbackSurface
        }
        return this
            .then(if (interaction != null) Modifier.graphicsLayer(interaction.layerBlock) else Modifier)
            .then(
                if (!insideGlass && style.shadowRadius > 0.dp && shadowsEnabled) {
                    Modifier.shadow(style.shadowRadius / 3, shape, clip = false, ambientColor = style.shadowColor, spotColor = style.shadowColor)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(fill)
            .then(
                if (!insideGlass && style.fallbackSheen > 0f && intensity > 0.1f) {
                    Modifier.frostSheen(highlightColor.copy(alpha = (highlightColor.alpha * style.fallbackSheen * 0.40f * intensity).coerceIn(0f, 0.36f)))
                } else {
                    Modifier
                }
            )
            .then(interaction?.highlightModifier ?: Modifier)
            .then(interaction?.gestureModifier ?: Modifier)
    }

    val backdrop = backdrops.background
    val allowLens = quality == GlassQuality.Full && intensity >= 0.4f
    val blurRadius = style.blur * blurScale
    val useLens = allowLens && style.lensHeight > 0.dp && style.lensAmount > 0.dp
    val scaledHighlight = (style.highlightAlpha * intensity * 0.28f).coerceIn(0f, 0.35f)
    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (style.vibrancy && intensity >= 0.5f) vibrancy()
                if (blurRadius > 0.dp) blur(blurRadius.toPx())
                if (useLens) {
                    lens(
                        refractionHeight = (style.lensHeight * intensity).toPx(),
                        refractionAmount = (style.lensAmount * intensity).toPx(),
                        depthEffect = style.depthEffect && intensity >= 0.8f,
                        chromaticAberration = style.chromaticAberration && intensity >= 0.9f,
                    )
                }
            },
            highlight = if (style.highlightWidth > 0.dp && scaledHighlight > 0f) {
                {
                    Highlight.Ambient.copy(
                        width = style.highlightWidth.coerceAtMost(0.45.dp),
                        alpha = scaledHighlight,
                    )
                }
            } else {
                null
            },
            shadow = if (style.shadowRadius > 0.dp && shadowsEnabled) {
                { Shadow(radius = style.shadowRadius, color = style.shadowColor) }
            } else {
                null
            },
            layerBlock = interaction?.layerBlock,
            onDrawSurface = {
                if (style.surface.alpha > 0f) drawRect(style.surface)
            },
        )
        .then(interaction?.highlightModifier ?: Modifier)
        .then(interaction?.gestureModifier ?: Modifier)
}

/** Soft top-left specular sheen without any harsh border outline. Cached per size. */
private fun Modifier.frostSheen(highlight: Color): Modifier = drawWithCache {
    val brush = Brush.radialGradient(
        colors = listOf(highlight, Color.Transparent),
        center = Offset(size.width * 0.18f, 0f),
        radius = size.maxDimension * 0.85f,
    )
    onDrawBehind { drawRect(brush) }
}
