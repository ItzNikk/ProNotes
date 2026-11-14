package io.github.ntvien.liquidglass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Colors used by glass surfaces and the components of this library. Start from [light] or [dark] and [copy] the
 * values your brand needs; everything else follows.
 *
 * @property content text and icons on glass.
 * @property contentMuted unselected tabs, secondary text.
 * @property accent selected tab (stacked labels), accent glass.
 * @property accentSoft fill behind the selected stacked tab.
 * @property onAccent content on [accent].
 * @property selectedFill strong pill for the selected tab (selected-only labels) and segmented control.
 * @property selectedContent content on [selectedFill].
 * @property badge tab badge dot.
 * @property glassFill tint over real glass.
 * @property glassFillStrong fallback fill for cards and controls when real glass is not drawn.
 * @property glassFillOpaque fallback fill for floating chrome (tab bar) when real glass is not drawn.
 * @property glassStroke 1 dp stroke of the frosted fallback.
 * @property glassHighlight sheen of the frosted fallback.
 * @property glassShadow soft shadow under floating glass.
 * @property raisedFill light glass raised above a track: the selected segment of [io.github.ntvien.liquidglass.component.LiquidSegmentedControl].
 * @property chromeFill tint of floating glass (tab bar): light enough to show what it refracts, strong enough for labels.
 * @property tabSelectedFill droplet behind the selected tab of the stacked tab bar.
 * @property tabSelectedContent icon and label of the selected tab of the stacked tab bar.
 */
@Immutable
public class GlassColors(
    public val content: Color,
    public val contentMuted: Color,
    public val accent: Color,
    public val accentSoft: Color,
    public val onAccent: Color,
    public val selectedFill: Color,
    public val selectedContent: Color,
    public val badge: Color,
    public val glassFill: Color,
    public val glassFillStrong: Color,
    public val glassFillOpaque: Color,
    public val glassStroke: Color,
    public val glassHighlight: Color,
    public val glassShadow: Color,
    public val raisedFill: Color,
    public val chromeFill: Color,
    public val tabSelectedFill: Color,
    public val tabSelectedContent: Color,
) {
    /** Returns a copy with the given colors replaced. */
    public fun copy(
        content: Color = this.content,
        contentMuted: Color = this.contentMuted,
        accent: Color = this.accent,
        accentSoft: Color = this.accentSoft,
        onAccent: Color = this.onAccent,
        selectedFill: Color = this.selectedFill,
        selectedContent: Color = this.selectedContent,
        badge: Color = this.badge,
        glassFill: Color = this.glassFill,
        glassFillStrong: Color = this.glassFillStrong,
        glassFillOpaque: Color = this.glassFillOpaque,
        glassStroke: Color = this.glassStroke,
        glassHighlight: Color = this.glassHighlight,
        glassShadow: Color = this.glassShadow,
        raisedFill: Color = this.raisedFill,
        chromeFill: Color = this.chromeFill,
        tabSelectedFill: Color = this.tabSelectedFill,
        tabSelectedContent: Color = this.tabSelectedContent,
    ): GlassColors = GlassColors(
        content, contentMuted, accent, accentSoft, onAccent, selectedFill, selectedContent, badge,
        glassFill, glassFillStrong, glassFillOpaque, glassStroke, glassHighlight, glassShadow, raisedFill,
        chromeFill, tabSelectedFill, tabSelectedContent,
    )

    private fun values(): List<Color> = listOf(
        content, contentMuted, accent, accentSoft, onAccent, selectedFill, selectedContent, badge,
        glassFill, glassFillStrong, glassFillOpaque, glassStroke, glassHighlight, glassShadow, raisedFill,
        chromeFill, tabSelectedFill, tabSelectedContent,
    )

    override fun equals(other: Any?): Boolean = other is GlassColors && values() == other.values()

    override fun hashCode(): Int = values().hashCode()

    public companion object {
        /** Neutral colors for light backgrounds. */
        public fun light(): GlassColors = GlassColors(
            content = Color(0xFF1D1B24),
            contentMuted = Color(0xFF66626F),
            accent = Color(0xFF3B57D6),
            accentSoft = Color(0x293B57D6),
            onAccent = Color(0xFFFFFFFF),
            selectedFill = Color(0xFF1D1B24),
            selectedContent = Color(0xFFFFFFFF),
            badge = Color(0xFFD93025),
            glassFill = Color(0x4DFFFFFF),
            glassFillStrong = Color(0xBFFFFFFF),
            glassFillOpaque = Color(0xF2FFFFFF),
            // Bright rim and edge light, as on iOS 26.
            glassStroke = Color(0xA6FFFFFF),
            glassHighlight = Color(0xF2FFFFFF),
            glassShadow = Color(0x2E000000),
            raisedFill = Color(0xF7FFFFFF),
            chromeFill = Color(0x33FFFFFF),
            tabSelectedFill = Color(0x293B57D6),
            tabSelectedContent = Color(0xFF3B57D6),
        )

        /** Neutral colors for dark backgrounds. */
        public fun dark(): GlassColors = GlassColors(
            content = Color(0xFFF2EFF8),
            contentMuted = Color(0xFFA6A1B3),
            accent = Color(0xFF8EA2FF),
            accentSoft = Color(0x338EA2FF),
            onAccent = Color(0xFF121119),
            selectedFill = Color(0xFFF2EFF8),
            selectedContent = Color(0xFF121119),
            badge = Color(0xFFFF8A7A),
            glassFill = Color(0x1FFFFFFF),
            glassFillStrong = Color(0x52262533),
            glassFillOpaque = Color(0xF2232230),
            glassStroke = Color(0x47FFFFFF),
            glassHighlight = Color(0x99FFFFFF),
            glassShadow = Color(0x66000000),
            raisedFill = Color(0x40FFFFFF),
            // Dark glass, as on iOS: 35 % of the dark background keeps light labels readable over busy content.
            chromeFill = Color(0x59121119),
            // The accent is too dark on a dark background: the selected tab is a light droplet with light content.
            tabSelectedFill = Color(0x40F2EFF8),
            tabSelectedContent = Color(0xFFF2EFF8),
        )
    }
}

/** How rich the glass looks. One switch for the whole app; screens never change. */
public enum class GlassLook {
    /** [Rich] on high-end devices (Media Performance Class ≥ 13), [Optimized] everywhere else. */
    Auto,

    /**
     * Lens on every block, shadows on every block, accent buttons are real glass, the tab bar refracts the scrolling
     * content (also on Android 12, blurred). Prettiest, most expensive.
     */
    Rich,

    /**
     * Tuned from measurements on mid-range devices: shadows only on floating chrome and accent buttons, accent buttons
     * are solid with a sheen. The tab bar refracts only the background (see `overlaySamplesContent` of
     * [LiquidGlassTheme]).
     */
    Optimized,
}

internal val LocalGlassColors = staticCompositionLocalOf { GlassColors.light() }
internal val LocalGlassStyles = staticCompositionLocalOf { GlassStyles.from(GlassColors.light(), rich = false) }
internal val LocalGlassLook = staticCompositionLocalOf { GlassLook.Optimized }

/**
 * Root of every screen that uses liquid glass. Wrap your app content once (inside your own theme).
 *
 * Measures the device ONCE: glass quality ([GlassQuality]), high-end or not ([GlassLook.Auto]) and Battery Saver
 * (follows changes live). Without this theme every surface falls back to the frosted look.
 *
 * @param colors colors of glass and components, see [GlassColors].
 * @param look [GlassLook.Auto] by default.
 * @param reduceEffects wire this to a "Reduce effects" switch in your settings: `true` turns glass into the frosted
 *   fallback ([GlassQuality.Off]).
 * @param overlaySamplesContent whether floating glass (tab bar, menus) refracts the scrolling content, not only the
 *   background. `null` (default): yes with [GlassLook.Rich] (also on Android 12, blurred) and on high-end devices
 *   (Media Performance Class ≥ 13) with [GlassQuality.Full]; elsewhere only the background. It is the most expensive
 *   part of the glass (measured numbers in the README, "Performance rules"): pass `true` to turn it on everywhere only
 *   after measuring your screens, `false` to turn it off everywhere. Ignored (always off) with [GlassQuality.Off].
 */
@Composable
public fun LiquidGlassTheme(
    colors: GlassColors = if (isSystemInDarkTheme()) GlassColors.dark() else GlassColors.light(),
    look: GlassLook = GlassLook.Auto,
    reduceEffects: Boolean = false,
    overlaySamplesContent: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val powerSave = rememberPowerSaveMode()
    val quality = rememberDeviceGlassQuality(reduceEffects = reduceEffects || powerSave)
    val resolved = remember(look) {
        if (look == GlassLook.Auto) (if (isHighEndDevice()) GlassLook.Rich else GlassLook.Optimized) else look
    }
    val rich = resolved == GlassLook.Rich
    // Refracting the content is the most expensive part of the glass: by default only the rich look and high-end devices.
    val samplesContent = when {
        quality == GlassQuality.Off -> false
        overlaySamplesContent != null -> overlaySamplesContent
        rich -> true
        else -> quality == GlassQuality.Full && isHighEndDevice()
    }
    val styles = remember(colors, rich) { GlassStyles.from(colors, rich) }
    CompositionLocalProvider(
        LocalGlassColors provides colors,
        LocalGlassStyles provides styles,
        LocalGlassLook provides resolved,
        LocalGlassQuality provides quality,
        LocalGlassOverlaySamplesContent provides samplesContent,
        LocalGlassShadows provides !powerSave,
        content = content,
    )
}

/** Reads the current glass configuration, e.g. `LiquidGlassTheme.colors.accent`. */
public object LiquidGlassTheme {
    /** Colors provided by the nearest [LiquidGlassTheme]. */
    public val colors: GlassColors
        @Composable @ReadOnlyComposable get() = LocalGlassColors.current

    /** Glass quality chosen for this device. */
    public val quality: GlassQuality
        @Composable @ReadOnlyComposable get() = LocalGlassQuality.current

    /** Resolved look: [GlassLook.Rich] or [GlassLook.Optimized], never [GlassLook.Auto]. */
    public val look: GlassLook
        @Composable @ReadOnlyComposable get() = LocalGlassLook.current
}

/** Follows Battery Saver and updates as soon as the user toggles it. */
@Composable
private fun rememberPowerSaveMode(): Boolean {
    val context = LocalContext.current
    val power = remember(context) { context.getSystemService(PowerManager::class.java) }
    var saving by remember(power) { mutableStateOf(power?.isPowerSaveMode == true) }
    DisposableEffect(context, power) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                saving = power?.isPowerSaveMode == true
            }
        }
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return saving
}
