package io.github.gonbei774.calisthenicsmemory.data.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.room.Transaction
import androidx.room.Update

/** A workout with its exercises and their sets, in order. */
data class WorkoutSessionGraph(
    val session: WorkoutSessionEntity,
    val exercises: List<Pair<SessionExerciseEntity, List<SetEntryEntity>>>,
)

@Dao
interface WorkoutSessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSessionExercise(exercise: SessionExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSetEntries(sets: List<SetEntryEntity>)

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun session(id: Long): WorkoutSessionEntity?

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Update
    suspend fun updateSetEntries(sets: List<SetEntryEntity>)

    @Query("SELECT * FROM set_entries WHERE id IN (:ids)")
    suspend fun setEntriesByIds(ids: List<Long>): List<SetEntryEntity>

    @Query("SELECT * FROM session_exercises WHERE id IN (:ids)")
    suspend fun sessionExercisesByIds(ids: List<Long>): List<SessionExerciseEntity>

    @Query("SELECT * FROM set_entries WHERE sessionExerciseId IN (SELECT id FROM session_exercises WHERE workoutSessionId = :sessionId)")
    suspend fun setEntriesOfSession(sessionId: Long): List<SetEntryEntity>

    /** Deletes the occurrences with their sets (the sets cascade). */
    @Query("DELETE FROM session_exercises WHERE id IN (:ids)")
    suspend fun deleteSessionExercises(ids: List<Long>)

    /** Deletes those of [ids] that no longer have any exercise. */
    @Query(
        "DELETE FROM workout_sessions WHERE id IN (:ids) " +
            "AND NOT EXISTS (SELECT 1 FROM session_exercises WHERE workoutSessionId = workout_sessions.id)"
    )
    suspend fun deleteEmptySessions(ids: List<Long>)

    @Query("SELECT * FROM workout_sessions ORDER BY startedAtEpochMillis DESC, id DESC")
    suspend fun sessionsNewestFirst(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM session_exercises WHERE workoutSessionId = :sessionId ORDER BY orderIndex")
    suspend fun sessionExercises(sessionId: Long): List<SessionExerciseEntity>

    @Query("SELECT * FROM set_entries WHERE sessionExerciseId = :sessionExerciseId ORDER BY orderIndex")
    suspend fun setEntries(sessionExerciseId: Long): List<SetEntryEntity>

    @Query("SELECT DISTINCT legacyTrainingRecordId FROM set_entries WHERE legacyTrainingRecordId IS NOT NULL")
    suspend fun legacyTrainingRecordIds(): List<Long>

    /**
     * Completed v2 sets that are not copies of legacy records and whose exercise still
     * exists, for [CompatibilityHistory].
     */
    @Query(
        """
        SELECT t.id AS setEntryId, t.sessionExerciseId, e.exerciseId, e.exerciseKindSnapshot, t.setNumber,
               t.orderIndex, t.side, t.repetitions, t.durationMillis, t.distanceCm, t.addedWeightGrams,
               t.assistanceGrams, t.completedAtEpochMillis, s.startedAtEpochMillis AS sessionStartedAtEpochMillis,
               s.comment AS sessionComment, s.id AS workoutSessionId, e.orderIndex AS sessionExerciseOrderIndex,
               t.roundNumber
        FROM set_entries t
        JOIN session_exercises e ON e.id = t.sessionExerciseId
        JOIN workout_sessions s ON s.id = e.workoutSessionId
        WHERE t.legacyTrainingRecordId IS NULL AND t.status = 'COMPLETED' AND e.exerciseId IS NOT NULL
        ORDER BY s.startedAtEpochMillis, e.orderIndex, t.orderIndex
        """
    )
    suspend fun v2OnlyHistoryRows(): List<V2HistoryRow>

    /** [v2OnlyHistoryRows], re-emitted whenever the joined tables change. */
    @Query(
        """
        SELECT t.id AS setEntryId, t.sessionExerciseId, e.exerciseId, e.exerciseKindSnapshot, t.setNumber,
               t.orderIndex, t.side, t.repetitions, t.durationMillis, t.distanceCm, t.addedWeightGrams,
               t.assistanceGrams, t.completedAtEpochMillis, s.startedAtEpochMillis AS sessionStartedAtEpochMillis,
               s.comment AS sessionComment, s.id AS workoutSessionId, e.orderIndex AS sessionExerciseOrderIndex,
               t.roundNumber
        FROM set_entries t
        JOIN session_exercises e ON e.id = t.sessionExerciseId
        JOIN workout_sessions s ON s.id = e.workoutSessionId
        WHERE t.legacyTrainingRecordId IS NULL AND t.status = 'COMPLETED' AND e.exerciseId IS NOT NULL
        ORDER BY s.startedAtEpochMillis, e.orderIndex, t.orderIndex
        """
    )
    fun observeV2OnlyHistoryRows(): Flow<List<V2HistoryRow>>

    /** The v2-only rows of [exerciseId] in the newest session that has any, for previous-value prefill. */
    @Query(
        """
        SELECT t.id AS setEntryId, t.sessionExerciseId, e.exerciseId, e.exerciseKindSnapshot, t.setNumber,
               t.orderIndex, t.side, t.repetitions, t.durationMillis, t.distanceCm, t.addedWeightGrams,
               t.assistanceGrams, t.completedAtEpochMillis, s.startedAtEpochMillis AS sessionStartedAtEpochMillis,
               s.comment AS sessionComment, s.id AS workoutSessionId, e.orderIndex AS sessionExerciseOrderIndex,
               t.roundNumber
        FROM set_entries t
        JOIN session_exercises e ON e.id = t.sessionExerciseId
        JOIN workout_sessions s ON s.id = e.workoutSessionId
        WHERE t.legacyTrainingRecordId IS NULL AND t.status = 'COMPLETED' AND e.exerciseId = :exerciseId
          AND s.id = (
            SELECT s2.id FROM set_entries t2
            JOIN session_exercises e2 ON e2.id = t2.sessionExerciseId
            JOIN workout_sessions s2 ON s2.id = e2.workoutSessionId
            WHERE t2.legacyTrainingRecordId IS NULL AND t2.status = 'COMPLETED' AND e2.exerciseId = :exerciseId
            ORDER BY s2.startedAtEpochMillis DESC, s2.id DESC
            LIMIT 1
          )
        ORDER BY e.orderIndex, t.orderIndex
        """
    )
    suspend fun latestV2OnlySessionRows(exerciseId: Long): List<V2HistoryRow>

    /** Whether a v2-only completed set of [exerciseId] belongs to a session started in [startMillis, endMillis). */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM set_entries t
            JOIN session_exercises e ON e.id = t.sessionExerciseId
            JOIN workout_sessions s ON s.id = e.workoutSessionId
            WHERE t.legacyTrainingRecordId IS NULL AND t.status = 'COMPLETED' AND e.exerciseId = :exerciseId
              AND s.startedAtEpochMillis >= :startMillis AND s.startedAtEpochMillis < :endMillis
        )
        """
    )
    suspend fun hasV2OnlySetStartedBetween(exerciseId: Long, startMillis: Long, endMillis: Long): Boolean

    /** Reads one workout consistently. */
    @Transaction
    suspend fun sessionGraph(id: Long): WorkoutSessionGraph? {
        val session = session(id) ?: return null
        return WorkoutSessionGraph(session, sessionExercises(id).map { it to setEntries(it.id) })
    }
}
