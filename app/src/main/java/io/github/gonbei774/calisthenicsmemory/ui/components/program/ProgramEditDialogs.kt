@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package io.github.gonbei774.calisthenicsmemory.ui.components.program

import io.github.gonbei774.calisthenicsmemory.ui.components.common.LabelledNumberStepper
import io.github.gonbei774.calisthenicsmemory.ui.components.common.ExerciseCheckRow
import io.github.gonbei774.calisthenicsmemory.ui.components.common.ExerciseBadges
import io.github.gonbei774.calisthenicsmemory.ui.components.common.GroupBadge
import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmSearchField
import io.github.gonbei774.calisthenicsmemory.ui.components.targetText
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.util.SearchUtils
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSettingsDialog(
    programExercise: ProgramExercise,
    exercise: Exercise,
    availableLoops: List<ProgramLoop>,
    onDismiss: () -> Unit,
    onSave: (ProgramExercise) -> Unit
) {
    var sets by remember {
        mutableStateOf(if (programExercise.sets == 0) "" else programExercise.sets.toString())
    }
    var targetValue by remember {
        mutableStateOf(if (programExercise.targetValue == 0) "" else programExercise.targetValue.toString())
    }
    var intervalSeconds by remember { mutableStateOf(programExercise.intervalSeconds.toString()) }

    // Loop selection state (only for standalone exercises)
    var selectedLoopId by remember { mutableStateOf<Long?>(programExercise.loopId) }
    var loopDropdownExpanded by remember { mutableStateOf(false) }

    // Show loop selection whenever loops exist, so exercises can be moved into,
    // between, or out of (via "None") a loop
    val showLoopSelection = availableLoops.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.exercise_settings),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LabelledNumberStepper(
                    label = stringResource(R.string.sets_label),
                    value = sets,
                    onValueChange = { sets = it },
                    range = 1..99,
                    unit = "",
                )

                LabelledNumberStepper(
                    label = stringResource(R.string.target_value_label),
                    value = targetValue,
                    onValueChange = { targetValue = it },
                    range = 1..999,
                    unit = stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds),
                )

                LabelledNumberStepper(
                    label = stringResource(R.string.interval_seconds_label),
                    value = intervalSeconds,
                    onValueChange = { intervalSeconds = it },
                    range = 0..600,
                    unit = stringResource(R.string.unit_seconds),
                    step = 5,
                )

                // Loop selection dropdown (only for standalone exercises)
                if (showLoopSelection) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.move_to_loop),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExposedDropdownMenuBox(
                        expanded = loopDropdownExpanded,
                        onExpandedChange = { loopDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedLoopId?.let { id ->
                                val loopIndex = availableLoops.indexOfFirst { it.id == id }
                                if (loopIndex >= 0) {
                                    stringResource(R.string.loop_number_format, loopIndex + 1)
                                } else {
                                    stringResource(R.string.none)
                                }
                            } ?: stringResource(R.string.none),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = loopDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = loopDropdownExpanded,
                            onDismissRequest = { loopDropdownExpanded = false }
                        ) {
                            // None option
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.none)) },
                                onClick = {
                                    selectedLoopId = null
                                    loopDropdownExpanded = false
                                }
                            )
                            // Loop options
                            availableLoops.forEachIndexed { index, loop ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.loop_number_format, index + 1) +
                                                " (${loop.rounds}x)"
                                        )
                                    },
                                    onClick = {
                                        selectedLoopId = loop.id
                                        loopDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val setsValue = sets.toIntOrNull()
            // An empty target is saved as 0, which means no target.
            val targetValueValue = if (targetValue.isEmpty()) 0 else targetValue.toIntOrNull()
            val isValid = setsValue != null && targetValueValue != null
            TextButton(
                onClick = {
                    onSave(
                        programExercise.copy(
                            sets = setsValue!!,
                            targetValue = targetValueValue!!,
                            intervalSeconds = intervalSeconds.toIntOrNull() ?: 0,
                            loopId = selectedLoopId
                        )
                    )
                },
                enabled = isValid
            ) {
                Text(stringResource(R.string.save), color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun LoopSettingsDialog(
    loop: ProgramLoop?,
    onDismiss: () -> Unit,
    onSave: (rounds: Int, restBetweenRounds: Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var rounds by remember(loop) { mutableStateOf(loop?.rounds?.toString() ?: "3") }
    var restBetweenRounds by remember(loop) { mutableStateOf(loop?.restBetweenRounds?.toString() ?: "60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.loop_settings),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LabelledNumberStepper(
                    label = stringResource(R.string.loop_rounds),
                    value = rounds,
                    onValueChange = { rounds = it },
                    range = 1..99,
                    unit = "",
                )

                LabelledNumberStepper(
                    label = stringResource(R.string.loop_rest_between_rounds),
                    value = restBetweenRounds,
                    onValueChange = { restBetweenRounds = it },
                    range = 0..600,
                    unit = stringResource(R.string.unit_seconds),
                    step = 5,
                )

                // Delete button (only for existing loops)
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error))
                        )
                    ) {
                        Icon(AppIcons.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        },
        confirmButton = {
            val roundsValue = rounds.toIntOrNull()
            val isValid = roundsValue != null && roundsValue > 0
            TextButton(
                onClick = {
                    onSave(
                        roundsValue!!,
                        restBetweenRounds.toIntOrNull() ?: 0
                    )
                },
                enabled = isValid
            ) {
                Text(stringResource(R.string.save), color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
