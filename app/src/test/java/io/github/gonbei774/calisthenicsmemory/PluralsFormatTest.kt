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
        assertEquals("سُجّلت مجموعتان!", recorded("ar", 2))
        assertEquals("Записано 5 подходов!", recorded("ru", 5))
    }

    @Test fun `a target reads with the right forms for its sets and repetitions`() {
        // As targetText builds it: the set count and the repetitions, each in its own plural.
        fun target(tag: String, sets: Int, reps: Int): String {
            val resources = context(tag).resources
            return resources.getString(
                R.string.target_format,
                resources.getQuantityString(R.plurals.set_count, sets, sets),
                resources.getQuantityString(R.plurals.target_reps, reps, reps),
            )
        }
        assertEquals("3 sets × 1 rep", target("en", 3, 1))
        assertEquals("1 set × 12 reps", target("en", 1, 12))
        assertEquals("3 підходи × 1 раз", target("uk", 3, 1))
        assertEquals("5 підходів × 3 рази", target("uk", 5, 3))
        assertEquals("3セット × 10回", target("ja", 3, 10))
    }

    @Test fun `escaped special spaces and Chinese quotes reach the app`() {
        assertEquals("-10\u00A0с", context("uk").resources.getString(R.string.minus_10sec))
        assertTrue(context("fr").resources.getString(R.string.exercise_type_dynamic_description).startsWith("Par exemple\u202F:"))
        assertEquals("从ToDo中移除“深蹲”？", context("zh-CN").resources.getString(R.string.todo_delete_confirm_message, "深蹲"))
    }

    @Test fun `the share preview keeps the space before the auto-included count`() {
        // Compiled resources lose their outer spaces, so the space has to come from the join.
        fun preview(tag: String, total: Int, auto: Int): String {
            val resources = context(tag).resources
            return resources.getString(
                R.string.share_preview_total_with_auto,
                resources.getString(R.string.share_preview_total_exercises, total),
                resources.getQuantityString(R.plurals.share_preview_auto_included, auto, auto),
            )
        }
        assertEquals("Total exercises: 5 (2 auto-included)", preview("en", 5, 2))
        assertEquals("动作总数：5（2 自动包含）", preview("zh-CN", 5, 2))
    }
}
