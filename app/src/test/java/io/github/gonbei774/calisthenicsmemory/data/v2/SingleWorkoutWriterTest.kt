package io.github.gonbei774.calisthenicsmemory.data.v2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SingleWorkoutWriterTest {
    private fun set(number: Int, value: Int, side: BodySide = BodySide.BILATERAL, at: Long? = 1_000L * number) =
        SingleWorkoutSet(number, side, value, targetValue = 10, distanceCm = null, weightG = 2_500, assistanceG = null, completedAtWallMillis = at)

    private fun workout(
        sets: List<SingleWorkoutSet>,
        kind: ExerciseKind = ExerciseKind.DYNAMIC,
        laterality: Laterality = Laterality.BILATERAL,
        start: Long? = 500,
        comment: String = "Workout",
    ) = SingleWorkout(
        exerciseId = 7, exerciseName = "Push-up", kind = kind, laterality = laterality, groupId = 3, groupName = "Push",
        targetSets = sets.size, targetValue = 10, sets = sets, comment = comment,
        startedAtWallMillis = start, savedAtWallMillis = 9_000,
    )

    @Test fun `a done set is completed at its observed time and an undone one is skipped`() {
        val rows = SingleWorkoutWriter.rows(workout(listOf(set(1, 12), set(2, 0))))
        assertEquals(listOf(SetEntryStatus.COMPLETED, SetEntryStatus.SKIPPED), rows.sets.map { it.status })
        assertEquals(listOf(12, null), rows.sets.map { it.repetitions })
        assertEquals(listOf(1_000L, null), rows.sets.map { it.completedAtEpochMillis })
        assertEquals(listOf(TimePrecision.EXACT, TimePrecision.MINUTE), rows.sets.map { it.timePrecision })
        assertEquals(listOf(10, 10), rows.sets.map { it.targetRepetitions })
        assertEquals(listOf(2_500, 2_500), rows.sets.map { it.addedWeightGrams })
        assertEquals(listOf(0, 1), rows.sets.map { it.orderIndex })
    }

    @Test fun `the session spans the observed start to the save`() {
        val rows = SingleWorkoutWriter.rows(workout(listOf(set(1, 12))))
        assertEquals(500L, rows.session.startedAtEpochMillis)
        assertEquals(9_000L, rows.session.endedAtEpochMillis)
        assertEquals(TimePrecision.EXACT, rows.session.timePrecision)
        assertEquals(WorkoutSourceType.AD_HOC, rows.session.sourceType)
        assertEquals(WorkoutSessionStatus.COMPLETED, rows.session.status)
        assertEquals("Workout", rows.session.comment)
        assertEquals(listOf(7L, 3L), listOf(rows.exercise.exerciseId, rows.exercise.groupId))
        assertEquals(1, rows.exercise.targetSets)
        assertEquals("Push", rows.exercise.groupNameSnapshot)
    }

    @Test fun `an unobserved start falls back to the save time at minute precision`() {
        val rows = SingleWorkoutWriter.rows(workout(listOf(set(1, 12)), start = null, comment = " "))
        assertEquals(9_000L, rows.session.startedAtEpochMillis)
        assertEquals(TimePrecision.MINUTE, rows.session.timePrecision)
        assertNull(rows.session.comment)
    }

    @Test fun `isometric values are whole seconds`() {
        val rows = SingleWorkoutWriter.rows(workout(listOf(set(1, 45)), kind = ExerciseKind.ISOMETRIC))
        assertEquals(45_000L, rows.sets.single().durationMillis)
        assertNull(rows.sets.single().repetitions)
        assertEquals(10_000L, rows.sets.single().targetDurationMillis)
        assertEquals(10_000L, rows.exercise.targetDurationMillis)
        assertNull(rows.exercise.targetRepetitions)
    }

    @Test fun `unilateral sides stay separate and count as one set`() {
        val w = workout(
            listOf(set(1, 9, BodySide.RIGHT), set(1, 8, BodySide.LEFT), set(2, 7, BodySide.RIGHT), set(2, 0, BodySide.LEFT)),
            laterality = Laterality.UNILATERAL,
        )
        val rows = SingleWorkoutWriter.rows(w)
        assertEquals(listOf(BodySide.RIGHT, BodySide.LEFT, BodySide.RIGHT, BodySide.LEFT), rows.sets.map { it.side })
        assertEquals(listOf(SetEntryStatus.COMPLETED, SetEntryStatus.COMPLETED, SetEntryStatus.COMPLETED, SetEntryStatus.SKIPPED), rows.sets.map { it.status })
        assertEquals(2, w.completedSetCount)
    }

    @Test fun `negative values are refused`() {
        assertThrows(IllegalArgumentException::class.java) { SingleWorkoutWriter.rows(workout(listOf(set(1, -1)))) }
    }
}
