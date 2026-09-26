package io.github.gonbei774.calisthenicsmemory.data

import io.github.gonbei774.calisthenicsmemory.workout.JsonCheckpointFile
import kotlinx.serialization.Serializable
import java.io.File
import java.io.IOException

/**
 * An automatic checkpoint of a running program workout, kept apart from the
 * user's explicit "Save & Exit" slot ([SavedWorkoutState]) so neither overwrites
 * the other. It survives process death; a resume restarts the interrupted set.
 */
@Serializable
data class ProgramSessionCheckpoint(
    val version: Int = CURRENT_VERSION,
    val programId: Long,
    val currentSetIndex: Int,
    /** True once the result screen was reached, so finished sets are not lost before they are recorded. */
    val atResult: Boolean,
    val sets: List<ProgramWorkoutSet>,
    val comment: String,
    val savedAtWallMillis: Long,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

class ProgramSessionCheckpointStore(file: File) {
    private val checkpoint = JsonCheckpointFile(file, ProgramSessionCheckpoint.serializer(), ProgramSessionCheckpoint.CURRENT_VERSION)

    @Throws(IOException::class)
    fun save(value: ProgramSessionCheckpoint) = checkpoint.save(value)

    /** The checkpoint, or null when there is none or it cannot be used; an unusable file is left in place. */
    fun load(): ProgramSessionCheckpoint? = checkpoint.load()

    fun clear() = checkpoint.clear()

    companion object {
        const val FILE_NAME = "program-session-checkpoint.json"
    }
}
