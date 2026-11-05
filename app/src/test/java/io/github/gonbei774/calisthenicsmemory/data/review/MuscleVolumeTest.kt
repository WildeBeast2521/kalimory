package io.github.gonbei774.calisthenicsmemory.data.review

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalRecord
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.MovementPattern
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import io.github.gonbei774.calisthenicsmemory.data.v2.IntervalHistoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Sets per muscle for the body map (ADR 0011). */
class MuscleVolumeTest {
    // Monday 9 to Sunday 15 March 2026.
    private val start = LocalDate.of(2026, 3, 9)
    private val end = start.plusDays(6)

    private fun set(date: String, exerciseId: Long, left: Int? = null, warmUp: Boolean = false, source: HistorySource = HistorySource.V2) = HistorySet(
        source, if (source == HistorySource.LEGACY) 1 else null, emptyList(), exerciseId, date, "07:30", 1, 10, left, "", null, null, null,
        warmUp = warmUp,
    )

    private fun interval(date: String) = IntervalHistoryItem(
        IntervalRecord(programName = "T", date = date, time = "18:00", workSeconds = 20, restSeconds = 10, rounds = 2, roundRestSeconds = 0,
            completedRounds = 2, completedExercisesInLastRound = 0, exercisesJson = """["A","B"]"""),
    )

    private fun exercise(id: Long, stepId: String?) = Exercise(id = id, name = "E$id", type = "Dynamic", catalogId = stepId)

    /** A training step (not mobility) that names at least one helping muscle not in its main list. */
    private val stepWithHelpers = Catalogue.chains.filter { it.pattern != MovementPattern.MOBILITY }.flatMap { it.steps }
        .first { (it.secondaryMuscles - it.primaryMuscles).isNotEmpty() }
    private val linked = exercise(1, stepWithHelpers.id)
    private val mobilityStep = Catalogue.chains.first { it.pattern == MovementPattern.MOBILITY }.steps.first()
    private val conditioningStep = Catalogue.chains.first { it.pattern == MovementPattern.CONDITIONING }.steps.first()

    private fun build(history: List<HistorySet>, exercises: List<Exercise>, intervals: List<IntervalHistoryItem> = emptyList()) =
        MuscleVolume.build(start, end, history, intervals, exercises)

    @Test fun `main muscles get a set and helping muscles help`() {
        val summary = build(listOf(set("2026-03-10", linked.id)), listOf(linked))
        stepWithHelpers.primaryMuscles.forEach { muscle -> assertEquals(muscle.name, 1, summary.muscles.single { it.muscle == muscle }.direct) }
        (stepWithHelpers.secondaryMuscles - stepWithHelpers.primaryMuscles).forEach { muscle ->
            val sets = summary.muscles.single { it.muscle == muscle }
            assertEquals(muscle.name, 0 to 1, sets.direct to sets.helping)
        }
        assertEquals(1, summary.mappedSets)
    }

    @Test fun `no set counts a muscle twice`() {
        Catalogue.chains.filter { it.pattern != MovementPattern.MOBILITY }.flatMap { it.steps }.forEach { step ->
            val summary = build(listOf(set("2026-03-10", 1)), listOf(exercise(1, step.id)))
            summary.muscles.forEach { assertTrue("${step.id} ${it.muscle}", it.direct + it.helping == 1) }
        }
    }

    @Test fun `helping sets weigh half`() {
        assertEquals(2.5, MuscleSets(Muscle.TRICEPS, 2, 1).weighted, 0.0)
    }

    @Test fun `mobility sets are counted apart and leave the map empty`() {
        val stretch = exercise(2, mobilityStep.id)
        val summary = build(listOf(set("2026-03-10", stretch.id)), listOf(stretch))
        assertEquals(1, summary.mobilitySets)
        assertTrue(summary.muscles.isEmpty())
    }

    @Test fun `conditioning sets reach the map`() {
        val burpee = exercise(3, conditioningStep.id)
        val summary = build(listOf(set("2026-03-10", burpee.id)), listOf(burpee))
        assertEquals(1, summary.mappedSets)
        assertTrue(summary.muscles.isNotEmpty())
    }

    @Test fun `unlinked, unknown and missing exercises count as other exercises`() {
        val unlinked = exercise(4, null)
        val unknown = exercise(5, "push.not_yet")
        val summary = build(listOf(set("2026-03-10", 4), set("2026-03-10", 5), set("2026-03-10", 99)), listOf(unlinked, unknown))
        assertEquals(3, summary.otherSets)
        assertTrue(summary.muscles.isEmpty())
    }

    @Test fun `interval work counts its work intervals and nothing on the map`() {
        val summary = build(emptyList(), emptyList(), listOf(interval("2026-03-11"), interval("2026-03-20")))
        assertEquals(4, summary.intervalSets)
        assertTrue(summary.muscles.isEmpty())
    }

    @Test fun `a one-sided set is one set, and legacy and v2 sets both count`() {
        val summary = build(listOf(set("2026-03-10", linked.id, left = 9), set("2026-03-10", linked.id, source = HistorySource.LEGACY)), listOf(linked))
        assertEquals(2, summary.mappedSets)
    }

    @Test fun `warm-ups are left out`() {
        val summary = build(listOf(set("2026-03-10", linked.id, warmUp = true), set("2026-03-10", linked.id)), listOf(linked))
        assertEquals(1, summary.mappedSets)
    }

    @Test fun `the first and last day count, the days around them and unreadable dates do not`() {
        val history = listOf("2026-03-08", "2026-03-09", "2026-03-15", "2026-03-16", "2026-3-10", "").map { set(it, linked.id) }
        assertEquals(2, build(history, listOf(linked)).totalSets)
    }

    @Test fun `for a week the total matches the weekly review's`() {
        val stretch = exercise(2, mobilityStep.id)
        val unlinked = exercise(4, null)
        val exercises = listOf(linked, stretch, unlinked)
        val history = listOf(
            set("2026-03-09", linked.id), set("2026-03-10", stretch.id), set("2026-03-11", unlinked.id),
            set("2026-03-12", linked.id, warmUp = true), set("2026-03-16", linked.id),
        )
        val intervals = listOf(interval("2026-03-13"))
        val map = MuscleVolume.build(start, end, history, intervals, exercises)
        val review = WeeklyReview.build(start, history, intervals, exercises)
        assertEquals(review.totalSets, map.totalSets)
        assertEquals(review.families.singleOrNull { it.family == MovementFamily.MOBILITY }?.sets ?: 0, map.mobilitySets)
        assertEquals(review.otherSets, map.otherSets)
        assertEquals(review.intervalSets, map.intervalSets)
    }

    @Test fun `muscles are ordered by weighted sets, ties in muscle order`() {
        val summary = build(List(3) { set("2026-03-10", linked.id) }, listOf(linked))
        val weights = summary.muscles.map { it.weighted }
        assertEquals(weights.sortedDescending(), weights)
        summary.muscles.zipWithNext().filter { (a, b) -> a.weighted == b.weighted }.forEach { (a, b) -> assertTrue(a.muscle.ordinal < b.muscle.ordinal) }
    }

    @Test fun `shades change at 5 and 10 a week`() {
        assertEquals(
            listOf(VolumeTier.NONE, VolumeTier.LOW, VolumeTier.LOW, VolumeTier.MID, VolumeTier.MID, VolumeTier.HIGH, VolumeTier.HIGH),
            listOf(0.0, 0.5, 4.5, 5.0, 9.5, 10.0, 30.0).map(MuscleVolume::tier),
        )
    }

    @Test fun `longer periods shade by sets a week`() {
        val summary = MuscleVolumeSummary(start, end, listOf(MuscleSets(Muscle.CHEST, 20, 0)), 20, 0, 0, 0)
        assertEquals(VolumeTier.LOW, MuscleVolume.tiers(summary, weeks = 30 / 7.0)[Muscle.CHEST])
        assertEquals(VolumeTier.HIGH, MuscleVolume.tiers(summary, weeks = 1.0)[Muscle.CHEST])
    }

    // Not worked

    @Test fun `the not worked view lists the 13 muscles training steps aim at`() {
        assertEquals(13, MuscleVolume.trainable.size)
        // Never named, or only ever as helping: never listed as not worked.
        listOf(Muscle.NECK, Muscle.TRAPS, Muscle.ABDUCTORS, Muscle.ADDUCTORS).forEach { assertTrue(it.name, it !in MuscleVolume.trainable) }
    }

    @Test fun `a muscle that only helped this period counts as worked`() {
        val summary = build(listOf(set("2026-03-10", linked.id)), listOf(linked))
        val notWorked = MuscleVolume.notWorked(summary)
        (stepWithHelpers.primaryMuscles + stepWithHelpers.secondaryMuscles).forEach { assertTrue(it.name, it !in notWorked) }
        assertEquals(notWorked.sortedBy { it.ordinal }, notWorked)
        assertEquals(MuscleVolume.trainable.size, MuscleVolume.notWorked(build(emptyList(), emptyList())).size)
    }
}
