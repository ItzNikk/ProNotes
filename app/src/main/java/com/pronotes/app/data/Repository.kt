package com.pronotes.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.pronotes.app.editor.InkCodec
import com.pronotes.app.editor.Page
import com.pronotes.app.editor.PageRenderer
import com.pronotes.app.editor.PdfPages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream
import java.util.UUID

/**
 * File-based storage:
 *   files/library.json            folders + notebook metadata
 *   files/notes/<id>/note.json    pages and items
 *   files/notes/<id>/source.pdf   imported PDF (optional)
 *   files/notes/<id>/thumb.png    first-page thumbnail
 *   files/notes/<id>/img_*.png    inserted images
 */
class Repository private constructor(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val root = File(context.filesDir, "notes").apply { mkdirs() }
    private val indexFile = File(context.filesDir, "library.json")
    private val lock = Mutex()
    private val thumbSeen = HashMap<String, Page>()

    /** Outlives screens, so a save started on exit still finishes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _library = MutableStateFlow(loadIndex())
    val library: StateFlow<LibraryIndex> = _library

    init {
        appScope.launch { runCatching { purgeTrash() } }
    }

    private fun loadIndex(): LibraryIndex = runCatching {
        if (indexFile.exists()) json.decodeFromString(LibraryIndex.serializer(), indexFile.readText()) else LibraryIndex()
    }.getOrElse { LibraryIndex() }

    private suspend fun updateIndex(block: (LibraryIndex) -> LibraryIndex) = withContext(Dispatchers.IO) {
        lock.withLock {
            val next = block(_library.value)
            val tmp = File(indexFile.path + ".tmp")
            tmp.writeText(json.encodeToString(LibraryIndex.serializer(), next))
            tmp.renameTo(indexFile)
            _library.value = next
        }
    }

    fun noteDir(id: String) = File(root, id).apply { mkdirs() }
    fun pdfFile(id: String) = File(noteDir(id), "source.pdf")
    fun thumbFile(id: String) = File(noteDir(id), "thumb.png")

    // ------------------------------------------------------------ folders

    suspend fun createFolder(name: String, parentId: String?, color: Int, emoji: String = "") = updateIndex {
        it.copy(folders = it.folders + Folder(UUID.randomUUID().toString(), name, parentId, color, System.currentTimeMillis(), emoji))
    }

    suspend fun updateFolder(id: String, name: String, color: Int, emoji: String) = updateIndex {
        it.copy(folders = it.folders.map { f -> if (f.id == id) f.copy(name = name, color = color, emoji = emoji, modified = System.currentTimeMillis()) else f })
    }

    suspend fun setCover(id: String, cover: String) = updateIndex {
        it.copy(notebooks = it.notebooks.map { n -> if (n.id == id) n.copy(cover = cover) else n })
    }

    suspend fun markOpened(id: String) = updateIndex {
        it.copy(notebooks = it.notebooks.map { n -> if (n.id == id) n.copy(opened = System.currentTimeMillis()) else n })
    }

    /** Copies a picked photo into the notebook folder and returns its cover spec. */
    suspend fun saveCoverImage(id: String, uri: Uri): String = withContext(Dispatchers.IO) {
        val name = "cover_${System.currentTimeMillis()}.img"
        context.contentResolver.openInputStream(uri)!!.use { input -> File(noteDir(id), name).outputStream().use { input.copyTo(it) } }
        "image:$name"
    }

    fun coverFile(id: String, name: String) = File(noteDir(id), name)

    suspend fun renameFolder(id: String, name: String) = updateIndex {
        it.copy(folders = it.folders.map { f -> if (f.id == id) f.copy(name = name) else f })
    }

    /** Deletes a folder and everything inside it. */
    suspend fun deleteFolder(id: String) {
        val idx = _library.value
        val doomed = HashSet<String>().apply { add(id) }
        var grew = true
        while (grew) {
            grew = false
            idx.folders.forEach { f -> if (f.parentId in doomed && doomed.add(f.id)) grew = true }
        }
        // Notebooks inside go to the trash, so nothing is lost by accident.
        val now = System.currentTimeMillis()
        updateIndex { cur ->
            cur.copy(
                folders = cur.folders.filterNot { it.id in doomed },
                notebooks = cur.notebooks.map { n -> if (n.folderId in doomed) n.copy(folderId = null, deletedAt = n.deletedAt ?: now) else n },
            )
        }
    }

    suspend fun moveFolder(id: String, parentId: String?) = updateIndex {
        it.copy(folders = it.folders.map { f -> if (f.id == id && parentId != id) f.copy(parentId = parentId) else f })
    }

    // ---------------------------------------------------------- notebooks

    /**
     * Creates a notebook. With [coverPage], page 1 is the cover itself (drawn from [coverSpec])
     * so it can be written on, and the library shows that page as the cover.
     */
    suspend fun createNotebook(
        title: String, folderId: String?, cover: Int, paper: PaperStyle, coverSpec: String = "",
        width: Float = PageSize.WIDTH, height: Float = PageSize.HEIGHT, paperColor: Int? = null, coverPage: Boolean = false,
        isWhiteboard: Boolean = false,
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val pages = buildList {
            if (coverPage && !isWhiteboard) add(PageDoc(UUID.randomUUID().toString(), width, height, PaperStyle.BLANK, cover = coverSpec.ifBlank { "solid:$cover" }))
            add(PageDoc(UUID.randomUUID().toString(), width, height, paper, paperColor = paperColor, isWhiteboard = isWhiteboard))
        }
        withContext(Dispatchers.IO) { File(noteDir(id), "note.json").writeText(json.encodeToString(NoteDoc.serializer(), NoteDoc(pages = pages))) }
        updateIndex {
            it.copy(notebooks = it.notebooks + NotebookMeta(id, title, folderId, cover, paper, now, now, pages.size, cover = if (coverPage && !isWhiteboard) "thumb" else coverSpec, isWhiteboard = isWhiteboard))
        }
        if (coverPage && !isWhiteboard) withContext(Dispatchers.IO) { runCatching { writeThumbnail(id, pages.map { InkCodec.fromDoc(it) }) } }
        return id
    }

    /** Changes the design of a notebook's cover page, if it has one. */
    suspend fun setCoverPage(id: String, spec: String): Boolean = withContext(Dispatchers.IO) {
        val f = File(noteDir(id), "note.json")
        val doc = runCatching { json.decodeFromString(NoteDoc.serializer(), f.readText()) }.getOrNull() ?: return@withContext false
        val first = doc.pages.firstOrNull() ?: return@withContext false
        if (first.cover == null) return@withContext false
        val next = doc.copy(pages = listOf(first.copy(cover = spec)) + doc.pages.drop(1))
        f.writeText(json.encodeToString(NoteDoc.serializer(), next))
        runCatching { writeThumbnail(id, next.pages.map { InkCodec.fromDoc(it) }) }
        updateIndex { it.copy(notebooks = it.notebooks.map { n -> if (n.id == id) n.copy(modified = System.currentTimeMillis(), cover = "thumb") else n }) }
        true
    }

    suspend fun importPdf(uri: Uri, folderId: String?): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val target = pdfFile(id)
        context.contentResolver.openInputStream(uri)!!.use { input -> target.outputStream().use { input.copyTo(it) } }
        val pages = ArrayList<PageDoc>()
        PdfRenderer(ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)).use { r ->
            for (i in 0 until r.pageCount) {
                r.openPage(i).use { p ->
                    // Scale so the shorter side is about one notebook page wide (landscape slides and
                    // small PDFs get the same pen feel as an A4 page).
                    val k = (PageSize.WIDTH / minOf(p.width, p.height).toFloat()).coerceIn(1f, 4f)
                    pages += PageDoc(UUID.randomUUID().toString(), p.width * k, p.height * k, PaperStyle.BLANK, pdfPage = i)
                }
            }
        }
        File(noteDir(id), "note.json").writeText(json.encodeToString(NoteDoc.serializer(), NoteDoc(pages = pages)))
        val title = displayName(uri)?.removeSuffix(".pdf")?.removeSuffix(".PDF") ?: "Imported PDF"
        val now = System.currentTimeMillis()
        updateIndex {
            it.copy(notebooks = it.notebooks + NotebookMeta(id, title, folderId, CoverColors[0], PaperStyle.BLANK, now, now, pages.size, fromPdf = true))
        }
        runCatching { writeThumbnail(id, pages.map { InkCodec.fromDoc(it) }) }
        id
    }

    /** Imports PDFs, images, Markdown/TXT/HTML/CSV files, or JSON note exports into a new notebook. */
    suspend fun importFiles(uris: List<Uri>, folderId: String?): String? = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext null
        val first = uris.first()
        val firstName = displayName(first) ?: "Imported Note"
        val firstMime = context.contentResolver.getType(first)?.lowercase().orEmpty()
        if (uris.size == 1 && (firstMime == "application/pdf" || firstName.endsWith(".pdf", true))) {
            return@withContext importPdf(first, folderId)
        }
        val id = UUID.randomUUID().toString()
        val pageDocs = ArrayList<PageDoc>()
        var coverColor = CoverColors[0]
        var importedTitle = firstName.substringBeforeLast('.').ifBlank { "Imported Notes" }

        val imageUris = uris.filter { u ->
            val m = context.contentResolver.getType(u)?.lowercase().orEmpty()
            val n = displayName(u).orEmpty().lowercase()
            m.startsWith("image/") || n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".webp")
        }
        if (imageUris.size == uris.size) {
            for (u in imageUris) {
                val imgName = runCatching { saveImage(id, u) }.getOrNull() ?: continue
                val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                File(noteDir(id), imgName).inputStream().use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
                val bw = opts.outWidth.coerceAtLeast(200).toFloat()
                val bh = opts.outHeight.coerceAtLeast(200).toFloat()
                val maxW = PageSize.WIDTH - 120f
                val maxH = PageSize.HEIGHT - 160f
                val sc = minOf(maxW / bw, maxH / bh).coerceAtMost(3f)
                val w = bw * sc
                val h = bh * sc
                val x = (PageSize.WIDTH - w) / 2f
                val y = (PageSize.HEIGHT - h) / 2f
                pageDocs += PageDoc(
                    id = UUID.randomUUID().toString(),
                    width = PageSize.WIDTH,
                    height = PageSize.HEIGHT,
                    paper = PaperStyle.BLANK,
                    items = listOf(ItemDoc.Image(UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE, imgName, x, y, w, h)),
                )
            }
            if (imageUris.size > 1) importedTitle = "${firstName.substringBeforeLast('.')} (+${imageUris.size - 1})"
        } else {
            for (u in uris) {
                val name = displayName(u) ?: "Imported"
                val mime = context.contentResolver.getType(u)?.lowercase().orEmpty()
                when {
                    mime == "application/pdf" || name.endsWith(".pdf", true) -> {
                        val subId = importPdf(u, folderId)
                        if (uris.size == 1) return@withContext subId
                        val subPages = runCatching { json.decodeFromString(NoteDoc.serializer(), File(noteDir(subId), "note.json").readText()).pages }.getOrDefault(emptyList())
                        if (subPages.isNotEmpty()) return@withContext subId
                    }
                    mime.startsWith("image/") || name.endsWith(".png", true) || name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".webp", true) -> {
                        val imgName = runCatching { saveImage(id, u) }.getOrNull() ?: continue
                        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        File(noteDir(id), imgName).inputStream().use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
                        val bw = opts.outWidth.coerceAtLeast(200).toFloat()
                        val bh = opts.outHeight.coerceAtLeast(200).toFloat()
                        val sc = minOf((PageSize.WIDTH - 120f) / bw, (PageSize.HEIGHT - 160f) / bh).coerceAtMost(3f)
                        pageDocs += PageDoc(
                            id = UUID.randomUUID().toString(),
                            width = PageSize.WIDTH,
                            height = PageSize.HEIGHT,
                            paper = PaperStyle.BLANK,
                            items = listOf(ItemDoc.Image(nextImportItemId(), imgName, (PageSize.WIDTH - bw * sc) / 2f, (PageSize.HEIGHT - bh * sc) / 2f, bw * sc, bh * sc)),
                        )
                    }
                    else -> {
                        val raw = runCatching {
                            context.contentResolver.openInputStream(u)?.bufferedReader()?.use { it.readText() }
                        }.getOrNull().orEmpty()
                        if (raw.isBlank()) continue
                        if (name.endsWith(".json", true) || raw.trimStart().startsWith("{")) {
                            val decoded = runCatching { json.decodeFromString(NoteDoc.serializer(), raw) }.getOrNull()
                            if (decoded != null && decoded.pages.isNotEmpty()) {
                                pageDocs += decoded.pages.map { it.copy(id = UUID.randomUUID().toString(), pdfPage = -1) }
                                continue
                            }
                        }
                        val normalized = normalizeImportedText(raw, name)
                        pageDocs += textToPageDocs(name.substringBeforeLast('.'), normalized)
                    }
                }
            }
        }

        if (pageDocs.isEmpty()) {
            pageDocs += PageDoc(UUID.randomUUID().toString(), PageSize.WIDTH, PageSize.HEIGHT, PaperStyle.RULED)
        }
        File(noteDir(id), "note.json").writeText(json.encodeToString(NoteDoc.serializer(), NoteDoc(pages = pageDocs)))
        val now = System.currentTimeMillis()
        updateIndex {
            it.copy(notebooks = it.notebooks + NotebookMeta(id, importedTitle, folderId, coverColor, PaperStyle.RULED, now, now, pageDocs.size))
        }
        runCatching { writeThumbnail(id, pageDocs.map { InkCodec.fromDoc(it) }) }
        id
    }

    /** Creates a paginated notebook from pasted notes/Markdown/HTML/plain text copied from Notion, Obsidian, Keep, Apple Notes, etc. */
    suspend fun importStructuredText(title: String, rawText: String, folderId: String?): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val cleanTitle = title.trim().ifBlank { deriveTitleFromText(rawText) }
        val normalized = normalizeImportedText(rawText, "$cleanTitle.md")
        val pageDocs = textToPageDocs(cleanTitle, normalized).ifEmpty {
            listOf(PageDoc(UUID.randomUUID().toString(), PageSize.WIDTH, PageSize.HEIGHT, PaperStyle.RULED))
        }
        File(noteDir(id), "note.json").writeText(json.encodeToString(NoteDoc.serializer(), NoteDoc(pages = pageDocs)))
        val now = System.currentTimeMillis()
        updateIndex {
            it.copy(notebooks = it.notebooks + NotebookMeta(id, cleanTitle, folderId, CoverColors[1], PaperStyle.RULED, now, now, pageDocs.size))
        }
        runCatching { writeThumbnail(id, pageDocs.map { InkCodec.fromDoc(it) }) }
        id
    }

    /** Builds new pages from files/images to append into an existing notebook. */
    suspend fun buildPagesFromUris(noteId: String, uris: List<Uri>): List<Page> = withContext(Dispatchers.IO) {
        val out = ArrayList<PageDoc>()
        for (u in uris) {
            val name = displayName(u) ?: "Imported"
            val mime = context.contentResolver.getType(u)?.lowercase().orEmpty()
            if (mime.startsWith("image/") || name.endsWith(".png", true) || name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".webp", true)) {
                val imgName = runCatching { saveImage(noteId, u) }.getOrNull() ?: continue
                val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                File(noteDir(noteId), imgName).inputStream().use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
                val bw = opts.outWidth.coerceAtLeast(200).toFloat()
                val bh = opts.outHeight.coerceAtLeast(200).toFloat()
                val sc = minOf((PageSize.WIDTH - 120f) / bw, (PageSize.HEIGHT - 160f) / bh).coerceAtMost(3f)
                out += PageDoc(
                    id = UUID.randomUUID().toString(),
                    width = PageSize.WIDTH,
                    height = PageSize.HEIGHT,
                    paper = PaperStyle.BLANK,
                    items = listOf(ItemDoc.Image(nextImportItemId(), imgName, (PageSize.WIDTH - bw * sc) / 2f, (PageSize.HEIGHT - bh * sc) / 2f, bw * sc, bh * sc)),
                )
            } else {
                val raw = runCatching { context.contentResolver.openInputStream(u)?.bufferedReader()?.use { it.readText() } }.getOrNull().orEmpty()
                if (raw.isNotBlank()) {
                    val decoded = if (name.endsWith(".json", true) || raw.trimStart().startsWith("{")) {
                        runCatching { json.decodeFromString(NoteDoc.serializer(), raw) }.getOrNull()
                    } else null
                    if (decoded != null && decoded.pages.isNotEmpty()) {
                        out += decoded.pages.map { it.copy(id = UUID.randomUUID().toString(), pdfPage = -1) }
                    } else {
                        out += textToPageDocs(name.substringBeforeLast('.'), normalizeImportedText(raw, name))
                    }
                }
            }
        }
        out.map { InkCodec.fromDoc(it) }
    }

    fun buildPagesFromText(title: String, rawText: String): List<Page> =
        textToPageDocs(title.ifBlank { deriveTitleFromText(rawText) }, normalizeImportedText(rawText, "$title.md"))
            .map { InkCodec.fromDoc(it) }

    private fun nextImportItemId(): Long = (UUID.randomUUID().mostSignificantBits and 0x0000FFFFFFFFFFFFL).coerceAtLeast(1000L)

    private fun deriveTitleFromText(raw: String): String {
        val first = raw.lineSequence().map { it.trim().trimStart('#', '-', '*', '>', ' ') }.firstOrNull { it.isNotBlank() } ?: "Imported Note"
        return first.take(42)
    }

    private fun normalizeImportedText(raw: String, filename: String): String {
        val lower = filename.lowercase()
        var txt = raw.replace("\r\n", "\n").replace("\r", "\n")
        if (lower.endsWith(".html") || lower.endsWith(".htm") || (txt.contains("<p", true) && txt.contains("</", true))) {
            txt = txt
                .replace(Regex("""(?is)<style.*?</style>"""), "")
                .replace(Regex("""(?is)<script.*?</script>"""), "")
                .replace(Regex("""(?i)<br\s*/?>"""), "\n")
                .replace(Regex("""(?i)</(p|div|h[1-6]|li|tr|blockquote)>"""), "\n")
                .replace(Regex("""(?i)<li[^>]*>"""), "• ")
                .replace(Regex("""(?i)<h[1-3][^>]*>"""), "## ")
                .replace(Regex("""<[^>]+>"""), "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
        } else if (lower.endsWith(".csv")) {
            txt = txt.lines().filter { it.isNotBlank() }.joinToString("\n") { row ->
                row.split(',').joinToString("  |  ") { it.trim().trim('"') }
            }
        }
        // Clean markdown inline syntax cleanly while keeping visual structure
        return txt
            .replace(Regex("""^- \[x\]\s+""", RegexOption.MULTILINE), "☑ ")
            .replace(Regex("""^- \[ \]\s+""", RegexOption.MULTILINE), "☐ ")
            .replace(Regex("""^[-*]\s+""", RegexOption.MULTILINE), "• ")
            .replace(Regex("""\*\*(.+?)\*\*"""), "$1")
            .replace(Regex("""__(.+?)__"""), "$1")
            .replace(Regex("""`([^`]+)`"""), "$1")
            .trim()
    }

    private fun textToPageDocs(sectionTitle: String, body: String): List<PageDoc> {
        val out = ArrayList<PageDoc>()
        val marginX = 76f
        val maxY = PageSize.HEIGHT - 110f
        var curItems = ArrayList<ItemDoc>()
        var y = 84f

        fun flushPage() {
            if (curItems.isNotEmpty()) {
                out += PageDoc(UUID.randomUUID().toString(), PageSize.WIDTH, PageSize.HEIGHT, PaperStyle.RULED, items = curItems)
                curItems = ArrayList()
                y = 84f
            }
        }

        if (sectionTitle.isNotBlank()) {
            curItems += ItemDoc.Text(nextImportItemId(), sectionTitle, marginX, y, 36f, 0xFF1B1F2A.toInt(), style = TextStyleDoc(bold = true))
            y += 66f
        }

        val paragraphs = body.split(Regex("""\n{2,}""")).map { it.trim() }.filter { it.isNotEmpty() }
        for (para in paragraphs) {
            val isHeading = para.startsWith("#")
            val isCallout = para.startsWith(">")
            val clean = if (isHeading) para.trimStart('#', ' ') else if (isCallout) para.trimStart('>', ' ') else para
            if (isCallout && clean.length <= 360) {
                if (y + 220f > maxY) flushPage()
                val h = (120f + (clean.length / 36f) * 32f).coerceIn(140f, 300f)
                curItems += ItemDoc.Sticky(nextImportItemId(), marginX, y, PageSize.WIDTH - marginX * 2, h, 0xFFFFF3A3.toInt(), clean, fontSize = 23f)
                y += h + 28f
                continue
            }
            val lines = clean.lines()
            val estHeight = (lines.sumOf { ((it.length / 46) + 1) } * if (isHeading) 42 else 34).toFloat().coerceAtLeast(36f)
            if (y + estHeight > maxY && curItems.isNotEmpty()) flushPage()
            curItems += ItemDoc.Text(
                id = nextImportItemId(),
                text = clean,
                x = marginX,
                y = y,
                size = if (isHeading) 30f else 24f,
                color = if (isHeading) 0xFF2855C8.toInt() else 0xFF1B1F2A.toInt(),
                style = TextStyleDoc(bold = isHeading),
            )
            y += estHeight + if (isHeading) 24f else 20f
        }
        flushPage()
        return out
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

    suspend fun renameNotebook(id: String, title: String) = updateIndex {
        it.copy(notebooks = it.notebooks.map { n -> if (n.id == id) n.copy(title = title) else n })
    }

    suspend fun moveNotebook(id: String, folderId: String?) = updateIndex {
        it.copy(notebooks = it.notebooks.map { n -> if (n.id == id) n.copy(folderId = folderId) else n })
    }

    suspend fun toggleFavorite(id: String) = updateIndex {
        it.copy(notebooks = it.notebooks.map { n -> if (n.id == id) n.copy(favorite = !n.favorite) else n })
    }

    /** Moves a notebook to the trash. It stays there for 30 days. */
    suspend fun deleteNotebook(id: String) = updateIndex {
        it.copy(notebooks = it.notebooks.map { n -> if (n.id == id) n.copy(deletedAt = System.currentTimeMillis()) else n })
    }

    suspend fun restoreNotebook(id: String) = updateIndex { cur ->
        val folderIds = cur.folders.map { it.id }.toSet()
        cur.copy(notebooks = cur.notebooks.map { n ->
            if (n.id == id) n.copy(deletedAt = null, folderId = n.folderId?.takeIf { it in folderIds }) else n
        })
    }

    suspend fun deleteForever(id: String) {
        withContext(Dispatchers.IO) { File(root, id).deleteRecursively() }
        updateIndex { it.copy(notebooks = it.notebooks.filterNot { n -> n.id == id }) }
    }

    suspend fun emptyTrash() {
        _library.value.notebooks.filter { it.deletedAt != null }.forEach { deleteForever(it.id) }
    }

    /** Removes notebooks that have been in the trash for more than 30 days. */
    suspend fun purgeTrash() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
        _library.value.notebooks.filter { (it.deletedAt ?: Long.MAX_VALUE) < cutoff }.forEach { deleteForever(it.id) }
    }

    suspend fun duplicateNotebook(id: String): String? = withContext(Dispatchers.IO) {
        val meta = meta(id) ?: return@withContext null
        val newId = UUID.randomUUID().toString()
        File(root, id).copyRecursively(File(root, newId), overwrite = true)
        val now = System.currentTimeMillis()
        updateIndex { it.copy(notebooks = it.notebooks + meta.copy(id = newId, title = meta.title + " copy", created = now, modified = now, opened = now, favorite = false)) }
        newId
    }

    /** Writes every notebook, folder and sticker into one ZIP file. */
    suspend fun backup(out: OutputStream) = withContext(Dispatchers.IO) {
        java.util.zip.ZipOutputStream(out.buffered()).use { zip ->
            fun add(file: File, name: String) {
                if (file.isDirectory) file.listFiles()?.forEach { add(it, "$name/${it.name}") }
                else { zip.putNextEntry(java.util.zip.ZipEntry(name)); file.inputStream().use { it.copyTo(zip) }; zip.closeEntry() }
            }
            if (indexFile.exists()) add(indexFile, "library.json")
            add(root, "notes")
            File(context.filesDir, "stickers").takeIf { it.exists() }?.let { add(it, "stickers") }
        }
    }

    /** Restores a backup made by [backup]. Notebooks in the backup are added; existing ones are kept. */
    suspend fun restore(input: java.io.InputStream): Int = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        java.util.zip.ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                val f = File(tmp, e.name).canonicalFile
                if (!f.path.startsWith(tmp.canonicalPath)) continue   // ignore unsafe paths
                if (e.isDirectory) f.mkdirs() else { f.parentFile?.mkdirs(); f.outputStream().use { zip.copyTo(it) } }
            }
        }
        val idx = runCatching { json.decodeFromString(LibraryIndex.serializer(), File(tmp, "library.json").readText()) }.getOrNull()
            ?: return@withContext 0
        val have = _library.value.notebooks.map { it.id }.toSet()
        val haveFolders = _library.value.folders.map { it.id }.toSet()
        val newNotes = idx.notebooks.filter { it.id !in have }
        newNotes.forEach { n -> File(tmp, "notes/${n.id}").takeIf { it.exists() }?.copyRecursively(File(root, n.id), overwrite = true) }
        File(tmp, "stickers").takeIf { it.exists() }?.copyRecursively(File(context.filesDir, "stickers"), overwrite = true)
        updateIndex { cur -> cur.copy(folders = cur.folders + idx.folders.filter { it.id !in haveFolders }, notebooks = cur.notebooks + newNotes) }
        tmp.deleteRecursively()
        newNotes.size
    }

    fun meta(id: String): NotebookMeta? = _library.value.notebooks.firstOrNull { it.id == id }

    suspend fun loadPages(id: String): List<Page> = withContext(Dispatchers.IO) {
        val f = File(noteDir(id), "note.json")
        val doc = runCatching { json.decodeFromString(NoteDoc.serializer(), f.readText()) }.getOrNull()
        doc?.pages?.map { InkCodec.fromDoc(it) }
            ?: listOf(Page(UUID.randomUUID().toString(), PageSize.WIDTH, PageSize.HEIGHT, PaperStyle.RULED))
    }

    suspend fun savePages(id: String, pages: List<Page>) = withContext(Dispatchers.IO) {
        val doc = NoteDoc(pages = pages.map { InkCodec.toDoc(it) })
        val f = File(noteDir(id), "note.json")
        val tmp = File(f.path + ".tmp")
        tmp.writeText(json.encodeToString(NoteDoc.serializer(), doc))
        tmp.renameTo(f)
        // The library thumbnail shows page 1, so only redraw it when page 1 itself changed.
        val first = pages.firstOrNull()
        if (first != null && (thumbSeen[id] !== first || !thumbFile(id).exists())) {
            runCatching { writeThumbnail(id, pages) }
            thumbSeen[id] = first
        }
        updateIndex {
            it.copy(notebooks = it.notebooks.map { n ->
                if (n.id == id) n.copy(modified = System.currentTimeMillis(), pageCount = pages.size) else n
            })
        }
    }

    private fun writeThumbnail(id: String, pages: List<Page>) {
        val page = pages.firstOrNull() ?: return
        val scale = 360f / page.width
        val w = (page.width * scale).toInt(); val h = (page.height * scale).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        val pdf = if (page.pdfPage >= 0) PdfPages(pdfFile(id)).let { pp ->
            pp.render(page.pdfPage, w, h).also { pp.close() }
        } else null
        // Render the PDF bitmap at thumbnail size, so draw it with scale 1.
        val canvas = Canvas(bmp)
        PageRenderer(noteDir(id)).renderPage(canvas, page, scale, pdf)
        thumbFile(id).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
    }

    /** Writes the notebook as a PDF: original PDF pages as backgrounds, ink on top. */
    suspend fun exportPdf(id: String, pages: List<Page>, out: OutputStream) = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val renderer = PageRenderer(noteDir(id))
        val pdfPages = PdfPages(pdfFile(id))
        try {
            pages.forEachIndexed { i, page ->
                val wPt = (page.width / 2f).toInt(); val hPt = (page.height / 2f).toInt()
                val pdfPage = doc.startPage(PdfDocument.PageInfo.Builder(wPt, hPt, i + 1).create())
                val bg = if (page.pdfPage >= 0 && pdfPages.exists) {
                    pdfPages.render(page.pdfPage, page.width.toInt(), page.height.toInt(), useCache = false)
                } else null
                renderer.renderPage(pdfPage.canvas, page, 0.5f, bg)
                doc.finishPage(pdfPage)
            }
            doc.writeTo(out)
        } finally {
            doc.close()
            pdfPages.close()
        }
    }

    suspend fun saveImage(noteId: String, uri: Uri): String = withContext(Dispatchers.IO) {
        val name = "img_${UUID.randomUUID()}.img"
        context.contentResolver.openInputStream(uri)!!.use { input ->
            File(noteDir(noteId), name).outputStream().use { input.copyTo(it) }
        }
        name
    }

    companion object {
        @Volatile private var instance: Repository? = null
        fun get(context: Context): Repository =
            instance ?: synchronized(this) { instance ?: Repository(context.applicationContext).also { instance = it } }
    }
}
