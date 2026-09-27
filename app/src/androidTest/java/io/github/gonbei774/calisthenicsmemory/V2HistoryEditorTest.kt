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
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.Laterality
import io.github.gonbei774.calisthenicsmemory.data.v2.SessionExerciseEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.TimePrecision
import io.github.gonbei774.calisthenicsmemory.data.v2.V2HistoryEditor
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSourceType
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId
import java.time.ZonedDateTime

/** Editing and deleting v2 history through the shapes the history screens use. */
@RunWith(AndroidJUnit4::class)
class V2HistoryEditorTest {
    private lateinit var database: AppDatabase
    private val zone = ZoneId.of("Asia/Kolkata")
    private val startedAt = ZonedDateTime.of(2025, 2, 1, 6, 0, 0, 0, zone).toInstant().toEpochMilli()
    private var sessionId = 0L
    private var lungeId = 0L
    private var plankId = 0L

    @Before
    fun setUp(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        database.exerciseDao().insertExercise(Exercise(2, "Lunge", "Dynamic", laterality = "Unilateral"))
        database.exerciseDao().insertExercise(Exercise(4, "Plank", "Isometric"))
        val dao = database.workoutSessionDao()
        sessionId = dao.insertSession(
            WorkoutSessionEntity(
                status = WorkoutSessionStatus.COMPLETED, sourceType = WorkoutSourceType.AD_HOC,
                startedAtEpochMillis = startedAt, endedAtEpochMillis = startedAt + 600_000,
                updatedAtEpochMillis = startedAt, timePrecision = TimePrecision.EXACT, comment = "before",
            )
        )
        fun occurrence(order: Int, id: Long, name: String, kind: ExerciseKind, laterality: Laterality) = SessionExerciseEntity(
            workoutSessionId = sessionId, orderIndex = order, exerciseId = id, exerciseNameSnapshot = name,
            exerciseKindSnapshot = kind, lateralitySnapshot = laterality,
        )
        lungeId = dao.insertSessionExercise(occurrence(0, 2, "Lunge", ExerciseKind.DYNAMIC, Laterality.UNILATERAL))
        plankId = dao.insertSessionExercise(occurrence(1, 4, "Plank", ExerciseKind.ISOMETRIC, Laterality.BILATERAL))
        fun set(parent: Long, order: Int, number: Int, side: BodySide, reps: Int? = null, millis: Long? = null,
                status: SetEntryStatus = SetEntryStatus.COMPLETED) = SetEntryEntity(
            sessionExerciseId = parent, orderIndex = order, setNumber = number, status = status, side = side,
            repetitions = reps, durationMillis = millis, completedAtEpochMillis = startedAt + 60_000L * order,
            timePrecision = TimePrecision.EXACT,
        )
        dao.insertSetEntries(
            listOf(
                set(lungeId, 0, 1, BodySide.RIGHT, reps = 9),
                set(lungeId, 1, 1, BodySide.LEFT, reps = 8),
                set(lungeId, 2, 2, BodySide.RIGHT, status = SetEntryStatus.SKIPPED),
                set(plankId, 0, 1, BodySide.BILATERAL, millis = 45_500),
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun history() = CompatibilityHistory.read(database, zone)
    private suspend fun shown(exerciseId: Long): HistorySet = history().single { it.exerciseId == exerciseId }
    private suspend fun entries(occurrence: Long) = database.workoutSessionDao().setEntries(occurrence)

    @Test
    fun setValuesGoToEachSide() = runBlocking {
        V2HistoryEditor.updateSetValues(database, shown(2), valueRight = 10, valueLeft = 6)
        assertEquals(listOf(10, 6, null), entries(lungeId).map { it.repetitions })
        assertEquals(10 to 6, shown(2).let { it.valueRight to it.valueLeft })
    }

    @Test
    fun isometricValuesAreSecondsAndUnchangedSecondsKeepTheirPrecision() = runBlocking {
        V2HistoryEditor.updateSetValues(database, shown(4), valueRight = 45, valueLeft = null)
        assertEquals(45_500L, entries(plankId).single().durationMillis)
        V2HistoryEditor.updateSetValues(database, shown(4), valueRight = 50, valueLeft = null)
        assertEquals(50_000L, entries(plankId).single().durationMillis)
    }

    @Test
    fun sessionEditMovesTheWholeWorkoutAndAppliesComment() = runBlocking {
        val lunge = shown(2)
        V2HistoryEditor.updateSession(
            database, listOf(lunge), "2025-02-02", "07:30", "after",
            distancesCm = listOf(null), weightsG = listOf(5_000), assistancesG = listOf(null),
            zone = zone, nowEpochMillis = 123,
        )
        val moved = ZonedDateTime.of(2025, 2, 2, 7, 30, 0, 0, zone).toInstant().toEpochMilli()
        val shift = moved - startedAt
        val session = database.workoutSessionDao().session(sessionId)!!
        assertEquals(moved, session.startedAtEpochMillis)
        assertEquals(startedAt + 600_000 + shift, session.endedAtEpochMillis)
        assertEquals(TimePrecision.MINUTE, session.timePrecision)
        assertEquals(123L, session.updatedAtEpochMillis)
        assertEquals("after", session.comment)
        // Every set in the workout shifts by the same amount, the other exercise included.
        assertEquals(listOf(0L, 1L, 2L).map { startedAt + 60_000 * it + shift }, entries(lungeId).map { it.completedAtEpochMillis })
        assertEquals(startedAt + shift, entries(plankId).single().completedAtEpochMillis)
        assertEquals(setOf("2025-02-02" to "07:30"), history().map { it.date to it.time }.toSet())
        // The weight applies to both sides of the displayed set only.
        assertEquals(listOf(5_000, 5_000, null), entries(lungeId).map { it.addedWeightGrams })
    }

    @Test
    fun sameTimeKeepsExactPrecision() = runBlocking {
        V2HistoryEditor.updateSession(
            database, listOf(shown(4)), "2025-02-01", "06:00", "", listOf(null), listOf(null), listOf(null), zone, 1,
        )
        val session = database.workoutSessionDao().session(sessionId)!!
        assertEquals(TimePrecision.EXACT, session.timePrecision)
        assertNull(session.comment)
    }

    @Test
    fun deleteRemovesTheOccurrenceThenTheEmptyWorkout() = runBlocking {
        V2HistoryEditor.delete(database, listOf(shown(2)))
        // The skipped lunge set goes with it; the plank stays.
        assertEquals(emptyList<SetEntryEntity>(), entries(lungeId))
        assertEquals(listOf(4L), history().map { it.exerciseId })
        assertEquals(sessionId, database.workoutSessionDao().session(sessionId)?.id)

        V2HistoryEditor.delete(database, listOf(shown(4)))
        assertNull(database.workoutSessionDao().session(sessionId))
        assertEquals(emptyList<HistorySet>(), history())
    }

    @Test
    fun legacySetsAndNegativeValuesAreRefused() = runBlocking {
        database.trainingRecordDao().insertRecord(TrainingRecord(1, 2, 5, 5, 1, "2025-01-01", "08:00"))
        val legacy = history().single { it.legacyRecordId == 1L }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { V2HistoryEditor.updateSetValues(database, legacy, 1, 1) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { V2HistoryEditor.delete(database, listOf(legacy)) }
        }
        val lunge = history().single { it.exerciseId == 2L && it.legacyRecordId == null }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { V2HistoryEditor.updateSetValues(database, lunge, -1, 3) }
        }
        assertEquals(listOf(9, 8, null), entries(lungeId).map { it.repetitions })
    }
}
