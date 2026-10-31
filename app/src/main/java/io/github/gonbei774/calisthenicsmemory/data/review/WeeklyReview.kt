package io.github.gonbei774.calisthenicsmemory.data.review

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.MovementPattern
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.IntervalHistoryItem
import java.time.LocalDate

/**
 * How the weekly review groups the catalogue's movement patterns (ADR 0010). Only [hasReference]
 * groups show the mark at [WeeklyReview.REFERENCE_SETS] sets a week.
 */
enum class MovementFamily(val hasReference: Boolean) {
    PUSH(true), PULL(true), LEGS(true), CORE(false), CONDITIONING(false), MOBILITY(false),
}

/** The review's group for a catalogue pattern. Exhaustive: a new pattern must be placed here. */
fun MovementPattern.family(): MovementFamily = when (this) {
    MovementPattern.HORIZONTAL_PUSH, MovementPattern.VERTICAL_PUSH -> MovementFamily.PUSH
    MovementPattern.HORIZONTAL_PULL, MovementPattern.VERTICAL_PULL -> MovementFamily.PULL
    MovementPattern.SQUAT, MovementPattern.HINGE -> MovementFamily.LEGS
    MovementPattern.CORE_ANTI_EXTENSION, MovementPattern.CORE_FLEXION, MovementPattern.CORE_LATERAL -> MovementFamily.CORE
    MovementPattern.CONDITIONING -> MovementFamily.CONDITIONING
    MovementPattern.MOBILITY -> MovementFamily.MOBILITY
}

data class FamilySets(val family: MovementFamily, val sets: Int)

/** One week of training, from history only: nothing here is stored or invented. */
data class WeekReview(
    val weekStart: LocalDate,
    /** Distinct days in the week with a set or an interval record. */
    val daysTrained: Int,
    /** History sets plus work intervals done: the number Today shows for the same week. */
    val totalSets: Int,
    /** Groups with at least one set, in [MovementFamily] order. */
    val families: List<FamilySets>,
    /** Sets of exercises with no catalogue link, or a link this catalogue does not know. */
    val otherSets: Int,
    /** Work intervals done ([IntervalHistoryItem.completedSets]). */
    val intervalSets: Int,
) {
    val isEmpty: Boolean get() = totalSets == 0 && daysTrained == 0
}

object WeeklyReview {
    /** The mark on Push, Pull and Legs: a common weekly reference for building muscle. */
    const val REFERENCE_SETS = 10

    /**
     * The week starting on [weekStart] (as Today's `weekOf` gives it). A set counts on its stored
     * day; rows whose date does not parse are skipped, as Today skips them. Every set lands in a
     * group, "other exercises" or intervals, so nothing is dropped.
     */
    fun build(
        weekStart: LocalDate,
        history: List<HistorySet>,
        intervals: List<IntervalHistoryItem>,
        exercises: List<Exercise>,
    ): WeekReview {
        val end = weekStart.plusDays(6)
        fun inWeek(date: String): LocalDate? = parse(date)?.takeIf { it in weekStart..end }

        val exercisesById = exercises.associateBy { it.id }
        val days = mutableSetOf<LocalDate>()
        val counts = mutableMapOf<MovementFamily, Int>()
        var other = 0
        var setCount = 0
        for (set in history) {
            val day = inWeek(set.date) ?: continue
            days += day
            setCount++
            when (val family = familyOf(exercisesById[set.exerciseId])) {
                null -> other++
                else -> counts[family] = (counts[family] ?: 0) + 1
            }
        }
        var intervalSets = 0
        for (item in intervals) {
            val day = inWeek(item.record.date) ?: continue
            days += day
            intervalSets += item.completedSets
        }
        return WeekReview(
            weekStart = weekStart,
            daysTrained = days.size,
            totalSets = setCount + intervalSets,
            families = MovementFamily.entries.mapNotNull { family -> counts[family]?.let { FamilySets(family, it) } },
            otherSets = other,
            intervalSets = intervalSets,
        )
    }

    /** The group of a library exercise from its catalogue link; null when unlinked or unknown. */
    fun familyOf(exercise: Exercise?): MovementFamily? =
        exercise?.catalogId?.let(Catalogue::step)?.let { Catalogue.chain(it.chainId) }?.pattern?.family()

    /** The earliest day with a set or an interval record; null when there is none. */
    fun firstTrainedDay(history: List<HistorySet>, intervals: List<IntervalHistoryItem>): LocalDate? =
        (history.asSequence().map { it.date } + intervals.asSequence().map { it.record.date })
            .mapNotNull(::parse).minOrNull()

    /** Earlier weeks are open while there is history before [weekStart]. */
    fun canGoBack(weekStart: LocalDate, firstTrained: LocalDate?): Boolean =
        firstTrained != null && firstTrained < weekStart

    /** Later weeks are open up to the current one; there are no future weeks. */
    fun canGoForward(weekStart: LocalDate, currentWeekStart: LocalDate): Boolean = weekStart < currentWeekStart

    private fun parse(date: String): LocalDate? = runCatching { LocalDate.parse(date) }.getOrNull()
}
