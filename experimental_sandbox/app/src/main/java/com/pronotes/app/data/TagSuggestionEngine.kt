package com.pronotes.app.data

import kotlin.math.ln

/**
 * TF-IDF keyword extractor and tag suggester for note text.
 *
 * Given a target note and an optional corpus of other notes (for IDF weighting),
 * returns ranked keyword suggestions that could serve as tags.
 */
object TagSuggestionEngine {

    data class TagSuggestion(
        val tag: String,
        val score: Double,
    )

    private val STOP_WORDS = setOf(
        "a", "an", "and", "are", "as", "at", "be", "been", "but", "by", "can",
        "do", "for", "from", "has", "have", "he", "her", "his", "how", "i",
        "if", "in", "is", "it", "its", "me", "my", "not", "of", "on", "or",
        "our", "she", "so", "than", "that", "the", "their", "them", "then",
        "there", "they", "this", "to", "up", "us", "was", "we", "were",
        "what", "when", "which", "who", "will", "with", "you", "your",
        "also", "just", "no", "one", "two", "into", "about", "more",
        "some", "such", "each", "other", "these", "those",
    )

    private val WORD_RE = Regex("""[a-zA-Z]{3,}""")

    fun suggest(
        noteText: String,
        corpus: List<String> = emptyList(),
        maxTags: Int = 10,
        minCharLength: Int = 3,
    ): List<TagSuggestion> {
        if (noteText.isBlank()) return emptyList()

        val targetTokens = tokenize(noteText)
        if (targetTokens.isEmpty()) return emptyList()

        // Term frequency in target note
        val tf = mutableMapOf<String, Int>()
        for (tok in targetTokens) tf[tok] = (tf[tok] ?: 0) + 1
        val maxFreq = tf.values.maxOrNull() ?: 1

        // IDF: log((1 + N) / (1 + df)) + 1
        val allDocs = corpus.map { tokenize(it).toSet() }
        val N = (allDocs.size + 1).toDouble()

        val scores = tf.entries
            .filter { it.key.length >= minCharLength && it.key !in STOP_WORDS }
            .map { (term, freq) ->
                val tfNorm = freq.toDouble() / maxFreq
                val df = allDocs.count { term in it }
                val idf = ln((N + 1) / (df + 1).toDouble()) + 1.0
                TagSuggestion(tag = term, score = tfNorm * idf)
            }
            .sortedByDescending { it.score }

        return scores.take(maxTags)
    }

    private fun tokenize(text: String): List<String> =
        WORD_RE.findAll(text.lowercase())
            .map { it.value }
            .filter { it !in STOP_WORDS }
            .toList()
}
