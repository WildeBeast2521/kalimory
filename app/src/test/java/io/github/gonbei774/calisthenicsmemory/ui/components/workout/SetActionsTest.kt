package io.github.gonbei774.calisthenicsmemory.ui.components.workout

import androidx.compose.ui.test.junit4.createComposeRule
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

/** The quiet actions under a running set's main button. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SetActionsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun `Restart set calls its action`() {
        var restarted = 0
        compose.setContent { CalisthenicsMemoryTheme { SetActions(onRestart = { restarted++ }) } }
        compose.onNodeWithText("Restart set").performClick()
        assertEquals(1, restarted)
    }

    @Test fun `the skip actions show only when given`() {
        compose.setContent { CalisthenicsMemoryTheme { SetActions(onRestart = {}) } }
        compose.onNodeWithText("Skip set").assertDoesNotExist()
        compose.onNodeWithText("Skip exercise").assertDoesNotExist()
    }
}
