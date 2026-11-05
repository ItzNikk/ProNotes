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
        HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = pager.currentPage != 1) { page ->
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
 * The pen ring as an animated picture. A plain image view draws inside the circle clip reliably,
 * unlike a video surface. Android 8 falls back to the video.
 */
@Composable
private fun RingAnimation(modifier: Modifier) {
    if (android.os.Build.VERSION.SDK_INT < 28) { LoopingVideo(R.raw.pen_ring, circle = true, modifier = modifier); return }
    AndroidView(
        modifier = modifier.clip(CircleShape).background(Color.Black),
        factory = { ctx ->
            android.widget.ImageView(ctx).apply {
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                runCatching {
                    val d = android.graphics.ImageDecoder.decodeDrawable(android.graphics.ImageDecoder.createSource(ctx.resources, R.raw.pen_ring_anim))
                    setImageDrawable(d)
                    (d as? android.graphics.drawable.AnimatedImageDrawable)?.apply {
                        repeatCount = android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
                        start()
                    }
                }
            }
        },
    )
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

/** Tap the ring three times with the pen, then watch it scan. We read the pen's name and capabilities and pick its brand. */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
private fun PenCheckPage(active: Boolean, store: SettingsStore, onNext: () -> Unit) {
    val idleHint = "Tap the ring 3 times with your pen"
    var taps by remember { mutableIntStateOf(0) }
    var scanning by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var device by remember { mutableStateOf<String?>(null) }
    var brand by remember { mutableStateOf<String?>(null) }
    var caps by remember { mutableStateOf<List<String>>(emptyList()) }
    var hint by remember { mutableStateOf(idleHint) }
    var manual by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val pulse = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // After the third tap: a short scan from 30% to 100%, then reveal the result.
    LaunchedEffect(scanning) {
        if (!scanning) return@LaunchedEffect
        progress.animateTo(100f, tween(3200, easing = LinearEasing))
        delay(250)
        val b = PenBrands.detect(device)
        brand = b
        store.registerPen(device ?: "Stylus", b)
        hint = if (b != null && b != "Stylus") "Found: $b" else "Pen found. Pick its brand below if you like."
        scanning = false
        finished = true
    }
    val pct = progress.value.toInt().coerceIn(0, 100)
    val stepLabel = when {
        pct < 45 -> "Reading pen signal…"
        pct < 65 -> "Checking pressure…"
        pct < 85 -> "Checking tilt and hover…"
        else -> "Matching brand…"
    }

    AnimatedWords(
        "Meet your pen", TextStyle(fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, color = MaterialTheme.colorScheme.onBackground),
        active, gradient = listOf(Brand2, Brand1, Brand3),
    )
    Spacer(Modifier.height(8.dp))
    val shownHint = if (scanning) stepLabel else hint
    AnimatedContent(targetState = shownHint, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "hint") { h ->
        Text(h, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(26.dp))
    val primary = Color(0xFFB45CFF)
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Box(Modifier.size(230.dp).scale(pulse.value), contentAlignment = Alignment.Center) {
        // The ring video sits inside the progress arc.
        if (active) RingAnimation(Modifier.size(198.dp))
        else Box(Modifier.size(198.dp).clip(CircleShape).background(Color.Black))
        Canvas(Modifier.fillMaxSize()) {
            val w = 6.dp.toPx()
            drawCircle(track, size.minDimension / 2 - w / 2, style = Stroke(w))
            drawArc(
                primary, -90f, 360f * (progress.value / 100f), false, Offset(w / 2, w / 2),
                androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                style = Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
        // Percentage pill over the video.
        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
            Text(
                "$pct%", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            )
        }
        // Touch layer on top of the video.
        Box(
            Modifier.fillMaxSize().pointerInteropFilter { ev ->
                if (scanning || finished || ev.actionMasked != MotionEvent.ACTION_DOWN) return@pointerInteropFilter true
                val type = ev.getToolType(0)
                if (type == MotionEvent.TOOL_TYPE_STYLUS || type == MotionEvent.TOOL_TYPE_ERASER) {
                    taps++
                    val dev = ev.device
                    device = dev?.name
                    if (dev != null) {
                        caps = buildList {
                            if (dev.getMotionRange(MotionEvent.AXIS_PRESSURE, InputDevice.SOURCE_STYLUS) != null) add("Pressure")
                            if (dev.getMotionRange(MotionEvent.AXIS_TILT, InputDevice.SOURCE_STYLUS) != null) add("Tilt")
                            if (dev.getMotionRange(MotionEvent.AXIS_DISTANCE, InputDevice.SOURCE_STYLUS) != null) add("Hover")
                        }
                    }
                    scope.launch { pulse.snapTo(0.92f); pulse.animateTo(1f, spring(dampingRatio = 0.4f)) }
                    scope.launch { progress.animateTo(taps * 10f, tween(250)) }
                    if (taps >= 3) { hint = "Scanning your pen…"; scanning = true } else hint = "${3 - taps} more"
                } else hint = "That was a finger. Please use your pen."
                true
            },
        )
    }
    Spacer(Modifier.height(14.dp))
    if (finished && caps.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            caps.forEach { c ->
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(c, Modifier.padding(horizontal = 14.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (taps > 0 && !scanning) OutlinedButton(onClick = {
            taps = 0; finished = false; brand = null; device = null; caps = emptyList(); hint = idleHint
            scope.launch { progress.animateTo(0f, tween(300)) }
        }, shape = CircleShape) { Text("Try again") }
        Box {
            OutlinedButton(onClick = { manual = true }, enabled = !scanning, shape = CircleShape) { Text(brand?.let { "Brand: $it" } ?: "Choose manually") }
            DropdownMenu(manual, { manual = false }, shape = RoundedCornerShape(22.dp)) {
                PenBrands.manual.forEach { b ->
                    DropdownMenuItem(text = { Text(b) }, onClick = {
                        manual = false; brand = b
                        store.registerPen(device ?: "Stylus", b)
                        hint = "Set to $b"
                    })
                }
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { store.update { it.copy(stylusOnly = false) }; onNext() }) { Text("I don't use a pen") }
        Button(onClick = onNext, shape = CircleShape, enabled = finished || brand != null) { Text("Continue") }
    }
}

@Composable
private fun TourPage(active: Boolean, onNext: () -> Unit) {
    AnimatedWords(
        "Little things, big difference", TextStyle(fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, color = MaterialTheme.colorScheme.onBackground),
        active, gradient = listOf(Brand3, Brand1, Brand2),
    )
    Spacer(Modifier.height(20.dp))
    val tips = listOf(
        ProIcon.LASSO to ("Circle and hold" to "Loop around ink and keep the pen still to select it."),
        ProIcon.ERASER to ("Scribble to erase" to "Zig-zag over ink to delete it. The pen side button erases too."),
        ProIcon.SHAPES to ("Hold to snap" to "Hold at the end of a stroke to turn it into a clean shape."),
        ProIcon.MORE to ("Pen button click" to "Click the pen button while hovering for the tool wheel."),
        ProIcon.UNDO to ("Two-finger tap" to "Undo with two fingers, redo with three."),
        ProIcon.ADD to ("Pull for a page" to "Scroll past the last page to add a new one."),
    )
    Column(Modifier.widthIn(max = 580.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tips.forEachIndexed { i, (icon, t) ->
            val shown = remember { Animatable(0f) }
            LaunchedEffect(active) { if (active) { shown.snapTo(0f); delay(450L + 90L * i); shown.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 150f)) } }
            Surface(
                shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth().graphicsLayer {
                    alpha = shown.value.coerceIn(0f, 1f); translationX = (1f - shown.value) * 60.dp.toPx()
                }.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        ProIconView(icon, MaterialTheme.colorScheme.primary, Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(t.first, style = MaterialTheme.typography.titleMedium)
                        Text(t.second, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(22.dp))
    Button(onClick = onNext, shape = CircleShape, modifier = Modifier.widthIn(min = 200.dp)) { Text("Next") }
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
