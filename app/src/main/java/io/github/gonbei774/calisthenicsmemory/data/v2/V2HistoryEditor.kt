package io.github.gonbei774.calisthenicsmemory.data.v2

import androidx.room.withTransaction
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Edits and deletes v2 history from the legacy-shaped history screens. Each call is one
 * transaction. The screens show a v2 set as a [HistorySet], so these take the same shape.
 */
object V2HistoryEditor {
    /**
     * Sets the value of one displayed set: repetitions, or whole seconds for isometric
     * exercises, as the legacy screens enter them. [valueLeft] applies to the left side
     * of a unilateral set and is ignored when the set has no left entry.
     */
    suspend fun updateSetValues(database: AppDatabase, set: HistorySet, valueRight: Int, valueLeft: Int?) {
        require(set.source == HistorySource.V2) { "Not a v2 set" }
        require(valueRight >= 0 && (valueLeft == null || valueLeft >= 0)) { "Values must not be negative" }
        val dao = database.workoutSessionDao()
        database.withTransaction {
            val entries = dao.setEntriesByIds(set.setEntryIds)
            val kinds = dao.sessionExercisesByIds(entries.map { it.sessionExerciseId }.distinct())
                .associate { it.id to it.exerciseKindSnapshot }
            val updated = entries.mapNotNull { entry ->
                val value = when (entry.side) {
                    BodySide.LEFT -> valueLeft ?: return@mapNotNull null
                    BodySide.RIGHT, BodySide.BILATERAL -> valueRight
                }
                when (kinds.getValue(entry.sessionExerciseId)) {
                    ExerciseKind.DYNAMIC -> entry.copy(repetitions = value)
                    // Keep sub-second precision when the whole-second value is unchanged.
                    ExerciseKind.ISOMETRIC ->
                        if ((entry.durationMillis ?: -1) / 1_000 == value.toLong()) entry
                        else entry.copy(durationMillis = value * 1_000L)
                }
            }
            dao.updateSetEntries(updated)
        }
    }

    /**
     * Applies the session edit dialog to v2 sets: [date] and [time] move each whole workout
     * the sets belong to (its set times shift by the same amount, and its time becomes
     * minute precision because that is what was entered), [comment] becomes the workout
     * comment, and the metric lists apply to the displayed sets in order.
     */
    suspend fun updateSession(
        database: AppDatabase,
        sets: List<HistorySet>,
        date: String,
        time: String,
        comment: String,
        distancesCm: List<Int?>,
        weightsG: List<Int?>,
        assistancesG: List<Int?>,
        zone: ZoneId,
        nowEpochMillis: Long,
    ) {
        require(sets.all { it.source == HistorySource.V2 }) { "Not v2 sets" }
        val start = LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(zone).toInstant().toEpochMilli()
        val dao = database.workoutSessionDao()
        database.withTransaction {
            val occurrences = dao.sessionExercisesByIds(sets.mapNotNull { it.sessionExerciseId }.distinct())
            for (sessionId in occurrences.map { it.workoutSessionId }.distinct()) {
                moveSession(database, sessionId, start, comment, nowEpochMillis)
            }
            // After the move, so the metric edits apply to the shifted rows.
            sets.forEachIndexed { index, set ->
                dao.updateSetEntries(
                    dao.setEntriesByIds(set.setEntryIds).map {
                        it.copy(
                            distanceCm = distancesCm.getOrNull(index),
                            addedWeightGrams = weightsG.getOrNull(index),
                            assistanceGrams = assistancesG.getOrNull(index),
                        )
                    }
                )
            }
        }
    }

    /**
     * Moves a whole workout to [date] and [time] in [zone] and sets its [comment]. Every set
     * time shifts by the same amount, and a moved workout becomes MINUTE precision.
     */
    suspend fun updateSession(
        database: AppDatabase,
        sessionId: Long,
        date: String,
        time: String,
        comment: String,
        zone: ZoneId,
        nowEpochMillis: Long,
    ) {
        val start = LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(zone).toInstant().toEpochMilli()
        database.withTransaction { moveSession(database, sessionId, start, comment, nowEpochMillis) }
    }

    /** Deletes a whole workout; its exercises and sets cascade. */
    suspend fun deleteSession(database: AppDatabase, sessionId: Long) {
        database.workoutSessionDao().deleteSessions(listOf(sessionId))
    }

    // Call inside a transaction.
    private suspend fun moveSession(database: AppDatabase, sessionId: Long, start: Long, comment: String, nowEpochMillis: Long) {
        val dao = database.workoutSessionDao()
        val session = dao.session(sessionId) ?: return
        val shift = start - session.startedAtEpochMillis
        val moved = shift != 0L
        dao.updateSession(
            session.copy(
                startedAtEpochMillis = start,
                endedAtEpochMillis = session.endedAtEpochMillis?.plus(shift),
                updatedAtEpochMillis = nowEpochMillis,
                timePrecision = if (moved) TimePrecision.MINUTE else session.timePrecision,
                comment = comment.ifBlank { null },
            )
        )
        if (moved) {
            dao.updateSetEntries(
                dao.setEntriesOfSession(sessionId).map {
                    it.copy(
                        startedAtEpochMillis = it.startedAtEpochMillis?.plus(shift),
                        completedAtEpochMillis = it.completedAtEpochMillis?.plus(shift),
                    )
                }
            )
        }
    }

    /**
     * Deletes the exercise occurrences the sets belong to, with all their sets (skipped ones
     * too), and then each workout left without any exercise.
     */
    suspend fun delete(database: AppDatabase, sets: List<HistorySet>) {
        require(sets.all { it.source == HistorySource.V2 }) { "Not v2 sets" }
        val dao = database.workoutSessionDao()
        database.withTransaction {
            val occurrences = dao.sessionExercisesByIds(sets.mapNotNull { it.sessionExerciseId }.distinct())
            dao.deleteSessionExercises(occurrences.map { it.id })
            dao.deleteEmptySessions(occurrences.map { it.workoutSessionId }.distinct())
        }
    }
}
