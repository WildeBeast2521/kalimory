package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.MainActivity
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import io.github.gonbei774.calisthenicsmemory.ui.screens.MainScreenFixture.STARTUP_TIMEOUT_MS
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A record whose date is not in the app's form, as an old CSV import or a restored backup can hold,
 * does not crash Progress: its calendar, list and graph still open.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class ProgressMalformedDateTest {
    private val compose = createAndroidComposeRule<MainActivity>()

    // Added after the fixture's history, before the app starts.
    private val malformedRecord = object : ExternalResource() {
        override fun before() = runBlocking {
            val database = AppDatabase.getDatabase(ApplicationProvider.getApplicationContext())
            val row = database.exerciseDao().getExerciseByNameAndType("Australian row", "Dynamic")!!
            database.trainingRecordDao().insertRecord(
                TrainingRecord(exerciseId = row.id, valueRight = 9, setNumber = 1, date = "2026/3/9", time = "7:30")
            )
            Unit
        }
    }

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(MainScreenFixture.state(AppTheme.LIGHT)).around(malformedRecord).around(compose)

    @Test fun progressOpensEveryViewDespiteAMalformedDate() {
        val progress = hasText("Progress") and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))
        compose.waitUntil(STARTUP_TIMEOUT_MS) { compose.onAllNodes(progress).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(progress).performClick()
        for (tab in listOf("Calendar", "List", "Graph")) tap(tab)
        // The graph draws once an exercise is chosen: the one with the malformed record, over all time.
        compose.onAllNodes(hasContentDescription("Select exercise")).onFirst().performClick()
        compose.waitForIdle()
        // Exercises sit in their group, which may need opening first.
        if (compose.onAllNodes(hasText("Australian row")).fetchSemanticsNodes().isEmpty()) tap("Pull")
        tap("Australian row")
        compose.waitForIdle()
        // The graph is drawn, not still asking for an exercise.
        assertTrue(compose.onAllNodes(hasText("Please select an exercise")).fetchSemanticsNodes().isEmpty())
    }

    private fun tap(text: String) {
        compose.waitUntil(STARTUP_TIMEOUT_MS) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText(text)).onFirst().performClick()
        compose.waitForIdle()
    }
}
