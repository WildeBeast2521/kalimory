package io.github.gonbei774.calisthenicsmemory.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate

/**
 * Today's date for a screen, kept current. The app stays alive in the background, so a date read
 * once would still be yesterday's when the app is opened the next morning. It is read again each
 * time the app resumes, and at midnight while the app stays in front.
 */
@Composable
fun rememberToday(): LocalDate {
    var today by remember { mutableStateOf(AppClock.today()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                today = AppClock.today()
                val now = AppClock.now()
                val untilMidnight = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis()
                // A second past midnight, so the new date has certainly begun.
                delay(untilMidnight + 1_000)
            }
        }
    }
    return today
}
