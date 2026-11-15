package io.github.ntvien.liquidglass

import android.app.ActivityManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * How much glass the device can draw. [LiquidGlassTheme] decides it once; every surface and component follows it.
 */
public enum class GlassQuality {
    /** Android 13+: blur, lens refraction (AGSL), chromatic aberration, highlight and shadow. */
    Full,

    /** Android 12: blur, highlight and shadow, no lens refraction (no AGSL). */
    BlurOnly,

    /**
     * Android 11 and lower, low-RAM devices, Classic mode, or throttled conditions:
     * flat/frosted fallback without runtime shaders or backdrop capture layers.
     */
    Flat,
}

/** Manual or automatic glass tier selection from Settings. */
public enum class GlassQualityOverride(public val label: String) {
    Auto("Automatic"),
    Full("Full glass"),
    BlurOnly("Blur only"),
    Flat("Flat"),
}

internal val LocalGlassQuality = staticCompositionLocalOf { GlassQuality.Flat }
internal val LocalGlassQualityOverride = staticCompositionLocalOf { GlassQualityOverride.Auto }

/** True inside a parent glass surface so child components never render glass-inside-glass. */
public val LocalInsideGlass = staticCompositionLocalOf { false }

/** Whether glass draws shadows. Off in Battery Saver or thermal throttling. */
internal val LocalGlassShadows = staticCompositionLocalOf { true }

/** Blur radius multiplier controlled by Performance mode (0f..1.25f). */
internal val LocalGlassBlurScale = staticCompositionLocalOf { 1f }

/** Refraction/highlight intensity multiplier controlled by Performance mode (0f..1.25f). */
internal val LocalGlassEffectIntensity = staticCompositionLocalOf { 1f }

/** Whether floating glass samples scrolling content. Kept false per window-single-backdrop architecture. */
internal val LocalGlassOverlaySamplesContent = staticCompositionLocalOf { false }

internal fun isRuntimeShaderSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

internal fun isRenderEffectSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
internal fun rememberDeviceGlassQuality(
    reduceEffects: Boolean,
    throttled: Boolean,
    manualOverride: GlassQualityOverride = GlassQualityOverride.Auto,
): GlassQuality {
    val context = LocalContext.current
    return remember(context, reduceEffects, throttled, manualOverride) {
        if (reduceEffects) return@remember GlassQuality.Flat
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val hardwareCeiling = when {
            activityManager?.isLowRamDevice == true -> GlassQuality.Flat
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> GlassQuality.Full
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> GlassQuality.BlurOnly
            else -> GlassQuality.Flat
        }
        val effective = when (manualOverride) {
            GlassQualityOverride.Auto -> {
                if (!throttled) hardwareCeiling
                else when (hardwareCeiling) {
                    GlassQuality.Full -> GlassQuality.BlurOnly
                    GlassQuality.BlurOnly -> GlassQuality.Flat
                    GlassQuality.Flat -> GlassQuality.Flat
                }
            }
            GlassQualityOverride.Full -> hardwareCeiling
            GlassQualityOverride.BlurOnly -> when (hardwareCeiling) {
                GlassQuality.Full -> GlassQuality.BlurOnly
                GlassQuality.BlurOnly -> GlassQuality.BlurOnly
                GlassQuality.Flat -> GlassQuality.Flat
            }
            GlassQualityOverride.Flat -> GlassQuality.Flat
        }
        effective
    }
}

/** High-end device by Google's definition: Media Performance Class >= Android 13. */
internal fun isHighEndDevice(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.VERSION.MEDIA_PERFORMANCE_CLASS >= Build.VERSION_CODES.TIRAMISU
