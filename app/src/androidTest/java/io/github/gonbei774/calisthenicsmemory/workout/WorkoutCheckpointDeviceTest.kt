package io.github.gonbei774.calisthenicsmemory.workout

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** The checkpoint pieces that depend on Android: file system, monotonic clock, and boot count. */
@RunWith(AndroidJUnit4::class)
class WorkoutCheckpointDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val file = File(context.filesDir, "workout-checkpoint-test.json")

    @After
    fun cleanUp() {
        file.delete()
        File(file.path + ".tmp").delete()
    }

    @Test
    fun checkpointRoundTripsThroughAppStorage() {
        val clock = MonotonicClock.SYSTEM
        val start = clock.nowMillis()
        val state = WorkoutReducer.reduce(
            WorkoutState(listOf(WorkoutStep("rest", StepKind.Countdown(60_000)))),
            WorkoutEvent.Start(start),
        ).state
        val checkpoint = WorkoutCheckpoint(
            state = state,
            savedAtMonotonicMillis = clock.nowMillis(),
            savedAtWallMillis = System.currentTimeMillis(),
            bootCount = currentBootCount(context),
        )
        assertNotNull("BOOT_COUNT should be available on API 24+", checkpoint.bootCount)

        WorkoutCheckpointStore(file).save(checkpoint)
        val loaded = WorkoutCheckpointStore(file).load()
        assertEquals(WorkoutCheckpointStore.LoadResult.Found(checkpoint), loaded)

        val recovered = WorkoutRecovery.recover(checkpoint, clock.nowMillis(), System.currentTimeMillis(), currentBootCount(context))
        assertEquals(RecoveryTiming.EXACT, recovered.timing)
        assertTrue(clock.nowMillis() >= start)
    }
}
