package com.pronotes.app.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** "Thinking…" pill with three bouncing dots and a moving shine, shown while on-device AI works. */
@Composable
fun ThinkingPill() {
    val t = rememberInfiniteTransition(label = "think")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "p")
    val shine by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "s")
    Surface(shape = CircleShape, color = Color(0xF21B1F2A), shadowElevation = 8.dp) {
        Box {
            Canvas(Modifier.matchParentSizeCompat()) {
                drawRect(Brush.linearGradient(listOf(Color.Transparent, Color(0x33B45CFF), Color.Transparent), Offset(size.width * shine - 80f, 0f), Offset(size.width * shine + 80f, size.height)))
            }
            Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✨", color = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Thinking", color = Color.White, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(6.dp))
                for (i in 0 until 3) {
                    val k = ((phase * 3f - i) % 3f + 3f) % 3f
                    val up = if (k < 1f) kotlin.math.sin(k * Math.PI).toFloat() else 0f
                    Box(Modifier.padding(horizontal = 2.dp).size(5.dp).graphicsLayer { translationY = -up * 6.dp.toPx() }) {
                        Canvas(Modifier.size(5.dp)) { drawCircle(Color(0xFFE38BFF)) }
                    }
                }
            }
        }
    }
}

private fun Modifier.matchParentSizeCompat() = this.then(Modifier.size(220.dp, 44.dp))

/**
 * Pip, the study buddy: a little round blob that breathes and blinks, watches while you write,
 * thinks with you while the AI works, cheers when something is added, and wiggles when tapped.
 */
@Composable
fun StudyPet(vm: EditorViewModel, modifier: Modifier, compact: Boolean = false) {
    val t = rememberInfiniteTransition(label = "pet")
    val breathe by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    var blink by remember { mutableStateOf(false) }
    var say by remember { mutableStateOf<String?>(null) }
    val jump = remember { Animatable(0f) }
    val wiggle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { while (true) { delay(2600L + (0..2500).random()); blink = true; delay(140); blink = false } }
    fun react(msg: String?) = scope.launch {
        say = msg
        jump.snapTo(0f); jump.animateTo(1f, tween(180)); jump.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 300f))
        delay(1800); if (say == msg) say = null
    }
    // Cheer on good news.
    LaunchedEffect(vm.toast) { val m = vm.toast ?: return@LaunchedEffect; if (m.startsWith("Added") || m.startsWith("Saved") || m.startsWith("Link")) react(listOf("Yay!", "Nice one!", "Great!").random()) }
    val writing = vm.penActive
    val thinking = vm.aiBusy
    val cheers = listOf("You've got this!", "Keep going!", "Hi! 👋", "Study time?", "Proud of you!", "Take a sip of water 💧")
    // Hops when you turn a page, dozes off after a minute without writing.
    var sleepy by remember { mutableStateOf(false) }
    LaunchedEffect(vm.pageIndex) { if ((0..2).random() == 0) react(null) }
    LaunchedEffect(vm.lastActivity) { sleepy = false; delay(60_000); sleepy = true }
    LaunchedEffect(vm.cards.size, vm.quiz.size) { if (vm.cards.isNotEmpty() || vm.quiz.isNotEmpty()) react(null) }
    Column(modifier.graphicsLayer { alpha = if (writing && !compact) 0.35f else 1f }, horizontalAlignment = Alignment.CenterHorizontally) {
        val bubble = say ?: if (thinking) "Hmm…" else if (sleepy) "zzz…" else null
        if (bubble != null) {
            val content = @Composable {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                    Text(bubble, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
            if (compact) androidx.compose.ui.window.Popup(alignment = Alignment.TopCenter, offset = androidx.compose.ui.unit.IntOffset(0, 120)) { content() } else content()
        }
        Canvas(
            Modifier.size(if (compact) 34.dp else 56.dp).graphicsLayer {
                translationY = -jump.value * 18.dp.toPx()
                rotationZ = wiggle.value * 12f
            }.clickable(remember { MutableInteractionSource() }, null) {
                scope.launch { for (k in listOf(1f, -1f, 0.6f, -0.6f, 0f)) wiggle.animateTo(k, tween(70)) }
                react(cheers.random())
            },
        ) {
            val w = size.width; val h = size.height
            val squish = 1f + 0.04f * breathe - jump.value * 0.08f
            val bodyH = h * 0.78f * squish; val bodyW = w * 0.9f / squish
            val top = h - bodyH
            // Shadow, body with a soft gradient, cheeks.
            drawOval(Color(0x22000000), Offset(w * 0.18f, h - 6f), Size(w * 0.64f, 8f))
            drawOval(Brush.verticalGradient(listOf(Color(0xFFC9A4FF), Color(0xFF8B6CF0)), top, h), Offset((w - bodyW) / 2, top), Size(bodyW, bodyH))
            drawOval(Color(0x55FFFFFF), Offset(w * 0.25f, top + bodyH * 0.1f), Size(bodyW * 0.3f, bodyH * 0.18f))
            drawCircle(Color(0x66FF8FB1), w * 0.07f, Offset(w * 0.27f, top + bodyH * 0.62f))
            drawCircle(Color(0x66FF8FB1), w * 0.07f, Offset(w * 0.73f, top + bodyH * 0.62f))
            // Eyes: blink, look down at the page while you write, look up while thinking.
            val ey = top + bodyH * (if (writing) 0.5f else if (thinking) 0.36f else 0.44f)
            val ex = if (thinking) w * 0.04f else 0f
            for (x in listOf(w * 0.38f, w * 0.62f)) {
                if (blink || (sleepy && say == null)) drawLine(Color(0xFF2B2F3A), Offset(x - 4f + ex, ey), Offset(x + 4f + ex, ey), 3f)
                else { drawCircle(Color(0xFF2B2F3A), w * 0.055f, Offset(x + ex, ey)); drawCircle(Color.White, w * 0.018f, Offset(x + ex + 2f, ey - 2f)) }
            }
            // Mouth: smile, or a little "o" while thinking.
            if (thinking) drawCircle(Color(0xFF2B2F3A), w * 0.03f, Offset(w / 2, top + bodyH * 0.66f), style = Stroke(2.5f))
            else {
                val p = Path().apply { moveTo(w * 0.43f, top + bodyH * 0.63f); quadraticTo(w / 2, top + bodyH * (0.72f + 0.06f * jump.value), w * 0.57f, top + bodyH * 0.63f) }
                drawPath(p, Color(0xFF2B2F3A), style = Stroke(2.5f))
            }
        }
    }
}
