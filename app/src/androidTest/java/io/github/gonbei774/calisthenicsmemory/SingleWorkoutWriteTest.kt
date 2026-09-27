package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.BodySide
import io.github.gonbei774.calisthenicsmemory.data.v2.CompatibilityHistory
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import io.github.gonbei774.calisthenicsmemory.data.v2.Laterality
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.SingleWorkout
import io.github.gonbei774.calisthenicsmemory.data.v2.SingleWorkoutSet
import io.github.gonbei774.calisthenicsmemory.data.v2.SingleWorkoutWriter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId
import java.time.LocalTime
import java.time.LocalDate
import io.github.gonbei774.calisthenicsmemory.data.v2.ManualWorkoutWriter
import io.github.gonbei774.calisthenicsmemory.data.v2.ManualWorkout
import io.github.gonbei774.calisthenicsmemory.data.v2.ManualSet
import java.time.ZonedDateTime

/** A finished single workout is written as one v2 session and shows in history like a legacy save. */
@RunWith(AndroidJUnit4::class)
class SingleWorkoutWriteTest {
    private lateinit var database: AppDatabase
    private val zone = ZoneId.of("Europe/Berlin")
    private val start = ZonedDateTime.of(2025, 3, 30, 1, 59, 0, 0, zone).toInstant().toEpochMilli()

    @Before
    fun setUp(): Unit = runBlocking {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).build()
        database.exerciseDao().insertExercise(Exercise(1, "Lunge", "Dynamic", laterality = "Unilateral"))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun set(number: Int, side: BodySide, value: Int, at: Long?) =
        SingleWorkoutSet(number, side, value, 10, null, 5_000, null, at)

    private fun workout(sets: List<SingleWorkoutSet>) = SingleWorkout(
        exerciseId = 1, exerciseName = "Lunge", kind = ExerciseKind.DYNAMIC, laterality = Laterality.UNILATERAL,
        groupId = null, groupName = null, targetSets = 3, targetValue = 10, sets = sets, comment = "Workout",
        startedAtWallMillis = start, savedAtWallMillis = start + 900_000,
    )

    @Test
    fun oneSessionWithEverySetAndHistoryShowsTheDoneOnes() = runBlocking {
        val sessionId = SingleWorkoutWriter.write(
            database,
            workout(
                listOf(
                    set(1, BodySide.RIGHT, 9, start + 60_000), set(1, BodySide.LEFT, 8, start + 120_000),
                    set(2, BodySide.RIGHT, 7, start + 240_000), set(2, BodySide.LEFT, 0, null),
                    set(3, BodySide.RIGHT, 0, null), set(3, BodySide.LEFT, 0, null),
                )
            ),
        )!!
        val graph = database.workoutSessionDao().sessionGraph(sessionId)!!
        val sets = graph.exercises.single().second
        assertEquals(6, sets.size)
        assertEquals(3, sets.count { it.status == SetEntryStatus.COMPLETED })

        // Shown like the legacy save: one row per set with a value, the left side paired in.
        // 01:59 on the night Berlin moves to summer time is still 01:59 local.
        val history = CompatibilityHistory.read(database, zone)
        assertEquals(listOf(HistorySource.V2, HistorySource.V2), history.map { it.source })
        assertEquals(listOf(1 to (9 to 8), 2 to (7 to null)), history.map { it.setNumber to (it.valueRight to it.valueLeft) })
        assertEquals(setOf("2025-03-30" to "01:59"), history.map { it.date to it.time }.toSet())
        assertEquals(setOf("Workout"), history.map { it.comment }.toSet())
        assertEquals(setOf(5_000), history.map { it.weightG }.toSet())
    }

    @Test
    fun nothingIsWrittenWhenNoSetWasDone() = runBlocking {
        assertNull(SingleWorkoutWriter.write(database, workout(listOf(set(1, BodySide.RIGHT, 0, null)))))
        assertEquals(emptyList<Any>(), database.workoutSessionDao().sessionsNewestFirst())
    }

    @Test
    fun aManualEntryShowsAtTheChosenMinuteLikeALegacyRecord() = runBlocking {
        val entry = ManualWorkout(
            exerciseId = 1, exerciseName = "Lunge", kind = ExerciseKind.DYNAMIC, laterality = Laterality.UNILATERAL,
            groupId = null, groupName = null, date = LocalDate.of(2025, 1, 2), time = LocalTime.of(7, 5),
            sets = listOf(ManualSet(9, 8, null, null, null), ManualSet(0, null, null, null, null)), comment = "by hand",
            savedAtWallMillis = start,
        )
        ManualWorkoutWriter.write(database, entry, zone)
        val history = CompatibilityHistory.read(database, zone)
        assertEquals(
            listOf(Triple("2025-01-02", "07:05", 1 to (9 to 8)), Triple("2025-01-02", "07:05", 2 to (0 to null))),
            history.map { Triple(it.date, it.time, it.setNumber to (it.valueRight to it.valueLeft)) },
        )
        assertEquals(setOf("by hand"), history.map { it.comment }.toSet())
    }
}
