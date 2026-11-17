package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import io.github.gonbei774.calisthenicsmemory.util.AppClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/** Progress › Graph: volume for holds, the weekly change and one-sided days. */
class GraphFixesTest {
    private val today = AppClock.today()
    private fun day(ago: Long) = today.minusDays(ago).toString()

    private fun set(date: String, value: Int, left: Int? = null, weightG: Int? = null, exerciseId: Long = 1, n: Int = 1) =
        HistorySet(HistorySource.V2, null, emptyList(), exerciseId, date, "07:00", n, value, left, "", null, weightG, null)

    private val weighted = Exercise(id = 1, name = "Pull-up", type = "Dynamic", weightTrackingEnabled = true)

    @Test fun `a rep exercise with weight has volume, a hold does not`() {
        val records = listOf(set(day(0), 10, weightG = 5_000))
        assertEquals(1, prepareVolumeData(weighted, records, null, DayOfWeek.MONDAY).size)
        val hold = weighted.copy(type = "Isometric")
        assertTrue(prepareVolumeData(hold, records, null, DayOfWeek.MONDAY).isEmpty())
        // Its max weight is still there, and so are no volume statistics.
        assertNotNull(calculateStatistics(hold, records, null, DayOfWeek.MONDAY).maxWeight)
        assertNull(calculateStatistics(hold, records, null, DayOfWeek.MONDAY).maxDailyVolume)
        assertNotNull(calculateStatistics(weighted, records, null, DayOfWeek.MONDAY).maxDailyVolume)
    }

    @Test fun `the weekly change compares the last 7 days with the 7 before, whatever the period`() {
        val records = listOf(day(0), day(1), day(2)).map { set(it, 10) } + listOf(day(8), day(9)).map { set(it, 10) }
        val plain = Exercise(id = 1, name = "Push-up", type = "Dynamic")
        // 3 sets this week, 2 the week before: +1, even when the chart shows only "1 week".
        assertEquals(1, calculateStatistics(plain, records, Period.OneWeek, DayOfWeek.MONDAY).weeklyChange)
        assertEquals(1, calculateStatistics(plain, records, null, DayOfWeek.MONDAY).weeklyChange)
    }

    @Test fun `a one-sided exercise is charted right and left even when the first set has no left`() {
        val oneSided = Exercise(id = 1, name = "Pistol squat", type = "Dynamic", laterality = "Unilateral")
        val records = listOf(set(day(0), 5, left = null, n = 1), set(day(0), 6, left = 4, n = 2))
        val point = prepareGraphData(oneSided, records, null, GraphType.Max, "reps", DayOfWeek.MONDAY).single()
        assertEquals(4f, point.valueLeft)
        // A two-sided exercise with no left values stays two-sided.
        val plain = Exercise(id = 1, name = "Push-up", type = "Dynamic")
        assertNull(prepareGraphData(plain, listOf(set(day(0), 5)), null, GraphType.Max, "reps", DayOfWeek.MONDAY).single().valueLeft)
    }
}
