package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.AppLanguage
import io.github.gonbei774.calisthenicsmemory.data.DatabaseQuarantine
import io.github.gonbei774.calisthenicsmemory.data.DatabaseStartupCheck
import io.github.gonbei774.calisthenicsmemory.data.DatabaseStartupState
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.data.LanguagePreferences
import io.github.gonbei774.calisthenicsmemory.data.ThemePreferences
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.gonbei774.calisthenicsmemory.ui.UiMessage
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryNavigationBar
import io.github.gonbei774.calisthenicsmemory.ui.screens.library.LibraryScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.ResumableWorkout
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.TodayScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.train.TrainScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.RecordScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.CreateScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.SettingsScreenNew
import io.github.gonbei774.calisthenicsmemory.ui.screens.LicensesScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.WorkoutScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.view.ViewScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.ToDoScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProgramListScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProgramEditScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProgramExecutionScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.IntervalListScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.IntervalEditScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.IntervalExecutionScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.CommunityShareExportScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.BackupScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.DatabaseUnavailableScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.gonbei774.calisthenicsmemory.ui.screens.CsvDataManagementScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.ShareHubScreen
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.theme.LocalFirstDayOfWeekSetting
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

class MainActivity : ComponentActivity() {
    private val systemDarkMode = mutableStateOf(false)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(updateBaseContextLocale(newBase))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        systemDarkMode.value =
            (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        systemDarkMode.value =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val themePrefs = ThemePreferences(this)

        setContent {
            val isSystemDark by systemDarkMode
            val savedTheme = remember { themePrefs.getTheme() }
            var currentTheme by remember { mutableStateOf(savedTheme) }
            var dynamicColor by remember { mutableStateOf(themePrefs.isDynamicColor()) }
            var firstDayOfWeek by remember { mutableStateOf(themePrefs.getFirstDayOfWeek()) }

            val darkTheme = when (currentTheme) {
                AppTheme.SYSTEM -> isSystemDark
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            // Open the database before any screen uses it; failures show a recovery screen.
            var startupState by remember { mutableStateOf<DatabaseStartupState?>(null) }
            LaunchedEffect(Unit) {
                startupState = withContext(Dispatchers.IO) { DatabaseStartupCheck.run(applicationContext) }
            }

            CalisthenicsMemoryTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
              CompositionLocalProvider(LocalFirstDayOfWeekSetting provides firstDayOfWeek) {
                when (val state = startupState) {
                    null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    DatabaseStartupState.Ready -> CalisthenicsMemoryApp(
                        currentTheme = currentTheme,
                        onThemeChange = { newTheme ->
                            themePrefs.setTheme(newTheme)
                            currentTheme = newTheme
                        },
                        dynamicColor = dynamicColor,
                        onDynamicColorChange = { enabled ->
                            themePrefs.setDynamicColor(enabled)
                            dynamicColor = enabled
                        },
                        firstDayOfWeek = firstDayOfWeek,
                        onFirstDayOfWeekChange = { day ->
                            themePrefs.setFirstDayOfWeek(day)
                            firstDayOfWeek = day
                        }
                    )
                    else -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        DatabaseUnavailableScreen(
                            state = state,
                            onContinue = {
                                DatabaseQuarantine.acknowledge(getDatabasePath(AppDatabase.DATABASE_NAME))
                                startupState = DatabaseStartupState.Ready
                            }
                        )
                    }
                }
            }
          }
            }
    }

    /**
     * Context の言語設定を更新する（全Androidバージョン対応）
     */
    private fun updateBaseContextLocale(context: Context): Context {
        val languagePrefs = LanguagePreferences(context)
        val selectedLanguage = languagePrefs.getLanguage()

        android.util.Log.d("MainActivity", "Selected language: ${selectedLanguage.code}")

        // システム設定に従う場合は何もしない
        if (selectedLanguage == AppLanguage.SYSTEM) {
            android.util.Log.d("MainActivity", "Using system language")
            return context
        }

        val locale = when (selectedLanguage) {
            AppLanguage.JAPANESE -> Locale("ja")
            AppLanguage.ENGLISH -> Locale("en")
            AppLanguage.SPANISH -> Locale("es")
            AppLanguage.GERMAN -> Locale("de")
            AppLanguage.CHINESE -> Locale("zh", "CN")
            AppLanguage.FRENCH -> Locale("fr")
            AppLanguage.ITALIAN -> Locale("it")
            AppLanguage.UKRAINIAN -> Locale("uk")
            AppLanguage.SYSTEM -> return context
        }

        android.util.Log.d("MainActivity", "Setting locale to: ${locale.language}")
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}

/**
 * UiMessageを現在の言語の文字列に変換
 * UI層で文字列リソースを取得することで、言語変更に即座に対応
 */
@Composable
fun UiMessage.toMessageString(): String {
    return when (this) {
        is UiMessage.ExerciseAdded -> stringResource(R.string.exercise_added)
        is UiMessage.ExerciseUpdated -> stringResource(R.string.exercise_updated)
        is UiMessage.ExerciseDeleted -> stringResource(R.string.exercise_deleted)
        is UiMessage.ExerciseAlreadyExists -> stringResource(R.string.exercise_already_exists)
        is UiMessage.AlreadyRegistered -> {
            val typeLabel = stringResource(if (type == "Dynamic") R.string.dynamic_label else R.string.isometric_label)
            stringResource(R.string.already_registered_format, name, typeLabel)
        }
        is UiMessage.AlreadyInUse -> {
            val typeLabel = stringResource(if (type == "Dynamic") R.string.dynamic_label else R.string.isometric_label)
            stringResource(R.string.already_in_use_format, name, typeLabel)
        }
        is UiMessage.SetsRecorded -> stringResource(R.string.sets_recorded, count)
        is UiMessage.ProgramSetsRecorded -> stringResource(R.string.sets_recorded, totalCount)
        is UiMessage.RecordUpdated -> stringResource(R.string.record_updated)
        is UiMessage.RecordDeleted -> stringResource(R.string.record_deleted)
        is UiMessage.GroupCreated -> stringResource(R.string.group_created)
        is UiMessage.GroupRenamed -> stringResource(R.string.group_renamed)
        is UiMessage.GroupDeleted -> stringResource(R.string.group_deleted)
        is UiMessage.GroupAlreadyExists -> stringResource(R.string.group_already_exists)
        is UiMessage.ExportComplete -> stringResource(R.string.export_complete, groupCount, exerciseCount, recordCount)
        is UiMessage.ImportComplete -> stringResource(R.string.import_complete, groupCount, exerciseCount, recordCount)
        is UiMessage.ExportError -> stringResource(R.string.export_error, errorMessage)
        is UiMessage.ImportError -> stringResource(R.string.import_error, errorMessage)
        is UiMessage.CsvExportSuccess -> stringResource(R.string.csv_export_success, type, count)
        is UiMessage.CsvTemplateExported -> stringResource(R.string.csv_template_exported, exerciseCount)
        is UiMessage.CsvEmpty -> stringResource(R.string.csv_empty)
        is UiMessage.CsvImportSuccess -> stringResource(R.string.csv_import_success, successCount)
        is UiMessage.CsvImportPartial -> stringResource(R.string.csv_import_partial, successCount, errorCount)
        is UiMessage.BackupSaved -> stringResource(R.string.backup_saved_successfully)
        is UiMessage.BackupFailed -> stringResource(R.string.backup_failed)
        is UiMessage.CopiedToClipboard -> stringResource(R.string.copied_to_clipboard)
        is UiMessage.ProgramDuplicated -> stringResource(R.string.program_duplicated)
        is UiMessage.CommunityShareExportComplete ->
            "Export complete: $exerciseCount exercises, $programCount programs, $intervalProgramCount interval programs"
        is UiMessage.CommunityShareImportComplete -> {
            val r = report
            "Import complete: ${r.exercisesAdded} added, ${r.exercisesSkipped} skipped, ${r.programsAdded} programs, ${r.intervalProgramsAdded} intervals"
        }
        is UiMessage.CommunityShareImportError -> "Import error: $errorMessage"
        is UiMessage.FileTooLarge -> "File too large: ${sizeMb}MB (limit: ${limitMb}MB)"
        is UiMessage.WrongFileType -> "Wrong file type: $detected (expected: $expected)"
        is UiMessage.ErrorOccurred -> stringResource(R.string.error_occurred)
    }
}

@Composable
fun CalisthenicsMemoryApp(
    currentTheme: AppTheme = AppTheme.SYSTEM,
    onThemeChange: (AppTheme) -> Unit = {},
    dynamicColor: Boolean = false,
    onDynamicColorChange: (Boolean) -> Unit = {},
    firstDayOfWeek: java.time.DayOfWeek? = null,
    onFirstDayOfWeekChange: (java.time.DayOfWeek?) -> Unit = {}
) {
    val viewModel: TrainingViewModel = viewModel()
    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Home) }
    // Screen.Home shows this destination, so "back to Home" returns to the tab the user came from.
    var primaryDestination by rememberSaveable { mutableStateOf(PrimaryDestination.TODAY) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar message handling
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    // UiMessageを文字列に変換（Composable関数内で実行）
    val messageString = snackbarMessage?.toMessageString()

    LaunchedEffect(snackbarMessage) {
        messageString?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbarMessage()
        }
    }

    val showPrimaryNavigation = currentScreen is Screen.Home
    Scaffold(
        bottomBar = {
            if (showPrimaryNavigation) {
                PrimaryNavigationBar(
                    selected = primaryDestination,
                    onSelect = { primaryDestination = it }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                // The navigation bar already clears the system bar when shown.
                modifier = if (showPrimaryNavigation) Modifier else
                    Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                is Screen.Home -> {
                    BackHandler(enabled = primaryDestination != PrimaryDestination.TODAY) {
                        primaryDestination = PrimaryDestination.TODAY
                    }
                    when (primaryDestination) {
                        PrimaryDestination.TODAY -> TodayScreen(
                            viewModel = viewModel,
                            onResume = { workout ->
                                currentScreen = when (workout) {
                                    // The single-workout screen offers its checkpoint whenever it opens.
                                    is ResumableWorkout.Single -> Screen.Workout(fromToday = true)
                                    is ResumableWorkout.Program -> Screen.ProgramExecution(
                                        workout.programId,
                                        resumeSavedState = workout.savedByUser,
                                        fromToday = true
                                    )
                                    is ResumableWorkout.Interval -> Screen.IntervalExecution(workout.programId, fromToday = true)
                                }
                            },
                            onOpenTask = { task ->
                                currentScreen = when (task.type) {
                                    TodoTask.TYPE_EXERCISE ->
                                        Screen.Workout(exerciseId = task.referenceId, fromToDo = true, fromToday = true)
                                    TodoTask.TYPE_PROGRAM ->
                                        Screen.ProgramExecution(task.referenceId, fromToDo = true, fromToday = true)
                                    TodoTask.TYPE_INTERVAL ->
                                        Screen.IntervalExecution(task.referenceId, fromToDo = true, fromToday = true)
                                    // A group needs an exercise chosen first, which the To Do screen offers.
                                    else -> Screen.ToDo
                                }
                            },
                            onOpenToDo = { currentScreen = Screen.ToDo },
                            onOpenHistory = { primaryDestination = PrimaryDestination.PROGRESS },
                            onOpenSettings = { currentScreen = Screen.Settings },
                            onOpenTrain = { primaryDestination = PrimaryDestination.TRAIN }
                        )
                        PrimaryDestination.TRAIN -> TrainScreen(
                            viewModel = viewModel,
                            // fromToday returns to the primary destination (here Train) on back.
                            onStartProgram = { id -> currentScreen = Screen.ProgramExecution(id, fromToday = true) },
                            onEditProgram = { id -> currentScreen = Screen.ProgramEdit(id) },
                            onStartInterval = { id -> currentScreen = Screen.IntervalExecution(id, fromToday = true) },
                            onStartWorkout = { currentScreen = Screen.Workout() },
                            onRecordManually = { currentScreen = Screen.Record() },
                            onOpenPrograms = { currentScreen = Screen.ProgramList },
                            onOpenIntervals = { currentScreen = Screen.IntervalList }
                        )
                        PrimaryDestination.PROGRESS -> ViewScreen(viewModel = viewModel)
                        PrimaryDestination.LIBRARY -> LibraryScreen(
                            viewModel = viewModel,
                            onOpenExercises = { currentScreen = Screen.Create },
                            onOpenPrograms = { currentScreen = Screen.ProgramList },
                            onOpenIntervals = { currentScreen = Screen.IntervalList },
                            onOpenSettings = { currentScreen = Screen.Settings }
                        )
                    }
                }
                is Screen.ToDo -> {
                    BackHandler { currentScreen = Screen.Home }
                    ToDoScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToRecord = { exerciseId ->
                            currentScreen = Screen.Record(exerciseId = exerciseId, fromToDo = true)
                        },
                        onNavigateToWorkout = { exerciseId ->
                            currentScreen = Screen.Workout(exerciseId = exerciseId, fromToDo = true)
                        },
                        onNavigateToProgramPreview = { programId ->
                            currentScreen = Screen.ProgramExecution(programId = programId, fromToDo = true)
                        },
                        onNavigateToIntervalPreview = { programId ->
                            currentScreen = Screen.IntervalExecution(programId = programId, fromToDo = true)
                        }
                    )
                }
                is Screen.Create -> {
                    BackHandler {
                        viewModel.saveGroupOrder()
                        currentScreen = Screen.Home
                    }
                    CreateScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            viewModel.saveGroupOrder()
                            currentScreen = Screen.Home
                        }
                    )
                }
                is Screen.Settings -> {
                    BackHandler { currentScreen = Screen.Home }
                    SettingsScreenNew(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToLicenses = { currentScreen = Screen.Licenses },
                        onNavigateToBackup = { currentScreen = Screen.Backup },
                        onNavigateToCsvDataManagement = { currentScreen = Screen.CsvDataManagement },
                        onNavigateToShareHub = { currentScreen = Screen.ShareHub },
                        currentTheme = currentTheme,
                        onThemeChange = onThemeChange,
                        dynamicColor = dynamicColor,
                        onDynamicColorChange = onDynamicColorChange,
                        firstDayOfWeek = firstDayOfWeek,
                        onFirstDayOfWeekChange = onFirstDayOfWeekChange
                    )
                }
                is Screen.Licenses -> {
                    BackHandler { currentScreen = Screen.Settings }
                    LicensesScreen(
                        onNavigateBack = { currentScreen = Screen.Settings }
                    )
                }
                is Screen.Record -> {
                    val recordScreen = currentScreen as Screen.Record
                    val backDestination = if (recordScreen.fromToDo) Screen.ToDo else Screen.Home
                    BackHandler { currentScreen = backDestination }
                    RecordScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = backDestination },
                        initialExerciseId = recordScreen.exerciseId,
                        fromToDo = recordScreen.fromToDo
                    )
                }
                is Screen.Workout -> {
                    val workoutScreen = currentScreen as Screen.Workout
                    val backDestination = if (workoutScreen.fromToDo && !workoutScreen.fromToday) Screen.ToDo else Screen.Home
                    BackHandler { currentScreen = backDestination }
                    WorkoutScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = backDestination },
                        initialExerciseId = workoutScreen.exerciseId,
                        fromToDo = workoutScreen.fromToDo
                    )
                }
                is Screen.ProgramList -> {
                    BackHandler { currentScreen = Screen.Home }
                    ProgramListScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToEdit = { programId -> currentScreen = Screen.ProgramEdit(programId) },
                        onNavigateToExecute = { programId ->
                            currentScreen = Screen.ProgramExecution(programId)
                        },
                        onNavigateToResume = { programId ->
                            currentScreen = Screen.ProgramExecution(programId, resumeSavedState = true)
                        }
                    )
                }
                is Screen.ProgramEdit -> {
                    val editScreen = currentScreen as Screen.ProgramEdit
                    BackHandler { currentScreen = Screen.ProgramList }
                    ProgramEditScreen(
                        viewModel = viewModel,
                        programId = editScreen.programId,
                        onNavigateBack = { currentScreen = Screen.ProgramList },
                        onSaved = { currentScreen = Screen.ProgramList }
                    )
                }
                is Screen.ProgramExecution -> {
                    val execScreen = currentScreen as Screen.ProgramExecution
                    val backDestination = when {
                        execScreen.fromToday -> Screen.Home
                        execScreen.fromToDo -> Screen.ToDo
                        else -> Screen.ProgramList
                    }
                    BackHandler { currentScreen = backDestination }
                    ProgramExecutionScreen(
                        viewModel = viewModel,
                        programId = execScreen.programId,
                        resumeSavedState = execScreen.resumeSavedState,
                        onNavigateBack = { currentScreen = backDestination },
                        onComplete = {
                            if (execScreen.fromToDo) {
                                viewModel.completeTodoTaskByReference(TodoTask.TYPE_PROGRAM, execScreen.programId)
                            }
                            currentScreen = backDestination
                        }
                    )
                }
                is Screen.IntervalList -> {
                    BackHandler { currentScreen = Screen.Home }
                    IntervalListScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToEdit = { programId -> currentScreen = Screen.IntervalEdit(programId) },
                        onNavigateToExecute = { programId ->
                            currentScreen = Screen.IntervalExecution(programId)
                        }
                    )
                }
                is Screen.IntervalEdit -> {
                    val editScreen = currentScreen as Screen.IntervalEdit
                    BackHandler { currentScreen = Screen.IntervalList }
                    IntervalEditScreen(
                        viewModel = viewModel,
                        programId = editScreen.programId,
                        onNavigateBack = { currentScreen = Screen.IntervalList },
                        onSaved = { currentScreen = Screen.IntervalList }
                    )
                }
                is Screen.IntervalExecution -> {
                    val execScreen = currentScreen as Screen.IntervalExecution
                    val backDestination = when {
                        execScreen.fromToday -> Screen.Home
                        execScreen.fromToDo -> Screen.ToDo
                        else -> Screen.IntervalList
                    }
                    BackHandler { currentScreen = backDestination }
                    IntervalExecutionScreen(
                        viewModel = viewModel,
                        programId = execScreen.programId,
                        onNavigateBack = { currentScreen = backDestination },
                        onComplete = {
                            if (execScreen.fromToDo) {
                                viewModel.completeTodoTaskByReference(TodoTask.TYPE_INTERVAL, execScreen.programId)
                            }
                            currentScreen = backDestination
                        }
                    )
                }
                is Screen.CommunityShareExport -> {
                    BackHandler { currentScreen = Screen.ShareHub }
                    CommunityShareExportScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.ShareHub }
                    )
                }
                is Screen.Backup -> {
                    BackHandler { currentScreen = Screen.Settings }
                    BackupScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Settings }
                    )
                }
                is Screen.CsvDataManagement -> {
                    BackHandler { currentScreen = Screen.Settings }
                    CsvDataManagementScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Settings }
                    )
                }
                is Screen.ShareHub -> {
                    BackHandler { currentScreen = Screen.Settings }
                    ShareHubScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Settings },
                        onNavigateToCommunityShareExport = { currentScreen = Screen.CommunityShareExport }
                    )
                }
            }
        }
    }
}

sealed class Screen {
    object Home : Screen()
    object ToDo : Screen()
    object Create : Screen()
    object Settings : Screen()
    object Licenses : Screen()
    data class Record(val exerciseId: Long? = null, val fromToDo: Boolean = false) : Screen()
    // fromToday: opened from the Today destination, so back returns there.
    data class Workout(val exerciseId: Long? = null, val fromToDo: Boolean = false, val fromToday: Boolean = false) : Screen()
    object ProgramList : Screen()
    data class ProgramEdit(val programId: Long?) : Screen()
    data class ProgramExecution(
        val programId: Long,
        val resumeSavedState: Boolean = false,
        val fromToDo: Boolean = false,
        val fromToday: Boolean = false
    ) : Screen()
    object IntervalList : Screen()
    data class IntervalEdit(val programId: Long?) : Screen()
    data class IntervalExecution(val programId: Long, val fromToDo: Boolean = false, val fromToday: Boolean = false) : Screen()
    object CommunityShareExport : Screen()
    object Backup : Screen()
    object CsvDataManagement : Screen()
    object ShareHub : Screen()
}

private val ScreenSaver = mapSaver(
    save = { screen: Screen ->
        buildMap {
            when (screen) {
                Screen.Home -> put("type", "Home")
                Screen.ToDo -> put("type", "ToDo")
                Screen.Create -> put("type", "Create")
                Screen.Settings -> put("type", "Settings")
                Screen.Licenses -> put("type", "Licenses")
                Screen.ProgramList -> put("type", "ProgramList")
                Screen.IntervalList -> put("type", "IntervalList")
                is Screen.Record -> {
                    put("type", "Record")
                    put("exerciseId", screen.exerciseId ?: -1L)
                    put("fromToDo", screen.fromToDo)
                }
                is Screen.Workout -> {
                    put("type", "Workout")
                    put("exerciseId", screen.exerciseId ?: -1L)
                    put("fromToDo", screen.fromToDo)
                    put("fromToday", screen.fromToday)
                }
                is Screen.ProgramEdit -> {
                    put("type", "ProgramEdit")
                    put("programId", screen.programId ?: -1L)
                }
                is Screen.ProgramExecution -> {
                    put("type", "ProgramExecution")
                    put("programId", screen.programId)
                    put("resumeSavedState", screen.resumeSavedState)
                    put("fromToDo", screen.fromToDo)
                    put("fromToday", screen.fromToday)
                }
                is Screen.IntervalEdit -> {
                    put("type", "IntervalEdit")
                    put("programId", screen.programId ?: -1L)
                }
                is Screen.IntervalExecution -> {
                    put("type", "IntervalExecution")
                    put("programId", screen.programId)
                    put("fromToDo", screen.fromToDo)
                    put("fromToday", screen.fromToday)
                }
                Screen.CommunityShareExport -> put("type", "CommunityShareExport")
                Screen.Backup -> put("type", "Backup")
                Screen.CsvDataManagement -> put("type", "CsvDataManagement")
                Screen.ShareHub -> put("type", "ShareHub")
            }
        }
    },
    restore = { map ->
        when (map["type"] as String) {
            "ToDo" -> Screen.ToDo
            "Create" -> Screen.Create
            "Settings" -> Screen.Settings
            "Licenses" -> Screen.Licenses
            "ProgramList" -> Screen.ProgramList
            "IntervalList" -> Screen.IntervalList
            "Record" -> Screen.Record(
                exerciseId = (map["exerciseId"] as Long).takeIf { it != -1L },
                fromToDo = map["fromToDo"] as Boolean
            )
            "Workout" -> Screen.Workout(
                exerciseId = (map["exerciseId"] as Long).takeIf { it != -1L },
                fromToDo = map["fromToDo"] as Boolean,
                fromToday = map["fromToday"] as? Boolean ?: false
            )
            "ProgramEdit" -> Screen.ProgramEdit(
                programId = (map["programId"] as Long).takeIf { it != -1L }
            )
            "ProgramExecution" -> Screen.ProgramExecution(
                programId = map["programId"] as Long,
                resumeSavedState = map["resumeSavedState"] as Boolean,
                fromToDo = map["fromToDo"] as Boolean,
                fromToday = map["fromToday"] as? Boolean ?: false
            )
            "IntervalEdit" -> Screen.IntervalEdit(
                programId = (map["programId"] as Long).takeIf { it != -1L }
            )
            "IntervalExecution" -> Screen.IntervalExecution(
                programId = map["programId"] as Long,
                fromToDo = map["fromToDo"] as Boolean,
                fromToday = map["fromToday"] as? Boolean ?: false
            )
            "CommunityShareExport" -> Screen.CommunityShareExport
            "Backup" -> Screen.Backup
            "CsvDataManagement" -> Screen.CsvDataManagement
            "ShareHub" -> Screen.ShareHub
            else -> Screen.Home
        }
    }
)