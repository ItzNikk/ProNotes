package com.pronotes.app.data

import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogEntryBuilderTest {

    @Test
    fun `builds formatted changelog with all types`() {
        val entry = ChangelogEntryBuilder.ChangelogEntry(
            version = "2.1.0",
            dateMs = 1_750_000_000_000L,
            items = listOf(
                ChangelogEntryBuilder.ChangeItem(ChangelogEntryBuilder.ChangeType.ADDED, "Inline math evaluator with trailing-equals chip", "MathEvaluator"),
                ChangelogEntryBuilder.ChangeItem(ChangelogEntryBuilder.ChangeType.ADDED, "TF-IDF tag suggestions for every note", "TagSuggestionEngine"),
                ChangelogEntryBuilder.ChangeItem(ChangelogEntryBuilder.ChangeType.FIXED, "Similarity score for identical text now returns 100", "NoteRevisionDiff"),
                ChangelogEntryBuilder.ChangeItem(ChangelogEntryBuilder.ChangeType.CHANGED, "Seyès template threshold lowered to 15 avg words/sentence", "PageTemplateSuggester"),
            )
        )
        val md = ChangelogEntryBuilder.buildMarkdown(entry)
        assertTrue(md.contains("## [2.1.0]"))
        assertTrue(md.contains("### Added"))
        assertTrue(md.contains("### Fixed"))
        assertTrue(md.contains("### Changed"))
        assertTrue(md.contains("**MathEvaluator**"))
        assertTrue(!md.contains("### Removed"))
    }
}
