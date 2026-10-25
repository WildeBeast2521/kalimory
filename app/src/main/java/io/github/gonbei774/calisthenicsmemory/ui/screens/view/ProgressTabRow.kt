package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import io.github.gonbei774.calisthenicsmemory.ui.components.common.isLargeFontScale
import androidx.compose.material3.PrimaryScrollableTabRow
import io.github.gonbei774.calisthenicsmemory.ui.components.common.FitText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Progress's four fixed tabs, so none is clipped at the screen edge; long labels shrink to fit.
 * A text tab pads its label by 16 dp on each side, which left a quarter of a 360 dp screen too
 * little room for "Progressions" in most languages, so the label gets 4 dp instead. Japanese
 * (eight full-width characters) still needs 10 sp there; the size only drops as far as a label
 * needs, so the others stay larger. ProgressTabRowTest checks every language.
 */
@Composable
internal fun ProgressTabRow(titles: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val tabs: @Composable () -> Unit = {
        titles.forEachIndexed { index, title ->
            Tab(selected = selected == index, onClick = { onSelect(index) }) {
                // The height of a text-only tab.
                Box(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                    FitText(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                }
            }
        }
    }
    if (isLargeFontScale()) {
        // With a large font four equal tabs cannot hold their names; the row scrolls instead, and
        // each tab is as wide as its name.
        PrimaryScrollableTabRow(
            selectedTabIndex = selected,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onSurface,
            edgePadding = 0.dp,
        ) {
            titles.forEachIndexed { index, title ->
                Tab(selected = selected == index, onClick = { onSelect(index) }) {
                    Box(Modifier.height(48.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    }
                }
            }
        }
    } else {
        PrimaryTabRow(
            selectedTabIndex = selected,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { tabs() }
    }
}
