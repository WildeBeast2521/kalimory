package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Every Progress tab label is shown whole on a 360 dp phone, in every language the app ships. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ProgressTabRowTest(private val locale: String) {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun noLabelIsCut() {
        RuntimeEnvironment.setQualifiers("$locale-w360dp-h740dp-xhdpi")
        val context = compose.activity
        val titles = listOf(R.string.tab_calendar, R.string.tab_list, R.string.tab_graph, R.string.tab_progressions)
            .map { context.getString(it) }
        compose.setContent {
            CalisthenicsMemoryTheme(darkTheme = false) { ProgressTabRow(titles, selected = 0, onSelect = {}) }
        }
        compose.waitForIdle()
        val cut = titles.filter { title ->
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(title).fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            layouts.single().hasVisualOverflow
        }
        assertTrue("Cut at 360 dp in $locale: $cut", cut.isEmpty())
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun locales() = listOf("en", "ar", "de", "es", "fr", "it", "ja", "ru", "uk", "zh-rCN").map { arrayOf<Any>(it) }
    }
}
