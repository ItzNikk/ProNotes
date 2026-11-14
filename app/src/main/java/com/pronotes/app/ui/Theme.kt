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
import com.pronotes.app.data.AccentColor
import com.pronotes.app.data.AppFont
import com.pronotes.app.data.MotionStyle
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.ThemeMode
import io.github.ntvien.liquidglass.GlassColors

/** OpenLeaf primary botanical leaf green (`Y.Light.accent`). */
val Accent = Color(0xFF24483F)

/** The two accent colours used for gradient titles. Null when gradients are off. */
val LocalTitleGradient = compositionLocalOf<List<Color>?> { null }
val LocalMotionStyle = compositionLocalOf { MotionStyle.ULTRA_SPRING }
val LocalGlassTransparency = compositionLocalOf { 0.58f }

/** Builds adaptive OpenLeaf GlassColors responding to [transparency] (0.15f..0.95f) while preserving crisp readability. */
fun openLeafGlassColors(dark: Boolean, accent: AccentColor, transparency: Float): GlassColors {
    val t = ((transparency.coerceIn(0.15f, 0.95f) - 0.15f) / 0.80f).coerceIn(0f, 1f)
    val primary = Color(accent.argb)
    val darkPrimary = if (accent == AccentColor.GREEN) Color(0xFF8CC1AC) else lerp(primary, Color(0xFFEDE8DB), 0.38f)
    return if (!dark) {
        val base = Color(0xFFFBF9F4)
        io.github.ntvien.liquidglass.GlassColors(
            content = Color(0xFF1F2421),
            contentMuted = Color(0xFF5A5F58),
            accent = primary,
            accentSoft = primary.copy(alpha = 0.18f),
            onAccent = Color(0xFFF5F2E8),
            selectedFill = primary,
            selectedContent = Color(0xFFF5F2E8),
            badge = Color(0xFF9B3B3B),
            glassFill = base.copy(alpha = androidx.compose.ui.util.lerp(0.54f, 0.10f, t)),
            glassFillStrong = base.copy(alpha = androidx.compose.ui.util.lerp(0.68f, 0.22f, t)),
            glassFillOpaque = base.copy(alpha = androidx.compose.ui.util.lerp(0.88f, 0.66f, t)),
            glassStroke = Color.White.copy(alpha = 0.18f),
            glassHighlight = Color.White.copy(alpha = 0.90f),
            glassShadow = Color(0x221F2421),
            raisedFill = Color(0xE6FDFCF8),
            chromeFill = base.copy(alpha = androidx.compose.ui.util.lerp(0.60f, 0.16f, t)),
            tabSelectedFill = primary.copy(alpha = 0.20f),
            tabSelectedContent = primary,
        )
    } else {
        val base = Color(0xFF1D1C18)
        io.github.ntvien.liquidglass.GlassColors(
            content = Color(0xFFEDE8DB),
            contentMuted = Color(0xFFB1AC9F),
            accent = darkPrimary,
            accentSoft = darkPrimary.copy(alpha = 0.24f),
            onAccent = Color(0xFF0F1D18),
            selectedFill = darkPrimary,
            selectedContent = Color(0xFF0F1D18),
            badge = Color(0xFFD67B7B),
            glassFill = base.copy(alpha = androidx.compose.ui.util.lerp(0.58f, 0.12f, t)),
            glassFillStrong = base.copy(alpha = androidx.compose.ui.util.lerp(0.72f, 0.26f, t)),
            glassFillOpaque = base.copy(alpha = androidx.compose.ui.util.lerp(0.90f, 0.70f, t)),
            glassStroke = Color(0x24EDE8DB),
            glassHighlight = Color(0x78EDE8DB),
            glassShadow = Color(0x66000000),
            raisedFill = Color(0xE62D2B25),
            chromeFill = base.copy(alpha = androidx.compose.ui.util.lerp(0.64f, 0.18f, t)),
            tabSelectedFill = darkPrimary.copy(alpha = 0.28f),
            tabSelectedContent = darkPrimary,
        )
    }
}

private fun typographyFor(font: AppFont): Typography {
    val bodyFamily = when (font) {
        AppFont.MODERN_SANS -> FontFamily.SansSerif
        AppFont.EDITORIAL_SERIF -> FontFamily.SansSerif
        AppFont.STUDIO_MONO -> FontFamily.Monospace
        AppFont.ROUNDED -> FontFamily.SansSerif
    }
    // OpenLeaf signature typography: editorial serif for display/headings, crisp sans for UI controls.
    val headingFamily = when (font) {
        AppFont.EDITORIAL_SERIF, AppFont.MODERN_SANS -> FontFamily.Serif
        AppFont.STUDIO_MONO -> FontFamily.Monospace
        AppFont.ROUNDED -> FontFamily.SansSerif
    }
    val trackingBoost = if (font == AppFont.ROUNDED) 0.15f else 0f
    return Typography(
        displaySmall = TextStyle(fontFamily = headingFamily, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5 + trackingBoost).sp),
        headlineLarge = TextStyle(fontFamily = headingFamily, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4 + trackingBoost).sp),
        headlineMedium = TextStyle(fontFamily = headingFamily, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3 + trackingBoost).sp),
        headlineSmall = TextStyle(fontFamily = headingFamily, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2 + trackingBoost).sp),
        titleLarge = TextStyle(fontFamily = headingFamily, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1 + trackingBoost).sp),
        titleMedium = TextStyle(fontFamily = bodyFamily, fontSize = 15.5.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, letterSpacing = trackingBoost.sp),
        titleSmall = TextStyle(fontFamily = bodyFamily, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (0.05 + trackingBoost).sp),
        bodyLarge = TextStyle(fontFamily = bodyFamily, fontSize = 15.5.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
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
    val settings by remember { SettingsStore.get(context) }.settings.collectAsState()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = settings.accent
    val primary = Color(accent.argb)
    // OpenLeaf Light Palette (Y.Light: bg=#EFEBE2, surface=#FBF9F4, surface2=#F4F0E7, ink=#1F2421, ink2=#5A5F58)
    val light = lightColorScheme(
        primary = primary,
        onPrimary = Color(0xFFF5F2E8),
        primaryContainer = lerp(primary, Color(0xFFFBF9F4), 0.86f),
        onPrimaryContainer = lerp(primary, Color(0xFF1F2421), 0.35f),
        secondary = Color(0xFF35655A),
        background = Color(0xFFEFEBE2),
        onBackground = Color(0xFF1F2421),
        surface = Color(0xFFFBF9F4),
        onSurface = Color(0xFF1F2421),
        surfaceVariant = Color(0xFFF4F0E7),
        onSurfaceVariant = Color(0xFF5A5F58),
        surfaceContainer = Color(0xFFF4F0E7),
        surfaceContainerHigh = Color(0xFFFBF9F4),
        outline = Color(0xFF8E9189),
        outlineVariant = Color(0xFFE0DBD0),
        error = Color(0xFF9B3B3B),
    )
    // OpenLeaf Dark Palette (Y.Dark: bg=#141411, surface=#1D1C18, surface2=#25241F, ink=#EDE8DB, ink2=#B1AC9F)
    val darkPrimary = if (accent == AccentColor.GREEN) Color(0xFF8CC1AC) else lerp(primary, Color(0xFFEDE8DB), 0.38f)
    val darkScheme = darkColorScheme(
        primary = darkPrimary,
        onPrimary = Color(0xFF0F1D18),
        primaryContainer = lerp(darkPrimary, Color(0xFF141411), 0.72f),
        onPrimaryContainer = Color(0xFFEDE8DB),
        secondary = Color(0xFFA5D3C0),
        background = Color(0xFF141411),
        onBackground = Color(0xFFEDE8DB),
        surface = Color(0xFF1D1C18),
        onSurface = Color(0xFFEDE8DB),
        surfaceVariant = Color(0xFF25241F),
        onSurfaceVariant = Color(0xFFB1AC9F),
        surfaceContainer = Color(0xFF25241F),
        surfaceContainerHigh = Color(0xFF2E2C26),
        outline = Color(0xFF7F7B70),
        outlineVariant = Color(0xFF2E2C26),
        error = Color(0xFFD67B7B),
    )
    val gradient = if (settings.gradientTitles) listOf(primary, Color(accent.argb2)).let { g ->
        if (dark) g.map { lerp(it, Color.White, 0.3f) } else g
    } else null
    val typography = remember(settings.appFont) { typographyFor(settings.appFont) }
    val glassColors = remember(dark, accent, settings.glassTransparency) {
        openLeafGlassColors(dark, accent, settings.glassTransparency)
    }
    CompositionLocalProvider(
        LocalTitleGradient provides gradient,
        LocalMotionStyle provides settings.motionStyle,
        LocalGlassTransparency provides settings.glassTransparency,
    ) {
        io.github.ntvien.liquidglass.LiquidGlassTheme(
            colors = glassColors,
            look = io.github.ntvien.liquidglass.GlassLook.Auto,
            reduceEffects = !settings.liquidGlass,
            overlaySamplesContent = if (settings.liquidGlass) null else false,
        ) {
            MaterialTheme(
            colorScheme = if (dark) darkScheme else light,
            typography = typography,
            shapes = Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(14.dp),
                large = RoundedCornerShape(20.dp),
            ),
            content = content,
        )
        }
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
