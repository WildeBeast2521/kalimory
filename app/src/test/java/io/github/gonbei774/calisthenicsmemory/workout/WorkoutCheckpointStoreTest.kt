package io.github.gonbei774.calisthenicsmemory.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WorkoutCheckpointStoreTest {
    @get:Rule val folder = TemporaryFolder()

    private val file get() = File(folder.root, "workout-checkpoint.json")
    private val store get() = WorkoutCheckpointStore(file)

    private val state = WorkoutReducer.reduce(
        WorkoutState(listOf(WorkoutStep("set", StepKind.Stopwatch), WorkoutStep("rest", StepKind.Countdown(60_000)))),
        WorkoutEvent.Start(1_000),
    ).state
    private val checkpoint = WorkoutCheckpoint(state = state, savedAtMonotonicMillis = 2_000, savedAtWallMillis = 3_000, bootCount = 4)

    @Test fun `missing file is reported as missing`() {
        assertEquals(WorkoutCheckpointStore.LoadResult.Missing, store.load())
    }

    @Test fun `saves and loads the exact checkpoint with an explicit version`() {
        store.save(checkpoint)
        assertEquals(WorkoutCheckpointStore.LoadResult.Found(checkpoint), store.load())
        assertTrue(file.readText().contains("\"version\":1"))
        assertFalse(File(file.path + ".tmp").exists())

        val later = checkpoint.copy(savedAtMonotonicMillis = 9_000)
        store.save(later)
        assertEquals(WorkoutCheckpointStore.LoadResult.Found(later), store.load())
    }

    @Test fun `a newer version is refused, not misread`() {
        store.save(checkpoint)
        file.writeText(file.readText().replace("\"version\":1", "\"version\":2"))
        assertEquals(WorkoutCheckpointStore.LoadResult.UnsupportedVersion(2), store.load())
    }

    @Test fun `garbage is reported as unreadable and left in place`() {
        file.writeText("{not json")
        assertTrue(store.load() is WorkoutCheckpointStore.LoadResult.Unreadable)
        assertEquals("{not json", file.readText())
        file.writeText("""{"version":1,"state":{"steps":[]},"savedAtMonotonicMillis":0,"savedAtWallMillis":0,"bootCount":null}""")
        assertTrue(store.load() is WorkoutCheckpointStore.LoadResult.Unreadable)
    }

    @Test fun `clear removes the checkpoint`() {
        store.save(checkpoint)
        store.clear()
        assertEquals(WorkoutCheckpointStore.LoadResult.Missing, store.load())
    }
}
