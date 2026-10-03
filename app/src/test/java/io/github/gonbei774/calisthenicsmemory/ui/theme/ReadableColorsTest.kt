package io.github.gonbei774.calisthenicsmemory.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Text on any role stays readable, whatever colours the wallpaper gives (owner report, 2026-10-03). */
class ReadableColorsTest {
    @Test fun `white text on a white accent becomes black`() {
        val scheme = darkColorScheme(primary = Color.White, onPrimary = Color(0xFFF5F5F5)).withReadableText()
        assertEquals(Color.Black, scheme.onPrimary)
    }

    @Test fun `dark text on a dark container becomes white`() {
        val scheme = lightColorScheme(primaryContainer = Color(0xFF111111), onPrimaryContainer = Color(0xFF222222)).withReadableText()
        assertEquals(Color.White, scheme.onPrimaryContainer)
    }

    @Test fun `readable pairs are left as they are`() {
        val original = lightColorScheme(primary = Color(0xFF2F6B5E), onPrimary = Color.White)
        val scheme = original.withReadableText()
        assertEquals(original.onPrimary, scheme.onPrimary)
        assertEquals(original.onSurface, scheme.onSurface)
    }

    @Test fun `every pair meets the contrast floor afterwards`() {
        val scheme = darkColorScheme(
            primary = Color(0xFFEEEEEE), onPrimary = Color(0xFFDDDDDD),
            secondary = Color(0xFF999999), onSecondary = Color(0xFF888888),
            surface = Color(0xFF101010), onSurface = Color(0xFF202020),
        ).withReadableText()
        listOf(
            scheme.primary to scheme.onPrimary,
            scheme.secondary to scheme.onSecondary,
            scheme.surface to scheme.onSurface,
        ).forEach { (back, fore) -> assertTrue("$fore on $back", contrastRatio(fore, back) >= MIN_TEXT_CONTRAST) }
    }
}
