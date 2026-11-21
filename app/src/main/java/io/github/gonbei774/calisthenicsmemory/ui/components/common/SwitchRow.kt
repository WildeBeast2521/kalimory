package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing

/**
 * A setting that is on or off: the whole row toggles, not only the small switch, and TalkBack reads
 * the title, the description and the state as one control. The text takes the room the switch leaves,
 * so at large text the switch stays on the card.
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = Spacing.m)) {
            val alpha = if (enabled) 1f else 0.38f
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
            }
        }
        // The row is the control: the switch only shows its state.
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/**
 * A [SwitchRow] that has a number: while the setting is on, a − value + stepper sits under it, so the
 * value is never a field hidden in the same line as the switch (a row that looked like a plain switch).
 * The number stays typeable; an empty field keeps the last value until a number is typed.
 */
@Composable
fun SwitchValueRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    value: Int,
    onValueChange: (Int) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    min: Int = 1,
    max: Int = 999,
    step: Int = 1,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SwitchRow(title = title, description = description, checked = checked, onCheckedChange = onCheckedChange)
        if (checked) {
            var text by remember(value) { mutableStateOf(value.toString()) }
            NumberStepper(
                value = text,
                onValueChange = { typed ->
                    text = typed
                    typed.toIntOrNull()?.takeIf { it in min..max }?.let(onValueChange)
                },
                unit = unit,
                step = step,
                min = min,
                max = max,
            )
        }
    }
}
