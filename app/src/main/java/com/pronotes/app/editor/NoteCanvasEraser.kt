package com.pronotes.app.editor

import android.graphics.RectF
import android.text.TextPaint
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.Intersection.intersects
import androidx.ink.geometry.MutableParallelogram
import androidx.ink.geometry.MutableSegment
import androidx.ink.geometry.MutableVec
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val WORD_TOKEN_REGEX = Regex("\\S+")
private val eraserTextPaint = TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG)

internal fun NoteCanvasView.eraserAllows(item: PageItem): Boolean {
    if (settings.eraseHighlighterOnly) {
        return item is InkItem && item.isHighlighter
    }
    val ef = settings.eraserFilter
    return when (item) {
        is InkItem -> if (item.isHighlighter) ef.highlighter else ef.ink
        is FillItem -> ef.shapes
        is TextItem, is WriteBoxItem -> ef.text
        is StickyItem, is TapeItem, is WebLinkItem, is MindNodeItem -> ef.stickyAndTape
        is ImageItem -> ef.images
    }
}

/** Rotates point [p] by [deg] degrees around ([cx], [cy]). */
private fun rotateAround(p: Pt, cx: Float, cy: Float, deg: Float): Pt {
    if (deg == 0f) return p
    val rad = Math.toRadians(deg.toDouble())
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()
    val dx = p.first - cx
    val dy = p.second - cy
    return (cx + dx * c - dy * s) to (cy + dx * s + dy * c)
}

/** Exact Liang-Barsky check for whether segment ([ax],[ay])->([bx],[by]) intersects [rect]. */
private fun segmentIntersectsRect(ax: Float, ay: Float, bx: Float, by: Float, rect: RectF): Boolean {
    if (rect.contains(ax, ay) || rect.contains(bx, by)) return true
    val dx = bx - ax
    val dy = by - ay
    var t0 = 0f
    var t1 = 1f
    val p = floatArrayOf(-dx, dx, -dy, dy)
    val q = floatArrayOf(ax - rect.left, rect.right - ax, ay - rect.top, rect.bottom - ay)
    for (i in 0 until 4) {
        if (p[i] == 0f) {
            if (q[i] < 0f) return false
        } else {
            val t = q[i] / p[i]
            if (p[i] < 0f) {
                if (t > t1) return false
                if (t > t0) t0 = t
            } else {
                if (t < t0) return false
                if (t < t1) t1 = t
            }
        }
    }
    return t0 <= t1
}

/**
 * Erases ONLY the specific word(s) inside [item] touched by the eraser stroke segment [prev]->[cur],
 * preserving the rest of the line/paragraph. Returns `(changed, updatedItemOrNull)`.
 */
internal fun eraseWordsFromTextItem(
    item: TextItem,
    prev: Pt,
    cur: Pt,
    radius: Float,
): Pair<Boolean, TextItem?> {
    val rawBox = item.raw
    val a = rotateAround(prev, rawBox.centerX(), rawBox.centerY(), -item.rot)
    val b = rotateAround(cur, rawBox.centerX(), rawBox.centerY(), -item.rot)

    // Surgical padding so a wide eraser doesn't wipe out adjacent untouched words
    val padX = min(radius * 0.42f, item.size * 0.22f).coerceAtLeast(3f)
    val padY = min(radius * 0.42f, item.size * 0.20f).coerceAtLeast(3f)

    val segMinX = min(a.first, b.first) - padX
    val segMaxX = max(a.first, b.first) + padX
    val segMinY = min(a.second, b.second) - padY
    val segMaxY = max(a.second, b.second) + padY
    if (segMaxX < rawBox.left || segMinX > rawBox.right || segMaxY < rawBox.top || segMinY > rawBox.bottom) {
        return false to item
    }

    applyTextStyle(eraserTextPaint, item.size, item.style)
    val lh = item.size * 1.3f
    val lines = item.text.split('\n')
    val outLines = ArrayList<String>(lines.size)
    var changed = false

    lines.forEachIndexed { lineIdx, line ->
        val lineTop = item.y + lineIdx * lh
        val lineBottom = lineTop + lh
        if (segMaxY < lineTop || segMinY > lineBottom || line.isBlank()) {
            outLines += line
            return@forEachIndexed
        }
        val lineWidth = eraserTextPaint.measureText(line)
        val lineX = when (item.style.align) {
            TextAlign.LEFT -> rawBox.left
            TextAlign.CENTER -> rawBox.left + (rawBox.width() - lineWidth) / 2f
            TextAlign.RIGHT -> rawBox.right - lineWidth
        }
        val tokens = WORD_TOKEN_REGEX.findAll(line).toList()
        if (tokens.isEmpty()) {
            outLines += line
            return@forEachIndexed
        }
        val keptWords = ArrayList<String>(tokens.size)
        var lineChanged = false
        var firstKeptIndex = -1
        for ((tokIdx, m) in tokens.withIndex()) {
            val wLeft = lineX + eraserTextPaint.measureText(line, 0, m.range.first)
            val wRight = lineX + eraserTextPaint.measureText(line, 0, m.range.last + 1)
            val wordRect = RectF(wLeft - padX, lineTop - padY, wRight + padX, lineBottom + padY)
            if (segmentIntersectsRect(a.first, a.second, b.first, b.second, wordRect)) {
                lineChanged = true
                changed = true
            } else {
                if (firstKeptIndex < 0) firstKeptIndex = tokIdx
                keptWords += m.value
            }
        }
        if (!lineChanged) {
            outLines += line
        } else if (keptWords.isNotEmpty()) {
            val leadingIndent = if (firstKeptIndex == 0) line.takeWhile { it == ' ' || it == '\t' } else ""
            outLines += leadingIndent + keptWords.joinToString(" ")
        }
        // If every word on this line was erased, drop the emptied line cleanly.
    }

    if (!changed) return false to item
    val joined = outLines.joinToString("\n").trimEnd('\n')
    return if (joined.isBlank()) {
        true to null
    } else {
        true to item.copy(text = joined)
    }
}

/**
 * Scribble-to-erase support for individual words inside [TextItem]s:
 * returns updated page items and the world-space rects + colors of erased words, or null if no word matched.
 */
internal fun NoteCanvasView.scribbleEraseTextWords(page: Int, pts: List<Pt>): Pair<List<PageItem>, List<Pair<RectF, Int>>>? {
    if (pts.size < 6) return null
    val sheet = pages.getOrNull(page) ?: return null
    val padding = (6f * density / scale).coerceIn(3f, 14f)
    val envelope = RectF(
        pts.minOf { it.first },
        pts.minOf { it.second },
        pts.maxOf { it.first },
        pts.maxOf { it.second },
    ).apply { inset(-padding, -padding) }

    val step = max(1, pts.size / 48)
    val erasedVisuals = ArrayList<Pair<RectF, Int>>()
    var anyChanged = false
    val nextItems = ArrayList<PageItem>(sheet.items.size)

    for (item in sheet.items) {
        if (item !is TextItem || !eraserAllows(item) || !RectF.intersects(envelope, item.bounds)) {
            nextItems += item
            continue
        }
        val rawBox = item.raw
        applyTextStyle(eraserTextPaint, item.size, item.style)
        val lh = item.size * 1.3f
        val lines = item.text.split('\n')
        val outLines = ArrayList<String>(lines.size)
        var itemChanged = false

        lines.forEachIndexed { lineIdx, line ->
            val lineTop = item.y + lineIdx * lh
            val lineBottom = lineTop + lh
            val tokens = WORD_TOKEN_REGEX.findAll(line).toList()
            if (tokens.isEmpty()) {
                outLines += line
                return@forEachIndexed
            }
            val lineWidth = eraserTextPaint.measureText(line)
            val lineX = when (item.style.align) {
                TextAlign.LEFT -> rawBox.left
                TextAlign.CENTER -> rawBox.left + (rawBox.width() - lineWidth) / 2f
                TextAlign.RIGHT -> rawBox.right - lineWidth
            }
            val keptWords = ArrayList<String>(tokens.size)
            var lineChanged = false
            var firstKeptIndex = -1
            for ((tokIdx, m) in tokens.withIndex()) {
                val wLeft = lineX + eraserTextPaint.measureText(line, 0, m.range.first)
                val wRight = lineX + eraserTextPaint.measureText(line, 0, m.range.last + 1)
                val wordRect = RectF(wLeft, lineTop, wRight, lineBottom)
                val hitRect = RectF(wordRect).apply { inset(-padding * 0.45f, -padding * 0.4f) }
                var segHits = 0
                var i = step
                while (i < pts.size) {
                    val a = rotateAround(pts[i - step], rawBox.centerX(), rawBox.centerY(), -item.rot)
                    val b = rotateAround(pts[i], rawBox.centerX(), rawBox.centerY(), -item.rot)
                    if (segmentIntersectsRect(a.first, a.second, b.first, b.second, hitRect)) {
                        segHits++
                        if (segHits >= 2) break
                    }
                    i += step
                }
                if (segHits >= 2) {
                    lineChanged = true
                    itemChanged = true
                    anyChanged = true
                    erasedVisuals += wordRect to item.color
                } else {
                    if (firstKeptIndex < 0) firstKeptIndex = tokIdx
                    keptWords += m.value
                }
            }
            if (!lineChanged) {
                outLines += line
            } else if (keptWords.isNotEmpty()) {
                val leadingIndent = if (firstKeptIndex == 0) line.takeWhile { it == ' ' || it == '\t' } else ""
                outLines += leadingIndent + keptWords.joinToString(" ")
            }
        }

        if (!itemChanged) {
            nextItems += item
        } else {
            val joined = outLines.joinToString("\n").trimEnd('\n')
            if (joined.isNotBlank()) {
                nextItems += item.copy(text = joined)
            }
        }
    }

    return if (anyChanged) nextItems to erasedVisuals else null
}

/**
 * Finds all erasable strokes actually scrubbed over by [pts].
 * Uses bounding-box intersection + generous sweep padding + either direct segment hits or
 * substantial stroke-point overlap inside the scribble envelope so scrubbing across any word
 * or cursive line erases cleanly without requiring the scribble to swallow every tail pixel.
 */
internal fun NoteCanvasView.scribbleHitIds(page: Int, pts: List<Pt>): Set<Long> {
    if (pts.size < 6) return emptySet()
    val sheet = pages.getOrNull(page) ?: return emptySet()
    val padding = (7f * density / scale).coerceIn(4f, 18f)
    val envelope = RectF(
        pts.minOf { it.first },
        pts.minOf { it.second },
        pts.maxOf { it.first },
        pts.maxOf { it.second },
    ).apply { inset(-padding, -padding) }

    val sweep = MutableParallelogram()
    val step = max(1, pts.size / 64)
    return sheet.items.asSequence().filterIsInstance<InkItem>()
        .filter { eraserAllows(it) && RectF.intersects(envelope, it.bounds) }
        .filter { item ->
            val b = item.bounds
            val insideX = (min(envelope.right, b.right) - max(envelope.left, b.left)).coerceAtLeast(0f)
            val insideY = (min(envelope.bottom, b.bottom) - max(envelope.top, b.top)).coerceAtLeast(0f)
            val bArea = (b.width() * b.height()).coerceAtLeast(1f)
            val overlapRatio = (insideX * insideY) / bArea

            var hits = 0
            var touching = false
            var i = step
            while (i < pts.size) {
                val a = pts[i - step]
                val c = pts[i]
                val segment = MutableSegment(
                    MutableVec(a.first - item.dx, a.second - item.dy),
                    MutableVec(c.first - item.dx, c.second - item.dy),
                )
                sweep.populateFromSegmentAndPadding(segment, padding)
                val hit = item.stroke.shape.intersects(sweep, AffineTransform.IDENTITY)
                if (hit && !touching) hits++
                if (hits >= 2) return@filter true
                if (hits >= 1 && overlapRatio >= 0.48f) return@filter true
                touching = hit
                i += step
            }
            hits >= 2 || (hits >= 1 && overlapRatio >= 0.48f)
        }.map { it.id }.toSet()
}

/** Finds the topmost selectable item under a tap in Lasso mode. */
internal fun NoteCanvasView.topSelectableItemAt(page: Int, wx: Float, wy: Float): Long? {
    val p = pages.getOrNull(page) ?: return null
    val r = (18f / scale).coerceIn(8f, 36f)
    val para = MutableParallelogram()
    for (item in p.items.asReversed()) {
        if (!lassoAllows(item)) continue
        when (item) {
            is InkItem -> {
                val b = item.bounds
                if (!b.intersects(wx - r, wy - r, wx + r, wy + r)) continue
                val localSeg = MutableSegment(
                    MutableVec(wx - item.dx - 0.5f, wy - item.dy - 0.5f),
                    MutableVec(wx - item.dx + 0.5f, wy - item.dy + 0.5f),
                )
                para.populateFromSegmentAndPadding(localSeg, r)
                if (item.stroke.shape.intersects(para, AffineTransform.IDENTITY)) return item.id
            }
            else -> {
                val b = RectF(item.bounds).apply { inset(-r * 0.35f, -r * 0.35f) }
                if (b.contains(wx, wy)) return item.id
            }
        }
    }
    return null
}

internal fun NoteCanvasView.eraseAt(wx: Float, wy: Float) {
    val p = pages.getOrNull(activePage) ?: return
    val prev = lastErase ?: (wx to wy)
    lastErase = wx to wy
    val r = host.eraserRadius() / scale
    if (settings.eraserMode == com.pronotes.app.data.EraserMode.PIXEL) {
        pixelErase(p, prev, wx, wy, r)
        return
    }
    val next = ArrayList<PageItem>(p.items.size)
    var changed = false
    val para = MutableParallelogram()
    for (item in p.items) {
        if (!eraserAllows(item)) {
            next += item
            continue
        }
        when (item) {
            is InkItem -> {
                val b = item.bounds
                if (!b.intersects(min(prev.first, wx) - r, min(prev.second, wy) - r, max(prev.first, wx) + r, max(prev.second, wy) + r)) {
                    next += item
                    continue
                }
                val seg = MutableSegment(
                    MutableVec(prev.first - item.dx, prev.second - item.dy),
                    MutableVec(wx - item.dx, wy - item.dy),
                )
                para.populateFromSegmentAndPadding(seg, r)
                if (item.stroke.shape.intersects(para, AffineTransform.IDENTITY)) {
                    changed = true
                } else {
                    next += item
                }
            }
            is TextItem -> {
                val (wordErased, updated) = eraseWordsFromTextItem(item, prev, wx to wy, r)
                if (wordErased) {
                    changed = true
                    if (updated != null) next += updated
                } else {
                    next += item
                }
            }
            else -> {
                if (item.bounds.contains(wx, wy)) {
                    changed = true
                } else {
                    next += item
                }
            }
        }
    }
    if (changed) {
        updatePage(activePage, next)
    }
}

/** Pixel eraser: cuts strokes where the eraser passes, erases individual words from text items, and keeps the rest. */
internal fun NoteCanvasView.pixelErase(p: Page, prev: Pt, wx: Float, wy: Float, r: Float) {
    var items = p.items
    var changed = false

    // 1. Word-level erasing for TextItems over the full segment (prev -> (wx, wy))
    val afterText = ArrayList<PageItem>(items.size)
    for (item in items) {
        if (item is TextItem && eraserAllows(item)) {
            val (wordErased, updated) = eraseWordsFromTextItem(item, prev, wx to wy, r)
            if (wordErased) {
                changed = true
                if (updated != null) afterText += updated
            } else {
                afterText += item
            }
        } else {
            afterText += item
        }
    }
    items = afterText

    // 2. Sub-stepped pixel erasing for InkItems & other page items
    val steps = max(1, (hypot(wx - prev.first, wy - prev.second) / (r * 0.5f)).toInt())
    for (s in 1..steps) {
        val t = s / steps.toFloat()
        val cx = prev.first + (wx - prev.first) * t
        val cy = prev.second + (wy - prev.second) * t
        val next = ArrayList<PageItem>(items.size)
        for (item in items) {
            if (!eraserAllows(item) || item is TextItem) {
                next += item
                continue
            }
            if (item !is InkItem) {
                if (item.bounds.contains(cx, cy)) {
                    changed = true
                    continue
                }
                next += item
                continue
            }
            val b = item.bounds
            if (cx < b.left - r || cx > b.right + r || cy < b.top - r || cy > b.bottom + r) {
                next += item
                continue
            }
            val pieces = InkCodec.eraseFrom(item.stroke, cx - item.dx, cy - item.dy, r + item.stroke.brush.size / 2f)
            if (pieces == null) {
                next += item
            } else {
                changed = true
                pieces.forEach { next += InkItem(ItemIds.next(), it, item.dx, item.dy, familyHint = item.familyHint) }
            }
        }
        items = next
    }
    if (changed) updatePage(activePage, items)
}
