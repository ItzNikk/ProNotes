package com.pronotes.app.editor

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pronotes.app.data.ToolbarDock
import com.pronotes.app.data.ToolbarStyle
import kotlin.math.roundToInt

enum class ToolbarOrientation { HORIZONTAL, VERTICAL }
enum class PopoverCaretSide { TOP, BOTTOM, LEFT, RIGHT }

@Immutable
data class ToolbarAdaptiveLayout(
    val orientation: ToolbarOrientation,
    val isFloatingOverlay: Boolean,
    val showInlineOptions: Boolean,
    val pinnedTools: List<Tool>,
    val overflowTools: List<Tool>,
    val caretSide: PopoverCaretSide,
)

/**
 * Pure geometry helpers for single-pass toolbar overflow splitting and safe bounds clamping
 * across window resizes, rotation, and multi-window split screen changes.
 */
object ToolbarGeometry {
    fun computeAdaptiveLayout(
        dockEdge: ToolbarDock,
        style: ToolbarStyle,
        visibleTools: List<Tool>,
        activeTool: Tool,
        availablePx: Int,
        slotExtentPx: Int,
        reservedPx: Int,
    ): ToolbarAdaptiveLayout {
        val orientation = if (dockEdge == ToolbarDock.LEFT || dockEdge == ToolbarDock.RIGHT) {
            ToolbarOrientation.VERTICAL
        } else {
            ToolbarOrientation.HORIZONTAL
        }
        val isFloating = dockEdge == ToolbarDock.FREE || style != ToolbarStyle.FIXED_BAR
        val showInlineOptions = style != ToolbarStyle.COMPACT && !isVerticalCompact(orientation, style)
        val caretSide = when {
            dockEdge == ToolbarDock.LEFT -> PopoverCaretSide.LEFT
            dockEdge == ToolbarDock.RIGHT -> PopoverCaretSide.RIGHT
            dockEdge == ToolbarDock.BOTTOM -> PopoverCaretSide.BOTTOM
            else -> PopoverCaretSide.TOP
        }

        if (visibleTools.isEmpty()) {
            return ToolbarAdaptiveLayout(
                orientation = orientation,
                isFloatingOverlay = isFloating,
                showInlineOptions = showInlineOptions,
                pinnedTools = emptyList(),
                overflowTools = emptyList(),
                caretSide = caretSide,
            )
        }

        val usablePx = (availablePx - reservedPx).coerceAtLeast(slotExtentPx * 3)
        val maxSlotsBySpace = (usablePx / slotExtentPx.coerceAtLeast(1)).coerceAtLeast(3)
        val styleCap = when (style) {
            ToolbarStyle.FIXED_BAR -> visibleTools.size
            ToolbarStyle.FLOATING_PILL -> 7
            ToolbarStyle.COMPACT -> 4
        }
        val maxSlots = minOf(maxSlotsBySpace, styleCap, visibleTools.size)

        if (maxSlots >= visibleTools.size) {
            return ToolbarAdaptiveLayout(
                orientation = orientation,
                isFloatingOverlay = isFloating,
                showInlineOptions = showInlineOptions,
                pinnedTools = visibleTools,
                overflowTools = emptyList(),
                caretSide = caretSide,
            )
        }

        // Keep the active tool guaranteed visible inside the pinned bar; least-used trailing tools move to overflow.
        val pinned = visibleTools.take(maxSlots).toMutableList()
        if (activeTool in visibleTools && activeTool !in pinned && pinned.isNotEmpty()) {
            pinned[pinned.lastIndex] = activeTool
        }
        val overflow = visibleTools.filter { it !in pinned }

        return ToolbarAdaptiveLayout(
            orientation = orientation,
            isFloatingOverlay = isFloating,
            showInlineOptions = showInlineOptions,
            pinnedTools = pinned,
            overflowTools = overflow,
            caretSide = caretSide,
        )
    }

    private fun isVerticalCompact(orientation: ToolbarOrientation, style: ToolbarStyle): Boolean =
        orientation == ToolbarOrientation.VERTICAL && style == ToolbarStyle.COMPACT

    /**
     * Clamps floating/anchored pill coordinates so the toolbar never drifts off-screen or under system bars
     * across rotation, split-screen resizing, or multi-window changes.
     */
    fun clampFloatingOffset(
        desiredX: Int,
        desiredY: Int,
        pillSize: IntSize,
        areaSize: IntSize,
        marginPx: Int,
        topSafePx: Int = marginPx,
        bottomSafePx: Int = marginPx,
    ): IntOffset {
        if (areaSize.width <= 0 || areaSize.height <= 0) {
            return IntOffset(marginPx, topSafePx)
        }
        val w = pillSize.width.coerceAtLeast(1)
        val h = pillSize.height.coerceAtLeast(1)
        val maxX = (areaSize.width - w - marginPx).coerceAtLeast(marginPx)
        val maxY = (areaSize.height - h - bottomSafePx).coerceAtLeast(topSafePx)
        return IntOffset(
            x = desiredX.coerceIn(marginPx, maxX),
            y = desiredY.coerceIn(topSafePx, maxY),
        )
    }

    fun normalizedToOffset(
        normX: Float,
        normY: Float,
        pillSize: IntSize,
        areaSize: IntSize,
        marginPx: Int,
        topSafePx: Int = marginPx,
        bottomSafePx: Int = marginPx,
    ): IntOffset {
        val w = pillSize.width.coerceAtLeast(1)
        val h = pillSize.height.coerceAtLeast(1)
        val availW = (areaSize.width - w - marginPx * 2).coerceAtLeast(1)
        val availH = (areaSize.height - h - topSafePx - bottomSafePx).coerceAtLeast(1)
        val dx = marginPx + (normX.coerceIn(0f, 1f) * availW).roundToInt()
        val dy = topSafePx + (normY.coerceIn(0f, 1f) * availH).roundToInt()
        return clampFloatingOffset(dx, dy, pillSize, areaSize, marginPx, topSafePx, bottomSafePx)
    }
}

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
