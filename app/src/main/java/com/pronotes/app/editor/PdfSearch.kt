package com.pronotes.app.editor

import android.content.Context
import android.graphics.RectF
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File

data class SearchHit(
    val page: Int,
    val rects: List<RectF>,
    val snippet: String,
    val source: String = "Text",
    val score: Int = 100,
    val highlightColors: List<Int> = emptyList(),
)

/**
 * Text index of an imported PDF, built with PdfBox-Android.
 * Each character keeps its box so hits can be highlighted on the page.
 */
class PdfTextIndex private constructor(private val pages: Map<Int, PageText>) {

    private class PageText(val text: String, val boxes: List<RectF?>)

    private fun foldSearchString(raw: String): Pair<String, IntArray> {
        val sb = StringBuilder(raw.length)
        val map = IntArray(raw.length + 1)
        var lastWasSpace = false
        for (i in raw.indices) {
            val ch = raw[i]
            if (ch.isWhitespace()) {
                if (!lastWasSpace && sb.isNotEmpty()) {
                    map[sb.length] = i
                    sb.append(' ')
                    lastWasSpace = true
                }
                continue
            }
            lastWasSpace = false
            val norm = java.text.Normalizer.normalize(ch.toString(), java.text.Normalizer.Form.NFD)
            for (c in norm) {
                if (Character.getType(c) != Character.NON_SPACING_MARK.toInt()) {
                    if (sb.length < map.size) map[sb.length] = i
                    sb.append(c.lowercaseChar())
                }
            }
        }
        return sb.toString() to map
    }

    /** [pdfPageToNotePage] maps PDF page numbers to notebook page indices. */
    fun search(query: String, pdfPageToNotePage: (Int) -> Int?): List<SearchHit> {
        val q = foldSearchString(query.trim()).first.trim()
        if (q.isEmpty()) return emptyList()
        val out = ArrayList<SearchHit>()
        for ((pdfPage, pt) in pages.toSortedMap()) {
            val notePage = pdfPageToNotePage(pdfPage) ?: continue
            val (folded, idxMap) = foldSearchString(pt.text)
            var from = 0
            while (true) {
                val at = folded.indexOf(q, from)
                if (at < 0) break
                val origStart = idxMap[at.coerceIn(0, idxMap.lastIndex)]
                val origEnd = idxMap[(at + q.length - 1).coerceIn(0, idxMap.lastIndex)] + 1
                val rects = mergeLine((origStart until origEnd).mapNotNull { pt.boxes.getOrNull(it) })
                val s = (origStart - 30).coerceAtLeast(0)
                val e = (origEnd + 36).coerceAtMost(pt.text.length)
                out += SearchHit(
                    page = notePage,
                    rects = rects,
                    snippet = pt.text.substring(s, e).replace('\n', ' ').replace(Regex("\\s+"), " ").trim(),
                    source = "PDF",
                    score = 100,
                )
                from = at + q.length
            }
        }
        return out
    }

    /** All text of one PDF page (for reading and flashcard study). */
    fun pageText(pdfPage: Int): String = pages[pdfPage]?.text ?: ""

    /** Character under (x, y) in page units, or the nearest one on that line. */
    fun charAt(pdfPage: Int, x: Float, y: Float): Int? {
        val pt = pages[pdfPage] ?: return null
        var best = -1; var bestD = Float.MAX_VALUE
        pt.boxes.forEachIndexed { i, b ->
            if (b == null) return@forEachIndexed
            if (b.contains(x, y)) return i
            if (y < b.top - b.height() || y > b.bottom + b.height()) return@forEachIndexed
            val d = kotlin.math.abs(b.centerX() - x) + kotlin.math.abs(b.centerY() - y) * 3f
            if (d < bestD) { bestD = d; best = i }
        }
        return best.takeIf { it >= 0 && bestD < 120f }
    }

    /** Start and end (exclusive) of the word around [i]. */
    fun wordAt(pdfPage: Int, i: Int): IntRange {
        val t = pages[pdfPage]?.text ?: return i until i + 1
        var a = i; var b = i + 1
        while (a > 0 && t[a - 1].isLetterOrDigit()) a--
        while (b < t.length && t[b].isLetterOrDigit()) b++
        return a until b
    }

    fun text(pdfPage: Int, range: IntRange): String {
        val t = pages[pdfPage]?.text ?: return ""
        val a = range.first.coerceIn(0, t.length); val b = (range.last + 1).coerceIn(a, t.length)
        return t.substring(a, b).replace('\n', ' ').trim()
    }

    fun rects(pdfPage: Int, range: IntRange): List<RectF> {
        val pt = pages[pdfPage] ?: return emptyList()
        return mergeLine(range.mapNotNull { pt.boxes.getOrNull(it) })
    }

    private fun mergeLine(boxes: List<RectF>): List<RectF> {
        val merged = ArrayList<RectF>()
        for (b in boxes) {
            val last = merged.lastOrNull()
            if (last != null && abs(last.centerY() - b.centerY()) < last.height() * 0.6f) last.union(b) else merged += RectF(b)
        }
        return merged
    }

    private fun abs(f: Float) = if (f < 0) -f else f

    companion object {
        /**
         * Blocking. [pageWidth] gives the notebook width of each PDF page, so boxes line up with
         * the page however it was scaled on import.
         */
        fun build(context: Context, file: File, only: Set<Int>? = null, pageWidth: (Int) -> Float? = { null }): PdfTextIndex {
            PDFBoxResourceLoader.init(context.applicationContext)
            val result = HashMap<Int, PageText>()
            PDDocument.load(file).use { doc ->
                for (i in 0 until doc.numberOfPages) {
                    if (only != null && i !in only) continue
                    val mw = runCatching { doc.getPage(i).mediaBox.width }.getOrDefault(0f)
                    val scale = pageWidth(i)?.takeIf { mw > 0f }?.let { it / mw } ?: 2f
                    val chars = StringBuilder()
                    val boxes = ArrayList<RectF?>()
                    val stripper = object : PDFTextStripper() {
                        override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
                            textPositions?.forEach { tp ->
                                val u = tp.unicode ?: return@forEach
                                val x = tp.xDirAdj * scale
                                val y = tp.yDirAdj * scale
                                val h = (tp.heightDir.takeIf { it > 0 } ?: tp.fontSizeInPt) * scale * 1.25f
                                val w = tp.widthDirAdj * scale
                                for (c in u) {
                                    chars.append(c)
                                    boxes += RectF(x, y - h, x + w, y + h * 0.2f)
                                }
                            }
                        }

                        override fun writeWordSeparator() { chars.append(' '); boxes += null }
                        override fun writeLineSeparator() { chars.append('\n'); boxes += null }
                    }
                    stripper.startPage = i + 1
                    stripper.endPage = i + 1
                    stripper.getText(doc)
                    result[i] = PageText(chars.toString(), boxes)
                }
            }
            return PdfTextIndex(result)
        }
    }
}
