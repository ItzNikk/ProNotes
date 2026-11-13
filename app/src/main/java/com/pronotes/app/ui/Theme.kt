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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import com.pronotes.app.data.AccentColor
import com.pronotes.app.data.AppFont
import com.pronotes.app.data.MotionStyle
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.ThemeMode

val Accent = Color(0xFF2A78D6)

/** The two accent colours used for gradient titles. Null when gradients are off. */
val LocalTitleGradient = compositionLocalOf<List<Color>?> { null }
val LocalMotionStyle = compositionLocalOf { MotionStyle.ULTRA_SPRING }

private fun typographyFor(font: AppFont): Typography {
    val ff = when (font) {
        AppFont.MODERN_SANS -> FontFamily.SansSerif
        AppFont.EDITORIAL_SERIF -> FontFamily.Serif
        AppFont.STUDIO_MONO -> FontFamily.Monospace
        AppFont.ROUNDED -> FontFamily.SansSerif
    }
    val trackingBoost = if (font == AppFont.ROUNDED) 0.15f else 0f
    return Typography(
        displaySmall = TextStyle(fontFamily = ff, fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8 + trackingBoost).sp),
        headlineLarge = TextStyle(fontFamily = ff, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6 + trackingBoost).sp),
        headlineMedium = TextStyle(fontFamily = ff, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4 + trackingBoost).sp),
        headlineSmall = TextStyle(fontFamily = ff, fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2 + trackingBoost).sp),
        titleLarge = TextStyle(fontFamily = ff, fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.15 + trackingBoost).sp),
        titleMedium = TextStyle(fontFamily = ff, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = trackingBoost.sp),
        titleSmall = TextStyle(fontFamily = ff, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (0.1 + trackingBoost).sp),
        bodyLarge = TextStyle(fontFamily = ff, fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontFamily = ff, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
        bodySmall = TextStyle(fontFamily = ff, fontSize = 12.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal),
        labelLarge = TextStyle(fontFamily = ff, fontSize = 13.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (0.1 + trackingBoost).sp),
        labelMedium = TextStyle(fontFamily = ff, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = (0.2 + trackingBoost).sp),
        labelSmall = TextStyle(fontFamily = ff, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (0.35 + trackingBoost).sp),
    )
}

@Composable
fun ProNotesTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val settings by remember { SettingsStore.get(context) }.settings.collectAsState()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = settings.accent
    val primary = Color(accent.argb)
    val light = lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = lerp(primary, Color.White, 0.86f),
        onPrimaryContainer = lerp(primary, Color.Black, 0.5f),
        secondary = Color(0xFF5B6475),
        background = Color(0xFFF6F6F3),
        onBackground = Color(0xFF1B1F2A),
        surface = Color.White,
        onSurface = Color(0xFF1B1F2A),
        surfaceVariant = Color(0xFFEFEFEC),
        onSurfaceVariant = Color(0xFF5F5E5A),
        surfaceContainer = Color(0xFFF9F9F7),
        surfaceContainerHigh = Color.White,
        outline = Color(0xFFD2D1C7),
        outlineVariant = Color(0xFFE7E6E1),
    )
    val darkScheme = darkColorScheme(
        primary = lerp(primary, Color.White, 0.35f),
        onPrimary = Color(0xFF0B0B0B),
        primaryContainer = lerp(primary, Color.Black, 0.55f),
        onPrimaryContainer = lerp(primary, Color.White, 0.7f),
        background = Color(0xFF0E0F12),
        surface = Color(0xFF18191D),
        surfaceVariant = Color(0xFF25262B),
        surfaceContainer = Color(0xFF131418),
        surfaceContainerHigh = Color(0xFF1F2026),
        outline = Color(0xFF45464D),
        outlineVariant = Color(0xFF2B2C32),
    )
    val gradient = if (settings.gradientTitles) listOf(primary, Color(accent.argb2)).let { g ->
        if (dark) g.map { lerp(it, Color.White, 0.3f) } else g
    } else null
    val typography = remember(settings.appFont) { typographyFor(settings.appFont) }
    CompositionLocalProvider(
        LocalTitleGradient provides gradient,
        LocalMotionStyle provides settings.motionStyle,
    ) {
        MaterialTheme(
            colorScheme = if (dark) darkScheme else light,
            typography = typography,
            shapes = Shapes(
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(24.dp),
            ),
            content = content,
        )
    }
}

/** A title drawn with the accent gradient (or plain, when gradients are off). */
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

@Suppress("unused")
private fun accentFor(a: AccentColor) = Color(a.argb)
