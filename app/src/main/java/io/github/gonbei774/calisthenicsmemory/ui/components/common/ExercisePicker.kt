package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.util.SearchUtils
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

/**
 * Choosing several exercises to add (to a program, an interval or a list), in one full-screen window: a title, a
 * search field, the exercises under their groups (all open at first, and each group can be closed), and an Add
 * button that says how many are chosen. A search shows matching exercises with their group instead of the groups.
 */
@Composable
fun ExercisePicker(
    viewModel: TrainingViewModel,
    exercises: List<Exercise>,
    title: String,
    onDismiss: () -> Unit,
    onAdd: (List<Exercise>) -> Unit,
) {
    val groups by viewModel.hierarchicalExercises.collectAsState()
    val collapsed by viewModel.collapsedPickerGroups.collectAsState()
    var selected by remember { mutableStateOf(listOf<Long>()) }
    var query by remember { mutableStateOf("") }
    val results = remember(exercises, query) { SearchUtils.searchExercises(exercises, query) }
    val listState = rememberLazyListState()
    LaunchedEffect(query, results) { if (query.isNotBlank() && results.isNotEmpty()) listState.scrollToItem(0) }
    fun toggle(id: Long) { selected = if (id in selected) selected - id else selected + id }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        DialogSystemBars()
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.s, vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(AppIcons.Close, contentDescription = stringResource(R.string.cancel)) }
                    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                }
                if (exercises.isEmpty()) {
                    Text(stringResource(R.string.no_exercises_available), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(Spacing.l))
                } else {
                    CalmSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = stringResource(R.string.search_placeholder),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.s),
                    )
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.l, vertical = Spacing.s),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s),
                    ) {
                        if (query.isNotBlank()) {
                            if (results.isEmpty()) {
                                item { Text(stringResource(R.string.no_results), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(Spacing.l)) }
                            } else {
                                items(results, key = { it.id }) { exercise ->
                                    ExerciseCheckRow(exercise, isSelected = exercise.id in selected, onToggle = ::toggle, showGroup = true)
                                }
                            }
                        } else {
                            groups.filter { it.exercises.isNotEmpty() }.forEach { group ->
                                val key = group.groupName ?: "ungrouped"
                                val open = key !in collapsed
                                item(key = "header-$key") {
                                    PickerGroupHeader(group.groupName ?: stringResource(R.string.no_group), group.exercises.size, open) { viewModel.togglePickerGroup(key) }
                                }
                                if (open) {
                                    items(group.exercises, key = { "ex-${it.id}" }) { exercise ->
                                        ExerciseCheckRow(exercise, isSelected = exercise.id in selected, onToggle = ::toggle)
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { onAdd(selected.mapNotNull { id -> exercises.find { it.id == id } }) },
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().padding(Spacing.l),
                    ) {
                        Text(if (selected.isEmpty()) stringResource(R.string.add) else stringResource(R.string.add) + " (" + selected.size + ")")
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerGroupHeader(name: String, count: Int, open: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Icon(if (open) AppIcons.ExpandMore else AppIcons.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("($count)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
