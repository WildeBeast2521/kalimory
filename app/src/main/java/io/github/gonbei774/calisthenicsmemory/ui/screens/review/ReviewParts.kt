package io.github.gonbei774.calisthenicsmemory.ui.screens.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import io.github.gonbei774.calisthenicsmemory.ui.components.common.isLargeFontScale
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing

/**
 * A row of the review: a name with a value at the end and an optional detail below, read by
 * TalkBack as one item. With a large font the name and value share no line, so neither is squeezed.
 */
@Composable
internal fun ReviewRow(
    name: String,
    value: String,
    detail: String?,
    onClick: (() -> Unit)? = null,
    /** Drawn before the name, such as the body map's legend swatch. */
    leading: (@Composable () -> Unit)? = null,
    below: @Composable () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier.semantics(mergeDescendants = true) {})
            .padding(horizontal = Spacing.l, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        val nameText: @Composable (Modifier) -> Unit = { m ->
            Text(name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = m)
        }
        val valueText: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // A row that opens something ends in a chevron, as Today's rows do.
                if (onClick != null) Icon(AppIcons.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isLargeFontScale()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                leading?.let { Box(Modifier.padding(end = Spacing.m)) { it() } }
                nameText(Modifier.weight(1f))
            }
            valueText()
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                leading?.let { Box(Modifier.padding(end = Spacing.m)) { it() } }
                nameText(Modifier.weight(1f).padding(end = Spacing.s))
                valueText()
            }
        }
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        below()
    }
}
