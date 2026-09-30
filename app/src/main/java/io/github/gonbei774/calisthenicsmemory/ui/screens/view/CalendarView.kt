package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import android.text.format.DateUtils
import androidx.compose.ui.draw.alpha
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.weekOf
import io.github.gonbei774.calisthenicsmemory.ui.theme.firstDayOfWeek
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalRecord
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import org.json.JSONArray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarView(
    items: List<RecordItem>,
    exercises: List<Exercise>,
    selectedExerciseFilter: Exercise?,
    selectedPeriod: Period?,
    onExerciseClick: (Exercise) -> Unit,
    focusDate: LocalDate? = null,
    onFocusShown: () -> Unit = {},
) {
    val firstDay = firstDayOfWeek()

    // 種目ID → Lv (sortOrder) マップ
    val exerciseLevelMap = remember(exercises) {
        exercises.associate { it.id to it.sortOrder }
    }

    // 各日の活動量スコアと記録タイプを集計
    val dayInfoMap = remember(items, exerciseLevelMap) {
        items.groupBy { it.date }.mapValues { (_, dayItems) ->
            val sessionScore = dayItems.filterIsInstance<RecordItem.Session>()
                .sumOf { sItem ->
                    sItem.session.records.sumOf { r -> calcSessionScore(r, exerciseLevelMap) }
                }
            val intervalScore = dayItems.filterIsInstance<RecordItem.Interval>()
                .sumOf { calcIntervalScore(it.record) }
            DayInfo(
                hasSession = dayItems.any { it is RecordItem.Session },
                hasInterval = dayItems.any { it is RecordItem.Interval },
                score = sessionScore + intervalScore,
                items = dayItems
            )
        }
    }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    LaunchedEffect(focusDate) {
        if (focusDate != null) {
            selectedDate = focusDate
            onFocusShown()
        }
    }
    val today = remember { LocalDate.now() }
    val exerciseMap = remember(exercises) { exercises.associateBy { it.id } }

    val statsRange = remember(items, selectedPeriod, selectedDate, today) {
        when {
            selectedDate != null -> selectedDate!! to selectedDate!!
            selectedPeriod != null -> selectedPeriod.startDate(today, firstDay) to today
            else -> {
                val earliest = items.mapNotNull {
                    try { LocalDate.parse(it.date) } catch (_: Exception) { null }
                }.minOrNull() ?: today
                earliest to today
            }
        }
    }

    val stats = remember(items, selectedExerciseFilter, selectedDate) {
        val targetItems = selectedDate?.let { d ->
            val key = d.toString()
            items.filter { it.date == key }
        } ?: items
        val sessions = targetItems.filterIsInstance<RecordItem.Session>()
        val totalSets = sessions.sumOf { it.session.records.size }
        val exerciseCount = sessions.map { it.session.exerciseId }.distinct().size
        val intervalCount = targetItems.count { it is RecordItem.Interval }
        val totalValue = if (selectedExerciseFilter != null) {
            sessions.sumOf { s ->
                s.session.records.sumOf { r -> r.valueRight + (r.valueLeft ?: 0) }
            }
        } else 0
        StatsSummary(
            totalSets = totalSets,
            exerciseCount = exerciseCount,
            intervalCount = intervalCount,
            totalValue = totalValue
        )
    }

    if (selectedPeriod == Period.OneWeek) {
        // 週間表示（1週間フィルター時）
        // The calendar week, as on Today, so later days show as still to come.
        val weekDays = remember(today, firstDay) { weekOf(today, firstDay) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 曜日 + 日付の大きなセル行
            item(key = "week-grid") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    weekDays.forEach { date ->
                        val dateKey = date.toString()
                        val dayInfo = dayInfoMap[dateKey]
                        val isToday = date == today
                        val isSelected = date == selectedDate

                        WeekDayCell(
                            date = date,
                            isToday = isToday,
                            isSelected = isSelected,
                            level = scoreToLevel(dayInfo?.score ?: 0.0),
                            hasInterval = dayInfo?.hasInterval == true,
                            onClick = {
                                selectedDate = if (selectedDate == date) null else date
                            },
                            modifier = Modifier.weight(1f),
                            future = date.isAfter(today)
                        )
                    }
                }
            }

            item(key = "week-stats") {
                StatsCard(
                    stats = stats,
                    selectedExercise = selectedExerciseFilter,
                    rangeStart = statsRange.first,
                    rangeEnd = statsRange.second
                )
            }

            // 選択日の記録サマリー
            val selected = selectedDate
            if (selected != null) {
                val dayItems = dayInfoMap[selected.toString()]?.items
                if (!dayItems.isNullOrEmpty()) {
                    item(key = "week-summary") {
                        DayRecordSummary(
                            date = selected,
                            items = dayItems,
                            exerciseMap = exerciseMap,
                            onExerciseClick = onExerciseClick
                        )
                    }
                }
            } else {
                // 日付未タップ → 1週間内の全記録を日付ごとに表示
                weekDays.forEach { date ->
                    val dayItems = dayInfoMap[date.toString()]?.items
                    if (!dayItems.isNullOrEmpty()) {
                        item(key = "week-all-${date}") {
                            DayRecordSummary(
                                date = date,
                                items = dayItems,
                                exerciseMap = exerciseMap,
                                onExerciseClick = onExerciseClick
                            )
                        }
                    }
                }
            }
        }
    } else {
        // 月間表示（通常・1ヶ月・3ヶ月）
        val months = remember(items, selectedPeriod, today) {
            val now = YearMonth.from(today)
            val dataDates = items.mapNotNull { item ->
                try {
                    LocalDate.parse(item.date)
                } catch (e: Exception) {
                    null
                }
            }
            val periodStart = selectedPeriod?.let {
                YearMonth.from(it.startDate(today, firstDay))
            }
            val earliest = when {
                periodStart != null -> periodStart
                dataDates.isNotEmpty() -> YearMonth.from(dataDates.min())
                else -> now
            }
            val latest = maxOf(
                dataDates.maxOfOrNull { YearMonth.from(it) } ?: now,
                now
            )
            generateSequence(earliest) { it.plusMonths(1) }
                .takeWhile { it <= latest }
                .toList()
        }

        val listState = rememberLazyListState()
        LaunchedEffect(months.size) {
            if (months.isNotEmpty()) {
                listState.scrollToItem(months.size - 1)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(months, key = { it.toString() }) { yearMonth ->
                MonthGrid(
                    yearMonth = yearMonth,
                    today = today,
                    selectedDate = selectedDate,
                    dayInfoMap = dayInfoMap,
                    selectedPeriod = selectedPeriod,
                    onDateClick = { date ->
                        selectedDate = if (selectedDate == date) null else date
                    }
                )
            }

            item(key = "month-stats") {
                StatsCard(
                    stats = stats,
                    selectedExercise = selectedExerciseFilter,
                    rangeStart = statsRange.first,
                    rangeEnd = statsRange.second
                )
            }

            // 選択日の記録サマリー
            val selected = selectedDate
            if (selected != null) {
                val dayItems = dayInfoMap[selected.toString()]?.items
                if (!dayItems.isNullOrEmpty()) {
                    item(key = "month-selected-summary") {
                        DayRecordSummary(
                            date = selected,
                            items = dayItems,
                            exerciseMap = exerciseMap,
                            onExerciseClick = onExerciseClick
                        )
                    }
                }
            }

            // 期間フィルターあり + 日付未タップ → 期間内の全記録を日付ごとに表示
            if (selectedPeriod != null && selectedDate == null) {
                val sortedDates = dayInfoMap.keys.sorted()
                sortedDates.forEach { dateKey ->
                    val dayItems = dayInfoMap[dateKey]?.items
                    if (!dayItems.isNullOrEmpty()) {
                        val date = try { LocalDate.parse(dateKey) } catch (e: Exception) { null }
                        if (date != null) {
                            item(key = "period-summary-$dateKey") {
                                DayRecordSummary(
                                    date = date,
                                    items = dayItems,
                                    exerciseMap = exerciseMap,
                                    onExerciseClick = onExerciseClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate?,
    dayInfoMap: Map<String, DayInfo>,
    selectedPeriod: Period? = null,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDay = firstDayOfWeek()
    val cutoffDate = remember(selectedPeriod, today) {
        selectedPeriod?.let { it.startDate(today, firstDay) }
    }

    Column {
        // 月ヘッダー
        Text(
            text = formatYearMonth(yearMonth),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 曜日ヘッダー
        val locale = LocalConfiguration.current.locales[0]
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDaysFrom(firstDay).forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, locale),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 日付グリッド
        val firstDayOfMonth = yearMonth.atDay(1)
        // Columns start on the chosen first day of the week.
        val startOffset = (firstDayOfMonth.dayOfWeek.value - firstDay.value + 7) % 7
        val daysInMonth = yearMonth.lengthOfMonth()
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayOfMonth = cellIndex - startOffset + 1

                    if (dayOfMonth in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayOfMonth)
                        val dateKey = date.toString()
                        val dayInfo = dayInfoMap[dateKey]
                        val isToday = date == today
                        val isSelected = date == selectedDate

                        val isOutOfRange = cutoffDate != null && (date < cutoffDate || date > today)

                        DayCell(
                            dayOfMonth = dayOfMonth,
                            isToday = isToday,
                            isSelected = isSelected,
                            isOutOfRange = isOutOfRange,
                            level = scoreToLevel(dayInfo?.score ?: 0.0),
                            hasInterval = dayInfo?.hasInterval == true,
                            onClick = { onDateClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .border(0.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }
    }
}

private data class DayInfo(
    val hasSession: Boolean,
    val hasInterval: Boolean,
    val score: Double,
    val items: List<RecordItem>
)

private data class StatsSummary(
    val totalSets: Int,
    val exerciseCount: Int,
    val intervalCount: Int,
    val totalValue: Int
)

@Composable
private fun StatsCard(
    stats: StatsSummary,
    selectedExercise: Exercise?,
    rangeStart: LocalDate,
    rangeEnd: LocalDate
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = formatStatsRange(rangeStart, rangeEnd),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StatsRow(label = stringResource(R.string.stats_total_sets), value = stats.totalSets.toString())
            if (selectedExercise == null) {
                StatsRow(label = stringResource(R.string.stats_exercise_count), value = stats.exerciseCount.toString())
                StatsRow(label = stringResource(R.string.stats_total_intervals), value = stats.intervalCount.toString())
            } else {
                if (selectedExercise.type == "Isometric") {
                    StatsRow(label = stringResource(R.string.stats_total_time), value = formatTotalSeconds(stats.totalValue))
                } else {
                    StatsRow(label = stringResource(R.string.stats_total_reps), value = stats.totalValue.toString())
                }
            }
        }
    }
}

@Composable
private fun StatsRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun formatStatsRange(start: LocalDate, end: LocalDate): String {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
    // formatDateRange treats the end as exclusive, so the last day is included by ending after it.
    val endMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return DateUtils.formatDateRange(
        context,
        startMillis,
        endMillis,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_YEAR
    )
}

@Composable
private fun formatTotalSeconds(totalSeconds: Int): String {
    if (totalSeconds < 60) return stringResource(R.string.stats_total_seconds_format, totalSeconds)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (seconds == 0) {
        stringResource(R.string.stats_total_minutes_format, minutes)
    } else {
        stringResource(R.string.stats_total_min_sec_format, minutes, seconds)
    }
}

@Composable
private fun DayCell(
    dayOfMonth: Int,
    isToday: Boolean,
    isSelected: Boolean,
    isOutOfRange: Boolean = false,
    level: Int,
    hasInterval: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showHeatmap = level > 0 && !isOutOfRange
    val textColor = when {
        isOutOfRange -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        isToday && !showHeatmap -> MaterialTheme.colorScheme.primary
        showHeatmap -> heatContent(level)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(
                if (showHeatmap) heatBackground(level) else Color.Transparent
            )
            .border(0.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface) else Modifier
            )
            .clickable(onClick = onClick)
    ) {
        // 日付テキスト（中央）
        Text(
            text = dayOfMonth.toString(),
            fontSize = 14.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
        // インターバルマーカー（右上）
        if (hasInterval) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-3).dp, y = 3.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
            )
        }
    }
}

@Composable
private fun WeekDayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    level: Int,
    hasInterval: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    future: Boolean = false,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dayOfWeekText = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .background(
                if (level > 0) heatBackground(level) else Color.Transparent,
                shape
            )
            .border(0.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), shape)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, shape) else Modifier
            )
            // Days still to come are faint and hold nothing to select, as on Today.
            .clickable(enabled = !future, onClick = onClick)
            .alpha(if (future) 0.4f else 1f)
            .padding(vertical = 8.dp)
    ) {
        // 曜日 + 日付（中央）
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = dayOfWeekText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 20.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isToday && level == 0 -> MaterialTheme.colorScheme.primary
                    level > 0 -> heatContent(level)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center
            )
        }
        // インターバルマーカー（右上）
        if (hasInterval) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
            )
        }
    }
}

@Composable
private fun DayRecordSummary(
    date: LocalDate,
    items: List<RecordItem>,
    exerciseMap: Map<Long, Exercise>,
    onExerciseClick: (Exercise) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 日付ヘッダー
            Text(
                text = formatDate(date, LocalConfiguration.current.locales[0]),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 同じ種目のセッションは1行に集約し、合計set数を表示する
            // （種目の初出順を維持。groupBy は LinkedHashMap で挿入順を保つ）
            val aggregatedSessions = items
                .filterIsInstance<RecordItem.Session>()
                .groupBy { it.session.exerciseId }
                .map { (exerciseId, sessionItems) ->
                    exerciseId to sessionItems.sumOf { it.session.records.size }
                }
            aggregatedSessions.forEach { (exerciseId, setCount) ->
                SessionSummaryRow(
                    exerciseId = exerciseId,
                    setCount = setCount,
                    exerciseMap = exerciseMap,
                    onExerciseClick = onExerciseClick
                )
            }

            items.filterIsInstance<RecordItem.Interval>().forEach { item ->
                IntervalSummaryRow(
                    record = item.record
                )
            }
        }
    }
}

@Composable
private fun SessionSummaryRow(
    exerciseId: Long,
    setCount: Int,
    exerciseMap: Map<Long, Exercise>,
    onExerciseClick: (Exercise) -> Unit
) {
    val exercise = exerciseMap[exerciseId]
    val exerciseName = exercise?.name ?: "?"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
                if (exercise != null) {
                    Modifier.clickable { onExerciseClick(exercise) }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Text(
                text = exerciseName,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = pluralStringResource(R.plurals.set_count, setCount, setCount),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IntervalSummaryRow(
    record: IntervalRecord
) {
    val isFullCompletion = record.completedRounds == record.rounds

    val exerciseNames = remember(record.exercisesJson) {
        try {
            val arr = JSONArray(record.exercisesJson)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) { emptyList() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
            )
            Column {
                Text(
                    text = record.programName,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (exerciseNames.isNotEmpty()) {
                    Text(
                        text = exerciseNames.joinToString(", "),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Text(
            text = "${record.completedRounds}/${record.rounds}",
            fontSize = 13.sp,
            color = if (isFullCompletion) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isFullCompletion) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun calcSessionScore(record: HistorySet, levelMap: Map<Long, Int>): Double {
    val lv = (levelMap[record.exerciseId] ?: 0).coerceAtLeast(1)
    val base = record.valueRight + (record.valueLeft ?: 0)
    return base * Math.pow(1.3, (lv - 1).toDouble())
}

private fun calcIntervalScore(record: IntervalRecord): Double {
    val exerciseCount = try {
        JSONArray(record.exercisesJson).length()
    } catch (e: Exception) {
        0
    }
    return record.workSeconds.toDouble() *
        (record.completedRounds * exerciseCount + record.completedExercisesInLastRound)
}

private fun scoreToLevel(score: Double): Int = when {
    score <= 0.0 -> 0
    score <= 100.0 -> 1
    score <= 300.0 -> 2
    score <= 700.0 -> 3
    else -> 4
}

/**
 * How much was trained on a day, from light to full spruce. Levels 1 and 2 use the container
 * colour and levels 3 and 4 the primary colour, each with its own readable text colour.
 */
@Composable
private fun heatBackground(level: Int): Color = when (level) {
    1 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    2 -> MaterialTheme.colorScheme.primaryContainer
    3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun heatContent(level: Int): Color =
    if (level >= 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer

private fun formatYearMonth(yearMonth: YearMonth): String {
    val locale = Locale.getDefault()
    return if (locale.language == "ja") {
        "${yearMonth.year}年${yearMonth.monthValue}月"
    } else {
        "${yearMonth.month.getDisplayName(TextStyle.FULL, locale)} ${yearMonth.year}"
    }
}
