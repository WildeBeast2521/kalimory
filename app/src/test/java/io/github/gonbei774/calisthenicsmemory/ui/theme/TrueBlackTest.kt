package io.github.gonbei774.calisthenicsmemory.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrueBlackTest {

    @Test fun `the AMOLED theme paints the background black and keeps cards layered`() {
        val scheme = CalmPalette.dark.trueBlack()
        assertEquals(Color.Black, scheme.background)
        assertEquals(Color.Black, scheme.surface)
        assertTrue(scheme.surfaceContainerLow.luminanceOrder() < scheme.surfaceContainerHigh.luminanceOrder())
        // Content colours stay those of the dark theme.
        assertEquals(CalmPalette.dark.primary, scheme.primary)
        assertEquals(CalmPalette.dark.onSurface, scheme.onSurface)
    }

    @Test fun `cards stand out from black and from the cards around them`() {
        val scheme = CalmPalette.dark.trueBlack()
        // A card on the page, and a card inside a card, must be told apart at a glance.
        assertTrue(contrastRatio(scheme.background, scheme.surfaceContainerLow) >= 1.3)
        assertTrue(contrastRatio(scheme.surfaceContainerLow, scheme.surfaceContainerHigh) >= 1.12)
        assertTrue(contrastRatio(scheme.surfaceContainerHigh, scheme.surfaceContainerHighest) >= 1.12)
        // Text stays readable on the lightest container.
        assertTrue(contrastRatio(scheme.onSurface, scheme.surfaceContainerHighest) >= 4.5)
        assertTrue(contrastRatio(scheme.onSurfaceVariant, scheme.surfaceContainerHighest) >= 4.5)
    }

    @Test fun `AMOLED containers keep the dark theme's hue and chroma, only lighter`() {
        val dark = CalmPalette.dark
        val amoled = dark.trueBlack()
        listOf(
            dark.surfaceContainerLow to amoled.surfaceContainerLow,
            dark.surfaceContainer to amoled.surfaceContainer,
            dark.surfaceContainerHigh to amoled.surfaceContainerHigh,
            dark.surfaceContainerHighest to amoled.surfaceContainerHighest,
            dark.surfaceBright to amoled.surfaceBright,
        ).forEach { (d, a) ->
            val (dl, dc, dh) = oklch(d)
            val (al, ac, ah) = oklch(a)
            assertEquals("lightness step", 0.03, al - dl, 0.006)
            assertEquals("chroma", dc, ac, 0.003)
            assertEquals("hue", dh, ah, 4.0)
        }
    }

    /** OKLCH lightness, chroma and hue (degrees) of an sRGB colour. */
    private fun oklch(c: Color): Triple<Double, Double, Double> {
        fun lin(v: Float) = if (v <= 0.04045f) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        val r = lin(c.red); val g = lin(c.green); val b = lin(c.blue)
        val l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        val okL = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
        val okA = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val okB = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        return Triple(okL, Math.hypot(okA, okB), Math.toDegrees(Math.atan2(okB, okA)))
    }

    private fun Color.luminanceOrder() = red + green + blue
}
