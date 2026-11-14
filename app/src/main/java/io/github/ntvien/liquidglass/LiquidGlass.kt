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
import io.github.ntvien.liquidglass.internal.backdrop.drawBackdrop
import io.github.ntvien.liquidglass.internal.backdrop.effects.VibrancySaturation
import io.github.ntvien.liquidglass.internal.backdrop.effects.blur
import io.github.ntvien.liquidglass.internal.backdrop.effects.lens
import io.github.ntvien.liquidglass.internal.backdrop.effects.vibrancy
import io.github.ntvien.liquidglass.internal.backdrop.highlight.Highlight
import io.github.ntvien.liquidglass.internal.backdrop.shadow.Shadow

/**
 * Draws borderless, high-transparency Liquid Glass behind this element.
 */
@Composable
public fun Modifier.liquidGlass(
    style: GlassStyle = GlassDefaults.card(),
    shape: Shape = GlassDefaults.CardShape,
    interaction: GlassInteraction? = null,
): Modifier {
    val quality = LocalGlassQuality.current
    val backdrops = LocalGlassBackdrops.current
    val layer = LocalGlassLayer.current
    val shadowsEnabled = LocalGlassShadows.current
    val colors = LocalGlassColors.current
    val highlightColor = colors.glassHighlight

    val useLiquid = style.kind == GlassKind.Liquid && quality != GlassQuality.Off && backdrops != null
    if (!useLiquid) {
        return this
            .then(if (interaction != null) Modifier.graphicsLayer(interaction.layerBlock) else Modifier)
            .then(
                if (style.shadowRadius > 0.dp && shadowsEnabled) {
                    Modifier.shadow(style.shadowRadius / 3, shape, clip = false, ambientColor = style.shadowColor, spotColor = style.shadowColor)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(style.fallbackSurface)
            .frostSheen(highlightColor.copy(alpha = (highlightColor.alpha * style.fallbackSheen * 0.45f).coerceIn(0f, 0.42f)))
            .then(interaction?.highlightModifier ?: Modifier)
            .then(interaction?.gestureModifier ?: Modifier)
    }

    val backdrop = if (layer == GlassLayer.Overlay) backdrops.overlay else backdrops.background
    val allowLens = quality == GlassQuality.Full
    val blurRadius = style.blur
    val useLens = allowLens && style.lensHeight > 0.dp && style.lensAmount > 0.dp
    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (style.vibrancy && !useLens) vibrancy()
                if (blurRadius > 0.dp) blur(blurRadius.toPx())
                if (useLens) {
                    lens(
                        refractionHeight = style.lensHeight.toPx(),
                        refractionAmount = style.lensAmount.toPx(),
                        depthEffect = style.depthEffect,
                        chromaticAberration = style.chromaticAberration,
                        saturation = if (style.vibrancy) VibrancySaturation else 1f,
                    )
                }
            },
            highlight = if (style.highlightWidth > 0.dp && style.highlightAlpha > 0f) {
                {
                    Highlight.Ambient.copy(
                        width = style.highlightWidth.coerceAtMost(0.45.dp),
                        alpha = (style.highlightAlpha * 0.28f).coerceIn(0f, 0.35f),
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
