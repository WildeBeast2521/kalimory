package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing

/**
 * A list row of text and the buttons that act on it (edit, resume, Start), optionally with a [leading]
 * piece such as a drag handle. The text takes the room the buttons leave; at large text the buttons
 * would leave a few letters, so they move under the text, at the end, and a name or a summary keeps the
 * whole width. [infoModifier] is for the text part only, such as a long-press handler, so the buttons
 * keep their own touches.
 */
@Composable
fun InfoActionRow(
    modifier: Modifier = Modifier,
    infoModifier: Modifier = Modifier,
    leading: (@Composable RowScope.() -> Unit)? = null,
    info: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    if (isLargeFontScale()) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                leading?.invoke(this)
                Column(infoModifier.weight(1f), content = info)
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.End),
                content = actions,
            )
        }
    } else {
        Row(
            modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            leading?.invoke(this)
            Column(infoModifier.weight(1f), content = info)
            actions()
        }
    }
}
