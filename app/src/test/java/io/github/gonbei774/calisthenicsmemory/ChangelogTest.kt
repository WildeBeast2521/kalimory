package io.github.gonbei774.calisthenicsmemory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every CHANGELOG heading links somewhere, and Kalimory's versions link to Kalimory's own releases.
 * The original app's releases share version numbers with Kalimory's (both have a 1.0.0), so they
 * are headed "Calisthenics Memory x.y.z" and keep their own links.
 */
class ChangelogTest {
    private val changelog = (File("../CHANGELOG.md").takeIf { it.exists() } ?: File("CHANGELOG.md")).readText()
    private val headings = Regex("""^## \[([^\]]+)\]""", RegexOption.MULTILINE).findAll(changelog).map { it.groupValues[1] }.toList()
    private val links = Regex("""^\[([^\]]+)\]: (\S+)$""", RegexOption.MULTILINE).findAll(changelog)
        .associate { it.groupValues[1] to it.groupValues[2] }
    private val github = "https://github.com/WildeBeast2521/kalimory"

    @Test fun `every heading is unique and has a link`() {
        assertEquals("Duplicate headings", headings, headings.distinct())
        val missing = headings.filter { it !in links }
        assertTrue("Add a link for: $missing", missing.isEmpty())
        val unused = links.keys - headings.toSet()
        assertTrue("Links without a heading: $unused", unused.isEmpty())
    }

    @Test fun `Kalimory versions link to their GitHub release and Unreleased compares with the latest`() {
        val versions = headings.filter { Regex("""\d+\.\d+\.\d+""").matches(it) }
        assertTrue(versions.isNotEmpty())
        versions.forEach { assertEquals(it, "$github/releases/tag/v$it", links[it]) }
        assertEquals("Unreleased", headings.first())
        assertEquals("$github/compare/v${versions.first()}...HEAD", links["Unreleased"])
    }

    @Test fun `the original app's releases link to Codeberg`() {
        val upstream = headings.filter { it.startsWith("Calisthenics Memory ") }
        assertTrue(upstream.isNotEmpty())
        upstream.forEach {
            assertEquals(it, "https://codeberg.org/Gonbei774/CalisthenicsMemory/releases/tag/v${it.removePrefix("Calisthenics Memory ")}", links[it])
        }
    }
}
