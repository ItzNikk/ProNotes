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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.collectAsState
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.pronotes.app.R
import com.pronotes.app.data.AccentColor
import com.pronotes.app.data.AppFont
import com.pronotes.app.data.CustomAiProvider
import com.pronotes.app.data.MotionStyle
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.PenBrands
import com.pronotes.app.data.ScrollDirection
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.ThemeMode
import com.pronotes.app.data.ToolbarDock
import com.pronotes.app.editor.LocalAiManager
import com.pronotes.app.editor.LocalModelStatus
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

/** Ultra Setup Studio (v9.11): 5-step interactive onboarding for look, workspace, AI engine, and stylus check. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val store = remember { SettingsStore.get(context) }
    val pager = rememberPagerState { 5 }
    val scope = rememberCoroutineScope()
    fun next() = scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(4)) }
    fun back() = scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) }
    fun finish() { store.update { it.copy(onboardingDone = true) }; onDone() }

    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AuroraBackground(pager.currentPage)
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            val active = pager.currentPage == page
            Box(Modifier.fillMaxSize()) {
                if (page == 0) IntroBackdrop()
                Column(
                    Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    when (page) {
                        0 -> IntroPage(active, onStart = { next() })
                        1 -> LookStudioPage(active, store, onNext = { next() })
                        2 -> WorkspaceStudioPage(active, store, onNext = { next() })
                        3 -> AiStudioSetupPage(active, store, onNext = { next() })
                        else -> PenCheckPage(active, store, onNext = { finish() }, isFinal = true)
                    }
                }
            }
        }
        val onVideo = pager.currentPage == 0
        if (pager.currentPage > 0) {
            TextButton(
                onClick = { back() },
                modifier = Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(8.dp),
            ) { Text("← Back") }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(5) { i ->
                val w by animateFloatAsState(if (pager.currentPage == i) 28f else 8f, spring(dampingRatio = 0.7f), label = "dot")
                Box(
                    Modifier.size(width = w.dp, height = 8.dp).clip(CircleShape)
                        .background(
                            if (pager.currentPage == i) (if (onVideo) Color.White else MaterialTheme.colorScheme.primary)
                            else (if (onVideo) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.25f),
                        )
                        .clickable { scope.launch { pager.animateScrollToPage(i) } },
                )
            }
        }
        TextButton(
            onClick = { finish() },
            modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = if (onVideo) Color.White else MaterialTheme.colorScheme.primary),
        ) { Text("Skip setup") }
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
 * Glowing blue opposite-rotating particle rings on solid black background for pen setup/check.
 */
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
    val electric = Color(0xFF2563EB)
    val sky = Color(0xFF38BDF8)
    val ice = Color(0xFFE0F2FE)
    val deep = Color(0xFF0A2472)
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f
        val ringR = r * 0.80f
        val e = (0.65f + 0.35f * energy.coerceIn(0f, 1f))
        // Solid black stage with central blue core aura
        drawCircle(Color(0xFF02040A), r * 0.97f, c)
        drawCircle(
            Brush.radialGradient(listOf(electric.copy(alpha = 0.24f * e), sky.copy(alpha = 0.14f * e), Color.Transparent), c, r),
            r, c,
        )
        // Outer CW blue energy band & inner CCW cyan orbital band
        rotate(spinCW, c) {
            drawCircle(
                Brush.sweepGradient(listOf(deep, sky, electric, ice, sky, deep), c),
                ringR, c, style = Stroke(r * (0.075f + 0.02f * pulse), cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
        rotate(spinCCW, c) {
            drawCircle(
                Brush.sweepGradient(listOf(Color.Transparent, ice.copy(alpha = 0.85f * e), sky.copy(alpha = 0.45f * e), Color.Transparent, electric.copy(alpha = 0.7f * e), Color.Transparent), c),
                ringR * 0.84f, c, style = Stroke(r * 0.032f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
        // Opposite-moving blue light particles with trailing arcs
        particles.forEach { p ->
            val deg = p[0] + (if (p[2] > 0f) spinCW else spinCCW - 360f) * p[3]
            val rr = r * p[1]
            val col = when (p[5].toInt()) { 0 -> electric; 1 -> sky; else -> ice }
            for (step in 3 downTo 0) {
                val radAngle = Math.toRadians((deg - p[2] * step * 6.5f).toDouble())
                val pos = Offset(c.x + kotlin.math.cos(radAngle).toFloat() * rr, c.y + kotlin.math.sin(radAngle).toFloat() * rr)
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
        val d = minOf(maxWidth * 0.54f, maxHeight * 0.36f).coerceIn(160.dp, 360.dp)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            GlowingCircle(d, active)
            Spacer(Modifier.height(26.dp))
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.16f)) {
                Text(
                    "PRO NOTES · ULTRA v9.11",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            ShimmerText(
                "Pro Notes",
                TextStyle(fontSize = 62.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp),
                listOf(Color.White, Color(0xFF93C5FD), Color.White, Color(0xFFE9D5FF)),
            )
            Spacer(Modifier.height(6.dp))
            AnimatedWords(
                "Write. Revise. Flow.",
                TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Light, letterSpacing = 1.sp, color = Color.White),
                active, startDelay = 450,
            )
            Spacer(Modifier.height(10.dp))
            Typewriter(
                "Slide show revision, interactive sticky checklists, elastic dock, and custom AI — shaped to your hand.",
                MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.88f)),
                active, Modifier.widthIn(max = 480.dp),
            )
            Spacer(Modifier.height(26.dp))
            val pulse by rememberInfiniteTransition(label = "cta")
                .animateFloat(1f, 1.045f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
            Button(
                onClick = onStart,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0F172A)),
                modifier = Modifier.widthIn(min = 240.dp).height(54.dp).scale(pulse),
            ) {
                Text("Customize your Studio →", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            Box(Modifier.size(width = w.dp, height = 8.dp).clip(CircleShape).background(if (reached) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)))
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

    val primary = Color(0xFF38BDF8)
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
                Surface(shape = CircleShape, color = Color(0xE6040B1A), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6638BDF8))) {
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
        TextButton(onClick = { store.update { it.copy(stylusOnly = false) }; onNext() }) {
            Text(if (isFinal) "Skip pen check & launch" else "I don't use a pen")
        }
        Button(
            onClick = onNext,
            shape = CircleShape,
            enabled = isFinal || done || brand != null,
        ) {
            Text(if (isFinal) "Launch Pro Notes ✓" else "Continue")
        }
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
    QUIZ("Quiz yourself", "Make quizzes from your PDF, a picture or your own questions. Get a score and feedback."),
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
            Demo.QUIZ -> {
                val r = u * 0.26f
                val k = seg(0.1f, 0.7f)
                drawArc(Color(0xFFE6EAF2), -90f, 360f, false, Offset(cx - r, cy - r), androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(u * 0.05f))
                drawArc(
                    Brush.sweepGradient(listOf(Color(0xFF2A78D6), Color(0xFF1BAF7A), Color(0xFF2A78D6)), Offset(cx, cy)), -90f, 300f * k, false,
                    Offset(cx - r, cy - r), androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(u * 0.05f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
                drawContext.canvas.nativeCanvas.drawText("${(83 * k).toInt()}%", cx, cy + u * 0.04f, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2B2F3A.toInt(); textSize = u * 0.11f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true })
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LookStudioPage(active: Boolean, store: SettingsStore, onNext: () -> Unit) {
    val s by store.settings.collectAsState()
    AnimatedWords(
        "Craft your aesthetic",
        TextStyle(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, color = MaterialTheme.colorScheme.onBackground),
        active,
        gradient = listOf(Brand1, Brand2, Brand3),
    )
    Text("Live preview updates as you pick theme, accent gradient, typography, and motion.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(14.dp))

    // Live interactive notebook card preview
    val accentBrush = Brush.linearGradient(listOf(Color(s.accent.argb), Color(s.accent.argb2)))
    val previewFamily = when (s.appFont) {
        AppFont.EDITORIAL_SERIF -> FontFamily.Serif
        AppFont.STUDIO_MONO -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 6.dp,
        modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth().border(1.5.dp, Color(s.accent.argb).copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(accentBrush), contentAlignment = Alignment.Center) {
                ProIconView(ProIcon.PEN, Color.White, Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Organic Chemistry · Lecture 4", fontFamily = previewFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("${s.themeMode.label} · ${s.appFont.label} · ${s.motionStyle.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp).heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("THEME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { t ->
                    FilterChip(s.themeMode == t, { store.update { it.copy(themeMode = t) } }, label = { Text(t.label) }, shape = CircleShape)
                }
            }
            Text("TYPOGRAPHY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppFont.entries.forEach { f ->
                    FilterChip(s.appFont == f, { store.update { it.copy(appFont = f) } }, label = { Text(f.label) }, shape = CircleShape)
                }
            }
            Text("ACCENT GRADIENT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentColor.entries.forEach { a ->
                    val on = a == s.accent
                    Box(
                        Modifier.size(36.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(a.argb), Color(a.argb2))))
                            .border(if (on) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            .clickable { store.update { it.copy(accent = a) } },
                    )
                }
            }
            Text("MOTION ENGINE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MotionStyle.entries.forEach { m ->
                    FilterChip(s.motionStyle == m, { store.update { it.copy(motionStyle = m) } }, label = { Text(m.label) }, shape = CircleShape)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(onClick = onNext, shape = CircleShape, modifier = Modifier.widthIn(min = 210.dp)) { Text("Next: Workspace →") }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkspaceStudioPage(active: Boolean, store: SettingsStore, onNext: () -> Unit) {
    val s by store.settings.collectAsState()
    AnimatedWords(
        "Flow & dock",
        TextStyle(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, color = MaterialTheme.colorScheme.onBackground),
        active,
        gradient = listOf(Brand2, Brand1, Color(0xFF10B981)),
    )
    Text("Tune page scrolling, elastic draggable dock placement, default paper, and stylus hover labels.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(14.dp))
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp).heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("SCROLL DIRECTION (MATCHES SPLIT VIEW TOO)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScrollDirection.entries.forEach { d ->
                    FilterChip(
                        s.scroll == d,
                        { store.update { it.copy(scroll = d) } },
                        label = { Text(if (d == ScrollDirection.VERTICAL) "Vertical Continuous" else "Horizontal Page Snap") },
                        shape = CircleShape,
                    )
                }
            }
            Text("TOOLBAR POSITION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ToolbarDock.entries.forEach { d ->
                    FilterChip(s.toolbarDock == d, { store.update { it.copy(toolbarDock = d) } }, label = { Text(d.label) }, shape = CircleShape)
                }
            }
            Text("DEFAULT PAPER TEMPLATE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PaperStyle.pickerEntries.forEach { p ->
                    FilterChip(s.defaultPaper == p, { store.update { it.copy(defaultPaper = p) } }, label = { Text(p.label) }, shape = CircleShape)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Liquid Glass bars")
                    Text("Frosted translucent toolbars with specular highlight", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(s.liquidGlass, { v -> store.update { it.copy(liquidGlass = v) } })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hover tool name tooltips")
                    Text("Shows a floating name pill when hovering with your stylus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(s.showHoverTooltips, { v -> store.update { it.copy(showHoverTooltips = v) } })
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(onClick = onNext, shape = CircleShape, modifier = Modifier.widthIn(min = 210.dp)) { Text("Next: AI Studio →") }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiStudioSetupPage(active: Boolean, store: SettingsStore, onNext: () -> Unit) {
    val context = LocalContext.current
    val s by store.settings.collectAsState()
    val modelStates by LocalAiManager.states.collectAsState()
    val recSpec = LocalAiManager.catalog.firstOrNull { it.id == "smollm2-360m-q4" } ?: LocalAiManager.catalog.first()
    val recRuntime = modelStates[recSpec.id]
    val scope = rememberCoroutineScope()
    var testStatus by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { LocalAiManager.refresh(context) }

    AnimatedWords(
        "Choose your AI engine",
        TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, color = MaterialTheme.colorScheme.onBackground),
        active,
        gradient = listOf(Color(0xFF38BDF8), Brand1, Brand3),
    )
    Text("Power slide shows, semantic search, mind maps, and summaries — offline or via your own API.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(14.dp))

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp).heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Option 1: Lightweight On-device GGUF
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("On-Device Offline AI · ${recSpec.name} (${recSpec.sizeLabel})", fontWeight = FontWeight.Bold)
                    Text(recSpec.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    when (recRuntime?.status) {
                        LocalModelStatus.READY -> Text("✓ Downloaded & Ready for offline study", color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                        LocalModelStatus.DOWNLOADING -> {
                            LinearProgressIndicator(progress = { recRuntime.progress }, modifier = Modifier.fillMaxWidth())
                            Text("Downloading ${(recRuntime.progress * 100).toInt()}%…", style = MaterialTheme.typography.labelSmall)
                        }
                        else -> OutlinedButton(
                            onClick = { LocalAiManager.startDownload(context, recSpec.id) },
                            shape = CircleShape,
                        ) { Text("Download Offline Model (${recSpec.sizeLabel})") }
                    }
                }
            }

            // Option 2: Custom AI API
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Custom AI API (OpenAI · Google AI · Ollama)", fontWeight = FontWeight.Bold)
                            Text("Optional cloud or LAN endpoint for instant study generation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(s.customAiEnabled, { v -> store.update { it.copy(customAiEnabled = v) } })
                    }
                    if (s.customAiEnabled) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CustomAiProvider.entries.forEach { p ->
                                FilterChip(
                                    s.customAiProvider == p,
                                    { store.update { cur -> cur.copy(customAiProvider = p, customAiBaseUrl = p.defaultBaseUrl, customAiModel = p.defaultModel) } },
                                    label = { Text(p.label, fontSize = 11.sp) },
                                    shape = CircleShape,
                                )
                            }
                        }
                        OutlinedTextField(s.customAiBaseUrl, { v -> store.update { it.copy(customAiBaseUrl = v) } }, label = { Text("Base URL") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(s.customAiModel, { v -> store.update { it.copy(customAiModel = v) } }, label = { Text("Model") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(s.customAiApiKey, { v -> store.update { it.copy(customAiApiKey = v) } }, label = { Text("API Key") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    testStatus = "Testing…"
                                    scope.launch {
                                        val res = LocalAiManager.testCustomApi(context)
                                        testStatus = res.fold({ "✓ Connected" }, { "✕ ${it.message}" })
                                    }
                                },
                                shape = CircleShape,
                            ) { Text("Test Connection") }
                            testStatus?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(onClick = onNext, shape = CircleShape, modifier = Modifier.widthIn(min = 210.dp)) { Text("Next: Stylus Check →") }
}

