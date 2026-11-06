package io.github.gonbei774.calisthenicsmemory.data.progression

import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueChain
import java.time.LocalDate

/** A step started ([met] false) or its move-on standard first met ([met] true), on [date]. [step] counts from 1. */
data class ClimbMark(val date: LocalDate, val step: Int, val met: Boolean)

/**
 * A chain's climb over time (#161), from the dates in [Progressions.journey]: [levels] is the highest
 * step started so far, changing only on the days it rose, so going back to an easier step for a
 * while does not read as a fall. Nothing here is stored or invented: every date is a session's.
 */
data class ChainClimb(
    /** The first session in the chain. */
    val start: LocalDate,
    /** Today, or the last date in the climb if that is later. */
    val end: LocalDate,
    /** (date, step) where the highest step started rose; the first is the first session. */
    val levels: List<Pair<LocalDate, Int>>,
    val marks: List<ClimbMark>,
) {
    val firstStep: Int get() = levels.first().second
    val highestStep: Int get() = levels.last().second
    val highestOn: LocalDate get() = levels.last().first
}

object Climb {
    /** The climb of [chain] from its [journey]; null when no step has been trained. Unreadable dates are skipped. */
    fun of(chain: CatalogueChain, journey: List<StepJourney>, today: LocalDate): ChainClimb? {
        fun parse(date: String?): LocalDate? = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val starts = journey.mapNotNull { entry ->
            parse(entry.firstOn)?.let { ClimbMark(it, chain.steps.indexOf(entry.step) + 1, met = false) }
        }.filter { it.step > 0 }
        if (starts.isEmpty()) return null
        val mets = journey.mapNotNull { entry ->
            parse(entry.metOn)?.let { ClimbMark(it, chain.steps.indexOf(entry.step) + 1, met = true) }
        }.filter { it.step > 0 }
        val levels = mutableListOf<Pair<LocalDate, Int>>()
        starts.sortedWith(compareBy({ it.date }, { it.step })).forEach { mark ->
            val current = levels.lastOrNull()
            when {
                current == null -> levels += mark.date to mark.step
                // Several steps started on the first day: the climb starts at the highest of them.
                current.first == mark.date && mark.step > current.second -> levels[levels.lastIndex] = mark.date to mark.step
                mark.step > current.second -> levels += mark.date to mark.step
            }
        }
        val marks = (starts + mets).sortedWith(compareBy({ it.date }, { it.step }, { it.met }))
        val end = maxOf(today, marks.maxOf { it.date })
        return ChainClimb(levels.first().first, end, levels, marks)
    }
}
