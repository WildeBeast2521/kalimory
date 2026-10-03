package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons

/**
 * The app's one floating action button: the size, shape and colours of Today's start-workout
 * button (primary container, icon in its on-colour), so every screen's main action looks alike.
 */
@Composable
fun AppFab(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier, icon: ImageVector = AppIcons.Add) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
