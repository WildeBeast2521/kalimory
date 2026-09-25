package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Ties the registered migrations to the supported-version policy
 * (docs/development/supported-database-versions.md) and the committed schemas.
 */
class MigrationRegistrationTest {
    // Unit tests run with the module directory as the working directory.
    private val schemaDir = File("schemas/io.github.gonbei774.calisthenicsmemory.data.AppDatabase")

    private fun committedSchemaVersions(): List<Int> {
        assertTrue("missing schema directory ${schemaDir.absolutePath}", schemaDir.isDirectory)
        return schemaDir.listFiles { file -> file.extension == "json" }!!
            .map { it.nameWithoutExtension.toInt() }
            .sorted()
    }

    @Test fun `oldest supported version is 9`() {
        assertEquals(9, AppDatabase.OLDEST_SUPPORTED_VERSION)
    }

    @Test fun `current version constant matches the newest committed schema`() {
        assertEquals(committedSchemaVersions().last(), AppDatabase.CURRENT_VERSION)
    }

    @Test fun `committed schemas cover exactly the supported versions`() {
        val versions = committedSchemaVersions()
        assertEquals((AppDatabase.OLDEST_SUPPORTED_VERSION..versions.last()).toList(), versions)
    }

    @Test fun `registered migrations form one consecutive path from the oldest supported version to the current version`() {
        val currentVersion = committedSchemaVersions().last()
        val edges = AppDatabase.ALL_MIGRATIONS.map { it.startVersion to it.endVersion }
        val expected = (AppDatabase.OLDEST_SUPPORTED_VERSION until currentVersion).map { it to it + 1 }
        assertEquals(expected, edges)
    }
}
