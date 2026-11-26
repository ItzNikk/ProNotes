package com.pronotes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteHealthCheckerTest {

    @Test
    fun `blank note returns empty score 100`() {
        val r = NoteHealthChecker.check("")
        assertEquals(100, r.score)
        assertTrue(r.healthy)
    }

    @Test
    fun `detects duplicate word`() {
        val text = "The mitochondria is is the powerhouse of the cell. It generates ATP for energy."
        val r = NoteHealthChecker.check(text)
        assertTrue(r.issues.any { it.code == "DUPLICATE_WORD" })
        assertFalse(r.healthy)
    }

    @Test
    fun `detects unclosed parenthesis`() {
        val text = "The formula (E = mc² is fundamental to understanding mass-energy equivalence in physics."
        val r = NoteHealthChecker.check(text)
        assertTrue(r.issues.any { it.code == "UNCLOSED_PAREN" })
    }

    @Test
    fun `healthy clean note scores high`() {
        val text = "Cellular respiration is the process by which cells break down glucose to produce ATP. " +
                "It occurs in the mitochondria and involves glycolysis, the Krebs cycle, and the electron transport chain. " +
                "This process is essential for all aerobic organisms to generate energy."
        val r = NoteHealthChecker.check(text)
        assertTrue("Score should be >= 70 for clean note", r.score >= 70)
        assertTrue(r.healthy)
    }
}
