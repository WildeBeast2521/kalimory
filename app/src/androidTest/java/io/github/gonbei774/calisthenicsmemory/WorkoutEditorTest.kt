package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.data.progression.Progressions
import io.github.gonbei774.calisthenicsmemory.data.v2.BodySide
import io.github.gonbei774.calisthenicsmemory.data.v2.CompatibilityHistory
import io.github.gonbei774.calisthenicsmemory.data.v2.DraftExercise
import io.github.gonbei774.calisthenicsmemory.data.v2.DraftSet
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.v2.Laterality
import io.github.gonbei774.calisthenicsmemory.data.v2.SessionExerciseEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.TimePrecision
import io.github.gonbei774.calisthenicsmemory.data.v2.V2HistoryEditor
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutDraft
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutEditor
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSaveResult
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSourceType
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSummaryBuilder
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupResult
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Editing whole saved workouts, v2 and legacy, against a real database (ADR 0009). */
@RunWith(AndroidJUnit4::class)
class WorkoutEditorTest {
    private lateinit var database: AppDatabase
    private val zone = ZoneId.of("Asia/Kolkata")
    private val startedAt = ZonedDateTime.of(2026, 3, 2, 7, 30, 42, 0, zone).toInstant().toEpochMilli()
    private val groups = listOf(ExerciseGroup(1, "Push", 0), ExerciseGroup(2, "Core", 1))
    private val library = listOf(
        // Its own move-on standard, 2 sets of 10, so one set decides whether it is met.
        Exercise(1, "Push-up", "Dynamic", "Push", targetSets = 2, targetValue = 10, catalogId = "push.incline"),
        Exercise(2, "Lunge", "Dynamic", laterality = "Unilateral"),
        Exercise(3, "Plank", "Isometric", "Core"),
    )
    private var sessionId = 0L
    private var pushUpId = 0L
    private var lungeId = 0L

    @Before
    fun setUp(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        groups.forEach { database.exerciseGroupDao().insertGroup(it) }
        library.forEach { database.exerciseDao().insertExercise(it) }
        val dao = database.workoutSessionDao()
        sessionId = dao.insertSession(
            WorkoutSessionEntity(
                status = WorkoutSessionStatus.COMPLETED, sourceType = WorkoutSourceType.PROGRAM_TEMPLATE,
                sourceTemplateId = 7, sourceNameSnapshot = "Upper body",
                startedAtEpochMillis = startedAt, endedAtEpochMillis = startedAt + 1_200_000,
                updatedAtEpochMillis = startedAt, timePrecision = TimePrecision.EXACT, comment = "Morning",
            )
        )
        pushUpId = dao.insertSessionExercise(occurrence(0, 1, "Push-up", ExerciseKind.DYNAMIC, Laterality.BILATERAL))
        lungeId = dao.insertSessionExercise(occurrence(1, 2, "Lunge", ExerciseKind.DYNAMIC, Laterality.UNILATERAL))
        dao.insertSetEntries(
            listOf(
                entry(pushUpId, 0, 1, BodySide.BILATERAL, 12),
                entry(pushUpId, 1, 2, BodySide.BILATERAL, 10),
                entry(pushUpId, 2, 3, BodySide.BILATERAL, null, SetEntryStatus.SKIPPED),
                entry(lungeId, 0, 1, BodySide.RIGHT, 8),
                entry(lungeId, 1, 1, BodySide.LEFT, 7),
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun occurrence(order: Int, exerciseId: Long?, name: String, kind: ExerciseKind, laterality: Laterality) =
        SessionExerciseEntity(
            workoutSessionId = sessionId, orderIndex = order, exerciseId = exerciseId, exerciseNameSnapshot = name,
            exerciseKindSnapshot = kind, lateralitySnapshot = laterality, targetSets = 3, targetRepetitions = 10,
        )

    private fun entry(parent: Long, order: Int, number: Int, side: BodySide, reps: Int?, status: SetEntryStatus = SetEntryStatus.COMPLETED) =
        SetEntryEntity(
            sessionExerciseId = parent, orderIndex = order, setNumber = number, status = status, side = side,
            repetitions = reps, targetRepetitions = 10, startedAtEpochMillis = startedAt + 60_000L * order,
            completedAtEpochMillis = startedAt + 60_000L * order + 30_000, timePrecision = TimePrecision.EXACT,
        )

    private suspend fun loadV2() = WorkoutEditor.loadV2(database, sessionId, zone)!!
    private suspend fun saveV2(original: WorkoutDraft, edited: WorkoutDraft) =
        WorkoutEditor.saveV2(database, sessionId, original, edited, library, groups, zone, nowEpochMillis = 999)
    private suspend fun entries(occurrence: Long) = database.workoutSessionDao().setEntries(occurrence)
    private suspend fun history() = CompatibilityHistory.read(database, zone)
    private suspend fun shownValues(exerciseId: Long) =
        history().filter { it.exerciseId == exerciseId }.sortedBy { it.setNumber }.map { it.valueRight to it.valueLeft }

    private fun WorkoutDraft.withExercise(key: Long, change: (DraftExercise) -> DraftExercise?) =
        copy(exercises = exercises.mapNotNull { if (it.key == key) change(it) else it })

    // ---- v2 ----

    @Test
    fun anUnchangedDraftWritesNothing() = runBlocking {
        val loaded = loadV2()
        assertEquals(WorkoutSaveResult.UNCHANGED, saveV2(loaded, loaded))
        assertEquals(startedAt, database.workoutSessionDao().session(sessionId)!!.updatedAtEpochMillis)
    }

    @Test
    fun setsAreAddedAndRemovedWithoutInventingTimesOrTargets() = runBlocking {
        val loaded = loadV2()
        val edited = loaded.withExercise(pushUpId) { it.copy(sets = listOf(it.sets[0], DraftSet(value = 11))) }
        assertEquals(WorkoutSaveResult.SAVED, saveV2(loaded, edited))

        val rows = entries(pushUpId)
        // The removed set is gone; the skipped one stays; the new one follows them.
        assertEquals(listOf(12, null, 11), rows.map { it.repetitions })
        assertEquals(listOf(SetEntryStatus.COMPLETED, SetEntryStatus.SKIPPED, SetEntryStatus.COMPLETED), rows.map { it.status })
        val added = rows.last()
        assertEquals(3 to 4, added.orderIndex to added.setNumber)
        assertNull(added.roundNumber)
        assertNull(added.startedAtEpochMillis)
        assertNull(added.completedAtEpochMillis)
        assertNull(added.targetRepetitions)
        assertEquals(TimePrecision.MINUTE, added.timePrecision)
        assertEquals(listOf(12 to null, 11 to null), shownValues(1))
        assertEquals(999L, database.workoutSessionDao().session(sessionId)!!.updatedAtEpochMillis)
    }

    @Test
    fun aUnilateralSetLosesAndRegainsItsLeftSide() = runBlocking {
        val loaded = loadV2()
        val rightOnly = loaded.withExercise(lungeId) { it.copy(sets = listOf(it.sets[0].copy(valueLeft = null))) }
        assertEquals(WorkoutSaveResult.SAVED, saveV2(loaded, rightOnly))
        assertEquals(listOf(BodySide.RIGHT), entries(lungeId).map { it.side })

        val reloaded = loadV2()
        val both = reloaded.withExercise(lungeId) { it.copy(sets = listOf(it.sets[0].copy(value = 9, valueLeft = 6))) }
        assertEquals(WorkoutSaveResult.SAVED, saveV2(reloaded, both))
        val rows = entries(lungeId)
        assertEquals(listOf(BodySide.RIGHT to 9, BodySide.LEFT to 6), rows.map { it.side to it.repetitions })
        // The new side belongs to the same set and has no time of its own.
        assertEquals(1, rows.last().setNumber)
        assertNull(rows.last().completedAtEpochMillis)
        assertEquals(listOf(9 to 6), shownValues(2))
    }

    @Test
    fun exercisesAreAddedWithSnapshotsAndRemovedWithTheirSets() = runBlocking {
        val loaded = loadV2()
        val edited = loaded.copy(
            exercises = loaded.exercises.filter { it.key != lungeId } +
                DraftExercise(null, 3, false, listOf(DraftSet(value = 60), DraftSet(value = 45))),
        )
        assertEquals(WorkoutSaveResult.SAVED, saveV2(loaded, edited))

        val occurrences = database.workoutSessionDao().sessionExercises(sessionId)
        assertEquals(listOf(pushUpId), occurrences.dropLast(1).map { it.id })
        val plank = occurrences.last()
        assertEquals(2, plank.orderIndex)
        assertEquals(3L to 2L, plank.exerciseId to plank.groupId)
        assertEquals("Plank" to "Core", plank.exerciseNameSnapshot to plank.groupNameSnapshot)
        assertEquals(ExerciseKind.ISOMETRIC to Laterality.BILATERAL, plank.exerciseKindSnapshot to plank.lateralitySnapshot)
        assertNull(plank.targetSets)
        assertEquals(listOf(60_000L, 45_000L), entries(plank.id).map { it.durationMillis })
        assertEquals(listOf(1, 2), entries(plank.id).map { it.setNumber })
        assertTrue(entries(lungeId).isEmpty())
        assertEquals(listOf(60 to null, 45 to null), shownValues(3))
        assertTrue(shownValues(2).isEmpty())
    }

    @Test
    fun aCommentEditKeepsTheExactStartAndAMoveShiftsEverySet() = runBlocking {
        val loaded = loadV2()
        assertEquals(LocalTime.of(7, 30), loaded.time)
        assertEquals(WorkoutSaveResult.SAVED, saveV2(loaded, loaded.copy(comment = "Felt strong")))
        database.workoutSessionDao().session(sessionId)!!.let {
            assertEquals(startedAt, it.startedAtEpochMillis)
            assertEquals(TimePrecision.EXACT, it.timePrecision)
            assertEquals("Felt strong", it.comment)
        }

        val commented = loadV2()
        val before = entries(pushUpId).map { it.completedAtEpochMillis }
        assertEquals(WorkoutSaveResult.SAVED, saveV2(commented, commented.copy(date = LocalDate.of(2026, 3, 4), time = LocalTime.of(18, 5))))
        val moved = ZonedDateTime.of(2026, 3, 4, 18, 5, 0, 0, zone).toInstant().toEpochMilli()
        val session = database.workoutSessionDao().session(sessionId)!!
        assertEquals(moved, session.startedAtEpochMillis)
        assertEquals(TimePrecision.MINUTE, session.timePrecision)
        assertEquals(before.map { it?.plus(moved - startedAt) }, entries(pushUpId).map { it.completedAtEpochMillis })
        assertFalse(CompatibilityHistory.hasSetOn(database, 1, LocalDate.of(2026, 3, 2), zone))
        assertTrue(CompatibilityHistory.hasSetOn(database, 1, LocalDate.of(2026, 3, 4), zone))
    }

    @Test
    fun aDraftOfAWorkoutThatChangedSinceLoadingIsRefused() = runBlocking {
        val loaded = loadV2()
        V2HistoryEditor.updateSetValues(database, history().first { it.exerciseId == 1L && it.valueRight == 12 }, 15, null)
        val edited = loaded.withExercise(pushUpId) { null }
        assertEquals(WorkoutSaveResult.STALE, saveV2(loaded, edited))
        assertEquals(listOf(15 to null, 10 to null), shownValues(1))
        assertEquals(listOf(8 to 7), shownValues(2))

        database.workoutSessionDao().deleteSessions(listOf(sessionId))
        assertEquals(WorkoutSaveResult.STALE, saveV2(loaded, edited))
    }

    @Test
    fun aFailedSaveChangesNothing() = runBlocking {
        val loaded = loadV2()
        val before = database.backupDao().snapshot()
        // The removal is written first; the unknown exercise then fails the save.
        val edited = loaded.withExercise(lungeId) { null }.let {
            it.copy(exercises = it.exercises + DraftExercise(null, 42, false, listOf(DraftSet(value = 5))))
        }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { saveV2(loaded, edited) } }
        assertEquals(before, database.backupDao().snapshot())
    }

    @Test
    fun removingEverySetDeletesTheWorkout() = runBlocking {
        val loaded = loadV2()
        assertEquals(WorkoutSaveResult.DELETED, saveV2(loaded, loaded.copy(exercises = emptyList())))
        assertNull(database.workoutSessionDao().session(sessionId))
        assertTrue(history().isEmpty())
    }

    @Test
    fun setsTheEditorDoesNotShowKeepTheWorkout() = runBlocking {
        val dao = database.workoutSessionDao()
        // An exercise since deleted from the library: History no longer shows it, but its sets are kept.
        val orphan = dao.insertSessionExercise(occurrence(2, null, "Dips", ExerciseKind.DYNAMIC, Laterality.BILATERAL))
        dao.insertSetEntries(listOf(entry(orphan, 0, 1, BodySide.BILATERAL, 6)))
        val loaded = loadV2()
        assertEquals(WorkoutSaveResult.SAVED, saveV2(loaded, loaded.copy(exercises = emptyList())))
        assertEquals(listOf(orphan), dao.sessionExercises(sessionId).map { it.id })
        assertNotNull(dao.session(sessionId))
    }

    @Test
    fun anEditedWorkoutSurvivesBackupAndRestore() = runBlocking {
        val loaded = loadV2()
        saveV2(
            loaded,
            loaded.withExercise(lungeId) { it.copy(sets = it.sets + DraftSet(value = 5, valueLeft = 4, weightG = 2_500)) }
                .let { it.copy(comment = "Edited", exercises = it.exercises + DraftExercise(null, 3, false, listOf(DraftSet(value = 30)))) },
        )
        assertRoundTrip()
    }

    @Test
    fun progressionsAndBestsFollowTheCorrectedHistory() = runBlocking {
        fun mastered(history: List<io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet>) =
            Progressions.chains(library, history).single { it.chain.id == "push" }.mastered
        assertTrue(mastered(history()))
        val loaded = loadV2()
        // The 10 met the 2 x 10 standard; corrected to 9, the step is no longer met.
        saveV2(loaded, loaded.withExercise(pushUpId) { it.copy(sets = listOf(it.sets[0], it.sets[1].copy(value = 9))) })
        assertFalse(mastered(history()))

        // A later workout of 13 push-ups is a best against 12...
        val dao = database.workoutSessionDao()
        val later = dao.insertSession(
            WorkoutSessionEntity(
                status = WorkoutSessionStatus.COMPLETED, sourceType = WorkoutSourceType.AD_HOC,
                startedAtEpochMillis = startedAt + 86_400_000, updatedAtEpochMillis = startedAt + 86_400_000,
                timePrecision = TimePrecision.EXACT,
            )
        )
        val laterPushUp = dao.insertSessionExercise(occurrence(0, 1, "Push-up", ExerciseKind.DYNAMIC, Laterality.BILATERAL).copy(workoutSessionId = later))
        dao.insertSetEntries(listOf(entry(laterPushUp, 0, 1, BodySide.BILATERAL, 13)))
        fun best() = runBlocking { WorkoutSummaryBuilder.build(dao.sessionGraph(later)!!, history()).personalBests.map { it.previousBest } }
        assertEquals(listOf(12), best())
        // ...until the earlier workout is corrected to hold 14.
        val corrected = loadV2()
        saveV2(corrected, corrected.withExercise(pushUpId) { it.copy(sets = it.sets + DraftSet(value = 14)) })
        assertEquals(emptyList<Int>(), best())
    }

    private suspend fun assertRoundTrip() {
        val exported = BackupService(database.backupDao()).export()
        val json = (exported as BackupResult.Success).value.json
        val context = ApplicationProvider.getApplicationContext<Context>()
        val target = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val service = BackupService(target.backupDao())
            val parsed = service.parse(json)
            assertTrue("parse: $parsed", parsed is BackupResult.Success)
            val restored = service.restore((parsed as BackupResult.Success).value.data)
            assertTrue("restore: $restored", restored is BackupResult.Success)
            assertEquals(database.backupDao().snapshot(), target.backupDao().snapshot())
            assertEquals(history(), CompatibilityHistory.read(target, zone))
        } finally {
            target.close()
        }
    }

    // ---- legacy ----

    private suspend fun insertLegacyWorkouts() {
        val records = database.trainingRecordDao()
        records.insertRecords(
            listOf(
                TrainingRecord(101, 1, 12, null, 1, "2026-03-01", "18:00", "Legs"),
                TrainingRecord(102, 1, 10, null, 2, "2026-03-01", "18:00", "Legs"),
                TrainingRecord(103, 2, 8, 7, 1, "2026-03-01", "18:00", "Legs"),
                // Another workout in the next hour.
                TrainingRecord(201, 1, 5, null, 1, "2026-03-01", "19:00", ""),
            )
        )
    }

    private suspend fun loadLegacy() = WorkoutEditor.loadLegacy(database, "2026-03-01", "18:00", "Legs", library)!!
    private suspend fun legacyRecords() = database.backupDao().records().sortedBy { it.id }

    @Test
    fun aLegacyWorkoutIsEditedAsLegacyRecordsAndMovesWhole() = runBlocking {
        insertLegacyWorkouts()
        val loaded = loadLegacy()
        val edited = loaded.withExercise(1) { it.copy(sets = listOf(it.sets[1], DraftSet(value = 11))) }.let {
            it.copy(
                date = LocalDate.of(2026, 3, 3), time = LocalTime.of(6, 15), comment = "Moved",
                exercises = it.exercises + DraftExercise(null, 3, false, listOf(DraftSet(value = 30))),
            )
        }
        assertEquals(WorkoutSaveResult.SAVED, WorkoutEditor.saveLegacy(database, loaded, edited, library))

        val records = legacyRecords()
        assertEquals(
            listOf(
                TrainingRecord(102, 1, 10, null, 1, "2026-03-03", "06:15", "Moved"),
                TrainingRecord(103, 2, 8, 7, 1, "2026-03-03", "06:15", "Moved"),
                TrainingRecord(201, 1, 5, null, 1, "2026-03-01", "19:00", ""),
            ),
            records.filter { it.id in setOf(102L, 103L, 201L) },
        )
        val added = records.filter { it.id !in setOf(102L, 103L, 201L) }
        // Each exercise's sets are numbered 1 to n again, as a legacy save numbers them.
        assertEquals(listOf(Triple(1L, 2, 11), Triple(3L, 1, 30)), added.map { Triple(it.exerciseId, it.setNumber, it.valueRight) })
        assertTrue(added.all { it.date == "2026-03-03" && it.time == "06:15" && it.comment == "Moved" })
        assertTrue(database.workoutSessionDao().sessionsNewestFirst().none { it.id != sessionId })
        assertRoundTrip()
    }

    @Test
    fun aLegacyWorkoutCannotLandOnAnother() = runBlocking {
        insertLegacyWorkouts()
        val loaded = loadLegacy()
        val before = legacyRecords()
        val result = WorkoutEditor.saveLegacy(database, loaded, loaded.copy(time = LocalTime.of(19, 0), comment = ""), library)
        assertEquals(WorkoutSaveResult.TIME_TAKEN, result)
        assertEquals(before, legacyRecords())
    }

    @Test
    fun aStaleLegacyDraftIsRefused() = runBlocking {
        insertLegacyWorkouts()
        val loaded = loadLegacy()
        database.trainingRecordDao().updateRecord(TrainingRecord(101, 1, 13, null, 1, "2026-03-01", "18:00", "Legs"))
        val result = WorkoutEditor.saveLegacy(database, loaded, loaded.withExercise(1) { null }, library)
        assertEquals(WorkoutSaveResult.STALE, result)
        assertEquals(4, legacyRecords().size)
    }

    @Test
    fun aLegacyWorkoutWithoutSetsIsDeletedAlone() = runBlocking {
        insertLegacyWorkouts()
        val loaded = loadLegacy()
        assertEquals(WorkoutSaveResult.DELETED, WorkoutEditor.saveLegacy(database, loaded, loaded.copy(exercises = emptyList()), library))
        assertEquals(listOf(201L), legacyRecords().map { it.id })
    }

    @Test
    fun hiddenV2CopiesOfChangedLegacyRecordsAreDropped() = runBlocking {
        insertLegacyWorkouts()
        val dao = database.workoutSessionDao()
        // v2 copies of records 101 and 103 (ADR 0003), which History hides behind the records.
        val copy = dao.insertSession(
            WorkoutSessionEntity(
                status = WorkoutSessionStatus.COMPLETED, sourceType = WorkoutSourceType.LEGACY_IMPORT,
                startedAtEpochMillis = startedAt, updatedAtEpochMillis = startedAt, timePrecision = TimePrecision.MINUTE,
            )
        )
        val copyPushUp = dao.insertSessionExercise(occurrence(0, 1, "Push-up", ExerciseKind.DYNAMIC, Laterality.BILATERAL).copy(workoutSessionId = copy))
        val copyLunge = dao.insertSessionExercise(occurrence(1, 2, "Lunge", ExerciseKind.DYNAMIC, Laterality.UNILATERAL).copy(workoutSessionId = copy))
        dao.insertSetEntries(
            listOf(
                entry(copyPushUp, 0, 1, BodySide.BILATERAL, 12).copy(legacyTrainingRecordId = 101),
                entry(copyLunge, 0, 1, BodySide.RIGHT, 8).copy(legacyTrainingRecordId = 103),
                entry(copyLunge, 1, 1, BodySide.LEFT, 7).copy(legacyTrainingRecordId = 103),
            )
        )

        // Removing record 101 drops its copy and the occurrence it leaves empty; 103's copy is untouched.
        val loaded = loadLegacy()
        WorkoutEditor.saveLegacy(database, loaded, loaded.withExercise(1) { it.copy(sets = it.sets.drop(1)) }, library)
        assertEquals(listOf(copyLunge), dao.sessionExercises(copy).map { it.id })
        assertEquals(2, entries(copyLunge).size)
        assertRoundTrip()

        // Deleting the workout drops the remaining copies and their now-empty session.
        val rest = loadLegacy()
        WorkoutEditor.saveLegacy(database, rest, rest.copy(exercises = emptyList()), library)
        assertNull(dao.session(copy))
        assertRoundTrip()
    }
}
