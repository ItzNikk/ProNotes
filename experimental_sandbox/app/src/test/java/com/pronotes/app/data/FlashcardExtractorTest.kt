package com.pronotes.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FlashcardExtractorTest {

    @Test
    fun `extracts Q-A, double-colon, and arrow flashcards`() {
        val input = """
            Q: What is the speed of light?
            A: ~3.0 x 10^8 m/s

            Mitochondria :: Powerhouse of the cell
            Osmosis -> Diffusion of water across a semipermeable membrane
            Mitochondria :: Powerhouse of the cell
        """.trimIndent()

        val cards = FlashcardExtractor.extract(input)
        assertEquals(3, cards.size)
        assertEquals("What is the speed of light?", cards[0].front)
        assertEquals("~3.0 x 10^8 m/s", cards[0].back)
        assertEquals("Mitochondria", cards[1].front)
        assertEquals("Osmosis", cards[2].front)
    }
}
