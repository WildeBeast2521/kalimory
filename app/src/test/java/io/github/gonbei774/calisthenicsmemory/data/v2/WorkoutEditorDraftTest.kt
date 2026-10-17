package io.github.gonbei774.calisthenicsmemory.data.v2

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** How saved workouts read into the editor (ADR 0009). */
class WorkoutEditorDraftTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val start = ZonedDateTime.of(2026, 3, 2, 7, 30, 42, 0, zone).toInstant().toEpochMilli()

    private fun session(source: WorkoutSourceType = WorkoutSourceType.PROGRAM_TEMPLATE) = WorkoutSessionEntity(
        id = 1, status = WorkoutSessionStatus.COMPLETED, sourceType = source, startedAtEpochMillis = start,
        updatedAtEpochMillis = start, timePrecision = TimePrecision.EXACT, comment = "Upper body",
    )

    private fun occurrence(id: Long, exerciseId: Long?, kind: ExerciseKind, laterality: Laterality) = SessionExerciseEntity(
        id = id, workoutSessionId = 1, orderIndex = id.toInt(), exerciseId = exerciseId, exerciseNameSnapshot = "E$id",
        exerciseKindSnapshot = kind, lateralitySnapshot = laterality,
    )

    private fun entry(
        id: Long, occurrence: Long, order: Int, number: Int, side: BodySide, reps: Int? = null, millis: Long? = null,
        status: SetEntryStatus = SetEntryStatus.COMPLETED, round: Int? = null, legacy: Long? = null, weight: Int? = null,
    ) = SetEntryEntity(
        id = id, sessionExerciseId = occurrence, orderIndex = order, setNumber = number, roundNumber = round, status = status,
        side = side, repetitions = reps, durationMillis = millis, addedWeightGrams = weight,
        timePrecision = TimePrecision.EXACT, legacyTrainingRecordId = legacy,
    )

    @Test
    fun aV2WorkoutShowsItsCompletedSetsByExercise() {
        val graph = WorkoutSessionGraph(
            session(),
            listOf(
                occurrence(10, 1, ExerciseKind.DYNAMIC, Laterality.UNILATERAL) to listOf(
                    entry(100, 10, 0, 1, BodySide.RIGHT, reps = 8, weight = 2_000),
                    entry(101, 10, 1, 1, BodySide.LEFT, reps = 7, weight = 2_000),
                    entry(102, 10, 2, 2, BodySide.RIGHT, status = SetEntryStatus.SKIPPED),
                    entry(103, 10, 3, 3, BodySide.LEFT, reps = 6),
                ),
                occurrence(11, 2, ExerciseKind.ISOMETRIC, Laterality.BILATERAL) to listOf(
                    // Rounds share set numbers; each round's set is its own set.
                    entry(110, 11, 0, 1, BodySide.BILATERAL, millis = 45_900, round = 1),
                    entry(111, 11, 1, 1, BodySide.BILATERAL, millis = 30_000, round = 2),
                ),
            ),
        )
        assertEquals(
            WorkoutDraft(
                LocalDate.of(2026, 3, 2), LocalTime.of(7, 30), "Upper body",
                listOf(
                    DraftExercise(
                        10, 1, true,
                        listOf(DraftSet(listOf(100, 101), 8, 7, weightG = 2_000), DraftSet(listOf(103), null, 6)),
                    ),
                    DraftExercise(11, 2, false, listOf(DraftSet(listOf(110), 45), DraftSet(listOf(111), 30))),
                ),
            ),
            WorkoutEditor.draftOf(graph, zone),
        )
    }

    @Test
    fun hiddenRowsStayOutOfTheDraft() {
        val graph = WorkoutSessionGraph(
            session(),
            listOf(
                // An exercise since deleted from the library, and copies of legacy records: History shows neither.
                occurrence(10, null, ExerciseKind.DYNAMIC, Laterality.BILATERAL) to listOf(entry(100, 10, 0, 1, BodySide.BILATERAL, reps = 5)),
                occurrence(11, 2, ExerciseKind.DYNAMIC, Laterality.BILATERAL) to listOf(entry(110, 11, 0, 1, BodySide.BILATERAL, reps = 5, legacy = 7)),
                occurrence(12, 3, ExerciseKind.DYNAMIC, Laterality.BILATERAL) to listOf(entry(120, 12, 0, 1, BodySide.BILATERAL, status = SetEntryStatus.SKIPPED)),
                occurrence(13, 4, ExerciseKind.DYNAMIC, Laterality.BILATERAL) to listOf(entry(130, 13, 0, 1, BodySide.BILATERAL, reps = 9)),
            ),
        )
        assertEquals(listOf(13L), WorkoutEditor.draftOf(graph, zone)!!.exercises.map { it.key })
    }

    @Test
    fun intervalAndEmptyWorkoutsHaveNoDraft() {
        val sets = listOf(occurrence(10, 1, ExerciseKind.DYNAMIC, Laterality.BILATERAL) to listOf(entry(100, 10, 0, 1, BodySide.BILATERAL, reps = 5)))
        assertNull(WorkoutEditor.draftOf(WorkoutSessionGraph(session(WorkoutSourceType.INTERVAL_TEMPLATE), sets), zone))
        assertNull(WorkoutEditor.draftOf(WorkoutSessionGraph(session(), emptyList()), zone))
    }

    @Test
    fun aLegacyWorkoutGroupsRecordsByExerciseInTheOrderRecorded() {
        val library = listOf(Exercise(1, "Push-up", "Dynamic"), Exercise(2, "Lunge", "Dynamic", laterality = "Unilateral"))
        fun record(id: Long, exercise: Long, number: Int, right: Int, left: Int? = null) =
            TrainingRecord(id, exercise, right, left, number, "2026-03-02", "07:30", "Legs")
        val draft = WorkoutEditor.draftOf(
            listOf(record(5, 2, 1, 8, 7), record(3, 1, 2, 10), record(2, 1, 1, 12), record(6, 2, 2, 6)),
            library,
        )
        assertEquals(
            WorkoutDraft(
                LocalDate.of(2026, 3, 2), LocalTime.of(7, 30), "Legs",
                listOf(
                    DraftExercise(1, 1, false, listOf(DraftSet(listOf(2), 12), DraftSet(listOf(3), 10))),
                    DraftExercise(2, 2, true, listOf(DraftSet(listOf(5), 8, 7), DraftSet(listOf(6), 6))),
                ),
            ),
            draft,
        )
    }

    @Test
    fun legacyDatesThatCannotBeWrittenBackAreNotEditable() {
        val library = listOf(Exercise(1, "Push-up", "Dynamic"))
        assertNull(WorkoutEditor.draftOf(listOf(TrainingRecord(1, 1, 5, null, 1, "2026-3-2", "07:30")), library))
        assertNull(WorkoutEditor.draftOf(listOf(TrainingRecord(1, 1, 5, null, 1, "2026-03-02", "7:30")), library))
        assertNull(WorkoutEditor.draftOf(emptyList(), library))
    }
}
