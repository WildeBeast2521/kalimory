package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.BodyWeight
import io.github.gonbei774.calisthenicsmemory.ui.components.common.AppFab
import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmTopBar
import io.github.gonbei774.calisthenicsmemory.ui.components.common.DestructiveButton
import io.github.gonbei774.calisthenicsmemory.ui.components.common.EmptyState
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SwipeToDeleteBackground
import io.github.gonbei774.calisthenicsmemory.ui.components.common.kgStringToG
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.util.rememberToday
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import java.math.BigDecimal
import java.time.LocalDate

/** A weight in grams as a person reads it, without trailing zeros: 62850 is "62.85", 65000 is "65". */
internal fun kgText(grams: Int): String = BigDecimal(grams).movePointLeft(3).stripTrailingZeros().toPlainString()

/** The body weight list (ADR 0016): newest first, to add, change or delete an entry. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyWeightScreen(viewModel: TrainingViewModel, onNavigateBack: () -> Unit) {
    val weights by viewModel.bodyWeights.collectAsState()
    val lastImportCount by viewModel.lastImportCount.collectAsState()
    BodyWeightContent(
        weights = weights,
        today = rememberToday(),
        onSave = { date, grams, replacing -> viewModel.saveBodyWeight(date, grams, replacing) },
        onDelete = { viewModel.deleteBodyWeight(it.id) },
        onNavigateBack = onNavigateBack,
        footer = {
            // The entries the latest import added can be taken back as a whole; typed and edited ones never are.
            if (lastImportCount > 0) {
                OutlinedButton(onClick = { viewModel.undoBodyWeightImport() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.body_weight_import_undo, lastImportCount))
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BodyWeightContent(
    weights: List<BodyWeight>,
    /** The day a new entry starts on. */
    today: LocalDate,
    onSave: (LocalDate, Int, BodyWeight?) -> Unit,
    onDelete: (BodyWeight) -> Unit,
    onNavigateBack: () -> Unit,
    /** Shown under the list, for example while an import can be undone. */
    footer: @Composable () -> Unit = {},
) {
    val locale = LocalConfiguration.current.locales[0]
    // The entry being edited by id, or -1 for a new one; null while no dialog is open.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<BodyWeight?>(null) }
    val topBarScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(topBarScroll.nestedScrollConnection),
        topBar = { CalmTopBar(title = stringResource(R.string.body_weight_title), onBack = onNavigateBack, scrollBehavior = topBarScroll) },
        floatingActionButton = { AppFab(onClick = { editingId = -1L }, contentDescription = stringResource(R.string.body_weight_add)) },
    ) { padding ->
        if (weights.isEmpty()) {
            Box(Modifier.padding(padding)) { EmptyState(stringResource(R.string.body_weight_empty)) }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, top = Spacing.m, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                items(weights.asReversed(), key = { it.id }) { entry ->
                    BodyWeightRow(entry, formatDate(LocalDate.parse(entry.date), locale), onEdit = { editingId = entry.id }, onDelete = { deleting = entry })
                }
                item { footer() }
            }
        }
    }

    editingId?.let { id ->
        val editing = weights.firstOrNull { it.id == id }
        BodyWeightDialog(
            editing = editing,
            weights = weights,
            today = today,
            onDismiss = { editingId = null },
            onSave = { date, grams -> onSave(date, grams, editing); editingId = null },
        )
    }
    deleting?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.body_weight_delete_title)) },
            text = { Text(stringResource(R.string.body_weight_delete_message, formatDate(LocalDate.parse(entry.date), locale))) },
            confirmButton = { DestructiveButton(stringResource(R.string.delete), onClick = { onDelete(entry); deleting = null }) },
            dismissButton = { OutlinedButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyWeightRow(entry: BodyWeight, date: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
            false // the entry goes only once the dialog is confirmed
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeToDeleteBackground(dismissState, MaterialTheme.shapes.large) },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
    ) {
        Card(
            Modifier.fillMaxWidth().clickable(onClick = onEdit),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.large,
        ) {
            Row(Modifier.fillMaxWidth().padding(Spacing.l), verticalAlignment = Alignment.CenterVertically) {
                Text(date, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text("${kgText(entry.weightG)} ${stringResource(R.string.unit_kg)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/** Date and weight of one entry. A date that already has an entry is said so: saving then changes that entry. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BodyWeightDialog(editing: BodyWeight?, weights: List<BodyWeight>, today: LocalDate, onDismiss: () -> Unit, onSave: (LocalDate, Int) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    var date by rememberSaveable { mutableStateOf(editing?.date ?: today.toString()) }
    var text by rememberSaveable { mutableStateOf(editing?.weightG?.let(::kgText) ?: "") }
    var picking by remember { mutableStateOf(false) }
    val grams = kgStringToG(text)?.takeIf { it in 1_000..500_000 }
    val taken = weights.any { it.date == date && it.id != editing?.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (editing == null) R.string.body_weight_add else R.string.body_weight_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Text(stringResource(R.string.body_weight_date_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) { Text(formatDate(LocalDate.parse(date), locale)) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.body_weight_kg_label)) },
                    singleLine = true,
                    isError = text.isNotBlank() && grams == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (taken) Text(stringResource(R.string.body_weight_date_exists), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { grams?.let { onSave(LocalDate.parse(date), it) } }, enabled = grams != null) { Text(stringResource(R.string.save)) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )

    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = LocalDate.parse(date).toEpochDay() * 86_400_000)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                Button(onClick = {
                    state.selectedDateMillis?.let { date = LocalDate.ofEpochDay(it / 86_400_000).toString() }
                    picking = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { OutlinedButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = state) }
    }
}
