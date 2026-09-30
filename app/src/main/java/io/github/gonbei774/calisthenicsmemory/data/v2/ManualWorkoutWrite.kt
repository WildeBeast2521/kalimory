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

    /**
     * Several exercises logged together as one past workout: one session at the first entry's
     * date, time and comment, with each entry as an exercise occurrence in the order entered.
     * The entries share the workout's date and time, which the screen keeps for all of them.
     */
    fun rows(workouts: List<ManualWorkout>, zone: ZoneId): ProgramWorkoutRows {
        require(workouts.isNotEmpty()) { "Nothing to record" }
        val first = workouts.first()
        require(workouts.all { it.date == first.date && it.time == first.time }) { "Entries of one workout share its date and time" }
        val parts = workouts.map { rows(it, zone) }
        return ProgramWorkoutRows(
            session = parts.first().session,
            exercises = parts.mapIndexed { index, part -> part.exercise.copy(orderIndex = index) to part.sets },
        )
    }

    /** Writes the entry as one session in one transaction; returns its id, or null when there is no set. */
    suspend fun write(database: AppDatabase, workout: ManualWorkout, zone: ZoneId): Long? =
        write(database, listOf(workout), zone)

    /** Writes the entries as one session in one transaction; entries without sets are left out. */
    suspend fun write(database: AppDatabase, workouts: List<ManualWorkout>, zone: ZoneId): Long? {
        val withSets = workouts.filter { it.sets.isNotEmpty() }
        if (withSets.isEmpty()) return null
        val rows = rows(withSets, zone)
        val dao = database.workoutSessionDao()
        return database.withTransaction {
            val sessionId = dao.insertSession(rows.session)
            rows.exercises.forEach { (exercise, sets) ->
                val exerciseId = dao.insertSessionExercise(exercise.copy(workoutSessionId = sessionId))
                dao.insertSetEntries(sets.map { it.copy(sessionExerciseId = exerciseId) })
            }
            sessionId
        }
    }
}
