package com.pronotes.app.editor

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.ntvien.liquidglass.GlassDefaults
import io.github.ntvien.liquidglass.component.LiquidSurface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

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
    LiquidSurface(
        style = GlassDefaults.frost(),
        shape = CircleShape,
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
