package com.pronotes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSimilarityMatrixTest {

    private val notes = listOf(
        "Photosynthesis converts sunlight into glucose using chlorophyll in plant cells.",
        "Plants use photosynthesis to make glucose from sunlight and carbon dioxide.",
        "Cellular respiration breaks down glucose to release ATP energy in mitochondria.",
        "The French Revolution began in 1789 and overthrew the monarchy of France.",
    )

    @Test
    fun `matrix is square and symmetric`() {
        val matrix = NoteSimilarityMatrix.compute(notes)
        assertEquals(notes.size, matrix.size)
        assertEquals(notes.size, matrix[0].size)
        for (i in notes.indices) {
            assertEquals(1f, matrix[i][i], 0.001f)
            for (j in notes.indices) {
                assertEquals(matrix[i][j], matrix[j][i], 0.001f)
            }
        }
    }

    @Test
    fun `similar notes score higher than unrelated ones`() {
        val matrix = NoteSimilarityMatrix.compute(notes)
        val photoBio = matrix[0][1]  // both about photosynthesis
        val photoFrench = matrix[0][3]  // unrelated topics
        assertTrue("Similar notes should score higher: $photoBio vs $photoFrench", photoBio > photoFrench)
    }

    @Test
    fun `topSimilar returns correct number of results`() {
        val matrix = NoteSimilarityMatrix.compute(notes)
        val top = NoteSimilarityMatrix.topSimilar(matrix, noteIndex = 0, topK = 2)
        assertEquals(2, top.size)
        assertTrue("Top result should not be itself", top.none { it.first == 0 })
    }

    @Test
    fun `identical notes have similarity 1`() {
        val same = listOf("hello world foo bar baz", "hello world foo bar baz")
        val matrix = NoteSimilarityMatrix.compute(same)
        assertEquals(1f, matrix[0][1], 0.001f)
    }
}
