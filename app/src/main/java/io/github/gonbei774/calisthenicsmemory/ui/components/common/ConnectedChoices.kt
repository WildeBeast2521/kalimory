package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow

/**
 * A single choice as an Expressive connected button group: the buttons sit edge to edge, and the
 * selected one rounds fully and fills. With [allowNone], tapping the selected option clears it.
 * With [fill], the group spans the width and the options share it; without it, each option takes
 * its own width, so the group can sit in a scrolling row.
 */
@Composable
fun <T> ConnectedChoices(
    options: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    // Read aloud instead of a short visible label, such as "4 of 7 days" for "4".
    description: (@Composable (T) -> String)? = null,
    allowNone: Boolean = false,
    fill: Boolean = true,
) {
    Row(
        (if (fill) modifier.fillMaxWidth() else modifier).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            val checked = option == selected
            val spoken = description?.invoke(option)
            ToggleButton(
                checked = checked,
                onCheckedChange = { onSelect(if (checked && allowNone) null else option) },
                modifier = (if (fill) Modifier.weight(1f) else Modifier)
                    .semantics {
                        role = Role.RadioButton
                        if (spoken != null) contentDescription = spoken
                    },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                // Narrow padding so up to six options fit a phone width.
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            ) {
                Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
