package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.gonbei774.calisthenicsmemory.ui.components.common.AppFab
import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmTopBar
import io.github.gonbei774.calisthenicsmemory.ui.components.common.NumberStepButton
import io.github.gonbei774.calisthenicsmemory.ui.components.common.NumberStepper
import io.github.gonbei774.calisthenicsmemory.ui.components.common.StartButton
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SwipeToDeleteBackground
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.RollingNumber
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.TimerDial
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.WorkoutPrimaryButton
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.HeroCard
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden images of the shared components (AGENTS.md, "One behaviour everywhere") in each app
 * theme. A component that drifts fails testDebugUnitTest; after a deliberate visual change,
 * `./gradlew recordRoborazziDebug` updates the images, which go into the same pull request.
 *
 * Dynamic colour is off: wallpaper colours differ between devices and are not the app's design.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class SharedComponentScreenshotTest(private val theme: ScreenshotTheme) {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun appFab() = snap("AppFab") {
        AppFab(onClick = {}, contentDescription = "Add")
    }

    @Test
    fun startButton() = snap("StartButton") {
        StartButton(onClick = {})
    }

    @Test
    fun numberStepper() = snap("NumberStepper") {
        NumberStepper(value = "12", onValueChange = {}, unit = "reps", modifier = Modifier.width(280.dp))
    }

    @Test
    fun numberStepButtons() = snap("NumberStepButton") {
        Row {
            NumberStepButton(increment = false, contentDescription = null, enabled = false, onStep = { false })
            NumberStepButton(increment = false, contentDescription = null, onStep = { false })
            NumberStepButton(increment = true, contentDescription = null, onStep = { false })
        }
    }

    @Test
    fun workoutPrimaryButton() = snap("WorkoutPrimaryButton") {
        Column {
            WorkoutPrimaryButton(text = "Done", onClick = {}, modifier = Modifier.width(280.dp))
            WorkoutPrimaryButton(text = "Done", onClick = {}, modifier = Modifier.width(280.dp), enabled = false)
        }
    }

    @Test
    fun timerDialRunning() = snap("TimerDial_running") {
        TimerDial(progress = 0.6f, value = "0:45", accent = MaterialTheme.colorScheme.primary, paused = false, onToggle = {})
    }

    @Test
    fun timerDialPaused() = snap("TimerDial_paused") {
        TimerDial(progress = 0.6f, value = "0:45", accent = MaterialTheme.colorScheme.primary, paused = true, onToggle = {})
    }

    @Test
    fun rollingNumber() = snap("RollingNumber") {
        RollingNumber(text = "128", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurface)
    }

    @Test
    fun calmTopBar() = snap("CalmTopBar") {
        CalmTopBar(title = "Push-ups", onBack = {}, scrollBehavior = null)
    }

    @Test
    fun swipeToDeleteBackground() = snap("SwipeToDeleteBackground") {
        // Settled at "dismissed", so the delete background shows across the whole row.
        val state = rememberSwipeToDismissBoxState(initialValue = SwipeToDismissBoxValue.EndToStart)
        SwipeToDismissBox(
            state = state,
            backgroundContent = { SwipeToDeleteBackground(state, MaterialTheme.shapes.large) },
            modifier = Modifier.width(320.dp).height(72.dp),
        ) {
            Box(Modifier.fillMaxWidth())
        }
    }

    @Test
    fun heroCard() = snap("HeroCard") {
        Box(Modifier.width(328.dp)) {
            HeroCard(
                label = "Next up",
                title = "Upper body",
                detail = "6 exercises · about 35 min",
                action = "Start",
                icon = AppIcons.Program,
                onClick = {},
            )
        }
    }

    private fun snap(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            CalisthenicsMemoryTheme(darkTheme = theme.dark, dynamicColor = false, trueBlack = theme.trueBlack) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.padding(16.dp)) { content() }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/${name}_${theme.label}.png")
    }

    enum class ScreenshotTheme(val label: String, val dark: Boolean, val trueBlack: Boolean) {
        LIGHT("light", dark = false, trueBlack = false),
        DARK("dark", dark = true, trueBlack = false),
        AMOLED("amoled", dark = true, trueBlack = true),
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes() = ScreenshotTheme.entries.map { arrayOf<Any>(it) }
    }
}
