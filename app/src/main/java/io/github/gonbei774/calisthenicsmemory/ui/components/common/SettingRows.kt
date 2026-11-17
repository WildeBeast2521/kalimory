package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing

/*
 * The rows of a settings screen. A screen is a few groups, each a heading above one card whose rows sit
 * inside it, as on the Library tab, and every row is built the same way: a leading icon, a title with an
 * optional description under it, and a control at the end. The icon slot is always kept, so titles line
 * up whether or not a row has an icon.
 */

/** A heading above one card that holds related rows. */
@Composable
fun SettingsGroup(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.s).semantics { heading() },
        )
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), content = content)
        }
    }
}

/**
 * One row: [icon], [title], an optional [description] and a [trailing] control. With [onClick] the whole
 * row is the button; otherwise the control inside it is.
 */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .let { if (onClick != null) it.clickable(enabled = enabled, onClick = onClick) else it }
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        } else {
            Spacer(Modifier.width(24.dp))
        }
        val alpha = if (enabled) 1f else 0.38f
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
            }
        }
        trailing?.invoke()
    }
}

/** A row that opens somewhere: the chevron says so. */
@Composable
fun SettingLinkRow(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, description: String? = null) {
    SettingRow(
        title = title, modifier = modifier, icon = icon, description = description, onClick = onClick,
        trailing = { Icon(AppIcons.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
    )
}

/** An on/off setting: the whole row toggles, and TalkBack reads one control with its state. */
@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
    enabled: Boolean = true,
) {
    SettingRow(
        title = title, icon = icon, description = description, enabled = enabled,
        modifier = modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        trailing = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
    )
}

/**
 * An on/off setting with a number: while it is on, a − value + stepper sits under it, so the value is not
 * a dialog hidden behind a row that looks like a plain switch.
 */
@Composable
fun SettingValueRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    value: Int,
    onValueChange: (Int) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
    min: Int = 1,
    max: Int = 999,
    step: Int = 1,
) {
    Column(modifier.fillMaxWidth()) {
        SettingSwitchRow(title = title, checked = checked, onCheckedChange = onCheckedChange, icon = icon, description = description)
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
                modifier = Modifier.padding(bottom = Spacing.s),
            )
        }
    }
}

/** A setting chosen from a few options shown in place, under its title. */
@Composable
fun SettingChoiceRow(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    choices: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        SettingRow(title = title, icon = icon, description = description)
        Column(Modifier.padding(start = Spacing.l, end = Spacing.l, bottom = Spacing.m)) { choices() }
    }
}
