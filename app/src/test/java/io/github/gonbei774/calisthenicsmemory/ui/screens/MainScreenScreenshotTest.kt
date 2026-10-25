package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.gonbei774.calisthenicsmemory.MainActivity
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.OnboardingPreferences
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ThemePreferences
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import io.github.gonbei774.calisthenicsmemory.util.AppClock
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.TimeZone

/**
 * Golden images of the four main screens (Today, Train, Progress, Library) in each app theme, from
 * the real app with a small made-up training history. The date is fixed (AppClock), so weeks,
 * calendars and charts look the same on every day. Update with `./gradlew recordRoborazziDebug`
 * after a deliberate change, as for the shared components.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class MainScreenScreenshotTest(private val theme: AppTheme) {

    private val appState = MainScreenFixture.state(theme)

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(appState).around(compose)

    @Test
    fun today() = snap("Today", waitFor = PROGRAM, PROGRAM_ESTIMATE)

    @Test
    fun train() {
        open("Train")
        snap("Train", waitFor = PROGRAM, PROGRAM_ESTIMATE)
    }

    @Test
    fun progress() {
        open("Progress")
        // The selected week lists Monday's exercises.
        snap("Progress", waitFor = "Australian row")
    }

    @Test
    fun library() {
        open("Library")
        // The exercise count, once the database has loaded.
        snap("Library", waitFor = "4")
    }

    private fun open(tab: String) {
        // The app checks its database on a background thread before the navigation bar appears.
        val item = hasText(tab) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))
        compose.waitUntil(STARTUP_TIMEOUT_MS) { compose.onAllNodes(item).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(item).performClick()
    }

    private fun snap(name: String, waitFor: String, vararg alsoWaitFor: String) {
        // Room loads on its own threads; wait until the seeded data is on screen. A program's time
        // estimate settles after its title, so screens that show one wait for it too.
        compose.waitUntil(STARTUP_TIMEOUT_MS) { compose.onAllNodes(hasText(waitFor)).fetchSemanticsNodes().isNotEmpty() }
        for (text in alsoWaitFor) {
            compose.waitUntil(STARTUP_TIMEOUT_MS) { compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/screen_${name}_${theme.code}.png")
    }

    companion object {
        private const val PROGRAM = MainScreenFixture.PROGRAM
        private const val PROGRAM_ESTIMATE = MainScreenFixture.PROGRAM_ESTIMATE
        private const val STARTUP_TIMEOUT_MS = MainScreenFixture.STARTUP_TIMEOUT_MS

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes() = listOf(AppTheme.LIGHT, AppTheme.DARK, AppTheme.AMOLED).map { arrayOf<Any>(it) }
    }
}
