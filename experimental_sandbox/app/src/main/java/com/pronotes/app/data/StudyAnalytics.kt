package com.pronotes.app.data

/**
 * Pure-Kotlin study analytics engine summarizing flashcard mastery, due workload,
 * and spaced-repetition readiness.
 */
object StudyAnalytics {

    data class MasterySummary(
        val totalCards: Int,
        val newCards: Int,
        val learningCards: Int,
        val masteredCards: Int,
        val dueNow: Int,
        val averageRepetitions: Double,
        val masteryPercent: Int,
    )

    data class CardSnapshot(
        val repetitions: Int,
        val intervalDays: Int,
        val dueAtMillis: Long,
        val starred: Boolean = false,
    )

    fun summarize(cards: List<CardSnapshot>, nowMillis: Long): MasterySummary {
        if (cards.isEmpty()) {
            return MasterySummary(
                totalCards = 0,
                newCards = 0,
                learningCards = 0,
                masteredCards = 0,
                dueNow = 0,
                averageRepetitions = 0.0,
                masteryPercent = 0,
            )
        }
        var newCount = 0
        var learningCount = 0
        var masteredCount = 0
        var dueCount = 0
        var totalReps = 0

        for (card in cards) {
            totalReps += card.repetitions
            if (card.dueAtMillis <= nowMillis) dueCount++
            when {
                card.repetitions == 0 -> newCount++
                card.intervalDays >= 7 -> masteredCount++
                else -> learningCount++
            }
        }

        val masteryPct = ((masteredCount * 100.0) / cards.size).toInt().coerceIn(0, 100)
        return MasterySummary(
            totalCards = cards.size,
            newCards = newCount,
            learningCards = learningCount,
            masteredCards = masteredCount,
            dueNow = dueCount,
            averageRepetitions = totalReps.toDouble() / cards.size,
            masteryPercent = masteryPct,
        )
    }
}
