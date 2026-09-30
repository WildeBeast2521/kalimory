package io.github.gonbei774.calisthenicsmemory.ui.components.program

import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.CountDisplay
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.StepButton
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.TimerDial
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.WorkoutHeader
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.WorkoutPrimaryButton
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.WorkoutTone
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.setLabel
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.ui.components.common.PreviousTargetRow
import io.github.gonbei774.calisthenicsmemory.ui.components.common.RecordAdjustDialog
import io.github.gonbei774.calisthenicsmemory.ui.components.common.RepPaceIndicator
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import io.github.gonbei774.calisthenicsmemory.util.SoundPlayer
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.ui.components.rememberStepStopwatch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// タイマーOFF版 Dynamic専用: UIはTimer ONと統一、自動遷移なし
@Composable
internal fun ProgramExecutingStepDynamicManual(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    soundPlayer: SoundPlayer,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isCountSoundEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    onSetComplete: (Int) -> Unit,
    onAbort: () -> Unit,
    onRetry: () -> Unit,
    onOpenNavigation: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    // 状態管理
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 1レップの秒数（種目設定があればそれを使用、なければ5秒）
    val repDuration = (exercise.repDuration ?: 5).coerceAtLeast(1)

    // 経過時間はモノトニッククロックから計算（一時停止中は進まず、端数も保持）
    var announcedCount by remember(currentSetIndex) { mutableIntStateOf(0) }
    val stopwatch = rememberStepStopwatch(paused = effectivelyPaused) { sw ->
        val count = sw.elapsedSeconds / repDuration
        if (count > announcedCount) {
            val reachedTarget = announcedCount < currentSet.targetValue && count >= currentSet.targetValue
            announcedCount = count
            if (reachedTarget) {
                // 目標達成の音（1回だけ）
                if (isFlashEnabled) {
                    launch { flashController.flashSetComplete() }
                }
                soundPlayer.playSetComplete()
            } else if (isCountSoundEnabled) {
                // 途中のレップは短いビープ（設定ONの場合のみ）
                soundPlayer.playRep()
                if (isFlashEnabled) {
                    launch { flashController.flashShort() }
                }
            }
        }
    }
    val elapsedTime = stopwatch.elapsedSeconds
    val currentCount = elapsedTime / repDuration

    // 記録するレップ数（自動カウント）
    val recordValue = currentCount

    // レップ内の経過時間（カウントアップ）
    val repTimeElapsed = elapsedTime % repDuration

    // タイマー完了フラグ
    val isTimerComplete = currentCount >= currentSet.targetValue


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProgramSetHeader(session, exercise.name, currentSetIndex)

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
                weightTrackingEnabled = exercise.weightTrackingEnabled,
                distanceTrackingEnabled = exercise.distanceTrackingEnabled,
                assistanceTrackingEnabled = exercise.assistanceTrackingEnabled,
                initialWeightG = currentSet.weightG,
                initialDistanceCm = currentSet.distanceCm,
                initialAssistanceG = currentSet.assistanceG,
                previousWeightG = currentSet.previousWeightG,
                previousDistanceCm = currentSet.previousDistanceCm,
                previousAssistanceG = currentSet.previousAssistanceG,
                onConfirm = { adjusted, weightG, distanceCm, assistanceG ->
                    currentSet.weightG = weightG
                    currentSet.distanceCm = distanceCm
                    currentSet.assistanceG = assistanceG
                    showConfirm = false
                    onSetComplete(adjusted)
                },
                onDismiss = {
                    showConfirm = false
                    isPaused = false
                }
            )
        }

        NextExerciseText(session = session, currentSetIndex = currentSetIndex)
    }
}

// タイマーOFF版 Isometric専用: カウントダウンタイマー + 手動完了
@Composable
internal fun ProgramExecutingStepIsometricManual(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    soundPlayer: SoundPlayer,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isIntervalSoundEnabled: Boolean,
    intervalSeconds: Int,
    isNavigationOpen: Boolean = false,
    onSetComplete: (Int) -> Unit,
    onAbort: () -> Unit,
    onRetry: () -> Unit,
    onOpenNavigation: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    // 状態管理
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 経過時間はモノトニッククロックから計算（一時停止中は進まず、端数も保持）
    var announcedSecond by remember(currentSetIndex) { mutableIntStateOf(0) }
    // 目標達成時のビープを一度だけ鳴らすためのフラグ
    var hasPlayedCompletionBeep by remember(currentSetIndex) { mutableStateOf(false) }
    val stopwatch = rememberStepStopwatch(paused = effectivelyPaused) { sw ->
        val second = sw.elapsedSeconds
        if (second <= announcedSecond) return@rememberStepStopwatch
        // 一定間隔ごとにビープ音（目標達成前のみ、設定ONの場合）
        if (isIntervalSoundEnabled && intervalSeconds > 0) {
            val lastMultiple = second / intervalSeconds * intervalSeconds
            if (lastMultiple > announcedSecond && lastMultiple < currentSet.targetValue) {
                soundPlayer.playHoldTick()
                if (isFlashEnabled) {
                    launch { flashController.flashShort() }
                }
            }
        }
        announcedSecond = second
        // 目標時間達成時に終了アラーム（1回だけ）
        if (second >= currentSet.targetValue && !hasPlayedCompletionBeep) {
            hasPlayedCompletionBeep = true
            if (isFlashEnabled) {
                launch { flashController.flashSetComplete() }
            }
            soundPlayer.playSetComplete()
        }
    }
    val elapsedTime = stopwatch.elapsedSeconds

    // 記録する値（経過時間）
    val recordValue = elapsedTime

    // 残り時間（カウントダウン用）
    val remainingTime = (currentSet.targetValue - elapsedTime).coerceAtLeast(0)

    // プログレス（0〜1）
    val progress = if (currentSet.targetValue > 0) {
        remainingTime.toFloat() / currentSet.targetValue
    } else 0f

    // タイマー完了フラグ
    val isTimerComplete = remainingTime <= 0

    // 色（一時停止中=グレー、完了=緑、実行中=オレンジ）
    val activeColor = WorkoutTone.work



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProgramSetHeader(session, exercise.name, currentSetIndex)

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
                weightTrackingEnabled = exercise.weightTrackingEnabled,
                distanceTrackingEnabled = exercise.distanceTrackingEnabled,
                assistanceTrackingEnabled = exercise.assistanceTrackingEnabled,
                initialWeightG = currentSet.weightG,
                initialDistanceCm = currentSet.distanceCm,
                initialAssistanceG = currentSet.assistanceG,
                previousWeightG = currentSet.previousWeightG,
                previousDistanceCm = currentSet.previousDistanceCm,
                previousAssistanceG = currentSet.previousAssistanceG,
                onConfirm = { adjusted, weightG, distanceCm, assistanceG ->
                    currentSet.weightG = weightG
                    currentSet.distanceCm = distanceCm
                    currentSet.assistanceG = assistanceG
                    showConfirm = false
                    onSetComplete(adjusted)
                },
                onDismiss = {
                    showConfirm = false
                    isPaused = false
                }
            )
        }

        NextExerciseText(session = session, currentSetIndex = currentSetIndex)
    }
}

// タイマーON版 Isometric専用: カウントダウンタイマー + 自動遷移
@Composable
internal fun ProgramExecutingStepIsometricAuto(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    soundPlayer: SoundPlayer,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isIntervalSoundEnabled: Boolean,
    intervalSeconds: Int,
    isNavigationOpen: Boolean = false,
    onSetComplete: (Int) -> Unit,
    onAbort: () -> Unit,
    onRetry: () -> Unit,
    onOpenNavigation: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    // 状態管理
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 経過時間はモノトニッククロックから計算（一時停止中は進まず、端数も保持）
    var announcedSecond by remember(currentSetIndex) { mutableIntStateOf(0) }
    var finished by remember(currentSetIndex) { mutableStateOf(false) }
    val stopwatch = rememberStepStopwatch(paused = effectivelyPaused) { sw ->
        if (finished) return@rememberStepStopwatch
        val second = sw.elapsedSeconds
        if (second <= announcedSecond) return@rememberStepStopwatch
        // 一定間隔ごとにビープ音（目標達成前のみ、設定ONの場合）
        if (isIntervalSoundEnabled && intervalSeconds > 0) {
            val lastMultiple = second / intervalSeconds * intervalSeconds
            if (lastMultiple > announcedSecond && lastMultiple < currentSet.targetValue) {
                soundPlayer.playHoldTick()
                if (isFlashEnabled) {
                    launch { flashController.flashShort() }
                }
            }
        }
        announcedSecond = second
        // 目標時間達成時に終了アラーム + 自動遷移
        if (second >= currentSet.targetValue) {
            finished = true
            if (isFlashEnabled) {
                launch { flashController.flashSetComplete() }
            }
            soundPlayer.playSetComplete()
            onSetComplete(currentSet.targetValue)
        }
    }
    // 目標到達で停止（従来どおり目標値を超えて数えない）
    val elapsedTime = stopwatch.elapsedSeconds.coerceAtMost(currentSet.targetValue)

    // 記録する値（経過時間）
    val recordValue = elapsedTime

    // 残り時間（カウントダウン用）
    val remainingTime = (currentSet.targetValue - elapsedTime).coerceAtLeast(0)

    // プログレス（0〜1）
    val progress = if (currentSet.targetValue > 0) {
        remainingTime.toFloat() / currentSet.targetValue
    } else 0f

    // 色（一時停止中=グレー、実行中=オレンジ）
    val activeColor = WorkoutTone.work


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProgramSetHeader(session, exercise.name, currentSetIndex)

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
            onClick = { onSetComplete(recordValue) },
        )

        NextExerciseText(session = session, currentSetIndex = currentSetIndex)
    }
}

// タイマーON版 Dynamic専用: 自動レップカウント + UI統一
@Composable
internal fun ProgramExecutingStepDynamicAuto(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    soundPlayer: SoundPlayer,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isCountSoundEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    onSetComplete: (Int) -> Unit,
    onAbort: () -> Unit,
    onRetry: () -> Unit,
    onOpenNavigation: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    // 状態管理
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 1レップの秒数（種目設定があればそれを使用、なければ5秒）
    val repDuration = (exercise.repDuration ?: 5).coerceAtLeast(1)

    // 経過時間はモノトニッククロックから計算（一時停止中は進まず、端数も保持）
    var announcedCount by remember(currentSetIndex) { mutableIntStateOf(0) }
    var finished by remember(currentSetIndex) { mutableStateOf(false) }
    val stopwatch = rememberStepStopwatch(paused = effectivelyPaused) { sw ->
        if (finished) return@rememberStepStopwatch
        val count = sw.elapsedSeconds / repDuration
        if (count > announcedCount) {
            announcedCount = count
            // 目標達成時に自動遷移
            if (count >= currentSet.targetValue) {
                finished = true
                if (isFlashEnabled) {
                    launch { flashController.flashSetComplete() }
                }
                soundPlayer.playSetComplete()
                onSetComplete(maxOf(currentSet.targetValue, 1))
            } else if (isCountSoundEnabled) {
                // 途中のレップは短いビープ（設定ONの場合のみ）
                soundPlayer.playRep()
                if (isFlashEnabled) {
                    launch { flashController.flashShort() }
                }
            }
        }
    }
    val elapsedTime = stopwatch.elapsedSeconds
    val currentCount = elapsedTime / repDuration

    // 記録するレップ数（自動カウント）
    val recordValue = currentCount

    // レップ内の経過時間（カウントアップ）
    val repTimeElapsed = elapsedTime % repDuration

    // タイマー完了フラグ
    val isTimerComplete = currentCount >= currentSet.targetValue


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProgramSetHeader(session, exercise.name, currentSetIndex)

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
            onClick = { onSetComplete(recordValue) },
        )

        NextExerciseText(session = session, currentSetIndex = currentSetIndex)
    }
}

// レップ数数え上げOFF版 Dynamic専用: タイマーなし、静的カウンター
@Composable
internal fun ProgramExecutingStepDynamicSimple(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    onSetComplete: (Int) -> Unit,
    onAbort: () -> Unit,
    onOpenNavigation: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    // 状態管理: 初期値は目標回数
    var repsCount by remember(currentSetIndex) { mutableIntStateOf(currentSet.targetValue) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProgramSetHeader(session, exercise.name, currentSetIndex)

        Spacer(modifier = Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.l),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepButton(AppIcons.Remove, stringResource(R.string.workout_count_decrease), enabled = repsCount > 0) { if (repsCount > 0) repsCount-- }
            CountDisplay(value = repsCount, unit = null, reached = repsCount >= currentSet.targetValue, paused = false, onToggle = null, compact = true)
            StepButton(AppIcons.Add, stringResource(R.string.workout_count_increase)) { repsCount++ }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 前回 ｜ 目標
        PreviousTargetRow(
            previous = currentSet.previousValue,
            target = currentSet.targetValue,
            unit = stringResource(R.string.unit_reps)
        )

        Spacer(modifier = Modifier.height(Spacing.l))

        // 完了ボタン
        WorkoutPrimaryButton(
            text = stringResource(R.string.complete_with_reps, repsCount),
            onClick = { onSetComplete(repsCount) },
        )

        NextExerciseText(session = session, currentSetIndex = currentSetIndex)
    }
}

/**
 * The shared workout header for a program set: the exercise, "Set 2/6" within that exercise
 * (and round, for loops), with one segment per set of the exercise.
 */
@Composable
internal fun ProgramSetHeader(session: ProgramExecutionSession, exerciseName: String, currentSetIndex: Int) {
    val currentSet = session.sets[currentSetIndex]
    val exerciseSets = session.sets
        .filter { it.exerciseIndex == currentSet.exerciseIndex && it.roundNumber == currentSet.roundNumber }
        .map { it.setNumber }
        .distinct()
    WorkoutHeader(
        exerciseName = exerciseName,
        setLabel = setLabel(currentSet.setNumber, exerciseSets.size, currentSet.side),
        setNumber = currentSet.setNumber,
        totalSets = exerciseSets.size,
        accent = WorkoutTone.work,
        caption = if (currentSet.loopId != null && currentSet.totalRounds > 1) {
            stringResource(R.string.loop_round_current, currentSet.roundNumber, currentSet.totalRounds)
        } else null,
    )
}
