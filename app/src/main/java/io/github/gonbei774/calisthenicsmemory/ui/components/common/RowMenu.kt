package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons

/** One thing a row's menu can do. A [destructive] action is drawn in the error colour. */
data class RowAction(val label: String, val icon: ImageVector, val onClick: () -> Unit, val destructive: Boolean = false)

/**
 * The ⋮ button at the end of a list row and the menu it opens: the row's other actions, in one place, so a
 * long press never has to hide them. Tapping the row itself opens what it is, and a long press only drags
 * where the list can be reordered.
 */
@Composable
fun RowMenu(actions: List<RowAction>, modifier: Modifier = Modifier) {
    Box(modifier) {
        var open by remember { mutableStateOf(false) }
        IconButton(onClick = { open = true }) {
            Icon(AppIcons.More, contentDescription = stringResource(R.string.menu), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.forEach { action ->
                val color = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                DropdownMenuItem(
                    text = { Text(action.label, color = color) },
                    leadingIcon = { Icon(action.icon, contentDescription = null, tint = if (action.destructive) color else MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        open = false
                        action.onClick()
                    },
                )
            }
        }
    }
}
