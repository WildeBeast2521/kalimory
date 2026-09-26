package io.github.gonbei774.calisthenicsmemory.data.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

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
               s.comment AS sessionComment
        FROM set_entries t
        JOIN session_exercises e ON e.id = t.sessionExerciseId
        JOIN workout_sessions s ON s.id = e.workoutSessionId
        WHERE t.legacyTrainingRecordId IS NULL AND t.status = 'COMPLETED' AND e.exerciseId IS NOT NULL
        ORDER BY s.startedAtEpochMillis, e.orderIndex, t.orderIndex
        """
    )
    suspend fun v2OnlyHistoryRows(): List<V2HistoryRow>

    /** Reads one workout consistently. */
    @Transaction
    suspend fun sessionGraph(id: Long): WorkoutSessionGraph? {
        val session = session(id) ?: return null
        return WorkoutSessionGraph(session, sessionExercises(id).map { it to setEntries(it.id) })
    }
}
