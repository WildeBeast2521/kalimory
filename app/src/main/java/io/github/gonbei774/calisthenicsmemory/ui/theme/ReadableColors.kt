package io.github.gonbei774.calisthenicsmemory.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** WCAG's floor for normal text. */
const val MIN_TEXT_CONTRAST = 4.5

/** WCAG contrast ratio between two colours, from 1 (none) to 21 (black on white). */
fun contrastRatio(a: Color, b: Color): Double {
    val la = a.luminance() + 0.05
    val lb = b.luminance() + 0.05
    return (maxOf(la, lb) / minOf(la, lb)).toDouble()
}

/** The text colour unchanged if it reads on [background], else black or white, whichever reads better. */
private fun Color.readableOn(background: Color): Color {
    if (contrastRatio(this, background) >= MIN_TEXT_CONTRAST) return this
    return if (contrastRatio(Color.Black, background) >= contrastRatio(Color.White, background)) Color.Black else Color.White
}

/**
 * Wallpaper colours can be nearly colourless, such as a white accent from a black-and-white
 * wallpaper. Every "on" colour is checked against its role, and against the most contrasting
 * surface it sits on, so text and icons never vanish into their background. Colour is kept
 * wherever it already reads.
 */
fun ColorScheme.withReadableText(): ColorScheme {
    // onSurface text sits on every surface container; check it against the furthest one.
    val onSurfaceFixed = onSurface.readableOn(surface).readableOn(surfaceContainerHighest)
    return copy(
        onPrimary = onPrimary.readableOn(primary),
        onPrimaryContainer = onPrimaryContainer.readableOn(primaryContainer),
        onSecondary = onSecondary.readableOn(secondary),
        onSecondaryContainer = onSecondaryContainer.readableOn(secondaryContainer),
        onTertiary = onTertiary.readableOn(tertiary),
        onTertiaryContainer = onTertiaryContainer.readableOn(tertiaryContainer),
        onError = onError.readableOn(error),
        onErrorContainer = onErrorContainer.readableOn(errorContainer),
        onBackground = onBackground.readableOn(background),
        onSurface = onSurfaceFixed,
        onSurfaceVariant = onSurfaceVariant.readableOn(surfaceVariant).readableOn(surface),
        inverseOnSurface = inverseOnSurface.readableOn(inverseSurface),
    )
}
