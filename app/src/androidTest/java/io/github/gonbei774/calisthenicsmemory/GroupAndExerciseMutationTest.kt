package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.BackupSnapshot
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Group and exercise mutations change every related row together, or none. */
@RunWith(AndroidJUnit4::class)
class GroupAndExerciseMutationTest {
    private lateinit var database: AppDatabase

    private val initial = BackupSnapshot(
        groups = listOf(ExerciseGroup(1, "Push", 0), ExerciseGroup(2, "Pull", 1)),
        exercises = listOf(
            Exercise(1, "Push-up", "Dynamic", "Push", sortOrder = 2),
            Exercise(2, "Dip", "Dynamic", "Push", sortOrder = 3),
            Exercise(3, "Pull-up", "Dynamic", "Pull", sortOrder = 1),
        ),
        records = listOf(TrainingRecord(1, 3, 5, null, 1, "2025-01-05", "07:30")),
        programs = emptyList(),
        programExercises = emptyList(),
        programLoops = emptyList(),
        intervalPrograms = emptyList(),
        intervalProgramExercises = emptyList(),
        intervalRecords = emptyList(),
        todoTasks = listOf(
            TodoTask(1, TodoTask.TYPE_GROUP, 1, 0),
            TodoTask(2, TodoTask.TYPE_GROUP, 2, 1),
            TodoTask(3, TodoTask.TYPE_EXERCISE, 3, 2),
            TodoTask(4, TodoTask.TYPE_EXERCISE, 1, 3),
        ),
    )

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        database.backupDao().replaceAll(initial)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun snapshot() = runBlocking { database.backupDao().snapshot() }

    @Test
    fun renameMovesTheGroupAndAllOfItsExercises() = runBlocking {
        database.exerciseGroupDao().renameGroupAndExercises("Push", "Press")

        val after = snapshot()
        assertEquals(listOf(ExerciseGroup(1, "Press", 0), ExerciseGroup(2, "Pull", 1)), after.groups)
        assertEquals(listOf("Press", "Press", "Pull"), after.exercises.map { it.group })
        assertEquals(initial.todoTasks, after.todoTasks)
    }

    @Test
    fun renameToAnExistingNameChangesNothing() {
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { database.exerciseGroupDao().renameGroupAndExercises("Push", "Pull") }
        }
        assertEquals(initial, snapshot())
    }

    @Test
    fun deleteRemovesGroupTodosAndUngroupsExercises() = runBlocking {
        database.exerciseGroupDao().deleteGroupAndUngroupExercises("Push")

        val after = snapshot()
        assertEquals(listOf(ExerciseGroup(2, "Pull", 1)), after.groups)
        assertEquals(
            listOf(Exercise(1, "Push-up", "Dynamic", null, 0), Exercise(2, "Dip", "Dynamic", null, 0), initial.exercises[2]),
            after.exercises,
        )
        assertEquals(initial.todoTasks.filterNot { it.id == 1L }, after.todoTasks)
        assertEquals(initial.records, after.records)
    }

    @Test
    fun deleteExerciseRemovesItsRecordsAndTodosOnly() = runBlocking {
        database.exerciseDao().deleteExerciseAndTodoTasks(initial.exercises[2])

        val after = snapshot()
        assertEquals(initial.exercises.take(2), after.exercises)
        assertEquals(emptyList<TrainingRecord>(), after.records)
        assertEquals(initial.todoTasks.filterNot { it.id == 3L }, after.todoTasks)
    }
}
