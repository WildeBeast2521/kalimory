package io.github.gonbei774.calisthenicsmemory.data.review

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import io.github.gonbei774.calisthenicsmemory.data.catalogue.MuscleGroup
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** Sets per week or month for each muscle and group (#274, ADR 0015). */
class MuscleTrendTest {
    private val today = LocalDate.of(2026, 11, 25) // a Wednesday

    private fun set(date: LocalDate, exerciseId: Long, warmUp: Boolean = false) = HistorySet(
        HistorySource.V2, null, emptyList(), exerciseId, date.toString(), "07:30", 1, 10, null, "", null, null, null, warmUp = warmUp,
    )

    private fun own(id: Long, primary: Set<Muscle> = emptySet(), helping: Set<Muscle> = emptySet()) =
        Exercise(id = id, name = "Own$id", type = "Dynamic", primaryMuscles = primary, secondaryMuscles = helping)

    private val pullUp = Catalogue.step("pull.full")!!

    @Test fun `weekly starts run from the first day of the week, the first complete and the last holding today`() {
        val starts = MuscleVolume.trendStarts(TrendSpan.MONTH, today, DayOfWeek.MONDAY, null)
        assertTrue(starts.all { it.dayOfWeek == DayOfWeek.MONDAY })
        assertEquals(LocalDate.of(2026, 11, 23), starts.last())
        assertTrue(starts.first() <= today.minusDays(29))
        assertEquals(starts.size, starts.distinct().size)
        // The first day of the week is the person's own.
        assertTrue(MuscleVolume.trendStarts(TrendSpan.MONTH, today, DayOfWeek.SUNDAY, null).all { it.dayOfWeek == DayOfWeek.SUNDAY })
    }

    @Test fun `monthly starts are calendar months, a year back or from the oldest set`() {
        val year = MuscleVolume.trendStarts(TrendSpan.YEAR, today, DayOfWeek.MONDAY, null)
        assertTrue(year.all { it.dayOfMonth == 1 })
        assertEquals(LocalDate.of(2026, 11, 1), year.last())
        assertEquals(LocalDate.of(2025, 11, 1), year.first())
        val all = MuscleVolume.trendStarts(TrendSpan.ALL, today, DayOfWeek.MONDAY, LocalDate.of(2024, 3, 17))
        assertEquals(LocalDate.of(2024, 3, 1), all.first())
        assertEquals(LocalDate.of(2026, 11, 1), all.last())
        assertEquals(listOf(LocalDate.of(2026, 11, 1)), MuscleVolume.trendStarts(TrendSpan.ALL, today, DayOfWeek.MONDAY, null))
    }

    @Test fun `a pull-up counts the back once, as working, and the arms as helping`() {
        val exercise = Exercise(id = 1, name = "Pull-up", type = "Dynamic", catalogId = pullUp.id)
        val starts = listOf(LocalDate.of(2026, 11, 23))
        val bucket = MuscleVolume.trend(starts, today, listOf(set(LocalDate.of(2026, 11, 24), 1)), listOf(exercise)).single()
        assertEquals(1, bucket.groups.getValue(MuscleGroup.BACK).direct)
        assertEquals(0, bucket.groups.getValue(MuscleGroup.BACK).helping)
        // Whatever the step names, no group is counted twice for one set.
        assertTrue(bucket.groups.values.all { it.direct + it.helping == 1 })
        pullUp.primaryMuscles.forEach { assertEquals(1, bucket.muscles.getValue(it).direct) }
    }

    @Test fun `a muscle's group keeps the full set when a helping muscle of the same group is also named`() {
        val exercise = own(2, primary = setOf(Muscle.LATS), helping = setOf(Muscle.TRAPS, Muscle.BICEPS))
        val bucket = MuscleVolume.trend(listOf(LocalDate.of(2026, 11, 23)), today, listOf(set(LocalDate.of(2026, 11, 23), 2)), listOf(exercise)).single()
        assertEquals(SetCounts(1, 0), bucket.groups.getValue(MuscleGroup.BACK))
        assertEquals(SetCounts(0, 1), bucket.groups.getValue(MuscleGroup.ARMS))
    }

    @Test fun `warm-ups, own exercises with no muscles and sets outside the buckets are left out`() {
        val curl = own(3, primary = setOf(Muscle.BICEPS))
        val history = listOf(
            set(LocalDate.of(2026, 11, 24), 3), set(LocalDate.of(2026, 11, 24), 3, warmUp = true),
            set(LocalDate.of(2026, 11, 24), 4), // exercise with no muscles chosen
            set(LocalDate.of(2026, 11, 22), 3), // the day before the first bucket
            set(LocalDate.of(2026, 11, 26), 3), // after today
        )
        val bucket = MuscleVolume.trend(listOf(LocalDate.of(2026, 11, 23)), today, history, listOf(curl, own(4))).single()
        assertEquals(SetCounts(1, 0), bucket.muscles.getValue(Muscle.BICEPS))
        assertEquals(1, bucket.muscles.size)
    }

    @Test fun `an empty week is a zero bucket, not a gap`() {
        val buckets = MuscleVolume.trend(listOf(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 23)), today, emptyList(), emptyList())
        assertEquals(3, buckets.size)
        assertTrue(buckets.all { it.muscles.isEmpty() && it.groups.isEmpty() })
    }

    @Test fun `a bucket shorter than a week is a week for its rate, a month is its length in weeks`() {
        val buckets = MuscleVolume.trend(listOf(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1)), today, emptyList(), emptyList())
        assertEquals(31 / 7.0, buckets[0].weeks, 1e-9)
        assertEquals(7 / 7.0, MuscleVolume.trend(listOf(LocalDate.of(2026, 11, 23)), LocalDate.of(2026, 11, 24), emptyList(), emptyList()).single().weeks, 1e-9)
        assertEquals(25 / 7.0, buckets[1].weeks, 1e-9)
    }

    // Three years of made-up training, own exercises included: every bucket must equal what build() says for its dates.
    @Test fun `each bucket matches build for the same dates over three years`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Pull-up", type = "Dynamic", catalogId = pullUp.id),
            Exercise(id = 2, name = "Push-up", type = "Dynamic", catalogId = Catalogue.chains.flatMap { it.steps }.first { Muscle.CHEST in it.primaryMuscles }.id),
            own(3, primary = setOf(Muscle.BICEPS), helping = setOf(Muscle.FOREARMS)),
            own(4, helping = setOf(Muscle.NECK)),
            own(5),
        )
        val history = (0 until 1095).flatMap { d ->
            val day = today.minusDays(d.toLong())
            (1..3).map { n -> set(day, ((d * 7 + n) % 5 + 1).toLong(), warmUp = (d + n) % 11 == 0) }
        }
        val starts = MuscleVolume.trendStarts(TrendSpan.ALL, today, DayOfWeek.MONDAY, MuscleVolume.earliest(history))
        val buckets = MuscleVolume.trend(starts, today, history, exercises)
        assertEquals(starts.size, buckets.size)
        buckets.forEach { b ->
            val summary = MuscleVolume.build(b.start, b.endInclusive, history, emptyList(), exercises)
            summary.muscles.forEach { assertEquals("${b.start} ${it.muscle}", SetCounts(it.direct, it.helping), b.muscles[it.muscle]) }
            assertEquals(summary.muscles.size, b.muscles.size)
        }
    }
}
