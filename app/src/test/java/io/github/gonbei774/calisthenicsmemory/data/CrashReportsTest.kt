package io.github.gonbei774.calisthenicsmemory.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CrashReportsTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val phone = CrashContext(
        versionName = "1.2.3", versionCode = 7, androidRelease = "14", sdkInt = 34,
        manufacturer = "Acme", model = "Phone 5",
    )

    private fun error(message: String) = IllegalStateException(message, IllegalArgumentException("cause"))

    @Test
    fun `a report holds the app, Android, device, thread and trace and nothing else`() {
        val text = CrashReports.format("main", error("boom"), phone)
        val lines = text.lines()
        assertEquals("**Kalimory crash report**", lines[0])
        assertEquals(
            listOf("- App: 1.2.3 (7)", "- Android: 14 (API 34)", "- Device: Acme Phone 5", "- Thread: main"),
            lines.filter { it.startsWith("- ") },
        )
        assertTrue(text.contains("```\njava.lang.IllegalStateException: boom\n"))
        assertTrue("the cause is kept", text.contains("Caused by: java.lang.IllegalArgumentException: cause"))
        assertTrue(text.endsWith("```"))
    }

    @Test
    fun `a very long trace is cut`() {
        val text = CrashReports.format("main", error("x".repeat(CrashReports.MAX_TRACE_CHARS * 2)), phone)
        assertTrue(text.contains("… (cut)"))
        assertTrue(text.length < CrashReports.MAX_TRACE_CHARS + 500)
    }

    @Test
    fun `only the newest reports are kept, newest first`() {
        val reports = CrashReports(folder.root)
        (1..5).forEach { reports.record("main", error("crash $it"), phone, atMillis = 1_000L * it) }
        val pending = reports.pending()
        assertEquals(CrashReports.KEEP, pending.size)
        assertEquals(listOf("crash 5", "crash 4", "crash 3"), pending.map { Regex("IllegalStateException: (.*)").find(it)!!.groupValues[1] })
        assertEquals(CrashReports.KEEP, folder.root.listFiles()!!.size)
    }

    @Test
    fun `ordering follows the time, not the file name's text`() {
        val reports = CrashReports(folder.root)
        reports.record("main", error("older"), phone, atMillis = 9_000L)
        reports.record("main", error("newer"), phone, atMillis = 10_000L)
        assertTrue(reports.pending().first().contains("newer"))
    }

    @Test
    fun `clear deletes every report and leaves other files alone`() {
        val reports = CrashReports(folder.root)
        reports.record("main", error("a"), phone, atMillis = 1L)
        val other = File(folder.root, "notes.txt").apply { writeText("keep") }
        reports.clear()
        assertTrue(reports.pending().isEmpty())
        assertTrue(other.exists())
    }

    @Test
    fun `recording never throws, even when it cannot write`() {
        val notADirectory = folder.newFile("taken")
        assertFalse(CrashReports(notADirectory).record("main", error("a"), phone, atMillis = 1L))
        assertTrue(CrashReports(notADirectory).pending().isEmpty())
    }

    @Test
    fun `the shared text has every report and where to file it`() {
        val text = CrashReports.shareText(listOf("first", "second"))
        assertEquals("first\n\n---\n\nsecond\n\nReport it at ${CrashReports.ISSUES_URL}\n", text)
    }

    @Test
    fun `crash reports are left out of Android backups and device transfers`() {
        val xml = File("src/main/res/xml").let { if (it.exists()) it else File("app/src/main/res/xml") }
        val exclude = """<exclude domain="file" path="${CrashReports.DIRECTORY}/" />"""
        assertEquals(1, xml.resolve("backup_rules.xml").readText().split(exclude).size - 1)
        assertEquals(2, xml.resolve("data_extraction_rules.xml").readText().split(exclude).size - 1)
    }
}
