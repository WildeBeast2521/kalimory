package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The accessibility audits themselves: they report what they should and pass what they should. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class TapTargetAuditTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun `a tiny unlabelled target is reported for both problems`() {
        // Two neighbours with no gap, so Compose cannot widen either touch area.
        compose.setContent {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                Box(Modifier.size(12.dp).clickable {})
                Box(Modifier.size(12.dp).clickable {})
            }
        }
        val problems = tapTargetProblems(compose, compose.density)
        assertEquals(problems.joinToString("\n"), 2, problems.size)
        assertTrue(problems.joinToString("\n"), problems.all { "no label" in it && "under 48 dp" in it })
    }

    @Test fun `a labelled Material button passes`() {
        compose.setContent { Button(onClick = {}) { Text("Start") } }
        assertEquals(emptyList<String>(), tapTargetProblems(compose, compose.density))
    }

    @Test fun `a word too wide for its space is reported, wrapped or shortened`() {
        compose.setContent {
            androidx.compose.foundation.layout.Column {
                Text("Progressions", Modifier.width(40.dp))
                Text("Progressions", Modifier.width(40.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        val problems = textFitProblems(compose)
        assertEquals(problems.joinToString("\n"), 2, problems.size)
        assertTrue(problems.any { "splits a word" in it } && problems.any { "shortened" in it })
    }

    @Test fun `a word that fits passes`() {
        compose.setContent { Text("Rest", Modifier.width(200.dp)) }
        assertEquals(emptyList<String>(), textFitProblems(compose))
    }
}
