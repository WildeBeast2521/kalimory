package io.github.gonbei774.calisthenicsmemory

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every translation formats with the arguments the English string is given. A %d left where English
 * has %s throws at runtime, and a missing or extra argument shows the wrong value, so neither may ship.
 */
class StringFormatTranslationTest {
    private val res = File("src/main/res").let { if (it.exists()) it else File("app/src/main/res") }
    private val locales = res.listFiles { dir -> dir.name.startsWith("values-") && dir.resolve("strings.xml").exists() }.orEmpty()
    private val placeholder = Regex("""%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([a-zA-Z])""")

    private fun read(dir: File): Pair<Map<String, String>, Map<String, Map<String, String>>> {
        val text = dir.resolve("strings.xml").readText()
        val strings = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(text).associate { it.groupValues[1] to it.groupValues[2] }
        val plurals = Regex("""<plurals name="([^"]+)"[^>]*>(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL).findAll(text).associate { p ->
            p.groupValues[1] to Regex("""<item quantity="(\w+)">(.*?)</item>""").findAll(p.groupValues[2])
                .associate { it.groupValues[1] to it.groupValues[2] }
        }
        return strings to plurals
    }

    /** Argument position to conversion, such as {1=d, 2=s}; a literal %% is not an argument. */
    private fun args(text: String): Map<Int, Char> {
        var next = 1
        return placeholder.findAll(text.replace("%%", "")).associate { m ->
            val position = m.groupValues[1].toIntOrNull() ?: next++
            position to m.groupValues[2].single().lowercaseChar()
        }
    }

    @Test fun `translated strings take the same arguments as English`() {
        val (english, _) = read(res.resolve("values"))
        val offenders = locales.flatMap { dir ->
            read(dir).first.filter { (key, value) -> key in english && args(value) != args(english.getValue(key)) }
                .map { (key, value) -> "${dir.name}/$key: ${args(value)} instead of ${args(english.getValue(key))} in \"$value\"" }
        }
        assertTrue(offenders.joinToString("\n"), offenders.isEmpty())
    }

    @Test fun `every plural form uses only the arguments English uses, with the same conversions`() {
        val (_, english) = read(res.resolve("values"))
        val offenders = locales.flatMap { dir ->
            read(dir).second.filter { it.key in english }.flatMap { (key, forms) ->
                val expected = english.getValue(key).values.map { args(it) }.reduce { a, b -> a + b }
                // A form may leave the number out ("one set" in Arabic), but never adds or changes one.
                forms.filter { (_, value) -> args(value).any { (position, conversion) -> expected[position] != conversion } }
                    .map { (quantity, value) -> "${dir.name}/$key[$quantity]: ${args(value)} not within $expected in \"$value\"" }
            }
        }
        assertTrue(offenders.joinToString("\n"), offenders.isEmpty())
    }

    @Test fun `every English plural is in every language`() {
        val (_, english) = read(res.resolve("values"))
        val offenders = locales.flatMap { dir -> (english.keys - read(dir).second.keys).map { "${dir.name}/$it" } }
        assertTrue("Missing plurals: $offenders", offenders.isEmpty())
    }
}
