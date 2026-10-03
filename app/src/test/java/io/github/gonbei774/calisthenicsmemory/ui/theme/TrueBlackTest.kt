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

    private fun Color.luminanceOrder() = red + green + blue
}
