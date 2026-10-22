package io.github.gonbei774.calisthenicsmemory

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepDynamicAuto
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepIsometricAuto
import io.github.gonbei774.calisthenicsmemory.ui.components.single.SingleExecutingStepDynamicAuto
import io.github.gonbei774.calisthenicsmemory.ui.components.single.SingleExecutingStepIsometricAuto
import io.github.gonbei774.calisthenicsmemory.ui.screens.WorkoutSession
import io.github.gonbei774.calisthenicsmemory.ui.screens.WorkoutSet
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import io.github.gonbei774.calisthenicsmemory.util.SoundPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * In auto mode a set ends by itself when the target is reached; ending it early with the complete
 * button asks for the value first, exactly as manual mode does, instead of saving the count so far.
 */
@RunWith(AndroidJUnit4::class)
class AutoModeConfirmTest {
    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val sound = SoundPlayer(context)
    private val flash = FlashController(context)
    private val confirmTitle = context.getString(R.string.record_confirm_title)
    private val save = context.getString(R.string.save)
    private val cancel = context.getString(R.string.cancel)
    private val completeButton = hasClickAction() and
        hasText(context.getString(R.string.complete_with_time, 0).substringBefore("(").trim(), substring = true)

    private fun singleSession(type: String, target: Int) = WorkoutSession(
        exercise = Exercise(id = 1, name = "Push-up", type = type),
        totalSets = 1, targetValue = target, repDuration = 1, startInterval = 0, intervalDuration = 60,
        sets = mutableListOf(WorkoutSet(setNumber = 1, side = null, targetValue = target)),
        isAutoMode = true,
    )

    private fun programSession(type: String, target: Int) = ProgramExecutionSession(
        program = Program(id = 1, name = "Upper body"),
        exercises = listOf(
            ProgramExercise(id = 1, programId = 1, exerciseId = 1, sortOrder = 0, targetValue = target) to
                Exercise(id = 1, name = "Push-up", type = type, repDuration = 1)
        ),
        sets = mutableListOf(ProgramWorkoutSet(exerciseIndex = 0, setNumber = 1, side = null, targetValue = target, intervalSeconds = 60)),
    )

    private fun showSingle(type: String, target: Int, completed: MutableList<WorkoutSession>): WorkoutSession {
        val session = singleSession(type, target)
        rule.setContent {
            if (type == "Dynamic") {
                SingleExecutingStepDynamicAuto(
                    session = session, currentSetIndex = 0, soundPlayer = sound, flashController = flash,
                    isFlashEnabled = false, isCountSoundEnabled = false,
                    onSetComplete = { completed += it }, onSkip = {}, onAbort = {}, onRetry = {},
                )
            } else {
                SingleExecutingStepIsometricAuto(
                    session = session, currentSetIndex = 0, soundPlayer = sound, flashController = flash,
                    isFlashEnabled = false, isIntervalSoundEnabled = false, intervalSeconds = 0,
                    onSetComplete = { completed += it }, onSkip = {}, onAbort = {}, onRetry = {},
                )
            }
        }
        return session
    }

    private fun showProgram(type: String, target: Int, completed: MutableList<Int>) {
        val session = programSession(type, target)
        rule.setContent {
            if (type == "Dynamic") {
                ProgramExecutingStepDynamicAuto(
                    session = session, currentSetIndex = 0, soundPlayer = sound, flashController = flash,
                    isFlashEnabled = false, isCountSoundEnabled = false,
                    onSetComplete = { completed += it }, onAbort = {}, onRetry = {},
                )
            } else {
                ProgramExecutingStepIsometricAuto(
                    session = session, currentSetIndex = 0, soundPlayer = sound, flashController = flash,
                    isFlashEnabled = false, isIntervalSoundEnabled = false, intervalSeconds = 0,
                    onSetComplete = { completed += it }, onAbort = {}, onRetry = {},
                )
            }
        }
    }

    private fun assertSingleAsksFirst(type: String) {
        val completed = mutableListOf<WorkoutSession>()
        val session = showSingle(type, target = 300, completed)
        rule.onNode(completeButton).performClick()
        rule.onNodeWithText(confirmTitle).assertExists()
        assertTrue("$type saved without asking", completed.isEmpty())

        rule.onNodeWithText(save).performClick()
        rule.waitForIdle()
        assertEquals(type, 1, completed.size)
        assertTrue(type, session.sets[0].isCompleted)
    }

    @Test
    fun anEarlyCompleteAsksBeforeSavingADynamicSet() = assertSingleAsksFirst("Dynamic")

    @Test
    fun anEarlyCompleteAsksBeforeSavingAnIsometricSet() = assertSingleAsksFirst("Isometric")

    @Test
    fun cancellingTheQuestionKeepsTheSetGoing() {
        val completed = mutableListOf<WorkoutSession>()
        val session = showSingle("Dynamic", target = 300, completed)
        rule.onNode(completeButton).performClick()
        rule.onNodeWithText(cancel).performClick()
        rule.onNodeWithText(confirmTitle).assertDoesNotExist()
        assertTrue(completed.isEmpty())
        assertTrue(!session.sets[0].isCompleted)
    }

    @Test
    fun reachingTheTargetStillEndsTheSetWithoutAsking() {
        val completed = mutableListOf<WorkoutSession>()
        val session = showSingle("Dynamic", target = 1, completed)
        rule.waitUntil(timeoutMillis = 5_000) { completed.isNotEmpty() }
        rule.onNodeWithText(confirmTitle).assertDoesNotExist()
        assertEquals(1, session.sets[0].actualValue)
    }

    private fun assertProgramAsksFirst(type: String) {
        val completed = mutableListOf<Int>()
        showProgram(type, target = 300, completed)
        rule.onNode(completeButton).performClick()
        rule.onNodeWithText(confirmTitle).assertExists()
        assertTrue("$type saved without asking", completed.isEmpty())

        rule.onNodeWithText(save).performClick()
        rule.waitForIdle()
        assertEquals(type, 1, completed.size)
    }

    @Test
    fun anEarlyCompleteAsksBeforeSavingAProgramDynamicSet() = assertProgramAsksFirst("Dynamic")

    @Test
    fun anEarlyCompleteAsksBeforeSavingAProgramIsometricSet() = assertProgramAsksFirst("Isometric")

    @Test
    fun aProgramSetStillEndsByItselfAtTheTarget() {
        val completed = mutableListOf<Int>()
        showProgram("Isometric", target = 1, completed)
        rule.waitUntil(timeoutMillis = 5_000) { completed.isNotEmpty() }
        rule.onNodeWithText(confirmTitle).assertDoesNotExist()
        assertEquals(listOf(1), completed)
    }
}
