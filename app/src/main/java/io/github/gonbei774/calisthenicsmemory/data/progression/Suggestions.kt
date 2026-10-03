package io.github.gonbei774.calisthenicsmemory.data.progression

import io.github.gonbei774.calisthenicsmemory.data.catalogue.MovementPattern
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueChain
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueStep
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Standard
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import java.time.LocalDate

enum class SuggestionReason {
    /** The step has not been trained yet: start at its working standard. */
    FIRST_SESSION,
    /** Keep building towards the move-on standard. */
    CONTINUE,
    /** The standard was met and the next step is in the library: start it. */
    NEXT_STEP,
    /** The standard was met and the next step is not in the library yet. */
    NEXT_STEP_NOT_ADDED,
}

/** One suggestion for today (ADR 0005, decision 5). It only suggests; starting it is up to the user. */
data class Suggestion(
    val chain: CatalogueChain,
    val step: CatalogueStep,
    /** The library exercise for [step]; null when the step is not in the library yet. */
    val exercise: Exercise?,
    val target: Standard,
    val reason: SuggestionReason,
    val lastSession: StepSession?,
    /** An easier step to consider after two sessions short of the working standard; never forced. */
    val easier: CatalogueStep?,
)

object Suggestions {
    /** Seconds added to a hold per session; one rep is added to a dynamic step. */
    const val HOLD_INCREMENT_SECONDS = 5

    /**
     * Suggestions for [today], least recently trained chain first. A chain is left out when it was
     * dismissed today, or when a chain of the same movement pattern was trained today or yesterday,
     * so the movement rests about two days. Mobility rests only for the day it was trained.
     * History holds dates, not exact hours, so rest is counted in days.
     */
    fun forDay(
        today: LocalDate,
        exercises: List<Exercise>,
        history: List<HistorySet>,
        dismissedChains: Set<String> = emptySet(),
        unfollowedChains: Set<String> = emptySet(),
    ): List<Suggestion> {
        val progressions = Progressions.chains(exercises, history)
        val byCatalogId = exercises.filter { it.catalogId != null }.associateBy { it.catalogId!! }
        val historyByExercise = history.groupBy { it.exerciseId }

        val lastTrained = progressions.associate { progress ->
            val dates = progress.chain.steps
                .mapNotNull { byCatalogId[it.id] }
                .flatMap { historyByExercise[it.id].orEmpty() }
                .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            progress.chain.id to dates.maxOrNull()
        }
        // Mobility needs no rest day; a mobility chain is only left out on the day it was trained.
        val restingPatterns = progressions
            .filter { it.chain.pattern != MovementPattern.MOBILITY }
            .filter { lastTrained[it.chain.id]?.let { date -> !date.isBefore(today.minusDays(1)) } == true }
            .map { it.chain.pattern }
            .toSet()
        val trainedToday = { progress: ChainProgress -> lastTrained[progress.chain.id] == today }

        return progressions
            // Training in a chain no longer followed still rests its movement pattern.
            .filter { it.chain.id !in dismissedChains && it.chain.id !in unfollowedChains && it.chain.pattern !in restingPatterns }
            .filter { it.chain.pattern != MovementPattern.MOBILITY || !trainedToday(it) }
            .sortedWith(compareBy(nullsFirst()) { lastTrained[it.chain.id] })
            .map { suggest(it, byCatalogId, historyByExercise) }
    }

    private fun suggest(
        progress: ChainProgress,
        byCatalogId: Map<String, Exercise>,
        historyByExercise: Map<Long, List<HistorySet>>,
    ): Suggestion {
        val next = progress.next
        if (progress.mastered && next != null) {
            val nextExercise = byCatalogId[next.id]
            return Suggestion(
                chain = progress.chain,
                step = next,
                exercise = nextExercise,
                target = next.working,
                reason = if (nextExercise != null) SuggestionReason.NEXT_STEP else SuggestionReason.NEXT_STEP_NOT_ADDED,
                lastSession = null,
                easier = null,
            )
        }
        val last = progress.lastSession
        val sessions = Progressions.sessions(
            historyByExercise[progress.exercise.id].orEmpty(),
            progress.exercise.laterality == "Unilateral",
        )
        val shortTwice = sessions.size >= 2 && sessions.takeLast(2).none { Progressions.meets(it, progress.step.working) }
        val steps = progress.chain.steps
        return Suggestion(
            chain = progress.chain,
            step = progress.step,
            exercise = progress.exercise,
            target = if (last == null) progress.step.working else nextTarget(last, progress.moveOn, progress.step.kind),
            reason = if (last == null) SuggestionReason.FIRST_SESSION else SuggestionReason.CONTINUE,
            lastSession = last,
            easier = if (shortTwice) steps.getOrNull(steps.indexOf(progress.step) - 1) else null,
        )
    }

    /**
     * Double progression (ADR 0005, decision 4): the weakest of the sets that count, plus one rep
     * or a few seconds, up to the move-on standard. Every set aims at the same value.
     */
    fun nextTarget(last: StepSession, moveOn: Standard, kind: ExerciseKind): Standard {
        val counted = last.values.take(moveOn.sets)
        val increment = if (kind == ExerciseKind.ISOMETRIC) HOLD_INCREMENT_SECONDS else 1
        val value = when {
            counted.isEmpty() -> 1
            // Fewer sets than the standard: complete every set at that level first.
            counted.size < moveOn.sets -> counted.min()
            else -> counted.min() + increment
        }
        return Standard(moveOn.sets, value.coerceIn(1, moveOn.value))
    }
}
