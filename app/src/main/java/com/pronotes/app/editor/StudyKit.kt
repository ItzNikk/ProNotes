package com.pronotes.app.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
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

/** Gradient banner at the top of a study sheet: title, subtitle, and trailing slot. */
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
                Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.labelMedium)
            }
            trailing()
        }
    }
}

/** Animated AI progress bar shown inside study dialogs when an operation is running. */
@Composable
fun AiScanBanner(stage: String, engine: String, colors: List<Color> = StudyColors.cards) {
    val t = rememberInfiniteTransition(label = "scan")
    val sweep by t.animateFloat(-0.4f, 1.4f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "sw")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.first().copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth().border(1.dp, colors.first().copy(alpha = 0.28f), RoundedCornerShape(16.dp)),
    ) {
        Column {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✦", color = colors.last(), fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(stage.ifBlank { "Synthesising with local AI…" }, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                if (engine.isNotBlank()) Text(engine, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.fillMaxWidth().height(3.dp).background(colors.first().copy(alpha = 0.15f))) {
                Canvas(Modifier.fillMaxWidth().height(3.dp)) {
                    drawRect(
                        Brush.linearGradient(listOf(Color.Transparent, colors.first(), colors.last(), Color.Transparent),
                            Offset(size.width * sweep - 120f, 0f), Offset(size.width * sweep + 120f, 0f)),
                    )
                }
            }
        }
    }
}

/** Running stopwatch for a study session. */
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

/** Animated score ring with percentage in the middle. */
@Composable
fun ScoreRing(percent: Int, colors: List<Color>, modifier: Modifier = Modifier.size(164.dp)) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(percent) { a.snapTo(0f); a.animateTo(percent / 100f, tween(1100, easing = FastOutSlowInEasing)) }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(164.dp)) {
            val w = 14.dp.toPx()
            drawArc(track, -90f, 360f, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
            drawArc(Brush.sweepGradient(colors + colors.first()), -90f, 360f * a.value, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(a.value * 100).toInt()}%", fontSize = 38.sp, fontWeight = FontWeight.Black)
            Text("score", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun feedbackFor(percent: Int): Pair<String, String> = when {
    percent >= 95 -> "🏆" to "Outstanding! You really know this."
    percent >= 80 -> "🎉" to "Great work. Just a few to polish."
    percent >= 60 -> "👍" to "Good progress. Review the ones you missed."
    percent >= 40 -> "📚" to "Getting there. Another round will help."
    else -> "🌱" to "A fresh start. Go through the notes and try again."
}

/** Redesigned structured Summary Studio sheet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummarySheet(
    result: RichSummaryResult,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onAdd: () -> Unit,
    onAddSticky: () -> Unit = {},
    onBuildMindMap: () -> Unit = {},
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(18.dp).widthIn(max = 680.dp).fillMaxWidth(),
        ) {
            Box {
                AiParticleBackdrop(Modifier.matchParentSize(), StudyColors.summary, active = true, variant = AiBackdropVariant.AI_WAVE)
                Column(Modifier.padding(20.dp)) {
                    StudyHero(
                        "AI Note Summary",
                        "${result.wordCount} words · ${result.points.size} core takeaways · ${result.engineLabel}",
                        StudyColors.summary,
                        "✨",
                    ) {
                        TextButton(onClick = onClose) { Text("Close", color = Color.White) }
                    }
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Headline synthesis card
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = StudyColors.summary.first().copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth().border(1.dp, StudyColors.summary.first().copy(alpha = 0.28f), RoundedCornerShape(20.dp)),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("EXECUTIVE TAKEAWAY", style = MaterialTheme.typography.labelSmall, color = StudyColors.summary.first(), fontWeight = FontWeight.Bold)
                            Text(result.headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Key takeaways with staggered slide-in reveal
                    Text("CORE POINTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    result.points.forEachIndexed { i, p ->
                        val a = remember { Animatable(0f) }
                        LaunchedEffect(result.points) { a.snapTo(0f); delay(90L * i); a.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 20.dp.toPx() },
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                Box(Modifier.size(26.dp).clip(CircleShape).background(Brush.linearGradient(StudyColors.summary)), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(p, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    if (result.definitions.isNotEmpty()) {
                        Text("KEY DEFINITIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        result.definitions.forEach { d ->
                            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(d.term, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(d.meaning, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    if (result.formulas.isNotEmpty()) {
                        Text("FORMULAS & EQUATIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            result.formulas.forEach { f ->
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF181C28)) {
                                    Text(f, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color(0xFFFFD666), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (result.keywords.isNotEmpty()) {
                        Text("CONCEPTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            result.keywords.forEach { k ->
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text(k, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAdd, shape = CircleShape) { Text("Add to page") }
                    FilledTonalButton(onClick = onAddSticky, shape = CircleShape) { Text("Pin as sticky") }
                    FilledTonalButton(onClick = onBuildMindMap, shape = CircleShape) { Text("AI Mind Map") }
                    OutlinedButton(onClick = onCopy, shape = CircleShape) { Text("Copy") }
                }
                }
            }
        }
    }
}
