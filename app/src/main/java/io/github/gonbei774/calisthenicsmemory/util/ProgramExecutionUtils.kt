package io.github.gonbei774.calisthenicsmemory.util

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.data.v2.BodySide
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.v2.Laterality
import io.github.gonbei774.calisthenicsmemory.data.v2.ProgramRun
import io.github.gonbei774.calisthenicsmemory.data.v2.ProgramRunExercise
import io.github.gonbei774.calisthenicsmemory.data.v2.ProgramRunSet
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

/**
 * プログラム実行結果を保存する: the run is saved as one v2 session (see [toProgramRun]).
 */
fun saveProgramResults(
    viewModel: TrainingViewModel,
    session: ProgramExecutionSession
) {
    viewModel.recordProgramWorkout(session.toProgramRun(viewModel.groups.value, System.currentTimeMillis()))
}

/**
 * This run as one v2 session. Every exercise occurrence keeps its own sets in execution
 * order, loop rounds included; sets that were completed or skipped are recorded, and those
 * without a value become skipped sets, which the legacy save dropped.
 */
fun ProgramExecutionSession.toProgramRun(groups: List<ExerciseGroup>, savedAtWallMillis: Long): ProgramRun =
    ProgramRun(
        programId = program.id,
        programName = program.name,
        exercises = exercises.mapIndexed { exerciseIndex, (programExercise, exercise) ->
            ProgramRunExercise(
                programExerciseId = programExercise.id,
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                kind = if (exercise.type == "Isometric") ExerciseKind.ISOMETRIC else ExerciseKind.DYNAMIC,
                laterality = if (exercise.laterality == "Unilateral") Laterality.UNILATERAL else Laterality.BILATERAL,
                groupId = exercise.group?.let { name -> groups.find { it.name == name }?.id },
                groupName = exercise.group,
                targetSets = programExercise.sets,
                targetValue = programExercise.targetValue,
                sets = sets.filter { it.exerciseIndex == exerciseIndex && (it.isCompleted || it.isSkipped) }.map { set ->
                    ProgramRunSet(
                        setNumber = set.setNumber,
                        roundNumber = set.roundNumber.takeIf { set.loopId != null },
                        side = when (set.side) {
                            "Right" -> BodySide.RIGHT
                            "Left" -> BodySide.LEFT
                            else -> BodySide.BILATERAL
                        },
                        value = set.actualValue.coerceAtLeast(0),
                        targetValue = set.targetValue,
                        distanceCm = set.distanceCm,
                        weightG = set.weightG,
                        assistanceG = set.assistanceG,
                        completedAtWallMillis = set.completedAtWallMillis,
                    )
                },
            )
        },
        comment = comment,
        startedAtWallMillis = startedAtWallMillis,
        savedAtWallMillis = savedAtWallMillis,
    )

/**
 * セットリストを構築するファクトリ関数
 * プログラム設定の値を使用
 * @param originalSets 元のセットリスト（previousValueとループ情報を引き継ぐため）
 */
fun buildProgramValueSets(
    exercises: List<Pair<ProgramExercise, Exercise>>,
    originalSets: List<ProgramWorkoutSet> = emptyList()
): MutableList<ProgramWorkoutSet> {
    val sets = mutableListOf<ProgramWorkoutSet>()
    // 実行順の (種目index, ラウンド) ブロック並びを originalSets から取得する。
    // 種目ごとに全ラウンドを展開すると、ループが「種目優先(A×3, B×3...)」の誤った順になるため、
    // 元のセット列の出現順(=ラウンド優先)をそのまま生成順として使う。
    val blockOrder = originalSets.map { it.exerciseIndex to it.roundNumber }.distinct()
    blockOrder.forEach { (index, round) ->
        val (pe, exercise) = exercises[index]
        // ループ情報を元のセットから取得
        val exerciseSets = originalSets.filter { it.exerciseIndex == index }
        val firstSet = exerciseSets.firstOrNull()
        val loopId = firstSet?.loopId
        val totalRounds = firstSet?.totalRounds ?: 1

        // このラウンドの既存セットからloopRestAfterSecondsを取得
        val existingRoundSets = exerciseSets.filter { it.roundNumber == round }
        val loopRestAfter = existingRoundSets.lastOrNull()?.loopRestAfterSeconds ?: 0

        for (setNum in 1..pe.sets) {
            val isLastSetOfRound = setNum == pe.sets
                if (exercise.laterality == "Unilateral") {
                    val priorRight = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == "Right" && it.roundNumber == round }
                    val priorLeft = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == "Left" && it.roundNumber == round }
                    val priorTracking = priorRight ?: priorLeft  // tracking値はR/L共通
                    sets.add(ProgramWorkoutSet(
                        exerciseIndex = index,
                        setNumber = setNum,
                        side = "Right",
                        targetValue = pe.targetValue,
                        intervalSeconds = pe.intervalSeconds,
                        previousValue = priorRight?.previousValue,
                        loopId = loopId,
                        roundNumber = round,
                        totalRounds = totalRounds,
                        loopRestAfterSeconds = 0,  // Rightの後はLeftが来る
                        weightG = priorTracking?.weightG,
                        distanceCm = priorTracking?.distanceCm,
                        assistanceG = priorTracking?.assistanceG,
                        previousWeightG = priorTracking?.previousWeightG,
                        previousDistanceCm = priorTracking?.previousDistanceCm,
                        previousAssistanceG = priorTracking?.previousAssistanceG
                    ))
                    sets.add(ProgramWorkoutSet(
                        exerciseIndex = index,
                        setNumber = setNum,
                        side = "Left",
                        targetValue = pe.targetValue,
                        intervalSeconds = pe.intervalSeconds,
                        previousValue = priorLeft?.previousValue,
                        loopId = loopId,
                        roundNumber = round,
                        totalRounds = totalRounds,
                        loopRestAfterSeconds = if (isLastSetOfRound) loopRestAfter else 0,
                        weightG = priorTracking?.weightG,
                        distanceCm = priorTracking?.distanceCm,
                        assistanceG = priorTracking?.assistanceG,
                        previousWeightG = priorTracking?.previousWeightG,
                        previousDistanceCm = priorTracking?.previousDistanceCm,
                        previousAssistanceG = priorTracking?.previousAssistanceG
                    ))
                } else {
                    val priorSet = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == null && it.roundNumber == round }
                    sets.add(ProgramWorkoutSet(
                        exerciseIndex = index,
                        setNumber = setNum,
                        side = null,
                        targetValue = pe.targetValue,
                        intervalSeconds = pe.intervalSeconds,
                        previousValue = priorSet?.previousValue,
                        loopId = loopId,
                        roundNumber = round,
                        totalRounds = totalRounds,
                        loopRestAfterSeconds = if (isLastSetOfRound) loopRestAfter else 0,
                        weightG = priorSet?.weightG,
                        distanceCm = priorSet?.distanceCm,
                        assistanceG = priorSet?.assistanceG,
                        previousWeightG = priorSet?.previousWeightG,
                        previousDistanceCm = priorSet?.previousDistanceCm,
                        previousAssistanceG = priorSet?.previousAssistanceG
                    ))
                }
            }
        }
    return sets
}

/**
 * セットリストを構築するファクトリ関数
 * チャレンジ設定（種目のデフォルト値）を使用、なければプログラム設定
 * @param originalSets 元のセットリスト（previousValueとループ情報を引き継ぐため）
 */
fun buildChallengeValueSets(
    exercises: List<Pair<ProgramExercise, Exercise>>,
    originalSets: List<ProgramWorkoutSet> = emptyList()
): MutableList<ProgramWorkoutSet> {
    val sets = mutableListOf<ProgramWorkoutSet>()
    // 実行順の (種目index, ラウンド) ブロック並びを originalSets から取得する。
    // 種目ごとに全ラウンドを展開すると、ループが「種目優先(A×3, B×3...)」の誤った順になるため、
    // 元のセット列の出現順(=ラウンド優先)をそのまま生成順として使う。
    val blockOrder = originalSets.map { it.exerciseIndex to it.roundNumber }.distinct()
    blockOrder.forEach { (index, round) ->
        val (pe, exercise) = exercises[index]
        val setCount = exercise.targetSets ?: pe.sets
        val targetValue = exercise.targetValue ?: pe.targetValue
        val interval = exercise.restInterval ?: pe.intervalSeconds

        // ループ情報を元のセットから取得
        val exerciseSets = originalSets.filter { it.exerciseIndex == index }
        val firstSet = exerciseSets.firstOrNull()
        val loopId = firstSet?.loopId
        val totalRounds = firstSet?.totalRounds ?: 1

        // このラウンドの既存セットからloopRestAfterSecondsを取得
        val existingRoundSets = exerciseSets.filter { it.roundNumber == round }
        val loopRestAfter = existingRoundSets.lastOrNull()?.loopRestAfterSeconds ?: 0

        for (setNum in 1..setCount) {
            val isLastSetOfRound = setNum == setCount
                if (exercise.laterality == "Unilateral") {
                    val priorRight = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == "Right" && it.roundNumber == round }
                    val priorLeft = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == "Left" && it.roundNumber == round }
                    val priorTracking = priorRight ?: priorLeft  // tracking値はR/L共通
                    sets.add(ProgramWorkoutSet(
                        exerciseIndex = index,
                        setNumber = setNum,
                        side = "Right",
                        targetValue = targetValue,
                        intervalSeconds = interval,
                        previousValue = priorRight?.previousValue,
                        loopId = loopId,
                        roundNumber = round,
                        totalRounds = totalRounds,
                        loopRestAfterSeconds = 0,  // Rightの後はLeftが来る
                        weightG = priorTracking?.weightG,
                        distanceCm = priorTracking?.distanceCm,
                        assistanceG = priorTracking?.assistanceG,
                        previousWeightG = priorTracking?.previousWeightG,
                        previousDistanceCm = priorTracking?.previousDistanceCm,
                        previousAssistanceG = priorTracking?.previousAssistanceG
                    ))
                    sets.add(ProgramWorkoutSet(
                        exerciseIndex = index,
                        setNumber = setNum,
                        side = "Left",
                        targetValue = targetValue,
                        intervalSeconds = interval,
                        previousValue = priorLeft?.previousValue,
                        loopId = loopId,
                        roundNumber = round,
                        totalRounds = totalRounds,
                        loopRestAfterSeconds = if (isLastSetOfRound) loopRestAfter else 0,
                        weightG = priorTracking?.weightG,
                        distanceCm = priorTracking?.distanceCm,
                        assistanceG = priorTracking?.assistanceG,
                        previousWeightG = priorTracking?.previousWeightG,
                        previousDistanceCm = priorTracking?.previousDistanceCm,
                        previousAssistanceG = priorTracking?.previousAssistanceG
                    ))
                } else {
                    val priorSet = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == null && it.roundNumber == round }
                    sets.add(ProgramWorkoutSet(
                        exerciseIndex = index,
                        setNumber = setNum,
                        side = null,
                        targetValue = targetValue,
                        intervalSeconds = interval,
                        previousValue = priorSet?.previousValue,
                        loopId = loopId,
                        roundNumber = round,
                        totalRounds = totalRounds,
                        loopRestAfterSeconds = if (isLastSetOfRound) loopRestAfter else 0,
                        weightG = priorSet?.weightG,
                        distanceCm = priorSet?.distanceCm,
                        assistanceG = priorSet?.assistanceG,
                        previousWeightG = priorSet?.previousWeightG,
                        previousDistanceCm = priorSet?.previousDistanceCm,
                        previousAssistanceG = priorSet?.previousAssistanceG
                    ))
                }
            }
        }
    return sets
}