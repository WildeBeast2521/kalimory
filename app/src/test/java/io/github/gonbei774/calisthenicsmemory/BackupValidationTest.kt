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

    @Test fun `rejects every broken reference class`() {
        val base = valid()
        val cases = listOf(
            base.copy(exercises = listOf(base.exercises.first().copy(group = "Missing"))) to "missing group",
            base.copy(records = listOf(base.records.first().copy(exerciseId = 99))) to "missing exercise",
            base.copy(programLoops = listOf(base.programLoops.first().copy(programId = 99))) to "missing program",
            base.copy(programExercises = listOf(base.programExercises.first().copy(programId = 99, loopId = null))) to "missing program",
            base.copy(programExercises = listOf(base.programExercises.first().copy(exerciseId = 99))) to "missing exercise",
            base.copy(programExercises = listOf(base.programExercises.first().copy(loopId = 99))) to "missing loop",
            base.copy(intervalProgramExercises = listOf(base.intervalProgramExercises.first().copy(programId = 99))) to "missing interval program",
            base.copy(intervalProgramExercises = listOf(base.intervalProgramExercises.first().copy(exerciseId = 99))) to "missing exercise",
            base.copy(todoTasks = listOf(ExportTodoTask(10, "EXERCISE", 99, 0))) to "missing exercise",
            base.copy(todoTasks = listOf(ExportTodoTask(10, "GROUP", 99, 0))) to "missing group",
            base.copy(todoTasks = listOf(ExportTodoTask(10, "PROGRAM", 99, 0))) to "missing program",
            base.copy(todoTasks = listOf(ExportTodoTask(10, "INTERVAL", 99, 0))) to "missing interval",
            base.copy(todoTasks = listOf(ExportTodoTask(10, "UNKNOWN", 99, 0))) to "unknown type"
        )
        cases.forEach { (data, message) -> assertInvalid(data, message) }
    }

    @Test fun `rejects a loop belonging to another program`() {
        val base = valid()
        assertInvalid(
            base.copy(programExercises = listOf(base.programExercises.first().copy(programId = 14))),
            "another program"
        )
    }

    @Test fun `accepts legacy defaults`() {
        val legacy = """{"version":1,"exportDate":"old","app":"CalisthenicsMemory","groups":[],"exercises":[],"records":[]}"""
        val result = service.parse(legacy)
        assertTrue(result is BackupResult.Success)
        assertTrue((result as BackupResult.Success).value.programs.isEmpty())
        assertTrue(result.value.todoTasks.isEmpty())
    }

    @Test fun `accepts a complete version 8 backup`() {
        val result = service.parse(json.encodeToString(valid()))
        assertTrue("Expected success, got $result", result is BackupResult.Success)
        assertEquals(valid(), (result as BackupResult.Success).value)
    }
}
