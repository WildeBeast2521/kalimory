package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.ui.UiMessage
import io.github.gonbei774.calisthenicsmemory.viewmodel.CommunityShareTransfer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** A community share made on one phone imports on another with what it depends on. */
@RunWith(AndroidJUnit4::class)
class CommunityShareTransferTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val source = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val target = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val messages = mutableListOf<UiMessage>()

    private fun transfer(database: AppDatabase) = CommunityShareTransfer(database, { "9.9.9" }) { messages += it }

    @After
    fun close() {
        source.close()
        target.close()
    }

    @Test
    fun aSharedProgramImportsWithItsLoopExercisesAndGroup() = runBlocking {
        source.exerciseGroupDao().insertGroup(ExerciseGroup(name = "Push"))
        val pushUp = source.exerciseDao().insertExercise(Exercise(name = "Push-up", type = "Dynamic", group = "Push"))
        val plank = source.exerciseDao().insertExercise(Exercise(name = "Plank", type = "Isometric"))
        val program = source.programDao().insert(Program(name = "Upper body"))
        val loop = source.programLoopDao().insert(ProgramLoop(programId = program, sortOrder = 0, rounds = 3, restBetweenRounds = 60))
        source.programExerciseDao().insert(
            ProgramExercise(programId = program, exerciseId = pushUp, sortOrder = 0, sets = 3, targetValue = 10, intervalSeconds = 90, loopId = loop)
        )
        val interval = source.intervalProgramDao().insert(IntervalProgram(name = "Core", workSeconds = 30, restSeconds = 15, rounds = 4, roundRestSeconds = 60))
        source.intervalProgramExerciseDao().insert(IntervalProgramExercise(programId = interval, exerciseId = plank, sortOrder = 0))

        val json = transfer(source).exportCommunityShare(setOf(program), setOf(interval), emptySet())
        assertEquals("share", transfer(source).detectJsonFileType(json))

        val report = transfer(target).importCommunityShare(json)

        assertTrue(report.errors.toString(), report.errors.isEmpty())
        val after = target.backupDao().snapshot()
        assertEquals(listOf("Push"), after.groups.map { it.name })
        assertEquals(setOf("Push-up", "Plank"), after.exercises.map { it.name }.toSet())
        assertEquals("Push", after.exercises.single { it.name == "Push-up" }.group)
        assertEquals(listOf("Upper body"), after.programs.map { it.name })
        assertEquals(after.programLoops.single().id, after.programExercises.single().loopId)
        assertEquals(listOf("Core"), after.intervalPrograms.map { it.name })
        assertEquals(after.exercises.single { it.name == "Plank" }.id, after.intervalProgramExercises.single().exerciseId)
    }

    @Test
    fun aBackupIsToldApartAndRefusedAsAShare() = runBlocking {
        val backup = """{"version":11,"exportDate":"2026-10-08T07:30:00","app":"CalisthenicsMemory","groups":[],"exercises":[],"records":[]}"""
        assertEquals("backup", transfer(target).detectJsonFileType(backup))
        assertEquals("unknown", transfer(target).detectJsonFileType("not json"))

        val report = transfer(target).importCommunityShare(backup)

        assertEquals(listOf("Wrong file type: backup"), report.errors)
        assertEquals(UiMessage.WrongFileType(detected = "backup", expected = "share"), messages.last())
        assertTrue(target.backupDao().snapshot().exercises.isEmpty())
    }
}
