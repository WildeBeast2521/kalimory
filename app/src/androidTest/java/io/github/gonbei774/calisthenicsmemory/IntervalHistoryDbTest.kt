package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.CompatibilityHistory
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.v2.IntervalHistory
import io.github.gonbei774.calisthenicsmemory.data.v2.IntervalRun
import io.github.gonbei774.calisthenicsmemory.data.v2.IntervalRunExercise
import io.github.gonbei774.calisthenicsmemory.data.v2.IntervalWorkoutWriter
import io.github.gonbei774.calisthenicsmemory.data.v2.Laterality
import io.github.gonbei774.calisthenicsmemory.data.v2.TimePrecision
import io.github.gonbei774.calisthenicsmemory.data.v2.V2HistoryEditor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Interval workouts written to v2 read back as interval history, and stay out of set history. */
@RunWith(AndroidJUnit4::class)
class IntervalHistoryDbTest {
    private lateinit var database: AppDatabase
    private val zone = ZoneId.of("Asia/Kolkata")
    private val start = ZonedDateTime.of(2025, 4, 1, 6, 30, 0, 0, zone).toInstant().toEpochMilli()

    @Before
    fun setUp(): Unit = runBlocking {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).build()
        database.exerciseDao().insertExercise(Exercise(1, "Squat", "Dynamic"))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val run = IntervalRun(
        programId = 4, programName = "Tabata", workSeconds = 20, restSeconds = 10, rounds = 2, roundRestSeconds = 30,
        exercises = listOf(
            IntervalRunExercise(1, "Squat", ExerciseKind.DYNAMIC, Laterality.BILATERAL, null, null),
            // Deleted since the start: only its snapshot remains.
            IntervalRunExercise(null, "Burpee", ExerciseKind.DYNAMIC, Laterality.BILATERAL, null, null),
        ),
        completedRounds = 1, completedExercisesInLastRound = 1, comment = "windy",
        startedAtWallMillis = start, savedAtWallMillis = start + 180_000,
    )

    private suspend fun intervalHistory() =
        IntervalHistory.merge(emptyList(), database.workoutSessionDao().observeIntervalSessionRows().first(), zone)

    @Test
    fun writtenRunReadsBackLikeALegacyRecord() = runBlocking {
        val sessionId = IntervalWorkoutWriter.write(database, run)
        val item = intervalHistory().single()
        assertEquals(sessionId, item.v2SessionId)
        with(item.record) {
            assertEquals(listOf("Tabata", "2025-04-01", "06:30"), listOf(programName, date, time))
            assertEquals(listOf(20, 10, 2, 30, 1, 1), listOf(workSeconds, restSeconds, rounds, roundRestSeconds, completedRounds, completedExercisesInLastRound))
            assertEquals("[\"Squat\",\"Burpee\"]", exercisesJson)
            assertEquals("windy", comment)
        }
        // Interval mode records no reps or time, so nothing reaches the per-set history or the day check.
        assertEquals(emptyList<Any>(), CompatibilityHistory.read(database, zone))
        assertFalse(CompatibilityHistory.hasSetOn(database, 1, LocalDate.of(2025, 4, 1), zone))
    }

    @Test
    fun editAndDeleteActOnTheWholeWorkout() = runBlocking {
        val sessionId = IntervalWorkoutWriter.write(database, run)
        V2HistoryEditor.updateSession(database, sessionId, "2025-04-02", "07:00", "moved", zone, nowEpochMillis = 1)
        val session = database.workoutSessionDao().session(sessionId)!!
        assertEquals(TimePrecision.MINUTE, session.timePrecision)
        assertEquals(listOf("2025-04-02", "07:00", "moved"), intervalHistory().single().record.let { listOf(it.date, it.time, it.comment) })

        V2HistoryEditor.deleteSession(database, sessionId)
        assertNull(database.workoutSessionDao().session(sessionId))
        assertEquals(emptyList<Any>(), intervalHistory())
    }
}
