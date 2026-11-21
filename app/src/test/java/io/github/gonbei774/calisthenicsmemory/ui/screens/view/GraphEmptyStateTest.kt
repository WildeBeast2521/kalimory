package io.github.gonbei774.calisthenicsmemory.ui.screens.view

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

/** With no exercises the graph says so and sends you to the Library, not to Settings. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class GraphEmptyStateTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun `the empty graph offers the Library`() {
        var opened = 0
        compose.setContent {
            CalisthenicsMemoryTheme {
                GraphView(exercises = emptyList(), records = emptyList(), selectedExerciseFilter = null, selectedPeriod = null, onOpenLibrary = { opened++ })
            }
        }
        compose.onNodeWithText("Add exercises in Library").assertExists()
        compose.onNodeWithText("Open Library").performClick()
        assertEquals(1, opened)
    }
}
