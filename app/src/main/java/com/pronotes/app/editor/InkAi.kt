package com.pronotes.app.editor

import android.graphics.Matrix
import android.graphics.RectF
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * On-device handwriting recognition with Google ML Kit Digital Ink. The language model
 * (about 20 MB) downloads once; after that everything runs offline on the tablet.
 */
object Handwriting {
    @Volatile private var recognizer: DigitalInkRecognizer? = null
    @Volatile private var lang: String? = null
    @Volatile var ready = false
        private set

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { c ->
        addOnSuccessListener { c.resume(it) }
        addOnFailureListener { c.resume(null) }
        addOnCanceledListener { c.resume(null) }
    }

    /** Downloads the model when needed. Returns false when offline and no model is on the device yet. */
    suspend fun prepare(language: String = "en-US"): Boolean {
        if (ready && lang == language) return true
        val id = runCatching { DigitalInkRecognitionModelIdentifier.fromLanguageTag(language) }.getOrNull()
            ?: DigitalInkRecognitionModelIdentifier.EN_US
        val model = DigitalInkRecognitionModel.builder(id).build()
        val mgr = RemoteModelManager.getInstance()
        val have = mgr.isModelDownloaded(model).awaitOrNull() == true
        if (!have) mgr.download(model, DownloadConditions.Builder().build()).awaitOrNull() ?: return false
        recognizer?.close()
        recognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
        lang = language
        ready = true
        return true
    }

    /** Text for a group of strokes, or null. [before] is text written just before (helps accuracy). */
    suspend fun recognize(strokes: List<InkItem>, language: String = "en-US", before: String = ""): String? =
        candidates(strokes, language, before).firstOrNull()

    /** All guesses, best first. Maths uses this to pick the first one that is a valid sum. */
    suspend fun candidates(strokes: List<InkItem>, language: String = "en-US", before: String = ""): List<String> {
        if (strokes.isEmpty() || !prepare(language)) return emptyList()
        val ink = Ink.builder()
        var t = 0L
        for (s in strokes) {
            val pts = s.worldPoints()
            if (pts.isEmpty()) continue
            val b = Ink.Stroke.builder()
            for ((x, y) in pts) { b.addPoint(Ink.Point.create(x, y, t)); t += 8 }
            ink.addStroke(b.build())
            t += 120
        }
        // The line box and the words before it help ML Kit tell letters apart (o / 0, l / 1, case).
        val box = InkLayout.bounds(strokes)
        val ctx = com.google.mlkit.vision.digitalink.RecognitionContext.builder()
            .setPreContext(before.takeLast(20))
            .setWritingArea(com.google.mlkit.vision.digitalink.WritingArea(box.width().coerceAtLeast(1f), box.height().coerceAtLeast(1f)))
            .build()
        val res = recognizer?.recognize(ink.build(), ctx)?.awaitOrNull() ?: return emptyList()
        return res.candidates.mapNotNull { it.text?.trim()?.takeIf { s -> s.isNotEmpty() } }
    }
}

/** Groups ink into lines and tidies it. */
object InkLayout {
    /** Splits strokes into lines by vertical overlap, each line sorted left to right. */
    fun lines(items: List<InkItem>): List<List<InkItem>> {
        val sorted = items.filter { !it.isHighlighter }.sortedBy { it.bounds.centerY() }
        val lines = ArrayList<MutableList<InkItem>>()
        val boxes = ArrayList<RectF>()
        for (s in sorted) {
            val b = s.bounds
            val i = boxes.indexOfFirst { lb ->
                val overlap = minOf(lb.bottom, b.bottom) - maxOf(lb.top, b.top)
                overlap > minOf(lb.height(), b.height()).coerceAtLeast(1f) * 0.35f
            }
            if (i >= 0) { lines[i] += s; boxes[i].union(b) } else { lines += mutableListOf(s); boxes += RectF(b) }
        }
        return lines.map { l -> l.sortedBy { it.bounds.left } }
    }

    fun bounds(items: List<PageItem>): RectF {
        val r = RectF(items.first().bounds)
        items.drop(1).forEach { r.union(it.bounds) }
        return r
    }

    /**
     * Straightens each line (removes slope by least squares through stroke bottoms) and lines
     * up their left edges and spacing, like tidying a page of notes.
     */
    fun tidy(items: List<PageItem>): List<PageItem> {
        val ink = items.filterIsInstance<InkItem>()
        if (ink.isEmpty()) return items
        val ls = lines(ink)
        val left = ls.minOf { l -> l.minOf { it.bounds.left } }
        val heights = ls.map { l -> bounds(l).height() }.sorted()
        val lineH = heights[heights.size / 2]
        var y = bounds(ls.first()).top
        val out = HashMap<Long, PageItem>()
        for (l in ls) {
            val pts = l.map { it.bounds.centerX() to it.bounds.bottom }
            val slope = if (pts.size >= 2) {
                val mx = pts.sumOf { it.first.toDouble() } / pts.size
                val my = pts.sumOf { it.second.toDouble() } / pts.size
                val num = pts.sumOf { (it.first - mx) * (it.second - my) }
                val den = pts.sumOf { (it.first - mx) * (it.first - mx) }.coerceAtLeast(1e-6)
                (num / den).coerceIn(-0.35, 0.35)
            } else 0.0
            val b = bounds(l)
            val deg = -Math.toDegrees(kotlin.math.atan(slope)).toFloat()
            val m = Matrix()
            m.setRotate(deg, b.left, b.centerY())
            m.postTranslate(left - b.left, y - b.top)
            for (s in l) out[s.id] = s.transformed(m, 1f, deg)
            y += maxOf(b.height(), lineH) * 1.25f
        }
        return items.map { out[it.id] ?: it }
    }
}
