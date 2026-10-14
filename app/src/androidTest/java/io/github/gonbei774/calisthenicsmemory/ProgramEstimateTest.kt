package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A program shows the same estimated time in the Train list as on its start screen (#157). The
 * last session is longer than the program's targets and prefill is on, so the start screen opens
 * with the last session's sets; the list used to estimate from the targets instead.
 */
@RunWith(AndroidJUnit4::class)
class ProgramEstimateTest {
    @get:Rule(order = 0)
    val welcomeSeen = WelcomeSeenRule()

    @get:Rule(order = 1)
    val rule = createEmptyComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = AppDatabase.getDatabase(context)
    private val prefs = context.getSharedPreferences("workout_preferences", Context.MODE_PRIVATE)
    private var savedPrefs: Map<String, *> = emptyMap<String, Any>()

    // Synthetic data, unique per run.
    private val programName = "Estimate program ${System.nanoTime()}"
    private var exerciseId = 0L
    private var programId = 0L
    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun text(id: Int) = context.getString(id)

    @Before
    fun seed(): Unit = runBlocking {
        savedPrefs = prefs.all.toMap()
        WorkoutPreferences(context).apply {
            setPrefillPreviousRecordEnabled(true)
            setStartCountdownEnabled(false)
        }
        exerciseId = database.exerciseDao().insertExercise(
            Exercise(name = "Estimate push-up ${System.nanoTime()}", type = "Dynamic", repDuration = 3)
        )
        programId = database.programDao().insert(Program(name = programName))
        // The plan: 2 sets of 5 reps, a minute's rest. Last time: 5 sets of 30 reps.
        database.programExerciseDao().insert(
            ProgramExercise(programId = programId, exerciseId = exerciseId, sortOrder = 0, sets = 2, targetValue = 5, intervalSeconds = 60)
        )
        database.trainingRecordDao().insertRecords(
            (1..5).map { TrainingRecord(exerciseId = exerciseId, valueRight = 30, setNumber = it, date = "2026-10-01", time = "08:00") }
        )
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::scenario.isInitialized) scenario.close()
        database.programDao().deleteById(programId)
        database.exerciseDao().deleteExerciseAndTodoTasks(database.exerciseDao().getExerciseById(exerciseId)!!)
        prefs.edit().clear().apply {
            savedPrefs.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is String -> putString(key, value)
                }
            }
        }.commit()
    }

    @Test
    fun theTrainListAndTheStartScreenShowTheSameEstimate() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val train = text(PrimaryDestination.TRAIN.label)
        rule.waitUntil(TIMEOUT_MS) { rule.onAllNodesWithText(train).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(hasText(train) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))).performClick()

        // The row's summary reads "1 exercise · ~N min" once the estimate has loaded; the row
        // merges its texts, so the program's own node carries its estimate.
        val estimate = Regex("~\\d+ min")
        fun rowEstimate(): String? = rule.onAllNodesWithText(programName, substring = true).fetchSemanticsNodes()
            .firstNotNullOfOrNull { node -> estimate.find(node.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString())?.value }
        rule.waitUntil(TIMEOUT_MS) { rowEstimate() != null }
        val listEstimate = rowEstimate()

        rule.onNodeWithText(programName).performScrollTo().performClick()
        rule.waitUntil(TIMEOUT_MS) {
            rule.onAllNodesWithText(text(R.string.auto_fill_target_label)).fetchSemanticsNodes().isNotEmpty()
        }
        val startEstimate = rule.onAllNodesWithText("min", substring = true).fetchSemanticsNodes()
            .mapNotNull { node -> estimate.find(node.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString())?.value }
            .first()

        // 5 x 30 reps x 3 s + 4 x 60 s rest = 690 s: ~12 min, not the plan's ~2 min.
        assertEquals("~12 min", startEstimate)
        assertEquals(startEstimate, listEstimate)
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
    }
}
