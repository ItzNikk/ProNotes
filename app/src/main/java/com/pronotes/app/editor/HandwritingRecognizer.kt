package com.pronotes.app.editor

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.RecognitionContext
import com.google.mlkit.vision.digitalink.WritingArea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Timed stylus point used for vector handwriting-to-text recognition.
 * Preserving real timestamps lets on-device digital ink recognition
 * distinguish cursive ligatures, stroke direction, and character boundaries.
 */
data class TimedStrokePoint(
    val x: Float,
    val y: Float,
    val tMs: Long,
)

/** Ranked handwriting recognition output with alternative candidate suggestions. */
data class HandwritingRecognitionResult(
    val text: String,
    val candidates: List<String> = emptyList(),
    val usedMlKit: Boolean = false,
)

enum class HandwritingModelState {
    IDLE,
    DOWNLOADING,
    READY,
    OFFLINE_FALLBACK,
}

/**
 * Clean-room on-device handwriting-to-text recognition engine:
 *
 * 1. Primary path: Google ML Kit Digital Ink Recognition (fully on-device vector
 *    Ink recognizer consuming stroke order, coordinates, timestamps, WritingArea,
 *    and preContext for cursive + print + punctuation + multi-line recognition).
 * 2. Offline / pre-download fallback: Resampled 32-point polyline template matcher
 *    ($1/$P-style cosine direction + spatial zoning classifier) with connected
 *    line/word segmentation so conversion never blocks or fails when offline.
 */
object HandwritingRecognizer {
    private val recognizers = ConcurrentHashMap<String, DigitalInkRecognizer>()
    private val _modelState = MutableStateFlow(HandwritingModelState.IDLE)
    val modelState: StateFlow<HandwritingModelState> = _modelState.asStateFlow()

    /** Ensures the on-device ML Kit language model is downloaded and ready. */
    suspend fun ensureModelReady(languageTag: String = "en-US"): Boolean = withContext(Dispatchers.IO) {
        val model = buildModel(languageTag) ?: return@withContext false
        val manager = RemoteModelManager.getInstance()
        val alreadyDownloaded = runCatching { manager.isModelDownloaded(model).await() }.getOrDefault(false)
        if (alreadyDownloaded) {
            _modelState.value = HandwritingModelState.READY
            return@withContext true
        }
        _modelState.value = HandwritingModelState.DOWNLOADING
        val downloaded = runCatching {
            manager.download(model, DownloadConditions.Builder().build()).await()
            true
        }.getOrDefault(false)
        _modelState.value = if (downloaded) HandwritingModelState.READY else HandwritingModelState.OFFLINE_FALLBACK
        downloaded
    }

    /**
     * Recognises timed vector strokes into text with ranked candidates.
     * Uses on-device ML Kit Digital Ink when available, falling back seamlessly
     * to the local resampled polyline template engine if offline before first download.
     */
    suspend fun recognize(
        strokes: List<List<TimedStrokePoint>>,
        writingWidth: Float = 0f,
        writingHeight: Float = 0f,
        preContext: String = "",
        languageTag: String = "en-US",
    ): HandwritingRecognitionResult = withContext(Dispatchers.Default) {
        val cleaned = strokes.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return@withContext HandwritingRecognitionResult("")

        // Tier 1: On-device Google ML Kit Digital Ink Recognition
        val mlResult = recognizeWithMlKit(
            strokes = cleaned,
            writingWidth = writingWidth,
            writingHeight = writingHeight,
            preContext = preContext,
            languageTag = languageTag,
        )
        if (mlResult != null && mlResult.text.isNotBlank()) {
            return@withContext mlResult
        }

        // Tier 2: Offline resampled vector template matcher fallback
        val fallbackText = OfflineStrokeRecognizer.recognize(cleaned)
        HandwritingRecognitionResult(
            text = fallbackText,
            candidates = if (fallbackText.isNotBlank()) listOf(fallbackText) else emptyList(),
            usedMlKit = false,
        )
    }

    private suspend fun recognizeWithMlKit(
        strokes: List<List<TimedStrokePoint>>,
        writingWidth: Float,
        writingHeight: Float,
        preContext: String,
        languageTag: String,
    ): HandwritingRecognitionResult? {
        if (!ensureModelReady(languageTag)) return null
        val model = buildModel(languageTag) ?: return null
        val recognizer = recognizers.getOrPut(languageTag) {
            DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
        }

        // Normalise origin near (0, 0) while keeping exact relative proportions & timestamps
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var baseTime = Long.MAX_VALUE
        for (stroke in strokes) {
            for (pt in stroke) {
                if (pt.x < minX) minX = pt.x
                if (pt.y < minY) minY = pt.y
                if (pt.x > maxX) maxX = pt.x
                if (pt.y > maxY) maxY = pt.y
                if (pt.tMs < baseTime) baseTime = pt.tMs
            }
        }
        if (!minX.isFinite() || !minY.isFinite()) return null
        val originT = if (baseTime == Long.MAX_VALUE) 0L else baseTime

        val inkBuilder = Ink.builder()
        var monotonicT = 0L
        for (stroke in strokes) {
            val sb = Ink.Stroke.builder()
            for (pt in stroke) {
                val relT = max(monotonicT, (pt.tMs - originT).coerceAtLeast(0L))
                monotonicT = relT
                sb.addPoint(Ink.Point.create(pt.x - minX, pt.y - minY, relT))
            }
            monotonicT += 45L
            inkBuilder.addStroke(sb.build())
        }
        val ink = inkBuilder.build()

        val areaW = if (writingWidth > 1f) writingWidth else (maxX - minX).coerceAtLeast(64f)
        val areaH = if (writingHeight > 1f) writingHeight else (maxY - minY).coerceAtLeast(48f)
        val context = RecognitionContext.builder()
            .setWritingArea(WritingArea(areaW, areaH))
            .setPreContext(preContext.takeLast(20))
            .build()

        return runCatching {
            val result = recognizer.recognize(ink, context).await()
            val ranked = result.candidates
                .map { it.text.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(5)
            val best = ranked.firstOrNull().orEmpty()
            HandwritingRecognitionResult(text = best, candidates = ranked, usedMlKit = true)
        }.getOrNull()
    }

    private fun buildModel(languageTag: String): DigitalInkRecognitionModel? {
        val identifier = runCatching {
            DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
                ?: DigitalInkRecognitionModelIdentifier.EN_US
        }.getOrNull() ?: return null
        return DigitalInkRecognitionModel.builder(identifier).build()
    }

    /** Estimates a natural canvas font size from the median stroke height of handwritten ink. */
    fun estimateFontSize(strokes: List<List<TimedStrokePoint>>, fallback: Float = 36f): Float {
        val heights = strokes.mapNotNull { s ->
            if (s.isEmpty()) null
            else {
                var lo = Float.MAX_VALUE; var hi = -Float.MAX_VALUE
                for (p in s) { if (p.y < lo) lo = p.y; if (p.y > hi) hi = p.y }
                val h = hi - lo
                if (h >= 8f) h else null
            }
        }.sorted()
        if (heights.isEmpty()) return fallback
        val median = heights[heights.size / 2]
        return (median * 1.15f).coerceIn(24f, 68f)
    }
}

/**
 * Offline fallback recognizer using connected vertical line grouping, spatial character
 * clustering, and 32-point resampled directional template matching ($1-inspired).
 * Used only when the on-device ML Kit Digital Ink pack has not been downloaded yet.
 */
internal object OfflineStrokeRecognizer {
    private const val RESAMPLE_N = 32

    private data class BoxedStroke(
        val pts: List<Pair<Float, Float>>,
        val minX: Float,
        val maxX: Float,
        val minY: Float,
        val maxY: Float,
    ) {
        val w: Float get() = (maxX - minX).coerceAtLeast(1f)
        val h: Float get() = (maxY - minY).coerceAtLeast(1f)
        val cx: Float get() = (minX + maxX) * 0.5f
        val cy: Float get() = (minY + maxY) * 0.5f
    }

    private data class GlyphTemplate(
        val ch: Char,
        val vec: FloatArray,
        val aspectMin: Float = 0.05f,
        val aspectMax: Float = 6f,
    )

    private val templates: List<GlyphTemplate> by lazy { buildTemplates() }

    fun recognize(strokes: List<List<TimedStrokePoint>>): String {
        val boxed = strokes.mapNotNull { s ->
            if (s.isEmpty()) null
            else {
                var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
                var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
                val pts = ArrayList<Pair<Float, Float>>(s.size)
                for (p in s) {
                    pts.add(p.x to p.y)
                    if (p.x < minX) minX = p.x
                    if (p.x > maxX) maxX = p.x
                    if (p.y < minY) minY = p.y
                    if (p.y > maxY) maxY = p.y
                }
                BoxedStroke(pts, minX, maxX, minY, maxY)
            }
        }
        if (boxed.isEmpty()) return ""

        // 1. Group strokes into natural writing lines using vertical overlap instead of rigid grid slicing
        val sortedByY = boxed.sortedBy { it.cy }
        val lines = mutableListOf<MutableList<BoxedStroke>>()
        for (sb in sortedByY) {
            val match = lines.firstOrNull { line ->
                val lTop = line.minOf { it.minY }
                val lBot = line.maxOf { it.maxY }
                val lHeight = (lBot - lTop).coerceAtLeast(24f)
                val vOverlap = min(lBot, sb.maxY) - max(lTop, sb.minY)
                vOverlap > -lHeight * 0.12f && abs(sb.cy - (lTop + lBot) * 0.5f) < lHeight * 0.85f
            }
            if (match != null) match.add(sb) else lines.add(mutableListOf(sb))
        }
        lines.sortBy { line -> line.minOf { it.minY } }

        // 2. Segment characters & words on each line
        val outLines = mutableListOf<String>()
        for (line in lines) {
            val ordered = line.sortedBy { it.minX }
            val lineHeight = (ordered.maxOf { it.maxY } - ordered.minOf { it.minY }).coerceAtLeast(20f)
            val clusters = mutableListOf<MutableList<BoxedStroke>>()
            for (sb in ordered) {
                val prev = clusters.lastOrNull()
                if (prev == null) {
                    clusters.add(mutableListOf(sb))
                } else {
                    val pMinX = prev.minOf { it.minX }
                    val pMaxX = prev.maxOf { it.maxX }
                    val overlap = min(pMaxX, sb.maxX) - max(pMinX, sb.minX)
                    val minW = min(pMaxX - pMinX, sb.w).coerceAtLeast(1f)
                    val isDotOrCross = (sb.w < lineHeight * 0.35f && sb.h < lineHeight * 0.35f && abs(sb.cx - (pMinX + pMaxX) * 0.5f) < lineHeight * 0.55f)
                    if (overlap > -3f && (overlap / minW > 0.24f || isDotOrCross)) {
                        prev.add(sb)
                    } else {
                        clusters.add(mutableListOf(sb))
                    }
                }
            }

            val widths = clusters.map { c -> (c.maxOf { it.maxX } - c.minOf { it.minX }).coerceAtLeast(8f) }.sorted()
            val medianW = widths[widths.size / 2].coerceIn(14f, 64f)
            val wordGap = max(medianW * 0.82f, lineHeight * 0.45f)

            val sb = StringBuilder()
            var prevRight = Float.NaN
            var wordStart = true
            for (cluster in clusters) {
                val cLeft = cluster.minOf { it.minX }
                val cRight = cluster.maxOf { it.maxX }
                val cTop = cluster.minOf { it.minY }
                val cBot = cluster.maxOf { it.maxY }
                if (!prevRight.isNaN() && (cLeft - prevRight) > wordGap) {
                    sb.append(' ')
                    wordStart = true
                }
                sb.append(classifyCluster(cluster, cRight - cLeft, cBot - cTop, lineHeight, wordStart))
                prevRight = if (prevRight.isNaN()) cRight else max(prevRight, cRight)
                wordStart = false
            }
            val text = sb.toString().trim()
            if (text.isNotEmpty()) outLines.add(text)
        }
        return outLines.joinToString("\n")
    }

    private fun classifyCluster(
        cluster: List<BoxedStroke>,
        w: Float,
        h: Float,
        lineHeight: Float,
        wordStart: Boolean,
    ): Char {
        val aspect = w / h.coerceAtLeast(1f)
        if (w < lineHeight * 0.22f && h < lineHeight * 0.22f) return '.'
        if (aspect > 2.4f && h < lineHeight * 0.28f) return '-'

        // Multi-stroke structural shortcuts (i, t, x, +, =, H, A, E, F)
        if (cluster.size == 2) {
            val a = cluster[0]; val b = cluster[1]
            val smallH = min(a.h, b.h); val tallH = max(a.h, b.h)
            val smallW = min(a.w, b.w); val wideW = max(a.w, b.w)
            // Dotted 'i'
            if (smallH < tallH * 0.34f && smallW < tallH * 0.34f && tallH > lineHeight * 0.32f) return 'i'
            // '=' two parallel horizontal strokes
            if (a.w > a.h * 1.8f && b.w > b.h * 1.8f) return '='
            // '+' or 't' (one horizontal crossbar + one vertical stem)
            val hasHoriz = (a.w > a.h * 1.5f) || (b.w > b.h * 1.5f)
            val hasVert = (a.h > a.w * 1.5f) || (b.h > b.w * 1.5f)
            if (hasHoriz && hasVert) {
                val stem = if (a.h >= b.h) a else b
                val bar = if (stem === a) b else a
                return if (abs(bar.cy - stem.cy) < stem.h * 0.18f && wideW > tallH * 0.65f) '+' else 't'
            }
            // 'x' two diagonals
            if (a.h > a.w * 0.55f && b.h > b.w * 0.55f && aspect in 0.55f..1.45f) return 'x'
        }

        // Single-stroke or concatenated polyline matching against resampled canonical vector templates
        val merged = cluster.flatMap { it.pts }
        if (merged.size < 2) return '.'
        if (aspect < 0.28f && h > lineHeight * 0.45f) return if (wordStart) 'I' else 'l'

        val candidateVec = toFeatureVector(merged) ?: return if (wordStart) 'I' else 'l'
        var bestChar = 'e'
        var bestScore = -Float.MAX_VALUE
        for (tpl in templates) {
            if (aspect < tpl.aspectMin || aspect > tpl.aspectMax) continue
            val score = cosineSim(candidateVec, tpl.vec)
            if (score > bestScore) {
                bestScore = score
                bestChar = tpl.ch
            }
        }
        return if (wordStart && h > lineHeight * 0.72f && bestChar.isLowerCase()) bestChar.uppercaseChar() else bestChar
    }

    private fun toFeatureVector(pts: List<Pair<Float, Float>>): FloatArray? {
        val resampled = resample(pts, RESAMPLE_N)
        if (resampled.size < RESAMPLE_N) return null
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for ((x, y) in resampled) {
            if (x < minX) minX = x; if (x > maxX) maxX = x
            if (y < minY) minY = y; if (y > maxY) maxY = y
        }
        val scale = max(maxX - minX, maxY - minY).coerceAtLeast(1f)
        val cx = (minX + maxX) * 0.5f
        val cy = (minY + maxY) * 0.5f

        // Pack normalised coordinates + local tangent direction (cos, sin)
        val vec = FloatArray(RESAMPLE_N * 4)
        for (i in 0 until RESAMPLE_N) {
            val nx = (resampled[i].first - cx) / scale
            val ny = (resampled[i].second - cy) / scale
            val prev = resampled[max(0, i - 1)]
            val next = resampled[min(RESAMPLE_N - 1, i + 1)]
            val dx = next.first - prev.first
            val dy = next.second - prev.second
            val d = hypot(dx, dy).coerceAtLeast(1e-4f)
            vec[i * 4] = nx
            vec[i * 4 + 1] = ny
            vec[i * 4 + 2] = (dx / d) * 0.75f
            vec[i * 4 + 3] = (dy / d) * 0.75f
        }
        // L2 normalise
        var norm = 0f
        for (v in vec) norm += v * v
        val inv = 1f / sqrt(norm.coerceAtLeast(1e-6f))
        for (i in vec.indices) vec[i] *= inv
        return vec
    }

    private fun cosineSim(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }

    private fun resample(pts: List<Pair<Float, Float>>, n: Int): List<Pair<Float, Float>> {
        if (pts.size <= 1) return pts
        var total = 0f
        for (i in 1 until pts.size) total += hypot(pts[i].first - pts[i - 1].first, pts[i].second - pts[i - 1].second)
        if (total < 1e-3f) return List(n) { pts.first() }
        val step = total / (n - 1)
        val out = ArrayList<Pair<Float, Float>>(n)
        out.add(pts.first())
        var acc = 0f
        var prev = pts.first()
        var idx = 1
        while (idx < pts.size && out.size < n) {
            val cur = pts[idx]
            val d = hypot(cur.first - prev.first, cur.second - prev.second)
            if (acc + d >= step) {
                val t = ((step - acc) / d.coerceAtLeast(1e-4f)).coerceIn(0f, 1f)
                val nx = prev.first + t * (cur.first - prev.first)
                val ny = prev.second + t * (cur.second - prev.second)
                val np = nx to ny
                out.add(np)
                prev = np
                acc = 0f
            } else {
                acc += d
                prev = cur
                idx++
            }
        }
        while (out.size < n) out.add(pts.last())
        return out
    }

    private fun arcPts(cx: Float, cy: Float, rx: Float, ry: Float, startDeg: Float, endDeg: Float, steps: Int = 20): List<Pair<Float, Float>> =
        List(steps + 1) { i ->
            val deg = startDeg + (endDeg - startDeg) * (i / steps.toFloat())
            val rad = Math.toRadians(deg.toDouble())
            (cx + rx * cos(rad).toFloat()) to (cy + ry * sin(rad).toFloat())
        }

    private fun buildTemplates(): List<GlyphTemplate> {
        val raw = listOf(
            Triple('o', arcPts(0.5f, 0.5f, 0.45f, 0.45f, -90f, -450f), 0.55f to 1.6f),
            Triple('o', arcPts(0.5f, 0.5f, 0.45f, 0.45f, -90f, 270f), 0.55f to 1.6f),
            Triple('c', arcPts(0.55f, 0.5f, 0.45f, 0.45f, -40f, -320f), 0.45f to 1.6f),
            Triple('e', listOf(0.15f to 0.5f, 0.85f to 0.48f) + arcPts(0.5f, 0.5f, 0.42f, 0.44f, -10f, -315f), 0.5f to 1.6f),
            Triple('a', arcPts(0.45f, 0.55f, 0.36f, 0.36f, -35f, -380f) + listOf(0.82f to 0.25f, 0.85f to 0.92f), 0.5f to 1.5f),
            Triple('s', arcPts(0.5f, 0.28f, 0.32f, 0.22f, -20f, -260f) + arcPts(0.5f, 0.72f, 0.32f, 0.22f, -90f, 160f), 0.35f to 1.4f),
            Triple('u', listOf(0.15f to 0.15f, 0.18f to 0.75f, 0.48f to 0.92f, 0.82f to 0.72f, 0.82f to 0.15f, 0.85f to 0.92f), 0.5f to 1.8f),
            Triple('n', listOf(0.15f to 0.18f, 0.15f to 0.9f, 0.20f to 0.35f, 0.52f to 0.15f, 0.82f to 0.35f, 0.85f to 0.9f), 0.5f to 1.8f),
            Triple('m', listOf(0.08f to 0.2f, 0.08f to 0.9f, 0.24f to 0.22f, 0.48f to 0.22f, 0.50f to 0.9f, 0.66f to 0.22f, 0.90f to 0.25f, 0.92f to 0.9f), 0.8f to 2.8f),
            Triple('w', listOf(0.08f to 0.15f, 0.25f to 0.88f, 0.50f to 0.32f, 0.74f to 0.88f, 0.92f to 0.15f), 0.75f to 2.6f),
            Triple('v', listOf(0.12f to 0.12f, 0.50f to 0.90f, 0.88f to 0.12f), 0.5f to 1.8f),
            Triple('r', listOf(0.22f to 0.20f, 0.22f to 0.90f, 0.26f to 0.42f, 0.55f to 0.20f, 0.84f to 0.24f), 0.35f to 1.4f),
            Triple('z', listOf(0.14f to 0.16f, 0.86f to 0.16f, 0.14f to 0.86f, 0.86f to 0.86f), 0.5f to 1.7f),
            Triple('h', listOf(0.18f to 0.06f, 0.18f to 0.92f, 0.22f to 0.54f, 0.56f to 0.42f, 0.82f to 0.58f, 0.84f to 0.92f), 0.3f to 1.1f),
            Triple('b', listOf(0.20f to 0.06f, 0.20f to 0.92f) + arcPts(0.48f, 0.68f, 0.30f, 0.24f, 180f, 520f), 0.3f to 1.1f),
            Triple('d', arcPts(0.46f, 0.66f, 0.30f, 0.24f, -30f, -360f) + listOf(0.78f to 0.06f, 0.78f to 0.92f), 0.3f to 1.1f),
            Triple('p', listOf(0.22f to 0.10f, 0.22f to 0.96f, 0.24f to 0.16f) + arcPts(0.50f, 0.34f, 0.30f, 0.22f, -150f, 150f), 0.3f to 1.1f),
            Triple('g', arcPts(0.50f, 0.34f, 0.28f, 0.22f, -30f, -360f) + listOf(0.78f to 0.24f, 0.76f to 0.86f, 0.52f to 0.96f, 0.26f to 0.88f), 0.3f to 1.1f),
            Triple('y', listOf(0.18f to 0.12f, 0.48f to 0.55f, 0.82f to 0.12f, 0.42f to 0.94f), 0.35f to 1.3f),
            Triple('2', arcPts(0.50f, 0.30f, 0.32f, 0.22f, -160f, 35f) + listOf(0.16f to 0.88f, 0.88f to 0.88f), 0.4f to 1.4f),
            Triple('3', arcPts(0.46f, 0.28f, 0.30f, 0.20f, -150f, 90f) + arcPts(0.46f, 0.72f, 0.32f, 0.22f, -90f, 160f), 0.35f to 1.3f),
            Triple('7', listOf(0.14f to 0.14f, 0.86f to 0.14f, 0.40f to 0.92f), 0.4f to 1.4f),
            Triple('L', listOf(0.20f to 0.10f, 0.20f to 0.88f, 0.84f to 0.88f), 0.4f to 1.5f),
        )
        return raw.mapNotNull { (ch, pts, aspect) ->
            toFeatureVector(pts)?.let { GlyphTemplate(ch, it, aspect.first, aspect.second) }
        }
    }
}
