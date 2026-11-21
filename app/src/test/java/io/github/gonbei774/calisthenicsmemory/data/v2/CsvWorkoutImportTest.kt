package io.github.gonbei774.calisthenicsmemory.data.v2

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

/** Records CSV rows become workouts in the current model, grouped as the legacy conversion groups them. */
class CsvWorkoutImportTest {
    private val zone = ZoneId.of("UTC")
    private val push = Exercise(id = 1, name = "Push-up", type = "Dynamic", laterality = "Bilateral")
    private val plank = Exercise(id = 2, name = "Plank", type = "Isometric", laterality = "Bilateral")
    private val lunge = Exercise(id = 3, name = "Lunge", type = "Dynamic", laterality = "Unilateral")

    private fun row(
        line: Int, exercise: Exercise, set: Int, value: Int, left: Int? = null, date: String = "2026-03-09", time: String = "07:30",
        comment: String = "", warmUp: Boolean = false, effort: Int? = null,
    ) = CsvWorkoutImport.Row(line, exercise, date, time, set, value, left, comment, null, null, null, warmUp, effort)

    private fun plan(rows: List<CsvWorkoutImport.Row>, keys: Set<List<Any>> = emptySet(), minutes: Set<List<Any>> = emptySet()) =
        CsvWorkoutImport.plan(rows, emptyList(), keys, minutes, zone, nowEpochMillis = 1_000L)

    @Test fun `rows with one date, time and comment form one workout`() {
        val p = plan(listOf(row(2, push, 1, 10), row(3, push, 2, 9), row(4, plank, 1, 30)))
        assertEquals(1, p.sessions.size)
        assertEquals(listOf(1L, 2L), p.sessions[0].exercises.map { it.exercise.exerciseId })
        assertEquals(WorkoutSourceType.LEGACY_IMPORT, p.sessions[0].session.sourceType)
        assertEquals(TimePrecision.MINUTE, p.sessions[0].session.timePrecision)
        assertEquals(3, p.added)
    }

    @Test fun `another minute or comment is another workout, in order`() {
        val p = plan(listOf(row(2, push, 1, 10, time = "09:00"), row(3, push, 1, 8, time = "07:30"), row(4, plank, 1, 30, comment = "evening")))
        assertEquals(3, p.sessions.size)
        // 07:30 without a comment, 07:30 "evening", then 09:00: by start, then by first line.
        assertEquals(listOf(null, "evening", null), p.sessions.map { it.session.comment })
        assertEquals(p.sessions.map { it.session.startedAtEpochMillis }.sorted(), p.sessions.map { it.session.startedAtEpochMillis })
    }

    @Test fun `exercises keep file order and sets their number`() {
        val p = plan(listOf(row(2, plank, 1, 30), row(3, push, 2, 9), row(4, push, 1, 10)))
        val ex = p.sessions.single().exercises
        assertEquals(listOf(2L, 1L), ex.map { it.exercise.exerciseId })
        assertEquals(listOf(1, 2), ex[1].sets.map { it.setNumber })
        assertEquals(listOf(0, 1), ex.map { it.exercise.orderIndex })
    }

    @Test fun `a left value gives a right and a left entry, even for a two-sided exercise`() {
        val p = plan(listOf(row(2, push, 1, 10, left = 8)))
        assertEquals(listOf(BodySide.RIGHT, BodySide.LEFT), p.sessions.single().exercises.single().sets.map { it.side })
        assertEquals(1, p.added)
    }

    @Test fun `a one-sided exercise with no left value is the right side`() {
        val p = plan(listOf(row(2, lunge, 1, 8)))
        assertEquals(listOf(BodySide.RIGHT), p.sessions.single().exercises.single().sets.map { it.side })
    }

    @Test fun `a hold is stored as a duration and reps as repetitions`() {
        val p = plan(listOf(row(2, plank, 1, 30), row(3, push, 1, 10)))
        val sets = p.sessions.single().exercises.flatMap { it.sets }
        assertEquals(30_000L, sets.first { it.durationMillis != null }.durationMillis)
        assertEquals(10, sets.first { it.repetitions != null }.repetitions)
        assertNull(sets.first { it.repetitions != null }.durationMillis)
    }

    @Test fun `a set already in history, or repeated in the file, is skipped`() {
        val key = listOf<Any>(1L, "2026-03-09", "07:30", 1)
        val p = plan(listOf(row(2, push, 1, 10), row(3, push, 2, 9), row(4, push, 2, 9)), keys = setOf(key))
        assertEquals(listOf(2, 4), p.skipped.map { it.line })
        assertEquals(2, p.skipped.size)
        assertEquals(1, p.added)
    }

    @Test fun `an exercise with sets at that minute stays an older-format record`() {
        val minute = listOf<Any>(1L, "2026-03-09", "07:30")
        val p = plan(listOf(row(2, push, 3, 10)), minutes = setOf(minute))
        assertEquals(0, p.sessions.size)
        assertEquals(1, p.legacy.size)
    }

    @Test fun `marks on such a set are refused, not dropped`() {
        val minute = listOf<Any>(1L, "2026-03-09", "07:30")
        val p = plan(listOf(row(2, push, 3, 10, warmUp = true)), minutes = setOf(minute))
        assertEquals(0, p.legacy.size)
        assertEquals(1, p.refused.size)
    }

    @Test fun `the same exercise in two workouts at one minute does not restart set numbers`() {
        // Set 2 of workout "b" would be shown as set 1 in a new workout of its own, clashing with set 1 of "a".
        val p = plan(listOf(row(2, push, 1, 10, comment = "a"), row(3, push, 2, 9, comment = "b")))
        assertEquals(1, p.sessions.size)
        assertEquals(listOf(2), p.legacy.map { it.setNumber })
        // The same key twice is just a repeated row.
        val repeated = plan(listOf(row(2, push, 1, 10, comment = "a"), row(3, push, 1, 9, comment = "b")))
        assertEquals(1, repeated.skipped.size)
    }

    @Test fun `warm-up marks and effort go to the set entries`() {
        val p = plan(listOf(row(2, push, 1, 10, warmUp = true, effort = 3)))
        val set = p.sessions.single().exercises.single().sets.single()
        assertEquals(true, set.isWarmUp)
        assertEquals(3, set.effortLeft)
    }
}
