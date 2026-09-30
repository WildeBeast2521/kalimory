package io.github.gonbei774.calisthenicsmemory.data.v2

/**
 * What the summary after a live workout shows: totals, each exercise's sets, the change since
 * that exercise was last done, and any new personal best. Built from the saved session and the
 * merged history, without touching the database, so it can be tested on the JVM.
 */
data class WorkoutSummary(
    val sourceType: WorkoutSourceType,
    /** The program or interval program name; null for a single exercise, whose row names it. */
    val title: String?,
    val completedSets: Int,
    val totalRepetitions: Int,
    val totalHoldSeconds: Int,
    /** Whole minutes from start to end; null when the start was not observed exactly. */
    val durationMinutes: Int?,
    val exercises: List<ExerciseResult>,
    val personalBests: List<PersonalBest>,
)

data class ExerciseResult(
    val name: String,
    val kind: ExerciseKind,
    /** Completed sets in order; [SetResult.left] is set for one-sided exercises. */
    val sets: List<SetResult>,
    /** Completed plus skipped sets, so the ring shows how much of the plan was done. */
    val plannedSets: Int,
    /**
     * Reps (or seconds held) in total the last time this exercise was done; null the first time.
     * Shown as a plain fact rather than a difference: a shorter workout is not a worse one.
     */
    val lastTimeTotal: Int?,
) {
    val total: Int get() = sets.sumOf { it.right + (it.left ?: 0) }
}

data class SetResult(val right: Int, val left: Int?)

data class PersonalBest(val exerciseName: String, val kind: ExerciseKind, val value: Int, val previousBest: Int)

object WorkoutSummaryBuilder {

    fun build(graph: WorkoutSessionGraph, history: List<HistorySet>): WorkoutSummary {
        val session = graph.session
        val sessionEntryIds = graph.exercises.flatMap { (_, sets) -> sets.map { it.id } }.toSet()
        // Earlier sets only: this workout's own sets are already in the history.
        val earlier = history.filter { set -> set.setEntryIds.none { it in sessionEntryIds } }
            .groupBy { it.exerciseId }

        val results = mutableListOf<ExerciseResult>()
        val bests = mutableListOf<PersonalBest>()
        for ((exercise, entries) in graph.exercises) {
            val completed = entries.filter { it.status == SetEntryStatus.COMPLETED }
            // The two sides of one set share its round and set number.
            val sets = completed
                .groupBy { it.roundNumber to it.setNumber }
                .values
                .map { sides ->
                    val left = sides.firstOrNull { it.side == BodySide.LEFT }
                    val right = sides.firstOrNull { it.side != BodySide.LEFT }
                    SetResult(right?.let { value(it, exercise.exerciseKindSnapshot) } ?: 0, left?.let { value(it, exercise.exerciseKindSnapshot) })
                }
            val planned = entries
                .filter { it.status == SetEntryStatus.COMPLETED || it.status == SetEntryStatus.SKIPPED }
                .map { it.roundNumber to it.setNumber }
                .distinct()
                .size
            val previous = exercise.exerciseId?.let { earlier[it] }.orEmpty()
            val result = ExerciseResult(
                name = exercise.exerciseNameSnapshot,
                kind = exercise.exerciseKindSnapshot,
                sets = sets,
                plannedSets = planned,
                lastTimeTotal = lastTimeTotal(previous),
            )
            results += result
            personalBest(exercise, completed, sets, previous)?.let { bests += it }
        }

        val ended = session.endedAtEpochMillis
        val duration = if (ended != null && session.timePrecision == TimePrecision.EXACT) {
            ((ended - session.startedAtEpochMillis) / 60_000L).toInt().coerceAtLeast(1)
        } else null

        return WorkoutSummary(
            sourceType = session.sourceType,
            title = session.sourceNameSnapshot.takeIf { session.sourceType != WorkoutSourceType.AD_HOC },
            completedSets = results.sumOf { it.sets.size },
            totalRepetitions = results.filter { it.kind == ExerciseKind.DYNAMIC }.sumOf { it.total },
            totalHoldSeconds = results.filter { it.kind == ExerciseKind.ISOMETRIC }.sumOf { it.total },
            durationMinutes = duration,
            exercises = results,
            personalBests = bests,
        )
    }

    private fun value(entry: SetEntryEntity, kind: ExerciseKind): Int = when (kind) {
        ExerciseKind.DYNAMIC -> entry.repetitions ?: 0
        ExerciseKind.ISOMETRIC -> ((entry.durationMillis ?: 0) / 1_000).toInt()
    }

    /** The total of the most recent earlier workout of this exercise, grouped as history shows it. */
    private fun lastTimeTotal(previous: List<HistorySet>): Int? =
        previous.groupBy { it.date to it.time }
            .maxByOrNull { (key, _) -> key.first + " " + key.second }
            ?.value
            ?.sumOf { it.valueRight + (it.valueLeft ?: 0) }

    /**
     * A best is the highest single-side value in one set. Sets with added weight, assistance or
     * distance are left out on both sides of the comparison, as their counts mean something else.
     * There is no best the first time an exercise is done.
     */
    private fun personalBest(
        exercise: SessionExerciseEntity,
        completed: List<SetEntryEntity>,
        sets: List<SetResult>,
        previous: List<HistorySet>,
    ): PersonalBest? {
        if (completed.any { it.addedWeightGrams != null || it.assistanceGrams != null || it.distanceCm != null }) return null
        val comparable = previous.filter { it.weightG == null && it.assistanceG == null && it.distanceCm == null }
        if (comparable.isEmpty()) return null
        val now = sets.maxOfOrNull { maxOf(it.right, it.left ?: 0) } ?: return null
        val before = comparable.maxOf { maxOf(it.valueRight, it.valueLeft ?: 0) }
        return if (now > before) PersonalBest(exercise.exerciseNameSnapshot, exercise.exerciseKindSnapshot, now, before) else null
    }
}
