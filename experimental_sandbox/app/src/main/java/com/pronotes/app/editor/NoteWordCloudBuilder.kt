package com.pronotes.app.editor

/**
 * Builds word-cloud data from note text — returns ranked words with relative size weights
 * suitable for direct use in a Jetpack Compose word-cloud Canvas draw call.
 *
 * Words are TF-normalised and stop-word-filtered. The highest-frequency word gets weight 1.0;
 * all others are proportionally scaled down.
 */
object NoteWordCloudBuilder {

    data class WordEntry(
        val word: String,
        val weight: Float,  // 0.0–1.0, higher = bigger
        val count: Int,
    )

    private val STOP = setOf(
        "a","an","and","are","as","at","be","been","but","by","can","do",
        "for","from","has","have","he","her","his","how","i","if","in",
        "is","it","its","me","my","not","of","on","or","our","she","so",
        "than","that","the","their","them","then","there","they","this",
        "to","up","us","was","we","were","what","when","which","who",
        "will","with","you","your","also","just","no","one","two",
    )

    private val WORD_RE = Regex("""[a-zA-Z]{3,}""")

    fun build(text: String, maxWords: Int = 50): List<WordEntry> {
        if (text.isBlank()) return emptyList()
        val freq = mutableMapOf<String, Int>()
        WORD_RE.findAll(text.lowercase()).forEach { m ->
            val w = m.value
            if (w !in STOP) freq[w] = (freq[w] ?: 0) + 1
        }
        val maxFreq = freq.values.maxOrNull() ?: return emptyList()
        return freq.entries
            .sortedByDescending { it.value }
            .take(maxWords)
            .map { (word, count) ->
                WordEntry(
                    word = word,
                    weight = count.toFloat() / maxFreq.toFloat(),
                    count = count,
                )
            }
    }
}
