package com.pronotes.app.library

internal enum class PdfTask(val label: String) {
    MERGE("Merge PDFs"), IMAGES("Images to PDF"), EXTRACT("Extract page range"), ROTATE("Rotate clockwise"),
    REVERSE("Reverse page order"), ODD("Keep odd pages"), EVEN("Keep even pages"), TEXT("Convert to text")
}

internal fun PdfTask.pages(count: Int, range: String = ""): IntProgression {
    val selected = when (this) {
        PdfTask.REVERSE -> count - 1 downTo 0
        PdfTask.ODD -> 0 until count step 2
        PdfTask.EVEN -> 1 until count step 2
        PdfTask.EXTRACT -> {
            val parts = range.trim().replace('–', '-').split('-')
            val first = parts.first().trim().toIntOrNull() ?: 0
            val last = parts.last().trim().toIntOrNull() ?: 0
            require(parts.size <= 2 && first in 1..count && last in first..count) {
                "Enter a page or range within pages 1–$count."
            }
            first - 1 until last
        }
        else -> error("This operation does not select individual pages.")
    }
    require(!selected.isEmpty()) { "No matching pages." }
    return selected
}
