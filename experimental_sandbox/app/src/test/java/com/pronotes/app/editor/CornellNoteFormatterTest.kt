package com.pronotes.app.editor

import org.junit.Assert.assertTrue
import org.junit.Test

class CornellNoteFormatterTest {

    @Test
    fun `formats cornell sheet into markdown sections`() {
        val md = CornellNoteFormatter.toMarkdown(
            CornellNoteFormatter.CornellSheet(
                title = "Cell Biology",
                cues = listOf("What is ATP?", "Where does glycolysis occur?"),
                notes = listOf("ATP is the primary energy currency of the cell.", "Glycolysis occurs in the cytoplasm."),
                summary = "Cells convert glucose into ATP via glycolysis and cellular respiration.",
            )
        )
        assertTrue(md.contains("# Cell Biology"))
        assertTrue(md.contains("## Cues & Questions"))
        assertTrue(md.contains("- What is ATP?"))
        assertTrue(md.contains("## Main Notes"))
        assertTrue(md.contains("## Summary"))
    }
}
