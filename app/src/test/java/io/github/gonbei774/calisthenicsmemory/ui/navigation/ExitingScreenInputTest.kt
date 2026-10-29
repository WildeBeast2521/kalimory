package io.github.gonbei774.calisthenicsmemory.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A screen that is leaving stays on display while it animates out. A second tap in that time,
 * such as a double tap on "Record workout", must not reach it, or it would act twice.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExitingScreenInputTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aLeavingScreenTakesNoMoreTaps() {
        var screen by mutableStateOf("confirm")
        var saves by mutableIntStateOf(0)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            AnimatedContent(targetState = screen, transitionSpec = { screenTransition(forward = false) }, label = "test") { shown ->
                if (shown == "confirm") {
                    Button(onClick = { saves++; screen = "home" }, modifier = Modifier.fillMaxSize().ignoreInputWhileLeaving(this)) {
                        Text("Record workout")
                    }
                } else {
                    Text("Home")
                }
            }
        }
        compose.onNodeWithText("Record workout").performClick()
        compose.mainClock.advanceTimeBy(48) // A few frames into the exit animation.

        compose.onNodeWithText("Record workout").performClick()
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()

        assertEquals(1, saves)
    }
}
