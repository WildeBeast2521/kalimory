package io.github.gonbei774.calisthenicsmemory.data.v2

import io.github.gonbei774.calisthenicsmemory.data.IntervalRecord
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** One row of an interval session joined with its exercises and their sets (either may be absent). */
data class IntervalSessionRow(
    val sessionId: Long,
    val programName: String?,
    val startedAtEpochMillis: Long,
    val comment: String?,
    val workSeconds: Int?,
    val restSeconds: Int?,
    val rounds: Int?,
    val roundRestSeconds: Int?,
    val exerciseOrderIndex: Int?,
    val exerciseName: String?,
    val roundNumber: Int?,
    val status: SetEntryStatus?,
)

/**
 * An interval workout as the history screens show it: the legacy [IntervalRecord] shape, from
 * either store. [v2SessionId] is set for v2 workouts; edits and deletes go to that session, and
 * [record]'s id is then meaningless.
 */
data class IntervalHistoryItem(val record: IntervalRecord, val v2SessionId: Long? = null) {
    /** Work intervals done, each counted as one set, so weekly totals include interval workouts. */
    val completedSets: Int get() {
        val exerciseCount = runCatching { Json.decodeFromString(ListSerializer(String.serializer()), record.exercisesJson).size }.getOrDefault(0)
        return record.completedRounds * exerciseCount + record.completedExercisesInLastRound
    }
}

/** Legacy interval records plus v2 interval workouts, each shown once. */
object IntervalHistory {
    private val DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
    private val TIME = DateTimeFormatter.ofPattern("HH:mm")
    private val names = ListSerializer(String.serializer())

    fun merge(legacy: List<IntervalRecord>, v2: List<IntervalSessionRow>, zone: ZoneId): List<IntervalHistoryItem> =
        legacy.map { IntervalHistoryItem(it) } + v2.groupBy { it.sessionId }.map { (sessionId, rows) ->
            val session = rows.first()
            val exercises = rows.filter { it.exerciseOrderIndex != null }
                .distinctBy { it.exerciseOrderIndex }.sortedBy { it.exerciseOrderIndex }.map { it.exerciseName.orEmpty() }
            val rounds = session.rounds ?: 0
            val doneByRound = rows.filter { it.status == SetEntryStatus.COMPLETED && it.roundNumber != null }
                .groupingBy { it.roundNumber!! }.eachCount()
            // Full rounds count from the first; a run that did all of them reads as the legacy "full" record.
            val full = generateSequence(1) { it + 1 }.takeWhile { it <= rounds && (doneByRound[it] ?: 0) >= exercises.size }.count()
            val (completedRounds, lastRound) =
                if (exercises.isNotEmpty() && full >= rounds) rounds to exercises.size else full to (doneByRound[full + 1] ?: 0)
            val at = Instant.ofEpochMilli(session.startedAtEpochMillis).atZone(zone)
            IntervalHistoryItem(
                IntervalRecord(
                    programName = session.programName.orEmpty(),
                    date = at.format(DATE),
                    time = at.format(TIME),
                    workSeconds = session.workSeconds ?: 0,
                    restSeconds = session.restSeconds ?: 0,
                    rounds = rounds,
                    roundRestSeconds = session.roundRestSeconds ?: 0,
                    completedRounds = completedRounds,
                    completedExercisesInLastRound = lastRound,
                    exercisesJson = Json.encodeToString(names, exercises),
                    comment = session.comment,
                ),
                v2SessionId = sessionId,
            )
        }
}
