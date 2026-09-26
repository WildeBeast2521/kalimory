package io.github.gonbei774.calisthenicsmemory.migration

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.v2.BodySide
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.v2.Laterality
import io.github.gonbei774.calisthenicsmemory.data.v2.SessionExerciseEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.TimePrecision
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSourceType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The v2 workout tables store typed values and keep history when library rows go away. */
@RunWith(AndroidJUnit4::class)
class V2SchemaTest {
    private lateinit var database: AppDatabase
    private val dao get() = database.workoutSessionDao()

    @Before
    fun setUp(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        database.exerciseGroupDao().insertGroup(ExerciseGroup(id = 1, name = "Push"))
        database.exerciseDao().insertExercise(Exercise(id = 1, name = "Push-up", type = "Dynamic", group = "Push"))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertWorkout(): Long {
        val sessionId = dao.insertSession(
            WorkoutSessionEntity(
                status = WorkoutSessionStatus.COMPLETED,
                sourceType = WorkoutSourceType.PROGRAM_TEMPLATE,
                sourceTemplateId = 99,
                sourceNameSnapshot = "Beginner",
                startedAtEpochMillis = 1_700_000_000_000,
                endedAtEpochMillis = 1_700_000_600_000,
                updatedAtEpochMillis = 1_700_000_600_000,
                timePrecision = TimePrecision.EXACT,
                comment = "good",
            )
        )
        val exerciseId = dao.insertSessionExercise(
            SessionExerciseEntity(
                workoutSessionId = sessionId, orderIndex = 0, exerciseId = 1, groupId = 1,
                exerciseNameSnapshot = "Push-up", exerciseKindSnapshot = ExerciseKind.DYNAMIC,
                lateralitySnapshot = Laterality.BILATERAL, groupNameSnapshot = "Push", targetSets = 2, targetRepetitions = 10,
            )
        )
        dao.insertSetEntries(
            listOf(
                SetEntryEntity(
                    sessionExerciseId = exerciseId, orderIndex = 0, setNumber = 1, status = SetEntryStatus.COMPLETED,
                    side = BodySide.BILATERAL, repetitions = 10, addedWeightGrams = 5_000, targetRepetitions = 10,
                    completedAtEpochMillis = 1_700_000_100_000, timePrecision = TimePrecision.EXACT,
                ),
                SetEntryEntity(
                    sessionExerciseId = exerciseId, orderIndex = 1, setNumber = 2, status = SetEntryStatus.SKIPPED,
                    side = BodySide.BILATERAL, targetRepetitions = 10, timePrecision = TimePrecision.EXACT,
                ),
            )
        )
        return sessionId
    }

    @Test
    fun aWorkoutRoundTripsWithTypedValues() = runBlocking {
        val id = insertWorkout()
        val graph = dao.sessionGraph(id)!!
        assertEquals(WorkoutSessionStatus.COMPLETED, graph.session.status)
        assertEquals(1, graph.exercises.size)
        val (exercise, sets) = graph.exercises.single()
        assertEquals("Push-up", exercise.exerciseNameSnapshot)
        assertEquals(listOf(SetEntryStatus.COMPLETED, SetEntryStatus.SKIPPED), sets.map { it.status })
        assertEquals(listOf(10, null), sets.map { it.repetitions })
        assertEquals(5_000, sets[0].addedWeightGrams)
    }

    @Test
    fun enumsAreStoredAsTheirCodes() = runBlocking {
        insertWorkout()
        database.openHelper.readableDatabase.query(
            "SELECT s.status, s.sourceType, s.timePrecision, e.exerciseKindSnapshot, e.lateralitySnapshot, t.status, t.side " +
                "FROM workout_sessions s JOIN session_exercises e ON e.workoutSessionId = s.id " +
                "JOIN set_entries t ON t.sessionExerciseId = e.id ORDER BY t.orderIndex LIMIT 1"
        ).use { c ->
            c.moveToFirst()
            assertEquals(
                listOf("COMPLETED", "PROGRAM_TEMPLATE", "EXACT", "DYNAMIC", "BILATERAL", "COMPLETED", "BILATERAL"),
                (0 until c.columnCount).map { c.getString(it) },
            )
        }
    }

    @Test
    fun historySurvivesDeletingTheExerciseAndGroup() = runBlocking {
        val id = insertWorkout()
        database.exerciseGroupDao().deleteGroupAndUngroupExercises("Push")
        database.exerciseDao().deleteExerciseById(1)

        val exercise = dao.sessionGraph(id)!!.exercises.single().first
        assertNull(exercise.exerciseId)
        assertNull(exercise.groupId)
        assertEquals("Push-up", exercise.exerciseNameSnapshot)
        assertEquals("Push", exercise.groupNameSnapshot)
        assertEquals(2, dao.sessionGraph(id)!!.exercises.single().second.size)
    }

    @Test
    fun deletingASessionRemovesItsExercisesAndSets() = runBlocking {
        val id = insertWorkout()
        database.openHelper.writableDatabase.execSQL("DELETE FROM workout_sessions WHERE id = $id")
        val db = database.openHelper.readableDatabase
        for (table in listOf("session_exercises", "set_entries")) {
            db.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); assertEquals(table, 0, it.getInt(0)) }
        }
    }

    @Test
    fun orderAndLegacyLinksAreUnique() {
        runBlocking { insertWorkout() }
        val exerciseId = runBlocking { dao.sessionGraph(1)!!.exercises.single().first.id }
        fun set(order: Int, legacy: Long?) = SetEntryEntity(
            sessionExerciseId = exerciseId, orderIndex = order, setNumber = 3, status = SetEntryStatus.COMPLETED,
            side = BodySide.LEFT, timePrecision = TimePrecision.MINUTE, legacyTrainingRecordId = legacy,
        )
        assertThrows(SQLiteConstraintException::class.java) { runBlocking { dao.insertSetEntries(listOf(set(0, null))) } }
        runBlocking { dao.insertSetEntries(listOf(set(5, 42))) }
        assertThrows(SQLiteConstraintException::class.java) { runBlocking { dao.insertSetEntries(listOf(set(6, 42))) } }
    }
}
