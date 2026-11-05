package com.pronotes.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.pronotes.app.editor.Tool

/**
 * Pro Notes' own tool glyphs, drawn as upright instruments. Pens show their
 * current ink colour at the tip.
 */
@Composable
fun ToolGlyph(tool: Tool, tint: Color, inkColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val sw = w * 0.08f
        val line = Stroke(width = sw, cap = StrokeCap.Round)
        when (tool) {
            Tool.PEN -> instrument(tint, inkColor, tipPointed = true)
            Tool.HIGHLIGHTER -> instrument(tint, inkColor.copy(alpha = 1f), tipPointed = false)
            Tool.ERASER -> rotate(35f) {
                drawRoundRect(tint, Offset(w * 0.3f, h * 0.12f), Size(w * 0.4f, h * 0.76f), CornerRadius(w * 0.1f), style = line)
                drawRoundRect(tint.copy(alpha = 0.35f), Offset(w * 0.3f, h * 0.58f), Size(w * 0.4f, h * 0.3f), CornerRadius(w * 0.1f))
            }
            Tool.SHAPE -> {
                drawCircle(tint, w * 0.22f, Offset(w * 0.38f, h * 0.38f), style = line)
                drawRoundRect(tint, Offset(w * 0.42f, h * 0.42f), Size(w * 0.42f, h * 0.42f), CornerRadius(w * 0.06f), style = line)
            }
            Tool.LASSO -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.14f)
                    cubicTo(w * 0.95f, h * 0.14f, w * 0.95f, h * 0.66f, w * 0.5f, h * 0.66f)
                    cubicTo(w * 0.05f, h * 0.66f, w * 0.05f, h * 0.14f, w * 0.5f, h * 0.14f)
                }
                drawPath(p, tint, style = Stroke(sw, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.1f, w * 0.08f))))
                drawLine(tint, Offset(w * 0.4f, h * 0.66f), Offset(w * 0.32f, h * 0.9f), sw, StrokeCap.Round)
            }
            Tool.IMAGE -> {
                drawRoundRect(tint, Offset(w * 0.14f, h * 0.2f), Size(w * 0.72f, h * 0.6f), CornerRadius(w * 0.08f), style = line)
                val m = Path().apply {
                    moveTo(w * 0.22f, h * 0.72f); lineTo(w * 0.42f, h * 0.48f); lineTo(w * 0.56f, h * 0.62f)
                    lineTo(w * 0.66f, h * 0.52f); lineTo(w * 0.8f, h * 0.72f); close()
                }
                drawPath(m, tint.copy(alpha = 0.6f))
                drawCircle(tint, w * 0.06f, Offset(w * 0.66f, h * 0.34f))
            }
            Tool.TEXT -> {
                drawLine(tint, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.22f), sw * 1.3f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.22f), Offset(w * 0.5f, h * 0.82f), sw * 1.3f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.38f, h * 0.82f), Offset(w * 0.62f, h * 0.82f), sw, StrokeCap.Round)
            }
            Tool.LASER -> {
                rotate(35f) {
                    drawRoundRect(tint, Offset(w * 0.42f, h * 0.34f), Size(w * 0.16f, h * 0.56f), CornerRadius(w * 0.08f), style = line)
                }
                drawCircle(Color(0xFFFF3B30), w * 0.11f, Offset(w * 0.3f, h * 0.24f))
                drawCircle(Color(0x55FF3B30), w * 0.2f, Offset(w * 0.3f, h * 0.24f))
            }
        }
    }
}

private fun DrawScope.instrument(body: Color, ink: Color, tipPointed: Boolean) {
    val w = size.width; val h = size.height
    rotate(35f) {
        val bodyLeft = w * 0.38f; val bodyRight = w * 0.62f
        drawRoundRect(body, Offset(bodyLeft, h * 0.34f), Size(bodyRight - bodyLeft, h * 0.6f), CornerRadius(w * 0.05f))
        val tip = Path().apply {
            if (tipPointed) {
                moveTo(bodyLeft, h * 0.36f); lineTo(w * 0.5f, h * 0.06f); lineTo(bodyRight, h * 0.36f); close()
            } else {
                moveTo(bodyLeft, h * 0.36f); lineTo(bodyLeft + w * 0.03f, h * 0.12f); lineTo(bodyRight, h * 0.08f); lineTo(bodyRight, h * 0.36f); close()
            }
        }
        drawPath(tip, ink)
        drawRect(body.copy(alpha = 0.25f), Offset(bodyLeft, h * 0.34f), Size(bodyRight - bodyLeft, h * 0.05f))
    }
}
