package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.util.AppClock
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.ProgressionPreferences
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueAddResult
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueLibrary
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueStep
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueStepText
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalRecord
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.data.restoreMissingGroups
import io.github.gonbei774.calisthenicsmemory.ui.UiMessage
import io.github.gonbei774.calisthenicsmemory.data.v2.CompatibilityHistory
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.IntervalHistory
import io.github.gonbei774.calisthenicsmemory.data.v2.IntervalHistoryItem
import io.github.gonbei774.calisthenicsmemory.data.v2.IntervalRun
import io.github.gonbei774.calisthenicsmemory.data.v2.IntervalWorkoutWriter
import io.github.gonbei774.calisthenicsmemory.data.v2.ManualWorkout
import io.github.gonbei774.calisthenicsmemory.data.v2.ManualWorkoutWriter
import io.github.gonbei774.calisthenicsmemory.data.v2.ProgramRun
import io.github.gonbei774.calisthenicsmemory.data.v2.ProgramWorkoutWriter
import io.github.gonbei774.calisthenicsmemory.data.v2.SingleWorkout
import io.github.gonbei774.calisthenicsmemory.data.v2.SingleWorkoutWriter
import io.github.gonbei774.calisthenicsmemory.data.v2.V2HistoryEditor
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutDraft
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutEditor
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutRef
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSaveResult
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSummary
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSummaryBuilder
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 種目削除の影響範囲
data class ExerciseDeleteImpact(
    val recordCount: Int,
    val programNames: List<String>,
    val intervalNames: List<String>
)

class TrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val exerciseDao = database.exerciseDao()
    private val recordDao = database.trainingRecordDao()
    private val groupDao = database.exerciseGroupDao()
    private val todoTaskDao = database.todoTaskDao()
    private val programDao = database.programDao()
    private val programExerciseDao = database.programExerciseDao()
    private val programLoopDao = database.programLoopDao()
    private val intervalProgramDao = database.intervalProgramDao()
    private val intervalProgramExerciseDao = database.intervalProgramExerciseDao()
    private val intervalRecordDao = database.intervalRecordDao()
    private val backupService = BackupService(database.backupDao())
    private val progressionPrefs = ProgressionPreferences(application)

    // Training days a week the user aims for; Today counts the week against it.
    private val _weeklyGoal = MutableStateFlow(progressionPrefs.weeklyGoal())
    val weeklyGoal: StateFlow<Int?> = _weeklyGoal.asStateFlow()

    fun setWeeklyGoal(days: Int?) {
        progressionPrefs.setWeeklyGoal(days)
        _weeklyGoal.value = progressionPrefs.weeklyGoal()
    }

    // Today's suggestions (ADR 0005, decision 5): shown unless turned off; dismissed chains wait for tomorrow.
    private val _showSuggestions = MutableStateFlow(progressionPrefs.showSuggestions())
    val showSuggestions: StateFlow<Boolean> = _showSuggestions.asStateFlow()

    fun setShowSuggestions(show: Boolean) {
        progressionPrefs.setShowSuggestions(show)
        _showSuggestions.value = show
    }

    // Chains the user stopped following; Progressions and Today's suggestions leave them out.
    private val _unfollowedChains = MutableStateFlow(progressionPrefs.unfollowedChains())
    val unfollowedChains: StateFlow<Set<String>> = _unfollowedChains.asStateFlow()

    fun setChainFollowed(chainId: String, followed: Boolean) {
        progressionPrefs.setFollowed(chainId, followed)
        _unfollowedChains.value = progressionPrefs.unfollowedChains()
    }

    // Bumped on each dismissal so Today reads the dismissed chains for its own date again.
    private val _dismissals = MutableStateFlow(0)
    val dismissals: StateFlow<Int> = _dismissals.asStateFlow()

    fun dismissedChains(date: java.time.LocalDate): Set<String> = progressionPrefs.dismissedChains(date.toString())

    fun dismissSuggestion(date: java.time.LocalDate, chainId: String) {
        progressionPrefs.dismissChain(date.toString(), chainId)
        _dismissals.value++
    }

    companion object {
        // お気に入りグループの固定キー（UI側で翻訳される）
        const val FAVORITE_GROUP_KEY = "★FAVORITES"
    }

    // Exercises
    val exercises: StateFlow<List<Exercise>> = exerciseDao.getAllExercises()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Exercise Groups
    val groups: StateFlow<List<ExerciseGroup>> = groupDao.getAllGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // History screens read legacy records plus v2-only workouts, each set once (ADR 0003, stage 4).
    val history: StateFlow<List<HistorySet>> = combine(
        recordDao.getAllRecords(),
        database.workoutSessionDao().observeV2OnlyHistoryRows()
    ) { legacy, v2 -> CompatibilityHistory.merge(legacy, v2, ZoneId.systemDefault()) }
        // The whole history is merged and sorted on each change; keep that off the main thread.
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    /**
     * The day each exercise, program and interval program was last done: exercises from the
     * merged history, programs from the v2 sessions that name them.
     */
    val lastDone: StateFlow<LastDone> = combine(
        history,
        database.workoutSessionDao().observeTemplateLastRuns()
    ) { sets, runs ->
        val zone = ZoneId.systemDefault()
        fun day(millis: Long) = java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        LastDone(
            exercises = sets.groupBy { it.exerciseId }
                .mapValues { (_, s) -> s.maxOf { it.date } }
                .mapNotNull { (id, date) -> runCatching { LocalDate.parse(date) }.getOrNull()?.let { id to it } }
                .toMap(),
            programs = runs.filter { it.sourceType == WorkoutSourceType.PROGRAM_TEMPLATE }
                .associate { it.sourceTemplateId to day(it.lastStartedAtEpochMillis) },
            intervals = runs.filter { it.sourceType == WorkoutSourceType.INTERVAL_TEMPLATE }
                .associate { it.sourceTemplateId to day(it.lastStartedAtEpochMillis) },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, LastDone())

    // Snackbar message (UiMessage型で言語変更に追従)
    private val _snackbarMessage = MutableStateFlow<UiMessage?>(null)
    val snackbarMessage: StateFlow<UiMessage?> = _snackbarMessage.asStateFlow()

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun showWrongFileTypeMessage(detected: String, expected: String) {
        _snackbarMessage.value = UiMessage.WrongFileType(detected = detected, expected = expected)
    }

    fun showSnackbar(message: UiMessage) {
        _snackbarMessage.value = message
    }

    fun showBackupResult(success: Boolean) {
        _snackbarMessage.value = if (success) {
            UiMessage.BackupSaved
        } else {
            UiMessage.BackupFailed
        }
    }

    /** Adds a catalogue step to the library, or links the user's exercise of the same name (ADR 0007). */
    fun addFromCatalogue(step: CatalogueStep, name: String, description: String, chainName: String) {
        viewModelScope.launch {
            _snackbarMessage.value = try {
                when (CatalogueLibrary.add(database, step, CatalogueStepText(name, description, chainName))) {
                    is CatalogueAddResult.Added -> UiMessage.CatalogueAdded(name)
                    is CatalogueAddResult.Linked -> UiMessage.CatalogueLinked(name)
                    is CatalogueAddResult.NameTaken -> UiMessage.CatalogueNameTaken(name)
                    is CatalogueAddResult.AlreadyInLibrary -> return@launch
                }
            } catch (e: Exception) {
                UiMessage.ErrorOccurred
            }
        }
    }

    // Exercise operations
    fun addExercise(
        name: String,
        type: String,
        group: String? = null,
        sortOrder: Int = 0,
        laterality: String = "Bilateral",
        targetSets: Int? = null,
        targetValue: Int? = null,
        isFavorite: Boolean = false,
        restInterval: Int? = null,       // 種目固有の休憩時間（秒）
        repDuration: Int? = null,        // 種目固有の1レップ時間（秒）
        distanceTrackingEnabled: Boolean = false,  // 距離トラッキング有効
        weightTrackingEnabled: Boolean = false,    // 荷重トラッキング有効
        assistanceTrackingEnabled: Boolean = false, // アシストトラッキング有効
        description: String? = null                // 種目の説明文
    ) {
        viewModelScope.launch {
            try {
                val existingExercises = exercises.value
                val isDuplicate = existingExercises.any {
                    it.name.equals(name, ignoreCase = true) && it.type == type
                }

                if (isDuplicate) {
                    _snackbarMessage.value = UiMessage.AlreadyRegistered(name, type)
                    return@launch
                }

                val exercise = Exercise(
                    name = name,
                    type = type,
                    group = group,
                    sortOrder = sortOrder,
                    laterality = laterality,
                    targetSets = targetSets,
                    targetValue = targetValue,
                    isFavorite = isFavorite,
                    restInterval = restInterval,
                    repDuration = repDuration,
                    distanceTrackingEnabled = distanceTrackingEnabled,
                    weightTrackingEnabled = weightTrackingEnabled,
                    assistanceTrackingEnabled = assistanceTrackingEnabled,
                    description = description
                )
                exerciseDao.insertExercise(exercise)
                _snackbarMessage.value = UiMessage.ExerciseAdded
            } catch (e: SQLiteConstraintException) {
                _snackbarMessage.value = UiMessage.ExerciseAlreadyExists
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            try {
                val existingExercises = exercises.value
                val isDuplicate = existingExercises.any {
                    it.id != exercise.id &&
                            it.name.equals(exercise.name, ignoreCase = true) &&
                            it.type == exercise.type
                }

                if (isDuplicate) {
                    _snackbarMessage.value = UiMessage.AlreadyInUse(exercise.name, exercise.type)
                    return@launch
                }

                exerciseDao.updateExercise(exercise)
                _snackbarMessage.value = UiMessage.ExerciseUpdated
            } catch (e: SQLiteConstraintException) {
                _snackbarMessage.value = UiMessage.ExerciseAlreadyExists
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    // 種目削除の影響範囲（確認ダイアログ用）
    private val _deleteImpact = MutableStateFlow<ExerciseDeleteImpact?>(null)
    val deleteImpact: StateFlow<ExerciseDeleteImpact?> = _deleteImpact.asStateFlow()

    fun loadDeleteImpact(exercise: Exercise) {
        _deleteImpact.value = null
        viewModelScope.launch {
            // Legacy records are what the delete removes; v2 sets keep their snapshot and survive it.
            val recordCount = recordDao.countByExercise(exercise.id)
            val programNames = programExerciseDao.getProgramNamesUsingExercise(exercise.id)
            val intervalNames = intervalProgramExerciseDao.getProgramNamesUsingExercise(exercise.id)
            _deleteImpact.value = ExerciseDeleteImpact(recordCount, programNames, intervalNames)
        }
    }

    fun clearDeleteImpact() {
        _deleteImpact.value = null
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            try {
                exerciseDao.deleteExerciseAndTodoTasks(exercise)
                _snackbarMessage.value = UiMessage.ExerciseDeleted
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun toggleFavorite(exerciseId: Long) {
        viewModelScope.launch {
            try {
                val exercise = exercises.value.find { it.id == exerciseId }
                exercise?.let {
                    val updated = it.copy(isFavorite = !it.isFavorite)
                    exerciseDao.updateExercise(updated)
                }
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    // Training Record operations (legacy rows; new training is written as v2 sessions)
    fun updateRecord(record: TrainingRecord) {
        viewModelScope.launch {
            try {
                recordDao.updateRecord(record)
                _snackbarMessage.value = UiMessage.RecordUpdated
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun deleteSession(exerciseId: Long, date: String, time: String) {
        viewModelScope.launch {
            try {
                recordDao.deleteSession(exerciseId, date, time)
                _snackbarMessage.value = UiMessage.RecordDeleted
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun updateV2SetValues(set: HistorySet, valueRight: Int, valueLeft: Int?) = editV2(UiMessage.RecordUpdated) {
        V2HistoryEditor.updateSetValues(database, set, valueRight, valueLeft)
    }

    /** The exercise library as stored now, for the workout editor's names and fields. */
    suspend fun libraryExercises(): List<Exercise> = withContext(Dispatchers.IO) { database.backupDao().exercises() }

    /** The workout as the editor shows it, or null when there is nothing to edit (ADR 0009). */
    suspend fun loadWorkoutDraft(ref: WorkoutRef): WorkoutDraft? = withContext(Dispatchers.IO) {
        when (ref) {
            is WorkoutRef.V2 -> WorkoutEditor.loadV2(database, ref.sessionId, ZoneId.systemDefault())
            is WorkoutRef.Legacy -> WorkoutEditor.loadLegacy(database, ref.date, ref.time, ref.comment, database.backupDao().exercises())
        }
    }

    /**
     * Saves an edited workout in one transaction and says so; null when the save failed, with the
     * error shown. A stale draft or a taken time writes nothing and is for the editor to explain.
     */
    suspend fun saveWorkoutDraft(ref: WorkoutRef, original: WorkoutDraft, edited: WorkoutDraft): WorkoutSaveResult? =
        withContext(Dispatchers.IO) {
            try {
                val library = database.backupDao().exercises()
                val result = when (ref) {
                    is WorkoutRef.V2 -> WorkoutEditor.saveV2(
                        database, ref.sessionId, original, edited, library, database.backupDao().groups(),
                        ZoneId.systemDefault(), System.currentTimeMillis(),
                    )
                    is WorkoutRef.Legacy -> WorkoutEditor.saveLegacy(database, original, edited, library)
                }
                when (result) {
                    WorkoutSaveResult.SAVED -> _snackbarMessage.value = UiMessage.RecordUpdated
                    WorkoutSaveResult.DELETED -> _snackbarMessage.value = UiMessage.RecordDeleted
                    else -> {}
                }
                result
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
                null
            }
        }

    fun deleteV2Sets(sets: List<HistorySet>) = editV2(UiMessage.RecordDeleted) {
        V2HistoryEditor.delete(database, sets)
    }

    private fun editV2(done: UiMessage, edit: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                edit()
                _snackbarMessage.value = done
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    // ========================================
    // Group operations
    // ========================================

    fun createGroup(name: String) {
        viewModelScope.launch {
            flushGroupOrder()
            try {
                // After every existing group; the group count can repeat an order once one is deleted.
                val group = ExerciseGroup(name = name, displayOrder = groupDao.nextGroupDisplayOrder())
                groupDao.insertGroup(group)
                _snackbarMessage.value = UiMessage.GroupCreated
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.GroupAlreadyExists
            }
        }
    }

    fun renameGroup(oldName: String, newName: String) {
        viewModelScope.launch {
            flushGroupOrder()
            try {
                // グループと種目のgroupフィールドを1トランザクションで更新
                groupDao.renameGroupAndExercises(oldName, newName)

                _snackbarMessage.value = UiMessage.GroupRenamed
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun deleteGroup(groupName: String) {
        viewModelScope.launch {
            flushGroupOrder()
            try {
                // ToDo・グループ削除と種目のグループ解除を1トランザクションで実行
                groupDao.deleteGroupAndUngroupExercises(groupName)

                _snackbarMessage.value = UiMessage.GroupDeleted
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }
    // ========================================
    // 階層表示用データ構造
    // ========================================

    data class GroupWithExercises(
        val groupName: String?,  // nullは「グループなし」
        val exercises: List<Exercise>,
        val isExpanded: Boolean = true  // 展開状態
    )

    // 展開状態管理
    private val _expandedGroups = MutableStateFlow<Set<String>>(emptySet())
    val expandedGroups: StateFlow<Set<String>> = _expandedGroups.asStateFlow()

    fun toggleGroupExpansion(groupName: String) {
        _expandedGroups.value = if (groupName in _expandedGroups.value) {
            _expandedGroups.value - groupName
        } else {
            _expandedGroups.value + groupName
        }
    }

    // グループ並び替え用ローカルステート（画面離脱時にDBに保存）
    private val _localGroupOrder = MutableStateFlow<List<ExerciseGroup>?>(null)

    // 階層データ準備
    val hierarchicalExercises: StateFlow<List<GroupWithExercises>> =
        combine(groups, exercises, expandedGroups, _localGroupOrder) { dbGroups, exercises, expanded, localOrder ->
            libraryHierarchy(localOrder ?: dbGroups, exercises, expanded)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // ========================================
    // 並び替え機能
    // ========================================

    /**
     * 種目の並び順を変更する
     * @param groupName グループ名（null = グループなし、FAVORITE_GROUP_KEY = お気に入り）
     * @param fromIndex 移動元のインデックス
     * @param toIndex 移動先のインデックス
     */
    fun reorderExercises(groupName: String?, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            try {
                libraryActions.moveExercise(groupName, fromIndex, toIndex)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    /**
     * グループの並び順を変更する（ローカルステートのみ更新）
     * DBへの保存は saveGroupOrder() で行う
     */
    fun reorderGroups(fromIndex: Int, toIndex: Int) {
        val current = (_localGroupOrder.value ?: groups.value).toList()

        if (fromIndex < 0 || toIndex < 0 ||
            fromIndex >= current.size ||
            toIndex >= current.size) {
            return
        }

        val reordered = current.toMutableList()
        val item = reordered.removeAt(fromIndex)
        reordered.add(toIndex, item)
        _localGroupOrder.value = reordered
    }

    /**
     * 保留中のグループ並び順をDBに保存する
     */
    fun saveGroupOrder() {
        val order = _localGroupOrder.value ?: return
        _localGroupOrder.value = null
        viewModelScope.launch { writeGroupOrder(order) }
    }

    /**
     * 保留中のグループ並び順をDBに保存する（suspend版・内部用）
     */
    private suspend fun flushGroupOrder() {
        val order = _localGroupOrder.value ?: return
        _localGroupOrder.value = null
        writeGroupOrder(order)
    }

    private val libraryActions = LibraryActions(database)

    private suspend fun writeGroupOrder(order: List<ExerciseGroup>) = libraryActions.saveGroupOrder(order)

    // ========================================
    // エクスポート・インポート機能
    // ========================================

    /** Export one consistent database snapshot as the v8 JSON wire format. */
    suspend fun exportData(): BackupResult<ExportedBackup> = withContext(Dispatchers.IO) {
        backupService.export()
    }

    /** Parse and fully validate a backup, with its anomalies, before the UI offers to overwrite data. */
    fun parseBackupData(jsonString: String): BackupResult<ParsedBackup> =
        backupService.parse(jsonString)

    /** Atomically replace all backup tables with an already parsed payload. */
    suspend fun importData(backupData: BackupData): BackupResult<BackupSummary> =
        withContext(Dispatchers.IO) {
            backupService.restore(backupData).also {
                // Restored exercises may name groups the backup lacks; give them rows so they are listed.
                if (it is BackupResult.Success) restoreMissingGroups(database)
            }
        }

    // ========================================
    // CSV エクスポート・インポート機能 (CsvTransfer)
    // ========================================

    private val csvTransfer = CsvTransfer(database) { _snackbarMessage.value = it }

    suspend fun exportGroups(): String = csvTransfer.exportGroups()
    suspend fun exportExercises(): String = csvTransfer.exportExercises()
    suspend fun exportRecordTemplate(): String = csvTransfer.exportRecordTemplate()
    suspend fun exportRecords(): String = csvTransfer.exportRecords()
    suspend fun importGroups(csvString: String): CsvImportReport = csvTransfer.importGroups(csvString)
    suspend fun importExercises(csvString: String): CsvImportReport = csvTransfer.importExercises(csvString)
    suspend fun importRecordsFromCsv(csvString: String): CsvImportReport = csvTransfer.importRecordsFromCsv(csvString)

    // ========================================
    // To Do Task 操作
    // ========================================

    val todoTasks: StateFlow<List<TodoTask>> = todoTaskDao.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val todoActions = TodoActions(database)

    private fun addTodo(type: String, referenceIds: List<Long>) {
        viewModelScope.launch {
            try {
                todoActions.add(type, referenceIds)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun addTodoTask(exerciseId: Long) = addTodo(TodoTask.TYPE_EXERCISE, listOf(exerciseId))
    fun addTodoTasks(exerciseIds: List<Long>) = addTodo(TodoTask.TYPE_EXERCISE, exerciseIds)
    fun addTodoTaskProgram(programId: Long) = addTodo(TodoTask.TYPE_PROGRAM, listOf(programId))
    fun addTodoTaskPrograms(programIds: List<Long>) = addTodo(TodoTask.TYPE_PROGRAM, programIds)
    fun addTodoTaskGroups(groupIds: List<Long>) = addTodo(TodoTask.TYPE_GROUP, groupIds)
    fun addTodoTaskInterval(intervalProgramId: Long) = addTodo(TodoTask.TYPE_INTERVAL, listOf(intervalProgramId))
    fun addTodoTaskIntervals(intervalProgramIds: List<Long>) = addTodo(TodoTask.TYPE_INTERVAL, intervalProgramIds)

    fun deleteTodoTask(taskId: Long) {
        viewModelScope.launch {
            try {
                todoTaskDao.deleteById(taskId)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun deleteTodoTaskByReference(type: String, referenceId: Long) {
        viewModelScope.launch {
            try {
                todoTaskDao.deleteByReference(type, referenceId)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    // The session a live workout just saved; the app shows its summary, then clears this.
    private val _finishedWorkout = MutableStateFlow<Long?>(null)
    val finishedWorkout: StateFlow<Long?> = _finishedWorkout.asStateFlow()

    fun dismissWorkoutSummary() {
        _finishedWorkout.value = null
    }

    /** Reads a saved workout and the history before it; null when the session is gone. */
    suspend fun loadWorkoutSummary(sessionId: Long): WorkoutSummary? = withContext(Dispatchers.IO) {
        val graph = database.workoutSessionDao().sessionGraph(sessionId) ?: return@withContext null
        WorkoutSummaryBuilder.build(graph, CompatibilityHistory.read(database, ZoneId.systemDefault()))
    }

    /**
     * Saves a finished single-exercise workout as one v2 session, then, when it came from a
     * to-do, completes that to-do. They run in that order, so the group check sees the new sets.
     */
    fun recordSingleWorkout(workout: SingleWorkout, completeTodo: Boolean) {
        viewModelScope.launch {
            try {
                // The summary that follows confirms the save.
                SingleWorkoutWriter.write(database, workout)?.let { _finishedWorkout.value = it }
                if (completeTodo) completeTodoTask(TodoTask.TYPE_EXERCISE, workout.exerciseId)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    /** Saves a finished program run as one v2 session. */
    fun recordProgramWorkout(run: ProgramRun) {
        viewModelScope.launch {
            try {
                ProgramWorkoutWriter.write(database, run)?.let { _finishedWorkout.value = it }
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    /** Saves sets entered by hand as one v2 session, then completes the to-do it came from, in that order. */
    fun recordManualWorkout(workout: ManualWorkout, completeTodo: Boolean) =
        recordManualWorkouts(listOf(workout), completeTodoFor = workout.exerciseId.takeIf { completeTodo })

    /**
     * Saves one or more exercises entered by hand as one v2 session, then completes the to-do
     * the entry came from, in that order.
     */
    fun recordManualWorkouts(workouts: List<ManualWorkout>, completeTodoFor: Long?) {
        viewModelScope.launch {
            try {
                if (ManualWorkoutWriter.write(database, workouts, ZoneId.systemDefault()) != null) {
                    _snackbarMessage.value = UiMessage.SetsRecorded(workouts.sumOf { it.sets.size })
                }
                if (completeTodoFor != null) completeTodoTask(TodoTask.TYPE_EXERCISE, completeTodoFor)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun completeTodoTaskByReference(type: String, referenceId: Long) {
        viewModelScope.launch {
            try {
                completeTodoTask(type, referenceId)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    private suspend fun completeTodoTask(type: String, referenceId: Long) =
        todoActions.complete(type, referenceId, AppClock.today(), ZoneId.systemDefault())

    suspend fun hasRecordOnDate(exerciseId: Long, date: String): Boolean =
        CompatibilityHistory.hasSetOn(database, exerciseId, LocalDate.parse(date), ZoneId.systemDefault())

    fun updateTodoRepeatDays(taskId: Long, repeatDays: String) {
        viewModelScope.launch {
            try {
                todoTaskDao.updateRepeatDays(taskId, repeatDays)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun reorderTodoTasks(taskIds: List<Long>) {
        viewModelScope.launch {
            try {
                todoTaskDao.reorderTasks(taskIds)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun reorderTodoTasks(fromIndex: Int, toIndex: Int) {
        val currentTasks = todoTasks.value.toMutableList()
        if (fromIndex < 0 || toIndex < 0 ||
            fromIndex >= currentTasks.size || toIndex >= currentTasks.size) {
            return
        }

        // Move the item in the list
        val item = currentTasks.removeAt(fromIndex)
        currentTasks.add(toIndex, item)

        // Update database with new order
        val reorderedIds = currentTasks.map { it.id }
        reorderTodoTasks(reorderedIds)
    }

    /**
     * 指定した種目の前回セッション記録を取得
     * オートフィル機能用
     */
    suspend fun getLatestSession(exerciseId: Long): List<HistorySet> =
        CompatibilityHistory.latestSession(database, exerciseId, ZoneId.systemDefault())

    // ========================================
    // Program 操作
    // ========================================

    val programs: StateFlow<List<Program>> = programDao.getAllPrograms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun createProgram(name: String) {
        viewModelScope.launch {
            try {
                val program = Program(name = name)
                programDao.insert(program)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    private val programActions = ProgramActions(database)

    fun deleteProgram(programId: Long) {
        viewModelScope.launch {
            try {
                programActions.deleteProgram(programId)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun duplicateProgram(programId: Long, copySuffix: String) {
        viewModelScope.launch {
            try {
                if (programActions.duplicateProgram(programId, copySuffix) != null) {
                    _snackbarMessage.value = UiMessage.ProgramDuplicated
                }
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    /** Saves the program editor in one transaction; false, with the error shown, when it failed. */
    suspend fun saveProgramDraft(programId: Long?, draft: ProgramDraft): Boolean = saveDraft {
        programActions.saveProgram(programId, draft)
    }

    /** Saves the interval editor in one transaction; false, with the error shown, when it failed. */
    suspend fun saveIntervalProgramDraft(programId: Long?, draft: IntervalProgramDraft): Boolean = saveDraft {
        programActions.saveIntervalProgram(programId, draft)
    }

    private suspend fun saveDraft(save: suspend () -> Unit): Boolean = try {
        save()
        true
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        _snackbarMessage.value = UiMessage.ErrorOccurred
        false
    }

    suspend fun getProgramById(programId: Long): Program? {
        return programDao.getProgramById(programId)
    }

    // ========================================
    // ProgramExercise 操作
    // ========================================

    fun getProgramExercisesFlow(programId: Long) = programExerciseDao.getExercisesForProgram(programId)

    suspend fun getProgramExercisesSync(programId: Long): List<ProgramExercise> {
        return programExerciseDao.getExercisesForProgramSync(programId)
    }

    fun addProgramExercise(
        programId: Long,
        exerciseId: Long,
        sets: Int = 1,
        targetValue: Int,
        intervalSeconds: Int = 60
    ) {
        viewModelScope.launch {
            try {
                val sortOrder = programExerciseDao.getNextSortOrder(programId)
                val programExercise = ProgramExercise(
                    programId = programId,
                    exerciseId = exerciseId,
                    sortOrder = sortOrder,
                    sets = sets,
                    targetValue = targetValue,
                    intervalSeconds = intervalSeconds
                )
                programExerciseDao.insert(programExercise)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    suspend fun reorderProgramExercises(exerciseIds: List<Long>) {
        try {
            programExerciseDao.reorderExercises(exerciseIds)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    suspend fun reorderProgramExercises(programId: Long, fromIndex: Int, toIndex: Int) {
        try {
            programActions.moveProgramExercise(programId, fromIndex, toIndex)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    // ========================================
    // ProgramLoop 操作
    // ========================================

    fun getProgramLoopsFlow(programId: Long) = programLoopDao.getLoopsForProgram(programId)

    suspend fun getProgramLoopsSync(programId: Long): List<ProgramLoop> {
        return programLoopDao.getLoopsForProgramSync(programId)
    }

    suspend fun getProgramLoopById(loopId: Long): ProgramLoop? {
        return programLoopDao.getLoopById(loopId)
    }

    suspend fun addProgramExerciseToLoop(
        programId: Long,
        exerciseId: Long,
        loopId: Long,
        sets: Int = 1,
        targetValue: Int,
        intervalSeconds: Int = 60
    ): Long? {
        return try {
            val sortOrder = programExerciseDao.getNextSortOrder(programId)
            val programExercise = ProgramExercise(
                programId = programId,
                exerciseId = exerciseId,
                sortOrder = sortOrder,
                sets = sets,
                targetValue = targetValue,
                intervalSeconds = intervalSeconds,
                loopId = loopId
            )
            programExerciseDao.insert(programExercise)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
            null
        }
    }

    suspend fun moveExerciseToLoop(programExercise: ProgramExercise, loopId: Long?) {
        try {
            programExerciseDao.update(programExercise.copy(loopId = loopId))
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    // ========================================
    // IntervalProgram 操作
    // ========================================

    val intervalPrograms: StateFlow<List<IntervalProgram>> = intervalProgramDao.getAllPrograms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun deleteIntervalProgram(programId: Long) {
        viewModelScope.launch {
            try {
                programActions.deleteIntervalProgram(programId)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    suspend fun getIntervalProgramById(programId: Long): IntervalProgram? {
        return intervalProgramDao.getProgramById(programId)
    }

    fun duplicateIntervalProgram(programId: Long, copySuffix: String) {
        viewModelScope.launch {
            try {
                if (programActions.duplicateIntervalProgram(programId, copySuffix) != null) {
                    _snackbarMessage.value = UiMessage.ProgramDuplicated
                }
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    // ========================================
    // IntervalProgramExercise 操作
    // ========================================

    fun getIntervalProgramExercisesFlow(programId: Long) =
        intervalProgramExerciseDao.getExercisesForProgram(programId)

    suspend fun getIntervalProgramExercisesSync(programId: Long): List<IntervalProgramExercise> {
        return intervalProgramExerciseDao.getExercisesForProgramSync(programId)
    }

    suspend fun reorderIntervalProgramExercises(exerciseIds: List<Long>) {
        try {
            intervalProgramExerciseDao.reorderExercises(exerciseIds)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    // ========================================
    // IntervalRecord 操作
    // ========================================

    // Interval history screens read legacy interval records plus v2 interval workouts, each once.
    val intervalHistory: StateFlow<List<IntervalHistoryItem>> = combine(
        intervalRecordDao.getAllRecords(),
        database.workoutSessionDao().observeIntervalSessionRows()
    ) { legacy, v2 -> IntervalHistory.merge(legacy, v2, ZoneId.systemDefault()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    /** Saves a finished or stopped interval workout as one v2 session. */
    suspend fun recordIntervalWorkout(run: IntervalRun) {
        try {
            _finishedWorkout.value = IntervalWorkoutWriter.write(database, run)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    fun updateV2IntervalSession(sessionId: Long, date: String, time: String, comment: String) =
        editV2(UiMessage.RecordUpdated) {
            V2HistoryEditor.updateSession(
                database, sessionId, date, time, comment, ZoneId.systemDefault(), System.currentTimeMillis()
            )
        }

    fun deleteV2IntervalSession(sessionId: Long) = editV2(UiMessage.RecordDeleted) {
        V2HistoryEditor.deleteSession(database, sessionId)
    }

    suspend fun updateIntervalRecord(record: IntervalRecord) {
        try {
            intervalRecordDao.update(record)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    fun updateIntervalRecordAsync(record: IntervalRecord) {
        viewModelScope.launch {
            updateIntervalRecord(record)
        }
    }

    fun deleteIntervalRecord(recordId: Long) {
        viewModelScope.launch {
            try {
                intervalRecordDao.deleteById(recordId)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    // ========================================
    // コミュニティシェア エクスポート・インポート機能
    // ========================================

    private val communityShare = CommunityShareTransfer(
        database,
        appVersion = {
            try {
                val application = getApplication<Application>()
                application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: "unknown"
            } catch (_: Exception) {
                "unknown"
            }
        },
        onMessage = { _snackbarMessage.value = it },
    )

    /** "backup", "share" or "unknown". */
    fun detectJsonFileType(jsonString: String): String = communityShare.detectJsonFileType(jsonString)

    suspend fun exportCommunityShare(
        selectedProgramIds: Set<Long>,
        selectedIntervalProgramIds: Set<Long>,
        selectedExerciseIds: Set<Long>,
    ): String = communityShare.exportCommunityShare(selectedProgramIds, selectedIntervalProgramIds, selectedExerciseIds)

    suspend fun previewCommunityShareImport(data: CommunityShareData): CommunityShareImportReport =
        communityShare.previewCommunityShareImport(data)

    suspend fun importCommunityShare(jsonString: String): CommunityShareImportReport =
        communityShare.importCommunityShare(jsonString)

}

/** The day each thing was last done; missing when it never was (or only before v2, for programs). */
data class LastDone(
    val exercises: Map<Long, LocalDate> = emptyMap(),
    val programs: Map<Long, LocalDate> = emptyMap(),
    val intervals: Map<Long, LocalDate> = emptyMap(),
)
