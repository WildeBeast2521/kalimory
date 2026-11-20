package io.github.gonbei774.calisthenicsmemory.ui.components.program

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A program entry may have no target: the dialog saves it as 0 and Save stays enabled. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ExerciseSettingsDialogTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun `an empty target saves as no target`() {
        var saved: ProgramExercise? = null
        compose.setContent {
            CalisthenicsMemoryTheme {
                ExerciseSettingsDialog(
                    programExercise = ProgramExercise(id = 1, programId = 1, exerciseId = 1, sortOrder = 0, sets = 3, targetValue = 0),
                    exercise = Exercise(id = 1, name = "Push-up", type = "Dynamic"),
                    availableLoops = emptyList(),
                    onDismiss = {},
                    onSave = { saved = it },
                )
            }
        }
        compose.onNodeWithText("Save").assertIsEnabled()
        compose.onNodeWithText("Save").performClick()
        assertNotNull(saved)
        assertEquals(0, saved!!.targetValue)
        assertEquals(3, saved!!.sets)
    }
}
