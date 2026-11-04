package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.v2.Effort
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing

/**
 * A set's warm-up mark and effort rating (ADR 0012), the one control used wherever a set is
 * marked: the review before saving, the program's results, manual entry and the workout editor.
 * The effort part shows when [onEffort] is given. Nothing is ever set for the user.
 */
@Composable
fun SetMarks(
    warmUp: Boolean,
    onWarmUp: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showWarmUp: Boolean = true,
    effortLeft: Int? = null,
    /** A rep set is rated in reps left; a hold on the same five steps in words. */
    dynamic: Boolean = true,
    onEffort: ((Int?) -> Unit)? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        if (showWarmUp) {
            FilterChip(
                selected = warmUp,
                onClick = { onWarmUp(!warmUp) },
                label = { Text(stringResource(R.string.set_warm_up), style = MaterialTheme.typography.labelLarge) },
                leadingIcon = if (warmUp) {
                    { Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else null,
            )
        }
        if (onEffort != null) EffortChoices(effortLeft, dynamic, onEffort)
    }
}

/**
 * The effort question and its five choices, 0 (nothing left) to [Effort.MAX_EFFORT_LEFT].
 * Tapping the chosen one clears it; none is preselected, and answering is never required.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EffortChoices(selected: Int?, dynamic: Boolean, onSelect: (Int?) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            stringResource(if (dynamic) R.string.effort_question_reps else R.string.effort_question_hold),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            for (value in 0..Effort.MAX_EFFORT_LEFT) {
                val description = effortText(value, dynamic)
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(if (selected == value) null else value) },
                    label = { Text(effortChipLabel(value, dynamic), style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.semantics { contentDescription = description },
                )
            }
        }
    }
}

/** The short label on an effort chip. */
@Composable
private fun effortChipLabel(value: Int, dynamic: Boolean): String = when {
    !dynamic -> holdLabel(value)
    value == 0 -> stringResource(R.string.effort_reps_none)
    value >= Effort.MAX_EFFORT_LEFT -> stringResource(R.string.effort_reps_four_or_more)
    else -> "$value"
}

/** A rating in words, as History shows it and TalkBack reads a chip: "2 reps left", "Effort: Fair". */
@Composable
fun effortText(value: Int, dynamic: Boolean): String = when {
    !dynamic -> stringResource(R.string.effort_hold_history, holdLabel(value))
    value <= 0 -> stringResource(R.string.effort_no_reps_left)
    value >= Effort.MAX_EFFORT_LEFT -> stringResource(R.string.effort_four_or_more_left)
    else -> pluralStringResource(R.plurals.effort_reps_left, value, value)
}

@Composable
private fun holdLabel(value: Int): String = stringResource(
    when (value) {
        0 -> R.string.effort_hold_0
        1 -> R.string.effort_hold_1
        2 -> R.string.effort_hold_2
        3 -> R.string.effort_hold_3
        else -> R.string.effort_hold_4
    }
)

/** How a warm-up set is labelled where sets are listed: text, never colour alone. */
@Composable
fun warmUpLabel(): String = stringResource(R.string.set_warm_up)
