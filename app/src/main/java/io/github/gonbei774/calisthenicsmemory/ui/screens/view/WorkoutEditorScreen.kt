package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutDraft
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutRef
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSaveResult
import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmSearchField
import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmTopBar
import io.github.gonbei774.calisthenicsmemory.ui.components.common.NumberStepper
import io.github.gonbei774.calisthenicsmemory.ui.components.common.TopBarAction
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Tag of the editor's list, for tests that scroll it. */
const val WORKOUT_EDITOR_LIST_TAG = "workout-editor-list"

/**
 * Edits a whole saved workout (ADR 0009): its date, time and comment, each set, and which sets and
 * exercises it has. Nothing is written until Save, which saves everything in one transaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditorScreen(ref: WorkoutRef, viewModel: TrainingViewModel, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var library by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    // The workout as loaded, and what the user has made of it; null until loaded.
    var original by remember { mutableStateOf<WorkoutDraft?>(null) }
    var state by remember { mutableStateOf<EditorState?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var reloads by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var pickExercise by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val exercises by viewModel.exercises.collectAsState()

    LaunchedEffect(ref, reloads) {
        val draft = viewModel.loadWorkoutDraft(ref)
        library = viewModel.libraryExercises()
        original = draft
        state = draft?.let { WorkoutEditorModel.from(it, library, legacy = ref is WorkoutRef.Legacy) }
        loaded = true
    }

    val unchanged = original?.let { state == WorkoutEditorModel.from(it, library, legacy = ref is WorkoutRef.Legacy) } ?: true
    fun close() {
        if (unchanged) onClose() else confirmDiscard = true
    }

    fun save(edited: WorkoutDraft) {
        val from = original ?: return
        saving = true
        scope.launch {
            when (viewModel.saveWorkoutDraft(ref, from, edited)) {
                WorkoutSaveResult.SAVED, WorkoutSaveResult.DELETED, WorkoutSaveResult.UNCHANGED -> onClose()
                WorkoutSaveResult.STALE -> {
                    message = R.string.workout_edit_changed
                    reloads++
                }
                WorkoutSaveResult.TIME_TAKEN -> message = R.string.workout_edit_time_taken
                null -> {}
            }
            saving = false
        }
    }

    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val topBarScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            val current = state
            val draft = current?.let(WorkoutEditorModel::toDraft)
            Scaffold(
                modifier = Modifier.nestedScroll(topBarScroll.nestedScrollConnection),
                topBar = {
                    CalmTopBar(
                        title = stringResource(R.string.workout_edit_title),
                        onBack = { close() },
                        scrollBehavior = topBarScroll,
                        backIcon = AppIcons.Close,
                        backDescription = stringResource(R.string.cancel),
                    ) {
                        TopBarAction(stringResource(R.string.save), enabled = draft != null && !saving && !unchanged) {
                            if (draft == null) return@TopBarAction
                            if (draft.exercises.none { it.sets.isNotEmpty() }) confirmDelete = true else save(draft)
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                when {
                    !loaded -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    current == null -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.workout_edit_unavailable), style = MaterialTheme.typography.bodyLarge)
                    }
                    else -> EditorContent(
                        state = current,
                        padding = padding,
                        onChange = { state = it },
                        onAddExercise = { pickExercise = true },
                    )
                }
            }
        }

        if (pickExercise) {
            ExercisePicker(
                exercises = exercises,
                onPick = { exercise ->
                    state = state?.let { WorkoutEditorModel.withExercise(it, exercise) }
                    pickExercise = false
                },
                onDismiss = { pickExercise = false },
            )
        }
        message?.let { text ->
            AlertDialog(
                onDismissRequest = { message = null },
                text = { Text(stringResource(text)) },
                confirmButton = { TextButton(onClick = { message = null }) { Text(stringResource(R.string.ok)) } },
            )
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text(stringResource(R.string.workout_edit_delete_title)) },
                text = { Text(stringResource(R.string.workout_edit_delete_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        state?.let(WorkoutEditorModel::toDraft)?.let(::save)
                    }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
            )
        }
        if (confirmDiscard) {
            AlertDialog(
                onDismissRequest = { confirmDiscard = false },
                title = { Text(stringResource(R.string.discard_changes_title)) },
                text = { Text(stringResource(R.string.discard_changes_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDiscard = false
                        onClose()
                    }) { Text(stringResource(R.string.discard)) }
                },
                dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorContent(state: EditorState, padding: PaddingValues, onChange: (EditorState) -> Unit, onAddExercise: () -> Unit) {
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).testTag(WORKOUT_EDITOR_LIST_TAG),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.date_format, formatDate(state.date, LocalConfiguration.current.locales[0])))
                }
                OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.time_format, state.time.format(TIME)))
                }
                OutlinedTextField(
                    value = state.comment,
                    onValueChange = { onChange(state.copy(comment = it)) },
                    label = { Text(stringResource(R.string.comment)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
            }
        }
        itemsIndexed(state.exercises) { exerciseIndex, exercise ->
            ExerciseCard(
                exercise = exercise,
                onSetChange = { setIndex, set -> onChange(WorkoutEditorModel.withSet(state, exerciseIndex, setIndex, set)) },
                onRemoveSet = { setIndex -> onChange(WorkoutEditorModel.withoutSet(state, exerciseIndex, setIndex)) },
                onAddSet = { onChange(WorkoutEditorModel.withNewSet(state, exerciseIndex)) },
                onRemove = { onChange(WorkoutEditorModel.withoutExercise(state, exerciseIndex)) },
            )
        }
        item {
            OutlinedButton(onClick = onAddExercise, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Icon(AppIcons.Add, contentDescription = null)
                Text(stringResource(R.string.workout_edit_add_exercise), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    if (pickDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = state.date.toEpochDay() * 86_400_000)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onChange(state.copy(date = LocalDate.ofEpochDay(it / 86_400_000))) }
                    pickDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = picker) }
    }
    if (pickTime) {
        val picker = rememberTimePickerState(initialHour = state.time.hour, initialMinute = state.time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(state.copy(time = LocalTime.of(picker.hour, picker.minute)))
                    pickTime = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(state = picker) },
        )
    }
}

@Composable
private fun ExerciseCard(
    exercise: EditorExercise,
    onSetChange: (Int, EditorSet) -> Unit,
    onRemoveSet: (Int) -> Unit,
    onAddSet: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(AppIcons.Delete, contentDescription = stringResource(R.string.workout_edit_remove_exercise, exercise.name))
                }
            }
            val unit = stringResource(if (exercise.dynamic) R.string.unit_reps else R.string.unit_seconds)
            exercise.sets.forEachIndexed { index, set ->
                if (index > 0) HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.set_number_format, index + 1),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onRemoveSet(index) }) {
                            Icon(AppIcons.Close, contentDescription = stringResource(R.string.workout_edit_remove_set, index + 1))
                        }
                    }
                    if (exercise.unilateral) {
                        Text(stringResource(R.string.right_value_label), style = MaterialTheme.typography.bodyMedium)
                        NumberStepper(value = set.value, onValueChange = { onSetChange(index, set.copy(value = it)) }, unit = unit)
                        Text(stringResource(R.string.left_value_label), style = MaterialTheme.typography.bodyMedium)
                        NumberStepper(value = set.valueLeft, onValueChange = { onSetChange(index, set.copy(valueLeft = it)) }, unit = unit)
                    } else {
                        NumberStepper(value = set.value, onValueChange = { onSetChange(index, set.copy(value = it)) }, unit = unit)
                    }
                    if (exercise.showDistance) {
                        MetricField(R.string.distance_input_label, set.distanceCm, KeyboardType.Number, signed = true) {
                            onSetChange(index, set.copy(distanceCm = it))
                        }
                    }
                    if (exercise.showWeight) {
                        MetricField(R.string.weight_input_label, set.weightKg, KeyboardType.Decimal) { onSetChange(index, set.copy(weightKg = it)) }
                    }
                    if (exercise.showAssistance) {
                        MetricField(R.string.assistance_input_label, set.assistanceKg, KeyboardType.Decimal) {
                            onSetChange(index, set.copy(assistanceKg = it))
                        }
                    }
                }
            }
            TextButton(onClick = onAddSet) {
                Icon(AppIcons.Add, contentDescription = null)
                Text(stringResource(R.string.workout_edit_add_set), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** A distance (whole centimetres, may be negative) or a load (kilograms with up to three decimals). */
@Composable
private fun MetricField(label: Int, value: String, keyboard: KeyboardType, signed: Boolean = false, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            val allowed = if (signed) Regex("""-?\d*""") else Regex("""\d*([.,]\d{0,3})?""")
            if (text.matches(allowed)) onValueChange(text)
        },
        label = { Text(stringResource(label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    )
}

@Composable
private fun ExercisePicker(exercises: List<Exercise>, onPick: (Exercise) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_exercise)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CalmSearchField(query = query, onQueryChange = { query = it }, placeholder = stringResource(R.string.search_exercise))
                val shown = exercises.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }.sortedBy { it.name }
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(shown, key = { it.id }) { exercise ->
                        Text(
                            exercise.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().clickable { onPick(exercise) }.padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
