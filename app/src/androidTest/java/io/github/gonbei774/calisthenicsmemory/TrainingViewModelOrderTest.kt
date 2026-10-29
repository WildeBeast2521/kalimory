package io.github.gonbei774.calisthenicsmemory

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Groups and exercises keep a clean order through the view model, on the app's own database. */
@RunWith(AndroidJUnit4::class)
class TrainingViewModelOrderTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val database = AppDatabase.getDatabase(application)
    private val groups = database.exerciseGroupDao()

    // Synthetic names, unique per run.
    private val tag = System.nanoTime()
    private val groupNames = listOf("Order A $tag", "Order B $tag", "Order C $tag")

    @After
    fun cleanUp(): Unit = runBlocking {
        database.exerciseDao().getExercisesByGroup(groupNames[1]).forEach { database.exerciseDao().deleteExercise(it) }
        groupNames.forEach { groups.deleteGroupByName(it) }
    }

    // The view model is created on the main thread, as in the app.
    private fun viewModel(): TrainingViewModel {
        lateinit var viewModel: TrainingViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModel = TrainingViewModel(application) }
        return viewModel
    }

    private suspend fun until(condition: suspend () -> Boolean) = withTimeout(10_000) {
        while (!condition()) delay(50)
    }

    @Test
    fun aNewGroupComesAfterEveryGroupEvenAfterADeletion() = runBlocking {
        val (a, b, c) = groupNames
        val start = groups.nextGroupDisplayOrder()
        groups.insertGroup(ExerciseGroup(name = a, displayOrder = start))
        groups.insertGroup(ExerciseGroup(name = b, displayOrder = start + 1))
        groups.deleteGroupByName(a)

        viewModel().createGroup(c)
        until { groups.getGroupByName(c) != null }

        // Counting the groups would give the new one the deleted group's slot, tying it with B.
        assertTrue(groups.getGroupByName(c)!!.displayOrder > groups.getGroupByName(b)!!.displayOrder)
    }

    @Test
    fun quickMovesLeaveEachExerciseItsOwnPlace() = runBlocking {
        val group = groupNames[1]
        groups.insertGroup(ExerciseGroup(name = group, displayOrder = groups.nextGroupDisplayOrder()))
        repeat(6) { database.exerciseDao().insertExercise(Exercise(name = "Order $tag $it", type = "Dynamic", group = group, displayOrder = it)) }

        val viewModel = viewModel()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            repeat(10) { viewModel.reorderExercises(group, it % 6, (it * 5 + 3) % 6) }
        }

        until { database.exerciseDao().getExercisesByGroup(group).map { it.displayOrder }.sorted() == (0..5).toList() }
        assertEquals((0..5).toList(), database.exerciseDao().getExercisesByGroup(group).map { it.displayOrder }.sorted())
    }
}
