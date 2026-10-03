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

    private fun Color.luminanceOrder() = red + green + blue
}
