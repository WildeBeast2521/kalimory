package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.data.ProgramSessionCheckpoint
import io.github.gonbei774.calisthenicsmemory.data.ProgramSessionCheckpointStore
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProgramSessionCheckpointStoreTest {
    @get:Rule val folder = TemporaryFolder()
    private val file get() = File(folder.root, ProgramSessionCheckpointStore.FILE_NAME)
    private val store get() = ProgramSessionCheckpointStore(file)

    private val checkpoint = ProgramSessionCheckpoint(
        programId = 3,
        currentSetIndex = 1,
        atResult = false,
        sets = listOf(
            ProgramWorkoutSet(0, 1, null, targetValue = 10, actualValue = 9, isCompleted = true, intervalSeconds = 60, weightG = 5_000),
            ProgramWorkoutSet(0, 2, "Left", targetValue = 10, intervalSeconds = 60, loopId = 4, roundNumber = 2, totalRounds = 3),
        ),
        comment = "【Program】Test",
        savedAtWallMillis = 1_000,
    )

    @Test fun `round trips every set field`() {
        store.save(checkpoint)
        assertEquals(checkpoint, store.load())
        assertFalse(File(file.path + ".tmp").exists())
    }

    @Test fun `missing, unreadable, and newer checkpoints load as null and are left in place`() {
        assertNull(store.load())
        file.writeText("{broken")
        assertNull(store.load())
        assertEquals("{broken", file.readText())
        store.save(checkpoint)
        file.writeText(file.readText().replace("\"version\":1", "\"version\":2"))
        assertNull(store.load())
    }

    @Test fun `clear removes the checkpoint`() {
        store.save(checkpoint)
        store.clear()
        assertNull(store.load())
    }
}
