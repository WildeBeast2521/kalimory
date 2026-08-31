package io.github.gonbei774.calisthenicsmemory.ui.screens

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class BackupSizeLimitException(val limitBytes: Int) :
    IOException("Backup exceeds the $limitBytes byte limit")

object BackupFileIo {
    const val MAX_BACKUP_BYTES: Int = 50 * 1024 * 1024

    @Throws(IOException::class)
    fun readUtf8(
        openStream: () -> InputStream?,
        maxBytes: Int = MAX_BACKUP_BYTES
    ): String {
        require(maxBytes >= 0) { "maxBytes must not be negative" }
        val input = openStream() ?: throw IOException("Unable to open backup for reading")
        return input.use {
            val bytes = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = it.read(buffer)
                if (read == -1) break
                if (read == 0) continue
                if (total > maxBytes - read) throw BackupSizeLimitException(maxBytes)
                bytes.write(buffer, 0, read)
                total += read
            }
            bytes.toString(StandardCharsets.UTF_8.name())
        }
    }

    @Throws(IOException::class)
    fun writeUtf8(openStream: () -> OutputStream?, value: String) {
        val output = openStream() ?: throw IOException("Unable to open backup for writing")
        output.use {
            it.write(value.toByteArray(StandardCharsets.UTF_8))
            it.flush()
        }
    }
}
