package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.R
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Community share carries the muscles chosen for an own exercise (ADR 0014), without a new format version. */
class CommunityShareMusclesTest {
    private fun share(vararg exercises: ShareExercise) =
        CommunityShareData(1, "share", "2026-10-01", "synthetic", "test", CommunityShareContent(exercises = exercises.toList()))

    @Test fun `known muscle codes pass`() {
        val errors = validateCommunityShareContent(
            share(ShareExercise("Curl", "Dynamic", primaryMuscles = listOf("biceps"), secondaryMuscles = listOf("forearms", "lower back")))
        )
        assertEquals(emptyList<ImportIssue>(), errors)
    }

    @Test fun `an unknown muscle code is rejected, in either list`() {
        val primary = validateCommunityShareContent(share(ShareExercise("Curl", "Dynamic", primaryMuscles = listOf("wings"))))
        val helping = validateCommunityShareContent(share(ShareExercise("Curl", "Dynamic", secondaryMuscles = listOf("biceps", "tail"))))
        assertEquals(R.string.share_issue_unknown_muscle, primary.single().res)
        assertEquals(R.string.share_issue_unknown_muscle, helping.single().res)
    }

    @Test fun `a file from before the muscles reads with none, and one with them is read by apps that ignore them`() {
        val json = Json { ignoreUnknownKeys = true }
        val old = json.decodeFromString<ShareExercise>("""{"name":"Curl","type":"Dynamic"}""")
        assertTrue(old.primaryMuscles.isEmpty() && old.secondaryMuscles.isEmpty())
        val new = json.decodeFromString<ShareExercise>("""{"name":"Curl","type":"Dynamic","primaryMuscles":["biceps"]}""")
        assertEquals(listOf("biceps"), new.primaryMuscles)
    }
}
