package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.BuildConfig
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.AppLanguage
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.data.LanguagePreferences
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import io.github.gonbei774.calisthenicsmemory.viewmodel.CsvImportReport
import io.github.gonbei774.calisthenicsmemory.viewmodel.CsvType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenNew(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLicenses: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToCsvDataManagement: () -> Unit = {},
    onNavigateToShareHub: () -> Unit = {},
    currentTheme: AppTheme = AppTheme.SYSTEM,
    onThemeChange: (AppTheme) -> Unit = {},
    dynamicColor: Boolean = false,
    onDynamicColorChange: (Boolean) -> Unit = {},
    firstDayOfWeek: java.time.DayOfWeek? = null,
    onFirstDayOfWeekChange: (java.time.DayOfWeek?) -> Unit = {}
) {
    val context = LocalContext.current

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
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            AppIcons.Back,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ========================================
            // セクション: 言語設定
            // ========================================

            // セクションタイトルと説明
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.section_language_description),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            // 言語選択カード
            item {
                val languagePrefs = remember { LanguagePreferences(context) }
                var selectedLanguage by remember { mutableStateOf(languagePrefs.getLanguage()) }
                var showLanguageDialog by remember { mutableStateOf(false) }

                // 現在のシステム言語を取得
                val currentLocale = LocalConfiguration.current.locales[0].language

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.large,
                    onClick = { showLanguageDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AppIcons.Language, contentDescription = null, modifier = Modifier.size(32.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.language_setting),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    R.string.current_language,
                                    selectedLanguage.getDisplayName(currentLocale)
                                ),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // 言語選択ダイアログ
                if (showLanguageDialog) {
                    AlertDialog(
                        onDismissRequest = { showLanguageDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.language_setting),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppLanguage.entries.forEach { language ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedLanguage == language) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            }
                                        ),
                                        onClick = {
                                            android.util.Log.d("SettingsScreen", "Language selected: ${language.code}")
                                            selectedLanguage = language
                                            languagePrefs.setLanguage(language)
                                            android.util.Log.d("SettingsScreen", "Language saved, recreating activity")
                                            showLanguageDialog = false

                                            // Activity を再作成して言語を適用
                                            (context as? Activity)?.recreate()
                                        }
                                    ) {
                                        Text(
                                            text = language.getDisplayName(currentLocale),
                                            modifier = Modifier.padding(16.dp),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showLanguageDialog = false }) {
                                Text(stringResource(R.string.close))
                            }
                        }
                    )
                }
            }

            // ========================================
            // セクション: テーマ設定
            // ========================================

            // セクションタイトルと説明
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_theme),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.section_theme_description),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            // テーマ選択カード
            item {
                var showThemeDialog by remember { mutableStateOf(false) }

                val themeDisplayName = when (currentTheme) {
                    AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                    AppTheme.LIGHT -> stringResource(R.string.theme_light)
                    AppTheme.DARK -> stringResource(R.string.theme_dark)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.large,
                    onClick = { showThemeDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AppIcons.Theme, contentDescription = null, modifier = Modifier.size(32.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.theme_setting),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.current_theme, themeDisplayName),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // テーマ選択ダイアログ
                if (showThemeDialog) {
                    AlertDialog(
                        onDismissRequest = { showThemeDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.theme_setting),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppTheme.entries.forEach { theme ->
                                    val displayName = when (theme) {
                                        AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                                        AppTheme.LIGHT -> stringResource(R.string.theme_light)
                                        AppTheme.DARK -> stringResource(R.string.theme_dark)
                                    }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (currentTheme == theme) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            }
                                        ),
                                        onClick = {
                                            onThemeChange(theme)
                                            showThemeDialog = false
                                        }
                                    ) {
                                        Text(
                                            text = displayName,
                                            modifier = Modifier.padding(16.dp),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showThemeDialog = false }) {
                                Text(stringResource(R.string.close))
                            }
                        }
                    )
                }
            }

            // Wallpaper colours (Material You), Android 12+ only
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.dynamic_color_title),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.dynamic_color_description),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Switch(checked = dynamicColor, onCheckedChange = onDynamicColorChange)
                        }
                    }
                }
            }

            // First day of the week, used by Today's week strip, the Progress calendar and To Do.
            item {
                var showDialog by remember { mutableStateOf(false) }
                val locale = LocalConfiguration.current.locales[0]
                val automaticDay = java.time.temporal.WeekFields.of(locale).firstDayOfWeek
                fun dayName(day: java.time.DayOfWeek) = day.getDisplayName(java.time.format.TextStyle.FULL, locale)
                val automaticLabel = stringResource(R.string.first_day_of_week_automatic, dayName(automaticDay))
                val current = firstDayOfWeek?.let(::dayName) ?: automaticLabel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.large,
                    onClick = { showDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AppIcons.Today, contentDescription = null, modifier = Modifier.size(32.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.first_day_of_week),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = current,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                if (showDialog) {
                    val options: List<Pair<java.time.DayOfWeek?, String>> =
                        listOf<Pair<java.time.DayOfWeek?, String>>(null to automaticLabel) +
                            listOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY).map { it to dayName(it) }
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text(stringResource(R.string.first_day_of_week), fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                options.forEach { (day, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onFirstDayOfWeekChange(day)
                                                showDialog = false
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = day == firstDayOfWeek, onClick = null)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(label)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.close)) }
                        }
                    )
                }
            }

            // ========================================
            // セクション: ワークアウト設定
            // ========================================

            // セクションタイトルと説明
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_workout_settings),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.section_workout_settings_description),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            // ワークアウト設定カード
            item {
                val workoutPrefs = remember { io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences(context) }
                var prefillEnabled by remember { mutableStateOf(workoutPrefs.isPrefillPreviousRecordEnabled()) }
                var startCountdown by remember { mutableStateOf(workoutPrefs.getStartCountdown()) }
                var setInterval by remember { mutableStateOf(workoutPrefs.getSetInterval()) }
                var startCountdownEnabled by remember { mutableStateOf(workoutPrefs.isStartCountdownEnabled()) }
                var setIntervalEnabled by remember { mutableStateOf(workoutPrefs.isSetIntervalEnabled()) }
                var flashNotificationEnabled by remember { mutableStateOf(workoutPrefs.isFlashNotificationEnabled()) }
                var keepScreenOnEnabled by remember { mutableStateOf(workoutPrefs.isKeepScreenOnEnabled()) }
                var showStartCountdownDialog by remember { mutableStateOf(false) }
                var showSetIntervalDialog by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // プリフィル設定
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(AppIcons.RecordManually, contentDescription = null, modifier = Modifier.size(32.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_prefill_previous),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.settings_prefill_previous_description),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Switch(
                                checked = prefillEnabled,
                                onCheckedChange = { enabled ->
                                    prefillEnabled = enabled
                                    workoutPrefs.setPrefillPreviousRecordEnabled(enabled)
                                }
                            )
                        }
                    }

                    // 開始カウントダウン設定
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = MaterialTheme.shapes.large,
                        onClick = { if (startCountdownEnabled) showStartCountdownDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(AppIcons.Timer, contentDescription = null, modifier = Modifier.size(32.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.start_countdown_setting),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.current_start_countdown, startCountdown),
                                    fontSize = 14.sp,
                                    color = if (startCountdownEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Switch(
                                checked = startCountdownEnabled,
                                onCheckedChange = { enabled ->
                                    startCountdownEnabled = enabled
                                    workoutPrefs.setStartCountdownEnabled(enabled)
                                }
                            )
                        }
                    }

                    // セット間インターバル設定
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = MaterialTheme.shapes.large,
                        onClick = { if (setIntervalEnabled) showSetIntervalDialog = true }
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(AppIcons.Rest, contentDescription = null, modifier = Modifier.size(32.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.set_interval_setting),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.current_set_interval, setInterval),
                                        fontSize = 14.sp,
                                        color = if (setIntervalEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Switch(
                                    checked = setIntervalEnabled,
                                    onCheckedChange = { enabled ->
                                        setIntervalEnabled = enabled
                                        workoutPrefs.setSetIntervalEnabled(enabled)
                                    }
                                )
                            }
                            // 注意書き
                            Text(
                                text = stringResource(R.string.set_interval_note),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 68.dp, end = 20.dp, bottom = 16.dp)
                            )
                        }
                    }

                    // LEDフラッシュ通知設定
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(AppIcons.Camera, contentDescription = null, modifier = Modifier.size(32.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.flash_notification_setting),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.flash_notification_description),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Switch(
                                    checked = flashNotificationEnabled,
                                    onCheckedChange = { enabled ->
                                        flashNotificationEnabled = enabled
                                        workoutPrefs.setFlashNotificationEnabled(enabled)
                                    }
                                )
                            }
                        }
                    }

                    // 画面オン維持設定
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(AppIcons.Brightness, contentDescription = null, modifier = Modifier.size(32.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.keep_screen_on_setting),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.keep_screen_on_description),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Switch(
                                    checked = keepScreenOnEnabled,
                                    onCheckedChange = { enabled ->
                                        keepScreenOnEnabled = enabled
                                        workoutPrefs.setKeepScreenOnEnabled(enabled)
                                    }
                                )
                            }
                        }
                    }
                }

                // 開始カウントダウン設定ダイアログ
                if (showStartCountdownDialog) {
                    var inputValue by remember { mutableStateOf(startCountdown.toString()) }

                    AlertDialog(
                        onDismissRequest = { showStartCountdownDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.start_countdown_dialog_title),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            OutlinedTextField(
                                value = inputValue,
                                onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) inputValue = it },
                                label = { Text(stringResource(R.string.enter_seconds)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                                    focusedLabelColor = MaterialTheme.colorScheme.tertiary,
                                    cursorColor = MaterialTheme.colorScheme.tertiary
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val newValue = inputValue.toIntOrNull() ?: io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences.DEFAULT_START_COUNTDOWN
                                    workoutPrefs.setStartCountdown(newValue)
                                    startCountdown = newValue
                                    showStartCountdownDialog = false
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Text(stringResource(R.string.save))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showStartCountdownDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }

                // セット間インターバル設定ダイアログ
                if (showSetIntervalDialog) {
                    var inputValue by remember { mutableStateOf(setInterval.toString()) }
                    val maxInterval = io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences.MAX_SET_INTERVAL

                    AlertDialog(
                        onDismissRequest = { showSetIntervalDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.set_interval_dialog_title),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            OutlinedTextField(
                                value = inputValue,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.all { c -> c.isDigit() }) {
                                        // 上限チェック
                                        val intValue = newValue.toIntOrNull()
                                        if (intValue == null || intValue <= maxInterval) {
                                            inputValue = newValue
                                        }
                                    }
                                },
                                label = { Text(stringResource(R.string.enter_seconds_max, maxInterval)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                                    focusedLabelColor = MaterialTheme.colorScheme.tertiary,
                                    cursorColor = MaterialTheme.colorScheme.tertiary
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val newValue = (inputValue.toIntOrNull() ?: io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences.DEFAULT_SET_INTERVAL)
                                        .coerceIn(1, maxInterval)
                                    workoutPrefs.setSetInterval(newValue)
                                    setInterval = newValue
                                    showSetIntervalDialog = false
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Text(stringResource(R.string.save))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSetIntervalDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }

            // ========================================
            // セクション: データ管理
            // ========================================

            // セクションタイトル
            item {
                Text(
                    text = stringResource(R.string.data_management),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // ナビゲーションカード: 完全バックアップ
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.large,
                    onClick = { onNavigateToBackup() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AppIcons.Save, contentDescription = null, modifier = Modifier.size(32.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.section_full_backup),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.section_full_backup_description),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 4
                            )
                        }
                        Icon(AppIcons.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ナビゲーションカード: 部分データ管理 (CSV)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.large,
                    onClick = { onNavigateToCsvDataManagement() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AppIcons.List, contentDescription = null, modifier = Modifier.size(32.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.section_partial_data_management),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.section_partial_data_management_description),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 2
                            )
                        }
                        Icon(AppIcons.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ナビゲーションカード: Share
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.large,
                    onClick = { onNavigateToShareHub() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AppIcons.Share, contentDescription = null, modifier = Modifier.size(32.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.share_section_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.share_section_description),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 2
                            )
                        }
                        Icon(AppIcons.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ========================================
            // セクション: アプリ情報
            // ========================================

            // セクションタイトルと説明
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_app_info),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.section_app_info_description),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            // アプリ情報カード
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // アプリ名と説明（中央揃え）
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.app_description),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // バージョン
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.app_version),
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = BuildConfig.VERSION_NAME,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // ソースコード
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL))
                                    context.startActivity(intent)
                                },
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.SourceCode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.app_source_code),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 使用許諾（ライセンス）
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToLicenses() },
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(AppIcons.Document, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                text = stringResource(R.string.open_source_licenses),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 著者カード
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // セクションタイトル
                        Text(
                            text = stringResource(R.string.app_author),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 開発者
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.Exercise,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Gonbei774",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // The upstream project this app is based on (GPL-3.0 attribution).
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UPSTREAM_URL)))
                                },
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.SourceCode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.app_original_project),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // フィードバックカード
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // セクションタイトル
                        Text(
                            text = stringResource(R.string.app_feedback),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // GitHubで問題を報告
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$REPOSITORY_URL/issues"))
                                    context.startActivity(intent)
                                },
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(AppIcons.RecordManually, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                text = stringResource(R.string.report_issue_github),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * CSV種類を多言語化された文字列に変換する関数
 */
@Composable
fun getCsvTypeLocalizedString(csvType: CsvType?): String {
    return when (csvType) {
        CsvType.GROUPS -> stringResource(R.string.groups)
        CsvType.EXERCISES -> stringResource(R.string.exercises)
        CsvType.RECORDS -> stringResource(R.string.records)
        null -> stringResource(R.string.unknown_short)
    }
}

/**
 * CSV種類を自動判定する関数
 */
fun detectCsvType(csvString: String): CsvType? {
    val firstLine = csvString.lines()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .firstOrNull() ?: return null

    return when {
        firstLine.trim() == "name" -> CsvType.GROUPS
        firstLine.startsWith("name,type,group,sortOrder") -> CsvType.EXERCISES
        firstLine.startsWith("exerciseName,exerciseType") -> CsvType.RECORDS
        else -> null
    }
}

/**
 * CSVインポートを実行する関数
 */
suspend fun executeCsvImport(
    viewModel: TrainingViewModel,
    csvData: String,
    csvImportType: CsvType?
): CsvImportReport? {
    return withContext(Dispatchers.IO) {
        when (csvImportType) {
            CsvType.GROUPS -> viewModel.importGroups(csvData)
            CsvType.EXERCISES -> viewModel.importExercises(csvData)
            CsvType.RECORDS -> viewModel.importRecordsFromCsv(csvData)
            else -> null
        }
    }
}

/** This app's own repository: source code and issue reports. */
private const val REPOSITORY_URL = "https://github.com/WildeBeast2521/CalisthenicsMemory"

/** The upstream project this app is based on. */
private const val UPSTREAM_URL = "https://codeberg.org/Gonbei774/CalisthenicsMemory"
