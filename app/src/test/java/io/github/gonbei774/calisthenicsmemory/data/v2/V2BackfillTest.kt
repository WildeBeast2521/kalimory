package io.github.gonbei774.calisthenicsmemory.data.v2

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class V2BackfillTest {
    private val tokyo = ZoneId.of("Asia/Tokyo")
    private val groups = listOf(ExerciseGroup(1, "Push"))
    private val pushUp = Exercise(1, "Push-up", "Dynamic", "Push")
    private val plank = Exercise(2, "Plank", "Isometric")
    private val lunge = Exercise(3, "Lunge", "Dynamic", "Legs", laterality = "Unilateral")
    private val exercises = listOf(pushUp, plank, lunge)

    private fun record(id: Long, exercise: Long, value: Int, set: Int, date: String = "2025-01-05", time: String = "07:30",
                       comment: String = "【Program】Morning", left: Int? = null, weightG: Int? = null) =
        TrainingRecord(id, exercise, value, left, set, date, time, comment, weightG = weightG)

    private fun plan(records: List<TrainingRecord>, done: Set<Long> = emptySet()) =
        V2Backfill.plan(exercises, groups, records, done, tokyo, nowEpochMillis = 9_999)

    @Test fun `one legacy save becomes one session with its exercises in recorded order`() {
        val records = listOf(
            record(10, exercise = 1, value = 12, set = 1),
            record(11, exercise = 2, value = 45, set = 1),
            record(12, exercise = 1, value = 10, set = 2),
        )
        val p = plan(records)
        val session = p.sessions.single()
        assertEquals(WorkoutSourceType.LEGACY_IMPORT, session.session.sourceType)
        assertEquals(WorkoutSessionStatus.COMPLETED, session.session.status)
        assertEquals("【Program】Morning", session.session.comment)
        assertEquals(listOf("Push-up", "Plank"), session.exercises.map { it.exercise.exerciseNameSnapshot })

        val push = session.exercises[0]
        assertEquals(1L, push.exercise.groupId)
        assertEquals("Push", push.exercise.groupNameSnapshot)
        assertEquals(listOf(12, 10), push.sets.map { it.repetitions })
        assertEquals(listOf(null, null), push.sets.map { it.durationMillis })
        assertEquals(listOf(10L, 12L), push.sets.map { it.legacyTrainingRecordId })
        assertEquals(listOf(0, 1), push.sets.map { it.orderIndex })

        val hold = session.exercises[1].sets.single()
        assertEquals(45_000L, hold.durationMillis)
        assertNull(hold.repetitions)
    }

    @Test fun `different minutes or comments are different sessions`() {
        val records = listOf(
            record(1, 1, 10, 1, time = "07:30"),
            record(2, 1, 10, 1, time = "07:31"),
            record(3, 1, 10, 1, time = "07:31", comment = "Workout"),
        )
        assertEquals(3, plan(records).sessions.size)
    }

    @Test fun `times are the recorded minute in the given zone, marked MINUTE, with nothing invented`() {
        val p = plan(listOf(record(1, 1, 10, 1, date = "2025-03-09", time = "23:59")))
        val session = p.sessions.single().session
        assertEquals(ZonedDateTime.of(2025, 3, 9, 23, 59, 0, 0, tokyo).toInstant().toEpochMilli(), session.startedAtEpochMillis)
        assertEquals(TimePrecision.MINUTE, session.timePrecision)
        assertNull(session.endedAtEpochMillis)
        assertEquals(9_999L, session.updatedAtEpochMillis)
        val set = p.sessions.single().exercises.single().sets.single()
        assertNull(set.startedAtEpochMillis)
        assertNull(set.completedAtEpochMillis)
        assertEquals(TimePrecision.MINUTE, set.timePrecision)
    }

    @Test fun `a record with a left value becomes right and left entries sharing its load`() {
        val p = plan(listOf(record(1, exercise = 3, value = 8, set = 1, left = 7, weightG = 4_000)))
        val sets = p.sessions.single().exercises.single().sets
        assertEquals(listOf(BodySide.RIGHT, BodySide.LEFT), sets.map { it.side })
        assertEquals(listOf(8, 7), sets.map { it.repetitions })
        assertEquals(listOf(4_000, 4_000), sets.map { it.addedWeightGrams })
        assertEquals(listOf(1L, 1L), sets.map { it.legacyTrainingRecordId })
    }

    @Test fun `records that cannot be converted exactly stay on the legacy path with a reason`() {
        val odd = Exercise(4, "Odd", "Cardio")
        val sideways = Exercise(5, "Side", "Dynamic", laterality = "Both")
        val records = listOf(
            record(1, 1, 10, 1, date = "2025-13-01"),
            record(2, 1, 10, 1, time = "7:05"),
            record(3, 1, 10, 1, date = ""),
            record(4, 4, 10, 1),
            record(5, 5, 10, 1),
            record(6, 1, -1, 1),
            record(7, 1, 10, 1, weightG = -5),
            record(8, 3, 10, 1),
            record(9, 99, 10, 1),
            record(10, 1, 10, 1),
        )
        val p = V2Backfill.plan(exercises + odd + sideways, groups, records, emptySet(), tokyo, 0)
        assertEquals(
            mapOf(
                LegacyAnomalyReason.UNPARSEABLE_DATE_TIME to listOf(1L, 2L, 3L),
                LegacyAnomalyReason.UNKNOWN_EXERCISE_TYPE to listOf(4L),
                LegacyAnomalyReason.UNKNOWN_LATERALITY to listOf(5L),
                LegacyAnomalyReason.NEGATIVE_VALUE to listOf(6L, 7L),
                LegacyAnomalyReason.AMBIGUOUS_SIDE to listOf(8L),
                LegacyAnomalyReason.MISSING_EXERCISE to listOf(9L),
            ),
            p.report.anomalies.groupBy({ it.reason }, { it.trainingRecordId }),
        )
        assertEquals(listOf(10L), p.sessions.flatMap { s -> s.exercises.flatMap { e -> e.sets.map { it.legacyTrainingRecordId } } })
    }

    @Test fun `already converted records are skipped and every record is counted once`() {
        val records = (1L..6L).map { record(it, 1, 10, it.toInt(), time = if (it <= 3) "07:30" else "08:00") } +
            record(7, 3, 10, 1)
        val p = plan(records, done = setOf(1L, 2L, 3L))
        val r = p.report
        assertEquals(7, r.legacyRecords)
        assertEquals(3, r.alreadyConvertedRecords)
        assertEquals(3, r.convertedRecords)
        assertEquals(1, r.anomalies.size)
        assertEquals(r.legacyRecords, r.convertedRecords + r.alreadyConvertedRecords + r.anomalies.size)
        assertEquals(1, r.sessionsCreated)
        assertEquals(3, r.setEntriesCreated)
        assertTrue(p.sessions.single().exercises.single().sets.all { it.legacyTrainingRecordId!! in 4L..6L })
    }
}
