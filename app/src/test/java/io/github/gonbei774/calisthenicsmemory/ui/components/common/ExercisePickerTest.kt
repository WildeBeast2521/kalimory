package io.github.gonbei774.calisthenicsmemory.ui.components.common

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.ui.screens.MainScreenFixture
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The one picker for choosing exercises to add: search, tick, Add with the count. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class ExercisePickerTest {
    private val compose = createComposeRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(MainScreenFixture.state(AppTheme.LIGHT)).around(compose)

    private val push = Exercise(id = 1, name = "Push-up", type = "Dynamic", group = "Push")
    private val plank = Exercise(id = 2, name = "Plank", type = "Isometric")

    private fun open(onAdd: (List<Exercise>) -> Unit) {
        val vm = TrainingViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {
            CalisthenicsMemoryTheme { ExercisePicker(vm, listOf(push, plank), "Add exercise", onDismiss = {}, onAdd = onAdd) }
        }
    }

    private fun search(text: String) = compose.onNode(androidx.compose.ui.test.hasSetTextAction()).performTextReplacement(text)

    @Test fun `Add is off until something is ticked and then says how many`() {
        open {}
        compose.onNodeWithText("Add exercise").assertExists()
        compose.onNodeWithText("Add").assertExists()
        search("p")
        compose.onNodeWithText("Push-up").performClick()
        compose.onNodeWithText("Plank").performClick()
        compose.onNodeWithText("Add (2)").assertExists()
    }

    @Test fun `ticked exercises come back in the order they were ticked, across searches`() {
        var added: List<Exercise> = emptyList()
        open { added = it }
        search("plank")
        compose.onNodeWithText("Plank").performClick()
        search("push")
        compose.onNodeWithText("Push-up").performClick()
        compose.onNodeWithText("Add (2)").performClick()
        assertEquals(listOf("Plank", "Push-up"), added.map { it.name })
    }

    @Test fun `a search narrows the list`() {
        open {}
        search("plan")
        compose.onNodeWithText("Push-up").assertDoesNotExist()
        compose.onNodeWithText("Plank").assertExists()
    }
}
