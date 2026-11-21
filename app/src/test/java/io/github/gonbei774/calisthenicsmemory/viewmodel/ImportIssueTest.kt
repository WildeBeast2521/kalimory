package io.github.gonbei774.calisthenicsmemory.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImportIssueTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test fun `a line issue puts the line number first`() {
        assertEquals("Line 7: invalid type \"Cardio\" (must be Dynamic or Isometric)", ImportIssue(R.string.csv_issue_type, listOf("Cardio")).atLine(7).text(context))
    }

    @Test fun `an issue about an item names its place in the file`() {
        assertEquals("exercises[2]: the name is empty", ImportIssue(R.string.share_issue_name_empty, listOf("exercises[2]")).text(context))
    }

    @Test fun `a literal issue is shown as it is`() {
        assertEquals("disk full", ImportIssue.literal("disk full").text(context))
    }

    @Test fun `in German the line word and the order change`() {
        val de = context.createConfigurationContext(android.content.res.Configuration().apply { setLocale(java.util.Locale.GERMAN) })
        assertEquals("Zeile 3: Der Gruppenname ist leer", ImportIssue(R.string.csv_issue_group_empty).atLine(3).text(de))
    }
}
