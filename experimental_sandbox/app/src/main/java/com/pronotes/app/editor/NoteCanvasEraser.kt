package com.pronotes.app.editor

import android.graphics.RectF
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.Intersection.intersects
import androidx.ink.geometry.MutableParallelogram
import androidx.ink.geometry.MutableSegment
import androidx.ink.geometry.MutableVec
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

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
    val hit = HashSet<Long>()
    val para = MutableParallelogram()
    for (item in p.items) {
        if (!eraserAllows(item)) continue
        when (item) {
            is InkItem -> {
                val b = item.bounds
                if (!b.intersects(min(prev.first, wx) - r, min(prev.second, wy) - r, max(prev.first, wx) + r, max(prev.second, wy) + r)) continue
                val seg = MutableSegment(
                    MutableVec(prev.first - item.dx, prev.second - item.dy),
                    MutableVec(wx - item.dx, wy - item.dy),
                )
                para.populateFromSegmentAndPadding(seg, r)
                if (item.stroke.shape.intersects(para, AffineTransform.IDENTITY)) hit += item.id
            }
            else -> if (item.bounds.contains(wx, wy)) hit += item.id
        }
    }
    if (hit.isNotEmpty()) {
        updatePage(activePage, p.items.filterNot { it.id in hit })
    }
}

/** Pixel eraser: cuts strokes where the eraser passes and keeps the rest. */
internal fun NoteCanvasView.pixelErase(p: Page, prev: Pt, wx: Float, wy: Float, r: Float) {
    val steps = max(1, (hypot(wx - prev.first, wy - prev.second) / (r * 0.5f)).toInt())
    var items = p.items
    var changed = false
    for (s in 1..steps) {
        val t = s / steps.toFloat()
        val cx = prev.first + (wx - prev.first) * t
        val cy = prev.second + (wy - prev.second) * t
        val next = ArrayList<PageItem>(items.size)
        for (item in items) {
            if (!eraserAllows(item)) {
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
                pieces.forEach { next += InkItem(ItemIds.next(), it, item.dx, item.dy) }
            }
        }
        items = next
    }
    if (changed) updatePage(activePage, items)
}
