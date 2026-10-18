package io.github.gonbei774.calisthenicsmemory.data.v2

import java.time.LocalDate
import java.time.LocalTime

/**
 * One completed set as the workout editor shows it (ADR 0009). [ids] are the rows behind it:
 * a v2 set's entries, one per side, or one legacy record; empty for a set added in the editor.
 * Values are repetitions, or whole seconds for isometric exercises, as History shows them.
 * [value] is the right side of a unilateral set (or the only value of a bilateral one) and
 * [valueLeft] its left side; a unilateral set may hold either side alone, as it was recorded.
 */
data class DraftSet(
    val ids: List<Long> = emptyList(),
    val value: Int?,
    val valueLeft: Int? = null,
    val distanceCm: Int? = null,
    val weightG: Int? = null,
    val assistanceG: Int? = null,
)

/**
 * One exercise of a workout in the editor. [key] names an exercise that was loaded: its v2
 * occurrence id, or its exercise id in a legacy workout. It is null for one added in the editor.
 */
data class DraftExercise(
    val key: Long?,
    val exerciseId: Long,
    val unilateral: Boolean,
    val sets: List<DraftSet>,
)

/** A workout as the editor holds it: what was loaded, or what the user has made of it. */
data class WorkoutDraft(
    val date: LocalDate,
    /** The start, to the minute: the precision History shows and the editor enters. */
    val time: LocalTime,
    val comment: String,
    val exercises: List<DraftExercise>,
)

/** The saved workout an editor opens: a v2 session, or the legacy records of one save (ADR 0009). */
sealed interface WorkoutRef {
    data class V2(val sessionId: Long) : WorkoutRef

    data class Legacy(val date: String, val time: String, val comment: String) : WorkoutRef
}

/** What saving an edited workout did, or why it did nothing. */
enum class WorkoutSaveResult {
    /** The edits were written. */
    SAVED,

    /** The draft had no set left, so the workout was deleted. */
    DELETED,

    /** The draft equals what was loaded; nothing was written. */
    UNCHANGED,

    /** The workout changed or disappeared after it was loaded; nothing was written. */
    STALE,

    /** A legacy workout would land on another one's date, time and comment and merge with it. */
    TIME_TAKEN,
}

/**
 * The difference between a loaded workout and its edited draft, as the changes a save makes
 * (ADR 0009, decision 3). It is pure, and it rejects drafts that do not fit what was loaded.
 */
data class WorkoutEditPlan(
    /** No set is left: the whole workout goes. Every other field is then empty or false. */
    val deleteWorkout: Boolean,
    /** The date or the time changed. */
    val move: Boolean,
    val commentChanged: Boolean,
    /** Keys of loaded exercises that were removed or left without a set. */
    val removedExercises: List<Long>,
    /** Sets removed from exercises that stay. */
    val removedSets: List<DraftSet>,
    /** Sets whose values changed, as loaded to as edited; the ids are the same. */
    val changedSets: List<Pair<DraftSet, DraftSet>>,
    /** New sets for exercises that were loaded, by exercise key, in the order entered. */
    val addedSets: List<Pair<Long, DraftSet>>,
    /** Exercises added in the editor, each with at least one set. */
    val addedExercises: List<DraftExercise>,
) {
    val isEmpty: Boolean
        get() = !deleteWorkout && !move && !commentChanged && removedExercises.isEmpty() && removedSets.isEmpty() &&
            changedSets.isEmpty() && addedSets.isEmpty() && addedExercises.isEmpty()

    companion object {
        fun of(original: WorkoutDraft, edited: WorkoutDraft): WorkoutEditPlan {
            edited.exercises.forEach { exercise -> exercise.sets.forEach { validate(it, exercise.unilateral) } }
            val loaded = original.exercises.associateBy { requireNotNull(it.key) { "A loaded exercise has a key" } }
            require(loaded.size == original.exercises.size) { "Loaded exercise keys are unique" }
            val keys = edited.exercises.mapNotNull { it.key }
            require(keys.size == keys.distinct().size) { "An exercise appears once" }
            edited.exercises.forEach { exercise ->
                val key = exercise.key ?: return@forEach require(exercise.sets.all { it.ids.isEmpty() }) {
                    "A new exercise has only new sets"
                }
                val before = requireNotNull(loaded[key]) { "Unknown exercise $key" }
                require(before.exerciseId == exercise.exerciseId && before.unilateral == exercise.unilateral) {
                    "Exercise $key cannot change what it is"
                }
                val loadedIds = before.sets.map { it.ids }.toSet()
                exercise.sets.filter { it.ids.isNotEmpty() }.forEach {
                    require(it.ids in loadedIds) { "Set ${it.ids} is not a set of exercise $key" }
                }
                val ids = exercise.sets.filter { it.ids.isNotEmpty() }.map { it.ids }
                require(ids.size == ids.distinct().size) { "A set appears once" }
            }

            if (edited.exercises.none { it.sets.isNotEmpty() }) {
                return WorkoutEditPlan(true, false, false, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            }
            val kept = edited.exercises.filter { it.key != null && it.sets.isNotEmpty() }.associateBy { it.key!! }
            val removedExercises = original.exercises.map { it.key!! }.filter { it !in kept }
            val removedSets = mutableListOf<DraftSet>()
            val changedSets = mutableListOf<Pair<DraftSet, DraftSet>>()
            val addedSets = mutableListOf<Pair<Long, DraftSet>>()
            for ((key, exercise) in kept) {
                val before = loaded.getValue(key).sets
                val after = exercise.sets.filter { it.ids.isNotEmpty() }.associateBy { it.ids }
                removedSets += before.filter { it.ids !in after }
                changedSets += before.mapNotNull { set -> after[set.ids]?.takeIf { it != set }?.let { set to it } }
                addedSets += exercise.sets.filter { it.ids.isEmpty() }.map { key to it }
            }
            return WorkoutEditPlan(
                deleteWorkout = false,
                move = edited.date != original.date || edited.time != original.time,
                commentChanged = edited.comment != original.comment,
                removedExercises = removedExercises,
                removedSets = removedSets,
                changedSets = changedSets,
                addedSets = addedSets,
                addedExercises = edited.exercises.filter { it.key == null && it.sets.isNotEmpty() },
            )
        }

        private fun validate(set: DraftSet, unilateral: Boolean) {
            // A distance may be negative, as every distance input in the app allows.
            listOfNotNull(set.value, set.valueLeft, set.weightG, set.assistanceG).forEach {
                require(it >= 0) { "Values must not be negative" }
            }
            if (unilateral) {
                require(set.value != null || set.valueLeft != null) { "A set has a value" }
            } else {
                require(set.value != null && set.valueLeft == null) { "A bilateral set has one value" }
            }
        }
    }
}
