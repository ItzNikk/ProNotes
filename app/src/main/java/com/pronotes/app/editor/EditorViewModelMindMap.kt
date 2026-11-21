package com.pronotes.app.editor

import android.graphics.RectF
import com.pronotes.app.data.Flashcard
import java.util.UUID
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

fun EditorViewModel.selectedMindNode(): MindNodeItem? {
    val id = activeMindNodeId ?: return null
    val p = pages.getOrNull(pageIndex) ?: return null
    return p.items.filterIsInstance<MindNodeItem>().firstOrNull { it.id == id }
}

private fun EditorViewModel.mindBoxSize(label: String, tag: String = "", notes: String = ""): Pair<Float, Float> {
    val lines = label.split('\n').ifEmpty { listOf("Idea") }
    val longest = max(lines.maxOf { it.length }, tag.length + 2).coerceAtLeast(6)
    val w = (longest * 13.5f + 68f).coerceIn(176f, 440f)
    val extraTag = if (tag.isNotBlank()) 22f else 0f
    val extraNotes = if (notes.isNotBlank()) 20f else 0f
    val h = (lines.size * 34f + 44f + extraTag + extraNotes).coerceIn(72f, 260f)
    return w to h
}

private fun EditorViewModel.clampMindPos(p: Page, x: Float, y: Float, w: Float, h: Float): Pair<Float, Float> {
    if (p.isWhiteboard) return x to y
    val maxX = (p.width - w - 56f).coerceAtLeast(56f)
    val maxY = (p.height - h - 56f).coerceAtLeast(56f)
    return x.coerceIn(56f, maxX) to y.coerceIn(56f, maxY)
}

fun EditorViewModel.fitMindMapToView() {
    val p = pages.getOrNull(pageIndex) ?: return
    val nodes = p.items.filterIsInstance<MindNodeItem>()
    if (nodes.isEmpty()) return
    val r = RectF(nodes.first().bounds)
    nodes.drop(1).forEach { r.union(it.bounds) }
    r.inset(-72f, -72f)
    fitMindMapOnCanvas?.invoke(pageIndex, r)
}

fun EditorViewModel.onMindMapTap(page: Int, x: Float, y: Float, existing: MindNodeItem?) {
    if (existing != null) {
        if (activeMindNodeId == existing.id) {
            mindNodeEdit = EditorViewModel.MindNodeEdit(page, existing, isNew = false)
        } else {
            activeMindNodeId = existing.id
            mindColor = existing.color
            mindShape = existing.shape
        }
        return
    }
    val p = pages.getOrNull(page) ?: return
    val parent = p.items.filterIsInstance<MindNodeItem>().firstOrNull { it.id == activeMindNodeId }
    val (w, h) = mindBoxSize(if (parent == null) "Central topic" else "New branch")
    val (cx, cy) = clampMindPos(p, x - w / 2f, y - h / 2f, w, h)
    val branchColor = if (parent == null) {
        mindColor
    } else {
        val siblings = p.items.filterIsInstance<MindNodeItem>().count { it.parentId == parent.id }
        MindPalette[(MindPalette.indexOf(parent.color).coerceAtLeast(0) + siblings + 1) % MindPalette.size]
    }
    val node = MindNodeItem(
        id = ItemIds.next(),
        label = if (parent == null) "Central topic" else "New branch",
        x = cx,
        y = cy,
        w = w,
        h = h,
        color = branchColor,
        parentId = parent?.id,
        shape = mindShape,
    )
    mindNodeEdit = EditorViewModel.MindNodeEdit(page, node, isNew = true)
}

fun EditorViewModel.openEditSelectedMindNode() {
    val node = selectedMindNode() ?: return
    mindNodeEdit = EditorViewModel.MindNodeEdit(pageIndex, node, isNew = false)
}

fun EditorViewModel.addChildMindNode() {
    val p = pages.getOrNull(pageIndex) ?: return
    val nodes = p.items.filterIsInstance<MindNodeItem>()
    val parent = nodes.firstOrNull { it.id == activeMindNodeId } ?: nodes.firstOrNull()
    val (w, h) = mindBoxSize(if (parent == null) "Central topic" else "Subtopic")
    val (rx, ry) = if (parent == null) {
        (p.width - w) / 2f to (p.height - h) / 2f
    } else {
        val kids = nodes.filter { it.parentId == parent.id }
        val angle = ((kids.size * 52.0 - 26.0) * Math.PI / 180.0)
        val dist = max(parent.w, w) + 96f
        val tx = parent.x + parent.w / 2f + cos(angle).toFloat() * dist - w / 2f
        val ty = parent.y + parent.h / 2f + sin(angle).toFloat() * dist - h / 2f
        clampMindPos(p, tx, ty, w, h)
    }
    val color = if (parent == null) mindColor else {
        val kids = nodes.count { it.parentId == parent.id }
        MindPalette[(MindPalette.indexOf(parent.color).coerceAtLeast(0) + kids + 1) % MindPalette.size]
    }
    val node = MindNodeItem(
        id = ItemIds.next(),
        label = if (parent == null) "Central topic" else "Subtopic",
        x = rx,
        y = ry,
        w = w,
        h = h,
        color = color,
        parentId = parent?.id,
        shape = mindShape,
    )
    mindNodeEdit = EditorViewModel.MindNodeEdit(pageIndex, node, isNew = true)
}

fun EditorViewModel.addSiblingMindNode() {
    val p = pages.getOrNull(pageIndex) ?: return
    val nodes = p.items.filterIsInstance<MindNodeItem>()
    val cur = nodes.firstOrNull { it.id == activeMindNodeId } ?: return
    val parentId = cur.parentId
    val (w, h) = mindBoxSize("Related idea")
    val (nx, ny) = clampMindPos(p, cur.x, cur.y + cur.h + 36f, w, h)
    val siblings = nodes.count { it.parentId == parentId }
    val color = MindPalette[(MindPalette.indexOf(cur.color).coerceAtLeast(0) + siblings) % MindPalette.size]
    val node = MindNodeItem(
        id = ItemIds.next(),
        label = "Related idea",
        x = nx,
        y = ny,
        w = w,
        h = h,
        color = color,
        parentId = parentId,
        shape = cur.shape,
    )
    mindNodeEdit = EditorViewModel.MindNodeEdit(pageIndex, node, isNew = true)
}

fun EditorViewModel.commitMindNode(
    label: String,
    color: Int,
    shape: String = mindShape,
    notes: String = "",
    tag: String = "",
) {
    val edit = mindNodeEdit ?: return
    mindNodeEdit = null
    val clean = label.trim()
    if (clean.isEmpty()) return
    val p = pages.getOrNull(edit.page) ?: return
    val cleanNotes = notes.trim()
    val cleanTag = tag.trim()
    val (w, h) = mindBoxSize(clean, cleanTag, cleanNotes)
    val (nx, ny) = clampMindPos(p, edit.item.x, edit.item.y, w, h)
    val updated = edit.item.copy(
        label = clean,
        x = nx,
        y = ny,
        w = w,
        h = h,
        color = color,
        shape = shape,
        notes = cleanNotes,
        tag = cleanTag,
    )
    mindColor = color
    mindShape = shape
    beginEdit(edit.page)
    val items = if (edit.isNew) {
        p.items + updated
    } else {
        p.items.map { if (it.id == updated.id) updated else it }
    }
    applyItems(edit.page, items)
    activeMindNodeId = updated.id
}

fun EditorViewModel.deleteMindNode(id: Long? = activeMindNodeId) {
    val target = id ?: return
    val p = pages.getOrNull(pageIndex) ?: return
    val nodes = p.items.filterIsInstance<MindNodeItem>()
    if (nodes.none { it.id == target }) return
    val toRemove = HashSet<Long>()
    val stack = ArrayDeque<Long>().apply { add(target) }
    while (stack.isNotEmpty()) {
        val cur = stack.removeFirst()
        if (toRemove.add(cur)) {
            nodes.filter { it.parentId == cur }.forEach { stack.addLast(it.id) }
        }
    }
    beginEdit(pageIndex)
    applyItems(pageIndex, p.items.filterNot { it.id in toRemove })
    if (activeMindNodeId in toRemove) activeMindNodeId = null
}

fun EditorViewModel.toggleCollapseMindNode(id: Long? = activeMindNodeId) {
    val target = id ?: return
    val p = pages.getOrNull(pageIndex) ?: return
    val node = p.items.filterIsInstance<MindNodeItem>().firstOrNull { it.id == target } ?: return
    beginEdit(pageIndex)
    applyItems(
        pageIndex,
        p.items.map { item ->
            if (item is MindNodeItem && item.id == target) item.copy(collapsed = !node.collapsed) else item
        },
    )
}

fun EditorViewModel.recolorMindNode(id: Long? = activeMindNodeId, color: Int) {
    val target = id ?: return
    val p = pages.getOrNull(pageIndex) ?: return
    mindColor = color
    beginEdit(pageIndex)
    applyItems(
        pageIndex,
        p.items.map { item ->
            if (item is MindNodeItem && item.id == target) item.copy(color = color) else item
        },
    )
}

fun EditorViewModel.autoLayoutMindMap(layout: EditorViewModel.MindLayout = EditorViewModel.MindLayout.RADIAL) {
    val p = pages.getOrNull(pageIndex) ?: return
    val nodes = p.items.filterIsInstance<MindNodeItem>()
    if (nodes.isEmpty()) return
    val byId = nodes.associateBy { it.id }
    val children = HashMap<Long, MutableList<MindNodeItem>>()
    val roots = ArrayList<MindNodeItem>()
    for (n in nodes) {
        val pid = n.parentId
        if (pid != null && pid in byId && pid != n.id) {
            children.getOrPut(pid) { ArrayList() }.add(n)
        } else {
            roots.add(n)
        }
    }
    if (roots.isEmpty()) roots.add(nodes.first())

    val placed = HashMap<Long, Pair<Float, Float>>()

    fun countLeaves(id: Long, seen: HashSet<Long>): Int {
        if (!seen.add(id)) return 1
        val kids = children[id].orEmpty()
        return if (kids.isEmpty()) 1 else kids.sumOf { countLeaves(it.id, seen) }
    }

    fun placeSubtree(
        id: Long,
        cx: Float,
        cy: Float,
        startAngle: Double,
        endAngle: Double,
        depth: Int,
        seen: HashSet<Long>,
    ) {
        if (!seen.add(id)) return
        val n = byId[id] ?: return
        placed[id] = (cx - n.w / 2f) to (cy - n.h / 2f)
        val kids = children[id].orEmpty()
        if (kids.isEmpty()) return
        val total = kids.sumOf { countLeaves(it.id, HashSet(seen)) }.coerceAtLeast(1)
        val radius = 250f + depth * 28f
        var cursor = startAngle
        for (k in kids) {
            val wLeaves = countLeaves(k.id, HashSet(seen))
            val sweep = (endAngle - startAngle) * (wLeaves.toDouble() / total.toDouble())
            val mid = cursor + sweep / 2.0
            val kx = cx + cos(mid).toFloat() * radius
            val ky = cy + sin(mid).toFloat() * radius
            placeSubtree(k.id, kx, ky, cursor, cursor + sweep, depth + 1, seen)
            cursor += sweep
        }
    }

    val vGap = 36f
    val hGap = 84f
    fun subtreeHeight(id: Long, seen: HashSet<Long>): Float {
        if (!seen.add(id)) return 96f
        val n = byId[id] ?: return 96f
        val kids = children[id].orEmpty()
        if (kids.isEmpty()) return n.h
        return max(n.h, kids.sumOf { subtreeHeight(it.id, seen).toDouble() }.toFloat() + vGap * (kids.size - 1))
    }

    fun placeTree(id: Long, x: Float, topY: Float, spanH: Float, seen: HashSet<Long>) {
        if (!seen.add(id)) return
        val n = byId[id] ?: return
        placed[id] = x to (topY + (spanH - n.h) / 2f)
        val kids = children[id].orEmpty()
        if (kids.isEmpty()) return
        val kidHeights = kids.map { subtreeHeight(it.id, HashSet(seen)) }
        val totalH = kidHeights.sum() + vGap * (kids.size - 1)
        var cy = topY + (spanH - totalH) / 2f
        val childX = x + n.w + hGap
        kids.forEachIndexed { idx, k ->
            val kh = kidHeights[idx]
            placeTree(k.id, childX, cy, kh, seen)
            cy += kh + vGap
        }
    }

    val orgHGap = 40f
    val orgVGap = 86f
    fun subtreeWidth(id: Long, seen: HashSet<Long>): Float {
        if (!seen.add(id)) return 180f
        val n = byId[id] ?: return 180f
        val kids = children[id].orEmpty()
        if (kids.isEmpty()) return n.w
        return max(n.w, kids.sumOf { subtreeWidth(it.id, seen).toDouble() }.toFloat() + orgHGap * (kids.size - 1))
    }

    fun placeOrg(id: Long, leftX: Float, y: Float, spanW: Float, seen: HashSet<Long>) {
        if (!seen.add(id)) return
        val n = byId[id] ?: return
        placed[id] = (leftX + (spanW - n.w) / 2f) to y
        val kids = children[id].orEmpty()
        if (kids.isEmpty()) return
        val kidWidths = kids.map { subtreeWidth(it.id, HashSet(seen)) }
        val totalW = kidWidths.sum() + orgHGap * (kids.size - 1)
        var cx = leftX + (spanW - totalW) / 2f
        val childY = y + n.h + orgVGap
        kids.forEachIndexed { idx, k ->
            val kw = kidWidths[idx]
            placeOrg(k.id, cx, childY, kw, seen)
            cx += kw + orgHGap
        }
    }

    when (layout) {
        EditorViewModel.MindLayout.RADIAL -> {
            val seen = HashSet<Long>()
            val baseCx = p.width / 2f
            val baseCy = p.height / 2f
            roots.forEachIndexed { idx, root ->
                val offset = idx * 420f
                placeSubtree(root.id, baseCx + offset, baseCy, 0.0, Math.PI * 2.0, 0, seen)
            }
        }
        EditorViewModel.MindLayout.TREE -> {
            val seen = HashSet<Long>()
            var yCursor = 96f
            roots.forEach { root ->
                val h = subtreeHeight(root.id, HashSet(seen))
                placeTree(root.id, 84f, yCursor, h, seen)
                yCursor += h + 72f
            }
        }
        EditorViewModel.MindLayout.ORG_CHART -> {
            val seen = HashSet<Long>()
            var xCursor = 84f
            roots.forEach { root ->
                val w = subtreeWidth(root.id, HashSet(seen))
                placeOrg(root.id, xCursor, 96f, w, seen)
                xCursor += w + 72f
            }
        }
    }

    beginEdit(pageIndex)
    applyItems(
        pageIndex,
        p.items.map { item ->
            if (item is MindNodeItem) {
                val pos = placed[item.id]
                if (pos != null) {
                    val (cx, cy) = clampMindPos(p, pos.first, pos.second, item.w, item.h)
                    item.copy(x = cx, y = cy)
                } else {
                    item
                }
            } else {
                item
            }
        },
    )
    fitMindMapToView()
    say("Arranged mind map (${layout.label})")
}

fun EditorViewModel.exportMindMapOutlineToPage() {
    val p = pages.getOrNull(pageIndex) ?: return
    val nodes = p.items.filterIsInstance<MindNodeItem>()
    if (nodes.isEmpty()) {
        say("No mind map nodes on this page")
        return
    }
    val byId = nodes.associateBy { it.id }
    val children = HashMap<Long, MutableList<MindNodeItem>>()
    val roots = ArrayList<MindNodeItem>()
    for (n in nodes) {
        val pid = n.parentId
        if (pid != null && pid in byId && pid != n.id) children.getOrPut(pid) { ArrayList() }.add(n)
        else roots.add(n)
    }
    if (roots.isEmpty()) roots.add(nodes.first())

    val sb = StringBuilder()
    fun walk(node: MindNodeItem, depth: Int, seen: HashSet<Long>) {
        if (!seen.add(node.id)) return
        val indent = "  ".repeat(depth)
        val prefix = if (depth == 0) "• " else "- "
        val tagPart = if (node.tag.isNotBlank()) "[${node.tag}] " else ""
        sb.append(indent).append(prefix).append(tagPart).append(node.label)
        if (node.notes.isNotBlank()) sb.append(" — ").append(node.notes)
        sb.append('\n')
        children[node.id].orEmpty().forEach { walk(it, depth + 1, seen) }
    }
    val seen = HashSet<Long>()
    roots.forEach { walk(it, 0, seen) }
    val outline = sb.toString().trim()
    if (outline.isEmpty()) return

    val minX = nodes.minOf { it.x }.coerceAtLeast(64f)
    val bottomY = (nodes.maxOf { it.y + it.h } + 48f).coerceAtMost((p.height - 220f).coerceAtLeast(96f))
    beginEdit(pageIndex)
    applyItems(
        pageIndex,
        p.items + TextItem(
            id = ItemIds.next(),
            text = outline,
            x = minX,
            y = bottomY,
            size = 24f,
            color = textColor,
            style = textStyle,
        ),
    )
    say("Exported outline onto page ${pageIndex + 1}")
}

fun EditorViewModel.flashcardsFromMindMap() {
    val p = pages.getOrNull(pageIndex) ?: return
    val nodes = p.items.filterIsInstance<MindNodeItem>()
    if (nodes.isEmpty()) {
        say("No mind map nodes on this page")
        return
    }
    val byId = nodes.associateBy { it.id }
    val children = nodes.groupBy { it.parentId }
    val now = System.currentTimeMillis()
    val created = ArrayList<Flashcard>()

    for (n in nodes) {
        val kids = children[n.id].orEmpty()
        when {
            n.notes.isNotBlank() -> {
                created += Flashcard(
                    id = UUID.randomUUID().toString(),
                    front = n.label,
                    back = n.notes,
                    pageId = p.id,
                    created = now,
                    hint = n.tag,
                )
            }
            kids.isNotEmpty() -> {
                val answer = kids.joinToString("\n") { "• ${it.label}" }
                created += Flashcard(
                    id = UUID.randomUUID().toString(),
                    front = "What branches connect to ${n.label}?",
                    back = answer,
                    pageId = p.id,
                    created = now,
                    hint = n.tag,
                )
            }
            n.parentId != null && byId[n.parentId] != null -> {
                val parent = byId.getValue(n.parentId)
                if (n.tag.isNotBlank()) {
                    created += Flashcard(
                        id = UUID.randomUUID().toString(),
                        front = "${parent.label}: ${n.tag}",
                        back = n.label,
                        pageId = p.id,
                        created = now,
                    )
                }
            }
        }
    }

    if (created.isEmpty()) {
        say("Add sub-branches or node notes to create flashcards")
        return
    }
    cards = cards + created
    saveStudy()
    say("Created ${created.size} flashcard${if (created.size == 1) "" else "s"} from mind map")
}

fun EditorViewModel.buildMindMapFromNotes() {
    val p = pages.getOrNull(pageIndex) ?: return
    val rawLines = buildList {
        p.items.forEach { item ->
            when (item) {
                is TextItem -> addAll(item.text.lines())
                is WriteBoxItem -> if (item.convertedText.isNotBlank()) addAll(item.convertedText.lines())
                is StickyItem -> {
                    if (item.title.isNotBlank()) add(item.title)
                    addAll(item.text.lines())
                }
                else -> Unit
            }
        }
    }.map { it.trim().trimStart('-', '*', '•', '—', '–').trim() }
        .filter { it.length >= 2 }
        .distinct()
        .take(14)

    if (rawLines.isEmpty()) {
        say("Add typed text or sticky notes first to generate a mind map")
        return
    }

    val rootLabel = rawLines.first().take(48)
    val (rw, rh) = mindBoxSize(rootLabel)
    val rootId = ItemIds.next()
    val created = ArrayList<MindNodeItem>()
    created += MindNodeItem(
        id = rootId,
        label = rootLabel,
        x = (p.width - rw) / 2f,
        y = (p.height - rh) / 2f,
        w = rw,
        h = rh,
        color = MindPalette[0],
        parentId = null,
        shape = mindShape,
    )
    rawLines.drop(1).forEachIndexed { idx, line ->
        val clean = line.take(56)
        val (w, h) = mindBoxSize(clean)
        val angle = idx * (2.0 * Math.PI / max(1, rawLines.size - 1))
        val tx = p.width / 2f + cos(angle).toFloat() * 290f - w / 2f
        val ty = p.height / 2f + sin(angle).toFloat() * 250f - h / 2f
        val (cx, cy) = clampMindPos(p, tx, ty, w, h)
        created += MindNodeItem(
            id = ItemIds.next(),
            label = clean,
            x = cx,
            y = cy,
            w = w,
            h = h,
            color = MindPalette[(idx + 1) % MindPalette.size],
            parentId = rootId,
            shape = mindShape,
        )
    }

    beginEdit(pageIndex)
    applyItems(pageIndex, p.items + created)
    activeMindNodeId = rootId
    autoLayoutMindMap(EditorViewModel.MindLayout.RADIAL)
    say("Built mind map with ${created.size} nodes")
}
