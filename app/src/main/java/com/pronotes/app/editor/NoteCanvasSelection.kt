package com.pronotes.app.editor

import android.graphics.Matrix
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

internal fun NoteCanvasView.select(page: Int, lasso: Lasso, outline: List<Pt>? = null) {
    val p = pages.getOrNull(page) ?: return
    selectItems(page, p.items.filter { lassoAllows(it) && lasso.selects(it) }.map { it.id }.toSet(), outline)
}

internal fun NoteCanvasView.selectionPageBounds(): RectF? {
    val p = pages.getOrNull(selectionPage) ?: return null
    val items = p.items.filter { it.id in selection }
    if (items.isEmpty()) return null
    val r = RectF(items.first().bounds)
    items.drop(1).forEach { r.union(it.bounds) }
    r.inset(-12f, -12f)
    liveMatrix?.let { if (grab != NoteCanvasView.Grab.ROTATE) it.mapRect(r) }
    return r
}

/** Selection box without live transform, for hit testing and rotate drawing. */
internal fun NoteCanvasView.selectionBaseBounds(): RectF? {
    val keep = liveMatrix
    liveMatrix = null
    val r = selectionPageBounds()
    liveMatrix = keep
    return r
}

internal fun NoteCanvasView.handleRadius() = 22f * density
internal fun NoteCanvasView.rotateHandleOffset() = 30f * density

/** Starts move / resize / stretch / rotate when the pen lands on the selection or one of its handles. */
internal fun NoteCanvasView.beginGrab(px: Float, py: Float): Boolean {
    val base = selectionBaseBounds() ?: return false
    val p = pages.getOrNull(selectionPage)
    if (p != null && selection.isNotEmpty() && p.items.filter { it.id in selection }.all { it is StickyItem && it.pinned }) {
        return false
    }
    val sb = RectF(base)
    pageMatrix(selectionPage, tmpMatrix).mapRect(sb)
    val (lx, ly) = toLocal(px, py, selectionPage)
    val corners = listOf(sb.left to sb.top, sb.right to sb.top, sb.right to sb.bottom, sb.left to sb.bottom)
    val baseCorners = listOf(base.left to base.top, base.right to base.top, base.right to base.bottom, base.left to base.bottom)
    val hit = corners.indexOfFirst { (cx, cy) -> hypot(px - cx, py - cy) < handleRadius() }
    val midHit = listOf(
        (sb.left to sb.centerY()) to NoteCanvasView.Grab.STRETCH_L,
        (sb.right to sb.centerY()) to NoteCanvasView.Grab.STRETCH_R,
        (sb.centerX() to sb.top) to NoteCanvasView.Grab.STRETCH_T,
        (sb.centerX() to sb.bottom) to NoteCanvasView.Grab.STRETCH_B,
    ).firstOrNull { (pt, _) -> hypot(px - pt.first, py - pt.second) < handleRadius() * 0.85f }?.second
    grab = when {
        hit >= 0 -> {
            val opp = baseCorners[(hit + 2) % 4]
            anchorX = opp.first
            anchorY = opp.second
            NoteCanvasView.Grab.SCALE
        }
        midHit != null -> midHit
        hypot(px - sb.centerX(), py - (sb.top - rotateHandleOffset())) < handleRadius() -> {
            anchorX = base.centerX()
            anchorY = base.centerY()
            NoteCanvasView.Grab.ROTATE
        }
        sb.contains(px, py) -> NoteCanvasView.Grab.MOVE
        else -> return false
    }
    activePage = selectionPage
    mode = NoteCanvasView.Mode.MOVE
    grabStartX = lx
    grabStartY = ly
    grabBase.set(base)
    liveBox.set(base)
    liveScale = 1f
    liveScaleX = 1f
    liveScaleY = 1f
    liveDeg = 0f
    liveMatrix = Matrix()
    return true
}

internal fun NoteCanvasView.updateGrab(x: Float, y: Float) {
    val m = Matrix()
    when (grab) {
        NoteCanvasView.Grab.MOVE -> {
            m.setTranslate(x - grabStartX, y - grabStartY)
            if (width > 0 && height > 0) {
                val pt = floatArrayOf(x, y)
                pageMatrix(selectionPage, tmpMatrix).mapPoints(pt)
                val sx = pt[0]
                val sy = pt[1]
                val edge = 64f * density
                val step = 14f * density
                var scrolled = false
                if (sy < edge) { transY += step; scrolled = true }
                else if (sy > height - edge) { transY -= step; scrolled = true }
                if (sx < edge) { transX += step; scrolled = true }
                else if (sx > width - edge) { transX -= step; scrolled = true }
                if (scrolled) {
                    updateMatrix()
                }
            }
        }
        NoteCanvasView.Grab.SCALE -> {
            val d0 = hypot(grabStartX - anchorX, grabStartY - anchorY).coerceAtLeast(1f)
            liveScale = (hypot(x - anchorX, y - anchorY) / d0).coerceIn(0.1f, 12f)
            m.setScale(liveScale, liveScale, anchorX, anchorY)
        }
        NoteCanvasView.Grab.STRETCH_L,
        NoteCanvasView.Grab.STRETCH_R,
        NoteCanvasView.Grab.STRETCH_T,
        NoteCanvasView.Grab.STRETCH_B -> {
            val dx = x - grabStartX
            val dy = y - grabStartY
            liveBox.set(grabBase)
            when (grab) {
                NoteCanvasView.Grab.STRETCH_L -> liveBox.left = (grabBase.left + dx).coerceAtMost(grabBase.right - 36f)
                NoteCanvasView.Grab.STRETCH_R -> liveBox.right = (grabBase.right + dx).coerceAtLeast(grabBase.left + 36f)
                NoteCanvasView.Grab.STRETCH_T -> liveBox.top = (grabBase.top + dy).coerceAtMost(grabBase.bottom - 36f)
                NoteCanvasView.Grab.STRETCH_B -> liveBox.bottom = (grabBase.bottom + dy).coerceAtLeast(grabBase.top + 36f)
                else -> Unit
            }
            liveScaleX = (liveBox.width() / grabBase.width().coerceAtLeast(1f)).coerceIn(0.15f, 8f)
            liveScaleY = (liveBox.height() / grabBase.height().coerceAtLeast(1f)).coerceIn(0.15f, 8f)
            m.setRectToRect(grabBase, liveBox, Matrix.ScaleToFit.FILL)
        }
        NoteCanvasView.Grab.ROTATE -> {
            val a0 = Math.toDegrees(atan2((grabStartY - anchorY).toDouble(), (grabStartX - anchorX).toDouble()))
            val a1 = Math.toDegrees(atan2((y - anchorY).toDouble(), (x - anchorX).toDouble()))
            var deg = (a1 - a0).toFloat()
            val snapped = (Math.round(deg / 15f) * 15).toFloat()
            if (abs(deg - snapped) < 4f) deg = snapped // snap to 15° steps
            liveDeg = deg
            m.setRotate(deg, anchorX, anchorY)
        }
    }
    liveMatrix = m
}

internal fun NoteCanvasView.reportSelection() {
    val wb = if (selection.isEmpty()) null else selectionPageBounds()
    if (wb == null) {
        host.onSelectionChanged(selectionPage, emptySet(), null)
        return
    }
    val sb = RectF(wb)
    pageMatrix(selectionPage, tmpMatrix).mapRect(sb)
    host.onSelectionChanged(selectionPage, selection, sb)
}

internal fun NoteCanvasView.commitMove() {
    val p = pages.getOrNull(selectionPage) ?: return
    val m = liveMatrix
    liveMatrix = null
    if (m != null && !m.isIdentity) {
        if (grab == NoteCanvasView.Grab.MOVE) {
            val v = FloatArray(9)
            m.getValues(v)
            val dx = v[Matrix.MTRANS_X]
            val dy = v[Matrix.MTRANS_Y]
            val movedBounds = selectionBaseBounds()?.also { m.mapRect(it) }
            val targetPage = if (movedBounds != null) {
                val docCx = ox(selectionPage) + movedBounds.centerX()
                val docCy = oy(selectionPage) + movedBounds.centerY()
                pageAt(docCx, docCy)
            } else selectionPage

            if (targetPage != selectionPage && targetPage in pages.indices) {
                val tp = pages[targetPage]
                val totalDx = dx + (ox(selectionPage) - ox(targetPage))
                val totalDy = dy + (oy(selectionPage) - oy(targetPage))
                val movedToTarget = p.items.filter { it.id in selection }.map { item ->
                    val movedItem = item.translated(totalDx, totalDy)
                    if (movedItem is MindNodeItem && !tp.isWhiteboard) {
                        val maxX = (tp.width - movedItem.w - 56f).coerceAtLeast(56f)
                        val maxY = (tp.height - movedItem.h - 56f).coerceAtLeast(56f)
                        movedItem.copy(x = movedItem.x.coerceIn(56f, maxX), y = movedItem.y.coerceIn(56f, maxY))
                    } else {
                        movedItem
                    }
                }
                val origPage = selectionPage
                host.beginEdit(origPage)
                updatePage(origPage, p.items.filterNot { it.id in selection })
                host.beginEdit(targetPage)
                updatePage(targetPage, tp.items + movedToTarget)
                selectionOutline = selectionOutline?.map { (x, y) -> (x + totalDx) to (y + totalDy) }
                selectionPage = targetPage
                activePage = targetPage
            } else {
                host.beginEdit(selectionPage)
                val items = p.items.map {
                    if (it.id in selection) {
                        val movedItem = it.translated(dx, dy)
                        if (movedItem is MindNodeItem && !p.isWhiteboard) {
                            val maxX = (p.width - movedItem.w - 56f).coerceAtLeast(56f)
                            val maxY = (p.height - movedItem.h - 56f).coerceAtLeast(56f)
                            movedItem.copy(x = movedItem.x.coerceIn(56f, maxX), y = movedItem.y.coerceIn(56f, maxY))
                        } else {
                            movedItem
                        }
                    } else {
                        it
                    }
                }
                updatePage(selectionPage, items)
                selectionOutline = selectionOutline?.map { (x, y) ->
                    val a = floatArrayOf(x, y)
                    m.mapPoints(a)
                    a[0] to a[1]
                }
            }
        } else {
            host.beginEdit(selectionPage)
            val items = when (grab) {
                NoteCanvasView.Grab.SCALE -> p.items.map { if (it.id in selection) it.transformed(m, liveScale, 0f) else it }
                NoteCanvasView.Grab.STRETCH_L,
                NoteCanvasView.Grab.STRETCH_R,
                NoteCanvasView.Grab.STRETCH_T,
                NoteCanvasView.Grab.STRETCH_B -> p.items.map {
                    if (it.id in selection) it.stretched(grabBase, liveBox) else it
                }
                NoteCanvasView.Grab.ROTATE -> p.items.map { if (it.id in selection) it.transformed(m, 1f, liveDeg) else it }
                NoteCanvasView.Grab.MOVE -> p.items
            }
            updatePage(selectionPage, items)
            // The lasso loop follows the content.
            selectionOutline = selectionOutline?.map { (x, y) ->
                val a = floatArrayOf(x, y)
                m.mapPoints(a)
                a[0] to a[1]
            }
        }
    }
    liveScale = 1f
    liveScaleX = 1f
    liveScaleY = 1f
    liveDeg = 0f
    reportSelection()
}

/** Returns the line index in [item.text] when (wx, wy) hits a checklist checkbox gutter, else null. */
internal fun NoteCanvasView.findStickyChecklistLine(item: StickyItem, wx: Float, wy: Float): Int? {
    if (item.collapsed || item.text.isBlank()) return null
    val r = item.raw
    val barH = minOf(r.height(), if (item.title.isNotBlank()) 38f else 34f)
    if (wx > r.left + 64f || wy < r.top + barH) return null
    val fSize = item.fontSize.coerceIn(14f, 64f) * (r.width() / 280f).coerceIn(0.45f, 3.5f)
    val lineStep = (fSize * 1.45f).coerceAtLeast(24f)
    var cy = r.top + barH + 10f
    item.text.split('\n').forEachIndexed { idx, line ->
        val tr = line.trimStart()
        val isTask = tr.startsWith("- [ ] ") || tr.startsWith("- [x] ", ignoreCase = true) || tr.startsWith("☐ ") || tr.startsWith("☑ ")
        if (wy >= cy - 4f && wy <= cy + lineStep + 6f) {
            return if (isTask) idx else null
        }
        cy += lineStep
    }
    return null
}
