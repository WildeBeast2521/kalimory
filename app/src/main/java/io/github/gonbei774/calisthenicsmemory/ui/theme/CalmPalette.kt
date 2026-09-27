package io.github.gonbei774.calisthenicsmemory.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The app's own palette (ADR 0004, decision 5): calm and focused. Chalk and spruce set the
 * tone; brass marks what is active; colour otherwise means state, never which screen you are on.
 * Every text/background pair is checked by CalmPaletteContrastTest.
 */
object CalmPalette {
    val light: ColorScheme = lightColorScheme(
        primary = Color(0xFF2F6B5E), // spruce: done, primary actions
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCFE6DE),
        onPrimaryContainer = Color(0xFF0E3A31),
        inversePrimary = Color(0xFF7DB8A9),
        secondary = Color(0xFF52635D), // stone green: quiet supporting actions
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDCE6E1),
        onSecondaryContainer = Color(0xFF1B2B26),
        tertiary = Color(0xFF8A5E17), // brass: active, in progress
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF3DEB8),
        onTertiaryContainer = Color(0xFF3A2605),
        background = Color(0xFFF2F4F1), // chalk
        onBackground = Color(0xFF1F2A27), // spruce ink
        surface = Color(0xFFF2F4F1),
        onSurface = Color(0xFF1F2A27),
        surfaceVariant = Color(0xFFDDE3DF),
        onSurfaceVariant = Color(0xFF4A5551),
        surfaceTint = Color(0xFF2F6B5E),
        inverseSurface = Color(0xFF2E3834),
        inverseOnSurface = Color(0xFFEEF2EF),
        error = Color(0xFFB23A48), // crimson: destructive, errors
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF8D9DC),
        onErrorContainer = Color(0xFF4A0D16),
        outline = Color(0xFF6F7A75),
        outlineVariant = Color(0xFFC4CCC7),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFFAFBF9),
        surfaceDim = Color(0xFFD9DEDA),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F9F6),
        surfaceContainer = Color(0xFFEDF0EC),
        surfaceContainerHigh = Color(0xFFE6EAE6),
        surfaceContainerHighest = Color(0xFFDFE4E0),
    )

    val dark: ColorScheme = darkColorScheme(
        primary = Color(0xFF7DB8A9),
        onPrimary = Color(0xFF0B372E),
        primaryContainer = Color(0xFF1E4F45),
        onPrimaryContainer = Color(0xFFC4E6DB),
        inversePrimary = Color(0xFF2F6B5E),
        secondary = Color(0xFFB2C4BD),
        onSecondary = Color(0xFF1D2D28),
        secondaryContainer = Color(0xFF34443F),
        onSecondaryContainer = Color(0xFFD4E3DD),
        tertiary = Color(0xFFE2B866),
        onTertiary = Color(0xFF402B00),
        tertiaryContainer = Color(0xFF5C420F),
        onTertiaryContainer = Color(0xFFF7DDB0),
        background = Color(0xFF121816), // night track
        onBackground = Color(0xFFE3E9E5),
        surface = Color(0xFF121816),
        onSurface = Color(0xFFE3E9E5),
        surfaceVariant = Color(0xFF3A4541),
        onSurfaceVariant = Color(0xFFBAC5C0),
        surfaceTint = Color(0xFF7DB8A9),
        inverseSurface = Color(0xFFE3E9E5),
        inverseOnSurface = Color(0xFF2E3834),
        error = Color(0xFFF0A1A9),
        onError = Color(0xFF5C1320),
        errorContainer = Color(0xFF7E2533),
        onErrorContainer = Color(0xFFFAD7DB),
        outline = Color(0xFF86918C),
        outlineVariant = Color(0xFF3A4541),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF38413E),
        surfaceDim = Color(0xFF121816),
        surfaceContainerLowest = Color(0xFF0D1210),
        surfaceContainerLow = Color(0xFF1A2220),
        surfaceContainer = Color(0xFF1E2724),
        surfaceContainerHigh = Color(0xFF28312E),
        surfaceContainerHighest = Color(0xFF333C39),
    )
}
