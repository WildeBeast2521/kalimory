package io.github.gonbei774.calisthenicsmemory

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards for the consistency findings of the round 2 phone test (dev-docs). Each check reads the
 * sources or resources, so a screen that brings back its own variant fails here before it ships.
 */
class ConsistencyGuardTest {

    private val main = File("src/main").let { if (it.exists()) it else File("app/src/main") }
    private val kotlin = main.resolve("java").walkTopDown().filter { it.extension == "kt" }.toList()
    private fun uses(pattern: Regex, except: Set<String> = emptySet()) =
        kotlin.filter { it.name !in except && pattern.containsMatchIn(it.readText()) }.map { it.name }

    @Test fun `every floating action button is the shared AppFab`() {
        val offenders = uses(Regex("""\b(Medium|Large|Small)?FloatingActionButton\("""), except = setOf("AppFab.kt"))
        assertTrue("Use AppFab instead: $offenders", offenders.isEmpty())
    }

    @Test fun `every swipe to delete draws the shared background`() {
        val swipes = uses(Regex("""SwipeToDismissBox\("""))
        val shared = uses(Regex("""backgroundContent = \{ SwipeToDeleteBackground\("""))
        assertTrue("Swipe without SwipeToDeleteBackground: ${swipes - shared.toSet()}", swipes.toSet() == shared.toSet())
    }

    @Test fun `steppers use the shared step button, not text minus and plus`() {
        val offenders = uses(Regex("""Text\("[−+-]"\s*,"""))
        assertTrue("Use NumberStepButton: $offenders", offenders.isEmpty())
    }

    @Test fun `numbers use proportional digits outside the workout numeral style`() {
        val offenders = uses(Regex("""fontFeatureSettings = "tnum""""), except = setOf("Type.kt"))
        assertTrue("Tabular digits spread pairs apart: $offenders", offenders.isEmpty())
    }

    @Test fun `start buttons are the shared pill, not small rectangles`() {
        val offenders = uses(Regex("""R\.string\.(todo_start_button|interval_start|program_list_start)\b"""))
        assertTrue("Use StartButton: $offenders", offenders.isEmpty())
    }

    @Test fun `english counts are plurals, never "(s)"`() {
        val strings = main.resolve("res/values/strings.xml").readText()
        val offenders = Regex("""<string name="([^"]+)">[^<]*\(s\)""").findAll(strings).map { it.groupValues[1] }.toList()
        assertTrue("Use a plurals resource: $offenders", offenders.isEmpty())
    }

    private val stringFiles = main.resolve("res").listFiles { dir -> dir.name.startsWith("values") }.orEmpty()
        .flatMap { dir -> listOf("strings.xml", "catalogue_strings.xml").map { dir.resolve(it) }.filter { it.exists() } }
    private val stringValue = Regex("""<(?:string name="([^"]+)"[^>]*|item quantity="[^"]+")>([^<]*)<""")

    @Test fun `strings use the ellipsis character, never three dots`() {
        val offenders = stringFiles.flatMap { file ->
            stringValue.findAll(file.readText()).filter { "..." in it.groupValues[2] }.map { "${file.parentFile.name}/${it.groupValues[1]}" }
        }
        assertTrue("Use … instead of ...: $offenders", offenders.isEmpty())
    }

    @Test fun `French puts a space before colons, semicolons, question and exclamation marks`() {
        val offenders = stringFiles.filter { it.parentFile.name == "values-fr" }.flatMap { file ->
            stringValue.findAll(file.readText()).filter { Regex("""\p{L}[:;?!]""").containsMatchIn(it.groupValues[2]) }
                .map { it.groupValues[1] }
        }
        assertTrue("French typography needs a space before : ; ? !: $offenders", offenders.isEmpty())
    }

    @Test fun `user facing English strings use sentence case for dialog and screen titles`() {
        val strings = main.resolve("res/values/strings.xml").readText()
        val titleCase = listOf("Stop Workout?", "New Program", "Delete Program", "Set Challenge", "Get Ready", "Partial Data Management")
        val offenders = titleCase.filter { ">$it" in strings }
        assertTrue("Old wording is back: $offenders", offenders.isEmpty())
    }

    @Test fun `today and the time of day come from AppClock`() {
        // Screens and the view model read the date through one clock, which tests can fix.
        val offenders = uses(Regex("""\bLocal(Date|Time)\.now\(\s*\)"""), except = setOf("AppClock.kt"))
        assertTrue("Use AppClock.today() or AppClock.timeNow(): $offenders", offenders.isEmpty())
    }

    @Test fun `workouts save only the user's own comment`() {
        val offenders = uses(Regex("""【Program】|workout_mode_comment"""))
        assertTrue("No automatic comments: $offenders", offenders.isEmpty())
    }

    @Test fun `short English labels use sentence case`() {
        // Proper names and feature names keep their capitals; everything else is lower case after
        // the first word, so "Add Another Set" cannot come back beside "Add another exercise".
        val proper = setOf("Today", "Train", "Progress", "Progressions", "Library", "To", "Do", "Done",
            "JSON", "CSV", "LED", "ON", "GitHub", "Kalimory", "Android", "Programs", "Intervals", "Select", "New", "Morning")
        val strings = main.resolve("res/values/strings.xml").readText()
        val offenders = Regex("""<string name="([^"]+)"[^>]*>([^<]{1,60})</string>""").findAll(strings)
            .filter { m ->
                val words = Regex("""[A-Za-z][A-Za-z'\-]*""").findAll(m.groupValues[2].substringBefore(". ")).map { it.value }.toList()
                words.drop(1).any { it[0].isUpperCase() && !it.all(Char::isUpperCase) && it !in proper }
            }
            .map { it.groupValues[1] }.toList()
        assertTrue("Use sentence case: $offenders", offenders.isEmpty())
    }

    @Test fun `there is one start card style`() {
        // Resume, next up and start-a-workout cards on Today and Train all look the same.
        val offenders = uses(Regex("""emphasised"""))
        assertTrue("HeroCard has one style: $offenders", offenders.isEmpty())
    }

    @Test fun `every countdown says Get ready and every rest says Rest`() {
        val offenders = uses(Regex("""WorkoutStatus\(stringResource\(R\.string\.(preparing|interval_label)\)"""))
        assertTrue("Use interval_get_ready / interval_rest_label: $offenders", offenders.isEmpty())
    }
}
