package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onAllNodesWithContentDescription
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

    private fun open(exercise: io.github.gonbei774.calisthenicsmemory.data.Exercise? = null) {
        val vm = TrainingViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {
            CalisthenicsMemoryTheme { UnifiedAddDialog(exercise = exercise, viewModel = vm, onDismiss = { dismissed++ }) }
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
        // Difficulty, seconds per rep and rest all start blank.
        compose.onAllNodesWithText("–").assertCountEquals(3)
    }

    @Test fun `an exercise from the catalogue says so and names its muscles`() {
        val step = io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue.chains.first().steps.first()
        open(io.github.gonbei774.calisthenicsmemory.data.Exercise(id = 5, name = "Wall push-up", type = "Dynamic", catalogId = step.id))
        compose.onNode(androidx.compose.ui.test.hasText("From the catalogue:", substring = true)).assertExists()
    }

    @Test fun `an exercise of your own does not mention the catalogue`() {
        open(io.github.gonbei774.calisthenicsmemory.data.Exercise(id = 6, name = "My move", type = "Dynamic"))
        compose.onNode(androidx.compose.ui.test.hasText("From the catalogue:", substring = true)).assertDoesNotExist()
    }

    @Test fun `the rest field steps from blank to zero, then by five, and back to blank`() {
        open()
        val more = "One more"
        val fewer = "One fewer"
        // The rest stepper is the last "One more" on the form.
        fun restMore() = compose.onAllNodesWithContentDescription(more).onLast()
        fun restFewer() = compose.onAllNodesWithContentDescription(fewer).onLast()
        restMore().performScrollTo().performClick()
        compose.onNodeWithText("0").assertExists()
        restMore().performClick()
        compose.onNodeWithText("5").assertExists()
        restFewer().performClick()
        restFewer().performClick()
        compose.onNodeWithText("0").assertDoesNotExist()
    }
}
