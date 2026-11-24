package com.pronotes.app.editor

import android.animation.ValueAnimator
import android.graphics.RectF
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.animation.DecelerateInterpolator
import com.pronotes.app.data.ScrollDirection
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

internal fun NoteCanvasView.handleFingers(e: MotionEvent, action: Int) {
    if (velocity == null) velocity = VelocityTracker.obtain()
    velocity?.addMovement(e)
    when (action) {
        MotionEvent.ACTION_DOWN -> {
            scroller.forceFinished(true)
            zoomAnim?.cancel()
            mode = NoteCanvasView.Mode.PAN
            panLastX = e.x
            panLastY = e.y
            fingerStartX = e.x
            fingerStartY = e.y
            maxFingers = 1
            fingerDownTime = SystemClock.uptimeMillis()
            fingerMoved = false
            pinched = false
            textSelecting = false
            clearStickyDrag()
            // Hold on a collapsed sticky note icon to move it; quick tap still expands it.
            val hitCollapsedSticky = if (!readOnly && pages.isNotEmpty()) {
                val (ddx, ddy) = toDoc(e.x, e.y)
                val pi = pageAt(ddx, ddy)
                val (lx, ly) = toLocal(e.x, e.y, pi)
                val s = pages.getOrNull(pi)?.items?.filterIsInstance<StickyItem>()
                    ?.lastOrNull { it.collapsed && it.bounds.contains(lx, ly) }
                if (s != null) Triple(pi, s, lx to ly) else null
            } else null
            removeCallbacks(textPressRunnable)
            if (hitCollapsedSticky != null) {
                val (pi, s, pt) = hitCollapsedSticky
                dragStickyId = s.id
                dragStickyPage = pi
                dragStickyOrigX = s.x
                dragStickyOrigY = s.y
                dragStickyDownWx = pt.first
                dragStickyDownWy = pt.second
                dragStickyActive = false
                dragStickyFromFinger = true
                postDelayed(stickyHoldRunnable, 280L)
            } else {
                // Long-press on PDF text selects it.
                postDelayed(textPressRunnable, 450)
            }
        }
        MotionEvent.ACTION_POINTER_DOWN -> {
            removeCallbacks(textPressRunnable)
            clearStickyDrag()
            if (textSelecting) return
            mode = NoteCanvasView.Mode.PAN
            maxFingers = max(maxFingers, e.pointerCount)
            if (fingerDownTime == 0L) fingerDownTime = SystemClock.uptimeMillis()
            if (e.pointerCount >= 2) {
                if (!pinched) gestureFit = fitScale(pageAtGestureStart.coerceIn(0, max(0, pages.size - 1)))
                pinched = true
                scroller.forceFinished(true)
                pinchStartDist = spacing(e)
                pinchStartScale = scale
            }
            val (cx, cy) = focus(e)
            panLastX = cx
            panLastY = cy
        }
        MotionEvent.ACTION_MOVE -> {
            if (dragStickyActive && dragStickyFromFinger) {
                moveDraggedSticky(e.x, e.y)
                return
            }
            if (textSelecting) {
                val (lx, ly) = toLocal(e.x, e.y, textPage)
                host.onTextDrag(lx, ly)
                layer.invalidate()
                return
            }
            val (cx, cy) = focus(e)
            if (hypot(cx - fingerStartX, cy - fingerStartY) > effectiveTouchSlop * 2f) {
                fingerMoved = true
                removeCallbacks(textPressRunnable)
                if (dragStickyFromFinger && !dragStickyActive) clearStickyDrag()
            }
            if (e.pointerCount >= 2 && pinchStartDist > 0f) {
                val minScale = if (isWhiteboard) 0.16f else gestureFit * 0.9f
                val maxScale = if (isWhiteboard) 8f else 6f
                val newScale = (pinchStartScale * spacing(e) / pinchStartDist).coerceIn(minScale, maxScale)
                if (abs(newScale - scale) > 0.001f) {
                    fingerMoved = true
                    host.onZoom((newScale / gestureFit * 100f).toInt())
                    val (fx, fy) = toDoc(cx, cy)
                    scale = newScale
                    transX = cx - fx * scale
                    transY = cy - fy * scale
                }
            }
            var dx = cx - panLastX
            var dy = cy - panLastY
            if (pull > 0f) {
                dx *= 0.45f
                dy *= 0.45f
            }
            val horizontal = settings.scroll == ScrollDirection.HORIZONTAL
            val paging = !isWhiteboard && !pinched && horizontal && e.pointerCount == 1 && scale <= fitScale() * 1.05f
            if (paging) dy = 0f
            transX += dx
            if (horizontal && !paging && !isWhiteboard && e.pointerCount == 1 && pages.isNotEmpty()) {
                val (lo, hi) = pageXRange(pageAtGestureStart.coerceIn(0, pages.size - 1))
                transX = transX.coerceIn(lo, hi)
            }
            if (paging && pages.isNotEmpty()) {
                val last = pageAtGestureStart >= pages.size - 1
                var lo = pageCenterX((pageAtGestureStart + 1).coerceAtMost(pages.size - 1))
                val hi = pageCenterX((pageAtGestureStart - 1).coerceAtLeast(0))
                if (last && settings.pullToAddPage) lo -= 220f * density
                transX = transX.coerceIn(min(lo, hi), max(lo, hi))
            }
            transY += dy
            panLastX = cx
            panLastY = cy
            clamp(allowPull = e.pointerCount == 1)
            updateMatrix()
        }
        MotionEvent.ACTION_POINTER_UP -> {
            val remaining = (0 until e.pointerCount).filter { it != e.actionIndex }
            if (remaining.isNotEmpty()) {
                panLastX = remaining.map { e.getX(it) }.average().toFloat()
                panLastY = remaining.map { e.getY(it) }.average().toFloat()
            }
            pinchStartDist = 0f
            if (e.pointerCount - 1 >= 2) {
                pinchStartDist = spacing(e, skip = e.actionIndex)
                pinchStartScale = scale
            }
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            removeCallbacks(textPressRunnable)
            if (dragStickyActive && dragStickyFromFinger) {
                val sp = pages.getOrNull(dragStickyPage)
                if (sp != null) updatePage(dragStickyPage, sp.items)
                clearStickyDrag()
                mode = NoteCanvasView.Mode.NONE
                maxFingers = 0
                fingerDownTime = 0L
                velocity?.recycle()
                velocity = null
                return
            }
            clearStickyDrag()
            if (textSelecting) {
                textSelecting = false
                val rects = host.textSelection(textPage)
                val sb = if (rects.isEmpty()) null else RectF(rects.first()).also { r ->
                    rects.drop(1).forEach { r.union(it) }
                    pageMatrix(textPage, tmpMatrix).mapRect(r)
                }
                host.onTextEnd(sb)
                mode = NoteCanvasView.Mode.NONE
                maxFingers = 0
                fingerDownTime = 0L
                velocity?.recycle()
                velocity = null
                return
            }
            val quick = SystemClock.uptimeMillis() - fingerDownTime < 300
            if (action == MotionEvent.ACTION_UP && quick && !fingerMoved && maxFingers == 1) host.onTextClear()
            if (action == MotionEvent.ACTION_UP && quick && !fingerMoved && settings.fingerTapUndo && !readOnly) {
                when (maxFingers) {
                    2 -> {
                        host.undo()
                        syncFeel()
                        feel.cue(PaperFeel.Cue.UNDO)
                    }
                    3 -> {
                        host.redo()
                        syncFeel()
                        feel.cue(PaperFeel.Cue.UNDO)
                    }
                }
            }
            val link = if (action == MotionEvent.ACTION_UP && maxFingers == 1 && quick && !fingerMoved) linkBoxAt(e.x, e.y) else null
            val webCard = if (link == null && action == MotionEvent.ACTION_UP && maxFingers == 1 && quick && !fingerMoved) {
                val (dx, dy) = toDoc(e.x, e.y)
                val pi = pageAt(dx, dy)
                val (lx, ly) = toLocal(e.x, e.y, pi)
                val pItems = pages.getOrNull(pi)?.items.orEmpty()
                val taskSticky = if (!readOnly) pItems.lastOrNull { it is StickyItem && it.bounds.contains(lx, ly) } as StickyItem? else null
                val hitCollapse = taskSticky != null && (taskSticky.collapsed || (lx >= taskSticky.raw.right - 46f && ly <= taskSticky.raw.top + 44f))
                val hitEdit = taskSticky != null && !taskSticky.collapsed && !hitCollapse &&
                    (lx >= taskSticky.raw.right - 84f && ly <= taskSticky.raw.top + 44f)
                val taskIdx = if (!hitCollapse && !hitEdit) taskSticky?.let { findStickyChecklistLine(it, lx, ly) } else null
                if (taskSticky != null && hitCollapse) {
                    buzz(HapticFeedbackConstants.CLOCK_TICK)
                    host.onStickyToggleCollapse(pi, taskSticky)
                    lastTapTime = 0L
                    null
                } else if (taskSticky != null && hitEdit) {
                    buzz(HapticFeedbackConstants.CLOCK_TICK)
                    host.onStickyTap(pi, lx, ly, taskSticky)
                    lastTapTime = 0L
                    null
                } else if (taskSticky != null && taskIdx != null) {
                    buzz(HapticFeedbackConstants.CLOCK_TICK)
                    host.onStickyChecklistToggle(pi, taskSticky, taskIdx)
                    lastTapTime = 0L
                    null
                } else if (taskSticky != null && !taskSticky.collapsed && !readOnly) {
                    // Notion-style direct tap to edit sticky note inline
                    buzz(HapticFeedbackConstants.CLOCK_TICK)
                    host.onStickyTap(pi, lx, ly, taskSticky)
                    lastTapTime = 0L
                    null
                } else {
                    pItems.lastOrNull { it is WebLinkItem && it.bounds.contains(lx, ly) } as WebLinkItem?
                }
            } else {
                null
            }
            if (link != null) {
                jumpTo(link)
                lastTapTime = 0L
            } else if (webCard != null) {
                buzz()
                host.onOpenUrl(webCard.url)
                lastTapTime = 0L
            } else if (action == MotionEvent.ACTION_UP && maxFingers == 1 && quick && !fingerMoved) {
                val now = SystemClock.uptimeMillis()
                if (now - lastTapTime < 320 && hypot(e.x - lastTapX, e.y - lastTapY) < effectiveTouchSlop * 3f) {
                    val (dx, dy) = toDoc(e.x, e.y)
                    val pi = pageAt(dx, dy)
                    val (lx, ly) = toLocal(e.x, e.y, pi)
                    val writeBox = pages.getOrNull(pi)?.items?.filterIsInstance<WriteBoxItem>()?.lastOrNull { it.bounds.contains(lx, ly) }
                    if (writeBox != null && !readOnly) {
                        buzz(HapticFeedbackConstants.CONTEXT_CLICK)
                        host.onWriteBoxDoubleTap(pi, writeBox)
                        lastTapTime = 0L
                    } else if (settings.doubleTapZoom) {
                        toggleZoom(e.x, e.y)
                        lastTapTime = 0L
                    }
                } else {
                    lastTapTime = now
                    lastTapX = e.x
                    lastTapY = e.y
                    if (selection.isNotEmpty()) clearSelection()
                    clearCircleCandidate()
                    if (host.isKeyboardMode()) {
                        val (dx, dy) = toDoc(e.x, e.y)
                        val pi = pageAt(dx, dy)
                        val (lx, ly) = toLocal(e.x, e.y, pi)
                        host.onPageTap(pi, lx, ly)
                    }
                }
            }
            host.onZoom(null)
            val pulled = pull >= pullTrigger
            if (pulled) {
                buzz()
                syncFeel()
                feel.cue(PaperFeel.Cue.ADD)
                host.onPullAddPage()
            }
            pull = 0f
            releaseFling(pulled)
            mode = NoteCanvasView.Mode.NONE
            maxFingers = 0
            fingerDownTime = 0L
            pinchStartDist = 0f
            velocity?.recycle()
            velocity = null
        }
    }
}

/** transX range that keeps page [i] filling the screen sideways (or centred when narrower). */
internal fun NoteCanvasView.pageXRange(i: Int): Pair<Float, Float> {
    val p = pages[i]
    val left = -ox(i) * scale
    val w = p.width * scale
    if (w <= width) {
        val c = (width - w) / 2f + left
        return c to c
    }
    return (width - w + left) to left
}

internal fun NoteCanvasView.releaseFling(pulled: Boolean) {
    if (pinched) {
        pinched = false
        settleAfterPinch()
        return
    }
    val vt = velocity ?: return
    vt.computeCurrentVelocity(1000)
    val vx = vt.xVelocity
    val vy = vt.yVelocity
    val horizontal = settings.scroll == ScrollDirection.HORIZONTAL
    if (!fingerMoved || pulled) return
    if (isWhiteboard) {
        scroller.fling(
            transX.toInt(), transY.toInt(), vx.toInt(), vy.toInt(),
            -1_000_000, 1_000_000, -1_000_000, 1_000_000,
        )
        postInvalidateOnAnimation()
        return
    }
    val atFit = scale <= fitScale() * 1.05f
    if (horizontal && !atFit && pages.isNotEmpty()) {
        val (lo, hi) = pageXRange(pageAtGestureStart.coerceIn(0, pages.size - 1))
        val ch = docH * scale
        scroller.fling(
            transX.toInt(), transY.toInt(), vx.toInt(), vy.toInt(), lo.toInt(), hi.toInt(),
            min(0f, height - ch - 24f * density).toInt(), (24f * density).toInt(),
        )
        postInvalidateOnAnimation()
        return
    }
    if (horizontal && atFit && !pulled) {
        val start = pageAtGestureStart.coerceIn(0, max(0, pages.size - 1))
        var target = currentIndex.coerceIn((start - 1).coerceAtLeast(0), (start + 1).coerceAtMost(pages.size - 1))
        if (vx < -400) target = (start + 1).coerceAtMost(pages.size - 1)
        else if (vx > 400) target = (start - 1).coerceAtLeast(0)
        scale = fitScale(target)
        scrollToPage(target, animate = true)
        return
    }
    val cw = docW * scale
    val ch = docH * scale
    scroller.fling(
        transX.toInt(), transY.toInt(), vx.toInt(), vy.toInt(),
        min(0f, width - cw - 24f * density).toInt(), (24f * density).toInt(),
        min(0f, height - ch - 24f * density).toInt(), (24f * density).toInt(),
    )
    clamp()
    updateMatrix()
    postInvalidateOnAnimation()
}

internal fun NoteCanvasView.settleAfterPinch() {
    if (pages.isEmpty() || width == 0) return
    if (isWhiteboard) {
        updateMatrix()
        return
    }
    val horizontal = settings.scroll == ScrollDirection.HORIZONTAL
    val page = pageAtGestureStart.coerceIn(0, pages.size - 1)
    val fromS = scale
    val fromX = transX
    val fromY = transY
    when {
        horizontal && scale <= gestureFit * 1.3f -> {
            scale = fitScale(page)
            centerOn(page)
            lastTarget = page
        }
        scale < gestureFit -> {
            val (fx, fy) = toDoc(width / 2f, height / 2f)
            scale = gestureFit
            transX = width / 2f - fx * scale
            transY = height / 2f - fy * scale
            clamp()
        }
        else -> {
            clamp()
            updateMatrix()
            return
        }
    }
    val toS = scale
    val toX = transX
    val toY = transY
    scale = fromS
    transX = fromX
    transY = fromY
    zoomAnim?.cancel()
    zoomAnim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 240
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            val t = it.animatedValue as Float
            scale = fromS + (toS - fromS) * t
            transX = fromX + (toX - fromX) * t
            transY = fromY + (toY - fromY) * t
            updateMatrix()
        }
        start()
    }
}

internal fun NoteCanvasView.toggleZoom(x: Float, y: Float) {
    if (pages.isEmpty()) return
    zoomAnim?.cancel()
    scroller.forceFinished(true)
    val page = currentIndex
    val fit = fitScale(page)
    val zoomIn = scale <= fit * 1.3f
    val fromS = scale
    val fromX = transX
    val fromY = transY
    val (fx, fy) = toDoc(x, y)
    if (zoomIn) {
        scale = fit * 2f
        transX = x - fx * scale
        transY = y - fy * scale
        clamp()
    } else {
        scale = fit
        if (isWhiteboard) {
            val p = pages[page]
            transX = (width - p.width * scale) / 2f - ox(page) * scale
            transY = (height - p.height * scale) / 2f - oy(page) * scale
        } else if (settings.scroll == ScrollDirection.VERTICAL) {
            transY = y - fy * scale
            transX = (width - docW * scale) / 2f
            clamp()
        } else {
            centerOn(page)
        }
        lastTarget = page
    }
    val toS = scale
    val toX = transX
    val toY = transY
    scale = fromS
    transX = fromX
    transY = fromY
    zoomAnim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 260
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            val t = it.animatedValue as Float
            scale = fromS + (toS - fromS) * t
            transX = fromX + (toX - fromX) * t
            transY = fromY + (toY - fromY) * t
            updateMatrix()
        }
        start()
    }
}

internal fun NoteCanvasView.focus(e: MotionEvent): Pair<Float, Float> {
    var sx = 0f
    var sy = 0f
    for (i in 0 until e.pointerCount) {
        sx += e.getX(i)
        sy += e.getY(i)
    }
    return sx / e.pointerCount to sy / e.pointerCount
}

internal fun NoteCanvasView.spacing(e: MotionEvent, skip: Int = -1): Float {
    val idx = (0 until e.pointerCount).filter { it != skip }
    if (idx.size < 2) return 0f
    return hypot(e.getX(idx[0]) - e.getX(idx[1]), e.getY(idx[0]) - e.getY(idx[1]))
}

internal fun NoteCanvasView.trackPenButton(e: MotionEvent) {
    val held = e.buttonState and (MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_SECONDARY) != 0 ||
        e.actionMasked == MotionEvent.ACTION_BUTTON_PRESS
    val now = SystemClock.uptimeMillis()
    if (held && !buttonDown) {
        buttonDown = true
        buttonDownAt = now
        return
    }
    val released = (!held && buttonDown) || e.actionMasked == MotionEvent.ACTION_BUTTON_RELEASE
    if (!released || !buttonDown) return
    buttonDown = false
    val pressMs = now - buttonDownAt
    if (pressMs < 40 || pressMs > 450) return
    if (now - lastPenTouchAt < 600) return
    val x = e.x
    val y = e.y
    val name = e.device?.name ?: "Stylus"
    pendingClick?.let { removeCallbacks(it) }
    if (now - lastClickAt < 450) {
        pendingClick = null
        lastClickAt = 0L
        host.onPenButton(true, x, y, name)
    } else {
        lastClickAt = now
        val wait = settings.penClickDelay.ms.coerceAtLeast(500L)
        pendingClick = Runnable {
            pendingClick = null
            host.onPenButton(false, x, y, name)
        }.also { postDelayed(it, wait) }
    }
}

internal fun NoteCanvasView.clearHover() {
    removeCallbacks(hoverTimeout)
    if (layer.hoverDot != null || layer.hoverAt != null) {
        layer.hoverDot = null
        layer.hoverAt = null
        layer.invalidate()
    }
}
