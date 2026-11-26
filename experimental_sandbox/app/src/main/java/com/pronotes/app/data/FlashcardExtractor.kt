package com.pronotes.app.data

/**
 * Automatically extracts flashcard pairs (front/back) from structured note text.
 *
 * Supported patterns:
 * - `Q: ...` followed by `A: ...` (case-insensitive)
 * - `Front :: Back`
 * - `Front -> Back`
 */
object FlashcardExtractor {

    data class ExtractedCard(
        val front: String,
        val back: String,
    )

    fun extract(text: String): List<ExtractedCard> {
        if (text.isBlank()) return emptyList()
        val cards = mutableListOf<ExtractedCard>()
        val seen = HashSet<Pair<String, String>>()
        val lines = text.lines()

        var pendingQuestion: String? = null
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                pendingQuestion = null
                continue
            }

            if (line.startsWith("Q:", ignoreCase = true)) {
                pendingQuestion = line.substring(2).trim()
                continue
            }

            if (line.startsWith("A:", ignoreCase = true) && !pendingQuestion.isNullOrBlank()) {
                val answer = line.substring(2).trim()
                if (answer.isNotEmpty()) {
                    addUnique(cards, seen, pendingQuestion, answer)
                }
                pendingQuestion = null
                continue
            }

            pendingQuestion = null

            val delimiter = when {
                "::" in line -> "::"
                "->" in line -> "->"
                else -> null
            }
            if (delimiter != null) {
                val parts = line.split(delimiter, limit = 2)
                val front = parts[0].trim()
                val back = parts[1].trim()
                if (front.isNotEmpty() && back.isNotEmpty()) {
                    addUnique(cards, seen, front, back)
                }
            }
        }

        return cards
    }

    private fun addUnique(
        cards: MutableList<ExtractedCard>,
        seen: MutableSet<Pair<String, String>>,
        front: String,
        back: String,
    ) {
        val key = front to back
        if (seen.add(key)) {
            cards.add(ExtractedCard(front, back))
        }
    }
}
