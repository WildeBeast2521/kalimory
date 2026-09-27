package io.github.gonbei774.calisthenicsmemory.data.v2

import androidx.room.withTransaction
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase

/** One set of a finished single-exercise workout, as the workout screen holds it. */
data class SingleWorkoutSet(
    val setNumber: Int,
    val side: BodySide,
    /** Repetitions, or whole seconds for isometric exercises; 0 means the set was not done. */
    val value: Int,
    val targetValue: Int,
    val distanceCm: Int?,
    val weightG: Int?,
    val assistanceG: Int?,
    /** When the set was observed to finish; null when it was not timed (for example typed in afterwards). */
    val completedAtWallMillis: Long?,
)

/** A finished single-exercise workout, ready to be written as one v2 session. */
data class SingleWorkout(
    val exerciseId: Long,
    val exerciseName: String,
    val kind: ExerciseKind,
    val laterality: Laterality,
    val groupId: Long?,
    val groupName: String?,
    val targetSets: Int,
    val targetValue: Int,
    val sets: List<SingleWorkoutSet>,
    val comment: String,
    /** When the first set (or its countdown) began; null when that was not observed. */
    val startedAtWallMillis: Long?,
    val savedAtWallMillis: Long,
) {
    /** Sets with a value, as the legacy save counted them. */
    val completedSetCount: Int get() = sets.filter { it.value > 0 }.map { it.setNumber }.distinct().size
}

/** The rows [SingleWorkoutWriter.write] inserts, built without touching the database. */
data class SingleWorkoutRows(
    val session: WorkoutSessionEntity,
    val exercise: SessionExerciseEntity,
    /** [SetEntryEntity.sessionExerciseId] is filled in when written. */
    val sets: List<SetEntryEntity>,
)

object SingleWorkoutWriter {
    /**
     * Maps a finished workout to v2 rows. Nothing is invented: a set with a value is
     * COMPLETED and one without is SKIPPED (the legacy save dropped it); set times are
     * only those observed; and when the start was not observed, the save time stands in
     * for it and the session is marked MINUTE precision rather than EXACT.
     */
    fun rows(workout: SingleWorkout): SingleWorkoutRows {
        require(workout.sets.all { it.value >= 0 }) { "Values must not be negative" }
        val start = workout.startedAtWallMillis
        val session = WorkoutSessionEntity(
            status = WorkoutSessionStatus.COMPLETED,
            sourceType = WorkoutSourceType.AD_HOC,
            startedAtEpochMillis = start ?: workout.savedAtWallMillis,
            endedAtEpochMillis = workout.savedAtWallMillis,
            updatedAtEpochMillis = workout.savedAtWallMillis,
            timePrecision = if (start != null) TimePrecision.EXACT else TimePrecision.MINUTE,
            comment = workout.comment.ifBlank { null },
        )
        val isometric = workout.kind == ExerciseKind.ISOMETRIC
        val exercise = SessionExerciseEntity(
            workoutSessionId = 0,
            orderIndex = 0,
            exerciseId = workout.exerciseId,
            groupId = workout.groupId,
            exerciseNameSnapshot = workout.exerciseName,
            exerciseKindSnapshot = workout.kind,
            lateralitySnapshot = workout.laterality,
            groupNameSnapshot = workout.groupName,
            targetSets = workout.targetSets,
            targetRepetitions = if (isometric) null else workout.targetValue,
            targetDurationMillis = if (isometric) workout.targetValue * 1_000L else null,
        )
        val sets = workout.sets.mapIndexed { index, set ->
            val done = set.value > 0
            SetEntryEntity(
                sessionExerciseId = 0,
                orderIndex = index,
                setNumber = set.setNumber,
                status = if (done) SetEntryStatus.COMPLETED else SetEntryStatus.SKIPPED,
                side = set.side,
                repetitions = if (done && !isometric) set.value else null,
                durationMillis = if (done && isometric) set.value * 1_000L else null,
                distanceCm = set.distanceCm,
                addedWeightGrams = set.weightG,
                assistanceGrams = set.assistanceG,
                targetRepetitions = if (isometric) null else set.targetValue,
                targetDurationMillis = if (isometric) set.targetValue * 1_000L else null,
                completedAtEpochMillis = if (done) set.completedAtWallMillis else null,
                timePrecision = if (done && set.completedAtWallMillis != null) TimePrecision.EXACT else TimePrecision.MINUTE,
            )
        }
        return SingleWorkoutRows(session, exercise, sets)
    }

    /** Writes the workout as one session in one transaction; returns the session id, or null when no set was done. */
    suspend fun write(database: AppDatabase, workout: SingleWorkout): Long? {
        if (workout.completedSetCount == 0) return null
        val rows = rows(workout)
        val dao = database.workoutSessionDao()
        return database.withTransaction {
            val sessionId = dao.insertSession(rows.session)
            val exerciseId = dao.insertSessionExercise(rows.exercise.copy(workoutSessionId = sessionId))
            dao.insertSetEntries(rows.sets.map { it.copy(sessionExerciseId = exerciseId) })
            sessionId
        }
    }
}
