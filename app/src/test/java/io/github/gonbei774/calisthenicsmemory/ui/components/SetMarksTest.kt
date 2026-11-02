package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SetMarks
import io.github.gonbei774.calisthenicsmemory.ui.screens.tapTargetProblems
import io.github.gonbei774.calisthenicsmemory.ui.screens.textFitProblems
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

/** The shared warm-up control (ADR 0012): never preset, toggles, reads as selected, full target. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class SetMarksTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun `the warm-up chip toggles and reads as selected`() {
        val changes = mutableListOf<Boolean>()
        compose.setContent {
            CalisthenicsMemoryTheme {
                var warmUp by remember { mutableStateOf(false) }
                SetMarks(warmUp, onWarmUp = { warmUp = it; changes += it })
            }
        }
        compose.onNodeWithText("Warm-up").assertIsNotSelected().performClick()
        compose.onNodeWithText("Warm-up").assertIsSelected().performClick()
        assertEquals(listOf(true, false), changes)
    }

    @Test fun `at 200 percent font the chip is a full target and its label fits`() {
        RuntimeEnvironment.setFontScale(2f)
        try {
            compose.setContent { CalisthenicsMemoryTheme { SetMarks(true, onWarmUp = {}) } }
            compose.waitForIdle()
            val problems = tapTargetProblems(compose, compose.density) + textFitProblems(compose)
            assertTrue(problems.joinToString("\n"), problems.isEmpty())
        } finally {
            RuntimeEnvironment.setFontScale(1f)
        }
    }
}
