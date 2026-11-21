package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A chart is a picture; a screen reader gets its date range, latest and highest values instead. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ChartSemanticsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun `the line chart says what it shows`() {
        compose.setContent {
            CalisthenicsMemoryTheme {
                Box(Modifier.size(300.dp)) {
                    SimpleLineChart(
                        data = listOf(GraphDataPoint("2026-03-01", 10f), GraphDataPoint("2026-03-05", 16f), GraphDataPoint("2026-03-09", 14f)),
                        exercise = Exercise(id = 1, name = "Push-up", type = "Dynamic"),
                        period = null,
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Chart from 2026-03-01 to 2026-03-09. Latest 14, highest 16.").assertExists()
    }
}
