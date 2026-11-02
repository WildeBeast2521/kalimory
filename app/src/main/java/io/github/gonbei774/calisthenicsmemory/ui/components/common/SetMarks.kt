package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons

/**
 * A set's warm-up mark (ADR 0012), the one control used wherever a set is marked: the review
 * before saving, the program's results, manual entry and the workout editor. A selected chip
 * reads as selected to TalkBack; a mark is never set for the user.
 */
@Composable
fun SetMarks(warmUp: Boolean, onWarmUp: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = warmUp,
        onClick = { onWarmUp(!warmUp) },
        label = { Text(stringResource(R.string.set_warm_up), style = MaterialTheme.typography.labelLarge) },
        leadingIcon = if (warmUp) {
            { Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else null,
        modifier = modifier,
    )
}

/** How a warm-up set is labelled where sets are listed: text, never colour alone. */
@Composable
fun warmUpLabel(): String = stringResource(R.string.set_warm_up)
