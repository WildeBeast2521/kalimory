package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Every plural formats in every language for numbers that reach each of its forms (zero, one,
 * two, few, many and other across the shipped languages), so no form is missing or throws.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PluralsFormatTest {
    private val languages = listOf("en", "ar", "de", "es", "fr", "it", "ja", "ru", "uk", "zh-CN")
    private val numbers = listOf(0, 1, 2, 3, 5, 11, 21, 100, 1_000_000)
    private val plurals = R.plurals::class.java.fields.map { it.name to it.getInt(null) }

    private fun context(tag: String): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
        return base.createConfigurationContext(config)
    }

    @Test fun `every plural formats for every number in every language`() {
        val failures = languages.flatMap { tag ->
            val resources = context(tag).resources
            plurals.flatMap { (name, id) ->
                numbers.mapNotNull { n ->
                    // Up to four arguments; a form uses the ones it needs.
                    runCatching { resources.getQuantityString(id, n, n, n, n, n) }
                        .fold({ if (it.isBlank()) "$tag/$name($n): blank" else null }, { "$tag/$name($n): $it" })
                }
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test fun `counts take the form of their number`() {
        fun sets(tag: String, n: Int) = context(tag).resources.getQuantityString(R.plurals.set_count, n, n)
        assertEquals("1 set", sets("en", 1))
        assertEquals("2 sets", sets("en", 2))
        assertEquals("1 подход", sets("ru", 1))
        assertEquals("3 подхода", sets("ru", 3))
        assertEquals("5 подходов", sets("ru", 5))
        assertEquals("21 подход", sets("ru", 21))
        fun recorded(tag: String, n: Int) = context(tag).resources.getQuantityString(R.plurals.sets_recorded, n, n)
        assertEquals("1 set recorded!", recorded("en", 1))
        assertEquals("2 sets recorded!", recorded("en", 2))
        assertEquals("سُجّلت مجموعتان", recorded("ar", 2))
        assertEquals("Записано 5 подходов", recorded("ru", 5))
    }
}
