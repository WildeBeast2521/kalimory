package io.github.gonbei774.calisthenicsmemory

import org.junit.Assert.assertEquals
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

    @Test fun `the guards can see the sources`() {
        // If neither path resolved, every guard below would pass on an empty list.
        assertTrue("Found ${kotlin.size} Kotlin files: the guards are not reading the app", kotlin.size > 100)
    }

    // ---- The button scheme (docs/ux.md): filled primary, tonal for helpers, outlined for quiet actions,
    // DestructiveButton, and a text button only in a dialog's action slots. ----

    /** Index of the bracket that closes the one opening at [open]; the text is not parsed, so brackets inside strings count. */
    private fun closing(text: String, open: Int): Int {
        val pair = mapOf('(' to ')', '{' to '}', '[' to ']')
        var depth = 0
        for (i in open until text.length) {
            val c = text[i]
            if (c == text[open]) depth++ else if (c == pair.getValue(text[open]) && --depth == 0) return i
        }
        return text.length - 1
    }

    /** The character ranges of every `confirmButton = { ... }` and `dismissButton = { ... }`, a dialog's action slots. */
    private fun dialogSlots(text: String): List<IntRange> =
        Regex("""\b(confirmButton|dismissButton)\s*=\s*\{""").findAll(text).map { it.range.last..closing(text, it.range.last) }.toList()

    /** Positions of `TextButton(` calls: there are none, a dialog's actions are buttons too. */
    private fun textButtons(text: String): List<Int> = Regex("""\bTextButton\(""").findAll(text).map { it.range.first }.toList()

    @Test fun `the dialog slot helper finds the slots`() {
        val text = "AlertDialog(confirmButton = { Row { Button(onClick = {}) { Text(\"OK\") } } }, dismissButton = { OutlinedButton(onClick = {}) { } })"
        assertEquals(2, dialogSlots(text).size)
        assertEquals(1, textButtons("Column { TextButton(onClick = {}) { Text(\"Skip\") } }").size)
    }

    @Test fun `there is no text button, a dialog's actions are buttons too`() {
        val offenders = kotlin.filter { textButtons(it.readText()).isNotEmpty() }.map { it.name }
        assertTrue("Use a filled, tonal or outlined button (docs/ux.md): $offenders", offenders.isEmpty())
    }

    @Test fun `a dialog confirms with a filled button and dismisses with an outlined one`() {
        val offenders = kotlin.flatMap { file ->
            val text = file.readText()
            Regex("""\b(confirmButton|dismissButton)\s*=\s*\{""").findAll(text).mapNotNull { m ->
                val slot = text.substring(m.range.last, closing(text, m.range.last))
                val first = Regex("""\b(Outlined|FilledTonal|Text|Elevated|Destructive)?Button\(""").find(slot)?.value
                val ok = if (m.groupValues[1] == "confirmButton") first == "Button(" || first == "DestructiveButton(" else first == "OutlinedButton(" || first == "DestructiveButton("
                file.name.takeIf { first != null && !ok }
            }.toList()
        }.distinct()
        assertTrue("Confirm is a Button and dismiss an OutlinedButton: $offenders", offenders.isEmpty())
    }

    @Test fun `text is never tapped on its own`() {
        val offenders = kotlin.filter { file ->
            val text = file.readText()
            Regex("""(?<![A-Za-z])(Fit)?Text\(""").findAll(text).any { m ->
                val args = text.substring(m.range.last, closing(text, m.range.last))
                Regex("""\.(clickable|combinedClickable|selectable|toggleable)\(""").containsMatchIn(args)
            } || "ClickableText(" in text || "LinkAnnotation" in text
        }.map { it.name }
        assertTrue("Make it a button: $offenders", offenders.isEmpty())
    }

    @Test fun `a button keeps the theme's colours and border`() {
        // Only an error colour (a destructive confirm) may be set by hand; WorkoutPrimaryButton and DestructiveButton
        // are the two components that own their look.
        val owners = setOf("WorkoutKit.kt", "DestructiveButton.kt")
        val call = Regex("""\b(Outlined|FilledTonal|Text|Elevated)?Button\(""")
        val offenders = kotlin.filter { it.name !in owners }.flatMap { file ->
            val text = file.readText()
            call.findAll(text).mapNotNull { m ->
                val args = text.substring(m.range.last, closing(text, m.range.last))
                val set = Regex("""\b(colors|border)\s*=\s*([^\n]*(\n\s+[^\n=]*)*)""").findAll(args).map { it.value }.toList()
                file.name.takeIf { set.any { v -> ".error" !in v } }
            }.toList()
        }.distinct()
        assertTrue("Leave colours and borders to the theme: $offenders", offenders.isEmpty())
    }

    @Test fun `a button never has a fixed height`() {
        // A fixed height cuts the label off at large text; a minimum height grows with it. The check reads each
        // Button( call up to its closing bracket, so a height on something else nearby is not counted.
        val call = Regex("""\b(Outlined|FilledTonal|Text|Elevated)?Button\(""")
        val offenders = kotlin.flatMap { file ->
            val text = file.readText()
            call.findAll(text).mapNotNull { m ->
                var depth = 0
                var i = m.range.last
                while (i < text.length) {
                    if (text[i] == '(') depth++ else if (text[i] == ')' && --depth == 0) break
                    i++
                }
                file.name.takeIf { Regex("""\.height\(""").containsMatchIn(text.substring(m.range.first, i)) }
            }.toList()
        }.distinct()
        assertTrue("Use heightIn(min = ...) on a button: $offenders", offenders.isEmpty())
    }

    @Test fun `every floating action button is the shared AppFab`() {
        val offenders = uses(Regex("""\b(Medium|Large|Small)?FloatingActionButton\("""), except = setOf("AppFab.kt"))
        assertTrue("Use AppFab instead: $offenders", offenders.isEmpty())
    }

    @Test fun `the keyboard is applied once, by the shared helper`() {
        // A screen padding for the keyboard on its own counted it twice (#280).
        val offenders = uses(Regex("""imePadding\(|WindowInsets\.ime\b|isImeVisible"""), except = setOf("KeyboardInsets.kt"))
        assertTrue("Use screenBottomInsets/keyboardInsets/keyboardVisible from KeyboardInsets.kt: $offenders", offenders.isEmpty())
        val activity = Regex("""<activity\s[^>]*android:name="\.MainActivity"[^>]*>""").find(main.resolve("AndroidManifest.xml").readText())?.value
        assertTrue("MainActivity must resize for the keyboard", activity?.contains("""android:windowSoftInputMode="adjustResize"""") == true)
    }

    @Test fun `a full-screen dialog drawn edge to edge sets its bar icons`() {
        // Such a dialog is its own window: without DialogSystemBars its status bar icons come out white on light.
        val offenders = kotlin.filter { it.name != "DialogSystemBars.kt" }
            .filter { Regex("""decorFitsSystemWindows\s*=\s*false""").containsMatchIn(it.readText()) && !it.readText().contains("DialogSystemBars()") }
            .map { it.name }
        assertTrue("Call DialogSystemBars() in: $offenders", offenders.isEmpty())
    }

    @Test fun `a corner radius the theme already has comes from the theme`() {
        // 8, 12, 16, 20 and 28 dp are the theme's shapes; the numbers drifted when each screen set its own.
        val offenders = uses(Regex("""RoundedCornerShape\((8|12|16|20|28)\.dp\)"""), except = setOf("Shapes.kt"))
        assertTrue("Use MaterialTheme.shapes instead: $offenders", offenders.isEmpty())
    }

    @Test fun `a dialog keeps the theme's background`() {
        // Dialogs set their own surface colour in a dozen places and no two agreed.
        val offenders = kotlin.filter { file -> setsDialogContainerColor(file.readText()) }.map { it.name }
        assertTrue("Leave the AlertDialog's containerColor to the theme: $offenders", offenders.isEmpty())
    }

    /** True when an AlertDialog( call has a containerColor argument of its own (not one inside its content). */
    private fun setsDialogContainerColor(text: String): Boolean {
        var from = 0
        while (true) {
            val at = Regex("""(?<![A-Za-z])AlertDialog\(""").find(text, from) ?: return false
            var depth = 0
            var i = at.range.last
            while (i < text.length) {
                when (text[i]) {
                    '(', '{', '[' -> depth++
                    ')', '}', ']' -> depth--
                }
                if (depth == 1 && text.startsWith("containerColor = ", i) && text[i - 1] in " \n") return true
                if (depth == 0) break
                i++
            }
            from = at.range.last + 1
        }
    }

    @Test fun `a long press does not hide a menu`() {
        // A row's other actions sit behind its menu button; a long press is for dragging.
        val offenders = uses(Regex("""onLongClick\s*=\s*\{\s*(show|open)\w*Menu"""))
        assertTrue("Put the action in RowMenu instead: $offenders", offenders.isEmpty())
    }

    @Test fun `an on-off setting is a SwitchRow, not a switch built by hand`() {
        val offenders = uses(Regex("""(?<![A-Za-z])Switch\("""), except = setOf("SwitchRow.kt", "SettingRows.kt"))
        assertTrue("Use SwitchRow or SettingSwitchRow instead: $offenders", offenders.isEmpty())
    }

    @Test fun `an exercise's badges come from ExerciseBadges`() {
        // The level text beside an exercise was built by hand in a dozen places, in three sizes.
        val offenders = uses(Regex("""R\.string\.level_format"""), except = setOf("ExerciseBadges.kt", "ViewScreen.kt"))
        assertTrue("Use ExerciseBadges instead: $offenders", offenders.isEmpty())
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

    @Test fun `user-visible text comes from string resources, not literals`() {
        // Every user-visible string is a resource (AGENTS.md), so all ten languages get it. Proper
        // names stay as they are.
        val names = setOf("WildeBeast2521", "Gonbei774")
        val literal = Regex("""(?:\bText\(\s*|\btext\s*=\s*)"([^"]*)"""")
        // What a reader sees: without templates (${'$'}{…}, ${'$'}name) and escapes (\u2022, \n).
        val code = Regex("""\$\{[^}]*}|\$[A-Za-z_][A-Za-z0-9_.]*|\\u[0-9A-Fa-f]{4}|\\[ntr]""")
        val offenders = kotlin.flatMap { file ->
            literal.findAll(file.readText()).map { it.groupValues[1] }
                .filter { text -> text !in names && code.replace(text, "").any { it.isLetter() } }
                .map { "${file.name}: \"$it\"" }.toList()
        }
        assertTrue("Move these into strings.xml: $offenders", offenders.isEmpty())
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

    /** The two special spaces, which a resource file may write as the escapes \u00A0 and \u202F or as the characters themselves. */
    private fun decodeSpaces(value: String) = value.replace("\\u00A0", "\u00A0").replace("\\u202F", "\u202F")

    @Test fun `Russian and Ukrainian plurals use the many wording for other, as Weblate writes it`() {
        // Weblate keeps three forms for these languages and writes Android's fourth, "other",
        // from "many". Android uses "other" only for fractions, which the app never shows.
        val plurals = Regex("""<plurals name="([^"]+)"[^>]*>(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
        fun form(body: String, quantity: String) = Regex("""<item quantity="$quantity">([^<]*)</item>""").find(body)?.groupValues?.get(1)
        val offenders = listOf("values-ru", "values-uk").flatMap { dir ->
            plurals.findAll(main.resolve("res/$dir/strings.xml").readText())
                .filter { form(it.groupValues[2], "other") != form(it.groupValues[2], "many") }
                .map { "$dir/${it.groupValues[1]}" }
        }
        assertTrue("Make other the same as many: $offenders", offenders.isEmpty())
    }

    @Test fun `strings never start or end with a space, which Android drops`() {
        // An unquoted resource loses its outer spaces when it is compiled, so text glued on with a
        // leading space shows without it. Join the parts with a format string instead.
        val offenders = stringFiles.flatMap { file ->
            stringValue.findAll(file.readText()).filter { it.groupValues[2].let { v -> v.startsWith(" ") || v.endsWith(" ") } }
                .map { "${file.parentFile.name}/${it.groupValues[1]}" }
        }
        assertTrue("Remove the outer space and use a format string: $offenders", offenders.isEmpty())
    }

    @Test fun `French puts a space before colons, semicolons, question and exclamation marks`() {
        val offenders = stringFiles.filter { it.parentFile.name == "values-fr" }.flatMap { file ->
            stringValue.findAll(file.readText()).filter { Regex("""\p{L}[:;?!]""").containsMatchIn(decodeSpaces(it.groupValues[2])) }
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
            "JSON", "CSV", "LED", "ON", "GitHub", "Kalimory", "Android", "English", "Programs", "Intervals", "Select", "New", "Morning", "L-sit", "Complete")
        val strings = main.resolve("res/values/strings.xml").readText()
        val offenders = Regex("""<string name="([^"]+)"[^>]*>([^<]{1,60})</string>""").findAll(strings)
            .filter { m ->
                val words = Regex("""[A-Za-z][A-Za-z'\-]*""").findAll(m.groupValues[2].substringBefore(". ")).map { it.value }.toList()
                words.drop(1).any { it[0].isUpperCase() && !it.all(Char::isUpperCase) && it !in proper }
            }
            .map { it.groupValues[1] }.toList()
        assertTrue("Use sentence case: $offenders", offenders.isEmpty())
    }

    @Test fun `one-line labels that shrink to fit use FitText`() {
        // FitText keeps a label whole at large font sizes without shrinking it out of reach; a
        // screen with its own TextAutoSize would pick different limits.
        val offenders = uses(Regex("""TextAutoSize"""), except = setOf("FitText.kt"))
        assertTrue("Use FitText: $offenders", offenders.isEmpty())
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
