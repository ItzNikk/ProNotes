package com.pronotes.app.editor

import android.content.Context
import android.graphics.RectF
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File

data class SearchHit(val page: Int, val rects: List<RectF>, val snippet: String)

/**
 * Text index of an imported PDF, built with PdfBox-Android.
 * Each character keeps its box so hits can be highlighted on the page.
 */
class PdfTextIndex private constructor(private val pages: Map<Int, PageText>) {

    private class PageText(val text: String, val boxes: List<RectF?>)

    /** [pdfPageToNotePage] maps PDF page numbers to notebook page indices. */
    fun search(query: String, pdfPageToNotePage: (Int) -> Int?): List<SearchHit> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val out = ArrayList<SearchHit>()
        for ((pdfPage, pt) in pages.toSortedMap()) {
            val notePage = pdfPageToNotePage(pdfPage) ?: continue
            var from = 0
            while (true) {
                val at = pt.text.indexOf(q, from, ignoreCase = true)
                if (at < 0) break
                val rects = mergeLine((at until at + q.length).mapNotNull { pt.boxes.getOrNull(it) })
                val s = (at - 30).coerceAtLeast(0); val e = (at + q.length + 30).coerceAtMost(pt.text.length)
                out += SearchHit(notePage, rects, pt.text.substring(s, e).replace('\n', ' ').trim())
                from = at + q.length
            }
        }
        return out
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
        /** Blocking. [scale] converts PDF points to page units (pages are stored at 2x). */
        fun build(context: Context, file: File, scale: Float = 2f): PdfTextIndex {
            PDFBoxResourceLoader.init(context.applicationContext)
            val result = HashMap<Int, PageText>()
            PDDocument.load(file).use { doc ->
                for (i in 0 until doc.numberOfPages) {
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
