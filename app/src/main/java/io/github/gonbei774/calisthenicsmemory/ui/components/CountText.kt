package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R

/** [count] in the plural form the language uses for it, such as "1 set" or "3 sets". */
@Composable
fun countText(@PluralsRes id: Int, count: Int): String = pluralStringResource(id, count, count)

/**
 * An exercise's target, such as "3 sets × 10 reps" or "2 sets × 30 sec", with the counts in the
 * plural forms of the language. [dynamic] counts repetitions, else seconds; [display] adds "Target:".
 */
@Composable
fun targetText(sets: Int, value: Int, dynamic: Boolean, unilateral: Boolean, display: Boolean = false): String =
    stringResource(
        when {
            display && unilateral -> R.string.target_display_unilateral
            display -> R.string.target_display
            unilateral -> R.string.target_format_unilateral
            else -> R.string.target_format
        },
        countText(R.plurals.set_count, sets),
        if (dynamic) countText(R.plurals.target_reps, value)
        else stringResource(R.string.value_with_unit, value, stringResource(R.string.unit_seconds)),
    )
