package io.github.gonbei774.calisthenicsmemory.data.v2

import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import java.time.Instant
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
)

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
        // RIGHT and LEFT entries of one set become one row, as the legacy screens store them.
        val v2Sets = v2.groupBy { it.sessionExerciseId to it.setNumber }.values.map { rows ->
            val right = rows.firstOrNull { it.side == BodySide.RIGHT }
            val left = rows.firstOrNull { it.side == BodySide.LEFT }
            val primary = rows.firstOrNull { it.side == BodySide.BILATERAL } ?: right ?: left!!
            val at = Instant.ofEpochMilli(primary.completedAtEpochMillis ?: primary.sessionStartedAtEpochMillis).atZone(zone)
            HistorySet(
                source = HistorySource.V2,
                legacyRecordId = null,
                setEntryIds = rows.sortedBy { it.orderIndex }.map { it.setEntryId },
                exerciseId = primary.exerciseId,
                date = at.format(DATE),
                time = at.format(TIME),
                setNumber = primary.setNumber,
                valueRight = value(primary),
                valueLeft = if (primary.side == BodySide.BILATERAL || left == null || left === primary) null else value(left),
                comment = primary.sessionComment.orEmpty(),
                distanceCm = primary.distanceCm,
                weightG = primary.addedWeightGrams,
                assistanceG = primary.assistanceGrams,
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
}
