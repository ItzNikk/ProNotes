package com.pronotes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManifestBuilderTest {

    private fun makeNotebook(id: String, pageSizes: List<Long>) = BackupManifestBuilder.NotebookEntry(
        id = id,
        title = "Notebook $id",
        createdAtMs = 1000L,
        modifiedAtMs = 5000L,
        pageCount = pageSizes.size,
        pages = pageSizes.mapIndexed { i, size ->
            BackupManifestBuilder.PageEntry(
                pageIndex = i,
                hasInk = true,
                hasText = false,
                hasPdf = false,
                hasStickyNotes = false,
                hasImages = false,
                estimatedSizeBytes = size,
            )
        },
        tagNames = listOf("science"),
        isFavourite = id == "nb1",
    )

    @Test
    fun `builds manifest with correct aggregates`() {
        val notebooks = listOf(
            makeNotebook("nb1", listOf(1024L, 2048L)),
            makeNotebook("nb2", listOf(512L)),
        )
        val manifest = BackupManifestBuilder.build(notebooks, nowMs = 100_000L)
        assertEquals(2, manifest.notebookCount)
        assertEquals(3, manifest.totalPageCount)
        assertEquals(3584L, manifest.totalEstimatedSizeBytes)
        assertEquals(100_000L, manifest.generatedAtMs)
    }

    @Test
    fun `human readable size formats correctly`() {
        val nb = makeNotebook("nb", listOf(2 * 1024 * 1024L)) // 2 MB
        val manifest = BackupManifestBuilder.build(listOf(nb))
        assertTrue(manifest.humanReadableSize().contains("MB"))
    }

    @Test
    fun `empty notebook list produces zero manifest`() {
        val manifest = BackupManifestBuilder.build(emptyList(), nowMs = 0L)
        assertEquals(0, manifest.notebookCount)
        assertEquals(0, manifest.totalPageCount)
        assertEquals(0L, manifest.totalEstimatedSizeBytes)
    }
}
