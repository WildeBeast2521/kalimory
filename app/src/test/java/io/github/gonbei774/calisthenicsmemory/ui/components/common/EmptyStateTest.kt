package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

/** The shared "nothing here yet" message and its optional next step. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class EmptyStateTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun `it shows the message, and a button only when given one`() {
        var taps = 0
        compose.setContent {
            CalisthenicsMemoryTheme {
                EmptyState(text = "No programs yet", actionLabel = "New program", onAction = { taps++ })
            }
        }
        compose.onNodeWithText("No programs yet").assertExists()
        compose.onNodeWithText("New program").performClick()
        assertEquals(1, taps)
    }

    @Test fun `without an action there is no button`() {
        compose.setContent { CalisthenicsMemoryTheme { EmptyState(text = "No programs yet") } }
        compose.onNodeWithText("No programs yet").assertExists()
        assertEquals(0, compose.onAllNodes(androidx.compose.ui.test.hasClickAction()).fetchSemanticsNodes().size)
    }

    @Test fun `a long message fits at 200 percent text`() {
        RuntimeEnvironment.setFontScale(2f)
        try {
            compose.setContent { CalisthenicsMemoryTheme { EmptyState(text = "No exercises yet\nAdd one using the + button below") } }
            val problems = textFitProblems(compose)
            assertTrue(problems.joinToString("\n"), problems.isEmpty())
        } finally {
            RuntimeEnvironment.setFontScale(1f)
        }
    }
}
