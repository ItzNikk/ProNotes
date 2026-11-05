package com.pronotes.app.editor

import android.graphics.Path
import android.graphics.Region
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.pillStar
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
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
    if (size <= 30f) return false
    if (dist(pts.first(), pts.last()) < size * 0.35f) return true
    // Overlapping loops: the end has come back near any early point.
    val end = pts.last()
    val early = pts.subList(0, pts.size / 3)
    return early.any { dist(it, end) < size * 0.25f } && pathLength(pts) > size * 2.2f
}

/** Ramer-Douglas-Peucker simplification of an open polyline. */
private fun simplify(pts: List<Pt>, eps: Float): List<Pt> {
    if (pts.size < 3) return pts
    val a = pts.first(); val b = pts.last()
    var maxD = 0f; var idx = 0
    for (i in 1 until pts.size - 1) {
        val d = segDist(pts[i], a, b)
        if (d > maxD) { maxD = d; idx = i }
    }
    return if (maxD > eps) {
        val left = simplify(pts.subList(0, idx + 1), eps)
        val right = simplify(pts.subList(idx, pts.size), eps)
        left.dropLast(1) + right
    } else listOf(a, b)
}

/** Distance from [p] to the segment a-b (works when a == b). */
private fun segDist(p: Pt, a: Pt, b: Pt): Float {
    val vx = b.first - a.first; val vy = b.second - a.second
    val len2 = vx * vx + vy * vy
    if (len2 < 1e-6f) return dist(p, a)
    val t = (((p.first - a.first) * vx + (p.second - a.second) * vy) / len2).coerceIn(0f, 1f)
    return hypot(p.first - (a.first + vx * t), p.second - (a.second + vy * t))
}

private fun turnAngle(prev: Pt, cur: Pt, next: Pt): Double {
    val a1 = atan2((cur.second - prev.second).toDouble(), (cur.first - prev.first).toDouble())
    val a2 = atan2((next.second - cur.second).toDouble(), (next.first - cur.first).toDouble())
    var d = Math.toDegrees(abs(a2 - a1))
    if (d > 180) d = 360 - d
    return d   // 0 = straight on, 180 = full reversal
}

/**
 * Corners of a closed hand-drawn loop. The loop is cut at the point farthest from the
 * start so both halves are open polylines, then each half is simplified.
 */
private fun closedCorners(raw: List<Pt>, eps: Float): List<Pt> {
    val start = raw.first()
    val far = raw.indices.maxByOrNull { dist(raw[it], start) } ?: return emptyList()
    if (far == 0) return emptyList()
    val a = simplify(raw.subList(0, far + 1), eps)
    val b = simplify(raw.subList(far, raw.size) + listOf(start), eps)
    var corners = (a.dropLast(1) + b.dropLast(1)).toMutableList()
    // Drop near-duplicates and corners that are really straight lines.
    var changed = true
    while (changed && corners.size > 3) {
        changed = false
        for (i in corners.indices) {
            val prev = corners[(i - 1 + corners.size) % corners.size]
            val cur = corners[i]
            val next = corners[(i + 1) % corners.size]
            if (dist(prev, cur) < eps * 1.2f || turnAngle(prev, cur, next) < 28.0) {
                corners.removeAt(i); changed = true; break
            }
        }
    }
    return corners
}

/** Mean distance from the stroke to a closed polygon, relative to its size. */
private fun polygonError(raw: List<Pt>, poly: List<Pt>, diag: Float): Float {
    if (poly.size < 3) return Float.MAX_VALUE
    var sum = 0f
    for (p in raw) {
        var best = Float.MAX_VALUE
        for (i in poly.indices) best = min(best, segDist(p, poly[i], poly[(i + 1) % poly.size]))
        sum += best
    }
    return sum / raw.size / diag
}

/**
 * Turns a hand-drawn stroke into a clean shape: line, elbow/zig-zag line, triangle,
 * rectangle, square, diamond, quad, pentagon, hexagon, circle or ellipse.
 * Returns null when nothing fits well.
 */
object ShapeRecognizer {
    fun recognize(raw: List<Pt>): List<Pt>? {
        if (raw.size < 4) return null
        val len = pathLength(raw)
        if (len < 20f) return null
        val bx = box(raw)
        val diag = bx.diag.coerceAtLeast(1f)

        // Straight line.
        if (dist(raw.first(), raw.last()) / len > 0.92f) return densify(listOf(raw.first(), raw.last()))

        if (!isClosedLoop(raw)) {
            // Elbow or zig-zag made of 2-3 straight segments.
            val pts = simplify(raw, diag * 0.06f)
            if (pts.size in 3..4 && abs(pathLength(pts) - len) / len < 0.12f) return densify(pts)
            return null
        }

        val rx = bx.w / 2f; val ry = bx.h / 2f
        var ellipseErr = Float.MAX_VALUE
        if (rx > 5f && ry > 5f) {
            var err = 0f
            for ((x, y) in raw) err += abs(hypot((x - bx.cx) / rx, (y - bx.cy) / ry) - 1f)
            ellipseErr = err / raw.size
        }

        val corners = closedCorners(raw, diag * 0.06f)
        val polyErr = polygonError(raw, corners, diag)
        val n = corners.size
        if (n in 3..6 && polyErr < 0.035f && (polyErr < ellipseErr * 0.5f || ellipseErr > 0.08f)) {
            return when (n) {
                3 -> densify(corners + corners.first())
                4 -> quad(corners, bx)
                else -> densify(corners + corners.first())
            }
        }
        if (ellipseErr < 0.2f) return ellipse(bx, rx, ry)
        return null
    }

    /** Four corners: rectangle (or square) when the sides follow the axes, diamond when they are 45°, otherwise as drawn. */
    private fun quad(c: List<Pt>, bx: Box): List<Pt> {
        val angles = c.indices.map { i ->
            val a = c[i]; val b = c[(i + 1) % c.size]
            val ang = Math.toDegrees(atan2((b.second - a.second).toDouble(), (b.first - a.first).toDouble()))
            ((ang % 90) + 90) % 90
        }
        val axis = angles.all { it < 14 || it > 76 }
        val diagonal = angles.all { it in 31.0..59.0 }
        return when {
            axis -> {
                var l = bx.l; var t = bx.t; var r = bx.r; var b = bx.b
                if (abs(bx.w - bx.h) / max(bx.w, bx.h) < 0.1f) {
                    val s = (bx.w + bx.h) / 2f; l = bx.cx - s / 2; r = bx.cx + s / 2; t = bx.cy - s / 2; b = bx.cy + s / 2
                }
                densify(listOf(l to t, r to t, r to b, l to b, l to t))
            }
            diagonal -> densify(listOf(bx.cx to bx.t, bx.r to bx.cy, bx.cx to bx.b, bx.l to bx.cy, bx.cx to bx.t))
            else -> densify(c + c.first())
        }
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

private fun densify(corners: List<Pt>, step: Float = 6f): List<Pt> = densifyPublic(corners, step)

/** True when a stroke is nearly straight: every point within [tolerance] of the chord. */
fun isNearlyStraight(pts: List<Pt>, tolerance: Float): Boolean {
    if (pts.size < 2) return false
    val a = pts.first(); val b = pts.last()
    if (dist(a, b) < 20f) return false
    return pts.all { segDist(it, a, b) <= tolerance }
}

/**
 * A fast back-and-forth zig-zag means "erase this". The stroke is projected onto its
 * main direction (PCA); the motion along that axis must reverse at least three times.
 * Letters like "W" or "m" move steadily forward and never qualify.
 */
fun isScribble(pts: List<Pt>): Boolean {
    if (pts.size < 8) return false
    var mx = 0.0; var my = 0.0
    for ((x, y) in pts) { mx += x; my += y }
    mx /= pts.size; my /= pts.size
    var sxx = 0.0; var syy = 0.0; var sxy = 0.0
    for ((x, y) in pts) { val dx = x - mx; val dy = y - my; sxx += dx * dx; syy += dy * dy; sxy += dx * dy }
    val theta = 0.5 * atan2(2 * sxy, sxx - syy)
    val ux = cos(theta).toFloat(); val uy = sin(theta).toFloat()
    val proj = pts.map { (x, y) -> x * ux + y * uy }
    val extent = (proj.maxOrNull() ?: 0f) - (proj.minOrNull() ?: 0f)
    if (extent < 18f) return false
    val thresh = extent * 0.3f
    var dir = 0; var anchor = proj[0]; var reversals = 0
    for (v in proj) {
        when (dir) {
            0 -> if (v - anchor > thresh) { dir = 1; anchor = v } else if (anchor - v > thresh) { dir = -1; anchor = v }
            1 -> if (v > anchor) anchor = v else if (anchor - v > thresh) { reversals++; dir = -1; anchor = v }
            else -> if (v < anchor) anchor = v else if (v - anchor > thresh) { reversals++; dir = 1; anchor = v }
        }
    }
    return reversals >= 3
}

/** Point-in-polygon test backed by the platform's Region. */
class Lasso(points: List<Pt>, private val touching: Boolean = false) {
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
                if (touching) inside > 0 else inside >= total * 0.6f
            }
        }
        else -> item.bounds.let { b ->
            if (touching) contains(b.centerX(), b.centerY()) || contains(b.left, b.top) || contains(b.right, b.top) ||
                contains(b.left, b.bottom) || contains(b.right, b.bottom)
            else contains(b.centerX(), b.centerY())
        }
    }
}

/** Shapes in the shape picker. AUTO turns a freehand drawing into the closest shape. */
enum class ShapeKind(val label: String) {
    AUTO("Auto"), LINE("Line"), ARROW("Arrow"), DOUBLE_ARROW("Double arrow"), RECT("Rectangle"),
    ROUNDED("Rounded"), ELLIPSE("Ellipse"), TRIANGLE("Triangle"), RIGHT_TRIANGLE("Right triangle"),
    DIAMOND("Diamond"), PENTAGON("Pentagon"), HEXAGON("Hexagon"), STAR("Star"), HEART("Heart"),
    CLOUD("Cloud"), SPEECH("Speech"),
    // Shapes below come from androidx.graphics:graphics-shapes.
    OCTAGON("Octagon"), SOFT_TRIANGLE("Soft triangle"), SQUIRCLE("Squircle"), PILL("Pill"),
    SOFT_STAR("Soft star"), SUNNY("Sunny"), BURST("Burst"), COOKIE("Cookie"), CLOVER("Clover"),
    FLOWER("Flower"), GEM("Gem"), PILL_STAR("Pill star"), BADGE("Badge"), SPARKLE("Sparkle");

    /** Closed outlines can be filled. */
    val closed: Boolean get() = this !in setOf(AUTO, LINE, ARROW, DOUBLE_ARROW)
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
            else -> listOf(LibShapes.outline(kind, l, t, r, b))
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

/** Rounded polygons, stars and blobs from Jetpack Graphics Shapes, sampled into points. */
object LibShapes {
    private fun poly(kind: ShapeKind): androidx.graphics.shapes.RoundedPolygon {
        val rp = androidx.graphics.shapes.RoundedPolygon
        fun r(radius: Float, smoothing: Float = 0f) = androidx.graphics.shapes.CornerRounding(radius, smoothing)
        return when (kind) {
            ShapeKind.OCTAGON -> androidx.graphics.shapes.RoundedPolygon(8)
            ShapeKind.SOFT_TRIANGLE -> androidx.graphics.shapes.RoundedPolygon(3, rounding = r(0.25f, 0.4f))
            ShapeKind.SQUIRCLE -> rp.rectangle(2f, 2f, rounding = r(0.6f, 1f))
            ShapeKind.PILL -> rp.pill(2f, 1f)
            ShapeKind.SOFT_STAR -> rp.star(5, innerRadius = 0.5f, rounding = r(0.12f))
            ShapeKind.SUNNY -> rp.star(8, innerRadius = 0.8f, rounding = r(0.15f))
            ShapeKind.BURST -> rp.star(12, innerRadius = 0.78f)
            ShapeKind.COOKIE -> rp.star(9, innerRadius = 0.8f, rounding = r(0.5f))
            ShapeKind.CLOVER -> rp.star(4, innerRadius = 0.25f, rounding = r(0.45f))
            ShapeKind.FLOWER -> rp.star(8, innerRadius = 0.6f, rounding = r(0.35f))
            ShapeKind.GEM -> androidx.graphics.shapes.RoundedPolygon(6, rounding = r(0.2f, 0.6f))
            ShapeKind.PILL_STAR -> rp.pillStar(2f, 1f, numVerticesPerRadius = 10, innerRadiusRatio = 0.7f)
            ShapeKind.BADGE -> rp.star(16, innerRadius = 0.86f, rounding = r(0.1f))
            ShapeKind.SPARKLE -> rp.star(4, innerRadius = 0.32f, rounding = r(0.05f))
            else -> androidx.graphics.shapes.RoundedPolygon(6)
        }
    }

    fun outline(kind: ShapeKind, l: Float, t: Float, r: Float, b: Float): List<Pt> {
        val path = poly(kind).normalized().toPath()
        val m = android.graphics.Matrix().apply { setScale(max(r - l, 1f), max(b - t, 1f)); postTranslate(l, t) }
        path.transform(m)
        val measure = android.graphics.PathMeasure(path, true)
        val out = ArrayList<Pt>()
        val pos = FloatArray(2)
        do {
            val len = measure.length
            var d = 0f
            while (d <= len) { measure.getPosTan(d, pos, null); out += pos[0] to pos[1]; d += 4f }
            measure.getPosTan(len, pos, null); out += pos[0] to pos[1]
        } while (measure.nextContour())
        if (out.isNotEmpty()) out += out.first()
        return out
    }
}
