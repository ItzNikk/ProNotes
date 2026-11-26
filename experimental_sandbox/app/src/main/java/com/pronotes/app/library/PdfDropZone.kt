package com.pronotes.app.library

import android.app.Activity
import android.content.ContextWrapper
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Holds the source app's temporary URI grant until the PDF bytes have been imported. */
@Composable
internal fun PdfDropZone(
    enabled: Boolean,
    busy: Boolean,
    onImport: suspend (List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importCurrent by rememberUpdatedState(onImport)
    val canDrop by rememberUpdatedState(enabled && !busy)
    var hovering by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    val target = remember(context, scope) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) { hovering = true }
            override fun onExited(event: DragAndDropEvent) { hovering = false }
            override fun onEnded(event: DragAndDropEvent) { hovering = false }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                hovering = false
                if (!canDrop || importing) return false
                val native = event.toAndroidDragEvent()
                val clip = native.clipData ?: return false
                val uris = (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
                    .filter { it.scheme == "content" }.distinct()
                if (uris.isEmpty()) return false
                val activity = generateSequence(context) { (it as? ContextWrapper)?.baseContext }
                    .filterIsInstance<Activity>().firstOrNull() ?: return false
                val permission = activity.requestDragAndDropPermissions(native)
                val importAtDrop = importCurrent
                importing = true
                val job = scope.launch {
                    try {
                        val pdfs = withContext(Dispatchers.IO) {
                            uris.filter { uri ->
                                val resolver = context.contentResolver
                                resolver.getType(uri).equals("application/pdf", ignoreCase = true) ||
                                    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                                        cursor.moveToFirst() && cursor.getString(0).orEmpty().endsWith(".pdf", ignoreCase = true)
                                    } == true
                            }
                        }
                        if (pdfs.isEmpty()) {
                            Toast.makeText(context, "Drop a PDF file to import it.", Toast.LENGTH_LONG).show()
                        } else importAtDrop(pdfs)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        Toast.makeText(context, "Could not import this PDF. Check that it is readable and unlocked.", Toast.LENGTH_LONG).show()
                    } finally {
                        importing = false
                    }
                }
                // Also releases the grant if the scope is cancelled before the coroutine starts.
                job.invokeOnCompletion { permission?.release() }
                return true
            }
        }
    }
    Box(
        modifier.dragAndDropTarget(
            shouldStartDragAndDrop = { event ->
                canDrop && !importing && event.mimeTypes().any {
                    it == "application/pdf" || it == "application/octet-stream" ||
                        it == "text/uri-list" || it == "*/*"
                }
            },
            target = target,
        ),
    ) {
        content()
        if (hovering && canDrop && !importing) {
            Box(
                Modifier.fillMaxSize().padding(12.dp)
                    .background(Color(0xF00D1528), RoundedCornerShape(24.dp))
                    .border(2.dp, Color(0xFF7BAAFF), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.UploadFile, null, Modifier.size(44.dp), tint = Color(0xFF9CBFFF))
                    Text("Drop PDFs to import", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("Add them to this folder", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB9C8E1))
                }
            }
        }
    }
}
