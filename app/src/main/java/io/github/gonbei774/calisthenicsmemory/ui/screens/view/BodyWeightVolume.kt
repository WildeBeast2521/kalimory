package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import io.github.gonbei774.calisthenicsmemory.data.BodyWeight
import io.github.gonbei774.calisthenicsmemory.data.BodyWeightLoad
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.leftAssistanceG
import io.github.gonbei774.calisthenicsmemory.data.v2.leftWeightG
import kotlin.math.max

/** One day of an exercise with a share of body weight: volume in kg and the heaviest load as a share of that day's body weight. */
internal data class ShareDay(val date: String, val volumeRight: Float, val volumeLeft: Float?, val heaviestPercent: Int)

/** The days that have a body weight, and how many days had none and so are not counted (ADR 0016). */
internal class ShareVolumes(val days: List<ShareDay>, val daysWithoutBodyWeight: Int)

/**
 * Volume per day for an exercise with a share of body weight: each rep carries the share of the body
 * weight of its day, plus added weight, minus assistance. One-sided sets count each side's reps at the
 * full load. A day with no body weight yet (before the first entry) is left out whole, never drawn as a dip.
 * [sets] are the exercise's sets that count as work, already limited to the period.
 */
internal fun shareVolumes(exercise: Exercise, sets: List<HistorySet>, bodyWeights: List<BodyWeight>): ShareVolumes {
    val percent = exercise.bodyWeightPercent ?: return ShareVolumes(emptyList(), 0)
    val sorted = bodyWeights.sortedBy { it.date }
    var without = 0
    val days = sets.groupBy { it.date }.mapNotNull { (date, day) ->
        val bodyG = BodyWeightLoad.on(date, sorted)
        if (bodyG == null) { without++; return@mapNotNull null }
        val oneSided = isOneSided(exercise, day)
        var right = 0.0
        var left = 0.0
        var heaviest = 0
        day.forEach { set ->
            val loadRight = BodyWeightLoad.perRepG(percent, bodyG, set.weightG, set.assistanceG)
            right += set.valueRight * loadRight / 1000.0
            heaviest = max(heaviest, BodyWeightLoad.percentOfBody(loadRight, bodyG))
            if (oneSided) {
                val loadLeft = BodyWeightLoad.perRepG(percent, bodyG, set.leftWeightG, set.leftAssistanceG)
                left += (set.valueLeft ?: 0) * loadLeft / 1000.0
                heaviest = max(heaviest, BodyWeightLoad.percentOfBody(loadLeft, bodyG))
            }
        }
        ShareDay(date, right.toFloat(), if (oneSided) left.toFloat() else null, heaviest)
    }.sortedBy { it.date }
    return ShareVolumes(days, without)
}
