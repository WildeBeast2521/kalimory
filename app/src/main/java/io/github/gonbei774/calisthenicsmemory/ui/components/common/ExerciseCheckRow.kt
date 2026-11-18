package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.ui.components.targetText
import io.github.gonbei774.calisthenicsmemory.ui.theme.Pink600

/**
 * An exercise in a list where several are chosen (adding to a program, an interval or the To Do list): a
 * checkbox, the name, what the row says about it, what it records besides reps or time, and its target.
 * The whole row toggles, not only the checkbox.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseCheckRow(
    exercise: Exercise,
    isSelected: Boolean,
    onToggle: (Long) -> Unit,
    showGroup: Boolean = false,
) {
    val tag = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { _ -> onToggle(exercise.id) })
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp, top = 10.dp, bottom = 6.dp)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    ExerciseBadges(exercise, showGroup = showGroup)
                    if (exercise.weightTrackingEnabled) {
                        Text(stringResource(R.string.legend_weight), style = tag, color = MaterialTheme.colorScheme.primary)
                    }
                    if (exercise.distanceTrackingEnabled) {
                        Text(stringResource(R.string.legend_distance), style = tag, color = MaterialTheme.colorScheme.secondary)
                    }
                    if (exercise.assistanceTrackingEnabled) {
                        Text(stringResource(R.string.legend_assistance), style = tag, color = Pink600)
                    }
                }
                val sets = exercise.targetSets
                val value = exercise.targetValue
                if (sets != null && value != null) {
                    Text(
                        text = targetText(sets, value, dynamic = exercise.type == "Dynamic", unilateral = exercise.laterality == "Unilateral"),
                        style = tag,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
