package com.pronotes.app.editor

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.lifecycle.viewModelScope
import com.pronotes.app.data.Flashcard
import com.pronotes.app.data.PaperStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

fun EditorViewModel.saveSelectionScreenshot() {
    viewModelScope.launch {
        val bmp = selectionImage()
        if (bmp == null) {
            say("Nothing selected to save")
            return@launch
        }
        val saved = withContext(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val resolver = app.contentResolver
                val name = "ProNotes_${System.currentTimeMillis()}.png"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ProNotes")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                val stream = resolver.openOutputStream(uri)
                if (stream == null) {
                    runCatching { resolver.delete(uri, null, null) }
                    return@withContext false
                }
                stream.use { out ->
                    if (!bmp.compress(Bitmap.CompressFormat.PNG, 95, out)) {
                        runCatching { resolver.delete(uri, null, null) }
                        return@withContext false
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                true
            } catch (_: Throwable) {
                false
            } finally {
                bmp.recycle()
            }
        }
        say(if (saved) "Saved selection to Pictures/ProNotes" else "Could not save selection screenshot")
    }
}

// --------------------------------------------------------------- PDF text selection

internal fun EditorViewModel.indexFor(page: Int): PdfTextIndex? {
    val p = pages.getOrNull(page) ?: return null
    if (p.pdfPage < 0) return null
    pdfIndex?.let { return it }
    pageTextIdx[p.pdfPage]?.let { return it }
    if (pageTextJob?.isActive != true) {
        pdfTextBusy = true
        pageTextJob = viewModelScope.launch {
            val built: PdfTextIndex? = withContext(Dispatchers.IO) {
                runCatching {
                    PdfTextIndex.build(
                        context = getApplication(),
                        file = repo.pdfFile(noteId),
                        only = setOf(p.pdfPage),
                    ) { p.width }
                }.getOrNull()
            }
            if (built != null) pageTextIdx[p.pdfPage] = built
            pdfTextBusy = false
            onRedraw?.invoke()
        }
    }
    return null
}

fun EditorViewModel.textPress(page: Int, x: Float, y: Float): Boolean {
    val p = pages.getOrNull(page) ?: return false
    if (p.pdfPage < 0) return false
    val idx = indexFor(page) ?: return false
    val charIdx = idx.charAt(p.pdfPage, x, y) ?: return false
    val word = idx.wordAt(p.pdfPage, charIdx)
    val rects = idx.rects(p.pdfPage, word)
    val text = idx.text(p.pdfPage, word)
    if (rects.isEmpty() || text.isBlank()) return false
    textSel = EditorViewModel.TextSel(page, p.pdfPage, word, word, rects, text)
    textMenu = null
    return true
}

fun EditorViewModel.textDrag(x: Float, y: Float) {
    val cur = textSel ?: return
    val idx = indexFor(cur.page) ?: return
    val hit = idx.charAt(cur.pdfPage, x, y) ?: return
    val lo = minOf(cur.anchor.first, hit)
    val hi = maxOf(cur.anchor.last, hit)
    val range = lo..hi
    if (range == cur.range) return
    textSel = cur.copy(
        range = range,
        rects = idx.rects(cur.pdfPage, range),
        text = idx.text(cur.pdfPage, range),
    )
}

fun EditorViewModel.textEnd(screen: RectF?) {
    textMenu = screen
}

fun EditorViewModel.clearTextSel() {
    if (textSel == null && textMenu == null) return
    textSel = null
    textMenu = null
    onRedraw?.invoke()
}

fun EditorViewModel.textSelection(page: Int): List<RectF> =
    textSel?.takeIf { it.page == page }?.rects.orEmpty()

fun EditorViewModel.highlightTextSel(color: Int? = null) {
    val sel = textSel ?: return
    val p = pages.getOrNull(sel.page) ?: return
    val c = color ?: highlighterStyle.color
    val added = sel.rects.mapNotNull { r ->
        val h = r.height().coerceIn(8f, 72f)
        val brush = Brush.createWithColorIntArgb(StockBrushes.highlighter(), c, h, 0.05f)
        val batch = MutableStrokeInputBatch().apply {
            add(androidx.ink.strokes.StrokeInput.create(r.left, r.centerY(), 0L))
            add(androidx.ink.strokes.StrokeInput.create(r.right, r.centerY(), 40L))
        }
        runCatching { InkItem(ItemIds.next(), Stroke(brush, batch)) }.getOrNull()
    }
    if (added.isNotEmpty()) {
        beginEdit(sel.page)
        applyItems(sel.page, p.items + added)
    }
    clearTextSel()
}

fun EditorViewModel.lineTextSel(strike: Boolean) {
    val sel = textSel ?: return
    val p = pages.getOrNull(sel.page) ?: return
    val color = if (strike) 0xFFD93025.toInt() else penStyle.color
    val added = sel.rects.mapNotNull { r ->
        val y = if (strike) r.centerY() else r.bottom - 1.5f
        val brush = Brush.createWithColorIntArgb(StockBrushes.marker(), color, 3f, 0.05f)
        val batch = MutableStrokeInputBatch().apply {
            add(androidx.ink.strokes.StrokeInput.create(r.left, y, 0L))
            add(androidx.ink.strokes.StrokeInput.create(r.right, y, 30L))
        }
        runCatching { InkItem(ItemIds.next(), Stroke(brush, batch)) }.getOrNull()
    }
    if (added.isNotEmpty()) {
        beginEdit(sel.page)
        applyItems(sel.page, p.items + added)
    }
    clearTextSel()
}

fun EditorViewModel.cardFromTextSel() {
    val sel = textSel ?: return
    val p = pages.getOrNull(sel.page) ?: return
    val text = sel.text.trim()
    clearTextSel()
    if (text.isBlank()) return
    cardDraft = EditorViewModel.CardDraft(
        Flashcard(
            id = UUID.randomUUID().toString(),
            front = text,
            pageId = p.id,
            created = System.currentTimeMillis(),
        ),
        isNew = true,
    )
}

internal suspend fun EditorViewModel.studyText(range: IntRange): Pair<String, String> = withContext(Dispatchers.Default) {
    val sb = StringBuilder()
    val lo = range.first.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val hi = range.last.coerceIn(lo, (pages.size - 1).coerceAtLeast(0))
    val idx = if (isPdf) textIndex() else null
    for (pi in lo..hi) {
        val p = pages.getOrNull(pi) ?: continue
        val pdfText = if (p.pdfPage >= 0 && idx != null) idx.pageText(p.pdfPage) else ""
        val noteText = extractPageSummaryText(pi)
        val combined = listOf(pdfText, noteText).filter { it.isNotBlank() }.joinToString("\n")
        if (combined.isNotBlank()) {
            sb.appendLine("--- Page ${pi + 1} ---")
            sb.appendLine(combined)
        }
    }
    title to sb.toString().trim()
}

fun EditorViewModel.clipTextAsSticky(snippet: String, sourceTitle: String = "") {
    val clean = snippet.trim().take(1600)
    if (clean.isBlank()) return
    val idx = pageIndex
    val p = pages.getOrNull(idx) ?: return
    beginEdit(idx)
    val sticky = StickyItem(
        id = ItemIds.next(),
        text = clean,
        x = (p.width * 0.14f).coerceAtLeast(64f),
        y = (p.height * 0.16f).coerceAtLeast(80f),
        w = 340f,
        h = 260f,
        color = stickyColor,
        title = sourceTitle.take(48),
    )
    applyItems(idx, p.items + sticky)
    say("Clipped to sticky note")
}

fun EditorViewModel.clipTextAsPageItem(snippet: String, sourceTitle: String = "") {
    val clean = buildString {
        if (sourceTitle.isNotBlank()) appendLine(sourceTitle.trim())
        append(snippet.trim())
    }.trim().take(2400)
    if (clean.isBlank()) return
    insertTextToPage(clean)
}

fun EditorViewModel.importFilesIntoCurrent(uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
        var addedPages = 0
        var addedImages = 0
        var failed = 0
        for (uri in uris) {
            val mime = runCatching { getApplication<Application>().contentResolver.getType(uri) }.getOrNull().orEmpty()
            val name = uri.lastPathSegment.orEmpty().lowercase()
            when {
                mime.startsWith("image/") || name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp") -> {
                    val ok = runCatching {
                        insertImage(uri) { _, _ -> }.join()
                    }.isSuccess
                    if (ok) addedImages++ else failed++
                }
                mime == "application/pdf" || name.endsWith(".pdf") -> {
                    val res = runCatching {
                        withContext(Dispatchers.IO) {
                            val tmp = File(getApplication<Application>().cacheDir, "imp_${System.currentTimeMillis()}.pdf")
                            try {
                                val input = getApplication<Application>().contentResolver.openInputStream(uri)
                                    ?: error("Cannot open PDF")
                                input.use { src -> tmp.outputStream().use { dst -> src.copyTo(dst) } }
                                val importedPages = ArrayList<Page>()
                                ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                                    PdfRenderer(pfd).use { doc ->
                                        for (i in 0 until doc.pageCount) {
                                            doc.openPage(i).use { pdfPage ->
                                                val pw = 1240
                                                val ph = (pw * pdfPage.height / pdfPage.width.coerceAtLeast(1)).coerceIn(400, 2600)
                                                val bmp = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888)
                                                bmp.eraseColor(Color.WHITE)
                                                val imgName = "pdf_${UUID.randomUUID().toString().take(8)}.png"
                                                val outFile = File(repo.noteDir(noteId), imgName)
                                                try {
                                                    pdfPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                                    outFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
                                                } finally {
                                                    bmp.recycle()
                                                }
                                                val w = 1000f
                                                val h = (1000f * ph / pw.toFloat()).coerceIn(600f, 2200f)
                                                importedPages += Page(
                                                    id = UUID.randomUUID().toString(),
                                                    width = w,
                                                    height = h,
                                                    paper = PaperStyle.BLANK,
                                                    items = listOf(ImageItem(ItemIds.next(), imgName, 0f, 0f, w, h)),
                                                )
                                            }
                                        }
                                    }
                                }
                                importedPages
                            } finally {
                                tmp.delete()
                            }
                        }
                    }
                    res.onSuccess { newPages ->
                        if (newPages.isNotEmpty()) {
                            pages.addAll(newPages)
                            structureDirty = true
                            addedPages += newPages.size
                        }
                    }.onFailure {
                        failed++
                    }
                }
                else -> {
                    val text = runCatching {
                        withContext(Dispatchers.IO) {
                            getApplication<Application>().contentResolver.openInputStream(uri)
                                ?.bufferedReader()
                                ?.use { it.readText() }
                        }
                    }.getOrNull()
                    if (!text.isNullOrBlank()) {
                        insertTextToPage(text)
                        addedImages++
                    } else {
                        failed++
                    }
                }
            }
        }
        if (addedPages > 0) {
            scheduleSave()
            onPagesChanged?.invoke()
        }
        val msg = when {
            addedPages > 0 && failed == 0 -> "Imported $addedPages page${if (addedPages == 1) "" else "s"}"
            addedImages > 0 && failed == 0 -> "Imported $addedImages file${if (addedImages == 1) "" else "s"}"
            addedPages + addedImages > 0 -> "Imported with $failed skipped file${if (failed == 1) "" else "s"}"
            else -> "Could not import selected file"
        }
        say(msg)
    }
}

// --------------------------------------------------------------- write boxes & keyboard mode

suspend fun EditorViewModel.recognizeHandwritingPad(
    strokes: List<List<TimedStrokePoint>>,
    width: Float = 0f,
    height: Float = 0f,
    preContext: String = "",
): HandwritingRecognitionResult = HandwritingRecognizer.recognize(
    strokes = strokes,
    writingWidth = width,
    writingHeight = height,
    preContext = preContext,
    languageTag = settingsFlow.value.toolDefaults.handwritingLanguage,
    allowFallback = true,
)

fun EditorViewModel.activeWriteBox(): WriteBoxItem? {
    val id = activeWriteBoxId ?: return null
    val p = pages.getOrNull(activeWriteBoxPage) ?: return null
    return p.items.filterIsInstance<WriteBoxItem>().firstOrNull { it.id == id }
}

fun EditorViewModel.selectWriteBox(page: Int, boxId: Long?) {
    activeWriteBoxPage = page
    activeWriteBoxId = boxId
}

fun EditorViewModel.placeWriteBox(page: Int, x: Float, y: Float, w: Float, h: Float) {
    val p = pages.getOrNull(page) ?: return
    val width = w.coerceIn(220f, (p.width - 48f).coerceAtLeast(220f))
    val height = h.coerceIn(110f, (p.height - 48f).coerceAtLeast(110f))
    val nx = x.coerceIn(24f, (p.width - width - 24f).coerceAtLeast(24f))
    val ny = y.coerceIn(24f, (p.height - height - 24f).coerceAtLeast(24f))
    val box = WriteBoxItem(
        id = ItemIds.next(),
        x = nx,
        y = ny,
        w = width,
        h = height,
        fontSize = textSize.coerceIn(18f, 64f),
        font = textStyle.font,
    )
    beginEdit(page)
    applyItems(page, p.items + box)
    activeWriteBoxPage = page
    activeWriteBoxId = box.id
    say("Write inside the box to convert to text")
}

fun EditorViewModel.addWriteBox(page: Int = pageIndex, x: Float? = null, y: Float? = null) {
    val p = pages.getOrNull(page) ?: return
    val w = (p.width * 0.72f).coerceIn(320f, 760f)
    val h = 210f
    placeWriteBox(page, x ?: ((p.width - w) / 2f), y ?: (p.height * 0.22f), w, h)
}

fun EditorViewModel.selectedWriteBox(): WriteBoxItem? {
    val p = pages.getOrNull(selectionPage) ?: return null
    return p.items.filterIsInstance<WriteBoxItem>().firstOrNull { it.id in selection }
}

fun EditorViewModel.autoConvertWriteBox(page: Int, boxId: Long) {
    val p = pages.getOrNull(page) ?: return
    val box = p.items.filterIsInstance<WriteBoxItem>().firstOrNull { it.id == boxId } ?: return
    val insideInk = p.items.filterIsInstance<InkItem>().filter { ink ->
        !ink.isHighlighter && (RectF.intersects(box.raw, ink.bounds) || box.raw.contains(ink.bounds.centerX(), ink.bounds.centerY()))
    }
    if (insideInk.isEmpty()) return
    val timedStrokes = extractTimedStrokes(insideInk)
    viewModelScope.launch {
        val result = runCatching {
            HandwritingRecognizer.recognize(
                strokes = timedStrokes,
                writingWidth = box.w,
                writingHeight = box.h,
                preContext = box.convertedText.takeLast(24),
                languageTag = settingsFlow.value.toolDefaults.handwritingLanguage,
                allowFallback = true,
            )
        }.getOrNull() ?: return@launch
        val recognized = result.text.trim()
        if (recognized.isBlank()) return@launch
        val freshPage = pages.getOrNull(page) ?: return@launch
        val freshBox = freshPage.items.filterIsInstance<WriteBoxItem>().firstOrNull { it.id == boxId } ?: return@launch
        val inkIds = insideInk.map { it.id }.toSet()
        val combined = if (freshBox.convertedText.isBlank()) {
            recognized
        } else {
            "${freshBox.convertedText.trimEnd()} $recognized"
        }
        val updated = freshBox.copy(
            convertedText = combined,
            isConverted = true,
        )
        beginEdit(page)
        applyItems(
            page,
            freshPage.items
                .filterNot { it.id in inkIds }
                .map { if (it.id == boxId) updated else it },
        )
        activeWriteBoxPage = page
        activeWriteBoxId = boxId
    }
}

fun EditorViewModel.updateWriteBoxText(
    page: Int,
    boxId: Long,
    text: String,
    font: TextFont? = null,
    fontSize: Float? = null,
) {
    val p = pages.getOrNull(page) ?: return
    if (p.items.none { it is WriteBoxItem && it.id == boxId }) return
    beginEdit(page)
    applyItems(
        page,
        p.items.map { item ->
            if (item is WriteBoxItem && item.id == boxId) {
                item.copy(
                    convertedText = text,
                    font = font ?: item.font,
                    fontSize = (fontSize ?: item.fontSize).coerceIn(14f, 84f),
                    isConverted = true,
                )
            } else {
                item
            }
        },
    )
}

fun EditorViewModel.flattenWriteBoxToText(page: Int, boxId: Long) {
    val p = pages.getOrNull(page) ?: return
    val box = p.items.filterIsInstance<WriteBoxItem>().firstOrNull { it.id == boxId } ?: return
    val clean = box.convertedText.trim()
    beginEdit(page)
    val next = if (clean.isEmpty()) {
        p.items.filterNot { it.id == boxId }
    } else {
        val textItem = TextItem(
            id = ItemIds.next(),
            text = clean,
            x = box.x + 12f,
            y = box.y + 12f,
            size = box.fontSize,
            color = textColor,
            style = textStyle.copy(font = box.font),
        )
        p.items.map { if (it.id == boxId) textItem else it }
    }
    applyItems(page, next)
    if (activeWriteBoxId == boxId) activeWriteBoxId = null
}

fun EditorViewModel.deleteWriteBox(page: Int, boxId: Long) {
    val p = pages.getOrNull(page) ?: return
    beginEdit(page)
    applyItems(page, p.items.filterNot { it.id == boxId })
    if (activeWriteBoxId == boxId) activeWriteBoxId = null
}

fun EditorViewModel.toggleWriteBox(page: Int, item: WriteBoxItem) {
    val p = pages.getOrNull(page) ?: return
    beginEdit(page)
    applyItems(
        page,
        p.items.map { cur ->
            if (cur is WriteBoxItem && cur.id == item.id) cur.copy(isConverted = !cur.isConverted) else cur
        },
    )
    activeWriteBoxPage = page
    activeWriteBoxId = item.id
}

fun EditorViewModel.toggleKeyboardMode() {
    if (keyboardMode) {
        onRequestCommitText?.invoke()
        if (textEdit?.isFullPage == true) textEdit = null
        keyboardMode = false
    } else {
        onSelection(-1, emptySet(), null)
        activeWriteBoxId = null
        val p = pages.getOrNull(pageIndex) ?: return
        val existing = p.items.filterIsInstance<TextItem>().firstOrNull()
        keyboardMode = true
        openInlineTextBox(
            page = pageIndex,
            x = existing?.x ?: 52f,
            y = existing?.y ?: 68f,
            existing = existing,
            boxWidth = (p.width - 104f).coerceAtLeast(360f),
            boxHeight = (p.height - 140f).coerceAtLeast(320f),
            isFullPage = true,
        )
    }
}

fun EditorViewModel.getKeyboardPageText(page: Int = pageIndex): String {
    val p = pages.getOrNull(page) ?: return ""
    return p.items.filterIsInstance<TextItem>().joinToString("\n\n") { it.text }
}

fun EditorViewModel.saveKeyboardPageText(
    text: String,
    fontSize: Float = textSize,
    color: Int = textColor,
    style: TextStyleSpec = textStyle,
) {
    val idx = pageIndex
    val p = pages.getOrNull(idx) ?: return
    val existing = p.items.filterIsInstance<TextItem>().firstOrNull()
    val clean = text.trimEnd()
    if (existing == null && clean.isEmpty()) return
    if (existing != null && existing.text == clean && existing.size == fontSize && existing.color == color && existing.style == style) return
    beginEdit(idx)
    val nextItems = when {
        clean.isEmpty() -> p.items.filterNot { it is TextItem && it.id == existing?.id }
        existing != null -> p.items.map {
            if (it is TextItem && it.id == existing.id) {
                it.copy(
                    text = clean,
                    size = fontSize,
                    color = color,
                    style = style,
                )
            } else {
                it
            }
        }
        else -> p.items + TextItem(
            id = ItemIds.next(),
            text = clean,
            x = 48f,
            y = 56f,
            size = fontSize,
            color = color,
            style = style,
        )
    }
    applyItems(idx, nextItems)
}

fun EditorViewModel.clearPageAll() {
    val idx = pageIndex
    val p = pages.getOrNull(idx) ?: return
    if (p.items.isEmpty()) return
    beginEdit(idx)
    applyItems(idx, emptyList())
    onSelection(-1, emptySet(), null)
    activeWriteBoxId = null
    activeMindNodeId = null
    say("Cleared page ${idx + 1}")
}
