package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalTime

/** A one-sided set in the editor shows a weight, assistance or distance field for each side. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h1200dp-xhdpi")
class WorkoutEditorPerSideTest {
    @get:Rule
    val compose = createComposeRule()

    private fun state(unilateral: Boolean, legacy: Boolean = false) = EditorState(
        date = LocalDate.of(2026, 3, 12), time = LocalTime.of(18, 30), comment = "",
        exercises = listOf(
            EditorExercise(
                key = 1, exerciseId = 1, name = "One-arm pull-up", unilateral = unilateral, dynamic = true,
                showDistance = false, showWeight = false, showAssistance = true,
                sets = listOf(EditorSet(listOf(1, 2), value = "3", valueLeft = if (unilateral) "3" else "", assistanceKg = "5", assistanceLeftKg = "9", perSide = true)),
            ),
        ),
        legacy = legacy,
    )

    private fun show(state: EditorState) = compose.setContent {
        CalisthenicsMemoryTheme { EditorContent(state = state, padding = PaddingValues(), onChange = {}, onAddExercise = {}) }
    }

    @Test fun `a one-sided set shows assistance for the right and for the left side`() {
        show(state(unilateral = true))
        compose.onNodeWithText("Right: Assistance (kg)").assertIsDisplayed()
        compose.onNodeWithText("Left: Assistance (kg)").assertIsDisplayed()
        compose.onNodeWithText("9").assertIsDisplayed()
    }

    @Test fun `a two-sided set and an older-format workout keep a single field`() {
        show(state(unilateral = false))
        compose.onNodeWithText("Assistance (kg)").assertIsDisplayed()
        compose.onNodeWithText("Left: Assistance (kg)").assertDoesNotExist()
    }

    @Test fun `an older-format workout, which stores one value for both sides, shows one field`() {
        show(state(unilateral = true, legacy = true))
        compose.onNodeWithText("Assistance (kg)").assertIsDisplayed()
        compose.onNodeWithText("Left: Assistance (kg)").assertDoesNotExist()
    }
}
