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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.pronotes.app.editor.PenType
import com.pronotes.app.editor.ShapeKind
import com.pronotes.app.editor.ShapeBuilder
import com.pronotes.app.editor.Tool

/*
 * Pro Notes' own thin-line tool glyphs, drawn on a 24-unit grid so they sit
 * comfortably next to Material icons. Pens show their ink colour at the tip.
 */

private fun DrawScope.u() = size.minDimension / 24f
private fun DrawScope.p(x: Float, y: Float) = Offset(x * u(), y * u())
private fun DrawScope.line(tint: Color) = Stroke(width = 1.7f * u(), cap = StrokeCap.Round, join = StrokeJoin.Round)
private fun DrawScope.path(block: Path.(Float) -> Unit) = Path().apply { block(u()) }

@Composable
fun ToolGlyph(tool: Tool, tint: Color, inkColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = line(tint)
        when (tool) {
            Tool.PEN -> drawPen(tint, inkColor, PenType.FOUNTAIN)
            Tool.HIGHLIGHTER -> {
                drawPath(path { k -> moveTo(7f * k, 20f * k); lineTo(5f * k, 18f * k); lineTo(15f * k, 4f * k); lineTo(20f * k, 9f * k); lineTo(9f * k, 21f * k); close() }, tint, style = s)
                drawPath(path { k -> moveTo(5f * k, 18f * k); lineTo(3f * k, 22f * k); lineTo(8f * k, 21f * k) }, inkColor, style = s)
                drawLine(tint, p(9.5f, 11f), p(14f, 15.5f), s.width, StrokeCap.Round)
            }
            Tool.ERASER -> {
                drawPath(path { k ->
                    moveTo(8f * k, 20f * k); lineTo(3.5f * k, 15.5f * k); lineTo(13.5f * k, 5.5f * k); lineTo(19.5f * k, 11.5f * k); lineTo(11f * k, 20f * k); close()
                }, tint, style = s)
                drawLine(tint, p(7.5f, 11.5f), p(13.5f, 17.5f), s.width, StrokeCap.Round)
                drawLine(tint, p(8f, 20f), p(20f, 20f), s.width, StrokeCap.Round)
            }
            Tool.SHAPE -> {
                drawCircle(tint, 4.5f * u(), p(8f, 8f), style = s)
                drawRoundRect(tint, p(11f, 11f), Size(9f * u(), 9f * u()), CornerRadius(1.5f * u()), style = s)
                drawPath(path { k -> moveTo(4f * k, 21f * k); lineTo(7f * k, 15f * k); lineTo(10f * k, 21f * k); close() }, tint, style = s)
            }
            Tool.LASSO -> {
                drawOval(tint, p(3f, 3f), Size(18f * u(), 12f * u()),
                    style = Stroke(1.7f * u(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.6f * u(), 2.4f * u()))))
                drawPath(path { k -> moveTo(9f * k, 15f * k); cubicTo(6f * k, 17f * k, 8f * k, 21f * k, 11f * k, 21f * k) }, tint, style = s)
            }
            Tool.TEXT -> {
                drawRoundRect(tint, p(3f, 3f), Size(18f * u(), 18f * u()), CornerRadius(3f * u()), style = s)
                drawLine(tint, p(8f, 8f), p(16f, 8f), s.width, StrokeCap.Round)
                drawLine(tint, p(12f, 8f), p(12f, 17f), s.width, StrokeCap.Round)
            }
            Tool.IMAGE -> {
                drawRoundRect(tint, p(3f, 4f), Size(18f * u(), 16f * u()), CornerRadius(3f * u()), style = s)
                drawCircle(tint, 1.7f * u(), p(8.5f, 9.5f), style = s)
                drawPath(path { k -> moveTo(3.5f * k, 17f * k); lineTo(9f * k, 12.5f * k); lineTo(13f * k, 16f * k); lineTo(16f * k, 13.5f * k); lineTo(20.5f * k, 17.5f * k) }, tint, style = s)
            }
            Tool.STICKY -> {
                drawPath(path { k -> moveTo(4f * k, 4f * k); lineTo(20f * k, 4f * k); lineTo(20f * k, 14f * k); lineTo(14f * k, 20f * k); lineTo(4f * k, 20f * k); close() }, tint, style = s)
                drawPath(path { k -> moveTo(20f * k, 14f * k); lineTo(14f * k, 14f * k); lineTo(14f * k, 20f * k) }, tint, style = s)
            }
            Tool.TAPE -> {
                drawCircle(tint, 6f * u(), p(9f, 10f), style = s)
                drawCircle(tint, 2.2f * u(), p(9f, 10f), style = s)
                drawPath(path { k -> moveTo(9f * k, 16f * k); lineTo(21f * k, 16f * k); lineTo(19.5f * k, 18f * k); lineTo(21f * k, 20f * k); lineTo(9f * k, 20f * k) }, tint, style = s)
            }
            Tool.STICKER -> {
                drawCircle(tint, 9f * u(), p(12f, 12f), style = s)
                drawCircle(tint, 1.1f * u(), p(9f, 10f))
                drawCircle(tint, 1.1f * u(), p(15f, 10f))
                drawPath(path { k -> moveTo(8f * k, 14.5f * k); quadraticTo(12f * k, 18f * k, 16f * k, 14.5f * k) }, tint, style = s)
            }
            Tool.LASER -> {
                drawPath(path { k -> moveTo(5f * k, 19f * k); lineTo(13f * k, 11f * k) }, tint, style = Stroke(3.2f * u(), cap = StrokeCap.Round))
                drawCircle(Color(0xFFFF3B30), 2.2f * u(), p(15.5f, 8.5f))
                drawCircle(Color(0x44FF3B30), 4.2f * u(), p(15.5f, 8.5f))
                for ((a, b) in listOf(p(15.5f, 2f) to p(15.5f, 3.2f), p(22f, 8.5f) to p(20.8f, 8.5f), p(20.2f, 3.8f) to p(19.3f, 4.7f))) {
                    drawLine(tint, a, b, 1.5f * u(), StrokeCap.Round)
                }
            }
        }
    }
}

/** Pen bodies for the pen card and toolbar. */
@Composable
fun PenGlyph(type: PenType, tint: Color, ink: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawPen(tint, ink, type) }
}

private fun DrawScope.drawPen(tint: Color, ink: Color, type: PenType) {
    val s = line(tint)
    rotate(45f) {
        val k = u()
        when (type) {
            PenType.FOUNTAIN -> {
                drawRoundRect(tint, p(9.5f, 9f), Size(5f * k, 13f * k), CornerRadius(1.6f * k), style = s)
                drawPath(path { q -> moveTo(9.5f * q, 9f * q); lineTo(12f * q, 1.8f * q); lineTo(14.5f * q, 9f * q); close() }, ink)
                drawLine(tint, p(12f, 4.5f), p(12f, 8f), 1.1f * k, StrokeCap.Round)
            }
            PenType.BALL -> {
                drawRoundRect(tint, p(10f, 7f), Size(4f * k, 15f * k), CornerRadius(2f * k), style = s)
                drawPath(path { q -> moveTo(10.5f * q, 7f * q); lineTo(12f * q, 2.5f * q); lineTo(13.5f * q, 7f * q); close() }, ink)
                drawLine(tint, p(13.8f, 9f), p(13.8f, 14f), 1.1f * k, StrokeCap.Round)
            }
            PenType.BRUSH -> {
                drawRoundRect(tint, p(10f, 11f), Size(4f * k, 11f * k), CornerRadius(1.5f * k), style = s)
                drawPath(path { q ->
                    moveTo(10f * q, 11f * q); cubicTo(9f * q, 7f * q, 11f * q, 3f * q, 12f * q, 1.5f * q)
                    cubicTo(13f * q, 3f * q, 15f * q, 7f * q, 14f * q, 11f * q); close()
                }, ink)
            }
        }
    }
}

/** Small outline preview of a shape for the shape picker. */
@Composable
fun ShapeGlyph(kind: ShapeKind, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val k = u()
        if (kind == ShapeKind.AUTO) {
            drawPath(path { q -> moveTo(4f * q, 16f * q); cubicTo(6f * q, 4f * q, 14f * q, 22f * q, 20f * q, 8f * q) }, tint, style = line(tint))
            drawCircle(tint, 1.4f * k, p(20f, 8f))
            return@Canvas
        }
        val lines = ShapeBuilder.build(kind, 4f * k, 5f * k, 20f * k, 19f * k)
        for (pts in lines) {
            val path = Path()
            pts.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
            drawPath(path, tint, style = line(tint))
        }
    }
}
