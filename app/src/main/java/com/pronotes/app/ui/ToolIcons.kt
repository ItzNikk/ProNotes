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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.luminance
import com.pronotes.app.editor.PenType
import com.pronotes.app.editor.ShapeBuilder
import com.pronotes.app.editor.ShapeKind
import com.pronotes.app.editor.Tool

/*
 * Toolbar icon set (24-unit grid, 1.75 stroke, round caps), taken from
 * pro_notes_toolbar_icons_v2. Shapes are SVG path data drawn with Compose.
 */

internal sealed interface Part
internal data class P(val d: String, val dashed: Boolean = false, val filled: Boolean = false, val body: Boolean = false) : Part
internal data class C(val cx: Float, val cy: Float, val r: Float, val filled: Boolean = false, val body: Boolean = false) : Part
internal data class Rr(val x: Float, val y: Float, val w: Float, val h: Float, val rx: Float, val body: Boolean = false) : Part

enum class ProIcon(internal val parts: List<Part>) {
    SIDEBAR(listOf(Rr(3.5f, 4.5f, 17f, 15f, 2.5f), P("M9.5 4.5v15"))),
    SEARCH(listOf(C(11f, 11f, 6.5f), P("M20 20l-4.4-4.4"))),
    UNDO(listOf(P("M9 14L4 9l5-5"), P("M4 9h10.5a5.5 5.5 0 0 1 0 11H11"))),
    REDO(listOf(P("M15 14l5-5-5-5"), P("M20 9H9.5a5.5 5.5 0 0 0 0 11H13"))),
    PEN(listOf(P("M14.5 4.5l5 5L9.5 19.5l-5.5 1 1-5.5z", body = true), P("M12.5 6.5l5 5"), P("M5 15l4 4"))),
    HIGHLIGHTER(listOf(P("M14.5 4l5.5 5.5-8 8-5.5-5.5z", body = true), P("M6.5 12L4.5 17l2.5 2.5 4.5-1.5"), P("M14 21h7"))),
    ERASER(listOf(
        P("M5.5 14.5l8-8a2 2 0 0 1 2.8 0l2.2 2.2a2 2 0 0 1 0 2.8L11 19.5H8L5.5 17a1.2 1.2 0 0 1 0-2.5z"),
        P("M10 10l5.5 5.5"), P("M12 19.5h8"),
    )),
    SHAPES(listOf(C(7.5f, 7.5f, 4f, body = true), P("M16.5 4L21 12.5h-9z"), Rr(6f, 15f, 12f, 5.5f, 1.5f))),
    LASSO(listOf(
        P("M12 4c4.4 0 8 1.8 8 4.5S16.4 13 12 13 4 11.2 4 8.5 7.6 4 12 4z", dashed = true),
        P("M9 12.8c-1.2 2.2-.6 4.6 1.8 5.2 1.8.4 3-.6 2.7-1.8"), C(13.5f, 19.2f, 1.3f),
    )),
    IMAGE(listOf(Rr(3.5f, 4.5f, 17f, 15f, 2.5f), C(9f, 10f, 1.7f), P("M3.5 17l5-5 4 4 3-3 5 4.5"))),
    TEXT(listOf(P("M5.5 7V5h13v2"), P("M12 5v14"), P("M9.5 19h5"))),
    LASER(listOf(
        P("M12.2 11.8l-7.6 7.6a1.5 1.5 0 0 0 2.1 2.1l7.6-7.6z"), P("M13.8 10.2l1.6-1.6"),
        C(18f, 6f, 1.5f, filled = true, body = true), P("M18 2.5v1.2M21.5 6h-1.2M20.5 3.5l-.9.9M15.5 3.5l.9.9"),
    )),
    ADD(listOf(P("M12 5v14M5 12h14"))),
    SHARE(listOf(P("M12 15V4"), P("M8 8l4-4 4 4"), P("M5 12v6.5a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V12"))),
    MORE(listOf(C(5.5f, 12f, 1.2f, true), C(12f, 12f, 1.2f, true), C(18.5f, 12f, 1.2f, true))),
    // Same style, for the extra tools and actions.
    STICKY(listOf(P("M5 4.5h14a1 1 0 0 1 1 1V14l-6 6H5a1 1 0 0 1-1-1V5.5a1 1 0 0 1 1-1z", body = true), P("M20 14h-5a1 1 0 0 0-1 1v5"))),
    TAPE(listOf(C(9f, 10f, 5.5f, body = true), C(9f, 10f, 2f), P("M9 15.5h11l-1.5 2 1.5 2H9"))),
    STICKER(listOf(C(12f, 12f, 8.5f, body = true), P("M8.5 14.5c1.8 2 5.2 2 7 0"), C(9.3f, 10f, 0.9f, true), C(14.7f, 10f, 0.9f, true))),
    PAGES(listOf(Rr(4f, 4f, 7f, 7f, 1.5f), Rr(13f, 4f, 7f, 7f, 1.5f), Rr(4f, 13f, 7f, 7f, 1.5f), Rr(13f, 13f, 7f, 7f, 1.5f))),
    HOME(listOf(P("M4 11l8-7 8 7"), P("M6 9.5V19a1 1 0 0 0 1 1h3.5v-5h3v5H17a1 1 0 0 0 1-1V9.5"))),
    BOOKMARK(listOf(P("M7 4.5h10a1 1 0 0 1 1 1V20l-6-4-6 4V5.5a1 1 0 0 1 1-1z", body = true))),
    FOCUS(listOf(P("M4 9V5.5a1.5 1.5 0 0 1 1.5-1.5H9"), P("M15 4h3.5A1.5 1.5 0 0 1 20 5.5V9"), P("M20 15v3.5a1.5 1.5 0 0 1-1.5 1.5H15"), P("M9 20H5.5A1.5 1.5 0 0 1 4 18.5V15"))),
    CLOSE(listOf(P("M6 6l12 12M18 6L6 18"))),
    // Settings categories and rows: same thin-line style as the toolbar.
    DOCUMENT(listOf(Rr(5f, 3.5f, 14f, 17f, 2.5f), P("M8.5 8.5h7M8.5 12h7M8.5 15.5h4"))),
    TOOLBAR(listOf(Rr(3f, 13f, 18f, 7f, 3.5f), C(8f, 16.5f, 1f, true), C(12f, 16.5f, 1f, true), C(16f, 16.5f, 1f, true), P("M8 6.5h8"))),
    PALETTE(listOf(
        P("M12 4a8 8 0 1 0 0 16c1.1 0 1.6-.8 1.3-1.7-.4-1 .3-2.1 1.5-2.1H17a3 3 0 0 0 3-3C20 7.6 16.5 4 12 4z"),
        C(8f, 11f, 1f, true), C(10.5f, 7.5f, 1f, true), C(14.5f, 7.5f, 1f, true),
    )),
    FOLDER(listOf(P("M3.5 7.5a2 2 0 0 1 2-2h4l2 2.5h7a2 2 0 0 1 2 2v7.5a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2z"))),
    INFO(listOf(C(12f, 12f, 8.5f), P("M12 11v5"), C(12f, 8f, 0.7f, true))),
    HEART(listOf(P("M12 20s-7.5-4.6-7.5-10A4.2 4.2 0 0 1 12 7.6 4.2 4.2 0 0 1 19.5 10c0 5.4-7.5 10-7.5 10z", body = true))),
    STAR(listOf(P("M12 3.5l2.2 5.3 5.8.5-4.4 3.8 1.3 5.6L12 15.6l-4.9 3.1 1.3-5.6L4 9.3l5.8-.5z"))),
    DOWNLOAD(listOf(P("M12 4v11"), P("M8 11l4 4 4-4"), P("M5 19h14"))),
    RESTORE(listOf(P("M4.5 12a7.5 7.5 0 1 0 2.4-5.5"), P("M4.5 5v4h4"), P("M12 8.5V12l2.5 1.5"))),
    /** Two sliders: a calm, professional "settings" mark. */
    SETTINGS(listOf(P("M4 7.5h8.5M17.5 7.5H20"), C(15f, 7.5f, 2.3f), P("M4 16.5h2.5M11.5 16.5H20"), C(9f, 16.5f, 2.3f))),
}

private val parsed = HashMap<String, Path>()
private fun pathOf(d: String): Path = parsed.getOrPut(d) { PathParser().parsePathString(d).toPath() }

/**
 * Draws an icon. [ink], when set, fills the icon's "body" part, so the active
 * pen shows its ink colour and the shapes icon shows its fill.
 */
@Composable
fun ProIconView(icon: ProIcon, tint: Color, modifier: Modifier = Modifier, ink: Color? = null, inkAlpha: Float = 0.9f) {
    Canvas(modifier) { drawProIcon(icon, tint, ink, inkAlpha) }
}

fun DrawScope.drawProIcon(icon: ProIcon, tint: Color, ink: Color?, inkAlpha: Float = 0.9f) {
    val k = size.minDimension / 24f
    val ox = (size.width - 24f * k) / 2f
    val oy = (size.height - 24f * k) / 2f
    translate(ox, oy) {
        scale(k, k, pivot = Offset.Zero) {
            val stroke = Stroke(width = 1.75f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val dashed = Stroke(width = 1.75f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)))
            // A light ink colour (yellow highlighter) needs a darker outline to stay visible.
            val outline = if (ink != null && ink.luminance() > 0.8f) tint.copy(alpha = 1f) else tint
            for (part in icon.parts) when (part) {
                is P -> {
                    val path = pathOf(part.d)
                    if (part.body && ink != null) drawPath(path, ink.copy(alpha = inkAlpha))
                    if (part.filled) drawPath(path, tint) else drawPath(path, outline, style = if (part.dashed) dashed else stroke)
                }
                is C -> {
                    if (part.body && ink != null) drawCircle(ink.copy(alpha = inkAlpha), part.r, Offset(part.cx, part.cy))
                    if (part.filled) drawCircle(if (part.body && ink != null) ink else tint, part.r, Offset(part.cx, part.cy))
                    else drawCircle(outline, part.r, Offset(part.cx, part.cy), style = stroke)
                }
                is Rr -> {
                    if (part.body && ink != null) drawRoundRect(ink.copy(alpha = inkAlpha), Offset(part.x, part.y), Size(part.w, part.h), CornerRadius(part.rx))
                    drawRoundRect(outline, Offset(part.x, part.y), Size(part.w, part.h), CornerRadius(part.rx), style = stroke)
                }
            }
        }
    }
}

fun iconFor(tool: Tool): ProIcon = when (tool) {
    Tool.PEN -> ProIcon.PEN
    Tool.HIGHLIGHTER -> ProIcon.HIGHLIGHTER
    Tool.ERASER -> ProIcon.ERASER
    Tool.SHAPE -> ProIcon.SHAPES
    Tool.LASSO -> ProIcon.LASSO
    Tool.TEXT -> ProIcon.TEXT
    Tool.IMAGE -> ProIcon.IMAGE
    Tool.STICKY -> ProIcon.STICKY
    Tool.TAPE -> ProIcon.TAPE
    Tool.STICKER -> ProIcon.STICKER
    Tool.LASER -> ProIcon.LASER
}

/** Tool icon. Colour tools always show their current [inkColor]; the shapes icon is multicolour. */
@Composable
fun ToolGlyph(tool: Tool, tint: Color, inkColor: Color, modifier: Modifier = Modifier, active: Boolean = false) {
    if (tool == Tool.SHAPE) {
        Canvas(modifier) { drawShapesIcon(tint) }
        return
    }
    val ink = when (tool) {
        Tool.PEN, Tool.TAPE, Tool.STICKY, Tool.HIGHLIGHTER -> inkColor
        Tool.LASER -> if (active) Color(0xFFFF3B30) else null
        else -> null
    }
    ProIconView(iconFor(tool), tint, modifier, ink, if (tool == Tool.HIGHLIGHTER) 0.55f else if (active) 0.9f else 0.75f)
}

/** Circle, triangle and rounded box in three colours. */
private fun DrawScope.drawShapesIcon(tint: Color) {
    val k = size.minDimension / 24f
    translate((size.width - 24f * k) / 2f, (size.height - 24f * k) / 2f) {
        scale(k, k, pivot = Offset.Zero) {
            val s = Stroke(width = 1.75f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawCircle(Color(0xFFFF6B6B), 4f, Offset(7.5f, 7.5f))
            drawCircle(tint, 4f, Offset(7.5f, 7.5f), style = s)
            val tri = pathOf("M16.5 4L21 12.5h-9z")
            drawPath(tri, Color(0xFFFFC53D)); drawPath(tri, tint, style = s)
            drawRoundRect(Color(0xFF4D9BFF), Offset(6f, 15f), Size(12f, 5.5f), CornerRadius(1.5f))
            drawRoundRect(tint, Offset(6f, 15f), Size(12f, 5.5f), CornerRadius(1.5f), style = s)
        }
    }
}

/** Pen bodies for the pen card. */
@Composable
fun PenGlyph(type: PenType, tint: Color, ink: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawPen(tint, ink, type) }
}

private fun DrawScope.drawPen(tint: Color, ink: Color, type: PenType) {
    val k = size.minDimension / 24f
    fun p(x: Float, y: Float) = Offset(x * k, y * k)
    val s = Stroke(width = 1.7f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
    rotate(45f) {
        when (type) {
            PenType.FOUNTAIN -> {
                drawRoundRect(tint, p(9.5f, 9f), Size(5f * k, 13f * k), CornerRadius(1.6f * k), style = s)
                drawPath(Path().apply { moveTo(9.5f * k, 9f * k); lineTo(12f * k, 1.8f * k); lineTo(14.5f * k, 9f * k); close() }, ink)
                drawLine(tint, p(12f, 4.5f), p(12f, 8f), 1.1f * k, StrokeCap.Round)
            }
            PenType.BALL -> {
                drawRoundRect(tint, p(10f, 7f), Size(4f * k, 15f * k), CornerRadius(2f * k), style = s)
                drawPath(Path().apply { moveTo(10.5f * k, 7f * k); lineTo(12f * k, 2.5f * k); lineTo(13.5f * k, 7f * k); close() }, ink)
                drawLine(tint, p(13.8f, 9f), p(13.8f, 14f), 1.1f * k, StrokeCap.Round)
            }
            PenType.DASHED -> {
                drawRoundRect(tint, p(10f, 7f), Size(4f * k, 15f * k), CornerRadius(2f * k), style = s)
                drawPath(Path().apply { moveTo(10.5f * k, 7f * k); lineTo(12f * k, 2.5f * k); lineTo(13.5f * k, 7f * k); close() }, ink)
                for (y in listOf(9f, 12.5f, 16f)) drawLine(ink, p(12f, y), p(12f, y + 2f), 1.4f * k, StrokeCap.Round)
            }
            PenType.BRUSH -> {
                drawRoundRect(tint, p(10f, 11f), Size(4f * k, 11f * k), CornerRadius(1.5f * k), style = s)
                drawPath(Path().apply {
                    moveTo(10f * k, 11f * k); cubicTo(9f * k, 7f * k, 11f * k, 3f * k, 12f * k, 1.5f * k)
                    cubicTo(13f * k, 3f * k, 15f * k, 7f * k, 14f * k, 11f * k); close()
                }, ink)
            }
        }
    }
}

/** Small outline preview of a shape for the shape picker. */
@Composable
fun ShapeGlyph(kind: ShapeKind, tint: Color, modifier: Modifier = Modifier, fill: Color? = null) {
    Canvas(modifier) {
        val k = size.minDimension / 24f
        val s = Stroke(width = 1.6f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
        if (kind == ShapeKind.AUTO) {
            drawPath(Path().apply { moveTo(4f * k, 16f * k); cubicTo(6f * k, 4f * k, 14f * k, 22f * k, 20f * k, 8f * k) }, tint, style = s)
            drawCircle(tint, 1.4f * k, Offset(20f * k, 8f * k))
            return@Canvas
        }
        val lines = ShapeBuilder.build(kind, 4f * k, 5f * k, 20f * k, 19f * k)
        lines.forEachIndexed { li, pts ->
            val path = Path()
            pts.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
            if (li == 0 && fill != null && kind.closed) drawPath(path, fill)
            drawPath(path, tint, style = s)
        }
    }
}
