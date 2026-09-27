package io.github.gonbei774.calisthenicsmemory.data.v2

import androidx.room.withTransaction
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase

/** One recorded set of a program run, as the program screen holds it. */
data class ProgramRunSet(
    val setNumber: Int,
    /** The loop round, or null outside a loop. */
    val roundNumber: Int?,
    val side: BodySide,
    /** Repetitions, or whole seconds for isometric exercises; 0 means the set was not done. */
    val value: Int,
    val targetValue: Int,
    val distanceCm: Int?,
    val weightG: Int?,
    val assistanceG: Int?,
    /** When the set was observed to finish; null when not timed (for example ticked off afterwards). */
    val completedAtWallMillis: Long?,
)

/** One exercise of the program as run, with its recorded sets in execution order. */
data class ProgramRunExercise(
    val programExerciseId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val kind: ExerciseKind,
    val laterality: Laterality,
    val groupId: Long?,
    val groupName: String?,
    val targetSets: Int,
    val targetValue: Int,
    val sets: List<ProgramRunSet>,
)

/** A finished program run, ready to be written as one v2 session. */
data class ProgramRun(
    val programId: Long,
    val programName: String,
    val exercises: List<ProgramRunExercise>,
    val comment: String,
    /** When the first set (or its countdown) began; null when that was not observed. */
    val startedAtWallMillis: Long?,
    val savedAtWallMillis: Long,
) {
    /** Sets with a value, one per set however many sides, as the legacy save counted them. */
    val completedSetCount: Int
        get() = exercises.sumOf { exercise ->
            exercise.sets.filter { it.value > 0 }.map { it.roundNumber to it.setNumber }.distinct().size
        }
}

/** The rows [ProgramWorkoutWriter.write] inserts; the ids linking them are filled in when written. */
data class ProgramWorkoutRows(
    val session: WorkoutSessionEntity,
    val exercises: List<Pair<SessionExerciseEntity, List<SetEntryEntity>>>,
)

object ProgramWorkoutWriter {
    /**
     * Maps a program run to v2 rows, following the single-workout rules: a set with a value is
     * COMPLETED and one without is SKIPPED, set times are only those observed, and an
     * unobserved start falls back to the save time at MINUTE precision. Exercises with no
     * recorded set are left out, as the legacy save left them out.
     */
    fun rows(run: ProgramRun): ProgramWorkoutRows {
        require(run.exercises.all { exercise -> exercise.sets.all { it.value >= 0 } }) { "Values must not be negative" }
        val start = run.startedAtWallMillis
        val session = WorkoutSessionEntity(
            status = WorkoutSessionStatus.COMPLETED,
            sourceType = WorkoutSourceType.PROGRAM_TEMPLATE,
            sourceTemplateId = run.programId,
            sourceNameSnapshot = run.programName,
            startedAtEpochMillis = start ?: run.savedAtWallMillis,
            endedAtEpochMillis = run.savedAtWallMillis,
            updatedAtEpochMillis = run.savedAtWallMillis,
            timePrecision = if (start != null) TimePrecision.EXACT else TimePrecision.MINUTE,
            comment = run.comment.ifBlank { null },
        )
        val exercises = run.exercises.filter { it.sets.isNotEmpty() }.mapIndexed { order, exercise ->
            val isometric = exercise.kind == ExerciseKind.ISOMETRIC
            SessionExerciseEntity(
                workoutSessionId = 0,
                orderIndex = order,
                exerciseId = exercise.exerciseId,
                groupId = exercise.groupId,
                sourceProgramExerciseId = exercise.programExerciseId,
                exerciseNameSnapshot = exercise.exerciseName,
                exerciseKindSnapshot = exercise.kind,
                lateralitySnapshot = exercise.laterality,
                groupNameSnapshot = exercise.groupName,
                targetSets = exercise.targetSets,
                targetRepetitions = if (isometric) null else exercise.targetValue,
                targetDurationMillis = if (isometric) exercise.targetValue * 1_000L else null,
            ) to exercise.sets.mapIndexed { index, set ->
                val done = set.value > 0
                SetEntryEntity(
                    sessionExerciseId = 0,
                    orderIndex = index,
                    setNumber = set.setNumber,
                    roundNumber = set.roundNumber,
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
        }
        return ProgramWorkoutRows(session, exercises)
    }

    /** Writes the run as one session in one transaction; returns its id, or null when no set was done. */
    suspend fun write(database: AppDatabase, run: ProgramRun): Long? {
        if (run.completedSetCount == 0) return null
        val rows = rows(run)
        val dao = database.workoutSessionDao()
        return database.withTransaction {
            val sessionId = dao.insertSession(rows.session)
            rows.exercises.forEach { (exercise, sets) ->
                val exerciseId = dao.insertSessionExercise(exercise.copy(workoutSessionId = sessionId))
                dao.insertSetEntries(sets.map { it.copy(sessionExerciseId = exerciseId) })
            }
            sessionId
        }
    }
}
