package com.pronotes.app.editor

import android.graphics.RectF
import com.pronotes.app.data.PaperStyle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Groups handwritten ink strokes into horizontal reading lines sorted top-to-bottom,
 * with strokes inside each line ordered left-to-right.
 */
internal fun handwritingLines(strokes: List<InkItem>): List<List<InkItem>> {
    if (strokes.isEmpty()) return emptyList()
    val sorted = strokes.sortedWith(compareBy<InkItem> { it.bounds.centerY() }.thenBy { it.bounds.left })
    val groups = ArrayList<MutableList<InkItem>>()
    val groupBounds = ArrayList<RectF>()

    for (stroke in sorted) {
        val sb = stroke.bounds
        val sHeight = sb.height().coerceAtLeast(12f)
        var bestIdx = -1
        var bestDist = Float.MAX_VALUE
        for (i in groupBounds.indices) {
            val gb = groupBounds[i]
            val overlap = min(sb.bottom, gb.bottom) - max(sb.top, gb.top)
            val threshold = max(sHeight, gb.height().coerceAtLeast(12f)) * 0.65f
            val centerDist = abs(sb.centerY() - gb.centerY())
            if (overlap > -threshold * 0.25f || centerDist <= threshold) {
                if (centerDist < bestDist) {
                    bestDist = centerDist
                    bestIdx = i
                }
            }
        }
        if (bestIdx >= 0) {
            groups[bestIdx].add(stroke)
            groupBounds[bestIdx].union(sb)
        } else {
            groups.add(mutableListOf(stroke))
            groupBounds.add(RectF(sb))
        }
    }

    return groups.indices
        .sortedBy { groupBounds[it].top }
        .map { idx -> groups[idx].sortedBy { it.bounds.left } }
}

/**
 * Computes (fontSize, topY) for an automatically converted single line of handwriting,
 * optionally snapping onto ruled paper lines and ensuring it fits inside [page.width].
 */
internal fun singleLineLayout(
    page: Page,
    text: String,
    font: TextFont,
    x: Float,
    union: RectF,
    estimatedSize: Float,
    alignToLines: Boolean,
): Pair<Float, Float>? {
    val clean = text.trim()
    if (clean.isEmpty()) return null
    val availWidth = (page.width - x - 24f).coerceAtLeast(80f)
    var size = estimatedSize.coerceIn(18f, 68f)
    val style = TextStyleSpec(font = font)
    val measured = measureText(clean, size, x, union.top, style)
    if (measured.width() > availWidth && measured.width() > 1f) {
        size = (size * (availWidth / measured.width())).coerceIn(14f, size)
    }
    var y = union.top.coerceIn(0f, (page.height - size * 1.4f).coerceAtLeast(0f))
    if (alignToLines && (page.paper == PaperStyle.RULED || page.paper == PaperStyle.CORNELL || page.paper == PaperStyle.GRID)) {
        val spacing = (48f * page.lineScale).coerceIn(24f, 120f)
        val baselineOffset = size * 0.95f
        val snappedLine = ((union.bottom) / spacing).roundToInt().coerceAtLeast(1) * spacing
        y = (snappedLine - baselineOffset).coerceIn(0f, (page.height - size * 1.4f).coerceAtLeast(0f))
    }
    return size to y
}

// Estimate the baseline from similarly sized strokes, excluding dots and long underlines.
internal fun levelInkLine(page: Page, line: List<InkItem>): Map<Long, PageItem>? {
    if (line.size !in 5..160 || line.any { it !in page.items }) return null
    val heights = line.map { it.bounds.height() }.sorted()
    val height = heights[heights.size / 2]
    if (height < 5f) return null
    val anchors = line.map { it.bounds }.filter { it.height() in height * 0.65f..height * 1.5f && it.width() < height * 3 }
    if (anchors.size < 5) return null
    val left = anchors.minOf { it.centerX() }
    val right = anchors.maxOf { it.centerX() }
    if (right - left < height * 3) return null
    val slopes = mutableListOf<Float>()
    for ((i, a) in anchors.withIndex()) for (j in i + 1 until anchors.size) {
        val b = anchors[j]
        val dx = b.centerX() - a.centerX()
        if (abs(dx) > (right - left) / 3) slopes += (b.bottom - a.bottom) / dx
    }
    if (slopes.size < 3) return null
    slopes.sort()
    val slope = slopes[slopes.size / 2]
    if (abs(slope) !in 0.025f..0.18f) return null
    val offsets = anchors.map { it.bottom - slope * (it.centerX() - left) }.sorted()
    val baseline = offsets[offsets.size / 2]
    if (offsets.count { abs(it - baseline) <= height * 0.18f } < anchors.size * 0.8f) return null
    val degrees = -Math.toDegrees(kotlin.math.atan(slope.toDouble())).toFloat()
    val matrix = android.graphics.Matrix().apply { setRotate(degrees, left, baseline) }
    val corrected = line.associate { it.id to it.transformed(matrix, 1f, degrees) }
    val bounds = RectF(0f, 0f, page.width, page.height)
    if (corrected.values.any { !bounds.contains(it.bounds) }) return null
    val previous = RectF(line.first().bounds)
    val next = RectF(corrected.values.first().bounds)
    line.forEach { previous.union(it.bounds) }
    corrected.values.forEach { next.union(it.bounds) }
    if (page.items.any { it.id !in corrected && RectF.intersects(next, it.bounds) && !RectF.intersects(previous, it.bounds) }) return null
    return corrected
}
