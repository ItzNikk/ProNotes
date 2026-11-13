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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pronotes.app.data.SettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class CompanionMeta(
    val id: String,
    val name: String,
    val element: String,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val quotes: List<String>,
)

val Companions: List<CompanionMeta> = listOf(
    CompanionMeta(
        id = "sparkchu", name = "Sparkchu", element = "Electric",
        primary = Color(0xFFFFD644), secondary = Color(0xFFF5A623), accent = Color(0xFFFF4D4D),
        quotes = listOf("Pika-focus! ⚡", "High voltage brain!", "Shockingly good notes!", "Charge up & solve! ⚡"),
    ),
    CompanionMeta(
        id = "pip", name = "Pip", element = "Dream",
        primary = Color(0xFFC9A4FF), secondary = Color(0xFF8B6CF0), accent = Color(0xFFFF8FB1),
        quotes = listOf("You've got this!", "Step by step ✨", "Proud of your focus!", "Nice rhythm today!"),
    ),
    CompanionMeta(
        id = "emberkit", name = "Emberkit", element = "Fire",
        primary = Color(0xFFFF8A4C), secondary = Color(0xFFE5484D), accent = Color(0xFFFFE066),
        quotes = listOf("On fire today! 🔥", "Blaze through that chapter!", "Warm up that streak!", "Keep the spark alive!"),
    ),
    CompanionMeta(
        id = "aquapup", name = "Aquapup", element = "Water",
        primary = Color(0xFF66D1FF), secondary = Color(0xFF2A78D6), accent = Color(0xFFCFF4FF),
        quotes = listOf("Smooth like water 💧", "Take a sip of water!", "Flow state unlocked!", "Calm & clear mind 🌊"),
    ),
    CompanionMeta(
        id = "leafsprout", name = "Leafsprout", element = "Leaf",
        primary = Color(0xFF7AE582), secondary = Color(0xFF1BAF7A), accent = Color(0xFFD8F5A2),
        quotes = listOf("Growing smarter! 🌱", "Fresh ideas blooming!", "Deep roots, sharp notes 🌿", "Nature loves curiosity!"),
    ),
    CompanionMeta(
        id = "eevo", name = "Eevo", element = "Adapt",
        primary = Color(0xFFE3B178), secondary = Color(0xFFB57C4A), accent = Color(0xFFFFF1D6),
        quotes = listOf("Adapt & master anything!", "Evolving every page! ✨", "Fluffy focus buddy!", "Ready for any quiz!"),
    ),
    CompanionMeta(
        id = "snorblob", name = "Snorblob", element = "Zen",
        primary = Color(0xFF4CA6B8), secondary = Color(0xFF275C70), accent = Color(0xFFF9EED2),
        quotes = listOf("Steady & calm 😌", "Deep breaths, slow & steady", "Big brain power...", "No rush, you got it!"),
    ),
    CompanionMeta(
        id = "genglet", name = "Genglet", element = "Shadow",
        primary = Color(0xFF8E63D9), secondary = Color(0xFF4C2C85), accent = Color(0xFFFF5C8A),
        quotes = listOf("Hehe! Mastered that! 👾", "Secret genius mode!", "Outsmarting the exam!", "Spooky fast solving!"),
    ),
)

fun companionById(id: String): CompanionMeta = Companions.firstOrNull { it.id == id } ?: Companions.first()

enum class AiBackdropVariant { AI_WAVE, SPACE_NEBULA, QUIZ_CHALLENGE, PEN_SETUP }

/**
 * Continuous 60fps Canvas backdrops matching our 4 studio visual themes:
 * - AI_WAVE: Futuristic blue tech particle data-flow digital wave + network links
 * - SPACE_NEBULA: Outer-space gas clouds, stars, and space dust streaming past camera
 * - QUIZ_CHALLENGE: Energetic challenge spotlight beams, pulse radar rings, & floating option tiles
 * - PEN_SETUP: Glowing blue light particles orbiting in opposite circles on solid black
 */
@Composable
fun AiParticleBackdrop(
    modifier: Modifier = Modifier,
    accentColors: List<Color> = listOf(Color(0xFF2563EB), Color(0xFF38BDF8)),
    active: Boolean = false,
    variant: AiBackdropVariant = AiBackdropVariant.AI_WAVE,
) {
    val t = rememberInfiniteTransition(label = "ai-bg")
    val durationMs = when (variant) {
        AiBackdropVariant.PEN_SETUP -> 7500
        AiBackdropVariant.AI_WAVE -> if (active) 7200 else 11500
        AiBackdropVariant.SPACE_NEBULA -> 16000
        AiBackdropVariant.QUIZ_CHALLENGE -> 9500
    }
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing)),
        label = "ai-phase",
    )
    val spaceStars = remember {
        val rnd = Random(6861)
        List(86) {
            floatArrayOf(
                (rnd.nextFloat() - 0.5f) * 2.1f, // x norm
                (rnd.nextFloat() - 0.5f) * 2.1f, // y norm
                rnd.nextFloat(),                  // z phase
                0.7f + rnd.nextFloat() * 1.9f,    // size
                if (rnd.nextFloat() < 0.25f) 1f else 0f, // dust mote vs star
                rnd.nextFloat(),                  // hue selector
            )
        }
    }
    val packets = remember {
        val rnd = Random(6667)
        List(14) {
            floatArrayOf(
                rnd.nextFloat(),
                (0 until 6).random(rnd).toFloat(),
                0.45f + rnd.nextFloat() * 0.65f,
                1.8f + rnd.nextFloat() * 1.8f,
            )
        }
    }
    val penParticles = remember {
        val rnd = Random(7717)
        val list = ArrayList<FloatArray>(48)
        for (ring in 0 until 3) {
            val count = 16
            val rFactor = 0.28f + ring * 0.13f
            val dir = if (ring % 2 == 0) 1f else -1f
            val speed = 1f + (2 - ring) * 0.5f
            for (i in 0 until count) {
                list += floatArrayOf(
                    (i.toFloat() / count) * (2.0 * PI).toFloat() + rnd.nextFloat() * 0.14f,
                    rFactor + (rnd.nextFloat() - 0.5f) * 0.025f,
                    dir,
                    speed,
                    1.6f + rnd.nextFloat() * 2.6f,
                    rnd.nextFloat() * (2.0 * PI).toFloat(),
                    ring.toFloat(),
                )
            }
        }
        list
    }
    val tilePaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    val c0 = accentColors.firstOrNull() ?: Color(0xFF2563EB)
    val c1 = accentColors.lastOrNull() ?: Color(0xFF38BDF8)

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val minDim = minOf(w, h)

        when (variant) {
            AiBackdropVariant.PEN_SETUP -> {
                drawRect(Color(0xFF02040A))
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF2563EB).copy(alpha = 0.24f), Color(0xFF0284C7).copy(alpha = 0.08f), Color.Transparent),
                        center = center,
                        radius = minDim * 0.68f,
                    ),
                    radius = minDim * 0.68f,
                    center = center,
                )
                for (ring in 0 until 3) {
                    val rr = minDim * (0.28f + ring * 0.13f)
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = if (ring == 1) 0.18f else 0.11f),
                        radius = rr,
                        center = center,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
                for (p in penParticles) {
                    val theta = p[0] + phase * p[2] * p[3]
                    val r = minDim * p[1]
                    val px = center.x + cos(theta) * r
                    val py = center.y + sin(theta) * r
                    val twinkle = 0.65f + 0.35f * sin(phase * 2f + p[5])
                    val dotCol = when (p[6].toInt()) {
                        0 -> Color(0xFFE0F2FE)
                        1 -> Color(0xFF38BDF8)
                        else -> Color(0xFF3B82F6)
                    }
                    // Luminous comet tail
                    var prev = Offset(px, py)
                    for (tStep in 1..4) {
                        val tTheta = theta - p[2] * tStep * 0.09f
                        val nextPt = Offset(center.x + cos(tTheta) * r, center.y + sin(tTheta) * r)
                        drawLine(
                            color = dotCol.copy(alpha = (0.42f - tStep * 0.09f) * twinkle),
                            start = prev,
                            end = nextPt,
                            strokeWidth = (p[4] * (1.1f - tStep * 0.18f)).dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                        prev = nextPt
                    }
                    val dotR = p[4].dp.toPx() * twinkle
                    drawCircle(dotCol.copy(alpha = 0.26f * twinkle), radius = dotR * 2.5f, center = Offset(px, py))
                    drawCircle(dotCol.copy(alpha = 0.95f * twinkle), radius = dotR, center = Offset(px, py))
                }
            }

            AiBackdropVariant.SPACE_NEBULA -> {
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color(0xFF030511), Color(0xFF070B1E), Color(0xFF0C0922)),
                    ),
                )
                val clouds = listOf(
                    Triple(0.24f, 0.30f, Color(0xFF3B43D6)),
                    Triple(0.76f, 0.26f, Color(0xFF0EA5E9)),
                    Triple(0.54f, 0.72f, Color(0xFF7C3AED)),
                    Triple(0.18f, 0.78f, Color(0xFF0891B2)),
                )
                clouds.forEachIndexed { idx, (fx, fy, col) ->
                    val cx = w * (fx + 0.05f * sin(phase + idx * 1.4f))
                    val cy = h * (fy + 0.05f * cos(phase * 0.9f + idx * 1.1f))
                    val cr = minDim * (0.58f + idx * 0.06f)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(col.copy(alpha = 0.22f), col.copy(alpha = 0.06f), Color.Transparent), center = Offset(cx, cy), radius = cr),
                        radius = cr,
                        center = Offset(cx, cy),
                    )
                }
                val progress = phase / (2.0 * PI).toFloat()
                for (s in spaceStars) {
                    val z = ((s[2] - progress) % 1f + 1f) % 1f
                    val scale = 0.12f + (1f - z) * 1.35f
                    val sx = center.x + s[0] * w * 0.56f * scale
                    val sy = center.y + s[1] * h * 0.56f * scale
                    if (sx < -24f || sx > w + 24f || sy < -24f || sy > h + 24f) continue
                    val alpha = ((1f - z) * 0.92f).coerceIn(0f, 0.95f)
                    if (s[4] > 0.5f) {
                        val dr = (s[3] * (1.2f + (1f - z) * 3.2f)).dp.toPx()
                        val col = if (s[5] > 0.5f) Color(0xFFA5F3FC) else Color(0xFFC4B5FD)
                        drawCircle(col.copy(alpha = alpha * 0.22f), radius = dr, center = Offset(sx, sy))
                    } else {
                        val sr = (s[3] * (0.45f + (1f - z) * 0.85f)).dp.toPx()
                        val prevZ = ((z + 0.035f) % 1f)
                        val prevScale = 0.12f + (1f - prevZ) * 1.35f
                        val px = center.x + s[0] * w * 0.56f * prevScale
                        val py = center.y + s[1] * h * 0.56f * prevScale
                        val starCol = if (s[5] > 0.6f) Color(0xFFBAE6FD) else Color(0xFFE0E7FF)
                        drawLine(starCol.copy(alpha = alpha * 0.5f), Offset(px, py), Offset(sx, sy), strokeWidth = sr, cap = StrokeCap.Round)
                        drawCircle(starCol.copy(alpha = alpha), radius = sr, center = Offset(sx, sy))
                    }
                }
            }

            AiBackdropVariant.QUIZ_CHALLENGE -> {
                // Soft ambient stage + rotating spotlights + concentric challenge pulse + floating A/B/C/D tiles
                val wash = Color(0xFF0EA5E9).copy(alpha = 0.09f)
                drawCircle(Brush.radialGradient(listOf(wash, Color.Transparent), center, minDim * 0.75f), minDim * 0.75f, center)
                val deg = (phase * 180f / PI.toFloat())
                rotate(deg, center) {
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color.Transparent, Color(0x280EA5E9), Color.Transparent, Color(0x24F59E0B), Color.Transparent), center),
                        startAngle = 0f, sweepAngle = 360f, useCenter = true,
                        topLeft = Offset(center.x - w * 0.65f, center.y - h * 0.65f),
                        size = Size(w * 1.3f, h * 1.3f),
                    )
                }
                val pulseProgress = phase / (2.0 * PI).toFloat()
                for (ring in 0 until 2) {
                    val rp = (pulseProgress + ring * 0.5f) % 1f
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = (1f - rp) * 0.22f),
                        radius = minDim * (0.12f + rp * 0.44f),
                        center = center,
                        style = Stroke(width = 1.6.dp.toPx()),
                    )
                }
                val labels = listOf("A", "B", "C", "D", "?", "✓")
                val bx = floatArrayOf(0.14f, 0.85f, 0.20f, 0.82f, 0.50f, 0.12f)
                val by = floatArrayOf(0.22f, 0.25f, 0.78f, 0.74f, 0.14f, 0.50f)
                val colors = listOf(Color(0xFF38BDF8), Color(0xFFFBBF24), Color(0xFF10B981), Color(0xFF60A5FA), Color(0xFF818CF8), Color(0xFF34D399))
                val cardW = minDim * 0.15f
                val cardH = minDim * 0.09f
                labels.forEachIndexed { idx, lbl ->
                    val tx = w * bx[idx] + sin(phase + idx * 1.3f) * 14.dp.toPx()
                    val ty = h * by[idx] + cos(phase * 1.15f + idx * 0.9f) * 10.dp.toPx()
                    val col = colors[idx]
                    val activeSel = ((pulseProgress * 6f).toInt() % 6) == idx
                    val alpha = if (activeSel) 0.34f else 0.16f
                    val topLeft = Offset(tx - cardW / 2f, ty - cardH / 2f)
                    drawRoundRect(
                        color = col.copy(alpha = alpha * 0.45f),
                        topLeft = topLeft,
                        size = Size(cardW, cardH),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                    )
                    drawRoundRect(
                        color = col.copy(alpha = alpha * 1.2f),
                        topLeft = topLeft,
                        size = Size(cardW, cardH),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                        style = Stroke(width = (if (activeSel) 1.8f else 1f).dp.toPx()),
                    )
                    tilePaint.textSize = cardH * 0.54f
                    tilePaint.color = android.graphics.Color.argb(
                        ((alpha + 0.24f) * 255).toInt().coerceIn(40, 210),
                        (col.red * 255).toInt(),
                        (col.green * 255).toInt(),
                        (col.blue * 255).toInt(),
                    )
                    drawContext.canvas.nativeCanvas.drawText(lbl, tx, ty + cardH * 0.19f, tilePaint)
                }
            }

            AiBackdropVariant.AI_WAVE -> {
                val glowAlpha = if (active) 0.24f else 0.12f
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFF0284C7).copy(alpha = glowAlpha), Color.Transparent)),
                    radius = minDim * 0.72f,
                    center = Offset(w * 0.26f, h * 0.25f),
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(c0.copy(alpha = glowAlpha), Color.Transparent)),
                    radius = minDim * 0.72f,
                    center = Offset(w * 0.76f, h * 0.78f),
                )

                val cols = 16
                val rows = 6
                val pts = Array(rows) { Array(cols) { Offset.Zero } }
                val alphaScale = if (active) 1f else 0.6f
                for (r in 0 until rows) {
                    val depth = r.toFloat() / (rows - 1)
                    val baseY = h * (0.28f + depth * 0.58f)
                    val amp = minDim * (0.055f + depth * 0.045f) * (if (active) 1.25f else 0.9f)
                    for (c in 0 until cols) {
                        val u = c.toFloat() / (cols - 1)
                        val wave = sin(u * 5.2f + phase * 1.4f + r * 0.75f) + 0.45f * cos(u * 3.1f - phase * 1.1f - r * 0.5f)
                        val x = w * (-0.03f + u * 1.06f) + sin(phase + r) * 6.dp.toPx()
                        val y = baseY + wave * amp
                        pts[r][c] = Offset(x, y)
                    }
                }
                for (r in 0 until rows) {
                    val depth = r.toFloat() / (rows - 1)
                    val lineAlpha = (0.10f + depth * 0.26f) * alphaScale
                    for (c in 0 until cols) {
                        val a = pts[r][c]
                        if (c + 1 < cols) {
                            drawLine(Color(0xFF38BDF8).copy(alpha = lineAlpha), a, pts[r][c + 1], strokeWidth = 1.dp.toPx())
                        }
                        if (r + 1 < rows) {
                            drawLine(Color(0xFF2563EB).copy(alpha = lineAlpha * 0.65f), a, pts[r + 1][c], strokeWidth = 0.85.dp.toPx())
                        }
                        val pulse = 0.65f + 0.35f * sin(phase * 2.2f + c * 0.5f + r)
                        val rad = ((1.2f + depth * 1.8f) * pulse).dp.toPx()
                        drawCircle(c1.copy(alpha = (0.20f + depth * 0.55f) * alphaScale), radius = rad, center = a)
                    }
                }
                val tNorm = phase / (2.0 * PI).toFloat()
                for (pk in packets) {
                    val rowIdx = pk[1].toInt().coerceIn(0, rows - 1)
                    val u = (pk[0] + tNorm * pk[2]) % 1f
                    val colFloat = u * (cols - 1)
                    val c0Idx = colFloat.toInt().coerceIn(0, cols - 2)
                    val frac = colFloat - c0Idx
                    val p0 = pts[rowIdx][c0Idx]
                    val p1 = pts[rowIdx][c0Idx + 1]
                    val x = p0.x + (p1.x - p0.x) * frac
                    val y = p0.y + (p1.y - p0.y) * frac
                    val rPx = pk[3].dp.toPx()
                    drawCircle(Color(0xFF7DD3FC).copy(alpha = 0.28f * alphaScale), radius = rPx * 2.6f, center = Offset(x, y))
                    drawCircle(Color(0xFFE0F2FE).copy(alpha = 0.92f * alphaScale), radius = rPx, center = Offset(x, y))
                }
            }
        }
    }
}

/** Animated AI status pill with orbital glow, diagonal shimmer sweep, live stage & model badge. */
@Composable
fun ThinkingPill(engine: String = "", stage: String = "") {
    val t = rememberInfiniteTransition(label = "think")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1050, easing = LinearEasing)), label = "p")
    val shine by t.animateFloat(-0.6f, 1.8f, infiniteRepeatable(tween(1550, easing = LinearEasing)), label = "s")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2100, easing = LinearEasing)), label = "spin")
    val pulse by t.animateFloat(0.8f, 1.08f, infiniteRepeatable(tween(820, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    Surface(
        shape = CircleShape,
        color = Color(0xF5151926),
        shadowElevation = 12.dp,
        modifier = Modifier.border(1.dp, Color(0x55B45CFF), CircleShape),
    ) {
        Box(Modifier.clip(CircleShape)) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color(0x48B45CFF), Color(0x405AC8FA), Color.Transparent),
                        Offset(size.width * shine - 80f, 0f),
                        Offset(size.width * shine + 80f, size.height),
                    ),
                )
            }
            Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(22.dp).graphicsLayer { rotationZ = spin; scaleX = pulse; scaleY = pulse }) {
                    val r = size.minDimension / 2f
                    drawCircle(
                        brush = Brush.sweepGradient(listOf(Color(0xFFE38BFF), Color(0xFF5AC8FA), Color(0xFF1BAF7A), Color(0xFFE38BFF))),
                        radius = r - 2f,
                        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawCircle(Color(0xFFE38BFF), radius = 2.8.dp.toPx(), center = center)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stage.ifBlank { "Thinking" },
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.width(6.dp))
                        for (i in 0 until 3) {
                            val k = ((phase * 3f - i) % 3f + 3f) % 3f
                            val up = if (k < 1f) sin(k * PI).toFloat() else 0f
                            Box(Modifier.padding(horizontal = 2.dp).size(5.dp).graphicsLayer { translationY = -up * 6.dp.toPx() }) {
                                Canvas(Modifier.size(5.dp)) { drawCircle(if (i % 2 == 0) Color(0xFFE38BFF) else Color(0xFF74C0FC)) }
                            }
                        }
                    }
                    if (engine.isNotBlank()) {
                        Text(engine, color = Color(0xFFAEC0D8), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun StudyPet(vm: EditorViewModel, modifier: Modifier, compact: Boolean = false) {
    val settings by vm.settingsFlow.collectAsState()
    val pet = companionById(settings.petId)
    val displayName = settings.petName.ifBlank { pet.name }
    val level = 1 + settings.petXp / 100

    val t = rememberInfiniteTransition(label = "pet")
    val breathe by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    val aiSpin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "ai")
    var blink by remember { mutableStateOf(false) }
    var say by remember { mutableStateOf<String?>(null) }
    var hubOpen by remember { mutableStateOf(false) }
    val jump = remember { Animatable(0f) }
    val wiggle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { while (true) { delay(2400L + (0..2200).random()); blink = true; delay(130); blink = false } }

    fun react(msg: String?, xp: Int = 0) = scope.launch {
        if (xp > 0) vm.recordPetXp(xp)
        say = msg
        jump.snapTo(0f); jump.animateTo(1f, tween(170)); jump.animateTo(0f, spring(dampingRatio = 0.36f, stiffness = 320f))
        for (k in listOf(1f, -1f, 0.5f, -0.5f, 0f)) wiggle.animateTo(k, tween(65))
        delay(2200)
        if (say == msg) say = null
    }

    LaunchedEffect(vm.toast) {
        val m = vm.toast ?: return@LaunchedEffect
        if (m.startsWith("Added") || m.startsWith("Saved") || m.startsWith("Generated") || m.startsWith("Switched")) {
            react(pet.quotes.random())
        }
    }
    var sleepy by remember { mutableStateOf(false) }
    LaunchedEffect(vm.pageIndex) { if ((0..2).random() == 0) react(null) }
    LaunchedEffect(vm.lastActivity) { sleepy = false; delay(65_000); sleepy = true }

    val writing = vm.penActive
    val thinking = vm.aiBusy

    Box(modifier) {
        Column(
            Modifier.graphicsLayer { alpha = if (writing && !compact) 0.35f else 1f },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val bubble = say ?: if (thinking) "Thinking…" else if (sleepy) "zzz…" else null
            if (bubble != null && !hubOpen) {
                val content = @Composable {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
                        Text(bubble, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (compact) Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, 110)) { content() } else content()
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(
                    Modifier.size(if (compact) 36.dp else 58.dp).graphicsLayer {
                        translationY = -jump.value * 16.dp.toPx()
                        rotationZ = wiggle.value * 12f
                    }.clickable(remember { MutableInteractionSource() }, null) {
                        hubOpen = !hubOpen
                        if (!hubOpen) react("$displayName: ${pet.quotes.random()}", xp = 2)
                    },
                ) {
                    drawCompanion(
                        pet = pet,
                        breathe = breathe,
                        jump = jump.value,
                        blink = blink || (sleepy && say == null),
                        writing = writing,
                        thinking = thinking,
                        aiSpin = aiSpin,
                        compact = compact,
                    )
                }
                if (!compact) {
                    Spacer(Modifier.width(4.dp))
                    Surface(shape = CircleShape, color = pet.secondary.copy(alpha = 0.18f)) {
                        Text("$displayName · Lv.$level", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = pet.secondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (hubOpen) {
            CompanionHubPopup(
                pet = pet,
                petName = settings.petName,
                petXp = settings.petXp,
                breathe = breathe,
                jump = jump.value,
                blink = blink,
                writing = writing,
                thinking = thinking,
                aiSpin = aiSpin,
                onDismiss = { hubOpen = false },
                onReact = { msg, xp -> react(msg, xp) },
                onSwitchPet = { id -> vm.switchPet(id) },
                onRenamePet = { name -> vm.setPetName(name) },
            )
        }
    }
}

/** Standalone interactive Companion Pill usable on Library, Settings, Welcome, Support & Study screens. */
@Composable
fun CompanionBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    onDarkBackground: Boolean = false,
) {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val settings by store.settings.collectAsState()
    if (!settings.pet) return
    val pet = companionById(settings.petId)
    val displayName = settings.petName.ifBlank { pet.name }
    val level = 1 + settings.petXp / 100

    val t = rememberInfiniteTransition(label = "cbadge")
    val breathe by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "cb")
    var blink by remember { mutableStateOf(false) }
    var hubOpen by remember { mutableStateOf(false) }
    var bubble by remember { mutableStateOf<String?>(null) }
    val jump = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { while (true) { delay(2600L + (0..2400).random()); blink = true; delay(130); blink = false } }

    fun react(msg: String?, xp: Int = 0) = scope.launch {
        if (xp > 0) store.addPetXp(xp)
        bubble = msg
        jump.snapTo(0f); jump.animateTo(1f, tween(160)); jump.animateTo(0f, spring(dampingRatio = 0.38f, stiffness = 320f))
        delay(2100)
        if (bubble == msg) bubble = null
    }

    Box(modifier) {
        Surface(
            shape = CircleShape,
            color = if (onDarkBackground) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .border(1.dp, if (onDarkBackground) Color.White.copy(alpha = 0.35f) else pet.secondary.copy(alpha = 0.3f), CircleShape)
                .clip(CircleShape)
                .clickable { hubOpen = !hubOpen },
        ) {
            Row(
                Modifier.padding(start = 6.dp, end = if (compact) 10.dp else 12.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(26.dp).graphicsLayer { translationY = -jump.value * 8.dp.toPx() }) {
                    drawCompanion(pet, breathe, jump.value, blink, writing = false, thinking = false, aiSpin = 0f, compact = true)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    if (compact) "$displayName · Lv.$level" else "$displayName · ${pet.element} Lv.$level",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (onDarkBackground) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (bubble != null && !hubOpen) {
            Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, 96)) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
                    Text(bubble!!, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (hubOpen) {
            CompanionHubPopup(
                pet = pet,
                petName = settings.petName,
                petXp = settings.petXp,
                breathe = breathe,
                jump = jump.value,
                blink = blink,
                writing = false,
                thinking = false,
                aiSpin = 0f,
                onDismiss = { hubOpen = false },
                onReact = { msg, xp -> react(msg, xp) },
                onSwitchPet = { id -> store.update { it.copy(petId = id) } },
                onRenamePet = { name -> store.update { it.copy(petName = name.trim()) } },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompanionHubPopup(
    pet: CompanionMeta,
    petName: String,
    petXp: Int,
    breathe: Float,
    jump: Float,
    blink: Boolean,
    writing: Boolean,
    thinking: Boolean,
    aiSpin: Float,
    onDismiss: () -> Unit,
    onReact: (String?, Int) -> Unit,
    onSwitchPet: (String) -> Unit,
    onRenamePet: (String) -> Unit,
) {
    val level = 1 + petXp / 100
    val levelProgress = (petXp % 100) / 100f
    val displayName = petName.ifBlank { pet.name }
    var editingName by remember { mutableStateOf(false) }
    var draftName by remember(petName, pet.id) { mutableStateOf(petName.ifBlank { pet.name }) }

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(0, 108),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
            modifier = Modifier.widthIn(min = 296.dp, max = 356.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(56.dp)) {
                        drawCompanion(pet, breathe, jump, blink, writing, thinking, aiSpin, false)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        if (editingName) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(
                                    draftName,
                                    { draftName = it.take(18) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    placeholder = { Text(pet.name) },
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = {
                                        val clean = draftName.trim()
                                        onRenamePet(if (clean == pet.name) "" else clean)
                                        editingName = false
                                        onReact("Nice name! ♥", 3)
                                    },
                                    modifier = Modifier.size(34.dp),
                                ) { Icon(Icons.Outlined.Check, "Save name") }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                IconButton(onClick = { editingName = true; draftName = displayName }, modifier = Modifier.size(26.dp)) {
                                    Icon(Icons.Outlined.Edit, "Rename buddy", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(shape = CircleShape, color = pet.primary.copy(alpha = 0.25f)) {
                                    Text("${pet.element} · Lv.$level", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Text("$petXp Bond XP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { levelProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = pet.secondary,
                        )
                    }
                    TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Close") }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onReact("Loves the head pats! ♥", 5) }, shape = CircleShape, modifier = Modifier.weight(1f)) {
                        Text("Pat +5", fontSize = 12.sp)
                    }
                    FilledTonalButton(onClick = { onReact("Yum! Berry boost! 🍓", 15) }, shape = CircleShape, modifier = Modifier.weight(1f)) {
                        Text("Treat +15", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { onReact(pet.quotes.random(), 8) }, shape = CircleShape, modifier = Modifier.weight(1f)) {
                        Text("Cheer", fontSize = 12.sp)
                    }
                }

                Text("SWITCH COMPANION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Companions.forEach { c ->
                        val active = c.id == pet.id
                        Column(
                            Modifier.width(70.dp).clip(RoundedCornerShape(14.dp))
                                .background(if (active) c.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .border(if (active) 1.5.dp else 0.dp, if (active) c.secondary else Color.Transparent, RoundedCornerShape(14.dp))
                                .clickable { onSwitchPet(c.id); onReact(c.quotes.first(), 3) }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Canvas(Modifier.size(30.dp)) {
                                drawCompanion(c, breathe, 0f, false, false, false, 0f, true)
                            }
                            Text(c.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

fun DrawScope.drawCompanion(
    pet: CompanionMeta,
    breathe: Float,
    jump: Float,
    blink: Boolean,
    writing: Boolean,
    thinking: Boolean,
    aiSpin: Float,
    compact: Boolean,
) {
    val w = size.width
    val h = size.height
    if (thinking) {
        rotate(aiSpin, pivot = Offset(w / 2f, h * 0.56f)) {
            drawCircle(
                brush = Brush.sweepGradient(listOf(Color(0xFFE38BFF), Color(0xFF5AC8FA), Color.Transparent, Color(0xFFE38BFF))),
                radius = w * 0.56f,
                center = Offset(w / 2f, h * 0.56f),
                style = Stroke(width = if (compact) 2f else 3.5f),
            )
        }
    }
    val squish = 1f + 0.04f * breathe - jump * 0.08f
    val bodyH = h * 0.72f * squish
    val bodyW = w * 0.84f / squish
    val left = (w - bodyW) / 2f
    val top = h - bodyH

    // Ambient companion aura
    drawCircle(
        brush = Brush.radialGradient(listOf(pet.primary.copy(alpha = 0.24f), Color.Transparent)),
        radius = w * 0.52f,
        center = Offset(w * 0.5f, top + bodyH * 0.52f),
    )

    // Soft shadow
    drawOval(Color(0x26000000), Offset(w * 0.2f, h - 5f), Size(w * 0.6f, 6.5f))

    // Species-specific tails behind the body
    when (pet.id) {
        "sparkchu" -> {
            val bolt = Path().apply {
                moveTo(w * 0.78f, top + bodyH * 0.66f)
                lineTo(w * 0.94f, top + bodyH * 0.44f)
                lineTo(w * 0.86f, top + bodyH * 0.42f)
                lineTo(w * 0.98f, top + bodyH * 0.22f)
                lineTo(w * 0.85f, top + bodyH * 0.34f)
                lineTo(w * 0.75f, top + bodyH * 0.52f)
                close()
            }
            drawPath(bolt, pet.secondary)
        }
        "emberkit" -> {
            val tail = Path().apply {
                moveTo(w * 0.78f, top + bodyH * 0.68f)
                quadraticTo(w * 0.98f, top + bodyH * 0.48f, w * 0.91f, top + bodyH * 0.26f)
                quadraticTo(w * 0.80f, top + bodyH * 0.42f, w * 0.74f, top + bodyH * 0.58f)
                close()
            }
            drawPath(tail, pet.accent)
        }
        "aquapup" -> {
            val tailFin = Path().apply {
                moveTo(w * 0.80f, top + bodyH * 0.65f)
                quadraticTo(w * 0.98f, top + bodyH * 0.50f, w * 0.96f, top + bodyH * 0.75f)
                close()
            }
            drawPath(tailFin, pet.secondary)
        }
    }

    // Creature-specific ears / crests behind body
    when (pet.id) {
        "sparkchu", "eevo" -> {
            val lEar = Path().apply { moveTo(w * 0.24f, top + bodyH * 0.2f); lineTo(w * 0.13f, top - h * 0.19f); lineTo(w * 0.38f, top + bodyH * 0.08f); close() }
            val rEar = Path().apply { moveTo(w * 0.76f, top + bodyH * 0.2f); lineTo(w * 0.87f, top - h * 0.19f); lineTo(w * 0.62f, top + bodyH * 0.08f); close() }
            drawPath(lEar, pet.primary)
            drawPath(rEar, pet.primary)
            if (pet.id == "sparkchu") {
                val lTip = Path().apply { moveTo(w * 0.18f, top - h * 0.06f); lineTo(w * 0.13f, top - h * 0.19f); lineTo(w * 0.26f, top - h * 0.07f); close() }
                val rTip = Path().apply { moveTo(w * 0.82f, top - h * 0.06f); lineTo(w * 0.87f, top - h * 0.19f); lineTo(w * 0.74f, top - h * 0.07f); close() }
                drawPath(lTip, Color(0xFF2B2F3A))
                drawPath(rTip, Color(0xFF2B2F3A))
            } else {
                val lIn = Path().apply { moveTo(w * 0.24f, top + bodyH * 0.16f); lineTo(w * 0.17f, top - h * 0.11f); lineTo(w * 0.33f, top + bodyH * 0.08f); close() }
                val rIn = Path().apply { moveTo(w * 0.76f, top + bodyH * 0.16f); lineTo(w * 0.83f, top - h * 0.11f); lineTo(w * 0.67f, top + bodyH * 0.08f); close() }
                drawPath(lIn, pet.secondary)
                drawPath(rIn, pet.secondary)
            }
        }
        "leafsprout" -> {
            val lLeaf = Path().apply {
                moveTo(w * 0.5f, top + 3f)
                quadraticTo(w * 0.28f, top - h * 0.24f, w * 0.48f, top - h * 0.04f)
                close()
            }
            val rLeaf = Path().apply {
                moveTo(w * 0.5f, top + 3f)
                quadraticTo(w * 0.76f, top - h * 0.22f, w * 0.52f, top - h * 0.03f)
                close()
            }
            drawPath(lLeaf, Color(0xFF1BAF7A))
            drawPath(rLeaf, Color(0xFF7AE582))
        }
        "emberkit" -> {
            val flame = Path().apply {
                moveTo(w * 0.39f, top + 4f)
                quadraticTo(w * 0.5f, top - h * 0.24f, w * 0.61f, top + 4f)
                close()
            }
            drawPath(flame, Color(0xFFFFE066))
        }
        "aquapup" -> {
            val lFin = Path().apply { moveTo(w * 0.25f, top + bodyH * 0.24f); lineTo(w * 0.11f, top + bodyH * 0.06f); lineTo(w * 0.33f, top + bodyH * 0.12f); close() }
            val rFin = Path().apply { moveTo(w * 0.75f, top + bodyH * 0.24f); lineTo(w * 0.89f, top + bodyH * 0.06f); lineTo(w * 0.67f, top + bodyH * 0.12f); close() }
            drawPath(lFin, pet.accent)
            drawPath(rFin, pet.accent)
        }
        "pip" -> {
            val crest = Path().apply {
                moveTo(w * 0.44f, top + 3f); lineTo(w * 0.5f, top - h * 0.16f); lineTo(w * 0.56f, top + 3f); close()
            }
            drawPath(crest, pet.accent)
        }
        "genglet" -> {
            for (dx in listOf(-0.22f, 0f, 0.22f)) {
                val spike = Path().apply {
                    moveTo(w * (0.43f + dx), top + bodyH * 0.15f)
                    lineTo(w * (0.5f + dx), top - h * 0.12f)
                    lineTo(w * (0.57f + dx), top + bodyH * 0.15f)
                    close()
                }
                drawPath(spike, pet.secondary)
            }
        }
    }

    // Main body + crisp outline + inner specular
    drawOval(Brush.verticalGradient(listOf(pet.primary, pet.secondary), top, h), Offset(left, top), Size(bodyW, bodyH))
    drawOval(Color(0x28000000), Offset(left, top), Size(bodyW, bodyH), style = Stroke(width = if (compact) 1.2f else 2f))
    drawOval(Color(0x55FFFFFF), Offset(w * 0.26f, top + bodyH * 0.09f), Size(bodyW * 0.3f, bodyH * 0.16f))

    // Belly / fluffy ruff
    if (pet.id in setOf("snorblob", "eevo", "aquapup")) {
        drawOval(pet.accent.copy(alpha = 0.78f), Offset(w * 0.28f, top + bodyH * 0.52f), Size(bodyW * 0.52f, bodyH * 0.42f))
    }

    // Rosy cheeks
    drawCircle(pet.accent.copy(alpha = 0.82f), w * 0.082f, Offset(w * 0.25f, top + bodyH * 0.6f))
    drawCircle(pet.accent.copy(alpha = 0.82f), w * 0.082f, Offset(w * 0.75f, top + bodyH * 0.6f))

    // Glossy eyes with dual sparkle highlights
    val ey = top + bodyH * (if (writing) 0.5f else if (thinking) 0.36f else 0.44f)
    val ex = if (thinking) w * 0.035f else 0f
    val eyeColor = if (pet.id == "genglet") Color(0xFFFF3B6B) else Color(0xFF1F2430)
    for (x in listOf(w * 0.37f, w * 0.63f)) {
        if (blink) {
            drawLine(eyeColor, Offset(x - w * 0.055f + ex, ey), Offset(x + w * 0.055f + ex, ey), if (compact) 2.2f else 3f, StrokeCap.Round)
        } else {
            drawCircle(eyeColor, w * 0.062f, Offset(x + ex, ey))
            drawCircle(Color.White, w * 0.023f, Offset(x + ex + w * 0.018f, ey - w * 0.018f))
            if (!compact) drawCircle(Color.White.copy(alpha = 0.7f), w * 0.011f, Offset(x + ex - w * 0.018f, ey + w * 0.018f))
        }
    }

    // Mouth
    if (thinking) {
        drawCircle(Color(0xFF232734), w * 0.03f, Offset(w / 2f, top + bodyH * 0.66f), style = Stroke(2.2f))
    } else if (pet.id == "genglet") {
        val grin = Path().apply {
            moveTo(w * 0.36f, top + bodyH * 0.62f)
            quadraticTo(w / 2f, top + bodyH * 0.78f, w * 0.64f, top + bodyH * 0.62f)
        }
        drawPath(grin, Color.White, style = Stroke(if (compact) 2.6f else 3.4f, cap = StrokeCap.Round))
    } else {
        val smile = Path().apply {
            moveTo(w * 0.42f, top + bodyH * 0.62f)
            quadraticTo(w / 2f, top + bodyH * (0.72f + 0.06f * jump), w * 0.58f, top + bodyH * 0.62f)
        }
        drawPath(smile, Color(0xFF232734), style = Stroke(if (compact) 2f else 2.5f, cap = StrokeCap.Round))
    }
}
