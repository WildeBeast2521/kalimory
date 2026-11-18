package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The workout and program setup screens, Settings and the Programs and To Do lists at 200 % text on a 360 dp phone: no text cut off. These rows used
 * fixed widths and heights that only fit at 100 %, so switches left the card, buttons lost their text and
 * numbers were clipped. (Switch names and touch targets are checked in SwitchRowTest.)
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class SetupScreensLargeTextTest {
    private val compose = createComposeRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(MainScreenFixture.state(AppTheme.LIGHT)).around(compose)

    private fun audit(ignore: String? = null, content: @Composable (TrainingViewModel) -> Unit) {
        RuntimeEnvironment.setFontScale(2f)
        try {
            val vm = TrainingViewModel(ApplicationProvider.getApplicationContext<Application>())
            compose.setContent { CalisthenicsMemoryTheme { content(vm) } }
            repeat(4) { compose.waitForIdle(); Thread.sleep(300) }
            val problems = textFitProblems(compose).filterNot { ignore != null && it.startsWith(ignore) }
            assertTrue(problems.joinToString("\n"), problems.isEmpty())
        } finally {
            RuntimeEnvironment.setFontScale(1f)
        }
    }

    @Test fun `workout setup for a rep exercise`() = audit { WorkoutScreen(it, {}, initialExerciseId = 1L) }

    @Test fun `workout setup for a hold`() = audit { WorkoutScreen(it, {}, initialExerciseId = 4L) }

    @Test fun `program setup`() = audit { ProgramExecutionScreen(it, 1L, onNavigateBack = {}, onComplete = {}) }

    // The list rows: a full-size Start button used to leave the name a few letters wide.
    @Test fun `programs list`() = audit { ProgramListScreen(it, {}, {}, {}) }

    @Test fun `to do list`() = audit { ToDoScreen(it, {}, {}, {}, {}, {}) }

    // The top bar's collapsed title ("New interval") is shortened beside the Save button at this size: that
    // is the top bar's own problem (one shared bar, with room for two lines, is planned), not the rows.
    @Test fun `new interval`() = audit(ignore = "\"New interval\"") { IntervalEditScreen(it, null, {}, {}) }

    @Test fun `log a past workout entry`() = audit { RecordScreen(it, {}, initialExerciseId = 1L) }

    @Test fun `settings`() = audit { SettingsScreenNew(it, {}, {}, {}, {}, {}, {}) }

    // Same top-bar limit as above: the collapsed title is shortened beside the back button at this size.
    @Test fun `backup`() = audit(ignore = "\"Complete backup (JSON)\"") { BackupScreen(it, {}) }

    @Test fun `csv files`() = audit { CsvDataManagementScreen(it, {}) }

    @Test fun `share`() = audit { ShareHubScreen(it, {}, {}) }
}
