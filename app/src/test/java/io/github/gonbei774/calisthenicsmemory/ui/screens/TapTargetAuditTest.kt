package io.github.gonbei774.calisthenicsmemory.ui.screens

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

/** The audit itself: it reports what it should and passes what it should. */
@RunWith(RobolectricTestRunner::class)
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
}
