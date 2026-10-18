package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.DraftExercise
import io.github.gonbei774.calisthenicsmemory.data.v2.DraftSet
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutDraft
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/**
 * One set as the workout editor's fields hold it (ADR 0009). The values are text, so a field can be
 * empty while it is typed; [ids] are the saved rows behind the set, empty for a set added here.
 * Weight and assistance are kilograms, written exactly ("2.5", "2.55"), so an untouched value saves
 * back to the same grams.
 */
data class EditorSet(
    val ids: List<Long> = emptyList(),
    val value: String = "",
    val valueLeft: String = "",
    val distanceCm: String = "",
    val weightKg: String = "",
    val assistanceKg: String = "",
)

/** One exercise in the editor, with what its fields need from the library. */
data class EditorExercise(
    val key: Long?,
    val exerciseId: Long,
    val name: String,
    val unilateral: Boolean,
    val dynamic: Boolean,
    val showDistance: Boolean,
    val showWeight: Boolean,
    val showAssistance: Boolean,
    val sets: List<EditorSet>,
)

data class EditorState(
    val date: LocalDate,
    val time: LocalTime,
    val comment: String,
    val exercises: List<EditorExercise>,
    /** A legacy workout stores each set as one record, which always has a right value. */
    val legacy: Boolean = false,
)

/** Converts between a [WorkoutDraft] and the editor's fields, and makes the editor's changes. All pure. */
object WorkoutEditorModel {

    fun from(draft: WorkoutDraft, library: List<Exercise>, legacy: Boolean = false): EditorState {
        val byId = library.associateBy { it.id }
        return EditorState(
            date = draft.date,
            time = draft.time,
            comment = draft.comment,
            exercises = draft.exercises.map { exercise ->
                val sets = exercise.sets.map { set ->
                    EditorSet(
                        ids = set.ids,
                        value = set.value?.toString().orEmpty(),
                        valueLeft = set.valueLeft?.toString().orEmpty(),
                        distanceCm = set.distanceCm?.toString().orEmpty(),
                        weightKg = set.weightG?.let(::kilograms).orEmpty(),
                        assistanceKg = set.assistanceG?.let(::kilograms).orEmpty(),
                    )
                }
                editorExercise(exercise.key, exercise.exerciseId, exercise.unilateral, byId[exercise.exerciseId], sets)
            },
            legacy = legacy,
        )
    }

    /** A new exercise from the library, with one empty set to fill in. */
    fun withExercise(state: EditorState, exercise: Exercise): EditorState =
        state.copy(
            exercises = state.exercises + editorExercise(
                key = null, exerciseId = exercise.id, unilateral = exercise.laterality == "Unilateral",
                library = exercise, sets = listOf(EditorSet()),
            ),
        )

    fun withoutExercise(state: EditorState, index: Int): EditorState =
        state.copy(exercises = state.exercises.filterIndexed { i, _ -> i != index })

    /** An empty set at the end of the exercise: nothing is filled in for the user. */
    fun withNewSet(state: EditorState, exerciseIndex: Int): EditorState =
        withSets(state, exerciseIndex) { it + EditorSet() }

    fun withoutSet(state: EditorState, exerciseIndex: Int, setIndex: Int): EditorState =
        withSets(state, exerciseIndex) { sets -> sets.filterIndexed { i, _ -> i != setIndex } }

    fun withSet(state: EditorState, exerciseIndex: Int, setIndex: Int, set: EditorSet): EditorState =
        withSets(state, exerciseIndex) { sets -> sets.mapIndexed { i, old -> if (i == setIndex) set else old } }

    private fun withSets(state: EditorState, exerciseIndex: Int, change: (List<EditorSet>) -> List<EditorSet>) =
        state.copy(exercises = state.exercises.mapIndexed { i, e -> if (i == exerciseIndex) e.copy(sets = change(e.sets)) else e })

    /**
     * The draft the fields describe, or null while a field holds something that is not a value: an
     * empty count, or a weight with more than gram precision. Exercises keep their sets in order.
     */
    fun toDraft(state: EditorState): WorkoutDraft? {
        val exercises = state.exercises.map { exercise ->
            val sets = exercise.sets.map { set -> draftSet(set, exercise.unilateral, state.legacy) ?: return null }
            DraftExercise(exercise.key, exercise.exerciseId, exercise.unilateral, sets)
        }
        return WorkoutDraft(state.date, state.time, state.comment, exercises)
    }

    private fun draftSet(set: EditorSet, unilateral: Boolean, legacy: Boolean): DraftSet? {
        val value = count(set.value) ?: return null
        val left = count(set.valueLeft) ?: return null
        if (unilateral && !legacy) {
            if (value.value == null && left.value == null) return null
        } else if (value.value == null) return null
        val distance = optionalInt(set.distanceCm) ?: return null
        val weight = optionalGrams(set.weightKg) ?: return null
        val assistance = optionalGrams(set.assistanceKg) ?: return null
        return DraftSet(
            ids = set.ids,
            value = value.value,
            valueLeft = if (unilateral) left.value else null,
            distanceCm = distance.value,
            weightG = weight.value,
            assistanceG = assistance.value,
        )
    }

    /** A field's value; [value] is null for an empty field. The wrapper itself is null for text that is no value. */
    private class Field(val value: Int?)

    private fun count(text: String): Field? =
        if (text.isBlank()) Field(null) else text.trim().toIntOrNull()?.takeIf { it >= 0 }?.let(::Field)

    // A distance may be negative, as every other distance input allows.
    private fun optionalInt(text: String): Field? =
        if (text.isBlank()) Field(null) else text.trim().toIntOrNull()?.let(::Field)

    private fun optionalGrams(text: String): Field? = if (text.isBlank()) Field(null) else grams(text)?.let(::Field)

    /** Grams as kilograms, exactly: 2500 is "2.5", 2550 is "2.55", 10000 is "10". */
    fun kilograms(grams: Int): String = BigDecimal(grams).movePointLeft(3).stripTrailingZeros().toPlainString()

    /** Kilograms typed with a point or a comma, to whole grams; null when it is not that. */
    fun grams(kilograms: String): Int? {
        val number = kilograms.trim().replace(',', '.').toBigDecimalOrNull() ?: return null
        if (number.signum() < 0) return null
        val grams = number.movePointRight(3)
        return runCatching { grams.intValueExact() }.getOrNull()
    }

    private fun editorExercise(key: Long?, exerciseId: Long, unilateral: Boolean, library: Exercise?, sets: List<EditorSet>) =
        EditorExercise(
            key = key,
            exerciseId = exerciseId,
            name = library?.name.orEmpty(),
            unilateral = unilateral,
            dynamic = library?.type != "Isometric",
            // A field shows when the exercise tracks it or a set already holds a value for it.
            showDistance = library?.distanceTrackingEnabled == true || sets.any { it.distanceCm.isNotEmpty() },
            showWeight = library?.weightTrackingEnabled == true || sets.any { it.weightKg.isNotEmpty() },
            showAssistance = library?.assistanceTrackingEnabled == true || sets.any { it.assistanceKg.isNotEmpty() },
            sets = sets,
        )
}
