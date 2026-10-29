package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.viewmodel.ProgramActions
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Programs and interval programs are copied, deleted and reordered whole. */
@RunWith(AndroidJUnit4::class)
class ProgramActionsTest {
    private val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).build()
    private val actions = ProgramActions(database)

    @After
    fun close() = database.close()

    private suspend fun exercise(name: String) = database.exerciseDao().insertExercise(Exercise(name = name, type = "Dynamic"))

    @Test
    fun aCopyKeepsEveryExerciseInItsOwnCopiedLoop() = runBlocking {
        val pushUp = exercise("Push-up")
        val squat = exercise("Squat")
        val program = database.programDao().insert(Program(name = "Full body"))
        val loop = database.programLoopDao().insert(ProgramLoop(programId = program, sortOrder = 0, rounds = 3, restBetweenRounds = 60))
        val pes = database.programExerciseDao()
        pes.insert(ProgramExercise(programId = program, exerciseId = pushUp, sortOrder = 0, sets = 3, targetValue = 10, intervalSeconds = 90, loopId = loop))
        pes.insert(ProgramExercise(programId = program, exerciseId = squat, sortOrder = 1, sets = 2, targetValue = 15, intervalSeconds = 60))

        val copy = actions.duplicateProgram(program, "(copy)")!!

        assertEquals("Full body (copy)", database.programDao().getProgramById(copy)!!.name)
        val copiedLoop = database.programLoopDao().getLoopsForProgramSync(copy).single()
        assertEquals(3, copiedLoop.rounds)
        val copied = pes.getExercisesForProgramSync(copy)
        assertEquals(listOf(pushUp, squat), copied.map { it.exerciseId })
        assertEquals(listOf(copiedLoop.id, null), copied.map { it.loopId })
        // The original is untouched.
        assertEquals(loop, pes.getExercisesForProgramSync(program).first().loopId)
    }

    @Test
    fun aDeletedProgramTakesItsToDoWithIt() = runBlocking {
        val program = database.programDao().insert(Program(name = "Upper body"))
        val interval = database.intervalProgramDao().insert(IntervalProgram(name = "Core", workSeconds = 30, restSeconds = 15, rounds = 4, roundRestSeconds = 0))
        val todos = database.todoTaskDao()
        todos.insert(TodoTask(type = TodoTask.TYPE_PROGRAM, referenceId = program, sortOrder = 0))
        todos.insert(TodoTask(type = TodoTask.TYPE_INTERVAL, referenceId = interval, sortOrder = 1))

        actions.deleteProgram(program)
        actions.deleteIntervalProgram(interval)

        assertNull(database.programDao().getProgramById(program))
        assertNull(database.intervalProgramDao().getProgramById(interval))
        assertNull(todos.getTaskByReference(TodoTask.TYPE_PROGRAM, program))
        assertNull(todos.getTaskByReference(TodoTask.TYPE_INTERVAL, interval))
    }

    @Test
    fun anIntervalCopyKeepsItsSettingsAndExercises() = runBlocking {
        val plank = exercise("Plank")
        val burpee = exercise("Burpee")
        val interval = database.intervalProgramDao().insert(IntervalProgram(name = "Tabata", workSeconds = 20, restSeconds = 10, rounds = 8, roundRestSeconds = 60))
        database.intervalProgramExerciseDao().insert(IntervalProgramExercise(programId = interval, exerciseId = plank, sortOrder = 0))
        database.intervalProgramExerciseDao().insert(IntervalProgramExercise(programId = interval, exerciseId = burpee, sortOrder = 1))

        val copy = actions.duplicateIntervalProgram(interval, "(copy)")!!

        val copied = database.intervalProgramDao().getProgramById(copy)!!
        assertEquals(listOf("Tabata (copy)", 20, 10, 8, 60), listOf(copied.name, copied.workSeconds, copied.restSeconds, copied.rounds, copied.roundRestSeconds))
        assertEquals(listOf(plank, burpee), database.intervalProgramExerciseDao().getExercisesForProgramSync(copy).map { it.exerciseId })
        assertNull(actions.duplicateIntervalProgram(-1, "(copy)"))
    }

    @Test
    fun movingAnExerciseRenumbersTheProgram() = runBlocking {
        val program = database.programDao().insert(Program(name = "Legs"))
        val ids = listOf("A", "B", "C").mapIndexed { i, name ->
            database.programExerciseDao().insert(
                ProgramExercise(programId = program, exerciseId = exercise(name), sortOrder = i, sets = 1, targetValue = 5, intervalSeconds = 60)
            )
        }

        actions.moveProgramExercise(program, 0, 2)
        actions.moveProgramExercise(program, 5, 0) // Out of range: nothing changes.

        val after = database.programExerciseDao().getExercisesForProgramSync(program)
        assertEquals(listOf(ids[1], ids[2], ids[0]), after.map { it.id })
        assertEquals(listOf(0, 1, 2), after.map { it.sortOrder })
    }
}
