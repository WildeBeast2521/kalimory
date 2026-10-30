package io.github.gonbei774.calisthenicsmemory

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** The delete confirmation's counts belong to the exercise being deleted, on the app's own database. */
@RunWith(AndroidJUnit4::class)
class ExerciseDeleteImpactTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val database = AppDatabase.getDatabase(application)
    private var exerciseId = 0L

    @After
    fun cleanUp(): Unit = runBlocking { database.exerciseDao().deleteExerciseById(exerciseId) }

    @Test
    fun theImpactNamesItsExerciseAndCountsItsRecords() = runBlocking {
        exerciseId = database.exerciseDao().insertExercise(Exercise(name = "Impact test ${System.nanoTime()}", type = "Dynamic"))
        repeat(2) {
            database.trainingRecordDao().insertRecord(
                TrainingRecord(exerciseId = exerciseId, valueRight = 10, setNumber = it + 1, date = "2026-03-12", time = "07:30")
            )
        }
        val exercise = database.exerciseDao().getExerciseById(exerciseId)!!
        lateinit var viewModel: TrainingViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModel = TrainingViewModel(application) }

        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModel.loadDeleteImpact(exercise) }
        val impact = withTimeout(10_000) { viewModel.deleteImpact.first { it != null } }!!

        assertEquals(exerciseId, impact.exerciseId)
        assertEquals(2, impact.recordCount)
    }
}
