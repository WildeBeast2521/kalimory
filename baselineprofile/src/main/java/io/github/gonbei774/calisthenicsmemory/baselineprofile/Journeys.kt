package io.github.gonbei774.calisthenicsmemory.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

const val PACKAGE = "io.github.gonbei774.calisthenicsmemory"
private const val TIMEOUT_MS = 5_000L
private const val GUIDE_TIMEOUT_MS = 1_500L

/**
 * Waits until the primary navigation bar is on screen: Today has drawn its first frame. A fresh
 * install first shows the welcome guide, which is skipped.
 */
fun MacrobenchmarkScope.waitForToday() {
    if (device.wait(Until.hasObject(By.text("Skip")), GUIDE_TIMEOUT_MS) == true) {
        device.findObject(By.text("Skip"))?.click()
    }
    device.wait(Until.hasObject(By.text("Train")), TIMEOUT_MS)
}

/**
 * Visits each primary destination, the path most people take after launch. Labels are the
 * English ones, so run on an English device.
 */
fun MacrobenchmarkScope.visitDestinations() {
    for (label in listOf("Train", "Progress", "Library", "Today")) {
        device.findObject(By.text(label))?.click()
        device.waitForIdle()
    }
}
