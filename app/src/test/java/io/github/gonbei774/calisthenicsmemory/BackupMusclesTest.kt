package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.data.BackupDao
import io.github.gonbei774.calisthenicsmemory.data.BackupSnapshot
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupData
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupFailureKind
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupResult
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupService
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

/** The muscles chosen for an own exercise travel in backups, format 13 (ADR 0014). */
class BackupMusclesTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val curl = Exercise(
        id = 1, name = "Biceps curl", type = "Dynamic",
        primaryMuscles = linkedSetOf(Muscle.BICEPS), secondaryMuscles = linkedSetOf(Muscle.FOREARMS, Muscle.LOWER_BACK),
    )
    private val plain = Exercise(id = 2, name = "Plank", type = "Isometric")

    private fun snapshot() = BackupSnapshot(
        groups = emptyList(), exercises = listOf(curl, plain), records = emptyList(), programs = emptyList(),
        programExercises = emptyList(), programLoops = emptyList(), intervalPrograms = emptyList(),
        intervalProgramExercises = emptyList(), intervalRecords = emptyList(), todoTasks = emptyList(),
    )

    @Test fun `an export holds the chosen muscles as codes and restores them`() = runBlocking {
        val dao = mock<BackupDao> { onBlocking { snapshot() } doReturn snapshot() }
        val service = BackupService(dao)
        val exported = (service.export() as BackupResult.Success).value
        assertEquals(14, exported.data.version)
        val exportedCurl = exported.data.exercises.single { it.id == 1L }
        assertEquals(listOf("biceps"), exportedCurl.primaryMuscles)
        assertEquals(listOf("forearms", "lower back"), exportedCurl.secondaryMuscles)
        assertTrue(exported.data.exercises.single { it.id == 2L }.primaryMuscles.isEmpty())

        val parsed = (service.parse(exported.json) as BackupResult.Success).value.data
        assertTrue(service.restore(parsed) is BackupResult.Success)
        val restored = argumentCaptor<BackupSnapshot>().also { verify(dao).replaceAll(it.capture()) }.firstValue.exercises
        assertEquals(setOf(Muscle.BICEPS), restored.single { it.id == 1L }.primaryMuscles)
        assertEquals(setOf(Muscle.FOREARMS, Muscle.LOWER_BACK), restored.single { it.id == 1L }.secondaryMuscles)
        assertTrue(restored.single { it.id == 2L }.primaryMuscles.isEmpty())
    }

    private fun backup(version: Int, vararg exercises: ExportExercise) = BackupData(
        version = version, exportDate = "2026-11-25T08:00:00", app = BackupService.APP_NAME, groups = emptyList(), exercises = exercises.toList(), records = emptyList(),
    )

    @Test fun `a backup from before muscles existed restores with no muscles chosen`() = runBlocking {
        val dao = mock<BackupDao>()
        val service = BackupService(dao)
        val old = backup(12, ExportExercise(1, "Push-up", "Dynamic", null, 0, laterality = "Bilateral"))
        val parsed = (service.parse(json.encodeToString(old)) as BackupResult.Success).value.data
        assertTrue(service.restore(parsed) is BackupResult.Success)
        val restored = argumentCaptor<BackupSnapshot>().also { verify(dao).replaceAll(it.capture()) }.firstValue.exercises.single()
        assertTrue(restored.primaryMuscles.isEmpty() && restored.secondaryMuscles.isEmpty())
    }

    @Test fun `a backup naming an unknown muscle is refused rather than stored`() {
        val service = BackupService(mock<BackupDao>())
        val bad = backup(14, ExportExercise(1, "Curl", "Dynamic", null, 0, laterality = "Bilateral", primaryMuscles = listOf("biceps", "wings")))
        val result = service.parse(json.encodeToString(bad))
        assertTrue(result is BackupResult.Failure)
        result as BackupResult.Failure
        assertEquals(BackupFailureKind.VALIDATION, result.kind)
        assertTrue(result.message, result.message.contains("unknown muscle wings"))
    }

    @Test fun `a backup newer than this version is still refused`() {
        val result = BackupService(mock<BackupDao>()).parse(json.encodeToString(backup(15)))
        assertTrue(result is BackupResult.Failure)
    }
}
