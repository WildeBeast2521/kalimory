package io.github.gonbei774.calisthenicsmemory.screenshots

import android.app.Application
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.gonbei774.calisthenicsmemory.MainActivity
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.OnboardingPreferences
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ThemePreferences
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import io.github.gonbei774.calisthenicsmemory.util.AppClock
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.TimeZone

/**
 * The README and store screenshots (#158), from the real app with made-up training, in Light and
 * Dark at 1080 × 1920. Skipped unless KALIMORY_SCREENSHOTS names an output folder; run it with
 * scripts/screenshots/update-screenshots.sh, which also resizes the images and builds the social
 * previews.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xxhdpi")
class StoreScreenshotsTest(private val theme: AppTheme) {
    private val out: String? = System.getenv("KALIMORY_SCREENSHOTS")
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    private val state = object : ExternalResource() {
        private var zone: TimeZone? = null
        override fun before() {
            assumeTrue("Set KALIMORY_SCREENSHOTS to make the screenshots", out != null)
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
            zone?.let { TimeZone.setDefault(it) }
        }
    }
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(state).around(compose)

    @Test fun today() {
        waitFor(PROGRAM)
        save("today")
    }

    @Test fun calendar() {
        open("Progress")
        waitFor("Pike push-up")
        save("calendar")
    }

    @Test fun progressions() {
        openProgressions()
        save("progressions")
    }

    @Test fun chain() {
        openProgressions()
        compose.onAllNodes(hasClickAction() and hasText("Step 4 of", substring = true)).onFirst().performClick()
        waitFor("Diamond push-up")
        save("chain")
    }

    @Test fun step() {
        openProgressions()
        compose.onAllNodes(hasClickAction() and hasText("Step 4 of", substring = true)).onFirst().performClick()
        waitFor("Diamond push-up")
        compose.onAllNodes(hasClickAction() and hasText("Diamond push-up", substring = true)).onFirst().performClick()
        waitFor("How to do it")
        save("step")
    }

    private fun openProgressions() {
        open("Progress")
        waitFor("Progressions")
        compose.onAllNodes(hasText("Progressions") and hasClickAction()).onFirst().performClick()
        waitFor("Your progressions")
    }

    private fun open(tab: String) {
        val item = hasText(tab) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))
        compose.waitUntil(TIMEOUT_MS) { compose.onAllNodes(item).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(item).performClick()
    }

    private fun waitFor(text: String) {
        compose.waitUntil(TIMEOUT_MS) { compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun save(name: String) {
        compose.waitForIdle()
        captureScreenRoboImage(File(out!!, "$name-${theme.code}.png").path)
    }

    /** Made-up training: two progressions under way, a program due today and five weeks of sessions. */
    private fun seed(db: AppDatabase) = runBlocking {
        val exercises = db.exerciseDao()
        val knee = exercises.insertExercise(Exercise(name = "Knee push-up", type = "Dynamic", catalogId = "push.knee", displayOrder = 0, targetSets = 3, targetValue = 15))
        val pushUp = exercises.insertExercise(Exercise(name = "Push-up", type = "Dynamic", catalogId = "push.full", displayOrder = 1, targetSets = 3, targetValue = 12))
        val pike = exercises.insertExercise(Exercise(name = "Pike push-up", type = "Dynamic", catalogId = "vpush.pike", displayOrder = 2, targetSets = 3, targetValue = 10))
        val plank = exercises.insertExercise(Exercise(name = "Plank", type = "Isometric", catalogId = "core.plank", displayOrder = 3, targetSets = 2, targetValue = 45))

        val programId = db.programDao().insert(Program(name = PROGRAM))
        db.programExerciseDao().insertAll(
            listOf(
                ProgramExercise(programId = programId, exerciseId = pushUp, sortOrder = 0, sets = 3, targetValue = 12),
                ProgramExercise(programId = programId, exerciseId = pike, sortOrder = 1, sets = 3, targetValue = 10),
                ProgramExercise(programId = programId, exerciseId = plank, sortOrder = 2, sets = 2, targetValue = 45),
            )
        )
        db.todoTaskDao().insert(TodoTask(type = TodoTask.TYPE_PROGRAM, referenceId = programId, sortOrder = 0))

        // Mondays and Thursdays for five weeks, up to this morning: knee push-ups met their standard
        // in the first weeks, then push-ups took over; pike push-ups and planks grow slowly.
        val days = (0L until 5L).flatMap { week -> listOf(TODAY.minusWeeks(week), TODAY.minusDays(3).minusWeeks(week)) }.sorted()
        val records = mutableListOf<TrainingRecord>()
        days.forEachIndexed { session, day ->
            fun sets(exerciseId: Long, values: List<Int>, time: String) = values.forEachIndexed { i, value ->
                records += TrainingRecord(exerciseId = exerciseId, valueRight = value, setNumber = i + 1, date = day.toString(), time = time)
            }
            if (session < 4) sets(knee, listOf(12 + session, 12 + session, 11 + session), "07:00")
            else sets(pushUp, listOf(8 + session / 2, 8 + session / 2, 7 + session / 2), "07:00")
            sets(pike, listOf(6 + session / 3, 6 + session / 3, 5 + session / 3), "07:15")
            sets(plank, listOf(30 + 2 * session, 25 + 2 * session), "07:30")
        }
        db.trainingRecordDao().insertRecords(records)
    }

    companion object {
        /** A Thursday. */
        private val TODAY: LocalDate = LocalDate.of(2026, 10, 8)
        private const val PROGRAM = "Upper body"
        private const val TIMEOUT_MS = 10_000L

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes() = listOf(AppTheme.LIGHT, AppTheme.DARK).map { arrayOf<Any>(it) }
    }
}
