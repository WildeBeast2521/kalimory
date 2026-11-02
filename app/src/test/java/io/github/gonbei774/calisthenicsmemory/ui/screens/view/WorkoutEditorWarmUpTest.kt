package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalTime

/** When the editor offers the warm-up mark (ADR 0012). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h2000dp-xhdpi")
class WorkoutEditorWarmUpTest {
    @get:Rule
    val compose = createComposeRule()

    private fun state(legacy: Boolean = false) = EditorState(
        date = LocalDate.of(2026, 3, 12), time = LocalTime.of(18, 30), comment = "",
        exercises = listOf(
            EditorExercise(
                key = 1, exerciseId = 1, name = "Push-up", unilateral = false, dynamic = true,
                showDistance = false, showWeight = false, showAssistance = false,
                sets = listOf(EditorSet(listOf(1), value = "8", warmUp = true), EditorSet(listOf(2), value = "12")),
            ),
        ),
        legacy = legacy,
    )

    private fun show(state: EditorState, setting: Boolean) = compose.setContent {
        CalisthenicsMemoryTheme { EditorContent(state, PaddingValues(), onChange = {}, onAddExercise = {}, showWarmUp = setting) }
    }

    @Test fun `with the setting on every set offers the mark`() {
        show(state(), setting = true)
        compose.onAllNodesWithText("Warm-up").assertCountEquals(2)
    }

    @Test fun `with the setting off only a marked set shows it, so the mark can be removed`() {
        show(state(), setting = false)
        compose.onAllNodesWithText("Warm-up").assertCountEquals(1)
    }

    @Test fun `an old-format workout never offers it`() {
        show(state(legacy = true), setting = true)
        compose.onAllNodesWithText("Warm-up").assertCountEquals(0)
    }
}
