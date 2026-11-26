package com.pronotes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagSuggestionEngineTest {

    @Test
    fun `returns empty list for blank text`() {
        val tags = TagSuggestionEngine.suggest("")
        assertTrue(tags.isEmpty())
    }

    @Test
    fun `extracts dominant keywords as tags`() {
        val note = "Photosynthesis is the process by which plants use sunlight to synthesize " +
                "nutrients from carbon dioxide and water. Photosynthesis is fundamental to plant life. " +
                "Plants convert sunlight into glucose through photosynthesis."
        val tags = TagSuggestionEngine.suggest(note, maxTags = 5)
        assertTrue("Should return at most 5 tags", tags.size <= 5)
        val tagNames = tags.map { it.tag }
        assertTrue("photosynthesis should be a top tag", "photosynthesis" in tagNames)
    }

    @Test
    fun `idf boosts rare words across corpus`() {
        val note = "Mitochondria are membrane-bound organelles that generate ATP."
        val corpus = listOf(
            "The cell membrane controls what enters and exits the cell.",
            "Ribosomes synthesize proteins from amino acids.",
        )
        val tags = TagSuggestionEngine.suggest(note, corpus = corpus, maxTags = 5)
        // mitochondria appears only in note, not corpus, so should rank highly
        val names = tags.map { it.tag }
        assertTrue("Should suggest mitochondria", "mitochondria" in names)
    }
}
