package io.github.gonbei774.calisthenicsmemory.ui.screens

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

    private fun audit(screen: String, content: @Composable () -> Unit) {
        compose.setContent { CalisthenicsMemoryTheme { content() } }
        compose.waitForIdle()
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
    @Test fun singleRepsManual() = audit("Reps, manual") {
        SingleExecutingStepDynamicManual(single("Dynamic"), 0, sound, flash, isFlashEnabled = false, isCountSoundEnabled = false, onSetComplete = {}, onSkip = {}, onAbort = {}, onRetry = {})
    }
    @Test fun singleRepsAuto() = audit("Reps, auto") {
        SingleExecutingStepDynamicAuto(single("Dynamic"), 0, sound, flash, isFlashEnabled = false, isCountSoundEnabled = false, onSetComplete = {}, onSkip = {}, onAbort = {}, onRetry = {})
    }
    @Test fun singleRepsCounter() = audit("Reps, counter") {
        SingleExecutingStepDynamicSimple(single("Dynamic"), 0, sound, flash, isFlashEnabled = false, onSetComplete = {}, onSkip = {}, onAbort = {}, onRetry = {})
    }
    @Test fun singleHoldManual() = audit("Hold, manual") {
        SingleExecutingStepIsometricManual(single("Isometric"), 0, sound, flash, isFlashEnabled = false, isIntervalSoundEnabled = false, intervalSeconds = 0, onSetComplete = {}, onSkip = {}, onAbort = {}, onRetry = {})
    }
    @Test fun singleHoldAuto() = audit("Hold, auto") {
        SingleExecutingStepIsometricAuto(single("Isometric"), 0, sound, flash, isFlashEnabled = false, isIntervalSoundEnabled = false, intervalSeconds = 0, onSetComplete = {}, onSkip = {}, onAbort = {}, onRetry = {})
    }

    @Test fun programGetReady() = audit("Program get ready") {
        ProgramStartIntervalStep(program("Dynamic"), 0, startCountdownSeconds = 5, soundPlayer = sound, flashController = flash, isFlashEnabled = false, onComplete = {}, onSkip = {})
    }
    @Test fun programRest() = audit("Program rest") {
        ProgramIntervalStep(program("Dynamic"), 0, sound, flash, isFlashEnabled = false, onComplete = {}, onSkip = {})
    }
    @Test fun programRepsManual() = audit("Program reps, manual") {
        ProgramExecutingStepDynamicManual(program("Dynamic"), 0, sound, flash, isFlashEnabled = false, isCountSoundEnabled = false, onSetComplete = {}, onAbort = {}, onRetry = {})
    }
    @Test fun programRepsAuto() = audit("Program reps, auto") {
        ProgramExecutingStepDynamicAuto(program("Dynamic"), 0, sound, flash, isFlashEnabled = false, isCountSoundEnabled = false, onSetComplete = {}, onAbort = {}, onRetry = {})
    }
    @Test fun programRepsCounter() = audit("Program reps, counter") {
        ProgramExecutingStepDynamicSimple(program("Dynamic"), 0, onSetComplete = {}, onAbort = {})
    }
    @Test fun programHoldManual() = audit("Program hold, manual") {
        ProgramExecutingStepIsometricManual(program("Isometric"), 0, sound, flash, isFlashEnabled = false, isIntervalSoundEnabled = false, intervalSeconds = 0, onSetComplete = {}, onAbort = {}, onRetry = {})
    }
    @Test fun programHoldAuto() = audit("Program hold, auto") {
        ProgramExecutingStepIsometricAuto(program("Isometric"), 0, sound, flash, isFlashEnabled = false, isIntervalSoundEnabled = false, intervalSeconds = 0, onSetComplete = {}, onAbort = {}, onRetry = {})
    }
}
