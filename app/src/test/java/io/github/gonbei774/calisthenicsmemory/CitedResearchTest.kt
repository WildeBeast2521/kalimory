package io.github.gonbei774.calisthenicsmemory

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every English string that cites research ("et al.") is recorded word for word in
 * docs/wiki/Credits.md, with its source and what the source does and does not show (AGENTS.md,
 * "Strings and translations"). Rewording a claim in the app without updating the record fails here.
 */
class CitedResearchTest {
    private val strings = File("src/main/res/values/strings.xml").let { if (it.exists()) it else File("app/src/main/res/values/strings.xml") }
    private val credits = File("../docs/wiki/Credits.md").let { if (it.exists()) it else File("docs/wiki/Credits.md") }

    @Test fun `every cited claim is recorded in the credits`() {
        val citing = Regex("""<string name="([^"]+)">([^<]*et al\.[^<]*)</string>""").findAll(strings.readText())
            .map { it.groupValues[1] to it.groupValues[2].replace("\\'", "'") }
            .toList()
        assertTrue("No string cites research; this test then guards nothing", citing.isNotEmpty())
        val record = credits.readText()
        val missing = citing.filter { (key, text) -> "\"$text\"" !in record || "`$key`" !in record }.map { it.first }
        assertTrue("Cited in the app but not recorded word for word in docs/wiki/Credits.md: $missing", missing.isEmpty())
    }
}
