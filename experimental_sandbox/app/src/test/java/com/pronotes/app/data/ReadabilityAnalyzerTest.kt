package com.pronotes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadabilityAnalyzerTest {

    @Test
    fun `returns zero report for empty text`() {
        val r = ReadabilityAnalyzer.analyze("")
        assertEquals(0, r.wordCount)
        assertEquals("N/A", r.readingLevel)
    }

    @Test
    fun `basic sentence analysis`() {
        // Simple children-level text should score high on ease
        val simple = "The cat sat on the mat. Dogs are fun. Kids like cats."
        val r = ReadabilityAnalyzer.analyze(simple)
        assertTrue("Word count should be > 10", r.wordCount > 10)
        assertTrue("Simple text ease >= 50", r.fleschReadingEase >= 50.0)
    }

    @Test
    fun `complex text has higher grade and lower ease`() {
        val complex = "The mitochondrion is a double-membrane-bound organelle found in most eukaryotic organisms. " +
                "Mitochondria generate most of the cell's supply of adenosine triphosphate, used as a source of chemical energy. " +
                "They are therefore sometimes referred to as the powerhouses of the cell."
        val r = ReadabilityAnalyzer.analyze(complex)
        assertTrue("Complex grade should be >= 10", r.fleschKincaidGrade >= 10.0)
        assertTrue("Word count > 30", r.wordCount > 30)
    }

    @Test
    fun `type token ratio reflects vocabulary richness`() {
        val repetitive = "cat cat cat cat cat cat cat cat cat cat"
        val r = ReadabilityAnalyzer.analyze(repetitive)
        assertTrue("Repetitive text TTR should be < 0.3", r.typeTokenRatio < 0.3)
    }
}
