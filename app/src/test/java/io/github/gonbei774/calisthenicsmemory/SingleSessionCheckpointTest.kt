package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.ui.screens.SingleSessionCheckpoint
import io.github.gonbei774.calisthenicsmemory.ui.screens.WorkoutSession
import io.github.gonbei774.calisthenicsmemory.ui.screens.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SingleSessionCheckpointTest {
    @get:Rule val folder = TemporaryFolder()

    private val exercise = Exercise(id = 5, name = "Squat", type = "Dynamic", laterality = "Unilateral")
    private fun session() = WorkoutSession(
        exercise = exercise, totalSets = 2, targetValue = 12, repDuration = 3, startInterval = 5, intervalDuration = 60,
        sets = mutableListOf(
            WorkoutSet(1, "Right", 12, actualValue = 12, isCompleted = true, weightG = 10_000),
            WorkoutSet(1, "Left", 12),
        ),
        comment = "Workout", isAutoMode = false, isDynamicCountSoundEnabled = true,
    )

    @Test fun `round trips through the file and rebuilds an equal session`() {
        val original = session()
        val checkpoint = SingleSessionCheckpoint.of(original, currentSetIndex = 1, atConfirmation = false, fromToDo = true, savedAtWallMillis = 42)
        val file = SingleSessionCheckpoint.file(folder.root)
        file.save(checkpoint)

        val loaded = file.load()!!
        assertEquals(checkpoint, loaded)
        assertEquals(original, loaded.toSession(exercise))
    }

    @Test fun `the checkpoint does not change when the live session is mutated later`() {
        val live = session()
        val checkpoint = SingleSessionCheckpoint.of(live, 1, false, false, 0)
        live.sets[1].actualValue = 11
        live.sets[1].isCompleted = true
        assertEquals(0, checkpoint.sets[1].actualValue)
    }

    @Test fun `other versions and broken files load as null`() {
        val file = SingleSessionCheckpoint.file(folder.root)
        assertNull(file.load())
        File(folder.root, SingleSessionCheckpoint.FILE_NAME).writeText("{\"version\":2}")
        assertNull(file.load())
    }

    @Test fun `start and set times round trip, and older files without a start still load`() {
        val live = session().apply {
            startedAtWallMillis = 1_000
            sets[0].completedAtWallMillis = 2_000
        }
        val file = SingleSessionCheckpoint.file(folder.root)
        file.save(SingleSessionCheckpoint.of(live, 1, false, false, 3_000))
        val restored = file.load()!!.toSession(exercise)
        assertEquals(1_000L, restored.startedAtWallMillis)
        assertEquals(2_000L, restored.sets[0].completedAtWallMillis)

        val saved = File(folder.root, SingleSessionCheckpoint.FILE_NAME)
        saved.writeText(saved.readText().replace(Regex(",\\s*\"startedAtWallMillis\"\\s*:\\s*1000"), ""))
        assertNull(file.load()!!.startedAtWallMillis)
    }
}
