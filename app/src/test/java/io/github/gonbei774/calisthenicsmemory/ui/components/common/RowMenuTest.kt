package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A row's actions live behind one labelled ⋮ button. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RowMenuTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun `the menu opens from its button and runs the chosen action`() {
        val done = mutableListOf<String>()
        compose.setContent {
            CalisthenicsMemoryTheme {
                RowMenu(
                    listOf(
                        RowAction("Duplicate", AppIcons.Add, { done += "duplicate" }),
                        RowAction("Delete", AppIcons.Delete, { done += "delete" }, destructive = true),
                    )
                )
            }
        }
        compose.onNodeWithText("Duplicate").assertDoesNotExist()
        compose.onNodeWithContentDescription("Menu").performClick()
        compose.onNodeWithText("Duplicate").assertExists()
        compose.onNodeWithText("Delete").performClick()
        assertEquals(listOf("delete"), done)
        compose.onNodeWithText("Delete").assertDoesNotExist()
    }
}
