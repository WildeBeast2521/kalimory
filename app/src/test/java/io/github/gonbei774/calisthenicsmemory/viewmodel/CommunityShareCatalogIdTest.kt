package io.github.gonbei774.calisthenicsmemory.viewmodel

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Community share carries the catalogue link (ADR 0007, decision 6). */
class CommunityShareCatalogIdTest {
    private fun share(vararg exercises: ShareExercise) =
        CommunityShareData(1, "share", "2026-10-01", "synthetic", "test", CommunityShareContent(exercises = exercises.toList()))

    @Test fun `a well formed step id passes, including one this catalogue does not know`() {
        val errors = validateCommunityShareContent(
            share(ShareExercise("Chin-up", "Dynamic", catalogId = "pull.chin"), ShareExercise("Planche", "Isometric", catalogId = "planche.full"))
        )
        assertEquals(emptyList<String>(), errors)
    }

    @Test fun `a malformed step id is rejected`() {
        val errors = validateCommunityShareContent(share(ShareExercise("Chin-up", "Dynamic", catalogId = "Pull Chin")))
        assertTrue(errors.toString(), errors.single().contains("invalid catalogId"))
    }

    @Test fun `two exercises linked to one step are rejected`() {
        val errors = validateCommunityShareContent(
            share(ShareExercise("Chin-up", "Dynamic", catalogId = "pull.chin"), ShareExercise("Chin-ups", "Dynamic", catalogId = "pull.chin"))
        )
        assertTrue(errors.toString(), errors.single().contains("duplicate catalogId"))
    }

    @Test fun `a file from before the link reads with no link`() {
        val json = """{"name":"Chin-up","type":"Dynamic"}"""
        assertNull(Json { ignoreUnknownKeys = true }.decodeFromString<ShareExercise>(json).catalogId)
    }
}
