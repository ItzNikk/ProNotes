package com.pronotes.app.editor

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Free on-device language model: Gemini Nano through ML Kit's Prompt API. It runs fully on the
 * tablet on supported devices (recent Pixel, Galaxy and others with Android AICore). Everywhere
 * else [available] is false and the app falls back to its own rule-based study tools.
 */
object Nano {
    @Volatile private var model: GenerativeModel? = null
    @Volatile var available: Boolean = false
        private set
    @Volatile private var checked = false

    /** Checks support once; starts the model download when the device can get it. */
    suspend fun ready(): Boolean {
        if (available) return true
        if (checked) return false
        checked = true
        return runCatching {
            val m = model ?: Generation.getClient().also { model = it }
            when (m.checkStatus()) {
                FeatureStatus.AVAILABLE -> { available = true; true }
                FeatureStatus.DOWNLOADABLE -> {
                    var ok = false
                    withTimeoutOrNull(120_000) {
                        m.download().collect { s -> if (s is DownloadStatus.DownloadCompleted) ok = true }
                    }
                    available = ok
                    ok
                }
                else -> false
            }
        }.getOrDefault(false)
    }

    /** One answer, or null when the model is not there or fails. */
    suspend fun ask(prompt: String, timeoutMs: Long = 45_000): String? {
        if (!ready()) return null
        val m = model ?: return null
        return withTimeoutOrNull(timeoutMs) {
            runCatching { m.generateContent(prompt).candidates.firstOrNull()?.text?.trim() }.getOrNull()
        }?.takeIf { it.isNotBlank() }
    }

    /** About 2,500 words fit the model's window; keep the start of long text. */
    fun clip(text: String, words: Int = 2200): String = text.split(Regex("\\s+")).take(words).joinToString(" ")

    // --------------------------------------------------------------- study helpers

    suspend fun flashcards(text: String, max: Int): List<StudyGen.Card>? {
        val out = ask(
            "Make up to $max study flashcards from the notes below. Use only facts from the notes. " +
                "Write each card on its own line exactly as: Q: <question> || A: <short answer>\n\nNOTES:\n" + clip(text),
        ) ?: return null
        return out.lines().mapNotNull { l ->
            val m = Regex("""Q:\s*(.+?)\s*\|\|\s*A:\s*(.+)""").find(l) ?: return@mapNotNull null
            StudyGen.Card(m.groupValues[1].trim(), m.groupValues[2].trim())
        }.take(max).takeIf { it.isNotEmpty() }
    }

    suspend fun quiz(text: String, max: Int): List<StudyGen.Mcq>? {
        val out = ask(
            "Write up to $max multiple-choice questions that test the notes below. Use only facts from the notes. " +
                "Each question has 4 options and exactly one is correct. Use exactly this format, one block per question:\n" +
                "Q: <question>\nA) <option>\nB) <option>\nC) <option>\nD) <option>\nAnswer: <letter>\nWhy: <one sentence>\n\nNOTES:\n" + clip(text, 1900),
            timeoutMs = 60_000,
        ) ?: return null
        val res = ArrayList<StudyGen.Mcq>()
        for (block in out.split(Regex("(?m)^\\s*Q:")).drop(1)) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val q = lines.firstOrNull() ?: continue
            val opts = lines.mapNotNull { Regex("""^([A-D])[).:]\s*(.+)$""").find(it)?.groupValues?.get(2) }
            val ans = lines.firstNotNullOfOrNull { Regex("""(?i)^answer:\s*([A-D])""").find(it)?.groupValues?.get(1) } ?: continue
            val why = lines.firstNotNullOfOrNull { Regex("""(?i)^why:\s*(.+)$""").find(it)?.groupValues?.get(1) } ?: ""
            if (opts.size < 2) continue
            res += StudyGen.Mcq(q, opts.take(4), (ans[0] - 'A').coerceIn(0, opts.size - 1), why)
        }
        return res.take(max).takeIf { it.isNotEmpty() }
    }

    suspend fun summary(text: String): List<String>? {
        val out = ask("Summarise the notes below as 4 to 7 short key points. One point per line, starting with \"- \".\n\nNOTES:\n" + clip(text)) ?: return null
        return out.lines().map { it.trim().trimStart('-', '*', '•', ' ') }.filter { it.length > 3 }.takeIf { it.isNotEmpty() }
    }

    suspend fun explain(selection: String, context: String): String? =
        ask("Explain this simply for a student, in 2 to 4 sentences: \"$selection\"\n\nIt comes from these notes:\n" + clip(context, 1200))
}
