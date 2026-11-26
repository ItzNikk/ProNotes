package com.pronotes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRankerTest {

    private val notes = listOf(
        "Photosynthesis is the process by which plants use sunlight to synthesize glucose.",
        "Mitochondria are membrane-bound organelles that generate ATP through cellular respiration.",
        "The cell membrane is a phospholipid bilayer that controls what enters and exits the cell.",
        "DNA carries genetic information and is located in the cell nucleus.",
    )

    @Test
    fun `returns empty for blank query`() {
        val ranker = SearchRanker(notes)
        assertTrue(ranker.search("").isEmpty())
    }

    @Test
    fun `finds correct top result for specific term`() {
        val ranker = SearchRanker(notes)
        val results = ranker.search("photosynthesis plants sunlight", topK = 3)
        assertTrue(results.isNotEmpty())
        assertEquals(0, results.first().docIndex) // first note should rank highest
    }

    @Test
    fun `mitochondria query finds organelle note`() {
        val ranker = SearchRanker(notes)
        val results = ranker.search("mitochondria ATP", topK = 3)
        assertTrue(results.isNotEmpty())
        assertEquals(1, results.first().docIndex)
    }

    @Test
    fun `matched terms are returned`() {
        val ranker = SearchRanker(notes)
        val results = ranker.search("cell membrane phospholipid", topK = 1)
        assertTrue(results.isNotEmpty())
        val matched = results.first().matchedTerms
        assertTrue("matched should contain 'cell' or 'membrane'", matched.any { it in listOf("cell", "membrane", "phospholipid") })
    }
}
