package io.github.gonbei774.calisthenicsmemory.ui.screens.review

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.gonbei774.calisthenicsmemory.ui.components.SharedComponentScreenshotTest.ScreenshotTheme
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden images of the weekly review in each app theme, with a made-up week. Update with
 * `./gradlew recordRoborazziDebug` after a deliberate change.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class WeeklyReviewScreenshotTest(private val theme: ScreenshotTheme) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun weeklyReview() {
        compose.setContent {
            CalisthenicsMemoryTheme(darkTheme = theme.dark, dynamicColor = false, trueBlack = theme.trueBlack) {
                WeeklyReviewContent(
                    WeeklyReviewSamples.week, "This week", WeeklyReviewSamples.RANGE, goal = 4,
                    canGoBack = true, canGoForward = false, onPrevious = {}, onNext = {},
                )
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/WeeklyReview_${theme.label}.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes() = ScreenshotTheme.entries.map { arrayOf<Any>(it) }
    }
}
