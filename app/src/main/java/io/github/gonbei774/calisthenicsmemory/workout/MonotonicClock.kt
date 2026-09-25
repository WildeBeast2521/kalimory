package io.github.gonbei774.calisthenicsmemory.workout

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

/** Milliseconds that never go backwards while the device stays booted and include deep sleep. */
fun interface MonotonicClock {
    fun nowMillis(): Long

    companion object {
        val SYSTEM = MonotonicClock { SystemClock.elapsedRealtime() }
    }
}

/** Settings.Global.BOOT_COUNT, used to tell whether monotonic times from a checkpoint are still valid. */
fun currentBootCount(context: Context): Int? =
    Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
        .takeIf { it >= 0 }
