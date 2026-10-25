package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.gonbei774.calisthenicsmemory.MainActivity
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.OnboardingPreferences
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ThemePreferences
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import io.github.gonbei774.calisthenicsmemory.util.AppClock
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.TimeZone

/**
 * The real app with a small made-up training history and a fixed date (AppClock), shared by the
 * main-screen screenshot and accessibility tests. [state] runs before the activity starts and is
 * undone after it closes.
 */
internal object MainScreenFixture {
    /** A Thursday. */
    val TODAY: LocalDate = LocalDate.of(2026, 3, 12)
    const val PROGRAM = "Upper body"
    // The seeded program's settled estimate; a capture taken before it settled showed "~1 min".
    const val PROGRAM_ESTIMATE = "~13 min"
    const val STARTUP_TIMEOUT_MS = 10_000L

    fun state(theme: AppTheme): ExternalResource = object : ExternalResource() {
        private val context get() = ApplicationProvider.getApplicationContext<android.app.Application>()
        private var zone: TimeZone? = null

        override fun before() {
            zone = TimeZone.getDefault()
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            AppClock.fixed = Clock.fixed(TODAY.atTime(18, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
            OnboardingPreferences(context).markWelcomeSeen()
            ThemePreferences(context).setTheme(theme)
            seed(AppDatabase.getDatabase(context))
        }

        override fun after() {
            AppDatabase.closeForRestore()
            AppClock.fixed = null
            TimeZone.setDefault(zone)
        }
    }

    /** Made-up data: two groups, four exercises, a program due today and four weeks of training. */
    private fun seed(db: AppDatabase) = runBlocking {
        db.exerciseGroupDao().insertGroup(ExerciseGroup(name = "Push", displayOrder = 0))
        db.exerciseGroupDao().insertGroup(ExerciseGroup(name = "Pull", displayOrder = 1))
        val exercises = db.exerciseDao()
        val pushUp = exercises.insertExercise(Exercise(name = "Push-up", type = "Dynamic", group = "Push", sortOrder = 1, displayOrder = 0, targetSets = 3, targetValue = 12))
        val pike = exercises.insertExercise(Exercise(name = "Pike push-up", type = "Dynamic", group = "Push", sortOrder = 2, displayOrder = 1, targetSets = 3, targetValue = 8))
        val row = exercises.insertExercise(Exercise(name = "Australian row", type = "Dynamic", group = "Pull", sortOrder = 1, displayOrder = 2, targetSets = 3, targetValue = 10))
        val plank = exercises.insertExercise(Exercise(name = "Plank", type = "Isometric", displayOrder = 3, targetSets = 2, targetValue = 45))

        val programId = db.programDao().insert(Program(name = PROGRAM))
        db.programExerciseDao().insertAll(
            listOf(
                ProgramExercise(programId = programId, exerciseId = pushUp, sortOrder = 0, sets = 3, targetValue = 12),
                ProgramExercise(programId = programId, exerciseId = row, sortOrder = 1, sets = 3, targetValue = 10),
                ProgramExercise(programId = programId, exerciseId = plank, sortOrder = 2, sets = 2, targetValue = 45),
            )
        )
        db.todoTaskDao().insert(TodoTask(type = TodoTask.TYPE_PROGRAM, referenceId = programId, sortOrder = 0))

        // Mondays and Thursdays for four weeks, a little better each time; Thursday is today.
        val records = mutableListOf<TrainingRecord>()
        val days = (0L until 4L).flatMap { week -> listOf(TODAY.minusWeeks(week), TODAY.minusWeeks(week).minusDays(3)) }.sorted()
        days.forEachIndexed { session, day ->
            fun sets(exerciseId: Long, base: Int, count: Int, time: String) = (1..count).forEach { set ->
                records += TrainingRecord(exerciseId = exerciseId, valueRight = base + session - set + 1, setNumber = set, date = day.toString(), time = time)
            }
            sets(pushUp, base = 8, count = 3, time = "07:00")
            sets(row, base = 6, count = 3, time = "07:10")
            if (session % 2 == 0) sets(pike, base = 4, count = 2, time = "07:20")
            sets(plank, base = 30, count = 2, time = "07:30")
        }
        db.trainingRecordDao().insertRecords(records)
    }
}
