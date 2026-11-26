package com.pronotes.app.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ScaleInAnimatedText
import com.pronotes.app.ui.appCardShape
import kotlinx.coroutines.delay

/** Colour sets for study studios. */
object StudyColors {
    val cards = listOf(ProButtonPalette.RoyalBlue, ProButtonPalette.SkyCyan)
    val slides = listOf(ProButtonPalette.Violet, ProButtonPalette.MagentaPink)
}

/** Gradient banner at the top of a study sheet with animated heading, subtitle, and trailing slot. */
@Composable
fun StudyHero(
    title: String,
    subtitle: String,
    colors: List<Color>,
    glyph: String,
    trailing: @Composable () -> Unit = {},
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(appCardShape(4f))
            .background(Brush.linearGradient(colors))
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                ScaleInAnimatedText(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 23.sp, fontWeight = FontWeight.Bold),
                    staggerMs = 22L,
                )
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            trailing()
        }
    }
}

/** Running stopwatch for a study session. */
@Composable
fun SessionClock(running: Boolean, onTick: (Long) -> Unit = {}) {
    var ms by remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            ms += 1000
            onTick(ms)
        }
    }
    AppPill(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
        Text(
            "Time ${clock(ms)}",
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

fun clock(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/** Animated score ring with percentage in the middle. */
@Composable
fun ScoreRing(percent: Int, colors: List<Color>, modifier: Modifier = Modifier.size(164.dp)) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(percent) {
        a.snapTo(0f)
        a.animateTo(percent / 100f, tween(1100, easing = FastOutSlowInEasing))
    }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(164.dp)) {
            val w = 14.dp.toPx()
            drawArc(
                track,
                -90f,
                360f,
                false,
                Offset(w / 2, w / 2),
                Size(size.width - w, size.height - w),
                style = Stroke(w),
            )
            drawArc(
                Brush.sweepGradient(colors + colors.first()),
                -90f,
                360f * a.value,
                false,
                Offset(w / 2, w / 2),
                Size(size.width - w, size.height - w),
                style = Stroke(w, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(a.value * 100).toInt()}%", fontSize = 38.sp, fontWeight = FontWeight.Black)
            Text("score", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun feedbackFor(percent: Int): Pair<String, String> = when {
    percent >= 95 -> "A+" to "Outstanding! You really know this."
    percent >= 80 -> "A" to "Great work. Just a few to polish."
    percent >= 60 -> "B" to "Good progress. Review the ones you missed."
    percent >= 40 -> "C" to "Getting there. Another round will help."
    else -> "Go" to "A fresh start. Go through the notes and try again."
}
