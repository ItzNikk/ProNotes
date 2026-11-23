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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pronotes.app.data.AccentColor
import com.pronotes.app.data.AppFont
import com.pronotes.app.data.AppLanguages
import com.pronotes.app.data.LocalAppLanguage
import com.pronotes.app.data.LocalAppStrings
import com.pronotes.app.data.Localization
import com.pronotes.app.data.MotionStyle
import com.pronotes.app.data.PerformanceMode
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.ThemeMode
import com.pronotes.app.data.UiColorScheme
import kotlin.math.max
import kotlin.math.min

val Accent = Color(0xFF1A84FF)

val LocalTitleGradient = compositionLocalOf<List<Color>?> { null }
val LocalColorfulUiText = compositionLocalOf { true }
val LocalMotionStyle = compositionLocalOf { MotionStyle.SPRING }
val LocalCornerRoundness = compositionLocalOf<Dp> { 20.dp }
val LocalPerformanceMode = compositionLocalOf { PerformanceMode.BALANCED }
val LocalUiColorScheme = compositionLocalOf { UiColorScheme.SLATE_STUDIO }
val LocalLiveBlur = compositionLocalOf { true }
val LocalClassicAppearance = compositionLocalOf { false }
val LocalReduceEffects = compositionLocalOf { false }
val LocalReduceMotion = compositionLocalOf { false }

fun contrastRatio(fg: Color, bg: Color): Float {
    val l1 = fg.luminance()
    val l2 = bg.luminance()
    return (max(l1, l2) + 0.05f) / (min(l1, l2) + 0.05f)
}

private fun typographyFor(font: AppFont, headingGradient: List<Color>?, subGradient: List<Color>?): Typography {
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
    val trackingBoost = if (font == AppFont.ROUNDED) 0.18f else 0f
    val headBrush = headingGradient?.let { Brush.linearGradient(it) }
    val subBrush = subGradient?.let { Brush.linearGradient(it) }

    // Varied, expressive typographic hierarchy with vibrant gradient headings and high-contrast readable body/button text.
    return Typography(
        displaySmall = TextStyle(fontFamily = headingFamily, fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.7 + trackingBoost).sp, brush = headBrush),
        headlineLarge = TextStyle(fontFamily = headingFamily, fontSize = 33.sp, lineHeight = 39.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5 + trackingBoost).sp, brush = headBrush),
        headlineMedium = TextStyle(fontFamily = headingFamily, fontSize = 27.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.35 + trackingBoost).sp, brush = headBrush),
        headlineSmall = TextStyle(fontFamily = headingFamily, fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2 + trackingBoost).sp, brush = headBrush),
        titleLarge = TextStyle(fontFamily = headingFamily, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.15 + trackingBoost).sp, brush = headBrush),
        titleMedium = TextStyle(fontFamily = bodyFamily, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = trackingBoost.sp, brush = subBrush),
        titleSmall = TextStyle(fontFamily = bodyFamily, fontSize = 14.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (0.12 + trackingBoost).sp, brush = subBrush),
        bodyLarge = TextStyle(fontFamily = bodyFamily, fontSize = 16.5.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontFamily = bodyFamily, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
        bodySmall = TextStyle(fontFamily = bodyFamily, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal),
        labelLarge = TextStyle(fontFamily = bodyFamily, fontSize = 13.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (0.12 + trackingBoost).sp),
        labelMedium = TextStyle(fontFamily = bodyFamily, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (0.2 + trackingBoost).sp),
        labelSmall = TextStyle(fontFamily = bodyFamily, fontSize = 10.5.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = (0.35 + trackingBoost).sp),
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
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            (context as? android.app.Activity)?.window?.let { window ->
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val uiScheme = settings.uiColorScheme
    val colorful = uiScheme == UiColorScheme.COLORFUL
    val accent = settings.accent
    val schemePrimary = Color(uiScheme.seedPrimary)
    val primary = if (uiScheme == UiColorScheme.SLATE_STUDIO) Color(accent.argb) else if (colorful) schemePrimary else lerp(schemePrimary, Color(accent.argb), 0.35f)
    val secondary = Color(uiScheme.seedSecondary)
    val lightBg = Color(uiScheme.lightBg)
    val lightSurf = Color(uiScheme.lightSurface)
    val lightVar = if (colorful) Color(0xFFE9E4FB) else lerp(lightBg, schemePrimary, 0.06f)

    val light = lightColorScheme(
        primary = primary,
        onPrimary = if (contrastRatio(primary, Color.White) >= contrastRatio(primary, Color(0xFF101218))) Color.White else Color(0xFF101218),
        primaryContainer = lerp(primary, lightSurf, 0.84f),
        onPrimaryContainer = lerp(primary, Color(0xFF1F2421), 0.35f),
        secondary = secondary,
        onSecondary = if (colorful) Color.White else Color(0xFF111827),
        tertiary = if (colorful) Color(0xFFB53C58) else primary,
        tertiaryContainer = if (colorful) Color(0xFFFFD9E1) else lightVar,
        onTertiaryContainer = if (colorful) Color(0xFF450C20) else Color(0xFF181A1F),
        secondaryContainer = if (colorful) Color(0xFFB5F2E8) else lightVar,
        onSecondaryContainer = if (colorful) Color(0xFF093E3B) else Color(0xFF181A1F),
        background = lightBg,
        onBackground = Color(0xFF181A1F),
        surface = lightSurf,
        onSurface = Color(0xFF181A1F),
        surfaceVariant = lightVar,
        onSurfaceVariant = Color(0xFF565B64),
        surfaceContainer = lightVar,
        surfaceContainerHigh = if (colorful) Color(0xFFF1E8FF) else lerp(lightSurf, Color.White, 0.64f),
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
        secondary = if (colorful) Color(0xFF7ADFD3) else lerp(secondary, Color.White, 0.18f),
        onSecondary = Color(0xFF0A2020),
        tertiary = if (colorful) Color(0xFFFFA8BB) else darkPrimary,
        tertiaryContainer = if (colorful) Color(0xFF58213A) else darkVar,
        onTertiaryContainer = if (colorful) Color(0xFFFFD9E1) else Color(0xFFF3F4F6),
        secondaryContainer = if (colorful) Color(0xFF154944) else darkVar,
        onSecondaryContainer = if (colorful) Color(0xFFB5F2E8) else Color(0xFFF3F4F6),
        background = darkBg,
        onBackground = Color(0xFFF3F4F6),
        surface = darkSurf,
        onSurface = Color(0xFFF3F4F6),
        surfaceVariant = darkVar,
        onSurfaceVariant = Color(0xFFA7ACB8),
        surfaceContainer = lerp(darkBg, darkSurf, 0.65f),
        surfaceContainerHigh = if (colorful) Color(0xFF342754) else lerp(darkSurf, Color.White, 0.08f),
        outline = Color(0xFF5A5E6A),
        outlineVariant = lerp(darkSurf, Color.White, 0.12f),
        error = Color(0xFFF28B82),
    )

    // Vibrant multi-stop colorful UI gradient for headings & expressive titles.
    val enableGradients = settings.gradientTitles || settings.colorfulUiText || colorful
    val gradient = if (enableGradients) {
        if (dark) {
            listOf(
                lerp(darkPrimary, Color(0xFF60A5FA), 0.35f),
                Color(0xFFC084FC),
                Color(0xFFFF7EB6),
                Color(0xFFFBBF24),
            )
        } else {
            listOf(
                lerp(primary, Color(0xFF2563EB), 0.3f),
                Color(0xFF8B5CF6),
                Color(0xFFEC4899),
                Color(0xFFF97316),
            )
        }
    } else {
        null
    }
    val subGradient = if (settings.colorfulUiText) {
        if (dark) {
            listOf(Color(0xFF7DD3FC), Color(0xFFD8B4FE), Color(0xFFF9A8D4))
        } else {
            listOf(Color(0xFF1D4ED8), Color(0xFF7C3AED), Color(0xFFDB2777))
        }
    } else {
        null
    }
    val typography = remember(settings.appFont, gradient, subGradient) {
        typographyFor(settings.appFont, gradient, subGradient)
    }
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
    val packVersion by com.pronotes.app.data.LanguagePackManager.packVersion.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(context) {
        com.pronotes.app.data.LanguagePackManager.refreshInstalled(context)
    }
    val appLanguageOpt = remember(settings.appLanguage) { AppLanguages.findByTag(settings.appLanguage) }
    val appStrings = remember(settings.appLanguage, packVersion) {
        Localization.stringsFor(context, settings.appLanguage)
    }
    val layoutDirection = remember(settings.appLanguage, packVersion) {
        val rtlReady = AppLanguages.isRtl(settings.appLanguage) &&
            com.pronotes.app.data.LanguagePackManager.isInstalled(context, settings.appLanguage)
        if (rtlReady) LayoutDirection.Rtl else LayoutDirection.Ltr
    }
    CompositionLocalProvider(
        LocalTitleGradient provides gradient,
        LocalColorfulUiText provides settings.colorfulUiText,
        LocalMotionStyle provides effectiveMotion,
        LocalCornerRoundness provides cornerDp,
        LocalPerformanceMode provides perf,
        LocalUiColorScheme provides uiScheme,
        LocalLiveBlur provides settings.liveBlur,
        LocalClassicAppearance provides settings.classicAppearance,
        LocalReduceEffects provides settings.reduceEffects,
        LocalReduceMotion provides reduceMotion,
        LocalAppStrings provides appStrings,
        LocalAppLanguage provides appLanguageOpt,
        LocalLayoutDirection provides layoutDirection,
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
    if (g != null) {
        AnimatedGradientText(
            text = text,
            style = style,
            modifier = modifier,
            colors = g,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            durationMs = 3600,
            pulseAlpha = false,
        )
    } else {
        Text(
            text,
            modifier = modifier,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            style = style.copy(color = MaterialTheme.colorScheme.onBackground),
        )
    }
}

@Composable
fun GradientText(text: String, style: TextStyle = MaterialTheme.typography.bodyMedium, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    val g = LocalTitleGradient.current
    if (g != null) {
        AnimatedGradientText(
            text = text,
            style = style,
            modifier = modifier,
            colors = g,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            durationMs = 4000,
            pulseAlpha = false,
        )
    } else {
        Text(
            text,
            modifier = modifier,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            style = style,
        )
    }
}

/** A shared backdrop for library, settings and reading surfaces. */
@Composable
internal fun workspaceBackground(base: Color = MaterialTheme.colorScheme.background): Brush {
    val scheme = MaterialTheme.colorScheme
    val stops = if (LocalUiColorScheme.current == UiColorScheme.COLORFUL) {
        listOf(lerp(base, scheme.primaryContainer, .58f), base,
            lerp(base, scheme.secondaryContainer, .38f), lerp(base, scheme.tertiaryContainer, .34f))
    } else listOf(base, base)
    return Brush.verticalGradient(stops)
}