package io.github.gonbei774.calisthenicsmemory.data.v2

import androidx.room.withTransaction
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** One set entered by hand. [valueLeft] is the left side of a unilateral set, when entered. */
data class ManualSet(
    val valueRight: Int,
    val valueLeft: Int?,
    val distanceCm: Int?,
    val weightG: Int?,
    val assistanceG: Int?,
)

/** Sets entered by hand for one exercise, at a date and time the user chose. */
data class ManualWorkout(
    val exerciseId: Long,
    val exerciseName: String,
    val kind: ExerciseKind,
    val laterality: Laterality,
    val groupId: Long?,
    val groupName: String?,
    val date: LocalDate,
    val time: LocalTime,
    val sets: List<ManualSet>,
    val comment: String,
    val savedAtWallMillis: Long,
)

object ManualWorkoutWriter {
    /**
     * Maps a manual entry to v2 rows. The user chose a minute, so the session is MINUTE
     * precision with no end, and no set has a time. Every entered value is a completed
     * set, 0 included, as the legacy screen recorded it.
     */
    fun rows(workout: ManualWorkout, zone: ZoneId): SingleWorkoutRows {
        require(workout.sets.all { it.valueRight >= 0 && (it.valueLeft ?: 0) >= 0 }) { "Values must not be negative" }
        // The screen's default time is "now", which carries seconds the user never chose.
        val start = workout.date.atTime(workout.time.truncatedTo(ChronoUnit.MINUTES)).atZone(zone).toInstant().toEpochMilli()
        val isometric = workout.kind == ExerciseKind.ISOMETRIC
        fun entry(order: Int, number: Int, side: BodySide, value: Int, set: ManualSet) = SetEntryEntity(
            sessionExerciseId = 0,
            orderIndex = order,
            setNumber = number,
            status = SetEntryStatus.COMPLETED,
            side = side,
            repetitions = if (isometric) null else value,
            durationMillis = if (isometric) value * 1_000L else null,
            distanceCm = set.distanceCm,
            addedWeightGrams = set.weightG,
            assistanceGrams = set.assistanceG,
            timePrecision = TimePrecision.MINUTE,
        )
        val unilateral = workout.laterality == Laterality.UNILATERAL
        val entries = mutableListOf<SetEntryEntity>()
        workout.sets.forEachIndexed { index, set ->
            val number = index + 1
            if (unilateral) {
                entries += entry(entries.size, number, BodySide.RIGHT, set.valueRight, set)
                set.valueLeft?.let { entries += entry(entries.size, number, BodySide.LEFT, it, set) }
            } else {
                entries += entry(entries.size, number, BodySide.BILATERAL, set.valueRight, set)
            }
        }
        return SingleWorkoutRows(
            session = WorkoutSessionEntity(
                status = WorkoutSessionStatus.COMPLETED,
                sourceType = WorkoutSourceType.MANUAL,
                startedAtEpochMillis = start,
                updatedAtEpochMillis = workout.savedAtWallMillis,
                timePrecision = TimePrecision.MINUTE,
                comment = workout.comment.ifBlank { null },
            ),
            exercise = SessionExerciseEntity(
                workoutSessionId = 0,
                orderIndex = 0,
                exerciseId = workout.exerciseId,
                groupId = workout.groupId,
                exerciseNameSnapshot = workout.exerciseName,
                exerciseKindSnapshot = workout.kind,
                lateralitySnapshot = workout.laterality,
                groupNameSnapshot = workout.groupName,
            ),
            sets = entries,
        )
    }

    /** Writes the entry as one session in one transaction; returns its id, or null when there is no set. */
    suspend fun write(database: AppDatabase, workout: ManualWorkout, zone: ZoneId): Long? {
        if (workout.sets.isEmpty()) return null
        val rows = rows(workout, zone)
        val dao = database.workoutSessionDao()
        return database.withTransaction {
            val sessionId = dao.insertSession(rows.session)
            val exerciseId = dao.insertSessionExercise(rows.exercise.copy(workoutSessionId = sessionId))
            dao.insertSetEntries(rows.sets.map { it.copy(sessionExerciseId = exerciseId) })
            sessionId
        }
    }
}
