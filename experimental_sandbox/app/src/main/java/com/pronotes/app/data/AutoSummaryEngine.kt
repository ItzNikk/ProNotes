package com.pronotes.app.data

import kotlin.math.sqrt

/**
 * Extractive automatic summarizer using sentence scoring.
 *
 * Scoring approach: each sentence gets a score based on
 * - Term frequency (TF) of non-stop words
 * - Position bonus (earlier sentences score slightly higher)
 * - Length penalty (very short or very long sentences penalised)
 *
 * Returns the top-N highest-scoring sentences in their original order.
 */
object AutoSummaryEngine {

    data class Summary(
        val sentences: List<String>,
        val compressionRatio: Double,
    )

    private val STOP_WORDS = setOf(
        "a", "an", "and", "are", "as", "at", "be", "been", "but", "by", "can",
        "do", "for", "from", "has", "have", "he", "her", "his", "how", "i",
        "if", "in", "is", "it", "its", "me", "my", "not", "of", "on", "or",
        "our", "she", "so", "than", "that", "the", "their", "them", "then",
        "there", "they", "this", "to", "up", "us", "was", "we", "were",
        "what", "when", "which", "who", "will", "with", "you", "your",
    )

    private val SENTENCE_SPLIT = Regex("""(?<=[.!?])\s+""")
    private val WORD_RE = Regex("""[a-zA-Z']+""")

    fun summarize(text: String, targetSentences: Int = 3): Summary {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return Summary(emptyList(), 0.0)

        val sentences = SENTENCE_SPLIT.split(trimmed)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (sentences.size <= targetSentences) {
            return Summary(sentences, 1.0)
        }

        // Build global TF map
        val tf = mutableMapOf<String, Int>()
        for (s in sentences) {
            for (word in tokenize(s)) {
                tf[word] = (tf[word] ?: 0) + 1
            }
        }
        val maxTf = tf.values.maxOrNull()?.toDouble() ?: 1.0

        // Score each sentence
        val scored = sentences.mapIndexed { idx, sentence ->
            val words = tokenize(sentence)
            val wordCount = words.size
            if (wordCount == 0) {
                return@mapIndexed Pair(0.0, idx)
            }
            val tfScore = words.sumOf { (tf[it] ?: 0).toDouble() / maxTf } / wordCount
            val posBonus = 1.0 - (idx.toDouble() / sentences.size) * 0.3 // earlier = slightly higher
            val lenPenalty = if (wordCount < 5 || wordCount > 60) 0.6 else 1.0
            val score = tfScore * posBonus * lenPenalty
            Pair(score, idx)
        }

        val topIndices = scored
            .sortedByDescending { it.first }
            .take(targetSentences)
            .map { it.second }
            .toSortedSet()

        val selected = topIndices.map { sentences[it] }
        val compression = targetSentences.toDouble() / sentences.size
        return Summary(selected, sqrt(compression))
    }

    private fun tokenize(text: String): List<String> =
        WORD_RE.findAll(text.lowercase())
            .map { it.value }
            .filter { it !in STOP_WORDS && it.length > 2 }
            .toList()
}
