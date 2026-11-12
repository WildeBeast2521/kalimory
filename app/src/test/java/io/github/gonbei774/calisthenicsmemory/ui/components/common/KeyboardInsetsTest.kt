package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The keyboard is counted once (#280): with a keyboard open, a screen's bottom sits at the top of the
 * keyboard, not a keyboard height higher, and the system bar under it is not added on top.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class KeyboardInsetsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // xhdpi: 2 px per dp.
    private val keyboardPx = 600
    private val navBarPx = 96

    private fun show(keyboard: Boolean) {
        compose.runOnUiThread {
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, navBarPx))
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, if (keyboard) keyboardPx else 0))
                .setVisible(WindowInsetsCompat.Type.ime(), keyboard)
                .build()
            ViewCompat.dispatchApplyWindowInsets(compose.activity.window.decorView, insets)
        }
        compose.waitForIdle()
    }

    private fun setContent(content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.runOnUiThread { WindowCompat.setDecorFitsSystemWindows(compose.activity.window, false) }
        compose.setContent(content)
    }

    private fun bottomGap(tag: String) =
        compose.onRoot().getBoundsInRoot().bottom - compose.onNodeWithTag(tag).getBoundsInRoot().bottom

    @Test fun `a screen clears the keyboard once, or the system bar when it is closed`() {
        setContent {
            Box(Modifier.fillMaxSize().screenBottomInsets()) {
                // A nested use must add nothing: the outer one consumed the insets.
                Box(Modifier.fillMaxSize().screenBottomInsets().testTag("content"))
            }
        }
        show(keyboard = false)
        assertEquals(48.dp, bottomGap("content"))
        show(keyboard = true)
        assertEquals(300.dp, bottomGap("content"))
    }

    @Test fun `home pads for the keyboard only and knows when it is open`() {
        var visible = false
        setContent {
            visible = keyboardVisible()
            Box(Modifier.fillMaxSize().keyboardInsets()) {
                Box(Modifier.fillMaxSize().testTag("content")) { Text(if (keyboardVisible()) "open" else "closed") }
            }
        }
        show(keyboard = false)
        assertEquals(0.dp, bottomGap("content"))
        assertEquals(false, visible)
        show(keyboard = true)
        assertEquals(300.dp, bottomGap("content"))
        assertEquals(true, visible)
    }
}
