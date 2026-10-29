package io.github.gonbei774.calisthenicsmemory.util

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/** A screen left open overnight shows the new day when the app comes back. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RememberTodayTest {
    @get:Rule
    val compose = createComposeRule()

    private fun fixAt(date: LocalDate) {
        AppClock.fixed = Clock.fixed(date.atTime(23, 30).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    }

    @After
    fun release() {
        AppClock.fixed = null
    }

    @Test
    fun theDateIsReadAgainWhenTheAppResumes() {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry.createUnsafe(this)
            override val lifecycle: Lifecycle get() = registry
        }
        owner.registry.currentState = Lifecycle.State.RESUMED
        fixAt(LocalDate.of(2026, 10, 8))
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                Text(rememberToday().toString())
            }
        }
        compose.onNodeWithText("2026-10-08").assertExists()

        // Overnight in the background, then back.
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        fixAt(LocalDate.of(2026, 10, 9))
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }

        compose.onNodeWithText("2026-10-09").assertExists()
    }
}
