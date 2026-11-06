package io.github.gonbei774.calisthenicsmemory.ui.screens.catalogue

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.gonbei774.calisthenicsmemory.ui.components.SharedComponentScreenshotTest.ScreenshotTheme
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Golden images of the climb chart in each app theme, with a made-up climb. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class ClimbChartScreenshotTest(private val theme: ScreenshotTheme) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun climbChart() {
        compose.setContent {
            CalisthenicsMemoryTheme(darkTheme = theme.dark, dynamicColor = false, trueBlack = theme.trueBlack) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.padding(16.dp)) { ClimbChart(ClimbChartSamples.climb) }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/ClimbChart_${theme.label}.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes() = ScreenshotTheme.entries.map { arrayOf<Any>(it) }
    }
}
