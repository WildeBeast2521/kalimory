package io.github.gonbei774.calisthenicsmemory.workout

import android.os.SystemClock

/** Milliseconds that never go backwards while the device stays booted and include deep sleep. */
fun interface MonotonicClock {
    fun nowMillis(): Long

    companion object {
        val SYSTEM = MonotonicClock { SystemClock.elapsedRealtime() }
    }
}
