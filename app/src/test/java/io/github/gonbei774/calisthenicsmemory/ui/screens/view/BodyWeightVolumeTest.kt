package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import io.github.gonbei774.calisthenicsmemory.data.BodyWeight
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/** Volume of an exercise with a share of body weight (ADR 0016). */
class BodyWeightVolumeTest {
    private val pushUp = Exercise(id = 1, name = "Push-up", type = "Dynamic", bodyWeightPercent = 65)
    private val weights = listOf(BodyWeight(1, "2026-10-11", 60_000), BodyWeight(2, "2026-12-10", 65_000))

    private fun set(date: String, reps: Int, left: Int? = null, added: Int? = null, assistance: Int? = null, leftAdded: Int? = null, warmUp: Boolean = false, id: Long = 1) =
        HistorySet(HistorySource.V2, null, emptyList(), id, date, "07:30", 1, reps, left, "", null, added, assistance, weightLeftG = leftAdded, warmUp = warmUp)

    private fun volume(vararg sets: HistorySet, exercise: Exercise = pushUp, list: List<BodyWeight> = weights) =
        shareVolumes(exercise, sets.toList(), list)

    @Test fun `a rep carries the share of that day's body weight`() {
        val day = volume(set("2026-11-01", 10)).days.single()
        assertEquals(390f, day.volumeRight, 0.001f) // 10 reps x 65 % of 60 kg
        assertNull(day.volumeLeft)
        assertEquals(65, day.heaviestPercent)
    }

    @Test fun `added weight adds to the load and assistance takes from it`() {
        assertEquals(440f, volume(set("2026-11-01", 10, added = 5_000)).days.single().volumeRight, 0.001f)
        assertEquals(340f, volume(set("2026-11-01", 10, assistance = 5_000)).days.single().volumeRight, 0.001f)
        assertEquals(73, volume(set("2026-11-01", 10, added = 5_000)).days.single().heaviestPercent)
    }

    @Test fun `each day uses the weight that counted on it, so a heavier body is more volume`() {
        val days = volume(set("2026-11-01", 10), set("2026-12-20", 10)).days
        assertEquals(390f, days[0].volumeRight, 0.001f)
        assertEquals(422.5f, days[1].volumeRight, 0.001f) // 65 % of 65 kg
    }

    @Test fun `days before the first body weight are left out and counted, never drawn as a dip`() {
        val result = volume(set("2026-09-01", 10), set("2026-09-02", 10), set("2026-11-01", 10))
        assertEquals(listOf("2026-11-01"), result.days.map { it.date })
        assertEquals(2, result.daysWithoutBodyWeight)
    }

    @Test fun `with no body weight at all nothing is counted`() {
        val result = volume(set("2026-11-01", 10), list = emptyList())
        assertTrue(result.days.isEmpty())
        assertEquals(1, result.daysWithoutBodyWeight)
    }

    @Test fun `adding a past weight fixes past volume`() {
        val before = volume(set("2026-09-01", 10)).days
        val after = volume(set("2026-09-01", 10), list = listOf(BodyWeight(9, "2026-08-01", 80_000)) + weights).days
        assertTrue(before.isEmpty())
        assertEquals(520f, after.single().volumeRight, 0.001f) // 65 % of 80 kg
    }

    @Test fun `a one-sided day counts each side's reps at the full load`() {
        val day = volume(set("2026-11-01", reps = 8, left = 6, leftAdded = 5_000)).days.single()
        assertEquals(312f, day.volumeRight, 0.001f) // 8 x 39 kg
        assertEquals(264f, day.volumeLeft!!, 0.001f) // 6 x (39 + 5) kg
        assertEquals(73, day.heaviestPercent) // the heavier side
    }

    @Test fun `an exercise with no share counts nothing here`() {
        val result = volume(set("2026-11-01", 10), exercise = pushUp.copy(bodyWeightPercent = null))
        assertTrue(result.days.isEmpty())
        assertEquals(0, result.daysWithoutBodyWeight)
    }

    // The graph and statistics built on it.
    @Test fun `the volume chart counts a share without weight tracking, and a hold or no share gives none`() {
        val records = listOf(set("2026-11-01", 10))
        assertEquals(390f, prepareVolumeData(pushUp, records, null, DayOfWeek.MONDAY, weights).single().volumeRight, 0.001f)
        assertTrue(prepareVolumeData(pushUp.copy(type = "Isometric"), records, null, DayOfWeek.MONDAY, weights).isEmpty())
        assertTrue(prepareVolumeData(pushUp.copy(bodyWeightPercent = null), records, null, DayOfWeek.MONDAY, weights).isEmpty())
    }

    @Test fun `without a share volume is exactly what added weight gives, assistance not subtracted`() {
        val tracked = pushUp.copy(bodyWeightPercent = null, weightTrackingEnabled = true)
        val points = prepareVolumeData(tracked, listOf(set("2026-11-01", 10, added = 5_000, assistance = 3_000)), null, DayOfWeek.MONDAY, weights)
        assertEquals(50f, points.single().volumeRight, 0.001f) // 10 x 5 kg, as before
    }

    @Test fun `the statistics give the heaviest load per body weight and a daily volume`() {
        val records = listOf(set("2026-11-01", 10), set("2026-11-02", 10, added = 5_000))
        val stats = calculateStatistics(pushUp, records, null, DayOfWeek.MONDAY, weights)
        assertEquals(73, stats.heaviestLoadPercent)
        assertEquals(440f, stats.maxDailyVolume!!, 0.001f)
        assertEquals(415f, stats.avgDailyVolume!!, 0.001f)
    }

    @Test fun `no share, no heaviest load`() {
        val stats = calculateStatistics(pushUp.copy(bodyWeightPercent = null), listOf(set("2026-11-01", 10)), null, DayOfWeek.MONDAY, weights)
        assertNull(stats.heaviestLoadPercent)
    }

    @Test fun `the load chart and the not counted note read the same days`() {
        val records = listOf(set("2026-09-01", 10), set("2026-11-01", 10, added = 5_000))
        assertEquals(listOf(73f), prepareLoadPercentData(pushUp, records, null, DayOfWeek.MONDAY, weights).map { it.maxWeightKg })
        assertEquals(1, daysWithoutBodyWeight(pushUp, records, null, DayOfWeek.MONDAY, weights))
    }

    @Test fun `warm-ups stay out`() {
        val result = volume(set("2026-11-01", 10, warmUp = true))
        val records = listOf(set("2026-11-01", 10, warmUp = true), set("2026-11-01", 5))
        assertEquals(195f, prepareVolumeData(pushUp, records, null, DayOfWeek.MONDAY, weights).single().volumeRight, 0.001f)
        assertEquals(1, result.days.size) // shareVolumes trusts its caller to pass counted sets only
    }
}
