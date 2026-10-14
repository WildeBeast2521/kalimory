package io.github.gonbei774.calisthenicsmemory.util

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import kotlin.math.roundToInt

/**
 * プログラムの推定所要時間を算出する。
 *
 * 計算内訳:
 *   - 各セットの作業時間
 *       Isometric: targetValue 秒
 *       Dynamic: targetValue × (exercise.repDuration ?: DEFAULT_DYNAMIC_REP_SECONDS)
 *   - 各セット前の開始前カウントダウン（有効時のみ加算）
 *   - 各セット後の intervalSeconds と loopRestAfterSeconds
 *   - 最後のセット後のインターバル／ラウンド休憩は差し引く（実際には発生しないため）
 */
object ProgramTimeEstimator {
    private const val DEFAULT_DYNAMIC_REP_SECONDS = 3

    fun estimateSeconds(
        session: ProgramExecutionSession,
        startCountdownSeconds: Int
    ): Int = estimateSeconds(session.sets, session.exercises, startCountdownSeconds)

    /**
     * The estimate for a program's own sets and targets, as the program editor shows them. It is
     * the start screen's estimate for the same sets: both go through [ProgramSetPlanner].
     */
    fun estimateSeconds(
        programExercises: List<ProgramExercise>,
        loops: List<ProgramLoop>,
        exerciseMap: Map<Long, Exercise>,
        startCountdownSeconds: Int
    ): Int {
        val pairs = programExercises.mapNotNull { pe -> exerciseMap[pe.exerciseId]?.let { pe to it } }
        return estimateSeconds(ProgramSetPlanner.plan(pairs, loops, emptyMap(), prefill = false), pairs, startCountdownSeconds)
    }

    /**
     * The estimate for the next run of a program, as its start screen will show it: with prefill
     * on, exercises take their last session's sets and values. The Train and To Do lists use this,
     * so a program shows the same time in the list and when it opens.
     */
    suspend fun estimateNextRunSeconds(
        programExercises: List<ProgramExercise>,
        loops: List<ProgramLoop>,
        exerciseMap: Map<Long, Exercise>,
        startCountdownSeconds: Int,
        prefill: Boolean,
        latestSession: suspend (exerciseId: Long) -> List<HistorySet>,
    ): Int {
        val pairs = programExercises.mapNotNull { pe -> exerciseMap[pe.exerciseId]?.let { pe to it } }
        val previous = pairs.map { it.second.id }.distinct().associateWith { latestSession(it) }
        return estimateSeconds(ProgramSetPlanner.plan(pairs, loops, previous, prefill), pairs, startCountdownSeconds)
    }

    /** Work, the rest after every set but the last, and a countdown before each set. */
    fun estimateSeconds(
        sets: List<ProgramWorkoutSet>,
        exercises: List<Pair<ProgramExercise, Exercise>>,
        startCountdownSeconds: Int,
    ): Int = computeFromFlatSets(sets) { set -> exercises.getOrNull(set.exerciseIndex)?.second } +
        startCountdownSeconds * sets.size

    /**
     * The start countdown a program run adds before each set, as the user set it. Every screen
     * estimates with it, so one program shows the same time everywhere.
     */
    fun startCountdownSeconds(preferences: io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences): Int =
        if (preferences.isStartCountdownEnabled()) preferences.getStartCountdown() else 0

    fun formatMinutes(seconds: Int): Int =
        (seconds / 60.0).roundToInt().coerceAtLeast(1)

    fun estimateExerciseSeconds(exercise: Exercise): Int? {
        val sets = exercise.targetSets ?: return null
        val targetValue = exercise.targetValue ?: return null
        val rest = exercise.restInterval ?: 0
        val sides = if (exercise.laterality == "Unilateral") 2 else 1
        val work = workSecondsFor(exercise, targetValue)
        val totalUnits = sets * sides
        return totalUnits * work + (totalUnits - 1).coerceAtLeast(0) * rest
    }

    fun estimateIntervalSeconds(interval: IntervalProgram, exerciseCount: Int): Int {
        if (exerciseCount <= 0 || interval.rounds <= 0) return 0
        val perRoundWork = exerciseCount * interval.workSeconds
        val perRoundRest = (exerciseCount - 1).coerceAtLeast(0) * interval.restSeconds
        val rounds = interval.rounds
        return rounds * (perRoundWork + perRoundRest) +
            (rounds - 1).coerceAtLeast(0) * interval.roundRestSeconds
    }

    private fun computeFromFlatSets(
        sets: List<ProgramWorkoutSet>,
        exerciseLookup: (ProgramWorkoutSet) -> Exercise?
    ): Int {
        if (sets.isEmpty()) return 0
        var total = 0
        val lastIndex = sets.lastIndex
        sets.forEachIndexed { index, set ->
            val exercise = exerciseLookup(set) ?: return@forEachIndexed
            total += workSecondsFor(exercise, set.targetValue)
            if (index != lastIndex) {
                total += set.intervalSeconds + set.loopRestAfterSeconds
            }
        }
        return total
    }

    private fun workSecondsFor(exercise: Exercise, targetValue: Int): Int =
        when (exercise.type) {
            "Isometric" -> targetValue
            else -> targetValue * (exercise.repDuration ?: DEFAULT_DYNAMIC_REP_SECONDS)
        }
}