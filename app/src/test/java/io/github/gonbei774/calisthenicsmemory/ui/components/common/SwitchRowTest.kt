package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.gonbei774.calisthenicsmemory.ui.screens.textFitProblems
import io.github.gonbei774.calisthenicsmemory.ui.screens.tapTargetProblems
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The shared setting rows: one named control, toggled from anywhere on the row, fitting at 200 % text. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class SwitchRowTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun `tapping the text toggles, and the row is one named switch`() {
        compose.setContent {
            CalisthenicsMemoryTheme {
                var on by remember { mutableStateOf(false) }
                SwitchRow(title = "Count sound", description = "Assists counting", checked = on, onCheckedChange = { on = it })
            }
        }
        // The title and description are merged into the one toggleable control.
        compose.onNodeWithText("Count sound").assertIsToggleable().assertIsOff()
        compose.onNodeWithText("Count sound").performClick()
        compose.onNodeWithText("Count sound").assertIsOn()
    }

    @Test fun `the stepper shows under the setting only while it is on`() {
        compose.setContent {
            CalisthenicsMemoryTheme {
                var on by remember { mutableStateOf(false) }
                var seconds by remember { mutableIntStateOf(10) }
                SwitchValueRow(
                    title = "Sound during holds", checked = on, onCheckedChange = { on = it },
                    value = seconds, onValueChange = { seconds = it }, unit = "s",
                )
            }
        }
        compose.onNodeWithText("10").assertDoesNotExist()
        compose.onNodeWithText("Sound during holds").performClick()
        compose.onNodeWithText("10").assertExists()
    }

    @Test fun `a long title and description fit at 200 percent text with the switch on the card`() {
        RuntimeEnvironment.setFontScale(2f)
        try {
            compose.setContent {
                CalisthenicsMemoryTheme {
                    SwitchRow(
                        title = "Interval notification", description = "A short sound every few seconds while you hold",
                        checked = true, onCheckedChange = {},
                    )
                }
            }
            val problems = textFitProblems(compose) + tapTargetProblems(compose, compose.density)
            assertTrue(problems.joinToString("\n"), problems.isEmpty())
            assertEquals(1, compose.onAllNodes(androidx.compose.ui.test.isToggleable()).fetchSemanticsNodes().size)
        } finally {
            RuntimeEnvironment.setFontScale(1f)
        }
    }
}
