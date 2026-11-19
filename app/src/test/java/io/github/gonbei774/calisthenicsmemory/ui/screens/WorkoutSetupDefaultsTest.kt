package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.app.Application
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** An exercise with no target and no history starts with no invented numbers: Start says what is missing. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class WorkoutSetupDefaultsTest {
    private val compose = createComposeRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(MainScreenFixture.state(AppTheme.LIGHT)).around(compose)

    private var started = 0

    @Test fun `Start with no reps asks for a number and starts nothing`() {
        val vm = TrainingViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {
            CalisthenicsMemoryTheme {
                SettingsStep(exercise = Exercise(id = 99, name = "Hollow hold", type = "Dynamic"), viewModel = vm, onStartWorkout = { started++ })
            }
        }
        compose.onNodeWithText("Start").performScrollTo().performClick()
        compose.onNodeWithText("Enter a number").assertExists()
        assertEquals(0, started)
    }
}
