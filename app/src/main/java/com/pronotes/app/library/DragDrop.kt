package com.pronotes.app.library

import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent

/*
 * Drag and drop inside the library: long-press a notebook or folder and drop it on
 * a folder (move into it), on "Documents" (move to the top level) or on "Trash".
 * Payloads are "note:<id>" or "folder:<id>".
 */

private const val LABEL = "pronotes"

/** Long-press to pick this item up. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.libraryDragSource(payload: String): Modifier =
    dragAndDropSource { _ -> DragAndDropTransferData(ClipData.newPlainText(LABEL, payload)) }

/** Accepts library items; [onHover] tells the UI to highlight the drop target. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.libraryDropTarget(onHover: (Boolean) -> Unit, onDrop: (String) -> Boolean): Modifier {
    val hover by rememberUpdatedState(onHover)
    val drop by rememberUpdatedState(onDrop)
    val target = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                hover(false)
                val clip = event.toAndroidDragEvent().clipData ?: return false
                if (clip.description?.label != LABEL || clip.itemCount == 0) return false
                return drop(clip.getItemAt(0).text?.toString() ?: return false)
            }
            override fun onEntered(event: DragAndDropEvent) = hover(true)
            override fun onExited(event: DragAndDropEvent) = hover(false)
            override fun onEnded(event: DragAndDropEvent) = hover(false)
        }
    }
    return dragAndDropTarget(shouldStartDragAndDrop = { true }, target = target)
}
