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
 * Turns the rows of a records CSV into workouts in the current history model.
 *
 * Grouping is the rule the legacy conversion uses ([V2Backfill]): rows with the same date, minute and
 * comment form one workout, exercises keep the order of their first row and sets their set number. Only
 * what the file says is written: the session starts at the file's date and minute (minute precision),
 * and no end, seconds or per-set times are invented.
 *
 * Rows already in history, in either store, are skipped, so importing a file twice or importing an
 * export adds nothing. An exercise that already has sets at that minute (another workout, or the older
 * record format) keeps writing older-format records for that minute: a new workout there would restart its
 * set numbers at 1 and could repeat a key an export would then lose. Those older records cannot hold
 * warm-up marks or effort, so a row that carries them is refused, not silently stripped.
 */
object CsvWorkoutImport {
    private val DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
    private val TIME = DateTimeFormatter.ofPattern("HH:mm")

    /** One CSV row whose exercise was found. [line] is its place in the file, for ordering and messages. */
    data class Row(
        val line: Int,
        val exercise: Exercise,
        val date: String,
        val time: String,
        val setNumber: Int,
        val valueRight: Int,
        val valueLeft: Int?,
        val comment: String,
        val distanceCm: Int?,
        val weightG: Int?,
        val assistanceG: Int?,
        val warmUp: Boolean = false,
        val effortLeft: Int? = null,
        /** The left side's own values; null means the same as the right side. */
        val weightLeftG: Int? = null,
        val assistanceLeftG: Int? = null,
        val distanceLeftCm: Int? = null,
    ) {
        val hasMarks: Boolean get() = warmUp || effortLeft != null

        /** The two sides carry different values, which a legacy record (one value for both sides) cannot hold. */
        val sidesDiffer: Boolean get() = valueLeft != null &&
            ((weightLeftG != null && weightLeftG != weightG) || (assistanceLeftG != null && assistanceLeftG != assistanceG) ||
                (distanceLeftCm != null && distanceLeftCm != distanceCm))
        val key: List<Any> get() = listOf(exercise.id, date, time, setNumber)
    }

    data class PlannedExercise(val exercise: SessionExerciseEntity, val sets: List<SetEntryEntity>)
    data class PlannedSession(val session: WorkoutSessionEntity, val exercises: List<PlannedExercise>)

    /** What to write ([sessions], [legacy] records) and what to report ([skipped] as present, [refused] for marks). */
    data class Plan(
        val sessions: List<PlannedSession>,
        val legacy: List<Row>,
        val skipped: List<Row>,
        val refused: List<Row>,
    ) {
        val added: Int get() = sessions.sumOf { s -> s.exercises.sumOf { e -> e.sets.map { it.setNumber }.distinct().size } } + legacy.size
    }

    /**
     * [existingKeys] are the (exercise, date, time, set number) keys History shows; [existingMinutes] the
     * (exercise, date, time) triples it has any set for.
     */
    fun plan(
        rows: List<Row>,
        groups: List<ExerciseGroup>,
        existingKeys: Set<List<Any>>,
        existingMinutes: Set<List<Any>>,
        zone: ZoneId,
        nowEpochMillis: Long,
    ): Plan {
        val groupIdByName = groups.associate { it.name to it.id }
        val seen = HashSet<List<Any>>()
        val skipped = mutableListOf<Row>()
        val candidates = mutableListOf<Row>()
        for (row in rows) {
            if (row.key in existingKeys || !seen.add(row.key)) skipped += row else candidates += row
        }

        val legacy = mutableListOf<Row>()
        val refused = mutableListOf<Row>()
        val minuteOwner = HashMap<List<Any>, Triple<String, String, String>>()
        val sessions = candidates
            .groupBy { Triple(it.date, it.time, it.comment) }
            .entries
            .sortedWith(compareBy({ epochMillis(it.value.first().date, it.value.first().time, zone) ?: Long.MAX_VALUE }, { it.value.first().line }))
            .mapNotNull { (groupKey, batch) ->
                val startedAt = epochMillis(batch.first().date, batch.first().time, zone)
                val planned = mutableListOf<PlannedExercise>()
                batch.groupBy { it.exercise.id }.values.sortedBy { it.first().line }.forEach { exerciseRows ->
                    val exercise = exerciseRows.first().exercise
                    val kind = when (exercise.type) { "Dynamic" -> ExerciseKind.DYNAMIC; "Isometric" -> ExerciseKind.ISOMETRIC; else -> null }
                    val laterality = when (exercise.laterality) { "Unilateral" -> Laterality.UNILATERAL; "Bilateral" -> Laterality.BILATERAL; else -> null }
                    val minute = listOf(exercise.id, groupKey.first, groupKey.second)
                    val fits = startedAt != null && kind != null && laterality != null &&
                        minute !in existingMinutes && (minuteOwner[minute] ?: groupKey) == groupKey
                    if (!fits) {
                        exerciseRows.forEach { if (it.hasMarks || it.sidesDiffer) refused += it else legacy += it }
                        return@forEach
                    }
                    minuteOwner[minute] = groupKey
                    var orderIndex = 0
                    val sets = exerciseRows.sortedWith(compareBy({ it.setNumber }, { it.line })).flatMap { r ->
                        val sides = when {
                            r.valueLeft != null -> listOf(BodySide.RIGHT to r.valueRight, BodySide.LEFT to r.valueLeft)
                            laterality == Laterality.UNILATERAL -> listOf(BodySide.RIGHT to r.valueRight)
                            else -> listOf(BodySide.BILATERAL to r.valueRight)
                        }
                        sides.map { (side, value) ->
                            SetEntryEntity(
                                sessionExerciseId = 0,
                                orderIndex = orderIndex++,
                                setNumber = r.setNumber,
                                status = SetEntryStatus.COMPLETED,
                                side = side,
                                repetitions = if (kind == ExerciseKind.DYNAMIC) value else null,
                                durationMillis = if (kind == ExerciseKind.ISOMETRIC) value * 1_000L else null,
                                // The left side has its own value when the row gives one; empty means the same as the right.
                                distanceCm = if (side == BodySide.LEFT) r.distanceLeftCm ?: r.distanceCm else r.distanceCm,
                                addedWeightGrams = if (side == BodySide.LEFT) r.weightLeftG ?: r.weightG else r.weightG,
                                assistanceGrams = if (side == BodySide.LEFT) r.assistanceLeftG ?: r.assistanceG else r.assistanceG,
                                timePrecision = TimePrecision.MINUTE,
                                isWarmUp = r.warmUp,
                                effortLeft = r.effortLeft,
                            )
                        }
                    }
                    planned += PlannedExercise(
                        SessionExerciseEntity(
                            workoutSessionId = 0,
                            orderIndex = planned.size,
                            exerciseId = exercise.id,
                            groupId = exercise.group?.let(groupIdByName::get),
                            exerciseNameSnapshot = exercise.name,
                            exerciseKindSnapshot = kind!!,
                            lateralitySnapshot = laterality!!,
                            groupNameSnapshot = exercise.group,
                        ),
                        sets,
                    )
                }
                if (planned.isEmpty()) null
                else PlannedSession(
                    WorkoutSessionEntity(
                        status = WorkoutSessionStatus.COMPLETED,
                        sourceType = WorkoutSourceType.LEGACY_IMPORT,
                        startedAtEpochMillis = startedAt!!,
                        updatedAtEpochMillis = nowEpochMillis,
                        timePrecision = TimePrecision.MINUTE,
                        comment = batch.first().comment.ifEmpty { null },
                    ),
                    planned,
                )
            }
        return Plan(sessions, legacy, skipped, refused)
    }

    /** Writes a [plan] in one transaction: all of it or none of it. */
    suspend fun write(database: AppDatabase, plan: Plan) = database.withTransaction {
        val dao = database.workoutSessionDao()
        for (planned in plan.sessions) {
            val sessionId = dao.insertSession(planned.session)
            for (exercise in planned.exercises) {
                val exerciseId = dao.insertSessionExercise(exercise.exercise.copy(workoutSessionId = sessionId))
                dao.insertSetEntries(exercise.sets.map { it.copy(sessionExerciseId = exerciseId) })
            }
        }
        for (r in plan.legacy) {
            database.trainingRecordDao().insertRecord(
                TrainingRecord(
                    exerciseId = r.exercise.id, valueRight = r.valueRight, valueLeft = r.valueLeft, setNumber = r.setNumber,
                    date = r.date, time = r.time, comment = r.comment, distanceCm = r.distanceCm, weightG = r.weightG, assistanceG = r.assistanceG,
                ),
            )
        }
    }

    private fun epochMillis(date: String, time: String, zone: ZoneId): Long? = try {
        LocalDate.parse(date, DATE).atTime(LocalTime.parse(time, TIME)).atZone(zone).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }
}
