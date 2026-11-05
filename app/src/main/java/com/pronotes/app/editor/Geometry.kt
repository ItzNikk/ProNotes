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
