package com.pronotes.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pronotes.app.data.AccentColor
import com.pronotes.app.data.AppFont
import com.pronotes.app.data.MotionStyle
import com.pronotes.app.data.PerformanceMode
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.ThemeMode
import com.pronotes.app.data.UiColorScheme
import kotlin.math.max
import kotlin.math.min

val Accent = Color(0xFF1A84FF)

val LocalTitleGradient = compositionLocalOf<List<Color>?> { null }
val LocalMotionStyle = compositionLocalOf { MotionStyle.SPRING }
val LocalCornerRoundness = compositionLocalOf<Dp> { 20.dp }
val LocalPerformanceMode = compositionLocalOf { PerformanceMode.BALANCED }
val LocalUiColorScheme = compositionLocalOf { UiColorScheme.SLATE_STUDIO }
val LocalLiquidGlass = compositionLocalOf { false }
val LocalClassicAppearance = compositionLocalOf { false }
val LocalReduceEffects = compositionLocalOf { false }
val LocalReduceMotion = compositionLocalOf { false }

fun contrastRatio(fg: Color, bg: Color): Float {
    val l1 = fg.luminance()
    val l2 = bg.luminance()
    return (max(l1, l2) + 0.05f) / (min(l1, l2) + 0.05f)
}

private fun typographyFor(font: AppFont): Typography {
    val bodyFamily = when (font) {
        AppFont.MODERN_SANS -> FontFamily.SansSerif
        AppFont.EDITORIAL_SERIF -> FontFamily.SansSerif
        AppFont.STUDIO_MONO -> FontFamily.Monospace
        AppFont.ROUNDED -> FontFamily.SansSerif
    }
    val headingFamily = when (font) {
        AppFont.EDITORIAL_SERIF -> FontFamily.Serif
        AppFont.MODERN_SANS, AppFont.ROUNDED -> FontFamily.SansSerif
        AppFont.STUDIO_MONO -> FontFamily.Monospace
    }
    val trackingBoost = if (font == AppFont.ROUNDED) 0.15f else 0f
    return Typography(
        displaySmall = TextStyle(fontFamily = headingFamily, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5 + trackingBoost).sp),
        headlineLarge = TextStyle(fontFamily = headingFamily, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4 + trackingBoost).sp),
        headlineMedium = TextStyle(fontFamily = headingFamily, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3 + trackingBoost).sp),
        headlineSmall = TextStyle(fontFamily = headingFamily, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2 + trackingBoost).sp),
        titleLarge = TextStyle(fontFamily = headingFamily, fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1 + trackingBoost).sp),
        titleMedium = TextStyle(fontFamily = bodyFamily, fontSize = 15.5.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, letterSpacing = trackingBoost.sp),
        titleSmall = TextStyle(fontFamily = bodyFamily, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (0.05 + trackingBoost).sp),
        bodyLarge = TextStyle(fontFamily = bodyFamily, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontFamily = bodyFamily, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
        bodySmall = TextStyle(fontFamily = bodyFamily, fontSize = 12.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal),
        labelLarge = TextStyle(fontFamily = bodyFamily, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = (0.1 + trackingBoost).sp),
        labelMedium = TextStyle(fontFamily = bodyFamily, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = (0.15 + trackingBoost).sp),
        labelSmall = TextStyle(fontFamily = bodyFamily, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (0.3 + trackingBoost).sp),
    )
}

@Composable
fun ProNotesTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val settings by store.settings.collectAsStateWithLifecycle()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val uiScheme = settings.uiColorScheme
    val accent = settings.accent
    val schemePrimary = Color(uiScheme.seedPrimary)
    val primary = if (uiScheme == UiColorScheme.SLATE_STUDIO) Color(accent.argb) else lerp(schemePrimary, Color(accent.argb), 0.35f)
    val secondary = Color(uiScheme.seedSecondary)
    val lightBg = Color(uiScheme.lightBg)
    val lightSurf = Color(uiScheme.lightSurface)
    val lightVar = lerp(lightBg, schemePrimary, 0.06f)

    val light = lightColorScheme(
        primary = primary,
        onPrimary = if (contrastRatio(primary, Color.White) >= contrastRatio(primary, Color(0xFF101218))) Color.White else Color(0xFF101218),
        primaryContainer = lerp(primary, lightSurf, 0.84f),
        onPrimaryContainer = lerp(primary, Color(0xFF1F2421), 0.35f),
        secondary = secondary,
        background = lightBg,
        onBackground = Color(0xFF181A1F),
        surface = lightSurf,
        onSurface = Color(0xFF181A1F),
        surfaceVariant = lightVar,
        onSurfaceVariant = Color(0xFF565B64),
        surfaceContainer = lightVar,
        surfaceContainerHigh = lerp(lightSurf, Color.White, 0.64f),
        outline = Color(0xFF9EA3AE),
        outlineVariant = lerp(lightVar, Color(0xFF9EA3AE), 0.25f),
        error = Color(0xFFDC2626),
    )
    val darkBg = Color(uiScheme.darkBg)
    val darkSurf = Color(uiScheme.darkSurface)
    val darkVar = lerp(darkSurf, Color.White, 0.05f)
    val darkPrimary = if (accent == AccentColor.GRAPHITE && uiScheme == UiColorScheme.SLATE_STUDIO) {
        Color(0xFF93C5FD)
    } else {
        lerp(primary, Color.White, 0.26f)
    }
    val darkScheme = darkColorScheme(
        primary = darkPrimary,
        onPrimary = Color(0xFF0A0D14),
        primaryContainer = lerp(darkSurf, primary, 0.34f),
        onPrimaryContainer = Color(0xFFF1F5F9),
        secondary = lerp(secondary, Color.White, 0.18f),
        background = darkBg,
        onBackground = Color(0xFFF3F4F6),
        surface = darkSurf,
        onSurface = Color(0xFFF3F4F6),
        surfaceVariant = darkVar,
        onSurfaceVariant = Color(0xFFA7ACB8),
        surfaceContainer = lerp(darkBg, darkSurf, 0.65f),
        surfaceContainerHigh = lerp(darkSurf, Color.White, 0.08f),
        outline = Color(0xFF5A5E6A),
        outlineVariant = lerp(darkSurf, Color.White, 0.12f),
        error = Color(0xFFF28B82),
    )
    val gradient = if (settings.gradientTitles) listOf(primary, Color(accent.argb2)).let { g ->
        if (dark) g.map { lerp(it, Color.White, 0.26f) } else g
    } else null
    val typography = remember(settings.appFont) { typographyFor(settings.appFont) }
    val roundnessValue = settings.cornerRoundnessDp.coerceIn(8f, 32f)
    val cornerDp = roundnessValue.dp
    val shapes = remember(roundnessValue) {
        Shapes(
            small = RoundedCornerShape((roundnessValue * 0.55f).coerceIn(6f, 18f).dp),
            medium = RoundedCornerShape((roundnessValue * 0.75f).coerceIn(8f, 24f).dp),
            large = RoundedCornerShape(cornerDp),
        )
    }
    val perf = settings.performanceMode
    val systemRemoveAnimations = remember(context) {
        runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
    val reduceMotion = systemRemoveAnimations || perf == PerformanceMode.BATTERY_SAVER || settings.motionStyle == MotionStyle.REDUCED
    val effectiveMotion = if (reduceMotion) MotionStyle.REDUCED else settings.motionStyle
    CompositionLocalProvider(
        LocalTitleGradient provides gradient,
        LocalMotionStyle provides effectiveMotion,
        LocalCornerRoundness provides cornerDp,
        LocalPerformanceMode provides perf,
        LocalUiColorScheme provides uiScheme,
        LocalLiquidGlass provides settings.liquidGlass,
        LocalClassicAppearance provides settings.classicAppearance,
        LocalReduceEffects provides settings.reduceEffects,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = if (dark) darkScheme else light,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}

@Composable
fun GradientTitle(text: String, style: TextStyle, modifier: Modifier = Modifier, maxLines: Int = 1) {
    val g = LocalTitleGradient.current
    Text(
        text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = if (g != null) style.copy(brush = Brush.linearGradient(g)) else style.copy(color = MaterialTheme.colorScheme.onBackground),
    )
}
