package com.pronotes.app.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Multi-format document importer for Pro Notes v13.
 * Supports Word (.docx, .doc, .odt, .rtf), PowerPoint (.pptx, .ppt, .odp),
 * Excel / Spreadsheets (.xlsx, .xls, .ods, .csv, .tsv), and EPUB books (.epub).
 * Converts imported documents into fully editable PageDoc pages with text blocks,
 * headings, slide layouts, spreadsheet grids, and embedded images.
 */
object OfficeDocumentImporter {

    private val SUPPORTED_EXTENSIONS = setOf(
        "docx", "doc", "odt", "rtf",
        "pptx", "ppt", "odp",
        "xlsx", "xls", "ods", "csv", "tsv",
        "epub"
    )

    fun isSupportedFile(fileName: String, mimeType: String = ""): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.US)
        if (ext in SUPPORTED_EXTENSIONS) return true
        val mime = mimeType.lowercase(Locale.US)
        return mime.contains("officedocument") ||
            mime.contains("msword") ||
            mime.contains("ms-powerpoint") ||
            mime.contains("ms-excel") ||
            mime.contains("opendocument") ||
            mime.contains("epub") ||
            mime == "application/rtf" ||
            mime == "text/rtf" ||
            mime == "text/csv" ||
            mime == "text/tab-separated-values"
    }

    fun importDocumentPages(
        context: Context,
        uri: Uri,
        displayName: String,
        mimeType: String = "",
        saveAssetBytes: (ByteArray, String) -> String?
    ): List<PageDoc> {
        val cleanExt = resolveExtension(displayName, mimeType)
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return listOf(fallbackPage(displayName, "Unable to read file content."))

        return runCatching {
            when (cleanExt) {
                "docx" -> parseDocx(rawBytes, displayName, saveAssetBytes)
                "odt" -> parseOdt(rawBytes, displayName, saveAssetBytes)
                "rtf" -> parseRtf(rawBytes, displayName)
                "doc" -> parseLegacyBinaryText(rawBytes, displayName, "Word Document")
                "pptx" -> parsePptx(rawBytes, displayName, saveAssetBytes)
                "odp" -> parseOdp(rawBytes, displayName, saveAssetBytes)
                "ppt" -> parseLegacyBinaryText(rawBytes, displayName, "Presentation")
                "xlsx" -> parseXlsx(rawBytes, displayName)
                "ods" -> parseOds(rawBytes, displayName)
                "csv" -> parseDelimitedSheet(rawBytes, displayName, ',')
                "tsv" -> parseDelimitedSheet(rawBytes, displayName, '\t')
                "xls" -> parseLegacyBinaryText(rawBytes, displayName, "Spreadsheet")
                "epub" -> parseEpub(rawBytes, displayName, saveAssetBytes)
                else -> listOf(fallbackPage(displayName, "Imported file: $displayName"))
            }
        }.getOrElse {
            listOf(fallbackPage(displayName, "Imported $displayName"))
        }.ifEmpty {
            listOf(fallbackPage(displayName, "Imported $displayName"))
        }
    }

    private fun resolveExtension(fileName: String, mimeType: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.US)
        if (ext in SUPPORTED_EXTENSIONS) return ext
        val mime = mimeType.lowercase(Locale.US)
        return when {
            mime.contains("wordprocessingml") -> "docx"
            mime.contains("msword") -> "doc"
            mime.contains("presentationml") -> "pptx"
            mime.contains("ms-powerpoint") -> "ppt"
            mime.contains("spreadsheetml") -> "xlsx"
            mime.contains("ms-excel") -> "xls"
            mime.contains("opendocument.text") -> "odt"
            mime.contains("opendocument.presentation") -> "odp"
            mime.contains("opendocument.spreadsheet") -> "ods"
            mime.contains("epub") -> "epub"
            mime.contains("rtf") -> "rtf"
            mime.contains("csv") -> "csv"
            mime.contains("tab-separated") -> "tsv"
            else -> ext
        }
    }

    private data class DocParagraph(
        val text: String,
        val isTitle: Boolean = false,
        val isHeading1: Boolean = false,
        val isHeading2: Boolean = false,
        val isBullet: Boolean = false,
        val isTableRow: Boolean = false
    )

    private fun readZipEntries(bytes: ByteArray): Map<String, ByteArray> {
        val map = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.size < 25_000_000L) {
                    map[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return map
    }

    // ─── Word (.docx) ───────────────────────────────────────────────────────
    private fun parseDocx(
        bytes: ByteArray,
        displayName: String,
        saveAssetBytes: (ByteArray, String) -> String?
    ): List<PageDoc> {
        val entries = readZipEntries(bytes)
        val docXml = entries["word/document.xml"]
            ?: return parseLegacyBinaryText(bytes, displayName, "Word Document")

        val paragraphs = mutableListOf<DocParagraph>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(docXml), "UTF-8")

        var inParagraph = false
        var inTableRow = false
        var currentStyle = ""
        var hasNumPr = false
        val paraBuilder = StringBuilder()
        val rowCells = mutableListOf<String>()
        val cellBuilder = StringBuilder()
        var inTableCell = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.substringAfterLast(':')) {
                        "tr" -> {
                            inTableRow = true
                            rowCells.clear()
                        }
                        "tc" -> {
                            inTableCell = true
                            cellBuilder.setLength(0)
                        }
                        "p" -> {
                            inParagraph = true
                            currentStyle = ""
                            hasNumPr = false
                            paraBuilder.setLength(0)
                        }
                        "pStyle" -> {
                            val v = parser.getAttributeValue(null, "w:val")
                                ?: parser.getAttributeValue(null, "val")
                                ?: ""
                            currentStyle = v.lowercase(Locale.US)
                        }
                        "numPr" -> hasNumPr = true
                        "t" -> {
                            val txt = parser.nextText()
                            if (inTableCell) {
                                cellBuilder.append(txt)
                            } else if (inParagraph) {
                                paraBuilder.append(txt)
                            }
                        }
                        "tab" -> if (inParagraph) paraBuilder.append("    ")
                        "br", "cr" -> if (inParagraph) paraBuilder.append("\n")
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name.substringAfterLast(':')) {
                        "tc" -> {
                            if (inTableCell) {
                                rowCells.add(cellBuilder.toString().trim())
                                inTableCell = false
                            }
                        }
                        "tr" -> {
                            if (inTableRow && rowCells.any { it.isNotBlank() }) {
                                paragraphs.add(
                                    DocParagraph(
                                        text = rowCells.joinToString("  │  "),
                                        isTableRow = true
                                    )
                                )
                            }
                            inTableRow = false
                        }
                        "p" -> {
                            if (inParagraph && !inTableRow) {
                                val raw = paraBuilder.toString().trim()
                                if (raw.isNotEmpty()) {
                                    paragraphs.add(
                                        DocParagraph(
                                            text = raw,
                                            isTitle = "title" in currentStyle,
                                            isHeading1 = "heading1" in currentStyle || currentStyle == "h1",
                                            isHeading2 = "heading2" in currentStyle || "heading3" in currentStyle || "subtitle" in currentStyle,
                                            isBullet = hasNumPr || "list" in currentStyle
                                        )
                                    )
                                }
                            }
                            inParagraph = false
                        }
                    }
                }
            }
            event = parser.next()
        }

        val mediaFiles = entries.entries
            .filter { it.key.startsWith("word/media/") && isImageFileName(it.key) }
            .take(6)
            .mapNotNull { (name, data) ->
                val ext = name.substringAfterLast('.', "png")
                saveAssetBytes(data, ext)?.let { fileName -> fileName to data }
            }

        return paginateDocumentParagraphs(displayName, paragraphs, mediaFiles)
    }

    // ─── OpenDocument Text (.odt) ───────────────────────────────────────────
    private fun parseOdt(
        bytes: ByteArray,
        displayName: String,
        saveAssetBytes: (ByteArray, String) -> String?
    ): List<PageDoc> {
        val entries = readZipEntries(bytes)
        val contentXml = entries["content.xml"]
            ?: return listOf(fallbackPage(displayName, "Unable to read ODT content."))
        val paragraphs = extractGenericXmlParagraphs(contentXml)
        val mediaFiles = entries.entries
            .filter { it.key.startsWith("Pictures/") && isImageFileName(it.key) }
            .take(4)
            .mapNotNull { (name, data) ->
                val ext = name.substringAfterLast('.', "png")
                saveAssetBytes(data, ext)?.let { it to data }
            }
        return paginateDocumentParagraphs(displayName, paragraphs, mediaFiles)
    }

    // ─── Rich Text Format (.rtf) ────────────────────────────────────────────
    private fun parseRtf(bytes: ByteArray, displayName: String): List<PageDoc> {
        val raw = bytes.toString(Charsets.UTF_8)
        val cleaned = raw
            .replace(Regex("\\\\par\\b"), "\n")
            .replace(Regex("\\\\line\\b"), "\n")
            .replace(Regex("\\\\tab\\b"), "    ")
            .replace(Regex("\\\\'[0-9a-fA-F]{2}"), "")
            .replace(Regex("\\{\\\\[^{}]*\\}"), "")
            .replace(Regex("\\\\[a-zA-Z]+-?\\d* ?"), "")
            .replace("{", "")
            .replace("}", "")
        val paragraphs = cleaned.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapIndexed { idx, line ->
                DocParagraph(
                    text = line,
                    isTitle = idx == 0 && line.length < 70
                )
            }
        return paginateDocumentParagraphs(displayName, paragraphs, emptyList())
    }

    // ─── PowerPoint (.pptx) ─────────────────────────────────────────────────
    private fun parsePptx(
        bytes: ByteArray,
        displayName: String,
        saveAssetBytes: (ByteArray, String) -> String?
    ): List<PageDoc> {
        val entries = readZipEntries(bytes)
        val slideEntries = entries.entries
            .filter { it.key.matches(Regex("ppt/slides/slide\\d+\\.xml")) }
            .sortedBy {
                Regex("slide(\\d+)\\.xml").find(it.key)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            }

        if (slideEntries.isEmpty()) {
            return parseLegacyBinaryText(bytes, displayName, "Presentation")
        }

        val mediaByZipPath = LinkedHashMap<String, Pair<String, ByteArray>>()
        entries.entries
            .filter { it.key.startsWith("ppt/media/") && isImageFileName(it.key) }
            .forEach { (name, data) ->
                val ext = name.substringAfterLast('.', "png")
                saveAssetBytes(data, ext)?.let { saved ->
                    mediaByZipPath[name] = saved to data
                }
            }
        val mediaPool = mediaByZipPath.values.toList()

        val slideW = 1600f
        val slideH = 900f

        return slideEntries.mapIndexed { index, slideEntry ->
            val slideFileNum = Regex("slide(\\d+)\\.xml").find(slideEntry.key)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: (index + 1)
            val slideNum = index + 1
            val lines = extractXmlTextParagraphs(slideEntry.value, "p", "t")
            val notesBytes = entries["ppt/notesSlides/notesSlide$slideFileNum.xml"]
            val notesLines = notesBytes?.let { extractXmlTextParagraphs(it, "p", "t") }
                ?.filter { it.isNotBlank() && it != slideNum.toString() }
                .orEmpty()

            // Resolve exact images referenced by this slide's .rels file
            val relsBytes = entries["ppt/slides/_rels/slide$slideFileNum.xml.rels"]
            val slideImages = if (relsBytes != null) {
                val relsText = relsBytes.toString(Charsets.UTF_8)
                Regex("Target=\"([^\"]+)\"").findAll(relsText)
                    .mapNotNull { m ->
                        val target = m.groupValues[1].replace("../", "ppt/")
                        mediaByZipPath[target]
                    }
                    .distinctBy { it.first }
                    .toList()
            } else emptyList()
            val effectiveImages = slideImages.ifEmpty { listOfNotNull(mediaPool.getOrNull(index)) }

            val items = mutableListOf<ItemDoc>()
            val slideTitle = lines.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: "${displayName.substringBeforeLast('.')} — Slide $slideNum"
            val bodyLines = if (lines.size > 1) lines.drop(1) else emptyList()

            items.add(
                ItemDoc.Text(
                    text = slideTitle,
                    x = 76f,
                    y = 66f,
                    size = 34f,
                    color = 0xFF0F172A.toInt(),
                    font = "SANS",
                    bold = true
                )
            )

            items.add(
                ItemDoc.Text(
                    text = "SLIDE $slideNum / ${slideEntries.size}",
                    x = 1380f,
                    y = 56f,
                    size = 16f,
                    color = 0xFF64748B.toInt(),
                    font = "MONO",
                    bold = true
                )
            )

            if (bodyLines.isNotEmpty()) {
                val formattedBullets = bodyLines
                    .flatMap { it.lines() }
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .take(14)
                    .joinToString("\n\n") { line ->
                        if (line.startsWith("•") || line.startsWith("-")) line else "•  $line"
                    }

                items.add(
                    ItemDoc.Text(
                        text = formattedBullets,
                        x = 84f,
                        y = 148f,
                        size = 24f,
                        color = 0xFF1E293B.toInt(),
                        font = "SANS"
                    )
                )
            }

            effectiveImages.take(3).forEachIndexed { imgIdx, (fileName, imgBytes) ->
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size, opts)
                val aspect = if (opts.outWidth > 0 && opts.outHeight > 0) {
                    opts.outHeight.toFloat() / opts.outWidth.toFloat()
                } else 0.65f
                val targetW = 460f
                val targetH = (targetW * aspect).coerceIn(160f, 480f)
                items.add(
                    ItemDoc.Image(
                        file = fileName,
                        x = 1040f,
                        y = (140f + imgIdx * 240f).coerceAtMost(slideH - targetH - 32f),
                        w = targetW,
                        h = targetH
                    )
                )
            }

            if (notesLines.isNotEmpty()) {
                val noteSummary = notesLines.joinToString("\n").take(380)
                items.add(
                    ItemDoc.Sticky(
                        x = 84f,
                        y = 680f,
                        w = 720f,
                        h = 170f,
                        color = 0xFFFFF3A3.toInt(),
                        text = noteSummary,
                        title = "Speaker Notes",
                        fontSize = 20f
                    )
                )
            }

            PageDoc(
                id = UUID.randomUUID().toString(),
                width = slideW,
                height = slideH,
                paper = PaperStyle.BLANK,
                paperColor = 0xFFFFFFFF.toInt(),
                items = items
            )
        }
    }

    // ─── OpenDocument Presentation (.odp) ───────────────────────────────────
    private fun parseOdp(
        bytes: ByteArray,
        displayName: String,
        saveAssetBytes: (ByteArray, String) -> String?
    ): List<PageDoc> {
        val entries = readZipEntries(bytes)
        val contentXml = entries["content.xml"]
            ?: return listOf(fallbackPage(displayName, "Unable to read ODP presentation."))
        val paragraphs = extractGenericXmlParagraphs(contentXml)
        val mediaFiles = entries.entries
            .filter { it.key.startsWith("Pictures/") && isImageFileName(it.key) }
            .take(6)
            .mapNotNull { (name, data) ->
                val ext = name.substringAfterLast('.', "png")
                saveAssetBytes(data, ext)?.let { it to data }
            }
        return paginateDocumentParagraphs(displayName, paragraphs, mediaFiles)
    }

    // ─── Excel (.xlsx) ──────────────────────────────────────────────────────
    private fun parseXlsx(bytes: ByteArray, displayName: String): List<PageDoc> {
        val entries = readZipEntries(bytes)
        val sharedStrings = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val sheetEntries = entries.entries
            .filter { it.key.matches(Regex("xl/worksheets/sheet\\d+\\.xml")) }
            .sortedBy {
                Regex("sheet(\\d+)\\.xml").find(it.key)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            }

        if (sheetEntries.isEmpty()) {
            return parseLegacyBinaryText(bytes, displayName, "Spreadsheet")
        }

        val pages = mutableListOf<PageDoc>()
        sheetEntries.forEachIndexed { sheetIdx, entry ->
            val rows = parseWorksheetRows(entry.value, sharedStrings)
            if (rows.isNotEmpty()) {
                val sheetTitle = "${displayName.substringBeforeLast('.')} — Sheet ${sheetIdx + 1}"
                pages.addAll(buildSpreadsheetPages(sheetTitle, rows))
            }
        }

        return pages.ifEmpty {
            listOf(fallbackPage(displayName, "Empty spreadsheet: $displayName"))
        }
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
        var inSi = false
        val sb = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.substringAfterLast(':')
                    if (tag == "si") {
                        inSi = true
                        sb.setLength(0)
                    } else if (tag == "t" && inSi) {
                        sb.append(parser.nextText())
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.substringAfterLast(':') == "si" && inSi) {
                        result.add(sb.toString())
                        inSi = false
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseWorksheetRows(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var currentRow = mutableListOf<String>()
        var cellType = ""
        var cellValue = ""
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.substringAfterLast(':')) {
                        "row" -> currentRow = mutableListOf()
                        "c" -> {
                            cellType = parser.getAttributeValue(null, "t") ?: ""
                            cellValue = ""
                        }
                        "v", "t" -> {
                            cellValue = parser.nextText()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name.substringAfterLast(':')) {
                        "c" -> {
                            val resolved = if (cellType == "s") {
                                val idx = cellValue.toIntOrNull() ?: -1
                                sharedStrings.getOrElse(idx) { cellValue }
                            } else {
                                cellValue
                            }
                            currentRow.add(resolved.trim())
                        }
                        "row" -> {
                            if (currentRow.any { it.isNotBlank() }) {
                                rows.add(currentRow)
                            }
                        }
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    // ─── OpenDocument Spreadsheet (.ods) & CSV / TSV ────────────────────────
    private fun parseOds(bytes: ByteArray, displayName: String): List<PageDoc> {
        val entries = readZipEntries(bytes)
        val contentXml = entries["content.xml"]
            ?: return listOf(fallbackPage(displayName, "Unable to read ODS spreadsheet."))
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(contentXml), "UTF-8")
        var currentRow = mutableListOf<String>()
        var inCell = false
        val cellSb = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.substringAfterLast(':')) {
                        "table-row" -> currentRow = mutableListOf()
                        "table-cell" -> {
                            inCell = true
                            cellSb.setLength(0)
                        }
                        "p" -> if (inCell) {
                            if (cellSb.isNotEmpty()) cellSb.append(" ")
                            cellSb.append(parser.nextText())
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name.substringAfterLast(':')) {
                        "table-cell" -> {
                            if (inCell) {
                                currentRow.add(cellSb.toString().trim())
                                inCell = false
                            }
                        }
                        "table-row" -> {
                            if (currentRow.any { it.isNotBlank() }) rows.add(currentRow)
                        }
                    }
                }
            }
            event = parser.next()
        }
        return buildSpreadsheetPages(displayName.substringBeforeLast('.'), rows)
    }

    private fun parseDelimitedSheet(bytes: ByteArray, displayName: String, delimiter: Char): List<PageDoc> {
        val text = bytes.toString(Charsets.UTF_8)
        val rows = text.lines()
            .filter { it.isNotBlank() }
            .map { line -> parseDelimitedLine(line, delimiter) }
        return buildSpreadsheetPages(displayName.substringBeforeLast('.'), rows)
    }

    private fun parseDelimitedLine(line: String, delimiter: Char): List<String> {
        val cells = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> {
                    cells.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> sb.append(ch)
            }
        }
        cells.add(sb.toString().trim())
        return cells
    }

    private fun buildSpreadsheetPages(
        sheetTitle: String,
        rows: List<List<String>>
    ): List<PageDoc> {
        if (rows.isEmpty()) return emptyList()
        val rowsPerPage = 32
        val chunks = rows.chunked(rowsPerPage)
        val headerRow = rows.firstOrNull().orEmpty()

        return chunks.mapIndexed { chunkIdx, chunkRows ->
            val items = mutableListOf<ItemDoc>()
            items.add(
                ItemDoc.Text(
                    text = if (chunks.size > 1) "$sheetTitle (Part ${chunkIdx + 1}/${chunks.size})" else sheetTitle,
                    x = 56f,
                    y = 56f,
                    size = 28f,
                    color = 0xFF0F172A.toInt(),
                    font = "SANS",
                    bold = true
                )
            )

            val colCount = chunkRows.maxOfOrNull { it.size }?.coerceIn(1, 8) ?: 1
            val colWidths = IntArray(colCount) { col ->
                val maxLen = chunkRows.maxOfOrNull { row -> row.getOrNull(col)?.length ?: 0 } ?: 8
                maxLen.coerceIn(8, 22)
            }

            val formattedRows = buildString {
                if (chunkIdx > 0 && headerRow.isNotEmpty()) {
                    appendLine(formatRowCells(headerRow, colCount, colWidths))
                    appendLine("─".repeat(colWidths.sum() + (colCount - 1) * 3))
                }
                chunkRows.forEachIndexed { rIdx, row ->
                    appendLine(formatRowCells(row, colCount, colWidths))
                    if (chunkIdx == 0 && rIdx == 0 && chunkRows.size > 1) {
                        appendLine("─".repeat(colWidths.sum() + (colCount - 1) * 3))
                    }
                }
            }.trimEnd()

            items.add(
                ItemDoc.Text(
                    text = formattedRows,
                    x = 56f,
                    y = 114f,
                    size = 18f,
                    color = 0xFF1E293B.toInt(),
                    font = "MONO"
                )
            )

            PageDoc(
                id = UUID.randomUUID().toString(),
                width = PageSize.WIDTH,
                height = PageSize.HEIGHT,
                paper = PaperStyle.GRID,
                paperColor = 0xFFFFFFFF.toInt(),
                items = items
            )
        }
    }

    private fun formatRowCells(row: List<String>, colCount: Int, colWidths: IntArray): String {
        return (0 until colCount).joinToString(" │ ") { col ->
            val raw = row.getOrNull(col)?.replace('\n', ' ')?.trim().orEmpty()
            val width = colWidths[col]
            if (raw.length > width) raw.take(width - 1) + "…" else raw.padEnd(width, ' ')
        }
    }

    // ─── EPUB Books (.epub) ─────────────────────────────────────────────────
    private fun parseEpub(
        bytes: ByteArray,
        displayName: String,
        saveAssetBytes: (ByteArray, String) -> String?
    ): List<PageDoc> {
        val entries = readZipEntries(bytes)
        val htmlEntries = entries.entries
            .filter {
                val lower = it.key.lowercase(Locale.US)
                (lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")) &&
                    !lower.contains("nav") && !lower.contains("toc")
            }
            .sortedBy { it.key }

        val paragraphs = mutableListOf<DocParagraph>()
        paragraphs.add(DocParagraph(text = displayName.substringBeforeLast('.'), isTitle = true))

        for (entry in htmlEntries) {
            val html = entry.value.toString(Charsets.UTF_8)
            val blockRegex = Regex("<(h1|h2|h3|p|li)[^>]*>(.*?)</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            val matches = blockRegex.findAll(html).toList()
            if (matches.isNotEmpty()) {
                for (match in matches) {
                    val tag = match.groupValues[1].lowercase(Locale.US)
                    val clean = stripHtmlTags(match.groupValues[2])
                    if (clean.isNotBlank()) {
                        paragraphs.add(
                            DocParagraph(
                                text = clean,
                                isHeading1 = tag == "h1",
                                isHeading2 = tag == "h2" || tag == "h3",
                                isBullet = tag == "li"
                            )
                        )
                    }
                }
            } else {
                val fallbackText = stripHtmlTags(html)
                fallbackText.lines().map { it.trim() }.filter { it.isNotBlank() }.forEach {
                    paragraphs.add(DocParagraph(text = it))
                }
            }
        }

        val coverImages = entries.entries
            .filter { isImageFileName(it.key) }
            .sortedByDescending { if ("cover" in it.key.lowercase(Locale.US)) 1 else 0 }
            .take(4)
            .mapNotNull { (name, data) ->
                val ext = name.substringAfterLast('.', "png")
                saveAssetBytes(data, ext)?.let { it to data }
            }

        return paginateDocumentParagraphs(displayName, paragraphs, coverImages)
    }

    private fun stripHtmlTags(html: String): String {
        return html
            .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("[ \\t]+"), " ")
            .trim()
    }

    // ─── Legacy binary fallback (.doc / .ppt / .xls) ────────────────────────
    private fun parseLegacyBinaryText(
        bytes: ByteArray,
        displayName: String,
        kindLabel: String
    ): List<PageDoc> {
        val asciiRuns = mutableListOf<String>()
        val sb = StringBuilder()
        for (b in bytes) {
            val ch = b.toInt() and 0xFF
            if (ch in 32..126 || ch == 10 || ch == 13 || ch == 9) {
                sb.append(ch.toChar())
            } else {
                if (sb.length >= 12) {
                    val candidate = sb.toString().trim()
                    if (!looksLikeBinaryMetadata(candidate)) {
                        asciiRuns.add(candidate)
                    }
                }
                sb.setLength(0)
            }
        }
        if (sb.length >= 12) {
            val candidate = sb.toString().trim()
            if (!looksLikeBinaryMetadata(candidate)) asciiRuns.add(candidate)
        }

        val paragraphs = mutableListOf<DocParagraph>()
        paragraphs.add(DocParagraph(text = "${displayName.substringBeforeLast('.')} ($kindLabel)", isTitle = true))
        asciiRuns
            .flatMap { it.lines() }
            .map { it.trim() }
            .filter { it.length >= 4 && !looksLikeBinaryMetadata(it) }
            .distinct()
            .take(240)
            .forEach { line ->
                paragraphs.add(DocParagraph(text = line))
            }

        return paginateDocumentParagraphs(displayName, paragraphs, emptyList())
    }

    private fun looksLikeBinaryMetadata(s: String): Boolean {
        val lower = s.lowercase(Locale.US)
        return lower.contains("content_types") ||
            lower.contains("root entry") ||
            (lower.contains("microsoft") && s.length < 28) ||
            lower.contains("normal.dot") ||
            (lower.contains("xml") && lower.contains("schemas")) ||
            s.count { it.isLetterOrDigit() || it.isWhitespace() } < (s.length * 0.75f).toInt()
    }

    private fun extractGenericXmlParagraphs(xmlBytes: ByteArray): List<DocParagraph> {
        val list = mutableListOf<DocParagraph>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
        var inBlock = false
        var isHeading = false
        val sb = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.substringAfterLast(':').lowercase(Locale.US)
                    if (tag == "p" || tag == "h") {
                        inBlock = true
                        isHeading = tag == "h"
                        sb.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inBlock) sb.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.substringAfterLast(':').lowercase(Locale.US)
                    if ((tag == "p" || tag == "h") && inBlock) {
                        val txt = sb.toString().trim()
                        if (txt.isNotEmpty()) {
                            list.add(DocParagraph(text = txt, isHeading1 = isHeading))
                        }
                        inBlock = false
                    }
                }
            }
            event = parser.next()
        }
        return list
    }

    private fun extractXmlTextParagraphs(xmlBytes: ByteArray, pTag: String, tTag: String): List<String> {
        val lines = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
        var inP = false
        val sb = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.substringAfterLast(':')
                    if (tag == pTag) {
                        inP = true
                        sb.setLength(0)
                    } else if (tag == tTag && inP) {
                        sb.append(parser.nextText())
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.substringAfterLast(':')
                    if (tag == pTag && inP) {
                        val txt = sb.toString().trim()
                        if (txt.isNotEmpty()) lines.add(txt)
                        inP = false
                    }
                }
            }
            event = parser.next()
        }
        return lines
    }

    private fun paginateDocumentParagraphs(
        displayName: String,
        paragraphs: List<DocParagraph>,
        mediaFiles: List<Pair<String, ByteArray>>
    ): List<PageDoc> {
        val cleanParagraphs = if (paragraphs.isEmpty()) {
            listOf(DocParagraph(text = displayName.substringBeforeLast('.'), isTitle = true))
        } else paragraphs

        val pages = mutableListOf<PageDoc>()
        var currentItems = mutableListOf<ItemDoc>()
        var cursorY = 84f
        val maxPageY = PageSize.HEIGHT - 120f
        var bodyAccumulator = StringBuilder()
        var bodyStartY = cursorY

        fun flushBodyText() {
            val txt = bodyAccumulator.toString().trimEnd()
            if (txt.isNotEmpty()) {
                currentItems.add(
                    ItemDoc.Text(
                        text = txt,
                        x = 76f,
                        y = bodyStartY,
                        size = 24f,
                        color = 0xFF1E293B.toInt(),
                        font = "SANS"
                    )
                )
                bodyAccumulator.setLength(0)
            }
        }

        fun pushNewPage() {
            flushBodyText()
            if (currentItems.isNotEmpty()) {
                pages.add(
                    PageDoc(
                        id = UUID.randomUUID().toString(),
                        width = PageSize.WIDTH,
                        height = PageSize.HEIGHT,
                        paper = PaperStyle.RULED,
                        paperColor = 0xFFFFFFFF.toInt(),
                        items = currentItems
                    )
                )
                currentItems = mutableListOf()
            }
            cursorY = 84f
            bodyStartY = cursorY
        }

        cleanParagraphs.forEach { para ->
            val wrappedLineCount = (para.text.length / 52) + para.text.count { it == '\n' } + 1
            val blockHeight = when {
                para.isTitle -> wrappedLineCount * 44f + 26f
                para.isHeading1 -> wrappedLineCount * 38f + 22f
                para.isHeading2 -> wrappedLineCount * 34f + 18f
                else -> wrappedLineCount * 32f + 14f
            }

            if (cursorY + blockHeight > maxPageY && (currentItems.isNotEmpty() || bodyAccumulator.isNotEmpty())) {
                pushNewPage()
            }

            when {
                para.isTitle || para.isHeading1 || para.isHeading2 -> {
                    flushBodyText()
                    currentItems.add(
                        ItemDoc.Text(
                            text = para.text,
                            x = 76f,
                            y = cursorY,
                            size = when {
                                para.isTitle -> 34f
                                para.isHeading1 -> 29f
                                else -> 25f
                            },
                            color = if (para.isTitle) 0xFF0F172A.toInt() else 0xFF2855C8.toInt(),
                            font = "SANS",
                            bold = true
                        )
                    )
                    cursorY += blockHeight
                    bodyStartY = cursorY
                }
                para.isTableRow -> {
                    flushBodyText()
                    currentItems.add(
                        ItemDoc.Text(
                            text = para.text,
                            x = 76f,
                            y = cursorY,
                            size = 19f,
                            color = 0xFF334155.toInt(),
                            font = "MONO"
                        )
                    )
                    cursorY += blockHeight
                    bodyStartY = cursorY
                }
                else -> {
                    if (bodyAccumulator.isEmpty()) bodyStartY = cursorY
                    val formatted = if (para.isBullet && !para.text.startsWith("•")) {
                        "•  ${para.text}"
                    } else para.text
                    bodyAccumulator.append(formatted).append("\n\n")
                    cursorY += blockHeight
                }
            }
        }

        flushBodyText()

        mediaFiles.forEachIndexed { idx, (fileName, imgBytes) ->
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size, opts)
            val aspect = if (opts.outWidth > 0 && opts.outHeight > 0) {
                opts.outHeight.toFloat() / opts.outWidth.toFloat()
            } else 0.65f
            val targetW = 480f
            val targetH = (targetW * aspect).coerceIn(160f, 520f)
            if (idx == 0 && cursorY + targetH + 40f <= maxPageY) {
                currentItems.add(
                    ItemDoc.Image(
                        file = fileName,
                        x = 76f,
                        y = cursorY + 12f,
                        w = targetW,
                        h = targetH
                    )
                )
                cursorY += targetH + 28f
            } else {
                val targetPageIdx = idx.coerceAtMost((pages.size - 1).coerceAtLeast(0))
                if (pages.isNotEmpty()) {
                    val p = pages[targetPageIdx]
                    pages[targetPageIdx] = p.copy(
                        items = p.items + ItemDoc.Image(
                            file = fileName,
                            x = 640f,
                            y = 92f,
                            w = targetW,
                            h = targetH
                        )
                    )
                }
            }
        }

        if (currentItems.isNotEmpty() || pages.isEmpty()) {
            pages.add(
                PageDoc(
                    id = UUID.randomUUID().toString(),
                    width = PageSize.WIDTH,
                    height = PageSize.HEIGHT,
                    paper = PaperStyle.RULED,
                    paperColor = 0xFFFFFFFF.toInt(),
                    items = currentItems
                )
            )
        }

        return pages
    }

    private fun isImageFileName(name: String): Boolean {
        val lower = name.lowercase(Locale.US)
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp")
    }

    private fun fallbackPage(title: String, body: String): PageDoc {
        return PageDoc(
            id = UUID.randomUUID().toString(),
            width = PageSize.WIDTH,
            height = PageSize.HEIGHT,
            paper = PaperStyle.RULED,
            paperColor = 0xFFFFFFFF.toInt(),
            items = listOf(
                ItemDoc.Text(
                    text = title.substringBeforeLast('.'),
                    x = 76f,
                    y = 84f,
                    size = 34f,
                    color = 0xFF0F172A.toInt(),
                    font = "SANS",
                    bold = true
                ),
                ItemDoc.Text(
                    text = body,
                    x = 76f,
                    y = 148f,
                    size = 24f,
                    color = 0xFF334155.toInt(),
                    font = "SANS"
                )
            )
        )
    }
}
