package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import io.github.gonbei774.calisthenicsmemory.util.SearchUtils
import io.github.gonbei774.calisthenicsmemory.ui.components.common.MetricStepper
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SettingLinkRow
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SettingsCard
import io.github.gonbei774.calisthenicsmemory.ui.components.common.GroupBadge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import io.github.gonbei774.calisthenicsmemory.data.v2.linkedLeft
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import androidx.compose.ui.platform.LocalContext
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutDraft
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutRef
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSaveResult
import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmSearchField
import io.github.gonbei774.calisthenicsmemory.ui.components.common.screenBottomInsets
import io.github.gonbei774.calisthenicsmemory.ui.components.common.DialogSystemBars
import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmTopBar
import io.github.gonbei774.calisthenicsmemory.ui.components.common.NumberStepper
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SetMarks
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
    val context = LocalContext.current
    val showWarmUp = remember { WorkoutPreferences(context).isWarmUpMarkingEnabled() }
    val showEffort = remember { WorkoutPreferences(context).isEffortRatingEnabled() }
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

    // Edge to edge like the main window, so the keyboard is applied once, by screenBottomInsets (#280).
    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        DialogSystemBars()
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val topBarScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            val current = state
            val draft = current?.let(WorkoutEditorModel::toDraft)
            Scaffold(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .screenBottomInsets()
                    .nestedScroll(topBarScroll.nestedScrollConnection),
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
                        showWarmUp = showWarmUp,
                        showEffort = showEffort,
                    )
                }
            }
        }

        if (pickExercise) {
            io.github.gonbei774.calisthenicsmemory.ui.components.common.ExercisePicker(
                viewModel = viewModel,
                exercises = exercises,
                title = stringResource(R.string.select_exercise),
                onDismiss = { pickExercise = false },
                onAdd = { picked ->
                    // Each chosen exercise joins the workout, in the order they were chosen.
                    state = picked.fold(state) { draft, exercise -> draft?.let { WorkoutEditorModel.withExercise(it, exercise) } }
                    pickExercise = false
                },
            )
        }
        message?.let { text ->
            AlertDialog(
                onDismissRequest = { message = null },
                text = { Text(stringResource(text)) },
                confirmButton = { Button(onClick = { message = null }) { Text(stringResource(R.string.ok)) } },
            )
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text(stringResource(R.string.workout_edit_delete_title)) },
                text = { Text(stringResource(R.string.workout_edit_delete_message)) },
                confirmButton = {
                    Button(onClick = {
                        confirmDelete = false
                        state?.let(WorkoutEditorModel::toDraft)?.let(::save)
                    }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) { Text(stringResource(R.string.delete)) }
                },
                dismissButton = { OutlinedButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
            )
        }
        if (confirmDiscard) {
            AlertDialog(
                onDismissRequest = { confirmDiscard = false },
                title = { Text(stringResource(R.string.discard_changes_title)) },
                text = { Text(stringResource(R.string.discard_changes_message)) },
                confirmButton = {
                    Button(onClick = {
                        confirmDiscard = false
                        onClose()
                    }) { Text(stringResource(R.string.discard)) }
                },
                dismissButton = { OutlinedButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorContent(
    state: EditorState,
    padding: PaddingValues,
    onChange: (EditorState) -> Unit,
    onAddExercise: () -> Unit,
    showWarmUp: Boolean = false,
    showEffort: Boolean = false,
) {
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).testTag(WORKOUT_EDITOR_LIST_TAG),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SettingsCard {
                SettingLinkRow(
                    title = stringResource(R.string.date_format, formatDate(state.date, LocalConfiguration.current.locales[0])),
                    onClick = { pickDate = true },
                )
                SettingLinkRow(
                    title = stringResource(R.string.time_format, state.time.format(TIME)),
                    onClick = { pickTime = true },
                )
                OutlinedTextField(
                    value = state.comment,
                    onValueChange = { onChange(state.copy(comment = it)) },
                    label = { Text(stringResource(R.string.comment)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    maxLines = 3,
                )
            }
        }
        itemsIndexed(state.exercises) { exerciseIndex, exercise ->
            ExerciseCard(
                exercise = exercise,
                // Old-format workouts cannot hold a warm-up mark (ADR 0012).
                showWarmUp = !state.legacy && showWarmUp,
                showEffort = !state.legacy && showEffort,
                marksAllowed = !state.legacy,
                // A legacy record stores one value for both sides, so only a v2 workout shows a field per side.
                perSideAllowed = !state.legacy,
                onSetChange = { setIndex, set -> onChange(WorkoutEditorModel.withSet(state, exerciseIndex, setIndex, set)) },
                onRemoveSet = { setIndex -> onChange(WorkoutEditorModel.withoutSet(state, exerciseIndex, setIndex)) },
                onAddSet = { onChange(WorkoutEditorModel.withNewSet(state, exerciseIndex)) },
                onRemove = { onChange(WorkoutEditorModel.withoutExercise(state, exerciseIndex)) },
            )
        }
        item {
            FilledTonalButton(onClick = onAddExercise, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
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
                Button(onClick = {
                    picker.selectedDateMillis?.let { onChange(state.copy(date = LocalDate.ofEpochDay(it / 86_400_000))) }
                    pickDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { OutlinedButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = picker) }
    }
    if (pickTime) {
        val picker = rememberTimePickerState(initialHour = state.time.hour, initialMinute = state.time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = {
                Button(onClick = {
                    onChange(state.copy(time = LocalTime.of(picker.hour, picker.minute)))
                    pickTime = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { OutlinedButton(onClick = { pickTime = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(state = picker) },
        )
    }
}

@Composable
private fun ExerciseCard(
    exercise: EditorExercise,
    showWarmUp: Boolean,
    showEffort: Boolean,
    marksAllowed: Boolean,
    perSideAllowed: Boolean,
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
                        NumberStepper(value = set.value, onValueChange = { onSetChange(index, set.copy(value = it)) }, unit = unit, placeholder = "–")
                        Text(stringResource(R.string.left_value_label), style = MaterialTheme.typography.bodyMedium)
                        NumberStepper(value = set.valueLeft, onValueChange = { onSetChange(index, set.copy(valueLeft = it)) }, unit = unit, placeholder = "–")
                    } else {
                        NumberStepper(value = set.value, onValueChange = { onSetChange(index, set.copy(value = it)) }, unit = unit, placeholder = "–")
                    }
                    val perSide = exercise.unilateral && perSideAllowed
                    val right = if (perSide) stringResource(R.string.right_colon) + " " else ""
                    val left = if (perSide) stringResource(R.string.left_colon) + " " else ""
                    if (exercise.showDistance) {
                        MetricField(right + stringResource(R.string.distance_input_label), set.distanceCm, KeyboardType.Number, signed = true) {
                            onSetChange(index, set.copy(distanceCm = it, distanceLeftCm = linkedLeft(set.distanceCm, it, set.distanceLeftCm)))
                        }
                        if (perSide) {
                            MetricField(left + stringResource(R.string.distance_input_label), set.distanceLeftCm, KeyboardType.Number, signed = true) {
                                onSetChange(index, set.copy(distanceLeftCm = it))
                            }
                        }
                    }
                    if (exercise.showWeight) {
                        MetricField(right + stringResource(R.string.weight_input_label), set.weightKg, KeyboardType.Decimal) {
                            onSetChange(index, set.copy(weightKg = it, weightLeftKg = linkedLeft(set.weightKg, it, set.weightLeftKg)))
                        }
                        if (perSide) {
                            MetricField(left + stringResource(R.string.weight_input_label), set.weightLeftKg, KeyboardType.Decimal) {
                                onSetChange(index, set.copy(weightLeftKg = it))
                            }
                        }
                    }
                    if (exercise.showAssistance) {
                        MetricField(right + stringResource(R.string.assistance_input_label), set.assistanceKg, KeyboardType.Decimal) {
                            onSetChange(index, set.copy(assistanceKg = it, assistanceLeftKg = linkedLeft(set.assistanceKg, it, set.assistanceLeftKg)))
                        }
                        if (perSide) {
                            MetricField(left + stringResource(R.string.assistance_input_label), set.assistanceLeftKg, KeyboardType.Decimal) {
                                onSetChange(index, set.copy(assistanceLeftKg = it))
                            }
                        }
                    }
                    // Shown while a setting is on, and always for a marked or rated set, so it can be removed.
                    val warmUpShown = marksAllowed && (showWarmUp || set.warmUp)
                    val effortShown = marksAllowed && (showEffort || set.effortLeft != null)
                    if (warmUpShown || effortShown) {
                        SetMarks(
                            set.warmUp,
                            onWarmUp = { onSetChange(index, set.copy(warmUp = it)) },
                            showWarmUp = warmUpShown,
                            effortLeft = set.effortLeft,
                            dynamic = exercise.dynamic,
                            onEffort = if (effortShown) { rating -> onSetChange(index, set.copy(effortLeft = rating)) } else null,
                        )
                    }
                }
            }
            FilledTonalButton(onClick = onAddSet) {
                Icon(AppIcons.Add, contentDescription = null)
                Text(stringResource(R.string.workout_edit_add_set), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** A distance (whole centimetres, may be negative) or a load (kilograms with up to three decimals). */
@Composable
private fun MetricField(label: String, value: String, keyboard: KeyboardType, signed: Boolean = false, onValueChange: (String) -> Unit) {
    MetricStepper(
        label = label,
        value = value,
        onValueChange = { text ->
            val allowed = if (signed) Regex("""-?\d*""") else Regex("""\d*([.,]\d{0,3})?""")
            if (text.matches(allowed)) onValueChange(text)
        },
        keyboardType = keyboard,
        decimals = !signed,
        // A distance may go below zero; a load may not.
        min = if (signed) null else 0.0,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
