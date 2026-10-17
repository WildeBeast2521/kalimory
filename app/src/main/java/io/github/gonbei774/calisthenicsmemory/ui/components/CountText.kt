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
 * An exercise's target, such as "3 sets × 10 reps", with the set count in the plural form of the
 * language. [unit] is the value's unit; [display] adds the "Target:" label.
 */
@Composable
fun targetText(sets: Int, value: Int, unit: String, unilateral: Boolean, display: Boolean = false): String =
    stringResource(
        when {
            display && unilateral -> R.string.target_display_unilateral
            display -> R.string.target_display
            unilateral -> R.string.target_format_unilateral
            else -> R.string.target_format
        },
        countText(R.plurals.set_count, sets),
        value,
        unit,
    )
