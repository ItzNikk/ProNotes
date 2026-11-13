package com.pronotes.app.editor

/** Order and visibility of toolbar tools, stored as "PEN,HIGHLIGHTER,-LASER,..." ("-" = hidden). */
object ToolbarOrder {
    val defaults = listOf(
        Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.SHAPE, Tool.LASSO, Tool.MINDMAP,
        Tool.IMAGE, Tool.TEXT, Tool.STICKY, Tool.WEBLINK, Tool.TAPE, Tool.STICKER, Tool.LASER,
    )

    fun parse(spec: String): List<Pair<Tool, Boolean>> {
        val parsed = spec.split(',').filter { it.isNotBlank() }.mapNotNull { s ->
            val hidden = s.startsWith("-")
            runCatching { Tool.valueOf(s.removePrefix("-")) }.getOrNull()?.let { it to !hidden }
        }.distinctBy { it.first }
        // Tools added in a later version show up at the end.
        return parsed + defaults.filter { t -> parsed.none { it.first == t } }.map { it to true }
    }

    fun encode(list: List<Pair<Tool, Boolean>>) = list.joinToString(",") { (t, v) -> (if (v) "" else "-") + t.name }

    fun visible(spec: String) = parse(spec).filter { it.second }.map { it.first }

    /** Reorders visible tool [fromTool] to match [toTool]'s slot in the full persisted list. */
    fun moveVisible(spec: String, fromTool: Tool, toTool: Tool): String {
        if (fromTool == toTool) return spec
        val all = parse(spec).toMutableList()
        val fromIdx = all.indexOfFirst { it.first == fromTool }
        val toIdx = all.indexOfFirst { it.first == toTool }
        if (fromIdx < 0 || toIdx < 0) return spec
        val item = all.removeAt(fromIdx)
        all.add(toIdx, item)
        return encode(all)
    }
}
