package com.pronotes.app.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MindMapLayoutEngineTest {

    private fun sampleTree() = MindMapLayoutEngine.MindNode(
        id = "root", label = "Biology",
        children = listOf(
            MindMapLayoutEngine.MindNode("cell", "Cell", children = listOf(
                MindMapLayoutEngine.MindNode("mito", "Mitochondria"),
                MindMapLayoutEngine.MindNode("nucleus", "Nucleus"),
            )),
            MindMapLayoutEngine.MindNode("ecology", "Ecology", children = listOf(
                MindMapLayoutEngine.MindNode("food", "Food Web"),
            )),
            MindMapLayoutEngine.MindNode("genetics", "Genetics"),
        )
    )

    @Test
    fun `radial layout places correct number of nodes and edges`() {
        val result = MindMapLayoutEngine.layout(sampleTree(), MindMapLayoutEngine.LayoutMode.RADIAL)
        assertEquals(7, result.nodes.size)
        assertEquals(6, result.edges.size) // 7 nodes - 1 root = 6 parent-child edges
    }

    @Test
    fun `tree layout root is at depth 0`() {
        val result = MindMapLayoutEngine.layout(sampleTree(), MindMapLayoutEngine.LayoutMode.TREE)
        val root = result.nodes.first { it.id == "root" }
        assertEquals(0, root.depth)
    }

    @Test
    fun `bounding box has positive dimensions`() {
        val result = MindMapLayoutEngine.layout(sampleTree(), MindMapLayoutEngine.LayoutMode.RADIAL)
        assertTrue(result.boundingWidth > 0)
        assertTrue(result.boundingHeight > 0)
    }

    @Test
    fun `single root node has no edges`() {
        val solo = MindMapLayoutEngine.MindNode("solo", "Solo Node")
        val result = MindMapLayoutEngine.layout(solo, MindMapLayoutEngine.LayoutMode.RADIAL)
        assertEquals(1, result.nodes.size)
        assertEquals(0, result.edges.size)
    }
}
