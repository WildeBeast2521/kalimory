package io.github.gonbei774.calisthenicsmemory.data.v2

import androidx.room.withTransaction
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Loads a saved workout into a [WorkoutDraft] and saves an edited draft back (ADR 0009).
 * Each save is one transaction. It first checks that the workout is still what was loaded,
 * and writes only what the user entered: no set time, target or grouping is invented.
 */
object WorkoutEditor {
    private val DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
    private val TIME = DateTimeFormatter.ofPattern("HH:mm")

    // ---- v2 workouts ----

    /** The editable part of a v2 workout, or null when there is none (as for interval workouts). */
    suspend fun loadV2(database: AppDatabase, sessionId: Long, zone: ZoneId): WorkoutDraft? =
        database.workoutSessionDao().sessionGraph(sessionId)?.let { draftOf(it, zone) }

    /**
     * What History shows of [graph]: its completed sets of exercises still in the library, without
     * the hidden copies of legacy records. Skipped and pending sets are left out, and so they stay.
     * Null for an interval workout, which has its own editor, or when nothing is shown.
     */
    fun draftOf(graph: WorkoutSessionGraph, zone: ZoneId): WorkoutDraft? {
        if (graph.session.sourceType == WorkoutSourceType.INTERVAL_TEMPLATE) return null
        val start = Instant.ofEpochMilli(graph.session.startedAtEpochMillis).atZone(zone)
        val exercises = graph.exercises.mapNotNull { (occurrence, entries) ->
            val exerciseId = occurrence.exerciseId ?: return@mapNotNull null
            val shown = entries.filter { it.status == SetEntryStatus.COMPLETED && it.legacyTrainingRecordId == null }
            if (shown.isEmpty()) return@mapNotNull null
            val kind = occurrence.exerciseKindSnapshot
            val sets = shown.groupBy { it.roundNumber to it.setNumber }.values
                .map { sides -> sides.sortedBy { it.orderIndex } }
                .sortedBy { it.first().orderIndex }
                .map { sides ->
                    val right = sides.firstOrNull { it.side != BodySide.LEFT }
                    val left = sides.firstOrNull { it.side == BodySide.LEFT }
                    val primary = right ?: left!!
                    DraftSet(
                        ids = sides.map { it.id },
                        value = right?.let { value(it, kind) },
                        valueLeft = left?.let { value(it, kind) },
                        distanceCm = primary.distanceCm,
                        weightG = primary.addedWeightGrams,
                        assistanceG = primary.assistanceGrams,
                    )
                }
            DraftExercise(
                key = occurrence.id,
                exerciseId = exerciseId,
                // A left entry means the set was recorded per side, whatever the snapshot says.
                unilateral = occurrence.lateralitySnapshot == Laterality.UNILATERAL || shown.any { it.side == BodySide.LEFT },
                sets = sets,
            )
        }
        if (exercises.isEmpty()) return null
        return WorkoutDraft(
            date = start.toLocalDate(),
            time = start.toLocalTime().truncatedTo(ChronoUnit.MINUTES),
            comment = graph.session.comment.orEmpty(),
            exercises = exercises,
        )
    }

    /**
     * Saves [edited] over the v2 workout [sessionId], which was loaded as [original]. Exercises added
     * in the editor are looked up in [library], and their groups in [groups].
     */
    suspend fun saveV2(
        database: AppDatabase,
        sessionId: Long,
        original: WorkoutDraft,
        edited: WorkoutDraft,
        library: List<Exercise>,
        groups: List<ExerciseGroup>,
        zone: ZoneId,
        nowEpochMillis: Long,
    ): WorkoutSaveResult {
        val dao = database.workoutSessionDao()
        return database.withTransaction {
            val graph = dao.sessionGraph(sessionId) ?: return@withTransaction WorkoutSaveResult.STALE
            if (draftOf(graph, zone) != original) return@withTransaction WorkoutSaveResult.STALE
            val plan = WorkoutEditPlan.of(original, edited)
            if (plan.isEmpty) return@withTransaction WorkoutSaveResult.UNCHANGED

            if (plan.deleteWorkout) {
                dao.deleteSessionExercises(original.exercises.map { it.key!! })
                // Keep the workout when it still holds sets the editor does not show.
                val rest = dao.sessionGraph(sessionId)!!
                return@withTransaction if (rest.exercises.none { (_, sets) -> sets.any { it.status == SetEntryStatus.COMPLETED } }) {
                    dao.deleteSessions(listOf(sessionId))
                    WorkoutSaveResult.DELETED
                } else {
                    V2HistoryEditor.moveSession(database, sessionId, graph.session.startedAtEpochMillis, graph.session.comment.orEmpty(), nowEpochMillis)
                    WorkoutSaveResult.SAVED
                }
            }

            // Unmoved workouts keep their exact start; a moved one starts at the entered minute.
            val start = if (plan.move) {
                edited.date.atTime(edited.time).atZone(zone).toInstant().toEpochMilli()
            } else graph.session.startedAtEpochMillis
            V2HistoryEditor.moveSession(database, sessionId, start, edited.comment, nowEpochMillis)

            dao.deleteSessionExercises(plan.removedExercises)
            dao.deleteSetEntries(plan.removedSets.flatMap { it.ids })

            val occurrences = graph.exercises.map { it.first }.associateBy { it.id }
            val unilateralOf = original.exercises.associate { it.key!! to it.unilateral }
            // Next free order index and set number of each occurrence, after the deletions above.
            val nextOrder = mutableMapOf<Long, Int>()
            val nextSet = mutableMapOf<Long, Int>()
            suspend fun reserve(occurrenceId: Long): Pair<Int, Int> {
                if (occurrenceId !in nextOrder) {
                    val entries = dao.setEntries(occurrenceId)
                    nextOrder[occurrenceId] = (entries.maxOfOrNull { it.orderIndex } ?: -1) + 1
                    nextSet[occurrenceId] = (entries.maxOfOrNull { it.setNumber } ?: 0) + 1
                }
                return nextOrder.getValue(occurrenceId) to nextSet.getValue(occurrenceId)
            }
            suspend fun takeOrder(occurrenceId: Long): Int {
                val order = reserve(occurrenceId).first
                nextOrder[occurrenceId] = order + 1
                return order
            }
            suspend fun takeSetNumber(occurrenceId: Long): Int {
                val number = reserve(occurrenceId).second
                nextSet[occurrenceId] = number + 1
                return number
            }

            for ((before, after) in plan.changedSets) {
                val entries = dao.setEntriesByIds(before.ids)
                val occurrence = occurrences.getValue(entries.first().sessionExerciseId)
                val kind = occurrence.exerciseKindSnapshot
                val unilateral = unilateralOf.getValue(occurrence.id)
                val right = entries.firstOrNull { it.side != BodySide.LEFT }
                val left = entries.firstOrNull { it.side == BodySide.LEFT }
                val template = right ?: left!!
                val updates = mutableListOf<SetEntryEntity>()
                val deletes = mutableListOf<Long>()
                val inserts = mutableListOf<SetEntryEntity>()
                fun side(existing: SetEntryEntity?, wanted: Int?, newSide: BodySide) {
                    when {
                        existing != null && wanted == null -> deletes += existing.id
                        existing != null -> updates += withMetrics(withValue(existing, wanted!!, kind), after)
                        wanted != null -> inserts += newEntry(
                            occurrence.id, -1, template.setNumber, template.roundNumber, newSide, wanted, after, kind,
                        )
                    }
                }
                side(right, after.value, if (unilateral) BodySide.RIGHT else BodySide.BILATERAL)
                if (unilateral) side(left, after.valueLeft, BodySide.LEFT)
                dao.deleteSetEntries(deletes)
                dao.updateSetEntries(updates)
                dao.insertSetEntries(inserts.map { it.copy(orderIndex = takeOrder(occurrence.id)) })
            }

            for ((key, set) in plan.addedSets) {
                val occurrence = occurrences.getValue(key)
                val number = takeSetNumber(key)
                val sides = sidesOf(set, unilateralOf.getValue(key))
                dao.insertSetEntries(sides.map { (side, value) ->
                    newEntry(key, takeOrder(key), number, null, side, value, set, occurrence.exerciseKindSnapshot)
                })
            }

            var nextOccurrence = (graph.exercises.maxOfOrNull { it.first.orderIndex } ?: -1) + 1
            for (added in plan.addedExercises) {
                val exercise = requireNotNull(library.find { it.id == added.exerciseId }) { "Exercise ${added.exerciseId} is not in the library" }
                val kind = if (exercise.type == "Isometric") ExerciseKind.ISOMETRIC else ExerciseKind.DYNAMIC
                val laterality = if (exercise.laterality == "Unilateral") Laterality.UNILATERAL else Laterality.BILATERAL
                require(added.unilateral == (laterality == Laterality.UNILATERAL)) { "Exercise ${exercise.id} laterality differs" }
                val occurrenceId = dao.insertSessionExercise(
                    SessionExerciseEntity(
                        workoutSessionId = sessionId,
                        orderIndex = nextOccurrence++,
                        exerciseId = exercise.id,
                        groupId = exercise.group?.let { name -> groups.find { it.name == name }?.id },
                        exerciseNameSnapshot = exercise.name,
                        exerciseKindSnapshot = kind,
                        lateralitySnapshot = laterality,
                        groupNameSnapshot = exercise.group,
                    )
                )
                var order = 0
                dao.insertSetEntries(added.sets.flatMapIndexed { index, set ->
                    sidesOf(set, added.unilateral).map { (side, value) ->
                        newEntry(occurrenceId, order++, index + 1, null, side, value, set, kind)
                    }
                })
            }
            WorkoutSaveResult.SAVED
        }
    }

    private fun value(entry: SetEntryEntity, kind: ExerciseKind): Int = when (kind) {
        ExerciseKind.DYNAMIC -> entry.repetitions ?: 0
        ExerciseKind.ISOMETRIC -> ((entry.durationMillis ?: 0) / 1_000).toInt()
    }

    // Keeps sub-second precision when the whole-second value is unchanged, as V2HistoryEditor does.
    private fun withValue(entry: SetEntryEntity, value: Int, kind: ExerciseKind): SetEntryEntity = when (kind) {
        ExerciseKind.DYNAMIC -> entry.copy(repetitions = value)
        ExerciseKind.ISOMETRIC ->
            if ((entry.durationMillis ?: -1) / 1_000 == value.toLong()) entry else entry.copy(durationMillis = value * 1_000L)
    }

    private fun withMetrics(entry: SetEntryEntity, set: DraftSet) =
        entry.copy(distanceCm = set.distanceCm, addedWeightGrams = set.weightG, assistanceGrams = set.assistanceG)

    private fun sidesOf(set: DraftSet, unilateral: Boolean): List<Pair<BodySide, Int>> =
        if (unilateral) listOfNotNull(set.value?.let { BodySide.RIGHT to it }, set.valueLeft?.let { BodySide.LEFT to it })
        else listOf(BodySide.BILATERAL to set.value!!)

    /** A set entered in the editor: completed, with no time, as hand-entered sets are saved. */
    private fun newEntry(
        occurrenceId: Long, order: Int, setNumber: Int, round: Int?, side: BodySide, value: Int, set: DraftSet, kind: ExerciseKind,
    ) = SetEntryEntity(
        sessionExerciseId = occurrenceId,
        orderIndex = order,
        setNumber = setNumber,
        roundNumber = round,
        status = SetEntryStatus.COMPLETED,
        side = side,
        repetitions = if (kind == ExerciseKind.DYNAMIC) value else null,
        durationMillis = if (kind == ExerciseKind.ISOMETRIC) value * 1_000L else null,
        distanceCm = set.distanceCm,
        addedWeightGrams = set.weightG,
        assistanceGrams = set.assistanceG,
        timePrecision = TimePrecision.MINUTE,
    )

    // ---- legacy workouts ----

    /**
     * The legacy workout saved at [date], [time] and [comment]: every record one save stamped with
     * them (the grouping V2Backfill uses). Null when there is none, or when the stored date or time
     * is not in the form the screens write, since it could not be written back unchanged.
     */
    suspend fun loadLegacy(database: AppDatabase, date: String, time: String, comment: String, library: List<Exercise>): WorkoutDraft? =
        draftOf(database.trainingRecordDao().workoutRecords(date, time, comment), library)

    /** [records] of one legacy workout as a draft: exercises in the order first recorded, sets by number. */
    fun draftOf(records: List<TrainingRecord>, library: List<Exercise>): WorkoutDraft? {
        val first = records.minByOrNull { it.id } ?: return null
        val date = runCatching { LocalDate.parse(first.date, DATE) }.getOrNull()?.takeIf { it.format(DATE) == first.date } ?: return null
        val time = runCatching { LocalTime.parse(first.time, TIME) }.getOrNull()?.takeIf { it.format(TIME) == first.time } ?: return null
        val laterality = library.associate { it.id to it.laterality }
        val exercises = records.sortedBy { it.id }.groupBy { it.exerciseId }.map { (exerciseId, sets) ->
            DraftExercise(
                key = exerciseId,
                exerciseId = exerciseId,
                // A left value means the set was recorded per side, whatever the exercise is now.
                unilateral = laterality[exerciseId] == "Unilateral" || sets.any { it.valueLeft != null },
                sets = sets.sortedWith(compareBy({ it.setNumber }, { it.id })).map {
                    DraftSet(listOf(it.id), it.valueRight, it.valueLeft, it.distanceCm, it.weightG, it.assistanceG)
                },
            )
        }
        return WorkoutDraft(date, time, first.comment, exercises)
    }

    /**
     * Saves [edited] over the legacy workout loaded as [original], as legacy records (ADR 0009,
     * decision 5). Each exercise's sets are numbered 1 to n in the order shown, as a legacy save
     * numbers them. The hidden v2 copies of changed or removed records are dropped, so they never
     * contradict the records or point at a deleted one.
     */
    suspend fun saveLegacy(
        database: AppDatabase,
        original: WorkoutDraft,
        edited: WorkoutDraft,
        library: List<Exercise>,
    ): WorkoutSaveResult {
        val records = database.trainingRecordDao()
        return database.withTransaction {
            val loaded = records.workoutRecords(original.date.format(DATE), original.time.format(TIME), original.comment)
            if (draftOf(loaded, library) != original) return@withTransaction WorkoutSaveResult.STALE
            val plan = WorkoutEditPlan.of(original, edited)
            if (plan.isEmpty) return@withTransaction WorkoutSaveResult.UNCHANGED
            val loadedIds = loaded.map { it.id }

            if (plan.deleteWorkout) {
                records.deleteRecordsByIds(loadedIds)
                dropV2Copies(database, loadedIds)
                return@withTransaction WorkoutSaveResult.DELETED
            }

            val date = edited.date.format(DATE)
            val time = edited.time.format(TIME)
            // A legacy workout is only its date, minute and comment: landing on another would merge them.
            if ((plan.move || plan.commentChanged) &&
                records.workoutRecords(date, time, edited.comment).any { it.id !in loadedIds }
            ) return@withTransaction WorkoutSaveResult.TIME_TAKEN

            val byId = loaded.associateBy { it.id }
            val wanted = edited.exercises.filter { it.sets.isNotEmpty() }
                .groupBy { it.exerciseId }
                .flatMap { (exerciseId, exercises) ->
                    exercises.flatMap { it.sets }.mapIndexed { index, set ->
                        require(set.ids.size <= 1) { "A legacy set is one record" }
                        TrainingRecord(
                            id = set.ids.singleOrNull() ?: 0,
                            exerciseId = exerciseId,
                            valueRight = requireNotNull(set.value) { "A legacy set has a right value" },
                            valueLeft = set.valueLeft,
                            setNumber = index + 1,
                            date = date,
                            time = time,
                            comment = edited.comment,
                            distanceCm = set.distanceCm,
                            weightG = set.weightG,
                            assistanceG = set.assistanceG,
                        )
                    }
                }
            val kept = wanted.filter { it.id != 0L }
            val updated = kept.filter { it != byId[it.id] }
            val removed = loadedIds - kept.map { it.id }.toSet()
            records.deleteRecordsByIds(removed)
            records.updateRecords(updated)
            records.insertRecords(wanted.filter { it.id == 0L })
            dropV2Copies(database, removed + updated.map { it.id })
            WorkoutSaveResult.SAVED
        }
    }

    // Call inside a transaction.
    private suspend fun dropV2Copies(database: AppDatabase, recordIds: List<Long>) {
        if (recordIds.isEmpty()) return
        val dao = database.workoutSessionDao()
        val copies = dao.setEntriesLinkedTo(recordIds)
        if (copies.isEmpty()) return
        val occurrenceIds = copies.map { it.sessionExerciseId }.distinct()
        val sessionIds = dao.sessionExercisesByIds(occurrenceIds).map { it.workoutSessionId }.distinct()
        dao.deleteSetEntries(copies.map { it.id })
        dao.deleteEmptySessionExercises(occurrenceIds)
        dao.deleteEmptySessions(sessionIds)
    }
}
