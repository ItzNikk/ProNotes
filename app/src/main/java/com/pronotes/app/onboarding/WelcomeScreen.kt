package com.pronotes.app.onboarding

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pronotes.app.data.AccentColor
import com.pronotes.app.data.AppFont
import com.pronotes.app.data.MotionStyle
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.PenBrands
import com.pronotes.app.data.ScrollDirection
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.ThemeMode
import com.pronotes.app.data.ToolbarDock
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppMenuButton
import com.pronotes.app.ui.AppMenuItem
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.AppSliderRow
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.AppToggle
import com.pronotes.app.ui.FadeAnimatedText
import com.pronotes.app.ui.FannedColorfulCardsDeck
import com.pronotes.app.ui.JumpAnimatedText
import com.pronotes.app.ui.MovingColorfulGradientBackground
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import com.pronotes.app.ui.RotateAnimatedText
import com.pronotes.app.ui.ScaleInAnimatedText
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.appCardShape
import com.pronotes.app.ui.appPillShape
import com.pronotes.app.ui.elasticStretch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

private val BrandBlue = ProButtonPalette.RoyalBlue
private val BrandSky = ProButtonPalette.SkyCyan
private val BrandOrange = ProButtonPalette.VibrantOrange
private val BrandPink = ProButtonPalette.MagentaPink

/**
 * Interactive Preview & Setup Studio:
 * 1) Welcome Showcase with auto-cycling fanned colorful cards over a slowly moving gradient
 * 2) Appearance & Typography Studio
 * 3) Workspace & Gestures Tour (including Kindle Book Mode)
 * 4) Stylus Check & Calibration
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val store = remember { SettingsStore.get(context) }
    val pager = rememberPagerState { 4 }
    val scope = rememberCoroutineScope()
    fun next() = scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(3)) }
    fun back() = scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) }
    fun finish() {
        store.update { it.copy(onboardingDone = true) }
        onDone()
    }

    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        MovingColorfulGradientBackground(
            modifier = Modifier.fillMaxSize(),
            intensity = if (pager.currentPage == 0) 1.2f else 0.72f,
        )

        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            val active = pager.currentPage == page
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (page) {
                    0 -> IntroPage(active, onStart = { next() })
                    1 -> LookStudioPage(active, store, onNext = { next() })
                    2 -> WorkspaceTourPage(active, store, onNext = { next() })
                    else -> PenCheckPage(active, store, onNext = { finish() }, isFinal = true)
                }
            }
        }

        if (pager.currentPage > 0) {
            AppPill(
                onClick = { back() },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .safeDrawingPadding()
                    .padding(10.dp),
            ) {
                Text("Back", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(4) { i ->
                val w by animateFloatAsState(if (pager.currentPage == i) 28f else 8f, spring(dampingRatio = 0.7f), label = "dot")
                val dotColor = when (i) {
                    0 -> BrandBlue
                    1 -> BrandSky
                    2 -> BrandOrange
                    else -> BrandPink
                }
                Box(
                    Modifier
                        .size(width = w.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pager.currentPage == i) dotColor
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        )
                        .clickable { scope.launch { pager.animateScrollToPage(i) } },
                )
            }
        }

        AppPill(
            selected = true,
            tint = ProButtonPalette.ElectricBlue,
            onClick = { finish() },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(10.dp),
        ) {
            Text(
                if (pager.currentPage == 0) "Open Library" else "Skip setup",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun IntroPage(active: Boolean, onStart: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.45f
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 660.dp
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            FannedColorfulCardsDeck(
                autoCycle = active,
                showLabels = true,
                compact = compact,
            )
            Spacer(Modifier.height(if (compact) 14.dp else 26.dp))

            AppPill(
                selected = true,
                tint = BrandBlue,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(
                    "PRO NOTES STUDIO",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.8.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(12.dp))
            ScaleInAnimatedText(
                text = "Pro Notes",
                color = if (dark) Color.White else MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = if (compact) 42.sp else 54.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                ),
                staggerMs = 45L,
            )
            Spacer(Modifier.height(6.dp))
            JumpAnimatedText(
                text = "Write. Read. Master.",
                color = if (dark) Color(0xFFDBEAFE) else ProButtonPalette.RoyalBlue,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = if (compact) 20.sp else 24.sp,
                    fontWeight = FontWeight.Medium,
                ),
                staggerMs = 30L,
                startDelayMs = 240L,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Zero-latency stylus ink, 3D Kindle page-curl reading mode, flashcards decks, and structured slide shows.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 500.dp),
            )
            Spacer(Modifier.height(24.dp))

            val pulse by rememberInfiniteTransition(label = "cta")
                .animateFloat(1f, 1.04f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")

            Button(
                onClick = onStart,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White),
                modifier = Modifier
                    .widthIn(min = 240.dp)
                    .height(54.dp)
                    .scale(pulse)
                    .elasticStretch(),
            ) {
                Text("Customize your Studio", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LookStudioPage(active: Boolean, store: SettingsStore, onNext: () -> Unit) {
    val s by store.settings.collectAsStateWithLifecycle()
    val previewFamily = when (s.appFont) {
        AppFont.EDITORIAL_SERIF -> FontFamily.Serif
        AppFont.STUDIO_MONO -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    val accentBrush = Brush.linearGradient(listOf(Color(s.accent.argb), Color(s.accent.argb2)))

    ScaleInAnimatedText(
        text = "Craft your look",
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold),
    )
    Text(
        "Live preview updates as you pick theme, corner roundness, accent color, and typography.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(14.dp))

    AppCard(
        hero = true,
        shape = appCardShape(),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape((s.cornerRoundnessDp * 0.7f).coerceIn(8f, 20f).dp))
                    .background(accentBrush),
                contentAlignment = Alignment.Center,
            ) {
                ProIconView(ProIcon.PEN, Color.White, Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Organic Chemistry · Lecture 4", fontFamily = previewFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "${s.themeMode.label} · ${s.appFont.label} · ${s.cornerRoundnessDp.toInt()}dp · ${s.motionStyle.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    AppCard(
        hero = false,
        shape = appCardShape(),
        contentPadding = PaddingValues(18.dp),
        modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("THEME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ThemeMode.entries.forEach { t ->
                    AppChip(s.themeMode == t, { store.update { it.copy(themeMode = t) } }, t.label, tint = ProButtonPalette.ElectricBlue)
                }
            }
            AppSliderRow(
                title = "CORNER ROUNDNESS",
                valueText = "${s.cornerRoundnessDp.toInt()} dp",
                value = s.cornerRoundnessDp.coerceIn(8f, 32f),
                valueRange = 8f..32f,
                onValueChange = { v -> store.update { it.copy(cornerRoundnessDp = v.coerceIn(8f, 32f)) } },
            )
            Text("TYPOGRAPHY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppFont.entries.forEach { f ->
                    AppChip(s.appFont == f, { store.update { it.copy(appFont = f) } }, f.label, tint = ProButtonPalette.SkyCyan)
                }
            }
            Text("ACCENT COLOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentColor.entries.forEach { a ->
                    val on = a == s.accent
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(a.argb), Color(a.argb2))))
                            .border(if (on) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            .elasticStretch()
                            .clickable { store.update { it.copy(accent = a) } },
                    )
                }
            }
            Text("MOTION ENGINE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MotionStyle.entries.forEach { m ->
                    AppChip(s.motionStyle == m, { store.update { it.copy(motionStyle = m) } }, m.label, tint = ProButtonPalette.VibrantOrange)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onNext,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = BrandSky, contentColor = Color.White),
        modifier = Modifier.widthIn(min = 210.dp).elasticStretch(),
    ) { Text("Next: Workspace & Book Mode", fontWeight = FontWeight.SemiBold) }
}

private enum class Demo(val title: String, val body: String) {
    LASSO("Circle to select", "Draw a loop around ink, lift, then press and hold the blue dot at its end."),
    SCRIBBLE("Scribble to erase", "Zig-zag over ink to delete it. The pen's side button erases too."),
    SNAP("Hold to snap", "Hold the pen still at the end of a stroke to turn it into a clean shape."),
    KINDLE("Kindle Book Mode", "Flip through any notebook in 3D page-curl view with Paper, Sepia, or Night mode."),
    CARDS("Flashcards", "Drag a box over a PDF to make a card, then study with spaced review."),
    UNDO("Two-finger tap", "Tap with two fingers to undo, three to redo. Double-tap to zoom."),
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun WorkspaceTourPage(active: Boolean, store: SettingsStore, onNext: () -> Unit) {
    val s by store.settings.collectAsStateWithLifecycle()
    val demos = Demo.entries
    val inner = rememberPagerState { demos.size }
    val scope = rememberCoroutineScope()

    RotateAnimatedText(
        text = "Workspace & Gestures",
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold),
    )
    Text(
        "Interactive pen gestures, Kindle book reading, and default paper settings.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))

    HorizontalPager(inner, Modifier.widthIn(max = 580.dp).fillMaxWidth().height(250.dp), pageSpacing = 14.dp) { i ->
        val d = demos[i]
        AppCard(
            hero = inner.currentPage == i,
            shape = appCardShape(),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    Modifier
                        .size(width = 170.dp, height = 190.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFF4F1FF), Color(0xFFEAF2FF)))),
                    contentAlignment = Alignment.Center,
                ) { DemoArt(d, active && inner.currentPage == i, Modifier.fillMaxSize().padding(14.dp)) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(d.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(d.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(demos.size) { i ->
            val w by animateFloatAsState(if (inner.currentPage == i) 22f else 7f, spring(dampingRatio = 0.7f), label = "td")
            Box(
                Modifier
                    .size(width = w.dp, height = 7.dp)
                    .clip(CircleShape)
                    .background(if (inner.currentPage == i) BrandOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    .clickable { scope.launch { inner.animateScrollToPage(i) } },
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    AppCard(
        hero = false,
        shape = appCardShape(),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.widthIn(max = 580.dp).fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 175.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("SCROLL DIRECTION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScrollDirection.entries.forEach { d ->
                    AppChip(
                        s.scroll == d,
                        { store.update { it.copy(scroll = d) } },
                        if (d == ScrollDirection.VERTICAL) "Vertical Continuous" else "Horizontal Page Snap",
                        tint = ProButtonPalette.ElectricBlue,
                    )
                }
            }
            Text("DEFAULT PAPER TEMPLATE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PaperStyle.pickerEntries.forEach { p ->
                    AppChip(s.defaultPaper == p, { store.update { it.copy(defaultPaper = p) } }, p.label, tint = ProButtonPalette.Emerald)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hover tool name tooltips")
                    Text("Shows floating name pill when hovering with stylus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AppToggle(checked = s.showHoverTooltips, onCheckedChange = { v -> store.update { it.copy(showHoverTooltips = v) } })
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Button(
        onClick = onNext,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange, contentColor = Color.White),
        modifier = Modifier.widthIn(min = 210.dp).elasticStretch(),
    ) { Text("Next: Stylus Check", fontWeight = FontWeight.SemiBold) }
}

private enum class PenStep(val title: String, val hint: String) {
    HOVER("Bring your pen close", "Hold the pen tip just above the ring without touching."),
    BUTTON("Press and hold the button", "Keep the side button pressed while the pen hovers."),
    TAPS("Tap the ring 3 times", "Tap gently with the pen tip, one tap at a time."),
    SCAN("Scanning your pen", ""),
    BURST("", ""),
    DONE("", ""),
}

private class PenSpy(private val base: android.view.Window.Callback, private val onEvent: (MotionEvent) -> Unit) :
    android.view.Window.Callback by base {
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        onEvent(event)
        return base.dispatchGenericMotionEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        onEvent(event)
        return base.dispatchTouchEvent(event)
    }
}

private fun MotionEvent.isPen(): Boolean {
    val t = getToolType(0)
    return t == MotionEvent.TOOL_TYPE_STYLUS || t == MotionEvent.TOOL_TYPE_ERASER || isFromSource(InputDevice.SOURCE_STYLUS)
}

private fun MotionEvent.penButton(): Boolean =
    buttonState and (MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_STYLUS_SECONDARY or MotionEvent.BUTTON_SECONDARY or MotionEvent.BUTTON_TERTIARY) != 0

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
private fun PenCheckPage(active: Boolean, store: SettingsStore, onNext: () -> Unit, isFinal: Boolean = false) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(PenStep.HOVER) }
    var taps by remember { mutableIntStateOf(0) }
    var tapBusy by remember { mutableStateOf(false) }
    var hovering by remember { mutableStateOf(false) }
    var buttonDown by remember { mutableStateOf(false) }
    var device by remember { mutableStateOf<String?>(null) }
    var caps by remember { mutableStateOf<List<String>>(emptyList()) }
    var brand by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    var canSkip by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val pulse = remember { Animatable(1f) }
    val burst = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun readDevice(ev: MotionEvent) {
        val dev = ev.device ?: return
        device = dev.name
        caps = buildList {
            if (dev.getMotionRange(MotionEvent.AXIS_PRESSURE, InputDevice.SOURCE_STYLUS) != null) add("Pressure")
            if (dev.getMotionRange(MotionEvent.AXIS_TILT, InputDevice.SOURCE_STYLUS) != null) add("Tilt")
            if (dev.getMotionRange(MotionEvent.AXIS_DISTANCE, InputDevice.SOURCE_STYLUS) != null) add("Hover")
        }
    }

    DisposableEffect(active) {
        val window = context.findActivity()?.window
        val original = window?.callback
        if (active && window != null && original != null) {
            window.callback = PenSpy(original) { ev ->
                if (!ev.isPen()) return@PenSpy
                when (ev.actionMasked) {
                    MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                        hovering = true
                        buttonDown = ev.penButton()
                        readDevice(ev)
                    }
                    MotionEvent.ACTION_HOVER_EXIT -> {
                        hovering = false
                        buttonDown = false
                    }
                    MotionEvent.ACTION_BUTTON_PRESS -> buttonDown = true
                    MotionEvent.ACTION_BUTTON_RELEASE -> buttonDown = false
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        buttonDown = ev.penButton()
                        readDevice(ev)
                    }
                    MotionEvent.ACTION_UP -> buttonDown = false
                }
            }
        }
        onDispose { if (window != null && original != null && window.callback is PenSpy) window.callback = original }
    }

    LaunchedEffect(step) {
        canSkip = false
        note = null
        if (step == PenStep.HOVER || step == PenStep.BUTTON) {
            delay(6000)
            canSkip = true
        }
    }

    LaunchedEffect(step, hovering) {
        if (step != PenStep.HOVER) return@LaunchedEffect
        if (!hovering) {
            progress.animateTo(0f, tween(300))
            return@LaunchedEffect
        }
        note = "Pen detected. Keep it there…"
        progress.animateTo(15f, tween(1400, easing = LinearEasing))
        if (hovering) {
            pulse.snapTo(0.94f)
            pulse.animateTo(1f, spring(dampingRatio = 0.4f))
            delay(250)
            step = PenStep.BUTTON
        }
    }

    LaunchedEffect(step, buttonDown) {
        if (step != PenStep.BUTTON) return@LaunchedEffect
        if (!buttonDown) {
            if (progress.value > 15f) progress.animateTo(15f, tween(300))
            return@LaunchedEffect
        }
        note = "Button pressed. Keep holding…"
        progress.animateTo(30f, tween(1200, easing = LinearEasing))
        if (buttonDown) {
            pulse.snapTo(0.94f)
            pulse.animateTo(1f, spring(dampingRatio = 0.4f))
            delay(250)
            step = PenStep.TAPS
        }
    }

    LaunchedEffect(step) {
        if (step != PenStep.SCAN) return@LaunchedEffect
        progress.animateTo(100f, tween(3400, easing = LinearEasing))
        delay(200)
        val b = PenBrands.detect(device)
        brand = b
        store.registerPen(device ?: "Stylus", b)
        step = PenStep.BURST
        burst.snapTo(0f)
        burst.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        step = PenStep.DONE
        reveal.snapTo(0f)
        reveal.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 140f))
    }

    val pct = progress.value.toInt().coerceIn(0, 100)
    val scanLabel = when {
        pct < 50 -> "Reading pen signal…"
        pct < 68 -> "Checking pressure…"
        pct < 86 -> "Checking tilt and hover…"
        else -> "Matching brand…"
    }
    val done = step == PenStep.DONE

    FadeAnimatedText(
        text = "Meet your pen",
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp, fontWeight = FontWeight.ExtraBold),
    )
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(PenStep.HOVER, PenStep.BUTTON, PenStep.TAPS, PenStep.SCAN).forEach { s ->
            val reached = step.ordinal >= s.ordinal
            val w by animateFloatAsState(if (step == s) 22f else 8f, spring(dampingRatio = 0.7f), label = "sd")
            Box(
                Modifier
                    .size(width = w.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (reached) BrandSky else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)),
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    val title = when (step) {
        PenStep.SCAN -> scanLabel
        PenStep.BURST -> "Got it!"
        PenStep.DONE -> brand?.takeIf { it != "Stylus" } ?: "Pen ready"
        PenStep.TAPS -> if (taps == 0) step.title else if (tapBusy) "Reading tap $taps…" else "${3 - taps} more"
        else -> step.title
    }
    AnimatedContent(targetState = title, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "title") { h ->
        Text(h, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
    }
    val sub = note ?: step.hint
    if (sub.isNotEmpty() && !done) {
        Text(sub, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
    Spacer(Modifier.height(18.dp))

    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
        if (!done) {
            Box(
                Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        val s = pulse.value * (1f + 0.25f * burst.value)
                        scaleX = s; scaleY = s; alpha = 1f - burst.value
                    },
                contentAlignment = Alignment.Center,
            ) {
                GlowRing(Modifier.size(196.dp), energy = progress.value / 100f)
                Canvas(Modifier.fillMaxSize()) {
                    val w = 6.dp.toPx()
                    drawCircle(track, size.minDimension / 2 - w / 2, style = Stroke(w))
                    drawArc(
                        BrandSky,
                        -90f,
                        360f * (progress.value / 100f),
                        false,
                        Offset(w / 2, w / 2),
                        Size(size.width - w, size.height - w),
                        style = Stroke(w, cap = StrokeCap.Round),
                    )
                }
                AppSurface(
                    role = SurfaceRole.Control,
                    containerColor = Color(0xE6040B1A),
                    contentColor = Color.White,
                    shape = appPillShape(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                ) {
                    Text("$pct%", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInteropFilter { ev ->
                            if (ev.actionMasked != MotionEvent.ACTION_DOWN) return@pointerInteropFilter true
                            if (step != PenStep.TAPS) return@pointerInteropFilter true
                            if (!ev.isPen()) {
                                note = "That was a finger. Please use your pen."
                                return@pointerInteropFilter true
                            }
                            if (tapBusy) return@pointerInteropFilter true
                            readDevice(ev)
                            tapBusy = true
                            taps++
                            note = null
                            scope.launch {
                                pulse.snapTo(0.92f)
                                pulse.animateTo(1f, spring(dampingRatio = 0.4f))
                            }
                            scope.launch {
                                progress.animateTo(30f + taps * 10f, tween(750, easing = FastOutSlowInEasing))
                                delay(250)
                                tapBusy = false
                                if (taps >= 3) step = PenStep.SCAN
                            }
                            true
                        },
                )
            }
            if (step == PenStep.BURST || burst.value > 0f) Bubbles(Modifier.fillMaxSize(), burst.value)
        } else {
            Box(
                Modifier
                    .size(150.dp)
                    .graphicsLayer { scaleX = 0.4f + 0.6f * reveal.value; scaleY = scaleX; alpha = reveal.value.coerceIn(0f, 1f) }
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(BrandBlue, BrandSky, BrandPink))),
                contentAlignment = Alignment.Center,
            ) { ProIconView(ProIcon.PEN, Color.White, Modifier.size(66.dp)) }
        }
    }
    Spacer(Modifier.height(14.dp))
    if (done) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { alpha = reveal.value.coerceIn(0f, 1f) }) {
            device?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (caps.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    caps.forEach { c ->
                        AppPill(selected = true, tint = ProButtonPalette.Emerald, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                            Text(c, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (canSkip && (step == PenStep.HOVER || step == PenStep.BUTTON)) {
            OutlinedButton(
                onClick = {
                    scope.launch { progress.animateTo(if (step == PenStep.HOVER) 15f else 30f, tween(400)) }
                    step = if (step == PenStep.HOVER) PenStep.BUTTON else PenStep.TAPS
                },
                shape = CircleShape,
            ) { Text(if (step == PenStep.HOVER) "My pen can't hover" else "My pen has no button") }
        }
        if (done) {
            OutlinedButton(
                onClick = {
                    step = PenStep.HOVER; taps = 0; tapBusy = false; brand = null; device = null; caps = emptyList()
                    scope.launch { progress.snapTo(0f); burst.snapTo(0f); reveal.snapTo(0f) }
                },
                shape = CircleShape,
            ) { Text("Try again") }
        }
        if (step != PenStep.SCAN && step != PenStep.BURST) {
            AppMenuButton(
                contentPadding = PaddingValues(0.dp),
                items = PenBrands.manual.map { b ->
                    AppMenuItem(b, selected = brand == b) {
                        brand = b
                        store.registerPen(device ?: "Stylus", b)
                    }
                },
                contentDescription = "Choose pen brand",
                role = SurfaceRole.Control,
                shape = appPillShape(),
            ) {
                Text(
                    brand?.let { "Brand: $it" } ?: "Choose manually",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { store.update { it.copy(stylusOnly = false) }; onNext() }) {
            Text(if (isFinal) "Skip pen check & launch" else "I don't use a pen")
        }
        Button(
            onClick = onNext,
            shape = CircleShape,
            enabled = isFinal || done || brand != null,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPink, contentColor = Color.White),
            modifier = Modifier.elasticStretch(),
        ) {
            Text(if (isFinal) "Launch Pro Notes" else "Continue", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GlowRing(modifier: Modifier, energy: Float) {
    val t = rememberInfiniteTransition(label = "glow")
    val spinCW by t.animateFloat(0f, 360f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "spinCW")
    val spinCCW by t.animateFloat(360f, 0f, infiniteRepeatable(tween(5800, easing = LinearEasing)), label = "spinCCW")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val particles = remember {
        val rnd = kotlin.random.Random(7717)
        val list = ArrayList<FloatArray>(48)
        for (ring in 0 until 3) {
            val dir = if (ring % 2 == 0) 1f else -1f
            val rNorm = when (ring) { 0 -> 0.86f; 1 -> 0.72f; else -> 0.58f }
            val speed = 1f + ring * 0.28f
            for (i in 0 until 16) {
                list += floatArrayOf(
                    (i * 22.5f) + rnd.nextFloat() * 8f,
                    rNorm + (rnd.nextFloat() - 0.5f) * 0.04f,
                    dir,
                    speed,
                    0.5f + rnd.nextFloat() * 0.7f,
                    ring.toFloat(),
                )
            }
        }
        list
    }
    val ice = Color(0xFFE0F2FE)
    val deep = Color(0xFF0A2472)
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f
        val ringR = r * 0.80f
        val e = (0.65f + 0.35f * energy.coerceIn(0f, 1f))
        drawCircle(Color(0xFF02040A), r * 0.97f, c)
        drawCircle(
            Brush.radialGradient(listOf(BrandBlue.copy(alpha = 0.24f * e), BrandSky.copy(alpha = 0.14f * e), Color.Transparent), c, r),
            r, c,
        )
        rotate(spinCW, c) {
            drawCircle(
                Brush.sweepGradient(listOf(deep, BrandSky, BrandBlue, ice, BrandSky, deep), c),
                ringR, c, style = Stroke(r * (0.075f + 0.02f * pulse), cap = StrokeCap.Round),
            )
        }
        rotate(spinCCW, c) {
            drawCircle(
                Brush.sweepGradient(listOf(Color.Transparent, ice.copy(alpha = 0.85f * e), BrandSky.copy(alpha = 0.45f * e), Color.Transparent, BrandBlue.copy(alpha = 0.7f * e), Color.Transparent), c),
                ringR * 0.84f, c, style = Stroke(r * 0.032f, cap = StrokeCap.Round),
            )
        }
        particles.forEach { p ->
            val deg = p[0] + (if (p[2] > 0f) spinCW else spinCCW - 360f) * p[3]
            val rr = r * p[1]
            val col = when (p[5].toInt()) { 0 -> BrandBlue; 1 -> BrandSky; else -> ice }
            for (step in 3 downTo 0) {
                val radAngle = Math.toRadians((deg - p[2] * step * 6.5f).toDouble())
                val pos = Offset(c.x + cos(radAngle).toFloat() * rr, c.y + sin(radAngle).toFloat() * rr)
                val fade = (1f - step * 0.24f) * e
                if (step == 0) {
                    drawCircle(col.copy(alpha = 0.28f * fade), r * 0.042f * p[4], pos)
                    drawCircle(col.copy(alpha = 0.95f * fade), r * 0.017f * p[4], pos)
                } else {
                    drawCircle(col.copy(alpha = 0.35f * fade), r * 0.013f * p[4] * (1f - step * 0.2f), pos)
                }
            }
        }
    }
}

@Composable
private fun Bubbles(modifier: Modifier, t: Float) {
    val bubbles = remember {
        val rnd = kotlin.random.Random(11)
        List(26) { floatArrayOf(rnd.nextFloat() * 360f, 0.55f + rnd.nextFloat() * 0.7f, 5f + rnd.nextFloat() * 13f, rnd.nextFloat() * 0.25f) }
    }
    val colors = listOf(BrandBlue, BrandSky, BrandOrange, BrandPink, Color(0xFFF6D6FF))
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f
        drawCircle(BrandPink.copy(alpha = (0.45f * (1f - t)).coerceIn(0f, 1f)), r * (0.3f + 0.9f * t), c)
        bubbles.forEachIndexed { i, b ->
            val local = ((t - b[3]) / (1f - b[3])).coerceIn(0f, 1f)
            if (local <= 0f) return@forEachIndexed
            val a = Math.toRadians(b[0].toDouble())
            val dist = r * (0.6f + b[1] * local)
            val pos = Offset(c.x + cos(a).toFloat() * dist, c.y + sin(a).toFloat() * dist)
            val rad = b[2].dp.toPx() * (1f + 0.6f * local)
            val alpha = (1f - local).coerceIn(0f, 1f)
            val col = colors[i % colors.size]
            drawCircle(col.copy(alpha = 0.25f * alpha), rad, pos)
            drawCircle(col.copy(alpha = 0.9f * alpha), rad, pos, style = Stroke(1.5.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.7f * alpha), rad * 0.25f, Offset(pos.x - rad * 0.35f, pos.y - rad * 0.35f))
        }
    }
}

@Composable
private fun DemoArt(d: Demo, playing: Boolean, modifier: Modifier) {
    val t by rememberInfiniteTransition(label = "demo")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "t")
    val p = if (playing) t else 0.75f
    val ink = Color(0xFF2B2F3A)
    val blue = ProButtonPalette.ElectricBlue
    val orange = ProButtonPalette.VibrantOrange
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val u = minOf(w, h)
        fun seg(a: Float, b: Float) = ((p - a) / (b - a)).coerceIn(0f, 1f)
        fun wordAt(x: Float, y: Float, alpha: Float = 1f) {
            val path = Path()
            val ww = u * 0.42f
            path.moveTo(x - ww / 2, y)
            var k = 0
            var xx = x - ww / 2
            while (xx < x + ww / 2) {
                val nx = xx + u * 0.035f
                path.quadraticBezierTo((xx + nx) / 2, y + if (k % 2 == 0) -u * 0.05f else u * 0.03f, nx, y)
                xx = nx
                k++
            }
            drawPath(path, ink.copy(alpha = alpha), style = Stroke(u * 0.012f, cap = StrokeCap.Round))
        }
        fun penAt(x: Float, y: Float) {
            rotate(-35f, Offset(x, y)) {
                drawRoundRect(blue, Offset(x - u * 0.018f, y - u * 0.2f), Size(u * 0.036f, u * 0.18f), CornerRadius(u * 0.02f))
            }
            drawCircle(blue, u * 0.012f, Offset(x, y))
        }
        when (d) {
            Demo.LASSO -> {
                wordAt(cx, cy)
                val loop = seg(0.05f, 0.45f)
                val r = u * 0.3f
                val sweep = 340f * loop
                val dashed = p > 0.62f
                drawArc(
                    if (dashed) blue else ink, -70f, sweep, false, Offset(cx - r, cy - r * 0.65f), Size(r * 2, r * 1.3f),
                    style = Stroke(u * 0.01f, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(u * 0.03f, u * 0.02f), p * 200f) else null),
                )
                val endA = Math.toRadians((-70f + sweep).toDouble())
                val ex = cx + cos(endA).toFloat() * r
                val ey = cy + sin(endA).toFloat() * r * 0.65f
                if (dashed) drawRoundRect(blue.copy(alpha = 0.08f), Offset(cx - r, cy - r * 0.65f), Size(r * 2, r * 1.3f), CornerRadius(u * 0.05f))
                if (p < 0.62f) penAt(ex, ey)
            }
            Demo.SCRIBBLE -> {
                val gone = p > 0.6f
                if (!gone) wordAt(cx, cy) else wordAt(cx, cy, alpha = (1f - seg(0.6f, 0.75f)))
                val s = seg(0.15f, 0.58f)
                if (s > 0f && p < 0.75f) {
                    val path = Path()
                    val n = (12 * s).toInt().coerceAtLeast(1)
                    path.moveTo(cx - u * 0.25f, cy)
                    for (i in 1..n) path.lineTo(cx - u * 0.25f + i * u * 0.042f, cy + if (i % 2 == 0) u * 0.06f else -u * 0.06f)
                    drawPath(path, Color(0xFFE5484D).copy(alpha = 0.8f), style = Stroke(u * 0.01f, join = StrokeJoin.Round))
                    penAt(cx - u * 0.25f + n * u * 0.042f, cy)
                }
            }
            Demo.SNAP -> {
                val r = u * 0.26f
                val draw = seg(0.05f, 0.45f)
                val snapped = p > 0.62f
                if (!snapped) {
                    val path = Path()
                    val steps = (60 * draw).toInt()
                    for (i in 0..steps) {
                        val a = i / 60f * 2 * Math.PI
                        val wob = 1f + 0.08f * sin(a * 5).toFloat()
                        val x = cx + cos(a).toFloat() * r * wob
                        val y = cy + sin(a).toFloat() * r * wob
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, ink, style = Stroke(u * 0.012f, cap = StrokeCap.Round))
                } else {
                    drawCircle(ink, r, Offset(cx, cy), style = Stroke(u * 0.012f))
                }
            }
            Demo.KINDLE -> {
                val pw = u * 0.44f
                val ph = u * 0.58f
                val k = seg(0.15f, 0.75f)
                drawRoundRect(Color(0xFFF6EBD4), Offset(cx - pw * 0.9f, cy - ph * 0.5f), Size(pw * 1.8f, ph), CornerRadius(u * 0.03f))
                drawLine(Color(0xFF9C7E54), Offset(cx, cy - ph * 0.5f), Offset(cx, cy + ph * 0.5f), u * 0.012f)
                val curlX = cx + pw * 0.85f * (1f - 2f * k)
                drawRoundRect(Color(0xFFFFFDF7), Offset(cx.coerceAtMost(curlX), cy - ph * 0.46f), Size(kotlin.math.abs(curlX - cx).coerceAtLeast(u * 0.04f), ph * 0.92f), CornerRadius(u * 0.02f))
            }
            Demo.CARDS -> {
                drawRoundRect(Color.White, Offset(cx - u * 0.42f, cy - u * 0.3f), Size(u * 0.84f, u * 0.6f), CornerRadius(u * 0.03f))
                for (i in 0..3) drawLine(Color(0xFFB8C0CC), Offset(cx - u * 0.34f, cy - u * 0.2f + i * u * 0.1f), Offset(cx + u * 0.3f, cy - u * 0.2f + i * u * 0.1f), u * 0.01f)
                val fly = seg(0.5f, 0.85f)
                if (fly > 0f) {
                    val card = Offset(cx - u * 0.25f + u * 0.3f * fly, cy - u * 0.14f - u * 0.1f * sin(fly * Math.PI).toFloat())
                    rotate(-8f * fly, card) {
                        drawRoundRect(orange.copy(alpha = 0.25f), Offset(card.x - u * 0.16f, card.y - u * 0.1f), Size(u * 0.32f, u * 0.2f), CornerRadius(u * 0.03f))
                    }
                }
            }
            Demo.UNDO -> {
                wordAt(cx, cy - u * 0.05f, alpha = if (p in 0.35f..0.75f) 0.15f else 1f)
                val tap = p in 0.2f..0.35f
                listOf(-1f, 1f).forEach { s ->
                    val f = Offset(cx + s * u * 0.08f, cy + u * 0.18f)
                    drawCircle(Color(0xFFFFC9A8).copy(alpha = if (tap) 1f else 0.5f), u * 0.045f, f)
                }
            }
        }
    }
}
