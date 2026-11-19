package com.pronotes.app.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfTaskTest {
    @Test fun pageNumbersMatchThePrintedOrder() {
        assertEquals(listOf(0, 2, 4), PdfTask.ODD.pages(5).toList())
        assertEquals(listOf(1, 3), PdfTask.EVEN.pages(5).toList())
        assertEquals(listOf(2, 1, 0), PdfTask.REVERSE.pages(3).toList())
    }

    @Test fun singlePageAndInclusiveRanges() {
        assertEquals(listOf(2), PdfTask.EXTRACT.pages(3, "3").toList())
        assertEquals(listOf(0, 1, 2), PdfTask.EXTRACT.pages(3, "1 – 3").toList())
    }

    @Test fun invalidSelectionsAreRejected() {
        for (range in listOf("0", "4", "2-1", "1-2-3", "", "-1", "1-")) {
            assertThrows(IllegalArgumentException::class.java) { PdfTask.EXTRACT.pages(3, range) }
        }
        assertThrows(IllegalArgumentException::class.java) { PdfTask.EVEN.pages(1) }
    }
}
