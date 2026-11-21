package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
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

/** The Add button of the new-exercise form is never greyed out: with no name it says what is missing. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class AddExerciseFormTest {
    private val compose = createComposeRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(MainScreenFixture.state(AppTheme.LIGHT)).around(compose)

    private var dismissed = 0

    private fun open() {
        val vm = TrainingViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {
            CalisthenicsMemoryTheme { UnifiedAddDialog(exercise = null, viewModel = vm, onDismiss = { dismissed++ }) }
        }
    }

    @Test fun `Add is enabled with an empty name and the message waits for a tap`() {
        open()
        compose.onNodeWithText("Add").assertIsEnabled()
        compose.onNodeWithText("Enter a name").assertDoesNotExist()
    }

    @Test fun `tapping Add with no name marks the field and saves nothing`() {
        open()
        compose.onNodeWithText("Add").performClick()
        compose.onNodeWithText("Enter a name").assertExists()
        assertEquals(0, dismissed)
    }

    @Test fun `difficulty starts as not set, with a dash and no invented number`() {
        open()
        compose.onNodeWithText("Difficulty").assertExists()
        compose.onNodeWithText("–").assertExists()
    }
}
