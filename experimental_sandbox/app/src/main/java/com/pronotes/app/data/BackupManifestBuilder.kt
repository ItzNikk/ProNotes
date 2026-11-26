package com.pronotes.app.data

/**
 * Notebook backup manifest generator.
 *
 * Produces a structured inventory of all notebooks and pages
 * suitable for writing to a JSON-compatible Kotlin data structure
 * before triggering a backup archive.
 */
object BackupManifestBuilder {

    data class PageEntry(
        val pageIndex: Int,
        val hasInk: Boolean,
        val hasText: Boolean,
        val hasPdf: Boolean,
        val hasStickyNotes: Boolean,
        val hasImages: Boolean,
        val estimatedSizeBytes: Long,
    )

    data class NotebookEntry(
        val id: String,
        val title: String,
        val createdAtMs: Long,
        val modifiedAtMs: Long,
        val pageCount: Int,
        val pages: List<PageEntry>,
        val tagNames: List<String> = emptyList(),
        val isFavourite: Boolean = false,
    ) {
        val totalEstimatedSizeBytes: Long get() = pages.sumOf { it.estimatedSizeBytes }
    }

    data class BackupManifest(
        val schemaVersion: Int = 1,
        val generatedAtMs: Long,
        val notebookCount: Int,
        val totalPageCount: Int,
        val totalEstimatedSizeBytes: Long,
        val notebooks: List<NotebookEntry>,
    ) {
        fun humanReadableSize(): String {
            val kb = totalEstimatedSizeBytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> "%.1f MB".format(mb)
                kb >= 1.0 -> "%.1f KB".format(kb)
                else -> "$totalEstimatedSizeBytes B"
            }
        }
    }

    fun build(
        notebooks: List<NotebookEntry>,
        nowMs: Long = System.currentTimeMillis(),
    ): BackupManifest {
        return BackupManifest(
            generatedAtMs = nowMs,
            notebookCount = notebooks.size,
            totalPageCount = notebooks.sumOf { it.pageCount },
            totalEstimatedSizeBytes = notebooks.sumOf { it.totalEstimatedSizeBytes },
            notebooks = notebooks,
        )
    }
}
