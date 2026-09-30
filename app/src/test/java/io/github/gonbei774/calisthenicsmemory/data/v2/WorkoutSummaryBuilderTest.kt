package io.github.gonbei774.calisthenicsmemory.data.v2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSummaryBuilderTest {

    private val start = 1_790_000_000_000L

    private fun session(type: WorkoutSourceType = WorkoutSourceType.AD_HOC, name: String? = null, minutes: Long? = 21, precision: TimePrecision = TimePrecision.EXACT) =
        WorkoutSessionEntity(
            id = 9, status = WorkoutSessionStatus.COMPLETED, sourceType = type, sourceNameSnapshot = name,
            startedAtEpochMillis = start, endedAtEpochMillis = minutes?.let { start + it * 60_000 },
            updatedAtEpochMillis = start, timePrecision = precision,
        )

    private fun exercise(id: Long, name: String, kind: ExerciseKind = ExerciseKind.DYNAMIC, laterality: Laterality = Laterality.BILATERAL) =
        SessionExerciseEntity(id = id * 10, workoutSessionId = 9, orderIndex = id.toInt(), exerciseId = id, exerciseNameSnapshot = name, exerciseKindSnapshot = kind, lateralitySnapshot = laterality)

    private var nextEntry = 100L
    private fun set(number: Int, reps: Int? = null, seconds: Int? = null, side: BodySide = BodySide.BILATERAL, status: SetEntryStatus = SetEntryStatus.COMPLETED, weight: Int? = null) =
        SetEntryEntity(
            id = nextEntry++, sessionExerciseId = 0, orderIndex = number, setNumber = number, status = status, side = side,
            repetitions = reps, durationMillis = seconds?.let { it * 1_000L }, addedWeightGrams = weight, timePrecision = TimePrecision.EXACT,
        )

    private fun history(exerciseId: Long, date: String, time: String, right: Int, left: Int? = null, setEntryIds: List<Long> = emptyList(), weight: Int? = null) =
        HistorySet(
            source = if (setEntryIds.isEmpty()) HistorySource.LEGACY else HistorySource.V2, legacyRecordId = null, setEntryIds = setEntryIds,
            exerciseId = exerciseId, date = date, time = time, setNumber = 1, valueRight = right, valueLeft = left, comment = "",
            distanceCm = null, weightG = weight, assistanceG = null,
        )

    @Test fun `totals split reps from hold time and count minutes`() {
        val summary = WorkoutSummaryBuilder.build(
            WorkoutSessionGraph(
                session(WorkoutSourceType.PROGRAM_TEMPLATE, "Upper body"),
                listOf(
                    exercise(1, "Push-up") to listOf(set(1, reps = 15), set(2, reps = 12), set(3, status = SetEntryStatus.SKIPPED)),
                    exercise(2, "L-sit", ExerciseKind.ISOMETRIC) to listOf(set(1, seconds = 20), set(2, seconds = 18)),
                ),
            ),
            history = emptyList(),
        )
        assertEquals("Upper body", summary.title)
        assertEquals(4, summary.completedSets)
        assertEquals(27, summary.totalRepetitions)
        assertEquals(38, summary.totalHoldSeconds)
        assertEquals(21, summary.durationMinutes)
        assertEquals(3, summary.exercises[0].plannedSets)
        assertEquals(2, summary.exercises[0].sets.size)
    }

    @Test fun `one-sided sets pair their sides`() {
        val summary = WorkoutSummaryBuilder.build(
            WorkoutSessionGraph(
                session(),
                listOf(exercise(3, "Pistol squat", laterality = Laterality.UNILATERAL) to listOf(
                    set(1, reps = 6, side = BodySide.RIGHT), set(1, reps = 5, side = BodySide.LEFT),
                    set(2, reps = 5, side = BodySide.RIGHT), set(2, reps = 5, side = BodySide.LEFT),
                )),
            ),
            history = emptyList(),
        )
        assertNull(summary.title)
        assertEquals(listOf(SetResult(6, 5), SetResult(5, 5)), summary.exercises[0].sets)
        assertEquals(21, summary.totalRepetitions)
    }

    @Test fun `last time is the latest earlier workout, not this one`() {
        val sets = listOf(set(1, reps = 15), set(2, reps = 14))
        val summary = WorkoutSummaryBuilder.build(
            WorkoutSessionGraph(session(), listOf(exercise(1, "Push-up") to sets)),
            history = listOf(
                history(1, "2026-09-20", "07:00", 10), history(1, "2026-09-20", "07:00", 10),
                history(1, "2026-09-27", "07:30", 12), history(1, "2026-09-27", "07:30", 11),
                // This workout, already in the history.
                history(1, "2026-09-30", "08:00", 15, setEntryIds = listOf(sets[0].id)),
                history(1, "2026-09-30", "08:00", 14, setEntryIds = listOf(sets[1].id)),
            ),
        )
        assertEquals(23, summary.exercises[0].lastTimeTotal)
    }

    @Test fun `first time has no last time and no best`() {
        val summary = WorkoutSummaryBuilder.build(
            WorkoutSessionGraph(session(), listOf(exercise(1, "Push-up") to listOf(set(1, reps = 15)))),
            history = emptyList(),
        )
        assertNull(summary.exercises[0].lastTimeTotal)
        assertTrue(summary.personalBests.isEmpty())
    }

    @Test fun `a higher set than ever before is a personal best`() {
        val summary = WorkoutSummaryBuilder.build(
            WorkoutSessionGraph(session(), listOf(exercise(1, "Push-up") to listOf(set(1, reps = 16), set(2, reps = 12)))),
            history = listOf(history(1, "2026-09-27", "07:30", 15), history(1, "2026-09-26", "07:30", 14)),
        )
        assertEquals(listOf(PersonalBest("Push-up", ExerciseKind.DYNAMIC, 16, 15)), summary.personalBests)
    }

    @Test fun `equal to the best is not a new best`() {
        val summary = WorkoutSummaryBuilder.build(
            WorkoutSessionGraph(session(), listOf(exercise(1, "Push-up") to listOf(set(1, reps = 15)))),
            history = listOf(history(1, "2026-09-27", "07:30", 15)),
        )
        assertTrue(summary.personalBests.isEmpty())
    }

    @Test fun `weighted sets never count as a best`() {
        val weightedNow = WorkoutSummaryBuilder.build(
            WorkoutSessionGraph(session(), listOf(exercise(1, "Pull-up") to listOf(set(1, reps = 20, weight = 5_000)))),
            history = listOf(history(1, "2026-09-27", "07:30", 8)),
        )
        assertTrue(weightedNow.personalBests.isEmpty())
        val onlyWeightedBefore = WorkoutSummaryBuilder.build(
            WorkoutSessionGraph(session(), listOf(exercise(1, "Pull-up") to listOf(set(1, reps = 9)))),
            history = listOf(history(1, "2026-09-27", "07:30", 8, weight = 10_000)),
        )
        assertTrue(onlyWeightedBefore.personalBests.isEmpty())
    }

    @Test fun `no duration without an exact start`() {
        val summary = WorkoutSummaryBuilder.build(
            WorkoutSessionGraph(session(precision = TimePrecision.MINUTE), emptyList()),
            history = emptyList(),
        )
        assertNull(summary.durationMinutes)
    }
}
