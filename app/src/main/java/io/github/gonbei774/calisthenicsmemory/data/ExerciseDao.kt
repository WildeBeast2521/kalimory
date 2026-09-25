package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): Exercise?

    @Query("SELECT * FROM exercises WHERE name = :name AND type = :type LIMIT 1")
    suspend fun getExerciseByNameAndType(name: String, type: String): Exercise?

    @Query("SELECT * FROM exercises WHERE `group` = :groupName ORDER BY displayOrder ASC")
    suspend fun getExercisesByGroup(groupName: String): List<Exercise>

    @Query("SELECT * FROM exercises WHERE `group` IS NULL ORDER BY displayOrder ASC")
    suspend fun getUngroupedExercises(): List<Exercise>

    // ABORT, not REPLACE: replacing on the (name, type) unique index would delete the existing
    // exercise and cascade-delete its training records and program entries.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercise(exercise: Exercise): Long

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: Long)

    @Query("DELETE FROM todo_tasks WHERE type = 'EXERCISE' AND referenceId = :exerciseId")
    suspend fun deleteExerciseTodoTasks(exerciseId: Long)

    /** Deletes the exercise (its records and program entries cascade) and its todo tasks together. */
    @Transaction
    suspend fun deleteExerciseAndTodoTasks(exercise: Exercise) {
        deleteExercise(exercise)
        deleteExerciseTodoTasks(exercise.id)
    }

    @Query("DELETE FROM exercises")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(MAX(displayOrder), -1) FROM exercises")
    suspend fun getMaxDisplayOrder(): Int
}