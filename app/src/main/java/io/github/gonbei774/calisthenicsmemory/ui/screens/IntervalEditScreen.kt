@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package io.github.gonbei774.calisthenicsmemory.ui.screens

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

    fun saveProgram() {
        coroutineScope.launch {
            val workSecondsVal = workSeconds.toIntOrNull() ?: 40
            val restSecondsVal = restSeconds.toIntOrNull() ?: 20
            val roundsVal = rounds.toIntOrNull() ?: 3
            val roundRestSecondsVal = roundRestSeconds.toIntOrNull() ?: 60

            if (programId != null) {
                // Update existing program
                program?.copy(
                    name = name,
                    workSeconds = workSecondsVal,
                    restSeconds = restSecondsVal,
                    rounds = roundsVal,
                    roundRestSeconds = roundRestSecondsVal
                )?.let { updatedProgram ->
                    viewModel.updateIntervalProgram(updatedProgram)
                }

                // Sync exercises
                val originalIds = originalProgramExercises.map { it.id }.toSet()
                val currentIds = programExercises.map { it.id }.toSet()

                // Delete removed exercises
                val deletedIds = originalIds - currentIds
                deletedIds.forEach { id ->
                    originalProgramExercises.find { it.id == id }?.let { pe ->
                        viewModel.deleteIntervalProgramExercise(pe)
                    }
                }

                // Add new exercises
                val addedExercises = programExercises.filter { it.id !in originalIds }
                addedExercises.forEach { pe ->
                    viewModel.addIntervalProgramExerciseSync(
                        programId = programId,
                        exerciseId = pe.exerciseId,
                        sortOrder = pe.sortOrder
                    )
                }

                // Update modified exercises (sortOrder changes)
                val existingExercises = programExercises.filter { it.id in originalIds }
                existingExercises.forEach { pe ->
                    val original = originalProgramExercises.find { it.id == pe.id }
                    if (original != null && pe != original) {
                        viewModel.updateIntervalProgramExercise(pe)
                    }
                }
            } else {
                // Create new program
                val newProgramId = viewModel.createIntervalProgramAndGetId(
                    name = name,
                    workSeconds = workSecondsVal,
                    restSeconds = restSecondsVal,
                    rounds = roundsVal,
                    roundRestSeconds = roundRestSecondsVal
                )
                if (newProgramId != null) {
                    programExercises.forEach { pe ->
                        viewModel.addIntervalProgramExerciseSync(
                            programId = newProgramId,
                            exerciseId = pe.exerciseId,
                            sortOrder = pe.sortOrder
                        )
                    }
                }
            }
            onSaved()
        }
    }

    val isValid = name.isNotBlank() && programExercises.isNotEmpty() &&
        (workSeconds.toIntOrNull() ?: 0) > 0 &&
        (restSeconds.toIntOrNull() ?: 0) >= 0 &&
        (rounds.toIntOrNull() ?: 0) > 0 &&
        (roundRestSeconds.toIntOrNull() ?: 0) >= 0

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { handleBackPress() }) {
                        Icon(
                            AppIcons.Back,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(
                            if (programId == null) R.string.new_interval_program else R.string.edit_interval_program
                        ),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { saveProgram() },
                        enabled = isValid
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            color = if (isValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.interval_program_name)) },
                        modifier = Modifier.fillMaxWidth(),
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

                // Timer Settings Section
                item {
                    Text(
                        text = stringResource(R.string.interval_timer_settings),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Work Time
                item {
                    TimerSettingRow(
                        label = stringResource(R.string.interval_work_seconds),
                        value = workSeconds,
                        onValueChange = { workSeconds = it.filter { c -> c.isDigit() } },
                        suffix = stringResource(R.string.interval_seconds_suffix)
                    )
                }

                // Rest Time (only shown when 2+ exercises; irrelevant for single-exercise programs)
                if (programExercises.size > 1) {
                    item {
                        TimerSettingRow(
                            label = stringResource(R.string.interval_rest_seconds),
                            value = restSeconds,
                            onValueChange = { restSeconds = it.filter { c -> c.isDigit() } },
                            suffix = stringResource(R.string.interval_seconds_suffix)
                        )
                    }
                }

                // Rounds
                item {
                    TimerSettingRow(
                        label = stringResource(R.string.interval_rounds),
                        value = rounds,
                        onValueChange = { rounds = it.filter { c -> c.isDigit() } },
                        suffix = stringResource(R.string.interval_rounds_suffix)
                    )
                }

                // Round Rest
                item {
                    TimerSettingRow(
                        label = stringResource(R.string.interval_round_rest_seconds),
                        value = roundRestSeconds,
                        onValueChange = { roundRestSeconds = it.filter { c -> c.isDigit() } },
                        suffix = stringResource(R.string.interval_seconds_suffix)
                    )
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
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.interval_exercises_required),
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
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
                                    backgroundContent = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    color = MaterialTheme.colorScheme.error,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                AppIcons.Delete,
                                                contentDescription = stringResource(R.string.delete),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
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
                    Button(
                        onClick = { showAddExerciseDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.interval_no_record_note),
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                // Delete program button (only for existing programs)
                if (programId != null) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
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
                            Text(stringResource(R.string.interval_delete_program))
                        }
                    }
                }
            }
        }
    }

    // Add Exercise Dialog (batch selection with checkboxes)
    if (showAddExerciseDialog) {
        AddExerciseToIntervalDialog(
            viewModel = viewModel,
            exercises = exercises,
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Discard Changes Confirmation Dialog
    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    suffix: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.width(80.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 14.sp
                )
            )
            Text(
                text = suffix,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        shape = RoundedCornerShape(12.dp),
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
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .then(dragHandle())
            )

            // Number
            Text(
                text = "$index.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Exercise info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!exercise.description.isNullOrBlank()) {
                    Text(
                        text = exercise.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

// Batch exercise selection dialog for interval mode (checkboxes + badges like ToDo)
@Composable
private fun AddExerciseToIntervalDialog(
    viewModel: TrainingViewModel,
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onAdd: (List<Exercise>) -> Unit
) {
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()

    var selectedExercises by remember { mutableStateOf(listOf<Long>()) }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember(exercises, searchQuery) {
        SearchUtils.searchExercises(exercises, searchQuery)
    }
    val listState = rememberLazyListState()

    LaunchedEffect(searchQuery, searchResults) {
        if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = {
            Text(
                text = stringResource(R.string.interval_add_exercise),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (exercises.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_exercises_available),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)
                ) {
                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                AppIcons.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        AppIcons.Close,
                                        contentDescription = stringResource(R.string.clear),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Exercise list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (searchQuery.isNotBlank()) {
                            if (searchResults.isEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.no_results),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                items(
                                    count = searchResults.size,
                                    key = { index -> searchResults[index].id }
                                ) { index ->
                                    val exercise = searchResults[index]
                                    IntervalExerciseSelectItem(
                                        exercise = exercise,
                                        isSelected = exercise.id in selectedExercises,
                                        onToggle = { exerciseId ->
                                            selectedExercises = if (exerciseId in selectedExercises) {
                                                selectedExercises - exerciseId
                                            } else {
                                                selectedExercises + exerciseId
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            items(
                                count = hierarchicalData.size,
                                key = { index -> hierarchicalData[index].groupName ?: "ungrouped" }
                            ) { index ->
                                val group = hierarchicalData[index]
                                if (group.exercises.isNotEmpty()) {
                                    IntervalSelectExerciseGroup(
                                        groupName = group.groupName,
                                        exercises = group.exercises,
                                        selectedExercises = selectedExercises,
                                        isExpanded = (group.groupName ?: "ungrouped") in expandedGroups,
                                        onExpandToggle = {
                                            viewModel.toggleGroupExpansion(group.groupName ?: "ungrouped")
                                        },
                                        onExerciseToggle = { exerciseId ->
                                            selectedExercises = if (exerciseId in selectedExercises) {
                                                selectedExercises - exerciseId
                                            } else {
                                                selectedExercises + exerciseId
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = selectedExercises.mapNotNull { id -> exercises.find { it.id == id } }
                    onAdd(selected)
                },
                enabled = selectedExercises.isNotEmpty()
            ) {
                Text(
                    text = stringResource(R.string.add),
                    color = if (selectedExercises.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
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
private fun IntervalExerciseSelectItem(
    exercise: Exercise,
    isSelected: Boolean,
    onToggle: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle(exercise.id) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Column(modifier = Modifier.padding(start = 4.dp, top = 10.dp)) {
                Text(
                    text = exercise.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Badges row
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (exercise.isFavorite) {
                        Icon(AppIcons.FavoriteFilled, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.tertiary)
                    }
                    if (exercise.targetSets != null && exercise.targetValue != null && exercise.sortOrder > 0) {
                        Text(
                            text = stringResource(R.string.level_format, exercise.sortOrder),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (exercise.laterality == "Unilateral") {
                        Text(
                            text = stringResource(R.string.one_sided),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (exercise.weightTrackingEnabled) {
                        Text(
                            text = stringResource(R.string.legend_weight),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (exercise.distanceTrackingEnabled) {
                        Text(
                            text = stringResource(R.string.legend_distance),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (exercise.assistanceTrackingEnabled) {
                        Text(
                            text = stringResource(R.string.legend_assistance),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Pink600
                        )
                    }
                }
                if (exercise.targetSets != null && exercise.targetValue != null) {
                    Text(
                        text = stringResource(
                            if (exercise.laterality == "Unilateral") R.string.target_format_unilateral else R.string.target_format,
                            exercise.targetSets!!,
                            exercise.targetValue!!,
                            stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)
                        ),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun IntervalSelectExerciseGroup(
    groupName: String?,
    exercises: List<Exercise>,
    selectedExercises: List<Long>,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onExerciseToggle: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // Group header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                onClick = onExpandToggle
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isExpanded)
                                AppIcons.ExpandMore
                            else
                                AppIcons.Forward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = groupName ?: stringResource(R.string.no_group),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "(${exercises.size})",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Exercise list with animation
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    exercises.forEach { exercise ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = exercise.id in selectedExercises,
                                onCheckedChange = { onExerciseToggle(exercise.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Column(modifier = Modifier.padding(start = 4.dp, top = 10.dp)) {
                                Text(
                                    text = exercise.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                // Badges row
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    if (exercise.isFavorite) {
                                        Icon(AppIcons.FavoriteFilled, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.tertiary)
                                    }
                                    if (exercise.targetSets != null && exercise.targetValue != null && exercise.sortOrder > 0) {
                                        Text(
                                            text = stringResource(R.string.level_format, exercise.sortOrder),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Text(
                                        text = stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (exercise.laterality == "Unilateral") {
                                        Text(
                                            text = stringResource(R.string.one_sided),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (exercise.weightTrackingEnabled) {
                                        Text(
                                            text = stringResource(R.string.legend_weight),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    if (exercise.distanceTrackingEnabled) {
                                        Text(
                                            text = stringResource(R.string.legend_distance),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    if (exercise.assistanceTrackingEnabled) {
                                        Text(
                                            text = stringResource(R.string.legend_assistance),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Pink600
                                        )
                                    }
                                }
                                if (exercise.targetSets != null && exercise.targetValue != null) {
                                    Text(
                                        text = stringResource(
                                            if (exercise.laterality == "Unilateral") R.string.target_format_unilateral else R.string.target_format,
                                            exercise.targetSets!!,
                                            exercise.targetValue!!,
                                            stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)
                                        ),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
