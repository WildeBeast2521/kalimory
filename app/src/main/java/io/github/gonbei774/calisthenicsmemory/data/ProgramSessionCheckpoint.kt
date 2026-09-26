package io.github.gonbei774.calisthenicsmemory.data

import io.github.gonbei774.calisthenicsmemory.workout.writeTextAtomically
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

class ProgramSessionCheckpointStore(private val file: File) {
    private val json = Json { encodeDefaults = true }

    @Throws(IOException::class)
    fun save(checkpoint: ProgramSessionCheckpoint) {
        file.writeTextAtomically(json.encodeToString(checkpoint))
    }

    /** The checkpoint, or null when there is none or it cannot be used; an unusable file is left in place. */
    fun load(): ProgramSessionCheckpoint? {
        if (!file.isFile) return null
        return try {
            val text = file.readText(Charsets.UTF_8)
            val version = json.parseToJsonElement(text).jsonObject["version"]?.jsonPrimitive?.int
            if (version != ProgramSessionCheckpoint.CURRENT_VERSION) return null
            json.decodeFromString<ProgramSessionCheckpoint>(text)
        } catch (e: IOException) {
            null
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun clear() {
        file.delete()
    }

    companion object {
        const val FILE_NAME = "program-session-checkpoint.json"
    }
}
