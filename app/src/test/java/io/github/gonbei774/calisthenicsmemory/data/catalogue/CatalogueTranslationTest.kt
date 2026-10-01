package io.github.gonbei774.calisthenicsmemory.data.catalogue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Every locale carries the whole catalogue, with the same cues, line for line (ADR 0006). */
class CatalogueTranslationTest {
    private val res = File("src/main/res")
    private val locales = listOf("ar", "de", "es", "fr", "it", "ja", "ru", "uk", "zh-rCN")

    private fun strings(dir: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$dir/catalogue_strings.xml"))
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index) as Element
            node.getAttribute("name") to node.textContent
        }
    }

    private val english = strings("values")

    @Test fun `every locale has every catalogue string and no others`() {
        locales.forEach { locale ->
            assertEquals(locale, english.keys, strings("values-$locale").keys)
        }
    }

    @Test fun `cues have as many lines as in English`() {
        locales.forEach { locale ->
            val translated = strings("values-$locale")
            english.filterKeys { it.endsWith("_cues") }.forEach { (key, cues) ->
                assertEquals("$locale $key", cues.split("\\n").size, translated.getValue(key).split("\\n").size)
            }
        }
    }

    @Test fun `no translation is left blank`() {
        locales.forEach { locale ->
            strings("values-$locale").forEach { (key, text) -> assertTrue("$locale $key", text.isNotBlank()) }
        }
    }
}
