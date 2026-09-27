package io.github.gonbei774.calisthenicsmemory.data.v2

import androidx.room.withTransaction
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase

/** One exercise of an interval program as it ran. [exerciseId] is null when that exercise no longer exists. */
data class IntervalRunExercise(
    val exerciseId: Long?,
    val name: String,
    val kind: ExerciseKind,
    val laterality: Laterality,
    val groupId: Long?,
    val groupName: String?,
)

/**
 * A finished or stopped interval workout. [completedRounds] and [completedExercisesInLastRound]
 * follow the legacy record: a full run is (rounds, all exercises); a stopped one is the full
 * rounds done plus how many exercises of the next round were done.
 */
data class IntervalRun(
    val programId: Long,
    val programName: String,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
    val roundRestSeconds: Int,
    val exercises: List<IntervalRunExercise>,
    val completedRounds: Int,
    val completedExercisesInLastRound: Int,
    val comment: String,
    val startedAtWallMillis: Long?,
    val savedAtWallMillis: Long,
) {
    /** The (round, exercise index) work intervals that were done, in order. */
    val completedSlots: List<Pair<Int, Int>>
        get() {
            val full = completedRounds.coerceIn(0, rounds)
            val partial = if (full >= rounds) 0 else completedExercisesInLastRound.coerceIn(0, exercises.size)
            return (1..full).flatMap { round -> exercises.indices.map { round to it } } +
                (0 until partial).map { full + 1 to it }
        }
}

object IntervalWorkoutWriter {
    /**
     * Maps an interval run to v2 rows: one occurrence per program exercise, and one COMPLETED
     * set per work interval done, carrying its round. Interval mode records neither reps nor
     * hold time, so the sets have no metric and no time; the work length is only the target.
     */
    fun rows(run: IntervalRun): ProgramWorkoutRows {
        val start = run.startedAtWallMillis
        val session = WorkoutSessionEntity(
            status = WorkoutSessionStatus.COMPLETED,
            sourceType = WorkoutSourceType.INTERVAL_TEMPLATE,
            sourceTemplateId = run.programId,
            sourceNameSnapshot = run.programName,
            startedAtEpochMillis = start ?: run.savedAtWallMillis,
            endedAtEpochMillis = run.savedAtWallMillis,
            updatedAtEpochMillis = run.savedAtWallMillis,
            timePrecision = if (start != null) TimePrecision.EXACT else TimePrecision.MINUTE,
            comment = run.comment.ifBlank { null },
            intervalWorkSeconds = run.workSeconds,
            intervalRestSeconds = run.restSeconds,
            intervalRounds = run.rounds,
            intervalRoundRestSeconds = run.roundRestSeconds,
        )
        val slots = run.completedSlots.groupBy({ it.second }, { it.first })
        val exercises = run.exercises.mapIndexed { index, exercise ->
            SessionExerciseEntity(
                workoutSessionId = 0,
                orderIndex = index,
                exerciseId = exercise.exerciseId,
                groupId = exercise.groupId,
                exerciseNameSnapshot = exercise.name,
                exerciseKindSnapshot = exercise.kind,
                lateralitySnapshot = exercise.laterality,
                groupNameSnapshot = exercise.groupName,
                targetSets = run.rounds,
                targetDurationMillis = run.workSeconds * 1_000L,
            ) to slots[index].orEmpty().mapIndexed { order, round ->
                SetEntryEntity(
                    sessionExerciseId = 0,
                    orderIndex = order,
                    setNumber = round,
                    roundNumber = round,
                    status = SetEntryStatus.COMPLETED,
                    side = BodySide.BILATERAL,
                    targetDurationMillis = run.workSeconds * 1_000L,
                    timePrecision = TimePrecision.MINUTE,
                )
            }
        }
        return ProgramWorkoutRows(session, exercises)
    }

    /** Writes the run as one session in one transaction and returns its id. */
    suspend fun write(database: AppDatabase, run: IntervalRun): Long {
        val rows = rows(run)
        val dao = database.workoutSessionDao()
        return database.withTransaction {
            val sessionId = dao.insertSession(rows.session)
            rows.exercises.forEach { (exercise, sets) ->
                val exerciseId = dao.insertSessionExercise(exercise.copy(workoutSessionId = sessionId))
                if (sets.isNotEmpty()) dao.insertSetEntries(sets.map { it.copy(sessionExerciseId = exerciseId) })
            }
            sessionId
        }
    }
}
