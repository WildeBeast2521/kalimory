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
 * The AMOLED theme: the dark scheme on true black, so unlit pixels stay off. Each container is
 * the dark theme's own container lifted by 0.03 in OKLCH lightness with its hue and chroma kept,
 * so cards read as the same colour as in Dark, only a step brighter to stand off black (at least
 * 1.3:1, and 1.12:1 between levels). Hand-picked greys drifted in hue and looked like another
 * colour. Regenerate with the formula in TrueBlackTest if the dark palette changes.
 */
internal fun ColorScheme.trueBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF212927),
    surfaceContainer = Color(0xFF252E2B),
    surfaceContainerHigh = Color(0xFF2F3936),
    surfaceContainerHighest = Color(0xFF3B4441),
    surfaceBright = Color(0xFF404946),
)
