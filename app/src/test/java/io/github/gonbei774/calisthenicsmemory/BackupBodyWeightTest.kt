package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.data.BackupDao
import io.github.gonbei774.calisthenicsmemory.data.BackupSnapshot
import io.github.gonbei774.calisthenicsmemory.data.BodyWeight
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupData
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupFailureKind
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupResult
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupService
import io.github.gonbei774.calisthenicsmemory.viewmodel.ExportBodyWeight
import io.github.gonbei774.calisthenicsmemory.viewmodel.ExportExercise
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/** Body weights and the share of body weight travel in backups, format 14 (ADR 0016). */
class BackupBodyWeightTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun snapshot() = BackupSnapshot(
        groups = emptyList(), exercises = listOf(Exercise(1, "Push-up", "Dynamic", bodyWeightPercent = 65), Exercise(2, "Plank", "Isometric")),
        records = emptyList(), programs = emptyList(), programExercises = emptyList(), programLoops = emptyList(),
        intervalPrograms = emptyList(), intervalProgramExercises = emptyList(), intervalRecords = emptyList(), todoTasks = emptyList(),
        bodyWeights = listOf(BodyWeight(1, "2026-10-12", 65_000), BodyWeight(2, "2026-11-11", 62_850, importId = 99)),
    )

    private fun backup(version: Int = 14, weights: List<ExportBodyWeight> = emptyList(), percent: Int? = null) = BackupData(
        version = version, exportDate = "2026-11-26T08:00:00", app = BackupService.APP_NAME, groups = emptyList(),
        exercises = listOf(ExportExercise(1, "Push-up", "Dynamic", null, 0, laterality = "Bilateral", bodyWeightPercent = percent)),
        records = emptyList(), bodyWeights = weights,
    )

    private fun parse(data: BackupData) = BackupService(mock<BackupDao>()).parse(json.encodeToString(data))

    private fun assertInvalid(data: BackupData, text: String) {
        val result = parse(data)
        assertTrue("Expected a refusal for $text, got $result", result is BackupResult.Failure)
        result as BackupResult.Failure
        assertEquals(BackupFailureKind.VALIDATION, result.kind)
        assertTrue(result.message, result.message.contains(text, ignoreCase = true))
    }

    @Test fun `an export holds the list and the share, without the import marker, and restores them`() = runBlocking {
        val dao = mock<BackupDao> { onBlocking { snapshot() } doReturn snapshot() }
        val service = BackupService(dao)
        val exported = (service.export() as BackupResult.Success).value
        assertEquals(14, exported.data.version)
        assertEquals(listOf("2026-10-12" to 65_000, "2026-11-11" to 62_850), exported.data.bodyWeights.map { it.date to it.weightG })
        assertEquals(65, exported.data.exercises.single { it.id == 1L }.bodyWeightPercent)
        assertTrue("an import marker must not travel", !exported.json.contains("importId"))

        val parsed = (service.parse(exported.json) as BackupResult.Success).value.data
        assertTrue(service.restore(parsed) is BackupResult.Success)
        val restored = argumentCaptor<BackupSnapshot>().also { verify(dao).replaceAll(it.capture()) }.firstValue
        assertEquals(65, restored.exercises.single { it.id == 1L }.bodyWeightPercent)
        assertEquals(listOf(BodyWeight(1, "2026-10-12", 65_000), BodyWeight(2, "2026-11-11", 62_850)), restored.bodyWeights)
    }

    @Test fun `a backup from before body weights restores with an empty list and no share`() = runBlocking {
        val dao = mock<BackupDao>()
        val service = BackupService(dao)
        val parsed = (service.parse(json.encodeToString(backup(version = 13))) as BackupResult.Success).value.data
        assertTrue(service.restore(parsed) is BackupResult.Success)
        val restored = argumentCaptor<BackupSnapshot>().also { verify(dao).replaceAll(it.capture()) }.firstValue
        assertTrue(restored.bodyWeights.isEmpty())
        assertEquals(null, restored.exercises.single().bodyWeightPercent)
    }

    @Test fun `two entries for one date are refused`() =
        assertInvalid(backup(weights = listOf(ExportBodyWeight(1, "2026-11-11", 60_000), ExportBodyWeight(2, "2026-11-11", 61_000))), "date")

    @Test fun `an unreadable date is refused`() =
        assertInvalid(backup(weights = listOf(ExportBodyWeight(1, "11/11/2026", 60_000))), "unreadable date")

    @Test fun `a weight of nothing or an absurd weight is refused`() {
        assertInvalid(backup(weights = listOf(ExportBodyWeight(1, "2026-11-11", 0))), "out of range")
        assertInvalid(backup(weights = listOf(ExportBodyWeight(1, "2026-11-11", 600_000))), "out of range")
    }

    @Test fun `a share outside 1 to 100 percent is refused`() {
        assertInvalid(backup(percent = 0), "percent")
        assertInvalid(backup(percent = 101), "percent")
    }

    @Test fun `a duplicate entry id is refused`() =
        assertInvalid(backup(weights = listOf(ExportBodyWeight(1, "2026-11-11", 60_000), ExportBodyWeight(1, "2026-11-12", 61_000))), "duplicate body weight")
}
