package io.github.gonbei774.calisthenicsmemory.ui.components

import io.github.gonbei774.calisthenicsmemory.workout.MonotonicClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepStopwatchTest {
    private var now = 1_000L
    private val clock = MonotonicClock { now }

    @Test fun `counts elapsed time from the clock, not from ticks`() {
        val stopwatch = StepStopwatch(clock)
        now += 2_500
        stopwatch.tick()
        assertEquals(2_500, stopwatch.elapsedMillis)
        assertEquals(2, stopwatch.elapsedSeconds)
        now += 60_000 // one late tick
        stopwatch.tick()
        assertEquals(62, stopwatch.elapsedSeconds)
    }

    @Test fun `pauses keep partial seconds and do not elapse`() {
        val stopwatch = StepStopwatch(clock)
        now += 1_400; stopwatch.setPaused(true)
        now += 30_000; stopwatch.tick()
        assertTrue(stopwatch.isPaused)
        assertEquals(1_400, stopwatch.elapsedMillis)
        stopwatch.setPaused(false)
        now += 700; stopwatch.tick()
        assertEquals(2_100, stopwatch.elapsedMillis)
        assertEquals(2, stopwatch.elapsedSeconds)
    }

    @Test fun `repeated pause or resume changes nothing`() {
        val stopwatch = StepStopwatch(clock)
        now += 1_000; stopwatch.setPaused(true)
        now += 1_000; stopwatch.setPaused(true)
        stopwatch.setPaused(false); stopwatch.setPaused(false)
        now += 500; stopwatch.tick()
        assertEquals(1_500, stopwatch.elapsedMillis)
        assertFalse(stopwatch.isPaused)
    }

    @Test fun `can start paused`() {
        val stopwatch = StepStopwatch(clock, startPaused = true)
        now += 10_000; stopwatch.tick()
        assertEquals(0, stopwatch.elapsedMillis)
    }

    @Test fun `countdown seconds round up like the old display`() {
        assertEquals(5, countdownSeconds(5_000))
        assertEquals(5, countdownSeconds(4_001))
        assertEquals(1, countdownSeconds(1))
        assertEquals(0, countdownSeconds(0))
        assertEquals(0, countdownSeconds(-50))
    }
}
