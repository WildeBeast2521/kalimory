package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.ui.components.targetText
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.Pink600
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing

/**
 * An exercise in a list where one is chosen, to start a workout or log one: tapping the row picks it. It
 * says what [ExerciseCheckRow] says, with a chevron for "opens". [showGroup] is for search results, which
 * are not under their group's heading.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExercisePickRow(exercise: Exercise, onClick: () -> Unit, showGroup: Boolean = false) {
    val tag = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = Spacing.xs),
                ) {
                    ExerciseBadges(exercise, showGroup = showGroup)
                    if (exercise.weightTrackingEnabled) Text(stringResource(R.string.legend_weight), style = tag, color = MaterialTheme.colorScheme.primary)
                    if (exercise.distanceTrackingEnabled) Text(stringResource(R.string.legend_distance), style = tag, color = MaterialTheme.colorScheme.secondary)
                    if (exercise.assistanceTrackingEnabled) Text(stringResource(R.string.legend_assistance), style = tag, color = Pink600)
                }
                val sets = exercise.targetSets
                val value = exercise.targetValue
                if (sets != null && value != null) {
                    Text(
                        text = targetText(sets, value, dynamic = exercise.type == "Dynamic", unilateral = exercise.laterality == "Unilateral"),
                        style = tag,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
            Icon(AppIcons.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
