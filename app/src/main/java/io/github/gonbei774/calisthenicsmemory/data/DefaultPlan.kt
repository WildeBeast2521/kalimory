package io.github.gonbei774.calisthenicsmemory.data

import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.countsAsWork

/** What to start from for an exercise: a number of sets, and the reps or seconds when there is something to base them on. */
data class PlanDefault(val sets: Int, val value: Int?)

/**
 * The starting plan for an exercise that is being added to a workout or a program. The exercise's own target
 * wins. Without one it is one set, with the reps or seconds of the best set of the last session (warm-ups do
 * not count), or no value at all when there is no such session. It is never 0: nothing is invented.
 */
fun defaultPlan(targetSets: Int?, targetValue: Int?, last: List<HistorySet>): PlanDefault {
    val sets = targetSets?.takeIf { it > 0 } ?: 1
    val value = targetValue?.takeIf { it > 0 }
        ?: last.filter { it.countsAsWork }.maxOfOrNull { maxOf(it.valueRight, it.valueLeft ?: 0) }?.takeIf { it > 0 }
    return PlanDefault(sets, value)
}
