package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel.GroupWithExercises
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Progress › Select exercise: a search opens the groups it found and says when nothing matches (#278). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class FilterSearchTest {
    @get:Rule
    val compose = createComposeRule()

    private val row = Exercise(id = 1, name = "Australian row", type = "Dynamic", group = "Pull")
    private val push = Exercise(id = 2, name = "Push-up", type = "Dynamic", group = "Push")

    @Test fun `matches show without opening a group, and an empty search says so`() {
        compose.setContent {
            CalisthenicsMemoryTheme {
                FilterBottomSheetContent(
                    exercises = listOf(row, push),
                    hierarchicalData = listOf(GroupWithExercises("Push", listOf(push)), GroupWithExercises("Pull", listOf(row))),
                    selectedExercise = null,
                    onExerciseSelected = {},
                    onClearFilter = {},
                )
            }
        }
        compose.onNodeWithText("Australian row").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextInput("pull")
        compose.onNodeWithText("Australian row").assertExists()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("zzz")
        compose.onNodeWithText("No results found").assertExists()
    }
}
