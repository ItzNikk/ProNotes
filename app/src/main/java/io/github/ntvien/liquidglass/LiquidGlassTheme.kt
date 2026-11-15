package io.github.ntvien.liquidglass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Colors used by glass surfaces and the components of this library. Start from [light] or [dark] and [copy] the
 * values your brand needs; everything else follows.
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
            contentMuted = Color(0xFF53505C),
            accent = Color(0xFF3B57D6),
            accentSoft = Color(0x293B57D6),
            onAccent = Color(0xFFFFFFFF),
            selectedFill = Color(0xFF1D1B24),
            selectedContent = Color(0xFFFFFFFF),
            badge = Color(0xFFD93025),
            glassFill = Color(0x88FFFFFF),
            glassFillStrong = Color(0xCCFFFFFF),
            glassFillOpaque = Color(0xF5FFFFFF),
            glassStroke = Color(0xA6FFFFFF),
            glassHighlight = Color(0xF2FFFFFF),
            glassShadow = Color(0x2E000000),
            raisedFill = Color(0xF7FFFFFF),
            chromeFill = Color(0x99FFFFFF),
            tabSelectedFill = Color(0x293B57D6),
            tabSelectedContent = Color(0xFF3B57D6),
        )

        /** Neutral colors for dark backgrounds. */
        public fun dark(): GlassColors = GlassColors(
            content = Color(0xFFF2EFF8),
            contentMuted = Color(0xFFC0BBCB),
            accent = Color(0xFF8EA2FF),
            accentSoft = Color(0x338EA2FF),
            onAccent = Color(0xFF121119),
            selectedFill = Color(0xFFF2EFF8),
            selectedContent = Color(0xFF121119),
            badge = Color(0xFFFF8A7A),
            glassFill = Color(0x7A1E1D28),
            glassFillStrong = Color(0xAD262533),
            glassFillOpaque = Color(0xF5232230),
            glassStroke = Color(0x47FFFFFF),
            glassHighlight = Color(0x99FFFFFF),
            glassShadow = Color(0x66000000),
            raisedFill = Color(0xE62E2D3C),
            chromeFill = Color(0x99121119),
            tabSelectedFill = Color(0x40F2EFF8),
            tabSelectedContent = Color(0xFFF2EFF8),
        )
    }
}

/** How rich the glass looks. One switch for the whole app; screens never change. */
public enum class GlassLook {
    Auto,
    Rich,
    Optimized,
}

internal val LocalGlassColors = staticCompositionLocalOf { GlassColors.light() }
internal val LocalGlassStyles = staticCompositionLocalOf { GlassStyles.from(GlassColors.light(), rich = false) }
internal val LocalGlassLook = staticCompositionLocalOf { GlassLook.Optimized }

/**
 * Root of every screen that uses liquid glass. Wrap your app content once (inside your own theme).
 */
@Composable
public fun LiquidGlassTheme(
    colors: GlassColors = if (isSystemInDarkTheme()) GlassColors.dark() else GlassColors.light(),
    look: GlassLook = GlassLook.Auto,
    reduceEffects: Boolean = false,
    reduceMotion: Boolean = false,
    qualityOverride: GlassQualityOverride = GlassQualityOverride.Auto,
    cornerRoundness: Dp = 24.dp,
    blurScale: Float = 1f,
    effectIntensity: Float = 1f,
    overlaySamplesContent: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val powerSave = rememberPowerSaveMode()
    val thermalThrottled = rememberThermalThrottled()
    val systemRemoveAnimations = rememberSystemAnimationsRemoved()
    val throttled = powerSave || thermalThrottled || systemRemoveAnimations || reduceMotion
    val quality = rememberDeviceGlassQuality(
        reduceEffects = reduceEffects,
        throttled = throttled,
        manualOverride = qualityOverride,
    )
    val resolved = remember(look) {
        if (look == GlassLook.Auto) (if (isHighEndDevice()) GlassLook.Rich else GlassLook.Optimized) else look
    }
    val safeBlurScale = blurScale.coerceIn(0f, 1.25f)
    val safeIntensity = effectIntensity.coerceIn(0f, 1.25f)
    val rich = resolved == GlassLook.Rich && quality == GlassQuality.Full && safeIntensity >= 0.85f
    // Single shared window backdrop captures only static/cached background to guarantee 0 ink latency.
    @Suppress("UNUSED_VARIABLE")
    val unusedOverlaySamples = overlaySamplesContent
    val samplesContent = false
    val styles = remember(colors, rich) { GlassStyles.from(colors, rich) }
    CompositionLocalProvider(
        LocalGlassColors provides colors,
        LocalGlassStyles provides styles,
        LocalGlassLook provides resolved,
        LocalGlassQuality provides quality,
        LocalGlassQualityOverride provides qualityOverride,
        LocalGlassCornerRoundness provides cornerRoundness.coerceIn(8.dp, 36.dp),
        LocalGlassBlurScale provides safeBlurScale,
        LocalGlassEffectIntensity provides safeIntensity,
        LocalGlassOverlaySamplesContent provides samplesContent,
        LocalGlassShadows provides (!powerSave && !thermalThrottled && safeIntensity > 0.2f),
        LocalInsideGlass provides false,
        content = content,
    )
}

/** Reads the current glass configuration, e.g. `LiquidGlassTheme.colors.accent`. */
public object LiquidGlassTheme {
    public val colors: GlassColors
        @Composable @ReadOnlyComposable get() = LocalGlassColors.current

    public val quality: GlassQuality
        @Composable @ReadOnlyComposable get() = LocalGlassQuality.current

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
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return saving
}

/** Monitors PowerManager thermal status on API 29+ (drops glass tier when >= MODERATE). */
@Composable
private fun rememberThermalThrottled(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val context = LocalContext.current
    val power = remember(context) { context.getSystemService(PowerManager::class.java) } ?: return false
    var throttled by remember(power) {
        mutableStateOf(power.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE)
    }
    DisposableEffect(power) {
        val listener = PowerManager.OnThermalStatusChangedListener { status ->
            throttled = status >= PowerManager.THERMAL_STATUS_MODERATE
        }
        power.addThermalStatusListener(listener)
        onDispose { power.removeThermalStatusListener(listener) }
    }
    return throttled
}

/** Monitors Android Accessibility "Remove animations" (`animator_duration_scale == 0f`). */
@Composable
private fun rememberSystemAnimationsRemoved(): Boolean {
    val context = LocalContext.current
    fun readRemoved(): Boolean = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)

    var removed by remember(context) { mutableStateOf(readRemoved()) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                removed = readRemoved()
            }
        }
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        runCatching { context.contentResolver.registerContentObserver(uri, false, observer) }
        onDispose { runCatching { context.contentResolver.unregisterContentObserver(observer) } }
    }
    return removed
}
