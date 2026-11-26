package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.R
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Community share carries the share of body weight (ADR 0016), without a new format version. */
class CommunityShareBodyWeightTest {
    private fun share(vararg exercises: ShareExercise) =
        CommunityShareData(1, "share", "2026-10-01", "synthetic", "test", CommunityShareContent(exercises = exercises.toList()))

    @Test fun `a share from 1 to 100 percent passes`() {
        listOf(1, 65, 100).forEach {
            assertEquals(emptyList<ImportIssue>(), validateCommunityShareContent(share(ShareExercise("Push-up", "Dynamic", bodyWeightPercent = it))))
        }
    }

    @Test fun `a share outside 1 to 100 is refused`() {
        listOf(0, 101, -1).forEach {
            val errors = validateCommunityShareContent(share(ShareExercise("Push-up", "Dynamic", bodyWeightPercent = it)))
            assertEquals(R.string.share_issue_body_weight_percent, errors.single().res)
        }
    }

    @Test fun `a file from before the share reads with none`() {
        assertNull(Json { ignoreUnknownKeys = true }.decodeFromString<ShareExercise>("""{"name":"Push-up","type":"Dynamic"}""").bodyWeightPercent)
    }
}
