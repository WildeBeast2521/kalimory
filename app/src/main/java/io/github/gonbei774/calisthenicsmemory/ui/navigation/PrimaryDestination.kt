package io.github.gonbei774.calisthenicsmemory.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R

/** The four primary destinations from ADR 0001, in bottom-bar order. */
enum class PrimaryDestination(@StringRes val label: Int, val icon: ImageVector) {
    TODAY(R.string.nav_today, Icons.Filled.Home),
    TRAIN(R.string.nav_train, Icons.Filled.PlayArrow),
    PROGRESS(R.string.nav_progress, Icons.Filled.DateRange),
    LIBRARY(R.string.nav_library, Icons.AutoMirrored.Filled.List),
}

@Composable
fun PrimaryNavigationBar(
    selected: PrimaryDestination,
    onSelect: (PrimaryDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ShortNavigationBar(modifier = modifier.testTag(PRIMARY_NAVIGATION_BAR_TAG)) {
        PrimaryDestination.entries.forEach { destination ->
            ShortNavigationBarItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                // The label names the item, so the icon needs no separate description.
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.label)) },
                // The default selected label uses the secondary color, green here, which clashes with the blue
                // indicator and is low-contrast on the dark bar. Match the indicator's content color instead.
                colors = ShortNavigationBarItemDefaults.colors(
                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }
    }
}

const val PRIMARY_NAVIGATION_BAR_TAG = "primary_navigation_bar"
