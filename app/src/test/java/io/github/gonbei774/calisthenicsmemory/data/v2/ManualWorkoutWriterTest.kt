package io.github.gonbei774.calisthenicsmemory.data.v2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ManualWorkoutWriterTest {
    private val zone = ZoneId.of("Asia/Tokyo")

    private fun workout(
        sets: List<ManualSet>,
        kind: ExerciseKind = ExerciseKind.DYNAMIC,
        laterality: Laterality = Laterality.BILATERAL,
    ) = ManualWorkout(
        exerciseId = 5, exerciseName = "Pull-up", kind = kind, laterality = laterality, groupId = null, groupName = null,
        date = LocalDate.of(2025, 6, 1), time = LocalTime.of(18, 30), sets = sets, comment = "", savedAtWallMillis = 42,
    )

    @Test fun `the chosen minute starts the session and nothing else is timed`() {
        val rows = ManualWorkoutWriter.rows(workout(listOf(ManualSet(8, null, null, null, null))), zone)
        assertEquals(ZonedDateTime.of(2025, 6, 1, 18, 30, 0, 0, zone).toInstant().toEpochMilli(), rows.session.startedAtEpochMillis)
        assertNull(rows.session.endedAtEpochMillis)
        assertEquals(TimePrecision.MINUTE, rows.session.timePrecision)
        assertEquals(WorkoutSourceType.MANUAL, rows.session.sourceType)
        assertEquals(42L, rows.session.updatedAtEpochMillis)
        assertNull(rows.session.comment)
        assertNull(rows.sets.single().completedAtEpochMillis)
        assertEquals(TimePrecision.MINUTE, rows.sets.single().timePrecision)
    }

    @Test fun `every entered value is a completed set, zero included`() {
        val rows = ManualWorkoutWriter.rows(workout(listOf(ManualSet(8, null, 100, 2_000, null), ManualSet(0, null, null, null, 500))), zone)
        assertEquals(listOf(SetEntryStatus.COMPLETED, SetEntryStatus.COMPLETED), rows.sets.map { it.status })
        assertEquals(listOf(8, 0), rows.sets.map { it.repetitions })
        assertEquals(listOf(1, 2), rows.sets.map { it.setNumber })
        assertEquals(listOf(100, null), rows.sets.map { it.distanceCm })
        assertEquals(listOf(2_000, null), rows.sets.map { it.addedWeightGrams })
        assertEquals(listOf(null, 500), rows.sets.map { it.assistanceGrams })
    }

    @Test fun `unilateral sets get a left entry only when a left value was entered`() {
        val rows = ManualWorkoutWriter.rows(
            workout(listOf(ManualSet(9, 8, null, null, null), ManualSet(7, null, null, null, null)), laterality = Laterality.UNILATERAL),
            zone,
        )
        assertEquals(listOf(1 to BodySide.RIGHT, 1 to BodySide.LEFT, 2 to BodySide.RIGHT), rows.sets.map { it.setNumber to it.side })
        assertEquals(listOf(9, 8, 7), rows.sets.map { it.repetitions })
        assertEquals(listOf(0, 1, 2), rows.sets.map { it.orderIndex })
    }

    @Test fun `isometric values are whole seconds`() {
        val rows = ManualWorkoutWriter.rows(workout(listOf(ManualSet(30, null, null, null, null)), kind = ExerciseKind.ISOMETRIC), zone)
        assertEquals(30_000L, rows.sets.single().durationMillis)
        assertNull(rows.sets.single().repetitions)
    }

    @Test fun `negative values are refused`() {
        assertThrows(IllegalArgumentException::class.java) {
            ManualWorkoutWriter.rows(workout(listOf(ManualSet(3, -1, null, null, null)), laterality = Laterality.UNILATERAL), zone)
        }
    }

    @Test fun `seconds from the default time are dropped`() {
        val rows = ManualWorkoutWriter.rows(
            workout(listOf(ManualSet(1, null, null, null, null))).copy(time = LocalTime.of(18, 30, 15, 968_000_000)), zone,
        )
        assertEquals(ZonedDateTime.of(2025, 6, 1, 18, 30, 0, 0, zone).toInstant().toEpochMilli(), rows.session.startedAtEpochMillis)
    }

    @Test fun `several exercises become one session with occurrences in the order entered`() {
        val pullUps = workout(listOf(ManualSet(8, null, null, null, null), ManualSet(7, null, null, null, null)))
        val plank = workout(listOf(ManualSet(60, null, null, null, null)), kind = ExerciseKind.ISOMETRIC)
            .copy(exerciseId = 9, exerciseName = "Plank")
        val rows = ManualWorkoutWriter.rows(listOf(pullUps, plank), zone)
        assertEquals(ZonedDateTime.of(2025, 6, 1, 18, 30, 0, 0, zone).toInstant().toEpochMilli(), rows.session.startedAtEpochMillis)
        assertEquals(listOf("Pull-up", "Plank"), rows.exercises.map { it.first.exerciseNameSnapshot })
        assertEquals(listOf(0, 1), rows.exercises.map { it.first.orderIndex })
        assertEquals(listOf(8, 7), rows.exercises[0].second.map { it.repetitions })
        assertEquals(listOf(60_000L), rows.exercises[1].second.map { it.durationMillis })
    }

    @Test fun `entries of one workout must share its date and time`() {
        val a = workout(listOf(ManualSet(8, null, null, null, null)))
        val b = a.copy(time = LocalTime.of(19, 0))
        assertThrows(IllegalArgumentException::class.java) { ManualWorkoutWriter.rows(listOf(a, b), zone) }
    }
}
