package io.github.gonbei774.calisthenicsmemory

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
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
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/** Progress shows legacy and v2-only workouts together, and offers edit actions only for legacy ones. */
@RunWith(AndroidJUnit4::class)
class ProgressHistoryTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = AppDatabase.getDatabase(context)

    // Synthetic data, unique per run.
    private val exerciseName = "Progress test row ${System.nanoTime()}"
    private var exerciseId = 0L
    private var sessionId = 0L
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun seed(): Unit = runBlocking {
        exerciseId = database.exerciseDao().insertExercise(Exercise(name = exerciseName, type = "Dynamic"))
        val today = LocalDate.now()
        database.trainingRecordDao().insertRecord(
            TrainingRecord(exerciseId = exerciseId, valueRight = 41, setNumber = 1, date = today.toString(), time = "06:00")
        )
        // A v2-only workout later the same day.
        val startedAt = today.atTime(20, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dao = database.workoutSessionDao()
        sessionId = dao.insertSession(
            WorkoutSessionEntity(
                status = WorkoutSessionStatus.COMPLETED, sourceType = WorkoutSourceType.AD_HOC,
                startedAtEpochMillis = startedAt, updatedAtEpochMillis = startedAt, timePrecision = TimePrecision.EXACT,
            )
        )
        val sessionExerciseId = dao.insertSessionExercise(
            SessionExerciseEntity(
                workoutSessionId = sessionId, orderIndex = 0, exerciseId = exerciseId, exerciseNameSnapshot = exerciseName,
                exerciseKindSnapshot = ExerciseKind.DYNAMIC, lateralitySnapshot = Laterality.BILATERAL,
            )
        )
        dao.insertSetEntries(
            listOf(
                SetEntryEntity(
                    sessionExerciseId = sessionExerciseId, orderIndex = 0, setNumber = 1, status = SetEntryStatus.COMPLETED,
                    side = BodySide.BILATERAL, repetitions = 37, completedAtEpochMillis = startedAt,
                    timePrecision = TimePrecision.EXACT,
                )
            )
        )
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::scenario.isInitialized) scenario.close()
        // Session exercises and set entries cascade from the session; records cascade from the exercise.
        database.openHelper.writableDatabase.execSQL("DELETE FROM workout_sessions WHERE id = ?", arrayOf<Any>(sessionId))
        database.exerciseDao().deleteExerciseById(exerciseId)
    }

    @Test
    fun listShowsLegacyAndV2WorkoutsButOnlyLegacyIsEditable() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val progress = context.getString(PrimaryDestination.PROGRESS.label)
        rule.waitUntil(STARTUP_TIMEOUT_MS) {
            rule.onAllNodesWithText(progress).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNode(hasText(progress) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))).performClick()
        rule.onNodeWithText(context.getString(R.string.tab_list)).performClick()
        rule.waitUntil(STARTUP_TIMEOUT_MS) {
            rule.onAllNodesWithText(exerciseName).fetchSemanticsNodes().size == 2
        }

        val today = LocalDate.now().toString()
        rule.onNodeWithText("$today 06:00").assertExists()
        rule.onNodeWithText("$today 20:00").assertExists()
        // Only the legacy session has the edit/delete menu.
        rule.onAllNodesWithContentDescription(context.getString(R.string.menu)).assertCountEquals(1)
    }

    private companion object {
        const val STARTUP_TIMEOUT_MS = 10_000L
    }
}
