package io.github.gonbei774.calisthenicsmemory.data.progression

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueChain
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueStep
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Standard
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet

/** One session of a step: its sets, best first. Unilateral sets use the weaker side. */
data class StepSession(val date: String, val time: String, val values: List<Int>)

/** Where the user stands in one chain they follow (ADR 0005, decision 7). */
data class ChainProgress(
    val chain: CatalogueChain,
    /** The step being worked: the hardest one trained, or the easiest one in the library. */
    val step: CatalogueStep,
    val exercise: Exercise,
    /** The exercise's own target when set, which the user may have changed, else the step's. */
    val moveOn: Standard,
    /** The latest session of the current step, or null when it has not been trained. */
    val lastSession: StepSession?,
    /** How close the latest session came to the move-on standard, from 0 to 100. */
    val percent: Int,
    /** Some session of the current step met the move-on standard. */
    val mastered: Boolean,
    /** The step after the current one, in the library or not; null at the top of the chain. */
    val next: CatalogueStep?,
)

/**
 * The Progressions view as pure functions of the library, the history and the catalogue (ADR 0005).
 * A chain is followed when one of its steps is in the library, unless the user stopped following it.
 * Nothing here locks anything: it only describes where the user stands.
 */
object Progressions {

    fun chains(exercises: List<Exercise>, history: List<HistorySet>, unfollowed: Set<String> = emptySet()): List<ChainProgress> {
        val byCatalogId = exercises.filter { it.catalogId != null }.associateBy { it.catalogId!! }
        val historyByExercise = history.groupBy { it.exerciseId }
        return Catalogue.chains.filter { it.id !in unfollowed }.mapNotNull { chain ->
            val linked = chain.steps.filter { it.id in byCatalogId }
            if (linked.isEmpty()) return@mapNotNull null
            val sessionsByStep = linked.associateWith { step ->
                val exercise = byCatalogId.getValue(step.id)
                sessions(historyByExercise[exercise.id].orEmpty(), exercise.laterality == "Unilateral")
            }
            val step = linked.lastOrNull { sessionsByStep.getValue(it).isNotEmpty() } ?: linked.first()
            val exercise = byCatalogId.getValue(step.id)
            val moveOn = moveOnFor(exercise, step)
            val sessions = sessionsByStep.getValue(step)
            val last = sessions.lastOrNull()
            ChainProgress(
                chain = chain,
                step = step,
                exercise = exercise,
                moveOn = moveOn,
                lastSession = last,
                percent = last?.let { percentOf(it, moveOn) } ?: 0,
                mastered = sessions.any { meets(it, moveOn) },
                next = chain.steps.getOrNull(chain.steps.indexOf(step) + 1),
            )
        }
    }

    fun moveOnFor(exercise: Exercise, step: CatalogueStep): Standard {
        val sets = exercise.targetSets
        val value = exercise.targetValue
        return if (sets != null && value != null && sets > 0 && value > 0) Standard(sets, value) else step.moveOn
    }

    /** Sessions oldest first. Sets logged at the same date and time from one source are one session. */
    fun sessions(history: List<HistorySet>, unilateral: Boolean): List<StepSession> =
        history
            .groupBy { Triple(it.source, it.date, it.time) }
            .map { (key, sets) ->
                val values = sets.map { set ->
                    val left = set.valueLeft
                    if (unilateral && left != null) minOf(set.valueRight, left) else set.valueRight
                }
                StepSession(key.second, key.third, values.sortedDescending())
            }
            .sortedWith(compareBy({ it.date }, { it.time }))

    /** The move-on standard is met when enough sets each reach its value. */
    fun meets(session: StepSession, standard: Standard): Boolean =
        session.values.count { it >= standard.value } >= standard.sets

    /** The best sets, each counted up to the standard's value, as a share of the standard. */
    fun percentOf(session: StepSession, standard: Standard): Int {
        val total = standard.sets * standard.value
        if (total <= 0) return 0
        val done = session.values.take(standard.sets).sumOf { minOf(it, standard.value) }
        return done * 100 / total
    }
}
