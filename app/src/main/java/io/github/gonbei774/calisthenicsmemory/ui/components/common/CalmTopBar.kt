package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons

/**
 * The app's screen header: an Expressive medium flexible top app bar. The title starts large and
 * shrinks into the bar as the content scrolls (pass an exit-until-collapsed [scrollBehavior] and
 * connect it to the screen's nestedScroll). The window insets are already handled by the
 * activity's Scaffold, so the bar adds none.
 */
@Composable
fun CalmTopBar(
    title: String,
    onBack: (() -> Unit)?,
    scrollBehavior: TopAppBarScrollBehavior?,
    modifier: Modifier = Modifier,
    // Full-screen editors close rather than go back.
    backIcon: ImageVector = AppIcons.Back,
    backDescription: String = stringResource(R.string.back),
    actions: @Composable RowScope.() -> Unit = {},
) {
    MediumFlexibleTopAppBar(
        title = {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
        },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(backIcon, contentDescription = backDescription)
                }
            }
        },
        actions = actions,
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior,
    )
}

/** A top bar's confirming action, such as Save, as an Expressive tonal button that squeezes when pressed. */
@Composable
fun TopBarAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shapes = androidx.compose.material3.ButtonDefaults.shapes(),
        modifier = Modifier.padding(end = 8.dp),
    ) {
        Text(text)
    }
}
