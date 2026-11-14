package io.github.ntvien.liquidglass

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Kind of glass. */
public enum class GlassKind {
    /** Real glass: samples what is behind it, lens refraction at the edge, subtle specular highlight, blur. */
    Liquid,

    /** Frosted glass: translucent fill + soft specular sheen, never samples the background. */
    Frost,
}

@Immutable
public class GlassStyle(
    public val kind: GlassKind,
    public val blur: Dp = 0.dp,
    public val lensHeight: Dp = 0.dp,
    public val lensAmount: Dp = 0.dp,
    public val chromaticAberration: Boolean = false,
    public val depthEffect: Boolean = false,
    public val vibrancy: Boolean = true,
    public val surface: Color = Color.Transparent,
    public val fallbackSurface: Color,
    public val highlightWidth: Dp = 0.dp,
    public val highlightAlpha: Float = 0.25f,
    public val fallbackSheen: Float = 0.45f,
    public val shadowRadius: Dp = 0.dp,
    public val shadowColor: Color = Color.Transparent,
    public val contentColor: Color,
) {
    public fun copy(
        kind: GlassKind = this.kind,
        blur: Dp = this.blur,
        lensHeight: Dp = this.lensHeight,
        lensAmount: Dp = this.lensAmount,
        chromaticAberration: Boolean = this.chromaticAberration,
        depthEffect: Boolean = this.depthEffect,
        vibrancy: Boolean = this.vibrancy,
        surface: Color = this.surface,
        fallbackSurface: Color = this.fallbackSurface,
        highlightWidth: Dp = this.highlightWidth,
        highlightAlpha: Float = this.highlightAlpha,
        fallbackSheen: Float = this.fallbackSheen,
        shadowRadius: Dp = this.shadowRadius,
        shadowColor: Color = this.shadowColor,
        contentColor: Color = this.contentColor,
    ): GlassStyle = GlassStyle(
        kind, blur, lensHeight, lensAmount, chromaticAberration, depthEffect, vibrancy, surface, fallbackSurface,
        highlightWidth, highlightAlpha, fallbackSheen, shadowRadius, shadowColor, contentColor,
    )

    private fun values(): List<Any> = listOf(
        kind, blur, lensHeight, lensAmount, chromaticAberration, depthEffect, vibrancy, surface, fallbackSurface,
        highlightWidth, highlightAlpha, fallbackSheen, shadowRadius, shadowColor, contentColor,
    )

    override fun equals(other: Any?): Boolean = other is GlassStyle && values() == other.values()

    override fun hashCode(): Int = values().hashCode()
}

/** Presets built from the theme colors, cached per theme. */
@Immutable
internal class GlassStyles(
    val chrome: GlassStyle,
    val card: GlassStyle,
    val control: GlassStyle,
    val accent: GlassStyle,
    val frost: GlassStyle,
) {
    companion object {
        fun from(c: GlassColors, rich: Boolean): GlassStyles = GlassStyles(
            chrome = GlassStyle(
                kind = GlassKind.Liquid,
                blur = 12.dp, lensHeight = 14.dp, lensAmount = 22.dp,
                chromaticAberration = false, depthEffect = true,
                surface = c.chromeFill,
                fallbackSurface = c.glassFillStrong,
                highlightWidth = 0.35.dp,
                highlightAlpha = 0.28f,
                shadowRadius = 14.dp, shadowColor = c.glassShadow,
                contentColor = c.content,
            ),
            card = GlassStyle(
                kind = GlassKind.Liquid,
                blur = 10.dp, lensHeight = 10.dp, lensAmount = 18.dp,
                surface = c.glassFill, fallbackSurface = c.glassFillStrong,
                highlightWidth = 0.3.dp,
                highlightAlpha = 0.22f,
                shadowRadius = if (rich) 10.dp else 6.dp, shadowColor = c.glassShadow,
                contentColor = c.content,
            ),
            control = GlassStyle(
                kind = GlassKind.Liquid,
                blur = 8.dp, lensHeight = 8.dp, lensAmount = 15.dp,
                surface = c.glassFill, fallbackSurface = c.glassFillStrong,
                highlightWidth = 0.dp,
                highlightAlpha = 0f,
                shadowRadius = if (rich) 6.dp else 0.dp, shadowColor = c.glassShadow,
                contentColor = c.content,
            ),
            accent = GlassStyle(
                kind = GlassKind.Liquid,
                blur = 8.dp, lensHeight = 10.dp, lensAmount = 18.dp,
                surface = c.accent.copy(alpha = 0.82f), fallbackSurface = c.accent.copy(alpha = 0.88f),
                highlightWidth = 0.3.dp,
                highlightAlpha = 0.30f, fallbackSheen = 0.32f,
                shadowRadius = 8.dp, shadowColor = c.accent.copy(alpha = 0.26f),
                contentColor = c.onAccent,
            ),
            frost = GlassStyle(
                kind = GlassKind.Frost,
                vibrancy = false,
                fallbackSurface = c.glassFillStrong,
                highlightWidth = 0.dp,
                highlightAlpha = 0f,
                fallbackSheen = 0.38f,
                contentColor = c.content,
            ),
        )
    }
}

public object GlassDefaults {
    @Composable
    @ReadOnlyComposable
    public fun chrome(): GlassStyle = LocalGlassStyles.current.chrome

    @Composable
    @ReadOnlyComposable
    public fun card(): GlassStyle = LocalGlassStyles.current.card

    @Composable
    @ReadOnlyComposable
    public fun control(): GlassStyle = LocalGlassStyles.current.control

    @Composable
    @ReadOnlyComposable
    public fun accent(): GlassStyle = LocalGlassStyles.current.accent

    @Composable
    @ReadOnlyComposable
    public fun frost(): GlassStyle = LocalGlassStyles.current.frost

    /** Tinted translucent Liquid Glass preset for colorful interactive buttons. */
    @Composable
    @ReadOnlyComposable
    public fun tinted(color: Color, onColor: Color = Color.White): GlassStyle =
        LocalGlassStyles.current.accent.copy(
            kind = GlassKind.Liquid,
            surface = color.copy(alpha = 0.78f),
            fallbackSurface = color.copy(alpha = 0.84f),
            shadowColor = color.copy(alpha = 0.28f),
            contentColor = onColor,
        )

    public val CardShape: Shape = RoundedCornerShape(24.dp)
    public val ControlShape: Shape = RoundedCornerShape(16.dp)
    public val PillShape: Shape = RoundedCornerShape(percent = 50)
    public val ContinuousCardShape: Shape = RoundedCornerShape(24.dp)
    public val ContinuousPillShape: Shape = RoundedCornerShape(percent = 50)
}
