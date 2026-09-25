package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.BackupSnapshot
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestoreMissingGroupsTest {
    private lateinit var database: AppDatabase

    private val initial = BackupSnapshot(
        groups = listOf(ExerciseGroup(1, "Push", 4)),
        exercises = listOf(
            Exercise(1, "Push-up", "Dynamic", "Push"),
            Exercise(2, "Squat", "Dynamic", "Legs"),
            Exercise(3, "Lunge", "Dynamic", "Legs"),
            Exercise(4, "Pull-up", "Dynamic", "Deleted pull"),
            Exercise(5, "Plank", "Isometric", null),
        ),
        records = emptyList(), programs = emptyList(), programExercises = emptyList(), programLoops = emptyList(),
        intervalPrograms = emptyList(), intervalProgramExercises = emptyList(), intervalRecords = emptyList(),
        todoTasks = emptyList(),
    )

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        database.backupDao().replaceAll(initial)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addsOnlyTheMissingGroupsAfterTheExistingOnes() = runBlocking {
        assertEquals(2, database.exerciseGroupDao().restoreMissingGroups())

        val after = database.backupDao().snapshot()
        assertEquals(
            listOf(ExerciseGroup(1, "Push", 4), ExerciseGroup(2, "Deleted pull", 5), ExerciseGroup(3, "Legs", 6)),
            after.groups,
        )
        assertEquals(initial.exercises, after.exercises)
        assertEquals(0, database.exerciseGroupDao().restoreMissingGroups())
    }
}
