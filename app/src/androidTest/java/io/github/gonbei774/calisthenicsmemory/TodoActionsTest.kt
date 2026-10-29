package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.viewmodel.TodoActions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneOffset

/** The To Do list adds at its end and completes entries, and their group, as the day's training shows. */
@RunWith(AndroidJUnit4::class)
class TodoActionsTest {
    private val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).build()
    private val todos = database.todoTaskDao()
    private val actions = TodoActions(database)

    // Thursday, 8 October 2026 (day 4).
    private val today = LocalDate.of(2026, 10, 8)
    private val zone = ZoneOffset.UTC

    @After
    fun close() = database.close()

    private suspend fun exercise(name: String, group: String? = null) =
        database.exerciseDao().insertExercise(Exercise(name = name, type = "Dynamic", group = group))

    private suspend fun trainedToday(exerciseId: Long) {
        database.trainingRecordDao().insertRecord(
            TrainingRecord(exerciseId = exerciseId, valueRight = 10, setNumber = 1, date = today.toString(), time = "07:30")
        )
    }

    @Test
    fun newEntriesGoAtTheEndInOrder() = runBlocking {
        actions.add(TodoTask.TYPE_PROGRAM, listOf(9))
        actions.add(TodoTask.TYPE_EXERCISE, listOf(3, 1, 2))

        assertEquals(listOf(9L, 3L, 1L, 2L), todos.getAllTasks().first().map { it.referenceId })
    }

    @Test
    fun aOneOffEntryIsRemovedAndARepeatingOneMarkedForToday() = runBlocking {
        val once = exercise("Squat")
        val daily = exercise("Plank")
        actions.add(TodoTask.TYPE_EXERCISE, listOf(once, daily))
        val repeating = todos.getTaskByReference(TodoTask.TYPE_EXERCISE, daily)!!
        todos.updateRepeatDays(repeating.id, "1,2,3,4,5,6,7")

        actions.complete(TodoTask.TYPE_EXERCISE, once, today, zone)
        actions.complete(TodoTask.TYPE_EXERCISE, daily, today, zone)

        assertNull(todos.getTaskByReference(TodoTask.TYPE_EXERCISE, once))
        assertEquals(today.toString(), todos.getTaskByReference(TodoTask.TYPE_EXERCISE, daily)!!.lastCompletedDate)
    }

    @Test
    fun aGroupIsCompletedOnlyOnceEveryExerciseInItWasTrainedToday() = runBlocking {
        database.exerciseGroupDao().insertGroup(ExerciseGroup(name = "Push"))
        val groupId = database.exerciseGroupDao().getGroupByName("Push")!!.id
        val pushUp = exercise("Push-up", "Push")
        val dip = exercise("Dip", "Push")
        actions.add(TodoTask.TYPE_GROUP, listOf(groupId))

        trainedToday(pushUp)
        actions.complete(TodoTask.TYPE_EXERCISE, pushUp, today, zone)
        assertEquals(groupId, todos.getTaskByReference(TodoTask.TYPE_GROUP, groupId)?.referenceId)

        trainedToday(dip)
        actions.complete(TodoTask.TYPE_EXERCISE, dip, today, zone)
        assertNull(todos.getTaskByReference(TodoTask.TYPE_GROUP, groupId))
    }

    @Test
    fun aRepeatingGroupWaitsForOneOfItsDays() = runBlocking {
        database.exerciseGroupDao().insertGroup(ExerciseGroup(name = "Legs"))
        val groupId = database.exerciseGroupDao().getGroupByName("Legs")!!.id
        val squat = exercise("Squat", "Legs")
        actions.add(TodoTask.TYPE_GROUP, listOf(groupId))
        val task = todos.getTaskByReference(TodoTask.TYPE_GROUP, groupId)!!
        todos.updateRepeatDays(task.id, "1,3") // Monday and Wednesday, not Thursday.

        trainedToday(squat)
        actions.complete(TodoTask.TYPE_EXERCISE, squat, today, zone)

        assertNull(todos.getTaskByReference(TodoTask.TYPE_GROUP, groupId)!!.lastCompletedDate)
    }
}
