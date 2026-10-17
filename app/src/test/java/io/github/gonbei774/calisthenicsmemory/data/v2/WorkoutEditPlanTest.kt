package io.github.gonbei774.calisthenicsmemory.data.v2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class WorkoutEditPlanTest {
    private val pushUp = DraftExercise(
        key = 10, exerciseId = 1, unilateral = false,
        sets = listOf(DraftSet(listOf(100), 12), DraftSet(listOf(101), 10)),
    )
    private val lunge = DraftExercise(
        key = 11, exerciseId = 2, unilateral = true,
        sets = listOf(DraftSet(listOf(110, 111), 8, 7)),
    )
    private val loaded = WorkoutDraft(LocalDate.of(2026, 3, 2), LocalTime.of(7, 30), "Morning", listOf(pushUp, lunge))

    private fun plan(edited: WorkoutDraft) = WorkoutEditPlan.of(loaded, edited)

    @Test
    fun anUnchangedDraftPlansNothing() {
        assertTrue(plan(loaded).isEmpty)
    }

    @Test
    fun dateTimeAndCommentChangesAreSeparate() {
        val moved = plan(loaded.copy(date = loaded.date.plusDays(1)))
        assertTrue(moved.move)
        assertFalse(moved.commentChanged)
        assertTrue(plan(loaded.copy(time = LocalTime.of(7, 31))).move)
        val commented = plan(loaded.copy(comment = "Evening"))
        assertTrue(commented.commentChanged)
        assertFalse(commented.move)
    }

    @Test
    fun aChangedValueIsOneChangedSet() {
        val edited = loaded.copy(exercises = listOf(pushUp.copy(sets = listOf(DraftSet(listOf(100), 13), pushUp.sets[1])), lunge))
        val plan = plan(edited)
        assertEquals(listOf(pushUp.sets[0] to DraftSet(listOf(100), 13)), plan.changedSets)
        assertTrue(plan.removedSets.isEmpty() && plan.addedSets.isEmpty() && plan.removedExercises.isEmpty())
    }

    @Test
    fun metricsCountAsChanges() {
        val weighted = pushUp.sets[1].copy(weightG = 5_000)
        val plan = plan(loaded.copy(exercises = listOf(pushUp.copy(sets = listOf(pushUp.sets[0], weighted)), lunge)))
        assertEquals(listOf(pushUp.sets[1] to weighted), plan.changedSets)
    }

    @Test
    fun addedAndRemovedSetsBelongToTheirExercise() {
        val added = DraftSet(value = 9)
        val plan = plan(loaded.copy(exercises = listOf(pushUp.copy(sets = listOf(pushUp.sets[0], added)), lunge)))
        assertEquals(listOf(pushUp.sets[1]), plan.removedSets)
        assertEquals(listOf(10L to added), plan.addedSets)
        assertTrue(plan.changedSets.isEmpty())
    }

    @Test
    fun aUnilateralSetMayGainOrLoseASide() {
        val rightOnly = DraftSet(listOf(110, 111), 8, null)
        val plan = plan(loaded.copy(exercises = listOf(pushUp, lunge.copy(sets = listOf(rightOnly)))))
        assertEquals(listOf(lunge.sets[0] to rightOnly), plan.changedSets)
    }

    @Test
    fun anExerciseLeftWithoutSetsIsRemoved() {
        assertEquals(listOf(11L), plan(loaded.copy(exercises = listOf(pushUp, lunge.copy(sets = emptyList())))).removedExercises)
        assertEquals(listOf(11L), plan(loaded.copy(exercises = listOf(pushUp))).removedExercises)
    }

    @Test
    fun aNewExerciseNeedsSetsToBeAdded() {
        val plank = DraftExercise(null, 3, false, listOf(DraftSet(value = 60)))
        val plan = plan(loaded.copy(exercises = loaded.exercises + plank + plank.copy(sets = emptyList())))
        assertEquals(listOf(plank), plan.addedExercises)
    }

    @Test
    fun noSetLeftDeletesTheWorkoutAndNothingElse() {
        val plan = plan(loaded.copy(comment = "changed", exercises = listOf(pushUp.copy(sets = emptyList()))))
        assertTrue(plan.deleteWorkout)
        assertFalse(plan.isEmpty)
        assertFalse(plan.move || plan.commentChanged)
        assertTrue(plan.removedExercises.isEmpty() && plan.addedExercises.isEmpty())
    }

    @Test
    fun draftsThatDoNotFitWhatWasLoadedAreRejected() {
        fun rejects(edited: WorkoutDraft) {
            assertThrows(IllegalArgumentException::class.java) { plan(edited) }
        }
        // A negative value, a left value on a bilateral set, and a unilateral set without a value.
        rejects(loaded.copy(exercises = listOf(pushUp.copy(sets = listOf(DraftSet(value = -1))))))
        rejects(loaded.copy(exercises = listOf(pushUp.copy(sets = listOf(DraftSet(value = 5, valueLeft = 5))))))
        rejects(loaded.copy(exercises = listOf(lunge.copy(sets = listOf(DraftSet(value = null))))))
        rejects(loaded.copy(exercises = listOf(pushUp.copy(sets = listOf(DraftSet(value = 5, weightG = -1))))))
        // An unknown exercise key, a loaded exercise turned into another, and one listed twice.
        rejects(loaded.copy(exercises = listOf(pushUp.copy(key = 99))))
        rejects(loaded.copy(exercises = listOf(pushUp.copy(exerciseId = 3))))
        rejects(loaded.copy(exercises = listOf(pushUp, pushUp)))
        // A set moved to another exercise, a set listed twice, and a loaded set in a new exercise.
        rejects(loaded.copy(exercises = listOf(pushUp.copy(sets = lunge.sets.map { it.copy(valueLeft = null) }), lunge)))
        rejects(loaded.copy(exercises = listOf(pushUp.copy(sets = listOf(pushUp.sets[0], pushUp.sets[0])), lunge)))
        rejects(loaded.copy(exercises = loaded.exercises + DraftExercise(null, 1, false, listOf(pushUp.sets[0]))))
    }
}
