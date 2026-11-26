package com.pronotes.app.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteRevisionDiffTest {

    @Test
    fun `identical texts have zero added or removed`() {
        val text = "The quick brown fox jumps over the lazy dog."
        val d = NoteRevisionDiff.diff(text, text)
        assertEquals(0, d.addedWordCount)
        assertEquals(0, d.removedWordCount)
        assertEquals(100, d.similarityPercent)
    }

    @Test
    fun `detects added words`() {
        val before = "Photosynthesis uses sunlight."
        val after = "Photosynthesis uses sunlight and water."
        val d = NoteRevisionDiff.diff(before, after)
        assertTrue("Should have added words", d.addedWordCount > 0)
        assertTrue(d.spans.any { it.type == NoteRevisionDiff.DiffType.ADDED && it.text.trim() == "and" })
    }

    @Test
    fun `detects removed words`() {
        val before = "The cell membrane is a phospholipid bilayer."
        val after = "The cell membrane is a bilayer."
        val d = NoteRevisionDiff.diff(before, after)
        assertTrue("Should have removed words", d.removedWordCount > 0)
        assertTrue(d.spans.any { it.type == NoteRevisionDiff.DiffType.REMOVED && it.text.trim() == "phospholipid" })
    }

    @Test
    fun `empty before gives all added`() {
        val d = NoteRevisionDiff.diff("", "Hello world")
        assertEquals(0, d.removedWordCount)
        assertTrue(d.addedWordCount > 0)
    }

    @Test
    fun `empty after gives all removed`() {
        val d = NoteRevisionDiff.diff("Hello world", "")
        assertEquals(0, d.addedWordCount)
        assertTrue(d.removedWordCount > 0)
    }
}
