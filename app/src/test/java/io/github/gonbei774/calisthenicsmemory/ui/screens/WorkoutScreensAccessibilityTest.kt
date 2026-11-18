package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepDynamicAuto
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepDynamicManual
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepDynamicSimple
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepIsometricAuto
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepIsometricManual
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramIntervalStep
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramStartIntervalStep
import io.github.gonbei774.calisthenicsmemory.ui.components.single.SingleExecutingStepDynamicAuto
import io.github.gonbei774.calisthenicsmemory.ui.components.single.SingleExecutingStepDynamicManual
import io.github.gonbei774.calisthenicsmemory.ui.components.single.SingleExecutingStepDynamicSimple
import io.github.gonbei774.calisthenicsmemory.ui.components.single.SingleExecutingStepIsometricAuto
import io.github.gonbei774.calisthenicsmemory.ui.components.single.SingleExecutingStepIsometricManual
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import io.github.gonbei774.calisthenicsmemory.util.SoundPlayer
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The workout screens at Android's largest font size (200 %), checked like the main screens: every
 * element that can be tapped has a label and a touch area of at least 48 × 48 dp (tapTargetProblems),
 * and no text is cut off, shortened or split inside a word (textFitProblems). Each step is shown on
 * its own, with a made-up exercise, as it looks mid-workout.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class WorkoutScreensAccessibilityTest {
    // Android's largest font size, where text is most likely not to fit.
    @Before fun largeFont() = RuntimeEnvironment.setFontScale(2f)
    @After fun normalFont() = RuntimeEnvironment.setFontScale(1f)

    @get:Rule
    val compose = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val sound by lazy { SoundPlayer(context) }
    private val flash by lazy { FlashController(context) }

    private fun single(type: String, unilateral: Boolean = false) = WorkoutSession(
        exercise = Exercise(id = 1, name = "Push-up", type = type, laterality = if (unilateral) "Unilateral" else "Bilateral"),
        totalSets = 3, targetValue = 12, repDuration = 2, startInterval = 5, intervalDuration = 60,
        sets = MutableList(3) { WorkoutSet(setNumber = it + 1, side = null, targetValue = 12, previousValue = 10) },
    )

    private fun program(type: String) = ProgramExecutionSession(
        program = Program(id = 1, name = "Upper body"),
        exercises = listOf(
            ProgramExercise(id = 1, programId = 1, exerciseId = 1, sortOrder = 0, targetValue = 12) to
                Exercise(id = 1, name = "Push-up", type = type, repDuration = 2),
        ),
        sets = MutableList(3) { ProgramWorkoutSet(exerciseIndex = 0, setNumber = it + 1, side = null, targetValue = 12, intervalSeconds = 60) },
    )

    // [actions]: the screen of a running set, which must show its quiet actions on screen, not pushed out of view.
    private fun audit(screen: String, actions: List<String> = emptyList(), content: @Composable () -> Unit) {
        compose.setContent { CalisthenicsMemoryTheme { content() } }
        compose.waitForIdle()
        actions.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        // The screen really rendered its controls, so a pass means something.
        val controls = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().size
        assertTrue("$screen shows no controls", controls >= 2)
        val problems = (tapTargetProblems(compose, compose.density) + textFitProblems(compose)).map { "$screen: $it" }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test fun singleGetReady() = audit("Get ready") {
        StartIntervalStep(single("Dynamic"), 0, sound, flash, isFlashEnabled = false, onIntervalComplete = {}, onSkip = {})
    }
    @Test fun singleRest() = audit("Rest") {
        IntervalStep(single("Dynamic"), 1, sound, flash, isFlashEnabled = false, onIntervalComplete = {}, onSkip = {}, onUpdateInterval = {})
    }
    // With effort rating on, the rest also asks about the set just done (ADR 0012).
    @Test fun singleRestWithEffort() = audit("Rest, rating reps") {
        val session = single("Dynamic").also { it.sets[0].isCompleted = true; it.sets[0].actualValue = 12 }
        IntervalStep(session, 1, sound, flash, isFlashEnabled = false, onIntervalComplete = {}, onSkip = {}, onUpdateInterval = {}, showEffort = true)
    }
    @Test fun singleRestWithEffortOnAHold() = audit("Rest, rating a hold") {
        val session = single("Isometric").also { it.sets[0].isCompleted = true; it.sets[0].actualValue = 40 }
        IntervalStep(session, 1, sound, flash, isFlashEnabled = false, onIntervalComplete = {}, onSkip = {}, onUpdateInterval = {}, showEffort = true)
    }
    @Test fun singleRepsManual() = audit("Reps, manual", actions = listOf("Restart set", "Skip set")) {
        SingleExecutingStepDynamicManual(single("Dynamic"), 0, sound, flash, isFlashEnabled = false, isCountSoundEnabled = false, onSetComplete = {}, onSkip = {}, onAbort = {}, onRetry = {})
    }
    @Test fun singleRepsAuto() = audit("Reps, auto", actions = listOf("Restart set", "Skip set")) {
        SingleExecutingStepDynamicAuto(single("Dynamic"), 0, sound, flash, isFlashEnabled = false, isCountSoundEnabled = false, onSetComplete = {}, onSkip = {}, onAbort = {}, onRetry = {})
    }
    @Test fun singleRepsCounter() = audit("Reps, counter", actions = listOf("Restart set", "Skip set")) {
        SingleExecutingStepDynamicSimple(single("Dynamic"), 0, sound, flash, isFlashEnabled = false, onSetComplete = {}, onSkip = {}, onAbort = {}, onRetry = {})
    }
    @Test fun singleHoldManual() = audit("Hold, manual", actions = listOf("Restart set", "Skip set")) {
        SingleExecutingStepIsometricManual(single("Isometric"), 0, sound, flash, isFlashEnabled = false, isIntervalSoundEnabled = false, intervalSeconds = 0, onSetComplete = {}, onSkip = {}, onAbort = {}, onRetry = {})
    }
    @Test fun singleHoldAuto() = audit("Hold, auto", actions = listOf("Restart set", "Skip set")) {
        SingleExecutingStepIsometricAuto(single("Isometric"), 0, sound, flash, isFlashEnabled = false, isIntervalSoundEnabled = false, intervalSeconds = 0, onSetComplete = {}, onSkip = {}, onAbort = {}, onRetry = {})
    }

    @Test fun programGetReady() = audit("Program get ready") {
        ProgramStartIntervalStep(program("Dynamic"), 0, startCountdownSeconds = 5, soundPlayer = sound, flashController = flash, isFlashEnabled = false, onComplete = {}, onSkip = {})
    }
    @Test fun programRest() = audit("Program rest") {
        ProgramIntervalStep(program("Dynamic"), 0, sound, flash, isFlashEnabled = false, onComplete = {}, onSkip = {})
    }
    @Test fun programRestWithEffort() = audit("Program rest, rating reps") {
        val session = program("Dynamic").also { it.sets[0].isCompleted = true; it.sets[0].actualValue = 12 }
        ProgramIntervalStep(session, 0, sound, flash, isFlashEnabled = false, onComplete = {}, onSkip = {}, showEffort = true)
    }
    @Test fun programRepsManual() = audit("Program reps, manual", actions = listOf("Restart set", "Skip set", "Skip exercise")) {
        ProgramExecutingStepDynamicManual(program("Dynamic"), 0, sound, flash, isFlashEnabled = false, isCountSoundEnabled = false, onSetComplete = {}, onAbort = {}, onRetry = {}, onSkipSet = {}, onSkipExercise = {})
    }
    @Test fun programRepsAuto() = audit("Program reps, auto", actions = listOf("Restart set", "Skip set", "Skip exercise")) {
        ProgramExecutingStepDynamicAuto(program("Dynamic"), 0, sound, flash, isFlashEnabled = false, isCountSoundEnabled = false, onSetComplete = {}, onAbort = {}, onRetry = {}, onSkipSet = {}, onSkipExercise = {})
    }
    @Test fun programRepsCounter() = audit("Program reps, counter", actions = listOf("Restart set", "Skip set", "Skip exercise")) {
        ProgramExecutingStepDynamicSimple(program("Dynamic"), 0, onSetComplete = {}, onAbort = {}, onRetry = {}, onSkipSet = {}, onSkipExercise = {})
    }
    @Test fun programHoldManual() = audit("Program hold, manual", actions = listOf("Restart set", "Skip set", "Skip exercise")) {
        ProgramExecutingStepIsometricManual(program("Isometric"), 0, sound, flash, isFlashEnabled = false, isIntervalSoundEnabled = false, intervalSeconds = 0, onSetComplete = {}, onAbort = {}, onRetry = {}, onSkipSet = {}, onSkipExercise = {})
    }
    @Test fun programHoldAuto() = audit("Program hold, auto", actions = listOf("Restart set", "Skip set", "Skip exercise")) {
        ProgramExecutingStepIsometricAuto(program("Isometric"), 0, sound, flash, isFlashEnabled = false, isIntervalSoundEnabled = false, intervalSeconds = 0, onSetComplete = {}, onAbort = {}, onRetry = {}, onSkipSet = {}, onSkipExercise = {})
    }
}
