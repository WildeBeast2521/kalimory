package io.github.gonbei774.calisthenicsmemory.data.review

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueStep
import io.github.gonbei774.calisthenicsmemory.data.catalogue.MovementPattern
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import io.github.gonbei774.calisthenicsmemory.data.catalogue.MuscleGroup
import io.github.gonbei774.calisthenicsmemory.data.catalogue.group
import io.github.gonbei774.calisthenicsmemory.data.v2.Effort
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.IntervalHistoryItem
import io.github.gonbei774.calisthenicsmemory.data.v2.countsAsWork
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
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
    /** Sets behind the figure: catalogue training steps, and own exercises with muscles chosen. */
    val mappedSets: Int,
    /** Sets of mobility steps, which stretch rather than train and are not on the figure. */
    val mobilitySets: Int,
    /** Sets of exercises with no catalogue link and no muscles chosen, or missing from the library. */
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

/** Sets where something does the work, and sets it helps in; helping counts half. */
data class SetCounts(val direct: Int, val helping: Int) {
    val weighted: Double get() = direct + helping / 2.0
}

/** How far back the over-time view looks. Month and three months count by week; a year and all time by month. */
enum class TrendSpan(val days: Int?) { MONTH(30), THREE_MONTHS(90), YEAR(365), ALL(null); val byMonth: Boolean get() = this == YEAR || this == ALL }

/**
 * One week or month of [MuscleVolume.trend]: [start] to [endInclusive] (cut at today), the sets per
 * muscle and per muscle group, and how many weeks it spans for a rate. A bucket shorter than a week
 * counts as a week, so a few days at the start of a month are never scaled up.
 */
data class TrendBucket(
    val start: LocalDate,
    val endInclusive: LocalDate,
    val muscles: Map<Muscle, SetCounts>,
    val groups: Map<MuscleGroup, SetCounts>,
    val weeks: Double,
)

/** Sets per muscle from history and each exercise's muscles: its catalogue step's, or those chosen for an own exercise (ADR 0011, ADR 0014). Pure, nothing stored. */
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
            when (val target = targetOf(exercisesById[set.exerciseId])) {
                Target.Other -> other++
                Target.Mobility -> mobility++
                is Target.Muscles -> {
                    mapped++
                    target.primary.forEach { direct[it] = (direct[it] ?: 0) + 1 }
                    target.helping.forEach { helping[it] = (helping[it] ?: 0) + 1 }
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
     * exercises with no known muscles count for neither, and an unrated set is never "recovering": the app does
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
            val target = targetOf(exercisesById[set.exerciseId]) as? Target.Muscles ?: continue
            val weights = target.primary.associateWith { 1.0 } + target.helping.associateWith { 0.5 }
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

    /** What a set's exercise trains: a catalogue step's muscles, the muscles chosen for an own exercise, or nothing known. */
    private sealed interface Target {
        data object Mobility : Target
        data object Other : Target
        /** [helping] never repeats a muscle in [primary]: a muscle named in both counts once, as doing the work. */
        data class Muscles(val primary: Set<Muscle>, val helping: Set<Muscle>) : Target
    }

    /**
     * A linked exercise uses its step's muscles, fixed (ADR 0007). Only an exercise with no step, or an
     * unknown one, uses the muscles its owner chose (ADR 0014); with none chosen it is "other".
     */
    private fun targetOf(exercise: Exercise?): Target {
        val step = stepOf(exercise)
        return when {
            step != null && isMobility(step) -> Target.Mobility
            step != null -> Target.Muscles(step.primaryMuscles, step.secondaryMuscles - step.primaryMuscles)
            exercise != null && (exercise.primaryMuscles.isNotEmpty() || exercise.secondaryMuscles.isNotEmpty()) ->
                Target.Muscles(exercise.primaryMuscles, exercise.secondaryMuscles - exercise.primaryMuscles)
            else -> Target.Other
        }
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

    /**
     * The first day of each week or month the over-time view shows (#274, ADR 0015): weeks from the
     * user's [firstDay] for a month or three months, calendar months for a year or all time, the
     * first one complete and the last the one that contains [today]. [earliest] is the oldest date
     * with sets, for all time.
     */
    fun trendStarts(span: TrendSpan, today: LocalDate, firstDay: DayOfWeek, earliest: LocalDate?): List<LocalDate> {
        val from = if (span.days != null) today.minusDays(span.days - 1L) else earliest?.takeIf { it <= today } ?: today
        return if (span.byMonth) {
            generateSequence(from.withDayOfMonth(1)) { it.plusMonths(1) }.takeWhile { it <= today }.toList()
        } else {
            generateSequence(from.with(TemporalAdjusters.previousOrSame(firstDay))) { it.plusWeeks(1) }.takeWhile { it <= today }.toList()
        }
    }

    /** The oldest date among the sets that count as work and can be read, for [trendStarts] of all time. */
    fun earliest(history: List<HistorySet>): LocalDate? = history.asSequence().filter { it.countsAsWork }.mapNotNull { parse(it.date) }.minOrNull()

    /**
     * Sets per muscle and per group in each bucket that starts at [starts] (ascending; a bucket ends
     * the day before the next one, the last at [end]), in one pass over [history]: each set is placed
     * by binary search, so years of sets stay cheap. A set counts like [build] counts it: the same
     * resolver, warm-ups, mobility and unknown muscles left out. A group gets a set in full when any
     * muscle that does the work is in it, otherwise as helping when any helping muscle is, never twice.
     */
    fun trend(starts: List<LocalDate>, end: LocalDate, history: List<HistorySet>, exercises: List<Exercise>): List<TrendBucket> {
        if (starts.isEmpty()) return emptyList()
        val exercisesById = exercises.associateBy { it.id }
        val muscleDirect = List(starts.size) { mutableMapOf<Muscle, Int>() }
        val muscleHelping = List(starts.size) { mutableMapOf<Muscle, Int>() }
        val groupDirect = List(starts.size) { mutableMapOf<MuscleGroup, Int>() }
        val groupHelping = List(starts.size) { mutableMapOf<MuscleGroup, Int>() }
        for (set in history) {
            if (!set.countsAsWork) continue
            val day = parse(set.date) ?: continue
            if (day < starts.first() || day > end) continue
            val target = targetOf(exercisesById[set.exerciseId]) as? Target.Muscles ?: continue
            var index = starts.binarySearch(day)
            if (index < 0) index = -index - 2
            target.primary.forEach { muscleDirect[index].merge(it, 1, Int::plus) }
            target.helping.forEach { muscleHelping[index].merge(it, 1, Int::plus) }
            val mainGroups = target.primary.mapTo(mutableSetOf()) { it.group }
            mainGroups.forEach { groupDirect[index].merge(it, 1, Int::plus) }
            (target.helping.mapTo(mutableSetOf()) { it.group } - mainGroups).forEach { groupHelping[index].merge(it, 1, Int::plus) }
        }
        return starts.mapIndexed { i, start ->
            val last = if (i + 1 < starts.size) starts[i + 1].minusDays(1) else end
            val days = ChronoUnit.DAYS.between(start, last).toInt() + 1
            TrendBucket(
                start, last,
                (muscleDirect[i].keys + muscleHelping[i].keys).associateWith { SetCounts(muscleDirect[i][it] ?: 0, muscleHelping[i][it] ?: 0) },
                (groupDirect[i].keys + groupHelping[i].keys).associateWith { SetCounts(groupDirect[i][it] ?: 0, groupHelping[i][it] ?: 0) },
                maxOf(days, 7) / 7.0,
            )
        }
    }

    private fun parse(date: String): LocalDate? = runCatching { LocalDate.parse(date) }.getOrNull()
}
