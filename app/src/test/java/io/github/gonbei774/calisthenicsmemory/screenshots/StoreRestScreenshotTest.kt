package io.github.gonbei774.calisthenicsmemory.screenshots

import android.app.Application
import android.os.Looper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramIntervalStep
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import io.github.gonbei774.calisthenicsmemory.util.SoundPlayer
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

/**
 * The rest between two sets of a program, for the README and store screenshots (#158), drawn as
 * the workout shows it after the first set of push-ups. Skipped unless KALIMORY_SCREENSHOTS is set
 * (see StoreScreenshotsTest).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xxhdpi")
class StoreRestScreenshotTest(private val dark: Boolean) {
    private val out: String? = System.getenv("KALIMORY_SCREENSHOTS")

    @get:Rule
    val compose = createComposeRule()

    @Before fun onlyWhenAsked() = assumeTrue("Set KALIMORY_SCREENSHOTS to make the screenshots", out != null)

    @Test fun rest() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val session = ProgramExecutionSession(
            program = Program(id = 1, name = "Upper body"),
            exercises = listOf(
                ProgramExercise(id = 1, programId = 1, exerciseId = 1, sortOrder = 0, sets = 3, targetValue = 12) to
                    Exercise(id = 1, name = "Push-up", type = "Dynamic", catalogId = "push.full"),
            ),
            sets = MutableList(3) { ProgramWorkoutSet(exerciseIndex = 0, setNumber = it + 1, side = null, targetValue = 12, intervalSeconds = 90, previousValue = 11) }
                .also { it[0].actualValue = 12; it[0].isCompleted = true },
        )
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CalisthenicsMemoryTheme(darkTheme = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ProgramIntervalStep(session, 0, SoundPlayer(context), FlashController(context), isFlashEnabled = false, onComplete = {}, onSkip = {})
                }
            }
        }
        // The stopwatch reads SystemClock, which Robolectric moves with the main looper: 18 s into the rest.
        compose.mainClock.advanceTimeBy(500)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(18_000))
        compose.mainClock.advanceTimeBy(500)
        captureScreenRoboImage(File(out!!, "rest-${if (dark) "dark" else "light"}.png").path)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun themes() = listOf(false, true).map { arrayOf<Any>(it) }
    }
}
