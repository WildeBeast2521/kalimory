package io.github.gonbei774.calisthenicsmemory.workout

/**
 * How full a rest dial is: the time left over the planned rest. +10 s that lifts the time left
 * back to the plan refills the ring; only time beyond the plan stretches the scale, so the ring
 * is never shown short of full while the full plan remains.
 */
fun restDialProgress(plannedMillis: Long, remainingMillis: Long): Float {
    val left = remainingMillis.coerceAtLeast(0)
    val scale = maxOf(plannedMillis, left)
    return if (scale > 0) left.toFloat() / scale else 0f
}
