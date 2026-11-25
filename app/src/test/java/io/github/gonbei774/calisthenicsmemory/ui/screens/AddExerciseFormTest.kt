package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onAllNodesWithContentDescription
import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
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

/** The Add button of the new-exercise form is never greyed out: with no name it says what is missing. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class AddExerciseFormTest {
    private val compose = createComposeRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(MainScreenFixture.state(AppTheme.LIGHT)).around(compose)

    private var dismissed = 0

    private fun open(exercise: io.github.gonbei774.calisthenicsmemory.data.Exercise? = null) {
        val vm = TrainingViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {
            CalisthenicsMemoryTheme { UnifiedAddDialog(exercise = exercise, viewModel = vm, onDismiss = { dismissed++ }) }
        }
    }

    @Test fun `Add is enabled with an empty name and the message waits for a tap`() {
        open()
        compose.onNodeWithText("Add").assertIsEnabled()
        compose.onNodeWithText("Enter a name").assertDoesNotExist()
    }

    @Test fun `tapping Add with no name marks the field and saves nothing`() {
        open()
        compose.onNodeWithText("Add").performClick()
        compose.onNodeWithText("Enter a name").assertExists()
        assertEquals(0, dismissed)
    }

    @Test fun `difficulty starts as not set, with a dash and no invented number`() {
        open()
        compose.onNodeWithText("Difficulty").assertExists()
        // Difficulty, seconds per rep and rest all start blank.
        compose.onAllNodesWithText("–").assertCountEquals(3)
    }

    @Test fun `an exercise from the catalogue says so and names its muscles`() {
        val step = io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue.chains.first().steps.first()
        open(io.github.gonbei774.calisthenicsmemory.data.Exercise(id = 5, name = "Wall push-up", type = "Dynamic", catalogId = step.id))
        compose.onNode(androidx.compose.ui.test.hasText("From the catalogue:", substring = true)).assertExists()
    }

    @Test fun `an exercise of your own does not mention the catalogue`() {
        open(io.github.gonbei774.calisthenicsmemory.data.Exercise(id = 6, name = "My move", type = "Dynamic"))
        compose.onNode(androidx.compose.ui.test.hasText("From the catalogue:", substring = true)).assertDoesNotExist()
    }

    @Test fun `the rest field steps from blank to zero, then by five, and back to blank`() {
        open()
        val more = "One more"
        val fewer = "One fewer"
        // The rest stepper is the last "One more" on the form.
        fun restMore() = compose.onAllNodesWithContentDescription(more).onLast()
        fun restFewer() = compose.onAllNodesWithContentDescription(fewer).onLast()
        restMore().performScrollTo().performClick()
        compose.onNodeWithText("0").assertExists()
        restMore().performClick()
        compose.onNodeWithText("5").assertExists()
        restFewer().performClick()
        restFewer().performClick()
        compose.onNodeWithText("0").assertDoesNotExist()
    }

    // Muscles for an exercise of your own (ADR 0014).
    private val own = io.github.gonbei774.calisthenicsmemory.data.Exercise(id = 6, name = "My move", type = "Dynamic")

    @Test fun `an exercise of your own offers the muscles in two rows`() {
        open(own)
        compose.onNodeWithText("Muscles (optional)").performScrollTo().assertExists()
        compose.onNodeWithText("Works").assertExists()
        compose.onNodeWithText("Also").assertExists()
        compose.onAllNodesWithText("Biceps").assertCountEquals(2)
    }

    @Test fun `a new exercise offers the muscles too`() {
        open()
        compose.onNodeWithText("Muscles (optional)").assertExists()
    }

    @Test fun `an exercise from the catalogue offers no muscle choice, its step's are fixed`() {
        val step = io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue.chains.first().steps.first()
        open(own.copy(id = 7, catalogId = step.id))
        compose.onNodeWithText("Muscles (optional)").assertDoesNotExist()
    }

    @Test fun `a muscle is in one row at a time, and a second tap clears it`() {
        open(own)
        fun chip(row: Int) = compose.onAllNodesWithText("Biceps")[row]
        chip(0).performScrollTo().performClick()
        chip(0).assertIsSelected(); chip(1).assertIsNotSelected()
        chip(1).performScrollTo().performClick()
        chip(1).assertIsSelected(); chip(0).assertIsNotSelected()
        chip(1).performClick()
        chip(0).assertIsNotSelected(); chip(1).assertIsNotSelected()
    }

    @Test fun `the muscles chosen are saved with the exercise`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = io.github.gonbei774.calisthenicsmemory.data.AppDatabase.getDatabase(app)
        val id = kotlinx.coroutines.runBlocking { db.exerciseDao().insertExercise(own.copy(id = 0, name = "Chosen curl")) }
        open(db.let { kotlinx.coroutines.runBlocking { it.exerciseDao().getExerciseById(id)!! } })
        compose.onAllNodesWithText("Biceps")[0].performScrollTo().performClick()
        compose.onAllNodesWithText("Forearms")[1].performScrollTo().performClick()
        compose.onNode(androidx.compose.ui.test.hasText("Save") and androidx.compose.ui.test.hasClickAction()).performClick()
        val deadline = System.currentTimeMillis() + 5_000
        var saved = kotlinx.coroutines.runBlocking { db.exerciseDao().getExerciseById(id)!! }
        while (saved.primaryMuscles.isEmpty() && System.currentTimeMillis() < deadline) {
            compose.waitForIdle(); Thread.sleep(100)
            saved = kotlinx.coroutines.runBlocking { db.exerciseDao().getExerciseById(id)!! }
        }
        assertEquals(setOf(io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle.BICEPS), saved.primaryMuscles)
        assertEquals(setOf(io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle.FOREARMS), saved.secondaryMuscles)
    }

    @Test fun `the form with the muscle chips meets the touch rules`() {
        open(own)
        compose.onNodeWithText("Muscles (optional)").performScrollTo()
        val problems = tapTargetProblems(compose, compose.density)
        assertEquals(problems.joinToString("\n"), emptyList<String>(), problems)
    }

    @Test fun `the group dropdown is named for screen readers and reads its value`() {
        open(own)
        compose.onNode(androidx.compose.ui.test.hasContentDescription("Group (optional)") and androidx.compose.ui.test.hasClickAction())
            .performScrollTo().assertExists()
        compose.onNode(androidx.compose.ui.test.hasStateDescription("No group")).assertExists()
    }
}
