package com.pronotes.app.editor

import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import androidx.compose.runtime.Immutable
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.pillStar
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import com.pronotes.app.data.AppSettings
import com.pronotes.app.data.ScribbleSensitivity
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

typealias Pt = Pair<Float, Float>

/** Tunable thresholds for hold gestures, loop selection, scribble-to-erase, and palm rejection. */
@Immutable
data class GestureConfig(
    val holdDelayMs: Long = 420L,
    val holdSlopDp: Float = 14f,
    val circleWindowMs: Long = 3500L,
    val circleHoldDelayMs: Long = 450L,
    val circleEndRadiusDp: Float = 44f,
    val loopClosingRatio: Float = 0.38f,
    val overlapLoopClosingRatio: Float = 0.28f,
    val overlapMinLengthRatio: Float = 2.0f,
    val minLoopSizePx: Float = 26f,
    val minLoopScreenWdp: Float = 36f,
    val minLoopScreenHdp: Float = 26f,
    val scribbleSensitivity: ScribbleSensitivity = ScribbleSensitivity.BALANCED,
    val scribbleMinLengthPx: Float = 84f,
    val scribbleMaxNetDispRatio: Float = 0.34f,
    val palmRejectionWindowMs: Long = 300L,
) {
    companion object {
        fun fromSensitivity(sensitivity: ScribbleSensitivity): GestureConfig = when (sensitivity) {
            ScribbleSensitivity.GENTLE -> GestureConfig(
                scribbleSensitivity = sensitivity,
                scribbleMinLengthPx = 115f,
                scribbleMaxNetDispRatio = 0.26f,
            )
            ScribbleSensitivity.BALANCED -> GestureConfig(
                scribbleSensitivity = sensitivity,
                scribbleMinLengthPx = 84f,
                scribbleMaxNetDispRatio = 0.34f,
            )
            ScribbleSensitivity.INSTANT -> GestureConfig(
                scribbleSensitivity = sensitivity,
                scribbleMinLengthPx = 54f,
                scribbleMaxNetDispRatio = 0.40f,
            )
        }

        fun fromSettings(settings: AppSettings): GestureConfig =
            fromSensitivity(settings.scribbleSensitivity).copy(
                loopClosingRatio = if (settings.lassoTouching) 0.42f else 0.38f,
            )
    }
}

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

/** Single closing rule shared by shape recognition, lasso candidate checks, and circle selection. */
fun isClosedLoop(pts: List<Pt>, config: GestureConfig = GestureConfig()): Boolean {
    if (pts.size < 8) return false
    val bx = box(pts)
    val size = max(bx.w, bx.h)
    if (size <= config.minLoopSizePx) return false
    if (dist(pts.first(), pts.last()) < size * config.loopClosingRatio) return true
    val end = pts.last()
    val early = pts.subList(0, pts.size / 3)
    return early.any { dist(it, end) < size * config.overlapLoopClosingRatio } &&
        pathLength(pts) > size * config.overlapMinLengthRatio
}

/**
 * Unified state machine for circle-to-select (both live hold before lift and quick press-and-hold
 * immediately after drawing a closed loop).
 */
class CircleDetector {
    data class Candidate(
        val strokeKey: Any,
        val page: Int,
        val targetIds: Set<Long>,
        val loop: List<Pt>,
        val endX: Float,
        val endY: Float,
        val createdAtMs: Long,
        var itemId: Long = -1L,
        var pendingRemoval: Boolean = false,
    )

    sealed interface State {
        data object Idle : State
        data class CandidateReady(val candidate: Candidate) : State
        data class HoldArmed(
            val candidate: Candidate,
            val downX: Float,
            val downY: Float,
            val armedAtMs: Long,
        ) : State
    }

    var state: State = State.Idle
        private set

    fun isValidLoopCandidate(
        pts: List<Pt>,
        scale: Float,
        density: Float,
        config: GestureConfig = GestureConfig(),
    ): Boolean {
        if (pts.size < 8 || !isClosedLoop(pts, config)) return false
        val bx = box(pts)
        return bx.w * scale > config.minLoopScreenWdp * density &&
            bx.h * scale > config.minLoopScreenHdp * density
    }

    fun offerCandidate(
        strokeKey: Any,
        page: Int,
        targetIds: Set<Long>,
        loop: List<Pt>,
        nowMs: Long,
    ): Candidate? {
        if (loop.isEmpty() || targetIds.isEmpty()) return null
        val end = loop.last()
        val candidate = Candidate(
            strokeKey = strokeKey,
            page = page,
            targetIds = targetIds,
            loop = ArrayList(loop),
            endX = end.first,
            endY = end.second,
            createdAtMs = nowMs,
        )
        state = State.CandidateReady(candidate)
        return candidate
    }

    /**
     * Associates the asynchronously persisted [itemId] with the active candidate (if matching [strokeKey]).
     * Returns true if the user already triggered circle-select before persistence finished, meaning the
     * item should be removed instead of kept on the page.
     */
    fun attachItemId(strokeKey: Any, itemId: Long): Boolean {
        val candidate = activeCandidateUnchecked() ?: return false
        if (candidate.strokeKey != strokeKey) return false
        candidate.itemId = itemId
        return candidate.pendingRemoval
    }

    fun activeCandidate(nowMs: Long, config: GestureConfig): Candidate? {
        val c = activeCandidateUnchecked() ?: return null
        if (state is State.CandidateReady && nowMs - c.createdAtMs > config.circleWindowMs) {
            state = State.Idle
            return null
        }
        return c
    }

    fun isHoldArmed(): Boolean = state is State.HoldArmed

    fun tryArmHold(
        px: Float,
        py: Float,
        nowMs: Long,
        isPen: Boolean,
        density: Float,
        config: GestureConfig,
        mapPagePointToScreen: (page: Int, x: Float, y: Float) -> Pt,
    ): Boolean {
        val candidate = activeCandidate(nowMs, config) ?: return false
        if (!isPen) {
            reset()
            return false
        }
        val (sx, sy) = mapPagePointToScreen(candidate.page, candidate.endX, candidate.endY)
        val near = hypot(px - sx, py - sy) < config.circleEndRadiusDp * density
        if (!near) {
            reset()
            return false
        }
        state = State.HoldArmed(candidate, px, py, nowMs)
        return true
    }

    fun shouldCancelHoldOnMove(px: Float, py: Float, slopPx: Float): Boolean {
        val armed = state as? State.HoldArmed ?: return false
        return hypot(px - armed.downX, py - armed.downY) > slopPx
    }

    /** Fires selection from the armed post-lift hold and clears detector state. */
    fun triggerArmedHold(): Pair<Candidate, Pt>? {
        val armed = state as? State.HoldArmed ?: return null
        armed.candidate.pendingRemoval = true
        state = State.Idle
        return armed.candidate to (armed.downX to armed.downY)
    }

    fun cancelHold() {
        if (state is State.HoldArmed) state = State.Idle
    }

    fun reset() {
        state = State.Idle
    }

    private fun activeCandidateUnchecked(): Candidate? = when (val s = state) {
        is State.Idle -> null
        is State.CandidateReady -> s.candidate
        is State.HoldArmed -> s.candidate
    }
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
    val corners = (a.dropLast(1) + b.dropLast(1)).toMutableList()
    // Drop near-duplicates and corners that are really straight lines.
    var changed = true
    while (changed && corners.size > 3) {
        changed = false
        for (i in corners.indices) {
            val prev = corners[(i - 1 + corners.size) % corners.size]
            val cur = corners[i]
            val next = corners[(i + 1) % corners.size]
            if (dist(prev, cur) < eps * 1.15f || turnAngle(prev, cur, next) < 24.0) {
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
 * Turns a hand-drawn stroke into a clean shape: line, arrow, elbow/zig-zag, triangle,
 * rectangle, square, diamond, pentagon, hexagon, star, circle, or ellipse.
 *
 * Uses uniform arc-length resampling, loop overshoot trimming, and head-to-head geometric
 * fitting across canonical shape templates so common shapes (circle, square, rectangle,
 * triangle) snap reliably instead of collapsing into irregular polygons.
 */
object ShapeRecognizer {
    /**
     * Recognizes a hand-drawn stroke and returns one or more crisp strokes (e.g. two strokes for a
     * single-stroke hand-drawn arrow: shaft + arrowhead).
     */
    fun recognizeMulti(raw: List<Pt>, forceNearest: Boolean = false): List<List<Pt>>? {
        if (raw.size < 4) return null
        val len = pathLength(raw)
        if (len < 20f) return null
        val bx0 = box(raw)
        val diag0 = bx0.diag.coerceAtLeast(1f)
        if (forceNearest && diag0 < 14f) return null

        val start = raw.first()
        val end = raw.last()
        val endDist = dist(start, end)
        val straightRatio = endDist / len

        // 1. Straight line (with automatic horizontal/vertical snapping within 6°).
        if (straightRatio > 0.88f) {
            return listOf(densify(listOf(start, snapAxisEnd(start, end))))
        }

        // 2. Single-stroke hand-drawn arrow: straight shaft + acute V/chevron tip at the end.
        detectSingleStrokeArrow(raw, len, diag0)?.let { return it }

        val closed = isClosedLoop(raw)
        val nearlyClosed = closed || (endDist < max(bx0.w, bx0.h) * 0.58f && len > max(bx0.w, bx0.h) * 1.75f)

        if (!nearlyClosed) {
            // Open elbow or zig-zag made of 2–3 straight segments.
            val pts = simplify(raw, diag0 * 0.06f)
            if (pts.size in 3..4 && abs(pathLength(pts) - len) / len < 0.14f) return listOf(densify(pts))
            if (forceNearest) {
                if (straightRatio >= 0.72f && endDist >= 20f) {
                    return listOf(densify(listOf(start, snapAxisEnd(start, end))))
                }
                val relaxedPts = simplify(raw, diag0 * 0.08f)
                if (relaxedPts.size in 3..4 && abs(pathLength(relaxedPts) - len) / len < 0.24f) {
                    return listOf(densify(relaxedPts))
                }
            }
            return null
        }

        // 3. Clean closed loop: trim small overshoot tail and resample uniformly by arc length
        //    so slow dwell points while holding the stylus never bias centroids or fit errors.
        val trimmed = trimLoopOvershoot(raw, diag0)
        val sample = resampleUniform(trimmed, 96)
        val bx = box(sample)
        val diag = bx.diag.coerceAtLeast(1f)
        if (bx.w < 6f || bx.h < 6f) {
            return listOf(densify(listOf(start, snapAxisEnd(start, end))))
        }

        // Compute loop area & isoperimetric circularity (circle = 1.0, square = 0.785, triangle <= 0.605)
        val area = polygonAreaAbs(sample)
        val perim = pathLength(sample) + dist(sample.last(), sample.first())
        val circularity = if (perim > 1f) (4f * Math.PI.toFloat() * area) / (perim * perim) else 0f

        // --- Candidate 1: Five-pointed star (check before smooth shapes only if strongly alternating) ---
        val rawCorners = closedCorners(sample, diag * 0.055f)
        val rawPolyErr = polygonError(sample, rawCorners, diag)
        if (rawCorners.size in 9..11 && rawPolyErr < if (forceNearest) 0.085f else 0.055f) {
            val d = rawCorners.map { hypot(it.first - bx.cx, it.second - bx.cy) }
            val n = rawCorners.size
            val alternates = d.indices.count { i -> (d[i] > d[(i + 1) % n]) != (d[(i + 1) % n] > d[(i + 2) % n]) } >= n - 2
            if (alternates) return ShapeBuilder.build(ShapeKind.STAR, bx.l, bx.t, bx.r, bx.b)
        }

        // --- Candidate 2: Circle / Ellipse (centroid + axis-aligned semi-axes fit) ---
        var meanX = 0f
        var meanY = 0f
        for ((x, y) in sample) {
            meanX += x
            meanY += y
        }
        meanX /= sample.size
        meanY /= sample.size
        // Blend centroid with bounding-box center so slight asymmetry doesn't shift the circle off the ink
        val ecx = meanX * 0.55f + bx.cx * 0.45f
        val ecy = meanY * 0.55f + bx.cy * 0.45f
        val erx = bx.w * 0.5f
        val ery = bx.h * 0.5f
        val ellipseOutline = ellipse(ecx, ecy, erx, ery, snapCircleRatio = 0.22f)
        val ellipseErr = meanCurveDist(sample, ellipseOutline, diag)

        // --- Candidate 3: Axis-aligned Square / Rectangle ---
        val rectPoly = axisAlignedRectCorners(bx, snapSquareRatio = 0.20f)
        val rectErr = polygonError(sample, rectPoly, diag)
        val rectCornerCoverage = cornerNeighborhoodCoverage(sample, rectPoly, diag * 0.24f)

        // --- Candidate 4: Diamond (45° rhombus) ---
        val diamondPoly = listOf(bx.cx to bx.t, bx.r to bx.cy, bx.cx to bx.b, bx.l to bx.cy)
        val diamondErr = polygonError(sample, diamondPoly, diag)
        val diamondCoverage = cornerNeighborhoodCoverage(sample, diamondPoly, diag * 0.24f)

        // --- Candidate 5: Oriented (rotated) Rectangle (only when user drew a clearly tilted box) ---
        val obbPoly = orientedBoundingRect(sample)
        val obbErr = if (obbPoly != null) polygonError(sample, obbPoly, diag) else Float.MAX_VALUE
        val obbCoverage = if (obbPoly != null) cornerNeighborhoodCoverage(sample, obbPoly, diag * 0.24f) else 0

        // --- Candidate 6: Best 3-vertex Triangle ---
        val triPoly = fitBestTriangle(sample)
        val triErr = if (triPoly != null) polygonError(sample, triPoly, diag) else Float.MAX_VALUE
        val triCoverage = if (triPoly != null) cornerNeighborhoodCoverage(sample, triPoly, diag * 0.24f) else 0

        // --- Candidate 7: Clean Pentagon / Hexagon (strict corner sharpness required) ---
        val strictCorners = closedCorners(sample, diag * 0.075f)
        val sharpPolyCandidate: Pair<List<Pt>, Float>? = if (strictCorners.size in 5..6) {
            val allSharp = strictCorners.indices.all { i ->
                val prev = strictCorners[(i - 1 + strictCorners.size) % strictCorners.size]
                val cur = strictCorners[i]
                val next = strictCorners[(i + 1) % strictCorners.size]
                turnAngle(prev, cur, next) >= 42.0
            }
            if (allSharp) {
                val err = polygonError(sample, strictCorners, diag)
                strictCorners to err
            } else null
        } else null

        // Score every candidate cleanly (lower is better).
        // Add mild structural priors so:
        // - Circles never lose to squares when corners are empty (coverage < 3)
        // - Squares/rectangles with 4 visited corners & lower fit error beat circles reliably
        // - Triangles with circularity < 0.70 and sharp 3-corner fit beat squares & circles
        // - Axis-aligned squares/rects are preferred over slightly tilted OBBs unless tilted strongly
        data class Candidate(val score: Float, val rawErr: Float, val build: () -> List<Pt>)

        val candidates = ArrayList<Candidate>(6)

        // Circle / Ellipse score
        val circleBonus = when {
            circularity >= 0.85f && rectCornerCoverage <= 2 -> -0.014f
            circularity >= 0.80f && rectCornerCoverage <= 2 -> -0.008f
            rectCornerCoverage >= 4 && circularity < 0.83f -> +0.015f
            else -> 0f
        }
        candidates += Candidate(
            score = ellipseErr + circleBonus,
            rawErr = ellipseErr,
            build = { ellipseOutline },
        )

        // Axis-aligned Square / Rectangle score
        val rectPenalty = when {
            rectCornerCoverage == 4 -> -0.006f
            rectCornerCoverage == 3 -> +0.008f
            else -> +0.045f // Empty corners => definitely not an axis-aligned rectangle!
        }
        candidates += Candidate(
            score = rectErr + rectPenalty,
            rawErr = rectErr,
            build = { densify(rectPoly + rectPoly.first()) },
        )

        // Diamond score (require all 4 vertices visited & clearly better than circle)
        val diamondPenalty = when {
            diamondCoverage == 4 -> +0.005f
            diamondCoverage == 3 -> +0.022f
            else -> +0.060f
        }
        candidates += Candidate(
            score = diamondErr + diamondPenalty,
            rawErr = diamondErr,
            build = { densify(diamondPoly + diamondPoly.first()) },
        )

        // Rotated Rectangle score (only win if clearly better than axis-aligned rect & diamond)
        if (obbPoly != null && obbCoverage >= 3) {
            val obbPenalty = if (obbCoverage == 4) +0.012f else +0.030f
            candidates += Candidate(
                score = obbErr + obbPenalty,
                rawErr = obbErr,
                build = { densify(obbPoly + obbPoly.first()) },
            )
        }

        // Triangle score
        if (triPoly != null && triCoverage == 3) {
            val triBias = if (circularity < 0.72f) -0.004f else +0.025f
            candidates += Candidate(
                score = triErr + triBias,
                rawErr = triErr,
                build = { densify(snapTriangleBase(triPoly).let { it + it.first() }) },
            )
        }

        // Pentagon / Hexagon score (must beat circle AND square by a real margin)
        if (sharpPolyCandidate != null) {
            val (polyPts, pErr) = sharpPolyCandidate
            if (pErr < ellipseErr * 0.72f && pErr < rectErr * 0.80f) {
                candidates += Candidate(
                    score = pErr + 0.008f,
                    rawErr = pErr,
                    build = { densify(polyPts + polyPts.first()) },
                )
            }
        }

        val best = candidates.minByOrNull { it.score } ?: return null
        val maxErr = if (forceNearest) 0.135f else 0.088f
        if (best.rawErr <= maxErr) {
            return listOf(best.build())
        }
        return null
    }

    fun recognize(raw: List<Pt>, forceNearest: Boolean = false): List<Pt>? =
        recognizeMulti(raw, forceNearest)?.firstOrNull()

    /** Trims small overshoot tail at the end of a hand-drawn closed loop so it doesn't skew bounds or corners. */
    private fun trimLoopOvershoot(raw: List<Pt>, diag: Float): List<Pt> {
        if (raw.size < 14) return raw
        val start = raw.first()
        val half = raw.size / 2
        var bestIdx = raw.lastIndex
        var bestDist = dist(start, raw.last())
        for (i in half until raw.size) {
            val d = dist(start, raw[i])
            if (d < bestDist) {
                bestDist = d
                bestIdx = i
            }
        }
        // Only trim if the closest pass to start is genuinely close and leaves most of the loop intact
        return if (bestIdx >= (raw.size * 0.65f).toInt() && bestIdx < raw.lastIndex && bestDist < diag * 0.14f) {
            raw.subList(0, bestIdx + 1)
        } else raw
    }

    /** Resamples a polyline to [count] points spaced evenly by arc length. */
    private fun resampleUniform(pts: List<Pt>, count: Int): List<Pt> {
        if (pts.size <= 2 || count <= 2) return pts
        val total = pathLength(pts)
        if (total < 1e-3f) return pts
        val step = total / (count - 1)
        val out = ArrayList<Pt>(count)
        out += pts.first()
        var accum = 0f
        var prev = pts.first()
        var i = 1
        while (i < pts.size && out.size < count - 1) {
            val cur = pts[i]
            val seg = dist(prev, cur)
            if (accum + seg >= step && seg > 1e-4f) {
                val t = ((step - accum) / seg).coerceIn(0f, 1f)
                val nx = prev.first + (cur.first - prev.first) * t
                val ny = prev.second + (cur.second - prev.second) * t
                val np = nx to ny
                out += np
                prev = np
                accum = 0f
            } else {
                accum += seg
                prev = cur
                i++
            }
        }
        while (out.size < count) out += pts.last()
        return out
    }

    private fun polygonAreaAbs(pts: List<Pt>): Float {
        if (pts.size < 3) return 0f
        var sum = 0f
        for (i in pts.indices) {
            val a = pts[i]
            val b = pts[(i + 1) % pts.size]
            sum += a.first * b.second - b.first * a.second
        }
        return abs(sum) * 0.5f
    }

    /** Mean Euclidean distance from each sample point to the nearest point on a dense closed curve, normalized by [diag]. */
    private fun meanCurveDist(sample: List<Pt>, curve: List<Pt>, diag: Float): Float {
        if (sample.isEmpty() || curve.isEmpty()) return Float.MAX_VALUE
        var sum = 0f
        for (p in sample) {
            var best = Float.MAX_VALUE
            for (q in curve) {
                val d = hypot(p.first - q.first, p.second - q.second)
                if (d < best) best = d
            }
            sum += best
        }
        return sum / sample.size / diag
    }

    /** Counts how many polygon corners have at least one sample point within [radius]. */
    private fun cornerNeighborhoodCoverage(sample: List<Pt>, corners: List<Pt>, radius: Float): Int {
        var count = 0
        for (c in corners) {
            if (sample.any { dist(it, c) <= radius }) count++
        }
        return count
    }

    private fun axisAlignedRectCorners(bx: Box, snapSquareRatio: Float): List<Pt> {
        var l = bx.l
        var t = bx.t
        var r = bx.r
        var b = bx.b
        if (abs(bx.w - bx.h) / max(bx.w, bx.h).coerceAtLeast(1f) < snapSquareRatio) {
            val s = (bx.w + bx.h) * 0.5f
            l = bx.cx - s * 0.5f
            r = bx.cx + s * 0.5f
            t = bx.cy - s * 0.5f
            b = bx.cy + s * 0.5f
        }
        return listOf(l to t, r to t, r to b, l to b)
    }

    /** PCA-aligned oriented bounding rectangle (snapped to square if aspect ratio is near 1:1). */
    private fun orientedBoundingRect(pts: List<Pt>): List<Pt>? {
        if (pts.size < 8) return null
        var mx = 0.0; var my = 0.0
        for ((x, y) in pts) { mx += x; my += y }
        mx /= pts.size; my /= pts.size
        var sxx = 0.0; var syy = 0.0; var sxy = 0.0
        for ((x, y) in pts) {
            val dx = x - mx; val dy = y - my
            sxx += dx * dx; syy += dy * dy; sxy += dx * dy
        }
        val theta = 0.5 * atan2(2.0 * sxy, sxx - syy)
        val degMod = ((Math.toDegrees(theta) % 90.0) + 90.0) % 90.0
        // If already near axis-aligned (within 15°), let the axis-aligned rect candidate handle it cleanly
        if (degMod < 15.0 || degMod > 75.0) return null
        val ux = cos(theta).toFloat(); val uy = sin(theta).toFloat()
        val vx = -uy; val vy = ux
        var minU = Float.MAX_VALUE; var maxU = -Float.MAX_VALUE
        var minV = Float.MAX_VALUE; var maxV = -Float.MAX_VALUE
        for ((x, y) in pts) {
            val dx = x - mx.toFloat(); val dy = y - my.toFloat()
            val u = dx * ux + dy * uy
            val v = dx * vx + dy * vy
            if (u < minU) minU = u; if (u > maxU) maxU = u
            if (v < minV) minV = v; if (v > maxV) maxV = v
        }
        var w = maxU - minU
        var h = maxV - minV
        if (w < 8f || h < 8f) return null
        val cu = (minU + maxU) * 0.5f
        val cv = (minV + maxV) * 0.5f
        if (abs(w - h) / max(w, h) < 0.16f) {
            val s = (w + h) * 0.5f
            w = s; h = s
        }
        val hw = w * 0.5f; val hh = h * 0.5f
        fun toPt(u: Float, v: Float): Pt = (mx.toFloat() + u * ux + v * vx) to (my.toFloat() + u * uy + v * vy)
        return listOf(
            toPt(cu - hw, cv - hh),
            toPt(cu + hw, cv - hh),
            toPt(cu + hw, cv + hh),
            toPt(cu - hw, cv + hh),
        )
    }

    /** Finds the best-fitting 3 vertices of a closed loop (maximum-area inscribed triangle). */
    private fun fitBestTriangle(pts: List<Pt>): List<Pt>? {
        if (pts.size < 6) return null
        // Vertex A: point farthest from centroid
        var cx = 0f; var cy = 0f
        for ((x, y) in pts) { cx += x; cy += y }
        cx /= pts.size; cy /= pts.size
        var idxA = 0
        var bestD = -1f
        for (i in pts.indices) {
            val d = hypot(pts[i].first - cx, pts[i].second - cy)
            if (d > bestD) { bestD = d; idxA = i }
        }
        val a = pts[idxA]
        // Vertex B: point farthest from A
        var idxB = idxA
        bestD = -1f
        for (i in pts.indices) {
            val d = dist(a, pts[i])
            if (d > bestD) { bestD = d; idxB = i }
        }
        val b = pts[idxB]
        // Vertex C: point maximizing triangle area (distance to line AB)
        var idxC = idxA
        var bestArea = -1f
        for (i in pts.indices) {
            val ar = abs(orient2d(a, b, pts[i]))
            if (ar > bestArea) { bestArea = ar; idxC = i }
        }
        val c = pts[idxC]
        // Refine A once maximizing area against BC so all 3 vertices sit right on the true triangle tips
        var idxA2 = idxA
        var bestArea2 = -1f
        for (i in pts.indices) {
            val ar = abs(orient2d(b, c, pts[i]))
            if (ar > bestArea2) { bestArea2 = ar; idxA2 = i }
        }
        val a2 = pts[idxA2]
        if (dist(a2, b) < 12f || dist(b, c) < 12f || dist(c, a2) < 12f) return null
        return listOf(a2, b, c)
    }

    /** Gently levels a nearly horizontal triangle base (within 9°) so hand-drawn triangles sit neat and flat. */
    private fun snapTriangleBase(tri: List<Pt>): List<Pt> {
        if (tri.size != 3) return tri
        val out = tri.toMutableList()
        for (i in 0 until 3) {
            val j = (i + 1) % 3
            val p1 = out[i]
            val p2 = out[j]
            val dx = p2.first - p1.first
            val dy = p2.second - p1.second
            val ang = abs(Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())))
            if (ang < 9.0 || ang > 171.0) {
                val midY = (p1.second + p2.second) * 0.5f
                out[i] = p1.first to midY
                out[j] = p2.first to midY
                break
            }
        }
        return out
    }

    /** Snaps nearly horizontal or vertical lines (within 6°) to dead-level / dead-plumb. */
    private fun snapAxisEnd(a: Pt, b: Pt): Pt {
        val dx = b.first - a.first
        val dy = b.second - a.second
        val angDeg = abs(Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())))
        return when {
            angDeg < 6.0 || angDeg > 174.0 -> b.first to a.second
            abs(angDeg - 90.0) < 6.0 -> a.first to b.second
            else -> b
        }
    }

    /** Detects a single-stroke arrow (long straight shaft ending with a sharp acute hook/chevron). */
    private fun detectSingleStrokeArrow(raw: List<Pt>, len: Float, diag: Float): List<List<Pt>>? {
        if (raw.size < 8 || diag < 42f) return null
        val simp = simplify(raw, diag * 0.045f)
        if (simp.size !in 3..4) return null
        val tail = simp.first()
        val tip = simp[1]
        val shaftLen = dist(tail, tip)
        if (shaftLen < len * 0.50f || shaftLen < 36f) return null
        // Verify the shaft portion was drawn fairly straight.
        val tipIdx = raw.indices.minByOrNull { dist(raw[it], tip) } ?: return null
        if (tipIdx < 3 || tipIdx >= raw.lastIndex) return null
        val shaftPath = pathLength(raw.subList(0, tipIdx + 1))
        if (shaftLen / shaftPath.coerceAtLeast(1f) < 0.84f) return null
        val wing1 = simp[2]
        val wingLen = dist(tip, wing1)
        if (wingLen !in (shaftLen * 0.12f)..(shaftLen * 0.50f)) return null
        // Turn angle at the tip must reverse sharply (acute arrowhead barb: 115°..172° turn).
        val turn = turnAngle(tail, tip, wing1)
        if (turn !in 115.0..172.0) return null
        val snappedTip = snapAxisEnd(tail, tip)
        return ShapeBuilder.build(ShapeKind.ARROW, tail.first, tail.second, snappedTip.first, snappedTip.second)
    }

    private fun ellipse(cx: Float, cy: Float, rx0: Float, ry0: Float, snapCircleRatio: Float = 0.22f): List<Pt> {
        var rx = rx0
        var ry = ry0
        if (abs(rx - ry) / max(rx, ry).coerceAtLeast(1f) < snapCircleRatio) {
            val r = (rx + ry) * 0.5f
            rx = r
            ry = r
        }
        val n = 96
        return List(n + 1) { i ->
            val a = (i / n.toFloat()) * 2.0 * Math.PI
            (cx + rx * cos(a).toFloat()) to (cy + ry * sin(a).toFloat())
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

private data class ScribbleThresholds(
    val minPts: Int,
    val minLen: Float,
    val maxNetRatio: Float,
    val minRev: Int,
    val minAxisRatio: Float,
    val minSharp: Int,
    val minZigRatio: Float,
    val minLoopTurns: Float,
    val minLoopRatio: Float,
)

private fun scribbleThresholds(config: GestureConfig): ScribbleThresholds = when (config.scribbleSensitivity) {
    ScribbleSensitivity.GENTLE -> ScribbleThresholds(
        minPts = 15,
        minLen = config.scribbleMinLengthPx.coerceAtLeast(115f),
        maxNetRatio = config.scribbleMaxNetDispRatio.coerceAtMost(0.30f),
        minRev = 5,
        minAxisRatio = 3.50f,
        minSharp = 5,
        minZigRatio = 3.50f,
        minLoopTurns = 3.0f,
        minLoopRatio = 3.80f,
    )
    ScribbleSensitivity.BALANCED -> ScribbleThresholds(
        minPts = 12,
        minLen = config.scribbleMinLengthPx.coerceAtLeast(85f),
        maxNetRatio = config.scribbleMaxNetDispRatio.coerceAtMost(0.35f),
        minRev = 4,
        minAxisRatio = 2.90f,
        minSharp = 4,
        minZigRatio = 2.90f,
        minLoopTurns = 2.5f,
        minLoopRatio = 3.30f,
    )
    ScribbleSensitivity.INSTANT -> ScribbleThresholds(
        minPts = 10,
        minLen = config.scribbleMinLengthPx.coerceAtLeast(62f),
        maxNetRatio = config.scribbleMaxNetDispRatio.coerceAtMost(0.40f),
        minRev = 4,
        minAxisRatio = 2.50f,
        minSharp = 4,
        minZigRatio = 2.50f,
        minLoopTurns = 2.2f,
        minLoopRatio = 2.80f,
    )
}

/**
 * Recognizes deliberate horizontal, diagonal, vertical, zig-zag, or dense swirl scrub gestures at screen scale.
 * Strictly rejects normal cursive/print handwriting (which advances along a baseline with occasional loops or crossings)
 * while cleanly accepting deliberate back-and-forth scrubbing over existing ink.
 */
internal fun isDeliberateScribble(points: List<Pt>, pageToDp: Float, durationMs: Long, config: GestureConfig): Boolean {
    val t = scribbleThresholds(config)
    if (durationMs !in 110L..3500L || points.size < t.minPts) return false
    val path = points.map { (x, y) -> x * pageToDp to y * pageToDp }
    val width = path.maxOf { it.first } - path.minOf { it.first }
    val height = path.maxOf { it.second } - path.minOf { it.second }
    val span = maxOf(width, height)
    if (span !in 18f..780f) return false
    val length = pathLength(path)
    // A deliberate scrub covers multiple passes across its own bounding span, far denser than normal handwriting
    if (length < maxOf(t.minLen, span * t.minAxisRatio)) return false
    val speedDpPerSec = length * 1000f / durationMs.coerceAtLeast(1L)
    if (speedDpPerSec < 95f) return false
    // A true scribble stays localized over the target instead of progressing forward like a written word
    val netDisp = dist(path.first(), path.last())
    if (netDisp / length > t.maxNetRatio) return false

    val minCrossings = when (config.scribbleSensitivity) {
        ScribbleSensitivity.GENTLE -> 5
        ScribbleSensitivity.BALANCED -> 4
        ScribbleSensitivity.INSTANT -> 4
    }
    // Multi-signal verification: must have either deliberate axis reversals, sharp zig-zags,
    // or dense multi-revolution swirling that repeatedly crosses itself.
    // Numbers (2,3,5,8,0) and cursive loops never satisfy these strict thresholds.
    return axisScribble(path, t.minRev, t.minAxisRatio) ||
        zigZagScribble(path, t.minSharp, t.minZigRatio) ||
        (loopScrubScribble(path, t.minLoopTurns, t.minLoopRatio) && selfCrossingScribble(path, minCrossings = minCrossings, minDensityRatio = t.minAxisRatio))
}

/** Detects dense scratch-outs that cross over their own path repeatedly within a compact region. */
private fun selfCrossingScribble(pts: List<Pt>, minCrossings: Int, minDensityRatio: Float = 2.5f): Boolean {
    val bx = box(pts)
    val diag = bx.diag
    if (diag < 16f || pathLength(pts) < diag * minDensityRatio) return false
    val s = simplify(pts, 4.0f)
    if (s.size < 7) return false
    var crossings = 0
    for (i in 0 until s.size - 3) {
        val a = s[i]; val b = s[i + 1]
        for (j in i + 2 until s.size - 1) {
            if (segmentsIntersect(a, b, s[j], s[j + 1])) {
                crossings++
                if (crossings >= minCrossings) return true
            }
        }
    }
    return false
}

private fun matchesAnyScribblePattern(pts: List<Pt>, t: ScribbleThresholds): Boolean =
    axisScribble(pts, t.minRev, t.minAxisRatio) ||
        zigZagScribble(pts, t.minSharp, t.minZigRatio) ||
        (loopScrubScribble(pts, t.minLoopTurns, t.minLoopRatio) && selfCrossingScribble(pts, minCrossings = maxOf(4, t.minRev), minDensityRatio = t.minAxisRatio))

/**
 * A fast back-and-forth zig-zag, diagonal cross-out, or tight circular scrub means "erase this".
 * Normal handwriting advances along a baseline and is filtered by net-displacement, axis reversal,
 * and canvas-ink-underneath checks.
 */
fun isScribble(
    pts: List<Pt>,
    config: GestureConfig = GestureConfig(),
): Boolean {
    val t = scribbleThresholds(config)
    if (pts.size < t.minPts) return false
    val totalLen = pathLength(pts)
    if (totalLen < t.minLen) return false
    val netDisp = dist(pts.first(), pts.last())
    if (netDisp / totalLen > t.maxNetRatio) return false

    if (matchesAnyScribblePattern(pts, t)) return true
    for (w in intArrayOf(20, 36, 60)) {
        if (pts.size > w) {
            val sub = pts.subList(pts.size - w, pts.size)
            val subLen = pathLength(sub)
            val subDisp = dist(sub.first(), sub.last())
            if (subLen >= t.minLen && (subDisp / subLen) <= t.maxNetRatio) {
                if (matchesAnyScribblePattern(sub, t)) return true
            }
        }
    }
    return false
}

fun isScribble(
    pts: List<Pt>,
    sensitivity: ScribbleSensitivity,
): Boolean = isScribble(pts, GestureConfig.fromSensitivity(sensitivity))

/**
 * Live continuous scribble detection called while moving: inspects multi-scale trailing windows
 * so users can scrub back and forth (horizontally, diagonally, or in tight circular swirls)
 * and erase without lifting.
 */
fun isLiveScribble(
    pts: List<Pt>,
    config: GestureConfig = GestureConfig(),
): Boolean {
    val t = scribbleThresholds(config)
    if (pts.size < t.minPts) return false
    for (w in intArrayOf(18, 32, 54)) {
        val win = if (pts.size > w) pts.subList(pts.size - w, pts.size) else pts
        if (win.size < t.minPts) continue
        val bx = box(win)
        val diag = bx.diag
        if (diag < 14f) continue
        val len = pathLength(win)
        if (len < t.minLen * 0.85f || len < diag * t.minAxisRatio) continue
        val netDisp = dist(win.first(), win.last())
        if (netDisp / len > t.maxNetRatio) continue

        if (matchesAnyScribblePattern(win, t)) return true
    }
    return false
}

fun isLiveScribble(
    pts: List<Pt>,
    sensitivity: ScribbleSensitivity,
): Boolean = isLiveScribble(pts, GestureConfig.fromSensitivity(sensitivity))

/** Dense scrubbing that does not follow a single axis, e.g. diagonal zig-zags over a word. */
private fun zigZagScribble(pts: List<Pt>, minSharp: Int = 4, minRatio: Float = 2.40f): Boolean {
    val bx = box(pts)
    val diag = bx.diag
    if (diag < 12f) return false
    if (pathLength(pts) < diag * minRatio) return false
    val s = simplify(pts, max(1.4f, diag * 0.035f))
    if (s.size < 5) return false
    var sharp = 0
    for (i in 1 until s.size - 1) if (turnAngle(s[i - 1], s[i], s[i + 1]) > 85.0) sharp++
    return sharp >= minSharp
}

/** Tight circular/oval swirling scrub over a word or symbol to erase it. */
private fun loopScrubScribble(pts: List<Pt>, minTurns: Float = 1.45f, minRatio: Float = 2.25f): Boolean {
    val bx = box(pts)
    val diag = bx.diag
    if (diag < 14f) return false
    if (pathLength(pts) < diag * minRatio) return false
    val minSide = min(bx.w, bx.h)
    if (minSide < diag * 0.16f) return false
    val cx = bx.cx
    val cy = bx.cy
    var accum = 0.0
    var prevAng = atan2((pts[0].second - cy).toDouble(), (pts[0].first - cx).toDouble())
    for (i in 1 until pts.size) {
        val (x, y) = pts[i]
        if (hypot(x - cx, y - cy) < diag * 0.06f) continue
        val ang = atan2((y - cy).toDouble(), (x - cx).toDouble())
        var delta = ang - prevAng
        while (delta > Math.PI) delta -= 2.0 * Math.PI
        while (delta < -Math.PI) delta += 2.0 * Math.PI
        accum += delta
        prevAng = ang
    }
    val revolutions = abs(accum) / (2.0 * Math.PI)
    return revolutions >= minTurns
}

private fun axisScribble(pts: List<Pt>, minReversals: Int = 4, minRatio: Float = 2.05f): Boolean {
    var mx = 0.0; var my = 0.0
    for ((x, y) in pts) { mx += x; my += y }
    mx /= pts.size; my /= pts.size
    var sxx = 0.0; var syy = 0.0; var sxy = 0.0
    for ((x, y) in pts) { val dx = x - mx; val dy = y - my; sxx += dx * dx; syy += dy * dy; sxy += dx * dy }
    val theta = 0.5 * atan2(2 * sxy, sxx - syy)
    val ux = cos(theta).toFloat(); val uy = sin(theta).toFloat()
    val proj = pts.map { (x, y) -> x * ux + y * uy }
    val extent = (proj.maxOrNull() ?: 0f) - (proj.minOrNull() ?: 0f)
    if (extent < 16f) return false
    if (pathLength(pts) < extent * minRatio) return false
    val thresh = max(4.2f, extent * 0.16f)
    var dir = 0; var anchor = proj[0]; var reversals = 0
    for (v in proj) {
        when (dir) {
            0 -> if (v - anchor > thresh) { dir = 1; anchor = v } else if (anchor - v > thresh) { dir = -1; anchor = v }
            1 -> if (v > anchor) anchor = v else if (anchor - v > thresh) { reversals++; dir = -1; anchor = v }
            else -> if (v < anchor) anchor = v else if (v - anchor > thresh) { reversals++; dir = 1; anchor = v }
        }
    }
    return reversals >= minReversals
}

private fun orient2d(a: Pt, b: Pt, c: Pt): Float =
    (b.first - a.first) * (c.second - a.second) - (b.second - a.second) * (c.first - a.first)

private fun segmentsIntersect(a: Pt, b: Pt, c: Pt, d: Pt): Boolean {
    if (max(a.first, b.first) < min(c.first, d.first) || max(c.first, d.first) < min(a.first, b.first)) return false
    if (max(a.second, b.second) < min(c.second, d.second) || max(c.second, d.second) < min(a.second, b.second)) return false
    val o1 = orient2d(a, b, c)
    val o2 = orient2d(a, b, d)
    val o3 = orient2d(c, d, a)
    val o4 = orient2d(c, d, b)
    return ((o1 > 0f && o2 < 0f) || (o1 < 0f && o2 > 0f)) &&
        ((o3 > 0f && o4 < 0f) || (o3 < 0f && o4 > 0f))
}

/**
 * High-accuracy polygon lasso selector backed by Android [Region] plus fast AABB rejection,
 * segment-vs-edge boundary crossing (so long straight strokes passing through a small lasso
 * are reliably caught in touching mode), and inner-box containment for large cards/images.
 */
class Lasso(points: List<Pt>, private val touching: Boolean = false, private val minInside: Float = 0.55f) {
    private val region = Region()
    private val poly: List<Pt>
    private val polyBox: Box

    init {
        val bx = box(points)
        polyBox = bx
        // Simplify lasso outline for fast segment-intersection tests while keeping full-res Region
        poly = if (points.size > 48) simplify(points, max(2f, bx.diag * 0.012f)) else points
        val path = Path()
        points.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
        path.close()
        region.setPath(path, Region(bx.l.toInt() - 2, bx.t.toInt() - 2, bx.r.toInt() + 2, bx.b.toInt() + 2))
    }

    val bounds: RectF get() = RectF(polyBox.l, polyBox.t, polyBox.r, polyBox.b)

    fun contains(x: Float, y: Float) = region.contains(x.toInt(), y.toInt())

    private fun crossesBoundary(a: Pt, b: Pt): Boolean {
        if (poly.size < 3) return false
        for (i in poly.indices) {
            val c = poly[i]
            val d = poly[(i + 1) % poly.size]
            if (segmentsIntersect(a, b, c, d)) return true
        }
        return false
    }

    fun selects(item: PageItem): Boolean {
        val ib = item.bounds
        // Fast O(1) AABB reject
        if (ib.right < polyBox.l - 4f || ib.left > polyBox.r + 4f ||
            ib.bottom < polyBox.t - 4f || ib.top > polyBox.b + 4f
        ) return false

        return when (item) {
            is InkItem -> {
                val pts = item.worldPoints()
                if (pts.isEmpty()) false
                else if (pts.size <= 3) {
                    pts.any { (x, y) -> contains(x, y) } ||
                        (touching && pts.zipWithNext().any { (a, b) -> crossesBoundary(a, b) })
                } else {
                    val step = max(1, pts.size / 48)
                    var inside = 0
                    var total = 0
                    var prev = pts[0]
                    for (i in pts.indices step step) {
                        val cur = pts[i]
                        total++
                        if (contains(cur.first, cur.second)) {
                            inside++
                            if (touching) return true
                        } else if (touching && i > 0 && crossesBoundary(prev, cur)) {
                            return true
                        }
                        prev = cur
                    }
                    // Check last point as well so short tails are never skipped
                    val last = pts.last()
                    if (touching && (contains(last.first, last.second) || crossesBoundary(prev, last))) return true
                    inside >= total * minInside
                }
            }
            else -> {
                val cx = ib.centerX(); val cy = ib.centerY()
                if (contains(cx, cy)) return true
                if (!touching) {
                    // Also allow selecting a large box/image if most of the lasso lies inside its bounds
                    val insideBoxCount = poly.count { (x, y) -> ib.contains(x, y) }
                    return poly.isNotEmpty() && insideBoxCount >= poly.size * 0.7f
                }
                val probes = listOf(
                    ib.left to ib.top, ib.right to ib.top, ib.left to ib.bottom, ib.right to ib.bottom,
                    cx to ib.top, cx to ib.bottom, ib.left to cy, ib.right to cy,
                )
                probes.any { (x, y) -> contains(x, y) } || poly.any { (x, y) -> ib.contains(x, y) }
            }
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
        var ex1 = x1
        var ey1 = y1
        // Gentle 5° horizontal/vertical axis snap for lines and arrows
        if (kind == ShapeKind.AUTO || kind == ShapeKind.LINE || kind == ShapeKind.ARROW || kind == ShapeKind.DOUBLE_ARROW) {
            val dx = x1 - x0
            val dy = y1 - y0
            if (hypot(dx, dy) > 16f) {
                val ang = abs(Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())))
                when {
                    ang < 5.0 || ang > 175.0 -> ey1 = y0
                    abs(ang - 90.0) < 5.0 -> ex1 = x0
                }
            }
        }
        var l = min(x0, ex1); var r = max(x0, ex1); var t = min(y0, ey1); var b = max(y0, ey1)
        var rx = (r - l) / 2f; var ry = (b - t) / 2f
        val cx = (l + r) / 2f; val cy = (t + b) / 2f
        // Gentle 8% aspect-ratio snap to exact Square / Circle for common geometric shapes
        if (kind in setOf(ShapeKind.RECT, ShapeKind.ROUNDED, ShapeKind.ELLIPSE, ShapeKind.DIAMOND) && max(rx, ry) > 12f) {
            if (abs(rx - ry) / max(rx, ry) < 0.08f) {
                val s = (rx + ry) * 0.5f
                rx = s; ry = s
                l = cx - s; r = cx + s; t = cy - s; b = cy + s
            }
        }
        fun closed(vararg p: Pt) = densifyPublic(p.toList() + p.first())
        return when (kind) {
            ShapeKind.AUTO, ShapeKind.LINE -> listOf(densifyPublic(listOf(x0 to y0, ex1 to ey1)))
            ShapeKind.ARROW -> listOf(densifyPublic(listOf(x0 to y0, ex1 to ey1)), arrowHead(x0, y0, ex1, ey1))
            ShapeKind.DOUBLE_ARROW -> listOf(
                densifyPublic(listOf(x0 to y0, ex1 to ey1)), arrowHead(x0, y0, ex1, ey1), arrowHead(ex1, ey1, x0, y0),
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
