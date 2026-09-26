package io.github.gonbei774.calisthenicsmemory.workout

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Stores one [WorkoutCheckpoint] as versioned JSON. A save writes a temporary file,
 * syncs it, and renames it over the old one, so a crash leaves either the old or
 * the new checkpoint, never a mix. Unreadable files are reported, never deleted.
 */
class WorkoutCheckpointStore(private val file: File) {
    sealed interface LoadResult {
        data object Missing : LoadResult
        data class Found(val checkpoint: WorkoutCheckpoint) : LoadResult
        data class UnsupportedVersion(val version: Int) : LoadResult
        data class Unreadable(val reason: String) : LoadResult
    }

    // Defaults are written so every field, including the version, is explicit in the file.
    private val json = Json { encodeDefaults = true }

    @Throws(IOException::class)
    fun save(checkpoint: WorkoutCheckpoint) {
        file.writeTextAtomically(json.encodeToString(checkpoint))
    }

    fun load(): LoadResult {
        if (!file.isFile) return LoadResult.Missing
        return try {
            val text = file.readText(Charsets.UTF_8)
            val version = json.parseToJsonElement(text).jsonObject["version"]?.jsonPrimitive?.int
                ?: return LoadResult.Unreadable("missing version")
            if (version != WorkoutCheckpoint.CURRENT_VERSION) return LoadResult.UnsupportedVersion(version)
            LoadResult.Found(json.decodeFromString<WorkoutCheckpoint>(text))
        } catch (e: IOException) {
            LoadResult.Unreadable(e.toString())
        } catch (e: SerializationException) {
            LoadResult.Unreadable(e.toString())
        } catch (e: IllegalArgumentException) {
            LoadResult.Unreadable(e.toString())
        }
    }

    /** Removes the checkpoint once the workout is finished, saved to history, or abandoned. */
    fun clear() {
        file.delete()
    }
}

/**
 * Replaces this file with [text]: writes a temporary file, syncs it, and renames it
 * over the old one, so a crash leaves either the old or the new content, never a mix.
 */
@Throws(IOException::class)
fun File.writeTextAtomically(text: String) {
    val temporary = File(path + ".tmp")
    FileOutputStream(temporary).use { output ->
        output.write(text.toByteArray(Charsets.UTF_8))
        output.flush()
        output.fd.sync()
    }
    Files.move(temporary.toPath(), toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
}
