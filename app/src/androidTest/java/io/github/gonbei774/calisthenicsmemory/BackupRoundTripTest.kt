package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.BackupSnapshot
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupAnomalyKind
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupResult
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Every state the database can hold survives export, parse, and restore through BackupService. */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {
    private lateinit var source: AppDatabase
    private lateinit var target: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        source = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        target = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    // Synthetic rows without a database constraint that differ from what the app writes.
    private val anomalous = BackupSnapshot(
        groups = listOf(ExerciseGroup(1, "Push", 0)),
        exercises = listOf(
            Exercise(1, "Push-up", "Dynamic", "Push"),
            Exercise(2, "Squat", "Dynamic", "Deleted group"),
        ),
        records = emptyList(),
        programs = listOf(Program(1, "A"), Program(2, "B")),
        programExercises = listOf(ProgramExercise(1, 2, 1, 0, 3, 10, 60, loopId = 1)),
        programLoops = listOf(ProgramLoop(1, 1, 0, 2, 30)),
        intervalPrograms = emptyList(),
        intervalProgramExercises = emptyList(),
        intervalRecords = emptyList(),
        todoTasks = listOf(
            TodoTask(1, TodoTask.TYPE_EXERCISE, 1, 0),
            TodoTask(2, TodoTask.TYPE_EXERCISE, 99, 1),
            TodoTask(3, "RETIRED_TYPE", 1, 2),
            TodoTask(4, TodoTask.TYPE_EXERCISE, 2, 3, repeatDays = "1,x"),
        ),
    )

    @Test
    fun anomalousDatabaseRestoresUnchangedWithReportedAnomalies() = runBlocking {
        source.backupDao().replaceAll(anomalous)

        val exported = BackupService(source.backupDao()).export()
        assertTrue("export: $exported", exported is BackupResult.Success)
        val json = (exported as BackupResult.Success).value.json

        val targetService = BackupService(target.backupDao())
        val parsed = targetService.parse(json)
        assertTrue("parse: $parsed", parsed is BackupResult.Success)
        val parsedBackup = (parsed as BackupResult.Success).value
        assertEquals(
            listOf(
                BackupAnomalyKind.EXERCISE_MISSING_GROUP to 2L,
                BackupAnomalyKind.PROGRAM_EXERCISE_FOREIGN_LOOP to 1L,
                BackupAnomalyKind.TODO_MISSING_TARGET to 2L,
                BackupAnomalyKind.TODO_UNKNOWN_TYPE to 3L,
                BackupAnomalyKind.TODO_INVALID_REPEAT_DAYS to 4L,
            ),
            parsedBackup.anomalies.map { it.kind to it.entityId },
        )

        val restored = targetService.restore(parsedBackup.data)
        assertTrue("restore: $restored", restored is BackupResult.Success)
        assertEquals(anomalous, target.backupDao().snapshot())
    }
}
