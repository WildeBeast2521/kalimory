package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.viewmodel.CommunityShareContent
import io.github.gonbei774.calisthenicsmemory.viewmodel.CommunityShareImportReport
import io.github.gonbei774.calisthenicsmemory.viewmodel.CommunityShareImporter
import io.github.gonbei774.calisthenicsmemory.viewmodel.ShareExercise
import io.github.gonbei774.calisthenicsmemory.viewmodel.ShareGroup
import io.github.gonbei774.calisthenicsmemory.viewmodel.ShareIntervalProgram
import io.github.gonbei774.calisthenicsmemory.viewmodel.ShareIntervalProgramExercise
import io.github.gonbei774.calisthenicsmemory.viewmodel.ShareProgram
import io.github.gonbei774.calisthenicsmemory.viewmodel.ShareProgramExercise
import io.github.gonbei774.calisthenicsmemory.viewmodel.ShareProgramLoop
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommunityShareImporterTest {
    private lateinit var database: AppDatabase

    private val content = CommunityShareContent(
        groups = listOf(ShareGroup("Push")),
        exercises = listOf(ShareExercise("Push-up", "Dynamic", group = "Push"), ShareExercise("Plank", "Isometric")),
        programs = listOf(
            ShareProgram(
                "Beginner",
                exercises = listOf(ShareProgramExercise("Push-up", "Dynamic", 0, sets = 3, targetValue = 10, loopId = 1)),
                loops = listOf(ShareProgramLoop(1, 0, 2, 60)),
            )
        ),
        intervalPrograms = listOf(
            ShareIntervalProgram("Tabata", 20, 10, 8, 60, listOf(ShareIntervalProgramExercise("Plank", "Isometric", 0)))
        ),
    )

    @Before
    fun setUp(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        // Existing user data that an import must never disturb.
        database.exerciseDao().insertExercise(Exercise(name = "Plank", type = "Isometric"))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun snapshot() = runBlocking { database.backupDao().snapshot() }

    @Test
    fun importsEveryEntityAndReusesExistingExercises() = runBlocking {
        val report = CommunityShareImporter(database).import(content)

        assertEquals(
            CommunityShareImportReport(
                groupsAdded = 1, exercisesAdded = 1, exercisesSkipped = 1, programsAdded = 1, intervalProgramsAdded = 1,
            ),
            report,
        )
        val after = snapshot()
        assertEquals(listOf("Plank", "Push-up"), after.exercises.map { it.name })
        assertEquals(1, after.programLoops.size)
        assertEquals(after.programLoops.single().id, after.programExercises.single().loopId)
        assertEquals(after.exercises.first { it.name == "Plank" }.id, after.intervalProgramExercises.single().exerciseId)
    }

    @Test
    fun newExercisesKeepTheirCatalogueLinkUnlessTheStepIsTakenHere() = runBlocking {
        database.exerciseDao().insertExercise(Exercise(name = "My chin-ups", type = "Dynamic", catalogId = "pull.chin"))
        val linked = CommunityShareContent(
            exercises = listOf(
                ShareExercise("Pull-up", "Dynamic", catalogId = "pull.full"),
                ShareExercise("Chin-up", "Dynamic", catalogId = "pull.chin"),
                ShareExercise("Plank", "Isometric", catalogId = "core.plank"),
            )
        )

        CommunityShareImporter(database).import(linked)

        val byName = snapshot().exercises.associateBy { it.name }
        assertEquals("pull.full", byName.getValue("Pull-up").catalogId)
        // The step is already linked here, so the new exercise comes in unlinked.
        assertEquals(null, byName.getValue("Chin-up").catalogId)
        assertEquals("pull.chin", byName.getValue("My chin-ups").catalogId)
        // An existing exercise is never changed by an import.
        assertEquals(null, byName.getValue("Plank").catalogId)
    }

    @Test
    fun aFailurePartWayThroughLeavesNoPartialImport() {
        val before = snapshot()
        // Fail the last step, after groups, exercises, and programs have been written.
        database.openHelper.writableDatabase.execSQL(
            "CREATE TEMP TRIGGER fail_interval BEFORE INSERT ON interval_programs BEGIN SELECT RAISE(ABORT, 'injected failure'); END"
        )

        assertThrows(SQLiteException::class.java) {
            runBlocking { CommunityShareImporter(database).import(content) }
        }

        assertEquals(before, snapshot())
    }
}
