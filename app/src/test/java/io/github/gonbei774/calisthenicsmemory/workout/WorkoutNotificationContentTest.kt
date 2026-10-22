package io.github.gonbei774.calisthenicsmemory.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What the workout notification says for each kind of step. */
class WorkoutNotificationContentTest {
    private val labels = object : WorkoutNotificationLabels {
        override fun phase(phase: WorkoutPhase) = phase.name.lowercase()
        override fun reps(done: Int, target: Int) = "$done / $target reps"
        override val paused = "Paused"
        override fun pausedWithTimeLeft(time: String) = "Paused, $time left"
        override fun time(millis: Long) = "${(millis + 999) / 1_000}s"
    }

    @Test fun `a running rest shows its countdown end and the progress`() {
        val content = WorkoutNotificationContent.of(
            WorkoutProgress("Push-up", "Set 2 of 4", WorkoutPhase.REST, countdownEndsAtMillis = 90_000, stepsDone = 1, stepsTotal = 4),
            labels,
        )
        assertEquals("Push-up", content.title)
        assertEquals("rest", content.text)
        assertEquals("Set 2 of 4", content.subText)
        assertEquals(90_000L, content.countdownEndsAtMillis)
        assertEquals(1 to 4, content.progressDone to content.progressTotal)
    }

    @Test fun `reps show the count against the goal and no timer`() {
        val content = WorkoutNotificationContent.of(
            WorkoutProgress("Push-up", "Set 1 of 3", WorkoutPhase.REPS, repsDone = 7, repsTarget = 12, stepsTotal = 3),
            labels,
        )
        assertEquals("7 / 12 reps", content.text)
        assertNull(content.countdownEndsAtMillis)
    }

    @Test fun `a paused countdown shows the time left instead of a ticking timer`() {
        val content = WorkoutNotificationContent.of(
            WorkoutProgress("Plank", "Set 1 of 2", WorkoutPhase.HOLD, countdownEndsAtMillis = 5_000, pausedRemainingMillis = 41_200, paused = true),
            labels,
        )
        assertEquals("Paused, 42s left", content.text)
        assertNull(content.countdownEndsAtMillis)
    }

    @Test fun `paused reps keep their count`() {
        val content = WorkoutNotificationContent.of(
            WorkoutProgress("Push-up", "Set 1 of 3", WorkoutPhase.REPS, repsDone = 4, repsTarget = 10, paused = true),
            labels,
        )
        assertEquals("Paused · 4 / 10 reps", content.text)
    }

    @Test fun `progress never runs past its total`() {
        val content = WorkoutNotificationContent.of(
            WorkoutProgress("Push-up", "", WorkoutPhase.GET_READY, stepsDone = 9, stepsTotal = 4),
            labels,
        )
        assertEquals(4 to 4, content.progressDone to content.progressTotal)
    }
}
