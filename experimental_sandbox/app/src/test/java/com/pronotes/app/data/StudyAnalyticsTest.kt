package com.pronotes.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StudyAnalyticsTest {

    @Test
    fun `empty deck returns zero summary`() {
        val summary = StudyAnalytics.summarize(emptyList(), nowMillis = 1000L)
        assertEquals(0, summary.totalCards)
        assertEquals(0, summary.masteryPercent)
    }

    @Test
    fun `summarizes new learning mastered and due cards`() {
        val now = 10_000L
        val cards = listOf(
            StudyAnalytics.CardSnapshot(repetitions = 0, intervalDays = 0, dueAtMillis = 5_000L),
            StudyAnalytics.CardSnapshot(repetitions = 2, intervalDays = 3, dueAtMillis = 9_000L),
            StudyAnalytics.CardSnapshot(repetitions = 5, intervalDays = 14, dueAtMillis = 20_000L),
            StudyAnalytics.CardSnapshot(repetitions = 5, intervalDays = 21, dueAtMillis = 30_000L),
        )
        val summary = StudyAnalytics.summarize(cards, nowMillis = now)
        assertEquals(4, summary.totalCards)
        assertEquals(1, summary.newCards)
        assertEquals(1, summary.learningCards)
        assertEquals(2, summary.masteredCards)
        assertEquals(2, summary.dueNow)
        assertEquals(50, summary.masteryPercent)
        assertEquals(3.0, summary.averageRepetitions, 1e-6)
    }
}
