package com.pronotes.app.data

/**
 * Smart spaced-repetition scheduler that assigns the next review interval for flashcards.
 *
 * Uses a simplified SM-2 algorithm:
 * - `Again` (quality = 0): reset to 1-day interval, decrement easiness
 * - `Hard`  (quality = 1): repeat with 60% of previous interval
 * - `Good`  (quality = 2): normal SM-2 advance
 * - `Easy`  (quality = 3): SM-2 advance + bonus day
 *
 * Returns the new state to persist (immutable data model).
 */
object SpacedRepetitionScheduler {

    data class CardState(
        val repetitions: Int = 0,
        val intervalDays: Int = 1,
        val easinessFactor: Double = 2.5,
    )

    enum class Rating { AGAIN, HARD, GOOD, EASY }

    /**
     * @param state current persisted state of the card
     * @param rating user's self-assessment after reviewing the card
     * @return new state to persist; next review is [intervalDays] from now
     */
    fun advance(state: CardState, rating: Rating): CardState {
        val quality = rating.ordinal  // 0, 1, 2, 3

        if (rating == Rating.AGAIN) {
            // Lapse: restart learning, lower ease
            val newEF = (state.easinessFactor - 0.2).coerceAtLeast(1.3)
            return CardState(repetitions = 0, intervalDays = 1, easinessFactor = newEF)
        }

        val newEF = (state.easinessFactor + (0.1 - (3 - quality) * (0.08 + (3 - quality) * 0.02)))
            .coerceIn(1.3, 4.0)

        val newInterval = when {
            rating == Rating.HARD -> (state.intervalDays * 0.6).toInt().coerceAtLeast(1)
            state.repetitions == 0 -> 1
            state.repetitions == 1 -> 6
            else -> (state.intervalDays * newEF).toInt().coerceAtLeast(1)
        }

        val bonus = if (rating == Rating.EASY) 1 else 0

        return CardState(
            repetitions = state.repetitions + 1,
            intervalDays = newInterval + bonus,
            easinessFactor = newEF,
        )
    }

    /**
     * Estimates how many reviews a card will need to reach a given interval threshold
     * starting from [state], always assuming [Rating.GOOD].
     */
    fun estimateReviewsToMastery(
        state: CardState,
        masteryIntervalDays: Int = 21,
    ): Int {
        var current = state
        var count = 0
        while (current.intervalDays < masteryIntervalDays && count < 100) {
            current = advance(current, Rating.GOOD)
            count++
        }
        return count
    }
}
