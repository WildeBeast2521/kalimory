package io.github.gonbei774.calisthenicsmemory.data.v2

import io.github.gonbei774.calisthenicsmemory.data.IntervalRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class IntervalWorkoutTest {
    private val zone = ZoneId.of("Europe/Paris")
    private val start = ZonedDateTime.of(2025, 5, 4, 7, 15, 30, 0, zone).toInstant().toEpochMilli()

    private fun exercise(name: String) = IntervalRunExercise(null, name, ExerciseKind.DYNAMIC, Laterality.BILATERAL, null, null)

    private fun run(completedRounds: Int, lastRound: Int) = IntervalRun(
        programId = 4, programName = "Tabata", workSeconds = 20, restSeconds = 10, rounds = 3, roundRestSeconds = 60,
        exercises = listOf(exercise("Squat"), exercise("Push-up")),
        completedRounds = completedRounds, completedExercisesInLastRound = lastRound, comment = "",
        startedAtWallMillis = start, savedAtWallMillis = start + 300_000,
    )

    /** What the history reader makes of a run written by the writer. */
    private fun readBack(run: IntervalRun): IntervalHistoryItem {
        val rows = IntervalWorkoutWriter.rows(run)
        val joined = rows.exercises.flatMap { (exercise, sets) ->
            val base = IntervalSessionRow(
                7, rows.session.sourceNameSnapshot, rows.session.startedAtEpochMillis, rows.session.comment,
                rows.session.intervalWorkSeconds, rows.session.intervalRestSeconds, rows.session.intervalRounds,
                rows.session.intervalRoundRestSeconds, exercise.orderIndex, exercise.exerciseNameSnapshot, null, null,
            )
            if (sets.isEmpty()) listOf(base) else sets.map { base.copy(roundNumber = it.roundNumber, status = it.status) }
        }
        return IntervalHistory.merge(emptyList(), joined, zone).single()
    }

    @Test fun `a stopped run keeps its full rounds and the exercises done in the next`() {
        assertEquals(listOf(1 to 0, 1 to 1, 2 to 0), run(1, 1).completedSlots)
        val record = readBack(run(1, 1)).record
        assertEquals(1 to 1, record.completedRounds to record.completedExercisesInLastRound)
    }

    @Test fun `a full run reads back as the legacy full record`() {
        assertEquals(6, run(3, 2).completedSlots.size)
        val record = readBack(run(3, 2)).record
        assertEquals(3 to 2, record.completedRounds to record.completedExercisesInLastRound)
    }

    @Test fun `a run stopped before any work interval reads back as nothing done`() {
        val item = readBack(run(0, 0))
        assertEquals(0 to 0, item.record.completedRounds to item.record.completedExercisesInLastRound)
        assertEquals("[\"Squat\",\"Push-up\"]", item.record.exercisesJson)
    }

    @Test fun `the session keeps the settings and the sets hold no invented metric or time`() {
        val rows = IntervalWorkoutWriter.rows(run(1, 1))
        assertEquals(WorkoutSourceType.INTERVAL_TEMPLATE, rows.session.sourceType)
        assertEquals(listOf(20, 10, 3, 60), listOf(rows.session.intervalWorkSeconds, rows.session.intervalRestSeconds, rows.session.intervalRounds, rows.session.intervalRoundRestSeconds))
        val sets = rows.exercises.flatMap { it.second }
        assertEquals(listOf(1, 2, 1), sets.map { it.roundNumber })
        sets.forEach {
            assertNull(it.repetitions); assertNull(it.durationMillis); assertNull(it.completedAtEpochMillis)
            assertEquals(20_000L, it.targetDurationMillis)
        }
    }

    @Test fun `the record shows the start in the given zone and the session's settings`() {
        val item = readBack(run(1, 1).copy(comment = "hard"))
        assertEquals(7L, item.v2SessionId)
        assertEquals(
            IntervalRecord(0, "Tabata", "2025-05-04", "07:15", 20, 10, 3, 60, 1, 1, "[\"Squat\",\"Push-up\"]", "hard"),
            item.record,
        )
    }

    @Test fun `legacy records pass through with no session`() {
        val legacy = IntervalRecord(3, "Old", "2024-01-01", "06:00", 30, 15, 4, 0, 4, 2, "[\"Plank\"]")
        assertEquals(listOf(IntervalHistoryItem(legacy)), IntervalHistory.merge(listOf(legacy), emptyList(), zone))
    }
}
