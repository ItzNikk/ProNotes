package com.pronotes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class StreakTrackerTest {

    private val DAY = TimeUnit.DAYS.toMillis(1)
    private val now = 1_000 * DAY // some fixed reference day

    @Test
    fun `empty sessions returns zero streak`() {
        val r = StreakTracker.report(emptyList(), nowMs = now)
        assertEquals(0, r.currentStreak)
        assertEquals(0, r.longestStreak)
        assertFalse(r.studiedToday)
    }

    @Test
    fun `consecutive days compute correct streak`() {
        val sessions = listOf(
            (now - 2 * DAY) + 3600_000,  // 2 days ago
            (now - DAY) + 7200_000,       // 1 day ago
            now + 3600_000,               // today
        )
        val r = StreakTracker.report(sessions, nowMs = now)
        assertEquals(3, r.currentStreak)
        assertEquals(3, r.longestStreak)
        assertTrue(r.studiedToday)
        assertEquals(3, r.totalStudyDays)
    }

    @Test
    fun `streak breaks on gap`() {
        val sessions = listOf(
            (now - 10 * DAY) + 1000,  // 10 days ago
            (now - 9 * DAY) + 1000,   // 9 days ago
            // gap - no sessions for 8 days
            now + 1000,               // today
        )
        val r = StreakTracker.report(sessions, nowMs = now)
        assertEquals(1, r.currentStreak)   // only today
        assertEquals(2, r.longestStreak)   // 2-day run earlier
    }

    @Test
    fun `consistency7 and consistency30 computed correctly`() {
        // Study every day for the last 14 days
        val sessions = (0L..13L).map { now - it * DAY + 1000 }
        val r = StreakTracker.report(sessions, nowMs = now)
        assertEquals(1.0, r.consistency7Days, 1e-3)
        assertTrue(r.consistency30Days in 0.4..0.5) // 14/30 ≈ 0.467
    }
}
