package com.pronotes.app.onboarding

import android.net.Uri
import android.view.MotionEvent
import android.widget.VideoView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pronotes.app.R
import com.pronotes.app.data.PenBrands
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.ui.GradientTitle
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import kotlinx.coroutines.launch

/** First-run welcome: video, pen check, a short tour. Can be replayed from Settings. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val pager = rememberPagerState { 4 }
    val scope = rememberCoroutineScope()
    fun next() = scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(3)) }
    fun finish() { store.update { it.copy(onboardingDone = true) }; onDone() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = pager.currentPage != 1) { page ->
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                when (page) {
                    0 -> IntroPage(onStart = { next() })
                    1 -> PenCheckPage(store, onNext = { next() })
                    2 -> TourPage(onNext = { next() })
                    else -> ReadyPage(onDone = { finish() })
                }
            }
        }
        // Page dots
        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(4) { i ->
                val w by animateFloatAsState(if (pager.currentPage == i) 22f else 8f, label = "dot")
                Box(Modifier.size(width = w.dp, height = 8.dp).clip(CircleShape)
                    .background(if (pager.currentPage == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
            }
        }
        TextButton(onClick = { finish() }, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) { Text("Skip") }
    }
}

@Composable
private fun IntroPage(onStart: () -> Unit) {
    val context = LocalContext.current
    Box(
        Modifier.widthIn(max = 520.dp).fillMaxWidth().height(300.dp).clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(Uri.parse("android.resource://${context.packageName}/${R.raw.welcome}"))
                    setOnPreparedListener { mp -> mp.isLooping = true; mp.setVolume(0f, 0f); start() }
                }
            },
        )
    }
    Spacer(Modifier.height(32.dp))
    GradientTitle("Welcome to Pro Notes", MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(10.dp))
    Text(
        "Handwriting, PDFs and ideas in one place, built for your pen.",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(28.dp))
    Button(onClick = onStart, shape = CircleShape, modifier = Modifier.widthIn(min = 220.dp)) { Text("Get started") }
}

/** Tap the circle three times with the pen; we read the pen's name and pick its brand. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PenCheckPage(store: SettingsStore, onNext: () -> Unit) {
    var taps by remember { mutableIntStateOf(0) }
    var device by remember { mutableStateOf<String?>(null) }
    var brand by remember { mutableStateOf<String?>(null) }
    var hint by remember { mutableStateOf("Tap the circle 3 times with your pen") }
    var manual by remember { mutableStateOf(false) }
    val pulse = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val ring = rememberInfiniteTransition(label = "ring")
    val breathe by ring.animateFloat(0.92f, 1.06f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val done = taps >= 3

    GradientTitle("Set up your pen", MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    AnimatedContent(targetState = hint, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "hint") { h ->
        Text(h, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(28.dp))
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier.size(200.dp).scale(if (done) 1f else breathe * pulse.value)
            .pointerInteropFilter { ev ->
                if (done || ev.actionMasked != MotionEvent.ACTION_DOWN) return@pointerInteropFilter true
                val type = ev.getToolType(0)
                if (type == MotionEvent.TOOL_TYPE_STYLUS || type == MotionEvent.TOOL_TYPE_ERASER) {
                    taps++
                    device = ev.device?.name
                    scope.launch { pulse.snapTo(0.85f); pulse.animateTo(1f) }
                    if (taps >= 3) {
                        brand = PenBrands.detect(device)
                        val name = device ?: "Stylus"
                        store.registerPen(name, brand)
                        hint = if (brand != null && brand != "Stylus") "Found: $brand" else "Pen found. Pick its brand below if you like."
                    } else hint = "${3 - taps} more"
                } else {
                    hint = "That was a finger. Please use your pen."
                }
                true
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 10.dp.toPx()
            drawCircle(track, size.minDimension / 2 - w, style = Stroke(w))
            drawArc(primary, -90f, 360f * (taps.coerceAtMost(3) / 3f), false, Offset(w, w),
                androidx.compose.ui.geometry.Size(size.width - 2 * w, size.height - 2 * w), style = Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        if (done) ProIconView(ProIcon.PEN, primary, Modifier.size(64.dp), ink = primary)
        else Text("${taps}/3", style = MaterialTheme.typography.headlineSmall)
    }
    Spacer(Modifier.height(28.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (taps > 0) OutlinedButton(onClick = { taps = 0; brand = null; device = null; hint = "Tap the circle 3 times with your pen" }, shape = CircleShape) { Text("Try again") }
        Box {
            OutlinedButton(onClick = { manual = true }, shape = CircleShape) { Text(brand?.let { "Brand: $it" } ?: "Choose manually") }
            DropdownMenu(manual, { manual = false }, shape = RoundedCornerShape(18.dp)) {
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
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { store.update { it.copy(stylusOnly = false) }; onNext() }) { Text("I don't use a pen") }
        Button(onClick = onNext, shape = CircleShape, enabled = done || brand != null) { Text("Continue") }
    }
}

@Composable
private fun TourPage(onNext: () -> Unit) {
    GradientTitle("A few things to know", MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(20.dp))
    val tips = listOf(
        ProIcon.LASSO to ("Circle and hold" to "Draw a loop around ink and keep the pen still to select it."),
        ProIcon.ERASER to ("Scribble to erase" to "Zig-zag over ink to delete it. The pen side button erases too."),
        ProIcon.SHAPES to ("Hold to snap" to "Hold at the end of a stroke to turn it into a clean shape."),
        ProIcon.MORE to ("Pen button click" to "Click the pen button while hovering to open the tool wheel."),
        ProIcon.UNDO to ("Two-finger tap" to "Undo with two fingers, redo with three."),
        ProIcon.ADD to ("Pull for a page" to "Scroll past the last page to add a new one."),
    )
    Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tips.forEachIndexed { i, (icon, t) ->
            val shown = remember { Animatable(0f) }
            LaunchedEffect(Unit) { kotlinx.coroutines.delay(80L * i); shown.animateTo(1f, tween(380)) }
            Surface(
                shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().scale(0.94f + 0.06f * shown.value)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = shown.value), RoundedCornerShape(22.dp)),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
    Spacer(Modifier.height(24.dp))
    Button(onClick = onNext, shape = CircleShape) { Text("Next") }
}

@Composable
private fun ReadyPage(onDone: () -> Unit) {
    val grow = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.5f)) }
    Box(
        Modifier.size(120.dp).scale(grow.value).clip(CircleShape)
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF7161E0)))),
        contentAlignment = Alignment.Center,
    ) { ProIconView(ProIcon.PEN, Color.White, Modifier.size(56.dp)) }
    Spacer(Modifier.height(28.dp))
    GradientTitle("You're all set", MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(8.dp))
    Text("Create your first notebook or import a PDF.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(28.dp))
    Button(onClick = onDone, shape = CircleShape, modifier = Modifier.widthIn(min = 220.dp)) { Text("Start writing") }
}
