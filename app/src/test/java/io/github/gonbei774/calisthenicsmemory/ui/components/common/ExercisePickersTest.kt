package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.gonbei774.calisthenicsmemory.data.Exercise
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

/** The two rows every exercise list is built from: choose one, or tick several. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class ExercisePickersTest {
    @get:Rule
    val compose = createComposeRule()

    private val pullUp = Exercise(
        id = 7, name = "Weighted pull-up", type = "Dynamic", group = "Pull", weightTrackingEnabled = true,
        targetSets = 3, targetValue = 8, sortOrder = 5,
    )

    @Test fun `tapping a pick row picks it`() {
        var picked = 0
        compose.setContent { CalisthenicsMemoryTheme { ExercisePickRow(pullUp, onClick = { picked++ }) } }
        compose.onNodeWithText("Weighted pull-up").performClick()
        assertEquals(1, picked)
    }

    @Test fun `a pick row leaves the group out under its own heading`() {
        compose.setContent { CalisthenicsMemoryTheme { ExercisePickRow(pullUp, onClick = {}) } }
        compose.onNodeWithText("Pull").assertDoesNotExist()
    }

    @Test fun `a pick row names its group in search results`() {
        compose.setContent { CalisthenicsMemoryTheme { ExercisePickRow(pullUp, onClick = {}, showGroup = true) } }
        compose.onNodeWithText("Pull").assertExists()
    }

    @Test fun `the whole check row toggles`() {
        compose.setContent {
            CalisthenicsMemoryTheme {
                var on by remember { mutableStateOf(false) }
                ExerciseCheckRow(pullUp, isSelected = on, onToggle = { on = !on })
            }
        }
        compose.onNodeWithText("Weighted pull-up").assertIsOff()
        compose.onNodeWithText("Weighted pull-up").performClick()
        compose.onNodeWithText("Weighted pull-up").assertIsOn()
    }

    @Test fun `both rows fit and are reachable at 200 percent text`() {
        RuntimeEnvironment.setFontScale(2f)
        try {
            compose.setContent {
                CalisthenicsMemoryTheme {
                    androidx.compose.foundation.layout.Column {
                        ExercisePickRow(pullUp, onClick = {}, showGroup = true)
                        ExerciseCheckRow(pullUp, isSelected = true, onToggle = {}, showGroup = true)
                    }
                }
            }
            val problems = textFitProblems(compose) + tapTargetProblems(compose, compose.density)
            assertTrue(problems.joinToString("\n"), problems.isEmpty())
        } finally {
            RuntimeEnvironment.setFontScale(1f)
        }
    }
}
