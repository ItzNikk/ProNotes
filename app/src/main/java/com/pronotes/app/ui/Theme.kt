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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import com.pronotes.app.data.AccentColor
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.ThemeMode

val Accent = Color(0xFF2A78D6)

/** The two accent colours used for gradient titles. Null when gradients are off. */
val LocalTitleGradient = compositionLocalOf<List<Color>?> { null }

private val Type = Typography(
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.15).sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 13.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.35.sp),
)

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
        background = Color(0xFFF7F7F5),
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
        background = Color(0xFF0F0F0F),
        surface = Color(0xFF1A1A19),
        surfaceVariant = Color(0xFF262624),
        surfaceContainer = Color(0xFF151515),
        surfaceContainerHigh = Color(0xFF20201F),
        outline = Color(0xFF454442),
        outlineVariant = Color(0xFF2C2C2A),
    )
    val gradient = if (settings.gradientTitles) listOf(primary, Color(accent.argb2)).let { g ->
        if (dark) g.map { lerp(it, Color.White, 0.3f) } else g
    } else null
    CompositionLocalProvider(LocalTitleGradient provides gradient) {
        MaterialTheme(
            colorScheme = if (dark) darkScheme else light,
            typography = Type,
            shapes = Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(14.dp),
                large = RoundedCornerShape(22.dp),
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
