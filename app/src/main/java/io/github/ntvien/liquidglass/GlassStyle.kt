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
    /** Real glass: samples what is behind it, lens refraction at the edge, highlight, blur when sampling content. */
    Liquid,

    /** Frosted glass: translucent fill + sheen + stroke, never samples the background. Use it for lists and chips. */
    Frost,
}

/**
 * One glass recipe. Prefer the presets in [GlassDefaults] and [copy] them when you really need to; every preset
 * already follows the device tiers ([GlassQuality]).
 *
 * @property kind [GlassKind.Liquid] or [GlassKind.Frost].
 * @property blur blur radius (Android 12+). Applied only when the glass samples scrolling content: blurring a smooth
 *   background costs GPU time and changes nothing.
 * @property lensHeight width of the refracting band at the edge (Android 13+). 0 disables the lens.
 * @property lensAmount how strongly the edge bends light.
 * @property chromaticAberration rainbow fringe at the lens edge; slightly more expensive, keep it for chrome.
 * @property depthEffect thick-glass light/dark edge.
 * @property vibrancy boosts the saturation of what is behind, like iOS.
 * @property surface tint drawn over real glass.
 * @property fallbackSurface fill used when real glass is not drawn ([GlassKind.Frost], [GlassQuality.Off]).
 * @property highlightWidth width of the bright rim.
 * @property highlightAlpha opacity of the bright rim.
 * @property fallbackSheen strength (0..1) of the fake sheen in the frosted fallback.
 * @property shadowRadius outer shadow; 0 disables it. Shadows cost ~10% janky frames on mid-range devices.
 * @property shadowColor shadow color.
 * @property contentColor default color of text and icons on this glass.
 */
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
    public val highlightWidth: Dp = 0.5.dp,
    public val highlightAlpha: Float = 1f,
    public val fallbackSheen: Float = 1f,
    public val shadowRadius: Dp = 0.dp,
    public val shadowColor: Color = Color.Transparent,
    public val contentColor: Color,
) {
    /** Returns a copy with the given values replaced. */
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
                blur = 10.dp, lensHeight = 16.dp, lensAmount = 24.dp,
                chromaticAberration = false, depthEffect = true,
                surface = c.chromeFill,
                fallbackSurface = c.glassFillOpaque,
                highlightWidth = 1.dp,
                shadowRadius = 16.dp, shadowColor = c.glassShadow,
                contentColor = c.content,
            ),
            card = GlassStyle(
                kind = GlassKind.Liquid,
                blur = 8.dp, lensHeight = 10.dp, lensAmount = 18.dp,
                surface = c.glassFill, fallbackSurface = c.glassFillStrong,
                highlightWidth = 0.8.dp,
                shadowRadius = if (rich) 12.dp else 0.dp, shadowColor = c.glassShadow,
                contentColor = c.content,
            ),
            control = GlassStyle(
                kind = GlassKind.Liquid,
                blur = 6.dp, lensHeight = 8.dp, lensAmount = 16.dp,
                surface = c.glassFillStrong, fallbackSurface = c.glassFillStrong,
                highlightWidth = 0.8.dp,
                shadowRadius = if (rich) 8.dp else 0.dp, shadowColor = c.glassShadow,
                contentColor = c.content,
            ),
            accent = GlassStyle(
                kind = GlassKind.Frost,
                blur = 4.dp, lensHeight = 8.dp, lensAmount = 16.dp,
                surface = c.accent.copy(alpha = 0.92f), fallbackSurface = c.accent,
                highlightWidth = 0.8.dp,
                highlightAlpha = 0.85f, fallbackSheen = 0.22f,
                shadowRadius = 10.dp, shadowColor = c.accent.copy(alpha = 0.32f),
                contentColor = c.onAccent,
            ),
            frost = GlassStyle(
                kind = GlassKind.Frost,
                vibrancy = false,
                fallbackSurface = c.glassFillStrong,
                highlightWidth = 0.8.dp,
                fallbackSheen = 0.35f,
                contentColor = c.content,
            ),
        )
    }
}

/**
 * Glass presets and shapes. Presets come from the nearest [LiquidGlassTheme] and follow its colors and look.
 *
 * | Preset    | Kind   | Use for                                   | Avoid for            |
 * |-----------|--------|-------------------------------------------|----------------------|
 * | [chrome]  | Liquid | tab bar, floating header, floating button | cards in a list      |
 * | [card]    | Liquid | hero and large cards (a few per screen)   | every list row       |
 * | [control] | Liquid | glass buttons, round buttons              | repeated small items |
 * | [accent]  | Frost* | primary button in the accent color        |                      |
 * | [frost]   | Frost  | lists, chips, segmented track, fields     | the tab bar          |
 *
 * *Liquid with [GlassLook.Rich].
 */
public object GlassDefaults {
    /** Floating chrome: strong lens, chromatic aberration, shadow. */
    @Composable
    @ReadOnlyComposable
    public fun chrome(): GlassStyle = LocalGlassStyles.current.chrome

    /** Large content card: medium lens. */
    @Composable
    @ReadOnlyComposable
    public fun card(): GlassStyle = LocalGlassStyles.current.card

    /** Glass button: small lens. */
    @Composable
    @ReadOnlyComposable
    public fun control(): GlassStyle = LocalGlassStyles.current.control

    /** Primary button in the accent color. */
    @Composable
    @ReadOnlyComposable
    public fun accent(): GlassStyle = LocalGlassStyles.current.accent

    /** Frosted, never samples the background: lists, chips, fields. */
    @Composable
    @ReadOnlyComposable
    public fun frost(): GlassStyle = LocalGlassStyles.current.frost

    /** 24 dp rounded corners for cards in lists and repeated blocks. */
    public val CardShape: Shape = RoundedCornerShape(24.dp)

    /** 16 dp rounded corners for controls. */
    public val ControlShape: Shape = RoundedCornerShape(16.dp)

    /** Fully rounded ends (segmented control, pills, chips, circles). */
    public val PillShape: Shape = RoundedCornerShape(percent = 50)

    /**
     * iOS-style continuous corners (the curve eases into the straight edge), 24 dp: the default of
     * [io.github.ntvien.liquidglass.component.LiquidSurface].
     * Drawn with a path, so it costs more than [CardShape]: use it for a few large blocks per screen, never for list
     * rows, chips or other repeated items.
     */
    public val ContinuousCardShape: Shape = RoundedCornerShape(24.dp)

    /** iOS-style continuous capsule: the tab bar and its droplet. Same cost rule as [ContinuousCardShape]. */
    public val ContinuousPillShape: Shape = RoundedCornerShape(percent = 50)
}
