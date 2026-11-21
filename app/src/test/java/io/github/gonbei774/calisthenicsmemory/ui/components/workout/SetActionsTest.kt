package io.github.gonbei774.calisthenicsmemory.ui.components.workout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
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

    @Test fun `the buttons never touch, side by side or when the row wraps, nor the main button above`() {
        compose.setContent {
            CalisthenicsMemoryTheme {
                // 200 dp is narrower than the three buttons side by side, so the row has to wrap.
                Box(Modifier.width(200.dp).testTag("box")) { SetActions(onRestart = {}, onSkipSet = {}, onSkipExercise = {}) }
            }
        }
        val gap = with(compose.density) { 8.dp.toPx() }
        val box = compose.onNodeWithTag("box").fetchSemanticsNode().boundsInRoot
        val bounds = listOf("Restart set", "Skip set", "Skip exercise").map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot }
        // Room above the first button, for the main button it sits under.
        assertTrue("no gap above the buttons", bounds.minOf { it.top } - box.top >= gap - 1)
        for (a in bounds) for (b in bounds) {
            if (a === b) continue
            val sideBySide = a.top < b.bottom && b.top < a.bottom
            if (sideBySide) assertTrue("buttons touch: $a $b", a.right <= b.left + 0.5f || b.right <= a.left + 0.5f)
            if (sideBySide && a.right <= b.left + 0.5f) assertTrue("no gap between buttons", b.left - a.right >= gap - 1)
            if (!sideBySide && a.bottom <= b.top) assertTrue("rows touch: $a $b", b.top - a.bottom >= gap - 1)
        }
    }
}
