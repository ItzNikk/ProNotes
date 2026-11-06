package com.pronotes.app.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/** Colour sets for the study sheets: each feature has its own mood. */
object StudyColors {
    val cards = listOf(Color(0xFF7161E0), Color(0xFFB45CFF))
    val quiz = listOf(Color(0xFF2A78D6), Color(0xFF1BAF7A))
    val summary = listOf(Color(0xFFEB6834), Color(0xFFD55181))
}

/** Gradient banner at the top of a study sheet: title, a line under it, and room on the right. */
@Composable
fun StudyHero(title: String, subtitle: String, colors: List<Color>, glyph: String, trailing: @Composable () -> Unit = {}) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors)).padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                Text(glyph, fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
            }
            trailing()
        }
    }
}

/** Running stopwatch for a study session. [running] pauses it. Returns elapsed ms through [onTick]. */
@Composable
fun SessionClock(running: Boolean, onTick: (Long) -> Unit = {}) {
    var ms by remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) {
        while (running) { delay(1000); ms += 1000; onTick(ms) }
    }
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⏱", fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text(clock(ms), style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

fun clock(ms: Long): String { val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }

/** Animated score ring with the percentage in the middle. */
@Composable
fun ScoreRing(percent: Int, colors: List<Color>, modifier: Modifier = Modifier.size(170.dp)) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(percent) { a.snapTo(0f); a.animateTo(percent / 100f, tween(1100, easing = FastOutSlowInEasing)) }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSizeCompat()) {
            val w = 14.dp.toPx()
            drawArc(track, -90f, 360f, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
            drawArc(Brush.sweepGradient(colors + colors.first()), -90f, 360f * a.value, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(a.value * 100).toInt()}%", fontSize = 40.sp, fontWeight = FontWeight.Black)
            Text("score", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Modifier.matchParentSizeCompat(): Modifier = this.then(Modifier.fillMaxWidth().height(170.dp))

/** A friendly line for a score. */
fun feedbackFor(percent: Int): Pair<String, String> = when {
    percent >= 95 -> "🏆" to "Outstanding! You really know this."
    percent >= 80 -> "🎉" to "Great work. Just a few to polish."
    percent >= 60 -> "👍" to "Good progress. Review the ones you missed."
    percent >= 40 -> "📚" to "Getting there. Another round will help."
    else -> "🌱" to "A fresh start. Go through the notes and try again."
}

/** Key points sheet for "Summarise page". */
@Composable
fun SummarySheet(points: List<String>, onClose: () -> Unit, onCopy: () -> Unit, onAdd: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(20.dp).widthIn(max = 640.dp).fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                StudyHero("Key points", "${points.size} ideas from this page", StudyColors.summary, "✨")
                Spacer(Modifier.height(14.dp))
                Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    points.forEachIndexed { i, p ->
                        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                Box(Modifier.size(28.dp).clip(CircleShape).background(Brush.linearGradient(StudyColors.summary)), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(p, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAdd, shape = CircleShape) { Text("Add to page") }
                    OutlinedButton(onClick = onCopy, shape = CircleShape) { Text("Copy") }
                    Spacer(Modifier.weight(1f))
                    androidx.compose.material3.TextButton(onClick = onClose) { Text("Close") }
                }
            }
        }
    }
}
