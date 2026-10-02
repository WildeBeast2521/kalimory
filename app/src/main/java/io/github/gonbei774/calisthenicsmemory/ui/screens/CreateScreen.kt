package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmSearchField
import io.github.gonbei774.calisthenicsmemory.ui.components.common.TopBarAction
import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmTopBar
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.TopAppBarDefaults
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.components.common.ConnectedChoices
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.util.SearchUtils
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit
) {
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }
    var addToGroup by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Exercise?>(null) }
    var showGroupMenu by remember { mutableStateOf<String?>(null) }
    var showGroupEditDialog by remember { mutableStateOf<String?>(null) }
    var showGroupDeleteDialog by remember { mutableStateOf<String?>(null) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember(hierarchicalData, searchQuery) {
        SearchUtils.searchHierarchicalExercises(hierarchicalData, searchQuery)
    }

    val topBarScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(topBarScroll.nestedScrollConnection),
        topBar = { CalmTopBar(title = stringResource(R.string.library_exercises), onBack = onNavigateBack, scrollBehavior = topBarScroll) },
        floatingActionButton = {
            // Expressive medium FAB: a larger, softer target for the screen's main action.
            MediumFloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(
                    AppIcons.Add,
                    contentDescription = stringResource(R.string.add),
                    modifier = Modifier.size(FloatingActionButtonDefaults.MediumIconSize),
                )
            }
        }
    ) { paddingValues ->
        if (hierarchicalData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_exercises_add_with_plus),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            }
        } else {
            // 階層データを3つに分離
            val favoriteGroup = hierarchicalData.firstOrNull {
                it.groupName == TrainingViewModel.FAVORITE_GROUP_KEY
            }
            val regularGroups = hierarchicalData.filter {
                it.groupName != null && it.groupName != TrainingViewModel.FAVORITE_GROUP_KEY
            }
            val ungroupedGroup = hierarchicalData.firstOrNull { it.groupName == null }

            val lazyListState = rememberLazyListState()
            val isSearching = searchQuery.isNotBlank()
            val headerCount = if (!isSearching && favoriteGroup != null) 1 else 0

            val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
                val fromIndex = from.index - headerCount
                val toIndex = to.index - headerCount
                if (fromIndex >= 0 && toIndex >= 0 &&
                    fromIndex < regularGroups.size && toIndex < regularGroups.size) {
                    viewModel.reorderGroups(fromIndex, toIndex)
                }
            }

            // Scroll to top when search results change
            LaunchedEffect(searchQuery, searchResults) {
                if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
                    lazyListState.scrollToItem(0)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                CalmSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = stringResource(R.string.search_placeholder),
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                LazyColumn(
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isSearching) {
                        // Search results (hierarchical)
                        if (searchResults.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.no_results),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            searchResults.forEach { group ->
                                val groupKey = group.groupName ?: "ungrouped"
                                item(key = "search_$groupKey") {
                                    ExpandableGroupCard(
                                        group = group,
                                        isExpanded = true,
                                        onExpandToggle = { },
                                        onGroupMenuClick = { },
                                        onExerciseEdit = { exercise ->
                                            editingExercise = exercise
                                            showAddDialog = true
                                        },
                                        onExerciseDelete = { exercise ->
                                            showDeleteDialog = exercise
                                        },
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    } else {
                        // お気に入りグループ（固定位置）
                        if (favoriteGroup != null) {
                            item(key = "favorite") {
                                ExpandableGroupCard(
                                    group = favoriteGroup,
                                    isExpanded = TrainingViewModel.FAVORITE_GROUP_KEY in expandedGroups,
                                    onExpandToggle = {
                                        viewModel.toggleGroupExpansion(TrainingViewModel.FAVORITE_GROUP_KEY)
                                    },
                                    onGroupMenuClick = {
                                        showGroupMenu = favoriteGroup.groupName
                                    },
                                    onExerciseEdit = { exercise ->
                                        editingExercise = exercise
                                        showAddDialog = true
                                    },
                                    onExerciseDelete = { exercise ->
                                        showDeleteDialog = exercise
                                    },
                                    viewModel = viewModel
                                )
                            }
                        }

                        // 通常グループ（並び替え可能）
                        items(
                            count = regularGroups.size,
                            key = { index -> "group_${regularGroups[index].groupName}" }
                        ) { index ->
                            val group = regularGroups[index]
                            ReorderableItem(reorderableLazyListState, key = "group_${group.groupName}") { isDragging ->
                                val elevation by animateDpAsState(
                                    targetValue = if (isDragging) 4.dp else 0.dp,
                                    label = "elevation"
                                )
                                ExpandableGroupCard(
                                    group = group,
                                    isExpanded = group.groupName!! in expandedGroups,
                                    onExpandToggle = {
                                        viewModel.toggleGroupExpansion(group.groupName!!)
                                    },
                                    onGroupMenuClick = {
                                        showGroupMenu = group.groupName
                                    },
                                    onExerciseEdit = { exercise ->
                                        editingExercise = exercise
                                        showAddDialog = true
                                    },
                                    onExerciseDelete = { exercise ->
                                        showDeleteDialog = exercise
                                    },
                                    viewModel = viewModel,
                                    isDragging = isDragging,
                                    elevation = elevation,
                                    dragHandle = { Modifier.longPressDraggableHandle() }
                                )
                            }
                        }

                        // グループなし（固定位置）
                        if (ungroupedGroup != null) {
                            item(key = "ungrouped") {
                                ExpandableGroupCard(
                                    group = ungroupedGroup,
                                    isExpanded = "ungrouped" in expandedGroups,
                                    onExpandToggle = {
                                        viewModel.toggleGroupExpansion("ungrouped")
                                    },
                                    onGroupMenuClick = {
                                        showGroupMenu = ungroupedGroup.groupName
                                    },
                                    onExerciseEdit = { exercise ->
                                        editingExercise = exercise
                                        showAddDialog = true
                                    },
                                    onExerciseDelete = { exercise ->
                                        showDeleteDialog = exercise
                                    },
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 追加/編集ダイアログ
    if (showAddDialog) {
        UnifiedAddDialog(
            exercise = editingExercise,
            presetGroup = addToGroup,
            viewModel = viewModel,
            onDismiss = {
                showAddDialog = false
                editingExercise = null
                addToGroup = null
            }
        )
    }

    // グループメニュー
    showGroupMenu?.let { groupName ->
        DropdownMenu(
            expanded = true,
            onDismissRequest = { showGroupMenu = null }
        ) {
            if (groupName != TrainingViewModel.FAVORITE_GROUP_KEY) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.add_exercise_to_group)) },
                    onClick = {
                        addToGroup = groupName
                        showAddDialog = true
                        showGroupMenu = null
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rename_group)) },
                onClick = {
                    showGroupEditDialog = groupName
                    showGroupMenu = null
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_group), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    showGroupDeleteDialog = groupName
                    showGroupMenu = null
                }
            )
        }
    }

    // グループ編集ダイアログ
    showGroupEditDialog?.let { oldName ->
        GroupEditDialog(
            oldName = oldName,
            onDismiss = { showGroupEditDialog = null },
            onConfirm = { newName ->
                viewModel.renameGroup(oldName, newName)
                showGroupEditDialog = null
            }
        )
    }

    // グループ削除確認ダイアログ
    showGroupDeleteDialog?.let { groupName ->
        AlertDialog(
            onDismissRequest = { showGroupDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_confirmation)) },
            text = { Text(stringResource(R.string.delete_group_confirm_message, groupName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGroup(groupName)
                        showGroupDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGroupDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 種目削除確認ダイアログ
    showDeleteDialog?.let { exercise ->
        val deleteImpact by viewModel.deleteImpact.collectAsState()
        LaunchedEffect(exercise.id) { viewModel.loadDeleteImpact(exercise) }

        val dismiss = {
            showDeleteDialog = null
            viewModel.clearDeleteImpact()
        }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(stringResource(R.string.delete_confirmation)) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_exercise_confirm_title, exercise.name))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(
                            R.string.delete_exercise_record_count,
                            deleteImpact?.recordCount ?: 0
                        )
                    )

                    val programNames = deleteImpact?.programNames ?: emptyList()
                    val intervalNames = deleteImpact?.intervalNames ?: emptyList()
                    if (programNames.isNotEmpty() || intervalNames.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.delete_exercise_in_use))
                        if (programNames.isNotEmpty()) {
                            Text(
                                stringResource(
                                    R.string.delete_exercise_used_in_programs,
                                    programNames.joinToString(", ")
                                )
                            )
                        }
                        if (intervalNames.isNotEmpty()) {
                            Text(
                                stringResource(
                                    R.string.delete_exercise_used_in_intervals,
                                    intervalNames.joinToString(", ")
                                )
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.delete_exercise_removed_note))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExercise(exercise)
                        dismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = dismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// 階層表示カード
@Composable
fun ExpandableGroupCard(
    group: TrainingViewModel.GroupWithExercises,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onGroupMenuClick: () -> Unit,
    onExerciseEdit: (Exercise) -> Unit,
    onExerciseDelete: (Exercise) -> Unit,
    viewModel: TrainingViewModel,
    isDragging: Boolean = false,
    elevation: Dp = 0.dp,
    dragHandle: (@Composable () -> Modifier)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column {
            // グループヘッダー
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
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // ドラッグハンドル（通常グループのみ）
                        if (dragHandle != null) {
                            Icon(
                                imageVector = AppIcons.DragHandle,
                                contentDescription = stringResource(R.string.todo_drag_to_reorder),
                                tint = if (isDragging) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .then(dragHandle())
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) AppIcons.ExpandMore else AppIcons.Forward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (group.groupName) {
                                TrainingViewModel.FAVORITE_GROUP_KEY -> stringResource(R.string.favorite)
                                null -> stringResource(R.string.no_group)
                                else -> group.groupName
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.exercises_count, group.exercises.size),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (group.groupName != null) {
                        IconButton(
                            onClick = onGroupMenuClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                AppIcons.More,
                                contentDescription = stringResource(R.string.menu),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 種目リスト
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                // お気に入りグループでは並び替え不可
                val isFavoriteGroup = group.groupName == TrainingViewModel.FAVORITE_GROUP_KEY

                ReorderableColumn(
                    list = group.exercises,
                    onSettle = { fromIndex, toIndex ->
                        if (!isFavoriteGroup) {
                            viewModel.reorderExercises(
                                groupName = group.groupName,
                                fromIndex = fromIndex,
                                toIndex = toIndex
                            )
                        }
                    },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) { index, exercise, isDragging ->
                    key(exercise.id) {
                        ReorderableItem {
                            val elevation by animateDpAsState(
                                targetValue = if (isDragging) 4.dp else 0.dp,
                                label = "elevation"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // ドラッグハンドル（お気に入りグループ以外で表示）
                                if (!isFavoriteGroup) {
                                    Icon(
                                        imageVector = AppIcons.DragHandle,
                                        contentDescription = stringResource(R.string.todo_drag_to_reorder),
                                        tint = if (isDragging) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .longPressDraggableHandle()
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                // 種目アイテム（カードのタップで編集）
                                Card(
                                    onClick = { onExerciseEdit(exercise) },
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                                ) {
                                    ExerciseItemCompactContent(
                                        exercise = exercise,
                                        onEdit = { onExerciseEdit(exercise) },
                                        onDelete = { onExerciseDelete(exercise) }
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

// 種目アイテムの内容部分（Card内で使用）
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseItemCompactContent(
    exercise: Exercise,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                // お気に入り
                if (exercise.isFavorite) {
                    Icon(AppIcons.FavoriteFilled, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                }

                // レベル（課題設定がある場合のみ）
                if (exercise.targetSets != null && exercise.targetValue != null && exercise.sortOrder > 0) {
                    Text(
                        text = stringResource(R.string.level_format, exercise.sortOrder),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // タイプ（回数制/時間制）
                Text(
                    text = stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Unilateral
                if (exercise.laterality == "Unilateral") {
                    Text(
                        text = stringResource(R.string.one_sided),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // 有効化している記録オプション（荷重/距離/アシスト）
                if (exercise.weightTrackingEnabled) {
                    Text(
                        text = stringResource(R.string.legend_weight),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (exercise.distanceTrackingEnabled) {
                    Text(
                        text = stringResource(R.string.legend_distance),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (exercise.assistanceTrackingEnabled) {
                    Text(
                        text = stringResource(R.string.legend_assistance),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Pink600
                    )
                }
            }

            // 課題バッジ
            if (exercise.targetSets != null && exercise.targetValue != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            if (exercise.laterality == "Unilateral") R.string.target_format_unilateral else R.string.target_format,
                            exercise.targetSets!!,
                            exercise.targetValue!!,
                            stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Box {
            var menuExpanded by remember { mutableStateOf(false) }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    AppIcons.More,
                    contentDescription = stringResource(R.string.menu),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit)) },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

// 統一追加ダイアログ（種目とグループの両方に対応）- フルスクリーン版
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedAddDialog(
    exercise: Exercise?,
    presetGroup: String? = null,
    viewModel: TrainingViewModel,
    onDismiss: () -> Unit
) {
    val groups by viewModel.groups.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val existingGroupNames = remember(groups) { groups.map { it.name }.sorted() }

    // 作成種類（新規作成時のみ使用）
    var creationType by remember { mutableStateOf(if (exercise != null) "exercise" else "exercise") }

    // 種目用の状態
    var exerciseName by remember { mutableStateOf(exercise?.name ?: "") }
    var selectedType by remember { mutableStateOf(exercise?.type ?: "Dynamic") }
    var selectedLaterality by remember { mutableStateOf(exercise?.laterality ?: "Bilateral") }
    var selectedGroup by remember { mutableStateOf(exercise?.group ?: presetGroup) }
    var selectedLevel by remember { mutableStateOf(exercise?.sortOrder?.coerceIn(1, 10) ?: 5) }
    var showGroupDropdown by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var isCreatingNewGroup by remember { mutableStateOf(false) }

    // お気に入り状態（ローカル管理でリアルタイム反映）
    var isFavorite by remember { mutableStateOf(exercise?.isFavorite ?: false) }

    // 課題設定用の状態
    var hasTarget by remember { mutableStateOf(exercise?.targetSets != null && exercise.targetValue != null) }
    var targetSets by remember { mutableStateOf(exercise?.targetSets?.toString() ?: "") }
    var targetValue by remember { mutableStateOf(exercise?.targetValue?.toString() ?: "") }

    // タイマー設定用の状態
    var repDuration by remember { mutableStateOf(exercise?.repDuration?.toString() ?: "") }
    var restInterval by remember { mutableStateOf(exercise?.restInterval?.toString() ?: "") }

    // 説明文の状態
    var description by remember { mutableStateOf(exercise?.description ?: "") }

    // トラッキング設定用の状態
    var distanceTrackingEnabled by remember { mutableStateOf(exercise?.distanceTrackingEnabled ?: false) }
    var weightTrackingEnabled by remember { mutableStateOf(exercise?.weightTrackingEnabled ?: false) }
    var assistanceTrackingEnabled by remember { mutableStateOf(exercise?.assistanceTrackingEnabled ?: false) }

    // グループ用の状態
    var groupName by remember { mutableStateOf("") }

    val isDuplicate = remember(exerciseName, selectedType, exercises, exercise) {
        if (exerciseName.isBlank()) {
            false
        } else {
            exercises.any { ex ->
                ex.id != exercise?.id &&
                        ex.name.equals(exerciseName, ignoreCase = true) &&
                        ex.type == selectedType
            }
        }
    }

    val isGroupDuplicate = remember(groupName, groups) {
        if (groupName.isBlank()) {
            false
        } else {
            groups.any { it.name.equals(groupName, ignoreCase = true) }
        }
    }

    val isExerciseNameValid = exerciseName.isNotBlank() && exerciseName.length <= 30 && !isDuplicate
    val isGroupNameValid = groupName.isNotBlank() && groupName.length <= 20 && !isGroupDuplicate

    val isFormValid = when {
        creationType == "group" && exercise == null -> isGroupNameValid
        else -> isExerciseNameValid && (!isCreatingNewGroup || newGroupName.isNotBlank())
    }

    // 保存処理
    val onSave: () -> Unit = {
        when {
            // グループ作成
            creationType == "group" && exercise == null && isGroupNameValid -> {
                viewModel.createGroup(groupName)
                onDismiss()
            }
            // 種目編集
            exercise != null && isExerciseNameValid -> {
                val finalGroup = if (isCreatingNewGroup && newGroupName.isNotBlank()) {
                    viewModel.createGroup(newGroupName)
                    newGroupName
                } else {
                    selectedGroup
                }

                val finalTargetSets = if (hasTarget) targetSets.toIntOrNull() else null
                val finalTargetValue = if (hasTarget) targetValue.toIntOrNull() else null
                val finalSortOrder = if (hasTarget) selectedLevel else 0
                val finalRepDuration = repDuration.toIntOrNull()
                val finalRestInterval = restInterval.toIntOrNull()

                viewModel.updateExercise(
                    exercise.copy(
                        name = exerciseName,
                        type = selectedType,
                        laterality = selectedLaterality,
                        group = finalGroup,
                        sortOrder = finalSortOrder,
                        targetSets = finalTargetSets,
                        targetValue = finalTargetValue,
                        isFavorite = isFavorite,
                        repDuration = finalRepDuration,
                        restInterval = finalRestInterval,
                        distanceTrackingEnabled = distanceTrackingEnabled,
                        weightTrackingEnabled = weightTrackingEnabled,
                        assistanceTrackingEnabled = assistanceTrackingEnabled,
                        description = description.ifBlank { null }
                    )
                )
                onDismiss()
            }
            // 種目追加
            creationType == "exercise" && exercise == null && isExerciseNameValid -> {
                val finalGroup = if (isCreatingNewGroup && newGroupName.isNotBlank()) {
                    viewModel.createGroup(newGroupName)
                    newGroupName
                } else {
                    selectedGroup
                }

                val finalTargetSets = if (hasTarget) targetSets.toIntOrNull() else null
                val finalTargetValue = if (hasTarget) targetValue.toIntOrNull() else null
                val finalSortOrder = if (hasTarget) selectedLevel else 0
                val finalRepDuration = repDuration.toIntOrNull()
                val finalRestInterval = restInterval.toIntOrNull()

                viewModel.addExercise(
                    exerciseName,
                    selectedType,
                    finalGroup,
                    finalSortOrder,
                    selectedLaterality,
                    finalTargetSets,
                    finalTargetValue,
                    isFavorite,
                    finalRestInterval,
                    finalRepDuration,
                    distanceTrackingEnabled,
                    weightTrackingEnabled,
                    assistanceTrackingEnabled,
                    description.ifBlank { null }
                )
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val topBarScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            Scaffold(
                modifier = Modifier.nestedScroll(topBarScroll.nestedScrollConnection),
                topBar = {
                    CalmTopBar(
                        title = stringResource(
                            when {
                                exercise != null -> R.string.edit_exercise_title
                                creationType == "group" -> R.string.create_group_title
                                else -> R.string.add_exercise_title
                            }
                        ),
                        onBack = onDismiss,
                        scrollBehavior = topBarScroll,
                        backIcon = AppIcons.Close,
                        backDescription = stringResource(R.string.cancel),
                    ) {
                        // Favourite star, for exercises.
                        if (creationType == "exercise") {
                            IconToggleButton(checked = isFavorite, onCheckedChange = { isFavorite = it }) {
                                Icon(
                                    imageVector = if (isFavorite) AppIcons.FavoriteFilled else AppIcons.Favorite,
                                    contentDescription = stringResource(if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites),
                                )
                            }
                        }
                        TopBarAction(
                            stringResource(
                                when {
                                    exercise != null -> R.string.save_button
                                    creationType == "group" -> R.string.create_button
                                    else -> R.string.add_button
                                }
                            ),
                            enabled = isFormValid,
                            onClick = onSave,
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 作成種類選択（新規作成時のみ）
                    if (exercise == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.create_type),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                ConnectedChoices(
                                    options = listOf("exercise", "group"),
                                    selected = creationType,
                                    onSelect = { it?.let { type -> creationType = type } },
                                    label = { stringResource(if (it == "group") R.string.group else R.string.exercise) },
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }

                    // グループ作成フォーム
                    if (creationType == "group" && exercise == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = groupName,
                                    onValueChange = { if (it.length <= 20) groupName = it },
                                    label = { Text(stringResource(R.string.group_name)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    isError = isGroupDuplicate,
                                    supportingText = {
                                        when {
                                            isGroupDuplicate -> Text(stringResource(R.string.duplicate_group_name), color = MaterialTheme.colorScheme.error)
                                            else -> Text(stringResource(R.string.character_count, groupName.length, 20), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // 種目作成/編集フォーム
                    if (creationType == "exercise" || exercise != null) {
                        // 基本情報カード
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = exerciseName,
                                    onValueChange = { if (it.length <= 30) exerciseName = it },
                                    label = { Text(stringResource(R.string.exercise_name)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    isError = isDuplicate,
                                    supportingText = {
                                        when {
                                            isDuplicate -> Text(stringResource(R.string.duplicate_exercise_name), color = MaterialTheme.colorScheme.error)
                                            else -> Text(stringResource(R.string.character_count, exerciseName.length, 30), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                )

                                // タイプ選択
                                Column {
                                    Text(
                                        text = stringResource(R.string.type),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    ConnectedChoices(
                                        options = listOf("Dynamic", "Isometric"),
                                        selected = selectedType,
                                        onSelect = { it?.let { type -> selectedType = type } },
                                        label = { stringResource(if (it == "Isometric") R.string.exercise_type_isometric else R.string.exercise_type_dynamic) },
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                    Text(
                                        text = if (selectedType == "Isometric")
                                            stringResource(R.string.exercise_type_isometric_description)
                                        else
                                            stringResource(R.string.exercise_type_dynamic_description),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                // 左右種別選択
                                Column {
                                    Text(
                                        text = stringResource(R.string.laterality),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    ConnectedChoices(
                                        options = listOf("Bilateral", "Unilateral"),
                                        selected = selectedLaterality,
                                        onSelect = { it?.let { laterality -> selectedLaterality = laterality } },
                                        label = { stringResource(if (it == "Unilateral") R.string.unilateral_with_parenthesis else R.string.bilateral_with_parenthesis) },
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                    Text(
                                        text = stringResource(
                                            if (selectedLaterality == "Bilateral") R.string.example_bilateral else R.string.example_unilateral
                                        ),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }

                        // グループ設定カード
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.group_optional),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )

                                if (isCreatingNewGroup) {
                                    OutlinedTextField(
                                        value = newGroupName,
                                        onValueChange = { if (it.length <= 20) newGroupName = it },
                                        label = { Text(stringResource(R.string.new_group_name)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        supportingText = { Text("${newGroupName.length}/20", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                isCreatingNewGroup = false
                                                newGroupName = ""
                                            }) {
                                                Icon(AppIcons.Close, contentDescription = stringResource(R.string.cancel))
                                            }
                                        }
                                    )
                                } else {
                                    ExposedDropdownMenuBox(
                                        expanded = showGroupDropdown,
                                        onExpandedChange = { showGroupDropdown = it }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedGroup ?: stringResource(R.string.no_group_display),
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                            trailingIcon = { Icon(AppIcons.DropDown, null) }
                                        )

                                        ExposedDropdownMenu(
                                            expanded = showGroupDropdown,
                                            onDismissRequest = { showGroupDropdown = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.no_group_display)) },
                                                onClick = {
                                                    selectedGroup = null
                                                    showGroupDropdown = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.new_group_plus), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary) },
                                                onClick = {
                                                    isCreatingNewGroup = true
                                                    showGroupDropdown = false
                                                }
                                            )
                                            existingGroupNames.forEach { group ->
                                                DropdownMenuItem(
                                                    text = { Text(group) },
                                                    onClick = {
                                                        selectedGroup = group
                                                        showGroupDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 課題設定カード
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.set_challenge),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Switch(
                                        checked = hasTarget,
                                        onCheckedChange = { hasTarget = it }
                                    )
                                }

                                if (hasTarget) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = targetSets,
                                            onValueChange = {
                                                if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.toIntOrNull()?.let { num -> num in 1..20 } == true)) {
                                                    targetSets = it
                                                }
                                            },
                                            label = { Text(stringResource(R.string.sets_count)) },
                                            placeholder = { Text(stringResource(R.string.example_3)) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                            )
                                        )

                                        OutlinedTextField(
                                            value = targetValue,
                                            onValueChange = {
                                                if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.toIntOrNull()?.let { num -> num >= 1 } == true)) {
                                                    targetValue = it
                                                }
                                            },
                                            label = { Text(stringResource(if (selectedType == "Dynamic") R.string.reps_label else R.string.time_label)) },
                                            placeholder = { Text(stringResource(if (selectedType == "Dynamic") R.string.example_10 else R.string.example_30)) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                            )
                                        )
                                    }

                                    if (selectedLaterality == "Unilateral") {
                                        Text(
                                            text = stringResource(R.string.per_side_parenthesis),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // レベル設定
                                    Column {
                                        Text(
                                            text = stringResource(R.string.level_display, selectedLevel),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Slider(
                                            value = selectedLevel.toFloat(),
                                            onValueChange = { selectedLevel = it.toInt() },
                                            valueRange = 1f..10f,
                                            steps = 8,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // タイマー設定カード
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.timer_settings_optional),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.timer_settings_description),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                // 1レップ時間（Dynamic種目のみ）
                                if (selectedType == "Dynamic") {
                                    OutlinedTextField(
                                        value = repDuration,
                                        onValueChange = {
                                            if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.toIntOrNull()?.let { num -> num in 1..60 } == true)) {
                                                repDuration = it
                                            }
                                        },
                                        label = { Text(stringResource(R.string.rep_duration_seconds)) },
                                        placeholder = { Text(stringResource(R.string.example_5_seconds)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                        )
                                    )
                                }

                                // セット間インターバル
                                OutlinedTextField(
                                    value = restInterval,
                                    onValueChange = {
                                        if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.toIntOrNull()?.let { num -> num in 0..600 } == true)) {
                                            restInterval = it
                                        }
                                    },
                                    label = { Text(stringResource(R.string.rest_interval_seconds)) },
                                    placeholder = { Text(stringResource(R.string.example_240_seconds)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    )
                                )
                            }
                        }

                        // トラッキング設定カード
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.tracking_settings_optional),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )

                                // 距離トラッキング
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(
                                            text = stringResource(R.string.track_distance),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.track_distance_description),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = distanceTrackingEnabled,
                                        onCheckedChange = { distanceTrackingEnabled = it }
                                    )
                                }

                                // 荷重トラッキング
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(
                                            text = stringResource(R.string.track_weight),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.track_weight_description),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = weightTrackingEnabled,
                                        onCheckedChange = { weightTrackingEnabled = it }
                                    )
                                }

                                // アシストトラッキング
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(
                                            text = stringResource(R.string.track_assistance),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.track_assistance_description),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = assistanceTrackingEnabled,
                                        onCheckedChange = { assistanceTrackingEnabled = it }
                                    )
                                }
                            }
                        }
                        // 説明文カード
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.description_optional),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { if (it.length <= 120) description = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    maxLines = 5,
                                    supportingText = {
                                        Text(
                                            stringResource(R.string.character_count, description.length, 120),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // 下部のスペース（スクロール時の余白）
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// グループ編集ダイアログ
@Composable
fun GroupEditDialog(
    oldName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(oldName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_group)) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { if (it.length <= 20) newName = it },
                label = { Text(stringResource(R.string.group_name)) },
                singleLine = true,
                supportingText = { Text("${newName.length}/20") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank() && newName != oldName
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}