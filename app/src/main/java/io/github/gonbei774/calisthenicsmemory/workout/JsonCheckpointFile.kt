package io.github.gonbei774.calisthenicsmemory.workout

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException

/**
 * One automatic checkpoint stored as JSON with a top-level "version" field. Saves are
 * atomic ([writeTextAtomically]). A missing, unreadable, or other-version file loads
 * as null and is left in place.
 */
class JsonCheckpointFile<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
    private val version: Int,
) {
    private val json = Json { encodeDefaults = true }

    @Throws(IOException::class)
    fun save(value: T) {
        file.writeTextAtomically(json.encodeToString(serializer, value))
    }

    fun load(): T? {
        if (!file.isFile) return null
        return try {
            val text = file.readText(Charsets.UTF_8)
            if (json.parseToJsonElement(text).jsonObject["version"]?.jsonPrimitive?.int != version) return null
            json.decodeFromString(serializer, text)
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
}
