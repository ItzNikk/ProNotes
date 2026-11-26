package com.pronotes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoSummaryEngineTest {

    @Test
    fun `returns empty for blank text`() {
        val s = AutoSummaryEngine.summarize("")
        assertTrue(s.sentences.isEmpty())
    }

    @Test
    fun `returns all sentences when fewer than target`() {
        val text = "Cats are pets. Dogs are also pets."
        val s = AutoSummaryEngine.summarize(text, targetSentences = 5)
        assertEquals(2, s.sentences.size)
        assertEquals(1.0, s.compressionRatio, 1e-9)
    }

    @Test
    fun `extracts correct number of summary sentences`() {
        val text = """
            The Internet is a global system of interconnected computer networks.
            It carries a vast range of information resources and services.
            The World Wide Web is one of the most popular services on the Internet.
            Email and messaging are also widely used Internet services.
            The Internet has fundamentally changed communications worldwide.
            Billions of people rely on the Internet every day.
        """.trimIndent()
        val s = AutoSummaryEngine.summarize(text, targetSentences = 3)
        assertEquals(3, s.sentences.size)
        assertTrue(s.compressionRatio < 1.0)
    }
}
