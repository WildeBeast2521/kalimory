package io.github.gonbei774.calisthenicsmemory.data

import kotlinx.serialization.Serializable

/**
 * プログラム実行用のセットデータ
 */
@Serializable
data class ProgramWorkoutSet(
    val exerciseIndex: Int,       // プログラム内の種目インデックス
    val setNumber: Int,           // セット番号（1始まり）
    val side: String?,            // "Right" or "Left" or null
    var targetValue: Int,         // 目標値
    var actualValue: Int = 0,     // 実際の値
    var isCompleted: Boolean = false,
    var isSkipped: Boolean = false,
    var intervalSeconds: Int,     // このセット後のインターバル
    val previousValue: Int? = null, // 前回値（表示用）
    // ループ関連
    val loopId: Long? = null,     // 所属ループID（nullはループ外）
    val roundNumber: Int = 1,     // ラウンド番号（1始まり）
    val totalRounds: Int = 1,     // 総ラウンド数
    val loopRestAfterSeconds: Int = 0,  // ラウンド間休憩（このセット後に追加、最終ラウンドは0）
    // セット別トラッキング値（種目の*TrackingEnabledがtrueの項目のみ使用）
    var weightG: Int? = null,     // 追加ウエイト（g）
    var distanceCm: Int? = null,  // 距離（cm）
    var assistanceG: Int? = null, // アシスト量（g）
    // 前回値（表示用）
    val previousWeightG: Int? = null,
    val previousDistanceCm: Int? = null,
    val previousAssistanceG: Int? = null,
    // When the set was observed to finish (wall clock); null when not timed.
    var completedAtWallMillis: Long? = null,
    // The user marked the set as a warm-up (ADR 0012); the default keeps older checkpoints loadable.
    var isWarmUp: Boolean = false,
    // The user's effort rating, 0..4 (ADR 0012); null when not rated.
    var effortLeft: Int? = null,
)

/**
 * プログラム実行用のセッションデータ
 */
data class ProgramExecutionSession(
    val program: Program,
    val exercises: List<Pair<ProgramExercise, Exercise>>, // ProgramExercise + Exercise情報
    val sets: MutableList<ProgramWorkoutSet>,             // 実行順の全セット
    var comment: String = "",
    val loops: List<ProgramLoop> = emptyList(),           // ループ情報
    // When the first set, or its countdown, began (wall clock); null until then.
    var startedAtWallMillis: Long? = null
)

/**
 * プログラム実行画面のステップ（状態機械）
 */
sealed class ProgramExecutionStep {
    data class Confirm(val session: ProgramExecutionSession) : ProgramExecutionStep()
    data class StartInterval(val session: ProgramExecutionSession, val currentSetIndex: Int) : ProgramExecutionStep()
    data class Executing(val session: ProgramExecutionSession, val currentSetIndex: Int) : ProgramExecutionStep()
    data class Interval(val session: ProgramExecutionSession, val currentSetIndex: Int) : ProgramExecutionStep()
    data class Result(val session: ProgramExecutionSession) : ProgramExecutionStep()
}
/** What follows a finished set; [index] is the set to go to, or the finished set itself for a rest. */
data class AfterSet(val kind: Kind, val index: Int, val redoMode: Boolean) {
    enum class Kind { REST, COUNTDOWN, RUN, RESULT }
}

/** A set still to do: neither finished nor skipped. */
val ProgramWorkoutSet.isPending: Boolean get() = !isCompleted && !isSkipped

/** True when [b] is the other side of the same one-sided set as [a]: no rest between the two. */
fun isOtherSide(a: ProgramWorkoutSet, b: ProgramWorkoutSet): Boolean =
    a.side != null && b.side != null && a.side != b.side &&
        a.exerciseIndex == b.exerciseIndex && a.setNumber == b.setNumber && a.roundNumber == b.roundNumber

/**
 * After the set at [index] was done: the sets still to do for the same exercise and side aim for at least what
 * was just done. It only raises, never lowers, and a skipped set or a warm-up changes nothing.
 */
fun MutableList<ProgramWorkoutSet>.raiseLaterTargets(index: Int) {
    val done = this[index]
    if (!done.isCompleted || done.isWarmUp || done.actualValue <= 0) return
    for (i in index + 1..lastIndex) {
        val later = this[i]
        if (later.isPending && later.exerciseIndex == done.exerciseIndex && later.side == done.side && later.targetValue < done.actualValue) {
            this[i] = later.copy(targetValue = done.actualValue)
        }
    }
}

/** The first set after [after] that is still to do, or null when nothing is left. */
fun nextPendingIndex(sets: List<ProgramWorkoutSet>, after: Int): Int? =
    (after + 1 until sets.size).firstOrNull { sets[it].isPending }

/** Marks the set at [index] skipped: no value, no finish time. A set already done is left as it is. */
fun MutableList<ProgramWorkoutSet>.markSkipped(index: Int) {
    val set = this[index]
    if (set.isCompleted) return
    this[index] = set.copy(isSkipped = true, actualValue = 0, completedAtWallMillis = null)
}

/**
 * Skip exercise from [index]: marks the rest of this exercise's run in this round, both sides of a
 * one-sided set, as skipped. Sets already done stay as they are, and other rounds are not touched.
 * Returns the index of the last set of the run.
 */
fun skipExercise(sets: MutableList<ProgramWorkoutSet>, index: Int): Int {
    val first = sets[index]
    var last = index
    while (last + 1 < sets.size && sets[last + 1].let { it.exerciseIndex == first.exerciseIndex && it.roundNumber == first.roundNumber }) last++
    for (i in index..last) sets.markSkipped(i)
    return last
}

/**
 * Where a program goes after the set at [index] is done or skipped, the same for every timer mode and for
 * the end of a rest ([restDone]). The next set is the first one still to do. The first side of a one-sided
 * set goes straight to the other side. A skipped set has no rest of its own, but the rest between rounds is kept. The next set starts at once after a finished set: the start
 * countdown belongs before the first set and after a retry or a jump, so it shows after a skip or a redo,
 * when [startCountdownSeconds] is above zero.
 */
fun afterSet(
    sets: List<ProgramWorkoutSet>,
    index: Int,
    redoMode: Boolean,
    startCountdownSeconds: Int,
    restDone: Boolean = false,
): AfterSet {
    val target = nextPendingIndex(sets, index) ?: return AfterSet(AfterSet.Kind.RESULT, index, redoMode = false)
    val done = sets[index]
    val ready = if (startCountdownSeconds > 0) AfterSet.Kind.COUNTDOWN else AfterSet.Kind.RUN
    // The first side of a one-sided set is followed by the other side, not by a rest: the countdown is
    // the pause to switch sides. The rest comes after the second side.
    if (isOtherSide(done, sets[target])) return AfterSet(ready, target, redoMode)
    val skipped = done.isSkipped && !done.isCompleted
    val rest = (if (skipped) 0 else done.intervalSeconds) + done.loopRestAfterSeconds
    if (!restDone && rest > 0) return AfterSet(AfterSet.Kind.REST, index, redoMode)
    return AfterSet(if (redoMode || skipped) ready else AfterSet.Kind.RUN, target, redoMode = false)
}
