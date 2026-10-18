package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.DraftExercise
import io.github.gonbei774.calisthenicsmemory.data.v2.DraftSet
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutDraft
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutEditPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class WorkoutEditorModelTest {
    private val library = listOf(
        Exercise(1, "Push-up", "Dynamic", weightTrackingEnabled = true),
        Exercise(2, "Lunge", "Dynamic", laterality = "Unilateral"),
        Exercise(3, "Plank", "Isometric"),
    )
    private val draft = WorkoutDraft(
        LocalDate.of(2026, 3, 2), LocalTime.of(7, 30), "Morning",
        listOf(
            DraftExercise(10, 1, false, listOf(DraftSet(listOf(100), 12, weightG = 2_550), DraftSet(listOf(101), 10, distanceCm = -5))),
            DraftExercise(11, 2, true, listOf(DraftSet(listOf(110, 111), 8, 7), DraftSet(listOf(112), null, 6))),
        ),
    )
    private val state = WorkoutEditorModel.from(draft, library)

    @Test fun `an untouched workout saves back exactly as it was loaded`() {
        assertEquals(draft, WorkoutEditorModel.toDraft(state))
        assertTrue(WorkoutEditPlan.of(draft, WorkoutEditorModel.toDraft(state)!!).isEmpty)
    }

    @Test fun `loads show in exact kilograms and read back to the same grams`() {
        assertEquals("2.55", state.exercises[0].sets[0].weightKg)
        assertEquals("2.5", WorkoutEditorModel.kilograms(2_500))
        assertEquals("10", WorkoutEditorModel.kilograms(10_000))
        assertEquals("0.005", WorkoutEditorModel.kilograms(5))
        assertEquals(2_550, WorkoutEditorModel.grams("2,55"))
        assertEquals(12_000, WorkoutEditorModel.grams("12"))
        assertNull(WorkoutEditorModel.grams("2.5555"))
        assertNull(WorkoutEditorModel.grams("-1"))
        assertNull(WorkoutEditorModel.grams("kg"))
    }

    @Test fun `fields show for what the exercise tracks and for values already saved`() {
        val pushUp = state.exercises[0]
        assertTrue(pushUp.showWeight)
        // Distance is not tracked, but a saved set has one, so it stays visible and editable.
        assertTrue(pushUp.showDistance)
        assertFalse(pushUp.showAssistance)
        assertEquals("Push-up", pushUp.name)
        assertFalse(state.exercises[1].showDistance)
    }

    @Test fun `a new set starts empty and blocks saving until its value is entered`() {
        val added = WorkoutEditorModel.withNewSet(state, 0)
        assertEquals(EditorSet(), added.exercises[0].sets.last())
        assertNull(WorkoutEditorModel.toDraft(added))
        val filled = WorkoutEditorModel.withSet(added, 0, 2, EditorSet(value = "9"))
        assertEquals(DraftSet(value = 9), WorkoutEditorModel.toDraft(filled)!!.exercises[0].sets.last())
    }

    @Test fun `a unilateral set needs one side, and a legacy one its right side`() {
        val leftOnly = WorkoutEditorModel.withSet(state, 1, 0, EditorSet(listOf(110, 111), value = "", valueLeft = "7"))
        assertEquals(DraftSet(listOf(110, 111), null, 7), WorkoutEditorModel.toDraft(leftOnly)!!.exercises[1].sets[0])
        assertNull(WorkoutEditorModel.toDraft(leftOnly.copy(legacy = true)))
        val neither = WorkoutEditorModel.withSet(state, 1, 0, EditorSet(listOf(110, 111)))
        assertNull(WorkoutEditorModel.toDraft(neither))
    }

    @Test fun `bad values block saving`() {
        fun with(set: EditorSet) = WorkoutEditorModel.toDraft(WorkoutEditorModel.withSet(state, 0, 0, set))
        assertNull(with(EditorSet(value = "-1")))
        assertNull(with(EditorSet(value = "x")))
        assertNull(with(EditorSet(value = "5", weightKg = "1.2345")))
        assertNull(with(EditorSet(value = "5", distanceCm = "5.5")))
        assertEquals(-10, with(EditorSet(value = "5", distanceCm = "-10"))!!.exercises[0].sets[0].distanceCm)
    }

    @Test fun `exercises are added from the library with one empty set and removed whole`() {
        val withPlank = WorkoutEditorModel.withExercise(state, library[2])
        val plank = withPlank.exercises.last()
        assertNull(plank.key)
        assertFalse(plank.dynamic)
        assertEquals(listOf(EditorSet()), plank.sets)
        val filled = WorkoutEditorModel.withSet(withPlank, 2, 0, EditorSet(value = "60"))
        assertEquals(DraftExercise(null, 3, false, listOf(DraftSet(value = 60))), WorkoutEditorModel.toDraft(filled)!!.exercises.last())

        val withoutLunge = WorkoutEditorModel.withoutExercise(state, 1)
        assertEquals(listOf(10L), WorkoutEditorModel.toDraft(withoutLunge)!!.exercises.map { it.key })
        val withoutSet = WorkoutEditorModel.withoutSet(state, 0, 0)
        assertEquals(listOf(listOf(101L)), WorkoutEditorModel.toDraft(withoutSet)!!.exercises[0].sets.map { it.ids })
    }
}
