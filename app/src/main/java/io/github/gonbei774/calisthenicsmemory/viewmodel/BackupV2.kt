package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.data.v2.BodySide
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.v2.Laterality
import io.github.gonbei774.calisthenicsmemory.data.v2.SessionExerciseEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.TimePrecision
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSourceType
import io.github.gonbei774.calisthenicsmemory.data.v2.codeOf
import kotlinx.serialization.Serializable

// v2 workout history in the JSON backup (format 9). Enums are written as their stable
// database codes, so the file and the database use the same vocabulary.

@Serializable
data class ExportWorkoutSession(
    val id: Long,
    val status: String,
    val sourceType: String,
    val sourceTemplateId: Long? = null,
    val sourceNameSnapshot: String? = null,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long,
    val timePrecision: String,
    val comment: String? = null,
    // Format 10: interval settings of INTERVAL_TEMPLATE sessions.
    val intervalWorkSeconds: Int? = null,
    val intervalRestSeconds: Int? = null,
    val intervalRounds: Int? = null,
    val intervalRoundRestSeconds: Int? = null,
)

@Serializable
data class ExportSessionExercise(
    val id: Long,
    val workoutSessionId: Long,
    val orderIndex: Int,
    val exerciseId: Long? = null,
    val groupId: Long? = null,
    val sourceProgramExerciseId: Long? = null,
    val exerciseNameSnapshot: String,
    val exerciseKindSnapshot: String,
    val lateralitySnapshot: String,
    val groupNameSnapshot: String? = null,
    val targetSets: Int? = null,
    val targetRepetitions: Int? = null,
    val targetDurationMillis: Long? = null,
)

@Serializable
data class ExportSetEntry(
    val id: Long,
    val sessionExerciseId: Long,
    val orderIndex: Int,
    val setNumber: Int,
    val roundNumber: Int? = null,
    val status: String,
    val side: String,
    val repetitions: Int? = null,
    val durationMillis: Long? = null,
    val distanceCm: Int? = null,
    val addedWeightGrams: Int? = null,
    val assistanceGrams: Int? = null,
    val targetRepetitions: Int? = null,
    val targetDurationMillis: Long? = null,
    val startedAtEpochMillis: Long? = null,
    val completedAtEpochMillis: Long? = null,
    val timePrecision: String,
    val comment: String? = null,
    val legacyTrainingRecordId: Long? = null,
)

internal fun WorkoutSessionEntity.toExport() = ExportWorkoutSession(
    id, status.code, sourceType.code, sourceTemplateId, sourceNameSnapshot, startedAtEpochMillis,
    endedAtEpochMillis, updatedAtEpochMillis, timePrecision.code, comment,
    intervalWorkSeconds, intervalRestSeconds, intervalRounds, intervalRoundRestSeconds,
)

internal fun SessionExerciseEntity.toExport() = ExportSessionExercise(
    id, workoutSessionId, orderIndex, exerciseId, groupId, sourceProgramExerciseId, exerciseNameSnapshot,
    exerciseKindSnapshot.code, lateralitySnapshot.code, groupNameSnapshot, targetSets, targetRepetitions, targetDurationMillis,
)

internal fun SetEntryEntity.toExport() = ExportSetEntry(
    id, sessionExerciseId, orderIndex, setNumber, roundNumber, status.code, side.code, repetitions, durationMillis,
    distanceCm, addedWeightGrams, assistanceGrams, targetRepetitions, targetDurationMillis, startedAtEpochMillis,
    completedAtEpochMillis, timePrecision.code, comment, legacyTrainingRecordId,
)

// These throw IllegalArgumentException for unknown codes; validation rejects those first.
internal fun ExportWorkoutSession.toEntity() = WorkoutSessionEntity(
    id, codeOf<WorkoutSessionStatus>(status), codeOf<WorkoutSourceType>(sourceType), sourceTemplateId, sourceNameSnapshot,
    startedAtEpochMillis, endedAtEpochMillis, updatedAtEpochMillis, codeOf<TimePrecision>(timePrecision), comment,
    intervalWorkSeconds, intervalRestSeconds, intervalRounds, intervalRoundRestSeconds,
)

internal fun ExportSessionExercise.toEntity() = SessionExerciseEntity(
    id, workoutSessionId, orderIndex, exerciseId, groupId, sourceProgramExerciseId, exerciseNameSnapshot,
    codeOf<ExerciseKind>(exerciseKindSnapshot), codeOf<Laterality>(lateralitySnapshot), groupNameSnapshot,
    targetSets, targetRepetitions, targetDurationMillis,
)

internal fun ExportSetEntry.toEntity() = SetEntryEntity(
    id, sessionExerciseId, orderIndex, setNumber, roundNumber, codeOf<SetEntryStatus>(status), codeOf<BodySide>(side),
    repetitions, durationMillis, distanceCm, addedWeightGrams, assistanceGrams, targetRepetitions, targetDurationMillis,
    startedAtEpochMillis, completedAtEpochMillis, codeOf<TimePrecision>(timePrecision), comment, legacyTrainingRecordId,
)

/** Returns the first code in the v2 collections that no enum knows, as an error message. */
internal fun unknownV2Code(data: BackupData): String? {
    fun <T> check(label: String, id: Long, code: String, decode: (String) -> T): String? =
        try { decode(code); null } catch (e: IllegalArgumentException) { "$label $id has unknown code $code" }
    data.workoutSessions.forEach {
        check("Workout session", it.id, it.status) { c -> codeOf<WorkoutSessionStatus>(c) }?.let { e -> return e }
        check("Workout session", it.id, it.sourceType) { c -> codeOf<WorkoutSourceType>(c) }?.let { e -> return e }
        check("Workout session", it.id, it.timePrecision) { c -> codeOf<TimePrecision>(c) }?.let { e -> return e }
    }
    data.sessionExercises.forEach {
        check("Session exercise", it.id, it.exerciseKindSnapshot) { c -> codeOf<ExerciseKind>(c) }?.let { e -> return e }
        check("Session exercise", it.id, it.lateralitySnapshot) { c -> codeOf<Laterality>(c) }?.let { e -> return e }
    }
    data.setEntries.forEach {
        check("Set entry", it.id, it.status) { c -> codeOf<SetEntryStatus>(c) }?.let { e -> return e }
        check("Set entry", it.id, it.side) { c -> codeOf<BodySide>(c) }?.let { e -> return e }
        check("Set entry", it.id, it.timePrecision) { c -> codeOf<TimePrecision>(c) }?.let { e -> return e }
    }
    return null
}
