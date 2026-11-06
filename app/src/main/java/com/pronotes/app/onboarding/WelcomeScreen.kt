package com.pronotes.app.onboarding

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.SurfaceTexture
import android.view.InputDevice
import android.media.MediaPlayer
import android.net.Uri
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewOutlineProvider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.pronotes.app.R
import com.pronotes.app.data.PenBrands
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

private val Brand1 = Color(0xFF7161E0)
private val Brand2 = Color(0xFF2A78D6)
private val Brand3 = Color(0xFFD55181)

/** First-run welcome. Full screen: system bars hidden, animated background, video in a circle. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val store = remember { SettingsStore.get(context) }
    val pager = rememberPagerState { 4 }
    val scope = rememberCoroutineScope()
    fun next() = scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(3)) }
    fun finish() { store.update { it.copy(onboardingDone = true) }; onDone() }

    // Immersive: hide the status and navigation bars while the welcome is on screen.
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AuroraBackground(pager.currentPage)
        HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = pager.currentPage != 1 && pager.currentPage != 2) { page ->
            // The pager reports the page being settled; only animate the visible one.
            val active = pager.currentPage == page
            Box(Modifier.fillMaxSize()) {
                // The gradient video fills the whole first page, edge to edge.
                if (page == 0) IntroBackdrop()
                Column(
                    Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 28.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    when (page) {
                        0 -> IntroPage(active, onStart = { next() })
                        1 -> PenCheckPage(active, store, onNext = { next() })
                        2 -> TourPage(active, onNext = { next() })
                        else -> ReadyPage(active, onDone = { finish() })
                    }
                }
            }
        }
        val onVideo = pager.currentPage == 0
        Row(
            Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(4) { i ->
                val w by animateFloatAsState(if (pager.currentPage == i) 26f else 8f, spring(dampingRatio = 0.7f), label = "dot")
                Box(
                    Modifier.size(width = w.dp, height = 8.dp).clip(CircleShape)
                        .background(
                            if (pager.currentPage == i) (if (onVideo) Color.White else MaterialTheme.colorScheme.primary)
                            else (if (onVideo) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.25f),
                        ),
                )
            }
        }
        TextButton(
            onClick = { finish() }, modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp),
            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = if (onVideo) Color.White else MaterialTheme.colorScheme.primary),
        ) { Text("Skip") }
    }
}

// ---------------------------------------------------------------- background

/** Slowly drifting colour blobs; the palette shifts as you move through the pages. */
@Composable
private fun AuroraBackground(page: Int) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val palettes = listOf(
        listOf(Brand1, Brand2, Brand3),
        listOf(Brand2, Color(0xFF1BAF7A), Brand1),
        listOf(Brand3, Color(0xFFEB6834), Brand1),
        listOf(Color(0xFF1BAF7A), Brand2, Color(0xFFFAB219)),
    )
    val p = palettes[page.coerceIn(0, 3)]
    val c0 by animateColorAsState(p[0], tween(1000), label = "c0")
    val c1 by animateColorAsState(p[1], tween(1000), label = "c1")
    val c2 by animateColorAsState(p[2], tween(1000), label = "c2")
    val phase by rememberInfiniteTransition(label = "aurora")
        .animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "phase")
    val a = if (dark) 0.5f else 0.38f
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        fun blob(color: Color, cx: Float, cy: Float, r: Float) =
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = a), Color.Transparent), center = Offset(cx, cy), radius = r), r, Offset(cx, cy))
        blob(c0, w * (0.22f + 0.12f * sin(phase)), h * (0.22f + 0.10f * cos(phase * 0.8f)), min(w, h) * 0.95f)
        blob(c1, w * (0.82f + 0.10f * cos(phase * 1.1f)), h * (0.34f + 0.12f * sin(phase * 0.9f)), min(w, h) * 0.85f)
        blob(c2, w * (0.5f + 0.16f * sin(phase * 0.7f)), h * (0.88f + 0.06f * cos(phase)), min(w, h) * 1.0f)
    }
}

// ---------------------------------------------------------------- typography

/** Words rise and fade in one after another. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimatedWords(
    text: String, style: TextStyle, active: Boolean, modifier: Modifier = Modifier,
    startDelay: Long = 0, gradient: List<Color>? = null,
) {
    val words = remember(text) { text.split(' ') }
    FlowRow(
        modifier, horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        words.forEachIndexed { i, word ->
            val a = remember(text) { Animatable(0f) }
            LaunchedEffect(text, active) {
                if (active) { a.snapTo(0f); delay(startDelay + i * 110L); a.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 130f)) }
            }
            Text(
                word,
                style = if (gradient != null) style.copy(brush = Brush.linearGradient(gradient)) else style,
                modifier = Modifier.graphicsLayer {
                    alpha = a.value.coerceIn(0f, 1f)
                    translationY = (1f - a.value) * 46.dp.toPx()
                    val s = 0.88f + 0.12f * a.value
                    scaleX = s; scaleY = s
                },
            )
        }
    }
}

/** A gradient that slides across the text. */
@Composable
private fun ShimmerText(text: String, style: TextStyle, colors: List<Color>, modifier: Modifier = Modifier) {
    val x by rememberInfiniteTransition(label = "shimmer")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Reverse), label = "x")
    Text(
        text, modifier = modifier,
        style = style.copy(brush = Brush.linearGradient(colors, Offset(x * 700f, 0f), Offset(x * 700f + 520f, 160f), TileMode.Mirror)),
    )
}

/** Types the text out letter by letter. */
@Composable
private fun Typewriter(text: String, style: TextStyle, active: Boolean, modifier: Modifier = Modifier, startDelay: Long = 700) {
    var n by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text, active) {
        if (!active) return@LaunchedEffect
        n = 0; delay(startDelay)
        while (n < text.length) { n++; delay(28) }
    }
    Box(modifier) {
        Text(text, style = style, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = 0f })   // reserves the space
        Text(text.take(n) + if (n in 1 until text.length) "▍" else "", style = style, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

// ---------------------------------------------------------------------- video

/** A bundled video, looping and silent, cropped to fill its box (or a circle when [circle] is set). */
@Composable
private fun LoopingVideo(@androidx.annotation.RawRes res: Int, circle: Boolean, modifier: Modifier) {
    val player = remember { arrayOfNulls<MediaPlayer>(1) }
    DisposableEffect(Unit) { onDispose { player[0]?.release(); player[0] = null } }
    AndroidView(
        modifier = if (circle) modifier.clip(CircleShape) else modifier,
        factory = { ctx ->
            val tv = TextureView(ctx)
            tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                    runCatching {
                        val mp = MediaPlayer()
                        player[0] = mp
                        mp.setDataSource(ctx, Uri.parse("android.resource://${ctx.packageName}/$res"))
                        mp.setSurface(Surface(st))
                        mp.isLooping = true
                        mp.setVolume(0f, 0f)
                        mp.setOnVideoSizeChangedListener { _, vw, vh -> cropToFill(tv, vw, vh) }
                        mp.setOnPreparedListener { it.start() }
                        mp.prepareAsync()
                    }
                }
                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { player[0]?.release(); player[0] = null; return true }
                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
            }
            tv
        },
    )
}

/**
 * Glowing purple ring for the pen check, drawn in code so it always shows: soft haze, a turning
 * gradient band, a bright streak and drifting sparks. It glows brighter as the scan goes on.
 */
@Composable
private fun GlowRing(modifier: Modifier, energy: Float) {
    val t = rememberInfiniteTransition(label = "glow")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "spin")
    val spin2 by t.animateFloat(360f, 0f, infiniteRepeatable(tween(9500, easing = LinearEasing)), label = "spin2")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val sparks = remember {
        val rnd = kotlin.random.Random(7)
        List(40) { floatArrayOf(rnd.nextFloat() * 360f, rnd.nextFloat(), 0.4f + rnd.nextFloat() * 0.6f) }
    }
    val purple = Color(0xFFB45CFF); val pink = Color(0xFFE38BFF); val deep = Color(0xFF5A1AB0); val light = Color(0xFFF6D6FF)
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f
        val ringR = r * 0.80f
        val e = (0.55f + 0.45f * energy.coerceIn(0f, 1f))
        drawCircle(Color.Black, r * 0.97f, c)
        drawCircle(
            Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color(0xFF7A2BD9).copy(alpha = 0.45f * e), Color.Transparent), c, r),
            r, c,
        )
        for (k in 4 downTo 1) drawCircle(purple.copy(alpha = 0.09f * e), ringR, c, style = Stroke(r * 0.055f * k))
        rotate(spin, c) {
            drawCircle(
                Brush.sweepGradient(listOf(deep, pink, purple, deep, light, purple, deep), c),
                ringR, c, style = Stroke(r * (0.10f + 0.025f * pulse)),
            )
        }
        rotate(spin2, c) {
            drawCircle(
                Brush.sweepGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.6f * e), Color.Transparent, Color.Transparent, pink.copy(alpha = 0.5f * e), Color.Transparent), c),
                ringR * 1.02f, c, style = Stroke(r * 0.025f),
            )
        }
        sparks.forEach { s ->
            val a = Math.toRadians((s[0] + spin * s[2]).toDouble())
            val rr = ringR + (s[1] - 0.5f) * r * 0.24f
            drawCircle(
                Color.White.copy(alpha = ((0.2f + 0.6f * s[1]) * e).coerceAtMost(1f)), r * 0.012f * s[2],
                Offset(c.x + kotlin.math.cos(a).toFloat() * rr, c.y + kotlin.math.sin(a).toFloat() * rr),
            )
        }
    }
}

/** TextureView stretches video to its size; scale back so the picture fills without distortion. */
private fun cropToFill(tv: TextureView, vw: Int, vh: Int) {
    if (vw == 0 || vh == 0 || tv.width == 0 || tv.height == 0) return
    val videoAspect = vw.toFloat() / vh
    val viewAspect = tv.width.toFloat() / tv.height
    val m = Matrix()
    if (videoAspect > viewAspect) m.setScale(videoAspect / viewAspect, 1f, tv.width / 2f, tv.height / 2f)
    else m.setScale(1f, viewAspect / videoAspect, tv.width / 2f, tv.height / 2f)
    tv.setTransform(m)
}

// ---------------------------------------------------------------------- pages

@Composable
private fun IntroPage(active: Boolean, onStart: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val d = minOf(maxWidth * 0.62f, maxHeight * 0.42f).coerceIn(180.dp, 400.dp)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            GlowingCircle(d, active)
            Spacer(Modifier.height(34.dp))
            Text(
                "WELCOME TO", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 6.sp),
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(6.dp))
            ShimmerText(
                "Pro Notes",
                TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp),
                listOf(Color.White, Color(0xFFB9C8FF), Color.White, Color(0xFFE5C8FF)),
            )
            Spacer(Modifier.height(6.dp))
            AnimatedWords(
                "Write. Think. Create.",
                TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Light, letterSpacing = 1.sp, color = Color.White),
                active, startDelay = 500,
            )
            Spacer(Modifier.height(12.dp))
            Typewriter(
                "Handwriting, PDFs and ideas in one place, built for your pen.",
                MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.85f)),
                active, Modifier.widthIn(max = 460.dp),
            )
            Spacer(Modifier.height(30.dp))
            val pulse by rememberInfiniteTransition(label = "cta")
                .animateFloat(1f, 1.05f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
            Button(onClick = onStart, shape = CircleShape, modifier = Modifier.widthIn(min = 240.dp).height(54.dp).scale(pulse)) {
                Text("Get started", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Video in a circle with a rotating gradient ring and soft pulsing halos. */
@Composable
private fun GlowingCircle(diameter: androidx.compose.ui.unit.Dp, active: Boolean) {
    val t = rememberInfiniteTransition(label = "ring")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "spin")
    val breathe by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val enter = remember { Animatable(0f) }
    LaunchedEffect(active) { if (active) { enter.snapTo(0f); enter.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 90f)) } }
    val pad = 26.dp
    Box(Modifier.size(diameter + pad * 2).graphicsLayer { scaleX = 0.5f + 0.5f * enter.value; scaleY = scaleX; alpha = enter.value.coerceIn(0f, 1f) }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = diameter.toPx() / 2f
            val c = center
            // soft halos
            for (k in 1..3) {
                val rr = r + pad.toPx() * (0.35f * k) * (0.7f + 0.3f * breathe)
                drawCircle(Brand1.copy(alpha = 0.10f / k), rr, c)
            }
            // rotating gradient ring
            rotate(spin) {
                drawCircle(
                    Brush.sweepGradient(listOf(Brand2, Brand1, Brand3, Color(0xFFFAB219), Brand2), c),
                    r + 8.dp.toPx(), c, style = Stroke(5.dp.toPx()),
                )
            }
        }
        Box(Modifier.size(diameter).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Image(
                painterResource(R.drawable.ic_launcher_foreground), null,
                Modifier.fillMaxSize().scale(1.5f),
            )
        }
    }
}

/** Full-screen gradient video behind the first page, with a scrim so the text stays readable. */
@Composable
private fun IntroBackdrop() {
    Box(Modifier.fillMaxSize()) {
        LoopingVideo(R.raw.welcome, circle = false, modifier = Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0x66000000), Color(0x22000000), Color(0x88000000))),
            ),
        )
    }
}

/** Steps of the pen check, in order. */
private enum class PenStep(val title: String, val hint: String) {
    HOVER("Bring your pen close", "Hold the pen tip just above the ring without touching."),
    BUTTON("Press and hold the button", "Keep the side button pressed while the pen hovers."),
    TAPS("Tap the ring 3 times", "Tap gently with the pen tip, one tap at a time."),
    SCAN("Scanning your pen", ""),
    BURST("", ""),
    DONE("", ""),
}

/** Watches every pen event in the window (hover and button presses never reach a normal touch handler). */
private class PenSpy(private val base: android.view.Window.Callback, private val onEvent: (MotionEvent) -> Unit) :
    android.view.Window.Callback by base {
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean { onEvent(event); return base.dispatchGenericMotionEvent(event) }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean { onEvent(event); return base.dispatchTouchEvent(event) }
}

private fun MotionEvent.isPen(): Boolean {
    val t = getToolType(0)
    return t == MotionEvent.TOOL_TYPE_STYLUS || t == MotionEvent.TOOL_TYPE_ERASER || isFromSource(InputDevice.SOURCE_STYLUS)
}

private fun MotionEvent.penButton(): Boolean =
    buttonState and (MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_STYLUS_SECONDARY or MotionEvent.BUTTON_SECONDARY or MotionEvent.BUTTON_TERTIARY) != 0

/**
 * Pen check in steps: hover close, hold the button, three paced taps, a short scan, then a bubble
 * burst reveals the pen. Each step waits a moment before it counts, so nothing feels instant.
 * Steps the pen cannot do (no hover, no button) can be skipped after a few seconds.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
private fun PenCheckPage(active: Boolean, store: SettingsStore, onNext: () -> Unit) {
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
    var manual by remember { mutableStateOf(false) }
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

    // Window-level spy for hover and the pen button, only while this page is on screen.
    DisposableEffect(active) {
        val window = context.findActivity()?.window
        val original = window?.callback
        if (active && window != null && original != null) {
            window.callback = PenSpy(original) { ev ->
                if (!ev.isPen()) return@PenSpy
                when (ev.actionMasked) {
                    MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> { hovering = true; buttonDown = ev.penButton(); readDevice(ev) }
                    MotionEvent.ACTION_HOVER_EXIT -> { hovering = false; buttonDown = false }
                    MotionEvent.ACTION_BUTTON_PRESS -> buttonDown = true
                    MotionEvent.ACTION_BUTTON_RELEASE -> buttonDown = false
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> { buttonDown = ev.penButton(); readDevice(ev) }
                    MotionEvent.ACTION_UP -> buttonDown = false
                }
            }
        }
        onDispose { if (window != null && original != null && window.callback is PenSpy) window.callback = original }
    }

    // Offer "skip" when a step takes a while (some pens cannot hover or have no button).
    LaunchedEffect(step) {
        canSkip = false; note = null
        if (step == PenStep.HOVER || step == PenStep.BUTTON) { delay(6000); canSkip = true }
    }

    // Step 1: the pen must hover for a moment before it counts.
    LaunchedEffect(step, hovering) {
        if (step != PenStep.HOVER) return@LaunchedEffect
        if (!hovering) { progress.animateTo(0f, tween(300)); return@LaunchedEffect }
        note = "Pen detected. Keep it there…"
        progress.animateTo(15f, tween(1400, easing = LinearEasing))
        if (hovering) { pulse.snapTo(0.94f); pulse.animateTo(1f, spring(dampingRatio = 0.4f)); delay(250); step = PenStep.BUTTON }
    }

    // Step 2: the button must stay held for about a second.
    LaunchedEffect(step, buttonDown) {
        if (step != PenStep.BUTTON) return@LaunchedEffect
        if (!buttonDown) { if (progress.value > 15f) progress.animateTo(15f, tween(300)); return@LaunchedEffect }
        note = "Button pressed. Keep holding…"
        progress.animateTo(30f, tween(1200, easing = LinearEasing))
        if (buttonDown) { pulse.snapTo(0.94f); pulse.animateTo(1f, spring(dampingRatio = 0.4f)); delay(250); step = PenStep.TAPS }
    }

    // Step 4 and 5: scan, then the bubbles burst and the pen is revealed.
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

    AnimatedWords(
        "Meet your pen", TextStyle(fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, color = MaterialTheme.colorScheme.onBackground),
        active, gradient = listOf(Brand2, Brand1, Brand3),
    )
    Spacer(Modifier.height(10.dp))
    // Step dots: hover, button, taps, scan.
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(PenStep.HOVER, PenStep.BUTTON, PenStep.TAPS, PenStep.SCAN).forEach { s ->
            val reached = step.ordinal >= s.ordinal
            val w by animateFloatAsState(if (step == s) 22f else 8f, spring(dampingRatio = 0.7f), label = "sd")
            Box(Modifier.size(width = w.dp, height = 8.dp).clip(CircleShape).background(if (reached) Color(0xFFB45CFF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)))
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
    Spacer(Modifier.height(22.dp))

    val primary = Color(0xFFB45CFF)
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        if (!done) {
            Box(
                Modifier.size(230.dp).graphicsLayer {
                    val s = pulse.value * (1f + 0.25f * burst.value)
                    scaleX = s; scaleY = s; alpha = 1f - burst.value
                },
                contentAlignment = Alignment.Center,
            ) {
                GlowRing(Modifier.size(206.dp), energy = progress.value / 100f)
                Canvas(Modifier.fillMaxSize()) {
                    val w = 6.dp.toPx()
                    drawCircle(track, size.minDimension / 2 - w / 2, style = Stroke(w))
                    drawArc(
                        primary, -90f, 360f * (progress.value / 100f), false, Offset(w / 2, w / 2),
                        androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                        style = Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                }
                Surface(shape = CircleShape, color = Color(0xE6150A24), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66E38BFF))) {
                    Text("$pct%", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
                }
                // Taps on the ring, paced: each one is "read" before the next counts.
                Box(
                    Modifier.fillMaxSize().pointerInteropFilter { ev ->
                        if (ev.actionMasked != MotionEvent.ACTION_DOWN) return@pointerInteropFilter true
                        if (step != PenStep.TAPS) return@pointerInteropFilter true
                        if (!ev.isPen()) { note = "That was a finger. Please use your pen."; return@pointerInteropFilter true }
                        if (tapBusy) return@pointerInteropFilter true
                        readDevice(ev)
                        tapBusy = true
                        taps++
                        note = null
                        scope.launch {
                            pulse.snapTo(0.92f); pulse.animateTo(1f, spring(dampingRatio = 0.4f))
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
            // The pen, revealed.
            Box(
                Modifier.size(150.dp).graphicsLayer { scaleX = 0.4f + 0.6f * reveal.value; scaleY = scaleX; alpha = reveal.value.coerceIn(0f, 1f) }
                    .clip(CircleShape).background(Brush.linearGradient(listOf(Brand2, Brand1, Color(0xFFB45CFF)))),
                contentAlignment = Alignment.Center,
            ) { ProIconView(ProIcon.PEN, Color.White, Modifier.size(70.dp)) }
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
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(c, Modifier.padding(horizontal = 14.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (canSkip && (step == PenStep.HOVER || step == PenStep.BUTTON)) {
            OutlinedButton(onClick = {
                scope.launch { progress.animateTo(if (step == PenStep.HOVER) 15f else 30f, tween(400)) }
                step = if (step == PenStep.HOVER) PenStep.BUTTON else PenStep.TAPS
            }, shape = CircleShape) { Text(if (step == PenStep.HOVER) "My pen can't hover" else "My pen has no button") }
        }
        if (done) {
            OutlinedButton(onClick = {
                step = PenStep.HOVER; taps = 0; tapBusy = false; brand = null; device = null; caps = emptyList()
                scope.launch { progress.snapTo(0f); burst.snapTo(0f); reveal.snapTo(0f) }
            }, shape = CircleShape) { Text("Try again") }
        }
        Box {
            OutlinedButton(onClick = { manual = true }, enabled = step != PenStep.SCAN && step != PenStep.BURST, shape = CircleShape) {
                Text(brand?.let { "Brand: $it" } ?: "Choose manually")
            }
            DropdownMenu(manual, { manual = false }, shape = RoundedCornerShape(22.dp)) {
                PenBrands.manual.forEach { b ->
                    DropdownMenuItem(text = { Text(b) }, onClick = {
                        manual = false; brand = b
                        store.registerPen(device ?: "Stylus", b)
                    })
                }
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { store.update { it.copy(stylusOnly = false) }; onNext() }) { Text("I don't use a pen") }
        Button(onClick = onNext, shape = CircleShape, enabled = done || brand != null) { Text("Continue") }
    }
}

/** Bubbles fly out of the ring and pop. [t] runs from 0 to 1. */
@Composable
private fun Bubbles(modifier: Modifier, t: Float) {
    val bubbles = remember {
        val rnd = kotlin.random.Random(11)
        List(26) { floatArrayOf(rnd.nextFloat() * 360f, 0.55f + rnd.nextFloat() * 0.7f, 5f + rnd.nextFloat() * 13f, rnd.nextFloat() * 0.25f) }
    }
    val colors = listOf(Color(0xFFB45CFF), Color(0xFFE38BFF), Color(0xFF7161E0), Color(0xFF2A78D6), Color(0xFFF6D6FF))
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f
        // A soft flash at the centre as the ring pops.
        drawCircle(Color(0xFFE38BFF).copy(alpha = (0.5f * (1f - t)).coerceIn(0f, 1f)), r * (0.3f + 0.9f * t), c)
        bubbles.forEachIndexed { i, b ->
            val local = ((t - b[3]) / (1f - b[3])).coerceIn(0f, 1f)
            if (local <= 0f) return@forEachIndexed
            val a = Math.toRadians(b[0].toDouble())
            val dist = r * (0.6f + b[1] * local)
            val pos = Offset(c.x + kotlin.math.cos(a).toFloat() * dist, c.y + kotlin.math.sin(a).toFloat() * dist)
            val rad = b[2].dp.toPx() * (1f + 0.6f * local)
            val alpha = (1f - local).coerceIn(0f, 1f)
            val col = colors[i % colors.size]
            drawCircle(col.copy(alpha = 0.25f * alpha), rad, pos)
            drawCircle(col.copy(alpha = 0.9f * alpha), rad, pos, style = Stroke(1.5.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.7f * alpha), rad * 0.25f, Offset(pos.x - rad * 0.35f, pos.y - rad * 0.35f))
        }
    }
}

/** One tip in the tour, with a looping drawing that shows the gesture. */
private enum class Demo(val title: String, val body: String) {
    LASSO("Circle to select", "Draw a loop around ink, lift, then press and hold the blue dot at its end."),
    SCRIBBLE("Scribble to erase", "Zig-zag over ink to delete it. The pen's side button erases too."),
    SNAP("Hold to snap", "Hold the pen still at the end of a stroke to turn it into a clean shape."),
    LINK("Link anything", "Select ink, tap the link button, and pick a page or a website."),
    CARDS("Flashcards", "Drag a box over a PDF to make a card, then study with spaced review."),
    UNDO("Two-finger tap", "Tap with two fingers to undo, three to redo. Double-tap to zoom."),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TourPage(active: Boolean, onNext: () -> Unit) {
    val demos = Demo.entries
    val inner = rememberPagerState { demos.size }
    val scope = rememberCoroutineScope()
    AnimatedWords(
        "Little things, big difference", TextStyle(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, color = MaterialTheme.colorScheme.onBackground),
        active, gradient = listOf(Brand3, Brand1, Brand2),
    )
    Spacer(Modifier.height(18.dp))
    HorizontalPager(inner, Modifier.widthIn(max = 560.dp).fillMaxWidth().height(380.dp), pageSpacing = 16.dp) { i ->
        val d = demos[i]
        val offset = (inner.currentPage - i) + inner.currentPageOffsetFraction
        Surface(
            shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), shadowElevation = 8.dp,
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).graphicsLayer {
                val k = 1f - 0.08f * kotlin.math.abs(offset).coerceAtMost(1f)
                scaleX = k; scaleY = k; alpha = 1f - 0.4f * kotlin.math.abs(offset).coerceAtMost(1f)
            }.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(32.dp)),
        ) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFF4F1FF), Color(0xFFEAF2FF)))),
                    contentAlignment = Alignment.Center,
                ) { DemoArt(d, active && inner.currentPage == i, Modifier.fillMaxSize().padding(18.dp)) }
                Spacer(Modifier.height(16.dp))
                Text(d.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(d.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(demos.size) { i ->
            val w by animateFloatAsState(if (inner.currentPage == i) 22f else 7f, spring(dampingRatio = 0.7f), label = "td")
            Box(
                Modifier.size(width = w.dp, height = 7.dp).clip(CircleShape)
                    .background(if (inner.currentPage == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    .clickable { scope.launch { inner.animateScrollToPage(i) } },
            )
        }
    }
    Spacer(Modifier.height(18.dp))
    val last = inner.currentPage == demos.size - 1
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!last) TextButton(onClick = onNext) { Text("Skip tips") }
        Button(
            onClick = { if (last) onNext() else scope.launch { inner.animateScrollToPage(inner.currentPage + 1) } },
            shape = CircleShape, modifier = Modifier.widthIn(min = 180.dp),
        ) { Text(if (last) "Continue" else "Next tip") }
    }
}

/** Small looping drawings: a pen moves, ink appears, and the result plays out. */
@Composable
private fun DemoArt(d: Demo, playing: Boolean, modifier: Modifier) {
    val t by rememberInfiniteTransition(label = "demo")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "t")
    val p = if (playing) t else 0.75f
    val ink = Color(0xFF2B2F3A); val blue = Color(0xFF3D6CF4); val purple = Color(0xFF7161E0)
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f; val cy = h / 2f
        val u = minOf(w, h)
        fun seg(a: Float, b: Float) = ((p - a) / (b - a)).coerceIn(0f, 1f)
        fun wordAt(x: Float, y: Float, alpha: Float = 1f, len: Float = 1f) {
            // A handwritten-looking word: a few joined waves.
            val path = androidx.compose.ui.graphics.Path()
            val ww = u * 0.42f * len
            path.moveTo(x - ww / 2, y)
            var k = 0
            var xx = x - ww / 2
            while (xx < x + ww / 2) {
                val nx = xx + u * 0.035f
                path.quadraticBezierTo((xx + nx) / 2, y + if (k % 2 == 0) -u * 0.05f else u * 0.03f, nx, y)
                xx = nx; k++
            }
            drawPath(path, ink.copy(alpha = alpha), style = Stroke(u * 0.012f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        fun penAt(x: Float, y: Float, alpha: Float = 1f) {
            rotate(-35f, Offset(x, y)) {
                drawRoundRect(purple.copy(alpha = alpha), Offset(x - u * 0.018f, y - u * 0.2f), androidx.compose.ui.geometry.Size(u * 0.036f, u * 0.18f), androidx.compose.ui.geometry.CornerRadius(u * 0.02f))
            }
            drawCircle(purple.copy(alpha = alpha), u * 0.012f, Offset(x, y))
        }
        when (d) {
            Demo.LASSO -> {
                wordAt(cx, cy)
                val loop = seg(0.05f, 0.45f)
                val r = u * 0.3f
                val sweep = 340f * loop
                val dashed = p > 0.62f
                drawArc(
                    if (dashed) blue else ink, -70f, sweep, false, Offset(cx - r, cy - r * 0.65f), androidx.compose.ui.geometry.Size(r * 2, r * 1.3f),
                    style = Stroke(u * 0.01f, pathEffect = if (dashed) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(u * 0.03f, u * 0.02f), p * 200f) else null),
                )
                val endA = Math.toRadians((-70f + sweep).toDouble())
                val ex = cx + kotlin.math.cos(endA).toFloat() * r; val ey = cy + kotlin.math.sin(endA).toFloat() * r * 0.65f
                if (p in 0.45f..0.62f) {
                    val k = seg(0.45f, 0.62f)
                    drawCircle(blue.copy(alpha = 1f - k), u * (0.03f + 0.05f * k), Offset(ex, ey), style = Stroke(u * 0.008f))
                }
                if (dashed) drawRoundRect(blue.copy(alpha = 0.08f), Offset(cx - r, cy - r * 0.65f), androidx.compose.ui.geometry.Size(r * 2, r * 1.3f), androidx.compose.ui.geometry.CornerRadius(u * 0.05f))
                if (p < 0.62f) penAt(ex, ey)
            }
            Demo.SCRIBBLE -> {
                val gone = p > 0.6f
                if (!gone) wordAt(cx, cy) else wordAt(cx, cy, alpha = (1f - seg(0.6f, 0.75f)))
                val s = seg(0.15f, 0.58f)
                if (s > 0f && p < 0.75f) {
                    val path = androidx.compose.ui.graphics.Path()
                    val n = (12 * s).toInt().coerceAtLeast(1)
                    path.moveTo(cx - u * 0.25f, cy)
                    for (i in 1..n) path.lineTo(cx - u * 0.25f + i * u * 0.042f, cy + if (i % 2 == 0) u * 0.06f else -u * 0.06f)
                    drawPath(path, Color(0xFFE5484D).copy(alpha = 0.8f), style = Stroke(u * 0.01f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                    penAt(cx - u * 0.25f + n * u * 0.042f, cy)
                }
            }
            Demo.SNAP -> {
                val r = u * 0.26f
                val draw = seg(0.05f, 0.45f)
                val snapped = p > 0.62f
                if (!snapped) {
                    val path = androidx.compose.ui.graphics.Path()
                    val steps = (60 * draw).toInt()
                    for (i in 0..steps) {
                        val a = i / 60f * 2 * Math.PI
                        val wob = 1f + 0.08f * kotlin.math.sin(a * 5).toFloat()
                        val x = cx + kotlin.math.cos(a).toFloat() * r * wob; val y = cy + kotlin.math.sin(a).toFloat() * r * wob
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, ink, style = Stroke(u * 0.012f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    val a = draw * 2 * Math.PI
                    penAt(cx + kotlin.math.cos(a).toFloat() * r, cy + kotlin.math.sin(a).toFloat() * r)
                    if (p in 0.45f..0.62f) {
                        val k = seg(0.45f, 0.62f)
                        drawCircle(blue.copy(alpha = 0.6f * (1f - k)), u * 0.06f * (1f + k), Offset(cx + r, cy), style = Stroke(u * 0.008f))
                    }
                } else {
                    val k = seg(0.62f, 0.72f)
                    drawCircle(blue.copy(alpha = 0.15f * (1f - k)), r * (1.1f + 0.2f * k), Offset(cx, cy))
                    drawCircle(ink, r, Offset(cx, cy), style = Stroke(u * 0.012f))
                }
            }
            Demo.LINK -> {
                val top = cy - u * 0.18f; val bottom = cy + u * 0.22f
                drawRoundRect(Color.White, Offset(cx - u * 0.45f, top - u * 0.14f), androidx.compose.ui.geometry.Size(u * 0.4f, u * 0.5f), androidx.compose.ui.geometry.CornerRadius(u * 0.03f))
                drawRoundRect(Color.White, Offset(cx + u * 0.05f, top - u * 0.14f), androidx.compose.ui.geometry.Size(u * 0.4f, u * 0.5f), androidx.compose.ui.geometry.CornerRadius(u * 0.03f))
                val lx = cx - u * 0.25f
                drawLine(ink, Offset(lx - u * 0.12f, top), Offset(lx + u * 0.12f, top), u * 0.012f)
                val linked = p > 0.3f
                if (linked) drawRoundRect(blue.copy(alpha = 0.12f), Offset(lx - u * 0.15f, top - u * 0.04f), androidx.compose.ui.geometry.Size(u * 0.3f, u * 0.08f), androidx.compose.ui.geometry.CornerRadius(u * 0.02f))
                val k = seg(0.4f, 0.8f)
                if (k > 0f) {
                    val from = Offset(lx + u * 0.12f, top); val to = Offset(cx + u * 0.25f, bottom - u * 0.1f)
                    val now = Offset(from.x + (to.x - from.x) * k, from.y + (to.y - from.y) * k - kotlin.math.sin(k * Math.PI).toFloat() * u * 0.12f)
                    drawLine(blue.copy(alpha = 0.5f), from, now, u * 0.008f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(u * 0.02f, u * 0.015f)))
                    drawCircle(blue, u * 0.03f, now)
                }
                if (p > 0.8f) drawCircle(blue.copy(alpha = 1f - seg(0.8f, 1f)), u * (0.04f + 0.08f * seg(0.8f, 1f)), Offset(cx + u * 0.25f, bottom - u * 0.1f), style = Stroke(u * 0.008f))
                drawCircle(blue, u * 0.03f, Offset(lx + u * 0.15f, top - u * 0.04f))
            }
            Demo.CARDS -> {
                drawRoundRect(Color.White, Offset(cx - u * 0.42f, cy - u * 0.3f), androidx.compose.ui.geometry.Size(u * 0.84f, u * 0.6f), androidx.compose.ui.geometry.CornerRadius(u * 0.03f))
                for (i in 0..3) drawLine(Color(0xFFB8C0CC), Offset(cx - u * 0.34f, cy - u * 0.2f + i * u * 0.1f), Offset(cx + u * 0.3f, cy - u * 0.2f + i * u * 0.1f), u * 0.01f)
                val box = seg(0.05f, 0.4f)
                drawRect(
                    blue, Offset(cx - u * 0.36f, cy - u * 0.24f), androidx.compose.ui.geometry.Size(u * 0.5f * box, u * 0.2f * box),
                    style = Stroke(u * 0.008f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(u * 0.02f, u * 0.015f))),
                )
                val fly = seg(0.5f, 0.85f)
                if (fly > 0f) {
                    val card = Offset(cx - u * 0.25f + u * 0.3f * fly, cy - u * 0.14f - u * 0.1f * kotlin.math.sin(fly * Math.PI).toFloat())
                    rotate(-8f * fly, card) {
                        drawRoundRect(Color(0xFFE9E3FF), Offset(card.x - u * 0.16f, card.y - u * 0.1f), androidx.compose.ui.geometry.Size(u * 0.32f, u * 0.2f), androidx.compose.ui.geometry.CornerRadius(u * 0.03f))
                        drawRoundRect(purple, Offset(card.x - u * 0.16f, card.y - u * 0.1f), androidx.compose.ui.geometry.Size(u * 0.32f, u * 0.2f), androidx.compose.ui.geometry.CornerRadius(u * 0.03f), style = Stroke(u * 0.006f))
                    }
                }
            }
            Demo.UNDO -> {
                wordAt(cx, cy - u * 0.05f, alpha = if (p in 0.35f..0.75f) 0.15f else 1f)
                val tap = p in 0.2f..0.35f
                listOf(-1f, 1f).forEach { s ->
                    val f = Offset(cx + s * u * 0.08f, cy + u * 0.18f)
                    drawCircle(Color(0xFFFFC9A8).copy(alpha = if (tap) 1f else 0.5f), u * 0.045f, f)
                    if (tap) drawCircle(blue.copy(alpha = 1f - seg(0.2f, 0.35f)), u * (0.05f + 0.06f * seg(0.2f, 0.35f)), f, style = Stroke(u * 0.006f))
                }
            }
        }
    }
}

@Composable
private fun ReadyPage(active: Boolean, onDone: () -> Unit) {
    val t = rememberInfiniteTransition(label = "ready")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "ring")
    val grow = remember { Animatable(0.4f) }
    LaunchedEffect(active) { if (active) { grow.snapTo(0.4f); grow.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 120f)) } }
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            // Two rings expand outwards and fade, one half a cycle behind the other.
            for (k in 0..1) {
                val p = (ring + k * 0.5f) % 1f
                drawCircle(Brand1.copy(alpha = (1f - p) * 0.35f), size.minDimension * (0.28f + 0.22f * p), center, style = Stroke(3.dp.toPx()))
            }
        }
        Box(
            Modifier.size(112.dp).scale(grow.value).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Brand2, Brand1, Brand3))),
            contentAlignment = Alignment.Center,
        ) { ProIconView(ProIcon.PEN, Color.White, Modifier.size(54.dp)) }
    }
    Spacer(Modifier.height(24.dp))
    AnimatedWords(
        "You're all set", TextStyle(fontSize = 52.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp, color = MaterialTheme.colorScheme.onBackground),
        active, gradient = listOf(Brand2, Brand1, Brand3),
    )
    Spacer(Modifier.height(8.dp))
    Typewriter("Create your first notebook or import a PDF.", MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant), active, startDelay = 600)
    Spacer(Modifier.height(28.dp))
    Button(onClick = onDone, shape = CircleShape, modifier = Modifier.widthIn(min = 240.dp).height(54.dp)) {
        Text("Start writing", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}
