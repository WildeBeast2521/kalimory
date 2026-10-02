package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.material3.LoadingIndicator
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.WorkoutTone
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.WorkoutStatus
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.WorkoutSkipButton
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.WorkoutHeader
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.TimerDial
import androidx.activity.compose.BackHandler
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.v2.Laterality
import io.github.gonbei774.calisthenicsmemory.data.v2.IntervalRunExercise
import io.github.gonbei774.calisthenicsmemory.data.v2.IntervalRun
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import io.github.gonbei774.calisthenicsmemory.service.WorkoutTimerService
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import io.github.gonbei774.calisthenicsmemory.util.SoundPlayer
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import io.github.gonbei774.calisthenicsmemory.workout.IntervalExerciseSnapshot
import io.github.gonbei774.calisthenicsmemory.workout.IntervalSessionContext
import io.github.gonbei774.calisthenicsmemory.workout.IntervalStepType
import io.github.gonbei774.calisthenicsmemory.workout.RecoveryTiming
import io.github.gonbei774.calisthenicsmemory.workout.Transition
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutCheckpoint
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutCheckpointStore
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutRecovery
import io.github.gonbei774.calisthenicsmemory.workout.currentBootCount
import io.github.gonbei774.calisthenicsmemory.workout.IntervalWorkoutPlan
import io.github.gonbei774.calisthenicsmemory.workout.MonotonicClock
import io.github.gonbei774.calisthenicsmemory.workout.StepKind
import io.github.gonbei774.calisthenicsmemory.workout.StepOutcome
import io.github.gonbei774.calisthenicsmemory.workout.StepTimer
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutEvent
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutReducer
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutState
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * Exercise info resolved from IntervalProgramExercise + Exercise
 */
private data class IntervalExerciseInfo(
    val exerciseId: Long,
    val name: String,
    val description: String?
)

/**
 * Execution phase state machine
 */
private sealed class IntervalPhase {
    object Loading : IntervalPhase()

    data class Confirm(
        val program: IntervalProgram,
        val exercises: List<IntervalExerciseInfo>
    ) : IntervalPhase()

    /** Prepare, work, rest, and round rest; the timer itself lives in a [WorkoutState]. */
    object Running : IntervalPhase()

    data class Complete(
        val completedRounds: Int,
        val completedExercisesInLastRound: Int,
        val isFullCompletion: Boolean
    ) : IntervalPhase()
}

internal const val INTERVAL_CHECKPOINT_FILE = "interval-workout-checkpoint.json"

/** Whole seconds shown for [remainingMillis]: 0.1 s left still shows 1. */
private fun displaySeconds(remainingMillis: Long): Int = ((remainingMillis + 999) / 1_000).toInt()

@Composable
fun IntervalExecutionScreen(
    viewModel: TrainingViewModel,
    programId: Long,
    onNavigateBack: () -> Unit,
    onComplete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // Preferences
    val workoutPrefs = remember { WorkoutPreferences(context) }
    val isKeepScreenOnEnabled = remember { workoutPrefs.isKeepScreenOnEnabled() }
    val isFlashEnabled = remember { workoutPrefs.isFlashNotificationEnabled() }

    // Audio & Flash
    val soundPlayer = remember { SoundPlayer(context) }
    val flashController = remember { FlashController(context) }

    // State
    var phase by remember { mutableStateOf<IntervalPhase>(IntervalPhase.Loading) }
    var program by remember { mutableStateOf<IntervalProgram?>(null) }
    var exercises by remember { mutableStateOf<List<IntervalExerciseInfo>>(emptyList()) }
    // Timer truth: a WorkoutState driven by WorkoutReducer, rendered from the monotonic clock.
    val clock = remember { MonotonicClock.SYSTEM }
    var workout by remember { mutableStateOf<WorkoutState?>(null) }
    var nowMillis by remember { mutableLongStateOf(0L) }
    var observedResultCount by remember { mutableIntStateOf(0) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    // Checkpoint: survives process death. Writes run in order on one background thread,
    // independent of composition, so a save is never cancelled half-way.
    val checkpointStore = remember { WorkoutCheckpointStore(File(context.filesDir, INTERVAL_CHECKPOINT_FILE)) }
    val checkpointWriter = remember { Executors.newSingleThreadExecutor() }
    var sessionContext by remember { mutableStateOf<IntervalSessionContext?>(null) }
    // The checkpoint and how it will be timed; recovery itself runs when the user taps Resume,
    // so time spent reading the dialog is not added to an estimate.
    var pendingResume by remember { mutableStateOf<Pair<WorkoutCheckpoint, RecoveryTiming>?>(null) }

    fun saveCheckpoint(state: WorkoutState) {
        val interval = sessionContext ?: return
        val checkpoint = WorkoutCheckpoint(
            state = state,
            savedAtMonotonicMillis = clock.nowMillis(),
            savedAtWallMillis = System.currentTimeMillis(),
            bootCount = currentBootCount(context),
            interval = interval
        )
        checkpointWriter.execute {
            try {
                checkpointStore.save(checkpoint)
            } catch (e: java.io.IOException) {
                android.util.Log.e("IntervalCheckpoint", "Could not save the interval checkpoint", e)
            }
        }
    }

    fun clearCheckpoint() {
        checkpointWriter.execute { checkpointStore.clear() }
    }

    // Load program data
    LaunchedEffect(programId) {
        val loadedProgram = viewModel.getIntervalProgramById(programId)
        if (loadedProgram == null) {
            onNavigateBack()
            return@LaunchedEffect
        }
        program = loadedProgram

        val programExercises = viewModel.getIntervalProgramExercisesSync(programId)
        val allExercises = viewModel.exercises.value
        val exerciseMap = allExercises.associateBy { it.id }

        val resolved = programExercises
            .sortedBy { it.sortOrder }
            .mapNotNull { pe ->
                exerciseMap[pe.exerciseId]?.let { ex ->
                    IntervalExerciseInfo(
                        exerciseId = ex.id,
                        name = ex.name,
                        description = ex.description
                    )
                }
            }
        exercises = resolved
        phase = IntervalPhase.Confirm(loadedProgram, resolved)

        // Offer to resume a workout of this program that the system interrupted.
        val saved = withContext(Dispatchers.IO) { checkpointStore.load() }
        val checkpoint = (saved as? WorkoutCheckpointStore.LoadResult.Found)?.checkpoint
        val interval = checkpoint?.interval
        if (checkpoint != null && interval != null && interval.programId == programId) {
            val timing = WorkoutRecovery.recover(
                checkpoint, clock.nowMillis(), System.currentTimeMillis(), currentBootCount(context)
            ).timing
            pendingResume = checkpoint to timing
        }
    }

    // Keep screen on
    LaunchedEffect(phase, isKeepScreenOnEnabled) {
        val window = (view.context as? android.app.Activity)?.window
        if (isKeepScreenOnEnabled) {
            if (phase is IntervalPhase.Running) {
                window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Foreground service
    LaunchedEffect(phase) {
        if (phase is IntervalPhase.Running) {
            WorkoutTimerService.startService(context)
        } else {
            WorkoutTimerService.stopService(context)
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            val window = (view.context as? android.app.Activity)?.window
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            soundPlayer.release()
            flashController.turnOff()
            WorkoutTimerService.stopService(context)
            // Queued checkpoint writes still finish; no new ones are accepted.
            checkpointWriter.shutdown()
        }
    }

    // Back gesture: show exit dialog during active phases, unsaved dialog on complete
    val isActivePhase = phase is IntervalPhase.Running
    val isCompletePhase = phase is IntervalPhase.Complete
    BackHandler(enabled = isActivePhase || isCompletePhase) {
        if (isCompletePhase) {
            showUnsavedDialog = true
        } else {
            showExitDialog = true
        }
    }

    // Exit dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = cs.surfaceContainerLow,
            title = {
                Text(
                    stringResource(R.string.interval_exit_confirm_title),
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    stringResource(R.string.interval_exit_confirm_message),
                    color = cs.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    val current = workout ?: return@TextButton
                    // Catch up first, so a countdown that already ended counts as done.
                    val now = clock.nowMillis()
                    val caughtUp = WorkoutReducer.reduce(current, WorkoutEvent.Tick(now)).state
                    val step = IntervalWorkoutPlan.describe(caughtUp.currentStep, exercises.size)
                    val (completedRounds, completedExInLast) =
                        IntervalWorkoutPlan.progressWhenStopped(step, exercises.size)
                    workout = WorkoutReducer.reduce(caughtUp, WorkoutEvent.Abandon(now)).state
                    clearCheckpoint()

                    phase = IntervalPhase.Complete(
                        completedRounds = completedRounds,
                        completedExercisesInLastRound = completedExInLast,
                        isFullCompletion = false
                    )
                }) {
                    Text(stringResource(R.string.interval_stop), color = cs.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.cancel), color = cs.onSurfaceVariant)
                }
            }
        )
    }

    // Resume dialog for a workout the system interrupted
    pendingResume?.let { (checkpoint, timing) ->
        val interval = checkpoint.interval ?: return@let
        AlertDialog(
            onDismissRequest = {},
            containerColor = cs.surfaceContainerLow,
            title = {
                Text(
                    stringResource(R.string.interval_resume_title),
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.interval_resume_message), color = cs.onSurfaceVariant)
                    if (timing != RecoveryTiming.EXACT) {
                        Text(stringResource(R.string.interval_resume_approximate), color = cs.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingResume = null
                    program = IntervalProgram(
                        id = interval.programId,
                        name = interval.programName,
                        workSeconds = interval.workSeconds,
                        restSeconds = interval.restSeconds,
                        rounds = interval.rounds,
                        roundRestSeconds = interval.roundRestSeconds
                    )
                    exercises = interval.exercises.map { IntervalExerciseInfo(it.exerciseId, it.name, it.description) }
                    sessionContext = interval
                    val now = clock.nowMillis()
                    val recovered = WorkoutRecovery.recover(
                        checkpoint, now, System.currentTimeMillis(), currentBootCount(context)
                    )
                    val resumed = WorkoutReducer.reduce(recovered.state, WorkoutEvent.Tick(now)).state
                    workout = resumed
                    nowMillis = now
                    // Countdowns that ended while the app was gone get no sound now.
                    observedResultCount = resumed.results.size
                    if (resumed.status == WorkoutStatus.FINISHED) {
                        clearCheckpoint()
                        phase = IntervalPhase.Complete(
                            completedRounds = interval.rounds,
                            completedExercisesInLastRound = interval.exercises.size,
                            isFullCompletion = true
                        )
                    } else {
                        saveCheckpoint(resumed)
                        phase = IntervalPhase.Running
                    }
                }) {
                    Text(stringResource(R.string.interval_resume_confirm), color = cs.tertiary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingResume = null
                    clearCheckpoint()
                }) {
                    Text(stringResource(R.string.interval_resume_discard), color = cs.error)
                }
            }
        )
    }

    // Unsaved record dialog (back gesture on complete screen)
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            containerColor = cs.surfaceContainerLow,
            title = {
                Text(
                    stringResource(R.string.interval_unsaved_title),
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    stringResource(R.string.interval_unsaved_message),
                    color = cs.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onComplete()
                }) {
                    Text(stringResource(R.string.interval_unsaved_leave), color = cs.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text(stringResource(R.string.cancel), color = cs.onSurfaceVariant)
                }
            }
        )
    }

    // Render ticks: time comes from the clock, so a late or skipped tick adds no drift.
    LaunchedEffect(phase) {
        if (phase !is IntervalPhase.Running) return@LaunchedEffect
        var lastStepIndex = -1
        var lastShownSeconds = -1
        while (true) {
            val now = clock.nowMillis()
            val state = WorkoutReducer.reduce(workout ?: break, WorkoutEvent.Tick(now)).state
            workout = state
            nowMillis = now

            // Cue for countdowns that ended by themselves since the last tick.
            val finished = state.results.drop(observedResultCount).lastOrNull { it.outcome == StepOutcome.COMPLETED }
            observedResultCount = state.results.size
            if (finished != null) {
                val finishedStep = IntervalWorkoutPlan.describe(state.steps[finished.stepIndex], exercises.size)
                if (finishedStep.type == IntervalStepType.WORK) {
                    if (isFlashEnabled) launch { flashController.flashSetComplete() }
                    soundPlayer.playSetComplete()
                } else {
                    soundPlayer.playStartCue()
                    if (isFlashEnabled) launch { flashController.flashComplete() }
                }
            }
            if (state.status == WorkoutStatus.FINISHED) {
                clearCheckpoint()
                phase = IntervalPhase.Complete(
                    completedRounds = program?.rounds ?: 0,
                    completedExercisesInLastRound = exercises.size,
                    isFullCompletion = true
                )
                break
            }

            // Beep as the display reaches 3, 2, and 1 within a step.
            val shown = displaySeconds(state.remainingInStep(now) ?: 0)
            if (state.stepIndex == lastStepIndex && shown != lastShownSeconds && shown in 1..3 &&
                state.timer is StepTimer.Running
            ) {
                soundPlayer.playCountdown()
                if (isFlashEnabled) launch { flashController.flashShort() }
            }
            lastStepIndex = state.stepIndex
            lastShownSeconds = shown
            delay(100L)
        }
    }

    fun togglePause() {
        val current = workout ?: return
        val now = clock.nowMillis()
        val event = if (current.timer is StepTimer.Paused) WorkoutEvent.Resume(now) else WorkoutEvent.Pause(now)
        val transition = WorkoutReducer.reduce(current, event)
        workout = transition.state
        nowMillis = now
        if (transition is Transition.Accepted) saveCheckpoint(transition.state)
    }

    fun skipCurrentStep() {
        val current = workout ?: return
        val now = clock.nowMillis()
        val transition = WorkoutReducer.reduce(current, WorkoutEvent.SkipStep(now, current.stepIndex))
        workout = transition.state
        observedResultCount = transition.state.results.size
        nowMillis = now
        if (transition is Transition.Accepted) saveCheckpoint(transition.state)
    }

    // Main content
    when (val currentPhase = phase) {
        is IntervalPhase.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(color = cs.tertiary)
            }
        }

        is IntervalPhase.Confirm -> {
            IntervalConfirmContent(
                program = currentPhase.program,
                exercises = currentPhase.exercises,
                onStart = {
                    val p = currentPhase.program
                    val steps = IntervalWorkoutPlan.build(
                        p.workSeconds, p.restSeconds, p.rounds, p.roundRestSeconds, currentPhase.exercises.size
                    )
                    val now = clock.nowMillis()
                    val started = WorkoutReducer.reduce(WorkoutState(steps), WorkoutEvent.Start(now)).state
                    workout = started
                    nowMillis = now
                    observedResultCount = 0
                    sessionContext = IntervalSessionContext(
                        programId = p.id,
                        programName = p.name,
                        workSeconds = p.workSeconds,
                        restSeconds = p.restSeconds,
                        rounds = p.rounds,
                        roundRestSeconds = p.roundRestSeconds,
                        exercises = currentPhase.exercises.map {
                            IntervalExerciseSnapshot(it.exerciseId, it.name, it.description)
                        },
                        startedAtWallMillis = System.currentTimeMillis()
                    )
                    saveCheckpoint(started)
                    phase = IntervalPhase.Running
                },
                onBack = onNavigateBack
            )
        }

        is IntervalPhase.Running -> {
            val state = workout ?: return
            val step = IntervalWorkoutPlan.describe(state.currentStep, exercises.size)
            val remainingSeconds = displaySeconds(state.remainingInStep(nowMillis) ?: 0)
            val totalSeconds = displaySeconds((state.currentStep.kind as StepKind.Countdown).durationMillis)
            val isPaused = state.timer is StepTimer.Paused
            when (step.type) {
                IntervalStepType.PREPARE -> IntervalPrepareContent(
                    exercises = exercises,
                    remainingSeconds = remainingSeconds,
                    totalSeconds = totalSeconds,
                    isPaused = isPaused,
                    onPauseToggle = ::togglePause
                )
                IntervalStepType.WORK -> IntervalTimerContent(
                    program = program!!,
                    exercises = exercises,
                    round = step.round,
                    exerciseIndex = step.exerciseIndex,
                    remainingSeconds = remainingSeconds,
                    totalSeconds = totalSeconds,
                    phaseColor = cs.tertiary,
                    phaseLabel = stringResource(R.string.interval_work_label),
                    exerciseName = exercises[step.exerciseIndex].name,
                    nextPreview = null,
                    isPaused = isPaused,
                    onPauseToggle = ::togglePause,
                    onStop = { showExitDialog = true },
                    onSkip = null
                )
                IntervalStepType.REST -> {
                    val nextExercise = exercises.getOrNull(step.exerciseIndex + 1)
                    IntervalTimerContent(
                        program = program!!,
                        exercises = exercises,
                        round = step.round,
                        exerciseIndex = step.exerciseIndex,
                        remainingSeconds = remainingSeconds,
                        totalSeconds = totalSeconds,
                        phaseColor = cs.secondary,
                        phaseLabel = stringResource(R.string.interval_rest_label),
                        exerciseName = null,
                        nextPreview = nextExercise?.let {
                            NextPreviewInfo(
                                label = stringResource(R.string.interval_next),
                                exerciseName = it.name,
                                description = it.description
                            )
                        },
                        isPaused = isPaused,
                        onPauseToggle = ::togglePause,
                        onStop = null,
                        onSkip = ::skipCurrentStep
                    )
                }
                IntervalStepType.ROUND_REST -> {
                    val firstExercise = exercises.firstOrNull()
                    IntervalTimerContent(
                        program = program!!,
                        exercises = exercises,
                        round = step.round,
                        exerciseIndex = step.exerciseIndex,
                        remainingSeconds = remainingSeconds,
                        totalSeconds = totalSeconds,
                        phaseColor = cs.secondary,
                        phaseLabel = stringResource(R.string.interval_round_rest_label),
                        exerciseName = null,
                        nextPreview = firstExercise?.let {
                            NextPreviewInfo(
                                label = stringResource(R.string.interval_next_round),
                                exerciseName = it.name,
                                description = it.description
                            )
                        },
                        roundCompleteMessage = stringResource(R.string.interval_round_complete_format, step.round),
                        isPaused = isPaused,
                        onPauseToggle = ::togglePause,
                        onStop = null,
                        onSkip = ::skipCurrentStep
                    )
                }
            }
        }

        is IntervalPhase.Complete -> {
            IntervalCompleteContent(
                program = program!!,
                exercises = exercises,
                completedRounds = currentPhase.completedRounds,
                completedExercisesInLastRound = currentPhase.completedExercisesInLastRound,
                isFullCompletion = currentPhase.isFullCompletion,
                onSave = { comment ->
                    scope.launch {
                        // The snapshot taken at the start; a resumed workout brings it from its checkpoint.
                        val p = program!!
                        val context = sessionContext ?: IntervalSessionContext(
                            p.id, p.name, p.workSeconds, p.restSeconds, p.rounds, p.roundRestSeconds,
                            exercises.map { IntervalExerciseSnapshot(it.exerciseId, it.name, it.description) }
                        )
                        val library = viewModel.exercises.value.associateBy { it.id }
                        val groups = viewModel.groups.value
                        viewModel.recordIntervalWorkout(
                            IntervalRun(
                                programId = context.programId,
                                programName = context.programName,
                                workSeconds = context.workSeconds,
                                restSeconds = context.restSeconds,
                                rounds = context.rounds,
                                roundRestSeconds = context.roundRestSeconds,
                                exercises = context.exercises.map { snapshot ->
                                    val exercise = library[snapshot.exerciseId]
                                    IntervalRunExercise(
                                        // An exercise deleted since the start keeps only its snapshot.
                                        exerciseId = exercise?.id,
                                        name = snapshot.name,
                                        kind = if (exercise?.type == "Isometric") ExerciseKind.ISOMETRIC else ExerciseKind.DYNAMIC,
                                        laterality = if (exercise?.laterality == "Unilateral") Laterality.UNILATERAL else Laterality.BILATERAL,
                                        groupId = exercise?.group?.let { name -> groups.find { it.name == name }?.id },
                                        groupName = exercise?.group,
                                    )
                                },
                                completedRounds = currentPhase.completedRounds,
                                completedExercisesInLastRound = currentPhase.completedExercisesInLastRound,
                                comment = comment,
                                startedAtWallMillis = context.startedAtWallMillis,
                                savedAtWallMillis = System.currentTimeMillis()
                            )
                        )
                        onComplete()
                    }
                }
            )
        }
    }
}

// ========================================
// Confirm Screen
// ========================================

@Composable
private fun IntervalConfirmContent(
    program: IntervalProgram,
    exercises: List<IntervalExerciseInfo>,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val exerciseCount = exercises.size
    val perRoundSeconds = exerciseCount * program.workSeconds +
            (exerciseCount - 1).coerceAtLeast(0) * program.restSeconds
    val totalSeconds = perRoundSeconds * program.rounds +
            program.roundRestSeconds * (program.rounds - 1).coerceAtLeast(0)
    val totalMinutes = totalSeconds / 60
    val totalRemainSeconds = totalSeconds % 60

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = cs.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            AppIcons.Back,
                            contentDescription = stringResource(R.string.back),
                            tint = cs.onSurface
                        )
                    }
                    Text(
                        text = program.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    // The large Start button at the bottom is the one way to begin.
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Program name
            // Settings summary
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ConfirmSettingRow(
                            label = stringResource(R.string.interval_work_seconds),
                            value = stringResource(R.string.value_with_unit, program.workSeconds, stringResource(R.string.interval_seconds_suffix))
                        )
                        ConfirmSettingRow(
                            label = stringResource(R.string.interval_rest_seconds),
                            value = stringResource(R.string.value_with_unit, program.restSeconds, stringResource(R.string.interval_seconds_suffix))
                        )
                        ConfirmSettingRow(
                            label = stringResource(R.string.interval_rounds),
                            value = stringResource(R.string.value_with_unit, program.rounds, stringResource(R.string.interval_rounds_suffix))
                        )
                        if (program.roundRestSeconds > 0) {
                            ConfirmSettingRow(
                                label = stringResource(R.string.interval_round_rest_seconds),
                                value = stringResource(R.string.value_with_unit, program.roundRestSeconds, stringResource(R.string.interval_seconds_suffix))
                            )
                        }
                        HorizontalDivider(
                            color = cs.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        ConfirmSettingRow(
                            label = stringResource(R.string.interval_total_time),
                            value = stringResource(
                                R.string.interval_total_time_format,
                                totalMinutes,
                                totalRemainSeconds
                            ),
                            isBold = true
                        )
                    }
                }
            }

            // Exercise list
            item {
                Text(
                    text = stringResource(R.string.interval_exercises),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            itemsIndexed(exercises) { index, exercise ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = cs.tertiary,
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.Center
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exercise.name,
                                fontSize = 15.sp,
                                color = cs.onSurface
                            )
                            if (!exercise.description.isNullOrBlank()) {
                                Text(
                                    text = exercise.description,
                                    fontSize = 12.sp,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Start button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.tertiary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.interval_start_workout),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmSettingRow(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = cs.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = cs.onSurface
        )
    }
}

// ========================================
// Prepare Countdown Screen
// ========================================

@Composable
private fun IntervalPrepareContent(
    exercises: List<IntervalExerciseInfo>,
    remainingSeconds: Int,
    totalSeconds: Int,
    isPaused: Boolean,
    onPauseToggle: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val progress = remainingSeconds.toFloat() / totalSeconds
    val firstExercise = exercises.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // The same parts as single and program runs: status, then the dial (tap to pause).
        WorkoutStatus(stringResource(R.string.interval_get_ready), WorkoutTone.prepare)
        Spacer(modifier = Modifier.height(Spacing.xl))
        TimerDial(
            progress = progress,
            value = "$remainingSeconds",
            accent = WorkoutTone.prepare,
            paused = isPaused,
            onToggle = onPauseToggle,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Bottom: first exercise preview
        if (firstExercise != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.interval_next),
                        style = MaterialTheme.typography.labelLarge,
                        color = cs.secondary
                    )
                    Text(
                        text = firstExercise.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = cs.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (!firstExercise.description.isNullOrBlank()) {
                        Text(
                            text = firstExercise.description,
                            fontSize = 13.sp,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ========================================
// Timer Screen (Work / Rest / RoundRest)
// ========================================

private data class NextPreviewInfo(
    val label: String,
    val exerciseName: String,
    val description: String?
)

@Composable
private fun IntervalTimerContent(
    program: IntervalProgram,
    exercises: List<IntervalExerciseInfo>,
    round: Int,
    exerciseIndex: Int,
    remainingSeconds: Int,
    totalSeconds: Int,
    phaseColor: Color,
    phaseLabel: String,
    exerciseName: String?,
    nextPreview: NextPreviewInfo?,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onStop: (() -> Unit)?,
    onSkip: (() -> Unit)?,
    roundCompleteMessage: String? = null
) {
    val cs = MaterialTheme.colorScheme
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The exercise and where the run is, with one segment per round.
        val position = if (exerciseName != null) {
            stringResource(R.string.interval_exercise_format, exerciseIndex + 1, exercises.size) +
                " · " + stringResource(R.string.interval_round_format, round, program.rounds)
        } else {
            roundCompleteMessage ?: stringResource(R.string.interval_round_format, round, program.rounds)
        }
        WorkoutHeader(
            exerciseName = exerciseName ?: phaseLabel,
            setLabel = position,
            setNumber = round,
            totalSets = program.rounds,
            accent = phaseColor,
        )

        Spacer(modifier = Modifier.weight(1f))

        WorkoutStatus(phaseLabel, phaseColor)
        Spacer(modifier = Modifier.height(Spacing.l))
        TimerDial(
            progress = progress,
            value = "$remainingSeconds",
            accent = phaseColor,
            paused = isPaused,
            onToggle = onPauseToggle,
            // Smaller than elsewhere: this screen also shows what comes next and two actions.
            size = 220.dp,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Next exercise info
        val nextExIndex = exerciseIndex + 1
        val nextEx = if (exerciseName != null) {
            if (nextExIndex < exercises.size) exercises[nextExIndex]
            else if (round < program.rounds) exercises.firstOrNull()
            else null
        } else null

        if (nextPreview != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "${nextPreview.label}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = phaseColor
                    )
                    Text(
                        text = nextPreview.exerciseName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = cs.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (!nextPreview.description.isNullOrBlank()) {
                        Text(
                            text = nextPreview.description,
                            fontSize = 13.sp,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        } else if (nextEx != null) {
            Text(
                text = "${stringResource(R.string.interval_next)}: ${nextEx.name}",
                fontSize = 14.sp,
                color = cs.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control buttons - 2 rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stop
            if (onStop != null) {
                // Stopping ends the run early (it is saved), so it is visible but not loud.
                OutlinedButton(
                    onClick = onStop,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(text = stringResource(R.string.interval_stop), style = MaterialTheme.typography.titleMedium)
                }
            }

            // Skip is the quiet action, as in the other runs.
            if (onSkip != null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { WorkoutSkipButton(onSkip) }
            }
        }
    }
}

// ========================================
// Complete Screen
// ========================================

@Composable
private fun IntervalCompleteContent(
    program: IntervalProgram,
    exercises: List<IntervalExerciseInfo>,
    completedRounds: Int,
    completedExercisesInLastRound: Int,
    isFullCompletion: Boolean,
    onSave: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val statusColor = cs.tertiary
    var comment by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = cs.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFullCompletion) stringResource(R.string.interval_complete_title)
                        else stringResource(R.string.interval_ended_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status icon
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    imageVector = if (isFullCompletion) AppIcons.Check else AppIcons.Close,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(64.dp)
                )
            }

            // Program name
            item {
                Text(
                    text = program.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            // Completion stats
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isFullCompletion) {
                            Text(
                                text = stringResource(
                                    R.string.interval_full_complete_format,
                                    completedRounds,
                                    program.rounds
                                ),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    R.string.interval_partial_format,
                                    completedRounds,
                                    program.rounds,
                                    completedExercisesInLastRound,
                                    exercises.size
                                ),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                            Text(
                                text = stringResource(R.string.interval_partial_note),
                                fontSize = 14.sp,
                                color = cs.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(
                            color = cs.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Text(
                            text = stringResource(
                                R.string.interval_settings_format,
                                program.workSeconds,
                                exercises.size,
                                program.rounds
                            ),
                            fontSize = 13.sp,
                            color = cs.onSurfaceVariant
                        )

                    }
                }
            }

            // Exercise list with per-exercise round count
            item {
                Text(
                    text = stringResource(R.string.interval_exercises_done),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }

            itemsIndexed(exercises) { index, exercise ->
                val doneRounds = if (isFullCompletion) {
                    program.rounds
                } else if (index < completedExercisesInLastRound) {
                    completedRounds + 1
                } else {
                    completedRounds
                }
                val isComplete = doneRounds == program.rounds

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = exercise.name,
                        fontSize = 14.sp,
                        color = if (isComplete) cs.onSurface
                        else cs.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$doneRounds/${program.rounds}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isComplete) cs.tertiary
                        else cs.onSurfaceVariant
                    )
                }
            }

            // Comment input
            item {
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(stringResource(R.string.interval_comment_label)) },
                    placeholder = { Text(stringResource(R.string.interval_comment_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = cs.onSurface,
                        unfocusedTextColor = cs.onSurface,
                        focusedBorderColor = cs.tertiary,
                        unfocusedBorderColor = cs.onSurfaceVariant,
                        focusedLabelColor = cs.tertiary,
                        unfocusedLabelColor = cs.onSurfaceVariant,
                        cursorColor = cs.tertiary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Buttons
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onSave(comment) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = statusColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.interval_save_and_finish),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColorFor(statusColor)
                    )
                }
            }

        }
    }
}
