package com.pronotes.app.editor

/**
 * Formats lecture notes into a structured Cornell Note layout (Cues, Notes, Summary).
 */
object CornellNoteFormatter {

    data class CornellSheet(
        val title: String,
        val cues: List<String>,
        val notes: List<String>,
        val summary: String,
    )

    fun toMarkdown(sheet: CornellSheet): String {
        val sb = StringBuilder()
        sb.append("# ").append(sheet.title.ifBlank { "Cornell Notes" }.trim()).append("\n\n")

        sb.append("## Cues & Questions\n")
        val cleanCues = sheet.cues.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanCues.isEmpty()) {
            sb.append("- _None_\n")
        } else {
            cleanCues.forEach { sb.append("- ").append(it).append('\n') }
        }

        sb.append("\n## Main Notes\n")
        val cleanNotes = sheet.notes.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanNotes.isEmpty()) {
            sb.append("_No notes recorded._\n")
        } else {
            cleanNotes.forEach { sb.append(it).append("\n\n") }
        }

        sb.append("## Summary\n")
        val cleanSummary = sheet.summary.trim()
        sb.append(if (cleanSummary.isEmpty()) "_No summary yet._" else cleanSummary).append('\n')

        return sb.toString()
    }
}
