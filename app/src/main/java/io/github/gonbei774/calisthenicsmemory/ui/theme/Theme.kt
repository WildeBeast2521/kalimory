package io.github.gonbei774.calisthenicsmemory.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class AppColors(
    val background: Color,
    val backgroundGradientStart: Color,
    val backgroundGradientEnd: Color,
    val cardBackground: Color,
    val cardBackgroundSelected: Color,
    val cardBackgroundSecondary: Color,
    val cardBackgroundDisabled: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val border: Color,
    val borderFocused: Color,
    val divider: Color,
    val switchTrack: Color,
    val switchThumb: Color,
    val timerTrack: Color,
    val isDark: Boolean
)

private val DarkAppColors = AppColors(
    background = Slate900,
    backgroundGradientStart = Slate900,
    backgroundGradientEnd = Slate800,
    cardBackground = Slate800,
    cardBackgroundSelected = Slate750,
    cardBackgroundSecondary = Slate700,
    cardBackgroundDisabled = Slate700,
    textPrimary = Color.White,
    textSecondary = Slate400,
    textTertiary = Slate300,
    textDisabled = Slate500,
    border = Slate600,
    borderFocused = Blue600,
    divider = Slate700,
    switchTrack = Slate500,
    switchThumb = Color.White,
    timerTrack = Slate600,
    isDark = true
)

private val LightAppColors = AppColors(
    background = Color.White,
    backgroundGradientStart = Color.White,
    backgroundGradientEnd = Slate50,
    cardBackground = Slate50,
    cardBackgroundSelected = Slate100,
    cardBackgroundSecondary = Slate100,
    cardBackgroundDisabled = Slate200,
    textPrimary = Slate800,
    textSecondary = Slate500,
    textTertiary = Slate600,
    textDisabled = Slate400,
    border = Slate300,
    borderFocused = Blue600,
    divider = Slate200,
    switchTrack = Slate300,
    switchThumb = Color.White,
    timerTrack = Slate200,
    isDark = false
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

/**
 * The app theme (ADR 0004, decision 5): Material 3 Expressive components with the app's own calm
 * palette, type and shapes. With [dynamicColor] on Android 12+, colours follow the wallpaper
 * instead. [LocalAppColors] still serves the legacy screens until each is moved to the theme roles.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalisthenicsMemoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> CalmPalette.dark
        else -> CalmPalette.light
    }
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            // Calm, not bouncy; the one expressive moment is reserved for completing a set.
            motionScheme = MotionScheme.standard(),
            typography = Typography,
            shapes = CalmShapes,
            content = content
        )
    }
}
