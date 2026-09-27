package io.github.gonbei774.calisthenicsmemory

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.data.v2.BodySide
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import io.github.gonbei774.calisthenicsmemory.data.v2.Laterality
import io.github.gonbei774.calisthenicsmemory.data.v2.SessionExerciseEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.TimePrecision
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSourceType
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * The view model's remaining history readers (CSV export and its duplicate check, previous-value
 * prefill, and the per-day check) see v2-only workouts, on the app's own database.
 */
@RunWith(AndroidJUnit4::class)
class HistoryReadersViewModelTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val database = AppDatabase.getDatabase(application)

    // Synthetic data, unique per run.
    private val exerciseName = "Readers test ${System.nanoTime()}"
    private val today = LocalDate.now()
    private var exerciseId = 0L
    private var sessionId = 0L

    @Before
    fun seed(): Unit = runBlocking {
        exerciseId = database.exerciseDao().insertExercise(Exercise(name = exerciseName, type = "Dynamic"))
        database.trainingRecordDao().insertRecord(
            TrainingRecord(exerciseId = exerciseId, valueRight = 41, setNumber = 1, date = today.toString(), time = "06:00")
        )
        val startedAt = today.atTime(20, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dao = database.workoutSessionDao()
        sessionId = dao.insertSession(
            WorkoutSessionEntity(
                status = WorkoutSessionStatus.COMPLETED, sourceType = WorkoutSourceType.AD_HOC,
                startedAtEpochMillis = startedAt, updatedAtEpochMillis = startedAt, timePrecision = TimePrecision.EXACT,
                comment = "v2 session",
            )
        )
        val sessionExerciseId = dao.insertSessionExercise(
            SessionExerciseEntity(
                workoutSessionId = sessionId, orderIndex = 0, exerciseId = exerciseId, exerciseNameSnapshot = exerciseName,
                exerciseKindSnapshot = ExerciseKind.DYNAMIC, lateralitySnapshot = Laterality.BILATERAL,
            )
        )
        dao.insertSetEntries(
            listOf(1 to 37, 2 to 35).mapIndexed { index, (number, reps) ->
                SetEntryEntity(
                    sessionExerciseId = sessionExerciseId, orderIndex = index, setNumber = number,
                    status = SetEntryStatus.COMPLETED, side = BodySide.BILATERAL, repetitions = reps,
                    completedAtEpochMillis = startedAt, timePrecision = TimePrecision.EXACT,
                )
            }
        )
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        database.openHelper.writableDatabase.execSQL("DELETE FROM workout_sessions WHERE id = ?", arrayOf<Any>(sessionId))
        // Records, including any a failed duplicate check imported, cascade from the exercise.
        database.exerciseDao().deleteExerciseById(exerciseId)
    }

    private fun viewModel(): TrainingViewModel = runBlocking {
        val viewModel = TrainingViewModel(application)
        // Export resolves exercise names from the loaded list.
        withTimeout(10_000) { viewModel.exercises.first { list -> list.any { it.id == exerciseId } } }
        viewModel
    }

    @Test
    fun csvExportIncludesV2SetsAndReimportingItAddsNothing() = runBlocking {
        val viewModel = viewModel()
        val csv = viewModel.exportRecords()
        val ours = csv.lines().filter { it.startsWith(exerciseName) }
        // Columns: exerciseName, exerciseType, date, time, setNumber, valueRight, ...
        assertEquals(
            listOf("$today,06:00,1,41", "$today,20:00,1,37", "$today,20:00,2,35"),
            ours.map { it.split(",").subList(2, 6).joinToString(",") },
        )

        val report = viewModel.importRecordsFromCsv(csv)
        assertEquals(0, report.successCount)
        assertEquals(1, database.trainingRecordDao().countByExercise(exerciseId))
    }

    @Test
    fun previousValuesComeFromTheNewestWorkoutInEitherStore() = runBlocking {
        val latest = viewModel().getLatestSession(exerciseId)
        assertEquals(listOf(HistorySource.V2, HistorySource.V2), latest.map { it.source })
        assertEquals(listOf(37, 35), latest.map { it.valueRight })
    }

    @Test
    fun aV2WorkoutCountsAsTrainingThatDay() = runBlocking {
        val viewModel = viewModel()
        database.trainingRecordDao().deleteSession(exerciseId, today.toString(), "06:00")
        assertTrue(viewModel.hasRecordOnDate(exerciseId, today.toString()))
    }
}
