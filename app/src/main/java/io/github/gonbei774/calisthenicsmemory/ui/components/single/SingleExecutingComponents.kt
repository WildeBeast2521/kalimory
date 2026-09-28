package io.github.gonbei774.calisthenicsmemory.ui.components.single

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.components.common.PreviousTargetRow
import io.github.gonbei774.calisthenicsmemory.ui.components.common.RecordAdjustDialog
import io.github.gonbei774.calisthenicsmemory.ui.components.common.RepPaceIndicator
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.CountDisplay
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.StepButton
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.TimerDial
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.WorkoutHeader
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.WorkoutPrimaryButton
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.WorkoutTone
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.setLabel
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.screens.NextSetText
import io.github.gonbei774.calisthenicsmemory.ui.screens.WorkoutSession
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import io.github.gonbei774.calisthenicsmemory.util.SoundPlayer
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.components.rememberStepStopwatch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dynamic + タイマーOFF + レップカウントON: 手動完了ボタンで遷移
 */
@Composable
fun SingleExecutingStepDynamicManual(
    session: WorkoutSession,
    currentSetIndex: Int,
    soundPlayer: SoundPlayer,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isCountSoundEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    onSetComplete: (WorkoutSession) -> Unit,
    onSkip: (WorkoutSession) -> Unit,
    onAbort: (WorkoutSession) -> Unit,
    onRetry: () -> Unit
) {
    val currentSet = session.sets.getOrNull(currentSetIndex) ?: return
    val repDuration = (session.repDuration ?: 5).coerceAtLeast(1)

    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 経過時間はモノトニッククロックから計算（一時停止中は進まず、端数も保持）
    var announcedCount by remember(currentSetIndex) { mutableIntStateOf(0) }
    val stopwatch = rememberStepStopwatch(paused = effectivelyPaused, key = currentSetIndex) { sw ->
        val count = sw.elapsedSeconds / repDuration
        if (count > announcedCount) {
            val reachedTarget = announcedCount < currentSet.targetValue && count >= currentSet.targetValue
            announcedCount = count
            if (reachedTarget) {
                if (isFlashEnabled) {
                    launch { flashController.flashSetComplete() }
                }
                soundPlayer.playSetComplete()
            } else if (isCountSoundEnabled) {
                soundPlayer.playBeep()
                if (isFlashEnabled) {
                    launch { flashController.flashShort() }
                }
            }
        }
    }
    val elapsedTime = stopwatch.elapsedSeconds
    val currentCount = elapsedTime / repDuration

    val recordValue = currentCount
    val repTimeElapsed = elapsedTime % repDuration
    val isTimerComplete = currentCount >= currentSet.targetValue


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WorkoutHeader(
            exerciseName = session.exercise.name,
            setLabel = setLabel(currentSet.setNumber, session.totalSets, currentSet.side),
            setNumber = currentSet.setNumber,
            totalSets = session.totalSets,
            accent = WorkoutTone.work,
        )

        Spacer(modifier = Modifier.weight(1f))

        CountDisplay(
            value = recordValue,
            unit = stringResource(R.string.unit_reps),
            reached = isTimerComplete,
            paused = effectivelyPaused,
            onToggle = { isPaused = !isPaused },
        )

        Spacer(modifier = Modifier.height(Spacing.l))

        // レップ内ペース表示（ドット or 横バー）
        RepPaceIndicator(
            repDuration = repDuration,
            secondsInRep = repTimeElapsed,
            paused = effectivelyPaused,
            modifier = Modifier.fillMaxWidth(0.7f)
        )

        Spacer(modifier = Modifier.weight(1f))

        // 前回 ｜ 目標
        PreviousTargetRow(
            previous = currentSet.previousValue,
            target = currentSet.targetValue,
            unit = stringResource(R.string.unit_reps)
        )

        Spacer(modifier = Modifier.height(Spacing.l))

        // 完了ボタン（押下で記録確認シートを表示）
        var showConfirm by remember(currentSetIndex) { mutableStateOf(false) }
        WorkoutPrimaryButton(
            text = stringResource(R.string.complete_with_reps, recordValue),
            onClick = {
                isPaused = true
                showConfirm = true
            },
        )

        if (showConfirm) {
            RecordAdjustDialog(
                initialValue = recordValue,
                unit = stringResource(R.string.unit_reps),
                targetValue = currentSet.targetValue,
                weightTrackingEnabled = session.exercise.weightTrackingEnabled,
                distanceTrackingEnabled = session.exercise.distanceTrackingEnabled,
                assistanceTrackingEnabled = session.exercise.assistanceTrackingEnabled,
                initialWeightG = currentSet.weightG,
                initialDistanceCm = currentSet.distanceCm,
                initialAssistanceG = currentSet.assistanceG,
                onConfirm = { adjusted, weightG, distanceCm, assistanceG ->
                    currentSet.actualValue = adjusted
                    currentSet.weightG = weightG
                    currentSet.distanceCm = distanceCm
                    currentSet.assistanceG = assistanceG
                    currentSet.isCompleted = true
                    showConfirm = false
                    onSetComplete(session)
                },
                onDismiss = {
                    showConfirm = false
                    isPaused = false
                }
            )
        }

        NextSetText(session = session, currentSetIndex = currentSetIndex)
    }
}

/**
 * Dynamic + タイマーON: 目標達成時に自動遷移（早期完了ボタンあり）
 */
@Composable
fun SingleExecutingStepDynamicAuto(
    session: WorkoutSession,
    currentSetIndex: Int,
    soundPlayer: SoundPlayer,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isCountSoundEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    onSetComplete: (WorkoutSession) -> Unit,
    onSkip: (WorkoutSession) -> Unit,
    onAbort: (WorkoutSession) -> Unit,
    onRetry: () -> Unit
) {
    val currentSet = session.sets.getOrNull(currentSetIndex) ?: return
    val repDuration = (session.repDuration ?: 5).coerceAtLeast(1)

    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 経過時間はモノトニッククロックから計算（一時停止中は進まず、端数も保持）
    var announcedCount by remember(currentSetIndex) { mutableIntStateOf(0) }
    var finished by remember(currentSetIndex) { mutableStateOf(false) }
    val stopwatch = rememberStepStopwatch(paused = effectivelyPaused, key = currentSetIndex) { sw ->
        if (finished) return@rememberStepStopwatch
        val count = sw.elapsedSeconds / repDuration
        if (count > announcedCount) {
            announcedCount = count
            if (count >= currentSet.targetValue) {
                finished = true
                if (isFlashEnabled) {
                    launch { flashController.flashSetComplete() }
                }
                soundPlayer.playSetComplete()
                currentSet.actualValue = maxOf(currentSet.targetValue, 1)
                currentSet.isCompleted = true
                onSetComplete(session)
            } else if (isCountSoundEnabled) {
                soundPlayer.playBeep()
                if (isFlashEnabled) {
                    launch { flashController.flashShort() }
                }
            }
        }
    }
    val elapsedTime = stopwatch.elapsedSeconds
    val currentCount = elapsedTime / repDuration

    val recordValue = currentCount
    val repTimeElapsed = elapsedTime % repDuration
    val isTimerComplete = currentCount >= currentSet.targetValue


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WorkoutHeader(
            exerciseName = session.exercise.name,
            setLabel = setLabel(currentSet.setNumber, session.totalSets, currentSet.side),
            setNumber = currentSet.setNumber,
            totalSets = session.totalSets,
            accent = WorkoutTone.work,
        )

        Spacer(modifier = Modifier.weight(1f))

        CountDisplay(
            value = recordValue,
            unit = stringResource(R.string.unit_reps),
            reached = isTimerComplete,
            paused = effectivelyPaused,
            onToggle = { isPaused = !isPaused },
        )

        Spacer(modifier = Modifier.height(Spacing.l))

        // レップ内ペース表示（ドット or 横バー）
        RepPaceIndicator(
            repDuration = repDuration,
            secondsInRep = repTimeElapsed,
            paused = effectivelyPaused,
            modifier = Modifier.fillMaxWidth(0.7f)
        )

        Spacer(modifier = Modifier.weight(1f))

        // 前回 ｜ 目標
        PreviousTargetRow(
            previous = currentSet.previousValue,
            target = currentSet.targetValue,
            unit = stringResource(R.string.unit_reps)
        )

        Spacer(modifier = Modifier.height(Spacing.l))

        // 完了ボタン（早期完了用）
        WorkoutPrimaryButton(
            text = stringResource(R.string.complete_with_reps, recordValue),
            onClick = {
                currentSet.actualValue = recordValue
                currentSet.isCompleted = true
                onSetComplete(session)
            },
        )

        NextSetText(session = session, currentSetIndex = currentSetIndex)
    }
}

/**
 * Dynamic + レップカウントOFF: シンプルなカウンターUI
 */
@Composable
fun SingleExecutingStepDynamicSimple(
    session: WorkoutSession,
    currentSetIndex: Int,
    soundPlayer: SoundPlayer,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    onSetComplete: (WorkoutSession) -> Unit,
    onSkip: (WorkoutSession) -> Unit,
    onAbort: (WorkoutSession) -> Unit,
    onRetry: () -> Unit
) {
    val currentSet = session.sets.getOrNull(currentSetIndex) ?: return

    var reps by remember(currentSetIndex) { mutableIntStateOf(currentSet.targetValue) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WorkoutHeader(
            exerciseName = session.exercise.name,
            setLabel = setLabel(currentSet.setNumber, session.totalSets, currentSet.side),
            setNumber = currentSet.setNumber,
            totalSets = session.totalSets,
            accent = WorkoutTone.work,
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.l),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepButton(AppIcons.Remove, stringResource(R.string.workout_count_decrease), enabled = reps > 0) { if (reps > 0) reps-- }
            CountDisplay(value = reps, unit = null, reached = reps >= currentSet.targetValue, paused = false, onToggle = null, compact = true)
            StepButton(AppIcons.Add, stringResource(R.string.workout_count_increase)) { reps++ }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 前回 ｜ 目標
        PreviousTargetRow(
            previous = currentSet.previousValue,
            target = currentSet.targetValue,
            unit = stringResource(R.string.unit_reps)
        )

        Spacer(modifier = Modifier.height(Spacing.l))

        WorkoutPrimaryButton(
            text = stringResource(R.string.complete_with_reps, reps),
            onClick = {
                if (isFlashEnabled) {
                    scope.launch { flashController.flashSetComplete() }
                }
                currentSet.actualValue = reps
                currentSet.isCompleted = true
                onSetComplete(session)
            },
        )

        NextSetText(session = session, currentSetIndex = currentSetIndex)
    }
}

/**
 * Isometric + タイマーOFF: カウントダウン + 手動完了
 */
@Composable
fun SingleExecutingStepIsometricManual(
    session: WorkoutSession,
    currentSetIndex: Int,
    soundPlayer: SoundPlayer,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isIntervalSoundEnabled: Boolean,
    intervalSeconds: Int,
    isNavigationOpen: Boolean = false,
    onSetComplete: (WorkoutSession) -> Unit,
    onSkip: (WorkoutSession) -> Unit,
    onAbort: (WorkoutSession) -> Unit,
    onRetry: () -> Unit
) {
    val currentSet = session.sets.getOrNull(currentSetIndex) ?: return

    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 経過時間はモノトニッククロックから計算（一時停止中は進まず、端数も保持）
    var announcedSecond by remember(currentSetIndex) { mutableIntStateOf(0) }
    var hasPlayedCompletionBeep by remember(currentSetIndex) { mutableStateOf(false) }
    val stopwatch = rememberStepStopwatch(paused = effectivelyPaused, key = currentSetIndex) { sw ->
        val second = sw.elapsedSeconds
        if (second <= announcedSecond) return@rememberStepStopwatch
        // 一定間隔ごとにビープ音（目標達成前のみ、設定ONの場合）
        if (isIntervalSoundEnabled && intervalSeconds > 0) {
            val lastMultiple = second / intervalSeconds * intervalSeconds
            if (lastMultiple > announcedSecond && lastMultiple < currentSet.targetValue) {
                soundPlayer.playBeep()
                if (isFlashEnabled) {
                    launch { flashController.flashShort() }
                }
            }
        }
        announcedSecond = second
        if (second >= currentSet.targetValue && !hasPlayedCompletionBeep) {
            hasPlayedCompletionBeep = true
            if (isFlashEnabled) {
                launch { flashController.flashSetComplete() }
            }
            soundPlayer.playSetComplete()
        }
    }
    val elapsedTime = stopwatch.elapsedSeconds

    val recordValue = elapsedTime
    val remainingTime = (currentSet.targetValue - elapsedTime).coerceAtLeast(0)
    val progress = if (currentSet.targetValue > 0) remainingTime.toFloat() / currentSet.targetValue else 0f
    val isTimerComplete = elapsedTime >= currentSet.targetValue

    val activeColor = WorkoutTone.work


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WorkoutHeader(
            exerciseName = session.exercise.name,
            setLabel = setLabel(currentSet.setNumber, session.totalSets, currentSet.side),
            setNumber = currentSet.setNumber,
            totalSets = session.totalSets,
            accent = WorkoutTone.work,
        )

        Spacer(modifier = Modifier.weight(1f))

        TimerDial(
            progress = progress,
            value = "$remainingTime",
            accent = activeColor,
            paused = effectivelyPaused,
            onToggle = { isPaused = !isPaused },
        )

        Spacer(modifier = Modifier.weight(1f))

        // 前回 ｜ 目標
        PreviousTargetRow(
            previous = currentSet.previousValue,
            target = currentSet.targetValue,
            unit = stringResource(R.string.unit_seconds_short)
        )

        Spacer(modifier = Modifier.height(Spacing.l))

        // 完了ボタン（押下で記録確認シートを表示）
        var showConfirm by remember(currentSetIndex) { mutableStateOf(false) }
        WorkoutPrimaryButton(
            text = stringResource(R.string.complete_with_time, recordValue),
            onClick = {
                isPaused = true
                showConfirm = true
            },
        )

        if (showConfirm) {
            RecordAdjustDialog(
                initialValue = recordValue,
                unit = stringResource(R.string.unit_seconds_short),
                targetValue = currentSet.targetValue,
                weightTrackingEnabled = session.exercise.weightTrackingEnabled,
                distanceTrackingEnabled = session.exercise.distanceTrackingEnabled,
                assistanceTrackingEnabled = session.exercise.assistanceTrackingEnabled,
                initialWeightG = currentSet.weightG,
                initialDistanceCm = currentSet.distanceCm,
                initialAssistanceG = currentSet.assistanceG,
                onConfirm = { adjusted, weightG, distanceCm, assistanceG ->
                    currentSet.actualValue = adjusted
                    currentSet.weightG = weightG
                    currentSet.distanceCm = distanceCm
                    currentSet.assistanceG = assistanceG
                    currentSet.isCompleted = true
                    showConfirm = false
                    onSetComplete(session)
                },
                onDismiss = {
                    showConfirm = false
                    isPaused = false
                }
            )
        }

        NextSetText(session = session, currentSetIndex = currentSetIndex)
    }
}

/**
 * Isometric + タイマーON: 目標達成時に自動遷移（早期完了ボタンあり）
 */
@Composable
fun SingleExecutingStepIsometricAuto(
    session: WorkoutSession,
    currentSetIndex: Int,
    soundPlayer: SoundPlayer,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isIntervalSoundEnabled: Boolean,
    intervalSeconds: Int,
    isNavigationOpen: Boolean = false,
    onSetComplete: (WorkoutSession) -> Unit,
    onSkip: (WorkoutSession) -> Unit,
    onAbort: (WorkoutSession) -> Unit,
    onRetry: () -> Unit
) {
    val currentSet = session.sets.getOrNull(currentSetIndex) ?: return

    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 経過時間はモノトニッククロックから計算（一時停止中は進まず、端数も保持）
    var announcedSecond by remember(currentSetIndex) { mutableIntStateOf(0) }
    var finished by remember(currentSetIndex) { mutableStateOf(false) }
    val stopwatch = rememberStepStopwatch(paused = effectivelyPaused, key = currentSetIndex) { sw ->
        if (finished) return@rememberStepStopwatch
        val second = sw.elapsedSeconds
        if (second <= announcedSecond) return@rememberStepStopwatch
        // 一定間隔ごとにビープ音（目標達成前のみ、設定ONの場合）
        if (isIntervalSoundEnabled && intervalSeconds > 0) {
            val lastMultiple = second / intervalSeconds * intervalSeconds
            if (lastMultiple > announcedSecond && lastMultiple < currentSet.targetValue) {
                soundPlayer.playBeep()
                if (isFlashEnabled) {
                    launch { flashController.flashShort() }
                }
            }
        }
        announcedSecond = second
        if (second >= currentSet.targetValue) {
            finished = true
            if (isFlashEnabled) {
                launch { flashController.flashSetComplete() }
            }
            soundPlayer.playSetComplete()
            currentSet.actualValue = currentSet.targetValue
            currentSet.isCompleted = true
            onSetComplete(session)
        }
    }
    val elapsedTime = stopwatch.elapsedSeconds.coerceAtMost(currentSet.targetValue)

    val recordValue = elapsedTime
    val remainingTime = (currentSet.targetValue - elapsedTime).coerceAtLeast(0)
    val progress = if (currentSet.targetValue > 0) remainingTime.toFloat() / currentSet.targetValue else 0f

    val activeColor = WorkoutTone.work


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WorkoutHeader(
            exerciseName = session.exercise.name,
            setLabel = setLabel(currentSet.setNumber, session.totalSets, currentSet.side),
            setNumber = currentSet.setNumber,
            totalSets = session.totalSets,
            accent = WorkoutTone.work,
        )

        Spacer(modifier = Modifier.weight(1f))

        TimerDial(
            progress = progress,
            value = "$remainingTime",
            accent = activeColor,
            paused = effectivelyPaused,
            onToggle = { isPaused = !isPaused },
        )

        Spacer(modifier = Modifier.weight(1f))

        // 前回 ｜ 目標
        PreviousTargetRow(
            previous = currentSet.previousValue,
            target = currentSet.targetValue,
            unit = stringResource(R.string.unit_seconds_short)
        )

        Spacer(modifier = Modifier.height(Spacing.l))

        // 完了ボタン（早期完了用）
        WorkoutPrimaryButton(
            text = stringResource(R.string.complete_with_time, recordValue),
            onClick = {
                currentSet.actualValue = recordValue
                currentSet.isCompleted = true
                onSetComplete(session)
            },
        )

        NextSetText(session = session, currentSetIndex = currentSetIndex)
    }
}