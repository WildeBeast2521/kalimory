package io.github.gonbei774.calisthenicsmemory.ui.components.program

import io.github.gonbei774.calisthenicsmemory.ui.components.common.SwipeToDeleteBackground
import io.github.gonbei774.calisthenicsmemory.ui.components.countText
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.ui.theme.*

@Composable
fun ProgramExerciseItem(
    programExercise: ProgramExercise,
    exercise: Exercise,
    isDragging: Boolean,
    elevation: Dp,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragHandle: @Composable () -> Modifier
) {
    var pendingDelete by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                pendingDelete = true
                true
            } else {
                false
            }
        }
    )

    // Wait for swipe animation to complete before deleting
    LaunchedEffect(pendingDelete) {
        if (pendingDelete) {
            kotlinx.coroutines.delay(300)
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeToDeleteBackground(dismissState, MaterialTheme.shapes.large) },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        Card(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Drag handle
                Icon(
                    AppIcons.DragHandle,
                    contentDescription = stringResource(R.string.todo_drag_to_reorder),
                    tint = if (isDragging) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .then(dragHandle())
                )

                // Exercise info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // The plan's numbers share one muted colour: they are information, not actions or states.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = countText(R.plurals.set_count, programExercise.sets),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                R.string.program_target_format,
                                programExercise.targetValue,
                                stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.program_interval_format, programExercise.intervalSeconds),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}