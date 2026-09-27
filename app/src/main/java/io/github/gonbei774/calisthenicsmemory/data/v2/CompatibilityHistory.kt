package io.github.gonbei774.calisthenicsmemory.data.v2

import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class HistorySource { LEGACY, V2 }

/**
 * One set as the legacy history screens show it, from either store. It is
 * read-only: [source] says which store owns it, and only [HistorySource.LEGACY]
 * rows carry a [legacyRecordId] that legacy edit and delete actions may use.
 */
data class HistorySet(
    val source: HistorySource,
    val legacyRecordId: Long?,
    val setEntryIds: List<Long>,
    val exerciseId: Long,
    val date: String,
    val time: String,
    val setNumber: Int,
    val valueRight: Int,
    val valueLeft: Int?,
    val comment: String,
    val distanceCm: Int?,
    val weightG: Int?,
    val assistanceG: Int?,
    /** The v2 exercise occurrence this set belongs to; null for legacy rows. */
    val sessionExerciseId: Long? = null,
) {
    /** The legacy row behind this set, for legacy edit and delete actions; null for v2-only sets. */
    fun toLegacyRecord(): TrainingRecord? = legacyRecordId?.takeIf { source == HistorySource.LEGACY }?.let {
        TrainingRecord(
            id = it, exerciseId = exerciseId, valueRight = valueRight, valueLeft = valueLeft, setNumber = setNumber,
            date = date, time = time, comment = comment, distanceCm = distanceCm, weightG = weightG, assistanceG = assistanceG,
        )
    }
}

/** A completed v2 set that no legacy record backs, joined with its session and exercise. */
data class V2HistoryRow(
    val setEntryId: Long,
    val sessionExerciseId: Long,
    val exerciseId: Long,
    val exerciseKindSnapshot: ExerciseKind,
    val setNumber: Int,
    val orderIndex: Int,
    val side: BodySide,
    val repetitions: Int?,
    val durationMillis: Long?,
    val distanceCm: Int?,
    val addedWeightGrams: Int?,
    val assistanceGrams: Int?,
    val completedAtEpochMillis: Long?,
    val sessionStartedAtEpochMillis: Long,
    val sessionComment: String?,
    val workoutSessionId: Long = 0,
    /** The occurrence's position in its workout, for execution order across occurrences. */
    val sessionExerciseOrderIndex: Int = 0,
    val roundNumber: Int? = null,
)

/**
 * The compatibility read path (ADR 0003, stage 4): legacy records plus v2-only
 * workouts, each set shown once. Converted legacy records are shown from the legacy
 * table, and their v2 copies (which carry legacyTrainingRecordId) are excluded, so
 * turning v2 on or off never hides or duplicates history.
 */
object CompatibilityHistory {
    private val DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
    private val TIME = DateTimeFormatter.ofPattern("HH:mm")

    fun merge(legacy: List<TrainingRecord>, v2: List<V2HistoryRow>, zone: ZoneId): List<HistorySet> {
        val legacySets = legacy.map {
            HistorySet(
                HistorySource.LEGACY, it.id, emptyList(), it.exerciseId, it.date, it.time, it.setNumber,
                it.valueRight, it.valueLeft, it.comment, it.distanceCm, it.weightG, it.assistanceG,
            )
        }
        // RIGHT and LEFT entries of one set (in one round) become one row, as the legacy screens store them.
        val v2Groups = v2.groupBy { Triple(it.sessionExerciseId, it.roundNumber, it.setNumber) }.values
            .map { rows -> rows.sortedBy { it.orderIndex } }
        // Legacy saves numbered an exercise's sets 1..n in execution order, across rounds and repeated
        // occurrences in one workout; displayed v2 set numbers follow that rule.
        val numbered = v2Groups.groupBy { it.first().workoutSessionId to it.first().exerciseId }.values.flatMap { sets ->
            sets.sortedWith(compareBy({ it.first().sessionExerciseOrderIndex }, { it.first().orderIndex }))
                .mapIndexed { index, rows -> index + 1 to rows }
        }
        val v2Sets = numbered.map { (displayNumber, rows) ->
            val right = rows.firstOrNull { it.side == BodySide.RIGHT }
            val left = rows.firstOrNull { it.side == BodySide.LEFT }
            val primary = rows.firstOrNull { it.side == BodySide.BILATERAL } ?: right ?: left!!
            // Legacy history stamps a whole saved workout with one time, and its screens group sets by it,
            // so a v2 set takes its session's start rather than its own completion time.
            val at = Instant.ofEpochMilli(primary.sessionStartedAtEpochMillis).atZone(zone)
            HistorySet(
                source = HistorySource.V2,
                legacyRecordId = null,
                setEntryIds = rows.map { it.setEntryId },
                exerciseId = primary.exerciseId,
                date = at.format(DATE),
                time = at.format(TIME),
                setNumber = displayNumber,
                valueRight = value(primary),
                valueLeft = if (primary.side == BodySide.BILATERAL || left == null || left === primary) null else value(left),
                comment = primary.sessionComment.orEmpty(),
                distanceCm = primary.distanceCm,
                weightG = primary.addedWeightGrams,
                assistanceG = primary.assistanceGrams,
                sessionExerciseId = primary.sessionExerciseId,
            )
        }
        return (legacySets + v2Sets).sortedWith(
            compareByDescending<HistorySet> { it.date }.thenByDescending { it.time }
                .thenBy { it.source }.thenBy { it.exerciseId }.thenBy { it.setNumber }
        )
    }

    /** Repetitions for dynamic sets; whole seconds (rounded down) for isometric ones, as legacy stores them. */
    private fun value(row: V2HistoryRow): Int = when (row.exerciseKindSnapshot) {
        ExerciseKind.DYNAMIC -> row.repetitions ?: 0
        ExerciseKind.ISOMETRIC -> ((row.durationMillis ?: 0) / 1_000).toInt()
    }

    suspend fun read(database: AppDatabase, zone: ZoneId): List<HistorySet> =
        merge(database.backupDao().records(), database.workoutSessionDao().v2OnlyHistoryRows(), zone)

    /**
     * The sets of the newest workout of [exerciseId], from either store, ordered by set number.
     * A legacy workout is every record at one date and time, as the legacy query defines it.
     */
    suspend fun latestSession(database: AppDatabase, exerciseId: Long, zone: ZoneId): List<HistorySet> {
        val merged = merge(
            database.trainingRecordDao().getLatestSessionByExercise(exerciseId),
            database.workoutSessionDao().latestV2OnlySessionRows(exerciseId),
            zone,
        )
        val newest = merged.firstOrNull() ?: return emptyList()
        return merged
            .filter { it.source == newest.source && it.date == newest.date && it.time == newest.time }
            .sortedBy { it.setNumber }
    }

    /** Whether [exerciseId] has a set on [date] in [zone], from either store. */
    suspend fun hasSetOn(database: AppDatabase, exerciseId: Long, date: LocalDate, zone: ZoneId): Boolean {
        if (database.trainingRecordDao().hasRecordOnDate(exerciseId, date.format(DATE))) return true
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return database.workoutSessionDao().hasV2OnlySetStartedBetween(exerciseId, start, end)
    }
}
