package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.data.BackupDao
import io.github.gonbei774.calisthenicsmemory.viewmodel.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class BackupValidationTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val service = BackupService(mock<BackupDao>())

    private fun valid() = BackupData(
        version = 8, exportDate = "2026-01-02T03:04:05", app = BackupService.APP_NAME,
        groups = listOf(ExportGroup(1, "Strength")),
        exercises = listOf(ExportExercise(2, "Pull-up", "Dynamic", "Strength", 0, laterality = "Bilateral")),
        records = listOf(ExportRecord(3, 2, 5, null, 1, "2026-01-02", "03:04", "ok")),
        programs = listOf(ExportProgram(4, "Main"), ExportProgram(14, "Other")),
        programExercises = listOf(ExportProgramExercise(6, 4, 2, 0, 3, 5, 60, 5)),
        programLoops = listOf(ExportProgramLoop(5, 4, 0, 2, 30)),
        intervalPrograms = listOf(ExportIntervalProgram(7, "Intervals", 30, 10, 3, 60)),
        intervalProgramExercises = listOf(ExportIntervalProgramExercise(8, 7, 2, 0)),
        intervalRecords = listOf(ExportIntervalRecord(9, "Intervals", "2026-01-02", "03:04", 30, 10, 3, 60, 3, 1, "[]")),
        todoTasks = listOf(
            ExportTodoTask(10, "EXERCISE", 2, 0), ExportTodoTask(11, "GROUP", 1, 1),
            ExportTodoTask(12, "PROGRAM", 4, 2), ExportTodoTask(13, "INTERVAL", 7, 3)
        )
    )

    private fun assertInvalid(data: BackupData, text: String) {
        val result = service.parse(json.encodeToString(data))
        assertTrue("Expected validation failure for $text, got $result", result is BackupResult.Failure)
        result as BackupResult.Failure
        assertEquals(BackupFailureKind.VALIDATION, result.kind)
        assertTrue("${result.message} should contain $text", result.message.contains(text, ignoreCase = true))
    }

    @Test fun `rejects wrong app and unsupported versions`() {
        assertInvalid(valid().copy(app = "Other"), "app")
        assertInvalid(valid().copy(version = 0), "version")
        assertInvalid(valid().copy(version = BackupService.CURRENT_VERSION + 1), "version")
    }

    @Test fun `rejects duplicate ids group names and exercise keys`() {
        assertInvalid(valid().copy(records = valid().records + valid().records.first()), "Duplicate record id")
        assertInvalid(valid().copy(groups = valid().groups + ExportGroup(20, "Strength")), "Duplicate group name")
        assertInvalid(valid().copy(exercises = valid().exercises + valid().exercises.first().copy(id = 20)), "Duplicate exercise name/type")
    }

    @Test fun `rejects zero and negative ids in every entity collection`() {
        val cases: List<Pair<String, (BackupData, Long) -> BackupData>> = listOf(
            "group" to { data, id -> data.copy(groups = listOf(data.groups.first().copy(id = id))) },
            "exercise" to { data, id -> data.copy(exercises = listOf(data.exercises.first().copy(id = id))) },
            "record" to { data, id -> data.copy(records = listOf(data.records.first().copy(id = id))) },
            "program" to { data, id -> data.copy(programs = listOf(data.programs.first().copy(id = id))) },
            "program exercise" to { data, id -> data.copy(programExercises = listOf(data.programExercises.first().copy(id = id))) },
            "program loop" to { data, id -> data.copy(programLoops = listOf(data.programLoops.first().copy(id = id))) },
            "interval program" to { data, id -> data.copy(intervalPrograms = listOf(data.intervalPrograms.first().copy(id = id))) },
            "interval program exercise" to { data, id -> data.copy(intervalProgramExercises = listOf(data.intervalProgramExercises.first().copy(id = id))) },
            "interval record" to { data, id -> data.copy(intervalRecords = listOf(data.intervalRecords.first().copy(id = id))) },
            "todo task" to { data, id -> data.copy(todoTasks = listOf(data.todoTasks.first().copy(id = id))) }
        )

        cases.forEach { (label, withId) ->
            listOf(0L, -1L).forEach { id ->
                assertInvalid(withId(valid(), id), "$label id must be positive")
            }
        }
    }

    @Test fun `rejects references the database enforces with foreign keys`() {
        val base = valid()
        val cases = listOf(
            base.copy(records = listOf(base.records.first().copy(exerciseId = 99))) to "missing exercise",
            base.copy(programLoops = listOf(base.programLoops.first().copy(programId = 99))) to "missing program",
            base.copy(programExercises = listOf(base.programExercises.first().copy(programId = 99, loopId = null))) to "missing program",
            base.copy(programExercises = listOf(base.programExercises.first().copy(exerciseId = 99))) to "missing exercise",
            base.copy(programExercises = listOf(base.programExercises.first().copy(loopId = 99))) to "missing loop",
            base.copy(intervalProgramExercises = listOf(base.intervalProgramExercises.first().copy(programId = 99))) to "missing interval program",
            base.copy(intervalProgramExercises = listOf(base.intervalProgramExercises.first().copy(exerciseId = 99))) to "missing exercise",
        )
        cases.forEach { (data, message) -> assertInvalid(data, message) }
    }

    private fun assertAcceptedWith(data: BackupData, vararg expected: Pair<BackupAnomalyKind, Long>) {
        val result = service.parse(json.encodeToString(data))
        assertTrue("Expected success for $data, got $result", result is BackupResult.Success)
        val parsed = (result as BackupResult.Success).value
        assertEquals(data, parsed.data)
        assertEquals(expected.toList(), parsed.anomalies.map { it.kind to it.entityId })
    }

    @Test fun `accepts and reports values the database can hold without constraints`() {
        val base = valid()
        val todo = base.todoTasks.first()
        assertAcceptedWith(
            base.copy(exercises = listOf(base.exercises.first().copy(group = "Missing"))),
            BackupAnomalyKind.EXERCISE_MISSING_GROUP to 2L,
        )
        assertAcceptedWith(
            base.copy(todoTasks = listOf(
                ExportTodoTask(10, "EXERCISE", 99, 0), ExportTodoTask(11, "GROUP", 99, 1),
                ExportTodoTask(12, "PROGRAM", 99, 2), ExportTodoTask(13, "INTERVAL", 99, 3),
            )),
            BackupAnomalyKind.TODO_MISSING_TARGET to 10L, BackupAnomalyKind.TODO_MISSING_TARGET to 11L,
            BackupAnomalyKind.TODO_MISSING_TARGET to 12L, BackupAnomalyKind.TODO_MISSING_TARGET to 13L,
        )
        assertAcceptedWith(
            base.copy(todoTasks = listOf(ExportTodoTask(10, "UNKNOWN", 99, 0))),
            BackupAnomalyKind.TODO_UNKNOWN_TYPE to 10L,
        )
        // The loop belongs to program 4; the exercise is moved to program 14.
        assertAcceptedWith(
            base.copy(programExercises = listOf(base.programExercises.first().copy(programId = 14))),
            BackupAnomalyKind.PROGRAM_EXERCISE_FOREIGN_LOOP to 6L,
        )
        listOf("x", "0", "8", "1,,2", "1,1").forEach { repeatDays ->
            assertAcceptedWith(
                base.copy(todoTasks = listOf(todo.copy(repeatDays = repeatDays))),
                BackupAnomalyKind.TODO_INVALID_REPEAT_DAYS to 10L,
            )
        }
        assertAcceptedWith(base.copy(todoTasks = listOf(todo.copy(repeatDays = "1, 3,7"))))
    }

    @Test fun `accepts legacy defaults`() {
        val legacy = """{"version":1,"exportDate":"old","app":"CalisthenicsMemory","groups":[],"exercises":[],"records":[]}"""
        val result = service.parse(legacy)
        assertTrue(result is BackupResult.Success)
        assertTrue((result as BackupResult.Success).value.data.programs.isEmpty())
        assertTrue(result.value.data.todoTasks.isEmpty())
        assertTrue(result.value.anomalies.isEmpty())
    }

    @Test fun `accepts a complete version 8 backup`() {
        val result = service.parse(json.encodeToString(valid()))
        assertTrue("Expected success, got $result", result is BackupResult.Success)
        assertEquals(ParsedBackup(valid(), emptyList()), (result as BackupResult.Success).value)
    }

    // ----- v2 history (format 9) -----

    private fun validV9() = valid().copy(
        version = 9,
        workoutSessions = listOf(ExportWorkoutSession(20, "COMPLETED", "AD_HOC", startedAtEpochMillis = 1_000, updatedAtEpochMillis = 2_000, timePrecision = "EXACT")),
        sessionExercises = listOf(
            ExportSessionExercise(21, 20, 0, exerciseId = 2, groupId = 1, exerciseNameSnapshot = "Pull-up", exerciseKindSnapshot = "DYNAMIC", lateralitySnapshot = "BILATERAL"),
        ),
        setEntries = listOf(
            ExportSetEntry(22, 21, 0, 1, status = "COMPLETED", side = "BILATERAL", repetitions = 8, timePrecision = "EXACT", legacyTrainingRecordId = 3),
            ExportSetEntry(23, 21, 1, 2, status = "SKIPPED", side = "BILATERAL", timePrecision = "EXACT"),
        ),
    )

    @Test fun `accepts a complete version 9 backup with v2 history`() {
        val result = service.parse(json.encodeToString(validV9()))
        assertTrue("Expected success, got $result", result is BackupResult.Success)
        assertEquals(ParsedBackup(validV9(), emptyList()), (result as BackupResult.Success).value)
    }

    @Test fun `rejects v2 history the database would refuse`() {
        val base = validV9()
        val exercise = base.sessionExercises.first()
        val set = base.setEntries.first()
        val cases = listOf(
            base.copy(workoutSessions = base.workoutSessions + base.workoutSessions.first()) to "Duplicate workout session id",
            base.copy(setEntries = listOf(set.copy(id = 0))) to "set entry id must be positive",
            base.copy(workoutSessions = listOf(base.workoutSessions.first().copy(status = "DONE"))) to "unknown code DONE",
            base.copy(sessionExercises = listOf(exercise.copy(exerciseKindSnapshot = "Dynamic"))) to "unknown code Dynamic",
            base.copy(setEntries = listOf(set.copy(side = "BOTH"))) to "unknown code BOTH",
            base.copy(sessionExercises = listOf(exercise.copy(workoutSessionId = 99))) to "missing workout session",
            base.copy(sessionExercises = listOf(exercise.copy(exerciseId = 99))) to "missing exercise",
            base.copy(sessionExercises = listOf(exercise.copy(groupId = 99))) to "missing group",
            base.copy(setEntries = listOf(set.copy(sessionExerciseId = 99))) to "missing session exercise",
            base.copy(sessionExercises = listOf(exercise, exercise.copy(id = 24))) to "Duplicate session exercise order",
            base.copy(setEntries = listOf(set, set.copy(id = 24))) to "Duplicate set entry order",
            base.copy(setEntries = listOf(set, set.copy(id = 24, orderIndex = 5))) to "Duplicate set entry legacy record and side",
        )
        cases.forEach { (data, message) -> assertInvalid(data, message) }
    }

    @Test fun `accepts and reports v2 values the database can hold but the app never writes`() {
        val base = validV9()
        val set = base.setEntries.first()
        assertAcceptedWith(
            base.copy(setEntries = listOf(set.copy(repetitions = -1), set.copy(id = 24, orderIndex = 1, legacyTrainingRecordId = 99, side = "LEFT"))),
            BackupAnomalyKind.V2_NEGATIVE_VALUE to 22L,
            BackupAnomalyKind.V2_LEGACY_RECORD_MISSING to 24L,
        )
    }

    private fun validV10() = validV9().copy(
        version = 10,
        workoutSessions = validV9().workoutSessions + ExportWorkoutSession(
            30, "COMPLETED", "INTERVAL_TEMPLATE", sourceTemplateId = 4, sourceNameSnapshot = "Tabata",
            startedAtEpochMillis = 3_000, updatedAtEpochMillis = 4_000, timePrecision = "EXACT",
            intervalWorkSeconds = 20, intervalRestSeconds = 10, intervalRounds = 8, intervalRoundRestSeconds = 60,
        ),
    )

    @Test fun `accepts a version 10 backup with interval settings`() {
        val result = service.parse(json.encodeToString(validV10()))
        assertTrue("Expected success, got $result", result is BackupResult.Success)
        assertEquals(ParsedBackup(validV10(), emptyList()), (result as BackupResult.Success).value)
    }

    @Test fun `a negative interval setting is accepted and reported`() {
        val base = validV10()
        val interval = base.workoutSessions.last()
        assertAcceptedWith(
            base.copy(workoutSessions = base.workoutSessions.dropLast(1) + interval.copy(intervalRestSeconds = -10)),
            BackupAnomalyKind.V2_NEGATIVE_VALUE to 30L,
        )
    }

    @Test fun `a v2 exercise whose library links are null is valid`() {
        val base = validV9()
        val detached = base.copy(sessionExercises = listOf(base.sessionExercises.first().copy(exerciseId = null, groupId = null)))
        assertAcceptedWith(detached)
    }

    // Format 11 (ADR 0007): catalogue links and custom exercises placed in built-in chains.
    private fun validV11() = validV10().let { base ->
        base.copy(
            version = 11,
            exercises = base.exercises.mapIndexed { index, e -> if (index == 0) e.copy(catalogId = "pull.full") else e } +
                ExportExercise(90, "Towel pull-up", "Dynamic", null, 0, laterality = "Bilateral"),
            chainPlacements = listOf(ExportChainPlacement(90, "pull", afterStepId = "pull.full")),
        )
    }

    @Test fun `accepts a version 11 backup with catalogue links and placements`() {
        val result = service.parse(json.encodeToString(validV11()))
        assertTrue("Expected success, got $result", result is BackupResult.Success)
        assertEquals(ParsedBackup(validV11(), emptyList()), (result as BackupResult.Success).value)
    }

    @Test fun `rejects what the progression tables would refuse`() {
        val base = validV11()
        assertInvalid(
            base.copy(exercises = base.exercises.map { if (it.id == 90L) it.copy(catalogId = "pull.full") else it }),
            "catalogue id",
        )
        assertInvalid(base.copy(chainPlacements = base.chainPlacements + ExportChainPlacement(90, "pull")), "chain placement")
        assertInvalid(base.copy(chainPlacements = listOf(ExportChainPlacement(404, "pull"))), "missing exercise 404")
    }

    @Test fun `an older backup restores with no links`() {
        val result = service.parse(json.encodeToString(validV10()))
        assertTrue(result is BackupResult.Success)
        result as BackupResult.Success
        assertTrue(result.value.data.exercises.all { it.catalogId == null })
        assertTrue(result.value.data.chainPlacements.isEmpty())
    }

    @Test fun `unknown catalogue ids are kept and reported`() {
        val base = validV11()
        assertAcceptedWith(
            base.copy(
                exercises = base.exercises.map { if (it.id == 90L) it.copy(catalogId = "push.from_the_future") else it },
                chainPlacements = listOf(ExportChainPlacement(90, "levers", afterStepId = null)),
            ),
            BackupAnomalyKind.UNKNOWN_CATALOGUE_ID to 90L,
            BackupAnomalyKind.UNKNOWN_CATALOGUE_ID to 90L,
        )
    }
}
