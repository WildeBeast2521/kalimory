package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What an exercise row says besides its name, from one place. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExerciseBadgesTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(exercise: Exercise, showGroup: Boolean = false) =
        compose.setContent { CalisthenicsMemoryTheme { Row { ExerciseBadges(exercise, showGroup) } } }

    private val base = Exercise(id = 1, name = "Push-up", type = "Dynamic", group = "Push")

    @Test fun `a plain rep exercise says Reps and nothing else`() {
        show(base)
        compose.onNodeWithText("Reps").assertExists()
        compose.onNodeWithText("Time").assertDoesNotExist()
        compose.onNodeWithText("One-sided").assertDoesNotExist()
        compose.onNodeWithText("Push").assertDoesNotExist()
    }

    @Test fun `a hold says Time, a one-sided exercise says so`() {
        show(base.copy(type = "Isometric", laterality = "Unilateral"))
        compose.onNodeWithText("Time").assertExists()
        compose.onNodeWithText("One-sided").assertExists()
    }

    @Test fun `the difficulty shows only for an exercise with a target`() {
        show(base.copy(sortOrder = 4))
        compose.onNodeWithText("Level 4").assertDoesNotExist()
    }

    @Test fun `with a target the difficulty shows`() {
        show(base.copy(sortOrder = 4, targetSets = 3, targetValue = 10))
        compose.onNodeWithText("Level 4").assertExists()
    }

    @Test fun `the group shows only where asked`() {
        show(base, showGroup = true)
        compose.onNodeWithText("Push").assertExists()
    }
}
