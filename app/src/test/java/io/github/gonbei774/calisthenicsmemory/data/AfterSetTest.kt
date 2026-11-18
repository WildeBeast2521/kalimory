package io.github.gonbei774.calisthenicsmemory.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Where a program goes after a set, for every timer mode alike. */
class AfterSetTest {
    private fun set(n: Int, rest: Int = 0, done: Boolean = false, loopRest: Int = 0, skipped: Boolean = false) = ProgramWorkoutSet(
        exerciseIndex = 0, setNumber = n, side = null, targetValue = 10, intervalSeconds = rest,
        isCompleted = done, loopRestAfterSeconds = loopRest, isSkipped = skipped,
    )

    @Test fun `the next set runs at once with no countdown`() {
        val r = afterSet(listOf(set(1), set(2)), 0, redoMode = false, startCountdownSeconds = 0)
        assertEquals(AfterSet(AfterSet.Kind.RUN, 1, false), r)
    }

    @Test fun `the countdown is not repeated between sets`() {
        val r = afterSet(listOf(set(1), set(2)), 0, redoMode = false, startCountdownSeconds = 5)
        assertEquals(AfterSet(AfterSet.Kind.RUN, 1, false), r)
    }

    @Test fun `after a rest the next set starts at once`() {
        val sets = listOf(set(1, rest = 30), set(2))
        assertEquals(AfterSet(AfterSet.Kind.RUN, 1, false), afterSet(sets, 0, false, 5, restDone = true))
    }

    @Test fun `redo moves on with a countdown, being a jump`() {
        val sets = listOf(set(1), set(2, done = true), set(3))
        assertEquals(AfterSet(AfterSet.Kind.COUNTDOWN, 2, false), afterSet(sets, 0, true, 5))
        assertEquals(AfterSet(AfterSet.Kind.RUN, 2, false), afterSet(sets, 0, true, 0))
    }

    @Test fun `a rest comes first and keeps redo mode`() {
        assertEquals(AfterSet(AfterSet.Kind.REST, 0, false), afterSet(listOf(set(1, rest = 30), set(2)), 0, false, 5))
        assertEquals(AfterSet(AfterSet.Kind.REST, 0, true), afterSet(listOf(set(1, rest = 30), set(2)), 0, true, 5))
    }

    @Test fun `a rest between rounds counts as a rest`() {
        assertEquals(AfterSet.Kind.REST, afterSet(listOf(set(1, loopRest = 60), set(2)), 0, false, 0).kind)
    }

    @Test fun `the last set ends the program and redo mode`() {
        assertEquals(AfterSet(AfterSet.Kind.RESULT, 1, false), afterSet(listOf(set(1), set(2)), 1, true, 5))
    }

    @Test fun `redo skips sets already done and ends redo mode`() {
        val sets = listOf(set(1), set(2, done = true), set(3, done = true), set(4))
        assertEquals(AfterSet(AfterSet.Kind.RUN, 3, false), afterSet(sets, 0, true, 0))
    }

    @Test fun `redo with nothing left to do ends the program`() {
        val sets = listOf(set(1), set(2, done = true), set(3, done = true))
        assertEquals(AfterSet(AfterSet.Kind.RESULT, 0, false), afterSet(sets, 0, true, 5))
    }

    @Test fun `a skipped set has no rest of its own, and the countdown shows`() {
        val sets = listOf(set(1, rest = 30, skipped = true), set(2))
        assertEquals(AfterSet(AfterSet.Kind.COUNTDOWN, 1, false), afterSet(sets, 0, false, 5))
        assertEquals(AfterSet(AfterSet.Kind.RUN, 1, false), afterSet(sets, 0, false, 0))
    }

    @Test fun `a skipped set that ends a round keeps the rest between rounds`() {
        val sets = listOf(set(1, rest = 30, loopRest = 60, skipped = true), set(2))
        assertEquals(AfterSet(AfterSet.Kind.REST, 0, false), afterSet(sets, 0, false, 5))
    }

    @Test fun `the next set is the first one still to do, skipped or done sets are passed over`() {
        val sets = listOf(set(1), set(2, skipped = true), set(3, done = true), set(4))
        assertEquals(AfterSet(AfterSet.Kind.RUN, 3, false), afterSet(sets, 0, false, 0))
    }

    @Test fun `redo passes over skipped sets`() {
        val sets = listOf(set(1), set(2, skipped = true), set(3))
        assertEquals(AfterSet(AfterSet.Kind.COUNTDOWN, 2, false), afterSet(sets, 0, true, 5))
    }

    @Test fun `nothing left to do ends the program, even before the last set`() {
        val sets = listOf(set(1, rest = 30), set(2, done = true), set(3, skipped = true))
        assertEquals(AfterSet(AfterSet.Kind.RESULT, 0, false), afterSet(sets, 0, false, 5))
    }

    // One-sided sets: Right then Left of the same set, the rest after the second side only.
    private fun side(n: Int, side: String, rest: Int = 0, done: Boolean = false, skipped: Boolean = false, round: Int = 1) = ProgramWorkoutSet(
        exerciseIndex = 0, setNumber = n, side = side, targetValue = 10, intervalSeconds = rest,
        isCompleted = done, isSkipped = skipped, roundNumber = round,
    )

    @Test fun `after the first side the countdown is the pause and no rest comes`() {
        val sets = listOf(side(1, "Right", rest = 30), side(1, "Left", rest = 30))
        assertEquals(AfterSet(AfterSet.Kind.COUNTDOWN, 1, false), afterSet(sets, 0, false, 5))
        assertEquals(AfterSet(AfterSet.Kind.RUN, 1, false), afterSet(sets, 0, false, 0))
    }

    @Test fun `after the second side the rest comes`() {
        val sets = listOf(side(1, "Right", rest = 30), side(1, "Left", rest = 30), set(2))
        assertEquals(AfterSet(AfterSet.Kind.REST, 1, false), afterSet(sets, 1, false, 5))
    }

    @Test fun `a side redone alone is not a switch and rests as usual`() {
        val sets = listOf(side(1, "Right", rest = 30), side(1, "Left", rest = 30, done = true), set(2))
        assertEquals(AfterSet(AfterSet.Kind.REST, 0, true), afterSet(sets, 0, true, 5))
    }

    @Test fun `redo keeps going through the switch`() {
        val sets = listOf(side(1, "Right", rest = 30), side(1, "Left", rest = 30), set(2, done = true))
        assertEquals(AfterSet(AfterSet.Kind.COUNTDOWN, 1, true), afterSet(sets, 0, true, 5))
    }

    @Test fun `a skipped first side still goes to the other side`() {
        val sets = listOf(side(1, "Right", rest = 30, skipped = true), side(1, "Left"))
        assertEquals(AfterSet(AfterSet.Kind.COUNTDOWN, 1, false), afterSet(sets, 0, false, 5))
    }

    @Test fun `sides of different sets are not a switch`() {
        val sets = listOf(side(1, "Left", rest = 30), side(2, "Right", rest = 30))
        assertEquals(AfterSet(AfterSet.Kind.REST, 0, false), afterSet(sets, 0, false, 5))
        val rounds = listOf(side(1, "Left", rest = 30, round = 1), side(1, "Right", rest = 30, round = 2))
        assertEquals(AfterSet.Kind.REST, afterSet(rounds, 0, false, 5).kind)
    }
}
