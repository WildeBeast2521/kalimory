package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.ui.screens.tapTargetProblems
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config

/** The step buttons TalkBack can name and a finger can hit, even where the circle is drawn small. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class NumberStepButtonTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun `a button with no description reads One fewer and One more`() {
        compose.setContent {
            CalisthenicsMemoryTheme {
                Row {
                    NumberStepButton(increment = false, contentDescription = null) { true }
                    NumberStepButton(increment = true, contentDescription = null) { true }
                }
            }
        }
        compose.onNodeWithContentDescription("One fewer").assertExists()
        compose.onNodeWithContentDescription("One more").assertExists()
    }

    @Test fun `a 32 dp circle still has a 48 dp touch target`() {
        compose.setContent {
            CalisthenicsMemoryTheme {
                Row {
                    NumberStepButton(increment = false, contentDescription = null, size = 32.dp) { true }
                    NumberStepButton(increment = true, contentDescription = null, size = 32.dp) { true }
                }
            }
        }
        val problems = tapTargetProblems(compose, compose.density)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test fun `an empty stepper shows its placeholder, a filled one does not`() {
        compose.setContent {
            CalisthenicsMemoryTheme {
                Row {
                    NumberStepper(value = "", onValueChange = {}, unit = "reps", placeholder = "–")
                }
            }
        }
        compose.onNodeWithText("–").assertExists()
    }

    @Test fun `a filled stepper has no placeholder`() {
        compose.setContent {
            CalisthenicsMemoryTheme {
                Row {
                    NumberStepper(value = "12", onValueChange = {}, unit = "reps", placeholder = "–")
                }
            }
        }
        compose.onNodeWithText("–").assertDoesNotExist()
    }
}
