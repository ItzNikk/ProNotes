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
