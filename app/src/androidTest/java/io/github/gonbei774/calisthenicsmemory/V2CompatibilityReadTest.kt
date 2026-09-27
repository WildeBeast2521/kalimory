package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.BackupSnapshot
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.data.v2.BodySide
import io.github.gonbei774.calisthenicsmemory.data.v2.CompatibilityHistory
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import io.github.gonbei774.calisthenicsmemory.data.v2.Laterality
import io.github.gonbei774.calisthenicsmemory.data.v2.SessionExerciseEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.TimePrecision
import io.github.gonbei774.calisthenicsmemory.data.v2.V2Backfill
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSourceType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.time.LocalDate
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The compatibility read path shows legacy history and v2-only workouts together,
 * each set once, whether or not the legacy records were backfilled into v2.
 */
@RunWith(AndroidJUnit4::class)
class V2CompatibilityReadTest {
    private lateinit var database: AppDatabase
    private val zone = ZoneId.of("Asia/Kolkata")

    private val legacyRecords = listOf(
        TrainingRecord(1, 1, 12, null, 1, "2025-01-05", "07:30", "Workout"),
        TrainingRecord(2, 1, 10, null, 2, "2025-01-05", "07:30", "Workout"),
        TrainingRecord(3, 2, 8, 7, 1, "2025-01-06", "18:00", "Workout"),
    )

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        database.backupDao().replaceAll(
            BackupSnapshot(
                groups = emptyList(),
                exercises = listOf(
                    Exercise(1, "Push-up", "Dynamic"),
                    Exercise(2, "Lunge", "Dynamic", laterality = "Unilateral"),
                    Exercise(3, "Temporary", "Dynamic"),
                ),
                records = legacyRecords,
                programs = emptyList(), programExercises = emptyList(), programLoops = emptyList(),
                intervalPrograms = emptyList(), intervalProgramExercises = emptyList(), intervalRecords = emptyList(),
                todoTasks = emptyList(),
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** A workout that exists only in v2: a two-sided lunge set, a skipped set, and a set of a later-deleted exercise. */
    private suspend fun writeV2OnlyWorkout() {
        val dao = database.workoutSessionDao()
        val startedAt = ZonedDateTime.of(2025, 2, 1, 6, 0, 0, 0, zone).toInstant().toEpochMilli()
        val sessionId = dao.insertSession(
            WorkoutSessionEntity(
                status = WorkoutSessionStatus.COMPLETED, sourceType = WorkoutSourceType.AD_HOC,
                startedAtEpochMillis = startedAt, updatedAtEpochMillis = startedAt, timePrecision = TimePrecision.EXACT,
                comment = "v2 only",
            )
        )
        fun exercise(order: Int, id: Long, name: String, laterality: Laterality) = SessionExerciseEntity(
            workoutSessionId = sessionId, orderIndex = order, exerciseId = id, exerciseNameSnapshot = name,
            exerciseKindSnapshot = ExerciseKind.DYNAMIC, lateralitySnapshot = laterality,
        )
        val lunge = dao.insertSessionExercise(exercise(0, 2, "Lunge", Laterality.UNILATERAL))
        val temporary = dao.insertSessionExercise(exercise(1, 3, "Temporary", Laterality.BILATERAL))
        fun set(parent: Long, order: Int, number: Int, side: BodySide, reps: Int?, status: SetEntryStatus = SetEntryStatus.COMPLETED) =
            SetEntryEntity(
                sessionExerciseId = parent, orderIndex = order, setNumber = number, status = status, side = side,
                repetitions = reps, completedAtEpochMillis = startedAt + 60_000L * order, timePrecision = TimePrecision.EXACT,
            )
        dao.insertSetEntries(
            listOf(
                set(lunge, 0, 1, BodySide.RIGHT, 9),
                set(lunge, 1, 1, BodySide.LEFT, 8),
                set(lunge, 2, 2, BodySide.RIGHT, null, SetEntryStatus.SKIPPED),
                set(temporary, 0, 1, BodySide.BILATERAL, 5),
            )
        )
        database.exerciseDao().deleteExerciseById(3)
    }

    @Test
    fun legacyAndV2OnlyWorkoutsAppearOnceEach() = runBlocking {
        writeV2OnlyWorkout()
        val before = CompatibilityHistory.read(database, zone)

        // Converting the legacy records into v2 must not change what history shows.
        V2Backfill.run(database, zone, nowEpochMillis = 1)
        val after = CompatibilityHistory.read(database, zone)
        assertEquals(before, after)

        assertEquals(legacyRecords.map { it.id }.toSet(), after.filter { it.source == HistorySource.LEGACY }.map { it.legacyRecordId }.toSet())
        assertEquals(legacyRecords.size, after.count { it.source == HistorySource.LEGACY })

        val v2 = after.single { it.source == HistorySource.V2 }
        assertEquals(2L, v2.exerciseId)
        assertEquals("2025-02-01", v2.date)
        assertEquals("06:00", v2.time)
        assertEquals(9, v2.valueRight)
        assertEquals(8, v2.valueLeft)
        assertEquals("v2 only", v2.comment)
    }

    @Test
    fun latestSessionIsTheNewestWorkoutFromEitherStore() = runBlocking {
        writeV2OnlyWorkout()

        // Lunge: the v2 workout (Feb 1) is newer than the legacy one (Jan 6); its sides pair into one set.
        val lunge = CompatibilityHistory.latestSession(database, 2, zone)
        assertEquals(listOf(HistorySource.V2), lunge.map { it.source })
        assertEquals(listOf(9 to 8), lunge.map { it.valueRight to it.valueLeft })

        // Push-up has only legacy history: every record of its newest date and time, by set number.
        val pushUp = CompatibilityHistory.latestSession(database, 1, zone)
        assertEquals(listOf(1L, 2L), pushUp.map { it.legacyRecordId })

        // A newer legacy workout wins again.
        database.trainingRecordDao().insertRecord(TrainingRecord(10, 2, 6, 6, 1, "2025-03-01", "08:00", "later"))
        assertEquals(listOf(10L), CompatibilityHistory.latestSession(database, 2, zone).map { it.legacyRecordId })

        assertEquals(emptyList<HistorySet>(), CompatibilityHistory.latestSession(database, 99, zone))
    }

    @Test
    fun hasSetOnChecksBothStoresInTheGivenZone() = runBlocking {
        writeV2OnlyWorkout()
        val feb1 = LocalDate.of(2025, 2, 1)

        assertTrue(CompatibilityHistory.hasSetOn(database, 2, feb1, zone))
        assertFalse(CompatibilityHistory.hasSetOn(database, 2, feb1.minusDays(1), zone))
        assertTrue(CompatibilityHistory.hasSetOn(database, 2, LocalDate.of(2025, 1, 6), zone))
        // 06:00 in Kolkata on Feb 1 is still Jan 31 in Los Angeles.
        val losAngeles = ZoneId.of("America/Los_Angeles")
        assertTrue(CompatibilityHistory.hasSetOn(database, 2, feb1.minusDays(1), losAngeles))
        assertFalse(CompatibilityHistory.hasSetOn(database, 2, feb1, losAngeles))
        // Push-up has no v2 sets and no legacy record that day.
        assertFalse(CompatibilityHistory.hasSetOn(database, 1, feb1, zone))
    }
}
