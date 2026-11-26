package com.pronotes.app.data

import kotlin.math.ln
import kotlin.math.sqrt

/**
 * TF-IDF + cosine-similarity full-text search ranker for a collection of notes.
 *
 * Usage:
 *   val ranker = SearchRanker(noteTexts)
 *   val results = ranker.search("photosynthesis chlorophyll", topK = 5)
 */
class SearchRanker(noteTexts: List<String>) {

    data class SearchResult(
        val docIndex: Int,
        val score: Double,
        val matchedTerms: List<String>,
    )

    private val WORD_RE = Regex("""[a-zA-Z']{2,}""")
    private val STOP_WORDS = setOf(
        "a", "an", "and", "are", "as", "at", "be", "been", "but", "by", "can",
        "do", "for", "from", "has", "have", "he", "her", "his", "how", "i",
        "if", "in", "is", "it", "its", "me", "my", "not", "of", "on", "or",
        "our", "she", "so", "than", "that", "the", "their", "them", "then",
        "there", "they", "this", "to", "up", "us", "was", "we", "were",
        "what", "when", "which", "who", "will", "with", "you", "your",
    )

    private val N = noteTexts.size
    private val tokenized: List<List<String>> = noteTexts.map { tokenize(it) }
    private val df: Map<String, Int>
    private val idf: Map<String, Double>
    private val tfidfVectors: List<Map<String, Double>>

    init {
        val dfMut = mutableMapOf<String, Int>()
        for (tokens in tokenized) {
            tokens.toSet().forEach { dfMut[it] = (dfMut[it] ?: 0) + 1 }
        }
        df = dfMut
        idf = dfMut.mapValues { (_, docFreq) ->
            ln((N + 1).toDouble() / (docFreq + 1).toDouble()) + 1.0
        }
        tfidfVectors = tokenized.map { tokens ->
            val tf = mutableMapOf<String, Int>()
            tokens.forEach { tf[it] = (tf[it] ?: 0) + 1 }
            val maxTf = tf.values.maxOrNull() ?: 1
            tf.entries.associate { (term, freq) ->
                term to (freq.toDouble() / maxTf * (idf[term] ?: 1.0))
            }
        }
    }

    fun search(query: String, topK: Int = 10): List<SearchResult> {
        if (query.isBlank() || N == 0) return emptyList()
        val qTokens = tokenize(query)
        if (qTokens.isEmpty()) return emptyList()

        // Build query TF-IDF vector
        val qTf = mutableMapOf<String, Int>()
        qTokens.forEach { qTf[it] = (qTf[it] ?: 0) + 1 }
        val maxQTf = qTf.values.maxOrNull() ?: 1
        val qVec = qTf.entries.associate { (term, freq) ->
            term to (freq.toDouble() / maxQTf * (idf[term] ?: (ln((N + 1).toDouble()) + 1.0)))
        }

        val results = tfidfVectors.mapIndexed { idx, docVec ->
            val cosine = cosineSim(qVec, docVec)
            val matched = qVec.keys.filter { it in docVec }.distinct()
            SearchResult(docIndex = idx, score = cosine, matchedTerms = matched)
        }

        return results
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .take(topK)
    }

    private fun cosineSim(a: Map<String, Double>, b: Map<String, Double>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val dot = a.entries.sumOf { (term, wa) -> wa * (b[term] ?: 0.0) }
        val normA = sqrt(a.values.sumOf { it * it })
        val normB = sqrt(b.values.sumOf { it * it })
        return if (normA == 0.0 || normB == 0.0) 0.0 else dot / (normA * normB)
    }

    private fun tokenize(text: String): List<String> =
        WORD_RE.findAll(text.lowercase())
            .map { it.value }
            .filter { it !in STOP_WORDS }
            .toList()
}
