package com.pronotes.app.library

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pronotes.app.ui.AppDialog
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private fun transform(context: Context, inputs: List<Uri>, task: PdfTask, range: String, output: File) {
    PDFBoxResourceLoader.init(context.applicationContext)
    require(inputs.isNotEmpty()) { "Choose a PDF first." }
    PDDocument().use { result ->
        if (task == PdfTask.IMAGES) {
            for (uri in inputs) {
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unreadable image." }
                val options = android.graphics.BitmapFactory.Options()
                while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize.coerceAtLeast(1) > 2200) {
                    options.inSampleSize = options.inSampleSize.coerceAtLeast(1) * 2
                }
                val bitmap = requireNotNull(context.contentResolver.openInputStream(uri)?.use {
                    android.graphics.BitmapFactory.decodeStream(it, null, options)
                })
                try {
                    val width = bitmap.width.toFloat()
                    val height = bitmap.height.toFloat()
                    val page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle(width, height))
                    result.addPage(page)
                    val image = com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(result, bitmap)
                    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(result, page).use { it.drawImage(image, 0f, 0f, width, height) }
                } finally { bitmap.recycle() }
            }
            result.save(output)
            return
        }
        for (uri in inputs) {
            requireNotNull(context.contentResolver.openInputStream(uri)).use { stream ->
                PDDocument.load(stream).use { source ->
                    val count = source.numberOfPages
                    when (task) {
                        PdfTask.MERGE -> PDFMergerUtility().appendDocument(result, source)
                        PdfTask.TEXT -> {
                            output.bufferedWriter().use { PDFTextStripper().writeText(source, it) }
                            return
                        }
                        PdfTask.ROTATE -> {
                            source.pages.forEach { it.rotation = (it.rotation + 90) % 360 }
                            source.save(output)
                            return
                        }
                        else -> {
                            val indices = task.pages(count, range)
                            indices.forEach { index ->
                                val page = source.getPage(index)
                                result.importPage(page).resources = page.resources
                            }
                            result.save(output)
                            return
                        }
                    }
                }
            }
        }
        result.save(output)
    }
}

@Composable
internal fun PdfStation(close: () -> Unit, importFiles: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var task by rememberSaveable { mutableStateOf(PdfTask.MERGE) }
    var files by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var range by rememberSaveable { mutableStateOf("1") }
    var pending by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) files = ArrayList(uris.map { it.toString() })
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val temp = File(context.cacheDir, pending)
        val destination = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && destination != null && temp.isFile) {
            busy = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        requireNotNull(context.contentResolver.openOutputStream(destination)).use { out -> temp.inputStream().use { it.copyTo(out) } }
                    }
                    status = "File saved."
                } catch (e: Exception) { status = e.message ?: "Could not save file." }
                finally { temp.delete(); busy = false }
            }
        } else if (temp.isFile) temp.delete()
    }
    AppDialog(onDismissRequest = { if (!busy) close() }, title = { Text("PDF Station") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = importFiles, enabled = !busy) { Text("Import documents or images into a notebook") }
            PdfTask.entries.forEach { item ->
                FilterChip(selected = item == task, onClick = {
                    if ((task == PdfTask.IMAGES) != (item == PdfTask.IMAGES)) files = arrayListOf()
                    task = item
                }, enabled = !busy, label = { Text(item.label) })
            }
            if (task == PdfTask.EXTRACT) OutlinedTextField(range, { range = it }, singleLine = true, label = { Text("Pages, e.g. 2-5 (type 2-5)") })
            TextButton(enabled = !busy, onClick = { pick.launch(arrayOf(if (task == PdfTask.IMAGES) "image/*" else "application/pdf")) }) {
                Text(if (files.isEmpty()) (if (task == PdfTask.IMAGES) "Choose images" else "Choose PDFs") else "Change selection (${files.size})")
            }
            if (files.size > 1 && task in listOf(PdfTask.MERGE, PdfTask.IMAGES)) {
                files.forEachIndexed { index, raw ->
                    val name = remember(raw) { runCatching { context.contentResolver.query(Uri.parse(raw), arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } }.getOrNull() ?: "File ${index + 1}" }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${index + 1}. $name", Modifier.weight(1f), maxLines = 2)
                        if (index > 0) TextButton(enabled = !busy, onClick = {
                            files = ArrayList(files).apply { add(index - 1, removeAt(index)) }
                        }) { Text("Move up") }
                    }
                }
            }
            if (files.size > 1 && task !in listOf(PdfTask.MERGE, PdfTask.IMAGES)) Text("Choose one PDF for this operation.")
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (status.isNotBlank()) Text(status)
        }
    }, dismissButton = { TextButton(onClick = close, enabled = !busy) { Text("Close") } }, confirmButton = {
        Button(enabled = !busy && files.isNotEmpty() && (task in listOf(PdfTask.MERGE, PdfTask.IMAGES) || files.size == 1), onClick = {
            busy = true
            scope.launch {
                try {
                    val temp = withContext(Dispatchers.IO) {
                        val file = File.createTempFile("pdf-station-", ".tmp", context.cacheDir)
                        try { transform(context, files.map(Uri::parse), task, range, file); file }
                        catch (e: Exception) { file.delete(); throw e }
                    }
                    pending = temp.name
                    val plain = task == PdfTask.TEXT
                    save.launch(android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(android.content.Intent.CATEGORY_OPENABLE)
                        type = if (plain) "text/plain" else "application/pdf"
                        putExtra(android.content.Intent.EXTRA_TITLE, if (plain) "Extracted text.txt" else "ProNotes.pdf")
                    })
                } catch (e: Exception) { status = e.message ?: "Could not process PDF." }
                finally { busy = false }
            }
        }) { Text("Export") }
    })
}
