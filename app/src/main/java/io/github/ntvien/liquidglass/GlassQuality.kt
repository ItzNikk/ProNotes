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
     * Android 11 and lower, low-RAM devices, Battery Saver, or `reduceEffects = true`: frosted fallback
     * (translucent fill, sheen and stroke) that never samples the background. Cheap and always readable.
     */
    Off,
}

internal val LocalGlassQuality = staticCompositionLocalOf { GlassQuality.Off }

@Composable
internal fun rememberDeviceGlassQuality(reduceEffects: Boolean): GlassQuality {
    val context = LocalContext.current
    return remember(context, reduceEffects) {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        when {
            reduceEffects -> GlassQuality.Off
            activityManager?.isLowRamDevice == true -> GlassQuality.Off
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> GlassQuality.Full
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> GlassQuality.BlurOnly
            else -> GlassQuality.Off
        }
    }
}

/** High-end device by Google's definition: Media Performance Class ≥ Android 13. */
internal fun isHighEndDevice(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.VERSION.MEDIA_PERFORMANCE_CLASS >= Build.VERSION_CODES.TIRAMISU

/** Whether floating glass (tab bar, menus) samples the scrolling content as well as the background. The most expensive part of the glass. */
internal val LocalGlassOverlaySamplesContent = staticCompositionLocalOf { false }

/** Whether glass draws shadows. Off in Battery Saver. */
internal val LocalGlassShadows = staticCompositionLocalOf { true }
