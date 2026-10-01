package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueAddResult
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueLibrary
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueStepText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Adding catalogue steps makes ordinary exercises, links existing ones and never duplicates (ADR 0007). */
@RunWith(AndroidJUnit4::class)
class CatalogueLibraryTest {
    private lateinit var database: AppDatabase

    private val pullUp = Catalogue.step("pull.full")!!
    private val pullUpText = CatalogueStepText("Pull-up", "Hang and pull.", "Pull-up")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addsTheStepAsAnExerciseInItsChainGroup() = runBlocking {
        val result = CatalogueLibrary.add(database, pullUp, pullUpText)

        assertTrue(result is CatalogueAddResult.Added)
        val exercise = database.exerciseDao().getExerciseById(result.exerciseId)!!
        assertEquals("Pull-up", exercise.name)
        assertEquals("Dynamic", exercise.type)
        assertEquals("Pull-up", exercise.group)
        assertEquals(pullUp.difficulty, exercise.sortOrder)
        assertEquals(pullUp.moveOn.sets, exercise.targetSets)
        assertEquals(pullUp.moveOn.value, exercise.targetValue)
        assertEquals("Hang and pull.", exercise.description)
        assertEquals("pull.full", exercise.catalogId)
        assertEquals(listOf("Pull-up"), database.exerciseGroupDao().getAllGroupsSync().map { it.name })
    }

    @Test
    fun addingTwiceReportsItIsAlreadyInTheLibrary() = runBlocking {
        val first = CatalogueLibrary.add(database, pullUp, pullUpText)
        val second = CatalogueLibrary.add(database, pullUp, pullUpText)

        assertEquals(CatalogueAddResult.AlreadyInLibrary(first.exerciseId), second)
        assertEquals(1, database.exerciseDao().getAllExercises().first().size)
    }

    @Test
    fun linksTheUsersExerciseOfTheSameNameInsteadOfDuplicating() = runBlocking {
        val ownId = database.exerciseDao().insertExercise(Exercise(name = "pull-up", type = "Dynamic", targetSets = 5, targetValue = 5))

        val result = CatalogueLibrary.add(database, pullUp, pullUpText)

        assertEquals(CatalogueAddResult.Linked(ownId), result)
        val own = database.exerciseDao().getExerciseById(ownId)!!
        assertEquals("pull.full", own.catalogId)
        // The user's own settings stay as they were.
        assertEquals("pull-up", own.name)
        assertNull(own.group)
        assertEquals(5, own.targetSets)
        assertEquals(1, database.exerciseDao().getAllExercises().first().size)
    }

    @Test
    fun leavesANameLinkedToAnotherStepAlone() = runBlocking {
        val ownId = database.exerciseDao().insertExercise(Exercise(name = "Pull-up", type = "Dynamic", catalogId = "pull.negative"))

        val result = CatalogueLibrary.add(database, pullUp, pullUpText)

        assertEquals(CatalogueAddResult.NameTaken(ownId), result)
        assertEquals("pull.negative", database.exerciseDao().getExerciseById(ownId)!!.catalogId)
        assertEquals(1, database.exerciseDao().getAllExercises().first().size)
    }

    @Test
    fun stepsOfOneChainShareOneGroup() = runBlocking {
        val chain = Catalogue.chain("pull")!!
        chain.steps.take(2).forEachIndexed { index, step ->
            CatalogueLibrary.add(database, step, CatalogueStepText("Step $index", "", "Pull-up"))
        }

        assertEquals(listOf("Pull-up"), database.exerciseGroupDao().getAllGroupsSync().map { it.name })
        val exercises = database.exerciseDao().getAllExercises().first()
        assertEquals(chain.steps.take(2).map { it.id }, exercises.sortedBy { it.displayOrder }.map { it.catalogId })
    }
}
