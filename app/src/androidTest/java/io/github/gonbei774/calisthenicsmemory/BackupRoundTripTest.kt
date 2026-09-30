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

    private val v2History = anomalous.copy(
        workoutSessions = listOf(
            io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionEntity(
                id = 5, status = io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionStatus.COMPLETED,
                sourceType = io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSourceType.PROGRAM_TEMPLATE,
                sourceTemplateId = 2, sourceNameSnapshot = "B", startedAtEpochMillis = 1_000, endedAtEpochMillis = 9_000,
                updatedAtEpochMillis = 9_000, timePrecision = io.github.gonbei774.calisthenicsmemory.data.v2.TimePrecision.EXACT, comment = "c",
            ),
            // An interval workout keeps its settings (database 24, backup format 10).
            io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionEntity(
                id = 6, status = io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionStatus.COMPLETED,
                sourceType = io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSourceType.INTERVAL_TEMPLATE,
                sourceTemplateId = 3, sourceNameSnapshot = "Tabata", startedAtEpochMillis = 20_000, endedAtEpochMillis = 260_000,
                updatedAtEpochMillis = 260_000, timePrecision = io.github.gonbei774.calisthenicsmemory.data.v2.TimePrecision.EXACT,
                intervalWorkSeconds = 20, intervalRestSeconds = 10, intervalRounds = 8, intervalRoundRestSeconds = 60,
            ),
        ),
        sessionExercises = listOf(
            io.github.gonbei774.calisthenicsmemory.data.v2.SessionExerciseEntity(
                id = 6, workoutSessionId = 5, orderIndex = 0, exerciseId = 2, groupId = 1, sourceProgramExerciseId = 1,
                exerciseNameSnapshot = "Squat", exerciseKindSnapshot = io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind.DYNAMIC,
                lateralitySnapshot = io.github.gonbei774.calisthenicsmemory.data.v2.Laterality.UNILATERAL, groupNameSnapshot = "Deleted group",
                targetSets = 3, targetRepetitions = 10, targetDurationMillis = null,
            )
        ),
        setEntries = listOf(
            io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryEntity(
                id = 7, sessionExerciseId = 6, orderIndex = 0, setNumber = 1, roundNumber = 2,
                status = io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryStatus.COMPLETED,
                side = io.github.gonbei774.calisthenicsmemory.data.v2.BodySide.RIGHT, repetitions = 9, durationMillis = null, distanceCm = 12,
                addedWeightGrams = 3_000, assistanceGrams = null, targetRepetitions = 10, targetDurationMillis = null,
                startedAtEpochMillis = 2_000, completedAtEpochMillis = 3_000,
                timePrecision = io.github.gonbei774.calisthenicsmemory.data.v2.TimePrecision.EXACT, comment = "set",
                legacyTrainingRecordId = null,
            ),
            io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryEntity(
                id = 8, sessionExerciseId = 6, orderIndex = 1, setNumber = 1,
                status = io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryStatus.SKIPPED,
                side = io.github.gonbei774.calisthenicsmemory.data.v2.BodySide.LEFT,
                timePrecision = io.github.gonbei774.calisthenicsmemory.data.v2.TimePrecision.MINUTE,
            ),
        ),
    )

    @Test
    fun v2HistoryRestoresExactlyAlongsideLegacyData() = runBlocking {
        source.backupDao().replaceAll(v2History)

        val exported = BackupService(source.backupDao()).export()
        val json = (exported as BackupResult.Success).value.json
        assertTrue(json.contains("\"version\":11"))
        assertTrue(json.contains("\"intervalRoundRestSeconds\":60"))
        val parsed = BackupService(target.backupDao()).parse(json)
        assertTrue("parse: $parsed", parsed is BackupResult.Success)
        val restored = BackupService(target.backupDao()).restore((parsed as BackupResult.Success).value.data)
        assertTrue("restore: $restored", restored is BackupResult.Success)
        assertEquals(2, (restored as BackupResult.Success).value.workoutSessions)
        assertEquals(2, restored.value.setEntries)

        assertEquals(v2History, target.backupDao().snapshot())
    }

    // Format 11 (ADR 0007): catalogue links and a custom exercise placed in a built-in chain.
    private val progression = BackupSnapshot(
        groups = listOf(ExerciseGroup(1, "Pull", 0)),
        exercises = listOf(
            Exercise(1, "Pull-up", "Dynamic", "Pull", catalogId = "pull.pull_up"),
            Exercise(2, "Towel pull-up", "Dynamic", "Pull"),
        ),
        records = emptyList(), programs = emptyList(), programExercises = emptyList(), programLoops = emptyList(),
        intervalPrograms = emptyList(), intervalProgramExercises = emptyList(), intervalRecords = emptyList(), todoTasks = emptyList(),
        chainPlacements = listOf(io.github.gonbei774.calisthenicsmemory.data.progression.ChainPlacement(2, "pull", "pull.pull_up")),
    )

    @Test
    fun catalogueLinksAndPlacementsRestoreExactly() = runBlocking {
        source.backupDao().replaceAll(progression)

        val json = (BackupService(source.backupDao()).export() as BackupResult.Success).value.json
        assertTrue(json.contains("\"catalogId\":\"pull.pull_up\""))
        assertTrue(json.contains("\"chainPlacements\""))
        val parsed = BackupService(target.backupDao()).parse(json)
        assertTrue("parse: $parsed", parsed is BackupResult.Success)
        val restored = BackupService(target.backupDao()).restore((parsed as BackupResult.Success).value.data)
        assertTrue("restore: $restored", restored is BackupResult.Success)

        assertEquals(progression, target.backupDao().snapshot())
    }

    @Test
    fun restoringAnOlderBackupReplacesV2HistoryToo() = runBlocking {
        target.backupDao().replaceAll(v2History)
        val service = BackupService(target.backupDao())
        val version8 = (BackupService(source.backupDao()).export() as BackupResult.Success).value.data.copy(version = 8)

        val restored = service.restore(version8)
        assertTrue("restore: $restored", restored is BackupResult.Success)
        assertTrue(target.backupDao().snapshot().workoutSessions.isEmpty())
        assertTrue(target.backupDao().snapshot().setEntries.isEmpty())
    }
}
