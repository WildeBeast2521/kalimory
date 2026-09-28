package io.github.gonbei774.calisthenicsmemory.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R

/** The four primary destinations from ADR 0001, in bottom-bar order; the filled icon marks the selected one. */
enum class PrimaryDestination(@StringRes val label: Int, @DrawableRes val icon: Int, @DrawableRes val selectedIcon: Int) {
    TODAY(R.string.nav_today, R.drawable.ms_today, R.drawable.ms_today_fill),
    TRAIN(R.string.nav_train, R.drawable.ms_fitness_center, R.drawable.ms_fitness_center_fill),
    PROGRESS(R.string.nav_progress, R.drawable.ms_insights, R.drawable.ms_insights_fill),
    LIBRARY(R.string.nav_library, R.drawable.ms_book_2, R.drawable.ms_book_2_fill),
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
                icon = {
                    Icon(
                        ImageVector.vectorResource(if (destination == selected) destination.selectedIcon else destination.icon),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.label)) },
                // The default selected label uses the secondary color, green here, which clashes with the blue
                // indicator and is low-contrast on the dark bar. Match the indicator's content color instead.
                colors = ShortNavigationBarItemDefaults.colors(
                    selectedTextColorTopIconPosition = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }
    }
}

const val PRIMARY_NAVIGATION_BAR_TAG = "primary_navigation_bar"
