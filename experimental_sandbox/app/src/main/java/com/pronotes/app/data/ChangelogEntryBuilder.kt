package com.pronotes.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a structured human-readable changelog entry from feature additions.
 * Output is compatible with standard Keep-a-Changelog format (https://keepachangelog.com/).
 */
object ChangelogEntryBuilder {

    enum class ChangeType { ADDED, CHANGED, FIXED, REMOVED, DEPRECATED, SECURITY }

    data class ChangeItem(
        val type: ChangeType,
        val description: String,
        val module: String = "",
    )

    data class ChangelogEntry(
        val version: String,
        val dateMs: Long,
        val items: List<ChangeItem>,
    )

    fun buildMarkdown(entry: ChangelogEntry): String {
        val sb = StringBuilder()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(entry.dateMs))
        sb.append("## [${entry.version}] – $dateStr\n\n")

        val grouped = entry.items.groupBy { it.type }
        val order = listOf(
            ChangeType.ADDED, ChangeType.CHANGED, ChangeType.FIXED,
            ChangeType.DEPRECATED, ChangeType.REMOVED, ChangeType.SECURITY,
        )

        for (type in order) {
            val group = grouped[type] ?: continue
            sb.append("### ${type.name.lowercase().replaceFirstChar { it.uppercaseChar() }}\n")
            group.forEach { item ->
                val prefix = if (item.module.isNotBlank()) "**${item.module}**: " else ""
                sb.append("- $prefix${item.description}\n")
            }
            sb.append('\n')
        }

        return sb.toString().trimEnd() + "\n"
    }
}
