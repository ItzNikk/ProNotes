package com.pronotes.app.editor

import org.junit.Assert.assertTrue
import org.junit.Test

class NoteOutlineExporterTest {

    @Test
    fun `exports multi-page note with text stickies and concept map`() {
        val md = NoteOutlineExporter.toMarkdown(
            title = "Physics 101",
            pages = listOf(
                NoteOutlineExporter.ExportPage(
                    pageNumber = 1,
                    textBlocks = listOf("Newton's Second Law: F = ma"),
                    stickyNotes = listOf("Remember units:\n1 N = 1 kg·m/s²"),
                    mindMapNodes = listOf("Mechanics", "Kinematics", "Dynamics"),
                ),
                NoteOutlineExporter.ExportPage(
                    pageNumber = 2,
                    textBlocks = listOf("Conservation of Momentum"),
                ),
            ),
        )
        assertTrue(md.contains("# Physics 101"))
        assertTrue(md.contains("## Page 1"))
        assertTrue(md.contains("Newton's Second Law: F = ma"))
        assertTrue(md.contains("### Sticky Notes"))
        assertTrue(md.contains("> Remember units:\n> 1 N = 1 kg·m/s²"))
        assertTrue(md.contains("### Concept Map"))
        assertTrue(md.contains("- Mechanics"))
        assertTrue(md.contains("## Page 2"))
    }
}
