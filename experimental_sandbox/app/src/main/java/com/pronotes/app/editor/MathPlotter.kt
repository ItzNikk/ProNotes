package com.pronotes.app.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.abs

/**
 * Samples a mathematical function `y = f(x)` over `[xMin, xMax]` into screen-ready segments,
 * breaking the path at discontinuities (e.g. `1/x`, `tan(x)`).
 */
object MathPlotSampler {

    data class PlotPoint(val x: Double, val y: Double)

    fun sample(
        expression: String,
        xMin: Double = -10.0,
        xMax: Double = 10.0,
        yMin: Double = -10.0,
        yMax: Double = 10.0,
        steps: Int = 240,
    ): List<List<PlotPoint>> {
        if (steps <= 1 || xMax <= xMin) return emptyList()
        val segments = mutableListOf<List<PlotPoint>>()
        var current = mutableListOf<PlotPoint>()
        val dx = (xMax - xMin) / steps
        val ySpan = (yMax - yMin).coerceAtLeast(1e-6)

        var prevY: Double? = null
        for (i in 0..steps) {
            val x = xMin + i * dx
            val y = MathEvaluator.evaluateWithVariable(expression, "x", x)
            val valid = !y.isNaN() && !y.isInfinite() && y >= yMin - ySpan * 0.5 && y <= yMax + ySpan * 0.5
            if (!valid) {
                if (current.size >= 2) segments.add(current)
                current = mutableListOf()
                prevY = null
                continue
            }
            if (prevY != null && abs(y - prevY) > ySpan * 0.85) {
                // Likely a vertical asymptote / discontinuity
                if (current.size >= 2) segments.add(current)
                current = mutableListOf()
            }
            current.add(PlotPoint(x, y))
            prevY = y
        }
        if (current.size >= 2) segments.add(current)
        return segments
    }
}

/**
 * Interactive function plotter dialog for `y = f(x)` expressions.
 */
@Composable
fun MathPlotDialog(
    initialExpression: String = "sin(x)",
    onDismiss: () -> Unit,
) {
    var expr by remember { mutableStateOf(initialExpression) }
    val xMin = -10.0
    val xMax = 10.0
    val yMin = -6.0
    val yMax = 6.0

    val segments = remember(expr) {
        MathPlotSampler.sample(expr, xMin = xMin, xMax = xMax, yMin = yMin, yMax = yMax)
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Quick Function Plotter",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = expr,
                onValueChange = { expr = it },
                label = { Text("y = f(x)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            val curveColor = MaterialTheme.colorScheme.primary

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(1.dp, gridColor, RoundedCornerShape(14.dp))
            ) {
                val w = size.width
                val h = size.height

                fun toScreenX(x: Double): Float = ((x - xMin) / (xMax - xMin) * w).toFloat()
                fun toScreenY(y: Double): Float = (h - (y - yMin) / (yMax - yMin) * h).toFloat()

                // Gridlines
                for (gx in -10..10 step 2) {
                    val sx = toScreenX(gx.toDouble())
                    drawLine(gridColor, Offset(sx, 0f), Offset(sx, h), strokeWidth = 1f)
                }
                for (gy in -6..6 step 2) {
                    val sy = toScreenY(gy.toDouble())
                    drawLine(gridColor, Offset(0f, sy), Offset(w, sy), strokeWidth = 1f)
                }

                // Axes
                val originX = toScreenX(0.0)
                val originY = toScreenY(0.0)
                drawLine(axisColor, Offset(0f, originY), Offset(w, originY), strokeWidth = 2f)
                drawLine(axisColor, Offset(originX, 0f), Offset(originX, h), strokeWidth = 2f)

                // Function curve
                segments.forEach { seg ->
                    val path = Path()
                    seg.forEachIndexed { idx, pt ->
                        val sx = toScreenX(pt.x)
                        val sy = toScreenY(pt.y)
                        if (idx == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
                    }
                    drawPath(
                        path = path,
                        color = curveColor,
                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}
