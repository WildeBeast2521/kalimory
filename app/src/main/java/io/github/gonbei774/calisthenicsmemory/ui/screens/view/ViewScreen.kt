package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import io.github.gonbei774.calisthenicsmemory.ui.components.common.ExerciseBadges
import io.github.gonbei774.calisthenicsmemory.util.AppClock
import io.github.gonbei774.calisthenicsmemory.ui.components.countText
import androidx.compose.animation.AnimatedVisibility
import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmSearchField
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
import androidx.compose.ui.res.pluralStringResource
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
import io.github.gonbei774.calisthenicsmemory.data.v2.countsAsWork
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutRef
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.data.progression.ChainProgress
import io.github.gonbei774.calisthenicsmemory.data.progression.Progressions
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Standard
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

private const val PROGRESSIONS_PAGE = 3

/** The saved workout a card belongs to: its v2 session, or the legacy save its first record came from. */
internal fun SessionInfo.workoutRef(): WorkoutRef? {
    val first = records.firstOrNull() ?: return null
    return if (source == HistorySource.V2) first.workoutSessionId?.let(WorkoutRef::V2)
    else WorkoutRef.Legacy(first.date, first.time, first.comment)
}

/**
 * History's cards, newest first: one per exercise and saved minute, and for v2 sets also per
 * workout, so two workouts saved in the same minute never share a card (ADR 0009, decision 7).
 */
internal fun historyCards(records: List<HistorySet>): List<SessionInfo> =
    records
        .groupBy { listOf(it.source, it.exerciseId, it.date, it.time, it.workoutSessionId) }
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
    onOpenBodyMap: () -> Unit = {},
    onOpenWeeklyReview: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
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
    // The week suits the calendar; the graph starts on a month unless the person has picked a period.
    var periodChosen by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState.currentPage) {
        if (!periodChosen && pagerState.currentPage == 2) selectedPeriod = Period.OneMonth
    }
    var showFilterBottomSheet by remember { mutableStateOf(false) }

    // 既存の状態変数
    var showDeleteDialog by remember { mutableStateOf<SessionInfo?>(null) }
    var editingWorkoutOf by remember { mutableStateOf<SessionInfo?>(null) }
    var showIntervalDeleteDialog by remember { mutableStateOf<RecordItem.Interval?>(null) }
    var showIntervalEditDialog by remember { mutableStateOf<RecordItem.Interval?>(null) }

    // 一覧モード用のセッションデータ
    val sessions = remember(records, exercises) { historyCards(records) }

    // フィルター済みセッション（種目＋期間）
    val filteredSessions = remember(sessions, selectedExerciseFilter, selectedPeriod) {
        var filtered = sessions

        // 種目フィルター
        if (selectedExerciseFilter != null) {
            filtered = filtered.filter { it.exerciseId == selectedExerciseFilter!!.id }
        }

        // 期間フィルター
        if (selectedPeriod != null) {
            val today = AppClock.today()
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
            val today = AppClock.today()
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
            // The same large title as the other primary destinations, with the body map at its end.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.nav_progress),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() }
                        .padding(start = Spacing.l, end = Spacing.l, top = Spacing.xl, bottom = Spacing.s)
                )
                IconButton(onClick = onOpenWeeklyReview, modifier = Modifier.padding(top = Spacing.xl)) {
                    Icon(AppIcons.Chart, contentDescription = stringResource(R.string.weekly_review_open), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onOpenBodyMap, modifier = Modifier.padding(top = Spacing.xl, end = Spacing.s)) {
                    Icon(AppIcons.BodyMap, contentDescription = stringResource(R.string.body_map_open), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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
            ProgressTabRow(
                titles = tabTitles,
                selected = pagerState.currentPage,
                onSelect = { index -> coroutineScope.launch { pagerState.animateScrollToPage(index) } },
            )

            // Exercise and period filters apply to the record tabs, not to Progressions.
            AnimatedVisibility(visible = pagerState.targetPage != PROGRESSIONS_PAGE) {
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
                        options = listOf<Period?>(Period.OneWeek, Period.OneMonth, Period.ThreeMonths, Period.OneYear, null),
                        selected = selectedPeriod,
                        onSelect = {
                            periodChosen = true
                            selectedPeriod = it
                        },
                        label = { stringResource(it?.displayNameResId ?: R.string.period_all) },
                        fill = false,
                    )
                }
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
                            // A tapped set opens the whole workout it belongs to, in the one editor.
                            onRecordClick = { record ->
                                editingWorkoutOf = filteredSessions.firstOrNull { card -> record in card.records }
                            },
                            onSessionLongPress = { session ->
                                editingWorkoutOf = session
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
                                selectedPeriod = selectedPeriod,
                                onOpenLibrary = onOpenLibrary,
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
                            // This tab hides the exercise and period chips, so it applies neither:
                            // targets are judged over all history, as the chain cards are.
                            selectedExerciseFilter = null,
                            selectedPeriod = null,
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
            title = {
                Text(
                    stringResource(R.string.delete_workout_title),
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
                    Text(stringResource(R.string.cancel))
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

    // The whole workout behind a card, in the workout editor (ADR 0009).
    editingWorkoutOf?.workoutRef()?.let { ref ->
        WorkoutEditorScreen(ref = ref, viewModel = viewModel, onClose = { editingWorkoutOf = null })
    }

    // Delete Dialog
    showDeleteDialog?.let { session ->
        val exercise = exercises.find { it.id == session.exerciseId }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_workout_title)) },
            text = {
                Text(stringResource(
                    R.string.delete_record_warning,
                    exercise?.name ?: stringResource(R.string.unknown_short),
                    session.date,
                    session.time,
                    countText(R.plurals.set_count, session.records.size)
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

    // 検索フィルター: groups are found by the names shown on them too (#278).
    val favoriteLabel = stringResource(R.string.favorite)
    val noGroupLabel = stringResource(R.string.no_group_display)
    val filteredHierarchicalData = remember(hierarchicalData, searchQuery, favoriteLabel, noGroupLabel) {
        SearchUtils.searchHierarchicalExercises(hierarchicalData, searchQuery) { name ->
            when (name) {
                TrainingViewModel.FAVORITE_GROUP_KEY -> favoriteLabel
                null -> noGroupLabel
                else -> name
            }
        }
    }
    // While searching, every group with a result is open, so the matches show without a tap; a group
    // can still be closed. Clearing the search closes them again.
    LaunchedEffect(filteredHierarchicalData, searchQuery.isBlank()) {
        expandedGroups = if (searchQuery.isBlank()) emptySet() else filteredHierarchicalData.map { it.groupName }.toSet()
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
        CalmSearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = stringResource(R.string.search_exercise),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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
            if (searchQuery.isNotBlank() && filteredHierarchicalData.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_results),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
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
                            shape = MaterialTheme.shapes.extraSmall
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
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = pluralStringResource(R.plurals.exercise_count, group.exercises.size, group.exercises.size),
                                        style = MaterialTheme.typography.bodyMedium,
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )

                // バッジ行（テキストのみ、スペース区切り）
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    ExerciseBadges(exercise)
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
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            if (isSelected) {
                Icon(
                    imageVector = AppIcons.Check,
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
        // Only exercises that have a target belong in this list.
        var filtered = exercises.filter { it.targetSets != null && it.targetValue != null }

        // 種目フィルター
        if (selectedExerciseFilter != null) {
            filtered = listOf(selectedExerciseFilter!!)
        }

        // 期間フィルター（nullの場合は全期間）
        if (selectedPeriod != null) {
            val today = AppClock.today()
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
                            style = MaterialTheme.typography.bodyLarge,
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
        // The last month, a set window now that this tab applies no period filter.
        remember(exercise, records) {
            calculateClearDays(exercise, records, Period.OneMonth, firstDay = firstDay)
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
        shape = MaterialTheme.shapes.large,
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
                        style = MaterialTheme.typography.bodySmall,
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
                        text = pluralStringResource(R.plurals.challenge_clear_days, clearData.totalDays, clearData.clearCount, clearData.totalDays),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (lastRecordDate != null) {
                        Text(
                            text = stringResource(R.string.last_record_short, lastRecordDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
    // Judged on sets of work; warm-ups are left out (ADR 0012).
    var exerciseRecords = records.filter { it.countsAsWork && it.exerciseId == exercise.id }

    // 期間フィルター適用（指定されている場合）
    if (period != null) {
        val today = AppClock.today()
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

fun calculateClearDays(
    exercise: Exercise,
    records: List<HistorySet>,
    period: Period?,
    firstDay: DayOfWeek
): ClearDayData {
    val targetSets = exercise.targetSets
    val targetValue = exercise.targetValue
    val today = AppClock.today()
    val exerciseRecords = records.filter { it.exerciseId == exercise.id }

    if (targetSets == null || targetValue == null || exerciseRecords.isEmpty()) {
        val days = period?.days ?: 0
        return ClearDayData(List(days) { DayClearState.Rest }, 0, days)
    }
    val standard = Standard(targetSets, targetValue)
    val unilateral = exercise.laterality == "Unilateral"

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
            // A day clears the target when one of its sessions meets it, by the chain cards' rule. A day of
            // warm-ups only is a day trained, but warm-ups never reach the target (ADR 0012).
            if (Progressions.sessions(dayRecords, unilateral).any { Progressions.meets(it, standard) }) {
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
    // Judged on sets of work; warm-ups are left out (ADR 0012).
    var exerciseRecords = records.filter { it.countsAsWork && it.exerciseId == exercise.id }

    // 期間フィルター適用（指定されている場合）
    if (period != null) {
        val today = AppClock.today()
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
    // The chain cards' rule (Progressions): the best sets, each counted up to the target, the weaker
    // side of a one-sided set, warm-ups and assisted sets left out. The old rule summed sets uncapped
    // and averaged the sides, so one set of 30 "cleared" 3 x 10.
    val standard = Standard(targetSets, targetValue)
    val sessions = Progressions.sessions(exerciseRecords, exercise.laterality == "Unilateral")
    if (sessions.isEmpty()) {
        return ChallengeStatus(level = exercise.sortOrder, status = ChallengeResult.NoRecord, achievementRate = 0, lastAchievedDate = null)
    }

    val lastMet = sessions.lastOrNull { Progressions.meets(it, standard) }
    if (lastMet != null) {
        return ChallengeStatus(
            level = exercise.sortOrder,
            status = ChallengeResult.Perfect,
            achievementRate = 100,
            lastAchievedDate = lastMet.date
        )
    }

    val bestRate = sessions.maxOf { Progressions.percentOf(it, standard) }

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