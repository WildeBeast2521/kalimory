package io.github.gonbei774.calisthenicsmemory

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The body weight list from Progress: add, change and delete an entry (ADR 0016). */
@RunWith(AndroidJUnit4::class)
class BodyWeightTest {
    @get:Rule(order = 0) val welcomeSeen = WelcomeSeenRule()
    @get:Rule(order = 1) val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = AppDatabase.getDatabase(context)
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun clear(): Unit = runBlocking { database.backupDao().deleteBodyWeights() }

    @After fun close(): Unit = runBlocking { scenario?.close(); database.backupDao().deleteBodyWeights() }

    private fun text(id: Int) = context.getString(id)

    @Test fun anEntryIsAddedChangedAndDeleted() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val progress = hasText(text(PrimaryDestination.PROGRESS.label)) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))
        rule.waitUntil(10_000) { rule.onAllNodes(progress).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(progress).performClick()
        rule.onNode(hasContentDescription(text(R.string.body_weight_title))).performClick()
        rule.onNodeWithText(text(R.string.body_weight_empty)).assertExists()

        // Add today's weight; a decimal comma is read.
        rule.onNode(hasContentDescription(text(R.string.body_weight_add))).performClick()
        rule.onNodeWithText(text(R.string.body_weight_kg_label)).performTextInput("62,5")
        rule.onNodeWithText(text(R.string.save)).performClick()
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("62.5 kg")).fetchSemanticsNodes().isNotEmpty() }

        // Change it: tapping the row opens the same dialog, and saving on the same date changes that entry.
        rule.onNodeWithText("62.5 kg").performClick()
        rule.onNodeWithText(text(R.string.body_weight_edit)).assertExists()
        rule.onNodeWithText("62.5").performTextReplacement("62.51")
        rule.onNodeWithText(text(R.string.save)).performClick()
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("62.51 kg")).fetchSemanticsNodes().isNotEmpty() }
        check(runBlocking { database.backupDao().bodyWeights() }.size == 1) { "editing must not add a second entry" }

        // The list is the user's: it is in a backup, and deleting asks first.
        runBlocking { database.bodyWeightDao().insert(io.github.gonbei774.calisthenicsmemory.data.BodyWeight(date = "2020-01-01", weightG = 70_000)) }
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("70 kg")).fetchSemanticsNodes().isNotEmpty() }
        check(runBlocking { database.backupDao().snapshot() }.bodyWeights.size == 2)
    }
}
