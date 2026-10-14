package io.github.gonbei774.calisthenicsmemory.util

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet

/**
 * The sets a program run will contain, in the order they are done: standalone exercises and
 * loops by their sort order, each loop repeated for its rounds. With [prefill] on, an exercise
 * outside a loop takes its set count and values from its last session, and an exercise in a loop
 * keeps the program's set count with the last session's values.
 *
 * The program start screen runs exactly these sets, and the Train and To Do lists estimate the
 * time from them, so every screen shows the same estimate for the same run.
 */
object ProgramSetPlanner {

    private sealed class ExecutionItem {
        data class StandaloneExercise(
            val exerciseIndex: Int,
            val pe: ProgramExercise,
            val exercise: Exercise,
            val sortOrder: Int
        ) : ExecutionItem()

        data class Loop(
            val loop: ProgramLoop,
            val exercises: List<Triple<Int, ProgramExercise, Exercise>>
        ) : ExecutionItem()
    }

    /**
     * @param exercisePairs the program's exercises with their definitions, in program order; the
     *   index into this list is each set's `exerciseIndex`.
     * @param previousRecords each exercise's last session, by exercise id (missing means none).
     */
    fun plan(
        exercisePairs: List<Pair<ProgramExercise, Exercise>>,
        programLoops: List<ProgramLoop>,
        previousRecords: Map<Long, List<HistorySet>>,
        prefill: Boolean,
    ): List<ProgramWorkoutSet> {
        // スタンドアロン種目とループを sortOrder でソートして実行順に並べる
        val executionItems = mutableListOf<ExecutionItem>()

        // スタンドアロン種目を追加
        exercisePairs.forEachIndexed { index, (pe, exercise) ->
            if (pe.loopId == null) {
                executionItems.add(ExecutionItem.StandaloneExercise(index, pe, exercise, pe.sortOrder))
            }
        }

        // ループを追加（ループ内の種目も含む）
        programLoops.forEach { loop ->
            val loopExercises = exercisePairs
                .mapIndexedNotNull { index, (pe, exercise) ->
                    if (pe.loopId == loop.id) Triple(index, pe, exercise) else null
                }
                .sortedBy { it.second.sortOrder }
            if (loopExercises.isNotEmpty()) {
                executionItems.add(ExecutionItem.Loop(loop, loopExercises))
            }
        }

        // sortOrder でソート（スタンドアロンはpe.sortOrder、ループはloop.sortOrder）
        val sortedItems = executionItems.sortedBy { item ->
            when (item) {
                is ExecutionItem.StandaloneExercise -> item.sortOrder
                is ExecutionItem.Loop -> item.loop.sortOrder
            }
        }

        // 実行順のセットリストを構築（前回値を含む）
        val allSets = mutableListOf<ProgramWorkoutSet>()

        // ヘルパー関数: 種目のセットを追加
        fun addExerciseSets(
            exerciseIndex: Int,
            pe: ProgramExercise,
            exercise: Exercise,
            loopId: Long? = null,
            roundNumber: Int = 1,
            totalRounds: Int = 1,
            loopRestAfterSeconds: Int = 0,
            isLastExerciseInRound: Boolean = false
        ) {
            val latestRecords = previousRecords[exercise.id] ?: emptyList()
            // プリフィルON かつ 前回記録あり →「前回」タブと同じく前回記録のセット数・値で構築する。
            // （従来はプログラム設定のセット数 pe.sets を使っており、前回タブと食い違っていた）
            // ただしループ内種目は対象外: 前回記録は全ラウンドの完了セットがフラット化されて
            // 保存されているため、記録件数をセット数にするとラウンド数分の掛け算で膨張する
            // (GitHub issue 18)。ループはセット数を pe.sets に固定し、値のみ前回から引き継ぐ。
            val usePreviousLayout = prefill && latestRecords.isNotEmpty() && loopId == null

            if (usePreviousLayout) {
                latestRecords.forEachIndexed { recordIdx, record ->
                    val isLastSetOfExercise = recordIdx == latestRecords.size - 1
                    val actualLoopRestAfter = if (isLastExerciseInRound && isLastSetOfExercise) loopRestAfterSeconds else 0

                    if (exercise.laterality == "Unilateral") {
                        val valueRight = record.valueRight
                        val valueLeft = record.valueLeft ?: record.valueRight
                        // 前回値は左右の平均値を使用（目標値も平均値に設定）
                        val prevAverage = (valueRight + valueLeft) / 2
                        allSets.add(
                            ProgramWorkoutSet(
                                exerciseIndex = exerciseIndex,
                                setNumber = record.setNumber,
                                side = "Right",
                                targetValue = prevAverage,
                                intervalSeconds = pe.intervalSeconds,
                                previousValue = prevAverage,
                                loopId = loopId,
                                roundNumber = roundNumber,
                                totalRounds = totalRounds,
                                loopRestAfterSeconds = 0,  // Rightの後はLeftが来るので0
                                weightG = record.weightG,
                                distanceCm = record.distanceCm,
                                assistanceG = record.assistanceG,
                                previousWeightG = record.weightG,
                                previousDistanceCm = record.distanceCm,
                                previousAssistanceG = record.assistanceG
                            )
                        )
                        allSets.add(
                            ProgramWorkoutSet(
                                exerciseIndex = exerciseIndex,
                                setNumber = record.setNumber,
                                side = "Left",
                                targetValue = prevAverage,
                                intervalSeconds = pe.intervalSeconds,
                                previousValue = prevAverage,
                                loopId = loopId,
                                roundNumber = roundNumber,
                                totalRounds = totalRounds,
                                loopRestAfterSeconds = actualLoopRestAfter,
                                weightG = record.weightG,
                                distanceCm = record.distanceCm,
                                assistanceG = record.assistanceG,
                                previousWeightG = record.weightG,
                                previousDistanceCm = record.distanceCm,
                                previousAssistanceG = record.assistanceG
                            )
                        )
                    } else {
                        allSets.add(
                            ProgramWorkoutSet(
                                exerciseIndex = exerciseIndex,
                                setNumber = record.setNumber,
                                side = null,
                                targetValue = record.valueRight,
                                intervalSeconds = pe.intervalSeconds,
                                previousValue = record.valueRight,
                                loopId = loopId,
                                roundNumber = roundNumber,
                                totalRounds = totalRounds,
                                loopRestAfterSeconds = actualLoopRestAfter,
                                weightG = record.weightG,
                                distanceCm = record.distanceCm,
                                assistanceG = record.assistanceG,
                                previousWeightG = record.weightG,
                                previousDistanceCm = record.distanceCm,
                                previousAssistanceG = record.assistanceG
                            )
                        )
                    }
                }
            } else {
            for (setNum in 1..pe.sets) {
                // ループ内種目は全ラウンドに同じ前回値（＝前回のラウンド1の値）を使う。
                // 確認画面はラウンド1しか表示しないため、見えない値で実行しないようにする
                val matchingRecord = latestRecords.find { it.setNumber == setNum }
                val isLastSetOfExercise = setNum == pe.sets

                // ラウンド間休憩は最後のラウンド以外で、ループの最後の種目の最後のセットに追加
                val actualLoopRestAfter = if (isLastExerciseInRound && isLastSetOfExercise) loopRestAfterSeconds else 0

                if (exercise.laterality == "Unilateral") {
                    val prevRight = matchingRecord?.valueRight
                    val prevLeft = matchingRecord?.valueLeft ?: matchingRecord?.valueRight
                    val prevAverage = when {
                        prevRight != null && prevLeft != null -> (prevRight + prevLeft) / 2
                        prevRight != null -> prevRight
                        prevLeft != null -> prevLeft
                        else -> null
                    }

                    allSets.add(
                        ProgramWorkoutSet(
                            exerciseIndex = exerciseIndex,
                            setNumber = setNum,
                            side = "Right",
                            targetValue = if (prefill && prevRight != null) prevRight else pe.targetValue,
                            intervalSeconds = pe.intervalSeconds,
                            previousValue = prevAverage,
                            loopId = loopId,
                            roundNumber = roundNumber,
                            totalRounds = totalRounds,
                            loopRestAfterSeconds = 0,  // Rightの後はLeftが来るので0
                            weightG = matchingRecord?.weightG,
                            distanceCm = matchingRecord?.distanceCm,
                            assistanceG = matchingRecord?.assistanceG,
                            previousWeightG = matchingRecord?.weightG,
                            previousDistanceCm = matchingRecord?.distanceCm,
                            previousAssistanceG = matchingRecord?.assistanceG
                        )
                    )
                    allSets.add(
                        ProgramWorkoutSet(
                            exerciseIndex = exerciseIndex,
                            setNumber = setNum,
                            side = "Left",
                            targetValue = if (prefill && prevLeft != null) prevLeft else pe.targetValue,
                            intervalSeconds = pe.intervalSeconds,
                            previousValue = prevAverage,
                            loopId = loopId,
                            roundNumber = roundNumber,
                            totalRounds = totalRounds,
                            loopRestAfterSeconds = actualLoopRestAfter,
                            weightG = matchingRecord?.weightG,
                            distanceCm = matchingRecord?.distanceCm,
                            assistanceG = matchingRecord?.assistanceG,
                            previousWeightG = matchingRecord?.weightG,
                            previousDistanceCm = matchingRecord?.distanceCm,
                            previousAssistanceG = matchingRecord?.assistanceG
                        )
                    )
                } else {
                    val prevValue = matchingRecord?.valueRight

                    allSets.add(
                        ProgramWorkoutSet(
                            exerciseIndex = exerciseIndex,
                            setNumber = setNum,
                            side = null,
                            targetValue = if (prefill && prevValue != null) prevValue else pe.targetValue,
                            intervalSeconds = pe.intervalSeconds,
                            previousValue = prevValue,
                            loopId = loopId,
                            roundNumber = roundNumber,
                            totalRounds = totalRounds,
                            loopRestAfterSeconds = actualLoopRestAfter,
                            weightG = matchingRecord?.weightG,
                            distanceCm = matchingRecord?.distanceCm,
                            assistanceG = matchingRecord?.assistanceG,
                            previousWeightG = matchingRecord?.weightG,
                            previousDistanceCm = matchingRecord?.distanceCm,
                            previousAssistanceG = matchingRecord?.assistanceG
                        )
                    )
                }
            }
            }
        }

        // 実行アイテムを順に処理
        sortedItems.forEach { item ->
            when (item) {
                is ExecutionItem.StandaloneExercise -> {
                    addExerciseSets(item.exerciseIndex, item.pe, item.exercise)
                }
                is ExecutionItem.Loop -> {
                    val loop = item.loop
                    val loopExercises = item.exercises

                    // 各ラウンドを処理
                    for (round in 1..loop.rounds) {
                        // 全ラウンド終了後も休憩を入れる（次の種目/ループへの準備時間として）
                        val loopRestAfter = loop.restBetweenRounds

                        loopExercises.forEachIndexed { loopExIdx, (exerciseIndex, pe, exercise) ->
                            val isLastExerciseInRound = loopExIdx == loopExercises.size - 1
                            addExerciseSets(
                                exerciseIndex = exerciseIndex,
                                pe = pe,
                                exercise = exercise,
                                loopId = loop.id,
                                roundNumber = round,
                                totalRounds = loop.rounds,
                                loopRestAfterSeconds = loopRestAfter,
                                isLastExerciseInRound = isLastExerciseInRound
                            )
                        }
                    }
                }
            }
        }

        return allSets
    }
}
