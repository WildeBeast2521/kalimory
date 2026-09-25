package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseGroupDao {

    @Query("SELECT * FROM exercise_groups ORDER BY displayOrder ASC")
    fun getAllGroups(): Flow<List<ExerciseGroup>>

    @Query("SELECT * FROM exercise_groups ORDER BY displayOrder ASC")
    suspend fun getAllGroupsSync(): List<ExerciseGroup>

    @Query("SELECT * FROM exercise_groups WHERE name = :name")
    suspend fun getGroupByName(name: String): ExerciseGroup?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroup(group: ExerciseGroup): Long

    @Update
    suspend fun updateGroup(group: ExerciseGroup)

    @Delete
    suspend fun deleteGroup(group: ExerciseGroup)

    @Query("DELETE FROM exercise_groups WHERE name = :name")
    suspend fun deleteGroupByName(name: String)

    @Query("DELETE FROM exercise_groups")
    suspend fun deleteAll()

    @Query("UPDATE exercise_groups SET name = :newName WHERE name = :oldName")
    suspend fun updateGroupName(oldName: String, newName: String)

    @Query("UPDATE exercises SET `group` = :newName WHERE `group` = :oldName")
    suspend fun moveExercisesToGroup(oldName: String, newName: String)

    @Query("UPDATE exercises SET `group` = NULL, sortOrder = 0 WHERE `group` = :name")
    suspend fun ungroupExercises(name: String)

    @Query("DELETE FROM todo_tasks WHERE type = 'GROUP' AND referenceId IN (SELECT id FROM exercise_groups WHERE name = :name)")
    suspend fun deleteGroupTodoTasks(name: String)

    /** Renames the group and every exercise that uses the old name, or nothing on failure. */
    @Transaction
    suspend fun renameGroupAndExercises(oldName: String, newName: String) {
        updateGroupName(oldName, newName)
        moveExercisesToGroup(oldName, newName)
    }

    @Query(
        "SELECT DISTINCT `group` FROM exercises WHERE `group` IS NOT NULL " +
            "AND `group` NOT IN (SELECT name FROM exercise_groups) ORDER BY `group`"
    )
    fun groupNamesWithoutRow(): List<String>

    @Query("SELECT COALESCE(MAX(displayOrder), -1) FROM exercise_groups")
    fun maxGroupDisplayOrder(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertGroupBlocking(group: ExerciseGroup): Long

    /**
     * Adds a group row for every group name that exercises use but exercise_groups lacks,
     * placed after the existing groups in name order. Without the row those exercises are
     * missing from every exercise list. Only inserts; returns the number of groups added.
     * Call from a background thread; callers treat a failure as non-fatal.
     */
    @Transaction
    fun restoreMissingGroups(): Int {
        val names = groupNamesWithoutRow()
        var next = maxGroupDisplayOrder() + 1
        names.forEach { insertGroupBlocking(ExerciseGroup(name = it, displayOrder = next++)) }
        return names.size
    }

    /** Deletes the group and its todo tasks and ungroups its exercises, or nothing on failure. */
    @Transaction
    suspend fun deleteGroupAndUngroupExercises(name: String) {
        deleteGroupTodoTasks(name)
        deleteGroupByName(name)
        ungroupExercises(name)
    }
}