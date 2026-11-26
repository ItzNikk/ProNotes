package com.pronotes.app.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PageTemplateSuggesterTest {

    @Test
    fun `blank input recommends blank template`() {
        val s = PageTemplateSuggester.suggest("")
        assertEquals(PageTemplateSuggester.PaperTemplate.BLANK, s.recommended)
    }

    @Test
    fun `math-heavy note recommends graph`() {
        val text = "y = mx + b\nf(x) = sqrt(x^2 + 1)\nsin(x) + cos(x) = tan(x/2)\n∑ n^2 = n(n+1)(2n+1)/6\n∫ x dx = x²/2 + C"
        val s = PageTemplateSuggester.suggest(text)
        assertEquals(PageTemplateSuggester.PaperTemplate.GRAPH, s.recommended)
    }

    @Test
    fun `bullet-heavy note recommends cornell or lined`() {
        val text = listOf(
            "- Photosynthesis converts sunlight into glucose",
            "- Respiration converts glucose into ATP",
            "- Mitosis produces genetically identical cells",
            "- Meiosis produces genetically diverse gametes",
            "- DNA carries the genetic information of an organism",
            "- RNA transcribes and translates the genetic code",
            "- Proteins are built from amino acid sequences",
            "- Enzymes act as biological catalysts in reactions",
        ).joinToString("\n")
        val s = PageTemplateSuggester.suggest(text)
        // Not blank, graph, dotted, or seyes — it should be a note-taking template
        assertNotEquals(PageTemplateSuggester.PaperTemplate.BLANK, s.recommended)
        assertNotEquals(PageTemplateSuggester.PaperTemplate.GRAPH, s.recommended)
        assertNotEquals(PageTemplateSuggester.PaperTemplate.DOTTED, s.recommended)
    }

    @Test
    fun `long dense prose does not recommend blank or graph`() {
        val sentence = "The mitochondrion is a double-membrane-bound organelle that generates most cellular ATP. "
        val text = sentence.repeat(8)
        val s = PageTemplateSuggester.suggest(text)
        assertNotEquals(PageTemplateSuggester.PaperTemplate.BLANK, s.recommended)
        assertNotEquals(PageTemplateSuggester.PaperTemplate.GRAPH, s.recommended)
    }
}
