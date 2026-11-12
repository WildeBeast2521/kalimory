package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * System bar icons for a full-screen dialog drawn edge to edge (`decorFitsSystemWindows = false`).
 * Such a dialog is its own window and does not take the activity's bar style, so in a light theme
 * its status bar icons came out white on a light background. Call it first inside the dialog's
 * content: the icons go dark on a light background and light on a dark one, as in the main window.
 */
@Composable
fun DialogSystemBars() {
    val view = LocalView.current
    val lightBackground = MaterialTheme.colorScheme.background.luminance() > 0.5f
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightBackground
            isAppearanceLightNavigationBars = lightBackground
        }
    }
}
