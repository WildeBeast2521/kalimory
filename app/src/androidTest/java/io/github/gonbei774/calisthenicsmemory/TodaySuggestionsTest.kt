package io.github.gonbei774.calisthenicsmemory

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgressionPreferences
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Today suggests a step from a followed chain; it can be dismissed for the day or turned off. */
@RunWith(AndroidJUnit4::class)
class TodaySuggestionsTest {
    @get:Rule(order = 0)
    val welcomeSeen = WelcomeSeenRule()

    @get:Rule(order = 1)
    val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = AppDatabase.getDatabase(context)
    private val prefs = ProgressionPreferences(context)
    private val step = Catalogue.chain("leg_raise")!!.steps.first()
    // Synthetic and unique per run.
    private val exerciseName = "Suggestion test ${System.nanoTime()}"
    private var exerciseId = 0L
    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun text(id: Int) = context.getString(id)

    @Before
    fun seed(): Unit = runBlocking {
        prefs.setShowSuggestions(true)
        database.exerciseDao().getExerciseByCatalogId(step.id)?.let { database.exerciseDao().updateExercise(it.copy(catalogId = null)) }
        // Its own target differs from the step's working standard, so the test sees which one is used.
        exerciseId = database.exerciseDao().insertExercise(
            Exercise(name = exerciseName, type = "Dynamic", targetSets = 5, targetValue = 25, catalogId = step.id)
        )
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::scenario.isInitialized) scenario.close()
        database.exerciseDao().deleteExerciseAndTodoTasks(database.exerciseDao().getExerciseById(exerciseId)!!)
        prefs.setShowSuggestions(true)
        prefs.setFollowed(step.chainId, true)
        context.getSharedPreferences("progression_preferences", 0).edit().remove("dismissed_date").remove("dismissed_chains").commit()
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        rule.waitUntil(TIMEOUT_MS) { rule.onAllNodesWithText(text(R.string.nav_train)).fetchSemanticsNodes().isNotEmpty() }
    }

    // Rows appear once the library and history have loaded; Today composes them all, on screen or not.
    private fun scrollTo(label: String) {
        rule.waitUntil(TIMEOUT_MS) { rule.onAllNodes(hasText(label)).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(label))
    }

    @Test
    fun aFollowedChainIsSuggestedAndCanBeDismissedForTheDay() {
        launch()
        scrollTo(exerciseName)
        rule.onNodeWithText(text(R.string.suggestion_first)).assertExists()

        // Scroll the row clear of the floating start-workout button, as a person would.
        rule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        // Other followed chains may be suggested too; dismiss the one beside this row.
        rule.onNode(
            hasContentDescription(text(R.string.suggestion_dismiss)) and
                hasAnySibling(hasText(exerciseName) or hasAnyDescendant(hasText(exerciseName)))
        ).performClick()
        rule.waitForIdle()
        rule.onNodeWithText(exerciseName).assertDoesNotExist()
        assertTrue(step.chainId in prefs.dismissedChains(LocalDate.now().toString()))
    }

    @Test
    fun startingASuggestionOpensTheWorkoutAtItsTarget() {
        launch()
        scrollTo(exerciseName)
        rule.onNodeWithText(exerciseName).performClick()
        rule.waitForIdle()

        // An untrained step starts at its working standard, not the exercise's own 5 × 25.
        rule.onAllNodes(hasSetTextAction() and hasText("25")).assertCountEquals(0)
        rule.onAllNodes(hasSetTextAction() and hasText("${step.working.sets}")).fetchSemanticsNodes().isNotEmpty().let(::assertTrue)
        rule.onAllNodes(hasSetTextAction() and hasText("${step.working.value}")).fetchSemanticsNodes().isNotEmpty().let(::assertTrue)
    }

    @Test
    fun unfollowingTheChainInTheCatalogueStopsItsSuggestion() {
        launch()
        scrollTo(exerciseName)
        rule.onNode(hasText(text(PrimaryDestination.LIBRARY.label)) and hasClickAction()).performClick()
        rule.onNodeWithText(text(R.string.library_catalogue)).performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(text(Catalogue.chain(step.chainId)!!.name)))
        rule.onNodeWithText(text(Catalogue.chain(step.chainId)!!.name)).performClick()
        rule.onNodeWithText(text(R.string.catalogue_follow)).performClick()
        assertTrue(step.chainId in prefs.unfollowedChains())

        // Back to Library, then Today.
        repeat(2) {
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            rule.waitForIdle()
        }
        val today = hasText(text(PrimaryDestination.TODAY.label)) and hasClickAction()
        rule.waitUntil(TIMEOUT_MS) { rule.onAllNodes(today).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(today).performClick()
        rule.waitForIdle()
        rule.onNodeWithText(exerciseName).assertDoesNotExist()
    }

    @Test
    fun turningSuggestionsOffInSettingsHidesThem() {
        launch()
        rule.onNode(hasContentDescription(text(R.string.settings))).performClick()
        // Settings is a lazy list; the row exists once scrolled to.
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(text(R.string.settings_suggestions)))
        rule.onNodeWithText(text(R.string.settings_suggestions)).performClick()
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()

        rule.onNodeWithText(text(R.string.today_suggested_title)).assertDoesNotExist()
        rule.onNodeWithText(exerciseName).assertDoesNotExist()
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
