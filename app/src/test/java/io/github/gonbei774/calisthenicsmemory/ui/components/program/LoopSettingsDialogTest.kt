package io.github.gonbei774.calisthenicsmemory.ui.components.program

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The loop dialog's numbers are steppers: tapping "One more" changes the value that is saved. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class LoopSettingsDialogTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun `a new loop starts at 3 rounds and 60 s, and the steppers change them`() {
        var saved: Pair<Int, Int>? = null
        compose.setContent {
            CalisthenicsMemoryTheme {
                LoopSettingsDialog(loop = null, onDismiss = {}, onSave = { rounds, rest -> saved = rounds to rest })
            }
        }
        // Rounds first, rest second.
        compose.onAllNodesWithContentDescription("One more")[0].performClick()
        compose.onAllNodesWithContentDescription("One more")[1].performClick()
        compose.onNodeWithText("Save").performClick()
        assertEquals(4 to 65, saved)
    }
}
