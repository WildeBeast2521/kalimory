package io.github.gonbei774.calisthenicsmemory.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Every text/background pair of the calm palette meets WCAG AA in both themes. */
class CalmPaletteContrastTest {
    private fun channel(c: Float) = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    private fun luminance(c: Color) = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun textPairs(s: ColorScheme) = mapOf(
        "onBackground/background" to (s.onBackground to s.background),
        "onSurface/surface" to (s.onSurface to s.surface),
        "onSurfaceVariant/surface" to (s.onSurfaceVariant to s.surface),
        "onSurface/surfaceContainerHighest" to (s.onSurface to s.surfaceContainerHighest),
        "onSurfaceVariant/surfaceContainerHigh" to (s.onSurfaceVariant to s.surfaceContainerHigh),
        "primary/surface" to (s.primary to s.surface),
        "onPrimary/primary" to (s.onPrimary to s.primary),
        "onPrimaryContainer/primaryContainer" to (s.onPrimaryContainer to s.primaryContainer),
        "onSecondary/secondary" to (s.onSecondary to s.secondary),
        "onSecondaryContainer/secondaryContainer" to (s.onSecondaryContainer to s.secondaryContainer),
        "tertiary/surface" to (s.tertiary to s.surface),
        "onTertiary/tertiary" to (s.onTertiary to s.tertiary),
        "onTertiaryContainer/tertiaryContainer" to (s.onTertiaryContainer to s.tertiaryContainer),
        "error/surface" to (s.error to s.surface),
        "onError/error" to (s.onError to s.error),
        "onErrorContainer/errorContainer" to (s.onErrorContainer to s.errorContainer),
        "inverseOnSurface/inverseSurface" to (s.inverseOnSurface to s.inverseSurface),
    )

    private fun check(name: String, scheme: ColorScheme) {
        val failures = textPairs(scheme).mapNotNull { (pair, colors) ->
            val r = ratio(colors.first, colors.second)
            if (r < 4.5) "$pair = ${"%.2f".format(r)}" else null
        } + listOfNotNull(
            // Borders and other non-text UI need 3:1 (WCAG 1.4.11).
            ratio(scheme.outline, scheme.surface).takeIf { it < 3.0 }?.let { "outline/surface = ${"%.2f".format(it)}" },
        )
        assertTrue("$name below AA: $failures", failures.isEmpty())
    }

    @Test fun `light palette meets AA`() = check("light", CalmPalette.light)

    @Test fun `dark palette meets AA`() = check("dark", CalmPalette.dark)
}
