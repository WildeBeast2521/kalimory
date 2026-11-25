package io.github.gonbei774.calisthenicsmemory.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The "What's new" sheet reads CHANGELOG.md; the real file must keep parsing (F-Droid does not run these tests, CI does). */
class ChangelogTest {
    private val file = File("../CHANGELOG.md").let { if (it.exists()) it else File("CHANGELOG.md") }
    private val releases get() = Changelog.parse(file.readText())

    private val sample = """
        # Changelog

        ## [Unreleased]

        ### Added
        - Not shipped yet ([#9](https://example.org/pull/9))

        ## [1.2.0] - 2026-12-01 [YANKED]

        ### Added
        - **Bold** and `code` with a [link](https://example.org) ([#8](https://example.org/pull/8)) ([#7](https://example.org/pull/7))
        - A wrapped
          bullet

        ### Fixed
        - Fixed one

        ## [1.0.0] - 2026-10-04

        An intro paragraph.

        ### Added
        - First

        ## [Calisthenics Memory 1.26.0] - 2026-07-20

        ### Changed
        - Original app change

        [Unreleased]: https://example.org/compare
        [1.2.0]: https://example.org/tag
    """.trimIndent()

    @Test fun `unreleased is left out and the rest keeps the file order`() {
        assertEquals(listOf("1.2.0", "1.0.0", "Calisthenics Memory 1.26.0"), Changelog.parse(sample).map { it.name })
    }

    @Test fun `a yanked release is flagged and dates are read`() {
        val first = Changelog.parse(sample).first()
        assertTrue(first.yanked)
        assertEquals("2026-12-01", first.date)
        assertFalse(Changelog.parse(sample)[1].yanked)
    }

    @Test fun `entries lose their pull request links and Markdown marks, and wrapped bullets join`() {
        val added = Changelog.parse(sample).first().sections.first { it.kind == ChangeKind.ADDED }
        assertEquals(listOf("Bold and code with a link", "A wrapped bullet"), added.entries)
    }

    @Test fun `an intro paragraph is kept and the link definitions are not part of any release`() {
        val releases = Changelog.parse(sample)
        assertEquals("An intro paragraph.", releases[1].intro)
        assertNull(releases[0].intro)
        assertEquals(listOf("Original app change"), releases.last().sections.single().entries)
    }

    @Test fun `upTo stops at the installed version and puts the original app last`() {
        val shown = Changelog.upTo(Changelog.parse(sample), "1.1.0").map { it.name }
        assertEquals(listOf("1.0.0", "Calisthenics Memory 1.26.0"), shown)
    }

    @Test fun `a version is new when it is newer than the last one seen, or is the installed one when none is known`() {
        assertTrue(Changelog.isNew("1.2.0", "1.1.1", "1.2.0"))
        assertFalse(Changelog.isNew("1.1.1", "1.1.1", "1.2.0"))
        assertTrue(Changelog.isNew("1.2.0", null, "1.2.0"))
        assertFalse(Changelog.isNew("1.1.1", null, "1.2.0"))
        assertEquals(1, Changelog.compare("1.10.0", "1.9.9"))
    }

    @Test fun `junk never throws`() {
        assertTrue(Changelog.parse("").isEmpty())
        assertTrue(Changelog.parse("## [").isEmpty())
        assertEquals(1, Changelog.parse("## [1.0.0]\n### Nonsense\n- x").size)
    }

    @Test fun `the sheet is due after an update, not on a fresh install, not on the same or an older version`() {
        assertTrue(whatsNewDue(welcomeSeen = true, lastSeenCode = null, currentCode = 6))
        assertTrue(whatsNewDue(true, 5, 6))
        assertFalse(whatsNewDue(welcomeSeen = false, lastSeenCode = null, currentCode = 6))
        assertFalse(whatsNewDue(true, 6, 6))
        assertFalse(whatsNewDue(true, 7, 6))
    }

    // The real file.
    @Test fun `the real changelog parses, with Kalimory 1_0_0 and the original app present`() {
        val all = releases
        assertTrue("CHANGELOG.md has no releases", all.isNotEmpty())
        val kalimory = all.filter { it.isKalimory }
        assertTrue(kalimory.any { it.version == "1.0.0" })
        assertTrue(all.any { !it.isKalimory })
        assertNotNull(all.first().date)
        // Newest first.
        assertEquals(kalimory.sortedWith { a, b -> Changelog.compare(b.version, a.version) }.map { it.version }, kalimory.map { it.version })
        assertTrue(all.none { it.name == "Unreleased" })
    }

    @Test fun `no entry of the real changelog shows Markdown or a pull request link`() {
        val text = releases.flatMap { r -> r.sections.flatMap { it.entries } + listOfNotNull(r.intro) }
        assertTrue(text.isNotEmpty())
        text.forEach { entry ->
            assertFalse(entry, "](" in entry || "**" in entry || "`" in entry || "/pull/" in entry || entry.isBlank())
        }
    }
}
