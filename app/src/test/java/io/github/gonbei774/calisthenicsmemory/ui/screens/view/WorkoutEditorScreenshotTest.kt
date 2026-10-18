package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.gonbei774.calisthenicsmemory.ui.components.SharedComponentScreenshotTest.ScreenshotTheme
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalTime

/**
 * Golden images of the workout editor (ADR 0009) in each theme, and in Arabic to check the
 * right-to-left layout. The workout is synthetic.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h900dp-xhdpi")
class WorkoutEditorScreenshotTest(private val theme: ScreenshotTheme) {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val state = EditorState(
        date = LocalDate.of(2026, 3, 12),
        time = LocalTime.of(18, 30),
        comment = "Felt strong",
        exercises = listOf(
            EditorExercise(
                key = 1, exerciseId = 1, name = "Push-up", unilateral = false, dynamic = true,
                showDistance = false, showWeight = true, showAssistance = false,
                sets = listOf(EditorSet(listOf(1), value = "12", weightKg = "5"), EditorSet(value = "")),
            ),
            EditorExercise(
                key = 2, exerciseId = 2, name = "Lunge", unilateral = true, dynamic = true,
                showDistance = false, showWeight = false, showAssistance = false,
                sets = listOf(EditorSet(listOf(2, 3), value = "8", valueLeft = "7")),
            ),
        ),
    )

    @Test
    fun editor() = snap("WorkoutEditor_${theme.label}")

    @Test
    fun editorRightToLeft() {
        // One theme is enough to see the mirrored layout.
        if (theme != ScreenshotTheme.LIGHT) return
        RuntimeEnvironment.setQualifiers("+ar")
        snap("WorkoutEditor_ar")
    }

    private fun snap(name: String) {
        compose.setContent {
            CalisthenicsMemoryTheme(darkTheme = theme.dark, dynamicColor = false, trueBlack = theme.trueBlack) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    EditorContent(state = state, padding = PaddingValues(), onChange = {}, onAddExercise = {})
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes() = ScreenshotTheme.entries.map { arrayOf<Any>(it) }
    }
}
