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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
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

data class HandwritingLanguage(
    val tag: String,
    val name: String,
    val nativeName: String,
    val region: String,
    val isRtl: Boolean = false,
    val mlKitTag: String = tag,
)

/**
 * Upgraded on-device handwriting-to-text recognition engine:
 *
 * 1. Preprocessing: corner-preserving stylus jitter smoothing, dot preservation,
 *    multi-line reading-order segmentation, and optimal scale/WritingArea normalization.
 * 2. Primary path: Google ML Kit Digital Ink Recognition across 58+ world languages,
 *    paired with a geometric math/symbol classifier and contextual candidate reranker
 *    for numbers, equations, arrows, currency, punctuation, and scientific symbols.
 * 3. Offline / pre-download engine: Rich 90+ template directional + spatial-zone +
 *    multi-stroke structural classifier covering A-Z, a-z, 0-9, math, arrows, and symbols.
 */
object HandwritingRecognizer {
    /** Symbols that are valid standalone recognition outputs (e.g. for Auto-Text & math notes). */
    val RECOGNIZABLE_SYMBOLS: Set<Char> = setOf(
        '+', '-', '=', '×', '÷', '±', '≠', '≤', '≥', '≈', '≡',
        '<', '>', '%', '$', '₹', '€', '£', '¥', '#', '@', '&',
        '?', '!', ':', ';', ',', '.', '(', ')', '[', ']', '{', '}',
        '/', '\\', '*', '^', '~', '_', '|', '°', '√', '∞', '✓',
        '→', '←', '↑', '↓', 'π', 'Δ', 'Σ', 'θ', 'α', 'β', 'λ', 'μ', 'Ω',
    )

    val supportedLanguages: List<HandwritingLanguage> = listOf(
        // Popular
        HandwritingLanguage("en-US", "English (US)", "English (US)", "Popular", mlKitTag = "en-US"),
        HandwritingLanguage("en-GB", "English (UK)", "English (UK)", "Popular", mlKitTag = "en-GB"),
        HandwritingLanguage("en-IN", "English (India)", "English (India)", "Popular", mlKitTag = "en-IN"),
        HandwritingLanguage("en-AU", "English (Australia)", "English (Australia)", "Popular", mlKitTag = "en-AU"),
        HandwritingLanguage("es", "Spanish", "Español", "Popular", mlKitTag = "es"),
        HandwritingLanguage("fr", "French", "Français", "Popular", mlKitTag = "fr"),
        HandwritingLanguage("de", "German", "Deutsch", "Popular", mlKitTag = "de"),
        HandwritingLanguage("hi", "Hindi", "हिन्दी", "Popular", mlKitTag = "hi"),
        HandwritingLanguage("zh-Hani-CN", "Chinese (Simplified)", "简体中文", "Popular", mlKitTag = "zh-Hani-CN"),
        HandwritingLanguage("zh-Hani-TW", "Chinese (Traditional)", "繁體中文", "Popular", mlKitTag = "zh-Hani-TW"),
        HandwritingLanguage("ja", "Japanese", "日本語", "Popular", mlKitTag = "ja"),
        HandwritingLanguage("ko", "Korean", "한국어", "Popular", mlKitTag = "ko"),
        HandwritingLanguage("ar", "Arabic", "العربية", "Popular", isRtl = true, mlKitTag = "ar"),
        HandwritingLanguage("pt", "Portuguese", "Português", "Popular", mlKitTag = "pt"),
        HandwritingLanguage("it", "Italian", "Italiano", "Popular", mlKitTag = "it"),
        HandwritingLanguage("ru", "Russian", "Русский", "Popular", mlKitTag = "ru"),
        // South Asia
        HandwritingLanguage("bn", "Bengali", "বাংলা", "South Asia", mlKitTag = "bn"),
        HandwritingLanguage("ta", "Tamil", "தமிழ்", "South Asia", mlKitTag = "ta"),
        HandwritingLanguage("te", "Telugu", "తెలుగు", "South Asia", mlKitTag = "te"),
        HandwritingLanguage("mr", "Marathi", "मराठी", "South Asia", mlKitTag = "mr"),
        HandwritingLanguage("gu", "Gujarati", "ગુજરાતી", "South Asia", mlKitTag = "gu"),
        HandwritingLanguage("kn", "Kannada", "ಕನ್ನಡ", "South Asia", mlKitTag = "kn"),
        HandwritingLanguage("ml", "Malayalam", "മലയാളം", "South Asia", mlKitTag = "ml"),
        HandwritingLanguage("pa", "Punjabi", "ਪੰਜਾਬੀ", "South Asia", mlKitTag = "pa"),
        HandwritingLanguage("ur", "Urdu", "اردو", "South Asia", isRtl = true, mlKitTag = "ur"),
        HandwritingLanguage("ne", "Nepali", "नेपाली", "South Asia", mlKitTag = "ne"),
        HandwritingLanguage("si", "Sinhala", "සිංහල", "South Asia", mlKitTag = "si"),
        // East & Southeast Asia
        HandwritingLanguage("id", "Indonesian", "Bahasa Indonesia", "East & SE Asia", mlKitTag = "id"),
        HandwritingLanguage("ms", "Malay", "Bahasa Melayu", "East & SE Asia", mlKitTag = "ms"),
        HandwritingLanguage("vi", "Vietnamese", "Tiếng Việt", "East & SE Asia", mlKitTag = "vi"),
        HandwritingLanguage("th", "Thai", "ไทย", "East & SE Asia", mlKitTag = "th"),
        HandwritingLanguage("fil", "Filipino", "Filipino / Tagalog", "East & SE Asia", mlKitTag = "fil"),
        HandwritingLanguage("my", "Burmese", "မြန်မာ", "East & SE Asia", mlKitTag = "my"),
        HandwritingLanguage("km", "Khmer", "ខ្មែរ", "East & SE Asia", mlKitTag = "km"),
        HandwritingLanguage("lo", "Lao", "ລາວ", "East & SE Asia", mlKitTag = "lo"),
        // Europe & Americas
        HandwritingLanguage("pt-BR", "Portuguese (Brazil)", "Português (Brasil)", "Europe & Americas", mlKitTag = "pt"),
        HandwritingLanguage("es-MX", "Spanish (Mexico)", "Español (México)", "Europe & Americas", mlKitTag = "es"),
        HandwritingLanguage("nl", "Dutch", "Nederlands", "Europe & Americas", mlKitTag = "nl"),
        HandwritingLanguage("pl", "Polish", "Polski", "Europe & Americas", mlKitTag = "pl"),
        HandwritingLanguage("tr", "Turkish", "Türkçe", "Europe & Americas", mlKitTag = "tr"),
        HandwritingLanguage("uk", "Ukrainian", "Українська", "Europe & Americas", mlKitTag = "uk"),
        HandwritingLanguage("sv", "Swedish", "Svenska", "Europe & Americas", mlKitTag = "sv"),
        HandwritingLanguage("no", "Norwegian", "Norsk", "Europe & Americas", mlKitTag = "no"),
        HandwritingLanguage("da", "Danish", "Dansk", "Europe & Americas", mlKitTag = "da"),
        HandwritingLanguage("fi", "Finnish", "Suomi", "Europe & Americas", mlKitTag = "fi"),
        HandwritingLanguage("cs", "Czech", "Čeština", "Europe & Americas", mlKitTag = "cs"),
        HandwritingLanguage("sk", "Slovak", "Slovenčina", "Europe & Americas", mlKitTag = "sk"),
        HandwritingLanguage("ro", "Romanian", "Română", "Europe & Americas", mlKitTag = "ro"),
        HandwritingLanguage("hu", "Hungarian", "Magyar", "Europe & Americas", mlKitTag = "hu"),
        HandwritingLanguage("el", "Greek", "Ελληνικά", "Europe & Americas", mlKitTag = "el"),
        HandwritingLanguage("bg", "Bulgarian", "Български", "Europe & Americas", mlKitTag = "bg"),
        HandwritingLanguage("hr", "Croatian", "Hrvatski", "Europe & Americas", mlKitTag = "hr"),
        HandwritingLanguage("sr-Cyrl", "Serbian", "Српски", "Europe & Americas", mlKitTag = "sr-Cyrl"),
        HandwritingLanguage("ca", "Catalan", "Català", "Europe & Americas", mlKitTag = "ca"),
        // Middle East & Africa
        HandwritingLanguage("fa", "Persian", "فارسی", "Middle East & Africa", isRtl = true, mlKitTag = "fa"),
        HandwritingLanguage("he", "Hebrew", "עברית", "Middle East & Africa", isRtl = true, mlKitTag = "he"),
        HandwritingLanguage("sw", "Swahili", "Kiswahili", "Middle East & Africa", mlKitTag = "sw"),
        HandwritingLanguage("am", "Amharic", "አማርኛ", "Middle East & Africa", mlKitTag = "am"),
    )

    val quickLanguages: List<HandwritingLanguage> = supportedLanguages.take(10)

    val languages: LinkedHashMap<String, String> = LinkedHashMap<String, String>().apply {
        supportedLanguages.forEach { put(it.tag, it.name) }
    }

    private val recognizers = ConcurrentHashMap<String, DigitalInkRecognizer>()
    private val _modelState = MutableStateFlow(HandwritingModelState.IDLE)
    val modelState: StateFlow<HandwritingModelState> = _modelState.asStateFlow()

    private val _downloadedTags = MutableStateFlow<Set<String>>(emptySet())
    val downloadedTags: StateFlow<Set<String>> = _downloadedTags.asStateFlow()

    private val _downloadingTags = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTags: StateFlow<Set<String>> = _downloadingTags.asStateFlow()

    fun findLanguage(tag: String): HandwritingLanguage {
        val norm = normalizeTag(tag)
        return supportedLanguages.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
            ?: supportedLanguages.firstOrNull { it.tag.equals(norm, ignoreCase = true) || it.mlKitTag.equals(norm, ignoreCase = true) }
            ?: supportedLanguages.firstOrNull { it.tag.substringBefore('-').equals(tag.substringBefore('-'), ignoreCase = true) }
            ?: supportedLanguages.first()
    }

    fun displayName(tag: String): String = findLanguage(tag).name

    fun normalizeTag(rawTag: String): String {
        val clean = rawTag.trim()
        return when (clean.lowercase()) {
            "zh", "zh-cn", "zh-hans", "zh-sg" -> "zh-Hani-CN"
            "zh-tw", "zh-hk", "zh-hant", "zh-mo" -> "zh-Hani-TW"
            "hi-in" -> "hi"
            "es-es" -> "es"
            "fr-fr" -> "fr"
            "de-de" -> "de"
            "it-it" -> "it"
            "ja-jp" -> "ja"
            "ko-kr" -> "ko"
            "iw", "iw-il", "he-il" -> "he"
            "in", "in-id", "id-id" -> "id"
            "tl", "tl-ph" -> "fil"
            else -> clean.ifBlank { "en-US" }
        }
    }

    suspend fun refreshDownloadedModels() = withContext(Dispatchers.IO) {
        val manager = RemoteModelManager.getInstance()
        val ready = mutableSetOf<String>()
        for (lang in supportedLanguages) {
            val model = buildModel(lang.mlKitTag) ?: continue
            val isReady = runCatching { manager.isModelDownloaded(model).await() }.getOrDefault(false)
            if (isReady) ready.add(lang.tag)
        }
        _downloadedTags.value = ready
    }

    suspend fun downloadLanguage(languageTag: String): Boolean = withContext(Dispatchers.IO) {
        val lang = findLanguage(languageTag)
        _downloadingTags.value = _downloadingTags.value + lang.tag
        try {
            val ok = ensureModelReady(lang.mlKitTag)
            if (ok) _downloadedTags.value = _downloadedTags.value + lang.tag
            ok
        } finally {
            _downloadingTags.value = _downloadingTags.value - lang.tag
        }
    }

    suspend fun deleteLanguageModel(languageTag: String): Boolean = withContext(Dispatchers.IO) {
        val lang = findLanguage(languageTag)
        val model = buildModel(lang.mlKitTag) ?: return@withContext false
        val ok = runCatching {
            RemoteModelManager.getInstance().deleteDownloadedModel(model).await()
            true
        }.getOrDefault(false)
        if (ok) {
            recognizers.remove(lang.tag)
            recognizers.remove(lang.mlKitTag)
            _downloadedTags.value = _downloadedTags.value - lang.tag
        }
        ok
    }

    suspend fun ensureModelReady(languageTag: String = "en-US"): Boolean = withContext(Dispatchers.IO) {
        val resolvedTag = findLanguage(languageTag).mlKitTag
        val model = buildModel(resolvedTag) ?: return@withContext false
        val manager = RemoteModelManager.getInstance()
        val alreadyDownloaded = runCatching { manager.isModelDownloaded(model).await() }.getOrDefault(false)
        if (alreadyDownloaded) {
            _modelState.value = HandwritingModelState.READY
            _downloadedTags.value = _downloadedTags.value + findLanguage(languageTag).tag
            return@withContext true
        }
        _modelState.value = HandwritingModelState.DOWNLOADING
        val downloaded = runCatching {
            manager.download(model, DownloadConditions.Builder().build()).await()
            true
        }.getOrDefault(false)
        if (downloaded) {
            _downloadedTags.value = _downloadedTags.value + findLanguage(languageTag).tag
        }
        _modelState.value = if (downloaded) HandwritingModelState.READY else HandwritingModelState.OFFLINE_FALLBACK
        downloaded
    }

    /**
     * Recognises timed vector strokes into text with ranked candidates.
     * Features:
     * - Corner-preserving stylus denoising & dot preservation
     * - Multi-line spatial segmentation with rolling context
     * - Optimal scale & padded WritingArea normalization for ML Kit Digital Ink
     * - Dedicated geometric symbol & math classifier + context-aware candidate reranking
     * - Full 90+ glyph offline fallback covering A-Z, a-z, 0-9, math, arrows, and symbols
     */
    suspend fun recognize(
        strokes: List<List<TimedStrokePoint>>,
        writingWidth: Float = 0f,
        writingHeight: Float = 0f,
        preContext: String = "",
        languageTag: String = "en-US",
        allowFallback: Boolean = true,
    ): HandwritingRecognitionResult = withContext(Dispatchers.Default) {
        val cleaned = preprocessStrokes(strokes)
        if (cleaned.isEmpty()) return@withContext HandwritingRecognitionResult("")

        val isLatinOrMathFriendly = findLanguage(languageTag).let { !it.isRtl }

        // Split multi-line ink blocks cleanly so ML Kit never merges lines or loses symbols/numbers
        val lineGroups = splitIntoReadingLines(cleaned)
        if (lineGroups.size > 1) {
            val lineTexts = ArrayList<String>(lineGroups.size)
            val altPerLine = ArrayList<List<String>>(lineGroups.size)
            var usedMl = false
            var rollingCtx = preContext.takeLast(20)
            for (lineStrokes in lineGroups) {
                val res = recognizeSingleLine(
                    strokes = lineStrokes,
                    writingWidth = writingWidth,
                    writingHeight = 0f,
                    preContext = rollingCtx,
                    languageTag = languageTag,
                    allowFallback = allowFallback,
                    boostSymbols = isLatinOrMathFriendly,
                )
                if (res.text.isNotBlank()) {
                    lineTexts.add(res.text)
                    altPerLine.add(res.candidates.ifEmpty { listOf(res.text) })
                    rollingCtx = (rollingCtx + " " + res.text).trim().takeLast(20)
                    if (res.usedMlKit) usedMl = true
                }
            }
            val joined = lineTexts.joinToString("\n")
            if (joined.isBlank()) return@withContext HandwritingRecognitionResult("")
            val candidates = LinkedHashSet<String>().apply {
                add(joined)
                // Build a few top-ranked multi-line alternatives from lines that have runner-up readings
                for (li in altPerLine.indices) {
                    val alts = altPerLine[li]
                    if (alts.size > 1) {
                        val copy = lineTexts.toMutableList()
                        copy[li] = alts[1]
                        add(copy.joinToString("\n"))
                    }
                    if (size >= 5) break
                }
            }.toList()
            return@withContext HandwritingRecognitionResult(
                text = joined,
                candidates = candidates,
                usedMlKit = usedMl,
            )
        }

        recognizeSingleLine(
            strokes = cleaned,
            writingWidth = writingWidth,
            writingHeight = writingHeight,
            preContext = preContext,
            languageTag = languageTag,
            allowFallback = allowFallback,
            boostSymbols = isLatinOrMathFriendly,
        )
    }

    private suspend fun recognizeSingleLine(
        strokes: List<List<TimedStrokePoint>>,
        writingWidth: Float,
        writingHeight: Float,
        preContext: String,
        languageTag: String,
        allowFallback: Boolean,
        boostSymbols: Boolean,
    ): HandwritingRecognitionResult {
        // Compute direct geometric symbol evidence for short symbol/math clusters (1..4 strokes)
        val geoHit = if (boostSymbols && strokes.size in 1..4) {
            GeometricSymbolClassifier.classifyIsolated(strokes)
        } else null

        // Tier 1: On-device Google ML Kit Digital Ink Recognition with optimal normalization
        val mlResult = recognizeWithMlKit(
            strokes = strokes,
            writingWidth = writingWidth,
            writingHeight = writingHeight,
            preContext = preContext,
            languageTag = languageTag,
        )
        if (mlResult != null && mlResult.candidates.isNotEmpty()) {
            val reranked = rerankAndNormalizeCandidates(
                rawCandidates = mlResult.candidates,
                strokes = strokes,
                geoHit = geoHit,
                preContext = preContext,
                isEnglishLike = languageTag.startsWith("en", ignoreCase = true) || boostSymbols,
            )
            val best = reranked.firstOrNull().orEmpty()
            if (best.isNotBlank()) {
                return HandwritingRecognitionResult(
                    text = best,
                    candidates = reranked.take(6),
                    usedMlKit = true,
                )
            }
        }

        if (!allowFallback) {
            if (geoHit != null && geoHit.confidence >= 0.88f) {
                return HandwritingRecognitionResult(
                    text = geoHit.symbol,
                    candidates = listOf(geoHit.symbol),
                    usedMlKit = false,
                )
            }
            return HandwritingRecognitionResult("")
        }

        // Tier 2: Comprehensive offline recognizer (A-Z, a-z, 0-9, math, arrows & symbols)
        val offlineRes = OfflineStrokeRecognizer.recognizeWithCandidates(strokes)
        val combinedCandidates = LinkedHashSet<String>().apply {
            if (geoHit != null && geoHit.confidence >= 0.85f) add(geoHit.symbol)
            offlineRes.forEach { if (it.isNotBlank()) add(it) }
        }.toList().take(6)
        val best = combinedCandidates.firstOrNull().orEmpty()
        return HandwritingRecognitionResult(
            text = best,
            candidates = combinedCandidates,
            usedMlKit = false,
        )
    }

    /**
     * Lightly denoises high-frequency stylus jitter while strictly preserving corners and dots.
     */
    private fun preprocessStrokes(raw: List<List<TimedStrokePoint>>): List<List<TimedStrokePoint>> {
        val nonEmpty = raw.filter { it.isNotEmpty() }
        if (nonEmpty.isEmpty()) return emptyList()

        // Estimate scale for adaptive minimum-step deduplication
        val extents = nonEmpty.mapNotNull { s ->
            var loX = Float.MAX_VALUE; var hiX = -Float.MAX_VALUE
            var loY = Float.MAX_VALUE; var hiY = -Float.MAX_VALUE
            for (p in s) {
                if (p.x < loX) loX = p.x; if (p.x > hiX) hiX = p.x
                if (p.y < loY) loY = p.y; if (p.y > hiY) hiY = p.y
            }
            val span = max(hiX - loX, hiY - loY)
            if (span > 4f) span else null
        }.sorted()
        val refSize = if (extents.isNotEmpty()) extents[extents.size / 2] else 48f
        val minStep = (refSize * 0.012f).coerceIn(0.45f, 2.2f)

        return nonEmpty.map { stroke ->
            if (stroke.size == 1) {
                // Turn single-tap dots into a crisp 2-point micro-stroke so ML Kit keeps periods/i-dots
                val p = stroke[0]
                listOf(p, TimedStrokePoint(p.x + 0.5f, p.y + 0.5f, p.tMs + 12L))
            } else {
                // Step 1: deduplicate micro-jitter points
                val dedup = ArrayList<TimedStrokePoint>(stroke.size)
                dedup.add(stroke.first())
                for (i in 1 until stroke.lastIndex) {
                    val cur = stroke[i]
                    val prev = dedup.last()
                    if (hypot(cur.x - prev.x, cur.y - prev.y) >= minStep) {
                        dedup.add(cur)
                    }
                }
                val last = stroke.last()
                if (hypot(last.x - dedup.last().x, last.y - dedup.last().y) >= 0.1f || dedup.size == 1) {
                    dedup.add(last)
                }
                if (dedup.size < 4) return@map dedup

                // Step 2: corner-preserving gentle 3-point smoothing
                val smoothed = ArrayList<TimedStrokePoint>(dedup.size)
                smoothed.add(dedup.first())
                for (i in 1 until dedup.lastIndex) {
                    val a = dedup[i - 1]
                    val b = dedup[i]
                    val c = dedup[i + 1]
                    val v1x = b.x - a.x; val v1y = b.y - a.y
                    val v2x = c.x - b.x; val v2y = c.y - b.y
                    val d1 = hypot(v1x, v1y)
                    val d2 = hypot(v2x, v2y)
                    if (d1 > 1e-3f && d2 > 1e-3f) {
                        val cosA = ((v1x * v2x + v1y * v2y) / (d1 * d2)).coerceIn(-1f, 1f)
                        val angleDeg = acos(cosA) * (180f / PI.toFloat())
                        if (angleDeg < 38f) {
                            smoothed.add(
                                TimedStrokePoint(
                                    x = a.x * 0.2f + b.x * 0.6f + c.x * 0.2f,
                                    y = a.y * 0.2f + b.y * 0.6f + c.y * 0.2f,
                                    tMs = b.tMs,
                                ),
                            )
                            continue
                        }
                    }
                    smoothed.add(b)
                }
                smoothed.add(dedup.last())
                smoothed
            }
        }
    }

    /**
     * Splits a multi-line ink selection into horizontal reading lines while attaching
     * dots, diacritics, minus/equal bars, and punctuation to their parent line.
     */
    private fun splitIntoReadingLines(strokes: List<List<TimedStrokePoint>>): List<List<List<TimedStrokePoint>>> {
        if (strokes.size <= 2) return listOf(strokes)
        data class SBox(
            val idx: Int,
            val pts: List<TimedStrokePoint>,
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

        val boxes = strokes.mapIndexed { idx, s ->
            var loX = Float.MAX_VALUE; var hiX = -Float.MAX_VALUE
            var loY = Float.MAX_VALUE; var hiY = -Float.MAX_VALUE
            for (p in s) {
                if (p.x < loX) loX = p.x; if (p.x > hiX) hiX = p.x
                if (p.y < loY) loY = p.y; if (p.y > hiY) hiY = p.y
            }
            SBox(idx, s, loX, hiX, loY, hiY)
        }

        val substantialHeights = boxes.map { it.h }.filter { it >= 10f }.sorted()
        val medianH = if (substantialHeights.isNotEmpty()) substantialHeights[substantialHeights.size / 2] else 36f
        val totalSpanY = boxes.maxOf { it.maxY } - boxes.minOf { it.minY }
        // Fast path: if entire block fits comfortably within ~1.8x typical character height, it's a single line
        if (totalSpanY < medianH * 1.85f) return listOf(strokes)

        val (smallMarks, mainStrokes) = boxes.partition { it.w < medianH * 0.55f && it.h < medianH * 0.48f }
        if (mainStrokes.isEmpty()) return listOf(strokes)

        val lines = ArrayList<MutableList<SBox>>()
        for (sb in mainStrokes.sortedBy { it.cy }) {
            var bestLine: MutableList<SBox>? = null
            var bestScore = Float.MAX_VALUE
            for (line in lines) {
                val lTop = line.minOf { it.minY }
                val lBot = line.maxOf { it.maxY }
                val lCy = (lTop + lBot) * 0.5f
                val lH = max(lBot - lTop, medianH)
                val vOverlap = min(lBot, sb.maxY) - max(lTop, sb.minY)
                val centerDist = abs(sb.cy - lCy)
                if (vOverlap > -lH * 0.12f || centerDist < lH * 0.62f) {
                    if (centerDist < bestScore) {
                        bestScore = centerDist
                        bestLine = line
                    }
                }
            }
            if (bestLine != null) bestLine.add(sb) else lines.add(mutableListOf(sb))
        }

        // Attach small marks (dots, accents, ÷ dots, punctuation, minus signs) to the closest line
        for (mark in smallMarks) {
            var bestLine = lines.first()
            var bestCost = Float.MAX_VALUE
            for (line in lines) {
                val lTop = line.minOf { it.minY }
                val lBot = line.maxOf { it.maxY }
                val lLeft = line.minOf { it.minX }
                val lRight = line.maxOf { it.maxX }
                val dy = when {
                    mark.cy < lTop -> lTop - mark.cy
                    mark.cy > lBot -> mark.cy - lBot
                    else -> 0f
                }
                val dx = when {
                    mark.cx < lLeft -> lLeft - mark.cx
                    mark.cx > lRight -> mark.cx - lRight
                    else -> 0f
                }
                val cost = dy * 2.2f + dx * 0.35f + abs(mark.cy - (lTop + lBot) * 0.5f) * 0.3f
                if (cost < bestCost) {
                    bestCost = cost
                    bestLine = line
                }
            }
            bestLine.add(mark)
        }

        if (lines.size <= 1) return listOf(strokes)

        lines.sortBy { l -> l.minOf { it.minY } }
        return lines.map { line ->
            // Preserve natural writing order (idx) inside each line so ML Kit's temporal stroke order stays intact
            line.sortedBy { it.idx }.map { it.pts }
        }
    }

    private suspend fun recognizeWithMlKit(
        strokes: List<List<TimedStrokePoint>>,
        writingWidth: Float,
        writingHeight: Float,
        preContext: String,
        languageTag: String,
    ): HandwritingRecognitionResult? {
        val resolvedTag = findLanguage(languageTag).mlKitTag
        if (!ensureModelReady(resolvedTag)) return null
        val model = buildModel(resolvedTag) ?: return null
        val recognizer = recognizers.getOrPut(resolvedTag) {
            DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
        }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        val strokeHeights = ArrayList<Float>(strokes.size)
        for (stroke in strokes) {
            var sLoY = Float.MAX_VALUE
            var sHiY = -Float.MAX_VALUE
            for (pt in stroke) {
                if (pt.x < minX) minX = pt.x
                if (pt.y < minY) minY = pt.y
                if (pt.x > maxX) maxX = pt.x
                if (pt.y > maxY) maxY = pt.y
                if (pt.y < sLoY) sLoY = pt.y
                if (pt.y > sHiY) sHiY = pt.y
            }
            val sh = sHiY - sLoY
            if (sh >= 4f) strokeHeights.add(sh)
        }
        if (!minX.isFinite() || !minY.isFinite()) return null

        val rawW = (maxX - minX).coerceAtLeast(1f)
        val rawH = (maxY - minY).coerceAtLeast(1f)
        strokeHeights.sort()
        val refHeight = if (strokeHeights.isNotEmpty()) {
            strokeHeights[strokeHeights.size / 2].coerceAtLeast(rawH * 0.45f)
        } else {
            max(rawH, rawW * 0.5f).coerceAtLeast(12f)
        }

        // Scale strokes uniformly into ML Kit's sweet-spot glyph height (~84px) so tiny or giant writing is crystal clear
        val scale = (84f / refHeight).coerceIn(0.25f, 6.0f)
        val scaledW = rawW * scale
        val scaledH = rawH * scale
        val padX = max(56f, scaledW * 0.20f)
        val padY = max(48f, scaledH * 0.35f)

        val inkBuilder = Ink.builder()
        var monotonicT = 0L
        for (stroke in strokes) {
            val sb = Ink.Stroke.builder()
            var prevRawT = stroke.first().tMs
            for (i in stroke.indices) {
                val pt = stroke[i]
                if (i > 0) {
                    val delta = (pt.tMs - prevRawT).coerceIn(4L, 65L)
                    monotonicT += delta
                    prevRawT = pt.tMs
                }
                val sx = (pt.x - minX) * scale + padX
                val sy = (pt.y - minY) * scale + padY
                sb.addPoint(Ink.Point.create(sx, sy, monotonicT))
            }
            monotonicT += 90L
            inkBuilder.addStroke(sb.build())
        }
        val ink = inkBuilder.build()

        val areaW = max(scaledW + padX * 2f, if (writingWidth > 1f) writingWidth * scale else 0f).coerceAtLeast(220f)
        val areaH = max(scaledH + padY * 2f, if (writingHeight > 1f) writingHeight * scale else 0f).coerceAtLeast(160f)
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
                .take(10)
            val best = ranked.firstOrNull().orEmpty()
            HandwritingRecognitionResult(text = best, candidates = ranked, usedMlKit = true)
        }.getOrNull()
    }

    /**
     * Reranks ML Kit candidates using geometric symbol evidence and contextual math/numeric disambiguation.
     */
    private fun rerankAndNormalizeCandidates(
        rawCandidates: List<String>,
        strokes: List<List<TimedStrokePoint>>,
        geoHit: GeometricSymbolClassifier.SymbolMatch?,
        preContext: String,
        isEnglishLike: Boolean,
    ): List<String> {
        val cleaned = rawCandidates.map { candidate ->
            if (isEnglishLike) normalizeMathAndNumbers(candidate, preContext) else candidate
        }.distinct()

        val out = LinkedHashSet<String>()

        // 1. If geometric classifier strongly recognizes a symbol (arrows, ÷, ±, ≠, ≤, ≥, √, ∞, π, Δ, Σ, ✓, %, #, =, +)
        if (geoHit != null) {
            val exactMlMatch = cleaned.firstOrNull { it == geoHit.symbol }
            if (exactMlMatch != null && geoHit.confidence >= 0.80f) {
                out.add(exactMlMatch)
            } else if (geoHit.confidence >= 0.90f) {
                // High-confidence geometric symbol beats garbled single-char ML Kit guesses (e.g. '÷' read as 'i' or '→' read as '7')
                val top = cleaned.firstOrNull().orEmpty()
                val looksLikeWord = top.length >= 3 && top.all { it.isLetter() }
                if (!looksLikeWord || strokes.size <= 3) {
                    out.add(geoHit.symbol)
                }
            }
        }

        // 2. If preceding context ends with a digit or math operator, prefer numeric/symbol candidates when ML Kit is torn
        val ctxTrim = preContext.trimEnd()
        val numericContext = ctxTrim.lastOrNull()?.let { it.isDigit() || it in setOf('+', '-', '=', '×', '÷', '<', '>', '(', '$', '₹', '€', '£', '.', ',') } == true
        if (numericContext && cleaned.size > 1) {
            val top = cleaned.first()
            val topIsAmbiguousSingle = top.length == 1 && top[0] in setOf('O', 'o', 'l', 'I', 'S', 's', 'Z', 'z', 'B', 'q', 'g', 'b')
            if (topIsAmbiguousSingle) {
                val digitCand = cleaned.firstOrNull { c -> c.all { it.isDigit() || it in setOf('.', ',', '%', '+', '-', '=', '×', '÷') } }
                if (digitCand != null) out.add(digitCand)
            }
        }

        cleaned.forEach { out.add(it) }
        if (geoHit != null && geoHit.confidence >= 0.76f) out.add(geoHit.symbol)
        return out.toList()
    }

    /**
     * Cleans unambiguous OCR digit/letter confusions inside numeric/math expressions
     * while leaving regular prose words untouched.
     */
    internal fun normalizeMathAndNumbers(input: String, preContext: String = ""): String {
        var s = input.trim()
        if (s.isEmpty()) return s

        // Normalize ASCII math digraphs cleanly when unambiguous
        s = s.replace("->", "→")
            .replace("<-", "←")
            .replace("!=", "≠")
            .replace("<=", "≤")
            .replace(">=", "≥")
            .replace("+-", "±")

        // Fix multiplication 'x'/'X' strictly between numbers (e.g. "5 x 4" -> "5 × 4", "12x3" -> "12 × 3")
        s = Regex("(\\d)\\s*[xX]\\s*(\\d)").replace(s) { m -> "${m.groupValues[1]} × ${m.groupValues[2]}" }

        // Fix O/o -> 0 and l/I -> 1 ONLY inside tokens that already contain digits, currency, or decimal points
        val tokens = s.split(Regex("(?<=\\s)|(?=\\s)"))
        val fixedTokens = tokens.mapIndexed { idx, tok ->
            if (tok.isBlank()) return@mapIndexed tok
            val hasDigit = tok.any { it.isDigit() }
            val hasCurrencyOrPct = tok.firstOrNull() in setOf('$', '₹', '€', '£', '¥') || tok.lastOrNull() in setOf('%', '°')
            val prevNonBlank = tokens.take(idx).lastOrNull { it.isNotBlank() }
            val nextNonBlank = tokens.drop(idx + 1).firstOrNull { it.isNotBlank() }
            val adjMathOp = prevNonBlank?.lastOrNull() in setOf('+', '-', '=', '×', '÷', '±', '<', '>', '≤', '≥', '≠', '^', '/') ||
                nextNonBlank?.firstOrNull() in setOf('+', '-', '=', '×', '÷', '±', '<', '>', '≤', '≥', '≠', '^', '/')

            if (hasDigit || hasCurrencyOrPct) {
                val sb = StringBuilder(tok.length)
                for (i in tok.indices) {
                    val ch = tok[i]
                    val prevCh = tok.getOrNull(i - 1)
                    val nextCh = tok.getOrNull(i + 1)
                    val nearNum = prevCh?.isDigit() == true || nextCh?.isDigit() == true ||
                        prevCh in setOf('.', ',', '$', '₹', '€', '£', '¥', '-', '+', '/') ||
                        nextCh in setOf('.', ',', '%', '°', '/')
                    val mapped = when {
                        (ch == 'O' || ch == 'o') && nearNum -> '0'
                        (ch == 'l' || ch == 'I' || ch == '|') && nearNum -> '1'
                        else -> ch
                    }
                    sb.append(mapped)
                }
                sb.toString()
            } else if (adjMathOp && tok.length == 1) {
                // Single ambiguous character directly next to a math operator + digit in the line
                val lineHasDigits = s.any { it.isDigit() } || preContext.trimEnd().lastOrNull()?.isDigit() == true
                if (lineHasDigits) {
                    when (tok[0]) {
                        'O', 'o' -> "0"
                        'l', 'I', '|' -> "1"
                        else -> tok
                    }
                } else tok
            } else {
                tok
            }
        }
        return fixedTokens.joinToString("")
    }

    private fun buildModel(languageTag: String): DigitalInkRecognitionModel? {
        val norm = normalizeTag(languageTag)
        val candidates = listOfNotNull(
            norm,
            languageTag,
            norm.substringBefore('-').takeIf { it != norm },
            if (norm == "he") "iw" else null,
            if (norm == "fil") "tl" else null,
        ).distinct()
        val identifier = candidates.firstNotNullOfOrNull { tag ->
            runCatching { DigitalInkRecognitionModelIdentifier.fromLanguageTag(tag) }.getOrNull()
        } ?: DigitalInkRecognitionModelIdentifier.EN_US
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
        return (median * 1.15f).coerceIn(22f, 68f)
    }
}

/**
 * High-precision geometric classifier for math operators, arrows, currency, and scientific symbols.
 * Runs in microseconds on 1..4 stroke clusters to boost ML Kit and power offline recognition.
 */
internal object GeometricSymbolClassifier {
    data class SymbolMatch(val symbol: String, val confidence: Float)

    data class StrokeGeom(
        val pts: List<Pair<Float, Float>>,
        val minX: Float,
        val maxX: Float,
        val minY: Float,
        val maxY: Float,
        val pathLen: Float,
    ) {
        val w: Float get() = (maxX - minX).coerceAtLeast(1f)
        val h: Float get() = (maxY - minY).coerceAtLeast(1f)
        val cx: Float get() = (minX + maxX) * 0.5f
        val cy: Float get() = (minY + maxY) * 0.5f
        val start: Pair<Float, Float> get() = pts.first()
        val end: Pair<Float, Float> get() = pts.last()
        val chord: Float get() = hypot(end.first - start.first, end.second - start.second)
        val straightness: Float get() = chord / pathLen.coerceAtLeast(1e-3f)
        val angleDeg: Float get() = Math.toDegrees(atan2((end.second - start.second).toDouble(), (end.first - start.first).toDouble())).toFloat()
        val isDot: Boolean get() = w < 14f && h < 14f && pathLen < 38f
        val isStraight: Boolean get() = straightness > 0.86f
        val isHorizontal: Boolean get() = isStraight && w > h * 2.1f
        val isVertical: Boolean get() = isStraight && h > w * 2.1f
        val isSlashUpRight: Boolean get() = isStraight && w > 6f && h > 6f && ((end.first - start.first) * (end.second - start.second) < 0f)
        val isSlashDownRight: Boolean get() = isStraight && w > 6f && h > 6f && ((end.first - start.first) * (end.second - start.second) > 0f)
        val isClosedLoop: Boolean get() = pathLen > 24f && chord < max(w, h) * 0.42f && straightness < 0.35f
    }

    fun buildGeom(strokes: List<List<TimedStrokePoint>>): List<StrokeGeom> = strokes.mapNotNull { s ->
        if (s.isEmpty()) null
        else {
            var loX = Float.MAX_VALUE; var hiX = -Float.MAX_VALUE
            var loY = Float.MAX_VALUE; var hiY = -Float.MAX_VALUE
            var len = 0f
            val pts = ArrayList<Pair<Float, Float>>(s.size)
            for (i in s.indices) {
                val p = s[i]
                pts.add(p.x to p.y)
                if (p.x < loX) loX = p.x; if (p.x > hiX) hiX = p.x
                if (p.y < loY) loY = p.y; if (p.y > hiY) hiY = p.y
                if (i > 0) len += hypot(p.x - s[i - 1].x, p.y - s[i - 1].y)
            }
            StrokeGeom(pts, loX, hiX, loY, hiY, len)
        }
    }

    fun classifyIsolated(strokes: List<List<TimedStrokePoint>>): SymbolMatch? {
        val gs = buildGeom(strokes)
        if (gs.isEmpty()) return null
        val minX = gs.minOf { it.minX }; val maxX = gs.maxOf { it.maxX }
        val minY = gs.minOf { it.minY }; val maxY = gs.maxOf { it.maxY }
        val totalW = (maxX - minX).coerceAtLeast(1f)
        val totalH = (maxY - minY).coerceAtLeast(1f)
        val maxDim = max(totalW, totalH)

        fun isSmallMark(g: StrokeGeom): Boolean =
            g.w < maxDim * 0.34f && g.h < maxDim * 0.34f && g.pathLen < maxDim * 1.15f

        when (gs.size) {
            1 -> {
                val g = gs[0]
                // Single straight horizontal bar -> '-'
                if (g.isHorizontal && totalW > totalH * 2.8f) return SymbolMatch("-", 0.84f)
                // Checkmark '✓' (down-right short leg then sharp turn up-right long leg)
                if (detectCheckmark(g)) return SymbolMatch("✓", 0.91f)
                // Square root '√' (small lip/down-right, sharp up-right, then horizontal roof right)
                if (detectSqrt(g)) return SymbolMatch("√", 0.93f)
                // Angle brackets '<' and '>'
                detectAngleBracket(g)?.let { return it }
                // Infinity '∞' (wide horizontal figure-8)
                if (detectInfinity(g)) return SymbolMatch("∞", 0.92f)
                // Approximate/tilde '~' (horizontal wave)
                if (detectTilde(g)) return SymbolMatch("~", 0.84f)
                return null
            }

            2 -> {
                val a = gs[0]; val b = gs[1]
                // Two parallel horizontal strokes -> '='
                if (a.isHorizontal && b.isHorizontal) {
                    val vSep = abs(a.cy - b.cy)
                    val xOverlap = min(a.maxX, b.maxX) - max(a.minX, b.minX)
                    if (xOverlap > min(a.w, b.w) * 0.45f && vSep > max(a.h, b.h) * 0.4f && vSep < totalW * 0.9f) {
                        return SymbolMatch("=", 0.93f)
                    }
                }
                // Two stacked horizontal waves -> '≈'
                if (detectTilde(a) && detectTilde(b) && abs(a.cy - b.cy) > totalH * 0.25f) {
                    return SymbolMatch("≈", 0.93f)
                }
                // Arrow (1 straight shaft + 1 V-shaped head at one end) -> →, ←, ↑, ↓
                detectArrow(a, b)?.let { return it }
                detectArrow(b, a)?.let { return it }

                // '≤' and '≥' (one '<' or '>' above a horizontal bar)
                if (a.isHorizontal || b.isHorizontal) {
                    val bar = if (a.isHorizontal) a else b
                    val other = if (bar === a) b else a
                    if (bar.cy > other.cy + totalH * 0.18f && bar.minX < other.maxX && bar.maxX > other.minX) {
                        val angle = detectAngleBracket(other)
                        if (angle?.symbol == "<") return SymbolMatch("≤", 0.94f)
                        if (angle?.symbol == ">") return SymbolMatch("≥", 0.94f)
                    }
                }

                // '+' (vertical stem + horizontal bar intersecting near center)
                if ((a.isHorizontal && b.isVertical) || (b.isHorizontal && a.isVertical)) {
                    val hBar = if (a.isHorizontal) a else b
                    val vBar = if (hBar === a) b else a
                    val aspect = hBar.w / vBar.h.coerceAtLeast(1f)
                    if (segmentsIntersect(hBar.start, hBar.end, vBar.start, vBar.end) &&
                        abs(hBar.cy - vBar.cy) < vBar.h * 0.24f &&
                        abs(hBar.cx - vBar.cx) < hBar.w * 0.24f &&
                        aspect in 0.62f..1.55f
                    ) {
                        return SymbolMatch("+", 0.92f)
                    }
                }

                // Two small vertical dots -> ':'
                if (isSmallMark(a) && isSmallMark(b) && abs(a.cx - b.cx) < totalH * 0.35f && abs(a.cy - b.cy) > max(a.h, b.h) * 1.1f) {
                    return SymbolMatch(":", 0.89f)
                }

                // '!' (tall vertical stem above a small dot)
                val topS = if (a.cy < b.cy) a else b
                val botS = if (topS === a) b else a
                if (topS.isVertical && isSmallMark(botS) && abs(topS.cx - botS.cx) < totalH * 0.32f && botS.minY > topS.maxY - topS.h * 0.1f) {
                    return SymbolMatch("!", 0.88f)
                }

                // '?' (curved top hook above a small dot)
                if (!topS.isStraight && topS.h > totalH * 0.45f && isSmallMark(botS) &&
                    abs(topS.cx - botS.cx) < totalW * 0.55f && botS.minY > topS.maxY - totalH * 0.08f
                ) {
                    // Verify top half arches right then down
                    val topPts = topS.pts
                    if (topPts.size >= 4 && topPts.first().second < topS.cy) {
                        return SymbolMatch("?", 0.86f)
                    }
                }

                // '×' (two intersecting diagonals roughly square)
                if ((a.isSlashUpRight && b.isSlashDownRight) || (a.isSlashDownRight && b.isSlashUpRight)) {
                    if (segmentsIntersect(a.start, a.end, b.start, b.end) && (totalW / totalH) in 0.6f..1.5f) {
                        return SymbolMatch("×", 0.82f)
                    }
                }

                // '$' (S-curve + vertical bar through it)
                val vCandidate = if (a.isVertical) a else if (b.isVertical) b else null
                if (vCandidate != null) {
                    val sCandidate = if (vCandidate === a) b else a
                    if (!sCandidate.isStraight && sCandidate.pathLen > sCandidate.h * 1.6f &&
                        abs(vCandidate.cx - sCandidate.cx) < sCandidate.w * 0.35f &&
                        vCandidate.h >= sCandidate.h * 0.85f
                    ) {
                        return SymbolMatch("$", 0.86f)
                    }
                }
                return null
            }

            3 -> {
                val horiz = gs.filter { it.isHorizontal }
                val verts = gs.filter { it.isVertical }
                val dots = gs.filter { isSmallMark(it) }

                // '÷' (1 horizontal bar + 1 dot above + 1 dot below)
                if (horiz.size == 1 && dots.size == 2) {
                    val bar = horiz[0]
                    val above = dots.count { it.cy < bar.cy - bar.h * 0.3f && it.cx in (bar.minX - totalW * 0.15f)..(bar.maxX + totalW * 0.15f) }
                    val below = dots.count { it.cy > bar.cy + bar.h * 0.3f && it.cx in (bar.minX - totalW * 0.15f)..(bar.maxX + totalW * 0.15f) }
                    if (above == 1 && below == 1) return SymbolMatch("÷", 0.96f)
                }

                // '%' (1 diagonal '/' + 1 small circle/dot top-left + 1 small circle/dot bottom-right)
                val slash = gs.firstOrNull { it.isSlashUpRight && it.h > totalH * 0.55f }
                if (slash != null) {
                    val others = gs.filter { it !== slash }
                    if (others.size == 2 && others.all { it.w < totalW * 0.52f && it.h < totalH * 0.52f }) {
                        val tl = others.any { it.cx < slash.cx && it.cy < slash.cy }
                        val br = others.any { it.cx > slash.cx && it.cy > slash.cy }
                        if (tl && br) return SymbolMatch("%", 0.95f)
                    }
                }

                // '≠' (2 parallel horizontal bars + 1 crossing slash)
                if (horiz.size == 2) {
                    val rest = gs.first { it !in horiz }
                    val crossesBoth = segmentsIntersect(rest.start, rest.end, horiz[0].start, horiz[0].end) &&
                        segmentsIntersect(rest.start, rest.end, horiz[1].start, horiz[1].end)
                    if (crossesBoth && !rest.isHorizontal) {
                        return SymbolMatch("≠", 0.96f)
                    }
                }

                // '±' (1 vertical + 2 horizontals: top horizontal crosses vertical, bottom horizontal sits below)
                if (horiz.size == 2 && verts.size == 1) {
                    val v = verts[0]
                    val topH = if (horiz[0].cy < horiz[1].cy) horiz[0] else horiz[1]
                    val botH = if (topH === horiz[0]) horiz[1] else horiz[0]
                    if (segmentsIntersect(v.start, v.end, topH.start, topH.end) &&
                        botH.cy > v.maxY - v.h * 0.15f
                    ) {
                        return SymbolMatch("±", 0.96f)
                    }
                }

                // '≡' (3 parallel horizontal bars)
                if (horiz.size == 3) {
                    val sortedY = horiz.sortedBy { it.cy }
                    if (sortedY[1].cy - sortedY[0].cy > totalH * 0.15f && sortedY[2].cy - sortedY[1].cy > totalH * 0.15f) {
                        return SymbolMatch("≡", 0.93f)
                    }
                }

                // 'π' (1 top horizontal bar + 2 vertical legs below it)
                if (horiz.size == 1 && verts.size == 2) {
                    val topBar = horiz[0]
                    if (topBar.cy < min(verts[0].cy, verts[1].cy) &&
                        abs(verts[0].cx - verts[1].cx) > topBar.w * 0.25f
                    ) {
                        return SymbolMatch("π", 0.92f)
                    }
                }
                return null
            }

            4 -> {
                // '#' (2 horizontal bars + 2 vertical/slanted bars intersecting)
                val horiz = gs.filter { it.isHorizontal }
                val nonHoriz = gs.filter { !it.isHorizontal && it.isStraight && it.h > it.w * 1.1f }
                if (horiz.size == 2 && nonHoriz.size == 2) {
                    var crosses = 0
                    for (hBar in horiz) for (vBar in nonHoriz) {
                        if (segmentsIntersect(hBar.start, hBar.end, vBar.start, vBar.end)) crosses++
                    }
                    if (crosses >= 3) return SymbolMatch("#", 0.95f)
                }
                return null
            }

            else -> return null
        }
    }

    private fun detectArrow(shaft: StrokeGeom, head: StrokeGeom): SymbolMatch? {
        if (!shaft.isStraight || shaft.chord < 18f) return null
        if (head.isStraight || head.pts.size < 3) return null
        val headSpan = max(head.w, head.h)
        if (headSpan > shaft.chord * 0.95f || headSpan < shaft.chord * 0.12f) return null

        // Check if the head sits near one endpoint of the shaft (its tip)
        val dStart = minDistToStroke(shaft.start, head.pts)
        val dEnd = minDistToStroke(shaft.end, head.pts)
        val tipAtEnd = dEnd < dStart
        val tipDist = if (tipAtEnd) dEnd else dStart
        if (tipDist > max(headSpan * 0.55f, shaft.chord * 0.25f)) return null

        val tail = if (tipAtEnd) shaft.start else shaft.end
        val tip = if (tipAtEnd) shaft.end else shaft.start
        val dx = tip.first - tail.first
        val dy = tip.second - tail.second

        // Ensure head's two arms flare backward from tip toward tail
        val headStartDot = (head.start.first - tip.first) * (-dx) + (head.start.second - tip.second) * (-dy)
        val headEndDot = (head.end.first - tip.first) * (-dx) + (head.end.second - tip.second) * (-dy)
        if (headStartDot <= 0f || headEndDot <= 0f) return null

        return if (abs(dx) >= abs(dy) * 1.6f) {
            SymbolMatch(if (dx > 0f) "→" else "←", 0.95f)
        } else if (abs(dy) >= abs(dx) * 1.6f) {
            SymbolMatch(if (dy > 0f) "↓" else "↑", 0.95f)
        } else null
    }

    private fun detectAngleBracket(g: StrokeGeom): SymbolMatch? {
        if (g.pts.size < 3 || g.isStraight) return null
        // Find extreme X vertex
        val minIdx = g.pts.indices.minByOrNull { g.pts[it].first } ?: return null
        val maxIdx = g.pts.indices.maxByOrNull { g.pts[it].first } ?: return null

        // '<' has apex at leftmost point near middle of stroke
        if (minIdx in 1 until g.pts.lastIndex) {
            val apex = g.pts[minIdx]
            val leg1 = subStraightness(g.pts, 0, minIdx)
            val leg2 = subStraightness(g.pts, minIdx, g.pts.lastIndex)
            val endSepY = abs(g.start.second - g.end.second)
            if (leg1 > 0.88f && leg2 > 0.88f &&
                abs(apex.second - g.cy) < g.h * 0.25f &&
                endSepY > g.h * 0.65f &&
                abs(g.start.first - g.end.first) < g.w * 0.45f &&
                (g.w / g.h) in 0.35f..1.6f
            ) {
                return SymbolMatch("<", 0.90f)
            }
        }
        // '>' has apex at rightmost point near middle of stroke
        if (maxIdx in 1 until g.pts.lastIndex) {
            val apex = g.pts[maxIdx]
            val leg1 = subStraightness(g.pts, 0, maxIdx)
            val leg2 = subStraightness(g.pts, maxIdx, g.pts.lastIndex)
            val endSepY = abs(g.start.second - g.end.second)
            if (leg1 > 0.88f && leg2 > 0.88f &&
                abs(apex.second - g.cy) < g.h * 0.25f &&
                endSepY > g.h * 0.65f &&
                abs(g.start.first - g.end.first) < g.w * 0.45f &&
                (g.w / g.h) in 0.35f..1.6f
            ) {
                return SymbolMatch(">", 0.90f)
            }
        }
        return null
    }

    private fun detectCheckmark(g: StrokeGeom): Boolean {
        if (g.pts.size < 4 || g.isStraight) return false
        val botIdx = g.pts.indices.maxByOrNull { g.pts[it].second } ?: return false
        if (botIdx <= 0 || botIdx >= g.pts.lastIndex) return false
        val bot = g.pts[botIdx]
        val leg1Len = hypot(bot.first - g.start.first, bot.second - g.start.second)
        val leg2Len = hypot(g.end.first - bot.first, g.end.second - bot.second)
        if (leg1Len < 6f || leg2Len < leg1Len * 1.45f) return false
        if (bot.first <= g.start.first || g.end.first <= bot.first) return false
        if (g.end.second >= g.start.second - g.h * 0.1f) return false
        return subStraightness(g.pts, 0, botIdx) > 0.86f && subStraightness(g.pts, botIdx, g.pts.lastIndex) > 0.86f
    }

    private fun detectSqrt(g: StrokeGeom): Boolean {
        if (g.pts.size < 6 || g.isStraight) return false
        // Must have a bottom valley early-mid, then rise steeply to top-mid, then extend horizontally right at the top
        val botIdx = g.pts.indices.maxByOrNull { g.pts[it].second } ?: return false
        if (botIdx < 1 || botIdx > (g.pts.size * 0.65f).toInt()) return false
        val afterBot = (botIdx + 1)..g.pts.lastIndex
        val peakIdx = afterBot.minByOrNull { g.pts[it].second } ?: return false
        if (peakIdx >= g.pts.lastIndex - 1) return false
        val bot = g.pts[botIdx]
        val peak = g.pts[peakIdx]
        val roofW = g.end.first - peak.first
        val roofDy = abs(g.end.second - peak.second)
        return roofW > g.w * 0.35f &&
            roofDy < g.h * 0.25f &&
            (bot.second - peak.second) > g.h * 0.65f &&
            peak.first > bot.first &&
            subStraightness(g.pts, peakIdx, g.pts.lastIndex) > 0.86f
    }

    private fun detectInfinity(g: StrokeGeom): Boolean {
        if (g.w < g.h * 1.45f || g.pts.size < 12) return false
        if (!g.isClosedLoop) return false
        // Check for self-intersection near the center of the bounding box
        val step = max(1, g.pts.size / 24)
        val sampled = (g.pts.indices step step).map { g.pts[it] }
        for (i in 0 until sampled.size - 3) {
            for (j in i + 2 until sampled.size - 1) {
                if (i == 0 && j == sampled.size - 2) continue
                if (segmentsIntersect(sampled[i], sampled[i + 1], sampled[j], sampled[j + 1])) {
                    val mx = (sampled[i].first + sampled[j].first) * 0.5f
                    val my = (sampled[i].second + sampled[j].second) * 0.5f
                    if (abs(mx - g.cx) < g.w * 0.25f && abs(my - g.cy) < g.h * 0.3f) return true
                }
            }
        }
        return false
    }

    private fun detectTilde(g: StrokeGeom): Boolean {
        if (g.w < g.h * 1.9f || g.isStraight || g.pts.size < 6) return false
        if (g.end.first <= g.start.first + g.w * 0.5f) return false
        var rises = 0; var falls = 0; var lastDir = 0
        for (i in 1 until g.pts.size) {
            val dy = g.pts[i].second - g.pts[i - 1].second
            if (abs(dy) < g.h * 0.08f) continue
            val dir = if (dy < 0f) -1 else 1
            if (dir != lastDir) {
                if (dir < 0) rises++ else falls++
                lastDir = dir
            }
        }
        return rises >= 1 && falls >= 1 && (rises + falls) in 2..4
    }

    private fun subStraightness(pts: List<Pair<Float, Float>>, from: Int, to: Int): Float {
        if (to <= from) return 1f
        var len = 0f
        for (i in from until to) {
            len += hypot(pts[i + 1].first - pts[i].first, pts[i + 1].second - pts[i].second)
        }
        val chord = hypot(pts[to].first - pts[from].first, pts[to].second - pts[from].second)
        return chord / len.coerceAtLeast(1e-3f)
    }

    private fun minDistToStroke(pt: Pair<Float, Float>, stroke: List<Pair<Float, Float>>): Float {
        var best = Float.MAX_VALUE
        for (p in stroke) {
            val d = hypot(p.first - pt.first, p.second - pt.second)
            if (d < best) best = d
        }
        return best
    }

    internal fun segmentsIntersect(
        a: Pair<Float, Float>,
        b: Pair<Float, Float>,
        c: Pair<Float, Float>,
        d: Pair<Float, Float>,
    ): Boolean {
        fun cross(p: Pair<Float, Float>, q: Pair<Float, Float>, r: Pair<Float, Float>): Float =
            (q.first - p.first) * (r.second - p.second) - (q.second - p.second) * (r.first - p.first)
        val d1 = cross(a, b, c)
        val d2 = cross(a, b, d)
        val d3 = cross(c, d, a)
        val d4 = cross(c, d, b)
        return ((d1 > 0f && d2 < 0f) || (d1 < 0f && d2 > 0f)) &&
            ((d3 > 0f && d4 < 0f) || (d3 < 0f && d4 > 0f))
    }
}

/**
 * Upgraded offline recognizer supporting full A-Z, a-z, 0-9, math operators, arrows,
 * currency, and punctuation via geometric structural classification + 90+ directional
 * & spatial-zone vector templates ($1/$P-inspired) with ranked top-5 candidates.
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
        val glyph: String,
        val vec: FloatArray,
        val revVec: FloatArray,
        val aspectMin: Float = 0.05f,
        val aspectMax: Float = 6f,
        val allowReversed: Boolean = false,
        val isDigit: Boolean = false,
        val isSymbol: Boolean = false,
    )

    private val templates: List<GlyphTemplate> by lazy { buildTemplates() }

    fun recognize(strokes: List<List<TimedStrokePoint>>): String =
        recognizeWithCandidates(strokes).firstOrNull().orEmpty()

    fun recognizeWithCandidates(strokes: List<List<TimedStrokePoint>>): List<String> {
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
        if (boxed.isEmpty()) return emptyList()

        // 1. Group strokes into natural writing lines
        val sortedByY = boxed.sortedBy { it.cy }
        val lines = mutableListOf<MutableList<BoxedStroke>>()
        for (sb in sortedByY) {
            val match = lines.firstOrNull { line ->
                val lTop = line.minOf { it.minY }
                val lBot = line.maxOf { it.maxY }
                val lHeight = (lBot - lTop).coerceAtLeast(24f)
                val vOverlap = min(lBot, sb.maxY) - max(lTop, sb.minY)
                vOverlap > -lHeight * 0.18f && abs(sb.cy - (lTop + lBot) * 0.5f) < lHeight * 0.92f
            }
            if (match != null) match.add(sb) else lines.add(mutableListOf(sb))
        }
        lines.sortBy { line -> line.minOf { it.minY } }

        val bestLines = mutableListOf<String>()
        val secondLines = mutableListOf<String>()

        // 2. Segment characters & words on each line
        for (line in lines) {
            val ordered = line.sortedBy { it.minX }
            val lineTop = ordered.minOf { it.minY }
            val lineBot = ordered.maxOf { it.maxY }
            val lineHeight = (lineBot - lineTop).coerceAtLeast(20f)

            val clusters = mutableListOf<MutableList<BoxedStroke>>()
            for (sb in ordered) {
                val prev = clusters.lastOrNull()
                if (prev == null) {
                    clusters.add(mutableListOf(sb))
                } else {
                    val pMinX = prev.minOf { it.minX }
                    val pMaxX = prev.maxOf { it.maxX }
                    val pCx = (pMinX + pMaxX) * 0.5f
                    val overlap = min(pMaxX, sb.maxX) - max(pMinX, sb.minX)
                    val minW = min(pMaxX - pMinX, sb.w).coerceAtLeast(1f)
                    val isDotOrCross = (sb.w < lineHeight * 0.42f && sb.h < lineHeight * 0.42f && abs(sb.cx - pCx) < lineHeight * 0.55f)
                    val isStackedBar = (sb.w > sb.h * 1.8f && overlap > minW * 0.35f)
                    val isCrossing = (overlap > -2f && overlap / minW > 0.22f)
                    if (isCrossing || isDotOrCross || isStackedBar) {
                        prev.add(sb)
                    } else {
                        clusters.add(mutableListOf(sb))
                    }
                }
            }

            val widths = clusters.map { c -> (c.maxOf { it.maxX } - c.minOf { it.minX }).coerceAtLeast(8f) }.sorted()
            val medianW = widths[widths.size / 2].coerceIn(14f, 64f)
            val wordGap = max(medianW * 0.85f, lineHeight * 0.46f)

            val primarySb = StringBuilder()
            val altSb = StringBuilder()
            var prevRight = Float.NaN
            var wordStart = true

            for (cluster in clusters) {
                val cLeft = cluster.minOf { it.minX }
                val cRight = cluster.maxOf { it.maxX }
                val cTop = cluster.minOf { it.minY }
                val cBot = cluster.maxOf { it.maxY }
                if (!prevRight.isNaN() && (cLeft - prevRight) > wordGap) {
                    primarySb.append(' ')
                    altSb.append(' ')
                    wordStart = true
                }
                val ranked = classifyClusterRanked(
                    cluster = cluster,
                    w = cRight - cLeft,
                    h = cBot - cTop,
                    relTop = (cTop - lineTop) / lineHeight,
                    relBot = (cBot - lineTop) / lineHeight,
                    lineHeight = lineHeight,
                    wordStart = wordStart,
                )
                primarySb.append(ranked.firstOrNull() ?: "")
                altSb.append(ranked.getOrNull(1) ?: ranked.firstOrNull() ?: "")
                prevRight = if (prevRight.isNaN()) cRight else max(prevRight, cRight)
                wordStart = false
            }
            val t1 = HandwritingRecognizer.normalizeMathAndNumbers(primarySb.toString().trim())
            val t2 = HandwritingRecognizer.normalizeMathAndNumbers(altSb.toString().trim())
            if (t1.isNotEmpty()) bestLines.add(t1)
            if (t2.isNotEmpty()) secondLines.add(t2)
        }

        val out = LinkedHashSet<String>()
        val main = bestLines.joinToString("\n").trim()
        val alt = secondLines.joinToString("\n").trim()
        if (main.isNotEmpty()) out.add(main)
        if (alt.isNotEmpty()) out.add(alt)
        return out.toList()
    }

    private fun classifyClusterRanked(
        cluster: List<BoxedStroke>,
        w: Float,
        h: Float,
        relTop: Float,
        relBot: Float,
        lineHeight: Float,
        wordStart: Boolean,
    ): List<String> {
        val aspect = w / h.coerceAtLeast(1f)

        // Small marks positioned by vertical zone (period, apostrophe, degree, comma, minus, underscore)
        if (w < lineHeight * 0.24f && h < lineHeight * 0.24f) {
            return when {
                relBot < 0.45f -> listOf("'", "°", ".")
                relTop > 0.65f -> listOf(".", ",")
                else -> listOf(".", "-")
            }
        }
        if (aspect > 2.4f && h < lineHeight * 0.28f) {
            return if (relTop > 0.78f) listOf("_", "-") else listOf("-", "—")
        }

        // Direct geometric symbol & math classifier first
        val timedProxy = cluster.map { bs ->
            bs.pts.mapIndexed { i, (x, y) -> TimedStrokePoint(x, y, i * 10L) }
        }
        val geo = GeometricSymbolClassifier.classifyIsolated(timedProxy)
        if (geo != null && geo.confidence >= 0.88f) {
            return listOf(geo.symbol)
        }

        // Structural multi-stroke shortcuts (2..4 strokes)
        if (cluster.size == 2) {
            val a = cluster[0]; val b = cluster[1]
            val smallH = min(a.h, b.h); val tallH = max(a.h, b.h)
            val smallW = min(a.w, b.w); val wideW = max(a.w, b.w)
            // Dotted 'i' or 'j'
            if (smallH < tallH * 0.34f && smallW < tallH * 0.34f && tallH > lineHeight * 0.32f) {
                val dot = if (a.h < b.h) a else b
                val stem = if (dot === a) b else a
                if (dot.cy < stem.cy) {
                    return if (stem.w > stem.h * 0.35f && relBot > 0.82f) listOf("j", "i") else listOf("i", "!")
                }
            }
            // '='
            if (a.w > a.h * 1.8f && b.w > b.h * 1.8f) return listOf("=", "≈")
            // '+' or 't' or 'T' or '7' (crossed)
            val hasHoriz = (a.w > a.h * 1.6f) || (b.w > b.h * 1.6f)
            val hasVert = (a.h > a.w * 1.6f) || (b.h > b.w * 1.6f)
            if (hasHoriz && hasVert) {
                val stem = if (a.h >= b.h) a else b
                val bar = if (stem === a) b else a
                return when {
                    bar.cy < stem.minY + stem.h * 0.22f -> listOf("T", "t", "+")
                    abs(bar.cy - stem.cy) < stem.h * 0.22f && wideW > tallH * 0.65f -> listOf("+", "t")
                    else -> listOf("t", "+", "f")
                }
            }
            // 'x' / '×'
            if (a.h > a.w * 0.5f && b.h > b.w * 0.5f && aspect in 0.55f..1.45f) {
                val aSlash = (a.pts.last().first - a.pts.first().first) * (a.pts.last().second - a.pts.first().second)
                val bSlash = (b.pts.last().first - b.pts.first().first) * (b.pts.last().second - b.pts.first().second)
                if (aSlash * bSlash < 0f) return listOf("x", "×")
            }
        }

        // Vertical straight stroke: '1', 'l', 'I', '|'
        if (aspect < 0.26f && h > lineHeight * 0.42f && cluster.size == 1) {
            return if (wordStart) listOf("I", "1", "l") else listOf("l", "1", "I")
        }

        // Single-stroke or concatenated polyline matching against 90+ templates
        val merged = cluster.flatMap { it.pts }
        if (merged.size < 2) return listOf(".")

        val candidateVec = toFeatureVector(merged) ?: return listOf(if (wordStart) "I" else "l")
        val scored = ArrayList<Pair<String, Float>>(templates.size)
        for (tpl in templates) {
            if (aspect < tpl.aspectMin || aspect > tpl.aspectMax) continue
            var score = cosineSim(candidateVec, tpl.vec)
            if (tpl.allowReversed) {
                score = max(score, cosineSim(candidateVec, tpl.revVec))
            }
            // Small boost if geometric classifier also hinted at this symbol
            if (geo != null && geo.symbol == tpl.glyph) score += 0.14f
            scored.add(tpl.glyph to score)
        }
        if (scored.isEmpty()) return listOf(geo?.symbol ?: "e")
        scored.sortByDescending { it.second }

        val ranked = LinkedHashSet<String>()
        if (geo != null && geo.confidence >= 0.78f) ranked.add(geo.symbol)
        for ((glyph, _) in scored) {
            val adjusted = if (wordStart && h > lineHeight * 0.74f && glyph.length == 1 && glyph[0].isLowerCase()) {
                glyph.uppercase()
            } else glyph
            ranked.add(adjusted)
            if (ranked.size >= 4) break
        }
        return ranked.toList()
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
            vec[i * 4 + 2] = (dx / d) * 0.78f
            vec[i * 4 + 3] = (dy / d) * 0.78f
        }
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
        val list = ArrayList<GlyphTemplate>(96)
        fun add(
            glyph: String,
            pts: List<Pair<Float, Float>>,
            minA: Float,
            maxA: Float,
            rev: Boolean = false,
            digit: Boolean = false,
            sym: Boolean = false,
        ) {
            val v = toFeatureVector(pts) ?: return
            val rv = toFeatureVector(pts.reversed()) ?: v
            list.add(GlyphTemplate(glyph, v, rv, minA, maxA, rev, digit, sym))
        }

        // ---- DIGITS 0-9 (multi-variant for high numeric accuracy) ----
        add("0", arcPts(0.5f, 0.5f, 0.34f, 0.46f, -90f, -450f), 0.35f, 1.15f, rev = true, digit = true)
        add("1", listOf(0.28f to 0.25f, 0.52f to 0.08f, 0.52f to 0.92f), 0.15f, 0.9f, digit = true)
        add("1", listOf(0.30f to 0.24f, 0.50f to 0.08f, 0.50f to 0.90f, 0.22f to 0.90f, 0.80f to 0.90f), 0.2f, 1.1f, digit = true)
        add("2", arcPts(0.50f, 0.30f, 0.32f, 0.22f, -160f, 35f) + listOf(0.16f to 0.88f, 0.88f to 0.88f), 0.4f, 1.45f, digit = true)
        add("3", arcPts(0.46f, 0.28f, 0.30f, 0.20f, -150f, 90f) + arcPts(0.46f, 0.72f, 0.32f, 0.22f, -90f, 160f), 0.35f, 1.35f, digit = true)
        add("4", listOf(0.62f to 0.08f, 0.14f to 0.64f, 0.88f to 0.64f, 0.62f to 0.10f, 0.62f to 0.92f), 0.4f, 1.4f, digit = true)
        add("4", listOf(0.22f to 0.10f, 0.22f to 0.58f, 0.84f to 0.58f, 0.70f to 0.12f, 0.70f to 0.92f), 0.4f, 1.4f, digit = true)
        add("5", listOf(0.80f to 0.12f, 0.24f to 0.12f, 0.22f to 0.48f) + arcPts(0.48f, 0.68f, 0.32f, 0.24f, -125f, 155f), 0.35f, 1.4f, digit = true)
        add("6", arcPts(0.56f, 0.30f, 0.32f, 0.24f, -45f, -180f) + arcPts(0.50f, 0.68f, 0.30f, 0.24f, 180f, -180f), 0.35f, 1.25f, digit = true)
        add("7", listOf(0.14f to 0.12f, 0.86f to 0.12f, 0.38f to 0.92f), 0.35f, 1.4f, digit = true)
        add("8", arcPts(0.50f, 0.28f, 0.26f, 0.20f, -90f, -450f) + arcPts(0.50f, 0.72f, 0.30f, 0.22f, -90f, 270f), 0.35f, 1.2f, rev = true, digit = true)
        add("9", arcPts(0.48f, 0.32f, 0.28f, 0.22f, -10f, -370f) + listOf(0.76f to 0.30f, 0.68f to 0.92f), 0.35f, 1.25f, digit = true)

        // ---- LOWERCASE a-z ----
        add("a", arcPts(0.45f, 0.55f, 0.36f, 0.36f, -35f, -380f) + listOf(0.82f to 0.25f, 0.85f to 0.92f), 0.5f, 1.5f)
        add("b", listOf(0.20f to 0.06f, 0.20f to 0.92f) + arcPts(0.48f, 0.68f, 0.30f, 0.24f, 180f, 520f), 0.3f, 1.15f)
        add("c", arcPts(0.55f, 0.5f, 0.44f, 0.44f, -40f, -320f), 0.45f, 1.6f)
        add("d", arcPts(0.46f, 0.66f, 0.30f, 0.24f, -30f, -360f) + listOf(0.78f to 0.06f, 0.78f to 0.92f), 0.3f, 1.15f)
        add("e", listOf(0.15f to 0.5f, 0.85f to 0.48f) + arcPts(0.5f, 0.5f, 0.42f, 0.44f, -10f, -315f), 0.5f, 1.6f)
        add("f", arcPts(0.62f, 0.22f, 0.22f, 0.16f, -20f, -180f) + listOf(0.40f to 0.22f, 0.40f to 0.94f, 0.18f to 0.46f, 0.76f to 0.46f), 0.25f, 1.1f)
        add("g", arcPts(0.50f, 0.34f, 0.28f, 0.22f, -30f, -360f) + listOf(0.78f to 0.24f, 0.76f to 0.86f, 0.52f to 0.96f, 0.26f to 0.88f), 0.3f, 1.15f)
        add("h", listOf(0.18f to 0.06f, 0.18f to 0.92f, 0.22f to 0.54f, 0.56f to 0.42f, 0.82f to 0.58f, 0.84f to 0.92f), 0.3f, 1.15f)
        add("j", listOf(0.62f to 0.22f, 0.60f to 0.84f, 0.42f to 0.96f, 0.22f to 0.88f), 0.2f, 0.9f)
        add("k", listOf(0.20f to 0.06f, 0.20f to 0.92f, 0.78f to 0.34f, 0.24f to 0.62f, 0.82f to 0.92f), 0.3f, 1.2f)
        add("m", listOf(0.08f to 0.2f, 0.08f to 0.9f, 0.24f to 0.22f, 0.48f to 0.22f, 0.50f to 0.9f, 0.66f to 0.22f, 0.90f to 0.25f, 0.92f to 0.9f), 0.8f, 2.8f)
        add("n", listOf(0.15f to 0.18f, 0.15f to 0.9f, 0.20f to 0.35f, 0.52f to 0.15f, 0.82f to 0.35f, 0.85f to 0.9f), 0.5f, 1.8f)
        add("o", arcPts(0.5f, 0.5f, 0.45f, 0.45f, -90f, -450f), 0.55f, 1.6f, rev = true)
        add("p", listOf(0.22f to 0.10f, 0.22f to 0.96f, 0.24f to 0.16f) + arcPts(0.50f, 0.34f, 0.30f, 0.22f, -150f, 150f), 0.3f, 1.15f)
        add("q", arcPts(0.48f, 0.34f, 0.28f, 0.22f, -20f, -360f) + listOf(0.76f to 0.22f, 0.76f to 0.94f, 0.90f to 0.80f), 0.3f, 1.2f)
        add("r", listOf(0.22f to 0.20f, 0.22f to 0.90f, 0.26f to 0.42f, 0.55f to 0.20f, 0.84f to 0.24f), 0.35f, 1.4f)
        add("s", arcPts(0.5f, 0.28f, 0.32f, 0.22f, -20f, -260f) + arcPts(0.5f, 0.72f, 0.32f, 0.22f, -90f, 160f), 0.35f, 1.4f)
        add("u", listOf(0.15f to 0.15f, 0.18f to 0.75f, 0.48f to 0.92f, 0.82f to 0.72f, 0.82f to 0.15f, 0.85f to 0.92f), 0.5f, 1.8f)
        add("v", listOf(0.12f to 0.12f, 0.50f to 0.90f, 0.88f to 0.12f), 0.5f, 1.8f)
        add("w", listOf(0.08f to 0.15f, 0.25f to 0.88f, 0.50f to 0.32f, 0.74f to 0.88f, 0.92f to 0.15f), 0.75f, 2.6f)
        add("y", listOf(0.18f to 0.12f, 0.48f to 0.55f, 0.82f to 0.12f, 0.42f to 0.94f), 0.35f, 1.3f)
        add("z", listOf(0.14f to 0.16f, 0.86f to 0.16f, 0.14f to 0.86f, 0.86f to 0.86f), 0.5f, 1.7f)

        // ---- UPPERCASE A-Z ----
        add("A", listOf(0.14f to 0.92f, 0.50f to 0.08f, 0.86f to 0.92f, 0.26f to 0.62f, 0.74f to 0.62f), 0.45f, 1.5f)
        add("B", listOf(0.20f to 0.08f, 0.20f to 0.92f, 0.22f to 0.10f) + arcPts(0.46f, 0.28f, 0.28f, 0.20f, -90f, 90f) + arcPts(0.48f, 0.70f, 0.32f, 0.22f, -90f, 90f), 0.35f, 1.25f)
        add("D", listOf(0.20f to 0.08f, 0.20f to 0.92f) + arcPts(0.28f, 0.50f, 0.54f, 0.42f, 90f, -90f), 0.4f, 1.35f)
        add("E", listOf(0.82f to 0.10f, 0.20f to 0.10f, 0.20f to 0.90f, 0.84f to 0.90f, 0.22f to 0.50f, 0.72f to 0.50f), 0.35f, 1.35f)
        add("F", listOf(0.82f to 0.10f, 0.20f to 0.10f, 0.20f to 0.92f, 0.22f to 0.50f, 0.72f to 0.50f), 0.35f, 1.3f)
        add("G", arcPts(0.52f, 0.50f, 0.38f, 0.42f, -35f, -340f) + listOf(0.88f to 0.52f, 0.54f to 0.52f), 0.45f, 1.5f)
        add("H", listOf(0.18f to 0.08f, 0.18f to 0.92f, 0.82f to 0.08f, 0.82f to 0.92f, 0.18f to 0.50f, 0.82f to 0.50f), 0.45f, 1.5f)
        add("J", listOf(0.68f to 0.10f, 0.68f to 0.74f, 0.46f to 0.92f, 0.18f to 0.76f), 0.3f, 1.15f)
        add("L", listOf(0.20f to 0.10f, 0.20f to 0.88f, 0.84f to 0.88f), 0.4f, 1.5f)
        add("M", listOf(0.12f to 0.92f, 0.16f to 0.10f, 0.50f to 0.58f, 0.84f to 0.10f, 0.88f to 0.92f), 0.6f, 2.0f)
        add("N", listOf(0.18f to 0.92f, 0.18f to 0.10f, 0.82f to 0.90f, 0.82f to 0.10f), 0.45f, 1.6f)
        add("P", listOf(0.20f to 0.92f, 0.20f to 0.10f) + arcPts(0.42f, 0.30f, 0.36f, 0.20f, -90f, 90f), 0.35f, 1.25f)
        add("Q", arcPts(0.48f, 0.48f, 0.36f, 0.40f, -90f, -450f) + listOf(0.56f to 0.64f, 0.88f to 0.94f), 0.5f, 1.5f)
        add("R", listOf(0.20f to 0.92f, 0.20f to 0.10f) + arcPts(0.42f, 0.30f, 0.34f, 0.20f, -90f, 90f) + listOf(0.44f to 0.50f, 0.84f to 0.92f), 0.35f, 1.3f)
        add("U", listOf(0.16f to 0.10f, 0.18f to 0.74f, 0.50f to 0.92f, 0.82f to 0.74f, 0.84f to 0.10f), 0.45f, 1.5f)

        // ---- SYMBOLS, BRACKETS, MATH, CURRENCY & GREEK ----
        add("(", arcPts(0.75f, 0.50f, 0.52f, 0.46f, -125f, -235f), 0.12f, 0.65f, sym = true)
        add(")", arcPts(0.25f, 0.50f, 0.52f, 0.46f, -55f, 55f), 0.12f, 0.65f, sym = true)
        add("[", listOf(0.75f to 0.08f, 0.25f to 0.08f, 0.25f to 0.92f, 0.75f to 0.92f), 0.15f, 0.75f, sym = true)
        add("]", listOf(0.25f to 0.08f, 0.75f to 0.08f, 0.75f to 0.92f, 0.25f to 0.92f), 0.15f, 0.75f, sym = true)
        add("{", listOf(0.75f to 0.08f, 0.45f to 0.18f, 0.45f to 0.42f, 0.20f to 0.50f, 0.45f to 0.58f, 0.45f to 0.82f, 0.75f to 0.92f), 0.15f, 0.8f, sym = true)
        add("}", listOf(0.25f to 0.08f, 0.55f to 0.18f, 0.55f to 0.42f, 0.80f to 0.50f, 0.55f to 0.58f, 0.55f to 0.82f, 0.25f to 0.92f), 0.15f, 0.8f, sym = true)
        add("<", listOf(0.84f to 0.14f, 0.16f to 0.50f, 0.84f to 0.86f), 0.35f, 1.6f, rev = true, sym = true)
        add(">", listOf(0.16f to 0.14f, 0.84f to 0.50f, 0.16f to 0.86f), 0.35f, 1.6f, rev = true, sym = true)
        add("^", listOf(0.14f to 0.84f, 0.50f to 0.16f, 0.86f to 0.84f), 0.45f, 2.2f, rev = true, sym = true)
        add("/", listOf(0.82f to 0.10f, 0.18f to 0.90f), 0.2f, 1.3f, rev = true, sym = true)
        add("\\", listOf(0.18f to 0.10f, 0.82f to 0.90f), 0.2f, 1.3f, rev = true, sym = true)
        add("@", arcPts(0.50f, 0.52f, 0.20f, 0.20f, -20f, -380f) + arcPts(0.50f, 0.50f, 0.42f, 0.42f, 20f, -320f), 0.6f, 1.5f, sym = true)
        add("&", listOf(0.84f to 0.88f, 0.24f to 0.32f, 0.42f to 0.10f, 0.64f to 0.24f, 0.16f to 0.72f, 0.46f to 0.90f, 0.86f to 0.52f), 0.5f, 1.5f, sym = true)
        add("?", arcPts(0.46f, 0.26f, 0.28f, 0.18f, -165f, 65f) + listOf(0.50f to 0.46f, 0.50f to 0.70f), 0.28f, 1.2f, sym = true)
        add("√", listOf(0.08f to 0.54f, 0.26f to 0.90f, 0.50f to 0.12f, 0.94f to 0.12f), 0.55f, 2.4f, sym = true)
        add("Σ", listOf(0.86f to 0.10f, 0.16f to 0.10f, 0.54f to 0.50f, 0.16f to 0.90f, 0.86f to 0.90f), 0.45f, 1.5f, sym = true)
        add("Δ", listOf(0.50f to 0.10f, 0.12f to 0.88f, 0.88f to 0.88f, 0.50f to 0.10f), 0.55f, 1.6f, rev = true, sym = true)
        add("α", listOf(0.86f to 0.22f, 0.50f to 0.84f, 0.18f to 0.56f, 0.48f to 0.18f, 0.86f to 0.82f), 0.65f, 1.8f, sym = true)
        add("β", listOf(0.22f to 0.94f, 0.28f to 0.10f) + arcPts(0.52f, 0.28f, 0.26f, 0.18f, -110f, 90f) + arcPts(0.54f, 0.68f, 0.28f, 0.22f, -90f, 150f), 0.3f, 1.15f, sym = true)
        add("θ", arcPts(0.50f, 0.50f, 0.34f, 0.44f, -90f, -450f) + listOf(0.18f to 0.50f, 0.82f to 0.50f), 0.4f, 1.25f, sym = true)
        add("€", arcPts(0.58f, 0.50f, 0.40f, 0.42f, -40f, -320f) + listOf(0.10f to 0.40f, 0.72f to 0.40f, 0.10f to 0.60f, 0.68f to 0.60f), 0.45f, 1.4f, sym = true)
        add("£", arcPts(0.56f, 0.24f, 0.26f, 0.18f, -20f, -180f) + listOf(0.30f to 0.26f, 0.28f to 0.88f, 0.84f to 0.88f, 0.14f to 0.52f, 0.68f to 0.52f), 0.4f, 1.35f, sym = true)
        add("✓", listOf(0.10f to 0.56f, 0.38f to 0.88f, 0.92f to 0.12f), 0.55f, 2.0f, sym = true)
        add("~", listOf(0.08f to 0.62f, 0.32f to 0.28f, 0.68f to 0.72f, 0.92f to 0.38f), 1.4f, 5.5f, sym = true)

        return list
    }
}
