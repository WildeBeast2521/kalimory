package io.github.gonbei774.calisthenicsmemory.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the app's Baseline Profile from a cold start and a visit to each destination.
 * Run with `./gradlew :app:generateBaselineProfile` on an emulator (API 28+; rooted below 33).
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupAndDestinations() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        waitForToday()
        visitDestinations()
    }
}
