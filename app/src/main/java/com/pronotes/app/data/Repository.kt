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

    /** Outlives screens, so a save started on exit still finishes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _library = MutableStateFlow(loadIndex())
    val library: StateFlow<LibraryIndex> = _library

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

    suspend fun createFolder(name: String, parentId: String?, color: Int) = updateIndex {
        it.copy(folders = it.folders + Folder(UUID.randomUUID().toString(), name, parentId, color, System.currentTimeMillis()))
    }

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
        val notes = idx.notebooks.filter { it.folderId in doomed }
        withContext(Dispatchers.IO) { notes.forEach { File(root, it.id).deleteRecursively() } }
        updateIndex { cur ->
            cur.copy(
                folders = cur.folders.filterNot { it.id in doomed },
                notebooks = cur.notebooks.filterNot { it.folderId in doomed },
            )
        }
    }

    suspend fun moveFolder(id: String, parentId: String?) = updateIndex {
        it.copy(folders = it.folders.map { f -> if (f.id == id && parentId != id) f.copy(parentId = parentId) else f })
    }

    // ---------------------------------------------------------- notebooks

    suspend fun createNotebook(title: String, folderId: String?, cover: Int, paper: PaperStyle): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val doc = NoteDoc(pages = listOf(PageDoc(UUID.randomUUID().toString(), PageSize.WIDTH, PageSize.HEIGHT, paper)))
        withContext(Dispatchers.IO) { File(noteDir(id), "note.json").writeText(json.encodeToString(NoteDoc.serializer(), doc)) }
        updateIndex {
            it.copy(notebooks = it.notebooks + NotebookMeta(id, title, folderId, cover, paper, now, now, 1))
        }
        return id
    }

    suspend fun importPdf(uri: Uri, folderId: String?): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val target = pdfFile(id)
        context.contentResolver.openInputStream(uri)!!.use { input -> target.outputStream().use { input.copyTo(it) } }
        val pages = ArrayList<PageDoc>()
        PdfRenderer(ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)).use { r ->
            for (i in 0 until r.pageCount) {
                r.openPage(i).use { p ->
                    // PDF points -> world units (x2 for crisper ink).
                    pages += PageDoc(UUID.randomUUID().toString(), p.width * 2f, p.height * 2f, PaperStyle.BLANK, pdfPage = i)
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

    suspend fun deleteNotebook(id: String) {
        withContext(Dispatchers.IO) { File(root, id).deleteRecursively() }
        updateIndex { it.copy(notebooks = it.notebooks.filterNot { n -> n.id == id }) }
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
        runCatching { writeThumbnail(id, pages) }
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
                    pdfPages.render(page.pdfPage, page.width.toInt(), page.height.toInt())
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
