package io.github.gonbei774.calisthenicsmemory

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
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
import io.github.gonbei774.calisthenicsmemory.ui.screens.view.WORKOUT_EDITOR_LIST_TAG
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/** The workout editor through the real screens: open it from History, change the workout, save or not. */
@RunWith(AndroidJUnit4::class)
class WorkoutEditorScreenTest {
    // Before the app launches: the welcome guide would otherwise open first.
    @get:Rule(order = 0)
    val welcomeSeen = WelcomeSeenRule()

    @get:Rule(order = 1)
    val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = AppDatabase.getDatabase(context)

    // Synthetic data, unique per run.
    private val exerciseName = "Editor squat ${System.nanoTime()}"
    private val otherName = "Editor plank ${System.nanoTime()}"
    private var exerciseId = 0L
    private var otherId = 0L
    private var sessionId = 0L
    private var occurrenceId = 0L
    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun text(id: Int) = context.getString(id)

    @Before
    fun seed(): Unit = runBlocking {
        exerciseId = database.exerciseDao().insertExercise(Exercise(name = exerciseName, type = "Dynamic"))
        otherId = database.exerciseDao().insertExercise(Exercise(name = otherName, type = "Isometric"))
        val startedAt = LocalDate.now().atTime(20, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dao = database.workoutSessionDao()
        sessionId = dao.insertSession(
            WorkoutSessionEntity(
                status = WorkoutSessionStatus.COMPLETED, sourceType = WorkoutSourceType.AD_HOC,
                startedAtEpochMillis = startedAt, updatedAtEpochMillis = startedAt, timePrecision = TimePrecision.EXACT,
            )
        )
        occurrenceId = dao.insertSessionExercise(
            SessionExerciseEntity(
                workoutSessionId = sessionId, orderIndex = 0, exerciseId = exerciseId, exerciseNameSnapshot = exerciseName,
                exerciseKindSnapshot = ExerciseKind.DYNAMIC, lateralitySnapshot = Laterality.BILATERAL,
            )
        )
        dao.insertSetEntries(
            listOf(
                SetEntryEntity(
                    sessionExerciseId = occurrenceId, orderIndex = 0, setNumber = 1, status = SetEntryStatus.COMPLETED,
                    side = BodySide.BILATERAL, repetitions = 10, completedAtEpochMillis = startedAt, timePrecision = TimePrecision.EXACT,
                )
            )
        )
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::scenario.isInitialized) scenario.close()
        database.openHelper.writableDatabase.execSQL("DELETE FROM workout_sessions WHERE id = ?", arrayOf<Any>(sessionId))
        database.exerciseDao().deleteExerciseById(exerciseId)
        database.exerciseDao().deleteExerciseById(otherId)
    }

    private fun openEditor() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val progress = text(PrimaryDestination.PROGRESS.label)
        rule.waitUntil(TIMEOUT_MS) { rule.onAllNodesWithText(progress).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(hasText(progress) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))).performClick()
        rule.onNodeWithText(text(R.string.tab_list)).performClick()
        rule.waitUntil(TIMEOUT_MS) { rule.onAllNodesWithText(exerciseName).fetchSemanticsNodes().isNotEmpty() }
        // Newest first: the seeded workout is the first card.
        rule.onAllNodesWithContentDescription(text(R.string.menu))[0].performClick()
        rule.onNodeWithText(text(R.string.edit)).performClick()
        rule.waitUntil(TIMEOUT_MS) { rule.onAllNodesWithText(text(R.string.workout_edit_title)).fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(TIMEOUT_MS) { rule.onAllNodes(hasText(exerciseName) and hasAnyAncestor(isDialog())).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun save() = rule.onNode(hasText(text(R.string.save)) and hasAnyAncestor(isDialog()))

    private suspend fun repetitions() = database.workoutSessionDao().setEntries(occurrenceId).map { it.repetitions }

    @Test
    fun anAddedSetIsSavedOnlyOnceItHasAValue() {
        openEditor()
        save().assertIsNotEnabled()
        rule.onNodeWithText(text(R.string.workout_edit_add_set)).performClick()
        // An empty set cannot be saved; nothing is filled in for the user.
        save().assertIsNotEnabled()
        // The fields are the comment, then each set's count in order: the new set is the last.
        val fields = rule.onAllNodes(hasSetTextAction() and hasAnyAncestor(isDialog()))
        fields[fields.fetchSemanticsNodes().size - 1].performTextInput("12")
        save().assertIsEnabled().performClick()

        rule.waitUntil(TIMEOUT_MS) { runBlocking { repetitions() == listOf(10, 12) } }
        rule.waitUntil(TIMEOUT_MS) { rule.onAllNodes(hasText(text(R.string.workout_edit_title))).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun anExerciseIsAddedFromTheLibraryAndTheOldOneRemoved() {
        openEditor()
        rule.onNodeWithContentDescription(context.getString(R.string.workout_edit_remove_exercise, exerciseName)).performClick()
        rule.onNodeWithText(text(R.string.workout_edit_add_exercise)).performClick()
        rule.onNodeWithText(otherName).performClick()
        rule.onNode(hasTestTag(WORKOUT_EDITOR_LIST_TAG)).performScrollToNode(hasText(otherName))
        val fields = rule.onAllNodes(hasSetTextAction() and hasAnyAncestor(isDialog()))
        fields[fields.fetchSemanticsNodes().size - 1].performTextInput("45")
        save().performClick()

        rule.waitUntil(TIMEOUT_MS) {
            runBlocking { database.workoutSessionDao().sessionExercises(sessionId).map { it.exerciseId } == listOf(otherId) }
        }
        val plank = runBlocking { database.workoutSessionDao().sessionExercises(sessionId).single() }
        assertEquals(listOf(45_000L), runBlocking { database.workoutSessionDao().setEntries(plank.id).map { it.durationMillis } })
    }

    @Test
    fun closingWithChangesAsksAndDiscardingSavesNothing() {
        openEditor()
        // The first field is the comment.
        rule.onAllNodes(hasSetTextAction() and hasAnyAncestor(isDialog()))[0].performTextReplacement("Changed")
        rule.onNode(hasContentDescription(text(R.string.cancel)) and hasAnyAncestor(isDialog())).performClick()
        rule.onNodeWithText(text(R.string.discard_changes_title)).assertExists()
        rule.onNodeWithText(text(R.string.discard)).performClick()

        rule.waitUntil(TIMEOUT_MS) { rule.onAllNodes(hasText(text(R.string.workout_edit_title))).fetchSemanticsNodes().isEmpty() }
        runBlocking {
            assertNull(database.workoutSessionDao().session(sessionId)!!.comment)
            assertEquals(listOf(10), repetitions())
        }
    }

    @Test
    fun removingTheLastSetAsksAndThenDeletesTheWorkout() {
        openEditor()
        rule.onNodeWithContentDescription(context.getString(R.string.workout_edit_remove_set, 1)).performClick()
        save().performClick()
        rule.onNodeWithText(text(R.string.workout_edit_delete_title)).assertExists()
        rule.onNode(hasText(text(R.string.delete)) and hasAnyAncestor(isDialog())).performClick()

        rule.waitUntil(TIMEOUT_MS) { runBlocking { database.workoutSessionDao().session(sessionId) == null } }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
