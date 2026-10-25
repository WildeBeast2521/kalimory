package io.github.gonbei774.calisthenicsmemory.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG AA contrast for Kalimory's own themes (Light, Dark and AMOLED): text at least 4.5:1 against
 * every background it can sit on, and borders and other non-text parts at least 3:1. Wallpaper
 * colours are made readable at run time instead (ReadableColorsTest).
 */
class CalmPaletteContrastTest {
    private val themes = mapOf(
        "Light" to CalmPalette.light,
        "Dark" to CalmPalette.dark,
        "AMOLED" to CalmPalette.dark.trueBlack(),
    )

    private fun ColorScheme.surfaces() = mapOf(
        "background" to background, "surface" to surface, "surfaceDim" to surfaceDim, "surfaceBright" to surfaceBright,
        "surfaceContainerLowest" to surfaceContainerLowest, "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainer" to surfaceContainer, "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest, "surfaceVariant" to surfaceVariant,
    )

    private fun ColorScheme.textPairs(): List<Triple<String, Color, Color>> = buildList {
        add(Triple("onPrimary on primary", onPrimary, primary))
        add(Triple("onPrimaryContainer on primaryContainer", onPrimaryContainer, primaryContainer))
        add(Triple("onSecondary on secondary", onSecondary, secondary))
        add(Triple("onSecondaryContainer on secondaryContainer", onSecondaryContainer, secondaryContainer))
        add(Triple("onTertiary on tertiary", onTertiary, tertiary))
        add(Triple("onTertiaryContainer on tertiaryContainer", onTertiaryContainer, tertiaryContainer))
        add(Triple("onError on error", onError, error))
        add(Triple("onErrorContainer on errorContainer", onErrorContainer, errorContainer))
        add(Triple("inverseOnSurface on inverseSurface", inverseOnSurface, inverseSurface))
        add(Triple("onBackground on background", onBackground, background))
        for ((name, back) in surfaces()) {
            add(Triple("onSurface on $name", onSurface, back))
            add(Triple("onSurfaceVariant on $name", onSurfaceVariant, back))
            // Text buttons, links and selected labels are written in the primary colour.
            add(Triple("primary on $name", primary, back))
        }
    }

    private fun failures(minimum: Double, pairs: (ColorScheme) -> List<Triple<String, Color, Color>>) =
        themes.flatMap { (theme, scheme) ->
            pairs(scheme).mapNotNull { (name, fore, back) ->
                val ratio = contrastRatio(fore, back)
                if (ratio < minimum) "$theme: $name is %.2f:1".format(ratio) else null
            }
        }

    @Test fun `text reaches 4·5 to 1 on everything it sits on`() {
        val problems = failures(MIN_TEXT_CONTRAST) { it.textPairs() }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test fun `borders reach 3 to 1 on every surface`() {
        val problems = failures(3.0) { scheme -> scheme.surfaces().map { (name, back) -> Triple("outline on $name", scheme.outline, back) } }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
