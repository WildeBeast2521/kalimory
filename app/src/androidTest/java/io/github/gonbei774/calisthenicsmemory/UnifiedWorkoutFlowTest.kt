package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import java.io.File
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutCheckpointStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.INTERVAL_CHECKPOINT_FILE
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.data.ProgramSessionCheckpointStore
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSourceType
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination
import io.github.gonbei774.calisthenicsmemory.ui.screens.SingleSessionCheckpoint
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end workout flows through the real screens (Task 8 acceptance): each run is saved as
 * one v2 session, an interrupted run is offered for resume after the activity is recreated, and
 * an abandoned run saves nothing.
 */
@RunWith(AndroidJUnit4::class)
class UnifiedWorkoutFlowTest {
    // Before the app launches: the welcome guide would otherwise open first.
    @get:Rule(order = 0)
    val welcomeSeen = WelcomeSeenRule()

    @get:Rule(order = 1)
    val rule = createEmptyComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = AppDatabase.getDatabase(context)
    private val prefs = context.getSharedPreferences("workout_preferences", Context.MODE_PRIVATE)
    private var savedPrefs: Map<String, *> = emptyMap<String, Any>()

    // Synthetic data, unique per run.
    private val exerciseName = "Flow squat ${System.nanoTime()}"
    private var exerciseId = 0L
    private var programId = 0L
    private var intervalProgramId = 0L
    private var sessionsBefore = emptySet<Long>()
    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun text(id: Int) = context.getString(id)

    @Before
    fun seed(): Unit = runBlocking {
        savedPrefs = prefs.all.toMap()
        WorkoutPreferences(context).apply {
            // No countdowns or rests, and sets are finished with the Complete button.
            setStartCountdownEnabled(false)
            // Program mode reads the seconds directly.
            setStartCountdown(0)
            setSetIntervalEnabled(false)
            setAutoMode(false)
        }
        exerciseId = database.exerciseDao().insertExercise(
            Exercise(name = exerciseName, type = "Dynamic", targetSets = 2, targetValue = 3, repDuration = 1, restInterval = 0)
        )
        val programName = "Flow program ${System.nanoTime()}"
        programId = database.programDao().insert(Program(name = programName))
        database.programExerciseDao().insert(
            ProgramExercise(programId = programId, exerciseId = exerciseId, sortOrder = 0, sets = 2, targetValue = 3, intervalSeconds = 0)
        )
        intervalProgramId = database.intervalProgramDao().insert(
            IntervalProgram(name = "Flow interval ${System.nanoTime()}", workSeconds = 2, restSeconds = 1, rounds = 1, roundRestSeconds = 0)
        )
        database.intervalProgramExerciseDao().insert(
            IntervalProgramExercise(programId = intervalProgramId, exerciseId = exerciseId, sortOrder = 0)
        )
        sessionsBefore = database.workoutSessionDao().sessionsNewestFirst().map { it.id }.toSet()
        clearCheckpoints()
    }

    private fun clearCheckpoints() {
        SingleSessionCheckpoint.file(context.filesDir).clear()
        ProgramSessionCheckpointStore(File(context.filesDir, ProgramSessionCheckpointStore.FILE_NAME)).clear()
        WorkoutCheckpointStore(File(context.filesDir, INTERVAL_CHECKPOINT_FILE)).clear()
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::scenario.isInitialized) scenario.close()
        val created = newSessions().map { it.id }
        if (created.isNotEmpty()) database.workoutSessionDao().deleteSessions(created)
        database.programDao().deleteById(programId)
        database.intervalProgramDao().deleteById(intervalProgramId)
        database.todoTaskDao().deleteByReference(TodoTask.TYPE_PROGRAM, programId)
        database.todoTaskDao().deleteByReference(TodoTask.TYPE_INTERVAL, intervalProgramId)
        database.exerciseDao().deleteExerciseAndTodoTasks(database.exerciseDao().getExerciseById(exerciseId)!!)
        clearCheckpoints()
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

    private suspend fun newSessions(): List<WorkoutSessionEntity> =
        database.workoutSessionDao().sessionsNewestFirst().filter { it.id !in sessionsBefore }

    private fun waitForText(value: String, substring: Boolean = false) = rule.waitUntil(TIMEOUT_MS) {
        rule.onAllNodesWithText(value, substring = substring).fetchSemanticsNodes().isNotEmpty()
    }

    private fun launchAndOpenTrain() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val train = text(PrimaryDestination.TRAIN.label)
        waitForText(train)
        rule.onNode(hasText(train) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))).performClick()
    }

    /** Train, then Workout, then the seeded exercise, then Start. Quick start skips any mode choice. */
    private fun startSingleWorkout() {
        launchAndOpenTrain()
        rule.onNodeWithText(text(R.string.home_workout)).performClick()
        waitForText(text(R.string.no_group))
        rule.onNodeWithText(text(R.string.no_group)).performClick()
        waitForText(exerciseName)
        rule.onNodeWithText(exerciseName).performClick()
        // Sets and targets start blank; this fills them from the exercise (2 sets of 3 reps).
        waitForText(text(R.string.apply_exercise_settings))
        rule.onNodeWithText(text(R.string.apply_exercise_settings)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.start_workout)).performScrollTo().performClick()
    }

    /** Finishes the set on screen with the Complete button and saves it at its target value. */
    private fun completeSet() {
        waitForText(COMPLETE, substring = true)
        rule.onNodeWithText(COMPLETE, substring = true).performClick()
        // The counter may still be at 0; a set saved without a value counts as not done.
        rule.onNode(hasText(SET_TO_TARGET, substring = true) and hasAnyAncestor(isDialog())).performClick()
        rule.onNode(hasText(text(R.string.save)) and hasAnyAncestor(isDialog())).performClick()
    }

    /** Opens the program or interval screen from its due to-do on Today, as a user would. */
    private fun openFromToday(type: String, referenceId: Long, name: String) = runBlocking {
        database.todoTaskDao().insert(TodoTask(type = type, referenceId = referenceId, sortOrder = 0))
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForText(name)
        rule.onNodeWithText(name).performClick()
    }

    private suspend fun programName() = database.programDao().getProgramById(programId)!!.name
    private suspend fun intervalName() = database.intervalProgramDao().getProgramById(intervalProgramId)!!.name

    private fun pressBack() {
        rule.waitForIdle()
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test
    fun singleWorkoutIsSavedAsOneSession() = runBlocking {
        startSingleWorkout()
        completeSet()
        completeSet()
        waitForText(text(R.string.record_button))
        rule.onNodeWithText(text(R.string.record_button)).performClick()

        rule.waitUntil(TIMEOUT_MS) { runBlocking { newSessions().isNotEmpty() } }
        val session = newSessions().single()
        assertEquals(WorkoutSourceType.AD_HOC, session.sourceType)
        val sets = database.workoutSessionDao().sessionGraph(session.id)!!.exercises.single().second
        assertEquals(listOf(SetEntryStatus.COMPLETED, SetEntryStatus.COMPLETED), sets.map { it.status })
        assertNull(SingleSessionCheckpoint.file(context.filesDir).load())
        // The summary follows the save, and Done returns to where the workout started.
        waitForText(text(R.string.workout_summary_title))
        rule.onNodeWithText(text(R.string.summary_done)).performClick()
        waitForText(text(PrimaryDestination.TRAIN.label))
    }

    @Test
    fun singleWorkoutSurvivesRecreationThroughTheResumeOffer() = runBlocking {
        startSingleWorkout()
        completeSet()
        waitForText(COMPLETE, substring = true)

        // The activity is recreated mid-workout; the screen state is gone but the checkpoint is not.
        scenario.recreate()
        waitForText(text(R.string.interval_resume_title))
        rule.onNodeWithText(text(R.string.interval_resume_confirm)).performClick()
        completeSet()
        waitForText(text(R.string.record_button))
        rule.onNodeWithText(text(R.string.record_button)).performClick()

        rule.waitUntil(TIMEOUT_MS) { runBlocking { newSessions().isNotEmpty() } }
        val sets = database.workoutSessionDao().sessionGraph(newSessions().single().id)!!.exercises.single().second
        assertEquals(2, sets.count { it.status == SetEntryStatus.COMPLETED })
    }

    @Test
    fun abandonedSingleWorkoutSavesNothing() = runBlocking {
        startSingleWorkout()
        completeSet()
        pressBack()
        rule.onNode(hasText(text(R.string.exit_workout_confirm)) and hasAnyAncestor(isDialog())).performClick()
        // Back on the Train destination, with nothing saved.
        rule.waitUntil(TIMEOUT_MS) { rule.onAllNodes(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG)).fetchSemanticsNodes().isNotEmpty() }

        assertEquals(emptyList<WorkoutSessionEntity>(), newSessions())
        assertNull(SingleSessionCheckpoint.file(context.filesDir).load())
    }

    @Test
    fun programWorkoutIsSavedAsOneSession() = runBlocking {
        openFromToday(TodoTask.TYPE_PROGRAM, programId, programName())
        waitForText(text(R.string.start_workout))
        rule.onNodeWithText(text(R.string.start_workout)).performClick()
        completeSet()
        completeSet()
        waitForText(text(R.string.record_button))
        rule.onNodeWithText(text(R.string.record_button)).performClick()

        rule.waitUntil(TIMEOUT_MS) { runBlocking { newSessions().isNotEmpty() } }
        val session = newSessions().single()
        assertEquals(WorkoutSourceType.PROGRAM_TEMPLATE, session.sourceType)
        assertEquals(programId, session.sourceTemplateId)
        val sets = database.workoutSessionDao().sessionGraph(session.id)!!.exercises.single().second
        assertEquals(listOf(3, 3), sets.map { it.repetitions })
        waitForText(text(R.string.workout_summary_title))
    }

    @Test
    fun programWorkoutSurvivesRecreationThroughTheResumeOffer() = runBlocking {
        openFromToday(TodoTask.TYPE_PROGRAM, programId, programName())
        waitForText(text(R.string.start_workout))
        rule.onNodeWithText(text(R.string.start_workout)).performClick()
        completeSet()
        waitForText(COMPLETE, substring = true)

        scenario.recreate()
        waitForText(text(R.string.interval_resume_title))
        rule.onNodeWithText(text(R.string.interval_resume_confirm)).performClick()
        completeSet()
        waitForText(text(R.string.record_button))
        rule.onNodeWithText(text(R.string.record_button)).performClick()

        rule.waitUntil(TIMEOUT_MS) { runBlocking { newSessions().isNotEmpty() } }
        val sets = database.workoutSessionDao().sessionGraph(newSessions().single().id)!!.exercises.single().second
        assertEquals(2, sets.count { it.status == SetEntryStatus.COMPLETED })
    }

    @Test
    fun pastWorkoutIsRecordedAsEnteredWithoutTickingSets() = runBlocking {
        launchAndOpenTrain()
        rule.onNodeWithText(text(R.string.home_record)).performScrollTo().performClick()
        waitForText(text(R.string.no_group))
        rule.onNodeWithText(text(R.string.no_group)).performClick()
        waitForText(exerciseName)
        rule.onNodeWithText(exerciseName).performClick()
        // Fills 2 sets of 3 reps from the exercise; no set is ticked as done.
        waitForText(text(R.string.apply_exercise_settings))
        rule.onNodeWithText(text(R.string.apply_exercise_settings)).performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(text(R.string.record_button)))
        rule.onNodeWithText(text(R.string.record_button)).performClick()

        rule.waitUntil(TIMEOUT_MS) { runBlocking { newSessions().isNotEmpty() } }
        val session = newSessions().single()
        assertEquals(WorkoutSourceType.MANUAL, session.sourceType)
        val sets = database.workoutSessionDao().sessionGraph(session.id)!!.exercises.single().second
        assertEquals(listOf(3, 3), sets.map { it.repetitions })
        assertEquals(listOf(SetEntryStatus.COMPLETED, SetEntryStatus.COMPLETED), sets.map { it.status })
    }

    @Test
    fun pastWorkoutWithSeveralExercisesIsOneSession() = runBlocking {
        launchAndOpenTrain()
        rule.onNodeWithText(text(R.string.home_record)).performScrollTo().performClick()
        repeat(2) { round ->
            // The group stays open after the first pick.
            if (rule.onAllNodesWithText(exerciseName).fetchSemanticsNodes().isEmpty()) {
                waitForText(text(R.string.no_group))
                rule.onNodeWithText(text(R.string.no_group)).performClick()
            }
            waitForText(exerciseName)
            rule.onNodeWithText(exerciseName).performClick()
            waitForText(text(R.string.apply_exercise_settings))
            rule.onNodeWithText(text(R.string.apply_exercise_settings)).performClick()
            // The first exercise is set aside; the second records the workout.
            val action = text(if (round == 0) R.string.record_add_exercise else R.string.record_button)
            rule.onNode(hasScrollAction()).performScrollToNode(hasText(action))
            rule.onNodeWithText(action).performClick()
        }

        rule.waitUntil(TIMEOUT_MS) { runBlocking { newSessions().isNotEmpty() } }
        val session = newSessions().single()
        assertEquals(WorkoutSourceType.MANUAL, session.sourceType)
        val exercises = database.workoutSessionDao().sessionGraph(session.id)!!.exercises
        assertEquals(listOf(0, 1), exercises.map { it.first.orderIndex })
        assertEquals(listOf(listOf(3, 3), listOf(3, 3)), exercises.map { (_, sets) -> sets.map { it.repetitions } })
    }

    @Test
    fun intervalWorkoutIsSavedAsOneSession() = runBlocking {
        openFromToday(TodoTask.TYPE_INTERVAL, intervalProgramId, intervalName())
        waitForText(text(R.string.start_workout))
        rule.onNodeWithText(text(R.string.start_workout)).performClick()
        rule.waitUntil(INTERVAL_TIMEOUT_MS) {
            rule.onAllNodesWithText(text(R.string.interval_save_and_finish)).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(text(R.string.interval_save_and_finish)).performClick()

        rule.waitUntil(TIMEOUT_MS) { runBlocking { newSessions().isNotEmpty() } }
        val session = newSessions().single()
        assertEquals(WorkoutSourceType.INTERVAL_TEMPLATE, session.sourceType)
        assertEquals(listOf(2, 1, 1, 0), listOf(session.intervalWorkSeconds, session.intervalRestSeconds, session.intervalRounds, session.intervalRoundRestSeconds))
        val sets = database.workoutSessionDao().sessionGraph(session.id)!!.exercises.single().second
        assertEquals(listOf(SetEntryStatus.COMPLETED), sets.map { it.status })
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
        const val INTERVAL_TIMEOUT_MS = 60_000L
        const val COMPLETE = "Complete ("
        const val SET_TO_TARGET = "Set to target"
    }
}
