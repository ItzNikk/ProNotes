package com.pronotes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedRepetitionSchedulerTest {

    @Test
    fun `again rating resets card to interval 1`() {
        val state = SpacedRepetitionScheduler.CardState(repetitions = 5, intervalDays = 30, easinessFactor = 2.5)
        val next = SpacedRepetitionScheduler.advance(state, SpacedRepetitionScheduler.Rating.AGAIN)
        assertEquals(0, next.repetitions)
        assertEquals(1, next.intervalDays)
        assertTrue(next.easinessFactor < 2.5)
    }

    @Test
    fun `good rating progresses repetitions`() {
        var state = SpacedRepetitionScheduler.CardState()
        state = SpacedRepetitionScheduler.advance(state, SpacedRepetitionScheduler.Rating.GOOD)
        assertEquals(1, state.repetitions)
        assertEquals(1, state.intervalDays)
        state = SpacedRepetitionScheduler.advance(state, SpacedRepetitionScheduler.Rating.GOOD)
        assertEquals(2, state.repetitions)
        assertEquals(6, state.intervalDays)
        state = SpacedRepetitionScheduler.advance(state, SpacedRepetitionScheduler.Rating.GOOD)
        assertEquals(3, state.repetitions)
        assertTrue(state.intervalDays > 6)
    }

    @Test
    fun `easy rating gives bonus day`() {
        val state = SpacedRepetitionScheduler.CardState(repetitions = 2, intervalDays = 6, easinessFactor = 2.5)
        val next = SpacedRepetitionScheduler.advance(state, SpacedRepetitionScheduler.Rating.EASY)
        val good = SpacedRepetitionScheduler.advance(state, SpacedRepetitionScheduler.Rating.GOOD)
        assertTrue("Easy should give >= Good interval", next.intervalDays >= good.intervalDays)
    }

    @Test
    fun `hard rating reduces interval`() {
        val state = SpacedRepetitionScheduler.CardState(repetitions = 3, intervalDays = 20, easinessFactor = 2.5)
        val next = SpacedRepetitionScheduler.advance(state, SpacedRepetitionScheduler.Rating.HARD)
        assertTrue("Hard should reduce interval below 20", next.intervalDays < 20)
    }

    @Test
    fun `estimate reviews to mastery returns reasonable count`() {
        val state = SpacedRepetitionScheduler.CardState()
        val reviews = SpacedRepetitionScheduler.estimateReviewsToMastery(state, masteryIntervalDays = 21)
        assertTrue("Should take 4-10 reviews to mastery", reviews in 4..10)
    }
}
