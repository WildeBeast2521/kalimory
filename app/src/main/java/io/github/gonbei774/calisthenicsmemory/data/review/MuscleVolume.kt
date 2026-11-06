package io.github.gonbei774.calisthenicsmemory.data.review

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueStep
import io.github.gonbei774.calisthenicsmemory.data.catalogue.MovementPattern
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import io.github.gonbei774.calisthenicsmemory.data.v2.Effort
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.IntervalHistoryItem
import io.github.gonbei774.calisthenicsmemory.data.v2.countsAsWork
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** One muscle's sets in a period (ADR 0011): sets where it does the work, and sets it helps in. */
data class MuscleSets(val muscle: Muscle, val direct: Int, val helping: Int) {
    /** Sets where it does the work in full, sets it helps in as half (the app's convention, ADR 0011). */
    val weighted: Double get() = direct + helping / 2.0
}

/** The figure's shades, by weighted sets a week: under 5, 5 to 9, 10 or more. A reading aid. */
enum class VolumeTier { NONE, LOW, MID, HIGH }

data class MuscleVolumeSummary(
    val start: LocalDate,
    val end: LocalDate,
    /** Muscles with at least one direct or helping set, most weighted first, ties in [Muscle] order. */
    val muscles: List<MuscleSets>,
    /** Sets of catalogue training steps: the sets behind the figure. */
    val mappedSets: Int,
    /** Sets of mobility steps, which stretch rather than train and are not on the figure. */
    val mobilitySets: Int,
    /** Sets of exercises with no catalogue link, an unknown link, or missing from the library. */
    val otherSets: Int,
    /** Work intervals done ([IntervalHistoryItem.completedSets]). */
    val intervalSets: Int,
) {
    /** Every set of work in the period: for a week, the weekly review's and Today's number. */
    val totalSets: Int get() = mappedSets + mobilitySets + otherSets + intervalSets
    val isEmpty: Boolean get() = totalSets == 0
}

/** How recently a muscle was worked (ADR 0011, ADR 0012), in calendar days. */
enum class RecentTier { WORKED, RECOVERING }

/**
 * A muscle worked recently. [daysAgo] is 0 for today, 1 for yesterday, 2 for the day before: the
 * last day of the sets that put it in its [tier].
 */
data class MuscleRecent(val muscle: Muscle, val tier: RecentTier, val daysAgo: Int)

/** Sets per muscle from history and each exercise's catalogue step (ADR 0011). Pure, nothing stored. */
object MuscleVolume {
    /** Weekly sets at which the shades change: under 5, 5 to 9, 10 or more. */
    const val MID_SETS = 5
    const val HIGH_SETS = WeeklyReview.REFERENCE_SETS

    /**
     * Sets from [start] to [end], both included. A set counts on its stored day; unreadable dates
     * are skipped and warm-ups left out (ADR 0012), as the weekly review does. Every other set lands
     * on the figure, in mobility, in other exercises or in intervals, so none is dropped.
     */
    fun build(
        start: LocalDate,
        end: LocalDate,
        history: List<HistorySet>,
        intervals: List<IntervalHistoryItem>,
        exercises: List<Exercise>,
    ): MuscleVolumeSummary {
        fun inPeriod(date: String): Boolean = parse(date)?.let { it in start..end } == true
        val exercisesById = exercises.associateBy { it.id }
        val direct = mutableMapOf<Muscle, Int>()
        val helping = mutableMapOf<Muscle, Int>()
        var mapped = 0
        var mobility = 0
        var other = 0
        for (set in history) {
            if (!set.countsAsWork || !inPeriod(set.date)) continue
            val step = stepOf(exercisesById[set.exerciseId])
            when {
                step == null -> other++
                isMobility(step) -> mobility++
                else -> {
                    mapped++
                    step.primaryMuscles.forEach { direct[it] = (direct[it] ?: 0) + 1 }
                    // A muscle named in both lists counts once, as doing the work.
                    (step.secondaryMuscles - step.primaryMuscles).forEach { helping[it] = (helping[it] ?: 0) + 1 }
                }
            }
        }
        val intervalSets = intervals.filter { inPeriod(it.record.date) }.sumOf { it.completedSets }
        val muscles = (direct.keys + helping.keys)
            .map { MuscleSets(it, direct[it] ?: 0, helping[it] ?: 0) }
            .sortedWith(compareByDescending<MuscleSets> { it.weighted }.thenBy { it.muscle.ordinal })
        return MuscleVolumeSummary(start, end, muscles, mapped, mobility, other, intervalSets)
    }

    /**
     * The muscles the catalogue's training steps name as doing the work (ADR 0011): the ones the
     * "Not worked" view can list. Muscles only ever named as helping, and those of mobility steps,
     * are left out, so the view never lists a muscle no training step aims at.
     */
    val trainable: Set<Muscle> by lazy {
        Catalogue.chains.filter { it.pattern != MovementPattern.MOBILITY }
            .flatMap { it.steps }.filterNot { it.retired }
            .flatMapTo(mutableSetOf()) { it.primaryMuscles }
    }

    /** Trainable muscles with no set at all in the period, doing the work or helping, in [Muscle] order. */
    fun notWorked(summary: MuscleVolumeSummary): List<Muscle> {
        val worked = summary.muscles.mapTo(mutableSetOf()) { it.muscle }
        return trainable.filterNot { it in worked }.sortedBy { it.ordinal }
    }

    /**
     * Muscles worked recently (ADR 0012, section 4). A muscle is [RecentTier.RECOVERING] when sets
     * rated 0 or 1 left ([Effort.isHard]) add up to at least one full set for it today or in the two
     * days before, and [RecentTier.WORKED] when any sets of work add up to one today or yesterday.
     * A set counts 1 for a muscle it mainly works and 0.5 for one that helps. Warm-ups, mobility and
     * unlinked exercises count for neither, and an unrated set is never "recovering": the app does
     * not know how hard it was. Recovering first, then worked; each in [Muscle] order.
     */
    fun recent(today: LocalDate, history: List<HistorySet>, exercises: List<Exercise>): List<MuscleRecent> {
        val exercisesById = exercises.associateBy { it.id }
        val hard = mutableMapOf<Muscle, Double>()
        val worked = mutableMapOf<Muscle, Double>()
        val lastHard = mutableMapOf<Muscle, Int>()
        val lastWorked = mutableMapOf<Muscle, Int>()
        for (set in history) {
            if (!set.countsAsWork) continue
            val day = parse(set.date) ?: continue
            val daysAgo = ChronoUnit.DAYS.between(day, today).toInt()
            if (daysAgo !in 0..2) continue
            val step = stepOf(exercisesById[set.exerciseId]) ?: continue
            if (isMobility(step)) continue
            val weights = step.primaryMuscles.associateWith { 1.0 } + (step.secondaryMuscles - step.primaryMuscles).associateWith { 0.5 }
            weights.forEach { (muscle, weight) ->
                if (Effort.isHard(set.effortLeft)) {
                    hard[muscle] = (hard[muscle] ?: 0.0) + weight
                    lastHard[muscle] = minOf(lastHard[muscle] ?: daysAgo, daysAgo)
                }
                if (daysAgo <= 1) {
                    worked[muscle] = (worked[muscle] ?: 0.0) + weight
                    lastWorked[muscle] = minOf(lastWorked[muscle] ?: daysAgo, daysAgo)
                }
            }
        }
        val recovering = hard.filterValues { it >= 1.0 }.keys
        val recentlyWorked = worked.filterValues { it >= 1.0 }.keys - recovering
        return recovering.sortedBy { it.ordinal }.map { MuscleRecent(it, RecentTier.RECOVERING, lastHard.getValue(it)) } +
            recentlyWorked.sortedBy { it.ordinal }.map { MuscleRecent(it, RecentTier.WORKED, lastWorked.getValue(it)) }
    }

    /** The catalogue step a library exercise is linked to; null when unlinked or unknown. */
    fun stepOf(exercise: Exercise?): CatalogueStep? = exercise?.catalogId?.let(Catalogue::step)

    /** Mobility steps name the muscles they stretch, so their sets are not training volume. */
    fun isMobility(step: CatalogueStep): Boolean = Catalogue.chain(step.chainId)?.pattern == MovementPattern.MOBILITY

    fun tier(weightedPerWeek: Double): VolumeTier = when {
        weightedPerWeek <= 0.0 -> VolumeTier.NONE
        weightedPerWeek < MID_SETS -> VolumeTier.LOW
        weightedPerWeek < HIGH_SETS -> VolumeTier.MID
        else -> VolumeTier.HIGH
    }

    /** Each muscle's shade, for a period [weeks] long (1 for the calendar week). */
    fun tiers(summary: MuscleVolumeSummary, weeks: Double): Map<Muscle, VolumeTier> =
        summary.muscles.associate { it.muscle to tier(it.weighted / weeks) }

    private fun parse(date: String): LocalDate? = runCatching { LocalDate.parse(date) }.getOrNull()
}
