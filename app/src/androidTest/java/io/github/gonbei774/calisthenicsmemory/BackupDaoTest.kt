package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: BackupDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.backupDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun snapshot_returnsEveryTableInPrimaryKeyOrder() = runBlocking {
        val input = syntheticSnapshot("snapshot", 100).reversed()

        dao.replaceAll(input)

        val actual = dao.snapshot()
        assertEquals(input.sorted(), actual)
        assertEquals(2, actual.groups.size)
        assertEquals(2, actual.exercises.size)
        assertEquals(2, actual.records.size)
        assertEquals(2, actual.programs.size)
        assertEquals(2, actual.programExercises.size)
        assertEquals(2, actual.programLoops.size)
        assertEquals(2, actual.intervalPrograms.size)
        assertEquals(2, actual.intervalProgramExercises.size)
        assertEquals(2, actual.intervalRecords.size)
        assertEquals(2, actual.todoTasks.size)
    }

    @Test
    fun replaceAll_replacesExistingRows() = runBlocking {
        dao.replaceAll(syntheticSnapshot("old", 100))
        val replacement = syntheticSnapshot("new", 200).reversed()

        dao.replaceAll(replacement)

        assertEquals(replacement.sorted(), dao.snapshot())
    }

    @Test
    fun replaceAll_foreignKeyFailureRollsBackDeletesAndInserts() = runBlocking {
        val old = syntheticSnapshot("old", 100)
        dao.replaceAll(old)
        val before = dao.snapshot()
        val replacement = syntheticSnapshot("new", 200)
        val invalid = replacement.copy(
            programExercises = replacement.programExercises + ProgramExercise(
                id = 999,
                programId = replacement.programs.first().id,
                exerciseId = 999_999,
                sortOrder = 99,
                targetValue = 1
            )
        )

        try {
            dao.replaceAll(invalid)
            fail("Expected the invalid foreign key to abort replacement")
        } catch (_: SQLiteConstraintException) {
            // Expected: the transaction must restore both deleted and partially inserted rows.
        }

        assertEquals(before, dao.snapshot())
    }

    private fun syntheticSnapshot(label: String, base: Long): BackupSnapshot {
        val groups = listOf(
            ExerciseGroup(base + 1, "$label-group-a", 0),
            ExerciseGroup(base + 2, "$label-group-b", 1)
        )
        val exercises = listOf(
            Exercise(base + 11, "$label-exercise-a", "reps", groups[0].name, targetSets = 3),
            Exercise(base + 12, "$label-exercise-b", "seconds", groups[1].name, targetValue = 30)
        )
        val programs = listOf(
            Program(base + 21, "$label-program-a"),
            Program(base + 22, "$label-program-b")
        )
        val loops = listOf(
            ProgramLoop(base + 31, programs[0].id, 0, 2, 15),
            ProgramLoop(base + 32, programs[1].id, 0, 3, 20)
        )
        val intervalPrograms = listOf(
            IntervalProgram(base + 41, "$label-interval-a", 30, 10, 3, 60),
            IntervalProgram(base + 42, "$label-interval-b", 40, 20, 4, 90)
        )
        return BackupSnapshot(
            groups = groups,
            exercises = exercises,
            records = listOf(
                TrainingRecord(base + 51, exercises[0].id, 8, null, 1, "2026-01-01", "08:00"),
                TrainingRecord(base + 52, exercises[1].id, 30, null, 1, "2026-01-02", "09:00")
            ),
            programs = programs,
            programExercises = listOf(
                ProgramExercise(base + 61, programs[0].id, exercises[0].id, 0, 2, 8, 45, loops[0].id),
                ProgramExercise(base + 62, programs[1].id, exercises[1].id, 0, 1, 30, 60, loops[1].id)
            ),
            programLoops = loops,
            intervalPrograms = intervalPrograms,
            intervalProgramExercises = listOf(
                IntervalProgramExercise(base + 71, intervalPrograms[0].id, exercises[0].id, 0),
                IntervalProgramExercise(base + 72, intervalPrograms[1].id, exercises[1].id, 0)
            ),
            intervalRecords = listOf(
                IntervalRecord(base + 81, intervalPrograms[0].name, "2026-01-03", "10:00", 30, 10, 3, 60, 3, 2, "[]"),
                IntervalRecord(base + 82, intervalPrograms[1].name, "2026-01-04", "11:00", 40, 20, 4, 90, 4, 2, "[]")
            ),
            todoTasks = listOf(
                TodoTask(base + 91, TodoTask.TYPE_EXERCISE, exercises[0].id, 0),
                TodoTask(base + 92, TodoTask.TYPE_PROGRAM, programs[1].id, 1, "1,3")
            )
        )
    }

    private fun BackupSnapshot.reversed() = copy(
        groups = groups.reversed(),
        exercises = exercises.reversed(),
        records = records.reversed(),
        programs = programs.reversed(),
        programExercises = programExercises.reversed(),
        programLoops = programLoops.reversed(),
        intervalPrograms = intervalPrograms.reversed(),
        intervalProgramExercises = intervalProgramExercises.reversed(),
        intervalRecords = intervalRecords.reversed(),
        todoTasks = todoTasks.reversed()
    )

    private fun BackupSnapshot.sorted() = copy(
        groups = groups.sortedBy { it.id },
        exercises = exercises.sortedBy { it.id },
        records = records.sortedBy { it.id },
        programs = programs.sortedBy { it.id },
        programExercises = programExercises.sortedBy { it.id },
        programLoops = programLoops.sortedBy { it.id },
        intervalPrograms = intervalPrograms.sortedBy { it.id },
        intervalProgramExercises = intervalProgramExercises.sortedBy { it.id },
        intervalRecords = intervalRecords.sortedBy { it.id },
        todoTasks = todoTasks.sortedBy { it.id }
    )
}
