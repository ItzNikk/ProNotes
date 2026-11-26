package com.pronotes.app.editor

import android.content.Context
import androidx.ink.brush.Brush
import com.pronotes.app.data.AtomicFiles
import com.pronotes.app.data.PaperStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Offline Personal Handwriting Font (Letter Library).
 *
 * Each character stores up to 3 hand-drawn samples in normalized [0..1] cell coordinates
 * anchored to standardized ruled-paper guides:
 *   - Cap line:       y = 0.16
 *   - x-height line:  y = 0.42
 *   - Baseline:       y = 0.74
 *   - Descender line: y = 0.92
 *
 * Typed text is synthesized into real Jetpack Ink [InkItem] strokes with randomized copy
 * selection and subtle organic scale, slant, baseline, and spacing jitter so every letter
 * feels hand-written and works seamlessly with Eraser, Lasso, Resize, Recolor, and Undo.
 */
object HandwritingGuides {
    const val CAP_LINE = 0.16f
    const val X_HEIGHT_LINE = 0.42f
    const val BASELINE = 0.74f
    const val DESCENDER_LINE = 0.92f
    const val COPIES_PER_CHAR = 3
}

object HandwritingFontChars {
    val LOWER: List<String> = ('a'..'z').map { it.toString() }
    val UPPER: List<String> = ('A'..'Z').map { it.toString() }
    val DIGITS: List<String> = ('0'..'9').map { it.toString() }
    val PUNCT: List<String> = listOf(".", ",", "?", "!")
    val ALL: List<String> = LOWER + UPPER + DIGITS + PUNCT
}

@Serializable
data class GlyphPt(val x: Float, val y: Float)

@Serializable
data class GlyphSample(
    val strokes: List<List<GlyphPt>> = emptyList(),
    val minX: Float = 0.35f,
    val maxX: Float = 0.65f,
) {
    val isRecorded: Boolean get() = strokes.any { it.isNotEmpty() }
}

@Serializable
data class HandwritingProfile(
    val glyphs: Map<String, List<GlyphSample>> = emptyMap(),
) {
    fun slotsFor(charKey: String): List<GlyphSample> {
        val raw = glyphs[charKey].orEmpty()
        return List(HandwritingGuides.COPIES_PER_CHAR) { i -> raw.getOrNull(i) ?: GlyphSample() }
    }

    fun recordedSamples(charKey: String): List<GlyphSample> =
        glyphs[charKey].orEmpty().filter { it.isRecorded }

    fun recordedCount(charKey: String): Int = recordedSamples(charKey).size

    fun trainedCharCount(): Int =
        HandwritingFontChars.ALL.count { recordedCount(it) > 0 }

    fun totalSampleCount(): Int =
        HandwritingFontChars.ALL.sumOf { recordedCount(it) }
}

class HandwritingFontStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "handwriting_font.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val _profile = MutableStateFlow(load())
    val profile: StateFlow<HandwritingProfile> = _profile.asStateFlow()

    private fun load(): HandwritingProfile = runCatching {
        if (!file.exists()) HandwritingProfile()
        else json.decodeFromString<HandwritingProfile>(file.readText())
    }.getOrElse { HandwritingProfile() }

    private fun persist(next: HandwritingProfile) {
        _profile.value = next
        runCatching {
            AtomicFiles.writeTextAtomic(file, json.encodeToString(HandwritingProfile.serializer(), next))
        }
    }

    fun saveSample(
        charKey: String,
        slotIndex: Int,
        rawStrokes: List<List<Pair<Float, Float>>>,
        boxW: Float,
        boxH: Float,
    ) {
        if (slotIndex !in 0 until HandwritingGuides.COPIES_PER_CHAR) return
        val w = boxW.coerceAtLeast(1f)
        val h = boxH.coerceAtLeast(1f)
        val normalized = rawStrokes.mapNotNull { stroke ->
            if (stroke.isEmpty()) return@mapNotNull null
            val pts = ArrayList<GlyphPt>(stroke.size)
            var lastX = Float.NaN
            var lastY = Float.NaN
            for ((rx, ry) in stroke) {
                val nx = (rx / w).coerceIn(-0.08f, 1.08f)
                val ny = (ry / h).coerceIn(-0.08f, 1.08f)
                if (lastX.isNaN() || hypot((nx - lastX).toDouble(), (ny - lastY).toDouble()) >= 0.008) {
                    pts += GlyphPt(nx, ny)
                    lastX = nx
                    lastY = ny
                }
            }
            // Preserve the final stroke endpoint for crisp corners & short dots.
            val end = stroke.last()
            val ex = (end.first / w).coerceIn(-0.08f, 1.08f)
            val ey = (end.second / h).coerceIn(-0.08f, 1.08f)
            if (pts.isEmpty() || pts.last().x != ex || pts.last().y != ey) {
                pts += GlyphPt(ex, ey)
            }
            pts.takeIf { it.isNotEmpty() }
        }

        val sample = if (normalized.isEmpty()) {
            GlyphSample()
        } else {
            var minX = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            for (s in normalized) {
                for (p in s) {
                    if (p.x < minX) minX = p.x
                    if (p.x > maxX) maxX = p.x
                }
            }
            if (!minX.isFinite() || !maxX.isFinite()) {
                minX = 0.35f; maxX = 0.65f
            }
            GlyphSample(normalized, minX, maxX)
        }

        val current = _profile.value
        val slots = current.slotsFor(charKey).toMutableList()
        slots[slotIndex] = sample
        val updatedMap = current.glyphs.toMutableMap()
        if (slots.any { it.isRecorded }) updatedMap[charKey] = slots else updatedMap.remove(charKey)
        persist(current.copy(glyphs = updatedMap))
    }

    fun clearSample(charKey: String, slotIndex: Int) {
        if (slotIndex !in 0 until HandwritingGuides.COPIES_PER_CHAR) return
        val current = _profile.value
        val slots = current.slotsFor(charKey).toMutableList()
        slots[slotIndex] = GlyphSample()
        val updatedMap = current.glyphs.toMutableMap()
        if (slots.any { it.isRecorded }) updatedMap[charKey] = slots else updatedMap.remove(charKey)
        persist(current.copy(glyphs = updatedMap))
    }

    fun clearChar(charKey: String) {
        val current = _profile.value
        if (charKey !in current.glyphs) return
        val updatedMap = current.glyphs.toMutableMap().apply { remove(charKey) }
        persist(current.copy(glyphs = updatedMap))
    }

    fun clearAll() {
        persist(HandwritingProfile())
    }

    companion object {
        @Volatile
        private var instance: HandwritingFontStore? = null

        fun get(context: Context): HandwritingFontStore =
            instance ?: synchronized(this) {
                instance ?: HandwritingFontStore(context).also { instance = it }
            }
    }
}

object HandwritingSynthesizer {

    private data class RuleSpec(val firstBaseline: Float, val step: Float)

    private fun ruleSpecFor(paper: PaperStyle, lineScale: Float): RuleSpec? {
        val ls = lineScale.coerceIn(0.6f, 1.8f)
        return when (paper) {
            PaperStyle.RULED -> RuleSpec(180f, 52f * ls)
            PaperStyle.NARROW -> RuleSpec(160f, 38f * ls)
            PaperStyle.COLLEGE -> RuleSpec(170f, 42f * ls)
            PaperStyle.WIDE -> RuleSpec(190f, 64f * ls)
            PaperStyle.LEGAL -> RuleSpec(220f, 50f * ls)
            PaperStyle.HANDWRITING -> RuleSpec(180f + 96f * ls, 96f * ls * 1.6f)
            PaperStyle.GRID, PaperStyle.DOTTED -> RuleSpec(40f * ls, 40f * ls)
            PaperStyle.SMALL_GRID -> RuleSpec(24f * ls, 24f * ls)
            else -> null
        }
    }

    /**
     * Synthesizes typed [text] into page-space polylines aligned onto ruled paper lines
     * with natural per-character variation (random sample pick, slight scale, slant,
     * baseline drift, and spacing jitter) and automatic edge word-wrapping.
     */
    fun synthesizeStrokes(
        text: String,
        startX: Float,
        startY: Float,
        maxWidth: Float,
        fontSize: Float,
        paper: PaperStyle = PaperStyle.BLANK,
        lineScale: Float = 1f,
        profile: HandwritingProfile,
        seed: Long = System.nanoTime(),
    ): List<List<Pair<Float, Float>>> {
        if (text.isBlank()) return emptyList()

        val rng = Random(seed)
        val cellH = (fontSize * 1.58f).coerceIn(22f, 220f)
        val cellW = cellH * 0.86f
        val safeMaxW = maxWidth.coerceAtLeast(cellW * 2.5f)
        val rightEdge = startX + safeMaxW

        val rules = ruleSpecFor(paper, lineScale)
        val rawLineStep = cellH * 0.96f
        val lineStep: Float
        var baselineY: Float

        if (rules != null) {
            val rulesPerLine = max(1, ceil((rawLineStep * 0.82f) / rules.step).toInt())
            lineStep = rules.step * rulesPerLine
            val desiredFirst = startY + cellH * HandwritingGuides.BASELINE
            val idx = ceil(((desiredFirst - rules.firstBaseline) / rules.step).toDouble()).toInt().coerceAtLeast(0)
            baselineY = rules.firstBaseline + idx * rules.step
        } else {
            lineStep = rawLineStep
            baselineY = startY + cellH * HandwritingGuides.BASELINE
        }

        val lastSlotByChar = HashMap<String, Int>()

        fun pickSample(ch: Char): Pair<GlyphSample, Boolean> {
            val key = ch.toString()
            val recorded = profile.recordedSamples(key)
            if (recorded.isNotEmpty()) {
                val chosenIdx = if (recorded.size == 1) {
                    0
                } else {
                    val prev = lastSlotByChar[key] ?: -1
                    var idx = rng.nextInt(recorded.size)
                    if (idx == prev) idx = (idx + 1 + rng.nextInt(recorded.size - 1)) % recorded.size
                    idx
                }
                lastSlotByChar[key] = chosenIdx
                return recorded[chosenIdx] to true
            }
            return fallbackGlyph(ch) to false
        }

        fun advanceFor(ch: Char, sample: GlyphSample): Float {
            if (ch == ' ') return cellW * 0.44f
            if (ch == '\t') return cellW * 1.6f
            val span = (sample.maxX - sample.minX).coerceIn(0.10f, 0.95f)
            val pad = when (ch) {
                '.', ',', '!', '\'', ':', ';' -> 0.14f
                'i', 'l', 'I', '1' -> 0.13f
                else -> 0.11f
            }
            return cellW * (span + pad)
        }

        val outStrokes = ArrayList<List<Pair<Float, Float>>>()
        val paragraphs = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')

        for ((pIdx, para) in paragraphs.withIndex()) {
            if (pIdx > 0) baselineY += lineStep
            var cursorX = startX

            // Tokenize into words and whitespace runs so wrapping respects word boundaries cleanly.
            val tokens = tokenizeLine(para)
            for (token in tokens) {
                if (token.isEmpty()) continue
                val isSpace = token.all { it == ' ' || it == '\t' }
                if (isSpace) {
                    // Skip leading spaces right after a soft line wrap.
                    if (cursorX > startX + 0.5f) {
                        for (sp in token) {
                            val adv = (if (sp == '\t') cellW * 1.6f else cellW * 0.44f) * (0.93f + rng.nextFloat() * 0.14f)
                            cursorX += adv
                        }
                    }
                    continue
                }

                // Pre-select samples for this word so measurement & rendering match exactly.
                val picked = token.map { ch ->
                    val (sample, isUser) = pickSample(ch)
                    Triple(ch, sample, isUser)
                }
                val wordWidth = picked.sumOf { (ch, sample, _) -> advanceFor(ch, sample).toDouble() }.toFloat()

                // Wrap onto the next line if the word would cross the right boundary.
                if (cursorX > startX + 0.5f && cursorX + wordWidth > rightEdge) {
                    cursorX = startX
                    baselineY += lineStep
                }

                for ((ch, sample, isUser) in picked) {
                    val baseAdv = advanceFor(ch, sample)
                    // Break ultra-long words that exceed a full line width.
                    if (cursorX > startX + 0.5f && cursorX + baseAdv > rightEdge) {
                        cursorX = startX
                        baselineY += lineStep
                    }

                    // Organic human variation per glyph instance.
                    val scaleJitter = 0.945f + rng.nextFloat() * 0.11f
                    val slant = -0.058f + rng.nextFloat() * 0.116f
                    val yDrift = (-0.022f + rng.nextFloat() * 0.044f) * cellH
                    val spacingJitter = 0.94f + rng.nextFloat() * 0.12f

                    val gW = cellW * scaleJitter
                    val gH = cellH * scaleJitter
                    val anchorBaseY = baselineY + yDrift
                    val anchorLeftX = cursorX - sample.minX * gW + cellW * 0.03f
                    val strokeMicroNoise = if (isUser) 0.0025f else 0.009f

                    for (stroke in sample.strokes) {
                        if (stroke.isEmpty()) continue
                        val mapped = ArrayList<Pair<Float, Float>>(stroke.size + 1)
                        val phase = rng.nextFloat() * (2f * PI.toFloat())
                        for ((ptIdx, pt) in stroke.withIndex()) {
                            val dyFromBase = (pt.y - HandwritingGuides.BASELINE) * gH
                            val wobbleX = sin(ptIdx * 0.9f + phase) * gW * strokeMicroNoise
                            val wobbleY = cos(ptIdx * 0.8f + phase) * gH * strokeMicroNoise
                            // Slant tilts tops slightly right/left around the baseline pivot.
                            val px = anchorLeftX + pt.x * gW - dyFromBase * slant + wobbleX
                            val py = anchorBaseY + dyFromBase + wobbleY
                            mapped += px to py
                        }
                        if (mapped.size == 1) {
                            val (x0, y0) = mapped[0]
                            mapped += (x0 + (fontSize * 0.04f).coerceAtLeast(0.9f)) to (y0 + (fontSize * 0.03f).coerceAtLeast(0.7f))
                        }
                        outStrokes += mapped
                    }

                    cursorX += baseAdv * spacingJitter
                }
            }
        }

        return outStrokes
    }

    /**
     * Synthesizes typed [text] directly into native Jetpack Ink [InkItem] strokes
     * that live on the page just like hand-drawn stylus strokes.
     */
    fun synthesizeInkItems(
        text: String,
        startX: Float,
        startY: Float,
        maxWidth: Float,
        fontSize: Float,
        color: Int,
        paper: PaperStyle,
        lineScale: Float,
        baseBrush: Brush,
        profile: HandwritingProfile,
    ): List<InkItem> {
        val rawStrokes = synthesizeStrokes(
            text = text,
            startX = startX,
            startY = startY,
            maxWidth = maxWidth,
            fontSize = fontSize,
            paper = paper,
            lineScale = lineScale,
            profile = profile,
        )
        if (rawStrokes.isEmpty()) return emptyList()

        // Scale stroke thickness gracefully with the chosen text size while respecting the user's pen feel.
        val strokeWidth = max(baseBrush.size, (fontSize * 0.085f).coerceIn(2.2f, 12f))
        val inkBrush = Brush.createWithColorIntArgb(
            baseBrush.family,
            color,
            strokeWidth,
            baseBrush.epsilon,
        )
        return rawStrokes.mapNotNull { pts ->
            if (pts.size < 2) null
            else InkItem(ItemIds.next(), InkCodec.strokeFromPoints(pts, inkBrush))
        }
    }

    private fun tokenizeLine(line: String): List<String> {
        if (line.isEmpty()) return emptyList()
        val result = ArrayList<String>()
        val sb = StringBuilder()
        var inSpace = line[0] == ' ' || line[0] == '\t'
        for (ch in line) {
            val sp = ch == ' ' || ch == '\t'
            if (sp != inSpace) {
                result += sb.toString()
                sb.setLength(0)
                inSpace = sp
            }
            sb.append(ch)
        }
        if (sb.isNotEmpty()) result += sb.toString()
        return result
    }

    // ------------------------------------------------------------------ fallback vector glyphs
    // Lightweight single-stroke handwriting geometry used for characters not yet trained by the user.

    private fun s(vararg coords: Float): List<GlyphPt> {
        val list = ArrayList<GlyphPt>(coords.size / 2)
        var i = 0
        while (i + 1 < coords.size) {
            list += GlyphPt(coords[i], coords[i + 1])
            i += 2
        }
        return list
    }

    private fun arc(
        cx: Float,
        cy: Float,
        rx: Float,
        ry: Float,
        startDeg: Float,
        endDeg: Float,
        steps: Int = 10,
    ): List<GlyphPt> {
        val out = ArrayList<GlyphPt>(steps + 1)
        for (i in 0..steps) {
            val deg = startDeg + (endDeg - startDeg) * (i.toFloat() / steps)
            val rad = deg * (PI / 180.0)
            out += GlyphPt(cx + rx * cos(rad).toFloat(), cy + ry * sin(rad).toFloat())
        }
        return out
    }

    private fun glyphOf(vararg strokes: List<GlyphPt>): GlyphSample {
        val valid = strokes.filter { it.isNotEmpty() }
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        for (st in valid) {
            for (p in st) {
                minX = min(minX, p.x)
                maxX = max(maxX, p.x)
            }
        }
        if (!minX.isFinite() || !maxX.isFinite()) {
            minX = 0.35f; maxX = 0.65f
        }
        return GlyphSample(valid, minX, maxX)
    }

    private val fallbackCache: Map<Char, GlyphSample> by lazy {
        val c = HandwritingGuides.CAP_LINE       // 0.16
        val x = HandwritingGuides.X_HEIGHT_LINE  // 0.42
        val b = HandwritingGuides.BASELINE       // 0.74
        val d = HandwritingGuides.DESCENDER_LINE // 0.92
        val m = (x + b) * 0.5f                   // 0.58
        val midCap = (c + b) * 0.5f              // 0.45

        mapOf(
            // Lowercase a-z
            'a' to glyphOf(arc(0.45f, m, 0.18f, 0.16f, -35f, 325f, 12), s(0.63f, x, 0.62f, b, 0.67f, b)),
            'b' to glyphOf(s(0.32f, c, 0.32f, b), arc(0.47f, m, 0.17f, 0.16f, -150f, 150f, 12)),
            'c' to glyphOf(arc(0.50f, m, 0.18f, 0.16f, -45f, -315f, 12)),
            'd' to glyphOf(arc(0.46f, m, 0.17f, 0.16f, -35f, 325f, 12), s(0.63f, c, 0.63f, b)),
            'e' to glyphOf(s(0.32f, m, 0.66f, m) + arc(0.49f, m, 0.17f, 0.16f, 0f, -315f, 12)),
            'f' to glyphOf(arc(0.55f, c + 0.08f, 0.12f, 0.09f, -30f, -180f, 7) + s(0.43f, b), s(0.32f, x + 0.02f, 0.60f, x + 0.02f)),
            'g' to glyphOf(arc(0.46f, m, 0.17f, 0.16f, -35f, 325f, 12), s(0.63f, x, 0.63f, d - 0.06f) + arc(0.48f, d - 0.07f, 0.15f, 0.08f, 0f, 165f, 8)),
            'h' to glyphOf(s(0.32f, c, 0.32f, b), s(0.32f, x + 0.07f) + arc(0.48f, x + 0.09f, 0.16f, 0.09f, -170f, 0f, 7) + s(0.64f, b)),
            'i' to glyphOf(s(0.48f, x, 0.48f, b), s(0.48f, x - 0.11f, 0.49f, x - 0.10f)),
            'j' to glyphOf(s(0.54f, x, 0.54f, d - 0.07f) + arc(0.43f, d - 0.07f, 0.11f, 0.08f, 0f, 160f, 7), s(0.54f, x - 0.11f, 0.55f, x - 0.10f)),
            'k' to glyphOf(s(0.32f, c, 0.32f, b), s(0.63f, x, 0.33f, m + 0.02f, 0.65f, b)),
            'l' to glyphOf(s(0.46f, c, 0.46f, b, 0.52f, b)),
            'm' to glyphOf(s(0.22f, x, 0.22f, b), arc(0.36f, x + 0.09f, 0.14f, 0.09f, -180f, 0f, 6) + s(0.50f, b), arc(0.64f, x + 0.09f, 0.14f, 0.09f, -180f, 0f, 6) + s(0.78f, b)),
            'n' to glyphOf(s(0.32f, x, 0.32f, b), arc(0.48f, x + 0.09f, 0.16f, 0.09f, -175f, 0f, 7) + s(0.64f, b)),
            'o' to glyphOf(arc(0.48f, m, 0.18f, 0.16f, -90f, 270f, 14)),
            'p' to glyphOf(s(0.32f, x, 0.32f, d), arc(0.48f, m, 0.17f, 0.16f, -155f, 155f, 12)),
            'q' to glyphOf(arc(0.46f, m, 0.17f, 0.16f, -35f, 325f, 12), s(0.63f, x, 0.63f, d, 0.71f, d - 0.06f)),
            'r' to glyphOf(s(0.35f, x, 0.35f, b), arc(0.50f, x + 0.10f, 0.15f, 0.10f, -170f, -35f, 6)),
            's' to glyphOf(arc(0.49f, x + 0.08f, 0.15f, 0.08f, -20f, -265f, 8) + arc(0.49f, b - 0.08f, 0.15f, 0.08f, -85f, 165f, 8)),
            't' to glyphOf(s(0.46f, c + 0.06f, 0.46f, b - 0.04f, 0.51f, b, 0.58f, b - 0.02f), s(0.34f, x + 0.02f, 0.60f, x + 0.02f)),
            'u' to glyphOf(s(0.32f, x, 0.32f, b - 0.09f) + arc(0.48f, b - 0.09f, 0.16f, 0.09f, 180f, 0f, 7), s(0.64f, x, 0.64f, b)),
            'v' to glyphOf(s(0.31f, x, 0.48f, b, 0.65f, x)),
            'w' to glyphOf(s(0.22f, x, 0.35f, b, 0.49f, x + 0.06f, 0.62f, b, 0.76f, x)),
            'x' to glyphOf(s(0.32f, x, 0.64f, b), s(0.64f, x, 0.32f, b)),
            'y' to glyphOf(s(0.32f, x, 0.48f, b - 0.02f), s(0.64f, x, 0.41f, d)),
            'z' to glyphOf(s(0.32f, x, 0.64f, x, 0.32f, b, 0.65f, b)),

            // Uppercase A-Z
            'A' to glyphOf(s(0.24f, b, 0.49f, c, 0.74f, b), s(0.33f, m - 0.03f, 0.65f, m - 0.03f)),
            'B' to glyphOf(s(0.28f, c, 0.28f, b), s(0.28f, c) + arc(0.48f, (c + midCap) * 0.5f, 0.20f, 0.14f, -90f, 90f, 8) + s(0.28f, midCap), arc(0.50f, (midCap + b) * 0.5f, 0.22f, 0.145f, -90f, 90f, 8) + s(0.28f, b)),
            'C' to glyphOf(arc(0.52f, midCap, 0.24f, 0.29f, -42f, -318f, 14)),
            'D' to glyphOf(s(0.28f, c, 0.28f, b), s(0.28f, c) + arc(0.45f, midCap, 0.26f, 0.29f, -90f, 90f, 12) + s(0.28f, b)),
            'E' to glyphOf(s(0.68f, c, 0.28f, c, 0.28f, b, 0.69f, b), s(0.28f, midCap, 0.60f, midCap)),
            'F' to glyphOf(s(0.68f, c, 0.28f, c, 0.28f, b), s(0.28f, midCap, 0.60f, midCap)),
            'G' to glyphOf(arc(0.51f, midCap, 0.24f, 0.29f, -35f, -345f, 15) + s(0.75f, midCap + 0.03f, 0.54f, midCap + 0.03f)),
            'H' to glyphOf(s(0.28f, c, 0.28f, b), s(0.70f, c, 0.70f, b), s(0.28f, midCap, 0.70f, midCap)),
            'I' to glyphOf(s(0.49f, c, 0.49f, b), s(0.38f, c, 0.60f, c), s(0.38f, b, 0.60f, b)),
            'J' to glyphOf(s(0.64f, c, 0.64f, b - 0.13f) + arc(0.46f, b - 0.13f, 0.18f, 0.13f, 0f, 160f, 8)),
            'K' to glyphOf(s(0.28f, c, 0.28f, b), s(0.70f, c, 0.29f, midCap + 0.03f, 0.72f, b)),
            'L' to glyphOf(s(0.30f, c, 0.30f, b, 0.69f, b)),
            'M' to glyphOf(s(0.22f, b, 0.24f, c, 0.50f, midCap + 0.06f, 0.76f, c, 0.78f, b)),
            'N' to glyphOf(s(0.27f, b, 0.27f, c, 0.71f, b, 0.71f, c)),
            'O' to glyphOf(arc(0.50f, midCap, 0.24f, 0.29f, -90f, 270f, 16)),
            'P' to glyphOf(s(0.28f, c, 0.28f, b), s(0.28f, c) + arc(0.48f, c + 0.15f, 0.22f, 0.15f, -90f, 90f, 10) + s(0.28f, c + 0.30f)),
            'Q' to glyphOf(arc(0.50f, midCap, 0.24f, 0.29f, -90f, 270f, 16), s(0.54f, b - 0.12f, 0.76f, b + 0.04f)),
            'R' to glyphOf(s(0.28f, c, 0.28f, b), s(0.28f, c) + arc(0.48f, c + 0.15f, 0.22f, 0.15f, -90f, 90f, 10) + s(0.28f, c + 0.30f), s(0.46f, c + 0.30f, 0.72f, b)),
            'S' to glyphOf(arc(0.50f, c + 0.14f, 0.21f, 0.14f, -20f, -265f, 9) + arc(0.50f, b - 0.14f, 0.22f, 0.145f, -85f, 165f, 9)),
            'T' to glyphOf(s(0.24f, c, 0.76f, c), s(0.50f, c, 0.50f, b)),
            'U' to glyphOf(s(0.28f, c, 0.28f, b - 0.16f) + arc(0.50f, b - 0.16f, 0.22f, 0.16f, 180f, 0f, 10) + s(0.72f, c)),
            'V' to glyphOf(s(0.25f, c, 0.50f, b, 0.75f, c)),
            'W' to glyphOf(s(0.18f, c, 0.34f, b, 0.50f, midCap - 0.04f, 0.66f, b, 0.82f, c)),
            'X' to glyphOf(s(0.26f, c, 0.74f, b), s(0.74f, c, 0.26f, b)),
            'Y' to glyphOf(s(0.25f, c, 0.50f, midCap + 0.02f, 0.75f, c), s(0.50f, midCap + 0.02f, 0.50f, b)),
            'Z' to glyphOf(s(0.26f, c, 0.74f, c, 0.26f, b, 0.74f, b)),

            // Digits 0-9
            '0' to glyphOf(arc(0.50f, midCap, 0.20f, 0.28f, -90f, 270f, 15)),
            '1' to glyphOf(s(0.38f, c + 0.09f, 0.50f, c, 0.50f, b), s(0.36f, b, 0.64f, b)),
            '2' to glyphOf(arc(0.49f, c + 0.14f, 0.19f, 0.14f, -165f, 35f, 9) + s(0.29f, b, 0.70f, b)),
            '3' to glyphOf(arc(0.48f, c + 0.14f, 0.19f, 0.14f, -155f, 90f, 9) + arc(0.48f, b - 0.145f, 0.21f, 0.145f, -90f, 160f, 9)),
            '4' to glyphOf(s(0.60f, b, 0.60f, c, 0.28f, midCap + 0.07f, 0.72f, midCap + 0.07f)),
            '5' to glyphOf(s(0.67f, c, 0.34f, c, 0.32f, midCap - 0.02f) + arc(0.49f, b - 0.15f, 0.20f, 0.15f, -125f, 155f, 10)),
            '6' to glyphOf(arc(0.52f, midCap, 0.22f, 0.29f, -45f, -210f, 9) + arc(0.50f, b - 0.15f, 0.19f, 0.15f, 180f, -180f, 12)),
            '7' to glyphOf(s(0.29f, c, 0.70f, c, 0.42f, b)),
            '8' to glyphOf(arc(0.50f, c + 0.135f, 0.17f, 0.135f, -90f, 270f, 12), arc(0.50f, b - 0.155f, 0.20f, 0.155f, -90f, 270f, 12)),
            '9' to glyphOf(arc(0.49f, c + 0.15f, 0.19f, 0.15f, 0f, 360f, 12), s(0.68f, c + 0.15f, 0.62f, b)),

            // Core punctuation + common symbols
            '.' to glyphOf(s(0.48f, b - 0.03f, 0.50f, b)),
            ',' to glyphOf(s(0.50f, b - 0.03f, 0.45f, b + 0.10f)),
            '?' to glyphOf(arc(0.49f, c + 0.13f, 0.18f, 0.13f, -165f, 55f, 9) + s(0.49f, midCap + 0.12f), s(0.49f, b - 0.02f, 0.50f, b)),
            '!' to glyphOf(s(0.50f, c, 0.50f, b - 0.14f), s(0.50f, b - 0.02f, 0.51f, b)),
            '\'' to glyphOf(s(0.49f, c, 0.46f, c + 0.12f)),
            '"' to glyphOf(s(0.41f, c, 0.39f, c + 0.12f), s(0.57f, c, 0.55f, c + 0.12f)),
            '-' to glyphOf(s(0.32f, m, 0.66f, m)),
            ':' to glyphOf(s(0.49f, x + 0.04f, 0.50f, x + 0.06f), s(0.49f, b - 0.03f, 0.50f, b)),
            ';' to glyphOf(s(0.49f, x + 0.04f, 0.50f, x + 0.06f), s(0.50f, b - 0.03f, 0.45f, b + 0.10f)),
            '(' to glyphOf(arc(0.60f, midCap, 0.18f, 0.30f, -110f, -250f, 10)),
            ')' to glyphOf(arc(0.40f, midCap, 0.18f, 0.30f, -70f, 70f, 10)),
            '/' to glyphOf(s(0.65f, c, 0.35f, b)),
            '+' to glyphOf(s(0.50f, x, 0.50f, b - 0.04f), s(0.32f, m, 0.68f, m)),
            '=' to glyphOf(s(0.32f, m - 0.06f, 0.68f, m - 0.06f), s(0.32f, m + 0.06f, 0.68f, m + 0.06f)),
        )
    }

    fun fallbackGlyph(ch: Char): GlyphSample =
        fallbackCache[ch] ?: fallbackCache[ch.lowercaseChar()] ?: GlyphSample(emptyList(), 0.35f, 0.65f)
}
