package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

data class BackupSnapshot(
    val groups: List<ExerciseGroup>,
    val exercises: List<Exercise>,
    val records: List<TrainingRecord>,
    val programs: List<Program>,
    val programExercises: List<ProgramExercise>,
    val programLoops: List<ProgramLoop>,
    val intervalPrograms: List<IntervalProgram>,
    val intervalProgramExercises: List<IntervalProgramExercise>,
    val intervalRecords: List<IntervalRecord>,
    val todoTasks: List<TodoTask>
)

@Dao
interface BackupDao {
    @Query("SELECT * FROM exercise_groups ORDER BY id")
    suspend fun groups(): List<ExerciseGroup>

    @Query("SELECT * FROM exercises ORDER BY id")
    suspend fun exercises(): List<Exercise>

    @Query("SELECT * FROM training_records ORDER BY id")
    suspend fun records(): List<TrainingRecord>

    @Query("SELECT * FROM programs ORDER BY id")
    suspend fun programs(): List<Program>

    @Query("SELECT * FROM program_exercises ORDER BY id")
    suspend fun programExercises(): List<ProgramExercise>

    @Query("SELECT * FROM program_loops ORDER BY id")
    suspend fun programLoops(): List<ProgramLoop>

    @Query("SELECT * FROM interval_programs ORDER BY id")
    suspend fun intervalPrograms(): List<IntervalProgram>

    @Query("SELECT * FROM interval_program_exercises ORDER BY id")
    suspend fun intervalProgramExercises(): List<IntervalProgramExercise>

    @Query("SELECT * FROM interval_records ORDER BY id")
    suspend fun intervalRecords(): List<IntervalRecord>

    @Query("SELECT * FROM todo_tasks ORDER BY id")
    suspend fun todoTasks(): List<TodoTask>

    @Transaction
    suspend fun snapshot(): BackupSnapshot = BackupSnapshot(
        groups(), exercises(), records(), programs(), programExercises(), programLoops(),
        intervalPrograms(), intervalProgramExercises(), intervalRecords(), todoTasks()
    )

    @Query("DELETE FROM todo_tasks")
    suspend fun deleteTodoTasks()

    @Query("DELETE FROM training_records")
    suspend fun deleteRecords()

    @Query("DELETE FROM program_exercises")
    suspend fun deleteProgramExercises()

    @Query("DELETE FROM program_loops")
    suspend fun deleteProgramLoops()

    @Query("DELETE FROM interval_program_exercises")
    suspend fun deleteIntervalProgramExercises()

    @Query("DELETE FROM interval_records")
    suspend fun deleteIntervalRecords()

    @Query("DELETE FROM programs")
    suspend fun deletePrograms()

    @Query("DELETE FROM interval_programs")
    suspend fun deleteIntervalPrograms()

    @Query("DELETE FROM exercises")
    suspend fun deleteExercises()

    @Query("DELETE FROM exercise_groups")
    suspend fun deleteGroups()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGroups(items: List<ExerciseGroup>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercises(items: List<Exercise>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecords(items: List<TrainingRecord>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPrograms(items: List<Program>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProgramLoops(items: List<ProgramLoop>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProgramExercises(items: List<ProgramExercise>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIntervalPrograms(items: List<IntervalProgram>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIntervalProgramExercises(items: List<IntervalProgramExercise>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIntervalRecords(items: List<IntervalRecord>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTodoTasks(items: List<TodoTask>)

    @Transaction
    suspend fun replaceAll(snapshot: BackupSnapshot) {
        // Children first. Keep this explicit so any failed insert rolls the whole transaction back.
        deleteTodoTasks()
        deleteRecords()
        deleteProgramExercises()
        deleteProgramLoops()
        deleteIntervalProgramExercises()
        deleteIntervalRecords()
        deletePrograms()
        deleteIntervalPrograms()
        deleteExercises()
        deleteGroups()

        // Parents first. ABORT conflict handling makes invalid snapshots fail closed.
        insertGroups(snapshot.groups)
        insertExercises(snapshot.exercises)
        insertPrograms(snapshot.programs)
        insertProgramLoops(snapshot.programLoops)
        insertProgramExercises(snapshot.programExercises)
        insertIntervalPrograms(snapshot.intervalPrograms)
        insertIntervalProgramExercises(snapshot.intervalProgramExercises)
        insertRecords(snapshot.records)
        insertIntervalRecords(snapshot.intervalRecords)
        insertTodoTasks(snapshot.todoTasks)
    }
}
