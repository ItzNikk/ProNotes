package com.pronotes.app.data

import kotlin.math.round

/**
 * Computes Flesch-Kincaid readability metrics for note text.
 *
 * Scores:
 * - Flesch Reading Ease  : 0–100 (higher = easier to read)
 * - Flesch-Kincaid Grade : grade level (lower = simpler)
 *
 * Also reports vocabulary richness (Type-Token Ratio) and
 * average sentence length.
 */
object ReadabilityAnalyzer {

    data class Report(
        val wordCount: Int,
        val sentenceCount: Int,
        val syllableCount: Int,
        val avgWordsPerSentence: Double,
        val avgSyllablesPerWord: Double,
        val fleschReadingEase: Double,
        val fleschKincaidGrade: Double,
        val typeTokenRatio: Double,
        val readingLevel: String,
    )

    private val SENTENCE_ENDS = Regex("""[.!?]+""")
    private val WORD_SPLIT = Regex("""[^a-zA-Z'-]+""")

    fun analyze(text: String): Report {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return Report(0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, "N/A")
        }

        val sentences = SENTENCE_ENDS.split(trimmed).filter { it.isNotBlank() }
        val sentenceCount = sentences.size.coerceAtLeast(1)

        val words = WORD_SPLIT.split(trimmed).filter { it.isNotBlank() }
        val wordCount = words.size

        val syllables = words.sumOf { countSyllables(it) }
        val avgWordsPerSent = wordCount.toDouble() / sentenceCount
        val avgSyllablesPerWord = if (wordCount > 0) syllables.toDouble() / wordCount else 0.0

        // Flesch Reading Ease
        val ease = 206.835 - 1.015 * avgWordsPerSent - 84.6 * avgSyllablesPerWord
        val clampedEase = ease.coerceIn(0.0, 100.0)

        // Flesch-Kincaid Grade Level
        val grade = 0.39 * avgWordsPerSent + 11.8 * avgSyllablesPerWord - 15.59

        // Type-Token Ratio (lexical richness): unique words / total words
        val uniqueWords = words.map { it.lowercase() }.toSet().size
        val ttr = if (wordCount > 0) uniqueWords.toDouble() / wordCount else 0.0

        val level = when {
            clampedEase >= 90 -> "Very Easy (Grade 5)"
            clampedEase >= 80 -> "Easy (Grade 6)"
            clampedEase >= 70 -> "Fairly Easy (Grade 7)"
            clampedEase >= 60 -> "Standard (Grade 8-9)"
            clampedEase >= 50 -> "Fairly Difficult (Grade 10-12)"
            clampedEase >= 30 -> "Difficult (College)"
            else -> "Very Difficult (Professional)"
        }

        return Report(
            wordCount = wordCount,
            sentenceCount = sentenceCount,
            syllableCount = syllables,
            avgWordsPerSentence = round(avgWordsPerSent * 100.0) / 100.0,
            avgSyllablesPerWord = round(avgSyllablesPerWord * 100.0) / 100.0,
            fleschReadingEase = round(clampedEase * 100.0) / 100.0,
            fleschKincaidGrade = round(grade * 100.0) / 100.0,
            typeTokenRatio = round(ttr * 1000.0) / 1000.0,
            readingLevel = level,
        )
    }

    /** Heuristic syllable counter based on English vowel-group rules. */
    private fun countSyllables(word: String): Int {
        val w = word.lowercase().trimEnd('e', 's')
        if (w.isEmpty()) return 1
        val vowels = Regex("[aeiouy]+")
        val count = vowels.findAll(w).count()
        return count.coerceAtLeast(1)
    }
}
