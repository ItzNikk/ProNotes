package com.pronotes.app.editor

/**
 * Exports structured note content (text blocks, sticky notes, and concept map nodes)
 * into clean Markdown suitable for sharing or archiving.
 */
object NoteOutlineExporter {

    data class ExportPage(
        val pageNumber: Int,
        val textBlocks: List<String> = emptyList(),
        val stickyNotes: List<String> = emptyList(),
        val mindMapNodes: List<String> = emptyList(),
    )

    fun toMarkdown(title: String, pages: List<ExportPage>): String {
        val sb = StringBuilder()
        val safeTitle = title.ifBlank { "Untitled Note" }.trim()
        sb.append("# ").append(safeTitle).append('\n')

        pages.forEachIndexed { idx, page ->
            if (idx > 0 || pages.size > 1) {
                sb.append("\n## Page ").append(page.pageNumber).append('\n')
            } else {
                sb.append('\n')
            }

            page.textBlocks
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { block ->
                    sb.append(block).append("\n\n")
                }

            val stickies = page.stickyNotes.map { it.trim() }.filter { it.isNotEmpty() }
            if (stickies.isNotEmpty()) {
                sb.append("### Sticky Notes\n")
                stickies.forEach { note ->
                    sb.append("> ").append(note.replace("\n", "\n> ")).append('\n')
                }
                sb.append('\n')
            }

            val nodes = page.mindMapNodes.map { it.trim() }.filter { it.isNotEmpty() }
            if (nodes.isNotEmpty()) {
                sb.append("### Concept Map\n")
                nodes.forEach { node ->
                    sb.append("- ").append(node).append('\n')
                }
                sb.append('\n')
            }
        }

        return sb.toString().trimEnd() + "\n"
    }
}
