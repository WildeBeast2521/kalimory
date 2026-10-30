package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** A restore needs a backup first only when the phone holds something it would overwrite. */
@RunWith(AndroidJUnit4::class)
class BackupUserRowCountTest {
    private val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).build()

    @After
    fun close() = database.close()

    @Test
    fun anEmptyDatabaseHasNothingToLoseAndAnyRowCounts() = runBlocking {
        assertEquals(0, database.backupDao().userRowCount())

        // A to-do alone is the user's data too.
        database.todoTaskDao().insert(TodoTask(type = TodoTask.TYPE_PROGRAM, referenceId = 1, sortOrder = 0))
        assertEquals(1, database.backupDao().userRowCount())

        database.exerciseDao().insertExercise(Exercise(name = "Push-up", type = "Dynamic"))
        assertTrue(database.backupDao().userRowCount() > 1)
    }
}
