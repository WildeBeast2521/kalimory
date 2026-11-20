@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.ui.components.common.DestructiveButton
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SettingsGroup
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SettingsCard
import io.github.gonbei774.calisthenicsmemory.ui.components.common.ExercisePicker
import io.github.gonbei774.calisthenicsmemory.ui.components.common.ExerciseCheckRow
import io.github.gonbei774.calisthenicsmemory.ui.components.common.ExerciseBadges
import io.github.gonbei774.calisthenicsmemory.ui.components.common.isLargeFontScale
import io.github.gonbei774.calisthenicsmemory.ui.components.common.GroupBadge
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SwipeToDeleteBackground
import io.github.gonbei774.calisthenicsmemory.ui.components.targetText
import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmSearchField
import io.github.gonbei774.calisthenicsmemory.ui.components.common.TopBarAction
import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmTopBar
import io.github.gonbei774.calisthenicsmemory.ui.components.common.NumberStepButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LoadingIndicator
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgramExercise
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.util.SearchUtils
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import io.github.gonbei774.calisthenicsmemory.viewmodel.IntervalProgramDraft

@Composable
fun IntervalEditScreen(
    viewModel: TrainingViewModel,
    programId: Long?,  // null = new program
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val exercises by viewModel.exercises.collectAsState()
    val lazyListState = rememberLazyListState()

    // Load existing program if editing
    var program by remember { mutableStateOf<IntervalProgram?>(null) }
    var programExercises by remember { mutableStateOf<List<IntervalProgramExercise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(programId != null) }

    // Form state
    var name by remember { mutableStateOf("") }
    var workSeconds by remember { mutableStateOf("40") }
    var restSeconds by remember { mutableStateOf("20") }
    var rounds by remember { mutableStateOf("3") }
    var roundRestSeconds by remember { mutableStateOf("60") }

    // Dialog states
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    // Track original values for existing programs
    var originalName by remember { mutableStateOf("") }
    var originalWorkSeconds by remember { mutableStateOf("40") }
    var originalRestSeconds by remember { mutableStateOf("20") }
    var originalRounds by remember { mutableStateOf("3") }
    var originalRoundRestSeconds by remember { mutableStateOf("60") }
    var originalProgramExercises by remember { mutableStateOf<List<IntervalProgramExercise>>(emptyList()) }

    // Load existing program data
    LaunchedEffect(programId) {
        if (programId != null) {
            program = viewModel.getIntervalProgramById(programId)
            program?.let {
                name = it.name
                workSeconds = it.workSeconds.toString()
                restSeconds = it.restSeconds.toString()
                rounds = it.rounds.toString()
                roundRestSeconds = it.roundRestSeconds.toString()
                originalName = it.name
                originalWorkSeconds = it.workSeconds.toString()
                originalRestSeconds = it.restSeconds.toString()
                originalRounds = it.rounds.toString()
                originalRoundRestSeconds = it.roundRestSeconds.toString()
            }
            val loadedExercises = viewModel.getIntervalProgramExercisesSync(programId)
            programExercises = loadedExercises
            originalProgramExercises = loadedExercises
            isLoading = false
        }
    }

    // Check for unsaved changes
    val hasUnsavedChanges = if (programId == null) {
        name.isNotBlank() || programExercises.isNotEmpty()
    } else {
        name != originalName ||
            workSeconds != originalWorkSeconds ||
            restSeconds != originalRestSeconds ||
            rounds != originalRounds ||
            roundRestSeconds != originalRoundRestSeconds ||
            programExercises != originalProgramExercises
    }

    fun handleBackPress() {
        if (hasUnsavedChanges) {
            showDiscardConfirmDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler { handleBackPress() }

    // Exercise map for display
    val exerciseMap = remember(exercises) {
        exercises.associateBy { it.id }
    }

    // One save at a time: a second tap while saving would apply the same changes twice.
    var saving by remember { mutableStateOf(false) }
    fun saveProgram() {
        if (saving) return
        saving = true
        coroutineScope.launch {
            val draft = IntervalProgramDraft(
                program = program,
                name = name,
                workSeconds = workSeconds.toIntOrNull() ?: 40,
                restSeconds = restSeconds.toIntOrNull() ?: 20,
                rounds = rounds.toIntOrNull() ?: 3,
                roundRestSeconds = roundRestSeconds.toIntOrNull() ?: 60,
                originalExercises = originalProgramExercises,
                exercises = programExercises,
            )
            if (viewModel.saveIntervalProgramDraft(programId, draft)) onSaved() else saving = false
        }
    }

    val isValid = name.isNotBlank() && programExercises.isNotEmpty() &&
        (workSeconds.toIntOrNull() ?: 0) > 0 &&
        (restSeconds.toIntOrNull() ?: 0) >= 0 &&
        (rounds.toIntOrNull() ?: 0) > 0 &&
        (roundRestSeconds.toIntOrNull() ?: 0) >= 0

    val topBarScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(topBarScroll.nestedScrollConnection),
        topBar = {
            CalmTopBar(
                title = stringResource(if (programId == null) R.string.new_interval_program else R.string.edit_interval_program),
                onBack = { handleBackPress() },
                scrollBehavior = topBarScroll,
            ) {
                TopBarAction(stringResource(R.string.save), enabled = isValid && !saving) { saveProgram() }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            // Number of header items before the exercise list
            // Name + Timer Settings title + timer fields (3 or 4) + Exercises title
            // Rest time row is hidden when there's only one exercise
            val headerItemCount = if (programExercises.size > 1) 7 else 6

            val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
                val fromIndex = from.index - headerItemCount
                val toIndex = to.index - headerItemCount
                if (fromIndex >= 0 && toIndex >= 0 && fromIndex < programExercises.size && toIndex < programExercises.size) {
                    val reorderedList = programExercises.toMutableList()
                    val item = reorderedList.removeAt(fromIndex)
                    reorderedList.add(toIndex, item)
                    programExercises = reorderedList.mapIndexed { index, pe ->
                        pe.copy(sortOrder = index)
                    }
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Program Name
                item {
                    SettingsCard {
OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.interval_program_name)) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        singleLine = true
                    )
}
                }

                // Timer settings: one card of rows (rest between exercises only with two or more)
                item {
                    SettingsGroup(title = stringResource(R.string.interval_timer_settings)) {
                    TimerSettingRow(
                        label = stringResource(R.string.interval_work_seconds),
                        value = workSeconds,
                        onValueChange = { workSeconds = it.filter { c -> c.isDigit() } },
                        suffix = stringResource(R.string.interval_seconds_suffix)
                    )
                    if (programExercises.size > 1) {
                        TimerSettingRow(
                            label = stringResource(R.string.interval_rest_seconds),
                            value = restSeconds,
                            onValueChange = { restSeconds = it.filter { c -> c.isDigit() } },
                            suffix = stringResource(R.string.interval_seconds_suffix)
                        )
                    }
                    TimerSettingRow(
                        label = stringResource(R.string.interval_rounds),
                        value = rounds,
                        onValueChange = { rounds = it.filter { c -> c.isDigit() } },
                        suffix = stringResource(R.string.interval_rounds_suffix),
                        step = 1,
                        min = 1
                    )
                    TimerSettingRow(
                        label = stringResource(R.string.interval_round_rest_seconds),
                        value = roundRestSeconds,
                        onValueChange = { roundRestSeconds = it.filter { c -> c.isDigit() } },
                        suffix = stringResource(R.string.interval_seconds_suffix)
                    )
                    }
                }

                // Exercises Section
                item {
                    Text(
                        text = stringResource(R.string.interval_exercises),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Exercise list
                if (programExercises.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(
                                text = stringResource(R.string.interval_exercises_required),
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(
                        count = programExercises.size,
                        key = { index -> "ex_${programExercises[index].id}" }
                    ) { index ->
                        val pe = programExercises[index]
                        val exercise = exerciseMap[pe.exerciseId]
                        if (exercise != null) {
                            ReorderableItem(reorderableLazyListState, key = "ex_${pe.id}") { isDragging ->
                                val elevation by animateDpAsState(
                                    targetValue = if (isDragging) 4.dp else 0.dp,
                                    label = "elevation"
                                )
                                val dismissState = rememberSwipeToDismissBoxState()
                                LaunchedEffect(dismissState.currentValue) {
                                    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                        programExercises = programExercises.filter { it != pe }
                                            .mapIndexed { i, e -> e.copy(sortOrder = i) }
                                    }
                                }
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = { SwipeToDeleteBackground(dismissState, MaterialTheme.shapes.large) },
                                    enableDismissFromStartToEnd = false,
                                    enableDismissFromEndToStart = true
                                ) {
                                    IntervalExerciseItem(
                                        index = index + 1,
                                        exercise = exercise,
                                        isDragging = isDragging,
                                        elevation = elevation,
                                        dragHandle = { Modifier.longPressDraggableHandle() }
                                    )
                                }
                            }
                        }
                    }
                }

                // Add exercise button
                item {
                    FilledTonalButton(
                        onClick = { showAddExerciseDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            AppIcons.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.interval_add_exercise))
                    }
                }

                // Note about no recording
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                        ),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = stringResource(R.string.interval_no_record_note),
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Delete program button (only for existing programs)
                if (programId != null) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        DestructiveButton(
                            text = stringResource(R.string.interval_delete_program),
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            icon = AppIcons.Delete,
                        )
                    }
                }
            }
        }
    }

    // Add Exercise Dialog (batch selection with checkboxes)
    if (showAddExerciseDialog) {
        ExercisePicker(
            viewModel = viewModel,
            exercises = exercises,
            title = stringResource(R.string.interval_add_exercise),
            onDismiss = { showAddExerciseDialog = false },
            onAdd = { selectedExercisesList ->
                val newEntries = selectedExercisesList.mapIndexed { index, exercise ->
                    IntervalProgramExercise(
                        id = System.currentTimeMillis() + index,
                        programId = programId ?: 0L,
                        exerciseId = exercise.id,
                        sortOrder = programExercises.size + index
                    )
                }
                programExercises = programExercises + newEntries
                showAddExerciseDialog = false
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.interval_delete_program),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.interval_delete_program_warning, name),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        programId?.let { viewModel.deleteIntervalProgram(it) }
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Discard Changes Confirmation Dialog
    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.discard_changes_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.discard_changes_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun TimerSettingRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    step: Int = 5,
    min: Int = 0,
) {
    // The app's stepper: − value unit +, with the number still typeable. Fixed widths keep every
    // row's controls in one column.
    fun stepped(delta: Int): Boolean {
        val next = ((value.toIntOrNull() ?: min) + delta).coerceAtLeast(min)
        onValueChange(next.toString())
        return next > min || delta > 0
    }
    val large = isLargeFontScale()
    val labelText: @Composable (Modifier) -> Unit = { modifier ->
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier
        )
    }
    val stepper: @Composable () -> Unit = {
        NumberStepButton(increment = false, contentDescription = null, enabled = (value.toIntOrNull() ?: min) > min) { stepped(-step) }
        Row(
            modifier = Modifier.widthIn(min = 112.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                // A fixed width that fits four digits: an intrinsic width let the text scroll inside
                // the field and hid it until scrolled. Wider at large text, where four digits are wider.
                modifier = Modifier.width(if (large) 96.dp else 52.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = suffix,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        NumberStepButton(increment = true, contentDescription = null) { stepped(step) }
    }
    // At large text the label goes above the stepper, so neither is squeezed or clipped.
    if (large) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            labelText(Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { stepper() }
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            labelText(Modifier.weight(1f))
            stepper()
        }
    }
}

@Composable
private fun IntervalExerciseItem(
    index: Int,
    exercise: Exercise,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    dragHandle: @Composable () -> Modifier
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(
                AppIcons.DragHandle,
                contentDescription = stringResource(R.string.todo_drag_to_reorder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .then(dragHandle())
            )

            // Number
            Text(
                text = "$index.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Exercise info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!exercise.description.isNullOrBlank()) {
                    Text(
                        text = exercise.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
