package com.pronotes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteLinkerTest {

    private val notes = listOf(
        "Photosynthesis is the process by which plants convert sunlight into glucose. Chlorophyll absorbs light energy.",
        "Cellular Respiration is the reverse of Photosynthesis. Glucose is broken down to release ATP and energy.",
        "The Krebs Cycle is a key step in Cellular Respiration. It occurs in the mitochondria.",
        "Genetics studies how DNA transmits traits. DNA replication ensures accurate cell division.",
    )

    @Test
    fun `finds cross-links between related notes`() {
        val links = NoteLinker.findLinks(notes)
        assertTrue("Should find at least one link", links.isNotEmpty())
    }

    @Test
    fun `notes sharing photosynthesis are linked`() {
        val links = NoteLinker.findLinks(notes)
        val photosynthesisLink = links.find { link ->
            link.sharedEntities.any { it.contains("Photosynthesis", ignoreCase = true) }
        }
        assertTrue("Notes 0 and 1 share Photosynthesis", photosynthesisLink != null)
    }

    @Test
    fun `cellular respiration links notes 1 and 2`() {
        val links = NoteLinker.findLinks(notes)
        val respirationLink = links.find { link ->
            (link.noteAIndex == 1 && link.noteBIndex == 2) ||
            (link.noteAIndex == 2 && link.noteBIndex == 1)
        }
        assertTrue("Notes 1 and 2 should share Cellular Respiration", respirationLink != null)
    }

    @Test
    fun `unrelated notes produce no link`() {
        val unrelated = listOf(
            "Impressionist painting emerged in France during the 19th century.",
            "Quantum mechanics describes the behavior of particles at subatomic scales.",
        )
        val links = NoteLinker.findLinks(unrelated)
        assertTrue("Unrelated notes should have no links", links.isEmpty())
    }

    @Test
    fun `suggest links returns correct target note links`() {
        val suggestions = NoteLinker.suggestLinks(notes, targetIndex = 1, maxSuggestions = 3)
        assertTrue(suggestions.all { it.noteAIndex == 1 || it.noteBIndex == 1 })
    }
}
