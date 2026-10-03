package io.github.gonbei774.calisthenicsmemory.ui.components.program

import io.github.gonbei774.calisthenicsmemory.workout.restDialProgress
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.RestToolbar
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.SetDoneBadge
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.TimerDial
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.WorkoutSkipButton
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.WorkoutStatus
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import io.github.gonbei774.calisthenicsmemory.util.SoundPlayer
import io.github.gonbei774.calisthenicsmemory.ui.components.countdownSeconds
import io.github.gonbei774.calisthenicsmemory.ui.components.rememberStepStopwatch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ProgramStartIntervalStep(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    startCountdownSeconds: Int,
    soundPlayer: SoundPlayer,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    nextSetIndexOverride: Int? = null,  // Redoモード時など、次のセットが+1でない場合に使用
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val currentSet = session.sets[currentSetIndex]
    val (_, exercise) = session.exercises[currentSet.exerciseIndex]

    var isPaused by remember { mutableStateOf(false) }
    val effectivelyPaused = isPaused || isNavigationOpen
    val totalMillis = startCountdownSeconds * 1_000L

    // 残り時間はモノトニッククロックから計算（一時停止中は進まず、端数も保持）
    var lastShown by remember { mutableIntStateOf(startCountdownSeconds) }
    var finished by remember { mutableStateOf(false) }
    val stopwatch = rememberStepStopwatch(paused = effectivelyPaused) { sw ->
        if (finished) return@rememberStepStopwatch
        val shown = countdownSeconds(totalMillis - sw.elapsedMillis)
        if (shown < lastShown) {
            lastShown = shown
            if (shown in 1..3) {
                soundPlayer.playCountdown()
                if (isFlashEnabled) {
                    launch { flashController.flashShort() }
                }
            }
        }
        if (shown <= 0) {
            finished = true
            soundPlayer.playStartCue()
            if (isFlashEnabled) {
                launch { flashController.flashComplete() }
            }
            launch {
                delay(300L)
                onComplete()
            }
        }
    }
    val remainingMillis = totalMillis - stopwatch.elapsedMillis
    val remainingTime = countdownSeconds(remainingMillis)
    val progress = if (totalMillis > 0) remainingMillis.coerceAtLeast(0).toFloat() / totalMillis else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProgramSetHeader(session, exercise.name, currentSetIndex)

        Spacer(modifier = Modifier.weight(1f))

        WorkoutStatus(stringResource(R.string.get_ready), WorkoutTone.prepare)

        Spacer(modifier = Modifier.height(Spacing.xl))

        TimerDial(
            progress = progress,
            value = "$remainingTime",
            accent = WorkoutTone.prepare,
            paused = effectivelyPaused,
            onToggle = { isPaused = !isPaused },
        )

        Spacer(modifier = Modifier.weight(1f))

        WorkoutSkipButton(onSkip)
    }
}

@Composable
internal fun ProgramIntervalStep(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    soundPlayer: SoundPlayer,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    nextSetIndexOverride: Int? = null,  // Redoモード時など、次のセットが+1でない場合に使用
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val currentSet = session.sets[currentSetIndex]
    // 次のセット：オーバーライドがあればそれを使用、なければ+1
    val nextSetIndex = nextSetIndexOverride ?: (currentSetIndex + 1)
    val nextSet = session.sets.getOrNull(nextSetIndex)

    // ループ間休憩がある場合は追加
    val totalInterval = currentSet.intervalSeconds + currentSet.loopRestAfterSeconds
    val hasLoopRest = currentSet.loopRestAfterSeconds > 0

    var isRunning by remember { mutableStateOf(true) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyRunning = isRunning && !isNavigationOpen

    // 残り時間はモノトニッククロックから計算。±10秒は extraMillis で調整する
    var extraMillis by remember { mutableLongStateOf(0L) }
    var lastShown by remember { mutableIntStateOf(totalInterval) }
    var finished by remember { mutableStateOf(false) }
    val stopwatch = rememberStepStopwatch(paused = !effectivelyRunning) { sw ->
        if (finished) return@rememberStepStopwatch
        val shown = countdownSeconds(totalInterval * 1_000L + extraMillis - sw.elapsedMillis)
        if (shown < lastShown) {
            lastShown = shown
            if (shown in 1..3) {
                soundPlayer.playCountdown()
                if (isFlashEnabled) {
                    launch { flashController.flashShort() }
                }
            }
        }
        // 一時停止中に -10 で 0 になった場合は、従来どおり再開後に完了する
        if (shown <= 0 && !sw.isPaused) {
            finished = true
            soundPlayer.playStartCue()
            if (isFlashEnabled) {
                launch { flashController.flashComplete() }
            }
            launch {
                delay(300L)
                onComplete()
            }
        }
    }
    val remainingMillis = totalInterval * 1_000L + extraMillis - stopwatch.elapsedMillis
    val remainingTime = countdownSeconds(remainingMillis)
    val progress = restDialProgress(totalInterval * 1_000L, remainingMillis)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The one expressive moment: a completed set is acknowledged as the rest begins.
        if (currentSet.isCompleted) {
            SetDoneBadge(
                label = stringResource(R.string.workout_set_done, currentSet.setNumber),
                modifier = Modifier.padding(top = Spacing.s)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        WorkoutStatus(
            stringResource(if (hasLoopRest) R.string.loop_round_rest else R.string.interval_rest_label),
            WorkoutTone.rest
        )
        if (currentSet.loopId != null && currentSet.totalRounds > 1) {
            Text(
                text = stringResource(R.string.loop_round_current, currentSet.roundNumber, currentSet.totalRounds),
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.l))

        TimerDial(
            progress = progress,
            value = "$remainingTime",
            accent = WorkoutTone.rest,
            paused = !effectivelyRunning,
            onToggle = { isRunning = !isRunning },
            // Smaller than elsewhere: this rest also shows the next exercise.
            size = 220.dp,
        )

        Spacer(modifier = Modifier.height(Spacing.l))


        Spacer(modifier = Modifier.weight(1f))

        // What comes next: the exercise, and its set within that exercise.
        nextSet?.let { next ->
            val (_, nextExercise) = session.exercises[next.exerciseIndex]
            val nextExerciseSets = session.sets
                .filter { it.exerciseIndex == next.exerciseIndex && it.roundNumber == next.roundNumber }
                .map { it.setNumber }
                .distinct()
                .size
            Surface(
                shape = MaterialTheme.shapes.large,
                color = cs.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Spacing.l)) {
                    Text(
                        text = stringResource(R.string.interval_next),
                        style = MaterialTheme.typography.labelLarge,
                        color = cs.secondary
                    )
                    Text(
                        text = nextExercise.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = cs.onSurface,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                    Text(
                        text = setLabel(next.setNumber, nextExerciseSets, next.side),
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onSurfaceVariant
                    )
                    if (!nextExercise.description.isNullOrBlank()) {
                        Text(
                            text = nextExercise.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.xs)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.s))

        RestToolbar(
            onMinus = {
                extraMillis -= minOf(10_000L, remainingMillis.coerceAtLeast(0))
                lastShown = countdownSeconds(totalInterval * 1_000L + extraMillis - stopwatch.elapsedMillis)
            },
            onPlus = {
                extraMillis += 10_000L
                lastShown = countdownSeconds(totalInterval * 1_000L + extraMillis - stopwatch.elapsedMillis)
            },
            onSkip = onSkip,
        )
    }
}
