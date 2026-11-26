package com.pronotes.app.editor

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.max

/**
 * Pure-Kotlin mind-map layout engine.
 *
 * Supports three layout strategies:
 * - [LayoutMode.RADIAL]:  Root at center, children fanned radially, grandchildren further out.
 * - [LayoutMode.TREE]:    Root at top, children in a horizontal tree below it.
 * - [LayoutMode.ORG]:     Root at top, balanced org-chart columns.
 *
 * All positions are in arbitrary float units (canvas pixels or dp — caller decides scale).
 */
object MindMapLayoutEngine {

    enum class LayoutMode { RADIAL, TREE, ORG }

    data class MindNode(
        val id: String,
        val label: String,
        val children: List<MindNode> = emptyList(),
    )

    data class LayoutNode(
        val id: String,
        val label: String,
        val x: Float,
        val y: Float,
        val depth: Int,
        val parentId: String?,
    )

    data class LayoutResult(
        val nodes: List<LayoutNode>,
        val edges: List<Pair<String, String>>,   // parentId → childId
        val boundingWidth: Float,
        val boundingHeight: Float,
    )

    fun layout(
        root: MindNode,
        mode: LayoutMode = LayoutMode.RADIAL,
        hSpacing: Float = 180f,
        vSpacing: Float = 120f,
    ): LayoutResult {
        val nodes = mutableListOf<LayoutNode>()
        val edges = mutableListOf<Pair<String, String>>()

        when (mode) {
            LayoutMode.RADIAL -> layoutRadial(root, nodes, edges, hSpacing, vSpacing)
            LayoutMode.TREE   -> layoutTree(root, nodes, edges, hSpacing, vSpacing)
            LayoutMode.ORG    -> layoutOrg(root, nodes, edges, hSpacing, vSpacing)
        }

        val minX = nodes.minOfOrNull { it.x } ?: 0f
        val minY = nodes.minOfOrNull { it.y } ?: 0f
        val maxX = nodes.maxOfOrNull { it.x } ?: 0f
        val maxY = nodes.maxOfOrNull { it.y } ?: 0f

        return LayoutResult(
            nodes = nodes,
            edges = edges,
            boundingWidth = maxX - minX + hSpacing,
            boundingHeight = maxY - minY + vSpacing,
        )
    }

    // ── Radial layout ──────────────────────────────────────────────────────────

    private fun layoutRadial(
        root: MindNode,
        nodes: MutableList<LayoutNode>,
        edges: MutableList<Pair<String, String>>,
        hSpacing: Float,
        vSpacing: Float,
    ) {
        nodes.add(LayoutNode(root.id, root.label, 0f, 0f, 0, null))
        placeRadialChildren(root, cx = 0f, cy = 0f, depth = 1,
            startAngle = 0.0, sweepAngle = 2 * PI,
            radius = hSpacing, nodes = nodes, edges = edges, hSpacing = hSpacing)
    }

    private fun placeRadialChildren(
        node: MindNode,
        cx: Float, cy: Float,
        depth: Int,
        startAngle: Double, sweepAngle: Double,
        radius: Float,
        nodes: MutableList<LayoutNode>,
        edges: MutableList<Pair<String, String>>,
        hSpacing: Float,
    ) {
        val n = node.children.size
        if (n == 0) return
        val angleStep = sweepAngle / n
        node.children.forEachIndexed { i, child ->
            val angle = startAngle + angleStep * i + angleStep / 2
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle)).toFloat()
            nodes.add(LayoutNode(child.id, child.label, x, y, depth, node.id))
            edges.add(node.id to child.id)
            placeRadialChildren(child, x, y, depth + 1,
                angle - angleStep / 2, angleStep,
                hSpacing * 0.9f, nodes, edges, hSpacing)
        }
    }

    // ── Tree layout ────────────────────────────────────────────────────────────

    private var treeXCursor = 0f

    private fun layoutTree(
        root: MindNode,
        nodes: MutableList<LayoutNode>,
        edges: MutableList<Pair<String, String>>,
        hSpacing: Float,
        vSpacing: Float,
    ) {
        treeXCursor = 0f
        placeTreeNode(root, depth = 0, parentId = null, nodes = nodes, edges = edges,
            hSpacing = hSpacing, vSpacing = vSpacing)
    }

    private fun placeTreeNode(
        node: MindNode,
        depth: Int,
        parentId: String?,
        nodes: MutableList<LayoutNode>,
        edges: MutableList<Pair<String, String>>,
        hSpacing: Float,
        vSpacing: Float,
    ): Float {
        return if (node.children.isEmpty()) {
            val x = treeXCursor
            treeXCursor += hSpacing
            val y = depth * vSpacing
            nodes.add(LayoutNode(node.id, node.label, x, y, depth, parentId))
            if (parentId != null) edges.add(parentId to node.id)
            x
        } else {
            val childXs = node.children.map { child ->
                placeTreeNode(child, depth + 1, node.id, nodes, edges, hSpacing, vSpacing)
            }
            val x = (childXs.first() + childXs.last()) / 2f
            val y = depth * vSpacing
            nodes.add(LayoutNode(node.id, node.label, x, y, depth, parentId))
            if (parentId != null) edges.add(parentId to node.id)
            x
        }
    }

    // ── Org-chart layout ───────────────────────────────────────────────────────

    private fun layoutOrg(
        root: MindNode,
        nodes: MutableList<LayoutNode>,
        edges: MutableList<Pair<String, String>>,
        hSpacing: Float,
        vSpacing: Float,
    ) {
        // Org reuses tree layout logic (balanced horizontal spread)
        treeXCursor = 0f
        placeTreeNode(root, depth = 0, parentId = null, nodes = nodes, edges = edges,
            hSpacing = hSpacing * 1.2f, vSpacing = vSpacing)
    }
}
