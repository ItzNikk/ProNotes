package com.pronotes.app.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteWordCloudBuilderTest {

    @Test
    fun `returns empty for blank input`() {
        assertTrue(NoteWordCloudBuilder.build("").isEmpty())
    }

    @Test
    fun `most frequent word gets weight 1`() {
        val text = "cat cat cat dog bird cat dog bird"
        val cloud = NoteWordCloudBuilder.build(text)
        assertEquals("cat", cloud.first().word)
        assertEquals(1.0f, cloud.first().weight, 0.001f)
    }

    @Test
    fun `stop words are excluded`() {
        val text = "the quick brown fox jumps over the lazy dog the the the"
        val cloud = NoteWordCloudBuilder.build(text)
        assertTrue(cloud.none { it.word == "the" })
        assertTrue(cloud.any { it.word == "fox" || it.word == "dog" || it.word == "quick" })
    }

    @Test
    fun `respects maxWords limit`() {
        val bigText = (1..200).joinToString(" ") { "word$it" }
        val cloud = NoteWordCloudBuilder.build(bigText, maxWords = 30)
        assertTrue(cloud.size <= 30)
    }
}
