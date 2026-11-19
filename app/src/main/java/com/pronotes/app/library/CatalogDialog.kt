package com.pronotes.app.library

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pronotes.app.data.Repository
import com.pronotes.app.ui.AppDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.io.ByteArrayOutputStream

private data class Listing(val title: String, val detail: String, val source: String)

private fun download(address: String, limit: Int): ByteArray {
    val connection = URL(address).openConnection().apply {
        connectTimeout = 15000
        readTimeout = 25000
        setRequestProperty("User-Agent", "ProNotes-Android/14")
    }
    return connection.getInputStream().use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= limit) { "This file is too large to import." }
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    }
}

private fun listings(books: Boolean, query: String, page: Int): List<Listing> {
    val address = if (books) "https://gutendex.com/books/?copyright=false&search=" + Uri.encode(query)
    else "https://api.github.com/search/issues?q=" + Uri.encode("repo:ItzNikk/ProNotes is:issue is:open in:title ProNotes-Asset " + query)
    val json = JSONObject(download("$address&page=$page", 2_000_000).toString(Charsets.UTF_8))
    val rows = json.getJSONArray(if (books) "results" else "items")
    return buildList {
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (books) {
                val formats = row.getJSONObject("formats")
                val source = formats.keys().asSequence().filter { it.startsWith("text/plain") }
                    .map { formats.getString(it) }.firstOrNull {
                        val uri = Uri.parse(it)
                        uri.scheme == "https" && uri.host in setOf("www.gutenberg.org", "gutenberg.org") && !uri.path.orEmpty().endsWith(".zip")
                    } ?: continue
                val author = row.getJSONArray("authors").optJSONObject(0)?.optString("name").orEmpty()
                add(Listing(row.getString("title"), author, source))
            } else {
                val attachment = Regex("""https://(?:github\.com/user-attachments/assets/[a-fA-F0-9-]+|user-images\.githubusercontent\.com/[\w./-]+)""")
                    .find(row.optString("body"))?.value ?: continue
                add(Listing(row.getString("title").removePrefix("ProNotes-Asset ").trim(), row.getJSONObject("user").getString("login"), attachment))
            }
        }
    }
}

@Composable
internal fun CatalogDialog(books: Boolean, folder: String?, close: () -> Unit, open: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(1) }
    var searched by rememberSaveable { mutableStateOf("") }
    var preview by remember { mutableStateOf<Pair<String, android.graphics.Bitmap>?>(null) }
    var results by remember { mutableStateOf(emptyList<Listing>()) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var pending by rememberSaveable { mutableStateOf("") }
    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        status = ""
        scope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { status = e.localizedMessage ?: "Could not connect. Please retry." }
            finally { busy = false }
        }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) run {
            withContext(Dispatchers.IO) {
                val bytes = download(pending, 8_000_000)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                require(bounds.outWidth.toLong() * bounds.outHeight in 1L..12_000_000L) { "Unsupported image size." }
                val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                try {
                    requireNotNull(context.contentResolver.openOutputStream(uri)).use {
                        check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
                    }
                } finally { bitmap.recycle() }
            }
            val sticker = withContext(Dispatchers.IO) { com.pronotes.app.editor.StickerLibrary.import(context, uri) }
            status = if (sticker != null) "Added to your sticker tray. The saved PNG can also be used as a cover." else "Artwork saved as a PNG."
        }
    }
    fun search(number: Int = 1, term: String = query.trim()) = run {
        val items = withContext(Dispatchers.IO) { listings(books, term, number) }
        page = number
        searched = term
        preview = null
        results = items
        if (results.isEmpty()) status = "No results found."
    }
    LaunchedEffect(Unit) { search(page, searched) }
    AppDialog(onDismissRequest = close, title = { Text(if (books) "Online books" else "Community marketplace") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, singleLine = true, label = { Text(if (books) "Title or author" else "Sticker or cover") }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { search() }, enabled = !busy) { Text("Search") }
                if (!books) TextButton(onClick = {
                    val url = Uri.parse("https://github.com/ItzNikk/ProNotes/issues/new").buildUpon()
                        .appendQueryParameter("title", "ProNotes-Asset ")
                        .appendQueryParameter("body", "Attach your original sticker or cover image here. By posting, I allow others to use this artwork for free in their notebooks.").build()
                    context.startActivity(Intent(Intent.ACTION_VIEW, url))
                }) { Text("Upload artwork on GitHub") }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (status.isNotEmpty()) Text(status)
                results.forEach { item ->
                    HorizontalDivider()
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(item.detail, style = MaterialTheme.typography.bodySmall)
                    if (!books) {
                        val current = preview
                        if (current?.first == item.source) {
                            androidx.compose.foundation.Image(current.second.asImageBitmap(), item.title,
                                Modifier.fillMaxWidth().height(150.dp))
                        } else TextButton(enabled = !busy, onClick = { run {
                            val thumb = withContext(Dispatchers.IO) {
                                val bytes = download(item.source, 8_000_000)
                                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                                require(opts.outWidth > 0 && opts.outHeight > 0) { "Unsupported image." }
                                opts.inJustDecodeBounds = false
                                opts.inSampleSize = 1
                                while (maxOf(opts.outWidth, opts.outHeight) / opts.inSampleSize > 512) opts.inSampleSize *= 2
                                requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts))
                            }
                            preview = item.source to thumb
                        } }) { Text("Preview artwork") }
                    }
                    TextButton(enabled = !busy, onClick = {
                        if (books) run {
                            val body = withContext(Dispatchers.IO) { download(item.source, 8_000_000).toString(Charsets.UTF_8) }
                            val id = Repository.get(context).importStructuredText(item.title, body, folder)
                            close()
                            open(id)
                        } else {
                            pending = item.source
                            save.launch("ProNotes-art.png")
                        }
                    }) { Text(if (books) "Add to library and read" else "Save artwork") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (page > 1) TextButton(enabled = !busy, onClick = { search(page - 1, searched) }) { Text("Previous page") }
                    if (results.isNotEmpty()) TextButton(enabled = !busy, onClick = { search(page + 1, searched) }) { Text("Next page") }
                }
            }
        }, confirmButton = { TextButton(onClick = close) { Text("Close") } })
}
