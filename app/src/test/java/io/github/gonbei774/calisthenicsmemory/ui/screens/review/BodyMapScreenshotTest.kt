package io.github.gonbei774.calisthenicsmemory.ui.screens.review

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.gonbei774.calisthenicsmemory.ui.components.SharedComponentScreenshotTest.ScreenshotTheme
import io.github.gonbei774.calisthenicsmemory.ui.screens.view.Period
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Golden images of the body map in each app theme, with a made-up week. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Tall enough for every section of the sample week.
@Config(sdk = [35], qualifiers = "w360dp-h1600dp-xhdpi")
class BodyMapScreenshotTest(private val theme: ScreenshotTheme) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun bodyMap() {
        compose.setContent {
            CalisthenicsMemoryTheme(darkTheme = theme.dark, dynamicColor = false, trueBlack = theme.trueBlack) {
                BodyMapContent(BodyMapSamples.week, weeks = 1.0, period = Period.OneWeek, onPeriod = {})
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/BodyMap_${theme.label}.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes() = ScreenshotTheme.entries.map { arrayOf<Any>(it) }
    }
}
