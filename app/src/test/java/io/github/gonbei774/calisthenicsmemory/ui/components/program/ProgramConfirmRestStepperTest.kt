package io.github.gonbei774.calisthenicsmemory.ui.components.program

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Before a program starts, an exercise's rest is a stepper under its sets, not a loose text box. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class ProgramConfirmRestStepperTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(rest: Int, onInterval: (Int) -> Unit) {
        val sets = listOf(ProgramWorkoutSet(exerciseIndex = 0, setNumber = 1, side = null, targetValue = 10, intervalSeconds = rest))
        compose.setContent {
            CalisthenicsMemoryTheme {
                ProgramConfirmExerciseCard(
                    exerciseIndex = 0, displayNumber = 1,
                    exercise = Exercise(id = 1, name = "Push-up", type = "Dynamic"),
                    programExercise = ProgramExercise(id = 1, programId = 1, exerciseId = 1, sortOrder = 0, sets = 1, targetValue = 10, intervalSeconds = rest),
                    sets = sets, allSets = sets, isExpanded = true,
                    onToggleExpanded = {}, onUpdateValue = { _, _ -> }, onUpdateInterval = onInterval, onUpdateSetCount = {},
                    onUpdateWeightG = { _, _ -> }, onUpdateDistanceCm = { _, _ -> }, onUpdateAssistanceG = { _, _ -> },
                )
            }
        }
    }

    @Test fun `rest shows as one label and one value, and the buttons step it by 5 seconds`() {
        var rest = 30
        show(rest) { rest = it }
        compose.onNodeWithText("Rest:").assertIsDisplayed()
        compose.onNodeWithText("30 s").assertIsDisplayed()
        // Sets has the first pair of buttons and Rest the second.
        compose.onAllNodesWithContentDescription("One more")[1].performClick()
        assertEquals(35, rest)
        // The number on screen changes at the tap, although the card is given the same sets again.
        compose.onNodeWithText("35 s").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("One fewer")[1].performClick()
        assertEquals(30, rest)
        compose.onNodeWithText("30 s").assertIsDisplayed()
    }

    @Test fun `rest cannot go below zero`() {
        var rest = 3
        show(rest) { rest = it }
        compose.onAllNodesWithContentDescription("One fewer")[1].performClick()
        assertEquals(0, rest)
        compose.onNodeWithText("0 s").assertIsDisplayed()
    }
}
