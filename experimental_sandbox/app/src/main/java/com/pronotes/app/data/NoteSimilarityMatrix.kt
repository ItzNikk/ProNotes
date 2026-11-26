package com.pronotes.app.data

import kotlin.math.sqrt

/**
 * Computes a symmetric pairwise cosine-similarity matrix for a note corpus.
 *
 * Uses term-frequency vectors (no IDF weighting) for speed. Values are in [0, 1].
 *
 * Usage:
 *   val matrix = NoteSimilarityMatrix.compute(noteTexts)
 *   val score = matrix[i][j]  // similarity between note i and note j
 */
object NoteSimilarityMatrix {

    private val WORD_RE = Regex("""[a-zA-Z]{3,}""")
    private val STOP = setOf(
        "a","an","and","are","as","at","be","been","but","by","can","do",
        "for","from","has","have","he","her","his","how","i","if","in",
        "is","it","its","me","my","not","of","on","or","our","she","so",
        "than","that","the","their","them","then","there","they","this",
        "to","up","us","was","we","were","what","when","which","who",
        "will","with","you","your",
    )

    fun compute(notes: List<String>): Array<FloatArray> {
        val n = notes.size
        val vectors = notes.map { tfVector(it) }
        val result = Array(n) { FloatArray(n) }
        for (i in 0 until n) {
            result[i][i] = 1f
            for (j in i + 1 until n) {
                val sim = cosine(vectors[i], vectors[j])
                result[i][j] = sim
                result[j][i] = sim
            }
        }
        return result
    }

    /** Returns the top-k most similar note indices for a given note index (excluding itself). */
    fun topSimilar(matrix: Array<FloatArray>, noteIndex: Int, topK: Int = 3): List<Pair<Int, Float>> =
        matrix[noteIndex]
            .mapIndexed { idx, score -> idx to score }
            .filter { (idx, _) -> idx != noteIndex }
            .sortedByDescending { (_, score) -> score }
            .take(topK)

    private fun tfVector(text: String): Map<String, Float> {
        val freq = mutableMapOf<String, Int>()
        WORD_RE.findAll(text.lowercase()).forEach { m ->
            val w = m.value
            if (w !in STOP) freq[w] = (freq[w] ?: 0) + 1
        }
        val maxF = freq.values.maxOrNull()?.toFloat() ?: 1f
        return freq.mapValues { (_, v) -> v / maxF }
    }

    private fun cosine(a: Map<String, Float>, b: Map<String, Float>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val dot = a.entries.sumOf { (k, v) -> v.toDouble() * (b[k] ?: 0f).toDouble() }
        val normA = sqrt(a.values.sumOf { it.toDouble() * it.toDouble() })
        val normB = sqrt(b.values.sumOf { it.toDouble() * it.toDouble() })
        return if (normA == 0.0 || normB == 0.0) 0f else (dot / (normA * normB)).toFloat()
    }
}
