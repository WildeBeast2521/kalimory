package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.ui.components.common.SettingRow
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SettingChoiceRow
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SettingValueRow
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SettingSwitchRow
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SettingLinkRow
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SettingsGroup
import androidx.core.net.toUri
import androidx.compose.runtime.mutableIntStateOf
import io.github.gonbei774.calisthenicsmemory.ui.components.common.NumberStepper
import io.github.gonbei774.calisthenicsmemory.ui.components.countText
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmTopBar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.TopAppBarDefaults
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.components.common.ConnectedChoices
import io.github.gonbei774.calisthenicsmemory.data.ProgressionPreferences
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
    onOpenWelcomeGuide: () -> Unit = {},
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

    val topBarScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(topBarScroll.nestedScrollConnection),
        topBar = { CalmTopBar(title = stringResource(R.string.settings), onBack = onNavigateBack, scrollBehavior = topBarScroll) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            // Language
            item {
                val languagePrefs = remember { LanguagePreferences(context) }
                var selectedLanguage by remember { mutableStateOf(languagePrefs.getLanguage()) }
                var showLanguageDialog by remember { mutableStateOf(false) }

                SettingsGroup(stringResource(R.string.section_language)) {
                    SettingLinkRow(
                        title = stringResource(R.string.language_setting),
                        description = stringResource(
                            R.string.current_language,
                            if (selectedLanguage == AppLanguage.SYSTEM) stringResource(R.string.theme_system) else selectedLanguage.nativeName
                        ),
                        icon = AppIcons.Language,
                        onClick = { showLanguageDialog = true },
                    )
                }

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
                            // Eleven choices outgrow a dialog on small screens, so the list scrolls.
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
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
                                            selectedLanguage = language
                                            languagePrefs.setLanguage(language)
                                            showLanguageDialog = false
                                            // The activity is recreated so the language applies.
                                            (context as? Activity)?.recreate()
                                        }
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.SYSTEM) stringResource(R.string.theme_system) else language.nativeName,
                                            modifier = Modifier.padding(16.dp),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            OutlinedButton(onClick = { showLanguageDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }

            // Appearance
            item {
                @Composable
                fun themeName(theme: AppTheme) = when (theme) {
                    AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                    AppTheme.LIGHT -> stringResource(R.string.theme_light)
                    AppTheme.DARK -> stringResource(R.string.theme_dark)
                    AppTheme.AMOLED -> stringResource(R.string.theme_amoled)
                }
                SettingsGroup(stringResource(R.string.section_theme)) {
                    SettingChoiceRow(
                        title = stringResource(R.string.theme_setting),
                        description = stringResource(R.string.current_theme, themeName(currentTheme)),
                        icon = AppIcons.Theme,
                    ) {
                        ConnectedChoices(
                            options = AppTheme.entries,
                            selected = currentTheme,
                            onSelect = { it?.let(onThemeChange) },
                            // Four choices share the row, so "Follow system" is shown as "System" here.
                            label = { if (it == AppTheme.SYSTEM) stringResource(R.string.theme_system_short) else themeName(it) },
                        )
                    }
                    // Wallpaper colours (Material You), Android 12+ only
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        SettingSwitchRow(
                            title = stringResource(R.string.dynamic_color_title),
                            description = stringResource(R.string.dynamic_color_description),
                            checked = dynamicColor,
                            onCheckedChange = onDynamicColorChange,
                            icon = AppIcons.Theme,
                        )
                    }
                }
            }

            // Week and suggestions
            item {
                val locale = LocalConfiguration.current.locales[0]
                val automaticDay = java.time.temporal.WeekFields.of(locale).firstDayOfWeek
                fun dayName(day: java.time.DayOfWeek) = day.getDisplayName(java.time.format.TextStyle.FULL, locale)
                fun shortDay(day: java.time.DayOfWeek) = day.getDisplayName(java.time.format.TextStyle.SHORT, locale)
                val automaticLabel = stringResource(R.string.first_day_of_week_automatic, dayName(automaticDay))
                val current = firstDayOfWeek?.let(::dayName) ?: automaticLabel
                // null stands for "Automatic", which follows the language's own week.
                val options = listOf<java.time.DayOfWeek?>(null, java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY)
                val weeklyGoal by viewModel.weeklyGoal.collectAsState()
                val showSuggestions by viewModel.showSuggestions.collectAsState()
                @Composable
                fun goalLabel(days: Int?) =
                    if (days == null) stringResource(R.string.weekly_goal_off) else stringResource(R.string.weekly_goal_days, days)

                SettingsGroup(stringResource(R.string.section_week)) {
                    // The first day is used by Today's week strip, the Progress calendar and To Do.
                    SettingChoiceRow(title = stringResource(R.string.first_day_of_week), description = current, icon = AppIcons.Today) {
                        ConnectedChoices(
                            options = options,
                            selected = firstDayOfWeek,
                            onSelect = { day -> onFirstDayOfWeekChange(day) },
                            label = { day -> day?.let(::shortDay) ?: stringResource(R.string.first_day_of_week_auto) },
                            description = { day -> day?.let(::dayName) ?: automaticLabel },
                        )
                    }
                    // Training days a week, which Today counts against.
                    SettingChoiceRow(title = stringResource(R.string.weekly_goal), description = goalLabel(weeklyGoal), icon = AppIcons.Done) {
                        ConnectedChoices(
                            options = listOf<Int?>(null) + ProgressionPreferences.WEEKLY_GOAL_RANGE,
                            selected = weeklyGoal,
                            onSelect = { days -> viewModel.setWeeklyGoal(days) },
                            label = { days -> days?.toString() ?: stringResource(R.string.weekly_goal_off) },
                            description = { days -> goalLabel(days) },
                        )
                    }
                    // On by default, and hidden entirely when off.
                    SettingSwitchRow(
                        title = stringResource(R.string.settings_suggestions),
                        description = stringResource(R.string.settings_suggestions_description),
                        checked = showSuggestions,
                        onCheckedChange = viewModel::setShowSuggestions,
                        icon = AppIcons.Chart,
                    )
                }
            }

            // Workouts
            item {
                val workoutPrefs = remember { io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences(context) }
                var prefillEnabled by remember { mutableStateOf(workoutPrefs.isPrefillPreviousRecordEnabled()) }
                var warmUpMarking by remember { mutableStateOf(workoutPrefs.isWarmUpMarkingEnabled()) }
                var effortRating by remember { mutableStateOf(workoutPrefs.isEffortRatingEnabled()) }
                var startCountdown by remember { mutableIntStateOf(workoutPrefs.getStartCountdown()) }
                var setInterval by remember { mutableIntStateOf(workoutPrefs.getSetInterval()) }
                var startCountdownEnabled by remember { mutableStateOf(workoutPrefs.isStartCountdownEnabled()) }
                var setIntervalEnabled by remember { mutableStateOf(workoutPrefs.isSetIntervalEnabled()) }
                var flashNotificationEnabled by remember { mutableStateOf(workoutPrefs.isFlashNotificationEnabled()) }
                var keepScreenOnEnabled by remember { mutableStateOf(workoutPrefs.isKeepScreenOnEnabled()) }
                var setDoneVibrationEnabled by remember { mutableStateOf(workoutPrefs.isSetDoneVibrationEnabled()) }
                var countdownVibrationEnabled by remember { mutableStateOf(workoutPrefs.isCountdownVibrationEnabled()) }
                var holdSoundEnabled by remember { mutableStateOf(workoutPrefs.isIsometricIntervalSoundEnabled()) }

                SettingsGroup(stringResource(R.string.section_workout_settings)) {
                    SettingSwitchRow(
                        title = stringResource(R.string.settings_prefill_previous),
                        description = stringResource(R.string.settings_prefill_previous_description),
                        checked = prefillEnabled,
                        onCheckedChange = { enabled ->
                            prefillEnabled = enabled
                            workoutPrefs.setPrefillPreviousRecordEnabled(enabled)
                        },
                        icon = AppIcons.RecordManually,
                    )
                    // Warm-up sets and effort (ADR 0012): off by default; turning one off hides its control only.
                    SettingSwitchRow(
                        title = stringResource(R.string.settings_warm_up_sets),
                        description = stringResource(R.string.settings_warm_up_sets_description),
                        checked = warmUpMarking,
                        onCheckedChange = { enabled ->
                            warmUpMarking = enabled
                            workoutPrefs.setWarmUpMarkingEnabled(enabled)
                        },
                        icon = AppIcons.Exercise,
                    )
                    SettingSwitchRow(
                        title = stringResource(R.string.settings_effort),
                        description = stringResource(R.string.settings_effort_description),
                        checked = effortRating,
                        onCheckedChange = { enabled ->
                            effortRating = enabled
                            workoutPrefs.setEffortRatingEnabled(enabled)
                        },
                        icon = AppIcons.Chart,
                    )
                    SettingValueRow(
                        title = stringResource(R.string.start_countdown_setting),
                        checked = startCountdownEnabled,
                        onCheckedChange = { enabled ->
                            startCountdownEnabled = enabled
                            workoutPrefs.setStartCountdownEnabled(enabled)
                        },
                        value = startCountdown,
                        onValueChange = { seconds ->
                            startCountdown = seconds
                            workoutPrefs.setStartCountdown(seconds)
                        },
                        unit = stringResource(R.string.unit_seconds_short),
                        icon = AppIcons.Timer,
                        max = 60,
                    )
                    SettingValueRow(
                        title = stringResource(R.string.set_interval_setting),
                        description = stringResource(R.string.set_interval_note),
                        checked = setIntervalEnabled,
                        onCheckedChange = { enabled ->
                            setIntervalEnabled = enabled
                            workoutPrefs.setSetIntervalEnabled(enabled)
                        },
                        value = setInterval,
                        onValueChange = { seconds ->
                            setInterval = seconds
                            workoutPrefs.setSetInterval(seconds)
                        },
                        unit = stringResource(R.string.unit_seconds_short),
                        icon = AppIcons.Rest,
                        step = 5,
                        max = io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences.MAX_SET_INTERVAL,
                    )
                    SettingSwitchRow(
                        title = stringResource(R.string.flash_notification_setting),
                        description = stringResource(R.string.flash_notification_description),
                        checked = flashNotificationEnabled,
                        onCheckedChange = { enabled ->
                            flashNotificationEnabled = enabled
                            workoutPrefs.setFlashNotificationEnabled(enabled)
                        },
                        icon = AppIcons.Camera,
                    )
                    SettingSwitchRow(
                        title = stringResource(R.string.keep_screen_on_setting),
                        description = stringResource(R.string.keep_screen_on_description),
                        checked = keepScreenOnEnabled,
                        onCheckedChange = { enabled ->
                            keepScreenOnEnabled = enabled
                            workoutPrefs.setKeepScreenOnEnabled(enabled)
                        },
                        icon = AppIcons.Brightness,
                    )
                    SettingSwitchRow(
                        title = stringResource(R.string.set_done_vibration_setting),
                        description = stringResource(R.string.set_done_vibration_description),
                        checked = setDoneVibrationEnabled,
                        onCheckedChange = { enabled ->
                            setDoneVibrationEnabled = enabled
                            workoutPrefs.setSetDoneVibrationEnabled(enabled)
                        },
                        icon = AppIcons.Vibration,
                    )
                    SettingSwitchRow(
                        title = stringResource(R.string.countdown_vibration_setting),
                        description = stringResource(R.string.countdown_vibration_description),
                        checked = countdownVibrationEnabled,
                        onCheckedChange = { enabled ->
                            countdownVibrationEnabled = enabled
                            workoutPrefs.setCountdownVibrationEnabled(enabled)
                        },
                        icon = AppIcons.Vibration,
                    )
                    SettingSwitchRow(
                        title = stringResource(R.string.isometric_interval_sound_label),
                        description = stringResource(R.string.isometric_interval_sound_description),
                        checked = holdSoundEnabled,
                        onCheckedChange = { enabled ->
                            holdSoundEnabled = enabled
                            workoutPrefs.setIsometricIntervalSoundEnabled(enabled)
                        },
                        icon = AppIcons.Interval,
                    )
                }
            }

            // Your data
            item {
                SettingsGroup(stringResource(R.string.data_management)) {
                    SettingLinkRow(
                        title = stringResource(R.string.section_full_backup),
                        description = stringResource(R.string.section_full_backup_description),
                        icon = AppIcons.Save,
                        onClick = onNavigateToBackup,
                    )
                    SettingLinkRow(
                        title = stringResource(R.string.section_partial_data_management),
                        description = stringResource(R.string.section_partial_data_management_description),
                        icon = AppIcons.List,
                        onClick = onNavigateToCsvDataManagement,
                    )
                    SettingLinkRow(
                        title = stringResource(R.string.share_section_title),
                        description = stringResource(R.string.share_section_description),
                        icon = AppIcons.Share,
                        onClick = onNavigateToShareHub,
                    )
                }
            }

            // About
            item {
                SettingsGroup(stringResource(R.string.section_app_info)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.m),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.app_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SettingRow(title = stringResource(R.string.app_version), description = BuildConfig.VERSION_NAME, icon = AppIcons.Info)
                    SettingLinkRow(
                        title = stringResource(R.string.app_source_code),
                        icon = AppIcons.SourceCode,
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, REPOSITORY_URL.toUri())) },
                    )
                    SettingLinkRow(title = stringResource(R.string.open_source_licenses), icon = AppIcons.Document, onClick = onNavigateToLicenses)
                    // The welcome guide from the first launch, again.
                    SettingLinkRow(title = stringResource(R.string.settings_welcome_guide), icon = AppIcons.Info, onClick = onOpenWelcomeGuide)
                    SettingLinkRow(
                        title = stringResource(R.string.report_issue_github),
                        icon = AppIcons.RecordManually,
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "$REPOSITORY_URL/issues".toUri())) },
                    )
                    // Kalimory's maintainer, then the original author and project (GPL-3.0 attribution).
                    SettingRow(title = "WildeBeast2521", description = stringResource(R.string.app_author), icon = AppIcons.Exercise)
                    SettingRow(title = "Gonbei774", icon = AppIcons.Exercise)
                    SettingLinkRow(
                        title = stringResource(R.string.app_original_project),
                        icon = AppIcons.SourceCode,
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, UPSTREAM_URL.toUri())) },
                    )
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
private const val REPOSITORY_URL = "https://github.com/WildeBeast2521/kalimory"

/** The upstream project this app is based on. */
private const val UPSTREAM_URL = "https://codeberg.org/Gonbei774/CalisthenicsMemory"
