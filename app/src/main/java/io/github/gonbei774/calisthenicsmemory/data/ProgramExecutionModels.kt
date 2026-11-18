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

/**
 * Where a program goes after the set at [index] is done, the same for every timer mode and for the end of
 * a rest ([restDone]). A set with a rest after it goes to the rest first. The next set then starts at
 * once: the start countdown belongs before the first set and after a jump or retry, not between sets.
 * Finishing a set that was redone moves on to the next unfinished set, which is a jump, so the countdown
 * shows when [startCountdownSeconds] is above zero.
 */
fun afterSet(
    sets: List<ProgramWorkoutSet>,
    index: Int,
    redoMode: Boolean,
    startCountdownSeconds: Int,
    restDone: Boolean = false,
): AfterSet {
    val next = index + 1
    if (next >= sets.size) return AfterSet(AfterSet.Kind.RESULT, index, redoMode = false)
    val done = sets[index]
    if (!restDone && (done.intervalSeconds > 0 || done.loopRestAfterSeconds > 0)) {
        return AfterSet(AfterSet.Kind.REST, index, redoMode)
    }
    if (!redoMode) return AfterSet(AfterSet.Kind.RUN, next, redoMode = false)
    val target = (next until sets.size).firstOrNull { !sets[it].isCompleted }
        ?: return AfterSet(AfterSet.Kind.RESULT, index, redoMode = false)
    val kind = if (startCountdownSeconds > 0) AfterSet.Kind.COUNTDOWN else AfterSet.Kind.RUN
    return AfterSet(kind, target, redoMode = false)
}
