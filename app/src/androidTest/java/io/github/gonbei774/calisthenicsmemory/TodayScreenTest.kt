package io.github.gonbei774.calisthenicsmemory

import androidx.compose.ui.test.assertCountEquals
import java.time.format.FormatStyle
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.SavedWorkoutState
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import io.github.gonbei774.calisthenicsmemory.ui.screens.SingleSessionCheckpoint
import kotlinx.coroutines.runBlocking
import io.github.gonbei774.calisthenicsmemory.ui.screens.view.formatDate
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Today lists unfinished workouts and due to-dos from real saved state, and opens the owning screens. */
@RunWith(AndroidJUnit4::class)
class TodayScreenTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = AppDatabase.getDatabase(context)
    private val singleCheckpoint = SingleSessionCheckpoint.file(context.filesDir)
    private val savedProgram = SavedWorkoutState(context)

    // Synthetic names, unique per run so rows left by another test cannot match.
    private val suffix = System.nanoTime().toString()
    private val exerciseName = "Today test squat $suffix"
    private val programName = "Today test program $suffix"
    private var exerciseId = 0L
    private var programId = 0L
    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun text(id: Int) = context.getString(id)

    @Before
    fun seed(): Unit = runBlocking {
        exerciseId = database.exerciseDao().insertExercise(Exercise(name = exerciseName, type = "Dynamic"))
        programId = database.programDao().insert(Program(name = programName))
        database.todoTaskDao().insert(TodoTask(type = TodoTask.TYPE_EXERCISE, referenceId = exerciseId, sortOrder = 0))
        singleCheckpoint.save(
            SingleSessionCheckpoint(
                exerciseId = exerciseId, totalSets = 1, targetValue = 10, repDuration = null, startInterval = 0,
                intervalDuration = 60, sets = emptyList(), comment = "", isAutoMode = false,
                isDynamicCountSoundEnabled = false, currentSetIndex = 0, atConfirmation = false, fromToDo = false,
                savedAtWallMillis = System.currentTimeMillis(),
            )
        )
        savedProgram.save(programId, currentSetIndex = 0, sets = emptyList(), comment = "")
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::scenario.isInitialized) scenario.close()
        singleCheckpoint.clear()
        savedProgram.clear()
        database.exerciseDao().deleteExerciseAndTodoTasks(database.exerciseDao().getExerciseById(exerciseId)!!)
        database.programDao().deleteById(programId)
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        rule.waitUntil(STARTUP_TIMEOUT_MS) {
            rule.onAllNodesWithText(exerciseName).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun pressBack() {
        rule.waitForIdle()
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test
    fun listsUnfinishedWorkoutsBeforeDueToDos() {
        launch()
        rule.onNode(hasText(text(R.string.today_resume_title)) and isHeading()).assertExists()
        // Once on the resume card, once as a due to-do.
        rule.onAllNodesWithText(exerciseName).assertCountEquals(2)
        rule.onNodeWithText(programName).assertExists()
        rule.onNodeWithText("${text(R.string.today_kind_program)} · ${text(R.string.today_saved_for_later)}").assertExists()
        rule.onNode(hasText(text(R.string.today_due_title)) and isHeading()).assertExists()
    }

    @Test
    fun tappingADayOpensItsHistoryInProgress() {
        launch()
        // Today's cell names the full date; trained or not, it opens that day on the Progress calendar.
        val date = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(context.resources.configuration.locales[0]))
        rule.onNode(hasContentDescription(date, substring = true) and hasClickAction()).performClick()
        rule.onNode(hasText(text(PrimaryDestination.PROGRESS.label)) and isHeading()).assertExists()
        rule.onNode(hasText(text(R.string.tab_calendar)) and isSelected()).assertExists()
        // The stats narrow from the week to that one day.
        val locale = context.resources.configuration.locales[0]
        rule.onAllNodesWithText(formatDate(LocalDate.now(), locale)).fetchSemanticsNodes().isNotEmpty().let(::assertTrue)
    }

    @Test
    fun resumingOpensTheWorkoutScreensOwnResumeOffer() {
        launch()
        // The week's hero number can push the resume card below the fold on small screens.
        rule.onAllNodesWithText(exerciseName)[0].performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.interval_resume_title)).assertExists()
        rule.onAllNodesWithTag(PRIMARY_NAVIGATION_BAR_TAG).assertCountEquals(0)
    }

    @Test
    fun aDueToDoOpensItsWorkoutAndBackReturnsToToday() {
        singleCheckpoint.clear()
        savedProgram.clear()
        launch()
        rule.onNodeWithText(text(R.string.today_resume_title)).assertDoesNotExist()
        rule.onNodeWithText(exerciseName).performScrollTo().performClick()
        rule.onAllNodesWithTag(PRIMARY_NAVIGATION_BAR_TAG).assertCountEquals(0)

        pressBack()
        // With nothing to resume, the first due to-do is the "Next up" card.
        rule.onNode(hasText(text(R.string.today_next_up)) and isHeading()).assertExists()
    }

    private companion object {
        const val STARTUP_TIMEOUT_MS = 10_000L
    }
}
