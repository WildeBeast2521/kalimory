package io.github.gonbei774.calisthenicsmemory

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.MovementPattern
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Progress opens the body map, which shows this week's sets by muscle, and back returns (ADR 0011). */
@RunWith(AndroidJUnit4::class)
class ProgressBodyMapTest {
    @get:Rule(order = 0)
    val welcomeSeen = WelcomeSeenRule()

    @get:Rule(order = 1)
    val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = AppDatabase.getDatabase(context)
    private var exerciseId = 0L
    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun text(id: Int) = context.getString(id)

    @Before
    fun seed(): Unit = runBlocking {
        // A chest step nobody else links to in these tests: the hardest one that names the chest.
        val step = Catalogue.chains.filter { it.pattern != MovementPattern.MOBILITY }.flatMap { it.steps }
            .last { Muscle.CHEST in it.primaryMuscles }
        exerciseId = database.exerciseDao().insertExercise(
            Exercise(name = "Body map test ${System.nanoTime()}", type = "Dynamic", catalogId = step.id)
        )
        database.trainingRecordDao().insertRecord(
            TrainingRecord(exerciseId = exerciseId, valueRight = 10, setNumber = 1, date = LocalDate.now().toString(), time = "07:00")
        )
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::scenario.isInitialized) scenario.close()
        database.exerciseDao().getExerciseById(exerciseId)?.let { database.exerciseDao().deleteExerciseAndTodoTasks(it) }
    }

    @Test
    fun theBodyMapOpensFromProgressAndListsThisWeeksMuscles() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val progress = hasText(text(PrimaryDestination.PROGRESS.label)) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))
        rule.waitUntil(STARTUP_TIMEOUT_MS) { rule.onAllNodes(progress).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(progress).performClick()

        rule.onNode(hasContentDescription(text(R.string.body_map_open))).performClick()
        rule.onNode(hasText(text(R.string.body_map_title)) and isHeading()).assertExists()
        rule.waitUntil(STARTUP_TIMEOUT_MS) { rule.onAllNodes(hasText(text(R.string.muscle_chest))).fetchSemanticsNodes().isNotEmpty() }

        rule.waitForIdle()
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNode(hasText(text(R.string.nav_progress)) and isHeading()).assertExists()
    }

    @Test
    fun aMuscleOnlyAnOwnExerciseTrainsShowsOnTheMap() {
        // An exercise of your own with a muscle chosen counts like a catalogue exercise (ADR 0014).
        val curlId = runBlocking {
            database.exerciseDao().insertExercise(
                Exercise(name = "Own neck work ${System.nanoTime()}", type = "Dynamic", primaryMuscles = linkedSetOf(Muscle.NECK))
            ).also {
                database.trainingRecordDao().insertRecord(
                    TrainingRecord(exerciseId = it, valueRight = 10, setNumber = 1, date = LocalDate.now().toString(), time = "07:05")
                )
            }
        }
        try {
            scenario = ActivityScenario.launch(MainActivity::class.java)
            val progress = hasText(text(PrimaryDestination.PROGRESS.label)) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))
            rule.waitUntil(STARTUP_TIMEOUT_MS) { rule.onAllNodes(progress).fetchSemanticsNodes().isNotEmpty() }
            rule.onNode(progress).performClick()
            rule.onNode(hasContentDescription(text(R.string.body_map_open))).performClick()
            rule.waitUntil(STARTUP_TIMEOUT_MS) { rule.onAllNodes(hasText(text(R.string.muscle_neck))).fetchSemanticsNodes().isNotEmpty() }
        } finally {
            runBlocking { database.exerciseDao().getExerciseById(curlId)?.let { database.exerciseDao().deleteExerciseAndTodoTasks(it) } }
        }
    }

    @Test
    fun overTimeListsEachWeekOfAMuscleGroup() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val progress = hasText(text(PrimaryDestination.PROGRESS.label)) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))
        rule.waitUntil(STARTUP_TIMEOUT_MS) { rule.onAllNodes(progress).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(progress).performClick()
        rule.onNode(hasContentDescription(text(R.string.body_map_open))).performClick()
        rule.waitUntil(STARTUP_TIMEOUT_MS) { rule.onAllNodes(hasText(text(R.string.body_map_view_trend))).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(hasText(text(R.string.body_map_view_trend))).performClick()
        // The seeded chest set is this week's: the chest group lists its weeks, the running one saying so.
        rule.onNode(hasText(text(R.string.muscle_chest)) and androidx.compose.ui.test.hasClickAction()).performClick()
        rule.waitUntil(STARTUP_TIMEOUT_MS) { rule.onAllNodes(hasText(text(R.string.body_map_trend_by_week))).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(hasText(text(R.string.body_map_trend_so_far))).fetchSemanticsNodes().isNotEmpty().also { check(it) { "the running week is not marked" } }
    }

    private companion object {
        const val STARTUP_TIMEOUT_MS = 10_000L
    }
}
