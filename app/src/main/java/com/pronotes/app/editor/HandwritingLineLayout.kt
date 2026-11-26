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
    if (strokes.size == 1) return listOf(strokes)

    val heights = strokes.map { it.bounds.height() }.filter { it >= 10f }.sorted()
    val medianH = if (heights.isNotEmpty()) heights[heights.size / 2] else 32f
    val (smallMarks, mainStrokes) = strokes.partition { s ->
        s.bounds.width() < medianH * 0.55f && s.bounds.height() < medianH * 0.48f
    }
    val base = if (mainStrokes.isNotEmpty()) mainStrokes else strokes
    val sorted = base.sortedWith(compareBy<InkItem> { it.bounds.centerY() }.thenBy { it.bounds.left })
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
            val threshold = max(max(sHeight, gb.height().coerceAtLeast(12f)), medianH) * 0.65f
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

    // Attach dots, accents, punctuation, and small symbol marks to their nearest parent line
    if (mainStrokes.isNotEmpty()) {
        for (mark in smallMarks) {
            val mb = mark.bounds
            var bestIdx = 0
            var bestCost = Float.MAX_VALUE
            for (i in groupBounds.indices) {
                val gb = groupBounds[i]
                val dy = when {
                    mb.centerY() < gb.top -> gb.top - mb.centerY()
                    mb.centerY() > gb.bottom -> mb.centerY() - gb.bottom
                    else -> 0f
                }
                val dx = when {
                    mb.centerX() < gb.left -> gb.left - mb.centerX()
                    mb.centerX() > gb.right -> mb.centerX() - gb.right
                    else -> 0f
                }
                val cost = dy * 2.2f + dx * 0.35f + abs(mb.centerY() - gb.centerY()) * 0.3f
                if (cost < bestCost) {
                    bestCost = cost
                    bestIdx = i
                }
            }
            groups[bestIdx].add(mark)
            groupBounds[bestIdx].union(mb)
        }
    }

    // Preserve original stroke creation order (via index in input) within each line for accurate ML Kit temporal decoding
    val orderMap = strokes.withIndex().associate { (idx, item) -> item.id to idx }
    return groups.indices
        .sortedBy { groupBounds[it].top }
        .map { idx -> groups[idx].sortedBy { orderMap[it.id] ?: Int.MAX_VALUE } }
}

/**
 * Splits a single line of handwritten strokes into left-to-right word clusters using adaptive
 * horizontal gap thresholds, so search highlighting can pinpoint individual handwritten words.
 */
internal fun handwritingWordClusters(lineStrokes: List<InkItem>): List<List<InkItem>> {
    if (lineStrokes.size <= 1) return if (lineStrokes.isEmpty()) emptyList() else listOf(lineStrokes)
    val sorted = lineStrokes.sortedBy { it.bounds.left }
    val heights = sorted.map { it.bounds.height() }.filter { it >= 8f }.sorted()
    val medianH = if (heights.isNotEmpty()) heights[heights.size / 2] else 28f
    val wordGap = (medianH * 0.56f).coerceIn(14f, 64f)

    val clusters = ArrayList<MutableList<InkItem>>()
    var cur = mutableListOf(sorted.first())
    var curRight = sorted.first().bounds.right
    for (i in 1 until sorted.size) {
        val s = sorted[i]
        val gap = s.bounds.left - curRight
        if (gap > wordGap) {
            clusters.add(cur)
            cur = mutableListOf(s)
            curRight = s.bounds.right
        } else {
            cur.add(s)
            if (s.bounds.right > curRight) curRight = s.bounds.right
        }
    }
    clusters.add(cur)
    return clusters
}

/** Smartly joins newly converted handwriting onto an existing line of text with natural spacing. */
internal fun smartJoinInlineText(existing: String, addition: String): String {
    val left = existing.trimEnd()
    val right = addition.trimStart()
    if (left.isEmpty()) return right
    if (right.isEmpty()) return left
    val noSpaceBefore = setOf('.', ',', '!', '?', ':', ';', ')', ']', '}', '%', '°')
    val noSpaceAfter = setOf('(', '[', '{', '-', '/', '$', '#', '£', '€', '₹')
    return when {
        right.first() in noSpaceBefore -> left + right
        left.last() in noSpaceAfter -> left + right
        else -> "$left $right"
    }
}

/**
 * Finds an existing [TextItem] on [page] that sits immediately to the left of [union] on the same
 * horizontal baseline so newly converted words continue seamlessly in one straight line instead
 * of scattering into disconnected floating text boxes.
 */
internal fun findSameLineTextTarget(
    page: Page,
    union: RectF,
    excludeIds: Set<Long>,
): TextItem? {
    val lineTol = max(union.height() * 0.85f, 26f)
    var best: TextItem? = null
    var bestGap = Float.MAX_VALUE
    for (item in page.items) {
        if (item !is TextItem || item.id in excludeIds || item.rot != 0f) continue
        val r = item.raw
        // Compare against the last line's vertical band of the existing TextItem
        val lineCount = item.text.count { it == '\n' } + 1
        val singleLineH = (r.height() / lineCount.coerceAtLeast(1)).coerceAtLeast(item.size * 1.05f)
        val lastLineTop = r.bottom - singleLineH
        val lastLineCenterY = (lastLineTop + r.bottom) * 0.5f
        val yDiff = abs(union.centerY() - lastLineCenterY)
        val tol = max(lineTol, singleLineH * 0.85f)
        if (yDiff > tol) continue

        // Must be to the right of the existing text's left edge and within reasonable word/phrase gap
        val maxHorizGap = max(item.size * 8.5f, page.width * 0.34f)
        val gap = union.left - r.right
        if (union.left >= r.left - item.size * 0.6f && gap in (-item.size * 1.4f)..maxHorizGap) {
            val score = abs(gap) + yDiff * 1.5f
            if (score < bestGap) {
                bestGap = score
                best = item
            }
        }
    }
    return best
}

/**
 * Finds the nearest [TextItem] directly above [union] so a new converted line aligns neatly to the
 * same left margin, font size, and uniform line rhythm.
 */
internal fun findParagraphAboveTarget(
    page: Page,
    union: RectF,
    excludeIds: Set<Long>,
): TextItem? {
    var best: TextItem? = null
    var bestDy = Float.MAX_VALUE
    for (item in page.items) {
        if (item !is TextItem || item.id in excludeIds || item.rot != 0f) continue
        val r = item.raw
        val dy = union.top - r.bottom
        val maxGap = max(item.size * 3.4f, 110f)
        val xDiff = abs(union.left - r.left)
        if (dy in (-item.size * 0.4f)..maxGap && xDiff <= max(item.size * 6.5f, 180f)) {
            if (dy < bestDy) {
                bestDy = dy
                best = item
            }
        }
    }
    return best
}

/**
 * Computes (x, y, fontSize) for cleanly aligned linear text conversion, snapping to an existing
 * paragraph margin/size when writing line-by-line, or snapping onto ruled paper baselines so
 * converted text always sits in crisp, straight lines rather than scattered positions.
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
    val availWidth = (page.width - x - 28f).coerceAtLeast(100f)
    var size = estimatedSize.coerceIn(20f, 56f)
    val style = TextStyleSpec(font = font)
    val measured = measureText(clean, size, x, union.top, style)
    if (measured.width() > availWidth && measured.width() > 1f) {
        size = (size * (availWidth / measured.width())).coerceIn(16f, size)
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
