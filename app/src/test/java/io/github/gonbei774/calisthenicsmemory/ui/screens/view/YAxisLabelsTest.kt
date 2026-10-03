package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YAxisLabelsTest {
    @Test fun `small ranges step by one`() {
        assertEquals(listOf(0f, 1f, 2f, 3f, 4f), calculateYAxisLabels(0f, 4f))
    }

    @Test fun `larger ranges use round steps`() {
        assertEquals(listOf(0f, 5f, 10f, 15f, 20f), calculateYAxisLabels(0f, 22f))
        assertEquals(listOf(0f, 20f, 40f, 60f, 80f), calculateYAxisLabels(0f, 99f))
    }

    @Test fun `labels are distinct whole numbers inside the plotted range`() {
        for (max in listOf(1f, 3f, 5.5f, 9.9f, 12f, 37f, 64f, 99f, 180f, 1234f)) {
            val labels = calculateYAxisLabels(0f, max)
            val whole = labels.map { it.toInt() }
            assertEquals("max=$max", whole.distinct(), whole)
            assertTrue("max=$max whole", labels.all { it == it.toInt().toFloat() })
            assertEquals(0f, labels.first())
            assertTrue("max=$max inside", labels.last() <= max)
            assertTrue("max=$max reaches the top half", labels.last() >= max / 2)
            assertTrue("max=$max at most 7", labels.size <= 7)
        }
    }

    @Test fun `the top of the graph is a labelled line at or above the highest point`() {
        assertEquals(20f, yAxisTop(16f))
        assertEquals(15f, yAxisTop(15f))
        assertEquals(4f, yAxisTop(4f))
        assertEquals(true, calculateYAxisLabels(0f, yAxisTop(16f)).last() >= 16f)
    }
}
