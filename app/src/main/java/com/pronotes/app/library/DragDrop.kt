package com.pronotes.app.library

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/*
 * Drag and drop inside the library. Long-press a notebook or folder, drag it, and drop it on a
 * folder (move into it), on "Documents" or the up arrow (move out), or on "Trash".
 * Payloads are "note:<id>" or "folder:<id>".
 *
 * This is our own implementation (pointer events + hit testing), not the system drag service,
 * so it behaves the same with a finger or any pen on every Android version.
 */

@Stable
class DragController {
    private class Target(var bounds: Rect, var onHover: (Boolean) -> Unit, var onDrop: (String) -> Boolean)

    private val targets = LinkedHashMap<Any, Target>()
    private var hovered: Target? = null

    /** What is being dragged ("note:<id>" / "folder:<id>"), or null. */
    var payload by mutableStateOf<String?>(null)
        private set
    var label by mutableStateOf("")
        private set
    /** Pointer position in root coordinates. */
    var position by mutableStateOf(Offset.Zero)
        private set
    /** True while the pointer is over a target that is highlighted. */
    var overTarget by mutableStateOf(false)
        private set

    internal fun register(key: Any, bounds: Rect, onHover: (Boolean) -> Unit, onDrop: (String) -> Boolean) {
        val t = targets[key]
        if (t == null) targets[key] = Target(bounds, onHover, onDrop)
        else { t.bounds = bounds; t.onHover = onHover; t.onDrop = onDrop }
    }

    internal fun unregister(key: Any) {
        val t = targets.remove(key)
        if (t != null && t === hovered) hovered = null
    }

    fun start(payload: String, label: String, at: Offset) {
        this.payload = payload; this.label = label; position = at
        update(at)
    }

    fun move(at: Offset) { position = at; update(at) }

    private fun update(at: Offset) {
        val hit = targets.values.lastOrNull { it.bounds.contains(at) }
        if (hit !== hovered) {
            hovered?.onHover?.invoke(false)
            hit?.onHover?.invoke(true)
            hovered = hit
        }
        overTarget = hit != null
    }

    fun end() {
        val p = payload
        val t = hovered
        t?.onHover?.invoke(false)
        hovered = null
        payload = null; overTarget = false
        if (p != null && t != null) t.onDrop(p)
    }

    fun cancel() {
        hovered?.onHover?.invoke(false)
        hovered = null; payload = null; overTarget = false
    }
}

val LocalDragController = compositionLocalOf<DragController?> { null }

/** Long-press to pick this item up. */
@Composable
fun Modifier.libraryDragSource(payload: String, label: String = ""): Modifier {
    val ctrl = LocalDragController.current ?: return this
    var origin by remember { mutableStateOf(Offset.Zero) }
    val lbl by rememberUpdatedState(label)
    return this
        .onGloballyPositioned { origin = it.positionInRoot() }
        .pointerInput(payload) {
            var current = Offset.Zero
            detectDragGesturesAfterLongPress(
                onDragStart = { local -> current = origin + local; ctrl.start(payload, lbl, current) },
                onDrag = { change, amount -> change.consume(); current += amount; ctrl.move(current) },
                onDragEnd = { ctrl.end() },
                onDragCancel = { ctrl.cancel() },
            )
        }
}

/** Accepts library items; [onHover] tells the UI to highlight the target. */
@Composable
fun Modifier.libraryDropTarget(onHover: (Boolean) -> Unit, onDrop: (String) -> Boolean): Modifier {
    val ctrl = LocalDragController.current ?: return this
    val key = remember { Any() }
    val hover by rememberUpdatedState(onHover)
    val drop by rememberUpdatedState(onDrop)
    DisposableEffect(key) { onDispose { ctrl.unregister(key) } }
    return this.onGloballyPositioned { ctrl.register(key, it.boundsInRoot(), { h -> hover(h) }, { p -> drop(p) }) }
}
