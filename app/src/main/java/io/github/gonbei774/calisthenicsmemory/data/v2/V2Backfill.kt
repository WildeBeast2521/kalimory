package io.github.gonbei774.calisthenicsmemory.data.v2

import androidx.room.withTransaction
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Conservative conversion of legacy training records into v2 sessions (ADR 0003).
 *
 * Grouping: records with the same date, time, and comment form one session. That is
 * exactly what one legacy save writes (every save stamps all its sets with one date,
 * minute, and comment), so no grouping is invented. Within a session, exercises keep
 * the order they were first recorded in, and sets are ordered by set number, then id.
 *
 * Time: the local date and minute are read in [zone] (the device zone at conversion)
 * and marked [TimePrecision.MINUTE]. The session start is that recorded minute; the
 * end, per-set times, durations between sets, and seconds are left unknown.
 *
 * Sides: a record with a left value becomes RIGHT and LEFT entries, which share the
 * record's weight, distance, and assistance as the legacy screens did. A record
 * without one becomes a BILATERAL entry, unless its exercise is now unilateral: then
 * the side is ambiguous and the record stays on the legacy path.
 *
 * Nothing is dropped: records that cannot be converted exactly are reported and
 * remain readable through the legacy tables.
 */
object V2Backfill {
    private val DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
    private val TIME = DateTimeFormatter.ofPattern("HH:mm")

    data class PlannedExercise(val exercise: SessionExerciseEntity, val sets: List<SetEntryEntity>)
    data class PlannedSession(val session: WorkoutSessionEntity, val exercises: List<PlannedExercise>)
    data class Plan(val sessions: List<PlannedSession>, val report: V2MigrationReport)

    fun plan(
        exercises: List<Exercise>,
        groups: List<ExerciseGroup>,
        records: List<TrainingRecord>,
        alreadyConvertedRecordIds: Set<Long>,
        zone: ZoneId,
        nowEpochMillis: Long,
    ): Plan {
        val exerciseById = exercises.associateBy { it.id }
        val groupIdByName = groups.associate { it.name to it.id }
        val anomalies = mutableListOf<LegacyAnomaly>()

        class Candidate(val record: TrainingRecord, val exercise: Exercise, val startedAt: Long, val kind: ExerciseKind, val laterality: Laterality)

        val candidates = mutableListOf<Candidate>()
        var already = 0
        for (record in records.sortedBy { it.id }) {
            if (record.id in alreadyConvertedRecordIds) { already++; continue }
            val exercise = exerciseById[record.exerciseId]
            val startedAt = epochMillis(record.date, record.time, zone)
            val kind = when (exercise?.type) { "Dynamic" -> ExerciseKind.DYNAMIC; "Isometric" -> ExerciseKind.ISOMETRIC; else -> null }
            val laterality = when (exercise?.laterality) { "Bilateral" -> Laterality.BILATERAL; "Unilateral" -> Laterality.UNILATERAL; else -> null }
            val reason = when {
                exercise == null -> LegacyAnomalyReason.MISSING_EXERCISE
                startedAt == null -> LegacyAnomalyReason.UNPARSEABLE_DATE_TIME
                kind == null -> LegacyAnomalyReason.UNKNOWN_EXERCISE_TYPE
                laterality == null -> LegacyAnomalyReason.UNKNOWN_LATERALITY
                listOfNotNull(record.valueRight, record.valueLeft, record.weightG, record.distanceCm, record.assistanceG).any { it < 0 } ->
                    LegacyAnomalyReason.NEGATIVE_VALUE
                record.valueLeft == null && laterality == Laterality.UNILATERAL -> LegacyAnomalyReason.AMBIGUOUS_SIDE
                else -> null
            }
            if (reason != null) anomalies += LegacyAnomaly(record.id, reason)
            else candidates += Candidate(record, exercise!!, startedAt!!, kind!!, laterality!!)
        }

        val sessions = candidates
            .groupBy { Triple(it.record.date, it.record.time, it.record.comment) }
            .values
            .sortedWith(compareBy({ it.first().startedAt }, { it.first().record.id }))
            .map { batch ->
                val first = batch.first()
                val session = WorkoutSessionEntity(
                    status = WorkoutSessionStatus.COMPLETED,
                    sourceType = WorkoutSourceType.LEGACY_IMPORT,
                    startedAtEpochMillis = first.startedAt,
                    updatedAtEpochMillis = nowEpochMillis,
                    timePrecision = TimePrecision.MINUTE,
                    comment = first.record.comment.ifEmpty { null },
                )
                // Exercises in first-recorded order; sets by set number, then record id.
                val byExercise = batch.groupBy { it.exercise.id }.values
                    .sortedBy { group -> group.minOf { it.record.id } }
                val plannedExercises = byExercise.mapIndexed { order, group ->
                    val c = group.first()
                    val sessionExercise = SessionExerciseEntity(
                        workoutSessionId = 0,
                        orderIndex = order,
                        exerciseId = c.exercise.id,
                        groupId = c.exercise.group?.let(groupIdByName::get),
                        exerciseNameSnapshot = c.exercise.name,
                        exerciseKindSnapshot = c.kind,
                        lateralitySnapshot = c.laterality,
                        groupNameSnapshot = c.exercise.group,
                    )
                    var orderIndex = 0
                    val sets = group.sortedWith(compareBy({ it.record.setNumber }, { it.record.id })).flatMap { candidate ->
                        val r = candidate.record
                        val sides = if (r.valueLeft != null) listOf(BodySide.RIGHT to r.valueRight, BodySide.LEFT to r.valueLeft)
                        else listOf(BodySide.BILATERAL to r.valueRight)
                        sides.map { (side, value) ->
                            SetEntryEntity(
                                sessionExerciseId = 0,
                                orderIndex = orderIndex++,
                                setNumber = r.setNumber,
                                status = SetEntryStatus.COMPLETED,
                                side = side,
                                repetitions = if (candidate.kind == ExerciseKind.DYNAMIC) value else null,
                                durationMillis = if (candidate.kind == ExerciseKind.ISOMETRIC) value * 1_000L else null,
                                distanceCm = r.distanceCm,
                                addedWeightGrams = r.weightG,
                                assistanceGrams = r.assistanceG,
                                timePrecision = TimePrecision.MINUTE,
                                legacyTrainingRecordId = r.id,
                            )
                        }
                    }
                    PlannedExercise(sessionExercise, sets)
                }
                PlannedSession(session, plannedExercises)
            }

        val report = V2MigrationReport(
            legacyRecords = records.size,
            convertedRecords = candidates.size,
            alreadyConvertedRecords = already,
            sessionsCreated = sessions.size,
            setEntriesCreated = sessions.sumOf { s -> s.exercises.sumOf { it.sets.size } },
            anomalies = anomalies,
        )
        return Plan(sessions, report)
    }

    private fun epochMillis(date: String, time: String, zone: ZoneId): Long? = try {
        LocalDate.parse(date, DATE).atTime(LocalTime.parse(time, TIME)).atZone(zone).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }

    /**
     * Plans and writes one run in a single transaction. Records converted by an
     * earlier run are skipped, so running again creates nothing new.
     */
    suspend fun run(database: AppDatabase, zone: ZoneId, nowEpochMillis: Long): V2MigrationReport =
        database.withTransaction {
            val dao = database.workoutSessionDao()
            val backup = database.backupDao()
            val plan = plan(
                exercises = backup.exercises(),
                groups = backup.groups(),
                records = backup.records(),
                alreadyConvertedRecordIds = dao.legacyTrainingRecordIds().toSet(),
                zone = zone,
                nowEpochMillis = nowEpochMillis,
            )
            for (planned in plan.sessions) {
                val sessionId = dao.insertSession(planned.session)
                for (exercise in planned.exercises) {
                    val exerciseId = dao.insertSessionExercise(exercise.exercise.copy(workoutSessionId = sessionId))
                    dao.insertSetEntries(exercise.sets.map { it.copy(sessionExerciseId = exerciseId) })
                }
            }
            plan.report
        }
}
