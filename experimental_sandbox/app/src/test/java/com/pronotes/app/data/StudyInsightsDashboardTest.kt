package com.pronotes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class StudyInsightsDashboardTest {

    private val DAY = TimeUnit.DAYS.toMillis(1)
    private val now = 1_000 * DAY

    private val sampleNote = """
        Cellular respiration is the process by which cells convert glucose into ATP.
        It occurs primarily in the mitochondria and involves three main stages.
        Glycolysis occurs in the cytoplasm and breaks glucose into pyruvate.
        The Krebs cycle further oxidizes pyruvate to release carbon dioxide.
        The electron transport chain produces the majority of ATP via oxidative phosphorylation.
    """.trimIndent()

    @Test
    fun `compute returns fully populated snapshot`() {
        val sessions = listOf(now - DAY + 1000, now + 1000)
        val cards = listOf(
            StudyAnalytics.CardSnapshot(repetitions = 0, intervalDays = 0, dueAtMillis = now - 1000),
            StudyAnalytics.CardSnapshot(repetitions = 5, intervalDays = 21, dueAtMillis = now + 10 * DAY),
        )
        val snapshot = StudyInsightsDashboard.compute(
            noteText = sampleNote,
            flashcardSnapshots = cards,
            sessionTimestampsMs = sessions,
            nowMs = now,
        )

        assertEquals(2, snapshot.streak.currentStreak)
        assertEquals(2, snapshot.mastery.totalCards)
        assertNotNull(snapshot.readability)
        assertTrue(snapshot.tagSuggestions.isNotEmpty())
        assertTrue(snapshot.sessionPlan.phases.isNotEmpty())
        assertTrue(snapshot.autoSummary.isNotEmpty())
    }
}
