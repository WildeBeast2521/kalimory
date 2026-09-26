package io.github.gonbei774.calisthenicsmemory.migration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.BackupSnapshot
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.data.v2.BodySide
import io.github.gonbei774.calisthenicsmemory.data.v2.LegacyAnomalyReason
import io.github.gonbei774.calisthenicsmemory.data.v2.V2Backfill
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId

/** The backfill writes v2 sessions in one transaction, leaves legacy rows untouched, and is idempotent. */
@RunWith(AndroidJUnit4::class)
class V2BackfillRunTest {
    private lateinit var database: AppDatabase
    private val zone = ZoneId.of("Europe/Berlin")

    private val legacy = BackupSnapshot(
        groups = listOf(ExerciseGroup(1, "Push", 0)),
        exercises = listOf(
            Exercise(1, "Push-up", "Dynamic", "Push"),
            Exercise(2, "Lunge", "Dynamic", laterality = "Unilateral"),
        ),
        records = listOf(
            TrainingRecord(1, 1, 12, null, 1, "2025-01-05", "07:30", "Workout"),
            TrainingRecord(2, 1, 10, null, 2, "2025-01-05", "07:30", "Workout"),
            TrainingRecord(3, 2, 8, 7, 1, "2025-01-05", "07:30", "Workout"),
            TrainingRecord(4, 2, 8, null, 1, "2025-01-06", "18:00", "Workout"),
        ),
        programs = emptyList(), programExercises = emptyList(), programLoops = emptyList(),
        intervalPrograms = emptyList(), intervalProgramExercises = emptyList(), intervalRecords = emptyList(),
        todoTasks = emptyList(),
    )

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        database.backupDao().replaceAll(legacy)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun convertsOnceKeepsLegacyRowsAndReportsTheRest() = runBlocking {
        val first = V2Backfill.run(database, zone, nowEpochMillis = 1)
        assertEquals(3, first.convertedRecords)
        assertEquals(mapOf(LegacyAnomalyReason.AMBIGUOUS_SIDE to 1), first.anomalyCounts)

        val dao = database.workoutSessionDao()
        val session = dao.sessionGraph(dao.sessionsNewestFirst().single().id)!!
        assertEquals(listOf("Push-up", "Lunge"), session.exercises.map { it.first.exerciseNameSnapshot })
        assertEquals(listOf(BodySide.RIGHT, BodySide.LEFT), session.exercises[1].second.map { it.side })

        val second = V2Backfill.run(database, zone, nowEpochMillis = 2)
        assertEquals(0, second.convertedRecords)
        assertEquals(3, second.alreadyConvertedRecords)
        assertEquals(0, second.sessionsCreated)
        assertEquals(1, dao.sessionsNewestFirst().size)

        // The legacy tables are unchanged (the snapshot now also carries the v2 rows).
        assertEquals(
            legacy,
            database.backupDao().snapshot().copy(workoutSessions = emptyList(), sessionExercises = emptyList(), setEntries = emptyList()),
        )
    }
}
