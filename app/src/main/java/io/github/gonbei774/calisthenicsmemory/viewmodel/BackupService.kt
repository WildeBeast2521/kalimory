package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.data.BackupDao
import io.github.gonbei774.calisthenicsmemory.data.BackupSnapshot
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalRecord
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class BackupFailureKind { INVALID_JSON, VALIDATION, DATABASE }

sealed interface BackupResult<out T> {
    data class Success<T>(val value: T) : BackupResult<T>
    data class Failure(
        val kind: BackupFailureKind,
        val message: String,
        val cause: Throwable? = null
    ) : BackupResult<Nothing>
}

data class BackupSummary(
    val groups: Int,
    val exercises: Int,
    val records: Int,
    val programs: Int,
    val programExercises: Int,
    val programLoops: Int,
    val intervalPrograms: Int,
    val intervalProgramExercises: Int,
    val intervalRecords: Int,
    val todoTasks: Int
)

/**
 * A value the database can hold but that does not match what the app writes.
 * Such rows are restored unchanged and reported rather than rejected, so every
 * backup this app exports can be restored.
 */
enum class BackupAnomalyKind {
    EXERCISE_MISSING_GROUP,
    TODO_MISSING_TARGET,
    TODO_UNKNOWN_TYPE,
    TODO_INVALID_REPEAT_DAYS,
    PROGRAM_EXERCISE_FOREIGN_LOOP,
}

data class BackupAnomaly(val kind: BackupAnomalyKind, val entityId: Long, val detail: String)

data class ParsedBackup(val data: BackupData, val anomalies: List<BackupAnomaly>)

data class ExportedBackup(
    val data: BackupData,
    val json: String,
    val summary: BackupSummary
)

class BackupService(
    private val dao: BackupDao,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val now: () -> LocalDateTime = LocalDateTime::now
) {
    suspend fun export(): BackupResult<ExportedBackup> = try {
        val snapshot = dao.snapshot()
        val data = snapshot.toBackupData(
            now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        BackupResult.Success(ExportedBackup(data, json.encodeToString(data), data.summary()))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        BackupResult.Failure(BackupFailureKind.DATABASE, e.message ?: "Backup export failed", e)
    }

    fun parse(value: String): BackupResult<ParsedBackup> {
        val data = try {
            json.decodeFromString<BackupData>(value)
        } catch (e: SerializationException) {
            return BackupResult.Failure(BackupFailureKind.INVALID_JSON, e.message ?: "Invalid JSON", e)
        } catch (e: IllegalArgumentException) {
            return BackupResult.Failure(BackupFailureKind.INVALID_JSON, e.message ?: "Invalid JSON", e)
        }
        return validate(data)?.let {
            BackupResult.Failure(BackupFailureKind.VALIDATION, it)
        } ?: BackupResult.Success(ParsedBackup(data, anomalies(data)))
    }

    suspend fun restore(data: BackupData): BackupResult<BackupSummary> {
        validate(data)?.let {
            return BackupResult.Failure(BackupFailureKind.VALIDATION, it)
        }
        return try {
            dao.replaceAll(data.toSnapshot())
            BackupResult.Success(data.summary())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BackupResult.Failure(BackupFailureKind.DATABASE, e.message ?: "Backup restore failed", e)
        }
    }

    private fun validate(data: BackupData): String? {
        if (data.app != APP_NAME) return "Unexpected backup app: ${data.app}"
        if (data.version !in MIN_VERSION..CURRENT_VERSION) return "Unsupported backup version: ${data.version}"

        duplicateId("group", data.groups.map { it.id })?.let { return it }
        duplicateId("exercise", data.exercises.map { it.id })?.let { return it }
        duplicateId("record", data.records.map { it.id })?.let { return it }
        duplicateId("program", data.programs.map { it.id })?.let { return it }
        duplicateId("program exercise", data.programExercises.map { it.id })?.let { return it }
        duplicateId("program loop", data.programLoops.map { it.id })?.let { return it }
        duplicateId("interval program", data.intervalPrograms.map { it.id })?.let { return it }
        duplicateId("interval program exercise", data.intervalProgramExercises.map { it.id })?.let { return it }
        duplicateId("interval record", data.intervalRecords.map { it.id })?.let { return it }
        duplicateId("todo task", data.todoTasks.map { it.id })?.let { return it }

        duplicateKey("group name", data.groups.map { it.name })?.let { return it }
        duplicateKey("exercise name/type", data.exercises.map { it.name to it.type })?.let { return it }

        val exerciseIds = data.exercises.mapTo(hashSetOf()) { it.id }
        val programIds = data.programs.mapTo(hashSetOf()) { it.id }
        val loopIds = data.programLoops.mapTo(hashSetOf()) { it.id }
        val intervalProgramIds = data.intervalPrograms.mapTo(hashSetOf()) { it.id }

        // These references are foreign keys in the database, so no exported backup can break them.
        data.records.firstOrNull { it.exerciseId !in exerciseIds }?.let {
            return "Record ${it.id} references missing exercise ${it.exerciseId}"
        }
        data.programLoops.firstOrNull { it.programId !in programIds }?.let {
            return "Program loop ${it.id} references missing program ${it.programId}"
        }
        data.programExercises.forEach {
            if (it.programId !in programIds) return "Program exercise ${it.id} references missing program ${it.programId}"
            if (it.exerciseId !in exerciseIds) return "Program exercise ${it.id} references missing exercise ${it.exerciseId}"
            if (it.loopId != null && it.loopId !in loopIds) return "Program exercise ${it.id} references missing loop ${it.loopId}"
        }
        data.intervalProgramExercises.forEach {
            if (it.programId !in intervalProgramIds) return "Interval program exercise ${it.id} references missing interval program ${it.programId}"
            if (it.exerciseId !in exerciseIds) return "Interval program exercise ${it.id} references missing exercise ${it.exerciseId}"
        }
        return null
    }

    /** Values without a database constraint that differ from what the app writes; restored unchanged. */
    fun anomalies(data: BackupData): List<BackupAnomaly> {
        val anomalies = mutableListOf<BackupAnomaly>()
        val groupNames = data.groups.mapTo(hashSetOf()) { it.name }
        val groupIds = data.groups.mapTo(hashSetOf()) { it.id }
        val exerciseIds = data.exercises.mapTo(hashSetOf()) { it.id }
        val programIds = data.programs.mapTo(hashSetOf()) { it.id }
        val loopById = data.programLoops.associateBy { it.id }
        val intervalProgramIds = data.intervalPrograms.mapTo(hashSetOf()) { it.id }

        data.exercises.filter { it.group != null && it.group !in groupNames }.forEach {
            anomalies += BackupAnomaly(BackupAnomalyKind.EXERCISE_MISSING_GROUP, it.id, "Exercise ${it.id} references missing group ${it.group}")
        }
        data.programExercises.forEach {
            val loop = it.loopId?.let(loopById::get)
            if (loop != null && loop.programId != it.programId) {
                anomalies += BackupAnomaly(
                    BackupAnomalyKind.PROGRAM_EXERCISE_FOREIGN_LOOP, it.id,
                    "Program exercise ${it.id} in program ${it.programId} uses loop ${loop.id} of program ${loop.programId}",
                )
            }
        }
        data.todoTasks.forEach {
            if (!TodoTask.isValidRepeatDays(it.repeatDays)) {
                anomalies += BackupAnomaly(BackupAnomalyKind.TODO_INVALID_REPEAT_DAYS, it.id, "Todo task ${it.id} has invalid repeat days ${it.repeatDays}")
            }
            val todo = TodoTask(it.id, it.type, it.referenceId, it.sortOrder, it.repeatDays, it.lastCompletedDate)
            when (todo.hasTarget(exerciseIds, groupIds, programIds, intervalProgramIds)) {
                null -> anomalies += BackupAnomaly(BackupAnomalyKind.TODO_UNKNOWN_TYPE, it.id, "Todo task ${it.id} has unknown type ${it.type}")
                false -> anomalies += BackupAnomaly(
                    BackupAnomalyKind.TODO_MISSING_TARGET, it.id,
                    "Todo task ${it.id} references missing ${it.type.lowercase()} ${it.referenceId}",
                )
                true -> Unit
            }
        }
        return anomalies
    }

    private fun duplicateId(label: String, ids: List<Long>): String? {
        ids.firstOrNull { it <= 0 }?.let { return "$label id must be positive: $it" }
        return ids.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }
            ?.let { "Duplicate $label id: ${it.key}" }
    }

    private fun <T> duplicateKey(label: String, values: List<T>): String? =
        values.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }
            ?.let { "Duplicate $label: ${it.key}" }

    companion object {
        const val APP_NAME = "CalisthenicsMemory"
        const val MIN_VERSION = 1
        const val CURRENT_VERSION = 8
    }
}

private fun BackupSnapshot.toBackupData(exportDate: String) = BackupData(
    version = BackupService.CURRENT_VERSION,
    exportDate = exportDate,
    app = BackupService.APP_NAME,
    groups = groups.map { ExportGroup(it.id, it.name, it.displayOrder) },
    exercises = exercises.map { ExportExercise(it.id, it.name, it.type, it.group, it.sortOrder, it.displayOrder, it.laterality, it.targetSets, it.targetValue, it.isFavorite, it.restInterval, it.repDuration, it.distanceTrackingEnabled, it.weightTrackingEnabled, it.assistanceTrackingEnabled, it.description) },
    records = records.map { ExportRecord(it.id, it.exerciseId, it.valueRight, it.valueLeft, it.setNumber, it.date, it.time, it.comment, it.distanceCm, it.weightG, it.assistanceG) },
    programs = programs.map { ExportProgram(it.id, it.name) },
    programExercises = programExercises.map { ExportProgramExercise(it.id, it.programId, it.exerciseId, it.sortOrder, it.sets, it.targetValue, it.intervalSeconds, it.loopId) },
    programLoops = programLoops.map { ExportProgramLoop(it.id, it.programId, it.sortOrder, it.rounds, it.restBetweenRounds) },
    intervalPrograms = intervalPrograms.map { ExportIntervalProgram(it.id, it.name, it.workSeconds, it.restSeconds, it.rounds, it.roundRestSeconds) },
    intervalProgramExercises = intervalProgramExercises.map { ExportIntervalProgramExercise(it.id, it.programId, it.exerciseId, it.sortOrder) },
    intervalRecords = intervalRecords.map { ExportIntervalRecord(it.id, it.programName, it.date, it.time, it.workSeconds, it.restSeconds, it.rounds, it.roundRestSeconds, it.completedRounds, it.completedExercisesInLastRound, it.exercisesJson, it.comment) },
    todoTasks = todoTasks.map { ExportTodoTask(it.id, it.type, it.referenceId, it.sortOrder, it.repeatDays, it.lastCompletedDate) }
)

private fun BackupData.toSnapshot() = BackupSnapshot(
    groups.map { ExerciseGroup(it.id, it.name, it.displayOrder) },
    exercises.map { Exercise(it.id, it.name, it.type, it.group, it.sortOrder, it.displayOrder, it.laterality, it.targetSets, it.targetValue, it.isFavorite, it.restInterval, it.repDuration, it.distanceTrackingEnabled, it.weightTrackingEnabled, it.assistanceTrackingEnabled, it.description) },
    records.map { TrainingRecord(it.id, it.exerciseId, it.valueRight, it.valueLeft, it.setNumber, it.date, it.time, it.comment, it.distanceCm, it.weightG, it.assistanceG) },
    programs.map { Program(it.id, it.name) },
    programExercises.map { ProgramExercise(it.id, it.programId, it.exerciseId, it.sortOrder, it.sets, it.targetValue, it.intervalSeconds, it.loopId) },
    programLoops.map { ProgramLoop(it.id, it.programId, it.sortOrder, it.rounds, it.restBetweenRounds) },
    intervalPrograms.map { IntervalProgram(it.id, it.name, it.workSeconds, it.restSeconds, it.rounds, it.roundRestSeconds) },
    intervalProgramExercises.map { IntervalProgramExercise(it.id, it.programId, it.exerciseId, it.sortOrder) },
    intervalRecords.map { IntervalRecord(it.id, it.programName, it.date, it.time, it.workSeconds, it.restSeconds, it.rounds, it.roundRestSeconds, it.completedRounds, it.completedExercisesInLastRound, it.exercisesJson, it.comment) },
    todoTasks.map { TodoTask(it.id, it.type, it.referenceId, it.sortOrder, it.repeatDays, it.lastCompletedDate) }
)

private fun BackupData.summary() = BackupSummary(
    groups.size, exercises.size, records.size, programs.size, programExercises.size,
    programLoops.size, intervalPrograms.size, intervalProgramExercises.size,
    intervalRecords.size, todoTasks.size
)
