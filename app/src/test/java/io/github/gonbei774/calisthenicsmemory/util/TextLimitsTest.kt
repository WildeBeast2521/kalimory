package io.github.gonbei774.calisthenicsmemory.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Names and descriptions can always be edited and saved, including every catalogue text. */
class TextLimitsTest {
    @Test fun `new text fits up to the limit`() {
        assertTrue(TextLimits.fits("a".repeat(60), TextLimits.EXERCISE_NAME, null))
        assertFalse(TextLimits.fits("a".repeat(61), TextLimits.EXERCISE_NAME, null))
    }

    @Test fun `stored text longer than the limit stays editable but does not grow`() {
        val stored = "a".repeat(100)
        assertEquals(100, TextLimits.allowed(TextLimits.EXERCISE_NAME, stored))
        assertTrue(TextLimits.fits(stored, TextLimits.EXERCISE_NAME, stored))
        assertTrue(TextLimits.fits(stored.dropLast(1), TextLimits.EXERCISE_NAME, stored))
        assertFalse(TextLimits.fits(stored + "b", TextLimits.EXERCISE_NAME, stored))
    }

    private val res = File("src/main/res").let { if (it.exists()) it else File("app/src/main/res") }

    @Test fun `every catalogue name, chain and description fits in every language`() {
        val dirs = res.listFiles { f -> f.name.startsWith("values") && File(f, "catalogue_strings.xml").exists() }!!.toList()
        assertTrue("locales found: ${dirs.size}", dirs.size >= 10)
        val tooLong = dirs.flatMap { dir ->
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(File(dir, "catalogue_strings.xml")).getElementsByTagName("string")
            (0 until nodes.length).mapNotNull { i ->
                val node = nodes.item(i) as Element
                val key = node.getAttribute("name")
                val text = node.textContent.replace("\\'", "'")
                val limit = when {
                    key.endsWith("_cues") -> return@mapNotNull null
                    key.endsWith("_desc") -> TextLimits.DESCRIPTION
                    key.startsWith("cat_chain_") -> TextLimits.GROUP_NAME // a chain becomes the exercise's group
                    else -> TextLimits.EXERCISE_NAME
                }
                if (text.length > limit) "${dir.name}/$key (${text.length} > $limit)" else null
            }
        }
        assertTrue(tooLong.joinToString("\n"), tooLong.isEmpty())
    }
}
