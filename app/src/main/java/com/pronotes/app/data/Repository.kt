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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream
import java.util.UUID

@Serializable
private data class PagesManifest(
    val version: Int = 2,
    val updatedAt: Long = 0L,
    val pageIds: List<String> = emptyList(),
)

/**
 * File-based storage:
 *   files/library.json                   folders + notebook metadata
 *   files/notes/<id>/note.json           consolidated pages snapshot
 *   files/notes/<id>/pages.index.json    ordered page IDs for incremental loading
 *   files/notes/<id>/pages/<pid>.json    single-page document for fast incremental saves
 *   files/notes/<id>/source.pdf          imported PDF (optional)
 *   files/notes/<id>/thumb.png           first-page thumbnail
 *   files/notes/<id>/img_*.png           inserted images
 */
class Repository private constructor(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val root = File(context.filesDir, "notes").apply { mkdirs() }
    private val indexFile = File(context.filesDir, "library.json")
    private val lock = Mutex()
    private val saveLock = Mutex()
    private val thumbSeen = HashMap<String, Page>()

    /** Outlives screens, so a save started on exit still finishes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val isFirstInstall = !indexFile.exists()
    private val _library = MutableStateFlow(loadIndex())
    val library: StateFlow<LibraryIndex> = _library

    init {
        appScope.launch {
            runCatching {
                if (isFirstInstall && _library.value.notebooks.isEmpty()) {
                    seedPreloadedWelcomeNotebook()
                }
                purgeTrash()
            }
        }
    }

    private suspend fun seedPreloadedWelcomeNotebook() {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val page1 = PageDoc(
            id = UUID.randomUUID().toString(),
            width = PageSize.WIDTH,
            height = PageSize.HEIGHT,
            paper = PaperStyle.CORNELL,
            items = listOf(
                ItemDoc.Text(
                    text = "Welcome to Pro Notes",
                    x = 76f,
                    y = 74f,
                    size = 38f,
                    color = 0xFF1E293B.toInt(),
                    font = "SANS",
                    bold = true
                ),
                ItemDoc.Text(
                    text = "• Write Box: Tap the Text tool to handwrite in a ruled box that converts to live text, or type directly.\n" +
                        "• Keyboard Mode: Tap the Keyboard icon in the top bar for full-page rich note typing.\n" +
                        "• Eraser Filters: Choose whether to erase pen ink, highlighter, shapes, text, stickies, or images.\n" +
                        "• Multi-Format Import: Import PDF, Word (.docx), PowerPoint (.pptx), Excel/Sheets (.xlsx/.csv), and EPUB.\n" +
                        "• Reader Mode: Enjoy curved page-turn physics with optional paper-turn audio.",
                    x = 76f,
                    y = 148f,
                    size = 24f,
                    color = 0xFF334155.toInt(),
                    font = "SANS"
                ),
                ItemDoc.WriteBox(
                    x = 76f,
                    y = 430f,
                    w = 760f,
                    h = 260f,
                    lineSpacing = 48f,
                    convertedText = "Write Box — Double-tap or use the Text tool to handwrite or type here.",
                    isConverted = true,
                    font = "SANS",
                    fontSize = 24f
                ),
                ItemDoc.Sticky(
                    x = 860f,
                    y = 430f,
                    w = 300f,
                    h = 240f,
                    color = 0xFFFFF3A3.toInt(),
                    title = "Quick Tip",
                    text = "Dock the toolbar to Top, Bottom, Left, or Right — or switch between 8 UI color schemes in Settings.",
                    fontSize = 21f
                ),
                ItemDoc.MindNode(
                    nodeId = 1001L,
                    x = 180f,
                    y = 820f,
                    w = 240f,
                    h = 84f,
                    label = "Pro Notes",
                    parentId = null,
                    color = 0xFF3D6CF4.toInt(),
                    shape = "pill",
                    tag = "Core"
                ),
                ItemDoc.MindNode(
                    nodeId = 1002L,
                    x = 520f,
                    y = 760f,
                    w = 230f,
                    h = 78f,
                    label = "Write Box & Keyboard Mode",
                    parentId = 1001L,
                    color = 0xFF2BB673.toInt(),
                    shape = "rounded",
                    tag = "Input"
                ),
                ItemDoc.MindNode(
                    nodeId = 1003L,
                    x = 520f,
                    y = 880f,
                    w = 230f,
                    h = 78f,
                    label = "Reader Mode & Flashcards",
                    parentId = 1001L,
                    color = 0xFFF2994A.toInt(),
                    shape = "rounded",
                    tag = "Study"
                )
            )
        )
        val pages = listOf(page1)
        writePageDocsIncremental(id, pages, dirtyIds = null, structureChanged = true, writeFullSnapshot = true)
        updateIndex { cur ->
            if (cur.notebooks.isNotEmpty()) cur
            else cur.copy(
                notebooks = listOf(
                    NotebookMeta(
                        id = id,
                        title = "Welcome to Pro Notes",
                        folderId = null,
                        coverColor = CoverColors[0],
                        paper = PaperStyle.CORNELL,
                        created = now,
                        modified = now,
                        pageCount = pages.size,
                        favorite = true,
                        tags = listOf("Guide", "Study", "Important"),
                    )
                )
            )
        }
        runCatching {
            val studyStore = StudyStore(noteDir(id))
            val starterCards = listOf(
                Flashcard(
                    id = UUID.randomUUID().toString(),
                    front = "How do you use a Write Box?",
                    back = "Select the Text tool on the canvas, handwrite with your stylus inside the ruled box to convert strokes to text, or switch to Keyboard Mode.",
                    pageId = page1.id,
                    created = now,
                    starred = true,
                    hint = "Look at the Text tool"
                ),
                Flashcard(
                    id = UUID.randomUUID().toString(),
                    front = "What file formats can Pro Notes import?",
                    back = "PDF, Word (.docx/.doc/.odt/.rtf), PowerPoint (.pptx/.ppt/.odp), Excel/Sheets (.xlsx/.xls/.ods/.csv/.tsv), EPUB books, Markdown, Text, and Images.",
                    pageId = page1.id,
                    created = now,
                    hint = "Documents + Books + Notes"
                )
            )
            studyStore.save(StudyData(cards = starterCards))
        }
        runCatching { writeThumbnail(id, pages.map { InkCodec.fromDoc(it) }) }
    }

    private fun saveAssetBytesToNote(noteId: String, bytes: ByteArray, ext: String): String? = runCatching {
        val cleanExt = ext.lowercase().filter { it.isLetterOrDigit() }.ifEmpty { "png" }
        val name = "img_${UUID.randomUUID()}.$cleanExt"
        val target = File(noteDir(noteId), name)
        AtomicFiles.writeBytesAtomic(target) { out -> out.write(bytes) }
        name
    }.getOrNull()

    private fun loadIndex(): LibraryIndex = runCatching {
        if (indexFile.exists()) json.decodeFromString(LibraryIndex.serializer(), indexFile.readText()) else LibraryIndex()
    }.getOrElse { LibraryIndex() }

    private suspend fun updateIndex(block: (LibraryIndex) -> LibraryIndex) = withContext(Dispatchers.IO) {
        lock.withLock {
            val next = block(_library.value)
            AtomicFiles.writeTextAtomic(indexFile, json.encodeToString(LibraryIndex.serializer(), next))
            _library.value = next
        }
    }

    fun noteDir(id: String) = File(root, id).apply { mkdirs() }
    private fun pagesDir(id: String) = File(noteDir(id), "pages").apply { mkdirs() }
    private fun manifestFile(id: String) = File(noteDir(id), "pages.index.json")
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

    suspend fun setNotebookTags(id: String, tags: List<String>) {
        val cleaned = tags.map { it.trim().removePrefix("#").trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(12)
        updateIndex { idx ->
            idx.copy(
                notebooks = idx.notebooks.map { n ->
                    if (n.id == id) n.copy(tags = cleaned, modified = System.currentTimeMillis()) else n
                },
            )
        }
    }

    suspend fun markOpened(id: String) = updateIndex {
        it.copy(notebooks = it.notebooks.map { n -> if (n.id == id) n.copy(opened = System.currentTimeMillis()) else n })
    }

    /** Copies a picked photo into the notebook folder atomically and returns its cover spec. */
    suspend fun saveCoverImage(id: String, uri: Uri): String = withContext(Dispatchers.IO) {
        val name = "cover_${System.currentTimeMillis()}.img"
        val target = File(noteDir(id), name)
        (context.contentResolver.openInputStream(uri) ?: error("Can't open file")).use { input ->
            AtomicFiles.writeBytesAtomic(target) { out -> input.copyTo(out) }
        }
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
        withContext(Dispatchers.IO) {
            writePageDocsIncremental(id, pages, dirtyIds = null, structureChanged = true, writeFullSnapshot = true)
        }
        updateIndex {
            it.copy(notebooks = it.notebooks + NotebookMeta(id, title, folderId, cover, paper, now, now, pages.size, cover = if (coverPage && !isWhiteboard) "thumb" else coverSpec, isWhiteboard = isWhiteboard))
        }
        if (coverPage && !isWhiteboard) withContext(Dispatchers.IO) { runCatching { writeThumbnail(id, pages.map { InkCodec.fromDoc(it) }) } }
        return id
    }

    /** Changes the design of a notebook's cover page, if it has one. */
    suspend fun setCoverPage(id: String, spec: String): Boolean = withContext(Dispatchers.IO) {
        val docs = loadPageDocsSync(id) ?: return@withContext false
        val first = docs.firstOrNull() ?: return@withContext false
        if (first.cover == null) return@withContext false
        val updatedFirst = first.copy(cover = spec)
        val nextPages = listOf(updatedFirst) + docs.drop(1)
        writePageDocsIncremental(id, nextPages, dirtyIds = setOf(updatedFirst.id), structureChanged = false, writeFullSnapshot = true)
        runCatching { writeThumbnail(id, nextPages.map { InkCodec.fromDoc(it) }) }
        updateIndex { it.copy(notebooks = it.notebooks.map { n -> if (n.id == id) n.copy(modified = System.currentTimeMillis(), cover = "thumb") else n }) }
        true
    }

    suspend fun importPdf(
        uri: Uri,
        folderId: String?,
        onProgress: ((title: String, page: Int, total: Int) -> Unit)? = null,
    ): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val target = pdfFile(id)
        val title = displayName(uri)?.removeSuffix(".pdf")?.removeSuffix(".PDF") ?: "Imported PDF"
        onProgress?.invoke(title, 0, 0)
        try {
            (context.contentResolver.openInputStream(uri) ?: error("Can't open file")).use { input ->
                AtomicFiles.writeBytesAtomic(target) { out -> input.copyTo(out) }
            }
            val pages = ArrayList<PageDoc>()
            PdfRenderer(ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)).use { r ->
                val total = r.pageCount.coerceAtLeast(1)
                onProgress?.invoke(title, 0, total)
                for (i in 0 until r.pageCount) {
                    r.openPage(i).use { p ->
                        // Scale so the shorter side is about one notebook page wide (landscape slides and
                        // small PDFs get the same pen feel as an A4 page).
                        val k = (PageSize.WIDTH / minOf(p.width, p.height).toFloat()).coerceIn(1f, 4f)
                        pages += PageDoc(UUID.randomUUID().toString(), p.width * k, p.height * k, PaperStyle.BLANK, pdfPage = i)
                    }
                    if ((i + 1) % 2 == 0 || i == r.pageCount - 1) {
                        onProgress?.invoke(title, i + 1, total)
                    }
                }
            }
            if (pages.isEmpty()) error("PDF has no readable pages")
            writePageDocsIncremental(id, pages, dirtyIds = null, structureChanged = true, writeFullSnapshot = true)
            val now = System.currentTimeMillis()
            updateIndex {
                it.copy(notebooks = it.notebooks + NotebookMeta(id, title, folderId, CoverColors[0], PaperStyle.BLANK, now, now, pages.size, fromPdf = true))
            }
            onProgress?.invoke(title, pages.size, pages.size)
            runCatching { writeThumbnail(id, pages.map { InkCodec.fromDoc(it) }) }
            id
        } catch (t: Throwable) {
            runCatching { target.delete() }
            runCatching {
                val dir = noteDir(id)
                if (dir.exists()) dir.deleteRecursively()
            }
            throw t
        }
    }

    /** Imports PDFs, Word, PowerPoint, Excel/Sheets, EPUB, images, Markdown/TXT/HTML/CSV files, or JSON note exports into a new notebook. */
    suspend fun importFiles(
        uris: List<Uri>,
        folderId: String?,
        onProgress: ((title: String, page: Int, total: Int) -> Unit)? = null,
    ): String? = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext null
        val first = uris.first()
        val firstName = displayName(first) ?: "Imported Note"
        val firstMime = context.contentResolver.getType(first)?.lowercase().orEmpty()
        if (uris.size == 1 && (firstMime == "application/pdf" || firstName.endsWith(".pdf", true))) {
            return@withContext importPdf(first, folderId, onProgress)
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
                    items = listOf(ItemDoc.Image(imgName, x, y, w, h)),
                )
            }
            if (imageUris.size > 1) importedTitle = "${firstName.substringBeforeLast('.')} (+${imageUris.size - 1})"
        } else {
            for ((idx, u) in uris.withIndex()) {
                val name = displayName(u) ?: "Imported"
                val mime = context.contentResolver.getType(u)?.lowercase().orEmpty()
                onProgress?.invoke(importedTitle, idx, uris.size)
                when {
                    mime == "application/pdf" || name.endsWith(".pdf", true) -> {
                        val subId = importPdf(u, folderId, onProgress)
                        if (uris.size == 1) return@withContext subId
                        val subPages = runCatching { json.decodeFromString(NoteDoc.serializer(), File(noteDir(subId), "note.json").readText()).pages }.getOrDefault(emptyList())
                        if (subPages.isNotEmpty()) return@withContext subId
                    }
                    OfficeDocumentImporter.isSupportedFile(name, mime) -> {
                        val importedPages = OfficeDocumentImporter.importDocumentPages(
                            context = context,
                            uri = u,
                            displayName = name,
                            mimeType = mime,
                            saveAssetBytes = { bytes, ext -> saveAssetBytesToNote(id, bytes, ext) }
                        )
                        pageDocs += importedPages
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
                            items = listOf(ItemDoc.Image(imgName, (PageSize.WIDTH - bw * sc) / 2f, (PageSize.HEIGHT - bh * sc) / 2f, bw * sc, bh * sc)),
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
        val primaryPaper = pageDocs.firstOrNull()?.paper ?: PaperStyle.RULED
        writePageDocsIncremental(id, pageDocs, dirtyIds = null, structureChanged = true, writeFullSnapshot = true)
        val now = System.currentTimeMillis()
        updateIndex {
            it.copy(notebooks = it.notebooks + NotebookMeta(id, importedTitle, folderId, coverColor, primaryPaper, now, now, pageDocs.size))
        }
        onProgress?.invoke(importedTitle, pageDocs.size, pageDocs.size)
        runCatching { writeThumbnail(id, pageDocs.map { InkCodec.fromDoc(it) }) }
        id
    }

    /** Creates a paginated notebook from pasted notes, Markdown, HTML, or plain text. */
    suspend fun importStructuredText(title: String, rawText: String, folderId: String?): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val cleanTitle = title.trim().ifBlank { deriveTitleFromText(rawText) }
        val normalized = normalizeImportedText(rawText, "$cleanTitle.md")
        val pageDocs = textToPageDocs(cleanTitle, normalized).ifEmpty {
            listOf(PageDoc(UUID.randomUUID().toString(), PageSize.WIDTH, PageSize.HEIGHT, PaperStyle.RULED))
        }
        writePageDocsIncremental(id, pageDocs, dirtyIds = null, structureChanged = true, writeFullSnapshot = true)
        val now = System.currentTimeMillis()
        updateIndex {
            it.copy(notebooks = it.notebooks + NotebookMeta(id, cleanTitle, folderId, CoverColors[1], PaperStyle.RULED, now, now, pageDocs.size))
        }
        runCatching { writeThumbnail(id, pageDocs.map { InkCodec.fromDoc(it) }) }
        id
    }

    /** Builds new pages from files/images/Word/PPT/Excel/EPUB to append into an existing notebook. */
    suspend fun buildPagesFromUris(noteId: String, uris: List<Uri>): List<Page> = withContext(Dispatchers.IO) {
        val out = ArrayList<PageDoc>()
        for (u in uris) {
            val name = displayName(u) ?: "Imported"
            val mime = context.contentResolver.getType(u)?.lowercase().orEmpty()
            if (OfficeDocumentImporter.isSupportedFile(name, mime)) {
                out += OfficeDocumentImporter.importDocumentPages(
                    context = context,
                    uri = u,
                    displayName = name,
                    mimeType = mime,
                    saveAssetBytes = { bytes, ext -> saveAssetBytesToNote(noteId, bytes, ext) }
                )
            } else if (mime.startsWith("image/") || name.endsWith(".png", true) || name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".webp", true)) {
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
                    items = listOf(ItemDoc.Image(imgName, (PageSize.WIDTH - bw * sc) / 2f, (PageSize.HEIGHT - bh * sc) / 2f, bw * sc, bh * sc)),
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
            curItems += ItemDoc.Text(sectionTitle, marginX, y, 36f, 0xFF1B1F2A.toInt(), bold = true)
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
                curItems += ItemDoc.Sticky(marginX, y, PageSize.WIDTH - marginX * 2, h, 0xFFFFF3A3.toInt(), clean, fontSize = 23f)
                y += h + 28f
                continue
            }
            val lines = clean.lines()
            val estHeight = (lines.sumOf { ((it.length / 46) + 1) } * if (isHeading) 42 else 34).toFloat().coerceAtLeast(36f)
            if (y + estHeight > maxY && curItems.isNotEmpty()) flushPage()
            curItems += ItemDoc.Text(
                text = clean,
                x = marginX,
                y = y,
                size = if (isHeading) 30f else 24f,
                color = if (isHeading) 0xFF2855C8.toInt() else 0xFF1B1F2A.toInt(),
                bold = isHeading,
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

    private fun loadPageDocsSync(id: String): List<PageDoc>? {
        val manifestF = manifestFile(id)
        if (manifestF.exists()) {
            val manifest = runCatching {
                json.decodeFromString(PagesManifest.serializer(), manifestF.readText())
            }.getOrNull()
            if (manifest != null && manifest.pageIds.isNotEmpty()) {
                val pDir = pagesDir(id)
                val loaded = ArrayList<PageDoc>(manifest.pageIds.size)
                var complete = true
                for (pid in manifest.pageIds) {
                    val pf = File(pDir, "$pid.json")
                    val pd = if (pf.exists()) {
                        runCatching { json.decodeFromString(PageDoc.serializer(), pf.readText()) }.getOrNull()
                    } else null
                    if (pd == null) {
                        complete = false
                        break
                    }
                    loaded += pd
                }
                if (complete) return loaded
            }
        }
        val legacyFile = File(noteDir(id), "note.json")
        if (!legacyFile.exists()) return null
        return runCatching { json.decodeFromString(NoteDoc.serializer(), legacyFile.readText()).pages }.getOrNull()
    }

    private fun writePageDocsIncremental(
        id: String,
        docs: List<PageDoc>,
        dirtyIds: Set<String>?,
        structureChanged: Boolean,
        writeFullSnapshot: Boolean,
    ) {
        val pDir = pagesDir(id)
        val mf = manifestFile(id)
        val writeAllPages = dirtyIds == null || !mf.exists()
        for (doc in docs) {
            val pf = File(pDir, "${doc.id}.json")
            if (writeAllPages || doc.id in dirtyIds || !pf.exists()) {
                AtomicFiles.writeTextAtomic(pf, json.encodeToString(PageDoc.serializer(), doc))
            }
        }
        if (structureChanged || !mf.exists()) {
            val keepIds = docs.map { it.id }.toSet()
            AtomicFiles.writeTextAtomic(
                mf,
                json.encodeToString(
                    PagesManifest.serializer(),
                    PagesManifest(updatedAt = System.currentTimeMillis(), pageIds = docs.map { it.id }),
                ),
            )
            pDir.listFiles()?.forEach { child ->
                if (child.isFile && child.name.endsWith(".json")) {
                    val pid = child.name.removeSuffix(".json")
                    if (pid !in keepIds) child.delete()
                }
            }
        }
        val noteFile = File(noteDir(id), "note.json")
        if (writeFullSnapshot || !noteFile.exists()) {
            AtomicFiles.writeTextAtomic(noteFile, json.encodeToString(NoteDoc.serializer(), NoteDoc(pages = docs)))
        }
    }

    /** Returns up-to-date serialized library index JSON for storage mirroring/sync. */
    fun readSerializedLibraryIndex(): String =
        json.encodeToString(LibraryIndex.serializer(), _library.value)

    /** Returns up-to-date serialized NoteDoc JSON for sync/export without stale snapshot drift. */
    fun readSerializedNoteDoc(id: String): String {
        val docs = loadPageDocsSync(id)
        return if (docs != null) {
            json.encodeToString(NoteDoc.serializer(), NoteDoc(pages = docs))
        } else {
            val f = File(noteDir(id), "note.json")
            if (f.exists()) f.readText() else "{}"
        }
    }

    /** Atomically imports a serialized NoteDoc JSON from cloud sync into both snapshot & per-page storage. */
    fun writeSerializedNoteDoc(id: String, rawJson: String) {
        val target = File(noteDir(id), "note.json")
        AtomicFiles.writeTextAtomic(target, rawJson)
        val parsed = runCatching { json.decodeFromString(NoteDoc.serializer(), rawJson) }.getOrNull()
        if (parsed != null && parsed.pages.isNotEmpty()) {
            writePageDocsIncremental(id, parsed.pages, dirtyIds = null, structureChanged = true, writeFullSnapshot = false)
        }
    }

    suspend fun loadPages(id: String): List<Page> = withContext(Dispatchers.IO) {
        val docs = loadPageDocsSync(id)
        docs?.map { InkCodec.fromDoc(it) }
            ?: listOf(Page(UUID.randomUUID().toString(), PageSize.WIDTH, PageSize.HEIGHT, PaperStyle.RULED))
    }

    suspend fun savePages(
        id: String,
        pages: List<Page>,
        dirtyPageIds: Set<String>? = null,
        structureChanged: Boolean = true,
        writeFullSnapshot: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        saveLock.withLock {
            val pDir = pagesDir(id)
            val mf = manifestFile(id)
            val needFullManifest = structureChanged || !mf.exists()
            val idsToWrite = dirtyPageIds
            val docs = if (writeFullSnapshot || idsToWrite == null || !File(noteDir(id), "note.json").exists()) {
                pages.map { InkCodec.toDoc(it) }
            } else {
                // Only serialize pages that actually changed when performing an incremental debounced write.
                val manifestOnly = PagesManifest(updatedAt = System.currentTimeMillis(), pageIds = pages.map { it.id })
                for (page in pages) {
                    val pf = File(pDir, "${page.id}.json")
                    if (page.id in idsToWrite || !pf.exists()) {
                        AtomicFiles.writeTextAtomic(pf, json.encodeToString(PageDoc.serializer(), InkCodec.toDoc(page)))
                    }
                }
                if (needFullManifest) {
                    val keep = pages.map { it.id }.toSet()
                    AtomicFiles.writeTextAtomic(mf, json.encodeToString(PagesManifest.serializer(), manifestOnly))
                    pDir.listFiles()?.forEach { f ->
                        if (f.isFile && f.name.endsWith(".json") && f.name.removeSuffix(".json") !in keep) f.delete()
                    }
                }
                null
            }
            if (docs != null) {
                writePageDocsIncremental(id, docs, dirtyIds = idsToWrite, structureChanged = needFullManifest, writeFullSnapshot = true)
            }
        }
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
        val treeUri = SettingsStore.get(context).settings.value.storageFolderUri
        if (treeUri.isNotBlank()) {
            val title = meta(id)?.title ?: "Notebook"
            val libJson = readSerializedLibraryIndex()
            val noteJson = readSerializedNoteDoc(id)
            appScope.launch {
                StorageFolderSync.mirrorNotebook(
                    context = context,
                    treeUriStr = treeUri,
                    libraryJson = libJson,
                    noteId = id,
                    noteTitle = title,
                    noteJson = noteJson,
                )
            }
        }
    }

    private fun writeThumbnail(id: String, pages: List<Page>) {
        val page = pages.firstOrNull() ?: return
        val targetWidth = SettingsStore.get(context).settings.value.performanceMode.thumbWidthPx.toFloat()
        val scale = (targetWidth / page.width).coerceAtLeast(0.05f)
        val w = (page.width * scale).toInt().coerceAtLeast(1)
        val h = (page.height * scale).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        var pdf: Bitmap? = null
        try {
            pdf = if (page.pdfPage >= 0) {
                val pp = PdfPages(pdfFile(id))
                try {
                    pp.render(page.pdfPage, w, h, useCache = false)
                } finally {
                    pp.close()
                }
            } else null
            // Render the PDF bitmap at thumbnail size, so draw it with scale 1.
            val canvas = Canvas(bmp)
            PageRenderer(noteDir(id)).renderPage(canvas, page, scale, pdf)
            AtomicFiles.writeBytesAtomic(thumbFile(id)) { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        } finally {
            pdf?.recycle()
            bmp.recycle()
        }
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
                try {
                    renderer.renderPage(pdfPage.canvas, page, 0.5f, bg)
                    doc.finishPage(pdfPage)
                } finally {
                    bg?.recycle()
                }
            }
            doc.writeTo(out)
        } finally {
            doc.close()
            pdfPages.close()
        }
    }

    suspend fun saveImage(noteId: String, uri: Uri): String = withContext(Dispatchers.IO) {
        val name = "img_${UUID.randomUUID()}.img"
        val target = File(noteDir(noteId), name)
        (context.contentResolver.openInputStream(uri) ?: error("Can't open file")).use { input ->
            AtomicFiles.writeBytesAtomic(target) { out -> input.copyTo(out) }
        }
        name
    }

    companion object {
        @Volatile private var instance: Repository? = null
        fun get(context: Context): Repository =
            instance ?: synchronized(this) { instance ?: Repository(context.applicationContext).also { instance = it } }
    }
}
