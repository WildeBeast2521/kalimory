package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons

/**
 * What a row says about an exercise besides its name: its group (where the row is not under the group's
 * heading), a star when it is a favourite, its difficulty when it has a target, whether it is counted in reps
 * or time, and whether it is done one side at a time.
 *
 * It emits its pieces straight into the caller's row (a Row or FlowRow), so a screen keeps its own wrapping
 * and can add its own pieces after them, such as the weight or the estimated time.
 */
@Composable
fun ExerciseBadges(exercise: Exercise, showGroup: Boolean = false) {
    val label = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
    if (showGroup) exercise.group?.let { GroupBadge(it) }
    if (exercise.isFavorite) {
        Icon(AppIcons.FavoriteFilled, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
    }
    if (exercise.sortOrder in 1..10) {
        Text(stringResource(R.string.level_format, exercise.sortOrder), style = label, color = MaterialTheme.colorScheme.secondary)
    }
    Text(
        stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
        style = label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (exercise.laterality == "Unilateral") {
        Text(stringResource(R.string.one_sided), style = label, color = MaterialTheme.colorScheme.secondary)
    }
}
