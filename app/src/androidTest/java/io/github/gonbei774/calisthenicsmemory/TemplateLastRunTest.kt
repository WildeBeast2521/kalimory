package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.v2.TemplateLastRun
import io.github.gonbei774.calisthenicsmemory.data.v2.TimePrecision
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** "Last done" for programs comes from the latest v2 session that names the program. */
@RunWith(AndroidJUnit4::class)
class TemplateLastRunTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).build()
    }

    @After
    fun tearDown() = database.close()

    private suspend fun session(type: WorkoutSourceType, templateId: Long?, startedAt: Long) =
        database.workoutSessionDao().insertSession(
            WorkoutSessionEntity(
                status = WorkoutSessionStatus.COMPLETED, sourceType = type, sourceTemplateId = templateId,
                startedAtEpochMillis = startedAt, updatedAtEpochMillis = startedAt, timePrecision = TimePrecision.EXACT,
            )
        )

    @Test
    fun latestRunPerProgramAndIntervalProgram(): Unit = runBlocking {
        session(WorkoutSourceType.PROGRAM_TEMPLATE, 1, 1_000)
        session(WorkoutSourceType.PROGRAM_TEMPLATE, 1, 3_000)
        session(WorkoutSourceType.PROGRAM_TEMPLATE, 2, 2_000)
        // The same id in another source is another thing.
        session(WorkoutSourceType.INTERVAL_TEMPLATE, 1, 5_000)
        // Workouts with no template do not count.
        session(WorkoutSourceType.AD_HOC, null, 9_000)

        val runs = database.workoutSessionDao().observeTemplateLastRuns().first().toSet()
        assertEquals(
            setOf(
                TemplateLastRun(WorkoutSourceType.PROGRAM_TEMPLATE, 1, 3_000),
                TemplateLastRun(WorkoutSourceType.PROGRAM_TEMPLATE, 2, 2_000),
                TemplateLastRun(WorkoutSourceType.INTERVAL_TEMPLATE, 1, 5_000),
            ),
            runs,
        )
    }
}
