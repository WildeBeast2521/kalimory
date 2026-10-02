package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.compose.material3.LinearWavyProgressIndicator
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.firstDayOfWeek
import java.time.DayOfWeek
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.time.LocalDate
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.components.common.ConnectedChoices
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalRecord
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.data.progression.ChainProgress
import io.github.gonbei774.calisthenicsmemory.data.progression.Progressions
import io.github.gonbei774.calisthenicsmemory.util.SearchUtils
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

// ViewMode enum
enum class ViewMode {
    Calendar,   // カレンダーモード
    List,       // 一覧モード
    Graph,      // グラフモード
    Challenge   // 課題モード
}

// データクラス
data class SessionInfo(
    val exerciseId: Long,
    val date: String,
    val time: String,
    val comment: String,
    val records: List<HistorySet>,
    val source: HistorySource = HistorySource.LEGACY
)

// 統一リスト用sealed class
sealed class RecordItem(val date: String, val time: String) {
    data class Session(val session: SessionInfo) : RecordItem(session.date, session.time)
    // v2SessionId is set for a v2 interval workout; edits and deletes then go to that session.
    data class Interval(val record: IntervalRecord, val v2SessionId: Long? = null) : RecordItem(record.date, record.time)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ViewScreen(
    viewModel: TrainingViewModel,
    // A day to show on the calendar, as when Today's week strip is tapped; cleared once shown.
    focusDate: LocalDate? = null,
    onFocusShown: () -> Unit = {},
    page: Int = 0,
    onPageChange: (Int) -> Unit = {},
    onOpenChain: (String) -> Unit = {},
    onOpenCatalogue: () -> Unit = {},
) {
    val firstDay = firstDayOfWeek()
    val exercises by viewModel.exercises.collectAsState()
    val records by viewModel.history.collectAsState()
    val intervalRecords by viewModel.intervalHistory.collectAsState()
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()

    // ViewModeの状態（HorizontalPager用）
    val pagerState = rememberPagerState(initialPage = page, pageCount = { 4 })
    LaunchedEffect(pagerState.currentPage) { onPageChange(pagerState.currentPage) }
    LaunchedEffect(focusDate) {
        if (focusDate != null) pagerState.scrollToPage(0)
    }
    val coroutineScope = rememberCoroutineScope()
    val currentMode = when (pagerState.currentPage) {
        0 -> ViewMode.Calendar
        1 -> ViewMode.List
        2 -> ViewMode.Graph
        else -> ViewMode.Challenge
    }

    // フィルター関連の状態
    var selectedExerciseFilter by remember { mutableStateOf<Exercise?>(null) }
    var selectedPeriod by remember { mutableStateOf<Period?>(Period.OneWeek) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }

    // 既存の状態変数
    var showDeleteDialog by remember { mutableStateOf<SessionInfo?>(null) }
    var editingRecord by remember { mutableStateOf<HistorySet?>(null) }
    var editValue by remember { mutableStateOf("") }
    var editValueRight by remember { mutableStateOf("") }
    var editValueLeft by remember { mutableStateOf("") }
    var showSessionEditDialog by remember { mutableStateOf<SessionInfo?>(null) }
    var showContextMenu by remember { mutableStateOf<SessionInfo?>(null) }
    var showIntervalDeleteDialog by remember { mutableStateOf<RecordItem.Interval?>(null) }
    var showIntervalEditDialog by remember { mutableStateOf<RecordItem.Interval?>(null) }

    // 一覧モード用のセッションデータ
    val sessions = remember(records, exercises) {
        records
            .groupBy { "${it.source}-${it.exerciseId}-${it.date}-${it.time}" }
            .map { (_, sessionRecords) ->
                val first = sessionRecords.first()
                SessionInfo(
                    exerciseId = first.exerciseId,
                    date = first.date,
                    time = first.time,
                    comment = first.comment,
                    records = sessionRecords.sortedBy { it.setNumber },
                    source = first.source
                )
            }
            .sortedWith(
                compareByDescending<SessionInfo> { it.date }
                    .thenByDescending { it.time }
            )
    }

    // フィルター済みセッション（種目＋期間）
    val filteredSessions = remember(sessions, selectedExerciseFilter, selectedPeriod) {
        var filtered = sessions

        // 種目フィルター
        if (selectedExerciseFilter != null) {
            filtered = filtered.filter { it.exerciseId == selectedExerciseFilter!!.id }
        }

        // 期間フィルター
        if (selectedPeriod != null) {
            val today = java.time.LocalDate.now()
            val cutoffDate = selectedPeriod!!.startDate(today, firstDay)

            filtered = filtered.filter { session ->
                try {
                    val sessionDate = java.time.LocalDate.parse(session.date)
                    sessionDate >= cutoffDate && sessionDate <= today
                } catch (e: Exception) {
                    false
                }
            }
        }

        filtered
    }

    // インターバル記録のフィルター（期間のみ、種目フィルター時は非表示）
    val filteredIntervalRecords = remember(intervalRecords, selectedExerciseFilter, selectedPeriod) {
        if (selectedExerciseFilter != null) {
            emptyList()
        } else if (selectedPeriod != null) {
            val today = java.time.LocalDate.now()
            val cutoffDate = selectedPeriod!!.startDate(today, firstDay)
            intervalRecords.filter { item ->
                try {
                    val recordDate = java.time.LocalDate.parse(item.record.date)
                    recordDate >= cutoffDate && recordDate <= today
                } catch (e: Exception) { false }
            }
        } else {
            intervalRecords
        }
    }

    // 統一リスト（セッション＋インターバル記録を日付順で混在）
    val filteredItems = remember(filteredSessions, filteredIntervalRecords) {
        val sessionItems = filteredSessions.map { RecordItem.Session(it) }
        val intervalItems = filteredIntervalRecords.map { RecordItem.Interval(it.record, it.v2SessionId) }
        (sessionItems + intervalItems).sortedWith(
            compareByDescending<RecordItem> { it.date }
                .thenByDescending { it.time }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // The same large title as the other primary destinations.
            Text(
                text = stringResource(R.string.nav_progress),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .semantics { heading() }
                    .padding(start = Spacing.l, end = Spacing.l, top = Spacing.xl, bottom = Spacing.s)
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // タブ
            val tabTitles = listOf(
                stringResource(R.string.tab_calendar),
                stringResource(R.string.tab_list),
                stringResource(R.string.tab_graph),
                stringResource(R.string.tab_progressions)
            )
            BoxWithConstraints {
                val minTabWidth = maxWidth / tabTitles.size
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    edgePadding = 0.dp
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            modifier = Modifier.widthIn(min = minTabWidth),
                            text = {
                                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            }
                        )
                    }
                }
            }

            // フィルターチップ（全タブで表示、一行に並べる）
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                // Chips scroll to the screen edge, and the edge fades while more lie beyond it,
                // so a clipped chip reads as "scroll for more".
                val chipScroll = rememberScrollState()
                val fade = MaterialTheme.colorScheme.background
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            drawContent()
                            if (chipScroll.canScrollForward) {
                                val width = 32.dp.toPx()
                                drawRect(
                                    Brush.horizontalGradient(listOf(fade.copy(alpha = 0f), fade), startX = size.width - width, endX = size.width),
                                    topLeft = androidx.compose.ui.geometry.Offset(size.width - width, 0f),
                                )
                            }
                        }
                        .horizontalScroll(chipScroll)
                        .padding(horizontal = Spacing.l, vertical = Spacing.s),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 種目フィルター
                    if (selectedExerciseFilter != null) {
                        // 選択中の種目を表示
                        FilterChip(
                            selected = true,
                            onClick = { selectedExerciseFilter = null },
                            label = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(AppIcons.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(selectedExerciseFilter!!.name)
                                }
                            },
                        )
                    } else {
                        // 未選択時は選択画面を開くボタンを表示
                        FilterChip(
                            selected = false,
                            onClick = { showFilterBottomSheet = true },
                            label = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        AppIcons.Search,
                                        contentDescription = stringResource(R.string.select_exercise_filter),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(stringResource(R.string.select_exercise_filter))
                                }
                            },
                        )
                    }

                    // 期間フィルター（トグル式）
                    // An Expressive connected group; tapping the selected period clears it.
                    ConnectedChoices(
                        options = listOf(Period.OneWeek, Period.OneMonth, Period.ThreeMonths),
                        selected = selectedPeriod,
                        onSelect = { selectedPeriod = it },
                        label = { stringResource(it.displayNameResId) },
                        allowNone = true,
                        fill = false,
                    )
                }
            }

            // モードに応じた表示（スワイプ対応）
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        CalendarView(
                            items = filteredItems,
                            exercises = exercises,
                            selectedExerciseFilter = selectedExerciseFilter,
                            selectedPeriod = selectedPeriod,
                            onExerciseClick = { exercise ->
                                selectedExerciseFilter = exercise
                            },
                            focusDate = focusDate,
                            onFocusShown = onFocusShown,
                        )
                    }
                    1 -> {
                        RecordListView(
                            items = filteredItems,
                            exercises = exercises,
                            selectedExerciseFilter = selectedExerciseFilter,
                            onExerciseClick = { exercise ->
                                selectedExerciseFilter = exercise
                            },
                            onRecordClick = { record ->
                                editingRecord = record
                                if (record.valueLeft != null) {
                                    // Unilateral
                                    editValueRight = record.valueRight.toString()
                                    editValueLeft = record.valueLeft.toString()
                                } else {
                                    // Bilateral
                                    editValue = record.valueRight.toString()
                                }
                            },
                            onSessionLongPress = { session ->
                                showSessionEditDialog = session
                            },
                            onDeleteClick = { session ->
                                showDeleteDialog = session
                            },
                            onIntervalEditClick = { record ->
                                showIntervalEditDialog = record
                            },
                            onIntervalDeleteClick = { record ->
                                showIntervalDeleteDialog = record
                            }
                        )
                    }
                    2 -> {
                        // With no exercise chosen, the graph shows the most recently trained one.
                        val graphExercise = selectedExerciseFilter ?: remember(records, exercises) {
                            records.maxByOrNull { "${it.date} ${it.time}" }
                                ?.let { latest -> exercises.find { it.id == latest.exerciseId } }
                        }
                        Column {
                            if (selectedExerciseFilter == null && graphExercise != null) {
                                Text(
                                    text = stringResource(R.string.graph_latest_exercise, graphExercise.name),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                                )
                            }
                            GraphView(
                                exercises = exercises,
                                records = records,
                                selectedExerciseFilter = graphExercise,
                                selectedPeriod = selectedPeriod
                            )
                        }
                    }
                    3 -> {
                        val unfollowed by viewModel.unfollowedChains.collectAsState()
                        val progressions = remember(exercises, records, unfollowed) { Progressions.chains(exercises, records, unfollowed) }
                        val allUnfollowed = progressions.isEmpty() && exercises.any { it.catalogId != null }
                        ChallengeView(
                            exercises = exercises,
                            records = records,
                            progressions = progressions,
                            onOpenChain = onOpenChain,
                            onOpenCatalogue = onOpenCatalogue,
                            allUnfollowed = allUnfollowed,
                            selectedExerciseFilter = selectedExerciseFilter,
                            selectedPeriod = selectedPeriod,
                            onExerciseClick = { exercise ->
                                selectedExerciseFilter = exercise
                            }
                        )
                    }
                }
            }
        }
    }

    // Filter BottomSheet
    if (showFilterBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterBottomSheet = false }
        ) {
            FilterBottomSheetContent(
                exercises = exercises,
                hierarchicalData = hierarchicalData,
                selectedExercise = selectedExerciseFilter,
                onExerciseSelected = { exercise ->
                    selectedExerciseFilter = exercise
                    showFilterBottomSheet = false
                },
                onClearFilter = {
                    selectedExerciseFilter = null
                    showFilterBottomSheet = false
                }
            )
        }
    }

    // Interval delete dialog
    showIntervalDeleteDialog?.let { item ->
        val record = item.record
        AlertDialog(
            onDismissRequest = { showIntervalDeleteDialog = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = {
                Text(
                    stringResource(R.string.delete_confirmation),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    stringResource(
                        R.string.interval_delete_record_confirm,
                        record.programName,
                        record.date
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val sessionId = item.v2SessionId
                    if (sessionId != null) viewModel.deleteV2IntervalSession(sessionId)
                    else viewModel.deleteIntervalRecord(record.id)
                    showIntervalDeleteDialog = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIntervalDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Interval edit dialog
    showIntervalEditDialog?.let { item ->
        IntervalRecordEditDialog(
            record = item.record,
            onDismiss = { showIntervalEditDialog = null },
            onConfirm = { updatedRecord ->
                val sessionId = item.v2SessionId
                if (sessionId != null) {
                    viewModel.updateV2IntervalSession(sessionId, updatedRecord.date, updatedRecord.time, updatedRecord.comment.orEmpty())
                } else {
                    viewModel.updateIntervalRecordAsync(updatedRecord)
                }
                showIntervalEditDialog = null
            }
        )
    }

    // Edit Record Dialog
    editingRecord?.let { record ->
        val isUnilateral = record.valueLeft != null

        AlertDialog(
            onDismissRequest = {
                editingRecord = null
                editValue = ""
                editValueRight = ""
                editValueLeft = ""
            },
            title = { Text(stringResource(R.string.edit_set_value)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.set_number_format, record.setNumber),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (isUnilateral) {
                        // Unilateral: 左右2つの入力
                        OutlinedTextField(
                            value = editValueRight,
                            onValueChange = {
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    editValueRight = it
                                }
                            },
                            label = { Text(stringResource(R.string.right_value_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editValueLeft,
                            onValueChange = {
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    editValueLeft = it
                                }
                            },
                            label = { Text(stringResource(R.string.left_value_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Bilateral: 1つの入力
                        OutlinedTextField(
                            value = editValue,
                            onValueChange = {
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    editValue = it
                                }
                            },
                            label = { Text(stringResource(R.string.value_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isUnilateral) {
                            // Unilateral: 左右両方更新
                            val newValueRight = editValueRight.toIntOrNull()
                            val newValueLeft = editValueLeft.toIntOrNull()

                            if (newValueRight != null && newValueRight >= 0) {
                                val legacy = record.toLegacyRecord()
                                if (legacy != null) {
                                    viewModel.updateRecord(legacy.copy(valueRight = newValueRight, valueLeft = newValueLeft))
                                } else {
                                    viewModel.updateV2SetValues(record, newValueRight, newValueLeft)
                                }
                                editingRecord = null
                                editValueRight = ""
                                editValueLeft = ""
                            }
                        } else {
                            // Bilateral: 従来通り
                            editValue.toIntOrNull()?.let { newValue ->
                                if (newValue >= 0) {
                                    val legacy = record.toLegacyRecord()
                                    if (legacy != null) viewModel.updateRecord(legacy.copy(valueRight = newValue))
                                    else viewModel.updateV2SetValues(record, newValue, null)
                                    editingRecord = null
                                    editValue = ""
                                }
                            }
                        }
                    },
                    enabled = if (isUnilateral) {
                        editValueRight.toIntOrNull()?.let { it >= 0 } == true
                    } else {
                        editValue.toIntOrNull()?.let { it >= 0 } == true
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    editingRecord = null
                    editValue = ""
                    editValueRight = ""
                    editValueLeft = ""
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Context Menu
    showContextMenu?.let { session ->
        DropdownMenu(
            expanded = true,
            onDismissRequest = { showContextMenu = null }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_session_info)) },
                onClick = {
                    showSessionEditDialog = session
                    showContextMenu = null
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_session), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    showDeleteDialog = session
                    showContextMenu = null
                }
            )
        }
    }

    // Session Edit Dialog
    showSessionEditDialog?.let { session ->
        val editExercise = exercises.find { it.id == session.exerciseId }
        SessionEditDialog(
            session = session,
            exercise = editExercise,
            onDismiss = { showSessionEditDialog = null },
            onConfirm = { newDate, newTime, newComment, newDistancesCm, newWeightsG, newAssistancesG ->
                if (session.source == HistorySource.V2) {
                    viewModel.updateV2Session(
                        session.records, newDate, newTime, newComment, newDistancesCm, newWeightsG, newAssistancesG
                    )
                }
                session.records.forEachIndexed { index, record ->
                    val legacy = record.toLegacyRecord() ?: return@forEachIndexed
                    viewModel.updateRecord(
                        legacy.copy(
                            date = newDate,
                            time = newTime,
                            comment = newComment,
                            distanceCm = newDistancesCm.getOrNull(index),
                            weightG = newWeightsG.getOrNull(index),
                            assistanceG = newAssistancesG.getOrNull(index)
                        )
                    )
                }
                showSessionEditDialog = null
            }
        )
    }

    // Delete Dialog
    showDeleteDialog?.let { session ->
        val exercise = exercises.find { it.id == session.exerciseId }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_confirmation)) },
            text = {
                Text(stringResource(
                    R.string.delete_record_warning,
                    exercise?.name ?: stringResource(R.string.unknown_short),
                    session.date,
                    session.time,
                    session.records.size
                ))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (session.source == HistorySource.V2) {
                            viewModel.deleteV2Sets(session.records)
                        } else {
                            viewModel.deleteSession(
                                session.exerciseId,
                                session.date,
                                session.time
                            )
                        }
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// フィルターBottomSheetのコンテンツ
@Composable
fun FilterBottomSheetContent(
    exercises: List<Exercise>,
    hierarchicalData: List<TrainingViewModel.GroupWithExercises>,
    selectedExercise: Exercise?,
    onExerciseSelected: (Exercise?) -> Unit,
    onClearFilter: () -> Unit
) {
    var expandedGroups by remember { mutableStateOf(setOf<String?>()) }
    var searchQuery by remember { mutableStateOf("") }

    // 検索フィルター
    val filteredHierarchicalData = remember(hierarchicalData, searchQuery) {
        SearchUtils.searchHierarchicalExercises(hierarchicalData, searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.select_exercise),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 検索バー
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text(stringResource(R.string.search_exercise), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(
                    AppIcons.Search,
                    contentDescription = stringResource(R.string.search),
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = true
        )

        // スクロール可能な階層表示
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            // The last rows scroll clear of the system navigation bar.
            contentPadding = WindowInsets.navigationBars.asPaddingValues()
        ) {
            // 全て表示
            if (searchQuery.isEmpty()) {
                item {
                    FilterTextItem(
                        text = stringResource(R.string.show_all),
                        isSelected = selectedExercise == null,
                        onClick = onClearFilter
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 階層表示（アコーディオン式）
            filteredHierarchicalData.forEach { group ->
                // グループヘッダー
                if (group.groupName != null || group.exercises.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = {
                                expandedGroups = if (expandedGroups.contains(group.groupName)) {
                                    expandedGroups - group.groupName
                                } else {
                                    expandedGroups + group.groupName
                                }
                            },
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(8.dp)
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
                                    Text(
                                        text = when (group.groupName) {
                                            TrainingViewModel.FAVORITE_GROUP_KEY -> stringResource(R.string.favorite)
                                            null -> stringResource(R.string.no_group_display)
                                            else -> group.groupName
                                        },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "(${group.exercises.size})",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (expandedGroups.contains(group.groupName)) {
                                        AppIcons.ExpandMore
                                    } else {
                                        AppIcons.Forward
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // グループ内の種目（展開時のみ表示）
                    if (expandedGroups.contains(group.groupName)) {
                        group.exercises.forEach { exercise ->
                            item {
                                FilterExerciseItem(
                                    exercise = exercise,
                                    isSelected = selectedExercise?.id == exercise.id,
                                    onClick = { onExerciseSelected(exercise) },
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Exerciseオブジェクト版（バッジ付き）
@Composable
fun FilterExerciseItem(
    exercise: Exercise,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    fontSize = 16.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )

                // バッジ行（テキストのみ、スペース区切り）
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // お気に入り
                    if (exercise.isFavorite) {
                        Icon(AppIcons.FavoriteFilled, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                    }

                    // レベル
                    if (exercise.targetSets != null && exercise.targetValue != null && exercise.sortOrder > 0) {
                        Text(
                            text = stringResource(R.string.level_format, exercise.sortOrder),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // タイプ
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
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = AppIcons.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// テキストのみ版（"全て表示"など用）
@Composable
fun FilterTextItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            if (isSelected) {
                Icon(
                    imageVector = AppIcons.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ========================================
// 課題画面
// ========================================

@Composable
fun ChallengeView(
    exercises: List<Exercise>,
    records: List<HistorySet>,
    progressions: List<ChainProgress>,
    onOpenChain: (String) -> Unit,
    onOpenCatalogue: () -> Unit,
    allUnfollowed: Boolean = false,
    selectedExerciseFilter: Exercise?,
    selectedPeriod: Period?,
    onExerciseClick: (Exercise) -> Unit
) {
    val firstDay = firstDayOfWeek()
    // ViewModelを取得（階層データ用）
    val viewModel: TrainingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()

    // フィルターを適用（種目 + 期間）
    val filteredExercises = remember(exercises, records, selectedExerciseFilter, selectedPeriod) {
        var filtered = exercises

        // 種目フィルター
        if (selectedExerciseFilter != null) {
            filtered = listOf(selectedExerciseFilter!!)
        }

        // 期間フィルター（nullの場合は全期間）
        if (selectedPeriod != null) {
            val today = java.time.LocalDate.now()
            val cutoffDate = selectedPeriod!!.startDate(today, firstDay)

            // 期間内に記録がある種目のみを抽出
            val exerciseIdsWithRecords = records.filter { record ->
                try {
                    val recordDate = java.time.LocalDate.parse(record.date)
                    recordDate >= cutoffDate && recordDate <= today
                } catch (e: Exception) {
                    false
                }
            }.map { it.exerciseId }.toSet()

            filtered = filtered.filter { it.id in exerciseIdsWithRecords }
        }

        filtered
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Followed chains first; the exercise targets the Challenge tab showed stay below (ADR 0005).
        progressionsSection(progressions, onOpenChain, onOpenCatalogue, allUnfollowed)

        if (filteredExercises.isEmpty()) {
            item {
                Text(
                    text = if (selectedPeriod == null) {
                        stringResource(R.string.no_exercises_available)
                    } else {
                        stringResource(R.string.no_exercises_in_period, stringResource(selectedPeriod.displayNameResId))
                    },
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            hierarchicalData.forEach { group ->
                // 種目を1つに絞り込んでいるときは、お気に入りグループには出さず
                // その種目が属する実グループのみに表示する（重複表示の防止）
                if (selectedExerciseFilter != null && group.groupName == TrainingViewModel.FAVORITE_GROUP_KEY) {
                    return@forEach
                }

                // グループ内の種目で期間フィルターを通過したものを抽出
                val groupFilteredExercises = group.exercises.filter { it in filteredExercises }

                if (groupFilteredExercises.isNotEmpty()) {
                    // グループヘッダー
                    item {
                        Text(
                            text = when (group.groupName) {
                                TrainingViewModel.FAVORITE_GROUP_KEY -> stringResource(R.string.favorite)
                                null -> stringResource(R.string.no_group_display)
                                else -> group.groupName
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    // グループ内の種目
                    groupFilteredExercises.forEach { exercise ->
                        item {
                            ChallengeExerciseCard(
                                exercise = exercise,
                                records = records,
                                selectedPeriod = selectedPeriod,
                                isSelected = selectedExerciseFilter?.id == exercise.id,
                                onClick = { onExerciseClick(exercise) }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// 課題評価データクラス
data class ChallengeStatus(
    val level: Int,
    val status: ChallengeResult,
    val achievementRate: Int,
    val lastAchievedDate: String?
)

enum class ChallengeResult {
    Perfect,      // 🟢 100%以上
    Good,         // 🟦 75-99%
    NearlyThere,  // 🟡 50-74%
    NeedWork,     // 🔴 50%未満
    NoRecord      // - 未記録
}

// 種目カード（課題タブ用）
@Composable
fun ChallengeExerciseCard(
    exercise: Exercise,
    records: List<HistorySet>,
    selectedPeriod: Period?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val firstDay = firstDayOfWeek()
    val hasChallenge = exercise.targetSets != null && exercise.targetValue != null

    // 課題ありの場合、ステータスを計算（期間を考慮）
    val status = if (hasChallenge) {
        remember(exercise, records, selectedPeriod) {
            calculateChallengeStatus(exercise, records, selectedPeriod, firstDay = firstDay)
        }
    } else null

    // 日単位のクリア状況（ヒートストリップ用）
    val clearData = if (hasChallenge) {
        remember(exercise, records, selectedPeriod) {
            calculateClearDays(exercise, records, selectedPeriod, firstDay = firstDay)
        }
    } else null

    // 最終記録日を取得（全期間で固定）
    val lastRecordDate = remember(exercise, records) {
        records.filter { it.exerciseId == exercise.id }
            .maxByOrNull { "${it.date} ${it.time}" }
            ?.date
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 種目名とレベル
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // レベル（左側）
                if (exercise.sortOrder > 0) {
                    Text(
                        text = stringResource(R.string.level_format, exercise.sortOrder),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // 種目名
                Text(
                    text = exercise.name,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f), //takes up available space in the middle without taking space of checkmark icon
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 達成マーク
                if (hasChallenge && status != null && status.achievementRate >= 100 && status.status == ChallengeResult.Perfect) {
                    Icon(AppIcons.DoneFilled, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            // 課題ありの場合
            if (hasChallenge && status != null && clearData != null) {
                val progress = (status.achievementRate / 100f).coerceIn(0f, 1f)
                val actualTotal = calculateActualTotal(exercise, records, selectedPeriod, firstDay = firstDay)
                val unit = stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)

                // プログレスバー + 達成率
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearWavyProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    )
                    Text(
                        text = "${status.achievementRate}%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // ベスト / 目標
                Row {
                    Text(
                        text = "${stringResource(R.string.challenge_best)} $actualTotal$unit",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " / ${stringResource(R.string.challenge_target_label)} ${exercise.targetSets}×${exercise.targetValue}$unit",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ヒートストリップ（クリア=濃 / トレしたが未達=中 / 休み=薄）
                ChallengeHeatStrip(clearData)

                // クリア日数 / 最終記録日
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.challenge_clear_days, clearData.clearCount, clearData.totalDays),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (lastRecordDate != null) {
                        Text(
                            text = stringResource(R.string.last_record_short, lastRecordDate),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // 課題なしの場合
                Text(
                    text = stringResource(R.string.no_challenge_set),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 実績の合計値を計算（上位N個の合計）
fun calculateActualTotal(
    exercise: Exercise,
    records: List<HistorySet>,
    period: Period? = null,
    firstDay: DayOfWeek
): Int {
    val targetSets = exercise.targetSets ?: return 0
    var exerciseRecords = records.filter { it.exerciseId == exercise.id }

    // 期間フィルター適用（指定されている場合）
    if (period != null) {
        val today = java.time.LocalDate.now()
        val cutoffDate = period.startDate(today, firstDay)
        exerciseRecords = exerciseRecords.filter { record ->
            try {
                val recordDate = java.time.LocalDate.parse(record.date)
                recordDate >= cutoffDate && recordDate <= today
            } catch (e: Exception) {
                false
            }
        }
    }

    if (exerciseRecords.isEmpty()) return 0

    // セッションごとにグループ化して最良セッションの上位N個の合計を計算
    val sessions = exerciseRecords
        .groupBy { "${it.date}-${it.time}" }
        .map { (_, sessionRecords) ->
            if (exercise.laterality == "Unilateral") {
                // Unilateral: 右・左それぞれの上位N個を計算して平均
                val topRight = sessionRecords
                    .map { it.valueRight }
                    .sortedDescending()
                    .take(targetSets)
                    .sum()
                val topLeft = sessionRecords
                    .mapNotNull { it.valueLeft }
                    .sortedDescending()
                    .take(targetSets)
                    .sum()
                (topRight + topLeft) / 2
            } else {
                // Bilateral: 右側の上位N個の合計
                sessionRecords
                    .map { it.valueRight }
                    .sortedDescending()
                    .take(targetSets)
                    .sum()
            }
        }

    return sessions.maxOrNull() ?: 0
}

// 日単位のクリア状態
enum class DayClearState {
    Clear,    // その日のいずれかのセッションが目標達成（≧100%）
    Trained,  // トレーニングはしたが未達
    Rest      // トレーニングなし
}

// ヒートストリップ用データ
data class ClearDayData(
    val states: List<DayClearState>,  // 期間内の各日（古い順）
    val clearCount: Int,
    val totalDays: Int
)

// 1セッションの達成率を計算（calculateChallengeStatus と同じロジック）
private fun sessionAchievementRate(
    exercise: Exercise,
    sessionRecords: List<HistorySet>,
    targetSets: Int,
    targetTotal: Int
): Int {
    if (targetTotal <= 0) return 0
    return if (exercise.laterality == "Unilateral") {
        val topRight = sessionRecords
            .map { it.valueRight }
            .sortedDescending()
            .take(targetSets)
            .sum()
        val topLeft = sessionRecords
            .mapNotNull { it.valueLeft }
            .sortedDescending()
            .take(targetSets)
            .sum()
        val rateRight = (topRight * 100) / targetTotal
        val rateLeft = (topLeft * 100) / targetTotal
        (rateRight + rateLeft) / 2
    } else {
        val topValues = sessionRecords
            .map { it.valueRight }
            .sortedDescending()
            .take(targetSets)
            .sum()
        (topValues * 100) / targetTotal
    }
}

// 期間内の日単位クリア状況を計算
fun calculateClearDays(
    exercise: Exercise,
    records: List<HistorySet>,
    period: Period?,
    firstDay: DayOfWeek
): ClearDayData {
    val targetSets = exercise.targetSets
    val targetValue = exercise.targetValue
    val today = java.time.LocalDate.now()
    val exerciseRecords = records.filter { it.exerciseId == exercise.id }

    if (targetSets == null || targetValue == null || exerciseRecords.isEmpty()) {
        val days = period?.days ?: 0
        return ClearDayData(List(days) { DayClearState.Rest }, 0, days)
    }
    val targetTotal = targetSets * targetValue

    // 開始日：期間指定があればその起点、なければ最初の記録日
    val startDate = if (period != null) {
        period.startDate(today, firstDay)
    } else {
        exerciseRecords
            .mapNotNull { try { java.time.LocalDate.parse(it.date) } catch (e: Exception) { null } }
            .minOrNull() ?: today
    }
    val totalDays = (java.time.temporal.ChronoUnit.DAYS.between(startDate, today) + 1)
        .toInt().coerceAtLeast(0)

    // 日付ごとにグループ化（その日の最良セッションの達成率を見る）
    val recordsByDate = exerciseRecords.groupBy { it.date }

    val states = ArrayList<DayClearState>(totalDays)
    var clearCount = 0
    for (i in 0 until totalDays) {
        val date = startDate.plusDays(i.toLong()).toString()
        val dayRecords = recordsByDate[date]
        if (dayRecords.isNullOrEmpty()) {
            states.add(DayClearState.Rest)
        } else {
            val bestRate = dayRecords
                .groupBy { it.time }  // 同一時刻＝1セッション
                .map { (_, sessionRecords) ->
                    sessionAchievementRate(exercise, sessionRecords, targetSets, targetTotal)
                }
                .maxOrNull() ?: 0
            if (bestRate >= 100) {
                states.add(DayClearState.Clear)
                clearCount++
            } else {
                states.add(DayClearState.Trained)
            }
        }
    }
    return ClearDayData(states, clearCount, totalDays)
}

// ヒートストリップ（期間内の各日を3状態で表示）
@Composable
fun ChallengeHeatStrip(data: ClearDayData) {
    if (data.totalDays <= 0) return
    val gap = if (data.totalDays > 31) 0.dp else 1.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        data.states.forEach { state ->
            val color = when (state) {
                DayClearState.Clear -> MaterialTheme.colorScheme.primary
                DayClearState.Trained -> MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                DayClearState.Rest -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

// 課題ステータス計算関数
fun calculateChallengeStatus(
    exercise: Exercise,
    records: List<HistorySet>,
    period: Period? = null,
    firstDay: DayOfWeek
): ChallengeStatus {
    val targetSets = exercise.targetSets ?: return ChallengeStatus(
        level = exercise.sortOrder,
        status = ChallengeResult.NoRecord,
        achievementRate = 0,
        lastAchievedDate = null
    )
    val targetValue = exercise.targetValue ?: return ChallengeStatus(
        level = exercise.sortOrder,
        status = ChallengeResult.NoRecord,
        achievementRate = 0,
        lastAchievedDate = null
    )

    // この種目の全記録を取得
    var exerciseRecords = records.filter { it.exerciseId == exercise.id }

    // 期間フィルター適用（指定されている場合）
    if (period != null) {
        val today = java.time.LocalDate.now()
        val cutoffDate = period.startDate(today, firstDay)
        exerciseRecords = exerciseRecords.filter { record ->
            try {
                val recordDate = java.time.LocalDate.parse(record.date)
                recordDate >= cutoffDate && recordDate <= today
            } catch (e: Exception) {
                false
            }
        }
    }

    if (exerciseRecords.isEmpty()) {
        return ChallengeStatus(
            level = exercise.sortOrder,
            status = ChallengeResult.NoRecord,
            achievementRate = 0,
            lastAchievedDate = null
        )
    }

    // セッションごとにグループ化
    val targetTotal = targetSets * targetValue

    val sessions = exerciseRecords
        .groupBy { "${it.date}-${it.time}" }
        .map { (dateTime, sessionRecords) ->
            val rate = if (exercise.laterality == "Unilateral") {
                // Unilateral: 右・左それぞれの上位N個を計算
                val topRight = sessionRecords
                    .map { it.valueRight }
                    .sortedDescending()
                    .take(targetSets)
                    .sum()
                val topLeft = sessionRecords
                    .mapNotNull { it.valueLeft }
                    .sortedDescending()
                    .take(targetSets)
                    .sum()

                // 右・左それぞれの達成率を計算して平均
                val rateRight = (topRight * 100) / targetTotal
                val rateLeft = (topLeft * 100) / targetTotal
                (rateRight + rateLeft) / 2
            } else {
                // Bilateral: 右側の上位N個の合計
                val topValues = sessionRecords
                    .map { it.valueRight }
                    .sortedDescending()
                    .take(targetSets)
                    .sum()
                (topValues * 100) / targetTotal
            }

            Pair(dateTime, rate)
        }
        .sortedBy { it.first }  // 古い順にソート

    // 優先順位1: クリア条件を満たすセッション（達成率≥100%）
    val clearSessions = sessions.filter { (_, rate) ->
        rate >= 100
    }

    if (clearSessions.isNotEmpty()) {
        // 最も良いクリアセッションを採用
        val (dateTime, rate) = clearSessions.maxBy { it.second }
        val parts = dateTime.split("-")
        val achievedDate = if (parts.size >= 3) "${parts[0]}-${parts[1]}-${parts[2]}" else null

        return ChallengeStatus(
            level = exercise.sortOrder,
            status = ChallengeResult.Perfect,
            achievementRate = rate,
            lastAchievedDate = achievedDate
        )
    }

    // 優先順位2: 未達成だが最善を尽くしているセッション
    val bestSession = sessions.maxByOrNull { it.second }!!
    val (_, bestRate) = bestSession

    val status = when {
        bestRate >= 75 -> ChallengeResult.Good
        bestRate >= 50 -> ChallengeResult.NearlyThere
        else -> ChallengeResult.NeedWork
    }

    return ChallengeStatus(
        level = exercise.sortOrder,
        status = status,
        achievementRate = bestRate,
        lastAchievedDate = null
    )
}