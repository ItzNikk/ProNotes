package com.pronotes.app.editor

import android.graphics.Path
import android.graphics.Region
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

typealias Pt = Pair<Float, Float>

private fun dist(a: Pt, b: Pt) = hypot(a.first - b.first, a.second - b.second)

fun pathLength(pts: List<Pt>): Float {
    var l = 0f
    for (i in 1 until pts.size) l += dist(pts[i - 1], pts[i])
    return l
}

private data class Box(val l: Float, val t: Float, val r: Float, val b: Float) {
    val w get() = r - l
    val h get() = b - t
    val cx get() = (l + r) / 2f
    val cy get() = (t + b) / 2f
    val diag get() = hypot(w, h)
}

private fun box(pts: List<Pt>): Box {
    var l = Float.MAX_VALUE; var t = Float.MAX_VALUE
    var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
    for ((x, y) in pts) {
        l = min(l, x); t = min(t, y); r = max(r, x); b = max(b, y)
    }
    return Box(l, t, r, b)
}

fun isClosedLoop(pts: List<Pt>): Boolean {
    if (pts.size < 8) return false
    val bx = box(pts)
    val size = max(bx.w, bx.h)
    return size > 30f && dist(pts.first(), pts.last()) < size * 0.3f
}

/** Ramer-Douglas-Peucker polyline simplification. */
private fun simplify(pts: List<Pt>, eps: Float): List<Pt> {
    if (pts.size < 3) return pts
    val a = pts.first(); val b = pts.last()
    var maxD = 0f; var idx = 0
    val len = dist(a, b).coerceAtLeast(1e-3f)
    for (i in 1 until pts.size - 1) {
        val p = pts[i]
        val d = abs((b.second - a.second) * p.first - (b.first - a.first) * p.second + b.first * a.second - b.second * a.first) / len
        if (d > maxD) { maxD = d; idx = i }
    }
    return if (maxD > eps) {
        val left = simplify(pts.subList(0, idx + 1), eps)
        val right = simplify(pts.subList(idx, pts.size), eps)
        left.dropLast(1) + right
    } else listOf(a, b)
}

private fun densify(corners: List<Pt>, step: Float = 6f): List<Pt> {
    val out = ArrayList<Pt>()
    for (i in 0 until corners.size - 1) {
        val a = corners[i]; val b = corners[i + 1]
        val n = max(1, (dist(a, b) / step).toInt())
        for (k in 0 until n) {
            val t = k / n.toFloat()
            out += (a.first + (b.first - a.first) * t) to (a.second + (b.second - a.second) * t)
        }
    }
    out += corners.last()
    return out
}

/**
 * Turns a hand-drawn stroke into a clean shape: line, triangle, rectangle/quad,
 * circle or ellipse. Returns null when nothing fits well.
 */
object ShapeRecognizer {
    fun recognize(raw: List<Pt>): List<Pt>? {
        if (raw.size < 4) return null
        val len = pathLength(raw)
        if (len < 20f) return null
        val bx = box(raw)

        // Straight line.
        if (dist(raw.first(), raw.last()) / len > 0.9f) {
            return densify(listOf(raw.first(), raw.last()))
        }
        if (!isClosedLoop(raw)) return null

        // Ellipse fit: how far each point is from the ellipse through the bounding box.
        val rx = bx.w / 2f; val ry = bx.h / 2f
        if (rx > 5f && ry > 5f) {
            var err = 0f
            for ((x, y) in raw) {
                val nx = (x - bx.cx) / rx; val ny = (y - bx.cy) / ry
                err += abs(hypot(nx, ny) - 1f)
            }
            err /= raw.size
            val closed = raw + raw.first()
            val corners = simplify(closed, bx.diag * 0.07f)
            val polyCorners = corners.size - 1
            if (err < 0.09f && polyCorners > 4) return ellipse(bx, rx, ry)
            when (polyCorners) {
                3 -> return densify(corners)
                4 -> {
                    val axisAligned = corners.zipWithNext().all { (a, b) ->
                        val ang = Math.toDegrees(atan2(b.second - a.second, b.first - a.first).toDouble())
                        val m = ((ang % 90) + 90) % 90
                        m < 12 || m > 78
                    }
                    return if (axisAligned) {
                        densify(listOf(bx.l to bx.t, bx.r to bx.t, bx.r to bx.b, bx.l to bx.b, bx.l to bx.t))
                    } else densify(corners)
                }
            }
            if (err < 0.16f) return ellipse(bx, rx, ry)
        }
        return null
    }

    private fun ellipse(bx: Box, rx0: Float, ry0: Float): List<Pt> {
        var rx = rx0; var ry = ry0
        if (abs(rx - ry) / max(rx, ry) < 0.15f) { val r = (rx + ry) / 2f; rx = r; ry = r }
        val n = 96
        return List(n + 1) { i ->
            val a = (i / n.toFloat()) * 2.0 * Math.PI
            (bx.cx + rx * cos(a).toFloat()) to (bx.cy + ry * sin(a).toFloat())
        }
    }
}

/** A fast zig-zag back and forth over existing ink means "erase this". */
fun isScribble(pts: List<Pt>): Boolean {
    if (pts.size < 10) return false
    val sampled = ArrayList<Pt>()
    for (p in pts) if (sampled.isEmpty() || dist(sampled.last(), p) > 6f) sampled += p
    if (sampled.size < 6) return false
    var reversals = 0
    for (i in 2 until sampled.size) {
        val a = sampled[i - 2]; val b = sampled[i - 1]; val c = sampled[i]
        val v1x = b.first - a.first; val v1y = b.second - a.second
        val v2x = c.first - b.first; val v2y = c.second - b.second
        val dot = v1x * v2x + v1y * v2y
        val m = hypot(v1x, v1y) * hypot(v2x, v2y)
        if (m > 0 && dot / m < -0.5f) reversals++
    }
    val bx = box(pts)
    return reversals >= 4 && pathLength(pts) > bx.diag * 3f
}

/** Point-in-polygon test backed by the platform's Region. */
class Lasso(points: List<Pt>) {
    private val region = Region()

    init {
        val path = Path()
        points.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
        path.close()
        val bx = box(points)
        region.setPath(path, Region(bx.l.toInt() - 1, bx.t.toInt() - 1, bx.r.toInt() + 1, bx.b.toInt() + 1))
    }

    fun contains(x: Float, y: Float) = region.contains(x.toInt(), y.toInt())

    fun selects(item: PageItem): Boolean = when (item) {
        is InkItem -> {
            val pts = item.worldPoints()
            if (pts.isEmpty()) false
            else {
                val step = max(1, pts.size / 40)
                var inside = 0; var total = 0
                for (i in pts.indices step step) {
                    total++
                    if (contains(pts[i].first, pts[i].second)) inside++
                }
                inside >= total * 0.6f
            }
        }
        else -> item.bounds.let { contains(it.centerX(), it.centerY()) }
    }
}

/** Shapes in the shape picker. AUTO turns a freehand drawing into the closest shape. */
enum class ShapeKind(val label: String) {
    AUTO("Auto"), LINE("Line"), ARROW("Arrow"), DOUBLE_ARROW("Double arrow"), RECT("Rectangle"),
    ROUNDED("Rounded"), ELLIPSE("Ellipse"), TRIANGLE("Triangle"), RIGHT_TRIANGLE("Right triangle"),
    DIAMOND("Diamond"), PENTAGON("Pentagon"), HEXAGON("Hexagon"), STAR("Star"), HEART("Heart"),
    CLOUD("Cloud"), SPEECH("Speech"),
}

/**
 * Builds the outline of [kind] for a drag from (x0, y0) to (x1, y1).
 * Returns one or more polylines (arrows need separate heads).
 */
object ShapeBuilder {
    fun build(kind: ShapeKind, x0: Float, y0: Float, x1: Float, y1: Float): List<List<Pt>> {
        val l = min(x0, x1); val r = max(x0, x1); val t = min(y0, y1); val b = max(y0, y1)
        val cx = (l + r) / 2f; val cy = (t + b) / 2f; val rx = (r - l) / 2f; val ry = (b - t) / 2f
        fun closed(vararg p: Pt) = densifyPublic(p.toList() + p.first())
        return when (kind) {
            ShapeKind.AUTO, ShapeKind.LINE -> listOf(densifyPublic(listOf(x0 to y0, x1 to y1)))
            ShapeKind.ARROW -> listOf(densifyPublic(listOf(x0 to y0, x1 to y1)), arrowHead(x0, y0, x1, y1))
            ShapeKind.DOUBLE_ARROW -> listOf(
                densifyPublic(listOf(x0 to y0, x1 to y1)), arrowHead(x0, y0, x1, y1), arrowHead(x1, y1, x0, y0),
            )
            ShapeKind.RECT -> listOf(closed(l to t, r to t, r to b, l to b))
            ShapeKind.ROUNDED -> listOf(roundedRect(l, t, r, b, min(rx, ry) * 0.35f))
            ShapeKind.ELLIPSE -> listOf(polar(cx, cy, 96) { rx to ry })
            ShapeKind.TRIANGLE -> listOf(closed(cx to t, r to b, l to b))
            ShapeKind.RIGHT_TRIANGLE -> listOf(closed(l to t, r to b, l to b))
            ShapeKind.DIAMOND -> listOf(closed(cx to t, r to cy, cx to b, l to cy))
            ShapeKind.PENTAGON -> listOf(regular(cx, cy, rx, ry, 5))
            ShapeKind.HEXAGON -> listOf(regular(cx, cy, rx, ry, 6))
            ShapeKind.STAR -> listOf(star(cx, cy, rx, ry))
            ShapeKind.HEART -> listOf(heart(cx, cy, rx, ry))
            ShapeKind.CLOUD -> listOf(cloud(cx, cy, rx, ry))
            ShapeKind.SPEECH -> listOf(speech(l, t, r, b))
        }
    }

    private fun arrowHead(x0: Float, y0: Float, x1: Float, y1: Float): List<Pt> {
        val ang = atan2(y1 - y0, x1 - x0)
        val len = (hypot(x1 - x0, y1 - y0) * 0.18f).coerceIn(14f, 60f)
        val a1 = ang + Math.PI.toFloat() * 0.82f
        val a2 = ang - Math.PI.toFloat() * 0.82f
        return densifyPublic(
            listOf(
                (x1 + len * cos(a1)) to (y1 + len * sin(a1)),
                x1 to y1,
                (x1 + len * cos(a2)) to (y1 + len * sin(a2)),
            ),
        )
    }

    private fun polar(cx: Float, cy: Float, n: Int, radius: (Double) -> Pair<Float, Float>): List<Pt> = List(n + 1) { i ->
        val a = i / n.toDouble() * 2 * Math.PI
        val (rx, ry) = radius(a)
        (cx + rx * cos(a).toFloat()) to (cy + ry * sin(a).toFloat())
    }

    private fun regular(cx: Float, cy: Float, rx: Float, ry: Float, n: Int): List<Pt> {
        val pts = List(n) { i ->
            val a = -Math.PI / 2 + i * 2 * Math.PI / n
            (cx + rx * cos(a).toFloat()) to (cy + ry * sin(a).toFloat())
        }
        return densifyPublic(pts + pts.first())
    }

    private fun star(cx: Float, cy: Float, rx: Float, ry: Float): List<Pt> {
        val pts = List(10) { i ->
            val a = -Math.PI / 2 + i * Math.PI / 5
            val k = if (i % 2 == 0) 1f else 0.42f
            (cx + rx * k * cos(a).toFloat()) to (cy + ry * k * sin(a).toFloat())
        }
        return densifyPublic(pts + pts.first())
    }

    private fun heart(cx: Float, cy: Float, rx: Float, ry: Float): List<Pt> = List(121) { i ->
        val t = i / 120.0 * 2 * Math.PI
        val x = 16 * sin(t) * sin(t) * sin(t)
        val y = 13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t)
        (cx + (x / 17.0 * rx).toFloat()) to (cy - (y / 17.0 * ry).toFloat() - ry * 0.1f)
    }

    private fun cloud(cx: Float, cy: Float, rx: Float, ry: Float): List<Pt> = List(181) { i ->
        val a = i / 180.0 * 2 * Math.PI
        val bump = 1 + 0.12 * abs(sin(a * 4))
        (cx + (rx * 0.9 * bump * cos(a)).toFloat()) to (cy + (ry * 0.9 * bump * sin(a)).toFloat())
    }

    private fun roundedRect(l: Float, t: Float, r: Float, b: Float, rad: Float): List<Pt> {
        val out = ArrayList<Pt>()
        fun arc(cx: Float, cy: Float, start: Double) {
            for (k in 0..12) {
                val a = start + k / 12.0 * Math.PI / 2
                out += (cx + rad * cos(a).toFloat()) to (cy + rad * sin(a).toFloat())
            }
        }
        arc(r - rad, t + rad, -Math.PI / 2)
        arc(r - rad, b - rad, 0.0)
        arc(l + rad, b - rad, Math.PI / 2)
        arc(l + rad, t + rad, Math.PI)
        out += out.first()
        return densifyPublic(out)
    }

    private fun speech(l: Float, t: Float, r: Float, b: Float): List<Pt> {
        val tailTop = t + (b - t) * 0.75f
        val w = r - l
        return densifyPublic(
            listOf(
                l to t, r to t, r to tailTop, l + w * 0.45f to tailTop, l + w * 0.2f to b,
                l + w * 0.25f to tailTop, l to tailTop, l to t,
            ),
        )
    }
}

fun densifyPublic(corners: List<Pt>, step: Float = 6f): List<Pt> {
    if (corners.size < 2) return corners
    val out = ArrayList<Pt>()
    for (i in 0 until corners.size - 1) {
        val a = corners[i]; val b = corners[i + 1]
        val n = max(1, (hypot(b.first - a.first, b.second - a.second) / step).toInt())
        for (k in 0 until n) {
            val t = k / n.toFloat()
            out += (a.first + (b.first - a.first) * t) to (a.second + (b.second - a.second) * t)
        }
    }
    out += corners.last()
    return out
}
