package io.github.gonbei774.calisthenicsmemory.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * The app theme (ADR 0004, decision 5): Material 3 Expressive components with the app's own calm
 * palette, type and shapes. With [dynamicColor] on Android 12+, colours follow the wallpaper
 * instead.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalisthenicsMemoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    // Dark with true black behind everything (the AMOLED theme); ignored in light mode.
    trueBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        // Wallpaper colours can be nearly colourless, so their text colours are checked for contrast.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            (if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).withReadableText()
        darkTheme -> CalmPalette.dark
        else -> CalmPalette.light
    }.let { if (darkTheme && trueBlack) it.trueBlack() else it }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        // Expressive springs give components a physical feel; screen and tab transitions stay short
        // and unbouncy (ui/navigation/ScreenMotion.kt).
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        shapes = CalmShapes,
        content = content
    )
}

/**
 * The AMOLED theme: the dark scheme with true black behind everything, so unlit pixels stay off.
 * Cards and other containers keep the Dark theme's exact greys, so both themes look the same apart
 * from the page behind them.
 */
internal fun ColorScheme.trueBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
)
